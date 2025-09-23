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
package org.apache.sis.image;

import java.util.Arrays;
import java.util.Random;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.Shapes2D;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.internal.shared.AffineTransform2D;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.math.Statistics;
import org.apache.sis.math.StatisticsFormat;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.referencing.operation.HardCodedConversions;


/**
 * Tests {@link ResamplingGrid}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ResamplingGridTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public ResamplingGridTest() {
    }

    /**
     * Tests {@link ResamplingGrid#create(MathTransform2D, Rectangle)} with an affine transform.
     * The method should detect the affine case and return an equal transform (not necessarily the same instance).
     *
     * @throws TransformException if an error occurred while transforming a coordinate.
     */
    @Test
    public void testCreateAffine() throws TransformException {
        final Rectangle       bounds    = new Rectangle(100, 200, 300, 400);
        final MathTransform2D reference = new AffineTransform2D(2, 0, 0, 3, -10, -20);
        final MathTransform2D grid      = ResamplingGrid.create(reference, bounds);
        assertEquals(reference, grid);
    }

    /**
     * Tests the {@link ResamplingGrid} class using an affine transform.
     * Because the transform is linear, results should be identical ignoring rounding errors.
     *
     * @throws TransformException if an error occurred while transforming a coordinate.
     */
    @Test
    public void compareWithAffine() throws TransformException {
        final AffineTransform2D reference = new AffineTransform2D(0.25, 0, 0, 2.5, 4, 2);
        final Rectangle         bounds    = new Rectangle(-7, 3, 12, 8);
        final ResamplingGrid    grid      = new ResamplingGrid(reference, bounds, new Dimension(4,3));
        final Random            random    = TestUtilities.createRandomNumberGenerator();
        final double[]          source    = new double[2];
        final double[]          actual    = new double[2];
        final double[]          expected  = new double[2];
        for (int i=0; i<100; i++) {
            source[0] = bounds.x + bounds.width  * random.nextDouble();
            source[1] = bounds.y + bounds.height * random.nextDouble();
            grid.transform(source, 0, actual, 0, 1);
            reference.transform(source, 0, expected, 0, 1);
            assertArrayEquals(expected, actual, Numerics.COMPARISON_THRESHOLD);
        }
    }

    /**
     * Creates a transform for coordinates from the given source region to the given target region.
     */
    private static AffineTransform2D affine(final Rectangle2D source, final Rectangle2D target) {
        final double scaleX = target.getWidth()  / source.getWidth();
        final double scaleY = target.getHeight() / source.getHeight();
        return new AffineTransform2D(scaleX, 0, 0, scaleY,
                target.getMinX() - source.getMinX() * scaleX,
                target.getMinY() - source.getMinY() * scaleY);
    }

    /**
     * Compares the result of a {@link ResamplingGrid} with the result of a reference transform.
     *
     * @param  projection  a non-linear map projection to use for creating the resampling grid.
     * @param  domain      the domain in source coordinates of the given projection.
     * @return the {@link ResamplingGrid} created by this method.
     * @throws TransformException if an error occurred while transforming a coordinate.
     */
    private static MathTransform2D compare(final String title, final MathTransform projection, final Rectangle2D domain)
            throws TransformException
    {
        final Rectangle bounds = new Rectangle(10, 20, 400, 150);
        final MathTransform2D reference = (MathTransform2D) MathTransforms.concatenate(
                affine(bounds, domain), projection,
                affine(Shapes2D.transform((MathTransform2D) projection, domain, null), bounds));

        final MathTransform2D grid     = ResamplingGrid.create(reference, bounds);
        final Statistics      sx       = new Statistics("sx");
        final Statistics      sy       = new Statistics("sy");
        final double[]        source   = new double[2];
        final double[]        actual   = new double[2];
        final double[]        expected = new double[2];
        final int             xmin     = bounds.x;
        final int             ymin     = bounds.y;
        final int             xmax     = bounds.width  + xmin;
        final int             ymax     = bounds.height + ymin;
        for (int y=ymin; y<ymax; y++) {
            for (int x=xmin; x<xmax; x++) {
                source[0] = x;
                source[1] = y;
                grid.transform(source, 0, actual, 0, 1);
                reference.transform(source, 0, expected, 0, 1);
                final double dx = StrictMath.abs(expected[0] - actual[0]);
                final double dy = StrictMath.abs(expected[1] - actual[1]);
                if (!(dx <= ResamplingGrid.TOLERANCE && dy <= ResamplingGrid.TOLERANCE)) {
                    fail("Error at (" + x + ',' + y + "): expected " +
                            Arrays.toString(expected) + " but got " +
                            Arrays.toString(actual) + ". Error is (" + dx + ", " + dy + ")." +
                            " Transform is:\n" + grid);
                }
                sx.accept(dx);
                sy.accept(dy);
            }
        }
        if (VERBOSE) {
            // Print a summary of errors.
            final StatisticsFormat f = StatisticsFormat.getInstance();
            f.setBorderWidth(1);
            out.println();
            out.println(title);
            out.println(f.format(new Statistics[] {sx, sy}));
        }
        return grid;
    }

    /**
     * Tests {@link ResamplingGrid} with the Mercator projection on a region crossing the equator.
     *
     * @throws TransformException if an error occurred while transforming a coordinate.
     */
    @Test
    public void testMercator() throws TransformException {
        final MathTransform projection = HardCodedConversions.mercator().getConversionFromBase().getMathTransform();
        final Rectangle domain = new Rectangle(-20, -40, 40, 80);
        final MathTransform2D tr = compare("Mercator", projection, domain);
        assertInstanceOf(ResamplingGrid.class, tr);
        final ResamplingGrid grid = (ResamplingGrid) tr;
        assertEquals( 1, grid.numXTiles, "The x dimension should be affine.");
        assertEquals(16, grid.numYTiles, "The y dimension cannot be affine.");     // Empirical value.
    }

    /**
     * Tests {@link ResamplingGrid} with the Mercator projection on a region
     * small enough for being wholly approximated by a single affine transform.
     *
     * @throws TransformException if an error occurred while transforming a coordinate.
     */
    @Test
    public void testMercatorOnSmallArea() throws TransformException {
        final MathTransform projection = HardCodedConversions.mercator().getConversionFromBase().getMathTransform();
        final Rectangle2D domain = new Rectangle2D.Double(-20, 20, 0.25, 0.25);
        final MathTransform2D tr = compare("Mercator (small area)", projection, domain);
        assertInstanceOf(AffineTransform2D.class, tr);
    }

    /**
     * Creates a <cite>Lambert Conic Conformal (1SP)</cite> projection
     * with a Latitude of natural origin arbitrarily set to 40.
     */
    private static MathTransform lambertProjection() {
        return HardCodedConversions.createCRS(HardCodedConversions.LAMBERT).getConversionFromBase().getMathTransform();
    }

    /**
     * Tests {@link ResamplingGrid} with a Lambert Conic Conformal projection.
     *
     * @throws TransformException if an error occurred while transforming a coordinate.
     */
    @Test
    public void testLambert() throws TransformException {
        final MathTransform projection = lambertProjection();
        final Rectangle domain = new Rectangle(-20, 30, 40, 20);
        final MathTransform2D tr = compare("Lambert", projection, domain);
        assertInstanceOf(ResamplingGrid.class, tr);
        final ResamplingGrid grid = (ResamplingGrid) tr;
        assertEquals(32, grid.numXTiles, "The x dimension cannot be affine.");     // Empirical value.
        assertEquals(16, grid.numYTiles, "The y dimension cannot be affine.");     // Empirical value.
    }

    /**
     * Tests {@link ResamplingGrid} with the Lambert Conic Conformal projection on a
     * region small enough for being wholly approximated by a single affine transform.
     *
     * @throws TransformException if an error occurred while transforming a coordinate.
     */
    @Test
    public void testLambertOnSmallArea() throws TransformException {
        final MathTransform projection = lambertProjection();
        final Rectangle2D domain = new Rectangle2D.Double(-20, 50, 0.025, 0.025);
        final MathTransform2D tr = compare("Lambert (small area)", projection, domain);
        assertInstanceOf(AffineTransform2D.class, tr);
    }
}
