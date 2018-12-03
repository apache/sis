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
import java.util.Random;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link CategoryList}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final strictfp class CategoryListTest extends TestCase {
    /**
     * Small value for comparisons.
     */
    private static final double EPS = 1E-9;

    /**
     * Asserts that the specified categories are sorted.
     * This method ignores {@code NaN} values.
     */
    private static void assertSorted(final Category[] categories) {
        for (int i=1; i<categories.length; i++) {
            final Category current  = categories[i  ];
            final Category previous = categories[i-1];
            assertFalse( current.minimum >  current.maximum);
            assertFalse(previous.minimum > previous.maximum);
            assertFalse(Category.compare(previous.maximum, current.minimum) > 0);
        }
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
                assertEquals("binarySearch", Arrays.binarySearch(array, searchFor),
                                       CategoryList.binarySearch(array, searchFor));
            }
            /*
             * Previous test didn't tested NaN values (which is the main difference
             * between binarySearch method in Arrays and CategoryList). Now test it.
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
                int foundAt = CategoryList.binarySearch(array, search);
                if (foundAt >= 0) {
                    assertEquals(Double.doubleToRawLongBits(search),
                                 Double.doubleToRawLongBits(array[foundAt]), STRICT);
                } else {
                    foundAt = ~foundAt;
                    if (foundAt < array.length) {
                        final double after = array[foundAt];
                        assertFalse(search >= after);
                        if (Double.isNaN(search)) {
                            assertTrue("isNaN", Double.isNaN(after));
                        }
                    }
                    if (foundAt > 0) {
                        final double before = array[foundAt - 1];
                        assertFalse(search <= before);
                        if (!Double.isNaN(search)) {
                            assertFalse("isNaN", Double.isNaN(before));
                        }
                    }
                }
            }
        }
    }
}
