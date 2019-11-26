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
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
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
abstract class ST_XFromGML extends AbstractSpatialFunction {

    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 6113283783777989678L;

    public ST_XFromGML(Expression... parameters) {
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
        return getName() + " ( <gml> [,<srid>] )";
    }

    @Override
    public Geometry evaluate(Object candidate) {

        final Object obj = parameters.get(0).evaluate(candidate);

        final Geometry geom;
        if (true) throw new RuntimeException("GML not supported yet");

        if (!getExpectedClass().isInstance(geom)) {
            warning(new Exception("GML is not of expected type : " + getExpectedClass().getSimpleName()));
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
     * SQL/MM, ISO/IEC 13249-3:2011, ST_GeomCollFromGML. <br>
     * Return an ST_GeomCollection value which is transformed from a CHARACTER
     * LARGE OBJECT value that represents the GML representation of an
     * ST_GeomCollection value.
     */
    public static final class GeomColl extends ST_XFromGML {

        public static final String NAME = "ST_GeomCollFromGML";
        private static final long serialVersionUID = 2729025080212643888L;

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
     * SQL/MM, ISO/IEC 13249-3:2011, ST_GeomFromGML. <br>
     * Return an ST_Geometry value which is transformed from a CHARACTER LARGE
     * OBJECT value that represents the GML representation of an ST_Geometry.     *
     */
    public static final class Geom extends ST_XFromGML {

        public static final String NAME = "ST_GeomFromGML";
        private static final long serialVersionUID = -2967969861402168905L;

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
     * SQL/MM, ISO/IEC 13249-3:2011, ST_LineFromGML. <br>
     * Return an ST_LineString value which is transformed from a CHARACTER LARGE
     * OBJECT value that represents the GML representation of an ST_LineString
     * value.
     */
    public static final class Line extends ST_XFromGML {

        public static final String NAME = "ST_LineFromGML";
        private static final long serialVersionUID = 2939373456076304217L;

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
     * SQL/MM, ISO/IEC 13249-3:2011, ST_MPointFromGML. <br>
     * Return an ST_MultiLineString value which is transformed from a CHARACTER
     * LARGE OBJECT value that represents the GML representation of an
     * ST_MultiLineString value.
     */
    public static final class MLine extends ST_XFromGML {

        public static final String NAME = "ST_MLineFromGML";
        private static final long serialVersionUID = -6449667109261980974L;

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
     * SQL/MM, ISO/IEC 13249-3:2011, ST_MPointFromGML. <br>
     * Return an ST_MultiPoint value which is transformed from a CHARACTER LARGE
     * OBJECT value that represents the GML representation of an ST_MultiPoint
     * value.
     */
    public static final class MPoint extends ST_XFromGML {

        public static final String NAME = "ST_MPointFromGML";
        private static final long serialVersionUID = 268946482386292257L;

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
     * SQL/MM, ISO/IEC 13249-3:2011, ST_MPolyFromGML. <br>
     * Return an ST_MultiPolygon value which is transformed from a CHARACTER
     * LARGE OBJECT value that represents the GML representation of an
     * ST_MultiPolygon value.
     */
    public static final class MPoly extends ST_XFromGML {

        public static final String NAME = "ST_MPolyFromGML";
        private static final long serialVersionUID = 3505318747927007728L;

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
     * SQL/MM, ISO/IEC 13249-3:2011, ST_PointFromGML. <br>
     * Return an ST_Point value which is transformed from a CHARACTER LARGE
     * OBJECT value that represents the GML representation of an ST_Point.
     */
    public static final class Point extends ST_XFromGML {

        public static final String NAME = "ST_PointFromGML";
        private static final long serialVersionUID = -1032773543624102657L;

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
     * SQL/MM, ISO/IEC 13249-3:2011, ST_PolyFromGML. <br>
     * Return an ST_Polygon value which is transformed from a CHARACTER LARGE
     * OBJECT value that represents the GML representation of an ST_Polygon
     * value.
     */
    public static final class Poly extends ST_XFromGML {

        public static final String NAME = "ST_PolyFromGML";
        private static final long serialVersionUID = 7305807862301697831L;

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
