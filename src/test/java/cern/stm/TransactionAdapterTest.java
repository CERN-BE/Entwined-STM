/*
 * Entwined STM
 * 
 * © Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.stm;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import cern.stm.TransactionAdapter;

/**
 * {@link TransactionAdapter} unit tests.
 * 
 * @author Ivan Koblik
 */
public class TransactionAdapterTest {

    protected <T> TransactionAdapter<T> getTransactionAdapter() {
        return new TransactionAdapter<T>();
    }

    /**
     * Test method for {@link cern.stm.TransactionAdapter#committed(java.lang.Object)}.
     */
    @Test
    public void testCommitted() throws Exception {
        getTransactionAdapter().committed(null);
    }

    /**
     * Test method for {@link cern.stm.TransactionAdapter#run(java.lang.Object)}.
     */
    @Test
    public void testRun() throws Exception {
        assertTrue("By default should return true", getTransactionAdapter().run(null));
    }

}
