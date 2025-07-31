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

import java.util.function.Function;
import java.awt.image.RenderedImage;
import java.awt.image.ImagingOpException;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.TransformSeparator;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.ImmutableEnvelope;
import org.apache.sis.referencing.privy.DirectPositionView;
import org.apache.sis.referencing.privy.ReferencingUtilities;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.privy.Numerics;
import org.apache.sis.util.resources.Errors;


/**
 * Builds a grid geometry for a slice in a {@link GridCoverage}. This is the implementation of
 * {@link GridGeometry#selectDimensions(int[])} and {@link ImageRenderer#getImageGeometry(int)} methods.
 *
 * <p>This class implements {@link Function} for allowing {@code apply(…)} to be invoked from outside this package.
 * That function is invoked (indirectly) by {@link org.apache.sis.image.privy.TiledImage#getProperty(String)}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class SliceGeometry implements Function<RenderedImage, GridGeometry> {
    /**
     * The coverage grid geometry from which to take a slice.
     */
    private final GridGeometry geometry;

    /**
     * Extents of the slice to take in the {@linkplain #geometry}.
     * May be {@code null} if unknown.
     */
    private final GridExtent sliceExtent;

    /**
     * Dimensions of the slice to retain. All dimensions not in this sequence will be discarded.
     * This is usually the array computed by {@link GridExtent#getSubspaceDimensions(int)}.
     */
    private final int[] gridDimensions;

    /**
     * Dimensions of the slices in the CRS space, or {@code null} if not yet computed.
     * This is computed as a side-product of {@link #reduce(GridExtent, int)} method call.
     * This is often the same dimensions as {@link #gridDimensions}, but not necessarily.
     *
     * @see #getTargetDimensions()
     */
    private int[] crsDimensions;

    /**
     * Factory to use for creating new transforms.
     */
    private final MathTransformFactory factory;

    /**
     * Creates a new builder of slice geometry.
     *
     * @param  geometry        the grid geometry for which the transform is desired.
     * @param  sliceExtent     the requested extent, or {@code null} for the whole coverage.
     * @param  gridDimensions  the grid (not CRS) dimensions to select, in strictly increasing order.
     * @param  factory         factory to use for creating new transforms, or {@code null} for default.
     */
    SliceGeometry(final GridGeometry geometry, final GridExtent sliceExtent,
                  final int[] gridDimensions, final MathTransformFactory factory)
    {
        this.geometry       = geometry;
        this.sliceExtent    = sliceExtent;
        this.gridDimensions = gridDimensions;
        this.factory        = ReferencingUtilities.nonNull(factory);
    }

    /**
     * Computes the {@value org.apache.sis.image.PlanarImage#GRID_GEOMETRY_KEY}
     * property value for the given image.
     *
     * @param  image  the image for which to compute the image geometry.
     * @throws ImagingOpException if the property cannot be computed.
     */
    @Override
    public GridGeometry apply(final RenderedImage image) {
        try {
            final GridExtent extent = new GridExtent(image.getMinX(), image.getMinY(), image.getWidth(), image.getHeight());
            return reduce(extent, GridCoverage2D.BIDIMENSIONAL);
        } catch (FactoryException e) {
            throw canNotCompute(e);
        }
    }

    /**
     * Creates a new grid geometry over the specified dimensions of the geometry specified at construction time.
     * The number of grid dimensions will be the length of the {@link #gridDimensions} array, and the number of
     * CRS dimensions will be reduced by the same amount.
     *
     * <p>If a non-null {@link #sliceExtent} has been specified, that extent shall be a sub-extent of the extent
     * of the original grid geometry. In particular it must have the same number of dimensions in same order and
     * the original "grid to CRS" transform shall be valid with that {@link #sliceExtent}. That sub-extent will
     * be used in replacement of the original extent for computing the geospatial area and the resolution.</p>
     *
     * <p>If a non-null {@code relativeExtent} is specified, a translation will be inserted before "grid to CRS"
     * conversion in order that lowest coordinate values of {@link #sliceExtent} (or original extent if there is
     * no slice extent) will map to (0,0,…,0) coordinate values in relative extent. This is used for taking in
     * account the translation between {@link #sliceExtent} coordinates and coordinates of the image returned by
     * {@link GridCoverage#render(GridExtent)}, in which case the relative extent is the location and size of the
     * {@link RenderedImage}. The number of dimensions of relative extent must be equal to {@code gridDimensions}
     * array length (i.e. the dimensionality reduction must be already done).</p>
     *
     * @param  relativeExtent  if non-null, an extent <em>relative</em> to {@link #sliceExtent} to assign to
     *                         the grid geometry to return. Dimensionality reduction shall be already applied.
     * @param  dimCRS          desired number of CRS dimensions, or -1 for automatic.
     * @throws FactoryException if an error occurred while separating the "grid to CRS" transform.
     *
     * @see GridGeometry#selectDimensions(int[])
     */
    final GridGeometry reduce(final GridExtent relativeExtent, final int dimCRS) throws FactoryException {
        GridExtent    extent      = geometry.extent;
        MathTransform gridToCRS   = geometry.gridToCRS;
        MathTransform cornerToCRS = geometry.cornerToCRS;
        double[]      resolution  = geometry.resolution;
        /*
         * If a `gridToCRS` transform is available, retain the source dimensions specified by `gridDimensions`.
         * We work on source dimensions because they are the grid dimensions. The CRS dimensions to retain are
         * often the same as the grid dimensions, but not necessarily. In particular the CRS may have more
         * elements if `TransformSeparator` detected that dropping a grid dimension does not force us to drop
         * the corresponding CRS dimension, for example because it has a constant value.
         */
        crsDimensions = gridDimensions;
        if (gridToCRS != null) {
            TransformSeparator sep = new TransformSeparator(gridToCRS, factory);
            sep.addSourceDimensions(gridDimensions);
            /*
             * Try to reduce the CRS by the same number of dimensions as the grid.
             */
            crsDimensions = findTargetDimensions(gridToCRS, extent, resolution, gridDimensions, dimCRS);
            if (crsDimensions != null) {
                sep.addTargetDimensions(crsDimensions);
            }
            gridToCRS     = sep.separate();
            crsDimensions = sep.getTargetDimensions();
            /*
             * We redo a separation for `cornerToCRS` instead of applying a translation of the `gridToCRS`
             * computed above because we don't know which of `gridToCRS` and `cornerToCRS` has less NaN values.
             * We require however the exact same sequence of target dimensions.
             */
            sep = new TransformSeparator(cornerToCRS, factory);
            sep.addSourceDimensions(gridDimensions);
            sep.addTargetDimensions(crsDimensions);
            cornerToCRS = sep.separate();
        }
        /*
         * Get an extent over only the specified grid dimensions. This code may opportunistically substitute
         * the full grid geometry extent by a sub-region. The use of a sub-region happens if this `reduce(…)`
         * method is invoked (indirectly) from a method like `GridGeometry.render(…)`.
         */
        final boolean useSubExtent = (sliceExtent != null) && !sliceExtent.equals(extent, ComparisonMode.IGNORE_METADATA);
        if (useSubExtent) {
            extent = sliceExtent;
        }
        if (extent != null) {
            extent = extent.selectDimensions(gridDimensions);
        }
        GeneralEnvelope subArea = null;
        if (useSubExtent && cornerToCRS != null) try {
            // `extent` is non-null if `useSubExtent` is true.
            subArea = extent.toEnvelope(cornerToCRS, false, gridToCRS, null);
        } catch (TransformException e) {
            // GridGeometry.reduce(…) is the public method invoking indirectly this method.
            GridGeometry.recoverableException("reduce", e);
        }
        /*
         * Create an envelope with only the requested dimensions, clipped to the sub-area if one has been
         * computed from `sliceExtent`.  The result after this code may still be a null envelope if there
         * is not enough information.
         */
        final int n = crsDimensions.length;
        ImmutableEnvelope envelope = geometry.envelope;
        if (envelope != null) {
            if (subArea != null || envelope.getDimension() != n) {
                final CoordinateReferenceSystem crs = CRS.selectDimensions(envelope.getCoordinateReferenceSystem(), crsDimensions);
                final double[] min = new double[n];
                final double[] max = new double[n];
                for (int i=0; i<n; i++) {
                    final int j = crsDimensions[i];
                    min[i] = envelope.getLower(j);
                    max[i] = envelope.getUpper(j);
                }
                if (subArea != null) {
                    for (int i=0; i<n; i++) {
                        double v;
                        if ((v = subArea.getLower(i)) > min[i]) min[i] = v;
                        if ((v = subArea.getUpper(i)) < max[i]) max[i] = v;
                    }
                }
                envelope = new ImmutableEnvelope(min, max, crs);
            }
        } else if (subArea != null) {
            envelope = new ImmutableEnvelope(subArea);
        }
        /*
         * If a `sliceExtent` has been specified, the resolution may differ because the "point of interest"
         * which is by default in extent center, may now be at a different location. In such case recompute
         * the resolution. Otherwise (same extent as original grid geometry), just copy resolution values
         * from the original grid geometry.
         */
        if (useSubExtent || resolution == null) {
            resolution = GridGeometry.resolution(gridToCRS, extent, PixelInCell.CELL_CENTER);
        } else if (resolution.length != n) {
            resolution = new double[n];
            for (int i=0; i<n; i++) {
                resolution[i] = geometry.resolution[crsDimensions[i]];
            }
        }
        /*
         * Coordinate (0,0) in `RenderedImage` corresponds to the lowest coordinates in `sliceExtent` request.
         * For taking that offset in account, we need to apply a translation. It happens when this method is
         * invoked (indirectly) from `GridCoverage.render(…)` but not when invoked from `GridGeometry.reduce(…)`
         */
        if (relativeExtent != null) {
            if (extent != null && !extent.startsAtZero()) {
                final double[] offset = new double[gridDimensions.length];
                for (int i=0; i<gridDimensions.length; i++) {
                    offset[i] = extent.getLow(gridDimensions[i]);
                }
                final LinearTransform translation = MathTransforms.translation(offset);
                if (gridToCRS != null) {
                    gridToCRS   = factory.createConcatenatedTransform(translation, gridToCRS);
                    cornerToCRS = factory.createConcatenatedTransform(translation, cornerToCRS);
                }
            }
            extent = relativeExtent;
        }
        /*
         * Slicing should not alter whether conversion in a dimension is a linear operation or not.
         * So we just copy the flags from the original grid geometry, selecting only the flags for
         * the specified dimensions.
         */
        long nonLinears = 0;
        for (int i=0; i<n; i++) {
            nonLinears |= ((geometry.nonLinears >>> crsDimensions[i]) & 1L) << i;
        }
        return new GridGeometry(extent, gridToCRS, cornerToCRS, envelope, resolution, nonLinears);
    }

    /**
     * Finds CRS (target) dimensions that are related to the given grid (source) dimensions.
     * This method returns an array where the number of CRS dimensions has been reduced by
     * the same amount as the reduction in number of grid dimensions.
     *
     * <p>If this method is not invoked, then {@link TransformSeparator} will retain as many target dimensions
     * as possible, which may be more than expected if a dimension that would normally be dropped is actually
     * a constant (all scale coefficients set to zero). This method tries to avoid this effect by forcing the
     * removal of CRS dimensions too. The CRS dimensions to remove are the ones that seem the less related to
     * the grid dimensions that we keep. This method is not provided in {@link TransformSeparator} because of
     * assumptions on the gridded nature of source coordinates.</p>
     *
     * <p>The algorithm used by this method (which is to compare the magnitude of scale coefficients anywhere
     * in the matrix) assumes that grid cells are "square", e.g. that a translation of 1 pixel to the left is
     * comparable in "real world" to a translation of 1 pixel to the bottom. This is often true but not always.
     * To compensate, we divide scale coefficients by the {@linkplain GridGeometry#resolution} for that CRS
     * dimension.</p>
     *
     * @param  gridToCRS       value of {@link GridGeometry#gridToCRS}  (may be {@code null}).
     * @param  extent          value of {@link GridGeometry#extent}     (may be {@code null}).
     * @param  resolution      value of {@link GridGeometry#resolution} (may be {@code null}).
     * @param  gridDimensions  the grid (source) dimensions to keep.
     * @param  dimCRS          desired number of CRS dimensions, or -1 for automatic.
     * @return the CRS (target) dimensions to keep, or {@code null} if this method cannot compute them.
     */
    private static int[] findTargetDimensions(final MathTransform gridToCRS, final GridExtent extent,
                                              final double[] resolution, int[] gridDimensions, int dimCRS)
    {
        /*
         * In most cases the transform is affine and we do not need a derivative computation
         * (which save us from requiring a point of interest).
         */
        int numRow = -1;        // The -1 is for later exclusion of the [0 0 0 … 1] row in affine transform.
        Matrix derivative = MathTransforms.getMatrix(gridToCRS);
        if (derivative == null) {
            if (extent != null) try {
                derivative = gridToCRS.derivative(new DirectPositionView.Double(extent.getPointOfInterest(PixelInCell.CELL_CENTER)));
            } catch (TransformException e) {
                // GridGeometry.reduce(…) is the public method invoking indirectly this method.
                GridGeometry.recoverableException("reduce", e);
                return null;
            } else {
                return null;
            }
            numRow = 0;         // Do not exclude any row in the matrix (cancel the -1 value set earlier).
        }
        numRow += derivative.getNumRow();
        if (dimCRS < 0) {
            dimCRS = gridDimensions.length;
        }
        /*
         * Search for the greatest scale coefficient. For the greatest value, take the row as the target
         * dimension and remember that we should not check anymore any value in the row and column where
         * the value has been found.
         */
        long selected = 0;
        while (Long.bitCount(selected) < dimCRS) {
            double max = -1;
            int   kmax = -1;
            int   jmax = -1;
            for (int j=0; j<numRow; j++) {
                if ((selected & Numerics.bitmask(j)) == 0) {
                    double r = 1;                               // For compensation of non-square cells.
                    if (resolution != null) {
                        final double t = resolution[j];
                        if (t > 0) r = t;                       // Exclude NaN values.
                    }
                    for (int k=0; k < gridDimensions.length; k++) {
                        final double e = Math.abs(derivative.getElement(j, gridDimensions[k])) / r;
                        if (e > max) {
                            max  = e;
                            kmax = k;
                            jmax = j;
                        }
                    }
                }
            }
            if ((kmax | jmax) < 0) {
                return null;                            // Cannot provide the requested number of dimensions.
            }
            if (max > 0) {                              // Dimensions are independent if scale factor is zero.
                if (jmax >= Long.SIZE) {
                    throw GridGeometry.excessiveDimension(gridToCRS);
                }
                selected |= (1L << jmax);
            }
            gridDimensions = ArraysExt.remove(gridDimensions, kmax, 1);
        }
        /*
         * Expand the values encoded in the `selected` bitmask.
         */
        final int[] crsDimensions = new int[dimCRS];
        for (int i=0; i<dimCRS; i++) {
            final int j = Long.numberOfTrailingZeros(selected);
            crsDimensions[i] = j;
            selected &= ~(1L << j);
        }
        return crsDimensions;
    }

    /**
     * Returns the dimensions in the slice in the CRS space.
     * This is a side-product of {@link #reduce(GridExtent, int)}.
     * Callers should not modify the returned array since it is not cloned.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final int[] getTargetDimensions() {
        return crsDimensions;
    }

    /**
     * Invoked if an error occurred while computing the {@link ImageRenderer#getImageGeometry(int)} value.
     * This exception should never occur actually, unless a custom factory implementation is used
     * (instead of the Apache SIS default) and there is a problem with that factory.
     */
    static ImagingOpException canNotCompute(final FactoryException e) {
        throw (ImagingOpException) new ImagingOpException(
                Errors.format(Errors.Keys.CanNotCompute_1, "ImageGeometry")).initCause(e);
    }
}
