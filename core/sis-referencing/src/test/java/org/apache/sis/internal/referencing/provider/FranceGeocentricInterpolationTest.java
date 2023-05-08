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

import java.net.URISyntaxException;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;
import org.opengis.geometry.Envelope;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestStep;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.opengis.test.Assert.assertInstanceOf;


/**
 * Tests {@link FranceGeocentricInterpolation}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 *
 * @see GeocentricTranslationTest#testFranceGeocentricInterpolationPoint()
 * @see org.apache.sis.referencing.operation.transform.MolodenskyTransformTest#testFranceGeocentricInterpolationPoint()
 *
 * @since 0.7
 */
public final class FranceGeocentricInterpolationTest extends DatumShiftTestCase {
    /**
     * Name of the file containing a small extract of the "{@code GR3DF97A.txt}" file.
     * The amount of data in this test file is less than 0.14% of the original file.
     */
    public static final String TEST_FILE = "GR3DF-extract.txt";

    /**
     * Returns the sample point for a step in the example given by the NTG_88 guidance note.
     * The steps numbers go from 1 (NTF) to 3 (RGF93).
     * Precision is given by {@link #ANGULAR_TOLERANCE}.
     *
     * <blockquote><b>Source:</b>
     * <cite>"Grille de paramètres de transformation de coordonnées GR3DF97A"</cite>
     * version 1.0, April 1997 in <a href="http://www.ign.fr">http://www.ign.fr</a>
     * </blockquote>
     *
     * @param  step  the step as a value from 1 to 3 inclusive.
     * @return the sample point at the given step.
     */
    public static double[] samplePoint(final int step) {
        switch (step) {
            case 1: return new double[] {           // NTF
                         2 + (25 + 32.4187/60)/60,  //  2°25′32.4187″
                        48 + (50 + 40.2441/60)/60   // 48°50′40.2441″
                    };
            case 2: return new double[] {           // RGF93 with constant (ΔX,ΔY,ΔZ)
                         2 + (25 + 29.8273/60)/60,  //  2°25′29.8273″
                        48 + (50 + 39.9967/60)/60   // 48°50′39.9967″
                    };
            case 3: return new double[] {           // RGF93 with interpolated (ΔX,ΔY,ΔZ)
                         2 + (25 + 29.89599/60)/60, //  2°25′29.89599″
                        48 + (50 + 40.00502/60)/60  // 48°50′40.00502″
                    };
            default: throw new AssertionError(step);
        }
    }

    /**
     * The precision of values returned by {@link #samplePoint(int)}, in degrees.
     * Use as the tolerance threshold in assertions.
     */
    public static final double ANGULAR_TOLERANCE = (0.0001 / 60 / 60) / 2;

    /**
     * Tests {@link FranceGeocentricInterpolation#isRecognized(URI)}.
     *
     * @throws URISyntaxException if the URL to the test file is not valid.
     */
    @Test
    public void testIsRecognized() throws URISyntaxException {
        assertTrue (FranceGeocentricInterpolation.isRecognized(new URI("GR3DF97A.txt")));
        assertTrue (FranceGeocentricInterpolation.isRecognized(new URI("gr3df")));
        assertFalse(FranceGeocentricInterpolation.isRecognized(new URI("gr3d")));
        assertTrue (FranceGeocentricInterpolation.isRecognized(new URI(TEST_FILE)));
    }

    /**
     * Tests {@link FranceGeocentricInterpolation#redimension(int, int)}.
     */
    @Test
    public void testRedimension() {
        MolodenskyTest.testRedimension(new FranceGeocentricInterpolation());
    }

    /**
     * Tests a small grid file with interpolations in geocentric coordinates.
     *
     * @throws URISyntaxException if the URL to the test file is not valid.
     * @throws IOException if an error occurred while loading the grid.
     * @throws FactoryException if an error occurred while computing the grid.
     * @throws TransformException if an error occurred while computing the envelope.
     */
    @Test
    public void testGrid() throws URISyntaxException, IOException, FactoryException, TransformException {
        testGridAsShorts(testGridAsFloats());
    }

    /**
     * Tests a small grid file with interpolations in geocentric coordinates as {@code float} values.
     *
     * <p>This method is part of a chain.
     * The next method is {@link #testGridAsShorts(DatumShiftGridFile)}.</p>
     *
     * @return the loaded grid with values as {@code float}.
     * @throws URISyntaxException if the URL to the test file is not valid.
     * @throws IOException if an error occurred while loading the grid.
     * @throws FactoryException if an error occurred while computing the grid.
     * @throws TransformException if an error occurred while computing the envelope.
     */
    @TestStep
    private static DatumShiftGridFile<Angle,Length> testGridAsFloats()
            throws URISyntaxException, IOException, FactoryException, TransformException
    {
        final URI file = getResource(TEST_FILE);
        final DatumShiftGridFile.Float<Angle,Length> grid;
        try (BufferedReader in = FranceGeocentricInterpolation.Loader.newBufferedReader(file)) {
            grid = FranceGeocentricInterpolation.Loader.load(in, file);
        }
        assertEquals("cellPrecision",   0.005, grid.getCellPrecision(), STRICT);
        assertEquals("getCellMean",  168.2587, grid.getCellMean(0), 0.0001);
        assertEquals("getCellMean",   58.7163, grid.getCellMean(1), 0.0001);
        assertEquals("getCellMean", -320.1801, grid.getCellMean(2), 0.0001);
        verifyGrid(grid);
        return grid;
    }

    /**
     * Tests a small grid file with interpolations in geocentric coordinates as {@code short} values.
     *
     * <p>This method is part of a chain.
     * The previous method is {@link #testGridAsFloats()}.</p>
     *
     * @param  grid  the grid created by {@link #testGridAsFloats()}.
     * @return the given grid, but compressed as {@code short} values.
     * @throws TransformException if an error occurred while computing the envelope.
     */
    @TestStep
    private static DatumShiftGridFile<Angle,Length> testGridAsShorts(DatumShiftGridFile<Angle,Length> grid)
            throws TransformException
    {
        grid = DatumShiftGridCompressed.compress((DatumShiftGridFile.Float<Angle,Length>) grid, new double[] {
                FranceGeocentricInterpolation.TX,           //  168 metres
                FranceGeocentricInterpolation.TY,           //   60 metres
                FranceGeocentricInterpolation.TZ},          // -320 metres
                FranceGeocentricInterpolation.PRECISION);
        assertInstanceOf("Failed to compress 'float' values into 'short' values.", DatumShiftGridCompressed.class, grid);
        assertEquals("cellPrecision", 0.0005, grid.getCellPrecision(), STRICT);
        assertEquals("getCellMean",  168, grid.getCellMean(0), STRICT);
        assertEquals("getCellMean",   60, grid.getCellMean(1), STRICT);
        assertEquals("getCellMean", -320, grid.getCellMean(2), STRICT);
        verifyGrid(grid);
        return grid;
    }

    /**
     * Verifies the envelope and the interpolation performed by the given grid.
     *
     * @throws TransformException if an error occurred while computing the envelope.
     */
    private static void verifyGrid(final DatumShiftGridFile<Angle,Length> grid) throws TransformException {
        final Envelope envelope = grid.getDomainOfValidity();
        assertEquals("xmin",  2.2, envelope.getMinimum(0), 1E-12);
        assertEquals("xmax",  2.5, envelope.getMaximum(0), 1E-12);
        assertEquals("ymin", 48.5, envelope.getMinimum(1), 1E-12);
        assertEquals("ymax", 49.0, envelope.getMaximum(1), 1E-12);
        /*
         * The values in the NTG_88 document are:
         *
         * (gridX=2, gridY=3)  00002    2.400000000   48.800000000  -168.252   -58.630   320.170  01   2314
         * (gridX=2, gridY=4)  00002    2.400000000   48.900000000  -168.275   -58.606   320.189  01   2314
         * (gridX=3, gridY=3)  00002    2.500000000   48.800000000  -168.204   -58.594   320.125  01   2314
         * (gridX=3, gridY=4)  00002    2.500000000   48.900000000  -168.253   -58.554   320.165  01   2314
         *
         * Directions (signs) are reversed compared to NTG_88 document.
         */
        assertEquals("translationDimensions", 3, grid.getTranslationDimensions());
        assertEquals("grid.accuracy",      0.05, grid.accuracy,              STRICT);
        assertEquals("getCellValue",    168.196, grid.getCellValue(0, 2, 1), STRICT);
        assertEquals("getCellValue",     58.778, grid.getCellValue(1, 2, 1), STRICT);
        assertEquals("getCellValue",   -320.127, grid.getCellValue(2, 2, 1), STRICT);
        /*
         * Interpolate the (ΔX, ΔY, ΔZ) at a point.
         * Directions (signs) are reversed compared to NTG_88 document.
         */
        final double[] expected = {
             168.253,       // ΔX: Toward prime meridian
              58.609,       // ΔY: Toward 90° east
            -320.170        // ΔZ: Toward north pole
        };
        final double[] point  = samplePoint(3);
        final double[] vector = grid.interpolateAt(point[0], point[1]);
        assertArrayEquals("(ΔX, ΔY, ΔZ)", expected, vector, 0.0005);
    }

    /**
     * Tests the {@link FranceGeocentricInterpolation#getOrLoad(URI, double[], double)} method and its cache.
     *
     * @throws Exception if an error occurred while loading or computing the grid, or while testing transformations.
     */
    @Test
    @DependsOnMethod("testGrid")
    public void testGetOrLoad() throws Exception {
        final DatumShiftGridFile<Angle,Length> grid = FranceGeocentricInterpolation.getOrLoad(
                getResource(TEST_FILE), new double[] {
                        FranceGeocentricInterpolation.TX,
                        FranceGeocentricInterpolation.TY,
                        FranceGeocentricInterpolation.TZ},
                        FranceGeocentricInterpolation.PRECISION);
        verifyGrid(grid);
        assertSame("Expected a cached value.", grid, FranceGeocentricInterpolation.getOrLoad(
                getResource(TEST_FILE), new double[] {
                        FranceGeocentricInterpolation.TX,
                        FranceGeocentricInterpolation.TY,
                        FranceGeocentricInterpolation.TZ},
                        FranceGeocentricInterpolation.PRECISION));
    }
}
