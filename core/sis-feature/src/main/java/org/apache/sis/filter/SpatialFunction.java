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
package org.apache.sis.filter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.math.Fraction;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.collection.BackingStoreException;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.opengis.feature.Feature;
import org.opengis.feature.PropertyNotFoundException;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.BinarySpatialOperator;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 *
 * @author  Johann Sorel (Geomatys)
 * @version 2.0
 * @since   2.0
 * @module
 */
abstract class SpatialFunction extends BinaryFunction implements BinarySpatialOperator {

    private static final LinearRing[] EMPTY_RINGS = new LinearRing[0];
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private static final PreparedGeometryFactory PREPARED_FACTORY = new PreparedGeometryFactory();

    private static CoordinateReferenceSystem MERCATOR;

    private static CoordinateReferenceSystem getMercator() throws FactoryException {
        if (MERCATOR == null) {
            MERCATOR = CRS.forCode("EPSG:3395");
        }
        return MERCATOR;
    }

    protected SpatialFunction(Expression expression1, Expression expression2) {
        super(expression1, expression2);
    }

    @Override
    protected Number applyAsLong(long left, long right) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    protected Number applyAsDouble(double left, double right) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    protected Number applyAsFraction(Fraction left, Fraction right) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    protected Number applyAsInteger(BigInteger left, BigInteger right) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    protected Number applyAsDecimal(BigDecimal left, BigDecimal right) {
        throw new UnsupportedOperationException("Not supported.");
    }

    private static Geometry toGeometry(final Object object, Expression exp) {
        Object value = null;
        if ((exp instanceof PropertyName) && object instanceof Feature && ((PropertyName) exp).getPropertyName().isEmpty()) {
            //Search for a default geometry.
            try {
                value = ((Feature) object).getPropertyValue(AttributeConvention.GEOMETRY_PROPERTY.toString());
            } catch (PropertyNotFoundException ex) {
                //no defined default geometry
            }
        } else {
            value = exp.evaluate(object);
        }

        Geometry candidate;
        if (value instanceof GridCoverage) {
            candidate = (Geometry) value;
        } else if (value instanceof GridCoverage) {
            //use the coverage envelope
            final GridCoverage coverage = (GridCoverage) value;
            candidate = toGeometry(coverage.getGridGeometry().getEnvelope());
        } else {
            try {
                candidate = ObjectConverters.convert(value, Geometry.class);
            } catch (UnconvertibleObjectException ex) {
                //cound not convert value to a Geometry
                candidate = null;
            }
        }
        return candidate;
    }

    /**
     * Envelope to geometry.
     */
    private static Polygon toGeometry(final Envelope env) {
        final Coordinate[] coordinates = new Coordinate[]{
            new Coordinate(env.getMinimum(0), env.getMinimum(1)),
            new Coordinate(env.getMinimum(0), env.getMaximum(1)),
            new Coordinate(env.getMaximum(0), env.getMaximum(1)),
            new Coordinate(env.getMaximum(0), env.getMinimum(1)),
            new Coordinate(env.getMinimum(0), env.getMinimum(1))};
        final LinearRing ring = GEOMETRY_FACTORY.createLinearRing(coordinates);
        Polygon polygon = GEOMETRY_FACTORY.createPolygon(ring, new LinearRing[0]);
        polygon.setUserData(env.getCoordinateReferenceSystem());
        return polygon;
    }

    /**
     * Envelope to prepared geometry.
     */
    private static PreparedGeometry toPreparedGeometry(final Envelope env){
        double minX = env.getMinimum(0);
        double minY = env.getMinimum(1);
        double maxX = env.getMaximum(0);
        double maxY = env.getMaximum(1);
        if (Double.isNaN(minX) || Double.isInfinite(minX)) minX = Double.MIN_VALUE;
        if (Double.isNaN(minY) || Double.isInfinite(minY)) minY = Double.MIN_VALUE;
        if (Double.isNaN(maxX) || Double.isInfinite(maxX)) maxX = Double.MAX_VALUE;
        if (Double.isNaN(maxY) || Double.isInfinite(maxY)) maxY = Double.MAX_VALUE;

        final Coordinate[] coords = new Coordinate[5];
        coords[0] = new Coordinate(minX, minY);
        coords[1] = new Coordinate(minX, maxY);
        coords[2] = new Coordinate(maxX, maxY);
        coords[3] = new Coordinate(maxX, minY);
        coords[4] = new Coordinate(minX, minY);
        final LinearRing ring = GEOMETRY_FACTORY.createLinearRing(coords);
        Geometry geom = GEOMETRY_FACTORY.createPolygon(ring, EMPTY_RINGS);
        return PREPARED_FACTORY.create(geom);
    }

    /**
     * Reproject geometries to the same CRS if needed and if possible.
     */
    private static Geometry[] toSameCRS(final Geometry leftGeom, final Geometry rightGeom)
            throws NoSuchAuthorityCodeException, FactoryException, TransformException {

        final CoordinateReferenceSystem leftCRS = Geometries.getCoordinateReferenceSystem(leftGeom);
        final CoordinateReferenceSystem rightCRS = Geometries.getCoordinateReferenceSystem(rightGeom);

        if (leftCRS == null || rightCRS == null) {
            //one or both geometries doesn't have a defined CRS, we assume that both
            //are in the same CRS
            return new Geometry[]{leftGeom, rightGeom};
        } else if (Utilities.equalsIgnoreMetadata(leftCRS, rightCRS)) {
            //both are in the same CRS, nothing to reproject
            return new Geometry[]{leftGeom, rightGeom};
        }

        //we choose to reproject the right operand.
        //there is no special reason to make this choice but we must make one.
        //perhaps there could be a way to determine a best crs ?
        final CoordinateOperation trs = CRS.findOperation(rightCRS, leftCRS, null);

        return new Geometry[]{leftGeom, (Geometry) Geometries.transform(rightGeom, trs)};
    }

    /**
     * Reproject one or both geometries to the same crs, the matching crs will
     * be compatible with the requested unit. return Array[leftGeometry,
     * rightGeometry, matchingCRS];
     */
    private static Object[] toSameCRS(final Geometry leftGeom, final Geometry rightGeom, final Unit unit)
            throws NoSuchAuthorityCodeException, FactoryException, TransformException {

        final CoordinateReferenceSystem leftCRS = Geometries.getCoordinateReferenceSystem(leftGeom);
        final CoordinateReferenceSystem rightCRS = Geometries.getCoordinateReferenceSystem(rightGeom);

        if (leftCRS == null && rightCRS == null) {
            //bother geometries doesn't have a defined SRID, we assume that both
            //are in the same CRS
            return new Object[]{leftGeom, rightGeom, null};
        } else if (leftCRS == null || rightCRS == null || Utilities.equalsIgnoreMetadata(leftCRS, rightCRS)) {
            //both are in the same CRS

            final CoordinateReferenceSystem geomCRS = (leftCRS == null) ? rightCRS : leftCRS;

            if (geomCRS.getCoordinateSystem().getAxis(0).getUnit().isCompatible(unit)) {
                //the geometries crs is compatible with the requested unit, nothing to reproject
                return new Object[]{leftGeom, rightGeom, geomCRS};
            } else {
                //the crs unit is not compatible, we must reproject both geometries to a more appropriate crs
                if (Units.METRE.isCompatible(unit)) {
                    //in that case we reproject to mercator EPSG:3395
                    final CoordinateReferenceSystem mercator = getMercator();
                    final CoordinateOperation trs = CRS.findOperation(geomCRS, mercator, null);

                    return new Object[]{
                        Geometries.transform(leftGeom, trs),
                        Geometries.transform(rightGeom, trs),
                        mercator};

                } else {
                    //we can not find a matching projection in this case
                    throw new TransformException("Could not find a matching CRS for both geometries for unit :" + unit);
                }
            }

        } else {
            //both have different CRS, try to find the most appropriate crs amoung both

            final CoordinateReferenceSystem matchingCRS;
            final Object leftMatch;
            final Object rightMatch;

            if (leftCRS.getCoordinateSystem().getAxis(0).getUnit().isCompatible(unit)) {
                matchingCRS = leftCRS;
                final CoordinateOperation trs = CRS.findOperation(rightCRS, matchingCRS, null);
                rightMatch = Geometries.transform(rightGeom, trs);
                leftMatch = leftGeom;
            } else if (rightCRS.getCoordinateSystem().getAxis(0).getUnit().isCompatible(unit)) {
                matchingCRS = rightCRS;
                final CoordinateOperation trs = CRS.findOperation(leftCRS, matchingCRS, null);
                leftMatch = Geometries.transform(leftGeom, trs);
                rightMatch = rightGeom;
            } else {
                //the crs unit is not compatible, we must reproject both geometries to a more appropriate crs
                if (Units.METRE.isCompatible(unit)) {
                    //in that case we reproject to mercator EPSG:3395
                    matchingCRS = getMercator();

                    CoordinateOperation trs = CRS.findOperation(leftCRS, matchingCRS, null);
                    leftMatch = Geometries.transform(leftGeom, trs);
                    trs = CRS.findOperation(rightCRS, matchingCRS, null);
                    rightMatch = Geometries.transform(rightGeom, trs);

                } else {
                    //we can not find a matching projection in this case
                    throw new TransformException("Could not find a matching CRS for both geometries for unit :" + unit);
                }
            }

            return new Object[]{leftMatch, rightMatch, matchingCRS};
        }
    }


    /**
     * The {@value #NAME} filter.
     */
    static final class BBOX extends SpatialFunction implements org.opengis.filter.spatial.BBOX {

        //cache the bbox geometry
        private transient PreparedGeometry boundingGeometry;
        private final org.locationtech.jts.geom.Envelope boundingEnv;
        private final CoordinateReferenceSystem crs;

        private final Envelope env;

        BBOX(Expression expression, Envelope env) {
            super(expression, new LeafExpression.Literal(env));
            this.env = env;
            boundingGeometry = toPreparedGeometry(env);
            boundingEnv = boundingGeometry.getGeometry().getEnvelopeInternal();
            final CoordinateReferenceSystem crsFilter = env.getCoordinateReferenceSystem();
            if (crsFilter != null) {
                this.crs = crsFilter;
            } else {
                // In CQL if crs is not specified, it is EPSG:4326
                this.crs = CommonCRS.WGS84.normalizedGeographic();
            }
        }

        private PreparedGeometry getPreparedGeometry(){
            if (boundingGeometry == null) {
                boundingGeometry = toPreparedGeometry(env);
            }
            return boundingGeometry;
        }

        @Override
        protected String getName() {
            return NAME;
        }

        @Override
        public String getPropertyName() {
            if (expression1 instanceof PropertyName) {
                return ((PropertyName) expression1).getPropertyName();
            }
            return null;
        }

        @Override
        public String getSRS() {
            try {
                return IdentifiedObjects.lookupURN(env.getCoordinateReferenceSystem(), null);
            } catch (FactoryException ex) {
                throw new BackingStoreException(ex.getMessage(), ex);
            }
        }

        @Override
        public double getMinX() {
            return env.getMinimum(0);
        }

        @Override
        public double getMinY() {
            return env.getMinimum(1);
        }

        @Override
        public double getMaxX() {
            return env.getMaximum(0);
        }

        @Override
        public double getMaxY() {
            return env.getMaximum(1);
        }

        @Override
        public boolean evaluate(Object object) {
            Geometry candidate = toGeometry(object, expression1);

            if (candidate == null) {
                return false;
            }

            //we don't know in which crs it is, try to find it
            CoordinateReferenceSystem candidateCrs = null;
            try {
                candidateCrs = Geometries.getCoordinateReferenceSystem(candidate);
            } catch (FactoryException ex) {
                warning(ex);
            }

            //if we don't know the crs, we will assume it's the objective crs already
            if (candidateCrs != null) {
                //reproject in objective crs if needed
                if (!Utilities.equalsIgnoreMetadata(this.crs, candidateCrs)) {
                    try {
                        candidate = (Geometry) Geometries.transform(candidate, CRS.findOperation(candidateCrs, this.crs, null));
                    } catch (MismatchedDimensionException | TransformException | FactoryException ex) {
                        warning(ex);
                        return false;
                    }
                }
            }

            final org.locationtech.jts.geom.Envelope candidateEnv = candidate.getEnvelopeInternal();

            if (boundingEnv.contains(candidateEnv) || candidateEnv.contains(boundingEnv)) {
                return true;
            } else if (boundingEnv.intersects(candidateEnv)) {
                return getPreparedGeometry().intersects(candidate);
            } else {
                return false;
            }
        }

        @Override
        public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }

    /**
     * The {@value #NAME} filter.
     */
    static final class Beyond extends SpatialFunction implements org.opengis.filter.spatial.Beyond {

        private final double distance;
        private final String units;
        private final Unit unit;

        Beyond(Expression expression1, Expression expression2, double distance, String units) {
            super(expression1, expression2);
            this.distance = distance;
            this.units = units;
            this.unit = Units.valueOf(units);
        }

        @Override
        protected String getName() {
            return NAME;
        }

        @Override
        public double getDistance() {
            return distance;
        }

        @Override
        public String getDistanceUnits() {
            return units;
        }

        @Override
        public boolean evaluate(Object object) {
            final Geometry leftGeom = toGeometry(object, expression1);
            final Geometry rightGeom = toGeometry(object, expression2);

            if (leftGeom == null || rightGeom == null) {
                return false;
            }

            try {
                final Object[] values = toSameCRS(leftGeom, rightGeom, unit);

                if (values[2] == null) {
                    //no matching crs was found, assume both have the same and valid unit
                    return !leftGeom.isWithinDistance(rightGeom, distance);
                } else {
                    final Geometry leftMatch = (Geometry) values[0];
                    final Geometry rightMatch = (Geometry) values[1];
                    final CoordinateReferenceSystem crs = (CoordinateReferenceSystem) values[2];
                    final UnitConverter converter = unit.getConverterTo(crs.getCoordinateSystem().getAxis(0).getUnit());

                    return !leftMatch.isWithinDistance(rightMatch, converter.convert(distance));
                }

            } catch (FactoryException | TransformException ex) {
                warning(ex);
                return false;
            }
        }

        @Override
        public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }

    /**
     * The {@value #NAME} filter.
     */
    static final class Contains extends SpatialFunction implements org.opengis.filter.spatial.Contains {

        Contains(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        @Override
        protected String getName() {
            return NAME;
        }

        @Override
        public boolean evaluate(Object object) {
            Geometry leftGeom = toGeometry(object, expression1);
            Geometry rightGeom = toGeometry(object, expression2);

            if (leftGeom == null || rightGeom == null) {
                return false;
            }

            final Geometry[] values;
            try {
                values = toSameCRS(leftGeom, rightGeom);
            } catch (FactoryException | TransformException ex) {
                warning(ex);
                return false;
            }
            leftGeom = values[0];
            rightGeom = values[1];

            final org.locationtech.jts.geom.Envelope envLeft = leftGeom.getEnvelopeInternal();
            final org.locationtech.jts.geom.Envelope envRight = rightGeom.getEnvelopeInternal();

            if (envLeft.contains(envRight)) {
                return leftGeom.contains(rightGeom);
            }
            return false;
        }

        @Override
        public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }

    /**
     * The {@value #NAME} filter.
     */
    static final class Crosses extends SpatialFunction implements org.opengis.filter.spatial.Crosses {

        Crosses(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        @Override
        protected String getName() {
            return NAME;
        }

        @Override
        public boolean evaluate(Object object) {
            Geometry leftGeom = toGeometry(object, expression1);
            Geometry rightGeom = toGeometry(object, expression2);

            if (leftGeom == null || rightGeom == null) {
                return false;
            }

            final Geometry[] values;
            try {
                values = toSameCRS(leftGeom, rightGeom);
            } catch (FactoryException | TransformException ex) {
                warning(ex);
                return false;
            }
            leftGeom = values[0];
            rightGeom = values[1];

            final org.locationtech.jts.geom.Envelope envLeft = leftGeom.getEnvelopeInternal();
            final org.locationtech.jts.geom.Envelope envRight = rightGeom.getEnvelopeInternal();

            if (envRight.intersects(envLeft)) {
                return leftGeom.crosses(rightGeom);
            }

            return false;
        }

        @Override
        public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }

    /**
     * The {@value #NAME} filter.
     */
    static final class Disjoint extends SpatialFunction implements org.opengis.filter.spatial.Disjoint {

        Disjoint(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        @Override
        protected String getName() {
            return NAME;
        }

        @Override
        public boolean evaluate(Object object) {
            Geometry leftGeom = toGeometry(object, expression1);
            Geometry rightGeom = toGeometry(object, expression2);

            if (leftGeom == null || rightGeom == null) {
                return false;
            }

            final Geometry[] values;
            try {
                values = toSameCRS(leftGeom, rightGeom);
            } catch (FactoryException | TransformException ex) {
                warning(ex);
                return false;
            }
            leftGeom = values[0];
            rightGeom = values[1];

            final org.locationtech.jts.geom.Envelope envLeft = leftGeom.getEnvelopeInternal();
            final org.locationtech.jts.geom.Envelope envRight = rightGeom.getEnvelopeInternal();

            if (envRight.intersects(envLeft)) {
                return leftGeom.disjoint(rightGeom);
            }

            return true;
        }

        @Override
        public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }

    /**
     * The {@value #NAME} filter.
     */
    static final class DWithin extends SpatialFunction implements org.opengis.filter.spatial.DWithin {

        private final double distance;
        private final String units;
        private final Unit unit;

        DWithin(Expression expression1, Expression expression2, double distance, String units) {
            super(expression1, expression2);
            this.distance = distance;
            this.units = units;
            this.unit = Units.valueOf(units);
        }

        @Override
        protected String getName() {
            return NAME;
        }

        @Override
        public double getDistance() {
            return distance;
        }

        @Override
        public String getDistanceUnits() {
            return units;
        }

        @Override
        public boolean evaluate(Object object) {
            final Geometry leftGeom = toGeometry(object, expression1);
            final Geometry rightGeom = toGeometry(object, expression2);

            if (leftGeom == null || rightGeom == null) {
                return false;
            }

            try {
                final Object[] values = toSameCRS(leftGeom, rightGeom, unit);

                if (values[2] == null) {
                    //no matching crs was found, assume both have the same and valid unit
                    return leftGeom.isWithinDistance(rightGeom, distance);
                } else {
                    final Geometry leftMatch = (Geometry) values[0];
                    final Geometry rightMatch = (Geometry) values[1];
                    final CoordinateReferenceSystem crs = (CoordinateReferenceSystem) values[2];
                    final UnitConverter converter = unit.getConverterTo(crs.getCoordinateSystem().getAxis(0).getUnit());

                    return leftMatch.isWithinDistance(rightMatch, converter.convert(distance));
                }

            } catch (FactoryException | TransformException ex) {
                warning(ex);
                return false;
            }
        }

        @Override
        public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }

    /**
     * The {@value #NAME} filter.
     */
    static final class Equals extends SpatialFunction implements org.opengis.filter.spatial.Equals {

        Equals(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        @Override
        protected String getName() {
            return NAME;
        }

        @Override
        public boolean evaluate(Object object) {
            Geometry leftGeom = toGeometry(object, expression1);
            Geometry rightGeom = toGeometry(object, expression2);

            if (leftGeom == null || rightGeom == null) {
                return false;
            }

            final Geometry[] values;
            try {
                values = toSameCRS(leftGeom, rightGeom);
            } catch (FactoryException | TransformException ex) {
                warning(ex);
                return false;
            }
            leftGeom = values[0];
            rightGeom = values[1];

            return leftGeom.equals(rightGeom);
        }

        @Override
        public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }

    /**
     * The {@value #NAME} filter.
     */
    static final class Intersects extends SpatialFunction implements org.opengis.filter.spatial.Intersects {

        Intersects(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        @Override
        protected String getName() {
            return NAME;
        }

        @Override
        public boolean evaluate(Object object) {
            Geometry leftGeom = toGeometry(object, expression1);
            Geometry rightGeom = toGeometry(object, expression2);

            if (leftGeom == null || rightGeom == null) {
                return false;
            }

            final Geometry[] values;
            try {
                values = toSameCRS(leftGeom, rightGeom);
            } catch (FactoryException | TransformException ex) {
                warning(ex);
                return false;
            }
            leftGeom = values[0];
            rightGeom = values[1];

            final org.locationtech.jts.geom.Envelope envLeft = leftGeom.getEnvelopeInternal();
            final org.locationtech.jts.geom.Envelope envRight = rightGeom.getEnvelopeInternal();

            if (envLeft.intersects(envRight)) {
                return leftGeom.intersects(rightGeom);
            }
            return false;
        }

        @Override
        public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }

    /**
     * The {@value #NAME} filter.
     */
    static final class Overlaps extends SpatialFunction implements org.opengis.filter.spatial.Overlaps {

        Overlaps(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        @Override
        protected String getName() {
            return NAME;
        }

        @Override
        public boolean evaluate(Object object) {
            Geometry leftGeom = toGeometry(object, expression1);
            Geometry rightGeom = toGeometry(object, expression2);

            if (leftGeom == null || rightGeom == null) {
                return false;
            }

            final Geometry[] values;
            try {
                values = toSameCRS(leftGeom, rightGeom);
            } catch (FactoryException | TransformException ex) {
                warning(ex);
                return false;
            }
            leftGeom = values[0];
            rightGeom = values[1];

            final org.locationtech.jts.geom.Envelope envLeft = leftGeom.getEnvelopeInternal();
            final org.locationtech.jts.geom.Envelope envRight = rightGeom.getEnvelopeInternal();

            if (envLeft.intersects(envRight)) {
                return leftGeom.overlaps(rightGeom);
            }
            return false;
        }

        @Override
        public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }

    /**
     * The {@value #NAME} filter.
     */
    static final class Touches extends SpatialFunction implements org.opengis.filter.spatial.Touches {

        Touches(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        @Override
        protected String getName() {
            return NAME;
        }

        @Override
        public boolean evaluate(Object object) {
            Geometry leftGeom = toGeometry(object, expression1);
            Geometry rightGeom = toGeometry(object, expression2);

            if (leftGeom == null || rightGeom == null) {
                return false;
            }

            final Geometry[] values;
            try {
                values = toSameCRS(leftGeom, rightGeom);
            } catch (FactoryException | TransformException ex) {
                warning(ex);
                return false;
            }
            leftGeom = values[0];
            rightGeom = values[1];
            return leftGeom.touches(rightGeom);
        }

        @Override
        public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }

    /**
     * The {@value #NAME} filter.
     */
    static final class Within extends SpatialFunction implements org.opengis.filter.spatial.Within {

        Within(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        @Override
        protected String getName() {
            return NAME;
        }

        @Override
        public boolean evaluate(Object object) {
            Geometry leftGeom = toGeometry(object, expression1);
            Geometry rightGeom = toGeometry(object, expression2);

            if (leftGeom == null || rightGeom == null) {
                return false;
            }

            final Geometry[] values;
            try {
                values = toSameCRS(leftGeom, rightGeom);
            } catch (FactoryException | TransformException ex) {
                warning(ex);
                return false;
            }
            leftGeom = values[0];
            rightGeom = values[1];

            final org.locationtech.jts.geom.Envelope envLeft = leftGeom.getEnvelopeInternal();
            final org.locationtech.jts.geom.Envelope envRight = rightGeom.getEnvelopeInternal();

            if (envRight.contains(envLeft)) {
                return leftGeom.within(rightGeom);
            }
            return false;
        }

        @Override
        public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }
}
