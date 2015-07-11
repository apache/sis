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

import java.util.Random;
import java.io.IOException;
import java.io.PrintStream;
import org.apache.sis.math.Statistics;
import org.apache.sis.math.StatisticsFormat;
import org.apache.sis.measure.Latitude;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.resources.Errors;

import static java.lang.Math.*;


/**
 * Implements two alternative methods to compute φ in Mercator projection.
 * Those two methods computing the latitude φ from {@code exp(-northing)}
 * (ignoring non-relevant terms for this discussion) are:
 *
 * <ul>
 *   <li>the series expansion given by §1.3.3 in Geomatics Guidance Note number 7 part 2 – April 2015,</li>
 *   <li>an iterative process for solving equation (7-9) from Snyder, initially implemented by USGS.</li>
 * </ul>
 *
 * In our measurements, both the iterative process (USGS) and the series expansion (EPSG) have the
 * same accuracy when applied on the WGS84 ellipsoid. However the EPSG formula is 2 times faster.
 * On the other hand, accuracy of the EPSG formula decreases when we increase the excentricity,
 * while the iterative process keeps its accuracy (at the cost of more iterations).
 * For the Earth (excentricity of about 0.082) the errors are less than 0.01 millimetres.
 * But the errors become centimetric (for a hypothetical planet of the size of the Earth)
 * before excentricity 0.2 and increase quickly after excentricity 0.3.
 *
 * <p>For the WGS84 ellipsoid and the iteration tolerance given by the {@link NormalizedProjection#ITERATION_TOLERANCE}
 * constant (currently about 0.25 cm on Earth), the two methods have equivalent precision. Computing φ values for
 * millions of random numbers and verifying which method is the most accurate give fifty-fifty results: each method
 * win in about 50% of cases. But as we increase the excentricity, the iterative method wins more often.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public final class MercatorAlternative {   // No 'strictfp' keyword here since we want to compare with Mercator class.
    /**
     * Ellipsoid excentricity. Value 0 means that the ellipsoid is spherical.
     */
    private final double excentricity;

    /**
     * Coefficients used in the series expansion.
     */
    private final double c2χ, c4χ, c6χ, c8χ;

    /**
     * Creates a new instance for the excentricty of the WGS84 ellipsoid, which is approximatively 0.08181919084262157.
     * Reminder: the excentricity of a sphere is 0.
     */
    public MercatorAlternative() {
        this(0.00669437999014133);  // Squared excentricity.
    }

    /**
     * Creates a new instance for the same excentricity than the given projection.
     *
     * @param projection the projection from which to take the excentricity.
     */
    public MercatorAlternative(final NormalizedProjection projection) {
        this(projection.excentricitySquared);
    }

    /**
     * Creates a new instance for the given squared excentricity.
     *
     * @param e2 the square of the excentricity.
     */
    public MercatorAlternative(final double e2) {
        excentricity = sqrt(e2);
        final double e4 = e2 * e2;
        final double e6 = e2 * e4;
        final double e8 = e4 * e4;
        /*
         * For each line below, add the smallest values first in order to reduce rounding errors.
         * The smallest values are the one using the excentricity raised to the highest power.
         */
        c2χ  =    13/   360.* e8  +   1/ 12.* e6  +  5/24.* e4  +  e2/2;
        c4χ  =   811/ 11520.* e8  +  29/240.* e6  +  7/48.* e4;
        c6χ  =    81/  1120.* e8  +   7/120.* e6;
        c8χ  =  4279/161280.* e8;
    }

    /**
     * Computes φ using the series expansion given by Geomatics Guidance Note number 7, part 2.
     *
     * @param  t The {@code expOfSouthing} parameter value.
     * @return The latitude (in radians) for the given parameter.
     */
    public double bySeriesExpansion(final double t) {
        final double χ = PI/2 - 2*atan(t);
        return c8χ * sin(8*χ) +   // Add the smallest values first for reducing rounding errors.
               c6χ * sin(6*χ) +
               c4χ * sin(4*χ) +
               c2χ * sin(2*χ) + χ;
    }

    /**
     * Computes φ using the iterative method used by USGS.
     *
     * @param  t The {@code expOfSouthing} parameter value.
     * @return The latitude (in radians) for the given parameter.
     * @throws ProjectionException if the iteration does not converge.
     */
    public double byIterativeMethod(final double t) throws ProjectionException {
        final double hℯ = 0.5 * excentricity;
        double φ = (PI/2) - 2*atan(t);                                      // Snyder (7-11)
        for (int i=0; i<NormalizedProjection.MAXIMUM_ITERATIONS; i++) {     // Iteratively solve equation (7-9) from Snyder
            final double ℯsinφ = excentricity * sin(φ);
            final double Δφ = abs(φ - (φ = PI/2 - 2*atan(t * pow((1 - ℯsinφ)/(1 + ℯsinφ), hℯ))));
            if (Δφ <= NormalizedProjection.ITERATION_TOLERANCE) {
                return φ;
            }
        }
        if (Double.isNaN(t)) {
            return Double.NaN;
        }
        throw new ProjectionException(Errors.Keys.NoConvergence);
    }

    /**
     * Basically a copy of {@link GeneralLambert#expOfNorthing(double, double)}.
     */
    final double expOfNorthing(final double φ) {
        final double ℯsinφ = excentricity * sin(φ);
        return tan(PI/4 + 0.5*φ) * pow((1 - ℯsinφ) / (1 + ℯsinφ), 0.5*excentricity);
    }

    /**
     * Performs a comparison between φ values computed by the iterative method
     * and φ values computed by series expansion.
     * The result is printed to the standard output stream.
     *
     * @param  numSamples Number of random sample values.
     * @throws ProjectionException if an error occurred during the calculation of φ.
     */
    public void compare(final int numSamples) throws ProjectionException {
        compare(null, numSamples);
    }

    /**
     * Implementation of {@link #compare(int)}, optionally with a comparison with {@link GeneralLambert}.
     */
    private void compare(final GeneralLambert projection, final int numSamples) throws ProjectionException {
        final Statistics iterativeMethodErrors = new Statistics("Iterative method error");
        final Statistics seriesExpansionErrors = new Statistics("Series expansion error");
        final Statistics generalLambertErrors  = new Statistics("'GeneralLambert' error");
        final Statistics methodDifferences     = new Statistics("Δ (iterative - series)");
        final Random random = new Random();
        for (int i=0; i<numSamples; i++) {
            final double φ_deg = random.nextDouble() * (Latitude.MAX_VALUE - Latitude.MIN_VALUE) + Latitude.MIN_VALUE;
            final double φ     = toRadians(φ_deg);
            final double t     = 1 / expOfNorthing(φ);
            final double byIterativeMethod = toDegrees(byIterativeMethod(t));
            final double bySeriesExpansion = toDegrees(bySeriesExpansion(t));

            iterativeMethodErrors.accept(abs(φ_deg - byIterativeMethod));
            seriesExpansionErrors.accept(abs(φ_deg - bySeriesExpansion));
            methodDifferences.accept(byIterativeMethod - bySeriesExpansion);
            if (projection != null) {
                generalLambertErrors.accept(abs(φ_deg - toDegrees(projection.φ(t))));
            }
        }
        /*
         * At this point we finished to collect the statistics.
         */
        Statistics[] stats = new Statistics[] {
            iterativeMethodErrors,
            seriesExpansionErrors,
            generalLambertErrors,
            methodDifferences
        };
        if (projection == null) {
            stats = ArraysExt.remove(stats, 2, 1);
        }
        final PrintStream out = System.out;
        out.println("Comparison of two different way to compute φ for excentricity " + excentricity);
        out.println("Values are in degrees, ");
        final StatisticsFormat format = StatisticsFormat.getInstance();
        format.setBorderWidth(1);
        try {
            format.format(stats, out);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        out.flush();
    }

    /**
     * Executes {@link #compare(int)} for the excentricity of an imaginary ellipsoid.
     * The result is printed to the standard output stream.
     *
     * @param  args ignored.
     * @throws ProjectionException if an error occurred in {@link #φ(double)}.
     */
    public static void main(String[] args) throws ProjectionException {
        final GeneralLambert projection = new NoOp(100, 95);
        final MercatorAlternative alt = new MercatorAlternative(projection);
        alt.compare(projection, 2000000);
    }
}
