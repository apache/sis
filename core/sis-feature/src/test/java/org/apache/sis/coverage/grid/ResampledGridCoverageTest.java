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
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.image.TiledImageMock;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.test.TestUtilities;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link ResampledGridCoverage} implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final strictfp class ResampledGridCoverageTest {
    /**
     * Creates a small grid coverage with arbitrary data. The rendered image will
     * have only one tile since testing tiling is not the purpose of this class.
     */
    private static GridCoverage createGridCoverage() {
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
     * Tests application of an identity transform.
     * We expect the source coverage to be returned unchanged.
     *
     * @throws TransformException if some coordinates can not be transformed to the target grid geometry.
     */
    @Test
    public void testIdentity() throws TransformException {
        final GridCoverage source = createGridCoverage();
        GridGeometry gg = source.getGridGeometry();
        gg = new GridGeometry(null, PixelInCell.CELL_CENTER, gg.getGridToCRS(PixelInCell.CELL_CENTER),
                              gg.getCoordinateReferenceSystem());
        final GridCoverage target = new GridCoverageProcessor().resample(source, gg);
        assertSame(source, target);
    }
}
