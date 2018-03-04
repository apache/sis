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

import javax.measure.Unit;
import javax.measure.Quantity;
import javax.measure.quantity.Angle;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.GeneralParameterDescriptor;
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
import org.apache.sis.measure.Units;
import org.apache.sis.util.Static;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.math.DecimalFunctions;


/**
 * Utility methods for referencing WKT formatting.
 *
 * This class provides a set of {@code toFormattable(…)} for various {@link IdentifiedObject} subtypes.
 * It is important to <strong>not</strong> provide a generic {@code toFormattable(IdentifiedObject)}
 * method, because the user may choose to implement more than one GeoAPI interface for the same object.
 * We need to be specific in order to select the right "aspect" of the given object.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.4
 * @module
 */
public final class WKTUtilities extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private WKTUtilities() {
    }

    /**
     * Returns the WKT type of the given interface.
     *
     * For {@link CoordinateSystem} base type, the returned value shall be one of
     * {@code affine}, {@code Cartesian}, {@code cylindrical}, {@code ellipsoidal}, {@code linear},
     * {@code parametric}, {@code polar}, {@code spherical}, {@code temporal} or {@code vertical}.
     *
     * @param  base  the abstract base interface.
     * @param  type  the interface or classes for which to get the WKT type.
     * @return the WKT type for the given class or interface, or {@code null} if none.
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

    /**
     * Returns the given coordinate reference system as a formattable object.
     *
     * @param  object  the coordinate reference system, or {@code null}.
     * @return the given coordinate reference system as a formattable object, or {@code null}.
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
     * @param  object  the coordinate system, or {@code null}.
     * @return the given coordinate system as a formattable object, or {@code null}.
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
     * @param  object  the coordinate system axis, or {@code null}.
     * @return the given coordinate system axis as a formattable object, or {@code null}.
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
     * @param  object  the datum, or {@code null}.
     * @return the given datum as a formattable object, or {@code null}.
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
     * @param  object  the datum, or {@code null}.
     * @return the given datum as a formattable object, or {@code null}.
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
     * @param  object  the ellipsoid, or {@code null}.
     * @return the given ellipsoid as a formattable object, or {@code null}.
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
     * @param  object  the prime meridian, or {@code null}.
     * @return the given prime meridian as a formattable object, or {@code null}.
     */
    public static FormattableObject toFormattable(final PrimeMeridian object) {
        if (object instanceof FormattableObject) {
            return (FormattableObject) object;
        } else {
            return DefaultPrimeMeridian.castOrCopy(object);
        }
    }

    /**
     * If the given unit is one of the unit that can not be formatted without ambiguity in WKT format,
     * return a proposed replacement. Otherwise returns {@code unit} unchanged.
     *
     * @param  <Q>   the unit dimension.
     * @param  unit  the unit to test.
     * @return the replacement to format, or {@code unit} if not needed.
     *
     * @since 0.8
     */
    @SuppressWarnings("unchecked")
    public static <Q extends Quantity<Q>> Unit<Q> toFormattable(Unit<Q> unit) {
        if (Units.isAngular(unit)) {
            if (!((Unit<Angle>) unit).getConverterTo(Units.RADIAN).isLinear()) {
                unit = (Unit<Q>) Units.DEGREE;
            }
        }
        return unit;
    }

    /**
     * Appends the name of the given object to the formatter.
     *
     * @param  object     the object from which to get the name.
     * @param  formatter  the formatter where to append the name.
     * @param  type       the key of colors to apply if syntax colors are enabled.
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
     * @param  parameters  the parameter to append to the WKT, or {@code null} if none.
     * @param  formatter   the formatter where to append the parameter.
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
     * @param  parameter  the parameter to append to the WKT, or {@code null} if none.
     * @param  formatter  the formatter where to append the parameter.
     */
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
                parameter = new DefaultParameterValue<>((ParameterValue<?>) parameter);
            }
            formatter.append((FormattableObject) parameter);
            formatter.newLine();
        }
    }

    /**
     * Returns {@code true} if the given parameter is defined in the EPSG code space. We handle EPSG
     * parameters in a special way because Apache SIS uses the EPSG geodetic dataset as the primary
     * source of coordinate operation definitions.
     *
     * <p>We intentionally don't define {@code isEPSG(OperationMethod)} method because the operation
     * method may be the inverse of an EPSG method (for example "Inverse of Mercator (variant A)")
     * which would not be recognized. Instead, {@code isEPSG(method.getParameters())} should work.</p>
     *
     * @param  descriptor   the parameter or group of parameters to inspect.
     * @param  ifUndefined  the value to return if the code space is undefined.
     * @return whether the given parameter is an EPSG parameter.
     */
    public static boolean isEPSG(final GeneralParameterDescriptor descriptor, final boolean ifUndefined) {
        if (descriptor != null) {
            final Identifier id = descriptor.getName();
            if (id != null) {
                final String cs = id.getCodeSpace();
                if (cs != null) {
                    return Constants.EPSG.equalsIgnoreCase(cs);
                }
            }
        }
        return ifUndefined;
    }

    /**
     * Suggests an amount of fraction digits to use for formatting numbers in each column of the given sequence
     * of points. The number of fraction digits may be negative if we could round the numbers to 10, <i>etc</i>.
     *
     * @param  crs     the coordinate reference system for each points, or {@code null} if unknown.
     * @param  points  the sequence of points. It is not required that each point has the same dimension.
     * @return suggested amount of fraction digits as an array as long as the longest row.
     */
    public static int[] suggestFractionDigits(final CoordinateReferenceSystem crs, final double[]... points) {
        final int[] fractionDigits = Numerics.suggestFractionDigits(points);
        final Ellipsoid ellipsoid = ReferencingUtilities.getEllipsoid(crs);
        if (ellipsoid != null) {
            /*
             * Use heuristic precisions for geodetic or projected CRS. We do not apply those heuristics
             * for other kind of CRS (e.g. engineering) because we do not know what could be the size
             * of the object attached to the CRS.
             */
            final CoordinateSystem cs = crs.getCoordinateSystem();
            final int dimension = Math.min(cs.getDimension(), fractionDigits.length);
            final double scale = Formulas.scaleComparedToEarth(ellipsoid);
            for (int i=0; i<dimension; i++) {
                final Unit<?> unit = cs.getAxis(i).getUnit();
                double precision;
                if (Units.isLinear(unit)) {
                    precision = Formulas.LINEAR_TOLERANCE * scale;                          // In metres
                } else if (Units.isAngular(unit)) {
                    precision = Formulas.ANGULAR_TOLERANCE * (Math.PI / 180) * scale;       // In radians
                } else if (Units.isTemporal(unit)) {
                    precision = Formulas.TEMPORAL_TOLERANCE;                                // In seconds
                } else {
                    continue;
                }
                precision /= Units.toStandardUnit(unit);                // In units used by the coordinates.
                final int f = DecimalFunctions.fractionDigitsForDelta(precision, false);
                if (f > fractionDigits[i]) {
                    fractionDigits[i] = f;                              // Use at least the heuristic precision.
                }
            }
        }
        return fractionDigits;
    }
}
