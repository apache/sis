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
import org.apache.sis.util.privy.CodeLists;
import org.apache.sis.measure.Units;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.datum.RealizationMethod;


/**
 * Extensions to the standard set of {@link RealizationMethod}.
 * Some of those constants are derived from a legacy {@code VerticalDatumType} code list.
 * Those constants are not in public API because they were intentionally omitted from ISO 19111,
 * and the ISO experts said that they should really not be public.
 *
 * <h2>Note on class naming</h2>
 * {@code RealizationMethods} could have been a more appropriate name.
 * For now we keep {@code VerticalDatumTypes} as a way to remind that this is currently only about vertical datum.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
public final class VerticalDatumTypes {
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
     * Do not allow instantiation of this class.
     */
    private VerticalDatumTypes() {
    }

    /**
     * Returns a pseudo-realization method for ellipsoidal heights.
     * <strong>The use of this method is deprecated</strong> as ellipsoidal height
     * should never be separated from the horizontal components according ISO 19111.
     *
     * <h4>Maintenance note</h4>
     * If the implementation of this method is modified, search for {@code RealizationMethod.valueOf}
     * at least in {@link org.apache.sis.referencing.CommonCRS.Vertical#datum()} and make sure that
     * the code is equivalent.
     *
     * @return the ellipsoidal pseudo-realization method.
     */
    public static RealizationMethod ellipsoidal() {
        return RealizationMethod.valueOf(ELLIPSOIDAL);
    }

    /**
     * Returns {@code true} if the given value is the ellipsoidal pseudo-realization method.
     *
     * @param  method  the method to test, or {@code null}.
     * @return whether the given method is the ellipsoidal pseudo-realization method.
     */
    public static boolean ellipsoidal(final RealizationMethod method) {
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
    public static RealizationMethod fromLegacyCode(final int code) {
        switch (code) {
        //  case 2000: return null;                                     // CS_VD_Other
            case 2001: return RealizationMethod.valueOf(ORTHOMETRIC);   // CS_VD_Orthometric
            case 2002: return ellipsoidal();                            // CS_VD_Ellipsoidal
            case 2003: return RealizationMethod.valueOf(BAROMETRIC);    // CS_VD_AltitudeBarometric
            case 2005: return RealizationMethod.GEOID;                  // CS_VD_GeoidModelDerived
            case 2006: return RealizationMethod.TIDAL;                  // CS_VD_Depth
            default:   return null;
        }
    }

    /**
     * Returns the legacy code for the datum type, or 2000 (other surface) if unknown.
     * This method is used for WKT 1 formatting.
     *
     * @param  method  the realization method, or {@code null} if unknown.
     * @return the legacy code for the given datum type, or 0 if unknown.
     */
    public static int toLegacyCode(final RealizationMethod method) {
        if (method != null) {
            switch (method.name().toUpperCase(Locale.US)) {
                case ORTHOMETRIC: return 2001;      // CS_VD_Orthometric
                case ELLIPSOIDAL: return 2002;      // CS_VD_Ellipsoidal
                case BAROMETRIC:  return 2003;      // CS_VD_AltitudeBarometric
                case "GEOID":     return 2005;      // CS_VD_GeoidModelDerived
                case "TIDAL":     return 2006;      // CS_VD_Depth
            }
        }
        return 2000;
    }

    /**
     * Returns the vertical datum type from a realization method.
     * If the given method cannot be mapped to a legacy type, then this method returns "other surface".
     * This is because the vertical datum type was a mandatory property in legacy OGC/ISO standards.
     * This method is used for writing GML documents older than GML 3.2.
     *
     * @param  method  the realization method, or {@code null}.
     * @return the vertical datum type name (never null).
     */
    public static String toLegacyName(final RealizationMethod method) {
        if (method == RealizationMethod.GEOID) return "geoidal";
        if (method == RealizationMethod.TIDAL) return "depth";
        if (method != null) {
            return method.name().toLowerCase(Locale.US);
        }
        return "other surface";
    }

    /**
     * Returns the realization method from a vertical datum type.
     * This method is used for reading GML documents older than GML 3.2.
     *
     * @param  type  the vertical datum type, or {@code null}.
     * @return the realization method, or {@code null} if none.
     */
    public static RealizationMethod fromLegacyName(final String type) {
        if ("geoidal"  .equalsIgnoreCase(type)) return RealizationMethod.GEOID;
        if ("depth"    .equalsIgnoreCase(type)) return RealizationMethod.TIDAL;
        if (LOCAL      .equalsIgnoreCase(type)) return RealizationMethod.valueOf(LOCAL);
        if (BAROMETRIC .equalsIgnoreCase(type)) return RealizationMethod.valueOf(BAROMETRIC);
        if (ORTHOMETRIC.equalsIgnoreCase(type)) return RealizationMethod.valueOf(ORTHOMETRIC);
        if (ELLIPSOIDAL.equalsIgnoreCase(type)) return ellipsoidal();
        return null;
    }

    /**
     * Returns the realization method from heuristic rules applied on the name.
     *
     * @param  name  the realization method name, or {@code null}.
     * @return the realization method, or {@code null} if the given name was null.
     */
    public static RealizationMethod fromMethod(final String name) {
        RealizationMethod method = fromLegacyName(name);
        if (method == null && name != null && !name.isBlank()) {
            final int s = name.lastIndexOf('-');
            if (s >= 0 && name.substring(s+1).strip().equalsIgnoreCase("based")) {
                method = CodeLists.forCodeName(RealizationMethod.class, name.substring(0, s));
            }
            if (method == null) {
                method = RealizationMethod.valueOf(name);
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
     * @return a datum type, or {@code null} if none can be guessed.
     */
    public static RealizationMethod fromDatum(final String name, final Collection<? extends GenericName> aliases,
            final CoordinateSystemAxis axis)
    {
        RealizationMethod method = fromDatum(name);
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
                        case 'H': method = RealizationMethod.GEOID; break;
                        case 'd': // Fall through
                        case 'D': method = RealizationMethod.TIDAL; dir = AxisDirection.DOWN; break;
                        default:  return null;
                    }
                    if (dir.equals(axis.getDirection())) {
                        return method;
                    }
                }
            } else if (Units.isPressure(unit)) {
                return RealizationMethod.valueOf(BAROMETRIC);
            }
        }
        return null;
    }

    /**
     * Guesses the realization method of a datum of the given name.
     * If the realization method cannot be determined, returns {@code null}.
     *
     * @param  name  name of the datum for which to guess a realization method, or {@code null}.
     * @return a realization method, or {@code null} if none can be guessed.
     */
    private static RealizationMethod fromDatum(final String name) {
        if (name != null) {
            if (CharSequences.equalsFiltered("Mean Sea Level", name, Characters.Filter.LETTERS_AND_DIGITS, true)) {
                return RealizationMethod.TIDAL;
            }
            int i = 0;
            do {
                if (name.regionMatches(true, i, "geoid", 0, 5)) {
                    return RealizationMethod.GEOID;
                }
                i = name.indexOf(' ', i) + 1;
            } while (i > 0);
        }
        return null;
    }
}
