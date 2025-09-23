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
package org.apache.sis.geometry.wrapper.jts;

import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.PathIterator;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.Objects;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.apache.sis.geometry.wrapper.j2d.EmptyShape;
import org.apache.sis.referencing.internal.shared.AbstractShape;
import org.apache.sis.referencing.internal.shared.IntervalRectangle;


/**
 * A thin wrapper that adapts a JTS geometry to the {@link Shape} interface so
 * that the geometry can be used by Java 2D without copying coordinate values.
 * This class does not cache any value; if the JTS geometry is changed,
 * the modifications will be immediately visible in this {@code Shape}.
 *
 * @author  Johann Sorel (Puzzle-GIS, Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ShapeAdapter extends AbstractShape implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -8536828815289601141L;

    /**
     * A lightweight JTS geometry factory using the default
     * {@link org.locationtech.jts.geom.impl.CoordinateArraySequenceFactory}.
     * This factory is inefficient for large geometries (it consumes more memory)
     * but is a little bit more straightforward for geometry with few coordinates
     * such as a point or a rectangle, because it stores the {@code Coordinate[]}.
     * Used for {@code contains(…)} and {@code intersects(…)} implementations only.
     */
    private static final GeometryFactory SMALL_FACTORY = new GeometryFactory();

    /**
     * The wrapped JTS geometry.
     */
    protected final Geometry geometry;

    /**
     * Creates a new wrapper for the given JTS geometry.
     *
     * @param  geometry  the JTS geometry to wrap.
     */
    protected ShapeAdapter(final Geometry geometry) {
        this.geometry = Objects.requireNonNull(geometry);
    }

    /**
     * Returns {@code true} if this shape backed by primitive {@code float} values.
     */
    @Override
    protected boolean isFloat() {
        final CoordinateSequence cs;
        if (geometry instanceof Point) {
            cs = ((Point) geometry).getCoordinateSequence();
        } else if (geometry instanceof LineString) {
            cs = ((LineString) geometry).getCoordinateSequence();
        } else {
            return super.isFloat();
        }
        return Factory.isFloat(cs);
    }

    /**
     * Returns an integer rectangle that completely encloses the shape.
     * There is no guarantee that the rectangle is the smallest bounding box that encloses the shape.
     */
    @Override
    public Rectangle getBounds() {
        return getBounds2D().getBounds();
    }

    /**
     * Returns a rectangle that completely encloses the shape.
     * There is no guarantee that the rectangle is the smallest bounding box that encloses the shape.
     */
    @Override
    public Rectangle2D getBounds2D() {
        final Envelope e = geometry.getEnvelopeInternal();
        return new IntervalRectangle(e.getMinX(), e.getMinY(),
                                     e.getMaxX(), e.getMaxY());
    }

    /**
     * Tests if the specified point is inside the boundary of the shape.
     * This method delegates to {@link #contains(double, double)}.
     */
    @Override
    public boolean contains(final Point2D p) {
        return contains(p.getX(), p.getY());
    }

    /**
     * Tests if the specified point is inside the boundary of the shape.
     */
    @Override
    public boolean contains(final double x, final double y) {
        return geometry.contains(SMALL_FACTORY.createPoint(new Coordinate(x, y)));
    }

    /**
     * Tests if the specified rectangle is inside the boundary of the shape.
     */
    @Override
    public boolean contains(final Rectangle2D r) {
        return geometry.contains(createRect(r.getMinX(), r.getMinY(), r.getMaxX(), r.getMaxY()));
    }

    /**
     * Tests if the specified rectangle is inside the boundary of the shape.
     */
    @Override
    public boolean contains(final double x, final double y, final double width, final double height) {
        return geometry.contains(createRect(x, y, x + width, y + height));
    }

    /**
     * Tests if the specified rectangle intersects this shape.
     */
    @Override
    public boolean intersects(final Rectangle2D r) {
        return geometry.intersects(createRect(r.getMinX(), r.getMinY(), r.getMaxX(), r.getMaxY()));
    }

    /**
     * Tests if the specified rectangle intersects this shape.
     */
    @Override
    public boolean intersects(final double x, final double y, final double width, final double height) {
        return geometry.intersects(createRect(x, y, x + width, y + height));
    }

    /**
     * Creates a JTS polygon which is a rectangle with the given coordinates.
     * This is a temporary shape used for union and intersection tests.
     */
    private static Geometry createRect(final double xmin, final double ymin, final double xmax, final double ymax) {
        final Coordinate origin = new Coordinate(xmin, ymin);
        final LinearRing ring = SMALL_FACTORY.createLinearRing(new Coordinate[] {
            origin,
            new Coordinate(xmin, ymax),
            new Coordinate(xmax, ymax),
            new Coordinate(xmax, ymin),
            origin
        });
        return SMALL_FACTORY.createPolygon(ring);
    }

    /**
     * Returns an iterator for the shape outline geometry. The flatness factor is ignored on the assumption
     * that this shape does not contain any Bézier curve.
     *
     * @param  at  optional transform to apply on coordinate values.
     * @param  flatness  ignored.
     * @return an iterator for the shape outline geometry.
     */
    @Override
    public PathIterator getPathIterator(final AffineTransform at, final double flatness) {
        return getPathIterator(at);
    }

    /**
     * Returns an iterator for the shape outline geometry.
     *
     * @param  at  optional transform to apply on coordinate values.
     * @return an iterator for the shape outline geometry.
     */
    @Override
    public PathIterator getPathIterator(final AffineTransform at) {
        if (geometry.isEmpty()) {
            return EmptyShape.INSTANCE;
        } else {
            return new PathIteratorAdapter(geometry, at);
        }
    }
}
