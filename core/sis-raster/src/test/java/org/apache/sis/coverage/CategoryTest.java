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

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.opengis.test.Assert.*;


/**
 * Tests {@link Category}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final strictfp class CategoryTest extends TestCase {
    /**
     * Small tolerance value for comparisons.
     */
    static final double EPS = 1E-9;

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
     * Tests that qualitative category produces the expected result.
     *
     * @throws TransformException if an error occurred while transforming a value.
     */
    @Test
    public void testQualitativeCategory() throws TransformException {
        final Random random = TestUtilities.createRandomNumberGenerator();
        final Set<Integer> padValues = new HashSet<>();
        for (int pass=0; pass<20; pass++) {
            final int      sample    = random.nextInt(20);
            final boolean  collision = padValues.contains(sample);
            final Category category  = new Category("Random", NumberRange.create(sample, true, sample, true), null, null, padValues);
            assertTrue("Allocated NaN ordinal", padValues.contains(sample));
            assertBoundEquals("range.minValue", sample, category.range.getMinValue());
            assertBoundEquals("range.maxValue", sample, category.range.getMaxValue());
            final MathTransform1D inverse = category.converted.transferFunction;
            for (int i=0; i<4; i++) {
                final float x = 100 * random.nextFloat();
                final float y = (float) category.transferFunction.transform(x);
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
     * Tests that quantitative category produces the expected result.
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

            assertBoundEquals("range.minValue",     lower,              category.range.getMinValue());
            assertBoundEquals("range.maxValue",     upper,              category.range.getMaxValue());
            assertBoundEquals("converted.minValue", lower*scale+offset, category.converted.range.getMinValue());
            assertBoundEquals("converted.maxValue", upper*scale+offset, category.converted.range.getMaxValue());

            final MathTransform1D inverse = category.converted.transferFunction;
            assertSame("inverse", inverse, category.transferFunction.inverse());

            for (int i=0; i<20; i++) {
                final double x = 100 * random.nextDouble();
                final double y = x*scale + offset;
                assertEquals("transferFunction", y, category.transferFunction.transform(x), EPS);
                assertEquals("inverse",          x, inverse.transform(y), EPS);
            }
        }
    }
}
