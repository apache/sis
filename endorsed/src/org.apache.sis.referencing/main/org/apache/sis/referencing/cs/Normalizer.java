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
import java.util.Arrays;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.IncommensurableException;
import org.opengis.referencing.cs.RangeMeaning;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.cs.CylindricalCS;
import org.opengis.referencing.cs.PolarCS;
import static org.opengis.referencing.IdentifiedObject.NAME_KEY;
import static org.opengis.referencing.IdentifiedObject.IDENTIFIERS_KEY;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.internal.shared.AxisDirections;
import org.apache.sis.measure.Units;


/*
 * The identifier for axis of unknown name. We have to use this identifier when the axis direction changed,
 * because such change often implies a name change too (e.g. "Westing" → "Easting"), and we cannot always
 * guess what the new name should be. This constant is used as a sentinel value set by Normalizer and checked
 * by DefaultCoordinateSystemAxis for skipping axis name comparisons when the axis name is unknown.
 */
import static org.apache.sis.referencing.internal.shared.NilReferencingObject.UNNAMED;


/**
 * Derives a coordinate system from an existing one for a given {@link AxesConvention}.
 * The main usage for this class is to reorder the axes in some fixed order like
 * (<var>x</var>, <var>y</var>, <var>z</var>) or (<var>longitude</var>, <var>latitude</var>).
 *
 * <p>The normalization performed by this class shall be compatible with axis order expected by various
 * {@code MathTransform} implementations in the {@link org.apache.sis.referencing.operation.transform} package.
 * In particular:</p>
 *
 * <ul>
 *   <li>{@code EllipsoidToCentricTransform} input:<ol>
 *     <li>Geodetic longitude (λ) in degrees</li>
 *     <li>Geodetic latitude (φ) in degrees</li>
 *     <li>Height in units of semi-axes</li>
 *   </ol></li>
 *   <li>{@code SphericalToCartesian} input:<ol>
 *     <li>Spherical longitude in degrees</li>
 *     <li>Spherical latitude in degrees</li>
 *     <li>Spherical radius (r) in any units</li>
 *   </ol></li>
 *   <li>{@code CartesianToSpherical} input:<ol>
 *     <li>X in units of the above radius</li>
 *     <li>Y in units of the above radius</li>
 *     <li>Z in units of the above radius</li>
 *   </ol></li>
 *   <li>{@code CylindricalToCartesian} input:<ol>
 *     <li>Radius (r) in any units</li>
 *     <li>Angle (θ) in degrees</li>
 *     <li>Height (z) in any units</li>
 *   </ol></li>
 * </ul>
 *
 * <p>This class implements {@link Comparable} for opportunist reasons.
 * This should be considered as an implementation details.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
final class Normalizer implements Comparable<Normalizer> {
    /**
     * The properties to exclude in calls to {@link IdentifiedObjects#getProperties(IdentifiedObject, String...)}.
     */
    private static final String[] EXCLUDES = {
        IDENTIFIERS_KEY
    };

    /**
     * Number of bits by which to shift the {@link AxisDirection#ordinal()} value in order to make room for
     * inserting intermediate values between them. A shift of 3 makes room for {@literal 1 << 3} intermediate
     * values. Those intermediate values are declared in the {@link #ORDER} map.
     *
     * @see #order(AxisDirection)
     */
    private static final int SHIFT = 3;

    /**
     * Custom code list values to handle as if the where defined between two GeoAPI values.
     *
     * @see #order(AxisDirection)
     */
    private static final Map<AxisDirection,Integer> ORDER = new HashMap<>(12);
    static {
        // Get ordinal of last compass direction defined by GeoAPI. We will continue on the horizontal plane.
        int code = (AxisDirection.NORTH.ordinal() + (AxisDirections.COMPASS_COUNT - 1)) << SHIFT;
        for (final AxisDirection d : new AxisDirection[] {
            AxisDirections.FORWARD,
            AxisDirections.STARBOARD,
            AxisDirections.COUNTER_CLOCKWISE,
            AxisDirections.CLOCKWISE,
            AxisDirections.AWAY_FROM
        }) ORDER.put(d, ++code);
        // Set the time coordinate as the last coordinate in all cases.
        ORDER.put(AxisDirection.PAST,   (Integer.MAX_VALUE >>> 1) - 1);
        ORDER.put(AxisDirection.FUTURE, (Integer.MAX_VALUE >>> 1));
    }

    /**
     * The axis to be compared by {@link #compareTo(Normalizer)}.
     */
    private final CoordinateSystemAxis axis;

    /**
     * The direction along meridian, or {@code null} if none. This is inferred from {@link #axis}
     * at construction time in order to compute it only once before to sort an array of axes.
     */
    private final DirectionAlongMeridian meridian;

    /**
     * Angular units order relative to other units.
     * A value of -1 means that angular units should be first.
     * A value of +1 means than angular units should be last.
     * A value of 0 means to not use this criterion.
     */
    private final int unitOrder;

    /**
     * For internal usage by {@link #sort(CoordinateSystemAxis[], int)} only.
     */
    private Normalizer(final CoordinateSystemAxis axis, final int angularUnitOrder) {
        this.axis = axis;
        unitOrder = Units.isAngular(axis.getUnit()) ? angularUnitOrder : 0;
        final AxisDirection dir = axis.getDirection();
        meridian = AxisDirections.isUserDefined(dir) ? DirectionAlongMeridian.parse(dir) : null;
    }

    /**
     * Returns the order of the given axis direction.
     */
    private static int order(final AxisDirection dir) {
        final Integer p = ORDER.get(dir);
        return (p != null) ? p : (dir.ordinal() << SHIFT);
    }

    /**
     * Compares two axis for an order that try to favor right-handed coordinate systems.
     * Compass directions like North and East are first. Vertical directions like Up or Down are next.
     */
    @Override
    public int compareTo(final Normalizer that) {
        int d = unitOrder - that.unitOrder;
        if (d == 0) {
            final AxisDirection d1 = this.axis.getDirection();
            final AxisDirection d2 = that.axis.getDirection();
            if ((d = AxisDirections.angleForCompass(d2, d1)) == Integer.MIN_VALUE &&
                (d = AxisDirections.angleForVehicle(d2, d1)) == Integer.MIN_VALUE)
            {
                if (meridian != null) {
                    if (that.meridian != null) {
                        d = meridian.compareTo(that.meridian);
                    } else {
                        d = -1;
                    }
                } else if (that.meridian != null) {
                    d = +1;
                } else {
                    d = order(d1) - order(d2);
                }
            }
        }
        return d;
    }

    /**
     * Sorts the specified axes in an attempt to create a right-handed system.
     * The sorting is performed in place. This method returns {@code true} if
     * at least one axis moved as result of this method call.
     *
     * @param axes              the axes to sort.
     * @param angularUnitOrder  -1 for sorting angular units first, +1 for sorting them last, or 0 if neutral.
     */
    static boolean sort(final CoordinateSystemAxis[] axes, final int angularUnitOrder) {
        final var wrappers = new Normalizer[axes.length];
        for (int i=0; i<axes.length; i++) {
            wrappers[i] = new Normalizer(axes[i], angularUnitOrder);
        }
        Arrays.sort(wrappers);
        boolean changed = false;
        for (int i=0; i<axes.length; i++) {
            final CoordinateSystemAxis a = wrappers[i].axis;
            changed |= (axes[i] != a);
            axes[i] = a;
        }
        return changed;
    }

    /**
     * Returns a new axis with the same properties (except identifiers) than given axis,
     * but with normalized axis direction and unit of measurement.
     *
     * @param  axis     the axis to normalize.
     * @param  changes  the change to apply on axis direction and units.
     * @return an axis using normalized direction and units, or {@code axis} if there is no change.
     */
    static CoordinateSystemAxis normalize(final CoordinateSystemAxis axis, final AxisFilter changes) {
        final Unit<?>       unit      = axis.getUnit();
        final AxisDirection direction = axis.getDirection();
        final Unit<?>       newUnit   = changes.getUnitReplacement(axis, unit);
        final AxisDirection newDir    = changes.getDirectionReplacement(axis, direction);
        /*
         * Reuse some properties (name, remarks, etc.) from the existing axis. If the direction changed,
         * then the axis name may need change too (e.g. "Westing" → "Easting"). The new axis name may be
         * set to "Unnamed", but the caller will hopefully be able to replace the returned instance by
         * an instance from the EPSG database with appropriate name.
         */
        final boolean sameDirection = newDir.equals(direction);
        if (sameDirection && newUnit.equals(unit)) {
            return axis;
        }
        final String abbreviation = axis.getAbbreviation();
        final String newAbbr = sameDirection ? abbreviation :
                AxisDirections.suggestAbbreviation(axis.getName().getCode(), newDir, newUnit);
        final var properties = new HashMap<String,Object>(8);
        if (newAbbr.equals(abbreviation)) {
            properties.putAll(IdentifiedObjects.getProperties(axis, EXCLUDES));
        } else {
            properties.put(NAME_KEY, UNNAMED);
        }
        /*
         * Convert the axis range and build the new axis. The axis range will be converted only if
         * the axis direction is the same or the opposite, otherwise we do not know what should be
         * the new values. In the particular case of opposite axis direction, we need to reverse the
         * sign of minimum and maximum values.
         */
        if (sameDirection || newDir.equals(AxisDirections.opposite(direction))) {
            final UnitConverter c;
            try {
                c = unit.getConverterToAny(newUnit);
            } catch (IncommensurableException e) {
                // Use IllegalStateException because the public API is an AbstractCS member method.
                throw new IllegalStateException(Resources.format(Resources.Keys.IllegalUnitFor_2, "axis", unit), e);
            }
            double minimum = c.convert(axis.getMinimumValue());
            double maximum = c.convert(axis.getMaximumValue());
            if (!sameDirection) {
                final double tmp = minimum;
                minimum = -maximum;
                maximum = -tmp;
            }
            properties.put(DefaultCoordinateSystemAxis.MINIMUM_VALUE_KEY, minimum);
            properties.put(DefaultCoordinateSystemAxis.MAXIMUM_VALUE_KEY, maximum);
            properties.put(DefaultCoordinateSystemAxis.RANGE_MEANING_KEY, axis.getRangeMeaning());
        }
        return new DefaultCoordinateSystemAxis(properties, newAbbr, newDir, newUnit);
    }

    /**
     * Optionally normalizes and reorders the axes in an attempt to get a right-handed system.
     * If no axis change is needed, then this method returns {@code null}.
     *
     * @param  cs       the coordinate system to normalize.
     * @param  changes  the change to apply on axis direction and units.
     * @param  reorder  {@code true} for reordering the axis for a right-handed coordinate system.
     * @return the normalized coordinate system, or {@code null} if no normalization is needed.
     */
    static AbstractCS normalize(final CoordinateSystem cs, final AxisFilter changes, final boolean reorder) {
        final int dimension = cs.getDimension();
        /*
         * Get the axes to retain, without normalizing them yet. We keep a list of
         * axes before normalization in order to detect which axes have been reused
         * and whether reused axes are in the same order as before.
         */
        final var oldAxes = new CoordinateSystemAxis[dimension];
        int n = 0;
        for (int i=0; i<dimension; i++) {
            final CoordinateSystemAxis axis = cs.getAxis(i);
            if (changes == null || changes.accept(axis)) {
                oldAxes[n++] = axis;
            }
        }
        /*
         * Normalize axis units and directions, without changing axis order yet.
         * We need to normalize direction before to check for axis order because
         * change in axis direction can change whether axes are right-handed.
         */
        final CoordinateSystemAxis[] newAxes = Arrays.copyOf(oldAxes, n);
        boolean changed = false;
        if (changes != null) {
            for (int i=0; i<n; i++) {
                newAxes[i] = normalize(newAxes[i], changes);
                changed |= (newAxes[i] != oldAxes[i]);
            }
        }
        /*
         * Sort the axes in an attempt to create a right-handed system.
         * If nothing changed, return the given Coordinate System as-is.
         */
        if (reorder) {
            int angularUnitOrder = 0;
            if  (cs instanceof EllipsoidalCS || cs instanceof SphericalCS) angularUnitOrder = -1;      // (λ,φ,h) order
            else if (cs instanceof CylindricalCS || cs instanceof PolarCS) angularUnitOrder = +1;      // (r,θ) order
            changed |= sort(newAxes, angularUnitOrder);
            if (angularUnitOrder == 1) {                            // Cylindrical or polar
                /*
                 * Change (r,z,θ) to (r,θ,z) order in CylindricalCS. The check on unit of
                 * measurements should be always true, but we verify as a paranoiac check.
                 */
                if (newAxes.length == 3 && isLengthAndAngle(newAxes, 1)) {
                    ArraysExt.swap(newAxes, 1, 2);
                }
                /*
                 * If we were not allowed to normalize the axis direction, we may have a
                 * left-handed coordinate system here. If so, make it right-handed.
                 */
                if (newAxes[1].getDirection() == AxisDirections.CLOCKWISE && isLengthAndAngle(newAxes, 0)) {
                    ArraysExt.swap(newAxes, 0, 1);
                }
            }
        }
        if (!changed && n == dimension) {
            return null;
        }
        /*
         * Verify is some axes have been reused as-is but in different order. The EPSG database uses different codes
         * for the same axis at different index. If we changed the position of an axis, then we remove its EPSG code
         * since it is no longer correct. Actually we conservatively remove all identifiers, not only EPSG ones, for
         * simplicity, consistency with handling of identifiers elsewhere in this class and because we don't know if
         * other identifier namespaces depend on axis order like EPSG ones.
         */
        for (int i=0; i<n; i++) {
            final CoordinateSystemAxis axis = newAxes[i];
            if (!axis.getIdentifiers().isEmpty()) {             // If the axis has no identifier, nothing to remove.
                for (int j=0; j<n; j++) {
                    if (j != i && axis == oldAxes[j]) {
                        newAxes[i] = forRange(axis, axis.getMinimumValue(), axis.getMaximumValue());
                    }
                }
            }
        }
        /*
         * Create a new coordinate system of the same type as the given one, but with the given axes.
         * We need to change the Coordinate System name, since it is likely to not be valid anymore.
         */
        final AbstractCS impl = castOrCopy(cs);
        final var buffer = (StringBuilder) CharSequences.camelCaseToSentence(impl.getInterface().getSimpleName());
        final String name = AxisDirections.appendTo(buffer, newAxes);
        return impl.createForAxes(name, newAxes);
    }

    /**
     * Returns {@code true} if the units of measurement at the given position is a linear unit,
     * followed by an angular unit on the next axis.
     */
    private static boolean isLengthAndAngle(final CoordinateSystemAxis[] axes, final int p) {
        return Units.isLinear(axes[p].getUnit()) && Units.isAngular(axes[p+1].getUnit());
    }

    /**
     * Returns a coordinate system with the same axes as the given CS, except that the wraparound axes
     * are shifted to a range of positive values. This method can be used in order to shift between the
     * [-180 … +180]° and [0 … 360]° ranges of longitude values.
     *
     * <p>This method shifts the axis {@linkplain CoordinateSystemAxis#getMinimumValue() minimum} and
     * {@linkplain CoordinateSystemAxis#getMaximumValue() maximum} values by a multiple of half the range
     * (typically 180°). This method does not change the meaning of coordinate values. For example, a longitude
     * of -60° still locate the same point in the old and the new coordinate system. But the preferred way to
     * locate that point become the 300° value if the longitude range has been shifted to positive values.</p>
     *
     * @param  cs  the coordinate system to shift.
     * @return a coordinate system using the given kind of longitude range, or {@code null} if no change is needed.
     */
    private static AbstractCS shiftAxisRange(final CoordinateSystem cs) {
        boolean changed = false;
        final var axes = new CoordinateSystemAxis[cs.getDimension()];
        for (int i=0; i<axes.length; i++) {
            CoordinateSystemAxis axis = cs.getAxis(i);
            if (axis.getRangeMeaning() == RangeMeaning.WRAPAROUND) {
                double min = axis.getMinimumValue();
                if (min < 0) {
                    double max = axis.getMaximumValue();
                    double offset = (max - min) / 2;
                    offset *= Math.floor(min/offset + Numerics.COMPARISON_THRESHOLD);
                    min -= offset;
                    max -= offset;
                    if (min < max) { // Paranoiac check, but also a way to filter NaN values when offset is infinite.
                        axis = forRange(axis, min, max);
                        changed = true;
                    }
                }
            }
            axes[i] = axis;
        }
        if (!changed) {
            return null;
        }
        return castOrCopy(cs).createForAxes(null, axes);
    }

    /**
     * Returns a new axis with the same properties as the given axis except the identifiers which are omitted,
     * and the minimum and maximum values which are set to the given values.
     */
    private static CoordinateSystemAxis forRange(final CoordinateSystemAxis axis, final double min, final double max) {
        final var properties = new HashMap<String,Object>(8);
        properties.putAll(IdentifiedObjects.getProperties(axis, EXCLUDES));
        properties.put(DefaultCoordinateSystemAxis.MINIMUM_VALUE_KEY, min);
        properties.put(DefaultCoordinateSystemAxis.MAXIMUM_VALUE_KEY, max);
        properties.put(DefaultCoordinateSystemAxis.RANGE_MEANING_KEY, axis.getRangeMeaning());
        return new DefaultCoordinateSystemAxis(properties, axis.getAbbreviation(), axis.getDirection(), axis.getUnit());
    }

    /**
     * Returns the given coordinate system as an {@code AbstractCS} instance. This method performs an
     * {@code instanceof} check before to delegate to {@link AbstractCS#castOrCopy(CoordinateSystem)}
     * because there is no need to check for all interfaces before the implementation class here.
     * Checking the implementation class first is usually more efficient in this particular case.
     */
    private static AbstractCS castOrCopy(final CoordinateSystem cs) {
        return (cs instanceof AbstractCS) ? (AbstractCS) cs : AbstractCS.castOrCopy(cs);
    }

    /**
     * Returns a coordinate system equivalent to the given one but with axes rearranged according the given convention.
     * If the given coordinate system is already compatible with the given convention, then returns {@code null}.
     *
     * @param  convention  the axes convention for which a coordinate system is desired.
     * @return a coordinate system compatible with the given convention, or {@code null} if no change is needed.
     *
     * @see AbstractCS#forConvention(AxesConvention)
     */
    static AbstractCS forConvention(final CoordinateSystem cs, final AxesConvention convention) {
        switch (convention) {
            case NORMALIZED:       // Fall through
            case DISPLAY_ORIENTED: return normalize(cs, convention, true);
            case RIGHT_HANDED:     return normalize(cs, null, true);
            case POSITIVE_RANGE:   return shiftAxisRange(cs);
            default: throw new AssertionError(convention);
        }
    }
}
