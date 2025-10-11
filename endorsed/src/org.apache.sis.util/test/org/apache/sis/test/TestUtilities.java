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
package org.apache.sis.test;

import java.util.Random;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import org.apache.sis.util.Debug;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;


/**
 * Miscellaneous utility methods for test cases.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class TestUtilities {
    /**
     * The thread group for every threads created for testing purpose.
     */
    public static final ThreadGroup THREADS = new ThreadGroup("SIS-Tests");

    /**
     * The seed for the random number generator created by {@link #createRandomNumberGenerator()}, or null if none.
     * This information is used for printing the seed in case of test failure, in order to allow the developer to
     * reproduce the failure.
     */
    static final ThreadLocal<Long> randomSeed = new ThreadLocal<>();

    /**
     * Do not allow instantiation of this class.
     */
    private TestUtilities() {
    }

    /**
     * Returns a new random number generator with a random seed.
     * If the test succeed, nothing else will happen. But if the test fails, then the seed value will
     * be logged to the {@link TestCase#out} stream in order to allow the developer to reproduce the
     * test failure.
     *
     * <p>This method shall be invoked only in the body of a test method - the random number generator
     * is not valid anymore after the test finished.</p>
     *
     * <p>This method doesn't need to be used in every cases. For example, test cases using
     * {@link Random#nextGaussian()} should create their own random numbers generator with
     * the {@link Random#Random(long)} constructor instead
     * (see {@link org.apache.sis.math.StatisticsTest} for more explanation).
     * Or test cases that are mostly insensitive to the exact sequence of numbers
     * can use the {@link Random#Random()} constructor instead.</p>
     *
     * <p>This method is rather for testing relatively complex code which are likely to behave
     * differently depending on the exact sequence of numbers. We want to use random sequence
     * of numbers in order to test the code in a wider range of scenarios. However, in case of
     * test failure, we need to know the <i>seed</i> which has been used in order to allow
     * the developer to reproduce the test with the exact same sequence of numbers.
     * Using this method, the seed can be retrieved in the messages sent to the output stream.</p>
     *
     * @return a new random number generator initialized with a random seed.
     */
    public static Random createRandomNumberGenerator() {
        final long seed = StrictMath.round(StrictMath.random() * (1L << 48));
        randomSeed.set(seed);
        return new Random(seed);
    }

    /**
     * Returns a new random number generator with the given seed. This method is used only for debugging a test failure.
     * The seed given in argument is the value printed by the test runner. This argument shall be removed after the test
     * has been fixed.
     *
     * <p>The work flow is as below:</p>
     * <ul>
     *   <li>Uses {@link #createRandomNumberGenerator()} (without argument} in tests.</li>
     *   <li>If a test fail, find the seed value printed by the test runner, then insert that value in argument
     *       to {@code createRandomNumberGenerator(…)}.</li>
     *   <li>Debug the test.</li>
     *   <li>Once the test has been fixed, remove the argument from the {@code createRandomNumberGenerator()} call.</li>
     * </ul>
     *
     * @param  seed  the random generator seed.
     * @return a new random number generator initialized with the given seed.
     */
    @Debug
    public static Random createRandomNumberGenerator(final long seed) {
        randomSeed.set(seed);
        return new Random(seed);
    }

    /**
     * Returns a copy of the given array with the last coordinate values dropped for each coordinates.
     * The array can contain many points, with an array length equals to a multiple of {@code sourceDim}.
     *
     * @param  coordinates  the source coordinates from which to drop the last coordinate values.
     * @param  sourceDim    number of dimensions of each point in the {@code coordinates} array.
     * @param  targetDim    number of dimensions to retain.
     * @return copy of the given {@code coordinates} array with only the {@code targetDim} first dimension for each point.
     */
    public static double[] dropLastDimensions(final double[] coordinates, final int sourceDim, final int targetDim) {
        assertEquals(0, coordinates.length % sourceDim, "Unexpected array length.");
        final int numPts = coordinates.length / sourceDim;
        final double[] reduced = new double[numPts * targetDim];
        for (int i=0; i<numPts; i++) {
            System.arraycopy(coordinates, i*sourceDim, reduced, i*targetDim, targetDim);
        }
        return reduced;
    }

    /**
     * Copies the full content of the given input stream in a temporary file and returns the channel for that file.
     * The file is opened with {@link StandardOpenOption#DELETE_ON_CLOSE}, together with read and write options.
     *
     * @param  data    the data to copy in the temporary file.
     * @param  suffix  suffix (dot included) to append to the temporary file name, or {@code null} if none.
     * @return a channel opened on a copy of the content of the given test resource.
     * @throws IOException if an error occurred while copying the data.
     */
    public static SeekableByteChannel createTemporaryFile(final InputStream data, final String suffix) throws IOException {
        final SeekableByteChannel channel;
        try (ReadableByteChannel in = Channels.newChannel(data)) {
            final Path file = Files.createTempFile("SIS", suffix);
            channel = Files.newByteChannel(file, StandardOpenOption.DELETE_ON_CLOSE,
                                StandardOpenOption.READ, StandardOpenOption.WRITE);
            final ByteBuffer buffer = ByteBuffer.allocate(4000);
            while (in.read(buffer) >= 0) {
                buffer.flip();
                channel.write(buffer);
                buffer.clear();
            }
        }
        return channel.position(0);
    }
}
