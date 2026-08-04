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
package org.apache.sis.storage.geojson;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.sis.feature.internal.shared.AttributeConvention;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.geojson.binding.GeoJSONFeature;
import org.apache.sis.storage.geojson.binding.GeoJSONFeatureCollection;
import org.apache.sis.storage.geojson.binding.GeoJSONGeometry;
import org.apache.sis.storage.geojson.binding.GeoJSONGeometryCollection;
import org.apache.sis.storage.geojson.binding.GeoJSONLineString;
import org.apache.sis.storage.geojson.binding.GeoJSONMultiLineString;
import org.apache.sis.storage.geojson.binding.GeoJSONMultiPoint;
import org.apache.sis.storage.geojson.binding.GeoJSONMultiPolygon;
import org.apache.sis.storage.geojson.binding.GeoJSONPoint;
import org.apache.sis.storage.geojson.binding.GeoJSONPolygon;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.IdentifiedType;
import org.opengis.feature.Operation;
import org.opengis.feature.PropertyNotFoundException;
import org.opengis.feature.PropertyType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class GeoJSONMapper {

    private boolean bboxOnCollection = false;
    private boolean bboxOnFeature = false;
    private boolean bboxOnGeometry = false;
    private boolean includeFeatureId = true;
    private boolean includeTypeOnFeature = false;
    private boolean includeTypeOnCollection = false;
    private boolean includeCoordRefSysOnCollection = false;
    private boolean includeCoordRefSysOnFeature = false;
    private boolean includeCoordRefSysOnGeometry = false;

    public void setBboxOnCollection(boolean bboxOnCollection) {
        this.bboxOnCollection = bboxOnCollection;
    }

    public boolean isBboxOnCollection() {
        return bboxOnCollection;
    }

    public void setBboxOnFeature(boolean bboxOnFeature) {
        this.bboxOnFeature = bboxOnFeature;
    }

    public boolean isBboxOnFeature() {
        return bboxOnFeature;
    }

    public void setBboxOnGeometry(boolean bboxOnGeometry) {
        this.bboxOnGeometry = bboxOnGeometry;
    }

    public boolean isBboxOnGeometry() {
        return bboxOnGeometry;
    }

    public void setIncludeFeatureId(boolean includeFeatureId) {
        this.includeFeatureId = includeFeatureId;
    }

    public boolean isIncludeFeatureId() {
        return includeFeatureId;
    }

    public void setIncludeTypeOnFeature(boolean includeTypeOnFeature) {
        this.includeTypeOnFeature = includeTypeOnFeature;
    }

    public boolean isIncludeTypeOnFeature() {
        return includeTypeOnFeature;
    }

    public void setIncludeTypeOnCollection(boolean includeTypeOnCollection) {
        this.includeTypeOnCollection = includeTypeOnCollection;
    }

    public boolean isIncludeTypeOnCollection() {
        return includeTypeOnCollection;
    }

    public void setIncludeCoordRefSysOnCollection(boolean includeCoordRefSysOnCollection) {
        this.includeCoordRefSysOnCollection = includeCoordRefSysOnCollection;
    }

    public boolean isIncludeCoordRefSysOnCollection() {
        return includeCoordRefSysOnCollection;
    }

    public void setIncludeCoordRefSysOnFeature(boolean includeCoordRefSysOnFeature) {
        this.includeCoordRefSysOnFeature = includeCoordRefSysOnFeature;
    }

    public boolean isIncludeCoordRefSysOnFeature() {
        return includeCoordRefSysOnFeature;
    }

    public void setIncludeCoordRefSysOnGeometry(boolean includeCoordRefSysOnGeometry) {
        this.includeCoordRefSysOnGeometry = includeCoordRefSysOnGeometry;
    }

    public boolean isIncludeCoordRefSysOnGeometry() {
        return includeCoordRefSysOnGeometry;
    }

    public GeoJSONFeature transform(Feature feature) throws DataStoreException {
        final FeatureType type = feature.getType();

        final GeoJSONFeature gf = new GeoJSONFeature();

        if (includeTypeOnFeature) {
            gf.setFeatureType(getTypeName(type));
        }

        if (includeFeatureId && feature.getType().hasProperty(AttributeConvention.IDENTIFIER)) {    // TODO: should be determined in advance.
            Object id = feature.getPropertyValue(AttributeConvention.IDENTIFIER);
            if (id != null) {
                gf.setId(id);
            }
        }
        if (includeCoordRefSysOnFeature) {
            gf.setCoordRefSys(getCoordRefSys(type));
        }

        if (feature.getType().hasProperty(AttributeConvention.GEOMETRY)) {  // TODO: should be determined in advance.
            Object geom = feature.getPropertyValue(AttributeConvention.GEOMETRY);
            if (geom instanceof Geometry g) {
                GeoJSONGeometry json = transform(g);
                gf.setGeometry(json);

                if (bboxOnFeature) {
                    Envelope env = g.getEnvelopeInternal();
                    if (env != null && !env.isNull()) {
                        final List<Double> bbox = new ArrayList<>();
                        bbox.add(env.getMinX());
                        bbox.add(env.getMinY());
                        bbox.add(env.getMaxX());
                        bbox.add(env.getMaxY());
                        gf.setBbox(bbox);
                    }
                }
            }
        }

        final Map<String,Object> properties = new LinkedHashMap();
        gf.setProperties(properties);
        for (PropertyType pt : type.getProperties(true)) {
            if (AttributeConvention.contains(pt.getName())) continue;
            if (pt instanceof AttributeType) {
                final String name = pt.getName().toString();
                final Object value = feature.getPropertyValue(name);
                properties.put(name, value);
            }
        }

        return gf;
    }

    public GeoJSONFeatureCollection transform(FeatureSet features) throws DataStoreException {
        final FeatureType type = features.getType();

        final GeoJSONFeatureCollection cl = new GeoJSONFeatureCollection();
        final List<GeoJSONFeature> lst = new ArrayList<>();
        try (Stream<Feature> stream = features.features(false)) {
            final Iterator<Feature> ite = stream.iterator();
            while (ite.hasNext()) {
                lst.add(transform(ite.next()));
            }
        }

        cl.setFeatures(lst);

        if (includeTypeOnCollection) {
            cl.setFeatureType(getTypeName(type));
        }
        if (includeCoordRefSysOnCollection) {
            cl.setCoordRefSys(getCoordRefSys(type));
        }
        if (bboxOnCollection) {
            org.opengis.geometry.Envelope env = features.getEnvelope().orElse(null);
            if (env != null) {
                final List<Double> bbox = new ArrayList<>();
                bbox.add(env.getMinimum(0));
                bbox.add(env.getMinimum(1));
                bbox.add(env.getMaximum(0));
                bbox.add(env.getMaximum(1));
                cl.setBbox(bbox);
            }
        }

        return cl;
    }

    public GeoJSONGeometry transform(Geometry geom) throws DataStoreException {

        final GeoJSONGeometry res;
        if (geom instanceof Point cdt) {
            final GeoJSONPoint json = new GeoJSONPoint();
            CoordinateSequence cs = cdt.getCoordinateSequence();
            json.setCoordinates(toList(cs).get(0));
            res = json;
        } else if (geom instanceof LineString cdt) {
            final GeoJSONLineString json = new GeoJSONLineString();
            CoordinateSequence cs = cdt.getCoordinateSequence();
            json.setCoordinates(toList(cs));
            res = json;
        } else if (geom instanceof Polygon cdt) {
            final GeoJSONPolygon json = new GeoJSONPolygon();
            final List<List<List<Double>>> lst = new ArrayList<>();
            lst.add(toList(cdt.getExteriorRing().getCoordinateSequence()));
            for (int i = 0, n = cdt.getNumInteriorRing(); i < n; i++) {
                lst.add(toList(cdt.getInteriorRingN(i).getCoordinateSequence()));
            }
            json.setCoordinates(lst);
            res = json;
        } else if (geom instanceof MultiPoint cdt) {
            final GeoJSONMultiPoint json = new GeoJSONMultiPoint();
            final List<List<Double>> lst = new ArrayList<>();
            for (int i = 0, n = cdt.getNumGeometries(); i < n; i++) {
                lst.add(toList(((Point)cdt.getGeometryN(i)).getCoordinateSequence()).get(0));
            }
            json.setCoordinates(lst);
            res = json;
        } else if (geom instanceof MultiLineString cdt) {
            final GeoJSONMultiLineString json = new GeoJSONMultiLineString();
            final List<List<List<Double>>> lst = new ArrayList<>();
            for (int i = 0, n = cdt.getNumGeometries(); i < n; i++) {
                lst.add(toList(((LineString)cdt.getGeometryN(i)).getCoordinateSequence()));
            }
            json.setCoordinates(lst);
            res = json;
        } else if (geom instanceof MultiPolygon cdt) {
            final GeoJSONMultiPolygon json = new GeoJSONMultiPolygon();
            final List<List<List<List<Double>>>> lst = new ArrayList<>();
            for (int i = 0, n = cdt.getNumGeometries(); i < n; i++) {
                final Polygon pl = (Polygon) cdt.getGeometryN(i);
                final List<List<List<Double>>> sublst = new ArrayList<>();
                sublst.add(toList(pl.getExteriorRing().getCoordinateSequence()));
                for (int j = 0, k = pl.getNumInteriorRing(); j < k; j++) {
                    sublst.add(toList(pl.getInteriorRingN(j).getCoordinateSequence()));
                }
                lst.add(sublst);
            }
            json.setCoordinates(lst);
            res = json;
        } else if (geom instanceof GeometryCollection cdt) {
            final GeoJSONGeometryCollection json = new GeoJSONGeometryCollection();
            final List<GeoJSONGeometry> geometries = new ArrayList<>();
            for (int i = 0, n = cdt.getNumGeometries(); i < n; i++) {
                final Geometry pl = (Geometry) cdt.getGeometryN(i);
                geometries.add(transform(pl));
            }
            json.setGeometries(geometries);
            res = json;
        } else {
            throw new DataStoreException("Geometry not supported yet " + geom);
        }

        if (bboxOnGeometry) {
            Envelope env = geom.getEnvelopeInternal();
            if (env != null && !env.isNull()) {
                final List<Double> bbox = new ArrayList<>();
                bbox.add(env.getMinX());
                bbox.add(env.getMinY());
                bbox.add(env.getMaxX());
                bbox.add(env.getMaxY());
                res.setBbox(bbox);
            }
        }

        if (includeCoordRefSysOnGeometry) {
            Object userData = geom.getUserData();
            if (userData instanceof CoordinateReferenceSystem crs) {
                res.setCoordRefSys(getCoordRefSys(crs));
            }
        }

        return res;
    }

    private List<List<Double>> toList(CoordinateSequence cs) throws DataStoreException {
        final int dim = cs.getDimension();
        final int size = cs.size();
        final List<List<Double>> lst = new ArrayList<>(size);
        switch (dim) {
            case 2 : {
                for (int i = 0; i < size; i++) {
                    lst.add(List.of(cs.getOrdinate(i, 0), cs.getOrdinate(i, 1)));
                }
            } break;
            case 3 : {
                for (int i = 0; i < size; i++) {
                    lst.add(List.of(cs.getOrdinate(i, 0), cs.getOrdinate(i, 1), cs.getOrdinate(i, 2)));
                }
            } break;
            case 4 : {
                for (int i = 0; i < size; i++) {
                    lst.add(List.of(cs.getOrdinate(i, 0), cs.getOrdinate(i, 1), cs.getOrdinate(i, 2), cs.getOrdinate(i, 3)));
                }
            } break;
            default: throw new DataStoreException("Unexpected coordinate sequence dimension " + dim);
        }
        return lst;
    }

    private static String getTypeName(FeatureType type) {
        return type.getName().toString();
    }

    /**
     * Returns the 'coordRefSys' value to use for the given feature type: a plain CRS
     * identifier URI {@link String}, per the common encoding used by real JSON-FG producers
     * (see JSON-FG version 1.0.0, {@code coordrefsys.json}).
     */
    private static Object getCoordRefSys(FeatureType ft) {
        final CoordinateReferenceSystem crs = getCRS(ft);
        return getCoordRefSys(crs);
    }

    private static Object getCoordRefSys(CoordinateReferenceSystem crs) {
        if (crs == null) return null;
        return IdentifiedObjects.getIdentifierOrName(crs);
    }


    /**
     * Extract the coordinate reference system associated to the primary geometry
     * of input data type.
     *
     * @implNote
     * Primary geometry is determined using {@link #getDefaultGeometry(org.opengis.feature.FeatureType) }.
     *
     * @param type The data type to extract reference system from.
     * @return The CRS associated to the default geometry of this data type, or
     * a null value if we cannot determine what is the primary geometry of the
     * data type. Note that a null value is also returned if a geometry property
     * is found, but no CRS characteristics is associated with it.
     */
    private static CoordinateReferenceSystem getCRS(FeatureType type){
        try {
            return getCRS(getDefaultGeometry(type));
        } catch (IllegalArgumentException|IllegalStateException ex) {
            //no default geometry property
            return null;
        }
    }

    /**
     * Extract CRS characteristic if it exist.
     *
     * @param type
     * @return CoordinateReferenceSystem or null
     */
    private static CoordinateReferenceSystem getCRS(PropertyType type){
        return getCharacteristicValue(type, AttributeConvention.CRS, null);
    }

    /**
     * Extract characteristic value if it exist.
     *
     * @param <T> expected value class
     * @param type base type to search in
     * @param charName characteristic name
     * @param defaulValue default value if characteristic is missing or null.
     * @return characteristic value or default value is not found
     */
    private static <T> T getCharacteristicValue(PropertyType type, String charName, T defaulValue){
        while(type instanceof Operation){
            type = (PropertyType) ((Operation)type).getResult();
        }
        if(type instanceof AttributeType){
            final AttributeType at = (AttributeType) ((AttributeType)type).characteristics().get(charName);
            if(at!=null){
                T val = (T) at.getDefaultValue();
                return val==null ? defaulValue : val;
            }
        }
        return defaulValue;
    }

    /**
     * Search for the main geometric property in the given type. We'll search
     * for an SIS convention first (see
     * {@link AttributeConvention#GEOMETRY_PROPERTY}. If no convention is set on
     * the input type, we'll check if it contains a single geometric property.
     * If it's the case, we return it. Otherwise (no or multiple geometries), we
     * throw an exception.
     *
     * @param type The data type to search into.
     * @return The main geometric property we've found.
     * @throws PropertyNotFoundException If no geometric property is available
     * in the given type.
     * @throws IllegalStateException If no convention is set (see
     * {@link AttributeConvention#GEOMETRY_PROPERTY}), and we've found more than
     * one geometry.
     */
    private static PropertyType getDefaultGeometry(final FeatureType type) throws PropertyNotFoundException, IllegalStateException {
        PropertyType geometry;
        try {
            geometry = type.getProperty(AttributeConvention.GEOMETRY);
        } catch (PropertyNotFoundException e) {
            try {
                geometry = searchForGeometry(type);
            } catch (RuntimeException e2) {
                e2.addSuppressed(e);
                throw e2;
            }
        }
        return geometry;
    }

    /**
     * Search for a geometric attribute outside SIS conventions. More accurately,
     * we expect the given type to have a single geometry attribute. If many are
     * found, an exception is thrown.
     *
     * @param type The data type to search into.
     * @return The only geometric property we've found.
     * @throws PropertyNotFoundException If no geometric property is available in
     * the given type.
     * @throws IllegalStateException If we've found more than one geometry.
     */
    private static PropertyType searchForGeometry(final FeatureType type) throws PropertyNotFoundException, IllegalStateException {
        final Predicate<IdentifiedType> isNotConvention = p -> !AttributeConvention.contains(p.getName());
        final List<? extends PropertyType> geometries = type.getProperties(true).stream()
                .filter(isNotConvention)
                .filter(AttributeConvention::isGeometryAttribute)
                .collect(Collectors.toList());

        if (geometries.size() < 1) {
            throw new PropertyNotFoundException("No geometric property can be found outside of sis convention.");
        } else if (geometries.size() > 1) {
            throw new IllegalStateException("Multiple geometries found. We don't know which one to select.");
        } else {
            return geometries.get(0);
        }
    }

}
