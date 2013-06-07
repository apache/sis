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
     * The characters used for the 32 bits variant of geohash.
     */
    private static final byte[] ENCODING_32 = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'b', 'c', 'd', 'e', 'f', 'g', 'h', 'j', 'k', 'm',
        'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
    };

    /**
     * The reverse of {@link #ENCODING_32}.
     */
    private static final byte[] DECODING_32 = reverse(ENCODING_32);

    /**
     * Creates a decoding array from the given encoding one.
     * Only the letters are stored in this array, not the digits.
     */
    private static byte[] reverse(final byte[] encoding) {
        final byte[] decoding = new byte[26];
        for (byte i=10; i<encoding.length; i++) {
            decoding[encoding[i] - 'a'] = i;
        }
        return decoding;
    }

    /**
     * Amount of letters or digits to format in the geohash.
     */
    private int precision;

    private static final int[] BITS = { 16, 8, 4, 2, 1 };

    /**
     * Creates a new geohash coder/decoder initialized to the default precision.
     */
    public GeoHashCoder() {
        precision = 12;
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
      final double[] latInterval = { -90.0, 90.0 };
      final double[] lngInterval = { -180.0, 180.0 };

      final StringBuilder geohash = new StringBuilder();
      boolean isEven = true;

      int bit = 0;
      int ch = 0;

      while (geohash.length() < precision) {
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
            geohash.append((char) ENCODING_32[ch]);
            bit = 0;
            ch = 0;
            }
        }
        return geohash.toString();
    }

    /**
     * Decodes the given geohash into a latitude and longitude
     *
     * @param geohash
     *          Geohash to decode
     * @return Array with the latitude at index 0, and longitude at index 1
     */
    public double[] decode(final String geohash) {
        final int length = geohash.length();
        final double[] latInterval = { -90.0, 90.0 };
        final double[] lngInterval = { -180.0, 180.0 };
        boolean isEven = true;

        int nc;
        for (int i=0; i<length; i+=nc) {
            int c = geohash.codePointAt(i);
            nc = Character.charCount(c);
            if (c >= 'a' && c <= 'z') {
                c = DECODING_32[c - 'a'];
                if (c == 0) {
                    throw new IllegalArgumentException(geohash);
                }
            } else if (c >= '0' && c <= '9') {
                c -= '0';
            } else {
                throw new IllegalArgumentException(geohash);
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
