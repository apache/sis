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
package org.apache.sis.test;

import java.awt.image.Raster;

import static org.junit.Assert.*;


/**
 * Assertion methods used by the {@code sis-feature} module in addition of the ones inherited
 * from other modules and libraries.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public strictfp class FeatureAssert extends ReferencingAssert {
    /**
     * For subclass constructor only.
     */
    protected FeatureAssert() {
    }

    /**
     * Verifies that sample values in the given raster are equal to the expected integer values.
     *
     * @param  raster    the raster to verify.
     * @param  band      the band to verify.
     * @param  expected  the expected sample values.
     */
    public static void assertValuesEqual(final Raster raster, final int band, final int[][] expected) {
        final int minX = raster.getMinX();
        final int minY = raster.getMinY();
        assertEquals("Height", expected.length, raster.getHeight());
        for (int j=0; j<expected.length; j++) {
            final int[] row = expected[j];
            assertEquals("Width", row.length, raster.getWidth());
            final int y = minY + j;
            for (int i=0; i<row.length; i++) {
                final int x = minX + i;
                final int a = raster.getSample(x, y, band);
                final int e = row[i];
                if (a != e) {
                    fail(mismatchedSampleValue(x, y, i, j, band, e, a));
                }
            }
        }
    }

    /**
     * Verifies that sample values in the given raster are equal to the expected floating-point values.
     *
     * @param  raster    the raster to verify.
     * @param  band      the band to verify.
     * @param  expected  the expected sample values.
     */
    public static void assertValuesEqual(final Raster raster, final int band, final float[][] expected) {
        final int minX = raster.getMinX();
        final int minY = raster.getMinY();
        assertEquals("Height", expected.length, raster.getHeight());
        for (int j=0; j<expected.length; j++) {
            final float[] row = expected[j];
            assertEquals("Width", row.length, raster.getWidth());
            final int y = minY + j;
            for (int i=0; i<row.length; i++) {
                final int   x = minX + i;
                final float a = raster.getSampleFloat(x, y, band);
                final float e = row[i];
                if (Float.floatToRawIntBits(a) != Float.floatToRawIntBits(e)) {
                    fail(mismatchedSampleValue(x, y, i, j, band, e, a));
                }
            }
        }
    }

    /**
     * Returns the error message for a test failure in an {@code assertValuesEqual(…)} method.
     */
    private static String mismatchedSampleValue(int x, int y, int i, int j, int band, Number expected, Number actual) {
        return "Mismatched sample value at image coordinates (" + x + ", " + y + ") "
                + "— matrix indices (" + i + ", " + j + ") band " + band
                + ": expected " + expected + " but found " + actual;
    }
}
