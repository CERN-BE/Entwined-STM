/*
 * Entwined STM
 * 
 * © Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.stm;

/**
 * The root to all the transactionally manged collections.
 * 
 * @param <T> The implementation collection type.
 * @author Ivan Koblik
 */
public abstract class SemiPersistent<T> implements Transactional<T> {

    /**
     * Creates a new transactional entity from the current one discarding all the local changes.
     * 
     * @return The new entity with no local changes.
     */
    protected abstract T cleanCopy();

    /**
     * Copies <b>references</b> to the source data along with local changes and read/write logs. <br>
     * <b>Note:</b> It is very important to copy source data by reference, otherwise
     * {@link SemiPersistent#update(Object)} will not work.
     * 
     * @return Dirty copy of this collection, with all the local changes and read/write logs preserved.
     */
    protected abstract T dirtyCopy();

    /**
     * Applies passed change logs of the argument to <code>this</code> collection. Argument must had been originally
     * branched from this collection with {@link SemiPersistent#dirtyCopy()}. <br>
     * <b>Note:</b> Any changes done to this collections after branching will be discarded.
     * 
     * @param changes The branched collection possibly with updates.
     * @param onlyReadLogs If true only read logs are updated.
     */
    protected abstract void update(T changes, boolean onlyReadLogs);
}
