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
package org.apache.sis.storage.gdal;

import java.lang.foreign.Arena;
import java.lang.foreign.ValueLayout;
import java.lang.foreign.MemorySegment;
import java.text.ParseException;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.internal.shared.AxisDirections;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.resources.Errors;


/**
 * Information about a Coordinate Reference System object in <abbr>GDAL</abbr>.
 * Instances of this class should be short-lived and used inside a synchronized block,
 * because it is a wrapper around {@code OGRSpatialReference} which has the following note:
 *
 * <blockquote>A pointer to an internal object. It should not be altered or freed.
 * Its lifetime will be the one of the dataset object, or until the next call to this method.
 * </blockquote>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class SpatialRef {
    /**
     * A constant for identifying the codes which assume two-dimensional data.
     */
    static final int BIDIMENSIONAL = 2;

    /**
     * The data set which provided the <abbr>CRS</abbr> definition.
     * A desired side-effect of this field is to prevent premature garbage collection of
     * that {@code GDALStore}, because it would cause the {@link #handle} to become invalid.
     */
    private final GDALStore owner;

    /**
     * Sets of handles for invoking <abbr>GDAL</abbr> functions. Usually not stored in objects,
     * but we make an exception for this {@code SpatialRef} class because instances should be short-lived.
     */
    private final GDAL gdal;

    /**
     * Pointer to the <abbr>GDAL</abbr> object in native memory.
     * This is a {@code OGRSpatialReferenceH} in the C/C++ <abbr>API</abbr>.
     */
    private final MemorySegment handle;

    /**
     * The result of the call to {@code OSRGetDataAxisToSRSAxisMapping}, computed when first needed.
     *
     * @see #getDataAxisToCRSAxis(int)
     */
    private int[] dataAxisToCRSAxis;

    /**
     * Creates a new instance.
     *
     * @param  owner   the dataset which is providing the <abbr>CRS</abbr> definition.
     * @param  gdal    sets of handles for invoking <abbr>GDAL</abbr> functions.
     * @param  handle  pointer to native {@code OGRSpatialReferenceH}.
     */
    private SpatialRef(final GDALStore owner, final GDAL gdal, final MemorySegment handle) {
        this.owner  = owner;
        this.gdal   = gdal;
        this.handle = handle;
    }

    /**
     * Creates a new instance.
     *
     * @param  owner    the dataset which is providing the <abbr>CRS</abbr> definition.
     * @param  gdal     sets of handles for invoking <abbr>GDAL</abbr> functions.
     * @param  dataset  value of {@link GDALStore#handle()}.
     * @return wrapper for the <abbr>CRS</abbr> definition provided by <abbr>GDAL</abbr>, or {@code null} if none.
     * @throws DataStoreException if an error occurred while fetching information from <abbr>GDAL</abbr>.
     */
    static SpatialRef create(final GDALStore owner, final GDAL gdal, final MemorySegment dataset) throws DataStoreException {
        MemorySegment handle;
        try {
            handle = (MemorySegment) gdal.getSpatialRef.invokeExact(dataset);
            if (GDAL.isNull(handle)) {
                handle = (MemorySegment) gdal.getGCPSpatialRef.invokeExact(owner.handle());
                if (GDAL.isNull(handle)) {
                    return null;
                }
            }
        } catch (Throwable e) {
            throw GDAL.propagate(e);
        }
        return new SpatialRef(owner, gdal, handle);
    }

    /**
     * Fetches the reference system of a (potentially null) {@code OGRSpatialReferenceH}.
     *
     * @param  owner   the dataset which is providing the <abbr>CRS</abbr> definition.
     * @param  gdal    sets of handles for invoking <abbr>GDAL</abbr> functions.
     * @param  handle  pointer to native {@code OGRSpatialReferenceH}.
     * @return wrapper for the <abbr>CRS</abbr> definition provided by <abbr>GDAL</abbr>, or {@code null} if none.
     */
    static CoordinateReferenceSystem parseCRS(final GDALStore owner, final GDAL gdal, final MemorySegment handle)
            throws Throwable
    {
        if (GDAL.isNull(handle)) {
            return null;
        }
        final var ref = new SpatialRef(owner, gdal, handle);
        final var crs = ref.parseCRS("components");
        if (crs != null) {
            final AxisDirection[] directions = ref.getDataAxisDirections(crs.getCoordinateSystem());
            if (directions != null) {
                final AbstractCRS sc = AbstractCRS.castOrCopy(crs);
                for (AxesConvention c : AxesConvention.valuesForOrder()) {
                    final AbstractCRS candidate = sc.forConvention(c);
                    if (AxisDirections.hasPrefix(candidate.getCoordinateSystem(), directions)) {
                        return candidate;
                    }
                }
                // TODO: create a derived CRS.
            }
        }
        return crs;
    }

    /**
     * Parses the <abbr>CRS</abbr> of the data set by parsing its <abbr>WKT</abbr> representation.
     * This method must be invoked from a method synchronized on {@link GDALStore}.
     *
     * @param  caller  the method in {@code GDALStore} to report as the emitter of the warning.
     * @return the parsed <abbr>CRS</abbr>, or {@code null} if none.
     * @throws DataStoreException if a fatal error occurred according <abbr>GDAL</abbr>.
     */
    final CoordinateReferenceSystem parseCRS(final String caller) throws DataStoreException {
        final int err;
        final String wkt;
        try (Arena arena = Arena.ofConfined()) {
            final var layout = ValueLayout.ADDRESS;
            final MemorySegment ptr = arena.allocate(layout).fill((byte) 0);
            final MemorySegment opt = GDAL.toNullTerminatedStrings(arena, "FORMAT=WKT2_2015");
            err = (int) gdal.exportToWkt.invokeExact(handle, ptr, opt);
            final MemorySegment result = ptr.get(layout, 0);
            if (GDAL.isNull(result)) {
                wkt = null;
            } else try {
                wkt = GDAL.toString(result);
            } finally {
                gdal.free.invokeExact(result);
            }
        } catch (Throwable e) {
            throw GDAL.propagate(e);
        }
        if (err == 0) {     // OGRErr, not CPLErr.
            if (wkt != null && !wkt.isBlank()) try {
                return (CoordinateReferenceSystem) owner.wktFormat().parseObject(wkt);
            } catch (ParseException | ClassCastException e) {
                owner.warning(caller, Errors.forLocale(owner.getLocale())
                        .getString(Errors.Keys.CanNotParseCRS_1, owner.getDisplayName()), e);
            }
        }
        return null;
    }

    /**
     * Returns the mapping from data axis order to CRS axis order. Numbering starts at 1.
     * Negative values mean that the axis direction needs to be flipped.
     * This is typically an array of length 2 with the following values:
     *
     * <ul>
     *   <li>{@code mapping[0]} (data axis number for the first  axis of the CRS) is usually 1, 2, -1, -2.</li>
     *   <li>{@code mapping[1]} (data axis number for the second axis of the CRS) is usually 1, 2, -1, -2.</li>
     * </ul>
     *
     * By convention, an array of length 0 means that no change is needed.
     *
     * @param  dimension  maximal number of dimensions of the <abbr>CRS</abbr>.
     * @return axis mapping using GDAL convention (axis numbering starts at 1).
     */
    @SuppressWarnings({"restricted", "ReturnOfCollectionOrArrayField"})
    private int[] getDataAxisToCRSAxis(int dimension) {
        if (dataAxisToCRSAxis == null) {
            dataAxisToCRSAxis = ArraysExt.EMPTY_INT;
            try (Arena arena = Arena.ofConfined()) {
                final var layout = ValueLayout.JAVA_INT;
                final MemorySegment count  = arena.allocate(layout);
                final MemorySegment vector = (MemorySegment) gdal.getDataAxisToCRSAxis.invokeExact(handle, count);
                if (!GDAL.isNull(vector)) {
                    dimension = Math.min(count.get(layout, 0), dimension);
                    if (dimension > 0) {
                        int[] indices = vector.reinterpret(layout.byteSize() * dimension).toArray(layout);
                        if (!ArraysExt.isRange(1, indices)) {
                            dataAxisToCRSAxis = indices;
                        }
                    }
                }
            } catch (Throwable e) {
                throw GDAL.propagate(e);
            }
        }
        return dataAxisToCRSAxis;
    }

    /**
     * Returns the axis directions of the data, or {@code null} if there is no change compared to the CRS.
     *
     * @param  cs  coordinate system of the CRS.
     * @return data axis directions, or {@code null} if there is no change compared to the CRS.
     */
    private AxisDirection[] getDataAxisDirections(final CoordinateSystem cs) {
        final int[] indices = getDataAxisToCRSAxis(cs.getDimension());
        final int length = indices.length;
        if (length == 0) {
            return null;
        }
        final var directions = new AxisDirection[length];
        for (int i=0; i<length; i++) {
            AxisDirection dir = cs.getAxis(i).getDirection();
            int t = indices[i];
            if (t < 0) {
                dir = AxisDirections.opposite(dir);
            }
            directions[Math.abs(t) - 1] = dir;
        }
        return directions;
    }

    /**
     * Returns the transform from data axis order to CRS axis order, or {@code null} if unspecified.
     * This method also takes care of adding a dimension to the "grid to CRS" transform if needed.
     *
     * @param  dimension  maximal number of dimensions of the <abbr>CRS</abbr>.
     * @return axis swapping as an affine transform matrix, or {@code null} if unspecified or identity.
     */
    final Matrix getDataToCRS(final int dimension) {
        final int[] indices = getDataAxisToCRSAxis(dimension);
        final int length = indices.length;
        if (length == 0) {
            return null;
        }
        /*
         * From GDAL documentation: The number of elements of the vector will be the number of axis of the CRS.
         * Values start at 1. A negative value can also be used to ask for a sign reversal during coordinate
         * transformation (to deal with northing vs southing, easting vs westing, heights vs depths).
         */
        final Matrix swap = Matrices.createZero(length+1, BIDIMENSIONAL + 1);
        swap.setElement(length, BIDIMENSIONAL, 1);
        for (int i=0; i<length; i++) {
            final int p = indices[i];
            if (p != 0) {
                swap.setElement(i, Math.abs(p) - 1, Integer.signum(p));
            }
        }
        return swap;
    }
}
