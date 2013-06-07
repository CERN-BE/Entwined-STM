/*
 * Entwined STM
 * 
 * © Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.stm;

/**
 * Empty implementation of {@link Transaction} interface.
 * 
 * @param <T> The client {@link Snapshot} type.
 * @author Ivan Koblik
 */
public class TransactionAdapter<T> implements Transaction<T> {
    /*
     * (non-Javadoc)
     * 
     * @see cern.oasis.server.stm.Transaction#committed(java.lang.Object)
     */
    @Override
    public void committed(T data) throws Exception {
        // Nothing here
    };

    /**
     * Implements the {@link Transaction#run(Object)} method by always returning true, to guarantee consequent
     * invocation of {@link Transaction#committed(Object)}.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public boolean run(T data) throws Exception {
        return true;
    };
}
