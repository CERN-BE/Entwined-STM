/*
 * Entwined STM
 * 
 * © Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.stm.exception;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

import cern.stm.exception.NoTransactionException;

/**
 * Unit tests for {@link NoTransactionException} class.
 * 
 * @author Ivan Koblik
 */
public class NoTransactionExceptionTest {

    @Test
    public void testNoTransactionException() {
        new NoTransactionException();
    }

    @Test
    public void testNoTransactionExceptionString() {
        NoTransactionException dut = new NoTransactionException("Test message");
        assertEquals("wrong message", "Test message", dut.getMessage());
    }

    @Test
    public void testNoTransactionExceptionStringThrowable() {
        Exception testEx = new Exception("Test exception");
        NoTransactionException dut = new NoTransactionException(testEx);
        assertEquals("wrong message", testEx, dut.getCause());
    }

    @Test
    public void testNoTransactionExceptionThrowable() {
        Exception testEx = new Exception("Test exception");
        NoTransactionException dut = new NoTransactionException("Test message", testEx);
        assertEquals("wrong message", testEx, dut.getCause());
        assertEquals("wrong message", "Test message, Test exception", dut.getMessage());
    }
}
