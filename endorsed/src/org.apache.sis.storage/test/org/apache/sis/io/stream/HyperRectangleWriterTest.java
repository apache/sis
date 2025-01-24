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
import java.awt.image.Raster;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.Arrays;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;


/**
 * Tests {@link HyperRectangleWriter}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class HyperRectangleWriterTest extends TestCase {
    /**
     * The writer to test.
     */
    private HyperRectangleWriter writer;

    /**
     * The channel where the {@linkplain #writer} will write.
     */
    private ChannelDataOutput output;

    /**
     * The data actually written by the {@linkplain #writer}.
     */
    private ByteBuffer actual;

    /**
     * Indices of the first value written along each dimension (inclusive).
     */
    private int lowerX, lowerY, lowerZ;

    /**
     * Indices after the last value written along each dimension (exclusive).
     */
    private int upperX, upperY, upperZ;

    /**
     * Subsampling along each dimension. Shall be greater than zero.
     * A value of 1 means no subsampling.
     */
    private int subsamplingX, subsamplingY, subsamplingZ;

    /**
     * An arbitrary offset for the first valid element in the arrays to write.
     */
    private int offset;

    /**
     * Creates a new test case.
     */
    public HyperRectangleWriterTest() {
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
        final Random random = TestUtilities.createRandomNumberGenerator();
        final int sourceSizeX, sourceSizeY, sourceSizeZ, sourceLength;
        subsamplingX = random.nextInt(2) + 1;
        subsamplingY = random.nextInt(2) + 1;
        subsamplingZ = random.nextInt(2) + 1;
        offset       = random.nextInt(4);
        lowerX       = random.nextInt(5);
        lowerY       = random.nextInt(5);
        lowerZ       = random.nextInt(5);
        upperX       = random.nextInt(5) + lowerX + subsamplingX * 3;
        upperY       = random.nextInt(5) + lowerY + subsamplingY * 3;
        upperZ       = random.nextInt(5) + lowerZ + subsamplingZ * 3;
        sourceSizeX  = random.nextInt(5) + upperX;      // Number of columns in source array.
        sourceSizeY  = random.nextInt(5) + upperY;      // Number of rows in source array.
        sourceSizeZ  = random.nextInt(5) + upperZ;
        sourceLength = sourceSizeX * sourceSizeY * sourceSizeZ;
        final A source = creator.apply(sourceLength + offset);
        for (int i=0; i<sourceLength; i++) {
            final int x = i % sourceSizeX;
            final int y = (i / sourceSizeX) % sourceSizeY;
            final int z = i / (sourceSizeX * sourceSizeY);
            Array.setShort(source, offset + i, (short) (1000 + (z*10 + y) * 10 + x));
        }
        final var region = new Region(
                new long[] {sourceSizeX,  sourceSizeY,  sourceSizeZ},
                new long[] {lowerX,       lowerY,       lowerZ},
                new long[] {upperX,       upperY,       upperZ},
                new long[] {subsamplingX, subsamplingY, subsamplingZ});

        final byte[] target = new byte[dataSize * region.targetLength(3)];
        final var buffer = ByteBuffer.allocate(random.nextInt(10) + Double.BYTES);
        actual = ByteBuffer.wrap(target);
        output = new ChannelDataOutput("Test", new ByteArrayChannel(target, false), buffer);
        writer = new HyperRectangleWriter(region);
        return source;
    }

    /**
     * Verifies that the bytes written by {@linkplain #writer} are equal to the expected value.
     *
     * @param  getter    the {@link ByteBuffer} getter method corresponding to the tested type.
     * @param  dataSize  size in bytes of the primitive type.
     * @throws IOException should never happen since we are writing in memory.
     */
    private void verifyWrittenBytes(final ToDoubleFunction<ByteBuffer> getter, final int dataSize) throws IOException {
        output.flush();
        for (int z = lowerZ; z < upperZ; z += subsamplingZ) {
            for (int y = lowerY; y < upperY; y += subsamplingY) {
                for (int x = lowerX; x < upperX; x += subsamplingX) {
                    assertEquals(1000 + (z*10 + y) * 10 + x, getter.applyAsDouble(actual),
                            () -> "At index " + (actual.position() / dataSize - 1));
                }
            }
        }
        assertEquals(0, actual.remaining());
    }

    /**
     * Tests the {@link HyperRectangleWriter#write(ChannelDataOutput, short[], int)} method.
     *
     * @throws IOException should never happen since we are writing in memory.
     */
    @Test
    public void testWriteShorts() throws IOException {
        final short[] source = allocate(short[]::new, Short.BYTES);
        writer.write(output, source, offset);
        verifyWrittenBytes(ByteBuffer::getShort, Short.BYTES);
    }

    /**
     * Tests the {@link HyperRectangleWriter#write(ChannelDataOutput, int[], int)} method.
     *
     * @throws IOException should never happen since we are writing in memory.
     */
    @Test
    public void testWriteInts() throws IOException {
        final int[] source = allocate(int[]::new, Integer.BYTES);
        writer.write(output, source, offset);
        verifyWrittenBytes(ByteBuffer::getInt, Integer.BYTES);
    }

    /**
     * Tests the {@link HyperRectangleWriter#write(ChannelDataOutput, long[], int)} method.
     *
     * @throws IOException should never happen since we are writing in memory.
     */
    @Test
    public void testWriteLongs() throws IOException {
        final long[] source = allocate(long[]::new, Long.BYTES);
        writer.write(output, source, offset);
        verifyWrittenBytes(ByteBuffer::getLong, Long.BYTES);
    }

    /**
     * Tests the {@link HyperRectangleWriter#write(ChannelDataOutput, float[], int)} method.
     *
     * @throws IOException should never happen since we are writing in memory.
     */
    @Test
    public void testWriteFloats() throws IOException {
        final float[] source = allocate(float[]::new, Float.BYTES);
        writer.write(output, source, offset);
        verifyWrittenBytes(ByteBuffer::getFloat, Float.BYTES);
    }

    /**
     * Tests the {@link HyperRectangleWriter#write(ChannelDataOutput, double[], int)} method.
     *
     * @throws IOException should never happen since we are writing in memory.
     */
    @Test
    public void testWriteDoubles() throws IOException {
        final double[] source = allocate(double[]::new, Double.BYTES);
        writer.write(output, source, offset);
        verifyWrittenBytes(ByteBuffer::getDouble, Double.BYTES);
    }

    /**
     * Tests writing a binary image through the builder.
     *
     * @throws IOException should never happen since we are writing in memory.
     */
    @Test
    public void testBinaryImage() throws IOException {
        final var image = new BufferedImage(10, 4, BufferedImage.TYPE_BYTE_BINARY);
        final var tile = image.getRaster();
        for (int y = tile.getHeight(); --y >= 0;) {
            for (int x = tile.getWidth(); --x >= 0;) {
                tile.setSample(x, y, 0, x ^ y);
            }
        }
        assertArrayEquals(new byte[] {
            (byte) 0b01010101, (byte) 0b01000000,
            (byte) 0b10101010, (byte) 0b10000000,
            (byte) 0b01010101, (byte) 0b01000000,
            (byte) 0b10101010, (byte) 0b10000000,
        }, writePixelValues(tile));
    }

    /**
     * Writes the pixel values of the given raster and returns the written bytes.
     * This method assumes that the raster uses {@link DataBufferByte}.
     *
     * @param  tile  the tile to write.
     * @return written pixel values.
     * @throws IOException should never happen since we are writing in memory.
     */
    private byte[] writePixelValues(final Raster tile) throws IOException {
        final var container = new ByteArrayChannel(new byte[40], false);
        output = new ChannelDataOutput("Test", container, ByteBuffer.allocate(20));
        writer = new HyperRectangleWriter.Builder().create(tile, -1, -1);
        writer.write(output, ((DataBufferByte) tile.getDataBuffer()).getData(), 0, false);
        output.flush();
        actual = container.toBuffer();
        return Arrays.copyOfRange(actual.array(), 0, actual.limit());
    }
}
