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

import java.util.Arrays;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import javax.imageio.stream.ImageOutputStream;

// Test dependencies
import org.junit.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.DependsOnMethod;
import static org.apache.sis.test.Assertions.assertMessageContains;


/**
 * Tests {@link ChannelDataOutput}.
 * First we write into two different output streams, then we compare theirs written byte array.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ChannelDataOutputTest extends ChannelDataTestCase {
    /**
     * The implementation to test. This implementation will write data to {@link #testedStreamBackingArray}.
     * The content of that array will be compared to {@link #expectedData} for verifying result correctness.
     */
    private ChannelDataOutput testedStream;

    /**
     * A stream to use as a reference implementation. Any data written in {@link #testedStream}
     * will also be written in {@code referenceStream}, for later comparison.
     */
    private ImageOutputStream referenceStream;

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
     * Creates a new test case.
     */
    public ChannelDataOutputTest() {
    }

    /**
     * Initializes all non-final fields before to execute a test.
     *
     * @param  testName      the name of the test method to be executed.
     * @param  streamLength  length of stream to create.
     * @param  bufferLength  length of the {@code ByteBuffer} to use for the tests.
     * @throws IOException should never happen since we read and write in memory only.
     */
    private void initialize(final String testName, final int streamLength, final int bufferLength) throws IOException {
        expectedData             = new ByteArrayOutputStream(streamLength);
        referenceStream          = new MemoryCacheImageOutputStream(expectedData);
        testedStreamBackingArray = new byte[streamLength];
        var channel              = new ByteArrayChannel(testedStreamBackingArray, false);
        testedStream             = new ChannelDataOutput(testName, channel, ByteBuffer.allocate(bufferLength));
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
     * @throws IOException should never happen since we read and write in memory only.
     */
    @Test
    public void testAllWriteMethods() throws IOException {
        initialize("testAllWriteMethods", STREAM_LENGTH, randomBufferCapacity());
        writeInStreams();
        assertStreamContentEquals();
    }

    /**
     * Asserts that the content of {@link #testedStream} is equal to the content of {@link #referenceStream}.
     * This method closes the reference stream before to perform the comparison.
     */
    private void assertStreamContentEquals() throws IOException {
        testedStream.flush();
        referenceStream.close();
        final byte[] expectedArray = expectedData.toByteArray();
        assertArrayEquals(expectedArray, Arrays.copyOf(testedStreamBackingArray, expectedArray.length));
    }

    /**
     * Tests write operations followed by seek operations.
     *
     * @throws IOException should never happen since we read and write in memory only.
     */
    @Test
    @DependsOnMethod("testAllWriteMethods")
    public void testWriteAndSeek() throws IOException {
        initialize("testWriteAndSeek", STREAM_LENGTH, randomBufferCapacity());
        writeInStreams();
        referenceStream.close();
        final byte[] expectedArray = expectedData.toByteArray();
        final int seekRange = expectedArray.length - Long.BYTES;
        final ByteBuffer arrayView = ByteBuffer.wrap(expectedArray);
        for (int i=0; i<100; i++) {
            final int position = random.nextInt(seekRange);
            testedStream.seek(position);
            assertEquals(position, testedStream.getStreamPosition(), "getStreamPosition()");
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
     * @throws IOException should never happen since we read and write in memory only.
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
        assertEquals(24, testedStream.getStreamPosition(), "getStreamPosition()");
        testedStream.seek(40);                          // Move 2 long ahead. Space shall be filled by 0.
        referenceStream.writeLong(0);
        referenceStream.writeLong(0);
        final long v = random.nextLong();
        referenceStream.writeLong(v);
        testedStream.writeLong(v);
        assertStreamContentEquals();
    }

    /**
     * Tests the argument checks performed by various methods. For example, this method
     * tests {@link ChannelDataOutput#seek(long)} with an invalid seek position.
     *
     * @throws IOException should never happen since we read and write in memory only.
     */
    @Test
    public void testArgumentChecks() throws IOException {
        initialize("testArgumentChecks", 20, 10);

        // Shall not accept invalid bitOffset.
        Exception exception;
        exception = assertThrows(IllegalArgumentException.class, () -> testedStream.setBitOffset(9));
        assertMessageContains(exception, "bitOffset");

        // Shall not accept reset without mark.
        exception = assertThrows(IOException.class, () -> testedStream.reset());
        assertMessageContains(exception);

        // Shall not flush at a position greater than buffer limit.
        final int v = random.nextInt();
        referenceStream.writeShort(v);
        testedStream.writeShort(v);
        testedStream.flushBefore(0);        // Valid.

        exception = assertThrows(IndexOutOfBoundsException.class, () -> testedStream.flushBefore(3));
        assertMessageContains(exception, "position");

        testedStream.flush();
        testedStream.flushBefore(0);        // Should be a no-operation.
        assertStreamContentEquals();
    }

    /**
     * Writes the same random data in both {@link #testedStream} and {@link #referenceStream}.
     *
     * @throws IOException should never happen since we read and write in memory only.
     */
    private void writeInStreams() throws IOException {
        transferRandomData(testedStream, testedStreamBackingArray.length - ARRAY_MAX_LENGTH, 20);
    }

    /**
     * Writes a random unit of data using a method selected randomly.
     * This method is invoked (indirectly) by {@link #writeInStreams()}.
     *
     * @param  operation  numerical identifier of the operation to test.
     */
    @Override
    final void transferRandomData(final int operation) throws IOException {
        final ImageOutputStream r = this.referenceStream;
        final ChannelDataOutput t = this.testedStream;
        switch (operation) {
            default: throw new AssertionError(operation);
            case  0: {byte     v = (byte)  random.nextInt();     r.writeByte   (v); t.writeByte   (v); break;}
            case  1: {short    v = (short) random.nextInt();     r.writeShort  (v); t.writeShort  (v); break;}
            case  2: {char     v = (char)  random.nextInt();     r.writeChar   (v); t.writeChar   (v); break;}
            case  3: {int      v =         random.nextInt();     r.writeInt    (v); t.writeInt    (v); break;}
            case  4: {long     v =         random.nextLong();    r.writeLong   (v); t.writeLong   (v); break;}
            case  5: {float    v =         random.nextFloat();   r.writeFloat  (v); t.writeFloat  (v); break;}
            case  6: {double   v =         random.nextDouble();  r.writeDouble (v); t.writeDouble (v); break;}
            case  7: {boolean  v =         random.nextBoolean(); r.writeBoolean(v); t.writeBoolean(v); break;}
            case  8: {byte[]   v = randomBytes();   r.write       (v);              t.write       (v); break;}
            case  9: {char[]   v = randomChars();   r.writeChars  (v, 0, v.length); t.writeChars  (v); break;}
            case 10: {short[]  v = randomShorts();  r.writeShorts (v, 0, v.length); t.writeShorts (v); break;}
            case 11: {int[]    v = randomInts();    r.writeInts   (v, 0, v.length); t.writeInts   (v); break;}
            case 12: {long[]   v = randomLongs();   r.writeLongs  (v, 0, v.length); t.writeLongs  (v); break;}
            case 13: {float[]  v = randomFloats();  r.writeFloats (v, 0, v.length); t.writeFloats (v); break;}
            case 14: {double[] v = randomDoubles(); r.writeDoubles(v, 0, v.length); t.writeDoubles(v); break;}
            case 15: {String   v = "Byte sequence";      r.writeBytes(v); t.writeBytes(v); break;}
            case 16: {String   v = "Character sequence"; r.writeChars(v); t.writeChars(v); break;}
            case 17: {String   v = "お元気ですか";       r.writeUTF  (v); t.writeUTF  (v); break;}
            case 18: {
                final long v = random.nextLong();
                final int numBits = random.nextInt(Byte.SIZE);
                r.writeBits(v, numBits);
                t.writeBits(v, numBits);
                break;
            }
            case 19: {
                long flushedPosition = StrictMath.max(r.getFlushedPosition(), t.getFlushedPosition());
                flushedPosition += random.nextInt(1 + (int) (r.getStreamPosition() - flushedPosition));
                r.flushBefore(flushedPosition);
                t.flushBefore(flushedPosition);
                break;
            }
            /*
             * Do not test flush, because `ChannelDataOutput.flush()` semantic
             * is very different than the `ImageOutputStream.flush()` semantic.
             */
        }
        assertEquals(r.getBitOffset(),      t.getBitOffset(),      "getBitOffset()");
        assertEquals(r.getStreamPosition(), t.getStreamPosition(), "getStreamPosition()");
    }

    /**
     * Test writing a sequence of bits.
     *
     * @throws IOException should never happen since we read and write in memory only.
     */
    @Test
    public void testWriteBits() throws IOException {
        initialize("testWriteBits", STREAM_LENGTH, randomBufferCapacity());
        final int length = testedStreamBackingArray.length - ARRAY_MAX_LENGTH;      // Keep a margin against buffer underflow.
        while (testedStream.getStreamPosition() < length) {
            final long v = random.nextLong();
            final int numBits = random.nextInt(Byte.SIZE);
            referenceStream.writeBits(v, numBits);
            testedStream.writeBits(v, numBits);
            /*
             * Randomly force flushing of bits.
             */
            if (randomEvent()) {
                final int f = random.nextInt(256);
                referenceStream.writeByte(f);
                testedStream.writeByte(f);
            }
            assertEquals(referenceStream.getBitOffset(),      testedStream.getBitOffset(),      "getBitOffset");
            assertEquals(referenceStream.getStreamPosition(), testedStream.getStreamPosition(), "getStreamPosition");
        }
        assertStreamContentEquals();
    }

    /**
     * Tests {@link ChannelImageOutputStream#mark()} and {@code reset()} methods.
     *
     * @throws IOException should never happen since we read and write in memory only.
     */
    @Test
    public void testMarkAndReset() throws IOException {
        initialize("testMarkAndReset", STREAM_LENGTH, 1000);        // We need a larger buffer for this test.
        /*
         * Fill both streams with random data.
         * During this process, randomly takes mark.
         */
        int nbMarks = 0;
        for (int i=0; i<STREAM_LENGTH; i++) {
            if (randomEvent() && i < STREAM_LENGTH - Long.BYTES) {
                referenceStream.mark();
                testedStream.mark();
                nbMarks++;
            }
            final int v = random.nextInt(256);
            referenceStream.writeByte(v);
            testedStream.writeByte(v);
        }
        compareMarks(nbMarks);
    }

    /**
     * Invokes {@link ChannelImageOutputStream#reset()} {@code nbMarks} times and verify that the stream position
     * is the expected one. This method will then write random values at those positions, and finally compare the
     * stream content.
     */
    private void compareMarks(int nbMarks) throws IOException {
        while (--nbMarks >= 0) {
            referenceStream.reset();
            testedStream.reset();
            assertEquals(referenceStream.getBitOffset(),      testedStream.getBitOffset());
            assertEquals(referenceStream.getStreamPosition(), testedStream.getStreamPosition());
            final long v = random.nextLong();
            referenceStream.writeLong(v);
            testedStream.writeLong(v);
        }
        /*
         * Verify that we have no remaining marks, and finally compare stream content.
         */
        var exception = assertThrows(IOException.class, () -> testedStream.reset());
        assertMessageContains(exception);
        assertStreamContentEquals();
    }

    /**
     * Tests {@link ChannelImageOutputStream#flushBefore(long)}.
     *
     * @throws IOException should never happen since we read and write in memory only.
     */
    @Test
    @DependsOnMethod("testMarkAndReset")
    public void testFlushBefore() throws IOException {
        final int N = 50; // Number of long values to write.
        initialize("testFlushBefore", N*Long.BYTES, 200);
        for (int i=0; i<N; i++) {
            switch (i) {
                case 20:
                case 30:
                case 40:
                case 45: {
                    referenceStream.mark();
                    testedStream.mark();
                    break;
                }
                case 10: {
                    referenceStream.flushBefore(5 * Long.BYTES);
                    testedStream.flushBefore(5 * Long.BYTES);
                    break;
                }
                case 35: {
                    referenceStream.flushBefore(32 * Long.BYTES);
                    testedStream.flushBefore(32 * Long.BYTES);
                    break;
                }
            }
            final long v = random.nextLong();
            referenceStream.writeLong(v);
            testedStream.writeLong(v);
        }
        compareMarks(2);
    }
}
