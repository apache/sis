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
import java.util.Random;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.io.IOException;
import org.apache.sis.util.Numbers;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link HyperRectangleReader}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn(ChannelDataInputTest.class)
public final strictfp class HyperRectangleReaderTest extends TestCase {
    /**
     * The hyper-cube dimensions.
     */
    private final long[] size = new long[4];

    /**
     * Lower values of the sub-region to test.
     */
    private final long[] lower = new long[size.length];

    /**
     * Upper values of the sub-region to test.
     */
    private final long[] upper = new long[size.length];

    /**
     * Sub-sampling values to use for the test.
     */
    private final int[] subsampling = new int[size.length];

    /**
     * The reader to test for an hyper-cube of {@code short} values, created by {@link #initialize(Random)}.
     * Sample values are index values encoded in base 10. For example the value at index (4,1,2,3) will be 4123.
     */
    private HyperRectangleReader reader;

    /**
     * Encodes the given index in the sample values to be stored in the array of data.
     * We use a decimal encoding for making easier to compare the actual values with the expected ones.
     */
    private static long sampleValue(final long i0, final long i1, final long i2, final long i3) {
        return i3*1000 + i2*100 + i1*10 + i0;
    }

    /**
     * Creates an hyper-rectangle of random size and initializes the sub-region and sub-sampling to random values.
     * Sample values are index values encoded in base 10. For example the value at index (4,1,2,3) will be 4123.
     *
     * @param random The random number generator to use for initializing the test.
     * @param useChannel {@code true} for fetching the data from channel to a small buffer,
     *        or {@code false} if the data are expected to be fully contained in the buffer.
     */
    private void initialize(final Random random, final boolean useChannel) throws IOException, DataStoreException {
        /*
         * Compute a random hyper-rectangle size, sub-region and sub-sampling. Each dimension will have a
         * size between 1 to 10, so we will be able to use decimal digits from 0 to 9 in the sample values.
         */
        int length = 1;
        for (int i=0; i<size.length; i++) {
            final int s = random.nextInt(9) + 1;
            int low = random.nextInt(s);
            int up  = random.nextInt(s);
            if (low > up) {
                final int t = low;
                low = up; up = t;
            }
            size [i] = s;
            lower[i] = low;
            upper[i] = up + 1;
            subsampling[i] = random.nextInt(3) + 1;
            length *= s;
        }
        /*
         * Prepare an array of bytes which will contain the short values using native byte order.
         * Put small amout of random value at the array beginning in order to test with an origin
         * different than zero.
         */
        final int origin = random.nextInt(10);
        final byte[] array = new byte[origin + length*(Short.SIZE / Byte.SIZE)];
        for (int i=0; i<origin; i++) {
            array[i] = (byte) random.nextInt(0x100);
        }
        /*
         * Fill the array with short values using the encoding describes in javadoc.
         * Then wrap the array in a pseudo-channel so we can create the reader to test.
         */
        final ShortBuffer view = ByteBuffer.wrap(array, origin, length*(Short.SIZE / Byte.SIZE)).order(ByteOrder.nativeOrder()).asShortBuffer();
        for (int i3=0; i3<size[3]; i3++) {
            for (int i2=0; i2<size[2]; i2++) {
                for (int i1=0; i1<size[1]; i1++) {
                    for (int i0=0; i0<size[0]; i0++) {
                        view.put((short) sampleValue(i0, i1, i2, i3));
                    }
                }
            }
        }
        assertEquals(length, view.position());
        if (useChannel) {
            final ByteArrayChannel channel = new ByteArrayChannel(array, true);
            final ByteBuffer       buffer  = ByteBuffer.allocate(random.nextInt(20) + 20).order(ByteOrder.nativeOrder());
            final ChannelDataInput input   = new ChannelDataInput("HyperRectangle in channel", channel, buffer, false);
            reader = new HyperRectangleReader(Numbers.SHORT, input, origin);
        } else {
            view.clear();
            reader = new HyperRectangleReader("HyperRectangle in buffer", view);
        }
    }

    /**
     * Extracts data from a region defined by current {@link #lower}, {@link #upper} and {@link #subsampling} values,
     * then compares against the expected values.
     */
    private void verifyRegionRead() throws IOException {
        final short[] data = (short[]) reader.read(new Region(size, lower, upper, subsampling));
        int p = 0;
        final int s3 = subsampling[3];
        final int s2 = subsampling[2];
        final int s1 = subsampling[1];
        final int s0 = subsampling[0];
        for (long i3=lower[3]; i3<upper[3]; i3 += s3) {
            for (long i2=lower[2]; i2<upper[2]; i2 += s2) {
                for (long i1=lower[1]; i1<upper[1]; i1 += s1) {
                    for (long i0=lower[0]; i0<upper[0]; i0 += s0) {
                        assertEquals("Sample value", sampleValue(i0, i1, i2, i3), data[p++]);
                    }
                }
            }
        }
        assertEquals("Array length", p, data.length);
    }

    /**
     * Tests reading a random part of the hyper-cube without sub-sampling.
     *
     * @throws IOException should never happen.
     * @throws DataStoreException should never happen.
     */
    @Test
    public void testSubRegion() throws IOException, DataStoreException {
        initialize(TestUtilities.createRandomNumberGenerator(), true);
        Arrays.fill(subsampling, 0, subsampling.length, 1);
        verifyRegionRead();
    }

    /**
     * Tests reading the full hyper-cube with a random sub-sampling.
     *
     * @throws IOException should never happen.
     * @throws DataStoreException should never happen.
     */
    @Test
    public void testSubSampling() throws IOException, DataStoreException {
        initialize(TestUtilities.createRandomNumberGenerator(), true);
        System.arraycopy(size, 0, upper, 0, size.length);
        Arrays.fill(lower, 0, lower.length, 0);
        verifyRegionRead();
    }

    /**
     * Tests reading a random part of the hyper-cube with a random sub-sampling.
     *
     * @throws IOException should never happen.
     * @throws DataStoreException should never happen.
     */
    @Test
    @DependsOnMethod({"testSubRegion", "testSubSampling"})
    public void testRandom() throws IOException, DataStoreException {
        initialize(TestUtilities.createRandomNumberGenerator(), true);
        verifyRegionRead();
    }

    /**
     * Tests reading data from an existing buffer, without channel.
     *
     * @throws IOException should never happen.
     * @throws DataStoreException should never happen.
     */
    @Test
    @DependsOnMethod("testRandom")
    public void testMemoryTransfer() throws IOException, DataStoreException {
        initialize(TestUtilities.createRandomNumberGenerator(), false);
        verifyRegionRead();
    }
}
