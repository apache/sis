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
import java.util.Arrays;
import javax.measure.unit.Unit;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import javax.measure.converter.UnitConverter;
import javax.measure.converter.ConversionException;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.apache.sis.internal.referencing.AxisDirections;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.CharSequences;
import org.apache.sis.measure.Units;

import static java.util.Collections.singletonMap;
import static org.opengis.referencing.IdentifiedObject.NAME_KEY;
import static org.opengis.referencing.IdentifiedObject.IDENTIFIERS_KEY;


/**
 * Derives an coordinate system from an existing one for {@link AxesConvention}.
 * The main usage for this class is to reorder the axes in some fixed order like
 * (<var>x</var>, <var>y</var>, <var>z</var>) or (<var>longitude</var>, <var>latitude</var>).
 *
 * <p>This class implements {@link Comparable} for opportunist reasons.
 * This should be considered as an implementation details.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.2)
 * @version 0.4
 * @module
 */
final class Normalizer implements Comparable<Normalizer> {
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
     * For internal usage by {@link #sort(CoordinateSystemAxis[])} only.
     */
    private Normalizer(final CoordinateSystemAxis axis) {
        this.axis = axis;
        meridian = DirectionAlongMeridian.parse(axis.getDirection());
    }

    /**
     * Compares two axis for an order that try to favor right-handed coordinate systems.
     * Compass directions like North and East are first. Vertical directions like Up or Down are next.
     */
    @Override
    public int compareTo(final Normalizer that) {
        final AxisDirection d1 = this.axis.getDirection();
        final AxisDirection d2 = that.axis.getDirection();
        final int compass = AxisDirections.angleForCompass(d2, d1);
        if (compass != Integer.MIN_VALUE) {
            return compass;
        }
        if (meridian != null) {
            if (that.meridian != null) {
                return meridian.compareTo(that.meridian);
            }
            return -1;
        } else if (that.meridian != null) {
            return +1;
        }
        return d1.ordinal() - d2.ordinal();
    }

    /**
     * Sorts the specified axis in an attempt to create a right-handed system.
     * The sorting is performed in place. This method returns {@code true} if
     * at least one axis moved as result of this method call.
     */
    static boolean sort(final CoordinateSystemAxis[] axis) {
        final Normalizer[] wrappers = new Normalizer[axis.length];
        for (int i=0; i<axis.length; i++) {
            wrappers[i] = new Normalizer(axis[i]);
        }
        Arrays.sort(wrappers);
        boolean changed = false;
        for (int i=0; i<axis.length; i++) {
            final CoordinateSystemAxis a = wrappers[i].axis;
            changed |= (axis[i] != a);
            axis[i] = a;
        }
        return changed;
    }

    /**
     * Returns a new axis with the same properties (except identifiers) than given axis,
     * but with normalized axis direction and unit of measurement.
     *
     * @param  axis The axis to normalize.
     * @return An axis using normalized direction unit, or {@code axis} if the given axis already uses the given unit.
     */
    static CoordinateSystemAxis normalize(final CoordinateSystemAxis axis) {
        /*
         * Normalize the axis direction. For now we do not touch to inter-cardinal directions (e.g. "North-East")
         * because it is not clear which normalization policy would match common usage.
         */
        final AxisDirection direction = axis.getDirection();
        AxisDirection newDir = direction;
        if (!AxisDirections.isIntercardinal(direction)) {
            newDir = AxisDirections.absolute(direction);
        }
        final boolean sameDirection = newDir.equals(direction);
        /*
         * Normalize unit of measurement.
         */
        final Unit<?> unit = axis.getUnit(), newUnit;
        if (Units.isLinear(unit)) {
            newUnit = SI.METRE;
        } else if (Units.isAngular(unit)) {
            newUnit = NonSI.DEGREE_ANGLE;
        } else if (Units.isTemporal(unit)) {
            newUnit = NonSI.DAY;
        } else {
            newUnit = unit;
        }
        /*
         * Reuse some properties (name, remarks, etc.) from the existing axis. If the direction changed,
         * then the axis name may need change too (e.g. "Westing" → "Easting"). The new axis name may be
         * set to "Unnamed", but the caller will hopefully be able to replace the returned instance by
         * an instance from the EPSG database with appropriate name.
         */
        if (sameDirection && newUnit.equals(unit)) {
            return axis;
        }
        final String abbreviation = axis.getAbbreviation();
        String newAbbr = abbreviation;
        if (!sameDirection) {
            if (AxisDirections.isCompass(direction)) {
                if (CharSequences.isAcronymForWords(abbreviation, direction.name())) {
                    if (newDir.equals(AxisDirection.EAST)) {
                        newAbbr = "E";
                    } else if (newDir.equals(AxisDirection.NORTH)) {
                        newAbbr = "N";
                    }
                }
            } else if (newDir.equals(AxisDirection.UP)) {
                newAbbr = "z";
            } else if (newDir.equals(AxisDirection.FUTURE)) {
                newAbbr = "t";
            }
        }
        final Map<String,?> properties;
        if (newAbbr.equals(abbreviation)) {
            properties = IdentifiedObjects.getProperties(axis, IDENTIFIERS_KEY);
        } else {
            properties = singletonMap(NAME_KEY, Vocabulary.format(Vocabulary.Keys.Unnamed));
        }
        /*
         * Converts the axis range and build the new axis.
         */
        final UnitConverter c;
        try {
            c = unit.getConverterToAny(newUnit);
        } catch (ConversionException e) {
            // Use IllegalStateException because the public API is an AbstractCS member method.
            throw new IllegalStateException(Errors.format(Errors.Keys.IllegalUnitFor_2, "axis", unit), e);
        }
        return new DefaultCoordinateSystemAxis(properties, newAbbr, newDir, newUnit,
                c.convert(axis.getMinimumValue()), c.convert(axis.getMaximumValue()), axis.getRangeMeaning());
    }

    /**
     * Reorder the axes in an attempt to get a right-handed system.
     * If no axis change is needed, then this method returns {@code cs} unchanged.
     *
     * @param  cs The coordinate system to normalize.
     * @param  allowAxisChanges {@code true} for normalizing axis directions and units.
     * @return The normalized coordinate system.
     */
    static AbstractCS normalize(final AbstractCS cs, final boolean allowAxisChanges) {
        boolean changed = false;
        final int dimension = cs.getDimension();
        final CoordinateSystemAxis[] axes = new CoordinateSystemAxis[dimension];
        for (int i=0; i<dimension; i++) {
            CoordinateSystemAxis axis = cs.getAxis(i);
            if (allowAxisChanges) {
                changed |= (axis != (axis = normalize(axis)));
            }
            axes[i] = axis;
        }
        /*
         * Sorts the axis in an attempt to create a right-handed system
         * and creates a new Coordinate System if at least one axis changed.
         */
        changed |= sort(axes);
        if (!changed) {
            return cs;
        }
        final StringBuilder buffer = (StringBuilder) CharSequences.camelCaseToSentence(cs.getInterface().getSimpleName());
        return cs.createSameType(singletonMap(AbstractCS.NAME_KEY, DefaultCompoundCS.createName(buffer, axes)), axes);
    }
}
