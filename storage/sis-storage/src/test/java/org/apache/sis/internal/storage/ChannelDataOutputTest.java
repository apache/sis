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
import java.io.Closeable;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.InvalidMarkException;
import java.nio.channels.ByteChannel;
import javax.imageio.stream.ImageOutputStream;
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
public strictfp class ChannelDataOutputTest extends ChannelDataTestCase {
    /**
     * The {@link DataOutput} implementation to test. This implementation will write data to
     * {@link #testedStreamBackingArray}. The content of that array will be compared to
     * {@link #expectedData} for verifying result correctness.
     */
    ChannelDataOutput testedStream;

    /**
     * A stream to use as a reference implementation. Any data written in {@link #testedStream}
     * will also be written in {@code referenceStream}, for later comparison.
     */
    DataOutput referenceStream;

    /**
     * Byte array which is filled by the {@linkplain #testedStream} implementation during write operations.
     * The content of this array will be compared to {@linkplain #expectedData}.
     */
    byte[] testedStreamBackingArray;

    /**
     * Object which is filled by {@link #referenceStream} implementation during write operations.
     * <b>Do not write to this stream</b> - this field is kept only for invocation of
     * {@link ByteArrayOutputStream#toByteArray()}.
     */
    ByteArrayOutputStream expectedData;

    /**
     * Initializes all non-final fields before to execute a test.
     *
     * @param  testName     The name of the test method to be executed.
     * @param  streamLength Length of stream to create.
     * @param  bufferLength Length of the {@code ByteBuffer} to use for the tests.
     * @throws IOException Should never happen.
     */
    void initialize(final String testName, final int streamLength, final int bufferLength) throws IOException {
        expectedData             = new ByteArrayOutputStream(streamLength);
        referenceStream          = new DataOutputStream(expectedData);
        testedStreamBackingArray = new byte[streamLength];
        testedStream             = new ChannelDataOutput(testName,
                new ByteArrayChannel(testedStreamBackingArray, false), ByteBuffer.allocate(bufferLength));
    }

    /**
     * May be invoked after {@link #initialize(String, int, int)} for replacing the seekable byte channel
     * by a non-seekable one. Used for testing different code paths in {@link ChannelDataOutput}.
     */
    private void nonSeekableChannel() throws IOException {
        final ByteChannel channel = (ByteChannel) testedStream.channel;
        testedStream = new ChannelDataOutput(testedStream.filename, new ByteChannel() {
            @Override public boolean isOpen()                                 {return channel.isOpen();}
            @Override public int     read(ByteBuffer dst)  throws IOException {return channel.read(dst);}
            @Override public int     write(ByteBuffer src) throws IOException {return channel.write(src);}
            @Override public void    close()               throws IOException {channel.close();}
        }, testedStream.buffer);
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
        initialize("testAllWriteMethods", STREAM_LENGTH, random.nextInt(BUFFER_MAX_CAPACITY) + (Double.SIZE / Byte.SIZE));
        writeInStreams();
        assertStreamContentEquals();
    }

    /**
     * Asserts that the content of {@link #testedStream} is equals to the content of {@link #referenceStream}.
     * This method closes the reference stream before to perform the comparison.
     */
    final void assertStreamContentEquals() throws IOException {
        testedStream.flush();
        ((Closeable) referenceStream).close();
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
        initialize("testWriteAndSeek", STREAM_LENGTH, random.nextInt(BUFFER_MAX_CAPACITY) + (Double.SIZE / Byte.SIZE));
        writeInStreams();
        ((Closeable) referenceStream).close();
        final byte[] expectedArray = expectedData.toByteArray();
        final int seekRange = expectedArray.length - (Long.SIZE / Byte.SIZE);
        final ByteBuffer arrayView = ByteBuffer.wrap(expectedArray);
        for (int i=0; i<100; i++) {
            final int position = random.nextInt(seekRange);

            // JDK6 specific: can not seek to arbitrary position because
            // Java 6 does not provide the SeekableByteChannel interface.
            if (position < testedStream.getFlushedPosition()) {
                continue;
            }
            // End of JDK6 specific.

            testedStream.seek(position);
            assertEquals("getStreamPosition()", position, testedStream.getStreamPosition());
            final long v = random.nextLong();
            testedStream.writeLong(v);
            arrayView.putLong(position, v);
        }
        testedStream.flush();
        assertArrayEquals(expectedArray, Arrays.copyOf(testedStreamBackingArray, expectedArray.length));
    }

    /**
     * Tests seeking ahead of buffer capacity.
     *
     * @throws IOException Should never happen.
     */
    @Test
    @DependsOnMethod("testWriteAndSeek")
    public void testSeekAhead() throws IOException {
        testSeekAhead(true);
        testSeekAhead(false);
    }

    /**
     * Implementation of {@link #testSeekAhead()} method, testing two different code paths depending
     * on the {@code seekable} argument value. Note: the two code paths are actually identical on JDK 6.
     */
    private void testSeekAhead(final boolean seekable) throws IOException {
        initialize("testArgumentChecks", 48, 10);
        if (!seekable) {
            nonSeekableChannel();
        }
        for (int i=0; i<3; i++) {
            final long v = random.nextLong();
            referenceStream.writeLong(v);
            testedStream.writeLong(v);
        }
        assertEquals("getStreamPosition()", 24, testedStream.getStreamPosition());
        testedStream.seek(40); // Move 2 long ahead. Space shall be filled by 0.
        referenceStream.writeLong(0);
        referenceStream.writeLong(0);
        final long v = random.nextLong();
        referenceStream.writeLong(v);
        testedStream.writeLong(v);
        assertStreamContentEquals();
    }

    /**
     * Tests the argument checks performed by various methods. For example this method
     * tests {@link ChannelDataOutput#seek(long)} with an invalid seek position.
     *
     * @throws IOException Should never happen.
     */
    @Test
    public void testArgumentChecks() throws IOException {
        initialize("testArgumentChecks", 20, 10);
        try {
            testedStream.setBitOffset(9);
            fail("Shall not accept invalid bitOffset.");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("bitOffset"));
        }
        try {
            testedStream.reset();
            fail("Shall not accept reset without mark.");
        } catch (InvalidMarkException e) {
            // This is the expected exception.
        }
        /*
         * flushBefore(int).
         */
        final int v = random.nextInt();
        referenceStream.writeShort(v);
        testedStream.writeShort(v);
        testedStream.flushBefore(0); // Valid.
        try {
            testedStream.flushBefore(3);
            fail("Shall not flush at a position greater than buffer limit.");
        } catch (IndexOutOfBoundsException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("position"));
        }
        testedStream.flush();
        try {
            testedStream.flushBefore(0);
            fail("Shall not flush at a position before buffer base.");
        } catch (IndexOutOfBoundsException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("position"));
        }
        assertStreamContentEquals();
    }

    /**
     * Writes the same random data in both {@link #testedStream} and {@link #referenceStream}.
     *
     * @throws IOException Should never happen.
     */
    private void writeInStreams() throws IOException {
        transferRandomData(testedStream, testedStreamBackingArray.length - ARRAY_MAX_LENGTH,
                (testedStream instanceof DataOutput) ? 21 : 14);
    }

    /**
     * Writes a random unit of data using a method selected randomly.
     * This method is invoked (indirectly) by {@link #writeInStreams()}.
     */
    @Override
    final void transferRandomData(final int operation) throws IOException {
        final DataOutput r = this.referenceStream;
        final ChannelDataOutput t = this.testedStream;
        switch (operation) {
            case 0: {
                final byte v = (byte) random.nextInt(1 << Byte.SIZE);
                r.writeByte(v);
                t.writeByte(v);
                break;
            }
            case 1: {
                final short v = (short) random.nextInt(1 << Short.SIZE);
                r.writeShort(v);
                t.writeShort(v);
                break;
            }
            case 2: {
                final char v = (char) random.nextInt(1 << Character.SIZE);
                r.writeChar(v);
                t.writeChar(v);
                break;
            }
            case 3: {
                final int v = random.nextInt();
                r.writeInt(v);
                t.writeInt(v);
                break;
            }
            case 4: {
                final long v = random.nextLong();
                r.writeLong(v);
                t.writeLong(v);
                break;
            }
            case 5: {
                final float v = random.nextFloat();
                r.writeFloat(v);
                t.writeFloat(v);
                break;
            }
            case 6: {
                final double v = random.nextDouble();
                r.writeDouble(v);
                t.writeDouble(v);
                break;
            }
            case 7: {
                final byte[] tmp = new byte[random.nextInt(ARRAY_MAX_LENGTH / (Byte.SIZE / Byte.SIZE))];
                random.nextBytes(tmp);
                r.write(tmp);
                t.write(tmp);
                break;
            }
            case 8: {
                final char[] tmp = new char[random.nextInt(ARRAY_MAX_LENGTH / (Character.SIZE / Byte.SIZE))];
                for (int i=0; i<tmp.length; i++) {
                    tmp[i] = (char) random.nextInt(1 << Character.SIZE);
                    if (!(r instanceof ImageOutputStream)) {
                        r.writeChar(tmp[i]);
                    }
                }
                if (r instanceof ImageOutputStream) {
                    ((ImageOutputStream) r).writeChars(tmp, 0, tmp.length);
                }
                t.writeChars(tmp);
                break;
            }
            case 9: {
                final short[] tmp = new short[random.nextInt(ARRAY_MAX_LENGTH / (Short.SIZE / Byte.SIZE))];
                for (int i=0; i<tmp.length; i++) {
                    tmp[i] = (short) random.nextInt(1 << Short.SIZE);
                    if (!(r instanceof ImageOutputStream)) {
                        r.writeShort(tmp[i]);
                    }
                }
                if (r instanceof ImageOutputStream) {
                    ((ImageOutputStream) r).writeShorts(tmp, 0, tmp.length);
                }
                t.writeShorts(tmp);
                break;
            }
            case 10: {
                final int[] tmp = new int[random.nextInt(ARRAY_MAX_LENGTH / (Integer.SIZE / Byte.SIZE))];
                for (int i=0; i<tmp.length; i++) {
                    tmp[i] = random.nextInt();
                    if (!(r instanceof ImageOutputStream)) {
                        r.writeInt(tmp[i]);
                    }
                }
                if (r instanceof ImageOutputStream) {
                    ((ImageOutputStream) r).writeInts(tmp, 0, tmp.length);
                }
                t.writeInts(tmp);
                break;
            }
            case 11: {
                final long[] tmp = new long[random.nextInt(ARRAY_MAX_LENGTH / (Long.SIZE / Byte.SIZE))];
                for (int i=0; i<tmp.length; i++) {
                    tmp[i] = random.nextLong();
                    if (!(r instanceof ImageOutputStream)) {
                        r.writeLong(tmp[i]);
                    }
                }
                if (r instanceof ImageOutputStream) {
                    ((ImageOutputStream) r).writeLongs(tmp, 0, tmp.length);
                }
                t.writeLongs(tmp);
                break;
            }
            case 12: {
                final float[] tmp = new float[random.nextInt(ARRAY_MAX_LENGTH / (Float.SIZE / Byte.SIZE))];
                for (int i=0; i<tmp.length; i++) {
                    tmp[i] = random.nextFloat();
                    if (!(r instanceof ImageOutputStream)) {
                        r.writeFloat(tmp[i]);
                    }
                }
                if (r instanceof ImageOutputStream) {
                    ((ImageOutputStream) r).writeFloats(tmp, 0, tmp.length);
                }
                t.writeFloats(tmp);
                break;
            }
            case 13: {
                final double[] tmp = new double[random.nextInt(ARRAY_MAX_LENGTH / (Double.SIZE / Byte.SIZE))];
                for (int i=0; i<tmp.length; i++) {
                    tmp[i] = random.nextDouble();
                    if (!(r instanceof ImageOutputStream)) {
                        r.writeDouble(tmp[i]);
                    }
                }
                if (r instanceof ImageOutputStream) {
                    ((ImageOutputStream) r).writeDoubles(tmp, 0, tmp.length);
                }
                t.writeDoubles(tmp);
                break;
            }
            /*
             * Cases below this point are executed only by ChannelImageOutputStreamTest.
             */
            case 14: {
                final long v = random.nextLong();
                final int numBits = random.nextInt(Byte.SIZE);
                ((ImageOutputStream) r).writeBits(v, numBits);
                t.writeBits(v, numBits);
                break;
            }
            case 15: {
                final boolean v = random.nextBoolean();
                r.writeBoolean(v);
                ((DataOutput) t).writeBoolean(v);
                break;
            }
            case 16: {
                final String s = "Byte sequence";
                r.writeBytes(s);
                ((DataOutput) t).writeBytes(s);
                break;
            }
            case 17: {
                final String s = "Character sequence";
                r.writeChars(s);
                ((DataOutput) t).writeChars(s);
                break;
            }
            case 18: {
                final String s = "お元気ですか";
                final byte[] array = s.getBytes("UTF-8");
                assertEquals(s.length() * 3, array.length); // Sanity check.
                r.writeUTF(s);
                ((DataOutput) t).writeUTF(s);
                break;
            }
            case 19: {
                final ImageOutputStream ir = (ImageOutputStream) r;
                long flushedPosition = StrictMath.max(ir.getFlushedPosition(), t.getFlushedPosition());
                flushedPosition += random.nextInt(1 + (int) (ir.getStreamPosition() - flushedPosition));
                ir.flushBefore(flushedPosition);
                t .flushBefore(flushedPosition);
                break;
            }
            case 20: {
                ((ImageOutputStream) r).flush();
                t.flush();
                break;
            }
            default: throw new AssertionError(operation);
        }
        if (r instanceof ImageOutputStream) {
            assertEquals("getBitOffset()",      ((ImageOutputStream) r).getBitOffset(),      t.getBitOffset());
            assertEquals("getStreamPosition()", ((ImageOutputStream) r).getStreamPosition(), t.getStreamPosition());
        }
    }
}
