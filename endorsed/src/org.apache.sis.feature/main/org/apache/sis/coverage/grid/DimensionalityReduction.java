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

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.io.Serializable;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArgumentCheckByAssertion;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.geometry.ImmutableEnvelope;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.coverage.SubspaceNotSpecifiedException;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.TransformSeparator;
import org.apache.sis.referencing.operation.transform.PassThroughTransform;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.coordinate.MismatchedDimensionException;
import org.opengis.coverage.PointOutsideCoverageException;


/**
 * Description about how to reduce the number of dimensions of the domain of a grid coverage.
 * This is a reduction in the number of dimensions of the grid extent, which usually implies
 * a reduction in the number of dimensions of the CRS but not necessarily at the same indices
 * (the relationship between grid dimensions and CRS dimensions is not necessarily straightforward).
 * The sample dimensions (coverage range) are unmodified.
 *
 * <p>{@code DimensionalityReduction} specifies which dimensions to keep, and which grid
 * values to use for the omitted dimensions. This information allows the conversion from
 * a source {@link GridGeometry} to a reduced grid geometry, and conversely.</p>
 *
 * <p>Instances of {@code DimensionalityReduction} are immutable and thread-safe.</p>
 *
 * <h2>Assumptions</h2>
 * The current implementation assumes that removing <var>n</var> dimensions in the grid extent
 * causes the removal of exactly <var>n</var> dimensions in the Coordinate Reference System.
 * However, the removed dimensions do not need to be at the same indices or in same order.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.4
 */
public class DimensionalityReduction implements UnaryOperator<GridCoverage>, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -6462887684250336261L;

    /**
     * The source grid geometry with all dimensions.
     *
     * @see #getSourceGridGeometry()
     */
    private final GridGeometry sourceGeometry;

    /**
     * The reduced grid geometry.
     * The number of dimensions shall be the number of bits set in the {@link #gridAxesToPass} bitmask.
     *
     * @see #getReducedGridGeometry()
     */
    private final GridGeometry reducedGeometry;

    /**
     * Indices of source grid dimensions to keep in the reduced grid.
     * This is the parameter of the "pass-through" coordinate operation.
     * Values must be in strictly increasing order.
     *
     * @see #getSelectedDimensions()
     */
    private final int[] gridAxesToPass;

    /**
     * Indices of target dimensions that have been removed.
     * Values must be in strictly increasing order.
     */
    private final int[] crsAxesToRemove;

    /**
     * Partially filled array of CRS components to use for building a compound CRS.
     * Elements in this array are either instances of {@link CoordinateReferenceSystem} or {@link Integer}.
     * The CRS elements are components in the dimensions that were removed, while the integer elements are
     * slots where to insert components of a reduced CRS with the number of dimensions given by the integer.
     * This array is {@code null} if at least one CRS component cannot be isolated.
     */
    @SuppressWarnings("serial")                                 // Most SIS implementations are serializable.
    private final Object[] componentsOfCRS;

    /**
     * The part of the "grid to CRS" transform which has been removed in the reduced grid geometry.
     * The number of source and target dimensions are the same as in the source grid geometry.
     * The dimensions identified by {@link #gridAxesToPass} are pass-through dimensions.
     *
     * @see #getRemovedGridToCRS(PixelInCell)
     */
    @SuppressWarnings("serial")                                 // Most SIS implementations are serializable.
    private final MathTransform removedGridToCRS, removedCornerToCRS;

    /**
     * Grid coordinates to use in {@code reverse(…)} method calls for reconstituting some removed dimensions.
     * Keys are grid dimensions of the source that are <em>not</em> in the {@link #gridAxesToPass} array.
     * Values are grid coordinates to assign to the source grid extent at the dimension identified by the key.
     * This map does not need to contain an entry for all removed dimensions.
     *
     * @see #getSliceCoordinates()
     */
    @SuppressWarnings("serial")                                 // Map.of(…) are serializable.
    private final Map<Integer, Long> sliceCoordinates;

    /**
     * A cache of {@link #gridAxesToPass} for all combinations of axes to retain in the first four dimensions.
     * We use this cache because the same sequences of dimension indices will be created most of the times.
     */
    private static final int[][] CACHED = new int[1 << 4][];    // Length must be a power of 2.

    /**
     * Returns the indices for which {@code axes} contains a bit in the set state.
     * This method may return a cached instance, <strong>do not modify.</strong>
     * Elements in the returned array are in strictly increasing order.
     */
    private static int[] toArray(final BitSet axes) {
        if (axes.length() >= CACHED.length) {
            return axes.stream().toArray();
        }
        final int bitmask = (int) axes.toLongArray()[0];
        int[] indices;
        synchronized (CACHED) {
            indices = CACHED[bitmask];
            if (indices == null) {
                CACHED[bitmask] = indices = axes.stream().toArray();
            }
        }
        return indices;
    }

    /**
     * Reduces the dimension of the specified grid geometry by retaining the axes specified in the given bitset.
     * Axes in the reduced grid geometry will be in the same order as in the source geometry:
     *
     * @param  source    the grid geometry on which to select a subset of its grid dimensions.
     * @param  gridAxes  bitmask of indices of source grid dimensions to keep in the reduced grid.
     *                   Will be modified by this constructor for internal purpose.
     * @param  factory   the factory to use for creating new math transforms, or {@code null} for the default.
     * @throws FactoryException if the dimensions to keep cannot be separated from the dimensions to omit.
     */
    protected DimensionalityReduction(final GridGeometry source, final BitSet gridAxes, final MathTransformFactory factory)
            throws FactoryException
    {
        gridAxesToPass   = toArray(gridAxes);
        sliceCoordinates = Map.of();
        sourceGeometry   = source;
        /*
         * Set `gridAxes` to its complement: instead of dimensions to pass, it will become
         * the dimensions to remove. If the result is empty, we have an identity operation.
         */
        final int sourceDim = source.getDimension();
        gridAxes.flip(0, sourceDim);
        if (gridAxes.isEmpty()) {
            reducedGeometry = source;
            crsAxesToRemove = ArraysExt.EMPTY_INT;
            componentsOfCRS = null;
        } else {
            /*
             * The calculation of `dimSubCRS` below assumes that 1 removed grid dimension
             * implies 1 removed CRS dimension. See "assumptions" in class javadoc.
             */
            final int targetDim = source.getTargetDimension();
            final int dimSubCRS = targetDim - (sourceDim - gridAxesToPass.length);
            final var helper    = new SliceGeometry(source, null, gridAxesToPass, factory);
            reducedGeometry = helper.reduce(null, dimSubCRS);
            /*
             * Get the sequence of CRS axes to remove. The result will often be
             * the same indices as `gridAxesToRemove`, but not necessarily.
             */
            final BitSet crsAxes = bitmask(helper.getTargetDimensions(), targetDim);
            crsAxes.flip(0, targetDim);
            crsAxesToRemove = toArray(crsAxes);
            if (source.isDefined(GridGeometry.CRS)) {
                componentsOfCRS = filterCRS(source.getCoordinateReferenceSystem(), crsAxes);
            } else {
                componentsOfCRS = null;
            }
            if (source.isDefined(GridGeometry.GRID_TO_CRS)) {
                final int[] gridAxesToRemove = gridAxes.stream().toArray();
                removedGridToCRS   = filterGridToCRS(gridAxesToRemove, gridAxes, PixelInCell.CELL_CENTER, factory);
                removedCornerToCRS = filterGridToCRS(gridAxesToRemove, gridAxes, PixelInCell.CELL_CORNER, factory);
                return;
            }
        }
        removedGridToCRS   = null;
        removedCornerToCRS = null;
    }

    /**
     * Returns all CRS components for the dimensions where the bit is set.
     * There is one CRS for each range of consecutive dimension indices.
     * If at least one CRS cannot be fetched, then this method returns {@code null}.
     *
     * @param  crs   the CRS for which to get components.
     * @param  axes  dimensions (or axis indices) of the components to get.
     * @throws FactoryException if the geodetic factory failed to create a compound CRS.
     * @return CRS for each range of consecutive axis indices.
     */
    private static Object[] filterCRS(final CoordinateReferenceSystem crs, final BitSet axes)
            throws FactoryException
    {
        final int dim = crs.getCoordinateSystem().getDimension();
        final var components = new Object[dim];
        int count = 0;
        int upper = 0;
        int lower;
        while ((lower = axes.nextSetBit(upper)) >= 0) {
            if (lower != upper) {
                components[count++] = lower - upper;        // Here `upper` is not yet updated to the higher value.
            }
            upper = axes.nextClearBit(lower);
            for (CoordinateReferenceSystem c : CRS.selectComponents(crs, ArraysExt.range(lower, upper))) {
                components[count++] = c;
            }
        }
        if (upper != dim) {
            components[count++] = dim - upper;              // Keep an empty slot for the reduced CRS component.
        }
        return ArraysExt.resize(components, count);
    }

    /**
     * Returns a "grid to CRS" transform which will transform only the "removed" dimensions.
     * Other dimensions are passed-through.
     *
     * @param  gridAxesToRemove  the dimensions on which to operate.
     * @param  bitset   same as {@link gridAxesToRemove} but as a bit set (for efficiency).
     * @param  anchor   whether to compute the transform for pixel corner or pixel center.
     * @param  factory  the factory to use for creating new math transforms, or {@code null} for the default.
     */
    private MathTransform filterGridToCRS(final int[] gridAxesToRemove, final BitSet bitset, final PixelInCell anchor,
            final MathTransformFactory factory) throws FactoryException
    {
        final MathTransform gridToCRS = sourceGeometry.getGridToCRS(anchor);
        final var sep = new TransformSeparator(gridToCRS, factory);
        sep.addSourceDimensions(gridAxesToRemove);
        sep.addTargetDimensions(crsAxesToRemove);
        return PassThroughTransform.create(bitset, sep.separate(), gridToCRS.getSourceDimensions(), factory);
    }

    /**
     * Creates the same dimensionality reduction as the specified {@code source}, but with different slice indices.
     *
     * @param  source  the dimensionality reduction to copy.
     * @param  slice   coordinates of the slice in removed dimensions.
     */
    private DimensionalityReduction(final DimensionalityReduction source, final Map<Integer, Long> slice) {
        sourceGeometry     = source.sourceGeometry;
        reducedGeometry    = source.reducedGeometry;
        gridAxesToPass     = source.gridAxesToPass;
        crsAxesToRemove    = source.crsAxesToRemove;
        componentsOfCRS    = source.componentsOfCRS;
        removedGridToCRS   = source.removedGridToCRS;
        removedCornerToCRS = source.removedCornerToCRS;
        sliceCoordinates   = Map.copyOf(slice);
    }

    /**
     * Returns a new bitmask of all dimension indices in the axes array.
     * The returned object can be safely modified.
     *
     * @param  axes       indices of axes to pass or to remove.
     * @param  sourceDim  maximal valid dimension index + 1.
     * @return bitmask of dimensions in the given array.
     * @throws IndexOutOfBoundsException if an axis index is out of bounds.
     */
    private static BitSet bitmask(final int[] axes, final int sourceDim) {
        final BitSet bitmask = new BitSet(sourceDim);
        for (final int dim : axes) {
            bitmask.set(Objects.checkIndex(dim, sourceDim));
        }
        return bitmask;
    }

    /**
     * Reduces the dimension of the specified grid geometry by retaining only the specified axes.
     * Axes in the reduced grid geometry will be in the same order as in the source geometry:
     * change of axis order and duplicated values in the {@code gridAxesToPass} argument are ignored.
     *
     * @param  source          the grid geometry to reduce.
     * @param  gridAxesToPass  the grid axes to retain, ignoring order and duplicated values.
     * @return reduced grid geometry together with other information.
     * @throws IndexOutOfBoundsException if a grid axis index is out of bounds.
     * @throws IllegalGridGeometryException if the dimensions to keep cannot be separated from the dimensions to omit.
     */
    public static DimensionalityReduction select(final GridGeometry source, final int... gridAxesToPass) {
        final BitSet bitmask = bitmask(gridAxesToPass, source.getDimension());
        try {
            return new DimensionalityReduction(source, bitmask, null);
        } catch (FactoryException e) {
            throw new IllegalGridGeometryException(Resources.format(Resources.Keys.NonSeparableReducedDimensions, e));
        }
    }

    /**
     * A convenience method for selecting the two first dimensions of the specified grid geometry.
     * This method can be used as a lambda function in resources query. Example:
     *
     * {@snippet lang="java" :
     *     CoverageQuery query = new CoverageQuery();
     *     query.setAxisSelection(DimensionalityReduction::select2D);
     *     }
     *
     * @param  source  the grid geometry to reduce.
     * @return reduced grid geometry together with other information.
     * @throws IndexOutOfBoundsException if the grid geometry does not have at least two dimensions.
     * @throws IllegalGridGeometryException if the dimensions to keep cannot be separated from the dimensions to omit.
     *
     * @see org.apache.sis.storage.CoverageQuery#setAxisSelection(Function)
     */
    public static DimensionalityReduction select2D(final GridGeometry source) {
        return select(source, 0, 1);
    }

    /**
     * Reduces the dimension of the specified grid geometry by removing the specified axes.
     * Axes in the reduced grid geometry will be in the same order as in the source geometry:
     * axis order and duplicated values in the {@code gridAxesToRemove} argument are not significant.
     *
     * @param  source            the grid geometry to reduce.
     * @param  gridAxesToRemove  the grid axes to remove, ignoring order and duplicated values.
     * @return reduced grid geometry together with other information.
     * @throws IndexOutOfBoundsException if a grid axis index is out of bounds.
     * @throws IllegalGridGeometryException if the dimensions to keep cannot be separated from the dimensions to omit.
     */
    public static DimensionalityReduction remove(final GridGeometry source, final int... gridAxesToRemove) {
        final int sourceDim = source.getDimension();
        final BitSet bitmask = bitmask(gridAxesToRemove, sourceDim);
        bitmask.flip(0, sourceDim);
        try {
            return new DimensionalityReduction(source, bitmask, null);
        } catch (FactoryException e) {
            throw new IllegalGridGeometryException(Resources.format(Resources.Keys.NonSeparableReducedDimensions, e));
        }
    }

    /**
     * Automatically reduces a grid geometry by removing all grid dimensions with an extent size of 1.
     * Axes in the reduced grid geometry will be in the same order as in the source geometry.
     *
     * @param  source  the grid geometry to reduce.
     * @return reduced grid geometry together with other information.
     * @throws IncompleteGridGeometryException if the grid geometry has no extent.
     * @throws IllegalGridGeometryException if the dimensions to keep cannot be separated from the dimensions to omit.
     *
     * @see #select2D(GridGeometry)
     */
    public static DimensionalityReduction reduce(final GridGeometry source) {
        final GridExtent extent = source.getExtent();
        final int sourceDim = extent.getDimension();
        final BitSet bitmask = new BitSet(sourceDim);
        for (int dim=0; dim < sourceDim; dim++) {
            if (extent.getLow(dim) != extent.getHigh(dim)) {
                bitmask.set(dim);
            }
        }
        try {
            return new DimensionalityReduction(source, bitmask, null);
        } catch (FactoryException e) {
            throw new IllegalGridGeometryException(Resources.format(Resources.Keys.NonSeparableReducedDimensions, e));
        }
    }

    /**
     * Returns {@code true} if this object does not reduce any dimension.
     * It may happen if {@code select(…)} has been invoked with all axes to keep,
     * or if {@code remove(…)} has been invoked with no axis to remove.
     *
     * @return whether this {@code DimensionalityReduction} does nothing.
     */
    public boolean isIdentity() {
        return reducedGeometry == sourceGeometry;
    }

    /**
     * Returns {@code true} if this dimensionality reduction is a slice in the source coverage.
     * This is true if all removed dimensions either have a {@linkplain GridExtent#getSize(int)
     * grid size} of one cell, or have a {@linkplain #getSliceCoordinates() slice coordinate} specified.
     *
     * <p>If this method returns {@code false}, then the results of {@code reverse(…)} method calls
     * are potentially ambiguous and may cause a {@link SubspaceNotSpecifiedException} to be thrown
     * at {@linkplain GridCoverage#render(GridExtent) rendering} time.</p>
     *
     * @return whether this dimensionality reduction is a slice in the source coverage.
     *
     * @see #getSliceCoordinates()
     * @see #withSlicePoint(long[])
     * @see #withSliceByRatio(double)
     */
    public boolean isSlice() {
        return indexOfNonSlice() >= 0;
    }

    /**
     * Ensures that {@link #isSlice()} returns {@code true}.
     *
     * @throws SubspaceNotSpecifiedException if this dimensionality reduction is not a slice of the source coverage.
     */
    final void ensureIsSlice() throws SubspaceNotSpecifiedException {
        final int dim = indexOfNonSlice();
        if (dim >= 0) {
            throw new SubspaceNotSpecifiedException(Resources.format(Resources.Keys.AmbiguousGridAxisOmission_1, dim));
        }
    }

    /**
     * If {@link #isSlice()} would returns {@code false}, returns the index of the problematic dimension.
     * Otherwise returns -1. This is used for more detailed error message.
     */
    private int indexOfNonSlice() {
        int i = gridAxesToPass.length - 1;
        final GridExtent extent = sourceGeometry.getExtent();
        for (int dim = extent.getDimension(); --dim >= 0;) {
            if (i >= 0 && dim == gridAxesToPass[i]) {
                i--;
            } else if (!sliceCoordinates.containsKey(dim)) {
                if (extent.getLow(dim) != extent.getHigh(dim)) {
                    return dim;
                }
            }
        }
        return -1;
    }

    /**
     * Returns {@code true} if the given grid geometry is likely to be already reduced.
     * Current implementation checks only the number of dimensions.
     *
     * @param  candidate  the grid geometry to test.
     * @return whether the given grid geometry is likely to be already reduced.
     */
    public boolean isReduced(final GridGeometry candidate) {
        int dim;
        if (candidate.extent == null && candidate.gridToCRS == null) {
            dim = reducedGeometry.getTargetDimension();
        } else {
            dim = reducedGeometry.getDimension();
        }
        return candidate.getDimension() == dim;
    }

    /**
     * Returns the grid geometry with only the retained grid axis dimension.
     * The number of CRS dimensions should be reduced as well,
     * but not necessarily in a one-to-one relationship.
     *
     * @return the grid geometry with retained grid dimensions.
     */
    public GridGeometry getReducedGridGeometry() {
        return reducedGeometry;
    }

    /**
     * Returns the grid geometry with all grid axis dimension.
     * This is the {@code source} argument given to factory methods.
     *
     * @return the grid geometry with all grid dimensions.
     */
    public GridGeometry getSourceGridGeometry() {
        return sourceGeometry;
    }

    /**
     * Returns the part of the "grid to CRS" transform which has been removed in the reduced grid geometry.
     * This is a pass-through transform (potentially, but not necessarily, implemented
     * by {@link org.apache.sis.referencing.operation.transform.PassThroughTransform}).
     * The number of source dimensions is the same as in the source grid geometry.
     * The dimensions that are passed-through are the dimensions on which the reduced grid geometry operates.
     *
     * @param  anchor  the cell part to map (center or corner).
     * @return removed part of the conversion from grid coordinates to "real world" coordinates.
     */
    private MathTransform getRemovedGridToCRS(final PixelInCell anchor) {
        switch (anchor) {
            case CELL_CENTER: return removedGridToCRS;
            case CELL_CORNER: return removedCornerToCRS;
            default: return PixelTranslation.translate(removedGridToCRS, PixelInCell.CELL_CENTER, anchor);
        }
    }

    /**
     * Returns the indices of the source dimensions that are kept in the reduced grid geometry.
     *
     * @return indices of source grid dimensions that are retained in the reduced grid geometry.
     */
    public int[] getSelectedDimensions() {
        return gridAxesToPass.clone();
    }

    /**
     * Returns the grid coordinates used in {@code reverse(…)} method calls for reconstituting some removed dimensions.
     * Keys are indices of grid dimensions in the source that are <em>not</em> retained in the reduced grid geometry.
     * Values are grid coordinates to assign to those dimensions when a {@code reverse(…)} method is executed.
     *
     * <p>This map does not need to contain an entry for all removed dimensions.
     * If no slice point is specified for a given dimension, then the {@code reverse(…)} methods will use the
     * full range of grid coordinates specified in the {@linkplain #getSourceGridGeometry() source geometry}.
     * Often, those ranges have a {@linkplain GridExtent#getSize(int) size} of 1,
     * in which case methods such as {@link GridCoverage#render(GridExtent)} will work anyway.
     * If a removed source grid dimension had a size greater than 1 and no slice coordinates is specified;
     * then the {@code reverse(…)} methods in this class will still work but an
     * {@link SubspaceNotSpecifiedException} may be thrown later by other classes.</p>
     *
     * <p>This map is initially empty. Slice coordinates can be specified by calls
     * to {@link #withSlicePoint(long[])} or {@link #withSliceByRatio(double)}.</p>
     *
     * @return source grid coordinates of the slice point used in {@code reverse(…)} method calls.
     *
     * @see #withSlicePoint(long[])
     * @see #withSliceByRatio(double)
     * @see GridExtent#getSliceCoordinates()
     * @see GridCoverage.Evaluator#setDefaultSlice(Map)
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")     // Map is immutable.
    public Map<Integer, Long> getSliceCoordinates() {
        return sliceCoordinates;
    }

    /**
     * Returns the dimension in the reduced grid for the given dimension in the source grid.
     * If the specified source grid dimension is not retained, then this method returns a negative number.
     *
     * @param  dim  source dimension index to map to reduced dimension index.
     * @return reduced dimension index, or a negative value if not mapped.
     */
    final int toReducedDimension(final int dim) {
        return Arrays.binarySearch(gridAxesToPass, dim);
    }

    /**
     * Returns the dimension in the source grid for the given dimension in the reduced grid.
     *
     * @param  dim  reduced dimension index to map to source dimension index.
     * @return source dimension index.
     * @throws IndexOutOfBoundsException if the given dimension is invalid.
     */
    final int toSourceDimension(final int dim) {
        return gridAxesToPass[dim];
    }

    /**
     * Returns the dimension in the source CRS for given counter of removed dimension.
     *
     * @param  i  0 for the first removed CRS dimension, 1 fo the second removed CRS dimension, <i>etc.</i>
     * @return dimension in the source CRS which has been removed, of -1 if <var>i</var> is above bounds.
     */
    private int toRemovedDimension(final int i) {
        return (i < crsAxesToRemove.length) ? crsAxesToRemove[i] : -1;
    }

    /**
     * Ensures that {@code source} has the same number of dimensions and the same axes as {@code expected}.
     * Only axis names that are specified in both extents are compared.
     * If the {@code source} to validate is null, it defaults to the {@code expected} extent.
     *
     * @param  expected  grid extent with expected axes, or {@code null} if none.
     * @param  source    grid extent to validate, or {@code null} if unspecified.
     * @return whether the two extents are equal.
     * @throws IllegalArgumentException if the number of dimensions or at least one axis name does not match.
     */
    private static boolean ensureSameAxes(final GridExtent expected, final GridExtent source) {
        if (source == null) {
            return true;
        }
        if (expected != null) {
            expected.ensureSameAxes(source, "source");
            if (expected.equals(source, ComparisonMode.IGNORE_METADATA)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the {@code actual} CRS is equal, ignore metadata, to the one in {@code expected}.
     * If any CRS is null, this method conservatively returns {@code true}.
     * This is used for assertions only.
     */
    private static boolean assertSameCRS(final GridGeometry expected, final CoordinateReferenceSystem actual) {
        if (actual != null && expected.isDefined(GridGeometry.CRS)) {
            final var crs = expected.getCoordinateReferenceSystem();
            if (crs != null) {
                return Utilities.deepEquals(crs, actual, ComparisonMode.DEBUG);
            }
        }
        return true;
    }

    /**
     * Returns a coordinate tuple on which dimensionality reduction has been applied.
     *
     * <h4>Precondition</h4>
     * The coordinate reference system of the given {@code source} should be either
     * null or equal (ignoring metadata) to the CRS of the source grid geometry.
     * For performance reason, this condition is not verified unless Java assertions are enabled.
     *
     * @param  source  the source coordinate tuple, or {@code null}.
     * @return the reduced coordinate tuple, or {@code null} if the given source was null.
     */
    @ArgumentCheckByAssertion
    public DirectPosition apply(final DirectPosition source) {
        if (source != null) {
            ArgumentChecks.ensureDimensionMatches("source", sourceGeometry.getTargetDimension(), source);
            assert assertSameCRS(sourceGeometry, source.getCoordinateReferenceSystem()) : source;
            if (!isIdentity()) {
                final var reduced = new GeneralDirectPosition(reducedGeometry.getTargetDimension());
                /*
                 * Following code is more complicated than what it could be if we stored a
                 * `crsAxesToPass` array in this object. But it may not be worth to store
                 * such array only for this method.
                 */
                int dim = -1, remCounter = 0, removedAxis = crsAxesToRemove[0];
                for (int i=0; i < reduced.coordinates.length; i++) {
                    while (++dim == removedAxis) {
                        removedAxis = toRemovedDimension(++remCounter);
                    }
                    reduced.coordinates[i] = source.getCoordinate(dim);
                }
                return reduced;
            }
        }
        return source;
    }

    /**
     * Returns a grid extent on which dimensionality reduction has been applied.
     * If the given source is {@code null}, then this method returns {@code null}.
     * Nulls are accepted because they are valid argument values in calls to
     * {@link GridCoverage#render(GridExtent)}.
     *
     * @param  source  the grid extent to reduce, or {@code null}.
     * @return the reduced grid extent. May be {@code source}, which may be null.
     * @throws MismatchedDimensionException if the given source does not have the expected number of dimensions.
     * @throws IllegalArgumentException if axis types are specified but inconsistent in at least one dimension.
     *
     * @see GridExtent#selectDimensions(int...)
     */
    public GridExtent apply(final GridExtent source) {
        if (source == null) return null;
        if (ensureSameAxes(sourceGeometry.extent, source)) {
            return reducedGeometry.extent;
        }
        return isIdentity() ? source : source.selectDimensions(gridAxesToPass);
    }

    /**
     * Returns a grid geometry on which dimensionality reduction of the grid extent has been applied.
     * It usually implies a reduction in the number of dimensions of the <abbr>CRS</abbr> as well,
     * but not necessarily in same order.
     *
     * <p>If the given source is {@code null}, then this method returns {@code null}.
     * Nulls are accepted because they are valid argument values in calls to
     * {@link org.apache.sis.storage.GridCoverageResource#read(GridGeometry, int...)}.</p>
     *
     * @param  source  the grid geometry to reduce, or {@code null}.
     * @return the reduced grid geometry. May be {@code source}, which may be null.
     * @throws MismatchedDimensionException if the given source does not have the expected number of dimensions.
     * @throws IllegalArgumentException if axis types are specified but inconsistent in at least one dimension.
     *
     * @see GridGeometry#selectDimensions(int...)
     */
    public GridGeometry apply(final GridGeometry source) {
        if (source == null) return null;
        if (ensureSameAxes(sourceGeometry.extent, source.extent)) {
            return reducedGeometry;
        }
        return isIdentity() ? source : source.selectDimensions(gridAxesToPass);
    }

    /**
     * Returns a grid coverage on which dimensionality reduction of the domain has been applied.
     * This is a reduction in the number of dimensions of the grid extent. It usually implies a
     * reduction in the number of dimensions of the CRS as well, but not necessarily in same order.
     * The sample dimensions (coverage range) are unmodified.
     *
     * <p>The returned coverage is a <em>view</em>: changes in the source coverage
     * are reflected immediately in the reduced coverage, and conversely.</p>
     *
     * <h4>Reversibility</h4>
     * If {@link #isSlice()} returns {@code false},
     * then the results of {@link #reverse(GridExtent)} are ambiguous
     * and calls to {@link GridCoverage#render(GridExtent)} may cause
     * an {@link SubspaceNotSpecifiedException} to be thrown.
     * Unless the specified {@code source} grid coverage knows how to handle those cases.
     *
     * @param  source  the grid coverage to reduce.
     * @return the reduced grid coverage, or {@code source} if this object {@linkplain #isIdentity() is identity}.
     * @throws MismatchedDimensionException if the given source does not have the expected number of dimensions.
     * @throws IllegalArgumentException if axis types are specified but inconsistent in at least one dimension.
     *
     * @see GridCoverageProcessor#selectGridDimensions(GridCoverage, int...)
     */
    @Override
    public GridCoverage apply(final GridCoverage source) {
        ensureSameAxes(sourceGeometry.extent, source.getGridGeometry().extent);
        if (isIdentity()) return source;
        if (source instanceof DimensionAppender) try {
            GridCoverage c = ((DimensionAppender) source).selectDimensions(gridAxesToPass);
            if (c != null) return c;
        } catch (FactoryException e) {
            throw new IllegalGridGeometryException(Resources.format(Resources.Keys.NonSeparableReducedDimensions, e));
        }
        return new ReducedGridCoverage(source, this);
    }

    /**
     * Returns a grid extent on which dimensionality reduction has been reverted.
     * For all dimensions that were removed, grid coordinates will be set to the
     * {@linkplain #getSliceCoordinates() slice coordinates} if specified,
     * or to the original source grid coordinates otherwise.
     * In the latter case, the reconstituted grid coordinates will be a single value
     * if {@link #isSlice()} returns {@code true} (in which case the returned extent
     * is unambiguous), or may be a (potentially ambiguous) range of values otherwise.
     *
     * <h4>Handling of null grid geometry</h4>
     * If the given extent is {@code null}, then this method returns an extent
     * with {@linkplain #getSliceCoordinates() slice coordinates} if they are known.
     * If no slice coordinate has been specified, then this method returns {@code null}.
     * Nulls are accepted because they are valid argument values
     * in calls to {@link GridCoverage#render(GridExtent)}.
     *
     * @param  reduced  the reduced grid extent to revert, or {@code null}.
     * @return the source grid extent. May be {@code reduced}, which may be null.
     * @throws IncompleteGridGeometryException if the source grid geometry has no extent.
     * @throws MismatchedDimensionException if the given extent does not have the expected number of dimensions.
     * @throws IllegalArgumentException if axis types are specified but inconsistent in at least one dimension.
     */
    public GridExtent reverse(final GridExtent reduced) {
        if (ensureSameAxes(reducedGeometry.extent, reduced)) {      // Argument validation.
            if (sliceCoordinates.isEmpty()) {                       // Must be after argument validation.
                return sourceGeometry.extent;
            }
        }
        if (isIdentity()) {
            return reduced;
        }
        final GridExtent source = sourceGeometry.getExtent();
        final long[] coordinates = source.getCoordinates();
        final int m = coordinates.length >>> 1;
        sliceCoordinates.forEach((dim, slice) -> {
            coordinates[dim  ] = slice;
            coordinates[dim+m] = slice;
        });
        if (reduced != null) {
            for (int i=0; i < gridAxesToPass.length; i++) {
                final int dim = gridAxesToPass[i];
                coordinates[dim]   = reduced.getLow (i);
                coordinates[dim+m] = reduced.getHigh(i);
            }
        }
        return new GridExtent(source, coordinates);
    }

    /**
     * Returns a grid geometry on which dimensionality reduction has been reverted.
     * For all dimensions that were removed, grid coordinates will be set to the
     * {@linkplain #getSliceCoordinates() slice coordinates} if specified,
     * or to the original source grid coordinates otherwise.
     * In the latter case, the reconstituted dimensions will map a single coordinate value
     * if {@link #isSlice()} returns {@code true} (in which case the returned grid geometry
     * is unambiguous), or may map a (potentially ambiguous) range of grid coordinate values otherwise.
     *
     * <h4>Handling of null grid geometry</h4>
     * If the given geometry is {@code null}, then this method returns a grid geometry
     * with the {@linkplain #getSliceCoordinates() slice coordinates} if they are known.
     * If no slice coordinate has been specified, then this method returns {@code null}.
     * Nulls are accepted because they are valid argument values in calls to
     * {@link org.apache.sis.storage.GridCoverageResource#read(GridGeometry, int...)}.
     *
     * @param  reduced  the reduced grid geometry to revert, or {@code null}.
     * @return the source grid geometry. May be {@code reduced}, which may be null.
     * @throws IncompleteGridGeometryException if the source grid geometry has no extent.
     * @throws MismatchedDimensionException if the given geometry does not have the expected number of dimensions.
     * @throws IllegalArgumentException if axis types are specified but inconsistent in at least one dimension.
     */
    public GridGeometry reverse(final GridGeometry reduced) {
        final GridExtent extent = (reduced != null) ? reduced.extent : null;
        if (ensureSameAxes(reducedGeometry.extent, extent)) {       // Argument validation.
            if (sliceCoordinates.isEmpty()) {                       // Must be after argument validation.
                return sourceGeometry;
            }
        }
        if (isIdentity()) {
            return reduced;
        }
        /*
         * Build a compound CRS on a "best effort" basis. This operation is costly
         * if `fullCRS(…)` must be invoked, so we try to use the existing CRS first.
         */
        CoordinateReferenceSystem crs = null;
        if (reduced.isDefined(GridGeometry.CRS) && sourceGeometry.isDefined(GridGeometry.CRS)) {
            final CoordinateReferenceSystem reducedCRS = reduced.getCoordinateReferenceSystem();
            if (CRS.equivalent(reducedGeometry.getCoordinateReferenceSystem(), reducedCRS)) {
                crs = sourceGeometry.getCoordinateReferenceSystem();
            } else {
                FactoryException cause = null;
                try {
                    crs = fullCRS(reducedCRS);
                } catch (FactoryException e) {
                    cause = e;
                }
                if (crs == null) {
                    throw new IllegalGridGeometryException(Resources.format(Resources.Keys.NonSeparableReducedDimensions, cause));
                }
            }
        }
        /*
         * Build an envelope and a resolution array where values at each dimension are copied
         * either from the source geometry or from the specified reduced geometry.
         */
        final int targetDim = sourceGeometry.getTargetDimension();
        final double[] lowerCorner = new double[targetDim];
        final double[] upperCorner = new double[targetDim];
        final double[] resolution  = new double[targetDim];
        long nonLinears  = 0;
        int  reducedDim  = 0;
        int  remCounter  = 0;
        int  removedAxis = crsAxesToRemove[0];
        for (int i=0; i<targetDim; i++) {
            final GridGeometry source;
            final int dim;
            if (i == removedAxis) {
                dim = removedAxis;
                source = sourceGeometry;
                removedAxis = toRemovedDimension(++remCounter);     // Dimension of the next axis to remove.
            } else {
                dim = reducedDim++;
                source = reduced;
            }
            lowerCorner[i] =  source.envelope.getLower(dim);
            upperCorner[i] =  source.envelope.getUpper(dim);
            resolution [i] = (source.resolution != null) ? source.resolution[dim] : Double.NaN;
            if ((source.nonLinears & Numerics.bitmask(dim)) != 0) {
                nonLinears |= Numerics.bitmask(i);
            }
        }
        return new GridGeometry(reverse(extent),
                fullGridToCRS(reduced, PixelInCell.CELL_CENTER),
                fullGridToCRS(reduced, PixelInCell.CELL_CORNER),
                new ImmutableEnvelope(lowerCorner, upperCorner, crs),
                ArraysExt.allEquals(resolution, Double.NaN) ? null : resolution,
                nonLinears);
    }

    /**
     * Builds a CRS with the same number of axes than the CRS of the source geometry.
     * Axes are copied either from the source CRS or the reduced CRS, depending on
     * whether the corresponding dimension is present in the reduced CRS.
     *
     * @param  reduced  the CRS to inflate to the same number of dimensions as the source CRS.
     * @return the "inflated" CRS.
     */
    private CoordinateReferenceSystem fullCRS(final CoordinateReferenceSystem reduced) throws FactoryException {
        if (componentsOfCRS == null) {
            return null;
        }
        final var components = new CoordinateReferenceSystem[componentsOfCRS.length];
        int lower = 0;
        for (int i=0; i<components.length; i++) {
            final Object element = componentsOfCRS[i];
            if (element instanceof CoordinateReferenceSystem) {
                components[i] = (CoordinateReferenceSystem) element;
            } else {
                final int upper = lower + (Integer) element;
                components[i] = CRS.getComponentAt(reduced, lower, upper);
                lower = upper;
            }
        }
        return CRS.compound(components);
    }

    /**
     * Builds a transform with the same number of dimensions that the transform of the source geometry.
     *
     * @param  reduced  the transform to inflate to the same number of dimensions that the source geometry.
     * @param  anchor   whether the transform map pixel centers or pixel corners.
     * @return the "inflated" transform.
     */
    private MathTransform fullGridToCRS(final GridGeometry reduced, final PixelInCell anchor) {
        final MathTransform removed = getRemovedGridToCRS(anchor);
        if (removed == null || !reduced.isDefined(GridGeometry.GRID_TO_CRS)) {
            return null;
        }
        MathTransform gridToCRS = reduced.getGridToCRS(anchor);
        if (Utilities.equalsIgnoreMetadata(reducedGeometry.getGridToCRS(anchor), gridToCRS)) {
            return sourceGeometry.getGridToCRS(anchor);
        }
        gridToCRS = MathTransforms.passThrough(gridAxesToPass, gridToCRS, removed.getTargetDimensions());
        return MathTransforms.concatenate(removed, gridToCRS);
    }

    /**
     * Returns a dimensional reduction which will use the given source grid indices for {@code reverse(…)} operations.
     * The length of the given {@code slicePoint} array shall be the number of dimensions of the source grid geometry.
     * All given coordinate values shall be inside the source grid extent.
     *
     * @param  point  grid coordinates of a point located on the slice.
     * @return the dimensionality reduction with the given slice point used for reverse operations.
     * @throws IncompleteGridGeometryException if the source grid geometry has no extent.
     * @throws MismatchedDimensionException if the given point does not have the expected number of dimensions.
     * @throws PointOutsideCoverageException if the given point is outside the source grid extent.
     */
    public DimensionalityReduction withSlicePoint(final long[] point) {
        Objects.requireNonNull(point);
        final GridExtent extent = sourceGeometry.getExtent();
        final int sourceDim = extent.getDimension();
        ArgumentChecks.ensureDimensionMatches("slicePoint", sourceDim, extent);
        final Map<Integer, Long> slices = new HashMap<>();
        for (int dim=0; dim < sourceDim; dim++) {
            final long low   = extent.getLow (dim);
            final long high  = extent.getHigh(dim);
            final long value = point[dim];
            if (value < low || value > high) {
                String b = Arrays.toString(point);
                b = b.substring(1, b.length() - 1);   // Remove brackets.
                throw new PointOutsideCoverageException(Resources.format(
                        Resources.Keys.GridCoordinateOutsideCoverage_4,
                        extent.getAxisIdentification(dim, dim), low, high, b));
            }
            if (low != high && toReducedDimension(dim) < 0) {
                slices.put(dim, value);
            }
        }
        return slices.equals(sliceCoordinates) ? this : new DimensionalityReduction(this, slices);
    }

    /**
     * Returns a dimensional reduction with a relative slice position
     * for every grid dimensions that have been removed.
     * The relative position is specified by a ratio between 0 and 1 where
     * 0 maps to {@linkplain GridExtent#getLow(int) low} grid coordinates,
     * 1 maps to {@linkplain GridExtent#getHigh(int) high grid coordinates} and
     * 0.5 maps to the median position.
     *
     * @param  ratio  the ratio to apply on all removed grid dimensions.
     * @return the dimensionality reduction with the given slice ratio applied.
     * @throws IncompleteGridGeometryException if the source grid geometry has no extent.
     * @throws IllegalArgumentException if the given ratio is not between 0 and 1 inclusive.
     *
     * @see GridExtent#getRelative(int, double)
     * @see GridDerivation#sliceByRatio(double, int...)
     */
    public DimensionalityReduction withSliceByRatio(final double ratio) {
        ArgumentChecks.ensureBetween("ratio", 0, 1, ratio);
        final GridExtent extent = sourceGeometry.getExtent();
        final int sourceDim = extent.getDimension();
        final Map<Integer, Long> slices = new HashMap<>();
        for (int dim=0; dim < sourceDim; dim++) {
            if (toReducedDimension(dim) < 0 && extent.getLow(dim) != extent.getHigh(dim)) {
                slices.put(dim, extent.getRelative(dim, ratio));
            }
        }
        return slices.equals(sliceCoordinates) ? this : new DimensionalityReduction(this, slices);
    }
}
