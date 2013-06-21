/*
 * Entwined STM
 * 
 * (c) Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.entwined;

import static cern.entwined.test.TestUtils.newList;
import static cern.entwined.test.TestUtils.newMap;
import static cern.entwined.test.TestUtils.newSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.Test;

import cern.entwined.exception.ConflictException;

import com.google.common.collect.ImmutableMap;

/**
 * Unit test for {@link TransactionalMap}.
 * 
 * @author Ivan Koblik
 */
public class TransactionalMapTest {

    /**
     * Test method for {@link cern.entwined.TransactionalMap#TransactionalMap()}.
     */
    @Test
    public void testTransactionalMap() {
        new TransactionalMap<Integer, Integer>();
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#TransactionalMap(java.util.Map)}.
     */
    @Test
    public void testTransactionalMapMapOfKV_checkPropertInit() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        TransactionalMap<Integer, Integer> map = new TransactionalMap<Integer, Integer>(sourceMap);
        for (Integer key : sourceMap.keySet()) {
            assertEquals("Initialized with passed map", sourceMap.get(key), map.get(key));
        }
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#TransactionalMap(java.util.Map)}.
     */
    @Test
    public void testTransactionalMapMapOfKV_checkCopyingSourceMap() {
        Map<Integer, Integer> origMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        Map<Integer, Integer> sourceMap = new HashMap<Integer, Integer>(origMap);
        TransactionalMap<Integer, Integer> map = new TransactionalMap<Integer, Integer>(sourceMap);

        sourceMap.remove(2);
        sourceMap.put(1, 3);
        sourceMap.clear();

        for (Integer key : origMap.keySet()) {
            assertEquals("Initialized with passed map", origMap.get(key), map.get(key));
        }
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#TransactionalMap(java.util.Map)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testTransactionalMapMapOfKV_fail_NullArgument() {
        new TransactionalMap<Integer, Integer>(null);
    }

    // ==================== Local operation tests ====================

    /**
     * Test method for {@link cern.entwined.TransactionalMap#clear()}.
     */
    @Test
    public void testClear_SimulatesRemovalOfAllItems() {
        Map<Integer, Integer> origMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        Map<Integer, Integer> sourceMap = new HashMap<Integer, Integer>(origMap);
        TransactionalMap<Integer, Integer> map = new TransactionalMap<Integer, Integer>(sourceMap);
        map.clear();
        for (Integer key : origMap.keySet()) {
            assertFalse("Key mustn't be present on contains", map.containsKey(key));
            assertEquals("Key mustn't be present on get", null, map.get(key));
        }
        assertTrue("empty?", map.isEmpty());
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#clear()}.
     */
    @Test
    public void testClear_RemovesFreshlyPutTuples() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        TransactionalMap<Integer, Integer> map = new TransactionalMap<Integer, Integer>(sourceMap);
        map.put(10, 100);
        map.clear();
        assertFalse("Key mustn't be present on contains", map.containsKey(10));
        assertEquals("Key mustn't be present on get", null, map.get(10));
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#clear()}.
     */
    @Test
    public void testClear_KeepsSourceMap() {
        Map<Integer, Integer> origMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        Map<Integer, Integer> sourceMap = new HashMap<Integer, Integer>(origMap);
        TransactionalMap<Integer, Integer> map = new TransactionalMap<Integer, Integer>(sourceMap);
        map.clear();
        for (Integer key : origMap.keySet()) {
            assertEquals("Source map shouldn't be touched", origMap.get(key), sourceMap.get(key));
        }
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#containsKey(java.lang.Object)}.
     */
    @Test
    public void testContainsKey_ExistingKeys() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        TransactionalMap<Integer, Integer> map = new TransactionalMap<Integer, Integer>(sourceMap);
        for (Integer key : sourceMap.keySet()) {
            assertTrue("Must contain the key", map.containsKey(key));
        }
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#containsKey(java.lang.Object)}.
     */
    @Test
    public void testContainsKey_NonExistingKeys() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        TransactionalMap<Integer, Integer> map = new TransactionalMap<Integer, Integer>(sourceMap);
        for (int i = 5; i < 10; i++) {
            assertFalse("Mustn't contain the key", map.containsKey(i));
        }
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#containsKey(java.lang.Object)}.
     */
    @Test
    public void testContainsKey_NullKey() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4, null), newList(5, 6, 7, 8, 9));
        TransactionalMap<Integer, Integer> map = new TransactionalMap<Integer, Integer>(sourceMap);
        assertTrue("Must contain the key", map.containsKey(null));

    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#containsKey(java.lang.Object)}.
     */
    @Test
    public void testContainsKey_FreshlyAddedTuple() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4, null), newList(5, 6, 7, 8, 9));
        TransactionalMap<Integer, Integer> map = new TransactionalMap<Integer, Integer>(sourceMap);
        map.put(10, 100);
        assertTrue("Must contain the freshly added key", map.containsKey(10));
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#containsKey(java.lang.Object)}.
     */
    @Test
    public void testContainsKey_ClearedAndFreshlyAddedTuple() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4, null), newList(5, 6, 7, 8, 9));
        TransactionalMap<Integer, Integer> map = new TransactionalMap<Integer, Integer>(sourceMap);
        map.clear();
        map.put(10, 100);
        assertTrue("Must contain the freshly added key", map.containsKey(10));
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#get(java.lang.Object)}.
     */
    @Test
    public void testGet_ExistingPairs() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        TransactionalMap<Integer, Integer> map = new TransactionalMap<Integer, Integer>(sourceMap);
        for (Integer key : sourceMap.keySet()) {
            assertEquals("Value should be preserved", sourceMap.get(key), map.get(key));
        }
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#get(java.lang.Object)}.
     */
    @Test
    public void testGet_NonExistingPairs() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        TransactionalMap<Integer, Integer> map = new TransactionalMap<Integer, Integer>(sourceMap);
        for (int i = 5; i < 10; i++) {
            assertEquals("Null for non-existent values", null, map.get(i));
        }
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#get(java.lang.Object)}.
     */
    @Test
    public void testGet_NullKey() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4, null), newList(5, 6, 7, 8, 9));
        TransactionalMap<Integer, Integer> map = new TransactionalMap<Integer, Integer>(sourceMap);
        assertEquals("Value should be preserved", 9, (Object) map.get(null));
    }

    /**
     * @see TransactionalMapTest#testSizeAndIsEmpty()
     */
    public void testIsEmpty() {
        // See testSizeAndIsEmpty.
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#put(java.lang.Object, java.lang.Object)}.
     */
    @Test
    public void testPut_Get_addOneElement() {
        TransactionalMap<Integer, Integer> map = new TransactionalMap<Integer, Integer>();
        Integer res = map.put(10, 90);
        assertEquals("Nothing was replaced", null, res);
        assertEquals("Freshly put value", (Integer) 90, map.get(10));
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#put(java.lang.Object, java.lang.Object)}.
     */
    @Test
    public void testPut_Get_nullAsKey() {
        TransactionalMap<Integer, Integer> map = new TransactionalMap<Integer, Integer>();
        Integer res = map.put(null, 90);
        assertEquals("Nothing was replaced", null, res);
        assertEquals("Freshly put value", (Integer) 90, map.get(null));
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#put(java.lang.Object, java.lang.Object)}.
     */
    @Test
    public void testPut_Get_nullAsValue() {
        TransactionalMap<Integer, Integer> map = new TransactionalMap<Integer, Integer>();
        Integer res = map.put(14, null);
        assertEquals("Nothing was replaced", null, res);
        assertEquals("Freshly put value", (Integer) null, map.get(14));
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#put(java.lang.Object, java.lang.Object)}.
     */
    @Test
    public void testPut_Get_cleardAndUpdated() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        TransactionalMap<Integer, Integer> map = new TransactionalMap<Integer, Integer>(sourceMap);
        map.clear();
        Integer res = map.put(14, 134);
        assertEquals("Nothing was replaced", null, res);
        assertEquals("Freshly put value", (Integer) 134, map.get(14));
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#put(java.lang.Object, java.lang.Object)}.
     */
    @Test
    public void testPut_Get_updatedOriginalTuples() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        TransactionalMap<Integer, Integer> map = new TransactionalMap<Integer, Integer>(sourceMap);
        Map<Integer, Integer> newMap = newMap(newList(1, 2, 3, 4), newList(11, 12, 13, 14));
        for (Integer key : sourceMap.keySet()) {
            Integer res = map.put(key, newMap.get(key));
            assertEquals("Tuple was updated", sourceMap.get(key), res);
        }
        for (Integer key : sourceMap.keySet()) {
            assertEquals("Updated value", newMap.get(key), map.get(key));
        }
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#put(java.lang.Object, java.lang.Object)}.
     */
    @Test
    public void testPut_Get_updatedFreshTuples() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        TransactionalMap<Integer, Integer> map = new TransactionalMap<Integer, Integer>(sourceMap);

        // Updating
        Map<Integer, Integer> newMap = newMap(newList(1, 2, 3, 4), newList(11, 12, 13, 14));
        for (Integer key : sourceMap.keySet()) {
            map.put(key, newMap.get(key));
        }

        // Updating again
        Map<Integer, Integer> newerMap = newMap(newList(1, 2, 3, 4), newList(100, 101, 103, 104));
        for (Integer key : sourceMap.keySet()) {
            Integer res = map.put(key, newerMap.get(key));
            assertEquals("Tuple was updated", newMap.get(key), res);
        }

        // Checking
        for (Integer key : sourceMap.keySet()) {
            assertEquals("Updated value", newerMap.get(key), map.get(key));
        }
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#putAll(java.util.Map)}.
     */
    @Test
    public void testPutAll_Get_ToEmptyMap() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        TransactionalMap<Integer, Integer> map = new TransactionalMap<Integer, Integer>();
        map.putAll(sourceMap);
        for (Integer key : sourceMap.keySet()) {
            assertEquals("Freshly added value", sourceMap.get(key), map.get(key));
        }
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#putAll(java.util.Map)}.
     */
    @Test
    public void testPutAll_Get_ToFilledMap() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        TransactionalMap<Integer, Integer> map = new TransactionalMap<Integer, Integer>(sourceMap);
        Map<Integer, Integer> newMap = newMap(newList(10, 11, 12, 13), newList(5, 6, 7, 8));
        map.putAll(newMap);
        for (Integer key : newMap.keySet()) {
            assertEquals("Freshly added value", newMap.get(key), map.get(key));
        }
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#putAll(java.util.Map)}.
     */
    @Test
    public void testPutAll_Get_OverwritingOriginalTuples() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        TransactionalMap<Integer, Integer> map = new TransactionalMap<Integer, Integer>(sourceMap);
        Map<Integer, Integer> newMap = newMap(newList(1, 2, 3, 4), newList(6, 7, 8, 9));
        map.putAll(newMap);
        for (Integer key : newMap.keySet()) {
            assertEquals("Freshly added value", newMap.get(key), map.get(key));
        }
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#remove(java.lang.Object)}.
     */
    @Test
    public void testRemove_OriginalTuplesOnExistingKey() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        TransactionalMap<Integer, Integer> map = new TransactionalMap<Integer, Integer>(sourceMap);

        Integer res = map.remove(1);
        assertEquals("Removed a tuple", (Integer) 5, res);
        assertEquals("Removed key on get", null, map.get(1));
        assertFalse("Removed key on contains", map.containsKey(1));

        for (Integer key : sourceMap.keySet()) {
            if (key == 1) {
                continue;
            }
            assertEquals("Untouched key on get", sourceMap.get(key), map.get(key));
            assertTrue("Untouched key on contains", map.containsKey(key));
        }
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#remove(java.lang.Object)}.
     */
    @Test
    public void testRemove_OriginalTuplesOnNonExistingKey() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        TransactionalMap<Integer, Integer> map = new TransactionalMap<Integer, Integer>(sourceMap);
        Integer res = map.remove(10);
        assertEquals("Nothing was removed", null, res);
        for (Integer key : sourceMap.keySet()) {
            assertEquals("Untouched key on get", sourceMap.get(key), map.get(key));
            assertTrue("Untouched key on contains", map.containsKey(key));
        }
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#remove(java.lang.Object)}.
     */
    @Test
    public void testRemove_FreshTuplesOnExistingKey() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        TransactionalMap<Integer, Integer> map = new TransactionalMap<Integer, Integer>(sourceMap);
        map.put(10, 11);
        map.put(11, 12);

        Integer res = map.remove(10);
        assertEquals("Removed a fresh tuple", (Integer) 11, res);

        assertEquals("Removed key on get", null, map.get(10));
        assertFalse("Removed key on contains", map.containsKey(10));

        assertEquals("Untouched key on get", (Integer) 12, map.get(11));
        assertTrue("Untouched key on contains", map.containsKey(11));
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#remove(java.lang.Object)}.
     */
    @Test
    public void testRemove_RemovedOriginalAndReadded() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        TransactionalMap<Integer, Integer> map = new TransactionalMap<Integer, Integer>(sourceMap);
        map.remove(1);
        map.put(1, 10);

        assertEquals("Readded tuple on get", (Integer) 10, map.get(1));
        assertTrue("Readded tuple on contains", map.containsKey(1));
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#remove(java.lang.Object)}.
     */
    @Test
    public void testRemove_RemovedFreshItemAndReadded() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        TransactionalMap<Integer, Integer> map = new TransactionalMap<Integer, Integer>(sourceMap);
        map.put(10, 11);
        map.remove(10);
        map.put(10, 12);

        assertEquals("Readded tuple on get", (Integer) 12, map.get(10));
        assertTrue("Readded tuple on contains", map.containsKey(10));
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#remove(java.lang.Object)}.
     */
    @Test
    public void testRemove_RepeatedRemoveReturnsNull() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        TransactionalMap<Integer, Integer> map = new TransactionalMap<Integer, Integer>(sourceMap);
        map.remove(1);
        assertEquals("Repeated remove return value", null, map.remove(1));
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#size()}
     */
    @Test
    public void testSizeAndIsEmpty() {
        defaultFixture();
        assertEquals(4, localMap.size());

        // Added new entry
        localMap.put(5, 6);
        assertEquals("Size", 5, localMap.size());
        assertFalse("empty?", localMap.isEmpty());

        for (int i = 4; i > 0; i--) {
            // Removed old entries
            localMap.remove(i);
            assertEquals("Size", i, localMap.size());
            assertFalse("empty?", localMap.isEmpty());
        }

        // Removed new entry
        localMap.remove(5);
        assertEquals("Removed last key", 0, localMap.size());
        assertTrue("empty?", localMap.isEmpty());

        // Removed non-existing entry
        localMap.remove(10);
        assertEquals("After remove for unknown key", 0, localMap.size());
        assertTrue("empty?", localMap.isEmpty());

        // Added new entry to empty map
        localMap.put(6, 7);
        assertEquals("Added one key", 1, localMap.size());
        assertFalse("empty?", localMap.isEmpty());
    }

    // ==================== Access logging tests. ====================

    TransactionalMap<Integer, Integer> localMap;
    TransactionalMap<Integer, Integer> globalState;

    /**
     * Test method for {@link cern.entwined.TransactionalMap#clear()}.
     */
    @Test(expected = ConflictException.class)
    public void testClear_LogsAccess() {
        defaultFixture(1);
        localMap.clear();
        localMap.commit(globalState);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#containsKey(Object)}.
     */
    @Test(expected = ConflictException.class)
    public void testContainsKey_OnExistingKeyLogsAccess() {
        defaultFixture(2);
        localMap.containsKey(2);
        localMap.commit(globalState);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#containsKey(Object)}.
     */
    @Test(expected = ConflictException.class)
    public void testContainsKey_OnNonExistingKeyLogsAccess() {
        defaultFixture(7);
        localMap.containsKey(7);
        localMap.commit(globalState);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#get(Object)}.
     */
    @Test(expected = ConflictException.class)
    public void testGet_OnExistingKeyLogsAccess() {
        defaultFixture(3);
        localMap.get(3);
        localMap.commit(globalState);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#get(Object)}.
     */
    @Test(expected = ConflictException.class)
    public void testGet_OnNonExistingKeyLogsAccess() {
        defaultFixture(12);
        localMap.get(12);
        localMap.commit(globalState);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#put(Object, Object)}.
     */
    @Test(expected = ConflictException.class)
    public void testPut_OnExistingKeyLogsAccess() {
        defaultFixture(4);
        localMap.put(4, 8); // To enforce test, putting same value as in original
        localMap.commit(globalState);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#put(Object, Object)}.
     */
    @Test(expected = ConflictException.class)
    public void testPut_OnNonExistingKeyLogsAccess() {
        defaultFixture(10);
        localMap.put(10, 0);
        localMap.commit(globalState);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#putAll(Map)}.
     */
    @Test(expected = ConflictException.class)
    public void testPutAll_OnExistingKeyLogsAccess() {
        defaultFixture(1);
        // To enforce test, putting same value as in original
        Map<Integer, Integer> changes = newMap(newList(1), newList(5));
        localMap.putAll(changes);
        localMap.commit(globalState);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#putAll(Map)}.
     */
    @Test(expected = ConflictException.class)
    public void testPutAll_OnNonExistingKeyLogsAccess() {
        defaultFixture(11);
        Map<Integer, Integer> changes = newMap(newList(11), newList(11));
        localMap.putAll(changes);
        localMap.commit(globalState);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#remove(Object)}.
     */
    @Test(expected = ConflictException.class)
    public void testRemove_OnExistingKeyLogsAccess() {
        defaultFixture(2);
        localMap.remove(2);
        localMap.commit(globalState);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#remove(Object)}.
     */
    @Test(expected = ConflictException.class)
    public void testRemove_OnNonExistingKeyLogsAccess() {
        defaultFixture(12);
        localMap.remove(12);
        localMap.commit(globalState);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#size()}
     */
    @Test(expected = ConflictException.class)
    public void testSize_marksAsGloballyAccessed() {
        defaultFixture(12);
        localMap.size();
        localMap.commit(globalState);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#size()}
     */
    @Test(expected = ConflictException.class)
    public void testSize_marksAsGloballyAccessedAsClearDoesntMatterWhenUsedAfter() {
        defaultFixture(12);
        localMap.size();
        localMap.clear();
        localMap.commit(globalState);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#size()}
     */
    @Test
    public void testSize_doesntMarksWhenCleared() {
        defaultFixture(12);
        localMap.clear();
        assertEquals("On empty map", 0, localMap.size());
        localMap.put(1, 1);
        assertEquals("On map with 1 element", 1, localMap.size());
        localMap.commit(globalState);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#isEmpty()}
     */
    @Test(expected = ConflictException.class)
    public void testIsEmpty_marksAsGloballyAccessedWhenEmpty() {
        Map<Integer, Integer> sourceMap = ImmutableMap.<Integer, Integer> of();
        localMap = new TransactionalMap<Integer, Integer>(sourceMap);

        Map<Integer, Integer> modifiedSourceMap = newMap(newList(1), newList(5));
        globalState = new TransactionalMap<Integer, Integer>(modifiedSourceMap);

        localMap.isEmpty();
        localMap.commit(globalState);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#isEmpty()}
     */
    @Test(expected = ConflictException.class)
    public void testIsEmpty_marksAsGloballyAccessedWhenEmptied() {
        defaultFixture(12);
        for (Integer i : newList(1, 2, 3, 4)) {
            localMap.remove(i);
        }
        localMap.isEmpty();
        localMap.commit(globalState);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#isEmpty()}
     */
    @Test
    public void testIsEmpty_doesntMarkWhenCleared() {
        Map<Integer, Integer> sourceMap = ImmutableMap.<Integer, Integer> of();
        localMap = new TransactionalMap<Integer, Integer>(sourceMap);

        Map<Integer, Integer> modifiedSourceMap = newMap(newList(1), newList(5));
        globalState = new TransactionalMap<Integer, Integer>(modifiedSourceMap);

        localMap.clear();
        localMap.isEmpty();

        localMap.commit(globalState);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#isEmpty()}
     */
    @Test(expected = ConflictException.class)
    public void testIsEmpty_marksAsGloballyAccessedAsClearDoesntMatterWhenCalledAfter() {
        defaultFixture(12);
        for (Integer i : newList(1, 2, 3, 4)) {
            localMap.remove(i);
        }
        localMap.isEmpty();
        localMap.clear();
        localMap.isEmpty();
        localMap.commit(globalState);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#isEmpty()}
     */
    @Test
    public void testIsEmpty_doesntMarkWhenNotEmpty() {
        defaultFixture(12);
        localMap.isEmpty();
        TransactionalMap<Integer, Integer> result = localMap.commit(globalState);
        assertTrue(result.containsKey(12));
    }

    // ==================== Clean copy tests ====================

    @Test
    public void testCleanCopy_sourceMapCopied() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2), newList(5, 6));
        localMap = new TransactionalMap<Integer, Integer>(sourceMap);

        TransactionalMap<Integer, Integer> copyMap = localMap.cleanCopy();
        assertEquals("As in original", (Integer) 5, copyMap.get(1));
        assertEquals("As in original", (Integer) 6, copyMap.get(2));
    }

    @Test
    public void testCleanCopy_localChangesDiscarded() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2), newList(5, 6));
        localMap = new TransactionalMap<Integer, Integer>(sourceMap);
        localMap.put(1, 10);
        localMap.put(3, 20);

        TransactionalMap<Integer, Integer> copyMap = localMap.cleanCopy();
        assertEquals("As in source", (Integer) 5, copyMap.get(1));
        assertEquals("As in source", (Integer) 6, copyMap.get(2));
        assertEquals("As in source", (Integer) null, copyMap.get(3));
    }

    @Test
    public void testCleanCopy_copyDoesntInfluenceOriginal() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2), newList(5, 6));
        localMap = new TransactionalMap<Integer, Integer>(sourceMap);

        TransactionalMap<Integer, Integer> copyMap = localMap.cleanCopy();
        copyMap.put(5, 10);
        copyMap.put(10, 100);
        assertEquals("Original unmodified", (Integer) 5, localMap.get(1));
        assertEquals("Original unmodified", (Integer) 6, localMap.get(2));
        assertEquals("Original unmodified", (Integer) null, localMap.get(10));
    }

    @Test
    public void testCleanCopy_resetAccessLog() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2), newList(5, 6));
        localMap = new TransactionalMap<Integer, Integer>(sourceMap);
        localMap.containsKey(1);
        localMap.containsKey(2);
        localMap.containsKey(10);

        TransactionalMap<Integer, Integer> copyMap = localMap.cleanCopy();
        localMap.size();
        TransactionalMap<Integer, Integer> copyOfGlAccessedMap = localMap.cleanCopy();

        Map<Integer, Integer> globalMap = newMap(newList(1, 2), newList(7, 8));
        globalState = new TransactionalMap<Integer, Integer>(globalMap);

        assertSame("Accessed map copy", globalState, copyMap.commit(globalState));
        assertSame("Globally accessed map copy", globalState, copyOfGlAccessedMap.commit(globalState));
    }

    @Test
    public void testCleanCopy_accessesToCopyDontIfluenceOriginal() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2), newList(5, 6));
        localMap = new TransactionalMap<Integer, Integer>(sourceMap);

        TransactionalMap<Integer, Integer> copyMap = localMap.cleanCopy();
        copyMap.containsKey(1);
        copyMap.containsKey(2);
        copyMap.containsKey(10);

        Map<Integer, Integer> globalMap = newMap(newList(1, 2), newList(7, 8));
        globalState = new TransactionalMap<Integer, Integer>(globalMap);

        assertSame(globalState, localMap.commit(globalState));
    }

    // ==================== Dirty copy/update tests ====================

    @Test
    public void testDirtyCopy_sourceDataCopied() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        localMap = new TransactionalMap<Integer, Integer>(sourceMap);
        TransactionalMap<Integer, Integer> copyMap = localMap.dirtyCopy();

        for (Integer key : sourceMap.keySet()) {
            assertEquals("Source data", sourceMap.get(key), copyMap.get(key));
        }
    }

    @Test
    public void testDirtyCopy_sourceDataReferentiallyEqual() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        localMap = new TransactionalMap<Integer, Integer>(sourceMap);
        TransactionalMap<Integer, Integer> copyMap = localMap.dirtyCopy();
        localMap.update(copyMap, false);
    }

    @Test
    public void testDirtyCopy_localModificationsAndDelitionsCopied() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        localMap = new TransactionalMap<Integer, Integer>(sourceMap);
        localMap.put(1, 10);
        localMap.put(5, 11);
        localMap.remove(2);
        TransactionalMap<Integer, Integer> copyMap = localMap.dirtyCopy();

        assertEquals("Modified data", (Integer) 10, copyMap.get(1));
        assertEquals("Added data", (Integer) 11, copyMap.get(5));
        assertFalse("Removed data", copyMap.containsKey(2));
    }

    @Test(expected = ConflictException.class)
    public void testDirtyCopy_localAccessLogsCopied() {
        int conflictKey = 1;
        this.createAndQueryMap(conflictKey);
        Map<Integer, Integer> sourceMap = newMap(newList(conflictKey, 2, 3, 4), newList(10, 6, 7, 8));
        globalState = new TransactionalMap<Integer, Integer>(sourceMap);

        TransactionalMap<Integer, Integer> copyMap = localMap.dirtyCopy();
        copyMap.commit(globalState);
    }

    @Test(expected = ConflictException.class)
    public void testDirtyCopy_globallyAccessedFlagCopied() {
        defaultFixture(12);
        localMap.size();
        TransactionalMap<Integer, Integer> copyMap = localMap.dirtyCopy();
        copyMap.commit(globalState);
    }

    @Test
    public void testDirtyCopy_changesToOriginalAfterCopyNotVisibleInCopy() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        localMap = new TransactionalMap<Integer, Integer>(sourceMap);
        TransactionalMap<Integer, Integer> copyMap = localMap.dirtyCopy();

        localMap.put(1, 10);
        localMap.put(5, 11);
        localMap.remove(2);

        // Assert access logs are separate
        globalState = new TransactionalMap<Integer, Integer>(newMap(newList(1, 2, 3, 4), newList(11, 12, 13, 14)));
        copyMap.commit(globalState);

        assertEquals("Modified data", sourceMap.get(1), copyMap.get(1));
        assertEquals("Added data", sourceMap.get(5), copyMap.get(5));
        assertEquals("Removed data", sourceMap.get(2), copyMap.get(2));
    }

    @Test
    public void testDirtyCopy_changesToCopyNotVisibleInOriginal() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        localMap = new TransactionalMap<Integer, Integer>(sourceMap);
        TransactionalMap<Integer, Integer> copyMap = localMap.dirtyCopy();

        copyMap.put(1, 10);
        copyMap.put(5, 11);
        copyMap.remove(2);

        // Assert access logs are separate
        globalState = new TransactionalMap<Integer, Integer>(newMap(newList(1, 2, 3, 4), newList(11, 12, 13, 14)));
        localMap.commit(globalState);

        assertEquals("Modified data", sourceMap.get(1), localMap.get(1));
        assertEquals("Added data", sourceMap.get(5), localMap.get(5));
        assertEquals("Removed data", sourceMap.get(2), localMap.get(2));
    }

    @Test
    public void testUpdate_allChangesPreserved() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        localMap = new TransactionalMap<Integer, Integer>(sourceMap);

        localMap.put(3, 12);
        localMap.remove(4);

        TransactionalMap<Integer, Integer> copyMap = localMap.dirtyCopy();

        copyMap.put(1, 10);
        copyMap.put(5, 11);
        copyMap.remove(2);

        localMap.update(copyMap, false);
        assertEquals("Modified data in original before copy", (Integer) 12, localMap.get(3));
        assertFalse("Removed data in original before copy", localMap.containsKey(4));
        assertEquals("Modified data", (Integer) 10, localMap.get(1));
        assertEquals("Added data", (Integer) 11, localMap.get(5));
        assertFalse("Removed data", localMap.containsKey(2));
    }

    @Test(expected = ConflictException.class)
    public void testUpdateLogsOnly_getLogsPreserved() {
        defaultFixture(1);
        TransactionalMap<Integer, Integer> copyMap = localMap.dirtyCopy();
        copyMap.get(1);

        localMap.update(copyMap, true);
        localMap.commit(globalState);
    }

    @Test(expected = ConflictException.class)
    public void testUpdate_accessLogsPreserved() {
        int conflictKey = 1;
        defaultFixture(conflictKey);
        TransactionalMap<Integer, Integer> copyMap = localMap.dirtyCopy();
        copyMap.containsKey(conflictKey);

        localMap.update(copyMap, true);
        localMap.commit(globalState);
    }

    @Test(expected = ConflictException.class)
    public void testUpdateLogsOnly_globallyAccessedPreserved() {
        defaultFixture(1);
        TransactionalMap<Integer, Integer> copyMap = localMap.dirtyCopy();
        copyMap.size();

        localMap.update(copyMap, true);
        localMap.commit(globalState);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdate_fail_NotWithCopy() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        localMap = new TransactionalMap<Integer, Integer>(sourceMap);

        TransactionalMap<Integer, Integer> copyMap = new TransactionalMap<Integer, Integer>(sourceMap);
        localMap.update(copyMap, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateLogsOnly_fail_NotWithCopy() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        localMap = new TransactionalMap<Integer, Integer>(sourceMap);

        TransactionalMap<Integer, Integer> copyMap = new TransactionalMap<Integer, Integer>(sourceMap);
        localMap.update(copyMap, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdate_fail_NullArgument() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        localMap = new TransactionalMap<Integer, Integer>(sourceMap);
        localMap.update(null, false);
    }

    // ==================== Conflict detection tests ====================

    /**
     * Test method for {@link cern.entwined.TransactionalMap#commit(cern.entwined.TransactionalMap)}.
     */
    @Test
    public void testCommit_GlobalExtended_NonConflicting() {
        createAndQueryMap(1, 2, 3, 4);

        Map<Integer, Integer> modifiedSourceMap = newMap(newList(1, 2, 3, 4, 5), newList(5, 6, 7, 8, 9));
        globalState = new TransactionalMap<Integer, Integer>(modifiedSourceMap);

        localMap.commit(globalState);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#commit(cern.entwined.TransactionalMap)}.
     */
    @Test(expected = ConflictException.class)
    public void testCommit_GlobalExtended_Conflicting() {
        final int conflickKey = 5;
        createAndQueryMap(conflickKey);

        Map<Integer, Integer> modifiedSourceMap = newMap(newList(1, 2, 3, 4, conflickKey), newList(5, 6, 7, 8, 10));
        globalState = new TransactionalMap<Integer, Integer>(modifiedSourceMap);

        localMap.commit(globalState);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#commit(cern.entwined.TransactionalMap)}.
     */
    @Test(expected = ConflictException.class)
    public void testCommit_GlobalExtended_ByNullValue_Conflicting() {
        final int conflickKey = 100;
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        localMap = new TransactionalMap<Integer, Integer>(sourceMap);
        localMap.containsKey(conflickKey);

        Map<Integer, Integer> modifiedSourceMap = newMap(newList(1, 2, 3, 4, 100), newList(5, 6, 7, 8, null));
        globalState = new TransactionalMap<Integer, Integer>(modifiedSourceMap);

        localMap.commit(globalState);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#commit(cern.entwined.TransactionalMap)}.
     */
    @Test
    public void testCommit_GlobalShrinked_NonConflicting() {
        createAndQueryMap(1, 2, 3);

        Map<Integer, Integer> modifiedSourceMap = newMap(newList(1, 2, 3), newList(5, 6, 7));
        globalState = new TransactionalMap<Integer, Integer>(modifiedSourceMap);

        localMap.commit(globalState);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#commit(cern.entwined.TransactionalMap)}.
     */
    @Test(expected = ConflictException.class)
    public void testCommit_GlobalShrinked_Conflicting() {
        final int conflickKey = 4;
        createAndQueryMap(conflickKey);

        Map<Integer, Integer> modifiedSourceMap = newMap(newList(1, 2, 3), newList(5, 6, 7));
        globalState = new TransactionalMap<Integer, Integer>(modifiedSourceMap);

        localMap.commit(globalState);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#commit(cern.entwined.TransactionalMap)}.
     */
    @Test(expected = ConflictException.class)
    public void testCommit_GlobalShrinked_ByNullValue_Conflicting() {
        final int conflickKey = 100;
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4, 100), newList(5, 6, 7, 8, null));
        localMap = new TransactionalMap<Integer, Integer>(sourceMap);
        localMap.containsKey(conflickKey);

        Map<Integer, Integer> modifiedSourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        globalState = new TransactionalMap<Integer, Integer>(modifiedSourceMap);

        localMap.commit(globalState);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#commit(cern.entwined.TransactionalMap)}.
     */
    @Test
    public void testCommit_GlobalUpdatedTuple_NonConflicting() {
        createAndQueryMap(1, 2, 4);

        int newValue = 10;
        Map<Integer, Integer> modifiedSourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, newValue, 8));
        globalState = new TransactionalMap<Integer, Integer>(modifiedSourceMap);

        localMap.commit(globalState);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#commit(cern.entwined.TransactionalMap)}.
     */
    @Test(expected = ConflictException.class)
    public void testCommit_GlobalUpdatedTuple_Conflicting() {
        final int conflickKey = 2;
        createAndQueryMap(conflickKey);

        final int newValue = 10;
        Map<Integer, Integer> modifiedSourceMap = newMap(newList(1, conflickKey, 3, 4), newList(5, newValue, 7, 8));
        globalState = new TransactionalMap<Integer, Integer>(modifiedSourceMap);

        localMap.commit(globalState);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#commit(cern.entwined.TransactionalMap)}.
     */
    @Test
    public void testCommit_GloballyAccessed_NonConflicting() {
        defaultFixture();
        localMap.size();
        localMap.remove(1);
        assertEquals(newSet(2, 3, 4), localMap.commit(globalState).keySet());
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#commit(cern.entwined.TransactionalMap)}.
     */
    @Test(expected = ConflictException.class)
    public void testCommit_GloballyAccessed_Conflicting() {
        defaultFixture(10);
        localMap.size();
        localMap.commit(globalState);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#commit(cern.entwined.TransactionalMap)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCommit_fail_globalStateAccessed() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        localMap = new TransactionalMap<Integer, Integer>(sourceMap);

        Map<Integer, Integer> newMap = newMap(newList(1, 2, 3, 4, 5, 6, 7), newList(5, 6, 7, 8, 10, 11, 12));
        globalState = new TransactionalMap<Integer, Integer>(newMap);
        globalState.get(7);

        localMap.commit(globalState);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#commit(cern.entwined.TransactionalMap)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCommit_fail_globalStateExtended() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        localMap = new TransactionalMap<Integer, Integer>(sourceMap);

        Map<Integer, Integer> newMap = newMap(newList(1, 2, 3, 4, 5, 6, 7), newList(5, 6, 7, 8, 10, 11, 12));
        globalState = new TransactionalMap<Integer, Integer>(newMap);
        globalState.put(8, 13);

        localMap.commit(globalState);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#commit(cern.entwined.TransactionalMap)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCommit_fail_removedFromGlobalState() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        localMap = new TransactionalMap<Integer, Integer>(sourceMap);

        Map<Integer, Integer> newMap = newMap(newList(1, 2, 3, 4, 5, 6, 7), newList(5, 6, 7, 8, 10, 11, 12));
        globalState = new TransactionalMap<Integer, Integer>(newMap);
        globalState.remove(7);

        localMap.commit(globalState);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#commit(cern.entwined.TransactionalMap)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCommit_fail_globalStateGloballyAccessed() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        localMap = new TransactionalMap<Integer, Integer>(sourceMap);

        Map<Integer, Integer> newMap = newMap(newList(1, 2, 3, 4, 5, 6, 7), newList(5, 6, 7, 8, 10, 11, 12));
        globalState = new TransactionalMap<Integer, Integer>(newMap);
        globalState.size();

        localMap.commit(globalState);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#commit(cern.entwined.TransactionalMap)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCommit_fail_NullArgument() {
        localMap = new TransactionalMap<Integer, Integer>();
        localMap.commit(null);
    }

    // ==================== Commit accuracy tests ====================

    @Test
    public void testCommitResult_localMapCleared() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        localMap = new TransactionalMap<Integer, Integer>(sourceMap);
        localMap.clear();

        Map<Integer, Integer> newMap = newMap(newList(1, 2, 3, 4, 5, 6, 7), newList(5, 6, 7, 8, 10, 11, 12));
        globalState = new TransactionalMap<Integer, Integer>(newMap);

        Map<Integer, Integer> result = newMap(newList(5, 6, 7), newList(10, 11, 12));
        TransactionalMap<Integer, Integer> commitResult = localMap.commit(globalState);
        for (Integer key : result.keySet()) {
            assertEquals(result.get(key), commitResult.get(key));
        }
    }

    @Test
    public void testCommitResult_localMapUntouched() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        localMap = new TransactionalMap<Integer, Integer>(sourceMap);
        for (Integer key : sourceMap.keySet()) {
            localMap.containsKey(key);
            localMap.get(key);
        }

        Map<Integer, Integer> newMap = newMap(newList(1, 2, 3, 4, 5, 6, 7), newList(5, 6, 7, 8, 10, 11, 12));
        globalState = new TransactionalMap<Integer, Integer>(newMap);

        Map<Integer, Integer> result = newMap;
        TransactionalMap<Integer, Integer> commitResult = localMap.commit(globalState);
        assertSame("Same instance of global state", globalState, commitResult);
        for (Integer key : result.keySet()) {
            assertEquals("All keys from global state", result.get(key), commitResult.get(key));
        }
    }

    @Test
    public void testCommitResult_localMapWithModifiedTuple() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        localMap = new TransactionalMap<Integer, Integer>(sourceMap);
        localMap.put(2, 100);

        Map<Integer, Integer> newMap = newMap(newList(1, 2, 3, 4, 5, 6, 7), newList(5, 6, 7, 8, 10, 11, 12));
        globalState = new TransactionalMap<Integer, Integer>(newMap);

        Map<Integer, Integer> result = newMap(newList(1, 2, 3, 4, 5, 6, 7), newList(5, 100, 7, 8, 10, 11, 12));
        TransactionalMap<Integer, Integer> commitResult = localMap.commit(globalState);
        for (Integer key : result.keySet()) {
            assertEquals("All keys from global state with updated tuple", result.get(key), commitResult.get(key));
        }
    }

    @Test
    public void testCommitResult_localMapWithRewrittenTuple() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        localMap = new TransactionalMap<Integer, Integer>(sourceMap);
        localMap.put(2, 6); // Putting same value counts as an update

        Map<Integer, Integer> newMap = newMap(newList(1, 2, 3, 4, 5, 6, 7), newList(5, 6, 7, 8, 10, 11, 12));
        globalState = new TransactionalMap<Integer, Integer>(newMap);

        TransactionalMap<Integer, Integer> commitResult = localMap.commit(globalState);
        assertNotSame("Global state had to be reconstructed", globalState, commitResult);
        for (Integer key : newMap.keySet()) {
            assertEquals("All keys from global state with updated tuple", newMap.get(key), commitResult.get(key));
        }
    }

    @Test
    public void testCommitResult_localMapWithAddedTuple() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        localMap = new TransactionalMap<Integer, Integer>(sourceMap);
        localMap.put(100, 100);

        Map<Integer, Integer> newMap = newMap(newList(1, 2, 3, 4, 5, 6, 7), newList(5, 6, 7, 8, 10, 11, 12));
        globalState = new TransactionalMap<Integer, Integer>(newMap);

        Map<Integer, Integer> result = newMap(newList(1, 2, 3, 4, 5, 6, 7, 100), newList(5, 6, 7, 8, 10, 11, 12, 100));
        TransactionalMap<Integer, Integer> commitResult = localMap.commit(globalState);
        for (Integer key : result.keySet()) {
            assertEquals("All keys from global state with added tuple", result.get(key), commitResult.get(key));
        }
    }

    @Test
    public void testCommitResult_localMapWithModifiedTuples() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        localMap = new TransactionalMap<Integer, Integer>(sourceMap);
        localMap.putAll(newMap(newList(2, 3), newList(100, 101)));

        Map<Integer, Integer> newMap = newMap(newList(1, 2, 3, 4, 5, 6, 7), newList(5, 6, 7, 8, 10, 11, 12));
        globalState = new TransactionalMap<Integer, Integer>(newMap);

        Map<Integer, Integer> result = newMap(newList(1, 2, 3, 4, 5, 6, 7), newList(5, 100, 101, 8, 10, 11, 12));
        TransactionalMap<Integer, Integer> commitResult = localMap.commit(globalState);
        for (Integer key : result.keySet()) {
            assertEquals("All keys from global state with updated tuple", result.get(key), commitResult.get(key));
        }
    }

    @Test
    public void testCommitResult_localMapWithRewrittenTuples() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        localMap = new TransactionalMap<Integer, Integer>(sourceMap);
        // Putting same values counts as an update
        localMap.putAll(sourceMap);

        Map<Integer, Integer> newMap = newMap(newList(1, 2, 3, 4, 5, 6, 7), newList(5, 6, 7, 8, 10, 11, 12));
        globalState = new TransactionalMap<Integer, Integer>(newMap);

        TransactionalMap<Integer, Integer> commitResult = localMap.commit(globalState);
        assertNotSame("Global state had to be reconstructed", globalState, commitResult);
        for (Integer key : newMap.keySet()) {
            assertEquals("All keys from global state with updated tuple", newMap.get(key), commitResult.get(key));
        }
    }

    @Test
    public void testCommitResult_localMapWithAddedTuples() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        localMap = new TransactionalMap<Integer, Integer>(sourceMap);
        localMap.putAll(newMap(newList(102, 103), newList(100, 101)));

        Map<Integer, Integer> newMap = newMap(newList(1, 2, 3, 4, 5, 6, 7), newList(5, 6, 7, 8, 10, 11, 12));
        globalState = new TransactionalMap<Integer, Integer>(newMap);

        Map<Integer, Integer> result = newMap(newList(1, 2, 3, 4, 5, 6, 7, 102, 103), newList(5, 6, 7, 8, 10, 11, 12,
                100, 101));
        TransactionalMap<Integer, Integer> commitResult = localMap.commit(globalState);
        for (Integer key : result.keySet()) {
            assertEquals("All keys from global state with added tuple", result.get(key), commitResult.get(key));
        }
    }

    @Test
    public void testCommitResult_localMapWithRemovedTuple() {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        localMap = new TransactionalMap<Integer, Integer>(sourceMap);
        localMap.remove(2);

        Map<Integer, Integer> newMap = newMap(newList(1, 2, 3, 4, 5, 6, 7), newList(5, 6, 7, 8, 10, 11, 12));
        globalState = new TransactionalMap<Integer, Integer>(newMap);

        Map<Integer, Integer> result = newMap(newList(1, 3, 4, 5, 6, 7), newList(5, 7, 8, 10, 11, 12));
        TransactionalMap<Integer, Integer> commitResult = localMap.commit(globalState);
        for (Integer key : result.keySet()) {
            assertEquals("All keys from global state with added tuple", result.get(key), commitResult.get(key));
        }
        assertFalse("Doesn't containg removed key", commitResult.containsKey(2));
    }

    // ==================== KeySet tests ====================

    /**
     * Test method for cern.oasis.server.stm.TransactionalMap.KeySet}.
     */
    @Test
    public void testKeySet_clear() {
        defaultFixture(10);
        Set<Integer> keySet = localMap.keySet();
        keySet.clear();

        assertTrue(localMap.isEmpty());
        assertTrue(keySet.isEmpty());

        assertEquals(newSet(10), localMap.commit(globalState).keySet());
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap.KeySet}.
     */
    @Test
    public void testKeySet_SizeAndIsEmpty() {
        defaultFixture();
        Set<Integer> keySet = localMap.keySet();
        assertEquals(4, keySet.size());

        // Added new entry
        localMap.put(5, 6);
        assertEquals("Size", 5, keySet.size());
        assertFalse("empty?", keySet.isEmpty());

        for (int i = 4; i > 0; i--) {
            // Removed old entries
            keySet.remove(i);
            assertEquals("Size", i, keySet.size());
            assertFalse("empty?", keySet.isEmpty());
            assertFalse("localMap.empty?", localMap.isEmpty());
        }

        // Removed new entry
        keySet.remove(5);
        assertEquals("Removed last key", 0, keySet.size());
        assertTrue("empty?", keySet.isEmpty());
        assertTrue("localMap.empty?", localMap.isEmpty());

        // Removed non-existing entry
        keySet.remove(10);
        assertEquals("After remove for unknown key", 0, keySet.size());
        assertTrue("empty?", keySet.isEmpty());
        assertTrue("localMap.empty?", localMap.isEmpty());

        // Added new entry to empty map
        localMap.put(6, 7);
        assertEquals("Added one key", 1, keySet.size());
        assertFalse("empty?", keySet.isEmpty());
        assertFalse("localMap.empty?", localMap.isEmpty());
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap.KeySet}.
     */
    @Test
    public void testKeySet_remove() {
        defaultFixture();
        Set<Integer> keySet = localMap.keySet();

        for (int i = 4; i > 0; i--) {
            // Removed old entries
            assertFalse("empty?", keySet.isEmpty());
            assertTrue(keySet.remove(i));
        }
        assertTrue("empty?", keySet.isEmpty());
        assertFalse(keySet.remove(7));

        localMap.put(10, 10);
        assertFalse(keySet.remove(9));
        assertFalse("empty?", keySet.isEmpty());
        assertTrue(keySet.remove(10));
        assertTrue("empty?", keySet.isEmpty());
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap.KeySet}.
     */
    @Test
    public void testKeySet_removeAll() {
        defaultFixture();
        Set<Integer> keySet = localMap.keySet();

        assertFalse("empty?", keySet.isEmpty());

        assertTrue(keySet.removeAll(newList(1, 2)));
        assertFalse(keySet.contains(1));
        assertFalse(keySet.contains(2));
        assertFalse("empty?", keySet.isEmpty());
        assertFalse(keySet.removeAll(newList(1, 2)));

        assertTrue(keySet.removeAll(newList(3, 4)));
        assertTrue("empty?", keySet.isEmpty());
        assertFalse(keySet.removeAll(newList(1, 2, 3, 4, 5)));
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap.KeySet}.
     */
    @Test
    public void testKeySet_contains() {
        defaultFixture();
        Set<Integer> keySet = localMap.keySet();

        for (Integer i : newList(1, 2, 3, 4)) {
            assertTrue(keySet.contains(i));
        }
        assertFalse(keySet.contains(5));

        localMap.remove(1);
        assertFalse(keySet.contains(1));

        localMap.put(6, 7);
        assertTrue(keySet.contains(6));

        localMap.clear();
        for (Integer i : newList(1, 2, 3, 4, 6)) {
            assertFalse(keySet.contains(i));
        }
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#keySet()}.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testKeySet_addUnsupported() {
        defaultFixture();
        Set<Integer> keySet = localMap.keySet();
        keySet.add(10);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#keySet()}.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testKeySet_addAllUnsupported() {
        defaultFixture();
        Set<Integer> keySet = localMap.keySet();
        keySet.addAll(newList(10, 11));
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap#keySet()}.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testKeySet_retainAllUnsupported() {
        defaultFixture();
        Set<Integer> keySet = localMap.keySet();
        keySet.retainAll(newList(10, 11));
    }

    // ==================== KeyIterator tests ====================

    /**
     * Test method for {@link cern.entwined.TransactionalMap.KeyIterator}.
     */
    @Test
    public void testKeyIterator_iteratesCollectionOnce() {
        defaultFixture();

        // Add one item and remove one existing item.
        localMap.put(5, 6);
        localMap.remove(3);

        Iterator<Integer> iterator = localMap.keySet().iterator();
        Set<Integer> keys = new HashSet<Integer>();
        while (iterator.hasNext()) {
            assertTrue("Added element", keys.add(iterator.next()));
        }
        assertEquals(newSet(1, 2, 4, 5), keys);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap.KeyIterator}.
     */
    @Test(expected = ConflictException.class)
    public void testKeyIterator_fullIterationMarksAsGloballyAccessed() {
        defaultFixture(10);
        // Add one item and remove one existing item.
        localMap.put(5, 6);
        localMap.remove(3);

        Iterator<Integer> iterator = localMap.keySet().iterator();
        for (int i = 0; i < 4; i++) {
            iterator.next();
            localMap.commit(globalState);
        }
        try {
            iterator.next();
            fail("Expected to be the last element");
        } catch (NoSuchElementException ex) {
            // OK
        }
        localMap.commit(globalState);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap.KeyIterator}.
     */
    @Test(expected = ConflictException.class)
    public void testKeyIterator_hasNextOnTheLastElementMarksAsGloballyAccessed() {
        defaultFixture(10);
        // Add one item and remove one existing item.
        localMap.put(5, 6);
        localMap.remove(3);

        Iterator<Integer> iterator = localMap.keySet().iterator();
        for (int i = 0; i < 4; i++) {
            assertTrue(iterator.hasNext());
            iterator.next();
            localMap.commit(globalState);
        }
        assertFalse(iterator.hasNext());
        localMap.commit(globalState);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap.KeyIterator}.
     */
    @Test
    public void testKeyIterator_doesntMarksAsGloballyAccessedOnClearedMap() {
        defaultFixture(10);

        // Add one item and remove one existing item.
        localMap.clear();
        localMap.put(6, 7);
        localMap.put(7, 8);
        localMap.put(8, 9);

        Set<Integer> keys = new HashSet<Integer>();
        Iterator<Integer> iterator = localMap.keySet().iterator();
        while (iterator.hasNext()) {
            assertTrue("Added element", keys.add(iterator.next()));
        }
        assertEquals(newSet(6, 7, 8), keys);
        assertFalse(iterator.hasNext());
        localMap.commit(globalState);
    }

    /**
     * Test method for {@link cern.entwined.TransactionalMap.KeyIterator}.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testKeyIterator_removeUnsupported() {
        defaultFixture();
        Iterator<Integer> iterator = localMap.keySet().iterator();
        iterator.next();
        iterator.remove();
    }

    // =======================================================
    // ==================== Test fixtures ====================
    // =======================================================

    /**
     * Creates a local map and touches listed keys.
     * 
     * @param keysToTouch The list of keys to be accessed.
     */
    private void createAndQueryMap(int... keysToTouch) {
        Utils.checkNull("Array of keys", keysToTouch);
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        localMap = new TransactionalMap<Integer, Integer>(sourceMap);

        for (Integer key : keysToTouch) {
            localMap.containsKey(key);
        }
    }

    /**
     * Preinitializes localMap and globalState for transaction conflict tests. Changes tuple with the given key in
     * globalMap.
     * 
     * @param conflictKey The key value to simulate conflict on. Values from 1 to 4 are in the source map.
     */
    private void defaultFixture(int... conflictKeys) {
        Map<Integer, Integer> sourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        localMap = new TransactionalMap<Integer, Integer>(sourceMap);

        Map<Integer, Integer> modifiedSourceMap = newMap(newList(1, 2, 3, 4), newList(5, 6, 7, 8));
        int i = 0;
        for (Integer key : conflictKeys) {
            modifiedSourceMap.put(key, 10 + i);
            i++;
        }
        globalState = new TransactionalMap<Integer, Integer>(modifiedSourceMap);
    }
}
