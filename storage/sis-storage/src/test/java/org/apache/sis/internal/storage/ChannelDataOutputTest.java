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
package org.apache.sis.internal.storage;


import java.util.Arrays;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.apache.sis.test.DependsOnMethod;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link ChannelDataOutput}.
 * First we write into two different output streams, then we compare theirs written byte array.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public final strictfp class ChannelDataOutputTest extends ChannelDataTestCase {
    /**
     * The {@link DataOutput} implementation to test. This implementation will write data to
     * {@link #testedStreamBackingArray}. The content of that array will be compared to
     * {@link #expectedData} for verifying result correctness.
     */
    private ChannelDataOutput testedStream;

    /**
     * A stream to use as a reference implementation. Any data written in {@link #testedStream}
     * will also be written in {@code referenceStream}, for later comparison.
     */
    private DataOutput referenceStream;

    /**
     * Byte array which is filled by the {@linkplain #testedStream} implementation during write operations.
     * The content of this array will be compared to {@linkplain #expectedData}.
     */
    private byte[] testedStreamBackingArray;

    /**
     * Object which is filled by {@link #referenceStream} implementation during write operations.
     * <b>Do not write to this stream</b> - this field is kept only for invocation of
     * {@link ByteArrayOutputStream#toByteArray()}.
     */
    private ByteArrayOutputStream expectedData;

    /**
     * Initializes all non-final fields before to execute a test.
     *
     * @param  testName     The name of the test method to be executed.
     * @param  streamLength Length of stream to create.
     * @param  bufferLength Length of the {@code ByteBuffer} to use for the tests.
     * @throws IOException Should never happen.
     */
    private void initialize(final String testName, final int streamLength, final int bufferLength) throws IOException {
        testedStreamBackingArray = new byte[streamLength];
        testedStream             = new ChannelDataOutput(testName, new ByteArrayChannel(testedStreamBackingArray), ByteBuffer.allocate(bufferLength));
        expectedData             = new ByteArrayOutputStream(streamLength);
        referenceStream          = new DataOutputStream(expectedData);
    }

    /**
     * Fills a stream with random data and compares the result with a reference output stream.
     * We allocate a small buffer for the {@code ChannelDataOutput} in order to force frequent
     * interactions between the buffer and the channel.
     *
     * @throws IOException Should never happen.
     */
    @Test
    public void testAllWriteMethods() throws IOException {
        initialize("testAllWriteMethods", STREAM_LENGTH, random.nextInt(BUFFER_MAX_CAPACITY) + Double.BYTES);
        writeInStreams();
        flush();
        final byte[] expectedArray = expectedData.toByteArray();
        assertArrayEquals(expectedArray, Arrays.copyOf(testedStreamBackingArray, expectedArray.length));
    }

    /**
     * Tests write operations followed by seek operations.
     *
     * @throws IOException Should never happen.
     */
    @Test
    @DependsOnMethod("testAllWriteMethods")
    public void testWriteAndSeek() throws IOException {
        initialize("testWriteAndSeek", STREAM_LENGTH, random.nextInt(BUFFER_MAX_CAPACITY) + Double.BYTES);
        writeInStreams();
        final byte[] expectedArray = expectedData.toByteArray();
        final int seekRange = expectedArray.length - Long.BYTES;
        final ByteBuffer arrayView = ByteBuffer.wrap(expectedArray);
        for (int i=0; i<100; i++) {
            final int position = random.nextInt(seekRange);
            testedStream.seek(position);
            assertEquals("getStreamPosition()", position, testedStream.getStreamPosition());
            final long v = random.nextLong();
            testedStream.writeLong(v);
            arrayView.putLong(position, v);
        }
        flush();
        assertArrayEquals(expectedArray, Arrays.copyOf(testedStreamBackingArray, expectedArray.length));
    }

    /**
     * Tests {@link ChannelDataOutput#seek(long)} with an invalid seek position.
     *
     * @throws IOException Should never happen.
     */
    @Test
    public void testInvalidSeek() throws IOException {
        initialize("dataOutput : fail test", 20, 20);
        try {
            testedStream.seek(1);
            fail("Shall not seek further than stream length.");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("position"));
        }
    }

    /**
     * Flushes the streams.
     *
     * @throws IOException Should never happen.
     */
    private void flush() throws IOException {
        testedStream.flush();
    }

    /**
     * Writes the same random data in both {@link #testedStream} and {@link #referenceStream}.
     *
     * @throws IOException Should never happen.
     */
    private void writeInStreams() throws IOException {
        while (testedStream.getStreamPosition() < testedStreamBackingArray.length - ARRAY_MAX_LENGTH) {
            final int operation = random.nextInt(16);
            switch (operation) {
                case 0: {
                    final byte v = (byte) random.nextInt(1 << Byte.SIZE);
                    referenceStream.writeByte(v);
                    testedStream.writeByte(v);
                    break;
                }
                case 1: {
                    final short v = (short) random.nextInt(1 << Short.SIZE);
                    referenceStream.writeShort(v);
                    testedStream.writeShort(v);
                    break;
                }
                case 2: {
                    final char v = (char) random.nextInt(1 << Character.SIZE);
                    referenceStream.writeChar(v);
                    testedStream.writeChar(v);
                    break;
                }
                case 3: {
                    final int v = random.nextInt();
                    referenceStream.writeInt(v);
                    testedStream.writeInt(v);
                    break;
                }
                case 4: {
                    final long v = random.nextLong();
                    referenceStream.writeLong(v);
                    testedStream.writeLong(v);
                    break;
                }
                case 5: {
                    final float v = random.nextFloat();
                    referenceStream.writeFloat(v);
                    testedStream.writeFloat(v);
                    break;
                }
                case 6: {
                    final double v = random.nextDouble();
                    referenceStream.writeDouble(v);
                    testedStream.writeDouble(v);
                    break;
                }
                case 7: {
                    final byte[] tmp = new byte[random.nextInt(ARRAY_MAX_LENGTH / Byte.BYTES)];
                    random.nextBytes(tmp);
                    referenceStream.write(tmp);
                    testedStream.write(tmp);
                    break;
                }
                case 8: {
                    final char[] tmp = new char[random.nextInt(ARRAY_MAX_LENGTH / Character.BYTES)];
                    for (int i=0; i<tmp.length; i++) {
                        referenceStream.writeChar(tmp[i] = (char) random.nextInt(1 << Character.SIZE));
                    }
                    testedStream.writeChars(tmp);
                    break;
                }
                case 9: {
                    final short[] tmp = new short[random.nextInt(ARRAY_MAX_LENGTH / Short.BYTES)];
                    for (int i=0; i<tmp.length; i++) {
                        referenceStream.writeShort(tmp[i] = (short) random.nextInt(1 << Short.SIZE));
                    }
                    testedStream.writeShorts(tmp);
                    break;
                }
                case 10: {
                    final int[] tmp = new int[random.nextInt(ARRAY_MAX_LENGTH / Integer.BYTES)];
                    for (int i=0; i<tmp.length; i++) {
                        referenceStream.writeInt(tmp[i] = random.nextInt());
                    }
                    testedStream.writeInts(tmp);
                    break;
                }
                case 11: {
                    final long[] tmp = new long[random.nextInt(ARRAY_MAX_LENGTH / Long.BYTES)];
                    for (int i=0; i<tmp.length; i++) {
                        referenceStream.writeLong(tmp[i] = random.nextLong());
                    }
                    testedStream.writeLongs(tmp);
                    break;
                }
                case 12: {
                    final float[] tmp = new float[random.nextInt(ARRAY_MAX_LENGTH / Float.BYTES)];
                    for (int i=0; i<tmp.length; i++) {
                        referenceStream.writeFloat(tmp[i] = random.nextFloat());
                    }
                    testedStream.writeFloats(tmp);
                    break;
                }
                case 13: {
                    final double[] tmp = new double[random.nextInt(ARRAY_MAX_LENGTH / Double.BYTES)];
                    for (int i=0; i<tmp.length; i++) {
                        referenceStream.writeDouble(tmp[i] = random.nextDouble());
                    }
                    testedStream.writeDoubles(tmp);
                    break;
                }
                case 14: {
                    final String str = "test : ChannelDataOutput";
//                  referenceStream.writeChars(str);
//                  testedStream.writeChars(str);
                    break;
                }
                case 15: {
                    final String str = "お元気ですか";
                    final byte[] array = str.getBytes("UTF-8");
                    assertEquals(str.length() * 3, array.length); // Sanity check.
//                  referenceStream.writeUTF(str);
//                  testedStream.writeUTF(str);
                    break;
                }
                default: throw new AssertionError(operation);
            }
        }
    }
}
