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
import javax.measure.Unit;
import org.apache.sis.measure.Units;
import org.apache.sis.util.ArraysExt;
import org.opengis.referencing.cs.AxisDirection;

import static org.apache.sis.internal.util.Constants.EPSG_METRE;
import static org.apache.sis.internal.util.Constants.EPSG_AXIS_DEGREES;
import static org.apache.sis.internal.util.Constants.EPSG_PROJECTED_CS;


/**
 * Map units of measurement and axis directions to {@link CoordinateSystem} objects defined in the EPSG database.
 * Current version uses hard-coded mapping.
 *
 * @author  Rémi Marechal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
final class Codes {
    /**
     * Axis directions as a maximum of 4 directions packed in a single integer.
     * Each byte is an {@link AxisDirection#ordinal()} value.
     *
     * @see #pack(AxisDirection[])
     */
    final int directions;

    /**
     * EPSG code of the target unit of measurement.
     */
    final short unit;

    /**
     * EPSG code of the coordinate reference system.
     */
    final short epsg;

    /**
     * Creates a new code for the given axis directions and units.
     */
    private Codes(final int directions, final short unit, final short epsg) {
        this.directions = directions;
        this.unit       = unit;
        this.epsg       = epsg;
    }

    /**
     * Packs the given axis directions in a single integer.
     *
     * @return the packed directions, or 0 if the given directions can not be packed.
     */
    private static int pack(final AxisDirection[] directions) {
        int packed = 0;
        int i = directions.length;
        if (i <= Integer.BYTES) {
            while (--i >= 0) {
                final int ordinal = directions[i].ordinal();
                if (ordinal <= 0 || ordinal > Byte.MAX_VALUE) {
                    return 0;
                }
                packed = (packed << Byte.SIZE) | ordinal;
            }
        }
        return packed;
    }

    /**
     * Returns the EPSG code for the given axis directions and unit of measurement, or 0 if none.
     */
    static short lookup(final Unit<?> unit, final AxisDirection[] directions) {
        final Integer uc = Units.getEpsgCode(unit, true);
        if (uc != null) {
            final int p = pack(directions);
            if (p != 0) {
                Codes m = new Codes(p, uc.shortValue(), (short) 0);
                m = EPSG.get(m);
                if (m != null) {
                    return m.epsg;
                }
            }
        }
        return 0;
    }

    /**
     * Returns the hash code value for this {@code Codes} instance.
     * Note that {@link #lookup(AxisDirection[], Unit)} needs that the hash code excludes the EPSG code.
     */
    @Override
    public int hashCode() {
        return directions + unit;
    }

    /**
     * Compares this {@code Codes} instance with the given object for equality.
     * Note that {@link #lookup(AxisDirection[], Unit)} needs that the comparison excludes the EPSG code.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof Codes) {
            final Codes other = (Codes) obj;
            return (directions == other.directions) && (unit == other.unit);
        }
        return false;
    }

    /**
     * All hard-coded EPSG codes known to this class.
     */
    private static final Map<Codes,Codes> EPSG = new HashMap<>(20);
    static {
        final AxisDirection[] directions = new AxisDirection[] {AxisDirection.EAST, AxisDirection.NORTH};
        int packed = pack(directions);
        short unit = EPSG_METRE;
loop:   for (int i=0; ; i++) {
            final short epsg;
            switch (i) {
                case  0: epsg = EPSG_PROJECTED_CS;              break;      //  Cartesian   [E,N] in metres
                case  1: epsg = 1039; unit = 9002;              break;      //  Cartesian   [E,N] in feet
                case  2: epsg = 4497; unit = 9003;              break;      //  Cartesian   [E,N] in US survey feet
                case  3: epsg = 4403; unit = 9005;              break;      //  Cartesian   [E,N] in Clarke feet
                case  4: epsg = 6424; unit = EPSG_AXIS_DEGREES; break;      //  Ellipsoidal [E,N] in degrees
                case  5: epsg = 6425; unit = 9105;              break;      //  Ellipsoidal [E,N] in gradians
                case  6: epsg = 6429; unit = 9101;              break;      //  Ellipsoidal [E,N] in radians
                case  7: ArraysExt.swap(directions, 0, 1);
                         packed = pack(directions);
                         epsg = 4500; unit = EPSG_METRE;        break;      //  Cartesian   [N,E] in metres
                case  8: epsg = 1029; unit = 9002;              break;      //  Cartesian   [N,E] in feet
                case  9: epsg = 4502; unit = 9005;              break;      //  Cartesian   [N,E] in Clarke feet
                case 10: epsg = 6422; unit = EPSG_AXIS_DEGREES; break;      //  Ellipsoidal [N,E] in degrees
                case 11: epsg = 6403; unit = 9105;              break;      //  Ellipsoidal [N,E] in gradians
                case 12: epsg = 6428; unit = 9101;              break;      //  Ellipsoidal [N,E] in radians
                case 13: directions[1] = AxisDirection.WEST;
                         packed = pack(directions);
                         epsg = 4501; unit = EPSG_METRE;        break;      //  Cartesian [N,W] in metres
                case 14: ArraysExt.swap(directions, 0, 1);
                         packed = pack(directions);
                         epsg = 4491; break;                                //  Cartesian [W,N] in metres
                case 15: directions[1] = AxisDirection.SOUTH;
                         packed = pack(directions);
                         epsg = 6503; break;                                //  Cartesian [W,S] in metres
                case 16: ArraysExt.swap(directions, 0, 1);
                         packed = pack(directions);
                         epsg = 6501; break;                                //  Cartesian [S,W] in metres
                default: break loop;
            }
            final Codes m = new Codes(packed, unit, epsg);
            if (packed == 0 || EPSG.put(m, m) != null) {
                throw new AssertionError(m.epsg);
            }
        }
    }
}
