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
package org.apache.sis.storage.gdal;

import javax.measure.Unit;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.cs.RangeMeaning;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.apache.sis.internal.simple.SimpleIdentifier;
import org.apache.sis.measure.Units;


/**
 * An axis of a {@link CRS} object.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class Axis extends PJObject implements CoordinateSystemAxis {
    /**
     * The direction, as a 'e', 'w', 'n', 's', 'u' or 'd' letter.
     */
    private final char direction;

    /**
     * The axis unit. If angular, then the CRS is presumed geographic or geocentric
     * and the units is fixed to {@link Units#DEGREE} (no other angular unit is permitted).
     * Otherwise the CRS is presumed projected with arbitrary linear unit.
     */
    private final Unit<?> unit;

    /**
     * Creates a new axis.
     */
    Axis(final char direction, final Unit<?> unit) {
        super(getName(direction, unit));
        this.direction = direction;
        this.unit = unit;
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    private static ReferenceIdentifier getName(final char direction, final Unit<?> unit) {
        final String name;
        if (Units.isAngular(unit)) {
            switch (direction) {
                case 'e':
                case 'w': name = "Geodetic longitude"; break;
                case 'n':
                case 's': name = "Geodetic latitude"; break;
                case 'u': name = "Height"; break;
                case 'd': name = "Depth"; break;
                default: return null;
            }
        } else {
            switch (direction) {
                case 'e': name = "Easting"; break;
                case 'w': name = "Westing"; break;
                case 'n': name = "Northing"; break;
                case 's': name = "Southing"; break;
                case 'u': name = "Height"; break;
                case 'd': name = "Depth"; break;
                default: return null;
            }
        }
        return new SimpleIdentifier(null, name, false);
    }

    /**
     * Returns the abbreviation, which is inferred from the unit and direction.
     */
    @Override
    public String getAbbreviation() {
        if (Units.isAngular(unit)) {
            switch (direction) {
                case 'e':
                case 'w': return "λ";
                case 'n':
                case 's': return "φ";
            }
        } else {
            switch (direction) {
                case 'e':
                case 'w': return "x";
                case 'n':
                case 's': return "y";
            }
        }
        switch (direction) {
            case 'u': return "h";
            case 'd': return "d";
            default:  return null;
        }
    }

    /**
     * Returns the direction.
     */
    @Override
    public AxisDirection getDirection() {
        final AxisDirection dir;
        switch (direction) {
            case 'e': dir = AxisDirection.EAST;  break;
            case 'w': dir = AxisDirection.WEST;  break;
            case 'n': dir = AxisDirection.NORTH; break;
            case 's': dir = AxisDirection.SOUTH; break;
            case 'u': dir = AxisDirection.UP;    break;
            case 'd': dir = AxisDirection.DOWN;  break;
            default:  dir = null; break;
        }
        return dir;
    }

    /**
     * Returns the minimal value permitted by this axis.
     */
    @Override
    public double getMinimumValue() {
        return -getMaximumValue();
    }

    /**
     * Returns the minimal value permitted by this axis.
     */
    @Override
    public double getMaximumValue() {
        if (Units.isAngular(unit)) {
            switch (direction) {
                case 'e':
                case 'w': return 180;
                case 'n':
                case 's': return 90;
            }
        }
        return Double.POSITIVE_INFINITY;
    }

    /**
     * Returns the range meaning.
     */
    @Override
    public RangeMeaning getRangeMeaning() {
        if (Units.isAngular(unit)) {
            switch (direction) {
                case 'e':
                case 'w': return RangeMeaning.WRAPAROUND;
                case 'n':
                case 's': return RangeMeaning.EXACT;
            }
        }
        return null;
    }

    /**
     * Returns the units given at construction time.
     */
    @Override
    public Unit<?> getUnit() {
        return unit;
    }
}
