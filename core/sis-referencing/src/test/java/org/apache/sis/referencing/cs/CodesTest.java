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
import javax.measure.Unit;
import java.lang.reflect.Field;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CSAuthorityFactory;
import org.apache.sis.referencing.factory.TestFactorySource;
import org.apache.sis.measure.Units;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Compares the {@link Codes} elements with the EPSG geodetic database.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.8
 * @module
 */
public final strictfp class CodesTest extends TestCase {
    /**
     * The unit of measurement of the vertical axis.
     */
    private static final Unit<?> VERTICAL_UNIT = Units.METRE;

    /**
     * Compares the axis directions and units with EPSG definitions.
     *
     * @throws Exception if an error occurred while fetching the codes or querying the database.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void verify() throws Exception {
        final CSAuthorityFactory factory = TestFactorySource.getSharedFactory();
        final Field field = Codes.class.getDeclaredField("EPSG");
        field.setAccessible(true);
        for (final Codes c : ((Map<Codes,?>) field.get(null)).keySet()) {
            final CoordinateSystem cs = factory.createCoordinateSystem(String.valueOf(c.epsg));
            final Unit<?> unit = cs.getAxis(0).getUnit();
            final AxisDirection[] directions = new AxisDirection[cs.getDimension()];
            for (int i=0; i<directions.length; i++) {
                assertEquals(i < 2 ? unit : VERTICAL_UNIT, cs.getAxis(i).getUnit());
                directions[i] = cs.getAxis(i).getDirection();
            }
            assertEquals("Codes.lookpup(…)", c.epsg, Codes.lookup(unit, directions));
        }
    }
}
