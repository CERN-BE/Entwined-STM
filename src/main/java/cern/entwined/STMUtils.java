/*
 * Entwined STM
 * 
 * (c) Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.entwined;

import cern.entwined.exception.MemoryException;

/**
 * A set of methods aimed at facilitation of client's interactions with the transactional memory.
 * 
 * @author Ivan Koblik
 */
public abstract class STMUtils {

    /**
     * Either updates the {@link GlobalReference} and returns it or, if the source is <code>null</code>, creates a new
     * {@link GlobalReference} with the given value and returns it.
     * 
     * @param <T> The referenced value type.
     * @param memory The transactional memory.
     * @param globalReference The global reference to be updated or null.
     * @param value The value to be assigned.
     * @return Either same {@link GlobalReference} as in the argument but with value changed or a new
     *         {@link GlobalReference} with the given value.
     */
    public static <T> GlobalReference<T> reference(Memory<?> memory, GlobalReference<T> globalReference, T value) {
        Utils.checkNull("Memory", memory);
        if (null != globalReference) {
            globalReference.set(value);
            return globalReference;
        } else {
            globalReference = new GlobalReference<T>(memory, value);
            return globalReference;
        }
    }

    /**
     * Returns referenced value or null if the reference is null.
     * 
     * @param <T> The referenced value type.
     * @param reference The {@link GlobalReference} to dereference.
     * @return The referenced value or null.
     */
    public static <T> T dereference(GlobalReference<T> reference) {
        if (null != reference) {
            return reference.get();
        } else {
            return null;
        }
    }

    /**
     * Returns <code>true</code> if the current code is being executed within a transaction.
     * 
     * @param <T> The memory root type.
     * @param memory The STM {@link Memory} instance.
     * @return <code>true</code> if called from within a transaction.
     * @throws MemoryException if this method is called from a committed block.
     */
    public static <T extends SemiPersistent<T>> boolean inTransaction(Memory<T> memory) {
        Utils.checkNull("Memory", memory);
        final boolean inTransactionBox[] = new boolean[] { true };
        memory.runTransaction(new TransactionAdapter<T>() {
            public void committed(T data) {
                // Will be executed only if this is an outer transaction's committed block.
                inTransactionBox[0] = false;
            };
        });
        return inTransactionBox[0];
    }
}
