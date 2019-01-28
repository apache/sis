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

import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.TransformSeparator;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.CRS;
import org.apache.sis.internal.referencing.DirectPositionView;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.internal.raster.Resources;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;

// Branch-dependent imports
import org.opengis.coverage.PointOutsideCoverageException;


/**
 * Creates a new grid geometry derived from a base grid geometry with different extent or resolution.
 * {@code GridDerivation} are created by calls to {@link GridGeometry#derive()}.
 * Properties of the desired grid geometry can be specified by calls to the following methods,
 * in that order (each method is optional):
 *
 * <ol>
 *   <li>{@link #rounding(GridRoundingMode)} and/or {@link #margin(int...)} in any order</li>
 *   <li>{@link #subgrid(Envelope, double...)}</li>
 *   <li>{@link #slice(DirectPosition)}</li>
 *   <li>{@link #sliceByRatio(double, int...)}</li>
 *   <li>{@link #reduce(int...)}</li>
 * </ol>
 *
 * Then the grid geometry is created by a call to {@link #build()}.
 * Alternatively, {@link #extent()} can be invoked if only the {@link GridExtent} is desired
 * instead than the full {@link GridGeometry}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @see GridGeometry#derive()
 *
 * @since 1.0
 * @module
 */
public class GridDerivation {
    /**
     * The base grid geometry from which to derive a new grid geometry.
     */
    protected final GridGeometry base;

    /**
     * If {@link #subgrid(Envelope, double...)},  {@link #slice(DirectPosition)} or {@link #reduce(int...)} has been invoked,
     * the method name. This is used for preventing those methods to be invoked twice or out-of-order, which is currently not
     * supported.
     */
    private String subGridSetter;

    /**
     * The sub-extent computed by a {@link #slice(DirectPosition)} or {@link #subgrid(Envelope, double...)} methods,
     * or {@code base.extent} if no slice or sub-grid has been requested. This field may be {@code null} if the base
     * grid geometry does not define any extent. A successful call to {@link GridGeometry#requireGridToCRS()} guarantees
     * that this field is non-null.
     */
    private GridExtent subExtent;

    /**
     * The conversion from the subsampled grid to the original grid, or {@code null} if no subsampling is applied.
     * This is computed by {@link #subgrid(Envelope, double...)}.
     */
    private MathTransform toBase;

    /**
     * List of grid dimensions that are modified by the {@code cornerToCRS} transform, or null for all dimensions.
     * The length of this array is the number of dimensions of the given Area Of Interest (AOI). Each value in this
     * array is between 0 inclusive and {@code extent.getDimension()} exclusive.
     */
    private int[] modifiedDimensions;

    /**
     * The grid dimension to keep, or {@code null} if no filtering is applied.
     * Those values are set by {@link #reduce(int...)}.
     */
    private int[] selectedDimensions;

    /**
     * If non-null, the extent will be expanded by that amount of cells on each grid dimension.
     *
     * @see #margin(int...)
     */
    private int[] margin;

    /**
     * Controls behavior of rounding from floating point values to integers.
     */
    private GridRoundingMode rounding;

    /**
     * Creates a new builder for deriving a grid geometry from the specified base.
     *
     * @param  base  the base to use as a template for deriving a new grid geometry.
     *
     * @see GridGeometry#derive()
     */
    protected GridDerivation(final GridGeometry base) {
        ArgumentChecks.ensureNonNull("base", base);
        this.base = base;
        subExtent = base.extent;                    // May be null.
        rounding  = GridRoundingMode.NEAREST;
    }

    /**
     * Verifies that a sub-grid has not yet been defined.
     * This method is invoked for enforcing the method call order defined in javadoc.
     */
    private void ensureSubgridNotSet() {
        if (subGridSetter != null) {
            throw new IllegalStateException(Resources.format(Resources.Keys.CanNotSetDerivedGridProperty_1, subGridSetter));
        }
    }

    /**
     * Verifies that {@link #reduce(int...)} has not yet been invoked.
     * This method is invoked for enforcing the method call order defined in javadoc.
     */
    private void ensureReduceNotSet() {
        if (selectedDimensions != null) {
            throw new IllegalStateException(Resources.format(Resources.Keys.CanNotSetDerivedGridProperty_1, "reduce"));
        }
    }

    /**
     * Controls behavior of rounding from floating point values to integers.
     * This setting modifies computations performed by the following methods
     * (it has no effect on other methods in this {@code GridDerivation} class):
     * <ul>
     *   <li>{@link #slice(DirectPosition)}</li>
     *   <li>{@link #subgrid(Envelope, double...)}</li>
     * </ul>
     *
     * If this method is never invoked, the default value is {@link GridRoundingMode#NEAREST}.
     * If this method is invoked too late, an {@link IllegalStateException} is thrown.
     *
     * @param  mode  the new rounding mode.
     * @return {@code this} for method call chaining.
     * @throws IllegalStateException if {@link #slice(DirectPosition)}, {@link #subgrid(Envelope, double...)} or
     *         {@link #reduce(int...)} have already been invoked.
     */
    public GridDerivation rounding(final GridRoundingMode mode) {
        ArgumentChecks.ensureNonNull("mode", mode);
        ensureSubgridNotSet();
        rounding = mode;
        return this;
    }

    /**
     * Specifies an amount of cells by which to expand {@code GridExtent} after rounding.
     * This setting modifies computations performed by the following methods
     * (it has no effect on other methods in this {@code GridDerivation} class):
     * <ul>
     *   <li>{@link #subgrid(Envelope, double...)}</li>
     * </ul>
     *
     * For each dimension <var>i</var> of the grid computed by above methods, the {@linkplain GridExtent#getLow(int) low} grid
     * coordinate is subtracted by {@code cellCount[i]} and the {@linkplain GridExtent#getHigh(int) high} grid coordinate is
     * increased by {@code cellCount[i]}. The result is intersected with the extent of the {@linkplain #base} grid geometry
     * given to the constructor.
     *
     * <div class="note"><b>Use case:</b>
     * if the caller wants to apply bilinear interpolations in an image, (s)he will need 1 more pixel on each image border.
     * If the caller wants to apply bi-cubic interpolations, (s)he will need 2 more pixels on each image border.</div>
     *
     * If this method is never invoked, the default value is zero for all dimensions.
     * If this method is invoked too late, an {@link IllegalStateException} is thrown.
     * If the {@code count} array length is shorter than the grid dimension,
     * then zero is assumed for all missing dimensions.
     *
     * @param  cellCounts  number of cells by which to expand the grid extent.
     * @return {@code this} for method call chaining.
     * @throws IllegalArgumentException if a value is negative.
     * @throws IllegalStateException if {@link #slice(DirectPosition)}, {@link #subgrid(Envelope, double...)} or
     *         {@link #reduce(int...)} have already been invoked.
     */
    public GridDerivation margin(final int... cellCounts) {
        ArgumentChecks.ensureNonNull("cellCounts", cellCounts);
        ensureSubgridNotSet();
        int[] margin = null;
        for (int i=cellCounts.length; --i >= 0;) {
            final int n = cellCounts[i];
            ArgumentChecks.ensurePositive("cellCounts", n);
            if (margin == null) {
                margin = new int[i+1];
            }
            margin[i] = n;
        }
        this.margin = margin;           // Set only on success.
        return this;
    }

    /**
     * Requests a grid geometry over a sub-region of the base grid geometry and optionally with subsampling.
     * The given envelope does not need to be expressed in the same coordinate reference system (CRS)
     * than {@linkplain GridGeometry#getCoordinateReferenceSystem() the CRS of the base grid geometry};
     * coordinate conversions or transformations will be applied as needed.
     * That envelope CRS may have fewer dimensions than the base grid geometry CRS,
     * in which case grid dimensions not mapped to envelope dimensions will be returned unchanged.
     * The target resolution, if provided, shall be in same units and same order than the given envelope axes.
     * If the length of {@code resolution} array is less than the number of dimensions of {@code areaOfInterest},
     * then no subsampling will be applied on the missing dimensions.
     *
     * <p>Notes:</p>
     * <ul>
     *   <li>This method can be invoked only once.</li>
     *   <li>This method can not be used together with {@link #slice(DirectPosition)}.</li>
     *   <li>If a non-default rounding mode is desired, it should be {@linkplain #rounding(GridRoundingMode) specified}
     *       before to invoke this method.</li>
     *   <li>This method does not reduce the number of dimensions of the grid geometry.
     *       For dimensionality reduction, see {@link #reduce(int...)}.</li>
     * </ul>
     *
     * @param  areaOfInterest  the desired spatiotemporal region in any CRS (transformations will be applied as needed),
     *                         or {@code null} for not restricting the sub-grid to a sub-area.
     * @param  resolution      the desired resolution in the same units and order than the axes of the given envelope,
     *                         or {@code null} or an empty array if no subsampling is desired. The array length should
     *                         be equal to the {@code areaOfInterest} dimension, but this is not mandatory
     *                         (zero or missing values mean no sub-sampling, extraneous values are ignored).
     * @return {@code this} for method call chaining.
     * @throws IncompleteGridGeometryException if the base grid geometry has no extent or no "grid to CRS" transform.
     * @throws IllegalGridGeometryException if an error occurred while converting the envelope coordinates to grid coordinates.
     * @throws IllegalStateException if {@code subgrid(Envelope, double...)}, {@link #slice(DirectPosition)} or
     *         {@link #reduce(int...)} have already been invoked.
     *
     * @see GridExtent#subsample(int[])
     */
    public GridDerivation subgrid(final Envelope areaOfInterest, double... resolution) {
        ensureSubgridNotSet();
        MathTransform cornerToCRS = base.requireGridToCRS();
        subGridSetter = "subgrid";
        try {
            /*
             * If the envelope CRS is different than the expected CRS, concatenate the envelope transformation
             * to the 'gridToCRS' transform.  We should not transform the envelope here - only concatenate the
             * transforms - because transforming envelopes twice would add errors.
             */
            final CoordinateOperation operation = Envelopes.findOperation(base.envelope, areaOfInterest);
            if (operation != null) {
                cornerToCRS = MathTransforms.concatenate(cornerToCRS, operation.getMathTransform());
            }
            /*
             * If the envelope dimensions does not encompass all grid dimensions, the envelope is probably non-invertible.
             * We need to reduce the number of grid dimensions in the transform for having a one-to-one relationship.
             */
            int dimension = cornerToCRS.getTargetDimensions();
            ArgumentChecks.ensureDimensionMatches("areaOfInterest", dimension, areaOfInterest);
            cornerToCRS = dropUnusedDimensions(cornerToCRS, dimension);
            /*
             * Compute the sub-extent for the given Area Of Interest (AOI), ignoring for now the subsampling.
             * If no area of interest has been specified, or if the result is identical to the original extent,
             * then we will keep the reference to the original GridExtent (i.e. we share existing instances).
             */
            dimension = subExtent.getDimension();
            GeneralEnvelope indices = null;
            if (areaOfInterest != null) {
                indices = Envelopes.transform(cornerToCRS.inverse(), areaOfInterest);
                setSubExtent(indices, subExtent);
            }
            if (indices == null || indices.getDimension() != dimension) {
                indices = new GeneralEnvelope(dimension);
            }
            for (int i=0; i<dimension; i++) {
                indices.setRange(i, subExtent.getLow(i), subExtent.getHigh(i) + 1.0);
            }
            /*
             * Convert the target resolutions to grid cell subsamplings and adjust the extent consequently.
             * We perform this conversion by handling the resolutions as a small translation vector located
             * at the point of interest, and converting it to a translation vector in grid coordinates. The
             * conversion is done by a multiplication with the "CRS to grid" derivative at that point.
             *
             * The subsampling will be rounded in such a way that the difference in grid size is less than
             * one half of cell. Demonstration:
             *
             *    e = Math.getExponent(span)     →    2^e ≦ span
             *    a = e+1                        →    2^a > span     →    1/2^a < 1/span
             *   Δs = (s - round(s)) / 2^a
             *   (s - round(s)) ≦ 0.5            →    Δs  ≦  0.5/2^a  <  0.5/span
             *   Δs < 0.5/span                   →    Δs⋅span < 0.5 cell.
             */
            if (resolution != null && resolution.length != 0) {
                resolution = ArraysExt.resize(resolution, cornerToCRS.getTargetDimensions());
                Matrix m = cornerToCRS.derivative(new DirectPositionView.Double(getPointOfInterest()));
                resolution = Matrices.inverse(m).multiply(resolution);
                final int[] modifiedDimensions = this.modifiedDimensions;                     // Will not change anymore.
                boolean modified = false;
                for (int k=0; k<resolution.length; k++) {
                    double s = Math.abs(resolution[k]);
                    if (s > 1) {                                // Also for skipping NaN values.
                        final int i = (modifiedDimensions != null) ? modifiedDimensions[k] : k;
                        final int accuracy = Math.max(0, Math.getExponent(indices.getSpan(i))) + 1;         // Power of 2.
                        s = Math.scalb(Math.rint(Math.scalb(s, accuracy)), -accuracy);
                        indices.setRange(i, indices.getLower(i) / s,
                                            indices.getUpper(i) / s);
                        modified = true;
                    }
                    resolution[k] = s;
                }
                /*
                 * If at least one subsampling is effective, build a scale from the old grid coordinates to the new
                 * grid coordinates. If we had no rounding, the conversion would be only a scale. But because of rounding,
                 * we need a small translation for the difference between the "real" coordinate and the integer coordinate.
                 *
                 * TODO: need to clip to base.extent, taking in account the difference in resolution.
                 */
                if (modified) {
                    final GridExtent unscaled = subExtent;
                    setSubExtent(indices, null);
                    m = Matrices.createIdentity(dimension + 1);
                    for (int k=0; k<resolution.length; k++) {
                        final double s = resolution[k];
                        if (s > 1) {                            // Also for skipping NaN values.
                            final int i = (modifiedDimensions != null) ? modifiedDimensions[k] : k;
                            m.setElement(i, i, s);
                            m.setElement(i, dimension, unscaled.getLow(i) - subExtent.getLow(i) * s);
                        }
                    }
                    toBase = MathTransforms.linear(m);
                }
            }
        } catch (FactoryException | TransformException e) {
            throw new IllegalGridGeometryException(e, "areaOfInterest");
        }
        modifiedDimensions = null;                  // Not needed anymore.
        return this;
    }

    /**
     * Drops the source dimensions that are not needed for producing the target dimensions.
     * The retained source dimensions are stored in {@link #modifiedDimensions}.
     * This method is invoked in an effort to make the transform invertible.
     *
     * @param  cornerToCRS  transform from grid coordinates to AOI coordinates.
     * @param  dimension    value of {@code cornerToCRS.getTargetDimensions()}.
     */
    private MathTransform dropUnusedDimensions(MathTransform cornerToCRS, final int dimension)
            throws FactoryException, TransformException
    {
        if (dimension < cornerToCRS.getSourceDimensions()) {
            final TransformSeparator sep = new TransformSeparator(cornerToCRS);
            sep.setTrimSourceDimensions(true);
            cornerToCRS = sep.separate();
            modifiedDimensions = sep.getSourceDimensions();
            if (modifiedDimensions.length != dimension) {
                throw new TransformException(Resources.format(Resources.Keys.CanNotMapToGridDimensions));
            }
        }
        return cornerToCRS;
    }

    /**
     * Returns the point of interest of current {@link #subExtent}, keeping only the remaining
     * dimensions after {@link #dropUnusedDimensions(MathTransform, int)} execution.
     */
    private double[] getPointOfInterest() {
        final double[] pointOfInterest = subExtent.getPointOfInterest();
        if (modifiedDimensions == null) {
            return pointOfInterest;
        }
        final double[] filtered = new double[modifiedDimensions.length];
        for (int i=0; i<filtered.length; i++) {
            filtered[i] = pointOfInterest[modifiedDimensions[i]];
        }
        return filtered;
    }

    /**
     * Sets {@link #subExtent} to the given envelope.
     *
     * @param  indices    the envelope to use for setting the grid extent.
     * @param  enclosing  the enclosing grid extent if a subsampling is not yet applied, {@code null} otherwise.
     */
    private void setSubExtent(final GeneralEnvelope indices, final GridExtent enclosing) {
        final GridExtent sub = new GridExtent(indices, rounding, margin, enclosing, modifiedDimensions);
        if (!sub.equals(subExtent)) {
            subExtent = sub;
        }
    }

    /**
     * Requests a grid geometry for a slice at the given "real world" position.
     * The given position can be expressed in any coordinate reference system (CRS).
     * The position should not define a coordinate for all dimensions, otherwise the slice would degenerate
     * to a single point. Dimensions can be left unspecified either by assigning to {@code slicePoint} a CRS
     * without those dimensions, or by assigning the NaN value to some coordinates.
     *
     * <div class="note"><b>Example:</b>
     * if the {@linkplain GridGeometry#getCoordinateReferenceSystem() coordinate reference system} of base grid geometry has
     * (<var>longitude</var>, <var>latitude</var>, <var>time</var>) axes, then a (<var>longitude</var>, <var>latitude</var>)
     * slice at time <var>t</var> can be created with one of the following two positions:
     * <ul>
     *   <li>A three-dimensional position with ({@link Double#NaN}, {@link Double#NaN}, <var>t</var>) coordinates.</li>
     *   <li>A one-dimensional position with (<var>t</var>) coordinate and the coordinate reference system set to
     *       {@linkplain org.apache.sis.referencing.CRS#getTemporalComponent(CoordinateReferenceSystem) the temporal component}
     *       of the grid geometry CRS.</li>
     * </ul></div>
     *
     * <p>Notes:</p>
     * <ul>
     *   <li>This method can be invoked after {@link #subgrid(Envelope, double...)}, but not before.</li>
     *   <li>If a non-default rounding mode is desired, it should be {@linkplain #rounding(GridRoundingMode) specified}
     *       before to invoke this method.</li>
     *   <li>This method does not reduce the number of dimensions of the grid geometry.
     *       For dimensionality reduction, see {@link #reduce(int...)}.</li>
     * </ul>
     *
     * @param  slicePoint   the coordinates where to get a slice.
     * @return {@code this} for method call chaining.
     * @throws IncompleteGridGeometryException if the base grid geometry has no extent or no "grid to CRS" transform.
     * @throws IllegalGridGeometryException if an error occurred while converting the point coordinates to grid coordinates.
     * @throws PointOutsideCoverageException if the given point is outside the grid extent.
     * @throws IllegalStateException if {@link #reduce(int...)} has already been invoked.
     */
    public GridDerivation slice(final DirectPosition slicePoint) {
        ArgumentChecks.ensureNonNull("slicePoint", slicePoint);
        ensureReduceNotSet();
        MathTransform cornerToCRS = base.requireGridToCRS();
        subGridSetter = "slice";
        try {
            if (toBase != null) {
                cornerToCRS = MathTransforms.concatenate(toBase, cornerToCRS);
            }
            if (base.envelope != null) {
                final CoordinateReferenceSystem sourceCRS = base.envelope.getCoordinateReferenceSystem();
                if (sourceCRS != null) {
                    final CoordinateReferenceSystem targetCRS = slicePoint.getCoordinateReferenceSystem();
                    if (targetCRS != null) {
                        final CoordinateOperation operation = CRS.findOperation(sourceCRS, targetCRS, null);
                        cornerToCRS = MathTransforms.concatenate(cornerToCRS, operation.getMathTransform());
                    }
                }
            }
            final int dimension = cornerToCRS.getTargetDimensions();
            ArgumentChecks.ensureDimensionMatches("slicePoint", dimension, slicePoint);
            cornerToCRS = dropUnusedDimensions(cornerToCRS, dimension);
            subExtent = subExtent.slice(cornerToCRS.inverse().transform(slicePoint, null), modifiedDimensions);
        } catch (FactoryException e) {
            throw new IllegalGridGeometryException(Resources.format(Resources.Keys.CanNotMapToGridDimensions), e);
        } catch (TransformException e) {
            throw new IllegalGridGeometryException(e, "slicePoint");
        }
        modifiedDimensions = null;              // Not needed anymore.
        return this;
    }

    /**
     * Requests a grid geometry for a slice at the given relative position.
     * The relative position is specified by a ratio between 0 and 1 where 0 maps to {@linkplain GridExtent#getLow(int) low}
     * grid coordinates, 1 maps to {@linkplain GridExtent#getHigh(int) high grid coordinates} and 0.5 maps the median point.
     * The slicing is applied on all dimensions except the specified dimensions to keep.
     *
     * @param  sliceRatio        the ration to apply on all grid dimensions except the ones to keep.
     * @param  dimensionsToKeep  the grid dimension to keep unchanged.
     * @return {@code this} for method call chaining.
     * @throws IncompleteGridGeometryException if the base grid geometry has no extent.
     * @throws IndexOutOfBoundsException if a {@code dimensionsToKeep} value is out of bounds.
     * @throws IllegalStateException if {@link #reduce(int...)} has already been invoked.
     */
    public GridDerivation sliceByRatio(final double sliceRatio, final int... dimensionsToKeep) {
        ArgumentChecks.ensureBetween("sliceRatio", 0, 1, sliceRatio);
        ArgumentChecks.ensureNonNull("dimensionsToKeep", dimensionsToKeep);
        ensureReduceNotSet();
        final GridExtent extent = (subExtent != null) ? subExtent : base.getExtent();
        final GeneralDirectPosition slicePoint = new GeneralDirectPosition(extent.getDimension());
        for (int i=0; i<slicePoint.ordinates.length; i++) {
            slicePoint.ordinates[i] = sliceRatio * (extent.getSize(i) - 1) + extent.getLow(i);
        }
        for (int i=0; i<dimensionsToKeep.length; i++) {
            slicePoint.ordinates[dimensionsToKeep[i]] = Double.NaN;
        }
        subExtent = extent.slice(slicePoint, null);
        return this;
    }

    /**
     * Requests a grid geometry that encompass only some dimensions of the grid extent.
     * The specified dimensions will be copied into a new grid geometry.
     * The selection is applied on {@linkplain GridGeometry#getExtent() grid extent} dimensions;
     * they are not necessarily the same than the {@linkplain GridGeometry#getEnvelope() envelope} dimensions.
     * The given dimensions must be in strictly ascending order without duplicated values.
     * The number of dimensions of the sub grid geometry will be {@code dimensions.length}.
     *
     * <p>This method performs a <cite>dimensionality reduction</cite>.
     * This method can not be used for changing dimension order.</p>
     *
     * @param  dimensions  the grid (not CRS) dimensions to select, in strictly increasing order.
     * @return {@code this} for method call chaining.
     * @throws IndexOutOfBoundsException if an index is out of bounds.
     *
     * @see GridExtent#getSubspaceDimensions(int)
     * @see GridExtent#reduce(int...)
     * @see org.apache.sis.referencing.CRS#reduce(CoordinateReferenceSystem, int...)
     */
    public GridDerivation reduce(final int... dimensions) {
        ensureReduceNotSet();
        subGridSetter = "reduce";
        selectedDimensions = GridExtent.verifyDimensions(dimensions, base.getDimension());
        return this;
    }

    /**
     * Returns the extent of the modified grid geometry. This method is more efficient than
     * {@link #build()} if only the grid extent is desired instead than the full grid geometry.
     *
     * @return the modified grid geometry extent.
     */
    public GridExtent extent() {
        GridExtent extent = (subExtent != null) ? subExtent : base.getExtent();
        if (selectedDimensions != null) {
            extent = extent.reduce(selectedDimensions);
        }
        return extent;
    }

    /**
     * Builds a grid geometry with the configuration specified by the other methods in this {@code GridDerivation} class.
     *
     * @return the modified grid geometry. May be the {@linkplain #base} grid geometry if no change apply.
     */
    public GridGeometry build() {
        GridGeometry grid = base;
        String cause = null;
        try {
            if (toBase != null || subExtent != base.extent) {
                cause = "subgrid";
                grid = new GridGeometry(grid, subExtent, toBase);
            }
            if (selectedDimensions != null) {
                cause = "dimensions";
                grid = new GridGeometry(grid, selectedDimensions);
            }
        } catch (FactoryException | TransformException e) {
            throw new IllegalGridGeometryException(e, cause);
        }
        return grid;
    }
}
