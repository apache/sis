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
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.Projection;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.referencing.provider.TransverseMercator;
import org.apache.sis.internal.referencing.provider.PolarStereographicA;
import org.apache.sis.internal.referencing.Resources;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.crs.DefaultProjectedCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.StringBuilders;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.geometry.DirectPosition2D;


/**
 * The Military Grid Reference System (MGRS).
 * The MGRS is the geocoordinate standard used by NATO militaries for locating points on the earth.
 * It is based on the Universal Transverse Mercator (UTM) and the Universal Polar Stereographic (UPS) projections.
 * Despite its name, MGRS is used not only for military purposes; it is used also for organizing Earth Observation
 * data in directory trees for example.
 *
 * <div class="section">Immutability and thread safety</div>
 * This class is immutable and thus thread-safe.
 * However the {@link Coder Coder} instances performing conversions between references and coordinates
 * are not thread-safe; it is recommended to create a new {@code Coder} instance for each thread.
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
     * Height of latitude bands, in degrees.
     * Those bands are labeled from {@code 'C'} to {@code 'X'} inclusive, excluding {@code 'I'} and {@code 'O'}.
     */
    private static final double LATITUDE_BAND_HEIGHT = 8;

    /**
     * Size of the 100 000-metres squares.
     */
    static final double GRID_SQUARE_SIZE = 100_000;

    /**
     * Number of letters available for grid rows. Those letters are "ABCDEFGHJKLMNPQRSTUV" (starting at letter
     * F for zones of even number), repeated in a cycle. Each row is {@value #GRID_SQUARE_SIZE} metres height.
     */
    private static final int GRID_ROW_COUNT = 20;

    /**
     * The number of digits in a one-meter precision when formatting MGRS references.
     *
     * <p><b>Invariant:</b> the following relationship must hold:
     * {@code GRID_SQUARE_SIZE == Math.pow(10, METRE_PRECISION_DIGITS)}
     */
    static final int METRE_PRECISION_DIGITS = 5;

    /**
     * The first of the two letters ({@code 'I'} and {@code 'O'}) excluded in MGRS notation.
     * This letter and all following letters shall be shifted by one character. Example:
     *
     * {@preformat java
     *     char band = ...;
     *     if (band >= EXCLUDE_I) {
     *         band++;
     *         if (band >= EXCLUDE_O) band++;
     *     }
     * }
     *
     * or equivalently:
     *
     * {@preformat java
     *     char band = ...;
     *     if (band >= EXCLUDE_I && ++band >= EXCLUDE_O) band++;
     * }
     */
    private static final char EXCLUDE_I = 'I';

    /**
     * The second of the two letters ({@code 'I'} and {@code 'O'}) excluded in MGRS notation.
     */
    private static final char EXCLUDE_O = 'O';

    /**
     * The datum to which to transform the coordinate before formatting the MGRS reference.
     * Only the datums enumerated in {@link CommonCRS} are currently supported.
     */
    final CommonCRS datum;

    /**
     * Whether {@link Encoder} should infer the datum from the given coordinates
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
     * @param  datum  the datum to which to transform coordinates before formatting the MGRS references,
     *                or {@code null} for inferring the datum from the CRS associated to each coordinate.
     */
    public MilitaryGridReferenceSystem(final CommonCRS datum) {
        this.datum = (datum != null) ? datum : CommonCRS.WGS84;
        avoidDatumChange = (datum == null);
    }

    /**
     * Returns a new object performing conversions between {@code DirectPosition} and MGRS references.
     * The returned object is <strong>not</strong> thread-safe; a new instance must be created for
     * each thread, or synchronization must be applied by the caller.
     *
     * @return a new object performing conversions between {@link DirectPosition} and MGRS references.
     */
    public Coder createCoder() {
        return new Coder();
    }

    /**
     * Conversions between direct positions and references in the Military Grid Reference System (MGRS).
     * Each {@code Coder} instance can read references at arbitrary precision, but formats at the
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
         * Number of digits to use for formatting the numerical part of a MGRS reference.
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
         * Cached information needed for building a MGRS reference from a direct position in the given CRS.
         */
        private final Map<CoordinateReferenceSystem,Encoder> encoders;

        /**
         * Temporary positions used by {@link Encoder} only. References are kept for avoiding to
         * recreate those temporary objects for every reference to format.
         */
        DirectPosition normalized, geographic;

        /**
         * A buffer where to create reference, to be reused for each new reference.
         */
        final StringBuilder buffer;

        /**
         * Creates a new coder initialized to the default precision.
         */
        protected Coder() {
            digits    = METRE_PRECISION_DIGITS;     // 1 metre precision.
            separator = trimmedSeparator = "";
            buffer    = new StringBuilder(18);      // Length of "4 Q FJ 12345 67890" sample value.
            encoders  = new IdentityHashMap<>();
        }

        /**
         * Returns the precision of the references formatted by this coder.
         * This method returns one of the following values:
         *
         * <table class="sis">
         *   <caption>MGRS reference precisions</caption>
         *   <tr><th>Precision (m)</th>             <th>Reference example</th></tr>
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
         * @return precision of formatted references in metres.
         */
        public double getPrecision() {
            return MathFunctions.pow10(METRE_PRECISION_DIGITS - digits);
        }

        /**
         * Sets the desired precision of the references formatted by this coder.
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
         * Components are zone number, latitude band, 100 000-metres square identifier and numerical values.
         * By default the separator is an empty string, which produce references like "4QFJ12345678".
         *
         * @return the separator to insert between each component of the MGRS identifier, or an empty string if none.
         */
        public String getSeparator() {
            return separator;
        }

        /**
         * Sets the separator to insert between each component of the MGRS identifier.
         * Components are zone number, latitude band, 100 000-metres square identifier and numerical values.
         * By default the separator is an empty string, which produce references like "4QFJ12345678".
         * If the separator is set to a space, then the references will be formatted like "4 Q FJ 1234 5678".
         *
         * <p>Note that a MGRS reference is normally written as an entity without spaces, parentheses, dashes,
         * or decimal points. Invoking this method with a non-empty separator produces non-conform MGRS, but
         * is sometime convenient for readability or for use in file systems (with the {@code '/'} separator).</p>
         *
         * @param  separator  the separator to insert between each component of the MGRS identifier.
         */
        public void setSeparator(final String separator) {
            ArgumentChecks.ensureNonNull("separator", separator);
            this.separator = separator;
            trimmedSeparator = CharSequences.trimWhitespaces(separator);
        }

        /**
         * Encodes the given position into a MGRS reference.
         * The given position must have a Coordinate Reference System (CRS) associated to it.
         *
         * @param  position  the coordinate to encode.
         * @return MGRS encoding of the given position.
         * @throws TransformException if an error occurred while transforming the given coordinate to a MGRS reference.
         */
        public String encode(final DirectPosition position) throws TransformException {
            ArgumentChecks.ensureNonNull("position", position);
            final CoordinateReferenceSystem crs = position.getCoordinateReferenceSystem();
            if (crs == null) {
                throw new GazetteerException(Errors.format(Errors.Keys.UnspecifiedCRS));
            }
            Encoder encoder = encoders.get(crs);
            try {
                // We can not use encoders.computeIfAbsent(crs, ...) because of checked exceptions.
                if (encoder == null) {
                    encoder = new Encoder(avoidDatumChange ? null : datum, crs);
                    if (encoders.put(crs, encoder) != null) {
                        throw new ConcurrentModificationException();            // Opportunistic check.
                    }
                }
                return encoder.encode(this, position, separator, digits);
            } catch (IllegalArgumentException | FactoryException e) {
                throw new GazetteerException(e.getLocalizedMessage(), e);
            }
        }

        /**
         * Decodes the given MGRS reference into a position.
         * The Coordinate Reference System (CRS) associated to the returned position depends on the given reference.
         *
         * @param  reference  MGRS string to decode.
         * @return a new position with the longitude at ordinate 0 and latitude at ordinate 1.
         * @throws TransformException if an error occurred while parsing the given string.
         */
        public DirectPosition decode(final CharSequence reference) throws TransformException {
            ArgumentChecks.ensureNonNull("reference", reference);
            final int end  = CharSequences.skipTrailingWhitespaces(reference, 0, reference.length());
            final int base = CharSequences.skipLeadingWhitespaces (reference, 0, end);
            int i = endOfDigits(reference, base, end);
            final int zone = parseInt(reference, base, i, Resources.Keys.IllegalUTMZone_1);
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
            double φs = Double.NaN;
            int col = 1, row = 0;
            for (int part = 1; part <= 3; part++) {
                if (part == 2 && i >= end) {
                    break;                                      // Allow to stop parsing only after part 1.
                }
                i = nextComponent(reference, base, i, end);
                int c = Character.codePointAt(reference, i);
                final int ni = i + Character.charCount(c);
                if (c < 'A' || c > 'Z') {
                    if (c >= 'a' && c <= 'z') {
                        c -= ('a' - 'A');
                    } else {
                        final short key;
                        final CharSequence token;
                        if (part == 1) {
                            key = Resources.Keys.IllegalLatitudeBand_1;
                            token = reference.subSequence(i, ni);
                        } else {
                            key = Resources.Keys.IllegalSquareIdentification_1;
                            token = CharSequences.token(reference, i);
                        }
                        throw new GazetteerException(Resources.format(key, token));
                    }
                }
                /*
                 * At this point, 'c' is a valid letter. First, applies a correction for the fact that 'I' and 'O'
                 * letters were excluded. Next, the conversion to latitude or 100 000 meters grid indices depends
                 * on which part we are parsing. The formulas used below are about the same than in Encoder class,
                 * with terms moved on the other side of the equations.
                 */
                if (c >= EXCLUDE_O) c--;
                if (c >= EXCLUDE_I) c--;
                switch (part) {
                    case 1: {
                        φs = (c - 'C') * LATITUDE_BAND_HEIGHT + TransverseMercator.Zoner.SOUTH_BOUNDS;
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
            final double λ0 = TransverseMercator.Zoner.UTM.centralMeridian(zone);
            final ProjectedCRS crs = datum.universal(φs, λ0);
            final DirectPosition2D pos = new DirectPosition2D(φs, λ0);
            final MathTransform projection = crs.getConversionFromBase().getMathTransform();
            row += ((int) (projection.transform(pos, pos).getOrdinate(1)
                    / (GRID_SQUARE_SIZE * GRID_ROW_COUNT))) * GRID_ROW_COUNT;

            pos.setCoordinateReferenceSystem(crs);
            pos.x = col * GRID_SQUARE_SIZE;
            pos.y = row * GRID_SQUARE_SIZE;
            if (i < end) {
                /*
                 * If we have not yet reached the end of string, parse the numerical location.
                 * That location is normally encoded as a single number with an even number of digits.
                 * The first half is the easting and the second half is the northing, both relative to the
                 * 100 000-meter square. However some variants of MGRS use a separator, in which case we get
                 * two distinct numbers. In both cases, the resolution is determined by the amount of digits.
                 */
                i = nextComponent(reference, base, i, end);
                int s = endOfDigits(reference, i, end);
                final double x, y;
                if (s >= end) {
                    final int length = s - i;
                    final int h = i + (length >>> 1);
                    if ((length & 1) != 0) {
                        throw new GazetteerException(Resources.format(Resources.Keys.MismatchedResolution_2,
                                reference.subSequence(i, h), reference.subSequence(h, s)));
                    }
                    x = parseCoordinate(reference, i, h);
                    y = parseCoordinate(reference, h, s);
                } else {
                    x = parseCoordinate(reference, i, s);
                    i = nextComponent(reference, base, s, end);
                    s = endOfDigits(reference, i, end);
                    y = parseCoordinate(reference, i, s);
                    if (s < end) {
                        throw new GazetteerException(Errors.format(Errors.Keys.UnexpectedCharactersAfter_2,
                                reference.subSequence(base, s), CharSequences.trimWhitespaces(reference, s, end)));
                    }
                }
                pos.x += x;
                pos.y += y;
            }
            /*
             * The southernmost bound of the latitude band (φs) has been computed at the longitude of the central
             * meridian (λ₀). But the (x,y) position that we just parsed is probably at another longitude (λ), in
             * which case its northing value (y) is closer to the pole of its hemisphere. The slight difference in
             * northing values can cause a change of 100 000-metres grid square. We detect this case by converting
             * (x,y) to geographic coordinates (φ,λ) and verifying if the result is in the expected latitude band.
             * We also use this calculation for error detection, by verifying if the given 100 000-metres square
             * identification is consistent with grid zone designation.
             */
            final MathTransform inverse = projection.inverse();
            DirectPosition check = inverse.transform(pos, null);
            final double sign = Math.signum(φs);
            double error = (φs - check.getOrdinate(0)) / LATITUDE_BAND_HEIGHT;
            if (error > 0) {
                if (error < 1) {
                    pos.y += sign * (GRID_SQUARE_SIZE * GRID_ROW_COUNT);
                } else {
                    throw new GazetteerException("Iconsistent MGRS reference.");    // TODO: localize
                }
            }
            return pos;
        }

        /**
         * Skips spaces, then the separator if present (optional).
         *
         * @param  reference  the reference to parse.
         * @param  base       index where the parsing began. Used for formatting error message only.
         * @param  start      current parsing position.
         * @param  end        where the parsing is expected to end.
         * @return position where to continue parsing (with spaces skipped).
         * @throws GazetteerException if this method unexpectedly reached the end of string.
         */
        private int nextComponent(final CharSequence reference, final int base, int start, final int end)
                throws GazetteerException
        {
            start = CharSequences.skipLeadingWhitespaces(reference, start, end);
            if (start < end) {
                if (!CharSequences.regionMatches(reference, start, trimmedSeparator)) {
                    return start;               // Separator not found, but it was optional.
                }
                start += trimmedSeparator.length();
                start = CharSequences.skipLeadingWhitespaces(reference, start, end);
                if (start < end) {
                    return start;
                }
            }
            throw new GazetteerException(Errors.format(Errors.Keys.UnexpectedEndOfString_1, reference.subSequence(base, end)));
        }
    }

    /**
     * Returns the index after the last digit in a sequence of ASCII characters.
     * Leading whitespaces must have been skipped before to invoke this method.
     */
    static int endOfDigits(final CharSequence reference, int i, final int end) {
        while (i < end) {
            final char c = reference.charAt(i);     // Code-point API not needed here because we restrict to ASCII.
            if (c < '0' || c > '9') {               // Do not use Character.isDigit(…) because we restrict to ASCII.
                break;
            }
            i++;
        }
        return i;
    }

    /**
     * Parses part of the given character sequence as an integer.
     *
     * @param  reference  the MGRS reference to parse.
     * @param  start      index of the first character to parse as an integer.
     * @param  end        index after the last character to parse as an integer.
     * @param  errorKey   {@link Resources.Keys} value to use in case of error.
     *                    The error message string shall accept exactly one argument.
     * @return the parsed integer.
     * @throws GazetteerException if the string can not be parsed as an integer.
     */
    static int parseInt(final CharSequence reference, final int start, final int end, final short errorKey)
            throws GazetteerException
    {
        NumberFormatException cause = null;
        final CharSequence part;
        if (start == end) {
            part = CharSequences.token(reference, start);
        } else {
            part = reference.subSequence(start, end);
            try {
                return Integer.parseInt(part.toString());
            } catch (NumberFormatException e) {
                cause = e;
            }
        }
        throw new GazetteerException(Resources.format(errorKey, part), cause);
    }

    /**
     * Parses part of the given character sequence as a grid coordinate.
     * The resolution is determined by the amount of digits.
     *
     * @param  reference  the MGRS reference to parse.
     * @param  start      index of the first character to parse as a grid coordinate.
     * @param  end        index after the last character to parse as a grid coordinate
     * @return the parsed grid coordinate (also referred to as rectangular coordinates).
     * @throws GazetteerException if the string can not be parsed as a grid coordinate.
     */
    static double parseCoordinate(final CharSequence reference, final int start, final int end) throws GazetteerException {
        return parseInt(reference, start, end, Resources.Keys.IllegalGridCoordinate_1)
                * MathFunctions.pow10(METRE_PRECISION_DIGITS - (end - start));
    }




    /**
     * Conversions from direct positions to Military Grid Reference System (MGRS) references.
     * Each {@code Encoder} instance is configured for one {@code DirectPosition} CRS.
     * If a position is given in another CRS, another {@code Encoder} instance must be created.
     *
     * <div class="section">Immutability and thread safety</div>
     * This class is <strong>not</strong> thread-safe. A new instance must be created for each thread,
     * or synchronization must be applied by the caller.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.8
     * @version 0.8
     * @module
     *
     * @see <a href="https://en.wikipedia.org/wiki/Military_Grid_Reference_System">Military Grid Reference System on Wikipedia</a>
     */
    static final class Encoder {
        /**
         * Special {@link #crsZone} value for the UPS South (Universal Polar Stereographic) projection.
         */
        private static final int SOUTH_POLE = -1000;

        /**
         * Special {@link #crsZone} value for the UPS North (Universal Polar Stereographic) projection.
         */
        private static final int NORTH_POLE = 1000;

        /**
         * The datum to which to transform the coordinate before formatting the MGRS reference.
         * Only the datums enumerated in {@link CommonCRS} are currently supported.
         */
        private final CommonCRS datum;

        /**
         * UTM zone of position CRS (negative for South hemisphere), or {@value #NORTH_POLE} or {@value #SOUTH_POLE}
         * if the CRS is a Universal Polar Stereographic projection, or 0 if the CRS is not a recognized projection.
         * Note that this is not necessarily the same zone than the one to use for formatting any given coordinate in
         * that projected CRS, since the {@link #zone(double, char)} method has special rules for some latitudes.
         */
        private final int crsZone;

        /**
         * Coordinate conversion from the position CRS to a CRS of the same type but with normalized axes,
         * or {@code null} if not needed. After conversion, axis directions will be (East, North) and axis
         * units will be metres.
         *
         * <p>This transform should perform only simple operations like swapping axis order and unit conversions.
         * It should not perform more complex operations that would require to go back to geographic coordinates.</p>
         */
        private final MathTransform toNormalized;

        /**
         * Coordinate conversion or transformation from the <em>normalized</em> CRS to geographic CRS.
         * Axis directions are (North, East) as in EPSG geodetic dataset and axis units are degrees.
         * This transform is never {@code null}.
         *
         * <p>This transform may contain datum change from the position datum to the target {@link #datum}.</p>
         */
        private final MathTransform toGeographic;

        /**
         * A transform from a position in the CRS given at construction time to a position in the CRS identified by
         * {@link #actualZone}. This field is updated only when a given position is not located in the zone of the
         * CRS given at construction time.
         */
        private MathTransform toActualZone;

        /**
         * The actual zone where the position to encode is located. Legal values are the same than {@link #crsZone}.
         * If non-zero, then this is the zone of the {@link #toActualZone} transform. This field is updated only when
         * a given position is not located in the zone of the CRS given at construction time.
         */
        private int actualZone;

        /**
         * Creates a new converter from direct positions to MGRS references.
         *
         * @param  datum  the datum to which to transform the coordinate before formatting the MGRS reference,
         *                or {@code null} for inferring the datum from the given {@code crs}.
         * @param  crs    the coordinate reference system of the coordinates for which to create MGRS references.
         * @throws IllegalArgumentException if the given CRS do not use one of the supported datums.
         */
        Encoder(CommonCRS datum, CoordinateReferenceSystem crs) throws FactoryException, TransformException {
            CoordinateReferenceSystem horizontal = CRS.getHorizontalComponent(crs);
            if (horizontal == null) {
                horizontal = crs;
            }
            if (datum == null) {
                datum = CommonCRS.forDatum(horizontal);
            }
            this.datum = datum;
            if (horizontal instanceof ProjectedCRS) {
                ProjectedCRS  projCRS = (ProjectedCRS) horizontal;
                Projection projection = projCRS.getConversionFromBase();
                final OperationMethod method = projection.getMethod();
                if (IdentifiedObjects.isHeuristicMatchForName(method, TransverseMercator.NAME)) {
                    crsZone = TransverseMercator.Zoner.UTM.zone(projection.getParameterValues());
                } else if (IdentifiedObjects.isHeuristicMatchForName(method, PolarStereographicA.NAME)) {
                    crsZone = NORTH_POLE * PolarStereographicA.isUPS(projection.getParameterValues());
                } else {
                    crsZone = 0;                                    // Neither UTM or UPS projection.
                }
                if (crsZone != 0) {
                    /*
                     * Usually, the projected CRS already has (E,N) axis orientations with metres units,
                     * so we let 'toNormalized' to null. In the rarer cases where the CRS axes do not
                     * have the expected orientations and units, then we build a normalized version of
                     * that CRS and compute the transformation to that CRS.
                     */
                    final DefaultProjectedCRS userAxisOrder = DefaultProjectedCRS.castOrCopy(projCRS);
                    projCRS = userAxisOrder.forConvention(AxesConvention.NORMALIZED);
                    if (crs != horizontal || projCRS != userAxisOrder) {
                        toNormalized = CRS.findOperation(crs, projCRS, null).getMathTransform();
                        projection   = projCRS.getConversionFromBase();
                        horizontal   = projCRS;
                        crs          = projCRS;         // Next step in the chain of transformations.
                    } else {
                        toNormalized = null;            // ProjectedCRS (UTM or UPS) is already normalized.
                    }
                } else {
                    toNormalized = null;    // ProjectedCRS is neither UTM or UPS — will need full reprojection.
                }
                /*
                 * We will also need the transformation from the normalized projected CRS to latitude and
                 * longitude (in that order) in degrees. We can get this transform directly from the
                 * projected CRS if its base CRS already has the expected axis orientations and units.
                 */
                if (crs == horizontal && Utilities.equalsIgnoreMetadata(projCRS.getBaseCRS(), datum.geographic())) {
                    toGeographic = projection.getMathTransform().inverse();
                    return;
                }
            } else {
                crsZone      = 0;
                toNormalized = null;
            }
            toGeographic = CRS.findOperation(crs, datum.geographic(), null).getMathTransform();
        }

        /**
         * Returns the band letter for the given latitude. It is caller responsibility to ensure that the
         * given latitude is between {@value #UTM_SOUTH_BOUNDS} and {@value #UTM_NORTH_BOUNDS} inclusive.
         * The returned letter will be one of {@code "CDEFGHJKLMNPQRSTUVWX"} (note that I and O letters
         * are excluded). All bands are 8° height except the X band which is 12° height.
         *
         * @param  φ  the latitude in degrees for which to get the band letter.
         * @return the band letter for the given latitude.
         */
        static char latitudeBand(final double φ) {
            int band = 'C' + (int) ((φ - TransverseMercator.Zoner.SOUTH_BOUNDS) / LATITUDE_BAND_HEIGHT);
            if (band >= EXCLUDE_I && ++band >= EXCLUDE_O && ++band == 'Y') {
                band = 'X';         // Because the last latitude band ('X') is 12° height instead of 8°.
            }
            assert band >= 'C' && band <= 'X' : band;
            return (char) band;
        }

        /**
         * Encodes the given position into a MGRS reference. It is caller responsibility to ensure that
         * the position CRS is the same than the CRS specified at this {@code Encoder} creation time.
         *
         * @param  owner      the {@code Coder} which own this {@code Encoder}.
         * @param  position   the direct position to format as a MGRS reference.
         * @param  separator  the separator to insert between each component of the MGRS identifier.
         * @param  digits     number of digits to use for formatting the numerical part of a MGRS reference.
         * @return the value of {@code buffer.toString()}.
         */
        String encode(final MilitaryGridReferenceSystem.Coder owner, DirectPosition position,
                final String separator, final int digits) throws FactoryException, TransformException
        {
            final StringBuilder buffer = owner.buffer;
            if (toNormalized != null) {
                owner.normalized = position = toNormalized.transform(position, owner.normalized);
            }
            final DirectPosition geographic = toGeographic.transform(position, owner.geographic);
            owner.geographic = geographic;                      // For reuse in next method calls.
            final double φ = geographic.getOrdinate(0);
            if (φ >= TransverseMercator.Zoner.SOUTH_BOUNDS &&
                φ <  TransverseMercator.Zoner.NORTH_BOUNDS)
            {
                /*
                 * Universal Transverse Mercator (UTM) case.
                 */
                final double λ = geographic.getOrdinate(1);
                final int zone = TransverseMercator.Zoner.UTM.zone(φ, λ);
                final int sz   = MathFunctions.isNegative(φ) ? -zone : zone;
                if (sz == 0) {
                    // Zero value at this point is the result of NaN of infinite ordinate value.
                    throw new GazetteerException(Errors.format(Errors.Keys.NotANumber_1, "longitude"));
                }
                if (sz != crsZone) {
                    if (sz != actualZone) {
                        actualZone   = 0;                           // In case an exception is thrown on the next line.
                        toActualZone = CRS.findOperation(datum.geographic(), datum.universal(φ, λ), null).getMathTransform();
                        actualZone   = sz;
                    }
                    owner.normalized = position = toActualZone.transform(geographic, owner.normalized);
                }
                buffer.setLength(0);
                buffer.append(zone).append(separator).append(latitudeBand(φ));
                if (digits >= 0) {
                    /*
                     * Specification said that 100,000-meters columns are lettered from A through Z (omitting I and O)
                     * starting at the 180° meridian, proceeding easterly for 18°, and repeating for each 18° intervals.
                     * Since a UTM zone is 6° width, a 18° interval is exactly 3 standard UTM zones. Columns in zone 1
                     * are A-H, zone 2 are J-R (skipping O), zone 3 are S-Z, then repeating every 3 zones.
                     */
                    final double x = position.getOrdinate(0);
                    final double y = position.getOrdinate(1);
                    final double cx = Math.floor(x / GRID_SQUARE_SIZE);
                    final double cy = Math.floor(y / GRID_SQUARE_SIZE);
                    int col = (int) cx;
                    if (col < 1 || col > 8) {
                        /*
                         * UTM northing values at the equator range from 166021 to 833979 meters approximatively
                         * (WGS84 ellipsoid). Consequently 'cx' ranges from approximatively 1.66 to 8.34, so 'c'
                         * should range from 1 to 8.
                         */
                        throw new GazetteerException(Errors.format(Errors.Keys.OutsideDomainOfValidity));
                    }
                    switch (zone % 3) {                          // First A-H sequence starts at zone number 1.
                        case 1: col += ('A' - 1); break;
                        case 2: col += ('J' - 1); if (col >= EXCLUDE_O) col++; break;
                        case 0: col += ('S' - 1); break;
                    }
                    /*
                     * Rows in odd  zones are ABCDEFGHJKLMNPQRSTUV
                     * Rows in even zones are FGHJKLMNPQRSTUVABCDE
                     * Those 20 letters are repeated in a cycle.
                     */
                    int row = (int) cy;
                    if ((zone & 1) == 0) {
                        row += ('F' - 'A');
                    }
                    row = 'A' + (row % GRID_ROW_COUNT);
                    if (row >= EXCLUDE_I && ++row >= EXCLUDE_O) row++;
                    buffer.append(separator).append((char) col).append((char) row);
                    /*
                     * Numerical location at the given precision.
                     * The specification requires us to truncate the number, not to round it.
                     */
                    if (digits > 0) {
                        final double precision = MathFunctions.pow10(METRE_PRECISION_DIGITS - digits);
                        append(buffer.append(separator), (int) ((x - cx * GRID_SQUARE_SIZE) / precision), digits);
                        append(buffer.append(separator), (int) ((y - cy * GRID_SQUARE_SIZE) / precision), digits);
                    }
                }
            } else {
                /*
                 * Universal Polar Stereographic (UPS) case.
                 */
                return null;    // TODO
            }
            return buffer.toString();
        }

        /**
         * Appends the given value in the given buffer, padding with zero digits in order to get
         * the specified total amount of digits.
         */
        private static void append(final StringBuilder buffer, final int value, int digits) throws TransformException {
            if (value >= 0) {
                final int p = buffer.length();
                digits -= (buffer.append(value).length() - p);
                if (digits >= 0) {
                    StringBuilders.repeat(buffer, p, '0', digits);
                    return;
                }
            }
            throw new TransformException(Errors.format(Errors.Keys.OutsideDomainOfValidity));
        }
    }
}
