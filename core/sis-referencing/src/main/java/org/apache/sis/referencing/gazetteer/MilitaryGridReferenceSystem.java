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

import java.util.Map;
import java.util.IdentityHashMap;
import java.util.ConcurrentModificationException;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.referencing.provider.TransverseMercator;
import org.apache.sis.internal.referencing.Resources;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.geometry.DirectPosition2D;

import static org.apache.sis.referencing.gazetteer.MGRSEncoder.*;


/**
 * The Military Grid Reference System (MGRS).
 * The MGRS is the geocoordinate standard used by NATO militaries for locating points on the earth.
 * It is based on the Universal Transverse Mercator (UTM) and the Universal Polar Stereographic (UPS) projections.
 * Despite its name, MGRS is used not only for military purposes; it is used also for organizing Earth Observation
 * data in directory trees for example.
 *
 * <div class="section">Immutability and thread safety</div>
 * This class is immutable and thus thread-safe.
 * However the {@link Coder Coder} instances performing conversions between labels and coordinates are not thread-safe;
 * it is recommended to create a new {@code Coder} instance for each thread.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 *
 * @see <a href="https://en.wikipedia.org/wiki/Military_Grid_Reference_System">Military Grid Reference System on Wikipedia</a>
 */
public class MilitaryGridReferenceSystem {
    /**
     * The datum to which to transform the coordinate before formatting the MGRS label.
     * Only the datums enumerated in {@link CommonCRS} are currently supported.
     */
    final CommonCRS datum;

    /**
     * Whether {@link MGRSEncoder} should infer the datum from the given coordinates
     * instead than using {@link #datum}.
     */
    final boolean avoidDatumChange;

    /**
     * Creates a new Military Grid Reference System (MGRS) using the WGS84 datum.
     */
    public MilitaryGridReferenceSystem() {
        datum = CommonCRS.WGS84;
        avoidDatumChange = false;
    }

    /**
     * Creates a new Military Grid Reference System (MGRS) using the specified datum.
     * Only the datums enumerated in {@link CommonCRS} are currently supported.
     *
     * @param  datum  the datum to which to transform coordinates before formatting the MGRS labels,
     *                or {@code null} for inferring the datum from the CRS associated to each coordinate.
     */
    public MilitaryGridReferenceSystem(final CommonCRS datum) {
        this.datum = (datum != null) ? datum : CommonCRS.WGS84;
        avoidDatumChange = (datum == null);
    }

    /**
     * Returns a new object performing conversions between {@code DirectPosition} and MGRS labels.
     * The returned object is <strong>not</strong> thread-safe; a new instance must be created for
     * each thread, or synchronization must be applied by the caller.
     *
     * @return a new object performing conversions between {@link DirectPosition} and MGRS labels.
     */
    public Coder createCoder() {
        return new Coder();
    }

    /**
     * Conversions between direct positions and labels in the Military Grid Reference System (MGRS).
     * Each {@code Coder} instance can read labels at arbitrary precision, but formats at the
     * {@linkplain #setPrecision specified precision}.
     *
     * <div class="section">Immutability and thread safety</div>
     * This class is <strong>not</strong> thread-safe. A new instance must be created for each thread,
     * or synchronization must be applied by the caller.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.8
     * @version 0.8
     * @module
     */
    public class Coder {
        /**
         * Number of digits to use for formatting the numerical part of a MGRS label.
         */
        private byte digits;

        /**
         * The separator to insert between each component of the MGRS identifier, or an empty string if none.
         */
        private String separator;

        /**
         * Same separator, but without leading and trailing spaces.
         */
        private String trimmedSeparator;

        /**
         * Cached information needed for building a MGRS label from a direct position in the given CRS.
         */
        private final Map<CoordinateReferenceSystem,MGRSEncoder> encoders;

        /**
         * Temporary positions used by {@link MGRSEncoder} only. References are kept for avoiding to
         * recreate those temporary objects for every label to format.
         */
        DirectPosition normalized, geographic;

        /**
         * A buffer where to create label, to be reused for each new label.
         */
        final StringBuilder buffer;

        /**
         * Creates a new coder initialized to the default precision.
         */
        protected Coder() {
            digits    = 5;                          // 1 meter precision.
            separator = trimmedSeparator = "";
            buffer    = new StringBuilder(16);      // Length of "4 Q FJ 1234 5678" sample value.
            encoders  = new IdentityHashMap<>();
        }

        /**
         * Returns the precision of the labels formatted by this coder.
         * This method returns one of the following values:
         *
         * <table class="sis">
         *   <caption>MGRS label precisions</caption>
         *   <tr><th>Precision (m)</th>             <th>Label example</th></tr>
         *   <tr><td style="text-align:right">1</td> <td>4 Q FJ 12345 67890</td></tr>
         *   <tr><td style="text-align:right">10</td> <td>4 Q FJ 1234 6789</td></tr>
         *   <tr><td style="text-align:right">100</td> <td>4 Q FJ 123 678</td></tr>
         *   <tr><td style="text-align:right">1000</td> <td>4 Q FJ 12 67</td></tr>
         *   <tr><td style="text-align:right">10 000</td> <td>4 Q FJ 1 6</td></tr>
         *   <tr><td style="text-align:right">100 000</td> <td>4 Q FJ</td></tr>
         *   <tr><td style="text-align:right">(approximative) 1 000 000</td> <td>4 Q</td></tr>
         * </table>
         *
         * Values smaller than 1 (e.g. 0.01 for a centimetre precision) may also be returned
         * if that value has been {@linkplain #setPrecision(double) explicitely set},
         * but sub-metric precision are usually not used with MGRS.
         *
         * @return precision of formatted labels in metres.
         */
        public double getPrecision() {
            return MathFunctions.pow10(METRE_PRECISION_DIGITS - digits);
        }

        /**
         * Sets the desired precision of the labels formatted by this coder.
         * This method rounds the given precision to one of the power of 10
         * documented in the {@link #getPrecision()} method.
         *
         * @param  precision  the desired precision in metres.
         */
        public void setPrecision(final double precision) {
            final double p = Math.floor(Math.log10(precision));
            if (!Double.isFinite(p)) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "precision", precision));
            }
            // The -3 is an arbitrary limit to millimetre precision.
            int n = Math.max(-3, Math.min(METRE_PRECISION_DIGITS + 1, (int) p));
            digits = (byte) (METRE_PRECISION_DIGITS - n);
        }

        /**
         * Returns the separator to insert between each component of the MGRS identifier.
         * Components are zone number, latitude band, 100,000-metres square identifier and numerical values.
         * By default the separator is an empty string, which produce labels like "4QFJ12345678".
         *
         * @return the separator to insert between each component of the MGRS identifier, or an empty string if none.
         */
        public String getSeparator() {
            return separator;
        }

        /**
         * Sets the separator to insert between each component of the MGRS identifier.
         * Components are zone number, latitude band, 100,000-metres square identifier and numerical values.
         * By default the separator is an empty string, which produce labels like "4QFJ12345678".
         * If the separator is set to a space, then the labels will be formatted like "4 Q FJ 1234 5678".
         *
         * @param  separator  the separator to insert between each component of the MGRS identifier.
         */
        public void setSeparator(final String separator) {
            ArgumentChecks.ensureNonNull("separator", separator);
            this.separator = separator;
            trimmedSeparator = CharSequences.trimWhitespaces(separator);
        }

        /**
         * Encodes the given position into a MGRS label.
         * The given position must have a CRS associated to it.
         *
         * @param  position  the coordinate to encode.
         * @return MGRS encoding of the given position.
         * @throws TransformException if an error occurred while transforming the given coordinate to a MGRS label.
         */
        public String encode(final DirectPosition position) throws TransformException {
            ArgumentChecks.ensureNonNull("position", position);
            final CoordinateReferenceSystem crs = position.getCoordinateReferenceSystem();
            MGRSEncoder encoder = encoders.get(crs);
            try {
                if (encoder == null) {
                    encoder = new MGRSEncoder(avoidDatumChange ? null : datum, crs);
                    if (encoders.put(crs, encoder) != null) {
                        throw new ConcurrentModificationException();            // Opportunistic check.
                    }
                }
                return encoder.encode(this, position, separator, digits);
            } catch (IllegalArgumentException  | FactoryException e) {
                throw new GazetteerException(e.getLocalizedMessage(), e);
            }
        }

        /**
         * Decodes the given MGRS label into a position.
         *
         * @param  label  MGRS string to decode.
         * @return a new position with the longitude at ordinate 0 and latitude at ordinate 1.
         * @throws TransformException if an error occurred while parsing the given string.
         */
        public DirectPosition decode(final CharSequence label) throws TransformException {
            ArgumentChecks.ensureNonNull("label", label);
            final int end  = CharSequences.skipTrailingWhitespaces(label, 0, label.length());
            final int base = CharSequences.skipLeadingWhitespaces (label, 0, end);
            int i = endOfDigits(label, base, end);
            final int zone = parseInt(label, base, i, Resources.Keys.IllegalUTMZone_1);
            if (zone < 1 || zone > 60) {
                throw new GazetteerException(Resources.format(Resources.Keys.IllegalUTMZone_1, zone));
            }
            /*
             * Parse the sub-sequence made of letters. That sub-sequence can have one or three parts.
             * The first part is mandatory and the two other parts are optional, but if the two last
             * parts are omitted, then they must be omitted together.
             *
             *   1 — latitude band: C-X (excluding I and O) for UTM. Other letters (A, B, Y, Z) are for UPS.
             *   2 — column letter: A-H in zone 1, J-R (skipping O) in zone 2, S-Z in zone 3, then repeat.
             *   3 — row letter:    ABCDEFGHJKLMNPQRSTUV in odd zones, FGHJKLMNPQRSTUVABCDE in even zones.
             */
            double φ = Double.NaN;
            int col = 1, row = 0;
            for (int part = 1; part <= 3; part++) {
                i = nextComponent(label, base, i, end, part != 2);
                if (i >= end) {
                    break;                                      // Allowed only for part 2 (the column letter).
                }
                int c = Character.codePointAt(label, i);
                final int ni = i + Character.charCount(c);
                if (c < 'A' || c > 'Z') {
                    if (c >= 'a' && c <= 'z') {
                        c -= ('a' - 'A');
                    } else {
                        final short key;
                        final CharSequence token;
                        if (part == 1) {
                            key = Resources.Keys.IllegalLatitudeBand_1;
                            token = label.subSequence(i, ni);
                        } else {
                            key = Resources.Keys.IllegalSquareIdentification_1;
                            token = CharSequences.token(label, i);
                        }
                        throw new GazetteerException(Resources.format(key, token));
                    }
                }
                /*
                 * At this point, 'c' is a valid letter. First, applies a correction for the fact that 'I' and 'O'
                 * letters were excluded. Next, the conversion to latitude or 100 000 meters grid indices depends
                 * on which part we are parsing. The formulas used below are about the same than in MGRSEncoder,
                 * with terms moved on the other side of the equations.
                 */
                if (c >= EXCLUDE_O) c--;
                if (c >= EXCLUDE_I) c--;
                switch (part) {
                    case 1: {
                        φ = (c - 'C') * LATITUDE_BAND_HEIGHT + TransverseMercator.Zoner.SOUTH_BOUNDS;
                        break;
                    }
                    case 2: {
                        switch (zone % 3) {                         // First A-H sequence starts at zone number 1.
                            case 1: col = c - ('A' - 1); break;
                            case 2: col = c - ('J' - 2); break;     // -2 because 'I' has already been excluded.
                            case 0: col = c - ('S' - 3); break;     // -3 because 'I' and 'O' have been excluded.
                        }
                        break;
                    }
                    case 3: {
                        row = c - (((zone & 1) == 0) ? 'F' : 'A');
                        break;
                    }
                }
                i = ni;
            }
            /*
             * We need to create a UTM projection from (φ,λ) coordinates, not from UTM zone,
             * because there is special cases to take in account for Norway and Svalbard.
             */
            final double λ = TransverseMercator.Zoner.UTM.centralMeridian(zone);
            final ProjectedCRS crs = datum.universal(φ,λ);
            final DirectPosition2D pos = new DirectPosition2D(φ,λ);
            row += ((int) (crs.getConversionFromBase().getMathTransform().transform(pos, pos).getOrdinate(1)
                    / (GRID_SQUARE_SIZE * GRID_ROW_COUNT))) * GRID_ROW_COUNT;

            pos.setCoordinateReferenceSystem(crs);
            pos.x = col * GRID_SQUARE_SIZE;
            pos.y = row * GRID_SQUARE_SIZE;
            return pos;
        }

        /**
         * Skips spaces, then the separator if present (optional).
         *
         * @param  label      the label to parse.
         * @param  base       index where the parsing began. Used for formatting error message only.
         * @param  start      current parsing position.
         * @param  end        where the parsing is expected to end.
         * @param  mandatory  whether to throw an exception or return {@code end} if we reached the end of string.
         * @return position where to continue parsing (with spaces skipped), or {@code end} if we reached end of string.
         * @throws GazetteerException if this method unexpectedly reached the end of string.
         */
        private int nextComponent(final CharSequence label, final int base, int start, final int end, final boolean mandatory)
                throws GazetteerException
        {
            start = CharSequences.skipLeadingWhitespaces(label, start, end);
            if (start < end) {
                if (!CharSequences.regionMatches(label, start, trimmedSeparator)) {
                    return start;               // Separator not found, but it was optional.
                }
                start += trimmedSeparator.length();
                start = CharSequences.skipLeadingWhitespaces(label, start, end);
                if (start < end) {
                    return start;
                }
            }
            if (!mandatory) return start;
            throw new GazetteerException(Errors.format(Errors.Keys.UnexpectedEndOfString_1, label.subSequence(base, end)));
        }
    }

    /**
     * Returns the index after the last digit in a sequence of ASCII characters.
     * Leading whitespaces must have been skipped before to invoke this method.
     */
    static int endOfDigits(final CharSequence label, int i, final int end) {
        while (i < end) {
            final char c = label.charAt(i);     // Code-point API not needed here because we restrict to ASCII.
            if (c < '0' || c > '9') {           // Do not use Character.isDigit(…) because we restrict to ASCII.
                break;
            }
            i++;
        }
        return i;
    }

    /**
     * Parses part of the given character sequence as an integer.
     *
     * @param  label     the MGRS label to parse.
     * @param  start     index of the first character to parse as an integer.
     * @param  end       index after the last character to parse as an integer.
     * @param  errorKey  {@link Resources.Keys} value to use in case of error.
     *                   The error message string shall accept exactly one argument.
     * @return the parsed integer.
     * @throws GazetteerException if the string can not be parsed as an integer.
     */
    static int parseInt(final CharSequence label, final int start, final int end, final short errorKey)
            throws GazetteerException
    {
        NumberFormatException cause = null;
        final CharSequence part;
        if (start == end) {
            part = CharSequences.token(label, start);
        } else {
            part = label.subSequence(start, end);
            try {
                return Integer.parseInt(part.toString());
            } catch (NumberFormatException e) {
                cause = e;
            }
        }
        throw new GazetteerException(Resources.format(errorKey, part), cause);
    }
}
