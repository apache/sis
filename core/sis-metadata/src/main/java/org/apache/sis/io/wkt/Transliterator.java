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
import java.util.HashMap;
import java.io.Serializable;
import org.opengis.referencing.cs.PolarCS;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.AxisDirection;
import org.apache.sis.internal.metadata.AxisDirections;
import org.apache.sis.internal.metadata.AxisNames;
import org.apache.sis.internal.metadata.WKTKeywords;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Characters;


/**
 * Controls the replacement of characters, abbreviations and names between the objects in memory and their
 * WKT representations. The mapping is not necessarily one-to-one, for example the replacement of a Unicode
 * character by an ASCII character may not be reversible. The mapping may also depend on the element to transliterate,
 * for example some Greek letters like φ, λ and θ are mapped differently when they are used as mathematical symbols in
 * axis abbreviations rather than texts. Some mappings may also apply to words instead than characters, when the word
 * come from a controlled vocabulary.
 *
 * <div class="section">Permitted characters in Well Known Text</div>
 * The ISO 19162 standard restricts <cite>Well Known Text</cite> to the following characters in all
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
 * <div class="section">Application to mathematical symbols</div>
 * For Greek letters used as mathematical symbols in
 * {@linkplain org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#getAbbreviation() coordinate axis abbreviations},
 * the ISO 19162 standard recommends:
 *
 * <ul>
 *   <li>(<var>P</var>, <var>L</var>) as the transliteration of the Greek letters (<var>phi</var>, <var>lambda</var>), or
 *       (<var>B</var>, <var>L</var>) from German “Breite” and “Länge” used in academic texts worldwide, or
 *       (<var>lat</var>, <var>long</var>).</li>
 *   <li>(<var>U</var>) for (θ) in {@linkplain org.apache.sis.referencing.cs.DefaultPolarCS polar coordinate systems}.</li>
 *   <li>(<var>U</var>, <var>V</var>) for (φ′, θ) in
 *       {@linkplain org.apache.sis.referencing.cs.DefaultSphericalCS spherical coordinate systems}.</li>
 * </ul>
 *
 * <div class="note"><b>Note:</b> at least two conventions exist about the meaning of (<var>r</var>, θ, φ) in a
 * spherical coordinate system (see <a href="http://en.wikipedia.org/wiki/Spherical_coordinate_system">Wikipedia</a>
 * or <a href="http://mathworld.wolfram.com/SphericalCoordinates.html">MathWorld</a> for more information).
 * When using the <em>mathematics</em> convention, θ is the azimuthal angle in the
 * equatorial plane (roughly equivalent to longitude λ) while φ is an angle measured from a pole (also known as
 * colatitude). But when using the <em>physics</em> convention, the meaning of θ and φ are interchanged.
 * Furthermore some other conventions may measure the φ angle from the equatorial plane – like latitude – instead
 * than from the pole. This class does not need to care about the meaning of those angles. The only recommendation
 * is that φ is mapped to <var>U</var> and θ is mapped to <var>V</var>, regardless of their meaning.</div>
 *
 * The {@link #toLatinAbbreviation toLatinAbbreviation(…)} and {@link #toUnicodeAbbreviation toUnicodeAbbreviation(…)}
 * methods are responsible for doing the transliteration at formatting and parsing time, respectively.
 *
 * <div class="section">Replacement of names</div>
 * The longitude and latitude axis names are explicitly fixed by ISO 19111:2007 to <cite>"Geodetic longitude"</cite>
 * and <cite>"Geodetic latitude"</cite>. But ISO 19162:2015 §7.5.3(ii) said that the <cite>"Geodetic"</cite> part in
 * those names shall be omitted at WKT formatting time.
 * The {@link #toShortAxisName toShortAxisName(…)} and {@link #toLongAxisName toLongAxisName(…)}
 * methods are responsible for doing the transliteration at formatting and parsing time, respectively.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 *
 * @see org.apache.sis.util.Characters#isValidWKT(int)
 * @see <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#39">WKT 2 specification §7.5.3</a>
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
     *
     * <p>For thread safety reasons, this map shall not be modified after construction.</p>
     */
    private static final Map<AxisDirection,String> CARTESIAN;
    static {
        final Map<AxisDirection,String> m = new HashMap<AxisDirection,String>(12);
        m.put(AxisDirection.EAST,         AxisNames.EASTING);
        m.put(AxisDirection.WEST,         AxisNames.WESTING);
        m.put(AxisDirection.NORTH,        AxisNames.NORTHING);
        m.put(AxisDirection.SOUTH,        AxisNames.SOUTHING);
        m.put(AxisDirection.GEOCENTRIC_X, AxisNames.GEOCENTRIC_X);
        m.put(AxisDirection.GEOCENTRIC_Y, AxisNames.GEOCENTRIC_Y);
        m.put(AxisDirection.GEOCENTRIC_Z, AxisNames.GEOCENTRIC_Z);
        CARTESIAN = m;
    }

    /**
     * A transliterator compliant with ISO 19162 on a <cite>"best effort"</cite> basis.
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
     * For example this method replaces “ç” by “c” in “Triangulation fran<b>ç</b>aise”.
     * This operation is usually not reversible; there is no converse method.
     *
     * <p>Implementations shall not care about {@linkplain Symbols#getOpeningQuote(int) opening} or
     * {@linkplain Symbols#getClosingQuote(int) closing quotes}. The quotes will be doubled by the
     * caller if needed after this method has been invoked.</p>
     *
     * <p>The default implementation invokes {@link CharSequences#toASCII(CharSequence)},
     * replaces line feed and tabulations by single spaces, then remove control characters.</p>
     *
     * @param  text The text to format without non-ASCII characters.
     * @return The text to write in <cite>Well Known Text</cite>.
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
     * Returns the axis name to format in WKT, or {@code null} if none. This method performs the mapping
     * between the names of axes in memory (designated by <cite>"long axis names"</cite> in this class)
     * and the names to format in the WKT (designated by <cite>"short axis names"</cite>).
     *
     * <div class="note"><b>Note:</b>
     * the <cite>"long axis names"</cite> are defined by ISO 19111 — <cite>referencing by coordinates</cite>
     * while the <cite>"short axis names"</cite> are defined by ISO 19162 — <cite>Well-known text representation
     * of coordinate reference systems</cite>.</div>
     *
     * This method can return {@code null} if the name should be omitted.
     * ISO 19162 recommends to omit the axis name when it is already given through the mandatory axis direction.
     *
     * <p>The default implementation performs at least the following replacements:</p>
     * <ul>
     *   <li>Replace <cite>“Geodetic latitude”</cite> (case insensitive) by <cite>“Latitude”</cite>.</li>
     *   <li>Replace <cite>“Geodetic longitude”</cite> (case insensitive) by <cite>“Longitude”</cite>.</li>
     *   <li>Return {@code null} if the axis direction is {@link AxisDirection#GEOCENTRIC_X}, {@code GEOCENTRIC_Y}
     *       or {@code GEOCENTRIC_Z} and the name is the same than the axis direction (ignoring case).</li>
     * </ul>
     *
     * @param  cs        The enclosing coordinate system, or {@code null} if unknown.
     * @param  direction The direction of the axis to format.
     * @param  name      The axis name, to be eventually replaced by this method.
     * @return The axis name to format, or {@code null} if the name shall be omitted.
     *
     * @see org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#formatTo(Formatter)
     */
    public String toShortAxisName(final CoordinateSystem cs, final AxisDirection direction, final String name) {
        if (name.equalsIgnoreCase(AxisNames.GEODETIC_LATITUDE )) return AxisNames.LATITUDE;  // ISO 19162:2015 §7.5.3(ii)
        if (name.equalsIgnoreCase(AxisNames.GEODETIC_LONGITUDE)) return AxisNames.LONGITUDE;
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
     *   <li>Replace <cite>“Lat”</cite> or <cite>“Latitude”</cite>
     *       (case insensitive) by <cite>“Geodetic latitude”</cite> or <cite>“Spherical latitude”</cite>,
     *       depending on whether the axis is part of an ellipsoidal or spherical CS respectively.</li>
     *   <li>Replace <cite>“Lon”</cite>, <cite>“Long”</cite> or <cite>“Longitude”</cite>
     *       (case insensitive) by <cite>“Geodetic longitude”</cite> or <cite>“Spherical longitude”</cite>,
     *       depending on whether the axis is part of an ellipsoidal or spherical CS respectively.</li>
     *   <li>Return <cite>“Geocentric X”</cite>, <cite>“Geocentric Y”</cite> and <cite>“Geocentric Z”</cite>
     *       for {@link AxisDirection#GEOCENTRIC_X}, {@link AxisDirection#GEOCENTRIC_Y GEOCENTRIC_Y}
     *       and {@link AxisDirection#GEOCENTRIC_Z GEOCENTRIC_Z} respectively in a Cartesian CS,
     *       if the given axis name is only an abbreviation.</li>
     *   <li>Use unique camel-case names for axis names defined by ISO 19111 and ISO 19162. For example this method
     *       replaces <cite>“<b>e</b>llipsoidal height”</cite> by <cite>“<b>E</b>llipsoidal height”</cite>.</li>
     * </ul>
     *
     * <div class="note"><b>Rational:</b>
     * Axis names are not really free text. They are specified by ISO 19111 and ISO 19162.
     * SIS does not put restriction on axis names, but we nevertheless try to use a unique
     * name when we recognize it.</div>
     *
     * @param  csType    The type of the coordinate system, or {@code null} if unknown.
     * @param  direction The parsed axis direction.
     * @param  name      The parsed axis abbreviation, to be eventually replaced by this method.
     * @return The axis name to use. Can not be null.
     */
    public String toLongAxisName(final String csType, final AxisDirection direction, final String name) {
        if (csType != null) /*switch (csType)*/ {
            if (csType.equals(WKTKeywords.ellipsoidal)) {
                if (isLatLong(AxisNames.LATITUDE,  name)) return AxisNames.GEODETIC_LATITUDE;
                if (isLatLong(AxisNames.LONGITUDE, name)) return AxisNames.GEODETIC_LONGITUDE;
            }
            else if (csType.equals(WKTKeywords.spherical)) {
                if (isLatLong(AxisNames.LATITUDE,  name)) return AxisNames.SPHERICAL_LATITUDE;
                if (isLatLong(AxisNames.LONGITUDE, name)) return AxisNames.SPHERICAL_LONGITUDE;
            }
            else if (csType.equals(WKTKeywords.Cartesian)) {
                if (name.length() <= 1) {
                    final String c = CARTESIAN.get(direction);
                    if (c != null) {
                        return c;
                    }
                }
            }
        }
        return AxisNames.toCamelCase(name);
    }

    /**
     * Returns {@code true} if the given axis name is at least part of the given expected axis name.
     *
     * @param expected {@link AxisNames#LATITUDE} or {@link AxisNames#LONGITUDE}.
     * @param name The parsed axis name.
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
     *   <li>λ → <var>L</var> (from German <cite>Länge</cite>) if used in an
     *       {@linkplain org.apache.sis.referencing.cs.DefaultEllipsoidalCS ellipsoidal CS}.</li>
     *   <li>φ → <var>B</var> (from German <cite>Breite</cite>) if used in an
     *       {@linkplain org.apache.sis.referencing.cs.DefaultEllipsoidalCS ellipsoidal CS}.</li>
     *   <li>φ or φ′ or φ<sub>c</sub> → <var>U</var> if used in a
     *       {@linkplain org.apache.sis.referencing.cs.DefaultSphericalCS spherical CS}, regardless of whether the
     *       coordinate system follows <a href="http://en.wikipedia.org/wiki/Spherical_coordinate_system">physics,
     *       mathematics or other conventions</a>.</li>
     *   <li>θ → <var>V</var> if used in a {@linkplain org.apache.sis.referencing.cs.DefaultSphericalCS spherical CS} (regardless of above-cited coordinate system convention).</li>
     *   <li>θ → <var>U</var> if used in a polar CS.</li>
     * </ul>
     *
     * Note that while this method may return a string of any length, ISO 19162 requires abbreviations
     * to be a single Latin character.
     *
     * @param  cs           The enclosing coordinate system, or {@code null} if unknown.
     * @param  direction    The direction of the axis to format.
     * @param  abbreviation The axis abbreviation, to be eventually replaced by this method.
     * @return The axis abbreviation to format.
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
     *   <li><var>U</var> → φ′ if {@code csType} is {@code "spherical"}, regardless of coordinate system convention.</li>
     *   <li><var>V</var> → θ  if {@code csType} is {@code "spherical"}, regardless of coordinate system convention.</li>
     *   <li><var>U</var> → θ  if {@code csType} is {@code "polar"}.</li>
     * </ul>
     *
     * @param  csType       The type of the coordinate system, or {@code null} if unknown.
     * @param  direction    The parsed axis direction.
     * @param  abbreviation The parsed axis abbreviation, to be eventually replaced by this method.
     * @return The axis abbreviation to use. Can not be null.
     */
    public String toUnicodeAbbreviation(final String csType, final AxisDirection direction, String abbreviation) {
        if (abbreviation.length() == 1) {
            final String replacement;
            final String condition;
            switch (abbreviation.charAt(0)) {
                case 'U': if (WKTKeywords.polar.equals(csType)) return "θ";
                          replacement = "φ′"; condition = WKTKeywords.spherical;   break;
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
        Object readResolve() {
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
        Object readResolve() {
            return IDENTITY;
        }
    }
}
