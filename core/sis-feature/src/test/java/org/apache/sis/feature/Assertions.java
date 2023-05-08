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
package org.apache.sis.feature;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.geom.PathIterator;
import org.opengis.coverage.grid.SequenceType;
import org.apache.sis.image.PixelIterator;
import org.apache.sis.util.Static;

import static org.junit.Assert.*;


/**
 * Assertion methods used by the {@code sis-feature} module in addition of the ones inherited
 * from other modules and libraries.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 */
public final class Assertions extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private Assertions() {
    }

    /**
     * Verifies that sample values in the given image are equal to the expected values.
     * Sample values are compared using floating point {@code double} precision.
     * NaN values must be strictly equals (same bit pattern).
     *
     * @param  expected       the expected sample values.
     * @param  boundsExpected bounds of the expected region, or {@code null} for the whole image.
     * @param  actual         the image to verify.
     * @param  boundsActual   bounds of the actual region, or {@code null} for the whole image.
     */
    public static void assertPixelsEqual(final RenderedImage expected, final Rectangle boundsExpected,
                                         final RenderedImage actual,   final Rectangle boundsActual)
    {
        if (boundsExpected != null && boundsActual != null) {
            assertEquals("width",  boundsExpected.width,  boundsActual.width);
            assertEquals("height", boundsExpected.height, boundsActual.height);
        }
        final PixelIterator ie = new PixelIterator.Builder().setIteratorOrder(SequenceType.LINEAR).setRegionOfInterest(boundsExpected).create(expected);
        final PixelIterator ia = new PixelIterator.Builder().setIteratorOrder(SequenceType.LINEAR).setRegionOfInterest(boundsActual).create(actual);
        double[] ev = null;
        double[] av = null;
        while (ie.next()) {
            assertTrue(ia.next());
            ev = ie.getPixel(ev);
            av = ia.getPixel(av);
            assertEquals(ev.length, av.length);
            for (int band=0; band<ev.length; band++) {
                final double e = ev[band];
                final double a = av[band];
                if (Double.doubleToRawLongBits(a) != Double.doubleToRawLongBits(e)) {
                    final Point p = ia.getPosition();
                    fail(mismatchedSampleValue(p.x, p.y, p.x - actual.getMinX(), p.y - actual.getMinY(), band, e, a));
                }
            }
        }
        assertFalse(ia.next());
    }

    /**
     * Verifies that sample values in the given image are equal to the expected floating point values.
     * NaN values are compared using {@link Double#doubleToRawLongBits(double)} (i.e. different NaNs
     * are <em>not</em> collapsed in a canonical NaN value).
     *
     * @param  image     the image to verify.
     * @param  band      the band to verify.
     * @param  expected  the expected sample values.
     */
    public static void assertValuesEqual(final RenderedImage image, final int band, final double[][] expected) {
        assertEquals("Height", expected.length, image.getHeight());
        final PixelIterator it = new PixelIterator.Builder().setIteratorOrder(SequenceType.LINEAR).create(image);
        for (int j=0; j<expected.length; j++) {
            final double[] row = expected[j];
            assertEquals("Width", row.length, image.getWidth());
            for (int i=0; i<row.length; i++) {
                assertTrue(it.next());
                final double a = it.getSampleDouble(band);
                final double e = row[i];
                if (Double.doubleToRawLongBits(a) != Double.doubleToRawLongBits(e)) {
                    final Point p = it.getPosition();
                    fail(mismatchedSampleValue(p.x, p.y, i, j, band, e, a));
                }
            }
        }
        assertFalse(it.next());
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

    /**
     * Asserts that the path is equal to given reference.
     *
     * @param  expected   expected geometry outline.
     * @param  actual     actual geometry outline.
     * @param  tolerance  tolerance threshold for floating point value comparisons.
     */
    @SuppressWarnings("fallthrough")
    public static void assertPathEquals(final PathIterator expected, final PathIterator actual, final double tolerance) {
        assertEquals("getWindingRule", expected.getWindingRule(), actual.getWindingRule());
        final double[] buffer = new double[6];
        final double[] values = new double[6];
        while (!expected.isDone()) {
            assertFalse("isDone", actual.isDone());
            final int type = expected.currentSegment(buffer);
            assertEquals("currentSegment", type, actual.currentSegment(values));
            switch (type) {
                case PathIterator.SEG_CUBICTO: assertEquals("x₃", buffer[4], values[4], tolerance);
                                               assertEquals("y₃", buffer[5], values[5], tolerance);
                case PathIterator.SEG_QUADTO:  assertEquals("x₂", buffer[2], values[2], tolerance);
                                               assertEquals("y₂", buffer[3], values[3], tolerance);
                case PathIterator.SEG_LINETO:  assertEquals("x₁", buffer[0], values[0], tolerance);
                                               assertEquals("y₁", buffer[1], values[1], tolerance); break;
                case PathIterator.SEG_MOVETO:  assertEquals("x₀", buffer[0], values[0], tolerance);
                                               assertEquals("y₀", buffer[1], values[1], tolerance);
                case PathIterator.SEG_CLOSE:   break;
                default: fail("Unexpected type: " + type);
            }
            expected.next();
            actual.next();
        }
        assertTrue("isDone", actual.isDone());
    }
}
