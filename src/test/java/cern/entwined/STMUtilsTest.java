/*
 * Entwined STM
 * 
 * © Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cern.entwined.exception.MemoryException;

/**
 * Unit tests for {@link STMUtils} class.
 * 
 * @author Ivan Koblik
 */
public class STMUtilsTest {

    Memory<TestSnapshot> memory;
    GlobalReference<Boolean> ref;

    @Before
    public void setUp() throws Exception {
        memory = new Memory<TestSnapshot>(new TestSnapshot());
        ref = null;
    }

    @After
    public void tearDown() throws Exception {
        memory = null;
        ref = null;
    }

    @Test
    public void testReference_createNew() {
        ref = STMUtils.reference(memory, null, false);
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                assertNotNull("Initialized refrence", ref);
                assertEquals("Correct value", false, ref.get());
                return true;
            }
        });
    }

    @Test
    public void testReference_reuseExisting() {
        ref = STMUtils.reference(memory, null, false);
        final GlobalReference<Boolean> oldRef = ref;
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                ref = STMUtils.reference(memory, ref, true);
                return true;
            }

            @Override
            public void committed(TestSnapshot snapshot) {
                assertNotNull("Initialized refrence", ref);
                assertEquals("Correct value", true, ref.get());
                assertSame("Same reference, different value", oldRef, ref);
            }
        });
    }

    @Test
    public void testReference_acceptNullValue() {
        ref = STMUtils.reference(memory, ref, null);
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                assertNull("Null value", ref.get());
                return true;
            }
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReference_failNullMemory() {
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                ref = STMUtils.reference(null, ref, false);
                return true;
            }
        });
    }

    @Test
    public void testDereference_nullIsNull() {
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                assertNull("Null refrence", STMUtils.dereference(null));
                return true;
            }
        });
    }

    @Test
    public void testDereference_refToNull() {
        ref = new GlobalReference<Boolean>(memory, null);
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                assertNull("Refrence to null", STMUtils.dereference(ref));
                return true;
            }
        });
    }

    @Test
    public void testDereference_refToAValue() {
        ref = new GlobalReference<Boolean>(memory, true);
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                assertTrue("Refrence to True", STMUtils.dereference(ref));
                return true;
            }
        });
    }

    @Test
    public void testInTransaction_notInTransaction() {
        assertFalse("Not in a transaction", STMUtils.inTransaction(memory));
    }

    @Test
    public void testInTransaction_inTransaction() {
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot data) {
                assertTrue("In transaction", STMUtils.inTransaction(memory));
                return false;
            }
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInTransaction_fail_nullMemory() {
        STMUtils.inTransaction((Memory<?>) null);
    }

    @Test(expected = MemoryException.class)
    public void testInTransaction_fail_fromCommittedBlock() {
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public void committed(TestSnapshot data) {
                STMUtils.inTransaction(memory);
            }
        });
    }
}
