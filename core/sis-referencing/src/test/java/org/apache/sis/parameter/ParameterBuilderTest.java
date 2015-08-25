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

import javax.measure.unit.Unit;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import org.opengis.util.GenericName;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link ParameterBuilder}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.6
 * @module
 */
@DependsOn({
    org.apache.sis.referencing.BuilderTest.class,
    DefaultParameterDescriptorTest.class,
    DefaultParameterValueTest.class
})
public final strictfp class ParameterBuilderTest extends TestCase {
    /**
     * Tests various {@code create(…)} methods.
     */
    @Test
    public void testCreate() {
        final ParameterBuilder builder = new ParameterBuilder();
        ParameterDescriptor<Double> p = builder.addName("Test 1").create(0, SI.METRE);
        assertEquals("name", "Test 1",    p.getName().getCode());
        assertEquals("defaultValue", 0.0, p.getDefaultValue(), 0);
        assertNull  ("minimumValue",      p.getMinimumValue());
        assertNull  ("maximumValue",      p.getMaximumValue());
        assertEquals("unit", SI.METRE,    p.getUnit());

        p = builder.addName("Test 2").create(Double.NaN, SI.METRE);
        assertEquals("name", "Test 2",    p.getName().getCode());
        assertNull  ("defaultValue",      p.getDefaultValue());
        assertNull  ("minimumValue",      p.getMinimumValue());
        assertNull  ("maximumValue",      p.getMaximumValue());
        assertEquals("unit", SI.METRE,    p.getUnit());

        p = builder.addName("Test 3").createBounded(1, 4, 3, SI.METRE);
        assertEquals("name", "Test 3",    p.getName().getCode());
        assertEquals("defaultValue", 3.0, p.getDefaultValue(), 0);
        assertEquals("minimumValue", 1.0, p.getMinimumValue());
        assertEquals("maximumValue", 4.0, p.getMaximumValue());
        assertEquals("unit", SI.METRE,    p.getUnit());
    }

    /**
     * Tests the <cite>"Mercator (variant A)"</cite> example given in Javadoc.
     */
    @Test
    @DependsOnMethod("testCreate")
    @SuppressWarnings("UnnecessaryBoxing")
    public void testMercatorProjection() {
        final ParameterBuilder builder = new ParameterBuilder();
        builder.setCodeSpace(Citations.EPSG, "EPSG").setRequired(true);
        final ParameterDescriptor<?>[] parameters = {
            builder.addName("Longitude of natural origin")
                   .addName(Citations.OGC, "central_meridian")
                   .addName(Citations.GEOTIFF, "NatOriginLong")
                   .setRemarks("Some remarks.")               .createBounded(-180, +180, 0, NonSI.DEGREE_ANGLE),
            builder.addName("Latitude of natural origin")     .createBounded( -80,  +84, 0, NonSI.DEGREE_ANGLE),
            builder.addName("Scale factor at natural origin") .createStrictlyPositive(1, Unit.ONE),
            builder.addName("False easting")                  .create(0, SI.METRE),
            builder.addName("False northing")                 .create(0, SI.METRE)
        };
        // Tests random properties.
        assertEquals("EPSG",             parameters[1].getName().getCodeSpace());
        assertEquals("False easting",    parameters[3].getName().getCode());
        assertEquals("Some remarks.",    parameters[0].getRemarks().toString());
        assertEquals(Double.valueOf(84), parameters[1].getMaximumValue());
        assertEquals(SI.METRE,           parameters[4].getUnit());
        assertTrue  (                    parameters[1].getAlias().isEmpty());

        final GenericName alias = parameters[0].getAlias().iterator().next();
        assertEquals("central_meridian",     alias.tip().toString());
        assertEquals("OGC",                  alias.head().toString());
        assertEquals("OGC:central_meridian", alias.toString());
    }
}
