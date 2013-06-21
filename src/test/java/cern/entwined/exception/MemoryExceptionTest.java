/*
 * Entwined STM
 * 
 * (c) Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.entwined.exception;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for {@link MemoryException} class.
 * 
 * @author Ivan Koblik
 */
public class MemoryExceptionTest {

    @Test
    public void testMemoryException() {
        new MemoryException();
    }

    @Test
    public void testMemoryExceptionString() {
        MemoryException dut = new MemoryException("Test message");
        assertEquals("wrong message", "Test message", dut.getMessage());
    }

    @Test
    public void testMemoryExceptionStringThrowable() {
        Exception testEx = new Exception("Test exception");
        MemoryException dut = new MemoryException(testEx);
        assertEquals("wrong message", testEx, dut.getCause());
    }

    @Test
    public void testMemoryExceptionThrowable() {
        Exception testEx = new Exception("Test exception");
        MemoryException dut = new MemoryException("Test message", testEx);
        assertEquals("wrong message", testEx, dut.getCause());
        assertEquals("wrong message", "Test message, Test exception", dut.getMessage());
    }
}
