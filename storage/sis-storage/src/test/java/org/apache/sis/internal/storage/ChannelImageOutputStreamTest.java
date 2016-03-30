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

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.InvalidMarkException;
import javax.imageio.stream.ImageOutputStream;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link ChannelImageOutputStream}.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@DependsOn(ChannelDataOutputTest.class)
public final strictfp class ChannelImageOutputStreamTest extends ChannelDataOutputTest {
    /**
     * Initializes all non-final fields before to execute a test.
     */
    @Override
    void initialize(final String fileName, final int streamLength, final int bufferLength) throws IOException {
        expectedData             = new ByteArrayOutputStream(streamLength);
        referenceStream          = new MemoryCacheImageOutputStream(expectedData);
        testedStreamBackingArray = new byte[streamLength];
        testedStream             = new ChannelImageOutputStream(fileName,
                new ByteArrayChannel(testedStreamBackingArray, false), ByteBuffer.allocate(bufferLength));
    }

    /**
     * Test writing a sequence of bits.
     *
     * @throws IOException Should never happen.
     */
    @Test
    public void testWriteBits() throws IOException {
        initialize("testWriteBits", STREAM_LENGTH, random.nextInt(BUFFER_MAX_CAPACITY) + (Long.SIZE / Byte.SIZE));
        final ImageOutputStream referenceStream = (ImageOutputStream) this.referenceStream;
        final int length = testedStreamBackingArray.length - ARRAY_MAX_LENGTH; // Keep a margin against buffer underflow.
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
            assertEquals("getBitOffset", referenceStream.getBitOffset(), testedStream.getBitOffset());
            assertEquals("getStreamPosition", referenceStream.getStreamPosition(), testedStream.getStreamPosition());
        }
        assertStreamContentEquals();
    }

    /**
     * Tests {@link ChannelImageOutputStream#mark()} and {@code reset()} methods.
     *
     * @throws IOException Should never happen.
     */
    @Test
    public void testMarkAndReset() throws IOException {
        initialize("testMarkAndReset", STREAM_LENGTH, STREAM_LENGTH); // We need a larger buffer for this test.
        final ImageOutputStream referenceStream = (ImageOutputStream) this.referenceStream;
        /*
         * Fill both streams with random data.
         * During this process, randomly takes mark.
         */
        int nbMarks = 0;
        for (int i=0; i<STREAM_LENGTH; i++) {
            if (randomEvent() && i < STREAM_LENGTH - (Long.SIZE / Byte.SIZE)) {
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
        final ImageOutputStream referenceStream = (ImageOutputStream) this.referenceStream;
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
        try {
            testedStream.reset();
            fail("Expected no remaining marks.");
        } catch (InvalidMarkException e) {
            // This is the expected exception.
        }
        assertStreamContentEquals();
    }

    /**
     * Tests {@link ChannelImageOutputStream#flushBefore(long)}.
     *
     * @throws IOException Should never happen.
     */
    @Test
    @DependsOnMethod("testMarkAndReset")
    public void testFlushBefore() throws IOException {
        final int N = 50; // Number of long values to write.
        initialize("testFlushBefore", N * (Long.SIZE / Byte.SIZE), 200);
        final ImageOutputStream referenceStream = (ImageOutputStream) this.referenceStream;
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
                    referenceStream.flushBefore(5 * (Long.SIZE / Byte.SIZE));
                    testedStream.flushBefore(5 * (Long.SIZE / Byte.SIZE));
                    break;
                }
                case 35: {
                    referenceStream.flushBefore(32 * (Long.SIZE / Byte.SIZE));
                    testedStream.flushBefore(32 * (Long.SIZE / Byte.SIZE));
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
