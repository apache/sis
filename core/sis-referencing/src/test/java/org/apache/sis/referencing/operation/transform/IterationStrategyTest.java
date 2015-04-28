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
import static org.apache.sis.referencing.operation.transform.IterationStrategy.*;

// Test imports
import org.junit.Test;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOnMethod;
import static org.junit.Assert.*;


/**
 * Tests the {@link IterationStrategy} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public final strictfp class IterationStrategyTest extends TestCase {
    /**
     * Maximum number of dimension tested. The referencing module should be able to handle high
     * numbers, but we stick to low one in order to avoid making the test to long to execute.
     */
    private static final int MAX_DIMENSION = 6;

    /**
     * Maximum offset to test. The referencing module should be able to handle high numbers,
     * but we stick to low one in order to avoid making the test to long to execute.
     */
    private static final int MAX_OFFSET = 20;

    /**
     * Tests {@link IterationStrategy#suggest(int, int, int, int, int)} with a few basic cases,
     * comparing the computed value with the expected value.
     */
    @Test
    public void testSuggest() {
        assertEquals("Target replace source.", ASCENDING,  suggest(0, 1, 0, 1, 240));
        assertEquals("Target before source.",  ASCENDING,  suggest(1, 1, 0, 1, 239));
        assertEquals("Target after source.",   DESCENDING, suggest(0, 1, 1, 1, 239));
        assertEquals("Overlaps.",           BUFFER_TARGET, suggest(0, 2, 1, 1, 120));
        assertEquals("Overlaps.",           BUFFER_SOURCE, suggest(1, 1, 0, 2, 120));
    }

    /**
     * An empirical test making sure that the target subarray didn't overwrote the
     * source subarray while a transformation was in progress.
     *
     * @throws TransformException Should never occur.
     */
    @Test
    @DependsOnMethod("testSuggest")
    public void empiricalTest() throws TransformException {
        final int[] statistics = new int[4];
        /*
         * The length of the above array is hard-coded on purpose, as a reminder that if we
         * need to augment this value, then we need to augment the statistic checks at the
         * end of this method as well.
         */
        final int length = (2*MAX_OFFSET) * MAX_DIMENSION;
        final double[] sourcePts = new double[length];
        final double[] targetPts = new double[length];
        final double[] sharedPts = new double[length];
        final Random random = new Random(650268926);
        for (int i=0; i<length; i++) {
            sourcePts[i] = random.nextDouble();
        }
        final int checksum = Arrays.hashCode(sourcePts);
        for (int sourceDimension=1; sourceDimension<=MAX_DIMENSION; sourceDimension++) {
            for (int targetDimension=1; targetDimension<=MAX_DIMENSION; targetDimension++) {
                final PseudoTransform tr = new PseudoTransform(sourceDimension, targetDimension);
                for (int srcOff=0; srcOff<=MAX_OFFSET; srcOff++) {
                    for (int dstOff=0; dstOff<=MAX_OFFSET; dstOff++) {
                        final int numPts = min((length-srcOff) / sourceDimension,
                                               (length-dstOff) / targetDimension);
                        final IterationStrategy strategy = IterationStrategy.suggest(
                                srcOff, sourceDimension, dstOff, targetDimension, numPts);
                        statistics[strategy.ordinal()]++;
                        Arrays.fill(targetPts, Double.NaN);
                        System.arraycopy(sourcePts, 0, sharedPts, 0, length);
                        tr.transform(sharedPts, srcOff, sharedPts, dstOff, numPts);
                        tr.transform(sourcePts, srcOff, targetPts, dstOff, numPts);
                        assertEquals("Source points have been modified.", checksum, Arrays.hashCode(sourcePts));
                        final int stop = dstOff + numPts * targetDimension;
                        for (int i=dstOff; i<stop; i++) {
                            final double expected = targetPts[i];
                            final double actual   = sharedPts[i];
                            if (actual != expected) {
                                final int index = i - dstOff;
                                fail("Transform" +
                                     "(srcOff=" + srcOff + " srcDim=" + sourceDimension +
                                     " dstOff=" + dstOff + " dstDim=" + targetDimension +
                                     " numPts=" + numPts + ") using strategy " + strategy +
                                     ": for point " + (index / targetDimension) +
                                     " at dimension " + (index % targetDimension) +
                                     ", expected " + expected + " but got " + actual);
                            }
                        }
                    }
                }
            }
        }
        int sum = 0;
        for (int i=0; i<statistics.length; i++) {
            sum += statistics[i];
        }
        assertEquals(MAX_DIMENSION * MAX_DIMENSION * (MAX_OFFSET+1) * (MAX_OFFSET+1), sum);
        /*
         * The following statistics were determined empirically at the time we wrote this test,
         * right after having debugged IterationStrategy. They depend on the value of MAX_OFFSET
         * and MAX_DIMENSION constants, but do not depend on the random number generation. The
         * sum checked just before this comment verifies partially that assertion.
         *
         * If we assume that the calculation done by IterationStrategy was optimal at the time
         * we wrote it, then the statistics below should not change for current MAX_* setting.
         * If those values change because of some algorithm change, then ASCENDING + DESCENDING
         * count should increase while the BUFFER_SOURCE + BUFFER_TARGET count should decrease,
         * otherwise it would not be an improvement.
         */
        assertEquals(4851, statistics[ASCENDING    .ordinal()]);
        assertEquals(4410, statistics[DESCENDING   .ordinal()]);
        assertEquals(3465, statistics[BUFFER_SOURCE.ordinal()]);
        assertEquals(3150, statistics[BUFFER_TARGET.ordinal()]);
    }
}
