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
package org.apache.sis.parameter;

import java.util.Map;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import static org.apache.sis.util.internal.shared.Constants.SEMI_MAJOR;
import static org.apache.sis.util.internal.shared.Constants.SEMI_MINOR;
import static org.apache.sis.util.internal.shared.Constants.EARTH_RADIUS;
import static org.apache.sis.util.internal.shared.Constants.INVERSE_FLATTENING;
import static org.apache.sis.util.internal.shared.Constants.IS_IVF_DEFINITIVE;
import static org.apache.sis.util.internal.shared.Constants.CENTRAL_MERIDIAN;
import static org.apache.sis.util.internal.shared.Constants.STANDARD_PARALLEL;
import static org.apache.sis.util.internal.shared.Constants.STANDARD_PARALLEL_1;
import static org.apache.sis.util.internal.shared.Constants.STANDARD_PARALLEL_2;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link MapProjectionParameters} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class MapProjectionParametersTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public MapProjectionParametersTest() {
    }

    /**
     * Creates a map projection descriptor with semi-major/minor axis lengths
     * and the given number of standard parallels.
     *
     * @param  numStandardParallels  1 or 2 for including the standard parallels.
     */
    @SuppressWarnings("fallthrough")
    private static MapProjectionDescriptor createDescriptor(final int numStandardParallels) {
        final ParameterDescriptor<?>[] parameters = new ParameterDescriptor<?>[numStandardParallels + 1];
        switch (numStandardParallels) {
            default: throw new IllegalArgumentException();
            case 2: parameters[2] = parameter(STANDARD_PARALLEL_2);   // Fall through
            case 1: parameters[1] = parameter(STANDARD_PARALLEL_1);   // Fall through
            case 0: parameters[0] = parameter(CENTRAL_MERIDIAN);
                    break;
        }
        return new MapProjectionDescriptor(name("Lambert Conic Conformal (2SP)"), parameters);
    }

    /** Creates a parameter of the given name. */
    private static DefaultParameterDescriptor<?> parameter(final String name) {
        return new DefaultParameterDescriptor<>(name(name), 1, 1, Double.class, null, null, null);
    }

    /** Returns properties map for an object of the given name. */
    private static Map<String,String> name(final String name) {
        return Map.of(MapProjectionDescriptor.NAME_KEY, name);
    }

    /**
     * Tests the {@code "earth_radius"} dynamic parameter.
     */
    @Test
    public void testEarthRadius() {
        final MapProjectionDescriptor descriptor = createDescriptor(0);
        final ParameterValueGroup parameters = descriptor.createValue();

        parameters.parameter(SEMI_MAJOR).setValue(6378137.000);                         // WGS84
        parameters.parameter(SEMI_MINOR).setValue(6356752.314);
        assertEquals(6371007, parameters.parameter(EARTH_RADIUS).doubleValue(), 0.5);   // Authalic radius.
        assertEquals(6378137, parameters.parameter(SEMI_MAJOR)  .doubleValue(), 0.5);
        assertEquals(6356752, parameters.parameter(SEMI_MINOR)  .doubleValue(), 0.5);

        parameters.parameter(EARTH_RADIUS).setValue(6371000);
        assertEquals(6371000, parameters.parameter(EARTH_RADIUS).doubleValue());
        assertEquals(6371000, parameters.parameter(SEMI_MAJOR)  .doubleValue());
        assertEquals(6371000, parameters.parameter(SEMI_MINOR)  .doubleValue());
    }

    /**
     * Tests the {@code "inverse_flattening"} dynamic parameter.
     */
    @Test
    public void testInverseFlattening() {
        final MapProjectionDescriptor descriptor = createDescriptor(0);
        final ParameterValueGroup parameters = descriptor.createValue();

        parameters.parameter(SEMI_MAJOR).setValue(6378206.4);  // Clarke 1866
        parameters.parameter(SEMI_MINOR).setValue(6356583.8);
        assertEquals(294.97870, parameters.parameter(INVERSE_FLATTENING).doubleValue(), 0.00001);
        assertEquals(6378206.4, parameters.parameter(SEMI_MAJOR)        .doubleValue(), 0.5);
        assertEquals(6356583.8, parameters.parameter(SEMI_MINOR)        .doubleValue(), 0.5);
        assertFalse(parameters.parameter(IS_IVF_DEFINITIVE).booleanValue());

        parameters.parameter(SEMI_MAJOR).setValue(6378137.0);  // WGS84
        parameters.parameter(INVERSE_FLATTENING).setValue(298.257223563);
        assertEquals(298.257, parameters.parameter(INVERSE_FLATTENING).doubleValue(), 0.001);
        assertEquals(6378137, parameters.parameter(SEMI_MAJOR)        .doubleValue(), 0.5);
        assertEquals(6356752, parameters.parameter(SEMI_MINOR)        .doubleValue(), 0.5);
        assertTrue(parameters.parameter(IS_IVF_DEFINITIVE).booleanValue());

        parameters.parameter(SEMI_MAJOR).setValue(6378350.9);  // Clarke 1858 (approximated)
        parameters.parameter(SEMI_MINOR).setValue(6356675.0);
        assertEquals(294.26, parameters.parameter(INVERSE_FLATTENING).doubleValue(), 0.001);
        assertFalse(parameters.parameter(IS_IVF_DEFINITIVE).booleanValue());
    }

    /**
     * Tests the inverse flattening dynamic parameter set after the semi-major axis length.
     * This method tests actually the capability to compute the semi-minor axis length.
     */
    @Test
    public void testSemiMinor() {
        final MapProjectionDescriptor descriptor = createDescriptor(0);
        final ParameterValueGroup parameters = descriptor.createValue();

        parameters.parameter(SEMI_MAJOR).setValue(6378137.000); // WGS84
        parameters.parameter(INVERSE_FLATTENING).setValue(298.257223563);
        assertEquals(298.257, parameters.parameter(INVERSE_FLATTENING).doubleValue(), 0.001);
        assertEquals(6378137, parameters.parameter(SEMI_MAJOR)        .doubleValue(), 0.5);
        assertEquals(6356752, parameters.parameter(SEMI_MINOR)        .doubleValue(), 0.5);
    }

    /**
     * Tests the standard parallel dynamic parameter.
     */
    @Test
    public void testStandardParallel() {
        final MapProjectionDescriptor descriptor = createDescriptor(2);
        final ParameterValueGroup parameters = descriptor.createValue();
        final ParameterValue<?> p  = parameters.parameter(STANDARD_PARALLEL);
        final ParameterValue<?> p1 = parameters.parameter(STANDARD_PARALLEL_1);
        final ParameterValue<?> p2 = parameters.parameter(STANDARD_PARALLEL_2);
        assertSame(p,  parameters.parameter(STANDARD_PARALLEL));
        assertSame(p1, parameters.parameter(STANDARD_PARALLEL_1));
        assertSame(p2, parameters.parameter(STANDARD_PARALLEL_2));
        assertNotSame(p1, p2);
        assertNotSame(p1, p);
        assertNotSame(p2, p);

        /* Empty */      assertArrayEquals(new double[] {     }, p.doubleValueList());
        p1.setValue(40); assertArrayEquals(new double[] {40   }, p.doubleValueList());
        p2.setValue(60); assertArrayEquals(new double[] {40,60}, p.doubleValueList());

        p.setValue(new double[] {30,40});
        assertEquals(30, p1.doubleValue());
        assertEquals(40, p2.doubleValue());

        p.setValue(new double[] {45});
        assertEquals(45,         p1.doubleValue());
        assertEquals(Double.NaN, p2.doubleValue());
    }
}
