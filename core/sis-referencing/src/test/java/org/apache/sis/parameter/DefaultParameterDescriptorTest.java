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
import org.apache.sis.measure.Range;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.opengis.test.Validators.*;
import static org.apache.sis.test.MetadataAssert.*;


/**
 * Tests the {@link DefaultParameterDescriptor} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
 * @version 0.7
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
    private static Map<String,Object> properties(final String name) {
        final Map<String,Object> properties = new HashMap<String,Object>(4);
        assertNull(properties.put(DefaultParameterDescriptor.NAME_KEY, name));
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
        return new DefaultParameterDescriptor<T>(properties(name), 0, 1, type, null, null, null);
    }

    /**
     * Creates a descriptor for a mandatory parameter in a range of integer values.
     *
     * @param  name         The parameter name.
     * @param  minimumValue The minimum parameter value.
     * @param  maximumValue The maximum parameter value.
     * @param  defaultValue The default value for the parameter.
     * @return The parameter descriptor for the given range of values.
     */
    static DefaultParameterDescriptor<Integer> create(final String name,
            final int minimumValue, final int maximumValue, final int defaultValue)
    {
        return new DefaultParameterDescriptor<Integer>(properties(name), 1, 1, Integer.class,
                NumberRange.create(minimumValue, true, maximumValue, true), null, defaultValue);
    }

    /**
     * Creates a descriptor for a mandatory parameter in a range of floating point values.
     *
     * @param  name         The parameter name.
     * @param  minimumValue The minimum parameter value, or {@link Double#NEGATIVE_INFINITY} if none.
     * @param  maximumValue The maximum parameter value, or {@link Double#POSITIVE_INFINITY} if none.
     * @param  defaultValue The default value for the parameter, or {@link Double#NaN} if none.
     * @param  unit         The unit for default, minimum and maximum values.
     * @return The parameter descriptor for the given range of values.
     */
    static DefaultParameterDescriptor<Double> create(final String name,
            final double minimumValue, final double maximumValue, final double defaultValue, final Unit<?> unit)
    {
        return new DefaultParameterDescriptor<Double>(properties(name), 1, 1, Double.class,
                MeasurementRange.create(minimumValue, true, maximumValue, true, unit), null,
                Double.isNaN(defaultValue) ? null : defaultValue);
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
        return new DefaultParameterDescriptor<T>(properties(name), 1, 1, type, null, validValues, defaultValue);
    }

    /**
     * Creates a descriptor for an array of {@code double[]} values.
     *
     * @param  name         The parameter name.
     * @param  minimumValue The minimum parameter value, or {@link Double#NEGATIVE_INFINITY} if none.
     * @param  maximumValue The maximum parameter value, or {@link Double#POSITIVE_INFINITY} if none.
     * @param  unit         The unit for minimum and maximum values.
     * @return The parameter descriptor for the given range of values.
     */
    static DefaultParameterDescriptor<double[]> createForArray(final String name,
            final double minimumValue, final double maximumValue, final Unit<?> unit)
    {
        final MeasurementRange<Double> valueDomain = MeasurementRange.create(minimumValue, true, maximumValue, true, unit);
        return new DefaultParameterDescriptor<double[]>(properties(name), 1, 1, double[].class, valueDomain, null, null);
    }

    /**
     * Creates a descriptor with the given EPSG identifier.
     *
     * @param  name The parameter name.
     * @param  code The parameter identifier.
     * @return The descriptor with the given EPSG identifier.
     */
    public static DefaultParameterDescriptor<Double> createEPSG(final String name, final short code) {
        final Map<String, Object> properties = properties(name);
        assertNull(properties.put(DefaultParameterDescriptor.IDENTIFIERS_KEY,
                new ImmutableIdentifier(Citations.EPSG, Constants.EPSG, Short.toString(code))));
        return new DefaultParameterDescriptor<Double>(properties, 0, 1, Double.class, null, null, null);
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
    @SuppressWarnings("UnnecessaryBoxing")
    public void testRangeValidation() {
        try {
            create("Test range", 20, 4, 12);
            fail("minimum > maximum");
        } catch (IllegalArgumentException exception) {
            assertEquals("Range [20 … 4] is not valid.", exception.getMessage());
        }
        final ParameterDescriptor<Integer> descriptor = create("Test range", 4, 20, 12);
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
            create("Test default", 4, 20, 3);
            fail("defaultValue < minimum");
        } catch (IllegalArgumentException exception) {
            assertEquals("Value ‘Test default’ = 3 is invalid. Expected a value in the [4 … 20] range.", exception.getMessage());
        }
    }

    /**
     * Tests {@code DefaultParameterDescriptor} construction for {@link Double} type.
     */
    @Test
    @SuppressWarnings("UnnecessaryBoxing")
    public void testDoubleType() {
        final ParameterDescriptor<Double> descriptor = create("Length measure", 4, 20, 12, SI.METRE);
        assertEquals("name",         "Length measure",   descriptor.getName().getCode());
        assertEquals("unit",         SI.METRE,           descriptor.getUnit());
        assertEquals("class",        Double.class,       descriptor.getValueClass());
        assertEquals("defaultValue", Double.valueOf(12), descriptor.getDefaultValue());
        assertEquals("minimum",      Double.valueOf( 4), descriptor.getMinimumValue());
        assertEquals("maximum",      Double.valueOf(20), descriptor.getMaximumValue());
        validate(descriptor);
    }

    /**
     * Verifies that we can not assign unit of measurements to non-numerical values.
     */
    @Test
    public void testStringType() {
        final Range<String> valueDomain = new Range<String>(String.class, "AAA", true, "BBB", true);
        final DefaultParameterDescriptor<String> descriptor = new DefaultParameterDescriptor<String>(
                properties("String param"), 0, 1, String.class, valueDomain, null, "ABC");
        assertEquals("name", "String param",     descriptor.getName().getCode());
        assertEquals("valueClass", String.class, descriptor.getValueClass());
        assertNull  ("validValues",              descriptor.getValidValues());
        assertSame  ("valueDomain", valueDomain, descriptor.getValueDomain());
        assertEquals("defaultValue",  "ABC",     descriptor.getDefaultValue());
        assertEquals("minimumValue",  "AAA",     descriptor.getMinimumValue());
        assertEquals("maximumValue",  "BBB",     descriptor.getMaximumValue());
        assertEquals("minimumOccurs", 0,         descriptor.getMinimumOccurs());
        assertEquals("maximumOccurs", 1,         descriptor.getMaximumOccurs());
        assertNull  ("unit",                     descriptor.getUnit());
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
        assertNull       ("unit",                      descriptor.getUnit());
        /*
         * Invalid operation: element not in the list of valid elements.
         */
        try {
            DefaultParameterDescriptor<String> p = create("Enumeration param", String.class, enumeration, "Pear");
            fail("Should not be allowed to create " + p);
        } catch (IllegalArgumentException e) {
            assertEquals("Parameter “Enumeration param” can not take the “Pear” value.", e.getMessage());
        }
    }

    /**
     * Tests a descriptor for a parameter value of kind {@code double[]}.
     */
    @Test
    @DependsOnMethod("testDoubleType")
    @SuppressWarnings("UnnecessaryBoxing")
    public void testArrayType() {
        final DefaultParameterDescriptor<double[]> descriptor = createForArray("Array param", 4, 9, SI.METRE);
        assertEquals("name",       "Array param",  descriptor.getName().getCode());
        assertEquals("valueClass", double[].class, descriptor.getValueClass());
        assertEquals("unit",       SI.METRE,       descriptor.getUnit());
        assertNull  ("validValues",                descriptor.getValidValues());
        assertNull  ("defaultValue",               descriptor.getDefaultValue());
        assertNull  ("minimumValue",               descriptor.getMinimumValue());
        assertNull  ("maximumValue",               descriptor.getMaximumValue());
        assertEquals("minimumOccurs", 1,           descriptor.getMinimumOccurs());
        assertEquals("maximumOccurs", 1,           descriptor.getMaximumOccurs());

        final Range<?> valueDomain = descriptor.getValueDomain();
        assertNotNull("valueDomain", valueDomain);
        assertEquals(Double.class,      valueDomain.getElementType());
        assertEquals(Double.valueOf(4), valueDomain.getMinValue());
        assertEquals(Double.valueOf(9), valueDomain.getMaxValue());
        /*
         * Invalid operation: wrong type of range value.
         */
        try {
            DefaultParameterDescriptor<double[]> p = new DefaultParameterDescriptor<double[]>(properties("Array param"),
                    0, 1, double[].class, NumberRange.create(4, true, 9, true), null, null);
            fail("Should not be allowed to create " + p);
        } catch (IllegalArgumentException e) {
            assertEquals("Argument ‘valueDomain’ can not be an instance of ‘Range<Integer>’.", e.getMessage());
        }
    }

    /**
     * Tests the WKT representation.
     */
    @Test
    public void testWKT() {
        final DefaultParameterDescriptor<Double> descriptor = create("Real number", 4, 8, 5, SI.METRE);
        assertWktEquals("PARAMETER[“Integer param”, 5]", create("Integer param", 4, 8, 5));
        assertWktEquals("PARAMETER[“Real number”, 5.0, LENGTHUNIT[“metre”, 1]]", descriptor);
        assertWktEquals(Convention.WKT2_SIMPLIFIED, "Parameter[“Real number”, 5.0, Unit[“metre”, 1]]", descriptor);
    }

    /**
     * Tests WKT formatting of a parameter having an identifier.
     *
     * @see DefaultParameterDescriptorGroupTest#testIdentifiedParameterWKT()
     *
     * @since 0.6
     */
    @Test
    @DependsOnMethod("testWKT")
    public void testIdentifiedParameterWKT() {
        final DefaultParameterDescriptor<Double> descriptor = createEPSG("A0", Constants.EPSG_A0);
        assertWktEquals("PARAMETER[“A0”, ID[“EPSG”, 8623, URI[“urn:ogc:def:parameter:EPSG::8623”]]]", descriptor);
    }
}
