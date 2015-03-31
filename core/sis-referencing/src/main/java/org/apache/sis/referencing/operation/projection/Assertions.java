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
package org.apache.sis.referencing.operation.projection;

import org.opengis.referencing.operation.Matrix;
import org.apache.sis.util.Static;

import static java.lang.Math.*;


/**
 * Static methods for assertions. This is used only when Java 1.4 assertions are enabled.
 * When a point has been projected using spherical formulas, compares with the same point
 * transformed using spherical formulas and throw an exception if the result differ.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
final class Assertions extends Static {
    /**
     * Maximum difference allowed when comparing the result of an inverse projections, in radians.
     * A value of 1E-7 radians is approximatively 0.5 kilometres.
     * Note that inverse projections are typically less accurate than forward projections.
     * This tolerance is set to such high value for avoiding too intrusive assertion errors.
     * This is okay only for catching gross programming errors.
     */
    private static final double INVERSE_TOLERANCE = 1E-7;

    /**
     * Maximum difference allowed when comparing the result of forward projections,
     * in distance on the unit ellipse. A value of 1E-7 is approximatively 0.1 metres.
     */
    private static final double FORWARD_TOLERANCE = 1E-7;

    /**
     * Maximum difference allowed between spherical and ellipsoidal formulas when
     * comparing derivatives. Units are metres.
     */
    private static final double DERIVATIVE_TOLERANCE = 1E-1;

    /**
     * Do not allows instantiation of this class.
     */
    private Assertions() {
    }

    /**
     * Checks if transform using spherical formulas produces the same result than ellipsoidal formulas.
     * This method is invoked during assertions only.
     *
     * @param  expected  The (easting,northing) computed by ellipsoidal formulas.
     * @param  offset    Index of the coordinate in the {@code expected} array.
     * @param  x         The easting computed by spherical formulas on the unit sphere.
     * @param  y         The northing computed by spherical formulas on the unit sphere.
     * @return Always {@code true}.
     * @throws ProjectionException if the comparison failed.
     */
    static boolean checkTransform(final double[] expected, final int offset, final double x, final double y)
            throws ProjectionException
    {
        if (expected != null) {
            compare("x", expected[offset  ], x, FORWARD_TOLERANCE);
            compare("y", expected[offset+1], y, FORWARD_TOLERANCE);
        }
        return true;
    }

    /**
     * Checks if inverse transform using spherical formulas produces the same result than ellipsoidal formulas.
     * This method is invoked during assertions only.
     *
     * @param  expected  The (longitude,latitude) computed by ellipsoidal formulas.
     * @param  offset    Index of the coordinate in the {@code expected} array.
     * @param  λ         The longitude computed by spherical formulas, in radians.
     * @param  φ         The latitude computed by spherical formulas, in radians.
     * @return Always {@code true}.
     * @throws ProjectionException if the comparison failed.
     */
    static boolean checkInverseTransform(final double[] expected, final int offset, final double λ, final double φ)
            throws ProjectionException
    {
        compare("latitude",  expected[offset+1], φ, INVERSE_TOLERANCE);
        compare("longitude", expected[offset  ], λ, INVERSE_TOLERANCE / abs(cos(φ)));
        return true;
    }

    /**
     * Checks if derivatives using spherical formulas produces the same result than ellipsoidal formulas.
     * This method is invoked during assertions only. The spherical formulas are used for the "expected"
     * results since they are simpler than the ellipsoidal formulas.
     */
    @SuppressWarnings("null")
    static boolean checkDerivative(final Matrix spherical, final Matrix ellipsoidal) throws ProjectionException {
        if (spherical != null || ellipsoidal != null) { // NullPointerException is ok if only one is null.
            compare("m00", spherical.getElement(0,0), ellipsoidal.getElement(0,0), DERIVATIVE_TOLERANCE);
            compare("m01", spherical.getElement(0,1), ellipsoidal.getElement(0,1), DERIVATIVE_TOLERANCE);
            compare("m10", spherical.getElement(1,0), ellipsoidal.getElement(1,0), DERIVATIVE_TOLERANCE);
            compare("m11", spherical.getElement(1,1), ellipsoidal.getElement(1,1), DERIVATIVE_TOLERANCE);
        }
        return true;
    }

    /**
     * Compares two value for equality up to some tolerance threshold. This is used during assertions only.
     * The comparison does not fail if at least one value to compare is {@link Double#NaN} or infinity.
     *
     * <p><strong>Hack:</strong> if the {@code variable} name starts by lower-case {@code L}
     * (as in "longitude" and "latitude"), then the value is assumed to be an angle in radians.
     * This is used for formatting an error message, if needed.</p>
     *
     * @throws ProjectionException if the comparison failed.
     */
    private static void compare(final String variable, double expected, double actual, final double tolerance)
            throws ProjectionException
    {
        final double delta = abs(expected - actual);
        if (delta > tolerance) {
            if (variable.charAt(0) == 'l') {
                actual   = toDegrees(actual);
                expected = toDegrees(expected);
            } else if (abs(actual) > 30 && abs(expected) > 30) {
                /*
                 * If the projected point tend toward infinity, treats the value as if is was
                 * really infinity. Note that 30 is considered as "close to infinity" because
                 * of the result we get when projecting 90°N using Mercator spherical formula:
                 *
                 *     y = log(tan(π/4 + φ/2))
                 *
                 * Because there is no exact representation of π/2 in base 2, the tangent
                 * function gives 1.6E+16 instead of infinity, which leads the logarithmic
                 * function to give us 37.3.
                 *
                 * This behavior is tested in MercatorTest.testSphericalFormulas().
                 */
                if (signum(actual) == signum(expected)) {
                    return;
                }
            }
            throw new ProjectionException("Assertion error: expected " + variable + "= " + expected + " but got " +
                    actual + ". Difference is " + delta + " and comparison threshold was " + tolerance + '.');
        }
    }
}
