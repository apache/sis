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

import java.util.List;
import java.util.Map.Entry;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.CoordinateXYZM;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.internal.shared.AttributeConvention;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.geojson.binding.GeoJSONFeature;
import org.apache.sis.storage.geojson.binding.GeoJSONGeometry;
import org.apache.sis.storage.geojson.binding.GeoJSONGeometryCollection;
import org.apache.sis.storage.geojson.binding.GeoJSONLineString;
import org.apache.sis.storage.geojson.binding.GeoJSONMultiLineString;
import org.apache.sis.storage.geojson.binding.GeoJSONMultiPoint;
import org.apache.sis.storage.geojson.binding.GeoJSONMultiPolygon;
import org.apache.sis.storage.geojson.binding.GeoJSONPoint;
import org.apache.sis.storage.geojson.binding.GeoJSONPolygon;

// Specific to the main branch:
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.DefaultFeatureType;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class GeoJSON {

    private static final GeometryFactory GF = new GeometryFactory();

    private GeoJSON() {

    }

    public static AbstractFeature fromGeoJSON(GeoJSONFeature json, DefaultFeatureType type) throws DataStoreException {

        final GeoJSONGeometry jsongeom = json.getGeometry();
        Geometry geom = null;
        if (jsongeom != null) {
            geom = fromGeoJSON(jsongeom);
        }

        if (type == null) {
            final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
            ftb.setName("geojson");

            if (json.getId() != null) {
                ftb.addAttribute(json.getId().getClass()).setName(AttributeConvention.IDENTIFIER_PROPERTY).addRole(AttributeRole.IDENTIFIER_COMPONENT);
            }
            if (geom != null) {
                ftb.addAttribute(geom.getClass()).setName(AttributeConvention.GEOMETRY_PROPERTY).addRole(AttributeRole.DEFAULT_GEOMETRY);
            }
            for (Entry<String,Object> entry : json.getProperties().entrySet()) {
                ftb.addAttribute(entry.getValue().getClass()).setName(entry.getKey());
            }
            type = ftb.build();
        }

        final AbstractFeature feature = type.newInstance();

        if (json.getId() != null) {
            feature.setPropertyValue(AttributeConvention.IDENTIFIER, json.getId());
        }

        if (geom != null) {
            feature.setPropertyValue(AttributeConvention.GEOMETRY, geom);
        }

        for (Entry<String,Object> entry : json.getProperties().entrySet()) {
            feature.setPropertyValue(entry.getKey(), entry.getValue());
        }
        return feature;
    }

    public static Geometry fromGeoJSON(GeoJSONGeometry geom) throws DataStoreException {
        if (geom instanceof GeoJSONPoint json) {
            return GF.createPoint(toCoordinate(json.getCoordinates()));
        } else if (geom instanceof GeoJSONLineString json) {
            return GF.createLineString(toCoordinates(json.getCoordinates()));
        } else if (geom instanceof GeoJSONPolygon json) {
            return toPolygon(json.getCoordinates());
        } else if (geom instanceof GeoJSONMultiPoint json) {
            final List<List<Double>> coordinates = json.getCoordinates();
            final Point[] points = new Point[coordinates.size()];
            for (int i = 0; i < points.length; i++) {
                points[i] = GF.createPoint(toCoordinate(coordinates.get(i)));
            }
            return GF.createMultiPoint(points);
        } else if (geom instanceof GeoJSONMultiLineString json) {
            final List<List<List<Double>>> coordinates = json.getCoordinates();
            final LineString[] lines = new LineString[coordinates.size()];
            for (int i = 0; i < lines.length; i++) {
                lines[i] = GF.createLineString(toCoordinates(coordinates.get(i)));
            }
            return GF.createMultiLineString(lines);
        } else if (geom instanceof GeoJSONMultiPolygon json) {
            final List<List<List<List<Double>>>> coordinates = json.getCoordinates();
            final Polygon[] polygons = new Polygon[coordinates.size()];
            for (int i = 0; i < polygons.length; i++) {
                polygons[i] = toPolygon(coordinates.get(i));
            }
            return GF.createMultiPolygon(polygons);
        } else if (geom instanceof GeoJSONGeometryCollection json) {
            final List<GeoJSONGeometry> geometries = json.getGeometries();
            final Geometry[] children = new Geometry[geometries.size()];
            for (int i = 0; i < children.length; i++) {
                children[i] = fromGeoJSON(geometries.get(i));
            }
            return GF.createGeometryCollection(children);
        } else {
            throw new DataStoreException("Geometry not supported " + geom);
        }
    }

    private static Polygon toPolygon(List<List<List<Double>>> rings) throws DataStoreException {
        final LinearRing shell = GF.createLinearRing(toCoordinates(rings.get(0)));
        final LinearRing[] holes = new LinearRing[rings.size() - 1];
        for (int i = 0; i < holes.length; i++) {
            holes[i] = GF.createLinearRing(toCoordinates(rings.get(i + 1)));
        }
        return GF.createPolygon(shell, holes);
    }

    private static Coordinate[] toCoordinates(List<List<Double>> positions) throws DataStoreException {
        final Coordinate[] coords = new Coordinate[positions.size()];
        for (int i = 0; i < coords.length; i++) {
            coords[i] = toCoordinate(positions.get(i));
        }
        return coords;
    }

    private static Coordinate toCoordinate(List<Double> coordinates) throws DataStoreException {
        final int size = coordinates.size();
        switch (size) {
            case 2 : return new CoordinateXY(coordinates.get(0), coordinates.get(1));
            case 3 : return new Coordinate(coordinates.get(0), coordinates.get(1), coordinates.get(2));
            case 4 : return new CoordinateXYZM(coordinates.get(0), coordinates.get(1), coordinates.get(2), coordinates.get(3));
            default : throw new DataStoreException("Geometry " + size + "not supported");
        }
    }

}
