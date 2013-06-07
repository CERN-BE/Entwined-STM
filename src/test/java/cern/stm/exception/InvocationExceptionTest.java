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

import cern.stm.exception.InvocationException;
import cern.stm.exception.MemoryException;

/**
 * Unit tests for {@link MemoryException} class.
 * 
 * @author Ivan Koblik
 */
public class InvocationExceptionTest {

    @Test
    public void testMemoryException() {
        new InvocationException();
    }

    @Test
    public void testMemoryExceptionString() {
        InvocationException dut = new InvocationException("Test message");
        assertEquals("wrong message", "Test message", dut.getMessage());
    }

    @Test
    public void testMemoryExceptionStringThrowable() {
        Exception testEx = new Exception("Test exception");
        InvocationException dut = new InvocationException(testEx);
        assertEquals("wrong message", testEx, dut.getCause());
    }

    @Test
    public void testMemoryExceptionThrowable() {
        Exception testEx = new Exception("Test exception");
        InvocationException dut = new InvocationException("Test message", testEx);
        assertEquals("wrong message", testEx, dut.getCause());
        assertEquals("wrong message", "Test message, Test exception", dut.getMessage());
    }
}
