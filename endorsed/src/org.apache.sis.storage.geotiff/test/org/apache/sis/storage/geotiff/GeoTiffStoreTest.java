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
package org.apache.sis.storage.geotiff;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.nio.file.Files;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.image.DataType;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageBuilder;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.matrix.Matrix4;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertSingleton;
import static org.apache.sis.feature.Assertions.assertGridToCornerEquals;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.operation.HardCodedConversions;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import static org.opengis.test.Assertions.assertAxisDirectionsEqual;


/**
 * integration tests for {@link GeoTiffStore}.
 * This class tests indirectly (via {@link GeoTiffStore}) the {@link Reader} and {@link Writer} classes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class GeoTiffStoreTest extends TestCase {
    /**
     * Name of a test file for an untiled image with a single band in gray-scale.
     */
    static final String UNTILED = "untiled.tiff";

    /**
     * Creates a new test case.
     */
    public GeoTiffStoreTest() {
    }

    /**
     * Tests writing an image with a non-linear vertical component in the "grid to CRS" transform.
     * This method merely tests that no exception is thrown during the execution, and that reading
     * the image gives back the three-dimensional <abbr>CRS</abbr>.
     *
     * @throws Exception if an error occurred while preparing or running the test.
     */
    @Test
    public void testNonLinearVerticalTransform() throws Exception {
        final int width     = 5;
        final int height    = 4;
        final var builder   = new GridCoverageBuilder();
        final var extent    = new GridExtent(null, null, new long[] {width, height, 1}, false);
        final var crs       = CRS.compound(HardCodedCRS.WGS84_LATITUDE_FIRST, HardCodedCRS.DEPTH);
        final var gridToCRS = MathTransforms.compound(
                MathTransforms.scale(0.25, -0.25),
                MathTransforms.interpolate(new double[] {0, 1, 2}, new double[] {3, 4, 9}));
        builder.setDomain(new GridGeometry(extent, PixelInCell.CELL_CORNER, gridToCRS, crs));
        builder.setValues(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB));
        final GridCoverage coverage = builder.build();
        final Path file = Files.createTempFile("sis-test-", ".tiff");
        try {
            try (DataStore store = DataStores.openWritable(file, "GeoTIFF")) {
                assertInstanceOf(GeoTiffStore.class, store).append(coverage, null);
            }
            /*
             * Read the image that we wrote in above block. This block merely tests that no exception is thrown,
             * and that the result has the expected number of dimensions, axis order and scale factors.
             */
            try (DataStore store = DataStores.open(file, "GeoTIFF")) {
                GridCoverageResource r = assertSingleton(assertInstanceOf(GeoTiffStore.class, store).components());
                GridGeometry gg = r.getGridGeometry();
                assertEquals(3, gg.getDimension());
                assertAxisDirectionsEqual(gg.getCoordinateReferenceSystem().getCoordinateSystem(),
                        AxisDirection.EAST, AxisDirection.NORTH, AxisDirection.DOWN);

                assertGridToCornerEquals(
                        new Matrix4(0, -0.25, 0, 0,
                                    0.25,  0, 0, 0,
                                    0,     0, 1, 3,
                                    0,     0, 0, 1), gg);

                RenderedImage image = r.read(null).render(null);
                assertEquals(width,  image.getWidth(),  "width");
                assertEquals(height, image.getHeight(), "height");
            }
        } finally {
            Files.delete(file);
        }
    }

    /**
     * Writes an image and compare with the {@code "untiled.tiff"} file.
     * This is an anti-regression test, as this test does not inspect the
     * content of the file.
     *
     * @throws TransformException if an error occurred while computing the domain of the image.
     * @throws DataStoreException if an error occurred while writing the GeoTIFF file.
     * @throws IOException if an error occurred while reading the file of expected content.
     */
    @Test
    public void testWriteUntiled() throws TransformException, DataStoreException, IOException {
        var geographicArea = new GeneralEnvelope(HardCodedCRS.WGS84);
        geographicArea.setRange(0, 132, 145);   // Range of longitude values.
        geographicArea.setRange(1,  30,  42);   // Range of latitude values.
        final GridCoverage coverage = new GridCoverageBuilder()
                .setDomain(Envelopes.transform(geographicArea, HardCodedConversions.mercator()))
                .setValues(DataType.BYTE, new Dimension(32, 16), (x, y) -> 100 * y + x)
                .flipGridAxis(1)
                .build();

        final var buffer = new ByteArrayOutputStream(2284);
        try (DataStore ds = DataStores.openWritable(buffer, "geotiff")) {
            assertInstanceOf(GeoTiffStore.class, ds).append(coverage, null);
        }
        final byte[] actual = buffer.toByteArray();
        final byte[] expected;
        try (InputStream in = GeoTiffStoreTest.class.getResourceAsStream(UNTILED)) {
            assertNotNull(in, UNTILED);
            expected = in.readAllBytes();
        }
        assertArrayEquals(expected, actual);
    }
}
