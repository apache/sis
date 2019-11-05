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
import org.apache.sis.math.Fraction;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.util.collection.BackingStoreException;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.BinarySpatialOperator;
import org.opengis.geometry.Envelope;
import org.opengis.util.FactoryException;

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
    static final class BBOX extends SpatialFunction implements org.opengis.filter.spatial.BBOX {

        private final Envelope env;

        BBOX(Expression expression, Envelope env) {
            super(expression, new LeafExpression.Literal(env));
            this.env = env;
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
            throw new UnsupportedOperationException("Not supported yet.");
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

        Beyond(Expression expression1, Expression expression2, double distance, String units) {
            super(expression1, expression2);
            this.distance = distance;
            this.units = units;
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
            throw new UnsupportedOperationException("Not supported yet.");
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
            throw new UnsupportedOperationException("Not supported yet.");
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
            throw new UnsupportedOperationException("Not supported yet.");
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
            throw new UnsupportedOperationException("Not supported yet.");
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

        DWithin(Expression expression1, Expression expression2, double distance, String units) {
            super(expression1, expression2);
            this.distance = distance;
            this.units = units;
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
            throw new UnsupportedOperationException("Not supported yet.");
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
            throw new UnsupportedOperationException("Not supported yet.");
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
            throw new UnsupportedOperationException("Not supported yet.");
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
            throw new UnsupportedOperationException("Not supported yet.");
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
            throw new UnsupportedOperationException("Not supported yet.");
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
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }
}
