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
import java.util.Objects;
import java.util.Iterator;
import java.util.function.DoubleBinaryOperator;
import static java.lang.Math.abs;
import static java.lang.Math.sqrt;
import static java.lang.Math.ulp;
import org.opengis.geometry.DirectPosition;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.internal.shared.DoubleDouble;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.resources.Errors;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.coordinate.MismatchedDimensionException;


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
 * @version 1.4
 *
 * @see Line
 * @see org.apache.sis.referencing.operation.builder.LinearTransformBuilder
 *
 * @since 0.5
 */
public class Plane implements DoubleBinaryOperator, Cloneable, Serializable {
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
     * @see org.apache.sis.referencing.operation.matrix.GeneralMatrix#ZERO_THRESHOLD
     * @see Numerics#COMPARISON_THRESHOLD
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
     * @param sx  the slope along the <var>x</var> values.
     * @param sy  the slope along the <var>y</var> values.
     * @param z0  the <var>z</var> value at (<var>x</var>,<var>y</var>) = (0,0).
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
     * @return the <var>sx</var> term.
     */
    public final double slopeX() {
        return sx;
    }

    /**
     * Returns the slope along the <var>y</var> values. This coefficient appears in the plane equation
     * <var>sx</var>⋅<var>x</var> + <var><b><u>sy</u></b></var>⋅<var>y</var> + <var>z₀</var>.
     *
     * @return the <var>sy</var> term.
     */
    public final double slopeY() {
        return sy;
    }

    /**
     * Returns the <var>z</var> value at (<var>x</var>,<var>y</var>) = (0,0). This coefficient appears in the
     * plane equation <var>sx</var>⋅<var>x</var> + <var>sy</var>⋅<var>y</var> + <b><var>z₀</var></b>.
     *
     * @return the <var>z₀</var> term.
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
     * @param y the <var>y</var> value where to compute <var>x</var>.
     * @param z the <var>z</var> value where to compute <var>x</var>.
     * @return  the <var>x</var> value.
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
     * @param x the <var>x</var> value where to compute <var>y</var>.
     * @param z the <var>z</var> value where to compute <var>y</var>.
     * @return  the <var>y</var> value.
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
     * @param x the <var>x</var> value where to compute <var>z</var>.
     * @param y the <var>y</var> value where to compute <var>z</var>.
     * @return  the <var>z</var> value.
     *
     * @see #z0()
     */
    public final double z(final double x, final double y) {
        return z0 + sx*x + sy*y;
    }

    /**
     * Evaluates this equation for the given values. The default implementation delegates to
     * {@link #z(double,double) z(x,y)}, but subclasses may override with different formulas.
     * This method is provided for interoperability with libraries making use of {@link java.util.function}.
     *
     * @param  x  the first operand where to evaluate the function.
     * @param  y  the second operand where to evaluate the function.
     * @return the function value for the given operands.
     *
     * @since 1.0
     */
    @Override
    public double applyAsDouble(double x, double y) {
        return z(x, y);
    }

    /**
     * Sets the equation of this plane to the given coefficients.
     *
     * @param sx  the slope along the <var>x</var> values.
     * @param sy  the slope along the <var>y</var> values.
     * @param z0  the <var>z</var> value at (<var>x</var>,<var>y</var>) = (0,0).
     */
    public void setEquation(final double sx, final double sy, final double z0) {
        this.sx = sx;
        this.sy = sy;
        this.z0 = z0;
    }

    /**
     * Sets this plane from values of arbitrary {@code Number} type. This method is invoked by algorithms that
     * may produce other kind of numbers (for example with different precision) than the usual {@code double}
     * primitive type. The default implementation delegates to {@link #setEquation(double, double, double)},
     * but subclasses can override this method if they want to process other kind of numbers in a special way.
     *
     * @param sx  the slope along the <var>x</var> values.
     * @param sy  the slope along the <var>y</var> values.
     * @param z0  the <var>z</var> value at (<var>x</var>,<var>y</var>) = (0,0).
     *
     * @since 0.8
     */
    public void setEquation(final Number sx, final Number sy, final Number z0) {
        setEquation(sx.doubleValue(), sy.doubleValue(), z0.doubleValue());
    }

    /**
     * Computes the plane's coefficients from the given coordinate values.
     * This method uses a linear regression in the least-square sense, with the assumption that
     * the (<var>x</var>,<var>y</var>) values are precise and all uncertainty is in <var>z</var>.
     * {@link Double#NaN} values are ignored. The result is undetermined if all points are colinear.
     *
     * <p>The default implementation delegates to {@link #fit(Vector, Vector, Vector)}.</p>
     *
     * @param  x  vector of <var>x</var> coordinates.
     * @param  y  vector of <var>y</var> coordinates.
     * @param  z  vector of <var>z</var> values.
     * @return an estimation of the Pearson correlation coefficient.
     *         The closer this coefficient is to +1 or -1, the better the fit.
     * @throws IllegalArgumentException if <var>x</var>, <var>y</var> and <var>z</var> do not have the same length.
     */
    public double fit(final double[] x, final double[] y, final double[] z) {
        ArgumentChecks.ensureNonNull("x", x);
        ArgumentChecks.ensureNonNull("y", y);
        ArgumentChecks.ensureNonNull("z", z);
        return fit(new ArrayVector.Doubles(x), new ArrayVector.Doubles(y), new ArrayVector.Doubles(z));
    }

    /**
     * Computes the plane's coefficients from the given coordinate values.
     * This method uses a linear regression in the least-square sense, with the assumption that
     * the (<var>x</var>,<var>y</var>) values are precise and all uncertainty is in <var>z</var>.
     * {@link Double#NaN} values are ignored. The result is undetermined if all points are colinear.
     *
     * <p>The default implementation delegates to {@link #fit(Iterable)}.</p>
     *
     * @param  x  vector of <var>x</var> coordinates.
     * @param  y  vector of <var>y</var> coordinates.
     * @param  z  vector of <var>z</var> values.
     * @return an estimation of the Pearson correlation coefficient.
     *         The closer this coefficient is to +1 or -1, the better the fit.
     * @throws IllegalArgumentException if <var>x</var>, <var>y</var> and <var>z</var> do not have the same length.
     *
     * @since 0.8
     */
    public double fit(final Vector x, final Vector y, final Vector z) {
        ArgumentChecks.ensureNonNull("x", x);
        ArgumentChecks.ensureNonNull("y", y);
        ArgumentChecks.ensureNonNull("z", z);
        return fit(new CompoundDirectPositions(x, y, z));
    }

    /**
     * Computes the plane's coefficients from values distributed on a regular grid. Invoking this method
     * is equivalent (except for NaN handling) to invoking {@link #fit(Vector, Vector, Vector)} where all
     * vectors have a length of {@code nx} × {@code ny} and the <var>x</var> and <var>y</var> vectors have
     * the following content:
     *
     * <blockquote>
     * <table class="compact">
     *   <caption><var>x</var> and <var>y</var> vectors content</caption>
     *   <tr>
     *     <th><var>x</var> vector</th>
     *     <th><var>y</var> vector</th>
     *   </tr><tr>
     *     <td>
     *       0 1 2 3 4 5 … n<sub>x</sub>-1<br>
     *       0 1 2 3 4 5 … n<sub>x</sub>-1<br>
     *       0 1 2 3 4 5 … n<sub>x</sub>-1<br>
     *       …<br>
     *       0 1 2 3 4 5 … n<sub>x</sub>-1<br>
     *     </td><td>
     *       0 0 0 0 0 0 … 0<br>
     *       1 1 1 1 1 1 … 1<br>
     *       2 2 2 2 2 2 … 2<br>
     *       …<br>
     *       n<sub>y</sub>-1 n<sub>y</sub>-1 n<sub>y</sub>-1 … n<sub>y</sub>-1<br>
     *     </td>
     *   </tr>
     * </table>
     * </blockquote>
     *
     * This method uses a linear regression in the least-square sense, with the assumption that
     * the (<var>x</var>,<var>y</var>) values are precise and all uncertainty is in <var>z</var>.
     * The result is undetermined if all points are colinear.
     *
     * @param  nx  number of columns.
     * @param  ny  number of rows.
     * @param  z   values of a matrix of {@code nx} columns by {@code ny} rows organized in a row-major fashion.
     * @return an estimation of the Pearson correlation coefficient.
     *         The closer this coefficient is to +1 or -1, the better the fit.
     * @throws IllegalArgumentException if <var>z</var> does not have the expected length or if a <var>z</var>
     *         value is {@link Double#NaN}.
     *
     * @since 0.8
     */
    public double fit(final int nx, final int ny, final Vector z) {
        ArgumentChecks.ensureStrictlyPositive("nx", nx);
        ArgumentChecks.ensureStrictlyPositive("ny", ny);
        ArgumentChecks.ensureNonNull("z", z);
        final int length = Math.multiplyExact(nx, ny);
        if (z.size() != length) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.UnexpectedArrayLength_2, length, z.size()));
        }
        final Fit r = new Fit(nx, ny, z);
        r.resolve();
        final double p = r.correlation(nx, length, z, null);
        setEquation(r.sx, r.sy, r.z0);
        return p;
    }

    /**
     * Computes the plane's coefficients from the given sequence of points.
     * This method uses a linear regression in the least-square sense, with the assumption that
     * the (<var>x</var>,<var>y</var>) values are precise and all uncertainty is in <var>z</var>.
     * Points shall be three dimensional with coordinate values in the (<var>x</var>,<var>y</var>,<var>z</var>) order.
     * {@link Double#NaN} values are ignored. The result is undetermined if all points are colinear.
     *
     * @param  points  the three-dimensional points.
     * @return an estimation of the Pearson correlation coefficient.
     *         The closer this coefficient is to +1 or -1, the better the fit.
     * @throws MismatchedDimensionException if a point is not three-dimensional.
     */
    public double fit(final Iterable<? extends DirectPosition> points) {
        final Fit r = new Fit(Objects.requireNonNull(points));
        r.resolve();
        final double p = r.correlation(0, 0, null, points.iterator());
        /*
         * Store the result only when we are done, so we have a "all or nothing" behavior.
         * We invoke the setEquation(sx, sy, z₀) method in case the user override it.
         */
        setEquation(r.sx, r.sy, r.z0);
        return p;
    }

    /**
     * Computes the plane coefficients. This class needs to iterate over the points two times:
     * one for computing the coefficients, and one for the computing the Pearson coefficient.
     * The second pass can also opportunistically checks if some small coefficients can be replaced by zero.
     */
    private static final class Fit {
        private DoubleDouble sum_x  = DoubleDouble.ZERO;
        private DoubleDouble sum_y  = DoubleDouble.ZERO;
        private DoubleDouble sum_z  = DoubleDouble.ZERO;
        private DoubleDouble sum_xx = DoubleDouble.ZERO;
        private DoubleDouble sum_yy = DoubleDouble.ZERO;
        private DoubleDouble sum_xy = DoubleDouble.ZERO;
        private DoubleDouble sum_zx = DoubleDouble.ZERO;
        private DoubleDouble sum_zy = DoubleDouble.ZERO;
        private final int n;

        /** Solution of the plane equation. */ DoubleDouble sx, sy, z0;

        /**
         * Computes the values of all {@code sum_*} fields from randomly distributed points.
         * Value of all other fields are undetermined..
         */
        Fit(final Iterable<? extends DirectPosition> points) {
            int i = 0, n = 0;
            for (final DirectPosition p : points) {
                final int dimension = p.getDimension();
                if (dimension != DIMENSION) {
                    throw new MismatchedDimensionException(Errors.format(Errors.Keys.MismatchedDimension_3,
                                Strings.toIndexed("points", i), DIMENSION, dimension));
                }
                i++;
                final double x = p.getCoordinate(0); if (Double.isNaN(x)) continue;
                final double y = p.getCoordinate(1); if (Double.isNaN(y)) continue;
                final double z = p.getCoordinate(2); if (Double.isNaN(z)) continue;
                sum_x  = sum_x .add(x, false);
                sum_y  = sum_y .add(y, false);
                sum_z  = sum_z .add(z, false);
                sum_xx = sum_xx.add(DoubleDouble.product(x, x));
                sum_yy = sum_yy.add(DoubleDouble.product(y, y));
                sum_xy = sum_xy.add(DoubleDouble.product(x, y));
                sum_zx = sum_zx.add(DoubleDouble.product(z, x));
                sum_zy = sum_zy.add(DoubleDouble.product(z, y));
                n++;
            }
            this.n = n;
        }

        /**
         * Computes the values of all {@code sum_*} fields from the <var>z</var> values on a regular grid.
         * Value of all other fields are undetermined..
         */
        Fit(final int nx, final int ny, final Vector vz) {
            /*
             * Computes the sum of x, y and z values. Computes also the sum of x², y², x⋅y, z⋅x and z⋅y values.
             * When possible, we will avoid to compute the sum inside the loop and use the following identities
             * instead:
             *
             *           1 + 2 + 3 ... + n    =    n⋅(n+1)/2              (arithmetic series)
             *        1² + 2² + 3² ... + n²   =    n⋅(n+0.5)⋅(n+1)/3
             *
             * Note that for exclusive upper bound, we need to replace n by n-1 in above formulas.
             */
            int n = 0;
            for (int y=0; y<ny; y++) {
                for (int x=0; x<nx; x++) {
                    final double z = vz.doubleValue(n);
                    if (Double.isNaN(z)) {
                        throw new IllegalArgumentException(Errors.format(Errors.Keys.NotANumber_1, Strings.toIndexed("z", n)));
                    }
                    sum_z  = sum_z .add(z, false);
                    sum_zx = sum_zx.add(DoubleDouble.product(z, x));
                    sum_zy = sum_zy.add(DoubleDouble.product(z, y));
                    n++;
                }
            }
            sum_x  = DoubleDouble.of(n/2d, false).multiply(nx-1);       // Division by 2 is exact.
            sum_y  = DoubleDouble.of(n/2d, false).multiply(ny-1);
            sum_xx = DoubleDouble.of(n)          .multiply(nx-0.5, false).multiply(nx-1).divide(3);
            sum_yy = DoubleDouble.of(n)          .multiply(ny-0.5, false).multiply(ny-1).divide(3);
            sum_xy = DoubleDouble.of(n/4d, false).multiply(ny-1)         .multiply(nx-1);
            this.n = n;
        }

        /**
         * Computes the {@link #sx}, {@link #sy} and {@link #z0} values using the sums computed by the constructor.
         */
        private void resolve() {
            /*
             *    ( sum_zx - sum_z⋅sum_x )  =  sx⋅(sum_xx - sum_x⋅sum_x) + sy⋅(sum_xy - sum_x⋅sum_y)
             *    ( sum_zy - sum_z⋅sum_y )  =  sx⋅(sum_xy - sum_x⋅sum_y) + sy⋅(sum_yy - sum_y⋅sum_y)
             */
            var zx = sum_zx.subtract(sum_z.multiply(sum_x).divide(n));      // zx = sum_zx - sum_z⋅sum_x/n
            var zy = sum_zy.subtract(sum_z.multiply(sum_y).divide(n));      // zy = sum_zy - sum_z⋅sum_y/n
            var xx = sum_xx.subtract(sum_x.multiply(sum_x).divide(n));      // xx = sum_xx - sum_x⋅sum_x/n
            var xy = sum_xy.subtract(sum_x.multiply(sum_y).divide(n));      // xy = sum_xy - sum_x⋅sum_y/n
            var yy = sum_yy.subtract(sum_y.multiply(sum_y).divide(n));      // yy = sum_yy - sum_y⋅sum_y/n
            /*
             * den = (xy⋅xy - xx⋅yy)
             */
            final DoubleDouble den = xy.square().subtract(xx.multiply(yy));
            /*
             * sx = (zy⋅xy - zx⋅yy) / den
             * sy = (zx⋅xy - zy⋅xx) / den
             * z₀ = (sum_z - (sx⋅sum_x + sy⋅sum_y)) / n
             */
            sx = zy.multiply(xy).subtract(zx.multiply(yy)).divide(den);
            sy = zx.multiply(xy).subtract(zy.multiply(xx)).divide(den);
            z0 = sum_z.subtract(sx.multiply(sum_x).add(sy.multiply(sum_y))).divide(n);
        }

        /**
         * Computes an estimation of the Pearson correlation coefficient. We do not use double-double arithmetic
         * here since the Pearson coefficient is for information purpose (quality estimation).
         *
         * <p>Only one of ({@code nx}, {@code length}, {@code z}) tuple or {@code points} argument should be non-null.</p>
         */
        double correlation(final int nx, final int length, final Vector vz,
                           final Iterator<? extends DirectPosition> points)
        {
            boolean detectZeroSx = true;
            boolean detectZeroSy = true;
            boolean detectZeroZ0 = true;
            final double sx     = this.sx.doubleValue();
            final double sy     = this.sy.doubleValue();
            final double z0     = this.z0.doubleValue();
            final double mean_x = sum_x.doubleValue() / n;
            final double mean_y = sum_y.doubleValue() / n;
            final double mean_z = sum_z.doubleValue() / n;
            final double offset = abs((sx * mean_x + sy * mean_y) + z0);    // Offsetted z₀ - see comment before usage.
            int index = 0;
            double sum_ds2 = 0, sum_dz2 = 0, sum_dsz = 0;
            for (;;) {
                double x, y, z;
                if (vz != null) {
                    if (index >= length) break;
                    x = index % nx;
                    y = index / nx;
                    z = vz.doubleValue(index++);
                } else {
                    if (!points.hasNext()) break;
                    final DirectPosition p = points.next();
                    x = p.getCoordinate(0);
                    y = p.getCoordinate(1);
                    z = p.getCoordinate(2);
                }
                x = (x - mean_x) * sx;
                y = (y - mean_y) * sy;
                z = (z - mean_z);
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
            if (detectZeroSx) this.sx = DoubleDouble.ZERO;
            if (detectZeroSy) this.sy = DoubleDouble.ZERO;
            if (detectZeroZ0) this.z0 = DoubleDouble.ZERO;
            return Math.min(sum_dsz / sqrt(sum_ds2 * sum_dz2), 1);
        }
    }

    /**
     * Returns a clone of this plane.
     *
     * @return a clone of this plane.
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
     * @param  object  the object to compare with this plane for equality.
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
        return Long.hashCode(serialVersionUID
                     ^ (Double.doubleToLongBits(z0)
                + 31 * (Double.doubleToLongBits(sx)
                + 31 * (Double.doubleToLongBits(sy)))));
    }

    /**
     * Returns a string representation of this plane.
     * The string will contain the plane's equation, as below:
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
