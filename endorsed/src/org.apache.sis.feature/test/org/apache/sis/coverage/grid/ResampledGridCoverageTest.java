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

import java.util.Arrays;
import java.util.Random;
import java.util.EnumSet;
import java.util.stream.IntStream;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.ImmutableEnvelope;
import org.apache.sis.image.Interpolation;
import org.apache.sis.image.internal.shared.TiledImage;
import org.apache.sis.image.internal.shared.ReshapedImage;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.internal.shared.Formulas;
import org.apache.sis.referencing.internal.shared.AffineTransform2D;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.TransformSeparator;
import static org.apache.sis.coverage.grid.PixelInCell.CELL_CENTER;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.image.TiledImageMock;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.operation.HardCodedConversions;
import static org.apache.sis.referencing.Assertions.assertEnvelopeEquals;
import static org.apache.sis.feature.Assertions.assertValuesEqual;
import static org.apache.sis.feature.Assertions.assertPixelsEqual;

// Specific to the main branch:
import static org.apache.sis.test.GeoapiAssert.assertAxisDirectionsEqual;


/**
 * Tests the {@link ResampledGridCoverage} implementation.
 * The tests in this class does not verify interpolation results
 * (this is {@link org.apache.sis.image.ResampledImageTest} job).
 * Instead, it focuses on the grid geometry inferred by the operation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @author  Johann Sorel (Geomatys)
 */
public final class ResampledGridCoverageTest extends TestCase {
    /**
     * The random number generator used for generating some grid coverage values.
     * Created only if needed.
     */
    private Random random;

    /**
     * Arbitrary non-zero grid coordinate for the <var>z</var> dimensions.
     */
    private int gridZ;

    /**
     * Creates a new test case.
     */
    public ResampledGridCoverageTest() {
    }

    /**
     * Creates a small grid coverage with arbitrary data. The rendered image will
     * have only one tile because testing tiling is not the purpose of this class.
     * This simple coverage is two-dimensional.
     */
    private GridCoverage2D createCoverage2D() {
        random = TestUtilities.createRandomNumberGenerator();
        final int width  = random.nextInt(8) + 3;
        final int height = random.nextInt(8) + 3;
        final TiledImageMock image = new TiledImageMock(
                DataBuffer.TYPE_USHORT, 2,      // dataType and numBands
                random.nextInt(32) - 10,        // minX (no effect on tests)
                random.nextInt(32) - 10,        // minY (no effect on tests)
                width, height,                  // Image size
                width, height,                  // Tile size
                random.nextInt(32) - 10,        // minTileX
                random.nextInt(32) - 10,        // minTileY
                random.nextBoolean());          // Banded or interleaved sample model
        image.validate();
        image.initializeAllTiles(0);
        final int x = random.nextInt(32) - 10;
        final int y = random.nextInt(32) - 10;
        final GridGeometry gg = new GridGeometry(
                new GridExtent(null, new long[] {x, y}, new long[] {x+width, y+height}, false),
                new Envelope2D(HardCodedCRS.WGS84, 20, 15, 60, 62), GridOrientation.HOMOTHETY);
        return new GridCoverage2D(gg, null, image);
    }

    /**
     * Size of a quadrant in the coverage created by {@link #createCoverageND(boolean)}.
     * The total image width and height are {@code 2*Q}.
     */
    private static final int QS = 3;

    /**
     * Low values for the grid extent created by {@link #createCoverage2D()}.
     */
    private static final int LX = 3, LY = -2;

    /**
     * Creates a coverage in {@linkplain HardCodedCRS#WGS84_3D OGC:CRS:84 + elevation} reference system.
     * If the {@code withTime} argument is {@code true}, then the coverage will also include a temporal
     * dimension. The grid coverage characteristics are:
     * <ul>
     *   <li>Dimension is 6×6.</li>
     *   <li>Grid extent starts at arbitrary non-zero low values.</li>
     *   <li>Envelope is arbitrary but stable (no random values).</li>
     *   <li>Display oriented (origin is in upper-left corner).</li>
     *   <li>3 byte bands for RGB coloration.</li>
     *   <li>Each quarter of the overall image is filled with a plain color:
     *     <table style="color:white;border-collapse:collapse;">
     *       <tbody style="border:none">
     *         <tr>
     *           <td style="width:50%; background-color:black">Black</td>
     *           <td style="width:50%; background-color:red">Red</td>
     *         </tr>
     *         <tr>
     *           <td style="width:50%; background-color:green">Green</td>
     *           <td style="width:50%; background-color:blue">Blue</td>
     *         </tr>
     *       </tbody>
     *     </table>
     *   </li>
     * </ul>
     *
     * @param  withTime  {@code false} for a three-dimensional coverage, or {@code true} for adding a temporal dimension.
     * @return a new three- or four-dimensional RGB Grid Coverage.
     */
    private GridCoverage createCoverageND(final boolean withTime) {
        random = TestUtilities.createRandomNumberGenerator();
        final BufferedImage image = new BufferedImage(2*QS, 2*QS, BufferedImage.TYPE_3BYTE_BGR);
        final int[] color = new int[QS*QS];
        /* Upper-left  quarter */ // Keep default value, which is black.
        /* Upper-right quarter */ Arrays.fill(color, Color.RED  .getRGB()); image.setRGB(QS,  0, QS, QS, color, 0, QS);
        /* Lower-left  quarter */ Arrays.fill(color, Color.GREEN.getRGB()); image.setRGB( 0, QS, QS, QS, color, 0, QS);
        /* Lower-right quarter */ Arrays.fill(color, Color.BLUE .getRGB()); image.setRGB(QS, QS, QS, QS, color, 0, QS);
        /*
         * Create an image with origin between -2 and +2. We use a random image location for more
         * complete testing, but actually the tests in this class are independent of image origin.
         * Note that grid extent origin does not need to be the same as image origin.
         */
        final int minX = random.nextInt(5) - 2;
        final int minY = random.nextInt(5) - 2;
        GridGeometry gg = createGridGeometryND(withTime ? HardCodedCRS.WGS84_4D : HardCodedCRS.WGS84_3D, 0, 1, 2, 3, false);
        final TiledImage shiftedImage = new TiledImage(null,
                image.getColorModel(),
                image.getWidth(), image.getHeight(),        // Image size
                random.nextInt(32) - 10,                    // minTileX
                random.nextInt(32) - 10,                    // minTileY
                image.getRaster().createTranslatedChild(minX, minY));
        return new GridCoverage2D(gg, null, shiftedImage);
    }

    /**
     * Creates the grid geometry associated with {@link #createCoverageND(boolean)}, optionally with swapped
     * horizontal axes and flipped Y axis. The given CRS shall have 3 or 4 dimensions.
     *
     * @param  crs    the coordinate reference system to assign to the grid geometry.
     * @param  x      dimension of <var>x</var> coordinates (typically 0).
     * @param  y      dimension of <var>y</var> coordinates (typically 1).
     * @param  z      dimension of <var>z</var> coordinates (typically 2).
     * @param  t      dimension of <var>t</var> coordinates (typically 3). Ignored if the CRS is not four-dimensional.
     * @param  flipY  whether to flip the <var>y</var> axis.
     */
    private GridGeometry createGridGeometryND(final CoordinateReferenceSystem crs,
            final int x, final int y, final int z, final int t, final boolean flipY)
    {
        final int dim = crs.getCoordinateSystem().getDimension();
        final long[] lower = new long[dim];
        final long[] upper = new long[dim];
        lower[x] = LX; upper[x] = LX + 2*QS - 1;
        lower[y] = LY; upper[y] = LY + 2*QS - 1;
        final MatrixSIS gridToCRS = Matrices.createIdentity(dim + 1);
        gridToCRS.setElement(x, x,    44./(2*QS));  // X scale
        gridToCRS.setElement(x, dim, -50./(2*QS));  // X translation
        gridToCRS.setElement(y, y,   -3.5);         // Y scale
        gridToCRS.setElement(y, dim, -0.75);        // Y translation
        gridToCRS.setElement(z, dim, -100);
        lower[z] = upper[z] = gridZ = 7;            // Arbitrary non-zero position in the grid.
        if (t < dim) {
            gridToCRS.setElement(t, dim, 48055);
            lower[t] = upper[t] = 12;
        }
        if (flipY) {
            /*
             * Lower Y coordinate before flip:    Ty₁ + scale × LY
             * Upper Y coordinate after flip:     Ty₂ − scale × (LY+2×QS−1)
             * Condition  Ty₁ = Ty₂  gives:       Ty₂ = Ty₁ + scale × (2(QS+LY)−1)
             */
            gridToCRS.setElement(y, y, 3.5);    // Inverse sign.
            gridToCRS.setElement(y, dim, -0.75 + -3.5 * (2*(QS+LY) - 1));
        }
        return new GridGeometry(new GridExtent(null, lower, upper, true),
                    CELL_CENTER, MathTransforms.linear(gridToCRS), crs);
    }

    /**
     * Verifies that the given target coverage has the same pixel values as the source coverage.
     * This method opportunistically verifies that the target {@link GridCoverage} instance has a
     * {@link GridCoverage#render(GridExtent)} implementation conforms to the specification, i.e.
     * that requesting only a sub-area results in an image where pixel coordinate (0,0) corresponds
     * to cell coordinates in the lower corner of specified {@code sliceExtent}.
     */
    private void assertContentEquals(final GridCoverage source, final GridCoverage target) {
        final int tx = random.nextInt(3);
        final int ty = random.nextInt(3);
        final GridExtent sourceExtent = source.gridGeometry.getExtent();
        final int newWidth   = StrictMath.toIntExact(sourceExtent.getSize(0) - tx);
        final int newHeight  = StrictMath.toIntExact(sourceExtent.getSize(1) - ty);
        GridExtent subExtent = new GridExtent(
                StrictMath.toIntExact(sourceExtent.getLow(0) + tx),
                StrictMath.toIntExact(sourceExtent.getLow(1) + ty),
                newWidth,
                newHeight
        );
        assertPixelsEqual(source.render(null), new Rectangle(tx, ty, newWidth, newHeight),
                          target.render(subExtent), new Rectangle(newWidth, newHeight));
    }

    /**
     * Returns a resampled coverage using processor with default configuration.
     * We use processor instead of instantiating {@link ResampledGridCoverage} directly in order
     * to test {@link GridCoverageProcessor#resample(GridCoverage, GridGeometry)} method as well.
     *
     * <p>{@link GridCoverageProcessor.Optimization#REPLACE_OPERATION} is disabled for avoiding to
     * test another operation than the resampling one.</p>
     */
    private static GridCoverage resample(final GridCoverage source, final GridGeometry target) throws TransformException {
        final GridCoverageProcessor processor = new GridCoverageProcessor();
        processor.setOptimizations(EnumSet.of(GridCoverageProcessor.Optimization.REPLACE_SOURCE));
        processor.setInterpolation(Interpolation.NEAREST);
        return processor.resample(source, target);
    }

    /**
     * Tests application of an identity transform computed from an explicitly given "grid to CRS" transform.
     * We expect the source coverage to be returned unchanged.
     *
     * @throws TransformException if some coordinates cannot be transformed to the target grid geometry.
     */
    @Test
    public void testExplicitIdentity() throws TransformException {
        final GridCoverage2D source = createCoverage2D();
        GridGeometry gg = source.getGridGeometry();
        gg = new GridGeometry(null, CELL_CENTER, gg.getGridToCRS(CELL_CENTER), gg.getCoordinateReferenceSystem());
        final GridCoverage target = resample(source, gg);
        assertSame(source, target, "Identity transform should result in same coverage.");
        assertContentEquals(source, target);
    }

    /**
     * Tests application of an identity transform without specifying explicitly the desired grid geometry.
     * This test is identical to {@link #testExplicitIdentity()} except that the "grid to CRS" transform
     * specified to the {@code resample(…)} operation is null.
     *
     * @throws TransformException if some coordinates cannot be transformed to the target grid geometry.
     */
    @Test
    public void testImplicitIdentity() throws TransformException {
        final GridCoverage2D source = createCoverage2D();
        GridGeometry gg = source.getGridGeometry();
        gg = new GridGeometry(null, CELL_CENTER, null, gg.getCoordinateReferenceSystem());
        final GridCoverage target = resample(source, gg);
        assertSame(source, target, "Identity transform should result in same coverage.");
        assertContentEquals(source, target);
    }

    /**
     * Tests resampling with a transform which is only a translation by integer values.
     * This test verifies that an optimized path (much cheaper than real resampling) is taken.
     *
     * @throws TransformException if some coordinates cannot be transformed to the target grid geometry.
     */
    @Test
    public void testIntegerTranslation() throws TransformException {
        final GridCoverageProcessor processor = new GridCoverageProcessor();    // With all optimization enabled.
        final GridCoverage source   = createCoverage2D();
        final GridGeometry sourceGG = source.getGridGeometry();
        final GridGeometry targetGG = sourceGG.shiftGrid(-10, 15);
        final GridCoverage target   = processor.resample(source, targetGG);
        assertInstanceOf(TranslatedGridCoverage.class, target, "Expected fast path.");
        assertSame(targetGG, target.getGridGeometry());
        assertEnvelopeEquals(sourceGG.getEnvelope(), targetGG.getEnvelope(), STRICT);
        /*
         * The envelope is BOX(20 15, 80 77). Evaluate a single point inside that envelope.
         * The result for identical "real world" coordinates should be the same for both coverages.
         */
        final DirectPosition2D p = new DirectPosition2D(sourceGG.getCoordinateReferenceSystem(), 50, 30);
        assertArrayEquals(source.evaluator().apply(p),
                          target.evaluator().apply(p));
    }

    /**
     * Tests application of axis swapping in a two-dimensional coverage.
     * This test verifies the envelope of resampled coverage.
     *
     * @throws TransformException if some coordinates cannot be transformed to the target grid geometry.
     */
    @Test
    public void testAxisSwap() throws TransformException {
        final GridCoverage2D source = createCoverage2D();
        GridGeometry gg = new GridGeometry(null, CELL_CENTER, null, HardCodedCRS.WGS84_LATITUDE_FIRST);
        final GridCoverage target = resample(source, gg);
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
        assertEquals(0, image.getMinX(), "GridCoverage.render(null) should have their origin at (0,0).");
        assertEquals(0, image.getMinY(), "GridCoverage.render(null) should have their origin at (0,0).");
        return (image instanceof ReshapedImage) ? ((ReshapedImage) image).source : image;
    }

    /**
     * Tests application of axis swapping in a three-dimensional coverage, together with an axis flip.
     * This test verifies that the pixel values of resampled coverage are found in expected quadrant.
     *
     * @throws TransformException if some coordinates cannot be transformed to the target grid geometry.
     */
    @Test
    public void testAxisSwapAndFlip() throws TransformException {
        final GridCoverage  source      = createCoverageND(false);
        final GridGeometry  target      = createGridGeometryND(CommonCRS.WGS84.geographic3D(), 1, 0, 2, 3, true);
        final GridCoverage  result      = resample(source, target);
        final RenderedImage sourceImage = source.render(null);
        final RenderedImage targetImage = result.render(null);
        assertEquals(target, result.getGridGeometry());
        assertEquals(0, sourceImage.getMinX());                     // As per GridCoverage.render(…) contract.
        assertEquals(0, sourceImage.getMinY());
        assertEquals(0, targetImage.getMinX());
        assertEquals(0, targetImage.getMinY());
        assertPixelsEqual(sourceImage, new Rectangle( 0, QS, QS, QS),
                          targetImage, new Rectangle( 0,  0, QS, QS));      // Green should be top-left.
        assertPixelsEqual(sourceImage, new Rectangle( 0,  0, QS, QS),
                          targetImage, new Rectangle(QS,  0, QS, QS));      // Black should be upper-right.
        assertPixelsEqual(sourceImage, new Rectangle(QS, QS, QS, QS),
                          targetImage, new Rectangle( 0, QS, QS, QS));      // Blue should be lower-left.
        assertPixelsEqual(sourceImage, new Rectangle(QS,  0, QS, QS),
                          targetImage, new Rectangle(QS, QS, QS, QS));      // Red should be lower-right.
    }

    /**
     * Tests an operation moving the dimension of temporal axis in a four-dimensional coverage.
     *
     * @throws TransformException if some coordinates cannot be transformed to the target grid geometry.
     */
    @Test
    public void testTemporalAxisMoved() throws TransformException {
        final GridCoverage source = createCoverageND(true);
        final GridGeometry target = createGridGeometryND(HardCodedCRS.WGS84_4D_TIME_FIRST, 1, 2, 3, 0, false);
        final GridCoverage result = resample(source, target);
        assertAxisDirectionsEqual(result.getGridGeometry().getCoordinateReferenceSystem().getCoordinateSystem(),
                new AxisDirection[] {AxisDirection.FUTURE, AxisDirection.EAST, AxisDirection.NORTH, AxisDirection.UP},
                "Expected (t,λ,φ,H) axes.");

        assertPixelsEqual(source.render(null), null, result.render(null), null);
    }

    /**
     * Tests resampling in a sub-region specified by a grid extent. This method uses a three-dimensional coverage,
     * which implies that this method also tests the capability to identify which slice needs to be resampled.
     *
     * @throws TransformException if some coordinates cannot be transformed to the target grid geometry.
     */
    @Test
    public void testSubGridExtent() throws TransformException {
        final GridCoverage source     = createCoverageND(false);
        final GridGeometry sourceGeom = source.getGridGeometry();
        final GridGeometry targetGeom = new GridGeometry(
                new GridExtent(null, new long[] {LX+2, LY+2, gridZ},
                                     new long[] {LX+5, LY+5, gridZ}, true),
                CELL_CENTER, sourceGeom.gridToCRS,
                sourceGeom.getCoordinateReferenceSystem());

        final GridCoverage result = resample(source, targetGeom);
        assertEquals(targetGeom, result.getGridGeometry());
        /*
         * Verify that the target coverage contains all pixel values of the source coverage.
         * Iteration over source pixels needs to be restricted to the `targetGeom` extent.
         */
        final RenderedImage sourceImage = source.render(null);
        RenderedImage targetImage = result.render(null);
        assertPixelsEqual(sourceImage, new Rectangle(2, 2, 4, 4),
                          targetImage, null);
        /*
         * Verify GridCoverage.render(GridExtent) contract: the origin of the returned image
         * shall be the lower-left corner of `sliceExtent`, which is (3,3) in this test.
         */
        targetImage = result.render(new GridExtent(null,
                new long[] {LX+3, LY+3, gridZ},
                new long[] {LX+4, LY+4, gridZ}, true));
        assertPixelsEqual(sourceImage, new Rectangle(3, 3, 2, 2),
                          targetImage, new Rectangle(0, 0, 2, 2));
    }

    /**
     * Tests resampling in a sub-region specified by a grid extent spanning a single column.
     * When trying to optimize resampling by dropping dimensions, it can happen that transform dimensions
     * are reduced to 1D. However, it is a problem for image case which requires 2D coordinates.
     * So we must ensure that resample conversion keeps at least two dimensions.
     *
     * @throws TransformException if some coordinates cannot be transformed to the target grid geometry.
     */
    @Test
    public void testSubGridExtentColumnar() throws TransformException {
        final GridCoverage2D source   = createCoverage2D();
        final GridGeometry sourceGeom = source.getGridGeometry();
        final GridExtent sourceExtent = sourceGeom.getExtent();
        final GridExtent targetExtent = new GridExtent(null,
                new long[] {sourceExtent.getLow(0), sourceExtent.getLow (1)},
                new long[] {sourceExtent.getLow(0), sourceExtent.getHigh(1)}, true);
        final GridGeometry targetGeom = new GridGeometry(
                targetExtent, CELL_CENTER,
                sourceGeom.getGridToCRS(CELL_CENTER),
                sourceGeom.getCoordinateReferenceSystem());

        final GridCoverage result = resample(source, targetGeom);
        final int height = (int) targetExtent.getSize(1);
        assertPixelsEqual(source.render(null), new Rectangle(0, 0, 1, height), result.render(null), null);
    }

    /**
     * Tests resampling in a sub-region specified by an envelope.
     *
     * @throws TransformException if some coordinates cannot be transformed to the target grid geometry.
     */
    @Test
    public void testSubGeographicArea() throws TransformException {
        final GridCoverage2D source = createCoverage2D();             // Envelope2D(20, 15, 60, 62)
        final GridGeometry gg = new GridGeometry(null,
                new Envelope2D(HardCodedCRS.WGS84, 18, 20, 17, 31), GridOrientation.HOMOTHETY);
        final GridCoverage target = resample(source, gg);
        final GridExtent sourceExtent = source.getGridGeometry().getExtent();
        final GridExtent targetExtent = target.getGridGeometry().getExtent();
        assertTrue(sourceExtent.getSize(0) > targetExtent.getSize(0));
        assertTrue(sourceExtent.getSize(1) > targetExtent.getSize(1));
    }

    /**
     * Tests application of a non-linear transform.
     *
     * @throws TransformException if some coordinates cannot be transformed to the target grid geometry.
     */
    @Test
    public void testReprojection() throws TransformException {
        final GridCoverage2D source = createCoverage2D();
        GridGeometry gg = new GridGeometry(null, CELL_CENTER, null, HardCodedConversions.mercator());
        final GridCoverage target = resample(source, gg);
        assertTrue(target.getGridGeometry().getExtent().startsAtZero());
        /*
         * Mercator projection does not change pixel width, but change pixel height.
         */
        final GridExtent sourceExtent = source.getGridGeometry().getExtent();
        final GridExtent targetExtent = target.getGridGeometry().getExtent();
        assertEquals(sourceExtent.getSize(0),   targetExtent.getSize(0));
        assertTrue  (sourceExtent.getSize(1) <= targetExtent.getSize(1));
    }

    /**
     * Tests application of a three-dimensional transform which cannot be reduced to a two-dimensional transform.
     * It happens for example when transformation of <var>x</var> or <var>y</var> coordinate depends on <var>z</var>
     * coordinate value. In such case we cannot separate the 3D transform into (2D + 1D) transforms. This method
     * verifies that {@link ResampledGridCoverage} nevertheless manages to do its work even in that situation.
     *
     * @throws TransformException if some coordinates cannot be transformed to the target grid geometry.
     */
    @Test
    public void testNonSeparableGridToCRS() throws TransformException {
        final GridCoverage source = createCoverageND(false);
        final MatrixSIS nonSeparableMatrix = Matrices.createDiagonal(4, 4);
        nonSeparableMatrix.setElement(0, 2, 1);     // Make X dependent of Z.
        nonSeparableMatrix.setElement(1, 2, 1);     // Make Y dependent of Z.
        final MathTransform nonSeparableG2C = MathTransforms.concatenate(
                source.getGridGeometry().getGridToCRS(CELL_CENTER),
                MathTransforms.linear(nonSeparableMatrix));
        {
            /*
             * The test in this block is not a `ResampleGridCoverage` test, but rather a
             * check for a condition that we need for the test performed in this method.
             */
            final TransformSeparator separator = new TransformSeparator(nonSeparableG2C);
            separator.addSourceDimensions(0, 1);
            separator.addTargetDimensions(0, 1);
            assertNotNull(assertThrows(FactoryException.class, () -> separator.separate(),
                    "Test requires a non-separable transform, but separation succeed."));
        }
        final GridGeometry targetGeom = new GridGeometry(
                null,           // Let the resample operation compute the extent automatically.
                CELL_CENTER, nonSeparableG2C,
                source.getCoordinateReferenceSystem());
        /*
         * Real test is below (above code was only initialization).
         * Target image should be 6×6 pixels, like source image.
         */
        final GridCoverage result = resample(source, targetGeom);
        assertPixelsEqual(source.render(null), null, result.render(null), null);
    }

    /**
     * Tests the addition of a temporal axis. The value to insert in the temporal coordinate can be computed
     * from the four-dimensional "grid to CRS" transform given in argument to the {@code resample(…)} method,
     * combined with the source grid extent.
     *
     * @throws TransformException if some coordinates cannot be transformed to the target grid geometry.
     */
    @Test
    public void testDimensionalityIncrease() throws TransformException {
        final GridCoverage source3D = createCoverageND(false);
        final GridGeometry target4D = createGridGeometryND(HardCodedCRS.WGS84_4D, 0, 1, 2, 3, false);
        final GridCoverage result   = resample(source3D, target4D);
        assertEquals(target4D, result.getGridGeometry());
        assertPixelsEqual(source3D.render(null), null, result.render(null), null);
    }

    /**
     * Tests the removal of temporal axis.
     *
     * @throws TransformException if some coordinates cannot be transformed to the target grid geometry.
     */
    @Test
    public void testDimensionalityReduction() throws TransformException {
        final GridGeometry target3D = createGridGeometryND(HardCodedCRS.WGS84_3D, 0, 1, 2, 3, false);
        final GridCoverage source4D = createCoverageND(true);
        final GridCoverage result   = resample(source4D, target3D);
        assertEquals(target3D, result.getGridGeometry());
        assertPixelsEqual(source4D.render(null), null, result.render(null), null);
    }

    /**
     * Tests resampling with a target domain larger than the source domain.
     * Pixel outside the source domain shall be set to fill value, which is 0.
     *
     * @throws TransformException if some coordinates cannot be transformed to the target grid geometry.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-495">SIS-495</a>
     */
    @Test
    public void testDomainIncrease() throws TransformException {
        final int size = 2;
        final CoordinateReferenceSystem crs = HardCodedCRS.WGS84;
        final BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_BYTE_GRAY);
        image.getRaster().setDataElements(0, 0, size, size, new byte[] {10, 12, 16, 14});
        final AffineTransform2D gridToCRS = new AffineTransform2D(1, 0, 0, -1, 0, 0);
        final GridGeometry sourceGrid = new GridGeometry(null, CELL_CENTER, gridToCRS, crs);
        final GridGeometry targetGrid = new GridGeometry(new GridExtent(4, 4), CELL_CENTER, gridToCRS, crs);
        final GridCoverage source     = new GridCoverage2D(sourceGrid, null, image);
        final GridCoverage target     = resample(source, targetGrid);
        assertValuesEqual(target.render(null), 0, new double[][] {
            {10, 12, 0, 0},
            {16, 14, 0, 0},
            { 0,  0, 0, 0},
            { 0,  0, 0, 0}
        });
    }

    /**
     * Tests resampling of an image associated to a coordinate system using the 0 to 360° range of longitude.
     * The image crosses the 180° longitude. The resampling does not involve map projection.
     *
     * @throws TransformException if some coordinates cannot be transformed to the target grid geometry.
     */
    @Test
    public void testLongitude360() throws TransformException {
        final int width = 32;
        final int height = 3;
        final GridGeometry source = new GridGeometry(
                new GridExtent(width, height), CELL_CENTER,
                new AffineTransform2D(1, 0, 0, -1, 164, 0),
                HardCodedCRS.WGS84.forConvention(AxesConvention.POSITIVE_RANGE));
        /*
         * Above grid extent is [164 … 195]° in longitude. The part that exceed 180° is equivalent to
         * a [-180 … -165]° range. The extent below requests only a part of it, namely [-173 … -167]°.
         * The first pixel of resampled image is the 23th pixel of original image.
         */
        final GridGeometry target = new GridGeometry(
                new GridExtent(7, height), CELL_CENTER,
                new AffineTransform2D(1, 0, 0, -1, -173, 0),
                HardCodedCRS.WGS84);

        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        image.getRaster().setPixels(0, 0, width, height, IntStream.range(100, 100 + width*height).toArray());
        final GridCoverage2D coverage = new GridCoverage2D(source, null, image);
        final GridCoverage resampled = resample(coverage, target);
        assertValuesEqual(resampled.render(null), 0, new double[][] {
            {123, 124, 125, 126, 127, 128, 129},
            {155, 156, 157, 158, 159, 160, 161},
            {187, 188, 189, 190, 191, 192, 193}
        });
    }

    /**
     * Tests map reprojection of an image associated to a coordinate system using the 0 to 360° range of longitude.
     *
     * @throws TransformException if some coordinates cannot be transformed to the target grid geometry.
     */
    @Test
    public void testReprojectionFromLongitude360() throws TransformException {
        /*
         * Longitudes from 91°E to 235°E (in WGS84 geographic CRS), which is equivalent to 91°E to 125°W.
         * Latitude range is not important for this test.
         */
        final int width  = 8;
        final int height = 5;
        final GridGeometry source = new GridGeometry(
                new GridExtent(null, null, new long[] {width, height}, false), CELL_CENTER,
                new AffineTransform2D(18, 0, 0, 19, 100, -20),
                HardCodedCRS.WGS84.forConvention(AxesConvention.POSITIVE_RANGE));
        /*
         * 180°W to 180″E (the world) and 80°S to 80°N in Mercator projection.
         * Latitude range is about the same as source grid geometry.
         */
        final double xmin = -2.0037508342789244E7;
        final GridGeometry target = new GridGeometry(
                new GridExtent(null, null, new long[] {2*width, height}, false), CELL_CENTER,
                new AffineTransform2D(-xmin/width, 0, 0, 2610000, xmin, -2376500),
                HardCodedConversions.mercator());
        /*
         * Resample the image by specifying fully the target grid geometry.
         * The grid coverage should have the exact same instance.
         */
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        image.getRaster().setPixels(0, 0, width, height, IntStream.range(100, 100 + width*height).toArray());
        final GridCoverage2D coverage = new GridCoverage2D(source, null, image);
        final GridCoverage resampled = resample(coverage, target);
        assertSame(target, resampled.getGridGeometry());
        /*
         * Sample values 100, 101, 102, … should be distributed on both sides of the image.
         */
        assertValuesEqual(resampled.render(null), 0, new double[][] {
            {104, 106, 107, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 101, 102, 103},
            {112, 114, 115, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 109, 110, 111},
            {120, 122, 123, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 117, 118, 119},
            {128, 130, 131, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 125, 126, 127},
            {136, 138, 139, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 133, 134, 135},
        });
    }

    /**
     * Returns an image with only the queries part of the given image.
     * This is a helper tools which can be invoked during debugging
     * session in IDE capable to display images.
     *
     * <h4>Usage</h4>
     * Add a new watch calling this method on wanted image.
     *
     * <h4>Limitations</h4>
     * <ul>
     *   <li>If given image color-model is null, this method assumes 3 byte/RGB image.</li>
     *   <li>Works only with single-tile images.</li>
     * </ul>
     *
     * @param source  the image to display.
     * @param extent  if non-null, crop rendering to the rectangle defined by given extent,
     *                assuming extent low coordinate matches source image (0,0) coordinate.
     * @return the image directly displayable through debugger.
     */
    private static BufferedImage debug(final RenderedImage source, final GridExtent extent) {
        Raster tile = source.getTile(source.getMinTileX(), source.getMinTileY());
        final int width, height;
        if (extent == null) {
            tile   = tile.createTranslatedChild(0, 0);
            width  = tile.getWidth();
            height = tile.getHeight();
        } else {
            width  = StrictMath.toIntExact(extent.getSize(0));
            height = StrictMath.toIntExact(extent.getSize(1));
            tile   = tile.createChild(0, 0, width, height, 0, 0, null);
        }
        final BufferedImage view;
        if (source.getColorModel() == null) {
            view = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            view.getRaster().setRect(tile);
        } else {
            final WritableRaster wr = tile.createCompatibleWritableRaster(0, 0, width, height);
            wr.setRect(tile);
            view = new BufferedImage(source.getColorModel(), wr, false, null);
        }
        return view;
    }
}
