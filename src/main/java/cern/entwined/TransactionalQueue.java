/*
 * Entwined STM
 * 
 * © Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.entwined;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Queue;

import cern.entwined.exception.ConflictException;

/**
 * Transactional queue implementation. It supports non-conflicting simultaneous reading from the head and writing to the
 * tail.
 * 
 * @author Ivan Koblik
 */
public class TransactionalQueue<E> extends SemiPersistent<TransactionalQueue<E>> implements Queue<E> {

    /**
     * Used to initialize soruceIterator, only not to have <code>null</code>.
     */
    private final ListIterator<?> EMPTY_ITERATOR = Collections.EMPTY_LIST.listIterator();

    /**
     * The original queue, passed at construction.
     */
    private final List<E> sourceQueue;

    /**
     * Global counter of items read from the queue, needed for fast conflict detection. It is normal for this value to
     * overflow, therefore it is illegal to perform numeric comparison with it. Only yes/no equality checks are allowed.
     */
    private final int globalPollCount;

    /**
     * The iterator over the source queue.
     */
    private ListIterator<E> sourceIterator;

    /**
     * The queue of elements added to the queue in this transaction.
     */
    private List<E> tail;

    /**
     * The highest item index that was polled from the queue (source queue extended with the new tail).
     */
    private int pollCount = 0;

    /**
     * The highest item index that was peeked in the queue (source queue extended with the new tail). Even discarded
     * inner transactions can increase this value.
     */
    private int peekCount = 0;

    /**
     * Creates an empty {@link TransactionalQueue}.
     */
    @SuppressWarnings("unchecked")
    public TransactionalQueue() {
        this(Collections.EMPTY_LIST);
    }

    /**
     * Creates a new {@link TransactionalQueue} initializing it with the given collection of elements.
     * 
     * @param sourceCollection The {@link Collection} of elements copy of which will be used as
     *            {@link TransactionalQueue} initial state.
     */
    public TransactionalQueue(Collection<E> sourceCollection) {
        Utils.checkNull("Source collection", sourceCollection);
        this.sourceQueue = new LinkedList<E>(sourceCollection);
        this.globalPollCount = 0;
        this.sourceIterator = this.sourceQueue.listIterator();
        this.tail = new LinkedList<E>();
    }

    /**
     * Constructs the new {@link TransactionalQueue} without copying the given {@link List}.
     * 
     * @param source The source list to initialize the queue.
     * @param globalPollCount The global poll count value.
     */
    private TransactionalQueue(List<E> source, int globalPollCount, ListIterator<E> sourceIterator, List<E> tail) {
        Utils.checkNull("Source list", source);
        this.sourceQueue = source;
        this.globalPollCount = globalPollCount;
        this.sourceIterator = sourceIterator;
        this.tail = tail;
    }

    //
    // SemiPersistent class methods.
    //

    @Override
    protected TransactionalQueue<E> cleanCopy() {
        return new TransactionalQueue<E>(this.sourceQueue, this.globalPollCount, sourceQueue.listIterator(),
                new LinkedList<E>());
    }

    @Override
    protected TransactionalQueue<E> dirtyCopy() {
        TransactionalQueue<E> copy = new TransactionalQueue<E>(this.sourceQueue, this.globalPollCount, this.sourceQueue
                .listIterator(Math.min(sourceQueue.size(), pollCount)), new LinkedList<E>(this.tail));
        copy.peekCount = this.peekCount;
        copy.pollCount = this.pollCount;
        return copy;
    }

    @Override
    protected void update(TransactionalQueue<E> changes, boolean onlyReadLogs) {
        Utils.checkNull("Local changes", changes);
        if (this.sourceQueue != changes.sourceQueue) {
            throw new IllegalArgumentException("Updates are only possible for collections with the same source");
        }
        this.updatePeekCount(changes.peekCount);
        if (!onlyReadLogs) {
            this.sourceIterator = changes.sourceIterator;
            this.tail = changes.tail;
            this.pollCount = changes.pollCount;
        }
    }

    /**
     * For performance purposes returned global state is initialized with empty iterator, therefore polling data from
     * the collection is not possible. As it should never be done to the global state, it is safe to do that. This
     * however does not affect clean or dirty copies.
     */
    @Override
    @SuppressWarnings("unchecked")
    public TransactionalQueue<E> commit(TransactionalQueue<E> globalState) {
        Utils.checkNull("Global state", globalState);
        if (0 != globalState.peekCount || !globalState.tail.isEmpty()) {
            throw new IllegalArgumentException("Global state map must be commited before calling this method");
        }

        // If not accessed, return the global state.
        if (this.peekCount == 0 && tail.isEmpty()) {
            return globalState;
        }

        int sourceSize = this.sourceQueue.size();
        if (this.peekCount != 0) {
            // There were readings, check for the conflicts.
            if (globalState.globalPollCount != this.globalPollCount) {
                // We were reading from the head, and it is different the global state.
                throw new ConflictException("Queue's head was updated");
            }

            boolean pastSource = this.peekCount > sourceSize;
            if (pastSource && globalState.sourceQueue != this.sourceQueue) {
                // We read past the source queue length, into the tail, while the global state had been extended with
                // more elements on the tail. (difference of references in this case means exactly that)
                throw new ConflictException("Reading past source queue with tail concurrently updated");
            }
        }

        if (this.pollCount == 0 && tail.isEmpty()) {
            return globalState;
        } else {
            // Remove the polled items from the head.
            int globalSize = globalState.sourceQueue.size();
            LinkedList<E> resultingList = new LinkedList<E>(globalState.sourceQueue.subList(Math.min(this.pollCount,
                    globalSize), globalSize));
            // Append the new items to the tail.
            resultingList.addAll(this.tail);
            // Increase the number of polled items with the local value.
            @SuppressWarnings("rawtypes")
			TransactionalQueue<E> result = new TransactionalQueue<E>(resultingList, globalState.globalPollCount
                    + this.pollCount, (ListIterator) EMPTY_ITERATOR, Collections.EMPTY_LIST);
            return result;
        }
    }

    /**
     * Chooses maximum value between current peek counter and the argument and assigns it to the peek counter.
     * 
     * @param newValue The proposed new value of the peek counter.
     */
    private void updatePeekCount(int newValue) {
        this.peekCount = Math.max(newValue, this.peekCount);
    }

    //
    // TransactionalQueue specific methods.
    //

    /**
     * Polls all the elements from the queue and adds them to the collection.
     * 
     * @param collection The collection to be populated with elements from the queue.
     */
    public void drainTo(Collection<E> collection) {
        Utils.checkNull("Receiving collection", collection);
        for (E elem = this.poll(); elem != null; elem = this.poll()) {
            collection.add(elem);
        }
    }

    //
    // Queue interface methods.
    //

    @Override
    public boolean offer(E e) {
        Utils.checkNull("Element", e);
        return this.tail.add(e);
    }

    @Override
    public E peek() {
        int newPeekCount = this.pollCount;
        E result;
        if (this.sourceIterator.hasNext()) {
            result = this.sourceIterator.next();
            this.sourceIterator.previous(); // return back to simulate peeking
            newPeekCount++;
        } else {
            int tailSize = this.tail.size();
            if (tailSize > 0) {
                newPeekCount++;
                result = this.tail.get(0);
            } else {
                // Not increasing peek count as this is a special case to reduce number of conflicts
                result = null;
            }
        }
        this.updatePeekCount(newPeekCount);
        return result;
    }

    @Override
    public E poll() {
        E result;
        if (this.sourceIterator.hasNext()) {
            this.pollCount++;
            result = this.sourceIterator.next();
        } else {
            int tailSize = this.tail.size();
            if (tailSize > 0) {
                this.pollCount++;
                result = this.tail.remove(0);
            } else {
                result = null;
            }
        }
        this.updatePeekCount(this.pollCount);
        return result;
    }

    @Override
    public boolean isEmpty() {
        return null == peek();
    }

    @Override
    public int size() {
        // Calling isEmpty to mark next item as accessed
        if (this.isEmpty()) {
            return 0;
        }
        int sourceSize = this.sourceIterator.hasNext() ? sourceQueue.size() - pollCount : 0;
        return sourceSize + tail.size();
    }

    //
    // Delegating methods
    //

    @Override
    public boolean add(E e) {
        return offer(e);
    }

    @Override
    public E element() {
        E x = peek();
        if (x != null) {
            return x;
        } else {
            throw new NoSuchElementException();
        }
    }

    @Override
    public E remove() {
        E x = poll();
        if (x != null) {
            return x;
        } else {
            throw new NoSuchElementException();
        }
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        Utils.checkNull("Collection being added", c);
        if (c == this) {
            throw new IllegalArgumentException("Cannot add to itself");
        }
        Iterator<? extends E> e = c.iterator();
        boolean modified = e.hasNext();
        while (e.hasNext()) {
            add(e.next());
        }
        return modified;
    }

    @Override
    public void clear() {
        while (poll() != null) {
            // DO NOTHING...
        }
    }

    //
    // Unsupported operations
    //

    /**
     * This operation is not supported.
     * 
     * @throws UnsupportedOperationException immediately on invocation.
     */
    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported.
     * 
     * @throws UnsupportedOperationException immediately on invocation.
     */
    @Override
    public boolean contains(Object o) {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported.
     * 
     * @throws UnsupportedOperationException immediately on invocation.
     */
    @Override
    public Iterator<E> iterator() {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported.
     * 
     * @throws UnsupportedOperationException immediately on invocation.
     */
    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported.
     * 
     * @throws UnsupportedOperationException immediately on invocation.
     */
    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported.
     * 
     * @throws UnsupportedOperationException immediately on invocation.
     */
    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported.
     * 
     * @throws UnsupportedOperationException immediately on invocation.
     */
    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported.
     * 
     * @throws UnsupportedOperationException immediately on invocation.
     */
    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException();
    }
}
