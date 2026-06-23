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
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.Units;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.util.ComparisonMode;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link Category}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Alexis Manin (Geomatys)
 */
@SuppressWarnings("exports")
public final class CategoryTest extends TestCase {
    /**
     * Small tolerance value for comparisons.
     */
    static final double EPS = 1E-9;

    /**
     * Creates a new test case.
     */
    public CategoryTest() {
    }

    /**
     * Asserts that the given range contains NaN values.
     */
    private static void assertNaN(final String message, final NumberRange<?> range) {
        final double value = range.getMinDouble();
        assertTrue(Double.isNaN(value), message);
        assertEquals(Double.doubleToRawLongBits(value), Double.doubleToRawLongBits(range.getMaxDouble()), message);
    }

    /**
     * Checks if a {@link Comparable} is a number identical to the supplied integer value.
     */
    private static void assertBoundEquals(final String message, final int expected, final Comparable<?> actual) {
        assertEquals(expected, assertInstanceOf(Integer.class, actual, message).intValue(), message);
    }

    /**
     * Checks if a {@link Comparable} is a number identical to the supplied float value.
     */
    private static void assertBoundEquals(final String message, final double expected, final Comparable<?> actual) {
        final double value = assertInstanceOf(Double.class, actual, message);
        if (Double.isNaN(expected)) {
            assertEquals(Double.doubleToRawLongBits(expected), Double.doubleToRawLongBits(value), message);
        } else {
            assertEquals(expected, value, EPS, message);
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
            assertTrue(toNaN.contains(sample), "Allocated NaN ordinal");
            /*
             * Verify properties on the category that we created.
             * The sample values are integers in our test.
             */
            assertEquals("Random", String.valueOf(category.name));
            assertEquals("Random", String.valueOf(category.getName()));
            assertBoundEquals("range.minValue", sample, category.range.getMinValue());
            assertBoundEquals("range.maxValue", sample, category.range.getMaxValue());
            assertSame (category.range, category.getSampleRange());
            assertFalse(category.getMeasurementRange().isPresent());
            assertFalse(category.toConverse.isIdentity());
            assertFalse(category.getTransferFunction().isPresent());
            assertFalse(category.isQuantitative());
            /*
             * Verify properties on the converse category. Values shall be NaN
             * and the transfer function shall be absent.
             */
            final Category converse = category.converse;
            assertNotSame(category, converse);
            assertSame   (category, converse.converse);
            assertSame   (converse, category.converted());
            assertSame   (converse, converse.converted());
            assertEquals ("Random", String.valueOf(converse.name));
            assertEquals ("Random", String.valueOf(converse.getName()));
            assertNaN    ("range", converse.range);
            assertNotNull(converse.getSampleRange());
            assertFalse  (category.getMeasurementRange().isPresent());
            assertFalse  (converse.toConverse.isIdentity());
            assertFalse  (converse.getTransferFunction().isPresent());
            assertFalse  (converse.isQuantitative());
            /*
             * Test sample values conversions. They are expected to produce NaN values and
             * the converter shall be able to go back to original values from those NaNs.
             */
            final MathTransform1D inverse = converse.toConverse;
            for (int i=0; i<4; i++) {
                final float x = 100 * random.nextFloat();
                final float y = (float) category.toConverse.transform(x);
                assertTrue(Float.isNaN(y));
                final int ordinal = MathFunctions.toNanOrdinal(y);
                if (collision) {
                    assertNotEquals(sample, ordinal);
                } else {
                    assertEquals(sample, ordinal);
                }
                assertEquals(sample, (float) inverse.transform(y));
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
            assertNotSame    (category, converse);
            assertSame       (category, converse.converse);
            assertSame       (converse, category.converted());
            assertSame       (converse, converse.converted());
            assertEquals     ("Random", String.valueOf(category.name));
            assertEquals     ("Random", String.valueOf(converse.name));
            assertEquals     ("Random", String.valueOf(category.getName()));
            assertEquals     ("Random", String.valueOf(converse.getName()));
            assertEquals     (lower,    category.range.getMinDouble(true));
            assertEquals     (upper,    category.range.getMaxDouble(true));
            assertEquals     (lower*scale+offset, converse.range.getMinDouble(true), EPS);
            assertEquals     (upper*scale+offset, converse.range.getMaxDouble(true), EPS);
            assertBoundEquals("range.minValue", lower, category.range.getMinValue());
            assertBoundEquals("range.maxValue", upper, category.range.getMaxValue());
            assertSame       (category.range,      category.getSampleRange());
            assertSame       (converse.range,      converse.getSampleRange());
            assertSame       (converse.range,      category.getMeasurementRange().get());
            assertSame       (converse.range,      converse.getMeasurementRange().get());
            assertSame       (category.toConverse, category.getTransferFunction().get());
            assertNotSame    (converse.toConverse, converse.getTransferFunction().get());
            assertTrue       (converse.getTransferFunction().get().isIdentity());
            assertFalse      (category.toConverse.isIdentity());
            assertFalse      (converse.toConverse.isIdentity());
            assertTrue       (category.isQuantitative());
            assertTrue       (converse.isQuantitative());
            /*
             * Test sample values conversions.
             */
            final MathTransform1D inverse = category.converse.toConverse;
            assertSame(inverse, category.toConverse.inverse());
            for (int i=0; i<20; i++) {
                final double x = 100 * random.nextDouble();
                final double y = x*scale + offset;
                assertEquals(y, category.toConverse.transform(x), EPS);
                assertEquals(x, inverse.transform(y), EPS);
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

            assertSame  (category, category.converse);
            assertEquals("Random", String.valueOf(category.name));
            assertEquals("Random", String.valueOf(category.getName()));
            assertBoundEquals("range.minValue", lower, category.range.getMinValue());
            assertBoundEquals("range.maxValue", upper, category.range.getMaxValue());
            assertSame(category.range,      category.getSampleRange());
            assertSame(category.range,      category.getMeasurementRange().get());
            assertSame(category.toConverse, category.getTransferFunction().get());
            assertTrue(category.toConverse.isIdentity());
            assertTrue(category.isQuantitative());
        }
    }

    /**
     * Tests a category with a NaN value.
     */
    @Test
    public void testCategoryNaN() {
        final Category category = new Category("NaN", new NumberRange<>(Float.class, Float.NaN, true, Float.NaN, true), null, null, null);
        final NumberRange<?> range = category.getSampleRange();
        assertSame  (category, category.converse);
        assertEquals("NaN",    String.valueOf(category.name));
        assertEquals("NaN",    String.valueOf(category.getName()));
        assertNaN   ("sampleRange", category.range);
        assertEquals(Float.NaN, range.getMinValue());
        assertEquals(Float.NaN, range.getMaxValue());
        assertFalse (category.getMeasurementRange().isPresent());
        assertFalse (category.getTransferFunction().isPresent());
        assertTrue  (category.toConverse.isIdentity());
        assertFalse (category.isQuantitative());
    }

    /**
     * Tests {@link Category#equals(Object, ComparisonMode)} for all comparison modes.
     * Covers qualitative and quantitative categories, name differences, NaN ordinal
     * differences, approximate transform comparison, and category-vs-converse inequality.
     */
    @Test
    public void testLenientEquality() {
        final Category qualA = new Category("Water", NumberRange.create(1, true, 1, true), null, null, new ToNaN());
        final Category qualB = new Category("Water", NumberRange.create(1, true, 1, true), null, null, new ToNaN());

        // Null / incompatible type — all modes must return false.
        assertFalse(qualA.equals(null,             ComparisonMode.STRICT),      "null/STRICT");
        assertFalse(qualA.equals("not a category", ComparisonMode.APPROXIMATE), "String/APPROXIMATE");

        // Self-equality — all modes must return true.
        assertTrue(qualA.equals(qualA, ComparisonMode.STRICT),          "self/STRICT");
        assertTrue(qualA.equals(qualA, ComparisonMode.BY_CONTRACT),     "self/BY_CONTRACT");
        assertTrue(qualA.equals(qualA, ComparisonMode.IGNORE_METADATA), "self/IGNORE_METADATA");
        assertTrue(qualA.equals(qualA, ComparisonMode.APPROXIMATE),     "self/APPROXIMATE");

        // Qualitative: same configuration (fresh ToNaN, same sample → same NaN ordinal).
        assertTrue(qualA.equals(qualB, ComparisonMode.STRICT),          "qual-same/STRICT");
        assertTrue(qualA.equals(qualB, ComparisonMode.BY_CONTRACT),     "qual-same/BY_CONTRACT");
        assertTrue(qualA.equals(qualB, ComparisonMode.IGNORE_METADATA), "qual-same/IGNORE_METADATA");
        assertTrue(qualA.equals(qualB, ComparisonMode.APPROXIMATE),     "qual-same/APPROXIMATE");

        // Qualitative: same sample range but different NaN ordinal.
        // Force ordinal 99 for qualC; standard ToNaN for sample value 1 yields ordinal 1.
        final Category qualC = new Category("Water", NumberRange.create(1, true, 1, true), null, null, (v) -> 99);
        assertFalse(qualA.equals(qualC, ComparisonMode.STRICT),      "qual-diffNaN/STRICT");
        assertTrue (qualA.equals(qualC, ComparisonMode.APPROXIMATE), "qual-diffNaN/APPROXIMATE");

        // Qualitative: same NaN ordinal but different name.
        final Category qualD = new Category("Land", NumberRange.create(1, true, 1, true), null, null, new ToNaN());
        assertFalse(qualA.equals(qualD, ComparisonMode.STRICT),          "qual-diffName/STRICT");
        assertFalse(qualA.equals(qualD, ComparisonMode.BY_CONTRACT),     "qual-diffName/BY_CONTRACT");
        assertTrue (qualA.equals(qualD, ComparisonMode.IGNORE_METADATA), "qual-diffName/IGNORE_METADATA");
        assertTrue (qualA.equals(qualD, ComparisonMode.APPROXIMATE),     "qual-diffName/APPROXIMATE");

        // Category vs its converse (ConvertedCategory): must be false in all modes.
        final Category qualConverse = qualA.converse;
        assertNotSame(qualA, qualConverse);
        assertFalse(qualA.equals(qualConverse, ComparisonMode.STRICT),      "vs-converse/STRICT");
        assertFalse(qualA.equals(qualConverse, ComparisonMode.BY_CONTRACT), "vs-converse/BY_CONTRACT");
        assertFalse(qualA.equals(qualConverse, ComparisonMode.IGNORE_METADATA), "vs-converse/IGNORE_METADATA");
        assertFalse(qualA.equals(qualConverse, ComparisonMode.APPROXIMATE), "vs-converse/APPROXIMATE");

        // ------------------------------------------------------------------
        // Quantitative categories with linear transfer function.
        // ------------------------------------------------------------------
        final MathTransform1D tf1 = (MathTransform1D) MathTransforms.linear(0.1, 5.0);
        final Category quantA = new Category("Temperature", NumberRange.create(10, true, 100, false), tf1, null, null);
        final Category quantB = new Category("Temperature", NumberRange.create(10, true, 100, false), tf1, null, null);

        // Same configuration — all modes must return true.
        assertTrue(quantA.equals(quantB, ComparisonMode.STRICT),          "quant-same/STRICT");
        assertTrue(quantA.equals(quantB, ComparisonMode.BY_CONTRACT),     "quant-same/BY_CONTRACT");
        assertTrue(quantA.equals(quantB, ComparisonMode.IGNORE_METADATA), "quant-same/IGNORE_METADATA");
        assertTrue(quantA.equals(quantB, ComparisonMode.APPROXIMATE),     "quant-same/APPROXIMATE");

        // Different name only.
        final Category quantC = new Category("Renamed", NumberRange.create(10, true, 100, false), tf1, null, null);
        assertFalse(quantA.equals(quantC, ComparisonMode.STRICT),          "quant-diffName/STRICT");
        assertFalse(quantA.equals(quantC, ComparisonMode.BY_CONTRACT),     "quant-diffName/BY_CONTRACT");
        assertTrue (quantA.equals(quantC, ComparisonMode.IGNORE_METADATA), "quant-diffName/IGNORE_METADATA");
        assertTrue (quantA.equals(quantC, ComparisonMode.APPROXIMATE),     "quant-diffName/APPROXIMATE");

        // Significantly different transfer function: scale 0.1 vs 0.2.
        final MathTransform1D tf2 = (MathTransform1D) MathTransforms.linear(0.2, 5.0);
        final Category quantD = new Category("Temperature", NumberRange.create(10, true, 100, false), tf2, null, null);
        assertFalse(quantA.equals(quantD, ComparisonMode.STRICT),      "quant-bigDiff/STRICT");
        assertFalse(quantA.equals(quantD, ComparisonMode.APPROXIMATE), "quant-bigDiff/APPROXIMATE");

        // Tiny transfer function difference: offset 5.0 vs 5.0 - 1e-13.
        // Relative threshold for offset ≈ 5.0 is 5.0 * 1E-13 = 5E-13 > 1E-13, so within tolerance.
        final MathTransform1D tf3 = (MathTransform1D) MathTransforms.linear(0.1, 5.0 - 1e-13);
        final Category quantE = new Category("Temperature", NumberRange.create(10, true, 100, false), tf3, null, null);
        assertFalse(quantA.equals(quantE, ComparisonMode.STRICT),      "quant-tinyDiff/STRICT");
        assertTrue (quantA.equals(quantE, ComparisonMode.APPROXIMATE), "quant-tinyDiff/APPROXIMATE");

        // ------------------------------------------------------------------
        // Category with MeasurementRange (identity transform, unit-bearing range).
        // ------------------------------------------------------------------
        final MathTransform1D identity = (MathTransform1D) MathTransforms.identity(1);
        final Category measA = new Category("Celsius",
                MeasurementRange.create(10f, true, 30f, true, Units.CELSIUS),
                identity, Units.CELSIUS, null);
        final Category measB = new Category("Celsius",
                MeasurementRange.create(10f, true, 30f, true, Units.CELSIUS),
                identity, Units.CELSIUS, null);

        // Same unit — equal in STRICT.
        assertTrue(measA.equals(measB, ComparisonMode.STRICT), "meas-sameUnit/STRICT");

        // Different units: CELSIUS vs KELVIN — ranges differ, must be false.
        final Category measK = new Category("Kelvin",
                MeasurementRange.create(10f, true, 30f, true, Units.KELVIN),
                identity, Units.KELVIN, null);
        assertFalse(measA.equals(measK, ComparisonMode.STRICT),      "meas-diffUnit/STRICT");
        assertFalse(measA.equals(measK, ComparisonMode.APPROXIMATE), "meas-diffUnit/APPROXIMATE");
    }
}
