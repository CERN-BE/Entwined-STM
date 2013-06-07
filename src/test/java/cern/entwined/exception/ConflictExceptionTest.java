/*
 * Entwined STM
 * 
 * © Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.entwined.exception;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for {@link ConflictException} class.
 * 
 * @author Ivan Koblik
 */
public class ConflictExceptionTest {

    @Test
    public void testConflictException() {
        new ConflictException();
    }

    @Test
    public void testConflictExceptionString() {
        ConflictException dut = new ConflictException("Test message");
        assertEquals("wrong message", "Test message", dut.getMessage());
    }

    @Test
    public void testConflictExceptionStringThrowable() {
        Exception testEx = new Exception("Test exception");
        ConflictException dut = new ConflictException(testEx);
        assertEquals("wrong message", testEx, dut.getCause());
    }

    @Test
    public void testConflictExceptionThrowable() {
        Exception testEx = new Exception("Test exception");
        ConflictException dut = new ConflictException("Test message", testEx);
        assertEquals("wrong message", testEx, dut.getCause());
        assertEquals("wrong message", "Test message, Test exception", dut.getMessage());
    }
}
