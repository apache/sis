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
import java.util.Locale;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.util.FactoryException;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.TransformSeparator;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.CRS;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.internal.referencing.DirectPositionView;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.internal.raster.Resources;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Debug;
import org.apache.sis.util.collection.DefaultTreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable;

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
 *   <li>{@link #resize(GridExtent, double...)}, {@link #subgrid(Envelope, double...)} or {@link #subgrid(GridGeometry)}</li>
 *   <li>{@link #subsample(int...)} (if not set indirectly by above methods)</li>
 *   <li>{@link #slice(DirectPosition)} and/or {@link #sliceByRatio(double, int...)}</li>
 * </ol>
 *
 * Then the grid geometry is created by a call to {@link #build()}.
 * Alternatively, {@link #getIntersection()} can be invoked if only the {@link GridExtent} is desired
 * instead than the full {@link GridGeometry} and no subsampling is applied.
 *
 * <p>All methods in this class preserve the number of dimensions. For example the {@link #slice(DirectPosition)} method sets
 * the {@linkplain GridExtent#getSize(int) grid size} to 1 in all dimensions specified by the <cite>slice point</cite>,
 * but does not remove those dimensions from the grid geometry.
 * For dimensionality reduction, see {@link GridGeometry#reduce(int...)}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @see GridGeometry#derive()
 * @see GridGeometry#reduce(int...)
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
     * Controls behavior of rounding from floating point values to integers.
     *
     * @see #rounding(GridRoundingMode)
     */
    private GridRoundingMode rounding;

    /**
     * If non-null, the extent will be expanded by that amount of cells on each grid dimension.
     *
     * @see #margin(int...)
     */
    private int[] margin;

    // ──────── COMPUTED BY METHODS IN THIS CLASS ─────────────────────────────────────────────────────────────────────
    /**
     * The sub-extent of {@link #base} grid geometry to use for the new grid geometry. This is the intersection of
     * {@code base.extent} with any area of interest specified to a {@link #subgrid(Envelope, double...)} method,
     * potentially with some grid size set to 1 by a {@link #slice(DirectPosition)} method.
     * This extent is <strong>not</strong> scaled or subsampled for a given resolution.
     *
     * <p>This extent is initialized to {@code base.extent} if no slice, scale or sub-grid has been requested.
     * This field may be {@code null} if the base grid geometry does not define any extent.
     * A successful call to {@link GridGeometry#requireGridToCRS()} guarantees that this field is non-null.</p>
     *
     * @see #getIntersection()
     */
    private GridExtent baseExtent;

    /**
     * Same as {@link #baseExtent}, but takes resolution or subsampling in account.
     * This is {@code null} if no scale or subsampling has been applied.
     *
     * @todo if a {@linkplain #margin} has been specified, then we need to perform an additional clipping.
     */
    private GridExtent scaledExtent;

    /**
     * The conversion from the derived grid to the original grid, or {@code null} if no scale or subsampling is applied.
     * This is computed by {@link #resize(GridExtent, double...)} or {@link #subgrid(Envelope, double...)}.
     */
    private MathTransform toBase;

    /**
     * List of grid dimensions that are modified by the {@code cornerToCRS} transform, or null for all dimensions.
     * The length of this array is the number of dimensions of the given Area Of Interest (AOI). Each value in this
     * array is between 0 inclusive and {@code extent.getDimension()} exclusive. This is a temporary information
     * set by {@link #dropUnusedDimensions(MathTransform, int)} and cleared when no longer needed.
     */
    private int[] modifiedDimensions;

    /**
     * An estimation of the multiplication factors when converting cell coordinates from {@code gridOfInterest} to {@link #base}
     * grid. Those factors appear in the order of <em>base</em> grid axes. May be {@code null} if the conversion is identity.
     * This is sometime redundant with {@link #toBase} but not always.
     *
     * @see #getSubsamplings()
     */
    private double[] scales;

    /**
     * If {@link #subgrid(Envelope, double...)} or {@link #slice(DirectPosition)} has been invoked, the method name.
     * This is used for preventing those methods to be invoked twice or out-of-order, which is currently not supported.
     */
    private String subGridSetter;

    /**
     * Creates a new builder for deriving a grid geometry from the specified base.
     *
     * @param  base  the base to use as a template for deriving a new grid geometry.
     *
     * @see GridGeometry#derive()
     */
    protected GridDerivation(final GridGeometry base) {
        ArgumentChecks.ensureNonNull("base", base);
        this.base  = base;
        baseExtent = base.extent;                    // May be null.
        rounding   = GridRoundingMode.NEAREST;
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
     * @throws IllegalStateException if {@link #subgrid(Envelope, double...)} or {@link #slice(DirectPosition)}
     *         has already been invoked.
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
     * increased by {@code cellCount[i]}. The result is intersected with the extent of the {@link #base} grid geometry
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
     * @throws IllegalStateException if {@link #subgrid(Envelope, double...)} or {@link #slice(DirectPosition)}
     *         has already been invoked.
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
     * Requests a grid geometry where cell sizes have been scaled by the given factors, which result in a change of grid size.
     * The new grid geometry is given a <cite>"grid to CRS"</cite> transform computed as the concatenation of given scale factors
     * (applied on grid indices) followed by the {@linkplain GridGeometry#getGridToCRS(PixelInCell) grid to CRS} transform of the
     * grid geometry specified at construction time. The resulting grid extent can be specified explicitly (typically as an extent
     * computed by {@link GridExtent#resize(long...)}) or computed automatically by this method.
     *
     * <div class="note"><b>Example:</b>
     * if the original grid geometry had an extent of [0 … 5] in <var>x</var> and [0 … 8] in <var>y</var>, then a call to
     * {@code resize(null, 0.1, 0.1)} will build a grid geometry with an extent of [0 … 50] in <var>x</var> and [0 … 80] in <var>y</var>.
     * This new extent covers the same geographic area than the old extent but with pixels having a size of 0.1 times the old pixels size.
     * The <cite>grid to CRS</cite> transform of the new grid geometry will be pre-concatenated with scale factors of 0.1 in compensation
     * for the shrink in pixels size.</div>
     *
     * <p>Notes:</p>
     * <ul>
     *   <li>This method can be invoked only once.</li>
     *   <li>This method can not be used together with a {@code subgrid(…)} method.</li>
     *   <li>If a non-default rounding mode is desired, it should be {@linkplain #rounding(GridRoundingMode) specified}
     *       before to invoke this method.</li>
     *   <li>This method does not reduce the number of dimensions of the grid geometry.
     *       For dimensionality reduction, see {@link GridGeometry#reduce(int...)}.</li>
     * </ul>
     *
     * This method can be seen as a complement of {@link #subgrid(Envelope, double...)} working in grid coordinates space
     * instead of CRS coordinates space.
     *
     * @param  extent  the grid extent to set as a result of the given scale, or {@code null} for computing it automatically.
     *                 In non-null, then this given extent is used <i>as-is</i> without checking intersection with the base
     *                 grid geometry.
     * @param  scales  the scale factors to apply on grid indices. If the length of this array is smaller than the number of
     *                 grid dimension, then a scale of 1 is assumed for all missing dimensions.
     * @return {@code this} for method call chaining.
     * @throws IllegalStateException if a {@link #subgrid(GridGeometry) subgrid(…)} or {@link #slice(DirectPosition) slice(…)}
     *         method has already been invoked.
     *
     * @see GridExtent#resize(long...)
     */
    public GridDerivation resize(GridExtent extent, double... scales) {
        ArgumentChecks.ensureNonNull("scales", scales);
        ensureSubgridNotSet();
        subGridSetter = "resize";
        final int n = base.getDimension();
        if (extent != null) {
            final int actual = extent.getDimension();
            if (actual != n) {
                throw new IllegalArgumentException(Errors.format(
                        Errors.Keys.MismatchedDimension_3, "extent", n, actual));
            }
        }
        /*
         * Computes the affine transform to pre-concatenate with the 'gridToCRS' transform.
         * This is the simplest calculation done in this class since we are already in grid coordinates.
         * The given 'scales' array will become identical to 'this.scales' after length adjustment.
         */
        final int actual = scales.length;
        scales = Arrays.copyOf(scales, n);
        if (actual < n) {
            Arrays.fill(scales, actual, n, 1);
        }
        this.toBase = MathTransforms.scale(scales);
        this.scales = scales;                           // No clone needed since the array has been copied above.
        /*
         * If the user did not specified explicitly the resulting grid extent, compute it now.
         * This operation should never fail since we use known implementation of MathTransform,
         * unless some of the given scale factors were too close to zero.
         */
        if (extent == null && baseExtent != null) try {
            final MathTransform mt = toBase.inverse();
            final GeneralEnvelope indices = baseExtent.toCRS(mt, mt);
            extent = new GridExtent(indices, rounding, margin, null, null);
        } catch (TransformException e) {
            throw new IllegalArgumentException(e);
        }
        scaledExtent = extent;
        // Note: current version does not update 'baseExtent'.
        return this;
    }

    /**
     * Adapts the base grid for the geographic area and resolution of the given grid geometry.
     * After this method invocation, {@code GridDerivation} will hold information about conversion
     * from the given {@code gridOfInterest} to the {@link #base} grid geometry.
     * Those information include the {@link MathTransform}s converting cell coordinates
     * from the {@code gridOfInterest} to cell coordinates in the {@code base} grid,
     * together with the grid extent that results from this conversion.
     *
     * <div class="note"><b>Usage:</b>
     * This method can be helpful for implementation of
     * {@link org.apache.sis.storage.GridCoverageResource#read(GridGeometry, int...)}.
     * Example:
     *
     * {@preformat java
     *     class MyDataStorage extends GridCoverageResource {
     *         &#64;Override
     *         public GridCoverage read(GridGeometry domain, int... range) throws DataStoreException {
     *             GridDerivation change = getGridGeometry().derive().subgrid(domain);
     *             GridExtent toRead = change.buildExtent();
     *             int[] subsampling = change.getSubsamplings());
     *             // Do reading here.
     *         }
     *     }
     * }
     * </div>
     *
     * The following information are mandatory:
     * <ul>
     *   <li>{@linkplain GridGeometry#getExtent() Extent} in {@code gridOfInterest}.</li>
     *   <li>{@linkplain GridGeometry#getGridToCRS(PixelInCell) Grid to CRS} conversion in {@code gridOfInterest}.</li>
     *   <li>{@linkplain GridGeometry#getGridToCRS(PixelInCell) Grid to CRS} conversion in {@link #base} grid.</li>
     * </ul>
     *
     * The following information are optional but recommended:
     * <ul>
     *   <li>{@linkplain GridGeometry#getCoordinateReferenceSystem() Coordinate reference system} in {@code gridOfInterest}.</li>
     *   <li>{@linkplain GridGeometry#getCoordinateReferenceSystem() Coordinate reference system} in {@link #base} grid.</li>
     *   <li>{@linkplain GridGeometry#getExtent() Extent} in {@link #base} grid.</li>
     * </ul>
     *
     * An optional {@link #margin(int...) margin} can be specified for increasing the size of the grid extent computed by this method.
     * For example if the caller wants to apply bilinear interpolations in an image, (s)he will need 1 more pixel on each image border.
     * If the caller wants to apply bi-cubic interpolations, (s)he will need 2 more pixels on each image border.
     *
     * <p>Notes:</p>
     * <ul>
     *   <li>This method can be invoked only once.</li>
     *   <li>This method can not be used together with {@link #subgrid(Envelope, double...)} or {@link #resize(GridExtent, double...)}.</li>
     *   <li>If a non-default rounding mode is desired, it should be {@linkplain #rounding(GridRoundingMode) specified}
     *       before to invoke this method.</li>
     *   <li>This method does not reduce the number of dimensions of the grid geometry.
     *       For dimensionality reduction, see {@link GridGeometry#reduce(int...)}.</li>
     * </ul>
     *
     * @param  gridOfInterest  the area of interest and desired resolution as a grid geometry.
     * @return {@code this} for method call chaining.
     * @throws IncompleteGridGeometryException if a mandatory property of a grid geometry is absent.
     * @throws IllegalGridGeometryException if an error occurred while converting the envelope coordinates to grid coordinates.
     * @throws IllegalStateException if a {@link #subgrid(Envelope, double...) subgrid(…)} or {@link #slice(DirectPosition) slice(…)}
     *         method has already been invoked.
     *
     * @see #getSubsamplings()
     * @see #subsample(int...)
     */
    public GridDerivation subgrid(final GridGeometry gridOfInterest) {
        ArgumentChecks.ensureNonNull("gridOfInterest", gridOfInterest);
        ensureSubgridNotSet();
        subGridSetter = "subgrid";
        if (!base.equals(gridOfInterest)) {
            final MathTransform mapCorners, mapCenters;
            final GridExtent domain = gridOfInterest.getExtent();                  // May throw IncompleteGridGeometryException.
            try {
                final CoordinateOperation crsChange;
                crsChange  = Envelopes.findOperation(gridOfInterest.envelope, base.envelope);       // Any envelope may be null.
                mapCorners = path(gridOfInterest, crsChange, base, PixelInCell.CELL_CORNER);
                mapCenters = path(gridOfInterest, crsChange, base, PixelInCell.CELL_CENTER);
                clipExtent(domain.toCRS(mapCorners, mapCenters));
            } catch (FactoryException | TransformException e) {
                throw new IllegalGridGeometryException(e, "gridOfInterest");
            }
            if (baseExtent != base.extent && baseExtent.equals(gridOfInterest.extent)) {
                baseExtent = gridOfInterest.extent;                                                 // Share common instance.
            }
            scales = GridGeometry.resolution(mapCenters, domain);
        }
        return this;
    }

    /**
     * Returns the concatenation of all transformation steps from the given source to the given target.
     *
     * @param  source     the source grid geometry.
     * @param  crsChange  the change of coordinate reference system, or {@code null} if none.
     * @param  target     the target grid geometry.
     * @param  anchor     whether we want the transform for cell corner or cell center.
     */
    private static MathTransform path(final GridGeometry source, final CoordinateOperation crsChange,
            final GridGeometry target, final PixelInCell anchor) throws NoninvertibleTransformException
    {
        MathTransform step1 = source.getGridToCRS(anchor);
        MathTransform step2 = target.getGridToCRS(anchor);
        if (crsChange != null) {
            step1 = MathTransforms.concatenate(step1, crsChange.getMathTransform());
        }
        if (step1.equals(step2)) {                                          // Optimization for a common case.
            return MathTransforms.identity(step1.getSourceDimensions());
        } else {
            return MathTransforms.concatenate(step1, step2.inverse());
        }
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
     *   <li>This method can not be used together with {@link #subgrid(GridGeometry)} or {@link #resize(GridExtent, double...)}.</li>
     *   <li>If a non-default rounding mode is desired, it should be {@linkplain #rounding(GridRoundingMode) specified}
     *       before to invoke this method.</li>
     *   <li>This method does not reduce the number of dimensions of the grid geometry.
     *       For dimensionality reduction, see {@link GridGeometry#reduce(int...)}.</li>
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
     * @throws IllegalStateException if a {@link #subgrid(GridGeometry) subgrid(…)} or {@link #slice(DirectPosition) slice(…)}
     *         method has already been invoked.
     *
     * @see GridExtent#subsample(int[])
     */
    public GridDerivation subgrid(Envelope areaOfInterest, double... resolution) {
        ensureSubgridNotSet();
        MathTransform cornerToCRS = base.requireGridToCRS();
        subGridSetter = "subgrid";
        try {
            /*
             * If the envelope CRS is different than the expected CRS, concatenate the envelope transformation
             * to the 'gridToCRS' transform.  We should not transform the envelope here - only concatenate the
             * transforms - because transforming envelopes twice would add errors.
             */
            final CoordinateOperation baseToAOI = Envelopes.findOperation(base.envelope, areaOfInterest);
            if (baseToAOI != null) {
                cornerToCRS = MathTransforms.concatenate(cornerToCRS, baseToAOI.getMathTransform());
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
            dimension = baseExtent.getDimension();      // Non-null since 'base.requireGridToCRS()' succeed.
            GeneralEnvelope indices = null;
            if (areaOfInterest != null) {
                areaOfInterest = ReferencingUtilities.adjustWraparoundAxes(areaOfInterest, base.envelope, baseToAOI);
                indices = Envelopes.transform(cornerToCRS.inverse(), areaOfInterest);
                clipExtent(indices);
            }
            if (indices == null || indices.getDimension() != dimension) {
                indices = new GeneralEnvelope(dimension);
            }
            for (int i=0; i<dimension; i++) {
                indices.setRange(i, baseExtent.getLow(i), baseExtent.getHigh(i) + 1.0);
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
                final double[] subsampling = Matrices.inverse(m).multiply(resolution);
                final int[] modifiedDimensions = this.modifiedDimensions;                     // Will not change anymore.
                boolean modified = false;
                for (int k=0; k<subsampling.length; k++) {
                    double s = Math.abs(subsampling[k]);
                    if (s > 1) {                                // Also for skipping NaN values.
                        final int i = (modifiedDimensions != null) ? modifiedDimensions[k] : k;
                        final int accuracy = Math.max(0, Math.getExponent(indices.getSpan(i))) + 1;         // Power of 2.
                        s = Math.scalb(Math.rint(Math.scalb(s, accuracy)), -accuracy);
                        indices.setRange(i, indices.getLower(i) / s,
                                            indices.getUpper(i) / s);
                        modified = true;
                    }
                    subsampling[k] = s;
                }
                /*
                 * If at least one subsampling is effective, build a scale from the old grid coordinates to the new
                 * grid coordinates. If we had no rounding, the conversion would be only a scale. But because of rounding,
                 * we need a small translation for the difference between the "real" coordinate and the integer coordinate.
                 *
                 * TODO: need to clip to baseExtent, taking in account the difference in resolution.
                 */
                if (modified) {
                    scaledExtent = new GridExtent(indices, rounding, null, null, modifiedDimensions);
                    if (baseExtent.equals(scaledExtent)) scaledExtent = baseExtent;
                    m = Matrices.createIdentity(dimension + 1);
                    for (int k=0; k<subsampling.length; k++) {
                        final double s = subsampling[k];
                        if (s > 1) {                            // Also for skipping NaN values.
                            final int i = (modifiedDimensions != null) ? modifiedDimensions[k] : k;
                            m.setElement(i, i, s);
                            m.setElement(i, dimension, baseExtent.getLow(i) - scaledExtent.getLow(i) * s);
                        }
                    }
                    toBase = MathTransforms.linear(m);
                    scales = subsampling;                       // For information purpose only.
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
     * Returns the point of interest of current {@link #baseExtent}, keeping only the remaining
     * dimensions after {@link #dropUnusedDimensions(MathTransform, int)} execution.
     * The position is in units of {@link #base} grid coordinates.
     */
    private double[] getPointOfInterest() {
        final double[] pointOfInterest = baseExtent.getPointOfInterest();
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
     * Sets {@link #baseExtent} to the given envelope clipped to the previous extent.
     * This method shall be invoked for clipping only, without any subsampling applied.
     *
     * @param  indices  the envelope to intersect in units of {@link #base} grid coordinates.
     */
    private void clipExtent(final GeneralEnvelope indices) {
        final GridExtent sub = new GridExtent(indices, rounding, margin, baseExtent, modifiedDimensions);
        if (!sub.equals(baseExtent)) {
            baseExtent = sub;
        }
    }

    /**
     * Applies a subsampling on the grid geometry to build.
     * The {@code subsamplings} argument is often the array returned by {@link #getSubsamplings()}, but not necessarily.
     * The {@linkplain GridGeometry#getExtent() extent} of the {@linkplain #build() built} grid geometry will be derived
     * from {@link #getIntersection()} as below for each dimension <var>i</var>:
     *
     * <ul>
     *   <li>The {@linkplain GridExtent#getLow(int)  low}  is divided by {@code subsamplings[i]}, rounded toward zero.</li>
     *   <li>The {@linkplain GridExtent#getSize(int) size} is divided by {@code subsamplings[i]}, rounded toward zero.</li>
     *   <li>The {@linkplain GridExtent#getHigh(int) high} is recomputed from above low and size.</li>
     * </ul>
     *
     * The {@linkplain GridGeometry#getGridToCRS(PixelInCell) grid to CRS} transform is scaled accordingly
     * in order to map approximately to the same {@linkplain GridGeometry#getEnvelope() envelope}.
     *
     * @param  subsamplings  the subsampling to apply on each grid dimension. All values shall be greater than zero.
     *         If the array length is shorter than the number of dimensions, missing values are assumed to be 1.
     * @return {@code this} for method call chaining.
     * @throws IllegalStateException if a subsampling has already been set,
     *         for example by a call to {@link #subgrid(Envelope, double...) subgrid(…)}.
     *
     * @see #subgrid(GridGeometry)
     * @see #getSubsamplings()
     * @see GridExtent#subsample(int...)
     */
    public GridDerivation subsample(final int... subsamplings) {
        ArgumentChecks.ensureNonNull("subsamplings", subsamplings);
        if (toBase != null) {
            throw new IllegalStateException(Errors.format(Errors.Keys.ValueAlreadyDefined_1, "subsamplings"));
        }
        final GridExtent extent = (baseExtent != null) ? baseExtent : base.getExtent();
        Matrix affine = null;
        scales = null;
        if (subsamplings != null) {
            // Validity of the subsamplings values will be verified by GridExtent.subsample(…) invoked below.
            final int dimension = extent.getDimension();
            for (int i = Math.min(dimension, subsamplings.length); --i >= 0;) {
                final int s = subsamplings[i];
                if (s != 1) {
                    if (scales == null) {
                        scaledExtent = extent.subsample(subsamplings);
                        scales = new double[dimension];
                        Arrays.fill(scales, 1);
                        if (!scaledExtent.startsAtZero()) {
                            affine = Matrices.createIdentity(dimension + 1);
                        }
                    }
                    final double sd = s;
                    scales[i] = sd;
                    if (affine != null) {
                        affine.setElement(i, i, sd);
                        affine.setElement(i, dimension, extent.getLow(i) - scaledExtent.getLow(i) * sd);
                    }
                }
            }
        }
        if (affine != null) {
            toBase = MathTransforms.linear(affine);
        } else if (scales != null) {
            toBase = MathTransforms.scale(scales);
        }
        return this;
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
     *       For dimensionality reduction, see {@link GridGeometry#reduce(int...)}.</li>
     * </ul>
     *
     * @param  slicePoint   the coordinates where to get a slice.
     * @return {@code this} for method call chaining.
     * @throws IncompleteGridGeometryException if the base grid geometry has no extent or no "grid to CRS" transform.
     * @throws IllegalGridGeometryException if an error occurred while converting the point coordinates to grid coordinates.
     * @throws PointOutsideCoverageException if the given point is outside the grid extent.
     */
    public GridDerivation slice(final DirectPosition slicePoint) {
        ArgumentChecks.ensureNonNull("slicePoint", slicePoint);
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
            DirectPosition gridPoint = cornerToCRS.inverse().transform(slicePoint, null);
            if (scaledExtent != null) {
                scaledExtent = scaledExtent.slice(gridPoint, modifiedDimensions);
            }
            if (toBase != null) {
                gridPoint = toBase.transform(gridPoint, gridPoint);
            }
            baseExtent = baseExtent.slice(gridPoint, modifiedDimensions);   // Non-null check by 'base.requireGridToCRS()'.
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
     * @param  sliceRatio        the ratio to apply on all grid dimensions except the ones to keep.
     * @param  dimensionsToKeep  the grid dimension to keep unchanged.
     * @return {@code this} for method call chaining.
     * @throws IncompleteGridGeometryException if the base grid geometry has no extent.
     * @throws IndexOutOfBoundsException if a {@code dimensionsToKeep} value is out of bounds.
     */
    public GridDerivation sliceByRatio(final double sliceRatio, final int... dimensionsToKeep) {
        ArgumentChecks.ensureBetween("sliceRatio", 0, 1, sliceRatio);
        ArgumentChecks.ensureNonNull("dimensionsToKeep", dimensionsToKeep);
        subGridSetter = "sliceByRatio";
        final GridExtent extent = (baseExtent != null) ? baseExtent : base.getExtent();
        final GeneralDirectPosition slicePoint = new GeneralDirectPosition(extent.getDimension());
        baseExtent = extent.sliceByRatio(slicePoint, sliceRatio, dimensionsToKeep);
        if (scaledExtent != null) {
            scaledExtent = scaledExtent.sliceByRatio(slicePoint, sliceRatio, dimensionsToKeep);
        }
        return this;
    }

    /*
     * RATIONAL FOR NOT PROVIDING reduce(int... dimensions) METHOD HERE: that method would need to be the last method invoked,
     * otherwise it makes more complicated to implement other methods in this class.  Forcing users to invoke 'build()' before
     * (s)he can invoke GridGeometry.reduce(…) makes that clear and avoid the need for more flags in this GridDerivation class.
     * Furthermore declaring the 'reduce(…)' method in GridGeometry is more consistent with 'GridExtent.reduce(…)'.
     */

    /**
     * Builds a grid geometry with the configuration specified by the other methods in this {@code GridDerivation} class.
     *
     * @return the modified grid geometry. May be the {@link #base} grid geometry if no change apply.
     */
    public GridGeometry build() {
        /*
         * Assuming:
         *
         *   • All low coordinates = 0
         *   • h₁ the high coordinate before subsampling
         *   • h₂ the high coordinates after subsampling
         *   • c  a conversion factor from grid indices to "real world" coordinates
         *   • s  a subsampling ≧ 1
         *
         * Then the envelope upper bounds x is:
         *
         *   • x = (h₁ + 1) × c
         *   • x = (h₂ + f) × c⋅s      which implies       h₂ = h₁/s      and       f = 1/s
         *
         * If we modify the later equation for integer division instead than real numbers, we have:
         *
         *   • x = (h₂ + f) × c⋅s      where        h₂ = floor(h₁/s)      and       f = ((h₁ mod s) + 1)/s
         *
         * Because s ≧ 1, then f ≦ 1. But the f value actually used by GridExtent.toCRS(…) is hard-coded to 1
         * since it assumes that all cells are whole, i.e. it does not take in account that the last cell may
         * actually be fraction of a cell. Since 1 ≧ f, the computed envelope may be larger. This explains the
         * need for envelope clipping performed by GridGeometry constructor.
         */
        final GridExtent extent = (scaledExtent != null) ? scaledExtent : baseExtent;
        if (toBase != null || extent != base.extent) try {
            return new GridGeometry(base, extent, toBase);
        } catch (TransformException e) {
            throw new IllegalGridGeometryException(e, "envelope");
        }
        return base;
    }

    /**
     * Returns the extent of the modified grid geometry, ignoring subsamplings or changes in resolution.
     * This is the intersection of the {@link #base} grid geometry with the (grid or geospatial) envelope
     * given to a {@link #subgrid(Envelope, double...) subgrid(…)} method,
     * expanded by the {@linkplain #margin(int...) specified margin} (if any)
     * and potentially with some {@linkplain GridExtent#getSize(int) grid sizes} set to 1
     * if a {@link #slice(DirectPosition) slice(…)} method has been invoked.
     * The returned extent is in units of the {@link #base} grid cells, i.e.
     * {@linkplain #getSubsamplings() subsamplings} are ignored.
     *
     * @return intersection of grid geometry extents in units of {@link #base} grid cells.
     */
    public GridExtent getIntersection() {
        return (baseExtent != null) ? baseExtent : base.getExtent();
    }

    /**
     * Returns an <em>estimation</em> of the steps for accessing cells along each axis of base grid.
     * Given a conversion from {@code gridOfInterest} grid coordinates
     * (<var>x</var>, <var>y</var>, <var>z</var>) to {@link #base} grid coordinates
     * (<var>x′</var>, <var>y′</var>, <var>z′</var>) defined as below (generalize to as many dimensions as needed):
     *
     * <ul>
     *   <li><var>x′</var> = s₀⋅<var>x</var></li>
     *   <li><var>y′</var> = s₁⋅<var>y</var></li>
     *   <li><var>z′</var> = s₂⋅<var>z</var></li>
     * </ul>
     *
     * Then this method returns {|s₀|, |s₁|, |s₂|} rounded toward zero and clamped to 1
     * (i.e. all values in the returned array are strictly positive, no zero values).
     * It means that an iteration over {@code gridOfInterest} grid coordinates with a step Δ<var>x</var>=1
     * corresponds approximately to an iteration in {@link #base} grid coordinates with a step of Δ<var>x′</var>=s₀,
     * a step Δ<var>y</var>=1 corresponds approximately to a step Δ<var>y′</var>=s₁, <i>etc.</i>
     * If the conversion changes grid axis order, then the order of elements in the returned array
     * is the order of axes in the {@link #base} grid.
     *
     * @return an <em>estimation</em> of the steps for accessing cells along each axis of {@link #base} grid.
     *
     * @see #subgrid(GridGeometry)
     * @see #subgrid(Envelope, double...)
     * @see #subsample(int...)
     */
    public int[] getSubsamplings() {
        final int[] subsamplings;
        if (scales == null) {
            subsamplings = new int[getIntersection().getDimension()];
            Arrays.fill(subsamplings, 1);
        } else {
            subsamplings = new int[scales.length];
            for (int i=0; i<subsamplings.length; i++) {
                subsamplings[i] = Math.max(1, (int) Math.nextUp(scales[i]));    // Really want rounding toward 0.
            }
        }
        return subsamplings;
    }

    /**
     * Returns an <em>estimation</em> of the scale factor when converting sub-grid coordinates to {@link #base} grid coordinates.
     * This is for information purpose only since this method combines potentially different scale factors for all dimensions.
     *
     * @return an <em>estimation</em> of the scale factor for all dimensions.
     *
     * @see #subgrid(GridGeometry)
     * @see #subgrid(Envelope, double...)
     */
    public double getGlobalScale() {
        if (scales != null) {
            double sum = 0;
            int count = 0;
            for (final double value : scales) {
                if (Double.isFinite(value)) {
                    sum += value;
                    count++;
                }
            }
            if (count != 0) {
                return sum / count;
            }
        }
        return 1;
    }

    /**
     * Returns a tree representation of this {@code GridDerivation}.
     * The tree representation is for debugging purpose only and may change in any future SIS version.
     *
     * @param  locale  the locale to use for textual labels.
     * @return a tree representation of this {@code GridDerivation}.
     */
    @Debug
    private TreeTable toTree(final Locale locale) {
        final TableColumn<CharSequence> column = TableColumn.VALUE_AS_TEXT;
        final TreeTable tree = new DefaultTreeTable(column);
        final TreeTable.Node root = tree.getRoot();
        root.setValue(column, Classes.getShortClassName(this));
        final StringBuilder buffer = new StringBuilder(256);
        /*
         * GridDerivation (example)
         *   └─Intersection
         *       ├─Dimension 0: [ 2000 … 5475] (3476 cells)
         *       └─Dimension 1: [-1000 … 7999] (9000 cells)
         */
        if (baseExtent != null) {
            TreeTable.Node section = root.newChild();
            section.setValue(column, "Intersection");
            getIntersection().appendTo(buffer, Vocabulary.getResources(locale));
            for (final CharSequence line : CharSequences.splitOnEOL(buffer)) {
                String text = line.toString().trim();
                if (!text.isEmpty()) {
                    section.newChild().setValue(column, text);
                }
            }
        }
        /*
         * GridDerivation (example)
         *   └─Subsamplings
         *       ├─{50, 300}
         *       └─Global ≈ 175.0
         */
        if (scales != null) {
            buffer.setLength(0);
            buffer.append('{');
            for (int s : getSubsamplings()) {
                if (buffer.length() > 1) buffer.append(", ");
                buffer.append(s);
            }
            TreeTable.Node section = root.newChild();
            section.setValue(column, "Subsamplings");
            section.newChild().setValue(column, buffer.append('}').toString()); buffer.setLength(0);
            section.newChild().setValue(column, buffer.append("Global ≈ ").append((float) getGlobalScale()).toString());
        }
        return tree;
    }

    /**
     * Returns a string representation of this {@code GridDerivation} for debugging purpose.
     * The returned string is implementation dependent and may change in any future version.
     *
     * @return a string representation of this {@code GridDerivation} for debugging purpose.
     */
    @Override
    public String toString() {
        return toTree(null).toString();
    }
}
