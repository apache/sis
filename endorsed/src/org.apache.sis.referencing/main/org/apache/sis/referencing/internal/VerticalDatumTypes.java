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
import org.opengis.referencing.datum.RealizationMethod;
import org.opengis.referencing.datum.VerticalDatumType;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.AxisDirection;
import org.apache.sis.util.Characters;
import org.apache.sis.util.CharSequences;
import org.apache.sis.measure.Units;


/**
 * Extensions to the standard set of {@link RealizationEpoch}.
 * Some of those constants are derived from a legacy {@link VerticalDatumType} code list.
 * Those constants are not in public API because they were intentionally omitted from ISO 19111,
 * and the ISO experts said that they should really not be public.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
public final class VerticalDatumTypes {
    /**
     * A pseudo-realization method for ellipsoidal heights that are measured along
     * the normal to the ellipsoid used in the definition of horizontal datum.
     *
     * <p>Identifier: {@code CS_DatumType.CS_VD_Ellipsoidal}</p>
     */
    private static final String ELLIPSOIDAL = "ELLIPSOIDAL";

    /**
     * A vertical datum for orthometric heights that are measured along the plumb line.
     *
     * <p>Identifier: {@code CS_DatumType.CS_VD_Orthometric}</p>
     */
    private static final String ORTHOMETRIC = "ORTHOMETRIC";

    /**
     * A vertical datum for origin of the vertical axis based on atmospheric pressure.
     *
     * <p>Identifier: {@code CS_DatumType.CS_VD_AltitudeBarometric}</p>
     */
    private static final String BAROMETRIC = "BAROMETRIC";

    /**
     * Do not allow instantiation of this class.
     */
    private VerticalDatumTypes() {
    }

    /**
     * Returns a pseudo-realization method for ellipsoidal heights.
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
    public static RealizationMethod fromLegacy(final int code) {
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
     * @param  type  the vertical datum type, or {@code null} if unknown.
     * @return the legacy code for the given datum type, or 0 if unknown.
     */
    @SuppressWarnings("deprecation")
    public static int toLegacy(final VerticalDatumType type) {
        if (type != null) {
            switch (type.name()) {
                case ORTHOMETRIC: return 2001;      // CS_VD_Orthometric
                case ELLIPSOIDAL: return 2002;      // CS_VD_Ellipsoidal
                case BAROMETRIC:  return 2003;      // CS_VD_AltitudeBarometric
                case "GEOIDAL":   return 2005;      // CS_VD_GeoidModelDerived
                case "DEPTH":     return 2006;      // CS_VD_Depth
            }
        }
        return 2000;
    }

    /**
     * Returns the vertical datum type from a realization method.
     * If the given method cannot be mapped to a legacy type, then this method returns "other surface".
     * This is because the vertical datum type was a mandatory property in legacy OGC/ISO standards.
     *
     * @param  method  the realization method, or {@code null}.
     * @return the vertical datum type (never null).
     */
    @SuppressWarnings("deprecation")
    public static VerticalDatumType fromMethod(final RealizationMethod method) {
        if (method == RealizationMethod.GEOID) return VerticalDatumType.GEOIDAL;
        if (method == RealizationMethod.TIDAL) return VerticalDatumType.DEPTH;
        if (method != null) {
            return VerticalDatumType.valueOf(method.name().toUpperCase(Locale.US));
        }
        return VerticalDatumType.OTHER_SURFACE;
    }

    /**
     * Returns the realization method from a name.
     *
     * @param  type  the vertical datum type, or {@code null}.
     * @return the realization method, or {@code null} if none.
     */
    @SuppressWarnings("deprecation")
    public static RealizationMethod toMethod(final VerticalDatumType type) {
        if (type != null) {
            if (type == VerticalDatumType.GEOIDAL)         return RealizationMethod.GEOID;
            if (type == VerticalDatumType.DEPTH)           return RealizationMethod.TIDAL;
            if (type == VerticalDatumType.BAROMETRIC)      return RealizationMethod.valueOf(BAROMETRIC);
            if (ORTHOMETRIC.equalsIgnoreCase(type.name())) return RealizationMethod.valueOf(ORTHOMETRIC);
            if (ELLIPSOIDAL.equalsIgnoreCase(type.name())) return ellipsoidal();
        }
        return null;
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
    public static RealizationMethod guess(final String name, final Collection<? extends GenericName> aliases,
            final CoordinateSystemAxis axis)
    {
        RealizationMethod method = guess(name);
        if (method != null) {
            return method;
        }
        if (aliases != null) {
            for (final GenericName alias : aliases) {
                method = guess(alias.tip().toString());
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
     * Guesses the realization method of a datum of the given name. This method attempts to guess only if
     * the given name contains at least one letter. If the type cannot be determined, returns {@code null}.
     *
     * @param  name  name of the datum for which to guess a realization method, or {@code null}.
     * @return a realization method, or {@code null} if none can be guessed.
     */
    private static RealizationMethod guess(final String name) {
        if (name != null) {
            if (CharSequences.equalsFiltered("Mean Sea Level", name, Characters.Filter.LETTERS_AND_DIGITS, true)) {
                return RealizationMethod.TIDAL;
            }
            if (name.contains("geoid")) {
                return RealizationMethod.GEOID;
            }
        }
        return null;
    }
}
