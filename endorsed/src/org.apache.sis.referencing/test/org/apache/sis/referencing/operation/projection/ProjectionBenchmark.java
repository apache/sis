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

import java.util.List;
import java.util.Random;
import java.io.IOException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.math.Statistics;
import org.apache.sis.math.StatisticsFormat;
import org.apache.sis.measure.Latitude;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.referencing.operation.provider.AbstractProvider;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;

// Test dependencies
import org.apache.sis.referencing.operation.transform.MathTransformFactoryMock;
import org.apache.sis.test.Benchmark;


/**
 * Measures the performance of a given map projection implementation.
 * This class can be used for comparing different implementation alternatives,
 * for example with {@code ALLOW_TRIGONOMETRIC_IDENTITIES} flag on or off.
 *
 * <h2>Usage</h2>
 * Modify the provider created in the {@code main} method if needed, and run. Change map projection implementation
 * (for example by changing a {@code ALLOW_TRIGONOMETRIC_IDENTITIES} flag value) and run again.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@Benchmark
public final class ProjectionBenchmark {
    /**
     * Runs the benchmark and prints the time result to the standard output.
     * Edit this method for measuring the performance of a different map projection implementation.
     *
     * @param  args ignored.
     * @throws Exception if an error occurred while creating the map projection, projecting a point, <i>etc.</i>
     */
    public static void main(String[] args) throws Exception {
        final var benchmark = new ProjectionBenchmark(    // Put on next line the provider of the projection to benchmark.
                new org.apache.sis.referencing.operation.provider.LambertConformal2SP(),
                8,      // Central meridian
                25,     // Standard parallel 1
                40);    // Standard parallel 2

        for (int i=0; i<10; i++) {
            benchmark.run(true);
            System.gc();
            Thread.sleep(1000);
        }
        benchmark.printStatistics();
    }

    /**
     * Number of points to project. Can be modified freely.
     */
    private static final int NUM_POINTS = 1000000;

    /**
     * Number of dimension (not modifiable).
     */
    private static final int DIMENSION = 2;

    /**
     * The math transforms to use for the forward projection.
     */
    private final Transforms forward;

    /**
     * The math transforms to use for the reverse projection.
     */
    private final Transforms inverse;

    /**
     * The coordinates to project, filled with random points.
     */
    private final double[] coordinates;

    /**
     * The result of map projections.
     */
    private final double[] result;

    /**
     * Difference between reverse projections and the original coordinates.
     */
    private final Statistics errors;

    /**
     * Random number generator for the points to project.
     */
    private final Random random;

    /**
     * Prepares benchmarking for the map projection created by the given provider.
     */
    private ProjectionBenchmark(final AbstractProvider provider,
                      final double centralMeridian,
                      final double standardParallel1,
                      final double standardParallel2) throws FactoryException, NoninvertibleTransformException
    {
        random = new Random();
        final Parameters values = Parameters.castOrWrap(provider.getParameters().createValue());
        values.parameter(Constants.SEMI_MAJOR)         .setValue(MapProjectionTestCase.WGS84_A);
        values.parameter(Constants.SEMI_MINOR)         .setValue(MapProjectionTestCase.WGS84_B);
        values.parameter(Constants.CENTRAL_MERIDIAN)   .setValue(centralMeridian);
        values.parameter(Constants.STANDARD_PARALLEL_1).setValue(standardParallel1);
        values.parameter(Constants.STANDARD_PARALLEL_2).setValue(standardParallel2);
        forward = new Transforms("Forward", provider.createMathTransform(new MathTransformFactoryMock(provider), values));
        inverse = new Transforms("Inverse", forward.projection.inverse());
        coordinates = new double[NUM_POINTS * DIMENSION];
        final double λmin = centralMeridian - 40;
        final double Δλ   = 80;
        final double φmin = standardParallel1 * 0.75;
        final double Δφ   = Math.min(standardParallel2 * 1.25, Latitude.MAX_VALUE) - φmin;
        for (int i=0; i<coordinates.length;) {
            coordinates[i++] = random.nextDouble() * Δλ + λmin;     // Longitude
            coordinates[i++] = random.nextDouble() * Δφ + φmin;     // Latitude
        }
        result = new double[coordinates.length];
        errors = new Statistics("Errors (cm)");
    }

    /**
     * Decomposition of the {@code MathTransform} that perform map projections.
     */
    private static final class Transforms {
        /**
         * The original (full) map projection.
         */
        final MathTransform projection;

        /**
         * The transform that performs the non-linear part of the map projection.
         * This is the transform that we want to benchmark.
         */
        private final MathTransform kernel;

        /**
         * The normalization performed before the map projection.
         */
        private final LinearTransform normalize;

        /**
         * The denormalization performed after the map projection.
         */
        private final LinearTransform denormalize;

        /**
         * Statistics about the time needed for performing the map projection.
         */
        private final Statistics performance;

        /**
         * Creates a decomposition of the given map projection.
         *
         * @param  label       a label for display purpose.
         * @param  projection  the map projection to benchmark.
         */
        private Transforms(final String label, final MathTransform projection) {
            this.projection = projection;
            performance = new Statistics(label);
            final List<MathTransform> steps = MathTransforms.getSteps(projection);
            int kernelIndex = -1;
            for (int i=steps.size(); --i >= 0;) {
                if (!(steps.get(i) instanceof LinearTransform)) {
                    if (kernelIndex >= 0) {
                        throw new IllegalArgumentException("Found more than one non-linear kernel.");
                    }
                    kernelIndex = i;
                }
            }
            if (kernelIndex < 0) {
                throw new IllegalArgumentException("Non-linear kernel not found.");
            }
            kernel      = steps.get(kernelIndex);
            normalize   = singleton(steps.subList(0, kernelIndex));
            denormalize = singleton(steps.subList(kernelIndex + 1, steps.size()));
        }

        /**
         * Returns the single elements of the given list, or an identity transform if none.
         */
        private static LinearTransform singleton(final List<MathTransform> components) {
            switch (components.size()) {
                case 0: return MathTransforms.identity(DIMENSION);
                case 1: return (LinearTransform) components.get(0);
                default: throw new IllegalArgumentException("Unexpected number of components.");
            }
        }

        /**
         * Runs the benchmark on the complete map projection, including the linear parts.
         */
        @SuppressWarnings("UseOfSystemOutOrSystemErr")
        final void runComplete(final double[] sources, final double[] targets) throws TransformException {
            long time = System.nanoTime();
            projection.transform(sources, 0, targets, 0, NUM_POINTS);
            time = System.nanoTime() - time;
            final double seconds = time / (double) Constants.NANOS_PER_SECOND;
            System.out.printf("%s time: %1.4f%n", performance.name(), seconds);
            performance.accept(seconds);
        }

        /**
         * Runs the benchmark only on the non-linear part of the map projection,
         * ignoring the linear parts.
         */
        @SuppressWarnings("UseOfSystemOutOrSystemErr")
        final void runKernel(final double[] sources, final double[] targets) throws TransformException {
            normalize.transform(sources, 0, targets, 0, NUM_POINTS);
            long time = System.nanoTime();
            kernel.transform(targets, 0, targets, 0, NUM_POINTS);
            time = System.nanoTime() - time;
            denormalize.transform(targets, 0, targets, 0, NUM_POINTS);
            final double seconds = time / (double) Constants.NANOS_PER_SECOND;
            System.out.printf("%s time: %1.4f%n", performance.name(), seconds);
            performance.accept(seconds);
        }
    }

    /**
     * Runs the benchmark.
     *
     * @param  kernelOnly  {@code true} for measuring the performance of only the non-linear part,
     *                     or {@code false} for measuring the performance of the whole projection.
     */
    private void run(final boolean kernelOnly) throws TransformException {
        if (kernelOnly) {
            forward.runKernel(coordinates, result);
            inverse.runKernel(result, result);
        } else {
            forward.runComplete(coordinates, result);
            inverse.runComplete(result, result);
        }
        for (int i=0; i<NUM_POINTS;) {
            final double dx = result[i] - coordinates[i]; coordinates[i++] += random.nextDouble() - 0.5;
            final double dy = result[i] - coordinates[i]; coordinates[i++] += random.nextDouble() - 0.5;
            errors.accept(Math.hypot(dx*DEGREES_TO_CENTIMETRES, dy*DEGREES_TO_CENTIMETRES));
        }
    }

    /**
     * For reporting the errors in more convenient units.
     * This is an approximated conversion based on the nautical mile length.
     */
    private static final double DEGREES_TO_CENTIMETRES = 60*1852*100;

    /**
     * Prints statistics about measured time.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private void printStatistics() throws IOException {
        System.out.println();
        StatisticsFormat.getInstance().format(new Statistics[] {forward.performance, inverse.performance}, System.out);
        System.out.printf("%nAverage error is %1.2E cm (standard deviation %1.1E).%n", errors.mean(), errors.standardDeviation(false));
        System.out.flush();
    }
}
