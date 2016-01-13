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
package org.apache.sis.internal.referencing.provider;

import java.net.URL;
import java.net.URISyntaxException;
import java.io.IOException;
import javax.measure.unit.NonSI;
import javax.measure.quantity.Angle;
import org.opengis.geometry.Envelope;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Path;
import org.apache.sis.internal.jdk7.Paths;


/**
 * Tests the {@link NTv2} grid loader.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 *
 * @see GeocentricTranslationTest#testFranceGeocentricInterpolationPoint()
 * @see org.apache.sis.referencing.operation.transform.MolodenskyTransformTest#testFranceGeocentricInterpolationPoint()
 */
public final strictfp class NTv2Test extends TestCase {
    /**
     * Name of the file containing a small extract of the "{@code NTF_R93.gsb}" file.
     * The amount of data in this test file is less than 0.14% of the original file.
     */
    public static final String TEST_FILE = "NTF_R93-extract.gsb";

    /**
     * Best accuracy found in the "{@code NTF_R93.gsb}" file.
     */
    private static final float ACCURACY = 0.001618f;

    /**
     * Tests loading a grid file and interpolating a sample point. The point used for
     * this test is given by {@link FranceGeocentricInterpolationTest#samplePoint(int)}.
     *
     * @throws URISyntaxException if the URL to the test file can not be converted to a path.
     * @throws IOException if an error occurred while loading the grid.
     * @throws FactoryException if an error occurred while computing the grid.
     * @throws TransformException if an error occurred while computing the envelope or testing the point.
     */
    @Test
    public void testLoader() throws URISyntaxException, IOException, FactoryException, TransformException {
        final URL url = NTv2Test.class.getResource(TEST_FILE);
        assertNotNull("Test file \"" + TEST_FILE + "\" not found.", url);
        testRGF93(Paths.get(url.toURI()),
                 36000 - 360 * (72 + 5),    // Subgrid of RGF93 beginning at gridX = 72
                 36000 - 360 * (72),        // Subgrid uses 6 cells along longitude axis
                147600 + 360 * (74),        // Subgrid of RGF93 beginning at gridY = 74
                147600 + 360 * (74 + 6));   // Subgrid uses 7 cells along latitude axis
    }

    /**
     * Tests loading an official {@code "NTF_R93.gsb"} datum shift grid file and interpolating the sample point
     * given by {@link FranceGeocentricInterpolationTest#samplePoint(int)}. This test is normally not executed
     * because Apache SIS does not redistribute the {@code "NTF_R93.gsb"}. But developers can invoke this method
     * explicitely if they can provide a path to the {@code "NTF_R93.gsb"} file.
     *
     * @param  file Path to the official {@code "NTF_R93.gsb"} file.
     * @throws IOException if an error occurred while loading the grid.
     * @throws FactoryException if an error occurred while computing the grid.
     * @throws TransformException if an error occurred while computing the envelope or testing the point.
     */
    public static void testRGF93(final Path file) throws IOException, FactoryException, TransformException {
        testRGF93(file, -19800, 36000, 147600, 187200);
    }

    /**
     * Implementation of {@link #testLoader()} and {@link #testRGF93(Path)}.
     *
     * @param xmin Negative of value of {@code "W_LONG"} record.
     * @param xmax Negative of value of {@code "E_LONG"} record.
     * @param ymin Value of the {@code "S_LAT"} record.
     * @param ymax Value of the {@code "N_LAT"} record.
     */
    private static void testRGF93(final Path file, final double xmin, final double xmax,
            final double ymin, final double ymax) throws IOException, FactoryException, TransformException
    {
        final double cellSize = 360;
        final DatumShiftGridFile<Angle,Angle> grid = NTv2.getOrLoad(file);
        assertInstanceOf("Should not be compressed.", DatumShiftGridFile.Float.class, grid);
        assertEquals("coordinateUnit",  NonSI.SECOND_ANGLE, grid.getCoordinateUnit());
        assertEquals("translationUnit", NonSI.SECOND_ANGLE, grid.getTranslationUnit());
        assertEquals("translationDimensions", 2, grid.getTranslationDimensions());
        assertTrue  ("isCellValueRatio", grid.isCellValueRatio());
        assertEquals("cellPrecision", (ACCURACY / 10) / cellSize, grid.getCellPrecision(), 0.5E-6 / cellSize);
        /*
         * Verify the envelope and the conversion between geographic coordinates and grid indices.
         * The cells are expected to have the same size (360″ or 0.1°) in longitudes and latitudes.
         */
        final Envelope envelope = grid.getDomainOfValidity();
        assertEquals("xmin", xmin - cellSize/2, envelope.getMinimum(0), STRICT);
        assertEquals("xmax", xmax + cellSize/2, envelope.getMaximum(0), STRICT);
        assertEquals("ymin", ymin - cellSize/2, envelope.getMinimum(1), STRICT);
        assertEquals("ymax", ymax + cellSize/2, envelope.getMaximum(1), STRICT);
        assertMatrixEquals("coordinateToGrid",
                new Matrix3(-cellSize,  0,  xmax,
                            0,  +cellSize,  ymin,
                            0,          0,    1),
                grid.getCoordinateToGrid().inverse().getMatrix(), STRICT);
        /*
         * Test the same point than FranceGeocentricInterpolationTest, which is itself derived from the
         * NTG_88 guidance note.  If we were using the official NTF_R93.gsb file, we would obtain after
         * conversion the grid indices listed on the left side. But since we are using a sub-set of the
         * grid, we rather obtain the grid indices on the right side.
         *
         *   gridX ≈ 75.7432814 in official file,   ≈ 3.7432814 in the test file.
         *   gridY ≈ 78.4451225 in official file,   ≈ 4.4451225 in the test file.
         */
        final double[] position = FranceGeocentricInterpolationTest.samplePoint(1);
        final double[] expected = FranceGeocentricInterpolationTest.samplePoint(3);
        final double[] indices  = new double[position.length];
        final double[] vector   = new double[2];
        for (int i=0; i<expected.length; i++) {
            position[i] *= DatumShiftGridLoader.DEGREES_TO_SECONDS;
            expected[i] *= DatumShiftGridLoader.DEGREES_TO_SECONDS;
            expected[i] -= position[i];  // We will test the interpolated shifts rather than final coordinates.
        }
        grid.getCoordinateToGrid().transform(position, 0, indices, 0, 1);
        grid.interpolateInCell(indices[0], indices[1], vector);
        vector[0] *= -cellSize;   // Was positive toward west.
        vector[1] *= +cellSize;
        assertArrayEquals("interpolateInCell", expected, vector,
                FranceGeocentricInterpolationTest.ANGULAR_TOLERANCE * DatumShiftGridLoader.DEGREES_TO_SECONDS);

        // Same test than above, but let DatumShiftGrid do the conversions for us.
        assertArrayEquals("interpolateAt", expected, grid.interpolateAt(position),
                FranceGeocentricInterpolationTest.ANGULAR_TOLERANCE * DatumShiftGridLoader.DEGREES_TO_SECONDS);
        assertSame("Grid should be cached.", grid, NTv2.getOrLoad(file));
    }
}
