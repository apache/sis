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
package org.apache.sis.coverage.grid;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.internal.shared.AffineTransform2D;
import org.apache.sis.referencing.operation.transform.MathTransforms;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertMessageContains;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.crs.HardCodedCRS;


/**
 * Tests the {@link GridCoverageBuilder} helper class.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class GridCoverageBuilderTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public GridCoverageBuilderTest() {
    }

    /**
     * Tests {@link GridCoverageBuilder#setValues(RenderedImage)}.
     */
    @Test
    public void testBuildFromImage() {
        final RenderedImage image = new BufferedImage(5, 8, BufferedImage.TYPE_INT_ARGB);
        final GridCoverageBuilder builder = new GridCoverageBuilder();
        assertSame(builder, builder.setValues(image));
        final GridCoverage coverage = testBuilder(builder, 4);
        assertSame(image, coverage.render(null));
    }

    /**
     * Tests {@link GridCoverageBuilder#setValues(Raster)}.
     */
    @Test
    public void testBuildFromRaster() {
        final WritableRaster raster = new BufferedImage(5, 8, BufferedImage.TYPE_3BYTE_BGR).getRaster();
        final GridCoverageBuilder builder = new GridCoverageBuilder();
        assertSame(builder, builder.setValues(raster));
        final GridCoverage coverage = testBuilder(builder, 3);
        assertSame(raster, coverage.render(null).getTile(0,0));
    }

    /**
     * Tests {@link GridCoverageBuilder#build()} with various properties defined.
     * Before to invoke this method, caller must invoke a {@code GridCoverageBuilder.setValues(…)} method
     * with an image or raster of size 5×8 pixels. This method starts by an attempt to build the coverage
     * with no other property set, then add properties like sample dimensions and grid extent one by one.
     *
     * @param  builder   the builder to test. Values must be already defined.
     * @param  numBands  the expected number of sample dimensions in the coverage.
     * @return the grid coverage created by the given builder.
     */
    private static GridCoverage testBuilder(final GridCoverageBuilder builder, final int numBands) {
        /*
         * Test creation with no properties other than data explicity set.
         * A default list of sample dimensions should be created.
         * Grid geometry should be undefined except for grid extent.
         */
        {
            final GridCoverage coverage = builder.build();
            assertEquals(numBands, coverage.getSampleDimensions().size());
            final GridGeometry gg = coverage.getGridGeometry();
            assertFalse(gg.isDefined(GridGeometry.CRS));
            assertFalse(gg.isDefined(GridGeometry.ENVELOPE));
            assertFalse(gg.isDefined(GridGeometry.GRID_TO_CRS));
            assertTrue (gg.isDefined(GridGeometry.EXTENT));
        }
        /*
         * Test creation with the sample dimensions specified. First, we try with a wrong
         * number of sample dimensions; that construction shall fail. Then try again with
         * the right number of sample dimensions. That time, construction shall succeed
         * and the coverage shall contain the sample dimensions that we specified.
         */
        {
            assertSame(builder, builder.setRanges(new SampleDimension.Builder().setName(0).build()));
            var e = assertThrows(IllegalStateException.class, () -> builder.build(),
                        "Wrong number of sample dimensions, build() should fail.");
            assertMessageContains(e);
            final var ranges = new SampleDimension[numBands];
            for (int i=0; i<numBands; i++) {
                ranges[i] = new SampleDimension.Builder().setName(i).build();
            }
            assertSame(builder, builder.setRanges(ranges));
            final GridCoverage coverage = builder.build();
            assertArrayEquals(ranges, coverage.getSampleDimensions().toArray());
        }
        /*
         * Test creation with grid extent and envelope specified. First, we try with a
         * wrong grid extent; that construction shall fail. Then try again with correct
         * grid extent.
         */
        final GridCoverage coverage = testSetDomain(builder, 5, 8);
        final Matrix gridToCRS = MathTransforms.getMatrix(coverage.getGridGeometry().getGridToCRS(PixelInCell.CELL_CENTER));
        assertEquals(2.0, gridToCRS.getElement(0, 0));
        assertEquals(0.5, gridToCRS.getElement(1, 1));
        return coverage;
    }

    /**
     * Tests {@link GridCoverageBuilder#setDomain(GridGeometry)}.
     *
     * @param  builder  the builder to test. Values must be already defined.
     * @param  width    expected width in pixels.
     * @param  height   expected height in pixels.
     * @return the grid coverage created by the given builder.
     */
    private static GridCoverage testSetDomain(final GridCoverageBuilder builder, final int width, final int height) {
        final GeneralEnvelope env = new GeneralEnvelope(HardCodedCRS.WGS84);
        env.setRange(0, 0, 10);     // Scale factor of 2 for grid size of 10.
        env.setRange(1, 0,  4);     // Scale factor of ½ for grid size of 8.
        GridGeometry grid = new GridGeometry(new GridExtent(8, 5), env, GridOrientation.HOMOTHETY);
        assertSame(builder, builder.setDomain(grid));
        try {
            builder.build();
            fail("Wrong extent size, build() should fail.");
        } catch (IllegalStateException ex) {
            assertNotNull(ex.getMessage());
        }
        grid = new GridGeometry(new GridExtent(width, height), env, GridOrientation.HOMOTHETY);
        assertSame(builder, builder.setDomain(grid));
        return builder.build();
    }

    /**
     * Tests {@link GridCoverageBuilder#setValues(DataBuffer, Dimension)}.
     */
    @Test
    public void testCreateFromBuffer() {
        final DataBuffer buffer = new DataBufferByte(new byte[] {1,2,3,4,5,6}, 6);
        final GridCoverageBuilder builder = new GridCoverageBuilder();
        assertSame(builder, builder.setValues(buffer, null));
        var e = assertThrows(IncompleteGridGeometryException.class, () -> builder.build(),
                             "Extent is undefined, build() should fail.");
        assertMessageContains(e, "size");
        final GridCoverage coverage = testSetDomain(builder, 3, 2);
        assertSame(buffer, coverage.render(null).getTile(0,0).getDataBuffer());
    }

    /**
     * Tests {@link GridCoverageBuilder#flipGridAxis(int)}.
     */
    @Test
    public void testFlipGridAxis() {
        final RenderedImage image = new BufferedImage(36, 18, BufferedImage.TYPE_INT_ARGB);
        final GeneralEnvelope domain = new GeneralEnvelope(HardCodedCRS.WGS84);
        domain.setRange(0, -180, +180);
        domain.setRange(1,  -90,  +90);

        final GridCoverageBuilder builder = new GridCoverageBuilder();
        assertSame(builder, builder.setValues(image));
        assertSame(builder, builder.setDomain(domain));
        /*
         * Test creation with the default axis direction:
         * latitude values are increasing with row indices.
         */
        {
            final GridCoverage coverage = builder.build();
            final GridGeometry gg = coverage.getGridGeometry();
            assertTrue(domain.equals(gg.getEnvelope(), STRICT, false));
            MathTransform gridToCRS = gg.getGridToCRS(PixelInCell.CELL_CENTER);
            assertEquals(new AffineTransform2D(10, 0, 0, 10, -175, -85), gridToCRS);
        }
        /*
         * Test creation with the reverse Y axis direction:
         * latitude values are decreasing with row indices.
         * This is the common orientation for images.
         */
        {
            assertSame(builder, builder.flipGridAxis(1));
            final GridCoverage coverage = builder.build();
            final GridGeometry gg = coverage.getGridGeometry();
            assertTrue(domain.equals(gg.getEnvelope(), STRICT, false));
            MathTransform gridToCRS = gg.getGridToCRS(PixelInCell.CELL_CENTER);
            assertEquals(new AffineTransform2D(10, 0, 0, -10, -175, 85), gridToCRS);
        }
    }

    /**
     * Tests constructions of a grid coverage with {@link GridGeometry#UNDEFINED} domain.
     */
    @Test
    public void testUndefinedDomain() {
        final GridCoverageBuilder builder = new GridCoverageBuilder();
        assertSame(builder, builder.setDomain(GridGeometry.UNDEFINED));
        assertSame(builder, builder.setValues(new BufferedImage(3, 4, BufferedImage.TYPE_BYTE_GRAY)));
        final GridCoverage coverage = builder.build();
        final GridExtent extent = coverage.getGridGeometry().getExtent();
        GridExtentTest.assertExtentEquals(extent, 0, 0, 2);
        GridExtentTest.assertExtentEquals(extent, 1, 0, 3);
    }
}
