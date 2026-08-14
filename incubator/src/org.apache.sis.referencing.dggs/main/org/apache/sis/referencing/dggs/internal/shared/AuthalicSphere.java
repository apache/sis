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
package org.apache.sis.referencing.dggs.internal.shared;

import org.opengis.referencing.datum.Ellipsoid;
import static java.lang.Math.*;

/**
 * Ellipse to/from Authalic sphere with formulas from document : https://arxiv.org/pdf/2212.05818
 * Authalic / Geodetic latitude conversion as defined by Charles Karney's "On auxiliary latitudes"
 *
 * @see port/refactored from https://github.com/ecere/dggal/blob/main/src/projections/authalic.ec with Jerome Saint-Louis authorization.
 */
public final class AuthalicSphere {

    private static final int ORDER = 6;

    /**
     * Appendix A: The series coefficients
     * <p>
     * Cξφ (A19) - coefficients to convert geodetic latitude to authalic latitude
     */
    private static final double[] Cξφ = new double[] {
       -4/3.0,    -4/45.0,    88/ 315.0,       538/ 4725.0,     20824/467775.0,      -44732/   2837835.0,
                  34/45.0,     8/ 105.0,     -2482/14175.0,    -37192/467775.0,   -12467764/ 212837625.0,
                           -1532/2835.0,      -898/14175.0,     54968/467775.0,   100320856/1915538625.0,
                                              6007/14175.0,     24496/467775.0,    -5884124/  70945875.0,
                                                               -23356/ 66825.0,     -839792/  19348875.0,
                                                                                  570284222/1915538625.0};

    /**
     * Appendix A: The series coefficients
     * <p>
     * Cφξ (A20) - coefficients to convert authalic latitude to geodetic latitude
     */
    private static final double[] Cφξ = new double[] {
       4 / 3.0,  4 / 45.0,   -16/35.0,  -2582 /14175.0,  60136 /467775.0,    28112932/ 212837625.0,
                46 / 45.0,  152/945.0, -11966 /14175.0, -21016 / 51975.0,   251310128/ 638512875.0,
                          3044/2835.0,   3802 /14175.0, -94388 / 66825.0,    -8797648/  10945935.0,
                                         6059 / 4725.0,  41072 / 93555.0, -1472637812/ 638512875.0,
                                                        768272 /467775.0,  -455935736/ 638512875.0,
                                                                          4210684958l/1915538625.0};

    /**
     * Coefficients for geodetic to authalic for this ellipsoid.
     */
    private final double[] cforward = new double[ORDER];
    /**
     * Coefficients for authalic to geodetic for this ellipsoid.
     */
    private final double[] cinverse = new double[ORDER];

    /**
     *
     * @param ellipsoid ellipsoid to work on, not null
     */
    public AuthalicSphere(Ellipsoid ellipsoid) {
        final double semiMajorAxis = ellipsoid.getSemiMajorAxis();
        final double semiMinorAxis = ellipsoid.getSemiMinorAxis();
        precomputeCoefficients(semiMajorAxis, semiMinorAxis, Cξφ, cforward);
        precomputeCoefficients(semiMajorAxis, semiMinorAxis, Cφξ, cinverse);
    }

    /**
     * Convert geodetic latitude to authalic latitude.
     *
     * @param φ geodetic latitude
     * @return authalic latitude
     */
    public double toAuthalic(double φ) {
        return applyCoefficients(cforward, φ);
    }

    /**
     * Convert authalic latitude to geodetic latitude.
     *
     * @param φ authalic latitude
     * @return geodetic latitude
     */
    public double toGeodetic(double φ) {
        return applyCoefficients(cinverse, φ);
    }

    /**
     * 3. SERIES EXPANSIONS -- (20)
     * Precomputing coefficients based on Horner's method
     *
     * ∆η(ζ) = S⁽ᴸ⁾(ζ) · C⁽ᴸ×ᴹ⁾ηζ · P⁽ᴹ⁾(n) + O(nᴸ⁺¹)
     */
    private static void precomputeCoefficients(double semiMajorAxis, double semiMinorAxis, double[] C, double[] cp) {
        final double n = (semiMajorAxis - semiMinorAxis) / (semiMajorAxis + semiMinorAxis);  // Third flattening
        double d = n;

        cp[0] = (((((C[ 5] * n + C[ 4]) * n + C[ 3]) * n + C[ 2]) * n + C[ 1]) * n + C[ 0]) * d; d *= n;
        cp[1] = ((((             C[10]  * n + C[ 9]) * n + C[ 8]) * n + C[ 7]) * n + C[ 6]) * d; d *= n;
        cp[2] = (((                           C[14]  * n + C[13]) * n + C[12]) * n + C[11]) * d; d *= n;
        cp[3] = ((                                         C[17]  * n + C[16]) * n + C[15]) * d; d *= n;
        cp[4] = (                                                       C[19]  * n + C[18]) * d; d *= n;
        cp[5] =                                                                      C[20]  * d;
    }

    /**
     * Using Clenshaw summation algorithm (order 6)
     */
    private static double applyCoefficients(double[] cp, double φ){
        final double sinζ = sin(φ);
        final double cosζ = cos(φ);
        final double x = 2 * (cosζ - sinζ) * (cosζ + sinζ); // 2 * cos(2*ζ)
        double u0, u1; // accumulators for sum

        u0 = x * cp[5]   + cp[4];
        u1 = x * u0      + cp[3];
        u0 = x * u1 - u0 + cp[2];
        u1 = x * u0 - u1 + cp[1];
        u0 = x * u1 - u0 + cp[0];

        return φ + /* sin(2*ζ) * u0 */ 2 * sinζ * cosζ * u0;
    }

}
