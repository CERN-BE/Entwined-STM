/*
 * Entwined STM
 * 
 * © Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.entwined;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cern.entwined.exception.NoTransactionException;

/**
 * Global reference unit tests.
 * 
 * @author Ivan Koblik
 */
public class GlobalReferenceTest {

    Memory<TestSnapshot> memory;

    @Before
    public void setUp() throws Exception {
        memory = new Memory<TestSnapshot>(new TestSnapshot());
    }

    @After
    public void tearDown() throws Exception {
        memory = null;
    }

    @Test
    public void testFinalize_removesValueFromGlobalMapWhenGCollected() {
        GlobalReference<Long> globalLong = new GlobalReference<Long>(memory, 10l);
        final Long refId = globalLong.getId();
        System.gc();
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                assertTrue("Registered reference", memory.getBaseSnapshot().getGlobalMap().containsKey(refId));
                return true;
            }
        });

        // Removing the only existing reference to the GlobalReference
        globalLong = null;
        final boolean[] flag = { false };
        for (int i = 0; i < 10 && flag[0] == false; i++) {
            System.gc();
            MemoryTest.sleep(500);
            memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
                @Override
                public boolean run(TestSnapshot snapshot) {
                    flag[0] = !memory.getBaseSnapshot().getGlobalMap().containsKey(refId);
                    return true;
                }
            });
        }
        assertTrue("Reference garbage collected", flag[0]);
    }

    /**
     * This test will lead to call to finalize method on a partially initialized {@link GlobalReference}, in particular
     * with memory reference set to null. Unfortunately there is no way to check if it was called or not.
     */
    @Test
    public void testFinalize_partiallyInitialized() {
        final Throwable[] throwable = new Throwable[] { null };
        final boolean[] finalized = new boolean[] { false };
        try {
            // This class is used to track invocations to finalize method.
            class FinalizeTest extends GlobalReference<Long> {
                public FinalizeTest() {
                    super(null, 10l);
                }

                @Override
                protected void finalize() throws Throwable {
                    try {
                        super.finalize();
                        finalized[0] = true;
                    } catch (Throwable th) {
                        throwable[0] = th;
                    }
                }
            }
            new FinalizeTest();
        } catch (IllegalArgumentException e) {
            // skipping the provoked exception
        }

        // Making sure finalize is invoked.
        System.gc();
        for (int i = 0; i < 10; i++) {
            if (finalized[0] || null != throwable[0]) {
                break;
            }
            System.gc();
            MemoryTest.sleep(500);
        }

        // Checking what happened
        if (null != throwable[0]) {
            fail("finalize method thrown an exception " + throwable[0].getMessage());
        } else if (!finalized[0]) {
            fail("finalize method wasn't called");
        }
    }

    @Test
    public void testGlobalReference_outsideTransaction() {
        final GlobalReference<Long> globalLong = new GlobalReference<Long>(memory, 101l);
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public void committed(TestSnapshot snapshot) {
                assertEquals("Expected the value passed to constructor", (Long) 101l, globalLong.get());
            }
        });
    }

    @Test
    public void testGlobalReference_outsideTransaction_nullAllowed() {
        final GlobalReference<Long> globalLong = new GlobalReference<Long>(memory, null);
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public void committed(TestSnapshot snapshot) {
                assertEquals("Expected the value passed to constructor", null, globalLong.get());
            }
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGlobalReference_withinTransaction() {
        final GlobalReference<Long>[] globalLong = new GlobalReference[1];
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) throws Exception {
                globalLong[0] = new GlobalReference<Long>(memory, 101l);
                super.run(snapshot);
                return true;
            }

            @Override
            public void committed(TestSnapshot snapshot) {
                assertEquals("Expected the value passed to constructor", (Long) 101l, globalLong[0].get());
            }
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGlobalReference_failNullMemory() {
        new GlobalReference<Long>(null, 101l);
    }

    @Test
    public void testGet_behavesInTransactionalFashion() {
        final GlobalReference<Long> globalLong = new GlobalReference<Long>(memory, 101l);

        final Thread conflictingThread = MemoryTest.concurrentUpdate(memory, new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                globalLong.set(1l);
                return true;
            }
        });

        final int[] counter = { 0 };
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                if (counter[0]++ == 1) {
                    return true;
                }

                conflictingThread.start();
                MemoryTest.joinThread(conflictingThread);

                globalLong.get();
                return true;
            }

            @Override
            public void committed(TestSnapshot snapshot) {
                assertEquals("Newly set value", (Long) 1l, globalLong.get());
            }
        });

        assertEquals("Due to conflict transaction must have been re-run", 2, counter[0]);
    }

    @Test(expected = NoTransactionException.class)
    public void testGet_fail_outsideTransaction() {
        final GlobalReference<Long> globalLong = new GlobalReference<Long>(memory, 101l);
        globalLong.get();
    }

    @Test
    public void testSet_updatesTheValue() {
        final GlobalReference<Long> globalLong = new GlobalReference<Long>(memory, 101l);
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                globalLong.set(1l);
                return true;
            }

            @Override
            public void committed(TestSnapshot snapshot) {
                assertEquals("Newly set value", (Long) 1l, globalLong.get());
            }
        });
    }

    @Test
    public void testSet_acceptsNull() {
        final GlobalReference<Long> globalLong = new GlobalReference<Long>(memory, 101l);
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                globalLong.set(null);
                return true;
            }

            @Override
            public void committed(TestSnapshot snapshot) {
                assertEquals("Newly set null as value", (Long) null, globalLong.get());
            }
        });
    }

    @Test
    public void testSet_behavesInTransactionalFashion() {
        final GlobalReference<Long> globalLong = new GlobalReference<Long>(memory, 101l);

        final Thread conflictingThread = MemoryTest.concurrentUpdate(memory, new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                globalLong.set(1l);
                return true;
            }
        });

        final int[] counter = { 0 };
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                if (counter[0]++ == 1) {
                    return true;
                }

                conflictingThread.start();
                MemoryTest.joinThread(conflictingThread);

                globalLong.set(2l);
                return true;
            }

            @Override
            public void committed(TestSnapshot snapshot) {
                assertEquals("Newly set value", (Long) 1l, globalLong.get());
            }
        });

        assertEquals("Due to conflict transaction must have been re-run", 2, counter[0]);
    }

    @Test(expected = NoTransactionException.class)
    public void testSet_fail_outsideTransaction() {
        final GlobalReference<Long> globalLong = new GlobalReference<Long>(memory, 101l);
        globalLong.set(1l);
    }
}
