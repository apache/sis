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

import java.io.IOException;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import org.opengis.filter.FilterVisitor;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.spatial.Intersects;
import org.opengis.geometry.Geometry;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

import org.apache.sis.internal.feature.GeometryWrapper;
import org.apache.sis.internal.feature.jts.JTS;
import org.apache.sis.util.collection.BackingStoreException;

import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

import static org.apache.sis.util.ArgumentChecks.ensureNonEmpty;
import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

/**
 * TODO: refine once Geometry API is stable.
 */
public final class ST_Intersects implements Intersects, Serializable {

    public static final String NAME = "ST_Intersect";

    final Expression left;
    final Expression right;

    private transient Predicate intersects;

    public ST_Intersects(Expression[] parameters) {
        ensureNonEmpty("Parameters", parameters);
        if (parameters.length != 2) throw new IllegalArgumentException("2 parameters are expected for intersection, but "+parameters.length+" are provided");

        left = parameters[0];
        right = parameters[1];
        init();
    }

    private void init() {
        ensureNonNull("Left operand", left);
        ensureNonNull("Right operand", right);
        if (left instanceof Literal && right instanceof Literal) {
            final boolean constantResult = nonOptimizedIntersects(null);
            intersects = it -> constantResult;
        } else if (left instanceof Literal) {
            intersects = intersect((Literal) left, right);
        } else if (right instanceof Literal) {
            intersects = intersect((Literal) right, left);
        } else intersects = this::nonOptimizedIntersects;
    }

    private boolean nonOptimizedIntersects(Object candidate) {
        final Object leftEval = left.evaluate(candidate);
        final Object rightEval = right.evaluate(candidate);
        if (leftEval == null || rightEval == null) return false;

        final Map.Entry<org.locationtech.jts.geom.Geometry, CoordinateReferenceSystem> leftEntry = toJTS(leftEval);
        final Map.Entry<org.locationtech.jts.geom.Geometry, CoordinateReferenceSystem> rightEntry = toJTS(rightEval);
        final CRSMatching.Match match = CRSMatching.left(leftEntry.getValue()).right(rightEntry.getValue());

        final org.locationtech.jts.geom.Geometry leftGeom = match.fromLeft()
                .map(op -> transformSilently(leftEntry.getKey(), op))
                .orElse(leftEntry.getKey());
        final org.locationtech.jts.geom.Geometry rightGeom = match.fromRight()
                .map(op -> transformSilently(rightEntry.getKey(), op))
                .orElse(rightEntry.getKey());

        return leftGeom.intersects(rightGeom);
    }

    private static org.locationtech.jts.geom.Geometry transformSilently(org.locationtech.jts.geom.Geometry target, CoordinateOperation op) {
        try {
            return JTS.transform(target, op);
        } catch (TransformException | FactoryException e) {
            throw new BackingStoreException(e);
        }
    }

    private static Map.Entry<org.locationtech.jts.geom.Geometry, CoordinateReferenceSystem> toJTS(Object value) {
        CoordinateReferenceSystem crs = null;
        if (value instanceof Geometry) crs = ((Geometry) value).getCoordinateReferenceSystem();
        if (value instanceof GeometryWrapper) value = ((GeometryWrapper) value).geometry;
        if (value instanceof org.locationtech.jts.geom.Geometry) {
            final org.locationtech.jts.geom.Geometry geom = (org.locationtech.jts.geom.Geometry) value;
            if (crs == null) {
                try {
                    crs = JTS.getCoordinateReferenceSystem(geom);
                } catch (FactoryException e) {
                    throw new BackingStoreException("Cannot extract CRS from operand", e);
                }
            }
            return new AbstractMap.SimpleImmutableEntry<>(geom, crs);
        }
        throw new UnsupportedOperationException("Unsupported geometry type: "+value.getClass().getCanonicalName());
    }

    private static Predicate intersect(Literal left, Expression right) {
        Object value = left.getValue();
        ensureNonNull("Literal value", value);
        // TODO: make more consistent strategy once Geometry API is stable.
        try {
            final Map.Entry<org.locationtech.jts.geom.Geometry, CoordinateReferenceSystem> leftEntry = toJTS(value);
            final CRSMatching crsMatching = CRSMatching.left(leftEntry.getValue());
            final PreparedGeometry optimizedGeom = new PreparedGeometryFactory().create((org.locationtech.jts.geom.Geometry) value);
            return it -> {
                Object val = right.evaluate(it);
                if (val == null) return false;
                final Map.Entry<org.locationtech.jts.geom.Geometry, CoordinateReferenceSystem> rightEntry = toJTS(val);
                final CRSMatching.Match match = crsMatching.right(rightEntry.getValue());
                final org.locationtech.jts.geom.Geometry rightGeom = match.fromRight()
                        .map(op -> transformSilently(rightEntry.getKey(), op))
                        .orElse(rightEntry.getKey());
                return match.fromLeft()
                        .map(op -> transformSilently(leftEntry.getKey(), op))
                        .map(geom -> geom.intersects(rightGeom))
                        .orElseGet(() -> optimizedGeom.intersects(rightGeom));
            };
        } catch (UnsupportedOperationException e) {
            if (value instanceof Geometry) {
                final Geometry geom = (Geometry) value;
                return it -> {
                    final Geometry newVal = left.evaluate(it, Geometry.class);
                    if (newVal == null) {
                        final Object testVal = left.evaluate(it);
                        if (testVal == null) return false;
                        throw new UnsupportedOperationException("Unsupported geometry type: "+testVal.getClass().getCanonicalName());
                    }
                    return geom.intersects(newVal);
                };
            }
        }
        throw new UnsupportedOperationException("Unsupported geometry type: "+value.getClass().getCanonicalName());
    }

    @Override
    public Expression getExpression1() {
        return left;
    }

    @Override
    public Expression getExpression2() {
        return right;
    }

    @Override
    public boolean evaluate(Object object) {
        return intersects.test(object);
    }

    @Override
    public Object accept(FilterVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ST_Intersects that = (ST_Intersects) o;
        return left.equals(that.left) &&
                right.equals(that.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }

    /**
     * Initialize this filter state. It is necessary because of serialization compliance.
     */
    private void readObject(java.io.ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        init();
    }
}
