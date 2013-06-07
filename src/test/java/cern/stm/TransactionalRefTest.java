/*
 * Entwined STM
 * 
 * © Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.stm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import cern.stm.TransactionalRef;
import cern.stm.exception.ConflictException;

/**
 * Unit tests of {@link TransactionalRef} class.
 * 
 * @author Ivan Koblik
 */
public class TransactionalRefTest {

    @Test
    public void testTransactionalRef_DefaultValue() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>();
        assertEquals("Uninitialized reference value", null, ref.deref());
    }

    @Test
    public void testTransactionalRefT_NonNullInitialValue() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(10);
        assertEquals("Initial value", (Integer) 10, ref.deref());
    }

    @Test
    public void testTransactionalRefT_NullInitialValue() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(null);
        assertEquals("Initial value", null, ref.deref());
    }

    @Test
    public void testAssociate_ReplaceInitialNullValue() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>();
        Integer replaced = ref.assoc(100);
        assertEquals("Replaced value", (Integer) null, replaced);
        assertEquals("Initial value", (Integer) 100, ref.deref());
    }

    @Test
    public void testAssociate_ReplaceInitialNonNullValue() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(10);
        Integer replaced = ref.assoc(100);
        assertEquals("Replaced value", (Integer) 10, replaced);
        assertEquals("Initial value", (Integer) 100, ref.deref());
    }

    @Test
    public void testAssociate_ReplaceInitialWithNullValue() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(10);
        Integer replaced = ref.assoc(null);
        assertEquals("Replaced value", (Integer) 10, replaced);
        assertEquals("Initial value", (Integer) null, ref.deref());
    }

    @Test
    public void testAssociate_ReplaceLocalNullValue() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(10);
        ref.assoc(null);
        Integer replaced = ref.assoc(100);
        assertEquals("Replaced value", (Integer) null, replaced);
        assertEquals("Initial value", (Integer) 100, ref.deref());
    }

    @Test
    public void testAssociate_ReplaceLocalNonNullValue() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(10);
        ref.assoc(1000);
        Integer replaced = ref.assoc(100);
        assertEquals("Replaced value", (Integer) 1000, replaced);
        assertEquals("Initial value", (Integer) 100, ref.deref());
    }

    @Test
    public void testAssociate_ReplaceLocalWithNullValue() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(10);
        ref.assoc(100);
        Integer replaced = ref.assoc(null);
        assertEquals("Replaced value", (Integer) 100, replaced);
        assertEquals("Initial value", (Integer) null, ref.deref());
    }

    public void testDereference() {
        // Indirectly tested in previous tests.
    }

    // ==================== Clean copy tests ====================

    @Test
    public void testCleanCopy_copiesUnderlyingCollections() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(10);
        TransactionalRef<Integer> copy = ref.cleanCopy();

        Integer sourceValue = copy.assoc(100);
        assertEquals("Source value copied", (Integer) 10, sourceValue);
        assertEquals("Source value in original presorved", (Integer) 10, ref.deref());
    }

    @Test
    public void testCleanCopy_ignoresLocalUpdates() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(10);
        ref.assoc(100);
        TransactionalRef<Integer> copy = ref.cleanCopy();

        assertEquals("Source value copied", (Integer) 10, copy.deref());
    }

    @Test
    public void testCleanCopy_resetsAccessLogs() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(10);
        ref.deref();
        ref.assoc(100);
        TransactionalRef<Integer> copy = ref.cleanCopy();

        TransactionalRef<Integer> globalState = new TransactionalRef<Integer>(100);
        copy.commit(globalState);
    }

    // ==================== Dirty copy/update tests ====================

    @Test
    public void testDirtyCopy_sourceDataCopied() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(10);
        TransactionalRef<Integer> copyRef = ref.dirtyCopy();
        assertEquals("Source data", (Integer) 10, copyRef.deref());
    }

    @Test
    public void testDirtyCopy_modificationCopied() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(10);
        ref.assoc(1);
        TransactionalRef<Integer> copyRef = ref.dirtyCopy();
        assertEquals("Local changes", (Integer) 1, copyRef.deref());
    }

    @Test
    public void testDirtyCopy_sourceValuesReferentiallyEqual() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(10);
        ref.assoc(1);
        TransactionalRef<Integer> copyRef = ref.dirtyCopy();
        // Checks that source values are same.
        ref.update(copyRef, false);
    }

    @Test(expected = ConflictException.class)
    public void testDirtyCopy_accessLogsCopied() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(10);
        ref.deref();
        TransactionalRef<Integer> copyRef = ref.dirtyCopy();

        TransactionalRef<Integer> globalState = new TransactionalRef<Integer>(11);
        copyRef.commit(globalState);
    }

    @Test
    public void testDirtyCopy_changesToOriginalAfterCopyNotVisibleInCopy() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(10);
        TransactionalRef<Integer> copyRef = ref.dirtyCopy();
        ref.deref();
        ref.assoc(100);

        // Global state would be in conflict if copyRef accessed its value
        TransactionalRef<Integer> globalState = new TransactionalRef<Integer>(11);
        copyRef.commit(globalState);

        assertEquals("Source data", (Integer) 10, copyRef.deref());
    }

    @Test
    public void testDirtyCopy_changesToCopyNotVisibleInOriginal() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(10);
        TransactionalRef<Integer> copyRef = ref.dirtyCopy();
        copyRef.deref();
        copyRef.assoc(100);

        // Global state would be in conflict if copyRef accessed its value
        TransactionalRef<Integer> globalState = new TransactionalRef<Integer>(11);
        ref.commit(globalState);

        assertEquals("Source data", (Integer) 10, ref.deref());
    }

    @Test
    public void testUpdate_allChangesPreserved() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(10);
        TransactionalRef<Integer> copyRef = ref.dirtyCopy();
        copyRef.assoc(100);

        ref.update(copyRef, false);
        assertEquals("Updated value", (Integer) 100, ref.deref());
    }

    @Test(expected = ConflictException.class)
    public void testUpdate_accessLogsPreserved() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(10);
        TransactionalRef<Integer> copyRef = ref.dirtyCopy();
        copyRef.deref();

        ref.update(copyRef, false);

        TransactionalRef<Integer> globalState = new TransactionalRef<Integer>(11);
        ref.commit(globalState);
    }

    @Test(expected = ConflictException.class)
    public void testUpdateLogsOnly_accessLogsPreserved() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(10);
        TransactionalRef<Integer> copyRef = ref.dirtyCopy();
        copyRef.deref();

        ref.update(copyRef, true);

        TransactionalRef<Integer> globalState = new TransactionalRef<Integer>(11);
        ref.commit(globalState);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdate_fail_NotWithCopy() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(1000);
        TransactionalRef<Integer> otherRef = new TransactionalRef<Integer>(1000);

        ref.update(otherRef, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateLogsOnly_fail_NotWithCopy() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(1000);
        TransactionalRef<Integer> otherRef = new TransactionalRef<Integer>(1000);

        ref.update(otherRef, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdate_fail_NullArgument() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(10);
        ref.update(null, false);
    }

    // ============== Commit conflicts tests ==============

    @Test(expected = ConflictException.class)
    public void testCommit_LocalAccessed_GlobalUpdated_Conflicting() {
        TransactionalRef<Integer> localState = new TransactionalRef<Integer>(10);
        localState.deref();

        TransactionalRef<Integer> globalState = new TransactionalRef<Integer>(11);
        localState.commit(globalState);
    }

    @Test(expected = ConflictException.class)
    public void testCommit_LocalUpdated_GlobalUpdated_Conflicting() {
        TransactionalRef<Integer> localState = new TransactionalRef<Integer>(10);
        localState.assoc(11); // To enforce test, putting same value as in global

        TransactionalRef<Integer> globalState = new TransactionalRef<Integer>(11);
        localState.commit(globalState);
    }

    @Test(expected = ConflictException.class)
    public void testCommit_LocalChangedBackToOriginal_GlobalUpdated_Conflicting() {
        TransactionalRef<Integer> localState = new TransactionalRef<Integer>(10);
        localState.assoc(100);
        localState.assoc(10);

        TransactionalRef<Integer> globalState = new TransactionalRef<Integer>(11);
        localState.commit(globalState);
    }

    // ============== Commit fail tests ==============

    @Test(expected = IllegalArgumentException.class)
    public void testCommit_fail_GlobalIsTransient() {
        TransactionalRef<Integer> localState = new TransactionalRef<Integer>(10);

        TransactionalRef<Integer> globalState = new TransactionalRef<Integer>(10);
        globalState.deref();
        localState.commit(globalState);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCommit_fail_NullArgument() {
        TransactionalRef<Integer> localState = new TransactionalRef<Integer>(10);
        localState.commit(null);
    }

    // ============== Commit result tests ==============

    @Test
    public void testCommitResult_LocalUpdated() {
        TransactionalRef<Integer> localState = new TransactionalRef<Integer>(10);
        localState.assoc(100);

        TransactionalRef<Integer> globalState = new TransactionalRef<Integer>(10);
        assertEquals("Locally updated value", (Integer) 100, localState.commit(globalState).deref());
    }

    @Test
    public void testCommitResult_LocalRewritten() {
        TransactionalRef<Integer> localState = new TransactionalRef<Integer>(10);
        localState.assoc(10);

        TransactionalRef<Integer> globalState = new TransactionalRef<Integer>(10);
        assertEquals("Locally updated value", (Integer) 10, localState.commit(globalState).deref());
    }

    @Test
    public void testCommitResult_LocalNotAccessed_GlobalUpdated() {
        TransactionalRef<Integer> localState = new TransactionalRef<Integer>(10);

        TransactionalRef<Integer> globalState = new TransactionalRef<Integer>(100);
        assertSame("Locally updated value", globalState, localState.commit(globalState));
    }
}
