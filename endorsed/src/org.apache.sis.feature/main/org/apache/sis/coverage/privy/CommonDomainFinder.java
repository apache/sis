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
package org.apache.sis.coverage.privy;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.NoSuchElementException;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.IllegalGridGeometryException;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.referencing.privy.ExtendedPrecisionMatrix;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.privy.Numerics;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.coordinate.MismatchedDimensionException;


/**
 * Helper class for building a combined domain from a list of grid geometries.
 * After construction, one of the following methods shall be invoked exactly once.
 *
 * <ul>
 *   <li>{@link #setFromGridAligned(GridGeometry...)}</li>
 * </ul>
 *
 * Then, the result can be obtained by the given methods:
 *
 * <ul>
 *   <li>{@link #result()}</li>
 *   <li>{@link #gridTranslations()}</li>
 *   <li>{@link #sourceOfGridToCRS()}</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class CommonDomainFinder {
    /**
     * Whether to compute intersection (true) or union (false) of all grid coverages.
     * This is not yet configurable in current version.
     *
     * <p>We use this constant for tracking the code to update when we will want to provide an option for using
     * union or strict equality instead (the latter would be a mode that fails if all extents are not identical).
     * Note that in the case of unions, it would be possible to specify coverages with no intersection.
     * Whether we should accept that or raise an exception is still an open question.</p>
     */
    public static final boolean INTERSECTION = true;

    /**
     * The grid geometry taken as the reference for computing the translations of all grid geometry items.
     * Values of {@link #gridTranslations} and {@link #itemTranslations} are relative to that reference.
     * At first this is an arbitrary grid geometry, for example the first encountered one.
     * After {@code CommonDomainFinder} completed its task, this is updated to the final result.
     *
     * @see #result()
     */
    private GridGeometry reference;

    /**
     * Coordinate reference system of the reference, or {@code null} if not yet known.
     * It will be set to the CRS of the first grid geometry where the CRS is defined.
     */
    private CoordinateReferenceSystem crs;

    /**
     * The inverse of the "grid to CRS" transform of the grid geometry taken as a reference.
     */
    private MathTransform crsToGrid;

    /**
     * The convention to use for fetching the "grid to CRS" transforms.
     */
    private final PixelInCell anchor;

    /**
     * The combined extent, as the union or intersection of all grid extents.
     */
    private GridExtent extent;

    /**
     * The translation in units of grid cells from the {@linkplain #reference} grid geometry
     * to the grid geometry in the key.
     */
    private final Map<GridGeometry,long[]> gridTranslations;

    /**
     * Translations in units of grid cells from the {@linkplain #reference} grid geometry to each item.
     * For each index <var>i</var>, {@code itemTranslations[i]} is a value from {@link #gridTranslations}
     * map and may be reused at more than one index <var>i</var>.
     *
     * @see #gridTranslations()
     */
    private long[][] itemTranslations;

    /**
     * If one of the grid geometries has the same "grid to CRS" than the common grid geometry, the index.
     * Otherwise -1.
     */
    private int sourceOfGridToCRS;

    /**
     * Creates a new common domain finder.
     *
     * @param  anchor  the convention to use for fetching the "grid to CRS" transforms.
     */
    CommonDomainFinder(final PixelInCell anchor) {
        this.anchor = anchor;
        gridTranslations = new LinkedHashMap<>();
    }

    /**
     * Computes a common grid geometry from the given items.
     * All items shall share be aligned on the same grid.
     * Items may be translated relative to each other,
     * but the translations shall be an integer number of grid cells.
     *
     * <h4>Coordinate reference system</h4>
     * If the CRS of a grid geometry is undefined, it is assumed the same as other grid geometries.
     *
     * @param  items  the grid geometries for which to compute a common grid geometry.
     * @throws IllegalGridGeometryException if the specified item is not compatible with the reference grid geometry.
     */
    final void setFromGridAligned(final GridGeometry... items) {
        itemTranslations = new long[items.length][];
        for (int i=0; i<items.length; i++) {
            itemTranslations[i] = gridTranslations.computeIfAbsent(items[i], this::itemToCommon);
        }
        /*
         * Change the reference grid geometry for matching more closely the desired grid extent.
         * If one item has exactly the desired grid extent, use it. Otherwise search for an item
         * having the same origin. This criterion is arbitrary and may change in future version.
         */
        GridGeometry fallback = null;
        GridExtent   location = null;
        for (final Map.Entry<GridGeometry,long[]> entry : gridTranslations.entrySet()) {
            final GridGeometry item   = entry.getKey();
            final GridExtent actual   = item.getExtent();
            final GridExtent expected = extent.translate(entry.getValue());
            if (actual.equals(expected)) {
                setGridToCRS(items, item);      // Must be before `reference` assignation.
                reference = item;
                return;
            }
            // Arbitrary criterion (may be revisited in any future version).
            if (fallback == null && expected.getLow().equals(actual.getLow())) {
                location = expected;
                fallback = item;
            }
        }
        if (fallback == null) {
            fallback = reference;
            location = extent;
        }
        setGridToCRS(items, fallback);
        try {
            reference = fallback.relocate(location);
        } catch (TransformException e) {
            throw new IllegalGridGeometryException(Resources.format(Resources.Keys.IncompatibleGridGeometries), e);
        }
    }

    /**
     * Given a grid geometry with the desired "grid to CRS", saves its index in {@link #sourceOfGridToCRS}.
     * This method updates all previously computed translations for making them relative to the new reference.
     *
     * Note: updating values of the {@link #gridTranslations} map indirectly update all
     * {@link #itemTranslations} array elements.
     */
    private void setGridToCRS(final GridGeometry[] items, final GridGeometry item) {
        sourceOfGridToCRS = indexOf(items, item);
        if (item == reference) {                    // Quick check for a common case.
            return;
        }
        final long[] oldReference = itemTranslations[indexOf(items, reference)];
        final long[] newReference = itemTranslations[sourceOfGridToCRS];
        final long[] change = new long[newReference.length];
        for (int i=0; i < newReference.length; i++) {
            change[i] = Math.subtractExact(newReference[i], oldReference[i]);
        }
        for (final long[] offset : gridTranslations.values()) {
            for (int i=0; i < offset.length; i++) {
                offset[i] = Math.subtractExact(offset[i], change[i]);
            }
        }
    }

    /**
     * Returns the index of the given grid geometry.
     * This method is invoked when the instance should always exist in the array.
     */
    private static int indexOf(final GridGeometry[] items, final GridGeometry item) {
        for (int i=0; i<items.length; i++) {
            if (items[i] == item) {
                return i;
            }
        }
        throw new NoSuchElementException();         // Should never happen.
    }

    /**
     * Returns the common grid geometry computed from all specified items.
     *
     * @return a grid geometry which is the union or intersection of all specified items.
     */
    final GridGeometry result() {
        if (crs != null && !reference.isDefined(GridGeometry.CRS)) {
            reference = new GridGeometry(extent, anchor, reference.getGridToCRS(anchor), crs);
        }
        return reference;
    }

    /**
     * Returns the translations (in units of grid cells) from the common grid geometry to all items.
     * The items are the arguments given to {@link #setFromGridAligned(GridGeometry...)}, in order.
     * The common grid geometry is the value returned by {@link #result()}.
     *
     * <p>The returned array should not be modified because it is not cloned.</p>
     *
     * @return translation from the common grid geometry to all items. This array is not cloned.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final long[][] gridTranslations() {
        return itemTranslations;
    }

    /**
     * If one items has the same "grid to CRS" than the common grid geometry, returns its index.
     *
     * @return index of an items having the desired "grid to CRS", or -1 if none.
     */
    final int sourceOfGridToCRS() {
        return sourceOfGridToCRS;
    }

    /**
     * Computes the translation in units of grid cells from the common grid geometry to the given item.
     * This method also opportunistically computes the union or intersection of all grid extents.
     *
     * @param  item  the grid geometry for which to get a translation from the common grid geometry.
     * @return translation in unis of grid cells. Note that the caller may reuse this array for many grid geometries.
     * @throws IllegalGridGeometryException if the specified item is not compatible with the reference grid geometry.
     */
    private long[] itemToCommon(final GridGeometry item) {
        /*
         * Compute the change ourselves instead of invoking `GridGeometry.createTransformTo(…)`
         * because we do not want wraparound handling when we search for a simple translation.
         */
        MathTransform change = item.getGridToCRS(anchor);
        try {
            if (crsToGrid == null) {
                crsToGrid = change.inverse();
                reference = item;
            }
            if (item.isDefined(GridGeometry.CRS)) {
                final CoordinateReferenceSystem src = item.getCoordinateReferenceSystem();
                if (crs == null) {
                    crs = src;
                } else {
                    /*
                     * Ask for a change of CRS without specifying an area of interest (AOI) on the assumption
                     * that if the transformation is only a translation, the AOI would not make a difference.
                     * It save not only the AOI computation cost, but also make easier for `findOperation(…)`
                     * to use its cache.
                     */
                    change = MathTransforms.concatenate(change, CRS.findOperation(src, crs, null).getMathTransform());
                }
            }
            change = MathTransforms.concatenate(change, crsToGrid);
        } catch (FactoryException | NoninvertibleTransformException | MismatchedDimensionException e) {
            throw new IllegalGridGeometryException(Resources.format(Resources.Keys.IncompatibleGridGeometries), e);
        }
        final long[] offset = integerTranslation(MathTransforms.getMatrix(change));
        if (offset == null) {
            throw new IllegalGridGeometryException(Resources.format(Resources.Keys.IncompatibleGridGeometries));
        }
        /*
         * The grid geometry has been accepted as valid. Now compute the combined extent,
         * taking offset in account. At this point this is an offset TO the common grid geometry.
         * It will be converted to an offset FROM the common grid geometry after the extent update.
         */
        final GridExtent e = item.getExtent().translate(offset);
        if (extent == null) {
            extent = e;
        } else if (INTERSECTION) {
            extent = extent.intersect(e);
        } else if (!extent.equals(e)) {
            throw new IllegalGridGeometryException();
        }
        for (int i=0; i<offset.length; i++) {
            offset[i] = Math.negateExact(offset[i]);
        }
        return offset;
    }

    /**
     * If the given matrix is the identity matrix except for translation terms, returns the translation.
     * These translation terms must be integer values. If the matrix is not an integer translation,
     * this method return {@code null}.
     *
     * @param  change  conversion between two grid geometries, or {@code null}.
     * @return the translation terms, or {@code null} if the given matrix does not met the conditions.
     *
     * @see org.apache.sis.referencing.operation.matrix.Matrices#isTranslation(Matrix)
     */
    public static long[] integerTranslation(final Matrix change) {
        if (change == null) {
            return null;
        }
        final int numRows = change.getNumRow();
        final int numCols = change.getNumCol();
        for (int j=0; j<numRows; j++) {
            for (int i=0; i<numCols; i++) {
                double tolerance = Numerics.COMPARISON_THRESHOLD;
                double e = change.getElement(j, i);
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
                    return null;
                }
            }
        }
        final long[] offset = new long[numRows - 1];
        final int i = numCols - 1;
        if (change instanceof ExtendedPrecisionMatrix) {
            final var epm = (ExtendedPrecisionMatrix) change;
            for (int j=0; j<offset.length; j++) {
                final Number e = epm.getElementOrNull(j, i);
                offset[j] = (e != null) ? Numbers.round(e) : 0;
            }
        } else {
            for (int j=0; j<offset.length; j++) {
                offset[j] = Math.round(change.getElement(j, i));
            }
        }
        return offset;
    }
}
