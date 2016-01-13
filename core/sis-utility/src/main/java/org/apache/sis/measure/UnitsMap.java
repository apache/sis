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
package org.apache.sis.measure;

import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import javax.measure.unit.Unit;
import javax.measure.quantity.Quantity;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.util.Static;

import static org.apache.sis.measure.Units.*;


/**
 * Map of units, defined in a separated class in order to avoid too-early class loading.
 * This class may be removed in a future SIS version if we provide our own units framework.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
final class UnitsMap extends Static {
    /**
     * Do not allows instantiation of this class.
     */
    private UnitsMap() {
    }

    /**
     * The 9122 integer, used as an alternative code for the degrees unit.
     * See {@link Units#getEpsgCode(Unit, boolean)} for more information.
     */
    static final Integer EPSG_AXIS_DEGREES = (int) Constants.EPSG_AXIS_DEGREES;

    /**
     * EPSG codes of some units. This map is the reverse of {@link Units#valueOfEPSG(int)}.
     * The map is defined in this class rather than in the {@code Units} class in order to
     * avoid loading the {@code SexagesimalConverter} class before needed, since their
     * constants appear in this map.
     */
    static final Map<Unit<?>,Integer> EPSG_CODES = new HashMap<Unit<?>,Integer>(20);
    static {
        final byte[] codes = {1, 2, 30, 36, 101, 102, 103, 104, 105, 108, 109, 111, 110, (byte) 201, (byte) 202};
        for (final byte c : codes) {
            final int code = 9000 + (c & 0xFF);
            EPSG_CODES.put(Units.valueOfEPSG(code), code);
        }
    }

    /**
     * A few units commonly used in GIS.
     */
    private static final Map<Unit<?>,Unit<?>> COMMONS = new HashMap<Unit<?>,Unit<?>>(48);
    static {
        COMMONS.put(MILLISECOND, MILLISECOND);
        boolean nonSI = false;
        do for (final Field field : (nonSI ? NonSI.class : SI.class).getFields()) {
            final int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)) {
                final Object value;
                try {
                    value = field.get(null);
                } catch (IllegalAccessException e) {
                    // Should not happen since we asked only for public static constants.
                    throw new AssertionError(e);
                }
                if (value instanceof Unit<?>) {
                    final Unit<?> unit = (Unit<?>) value;
                    if (isLinear(unit) || isAngular(unit) || isScale(unit)) {
                        COMMONS.put(unit, unit);
                    }
                }
            }
        } while ((nonSI = !nonSI) == true);
    }

    /**
     * Returns a unique instance of the given units if possible, or the units unchanged otherwise.
     *
     * @param  <A>    The quantity measured by the unit.
     * @param  unit   The unit to canonicalize.
     * @return A unit equivalents to the given unit, canonicalized if possible.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    static <A extends Quantity> Unit<A> canonicalize(final Unit<A> unit) {
        final Unit<?> candidate = COMMONS.get(unit);
        if (candidate != null) {
            return (Unit) candidate;
        }
        return unit;
    }
}
