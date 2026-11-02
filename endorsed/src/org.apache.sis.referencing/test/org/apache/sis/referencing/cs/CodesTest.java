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
import java.util.Collections;
import java.util.function.Supplier;
import java.lang.reflect.Field;
import javax.measure.Unit;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CSAuthorityFactory;
import org.apache.sis.measure.Units;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.factory.TestFactorySource;


/**
 * Compares the {@link Codes} elements with the <abbr>EPSG</abbr> geodetic database.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class CodesTest extends TestCase {
    /**
     * The unit of measurement of the vertical axis.
     */
    private static final Unit<?> VERTICAL_UNIT = Units.METRE;

    /**
     * Creates a new test case.
     */
    public CodesTest() {
    }

    /**
     * Returns the {@link Codes#EPSG} field value.
     */
    @SuppressWarnings("unchecked")
    private static Map<Codes, Codes> codes() throws ReflectiveOperationException {
        final Field field = Codes.class.getDeclaredField("EPSG");
        field.setAccessible(true);
        return Collections.unmodifiableMap(((Map<Codes, Codes>) field.get(null)));
    }

    /**
     * Compares the axis directions and units with <abbr>EPSG</abbr> definitions.
     *
     * @throws Exception if an error occurred while fetching the codes or querying the database.
     */
    @Test
    public void verify() throws Exception {
        int count = 0;
        final CSAuthorityFactory factory = TestFactorySource.getSharedFactory();
        for (final Codes code : codes().keySet()) {
            final Supplier<String> message = code::toString;
            final CoordinateSystem cs = factory.createCoordinateSystem(String.valueOf(code.epsg));
            final Unit<?> unit = cs.getAxis(0).getUnit();
            final var directions = new AxisDirection[cs.getDimension()];
            for (int i=0; i<directions.length; i++) {
                assertEquals(i < 2 ? unit : VERTICAL_UNIT, cs.getAxis(i).getUnit(), message);
                directions[i] = cs.getAxis(i).getDirection();
            }
            assertEquals(code.epsg, Codes.lookup(unit, directions), message);
            assertTrue(code.getDimension() == directions.length);
            count++;
        }
        assertEquals(23, count);    // Expected number to be adjusted when `Codes` is updated.
    }
}
