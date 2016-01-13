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

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;
import static java.lang.Math.ulp;


/**
 * Equation of a plane in a three-dimensional space (<var>x</var>,<var>y</var>,<var>z</var>).
 * The plane equation is expressed by {@linkplain #slopeX() sx}, {@linkplain #slopeY() sy} and
 * {@linkplain #z0() z₀} coefficients as below:
 *
 * <blockquote>
 *   <var>{@linkplain #z(double, double) z}</var>(<var>x</var>,<var>y</var>) =
 *   <var>sx</var>⋅<var>x</var> + <var>sy</var>⋅<var>y</var> + <var>z₀</var>
 * </blockquote>
 *
 * Those coefficients can be set directly, or computed by a linear regression of this plane
 * through a set of three-dimensional points.
 *
 * @author  Martin Desruisseaux (MPO, IRD)
 * @author  Howard Freeland (MPO, for algorithmic inspiration)
 * @since   0.5
 * @version 0.5
 * @module
 *
 * @see Line
 * @see org.apache.sis.referencing.operation.builder.LinearTransformBuilder
 */
public class Plane implements Cloneable, Serializable {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = 2956201711131316723L;

    /**
     * Number of dimensions.
     */
    private static final int DIMENSION = 3;

    /**
     * Threshold value relative to 1 ULP of other terms in the  z = sx⋅x + sy⋅y + z₀  equation.
     * A value of 1 would be theoretically sufficient since adding a value smaller to 1 ULP to
     * a {@code double} has no effect. Nevertheless we use a smaller value as a safety because:
     *
     * <ul>
     *   <li>We perform our checks using the points given by the user, but we don't know how
     *       representative those points are.</li>
     *   <li>We perform our checks using an approximation which may result in slightly different
     *       decisions (false positives) than what we would got by a more robust check.</li>
     * </ul>
     *
     * This arbitrary threshold value may change in any future SIS version according experience gained.
     *
     * <div class="note"><b>Note:</b>
     * A similar constant exists in {@code org.apache.sis.referencing.operation.matrix.GeneralMatrix}.
     * </div>
     */
    private static final double ZERO_THRESHOLD = 1E-14;

    /**
     * The slope along the <var>x</var> values. This coefficient appears in the plane equation
     * <var><b><u>sx</u></b></var>⋅<var>x</var> + <var>sy</var>⋅<var>y</var> + <var>z₀</var>.
     */
    private double sx;

    /**
     * The slope along the <var>y</var> values. This coefficient appears in the plane equation
     * <var>sx</var>⋅<var>x</var> + <var><b><u>sy</u></b></var>⋅<var>y</var> + <var>z₀</var>.
     */
    private double sy;

    /**
     * The <var>z</var> value at (<var>x</var>,<var>y</var>) = (0,0). This coefficient appears in the plane equation
     * <var>sx</var>⋅<var>x</var> + <var>sy</var>⋅<var>y</var> + <b><u><var>z₀</var></u></b>.
     */
    private double z0;

    /**
     * Constructs a new plane with all coefficients initialized to {@link Double#NaN}.
     */
    public Plane() {
        sx = sy = z0 = Double.NaN;
    }

    /**
     * Constructs a new plane initialized to the given coefficients.
     *
     * @param sx The slope along the <var>x</var> values.
     * @param sy The slope along the <var>y</var> values.
     * @param z0 The <var>z</var> value at (<var>x</var>,<var>y</var>) = (0,0).
     *
     * @see #setEquation(double, double, double)
     */
    public Plane(final double sx, final double sy, final double z0) {
        this.sx = sx;
        this.sy = sy;
        this.z0 = z0;
    }

    /**
     * Returns the slope along the <var>x</var> values. This coefficient appears in the plane equation
     * <var><b><u>sx</u></b></var>⋅<var>x</var> + <var>sy</var>⋅<var>y</var> + <var>z₀</var>.
     *
     * @return The <var>sx</var> term.
     */
    public final double slopeX() {
        return sx;
    }

    /**
     * Returns the slope along the <var>y</var> values. This coefficient appears in the plane equation
     * <var>sx</var>⋅<var>x</var> + <var><b><u>sy</u></b></var>⋅<var>y</var> + <var>z₀</var>.
     *
     * @return The <var>sy</var> term.
     */
    public final double slopeY() {
        return sy;
    }

    /**
     * Returns the <var>z</var> value at (<var>x</var>,<var>y</var>) = (0,0). This coefficient appears in the
     * plane equation <var>sx</var>⋅<var>x</var> + <var>sy</var>⋅<var>y</var> + <b><var>z₀</var></b>.
     *
     * @return The <var>z₀</var> term.
     *
     * @see #z(double, double)
     */
    public final double z0() {
        return z0;
    }

    /**
     * Computes the <var>x</var> value for the specified (<var>y</var>,<var>z</var>) point.
     * The <var>x</var> value is computed using the following equation:
     *
     * <blockquote>x(y,z) = (z - ({@linkplain #z0() z₀} + {@linkplain #slopeY() sy}⋅y)) / {@linkplain #slopeX() sx}</blockquote>
     *
     * @param y The <var>y</var> value where to compute <var>x</var>.
     * @param z The <var>z</var> value where to compute <var>x</var>.
     * @return  The <var>x</var> value.
     */
    public final double x(final double y, final double z) {
        return (z - (z0 + sy*y)) / sx;
    }

    /**
     * Computes the <var>y</var> value for the specified (<var>x</var>,<var>z</var>) point.
     * The <var>y</var> value is computed using the following equation:
     *
     * <blockquote>y(x,z) = (z - ({@linkplain #z0() z₀} + {@linkplain #slopeX() sx}⋅x)) / {@linkplain #slopeY() sy}</blockquote>
     *
     * @param x The <var>x</var> value where to compute <var>y</var>.
     * @param z The <var>z</var> value where to compute <var>y</var>.
     * @return  The <var>y</var> value.
     */
    public final double y(final double x, final double z) {
        return (z - (z0 + sx*x)) / sy;
    }

    /**
     * Computes the <var>z</var> value for the specified (<var>x</var>,<var>y</var>) point.
     * The <var>z</var> value is computed using the following equation:
     *
     * <blockquote>z(x,y) = {@linkplain #slopeX() sx}⋅x + {@linkplain #slopeY() sy}⋅y + {@linkplain #z0() z₀}</blockquote>
     *
     * @param x The <var>x</var> value where to compute <var>z</var>.
     * @param y The <var>y</var> value where to compute <var>z</var>.
     * @return  The <var>z</var> value.
     *
     * @see #z0()
     */
    public final double z(final double x, final double y) {
        return z0 + sx*x + sy*y;
    }

    /**
     * Sets the equation of this plane to the given coefficients.
     *
     * @param sx The slope along the <var>x</var> values.
     * @param sy The slope along the <var>y</var> values.
     * @param z0 The <var>z</var> value at (<var>x</var>,<var>y</var>) = (0,0).
     */
    public void setEquation(final double sx, final double sy, final double z0) {
        this.sx = sx;
        this.sy = sy;
        this.z0 = z0;
    }

    /**
     * Computes the plane's coefficients from the given ordinate values.
     * This method uses a linear regression in the least-square sense,
     * with the assumption that the (<var>x</var>,<var>y</var>) values are precise
     * and all uncertainty is in <var>z</var>.
     *
     * <p>{@link Double#NaN} values are ignored.
     * The result is undetermined if all points are colinear.</p>
     *
     * @param  x vector of <var>x</var> coordinates.
     * @param  y vector of <var>y</var> coordinates.
     * @param  z vector of <var>z</var> values.
     * @return An estimation of the Pearson correlation coefficient.
     * @throws IllegalArgumentException if <var>x</var>, <var>y</var> and <var>z</var> do not have the same length.
     */
    public double fit(final double[] x, final double[] y, final double[] z) {
        // Do not invoke an overrideable method with our tricky iterable.
        return fit(new CompoundDirectPositions(x, y, z), true, true, true);
    }

    /**
     * Computes the plane's coefficients from the given sequence of points.
     * This method uses a linear regression in the least-square sense,
     * with the assumption that the (<var>x</var>,<var>y</var>) values are precise
     * and all uncertainty is in <var>z</var>.
     *
     * <p>Points shall be three dimensional with ordinate values in the (<var>x</var>,<var>y</var>,<var>z</var>) order.
     * {@link Double#NaN} ordinate values are ignored.
     * The result is undetermined if all points are colinear.</p>
     *
     * @param  points The three-dimensional points.
     * @return An estimation of the Pearson correlation coefficient.
     * @throws MismatchedDimensionException if a point is not three-dimensional.
     */
    public double fit(final Iterable<? extends DirectPosition> points) {
        return fit(points, true, true, true);
    }

    /**
     * Implementation of public {@code fit(…)} methods.
     * This method needs to iterate over the points two times:
     * one for computing the coefficients, and one for the computing the Pearson coefficient.
     * The second pass can also opportunistically checks if some small coefficients can be replaced by zero.
     */
    private double fit(final Iterable<? extends DirectPosition> points,
            boolean detectZeroSx, boolean detectZeroSy, boolean detectZeroZ0)
    {
        int i = 0, n = 0;
        final DoubleDouble sum_x  = new DoubleDouble();
        final DoubleDouble sum_y  = new DoubleDouble();
        final DoubleDouble sum_z  = new DoubleDouble();
        final DoubleDouble sum_xx = new DoubleDouble();
        final DoubleDouble sum_yy = new DoubleDouble();
        final DoubleDouble sum_xy = new DoubleDouble();
        final DoubleDouble sum_zx = new DoubleDouble();
        final DoubleDouble sum_zy = new DoubleDouble();
        final DoubleDouble xx     = new DoubleDouble();
        final DoubleDouble yy     = new DoubleDouble();
        final DoubleDouble xy     = new DoubleDouble();
        final DoubleDouble zx     = new DoubleDouble();
        final DoubleDouble zy     = new DoubleDouble();
        for (final DirectPosition p : points) {
            final int dimension = p.getDimension();
            if (dimension != DIMENSION) {
                throw new MismatchedDimensionException(Errors.format(
                        Errors.Keys.MismatchedDimension_3, "points[" + i + ']', DIMENSION, dimension));
            }
            i++;
            final double x = p.getOrdinate(0); if (Double.isNaN(x)) continue;
            final double y = p.getOrdinate(1); if (Double.isNaN(y)) continue;
            final double z = p.getOrdinate(2); if (Double.isNaN(z)) continue;
            xx.setToProduct(x, x);
            yy.setToProduct(y, y);
            xy.setToProduct(x, y);
            zx.setToProduct(z, x);
            zy.setToProduct(z, y);
            sum_x.add(x);
            sum_y.add(y);
            sum_z.add(z);
            sum_xx.add(xx);
            sum_yy.add(yy);
            sum_xy.add(xy);
            sum_zx.add(zx);
            sum_zy.add(zy);
            n++;
        }
        /*
         *    ( sum_zx - sum_z*sum_x )  =  sx*(sum_xx - sum_x*sum_x) + sy*(sum_xy - sum_x*sum_y)
         *    ( sum_zy - sum_z*sum_y )  =  sx*(sum_xy - sum_x*sum_y) + sy*(sum_yy - sum_y*sum_y)
         */
        zx.setFrom(sum_x); zx.divide(-n, 0); zx.multiply(sum_z); zx.add(sum_zx);    // zx = sum_zx - sum_z*sum_x/n
        zy.setFrom(sum_y); zy.divide(-n, 0); zy.multiply(sum_z); zy.add(sum_zy);    // zy = sum_zy - sum_z*sum_y/n
        xx.setFrom(sum_x); xx.divide(-n, 0); xx.multiply(sum_x); xx.add(sum_xx);    // xx = sum_xx - sum_x*sum_x/n
        xy.setFrom(sum_y); xy.divide(-n, 0); xy.multiply(sum_x); xy.add(sum_xy);    // xy = sum_xy - sum_x*sum_y/n
        yy.setFrom(sum_y); yy.divide(-n, 0); yy.multiply(sum_y); yy.add(sum_yy);    // yy = sum_yy - sum_y*sum_y/n
        /*
         * den = (xy*xy - xx*yy)
         */
        final DoubleDouble tmp = new DoubleDouble(xx); tmp.multiply(yy);
        final DoubleDouble den = new DoubleDouble(xy); den.multiply(xy);
        den.subtract(tmp);
        /*
         * sx = (zy*xy - zx*yy) / den
         * sy = (zx*xy - zy*xx) / den
         * z₀ = (sum_z - (sx*sum_x + sy*sum_y)) / n
         */
        final DoubleDouble sx = new DoubleDouble(zy); sx.multiply(xy); tmp.setFrom(zx); tmp.multiply(yy); sx.subtract(tmp); sx.divide(den);
        final DoubleDouble sy = new DoubleDouble(zx); sy.multiply(xy); tmp.setFrom(zy); tmp.multiply(xx); sy.subtract(tmp); sy.divide(den);
        final DoubleDouble z0 = new DoubleDouble(sy);
        z0.multiply(sum_y);
        tmp.setFrom(sx);
        tmp.multiply(sum_x);
        tmp.add(z0);
        z0.setFrom(sum_z);
        z0.subtract(tmp);
        z0.divide(n, 0);
        /*
         * At this point, the model is computed. Now computes an estimation of the Pearson
         * correlation coefficient. Note that both the z array and the z computed from the
         * model have the same average, called sum_z below (the name is not true anymore).
         *
         * We do not use double-double arithmetic here since the Pearson coefficient is
         * for information purpose (quality estimation).
         */
        final double mean_x = sum_x.value / n;
        final double mean_y = sum_y.value / n;
        final double mean_z = sum_z.value / n;
        final double offset = abs((sx.value * mean_x + sy.value * mean_y) + z0.value); // Offsetted z₀ - see comment before usage.
        double sum_ds2 = 0, sum_dz2 = 0, sum_dsz = 0;
        for (final DirectPosition p : points) {
            final double x = (p.getOrdinate(0) - mean_x) * sx.value;
            final double y = (p.getOrdinate(1) - mean_y) * sy.value;
            final double z = (p.getOrdinate(2) - mean_z);
            final double s = x + y;
            if (!Double.isNaN(s) && !Double.isNaN(z)) {
                sum_ds2 += s * s;
                sum_dz2 += z * z;
                sum_dsz += s * z;
            }
            /*
             * Algorithm for detecting if a coefficient should be zero:
             * If for every points given by the user, adding (sx⋅x) in (sx⋅x + sy⋅y + z₀) does not make any difference
             * because (sx⋅x) is smaller than 1 ULP of (sy⋅y + z₀), then it is not worth adding it and  sx  can be set
             * to zero. The same rational applies to (sy⋅y) and z₀.
             *
             * Since we work with differences from the means, the  z = sx⋅x + sy⋅y + z₀  equation can be rewritten as:
             *
             *     Δz = sx⋅Δx + sy⋅Δy + (sx⋅mx + sy⋅my + z₀ - mz)    where the term between (…) is close to zero.
             *
             * The check for (sx⋅Δx) and (sy⋅Δy) below ignore the (…) term since it is close to zero.
             * The check for  z₀  is derived from an equation without the  -mz  term.
             */
            if (detectZeroSx && abs(x) >= ulp(y * ZERO_THRESHOLD)) detectZeroSx = false;
            if (detectZeroSy && abs(y) >= ulp(x * ZERO_THRESHOLD)) detectZeroSy = false;
            if (detectZeroZ0 && offset >= ulp(s * ZERO_THRESHOLD)) detectZeroZ0 = false;
        }
        /*
         * Store the result only when we are done, so we have a "all or nothing" behavior.
         * We invoke the setEquation(sx, sy, z₀) method in case the user override it.
         */
        setEquation(detectZeroSx ? 0 : sx.value,
                    detectZeroSy ? 0 : sy.value,
                    detectZeroZ0 ? 0 : z0.value);
        return Math.min(sum_dsz / sqrt(sum_ds2 * sum_dz2), 1);
    }

    /**
     * Returns a clone of this plane.
     *
     * @return A clone of this plane.
     */
    @Override
    public Plane clone() {
        try {
            return (Plane) super.clone();
        } catch (CloneNotSupportedException exception) {
            throw new AssertionError(exception);
        }
    }

    /**
     * Compares this plane with the specified object for equality.
     *
     * @param object The object to compare with this plane for equality.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object) {
        if (object != null && getClass() == object.getClass()) {
            final Plane that = (Plane) object;
            return Numerics.equals(this.z0,  that.z0 ) &&
                   Numerics.equals(this.sx, that.sx) &&
                   Numerics.equals(this.sy, that.sy);
        } else {
            return false;
        }
    }

    /**
     * Returns a hash code value for this plane.
     */
    @Override
    public int hashCode() {
        return Numerics.hashCode(serialVersionUID
                     ^ (Double.doubleToLongBits(z0)
                + 31 * (Double.doubleToLongBits(sx)
                + 31 * (Double.doubleToLongBits(sy)))));
    }

    /**
     * Returns a string representation of this plane.
     * The string will contains the plane's equation, as below:
     *
     * <blockquote>
     *     <var>z</var>(<var>x</var>,<var>y</var>) = {@linkplain #slopeX() sx}⋅<var>x</var>
     *     + {@linkplain #slopeY() sy}⋅<var>y</var> + {@linkplain #z0() z₀}
     * </blockquote>
     */
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder(60).append("z(x,y) = ");
        String separator = "";
        if (sx != 0) {
            buffer.append(sx).append("⋅x");
            separator = " + ";
        }
        if (sy != 0) {
            buffer.append(separator).append(sy).append("⋅y");
            separator = " + ";
        }
        return buffer.append(separator).append(z0).toString();
    }
}
