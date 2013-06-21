/*
 * Entwined STM
 * 
 * (c) Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.entwined;

import cern.entwined.exception.ConflictException;

/**
 * Transactional implementation of reference interface.
 * 
 * @param <T> The referenced value type.
 * @author Ivan Koblik
 */
public class TransactionalRef<T> extends SemiPersistent<TransactionalRef<T>> implements Ref<T> {

    /**
     * This flag signifies whether referenced value was accessed or not.
     */
    private boolean accessed = false;

    /**
     * This flag signifies whether referenced value was updated or not.
     */
    private boolean updated = false;

    /**
     * Value that was a part of global state when transaction began.
     */
    private final T sourceValue;

    /**
     * The referenced value.
     */
    private T value = null;

    /**
     * Constructs the reference object with null as value.
     */
    public TransactionalRef() {
        this(null);
    }

    /**
     * Constructs the reference object with the given value.
     * 
     * @param value The initial value.
     */
    public TransactionalRef(T value) {
        this.sourceValue = value;
        this.value = this.sourceValue;
    }

    /*
     * (non-Javadoc)
     * 
     * @see cern.oasis.server.stm.Reference#associate(java.lang.Object)
     */
    @Override
    public T assoc(T newValue) {
        this.accessed = true;
        this.updated = true;
        T oldValue = this.value;
        this.value = newValue;
        return oldValue;
    }

    /*
     * (non-Javadoc)
     * 
     * @see cern.oasis.server.stm.Reference#dereference()
     */
    @Override
    public T deref() {
        this.accessed = true;
        return this.value;
    }

    /*
     * (non-Javadoc)
     * 
     * @see cern.oasis.server.stm.SemiPersistent#cleanCopy()
     */
    @Override
    public TransactionalRef<T> cleanCopy() {
        return new TransactionalRef<T>(this.sourceValue);
    }

    /*
     * (non-Javadoc)
     * 
     * @see cern.oasis.server.stm.SemiPersistent#dirtyCopy()
     */
    @Override
    protected TransactionalRef<T> dirtyCopy() {
        TransactionalRef<T> copy = new TransactionalRef<T>(this.sourceValue);
        copy.accessed = this.accessed;
        copy.updated = this.updated;
        copy.value = this.value;
        return copy;
    }

    /*
     * (non-Javadoc)
     * 
     * @see cern.oasis.server.stm.SemiPersistent#update(java.lang.Object, boolean)
     */
    @Override
    protected void update(TransactionalRef<T> changes, boolean onlyReadLogs) {
        Utils.checkNull("Local changes", changes);
        if (changes.sourceValue != this.sourceValue) {
            throw new IllegalArgumentException("Updates are only possible for references with the same source");
        }
        this.accessed = changes.accessed;
        if (!onlyReadLogs) {
            this.updated = changes.updated;
            this.value = changes.value;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see cern.oasis.server.stm.ConflictAware#commit(java.lang.Object)
     */
    @Override
    public TransactionalRef<T> commit(TransactionalRef<T> globalState) {
        Utils.checkNull("Transactional reference", globalState);
        if (globalState.accessed) {
            throw new IllegalArgumentException("Global state must be commited before calling this method");
        }
        if (this.accessed && globalState.sourceValue != this.sourceValue) {
            throw new ConflictException("Conflicting update detected");
        }

        if (!this.updated) {
            // Return current global state if value hasn't been changed.
            return globalState;
        } else {
            // Return updated global state with the local value.
            return new TransactionalRef<T>(this.value);
        }
    }
}
