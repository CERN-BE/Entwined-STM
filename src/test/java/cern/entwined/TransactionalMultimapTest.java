/*
 * Entwined STM
 * 
 * © Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.entwined;

import static cern.entwined.test.TestUtils.newMap;
import static cern.entwined.test.TestUtils.newSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cern.entwined.exception.ConflictException;

import com.google.common.collect.ImmutableSet;

/**
 * {@link TransactionalMultimap} unit tests.
 * 
 * @author Ivan Koblik
 */
public class TransactionalMultimapTest {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMultimap#TransactionalMultimap()}.
     */
    @Test
    public void testTransactionalMultimap() {
        new TransactionalMultimap<Integer, Long>();
    }

    // ==================== Local operation tests ====================

    /**
     * Test method for {@link cern.entwined.TransactionalMultimap#get(java.lang.Object)}.
     */
    @Test
    public void testGet_onEmptyMap() {
        TransactionalMultimap<Integer, Long> map = new TransactionalMultimap<Integer, Long>();
        assertEquals("Non existing pair with key=null", ImmutableSet.<Long> of(), map.get(null));
        assertEquals("Non existing pair with key=1", ImmutableSet.<Long> of(), map.get(1));
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMultimap#get(java.lang.Object)}.
     */
    @Test
    public void testGet_onExistingPair() {
        TransactionalMultimap<Integer, Long> map = new TransactionalMultimap<Integer, Long>();

        Set<Long> expected = newSet(1l, 2l, 3l);
        map.put(2, expected);
        assertEquals("Non existing pair with key=1", ImmutableSet.<Long> of(), map.get(1));
        assertEquals("Existing pair with key=2", expected, map.get(2));
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMultimap#get(java.lang.Object)}.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testGet_returnsUnmodifiableSet() {
        TransactionalMultimap<Integer, Long> map = new TransactionalMultimap<Integer, Long>();

        Set<Long> expected = newSet(1l, 2l, 3l);
        map.put(1, expected);
        map.get(1).clear();
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMultimap#put(java.lang.Object, java.util.Set)}.
     */
    @Test
    public void testPutKSetOfV_copySourceAndReturnReplacedSet() {
        TransactionalMultimap<Integer, Long> map = new TransactionalMultimap<Integer, Long>();

        Set<Long> copy = newSet(1l, 2l, 3l);
        assertEquals("Replaced empty set", ImmutableSet.<Long> of(), map.put(1, copy));
        copy.clear();

        Set<Long> expected = newSet(1l, 2l, 3l);
        assertEquals("Stored set unchanged", expected, map.get(1));

        Set<Long> newExpected = newSet(1l);
        assertEquals("Replaced old set", expected, map.put(1, newExpected));
        assertEquals("New set stored", newExpected, map.get(1));
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMultimap#put(java.lang.Object, java.util.Set)}.
     */
    @Test
    public void testPutKSetOfV_emptySet() {
        TransactionalMultimap<Integer, Long> map = new TransactionalMultimap<Integer, Long>();
        map.put(1, ImmutableSet.<Long> of());
        assertFalse("Added empty set is discarded", map.containsKey(1));
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMultimap#put(java.lang.Object, java.util.Set)}.
     */
    @Test
    public void testPutKSetOfV_nullKeyAndOneNullItem() {
        TransactionalMultimap<Integer, Long> map = new TransactionalMultimap<Integer, Long>();

        Set<Long> copy = newSet(1l, 2l, null);
        assertEquals("Replaced empty set", ImmutableSet.<Long> of(), map.put(null, copy));
        Set<Long> expected = newSet(1l, 2l, null);
        assertEquals("Stored value unchanged", expected, map.get(null));
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMultimap#put(java.lang.Object, java.util.Set)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testPutKSetOfV_failNullSet() {
        TransactionalMultimap<Integer, Long> map = new TransactionalMultimap<Integer, Long>();
        map.put(1, (Set<Long>) null);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMultimap#remove(java.lang.Object)}.
     */
    @Test
    public void testRemoveK() {
        TransactionalMultimap<Integer, Long> map = new TransactionalMultimap<Integer, Long>();
        Set<Long> source = newSet(1l, 2l);
        map.put(1, source);
        assertEquals("The removed set", source, map.remove(1));
        assertEquals("Empty set in place of removed pair", ImmutableSet.<Long> of(), map.get(1));
        assertEquals("Empty set for non existing pair", ImmutableSet.<Long> of(), map.remove(null));
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMultimap#clear()}.
     */
    @Test
    public void testClear() {
        TransactionalMultimap<Integer, Long> map = new TransactionalMultimap<Integer, Long>();
        Set<Long> source = newSet(1l, 2l);
        map.put(1, source);
        map.clear();

        assertEquals("Empty set in place of removed pair", ImmutableSet.<Long> of(), map.get(1));
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMultimap#containsKey(java.lang.Object)}.
     */
    @Test
    public void testContainsKey() {
        TransactionalMultimap<Integer, Long> map = new TransactionalMultimap<Integer, Long>();
        Set<Long> source = newSet(1l, 2l);
        map.put(1, source);

        assertFalse("Non existing pair for key=null", map.containsKey(null));
        assertFalse("Non existing pair for key=0", map.containsKey(0));
        assertTrue("Existing pair", map.containsKey(1));
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMultimap#putAll(java.util.Map)}.
     */
    @Test
    public void testPutAllMapOfQextendsKQextendsSetOfV_storesAllPairs_extendsExisitingPairs() {
        TransactionalMultimap<Integer, Long> map = new TransactionalMultimap<Integer, Long>();
        Map<Integer, Set<Long>> source = newMap(//
                null, newSet(4l),//
                1, newSet(1l, 2l), //
                2, newSet(2l, 3l), //
                3, ImmutableSet.<Long> of());
        map.put(2, newSet(5l));
        map.putAll(source);

        assertEquals("Value set for key=null", newSet(4l), map.get(null));
        assertEquals("Value set for key=1", newSet(1l, 2l), map.get(1));
        assertEquals("Extended value set for key=2", newSet(2l, 3l, 5l), map.get(2));
        assertFalse("Pair with empty value set treated as non-existing", map.containsKey(3));
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMultimap#putAll(java.util.Map)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testPutAllMapOfQextendsKQextendsSetOfV_failNullArgument() {
        TransactionalMultimap<Integer, Long> map = new TransactionalMultimap<Integer, Long>();
        map.putAll(null);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMultimap#put(java.lang.Object, java.lang.Object)}.
     */
    @Test
    public void testPutKV() {
        TransactionalMultimap<Integer, Long> map = new TransactionalMultimap<Integer, Long>();
        map.put(1, 2l);

        assertEquals("Single value set for key=1", newSet(2l), map.get(1));

        map.put(1, 3l);
        assertEquals("Extended value set for key=1", newSet(2l, 3l), map.get(1));

        map.put((Integer) null, (Long) null);
        assertEquals("Value set for key=null", newSet((Long) null), map.get(null));
    }

    /**
     * Test method for
     * {@link cern.entwined.TransactionalMultimap#putAll(java.lang.Object, java.util.Collection)}.
     */
    @Test
    public void testPutAllKCollectionOfV() {
        TransactionalMultimap<Integer, Long> map = new TransactionalMultimap<Integer, Long>();
        map.put(2, newSet(5l));
        map.putAll(null, newSet(4l));
        map.putAll(1, newSet(1l, 2l));
        map.putAll(2, newSet(2l, 3l));
        map.putAll(3, ImmutableSet.<Long> of());

        assertEquals("Value set for key=null", newSet(4l), map.get(null));
        assertEquals("Value set for key=1", newSet(1l, 2l), map.get(1));
        assertEquals("Extended value set for key=2", newSet(2l, 3l, 5l), map.get(2));
        assertFalse("Pair with empty value set treated as non-existing", map.containsKey(3));
    }

    /**
     * Test method for
     * {@link cern.entwined.TransactionalMultimap#putAll(java.lang.Object, java.util.Collection)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testPutAllKCollectionOfV_failNullCollection() {
        TransactionalMultimap<Integer, Long> map = new TransactionalMultimap<Integer, Long>();
        map.putAll(1, null);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMultimap#remove(java.lang.Object, java.lang.Object)}.
     */
    @Test
    public void testRemoveKV() {
        TransactionalMultimap<Integer, Long> map = new TransactionalMultimap<Integer, Long>();
        map.putAll(null, newSet(4l));
        map.putAll(1, newSet(1l, 2l));

        assertEquals("On non-existing pair", ImmutableSet.<Long> of(), map.remove(2, 1l));

        assertEquals("The old value set", newSet(4l), map.remove(null, 4l));
        assertFalse("Pair with empty value set treated as non-existing", map.containsKey(2));

        assertEquals("The old value set", newSet(1l, 2l), map.remove(1, 1l));
        assertEquals("The updated value set", newSet(2l), map.get(1));

        assertEquals("On non-existing pair, but existing key", newSet(2l), map.remove(1, 5l));
        assertEquals("Value set unchanged", newSet(2l), map.get(1));
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMultimap#remove(java.lang.Object, java.lang.Object)}.
     */
    @Test
    public void testSizeAndIsEmpty() {
        TransactionalMultimap<Integer, Long> map = new TransactionalMultimap<Integer, Long>();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        map.putAll(null, newSet(4l));
        assertFalse(map.isEmpty());
        assertEquals(1, map.size());

        map.putAll(1, newSet(1l, 2l));
        assertFalse(map.isEmpty());
        assertEquals(2, map.size());
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMultimap#remove(java.lang.Object, java.lang.Object)}.
     */
    @Test
    public void testKeySet() {
        TransactionalMultimap<Integer, Long> map = new TransactionalMultimap<Integer, Long>();
        map.putAll(null, newSet(4l));
        map.putAll(1, newSet(1l, 2l));
        assertEquals(newSet(null, 1), map.keySet());
    }

    // ==================== Transactional methods delegation test ====================

    /**
     * Test method for
     * {@link cern.entwined.TransactionalMultimap#commit(cern.entwined.TransactionalMultimap)}.
     */
    @Test
    public void testCommit() {
        TransactionalMultimap<Integer, Long> map = new TransactionalMultimap<Integer, Long>();
        map.putAll(1, newSet(1l, 2l));
        TransactionalMultimap<Integer, Long> globalState = new TransactionalMultimap<Integer, Long>();
        TransactionalMultimap<Integer, Long> committed = map.commit(globalState);

        assertEquals("Committed map state", newSet(1l, 2l), committed.get(1));

        committed.clear();
        assertEquals("Change to commited state doesn't affect the source", newSet(1l, 2l), map.get(1));
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMultimap#cleanCopy()}.
     */
    @Test
    public void testCleanCopy() {
        TransactionalMultimap<Integer, Long> map = new TransactionalMultimap<Integer, Long>();
        map.putAll(1, newSet(1l, 2l));
        TransactionalMultimap<Integer, Long> globalState = new TransactionalMultimap<Integer, Long>();
        TransactionalMultimap<Integer, Long> committed = map.commit(globalState);
        committed.putAll(2, newSet(2l, 3l));

        TransactionalMultimap<Integer, Long> cleanCopy = committed.cleanCopy();

        assertEquals("Clean copy map state", newSet(1l, 2l), cleanCopy.get(1));
        assertFalse("Clean copy map state", cleanCopy.containsKey(2));

        committed.clear();
        assertEquals("Change to the source doesn't affect the clean copy", newSet(1l, 2l), map.get(1));
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMultimap#dirtyCopy()}.
     */
    @Test
    public void testDirtyCopy() {
        TransactionalMultimap<Integer, Long> map = new TransactionalMultimap<Integer, Long>();
        map.putAll(1, newSet(1l, 2l));
        TransactionalMultimap<Integer, Long> globalState = new TransactionalMultimap<Integer, Long>();
        TransactionalMultimap<Integer, Long> committed = map.commit(globalState);
        committed.putAll(2, newSet(2l, 3l));

        TransactionalMultimap<Integer, Long> dirtyCopy = committed.dirtyCopy();

        assertEquals("Dirty copy map state", newSet(1l, 2l), dirtyCopy.get(1));
        assertEquals("Dirty copy map state", newSet(2l, 3l), dirtyCopy.get(2));

        committed.clear();
        assertEquals("Change to the source doesn't affect the dirty copy", newSet(1l, 2l), map.get(1));
    }

    /**
     * Test method for
     * {@link cern.entwined.TransactionalMultimap#update(cern.entwined.TransactionalMultimap, boolean)}.
     */
    @Test
    public void testUpdateTransactionalMultimapOfKVBoolean() {
        TransactionalMultimap<Integer, Long> map = new TransactionalMultimap<Integer, Long>();
        map.putAll(1, newSet(1l, 2l));
        TransactionalMultimap<Integer, Long> dirtyCopy = map.dirtyCopy();
        dirtyCopy.putAll(2, newSet(2l, 3l));
        dirtyCopy.put(1, 3l);

        map.update(dirtyCopy, false);
        assertEquals("Updated source", newSet(1l, 2l, 3l), map.get(1));
        assertEquals("Updated source", newSet(2l, 3l), map.get(2));
    }

    /**
     * Test method for
     * {@link cern.entwined.TransactionalMultimap#update(cern.entwined.TransactionalMultimap, boolean)}.
     */
    @Test(expected = ConflictException.class)
    public void testUpdateTransactionalMultimapOfKVBoolean_onlyReadLogs() {
        TransactionalMultimap<Integer, Long> map = new TransactionalMultimap<Integer, Long>();
        map.putAll(1, newSet(1l, 2l));

        // Build a global state with pair for key=2 modified
        TransactionalMultimap<Integer, Long> globalState = new TransactionalMultimap<Integer, Long>();
        TransactionalMultimap<Integer, Long> conflicting = map.commit(globalState);
        conflicting.put(2, 1l);
        conflicting = conflicting.commit(globalState);

        TransactionalMultimap<Integer, Long> dirtyCopy = map.dirtyCopy();
        dirtyCopy.put(2, 1l);
        map.update(dirtyCopy, true);

        map.commit(conflicting);
    }

    /**
     * Test method for
     * {@link cern.entwined.TransactionalMultimap#update(cern.entwined.TransactionalMultimap, boolean)}.
     */
    @Test(expected = ConflictException.class)
    public void testUpdateTransactionalMultimapOfKVBoolean_onlyReadLogs_updatesGlobalLog() {
        TransactionalMultimap<Integer, Long> map = new TransactionalMultimap<Integer, Long>();
        map.putAll(1, newSet(1l, 2l));

        // Build a global state with pair for key=2 modified
        TransactionalMultimap<Integer, Long> globalState = new TransactionalMultimap<Integer, Long>();
        TransactionalMultimap<Integer, Long> conflicting = map.commit(globalState);
        conflicting.put(2, 1l);
        conflicting = conflicting.commit(globalState);

        TransactionalMultimap<Integer, Long> dirtyCopy = map.dirtyCopy();
        dirtyCopy.size();
        map.update(dirtyCopy, true);

        map.commit(conflicting);
    }
}
