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
 * Implementations of this interface are capable of transactional behavior and can detect conflicts between local
 * changes and changes to the global state.
 * 
 * @param <T> The implementation class type.
 * @author Ivan Koblik
 */
interface Transactional<T> {

    /**
     * Performs conflict checks and if no conflicts detected returns new instance with all the pending changes applied.
     * 
     * @param globalState The current system's global state.
     * @return New instance with all the pending changes applied.
     * @throws ConflictException When conflicting changes detected.
     */
    public T commit(T globalState);
}
