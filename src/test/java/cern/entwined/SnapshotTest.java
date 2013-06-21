/*
 * Entwined STM
 * 
 * (c) Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.entwined;

import org.junit.Test;

/**
 * @author Ivan Koblik
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class SnapshotTest extends SnapshotAbstractTest {

    @Override
    protected Snapshot getSnapshot(long timestamp) {
        return new TestSnapshot(timestamp);
    }

    @Test(expected = ClassCastException.class)
    public void testCompareTo_failWrongClassAlthoughSameHierarchy() {
        getSnapshot().compareTo(new TestSnapshot() {
            // This creates a new class.
        });
    }
}
