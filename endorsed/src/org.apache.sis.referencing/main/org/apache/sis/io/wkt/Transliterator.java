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
package org.apache.sis.io.wkt;

import java.util.Map;
import java.io.Serializable;
import java.io.ObjectStreamException;
import org.opengis.referencing.cs.PolarCS;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.AxisDirection;
import org.apache.sis.metadata.internal.shared.AxisNames;
import org.apache.sis.referencing.internal.shared.AxisDirections;
import org.apache.sis.referencing.internal.shared.WKTKeywords;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Characters;
import org.apache.sis.util.OptionalCandidate;


/**
 * Controls the replacement of characters, abbreviations and names between the objects in memory and their
 * WKT representations. The mapping is not necessarily one-to-one, for example the replacement of a Unicode
 * character by an ASCII character may not be reversible. The mapping may also depend on the element to transliterate,
 * for example some Greek letters like φ, λ and θ are mapped differently when they are used as mathematical symbols in
 * axis abbreviations rather than texts. Some mappings may also apply to words instead of characters, when the word
 * come from a controlled vocabulary.
 *
 * <h2>Permitted characters in Well Known Text</h2>
 * The ISO 19162 standard restricts <i>Well Known Text</i> to the following characters in all
 * {@linkplain Formatter#append(String, ElementKind) quoted texts} except in {@code REMARKS["…"]} elements:
 *
 * <blockquote><pre>{@literal A-Z a-z 0-9 _ [ ] ( ) { } < = > . , : ; + - (space) % & ' " * ^ / \ ? | °}</pre></blockquote>
 *
 * They are ASCII codes 32 to 125 inclusive except ! (33), # (35), $ (36), @ (64) and ` (96),
 * plus the addition of ° (176) despite being formally outside the ASCII character set.
 * The only exception to this rules is for the text inside {@code REMARKS["…"]} elements,
 * where all Unicode characters are allowed.
 *
 * <p>The {@link #filter(String)} method is responsible for replacing or removing characters outside the above-cited
 * set of permitted characters.</p>
 *
 * <h2>Application to mathematical symbols</h2>
 * For Greek letters used as mathematical symbols in
 * {@linkplain org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#getAbbreviation() coordinate axis abbreviations},
 * the ISO 19162 standard recommends:
 *
 * <ul>
 *   <li>(<var>P</var>, <var>L</var>) as the transliteration of the Greek letters (<var>phi</var>, <var>lambda</var>), or
 *       (<var>B</var>, <var>L</var>) from German “Breite” and “Länge” used in academic texts worldwide, or
 *       (<var>lat</var>, <var>long</var>).</li>
 *   <li>(<var>U</var>) for (θ) in {@linkplain org.apache.sis.referencing.cs.DefaultPolarCS polar coordinate systems}.</li>
 *   <li>(<var>U</var>, <var>V</var>) for (Ω, θ) in
 *       {@linkplain org.apache.sis.referencing.cs.DefaultSphericalCS spherical coordinate systems}.</li>
 * </ul>
 *
 * The {@link #toLatinAbbreviation toLatinAbbreviation(…)} and {@link #toUnicodeAbbreviation toUnicodeAbbreviation(…)}
 * methods are responsible for doing the transliteration at formatting and parsing time, respectively.
 *
 * <h3>Note on conventions</h3>
 * At least two conventions exist about the meaning of (<var>r</var>, θ, φ) in a
 * spherical coordinate system (see <a href="https://en.wikipedia.org/wiki/Spherical_coordinate_system">Wikipedia</a>
 * or <a href="https://mathworld.wolfram.com/SphericalCoordinates.html">MathWorld</a> for more information).
 * When using the <em>mathematics</em> convention, θ is the azimuthal angle in the
 * equatorial plane (roughly equivalent to longitude λ) while φ is an angle measured from a pole (also known as
 * colatitude). But when using the <em>physics</em> convention, the meaning of θ and φ are interchanged.
 * Furthermore, some other conventions may measure the φ angle from the equatorial plane – like latitude – instead
 * than from the pole. This class does not need to care about the meaning of those angles. The only recommendation
 * is that φ is mapped to <var>U</var> and θ is mapped to <var>V</var>, regardless of their meaning.
 *
 * <h2>Replacement of names</h2>
 * The longitude and latitude axis names are explicitly fixed by ISO 19111:2007 to <q>Geodetic longitude</q>
 * and <q>Geodetic latitude</q>. But ISO 19162:2015 §7.5.3(ii) said that the <q>Geodetic</q> part in
 * those names shall be omitted at WKT formatting time.
 * The {@link #toShortAxisName toShortAxisName(…)} and {@link #toLongAxisName toLongAxisName(…)}
 * methods are responsible for doing the transliteration at formatting and parsing time, respectively.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @see org.apache.sis.util.Characters#isValidWKT(int)
 *
 * @since 0.6
 */
public abstract class Transliterator implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 7115456393795045932L;

    /**
     * A bitmask of control characters that are considered as spaces according {@link Character#isWhitespace(char)}.
     */
    static final int SPACES = 0xF0003E00;

    /**
     * Default names to associate to axis directions in a Cartesian coordinate system.
     * Those names do not apply to other kind of coordinate systems.
     */
    private static final Map<AxisDirection,String> CARTESIAN = Map.of(
            AxisDirection.EAST,         AxisNames.EASTING,
            AxisDirection.WEST,         AxisNames.WESTING,
            AxisDirection.NORTH,        AxisNames.NORTHING,
            AxisDirection.SOUTH,        AxisNames.SOUTHING,
            AxisDirection.GEOCENTRIC_X, AxisNames.GEOCENTRIC_X,
            AxisDirection.GEOCENTRIC_Y, AxisNames.GEOCENTRIC_Y,
            AxisDirection.GEOCENTRIC_Z, AxisNames.GEOCENTRIC_Z);

    /**
     * A transliterator compliant with ISO 19162 on a <q>best effort</q> basis.
     * All methods perform the default implementation documented in this {@code Transliterator} class.
     */
    public static final Transliterator DEFAULT = new Default();

    /**
     * A transliterator that does not perform any replacement.
     * All methods let names, abbreviations and Unicode characters pass-through unchanged.
     */
    public static final Transliterator IDENTITY = new Unicode();

    /**
     * For sub-class constructors.
     */
    protected Transliterator() {
    }

    /**
     * Returns a character sequences with the non-ASCII characters replaced or removed.
     * For example, this method replaces “ç” by “c” in “Triangulation fran<b>ç</b>aise”.
     * This operation is usually not reversible; there is no converse method.
     *
     * <p>Implementations shall not care about {@linkplain Symbols#getOpeningQuote(int) opening} or
     * {@linkplain Symbols#getClosingQuote(int) closing quotes}. The quotes will be doubled by the
     * caller if needed after this method has been invoked.</p>
     *
     * <p>The default implementation invokes {@link CharSequences#toASCII(CharSequence)},
     * replaces line feed and tabulations by single spaces, then remove control characters.</p>
     *
     * @param  text  the text to format without non-ASCII characters.
     * @return the text to write in <i>Well Known Text</i>.
     *
     * @see org.apache.sis.util.Characters#isValidWKT(int)
     */
    public String filter(final String text) {
        CharSequence s = CharSequences.toASCII(text);
        StringBuilder buffer = null;
        for (int i=s.length(); --i >= 0;) {
            final char c = s.charAt(i);
            if (c < 32) {
                if (buffer == null) {
                    if (s == text) {
                        s = buffer = new StringBuilder(text);
                    } else {
                        buffer = (StringBuilder) s;
                    }
                }
                if ((SPACES & (1 << c)) != 0) {
                    buffer.setCharAt(i, ' ');
                    if (i != 0 && c == '\n' && s.charAt(i-1) == '\r') {
                        buffer.deleteCharAt(--i);
                    }
                } else {
                    buffer.deleteCharAt(i);
                }
            }
        }
        return s.toString();
    }

    /**
     * Returns the axis name to format in <abbr>WKT</abbr>, or {@code null} if none.
     * This method performs the mapping between the names of axes in memory (designated by <dfn>long axis names</dfn>
     * in this class) and the names to format in the <abbr>WKT</abbr> (designated by <dfn>short axis names</dfn>).
     * The long axis names are defined by <abbr>ISO</abbr> 19111 — <cite>referencing by coordinates</cite> while
     * the short axis names are defined by <abbr>ISO</abbr> 19162 — <cite>Well-known text representation
     * of coordinate reference systems</cite>.
     *
     * <p>This method returns {@code null} if the name should be omitted. <abbr>ISO</abbr> 19162 recommends
     * to omit the axis name when it is already given through the mandatory axis direction.</p>
     *
     * <p>The default implementation performs at least the following replacements:</p>
     * <ul>
     *   <li>Replace <q>Geodetic latitude</q> (case insensitive) by <q>Latitude</q>.</li>
     *   <li>Replace <q>Geodetic longitude</q> (case insensitive) by <q>Longitude</q>.</li>
     *   <li>Return {@code null} if the axis direction is {@link AxisDirection#GEOCENTRIC_X}, {@code GEOCENTRIC_Y}
     *       or {@code GEOCENTRIC_Z} and the name is the same as the axis direction (ignoring case).</li>
     * </ul>
     *
     * @param  cs         the enclosing coordinate system, or {@code null} if unknown.
     * @param  direction  the direction of the axis to format.
     * @param  name       the axis name, to be eventually replaced by this method.
     * @return the axis name to format, or {@code null} if the name shall be omitted.
     *
     * @see org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#formatTo(Formatter)
     */
    @OptionalCandidate
    public String toShortAxisName(final CoordinateSystem cs, final AxisDirection direction, final String name) {
        if (name.equalsIgnoreCase(AxisNames.GEODETIC_LATITUDE) ||               // ISO 19162:2015 §7.5.3(ii)
            name.equalsIgnoreCase(AxisNames.PLANETODETIC_LATITUDE))
        {
            return AxisNames.LATITUDE;
        }
        if (name.equalsIgnoreCase(AxisNames.GEODETIC_LONGITUDE) ||
            name.equalsIgnoreCase(AxisNames.PLANETODETIC_LONGITUDE))
        {
            return AxisNames.LONGITUDE;
        }
        if (AxisDirections.isGeocentric(direction) && CharSequences.equalsFiltered(
                name, direction.name(), Characters.Filter.LETTERS_AND_DIGITS, true))
        {
            return null;
        }
        return name;
    }

    /**
     * Returns the axis name to use in memory for an axis parsed from a WKT.
     * Since this method is invoked before the {@code CoordinateSystem} instance is created,
     * most coordinate system characteristics are known only as {@code String}.
     * In particular the {@code csType} argument, if non-null, should be one of the following values:
     *
     * <blockquote>{@code "affine"}, {@code "Cartesian"} (note the upper-case {@code "C"}), {@code "cylindrical"},
     * {@code "ellipsoidal"}, {@code "linear"}, {@code "parametric"}, {@code "polar"}, {@code "spherical"},
     * {@code "temporal"} or {@code "vertical"}</blockquote>
     *
     * This method is the converse of {@link #toShortAxisName(CoordinateSystem, AxisDirection, String)}.
     * The default implementation performs at least the following replacements:
     * <ul>
     *   <li>Replace <q>Lat</q> or <q>Latitude</q>
     *       (case insensitive) by <q>Geodetic latitude</q> or <q>Spherical latitude</q>,
     *       depending on whether the axis is part of an ellipsoidal or spherical CS respectively.</li>
     *   <li>Replace <q>Lon</q>, <q>Long</q> or <q>Longitude</q>
     *       (case insensitive) by <q>Geodetic longitude</q> or <q>Spherical longitude</q>,
     *       depending on whether the axis is part of an ellipsoidal or spherical CS respectively.</li>
     *   <li>Return <q>Geocentric X</q>, <q>Geocentric Y</q> and <q>Geocentric Z</q>
     *       for {@link AxisDirection#GEOCENTRIC_X}, {@link AxisDirection#GEOCENTRIC_Y GEOCENTRIC_Y}
     *       and {@link AxisDirection#GEOCENTRIC_Z GEOCENTRIC_Z} respectively in a Cartesian CS,
     *       if the given axis name is only an abbreviation.</li>
     *   <li>Use unique camel-case names for axis names defined by ISO 19111 and ISO 19162. For example, this method
     *       replaces <q><b>e</b>llipsoidal height</q> by <q><b>E</b>llipsoidal height</q>.</li>
     * </ul>
     *
     * <h4>Usage note</h4>
     * Axis names are not really free text. They are specified by ISO 19111 and ISO 19162.
     * SIS does not put restriction on axis names, but we nevertheless try to use a unique
     * name when we recognize it.
     *
     * @param  csType     the type of the coordinate system, or {@code null} if unknown.
     * @param  direction  the parsed axis direction.
     * @param  name       the parsed axis abbreviation, to be eventually replaced by this method.
     * @return the axis name to use. Cannot be null.
     */
    public String toLongAxisName(final String csType, final AxisDirection direction, final String name) {
        if (csType != null) switch (csType) {
            case WKTKeywords.ellipsoidal: {
                if (isLatLong(AxisNames.LATITUDE,  name)) return AxisNames.GEODETIC_LATITUDE;
                if (isLatLong(AxisNames.LONGITUDE, name)) return AxisNames.GEODETIC_LONGITUDE;
                break;
            }
            case WKTKeywords.spherical: {
                if (isLatLong(AxisNames.LATITUDE,  name)) return AxisNames.SPHERICAL_LATITUDE;
                if (isLatLong(AxisNames.LONGITUDE, name)) return AxisNames.SPHERICAL_LONGITUDE;
                break;
            }
            case WKTKeywords.Cartesian: {
                if (name.length() <= 1) {
                    final String c = CARTESIAN.get(direction);
                    if (c != null) {
                        return c;
                    }
                }
                break;
            }
        }
        return AxisNames.toCamelCase(name);
    }

    /**
     * Returns {@code true} if the given axis name is at least part of the given expected axis name.
     *
     * @param  expected  {@link AxisNames#LATITUDE} or {@link AxisNames#LONGITUDE}.
     * @param  name      the parsed axis name.
     */
    private static boolean isLatLong(final String expected, final String name) {
        final int length = name.length();
        return (length >= 3) && (length <= name.length()) && CharSequences.startsWith(expected, name, true);
    }

    /**
     * Returns the axis abbreviation to format in WKT, or {@code null} if none. The given abbreviation may contain
     * Greek letters, in particular φ, λ and θ. This {@code toLatinAbbreviation(…)} method is responsible
     * for replacing Greek letters by Latin letters for ISO 19162 compliance, if desired.
     *
     * <p>The default implementation performs at least the following mapping:</p>
     * <ul>
     *   <li>λ → <var>L</var> (from German <i>Länge</i>) if used in an
     *       {@linkplain org.apache.sis.referencing.cs.DefaultEllipsoidalCS ellipsoidal CS}.</li>
     *   <li>φ → <var>B</var> (from German <i>Breite</i>) if used in an
     *       {@linkplain org.apache.sis.referencing.cs.DefaultEllipsoidalCS ellipsoidal CS}.</li>
     *   <li>φ or φ′ or φ<sub>c</sub> or Ω → <var>U</var> if used in a
     *       {@linkplain org.apache.sis.referencing.cs.DefaultSphericalCS spherical CS}, regardless of whether the
     *       coordinate system follows <a href="https://en.wikipedia.org/wiki/Spherical_coordinate_system">physics,
     *       mathematics or other conventions</a>.</li>
     *   <li>θ → <var>V</var> if used in a {@linkplain org.apache.sis.referencing.cs.DefaultSphericalCS spherical CS} (regardless of above-cited coordinate system convention).</li>
     *   <li>θ → <var>U</var> if used in a polar CS.</li>
     * </ul>
     *
     * Note that while this method may return a string of any length, ISO 19162 requires abbreviations
     * to be a single Latin character.
     *
     * @param  cs            the enclosing coordinate system, or {@code null} if unknown.
     * @param  direction     the direction of the axis to format.
     * @param  abbreviation  the axis abbreviation, to be eventually replaced by this method.
     * @return the axis abbreviation to format.
     *
     * @see org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#formatTo(Formatter)
     */
    public String toLatinAbbreviation(final CoordinateSystem cs, final AxisDirection direction, String abbreviation) {
        if (abbreviation != null && !abbreviation.isEmpty()) {
            if (abbreviation.length() <= 2) {
                switch (abbreviation.charAt(0)) {
                    /*
                     * ISO 19162:2015 §7.5.3 recommendations:
                     *
                     *   a) For PolarCS using Greek letter θ for direction, the letter ‘U’ should be used in WKT.
                     *   b) For SphericalCS using φ and θ, the letter ‘U’ and ‘V’ respectively should be used in WKT.
                     */
                    case 'θ': {
                        if  (cs instanceof SphericalCS) abbreviation ="V";
                        else if (cs instanceof PolarCS) abbreviation ="U";
                        break;
                    }
                    /*
                     * ISO 19162:2015 §7.5.3 requirement (ii) and recommendation (b):
                     *
                     *  ii) Greek letters φ and λ for geodetic latitude and longitude must be replaced by Latin char.
                     *   b) For SphericalCS using φ and θ, the letter ‘U’ and ‘V’ respectively should be used in WKT.
                     *
                     * Note that some SphericalCS may use φ′ or φc for distinguishing from geodetic latitude φ.
                     */
                    case 'φ': {
                        if (cs instanceof SphericalCS) {
                            abbreviation = "U";
                        } else if (cs instanceof EllipsoidalCS) {
                            abbreviation = "B";    // From German "Breite", used in academic texts worldwide.
                        }
                        break;
                    }
                    case 'Ω': {                    // Used instead of 'φ' in ISO 19111.
                        if (cs instanceof SphericalCS) {
                            abbreviation = "U";
                        }
                        break;
                    }
                    case 'λ': {
                        if (cs instanceof EllipsoidalCS) {
                            abbreviation = "L";    // From German "Länge", used in academic texts worldwide.
                        }
                        break;
                    }
                }
            } else {
                if (abbreviation.equalsIgnoreCase("Lat")) {
                    abbreviation = "B";
                } else if (abbreviation.regionMatches(true, 0, "Long", 0,
                        Math.min(3, Math.max(4, abbreviation.length()))))   // Accept "Lon" or "Long".
                {
                    abbreviation = "L";
                }
            }
        }
        return abbreviation;
    }

    /**
     * Returns the axis abbreviation to use in memory for an axis parsed from a WKT.
     * Since this method is invoked before the {@code CoordinateSystem} instance is created,
     * most coordinate system characteristics are known only as {@code String}.
     * In particular the {@code csType} argument, if non-null, should be one of the following values:
     *
     * <blockquote>{@code "affine"}, {@code "Cartesian"} (note the upper-case {@code "C"}), {@code "cylindrical"},
     * {@code "ellipsoidal"}, {@code "linear"}, {@code "parametric"}, {@code "polar"}, {@code "spherical"},
     * {@code "temporal"} or {@code "vertical"}</blockquote>
     *
     * This method is the converse of {@link #toLatinAbbreviation(CoordinateSystem, AxisDirection, String)}.
     * The default implementation performs at least the following mapping:
     * <ul>
     *   <li><var>P</var> or <var>L</var> → λ if {@code csType} is {@code "ellipsoidal"}.</li>
     *   <li><var>B</var> → φ  if {@code csType} is {@code "ellipsoidal"}.</li>
     *   <li><var>U</var> → Ω  if {@code csType} is {@code "spherical"}, regardless of coordinate system convention.</li>
     *   <li><var>V</var> → θ  if {@code csType} is {@code "spherical"}, regardless of coordinate system convention.</li>
     *   <li><var>U</var> → θ  if {@code csType} is {@code "polar"}.</li>
     * </ul>
     *
     * @param  csType        the type of the coordinate system, or {@code null} if unknown.
     * @param  direction     the parsed axis direction.
     * @param  abbreviation  the parsed axis abbreviation, to be eventually replaced by this method.
     * @return the axis abbreviation to use. Cannot be null.
     */
    public String toUnicodeAbbreviation(final String csType, final AxisDirection direction, String abbreviation) {
        if (abbreviation.length() == 1) {
            final String replacement;
            final String condition;
            switch (abbreviation.charAt(0)) {
                case 'U': if (WKTKeywords.polar.equals(csType)) return "θ";
                          replacement = "Ω";  condition = WKTKeywords.spherical;   break;
                case 'V': replacement = "θ";  condition = WKTKeywords.spherical;   break;
                case 'L': replacement = "λ";  condition = WKTKeywords.ellipsoidal; break;
                case 'P': // Transliteration of "phi".
                case 'B': replacement = "φ";  condition = WKTKeywords.ellipsoidal; break;
                default: return abbreviation;
            }
            if (condition.equals(csType)) {
                return replacement;
            }
        } else {
            if (isLatLong(AxisNames.LATITUDE,  abbreviation)) return "φ";
            if (isLatLong(AxisNames.LONGITUDE, abbreviation)) return "λ";
        }
        return abbreviation;
    }

    /**
     * The {@link Transliterator#DEFAULT} implementation.
     */
    private static final class Default extends Transliterator {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 4869597020294928525L;

        /** Returns a string representation similar to enum. */
        @Override public String toString() {
            return "DEFAULT";
        }

        /** Replaces deserialized instances by the unique instance. */
        Object readResolve() throws ObjectStreamException {
            return DEFAULT;
        }
    }

    /**
     * The {@link Transliterator#IDENTITY} implementation.
     */
    private static final class Unicode extends Transliterator {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 7392131912748253956L;

        /** Performs no replacement. */
        @Override public String filter(String text) {
            return text;
        }

        /** Returns the axis name as-is. */
        @Override public String toShortAxisName(CoordinateSystem cs, AxisDirection direction, String name) {
            return name;
        }

        /** Returns the axis name as-is. */
        @Override public String toLongAxisName(String csType, AxisDirection direction, String name) {
            return name;
        }

        /** Returns the abbreviation as-is. */
        @Override public String toLatinAbbreviation(CoordinateSystem cs, AxisDirection direction, String abbreviation) {
            return abbreviation;
        }

        /** Returns the abbreviation as-is. */
        @Override
        public String toUnicodeAbbreviation(String csType, AxisDirection direction, String abbreviation) {
            return abbreviation;
        }

        /** Returns a string representation similar to enum. */
        @Override public String toString() {
            return "IDENTITY";
        }

        /** Replaces deserialized instances by the unique instance. */
        Object readResolve() throws ObjectStreamException {
            return IDENTITY;
        }
    }
}
