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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.lang.reflect.Array;
import java.util.Random;
import java.util.function.IntFunction;
import java.util.function.ToDoubleFunction;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;


/**
 * Tests {@link SubsampledRectangleWriter}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class SubsampledRectangleWriterTest extends TestCase {
    /**
     * The writer to test.
     */
    private SubsampledRectangleWriter writer;

    /**
     * The channel where the {@linkplain #writer} will write.
     */
    private ChannelDataOutput output;

    /**
     * The data actually written by the {@linkplain #writer}.
     */
    private ByteBuffer actual;

    /**
     * Value of the band offsets argument used for the test.
     */
    private int[] bandOffsets;

    /**
     * Lower value to store in the array.
     */
    private static final int BASE = 10;

    /**
     * Creates a new test case.
     */
    public SubsampledRectangleWriterTest() {
    }

    /**
     * Allocates resources for a test of a primitive type.
     *
     * @param  <A>       type of the array of primitive type.
     * @param  creator   function to invoke for creating an array of specified length.
     * @param  dataSize  size in bytes of the primitive type.
     * @return array of data which will be given to a {@code write(…)} method to test.
     * @throws IOException should never happen since we are writing in memory.
     */
    private <A> A allocate(final IntFunction<A> creator, final int dataSize) throws IOException {
        bandOffsets = new int[] {2, 1, 3, 0};

        final Random random = TestUtilities.createRandomNumberGenerator();
        final int    width  = (random.nextInt(9) + 3) * bandOffsets.length;
        final int    height = (random.nextInt(5) + 1);
        final int    length = width * height;
        final long[] lower  = new long[2];
        final long[] upper  = new long[] {width, height};
        final int[]  subsm  = new int[]  {1,1};
        final A source = creator.apply(length);
        for (int i=0; i<length; i++) {
            Array.setByte(source, i, (byte) (BASE + i));
        }
        final byte[] target = new byte[length * dataSize];
        final var buffer = ByteBuffer.allocate((random.nextInt(4) + 1) + bandOffsets.length * dataSize);
        actual = ByteBuffer.wrap(target);
        output = new ChannelDataOutput("Test", new ByteArrayChannel(target, false), buffer);
        writer = new SubsampledRectangleWriter(new Region(upper, lower, upper, subsm), bandOffsets, bandOffsets.length);
        return source;
    }

    /**
     * Verifies that the bytes written by {@linkplain #writer} are equal to the expected value.
     *
     * @param  getter    the {@link ByteBuffer} getter method corresponding to the tested type.
     * @param  dataSize  size in bytes of the primitive type.
     * @throws IOException should never happen since we are writing in memory.
     */
    private void verifyWrittenBytes(final ToDoubleFunction<ByteBuffer> getter) throws IOException {
        output.flush();
        int base = BASE;
        while (actual.hasRemaining()) {
            for (int offset : bandOffsets) {
                final double value = getter.applyAsDouble(actual);
                assertEquals((byte) (base + offset), (byte) value);
            }
            base += bandOffsets.length;
        }
    }

    /**
     * Tests the {@link HyperRectangleWriter#write(ChannelDataOutput, byte[], int, boolean)} method.
     *
     * @throws IOException should never happen since we are writing in memory.
     */
    @Test
    public void testWriteBytes() throws IOException {
        final byte[] source = allocate(byte[]::new, Byte.BYTES);
        writer.write(output, source, 0, false);
        verifyWrittenBytes(ByteBuffer::get);
    }

    /**
     * Tests the {@link HyperRectangleWriter#write(ChannelDataOutput, short[], int)} method.
     *
     * @throws IOException should never happen since we are writing in memory.
     */
    @Test
    public void testWriteShorts() throws IOException {
        final short[] source = allocate(short[]::new, Short.BYTES);
        writer.write(output, source, 0);
        verifyWrittenBytes(ByteBuffer::getShort);
    }

    /**
     * Tests the {@link HyperRectangleWriter#write(ChannelDataOutput, int[], int)} method.
     *
     * @throws IOException should never happen since we are writing in memory.
     */
    @Test
    public void testWriteInts() throws IOException {
        final int[] source = allocate(int[]::new, Integer.BYTES);
        writer.write(output, source, 0);
        verifyWrittenBytes(ByteBuffer::getInt);
    }

    /**
     * Tests the {@link HyperRectangleWriter#write(ChannelDataOutput, long[], int)} method.
     *
     * @throws IOException should never happen since we are writing in memory.
     */
    @Test
    public void testWriteLongs() throws IOException {
        final long[] source = allocate(long[]::new, Long.BYTES);
        writer.write(output, source, 0);
        verifyWrittenBytes(ByteBuffer::getLong);
    }

    /**
     * Tests the {@link HyperRectangleWriter#write(ChannelDataOutput, float[], int)} method.
     *
     * @throws IOException should never happen since we are writing in memory.
     */
    @Test
    public void testWriteFloats() throws IOException {
        final float[] source = allocate(float[]::new, Float.BYTES);
        writer.write(output, source, 0);
        verifyWrittenBytes(ByteBuffer::getFloat);
    }

    /**
     * Tests the {@link HyperRectangleWriter#write(ChannelDataOutput, double[], int)} method.
     *
     * @throws IOException should never happen since we are writing in memory.
     */
    @Test
    public void testWriteDoubles() throws IOException {
        final double[] source = allocate(double[]::new, Double.BYTES);
        writer.write(output, source, 0);
        verifyWrittenBytes(ByteBuffer::getDouble);
    }
}
