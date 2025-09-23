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
import javax.measure.Unit;
import org.apache.sis.measure.Range;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.ImmutableIdentifier;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.io.wkt.Convention;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.opengis.test.Validators.validate;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertMessageContains;
import static org.apache.sis.referencing.Assertions.assertWktEquals;


/**
 * Tests the {@link DefaultParameterDescriptor} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
public final class DefaultParameterDescriptorTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultParameterDescriptorTest() {
    }

    /**
     * Creates the map of properties to be given to {@link DefaultParameterDescriptor} constructor.
     *
     * @param  name  the parameter name.
     * @return the properties to be given to descriptor constructor.
     */
    private static Map<String,Object> properties(final String name) {
        final var properties = new HashMap<String,Object>(4);
        assertNull(properties.put(DefaultParameterDescriptor.NAME_KEY, name));
        assertNull(properties.put(DefaultParameterDescriptor.LOCALE_KEY, Locale.US));
        return properties;
    }

    /**
     * Creates a descriptor for an optional parameter without default value, minimum or maximum value.
     *
     * @param  name  the parameter name.
     * @param  type  the type of values.
     * @return the parameter descriptor.
     */
    static <T> DefaultParameterDescriptor<T> createSimpleOptional(final String name, final Class<T> type) {
        return new DefaultParameterDescriptor<>(properties(name), 0, 1, type, null, null, null);
    }

    /**
     * Creates a descriptor for a mandatory parameter in a range of integer values.
     *
     * @param  name          the parameter name.
     * @param  minimumValue  the minimum parameter value.
     * @param  maximumValue  the maximum parameter value.
     * @param  defaultValue  the default value for the parameter.
     * @return the parameter descriptor for the given range of values.
     */
    static DefaultParameterDescriptor<Integer> create(final String name,
            final int minimumValue, final int maximumValue, final int defaultValue)
    {
        return new DefaultParameterDescriptor<>(properties(name), 1, 1, Integer.class,
                NumberRange.create(minimumValue, true, maximumValue, true), null, defaultValue);
    }

    /**
     * Creates a descriptor for a mandatory parameter in a range of floating point values.
     *
     * @param  name          the parameter name.
     * @param  minimumValue  the minimum parameter value, or {@link Double#NEGATIVE_INFINITY} if none.
     * @param  maximumValue  the maximum parameter value, or {@link Double#POSITIVE_INFINITY} if none.
     * @param  defaultValue  the default value for the parameter, or {@link Double#NaN} if none.
     * @param  unit          the unit for default, minimum and maximum values.
     * @return the parameter descriptor for the given range of values.
     */
    static DefaultParameterDescriptor<Double> create(final String name,
            final double minimumValue, final double maximumValue, final double defaultValue, final Unit<?> unit)
    {
        return new DefaultParameterDescriptor<>(properties(name), 1, 1, Double.class,
                MeasurementRange.create(minimumValue, true, maximumValue, true, unit), null,
                Double.isNaN(defaultValue) ? null : defaultValue);
    }

    /**
     * Creates a descriptor for a parameter restricted to a set of valid values.
     * This is typically (but not necessarily) a code list parameter.
     *
     * @param  name          the parameter name.
     * @param  type          the type of values.
     * @param  validValues   the valid values.
     * @param  defaultValue  the default value for the parameter.
     * @return the parameter descriptor for the given range of values.
     */
    static <T> DefaultParameterDescriptor<T> create(final String name, final Class<T> type,
            final T[] validValues, final T defaultValue)
    {
        return new DefaultParameterDescriptor<>(properties(name), 1, 1, type, null, validValues, defaultValue);
    }

    /**
     * Creates a descriptor for an array of {@code double[]} values.
     *
     * @param  name          the parameter name.
     * @param  minimumValue  the minimum parameter value, or {@link Double#NEGATIVE_INFINITY} if none.
     * @param  maximumValue  the maximum parameter value, or {@link Double#POSITIVE_INFINITY} if none.
     * @param  unit          the unit for minimum and maximum values.
     * @return the parameter descriptor for the given range of values.
     */
    static DefaultParameterDescriptor<double[]> createForArray(final String name,
            final double minimumValue, final double maximumValue, final Unit<?> unit)
    {
        final var valueDomain = MeasurementRange.create(minimumValue, true, maximumValue, true, unit);
        return new DefaultParameterDescriptor<>(properties(name), 1, 1, double[].class, valueDomain, null, null);
    }

    /**
     * Creates a descriptor with the given EPSG identifier.
     *
     * @param  name  the parameter name.
     * @param  code  the parameter identifier.
     * @return the descriptor with the given EPSG identifier.
     */
    public static DefaultParameterDescriptor<Double> createEPSG(final String name, final short code) {
        final Map<String, Object> properties = properties(name);
        assertNull(properties.put(DefaultParameterDescriptor.IDENTIFIERS_KEY,
                new ImmutableIdentifier(Citations.EPSG, Constants.EPSG, Short.toString(code))));
        return new DefaultParameterDescriptor<>(properties, 0, 1, Double.class, null, null, null);
    }

    /**
     * Tests the creation of a simple descriptor for an optional parameter without minimum or maximum value.
     */
    @Test
    public void testOptionalInteger() {
        final var descriptor = createSimpleOptional("Simple param", Integer.class);
        assertEquals("Simple param", descriptor.getName().getCode());
        assertEquals("Integer",      descriptor.getValueType().toString());
        assertEquals(Integer.class,  descriptor.getValueClass());
        assertNull  (                descriptor.getValidValues());
        assertNull  (                descriptor.getDefaultValue());
        assertNull  (                descriptor.getMinimumValue());
        assertNull  (                descriptor.getMaximumValue());
        assertEquals(0,              descriptor.getMinimumOccurs());
        assertEquals(1,              descriptor.getMaximumOccurs());
    }

    /**
     * Tests {@code DefaultParameterDescriptor} constructor
     * with valid and invalid minimum and maximum values.
     */
    @Test
    @SuppressWarnings("UnnecessaryBoxing")
    public void testRangeValidation() {
        var e = assertThrows(IllegalArgumentException.class, () -> create("Test range", 20, 4, 12));
        assertMessageContains(e, "Range [20 … 4] is not valid.");

        final var descriptor = create("Test range", 4, 20, 12);
        assertEquals("Test range",        descriptor.getName().getCode());
        assertEquals("Integer",           descriptor.getValueType().toString());
        assertEquals(Integer.class,       descriptor.getValueClass());
        assertNull  (                     descriptor.getValidValues());
        assertEquals(Integer.valueOf(12), descriptor.getDefaultValue());
        assertEquals(Integer.valueOf( 4), descriptor.getMinimumValue());
        assertEquals(Integer.valueOf(20), descriptor.getMaximumValue());
        assertEquals(1,                   descriptor.getMinimumOccurs());
        assertEquals(1,                   descriptor.getMaximumOccurs());
    }

    /**
     * Tests {@code DefaultParameterDescriptor} constructor with an invalid default value.
     */
    @Test
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
        final var descriptor = create("Length measure", 4, 20, 12, Units.METRE);
        assertEquals("Length measure",   descriptor.getName().getCode());
        assertEquals("Real",             descriptor.getValueType().toString());
        assertEquals(Double.class,       descriptor.getValueClass());
        assertEquals(Double.valueOf(12), descriptor.getDefaultValue());
        assertEquals(Double.valueOf( 4), descriptor.getMinimumValue());
        assertEquals(Double.valueOf(20), descriptor.getMaximumValue());
        assertEquals(Units.METRE,        descriptor.getUnit());
        validate(descriptor);
    }

    /**
     * Verifies that we cannot assign unit of measurements to non-numerical values.
     */
    @Test
    public void testStringType() {
        final var valueDomain = new Range<>(String.class, "AAA", true, "BBB", true);
        final var descriptor  = new DefaultParameterDescriptor<>(properties("String param"),
                0, 1, String.class, valueDomain, null, "ABC");

        assertEquals("String param",    descriptor.getName().getCode());
        assertEquals("CharacterString", descriptor.getValueType().toString());
        assertEquals(String.class,      descriptor.getValueClass());
        assertNull  (                   descriptor.getValidValues());
        assertSame  (valueDomain,       descriptor.getValueDomain());
        assertEquals("ABC",             descriptor.getDefaultValue());
        assertEquals("AAA",             descriptor.getMinimumValue());
        assertEquals("BBB",             descriptor.getMaximumValue());
        assertEquals(0,                 descriptor.getMinimumOccurs());
        assertEquals(1,                 descriptor.getMaximumOccurs());
        assertNull  (                   descriptor.getUnit());
    }

    /**
     * Tests a descriptor for a parameter restricted to some values.
     * This is typically (but not necessarily) a code list parameter.
     */
    @Test
    public void testEnumeration() {
        final String[] enumeration = {"Apple", "Orange", "りんご"};
        final var descriptor = create("Enumeration param", String.class, enumeration, "Apple");

        assertEquals     ("Enumeration param", descriptor.getName().getCode());
        assertEquals     ("CharacterString",   descriptor.getValueType().toString());
        assertEquals     (String.class,        descriptor.getValueClass());
        assertArrayEquals(enumeration,         descriptor.getValidValues().toArray());
        assertEquals     ("Apple",             descriptor.getDefaultValue());
        assertNull       (                     descriptor.getMinimumValue());
        assertNull       (                     descriptor.getMaximumValue());
        assertEquals     (1,                   descriptor.getMinimumOccurs());
        assertEquals     (1,                   descriptor.getMaximumOccurs());
        assertNull       (                     descriptor.getUnit());
        /*
         * Invalid operation: element not in the list of valid elements.
         */
        var e = assertThrows(IllegalArgumentException.class,
                () -> create("Enumeration param", String.class, enumeration, "Pear"));
        assertEquals("Parameter “Enumeration param” cannot take the “Pear” value.", e.getMessage());
    }

    /**
     * Tests a descriptor for a parameter value of kind {@code double[]}.
     */
    @Test
    @SuppressWarnings("UnnecessaryBoxing")
    public void testArrayType() {
        final var descriptor = createForArray("Array param", 4, 9, Units.METRE);
        assertEquals("Array param",  descriptor.getName().getCode());
        assertEquals("Real",         descriptor.getValueType().toString());
        assertEquals(double[].class, descriptor.getValueClass());
        assertEquals(Units.METRE,    descriptor.getUnit());
        assertNull  (                descriptor.getValidValues());
        assertNull  (                descriptor.getDefaultValue());
        assertNull  (                descriptor.getMinimumValue());
        assertNull  (                descriptor.getMaximumValue());
        assertEquals(1,              descriptor.getMinimumOccurs());
        assertEquals(1,              descriptor.getMaximumOccurs());

        final Range<?> valueDomain = descriptor.getValueDomain();
        assertNotNull(valueDomain);
        assertEquals(Double.class,      valueDomain.getElementType());
        assertEquals(Double.valueOf(4), valueDomain.getMinValue());
        assertEquals(Double.valueOf(9), valueDomain.getMaxValue());
        /*
         * Invalid operation: wrong type of range value.
         */
        var e = assertThrows(IllegalArgumentException.class,
                () -> new DefaultParameterDescriptor<>(properties("Array param"), 0, 1, double[].class,
                        NumberRange.create(4, true, 9, true), null, null));
        assertEquals("Argument ‘valueDomain’ cannot be an instance of ‘Range<Integer>’.", e.getMessage());
    }

    /**
     * Tests the WKT representation.
     */
    @Test
    public void testWKT() {
        final DefaultParameterDescriptor<Double> descriptor = create("Real number", 4, 8, 5, Units.METRE);
        assertWktEquals(Convention.WKT2, "PARAMETER[“Integer param”, 5]", create("Integer param", 4, 8, 5));
        assertWktEquals(Convention.WKT2, "PARAMETER[“Real number”, 5.0, LENGTHUNIT[“metre”, 1]]", descriptor);
        assertWktEquals(Convention.WKT2_SIMPLIFIED, "Parameter[“Real number”, 5.0, Unit[“metre”, 1]]", descriptor);
    }

    /**
     * Tests WKT formatting of a parameter having an identifier.
     *
     * @see DefaultParameterDescriptorGroupTest#testIdentifiedParameterWKT()
     */
    @Test
    public void testIdentifiedParameterWKT() {
        final DefaultParameterDescriptor<Double> descriptor = createEPSG("A0", Constants.EPSG_A0);
        assertWktEquals(Convention.WKT2, "PARAMETER[“A0”, ID[“EPSG”, 8623, URI[“urn:ogc:def:parameter:EPSG::8623”]]]", descriptor);
    }
}
