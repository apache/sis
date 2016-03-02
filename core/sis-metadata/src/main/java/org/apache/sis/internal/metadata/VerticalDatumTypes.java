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
package org.apache.sis.internal.metadata;

import java.util.Collection;
import javax.measure.unit.Unit;
import org.opengis.util.CodeList;
import org.opengis.util.GenericName;
import org.opengis.referencing.datum.VerticalDatumType;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.AxisDirection;
import org.apache.sis.util.StringBuilders;
import org.apache.sis.measure.Units;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Characters;


/**
 * Extensions to the standard set of {@link VerticalDatumType}.
 * Those constants are not in public API because they were intentionally omitted from ISO 19111,
 * and the ISO experts said that they should really not be public.
 *
 * <p>This class implements {@code CodeList.Filter} for opportunist reasons.
 * This implementation convenience may change in any future SIS version.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 */
public final class VerticalDatumTypes implements CodeList.Filter {
    /**
     * A vertical datum for ellipsoidal heights that are measured along the
     * normal to the ellipsoid used in the definition of horizontal datum.
     *
     * <p>Identifier: {@code CS_DatumType.CS_VD_Ellipsoidal}</p>
     *
     * @see <a href="http://jira.codehaus.org/browse/GEO-133">GEO-133</a>
     */
    public static final VerticalDatumType ELLIPSOIDAL = VerticalDatumType.valueOf("ELLIPSOIDAL");

    /**
     * A vertical datum for orthometric heights that are measured along the plumb line.
     *
     * <p>Identifier: {@code CS_DatumType.CS_VD_Orthometric}</p>
     */
    public static final VerticalDatumType ORTHOMETRIC = VerticalDatumType.valueOf("ORTHOMETRIC");

    /**
     * Mapping from the numeric values used in legacy specification (OGC 01-009) to {@link VerticalDatumType}.
     * Indices in this array are the legacy codes minus 2000.
     *
     * <strong>This array shall not be fill before the above static constants.</strong>
     */
    private static final VerticalDatumType[] TYPES;

    /**
     * Mapping from {@link VerticalDatumType} to the numeric values used in legacy specification (OGC 01-009).
     */
    private static final short[] LEGACY_CODES;
    static {
        TYPES = new VerticalDatumType[7];
        LEGACY_CODES = new short[Math.max(ELLIPSOIDAL.ordinal(), ORTHOMETRIC.ordinal()) + 1];
        for (short code = 2000; code <= 2006; code++) {
            final VerticalDatumType type;
            switch (code) {
                case 2000: type = VerticalDatumType .OTHER_SURFACE; break;  // CS_VD_Other
                case 2001: type = VerticalDatumTypes.ORTHOMETRIC;   break;  // CS_VD_Orthometric
                case 2002: type = VerticalDatumTypes.ELLIPSOIDAL;   break;  // CS_VD_Ellipsoidal
                case 2003: type = VerticalDatumType .BAROMETRIC;    break;  // CS_VD_AltitudeBarometric
                case 2005: type = VerticalDatumType .GEOIDAL;       break;  // CS_VD_GeoidModelDerived
                case 2006: type = VerticalDatumType .DEPTH;         break;  // CS_VD_Depth
                default:   continue;
            }
            TYPES[code - 2000] = type;
            LEGACY_CODES[type.ordinal()] = code;
        }
    }

    /**
     * Returns the vertical datum type from a legacy code. The legacy codes were defined in
     * <a href="http://www.opengeospatial.org/standards/ct">OGC 01-009</a>
     * (<cite>Coordinate Transformation Services)</cite>, which also defined the version 1 of
     * <a href="http://www.geoapi.org/3.0/javadoc/org/opengis/referencing/doc-files/WKT.html"><cite>Well
     * Known Text</cite></a> format (WKT 1). This method is used for WKT 1 parsing.
     *
     * @param  code The legacy vertical datum code.
     * @return The vertical datum type, or {@code null} if the code is unrecognized.
     */
    public static VerticalDatumType fromLegacy(int code) {
        code -= 2000;
        return (code >= 0 && code < TYPES.length) ? TYPES[code] : null;
    }

    /**
     * Returns the legacy code for the datum type, or 0 if unknown.
     * This method is used for WKT 1 formatting.
     *
     * @param  type The vertical datum type, or {@code null} if unknown.
     * @return The legacy code for the given datum type, or 0 if unknown.
     */
    public static int toLegacy(final VerticalDatumType type) {
        if (type != null) {
            final int ordinal = type.ordinal();
            if (ordinal >= 0 && ordinal < LEGACY_CODES.length) {
                return LEGACY_CODES[ordinal];
            }
        }
        return 0;
    }

    /**
     * Guesses the type of a datum from its name, aliases or a given vertical axis. This is sometime needed
     * after XML unmarshalling or WKT parsing, since GML 3.2 and ISO 19162 do not contain any attribute for
     * the datum type.
     *
     * <p>This method uses heuristic rules and may be changed in any future SIS version.
     * If the type can not be determined, defaults to {@link VerticalDatumType#OTHER_SURFACE}.</p>
     *
     * @param  name    The name of the datum for which to guess a type, or {@code null} if unknown.
     * @param  aliases The aliases of the datum for which to guess a type, or {@code null} if unknown.
     * @param  axis    The vertical axis for which to guess a type, or {@code null} if unknown.
     * @return A datum type, or {@link VerticalDatumType#OTHER_SURFACE} if none can be guessed.
     */
    public static VerticalDatumType guess(final String name, final Collection<? extends GenericName> aliases,
            final CoordinateSystemAxis axis)
    {
        VerticalDatumType type = guess(name);
        if (type != null) {
            return type;
        }
        if (aliases != null) {
            for (final GenericName alias : aliases) {
                type = guess(alias.tip().toString());
                if (type != null) {
                    return type;
                }
            }
        }
        if (axis != null) {
            final Unit<?> unit = axis.getUnit();
            if (Units.isLinear(unit)) {
                final String abbreviation = axis.getAbbreviation();
                if (abbreviation.length() == 1) {
                    AxisDirection dir = AxisDirection.UP;   // Expected direction for accepting the type.
                    switch (abbreviation.charAt(0)) {
                        case 'h': type = ELLIPSOIDAL; break;
                        case 'H': type = VerticalDatumType.GEOIDAL; break;
                        case 'D': type = VerticalDatumType.DEPTH; dir = AxisDirection.DOWN; break;
                        default:  return VerticalDatumType.OTHER_SURFACE;
                    }
                    if (dir.equals(axis.getDirection())) {
                        return type;
                    }
                }
            } else if (Units.isPressure(unit)) {
                return VerticalDatumType.BAROMETRIC;
            }
        }
        return VerticalDatumType.OTHER_SURFACE;
    }

    /**
     * Guesses the type of a datum of the given name. This method attempts to guess only if the given name
     * contains at least one letter. If the type can not be determined, returns {@code null}.
     *
     * @param  name Name of the datum for which to guess a type, or {@code null}.
     * @return A datum type, or {@code null} if none can be guessed.
     */
    private static VerticalDatumType guess(final String name) {
        if (name != null) {
            if (CharSequences.equalsFiltered("Mean Sea Level", name, Characters.Filter.LETTERS_AND_DIGITS, true)) {
                return VerticalDatumType.GEOIDAL;
            }
            for (int i=0; i<name.length();) {
                final int c = name.codePointAt(i);
                if (Character.isLetter(c)) {
                    return CodeList.valueOf(VerticalDatumType.class, new VerticalDatumTypes(name));
                }
                i += Character.charCount(c);
            }
        }
        return null;
    }

    /**
     * The name of a datum to compare against the list of datum types,
     * in upper-case and (if possible) using US-ASCII characters.
     */
    private final StringBuilder datum;

    /**
     * Creates a new {@code CodeList.Filter} which will compare the given datum name against the list of datum types.
     * The comparison is case-insensitive and ignores some non-ASCII characters. The exact algorithm applied here is
     * implementation dependent and may change in any future version.
     *
     * @param name The datum name.
     */
    private VerticalDatumTypes(final String name) {
        final int length = name.length();
        datum = new StringBuilder(length);
        for (int i=0; i<length;) {
            final int c = name.codePointAt(i);
            datum.appendCodePoint(Character.toUpperCase(c));
            i += Character.charCount(c);
        }
        StringBuilders.toASCII(datum);
    }

    /**
     * Returns {@code true} if the name of the given code is the prefix of a word in the datum name.
     * We do not test the characters following the prefix because the word may be incomplete
     * (e.g. {@code "geoid"} versus {@code "geoidal"}).
     *
     * <p>This method is public as an implementation side-effect and should be ignored.</p>
     *
     * @param code The code to test.
     * @return {@code true} if the code matches the criterion.
      */
    @Override
    public boolean accept(final CodeList<?> code) {
        final int i = datum.indexOf(code.name());
        return (i == 0) || (i >= 0 && Character.isWhitespace(datum.codePointBefore(i)));
    }

    /**
     * Returns {@code null} for disabling the creation of new code list elements.
     * This method is public as an implementation side-effect and should be ignored.
     *
     * @return {@code null}.
     */
    @Override
    public String codename() {
        return null;
    }
}
