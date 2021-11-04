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

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link CategoryList}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.1
 * @since   1.0
 * @module
 */
@DependsOn(CategoryTest.class)
public final strictfp class CategoryListTest extends TestCase {
    /**
     * Asserts that the specified categories are sorted.
     * This method ignores {@code NaN} values.
     */
    private static void assertSorted(final List<Category> categories) {
        final int size = categories.size();
        for (int i=1; i<size; i++) {
            final Category current  = categories.get(i  );
            final Category previous = categories.get(i-1);
            assertFalse( current.range.getMinDouble(true) >  current.range.getMaxDouble(true));
            assertFalse(previous.range.getMinDouble(true) > previous.range.getMaxDouble(true));
            assertFalse(Category.compare(previous.range.getMaxDouble(true),
                                          current.range.getMinDouble(true)) > 0);
        }
    }

    /**
     * Asserts that the given categories defines range of sample values ({@literal i.e.} are not converse).
     */
    private static void assertNotConverted(final CategoryList categories) {
        for (final Category c : categories) {
            assertFalse("isNaN", Double.isNaN(c.range.getMinDouble()));
            assertFalse(c.range instanceof ConvertedRange);
        }
    }

    /**
     * Tests the checks performed by {@link CategoryList} constructor.
     */
    @Test
    public void testArgumentChecks() {
        final ToNaN toNaN = new ToNaN();
        Category[] categories = {
            new Category("No data", NumberRange.create( 0, true,  0, true), null, null, toNaN),
            new Category("Land",    NumberRange.create(10, true, 10, true), null, null, toNaN),
            new Category("Clouds",  NumberRange.create( 2, true,  2, true), null, null, toNaN),
            new Category("Again",   NumberRange.create(10, true, 10, true), null, null, toNaN)       // Range overlaps.
        };
        try {
            assertNotConverted(CategoryList.create(categories.clone(), null));
            fail("Should not have accepted range overlap.");
        } catch (IllegalArgumentException exception) {
            // This is the expected exception.
            final String message = exception.getMessage();
            assertTrue(message, message.contains("Land"));
            assertTrue(message, message.contains("Again"));
        }
        // Removes the wrong category. Now, construction should succeed.
        categories = Arrays.copyOf(categories, categories.length - 1);
        assertNotConverted(CategoryList.create(categories, null));
        assertSorted(Arrays.asList(categories));
    }

    /**
     * Tests the {@link CategoryList#binarySearch(double[], double)} method.
     */
    @Test
    public void testBinarySearch() {
        final Random random = TestUtilities.createRandomNumberGenerator();
        for (int pass=0; pass<50; pass++) {
            final double[] array = new double[random.nextInt(32) + 32];
            int realNumberLimit = 0;
            for (int i=0; i<array.length; i++) {
                realNumberLimit += random.nextInt(10) + 1;
                array[i] = realNumberLimit;
            }
            realNumberLimit += random.nextInt(10);
            for (int i=0; i<100; i++) {
                final double searchFor = random.nextInt(realNumberLimit);
                int expected = Arrays.binarySearch(array, searchFor);
                if (expected < 0) expected = ~expected - 1;
                assertEquals("binarySearch", expected, CategoryList.binarySearch(array, searchFor));
            }
            /*
             * Previous test didn't tested NaN values, which is the main difference between Arrays.binarySearch(…) and
             * CategoryList.binarySearch(…). Now test those NaNs. We fill the last half of the array with NaN values;
             * the first half keep original real values. Then we search sometime real values, sometime NaN values.
             */
            int nanOrdinalLimit = 0;
            realNumberLimit /= 2;
            for (int i = array.length / 2; i < array.length; i++) {
                nanOrdinalLimit += random.nextInt(10) + 1;
                array[i] = MathFunctions.toNanFloat(nanOrdinalLimit);
            }
            nanOrdinalLimit += random.nextInt(10);
            for (int i=0; i<100; i++) {
                final double search;
                if (random.nextBoolean()) {
                    search = random.nextInt(realNumberLimit);
                } else {
                    search = MathFunctions.toNanFloat(random.nextInt(nanOrdinalLimit));
                }
                /*
                 * At this point, 'search' is a real value or a NaN value to search.
                 */
                final int foundAt = CategoryList.binarySearch(array, search);
                if (foundAt < 0) {
                    // Expected only if the value to search is NaN or less than all values in the array.
                    assertFalse(search >= array[0]);
                } else if (foundAt >= array.length) {
                    // Expected only if the value to search is NaN or greater than all values in the array.
                    assertFalse(search <= array[array.length - 1]);
                } else if (Double.doubleToRawLongBits(array[foundAt]) != Double.doubleToRawLongBits(search)) {
                    final double before = array[foundAt];
                    assertFalse(search <= before);
                    if (!Double.isNaN(search)) {
                        assertFalse("isNaN", Double.isNaN(before));
                    }
                    if (foundAt + 1 < array.length) {
                        final double after = array[foundAt + 1];
                        assertFalse(search >= after);
                        if (Double.isNaN(search)) {
                            assertTrue("isNaN", Double.isNaN(after));
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates an array of category for {@link #testSearch()} and {@link #testTransform()}.
     */
    private static Category[] categories() {
        final ToNaN toNaN = new ToNaN();
        return new Category[] {
            /*[0]*/ new Category("No data",     NumberRange.create(  0, true,   0, true), null, null, toNaN),
            /*[1]*/ new Category("Land",        NumberRange.create(  7, true,   7, true), null, null, toNaN),
            /*[2]*/ new Category("Clouds",      NumberRange.create(  3, true,   3, true), null, null, toNaN),
            /*[3]*/ new Category("Temperature", NumberRange.create( 10, true, 100, false), (MathTransform1D) MathTransforms.linear(0.1, 5), null, toNaN),
            /*[4]*/ new Category("Foo",         NumberRange.create(100, true, 120, false), (MathTransform1D) MathTransforms.linear( -1, 3), null, toNaN)
        };
    }

    /**
     * Tests the sample values range and converted values range after construction of a list of categories.
     */
    @Test
    public void testRanges() {
        final CategoryList list = CategoryList.create(categories(), null);
        assertSorted(list);
        assertTrue  ("isMinIncluded",           list.range.isMinIncluded());
        assertFalse ("isMaxIncluded",           list.range.isMaxIncluded());
        assertFalse ("converse.isMinIncluded",  list.converse.range.isMinIncluded());     // Because computed from maxValue before conversion.
        assertFalse ("converse.isMaxIncluded",  list.converse.range.isMaxIncluded());
        assertEquals("minValue",             0, ((Number) list.range          .getMinValue()).doubleValue(), STRICT);
        assertEquals("maxValue",           120, ((Number) list.range          .getMaxValue()).doubleValue(), STRICT);
        assertEquals("converse.minValue", -117, ((Number) list.converse.range.getMinValue()).doubleValue(), STRICT);
        assertEquals("converse.maxValue",   15, ((Number) list.converse.range.getMaxValue()).doubleValue(), STRICT);
        assertEquals("converse.minValue", -117, list.converse.range.getMinDouble(false), STRICT);
        assertEquals("converse.maxValue",   15, list.converse.range.getMaxDouble(false), STRICT);
        assertEquals("converse.minValue", -116, list.converse.range.getMinDouble(true),  CategoryTest.EPS);
        assertEquals("converse.maxValue", 14.9, list.converse.range.getMaxDouble(true),  CategoryTest.EPS);
    }

    /**
     * Tests the {@link CategoryList#search(double)} method.
     */
    @Test
    @DependsOnMethod("testBinarySearch")
    public void testSearch() {
        final Category[] categories = categories();
        final CategoryList list = CategoryList.create(categories.clone(), null);
        assertTrue("containsAll", list.containsAll(Arrays.asList(categories)));
        /*
         * Checks category searches for values that are insides the range of a category.
         */
        assertSame(  "0", categories[0],          list.search(  0));
        assertSame(  "7", categories[1],          list.search(  7));
        assertSame(  "3", categories[2],          list.search(  3));
        assertSame(" 10", categories[3],          list.search( 10));
        assertSame(" 50", categories[3],          list.search( 50));
        assertSame("100", categories[4],          list.search(100));
        assertSame("110", categories[4],          list.search(110));
        assertSame(  "0", categories[0].converse, list.converse.search(MathFunctions.toNanFloat(  0)));
        assertSame(  "7", categories[1].converse, list.converse.search(MathFunctions.toNanFloat(  7)));
        assertSame(  "3", categories[2].converse, list.converse.search(MathFunctions.toNanFloat(  3)));
        assertSame(" 10", categories[3].converse, list.converse.search(  /* transform( 10) */     6 ));
        assertSame(" 50", categories[3].converse, list.converse.search(  /* transform( 50) */    10 ));
        assertSame("100", categories[4].converse, list.converse.search(  /* transform(100) */   -97 ));
        assertSame("110", categories[4].converse, list.converse.search(  /* transform(110) */  -107 ));
        /*
         * Checks values outside the range of any category.  The category below requested value has its
         * domain expanded up to the next category, except if one category is qualitative and the other
         * one is quantitative, in which case the quantitative category has precedence.
         */
        assertSame( "-1", categories[0],          list.search( -1));
        assertSame(  "2", categories[0],          list.search(  2));
        assertSame(  "4", categories[2],          list.search(  4));
        assertSame(  "9", categories[3],          list.search(  9));
        assertSame("120", categories[4],          list.search(120));
        assertSame("200", categories[4],          list.search(200));
        assertNull( "-1",                         list.converse.search(MathFunctions.toNanFloat(-1)));    // Nearest sample is 0
        assertNull(  "2",                         list.converse.search(MathFunctions.toNanFloat( 2)));    // Nearest sample is 3
        assertNull(  "4",                         list.converse.search(MathFunctions.toNanFloat( 4)));    // Nearest sample is 3
        assertNull(  "9",                         list.converse.search(MathFunctions.toNanFloat( 9)));    // Nearest sample is 10
        assertSame(  "9", categories[4].converse, list.converse.search( /* transform(  9) */   5.9 ));    // Nearest sample is 10
        assertSame("120", categories[4].converse, list.converse.search( /* transform(120) */  -117 ));    // Nearest sample is 119
        assertSame("200", categories[4].converse, list.converse.search( /* transform(200) */  -197 ));    // Nearest sample is 119
    }

    /**
     * Tests the {@link CategoryList#transform(double)} method.
     *
     * @throws TransformException if an error occurred while transforming a value.
     */
    @Test
    @DependsOnMethod("testSearch")
    public void testTransform() throws TransformException {
        final Random random = TestUtilities.createRandomNumberGenerator();
        final CategoryList list = CategoryList.create(categories(), null);
        /*
         * Checks conversions. We verified in 'testSearch()' that correct categories are found for those values.
         */
        assertTrue  (  "0", Double.isNaN(list.transform(  0)));
        assertTrue  (  "7", Double.isNaN(list.transform(  7)));
        assertTrue  (  "3", Double.isNaN(list.transform(  3)));
        assertEquals( "10",           6, list.transform( 10), CategoryTest.EPS);
        assertEquals( "50",          10, list.transform( 50), CategoryTest.EPS);
        assertEquals("100",         -97, list.transform(100), CategoryTest.EPS);
        assertEquals("110",        -107, list.transform(110), CategoryTest.EPS);
        /*
         * Tests conversions using methods working on arrays.
         * We assume that the 'transform(double)' version can be used as a reference.
         */
        final double[] input   = new double[337];                   // A prime number, for more randomness.
        final double[] output0 = new double[input.length];
        final double[] output1 = new double[input.length];
        for (int i=0; i < input.length;) {
            final Category c = list.get(random.nextInt(list.size()));
            final int lower  =  (int) c.range.getMinDouble(true);
            final int span   = ((int) c.range.getMaxDouble(false)) - lower;
            int count = StrictMath.min(random.nextInt(span + 5) + 1, input.length - i);
            while (--count >= 0) {
                input  [i] = random.nextInt(span) + lower;
                output0[i] = list.transform(input[i]);
                i++;
            }
        }
        list.transform(input, 0, output1, 0, input.length);
        compare(output0, output1);
        /*
         * Tests the transform using overlapping array.
         */
        final int n = 3;
        final int r = input.length - n;
        System.arraycopy(input,   0, output1, n, r);
        list.transform  (output1, n, output1, 0, r);
        System.arraycopy(output0, r, output1, r, n);
        compare(output0, output1);
        /*
         * Implementation will do the following transform in reverse direction.
         */
        System.arraycopy(input,   n, output1, 0, r);
        list.transform  (output1, 0, output1, n, r);
        System.arraycopy(output0, 0, output1, 0, n);
        compare(output0, output1);
        /*
         * Test inverse transfom.
         */
        list.inverse().transform(output0, 0, output0, 0, output0.length);
        for (int i=0; i<output0.length; i++) {
            final double expected = input[i];
            if (expected >= 10 && expected < 120) {
                // Values outside this range have been clamped.
                // They would usually not be equal.
                assertEquals("inverse", expected, output0[i], CategoryTest.EPS);
            }
        }
    }

    /**
     * Creates a category list for testing inverse transform with the given background value.
     *
     * @param  background  a value not used by {@link #categories()}, or {@code null}.
     * @return the list of categories for testing "units to sample" conversions.
     * @throws TransformException if an error occurred while transforming a value.
     */
    private static CategoryList createInverseTransform(final Number background) throws TransformException {
        final CategoryList list = CategoryList.create(categories(), background).converse;
        assertEquals( 10, list.transform(   6), CategoryTest.EPS);
        assertEquals( 50, list.transform(  10), CategoryTest.EPS);
        assertEquals(100, list.transform( -97), CategoryTest.EPS);
        assertEquals(110, list.transform(-107), CategoryTest.EPS);
        assertEquals(  0, list.transform(Double.NaN),                  CategoryTest.EPS);
        assertEquals(  7, list.transform(MathFunctions.toNanFloat(7)), CategoryTest.EPS);
        assertEquals(  3, list.transform(MathFunctions.toNanFloat(3)), CategoryTest.EPS);
        return list;
    }

    /**
     * Tests the {@link CategoryList#transform(double)} method from units to sample values.
     * This test includes {@link Double#NaN} values that are not among declared values.
     *
     * @throws TransformException if an error occurred while transforming a value.
     */
    @Test
    @DependsOnMethod("testTransform")
    public void testInverseTransform() throws TransformException {
        final int background = 2;   // Value not used by `categories()`.
        final CategoryList list = createInverseTransform(background);
        /*
         * Below is a NaN value which is not in the list of qualitative categories.
         * Trying to convert this value would result in an exception, but in this
         * test we specified a background value that `CategoryList` can use as fallback.
         */
        assertEquals(background, list.transform(MathFunctions.toNanFloat(background)), CategoryTest.EPS);
        assertEquals(background, list.transform(MathFunctions.toNanFloat(4)),          CategoryTest.EPS);
        /*
         * Same values in arrays.
         */
        final int dummyCount = 3;
        final double[] values = {
            -20, -10,  -1,                          // 3 dummy values for introducing an offset in the array.
              6,  10, -97,                          // First values to be transformed (from above test).
            MathFunctions.toNanFloat(background),
            MathFunctions.toNanFloat(4), -107, Double.NaN,
            MathFunctions.toNanFloat(7),
            MathFunctions.toNanFloat(3)
        };
        final double[] result = new double[values.length - dummyCount];
        list.transform(values, dummyCount, result, 0, result.length);
        assertArrayEquals(new double[] {
            10, 50, 100, background, background, 110, 0, 7, 3
        }, result, CategoryTest.EPS);
    }

    /**
     * Same tests than {@link #testInverseTransform()} but without background value
     * that the transform could use as a fallback.
     *
     * @throws TransformException if an error occurred while transforming a value.
     */
    @Test
    @DependsOnMethod("testInverseTransform")
    public void testInverseTransformFailure() throws TransformException {
        final CategoryList list = createInverseTransform(null);
        try {
            list.transform(MathFunctions.toNanFloat(4));
            fail("Expected TransformException");
        } catch (TransformException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("NaN #4"));
        }
    }

    /**
     * Compares two arrays. Special comparison is performed for NaN values.
     */
    private static void compare(final double[] output0, final double[] output1) {
        assertEquals("length", output0.length, output1.length);
        for (int i=0; i<output0.length; i++) {
            final double expected = output0[i];
            final double actual   = output1[i];
            if (Double.isNaN(expected)) {
                final int bits1 = Float.floatToRawIntBits((float) expected);
                final int bits2 = Float.floatToRawIntBits((float)   actual);
                assertEquals(bits1, bits2);
            }
            assertEquals(expected, actual, CategoryTest.EPS);
        }
    }

    /**
     * Tests construction from categories that already describe real values.
     * The constructor should have replaced {@link ConvertedCategory} instances
     * by plain {@link Category} instances without relationship with the sample values.
     */
    @Test
    public void testFromConvertedCategories() {
        final Category[] categories = categories();
        for (int i=0; i<categories.length; i++) {
            categories[i] = categories[i].converse;
        }
        final CategoryList list = CategoryList.create(categories, null);
        assertSorted(list);
        for (int i=list.size(); --i >= 0;) {
            final Category category = list.get(i);
            assertSame(category, category.converse);
            assertTrue(category.toConverse.isIdentity());
        }
    }
}
