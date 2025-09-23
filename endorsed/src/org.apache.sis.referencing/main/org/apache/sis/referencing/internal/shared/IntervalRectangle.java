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
package org.apache.sis.referencing.internal.shared;

import java.io.Serializable;
import java.awt.geom.Rectangle2D;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.util.internal.shared.Strings;


/**
 * Rectangle defines by intervals instead of by a size.
 * Instead of ({@code x}, {@code y}, {@code width}, {@code height}) values as do standard Java2D implementations,
 * this class contains ({@link #xmin}, {@link #xmax}, {@link #ymin}, {@link #ymax}) values. This choice provides
 * three benefits:
 *
 * <ul>
 *   <li>Allows this class to work correctly with {@linkplain java.lang.Double#isInfinite() infinite} and
 *       {@linkplain java.lang.Double#isNaN() NaN} values. By comparison, the (<var>width</var>, <var>height</var>)
 *       alternative is ambiguous.</li>
 *   <li>Slightly faster {@code contains(…)} and {@code intersects(…)} methods since there is no addition or
 *       subtraction to perform.</li>
 *   <li>Better inter-operability with {@link Envelope2D} when a rectangle spans the anti-meridian.
 *       This {@code IntervalRectangle} class does not support such envelopes by itself, but it is
 *       okay to create a rectangle with negative width and gives it in argument to
 *       {@link Envelope2D#contains(Rectangle2D)} or {@link Envelope2D#intersects(Rectangle2D)} methods.</li>
 * </ul>
 *
 * This class provides the following additional methods which are not defined in {@link Rectangle2D}:
 * <ul>
 *   <li>{@link #containsInclusive(double, double)}</li>
 *   <li>{@link #distanceSquared(double, double)}</li>
 * </ul>
 *
 * This class does <strong>not</strong> support by itself rectangles crossing the anti-meridian of a geographic CRS.
 * However, the {@link #getX()}, {@link #getY()}, {@link #getWidth()} and {@link #getHeight()} methods are defined in
 * the straightforward way expected by {@link Envelope2D#intersects(Rectangle2D)} and similar methods for computing
 * correct result if the given {@code Rectangle2D} crosses the anti-meridian.
 *
 * <h2>Internal usage of inheritance</h2>
 * This class may also be opportunistically extended by some Apache SIS internal classes that need a rectangle in
 * addition of their own information. All {@code Rectangle2D} methods are declared final for reducing the risk of
 * confusion with other aspects managed by subclasses. We don't do that in public API because this is not a
 * recommended approach, but for Apache SIS private classes this is a way to reduce pressure on garbage collector.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("CloneableImplementsClone")
public class IntervalRectangle extends Rectangle2D implements Serializable {
    /** For cross-version compatibility. */
    private static final long serialVersionUID = -5921513912411186629L;

    /** Minimal <var>x</var> coordinate value. */ public double xmin;
    /** Minimal <var>y</var> coordinate value. */ public double ymin;
    /** Maximal <var>x</var> coordinate value. */ public double xmax;
    /** Maximal <var>y</var> coordinate value. */ public double ymax;

    /**
     * Constructs a default rectangle initialized to {@code (0,0,0,0)}.
     */
    public IntervalRectangle() {
    }

    /**
     * Constructs a rectangle initialized to the two first dimensions of the given envelope.
     * If the given envelope crosses the anti-meridian, then the new rectangle will span the
     * full longitude range (i.e. this constructor does not preserve the convention of using
     * negative width for envelopes crossing anti-meridian).
     *
     * <h4>Design note</h4>
     * This constructor expands envelopes that cross the anti-meridian
     * because the methods defined in this class are not designed for handling such envelopes.
     * If a rectangle with negative width is nevertheless desired for envelope crossing the anti-meridian,
     * one can use the following constructor:
     *
     * {@snippet lang="java" :
     *     new IntervalRectangle(envelope.getLowerCorner(), envelope.getUpperCorner());
     *     }
     *
     * @param envelope  the envelope from which to copy the values.
     */
    public IntervalRectangle(final Envelope envelope) {
        xmin = envelope.getMinimum(0);
        xmax = envelope.getMaximum(0);
        ymin = envelope.getMinimum(1);
        ymax = envelope.getMaximum(1);
    }

    /**
     * Constructs a rectangle initialized to the two first dimensions of the given corners.
     * This constructor unconditionally assigns {@code lower} coordinates to {@link #xmin}, {@link #ymin} and
     * {@code upper} coordinates to {@link #xmax}, {@link #ymax} regardless of their values; this constructor
     * does not verify if {@code lower} coordinates are smaller than {@code upper} coordinates.
     * This is sometimes useful for creating a rectangle crossing the anti-meridian,
     * even if {@code IntervalRectangle} class does not support such rectangles by itself.
     *
     * @param lower  the limits in the direction of decreasing coordinate values for each dimension.
     * @param upper  the limits in the direction of increasing coordinate values for each dimension.
     *
     * @see Envelope#getLowerCorner()
     * @see Envelope#getUpperCorner()
     */
    public IntervalRectangle(final DirectPosition lower, final DirectPosition upper) {
        xmin = lower.getOrdinate(0);
        xmax = upper.getOrdinate(0);
        ymin = lower.getOrdinate(1);
        ymax = upper.getOrdinate(1);
    }

    /**
     * Creates a rectangle using maximal <var>x</var> and <var>y</var> values rather than width and height.
     * This constructor avoid the problem of NaN values when extremum are infinite numbers.
     *
     * @param xmin  minimal <var>x</var> coordinate value.
     * @param ymin  minimal <var>y</var> coordinate value.
     * @param xmax  maximal <var>x</var> coordinate value.
     * @param ymax  maximal <var>y</var> coordinate value.
     */
    public IntervalRectangle(final double xmin, final double ymin, final double xmax, final double ymax) {
        this.xmin = xmin;
        this.ymin = ymin;
        this.xmax = xmax;
        this.ymax = ymax;
    }

    /**
     * Determines whether this rectangle is empty. If this rectangle has at least one
     * {@linkplain java.lang.Double#NaN NaN} value, then it is considered empty.
     *
     * @return {@code true} if this rectangle is empty; {@code false} otherwise.
     */
    @Override
    public final boolean isEmpty() {
        return !(xmin < xmax && ymin < ymax);
    }

    /**
     * Returns the minimal <var>x</var> coordinate value.
     *
     * @return the minimal <var>x</var> coordinate value.
     */
    @Override
    public final double getX() {
        return xmin;
    }

    /**
     * Returns the minimal <var>y</var> coordinate value.
     *
     * @return the minimal <var>y</var> coordinate value.
     */
    @Override
    public final double getY() {
        return ymin;
    }

    /**
     * Returns the width of the rectangle. May be negative if the rectangle crosses the anti-meridian.
     * This {@code IntervalRectangle} class does not support such envelopes itself, but other classes
     * like {@link Envelope2D} will handle correctly the negative width.
     *
     * @return the width of the rectangle.
     */
    @Override
    public final double getWidth() {
        return xmax - xmin;
    }

    /**
     * Returns the height of the rectangle.
     *
     * @return the height of the rectangle.
     */
    @Override
    public final double getHeight() {
        return ymax - ymin;
    }

    /**
     * Returns the minimal <var>x</var> coordinate value.
     *
     * @return the minimal <var>x</var> coordinate value.
     */
    @Override
    public final double getMinX() {
        return xmin;
    }

    /**
     * Returns the minimal <var>y</var> coordinate value.
     *
     * @return the minimal <var>y</var> coordinate value.
     */
    @Override
    public final double getMinY() {
        return ymin;
    }

    /**
     * Returns the maximal <var>x</var> coordinate value.
     *
     * @return the maximal <var>x</var> coordinate value.
     */
    @Override
    public final double getMaxX() {
        return xmax;
    }

    /**
     * Returns the maximal <var>y</var> coordinate value.
     *
     * @return the maximal <var>y</var> coordinate value.
     */
    @Override
    public final double getMaxY() {
        return ymax;
    }

    /**
     * Returns the <var>x</var> coordinate of the center of the rectangle.
     *
     * @return the median <var>x</var> coordinate value.
     */
    @Override
    public final double getCenterX() {
        return (xmin + xmax) * 0.5;
    }

    /**
     * Returns the <var>y</var> coordinate of the center of the rectangle.
     *
     * @return the median <var>y</var> coordinate value.
     */
    @Override
    public final double getCenterY() {
        return (ymin + ymax) * 0.5;
    }

    /**
     * Sets the location and size of this rectangle to the specified values.
     *
     * @param  x       the <var>x</var> minimal coordinate value.
     * @param  y       the <var>y</var> minimal coordinate value.
     * @param  width   the rectangle width.
     * @param  height  the rectangle height.
     */
    @Override
    public final void setRect(final double x, final double y, final double width, final double height) {
        xmin = x;
        ymin = y;
        xmax = x + width;
        ymax = y + height;
    }

    /**
     * Sets this rectangle to be the same as the specified rectangle.
     *
     * @param  r  the rectangle to copy values from.
     */
    @Override
    public final void setRect(final Rectangle2D r) {
        if (r != this) {        // Optimization for methods chaining like r.setRect(Shapes.transform(…, r))
            xmin = r.getMinX();
            ymin = r.getMinY();
            xmax = r.getMaxX();
            ymax = r.getMaxY();
        }
    }

    /**
     * Sets the framing rectangle to the given rectangle. The current implementation delegates
     * to {@link #setRect(Rectangle2D)}. This is consistent with the default implementation of
     * {@link #setFrame(double, double, double, double)}, which delegates to the corresponding
     * method of {@link #setRect(double, double, double, double) setRect}.
     */
    @Override
    public final void setFrame(final Rectangle2D r) {
        setRect(r);
    }

    /**
     * Tests if the interior of this rectangle intersects the interior of a specified set of rectangular coordinates.
     * The edges are considered exclusive; this method returns {@code false} if the two rectangles just touch to each
     * other.
     *
     * @param  x       the <var>x</var> minimal coordinate value.
     * @param  y       the <var>y</var> minimal coordinate value.
     * @param  width   the rectangle width.
     * @param  height  the rectangle height.
     * @return {@code true} if this rectangle intersects the interior of the specified set of rectangular coordinates.
     */
    @Override
    public final boolean intersects(final double x, final double y, final double width, final double height) {
        if (!(xmin < xmax && ymin < ymax && width > 0 && height > 0)) {
            return false;
        } else {
            return (x < xmax && y < ymax && x+width > xmin && y+height > ymin);
        }
    }

    /**
     * Tests if the interior of this shape intersects the interior of a specified rectangle.
     * The edges are considered exclusive; this method returns {@code false} if the two rectangles
     * just touch to each other.
     *
     * @param  rect  the specified rectangle.
     * @return {@code true} if this shape and the specified rectangle intersect each other.
     */
    @Override
    public final boolean intersects(final Rectangle2D rect) {
        if (!(xmin < xmax && ymin < ymax)) {
            return false;
        } else {
            final double xmin2 = rect.getMinX();
            final double xmax2 = rect.getMaxX(); if (!(xmax2 > xmin2)) return false;
            final double ymin2 = rect.getMinY();
            final double ymax2 = rect.getMaxY(); if (!(ymax2 > ymin2)) return false;
            return (xmin2 < xmax && ymin2 < ymax && xmax2 > xmin && ymax2 > ymin);
        }
    }

    /**
     * Tests if the interior of this rectangle entirely contains the specified set of rectangular coordinates.
     *
     * @param  x       the <var>x</var> minimal coordinate value.
     * @param  y       the <var>y</var> minimal coordinate value.
     * @param  width   the rectangle width.
     * @param  height  the rectangle height.
     * @return {@code true} if this rectangle entirely contains specified set of rectangular coordinates.
     */
    @Override
    public final boolean contains(final double x, final double y, final double width, final double height) {
        if (!(xmin < xmax && ymin < ymax && width > 0 && height > 0)) {
            return false;
        } else {
            return (x >= xmin && y >= ymin && (x+width) <= xmax && (y+height) <= ymax);
        }
    }

    /**
     * Tests if the interior of this shape entirely contains the specified rectangle.
     * This methods overrides the default {@link Rectangle2D} implementation in order
     * to work correctly with {@linkplain java.lang.Double#POSITIVE_INFINITY infinites}
     * and {@linkplain java.lang.Double#NaN NaN} values.
     *
     * @param  rect  the specified rectangle.
     * @return {@code true} if this shape entirely contains the specified rectangle.
     */
    @Override
    public final boolean contains(final Rectangle2D rect) {
        if (!(xmin < xmax && ymin < ymax)) {
            return false;
        } else {
            final double xmin2 = rect.getMinX();
            final double xmax2 = rect.getMaxX(); if (!(xmax2 > xmin2)) return false;
            final double ymin2 = rect.getMinY();
            final double ymax2 = rect.getMaxY(); if (!(ymax2 > ymin2)) return false;
            return (xmin2 >= xmin && ymin2 >= ymin && xmax2 <= xmax && ymax2 <= ymax);
        }
    }

    /**
     * Tests if a specified coordinate is inside the boundary of this {@code Rectangle2D}.
     * The maximal <var>x</var> and <var>y</var> values (i.e. the right and bottom edges
     * of this rectangle) are exclusive.
     *
     * @param  x  the <var>x</var> coordinates to test.
     * @param  y  the <var>y</var> coordinates to test.
     * @return {@code true} if the specified coordinates are inside the boundary of this rectangle.
     */
    @Override
    public final boolean contains(final double x, final double y) {
        return (x >= xmin && y >= ymin && x < xmax && y < ymax);
    }

    /**
     * Tests if a specified coordinate is inside the boundary of this {@code Rectangle2D}
     * with all edges inclusive.
     *
     * @param  x  the <var>x</var> coordinates to test.
     * @param  y  the <var>y</var> coordinates to test.
     * @return {@code true} if the specified coordinates are inside this rectangle, all edges included.
     */
    public final boolean containsInclusive(final double x, final double y) {
        return (x >= xmin && x <= xmax && y >= ymin && y <= ymax);
    }

    /**
     * Returns the square of the minimal distance between a point and this rectangle.
     * If the point is inside the rectangle or on the edge, then this method returns 0.
     *
     * @param  x  the <var>x</var> coordinates to test.
     * @param  y  the <var>y</var> coordinates to test.
     * @return square of minimal distance, or 0 if the point is inside this rectangle.
     */
    public final double distanceSquared(final double x, final double y) {
        int outcode = 0;
        double dx = java.lang.Double.POSITIVE_INFINITY;
        double dy = java.lang.Double.POSITIVE_INFINITY;
        double d;
        if ((d = (x - xmax)) >= 0)           {dx = d; outcode =  1;}
        if ((d = (y - ymax)) >= 0)           {dy = d; outcode |= 2;}
        if ((d = (xmin - x)) >= 0 && d < dx) {dx = d; outcode |= 1;}
        if ((d = (ymin - y)) >= 0 && d < dy) {dy = d; outcode |= 2;}
        switch (outcode) {
            case 1:  return dx*dx;                                  // Only x coordinate is outside.
            case 2:  return dy*dy;                                  // Only y coordinate is outside.
            case 3:  return dx*dx + dy*dy;                          // Both coordinates are outside.
            case 0:  assert containsInclusive(x, y); return 0;      // No coordinate is outside.
            default: throw new AssertionError(outcode);
        }
        /*
         * Note: if we want non-squared distance in a future version, we can rely on the fact
         * that `dx` and `dy` are guaranteed positives (no need to take the absolute value).
         * So we would replace the 3 first cases as below:
         *
         *     case 1:  return dx;
         *     case 2:  return dy;
         *     case 3:  return Math.hypot(dx, dy);
         */
    }

    /**
     * Determines where the specified coordinates lie with respect to this rectangle.
     * This method computes a binary OR of the appropriate mask values indicating,
     * for each side of this {@code Rectangle2D}, whether or not the specified coordinates
     * are on the same side of the edge as the rest of this {@code Rectangle2D}.
     *
     * @return the logical OR of all appropriate out codes.
     *
     * @see #OUT_LEFT
     * @see #OUT_TOP
     * @see #OUT_RIGHT
     * @see #OUT_BOTTOM
     */
    @Override
    public final int outcode(final double x, final double y) {
        int out = 0;
        if (!(xmax > xmin)) out |= OUT_LEFT | OUT_RIGHT;
        else if (x < xmin)  out |= OUT_LEFT;
        else if (x > xmax)  out |= OUT_RIGHT;

        if (!(ymax > ymin)) out |= OUT_TOP | OUT_BOTTOM;
        else if (y < ymin)  out |= OUT_TOP;
        else if (y > ymax)  out |= OUT_BOTTOM;
        return out;
    }

    /**
     * Intersects a {@link Rectangle2D} object with this rectangle.
     * The resulting* rectangle is the intersection of the two {@code Rectangle2D} objects.
     * Invoking this method is equivalent to invoking the following code, except that this
     * method behaves correctly with infinite values and {@link Envelope2D} implementation.
     *
     * {@snippet lang="java" :
     *     Rectangle2D.intersect(this, rect, this);
     *     }
     *
     * @param  rect  the {@code Rectangle2D} to intersect with this rectangle.
     *
     * @see #intersect(Rectangle2D, Rectangle2D, Rectangle2D)
     * @see #createIntersection(Rectangle2D)
     */
    public final void intersect(final Rectangle2D rect) {
        double t;
        // Must use getMin/Max methods, not getX/Y/Width/Height, for inter-operability with Envelope2D.
        if ((t = rect.getMinX()) > xmin) xmin = t;
        if ((t = rect.getMaxX()) < xmax) xmax = t;
        if ((t = rect.getMinY()) > ymin) ymin = t;
        if ((t = rect.getMaxY()) < ymax) ymax = t;
    }

    /**
     * Returns a new {@code Rectangle2D} object representing the intersection of this rectangle with the specified one.
     *
     * @param  rect  the {@code Rectangle2D} to be intersected with this rectangle.
     * @return the largest {@code Rectangle2D} contained in both the specified rectangle and this one.
     *
     * @see #intersect(Rectangle2D)
     */
    @Override
    public final Rectangle2D createIntersection(final Rectangle2D rect) {
        final IntervalRectangle r = new IntervalRectangle();
        r.xmin = Math.max(xmin, rect.getMinX());
        r.ymin = Math.max(ymin, rect.getMinY());
        r.xmax = Math.min(xmax, rect.getMaxX());
        r.ymax = Math.min(ymax, rect.getMaxY());
        return r;
    }

    /**
     * Returns a new {@code Rectangle2D} object representing the
     * union of this rectangle with the specified one.
     *
     * @param  rect  the {@code Rectangle2D} to be combined with this rectangle.
     * @return the smallest {@code Rectangle2D} containing both the specified {@code Rectangle2D} and this one.
     */
    @Override
    public final Rectangle2D createUnion(final Rectangle2D rect) {
        final IntervalRectangle r = new IntervalRectangle();
        r.xmin = Math.min(xmin, rect.getMinX());
        r.ymin = Math.min(ymin, rect.getMinY());
        r.xmax = Math.max(xmax, rect.getMaxX());
        r.ymax = Math.max(ymax, rect.getMaxY());
        return r;
    }

    /**
     * Adds a point, specified by the arguments {@code x} and {@code y}, to this rectangle.
     * The resulting {@code Rectangle2D} is the smallest rectangle that contains both the
     * original rectangle and the specified point.
     *
     * <p>After adding a point, a call to {@code contains} with the added point as an argument
     * does not necessarily return {@code true}. The {@code contains} method does not return
     * {@code true} for points on the right or bottom edges of a rectangle. Therefore, if the
     * added point falls on the left or bottom edge of the enlarged rectangle, {@code contains}
     * returns {@code false} for that point.</p>
     *
     * @param  x  x coordinate value of the point to add.
     * @param  y  y coordinate value of the point to add.
     */
    @Override
    public final void add(final double x, final double y) {
        if (x < xmin) xmin = x;
        if (x > xmax) xmax = x;
        if (y < ymin) ymin = y;
        if (y > ymax) ymax = y;
    }

    /**
     * Adds a {@code Rectangle2D} object to this rectangle.
     * The resulting rectangle is the union of the two {@code Rectangle2D} objects.
     *
     * @param  rect  the {@code Rectangle2D} to add to this rectangle.
     */
    @Override
    public final void add(final Rectangle2D rect) {
        double t;
        if ((t = rect.getMinX()) < xmin) xmin = t;
        if ((t = rect.getMaxX()) > xmax) xmax = t;
        if ((t = rect.getMinY()) < ymin) ymin = t;
        if ((t = rect.getMaxY()) > ymax) ymax = t;
    }

    /**
     * Returns the {@code String} representation of this {@code Rectangle2D}. The coordinate order is
     * (<var>x</var><sub>min</sub>, <var>y</var><sub>min</sub>, <var>x</var><sub>max</sub>, <var>y</var><sub>max</sub>),
     * which is consistent with the {@link #IntervalRectangle(double, double, double, double)} constructor
     * and with the {@code BBOX} <i>Well Known Text</i> (WKT) syntax.
     *
     * @return a {@code String} representing this {@code Rectangle2D}.
     */
    @Override
    public String toString() {
        return Strings.toString(getClass(), "xmin", xmin, "ymin", ymin, "xmax", xmax, "ymax", ymax);
    }
}
