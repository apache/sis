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
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import javax.imageio.stream.ImageOutputStream;

// Test dependencies
import org.junit.Test;
import org.apache.sis.test.DependsOn;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests {@link ChannelImageOutputStream}.
 *
 * <h4>Unresolved issue</h4>
 * {@link ChannelImageOutputStream} seems consistent with Image I/O standard implementation
 * for all methods tested in this class, except {@link ChannelImageOutputStream#readBit()}.
 * After that method call, the value of {@link ChannelImageOutputStream#getBitOffset()} is
 * in disagreement with Image I/O implementation. We have not yet identified the cause.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
@DependsOn(ChannelDataOutputTest.class)
public final class ChannelImageOutputStreamTest extends ChannelDataTestCase {
    /**
     * The implementation to test. This implementation will write data to {@link #testedStreamBackingArray}.
     * The content of that array will be compared to {@link #expectedData} for verifying result correctness.
     */
    private ChannelImageOutputStream testedStream;

    /**
     * A wrapper around {@link #testedStream}, used only because required by the tests in super-class.
     */
    private ChannelData testWrapper;

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
    public ChannelImageOutputStreamTest() {
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
        testedStream             = new ChannelImageOutputStream(testName, channel, ByteBuffer.allocate(bufferLength));
        testWrapper              = new ChannelData(testName, channel, testedStream.input().buffer) {
            @Override public Channel channel()        {return channel;}
            @Override public long getStreamPosition() {return testedStream.getStreamPosition();}
            @Override public void seek(long p)        {fail("Should not be invoked.");}
            @Override void flushNBytes(int n)         {fail("Should not be invoked.");}
        };
    }

    /**
     * Fills a stream with random data and compares the result with a reference output stream.
     * This method tests both read and write operations.
     *
     * @throws IOException should never happen since we read and write in memory only.
     */
    @Test
    public void testAllMethods() throws IOException {
        initialize("testAllMethods", STREAM_LENGTH, randomBufferCapacity());
        transferRandomData(testWrapper, testedStreamBackingArray.length - ARRAY_MAX_LENGTH, 22);
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
     * Writes a random unit of data using a method selected randomly.
     * This method is invoked (indirectly) by {@link #writeInStreams()}.
     *
     * @param  operation  numerical identifier of the operation to test.
     */
    @Override
    final void transferRandomData(final int operation) throws IOException {
        final ImageOutputStream        r = this.referenceStream;
        final ChannelImageOutputStream t = this.testedStream;
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
            case  8: {byte[]   v = randomBytes();   r.write       (v);              t.write       (v);              break;}
            case  9: {char[]   v = randomChars();   r.writeChars  (v, 0, v.length); t.writeChars  (v, 0, v.length); break;}
            case 10: {short[]  v = randomShorts();  r.writeShorts (v, 0, v.length); t.writeShorts (v, 0, v.length); break;}
            case 11: {int[]    v = randomInts();    r.writeInts   (v, 0, v.length); t.writeInts   (v, 0, v.length); break;}
            case 12: {long[]   v = randomLongs();   r.writeLongs  (v, 0, v.length); t.writeLongs  (v, 0, v.length); break;}
            case 13: {float[]  v = randomFloats();  r.writeFloats (v, 0, v.length); t.writeFloats (v, 0, v.length); break;}
            case 14: {double[] v = randomDoubles(); r.writeDoubles(v, 0, v.length); t.writeDoubles(v, 0, v.length); break;}
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
            case 20: {
                r.flush();
                t.flush();
                break;
            }
            /*
             * Seek operation, potentially followed by a few read operations.
             * The seek is necessary for moving to a position where there is something to read.
             */
            case 21: {
                long length = r.length();
                assertTrue(length >= 0, "length");
                assertEquals(length, t.length(), "length");
                long position = Math.max(r.getFlushedPosition(), t.getFlushedPosition());
                if (position < length) {
                    position += random.nextInt(Math.toIntExact(length - position));
                    r.seek(position);
                    t.seek(position);
                    length -= Double.BYTES;             // Make room for the largest element that we will read.
                    for (int i = random.nextInt(5); --i >= 0;) {
                        position = r.getStreamPosition();
                        assertEquals(position, t.getStreamPosition());
                        if (position >= length) break;
                        switch (random.nextInt(11)) {                   // TODO: should be 12. See class javadoc.
                            default: throw new AssertionError();
                            case  0: assertEquals(r.read(),              t.read(),               "read()");              break;
                            case  1: assertEquals(r.readBoolean(),       t.readBoolean(),        "readBoolean()");       break;
                            case  2: assertEquals(r.readChar(),          t.readChar(),           "readChar()");          break;
                            case  3: assertEquals(r.readByte(),          t.readByte(),           "readByte()");          break;
                            case  4: assertEquals(r.readShort(),         t.readShort(),          "readShort()");         break;
                            case  5: assertEquals(r.readUnsignedShort(), t.readUnsignedShort(),  "readUnsignedShort()"); break;
                            case  6: assertEquals(r.readInt(),           t.readInt(),            "readInt()");           break;
                            case  7: assertEquals(r.readUnsignedInt(),   t.readUnsignedInt(),    "readUnsignedInt()");   break;
                            case  8: assertEquals(r.readLong(),          t.readLong(),           "readLong()");          break;
                            case  9: assertEquals(r.readFloat(),         t.readFloat(),          "readFloat()");         break;
                            case 10: assertEquals(r.readDouble(),        t.readDouble(),         "readDouble()");        break;
                            case 11: assertEquals(r.readBit(),           t.readBit(),            "readBit()");           break;
                        }
                    }
                }
                break;
            }
        }
        assertEquals(r.length(),            t.length(),            "length()");
        assertEquals(r.getBitOffset(),      t.getBitOffset(),      "getBitOffset()");
        assertEquals(r.getStreamPosition(), t.getStreamPosition(), "getStreamPosition()");
    }
}
