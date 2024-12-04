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

import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.lang.foreign.Arena;
import java.lang.foreign.AddressLayout;
import java.lang.foreign.MemorySegment;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.privy.AttributeConvention;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryType;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.storage.AbstractFeatureSet;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.privy.Strings;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;


/**
 * Information about a feature set in <abbr>OGR</abbr>.
 * This is a wrapper around {@code OGRLayerH} in the C/C++ <abbr>API</abbr>.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
final class FeatureLayer extends AbstractFeatureSet {
    /**
     * The data store which owns this feature layer.
     * All operations must be synchronized on this store, because <abbr>GDAL</abbr> is not thread-safe.
     */
    final GDALStore store;

    /**
     * Pointer to the <abbr>OGR</abbr> object in native memory.
     * This is a {@code OGRLayerH} in the C/C++ <abbr>API</abbr>.
     */
    final MemorySegment handle;

    /**
     * Description (list of fields) of all feature instances in this layer.
     */
    final FeatureType type;

    /**
     * Description of the fields to read with GDAL API. Each {@code FieldAccessor} contains the property name
     * and the code to execute for fetching the field value from an {@code OGRFeatureH} with <abbr>OGR</abbr>,
     */
    final FieldAccessor<?>[] fields;

    /**
     * The coordinate reference system of the default geometry field (the first one), or {@code null} if unknown.
     * Note that each geometry field can also have its own CRS, not necessarily equals to this one.
     */
    private final CoordinateReferenceSystem defaultCRS;

    /**
     * The library to use for building geometry objects.
     */
    final Geometries<?> library;

    /**
     * Non-null if an iteration is in progress. Each {@code FeatureLayer} can have only one iteration in progress
     * at a given time, because <abbr>GDAL</abbr> {@code OGRLayerH} C/C++ <abbr>API</abbr> provides only one cursor.
     */
    FeatureIterator iterationInProgress;

    /**
     * Wraps a <abbr>GDAL</abbr> {@code OGRLayerH} and builds the feature type.
     *
     * @param  store   the data store which owns this feature layer.
     * @param  gdal    set of native methods to use.
     * @param  handle  the {@code OGRLayerH} to wrap.
     * @throws Throwable if an error occurred during a call to a native method or in Java code.
     */
    private FeatureLayer(final GDALStore store, final GDAL gdal, final OGR ogr, final MemorySegment handle) throws Throwable {
        super(store);
        this.store           = store;
        this.handle          = handle;
        this.library         = Geometries.factory(store.library);
        final var builder    = new FeatureTypeBuilder();
        final var definition = (MemorySegment) ogr.getLayerDefinition.invokeExact(handle);
        final int fieldCount = (int) ogr.getFieldCount.invokeExact(definition);
        final int geomCount  = (int) ogr.getGeomFieldCount.invokeExact(definition);
        /*
         * Fetch the non-geometry fields.
         */
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final var fields = new FieldAccessor<?>[FieldAccessor.NUM_ADDITIONAL_FIELDS + fieldCount + geomCount];
        int fieldIndex = 0;
        fields[fieldIndex++] = addAttribute(builder, FieldAccessor.Identifier.INSTANCE);
        for (int i=0; i<fieldCount; i++) {
            fields[fieldIndex++] = addAttribute(builder, FieldAccessor.create(ogr, definition, i));
        }
        /*
         * Fetch the geometry fields with the first one taken as the default geometry.
         * Each geometry field can have its own coordinate reference system (optional).
         * The declared geometry type may be conservatively generalized to `GEOMETRY`
         * if the information provided by the driver is not reliable.
         */
        final String  driverName = store.getDriverName(gdal);
        final boolean generalize = (driverName != null) && driverName.toLowerCase(Locale.US).contains("shapefile");

        @SuppressWarnings("LocalVariableHidesMemberVariable")
        CoordinateReferenceSystem defaultCRS = null;

        boolean isFirstGeometry = true;
        for (int i=0; i<geomCount; i++) {
            final var geomField = (MemorySegment) ogr.getGeomFieldDefinition.invokeExact(definition, i);
            if (GDAL.isNull(geomField)) continue;       // Paranoiac check, but should never happen.
            boolean setAsDefault = isFirstGeometry;
            final int geomType  = (int) ogr.getGeomFieldType.invokeExact(geomField);
            String name = GDAL.toString((MemorySegment) ogr.getGeomFieldName.invokeExact(geomField));
            if (name == null || name.isBlank()) {
                name = setAsDefault ? AttributeConvention.GEOMETRY : "geometry" + i;
                setAsDefault = false;       // Because already has the default name.
            }
            /*
             * OGR Hack: ShapeFile geometry type is not correctly detected because the OGR driver does
             * not distinguish betwen polygon and multi-polygon. The same issue applies to line strings.
             * However, points are okay (ShapeFile has distinct Point and MultiPoint types).
             * https://code.djangoproject.com/ticket/7218
             */
            GeometryType gt = GeometryType.forBinaryType(geomType);
            if (generalize && (gt == GeometryType.POLYGON || gt == GeometryType.LINESTRING)) {
                gt = GeometryType.GEOMETRY;
            }
            Class<?> geomClass = library.getGeometryClass(gt);
            /*
             * Add the geometry attribute together with the corresponding `FieldAccessor` for fetching geometries.
             * Each field may have its own CRS, but the first one will be taken as the CRS for default geometries.
             */
            final CoordinateReferenceSystem crs = SpatialRef.parseCRS(store, gdal,
                    (MemorySegment) ogr.getGeomFieldSpatialRef.invokeExact(geomField));

            final AttributeTypeBuilder<?> attribute = builder.addAttribute(geomClass).setName(name).setCRS(crs);
            if (setAsDefault) {     // First geometry as default.
                attribute.addRole(AttributeRole.DEFAULT_GEOMETRY);
            }
            fields[fieldIndex++] = new FieldAccessor.Geometry<>(name, i, geomClass, crs);
            if (isFirstGeometry) {
                isFirstGeometry = false;
                defaultCRS = crs;           // May still be null.
            }
        }
        this.type       = builder.setName(GDAL.toString((MemorySegment) ogr.getLayerName.invokeExact(handle))).build();
        this.fields     = ArraysExt.resize(fields, fieldIndex);
        this.defaultCRS = defaultCRS;
    }

    /**
     * Adds a non-geometry attribute to a feature type. This is a helper method for the constructor.
     * The returned {@link FieldAccessor} instance encapsulates the code for fetching the attribute value.
     */
    private static FieldAccessor<?> addAttribute(final FeatureTypeBuilder builder, final FieldAccessor<?> field) {
        final AttributeTypeBuilder<?> attribute;
        Class<?> ft = field.getElementClass();
        if (ft != null) {
            attribute = builder.addAttribute(ft).setMaximumOccurs(Integer.MAX_VALUE);
        } else {
            attribute = builder.addAttribute(field.getJavaClass());
        }
        attribute.setName(field.name);
        return field;
    }

    /**
     * Returns all feature layers found in the data store.
     *
     * @param  parent  wrapper for the {@code GDALDatasetH} of <abbr>GDAL</abbr> C/C++ <abbr>API</abbr>.
     * @param  gdal    set of <abbr>GDAL</abbr> native functions.
     * @return all layers found, or an empty array if none.
     * @throws DataStoreException if an error occurred.
     */
    static FeatureLayer[] listLayers(final GDALStore parent, final GDAL gdal) throws DataStoreException {
        final MemorySegment dataset = parent.handle();
        try {
            final int n = (int) gdal.getLayerCount.invokeExact(dataset);
            final var layers = new FeatureLayer[n];
            if (n != 0) {
                final OGR ogr = gdal.ogr();     // Initialize only if we have at least one layer.
                for (int i=0; i<n; i++) {
                    final var layer = (MemorySegment) ogr.getLayer.invokeExact(dataset, i);
                    layers[i] = new FeatureLayer(parent, gdal, ogr, layer);
                }
            }
            return layers;
        } catch (Throwable e) {
            throw GDAL.propagate(e);
        }
    }

    /**
     * Returns the feature type of this feature set.
     */
    @Override
    public final FeatureType getType() {
        return type;
    }

    /**
     * Returns the set of <abbr>OGR</abbr> native functions.
     *
     * @return the set of native functions.
     * @throws DataStoreException if the native library is not available.
     */
    final OGR OGR() throws DataStoreException {
        return store.getProvider().GDAL().ogr();
    }

    /**
     * Returns the layer bounding box if its computation is not too expansive.
     *
     * @return the layer bounding box, or empty if none or too expensive to compute.
     * @throws DataStoreException if GDAL raised an error.
     */
    @Override
    public Optional<Envelope> getEnvelope() throws DataStoreException {
        final OGR ogr = OGR();
        try (Arena arena = Arena.ofConfined()) {
            final MemorySegment extent = arena.allocate(Double.BYTES * 4);      // minX, maxX, minY, maxY.
            int error;
            synchronized (store) {
                try {
                    error = (int) ogr.getLayerExtent.invokeExact(handle, extent, 0);
                } catch (Throwable e) {
                    throw GDAL.propagate(e);
                }
            }
            if (error != 0) {
                // GDAL documentation said that `OGRERR_FAILURE` can simply means that the extent is not known.
                return Optional.empty();
            }
            final GeneralEnvelope env;
            if (defaultCRS != null) {
                env = new GeneralEnvelope(defaultCRS);
            } else {
                env = new GeneralEnvelope(SpatialRef.BIDIMENSIONAL);
            }
            final var t = AddressLayout.JAVA_DOUBLE;
            env.setRange(0, extent.getAtIndex(t, 0), extent.getAtIndex(t, 1));
            env.setRange(1, extent.getAtIndex(t, 2), extent.getAtIndex(t, 3));
            return Optional.of(env);
        } finally {
            ErrorHandler.throwOnFailure(store, "getEnvelope");
        }
    }

    /**
     * Returns a stream of feature instances to be read from this layer.
     * Only one stream can be executed at a time.
     *
     * @param  parallel  ignored since <abbr>GDAL</abbr> is not thread-safe.
     */
    @Override
    public Stream<Feature> features(boolean parallel) throws DataStoreException {
        try {
            synchronized (store) {
                // Ignore the `parallel` argument, as it is not supported by the iterator.
                final var it = new FeatureIterator(this);
                return StreamSupport.stream(it, false).onClose(it);
            }
        } finally {
            ErrorHandler.throwOnFailure(store, "features");
        }
    }

    /**
     * Returns the locale-dependent resources for error messages.
     */
    final Errors errors() {
        return Errors.forLocale(store.getLocale());
    }

    /**
     * Returns a string representation of this layer for debugging purposes.
     */
    @Override
    public String toString() {
        return Strings.toString(getClass(), null, type.getName(), "crs", IdentifiedObjects.getDisplayName(defaultCRS));
    }
}
