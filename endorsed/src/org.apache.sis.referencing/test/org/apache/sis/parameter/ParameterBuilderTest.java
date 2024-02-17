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

import org.opengis.util.GenericName;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.measure.Units;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link ParameterBuilder}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ParameterBuilderTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public ParameterBuilderTest() {
    }

    /**
     * Tests various {@code create(…)} methods.
     */
    @Test
    public void testCreate() {
        final ParameterBuilder builder = new ParameterBuilder();
        ParameterDescriptor<Double> p = builder.addName("Test 1").create(0, Units.METRE);
        assertEquals("Test 1", p.getName().getCode());
        assertEquals(0.0, p.getDefaultValue());
        assertNull  (     p.getMinimumValue());
        assertNull  (     p.getMaximumValue());
        assertEquals(Units.METRE, p.getUnit());

        p = builder.addName("Test 2").create(Double.NaN, Units.METRE);
        assertEquals("Test 2", p.getName().getCode());
        assertNull  (p.getDefaultValue());
        assertNull  (p.getMinimumValue());
        assertNull  (p.getMaximumValue());
        assertEquals(Units.METRE, p.getUnit());

        p = builder.addName("Test 3").createBounded(1, 4, 3, Units.METRE);
        assertEquals("Test 3",    p.getName().getCode());
        assertEquals(3.0, p.getDefaultValue());
        assertEquals(1.0, p.getMinimumValue());
        assertEquals(4.0, p.getMaximumValue());
        assertEquals(Units.METRE, p.getUnit());
    }

    /**
     * Tests the <q>Mercator (variant A)</q> example given in Javadoc.
     */
    @Test
    @SuppressWarnings("UnnecessaryBoxing")
    public void testMercatorProjection() {
        final ParameterBuilder builder = new ParameterBuilder();
        builder.setCodeSpace(Citations.EPSG, "EPSG").setRequired(true);
        final ParameterDescriptor<?>[] parameters = {
            builder.addName("Longitude of natural origin")
                   .addName(Citations.OGC, "central_meridian")
                   .addName(Citations.GEOTIFF, "NatOriginLong")
                   .setRemarks("Some remarks.")               .createBounded(-180, +180, 0, Units.DEGREE),
            builder.addName("Latitude of natural origin")     .createBounded( -80,  +84, 0, Units.DEGREE),
            builder.addName("Scale factor at natural origin") .createStrictlyPositive(1, Units.UNITY),
            builder.addName("False easting")                  .create(0, Units.METRE),
            builder.addName("False northing")                 .create(0, Units.METRE)
        };
        // Tests random properties.
        assertEquals("EPSG",             parameters[1].getName().getCodeSpace());
        assertEquals("False easting",    parameters[3].getName().getCode());
        assertEquals("Some remarks.",    parameters[0].getRemarks().toString());
        assertEquals(Double.valueOf(84), parameters[1].getMaximumValue());
        assertEquals(Units.METRE,        parameters[4].getUnit());
        assertTrue  (                    parameters[1].getAlias().isEmpty());

        final GenericName alias = parameters[0].getAlias().iterator().next();
        assertEquals("central_meridian",     alias.tip().toString());
        assertEquals("OGC",                  alias.head().toString());
        assertEquals("OGC:central_meridian", alias.toString());
    }
}
