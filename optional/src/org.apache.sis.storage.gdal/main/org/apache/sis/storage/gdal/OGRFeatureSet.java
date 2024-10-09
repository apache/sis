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

import java.lang.foreign.MemorySegment;
import java.util.stream.Stream;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.privy.AttributeConvention;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.storage.AbstractFeatureSet;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.panama.NativeFunctions;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
final class OGRFeatureSet extends AbstractFeatureSet {

    private final GDALStore store;
    private final GDAL gdal;
    private final MemorySegment dataset;
    private final MemorySegment layer;
    private FeatureType type;

    OGRFeatureSet(final GDALStore store, final GDAL gdal, final MemorySegment dataset, MemorySegment layer) {
        super(store);
        this.store = store;
        this.gdal = gdal;
        this.dataset = dataset;
        this.layer = layer;
    }

    @Override
    public synchronized FeatureType getType() throws DataStoreException {
        if (type != null) return type;

        try {
            final MemorySegment driver = (MemorySegment) gdal.getDatasetDriver.invokeExact(dataset);
            final String driverName = NativeFunctions.toString((MemorySegment) gdal.getName.invokeExact(driver));

            final MemorySegment layerDef = (MemorySegment) gdal.ogrLayerGetLayerDefn.invokeExact(layer);
            final String name = NativeFunctions.toString( ((MemorySegment) gdal.ogrLayerGetName.invokeExact(layer)));

            final FeatureTypeBuilder ftb =  new FeatureTypeBuilder();
            ftb.addAttribute(Long.class).setName(AttributeConvention.IDENTIFIER_PROPERTY);
            ftb.setName(name);

            // List feature type fields
            final String[] fields = new String[(int)gdal.ogrFeatureDefinitionGetFieldCount.invokeExact(layerDef)];
            for (int i = 0 ; i<fields.length; i++) {
                final MemorySegment fieldDef  = (MemorySegment) gdal.ogrFeatureDefinitionGetFieldDefinition.invokeExact(layerDef,i);
                final OGRFieldType type = OGRFieldType.valueOf((int) gdal.ogrFeatureDefinitionGetFieldType.invokeExact(fieldDef));
                final String fieldName  = NativeFunctions.toString( (MemorySegment)gdal.ogrFeatureDefinitionGetFieldName.invokeExact(fieldDef));
                final Class<?> valueClass  = type.getJavaClass();
                ftb.addAttribute(valueClass).setName(fieldName);
                fields[i] = fieldName;
            }

            // List geometric feature type fields
            final String[] geomFields = new String[(int) gdal.ogrFeatureDefinitionGetGeomFieldCount.invokeExact(layerDef)];
            for(int i=0; i<geomFields.length; i++) {
                final MemorySegment ogrGeomFieldDefnH = (MemorySegment) gdal.ogrFeatureDefinitionGetGeomFieldDefinition.invokeExact(layerDef,i);
                final OGRwkbGeometryType typrgeom = OGRwkbGeometryType.valueOf( (int) gdal.ogrFeatureDefinitionGetGeomFieldType.invokeExact(ogrGeomFieldDefnH));
                geomFields[i]      = NativeFunctions.toString((MemorySegment)gdal.ogrFeatureDefinitionGetGeomFieldName.invokeExact(ogrGeomFieldDefnH));
                if(geomFields[i].isEmpty()) geomFields[i] = "geometry"+i;

                //read CRS
                final MemorySegment crsRef  = (MemorySegment) gdal.ogrFeatureDefinitionGetGeomFieldSpatialRef.invokeExact(ogrGeomFieldDefnH);
                final SpatialRef spatialRef = SpatialRef.createWithHandle(store, gdal, crsRef);
                CoordinateReferenceSystem crs = spatialRef.parseCRS("listVectors");
                //force longitude first
                crs = ((AbstractCRS)crs).forConvention(AxesConvention.RIGHT_HANDED);

                Class<?> geomClass = typrgeom.getJavaClass();
                if (driverName.toLowerCase().contains("shapefile")) {
                    //OGR Hack : shapefile geometry type is not correctly detected
                    //this seems to be because ogr shp driver do not make a difference betwen geom types
                    //https://code.djangoproject.com/ticket/7218
                    if (Polygon.class.equals(geomClass)) geomClass = MultiPolygon.class;
                    if (LineString.class.equals(geomClass)) geomClass = MultiLineString.class;
                }

                final AttributeTypeBuilder<?> attBuilder = ftb.addAttribute(geomClass).setName(geomFields[i]).setCRS(crs);
                if (i == 0) attBuilder.addRole(AttributeRole.DEFAULT_GEOMETRY);     // first geometry as default
            }
            type = ftb.build();
        } catch (Throwable e) {
            throw GDAL.propagate(e);
        }
        return type;
    }

    @Override
    public Stream<Feature> features(boolean parallel) throws DataStoreException {
        throw new DataStoreException("Not supported yet.");
    }

    /**
     * Returns featuresets found.
     *
     * @throws DataStoreException if an error occurred.
     */
    static OGRFeatureSet[] listVectors(final GDALStore parent, final GDAL gdal, final MemorySegment dataset)
        throws DataStoreException {
        try {
            final OGRFeatureSet[] array = new OGRFeatureSet[(int) gdal.datasetGetLayerCount.invokeExact(dataset)];
            for (int iLayer = 0; iLayer < array.length ; iLayer++) {
                final MemorySegment layer = (MemorySegment) gdal.datasetGetLayer.invokeExact(dataset, iLayer);
                array[iLayer] = new OGRFeatureSet(parent, gdal, dataset, layer);
            }
            return array;
        } catch (Throwable e) {
            throw GDAL.propagate(e);
        }
    }
}
