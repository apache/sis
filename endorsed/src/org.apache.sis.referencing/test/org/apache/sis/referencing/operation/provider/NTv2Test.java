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
package org.apache.sis.referencing.operation.provider;

import java.net.URI;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import javax.measure.quantity.Angle;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.internal.shared.Formulas;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.gridded.GridFile;
import org.apache.sis.referencing.operation.gridded.GridGroup;
import org.apache.sis.referencing.operation.gridded.LoadedGrid;
import static org.apache.sis.referencing.operation.gridded.GridLoader.DEGREES_TO_SECONDS;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.measure.Units;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.system.DataDirectory;

// Test dependencies
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.TestCase.TAG_SLOW;

// Specific to the main branch:
import static org.apache.sis.test.GeoapiAssert.assertMatrixEquals;


/**
 * Tests the {@link NTv2} grid loader.
 * It will also indirectly tests {@link GridGroup} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see org.apache.sis.referencing.operation.transform.MolodenskyTransformTest#testFranceGeocentricInterpolationPoint()
 */
public final class NTv2Test extends DatumShiftTestCase {
    /**
     * Name of the file containing a small extract of the "{@code NTF_R93.gsb}" file.
     * The amount of data in this test file is less than 0.14% of the original file.
     */
    public static final String TEST_FILE = "NTF_R93-extract.gsb";

    /**
     * Name of the file to load for testing the multi-grids support.
     * This file should be present in the {@code $SIS_DATA/DatumChanges} directory.
     * The test will be skipped if this file is absent.
     */
    private static final String MULTIGRID_TEST_FILE = "NTv2_0.gsb";

    /**
     * Best accuracy found in the "{@code NTF_R93.gsb}" file.
     */
    private static final float ACCURACY = 0.001618f;

    /**
     * Creates a new test case.
     */
    public NTv2Test() {
    }

    /**
     * Tests loading a grid file and interpolating a sample point. The point used for
     * this test is given by {@link FranceGeocentricInterpolationTest#samplePoint(int)}.
     *
     * @throws Exception if an error occurred while loading or computing the grid, or while testing transformations.
     */
    @Test
    public void testLoader() throws Exception {
        testRGF93(getResource(TEST_FILE),
                 36000 - 360 * (72 + 5),    // Subgrid of RGF93 beginning at gridX = 72
                 36000 - 360 * (72),        // Subgrid uses 6 cells along longitude axis
                147600 + 360 * (74),        // Subgrid of RGF93 beginning at gridY = 74
                147600 + 360 * (74 + 6));   // Subgrid uses 7 cells along latitude axis
    }

    /**
     * Tests loading an official {@code "NTF_R93.gsb"} datum shift grid file and interpolating the sample point
     * given by {@link FranceGeocentricInterpolationTest#samplePoint(int)}. This test is normally not executed
     * because Apache SIS does not redistribute the {@code "NTF_R93.gsb"}. But developers can invoke this method
     * explicitly if they can provide a path to the {@code "NTF_R93.gsb"} file.
     *
     * @param  file  path to the official {@code "NTF_R93.gsb"} file.
     * @throws Exception if an error occurred while loading or computing the grid, or while testing transformations.
     */
    public static void testRGF93(final GridFile file) throws Exception {
        testRGF93(file, -19800, 36000, 147600, 187200);
    }

    /**
     * Implementation of {@link #testLoader()} and {@link #testRGF93(URI)}.
     *
     * @param  xmin  negative of value of {@code "W_LONG"} record.
     * @param  xmax  negative of value of {@code "E_LONG"} record.
     * @param  ymin  value of the {@code "S_LAT"} record.
     * @param  ymax  value of the {@code "N_LAT"} record.
     */
    private static void testRGF93(final GridFile file, final double xmin, final double xmax,
            final double ymin, final double ymax) throws Exception
    {
        final double cellSize = 360;
        final LoadedGrid<Angle,Angle> grid = NTv2.getOrLoad(NTv2.class, file, 2);
        assertInstanceOf(LoadedGrid.Float.class, grid, "Should not be compressed.");
        assertEquals(Units.ARC_SECOND, grid.getCoordinateUnit());
        assertEquals(Units.ARC_SECOND, grid.getTranslationUnit());
        assertEquals(2, grid.getTranslationDimensions());
        assertTrue(grid.isCellValueRatio());
        assertEquals((ACCURACY / 10) / cellSize, grid.getCellPrecision(), 0.5E-6 / cellSize);
        /*
         * Verify the envelope and the conversion between geographic coordinates and grid indices.
         * The cells are expected to have the same size (360″ or 0.1°) in longitudes and latitudes.
         */
        final Envelope envelope = grid.getDomainOfValidity();
        assertEquals(xmin, envelope.getMinimum(0), "xmin");
        assertEquals(xmax, envelope.getMaximum(0), "xmax");
        assertEquals(ymin, envelope.getMinimum(1), "ymin");
        assertEquals(ymax, envelope.getMaximum(1), "ymax");
        assertMatrixEquals(new Matrix3(-cellSize,  0,  xmax,
                                       0,  +cellSize,  ymin,
                                       0,          0,    1),
                grid.getCoordinateToGrid().inverse().getMatrix(), STRICT, "coordinateToGrid");
        /*
         * Test the same point as FranceGeocentricInterpolationTest, which is itself derived from the
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
            position[i] *= DEGREES_TO_SECONDS;
            expected[i] *= DEGREES_TO_SECONDS;
            expected[i] -= position[i];  // We will test the interpolated shifts rather than final coordinates.
        }
        grid.getCoordinateToGrid().transform(position, 0, indices, 0, 1);
        grid.interpolateInCell(indices[0], indices[1], vector);
        vector[0] *= -cellSize;   // Was positive toward west.
        vector[1] *= +cellSize;
        assertArrayEquals(expected, vector,
                FranceGeocentricInterpolationTest.ANGULAR_TOLERANCE * DEGREES_TO_SECONDS);

        // Same test as above, but let DatumShiftGrid do the conversions for us.
        assertArrayEquals(expected, grid.interpolateAt(position),
                FranceGeocentricInterpolationTest.ANGULAR_TOLERANCE * DEGREES_TO_SECONDS);
        assertSame(grid, NTv2.getOrLoad(NTv2.class, file, 2), "Grid should be cached.");
    }

    /**
     * Tests using a file containing many grids. This tests depends on the {@value #MULTIGRID_TEST_FILE}
     * to be present in the {@code $SIS_DATA/DatumChanges} directory. This test is executed only if the
     * {@link #RUN_EXTENSIVE_TESTS} flag is set.
     *
     * @throws Exception if an error occurred while loading or computing the grid, or while testing transformations.
     */
    @Test
    @Tag(TAG_SLOW)
    public void testMultiGrids() throws Exception {
        assumeTrue(RUN_EXTENSIVE_TESTS, "Extensive tests not enabled.");
        assumeTrue(DataDirectory.getenv() != null);
        final Parameters pg = Parameters.castOrWrap(new NTv2().getParameters().createValue());
        pg.getOrCreate(NTv2.FILE).setValue(new URI(MULTIGRID_TEST_FILE));
        final GridFile file = new GridFile(pg, NTv2.FILE);
        assumeTrue(Files.exists(file.path().orElseThrow()));

        final LoadedGrid<Angle,Angle> grid = NTv2.getOrLoad(NTv2.class, file, 2);
        assertInstanceOf(GridGroup.class, grid, "Should contain many grids.");
        assertEquals(Units.ARC_SECOND, grid.getCoordinateUnit());
        assertEquals(Units.ARC_SECOND, grid.getTranslationUnit());
        assertEquals(2, grid.getTranslationDimensions());
        assertTrue(grid.isCellValueRatio());
        /*
         * Area of use declared in EPSG database for coordinate operation EPSG::1693:
         *
         *     40.04°N  to  83.17°N    and    141.01°W  to  47.74°W.
         *
         * In the assertions below, the `cellSize` value has been verified in the NTv2
         * file header but the envelope bounds have been determined empirically.
         */
        final double cellSize = 300;                                    // Number of arc-seconds in a cell.
        final Envelope envelope = grid.getDomainOfValidity();
        assertEquals(-142.25 * DEGREES_TO_SECONDS, envelope.getMinimum(0), 1E-10, "xmin");
        assertEquals( -44.00 * DEGREES_TO_SECONDS, envelope.getMaximum(0), 1E-10, "xmax");
        assertEquals(  40.00 * DEGREES_TO_SECONDS, envelope.getMinimum(1), 1E-10, "ymin");
        assertEquals(  84.00 * DEGREES_TO_SECONDS, envelope.getMaximum(1), 1E-10, "ymax");
        /*
         * Test a point. This point is located on the 3th grid in the NTv2 file.
         * Consequently, if the NTv2 implementation just pickups the first grid,
         * then this test would fail with an error around 100 metres.
         */
        final double[] position = {-134.998106062 * DEGREES_TO_SECONDS, 61.000285047 * DEGREES_TO_SECONDS};
        final double[] expected = {-135.0         * DEGREES_TO_SECONDS, 61.0         * DEGREES_TO_SECONDS};
        final double[] indices  = new double[position.length];
        grid.getCoordinateToGrid().transform(position, 0, indices, 0, 1);
        final int gridX = Math.toIntExact(Math.round(indices[0]));
        final int gridY = Math.toIntExact(Math.round(indices[1]));
        assertEquals(1092, gridX);                                 // Value determined empirically.
        assertEquals( 252, gridY);
        /*
         * First check the value computed by `getCellValue(…)`. This method is only a fallback and
         * should not be invoked in normal usage, so a direct invocation is the only way to test it.
         */
        final double[] result = new double[] {
            position[0] - grid.getCellValue(0, gridX, gridY) * cellSize,    // Positive translation is toward west.
            position[1] + grid.getCellValue(1, gridX, gridY) * cellSize
        };
        assertArrayEquals(expected, result, 0.001);
        /*
         * Check `interpolateInCell(…)`, which is the method invoked by `InterpolatedTransform`
         * when `SpecializableTransform` has not been able to find the most appropriate grid.
         */
        grid.interpolateInCell(indices[0], indices[1], result);
        result[0] = position[0] - result[0] * cellSize;                     // Positive translation is toward west.
        result[1] = position[1] + result[1] * cellSize;
        assertArrayEquals(expected, result, Formulas.ANGULAR_TOLERANCE * DEGREES_TO_SECONDS);
        /*
         * Verify that the caching mechanism works for GridGroup too.
         */
        assertSame(grid, NTv2.getOrLoad(NTv2.class, file, 2), "Grid should be cached.");
    }




    //  ╔══════════════════════════════════╗
    //  ║                                  ║
    //  ║        TEST FILE CREATION        ║
    //  ║                                  ║
    //  ╚══════════════════════════════════╝

    /**
     * Writes a sub-grid of the given grid in pseudo-NTv2 format. This method is used only for creating the test file.
     * The file created by this method is not fully NTv2 compliant (in particular, we do not write complete header),
     * but we take this opportunity for testing {@code NTv2.Loader} capability to be lenient.
     *
     * <p>This method has been executed once for creating the {@code "NTF_R93-extract.gsb"} test file and should not
     * be needed anymore, but we keep it around in case we have new test files to generate. The parameter used for
     * creating the test file are:</p>
     *
     * <ul>
     *   <li>{@code gridX} = 72</li>
     *   <li>{@code gridY} = 74</li>
     *   <li>{@code nx}    =  6</li>
     *   <li>{@code ny}    =  7</li>
     * </ul>
     *
     * This ensure that the grid indices (75.7432814, 78.4451225) is included in the test file.
     * Those grid indices is the location of the (2°25′32.4187″N 48°50′40.2441″W) test point to interpolate.
     *
     * <h4>Limitations</h4>
     * This method assumes that bounding box and increments have integer values, and that any fractional part
     * is rounding errors. This is usually the case when using the {@code "SECONDS"} unit of measurement.
     * This assumption does not apply to the shift values.
     *
     * @param  grid   the full grid from which to extract a few values.
     * @param  out    where to write the test file.
     * @param  gridX  index along the longitude axis of the first cell to write.
     * @param  gridY  index along the latitude axis of the first cell to write.
     * @param  nx     number of cells to write along the longitude axis.
     * @param  ny     number of cells to write along the latitude axis.
     * @throws TransformException if an error occurred while computing the envelope.
     * @throws IOException if an error occurred while writing the test file.
     */
    public static void writeSubGrid(final LoadedGrid<Angle,Angle> grid, final Path out,
            final int gridX, final int gridY, final int nx, final int ny) throws IOException, TransformException
    {
        Envelope envelope = new Envelope2D(null, gridX, gridY, nx - 1, ny - 1);
        envelope = Envelopes.transform(grid.getCoordinateToGrid().inverse(), envelope);
        final ByteBuffer buffer = ByteBuffer.allocate(4096);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        writeString(buffer, "NUM_OREC"); buffer.putInt(5); nextRecord(buffer);
        writeString(buffer, "NUM_SREC"); buffer.putInt(7); nextRecord(buffer);
        writeString(buffer, "NUM_FILE"); buffer.putInt(1); nextRecord(buffer);
        writeString(buffer, "GS_TYPE");  writeString(buffer, "SECONDS");
        writeString(buffer, "VERSION");  writeString(buffer, "SIS_TEST");   // Last overview record.
        writeString(buffer, "S_LAT");    buffer.putDouble(StrictMath.rint( envelope.getMinimum(1)));
        writeString(buffer, "N_LAT");    buffer.putDouble(StrictMath.rint( envelope.getMaximum(1)));
        writeString(buffer, "E_LONG");   buffer.putDouble(StrictMath.rint(-envelope.getMaximum(0)));  // Sign reversed.
        writeString(buffer, "W_LONG");   buffer.putDouble(StrictMath.rint(-envelope.getMinimum(0)));
        writeString(buffer, "LAT_INC");  buffer.putDouble(StrictMath.rint( envelope.getSpan(1) / (ny - 1)));
        writeString(buffer, "LONG_INC"); buffer.putDouble(StrictMath.rint( envelope.getSpan(0) / (nx - 1)));
        writeString(buffer, "GS_COUNT"); buffer.putInt(nx * ny); nextRecord(buffer);
        for (int y=0; y<ny; y++) {
            for (int x=0; x<nx; x++) {
                buffer.putFloat((float) grid.getCellValue(1, gridX + x, gridY + y));
                buffer.putFloat((float) grid.getCellValue(0, gridX + x, gridY + y));
                buffer.putFloat(ACCURACY);
                buffer.putFloat(ACCURACY);
            }
        }
        writeString(buffer, "END");
        nextRecord(buffer);
        try (WritableByteChannel c = Files.newByteChannel(out, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            buffer.flip();
            c.write(buffer);
        }
    }

    /**
     * Writes the given string in the given buffer. It is caller's responsibility to ensure that the
     * string does not occupy more than 8 bytes in US-ASCII encoding.
     */
    private static void writeString(final ByteBuffer buffer, final String keyword) {
        final int upper = buffer.position() + 8;    // "8" is the space allowed for strings in NTv2 format.
        buffer.put(keyword.getBytes(StandardCharsets.US_ASCII));
        while (buffer.position() != upper) buffer.put((byte) ' ');
    }

    /**
     * Moves the buffer position to the next record.
     */
    private static void nextRecord(final ByteBuffer buffer) {
        buffer.position(((buffer.position() / 16) + 1) * 16);   // "16" is the length of records in NTv2 format.
    }
}
