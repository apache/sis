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
abstract class ST_XFromText extends AbstractSpatialFunction {

    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -9046918207193451904L;

    public ST_XFromText(Expression... parameters) {
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

    @Override
    public String getSyntax() {
        return getName() + " ( <text> [,<srid>] )";
    }

    @Override
    public Geometry evaluate(Object candidate) {

        final Geometry geom = FilterGeometryUtils.toGeometry(candidate, parameters.get(0));

        if (!getExpectedClass().isInstance(geom)) {
            warning(new Exception("WKT is not of expected type : " + getExpectedClass().getSimpleName()));
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
     * SQL/MM, ISO/IEC 13249-3:2011, ST_BdMPolyFromText. <br>
     * Return an ST_MultiPolygon value which is transformed from a CHARACTER
     * LARGE OBJECT value that represents the well-known text representation of
     * an ST_MultiLineString value.
     */
    public static final class BdMPoly extends ST_XFromText {

        public static final String NAME = "ST_BdMPolyFromText";
        private static final long serialVersionUID = 9014831950657428233L;

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
     * SQL/MM, ISO/IEC 13249-3:2011, ST_BdPolyFromText. <br>
     * Return an ST_Polygon value which is transformed from a CHARACTER LARGE
     * OBJECT value that represents the well-known text representation of an
     * ST_MultiLineString value.
     */
    public static final class BdPoly extends ST_XFromText {

        public static final String NAME = "ST_BdPolyFromText";
        private static final long serialVersionUID = -5743188370723944952L;

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
     * SQL/MM, ISO/IEC 13249-3:2011, ST_GeomCollFromText. <br>
     * Return an ST_GeomCollection value which is transformed from a CHARACTER
     * LARGE OBJECT value that represents the well-known text representation of
     * an ST_GeomCollection value.
     */
    public static final class GeomColl extends ST_XFromText {

        public static final String NAME = "ST_GeomCollFromText";
        private static final long serialVersionUID = 8816234645129212298L;

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
     * SQL/MM, ISO/IEC 13249-3:2011, ST_GeomFromText. <br>
     * Return an ST_Geometry value which is transformed from a CHARACTER LARGE
     * OBJECT value that represents the well-known text representation of an
     * ST_Geometry value.
     */
    public static final class Geom extends ST_XFromText {

        public static final String NAME = "ST_GeomFromText";
        private static final long serialVersionUID = -1627156125717985980L;

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
     * SQL/MM, ISO/IEC 13249-3:2011, ST_LineFromText. <br>
     * Return an ST_LineString value which is transformed from a CHARACTER LARGE
     * OBJECT value that represents the well-known text representation of an
     * ST_LineString value.
     */
    public static final class Line extends ST_XFromText {

        public static final String NAME = "ST_LineFromText";
        private static final long serialVersionUID = -6384733422127389790L;

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
     * SQL/MM, ISO/IEC 13249-3:2011, ST_MLineFromText. <br>
     * Return an ST_MultiLineString value which is transformed from a CHARACTER
     * LARGE OBJECT value that represents the well-known text representation of
     * an ST_MultiLineString value.
     */
    public static final class MLine extends ST_XFromText {

        public static final String NAME = "ST_MLineFromText";
        private static final long serialVersionUID = -8797802130317419443L;

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
     * SQL/MM, ISO/IEC 13249-3:2011, ST_MPointFromText. <br>
     * Return an ST_MultiPoint value which is transformed from a CHARACTER LARGE
     * OBJECT value that represents the well-known text representation of an
     * ST_MultiPoint value.
     */
    public static final class MPoint extends ST_XFromText {

        public static final String NAME = "ST_MPointFromText";
        private static final long serialVersionUID = 5037254027906574270L;

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
     * SQL/MM, ISO/IEC 13249-3:2011, ST_MPolyFromText. <br>
     * Return an ST_MultiPolygon value which is transformed from a CHARACTER
     * LARGE OBJECT value that represents the well-known text representation of
     * an ST_MultiPolygon value.
     */
    public static final class MPoly extends ST_XFromText {

        public static final String NAME = "ST_MPolyFromText";
        private static final long serialVersionUID = 5914267165283411013L;

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
     * SQL/MM, ISO/IEC 13249-3:2011, ST_PointFromText. <br>
     * Return an ST_Point value which is transformed from a CHARACTER LARGE
     * OBJECT value that represents the well-known text representation of an
     * ST_Point value.
     */
    public static final class Point extends ST_XFromText {

        public static final String NAME = "ST_PointFromText";
        private static final long serialVersionUID = -5265959153677337532L;

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
     * SQL/MM, ISO/IEC 13249-3:2011, ST_PolyFromText. <br>
     * Return an ST_Polygon value which is transformed from a CHARACTER LARGE
     * OBJECT value that represents the well-known text representation of an
     * ST_Polygon value.
     */
    public static final class Poly extends ST_XFromText {

        public static final String NAME = "ST_PolyFromText";
        private static final long serialVersionUID = -2502891009213433036L;

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
