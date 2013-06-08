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

import java.text.ParseException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;


/**
 * Utilities for encoding and decoding geohashes.
 *
 * @author  Chris Mattmann (JPL)
 * @since   0.1
 * @version 0.3
 * @module
 *
 * @see <a href="http://en.wikipedia.org/wiki/Geohash">Wikipedia: Geohash</a>
 */
public class GeoHashCoder {
    /**
     * The encoding format used by {@link GeoHashCoder}.
     */
    public static enum Format {
        /**
         * Format consisting of 32 symbols used at {@code http://geohash.org}. This encoding uses digits 0 to 9,
         * and lower-case letters {@code 'b'} to {@code 'z'} excluding {@code 'i'}, {@code 'l'} and {@code 'o'}.
         * Decoding is case-insensitive.
         */
        BASE32(new byte[] {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'b', 'c', 'd', 'e', 'f', 'g', 'h', 'j', 'k', 'm',
            'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
        });

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
         * @param encoding The mapping from numerical values to symbols.
         */
        private Format(final byte[] encoding) {
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

    private static final byte[] BITS = { 16, 8, 4, 2, 1 };

    /**
     * Creates a new geohash coder/decoder initialized to the default precision.
     */
    public GeoHashCoder() {
        format = Format.BASE32;
        precision = 12;
    }

    /**
     * Returns the current encoding/decoding format.
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
     * Returns the length of geohashes strings to be encoded by the {@link #encode(double, double)} method.
     *
     * @return The length of geohashes strings.
     */
    public int getPrecision() {
        return precision;
    }

    /**
     * Sets the length of geohashes strings to be encoded by the {@link #encode(double, double)} method.
     *
     * @param precision The new length of geohashes strings.
     */
    public void setPrecision(final int precision) {
        ArgumentChecks.ensureBetween("precision", 1, 255, precision);
        this.precision = (byte) precision;
    }

    /**
     * Encodes the given latitude and longitude into a geohash.
     *
     * @param latitude
     *          Latitude to encode
     * @param longitude
     *          Longitude to encode
     * @return Geohash encoding of the longitude and latitude
     */
    public String encode(final double latitude, final double longitude) {
        final byte[] encoding = format.encoding;
        final double[] latInterval = { -90.0, 90.0 };
        final double[] lngInterval = { -180.0, 180.0 };

        char[] geohash = buffer;
        if (geohash == null) {
            buffer = geohash = new char[precision & 0xFF];
        }
        boolean isEven = true;

        int bit = 0;
        int ch = 0;

        for (int i=0; i<geohash.length;) {
            double mid;
            if (isEven) {
                mid = (lngInterval[0] + lngInterval[1]) / 2D;
                if (longitude > mid) {
                    ch |= BITS[bit];
                    lngInterval[0] = mid;
                } else {
                    lngInterval[1] = mid;
                }
            } else {
                mid = (latInterval[0] + latInterval[1]) / 2D;
                if (latitude > mid) {
                    ch |= BITS[bit];
                    latInterval[0] = mid;
                } else {
                    latInterval[1] = mid;
                }
            }

            isEven = !isEven;

            if (bit < 4) {
                bit++;
            } else {
                geohash[i++] = (char) encoding[ch];
                bit = 0;
                ch = 0;
            }
        }
        return new String(geohash);
    }

    /**
     * Decodes the given geohash into a latitude and longitude
     *
     * @param geohash
     *          Geohash to decode
     * @return Array with the latitude at index 0, and longitude at index 1
     * @throws ParseException
     */
    public double[] decode(final String geohash) throws ParseException {
        final int length = geohash.length();
        final byte[] decodingLowerCase = format.decodingLowerCase;
        final byte[] decodingUpperCase = format.decodingUpperCase;
        final double[] latInterval = { -90.0, 90.0 };
        final double[] lngInterval = { -180.0, 180.0 };
        boolean isEven = true;

        int nc;
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
            for (int mask : BITS) {
                if (isEven) {
                    if ((c & mask) != 0) {
                        lngInterval[0] = (lngInterval[0] + lngInterval[1]) / 2D;
                    } else {
                        lngInterval[1] = (lngInterval[0] + lngInterval[1]) / 2D;
                    }
                } else {
                    if ((c & mask) != 0) {
                        latInterval[0] = (latInterval[0] + latInterval[1]) / 2D;
                    } else {
                        latInterval[1] = (latInterval[0] + latInterval[1]) / 2D;
                    }
                }
                isEven = !isEven;
            }
        }
        double latitude = (latInterval[0] + latInterval[1]) / 2D;
        double longitude = (lngInterval[0] + lngInterval[1]) / 2D;

        return new double[] { latitude, longitude };
    }
}
