/*
 * Entwined STM
 * 
 * © Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.entwined;

import cern.entwined.exception.InvocationException;

/**
 * The transaction callback interface. Implementation of this interface must be passed to {@link Memory} object.
 * 
 * @param <T> The client {@link Snapshot} type.
 * @author Ivan Koblik
 */
public interface Transaction<T> {
    /**
     * Implementation of this method must include in-transactional code and manipulations with shared memory. Any
     * exception thrown by this method will cancel the transaction.
     * 
     * @param data The shared memory data.
     * @return the commit request <code>true</code> to commit, <code>false</code> to rollback.
     * @throws Exception Runtime exception will be propagated unchanged while checked exception will be wrapped into
     *             {@link InvocationException}.
     */
    public boolean run(T data) throws Exception;

    /**
     * Implementation of this method must contain all the I/O and post transactional processing. Exceptions thrown by
     * this method will not cancel the transaction.
     * 
     * @param data Shared memory data after commit.
     * @throws Exception Runtime exception will be propagated unchanged while checked exception will be wrapped into
     *             {@link InvocationException}.
     */
    public void committed(T data) throws Exception;
}
