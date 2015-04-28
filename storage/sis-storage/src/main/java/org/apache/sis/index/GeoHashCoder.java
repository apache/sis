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
package org.apache.sis.index;

import java.io.Serializable;
import java.text.ParseException;
import org.opengis.geometry.DirectPosition;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;


/**
 * Encodes geographic coordinates as <cite>geohashes</cite> strings, and decode such strings back to coordinates.
 * The current implementation is restricted to two-dimensional geographic coordinates, in the (<var>longitude</var>,
 * <var>latitude</var>) order with the longitudes ranging from -180° to 180° and the latitudes ranging from -90° to
 * 90°. The datum is unspecified.
 *
 * @author  Chris Mattmann (JPL)
 * @since   0.1
 * @version 0.3
 * @module
 *
 * @see <a href="http://en.wikipedia.org/wiki/Geohash">Wikipedia: Geohash</a>
 */
public class GeoHashCoder implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 9162259764027168776L;

    /**
     * The encoding format used by {@link GeoHashCoder}.
     */
    public static enum Format {
        /**
         * Format consisting of 32 symbols used at {@code http://geohash.org}. This encoding uses digits 0 to 9,
         * and lower-case letters {@code 'b'} to {@code 'z'} excluding {@code 'i'}, {@code 'l'} and {@code 'o'}.
         * Decoding is case-insensitive.
         */
        BASE32(16, new byte[] {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'b', 'c', 'd', 'e', 'f', 'g', 'h', 'j', 'k', 'm',
            'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
        });

        /**
         * A single one-bit in the position of the highest-order ("leftmost") one-bit of the value
         * represented by a letter or digit. This value can be computed as {@code 1 << (numBits-1)}
         * where {@code numBits} is the number of bits needed for representing a letter or digit.
         */
        final int highestOneBit;

        /**
         * Mapping from a numerical value to its symbol. The length of this array is the base of the encoding,
         * e.g. 32 for {@link #BASE32}.
         */
        final byte[] encoding;

        /**
         * Mapping from a lower-case letter symbols to its numerical value.
         */
        final byte[] decodingLowerCase;

        /**
         * Mapping from a upper-case letter symbols to its numerical value.
         * This is the same array than {@link #decodingLowerCase} if the format is case-insensitive.
         */
        final byte[] decodingUpperCase;

        /**
         * Creates a new format for the given {@coe encoding} mapping.
         * This constructor computes the {@code decoding} arrays from the {@code encoding} one.
         *
         * @param highestOneBit The leftmost one-bit of the value represented by a letter or digit.
         * @param encoding The mapping from numerical values to symbols.
         */
        private Format(final int highestOneBit, final byte[] encoding) {
            this.highestOneBit = highestOneBit;
            this.encoding = encoding;
            final byte[] decoding = new byte[26];
            for (byte i=10; i<encoding.length; i++) {
                decoding[encoding[i] - 'a'] = i;
            }
            decodingLowerCase = decoding;
            decodingUpperCase = decoding;
            // Current version create a case-insensitive format.
            // However if we implement BASE36 in a future version,
            // then the two 'decoding' arrays will differ.
        }
    }

    /**
     * The format used by the {@code GeoHashCoder}.
     */
    private Format format;

    /**
     * Amount of letters or digits to format in the geohash.
     * Stored as a {@code byte} on the assumption that attempts to create
     * geohashes of more then 255 characters is likely to be an error anyway.
     */
    private byte precision;

    /**
     * A buffer of length {@link #precision}, created when first needed.
     */
    private transient char[] buffer;

    /**
     * Creates a new geohash coder/decoder initialized to the default precision for {@link Format#BASE32}.
     */
    public GeoHashCoder() {
        format = Format.BASE32;
        precision = 12;
    }

    /**
     * Returns the current encoding/decoding format.
     * The default value is {@link Format#BASE32}.
     *
     * @return The current format.
     */
    public Format getFormat() {
        return format;
    }

    /**
     * Sets the encoding/decoding format.
     *
     * @param format The new format.
     */
    public void setFormat(final Format format) {
        ArgumentChecks.ensureNonNull("format", format);
        this.format = format;
    }

    /**
     * Returns the length of geohashes strings to be encoded by the {@link #encode(DirectPosition)} method.
     * The default value is 12.
     *
     * @return The length of geohashes strings.
     */
    public int getPrecision() {
        return precision;
    }

    /**
     * Sets the length of geohashes strings to be encoded by the {@link #encode(DirectPosition)} method.
     *
     * @param precision The new length of geohashes strings.
     */
    public void setPrecision(final int precision) {
        ArgumentChecks.ensureBetween("precision", 1, 255, precision);
        this.precision = (byte) precision;
        buffer = null; // Will recreate a new buffer when first needed.
    }

    /**
     * Encodes the given longitude and latitude into a geohash.
     *
     * @param longitude Longitude to encode, as decimal degrees in the [-180 … 180]° range.
     * @param latitude Latitude to encode, as decimal degrees in the [-90 … 90]° range.
     * @return Geohash encoding of the given longitude and latitude.
     */
    public String encode(final double longitude, final double latitude) {
        final byte[] encoding = format.encoding;
        final int highestOneBit = format.highestOneBit;
        char[] geohash = buffer;
        if (geohash == null) {
            buffer = geohash = new char[precision & 0xFF];
        }
        /*
         * The current implementation assumes a two-dimensional coordinates. The 'isEven' boolean takes
         * the 'true' value for longitude, and 'false' for latitude. We could extend this algorithm to
         * the multi-dimensional case by replacing 'isEven' by a counter over the ordinate dimension.
         */
        boolean isEven = true;
        double xmin = -180, ymin = -90;
        double xmax =  180, ymax =  90;
        /*
         * 'ch' is the index of the character to be added in the geohash. The actual character will be
         * given by the 'encoding' array. 'bit' shall have a single one-bit, rotating from 10000 in the
         * BASE32 case to 00001 (binary representation).
         */
        int ch = 0;
        int bit = highestOneBit;
        for (int i=0; i<geohash.length;) {
            if (isEven) {
                final double mid = (xmin + xmax) / 2;
                if (longitude > mid) {
                    ch |= bit;
                    xmin = mid;
                } else {
                    xmax = mid;
                }
            } else {
                final double mid = (ymin + ymax) / 2;
                if (latitude > mid) {
                    ch |= bit;
                    ymin = mid;
                } else {
                    ymax = mid;
                }
            }

            isEven = !isEven;

            bit >>>= 1;
            if (bit == 0) {
                geohash[i++] = (char) encoding[ch];
                bit = highestOneBit;
                ch = 0;
            }
        }
        return new String(geohash);
    }

    /**
     * Encodes the given position into a geohash. The current implementation takes the first ordinate value as the
     * longitude, the second ordinate value as the latitude, then delegates to {@link #encode(double, double)}.
     *
     * <p>The current implementation does not verify the Coordinate Reference System of the given position.
     * However this may change in future SIS versions.</p>
     *
     * @param  position The coordinate to encode.
     * @return Geohash encoding of the given position.
     */
    public String encode(final DirectPosition position) {
        ArgumentChecks.ensureDimensionMatches("position", 2, position);
        return encode(position.getOrdinate(0), position.getOrdinate(1));
    }

    /**
     * Decodes the given geohash into a longitude and a latitude.
     *
     * @param geohash Geohash string to decode.
     * @return A new position with the longitude at ordinate 0 and latitude at ordinate 1.
     * @throws ParseException If an error occurred while parsing the given string.
     */
    public DirectPosition decode(final String geohash) throws ParseException {
        final int length = geohash.length();
        final int highestOneBit = format.highestOneBit;
        final byte[] decodingLowerCase = format.decodingLowerCase;
        final byte[] decodingUpperCase = format.decodingUpperCase;
        /*
         * The current implementation assumes a two-dimensional coordinates. The 'isEven' boolean takes
         * the 'true' value for longitude, and 'false' for latitude. We could extend this algorithm to
         * the multi-dimensional case by replacing 'isEven' by a counter over the ordinate dimension.
         */
        boolean isEven = true;
        double xmin = -180, ymin = -90;
        double xmax =  180, ymax =  90;

        int nc; // Number of characters for the 'c' code point.
        for (int i=0; i<length; i+=nc) {
            int c = geohash.codePointAt(i);
            nc = Character.charCount(c);
            if (c >= '0' && c <= '9') {
                c -= '0';
            } else {
                if (c >= 'a' && c <= 'z') {
                    c = decodingLowerCase[c - 'a'];
                } else if (c >= 'A' && c <= 'Z') {
                    c = decodingUpperCase[c - 'A'];
                } else {
                    c = 0;
                }
                if (c == 0) {
                    throw new ParseException(Errors.format(Errors.Keys.UnparsableStringForClass_3,
                            "GeoHash",geohash, geohash.substring(i, i+nc)), i);
                }
            }
            int mask = highestOneBit;
            do {
                if (isEven) {
                    final double mid = (xmin + xmax) / 2;
                    if ((c & mask) != 0) {
                        xmin = mid;
                    } else {
                        xmax = mid;
                    }
                } else {
                    final double mid = (ymin + ymax) / 2;
                    if ((c & mask) != 0) {
                        ymin = mid;
                    } else {
                        ymax = mid;
                    }
                }
                isEven = !isEven;
            } while ((mask >>>= 1) != 0);
        }
        return new DirectPosition2D((xmin + xmax) / 2,
                                    (ymin + ymax) / 2);
    }
}
