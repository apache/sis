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
package org.apache.sis.internal.referencing;

import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.Ellipsoid;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AbstractCS;
import org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis;
import org.apache.sis.referencing.datum.AbstractDatum;
import org.apache.sis.referencing.datum.DefaultGeodeticDatum;
import org.apache.sis.referencing.datum.DefaultPrimeMeridian;
import org.apache.sis.referencing.datum.DefaultEllipsoid;
import org.apache.sis.parameter.DefaultParameterValue;
import org.apache.sis.io.wkt.ElementKind;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.util.Static;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Utility methods for referencing WKT formatting.
 *
 * This class provides a set of {@code toFormattable(…)} for various {@link IdentifiedObject} subtypes.
 * It is important to <strong>not</strong> provide a generic {@code toFormattable(IdentifiedObject)}
 * method, because the user may choose to implement more than one GeoAPI interface for the same object.
 * We need to be specific in order to select the right "aspect" of the given object.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 */
public final class WKTUtilities extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private WKTUtilities() {
    }

    /**
     * Returns the given coordinate reference system as a formattable object.
     *
     * @param  object The coordinate reference system, or {@code null}.
     * @return The given coordinate reference system as a formattable object, or {@code null}.
     */
    public static FormattableObject toFormattable(final CoordinateReferenceSystem object) {
        if (object instanceof FormattableObject) {
            return (FormattableObject) object;
        } else {
            return AbstractCRS.castOrCopy(object);
        }
    }

    /**
     * Returns the given coordinate system as a formattable object.
     *
     * @param  object The coordinate system, or {@code null}.
     * @return The given coordinate system as a formattable object, or {@code null}.
     */
    public static FormattableObject toFormattable(final CoordinateSystem object) {
        if (object instanceof FormattableObject) {
            return (FormattableObject) object;
        } else {
            return AbstractCS.castOrCopy(object);
        }
    }

    /**
     * Returns the given coordinate system axis as a formattable object.
     *
     * @param  object The coordinate system axis, or {@code null}.
     * @return The given coordinate system axis as a formattable object, or {@code null}.
     */
    public static FormattableObject toFormattable(final CoordinateSystemAxis object) {
        if (object instanceof FormattableObject) {
            return (FormattableObject) object;
        } else {
            return DefaultCoordinateSystemAxis.castOrCopy(object);
        }
    }

    /**
     * Returns the given datum as a formattable object.
     *
     * @param  object The datum, or {@code null}.
     * @return The given datum as a formattable object, or {@code null}.
     */
    public static FormattableObject toFormattable(final Datum object) {
        if (object instanceof FormattableObject) {
            return (FormattableObject) object;
        } else {
            return AbstractDatum.castOrCopy(object);
        }
    }

    /**
     * Returns the given geodetic datum as a formattable object.
     *
     * @param  object The datum, or {@code null}.
     * @return The given datum as a formattable object, or {@code null}.
     */
    public static FormattableObject toFormattable(final GeodeticDatum object) {
        if (object instanceof FormattableObject) {
            return (FormattableObject) object;
        } else {
            return DefaultGeodeticDatum.castOrCopy(object);
        }
    }

    /**
     * Returns the ellipsoid as a formattable object.
     *
     * @param  object The ellipsoid, or {@code null}.
     * @return The given ellipsoid as a formattable object, or {@code null}.
     */
    public static FormattableObject toFormattable(final Ellipsoid object) {
        if (object instanceof FormattableObject) {
            return (FormattableObject) object;
        } else {
            return DefaultEllipsoid.castOrCopy(object);
        }
    }

    /**
     * Returns the given prime meridian as a formattable object.
     *
     * @param  object The prime meridian, or {@code null}.
     * @return The given prime meridian as a formattable object, or {@code null}.
     */
    public static FormattableObject toFormattable(final PrimeMeridian object) {
        if (object instanceof FormattableObject) {
            return (FormattableObject) object;
        } else {
            return DefaultPrimeMeridian.castOrCopy(object);
        }
    }

    /**
     * Appends the name of the given object to the formatter.
     *
     * @param object    The object from which to get the name.
     * @param formatter The formatter where to append the name.
     * @param type      The key of colors to apply if syntax colors are enabled.
     */
    public static void appendName(final IdentifiedObject object, final Formatter formatter, final ElementKind type) {
        String name = IdentifiedObjects.getName(object, formatter.getNameAuthority());
        if (name == null) {
            name = IdentifiedObjects.getName(object, null);
            if (name == null) {
                name = Vocabulary.getResources(formatter.getLocale()).getString(Vocabulary.Keys.Unnamed);
            }
        }
        formatter.append(name, (type != null) ? type : ElementKind.NAME);
    }

    /**
     * Appends a {@linkplain ParameterValueGroup group of parameters} in a {@code Param_MT[…]} element.
     *
     * @param parameters The parameter to append to the WKT, or {@code null} if none.
     * @param formatter The formatter where to append the parameter.
     */
    public static void appendParamMT(final ParameterValueGroup parameters, final Formatter formatter) {
        if (parameters != null) {
            appendName(parameters.getDescriptor(), formatter, ElementKind.PARAMETER);
            append(parameters, formatter);
        }
    }

    /**
     * Appends a {@linkplain ParameterValue parameter} in a {@code PARAMETER[…]} element.
     * If the supplied parameter is actually a {@linkplain ParameterValueGroup parameter group},
     * all contained parameters will be flattened in a single list.
     *
     * @param parameter The parameter to append to the WKT, or {@code null} if none.
     * @param formatter The formatter where to append the parameter.
     */
    @SuppressWarnings({"unchecked","rawtypes"})    // Not needed on JDK7 branch.
    public static void append(GeneralParameterValue parameter, final Formatter formatter) {
        if (parameter instanceof ParameterValueGroup) {
            boolean first = true;
            for (final GeneralParameterValue param : ((ParameterValueGroup) parameter).values()) {
                if (first) {
                    formatter.newLine();
                    first = false;
                }
                append(param, formatter);
            }
        }
        if (parameter instanceof ParameterValue<?>) {
            if (!(parameter instanceof FormattableObject)) {
                parameter = new DefaultParameterValue((ParameterValue<?>) parameter);
            }
            formatter.append((FormattableObject) parameter);
            formatter.newLine();
        }
    }

    /**
     * Returns the WKT type of the given interface.
     *
     * For {@link CoordinateSystem} base type, the returned value shall be one of
     * {@code affine}, {@code Cartesian}, {@code cylindrical}, {@code ellipsoidal}, {@code linear},
     * {@code parametric}, {@code polar}, {@code spherical}, {@code temporal} or {@code vertical}.
     *
     * @param  base The abstract base interface.
     * @param  type The interface or classes for which to get the WKT type.
     * @return The WKT type for the given class or interface, or {@code null} if none.
     *
     * @see ReferencingUtilities#toPropertyName(Class, Class)
     */
    public static String toType(final Class<?> base, final Class<?> type) {
        if (type != base) {
            final StringBuilder name = ReferencingUtilities.toPropertyName(base, type);
            if (name != null) {
                int end = name.length() - 2;
                if (CharSequences.regionMatches(name, end, "CS")) {
                    name.setLength(end);
                    if ("time".contentEquals(name)) {
                        return "temporal";
                    }
                    if (CharSequences.regionMatches(name, 0, "cartesian")) {
                        name.setCharAt(0, 'C');     // "Cartesian"
                    }
                    return name.toString();
                }
            }
        }
        return null;
    }
}
