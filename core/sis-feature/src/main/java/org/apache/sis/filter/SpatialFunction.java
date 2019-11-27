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
import javax.measure.Unit;
import javax.measure.UnitConverter;
import org.apache.sis.internal.filter.FilterGeometryUtils;

import org.opengis.filter.FilterVisitor;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.spatial.BinarySpatialOperator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

import org.apache.sis.math.Fraction;
import org.apache.sis.measure.Units;

import org.locationtech.jts.geom.Geometry;

/**
 *
 * @author  Johann Sorel (Geomatys)
 * @version 2.0
 * @since   2.0
 * @module
 */
abstract class SpatialFunction extends BinaryFunction implements BinarySpatialOperator {

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

    /**
     * The {@value #NAME} filter.
     */
    static final class Beyond extends SpatialFunction implements org.opengis.filter.spatial.Beyond {

        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -610084000917390844L;

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
        public String getName() {
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
            final Geometry leftGeom = FilterGeometryUtils.toGeometry(object, expression1);
            final Geometry rightGeom = FilterGeometryUtils.toGeometry(object, expression2);

            if (leftGeom == null || rightGeom == null) {
                return false;
            }

            try {
                final Object[] values = FilterGeometryUtils.toSameCRS(leftGeom, rightGeom, unit);

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

        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -5510684627928940359L;

        Contains(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public boolean evaluate(Object object) {
            Geometry leftGeom = FilterGeometryUtils.toGeometry(object, expression1);
            Geometry rightGeom = FilterGeometryUtils.toGeometry(object, expression2);

            if (leftGeom == null || rightGeom == null) {
                return false;
            }

            final Geometry[] values;
            try {
                values = FilterGeometryUtils.toSameCRS(leftGeom, rightGeom);
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

        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -7022836273547341845L;

        Crosses(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public boolean evaluate(Object object) {
            Geometry leftGeom = FilterGeometryUtils.toGeometry(object, expression1);
            Geometry rightGeom = FilterGeometryUtils.toGeometry(object, expression2);

            if (leftGeom == null || rightGeom == null) {
                return false;
            }

            final Geometry[] values;
            try {
                values = FilterGeometryUtils.toSameCRS(leftGeom, rightGeom);
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

        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 8822946475076920125L;

        Disjoint(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public boolean evaluate(Object object) {
            Geometry leftGeom = FilterGeometryUtils.toGeometry(object, expression1);
            Geometry rightGeom = FilterGeometryUtils.toGeometry(object, expression2);

            if (leftGeom == null || rightGeom == null) {
                return false;
            }

            final Geometry[] values;
            try {
                values = FilterGeometryUtils.toSameCRS(leftGeom, rightGeom);
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

        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 7327351792495760963L;

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
        public String getName() {
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
            final Geometry leftGeom = FilterGeometryUtils.toGeometry(object, expression1);
            final Geometry rightGeom = FilterGeometryUtils.toGeometry(object, expression2);

            if (leftGeom == null || rightGeom == null) {
                return false;
            }

            try {
                final Object[] values = FilterGeometryUtils.toSameCRS(leftGeom, rightGeom, unit);

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

        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 6396428140074394187L;

        Equals(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public boolean evaluate(Object object) {
            Geometry leftGeom = FilterGeometryUtils.toGeometry(object, expression1);
            Geometry rightGeom = FilterGeometryUtils.toGeometry(object, expression2);

            if (leftGeom == null || rightGeom == null) {
                return false;
            }

            final Geometry[] values;
            try {
                values = FilterGeometryUtils.toSameCRS(leftGeom, rightGeom);
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

        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 6685367450421799746L;

        Intersects(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public boolean evaluate(Object object) {
            Geometry leftGeom = FilterGeometryUtils.toGeometry(object, expression1);
            Geometry rightGeom = FilterGeometryUtils.toGeometry(object, expression2);

            if (leftGeom == null || rightGeom == null) {
                return false;
            }

            final Geometry[] values;
            try {
                values = FilterGeometryUtils.toSameCRS(leftGeom, rightGeom);
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

        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 8793232792848445944L;

        Overlaps(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public boolean evaluate(Object object) {
            Geometry leftGeom = FilterGeometryUtils.toGeometry(object, expression1);
            Geometry rightGeom = FilterGeometryUtils.toGeometry(object, expression2);

            if (leftGeom == null || rightGeom == null) {
                return false;
            }

            final Geometry[] values;
            try {
                values = FilterGeometryUtils.toSameCRS(leftGeom, rightGeom);
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

        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -2747074093157567315L;

        Touches(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public boolean evaluate(Object object) {
            Geometry leftGeom = FilterGeometryUtils.toGeometry(object, expression1);
            Geometry rightGeom = FilterGeometryUtils.toGeometry(object, expression2);

            if (leftGeom == null || rightGeom == null) {
                return false;
            }

            final Geometry[] values;
            try {
                values = FilterGeometryUtils.toSameCRS(leftGeom, rightGeom);
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

        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 2089897118466562931L;

        Within(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public boolean evaluate(Object object) {
            Geometry leftGeom = FilterGeometryUtils.toGeometry(object, expression1);
            Geometry rightGeom = FilterGeometryUtils.toGeometry(object, expression2);

            if (leftGeom == null || rightGeom == null) {
                return false;
            }

            final Geometry[] values;
            try {
                values = FilterGeometryUtils.toSameCRS(leftGeom, rightGeom);
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
