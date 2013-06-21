/*
 * Entwined STM
 * 
 * (c) Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.entwined;

import static cern.entwined.test.TestUtils.newList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Before;
import org.junit.Test;

import cern.entwined.exception.ConflictException;

/**
 * {@link TransactionalQueue} unit tests.
 * 
 * @author Ivan Koblik
 */
public class TransactionalQueueTest {

    @Before
    public void setUp() throws Exception {
    }

    // ==================== Constructor tests ====================

    @Test
    public void testTransactionalQueue() {
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>();
        assertTrue("Queue is empty", queue.isEmpty());
    }

    @Test
    public void testTransactionalQueueCollectionOfE_copiesCollection() {
        List<Integer> originalList = newList(1, 2, 3);
        List<Integer> mutableList = new ArrayList<Integer>(originalList);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(mutableList);

        mutableList.clear();
        List<Integer> drainedList = new ArrayList<Integer>();
        queue.drainTo(drainedList);
        assertEquals("Original list and drained from the queue", originalList, drainedList);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTransactionalQueueCollectionOfE_failNullArg() {
        new TransactionalQueue<Integer>(null);
    }

    // ==================== Clean copy tests ====================

    @Test
    public void testCleanCopy_sourceQueueCopied() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);

        List<Integer> drainedTo = new ArrayList<Integer>();
        queue.cleanCopy().drainTo(drainedTo);
        assertEquals("Original list and drained from the copy", originalList, drainedTo);
    }

    @Test
    public void testCleanCopy_localChangesDiscarded() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);
        queue.poll();
        List<Integer> drainedTo = new ArrayList<Integer>();
        queue.cleanCopy().drainTo(drainedTo);
        assertEquals("Original list and drained from the copy", originalList, drainedTo);
    }

    @Test
    public void testCleanCopy_copyDoesntInfuenceOriginal() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);

        TransactionalQueue<Integer> copy = queue.cleanCopy();
        copy.clear();
        copy.offer(4);

        List<Integer> drainedTo = new ArrayList<Integer>();
        queue.drainTo(drainedTo);
        assertEquals("Original list and drained from the copy", originalList, drainedTo);
    }

    @Test
    public void testCleanCopy_resetAccessLog() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);
        queue.poll();
        queue.offer(4);
        queue.isEmpty();

        TransactionalQueue<Integer> copy = queue.cleanCopy();
        TransactionalQueue<Integer> globalState = new TransactionalQueue<Integer>(originalList);

        assertSame("Global state unchanged", globalState, copy.commit(globalState));
    }

    @Test
    public void testCleanCopy_accessesToCopyDontIfluenceOriginalLog() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);

        TransactionalQueue<Integer> copy = queue.cleanCopy();
        copy.poll();
        copy.offer(4);
        copy.isEmpty();

        TransactionalQueue<Integer> globalState = new TransactionalQueue<Integer>(originalList);
        assertSame("Global state unchanged", globalState, queue.commit(globalState));
    }

    @Test
    public void testCleanCopy_sourceDataReferentiallyEqual() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);
        TransactionalQueue<Integer> copy = queue.cleanCopy();
        queue.update(copy, false);
    }

    // ==================== Dirty copy tests ====================

    @Test
    public void testDirtyCopy_sourceDataCopied() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);

        List<Integer> drainedTo = new ArrayList<Integer>();
        queue.dirtyCopy().drainTo(drainedTo);
        assertEquals("Original list and drained from the copy", originalList, drainedTo);
    }

    @Test
    public void testDirtyCopy_sourceDataReferentiallyEqual() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);
        TransactionalQueue<Integer> copy = queue.dirtyCopy();
        queue.update(copy, false);
    }

    @Test
    public void testDirtyCopy_localModificationsAndDelitionsCopied() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);
        queue.poll();
        queue.offer(4);

        TransactionalQueue<Integer> copy = queue.dirtyCopy();
        List<Integer> drainedTo = new ArrayList<Integer>();
        copy.drainTo(drainedTo);
        assertEquals("Modified queue", newList(2, 3, 4), drainedTo);
    }

    @Test(expected = ConflictException.class)
    public void testDirtyCopy_localPeekCountCopied() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);
        queue.peek();
        queue.dirtyCopy().commit(pollGlobalState(queue));
    }

    @Test(expected = ConflictException.class)
    public void testDirtyCopy_localPollCountCopied() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);
        queue.offer(4);
        queue.clear();
        queue.dirtyCopy().commit(extendGlobalState(queue, 4));
    }

    @Test
    public void testDirtyCopy_changesToOriginalAfterCopyNotVisibleInCopy() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);
        TransactionalQueue<Integer> copy = queue.dirtyCopy();
        queue.clear();

        TransactionalQueue<Integer> globalState = queue.cleanCopy();
        copy.commit(globalState);

        List<Integer> drainedTo = new ArrayList<Integer>();
        copy.drainTo(drainedTo);
        assertEquals("Unchanged list", originalList, drainedTo);
    }

    @Test
    public void testDirtyCopy_changesToCopyNotVisibleInOriginal() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);
        TransactionalQueue<Integer> copy = queue.dirtyCopy();
        copy.clear();

        TransactionalQueue<Integer> globalState = queue.cleanCopy();
        queue.commit(globalState);

        List<Integer> drainedTo = new ArrayList<Integer>();
        queue.drainTo(drainedTo);
        assertEquals("Unchanged list", originalList, drainedTo);
    }

    // ==================== Update tests ====================

    @Test
    public void testUpdateTransactionalQueueOfEBoolean_allChangesPreserved() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);

        TransactionalQueue<Integer> copy = queue.dirtyCopy();
        copy.poll();
        copy.offer(10);
        copy.offer(11);
        copy.poll();
        queue.update(copy, false);

        List<Integer> drainedTo = new ArrayList<Integer>();
        queue.drainTo(drainedTo);
        assertEquals("Queue got updates from copy", newList(3, 10, 11), drainedTo);
    }

    @Test(expected = ConflictException.class)
    public void testUpdateTransactionalQueueOfEBoolean_peekLogsPreserved() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);

        TransactionalQueue<Integer> copy = queue.dirtyCopy();
        copy.peek();
        queue.update(copy, true);

        queue.commit(this.pollGlobalState(queue));
    }

    @Test(expected = ConflictException.class)
    public void testUpdateTransactionalQueueOfEBoolean_pollLogsPreserved() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);

        TransactionalQueue<Integer> copy = queue.dirtyCopy();
        copy.offer(10);
        copy.clear();
        queue.update(copy, true);

        queue.commit(this.extendGlobalState(queue, 10));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateTransactionalQueueOfEBoolean_fail_notWithCopy() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);

        TransactionalQueue<Integer> falseCopy = new TransactionalQueue<Integer>(originalList);
        queue.update(falseCopy, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateTransactionalQueueOfEBoolean_fail_nullArgument() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);
        queue.update(null, false);
    }

    // ==================== Conflict detection tests ====================

    @Test(expected = ConflictException.class)
    public void testCommit_conflict_poll() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);
        queue.peek();
        queue.commit(pollGlobalState(queue));
    }

    @Test(expected = ConflictException.class)
    public void testCommit_conflict_pollAndPeekPastSource() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);
        queue.offer(4);
        queue.clear();
        queue.commit(extendGlobalState(queue, 4));
    }

    @Test(expected = ConflictException.class)
    public void testCommit_conflict_innerPeekPastSource() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);
        TransactionalQueue<Integer> inner = queue.dirtyCopy();
        inner.offer(4);
        inner.clear();
        queue.update(inner, true);
        queue.commit(extendGlobalState(queue, 4));
    }

    // ==================== Commit accuracy/fail tests ====================

    @Test
    public void testCommit_globalStateCannotBePolled() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);
        queue.poll();
        TransactionalQueue<Integer> result = queue.commit(extendGlobalState(queue, 4));
        assertNull(result.poll());
    }

    @Test
    public void testCommit_nonConflict_pollAndConcurrentOffer() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);
        queue.poll();
        TransactionalQueue<Integer> result = queue.commit(extendGlobalState(queue, 4));
        List<Integer> drainedTo = new ArrayList<Integer>();
        // Taking clean copy as global state cannot be polled.
        result.cleanCopy().drainTo(drainedTo);

        assertEquals(newList(2, 3, 4), drainedTo);
    }

    @Test
    public void testCommit_nonConflict_clearAndConcurrentOffer() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);
        queue.clear();
        queue.offer(5);
        TransactionalQueue<Integer> result = queue.commit(extendGlobalState(queue, 4));
        List<Integer> drainedTo = new ArrayList<Integer>();
        // Taking clean copy as global state cannot be polled.
        result.cleanCopy().drainTo(drainedTo);

        assertEquals(newList(4, 5), drainedTo);
    }

    @Test
    public void testCommit_nonConflict_concurrentOffers() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);
        queue.offer(5);
        TransactionalQueue<Integer> result = queue.commit(extendGlobalState(queue, 4));
        List<Integer> drainedTo = new ArrayList<Integer>();
        // Taking clean copy as global state cannot be polled.
        result.cleanCopy().drainTo(drainedTo);

        assertEquals(newList(1, 2, 3, 4, 5), drainedTo);
    }

    @Test
    public void testCommit_nonConflict_peekAndConcurrentOffer() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);
        queue.peek();
        TransactionalQueue<Integer> globalState = extendGlobalState(queue, 4);
        TransactionalQueue<Integer> result = queue.commit(globalState);
        assertSame("Global state unchanged", globalState, result);
    }

    @Test
    public void testCommit_nonConflict_noLocalReads() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);
        TransactionalQueue<Integer> globalState = pollGlobalState(extendGlobalState(queue, 4));
        TransactionalQueue<Integer> result = queue.commit(globalState);
        assertSame("Global state unchanged", globalState, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCommit_fail_nullArgument() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);
        queue.commit(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCommit_fail_touchedGlobalState() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);
        TransactionalQueue<Integer> touchedState = queue.cleanCopy();
        touchedState.peek();
        queue.commit(touchedState);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCommit_fail_extendedGlobalState() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);
        TransactionalQueue<Integer> touchedState = queue.cleanCopy();
        touchedState.offer(10);
        queue.commit(touchedState);
    }

    @Test
    public void testDrainTo() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);
        List<Integer> drainedTo = new ArrayList<Integer>();
        queue.drainTo(drainedTo);

        assertEquals(originalList, drainedTo);
        assertNull(queue.poll());

        // It is OK to drain empty queue
        queue.drainTo(newList(1));
    }

    @Test
    public void testOffer() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);
        assertTrue("Offer returns true", queue.offer(4));
        assertTrue("Offer returns true", queue.offer(5));
        assertTrue("Offer returns true", queue.offer(5));

        List<Integer> drainedTo = new ArrayList<Integer>();
        queue.drainTo(drainedTo);
        assertEquals("Extended list", newList(1, 2, 3, 4, 5, 5), drainedTo);

        queue.offer(6);
        assertEquals("Offerd value after clearing up", (Integer) 6, queue.poll());
    }

    @Test
    public void testOffer_keepsCounter() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);

        queue.clear();
        queue.offer(5);
        queue.commit(extendGlobalState(queue, 4));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOffer_fail_Null() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);
        queue.offer(null);
    }

    @Test(expected = ConflictException.class)
    public void testPeek_getsFirstElementAndIncsPeekCount() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);
        assertEquals("First element", (Integer) 1, queue.peek());
        queue.commit(pollGlobalState(queue));
    }

    @Test
    public void testPeek_doesntGoPastSourceQueueIfNoTail() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);
        queue.clear();
        assertNull("Past source queue", queue.peek());
        queue.commit(extendGlobalState(queue, 4));
    }

    @Test(expected = ConflictException.class)
    public void testPeek_goesPastSourceQueueIfTheresTail() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);
        queue.clear();
        queue.offer(4);
        assertEquals("Reads tail", (Integer) 4, queue.peek());
        queue.commit(extendGlobalState(queue, 4));
    }

    @Test
    public void testPoll() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);
        assertEquals("First value", (Integer) 1, queue.poll());
        assertEquals("Consequtive value", (Integer) 2, queue.poll());
        assertEquals("Consequtive value", (Integer) 3, queue.poll());
        assertEquals("End value", null, queue.poll());
        queue.offer(4);
        assertEquals("Tail", (Integer) 4, queue.poll());
        assertEquals("End value", null, queue.poll());
    }

    @Test(expected = ConflictException.class)
    public void testPoll_increasesPeekCounter() {
        List<Integer> originalList = newList(1, 2, 3);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);
        assertEquals("First value", (Integer) 1, queue.poll());
        queue.commit(pollGlobalState(queue));
    }

    @Test(expected = ConflictException.class)
    public void testPoll_increasesPollCounter() {
        List<Integer> originalList = newList(1);
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(originalList);
        assertEquals("First value", (Integer) 1, queue.poll());
        queue.offer(2);
        assertEquals("First value", (Integer) 2, queue.poll());
        queue.commit(extendGlobalState(queue, 2));
    }

    @Test
    public void testIsEmpty() {
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>();
        assertTrue("Initially empty", queue.isEmpty());

        queue.offer(1);
        assertFalse("Extened", queue.isEmpty());

        queue = new TransactionalQueue<Integer>(newList(1, 2));
        assertFalse("Initially not empty", queue.isEmpty());

        queue.clear();
        assertTrue("Emptied", queue.isEmpty());

        queue.commit(extendGlobalState(queue, 3));
    }

    @Test
    public void testIsEmpty_peekCounterStopsOnSource() {
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(newList(1, 2));
        // Not empty queue
        queue.isEmpty();
        queue.commit(extendGlobalState(queue, 3));

        // Empty queue but without tail.
        queue.clear();
        queue.isEmpty();
        queue.commit(extendGlobalState(queue, 3));
    }

    @Test(expected = ConflictException.class)
    public void testIsEmpty_peekCounterEntersTailAsSourceIsEmpty() {
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(newList(1, 2));

        // Emptied and extended queue
        queue.clear();
        queue.offer(1);
        queue.isEmpty();
        queue.commit(extendGlobalState(queue, 3));
    }

    @Test
    public void testSize() {
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>();
        assertEquals("Initially empty", 0, queue.size());

        queue.offer(1);
        assertEquals("Extended", 1, queue.size());

        queue = new TransactionalQueue<Integer>(newList(1, 2));
        assertEquals("Initially not empty", 2, queue.size());

        queue.poll();
        assertEquals("Removed one item", 1, queue.size());

        queue.clear();
        assertEquals("Emptied", 0, queue.size());

        // Doesn't go past source queue if tail is empty
        queue.commit(extendGlobalState(queue, 3));

        queue = new TransactionalQueue<Integer>(newList(1, 2));
        queue.offer(3);
        assertEquals("Initially not empty and extended", 3, queue.size());
    }

    @Test
    public void testSize_peekCounterStopsOnSource() {
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(newList(1, 2));
        // Not empty queue
        queue.size();
        queue.commit(extendGlobalState(queue, 3));

        // Empty queue but without tail.
        queue.clear();
        queue.size();
        queue.commit(extendGlobalState(queue, 3));
    }

    @Test(expected = ConflictException.class)
    public void testSize_peekCounterEntersTailAsSourceIsEmpty() {
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(newList(1, 2));

        // Emptied and extended queue
        queue.clear();
        queue.offer(1);
        queue.size();
        queue.commit(extendGlobalState(queue, 3));
    }

    @Test
    public void testAdd() {
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>();
        queue.add(10);
        assertEquals((Integer) 10, queue.peek());
    }

    @Test
    public void testElement() {
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(newList(1, 2));
        assertEquals("First item in the queue", queue.peek(), queue.element());
    }

    @Test(expected = NoSuchElementException.class)
    public void testElement_emptyQueue() {
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>();
        queue.element();
    }

    @Test
    public void testRemove() {
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(newList(1, 2));
        assertEquals("First item in the queue", queue.peek(), queue.remove());
    }

    @Test(expected = NoSuchElementException.class)
    public void testRemove_emptyQueue() {
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>();
        queue.remove();
    }

    @Test
    public void testAddAll() {
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>();
        queue.addAll(newList(1, 2, 3));

        List<Integer> drainedTo = new ArrayList<Integer>();
        queue.drainTo(drainedTo);
        assertEquals(newList(1, 2, 3), drainedTo);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddAll_fail_cannotAddItself() {
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>();
        queue.addAll(queue);
    }

    @Test
    public void testClear() {
        TransactionalQueue<Integer> queue = new TransactionalQueue<Integer>(newList(1, 2, 3));
        queue.clear();
        assertTrue(queue.isEmpty());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testContainsAll() {
        new TransactionalQueue<Integer>().containsAll(newList(1, 2, 3));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testContains() {
        new TransactionalQueue<Integer>().contains(1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIterator() {
        new TransactionalQueue<Integer>().iterator();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveObject() {
        new TransactionalQueue<Integer>().remove(2);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveAll() {
        new TransactionalQueue<Integer>().removeAll(newList(1, 2, 3));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRetainAll() {
        new TransactionalQueue<Integer>().retainAll(newList(1, 2, 3));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testToArray() {
        new TransactionalQueue<Integer>().toArray();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testToArrayTArray() {
        new TransactionalQueue<Integer>().toArray(new Integer[2]);
    }

    /**
     * Creates global-state like queue, with first element polled from the clean copy of the given original.
     * 
     * @param original The original collection, clean copy of which will be used.
     * @return Global-state like queue.
     */
    private <E> TransactionalQueue<E> pollGlobalState(TransactionalQueue<E> original) {
        TransactionalQueue<E> updated = original.cleanCopy();
        updated.poll();
        return updated.commit(original.cleanCopy());
    }

    /**
     * Creates global-state like queue, with one element added to the clean copy of the given original.
     * 
     * @param original The original collection, clean copy of which will be used.
     * @param value Value to be appended.
     * @return Global-state like queue.
     */
    private <E> TransactionalQueue<E> extendGlobalState(TransactionalQueue<E> original, E value) {
        TransactionalQueue<E> copy = original.cleanCopy();
        copy.offer(value);
        return copy.commit(original.cleanCopy());
    }
}
