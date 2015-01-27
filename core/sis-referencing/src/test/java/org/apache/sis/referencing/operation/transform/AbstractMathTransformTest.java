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
package org.apache.sis.referencing.operation.transform;

import java.util.Arrays;
import java.util.Random;
import org.opengis.referencing.operation.TransformException;
import static java.lang.StrictMath.*;
import static org.apache.sis.referencing.operation.transform.AbstractMathTransform.MAXIMUM_FAILURES;
import static org.apache.sis.referencing.operation.transform.AbstractMathTransform.MAXIMUM_BUFFER_SIZE;

// Test imports
import org.junit.Test;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;
import static org.junit.Assert.*;


/**
 * Tests the {@link AbstractMathTransformTest} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@DependsOn(IterationStrategyTest.class)
public final strictfp class AbstractMathTransformTest extends TestCase {
    /**
     * Tests the transform methods expecting at least one argument of type {@code float[]}.
     * We will use the {@link AbstractMathTransform#transform(double[], int, double[], int, int)}
     * method as the reference implementation, assuming that the later has been correctly tested
     * by {@link IterationStrategyTest}.
     *
     * @throws TransformException Should never occur.
     */
    @Test
    public void testTransforms() throws TransformException {
        /*
         * Prepares an array of double values filled with random numbers. In order for the test
         * to be useful, the source array must be longer than the maximum buffer size. We also
         * add an arbitrary amount for making AbstractMathTransform job more difficult.
         */
        final int length = MAXIMUM_BUFFER_SIZE * 3 + 167;
        final float [] srcFlt = new float [length];
        final float [] dstFlt = new float [length];
        final double[] srcDbl = new double[length];
        final double[] dstDbl = new double[length];
        final double[] result = new double[length];
        final Random random = new Random(67285);
        for (int i=0; i<length; i++) {
            srcDbl[i] = random.nextDouble();
            srcFlt[i] = (float) srcDbl[i];
        }
        final int checksumFlt = Arrays.hashCode(srcFlt);
        final int checksumDbl = Arrays.hashCode(srcDbl);
        /*
         * Tests transformation using different combinations of dimensions and offsets.
         * The 'srcFlt' and 'srcDbl' array should never change in this test. We will
         * check that using checksums.
         */
        for (int sd=1; sd<=4; sd++) {
            for (int td=1; td<=4; td++) {
                final PseudoTransform tr = new PseudoTransform(sd, td);
                for (int so=0; so<=10; so++) {
                    for (int to=0; to<=10; to++) {
                        final int n = min((length-so)/sd, (length-to)/td);
                        final int sublength = n * td;
                        tr.transform(srcDbl, so, result, to, n);
                        for (int p=0; p<=3; p++) {
                            final boolean srcIsDouble;
                            final boolean dstIsDouble;
                            switch (p) {
                                case 0: {  // float[]  -->  float[]
                                    srcIsDouble = false;
                                    dstIsDouble = false;
                                    Arrays.fill(dstFlt, Float.NaN);
                                    tr.transform(srcFlt, so, dstFlt, to, n);
                                    break;
                                }
                                case 1: {  // double[]  -->  float[]
                                    srcIsDouble = true;
                                    dstIsDouble = false;
                                    Arrays.fill(dstFlt, Float.NaN);
                                    tr.transform(srcDbl, so, dstFlt, to, n);
                                    break;
                                }
                                case 2: {  // float[]  -->  double[]
                                    srcIsDouble = false;
                                    dstIsDouble = true;
                                    Arrays.fill(dstDbl, Double.NaN);
                                    tr.transform(srcFlt, so, dstDbl, to, n);
                                    break;
                                }
                                case 3: {  // float[]  -->  float[]  again but on the same array.
                                    srcIsDouble = false;
                                    dstIsDouble = false;
                                    System.arraycopy(srcFlt, 0, dstFlt, 0, length);
                                    tr.transform(dstFlt, so, dstFlt, to, n);
                                    break;
                                }
                                default: throw new AssertionError(p);
                            }
                            for (int i=0; i<sublength; i++) {
                                final int t = to + i;
                                final float expected = (float) result[t];
                                final float actual = (dstIsDouble) ? (float) dstDbl[t] : dstFlt[t];
                                // Don't use assertEquals(...) because we don't want to accept NaN.
                                if (expected != actual) {
                                    fail("Failure in transform(" +
                                        (srcIsDouble ? "double" : "float") + "[], " + so + ", " +
                                        (dstIsDouble ? "double" : "float") + "[], " + to + ", " + n +
                                        ") on (input,output) points of dimension (" + sd + "," + td +
                                        "): at point " + i + " (index " + (i % MAXIMUM_BUFFER_SIZE) +
                                        " of buffer " + (i / MAXIMUM_BUFFER_SIZE) + "), expected " +
                                        expected + " but got " + actual);
                                }
                            }
                        }
                    }
                }
                assertEquals(checksumFlt, Arrays.hashCode(srcFlt));
                assertEquals(checksumDbl, Arrays.hashCode(srcDbl));
            }
        }
    }

    /**
     * Tests the handling of {@link TransformException}. The code is expected to be tolerant
     * up to some frequency of errors. Untransformed coordinates are expected to be set to NaN.
     */
    @Test
    @DependsOnMethod("testTransforms")
    public void testExceptionTolerance() {
        final int length = MAXIMUM_BUFFER_SIZE * 3 + 186;
        final double[] dblPts = new double[length];
        final float [] fltPts = new float [length];
        final int[] denominators = {
           100,  // Failures in   1% of points.
            50,  // Failures in   2% of points.
            10,  // Failures in  10% of points.
             2,  // Failures in  50% of points.
             1   // Failures in 100% of points.
        };
        int abandonCount = 0;
        int completedCount = 0;
        for (int di=0; di<denominators.length; di++) {
            final int denominator = denominators[di];
            final RandomFailureTransform tr = new RandomFailureTransform(denominator);
            final int sourceDimension = tr.getSourceDimensions();
            final int targetDimension = tr.getTargetDimensions();
            final int numPts = length / max(sourceDimension, targetDimension);
            for (int p=0; p<4; p++) {
                tr.ordinal = 0;
                try {
                    switch (p) {
                        case 0: {  // double[]  -->  double[]
                            tr.fill(dblPts);
                            tr.transform(dblPts, 0, dblPts, 0, numPts);
                            break;
                        }
                        case 1: {  // double[]  -->  float[]
                            tr.fill(dblPts);
                            tr.transform(dblPts, 0, fltPts, 0, numPts);
                            break;
                        }
                        case 2: {  // float[]  -->  double[]
                            tr.fill(fltPts);
                            tr.transform(fltPts, 0, dblPts, 0, numPts);
                            break;
                        }
                        case 3: {  // float[]  -->  float[]
                            tr.fill(fltPts);
                            tr.transform(fltPts, 0, fltPts, 0, numPts);
                            break;
                        }
                        default: throw new AssertionError(p);
                    }
                    fail("Expected at least one TransformException.");
                } catch (TransformException exception) {
                    /*
                     * This is the expected path for this test. The transform may have completed its
                     * work despite the exception, or may have given up, depending on the frequency
                     * of exception occurrences. We check if the transform has completed its work and
                     * compare with the expected behavior for the statistical frequency in this run.
                     */
                    final boolean completed = (exception.getLastCompletedTransform() == tr);
                    final boolean expected  = MAXIMUM_BUFFER_SIZE/denominator < MAXIMUM_FAILURES;
                    final int count = tr.failures.size();
                    assertEquals("The completion state during pass #" + p + " (having " + count +
                                 " failures among " + numPts + " points = " + 100*count/numPts +
                                 "%) doesn't match the expected one for a statistitical frequency of " +
                                 100/denominator + "% of failures:", expected, completed);
                    // TIP: if the above assertion fails, make sure that the frequencies declared
                    //      in the 'denominators' array are not to close to the cutoff frequency.
                    if (completed) {
                        completedCount++;
                    } else {
                        // If the transform has given up, then there is no guarantee about the state
                        // of the destination array so we can not continue the tests in this run.
                        tr.failures.clear();
                        abandonCount++;
                        continue;
                    }
                }
                /*
                 * Inspects each transformed coordinates. Either all ordinates are NaN, or either
                 * none of them are. Verifies with RandomFailureTransform if the NaN state is the
                 * expected one.
                 */
                assertFalse("TransformExceptions should have been recorded.", tr.failures.isEmpty());
                for (int i=0; i<numPts; i++) {
                    final int dstOff = i * targetDimension;
                    final boolean failed = tr.failures.remove(i);
                    final boolean targetIsDouble = (p & 1) == 0;
                    for (int j=dstOff + targetDimension; --j >= dstOff;) {
                        final boolean isNaN = targetIsDouble ? Double.isNaN(dblPts[j])
                                                             : Float .isNaN(fltPts[j]);
                        assertEquals("Unexpected NaN state.", failed, isNaN);
                    }
                }
                assertTrue("Some TransformExceptions remainding.", tr.failures.isEmpty());
            }
        }
        /*
         * Following statistics depend on the value of MAXIMUM_BUFFER_SIZE and MAXIMUM_FAILURES.
         * They may depend slightly on the generated random values if the statistical frequency
         * of failures is close to the threshold. Our random generator is initialized with a
         * constant seed, so random fluctuation should not break this test.
         */
        assertEquals("Count of completion.", 8, completedCount);
        assertEquals("Count of abandons.",  12, abandonCount);
    }
}
