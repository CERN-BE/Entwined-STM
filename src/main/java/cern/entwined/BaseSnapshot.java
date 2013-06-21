/*
 * Entwined STM
 * 
 * (c) Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.entwined;

/**
 * A memory snapshot composed of client specific data of type V and of global storage. Global storage is used by
 * {@link GlobalReference}s to keep session wide data.
 * 
 * @author Ivan Koblik
 */
class BaseSnapshot<V extends SemiPersistent<V>> extends Snapshot<BaseSnapshot<V>> {

    /**
     * Map of globally visible references. It is used as a storage for {@link GlobalReference}s.
     */
    private final TransactionalMap<Long, Object> globalMap;

    /**
     * Client specific snapshot.
     */
    private final V clientData;

    /**
     * Creates the {@link BaseSnapshot} with the new timestamp.
     * 
     * @param timestamp The newly created snapshots timestamp.
     * @param snapshot Client specific data.
     * @see Snapshot#Snapshot(long) For more details on snapshot value see {@link Snapshot#Snapshot(long)}.
     */
    public BaseSnapshot(long timestamp, V snapshot) {
        this(timestamp, snapshot, new TransactionalMap<Long, Object>());
    }

    /**
     * Creates the {@link BaseSnapshot} passing timestamp to {@link Snapshot}'s constructor.
     * 
     * @param timestamp The newly created snapshots timestamp.
     * @param snapshot Client specific data.
     * @param globalMap The map of global references.
     * @see Snapshot#Snapshot(long) For more details on snapshot value see {@link Snapshot#Snapshot(long)}.
     */
    public BaseSnapshot(long timestamp, V snapshot, TransactionalMap<Long, Object> globalMap) {
        super(timestamp);
        Utils.checkNull("Snapshot", snapshot);
        Utils.checkNull("Global map", globalMap);
        this.clientData = snapshot;
        this.globalMap = globalMap;
    }

    /**
     * Returns the global references map.
     * 
     * @return The map of global references.
     * @see GlobalReference
     */
    protected TransactionalMap<Long, Object> getGlobalMap() {
        return this.globalMap;
    }

    /**
     * Returns the client snapshot.
     * 
     * @return The client snapshot.
     */
    protected V getClientData() {
        return this.clientData;
    }

    @Override
    protected BaseSnapshot<V> cleanCopy() {
        return new BaseSnapshot<V>(this.timestamp, this.clientData.cleanCopy(), this.globalMap.cleanCopy());
    }

    @Override
    protected BaseSnapshot<V> dirtyCopy() {
        return new BaseSnapshot<V>(this.timestamp, this.clientData.dirtyCopy(), this.globalMap.dirtyCopy());
    }

    @Override
    protected void update(BaseSnapshot<V> changes, boolean onlyReadLogs) {
        Utils.checkNull("Changes", changes);
        if (this.timestamp != changes.timestamp) {
            throw new IllegalArgumentException("Updates are only possible from dirty copies of this snapshot");
        }
        this.clientData.update(changes.clientData, onlyReadLogs);
        this.globalMap.update(changes.globalMap, onlyReadLogs);
    }

    @Override
    public BaseSnapshot<V> commit(BaseSnapshot<V> globalState) {
        Utils.checkNull("Global state", globalState);
        return new BaseSnapshot<V>(this.timestamp + 1, this.clientData.commit(globalState.clientData),
                this.globalMap.commit(globalState.globalMap));
    }
}
