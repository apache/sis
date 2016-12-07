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
import org.opengis.util.FactoryException;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CSAuthorityFactory;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.apache.sis.referencing.factory.UnavailableFactoryException;
import org.apache.sis.referencing.factory.sql.EPSGFactory;
import org.apache.sis.referencing.CRS;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assume.*;


/**
 * Compares the {@link Codes} elements with the EPSG geodetic database.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public final strictfp class CodesTest extends TestCase {
    /**
     * Returns the EPSG factory, or skips the test if the factory is not available.
     */
    private static CSAuthorityFactory factory() throws FactoryException {
        final CRSAuthorityFactory factory = CRS.getAuthorityFactory("EPSG");
        assumeTrue("No connection to EPSG dataset.", factory instanceof EPSGFactory);
        try {
            assertNotNull(factory.createGeographicCRS("4326"));
        } catch (UnavailableFactoryException e) {
            assumeTrue("No connection to EPSG dataset.", false);
        }
        return (EPSGFactory) factory;
    }

    /**
     * Compares the axis directions and units with EPSG definitions.
     *
     * @throws Exception if an error occurred while fetching the codes or querying the database.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void verify() throws Exception {
        final CSAuthorityFactory factory = factory();
        final Field field = Codes.class.getDeclaredField("EPSG");
        field.setAccessible(true);
        for (final Codes c : ((Map<Codes,?>) field.get(null)).keySet()) {
            final CoordinateSystem cs = factory.createCoordinateSystem(String.valueOf(c.epsg));
            final Unit<?> unit = cs.getAxis(0).getUnit();
            final AxisDirection[] directions = new AxisDirection[cs.getDimension()];
            for (int i=0; i<directions.length; i++) {
                assertEquals(unit, cs.getAxis(i).getUnit());
                directions[i] = cs.getAxis(i).getDirection();
            }
            assertEquals("Codes.lookpup(…)", c.epsg, Codes.lookup(unit, directions));
        }
    }
}
