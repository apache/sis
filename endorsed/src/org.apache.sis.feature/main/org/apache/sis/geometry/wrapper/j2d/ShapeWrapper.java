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
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.PathIterator;
import java.awt.geom.AffineTransform;
import java.io.Serializable;
import org.apache.sis.referencing.internal.shared.AbstractShape;


/**
 * A wrapper that delegate all {@link Shape} methods to a source shape.
 * Subclasses should override at least one method for making this class useful.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class ShapeWrapper extends AbstractShape implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -3917772814285085629L;

    /**
     * The source of coordinate values.
     */
    @SuppressWarnings("serial")         // Most Java2D implementations are serializable.
    protected final Shape source;

    /**
     * Creates a new wrapper for the given shape.
     *
     * @param  source  the source of coordinate values.
     */
    protected ShapeWrapper(final Shape source) {
        this.source = source;
    }

    /**
     * Returns {@code true} if this shape backed by primitive {@code float} values.
     */
    @Override
    protected boolean isFloat() {
        return isFloat(source);
    }

    /**
     * Returns a rectangle that completely encloses this {@code Shape}.
     * This is not necessarily the smallest bounding box if an accurate
     * computation would be too expensive.
     *
     * @return a rectangle that completely encloses this {@code Shape}.
     */
    @Override
    public Rectangle getBounds() {
        return source.getBounds();
    }

    /**
     * Returns a rectangle that completely encloses this {@code Shape}.
     * This is not necessarily the smallest bounding box if an accurate
     * computation would be too expensive.
     *
     * @return a rectangle that completely encloses this {@code Shape}.
     */
    @Override
    public Rectangle2D getBounds2D() {
        return source.getBounds2D();
    }

    /**
     * Tests if the specified coordinates are inside the boundary of this shape.
     *
     * @param  x  the first coordinate value.
     * @param  y  the second coordinate value.
     * @return whether the point specified by given coordinates is inside this shape.
     */
    @Override
    public boolean contains(double x, double y) {
        return source.contains(x, y);
    }

    /**
     * Tests if the point is inside the boundary of this shape.
     *
     * @param  p  the point to test.
     * @return whether the given point is inside this shape.
     */
    @Override
    public boolean contains(Point2D p) {
        return source.contains(p);
    }

    /**
     * Tests if the interior of this {@code Shape} intersects the interior of a specified rectangular area.
     * This method may conservatively return {@code true} if an accurate computation would be too expensive.
     *
     * @param  x  minimal <var>x</var> coordinate of the rectangle.
     * @param  y  minimal <var>y</var> coordinate of the rectangle.
     * @param  w  width of the rectangle.
     * @param  h  height of the rectangle.
     * @return whether the specified rectangle intersects the interior of this shape.
     */
    @Override
    public boolean intersects(double x, double y, double w, double h) {
        return source.intersects(x, y, w, h);
    }

    /**
     * Tests if the interior of this {@code Shape} intersects the interior of a specified rectangular area.
     * This method may conservatively return {@code true} if an accurate computation would be too expensive.
     *
     * @param  r  the rectangular area to test.
     * @return whether the specified rectangle intersects the interior of this shape.
     */
    @Override
    public boolean intersects(Rectangle2D r) {
        return source.intersects(r);
    }

    /**
     * Tests if the interior of this {@code Shape} entirely contains the interior of a specified rectangular area.
     * This method may conservatively return {@code false} if an accurate computation would be too expensive.
     *
     * @param  x  minimal <var>x</var> coordinate of the rectangle.
     * @param  y  minimal <var>y</var> coordinate of the rectangle.
     * @param  w  width of the rectangle.
     * @param  h  height of the rectangle.
     * @return whether the specified rectangle entirely contains the interior of this shape.
     */
    @Override
    public boolean contains(double x, double y, double w, double h) {
        return source.contains(x, y, w, h);
    }

    /**
     * Tests if the interior of this {@code Shape} entirely contains the interior of a specified rectangular area.
     * This method may conservatively return {@code false} if an accurate computation would be too expensive.
     *
     * @param  r  the rectangular area to test.
     * @return whether the specified rectangle entirely contains the interior of this shape.
     */
    @Override
    public boolean contains(Rectangle2D r) {
        return source.contains(r);
    }

    /**
     * Returns an iterator over the coordinates of this shape.
     *
     * @param  at  an optional transform to be applied on coordinate values, or {@code null} if none.
     * @return iterator over the coordinate values of this shape.
     */
    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        return source.getPathIterator(at);
    }

    /**
     * Returns an iterator over the coordinates of this shape, approximated by line segments.
     *
     * @param  at        an optional transform to be applied on coordinate values, or {@code null} if none.
     * @param  flatness  maximum distance between line segments approximations and the curve segments.
     * @return iterator over the coordinate values of line segments approximating this shape.
     */
    @Override
    public PathIterator getPathIterator(final AffineTransform at, final double flatness) {
        return source.getPathIterator(at, flatness);
    }
}
