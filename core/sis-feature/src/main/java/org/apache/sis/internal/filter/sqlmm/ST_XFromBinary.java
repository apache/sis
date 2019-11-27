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
package org.apache.sis.internal.filter.sqlmm;

import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.builder.PropertyTypeBuilder;
import org.apache.sis.internal.filter.FilterGeometryUtils;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.FeatureType;
import org.opengis.filter.expression.Expression;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @version 2.0
 * @since 2.0
 * @module
 */
abstract class ST_XFromBinary extends AbstractSpatialFunction {

    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 8111450596023441499L;

    public ST_XFromBinary(Expression... parameters) {
        super(parameters);
    }

    @Override
    protected int getMinParams() {
        return 1;
    }

    @Override
    protected int getMaxParams() {
        return 2;
    }

    protected abstract Class getExpectedClass();

    public String getSyntax() {
        return getName() + " ( <wkb> [,<srid>] )";
    }

    @Override
    public Geometry evaluate(Object candidate) {

        Geometry geom = FilterGeometryUtils.toGeometry(candidate, parameters.get(0));

        if (!getExpectedClass().isInstance(geom)) {
            warning(new Exception("WKB is not of expected type : " + getExpectedClass().getSimpleName()));
            return null;
        }

        if (parameters.size() > 1) {
            //srid
            geom.setSRID(((Number) parameters.get(1).evaluate(candidate)).intValue());
        }

        return geom;
    }

    @Override
    public PropertyTypeBuilder expectedType(FeatureType valueType, FeatureTypeBuilder addTo) {
        return addTo.addAttribute(getExpectedClass()).setName(getName());
    }

    /**
     * SQL/MM, ISO/IEC 13249-3:2011, ST_BdMPolyFromWKB. <br>
     * Return an ST_MultiPolygon value which is transformed from a BINARY LARGE
     * OBJECT value that represents the well-known binary representation of an
     * ST_MultiLineString value.
     */
    public static final class BdMPoly extends ST_XFromBinary {

        public static final String NAME = "ST_BdMPolyFromWKB";
        private static final long serialVersionUID = -5763967468534517956L;

        public BdMPoly(Expression[] parameters) {
            super(parameters);
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        protected Class<MultiLineString> getExpectedClass() {
            return MultiLineString.class;
        }

        @Override
        public Geometry evaluate(Object candidate) {
            final MultiLineString mls = (MultiLineString) FilterGeometryUtils.toGeometry(candidate, parameters.get(0));

            final LinearRing exterior;
            final LinearRing[] interiors = new LinearRing[mls.getNumGeometries() - 1];

            exterior = FilterGeometryUtils.GF.createLinearRing(mls.getGeometryN(0).getCoordinates());
            for (int i = 0; i < interiors.length; i++) {
                interiors[i] = FilterGeometryUtils.GF.createLinearRing(mls.getGeometryN(i + 1).getCoordinates());
            }

            final Polygon poly = FilterGeometryUtils.GF.createPolygon(exterior, interiors);
            final MultiPolygon mpoly = FilterGeometryUtils.GF.createMultiPolygon(new Polygon[]{poly});
            mpoly.setSRID(mls.getSRID());
            return mpoly;
        }
    }

    /**
     * SQL/MM, ISO/IEC 13249-3:2011, ST_BdPolyFromWKB. <br>
     * Return an ST_Polygon value which is transformed from a BINARY LARGE
     * OBJECT value that represents the well-known binary representation of an
     * ST_Polygon value.
     */
    public static final class BdPoly extends ST_XFromBinary {

        public static final String NAME = "ST_BdPolyFromWKB";
        private static final long serialVersionUID = 6661949194256216330L;

        public BdPoly(Expression[] parameters) {
            super(parameters);
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        protected Class<MultiLineString> getExpectedClass() {
            return MultiLineString.class;
        }

        @Override
        public Geometry evaluate(Object candidate) {
            final MultiLineString mls = (MultiLineString) FilterGeometryUtils.toGeometry(candidate, parameters.get(0));

            final LinearRing exterior;
            final LinearRing[] interiors = new LinearRing[mls.getNumGeometries() - 1];

            exterior = FilterGeometryUtils.GF.createLinearRing(mls.getGeometryN(0).getCoordinates());
            for (int i = 0; i < interiors.length; i++) {
                interiors[i] = FilterGeometryUtils.GF.createLinearRing(mls.getGeometryN(i + 1).getCoordinates());
            }

            final Polygon poly = FilterGeometryUtils.GF.createPolygon(exterior, interiors);
            poly.setSRID(mls.getSRID());
            return poly;
        }
    }

    /**
     * SQL/MM, ISO/IEC 13249-3:2011, ST_GeomCollFromWKB. <br>
     * Return an ST_GeomCollection value which is transformed from a BINARY
     * LARGE OBJECT value that represents the well-known binary representation
     * of an ST_GeomCollection value.
     */
    public static final class GeomColl extends ST_XFromBinary {

        public static final String NAME = "ST_GeomCollFromWKB";
        private static final long serialVersionUID = -1229981535753136679L;

        public GeomColl(Expression[] parameters) {
            super(parameters);
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        protected Class<GeometryCollection> getExpectedClass() {
            return GeometryCollection.class;
        }
    }

    /**
     * SQL/MM, ISO/IEC 13249-3:2011, ST_GeomFromWKB. <br>
     * Return an ST_Geometry value which is transformed from a BINARY LARGE
     * OBJECT value that represents the well-known binary representation of an
     * ST_Geometry value.
     */
    public static final class Geom extends ST_XFromBinary {

        public static final String NAME = "ST_GeomFromWKB";
        private static final long serialVersionUID = -1428672188136390056L;

        public Geom(Expression[] parameters) {
            super(parameters);
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        protected Class<Geometry> getExpectedClass() {
            return Geometry.class;
        }
    }

    /**
     * SQL/MM, ISO/IEC 13249-3:2011, ST_LineFromWKB. <br>
     * Return an ST_LineString value which is transformed from a BINARY LARGE
     * OBJECT value that represents the well-known binary representation of an
     * ST_LineString value.
     */
    public static final class Line extends ST_XFromBinary {

        public static final String NAME = "ST_LineFromWKB";
        private static final long serialVersionUID = -4525486088596088088L;

        public Line(Expression[] parameters) {
            super(parameters);
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        protected Class<LineString> getExpectedClass() {
            return LineString.class;
        }
    }

    /**
     * SQL/MM, ISO/IEC 13249-3:2011, ST_MPointFromWKB. <br>
     * Return an ST_MultiLineString value which is transformed from a BINARY
     * LARGE OBJECT value that represents the well-known binary representation
     * of an ST_MultiLineString value.
     */
    public static final class MLine extends ST_XFromBinary {

        public static final String NAME = "ST_MLineFromWKB";
        private static final long serialVersionUID = 1733897936536015735L;

        public MLine(Expression[] parameters) {
            super(parameters);
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        protected Class<MultiLineString> getExpectedClass() {
            return MultiLineString.class;
        }
    }

    /**
     * SQL/MM, ISO/IEC 13249-3:2011, ST_MPointFromWKB. <br>
     * Return an ST_MultiPoint value which is transformed from a BINARY LARGE
     * OBJECT value that represents the well-known binary representation of an
     * ST_MultiPoint value.
     */
    public static final class MPoint extends ST_XFromBinary {

        public static final String NAME = "ST_MPointFromWKB";
        private static final long serialVersionUID = 1725448922596814788L;

        public MPoint(Expression[] parameters) {
            super(parameters);
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        protected Class<MultiPoint> getExpectedClass() {
            return MultiPoint.class;
        }
    }

    /**
     * SQL/MM, ISO/IEC 13249-3:2011, ST_MPolyFromWKB. <br>
     * Return an ST_MultiPolygon value which is transformed from a BINARY LARGE
     * OBJECT value that represents the well-known binary representation of an
     * ST_MultiPolygon value.
     */
    public static final class MPoly extends ST_XFromBinary {

        public static final String NAME = "ST_MPolyFromWKB";
        private static final long serialVersionUID = 8180295093331098160L;

        public MPoly(Expression[] parameters) {
            super(parameters);
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        protected Class<MultiPolygon> getExpectedClass() {
            return MultiPolygon.class;
        }
    }

    /**
     * SQL/MM, ISO/IEC 13249-3:2011, ST_PointFromWKB. <br>
     * Return an ST_Point value which is transformed from a BINARY LARGE OBJECT
     * value that represents the well-known binary representation of an ST_Point
     * value.
     */
    public static final class Point extends ST_XFromBinary {

        public static final String NAME = "ST_PointFromWKB";
        private static final long serialVersionUID = 6307946926253919188L;

        public Point(Expression[] parameters) {
            super(parameters);
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        protected Class<org.locationtech.jts.geom.Point> getExpectedClass() {
            return org.locationtech.jts.geom.Point.class;
        }
    }

    /**
     * SQL/MM, ISO/IEC 13249-3:2011, ST_PolyFromWKB. <br>
     * Return an ST_Polygon value which is transformed from a BINARY LARGE
     * OBJECT value that represents the well-known binary representation of an
     * ST_Polygon value.
     */
    public static final class Poly extends ST_XFromBinary {

        public static final String NAME = "ST_PolyFromWKB";
        private static final long serialVersionUID = 3729779304169979534L;

        public Poly(Expression[] parameters) {
            super(parameters);
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        protected Class<Polygon> getExpectedClass() {
            return Polygon.class;
        }
    }

}
