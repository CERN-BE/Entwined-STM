/*
 * Entwined STM
 * 
 * © Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.stm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import cern.stm.TransactionAdapter;
import cern.stm.TransactionClosure;

/**
 * {@link TransactionClosure} unit tests.
 * 
 * @author Ivan Koblik
 */
public class TransactionClosureTest extends TransactionAdapterTest {

    @Override
    protected <T> TransactionAdapter<T> getTransactionAdapter() {
        return new TransactionClosure<T, Object>();
    }

    /**
     * Test method for {@link cern.stm.TransactionClosure#getResult()}.
     */
    @Test
    public void testGetResult() {
        TransactionClosure<Object, Integer> closure = new TransactionClosure<Object, Integer>();
        assertNull(closure.getResult());

        closure.setResult(100);
        assertEquals("The resulting value", (Integer) 100, closure.getResult());
    }

    /**
     * Test method for {@link cern.stm.TransactionClosure#setResult(java.lang.Object)}.
     */
    @Test
    public void testSetResult() {
        TransactionClosure<Object, Integer> closure = new TransactionClosure<Object, Integer>();

        closure.setResult(100);
        assertEquals("The resulting value", (Integer) 100, closure.getResult());

        closure.setResult(null);
        assertNull("The resulting value", closure.getResult());
    }
}
