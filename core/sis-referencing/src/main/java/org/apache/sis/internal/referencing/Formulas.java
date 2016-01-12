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
package org.apache.sis.internal.referencing;

import org.apache.sis.util.Static;
import org.apache.sis.measure.Latitude;
import org.apache.sis.internal.util.Numerics;

import static java.lang.Math.*;
import static org.apache.sis.math.MathFunctions.atanh;
import static org.apache.sis.internal.metadata.ReferencingServices.NAUTICAL_MILE;


/**
 * Miscellaneous numerical utilities which should not be put in public API.
 * This class contains methods that depend on hard-coded arbitrary tolerance threshold, and we
 * do not want to expose publicly those arbitrary values (or at least not in a too direct way).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 */
public final class Formulas extends Static {
    /**
     * Default tolerance threshold for comparing ordinate values in a projected CRS,
     * assuming that the unit of measurement is metre. This constant determines also
     * (indirectly) the minimum accuracy of iterative methods in map projections.
     *
     * @see #ANGULAR_TOLERANCE
     * @see org.apache.sis.internal.util.Numerics#COMPARISON_THRESHOLD
     */
    public static final double LINEAR_TOLERANCE = 0.01;

    /**
     * Default tolerance threshold for comparing ordinate values in a geographic CRS,
     * assuming that the unit of measurement is decimal degrees and using the standard
     * nautical mile length.
     *
     * <p>For a {@link #LINEAR_TOLERANCE} of 1 centimetre, this is slightly less than 1E-7°.</p>
     *
     * @see #LINEAR_TOLERANCE
     * @see org.apache.sis.internal.util.Numerics#COMPARISON_THRESHOLD
     */
    public static final double ANGULAR_TOLERANCE = LINEAR_TOLERANCE / (NAUTICAL_MILE * 60);

    /**
     * The maximal longitude value before normalization if a centimetric precision is desired.
     * This is about 4×10⁸ degrees.
     *
     * @see org.apache.sis.measure.Longitude#normalize(double)
     */
    public static final double LONGITUDE_MAX = (1L << Numerics.SIGNIFICAND_SIZE) * ANGULAR_TOLERANCE;

    /**
     * The length of a <cite>Julian year</cite> in milliseconds.
     * From Wikipedia, <cite>"In astronomy, a Julian year (symbol: <b>a</b>) is a unit of measurement of time
     * defined as exactly 365.25 days of 86,400 SI seconds each."</cite>.
     *
     * @see <a href="http://en.wikipedia.org/wiki/Julian_year_%28astronomy%29">Wikipedia: Julian year (astronomy)</a>
     */
    public static final long JULIAN_YEAR_LENGTH = 31557600000L;

    /**
     * Maximum number of iterations for iterative computations.
     */
    public static final int MAXIMUM_ITERATIONS = 15;

    /**
     * Do not allow instantiation of this class.
     */
    private Formulas() {
    }

    /**
     * Returns 3ⁿ for very small (less than 10) positive values of <var>n</var>.
     * Note that this method overflow for any value equals or greater than 20.
     *
     * @param n The exponent.
     * @return 3ⁿ
     *
     * @see org.apache.sis.math.DecimalFunctions#pow10(int)
     *
     * @since 0.5
     */
    public static int pow3(int n) {
        assert n >= 0 && n <= 19 : n;
        int p = 1;
        while (--n >= 0) {
            p *= 3;
        }
        return p;
    }

    /**
     * Returns {@code true} if {@code ymin} is the south pole and {@code ymax} is the north pole.
     *
     * @param ymin The minimal latitude to test.
     * @param ymax The maximal latitude to test.
     * @return {@code true} if the given latitudes are south pole to north pole respectively.
     */
    public static boolean isPoleToPole(final double ymin, final double ymax) {
        return abs(ymin - Latitude.MIN_VALUE) <= ANGULAR_TOLERANCE &&
               abs(ymax - Latitude.MAX_VALUE) <= ANGULAR_TOLERANCE;
    }

    /**
     * Returns the radius of a hypothetical sphere having the same surface than the ellipsoid
     * specified by the given axis length.
     *
     * @param  a The semi-major axis length.
     * @param  b The semi-minor axis length.
     * @return The radius of a sphere having the same surface than the specified ellipsoid.
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
     * Computes the semi-minor axis length from the given semi-major axis and inverse flattening factor.
     *
     * @param  semiMajorAxis     The semi-major axis length.
     * @param  inverseFlattening The inverse flattening factor.
     * @return The semi-minor axis length.
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
     * @param  semiMajorAxis The semi-major axis length.
     * @param  semiMinorAxis The semi-minor axis length.
     * @return The inverse flattening factor.
     */
    public static double getInverseFlattening(final double semiMajorAxis, final double semiMinorAxis) {
        /*
         * Note: double-double arithmetic here sometime change the last digit. We ignore for now.
         * We may consider using double-double arithmetic in a future SIS version, not for more
         * accurate map projection but rather for being able to find back the original value after
         * we convert back and forward betwen inverse flattening and semi-minor axis length.
         */
        return semiMajorAxis / (semiMajorAxis - semiMinorAxis);
    }
}
