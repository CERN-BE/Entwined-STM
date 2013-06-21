/*
 * Entwined STM
 * 
 * (c) Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.entwined;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link Snapshot} class.
 * 
 * @author Ivan Koblik
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public abstract class SnapshotAbstractTest {

    protected abstract Snapshot getSnapshot(long timestamp);

    protected Snapshot getSnapshot() {
        return getSnapshot(0l);
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testSnapshot() {
        getSnapshot(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSnapshot_failNegativeId() {
        getSnapshot(-1);
    }

    @Test
    public void testAbstractSnapshot_CompareTo_oneLessThanTheOther() {
        Snapshot lesserSnapshot = getSnapshot(100);
        Snapshot snapshot = getSnapshot(101);
        assertTrue("Snapshot with lesser timestamp is lessert", lesserSnapshot.compareTo(snapshot) < 0);
    }

    @Test
    public void testAbstractSnapshot_CompareTo_equal() {
        Snapshot lesserSnapshot = getSnapshot(100);
        Snapshot snapshot = getSnapshot(100);
        assertTrue("Snapshots with equal timestamps", lesserSnapshot.compareTo(snapshot) == 0);
    }

    @Test
    public void testAbstractSnapshot_CompareTo_oneGreaterThanTheOther() {
        Snapshot greaterSnapshot = getSnapshot(101);
        Snapshot snapshot = getSnapshot(100);
        assertTrue("Snapshot with greater timestamp is greater", greaterSnapshot.compareTo(snapshot) > 0);
    }

    @Test
    public void testAbstractSnapshot_CompareTo_hugeDifference() {
        Snapshot greaterSnapshot = getSnapshot(0);
        Snapshot snapshot = getSnapshot(100 * ((long) Integer.MAX_VALUE));
        assertTrue("Snapshot with greater timestamp is greater", greaterSnapshot.compareTo(snapshot) > 0);
        assertTrue("Snapshot with lesse timestamp is lesser", snapshot.compareTo(greaterSnapshot) < 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAbstractSnapshot_CompareTo_failNullArgument() {
        getSnapshot().compareTo(null);
    }

    @Test(expected = ClassCastException.class)
    public void testAbstractSnapshot_CompareTo_failWrongClass() {
        ((Comparable) getSnapshot()).compareTo(new Object());
    }
}
