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
import java.util.HashMap;
import java.util.Locale;
import javax.measure.unit.Unit;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.InvalidParameterValueException;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static javax.measure.unit.SI.*;
import static org.opengis.test.Validators.*;


/**
 * Tests the {@link DefaultParameterDescriptor} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.1)
 * @version 0.4
 * @module
 */
@DependsOn(org.apache.sis.referencing.AbstractIdentifiedObjectTest.class)
public final strictfp class DefaultParameterDescriptorTest extends TestCase {
    /**
     * Strict tolerance factor for floating point comparisons. In the particular
     * case of this test suite, we can afford to be strict since we will perform
     * arithmetic only on integer values.
     */
    private static final double STRICT = 0.0;

    /**
     * Small tolerance factor for floating point comparisons resulting from some calculation.
     */
    private static final double EPS = 1E-10;

    /**
     * Constructs a descriptor for a mandatory parameter in a range of integer values.
     *
     * @param  name         The parameter name.
     * @param  defaultValue The default value for the parameter.
     * @param  minimum      The minimum parameter value.
     * @param  maximum      The maximum parameter value.
     * @return The parameter descriptor for the given range of values.
     */
    private static DefaultParameterDescriptor<Integer> create(final String name,
            final int defaultValue, final int minimum, final int maximum)
    {
        final Map<String,Object> properties = new HashMap<>(4);
        assertNull(properties.put(DefaultParameterDescriptor.NAME_KEY, name));
        assertNull(properties.put(DefaultParameterDescriptor.LOCALE_KEY, Locale.US));
        return new DefaultParameterDescriptor<>(properties,
                 Integer.class, null, Integer.valueOf(defaultValue),
                 Integer.valueOf(minimum), Integer.valueOf(maximum), null, true);
    }

    /**
     * Constructs a descriptor for a mandatory parameter in a range of floating point values.
     *
     * @param  name         The parameter name.
     * @param  defaultValue The default value for the parameter, or {@link Double#NaN} if none.
     * @param  minimum      The minimum parameter value, or {@link Double#NEGATIVE_INFINITY} if none.
     * @param  maximum      The maximum parameter value, or {@link Double#POSITIVE_INFINITY} if none.
     * @param  unit         The unit for default, minimum and maximum values.
     * @return The parameter descriptor for the given range of values.
     */
    private static DefaultParameterDescriptor<Double> create(final String name,
            final double defaultValue, final double minimum, final double maximum, final Unit<?> unit)
    {
        final Map<String,Object> properties = new HashMap<>(4);
        assertNull(properties.put(DefaultParameterDescriptor.NAME_KEY, name));
        assertNull(properties.put(DefaultParameterDescriptor.LOCALE_KEY, Locale.US));
        return new DefaultParameterDescriptor<>(properties, Double.class, null,
                Double.isNaN(defaultValue)          ? null : Double.valueOf(defaultValue),
                minimum == Double.NEGATIVE_INFINITY ? null : Double.valueOf(minimum),
                maximum == Double.POSITIVE_INFINITY ? null : Double.valueOf(maximum), unit, true);
    }

    /**
     * Tests {@code DefaultParameterDescriptor} constructor
     * with valid and invalid minimum and maximum values.
     */
    @Test
    public void testRangeValidation() {
        try {
            create("Test range", 12, 20, 4);
            fail("minimum > maximum");
        } catch (IllegalArgumentException exception) {
            assertEquals("Range [20 … 4] is not valid.", exception.getMessage());
        }
        final ParameterDescriptor<Integer> descriptor = create("Test range", 12, 4, 20);
        assertEquals("name", "Test range", descriptor.getName().getCode());
        assertEquals("defaultValue",  Integer.valueOf(12), descriptor.getDefaultValue());
        assertEquals("minimumValue",  Integer.valueOf( 4), descriptor.getMinimumValue());
        assertEquals("maximumValue",  Integer.valueOf(20), descriptor.getMaximumValue());
        assertEquals("minimumOccurs", 1, descriptor.getMinimumOccurs());
        assertEquals("maximumOccurs", 1, descriptor.getMaximumOccurs());
    }

    /**
     * Tests {@code DefaultParameterDescriptor} constructor with an invalid default value.
     */
    @Test
    @DependsOnMethod("testRangeValidation")
    public void testDefaultValueValidation() {
        try {
            create("Test default", 3, 4, 20);
            fail("defaultValue < minimum");
        } catch (IllegalArgumentException exception) {
            assertEquals("Value ‘Test default’=3 is invalid. Expected a value in the [4 … 20] range.", exception.getMessage());
        }
    }

    /**
     * Tests {@code DefaultParameterDescriptor} construction for {@link Double} type.
     * Also tests the {@link Parameter} created from the {@code createValue} method.
     */
    @Test
    public void testDoubleType() {
        final ParameterDescriptor<Double> descriptor = create("Test", 12, 4, 20, METRE);
        assertEquals("name",         "Test",             descriptor.getName().getCode());
        assertEquals("unit",         METRE,              descriptor.getUnit());
        assertEquals("class",        Double.class,       descriptor.getValueClass());
        assertEquals("defaultValue", Double.valueOf(12), descriptor.getDefaultValue());
        assertEquals("minimum",      Double.valueOf( 4), descriptor.getMinimumValue());
        assertEquals("maximum",      Double.valueOf(20), descriptor.getMaximumValue());
        validate(descriptor);
        assertEquals("DefaultParameterDescriptor[\"Test\", mandatory, class=Double, " +
                "valid=[4.0 … 20.0], default=12.0, unit=m]", descriptor.toString());

        testDoubleValue(new DefaultParameterValue<>(descriptor));
    }

    /**
     * Helper method for {@link #testDoubleType()}.
     * This method tests a parameter value associated to the descriptor of the above test.
     *
     * @return The class of the given parameter, for convenience.
     */
    private static void testDoubleValue(final ParameterValue<Double> parameter) {
        assertEquals("value",    Double.valueOf(12), parameter.getValue());
        assertEquals("intValue", 12,                 parameter.intValue());
        assertEquals("unit",     METRE,              parameter.getUnit());
        validate(parameter);

        for (int i=4; i<=20; i++) {
            parameter.setValue(i);
            assertEquals("value", Double.valueOf(i), parameter.getValue());
            assertEquals("unit",  METRE,             parameter.getUnit());
            assertEquals("value", i,                 parameter.doubleValue(METRE), STRICT);
            assertEquals("value", 100*i,             parameter.doubleValue(CENTIMETRE), STRICT);
        }
        try {
            parameter.setValue(3.0);
            fail("setValue(< min)");
        } catch (InvalidParameterValueException exception) {
            assertEquals("Test", exception.getParameterName());
        }
        try {
            parameter.setValue("12");
            fail("setValue(Sring)");
        } catch (InvalidParameterValueException exception) {
            assertEquals("Test", exception.getParameterName());
        }
        for (int i=400; i<=2000; i+=100) {
            parameter.setValue(i, CENTIMETRE);
            assertEquals("value", Double.valueOf(i), parameter.getValue());
            assertEquals("unit",  CENTIMETRE,        parameter.getUnit());
            assertEquals("value", i/100,             parameter.doubleValue(METRE), EPS);
        }
    }
}
