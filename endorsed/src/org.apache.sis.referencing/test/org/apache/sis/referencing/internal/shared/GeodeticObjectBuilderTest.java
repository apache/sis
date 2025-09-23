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
package org.apache.sis.referencing.internal.shared;

import java.util.function.BiConsumer;
import org.opengis.util.FactoryException;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.measure.Units;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;

// Specific to the main branch:
import static org.apache.sis.test.GeoapiAssert.assertAxisDirectionsEqual;


/**
 * Tests {@link GeodeticObjectBuilder}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class GeodeticObjectBuilderTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public GeodeticObjectBuilderTest() {
    }

    /**
     * Tests {@link GeodeticObjectBuilder#changeConversion(String, BiConsumer)}.
     *
     * @throws FactoryException if an operation method name is not supported.
     */
    @Test
    public void testChangeConversion() throws FactoryException {
        final GeodeticObjectBuilder b = new GeodeticObjectBuilder();
        assertSame(b, b.setConversionName("Dummy projection"));
        assertSame(b, b.setConversionMethod("Popular Visualisation Pseudo Mercator"));
        assertSame(b, b.setParameter("Longitude of natural origin", 40, Units.DEGREE));
        assertSame(b, b.setParameter("Scale factor at natural origin", 0.5, Units.UNITY));
        assertSame(b, b.changeConversion("Mercator (Spherical)", null));
        final ProjectedCRS crs = b.setNormalizedAxisOrder(true).createProjectedCRS();
        final ParameterValueGroup p = crs.getConversionFromBase().getParameterValues();
        assertEquals(40,  p.parameter("Longitude of natural origin").doubleValue());
        assertEquals(0.5, p.parameter("Scale factor at natural origin").doubleValue());
        assertAxisDirectionsEqual(crs.getBaseCRS().getCoordinateSystem(), AxisDirection.EAST, AxisDirection.NORTH);
    }
}
