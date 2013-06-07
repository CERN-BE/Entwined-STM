/*
 * Entwined STM
 * 
 * © Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.stm;

/**
 * An extension of {@link TransactionAdapter} with ability to return result of type V.
 * 
 * @author Ivan Koblik
 */
public class TransactionClosure<T, V> extends TransactionAdapter<T> {

    /**
     * The result of the transaction execution.
     */
    private V result;

    /**
     * Gets the result of the transaction execution.
     * <p>
     * It doesn't matter if transaction succeeds or fails, this can be used in either case.
     * 
     * @return The result of the transaction execution.
     */
    public V getResult() {
        return result;
    }

    /**
     * Sets the result of the transaction execution.
     * 
     * @param result The result of the transaction execution.
     */
    protected void setResult(V result) {
        this.result = result;
    }
}
