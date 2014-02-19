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
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
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
     * Creates the map of properties to be given to {@link DefaultParameterDescriptor} constructor.
     *
     * @param  name The parameter name.
     * @return The properties to be given to descriptor constructor.
     */
    private static Map<String,Object> properties(final String name,
            final Object minimumValue, final Object maximumValue)
    {
        final Map<String,Object> properties = new HashMap<String,Object>(8);
        assertNull(properties.put(DefaultParameterDescriptor.NAME_KEY, name));
        assertNull(properties.put(DefaultParameterDescriptor.MINIMUM_VALUE_KEY, minimumValue));
        assertNull(properties.put(DefaultParameterDescriptor.MAXIMUM_VALUE_KEY, maximumValue));
        assertNull(properties.put(DefaultParameterDescriptor.LOCALE_KEY, Locale.US));
        return properties;
    }

    /**
     * Creates a descriptor for an optional parameter without default value, minimum or maximum value.
     *
     * @param  name The parameter name.
     * @param  type The type of values.
     * @return The parameter descriptor.
     */
    static <T> DefaultParameterDescriptor<T> createSimpleOptional(final String name, final Class<T> type) {
        return new DefaultParameterDescriptor<T>(properties(name, null, null), type, null, null, false);
    }

    /**
     * Creates a descriptor for a mandatory parameter in a range of integer values.
     *
     * @param  name         The parameter name.
     * @param  defaultValue The default value for the parameter.
     * @param  minimumValue The minimum parameter value.
     * @param  maximumValue The maximum parameter value.
     * @return The parameter descriptor for the given range of values.
     */
    static DefaultParameterDescriptor<Integer> create(final String name,
            final int defaultValue, final int minimumValue, final int maximumValue)
    {
        return new DefaultParameterDescriptor<Integer>(properties(name, minimumValue, maximumValue),
                 Integer.class, Integer.valueOf(defaultValue), null, true);
    }

    /**
     * Creates a descriptor for a mandatory parameter in a range of floating point values.
     *
     * @param  name         The parameter name.
     * @param  defaultValue The default value for the parameter, or {@link Double#NaN} if none.
     * @param  minimum      The minimum parameter value, or {@link minimumValue#NEGATIVE_INFINITY} if none.
     * @param  maximumValue The maximum parameter value, or {@link Double#POSITIVE_INFINITY} if none.
     * @param  unit         The unit for default, minimum and maximum values.
     * @return The parameter descriptor for the given range of values.
     */
    static DefaultParameterDescriptor<Double> create(final String name,
            final double defaultValue, final double minimumValue, final double maximumValue, final Unit<?> unit)
    {
        return new DefaultParameterDescriptor<Double>(properties(name,
                minimumValue == Double.NEGATIVE_INFINITY ? null : Double.valueOf(minimumValue),
                maximumValue == Double.POSITIVE_INFINITY ? null : Double.valueOf(maximumValue)),
                Double.class, Double.isNaN(defaultValue) ? null : Double.valueOf(defaultValue), unit, true);
    }

    /**
     * Creates a descriptor for a parameter restricted to a set of valid values.
     * This is typically (but not necessarily) a code list parameter.
     *
     * @param  name         The parameter name.
     * @param  type         The type of values.
     * @param  validValues  The valid values.
     * @param  defaultValue The default value for the parameter.
     * @return The parameter descriptor for the given range of values.
     */
    static <T> DefaultParameterDescriptor<T> create(final String name, final Class<T> type,
            final T[] validValues, final T defaultValue)
    {
        final Map<String,Object> properties = properties(name, null, null);
        assertNull(properties.put(DefaultParameterDescriptor.VALID_VALUES_KEY, validValues));
        return new DefaultParameterDescriptor<T>(properties, type, defaultValue, null, true);
    }

    /**
     * Tests the creation of a simple descriptor for an optional parameter without minimum or maximum value.
     */
    @Test
    public void testOptionalInteger() {
        final ParameterDescriptor<Integer> descriptor = createSimpleOptional("Simple param", Integer.class);
        assertEquals("name",      "Simple param", descriptor.getName().getCode());
        assertEquals("valueClass", Integer.class, descriptor.getValueClass());
        assertNull  ("validValues",               descriptor.getValidValues());
        assertNull  ("defaultValue",              descriptor.getDefaultValue());
        assertNull  ("minimumValue",              descriptor.getMinimumValue());
        assertNull  ("maximumValue",              descriptor.getMaximumValue());
        assertEquals("minimumOccurs", 0,          descriptor.getMinimumOccurs());
        assertEquals("maximumOccurs", 1,          descriptor.getMaximumOccurs());
    }

    /**
     * Tests {@code DefaultParameterDescriptor} constructor
     * with valid and invalid minimum and maximum values.
     */
    @Test
    @DependsOnMethod("testOptionalInteger")
    public void testRangeValidation() {
        try {
            create("Test range", 12, 20, 4);
            fail("minimum > maximum");
        } catch (IllegalArgumentException exception) {
            assertEquals("Range [20 … 4] is not valid.", exception.getMessage());
        }
        final ParameterDescriptor<Integer> descriptor = create("Test range", 12, 4, 20);
        assertEquals("name",          "Test range",        descriptor.getName().getCode());
        assertEquals("valueClass",    Integer.class,       descriptor.getValueClass());
        assertNull  ("validValues",                        descriptor.getValidValues());
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
     */
    @Test
    public void testDoubleType() {
        final ParameterDescriptor<Double> descriptor = create("Length measure", 12, 4, 20, SI.METRE);
        assertEquals("name",         "Length measure",   descriptor.getName().getCode());
        assertEquals("unit",         SI.METRE,           descriptor.getUnit());
        assertEquals("class",        Double.class,       descriptor.getValueClass());
        assertEquals("defaultValue", Double.valueOf(12), descriptor.getDefaultValue());
        assertEquals("minimum",      Double.valueOf( 4), descriptor.getMinimumValue());
        assertEquals("maximum",      Double.valueOf(20), descriptor.getMaximumValue());
        validate(descriptor);
        assertEquals("DefaultParameterDescriptor[\"Length measure\", mandatory, class=Double, " +
                "valid=[4.0 … 20.0], default=12.0, unit=m]", descriptor.toString());
    }

    /**
     * Verifies that we can not assign unit of measurements to non-numerical values.
     */
    @Test
    public void testStringType() {
        final ParameterDescriptor<String> descriptor = new DefaultParameterDescriptor<String>(
                properties("String param", "AAA", "BBB"), String.class, "ABC", null, false);
        assertEquals("name", "String param",     descriptor.getName().getCode());
        assertEquals("valueClass", String.class, descriptor.getValueClass());
        assertNull  ("validValues",              descriptor.getValidValues());
        assertEquals("defaultValue",  "ABC",     descriptor.getDefaultValue());
        assertEquals("minimumValue",  "AAA",     descriptor.getMinimumValue());
        assertEquals("maximumValue",  "BBB",     descriptor.getMaximumValue());
        assertEquals("minimumOccurs", 0,         descriptor.getMinimumOccurs());
        assertEquals("maximumOccurs", 1,         descriptor.getMaximumOccurs());
        /*
         * Same construction than above, except that we specify a unit of measurement.
         * This operation shall be invalid for non-numerical types.
         */
        try {
            new DefaultParameterDescriptor<String>(properties("Invalid param", "AAA", "BBB"),
                     String.class, "ABC", SI.METRE, false);
        } catch (IllegalArgumentException e) {
            assertEquals("Unit of measurement “m” is not valid for “Invalid param” values.", e.getMessage());
        }
    }

    /**
     * Tests a descriptor for a parameter restricted to some values.
     * This is typically (but not necessarily) a code list parameter.
     */
    @Test
    public void testEnumeration() {
        final String[] enumeration = {"Apple", "Orange", "りんご"};
        final ParameterDescriptor<String> descriptor = create(
                "Enumeration param", String.class, enumeration, "Apple");
        assertEquals     ("name", "Enumeration param", descriptor.getName().getCode());
        assertEquals     ("valueClass", String.class,  descriptor.getValueClass());
        assertArrayEquals("validValues", enumeration,  descriptor.getValidValues().toArray());
        assertEquals     ("defaultValue",  "Apple",    descriptor.getDefaultValue());
        assertNull       ("minimumValue",              descriptor.getMinimumValue());
        assertNull       ("maximumValue",              descriptor.getMaximumValue());
        assertEquals     ("minimumOccurs", 1,          descriptor.getMinimumOccurs());
        assertEquals     ("maximumOccurs", 1,          descriptor.getMaximumOccurs());
        /*
         * Invalid operation: element not in the list of valid elements.
         */
        try {
            create("Enumeration param", String.class, enumeration, "Pear");
        } catch (IllegalArgumentException e) {
            assertEquals("Parameter “Enumeration param” can not take the “Pear” value.", e.getMessage());
        }
    }
}
