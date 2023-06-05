/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.coverage;

import java.util.Random;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.opengis.test.Assert.assertInstanceOf;


/**
 * Tests {@link Category}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   1.0
 */
public final class CategoryTest extends TestCase {
    /**
     * Small tolerance value for comparisons.
     */
    static final double EPS = 1E-9;

    /**
     * Asserts that the given range contains NaN values.
     */
    private static void assertNaN(final String message, final NumberRange<?> range) {
        final double value = range.getMinDouble();
        assertTrue(message, Double.isNaN(value));
        assertEquals(message, Double.doubleToRawLongBits(value), Double.doubleToRawLongBits(range.getMaxDouble()));
    }

    /**
     * Checks if a {@link Comparable} is a number identical to the supplied integer value.
     */
    private static void assertBoundEquals(final String message, final int expected, final Comparable<?> actual) {
        assertInstanceOf(message, Integer.class, actual);
        assertEquals(message, expected, ((Number) actual).intValue());
    }

    /**
     * Checks if a {@link Comparable} is a number identical to the supplied float value.
     */
    private static void assertBoundEquals(final String message, final double expected, final Comparable<?> actual) {
        assertInstanceOf(message, Double.class, actual);
        final double value = ((Number) actual).doubleValue();
        if (Double.isNaN(expected)) {
            assertEquals(message, Double.doubleToRawLongBits(expected), Double.doubleToRawLongBits(value));
        } else {
            assertEquals(message, expected, value, EPS);
        }
    }

    /**
     * Tests that construction of qualitative category produces expected properties.
     * This method tests also the conversions of some random sample values.
     *
     * @throws TransformException if an error occurred while transforming a value.
     */
    @Test
    public void testQualitativeCategory() throws TransformException {
        final Random random = TestUtilities.createRandomNumberGenerator();
        final ToNaN toNaN = new ToNaN();
        for (int pass=0; pass<20; pass++) {
            final int      sample    = random.nextInt(20);
            final boolean  collision = toNaN.contains(sample);
            final Category category  = new Category("Random", NumberRange.create(sample, true, sample, true), null, null, toNaN);
            assertTrue("Allocated NaN ordinal", toNaN.contains(sample));
            /*
             * Verify properties on the category that we created.
             * The sample values are integers in our test.
             */
            assertEquals     ("name",           "Random",       String.valueOf(category.name));
            assertEquals     ("name",           "Random",       String.valueOf(category.getName()));
            assertBoundEquals("range.minValue", sample,         category.range.getMinValue());
            assertBoundEquals("range.maxValue", sample,         category.range.getMaxValue());
            assertSame       ("sampleRange",    category.range, category.getSampleRange());
            assertFalse      ("measurementRange",               category.getMeasurementRange().isPresent());
            assertFalse      ("toConverse.isIdentity",          category.toConverse.isIdentity());
            assertFalse      ("transferFunction",               category.getTransferFunction().isPresent());
            assertFalse      ("isQuantitative",                 category.isQuantitative());
            /*
             * Verify properties on the converse category. Values shall be NaN
             * and the transfer function shall be absent.
             */
            final Category converse = category.converse;
            assertNotSame("converse",           category, converse);
            assertSame   ("converse.converse",  category, converse.converse);
            assertSame   ("converted",          converse, category.converted());
            assertSame   ("converted",          converse, converse.converted());
            assertEquals ("name",               "Random", String.valueOf(converse.name));
            assertEquals ("name",               "Random", String.valueOf(converse.getName()));
            assertNaN    ("range",                        converse.range);
            assertNotNull("sampleRange",                  converse.getSampleRange());
            assertFalse  ("measurementRange",             category.getMeasurementRange().isPresent());
            assertFalse  ("toConverse.isIdentity",        converse.toConverse.isIdentity());
            assertFalse  ("transferFunction",             converse.getTransferFunction().isPresent());
            assertFalse  ("isQuantitative",               converse.isQuantitative());
            /*
             * Test sample values conversions. They are expected to produce NaN values and
             * the converter shall be able to go back to original values from those NaNs.
             */
            final MathTransform1D inverse = converse.toConverse;
            for (int i=0; i<4; i++) {
                final float x = 100 * random.nextFloat();
                final float y = (float) category.toConverse.transform(x);
                assertTrue("isNaN", Float.isNaN(y));
                final int ordinal = MathFunctions.toNanOrdinal(y);
                if (collision) {
                    assertNotEquals("ordinal", sample, ordinal);
                } else {
                    assertEquals("ordinal", sample, ordinal);
                }
                assertEquals("inverse", sample, (float) inverse.transform(y), (float) STRICT);
            }
        }
    }

    /**
     * Tests that construction of quantitative category produces expected properties.
     * This method tests also the conversions of some random sample values.
     *
     * @throws TransformException if an error occurred while transforming a value.
     */
    @Test
    public void testQuantitativeCategory() throws TransformException {
        final Random random = TestUtilities.createRandomNumberGenerator();
        for (int pass=0; pass<20; pass++) {
            final int     lower = random.nextInt(64);
            final int     upper = random.nextInt(128) + lower+1;
            final double  scale = 10*random.nextDouble() + 0.1;         // Must be positive for this test.
            final double offset = 10*random.nextDouble() - 5.0;
            final Category category = new Category("Random", NumberRange.create(lower, true, upper, true),
                    (MathTransform1D) MathTransforms.linear(scale, offset), null, null);

            final Category converse = category.converse;
            assertNotSame    ("converse",           category, converse);
            assertSame       ("converse.converse",  category, converse.converse);
            assertSame       ("converted",          converse, category.converted());
            assertSame       ("converted",          converse, converse.converted());
            assertEquals     ("name",               "Random",            String.valueOf(category.name));
            assertEquals     ("name",               "Random",            String.valueOf(converse.name));
            assertEquals     ("name",               "Random",            String.valueOf(category.getName()));
            assertEquals     ("name",               "Random",            String.valueOf(converse.getName()));
            assertEquals     ("minimum",            lower,               category.range.getMinDouble(true), STRICT);
            assertEquals     ("maximum",            upper,               category.range.getMaxDouble(true), STRICT);
            assertEquals     ("minimum",            lower*scale+offset,  converse.range.getMinDouble(true), EPS);
            assertEquals     ("maximum",            upper*scale+offset,  converse.range.getMaxDouble(true), EPS);
            assertBoundEquals("range.minValue",     lower,               category.range.getMinValue());
            assertBoundEquals("range.maxValue",     upper,               category.range.getMaxValue());
            assertSame       ("sampleRange",        category.range,      category.getSampleRange());
            assertSame       ("sampleRange",        converse.range,      converse.getSampleRange());
            assertSame       ("measurementRange",   converse.range,      category.getMeasurementRange().get());
            assertSame       ("measurementRange",   converse.range,      converse.getMeasurementRange().get());
            assertSame       ("transferFunction",   category.toConverse, category.getTransferFunction().get());
            assertNotSame    ("transferFunction",   converse.toConverse, converse.getTransferFunction().get());
            assertTrue       ("transferFunction",                        converse.getTransferFunction().get().isIdentity());
            assertFalse      ("toConverse.isIdentity",                   category.toConverse.isIdentity());
            assertFalse      ("toConverse.isIdentity",                   converse.toConverse.isIdentity());
            assertTrue       ("isQuantitative",                          category.isQuantitative());
            assertTrue       ("isQuantitative",                          converse.isQuantitative());
            /*
             * Test sample values conversions.
             */
            final MathTransform1D inverse = category.converse.toConverse;
            assertSame("inverse", inverse, category.toConverse.inverse());
            for (int i=0; i<20; i++) {
                final double x = 100 * random.nextDouble();
                final double y = x*scale + offset;
                assertEquals("toConverse", y, category.toConverse.transform(x), EPS);
                assertEquals("inverse",    x, inverse.transform(y), EPS);
            }
        }
    }

    /**
     * Creates a category for data that are already real values.
     */
    @Test
    public void testConvertedCategory() {
        final Random random = TestUtilities.createRandomNumberGenerator();
        for (int pass=0; pass<3; pass++) {
            final double lower = random.nextDouble() * 5;
            final double upper = random.nextDouble() * 10 + lower;
            final Category category = new Category("Random", NumberRange.create(lower, true, upper, true),
                    (MathTransform1D) MathTransforms.identity(1), null, null);

            assertSame       ("converse",           category,            category.converse);
            assertEquals     ("name",               "Random",            String.valueOf(category.name));
            assertEquals     ("name",               "Random",            String.valueOf(category.getName()));
            assertBoundEquals("range.minValue",     lower,               category.range.getMinValue());
            assertBoundEquals("range.maxValue",     upper,               category.range.getMaxValue());
            assertSame       ("sampleRange",        category.range,      category.getSampleRange());
            assertSame       ("measurementRange",   category.range,      category.getMeasurementRange().get());
            assertSame       ("transferFunction",   category.toConverse, category.getTransferFunction().get());
            assertTrue       ("toConverse.isIdentity",                   category.toConverse.isIdentity());
            assertTrue       ("isQuantitative",                          category.isQuantitative());
        }
    }

    /**
     * Tests a category with a NaN value.
     */
    @Test
    public void testCategoryNaN() {
        final Category category = new Category("NaN", new NumberRange<>(Float.class, Float.NaN, true, Float.NaN, true), null, null, null);
        final NumberRange<?> range = category.getSampleRange();
        assertSame  ("converse",       category,   category.converse);
        assertEquals("name",           "NaN",      String.valueOf(category.name));
        assertEquals("name",           "NaN",      String.valueOf(category.getName()));
        assertNaN   ("sampleRange",                category.range);
        assertEquals("range.minValue", Float.NaN,  range.getMinValue());
        assertEquals("range.maxValue", Float.NaN,  range.getMaxValue());
        assertFalse ("measurementRange",           category.getMeasurementRange().isPresent());
        assertFalse ("transferFunction",           category.getTransferFunction().isPresent());
        assertTrue  ("toConverse.isIdentity",      category.toConverse.isIdentity());
        assertFalse ("isQuantitative",             category.isQuantitative());
    }
}
