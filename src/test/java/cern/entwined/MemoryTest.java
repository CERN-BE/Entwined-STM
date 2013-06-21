/*
 * Entwined STM
 * 
 * (c) Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.entwined;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cern.entwined.exception.ConflictException;
import cern.entwined.exception.InvocationException;
import cern.entwined.exception.MemoryException;
import cern.entwined.exception.NoTransactionException;

import com.google.common.collect.Sets;

/**
 * Unit test of {@link Memory} class.
 * 
 * @author Ivan Koblik
 */
public class MemoryTest {

    volatile boolean flag = false;

    volatile boolean innerFlag = false;

    Memory<TestSnapshot> memory;

    @Before
    public void setUp() throws Exception {
        flag = false;
        innerFlag = false;
        memory = null;
    }

    @After
    public void tearDown() throws Exception {
    }

    private <T extends Snapshot<T>> Memory<T> getMemory(T snapshot) {
        return new Memory<T>(snapshot);
    }

    // ==================== Constructor tests ====================

    @Test
    public void testMemory() {
        TestSnapshot initialState = new TestSnapshot();

        initialState.getRef1().assoc(10);
        memory = new Memory<TestSnapshot>(initialState);
        initialState.getRef2().assoc(13);

        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot data) throws Exception {
                assertNull("Initial state has been clean copied", data.getRef1().deref());
                assertNull("Initial state has been clean copied", data.getRef2().deref());
                return true;
            }
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMemory_fail_NullArgument() {
        new Memory<TestSnapshot>((TestSnapshot) null);
    }

    // ==================== Base snapshot tests ====================

    @Test
    public void testBaseSnapshot_inTransactoin() {
        final TestSnapshot testSnapshot = new TestSnapshot();
        memory = getMemory(testSnapshot);
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                assertNotNull("Base snapshot", memory.getBaseSnapshot());
                return true;
            }
        });
    }

    @Test
    public void testBaseSnapshot_inCommittedClbk() {
        final TestSnapshot testSnapshot = new TestSnapshot();
        memory = getMemory(testSnapshot);
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public void committed(TestSnapshot snapshot) {
                assertNotNull("Base snapshot", memory.getBaseSnapshot());
            }
        });
    }

    @Test(expected = NoTransactionException.class)
    public void testBaseSnapshot_fail_outsideTransaction() {
        final TestSnapshot testSnapshot = new TestSnapshot();
        memory = getMemory(testSnapshot);
        memory.getBaseSnapshot();
    }

    @Test
    public void testGetNextId() {
        final TestSnapshot testSnapshot = new TestSnapshot();
        memory = getMemory(testSnapshot);
        Long id = memory.getNextId();
        assertEquals((Long) (id + 1), memory.getNextId());
        assertEquals((Long) (id + 2), memory.getNextId());
    }

    @Test(timeout = 10000)
    public void testGetNextId_inTwoThreads() throws InterruptedException {
        final TestSnapshot testSnapshot = new TestSnapshot();
        memory = getMemory(testSnapshot);

        final ReadWriteLock lock = new ReentrantReadWriteLock();
        class TestThread extends Thread {
            private final Set<Long> set;

            public TestThread(Set<Long> set) {
                this.set = set;
            }

            @Override
            public void run() {
                lock.readLock().lock();
                try {
                    for (int i = 0; i < 1000; i++) {
                        set.add(memory.getNextId());
                    }
                } finally {
                    lock.readLock().unlock();
                }
            }
        }

        Set<Long> res1 = new HashSet<Long>();
        Set<Long> res2 = new HashSet<Long>();
        TestThread th1 = new TestThread(res1);
        TestThread th2 = new TestThread(res2);

        lock.writeLock().lock();
        th1.start();
        th2.start();
        sleep(100);
        lock.writeLock().unlock();
        th1.join();
        th2.join();

        assertTrue("Intersection of two ID sets is empty.", Sets.intersection(res1, res2).isEmpty());
    }

    // ==================== Transaction invocation tests ====================

    @Test(expected = IllegalArgumentException.class)
    public void testRunTransaction_failNullArgument() {
        memory = getMemory(new TestSnapshot());
        memory.runTransaction(null);
    }

    @Test
    public void testRunTransaction_runCalled() {
        memory = getMemory(new TestSnapshot());
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                flag = true;
                return true;
            }
        });
        assertTrue("Run called", flag);
    }

    @Test
    public void testRunTransaction_committedClbkCalled() {
        memory = getMemory(new TestSnapshot());
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public void committed(TestSnapshot snapshot) {
                flag = true;
            }
        });
        assertTrue("Committed called", flag);
    }

    @Test
    public void testRunTransaction_correctOrderOfInvocationOfRunAndCommitted() {
        memory = getMemory(new TestSnapshot());
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                flag = true;
                return true;
            }

            @Override
            public void committed(TestSnapshot snapshot) {
                assertTrue("Run must have been invoked", flag);
            }
        });
    }

    // ==================== Transaction rollback tests ====================

    @Test
    public void testRunTransaction_rollbackPreventsCommittedClbkInvocation() {
        memory = getMemory(new TestSnapshot());
        boolean result = memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                return false;
            }

            @Override
            public void committed(TestSnapshot snapshot) {
                flag = true;
            }
        });
        assertFalse("Invocation must return transaction rollback status", result);
        assertFalse("Committed not called", flag);
    }

    @Test
    public void testRunTransaction_rollbackInInnerTransactionPreventsItsCommittedClbkInvocation() {
        memory = getMemory(new TestSnapshot());
        boolean result = memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                snapshot.getRef1().assoc(12343);
                boolean result = memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
                    @Override
                    public boolean run(TestSnapshot data) {
                        data.getRef1().assoc(100);
                        return false;
                    }

                    @Override
                    public void committed(TestSnapshot snapshot) {
                        innerFlag = true;
                    }
                });
                assertEquals("Value ignored in inner transaction", (Integer) 12343, snapshot.getRef1().deref());
                assertFalse("Invocation must return inner transaction rollback status", result);
                return true;
            }

            @Override
            public void committed(TestSnapshot snapshot) {
                flag = true;
                assertEquals("Value set in outer transaction", (Integer) 12343, snapshot.getRef1().deref());
            }

        });
        assertFalse("Inner committed not called", innerFlag);
        assertTrue("Outer committed called", flag);
        assertTrue("Invocation must return transaction rollback status", result);
    }

    @Test
    // (timeout = 10000)
    public void testRunTransaction_rollbackInInnerTransactionItsReadLogsNotDiscarded() {
        memory = getMemory(new TestSnapshot());

        // The conflicting transaction.
        TestSnapshot testSnapshot = new TestSnapshot();
        final Memory<TestSnapshot> memory = getMemory(testSnapshot);
        testSnapshot.getRef1().assoc(10);
        final Thread conflict = concurrentUpdate(memory, testSnapshot);

        final AtomicInteger restarts = new AtomicInteger(0);

        // Transaction with rolled back internal transaction.
        boolean result = memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                if (restarts.incrementAndGet() > 1) {
                    return true;
                }
                // Inner transaction
                memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
                    @Override
                    public boolean run(TestSnapshot data) {
                        data.getRef1().deref();
                        return false;
                    }
                });
                conflict.start();
                joinThread(conflict);
                return true;
            }

        });
        assertEquals("Restarted once", 2, restarts.get());
        assertTrue("Invocation must return transaction rollback status", result);
    }

    @Test(timeout = 10000)
    public void testRunTransaction_rollbackTransactionIsNeverRestarted() {
        memory = getMemory(new TestSnapshot());
        final AtomicInteger restarts = new AtomicInteger(0);

        // The conflicting transaction.
        TestSnapshot testSnapshot = new TestSnapshot();
        final Memory<TestSnapshot> memory = getMemory(testSnapshot);
        testSnapshot.getRef1().assoc(10);
        final Thread conflict = concurrentUpdate(memory, testSnapshot);

        // Transaction with rolled back internal transaction.
        boolean result = memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot data) {
                assertEquals("Expected only single run", 1, restarts.incrementAndGet());
                data.getRef1().deref();
                conflict.start();
                joinThread(conflict);
                return false;
            }

        });
        assertEquals("Rolled back transactions are not restarted", 1, restarts.get());
        assertFalse("Invocation must return transaction rollback status", result);
    }

    // ==================== Transaction fail tests ====================

    @Test(expected = RuntimeException.class)
    public void testRunTransaction_fail_ExceptionInRunIsPropagated() {
        memory = getMemory(new TestSnapshot());
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                throw new RuntimeException();
            }
        });
    }

    @Test(expected = InvocationException.class)
    public void testRunTransaction_fail_CheckedExceptionInRunIsPropagated() {
        memory = getMemory(new TestSnapshot());
        final Exception exception = new Exception();
        try {
            memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
                @Override
                public boolean run(TestSnapshot snapshot) throws Exception {
                    throw exception;
                }
            });
        } catch (InvocationException ex) {
            assertSame("Wrapped exception", exception, ex.getCause());
            throw ex;
        }
    }

    @Test(expected = RuntimeException.class)
    public void testRunTransaction_fail_ExceptionInCommittedClbkIsPropagated() {
        memory = getMemory(new TestSnapshot());
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public void committed(TestSnapshot snapshot) {
                throw new RuntimeException();
            }
        });
    }

    @Test(expected = InvocationException.class)
    public void testRunTransaction_fail_CheckedExceptionInCommittedClbkIsPropagated() {
        memory = getMemory(new TestSnapshot());
        final Exception exception = new Exception();
        try {
            memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
                @Override
                public void committed(TestSnapshot snapshot) throws Exception {
                    throw exception;
                }
            });
        } catch (InvocationException ex) {
            assertSame("Wrapped exception", exception, ex.getCause());
            throw ex;
        }
    }

    @Test
    public void testRunTransaction_fail_ExceptionInRunPreventsCommittedClbkInvocation() {
        memory = getMemory(new TestSnapshot());
        try {
            memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
                @Override
                public void committed(TestSnapshot snapshot) {
                    flag = true;
                }

                @Override
                public boolean run(TestSnapshot snapshot) {
                    throw new RuntimeException();
                }
            });
        } catch (RuntimeException e) {
        }
        assertFalse("Committed not called", flag);
    }

    // ==================== Global state update tests ====================

    @Test
    public void testRunTransaction_runPassedGlobalState() {
        TestSnapshot testSnapshot = new TestSnapshot();
        testSnapshot.getRef1().assoc(10);
        memory = getMemory(testSnapshot.commit(testSnapshot.cleanCopy()));
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                flag = snapshot.getRef1().deref() == 10;
                return true;
            }
        });
        assertTrue("Run passed global snapshot", flag);
    }

    @Test
    public void testRunTransaction_committedClbkPassedGlobalState() {
        TestSnapshot testSnapshot = new TestSnapshot();
        testSnapshot.getRef1().assoc(10);
        memory = getMemory(testSnapshot.commit(testSnapshot.cleanCopy()));
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public void committed(TestSnapshot snapshot) {
                flag = snapshot.getRef1().deref() == 10;
            }
        });
        assertTrue("Committed passed global snapshot", flag);
    }

    @Test
    public void testRunTransaction_commitUpdatesGlobalState() {
        memory = getMemory(new TestSnapshot());
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                snapshot.getRef1().assoc(10);
                return true;
            }
        });
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                flag = 10 == snapshot.getRef1().deref();
                return true;
            }
        });
        assertTrue("Updated reference", flag);
    }

    @Test
    public void testRunTransaction_committedClbkPassedAnUpdatedSnapshot() {
        memory = getMemory(new TestSnapshot());
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                snapshot.getRef1().assoc(10);
                return true;
            }

            @Override
            public void committed(TestSnapshot snapshot) {
                flag = 10 == snapshot.getRef1().deref();
            }
        });
        assertTrue("Updated reference", flag);
    }

    @Test
    public void testRunTransaction_committedClbkPassedACopyOfGlobalState() {
        memory = getMemory(new TestSnapshot());
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public void committed(TestSnapshot snapshot) {
                snapshot.getRef1().assoc(10);
                snapshot.getRef2().assoc(11);
            }
        });
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                flag = snapshot.getRef1().deref() == null && snapshot.getRef2().deref() == null;
                return true;
            }
        });
        assertTrue("References not updated", flag);
    }

    // ==================== Concurrent updates tests ====================

    @Test
    public void testRunTransaction_simpleConflict() {
        TestSnapshot testSnapshot = new TestSnapshot();
        final Memory<TestSnapshot> memory = getMemory(testSnapshot);

        testSnapshot.getRef1().assoc(10101);
        final Thread thread = concurrentUpdate(memory, testSnapshot);

        final int[] counter = { 0 };

        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                if (counter[0] == 1) {
                    snapshot.getRef1().deref();
                } else {
                    snapshot.getRef1().deref();

                    thread.start();
                    joinThread(thread);
                    counter[0]++;
                }
                return true;
            }
        });
        assertEquals("Transaction must be run twice due to conflicting changes", 1, counter[0]);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = ConflictException.class, timeout = 40000)
    public void testRunTransaction_fail_exhaustedTransactionRuns() {
        final Memory<TestSnapshot> memory = getMemory(new TestSnapshot(10, 10, Collections.EMPTY_MAP));

        final int[] counter = { 0 };

        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                snapshot.getRef1().deref();

                Thread thread = concurrentUpdate(memory, new TransactionAdapter<TestSnapshot>() {
                    @Override
                    public boolean run(TestSnapshot snapshot) {
                        snapshot.getRef1().assoc(snapshot.getRef1().deref() + 1);
                        return true;
                    }
                });
                thread.start();
                joinThread(thread);
                if (counter[0] > Memory.NUM_RETRIES) {
                    fail("More retries than expected");
                }
                counter[0]++;
                return true;
            }
        });
    }

    /**
     * Concurrent changes merged with changes within the transaction.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testRunTransaction_globalStateMerged() {
        final Memory<TestSnapshot> memory = getMemory(new TestSnapshot(10, 10, Collections.EMPTY_MAP));

        final int[] counter = { 0 };

        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                assertEquals("No retries expected", 0, counter[0]);
                snapshot.getRef2().assoc(100);

                Thread thread = concurrentUpdate(memory, new TransactionAdapter<TestSnapshot>() {
                    @Override
                    public boolean run(TestSnapshot snapshot) {
                        snapshot.getRef1().assoc(101);
                        return true;
                    }
                });
                thread.start();
                joinThread(thread);

                counter[0]++;
                return true;
            }
        });

        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                assertEquals("Updated global state", (Integer) 101, snapshot.getRef1().deref());
                assertEquals("Local updates applied to global state", (Integer) 100, snapshot.getRef2().deref());
                return true;
            }
        });
    }

    /**
     * Global state passed to committed is a result of merging transaction's snapshot with the global snapshot.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testRunTransaction_committedClbkPassedMergedGlobalState() {
        final Memory<TestSnapshot> memory = getMemory(new TestSnapshot(10, 10, Collections.EMPTY_MAP));

        final int[] counter = { 0 };

        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                assertEquals("No retries expected", 0, counter[0]);
                snapshot.getRef2().assoc(100);

                Thread thread = concurrentUpdate(memory, new TransactionAdapter<TestSnapshot>() {
                    @Override
                    public boolean run(TestSnapshot snapshot) {
                        snapshot.getRef1().assoc(101);
                        return true;
                    }
                });
                thread.start();
                joinThread(thread);

                counter[0]++;
                return true;
            }

            @Override
            public void committed(TestSnapshot snapshot) {
                assertEquals("Updated global state", (Integer) 101, snapshot.getRef1().deref());
                assertEquals("Local updates applied to global state", (Integer) 100, snapshot.getRef2().deref());
            }
        });
    }

    // ==================== Inner transactions tests ====================
    @Test
    public void testRunTransaction_innerTransactionSeesTransientState() {
        final Memory<TestSnapshot> memory = getMemory(new TestSnapshot());

        // Outer transaction
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                snapshot.getRef1().assoc(10);

                // Inner transaction
                memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
                    @Override
                    public boolean run(TestSnapshot snapshot) {
                        flag = snapshot.getRef1().deref() == 10;
                        return true;
                    }
                });
                return true;
            }
        });
        assertTrue("Inner transaction must be able to see the transient state", flag);
    }

    @Test
    public void testRunTransaction_innerTransactionUpdatesOuterSnapshot() {
        final Memory<TestSnapshot> memory = getMemory(new TestSnapshot());

        // Outer transaction
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                // Inner transaction
                memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
                    @Override
                    public boolean run(TestSnapshot snapshot) {
                        snapshot.getRef1().assoc(10);
                        return true;
                    }
                });
                flag = snapshot.getRef1().deref() == 10;
                return true;
            }
        });
        assertTrue("Inner transaction must be able to update outer transaction state", flag);
    }

    @Test
    public void testRunTransaction_failedInnerTransactionKeepsOuterTrIntact() {
        final Memory<TestSnapshot> memory = getMemory(new TestSnapshot());

        // Outer transaction
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                // Inner transaction
                TransactionAdapter<TestSnapshot> innerTransaction = new TransactionAdapter<TestSnapshot>() {
                    @Override
                    public boolean run(TestSnapshot snapshot) {
                        snapshot.getRef1().assoc(10);
                        throw new RuntimeException();
                    }
                };
                try {
                    memory.runTransaction(innerTransaction);
                } catch (RuntimeException e) {
                    // Ignore the exception
                }
                flag = snapshot.getRef1().deref() == null;
                return true;
            }
        });
        assertTrue("Outer transaction must be intact", flag);
    }

    @Test
    public void testRunTransaction_failedInnerTransactionKeepsLogs() {
        TestSnapshot testSnapshot = new TestSnapshot();
        final Memory<TestSnapshot> memory = getMemory(testSnapshot);

        testSnapshot.getRef1().assoc(10);
        final Thread thread = concurrentUpdate(memory, testSnapshot);
        final int[] counter = { 0 };
        // Outer transaction
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                // If counter[0] is one it means that the transaction is being retried.
                if (counter[0] == 1) {
                    flag = true;
                    return true;
                }
                counter[0]++;

                // Inner transaction

                Transaction<TestSnapshot> innerTransaction = new TransactionAdapter<TestSnapshot>() {
                    @Override
                    public boolean run(TestSnapshot snapshot) {
                        snapshot.getRef1().deref();
                        throw new RuntimeException();
                    }
                };

                try {
                    memory.runTransaction(innerTransaction);
                } catch (RuntimeException e) {
                    // Ignoring the exception
                }

                // Conflicting changes.
                thread.start();
                joinThread(thread);
                return true;
            }
        });
        assertTrue("Failed inner transaction's read logs must be taken into account", flag);
    }

    @Test
    public void testRunTransaction_failedWithCheckedExceptionInnerTransactionKeepsLogs() {
        TestSnapshot testSnapshot = new TestSnapshot();
        final Memory<TestSnapshot> memory = getMemory(testSnapshot);

        testSnapshot.getRef1().assoc(10);
        final Thread thread = concurrentUpdate(memory, testSnapshot);
        final int[] counter = { 0 };
        final Exception exception = new Exception();
        // Outer transaction
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                // If counter[0] is one it means that the transaction is being retried.
                if (counter[0] == 1) {
                    flag = true;
                    return true;
                }
                counter[0]++;

                // Inner transaction

                Transaction<TestSnapshot> innerTransaction = new TransactionAdapter<TestSnapshot>() {
                    @Override
                    public boolean run(TestSnapshot snapshot) throws Exception {
                        snapshot.getRef1().deref();
                        throw exception;
                    }
                };

                try {
                    memory.runTransaction(innerTransaction);
                } catch (InvocationException e) {
                    // Ignoring the exception
                    assertSame(exception, e.getCause());
                }

                // Conflicting changes.
                thread.start();
                joinThread(thread);
                return true;
            }
        });
        assertTrue("Failed inner transaction's read logs must be taken into account", flag);
    }

    @Test
    public void testRunTransaction_innerTransactionLogsTakenIntoAccount() {
        TestSnapshot testSnapshot = new TestSnapshot();
        final Memory<TestSnapshot> memory = getMemory(testSnapshot);

        testSnapshot.getRef1().assoc(10);
        final Thread thread = concurrentUpdate(memory, testSnapshot);
        final int[] counter = { 0 };

        // Outer transaction
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                // If counter[0] is one it means that the transaction is being retried.
                if (counter[0] == 1) {
                    flag = true;
                    return true;
                }
                counter[0]++;

                // Inner transaction
                memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
                    @Override
                    public boolean run(TestSnapshot snapshot) {
                        snapshot.getRef1().deref();
                        return true;
                    }
                });

                // Conflicting changes.
                thread.start();
                joinThread(thread);
                return true;
            }
        });
        assertTrue("Inner transaction's read logs must be taken into account", flag);
    }

    @Test
    public void testRunTransaction_callOrderOfCommittedClbk() {
        final Memory<TestSnapshot> memory = getMemory(new TestSnapshot());
        final int[] counter = { 0 };

        // Outer transaction
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                // Inner transaction
                memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
                    @Override
                    public void committed(TestSnapshot snapshot) {
                        flag = counter[0] == 0;
                        counter[0]++;
                    }
                });
                return true;
            }

            @Override
            public void committed(TestSnapshot snapshot) {
                flag &= counter[0] == 1;
            }
        });
        assertTrue("Inner transaction's commited callback must be called first", flag);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRunTransaction_committedClbkPassedFreshCopiesOfGlobalState() {
        final Memory<TestSnapshot> memory = getMemory(new TestSnapshot(10, 10, Collections.EMPTY_MAP));

        // Outer transaction
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                // Inner transaction
                memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
                    @Override
                    public void committed(TestSnapshot snapshot) {
                        snapshot.getRef1().assoc(10101);
                    }
                });
                return true;
            }

            @Override
            public void committed(TestSnapshot snapshot) {
                flag = snapshot.getRef1().deref() == 10;
            }
        });
        assertTrue("Every commited callbacks are given their own copy of global snapshot", flag);
    }

    @Test
    public void testRunTransaction_callOrderOfCommittedClbkWithFailedInnerTr() {
        final Memory<TestSnapshot> memory = getMemory(new TestSnapshot());
        final int[] counter = { 0 };

        // Outer transaction
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                // Inner transaction
                Transaction<TestSnapshot> testSnapshot = new TransactionAdapter<TestSnapshot>() {
                    @Override
                    public boolean run(TestSnapshot snapshot) {
                        throw new RuntimeException();
                    }

                    @Override
                    public void committed(TestSnapshot snapshot) {
                        counter[0]++; // Musn't be ever called
                    }
                };
                try {
                    memory.runTransaction(testSnapshot);
                } catch (RuntimeException e) {
                    // Ignore exception
                }
                return true;
            }

            @Override
            public void committed(TestSnapshot snapshot) {
                flag = counter[0] == 0;
            }
        });
        assertTrue("Inner failed transaction's commited callback must NOT be called", flag);
    }

    // ==================== Post-transactional callbacks ordering ====================
    /**
     * This test runs one principal transactions that interrupts itself within the transaction and checks in its
     * post-transactional callback that the flag is still set. To be sure that this transaction waits in the queue, test
     * spans number of concurrent 1ms transactions.<br>
     * <b>Note: </b> This test may give false negative (pass on broken code) but will never give false positive.
     */
    @Test(timeout = 60000)
    public void testRunTransaction_interruptedFlagPreserved() {
        final Memory<TestSnapshot> memory = getMemory(new TestSnapshot());
        final AtomicBoolean success = new AtomicBoolean(false);
        final AtomicBoolean executed = new AtomicBoolean(false);

        final Transaction<TestSnapshot> concurrent = new TransactionAdapter<TestSnapshot>() {
            @Override
            public void committed(TestSnapshot snapshot) {
                sleep(1);
            }
        };

        final Transaction<TestSnapshot> interrupted = new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                // Sleeping to let concurrent updates get in the way of this transaction
                sleep(1000);
                Thread.currentThread().interrupt();
                return true;
            }

            @Override
            public void committed(TestSnapshot snapshot) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    // Good interrupted flag preserved
                    success.set(true);
                }
                executed.set(true);
            }
        };

        final int numThreads = 100;
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(10000);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(numThreads, numThreads, 0, TimeUnit.SECONDS, queue);

        // Starting the monitoring transaction
        executor.execute(new Runnable() {
            @Override
            public void run() {
                memory.runTransaction(interrupted);
            }
        });

        while (!executed.get()) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    memory.runTransaction(concurrent);
                }
            });
            if (queue.remainingCapacity() < 1000) {
                sleep(10);
            }
        }
        assertEquals("Interrupted flag wasn't preserved", true, success.get());
    }

    /**
     * This test runs 200 concurrent transactions, every transactional callback uses {@link ReentrantLock} to register
     * itself. Other callbacks check if it is being used which would mean that at least two callbacks overlap.<br>
     * <b>Note: </b> This test may give false negative (pass on broken code) but will never give false positive.
     */
    @Test(timeout = 60000)
    public void testRunTransaction_committedClbksDontOverlap() {
        final Memory<TestSnapshot> memory = getMemory(new TestSnapshot());
        final ReentrantLock lock = new ReentrantLock();
        final AtomicInteger failed = new AtomicInteger(0);
        final Transaction<TestSnapshot> concurrent = new TransactionAdapter<TestSnapshot>() {
            @Override
            public void committed(TestSnapshot snapshot) {
                if (lock.isLocked()) {
                    failed.incrementAndGet();
                }

                lock.lock();

                try {
                    Thread.sleep(2);
                } catch (InterruptedException e) {
                } finally {
                    lock.unlock();
                }
            }
        };
        final int numThreads = 100;
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(10000);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(numThreads, numThreads, 0, TimeUnit.SECONDS, queue);
        final int NUMTIMES = 200;
        for (int i = 0; i < NUMTIMES; i++) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    memory.runTransaction(concurrent);
                }
            });
        }
        while (true) {
            if (executor.getCompletedTaskCount() == NUMTIMES) {
                break;
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }
        }
        assertEquals("Number of overlapping post-transactional callbacks", 0, failed.get());
    }

    /**
     * Attempts to run a transaction within another transaction's committed block, expected to fail with
     * {@link MemoryException}.
     */
    @Test(timeout = 60000, expected = MemoryException.class)
    public void testRunTransaction_committedCantStartTransactionHere() {
        final Memory<TestSnapshot> memory = getMemory(new TestSnapshot());
        final Transaction<TestSnapshot> concurrent = new TransactionAdapter<TestSnapshot>() {
            @Override
            public void committed(TestSnapshot snapshot) {
                memory.runTransaction(new TransactionAdapter<TestSnapshot>());
            }
        };
        memory.runTransaction(concurrent);
    }

    /**
     * This test verifies that transactional callbacks are called in their commit order. Failing test unquestionably
     * proves that something in commit ordering is broken. <br>
     * <b>Note: </b> This test may give false negative (pass on broken code) but will never give false positive.
     */
    @Test(timeout = 60000)
    public void testRunTransaction_crudeCommitOrderTest() {
        final int numThreads = 100;
        final int numTimes = 20;
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(numTimes * numThreads);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(numThreads, numThreads, 0, TimeUnit.SECONDS, queue);
        final List<Transaction<TestSnapshot>> transactions = new ArrayList<Transaction<TestSnapshot>>();
        final AtomicInteger cc = new AtomicInteger(0);

        final AtomicReference<TransactionalMap<Integer, Integer>> previousMap = new AtomicReference<TransactionalMap<Integer, Integer>>();
        final AtomicBoolean failed = new AtomicBoolean(false);

        /*
         * Every transaction updates its own item in the map. Transactional callback checks afterwards that the
         * previously committed snapshot precedes the current one.
         */
        for (int i = 0; i < numThreads; i++) {
            Transaction<TestSnapshot> transaction = new TransactionAdapter<TestSnapshot>() {
                private int nn = cc.getAndIncrement();

                @Override
                public void committed(TestSnapshot snapshot) {
                    TransactionalMap<Integer, Integer> previous = previousMap.get();
                    previousMap.set(snapshot.getMap());
                    if (previous == null) {
                        return;
                    }
                    for (int i = 0; i < numThreads; i++) {
                        if (previous.get(i) > snapshot.getMap().get(i)) {
                            failed.set(true);
                            break;
                        }
                    }
                }

                @Override
                public boolean run(TestSnapshot snapshot) {
                    TransactionalMap<Integer, Integer> vscopes = snapshot.getMap();
                    vscopes.put(nn, vscopes.get(nn) + 1);
                    return true;
                }
            };
            transactions.add(transaction);
        }

        // Initialize shared memory
        final Memory<TestSnapshot> memory = getMemory(new TestSnapshot());
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                for (int i = 0; i < numThreads; i++) {
                    snapshot.getMap().put(i, 0);
                }
                return true;
            }
        });

        /*
         * Executes predefined transactions give number of times for given number of items.
         */
        for (int j = 0; j < numTimes; j++) {
            for (int i = 0; i < numThreads; i++) {
                final int k = i;
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        memory.runTransaction(transactions.get(k));
                    }
                });
            }
        }

        /*
         * Wait for the processing to finish.
         */
        while (true) {
            if (executor.getCompletedTaskCount() == numTimes * numThreads) {
                break;
            }
            sleep(1);
        }

        assertFalse("Precedence is broken, transactional callbacks must be called in synchronous manner", failed.get());
    }

    /**
     * Updates memory global state with the given snapshot.
     * 
     * @param <T> The type of the snapshot.
     * @param memory The reference to the transactional memory.
     * @param updatedSnapshot Updated snapshot, make sure to branch it form current global snapshot or update will fail.
     * @return New dormant thread, use {@link Thread#start()} to run it.
     */
    private <T extends Snapshot<T>> Thread concurrentUpdate(final Memory<T> memory, final T updatedSnapshot) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                memory.runTransaction(new TransactionAdapter<T>() {
                    @Override
                    public boolean run(T snapshot) {
                        try {
                            snapshot.update(updatedSnapshot, false);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return true;
                    };
                });
            }
        });
        return thread;
    }

    /**
     * Runs the given transaction in a concurrent thread.
     * 
     * @param <T> The type of the snapshot.
     * @param memory The reference to the transactional memory.
     * @param transaction The transaction to run.
     * @return New dormant thread, use {@link Thread#start()} to run it.
     */
    public static <T extends Snapshot<T>> Thread concurrentUpdate(final Memory<T> memory,
            final Transaction<T> transaction) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                memory.runTransaction(transaction);
            }
        });
        return thread;
    }

    /**
     * Joins given thread catching all the {@link InterruptedException}s and restarting.
     * 
     * @param thread The thread to join.
     */
    public static void joinThread(Thread thread) {
        while (true) {
            try {
                thread.join();
                break;
            } catch (InterruptedException e) {
                continue;
            }
        }
    }

    /**
     * Delays thread's execution.
     * 
     * @param millis The delay period in milliseconds.
     */
    public static void sleep(long millis) {
        long start = System.currentTimeMillis();
        long end = start;
        while (true) {
            try {
                Thread.sleep(millis - (end - start));
                break;
            } catch (InterruptedException e) {
                end = System.currentTimeMillis();
                continue;
            }
        }
    }
}
