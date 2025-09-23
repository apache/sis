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

import static java.lang.StrictMath.*;
import javax.measure.Unit;
import org.opengis.metadata.citation.DateType;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.InvalidParameterTypeException;
import org.opengis.parameter.InvalidParameterValueException;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.measure.Units;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.opengis.test.Validators.validate;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertMessageContains;
import static org.apache.sis.test.Assertions.assertSerializedEquals;
import static org.apache.sis.referencing.Assertions.assertWktEquals;


/**
 * Tests the {@link DefaultParameterValue} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
@SuppressWarnings("UnnecessaryBoxing")
public final class DefaultParameterValueTest extends TestCase {
    /**
     * Small tolerance factor for floating point comparisons resulting from some calculation.
     */
    private static final double EPS = 1E-10;

    /**
     * Creates a new test case.
     */
    public DefaultParameterValueTest() {
    }

    /**
     * A subclass of {@code DefaultParameterValue} which store the value converted by {@link Verifier}.
     * This allows {@link DefaultParameterValueTest} methods to verify the conversion result.
     */
    @SuppressWarnings({"serial", "CloneableImplementsClone"})
    private static final class Watcher<T> extends DefaultParameterValue<T> {
        /** The value converted by {@link Verifier}. */
        T convertedValue;

        /** Creates a new parameter value for testing purpose. */
        Watcher(final DefaultParameterDescriptor<T> descriptor) {
            super(descriptor);
        }

        /** Automatically invoked when a new value is set. */
        @Override protected void validate(final T value) {
            convertedValue = value;
        }

        /** Asserts that the value and the converted value are equal to the expected one. */
        void assertValueEquals(final Object expected) {
            assertEquals(expected, getValue());
            assertEquals(expected, convertedValue);
        }

        /** Asserts that the value and the converted value are equal to the expected ones. */
        void assertValueEquals(final Object expected, final Object converted) {
            assertEquals(expected,  getValue());
            assertEquals(converted, convertedValue);
        }
    }

    /**
     * Constructs an optional parameter initialized to the given value.
     * The descriptor has no default value, no minimum and no maximum.
     *
     * @param  name   the parameter name.
     * @param  value  the parameter value.
     * @return a new parameter instance for the given name and value.
     */
    private static Watcher<Integer> createOptional(final String name, final int value) {
        final Watcher<Integer> parameter = new Watcher<>(
                DefaultParameterDescriptorTest.createSimpleOptional(name, Integer.class));
        parameter.setValue(value, null);
        return parameter;
    }

    /**
     * Constructs a mandatory parameter initialize to the given value and unit.
     *
     * @param  name   the parameter name.
     * @param  value  the parameter value.
     * @param  unit   the unit for the parameter value.
     * @return a new parameter instance for the given name and value.
     */
    private static Watcher<Double> create(final String name, final double value, final Unit<?> unit) {
        final Watcher<Double> parameter = new Watcher<>(DefaultParameterDescriptorTest.create(
                name, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN, unit));
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
        final Watcher<Integer> parameter = createOptional("Integer param", 14);
        final ParameterDescriptor<Integer> descriptor = parameter.getDescriptor();
        validate(parameter);

        assertEquals("Integer param", descriptor.getName().getCode());
        assertEquals(Integer.class,   descriptor.getValueClass());
        assertNull  (                 descriptor.getUnit());
        assertNull  (                 parameter .getUnit());
        assertNull  (                 descriptor.getDefaultValue());
        assertEquals((Integer) 14,    parameter .getValue());
        assertEquals(14,              parameter .intValue());
        assertEquals(14,              parameter .doubleValue());
        assertNull  (                 descriptor.getMinimumValue());
        assertNull  (                 descriptor.getMaximumValue());
        assertNull  (                 descriptor.getValidValues());
        /*
         * Invalid operation: this parameter does not have unit of measurement.
         */
        IllegalStateException ipt;
        ipt = assertThrows(IllegalStateException.class, () -> parameter.doubleValue(Units.METRE));
        assertMessageContains(ipt, "Integer param");
        /*
         * Invalid operation: this parameter is an integer, not a string.
         * While we could convert the integer to a string, in the context
         * of map projection parameters this is usually an error.
         */
        InvalidParameterTypeException exception;
        exception = assertThrows(InvalidParameterTypeException.class, () -> parameter.stringValue());
        assertMessageContains(exception, "Integer");
        assertEquals("Integer param", exception.getParameterName());
        /*
         * Set values. In the case of null value, since this parameter is optional
         * the value should stay null (not substituted by the default value).
         */
        parameter.setValue(-15);
        parameter.assertValueEquals(Integer.valueOf(-15));
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
    public void testBoundedInteger() {
        final Watcher<Integer> parameter = new Watcher<>(
                DefaultParameterDescriptorTest.create("Bounded param", -30, +40, 15));
        assertEquals(Integer.class, parameter.getDescriptor().getValueClass());
        assertEquals(Integer.valueOf(15), parameter.getValue());
        assertEquals(15, parameter.intValue());
        assertEquals(15, parameter.doubleValue());
        validate(parameter);
        /*
         * Set a value inside the range of valid values.
         */
        parameter.setValue(12);
        parameter.assertValueEquals(Integer.valueOf(12));
        assertEquals(12, parameter.intValue());
        assertEquals(12, parameter.doubleValue());
        validate(parameter);
        /*
         * Invalid operations: attempt to set values out of range.
         */
        InvalidParameterValueException exception;
        exception = assertThrows(InvalidParameterValueException.class, () -> parameter.setValue(50), "setValue(> max)");
        assertMessageContains(exception, "Bounded param");
        assertEquals("Bounded param", exception.getParameterName());

        exception = assertThrows(InvalidParameterValueException.class, () -> parameter.setValue(-40), "setValue(< min)");
        assertMessageContains(exception, "Bounded param");
        assertEquals("Bounded param", exception.getParameterName());
        /*
         * Invalid operation: attempt to set a floating point value.
         */
        exception = assertThrows(InvalidParameterValueException.class, () -> parameter.setValue(10.5), "setValue(double)");
        assertMessageContains(exception, "Integer");
        assertEquals("Bounded param", exception.getParameterName());
        /*
         * Try again to set a floating point value, but this time
         * the value can be converted to an integer.
         */
        parameter.setValue(10.0);
        parameter.assertValueEquals(Integer.valueOf(10));
        assertEquals(10, parameter.intValue());
        assertEquals(10, parameter.doubleValue());
        validate(parameter);
        /*
         * Invalid operation: set the same value as above, but with a unit of measurement.
         * This shall be an invalid operation since we created a unitless parameter.
         */
        exception = assertThrows(InvalidParameterValueException.class, () -> parameter.setValue(10.0, Units.METRE), "setValue(double,Unit)");
        assertMessageContains(exception, "Bounded param");
        assertEquals("Bounded param", exception.getParameterName());
    }

    /**
     * Tests a parameter for a floating point value with a unit of measurement.
     */
    @Test
    public void testMeasure() {
        final Watcher<Double> parameter = create("Numerical param", 3, Units.METRE);
        final ParameterDescriptor<Double> descriptor = parameter.getDescriptor();
        validate(parameter);

        assertEquals("Numerical param", descriptor.getName().getCode());
        assertEquals(Units.METRE,       descriptor.getUnit());
        assertEquals(Units.METRE,       parameter .getUnit());
        assertNull  (                   descriptor.getDefaultValue());
        assertEquals(Double.valueOf(3), parameter .getValue());
        assertEquals(  3,               parameter .intValue());
        assertEquals(  3,               parameter .doubleValue());
        assertEquals(300,               parameter .doubleValue(Units.CENTIMETRE));
        assertNull  (                   descriptor.getMinimumValue());
        assertNull  (                   descriptor.getMaximumValue());
        assertNull  (                   descriptor.getValidValues());
        /*
         * Invalid operation: this parameter is a real number, not a string.
         * While we could convert the number to a string, in the context of
         * map projection parameters this is usually an error.
         */
        InvalidParameterTypeException exception;
        exception = assertThrows(InvalidParameterTypeException.class, () -> parameter.stringValue());
        assertMessageContains(exception, "Double");
        assertEquals("Numerical param", exception.getParameterName());
        /*
         * Sets a value in centimetres.
         */
        parameter.setValue(400, Units.CENTIMETRE);
        parameter.assertValueEquals(Double.valueOf(400), Double.valueOf(4));
        assertEquals(Units.CENTIMETRE, parameter.getUnit());
        assertEquals(400, parameter.doubleValue());
        assertEquals(400, parameter.doubleValue(Units.CENTIMETRE));
        assertEquals(  4, parameter.doubleValue(Units.METRE));
        validate(parameter);
    }

    /**
     * Tests a parameter bounded by some range of floating point numbers, and tests values
     * inside and outside that range. Tests also the usage of values of the wrong type.
     */
    @Test
    public void testBoundedDouble() {
        final Watcher<Double> parameter = new Watcher<>(
                DefaultParameterDescriptorTest.create("Bounded param", -30.0, +40.0, 15.0, null));
        assertEquals(Double.class, parameter.getDescriptor().getValueClass());
        assertEquals(Double.valueOf(15), parameter.getValue());
        assertEquals(15, parameter.intValue());
        assertEquals(15, parameter.doubleValue());
        validate(parameter);

        parameter.setValue(12.0);
        parameter.assertValueEquals(Double.valueOf(12));
        assertEquals(12, parameter.intValue());
        assertEquals(12, parameter.doubleValue());
        validate(parameter);

        InvalidParameterValueException exception;
        exception = assertThrows(InvalidParameterValueException.class, () -> parameter.setValue(50.0), "setValue(> max)");
        assertEquals("Bounded param", exception.getParameterName());

        exception = assertThrows(InvalidParameterValueException.class, () -> parameter.setValue(-40.0), "setValue(< min)");
        assertEquals("Bounded param", exception.getParameterName());

        exception = assertThrows(InvalidParameterValueException.class, () -> parameter.setValue("12"), "setValue(String)");
        assertMessageContains(exception, "Bounded param");
        assertEquals("Bounded param", exception.getParameterName());
    }

    /**
     * Tests a floating point parameter with a unit of measurement bounded by a minimum and maximum values.
     */
    @Test
    public void testBoundedMeasure() {
        final Watcher<Double> parameter = new Watcher<>(
                DefaultParameterDescriptorTest.create("Length measure", 4, 20, 12, Units.METRE));
        assertEquals(Double.valueOf(12), parameter.getValue());
        assertEquals(12,                 parameter.intValue());
        assertEquals(Units.METRE,        parameter.getUnit());
        validate(parameter);

        for (int i=4; i<=20; i++) {
            parameter.setValue(i);
            parameter.assertValueEquals(Double.valueOf(i));
            assertEquals(Units.METRE,  parameter.getUnit());
            assertEquals(i,            parameter.doubleValue(Units.METRE));
            assertEquals(100*i,        parameter.doubleValue(Units.CENTIMETRE));
        }

        InvalidParameterValueException exception;
        exception = assertThrows(InvalidParameterValueException.class, () -> parameter.setValue(3.0), "setValue(< min)");
        assertEquals("Length measure", exception.getParameterName());

        // Out of range only after unit conversion.
        exception = assertThrows(InvalidParameterValueException.class, () -> parameter.setValue(10.0, Units.KILOMETRE), "setValue(> max)");
        assertEquals("Length measure", exception.getParameterName());

        exception = assertThrows(InvalidParameterValueException.class, () -> parameter.setValue("12"), "setValue(Sring)");
        assertEquals("Length measure", exception.getParameterName());

        for (int i=400; i<=2000; i+=100) {
            final double metres = i / 100.0;
            parameter.setValue(i, Units.CENTIMETRE);
            parameter.assertValueEquals(Double.valueOf(i), Double.valueOf(metres));
            assertEquals(Units.CENTIMETRE, parameter.getUnit());
            assertEquals(metres, parameter.doubleValue(Units.METRE), EPS);
        }
    }

    /**
     * Tests a parameter for values of type {@code double[]}.
     */
    @Test
    public void testArray() {
        double[] values = {5, 10, 15};
        final Watcher<double[]> parameter = new Watcher<>(
                DefaultParameterDescriptorTest.createForArray("myValues", 4, 4000, Units.METRE));
        parameter.setValue(values);
        assertArrayEquals(values, parameter.getValue());
        assertArrayEquals(values, parameter.convertedValue);
        assertArrayEquals(values, parameter.doubleValueList());
        assertArrayEquals(new double[] {500, 1000, 1500}, parameter.doubleValueList(Units.CENTIMETRE));
        /*
         * New values in kilometres.
         */
        values = new double[] {3, 2, 4};
        final double[] metres = new double[] {3000, 2000, 4000};
        parameter.setValue(values, Units.KILOMETRE);
        assertArrayEquals(values, parameter.getValue());
        assertArrayEquals(metres, parameter.convertedValue);
        assertArrayEquals(values, parameter.doubleValueList());
        assertArrayEquals(metres, parameter.doubleValueList(Units.METRE));

        // Values out of range.
        InvalidParameterValueException exception;
        exception = assertThrows(InvalidParameterValueException.class, () -> parameter.setValue(new double[] {5, 10, -5}, Units.METRE));
        assertMessageContains(exception, "myValues[2]");

        // Values out of range only after unit conversion.
        exception = assertThrows(InvalidParameterValueException.class, () -> parameter.setValue(new double[] {4, 5}, Units.KILOMETRE));
        assertMessageContains(exception, "myValues[1]");
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

        assertEquals     ("Direction",         descriptor.getName().getCode());
        assertEquals     (AxisDirection.NORTH, descriptor.getDefaultValue());
        assertEquals     (AxisDirection.NORTH, parameter .getValue());
        assertNull       (                     descriptor.getUnit());
        assertNull       (                     parameter .getUnit());
        assertNull       (                     descriptor.getMinimumValue());
        assertNull       (                     descriptor.getMaximumValue());
        assertArrayEquals(directions,          descriptor.getValidValues().toArray());
        /*
         * Invalid operation: attempt to get the value as a `double` is not allowed.
         */
        InvalidParameterTypeException ipt;
        ipt = assertThrows(InvalidParameterTypeException.class, () -> parameter.doubleValue(),
                           "doubleValue() shall not be allowed on AxisDirection");
        assertMessageContains(ipt, "AxisDirection");
        assertEquals("Direction", ipt.getParameterName());
        /*
         * Set a valid value.
         */
        parameter.setValue(AxisDirection.PAST);
        assertEquals(AxisDirection.PAST, parameter.getValue());
        /*
         * Invalid operation: set a value of valid type but not in the list of valid values.
         */
        InvalidParameterValueException exception;
        exception = assertThrows(InvalidParameterValueException.class, () -> parameter.setValue(AxisDirection.GEOCENTRIC_X));
        assertMessageContains(exception, "Direction", "Geocentric X");
        assertEquals("Direction", exception.getParameterName());
        /*
         * Invalid operation: attempt to set a value of wrong type.
         */
        exception = assertThrows(InvalidParameterValueException.class, () -> parameter.setValue(DateType.PUBLICATION));
        assertMessageContains(exception, "Direction", "DateType", "AxisDirection");
        assertEquals("Direction", exception.getParameterName());
    }

    /**
     * Tests the creation of many parameters for integer and floating point values.
     * Some on those values are cached (e.g. 0, 90, 360) because frequently used.
     * It should be transparent to the user.
     * Tests also unit conversions (degrees to radians in this case).
     */
    @Test
    public void testMany() {
        DefaultParameterValue<? extends Number> p;
        ParameterDescriptor<? extends Number> d;
        for (int i=-500; i<=500; i++) {
            p = createOptional("Unitlesss integer value", i);
            d = p.getDescriptor();
            validate(p);

            assertNotNull   (d);
            assertNull      (d.getDefaultValue());
            assertNull      (d.getMinimumValue());
            assertNull      (d.getMaximumValue());
            assertNull      (d.getValidValues());
            assertEquals    (Integer.class, d.getValueClass());
            assertInstanceOf(Integer.class, p.getValue());
            assertNull      (p.getUnit());
            assertEquals    (i, p.intValue());
            assertEquals    (i, p.doubleValue());

            p = create("Unitlesss double value", i, null);
            d = p.getDescriptor();
            validate(p);

            assertNotNull   (d);
            assertNull      (d.getDefaultValue());
            assertNull      (d.getMinimumValue());
            assertNull      (d.getMaximumValue());
            assertNull      (d.getValidValues());
            assertEquals    (Double.class, d.getValueClass());
            assertInstanceOf(Double.class, p.getValue());
            assertNull      (p.getUnit());
            assertEquals    (i, p.intValue());
            assertEquals    (i, p.doubleValue());

            p = create("Dimensionless double value", i, Units.UNITY);
            d = p.getDescriptor();
            validate(p);

            assertNotNull   (d);
            assertNull      (d.getDefaultValue());
            assertNull      (d.getMinimumValue());
            assertNull      (d.getMaximumValue());
            assertNull      (d.getValidValues());
            assertEquals    (Double.class, d.getValueClass());
            assertInstanceOf(Double.class, p.getValue());
            assertEquals    (Units.UNITY, p.getUnit());
            assertEquals    (i, p.intValue());
            assertEquals    (i, p.doubleValue());

            p = create("Angular double value", i, Units.DEGREE);
            d = p.getDescriptor();
            validate(p);

            assertNotNull   (d);
            assertNull      (d.getDefaultValue());
            assertNull      (d.getMinimumValue());
            assertNull      (d.getMaximumValue());
            assertNull      (d.getValidValues());
            assertEquals    (Double.class, d.getValueClass());
            assertInstanceOf(Double.class, p.getValue());
            assertEquals    (Units.DEGREE, p.getUnit());
            assertEquals    (i, p.intValue());
            assertEquals    (i, p.doubleValue());
            assertEquals    (toRadians(i), p.doubleValue(Units.RADIAN), EPS, "Require unit conversion.");
        }
    }

    /**
     * Tests clone.
     */
    @Test
    public void testClone() {
        DefaultParameterValue<Double> parameter = create("Clone test", 3, Units.METRE);
        assertEquals(parameter, parameter.clone());
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialization() {
        DefaultParameterValue<Double> parameter = create("Serialization test", 3, Units.METRE);
        assertNotSame(parameter, assertSerializedEquals(parameter));
    }

    /**
     * Tests WKT formatting.
     */
    @Test
    public void testWKT() {
        final DefaultParameterValue<Integer> count  = createOptional("Count", 4);
        final DefaultParameterValue<Double>  length = create("Length", 30, Units.CENTIMETRE);
        assertWktEquals(Convention.WKT1, "PARAMETER[“Count”, 4]", count);
        assertWktEquals(Convention.WKT1, "PARAMETER[“Length”, 30.0]", length);
        assertWktEquals(Convention.WKT2, "PARAMETER[“Count”, 4]", count);
        assertWktEquals(Convention.WKT2, "PARAMETER[“Length”, 30.0, LENGTHUNIT[“centimetre”, 0.01]]", length);
    }

    /**
     * Tests WKT formatting of a parameter with sexagesimal units.
     * Since those units cannot be formatted in a {@code UNIT["name", scale]} element,
     * the formatter should convert them to a formattable unit like degrees.
     */
    @Test
    public void testWKT_withUnformattableUnit() {
        final Unit<?> degreesAndMinutes = Units.valueOfEPSG(9111);
        DefaultParameterValue<Double> p = create("Angle", 10.3, degreesAndMinutes);
        assertWktEquals(Convention.WKT1,     "PARAMETER[“Angle”, 10.3]", p);  // 10.3 DM  ==  10.5°
        assertWktEquals(Convention.WKT2,     "PARAMETER[“Angle”, 10.5, ANGLEUNIT[“degree”, 0.017453292519943295]]", p);
        assertWktEquals(Convention.INTERNAL, "Parameter[“Angle”, 10.3, Unit[“D.M”, 0.017453292519943295, Id[“EPSG”, 9111]]]", p);
        // In above line, the parameter value in `INTERNAL` mode was formatted in the same unit as the descriptor.

        p = create("Angle", 0, Units.DEGREE);
        p.setValue(10.3, degreesAndMinutes);  // Cannot be formatted in WKT1.
        assertWktEquals(Convention.WKT2,     "PARAMETER[“Angle”, 10.5, ANGLEUNIT[“degree”, 0.017453292519943295]]", p);
        assertWktEquals(Convention.INTERNAL, "Parameter[“Angle”, 10.3, Unit[“D.M”, 0.017453292519943295, Id[“EPSG”, 9111]]]", p);
    }

    /**
     * Tests WKT formatting of a parameter having an identifier.
     */
    @Test
    public void testIdentifiedParameterWKT() {
        final Watcher<Double> parameter = new Watcher<>(DefaultParameterDescriptorTest.createEPSG("A0", Constants.EPSG_A0));
        assertWktEquals(Convention.WKT2, "PARAMETER[“A0”, null, ID[“EPSG”, 8623]]", parameter);
    }
}
