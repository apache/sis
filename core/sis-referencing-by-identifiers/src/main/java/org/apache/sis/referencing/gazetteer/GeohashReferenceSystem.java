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
package org.apache.sis.referencing.gazetteer;

import javax.xml.bind.annotation.XmlTransient;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;
import org.opengis.geometry.DirectPosition;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Longitude;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.crs.DefaultGeographicCRS;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;

// Branch-dependent imports
import org.opengis.referencing.gazetteer.Location;
import org.opengis.referencing.gazetteer.LocationType;


/**
 * Geographic coordinates represented as <cite>geohashes</cite> strings.
 *
 * @author  Chris Mattmann (JPL)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 *
 * @see <a href="http://en.wikipedia.org/wiki/Geohash">Geohash on Wikipedia</a>
 */
@XmlTransient
public class GeohashReferenceSystem extends ReferencingByIdentifiers {
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
         * Mapping from a numerical value to its symbol.
         * The length of this array is the base of the encoding, e.g. 32 for {@link #BASE32}.
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
         * @param  highestOneBit  the leftmost one-bit of the value represented by a letter or digit.
         * @param  encoding       the mapping from numerical values to symbols.
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
            /*
             * Current version creates a case-insensitive format.
             * However if we implement BASE36 in a future version,
             * then the two 'decoding' arrays will differ.
             */
        }
    }

    /**
     * The format used by the {@code GeohashReferenceSystem.Coder}.
     */
    final Format format;

    /**
     * The {@link #crs} with (<var>longitude</var>, <var>latitude</var>) axis order in degrees.
     */
    final DefaultGeographicCRS normalizedCRS;

    /**
     * The coordinate operation from {@link #normalizedCRS} to the CRS specified by the user.
     * The target CRS is the coordinate reference system to assign to the decoded positions.
     */
    final CoordinateOperation denormalize;

    /**
     * Creates a new geohash reference system for the given format and coordinate reference system.
     *
     * @param  format  the format used by the {@code GeohashReferenceSystem.Coder}.
     * @param  crs     the coordinate reference system. This is usually {@link CommonCRS#defaultGeographic()}.
     * @throws GazetteerException if the reference system can not be created.
     */
    public GeohashReferenceSystem(final Format format, final GeographicCRS crs) throws GazetteerException {
        super(properties("Geohash", null), types());
        ArgumentChecks.ensureNonNull("format", format);
        ArgumentChecks.ensureNonNull("crs", crs);
        ArgumentChecks.ensureDimensionMatches("crs", 2, crs);
        this.format = format;
        normalizedCRS = DefaultGeographicCRS.castOrCopy(crs).forConvention(AxesConvention.NORMALIZED);
        try {
            denormalize = CRS.findOperation(normalizedCRS, crs, null);
        } catch (FactoryException e) {
            throw new GazetteerException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.8")
    private static LocationType[] types() {
        final ModifiableLocationType gzd = new ModifiableLocationType("Geohash");
        gzd.addIdentification(Vocabulary.formatInternational(Vocabulary.Keys.Code));
        return new LocationType[] {gzd};
    }

    /**
     * Returns the encoding/decoding format.
     *
     * @return the encoding/decoding format.
     */
    public Format getFormat() {
        return format;
    }

    /**
     * Returns a new object performing conversions between {@code DirectPosition} and geohashes.
     * The returned object is <strong>not</strong> thread-safe; a new instance must be created
     * for each thread, or synchronization must be applied by the caller.
     *
     * @return a new object performing conversions between {@link DirectPosition} and geohashes.
     */
    public Coder createCoder() {
        return new Coder();
    }

    /**
     * Conversions between direct positions and geohashes. Each {@code Coder} instance can read codes
     * at arbitrary precision, but formats at the {@linkplain #setHashLength specified precision}.
     * The same {@code Coder} instance can be reused for reading or writing many geohashes.
     *
     * <div class="section">Immutability and thread safety</div>
     * This class is <strong>not</strong> thread-safe. A new instance must be created for each thread,
     * or synchronization must be applied by the caller.
     *
     * @author  Chris Mattmann (JPL)
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.8
     * @version 0.8
     * @module
     */
    public class Coder {
        /**
         * Amount of letters or digits to format in the geohash.
         * Stored as a {@code byte} on the assumption that attempts to create
         * geohashes of more then 255 characters is likely to be an error anyway.
         */
        private byte length;

        /**
         * A buffer of length {@link #length}, created when first needed.
         */
        private transient char[] buffer;

        /**
         * Last coordinate operation used by {@link #encode(DirectPosition)}.
         */
        private transient CoordinateOperation lastOp;

        /**
         * Temporary array for coordinate transformation, or {@code null} if not needed.
         */
        private transient final double[] coordinates;

        /**
         * Creates a new geohash coder/decoder initialized to the default precision.
         */
        protected Coder() {
            length = 12;
            coordinates = denormalize.getMathTransform().isIdentity() ? null : new double[8];
        }

        /**
         * Returns the length of geohashes strings to be encoded by the {@link #encode(DirectPosition)} method.
         * The default value for {@link Format#BASE32} is 12.
         *
         * @return the length of geohashes strings.
         */
        public int getHashLength() {
            return length;
        }

        /**
         * Sets the length of geohashes strings to be encoded by the {@link #encode(DirectPosition)} method.
         *
         * @param  length  the new length of geohashes strings.
         */
        public void setHashLength(final int length) {
            ArgumentChecks.ensureBetween("length", 1, 255, length);
            this.length = (byte) length;
            buffer = null;                                  // Will recreate a new buffer when first needed.
        }

        /**
         * Encodes the given latitude and longitude into a geohash.
         * This method does <strong>not</strong> take in account the axis order and units of the coordinate
         * reference system (CRS) given to the {@link GeohashReferenceSystem} constructor. For geohashing of
         * coordinates in different CRS, use {@link #encode(DirectPosition)} instead.
         *
         * @param  φ  latitude to encode,  as decimal degrees in the [-90 … 90]° range.
         * @param  λ  longitude to encode, as decimal degrees in the [-180 … 180]° range.
         * @return geohash encoding of the given longitude and latitude.
         * @throws TransformException if an error occurred while formatting the given coordinate.
         */
        public String encode(double φ, double λ) throws TransformException {
            φ = Latitude.clamp(φ);
            λ = Longitude.normalize(λ);
            final byte[] encoding   = format.encoding;
            final int highestOneBit = format.highestOneBit;
            char[] geohash = buffer;
            if (geohash == null) {
                buffer = geohash = new char[length & 0xFF];
            }
            /*
             * The current implementation assumes a two-dimensional coordinates. The 'isEven' boolean takes
             * the 'true' value for longitude, and 'false' for latitude. We could extend this algorithm to
             * the multi-dimensional case by replacing 'isEven' by a counter over the ordinate dimension.
             */
            boolean isEven = true;
            double xmin = Longitude.MIN_VALUE, ymin = Latitude.MIN_VALUE;
            double xmax = Longitude.MAX_VALUE, ymax = Latitude.MAX_VALUE;
            /*
             * 'ch' is the index of the character to be added in the geohash. The actual character will be
             * given by the 'encoding' array. 'bit' shall have a single one-bit, rotating from 10000 in the
             * BASE32 case to 00001 (binary representation).
             */
            int ch = 0;
            int bit = highestOneBit;
            for (int i=0; i < geohash.length;) {
                if (isEven) {
                    final double mid = (xmin + xmax) / 2;
                    if (λ > mid) {
                        ch |= bit;
                        xmin = mid;
                    } else {
                        xmax = mid;
                    }
                } else {
                    final double mid = (ymin + ymax) / 2;
                    if (φ > mid) {
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
         * Encodes the given position into a geohash. The default implementation transforms the given position
         * to the coordinate reference system expected by the enclosing {@link GeohashReferenceSystem}, then
         * delegates to {@link #encode(double, double)}.
         *
         * @param  position  the coordinate to encode.
         * @return geohash encoding of the given position.
         * @throws TransformException if an error occurred while transforming the given coordinate to a geohash reference.
         */
        public String encode(DirectPosition position) throws TransformException {
            ArgumentChecks.ensureNonNull("position", position);
            final CoordinateReferenceSystem ps = position.getCoordinateReferenceSystem();
            if (ps != null && !normalizedCRS.equals(ps, ComparisonMode.IGNORE_METADATA)) {
                if (lastOp == null || !Utilities.equalsIgnoreMetadata(lastOp.getSourceCRS(), ps)) try {
                    lastOp = CRS.findOperation(ps, normalizedCRS, null);
                } catch (FactoryException e) {
                    throw new GazetteerException(e.getLocalizedMessage(), e);
                }
                position = lastOp.getMathTransform().transform(position, null);
            }
            return encode(position.getOrdinate(1), position.getOrdinate(0));
        }

        /**
         * Decodes the given geohash into a latitude and a longitude.
         * The axis order depends on the coordinate reference system of the enclosing {@link GeohashReferenceSystem}.
         *
         * @param  geohash  geohash string to decode.
         * @return a new geographic coordinate for the given geohash.
         * @throws TransformException if an error occurred while parsing the given string.
         */
        public Location decode(final CharSequence geohash) throws TransformException {
            ArgumentChecks.ensureNonEmpty("geohash", geohash);
            return new Decoder(geohash, coordinates);
        }
    }

    /**
     * The result of decoding a geohash.
     * The {@linkplain #getPosition() position} represents the centroid of the decoded geohash.
     */
    private final class Decoder extends SimpleLocation {
        /**
         * Decodes the given geohash.
         *
         * @param owner  the {@code Coder} which is creating this {@code Decoder}.
         */
        Decoder(final CharSequence geohash, final double[] coordinates) throws TransformException {
            super(rootType(), geohash);
            final int    length            = geohash.length();
            final int    highestOneBit     = format.highestOneBit;
            final byte[] decodingLowerCase = format.decodingLowerCase;
            final byte[] decodingUpperCase = format.decodingUpperCase;
            /*
             * The current implementation assumes a two-dimensional coordinates. The 'isEven' boolean takes
             * the 'true' value for longitude, and 'false' for latitude. We could extend this algorithm to
             * the multi-dimensional case by replacing 'isEven' by a counter over the ordinate dimension.
             */
            boolean isEven = true;
            minX = Longitude.MIN_VALUE;
            maxX = Longitude.MAX_VALUE;
            minY = Latitude .MIN_VALUE;
            maxY = Latitude .MAX_VALUE;

            int nc;                                         // Number of characters for the 'c' code point.
            for (int i=0; i<length; i+=nc) {
                int c = Character.codePointAt(geohash, i);
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
                        throw new GazetteerException(Errors.format(Errors.Keys.UnparsableStringForClass_3,
                                "GeoHash", geohash, geohash.subSequence(i, i+nc)));
                    }
                }
                int mask = highestOneBit;
                do {
                    if (isEven) {
                        final double mid = (minX + maxX) / 2;
                        if ((c & mask) != 0) {
                            minX = mid;
                        } else {
                            maxX = mid;
                        }
                    } else {
                        final double mid = (minY + maxY) / 2;
                        if ((c & mask) != 0) {
                            minY = mid;
                        } else {
                            maxY = mid;
                        }
                    }
                    isEven = !isEven;
                } while ((mask >>>= 1) != 0);
            }
            if (coordinates != null) {
                convert(denormalize.getMathTransform(), coordinates);
            }
        }

        /**
         * Returns the Coordinate Reference System of the decoded geohash.
         */
        @Override
        public CoordinateReferenceSystem getCoordinateReferenceSystem() {
            return denormalize.getTargetCRS();
        }
    }
}
