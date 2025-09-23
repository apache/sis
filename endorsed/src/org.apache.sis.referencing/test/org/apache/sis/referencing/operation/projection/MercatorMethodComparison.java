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
import java.io.UncheckedIOException;
import static java.lang.Math.*;     // Not StrictMath in this particular case.
import org.apache.sis.io.TableAppender;
import org.apache.sis.math.Statistics;
import org.apache.sis.math.StatisticsFormat;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.metadata.internal.shared.ReferencingServices;
import static org.apache.sis.util.internal.shared.Constants.NANOS_PER_SECOND;

// Test dependencies
import org.apache.sis.test.Benchmark;


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
 * In our measurements, both the iterative process (USGS) and the series expansion (EPSG) have the same accuracy
 * when applied on the WGS84 ellipsoid. However, the EPSG formula is 2 times faster on Java 8 (less on more recent
 * Java versions). On the other hand, accuracy of the EPSG formula decreases when we increase the eccentricity,
 * while the iterative process keeps its accuracy (at the cost of more iterations).
 * For the Earth (eccentricity of about 0.082) the errors are less than 0.01 millimetres.
 * But the errors become centimetric (for a hypothetical planet of the size of the Earth)
 * before eccentricity 0.2 and increase quickly after eccentricity 0.3.
 *
 * <p>For the WGS84 ellipsoid and the iteration tolerance given by the {@link NormalizedProjection#ITERATION_TOLERANCE}
 * constant (currently about 0.25 cm on Earth), the two methods have equivalent precision. Computing φ values for
 * millions of random numbers and verifying which method is the most accurate give fifty-fifty results: each method
 * win in about 50% of cases. But as we increase the eccentricity, the iterative method wins more often.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@Benchmark
public final class MercatorMethodComparison {
    /**
     * Where to print the outputs of this class.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private static final PrintStream out = System.out;

    /**
     * Ellipsoid eccentricity. Value 0 means that the ellipsoid is spherical.
     */
    private final double eccentricity;

    /**
     * Coefficients used in the series expansion.
     */
    private final double c2χ, c4χ, c6χ, c8χ;

    /**
     * The actual implementation to compare with.
     */
    private final ConformalProjection implementation;

    /**
     * Creates a new instance for the same eccentricity as the given projection.
     *
     * @param  projection  the projection from which to take the eccentricity.
     */
    MercatorMethodComparison(final ConformalProjection projection) {
        implementation = projection;
        eccentricity = projection.eccentricity;
        final double e2 = projection.eccentricitySquared;
        final double e4 = e2 * e2;
        final double e6 = e2 * e4;
        final double e8 = e4 * e4;
        /*
         * For each line below, add the smallest values first in order to reduce rounding errors.
         * The smallest values are the one using the eccentricity raised to the highest power.
         */
        c2χ  =    13/   360.* e8  +   1/ 12.* e6  +  5/24.* e4  +  e2/2;
        c4χ  =   811/ 11520.* e8  +  29/240.* e6  +  7/48.* e4;
        c6χ  =    81/  1120.* e8  +   7/120.* e6;
        c8χ  =  4279/161280.* e8;
    }

    /**
     * Computes φ using the series expansion given by Geomatics Guidance Note number 7, part 2.
     * This is the first part of the {@link ConformalProjection#φ(double)} method.
     *
     * @param  t  the {@code rexpΨ} parameter value.
     * @return the latitude (in radians) for the given parameter.
     */
    public double bySeriesExpansion(final double t) {
        final double χ = PI/2 - 2*atan(t);
        return c8χ * sin(8*χ) +             // Add the smallest values first for reducing rounding errors.
               c6χ * sin(6*χ) +
               c4χ * sin(4*χ) +
               c2χ * sin(2*χ) + χ;
    }

    /**
     * Computes φ using the iterative method used by USGS.
     * This is the second part of the {@link ConformalProjection#φ(double)} method.
     *
     * @param  t  the {@code rexpΨ} parameter value.
     * @return the latitude (in radians) for the given parameter.
     * @throws ProjectionException if the iteration does not converge.
     */
    public double byIterativeMethod(final double t) throws ProjectionException {
        final double hℯ = 0.5 * eccentricity;
        double φ = (PI/2) - 2*atan(t);                                          // Snyder (7-11)
        for (int it=0; it < NormalizedProjection.MAXIMUM_ITERATIONS; it++) {    // Iteratively solve equation (7-9) from Snyder
            final double ℯsinφ = eccentricity * sin(φ);
            final double Δφ = φ - (φ = PI/2 - 2*atan(t * pow((1 - ℯsinφ)/(1 + ℯsinφ), hℯ)));
            if (abs(Δφ) <= NormalizedProjection.ITERATION_TOLERANCE) {
                return φ;
            }
        }
        if (Double.isNaN(t)) {
            return Double.NaN;
        }
        throw new ProjectionException(Resources.format(Resources.Keys.NoConvergence));
    }

    /**
     * Basically a copy of {@link ConformalProjection#expΨ(double, double)}.
     */
    final double expΨ(final double φ) {
        final double ℯsinφ = eccentricity * sin(φ);
        return tan(PI/4 + 0.5*φ) * pow((1 - ℯsinφ) / (1 + ℯsinφ), 0.5*eccentricity);
    }

    /**
     * Compares the φ values computed by the two methods (iterative and series expansion) against the expected φ
     * values for random numbers. The result is printed to the standard output stream as the maximum and average errors,
     * in units of {@link NormalizedProjection#ITERATION_TOLERANCE} (about 0.25 cm on a planet of the size of Earth).
     *
     * @param  numSamples  number of random sample values.
     * @throws ProjectionException if an error occurred during the calculation of φ.
     */
    public void printAccuracyComparison(final int numSamples) throws ProjectionException {
        compare(numSamples, null);
    }

    /**
     * Implementation of {@link #printAccuracyComparison(int)} and {@link #printErrorForExcentricities(double,double)},
     * optionally with a comparison with {@link ConformalProjection}.
     */
    private void compare(final int numSamples, final TableAppender summarize) throws ProjectionException {
        final var iterativeMethodErrors = new Statistics("Iterative method error");
        final var seriesExpansionErrors = new Statistics("Series expansion error");
        final var implementationErrors  = new Statistics("Implementation error");
        final var random = new Random();
        for (int i=0; i<numSamples; i++) {
            final double φ = random.nextDouble() * PI - PI/2;
            final double t = 1 / expΨ(φ);
            final double byIterativeMethod = byIterativeMethod(t);
            final double bySeriesExpansion = bySeriesExpansion(t);
            final double byImplementation  = implementation.φ(t);

            iterativeMethodErrors.accept(abs(φ - byIterativeMethod) / NormalizedProjection.ITERATION_TOLERANCE);
            seriesExpansionErrors.accept(abs(φ - bySeriesExpansion) / NormalizedProjection.ITERATION_TOLERANCE);
            implementationErrors .accept(abs(φ - byImplementation)  / NormalizedProjection.ITERATION_TOLERANCE);
        }
        /*
         * At this point we finished to collect the statistics for the eccentricity of this particular
         * MercatorMethodComparison instance. If this method call is only part of a longer calculation
         * for various excentricty values, print a summary in a single line.
         * Otherwise print more verbose results.
         */
        if (summarize != null) {
            summarize.append(String.valueOf(eccentricity));                     summarize.nextColumn();
            summarize.append(String.valueOf(iterativeMethodErrors.mean()));     summarize.nextColumn();
            summarize.append(String.valueOf(iterativeMethodErrors.maximum()));  summarize.nextColumn();
            summarize.append(String.valueOf(seriesExpansionErrors.mean()));     summarize.nextColumn();
            summarize.append(String.valueOf(seriesExpansionErrors.maximum()));  summarize.nextLine();
        } else {
            Statistics[] stats = new Statistics[] {
                iterativeMethodErrors,
                seriesExpansionErrors,
                implementationErrors,
            };
            out.println("Comparison of different ways to compute φ for eccentricity " + eccentricity + '.');
            out.println("Values are in units of " + NormalizedProjection.ITERATION_TOLERANCE + " radians (about "
                    + round(toDegrees(NormalizedProjection.ITERATION_TOLERANCE) * 60 * ReferencingServices.NAUTICAL_MILE * 1000)
                    + " mm on Earth).");
            final StatisticsFormat format = StatisticsFormat.getInstance();
            format.setBorderWidth(1);
            try {
                format.format(stats, out);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            out.flush();
        }
    }

    /**
     * Prints the error of the two methods for various eccentricity values.
     * The intent of this method is to find an eccentricity threshold value where we consider the errors too high.
     *
     * <p>This method is used for determining empirically a value for {@link ConformalProjection#ECCENTRICITY_THRESHOLD}.
     * The current threshold value is shown by inserting a horizontal line separator in the table when that threshold
     * is crossed.</p>
     *
     * @param  min  the first eccentricity value to test.
     * @param  max  the maximal eccentricity value to test.
     * @throws ProjectionException if an error occurred in {@link ConformalProjection#φ(double)}.
     */
    public static void printErrorForExcentricities(final double min, final double max) throws ProjectionException {
        final var table = new TableAppender(out);
        table.appendHorizontalSeparator();
        table.append("Eccentricity");            table.nextColumn();
        table.append("Mean iterative error");    table.nextColumn();
        table.append("Maximal iterative error"); table.nextColumn();
        table.append("Mean series error");       table.nextColumn();
        table.append("Maximal series error");    table.nextLine();
        table.appendHorizontalSeparator();
        boolean crossThreshold = false;
        final double step = 0.01;
        double eccentricity;
        for (int i=0; (eccentricity = min + step*i) < max; i++) {
            if (!crossThreshold && eccentricity >= ConformalProjection.ECCENTRICITY_THRESHOLD) {
                crossThreshold = true;
                table.appendHorizontalSeparator();
            }
            final double semiMinor = sqrt(1 - eccentricity * eccentricity);
            final MercatorMethodComparison alt = new MercatorMethodComparison(new NoOp(1, semiMinor));
            alt.compare(10000, table);
        }
        table.appendHorizontalSeparator();
        try {
            table.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Compares the performance of the 3 methods.
     *
     * @throws ProjectionException if an error occurred in {@link ConformalProjection#φ(double)}.
     */
    private void benchmark() throws ProjectionException {
        final Random random = new Random();
        final double[] t = new double[1000000];
        for (int i=0; i<t.length; i++) {
            t[i] = random.nextGaussian() * 3;
        }
        double s0 = 0, s1 = 0, s2 = 0;
        final long t0 = System.nanoTime();
        for (int i=0; i<t.length; i++) {
            s0 += byIterativeMethod(t[i]);
        }
        final long t1 = System.nanoTime();
        for (int i=0; i<t.length; i++) {
            s1 += bySeriesExpansion(t[i]);
        }
        final long t2 = System.nanoTime();
        for (int i=0; i<t.length; i++) {
            s2 += implementation.φ(t[i]);
        }
        final long t3 = System.nanoTime();
        final float c = (t1 - t0) / 100f;
        out.println("Iterative method: " + ((t1 - t0) / (float) NANOS_PER_SECOND) + " seconds (" + round((t1 - t0) / c) + "%).");
        out.println("Series expansion: " + ((t2 - t1) / (float) NANOS_PER_SECOND) + " seconds (" + round((t2 - t1) / c) + "%).");
        out.println("Implementation:   " + ((t3 - t2) / (float) NANOS_PER_SECOND) + " seconds (" + round((t3 - t2) / c) + "%).");
        out.println("Mean φ values: " + (s0 / t.length) + ", "
                                      + (s1 / t.length) + " and "
                                      + (s2 / t.length) + ".");
    }

    /**
     * The result is printed to the standard output stream.
     *
     * @param  args  ignored.
     * @throws ProjectionException if an error occurred in {@link ConformalProjection#φ(double)}.
     * @throws InterruptedException if the thread has been interrupted between two benchmarks.
     */
    public static void main(String[] args) throws ProjectionException, InterruptedException {
        out.println("Comparison of the errors of series expension and iterative method for various eccentricity values.");
        printErrorForExcentricities(0.08, 0.3);

        out.println();
        out.println("Comparison of the errors for a sphere.");
        out.println("The errors should be almost zero:");
        out.println();
        MercatorMethodComparison c = new MercatorMethodComparison(new NoOp(false));
        c.compare(10000, null);

        out.println();
        out.println("Comparison of the errors for the WGS84 eccentricity.");
        out.println("The 'ConformalProjection' errors should be the same as the series expansion errors:");
        out.println();
        c = new MercatorMethodComparison(new NoOp(true));
        c.compare(1000000, null);

        out.println();
        out.println("Comparison of the errors for the eccentricity of an imaginary ellipsoid.");
        out.println("The 'ConformalProjection' errors should be the close to the iterative method errors:");
        out.println();
        c = new MercatorMethodComparison(new NoOp(100, 95));
        c.compare(1000000, null);

        out.println();
        out.println("Benchmarks");
        c = new MercatorMethodComparison(new NoOp(true));
        for (int i=0; i<4; i++) {
            System.gc();
            Thread.sleep(1000);
            c.benchmark();
            out.println();
        }
        out.flush();
    }
}
