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
package org.apache.sis.pending.jdk;

import org.apache.sis.util.resources.Errors;


/**
 * Place holder for a functionality defined only in JDK17.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class HexFormat {
    private static final HexFormat INSTANCE = new HexFormat();

    private HexFormat() {
    }

    /**
     * Returns the singleton instance.
     */
    public static HexFormat of() {
        return INSTANCE;
    }

    /**
     * Returns the byte array parsed from the given hexadecimal string.
     *
     * @param  string  the hexadecimal string.
     * @return the parsed bytes.
     * @throws NumberFormatException if a character is not a hexadecimal digit.
     */
    public byte[] parseHex(final CharSequence string) {
        final int length = string.length();
        if ((length & 1) != 0) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.OddArrayLength_1, "wkb"));
        }
        final byte[] data = new byte[length >>> 1];
        for (int i=0; i<length;) {
            data[i >>> 1] = (byte) ((fromHexDigit(string.charAt(i++)) << 4) | fromHexDigit(string.charAt(i++)));
        }
        return data;
    }

    /**
     * Returns the numerical value of the given hexadecimal digit.
     * The hexadecimal digit can be the decimal digits 0 to 9, or the letters A to F ignoring case.
     *
     * <h4>Implementation note</h4>
     * We do not use {@link Character#digit(char, int)} because that method handled a large
     * range of Unicode characters, which is a wider scope than what is intended here.
     *
     * @param  c  the hexadecimal digit.
     * @return numerical value of the given hexadecimal digit.
     * @throws NumberFormatException if the given character is not a hexadecimal digit.
     */
    public static int fromHexDigit(final int c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'A' && c <= 'F') return c - ('A' - 10);
        if (c >= 'a' && c <= 'f') return c - ('a' - 10);
        throw new NumberFormatException(Errors.format(Errors.Keys.CanNotParse_1, String.valueOf(c)));
    }
}
