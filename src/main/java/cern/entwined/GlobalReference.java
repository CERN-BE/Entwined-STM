/*
 * Entwined STM
 * 
 * (c) Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.entwined;

import cern.entwined.exception.NoTransactionException;

/**
 * This class enables aggregation of transactional references within non transactional entities. Global reference must
 * be treated as transactional and thread local.
 * 
 * @param <T> The reference value type.
 * @author Ivan Koblik
 */
public class GlobalReference<T> {

    /**
     * Reference to the {@link Memory} class. It is used to access current snapshot.
     */
    private final Memory<?> memory;

    /**
     * {@link GlobalReference} unique identifier.
     */
    private final Long id;

    /**
     * Creates an instance of {@link GlobalReference} using given memory and object.
     * 
     * @param memory The instance of {@link Memory} class.
     * @param object The globally referenced object of type T.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public GlobalReference(final Memory<?> memory, final T object) {
        Utils.checkNull("Memory", memory);
        this.memory = memory;
        this.id = memory.getNextId();
        try {
            memory.getBaseSnapshot().getGlobalMap().put(id, object);
        } catch (NoTransactionException ex) {
            // There is no transaction running, crating a small one just to initialize the object.
            Transaction atomic = new TransactionAdapter<Object>() {
                @Override
                public boolean run(Object snapshot) {
                    memory.getBaseSnapshot().getGlobalMap().put(id, object);
                    return true;
                }
            };
            this.memory.runTransaction(atomic);
        }
    }

    /**
     * Retrieves the globally referenced object.
     * 
     * @return The globally referenced object.
     */
    @SuppressWarnings("unchecked")
    public T get() {
        try {
            return (T) memory.getBaseSnapshot().getGlobalMap().get(this.id);
        } catch (NoTransactionException ex) {
            throw new NoTransactionException("Cannot read from GlobalReference outside of a transaction", ex);
        }
    }

    /**
     * Sets the globally referenced object.
     * 
     * @param newValue The new value.
     * @return The replaced value.
     */
    @SuppressWarnings("unchecked")
    public T set(T newValue) {
        try {
            return (T) memory.getBaseSnapshot().getGlobalMap().put(this.id, newValue);
        } catch (NoTransactionException ex) {
            throw new NoTransactionException("Cannot write to GlobalReference outside of a transaction", ex);
        }
    }

    /**
     * Runs a small transaction that removes the referenced object from the global map.
     * <p>
     * May lead to memory leaks if transaction fails too many times. But, in theory, this transaction should never fail
     * as it removes an element that no one else would ever access.
     * 
     * @see java.lang.Object#finalize()
     */
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void finalize() throws Throwable {
        try {
            if (null != memory) {
                Transaction cleanup = new TransactionAdapter() {
                    @Override
                    public boolean run(Object snapshot) {
                        memory.getBaseSnapshot().getGlobalMap().remove(id);
                        return true;
                    }
                };
                memory.runTransaction(cleanup);
            }
        } finally {
            super.finalize();
        }
    }

    /**
     * Returns the reference unique id. Method is only for testing purposes.
     * 
     * @return The reference id.
     */
    Long getId() {
        return this.id;
    }
}
