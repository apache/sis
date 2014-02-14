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

import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import javax.measure.unit.Unit;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.datum.VerticalDatumType;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.InvalidParameterTypeException;
import org.opengis.parameter.InvalidParameterValueException;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static java.lang.StrictMath.*;
import static org.opengis.test.Validators.*;
import static org.apache.sis.test.MetadataAssert.*;


/**
 * Tests the {@link DefaultParameterValue} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.1)
 * @version 0.4
 * @module
 */
@DependsOn(DefaultParameterDescriptorTest.class)
public final strictfp class DefaultParameterValueTest extends TestCase {
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
     * Constructs an optional parameter initialized to the given value.
     * The descriptor has no default value, no minimum and no maximum.
     *
     * @param  name  The parameter name.
     * @param  value The parameter value.
     * @return A new parameter instance for the given name and value.
     */
    private static DefaultParameterValue<Integer> createOptional(final String name, final int value) {
        final DefaultParameterValue<Integer> parameter = new DefaultParameterValue<>(
                DefaultParameterDescriptorTest.createSimpleOptional(name, Integer.class));
        parameter.setValue(value, null);
        return parameter;
    }

    /**
     * Constructs a mandatory parameter initialize to the given value and unit.
     *
     * @param name  The parameter name.
     * @param value The parameter value.
     * @param unit  The unit for the parameter value.
     * @return A new parameter instance for the given name and value.
     */
    private static DefaultParameterValue<Double> create(final String name, final double value, final Unit<?> unit) {
        final ParameterDescriptor<Double> descriptor = DefaultParameterDescriptorTest.create(
                name, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, unit);
        final DefaultParameterValue<Double> parameter = new DefaultParameterValue<>(descriptor);
        parameter.setValue(value, unit);
        return parameter;
    }

    /**
     * Tests a parameter for an integer value.
     * First this method verifies the properties of the newly created instance, then attempts some
     * invalid operations on it. We test that the invalid operations throw the expected exceptions.
     */
    @Test
    public void testInteger() {
        final ParameterValue<Integer> parameter = createOptional("Integer param", 14);
        final ParameterDescriptor<Integer> descriptor = parameter.getDescriptor();
        validate(parameter);

        assertEquals("name",        "Integer param",     descriptor.getName().getCode());
        assertEquals("type",        Integer.class,       descriptor.getValueClass());
        assertNull  ("defaultUnit",                      descriptor.getUnit());
        assertNull  ("unit",                             parameter .getUnit());
        assertNull  ("defaultValue",                     descriptor.getDefaultValue());
        assertEquals("value",       Integer.valueOf(14), parameter .getValue());
        assertEquals("intValue",    14,                  parameter .intValue());
        assertEquals("doubleValue", 14,                  parameter .doubleValue(), STRICT);
        assertNull  ("minimum",                          descriptor.getMinimumValue());
        assertNull  ("maximum",                          descriptor.getMaximumValue());
        assertNull  ("validValues",                      descriptor.getValidValues());
        /*
         * Invalid operation: this parameter does not have unit of measurement.
         */
        try {
            parameter.doubleValue(SI.METRE);
            fail("doubleValue(METRE)");
        } catch (IllegalStateException exception) {
            final String message = exception.getMessage();
            assertTrue(message, message.contains("Integer param"));
        }
        /*
         * Invalid operation: this parameter is an integer, not a string.
         * While we could convert the integer to a string, in the context
         * of map projection parameters this is usually an error.
         */
        try {
            parameter.stringValue();
            fail("stringValue()");
        } catch (InvalidParameterTypeException exception) {
            final String message = exception.getMessage();
            assertTrue(message, message.contains("Integer"));
            assertEquals("Integer param", exception.getParameterName());
        }
        /*
         * Set values. In the case of null value, since this parameter is optional
         * the value should stay null (not substituted by the default value).
         */
        parameter.setValue(-15);
        assertEquals(Integer.valueOf(-15), parameter.getValue());
        parameter.setValue(null);
        assertNull(parameter.getValue());
        validate(parameter);
    }

    /**
     * Tests a parameter bounded by some range of integer numbers. This method try to set values inside and
     * outside the range of valid values, and verify that invalid values causes an exception to be thrown.
     * This method tests also the usage of values of the wrong type.
     */
    @Test
    @DependsOnMethod("testInteger")
    public void testBoundedInteger() {
        final DefaultParameterValue<Integer> parameter = new DefaultParameterValue<>(
                DefaultParameterDescriptorTest.create("Bounded param", 15, -30, +40));
        assertEquals(Integer.class, parameter.getDescriptor().getValueClass());
        assertEquals(      "value", Integer.valueOf(15), parameter.getValue());
        assertEquals(   "intValue", 15, parameter.intValue());
        assertEquals("doubleValue", 15, parameter.doubleValue(), STRICT);
        validate(parameter);
        /*
         * Set a value inside the range of valid values.
         */
        parameter.setValue(12);
        assertEquals(      "value", Integer.valueOf(12), parameter.getValue());
        assertEquals(   "intValue", 12, parameter.intValue());
        assertEquals("doubleValue", 12, parameter.doubleValue(), STRICT);
        validate(parameter);
        /*
         * Invalid operations: attempt to set values out of range.
         */
        try {
            parameter.setValue(50);
            fail("setValue(> max)");
        } catch (InvalidParameterValueException exception) {
            final String message = exception.getMessage();
            assertTrue(message, message.contains("Bounded param"));
            assertEquals("Bounded param", exception.getParameterName());
        }
        try {
            parameter.setValue(-40);
            fail("setValue(< min)");
        } catch (InvalidParameterValueException exception) {
            final String message = exception.getMessage();
            assertTrue(message, message.contains("Bounded param"));
            assertEquals("Bounded param", exception.getParameterName());
        }
        /*
         * Invalid operation: attempt to set a floating point value.
         */
        try {
            parameter.setValue(10.5);
            fail("setValue(double)");
        } catch (InvalidParameterValueException exception) {
            final String message = exception.getMessage();
            assertTrue(message, message.contains("Integer"));
            assertEquals("Bounded param", exception.getParameterName());
        }
        /*
         * Try again to set a floating point value, but this time
         * the value can be converted to an integer.
         */
        parameter.setValue(10.0);
        assertEquals(      "value", Integer.valueOf(10), parameter.getValue());
        assertEquals(   "intValue", 10, parameter.intValue());
        assertEquals("doubleValue", 10, parameter.doubleValue(), STRICT);
        validate(parameter);
        /*
         * Invalid operation: set the same value than above, but with a unit of measurement.
         * This shall be an invalid operation since we created a unitless parameter.
         */
        try {
            parameter.setValue(10.0, SI.METRE);
            fail("setValue(double,Unit)");
        } catch (InvalidParameterValueException exception) {
            final String message = exception.getMessage();
            assertTrue(message, message.contains("Bounded param"));
            assertEquals("Bounded param", exception.getParameterName());
        }
    }

    /**
     * Tests a parameter for a floating point value with a unit of measurement.
     */
    @Test
    public void testMeasure() {
        final DefaultParameterValue<Double> parameter = create("Numerical param", 3, SI.METRE);
        final ParameterDescriptor<Double> descriptor = parameter.getDescriptor();
        validate(parameter);

        assertEquals("name",        "Numerical param", descriptor.getName().getCode());
        assertEquals("defaultUnit", SI.METRE,          descriptor.getUnit());
        assertEquals("unit",        SI.METRE,          parameter .getUnit());
        assertNull  ("defaultValue",                   descriptor.getDefaultValue());
        assertEquals("value",       Double.valueOf(3), parameter .getValue());
        assertEquals("intValue",      3,               parameter .intValue());
        assertEquals("doubleValue",   3,               parameter .doubleValue(), STRICT);
        assertEquals("doubleValue", 300,               parameter .doubleValue(SI.CENTIMETRE), STRICT);
        assertNull  ("minimum",                        descriptor.getMinimumValue());
        assertNull  ("maximum",                        descriptor.getMaximumValue());
        assertNull  ("validValues",                    descriptor.getValidValues());
        /*
         * Invalid operation: this parameter is a real number, not a string.
         * While we could convert the number to a string, in the context of
         * map projection parameters this is usually an error.
         */
        try {
            parameter.stringValue();
            fail("stringValue()");
        } catch (InvalidParameterTypeException exception) {
            final String message = exception.getMessage();
            assertTrue(message, message.contains("Double"));
            assertEquals("Numerical param", exception.getParameterName());
        }
        /*
         * Sets a value in centimetres.
         */
        parameter.setValue(400, SI.CENTIMETRE);
        assertEquals("unit",        SI.CENTIMETRE, parameter.getUnit());
        assertEquals("doubleValue", 400, parameter.doubleValue(),              STRICT);
        assertEquals("doubleValue", 400, parameter.doubleValue(SI.CENTIMETRE), STRICT);
        assertEquals("doubleValue",   4, parameter.doubleValue(SI.METRE),      STRICT);
        validate(parameter);
    }

    /**
     * Tests a parameter bounded by some range of floating point numbers, and tests values
     * inside and outside that range. Tests also the usage of values of the wrong type.
     */
    @Test
    @DependsOnMethod("testMeasure")
    public void testBoundedDouble() {
        final DefaultParameterValue<Double> parameter = new DefaultParameterValue<>(
                DefaultParameterDescriptorTest.create("Bounded param", 15.0, -30.0, +40.0, null));
        assertEquals(Double.class, parameter.getDescriptor().getValueClass());
        assertEquals(      "value", Double.valueOf(15), parameter.getValue());
        assertEquals(   "intValue", 15, parameter.intValue());
        assertEquals("doubleValue", 15, parameter.doubleValue(), STRICT);
        validate(parameter);

        parameter.setValue(12.0);
        assertEquals(      "value", Double.valueOf(12), parameter.getValue());
        assertEquals(   "intValue", 12, parameter.intValue());
        assertEquals("doubleValue", 12, parameter.doubleValue(), STRICT);
        validate(parameter);

        try {
            parameter.setValue(50.0);
            fail("setValue(> max)");
        } catch (InvalidParameterValueException exception) {
            // This is the expected exception.
            assertEquals("Bounded param", exception.getParameterName());
        }
        try {
            parameter.setValue(-40.0);
            fail("setValue(< min)");
        } catch (InvalidParameterValueException exception) {
            // This is the expected exception.
            assertEquals("Bounded param", exception.getParameterName());
        }
        try {
            parameter.setValue("12");
            fail("setValue(String)");
        } catch (InvalidParameterValueException exception) {
            final String message = exception.getMessage();
            assertTrue(message, message.contains("Bounded param"));
            assertEquals("Bounded param", exception.getParameterName());
        }
    }

    /**
     * Tests a floating point parameter with a unit of measurement bounded by a minimum and maximum values.
     */
    @Test
    @DependsOnMethod({"testMeasure", "testBoundedDouble"})
    public void testBoundedMeasure() {
        final DefaultParameterValue<Double> parameter = new DefaultParameterValue<>(
                DefaultParameterDescriptorTest.create("Length measure", 12, 4, 20, SI.METRE));
        assertEquals("value",    Double.valueOf(12), parameter.getValue());
        assertEquals("intValue", 12,                 parameter.intValue());
        assertEquals("unit",     SI.METRE,           parameter.getUnit());
        validate(parameter);

        for (int i=4; i<=20; i++) {
            parameter.setValue(i);
            assertEquals("value", Double.valueOf(i), parameter.getValue());
            assertEquals("unit",  SI.METRE,          parameter.getUnit());
            assertEquals("value", i,                 parameter.doubleValue(SI.METRE), STRICT);
            assertEquals("value", 100*i,             parameter.doubleValue(SI.CENTIMETRE), STRICT);
        }
        try {
            parameter.setValue(3.0);
            fail("setValue(< min)");
        } catch (InvalidParameterValueException exception) {
            assertEquals("Length measure", exception.getParameterName());
        }
        try {
            parameter.setValue("12");
            fail("setValue(Sring)");
        } catch (InvalidParameterValueException exception) {
            assertEquals("Length measure", exception.getParameterName());
        }
        for (int i=400; i<=2000; i+=100) {
            parameter.setValue(i, SI.CENTIMETRE);
            assertEquals("value", Double.valueOf(i), parameter.getValue());
            assertEquals("unit",  SI.CENTIMETRE,     parameter.getUnit());
            assertEquals("value", i/100,             parameter.doubleValue(SI.METRE), EPS);
        }
    }

    /**
     * Tests a parameter for a code list.
     */
    @Test
    public void testCodeList() {
        final AxisDirection[] directions = {
            AxisDirection.NORTH,
            AxisDirection.SOUTH,
            AxisDirection.DISPLAY_LEFT,
            AxisDirection.PAST
        };
        final ParameterDescriptor<AxisDirection> descriptor = DefaultParameterDescriptorTest.create(
                "Direction", AxisDirection.class, directions, AxisDirection.NORTH);
        final DefaultParameterValue<AxisDirection> parameter = new DefaultParameterValue<>(descriptor);
        validate(parameter);

        assertEquals     ("name",         "Direction",         descriptor.getName().getCode());
        assertEquals     ("defaultValue", AxisDirection.NORTH, descriptor.getDefaultValue());
        assertEquals     ("value",        AxisDirection.NORTH, parameter .getValue());
        assertNull       ("defaultUnit",                       descriptor.getUnit());
        assertNull       ("unit",                              parameter .getUnit());
        assertNull       ("minimum",                           descriptor.getMinimumValue());
        assertNull       ("maximum",                           descriptor.getMaximumValue());
        assertArrayEquals("validValues", directions,           descriptor.getValidValues().toArray());
        /*
         * Invalid operation: attempt to get the value as a 'double' is not allowed.
         */
        try {
            parameter.doubleValue();
            fail("doubleValue shall not be allowed on AxisDirection");
        } catch (InvalidParameterTypeException exception) {
            final String message = exception.getMessage();
            assertTrue(message, message.contains("AxisDirection"));
            assertEquals("Direction", exception.getParameterName());
        }
        /*
         * Set a valid value.
         */
        parameter.setValue(AxisDirection.PAST);
        assertEquals("value", AxisDirection.PAST, parameter.getValue());
        /*
         * Invalid operation: set a value of valid type but not in the list of valid values.
         */
        try {
            parameter.setValue(AxisDirection.GEOCENTRIC_X);
            fail("setValue(AxisDirection.UP)");
        } catch (InvalidParameterValueException exception) {
            final String message = exception.getMessage();
            assertTrue(message, message.contains("Direction"));
            assertTrue(message, message.contains("Geocentric X"));
            assertEquals("Direction", exception.getParameterName());
        }
        /*
         * Invalid operation: attempt to set a value of wrong type.
         */
        try {
            parameter.setValue(VerticalDatumType.BAROMETRIC);
            fail("setValue(VerticalDatumType)");
        } catch (InvalidParameterValueException exception) {
            final String message = exception.getMessage();
            assertTrue(message, message.contains("Direction"));
            assertTrue(message, message.contains("VerticalDatumType"));
            assertTrue(message, message.contains("AxisDirection"));
            assertEquals("Direction", exception.getParameterName());
        }
    }

    /**
     * Tests the creation of many parameters for integer and floating point values.
     * Some on those values are cached (e.g. 0, 90, 360) because frequently used.
     * It should be transparent to the user.
     * Test also unit conversions (degrees to radians in this case).
     *
     * @todo Tests parallel instantiation on JDK8.
     */
    @Test
    @DependsOnMethod({"testBoundedInteger", "testBoundedDouble"})
    public void testMany() {
        DefaultParameterValue<? extends Number> p;
        ParameterDescriptor<? extends Number> d;
        for (int i=-500; i<=500; i++) {
            p = createOptional("Unitlesss integer value", i);
            d = p.getDescriptor();
            validate(p);

            assertNotNull("Expected a descriptor.",       d);
            assertNull   ("Expected no default value.",   d.getDefaultValue());
            assertNull   ("Expected no minimal value.",   d.getMinimumValue());
            assertNull   ("Expected no maximal value.",   d.getMaximumValue());
            assertNull   ("Expected no enumeration.",     d.getValidValues());
            assertEquals ("Expected integer type.",       Integer.class, d.getValueClass());
            assertTrue   ("Expected integer type.",       p.getValue() instanceof Integer);
            assertNull   ("Expected unitless parameter.", p.getUnit());
            assertEquals ("Expected integer value", i,    p.intValue());
            assertEquals ("Expected integer value", i,    p.doubleValue(), STRICT);

            p = create("Unitlesss double value", i, null);
            d = p.getDescriptor();
            validate(p);

            assertNotNull("Expected a descriptor.",       d);
            assertNull   ("Expected no default value.",   d.getDefaultValue());
            assertNull   ("Expected no minimal value.",   d.getMinimumValue());
            assertNull   ("Expected no maximal value.",   d.getMaximumValue());
            assertNull   ("Expected no enumeration.",     d.getValidValues());
            assertEquals ("Expected double type.",        Double.class, d.getValueClass());
            assertTrue   ("Expected double type.",        p.getValue() instanceof Double);
            assertNull   ("Expected unitless parameter.", p.getUnit());
            assertEquals ("Expected integer value", i,    p.intValue());
            assertEquals ("Expected integer value", i,    p.doubleValue(), STRICT);

            p = create("Dimensionless double value", i, Unit.ONE);
            d = p.getDescriptor();
            validate(p);

            assertNotNull("Expected a descriptor.",       d);
            assertNull   ("Expected no default value.",   d.getDefaultValue());
            assertNull   ("Expected no minimal value.",   d.getMinimumValue());
            assertNull   ("Expected no maximal value.",   d.getMaximumValue());
            assertNull   ("Expected no enumeration.",     d.getValidValues());
            assertEquals ("Expected double type.",        Double.class, d.getValueClass());
            assertTrue   ("Expected double type.",        p.getValue() instanceof Double);
            assertEquals ("Expected dimensionless.",      Unit.ONE, p.getUnit());
            assertEquals ("Expected integer value", i,    p.intValue());
            assertEquals ("Expected integer value", i,    p.doubleValue(), STRICT);

            p = create("Angular double value", i, NonSI.DEGREE_ANGLE);
            d = p.getDescriptor();
            validate(p);

            assertNotNull("Expected a descriptor.",       d);
            assertNull   ("Expected no default value.",   d.getDefaultValue());
            assertNull   ("Expected no minimal value.",   d.getMinimumValue());
            assertNull   ("Expected no maximal value.",   d.getMaximumValue());
            assertNull   ("Expected no enumeration.",     d.getValidValues());
            assertEquals ("Expected double type.",        Double.class, d.getValueClass());
            assertTrue   ("Expected double type.",        p.getValue() instanceof Double);
            assertEquals ("Expected angular unit.",       NonSI.DEGREE_ANGLE, p.getUnit());
            assertEquals ("Expected integer value", i,    p.intValue());
            assertEquals ("Expected integer value", i,    p.doubleValue(), STRICT);
            assertEquals ("Expected unit conversion.", toRadians(i), p.doubleValue(SI.RADIAN), EPS);
        }
    }

    /**
     * Tests clone.
     */
    @Test
    public void testClone() {
        final DefaultParameterValue<Double> parameter = create("Clone test", 3, SI.METRE);
        assertEquals("equals(clone)", parameter, parameter.clone());
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialization() {
        final DefaultParameterValue<Double> parameter = create("Serialization test", 3, SI.METRE);
        assertNotSame(parameter, assertSerializedEquals(parameter));
    }

    /**
     * Tests WKT formatting.
     */
    @Test
    public void testWKT() {
        assertWktEquals("Parameter[“WKT test”, 4]", createOptional("WKT test", 4));
        assertWktEquals("Parameter[“WKT test”, 30.0]", create("WKT test", 30, SI.CENTIMETRE));
    }
}
