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
import java.util.Spliterator;
import java.util.function.Consumer;
import org.apache.sis.feature.privy.AttributeConvention;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.panama.NativeFunctions;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
final class OgrSpliterator implements Spliterator<Feature>{

    private final OGRFeatureSet fs;
    private final OgrGeometryReader geomReader;
    private final FeatureType type;
    private boolean closed = false;
    private final GDAL gdal;

    OgrSpliterator(OGRFeatureSet fs, FeatureType type) throws DataStoreException {
        this.fs = fs;
        this.type = type;
        gdal = fs.gdal;
        geomReader = new OgrGeometryReader(gdal);
    }

    @Override
    public Spliterator<Feature> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
        return NONNULL;
    }

    @Override
    public boolean tryAdvance(Consumer<? super Feature> action) {
        //Reading the features of the layer
        try {
            final MemorySegment featurePt = (MemorySegment) gdal.ogrLayerGetNextFeature.invokeExact(fs.layer);
            if (featurePt == null || featurePt.address() == 0L) {
                return false;
            }

            final Feature feature = type.newInstance();
            feature.setPropertyValue(AttributeConvention.IDENTIFIER, (long) gdal.ogrFeatureGetFid.invokeExact(featurePt));
            //Loop for all the features of the layer
            for (int i = 0; i < fs.fields.length; i++) {
                OGRFieldType fieldType = fs.fieldEncs[i];
                String propName = fs.fields[i];
                if (fieldType == OGRFieldType.OFTInteger) {
                    feature.setPropertyValue(propName, (int) gdal.ogrFeatureGetFieldAsInteger.invokeExact(featurePt, i));
                } else if (fieldType == OGRFieldType.OFTInteger64) {
                    feature.setPropertyValue(propName, (long) gdal.ogrFeatureGetFieldAsInteger64.invokeExact(featurePt, i));
                } else if (fieldType == OGRFieldType.OFTReal) {
                    feature.setPropertyValue(propName, (double) gdal.ogrFeatureGetFieldAsDouble.invokeExact(featurePt, i));
                } else if (fieldType == OGRFieldType.OFTString) {
                    feature.setPropertyValue(propName, NativeFunctions.toString( (MemorySegment) gdal.ogrFeatureGetFieldAsString.invokeExact(featurePt, i)));
                } else {
                    feature.setPropertyValue(propName, NativeFunctions.toString( (MemorySegment) gdal.ogrFeatureGetFieldAsString.invokeExact(featurePt, i)));
                }
            }
            // read geometry
            if (fs.geomFields.length > 0) {
                //TODO loop on all geometries
                final MemorySegment ogrGeom = (MemorySegment) gdal.ogrFeatureGetGeometryRef.invokeExact(featurePt);
                Geometry geom = geomReader.toGeometry(ogrGeom);
                final AttributeType geomType = (AttributeType) type.getProperty(fs.geomFields[0]);

                //shapefile mix LineString with MultiLineString and Polygon with MultiPolygon, but not JTS
                if (geom instanceof LineString) {
                    final MultiLineString ml = geom.getFactory().createMultiLineString(new LineString[]{(LineString) geom});
                    ml.setUserData(geom.getUserData());
                    geom = ml;
                } else if (geom instanceof Polygon) {
                    final MultiPolygon mp = geom.getFactory().createMultiPolygon(new Polygon[]{(Polygon) geom});
                    mp.setUserData(geom.getUserData());
                    geom = mp;
                }
                geom.setUserData(fs.crs);
                feature.setPropertyValue(fs.geomFields[0], geom);
            }
            //release memory
            gdal.ogrFeatureDestroy.invokeExact(featurePt);

            action.accept(feature);
            return true;
        }  catch (Throwable e) {
            throw GDAL.propagate(e);
        }
    }

    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
    }

}
