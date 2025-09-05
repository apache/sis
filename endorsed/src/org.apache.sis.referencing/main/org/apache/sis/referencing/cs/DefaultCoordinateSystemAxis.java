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
import java.util.Locale;
import java.util.Objects;
import static java.lang.Double.doubleToLongBits;
import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.POSITIVE_INFINITY;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.quantity.Angle;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.cs.RangeMeaning;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.PolarCS;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.metadata.privy.AxisNames;
import org.apache.sis.metadata.privy.ImplementationHelper;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.privy.WKTKeywords;
import org.apache.sis.referencing.privy.AxisDirections;
import org.apache.sis.measure.Longitude;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Units;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.xml.bind.Context;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.io.wkt.ElementKind;
import org.apache.sis.io.wkt.Transliterator;
import org.apache.sis.io.wkt.FormattableObject;
import static org.apache.sis.util.ArgumentChecks.*;
import static org.apache.sis.util.collection.Containers.property;


/*
 * The identifier for axis of unknown name. We have to use this identifier when the axis direction changed,
 * because such change often implies a name change too (e.g. "Westing" → "Easting"), and we cannot always
 * guess what the new name should be. This constant is used as a sentinel value set by Normalizer and checked
 * by DefaultCoordinateSystemAxis for skipping axis name comparisons when the axis name is unknown.
 */
import static org.apache.sis.referencing.privy.NilReferencingObject.UNNAMED;


/**
 * Coordinate system axis name, direction, unit and range of values.
 *
 * <h2>Axis names</h2>
 * In some case, the axis name is constrained by ISO 19111 depending on the
 * {@linkplain org.opengis.referencing.crs.CoordinateReferenceSystem coordinate reference system} type.
 * This constraint works in two directions. For example, the names <q>geodetic latitude</q> and
 * <q>geodetic longitude</q> shall be used to designate the coordinate axis names associated
 * with a {@link org.opengis.referencing.crs.GeographicCRS}. Conversely, these names shall not be used
 * in any other context. See the GeoAPI {@link CoordinateSystemAxis} javadoc for more information.
 *
 * <h2>Immutability and thread safety</h2>
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself)
 * given to the constructor are also immutable. Unless otherwise noted in the javadoc, this condition holds if all
 * components were created using only SIS factories and static constants.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.4
 *
 * @see AbstractCS
 * @see Unit
 *
 * @since 0.4
 */
@XmlType(name = "CoordinateSystemAxisType", propOrder = {
    "abbreviation",
    "direction",
    "minimum",
    "maximum",
    "rangeMeaning"
})
@XmlRootElement(name = "CoordinateSystemAxis")
public class DefaultCoordinateSystemAxis extends AbstractIdentifiedObject implements CoordinateSystemAxis {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -7883614853277827689L;

    /**
     * Key for the <code>{@value}</code> property to be given to the constructor.
     * This is used for setting the value to be returned by {@link #getMinimumValue()}.
     */
    public static final String MINIMUM_VALUE_KEY = "minimumValue";

    /**
     * Key for the <code>{@value}</code> property to be given to the constructor.
     * This is used for setting the value to be returned by {@link #getMaximumValue()}.
     */
    public static final String MAXIMUM_VALUE_KEY = "maximumValue";

    /**
     * Key for the <code>{@value}</code> property to be given to the constructor.
     * This is used for setting the value to be returned by {@link #getRangeMeaning()}.
     */
    public static final String RANGE_MEANING_KEY = "rangeMeaning";

    /**
     * Some names to be treated as equivalent. This is needed because axis names are the primary way to
     * distinguish between {@link CoordinateSystemAxis} instances. Those names are strictly defined by
     * ISO 19111 as "Geodetic latitude" and "Geodetic longitude" among others, but the legacy WKT
     * specifications from OGC 01-009 defined the names as "Lon" and "Lat" for the same axis.
     *
     * <p>Keys in this map are names <strong>in lower cases</strong>.
     * Values are any object that allow us to differentiate latitude from longitude.</p>
     *
     * <p>Similar strings appear in {@link #formatTo(Formatter)} and
     * {@code org.apache.sis.io.wkt.GeodeticObjectParser.parseAxis(…)}.</p>
     *
     * @see #isHeuristicMatchForName(String)
     */
    private static final Map<String,Object> ALIASES = Map.of(
            "lat",                Boolean.TRUE,
            "latitude",           Boolean.TRUE,
            "geodetic latitude",  Boolean.TRUE,
            "lon",                Boolean.FALSE,
            "long",               Boolean.FALSE,
            "longitude",          Boolean.FALSE,
            "geodetic longitude", Boolean.FALSE);
    /*
     * Do not add aliases for "x" and "y" in this map. See ALIASES_XY for more information.
     */

    /**
     * Returns a value of {@link #ALIASES} map for the given axis name.
     */
    private static Object getAliasType(final String name) {
        return ALIASES.get(name.strip().toLowerCase(Locale.US));   // Our ALIASES are in English.
    }

    /**
     * Aliases for the "x" and "y" abbreviations (special cases). "x" and "y" are sometimes used (especially in WKT)
     * for meaning "Easting" and "Northing". However, we shall not add "x" and "y" as aliases in the {@link #ALIASES}
     * map, because experience has shown that doing so cause a lot of undesirable side effects. The "x" abbreviation
     * is used for too many things ("Easting", "Westing", "Geocentric X", "Display right", "Display left") and likewise
     * for "y". Declaring them as aliases introduces confusion in many places. Instead, the "x" and "y" cases are
     * handled in a special way by the {@code isHeuristicMatchForNameXY(…)} method.
     *
     * <p>Names at even index are for "x" and names at odd index are for "y".</p>
     *
     * @see #isHeuristicMatchForNameXY(String, String)
     */
    private static final String[] ALIASES_XY = {
        AxisNames.EASTING, AxisNames.NORTHING,
        AxisNames.WESTING, AxisNames.SOUTHING
    };

    /**
     * The abbreviation used for this coordinate system axes.
     * Examples are <q>X</q> and <q>Y</q>.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setAbbreviation(String)}</p>
     *
     * @see #getAbbreviation()
     */
    private String abbreviation;

    /**
     * Direction of this coordinate system axis. In the case of Cartesian projected
     * coordinates, this is the direction of this coordinate system axis locally.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setDirection(AxisDirection)}</p>
     *
     * @see #getDirection()
     */
    private AxisDirection direction;

    /**
     * The unit of measure used for this coordinate system axis.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setUnit(Unit)}</p>
     *
     * @see #getUnit()
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    private Unit<?> unit;

    /**
     * Minimal and maximal value for this axis, or negative/positive infinity if none.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setMinimum(Double)}
     * or {@link #setMaximum(Double)}</p>
     */
    private double minimumValue, maximumValue;

    /**
     * The range meaning for this axis, or {@code null} if unspecified.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setRangeMeaning(RangeMeaning)}</p>
     *
     * @see #getRangeMeaning()
     */
    private RangeMeaning rangeMeaning;

    /**
     * Constructs an axis from a set of properties. The properties given in argument follow the same rules
     * than for the {@linkplain AbstractIdentifiedObject#AbstractIdentifiedObject(Map) super-class constructor}.
     * Additionally, the following properties are understood by this constructor:
     *
     * <table class="sis">
     *   <caption>Recognized properties (non exhaustive list)</caption>
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr><tr>
     *     <td>{@value #MINIMUM_VALUE_KEY}</td>
     *     <td>{@link Number}</td>
     *     <td>{@link #getMinimumValue()}</td>
     *   </tr><tr>
     *     <td>{@value #MAXIMUM_VALUE_KEY}</td>
     *     <td>{@link Number}</td>
     *     <td>{@link #getMaximumValue()}</td>
     *   </tr><tr>
     *     <td>{@value #RANGE_MEANING_KEY}</td>
     *     <td>{@link RangeMeaning}</td>
     *     <td>{@link #getRangeMeaning()}</td>
     *   </tr><tr>
     *     <th colspan="3" class="hsep">Defined in parent class (reminder)</th>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link Identifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link Identifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     * </table>
     *
     * Generally speaking, information provided in the {@code properties} map are considered ignorable metadata
     * (except the axis name) while information provided as explicit arguments may have an impact on coordinate
     * transformation results. Exceptions to this rule are the {@code minimumValue} and {@code maximumValue} in
     * the particular case where {@code rangeMeaning} is {@link RangeMeaning#WRAPAROUND}.
     *
     * <p>If no minimum, maximum and range meaning are specified, then this constructor will infer them
     * from the axis unit and direction.</p>
     *
     * @param properties    the properties to be given to the identified object.
     * @param abbreviation  the {@linkplain #getAbbreviation() abbreviation} used for this coordinate system axis.
     * @param direction     the {@linkplain #getDirection() direction} of this coordinate system axis.
     * @param unit          the {@linkplain #getUnit() unit of measure} used for this coordinate system axis.
     *
     * @see org.apache.sis.referencing.factory.GeodeticObjectFactory#createCoordinateSystemAxis(Map, String, AxisDirection, Unit)
     */
    public DefaultCoordinateSystemAxis(final Map<String,?> properties,
                                       final String        abbreviation,
                                       final AxisDirection direction,
                                       final Unit<?>       unit)
    {
        super(properties);
        this.abbreviation = abbreviation;
        this.direction    = direction;
        this.unit         = unit;
        ensureNonEmpty("abbreviation", abbreviation);
        ensureNonNull ("direction",    direction);
        ensureNonNull ("unit",         unit);
        Number  minimum = property(properties, MINIMUM_VALUE_KEY, Number.class);
        Number  maximum = property(properties, MAXIMUM_VALUE_KEY, Number.class);
        RangeMeaning rm = property(properties, RANGE_MEANING_KEY, RangeMeaning.class);
        if (minimum == null && maximum == null && rm == null) {
            double min = Double.NEGATIVE_INFINITY;
            double max = Double.POSITIVE_INFINITY;
            if (Units.isAngular(unit)) {
                final UnitConverter fromDegrees = Units.DEGREE.getConverterTo(unit.asType(Angle.class));
                final AxisDirection dir = AxisDirections.absolute(direction);
                if (dir.equals(AxisDirection.NORTH)) {
                    min = fromDegrees.convert(Latitude.MIN_VALUE);
                    max = fromDegrees.convert(Latitude.MAX_VALUE);
                    rm  = RangeMeaning.EXACT;
                } else if (dir.equals(AxisDirection.EAST)) {
                    min = fromDegrees.convert(Longitude.MIN_VALUE);
                    max = fromDegrees.convert(Longitude.MAX_VALUE);
                    rm  = RangeMeaning.WRAPAROUND;                                  // 180°E wraps to 180°W
                }
                if (min > max) {
                    final double t = min;
                    min = max;
                    max = t;
                }
            }
            minimumValue = min;
            maximumValue = max;
        } else {
            minimumValue = (minimum != null) ? minimum.doubleValue() : Double.NEGATIVE_INFINITY;
            maximumValue = (maximum != null) ? maximum.doubleValue() : Double.POSITIVE_INFINITY;
            if (!(minimumValue < maximumValue)) { // Use '!' for catching NaN
                throw new IllegalArgumentException(Errors.forProperties(properties).getString(
                        Errors.Keys.IllegalRange_2, minimumValue, maximumValue));
            }
            if ((minimumValue != NEGATIVE_INFINITY) || (maximumValue != POSITIVE_INFINITY)) {
                ensureNonNull(RANGE_MEANING_KEY, rm);
            } else {
                rm = null;
            }
        }
        rangeMeaning = rm;
    }

    /**
     * Creates a new coordinate system axis with the same values as the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param  axis  the coordinate system axis to copy.
     *
     * @see #castOrCopy(CoordinateSystemAxis)
     */
    protected DefaultCoordinateSystemAxis(final CoordinateSystemAxis axis) {
        super(axis);
        abbreviation = axis.getAbbreviation();
        direction    = axis.getDirection();
        unit         = axis.getUnit();
        minimumValue = axis.getMinimumValue();
        maximumValue = axis.getMaximumValue();
        rangeMeaning = axis.getRangeMeaning();
    }

    /**
     * Returns a SIS axis implementation with the same values as the given arbitrary implementation.
     * If the given object is {@code null}, then this method returns {@code null}. Otherwise if the
     * given object is already a SIS implementation, then the given object is returned unchanged.
     * Otherwise a new SIS implementation is created and initialized to the values of the given object.
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultCoordinateSystemAxis castOrCopy(final CoordinateSystemAxis object) {
        return (object == null) || (object instanceof DefaultCoordinateSystemAxis)
                ? (DefaultCoordinateSystemAxis) object : new DefaultCoordinateSystemAxis(object);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code CoordinateSystemAxis.class}.
     *
     * <h4>Note for implementers</h4>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code CoordinateSystemAxis}
     * sub-interface. Overriding possibility is left mostly for implementers who wish to extend GeoAPI with their own
     * set of interfaces.
     *
     * @return {@code CoordinateSystemAxis.class} or a user-defined sub-interface.
     */
    @Override
    public Class<? extends CoordinateSystemAxis> getInterface() {
        return CoordinateSystemAxis.class;
    }

    /**
     * Returns the direction of this coordinate system axis.
     * This direction is often approximate and intended to provide a human interpretable meaning to the axis.
     * A {@linkplain AbstractCS coordinate system} cannot contain two axes having the same direction or
     * opposite directions.
     *
     * <p>Examples:
     * {@linkplain AxisDirection#NORTH north} or {@linkplain AxisDirection#SOUTH south},
     * {@linkplain AxisDirection#EAST  east}  or {@linkplain AxisDirection#WEST  west},
     * {@linkplain AxisDirection#UP    up}    or {@linkplain AxisDirection#DOWN  down}.</p>
     *
     * @return the direction of this coordinate system axis.
     */
    @Override
    @XmlElement(name = "axisDirection", required = true)
    public AxisDirection getDirection() {
        return direction;
    }

    /**
     * Returns the abbreviation used for this coordinate system axes.
     * Examples are <q>X</q> and <q>Y</q>.
     *
     * @return the coordinate system axis abbreviation.
     */
    @Override
    @XmlElement(name = "axisAbbrev", required = true)
    public String getAbbreviation() {
        return abbreviation;
    }

    /**
     * Returns the unit of measure used for this coordinate system axis. If this {@code CoordinateSystemAxis} was
     * given by <code>{@link AbstractCS#getAxis(int) CoordinateSystem.getAxis}(i)</code>, then all coordinate
     * values at dimension <var>i</var> in a coordinate tuple shall be recorded using this unit of measure.
     *
     * @return the unit of measure used for coordinate values along this coordinate system axis.
     */
    @Override
    @XmlAttribute(name= "uom", required = true)
    public Unit<?> getUnit() {
        return unit;
    }

    /**
     * Returns the minimum value normally allowed for this axis, in the {@linkplain #getUnit()
     * unit of measure for the axis}. If there is no minimum value, then this method returns
     * {@linkplain Double#NEGATIVE_INFINITY negative infinity}.
     *
     * @return the minimum value normally allowed for this axis.
     */
    @Override
    public double getMinimumValue() {
        return minimumValue;
    }

    /**
     * Returns the maximum value normally allowed for this axis, in the {@linkplain #getUnit()
     * unit of measure for the axis}. If there is no maximum value, then this method returns
     * {@linkplain Double#POSITIVE_INFINITY negative infinity}.
     *
     * @return the maximum value normally allowed for this axis.
     */
    @Override
    public double getMaximumValue() {
        return maximumValue;
    }

    /**
     * Invoked at unmarshalling time if a minimum or maximum value is out of range.
     *
     * @param  name   the property name. Will also be used as "method" name for logging purpose,
     *                since the setter method "conceptually" do not exist (it is only for JAXB).
     * @param  value  the invalid value.
     */
    private static void outOfRange(final String name, final Double value) {
        Context.warningOccured(Context.current(), DefaultCoordinateSystemAxis.class, name,
                Errors.class, Errors.Keys.InconsistentAttribute_2, name, value);
    }

    /**
     * Returns the meaning of axis value range specified by the {@linkplain #getMinimumValue() minimum}
     * and {@linkplain #getMaximumValue() maximum} values. If there is no minimum and maximum values
     * (i.e. if those values are {@linkplain Double#NEGATIVE_INFINITY negative infinity} and
     * {@linkplain Double#POSITIVE_INFINITY positive infinity} respectively), then this method returns {@code null}.
     *
     * @return the meaning of axis value range, or {@code null} if unspecified.
     */
    @Override
    @XmlElement(name = "rangeMeaning")
    public RangeMeaning getRangeMeaning() {
        return rangeMeaning;
    }

    /**
     * Returns {@code true} if either the {@linkplain #getName() primary name} or at least
     * one {@linkplain #getAlias() alias} matches the given string according heuristic rules.
     * This method performs the comparison documented in the
     * {@link AbstractIdentifiedObject#isHeuristicMatchForName(String) super-class},
     * with an additional flexibility for latitudes and longitudes:
     *
     * <ul>
     *   <li>{@code "Lat"}, {@code "Latitude"}  and {@code "Geodetic latitude"}  are considered equivalent.</li>
     *   <li>{@code "Lon"}, {@code "Longitude"} and {@code "Geodetic longitude"} are considered equivalent.</li>
     * </ul>
     *
     * The above special cases are needed in order to workaround a conflict in specifications:
     * ISO 19111 states explicitly that the latitude and longitude axis names shall be
     * <q>Geodetic latitude</q> and <q>Geodetic longitude</q>, while the legacy
     * OGC 01-009 (where version 1 of the WKT format is defined) said that the default values shall be
     * <q>Lat</q> and <q>Lon</q>.
     *
     * <h4>Future evolutions</h4>
     * This method implements heuristic rules learned from experience while trying to provide inter-operability
     * with different data producers. Those rules may be adjusted in any future SIS version according experience
     * gained while working with more data producers.
     *
     * @param  name  the name to compare.
     * @return {@code true} if the primary name of at least one alias matches the specified {@code name}.
     */
    @Override
    public boolean isHeuristicMatchForName(final String name) {
        if (super.isHeuristicMatchForName(name)) {
            return true;
        }
        /*
         * The standard comparisons didn't worked. Check for the aliases. Note: we don't test
         * for `isHeuristicMatchForNameXY(…)` here because the "x" and "y" axis names are too
         * generic. We test them only in the `equals(…)` method, which has the extra-safety
         * of units comparison (so less risk to treat incompatible axes as equivalent).
         */
        final Object type = getAliasType(name);
        return (type != null) && (type == getAliasType(getName().getCode()));
    }

    /**
     * Special cases for "x" and "y" names. "x" is considered equivalent to "Easting" or "Westing",
     * but the converse is not true. Note: by avoiding to put "x" in the {@link #ALIASES} map, we
     * avoid undesirable side effects like considering "Easting" as equivalent to "Westing".
     *
     * @param  xy    the name which may be "x" or "y".
     * @param  name  the second name to compare with.
     * @return {@code true} if the second name is equivalent to "x" or "y"
     *         (depending on the {@code xy} value), or {@code false} otherwise.
     */
    private static boolean isHeuristicMatchForNameXY(String xy, String name) {
        xy = xy.strip();
        if (xy.length() == 1) {
            int i = Character.toLowerCase(xy.charAt(0)) - 'x';
            if (i >= 0 && i <= 1) {
                name = name.strip();
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
     * Compares the unit and direction of this axis with the ones of the given axis.
     * The range minimum and maximum values are compared only if {@code cr} is {@code true},
     * i.e. it is caller responsibility to determine if range shall be considered as metadata.
     *
     * @param  that  the axis to compare with this axis.
     * @param  mode  whether the unit comparison is an approximation or exact.
     * @param  cr    {@code true} for comparing also the range minimum and maximum values.
     * @return {@code true} if unit, direction and optionally range extremum are equal.
     */
    private boolean equalsIgnoreMetadata(final CoordinateSystemAxis that, final ComparisonMode mode, final boolean cr) {
        return Objects.equals(getDirection(), that.getDirection()) &&
               Utilities.deepEquals(getUnit(), that.getUnit(), mode) &&
               (!cr || (doubleToLongBits(getMinimumValue()) == doubleToLongBits(that.getMinimumValue()) &&
                        doubleToLongBits(getMaximumValue()) == doubleToLongBits(that.getMaximumValue())));
    }

    /**
     * Compares the specified object with this axis for equality.
     * The strictness level is controlled by the second argument.
     * This method compares the following properties in every cases:
     *
     * <ul>
     *   <li>{@link #getName()}</li>
     *   <li>{@link #getDirection()}</li>
     *   <li>{@link #getUnit()}</li>
     * </ul>
     *
     * In the particular case where {@link #getRangeMeaning()} is {@code WRAPAROUND}, then {@link #getMinimumValue()}
     * and {@link #getMaximumValue()} are considered non-ignorable metadata and will be compared for every modes.
     * All other properties are compared only for modes stricter than {@link ComparisonMode#IGNORE_METADATA}.
     *
     * @param  object  the object to compare to {@code this}.
     * @param  mode    the strictness level of the comparison.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true;                                                // Slight optimization.
        }
        if (!super.equals(object, mode)) {
            return false;
        }
        switch (mode) {
            case STRICT: {
                final DefaultCoordinateSystemAxis that = (DefaultCoordinateSystemAxis) object;
                return Objects.equals(unit,         that.unit)         &&
                       Objects.equals(direction,    that.direction)    &&
                       Objects.equals(abbreviation, that.abbreviation) &&
                       Objects.equals(rangeMeaning, that.rangeMeaning) &&
                       doubleToLongBits(minimumValue) == doubleToLongBits(that.minimumValue) &&
                       doubleToLongBits(maximumValue) == doubleToLongBits(that.maximumValue);
            }
            case BY_CONTRACT: {
                final CoordinateSystemAxis that = (CoordinateSystemAxis) object;
                return equalsIgnoreMetadata(that, mode, true) &&
                       Objects.equals(getAbbreviation(), that.getAbbreviation()) &&
                       Objects.equals(getRangeMeaning(), that.getRangeMeaning());
            }
        }
        /*
         * At this point the comparison is in "ignore metadata" mode. We compare the axis range
         * only if the range meaning is "wraparound" for both axes, because only in such case a
         * coordinate operation may shift some coordinate values (typically ±360° on longitudes).
         */
        final var that = (CoordinateSystemAxis) object;
        if (!equalsIgnoreMetadata(that, mode, this.getRangeMeaning() == RangeMeaning.WRAPAROUND &&
                                              that.getRangeMeaning() == RangeMeaning.WRAPAROUND))
        {
            return false;
        }
        Identifier name = that.getName();
        if (name != UNNAMED) {
            /*
             * Checking the abbreviation is not sufficient. For example, the polar angle and the
             * spherical latitude have the same abbreviation (θ). Legacy names like "Longitude"
             * (in addition to ISO 19111 "Geodetic longitude") bring more potential confusion.
             * Furthermore, not all implementers use the greek letters. For example, most CRS in
             * WKT format use the "Lat" abbreviation instead of the greek letter φ.
             * For comparisons without metadata, we ignore the unreliable abbreviation and check
             * the axis name instead. These names are constrained by ISO 19111 specification
             * (see class javadoc), so they should be reliable enough.
             *
             * Note: there is no need to execute this block if metadata are not ignored,
             *       because in this case a stricter check has already been performed by
             *       the `equals(…)` method in the superclass.
             */
            final String thatCode = name.getCode();
            if (!isHeuristicMatchForName(thatCode)) {
                name = getName();
                if (name != UNNAMED) {
                    /*
                     * The above test checked for special cases ("Lat" / "Lon" aliases, etc.).
                     * The next line may repeat the same check, so we may have a partial waste
                     * of CPU. But we do it anyway for checking the `that` aliases, and also
                     * because the user may have overridden `that.isHeuristicMatchForName(…)`.
                     */
                    final String thisCode = name.getCode();
                    if (!IdentifiedObjects.isHeuristicMatchForName(that, thisCode)) {
                        // Check for the special case of "x" and "y" axis names.
                        if (!isHeuristicMatchForNameXY(thatCode, thisCode) &&
                            !isHeuristicMatchForNameXY(thisCode, thatCode))
                        {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Invoked by {@code hashCode()} for computing the hash code when first needed.
     * See {@link org.apache.sis.referencing.AbstractIdentifiedObject#computeHashCode()}
     * for more information.
     *
     * @return the hash code value. This value may change in any future Apache SIS version.
     *
     * @hidden because nothing new to said.
     */
    @Override
    protected long computeHashCode() {
        return super.computeHashCode() + Objects.hashCode(unit) + Objects.hashCode(direction)
                + doubleToLongBits(minimumValue) + 31*doubleToLongBits(maximumValue);
    }

    /**
     * Returns the enclosing coordinate system, or {@code null} if none. In ISO 19162 compliant WKT the coordinate
     * <strong>reference</strong> system should be the first parent ({@code formatter.getEnclosingElement(1)}) and
     * the coordinate system shall be obtained from that CRS (yes, this is convolved. This is because of historical
     * reasons, since compatibility with WKT 1 was a requirement of WKT 2).
     */
    private static CoordinateSystem getEnclosingCS(final Formatter formatter) {
        final FormattableObject e = formatter.getEnclosingElement(1);
        if (e instanceof CoordinateReferenceSystem) {           // This is what we expect in standard WKT.
            return ((CoordinateReferenceSystem) e).getCoordinateSystem();
        }
        if (e instanceof CoordinateSystem) {                    // Not standard WKT, but conceptually the right thing.
            return (CoordinateSystem) e;
        }
        return null;
    }

    /**
     * Formats this axis as a <i>Well Known Text</i> {@code Axis[…]} element.
     *
     * <h4>Constraints for WKT validity</h4>
     * The ISO 19162 specification puts many constraints on axis names, abbreviations and directions allowed in WKT.
     * Most of those constraints are inherited from ISO 19111 — see {@link CoordinateSystemAxis} javadoc for some of
     * those. The current Apache SIS implementation does not verify whether this axis name and abbreviation are
     * compliant; we assume that the user created a valid axis.
     * The only actions (derived from ISO 19162 rules) taken by this method (by default) are:
     *
     * <ul>
     *   <li>Replace <q>Geodetic latitude</q> and <q>Geodetic longitude</q> names (case insensitive)
     *       by <q>latitude</q> and <q>longitude</q> respectively.</li>
     *   <li>For latitude and longitude axes, replace “φ” and “λ” abbreviations by <var>“B”</var> and <var>“L”</var>
     *       respectively (from German “Breite” and “Länge”, used in academic texts worldwide).
     *       Note that <var>“L”</var> is also the transliteration of Greek letter “lambda” (λ).</li>
     *   <li>In {@link SphericalCS}, replace “φ” and “θ” abbreviations by <var>“U”</var> and <var>“V”</var> respectively.</li>
     *   <li>In {@link PolarCS}, replace “θ” abbreviation by <var>“U”</var>.</li>
     * </ul>
     *
     * The above-cited replacements of name and Greek letters can be controlled by a call to
     * {@link org.apache.sis.io.wkt.WKTFormat#setTransliterator(Transliterator)}.
     *
     * @return {@code "Axis"}.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        final Convention convention = formatter.getConvention();
        final boolean    isWKT1     = (convention.majorVersion() == 1);
        final boolean    isInternal = (convention == Convention.INTERNAL);
        final CoordinateSystem cs   = getEnclosingCS(formatter);
        AxisDirection dir = getDirection();
        String name = IdentifiedObjects.getName(this, formatter.getNameAuthority());
        if (name == null) {
            name = IdentifiedObjects.getName(this, null);
        }
        if (name != null && !isInternal) {
            final String old = name;
            name = formatter.getTransliterator().toShortAxisName(cs, dir, name);
            if (name == null && isWKT1) {
                name = old;                 // WKT 1 does not allow omission of name.
            }
        }
        /*
         * ISO 19162:2015 §7.5.3 suggests to put abbreviation in parentheses, e.g. "Easting (x)".
         * The specification also suggests to write only the abbreviation (e.g. "(X)") in the
         * special case of Geocentric axis, and disallows Greek letters.
         */
        if (!isWKT1) {
            final String a = formatter.getTransliterator().toLatinAbbreviation(cs, dir, getAbbreviation());
            if (a != null && !a.equals(name)) {
                final var buffer = new StringBuilder();
                if (name != null) {
                    buffer.append(name).append(' ');
                }
                name = buffer.append('(').append(a).append(')').toString();
            }
        }
        formatter.append(name, ElementKind.AXIS);
        /*
         * Format the axis direction, optionally followed by a MERIDIAN[…] element
         * if the direction is of the kind "South along 90°N" for instance.
         */
        DirectionAlongMeridian meridian = null;
        if (AxisDirections.isUserDefined(dir)) {
            meridian = DirectionAlongMeridian.parse(dir);
            if (meridian != null) {
                dir = meridian.baseDirection;
                if (isWKT1) {
                    formatter.setInvalidWKT(this, null);
                }
            }
        }
        formatter.append(dir);
        formatter.append(meridian);
        /*
         * Formats the axis unit only if the enclosing CRS element does not provide one.
         * If the enclosing CRS provided a contextual unit, then it is assumed to apply
         * to all axes (we do not verify).
         */
        if (!isWKT1) {
            if (cs != null && !convention.isSimplified()) {
                final Order order = Order.create(cs, this);
                if (order != null) {
                    formatter.append(order);
                } else {
                    formatter.setInvalidWKT(cs, null);
                }
            }
            if (!formatter.hasContextualUnit(1)) {
                formatter.append(getUnit());
            }
        }
        return WKTKeywords.Axis;
    }

    /**
     * The {@code ORDER[…]} element to be formatted inside {@code AXIS[…]} element.
     * This is an element of WKT 2 only.
     */
    private static final class Order extends FormattableObject {
        /**
         * The sequence number to format inside the {@code ORDER[…]} element.
         */
        private final int index;

        /**
         * Creates a new {@code ORDER[…]} element for the given axis in the given coordinate system.
         * If this method does not find exactly one instance of the given axis in the given coordinate system,
         * then returns {@code null}. In the latter case, it is caller's responsibility to declare the WKT as invalid.
         *
         * <p>This method is a little bit inefficient since the enclosing {@link AbstractCS#formatTo(Formatter)}
         * method already knows this axis index. But there is currently no API in {@link Formatter} for carrying
         * this information, and we are a little bit reluctant to introduce such API since it would force us to
         * introduce lists in a model which is, for everything else, purely based on trees.</p>
         *
         * @se <a href="https://issues.apache.org/jira/browse/SIS-163">SIS-163</a>
         */
        static Order create(final CoordinateSystem cs, final DefaultCoordinateSystemAxis axis) {
            Order order = null;
            final int dimension = cs.getDimension();
            for (int i=0; i<dimension;) {
                if (cs.getAxis(i++) == axis) {
                    if (order == null) {
                        order = new Order(i);
                    } else {
                        return null;
                    }
                }
            }
            return order;
        }

        /**
         * Creates new {@code ORDER[…]} element for the given sequential number.
         */
        private Order(final int index) {
            this.index = index;
        }

        /**
         * Formats the {@code ORDER[…]} element.
         */
        @Override
        protected String formatTo(final Formatter formatter) {
            formatter.append(index);
            return WKTKeywords.Order;
        }
    }




    /*
     ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
     ┃                                                                                  ┃
     ┃                               XML support with JAXB                              ┃
     ┃                                                                                  ┃
     ┃        The following methods are invoked by JAXB using reflection (even if       ┃
     ┃        they are private) or are helpers for other methods invoked by JAXB.       ┃
     ┃        Those methods can be safely removed if Geographic Markup Language         ┃
     ┃        (GML) support is not needed.                                              ┃
     ┃                                                                                  ┃
     ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
     */

    /**
     * Constructs a new object in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflection.
     */
    private DefaultCoordinateSystemAxis() {
        super(org.apache.sis.referencing.privy.NilReferencingObject.INSTANCE);
        minimumValue = NEGATIVE_INFINITY;
        maximumValue = POSITIVE_INFINITY;
        /*
         * Direction and unit of measurement are mandatory for SIS working. We do not verify their presence here
         * because the verification would have to be done in an `afterMarshal(…)` method and throwing an exception
         * in that method causes the whole unmarshalling to fail. But the CD_CoordinateSystemAxis adapter does some
         * verifications.
         */
    }

    /**
     * Invoked by JAXB at unmarshalling time.
     *
     * @see #getAbbreviation()
     */
    private void setAbbreviation(final String value) {
        if (abbreviation == null) {
            abbreviation = value;
        } else {
            ImplementationHelper.propertyAlreadySet(DefaultCoordinateSystemAxis.class, "setAbbreviation", "abbreviation");
        }
    }

    /**
     * Invoked by JAXB at unmarshalling time.
     *
     * @see #getDirection()
     */
    private void setDirection(final AxisDirection value) {
        if (direction == null) {
            direction = value;
        } else {
            ImplementationHelper.propertyAlreadySet(DefaultCoordinateSystemAxis.class, "setDirection", "direction");
        }
    }

    /**
     * Invoked by JAXB at unmarshalling time.
     *
     * @see #getUnit()
     */
    private void setUnit(final Unit<?> value) {
        if (unit == null) {
            unit = value;
        } else {
            ImplementationHelper.propertyAlreadySet(DefaultCoordinateSystemAxis.class, "setUnit", "unit");
        }
    }

    /**
     * Invoked by JAXB at unmarshalling time.
     *
     * @see #getRangeMeaning()
     */
    private void setRangeMeaning(final RangeMeaning value) {
        if (rangeMeaning == null) {
            rangeMeaning = value;
        } else {
            ImplementationHelper.propertyAlreadySet(DefaultCoordinateSystemAxis.class, "setRangeMeaning", "rangeMeaning");
        }
    }

    /**
     * Invoked by JAXB at marshalling time for fetching the minimum value, or {@code null} if none.
     *
     * @see #getMinimumValue()
     */
    @XmlElement(name = "minimumValue")
    private Double getMinimum() {
        return (minimumValue != NEGATIVE_INFINITY) ? minimumValue : null;
    }

    /**
     * Invoked by JAXB at unmarshalling time for setting the minimum value.
     */
    private void setMinimum(final Double value) {
        if (minimumValue == NEGATIVE_INFINITY) {
            final double min = value; // Apply unboxing.
            if (min < maximumValue) {
                minimumValue = min;
            } else {
                outOfRange("minimumValue", value);
            }
        } else {
            ImplementationHelper.propertyAlreadySet(DefaultCoordinateSystemAxis.class, "setMinimum", "minimumValue");
        }
    }

    /**
     * Invoked by JAXB at marshalling time for fetching the maximum value, or {@code null} if none.
     *
     * @see #getMaximumValue()
     */
    @XmlElement(name = "maximumValue")
    private Double getMaximum() {
        return (maximumValue != POSITIVE_INFINITY) ? maximumValue : null;
    }

    /**
     * Invoked by JAXB at unmarshalling time for setting the maximum value.
     */
    private void setMaximum(final Double value) {
        if (maximumValue == POSITIVE_INFINITY) {
            final double max = value; // Apply unboxing.
            if (max > minimumValue) {
                maximumValue = max;
            } else {
                outOfRange("maximumValue", value);
            }
        } else {
            ImplementationHelper.propertyAlreadySet(DefaultCoordinateSystemAxis.class, "setMaximum", "maximumValue");
        }
    }
}
