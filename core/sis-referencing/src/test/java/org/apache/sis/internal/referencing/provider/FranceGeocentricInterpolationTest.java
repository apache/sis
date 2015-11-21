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
import java.io.BufferedReader;
import java.io.IOException;
import org.opengis.geometry.Envelope;
import org.opengis.util.FactoryException;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestStep;
import org.junit.Test;

import static org.opengis.test.Assert.*;

// Branch-dependent imports
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;


/**
 * Tests {@link FranceGeocentricInterpolation}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 *
 * @see GeocentricTranslationTest#testFranceGeocentricInterpolationPoint()
 * @see org.apache.sis.referencing.operation.transform.MolodenskyTransformTest#testFranceGeocentricInterpolationPoint()
 */
public final strictfp class FranceGeocentricInterpolationTest extends TestCase {
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
     * @param  step The step as a value from 1 to 3 inclusive.
     * @return The sample point at the given step.
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
     * Tests {@link FranceGeocentricInterpolation#isRecognized(Path)}.
     */
    @Test
    public void testIsRecognized() {
        assertTrue (FranceGeocentricInterpolation.isRecognized(Paths.get("GR3DF97A.txt")));
        assertTrue (FranceGeocentricInterpolation.isRecognized(Paths.get("gr3df")));
        assertFalse(FranceGeocentricInterpolation.isRecognized(Paths.get("gr3d")));
    }

    /**
     * Tests a small grid file with interpolations in geocentric coordinates.
     *
     * @throws URISyntaxException if the URL to the test file can not be converted to a path.
     * @throws IOException if an error occurred while loading the grid.
     * @throws FactoryException if an error occurred while computing the grid.
     */
    @Test
    public void testGrid() throws URISyntaxException, IOException, FactoryException {
        testGridAsShorts(testGridAsFloats());
    }

    /**
     * Tests a small grid file with interpolations in geocentric coordinates as {@code float} values.
     *
     * <p>This method is part of a chain.
     * The next method is {@link #testGridAsShorts(DatumShiftGridFile)}.</p>
     *
     * @return The loaded grid with values as {@code float}.
     * @throws URISyntaxException if the URL to the test file can not be converted to a path.
     * @throws IOException if an error occurred while loading the grid.
     * @throws FactoryException if an error occurred while computing the grid.
     */
    @TestStep
    private static DatumShiftGridFile testGridAsFloats() throws URISyntaxException, IOException, FactoryException {
        final URL url = FranceGeocentricInterpolationTest.class.getResource("GR3DF97A.txt");
        assertNotNull("Test file \"GR3DF97A.txt\" not found.", url);
        final Path file = Paths.get(url.toURI());
        final DatumShiftGridFile.Float grid;
        try (final BufferedReader in = Files.newBufferedReader(file)) {
            grid = FranceGeocentricInterpolation.load(in, file);
        }
        assertEquals("getAverageOffset",  168.2587, grid.getAverageOffset(0), 1E-4);
        assertEquals("getAverageOffset",   58.7163, grid.getAverageOffset(1), 1E-4);
        assertEquals("getAverageOffset", -320.1801, grid.getAverageOffset(2), 1E-4);
        assertEquals("getCellValue",  168.196, grid.getCellValue(0, 2, 1), 0);
        assertEquals("getCellValue",   58.778, grid.getCellValue(1, 2, 1), 0);
        assertEquals("getCellValue", -320.127, grid.getCellValue(2, 2, 1), 0);
        verifyGrid(grid);
        return grid;
    }

    /**
     * Tests a small grid file with interpolations in geocentric coordinates as {@code short} values.
     *
     * <p>This method is part of a chain.
     * The previous method is {@link #testGridAsFloats()}.</p>
     *
     * @param  grid The grid created by {@link #testGridAsFloats()}.
     * @return The given grid, but compressed as {@code short} values.
     */
    @TestStep
    private static DatumShiftGridFile testGridAsShorts(DatumShiftGridFile grid) {
        grid = DatumShiftGridCompressed.compress((DatumShiftGridFile.Float) grid, new double[] {
                FranceGeocentricInterpolation.TX,           //  168 metres
                FranceGeocentricInterpolation.TY,           //   60 metres
                FranceGeocentricInterpolation.TZ},          // -320 metres
                FranceGeocentricInterpolation.PRECISION);
        assertInstanceOf("Failed to compress 'float' values into 'short' values.", DatumShiftGridCompressed.class, grid);
        assertEquals("getAverageOffset",  168, grid.getAverageOffset(0), 0);
        assertEquals("getAverageOffset",   60, grid.getAverageOffset(1), 0);
        assertEquals("getAverageOffset", -320, grid.getAverageOffset(2), 0);
        assertEquals("getCellValue",  168.196, ((DatumShiftGridCompressed) grid).getCellValue(0, 2, 1), 0);
        assertEquals("getCellValue",   58.778, ((DatumShiftGridCompressed) grid).getCellValue(1, 2, 1), 0);
        assertEquals("getCellValue", -320.127, ((DatumShiftGridCompressed) grid).getCellValue(2, 2, 1), 0);
        verifyGrid(grid);
        return grid;
    }

    /**
     * Verify the envelope and the interpolation performed by the given grid.
     */
    private static void verifyGrid(final DatumShiftGridFile grid) {
        final Envelope envelope = grid.getDomainOfValidity();
        assertEquals("xmin",  2.2 - 0.05, Math.toDegrees(envelope.getMinimum(0)), 1E-10);
        assertEquals("xmax",  2.5 + 0.05, Math.toDegrees(envelope.getMaximum(0)), 1E-10);
        assertEquals("ymin", 48.5 - 0.05, Math.toDegrees(envelope.getMinimum(1)), 1E-10);
        assertEquals("ymax", 49.0 + 0.05, Math.toDegrees(envelope.getMaximum(1)), 1E-10);
        assertEquals("shiftDimensions", 3, grid.getShiftDimensions());
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
        final double[] offset = new double[3];
        grid.offsetAt(Math.toRadians(point[0]), Math.toRadians(point[1]), offset);
        assertArrayEquals("(ΔX, ΔY, ΔZ)", expected, offset, 0.0005);
    }

    /**
     * Tests the {@link FranceGeocentricInterpolation#getOrLoad(Path, double[], double)} method and its cache.
     *
     * @throws URISyntaxException if the URL to the test file can not be converted to a path.
     * @throws FactoryException if an error occurred while computing the grid.
     */
    @Test
    @DependsOnMethod("testGrid")
    public void testGetOrLoad() throws URISyntaxException, FactoryException {
        final URL file = FranceGeocentricInterpolationTest.class.getResource("GR3DF97A.txt");
        assertNotNull("Test file \"GR3DF97A.txt\" not found.", file);
        final DatumShiftGridFile grid = FranceGeocentricInterpolation.getOrLoad(
                Paths.get(file.toURI()), new double[] {
                        FranceGeocentricInterpolation.TX,
                        FranceGeocentricInterpolation.TY,
                        FranceGeocentricInterpolation.TZ},
                        FranceGeocentricInterpolation.PRECISION);
        verifyGrid(grid);
        assertSame("Expected a cached value.", grid, FranceGeocentricInterpolation.getOrLoad(
                Paths.get(file.toURI()), new double[] {
                        FranceGeocentricInterpolation.TX,
                        FranceGeocentricInterpolation.TY,
                        FranceGeocentricInterpolation.TZ},
                        FranceGeocentricInterpolation.PRECISION));
    }
}
