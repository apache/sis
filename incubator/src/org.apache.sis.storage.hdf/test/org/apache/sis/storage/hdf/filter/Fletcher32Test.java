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
package org.apache.sis.storage.hdf.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 * Tests of the Fletcher32 checksum filter.
 * <p>
 * The expected values come from an implementation of the reference algorithm
 * validated against the checksums the HDF5 library writes in a filtered file.
 *
 * @author Johann Sorel (Geomatys)
 */
public class Fletcher32Test {

    @Test
    public void testChecksum() {
        assertEquals(0, Fletcher32.checksum(new byte[0], 0));
        assertEquals(640271558, Fletcher32.checksum(
                "abcd".getBytes(StandardCharsets.US_ASCII), 4));
        assertEquals(-858070013, Fletcher32.checksum(sequence(64), 64));
    }

    /**
     * A trailing odd byte counts as the high half of a word.
     */
    @Test
    public void testOddLength() {
        assertEquals(134809092, Fletcher32.checksum(sequence(5), 5));
    }

    /**
     * More than 360 words, which is where the accumulators are reduced.
     */
    @Test
    public void testBeyondReductionBlock() {
        assertEquals(224956803, Fletcher32.checksum(sequence(1000), 1000));
    }

    /**
     * Decoding strips the four checksum bytes and returns the data untouched.
     */
    @Test
    public void testDecodeRoundTrip() throws IOException {
        final Fletcher32 filter = new Fletcher32();
        final byte[] datas = sequence(64);
        final byte[] encoded = filter.encode(datas);
        assertEquals(datas.length + 4, encoded.length);
        assertArrayEquals(datas, filter.decode(encoded));
    }

    private static byte[] sequence(int length) {
        final byte[] datas = new byte[length];
        for (int i = 0; i < length; i++) {
            datas[i] = (byte) i;
        }
        return datas;
    }
}
