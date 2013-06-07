/*
 * Entwined STM
 * 
 * © Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.entwined;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import cern.entwined.exception.ConflictException;
import cern.entwined.exception.InvocationException;
import cern.entwined.exception.MemoryException;
import cern.entwined.exception.NoTransactionException;

/**
 * Software transactional memory root class. It manages global memory snapshot and concurrent execution of transactions.
 * 
 * @param <T> The client {@link SemiPersistent} type.
 * @author Ivan Koblik
 */
public class Memory<T extends SemiPersistent<T>> {

    /**
     * Number of retries of a transaction in case of conflicts.
     */
    protected static final int NUM_RETRIES = 10000;

    /**
     * Globally shared memory.
     */
    private volatile BaseSnapshot<T> globalSnapshot;

    /**
     * Sequence of unique identifiers, e.g. every instance of {@link GlobalReference} takes one id.
     */
    private final AtomicLong idSequence = new AtomicLong(0);

    /**
     * This lock is used to serialize updates of the global state.
     */
    private final ReadWriteLock accessLock = new ReentrantReadWriteLock();

    /**
     * Thread local variable storing stack of snapshots starting with outer transaction through all the inner
     * transactions.
     */
    private final ThreadLocal<LinkedList<BaseSnapshot<T>>> threadLocalSnapshots = new ThreadLocal<LinkedList<BaseSnapshot<T>>>();

    /**
     * Thread local variable pointing to the current node with an I/O call back as its value.
     */
    private final ThreadLocal<Node<Transaction<T>>> currentNode = new ThreadLocal<Node<Transaction<T>>>();

    /**
     * Queue of snapshots in commit order, needed for synchronous I/O after commit.
     */
    private final ConcurrentLinkedQueue<BaseSnapshot<T>> commitQueue = new ConcurrentLinkedQueue<BaseSnapshot<T>>();

    /**
     * Flag is set to true only after a transaction has been committed and its committed block is being executed.
     */
    private final ThreadLocal<Boolean> isCommitting = new ThreadLocal<Boolean>();

    /**
     * Creates transactional memory with given initial state.
     * 
     * @param initialState The initial state of transactional memory. It takes a clean copy of it.
     */
    public Memory(T initialState) {
        Utils.checkNull("Initial State", initialState);
        this.globalSnapshot = new BaseSnapshot<T>(0l, initialState.cleanCopy());
    }

    /**
     * Executes a transaction and calls the given transactional user code.
     * 
     * @param transaction The transaction interface implementation.
     * @return the commit state <code>true</code> if committed, <code>false</code> if rolled back.
     */
    public boolean runTransaction(Transaction<T> transaction) {
        Utils.checkNull("Transaction callback", transaction);
        if (Boolean.TRUE == this.isCommitting.get()) {
            throw new MemoryException("Cannot run transaction within committed block.");
        }

        if (null == this.currentNode.get()) {
            return execOuterTransaction(transaction);
        } else {
            return execInnerTransaction(transaction);
        }
    }

    /**
     * Checks if there is a running transaction and if there is returns its snapshot.
     * 
     * @return The currently running transaction's snapshot.
     * @throws MemoryException if there is no running transaction.
     */
    protected BaseSnapshot<T> getBaseSnapshot() {
        LinkedList<BaseSnapshot<T>> stack = this.getSnapshotStack();
        if (stack.isEmpty()) {
            throw new NoTransactionException("There is no running transaction, cannot access the base snapshot");
        }
        return stack.peek();
    }

    /**
     * Returns the next unique ID that can be used in {@link BaseSnapshot}.
     * 
     * @return Unique ID for this memory instance.
     */
    protected Long getNextId() {
        return idSequence.getAndIncrement();
    }

    /**
     * Invoked when an outer transaction needs to be executed.
     * 
     * @param transaction The in-transactional user code.
     * @return the commit state <code>true</code> if committed, <code>false</code> if rolled back.
     */
    private boolean execOuterTransaction(Transaction<T> transaction) {
        int retryIdx = 0;
        while (true) {
            // Getting copy of the global snapshot for the transaction
            BaseSnapshot<T> transactionSnapshot = this.cleanCopyGlobalSnapshot();

            // Saving transaction's starting point
            Node<Transaction<T>> transactionNode = new Node<Transaction<T>>(transaction);
            this.currentNode.set(transactionNode);

            // Invoking transactional user code
            try {
                if (!invokeUserCode(transaction, transactionSnapshot, this.getSnapshotStack())) {
                    return false;
                }
            } finally {
                this.currentNode.set(null);
                this.threadLocalSnapshots.get().clear();
            }

            // Committing
            BaseSnapshot<T> newGlobalState;
            try {
                newGlobalState = this.commitSnapshot(transactionSnapshot);
            } catch (ConflictException ex) {
                // Too many retries, transaction has failed.
                if (++retryIdx > NUM_RETRIES) {
                    throw ex;
                }
                continue;
            }

            // Invoking the post-transactional I/O callbacks
            try {
                /*
                 * Waiting for the new snapshot to appear in the head of the queue and invoking the callback in
                 * postorder.
                 * 
                 * This code adds up to contention, if performance issues detected its the first thing to change.
                 */
                waitItsTurn(newGlobalState);
                isCommitting.set(true);
                this.postorder(transactionNode, newGlobalState);
            } finally {
                this.commitQueue.poll();
                isCommitting.set(false);
            }
            break;
        }
        return true;
    }

    /**
     * Invokes after-transactional callback methods in commit order.
     * 
     * @param node The callbacks root node.
     * @param snapshot The commit time snapshot.
     */
    private void postorder(Node<Transaction<T>> node, BaseSnapshot<T> snapshot) {
        for (Node<Transaction<T>> child : node.getChildren()) {
            this.postorder(child, snapshot);
        }
        // It is crucial to copy the global state or transactional memory will get broken.
        LinkedList<BaseSnapshot<T>> stack = getSnapshotStack();
        try {
            BaseSnapshot<T> cleanCopy = snapshot.cleanCopy();
            stack.push(cleanCopy);
            node.getValue().committed(cleanCopy.getClientData());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new InvocationException("Exception in committed block", e);
        } finally {
            stack.pop();
        }
    }

    /**
     * Invoked when an inner transaction need to be executed.
     * 
     * @param transaction The user in-transactional code.
     * @return the commit state <code>true</code> if committed, <code>false</code> if rolled back.
     */
    private boolean execInnerTransaction(Transaction<T> transaction) {
        LinkedList<BaseSnapshot<T>> snapshotStack = getSnapshotStack();
        Node<Transaction<T>> childNode = new Node<Transaction<T>>(transaction);
        BaseSnapshot<T> innerSnapshot = snapshotStack.peek().dirtyCopy();

        Node<Transaction<T>> parentNode = currentNode.get();
        parentNode.addChild(childNode);
        currentNode.set(childNode);
        boolean success = false; // Set to true if the user code is executed successfully
        try {
            // Called method either returns true or throws an exception
            success = this.invokeUserCode(transaction, innerSnapshot, snapshotStack);
        } finally {
            currentNode.set(parentNode);

            BaseSnapshot<T> outerSnapshot = snapshotStack.peek();
            if (!success) {
                // If we're here an exception was thrown in inner transaction
                parentNode.removeChild();
                // Preserving only the read logs
                outerSnapshot.update(innerSnapshot, true);
            } else {
                // Preserving the inner transaction's updates and read logs
                outerSnapshot.update(innerSnapshot, false);
            }
        }
        return success;
    }

    /**
     * Invokes user in-transactional code.
     * 
     * @param transaction The user in-transactional code.
     * @param transactionSnapshot The snapshot to be used by the transaction.
     * @param snapshotStack The thread local stack of transactions snapshots.
     * @return the commit request, <code>true</code> to commit, <code>false</code> to rollback.
     */
    private boolean invokeUserCode(Transaction<T> transaction, BaseSnapshot<T> transactionSnapshot,
            LinkedList<BaseSnapshot<T>> snapshotStack) {
        // === Added to the stack ===
        snapshotStack.push(transactionSnapshot);
        try {
            return transaction.run(transactionSnapshot.getClientData());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new InvocationException("Exception in the transactional code", e);
        } finally {
            snapshotStack.poll();
            // === Removed from the stack ===
        }
    }

    /**
     * Returns the thread local snapshot stack or if null, creates new one.
     * 
     * @return The thread local snapshot stack.
     */
    private LinkedList<BaseSnapshot<T>> getSnapshotStack() {
        // Creating thread local transaction stack and transaction log.
        LinkedList<BaseSnapshot<T>> snapshotStack = threadLocalSnapshots.get();
        if (null == snapshotStack) {
            snapshotStack = new LinkedList<BaseSnapshot<T>>();
            threadLocalSnapshots.set(snapshotStack);
        }
        return snapshotStack;
    }

    /**
     * Attempts to commit local changes and update global state.
     * 
     * @param transactionSnapshot The local changes.
     * @return New global state if commit successful.
     * @throws ConflictException if a conflicting changes detected.
     */
    private BaseSnapshot<T> commitSnapshot(BaseSnapshot<T> transactionSnapshot) {
        accessLock.writeLock().lock();
        try {
            BaseSnapshot<T> committedSnapshot = transactionSnapshot.commit(this.globalSnapshot);
            this.globalSnapshot = committedSnapshot;

            // Adding new snapshot to the tail of the post-transactional callbacks queue
            commitQueue.add(committedSnapshot);
            return committedSnapshot;
        } finally {
            accessLock.writeLock().unlock();
        }
    }

    /**
     * Returns a clean copy of the global snapshot.
     * 
     * @return The global snapshot clean copy.
     */
    private BaseSnapshot<T> cleanCopyGlobalSnapshot() {
        BaseSnapshot<T> snapshotCopy;
        // Not needed for a single instance. May be replaced with RMI&transient.
        // accessLock.readLock().lock();
        // try {
        snapshotCopy = this.globalSnapshot.cleanCopy();
        // } finally {
        // accessLock.readLock().unlock();
        // }
        return snapshotCopy;
    }

    /**
     * Waits for in the queue for its turn to execute post transactional callback.
     * 
     * @param newGlobalState The snapshot associated with transaction.
     */
    private void waitItsTurn(BaseSnapshot<T> newGlobalState) {
        boolean interrupted = false;

        // Waiting for argument to appear in the head of queue.
        while (this.commitQueue.peek() != newGlobalState) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                // Remembering the flag, but continuing waiting for the queue.
                interrupted = true;
            }
        }

        // Preserving the interrupted flag
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
