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
package org.apache.sis.referencing.cs;

import java.util.Map;
import java.util.HashMap;
import javax.measure.unit.Unit;
import javax.measure.unit.NonSI;
import javax.measure.quantity.Angle;
import javax.measure.converter.UnitConverter;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlAttribute;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.cs.RangeMeaning;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.apache.sis.internal.referencing.AxisDirections;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.measure.Longitude;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Units;
import org.apache.sis.util.Immutable;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.io.wkt.Formatter;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.apache.sis.util.CharSequences.trimWhitespaces;

// Related to JDK7
import java.util.Objects;


/**
 * Definition of a coordinate system axis. This is used to label axes and indicate their orientation.
 *
 * {@section Axis names}
 * In some case, the axis name is constrained by ISO 19111 depending on the
 * {@linkplain org.opengis.referencing.crs.CoordinateReferenceSystem coordinate reference system} type.
 * These constraints are identified in the javadoc by "<cite>ISO 19111 name is...</cite>" sentences.
 * This constraint works in two directions; for example the names "<cite>geodetic latitude</cite>" and
 * "<cite>geodetic longitude</cite>" shall be used to designate the coordinate axis names associated
 * with a {@linkplain org.opengis.referencing.crs.GeographicCRS geographic coordinate reference system}.
 * Conversely, these names shall not be used in any other context.
 * See the GeoAPI {@linkplain org.opengis.referencing.cs#AxisNames axis name constraints} section
 * for more information.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.0)
 * @version 0.4
 * @module
 *
 * @see AbstractCS
 * @see Unit
 */
@Immutable
public class DefaultCoordinateSystemAxis extends AbstractIdentifiedObject implements CoordinateSystemAxis {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -7883614853277827689L;

    /**
     * Some names to be treated as equivalent. This is needed because axis names are the primary way to
     * distinguish between {@link CoordinateSystemAxis} instances. Those names are strictly defined by
     * ISO 19111 as "Geodetic latitude" and "Geodetic longitude" among others, but the legacy WKT
     * specifications from OGC 01-009 defined the names as "Lon" and "Lat" for the same axis.
     *
     * <p>Keys in this map are names <strong>in lower cases</strong>.
     * Values are any object that allow us to differentiate latitude from longitude.</p>
     *
     * @see #nameMatches(String)
     */
    private static final Map<String,Object> ALIASES = new HashMap<>(12);
    static {
        final Boolean latitude  = Boolean.TRUE;
        final Boolean longitude = Boolean.FALSE;
        ALIASES.put("lat",                latitude);
        ALIASES.put("latitude",           latitude);
        ALIASES.put("geodetic latitude",  latitude);
        ALIASES.put("lon",                longitude);
        ALIASES.put("long",               longitude);
        ALIASES.put("longitude",          longitude);
        ALIASES.put("geodetic longitude", longitude);
        /*
         * Do not add aliases for "x" and "y" in this map. See ALIASES_XY for more information.
         */
    }

    /**
     * Aliases for the "x" and "y" abbreviations (special cases). "x" and "y" are sometime used (especially in WKT)
     * for meaning "Easting" and "Northing". However we shall not add "x" and "y" as aliases in the {@link #ALIASES}
     * map, because experience has shown that doing so cause a lot of undesirable side effects. The "x" abbreviation
     * is used for too many things ("Easting", "Westing", "Geocentric X", "Display right", "Display left") and likewise
     * for "y". Declaring them as aliases introduces confusion in many places. Instead, the "x" and "y" cases are
     * handled in a special way by the {@code nameMatchesXY(…)} method.
     *
     * <p>Names at even index are for "x" and names at odd index are for "y".</p>
     *
     * @see #nameMatchesXY(String, String)
     */
    private static final String[] ALIASES_XY = {
        "Easting", "Northing",
        "Westing", "Southing"
    };

    /**
     * The abbreviation used for this coordinate system axes.
     * Examples are "<var>X</var>" and "<var>Y</var>".
     */
    @XmlElement(name = "axisAbbrev", required = true)
    private final String abbreviation;

    /**
     * Direction of this coordinate system axis. In the case of Cartesian projected
     * coordinates, this is the direction of this coordinate system axis locally.
     */
    @XmlElement(name = "axisDirection", required = true)
    private final AxisDirection direction;

    /**
     * The unit of measure used for this coordinate system axis.
     */
    @XmlAttribute(name= "uom", required = true)
    private final Unit<?> unit;

    /**
     * Minimal and maximal value for this axis.
     */
    private final double minimum, maximum;

    /**
     * The range meaning for this axis.
     */
    private final RangeMeaning rangeMeaning;

    /**
     * Constructs an axis from a set of properties and a given range.
     * The properties map is given unchanged to the
     * {@linkplain AbstractIdentifiedObject#AbstractIdentifiedObject(Map) super-class constructor}.
     * The following table is a reminder of main (not all) properties:
     *
     * <table class="sis">
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link ReferenceIdentifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link ReferenceIdentifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     * </table>
     *
     * @param properties   The properties to be given to the identified object.
     * @param abbreviation The {@linkplain #getAbbreviation() abbreviation} used for this coordinate system axis.
     * @param direction    The {@linkplain #getDirection() direction} of this coordinate system axis.
     * @param unit         The {@linkplain #getUnit() unit of measure} used for this coordinate system axis.
     * @param minimum      The minimum value normally allowed for this axis.
     * @param maximum      The maximum value normally allowed for this axis.
     * @param rangeMeaning The meaning of axis value range specified by the minimum and maximum values.
     */
    public DefaultCoordinateSystemAxis(final Map<String,?> properties,
                                       final String        abbreviation,
                                       final AxisDirection direction,
                                       final Unit<?>       unit,
                                       final double        minimum,
                                       final double        maximum,
                                       final RangeMeaning  rangeMeaning)
    {
        super(properties);
        this.abbreviation = abbreviation;
        this.direction    = direction;
        this.unit         = unit;
        this.minimum      = minimum;
        this.maximum      = maximum;
        this.rangeMeaning = rangeMeaning;
        ensureNonNull("abbreviation", abbreviation);
        ensureNonNull("direction",    direction);
        ensureNonNull("unit",         unit);
        ensureNonNull("rangeMeaning", rangeMeaning);
        if (!(minimum < maximum)) { // Use '!' for catching NaN
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalRange_2, minimum, maximum));
        }
    }

    /**
     * Constructs an axis from a set of properties and a range inferred from the axis unit and direction.
     * The properties map is the same than for the {@linkplain #DefaultCoordinateSystemAxis(Map, String,
     * AxisDirection, Unit, double, double, RangeMeaning) above constructor}.
     *
     * @param properties   The properties to be given to the identified object.
     * @param abbreviation The {@linkplain #getAbbreviation() abbreviation} used for this coordinate system axis.
     * @param direction    The {@linkplain #getDirection() direction} of this coordinate system axis.
     * @param unit         The {@linkplain #getUnit() unit of measure} used for this coordinate system axis.
     */
    public DefaultCoordinateSystemAxis(final Map<String,?> properties,
                                       final String        abbreviation,
                                       final AxisDirection direction,
                                       final Unit<?>       unit)
    {
        // NOTE: we would invoke this(properties, abbreviation, ...) instead if Oracle fixed
        // RFE #4093999 ("Relax constraint on placement of this()/super() call in constructors").
        super(properties);
        this.abbreviation = abbreviation;
        this.direction    = direction;
        this.unit         = unit;
        ensureNonNull("abbreviation", abbreviation);
        ensureNonNull("direction",    direction);
        ensureNonNull("unit",         unit);
        double min = Double.NEGATIVE_INFINITY;
        double max = Double.POSITIVE_INFINITY;
        RangeMeaning r = RangeMeaning.EXACT;
        if (Units.isAngular(unit)) {
            final UnitConverter fromDegrees = NonSI.DEGREE_ANGLE.getConverterTo(unit.asType(Angle.class));
            final AxisDirection dir = AxisDirections.absolute(direction);
            if (dir.equals(AxisDirection.NORTH)) {
                min = fromDegrees.convert(Latitude.MIN_VALUE);
                max = fromDegrees.convert(Latitude.MAX_VALUE);
            } else if (dir.equals(AxisDirection.EAST)) {
                min = fromDegrees.convert(Longitude.MIN_VALUE);
                max = fromDegrees.convert(Longitude.MAX_VALUE);
                r = RangeMeaning.WRAPAROUND; // 180°E wraps to 180°W
            }
            if (min > max) {
                final double t = min;
                min = max;
                max = t;
            }
        }
        minimum = min;
        maximum = max;
        rangeMeaning = r;
    }

    /**
     * Creates a new coordinate system axis with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param axis The coordinate system axis to copy.
     *
     * @see #castOrCopy(CoordinateSystemAxis)
     */
    protected DefaultCoordinateSystemAxis(final CoordinateSystemAxis axis) {
        super(axis);
        abbreviation = axis.getAbbreviation();
        direction    = axis.getDirection();
        unit         = axis.getUnit();
        minimum      = axis.getMinimumValue();
        maximum      = axis.getMaximumValue();
        rangeMeaning = axis.getRangeMeaning();
    }

    /**
     * Returns a SIS axis implementation with the same values than the given arbitrary implementation.
     * If the given object is {@code null}, then this method returns {@code null}. Otherwise if the
     * given object is already a SIS implementation, then the given object is returned unchanged.
     * Otherwise a new SIS implementation is created and initialized to the values of the given object.
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultCoordinateSystemAxis castOrCopy(final CoordinateSystemAxis object) {
        return (object == null) || (object instanceof DefaultCoordinateSystemAxis)
                ? (DefaultCoordinateSystemAxis) object : new DefaultCoordinateSystemAxis(object);
    }

    /**
     * Returns the direction of this coordinate system axis.
     * This direction is often approximate and intended to provide a human interpretable meaning to the axis.
     * A {@linkplain AbstractCS coordinate system} can not contain two axes having the same direction or
     * opposite directions.
     *
     * <p>Examples:
     * {@linkplain AxisDirection#NORTH north} or {@linkplain AxisDirection#SOUTH south},
     * {@linkplain AxisDirection#EAST  east}  or {@linkplain AxisDirection#WEST  west},
     * {@linkplain AxisDirection#UP    up}    or {@linkplain AxisDirection#DOWN  down}.</p>
     *
     * @return The direction of this coordinate system axis.
     */
    @Override
    public AxisDirection getDirection() {
        return direction;
    }

    /**
     * Returns the abbreviation used for this coordinate system axes.
     * Examples are "<var>X</var>" and "<var>Y</var>".
     *
     * @return The coordinate system axis abbreviation.
     */
    @Override
    public String getAbbreviation() {
        return abbreviation;
    }

    /**
     * Returns the unit of measure used for this coordinate system axis. If this {@code CoordinateSystemAxis}
     * was given by <code>{@link AbstractCS#getAxis(int) CoordinateSystem.getAxis}(i)</code>, then all ordinate
     * values at dimension <var>i</var> in a coordinate tuple shall be recorded using this unit of measure.
     *
     * @return The unit of measure used for ordinate values along this coordinate system axis.
     */
    @Override
    public Unit<?> getUnit() {
        return unit;
    }

    /**
     * Returns the minimum value normally allowed for this axis, in the {@linkplain #getUnit()
     * unit of measure for the axis}. If there is no minimum value, then this method returns
     * {@linkplain Double#NEGATIVE_INFINITY negative infinity}.
     *
     * @return The minimum value normally allowed for this axis.
     */
    @Override
    public double getMinimumValue() {
        return minimum;
    }

    /**
     * Returns the maximum value normally allowed for this axis, in the {@linkplain #getUnit()
     * unit of measure for the axis}. If there is no maximum value, then this method returns
     * {@linkplain Double#POSITIVE_INFINITY negative infinity}.
     *
     * @return The maximum value normally allowed for this axis.
     */
    @Override
    public double getMaximumValue() {
        return maximum;
    }

    /**
     * Returns the meaning of axis value range specified by the {@linkplain #getMinimumValue() minimum}
     * and {@linkplain #getMaximumValue() maximum} values.
     *
     * @return The meaning of axis value range.
     */
    @Override
    public RangeMeaning getRangeMeaning() {
        return rangeMeaning;
    }

    /**
     * Returns {@code true} if either the {@linkplain #getName() primary name} or at least
     * one {@linkplain #getAlias() alias} matches the specified string. This method performs
     * all the search done by the {@linkplain AbstractIdentifiedObject#nameMatches(String)
     * super-class}, with the addition of special processing for latitudes and longitudes:
     *
     * <ul>
     *   <li>{@code "Lat"}, {@code "Latitude"}  and {@code "Geodetic latitude"}  are considered equivalent.</li>
     *   <li>{@code "Lon"}, {@code "Longitude"} and {@code "Geodetic longitude"} are considered equivalent.</li>
     * </ul>
     *
     * The above special cases are needed in order to workaround a conflict in specifications:
     * ISO 19111 states explicitly that the latitude and longitude axis names shall be
     * "<cite>Geodetic latitude</cite>" and "<cite>Geodetic longitude</cite>", while the legacy
     * OGC 01-009 (where the WKT format is defined) said that the default values shall be
     * "<cite>Lat</cite>" and "<cite>Lon</cite>".
     *
     * @param  name The name to compare.
     * @return {@code true} if the primary name of at least one alias matches the specified {@code name}.
     */
    @Override
    public boolean nameMatches(final String name) {
        if (super.nameMatches(name)) {
            return true;
        }
        /*
         * The standard comparisons didn't worked. Check for the aliases. Note: we don't
         * test for 'nameMatchesXY(...)' here because the "x" and "y" axis names are too
         * generic. We test them only in the 'equals' method, which has the extra-safety
         * of units comparison (so less risk to treat incompatible axes as equivalent).
         */
        final Object type = ALIASES.get(trimWhitespaces(name).toLowerCase());
        return (type != null) && (type == ALIASES.get(trimWhitespaces(getName().getCode()).toLowerCase()));
    }

    /**
     * Special cases for "x" and "y" names. "x" is considered equivalent to "Easting" or "Westing",
     * but the converse is not true. Note: by avoiding to put "x" in the {@link #ALIASES} map, we
     * avoid undesirable side effects like considering "Easting" as equivalent to "Westing".
     *
     * @param  xy   The name which may be "x" or "y".
     * @param  name The second name to compare with.
     * @return {@code true} if the second name is equivalent to "x" or "y"
     *         (depending on the {@code xy} value), or {@code false} otherwise.
     */
    private static boolean nameMatchesXY(String xy, String name) {
        xy = trimWhitespaces(xy);
        if (xy.length() == 1) {
            int i = Character.toLowerCase(xy.charAt(0)) - 'x';
            if (i >= 0 && i <= 1) {
                name = trimWhitespaces(name);
                if (!name.isEmpty()) do {
                    if (name.regionMatches(true, 0, ALIASES_XY[i], 0, name.length())) {
                        return true;
                    }
                } while ((i += 2) < ALIASES_XY.length);
            }
        }
        return false;
    }

    /**
     * Compares the specified object with this axis for equality.
     *
     * @param  object The object to compare to {@code this}.
     * @param  mode {@link ComparisonMode#STRICT STRICT} for performing a strict comparison, or
     *         {@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA} for comparing only properties
     *         relevant to transformations.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true; // Slight optimization.
        }
        if (!(object instanceof CoordinateSystemAxis && super.equals(object, mode))) {
            return false;
        }
        final DefaultCoordinateSystemAxis that = castOrCopy((CoordinateSystemAxis) object);
        return equals(that, mode.ordinal() < ComparisonMode.IGNORE_METADATA.ordinal(), true);
    }

    /**
     * Compares the specified object with this axis for equality, with optional comparison of units.
     * Units shall always be compared (they are not just metadata), except in the particular case of
     * {@link CoordinateSystems#axisColinearWith}, which is used as a first step toward units conversions
     * through {@link CoordinateSystems#swapAndScaleAxis}.
     */
    final boolean equals(final DefaultCoordinateSystemAxis that,
                         final boolean compareMetadata, final boolean compareUnit)
    {
        /*
         * It is important to NOT compare the minimum and maximum values when we are in
         * "ignore metadata" mode,  because we want CRS with a [-180 … +180]° longitude
         * range to be considered equivalent, from a coordinate transformation point of
         * view, to a CRS with a [0 … 360]° longitude range.
         */
        if (compareMetadata) {
            if (!Objects.equals(this.abbreviation, that.abbreviation) ||
                !Objects.equals(this.rangeMeaning, that.rangeMeaning) ||
                Double.doubleToLongBits(minimum) != Double.doubleToLongBits(that.minimum) ||
                Double.doubleToLongBits(maximum) != Double.doubleToLongBits(that.maximum))
            {
                return false;
            }
        } else {
            /*
             * Checking the abbreviation is not sufficient. For example the polar angle and the
             * spherical latitude have the same abbreviation (θ). SIS names like "Longitude"
             * (in addition to ISO 19111 "Geodetic longitude") bring more potential confusion.
             * Furthermore, not all implementors use the greek letters. For example most CRS in
             * WKT format use the "Lat" abbreviation instead of the greek letter φ.
             * For comparisons without metadata, we ignore the unreliable abbreviation and check
             * the axis name instead. These names are constrained by ISO 19111 specification
             * (see class javadoc), so they should be reliable enough.
             *
             * Note: there is no need to execute this block if 'compareMetadata' is true,
             *       because in this case a stricter check has already been performed by
             *       the 'equals' method in the superclass.
             */
            final String thatName = that.getName().getCode();
            if (!nameMatches(thatName)) {
                /*
                 * The above test checked for special cases ("Lat" / "Lon" aliases, etc.).
                 * The next line may not, but is tested anyway in case the user overridden
                 * the 'that.nameMatches(...)' method.
                 */
                final String thisName = getName().getCode();
                if (!IdentifiedObjects.nameMatches(that, thisName)) {
                    /*
                     * For the needs of CoordinateSystems.axisColinearWith(...), we must stop here.
                     * In addition it may be safer to not test 'nameMatchesXY' when we don't have
                     * the extra-safety of units comparison, because "x" and "y" names are too generic.
                     */
                    if (!compareUnit) {
                        return false;
                    }
                    // Last chance: check for the special case of "x" and "y" axis names.
                    if (!nameMatchesXY(thatName, thisName) && !nameMatchesXY(thisName, thatName)) {
                        return false;
                    }
                }
            }
        }
        return Objects.equals(direction, that.direction) && (!compareUnit || Objects.equals(unit, that.unit));
    }

    /**
     * Computes a hash value consistent with the given comparison mode.
     *
     * @return The hash code value for the given comparison mode.
     */
    @Override
    public int hashCode(final ComparisonMode mode) throws IllegalArgumentException {
        int code = super.hashCode(mode);
        if (unit      != null) code = 31*code + unit     .hashCode();
        if (direction != null) code = 31*code + direction.hashCode();
        if (mode == ComparisonMode.STRICT) {
            code = Numerics.hash(minimum, code);
            code = Numerics.hash(maximum, code);
        }
        return code;
    }

    /**
     * Formats the inner part of a <cite>Well Known Text</cite> (WKT) element.
     *
     * @param  formatter The formatter to use.
     * @return The WKT element name, which is {@code "AXIS"}.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        formatter.append(direction);
        return "AXIS";
    }
}
