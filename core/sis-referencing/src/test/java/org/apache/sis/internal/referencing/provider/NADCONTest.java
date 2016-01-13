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

import java.util.Locale;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URISyntaxException;
import javax.measure.unit.NonSI;
import javax.measure.quantity.Angle;
import org.opengis.geometry.Envelope;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Path;
import org.apache.sis.internal.jdk7.Paths;
import org.apache.sis.internal.jdk8.JDK8;


/**
 * Tests the {@link NADCON} grid loader.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Simon Reynard (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final strictfp class NADCONTest extends TestCase {
    /**
     * Returns the sample point for a step in the transformation from NAD27 to NAD83.
     * The sample point is the coordinate of Meades Ranch station, which was the point
     * relative to which all land measurements in USA were made for NAD27.
     * Its position was officially set as 39°13′26.686″N, 98°32′30.506″W.
     * Precision is given by {@link #ANGULAR_TOLERANCE}.
     *
     * <p>The coordinate in NAD83 has been computed with the
     * <a href="http://www.ngs.noaa.gov/cgi-bin/nadcon.prl">NADCON tools on NOAA website.</a></p>
     *
     * @param  step The step as a value from 1 to 3 inclusive.
     * @return The sample point at the given step.
     */
    public static double[] samplePoint(final int step) {
        switch (step) {
            case 1: return new double[] {               // NAD27 coordinate
                      -(98 + (32 + 30.506/60)/60),      // 98°32′30.506″
                        39 + (13 + 26.686/60)/60        // 39°13′26.686″
                    };
            case 2: return new double[] {               // Shift in seconds of angle
                       -1.24121,                        // 1.24121″ (positive west in NADCON)
                        0.03498                         // 0.03498″
                    };
            case 3: return new double[] {               // NAD83 coordinate
                      -(98 + (32 + 31.74721/60)/60),    // 98°32′31.74721″
                        39 + (13 + 26.72098/60)/60      // 39°13′26.72098″
                    };
            default: throw new AssertionError(step);
        }
    }

    /**
     * The tolerance when comparing coordinates in degrees.
     * This tolerance is determined by the precision of the tools used for computing NAD83 coordinates.
     */
    public static final double ANGULAR_TOLERANCE = 0.5E-5 / DatumShiftGridLoader.DEGREES_TO_SECONDS;

    /**
     * Name of the file without extension containing a small extract of the NADCON grid.
     * The {@code ".laa"} and {@code ".loa"} extension must be added to that name.
     */
    public static final String TEST_FILE = "conus-extract";

    /**
     * Tests loading a grid file and interpolating a sample point.
     * The point used for this test is given by {@link #samplePoint(int)}.
     *
     * @throws URISyntaxException if the URL to the test file can not be converted to a path.
     * @throws IOException if an error occurred while loading the grid.
     * @throws FactoryException if an error occurred while computing the grid.
     * @throws TransformException if an error occurred while computing the envelope or testing the point.
     */
    @Test
    public void testLoader() throws URISyntaxException, IOException, FactoryException, TransformException {
        final URL latitudeShifts  = NADCONTest.class.getResource(TEST_FILE + ".laa");
        final URL longitudeShifts = NADCONTest.class.getResource(TEST_FILE + ".loa");
        assertNotNull("Test file \"" + TEST_FILE + ".laa\" not found.", latitudeShifts);
        assertNotNull("Test file \"" + TEST_FILE + ".loa\" not found.", longitudeShifts);
        testNADCON(Paths.get(latitudeShifts.toURI()),
                   Paths.get(longitudeShifts.toURI()),
                   -99.75, -98.0, 37.5, 39.75);
    }

    /**
     * Tests loading an official {@code "conus.las"} and {@code "conus.los"} datum shift grid files and interpolating
     * the sample point given by {@link #samplePoint(int)}. This test is normally not executed because Apache SIS does
     * not redistribute the datum shift grid files. But developers can invoke this method explicitely if they can
     * provide a path to those files.
     *
     * @param  latitudeShifts  Path to the official {@code "conus.las"} file.
     * @param  longitudeShifts Path to the official {@code "conus.los"} file.
     * @throws IOException if an error occurred while loading the grid.
     * @throws FactoryException if an error occurred while computing the grid.
     * @throws TransformException if an error occurred while computing the envelope or testing the point.
     */
    public static void testNADCON(final Path latitudeShifts, final Path longitudeShifts)
            throws IOException, FactoryException, TransformException
    {
        testNADCON(latitudeShifts, longitudeShifts, -131, -63, 20, 50);
    }

    /**
     * Implementation of {@link #testLoader()} and {@link #testNADCON(Path)}.
     *
     * @param xmin Westmost longitude.
     * @param xmax Eastmost longitude.
     * @param ymin Southmost latitude.
     * @param ymax Northmost latitude.
     */
    private static void testNADCON(final Path latitudeShifts, final Path longitudeShifts,
            final double xmin, final double xmax, final double ymin, final double ymax)
            throws IOException, FactoryException, TransformException
    {
        final DatumShiftGridFile<Angle,Angle> grid = NADCON.getOrLoad(latitudeShifts, longitudeShifts);
        assertInstanceOf("Should not be compressed.", DatumShiftGridFile.Float.class, grid);
        assertEquals("coordinateUnit",  NonSI.DEGREE_ANGLE, grid.getCoordinateUnit());
        assertEquals("translationUnit", NonSI.DEGREE_ANGLE, grid.getTranslationUnit());
        assertEquals("translationDimensions", 2, grid.getTranslationDimensions());
        assertTrue  ("isCellValueRatio", grid.isCellValueRatio());
        assertTrue  ("cellPrecision", grid.getCellPrecision() > 0);
        /*
         * Verify the envelope and the conversion between geographic coordinates and grid indices.
         * The cells are expected to have the same size (0.25°) in longitudes and latitudes.
         */
        final double cellSize = 0.25;
        final Envelope envelope = grid.getDomainOfValidity();
        assertEquals("xmin", xmin - cellSize/2, envelope.getMinimum(0), STRICT);
        assertEquals("xmax", xmax + cellSize/2, envelope.getMaximum(0), STRICT);
        assertEquals("ymin", ymin - cellSize/2, envelope.getMinimum(1), STRICT);
        assertEquals("ymax", ymax + cellSize/2, envelope.getMaximum(1), STRICT);
        assertMatrixEquals("coordinateToGrid",
                new Matrix3(cellSize,  0,  xmin,
                            0,  cellSize,  ymin,
                            0,         0,    1),
                grid.getCoordinateToGrid().inverse().getMatrix(), STRICT);
        /*
         * Test the Meades Ranch station. If we were using the complete Conus files, we would obtain
         * after conversion the grid indices listed on the left side. But since we are using a sub-set
         * of the grid, we rather obtain the grid indices on the right side.
         *
         *   gridX ≈ 129.83277 in official file,   ≈ 4.83277 in the test file.
         *   gridY ≈  76.89632 in official file,   ≈ 6.89632 in the test file.
         */
        final double[] position = samplePoint(1);
        final double[] expected = samplePoint(2);
        final double[] indices  = new double[position.length];
        final double[] vector   = new double[2];
        grid.getCoordinateToGrid().transform(position, 0, indices, 0, 1);
        grid.interpolateInCell(indices[0], indices[1], vector);
        vector[0] *= cellSize * DatumShiftGridLoader.DEGREES_TO_SECONDS;
        vector[1] *= cellSize * DatumShiftGridLoader.DEGREES_TO_SECONDS;
        assertArrayEquals("interpolateInCell", expected, vector, 0.5E-5);

        // Same test than above, but let DatumShiftGrid do the conversions for us.
        expected[0] /= DatumShiftGridLoader.DEGREES_TO_SECONDS;
        expected[1] /= DatumShiftGridLoader.DEGREES_TO_SECONDS;
        assertArrayEquals("interpolateAt", expected, grid.interpolateAt(position), ANGULAR_TOLERANCE);
        assertSame("Grid should be cached.", grid, NADCON.getOrLoad(latitudeShifts, longitudeShifts));
    }




    //////////////////////////////////////////////////
    ////////                                  ////////
    ////////        TEST FILE CREATION        ////////
    ////////                                  ////////
    //////////////////////////////////////////////////

    /**
     * Writes a sub-grid of the given grid in pseudo-NADCON ASCII format.
     * This method is used only for creating the test file, and the output is not fully NADCON compliant.
     * We take this opportunity for testing the parser capability to be lenient.
     *
     * <p>This method has been executed once for creating the {@code "conus-extract.laa"} and
     * {@code "conus-extract.loa"} test files and should not be needed anymore, but we keep it
     * around in case we have new test files to generate. The parameter used for creating the
     * test file are:</p>
     *
     * <ul>
     *   <li>{@code gridX} = 125</li>
     *   <li>{@code gridY} =  70</li>
     *   <li>{@code nx}    =   8</li>
     *   <li>{@code ny}    =  10</li>
     * </ul>
     *
     * This ensure that the grid indices (129.83277, 76.89632) is included in the test file.
     * Those grid indices is the location of the (39°13′26.71″N, 98°32′31.75″W) test point to interpolate.
     *
     * @param grid   The full grid from which to extract a few values.
     * @param file   Where to write the test file.
     * @param dim    0 for writing longitudes, or 1 for writing latitudes.
     * @param gridX  Index along the longitude axis of the first cell to write.
     * @param gridY  Index along the latitude axis of the first cell to write.
     * @param nx     Number of cells to write along the longitude axis.
     * @param ny     Number of cells to write along the latitude axis.
     * @throws TransformException if an error occurred while computing the envelope.
     * @throws IOException if an error occurred while writing the test file.
     */
    public static void writeSubGrid(final DatumShiftGridFile<Angle,Angle> grid, final Path file, final int dim,
            final int gridX, final int gridY, final int nx, final int ny) throws IOException, TransformException
    {
        Envelope envelope = new Envelope2D(null, gridX, gridY, nx - 1, ny - 1);
        envelope = Envelopes.transform(grid.getCoordinateToGrid().inverse(), envelope);
        final BufferedWriter out = JDK8.newBufferedWriter(file);
        try {
            out.write("NADCON EXTRACTED REGION\n");
            out.write(String.format(Locale.US, "%4d %3d %3d %11.5f %11.5f %11.5f %11.5f %11.5f\n", nx, ny, 1,
                    envelope.getMinimum(0), envelope.getSpan(0) / (nx - 1),
                    envelope.getMinimum(1), envelope.getSpan(1) / (ny - 1),
                    0.0));
            for (int y=0; y<ny; y++) {
                for (int x=0; x<nx; x++) {
                    out.write(String.format(Locale.US, " %11.6f", grid.getCellValue(dim, gridX + x, gridY + y)));
                }
                out.write('\n');
            }
        } finally {
            out.close();
        }
    }
}
