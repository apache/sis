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

import java.util.Random;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.geometry.ImmutableEnvelope;
import org.apache.sis.image.Interpolation;
import org.apache.sis.image.TiledImageMock;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.operation.HardCodedConversions;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.opengis.referencing.datum.PixelInCell.CELL_CENTER;
import static org.opengis.test.Assert.*;


/**
 * Tests the {@link ResampledGridCoverage} implementation.
 * The tests in this class does not verify interpolation values
 * (this is {@link org.apache.sis.image.ResampledImageTest} job).
 * Instead it focus on the grid geometry inferred by the operation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
@DependsOn(org.apache.sis.image.ResampledImageTest.class)
public final strictfp class ResampledGridCoverageTest {
    /**
     * Creates a small grid coverage with arbitrary data. The rendered image will
     * have only one tile since testing tiling is not the purpose of this class.
     */
    private static GridCoverage2D createGridCoverage() {
        final Random random  = TestUtilities.createRandomNumberGenerator();
        final int width  = random.nextInt(8) + 3;
        final int height = random.nextInt(8) + 3;
        final TiledImageMock image = new TiledImageMock(
                DataBuffer.TYPE_USHORT, 2,      // dataType and numBands
                random.nextInt(32) - 10,        // minX
                random.nextInt(32) - 10,        // minY
                width, height,
                width, height,
                random.nextInt(32) - 10,        // minTileX
                random.nextInt(32) - 10);       // minTileY
        image.validate();
        image.initializeAllTiles(0);
        final int x = random.nextInt(32) - 10;
        final int y = random.nextInt(32) - 10;
        final GridGeometry gg = new GridGeometry(
                new GridExtent(null, new long[] {x, y}, new long[] {x+width, y+height}, false),
                new Envelope2D(HardCodedCRS.WGS84, 20, 15, 60, 62));
        return new GridCoverage2D(gg, null, image);
    }

    /**
     * Tests application of an identity transform from an explicitly specified grid geometry.
     * We expect the source coverage to be returned unchanged.
     *
     * @throws FactoryException if transformation between CRS can not be computed.
     * @throws TransformException if some coordinates can not be transformed to the target grid geometry.
     */
    @Test
    public void testExplicitIdentity() throws FactoryException, TransformException {
        final GridCoverage2D source = createGridCoverage();
        GridGeometry gg = source.getGridGeometry();
        gg = new GridGeometry(null, CELL_CENTER, gg.getGridToCRS(CELL_CENTER), gg.getCoordinateReferenceSystem());
        final GridCoverage target = ResampledGridCoverage.create(source, gg, Interpolation.NEAREST);
        assertSame("Identity transform should result in same coverage.", source, target);
    }

    /**
     * Tests application of an identity transform without specifying explicitly the desired grid geometry.
     *
     * @throws FactoryException if transformation between CRS can not be computed.
     * @throws TransformException if some coordinates can not be transformed to the target grid geometry.
     */
    @Test
    public void testImplicitIdentity() throws FactoryException, TransformException {
        final GridCoverage2D source = createGridCoverage();
        GridGeometry gg = source.getGridGeometry();
        gg = new GridGeometry(null, CELL_CENTER, null, gg.getCoordinateReferenceSystem());
        final GridCoverage target = ResampledGridCoverage.create(source, gg, Interpolation.NEAREST);
        assertSame("Identity transform should result in same coverage.", source, target);
    }

    /**
     * Tests application of an axis swapping.
     *
     * @throws FactoryException if transformation between CRS can not be computed.
     * @throws TransformException if some coordinates can not be transformed to the target grid geometry.
     */
    @Test
    public void testAxisSwap() throws FactoryException, TransformException {
        final GridCoverage2D source = createGridCoverage();
        GridGeometry gg = new GridGeometry(null, CELL_CENTER, null, HardCodedCRS.WGS84_φλ);
        final GridCoverage target = ResampledGridCoverage.create(source, gg, Interpolation.NEAREST);
        /*
         * We expect the same image since `ResampledGridCoverage` should have been
         * able to apply the operation with only a change of `gridToCRS` transform.
         */
        assertNotSame(source, target);
        assertSame(unwrap(source.render(null)),
                   unwrap(target.render(null)));
        /*
         * As an easy way to check that axis swapping has happened, check the envelopes.
         */
        final ImmutableEnvelope se = source.getGridGeometry().envelope;
        final ImmutableEnvelope te = target.getGridGeometry().envelope;
        assertEquals(se.getLower(0), te.getLower(1), Formulas.ANGULAR_TOLERANCE);
        assertEquals(se.getLower(1), te.getLower(0), Formulas.ANGULAR_TOLERANCE);
        assertEquals(se.getUpper(0), te.getUpper(1), Formulas.ANGULAR_TOLERANCE);
        assertEquals(se.getUpper(1), te.getUpper(0), Formulas.ANGULAR_TOLERANCE);
    }

    /**
     * Unwraps the given image if it is an instance of {@link ReshapedImage}.
     */
    private static RenderedImage unwrap(final RenderedImage image) {
        assertEquals("GridCoverage.render(null) should have their origin at (0,0).", 0, image.getMinX());
        assertEquals("GridCoverage.render(null) should have their origin at (0,0).", 0, image.getMinY());
        return (image instanceof ReshapedImage) ? ((ReshapedImage) image).image : image;
    }

    /**
     * Tests application of a reprojection.
     *
     * @throws FactoryException if transformation between CRS can not be computed.
     * @throws TransformException if some coordinates can not be transformed to the target grid geometry.
     */
    @Test
    public void testReprojection() throws FactoryException, TransformException {
        final GridCoverage2D source = createGridCoverage();
        GridGeometry gg = source.getGridGeometry();
        gg = new GridGeometry(null, CELL_CENTER, null, HardCodedConversions.mercator());
        final GridCoverage target = ResampledGridCoverage.create(source, gg, Interpolation.NEAREST);
        assertTrue("GridExtent.startsAtZero", target.getGridGeometry().getExtent().startsAtZero());
        /*
         * Mercator projection does not change pixel width, but change pixel height.
         */
        final GridExtent sourceExtent = source.getGridGeometry().getExtent();
        final GridExtent targetExtent = target.getGridGeometry().getExtent();
        assertEquals(sourceExtent.getSize(0),   targetExtent.getSize(0));
        assertTrue  (sourceExtent.getSize(1) <= targetExtent.getSize(1));
    }
}
