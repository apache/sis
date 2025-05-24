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
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.image.ImageProcessor;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.privy.DoubleDouble;
import org.apache.sis.referencing.privy.DirectPositionView;
import org.apache.sis.referencing.privy.ExtendedPrecisionMatrix;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.TransformSeparator;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.matrix.Matrices;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.coverage.CannotEvaluateException;


/**
 * A multi-dimensional grid coverage where each two-dimensional slice is the resampling
 * of data from another grid coverage. This class is used when the resampling cannot be
 * stored in a {@link GridCoverage2D}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 */
final class ResampledGridCoverage extends DerivedGridCoverage {
    /**
     * The {@value} constant for identifying code specific to the two-dimensional case.
     */
    private static final int BIDIMENSIONAL = 2;

    /**
     * The transform from cell coordinates in this coverage to cell coordinates in {@linkplain #source source} coverage.
     * Note that an offset may exist between cell coordinates and pixel coordinates, so some translations may need
     * to be concatenated with this transform on an image-by-image basis.
     */
    private final MathTransform toSourceCorner, toSourceCenter;

    /**
     * Mapping from dimensions in this {@code ResampledGridCoverage} to dimensions in the {@linkplain #source source} coverage.
     * The mapping is represented by a bitmask. For a target dimension <var>i</var>, {@code toSourceDimensions[i]}
     * has a bit set to 1 for all source dimensions used in the computation of that target dimension.
     * This array may be {@code null} if the mapping cannot be computed or if it is not needed.
     */
    private final long[] toSourceDimensions;

    /**
     * The image processor to use for resampling operations. Its configuration shall not
     * be modified because this processor may be shared by different grid coverages.
     */
    private final ImageProcessor imageProcessor;

    /**
     * Value of {@link org.apache.sis.image.Interpolation#getSupportSize()}.
     * This is 1 for nearest-neighbor, 2 for bilinear, 4 for bicubic interpolation.
     */
    private final int supportSizeX, supportSizeY;

    /**
     * Creates a new grid coverage which will be the resampling of the given source.
     *
     * @param  source          the coverage to resample.
     * @param  domain          the grid extent, CRS and conversion from cell indices to CRS.
     * @param  toSourceCorner  transform from cell corner coordinates in this coverage to source coverage.
     * @param  toSourceCenter  transform from cell center coordinates in this coverage to source coverage.
     * @param  changeOfCRS     encapsulate information about the change of CRS.
     * @param  processor       the image processor to use for resampling images.
     */
    private ResampledGridCoverage(final GridCoverage source, final GridGeometry domain,
                                  final MathTransform toSourceCorner,
                                  final MathTransform toSourceCenter,
                                  final CoordinateOperationFinder changeOfCRS,
                                  ImageProcessor processor)
    {
        super(source, domain);
        this.toSourceCorner = toSourceCorner;
        this.toSourceCenter = toSourceCenter;
        toSourceDimensions  = findDependentDimensions(toSourceCenter, domain);
        /*
         * Get fill values from background values declared for each band, if any.
         * If no background value is declared, default is 0 for integer data or
         * NaN for floating point values.
         */
        processor = processor.clone();
        processor.setFillValues(getBackground());
        changeOfCRS.setAccuracyOf(processor);
        imageProcessor = GridCoverageProcessor.unique(processor);
        final Dimension s = imageProcessor.getInterpolation().getSupportSize();
        supportSizeX = s.width;
        supportSizeY = s.height;
    }

    /**
     * Returns the set of target dimensions that depend on each source dimension.
     * For a source dimension <var>i</var>, {@code dependentDimensions[i]} is a bitmask with bits set to 1
     * for each target dimension which require the source dimension <var>i</var> for its calculation.
     *
     * @param  mt      the transform (mapping pixel centers) for which to determine dimension dependencies.
     * @param  domain  domain of this {@code}.
     * @return for each source dimension, a bitmask of target dependent dimensions.
     *         May be {@code null} if the mapping cannot be computed or if it is not needed.
     */
    private static long[] findDependentDimensions(final MathTransform mt, final GridGeometry domain) {
        final int srcDim = mt.getSourceDimensions();
        if (srcDim <= BIDIMENSIONAL) return null;                           // Dimension mapping not needed.
        Matrix derivative = MathTransforms.getMatrix(mt);
        if (derivative == null) try {
            derivative = mt.derivative(new DirectPositionView.Double(
                    domain.getExtent().getPointOfInterest(PixelInCell.CELL_CENTER)));
        } catch (TransformException e) {
            GridCoverageProcessor.recoverableException("resample", e);      // Public caller of this method.
            return null;
        }
        final int tgtDim = mt.getTargetDimensions();
        final long[] usage = new long[srcDim];
        for (int i=0; i<srcDim; i++) {
            for (int j=0; j<tgtDim; j++) {
                if (derivative.getElement(j, i) != 0) {
                    if (j >= Long.SIZE) {
                        throw GridGeometry.excessiveDimension(mt);
                    }
                    usage[i] |= (1L << j);
                }
            }
        }
        return usage;
    }

    /**
     * If the given transform is a translation and all translation terms are integers, returns the translation.
     * Otherwise returns {@code null}. It does not matter if the given transform is {@link #toSourceCenter} or
     * {@link #toSourceCorner}, because those two transforms should be identical when all scale factors are 1.
     * We nevertheless test the two transforms in case one of them has rounding errors.
     */
    private static long[] getIntegerTranslation(final MathTransform toSource) {
        final Matrix m = MathTransforms.getMatrix(toSource);
        if (m == null || !Matrices.isTranslation(m)) {
            return null;
        }
        final int tc = m.getNumCol() - 1;
        final long[] translation = new long[m.getNumRow() - 1];
        for (int j = translation.length; --j >= 0;) {
            final double v = m.getElement(j, tc);
            if ((translation[j] = Math.round(v)) != v) {
                return null;
            }
        }
        return translation;
    }

    /**
     * If this coverage can be represented as a {@link GridCoverage2D} instance,
     * returns such instance. Otherwise returns {@code this}.
     *
     * @param  allowGeometryReplacement   whether to allow the replacement of grid geometry in the target coverage.
     * @param  allowOperationReplacement  whether to allow the replacement of this operation by a more efficient one.
     */
    private GridCoverage specialize(final boolean allowGeometryReplacement, final boolean allowOperationReplacement)
            throws TransformException
    {
        if (allowOperationReplacement) {
            long[] translation;
            if ((translation = getIntegerTranslation(toSourceCenter)) != null ||
                (translation = getIntegerTranslation(toSourceCorner)) != null)
            {
                // No need to allow source replacement because it is already done by caller.
                GridCoverage c = TranslatedGridCoverage.create(source, gridGeometry, translation, false);
                if (c != null) return c;
            }
        }
        GridExtent extent = gridGeometry.getExtent();
        if (extent.getDimension()    < GridCoverage2D.BIDIMENSIONAL ||
            extent.getSubDimension() > GridCoverage2D.BIDIMENSIONAL)
        {
            return this;
        }
        /*
         * If the transform is linear and the user did not specify explicitly a desired transform or grid extent
         * (i.e. user specified only a target CRS), keep same image with a different `gridToCRS` transform instead
         * than doing a resampling. The intent is to avoid creating a new image if user apparently doesn't care.
         */
        if (allowGeometryReplacement && toSourceCorner instanceof LinearTransform) {
            MathTransform gridToCRS = gridGeometry.getGridToCRS(PixelInCell.CELL_CORNER);
            if (gridToCRS instanceof LinearTransform) {
                final GridGeometry sourceGG = source.getGridGeometry();
                extent = sourceGG.getExtent();
                gridToCRS = MathTransforms.concatenate(toSourceCorner.inverse(), gridToCRS);
                final GridGeometry targetGG = new GridGeometry(extent, PixelInCell.CELL_CORNER, gridToCRS,
                                                               getCoordinateReferenceSystem());
                if (sourceGG.equals(targetGG, ComparisonMode.APPROXIMATE)) {
                    return source;
                }
                return new GridCoverage2D(source, targetGG, extent, source.render(null));
            }
        }
        return new GridCoverage2D(source, gridGeometry, extent, render(null));
    }

    /**
     * Checks if two grid geometries are equal, ignoring unspecified properties. If a geometry
     * has no extent or no {@code gridToCRS} transform, the missing property is not compared.
     * Same applies for the grid extent.
     *
     * @return {@code true} if the two geometries are equal, ignoring unspecified properties.
     */
    static boolean equivalent(final GridGeometry sourceGG, final GridGeometry targetGG) {
        return (!isDefined(sourceGG, targetGG, GridGeometry.EXTENT)
                || Utilities.equalsIgnoreMetadata(sourceGG.getExtent(),
                                                  targetGG.getExtent()))
            && (!isDefined(sourceGG, targetGG, GridGeometry.CRS)
                || Utilities.equalsIgnoreMetadata(sourceGG.getCoordinateReferenceSystem(),
                                                  targetGG.getCoordinateReferenceSystem()))
            && (!isDefined(sourceGG, targetGG, GridGeometry.GRID_TO_CRS)
                || Utilities.equalsIgnoreMetadata(sourceGG.getGridToCRS(PixelInCell.CELL_CORNER),
                                                  targetGG.getGridToCRS(PixelInCell.CELL_CORNER))
                || Utilities.equalsIgnoreMetadata(sourceGG.getGridToCRS(PixelInCell.CELL_CENTER),   // Its okay if only one is equal.
                                                  targetGG.getGridToCRS(PixelInCell.CELL_CENTER)))
            && (!isDefined(sourceGG, targetGG, GridGeometry.ENVELOPE)
                || isDefined(sourceGG, targetGG, GridGeometry.EXTENT | GridGeometry.GRID_TO_CRS)    // Compare only if not inferred.
                || sourceGG.equalsApproximately(targetGG.envelope));
    }

    /**
     * Returns whether the given property is defined in both grid geometries.
     *
     * @param  property  one of {@link GridGeometry} constants.
     */
    private static boolean isDefined(final GridGeometry sourceGG, final GridGeometry targetGG, final int property) {
        return targetGG.isDefined(property) && sourceGG.isDefined(property);
    }

    /**
     * Implementation of {@link GridCoverageProcessor#resample(GridCoverage, GridGeometry)}.
     * This method computes the <em>inverse</em> of the transform from <var>Source Grid</var>
     * to <var>Target Grid</var>. That transform will be computed using the following path:
     *
     * <blockquote>Target Grid  ⟶  Target CRS  ⟶  Source CRS  ⟶  Source Grid</blockquote>
     *
     * If the target {@link GridGeometry} is incomplete, this method provides default
     * values for the missing properties. The following cases may occur:
     *
     * <ul class="verbose">
     *   <li>
     *     User provided no {@link GridExtent}. This method will construct a "grid to CRS" transform
     *     preserving (at least approximately) axis directions and resolutions at the point of interest.
     *     Then a grid extent will be created with a size large enough for containing the original grid
     *     transformed by above <var>Source Grid</var> → <var>Target Grid</var> transform.
     *   </li><li>
     *     User provided only a {@link GridExtent}. This method will compute an envelope large enough
     *     for containing the projected coordinates, then a "grid to CRS" transform will be derived
     *     from the grid and the georeferenced envelope with an attempt to preserve axis directions
     *     at least approximately.
     *   </li><li>
     *     User provided only a "grid to CRS" transform. This method will transform the projected envelope
     *     to "grid units" using the specified transform and create a grid extent large enough to hold the
     *     result.</li>
     * </ul>
     *
     * @param  source     the grid coverage to resample.
     * @param  target     the desired geometry of returned grid coverage. May be incomplete.
     * @param  processor  the processor to use for executing the resample operation on images.
     * @param  allowOperationReplacement  whether to allow the replacement of this operation by a more efficient one.
     * @return a grid coverage with the characteristics specified in the given grid geometry.
     * @throws IncompleteGridGeometryException if the source grid geometry is missing an information.
     * @throws TransformException if some coordinates cannot be transformed to the specified target.
     */
    static GridCoverage create(final GridCoverage source, final GridGeometry target, final ImageProcessor processor,
                               final boolean allowOperationReplacement)
            throws FactoryException, TransformException
    {
        final GridGeometry sourceGG = source.getGridGeometry();
        final CoordinateOperationFinder changeOfCRS = new CoordinateOperationFinder(sourceGG, target);
        changeOfCRS.verifyPresenceOfCRS(true);
        /*
         * Compute the transform from source pixels to target CRS (to be completed to target pixels later).
         * The following lines may throw IncompleteGridGeometryException, which is desired because if that
         * transform is missing, we cannot continue (we have no way to guess it).
         */
        // Finder is initialized to PixelInCell.CELL_CORNER.
        final MathTransform sourceCornerToCRS = changeOfCRS.gridToCRS();
        final MathTransform crsToSourceCorner = changeOfCRS.inverse();
        changeOfCRS.setAnchor(PixelInCell.CELL_CENTER);
        final MathTransform sourceCenterToCRS = changeOfCRS.gridToCRS();
        final MathTransform crsToSourceCenter = changeOfCRS.inverse();
        /*
         * Compute the transform from target grid to target CRS. This transform may be unspecified,
         * in which case we need to compute a default transform trying to preserve resolution at the
         * point of interest.
         */
        boolean isGeometryExplicit = target.isDefined(GridGeometry.EXTENT);
        GridExtent targetExtent = isGeometryExplicit ? target.getExtent() : null;
        final MathTransform targetCenterToCRS;
        if (target.isDefined(GridGeometry.GRID_TO_CRS)) {
            isGeometryExplicit = true;
            targetCenterToCRS = target.getGridToCRS(PixelInCell.CELL_CENTER);
            if (targetExtent == null) {
                targetExtent = targetExtent(sourceGG.getExtent(), sourceCornerToCRS,
                        target.getGridToCRS(PixelInCell.CELL_CORNER).inverse(), false);
            }
        } else {
            /*
             * We will try to preserve resolution at the point of interest, which is typically in the center.
             * We will also try to align the grids in such a way that integer coordinates close to the point
             * of interest are integers in both grids. This correction is given by the fractional digits of
             * `originToPOI` vector (the integer digits do not matter; they will be cancelled later).
             */
            final GridExtent sourceExtent = sourceGG.getExtent();
            final double[]   sourcePOI    = sourceExtent.getPointOfInterest(PixelInCell.CELL_CENTER);
            final double[]   targetPOI    = new double[sourceCenterToCRS.getTargetDimensions()];
            final MatrixSIS  vectors      = MatrixSIS.castOrCopy(MathTransforms.derivativeAndTransform(
                                                        sourceCenterToCRS, sourcePOI, 0, targetPOI, 0));
            final double[]   originToPOI  = vectors.multiply(sourcePOI);
            /*
             * The first `vectors` column gives the displacement in target CRS when moving in source grid by one cell
             * toward right, and the second column gives the displacement when moving one cell toward up (positive y).
             * More columns may exist in 3D, 4D, etc. cases. We retain only the magnitudes of those vectors, in order
             * to build new vectors with directions parallel with target grid axes. There is one magnitude value for
             * each target CRS dimension. If there is more target grid dimensions than the number of magnitude values
             * (unusual, but not forbidden), some grid dimensions will be ignored provided that their size is 1
             * (otherwise a SubspaceNotSpecifiedException is thrown).
             */
            final MatrixSIS magnitudes = vectors.normalizeColumns();          // Length in dimension  of source grid.
            final int       crsDim     = vectors.getNumRow();                 // Number of dimensions of target CRS.
            final int       gridDim    = target.getDimension();               // Number of dimensions of target grid.
            final int       mappedDim  = Math.min(magnitudes.getNumCol(), Math.min(crsDim, gridDim));
            final MatrixSIS crsToGrid  = Matrices.create(gridDim + 1, crsDim + 1, ExtendedPrecisionMatrix.CREATE_ZERO);
            final int[]     dimSelect  = (gridDim > crsDim && targetExtent != null) ?
                                         targetExtent.getSubspaceDimensions(crsDim) : null;
            /*
             * The goal below is to build a target "gridToCRS" which perform the same axis swapping and flipping than
             * the source "gridToCRS". For example if the source "gridToCRS" was flipping y axis, then we want target
             * "gridToCRS" to also flips that axis, unless the transformation from "source CRS" to "target CRS" flips
             * that axis, in which case the result in target "gridToCRS" would be to not flip again:
             *
             *     (source gridToCRS)    →    (source CRS to target CRS)    →    (target gridToCRS)⁻¹
             *         flip y axis             no axis direction change              flip y axis
             *   or    flip y axis                  flip y axis                   no additional flip
             *
             * For each column, the row index of the greatest absolute value is taken as the target dimension where
             * to set vector magnitude in target "crsToGrid" matrix. That way, if transformation from source CRS to
             * target CRS does not flip or swap axis, target `gridToCRS` matrix should looks like source `gridToCRS`
             * matrix (i.e. zero and non-zero coefficients at the same places). If two vectors have their greatest
             * value on the same row, the largest value win (since the `vectors` matrix has been normalized to unit
             * vectors, values in different columns are comparable).
             */
            for (;;) {
                double max = -1;
                int sign   =  1;
                int tgDim  = -1;                        // Grid dimension of maximal value.
                int tcDim  = -1;                        // CRS dimension of maximal value.
                for (int i=0; i<mappedDim; i++) {
                    // `ci` differs from `i` only if the source grid has "too much" dimensions.
                    final int ci = (dimSelect != null) ? dimSelect[i] : i;
                    for (int j=0; j<crsDim; j++) {
                        final double v = vectors.getElement(j, ci);
                        final double m = Math.abs(v);
                        if (m > max) {
                            max   = m;
                            sign  = (v < 0) ? -1 : 1;           // Like `Math.signum(…)` but without 0.
                            tcDim = j;
                            tgDim = ci;
                        }
                    }
                }
                if (tgDim < 0) break;                           // No more non-zero value in the `vectors` matrix.
                for (int j=0; j<crsDim; j++) {
                    vectors.setElement(j, tgDim, Double.NaN);   // For preventing this column to be selected again.
                }
                for (int i=0; i<gridDim; i++) {
                    vectors.setElement(tcDim, i, Double.NaN);   // For preventing this row to be selected again.
                }
                DoubleDouble m = DoubleDouble.of(sign);
                m = m.divide(magnitudes.getNumber(0, tgDim), false);
                crsToGrid.setNumber(tgDim, tcDim, m);           // Scale factor from CRS coordinates to grid coordinates.
                /*
                 * Move the point of interest in a place where conversion to source grid coordinates
                 * will be close to integer. The exact location does not matter; an additional shift
                 * will be applied later for translating to target grid extent.
                 */
                m = m.multiply(DoubleDouble.sum(originToPOI[tcDim], -targetPOI[tcDim]));
                crsToGrid.setNumber(tgDim, crsDim, m);
            }
            crsToGrid.setElement(gridDim, crsDim, 1);
            /*
             * At this point we got a first estimation of "target CRS to grid" transform, without translation terms.
             * Apply the complete transform chain on source extent; this will give us a tentative target extent.
             * This tentative extent will be compared with desired target.
             */
            final GridExtent tentative = targetExtent(sourceExtent, sourceCornerToCRS,
                                                      MathTransforms.linear(crsToGrid), true);
            if (targetExtent == null) {
                // Create an extent of same size but with lower coordinates set to 0.
                if (tentative.startsAtZero()) {
                    targetExtent = tentative;
                } else {
                    final long[] coordinates = new long[gridDim * 2];
                    for (int i=0; i<gridDim; i++) {
                        coordinates[i + gridDim] = tentative.getSize(i) - 1;
                    }
                    targetExtent = new GridExtent(tentative, coordinates);
                }
            }
            /*
             * At this point we have the desired target extent and the extent that we actually got by applying
             * full "source to target" transform. Compute the scale and offset differences between target and
             * actual extents, then adjust matrix coefficients for compensating those differences.
             */
            for (int j=0; j<gridDim; j++) {
                DoubleDouble span  = DoubleDouble.of(targetExtent.getSize(j));
                DoubleDouble scale = DoubleDouble.of(tentative.getSize(j));
                scale = span.divide(scale);

                DoubleDouble offset = DoubleDouble.of(targetExtent.getLow(j));
                offset = offset.subtract(scale.multiply(tentative.getLow(j)));
                crsToGrid.convertAfter(j, scale, offset);
            }
            targetCenterToCRS = MathTransforms.linear(crsToGrid.inverse());
        }
        /*
         * At this point all target grid geometry components are non-null.
         * Build the final target GridGeometry if any components were missing.
         * If an envelope is defined, resample only that sub-region.
         */
        GridGeometry complete = target;
        ComparisonMode mode = ComparisonMode.IGNORE_METADATA;
        if (!target.isDefined(GridGeometry.EXTENT | GridGeometry.GRID_TO_CRS | GridGeometry.CRS)) {
            final CoordinateReferenceSystem targetCRS = changeOfCRS.getTargetCRS();
            complete = new GridGeometry(targetExtent, PixelInCell.CELL_CENTER, targetCenterToCRS, targetCRS);
            mode = ComparisonMode.APPROXIMATE;
            if (target.isDefined(GridGeometry.ENVELOPE)) {
                final MathTransform targetCornerToCRS = complete.getGridToCRS(PixelInCell.CELL_CORNER);
                GeneralEnvelope bounds = new GeneralEnvelope(complete.getEnvelope());
                bounds.intersect(target.getEnvelope());
                bounds = Envelopes.transform(targetCornerToCRS.inverse(), bounds);
                targetExtent = new GridExtent(bounds, false, GridRoundingMode.NEAREST, GridClippingMode.STRICT, null, null, targetExtent, null);
                complete = new GridGeometry(targetExtent, PixelInCell.CELL_CENTER, targetCenterToCRS, targetCRS);
                isGeometryExplicit = true;
            }
        }
        if (sourceGG.equals(complete, mode)) {
            return source;
        }
        /*
         * Complete the "target to source" transform.
         */
        final MathTransform targetCornerToCRS = complete.getGridToCRS(PixelInCell.CELL_CORNER);
        final ResampledGridCoverage resampled = new ResampledGridCoverage(source, complete,
                MathTransforms.concatenate(targetCornerToCRS, crsToSourceCorner),
                MathTransforms.concatenate(targetCenterToCRS, crsToSourceCenter),
                changeOfCRS, processor);
        return resampled.specialize(!isGeometryExplicit, allowOperationReplacement);
    }

    /**
     * Computes a target grid extent by transforming the source grid extent.
     *
     * <h4>Note on rounding mode</h4>
     * Calculation of source envelope should use {@link GridRoundingMode#ENCLOSING} for making sure that we include
     * all needed data. On the opposite, calculation of target envelope should use {@link GridRoundingMode#CONTAINED}
     * for making sure that we interpolate only values where data are available. However, such "fully contained" mode
     * is often overly strict because a very small rounding error can cause the lost of an image row or column,
     * while using extrapolations for those values produce no perceptible errors. Consequently, this method uses
     * {@link GridRoundingMode#NEAREST} as a compromise.
     *
     * @param  source       the source grid extent to transform.
     * @param  cornerToCRS  transform from source grid corners to target CRS.
     * @param  crsToGrid    transform from target CRS to target grid corners or centers.
     * @param  center       whether {@code crsToGrid} maps cell centers ({@code true}) or cell corners ({@code false}).
     * @return target grid extent.
     */
    private static GridExtent targetExtent(final GridExtent source, final MathTransform cornerToCRS,
            final MathTransform crsToGrid, final boolean center) throws TransformException
    {
        final MathTransform sourceToTarget = MathTransforms.concatenate(cornerToCRS, crsToGrid);
        final GeneralEnvelope bounds = source.toEnvelope(sourceToTarget, false, sourceToTarget, null);
        if (center) {
            final double[] vector = new double[bounds.getDimension()];
            Arrays.fill(vector, 0.5);
            bounds.translate(vector);       // Convert cell centers to cell corners.
        }
        return new GridExtent(bounds, false, GridRoundingMode.NEAREST, GridClippingMode.STRICT, null, null, null, null);
    }

    /**
     * Returns a two-dimensional slice of resampled grid data as a rendered image.
     *
     * @throws CannotEvaluateException if this method cannot produce the rendered image.
     */
    @Override
    public RenderedImage render(GridExtent sliceExtent) {
        if (sliceExtent == null) {
            sliceExtent = gridGeometry.getExtent();
        }
        final int width, height;            // Bounds (in pixel coordinates) of resampled image.
        final MathTransform toSource;       // From resampled image pixels to source image pixels.
        GridExtent sourceExtent;
        try {
            // Compute now for getting exception early if `sliceExtent` is invalid.
            final int[] resampledDimensions = sliceExtent.getSubspaceDimensions(BIDIMENSIONAL);
            width  = Math.toIntExact(sliceExtent.getSize(resampledDimensions[0]));
            height = Math.toIntExact(sliceExtent.getSize(resampledDimensions[1]));
            /*
             * Convert the given `sliceExtent` (in units of this grid) to units of the source grid.
             * If a dimension cannot be converted (e.g. because a `gridToCRS` transform has a NaN
             * factor in that dimension), the corresponding source grid coordinates will be copied.
             */
            final GeneralEnvelope sourceBounds = sliceExtent.toEnvelope(toSourceCorner, false, toSourceCenter, null);
            final int dimension = sourceBounds.getDimension();
            if (sourceBounds.isEmpty()) {
                final GridExtent se = source.gridGeometry.getExtent();
                for (int i=0; i<dimension; i++) {
                    double min = sourceBounds.getMinimum(i);
                    double max = sourceBounds.getMaximum(i);
                    if (Double.isNaN(min)) min = se.getLow (i);
                    if (Double.isNaN(max)) max = se.getHigh(i);
                    sourceBounds.setRange(i, min, max);
                }
            }
            /*
             * If the given `sliceExtent` has more than 2 dimensions, some dimensions must have a size of 1.
             * But the converted size may become greater than 1 after conversion to source coordinate space.
             * The following code forces the corresponding source dimensions to a thin slice in the middle.
             * This is necessary for avoiding a `SubspaceNotSpecifiedException` when requesting the slice of
             * source data.
             */
            int sourceDimX = 0, sourceDimY = 1;
            if (toSourceDimensions != null) {
                long mask = 0;
                for (final int i : resampledDimensions) {
                    mask |= toSourceDimensions[i];
                }
                sourceDimX = Long.numberOfTrailingZeros(mask);
                sourceDimY = Long.numberOfTrailingZeros(mask & ~(1L << sourceDimX));
                if (sourceDimY >= dimension) {
                    /*
                     * `mask` selected less than 2 dimensions. Unconditionally add
                     * 1 or 2 dimensions chosen among the first unused dimensions.
                     */
                    if (sourceDimX >= dimension) sourceDimX = 0;
                    sourceDimY = (sourceDimX != 0) ? 0 : 1;
                    mask = (1L << sourceDimX) | (1L << sourceDimY);
                }
                /*
                 * Modify the envelope by forcing to a thin slice
                 * all dimensions not used for interpolations.
                 */
                mask = ~mask;
                int i;
                while ((i = Long.numberOfTrailingZeros(mask)) < dimension) {
                    final double median = sourceBounds.getMedian(i);
                    if (Double.isFinite(median)) {
                        sourceBounds.setRange(i, median, median);
                    }
                    mask &= ~(1L << i);
                }
            }
            /*
             * Convert floating point values to long integers with a margin only in the dimensions
             * where interpolations will happen. All other dimensions should have a span of zero,
             * so the `ENCLOSING` rounding mode should assign a size of exactly 1 in those dimensions.
             */
            final int[] margin = new int[dimension];
            margin[sourceDimX] = supportSizeX;
            margin[sourceDimY] = supportSizeY;
            sourceExtent = new GridExtent(sourceBounds, false, GridRoundingMode.ENCLOSING, null, margin, null, null, null);
            /*
             * The transform inputs must be two-dimensional (outputs may be more flexible). If this is not the case,
             * try to extract a two-dimensional part operating only on the slice dimensions having an extent larger
             * than one cell. The choice of dimensions may vary between different calls to this `render(…)` method,
             * depending on `sliceExtent` value.
             */
            final TransformSeparator sep = new TransformSeparator(toSourceCenter);
            sep.addSourceDimensions(resampledDimensions);
            sep.addTargetDimensions(sourceDimX, sourceDimY);
            sep.setSourceExpandable(true);
            MathTransform toSourceSlice = sep.separate();
            final int[] requiredSources = sep.getSourceDimensions();
            if (requiredSources.length > BIDIMENSIONAL) {
                /*
                 * If we enter in this block, TransformSeparator cannot create a MathTransform with only the 2
                 * requested source dimensions; it needs more sources. In such case, if coordinates in missing
                 * dimensions can be set to constant values (grid low == grid high), create a transform which
                 * will add new dimensions with coordinates set to those constant values. The example below
                 * passes the two first dimensions as-is and set the third dimensions to constant value 7:
                 *
                 *     ┌   ┐   ┌         ┐┌   ┐
                 *     │ x │   │ 1  0  0 ││ x │
                 *     │ y │ = │ 0  1  0 ││ y │
                 *     │ z │   │ 0  0  7 ││ 1 │
                 *     │ 1 │   │ 0  0  1 │└   ┘
                 *     └   ┘   └         ┘
                 */
                final MatrixSIS m = Matrices.createZero(requiredSources.length + 1, BIDIMENSIONAL + 1);
                m.setElement(requiredSources.length, BIDIMENSIONAL, 1);
                for (int j=0; j < requiredSources.length; j++) {
                    final int r = requiredSources[j];
                    final int i = Arrays.binarySearch(resampledDimensions, r);
                    if (i >= 0) {
                        m.setElement(j, i, 1);
                    } else {
                        final long low = sliceExtent.getLow(r);
                        if (low == sliceExtent.getHigh(r)) {
                            m.setElement(j, BIDIMENSIONAL, low);
                        } else {
                            throw new CannotEvaluateException(Resources.format(
                                    Resources.Keys.TransformDependsOnDimension_1,
                                    sliceExtent.getAxisIdentification(r, r)));
                        }
                    }
                }
                toSourceSlice = MathTransforms.concatenate(MathTransforms.linear(m), toSourceSlice);
            }
            /*
             * Current `toSource` is a transform from source cell coordinates to target cell coordinates.
             * We need a transform from source pixel coordinates to target pixel coordinates (in images).
             * An offset may exist between cell coordinates and pixel coordinates.
             */
            final MathTransform resampledToGrid = MathTransforms.translation(
                    sliceExtent.getLow(resampledDimensions[0]),
                    sliceExtent.getLow(resampledDimensions[1]));

            final MathTransform gridToSource = MathTransforms.translation(
                    Math.negateExact(sourceExtent.getLow(sourceDimX)),
                    Math.negateExact(sourceExtent.getLow(sourceDimY)));

            toSource = MathTransforms.concatenate(resampledToGrid, toSourceSlice, gridToSource);
        } catch (FactoryException | TransformException | ArithmeticException e) {
            throw new CannotEvaluateException(e.getLocalizedMessage(), e);
        }
        /*
         * Following call is potentially costly, depending on `source` implementation.
         * For example, it may cause loading of tiles from a file. For this reason we
         * call this method only here, when remaining operations are unlikely to fail.
         */
        final RenderedImage values = source.render(sourceExtent);
        return imageProcessor.resample(values, new Rectangle(width, height), toSource);
    }
}
