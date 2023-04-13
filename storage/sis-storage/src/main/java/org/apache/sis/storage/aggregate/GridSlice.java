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
package org.apache.sis.storage.aggregate;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.internal.storage.MemoryGridResource;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.internal.util.Strings;
import org.apache.sis.util.Numbers;


/**
 * A grid resource which is a slice in a larger coverage.
 * A slice is not necessarily 1 cell tick; larger slices are accepted.
 *
 * <h2>Usage context</h2>
 * Instances of {@code Gridslice} are grouped by CRS, then instances having the same CRS
 * are grouped by "grid to CRS" transform in the {@link GroupByTransform#members} list.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.3
 */
final class GridSlice {
    /**
     * The resource associated to this slice.
     */
    final GridCoverageResource resource;

    /**
     * Geometry of the grid coverage or resource.
     */
    private final GridGeometry geometry;

    /**
     * Translation from source coordinates of {@link GroupByTransform#gridToCRS}
     * to grid coordinates of {@link #geometry}. Shall be considered read-only
     * after initialization of {@link #setOffset(MatrixSIS)}.
     */
    private final long[] offset;

    /**
     * Creates a new slice for the specified coverage.
     *
     * @param  slice  coverage associated to this slice.
     */
    GridSlice(final GridCoverage slice) {
        resource = new MemoryGridResource(null, slice);
        geometry = slice.getGridGeometry();
        offset   = new long[geometry.getDimension()];
    }

    /**
     * Creates a new slice for the specified resource.
     *
     * @param  slice  resource associated to this slice.
     */
    GridSlice(final GridCoverageResource slice) throws DataStoreException {
        resource = slice;
        geometry = slice.getGridGeometry();
        offset   = new long[geometry.getDimension()];
    }

    /**
     * Sets the {@link #offset} terms to the values of the translation columns of the given matrix.
     * This method shall be invoked if and only if {@link #isIntegerTranslation(Matrix)} returned {@code true}.
     *
     * @param  groupToSlice  conversion from source coordinates of {@link GroupByTransform#gridToCRS}
     *                       to grid coordinates of {@link #geometry}.
     * @throws ArithmeticException if a translation term is NaN or overflows {@code long} integer capacity.
     *
     * @see #getOffset(Map)
     */
    private void setOffset(final MatrixSIS groupToSlice) {
        final int i = groupToSlice.getNumCol() - 1;
        for (int j=0; j<offset.length; j++) {
            offset[j] = Numbers.round(groupToSlice.getNumber(j, i));
        }
    }

    /**
     * Returns {@code true} if the given matrix is the identity matrix except for translation terms.
     * Translation terms must be integer values.
     *
     * @param  groupToSlice  conversion from {@link GroupByTransform#gridToCRS}
     *         source coordinates to {@link #gridToCRS} source coordinates.
     * @return whether the matrix is identity, ignoring integer translation.
     *
     * @see org.apache.sis.referencing.operation.matrix.Matrices#isTranslation(Matrix)
     */
    private static boolean isIntegerTranslation(final Matrix groupToSlice) {
        final int numRows = groupToSlice.getNumRow();
        final int numCols = groupToSlice.getNumCol();
        for (int j=0; j<numRows; j++) {
            for (int i=0; i<numCols; i++) {
                double tolerance = Numerics.COMPARISON_THRESHOLD;
                double e = groupToSlice.getElement(j, i);
                if (i == j) {
                    e--;
                } else if (i == numCols - 1) {
                    final double a = Math.abs(e);
                    if (a > 1) {
                        tolerance = Math.min(tolerance*a, 0.125);
                    }
                    e -= Math.rint(e);
                }
                if (!(Math.abs(e) <= tolerance)) {      // Use `!` for catching NaN.
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns the group of objects associated to the CRS and "grid to CRS" transform.
     * The CRS comparisons ignore metadata and transform comparisons ignore integer translations.
     * This method takes a synchronization lock on the given list.
     *
     * @param  groups    the list where to search for a group.
     * @param  strategy  algorithm to apply when more than one grid coverage can be found at the same grid index.
     * @return group of objects associated to the given transform (never null).
     * @throws NoninvertibleTransformException if the transform is not invertible.
     * @throws ArithmeticException if a translation term is NaN or overflows {@code long} integer capacity.
     */
    final GroupByTransform getList(final List<GroupByCRS<GroupByTransform>> groups, final MergeStrategy strategy)
            throws NoninvertibleTransformException
    {
        final MathTransform gridToCRS = geometry.getGridToCRS(PixelInCell.CELL_CORNER);
        final MathTransform crsToGrid = gridToCRS.inverse();
        final List<GroupByTransform> transforms = GroupByCRS.getOrAdd(groups, geometry).members;
        synchronized (transforms) {
            for (final GroupByTransform c : transforms) {
                final Matrix groupToSlice = c.linearTransform(crsToGrid);
                if (groupToSlice != null && isIntegerTranslation(groupToSlice)) {
                    setOffset(MatrixSIS.castOrCopy(groupToSlice));
                    c.strategy = strategy;
                    return c;
                }
            }
            final GroupByTransform c = new GroupByTransform(geometry, gridToCRS, strategy);
            transforms.add(c);
            return c;
        }
    }

    /**
     * Returns the grid extent of this slice. The grid coordinate system is specific to this slice.
     * For converting grid coordinates to the concatenated grid coordinate system, {@link #offset}
     * must be subtracted.
     */
    final GridExtent getGridExtent() {
        return geometry.getExtent();
    }

    /**
     * Writes information about grid extent into the given {@code DimensionSelector} objects.
     * This is invoked by {@link GroupByTransform#findConcatenatedDimensions()} for choosing
     * a dimension to concatenate.
     */
    final void getGridExtent(final int i, final DimensionSelector[] writeTo) {
        final GridExtent extent = getGridExtent();
        for (int dim = writeTo.length; --dim >= 0;) {
            writeTo[dim].setSliceExtent(i, Math.subtractExact(extent.getMedian(dim), offset[dim]), extent.getSize(dim));
        }
    }

    /**
     * Returns the low grid index in the given dimension, relative to the grid of the group.
     * This is invoked by {@link GroupByTransform#sortAndGetLows(int)} for sorting coverages.
     *
     * @param  dim  dimension of the desired grid coordinates.
     * @return low index in the coordinate system of the group grid.
     */
    final long getGridLow(final int dim) {
        return Math.subtractExact(geometry.getExtent().getLow(dim), offset[dim]);
    }

    /**
     * Returns the translation from source coordinates of {@link GroupByTransform#gridToCRS} to
     * grid coordinates of {@link #geometry}. This method returns a unique instance if possible.
     *
     * @param  shared  a pool of existing offset instances.
     * @return translation from aggregated grid geometry to slice. Shall be considered read-only.
     */
    final long[] getOffset(final Map<GridSlice,long[]> shared) {
        final long[] old = shared.putIfAbsent(this, offset);
        return (old != null) ? old : offset;
    }

    /**
     * Compares the offset of this grid slice with the offset of given slice.
     * This method is defined only for the purpose of {@link #getOffset(Map)}.
     * Equality should not be used in other context.
     */
    @Override
    public final boolean equals(final Object other) {
        return (other instanceof GridSlice) && Arrays.equals(((GridSlice) other).offset, offset);
    }

    /**
     * Returns a hash code for the offset consistently with {@link #equals(Object)} purpose.
     */
    @Override
    public final int hashCode() {
        return Arrays.hashCode(offset);
    }

    /**
     * Returns a string representation for debugging purposes.
     */
    @Override
    public String toString() {
        Object id = null;
        if (resource != null) try {
            id = resource.getIdentifier().orElse(null);
        } catch (DataStoreException e) {
            id = e.toString();
        }
        return Strings.toString(getClass(), null, id);
    }
}
