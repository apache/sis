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

import java.util.List;
import java.util.Arrays;
import java.util.Optional;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import org.opengis.util.FactoryException;
import org.opengis.coverage.CannotEvaluateException;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.image.Interpolation;
import org.apache.sis.image.ResampledImage;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.internal.util.DoubleDouble;
import org.apache.sis.internal.coverage.j2d.ImageUtilities;
import org.apache.sis.internal.referencing.DirectPositionView;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.CRS;


/**
 * A multi-dimensional grid coverage where each two-dimensional slice is the resampling
 * of data from another grid coverage. This class is used when the resampling can not be
 * stored in a {@link GridCoverage2D}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class ResampledGridCoverage extends GridCoverage {
    /**
     * The {@value} constant for identifying code specific to the two-dimensional case.
     */
    private static final int BIDIMENSIONAL = 2;

    /**
     * The coverage to resample.
     */
    private final GridCoverage source;

    /**
     * The transform from cell coordinates in this coverage to cell coordinates in {@linkplain #source} coverage.
     * Note that an offset may exist between cell coordinates and pixel coordinates, so some translations may need
     * to be concatenated with this transform on an image-by-image basis.
     */
    private final MathTransform toSource;

    /**
     * The interpolation method to use for resampling images.
     */
    private final Interpolation interpolation;

    /**
     * Indices of extent dimensions corresponding to image <var>x</var> and <var>y</var> coordinates.
     * Typical values are 0 for {@code xDimension} and 1 for {@code yDimension}, but different values
     * are allowed. This class select the dimensions having largest size.
     */
    private final int xDimension, yDimension;

    /**
     * Creates a new grid coverage which will be the resampling of the given source.
     *
     * @param  source         the coverage to resample.
     * @param  domain         the grid extent, CRS and conversion from cell indices to CRS.
     * @param  toSource       transform from cell coordinates in this coverage to source coverage.
     * @param  interpolation  the interpolation method to use for resampling images.
     */
    private ResampledGridCoverage(final GridCoverage source, final GridGeometry domain,
                               final MathTransform toSource, final Interpolation interpolation)
    {
        super(source, domain);
        this.source        = source;
        this.toSource      = toSource;
        this.interpolation = interpolation;
        final GridExtent extent = domain.getExtent();
        long size1 = 0; int idx1 = 0;
        long size2 = 0; int idx2 = 1;
        final int dimension = extent.getDimension();
        for (int i=0; i<dimension; i++) {
            final long size = extent.getSize(i);
            if (size > size1) {
                size2 = size1; idx2 = idx1;
                size1 = size;  idx1 = i;
            } else if (size > size2) {
                size2 = size;  idx2 = i;
            }
        }
        if (idx1 < idx2) {          // Keep (x,y) dimensions in the order they appear.
            xDimension = idx1;
            yDimension = idx2;
        } else {
            xDimension = idx2;
            yDimension = idx1;
        }
    }

    /**
     * If this coverage can be represented as a {@link GridCoverage2D} instance,
     * returns such instance. Otherwise returns {@code this}.
     */
    private GridCoverage specialize() {
        final GridExtent extent = gridGeometry.getExtent();
        if (extent.getDimension() < GridCoverage2D.MIN_DIMENSION || extent.getSubDimension() > BIDIMENSIONAL) {
            return this;
        }
        return new GridCoverage2D(source, gridGeometry, extent, render(null), xDimension, yDimension);
    }

    /**
     * Implementation of {@link GridCoverageProcessor#resample(GridCoverage, GridGeometry)}.
     *
     * @param  source  the grid coverage to resample.
     * @param  target  the desired geometry of returned grid coverage. May be incomplete.
     * @return a grid coverage with the characteristics specified in the given grid geometry.
     * @throws IncompleteGridGeometryException if the source grid geometry is missing an information.
     * @throws TransformException if some coordinates can not be transformed to the specified target.
     */
    static GridCoverage create(final GridCoverage source, GridGeometry target, final Interpolation interpolation)
            throws FactoryException, TransformException
    {
        final CoordinateReferenceSystem sourceCRS = source.getCoordinateReferenceSystem();
        final CoordinateReferenceSystem targetCRS = target.isDefined(GridGeometry.CRS) ?
                                                    target.getCoordinateReferenceSystem() : sourceCRS;
        /*
         * Get the coordinate operation from source CRS to target CRS. It may be the identity operation,
         * or null only if there is not enough information for determining the operation. We try to take
         * envelopes in account because the operation choice may depend on the geographic area.
         */
        CoordinateOperation changeOfCRS = null;
        final GridGeometry sourceGG = source.getGridGeometry();
        if (sourceGG.isDefined(GridGeometry.ENVELOPE) && target.isDefined(GridGeometry.ENVELOPE)) {
            changeOfCRS = Envelopes.findOperation(sourceGG.getEnvelope(), target.getEnvelope());
        }
        if (changeOfCRS == null) try {
            DefaultGeographicBoundingBox areaOfInterest = null;
            if (sourceGG.isDefined(GridGeometry.ENVELOPE)) {
                areaOfInterest = new DefaultGeographicBoundingBox();
                areaOfInterest.setBounds(sourceGG.getEnvelope());
            }
            changeOfCRS = CRS.findOperation(sourceCRS, targetCRS, areaOfInterest);
        } catch (IncompleteGridGeometryException e) {
            // Happen if the source GridCoverage does not define a CRS.
            GridCoverageProcessor.recoverableException("resample", e);
        }
        /*
         * Compute the transform from source pixels to target CRS (to be completed to target pixels later).
         * The following line may throw IncompleteGridGeometryException, which is desired because if that
         * transform is missing, we can not continue (we have no way to guess it).
         */
        MathTransform sourceGridToCRS = sourceGG.getGridToCRS(PixelInCell.CELL_CENTER);
        if (changeOfCRS != null) {
            sourceGridToCRS = MathTransforms.concatenate(sourceGridToCRS, changeOfCRS.getMathTransform());
        }
        /*
         * Compute the transform from target grid to target CRS. This transform may be unspecified,
         * in which case we need to compute a default transform trying to preserve resolution at the
         * point of interest.
         */
        GridExtent targetExtent = target.isDefined(GridGeometry.EXTENT) ? target.getExtent() : null;
        final MathTransform targetGridToCRS;
        if (target.isDefined(GridGeometry.GRID_TO_CRS)) {
            targetGridToCRS = target.getGridToCRS(PixelInCell.CELL_CENTER);
            if (targetExtent == null) {
                targetExtent = targetExtent(sourceGG, changeOfCRS, targetGridToCRS.inverse());
            }
        } else {
            /*
             * The first column below gives the displacement in target CRS when moving is the source grid by one cell
             * toward right, and the second column gives the displacement when moving one cell toward up (positive y).
             * More columns may exist in 3D, 4D, etc. cases.
             */
            final MatrixSIS vectors = MatrixSIS.castOrCopy(sourceGridToCRS.derivative(
                    new DirectPositionView.Double(sourceGG.getExtent().getPointOfInterest())));
            /*
             * We will retain only the magnitudes of those vectors, in order to have directions parallel with target
             * grid axes. There is one magnitude value for each target CRS dimension. If there is more target grid
             * dimensions than magnitude values (unusual, but not forbidden), some grid dimensions will be ignored
             * provided that their size is 1 (otherwise a SubspaceNotSpecifiedException is thrown).
             */
            final double[]  magnitudes = vectors.normalizeColumns();          // Length is dimension  of source grid.
            final int       crsDim     = vectors.getNumRow();                 // Number of dimensions of target CRS.
            final int       gridDim    = target.getDimension();               // Number of dimensions of target grid.
            final int       mappedDim  = Math.min(magnitudes.length, Math.min(crsDim, gridDim));
            final MatrixSIS crsToGrid  = Matrices.createZero(gridDim + 1, crsDim + 1);
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
                double max = 0;
                int tgDim = -1;                         // Grid dimension of maximal value.
                int tcDim = -1;                         // CRS dimension of maximal value.
                for (int i=0; i<mappedDim; i++) {
                    // `ci` differs from `i` only if the source grid has "too much" dimensions.
                    final int ci = (dimSelect != null) ? dimSelect[i] : i;
                    for (int j=0; j<crsDim; j++) {
                        final double m = Math.abs(vectors.getElement(j,ci));
                        if (m > max) {
                            max   = m;
                            tcDim = j;
                            tgDim = ci;
                        }
                    }
                }
                if (tgDim < 0) break;                   // No more non-zero value in the `vectors` matrix.
                for (int j=0; j<crsDim; j++) {
                    vectors.setElement(j, tgDim, 0);    // For preventing this column to be selected again.
                }
                crsToGrid.setElement(tgDim, tcDim, magnitudes[tgDim]);
            }
            crsToGrid.setElement(gridDim, crsDim, 1);
            /*
             * At this point we got a first estimation of "target CRS to grid" transform, without translation terms.
             * Apply the complete transform chain on source extent; this will give us a tentative target extent.
             * This tentative extent will be compared with desired target.
             */
            final GridExtent tentative = targetExtent(sourceGG, changeOfCRS, MathTransforms.linear(crsToGrid));
            if (targetExtent == null) {
                // Create an extent of same size but with lower coordinates set to 0.
                final long[] coordinates = new long[gridDim * 2];
                for (int i=0; i<gridDim; i++) {
                    coordinates[i + gridDim] = tentative.getSize(i);
                }
                targetExtent = new GridExtent(tentative, coordinates);
            }
            /*
             * At this point we have the desired target extent and the extent that we actually got by applying
             * full "source to target" transform. Compute the scale and offset differences between target and
             * actual extents, then adjust matrix coefficients for compensating those differences.
             */
            final DoubleDouble scale  = new DoubleDouble();
            final DoubleDouble offset = new DoubleDouble();
            final DoubleDouble tmp    = new DoubleDouble();
            for (int j=0; j<gridDim; j++) {
                tmp.set(targetExtent.getSize(j));
                scale.set(tentative.getSize(j));
                scale.inverseDivide(tmp);

                tmp.set(targetExtent.getLow(j));
                offset.set(-tentative.getLow(j));
                offset.multiply(scale);
                offset.add(tmp);
                crsToGrid.convertAfter(j, scale, offset);
            }
            targetGridToCRS = MathTransforms.linear(crsToGrid.inverse());
        }
        /*
         * At this point all target grid geometry components are non-null.
         * Build the final target GridGeometry if any components were missing.
         */
        if (!target.isDefined(GridGeometry.EXTENT | GridGeometry.GRID_TO_CRS | GridGeometry.CRS)) {
            target = new GridGeometry(targetExtent, PixelInCell.CELL_CENTER, targetGridToCRS, targetCRS);
        }
        if (sourceGG.equals(target)) {
            return source;
        }
        /*
         * Complete the "target to source" transform.
         */
        final MathTransform toSource = MathTransforms.concatenate(targetGridToCRS, sourceGridToCRS.inverse());
        return new ResampledGridCoverage(source, target, toSource, interpolation).specialize();
    }

    /**
     * Computes a target grid extent by transforming the source grid extent. This method expects a transform
     * mapping cell centers, but will adjust for getting a result as if the transform was mapping cell corners.
     *
     * @param  source       the source grid geometry.
     * @param  changeOfCRS  the coordinate operation from source CRS to target CRS, or {@code null}.
     * @param  crsToGrid    the transform from target CRS to target grid, mapping cell <em>centers</em>.
     * @return target grid extent.
     */
    private static GridExtent targetExtent(final GridGeometry source, final CoordinateOperation changeOfCRS,
            final MathTransform crsToGrid) throws TransformException
    {
        MathTransform extentMapper = source.getGridToCRS(PixelInCell.CELL_CORNER);
        if (changeOfCRS != null) {
            extentMapper = MathTransforms.concatenate(extentMapper, changeOfCRS.getMathTransform());
        }
        extentMapper = MathTransforms.concatenate(extentMapper, crsToGrid);
        final GeneralEnvelope bounds = source.getExtent().toCRS(extentMapper, extentMapper, null);
        final double[] vector = new double[bounds.getDimension()];
        Arrays.fill(vector, 0.5);
        bounds.translate(vector);       // Convert cell centers to cell corners.
        return new GridExtent(bounds, GridRoundingMode.NEAREST, null, null, null);
    }

    /**
     * Returns a two-dimensional slice of resampled grid data as a rendered image.
     */
    @Override
    public RenderedImage render(final GridExtent sliceExtent) {
        if (sliceExtent != null) {
            throw new CannotEvaluateException("Slice extent not yet supported.");
        }
        final RenderedImage image        = source.render(null);           // TODO: compute slice.
        final GridExtent    sourceExtent = source.getGridGeometry().getExtent();
        final GridExtent    targetExtent = gridGeometry.getExtent();
        final Rectangle     bounds       = new Rectangle(Math.toIntExact(targetExtent.getSize(xDimension)),
                                                         Math.toIntExact(targetExtent.getSize(yDimension)));
        /*
         * `this.toSource` is a transform from source cell coordinates to target cell coordinates.
         * We need a transform from source pixel coordinates to target pixel coordinates (in images).
         * An offset may exist between cell coordinates and pixel coordinates.
         */
        final MathTransform pixelsToTransform = MathTransforms.translation(
                targetExtent.getLow(xDimension),
                targetExtent.getLow(yDimension));

        final MathTransform transformToPixels = MathTransforms.translation(
                Math.subtractExact(image.getMinX(), sourceExtent.getLow(xDimension)),
                Math.subtractExact(image.getMinY(), sourceExtent.getLow(yDimension)));

        final MathTransform toImage = MathTransforms.concatenate(pixelsToTransform, toSource, transformToPixels);
        /*
         * Get fill values from background values declared for each band, if any.
         * If no background value is declared, default is 0 for integer data or
         * NaN for floating point values.
         */
        final Number[] fillValues = new Number[ImageUtilities.getNumBands(image)];
        final List<SampleDimension> bands = getSampleDimensions();
        for (int i=Math.min(bands.size(), fillValues.length); --i >= 0;) {
            final SampleDimension band = bands.get(i);
            final Optional<Number> bg = band.getBackground();
            if (bg.isPresent()) {
                fillValues[i] = bg.get();
            }
        }
        return new ResampledImage(bounds, toImage, image, interpolation, fillValues);
    }
}
