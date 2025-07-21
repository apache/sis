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
package org.apache.sis.geometries.index;

import org.apache.sis.util.ArgumentChecks;


/**
 * Morton iterator utilities.
 *
 * Resources :
 * - https://en.wikipedia.org/wiki/Z-order_curve
 * - https://github.com/CesiumGS/3d-tiles/tree/main/extensions/3DTILES_implicit_tiling#appendix-a-availability-indexing
 * - https://stackoverflow.com/questions/58979713/interleave-2-32-bit-integers-into-64-integer
 * - https://stackoverflow.com/questions/1024754/how-to-compute-a-3d-morton-number-interleave-the-bits-of-3-ints
 *
 * @author Johann Sorel (Geomatys)
 */
public final class MortonIterator {

    private MortonIterator(){}

    public static long index(long x, long y) {
        return space1bit(x) | (space1bit(y) << 1);
    }

    public static long index(long x, long y, long z) {
        return space2bit(x) | (space2bit(y) << 1) | (space2bit(z) << 2);
    }

    /**
     * Separate bits of given number, a 0 bit is inserted between each bits.
     * @param x must be inferior to 0xFFFFFFFF;
     * @return spaced long
     */
    public static long space1bit(long x) {
        ArgumentChecks.ensureBetween("x", 0,                      0b11111111_11111111_11111111_11111111L, x);
        long s = x          & 0b00000000_00000000_00000000_00000000_11111111_11111111_11111111_11111111L;    //0x00000000FFFFFFFF
        s = (s | (s << 16)) & 0b00000000_00000000_11111111_11111111_00000000_00000000_11111111_11111111L;    //0x0000FFFF0000FFFF
        s = (s | (s <<  8)) & 0b00000000_11111111_00000000_11111111_00000000_11111111_00000000_11111111L;    //0x00FF00FF00FF00FF
        s = (s | (s <<  4)) & 0b00001111_00001111_00001111_00001111_00001111_00001111_00001111_00001111L;    //0x0F0F0F0F0F0F0F0F
        s = (s | (s <<  2)) & 0b00110011_00110011_00110011_00110011_00110011_00110011_00110011_00110011L;    //0x3333333333333333
        s = (s | (s <<  1)) & 0b01010101_01010101_01010101_01010101_01010101_01010101_01010101_01010101L;    //0x5555555555555555
        return s;
    }

    /**
     * Separate bits of given number, two 0 bit are inserted between each bits.
     * @param x must be inferior to 1FFFFF;
     * @return spaced long
     */
    public static long space2bit(long x) {
        ArgumentChecks.ensureBetween("x", 0,                               0b00011111_11111111_11111111L, x);
        long s = x          & 0b00000000_00000000_00000000_00000000_00000000_00011111_11111111_11111111L;    //0x00000000001FFFFF
        s = (s | (s << 32)) & 0b00000000_00011111_00000000_00000000_00000000_00000000_11111111_11111111L;    //0X001F00000000FFFF
        s = (s | (s << 16)) & 0b00000000_00011111_00000000_00000000_11111111_00000000_00000000_11111111L;    //0X001F0000FF0000FF
        s = (s | (s <<  8)) & 0b00010000_00001111_00000000_11110000_00001111_00000000_11110000_00001111L;    //0X100F00F00F00F00F
        s = (s | (s <<  4)) & 0b00010000_11000011_00001100_00110000_11000011_00001100_00110000_11000011L;    //0X10C30C30C30C30C3
        s = (s | (s <<  2)) & 0b00010010_01001001_00100100_10010010_01001001_00100100_10010010_01001001L;    //0x1249249249249249
        return s;
    }
}
