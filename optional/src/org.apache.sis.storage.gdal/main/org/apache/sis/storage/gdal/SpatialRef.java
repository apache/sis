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
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.storage.DataStoreException;
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
     * Creates a new instance.
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
     * Parses the <abbr>CRS</abbr> of the data set by parsing its <abbr>WKT</abbr> representation.
     * This method must be invoked from a method synchronized on {@link GDALStore}.
     *
     * @param  caller  name of the {@code GDALStore} method invoking this method.
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
                owner.warning(caller, Errors.format(Errors.Keys.CanNotParseCRS_1, owner.getDisplayName()), e);
            }
        }
        return null;
    }

    /**
     * Returns the transform from data axis order to CRS axis order, or {@code null} if unspecified.
     * This method also takes care of adding a dimension to the "grid to CRS" transform if needed.
     *
     * @param  dimension  maximal number of dimensions of the <abbr>CRS</abbr>.
     * @return axis swapping as an affine transform matrix.
     */
    @SuppressWarnings("restricted")
    final Matrix getDataToCRS(final int dimension) {
        final int length;
        MemorySegment vector;
        final var layout = ValueLayout.JAVA_INT;
        try (Arena arena = Arena.ofConfined()) {
            final MemorySegment ptr = arena.allocate(layout);
            vector = (MemorySegment) gdal.getDataAxisToCRSAxis.invokeExact(handle, ptr);
            if (GDAL.isNull(vector) || (length = Math.min(ptr.get(layout, 0), dimension)) < 0) {
                return null;
            }
        } catch (Throwable e) {
            throw GDAL.propagate(e);
        }
        vector = vector.reinterpret(layout.byteSize() * length);
        /*
         * From GDAL documentation: The number of elements of the vector will be the number of axis of the CRS.
         * Values start at 1. A negative value can also be used to ask for a sign reversal during coordinate
         * transformation (to deal with northing vs southing, easting vs westing, heights vs depths).
         */
        final Matrix swap = Matrices.createZero(length+1, BIDIMENSIONAL + 1);
        swap.setElement(length, BIDIMENSIONAL, 1);
        for (int i=0; i<length; i++) {
            final int p = vector.getAtIndex(layout, i);
            if (p != 0) {
                swap.setElement(i, Math.abs(p) - 1, Integer.signum(p));
            }
        }
        return swap;
    }
}
