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
package org.apache.sis.math;

import java.io.Serializable;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.apache.sis.internal.util.DoubleDouble;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.util.resources.Errors;

import static java.lang.Double.*;


/**
 * Equation of a line in a two dimensional space (<var>x</var>,<var>y</var>).
 * A line can be expressed by the <var>y</var> = <var>slope</var>⋅<var>x</var> + <var>y₀</var> equation
 * where <var>y₀</var> is the value of <var>y</var> at <var>x</var> = 0.
 *
 * <p>The equation parameters for a {@code Line} object can be set at construction time or using one
 * of the {@code setLine(…)} methods. The <var>y</var> value can be computed for a given <var>x</var>
 * value using the {@link #y(double)} method. Method {@link #x(double)} computes the converse and should
 * work even if the line is vertical.</p>
 *
 * <div class="note"><b>Comparison with Java2D geometries:</b>
 * At the difference of {@link java.awt.geom.Line2D} which is bounded by (<var>x₁</var>,<var>y₁</var>)
 * and (<var>x₂</var>,<var>y₂</var>) points, {@code Line} objects extend toward infinity.</div>
 *
 * @author  Martin Desruisseaux (MPO, IRD)
 * @since   0.5
 * @version 0.5
 * @module
 *
 * @see Plane
 * @see org.apache.sis.referencing.operation.builder.LinearTransformBuilder
 */
public class Line implements Cloneable, Serializable {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = 2185952238314399110L;

    /**
     * Number of dimensions.
     */
    private static final int DIMENSION = 2;

    /**
     * The slope for this line.
     */
    private double slope;

    /**
     * The <var>y</var> value at <var>x</var> = 0.
     */
    private double y0;

    /**
     * Value of <var>x</var> at <var>y</var> = 0.
     * This value is used for vertical lines.
     */
    private double x0;

    /**
     * Constructs an uninitialized line. All methods will return {@link Double#NaN}.
     */
    public Line() {
        slope = y0 = x0 = NaN;
    }

    /**
     * Constructs a line with the specified slope and offset.
     * The linear equation will be <var>y</var> = <var>slope</var>⋅<var>x</var> + <var>y₀</var>.
     *
     * @param slope The slope.
     * @param y0 The <var>y</var> value at <var>x</var> = 0.
     *
     * @see #setEquation(double, double)
     */
    public Line(final double slope, final double y0) {
        this.slope =  slope;
        this.y0    =  y0;
        this.x0    = -y0 / slope;
    }

    /**
     * Returns the slope.
     *
     * @return The slope.
     *
     * @see #x0()
     * @see #y0()
     */
    public final double slope() {
        return slope;
    }

    /**
     * Returns the <var>x</var> value for <var>y</var> = 0.
     * Coordinate (<var>x₀</var>, 0) is the intersection point with the <var>x</var> axis.
     *
     * @return The <var>x</var> value for <var>y</var> = 0.
     *
     * @see #y0()
     * @see #slope()
     */
    public final double x0() {
        return x0;
    }

    /**
     * Computes <var>x</var> = <var>f</var>⁻¹(<var>y</var>).
     * If the line is horizontal, then this method returns an infinite value.
     *
     * @param  y The <var>y</var> value where to evaluate the inverse function.
     * @return The <var>x</var> value for the given <var>y</var> value.
     *
     * @see #y(double)
     */
    public final double x(final double y) {
        return x0 + y/slope;
    }

    /**
     * Returns the <var>y</var> value for <var>x</var> = 0.
     * Coordinate (0, <var>y₀</var>) is the intersection point with the <var>y</var> axis.
     *
     * @return The <var>y</var> value for <var>x</var> = 0.
     *
     * @see #x0()
     * @see #slope()
     */
    public final double y0() {
        return y0;
    }

    /**
     * Computes <var>y</var> = <var>f</var>(<var>x</var>).
     * If the line is vertical, then this method returns an infinite value.
     *
     * @param  x The <var>x</var> value where to evaluate the inverse function.
     * @return The <var>y</var> value for the given <var>x</var> value.
     *
     * @see #x(double)
     */
    public final double y(final double x) {
        return y0 + x*slope;
    }

    /**
     * Translates the line. The slope stay unchanged.
     *
     * @param dx The horizontal translation.
     * @param dy The vertical translation.
     */
    public void translate(final double dx, final double dy) {
        if (slope == 0 || isInfinite(slope)) {
            x0 += dx;
            y0 += dy;
        } else {
            x0 += dx - dy/slope;
            y0 += dy - slope*dx;
        }
    }

    /**
     * Sets this line to the specified slope and offset.
     * The linear equation will be <var>y</var> = <var>slope</var>⋅<var>x</var> + <var>y₀</var>.
     *
     * @param slope The slope.
     * @param y0 The <var>y</var> value at <var>x</var> = 0.
     *
     * @see #setFromPoints(double, double, double, double)
     * @see #fit(double[], double[])
     */
    public void setEquation(final double slope, final double y0) {
        this.slope =  slope;
        this.y0    =  y0;
        this.x0    = -y0 / slope;
    }

    /**
     * Sets a line through the specified points.
     * The line will continue toward infinity after the points.
     *
     * @param x1 Ordinate <var>x</var> of the first point.
     * @param y1 Ordinate <var>y</var> of the first point.
     * @param x2 Ordinate <var>x</var> of the second point.
     * @param y2 Ordinate <var>y</var> of the second point.
     */
    public void setFromPoints(final double x1, final double y1, final double x2, final double y2) {
        this.slope = (y2 - y1) / (x2 - x1);
        this.x0    = x2 - y2/slope;
        this.y0    = y2 - slope*x2;
        if (isNaN(x0) && slope == 0) {
            // Occurs for horizontal lines right on the x axis.
            x0 = POSITIVE_INFINITY;
        }
        if (isNaN(y0) && isInfinite(slope)) {
            // Occurs for vertical lines right on the y axis.
            y0 = POSITIVE_INFINITY;
        }
    }

    /**
     * Given a set of data points <var>x</var>[0 … <var>n</var>-1], <var>y</var>[0 … <var>n</var>-1],
     * fits them to a straight line <var>y</var> = <var>slope</var>⋅<var>x</var> + <var>y₀</var> in a
     * least-squares senses. This method assume that the <var>x</var> values are precise and all uncertainty
     * is in <var>y</var>.
     *
     * @param x Vector of <var>x</var> values (independent variable).
     * @param y Vector of <var>y</var> values (dependent variable).
     * @return Estimation of the correlation coefficient. The closer this coefficient is to +1 or -1, the better the fit.
     *
     * @throws IllegalArgumentException if <var>x</var> and <var>y</var> do not have the same length.
     */
    public double fit(final double[] x, final double[] y) {
        // Do not invoke an overrideable method with our tricky iterable.
        return doFit(new CompoundDirectPositions(x, y));
    }

    /**
     * Given a sequence of points, fits them to a straight line <var>y</var> = <var>slope</var>⋅<var>x</var> +
     * <var>y₀</var> in a least-squares senses. This method assume that the <var>x</var> values are precise and
     * all uncertainty is in <var>y</var>.
     *
     * <p>Points shall be two dimensional with ordinate values in the (<var>x</var>,<var>y</var>) order.
     * {@link Double#NaN} ordinate values are ignored.</p>
     *
     * @param  points The two-dimensional points.
     * @return Estimation of the correlation coefficient. The closer this coefficient is to +1 or -1, the better the fit.
     * @throws MismatchedDimensionException if a point is not two-dimensional.
     */
    public double fit(final Iterable<? extends DirectPosition> points) {
        return doFit(points);
    }

    /**
     * Implementation of public {@code fit(…)} methods.
     */
    private double doFit(final Iterable<? extends DirectPosition> points) {
        int i = 0, n = 0;
        final DoubleDouble mean_x = new DoubleDouble();
        final DoubleDouble mean_y = new DoubleDouble();
        for (final DirectPosition p : points) {
            final int dimension = p.getDimension();
            if (dimension != DIMENSION) {
                throw new MismatchedDimensionException(Errors.format(
                        Errors.Keys.MismatchedDimension_3, "points[" + i + ']', DIMENSION, dimension));
            }
            i++;
            final double x,y;
            if (!isNaN(y = p.getOrdinate(1)) &&  // Test first the dimension which is most likely to contain NaN.
                !isNaN(x = p.getOrdinate(0)))
            {
                mean_x.add(x);
                mean_y.add(y);
                n++;
            }
        }
        mean_x.divide(n, 0);
        mean_y.divide(n, 0);
        /*
         * We have to solve two equations with two unknowns:
         *
         *   1)    mean(y)   = m⋅mean(x) + y₀
         *   2)    mean(x⋅y) = m⋅mean(x²) + y₀⋅mean(x)
         *
         * Those formulas lead to a quadratic equation. However, the formulas become very simples
         * if we set 'mean(x) = 0'. We can achieve this result by computing instead of (2):
         *
         *   2b)   mean(Δx⋅y) = m⋅mean(Δx²)
         *
         * where dx = x-mean(x). In this case mean(Δx) == 0.
         */
        final DoubleDouble a       = new DoubleDouble();
        final DoubleDouble b       = new DoubleDouble();
        final DoubleDouble mean_x2 = new DoubleDouble();
        final DoubleDouble mean_y2 = new DoubleDouble();
        final DoubleDouble mean_xy = new DoubleDouble();
        for (final DirectPosition p : points) {
            final double y;
            if (!isNaN(y       = p.getOrdinate(1)) &&  // Test first the dimension which is most likely to contain NaN.
                !isNaN(a.value = p.getOrdinate(0)))
            {
                a.error = 0;
                a.subtract(mean_x);     // Δx = x - mean_x
                b.setFrom(a);
                b.multiply(a);          // (Δx)² = Δx * Δx
                mean_x2.add(b);         // mean_x² += (Δx)²

                a.multiply(y);          // xy = Δx * y
                mean_xy.add(a);         // mean_xy += xy

                a.setToProduct(y, y);   // y² = y * y
                mean_y2.add(a);         // mean_y² += y²
            }
        }
        mean_x2.divide(n, 0);
        mean_y2.divide(n, 0);
        mean_xy.divide(n, 0);
        /*
         * Assuming that 'mean(x) == 0', then the correlation
         * coefficient can be approximate by:
         *
         * R = mean(xy) / sqrt( mean(x²) * (mean(y²) - mean(y)²) )
         */
        a.setFrom(mean_xy);
        a.divide (mean_x2);     // slope = mean_xy / mean_x²
        b.setFrom(mean_x);
        b.multiply(a);
        b.negate();
        b.add(mean_y);          // y₀ = mean_y - mean_x * slope
        setEquation(a.value, b.value);
        /*
         * Compute the correlation coefficient:
         * mean_xy / sqrt(mean_x2 * (mean_y2 - mean_y * mean_y))
         */
        a.setFrom(mean_y);
        a.multiply(mean_y);
        a.negate();
        a.add(mean_y2);
        a.multiply(mean_x2);
        a.sqrt();
        a.inverseDivide(mean_xy);
        return a.value;
    }

    /**
     * Returns a clone of this line.
     *
     * @return A clone of this line.
     */
    @Override
    public Line clone() {
        try {
            return (Line) super.clone();
        } catch (CloneNotSupportedException exception) {
            throw new AssertionError(exception);
        }
    }

    /**
     * Compares this line with the specified object for equality.
     *
     * @param object The object to compare with this line for equality.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object) {
        if (object != null && getClass() == object.getClass()) {
            final Line that = (Line) object;
            return Numerics.equals(this.slope, that.slope) &&
                   Numerics.equals(this.y0,    that.y0   ) &&
                   Numerics.equals(this.x0,    that.x0   );
        } else {
            return false;
        }
    }

    /**
     * Returns a hash code value for this line.
     */
    @Override
    public int hashCode() {
        return Numerics.hashCode(serialVersionUID ^ (doubleToLongBits(slope) + 31*doubleToLongBits(y0)));
    }

    /**
     * Returns a string representation of this line. This method returns the linear equation
     * in the form <var>y</var> = <var>slope</var>⋅<var>x</var> + <var>y₀</var>.
     *
     * @return A string representation of this line.
     */
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder(50);
        if (isInfinite(slope)) {
            buffer.append("x = ").append(x0);
        } else {
            buffer.append("y = ");
            String separator = "";
            if (slope != 0) {
                buffer.append(slope).append("⋅x");
                separator = " + ";
            }
            if (y0 != 0) {
                buffer.append(separator).append(y0);
            }
        }
        return buffer.toString();
    }
}
