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
package org.apache.sis.geometries.adapter;

import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.PathIterator;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Objects;
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.LinearRing;
import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.internal.shared.ArraySequence;
import org.apache.sis.geometries.math.Array;
import org.apache.sis.geometries.math.DataType;
import org.apache.sis.geometries.math.NDArrays;
import org.apache.sis.geometries.math.SampleSystem;
import org.apache.sis.geometry.wrapper.j2d.EmptyShape;
import org.apache.sis.referencing.internal.shared.AbstractShape;
import org.apache.sis.referencing.internal.shared.IntervalRectangle;
import org.opengis.geometry.Envelope;


/**
 * A thin wrapper that adapts a SIS geometry to the {@link Shape} interface so
 * that the geometry can be used by Java 2D without copying coordinate values.
 * This class does not cache any value; if the SIS geometry is changed,
 * the modifications will be immediately visible in this {@code Shape}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ShapeAdapter extends AbstractShape {

    /**
     * The wrapped SIS geometry.
     */
    protected final Geometry geometry;

    /**
     * Creates a new wrapper for the given SIS geometry.
     *
     * @param  geometry  the SIS geometry to wrap.
     */
    public ShapeAdapter(final Geometry geometry) {
        this.geometry = Objects.requireNonNull(geometry);
    }

    /**
     * Returns {@code true} if this shape backed by primitive {@code float} values.
     */
    @Override
    protected boolean isFloat() {
        final DataType dataType = geometry.getAttributesType().getAttributeType(AttributesType.ATT_POSITION);
        return dataType == DataType.FLOAT;
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
        final Envelope e = geometry.getEnvelope();
        return new IntervalRectangle(e.getMinimum(0), e.getMinimum(1),
                                     e.getMaximum(0), e.getMaximum(1));
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
        return geometry.contains(GeometryFactory.createPoint(SampleSystem.ofSize(2), x, y));
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
     * Creates a SIS polygon which is a rectangle with the given coordinates.
     * This is a temporary shape used for union and intersection tests.
     */
    private static Geometry createRect(final double xmin, final double ymin, final double xmax, final double ymax) {
        final Array positions = NDArrays.of(2, new double[]{
            xmin, ymin,
            xmin, ymax,
            xmax, ymax,
            xmax, ymin,
            xmin, ymin
        });
        final PointSequence ps = new ArraySequence(positions);
        final LinearRing ring = GeometryFactory.createLinearRing(ps);
        return GeometryFactory.createPolygon(ring, null);
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
