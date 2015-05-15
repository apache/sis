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

import java.util.Random;
import java.io.IOException;
import org.junit.Test;
import org.apache.sis.test.TestCase;

import static java.lang.StrictMath.*;
import static java.lang.Double.NaN;
import static java.lang.Double.isNaN;
import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link Statistics} class.
 *
 * <p>This class uses {@link Random} numbers generator with hard-coded seeds. We do not allow
 * random seeds because the tests invoke the {@link Random#nextGaussian()} method, then check
 * if the statistical values are within some range. Because Gaussian distributions have non-null
 * probability to contain arbitrary large values (infinity is the only limit), testing with random
 * seeds could produce statistical values out of the expected range no matter how large is this range.
 * We could only reduce the probability, but never make it null. This is not a flaw in the code,
 * but a consequence of the probabilistic nature of those statistical distributions.
 * Consequently, in order to keep the build stable, the random seeds are fixed to values
 * that are known to produce results inside the range expected by this test class.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final strictfp class StatisticsTest extends TestCase {
    /**
     * For floating point comparisons.
     */
    private static final double EPS = 1E-10;

    /**
     * Tests the initial state of newly constructed instance.
     */
    @Test
    public void testInitialState() {
        final Statistics statistics = new Statistics(null);
        assertEquals(0,  statistics.count());
        assertEquals(0,  statistics.countNaN());
        assertTrue(isNaN(statistics.minimum()));
        assertTrue(isNaN(statistics.maximum()));
        assertTrue(isNaN(statistics.mean()));
        assertTrue(isNaN(statistics.rms()));
        assertTrue(isNaN(statistics.standardDeviation(true)));
        assertTrue(isNaN(statistics.standardDeviation(false)));
    }

    /**
     * Tests the statistics over a large range of Gaussian values.
     * Means should be close to 0, RMS and standard deviation should be close to 1.
     */
    @Test
    public void testGaussian() {
        final Random random = new Random(317780561); // See class javadoc.
        final Statistics statistics = new Statistics(null);
        for (int i=0; i<10000; i++) {
            statistics.accept(random.nextGaussian());
        }
        assertEquals(0, statistics.countNaN());
        assertEquals(10000, statistics.count());
        assertEquals(0, statistics.mean(), 0.01);
        assertEquals(1, statistics.rms (), 0.01);
        assertEquals(1, statistics.standardDeviation(false), 0.01);
    }

    /**
     * Tests the statistics over a large range of values distributed between 0 and 1.
     * Means should be close to 0, minimum and maximum close to -1 and +1 respectively.
     */
    @Test
    public void testUniform() {
        // Theorical values for uniform distribution.
        final double lower  = -1;
        final double upper  = +1;
        final double range  = upper - lower;
        final double stdDev = sqrt(range*range / 12);

        // Now tests.
        final Random random = new Random(309080660);
        final Statistics statistics = new Statistics(null);
        for (int i=0; i<10000; i++) {
            statistics.accept(random.nextDouble()*range + lower);
        }
        assertEquals(0,      statistics.countNaN());
        assertEquals(10000,  statistics.count());
        assertEquals(0.0,    statistics.mean(),    0.01);
        assertEquals(lower,  statistics.minimum(), 0.01);
        assertEquals(upper,  statistics.maximum(), 0.01);
        assertEquals(stdDev, statistics.rms(),     0.01);
        assertEquals(stdDev, statistics.standardDeviation(false), 0.01);
    }

    /**
     * Same than {@link #testUniform()}, but on integer values.
     * Used for testing {@link Statistics#accept(long)}.
     */
    @Test
    public void testUniformUsingIntegers() {
        // Theorical values for uniform distribution.
        final int    lower  = -1000;
        final int    upper  = +1000;
        final int    range  = upper - lower;
        final double stdDev = sqrt(range*range / 12.0);

        // Now tests.
        final Random random = new Random(309080660);
        final Statistics statistics = new Statistics(null);
        for (int i=0; i<10000; i++) {
            statistics.accept(random.nextInt(range) + lower);
        }
        assertEquals(0,      statistics.countNaN());
        assertEquals(10000,  statistics.count());
        assertEquals(0.0,    statistics.mean(),    10.0);
        assertEquals(lower,  statistics.minimum(), 10.0);
        assertEquals(upper,  statistics.maximum(), 10.0);
        assertEquals(stdDev, statistics.rms(),     10.0);
        assertEquals(stdDev, statistics.standardDeviation(false), 10.0);
    }

    /**
     * Tests the statistics starting with a number big enough to make the code fails if we were
     * not using the <a href="http://en.wikipedia.org/wiki/Kahan_summation_algorithm">Kahan
     * summation algorithm</a>.
     */
    @Test
    public void testKahanAlgorithm() {
        final double[] offsetAndTolerancePairs = {
            // Starting with a reasonably small value, the result should be accurate
            // with or without Kahan algorithm. So we use that for testing the test.
            1000, 0.002,

            // First power of 10 for which the summation
            // fails if we don't use the Kahan algorithm.
            1E+16, 0.003,

            // Kahan algorithm still good here.
            1E+18, 0.003,

            // Last power of 10 before the summation fails
            // using the Kahan algorithm. Quality is lower.
            1E+19, 0.125,

            // Starting with this number fails in all case using our algorithm.
            // We test this value only in order to test our test method...
            1E+20, 0
        };
        final Random random = new Random(223386491);
        final Statistics statistics = new Statistics(null);
        for (int k=0; k<offsetAndTolerancePairs.length; k++) {
            final double offset = offsetAndTolerancePairs[k];
            final double tolerance = offsetAndTolerancePairs[++k];
            assertTrue("Possible misorder in offsetAndTolerancePairs", offset > 10);
            assertTrue("Possible misorder in offsetAndTolerancePairs", tolerance < 0.2);
            statistics.reset();
            statistics.accept(offset);
            for (int i=0; i<10000; i++) {
                statistics.accept(random.nextDouble());
            }
            final double r = statistics.mean() - offset / statistics.count();
            final double expected = (tolerance != 0) ? 0.5 : 0;
            assertEquals(expected, r, tolerance);

            statistics.accept(-offset); // Accuracy will be better than in previous test.
            assertEquals(expected, statistics.mean(), min(tolerance, 0.1));
        }
    }

    /**
     * Tests the concatenation of many {@link Statistics} objects.
     */
    @Test
    public void testConcatenation() {
        final Random random = new Random(429323868); // See class javadoc.
        final Statistics global = new Statistics(null);
        final Statistics byBlock = new Statistics(null);
        for (int i=0; i<10; i++) {
            final Statistics block = new Statistics(null);
            for (int j=0; j<1000; j++) {
                final double value;
                if (random.nextInt(800) == 0) {
                    value = Double.NaN;
                } else {
                    value = random.nextGaussian() + 10*random.nextDouble();
                }
                global.accept(value);
                block.accept(value);
            }
            byBlock.combine(block);
            if (i == 0) {
                assertEquals("Adding for the first time; should have the same amount of data.",   block,  byBlock);
                assertEquals("Adding for the first time; should have got exactly the same data.", global, byBlock);
            } else {
                assertFalse("Should have more data that the block we just computed.",
                        byBlock.equals(block));
            }
            assertEquals(global.count(),    byBlock.count());
            assertEquals(global.countNaN(), byBlock.countNaN());
            assertEquals(global.minimum(),  byBlock.minimum(), STRICT);
            assertEquals(global.maximum(),  byBlock.maximum(), STRICT);
            assertEquals(global.mean(),     byBlock.mean(),    1E-15);
            assertEquals(global.rms(),      byBlock.rms(),     1E-15);
        }
    }

    /**
     * Tests the serialization.
     *
     * @throws IOException Should never happen.
     */
    @Test
    public void testSerialization() throws IOException {
        final Statistics statistics = new Statistics(null);
        statistics.accept(40);
        statistics.accept(10);
        statistics.accept(NaN);
        statistics.accept(20);
        final Statistics after = assertSerializedEquals(statistics);
        assertNotSame(statistics, after);
        assertEquals( 3,                 after.count());
        assertEquals( 1,                 after.countNaN());
        assertEquals(10.0,               after.minimum(),             STRICT);
        assertEquals(40.0,               after.maximum(),             STRICT);
        assertEquals(23.333333333333333, after.mean(),                   EPS);
        assertEquals(26.457513110645905, after.rms(),                    EPS);
        assertEquals(12.472191289246473, after.standardDeviation(true),  EPS);
        assertEquals(15.275252316519468, after.standardDeviation(false), EPS);
    }
}
