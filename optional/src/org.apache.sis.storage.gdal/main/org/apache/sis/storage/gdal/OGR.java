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

import java.lang.foreign.ValueLayout;
import java.lang.foreign.FunctionDescriptor;
import java.lang.invoke.MethodHandle;


/**
 * Handlers to <abbr>GDAL</abbr> <abbr>OGR</abbr> native functions needed by this package.
 * The "<abbr>OGR</abbr>" part of <abbr>GDAL</abbr> contains the API for accessing vector data.
 * This class is created separately from {@link GDAL} in order to lookup the symbols only if needed.
 * Historically, <abbr>OGR</abbr> was a separated library before to be merged in <abbr>GDAL</abbr> 2.0.
 *
 * <p>Unless otherwise noted in the documentation, the objects returned by the native functions are
 * owned by their parent (dataset, layer, <i>etc.</i>) and shall not be deleted by the application.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see <a href="https://gdal.org/en/latest/api/vector_c_api.html">GDAL Vector C API</a>
 */
final class OGR {
    /**
     * <abbr>GDAL</abbr> {@code OGRLayerH GDALDatasetGetLayer(GDALDatasetH, int)}.
     * Fetch a layer by index.
     */
    final MethodHandle getLayer;

    /**
     * <abbr>OGR</abbr> {@code OGRFeatureDefnH OGR_L_GetLayerDefn(OGRLayerH)}.
     * Fetch the schema information for the layer.
     */
    final MethodHandle getLayerDefinition;

    /**
     * <abbr>OGR</abbr> {@code char* OGR_L_GetName(OGRLayerH)}.
     * Return the layer name.
     */
    final MethodHandle getLayerName;

    /**
     * <abbr>OGR</abbr> {@code OGRErr OGR_L_GetExtent(OGRLayerH, OGREnvelope*, int)}.
     * Fetch spatial extent. May or may not take the spatial filter in account.
     * May alter the read cursor of the layer.
     *
     * @todo Replace by {@code OGR_L_GetExtent3D()} for taking the spatial filter in account in all cases.
     */
    final MethodHandle getLayerExtent;

    /**
     * <abbr>OGR</abbr> {@code GIntBig OGR_L_GetFeatureCount(OGRLayerH, int)}.
     * Fetch the feature count in this layer, or returns -1 if the count is unknown.
     * The last argument specifies whether to force a count even if it would be expensive.
     */
    final MethodHandle getFeatureCount;

    /**
     * <abbr>OGR</abbr> {@code void OGR_L_ResetReading(OGRLayerH)}.
     * Reset feature reading to start on the first feature.
     */
    final MethodHandle resetReading;

    /**
     * <abbr>OGR</abbr> {@code OGRFeatureH OGR_L_GetNextFeature(OGRLayerH) CPL_WARN_UNUSED_RESULT}.
     * Fetch the next available feature from the layer, taking in account the current spatial filter.
     * If non-null, the returned instance must be released by a call to {@link #destroyFeature}.
     * A null value means that there is no more feature available.
     */
    final MethodHandle getNextFeature;

    /**
     * <abbr>OGR</abbr> {@code GIntBig OGR_F_GetFID(OGRFeatureH)}.
     * Get feature identifier, or {@code OGRNullFID} (-1) if none has been assigned.
     */
    final MethodHandle getFeatureId;

    /**
     * <abbr>OGR</abbr> {@code int OGR_FD_GetFieldCount(OGRFeatureDefnH)}.
     * Fetch number of fields on the passed feature definition.
     */
    final MethodHandle getFieldCount;

    /**
     * <abbr>OGR</abbr> {@code int OGR_FD_GetGeomFieldCount(OGRFeatureDefnH)}.
     * Fetch the number of geometry fields on the given feature definition.
     */
    final MethodHandle getGeomFieldCount;

    /**
     * <abbr>OGR</abbr> {@code OGRFieldDefnH OGR_FD_GetFieldDefn(OGRFeatureDefnH, int)}.
     * Fetch field definition of the passed feature definition.
     */
    final MethodHandle getFieldDefinition;

    /**
     * <abbr>OGR</abbr> {@code OGRGeomFieldDefnH OGR_FD_GetGeomFieldDefn(OGRFeatureDefnH, int)}.
     * Fetch geometry field definition of the passed feature definition.
     */
    final MethodHandle getGeomFieldDefinition;

    /**
     * <abbr>OGR</abbr> {@code OGRFieldType OGR_Fld_GetType(OGRFieldDefnH)}.
     * Fetch the attribute type of the specified non-geometry field.
     */
    final MethodHandle getFieldType;

    /**
     * <abbr>OGR</abbr> {@code OGRwkbGeometryType OGR_GFld_GetType(OGRGeomFieldDefnH)}.
     * Fetch the geometry type of the specified geometry field.
     */
    final MethodHandle getGeomFieldType;

    /**
     * <abbr>OGR</abbr> {@code char* OGR_Fld_GetNameRef(OGRFieldDefnH)}.
     * Fetch name of the specified non-geometry field.
     */
    final MethodHandle getFieldName;

    /**
     * <abbr>OGR</abbr> {@code char* OGR_GFld_GetNameRef(OGRGeomFieldDefnH)}.
     * Fetch name of the specified geometry field.
     */
    final MethodHandle getGeomFieldName;

    /**
     * <abbr>OGR</abbr> {@code OGRSpatialReferenceH OGR_GFld_GetSpatialRef(OGRGeomFieldDefnH)}.
     * Fetch the spatial reference system of the specified field.
     */
    final MethodHandle getGeomFieldSpatialRef;

    /**
     * <abbr>OGR</abbr> {@code int OGR_F_IsFieldNull(OGRFeatureH, int)}.
     * Test if a field is null.
     */
    final MethodHandle isFieldNull;

    /**
     * <abbr>OGR</abbr> {@code char* OGR_F_GetFieldAsString(OGRFeatureH, int)}.
     * Fetch field value as a string.
     */
    final MethodHandle getFieldAsString;

    /**
     * <abbr>OGR</abbr> {@code int OGR_F_GetFieldAsInteger(OGRFeatureH, int)}.
     * Fetch field value as integer. Unconvertible values are returned as 0.
     */
    final MethodHandle getFieldAsInteger;

    /**
     * <abbr>OGR</abbr> {@code GIntBig OGR_F_GetFieldAsInteger64(OGRFeatureH, int)}.
     * Fetch field value as 64 bits integer. Unconvertible values are returned as 0.
     */
    final MethodHandle getFieldAsLong;

    /**
     * <abbr>OGR</abbr> {@code double OGR_F_GetFieldAsDouble(OGRFeatureH, int)}.
     * Fetch field value as a double. Unconvertible values are returned as 0.
     */
    final MethodHandle getFieldAsDouble;

    /**
     * <abbr>OGR</abbr> {@code int* OGR_F_GetFieldAsDateTimeEx(OGRFeatureH, int, ...)}.
     * Fetch field value as date and time.
     */
    final MethodHandle getFieldAsDateTime;

    /**
     * <abbr>OGR</abbr> {@code char** OGR_F_GetFieldAsStringList(OGRFeatureH, int)}.
     * Fetch field value as a list of strings.
     */
    final MethodHandle getFieldAsStringList;

    /**
     * <abbr>OGR</abbr> {@code int* OGR_F_GetFieldAsIntegerList(OGRFeatureH, int, int*)}.
     * Fetch field value as a list of 32-bits integers.
     */
    final MethodHandle getFieldAsIntegerList;

    /**
     * <abbr>OGR</abbr> {@code GIntBig* OGR_F_GetFieldAsInteger64List(OGRFeatureH, int, int*)}.
     * Fetch field value as a list of 64-bits integers.
     */
    final MethodHandle getFieldAsLongList;

    /**
     * <abbr>OGR</abbr> {@code double* OGR_F_GetFieldAsDoubleList(OGRFeatureH, int, int*)}.
     * Fetch field value as a list of double-precision floating point numbers.
     */
    final MethodHandle getFieldAsDoubleList;

    /**
     * <abbr>OGR</abbr> {@code byte* OGR_F_GetFieldAsBinary(OGRFeatureH, int, int*)}.
     * Fetch field value as binary.
     */
    final MethodHandle getFieldAsBinary;

    /**
     * <abbr>OGR</abbr> {@code OGRGeometryH OGR_F_GetGeometryRef(OGRFeatureH)}.
     * Fetch a handle to feature geometry.
     */
    final MethodHandle getFeatureGeometry;

    /**
     * <abbr>OGR</abbr> {@code OGRwkbGeometryType OGR_G_GetGeometryType(OGRGeometryH)}.
     * Fetch the geometry type. The type may include the 2.5D flag.
     */
    final MethodHandle getGeometryType;

    /**
     * <abbr>OGR</abbr> {@code int OGR_G_GetGeometryCount(OGRGeometryH)}.
     * Fetch the number of elements in a geometry or number of geometries in container.
     * If invoked on an unsupported geometry type, silently returns 0.
     */
    final MethodHandle getGeometryCount;

    /**
     * <abbr>OGR</abbr> {@code OGRGeometryH OGR_G_GetGeometryRef(OGRGeometryH, int)}.
     * Fetch geometry from a geometry container. For a polygon, index 0 stands for the
     * exterior ring and other indices are the interior rings.
     */
    final MethodHandle getGeometryRef;

    /**
     * <abbr>OGR</abbr> {@code int OGR_G_GetPointCount(OGRGeometryH)}.
     * Fetch number of points from a Point or a LineString/LinearRing geometry.
     * If invoked on an unsupported geometry type, silently returns 0.
     */
    final MethodHandle getPointCount;

    /**
     * <abbr>OGR</abbr> {@code int OGR_G_GetPointsZM(OGRGeometryH, void*, int, void*, int, void*, int, void*, int)}.
     * Returns all points of line string.
     */
    final MethodHandle getPoints;

    /**
     * <abbr>OGR</abbr> {@code void OGR_F_Destroy(OGRFeatureH)}.
     * Destroy feature.
     */
    final MethodHandle destroyFeature;

    /**
     * Fetches the symbols for <abbr>OGR</abbr> native methods.
     *
     * @param  gdal  the <abbr>GDAL</abbr> native functions to be extended with <abbr>OGR</abbr> functions.
     */
    OGR(final GDAL gdal) {
        // A few frequently-used function signatures.
        final var V_A   = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS);
        final var A_A   = FunctionDescriptor.of(ValueLayout.ADDRESS,   ValueLayout.ADDRESS);
        final var I_A   = FunctionDescriptor.of(ValueLayout.JAVA_INT,  ValueLayout.ADDRESS);
        final var L_A   = FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS);
        final var I_AI  = FunctionDescriptor.of(ValueLayout.JAVA_INT,  ValueLayout.ADDRESS, ValueLayout.JAVA_INT);
        final var L_AI  = FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT);
        final var A_AI  = FunctionDescriptor.of(ValueLayout.ADDRESS,   ValueLayout.ADDRESS, ValueLayout.JAVA_INT);
        final var A_AIA = FunctionDescriptor.of(ValueLayout.ADDRESS,   ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS);

        getLayer           = gdal.lookup("GDALDatasetGetLayer", A_AI);
        getLayerDefinition = gdal.lookup("OGR_L_GetLayerDefn",  A_A);
        getLayerName       = gdal.lookup("OGR_L_GetName",       A_A);
        getLayerExtent     = gdal.lookup("OGR_L_GetExtent", FunctionDescriptor.of(
                ValueLayout.JAVA_INT,       // OGRERR_NONE on success, OGRERR_FAILURE if extent not known.
                ValueLayout.ADDRESS,        // Handle to the layer from which to get extent.
                ValueLayout.ADDRESS,        // Structure in which the extent value will be returned.
                ValueLayout.JAVA_INT));     // Whether the extent should be computed even if it is expensive.

        resetReading           = gdal.lookup("OGR_L_ResetReading",        V_A);
        getFeatureCount        = gdal.lookup("OGR_L_GetFeatureCount",     L_AI);
        getNextFeature         = gdal.lookup("OGR_L_GetNextFeature",      A_A);
        getFeatureId           = gdal.lookup("OGR_F_GetFID",              L_A);
        getFieldCount          = gdal.lookup("OGR_FD_GetFieldCount",      I_A);
        getGeomFieldCount      = gdal.lookup("OGR_FD_GetGeomFieldCount",  I_A);
        getFieldDefinition     = gdal.lookup("OGR_FD_GetFieldDefn",       A_AI);
        getGeomFieldDefinition = gdal.lookup("OGR_FD_GetGeomFieldDefn",   A_AI);
        getFieldType           = gdal.lookup("OGR_Fld_GetType",           I_A);
        getGeomFieldType       = gdal.lookup("OGR_GFld_GetType",          I_A);
        getFieldName           = gdal.lookup("OGR_Fld_GetNameRef",        A_A);
        getGeomFieldName       = gdal.lookup("OGR_GFld_GetNameRef",       A_A);
        getGeomFieldSpatialRef = gdal.lookup("OGR_GFld_GetSpatialRef",    A_A);
        isFieldNull            = gdal.lookup("OGR_F_IsFieldNull",         I_AI);
        getFieldAsString       = gdal.lookup("OGR_F_GetFieldAsString",    A_AI);
        getFieldAsInteger      = gdal.lookup("OGR_F_GetFieldAsInteger",   I_AI);
        getFieldAsLong         = gdal.lookup("OGR_F_GetFieldAsInteger64", L_AI);
        getFieldAsDouble       = gdal.lookup("OGR_F_GetFieldAsDouble", FunctionDescriptor.of(
                ValueLayout.JAVA_DOUBLE,    // Returned field value.
                ValueLayout.ADDRESS,        // Handle to the feature from which to get the value.
                ValueLayout.JAVA_INT));     // Index of the field to fetch.

        getFieldAsDateTime = gdal.lookup("OGR_F_GetFieldAsDateTimeEx", FunctionDescriptor.of(
                ValueLayout.JAVA_INT,       // Whether the operation was successful.
                ValueLayout.ADDRESS,        // Handle to the feature from which to get the value.
                ValueLayout.JAVA_INT,       // Index of the field to fetch.
                ValueLayout.ADDRESS,        // Where to store the year   as `int*`.
                ValueLayout.ADDRESS,        // Where to store the month  as `int*` (1-12).
                ValueLayout.ADDRESS,        // Where to store the day    as `int*` (1-31).
                ValueLayout.ADDRESS,        // Where to store the hour   as `int*` (0-23).
                ValueLayout.ADDRESS,        // Where to store the minute as `int*` (0-59).
                ValueLayout.ADDRESS,        // Where to store the second as `float*`.
                ValueLayout.ADDRESS));      // Where to store timezone information `int*`.

        getFieldAsStringList  = gdal.lookup("OGR_F_GetFieldAsStringList",    A_AI);
        getFieldAsIntegerList = gdal.lookup("OGR_F_GetFieldAsIntegerList",   A_AIA);
        getFieldAsLongList    = gdal.lookup("OGR_F_GetFieldAsInteger64List", A_AIA);
        getFieldAsDoubleList  = gdal.lookup("OGR_F_GetFieldAsDoubleList",    A_AIA);
        getFieldAsBinary      = gdal.lookup("OGR_F_GetFieldAsBinary",        A_AIA);
        getFeatureGeometry    = gdal.lookup("OGR_F_GetGeometryRef",          A_A);
        getGeometryType       = gdal.lookup("OGR_G_GetGeometryType",         I_A);
        getGeometryCount      = gdal.lookup("OGR_G_GetGeometryCount",        I_A);
        getGeometryRef        = gdal.lookup("OGR_G_GetGeometryRef",          A_AI);
        getPointCount         = gdal.lookup("OGR_G_GetPointCount",           I_A);
        getPoints             = gdal.lookup("OGR_G_GetPointsZM", FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_INT));

        destroyFeature = gdal.lookup("OGR_F_Destroy",  V_A);
    }
}
