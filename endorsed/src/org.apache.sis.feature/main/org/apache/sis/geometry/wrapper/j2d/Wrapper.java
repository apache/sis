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

import java.util.List;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.BiPredicate;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.geom.PathIterator;
import org.opengis.geometry.DirectPosition;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.apache.sis.filter.sqlmm.SQLMM;
import org.apache.sis.referencing.internal.shared.ShapeUtilities;
import org.apache.sis.referencing.internal.shared.AbstractShape;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Debug;

// Specific to the main branch:
import org.apache.sis.pending.geoapi.filter.SpatialOperatorName;


/**
 * The wrapper of Java2D geometries.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 */
final class Wrapper extends GeometryWrapper {
    /**
     * The wrapped implementation.
     */
    final Shape geometry;

    /**
     * Creates a new wrapper around the given geometry.
     */
    Wrapper(final Shape geometry) {
        this.geometry = geometry;
    }

    /**
     * Returns the implementation-dependent factory of geometric object.
     */
    @Override
    protected Geometries<Shape> factory() {
        return Factory.INSTANCE;
    }

    /**
     * Returns the geometry specified at construction time.
     */
    @Override
    protected Object implementation() {
        return geometry;
    }

    /**
     * Returns the geometry as an {@link Area} object, creating it on the fly if necessary.
     * The returned area shall not be modified because it may be the {@link #geometry} instance.
     */
    private Area area() {
        return (geometry instanceof Area) ? (Area) geometry : new Area(geometry);
    }

    /**
     * Returns the Java2D envelope as an Apache SIS implementation.
     *
     * @return the envelope of the geometry.
     */
    @Override
    public GeneralEnvelope getEnvelope() {
        final Rectangle2D bounds = geometry.getBounds2D();
        final GeneralEnvelope env = createEnvelope();
        env.setRange(0, bounds.getMinX(), bounds.getMaxX());
        env.setRange(1, bounds.getMinY(), bounds.getMaxY());
        return env;
    }

    /**
     * Returns the centroid of the wrapped geometry as a direct position.
     */
    @Override
    public DirectPosition getCentroid() {
        final RectangularShape frame = (geometry instanceof RectangularShape)
                        ? (RectangularShape) geometry : geometry.getBounds2D();
        return new DirectPosition2D(getCoordinateReferenceSystem(), frame.getCenterX(), frame.getCenterY());
    }

    /**
     * Returns {@code null} since {@link Shape} are never points in Java2D API.
     */
    @Override
    public double[] getPointCoordinates() {
        return null;
    }

    /**
     * Returns all coordinate tuples in the wrapped geometry.
     * This method is currently used for testing purpose only.
     */
    @Debug
    @Override
    public double[] getAllCoordinates() {
        final List<double[]> coordinates = new ShapeProperties(geometry).coordinatesAsDoubles();
        switch (coordinates.size()) {
            case 0:  return ArraysExt.EMPTY_DOUBLE;
            case 1:  return coordinates.get(0);
            default: {
                /*
                 * Concatenate the coordinates of all polygons in a single array. We lost the distinction
                 * between the different polygons, which is why this method should not be used except for
                 * testing.
                 */
                final double[] tgt = new double[coordinates.stream().mapToInt((a) -> a.length).sum()];
                int p = 0;
                for (final double[] src : coordinates) {
                    System.arraycopy(src, 0, tgt, p, src.length);
                    p += src.length;
                }
                return tgt;
            }
        }
    }

    /**
     * Merges a sequence of points or paths after this geometry.
     *
     * @throws ClassCastException if an element in the iterator is not a {@link Shape} or a {@link Point2D}.
     */
    @Override
    public Shape mergePolylines(final Iterator<?> polylines) {
        return mergePolylines(geometry, polylines);
    }

    /**
     * Implementation of {@link #mergePolylines(Iterator)} also shared by {@link PointWrapper}.
     */
    static Shape mergePolylines(Object next, final Iterator<?> polylines) {
        boolean isFloat = AbstractShape.isFloat(next);
        Path2D path = isFloat ? new Path2D.Float() : new Path2D.Double();
        boolean lineTo = false;
add:    for (;;) {
            if (next instanceof Point2D) {
                final var p = (Point2D) next;
                final double x = p.getX();
                final double y = p.getY();
                if (Double.isNaN(x) || Double.isNaN(y)) {
                    lineTo = false;
                } else if (lineTo) {
                    path.lineTo(x, y);
                } else {
                    path.moveTo(x, y);
                    lineTo = true;
                }
            } else {
                path.append((Shape) next, false);
                lineTo = false;
            }
            /*
             * 'polylines.hasNext()' check is conceptually part of 'for' instruction,
             * except that we need to skip this condition during the first iteration.
             */
            do if (!polylines.hasNext()) break add;
            while ((next = polylines.next()) == null);
            /*
             * Convert the path from single-precision to double-precision if needed.
             */
            if (isFloat && !AbstractShape.isFloat(next)) {
                path = new Path2D.Double(path);
                isFloat = false;
            }
        }
        return ShapeUtilities.toPrimitive(path);
    }

    /**
     * {@return directly the underlying Java2D geometry}. This method does not copy the shape.
     * Caller should not modify the returned shape (by casting to an implementation class).
     */
    @Override
    public Shape toJava2D() {
        return geometry;
    }

    /**
     * Applies a filter predicate between this geometry and another geometry.
     * This method assumes that the two geometries are in the same CRS (this is not verified).
     */
    @Override
    protected boolean predicateSameCRS(final SpatialOperatorName type, final GeometryWrapper other) {
        final int ordinal = type.ordinal();
        if (ordinal >= 0 && ordinal < PREDICATES.length) {
            final BiPredicate<Wrapper,Object> op = PREDICATES[ordinal];
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
    private static final BiPredicate<Wrapper,Object>[] PREDICATES =
            new BiPredicate[SpatialOperatorName.OVERLAPS.ordinal() + 1];
    static {
        PREDICATES[SpatialOperatorName.OVERLAPS  .ordinal()] = // Fallback on intersects.
        PREDICATES[SpatialOperatorName.INTERSECTS.ordinal()] = Wrapper::intersect;
        PREDICATES[SpatialOperatorName.CONTAINS  .ordinal()] = Wrapper::contain;
        PREDICATES[SpatialOperatorName.WITHIN    .ordinal()] = Wrapper::within;
        PREDICATES[SpatialOperatorName.BBOX      .ordinal()] = Wrapper::bbox;
        PREDICATES[SpatialOperatorName.EQUALS    .ordinal()] = Wrapper::equal;
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
    protected Object operationSameCRS(final SQLMM operation, final GeometryWrapper other, final Object argument) {
        final Shape result;
        switch (operation) {
            case ST_Dimension:
            case ST_CoordDim:   return 2;
            case ST_Is3D:
            case ST_IsMeasured: return Boolean.FALSE;
            case ST_IsEmpty: {
                if (geometry instanceof RectangularShape) {
                    return ((RectangularShape) geometry).isEmpty();
                } else {
                    return geometry.getPathIterator(null).isDone();
                }
            }
            case ST_Overlaps:   // Our approximate algorithm cannot distinguish with intersects.
            case ST_Intersects: return  intersect(other);
            case ST_Disjoint:   return !intersect(other);
            case ST_Contains:   return  contain  (other);
            case ST_Within:     return  within   (other);
            case ST_Equals:     return  equal    (other);
            case ST_Envelope:   return getEnvelope();
            case ST_Boundary:   result = geometry.getBounds2D(); break;
            case ST_Centroid: {
                final RectangularShape frame = (geometry instanceof RectangularShape)
                                ? (RectangularShape) geometry : geometry.getBounds2D();
                return new Point2D.Double(frame.getCenterX(), frame.getCenterY());
            }
            case ST_Intersection: {
                final Area area = new Area(geometry);
                area.intersect(((Wrapper) other).area());
                result = area;
                break;
            }
            case ST_Union: {
                final Area area = new Area(geometry);
                area.add(((Wrapper) other).area());
                result = area;
                break;
            }
            case ST_Difference: {
                final Area area = new Area(geometry);
                area.subtract(((Wrapper) other).area());
                result = area;
                break;
            }
            case ST_SymDifference: {
                final Area area = new Area(geometry);
                area.exclusiveOr(((Wrapper) other).area());
                result = area;
                break;
            }
            default: return super.operationSameCRS(operation, other, argument);
        }
        // No metadata to copy in current version.
        return result;
    }

    /**
     * Estimates whether the wrapped geometry is equal to the geometry of the given wrapper.
     *
     * @param  wrapper  instance of {@link Wrapper}.
     */
    private boolean equal(final Object wrapper) {       // "s" omitted for avoiding confusion with super.equals(…).
        if (wrapper instanceof Wrapper) {
            final Shape other = ((Wrapper) wrapper).geometry;
            final PathIterator it1 = geometry.getPathIterator(null);
            final PathIterator it2 = other.getPathIterator(null);
            if (it1.getWindingRule() == it2.getWindingRule()) {
                final double[] p1 = new double[6];
                final double[] p2 = new double[6];
                while (!it1.isDone()) {
                    if (it2.isDone()) return false;
                    final int c = it1.currentSegment(p1);
                    if (c != it2.currentSegment(p2)) {
                        return false;
                    }
                    it1.next();
                    it2.next();
                    final int n;
                    switch (c) {
                        case PathIterator.SEG_CLOSE: continue;
                        case PathIterator.SEG_MOVETO:
                        case PathIterator.SEG_LINETO: n=2; break;
                        case PathIterator.SEG_QUADTO: n=4; break;
                        default: n=6; break;
                    }
                    if (!Arrays.equals(p1, 0, n, p2, 0, n)) {
                        return false;
                    }
                }
                return it2.isDone();
            }
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
        if (wrapper instanceof Wrapper) {
            final Shape other = ((Wrapper) wrapper).geometry;
            return other.contains(geometry.getBounds2D());
        }
        return false;
    }

    /**
     * Estimates whether the wrapped geometry contains the geometry of the given wrapper.
     * This method may conservatively returns {@code false} if an accurate computation would
     * be too expensive.
     *
     * @param  wrapper  instance of {@link Wrapper} or {@link PointWrapper}.
     * @throws ClassCastException if the given object is not a recognized wrapper.
     */
    private boolean contain(final Object wrapper) {     // "s" omitted for avoiding confusion with super.contains(…).
        if (wrapper instanceof PointWrapper) {
            return geometry.contains(((PointWrapper) wrapper).point);
        }
        final Shape other = ((Wrapper) wrapper).geometry;
        return geometry.contains(other.getBounds2D());
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
            return geometry.contains(((PointWrapper) wrapper).point);
        }
        final Shape other = ((Wrapper) wrapper).geometry;
        return geometry.intersects(other.getBounds2D()) && other.intersects(geometry.getBounds2D());
    }

    /**
     * Estimates whether the wrapped geometry intersects the geometry of the given wrapper, testing only
     * the bounding box of the {@code wrapper} argument. This method may be more accurate than required
     * by OGC Filter Encoding specification in that this geometry is not simplified to a bounding box.
     * But Java2D implementations sometimes use bounding box approximation, so the result may be the same.
     *
     * @param  wrapper  instance of {@link Wrapper} or {@link PointWrapper}.
     * @throws ClassCastException if the given object is not a recognized wrapper.
     */
    private boolean bbox(final Object wrapper) {
        if (wrapper instanceof PointWrapper) {
            return geometry.contains(((PointWrapper) wrapper).point);
        }
        final Shape other = ((Wrapper) wrapper).geometry;
        return geometry.intersects(other.getBounds2D());
    }

    /**
     * Builds a WKT representation of the wrapped shape.
     * Current implementation assumes that all closed shapes are polygons and that polygons have no hole
     * (i.e. if a polygon is followed by more data, this method assumes that the additional data is a disjoint polygon).
     *
     * @see <a href="https://en.wikipedia.org/wiki/Well-known_text_representation_of_geometry">Well-known text on Wikipedia</a>
     */
    @Override
    public String formatWKT(final double flatness) {
        return new ShapeProperties(geometry).toWKT(flatness);
    }
}
