/*
 * Entwined STM
 * 
 * (c) Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.entwined;

/**
 * A common abstract class for client specific implementations of transactional shared memory. Snapshot mainly serves as
 * a root node in the tree of shared memory. It may also be described as a single access point to transactional shared
 * memory.
 * 
 * @param <T> The implementation class type.
 * @author Ivan Koblik
 */
public abstract class Snapshot<T extends Snapshot<T>> extends SemiPersistent<T> implements Comparable<T> {

    /**
     * Timestamp is incrementally generated from it previous value. It is supposed to be incremented only at commit.
     */
    protected final long timestamp;

    /**
     * Creates a new {@link Snapshot} initializing its timestamp with the give value.
     * <p>
     * <b>Note:</b> Pass following values
     * <ul>
     * <li><b>Zero</b> when creating a new snapshot,
     * <li><b>Previous timestamp</b> when copying a snapshot, and
     * <li><b>Previous timestamp + 1</b> when committing a snapshot.
     * </ul>
     * 
     * @param timestamp The timestamp of the created snapshot.
     */
    protected Snapshot(long timestamp) {
        if (timestamp < 0) {
            throw new IllegalArgumentException("Illegal timestamp value, it must be greater than zero");
        }
        this.timestamp = timestamp;
    }

    /**
     * {@inheritDoc}<br>
     * Compares snapshot timestamps only. Do not rely on this method when comparing snapshot states, use
     * {@link Transactional#commit(Object)} instead.
     */
    @Override
    public int compareTo(T otherSnapshot) {
        Utils.checkNull("Other snapshot", otherSnapshot);
        if (otherSnapshot.getClass() != this.getClass()) {
            throw new ClassCastException("Cannot compare with " + otherSnapshot.getClass());
        }
        return (int) (this.timestamp - otherSnapshot.timestamp);
    }
}
