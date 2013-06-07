/*
 * Entwined STM
 * 
 * © Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.entwined;

import java.util.Map;

/**
 * A simple implementation of {@link Snapshot} with two transactional references and a map.
 * 
 * @author Ivan Koblik
 */
public class TestSnapshot extends Snapshot<TestSnapshot> {

    private CompositeCollection compositeCollection;

    private final int REF1 = 0;
    private final int REF2 = 1;
    private final int MAP = 2;

    /**
     * Creates a new snapshot initializing it with given values.
     * 
     * @param ref1 The first reference value.
     * @param ref2 The second reference value.
     * @param map The map to be copied, make sure it is not null or {@link IllegalArgumentException} will be thrown.
     */
    public TestSnapshot(Integer ref1, Integer ref2, Map<Integer, Integer> map) {
        this(0, //
                new CompositeCollection(//
                        new TransactionalRef<Integer>(ref1),// 0
                        new TransactionalRef<Integer>(ref2),// 1
                        new TransactionalMap<Integer, Integer>(map)// 2
                ));
    }

    public TestSnapshot() {
        this(0, //
                new CompositeCollection(//
                        new TransactionalRef<Integer>(),// 0
                        new TransactionalRef<Integer>(),// 1
                        new TransactionalMap<Integer, Integer>()// 2
                ));
    }

    protected TestSnapshot(long timestamp) {
        this(timestamp, //
                new CompositeCollection(//
                        new TransactionalRef<Integer>(),// 0
                        new TransactionalRef<Integer>(),// 1
                        new TransactionalMap<Integer, Integer>()// 2
                ));
    }

    private TestSnapshot(long timestamp, CompositeCollection compositeCollection) {
        super(timestamp);
        this.compositeCollection = compositeCollection;
    }

    @Override
    protected TestSnapshot cleanCopy() {
        return new TestSnapshot(this.timestamp, this.compositeCollection.cleanCopy());
    }

    @Override
    protected TestSnapshot dirtyCopy() {
        return new TestSnapshot(this.timestamp, this.compositeCollection.dirtyCopy());
    }

    @Override
    protected void update(TestSnapshot changes, boolean onlyReadLogs) {
        this.compositeCollection.update(changes.compositeCollection, onlyReadLogs);
    }

    @Override
    public TestSnapshot commit(TestSnapshot globalState) {
        return new TestSnapshot(globalState.timestamp + 1, this.compositeCollection
                .commit(globalState.compositeCollection));
    }

    public TransactionalRef<Integer> getRef1() {
        return this.compositeCollection.get(REF1);
    }

    public TransactionalRef<Integer> getRef2() {
        return this.compositeCollection.get(REF2);
    }

    public TransactionalMap<Integer, Integer> getMap() {
        return this.compositeCollection.get(MAP);
    }
}
