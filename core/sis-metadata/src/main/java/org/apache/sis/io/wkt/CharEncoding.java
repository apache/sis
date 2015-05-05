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

import java.io.Serializable;
import org.opengis.referencing.cs.PolarCS;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.apache.sis.util.CharSequences;


/**
 * Controls the replacement on Unicode characters during WKT formatting.
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
 * <div class="section">Application to mathematical symbols</div>
 * For Greek letters used as mathematical symbols or
 * {@linkplain org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#getAbbreviation() coordinate axis abbreviations},
 * the ISO 19162 standard recommends:
 *
 * <ul>
 *   <li>(<var>P</var>, <var>L</var>) as the transliteration of the Greek letters (<var>phi</var>, <var>lambda</var>), or
 *       (<var>B</var>, <var>L</var>) from German “Breite” and “Länge” used in academic texts worldwide, or
 *       (<var>lat</var>, <var>long</var>).</li>
 *   <li>(<var>U</var>) for (θ) in {@linkplain org.apache.sis.referencing.cs.DefaultPolarCS polar coordinate systems}.</li>
 *   <li>(<var>U</var>, <var>V</var>) for (φ, θ) in
 *       {@linkplain org.apache.sis.referencing.cs.DefaultSphericalCS spherical coordinate systems}.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public abstract class CharEncoding implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 7115456393795045932L;

    /**
     * A character encoding compliant with ISO 19162 on a <cite>"best effort"</cite> basis.
     * All methods perform the default implementation documented in this {@code CharEncoding} class.
     */
    public static final CharEncoding DEFAULT = new Default();

    /**
     * A character encoding that does not perform any replacement.
     * All methods let Unicode characters pass-through unchanged.
     */
    public static final CharEncoding UNICODE = new Unicode();

    /**
     * For sub-class constructors.
     */
    protected CharEncoding() {
    }

    /**
     * Returns a character sequences with the non-ASCII characters replaced or removed.
     * The default implementation invokes {@link CharSequences#toASCII(CharSequence)}.
     *
     * <p>Implementations shall not care about {@linkplain Symbols#getOpeningQuote(int) opening} or
     * {@linkplain Symbols#getClosingQuote(int) closing quotes}. The quotes will be doubled by the
     * caller if needed.</p>
     *
     * @param  text The text to format without non-ASCII characters.
     * @return The text to write in <cite>Well Known Text</cite>.
     */
    public String filter(final String text) {
        return CharSequences.toASCII(text).toString();
    }

    /**
     * Returns the axis abbreviation to format, or {@code null} if none. The abbreviation is obtained by
     * {@link CoordinateSystemAxis#getAbbreviation()}, but may contain Greek letters (in particular φ, λ
     * and θ).
     *
     * @param  cs   The enclosing coordinate system, or {@code null} if unknown.
     * @param  axis The axis for which to get the abbreviation to format.
     * @return The axis abbreviation to format.
     *
     * @see org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis#formatTo(Formatter)
     */
    public String getAbbreviation(final CoordinateSystem cs, final CoordinateSystemAxis axis) {
        String a = axis.getAbbreviation();
        if (a != null && !a.isEmpty() && a.length() <= 2) {
            switch (a.charAt(0)) {
                /*
                 * ISO 19162 §7.5.3 recommendations:
                 *
                 *   a) For PolarCS using Greek letter θ for direction, the letter ‘U’ should be used in WKT.
                 *   b) For SphericalCS using φ and θ, the letter ‘U’ and ‘V’ respectively should be used in WKT.
                 */
                case 'θ': {
                    if  (cs instanceof SphericalCS) a ="V";
                    else if (cs instanceof PolarCS) a ="U";
                    break;
                }
                /*
                 * ISO 19162 §7.5.3 requirement (ii) and recommendation (b):
                 *
                 *  ii) Greek letters φ and λ for geodetic latitude and longitude must be replaced by Latin char.
                 *   b) For SphericalCS using φ and θ, the letter ‘U’ and ‘V’ respectively should be used in WKT.
                 *
                 * Note that some SphericalCS may use φ′ or φc for distinguishing from geodetic latitude φ.
                 */
                case 'φ': {
                    if (cs instanceof SphericalCS) {
                        a = "U";
                    } else if (cs instanceof EllipsoidalCS) {
                        a = "B";    // From German "Breite", used in academic texts worldwide.
                    }
                    break;
                }
                case 'λ': {
                    if (cs instanceof EllipsoidalCS) {
                        a = "L";    // From German "Länge", used in academic texts worldwide.
                    }
                    break;
                }
            }
        }
        return a;
    }

    /**
     * The {@link CharEncoding#DEFAULT} implementation.
     */
    private static final class Default extends CharEncoding {
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
     * The {@link CharEncoding#UNICODE} implementation.
     */
    private static final class Unicode extends CharEncoding {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 7392131912748253956L;

        /** Performs no replacement. */
        @Override public String filter(String text) {
            return text;
        }

        /** Returns the abbreviation as-is. */
        @Override public String getAbbreviation(CoordinateSystem cs, CoordinateSystemAxis axis) {
            return axis.getAbbreviation();
        }

        /** Returns a string representation similar to enum. */
        @Override public String toString() {
            return "UNICODE";
        }

        /** Replaces deserialized instances by the unique instance. */
        Object readResolve() {
            return UNICODE;
        }
    }
}
