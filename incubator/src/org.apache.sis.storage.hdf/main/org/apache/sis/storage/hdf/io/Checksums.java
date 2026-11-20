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
package org.apache.sis.storage.hdf.io;

import org.apache.sis.storage.hdf.internal.Constants;
import java.util.logging.Level;

/**
 * Jenkins lookup3 hash, used by the HDF5 format for the checksum stored at the
 * end of every version 2 structure (object headers, version 2 B-tree nodes,
 * fractal heap blocks, fixed and extensible array blocks) and for the name hash
 * of some version 2 B-tree records.
 * <p>
 * This is a transcription of the public domain {@code hashlittle} function by
 * Bob Jenkins (lookup3.c, May 2006), byte oriented variant, which is the one
 * used by {@code H5_checksum_lookup3}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Checksums {

    /**
     * Whether a mismatching checksum causes a failure.
     * <p>
     * Verification is disabled by default.
     */
    private static final boolean VERIFY = Boolean.getBoolean("org.apache.sis.storage.hdf.verifyChecksum");

    private Checksums() {}

    /**
     * Compute the lookup3 checksum of the given bytes, as
     * {@code H5_checksum_lookup3(data, data.length, 0)} would.
     *
     * @param datas bytes to hash, not null
     * @return the 32 bits checksum
     */
    public static int checksum(byte[] datas) {
        return hashLittle(datas, 0, datas.length, 0);
    }

    /**
     * Compute the lookup3 checksum of a range of bytes.
     *
     * @param datas bytes to hash, not null
     * @param offset index of the first byte to hash
     * @param length number of bytes to hash
     * @param initialValue previous hash or an arbitrary seed
     * @return the 32 bits checksum
     */
    public static int hashLittle(byte[] datas, int offset, int length, int initialValue) {
        int a, b, c;
        a = b = c = 0xdeadbeef + length + initialValue;

        int k = offset;
        int remaining = length;

        while (remaining > 12) {
            a += (datas[k    ] & 0xFF);
            a += (datas[k + 1] & 0xFF) << 8;
            a += (datas[k + 2] & 0xFF) << 16;
            a += (datas[k + 3] & 0xFF) << 24;
            b += (datas[k + 4] & 0xFF);
            b += (datas[k + 5] & 0xFF) << 8;
            b += (datas[k + 6] & 0xFF) << 16;
            b += (datas[k + 7] & 0xFF) << 24;
            c += (datas[k + 8] & 0xFF);
            c += (datas[k + 9] & 0xFF) << 8;
            c += (datas[k + 10] & 0xFF) << 16;
            c += (datas[k + 11] & 0xFF) << 24;

            // mix(a,b,c)
            a -= c;  a ^= Integer.rotateLeft(c,  4);  c += b;
            b -= a;  b ^= Integer.rotateLeft(a,  6);  a += c;
            c -= b;  c ^= Integer.rotateLeft(b,  8);  b += a;
            a -= c;  a ^= Integer.rotateLeft(c, 16);  c += b;
            b -= a;  b ^= Integer.rotateLeft(a, 19);  a += c;
            c -= b;  c ^= Integer.rotateLeft(b,  4);  b += a;

            remaining -= 12;
            k += 12;
        }

        /*
        Last block, affecting all 32 bits of c.
        The fall through is intentional and is part of the original algorithm.
        */
        switch (remaining) {
            case 12: c += (datas[k + 11] & 0xFF) << 24;
            case 11: c += (datas[k + 10] & 0xFF) << 16;
            case 10: c += (datas[k +  9] & 0xFF) << 8;
            case  9: c += (datas[k +  8] & 0xFF);
            case  8: b += (datas[k +  7] & 0xFF) << 24;
            case  7: b += (datas[k +  6] & 0xFF) << 16;
            case  6: b += (datas[k +  5] & 0xFF) << 8;
            case  5: b += (datas[k +  4] & 0xFF);
            case  4: a += (datas[k +  3] & 0xFF) << 24;
            case  3: a += (datas[k +  2] & 0xFF) << 16;
            case  2: a += (datas[k +  1] & 0xFF) << 8;
            case  1: a += (datas[k     ] & 0xFF);
                break;
            case  0:
                return c;
            default:
                throw new IllegalStateException("Unreachable, remaining = " + remaining);
        }

        // final(a,b,c)
        c ^= b;  c -= Integer.rotateLeft(b, 14);
        a ^= c;  a -= Integer.rotateLeft(c, 11);
        b ^= a;  b -= Integer.rotateLeft(a, 25);
        c ^= b;  c -= Integer.rotateLeft(b, 16);
        a ^= c;  a -= Integer.rotateLeft(c,  4);
        b ^= a;  b -= Integer.rotateLeft(a, 14);
        c ^= b;  c -= Integer.rotateLeft(b, 24);
        return c;
    }

    /**
     * Compare a checksum read from a file against the expected one.
     * <p>
     * A mismatch is logged, and only raises an error when checksum verification
     * has been explicitly enabled.
     *
     * @param structureName name of the structure being read, for the message
     * @param expected checksum computed from the structure bytes
     * @param found checksum read from the file
     * @throws java.io.IOException if verification is enabled and the checksums differ
     */
    public static void verify(String structureName, int expected, int found) throws java.io.IOException {
        if (expected == found) return;
        final String message = "Checksum mismatch in " + structureName
                + ", expected 0x" + Integer.toHexString(expected)
                + " but found 0x" + Integer.toHexString(found);
        if (VERIFY) {
            throw new java.io.IOException(message);
        }
        Constants.LOGGER.log(Level.FINE, message);
    }
}
