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
package org.apache.sis.referencing.internal;

import java.util.Locale;
import java.util.Collection;
import javax.measure.Unit;
import org.opengis.util.GenericName;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.AxisDirection;
import org.apache.sis.util.Characters;
import org.apache.sis.util.CharSequences;
import org.apache.sis.measure.Units;
import org.apache.sis.util.privy.CodeLists;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.referencing.datum.VerticalDatumType;

// Specific to the main branch:
import java.util.function.Predicate;
import org.opengis.util.CodeList;
import org.apache.sis.util.StringBuilders;


/**
 * Extensions to the standard set of {@link VerticalDatumType}.
 * Those constants are not in public API because they were intentionally omitted from ISO 19111,
 * and the ISO experts said that they should really not be public.
 *
 * <p>This class implements {@link Predicate} for opportunist reasons.
 * This implementation convenience may change in any future SIS version.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
public final class VerticalDatumTypes implements Predicate<CodeList<?>> {
    /**
     * A pseudo-realization method for ellipsoidal heights that are measured along
     * the normal to the ellipsoid used in the definition of horizontal datum.
     * <strong>The use of this method is deprecated</strong> as ellipsoidal height
     * should never be separated from the horizontal components according ISO 19111.
     *
     * <h4>Legacy</h4>
     * This type was associated to code 2000 in the {@code Vert_Datum} element of the legacy WKT 1 format.
     * The UML identifier was {@code CS_DatumType.CS_VD_Ellipsoidal}.
     *
     * @see org.apache.sis.referencing.CommonCRS.Vertical#ELLIPSOIDAL
     */
    static final String ELLIPSOIDAL = "ELLIPSOIDAL";

    /**
     * A vertical datum type for orthometric heights that are measured along the plumb line.
     *
     * <h4>Legacy</h4>
     * This type was associated to code 2001 in the {@code Vert_Datum} element of the legacy WKT 1 format.
     * The UML identifier was {@code CS_DatumType.CS_VD_Orthometric}.
     */
    private static final String ORTHOMETRIC = "ORTHOMETRIC";

    /**
     * A vertical datum type for origin of the vertical axis based on atmospheric pressure.
     *
     * <h4>Legacy</h4>
     * This type was associated to code 2003 in the {@code Vert_Datum} element of the legacy WKT 1 format.
     * The UML identifier was {@code CS_DatumType.CS_VD_AltitudeBarometric}.
     *
     * @see org.apache.sis.referencing.CommonCRS.Vertical#BAROMETRIC
     */
    static final String BAROMETRIC = "BAROMETRIC";

    /**
     * A value used in the <abbr>EPSG</abbr> database.
     */
    static final String LOCAL = "LOCAL";

    /**
     * Returns a pseudo-realization method for ellipsoidal heights.
     * <strong>The use of this method is deprecated</strong> as ellipsoidal height
     * should never be separated from the horizontal components according ISO 19111.
     *
     * <h4>Maintenance note</h4>
     * If the implementation of this method is modified, search for {@code VerticalDatumType.valueOf}
     * at least in {@link org.apache.sis.referencing.CommonCRS.Vertical#datum()} and make sure that
     * the code is equivalent.
     *
     * @return the ellipsoidal pseudo-realization method.
     */
    public static VerticalDatumType ellipsoidal() {
        return VerticalDatumType.valueOf(ELLIPSOIDAL);
    }

    /**
     * Returns {@code true} if the given value is the ellipsoidal pseudo-realization method.
     *
     * @param  method  the method to test, or {@code null}.
     * @return whether the given method is the ellipsoidal pseudo-realization method.
     */
    public static boolean ellipsoidal(final VerticalDatumType method) {
        return (method != null) && ELLIPSOIDAL.equalsIgnoreCase(method.name());
    }

    /**
     * Returns the vertical datum type from a legacy code. The legacy codes were defined in
     * OGC 01-009 (<cite>Coordinate Transformation Services)</cite>, which also defined the version 1
     * of <cite>Well Known Text</cite></a> format (WKT 1). This method is used for WKT 1 parsing.
     *
     * @param  code  the legacy vertical datum code.
     * @return the vertical datum type, or {@code null} if none.
     */
    public static VerticalDatumType fromLegacyCode(final int code) {
        switch (code) {
        //  case 2000: return null;                                     // CS_VD_Other
            case 2001: return VerticalDatumType.valueOf(ORTHOMETRIC);   // CS_VD_Orthometric
            case 2002: return ellipsoidal();                            // CS_VD_Ellipsoidal
            case 2003: return VerticalDatumType.BAROMETRIC;             // CS_VD_AltitudeBarometric
            case 2005: return VerticalDatumType.GEOIDAL;                // CS_VD_GeoidModelDerived
            case 2006: return VerticalDatumType.DEPTH;                  // CS_VD_Depth
            default:   return null;
        }
    }

    /**
     * Returns the legacy code for the datum type, or 2000 (other surface) if unknown.
     * This method is used for WKT 1 formatting.
     *
     * @param  method  the vertical datum type, or {@code null} if unknown.
     * @return the legacy code for the given datum type, or 0 if unknown.
     */
    public static int toLegacyCode(final VerticalDatumType method) {
        if (method != null) {
            switch (method.name().toUpperCase(Locale.US)) {
                case ORTHOMETRIC: return 2001;      // CS_VD_Orthometric
                case ELLIPSOIDAL: return 2002;      // CS_VD_Ellipsoidal
                case BAROMETRIC:  return 2003;      // CS_VD_AltitudeBarometric
                case "GEOIDAL":
                case "GEOID":     return 2005;      // CS_VD_GeoidModelDerived
                case "LEVELLING": // From ISO: "adjustment of a levelling network fixed to one or more tide gauges".
                case "TIDAL":
                case "DEPTH":     return 2006;      // CS_VD_Depth
            }
        }
        return 2000;
    }

    /**
     * Returns the realization method from heuristic rules applied on the name.
     *
     * @param  name  the realization method name, or {@code null}.
     * @return the realization method, or {@code null} if the given name was null.
     */
    public static VerticalDatumType fromMethod(final String name) {
        VerticalDatumType method = fromDatum(name);
        if (method == null && name != null && !name.isBlank()) {
            final int s = name.lastIndexOf('-');
            if (s >= 0 && name.substring(s+1).strip().equalsIgnoreCase("based")) {
                method = fromDatum(name.substring(0, s));
            }
            if (method == null) {
                method = VerticalDatumType.valueOf(name);
            }
        }
        return method;
    }

    /**
     * Guesses the realization method of a datum from its name, aliases or a given vertical axis.
     * This is sometimes needed after XML unmarshalling or WKT parsing, because GML 3.2 and ISO 19162
     * do not contain any attribute for the datum type.
     *
     * <p>This method uses heuristic rules and may be changed in any future SIS version.</p>
     *
     * @param  name     the name of the datum for which to guess a type, or {@code null} if unknown.
     * @param  aliases  the aliases of the datum for which to guess a type, or {@code null} if unknown.
     * @param  axis     the vertical axis for which to guess a type, or {@code null} if unknown.
     * @return a datum type, or {@link VerticalDatumType#OTHER_SURFACE} if none can be guessed.
     */
    public static VerticalDatumType fromDatum(final String name, final Collection<? extends GenericName> aliases,
            final CoordinateSystemAxis axis)
    {
        VerticalDatumType method = fromDatum(name);
        if (method != null) {
            return method;
        }
        if (aliases != null) {
            for (final GenericName alias : aliases) {
                method = fromDatum(alias.tip().toString());
                if (method != null) {
                    return method;
                }
            }
        }
        if (axis != null) {
            final Unit<?> unit = axis.getUnit();
            if (Units.isLinear(unit)) {
                final String abbreviation = axis.getAbbreviation();
                if (abbreviation.length() == 1) {
                    AxisDirection dir = AxisDirection.UP;               // Expected direction for accepting the type.
                    switch (abbreviation.charAt(0)) {
                        case 'h': method = ellipsoidal(); break;
                        case 'H': method = VerticalDatumType.GEOIDAL; break;
                        case 'd': // Fall through
                        case 'D': method = VerticalDatumType.DEPTH; dir = AxisDirection.DOWN; break;
                        default:  return null;
                    }
                    if (dir.equals(axis.getDirection())) {
                        return method;
                    }
                }
            } else if (Units.isPressure(unit)) {
                return VerticalDatumType.BAROMETRIC;
            }
        }
        return VerticalDatumType.OTHER_SURFACE;
    }

    /**
     * Guesses the realization method of a datum of the given name.
     * If the realization method cannot be determined, returns {@code null}.
     *
     * @param  name  name of the datum for which to guess a realization method, or {@code null}.
     * @return a realization method, or {@code null} if none can be guessed.
     */
    private static VerticalDatumType fromDatum(final String name) {
        if (name != null) {
            if (CharSequences.equalsFiltered("Mean Sea Level", name, Characters.Filter.LETTERS_AND_DIGITS, true)) {
                return VerticalDatumType.GEOIDAL;
            }
            int i = 0;
            do {
                if (name.regionMatches(true, i, "geoid", 0, 5)) {
                    return VerticalDatumType.GEOIDAL;
                }
                i = name.indexOf(' ', i) + 1;
            } while (i > 0);
            if (name.equalsIgnoreCase("Tidal")) {
                return VerticalDatumType.DEPTH;
            }
            i = 0;
            while (i < name.length()) {
                final int c = name.codePointAt(i);
                if (Character.isLetter(c)) {
                    return CodeLists.find(VerticalDatumType.class, new VerticalDatumTypes(name));
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
     * @param  name  the datum name.
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
     * @param  code  the code to test.
     * @return {@code true} if the code matches the criterion.
      */
    @Override
    public boolean test(final CodeList<?> code) {
        final int i = datum.indexOf(code.name());
        return (i == 0) || (i >= 0 && Character.isWhitespace(datum.codePointBefore(i)));
    }
}
