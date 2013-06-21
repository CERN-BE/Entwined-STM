/*
 * Entwined STM
 * 
 * (c) Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.entwined;

import static cern.entwined.Utils.NOT_EMPTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

/**
 * {@link Utils} unit tests.
 * 
 * @author Ivan Koblik
 */
public class UtilsTest {

    @Test
    @Ignore
    public void testCheckNullStringT() {
        fail("Not yet implemented");
    }

    @Test
    @Ignore
    public void testCheckNullStringString() {
        fail("Not yet implemented");
    }

    @Test
    @Ignore
    public void testCheckEmpty() {
        fail("Not yet implemented");
    }

    @Test
    @Ignore
    public void testHasCause() {
        fail("Not yet implemented");
    }

    @Test
    @Ignore
    public void testCheckNullState() {
        fail("Not yet implemented");
    }

    @Test
    public void testRound_exactFactor() {
        int exponent = -2;
        double value = 0.5; // 2^-1
        assertEquals("Rounded 0.5 to 2^-2", value, Utils.round(value, exponent), 0d);
    }

    @Test
    public void testRound_exactFactorFullPrecision() {
        int exponent = -2;
        double value = 0.25; // 2^-2
        assertEquals("Rounded 0.25 to 2^-2", value, Utils.round(value, exponent), 0d);
    }

    @Test
    public void testRound_inexactFactor() {
        int exponent = -2;
        double value = 0.53; // ~= 2^-1 + 2^-5; (2^-5 == 0.03125)
        assertEquals("Rounding 0.53 to 2^-2", 0.5, Utils.round(value, exponent), 0d);
    }

    @Test
    public void testRound_inexactFactorFullPrecision() {
        int exponent = -2;
        double value = 0.25 + 0.124; // ~= 2^-2 + k; (2^-3 - 2^-9) = k < (2^-3)
        assertEquals("Rounding 0.25 + 0.124 to 2^-2", 0.25, Utils.round(value, exponent), 0d);
    }

    @Test
    public void testRound_inexactFactorFullPrecisionWithCarry() {
        int exponent = -2;
        double value = 0.25 + 0.125; // ~= 2^-2 + 2^-3
        assertEquals("Rounding 0.25 + 0.125 to 2^-2", 0.5, Utils.round(value, exponent), 0d);
    }

    @Test
    public void testRound_highPrecision() {
        int exponent = (int) (Math.log(1e-10) / Math.log(2));
        double expected = Math.scalb(1, exponent);
        double noise = Math.scalb(1, exponent - 1) - Math.scalb(1, exponent - 2);
        double value = expected + noise;
        assertEquals("Rounding exp=log(2,1e-10); 2^exp + 2^(exp-1) - 2^(exp-2) to exp", expected, Utils.round(value,
                exponent), 0d);
    }

    @Test
    public void testRound_highPrecisionWithCarry() {
        int exponent = (int) (Math.log(1e-10) / Math.log(2));
        double expected = Math.scalb(1, exponent);
        // Adding lower power of two to be sure that carry will be performed
        double noise = Math.scalb(1, exponent - 1) + Math.scalb(1, exponent - 2);
        double value = expected + noise;
        assertEquals("Rounding exp=log(2,1e-10); 2^exp + 2^(exp-1) + 2^(exp-2) to exp", 2 * expected, Utils.round(
                value, exponent), 0d);
    }

    @Test
    public void testRound_highPrecisionComparedWithTolerance() {
        int exponent = (int) (Math.log(1e-10) / Math.log(2));
        double expected = Math.scalb(1, exponent);
        double noise1 = Math.scalb(1, exponent - 1) - Math.scalb(1, exponent - 2);
        double noise2 = Math.scalb(1, exponent - 1) - Math.scalb(1, exponent - 3);
        double value1 = expected + noise1;
        double value2 = expected + noise2;
        double difference = Math.abs(Utils.round(value1, exponent) - Utils.round(value2, exponent));
        assertTrue(difference < Math.scalb(1, exponent));
    }

    @Test
    public void testRound_highPrecisionWithCarryComparedWithTolerance() {
        int exponent = (int) (Math.log(1e-10) / Math.log(2));
        double expected = Math.scalb(1, exponent);
        double noise1 = Math.scalb(1, exponent - 1) + Math.scalb(1, exponent - 2);
        double noise2 = Math.scalb(1, exponent - 1) + Math.scalb(1, exponent - 3);
        double value1 = expected + noise1;
        double value2 = expected + noise2;
        double difference = Math.abs(Utils.round(value1, exponent) - Utils.round(value2, exponent));
        assertTrue(difference < Math.scalb(1, exponent));
    }

    private static final String[] ARRAY_LIST = { "foo", "bar", "baz" };
    private static final String[] EMPTY_ARRAY_LIST = {};
    private static final String[] NULL_ARRAY_LIST = { null };
    private static final String SEPARATOR = ",";
    private static final String TEXT_LIST_NOSEP = "foobarbaz";
    private static final String TEXT_LIST = "foo,bar,baz";

    @Test
    public void testJoin_IterableString() {
        assertEquals("null", Utils.join((Iterable<?>) null, null));
        assertEquals(TEXT_LIST_NOSEP, Utils.join(Arrays.asList(ARRAY_LIST), null));
        assertEquals(TEXT_LIST_NOSEP, Utils.join(Arrays.asList(ARRAY_LIST), ""));
        assertEquals("foo", Utils.join(Collections.singleton("foo"), "x"));
        assertEquals("foo", Utils.join(Collections.singleton("foo"), null));

        assertEquals("null", Utils.join(Arrays.asList(NULL_ARRAY_LIST), null));

        assertEquals("", Utils.join(Arrays.asList(EMPTY_ARRAY_LIST), null));
        assertEquals("", Utils.join(Arrays.asList(EMPTY_ARRAY_LIST), ""));
        assertEquals("", Utils.join(Arrays.asList(EMPTY_ARRAY_LIST), SEPARATOR));

        assertEquals(TEXT_LIST, Utils.join(Arrays.asList(ARRAY_LIST), SEPARATOR));
    }

    @Test
    public void testGetUnique_OnSingleElementCollection() {
        Collection<String> col = Collections.singleton("unique");
        assertEquals("The unique value in the collection", "unique", Utils.getUnique(col));
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("unchecked")
    public void testGetUnique_FailOnEmptyCollection() {
        Utils.getUnique(Collections.EMPTY_SET);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetUnique_FailOnBiggerCollection() {
        Utils.getUnique(ImmutableSet.of(1, 2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetUnique_FailOnNull() {
        Utils.getUnique(null);
    }

    @Test
    public void testNotEmpty() {
        assertTrue("On 123", NOT_EMPTY.apply("123"));
        assertFalse("On null", NOT_EMPTY.apply(null));
        assertFalse("On \"\"", NOT_EMPTY.apply(""));
        assertFalse("On \"   \"", NOT_EMPTY.apply("   "));
    }
}
