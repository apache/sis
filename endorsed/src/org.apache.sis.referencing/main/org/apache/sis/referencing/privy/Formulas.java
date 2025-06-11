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
package org.apache.sis.referencing.privy;

import static java.lang.Math.*;
import org.opengis.referencing.datum.Ellipsoid;
import org.apache.sis.util.Static;
import org.apache.sis.measure.Latitude;
import org.apache.sis.util.privy.Numerics;
import org.apache.sis.system.Configuration;
import org.apache.sis.referencing.datum.DefaultEllipsoid;
import static org.apache.sis.math.MathFunctions.atanh;
import static org.apache.sis.metadata.privy.ReferencingServices.NAUTICAL_MILE;
import static org.apache.sis.metadata.privy.ReferencingServices.AUTHALIC_RADIUS;


/**
 * Miscellaneous numerical utilities which should not be put in public API.
 * This class contains methods that depend on hard-coded arbitrary tolerance threshold, and we
 * do not want to expose publicly those arbitrary values (or at least not in a too direct way).
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class Formulas extends Static {
    /**
     * Default tolerance threshold for comparing coordinate values in a projected CRS,
     * assuming that the unit of measurement is metre. This constant determines also
     * (indirectly) the minimum accuracy of iterative methods in map projections.
     *
     * <h4>Maintenance</h4>
     * If this value is modified, then all usages of this constant should be verified.
     * Some usages may need to be compensated. For example, {@code GeodesicsOnEllipsoid}
     * uses a millimetric precision by dividing the tolerance by 10 or more. We way want
     * to keep the same precision there even if {@code LINEAR_TOLERANCE} was made smaller.
     *
     * @see #ANGULAR_TOLERANCE
     * @see org.apache.sis.util.privy.Numerics#COMPARISON_THRESHOLD
     */
    @Configuration
    public static final double LINEAR_TOLERANCE = 0.01;

    /**
     * Default tolerance threshold for comparing coordinate values in a geographic CRS,
     * assuming that the unit of measurement is decimal degrees and using the standard
     * nautical mile length.
     *
     * <p>For a {@link #LINEAR_TOLERANCE} of 1 centimetre, this is slightly less than 1E-7°.</p>
     *
     * @see #LINEAR_TOLERANCE
     * @see org.apache.sis.util.privy.Numerics#COMPARISON_THRESHOLD
     */
    @Configuration
    public static final double ANGULAR_TOLERANCE = LINEAR_TOLERANCE / (NAUTICAL_MILE * 60);

    /**
     * Default tolerance threshold for comparing coordinate values in temporal CRS,
     * assuming that the unit of measurement is second. Current value is arbitrary
     * and may change in any future Apache SIS version.
     */
    @Configuration
    public static final double TEMPORAL_TOLERANCE = 60;             // One minute.

    /**
     * The maximal longitude value before normalization if a centimetric precision is desired.
     * This is about 4×10⁸ degrees.
     *
     * @see org.apache.sis.measure.Longitude#normalize(double)
     */
    public static final double LONGITUDE_MAX = Numerics.MAX_INTEGER_CONVERTIBLE_TO_DOUBLE/2 * ANGULAR_TOLERANCE;

    /**
     * Maximum number of iterations for iterative computations. Defined in this {@code Formulas} class as a default value,
     * but some classes may use a derived value (for example twice this amount). This constant is mostly useful for identifying
     * places where iterations occur.
     *
     * <p>Current value has been determined empirically for allowing {@code GeodesicsOnEllipsoidTest} to pass.</p>
     */
    @Configuration
    public static final int MAXIMUM_ITERATIONS = 18;

    /**
     * Whether to use {@link Math#fma(double, double, double)} for performance reasons.
     * We do not use this flag when the goal is to get better accuracy rather than performance.
     * Use of FMA brings performance benefits on machines having hardware support,
     * but come at a high cost on older machines without hardware support.
     */
    @Configuration
    public static final boolean USE_FMA = false;

    /**
     * Do not allow instantiation of this class.
     */
    private Formulas() {
    }

    /**
     * Returns {@code true} if {@code ymin} is the south pole and {@code ymax} is the north pole.
     *
     * @param  ymin  the minimal latitude to test.
     * @param  ymax  the maximal latitude to test.
     * @return {@code true} if the given latitudes are south pole to north pole respectively.
     */
    public static boolean isPoleToPole(final double ymin, final double ymax) {
        return abs(ymin - Latitude.MIN_VALUE) <= ANGULAR_TOLERANCE &&
               abs(ymax - Latitude.MAX_VALUE) <= ANGULAR_TOLERANCE;
    }

    /**
     * Returns whether ellipsoidal formulas should be used for the given ellipsoid.
     * This method checks if the ellipsoid is a sphere with an arbitrary tolerance threshold.
     * If the semi-major or semi-minor axis length is NaN, this method returns {@code false}.
     *
     * @param  ellipsoid  the ellipsoid to test.
     * @return whether ellipsoidal formulas should be used.
     *
     * @see Ellipsoid#isSphere()
     */
    public static boolean isEllipsoidal(final Ellipsoid ellipsoid) {
        if (ellipsoid.isSphere()) {
            return false;
        }
        final double semiMajor = ellipsoid.getSemiMajorAxis();
        final double semiMinor = ellipsoid.getSemiMinorAxis();      // No need to check the unit of measurement.
        return Math.abs(semiMajor - semiMinor) > semiMajor * (LINEAR_TOLERANCE / AUTHALIC_RADIUS);
    }

    /**
     * Returns the size of a planet described by the given ellipsoid compared to earth.
     * This method returns a ratio of given planet authalic radius compared to WGS84.
     * This can be used for adjusting {@link #LINEAR_TOLERANCE} and {@link #ANGULAR_TOLERANCE} to another planet.
     *
     * @param  planet  ellipsoid of the other planet to compare to Earth, or {@code null}.
     * @return ratio of planet authalic radius on WGS84 authalic radius, or {@code NaN} if the given ellipsoid is null.
     */
    public static double scaleComparedToEarth(final Ellipsoid planet) {
        return getAuthalicRadius(planet) / 6371007.180918474;
    }

    /**
     * Returns the radius of a hypothetical sphere having the same surface as the given ellipsoid.
     *
     * @param  ellipsoid  the ellipsoid for which to get the radius, or {@code null}.
     * @return the authalic radius, or {@link Double#NaN} if the given ellipsoid is null.
     */
    public static double getAuthalicRadius(final Ellipsoid ellipsoid) {
        if (ellipsoid == null) {
            return Double.NaN;
        } else if (ellipsoid instanceof DefaultEllipsoid) {
            return ((DefaultEllipsoid) ellipsoid).getAuthalicRadius();      // Give a chance to subclasses to override.
        } else {
            return getAuthalicRadius(ellipsoid.getSemiMajorAxis(),
                                     ellipsoid.getSemiMinorAxis());
        }
    }

    /**
     * Returns the radius of a hypothetical sphere having the same surface as the ellipsoid
     * specified by the given axis length.
     *
     * @param  a  the semi-major axis length.
     * @param  b  the semi-minor axis length.
     * @return the radius of a sphere having the same surface as the specified ellipsoid.
     *
     * @see org.apache.sis.referencing.datum.DefaultEllipsoid#getAuthalicRadius()
     */
    public static double getAuthalicRadius(final double a, final double b) {
        if (a != b) {
            final double f = 1 - b/a;
            final double e = sqrt(2*f - f*f);
            return sqrt(0.5 * (a*a + b*b*atanh(e)/e));
        } else {
            return a;
        }
    }

    /**
     * Returns the radius of the conformal sphere at a given latitude.
     * The radius of conformal sphere is computed as below:
     *
     * <blockquote>Rc = √(1 – ℯ²) / (1 – ℯ²sin²φ)  where  ℯ² = 1 - (b/a)²</blockquote>
     *
     * This is a function of latitude and therefore not constant.
     *
     * @param  ellipsoid  the ellipsoid for which to compute the radius of conformal sphere.
     * @param  φ          the latitude in radians where to compute the radius of conformal sphere.
     * @return radius of the conformal sphere at latitude φ.
     */
    public static double radiusOfConformalSphere(final Ellipsoid ellipsoid, final double φ) {
        final double sinφ = Math.sin(φ);
        final double a = ellipsoid.getSemiMajorAxis();
        final double r = ellipsoid.getSemiMinorAxis() / a;
        return a * (r / (1 - (1 - r*r) * (sinφ*sinφ)));
    }

    /**
     * Returns the geocentric radius at the given geodetic latitude.
     *
     * @param  ellipsoid  the ellipsoid for which to compute the radius.
     * @param  φ          the geodetic latitude in radians where to compute the radius.
     * @return radius at the geodetic latitude φ.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Earth_radius#Geocentric_radius">Geocentric radius on Wikipedia</a>
     */
    public static double geocentricRadius(final Ellipsoid ellipsoid, final double φ) {
        final double a  = ellipsoid.getSemiMajorAxis();
        final double b  = ellipsoid.getSemiMinorAxis();
        double at = a * Math.cos(φ); at *= at;
        double bt = b * Math.sin(φ); bt *= bt;
        return Math.sqrt((a*a*at + b*b*bt) / (at + bt));
    }

    /**
     * Computes the semi-minor axis length from the given semi-major axis and inverse flattening factor.
     *
     * @param  semiMajorAxis      the semi-major axis length.
     * @param  inverseFlattening  the inverse flattening factor.
     * @return the semi-minor axis length.
     */
    public static double getSemiMinor(final double semiMajorAxis, final double inverseFlattening) {
        /*
         * Note: double-double arithmetic does not increase the accuracy here, unless the inverse flattening
         * factor given to this method is very high (i.e. the planet is very close to a perfect sphere).
         */
        return semiMajorAxis * (1 - 1/inverseFlattening);
    }

    /**
     * Computes the inverse flattening factor from the given axis lengths.
     *
     * @param  semiMajorAxis  the semi-major axis length.
     * @param  semiMinorAxis  the semi-minor axis length.
     * @return the inverse flattening factor.
     */
    public static double getInverseFlattening(final double semiMajorAxis, final double semiMinorAxis) {
        /*
         * Note: double-double arithmetic here sometimes change the last digit. We ignore for now.
         * We may consider using double-double arithmetic in a future SIS version, not for more
         * accurate map projection but rather for being able to find back the original value after
         * we convert back and forward betwen inverse flattening and semi-minor axis length.
         */
        return semiMajorAxis / (semiMajorAxis - semiMinorAxis);
    }

    /**
     * Returns {@code sqrt(x² + y²)} for coordinate values on an ellipsoid of semi-major axis length of 1.
     * This method does not provides the accuracy guarantees offered by {@link Math#hypot(double, double)}.
     * However for values close to 1, this approximation seems to stay within 1 ULP of {@code Math.hypot(…)}.
     * We tested with random values in ranges up to [-6 … +6].
     *
     * <p>We define this method because {@link Math#hypot(double, double)} has been measured with JMH as 6 times
     * slower than {@link Math#sqrt(double)} on Java 14.  According posts on internet, the same performance cost
     * is observed in C/C++ too. Despite its cost, {@code hypot(…)} is generally recommended because computing a
     * hypotenuse from large magnitudes has accuracy problems. But in the context of {@code NormalizedProjection}
     * where semi-axis lengths are close to 1, input values should be (x,y) coordinates in the [−1 … +1] range.
     * The actual range may be greater (e.g. [−5 … +5]), but it still far from ranges requiring protection against
     * overflow.</p>
     *
     * <h4>Caution</h4>
     * We may not need the full {@code Math.hypot(x,y)} accuracy in the context of map projections on ellipsoids.
     * However, some projection formulas require that {@code fastHypot(x,y) ≥ max(|x|,|y|)}, otherwise normalizations
     * such as {@code x/hypot(x,y)} could result in values larger than 1, which in turn result in {@link Double#NaN}
     * when given to {@link Math#asin(double)}. The assumption on x, y and {@code sqrt(x²+y²)} relative magnitude is
     * broken when x=0 and |y| ≤ 1.4914711209038602E-154 or conversely. This method does not check for such cases;
     * it is caller responsibility to add this check is necessary, for example as below:
     *
     * {@snippet lang="java" :
     *     double D = max(fastHypot(x, y), max(abs(x), abs(y)));
     *     }
     *
     * According JMH, above check is 1.65 time slower than {@code fastHypot} without checks.
     * We define this {@code fastHypot(…)} method for tracing where {@code sqrt(x² + y²)} is used,
     * so we can verify if it is used in context where the inaccuracy is acceptable.
     *
     * <h4>When to use</h4>
     * We reserve this method to ellipsoidal formulas where approximations are used anyway. Implementations using
     * exact formulas, such as spherical formulas, should use {@link Math#hypot(double, double)} for its accuracy.
     *
     * @param  x    one side of the triangle. Should be approximately in the [-1 … +1] range.
     * @param  y  other side of the triangle. Should be approximately in the [-1 … +1] range.
     * @return hypotenuse, not smaller than {@code max(|x|,|y|)} unless the values are less than 1.5E-154.
     *
     * @see <a href="https://issues.apache.org/jira/browse/NUMBERS-143">Investigate Math.hypot for computing the absolute of a complex number</a>
     * @see <a href="https://scicomp.stackexchange.com/questions/27758/is-there-any-point-to-using-hypot-for-sqrt1c2-0-le-c-le-1-for-real/27766">Is
     *      there any point to using <code>hypot(1, c)</code> for <code>sqrt(1 + c²)</code>, 0 ≤ c ≤ 1</a>
     */
    public static double fastHypot(final double x, final double y) {
        return sqrt(x*x + y*y);
    }
}
