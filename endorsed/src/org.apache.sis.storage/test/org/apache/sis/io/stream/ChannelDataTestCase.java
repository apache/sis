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
package org.apache.sis.io.stream;

import java.util.Random;
import java.io.IOException;
import org.apache.sis.util.Debug;

// Test dependencies
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;


/**
 * Base class of {@link ChannelDataInputTest} and {@link ChannelDataOutputTest}.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class ChannelDataTestCase extends TestCase {
    /**
     * The maximal length of the arrays to be read or written from/to the channel, in bytes.
     * This size may be smaller or greater than the buffer capacity, but a greater size is
     * recommended in order to test the {@link ChannelData} capability to split a read or
     * write operation in more than one access to the channel.
     */
    static final int ARRAY_MAX_LENGTH = 256;

    /**
     * The minimal capacity of the buffer to use for write operations.
     */
    static final int BUFFER_MIN_CAPACITY = Double.BYTES;

    /**
     * The maximal capacity of the buffer to use for write operations.
     */
    static final int BUFFER_MAX_CAPACITY = ARRAY_MAX_LENGTH / 4;

    /**
     * The size of the {@link ByteArrayChannel} backing array.
     * A greater size increases the number of iterations performed by test methods.
     */
    static final int STREAM_LENGTH = ARRAY_MAX_LENGTH * 1024;

    /**
     * Random number generator used for tests.
     */
    final Random random;

    /**
     * Creates a new test case.
     */
    ChannelDataTestCase() {
        random = TestUtilities.createRandomNumberGenerator();
    }

    /**
     * Edit this number if break point is desired in the execution of {@link #transferRandomData(int)}.
     * The breakpoint value is the value given by the {@code "Iter. count"} line on the console.
     *
     * <p>This breakpoint is useful only if the {@link #random} has been fixed to a constant seed.</p>
     */
    @Debug
    private static final int BREAKPOINT = 0;

    /**
     * Invoked when the iteration count reaches the {@link #BREAKPOINT} value.
     */
    @Debug
    void breakpoint() {
    }

    /**
     * Returns a random buffer capacity between {@value #BUFFER_MIN_CAPACITY}
     * and {@value #BUFFER_MAX_CAPACITY} inclusive.
     */
    final int randomBufferCapacity() {
        return random.nextInt(BUFFER_MAX_CAPACITY - BUFFER_MIN_CAPACITY + 1) + BUFFER_MIN_CAPACITY;
    }

    /**
     * Transfers many random data between the channel and the buffer.
     * This method invokes {@link #transferRandomData(int)} in a loop,
     * with an operation identifier selected randomly between 0 inclusive to {@code numOperations} exclusive.
     *
     * @param  testedStream   the stream to be tested.
     * @param  length         length threshold for stopping the test. Shall be the backing array length
     *                        minus {@link #ARRAY_MAX_LENGTH}, in order to keep a margin for test cases.
     * @param  numOperations  number of operations to be tested randomly.
     * @throws IOException if an I/O error occurred.
     */
    final void transferRandomData(final ChannelData testedStream, final int length, final int numOperations)
            throws IOException
    {
        long position  = 0;
        int  bitOffset = 0;
        int  operation = 0;
        int  count     = 0;
        try {
            while ((position = testedStream.getStreamPosition()) < length) {
                if (++count == BREAKPOINT) {
                    breakpoint();
                }
                bitOffset = testedStream.getBitOffset();
                operation = random.nextInt(numOperations);
                transferRandomData(operation);
            }
        } catch (AssertionError | RuntimeException e) {
            out.println("Iter. count: " + count);
            out.println("Position:    " + position);
            out.println("Bit offset:  " + bitOffset);
            out.println("Byte order:  " + testedStream.buffer.order());
            out.println("Operation:   " + operation);
            out.println("Exception:   " + e.getLocalizedMessage());
            throw e;
        }
    }

    /**
     * Transfers random data using a method selected randomly.
     *
     * @param  operation  an identifier of the method to use for transfering data.
     * @throws IOException if an I/O error occurred.
     */
    abstract void transferRandomData(final int operation) throws IOException;

    /**
     * Creates an array filled with random values.
     *
     * @param length The length of the array to create.
     */
    final byte[] createRandomArray(final int length) {
        final byte[] array = new byte[length];
        random.nextBytes(array);
        return array;
    }

    /** Returns a new array of bytes or random length. */
    final byte[] randomBytes() {
        final byte[] array = new byte[random.nextInt(ARRAY_MAX_LENGTH / Byte.BYTES)];
        random.nextBytes(array);
        return array;
    }

    /** Returns a new array of characters or random length. */
    final char[] randomChars() {
        final char[] array = new char[random.nextInt(ARRAY_MAX_LENGTH / Character.BYTES)];
        for (int i=0; i<array.length; i++) array[i] = (char) random.nextInt(1 << Character.SIZE);
        return array;
    }

    /** Returns a new array of short integers or random length. */
    final short[] randomShorts() {
        final short[] array = new short[random.nextInt(ARRAY_MAX_LENGTH / Short.BYTES)];
        for (int i=0; i<array.length; i++) array[i] = (short) random.nextInt(1 << Short.SIZE);
        return array;
    }

    /** Returns a new array of integers or random length. */
    final int[] randomInts() {
        final int[] array = new int[random.nextInt(ARRAY_MAX_LENGTH / Integer.BYTES)];
        for (int i=0; i<array.length; i++) array[i] = random.nextInt();
        return array;
    }

    /** Returns a new array of long integers or random length. */
    final long[] randomLongs() {
        final long[] array = new long[random.nextInt(ARRAY_MAX_LENGTH / Long.BYTES)];
        for (int i=0; i<array.length; i++) array[i] = random.nextLong();
        return array;
    }

    /** Returns a new array of single-precision floating point values or random length. */
    final float[] randomFloats() {
        final float[] array = new float[random.nextInt(ARRAY_MAX_LENGTH / Float.BYTES)];
        for (int i=0; i<array.length; i++) array[i] = random.nextFloat();
        return array;
    }

    /** Returns a new array of double-precision floating point values or random length. */
    final double[] randomDoubles() {
        final double[] array = new double[random.nextInt(ARRAY_MAX_LENGTH / Double.BYTES)];
        for (int i=0; i<array.length; i++) array[i] = random.nextDouble();
        return array;
    }

    /**
     * Randomly returns {@code true} for relatively rare events. The frequency is adjusted
     * in order to have about 1024 events in a stream of length {@link #STREAM_LENGTH}.
     */
    final boolean randomEvent() {
        return random.nextInt(STREAM_LENGTH / 1024) == 0;
    }
}
