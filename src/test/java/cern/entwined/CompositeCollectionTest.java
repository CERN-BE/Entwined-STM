/*
 * Entwined STM
 * 
 * © Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.entwined;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cern.entwined.exception.ConflictException;

/**
 * Unit tests for {@link CompositeCollection} class.
 * 
 * @author Ivan Koblik
 */
@SuppressWarnings( { "unchecked", "rawtypes" })
public class CompositeCollectionTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testCompositeCollection() {
        new CompositeCollection(new TransactionalRef<Integer>());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCompositeCollection_failNullArgument() {
        new CompositeCollection(new TransactionalRef<Integer>(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCompositeCollection_failNoArguments() {
        new CompositeCollection();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCompositeCollection_failNullArray() {
        new CompositeCollection((SemiPersistent[]) null);
    }

    @Test
    public void testCleanCopy_delegatesForEveryItem() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(10);
        TransactionalRef<Integer> ref2 = new TransactionalRef<Integer>(100);

        CompositeCollection ccollection = new CompositeCollection(ref, ref2);
        ((TransactionalRef<Integer>) ccollection.get(0)).assoc(100);
        ((TransactionalRef<Integer>) ccollection.get(1)).assoc(1000);

        CompositeCollection copy = ccollection.cleanCopy();

        assertEquals("Original reference", (Integer) 100, ((TransactionalRef<Integer>) ccollection.get(0)).deref());
        assertEquals("Original reference", (Integer) 1000, ((TransactionalRef<Integer>) ccollection.get(1)).deref());

        assertEquals("Copied reference", (Integer) 10, ((TransactionalRef<Integer>) copy.get(0)).deref());
        assertEquals("Copied reference", (Integer) 100, ((TransactionalRef<Integer>) copy.get(1)).deref());
    }

    @Test
    public void testCleanCopy_doesntShareSameCollections() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(10);
        CompositeCollection ccollection = new CompositeCollection(ref);
        assertNotSame(ccollection.get(0), ccollection.cleanCopy().get(0));
    }

    @Test
    public void testCleanCopy_discardsLogs() {
        SemiPersistent<SemiPersistent> ref11 = mock(SemiPersistent.class);
        SemiPersistent<SemiPersistent> ref12 = mock(SemiPersistent.class);
        when(ref11.cleanCopy()).thenReturn(ref11);
        when(ref12.cleanCopy()).thenReturn(ref12);
        CompositeCollection ccollection = new CompositeCollection(ref11, ref12);

        // Access first item
        ccollection.get(0);

        // Clean copy of the original collection
        CompositeCollection ccollection2 = ccollection.cleanCopy();

        SemiPersistent<SemiPersistent> ref21 = mock(SemiPersistent.class);
        SemiPersistent<SemiPersistent> ref22 = mock(SemiPersistent.class);
        CompositeCollection globalState = new CompositeCollection(ref21, ref22);

        // Commit shouldn't be called on any not previously accessed item.
        ccollection2.commit(globalState);
        // Doesn't call cleanCopy on ccollection2 items as no items were accessed.
        verifyNoMoreInteractions(ref11, ref12, ref21, ref22);
    }

    @Test
    public void testDirtyCopy_delegatesForEveryItem() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(10);
        TransactionalRef<Integer> ref2 = new TransactionalRef<Integer>(100);

        CompositeCollection ccollection = new CompositeCollection(ref, ref2);
        ((TransactionalRef<Integer>) ccollection.get(0)).assoc(100);
        ((TransactionalRef<Integer>) ccollection.get(1)).assoc(1000);

        CompositeCollection copy = ccollection.dirtyCopy();

        assertEquals("Original reference", (Integer) 100, ((TransactionalRef<Integer>) ccollection.get(0)).deref());
        assertEquals("Original reference", (Integer) 1000, ((TransactionalRef<Integer>) ccollection.get(1)).deref());

        assertEquals("Copied reference", (Integer) 100, ((TransactionalRef<Integer>) copy.get(0)).deref());
        assertEquals("Copied reference", (Integer) 1000, ((TransactionalRef<Integer>) copy.get(1)).deref());
    }

    @Test
    public void testDirtyCopy_doesntShareSameCollections() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(10);
        CompositeCollection ccollection = new CompositeCollection(ref);
        assertNotSame(ccollection.get(0), ccollection.dirtyCopy().get(0));
    }

    @Test
    public void testDirtyCopy_keepLogs() {
        SemiPersistent<SemiPersistent> ref11 = mock(SemiPersistent.class);
        SemiPersistent<SemiPersistent> ref12 = mock(SemiPersistent.class);
        when(ref11.dirtyCopy()).thenReturn(ref11);
        when(ref12.dirtyCopy()).thenReturn(ref12);
        CompositeCollection ccollection = new CompositeCollection(ref11, ref12);

        // Access first item
        ccollection.get(0);

        // Clean copy of the original collection
        CompositeCollection ccollection2 = ccollection.dirtyCopy();

        SemiPersistent<SemiPersistent> ref21 = mock(SemiPersistent.class);
        SemiPersistent<SemiPersistent> ref22 = mock(SemiPersistent.class);
        CompositeCollection globalState = new CompositeCollection(ref21, ref22);

        // Commit should be only called on previously accessed items.
        ccollection2.commit(globalState);
        // Calls dirtyCopy on ref11 because it was accessed in ccollection.
        verify(ref11, times(1)).dirtyCopy();
        verify(ref11, times(1)).commit(ref21);
        verifyNoMoreInteractions(ref11, ref12, ref21, ref22);
    }

    @Test
    public void testDirtyCopy_picksUpDirtyVersion() {
        SemiPersistent<SemiPersistent> ref11 = mock(SemiPersistent.class);
        SemiPersistent<SemiPersistent> ref12 = mock(SemiPersistent.class);
        when(ref11.dirtyCopy()).thenReturn(ref11);
        when(ref12.dirtyCopy()).thenReturn(ref12);
        CompositeCollection ccollection = new CompositeCollection(ref11, ref12);

        // Access first item
        ccollection.get(0);

        // Clean copy of the original collection
        CompositeCollection ccollection2 = ccollection.dirtyCopy();

        SemiPersistent<SemiPersistent> ref21 = mock(SemiPersistent.class);
        SemiPersistent<SemiPersistent> ref22 = mock(SemiPersistent.class);
        CompositeCollection globalState = new CompositeCollection(ref21, ref22);

        // Commit should be only called on previously accessed items.
        ccollection2.commit(globalState);
        // Calls dirtyCopy on ref11 because it was accessed in ccollection.
        verify(ref11, times(1)).dirtyCopy();
        verify(ref11, times(1)).commit(ref21);
        verifyNoMoreInteractions(ref11, ref12, ref21, ref22);
    }

    @Test
    public void testUpdateCompositeCollectionFalse_delegatesForEveryItem() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(10);
        TransactionalRef<Integer> ref2 = new TransactionalRef<Integer>(100);

        CompositeCollection ccollection = new CompositeCollection(ref, ref2);
        CompositeCollection copy = ccollection.dirtyCopy();
        ((TransactionalRef<Integer>) copy.get(0)).assoc(100);
        ((TransactionalRef<Integer>) copy.get(1)).assoc(1000);

        ccollection.update(copy, false);
        assertEquals("Updated reference", (Integer) 100, ((TransactionalRef<Integer>) ccollection.get(0)).deref());
        assertEquals("Updated reference", (Integer) 1000, ((TransactionalRef<Integer>) ccollection.get(1)).deref());
    }

    @Test(expected = ConflictException.class)
    public void testUpdateCompositeCollectionTrue_delegatesForEveryItem() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(10);
        TransactionalRef<Integer> ref2 = new TransactionalRef<Integer>(100);

        CompositeCollection ccollection = new CompositeCollection(ref, ref2);
        CompositeCollection copy = ccollection.dirtyCopy();
        ((TransactionalRef<Integer>) copy.get(0)).deref();
        ((TransactionalRef<Integer>) copy.get(1)).deref();

        TransactionalRef<Integer> gRef = new TransactionalRef<Integer>(11);
        TransactionalRef<Integer> gRef2 = new TransactionalRef<Integer>(101);
        CompositeCollection globalState = new CompositeCollection(gRef, gRef2);

        ccollection.update(copy, true);
        ccollection.commit(globalState);
    }

    @Test
    public void testUpdateCompositeCollectionFalseAndTrue_onlyAccessedReferences() {
        SemiPersistent<SemiPersistent> ref11 = mock(SemiPersistent.class);
        SemiPersistent<SemiPersistent> ref12 = mock(SemiPersistent.class);
        CompositeCollection ccollection = new CompositeCollection(ref11, ref12);

        SemiPersistent<SemiPersistent> ref21 = mock(SemiPersistent.class);
        SemiPersistent<SemiPersistent> ref22 = mock(SemiPersistent.class);
        CompositeCollection updatedState = new CompositeCollection(ref21, ref22);

        // Only first accessed/updated
        updatedState.get(0);

        ccollection.update(updatedState, false);
        ccollection.update(updatedState, true);

        assertSame("Updated reference", ref11, ccollection.get(0));
        assertSame("Updated reference", ref12, ccollection.get(1));

        verify(ref11, times(1)).update(ref21, false);
        verify(ref11, times(1)).update(ref21, true);
        verifyNoMoreInteractions(ref11, ref12, ref21, ref22);
    }

    @Test
    public void testUpdateCompositeCollection_alwaysUsesAccessors() {
        TransactionalRef<Integer> ref1 = mock(TransactionalRef.class);
        TransactionalRef<Integer> ref2 = spy(new TransactionalRef<Integer>(2));
        TransactionalRef<Integer> ref12 = mock(TransactionalRef.class), ref13 = mock(TransactionalRef.class);
        when(ref1.cleanCopy()).thenReturn(ref12);
        when(ref1.dirtyCopy()).thenReturn(ref13);
        CompositeCollection ccollection = new CompositeCollection(ref1, ref2);
        // Access it.
        ccollection.get(0);

        // Nothing was done to it.
        verifyZeroInteractions(ref1, ref2);

        // Polluting current collection.
        CompositeCollection dirtyCopy = ccollection.dirtyCopy();
        verify(ref1, times(1)).dirtyCopy();
        verifyZeroInteractions(ref2);
        ccollection.<TransactionalRef<Integer>> get(0).deref();

        CompositeCollection global = ccollection.cleanCopy();
        global.update(dirtyCopy, false);
        // Accessor was used for the both sides.
        verify(ref12, times(1)).update(ref13, false);

        // ref2 has never been touched.
        verifyZeroInteractions(ref2);
    }

    @Test
    public void testUpdateCompositeCollection_useAccessorForLocalReferences() {
        TransactionalRef<Integer> ref1 = spy(new TransactionalRef<Integer>(1));
        TransactionalRef<Integer> ref2 = spy(new TransactionalRef<Integer>(2));

        CompositeCollection global = new CompositeCollection(ref1, ref2);
        CompositeCollection data = global.cleanCopy();
        CompositeCollection dataCopy = data.dirtyCopy();

        dataCopy.<TransactionalRef<Integer>> get(1).deref();
        data.update(dataCopy, true);
        data.update(dataCopy, false);
        // Global should not be tainted
        data.commit(global);

        verify(ref1, times(0)).deref();
        verify(ref2, times(0)).deref();
    }

    @Test
    public void testCommit() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(0);
        CompositeCollection ccollection = new CompositeCollection(ref);

        TransactionalRef<Integer> ref2 = new TransactionalRef<Integer>(10);
        CompositeCollection globalState = new CompositeCollection(ref2);

        CompositeCollection committed = ccollection.commit(globalState);

        assertEquals("Committed reference", (Integer) 10, ((TransactionalRef<Integer>) committed.get(0)).deref());
    }

    @Test
    public void testCommit_sameAsGlobalAsNotAccessed() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(0);
        CompositeCollection ccollection = new CompositeCollection(ref);

        TransactionalRef<Integer> ref2 = new TransactionalRef<Integer>(10);
        CompositeCollection globalState = new CompositeCollection(ref2);

        CompositeCollection committed = ccollection.commit(globalState);

        assertSame("The global state unchanged", globalState, committed);
    }

    @Test
    public void testCommit_commitOnlyAccessedReferences() {
        SemiPersistent<SemiPersistent> ref11 = mock(SemiPersistent.class);
        SemiPersistent<SemiPersistent> ref12 = mock(SemiPersistent.class);
        CompositeCollection ccollection = new CompositeCollection(ref11, ref12);

        // Only first accessed
        ccollection.get(0);

        SemiPersistent<SemiPersistent> ref21 = mock(SemiPersistent.class);
        SemiPersistent<SemiPersistent> ref22 = mock(SemiPersistent.class);
        CompositeCollection globalState = new CompositeCollection(ref21, ref22);

        when(ref11.commit(ref21)).thenReturn(ref21);
        CompositeCollection committed = ccollection.commit(globalState);

        assertSame("Committed reference", ref21, committed.get(0));
        assertSame("Committed reference", ref22, committed.get(1));
        verify(ref11, times(1)).commit(ref21);
        verifyNoMoreInteractions(ref11, ref12, ref21, ref22);
    }

    @Test(expected = ConflictException.class)
    public void testCommit_conflict() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(10);
        CompositeCollection ccollection = new CompositeCollection(ref);

        TransactionalRef<Integer> ref2 = new TransactionalRef<Integer>(0);
        CompositeCollection globalState = new CompositeCollection(ref2);

        ((TransactionalRef<Integer>) ccollection.get(0)).deref();
        ccollection.commit(globalState);
    }

    @Test
    public void testCommit_useAccessorForGlobalReferences() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(10);
        CompositeCollection ccollection = new CompositeCollection(ref);
        // Polluting current collection.
        ((TransactionalRef<Integer>) ccollection.get(0)).assoc(11);

        CompositeCollection global = ccollection.cleanCopy();
        // Doesn't fail as global has got a clean copy.
        CompositeCollection result = ccollection.commit(global);
        assertEquals((Integer) 10, ((TransactionalRef<Integer>) global.get(0)).deref());
        assertEquals((Integer) 11, ((TransactionalRef<Integer>) result.get(0)).deref());
    }

    @Test
    public void testGet_numberingStartsFromZero() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(0);
        TransactionalRef<Integer> ref2 = new TransactionalRef<Integer>(1);
        CompositeCollection ccollection = new CompositeCollection(ref, ref2);

        assertEquals("Zero position element", (Integer) 0, ((TransactionalRef<Integer>) ccollection.get(0)).deref());
        assertEquals("First elemnt", (Integer) 1, ((TransactionalRef<Integer>) ccollection.get(1)).deref());
    }

    @Test
    public void testGet_exposesInteralCollections() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(0);
        CompositeCollection ccollection = new CompositeCollection(ref);

        ((TransactionalRef<Integer>) ccollection.get(0)).assoc(100);
        assertEquals("Updated reference", (Integer) 100, ((TransactionalRef<Integer>) ccollection.get(0)).deref());
    }

    @Test
    public void testGet_applyAccessorStrategyOnlyOnce() {
        TransactionalRef<Integer> ref = mock(TransactionalRef.class);
        when(ref.cleanCopy()).thenReturn(ref);
        when(ref.dirtyCopy()).thenReturn(ref);
        CompositeCollection ccollection = new CompositeCollection(ref);

        CompositeCollection cleanCopy = ccollection.cleanCopy();
        CompositeCollection dirtyCopy = ccollection.dirtyCopy();

        verifyZeroInteractions(ref);

        cleanCopy.get(0);
        verify(ref, times(1)).cleanCopy();
        verifyNoMoreInteractions(ref);

        dirtyCopy.get(0);
        verify(ref, times(1)).dirtyCopy();
        verifyNoMoreInteractions(ref);

        // Second access to the element is direct no matter what strategy was used.
        cleanCopy.get(0);
        dirtyCopy.get(0);
        verifyNoMoreInteractions(ref);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGet_failOutOfRange() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(0);
        CompositeCollection ccollection = new CompositeCollection(ref);
        ccollection.get(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGet_failNegativeIndex() {
        TransactionalRef<Integer> ref = new TransactionalRef<Integer>(0);
        CompositeCollection ccollection = new CompositeCollection(ref);
        ccollection.get(-1);
    }
}
