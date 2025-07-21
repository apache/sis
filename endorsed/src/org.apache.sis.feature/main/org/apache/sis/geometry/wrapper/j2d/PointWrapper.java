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
package org.apache.sis.geometry.wrapper.j2d;

import java.awt.Shape;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.function.BiPredicate;
import org.opengis.geometry.DirectPosition;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.apache.sis.filter.sqlmm.SQLMM;
import org.apache.sis.util.Debug;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.SpatialOperatorName;


/**
 * The wrapper of Java2D points. Has to be provided in a separated class because
 * {@link Point2D} are not {@link Shape} in Java2D API.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class PointWrapper extends GeometryWrapper {
    /**
     * The wrapped implementation.
     */
    final Point2D point;

    /**
     * Creates a new wrapper around the given point.
     */
    PointWrapper(final Point2D point) {
        this.point = point;
    }

    /**
     * Returns the implementation-dependent factory of geometric object.
     */
    @Override
    protected Geometries<Shape> factory() {
        return Factory.INSTANCE;
    }

    /**
     * Returns the point specified at construction time.
     */
    @Override
    protected Object implementation() {
        return point;
    }

    /**
     * Returns an empty envelope centered on this point.
     */
    @Override
    public GeneralEnvelope getEnvelope() {
        final GeneralEnvelope env = createEnvelope();
        final double x = point.getX();
        final double y = point.getY();
        env.setRange(0, x, y);
        env.setRange(1, x, y);
        return env;
    }

    /**
     * Returns the centroid of the wrapped geometry as a direct position.
     */
    @Override
    public DirectPosition getCentroid() {
        return new DirectPosition2D(getCoordinateReferenceSystem(), point.getX(), point.getY());
    }

    /**
     * Returns the point coordinates.
     */
    @Override
    public double[] getPointCoordinates() {
        return new double[] {
            point.getX(),
            point.getY()
        };
    }

    /**
     * Returns all coordinate tuples in the wrapped geometry.
     * This method is currently used for testing purpose only.
     */
    @Debug
    @Override
    public double[] getAllCoordinates() {
        return getPointCoordinates();
    }

    /**
     * Merges a sequence of points or paths after this geometry.
     *
     * @throws ClassCastException if an element in the iterator is not a {@link Shape} or a {@link Point2D}.
     */
    @Override
    public Shape mergePolylines(final Iterator<?> polylines) {
        return Wrapper.mergePolylines(point, polylines);
    }

    /**
     * Applies a filter predicate between this geometry and another geometry.
     * This method assumes that the two geometries are in the same CRS (this is not verified).
     */
    @Override
    protected boolean predicateSameCRS(final SpatialOperatorName type, final GeometryWrapper other) {
        final int ordinal = type.ordinal();
        if (ordinal >= 0 && ordinal < PREDICATES.length) {
            final BiPredicate<PointWrapper,Object> op = PREDICATES[ordinal];
            if (op != null) {
                return op.test(this, other);
            }
        }
        return super.predicateSameCRS(type, other);
    }

    /**
     * All predicates recognized by {@link #predicateSameCRS(SpatialOperatorName, GeometryWrapper)}.
     * Array indices are {@link SpatialOperatorName#ordinal()} values.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    private static final BiPredicate<PointWrapper,Object>[] PREDICATES =
            new BiPredicate[SpatialOperatorName.OVERLAPS.ordinal() + 1];
    static {
        PREDICATES[SpatialOperatorName.BBOX      .ordinal()] = // Fallback on intersects.
        PREDICATES[SpatialOperatorName.OVERLAPS  .ordinal()] = // Fallback on intersects.
        PREDICATES[SpatialOperatorName.INTERSECTS.ordinal()] = PointWrapper::intersect;
        PREDICATES[SpatialOperatorName.WITHIN    .ordinal()] = PointWrapper::within;
        PREDICATES[SpatialOperatorName.CONTAINS  .ordinal()] = // Fallback on equals.
        PREDICATES[SpatialOperatorName.EQUALS    .ordinal()] = PointWrapper::equal;
        PREDICATES[SpatialOperatorName.DISJOINT  .ordinal()] = (w,o) -> !w.intersect(o);
    }

    /**
     * Applies a SQLMM operation on this geometry.
     *
     * @param  operation  the SQLMM operation to apply.
     * @param  other      the other geometry, or {@code null} if the operation requires only one geometry.
     * @param  argument   an operation-specific argument, or {@code null} if not applicable.
     * @return result of the specified operation.
     */
    @Override
    @SuppressWarnings("fallthrough")
    protected Object operationSameCRS(final SQLMM operation, final GeometryWrapper other, final Object argument) {
        switch (operation) {
            case ST_Dimension:
            case ST_CoordDim:   return 2;
            case ST_Is3D:
            case ST_IsMeasured: return Boolean.FALSE;
            case ST_Centroid:   return point.clone();
            case ST_Envelope:   return getEnvelope();
            case ST_Boundary: {
                if (point instanceof Point) {
                    final var p = (Point) point;
                    final var r = new Rectangle();
                    r.x = p.x;
                    r.y = p.y;
                    return r;
                } else if (point instanceof Point2D.Float) {
                    final var p = (Point2D.Float) point;
                    final var r = new Rectangle2D.Float();
                    r.x = p.x;
                    r.y = p.y;
                    return r;
                } else {
                    final var r = new Rectangle2D.Double();
                    r.x = point.getX();
                    r.y = point.getY();
                    return r;
                }
            }
            case ST_Overlaps:   // Falback on "within".
            case ST_Within:     return  within(other);
            case ST_Intersects: return  intersect(other);
            case ST_Disjoint:   return !intersect(other);
            case ST_Contains:   // Fallback on "equals".
            case ST_Equals:     return  equal(other);
            default: return super.operationSameCRS(operation, other, argument);
        }
    }

    /**
     * Estimates whether the wrapped geometry is equal to the geometry of the given wrapper.
     *
     * @param  wrapper  instance of {@link PointWrapper}.
     */
    private boolean equal(final Object wrapper) {       // "s" omitted for avoiding confusion with super.equals(…).
        if (wrapper instanceof PointWrapper) {
            final Point2D p = ((PointWrapper) wrapper).point;
            return Double.doubleToLongBits(point.getX()) == Double.doubleToLongBits(p.getX())
                && Double.doubleToLongBits(point.getY()) == Double.doubleToLongBits(p.getY());
        }
        return false;
    }

    /**
     * Estimates whether the wrapped geometry is contained by the geometry of the given wrapper.
     * This method may conservatively returns {@code false} if an accurate computation would be
     * too expensive.
     *
     * @param  wrapper  instance of {@link Wrapper}.
     */
    private boolean within(final Object wrapper) {
        return (wrapper instanceof Wrapper) && (((Wrapper) wrapper).geometry).contains(point);
    }

    /**
     * Estimates whether the wrapped geometry intersects the geometry of the given wrapper.
     * This method may conservatively returns {@code true} if an accurate computation would
     * be too expensive.
     *
     * @param  wrapper  instance of {@link Wrapper} or {@link PointWrapper}.
     * @throws ClassCastException if the given object is not a recognized wrapper.
     */
    private boolean intersect(final Object wrapper) {   // "s" omitted for avoiding confusion with super.intersects(…).
        if (wrapper instanceof PointWrapper) {
            return point.equals(((PointWrapper) wrapper).point);
        } else {
            return within(wrapper);
        }
    }

    /**
     * Builds a WKT representation of the wrapped point.
     */
    @Override
    public String formatWKT(final double flatness) {
        return getCentroid().toString();
    }
}
