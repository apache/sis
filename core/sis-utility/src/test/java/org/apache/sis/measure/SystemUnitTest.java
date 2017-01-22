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
package org.apache.sis.measure;

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.lang.reflect.Field;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.UnconvertibleException;
import javax.measure.IncommensurableException;
import javax.measure.quantity.Length;
import javax.measure.quantity.Time;
import javax.measure.Quantity;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link SystemUnit} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
@DependsOn(UnitDimensionTest.class)
public final strictfp class SystemUnitTest extends TestCase {
    /**
     * Verifies the {@link SystemUnit#related} array content of all system units declared in {@link Units}.
     * This tests verify that the array has been fully populated and that the converter of all units are
     * instance of {@link LinearConverter}.
     *
     * @throws ReflectiveOperationException if an error occurred while iterating over the field values.
     *
     * @see ConventionalUnit#create(SystemUnit, UnitConverter)
     */
    @Test
    public void verifyRelatedUnits() throws ReflectiveOperationException {
        for (final Field f : Units.class.getFields()) {
            final Object value = f.get(null);
            if (value instanceof SystemUnit<?>) {
                final ConventionalUnit<?>[] related = ((SystemUnit<?>) value).related();
                if (related != null) {
                    final String symbol = ((SystemUnit<?>) value).getSymbol();
                    for (final ConventionalUnit<?> r : related) {
                        assertNotNull(symbol, r);
                        assertInstanceOf(symbol, LinearConverter.class, r.toTarget);
                    }
                }
            }
        }
    }

    /**
     * Tests {@link SystemUnit#multiply(Unit)}.
     */
    @Test
    @DependsOnMethod("testEqualsAndHashCode")
    public void testMultiply() {
        assertSame(Units.METRE,        Units.METRE.multiply(Units.UNITY));
        assertSame(Units.SQUARE_METRE, Units.METRE.multiply(Units.METRE));
        assertSame(Units.CUBIC_METRE,  Units.METRE.multiply(Units.SQUARE_METRE));
        assertSame(Units.CUBIC_METRE,  Units.SQUARE_METRE.multiply(Units.METRE));

        final Map<Unit<?>,Integer> expected = new HashMap<>(4);
        assertNull(expected.put(Units.METRE,  1));
        assertNull(expected.put(Units.SECOND, 1));
        assertMapEquals(expected, Units.METRE.multiply(Units.SECOND).getBaseUnits());
    }

    /**
     * Tests {@link SystemUnit#divide(Unit)}.
     */
    @Test
    @DependsOnMethod("testEqualsAndHashCode")
    public void testDivide() {
        assertSame(Units.METRE, Units.METRE.divide(Units.UNITY));
        assertSame(Units.METRE, Units.SQUARE_METRE.divide(Units.METRE));
        assertSame(Units.METRE, Units.CUBIC_METRE.divide(Units.SQUARE_METRE));
        assertSame(Units.SQUARE_METRE, Units.CUBIC_METRE.divide(Units.METRE));
        assertSame(Units.METRES_PER_SECOND, Units.METRE.divide(Units.SECOND));
    }

    /**
     * Tests {@link SystemUnit#pow(int)}.
     */
    @Test
    @DependsOnMethod("testEqualsAndHashCode")
    public void testPow() {
        assertSame("UNITY",        Units.UNITY,        Units.UNITY.pow(4));
        assertSame("SQUARE_METRE", Units.SQUARE_METRE, Units.METRE.pow(2));
        assertSame("CUBIC_METRE",  Units.CUBIC_METRE,  Units.METRE.pow(3));
    }

    /**
     * Tests {@link SystemUnit#root(int)}.
     */
    @Test
    @DependsOnMethod("testEqualsAndHashCode")
    public void testRoot() {
        assertSame("UNITY",        Units.UNITY, Units.UNITY.root(4));
        assertSame("SQUARE_METRE", Units.METRE, Units.SQUARE_METRE.root(2));
        assertSame("CUBIC_METRE",  Units.METRE, Units.CUBIC_METRE.root(3));
    }

    /**
     * Tests {@link SystemUnit#getBaseUnits()}. This method indirectly tests the results of
     * {@link SystemUnit#multiply(Unit)}, {@link SystemUnit#divide(Unit)} and {@link SystemUnit#pow(int)}
     * since this test uses constants that were created with above operations.
     */
    @Test
    public void testGetBaseDimensions() {
        assertNull("METRE",  Units.METRE .getBaseUnits());      // Null value as per JSR-363 specification.
        assertNull("SECOND", Units.SECOND.getBaseUnits());
        assertTrue("UNITY",  Units.UNITY .getBaseUnits().isEmpty());

        assertMapEquals(Collections.singletonMap(Units.METRE, 3), Units.CUBIC_METRE.getBaseUnits());

        final Map<Unit<?>,Integer> expected = new HashMap<>(4);
        assertNull(expected.put(Units.KILOGRAM, 1));
        assertNull(expected.put(Units.METRE,    1));
        assertNull(expected.put(Units.SECOND,  -2));
        assertMapEquals(expected, Units.NEWTON.getBaseUnits());
    }

    /**
     * Tests the {@link SystemUnit#equals(Object)} and {@link SystemUnit#hashCode()} methods.
     */
    @Test
    public void testEqualsAndHashCode() {
        verifyEqualsAndHashCode("Base units",     true,  Units.METRE,  Units.METRE);
        verifyEqualsAndHashCode("Base units",     true,  Units.SECOND, Units.SECOND);
        verifyEqualsAndHashCode("Base units",     false, Units.METRE,  Units.SECOND);
        verifyEqualsAndHashCode("Derived units",  true,  Units.NEWTON, Units.NEWTON);
        verifyEqualsAndHashCode("Derived units",  true,  Units.JOULE,  Units.JOULE);
        verifyEqualsAndHashCode("Derived units",  false, Units.NEWTON, Units.JOULE);
        verifyEqualsAndHashCode("Dimensionsless", true,  Units.UNITY,  Units.UNITY);
        verifyEqualsAndHashCode("Dimensionsless", true,  Units.DEGREE, Units.DEGREE);
        verifyEqualsAndHashCode("Dimensionsless", false, Units.UNITY,  Units.DEGREE);
        verifyEqualsAndHashCode("Mixed types",    false, Units.METRE,  Units.UNITY);
        verifyEqualsAndHashCode("Mixed types",    false, Units.METRE,  Units.NEWTON);
    }

    /**
     * Verifies that the test for equality between two units produce the expected result.
     * This method expects {@link Unit} instances instead than {@link Unit} for convenience,
     * but only the units will be compared.
     *
     * @param message   the message to show in case of failure.
     * @param expected  the expected result of {@link SystemUnit#equals(Object)}.
     * @param a         {@link Units} constant from which to get the first dimension to compare.
     * @param b         {@link Units} constant from which to get the first dimension to compare.
     */
    private static void verifyEqualsAndHashCode(final String message, final boolean expected, final Unit<?> a, final Unit<?> b) {
        assertEquals(message, expected, a.equals(b));
        assertEquals(message, expected, b.equals(a));
        assertEquals(message, expected, a.hashCode() == b.hashCode());
    }

    /**
     * Serializes some units, deserializes them and verifies that we get the same instance.
     */
    @Test
    @DependsOnMethod("testEqualsAndHashCode")
    public void testSerialization() {
        assertSame(Units.METRE,  assertSerializedEquals(Units.METRE));
        assertSame(Units.SECOND, assertSerializedEquals(Units.SECOND));
        assertSame(Units.NEWTON, assertSerializedEquals(Units.NEWTON));
        assertSame(Units.UNITY,  assertSerializedEquals(Units.UNITY));
    }

    /**
     * Tests {@link SystemUnit#isCompatible(Unit)}.
     */
    @Test
    public void testIsCompatible() {
        assertTrue (Units.METRE .isCompatible(Units.METRE ));
        assertTrue (Units.SECOND.isCompatible(Units.SECOND));
        assertTrue (Units.RADIAN.isCompatible(Units.RADIAN));
        assertFalse(Units.RADIAN.isCompatible(Units.METRE ));
        assertFalse(Units.METRE .isCompatible(Units.RADIAN));
        assertTrue (Units.UNITY .isCompatible(Units.RADIAN));   // Really true (not false) as per JSR-363 specification.
        assertTrue (Units.RADIAN.isCompatible(Units.UNITY ));
    }

    /**
     * Tests {@link SystemUnit#getConverterTo(Unit)}.
     */
    @Test
    @DependsOnMethod("testIsCompatible")
    public void testGetConverterTo() {
        assertTrue(Units.METRE .getConverterTo(Units.METRE ).isIdentity());
        assertTrue(Units.SECOND.getConverterTo(Units.SECOND).isIdentity());
        assertTrue(Units.RADIAN.getConverterTo(Units.RADIAN).isIdentity());
    }

    /**
     * Tests {@link SystemUnit#getConverterTo(Unit)} with illegal arguments.
     * Those calls are not allowed by the Java compiler if parameterized types have not been erased.
     * But if the users nevertheless erased parameter types, {@link SystemUnit} implementation should
     * have some safety nets.
     */
    @Test
    @SuppressWarnings("unchecked")
    @DependsOnMethod("testGetConverterTo")
    public void testIllegalGetConverterTo() {
        try {
            Units.METRE.getConverterTo((Unit) Units.SECOND);
            fail("Conversion should not have been allowed.");
        } catch (UnconvertibleException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("m"));     // metre unit symbol
            assertTrue(message, message.contains("s"));     // second unit symbol
        }
        /*
         * Following is tricker because "radian" and "unity" are compatible units,
         * but should nevertheless not be allowed by the 'getConverterTo' method.
         */
        try {
            Units.RADIAN.getConverterTo((Unit) Units.UNITY);
            fail("Conversion should not have been allowed.");
        } catch (UnconvertibleException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("rad"));
        }
    }

    /**
     * Tests {@link SystemUnit#getConverterToAny(Unit)}.
     *
     * @throws IncommensurableException if two units were not expected to be considered incompatible.
     */
    @Test
    @DependsOnMethod("testGetConverterTo")
    public void testGetConverterToAny() throws IncommensurableException {
        assertTrue(Units.METRE .getConverterToAny(Units.METRE ).isIdentity());
        assertTrue(Units.SECOND.getConverterToAny(Units.SECOND).isIdentity());
        assertTrue(Units.RADIAN.getConverterToAny(Units.RADIAN).isIdentity());
        assertTrue(Units.RADIAN.getConverterToAny(Units.UNITY ).isIdentity());
        assertTrue(Units.UNITY .getConverterToAny(Units.RADIAN).isIdentity());
        try {
            Units.METRE.getConverterToAny(Units.SECOND);
            fail("Conversion should not have been allowed.");
        } catch (IncommensurableException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("m"));     // metre unit symbol
            assertTrue(message, message.contains("s"));     // second unit symbol
        }
    }

    /**
     * Tests {@link SystemUnit#alternate(String)}.
     */
    @Test
    public void testAlternate() {
        assertSame(Units.RADIAN, Units.RADIAN.alternate("rad"));
        assertSame(Units.PIXEL,  Units.PIXEL .alternate("px"));
        assertSame(Units.PIXEL , Units.UNITY .alternate("px"));
        try {
            Units.UNITY.alternate("rad");
            fail("Should not accept since “rad” is already used for a unit of another quantity type (Angle).");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("rad"));
        }
        try {
            Units.RADIAN.alternate("°");
            fail("Should not accept since “°” is already used for a unit of another type (ConventionalUnit).");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("°"));
        }
    }

    /**
     * Tests {@link SystemUnit#asType(Class)}.
     */
    @Test
    public void testAsType() {
        assertSame(Units.METRE,  Units.METRE .asType(Length.class));
        assertSame(Units.SECOND, Units.SECOND.asType(Time.class));
        /*
         * Test with units outside the pre-defined constants in the Units class.
         */
        final Unit<Length> anonymous = new SystemUnit<>(Length.class, (UnitDimension) Units.METRE.getDimension(), null,  UnitRegistry.OTHER, (short) 0);
        final Unit<Length> otherName = new SystemUnit<>(Length.class, (UnitDimension) Units.METRE.getDimension(), "Foo", UnitRegistry.OTHER, (short) 0);
        assertSame(Units.METRE, anonymous.asType(Length.class));
        assertSame(otherName,   otherName.asType(Length.class));
        /*
         * Verify that the unit can not be casted to an incompatible units.
         */
        for (final Unit<Length> unit : Arrays.asList(Units.METRE, anonymous, otherName)) {
            try {
                unit.asType(Time.class);
                fail("Expected an exception for incompatible quantity types.");
            } catch (ClassCastException e) {
                final String message = e.getMessage();
                assertTrue(message, message.contains("Length"));
                assertTrue(message, message.contains("Time"));
            }
        }
    }

    /**
     * Tests {@link SystemUnit#asType(Class)} for a quantity unknown to Apache SIS.
     */
    @Test
    @DependsOnMethod({"testAsType", "testAlternate"})
    public void testAsTypeForNewQuantity() {
        /*
         * Tests with a new quantity type unknown to Apache SIS.
         * SIS can not proof that the type is wrong, so it should accept it.
         */
        final Unit<Strange> strange = Units.METRE.asType(Strange.class);
        final Unit<Strange> named   = strange.alternate("strange");
        assertNull  ("Should not have symbol since this is a unit for a new quantity.", strange.getSymbol());
        assertEquals("Should have a name since we invoked 'alternate'.", "strange", named.getSymbol());
        assertSame  ("Should prefer the named instance.", named, Units.METRE.asType(Strange.class));
        assertSame  ("Go back to the fundamental unit.",  Units.METRE, named.asType(Length.class));
        for (final Unit<Strange> unit : Arrays.asList(strange, named)) {
            try {
                unit.asType(Time.class);
                fail("Expected an exception for incompatible quantity types.");
            } catch (ClassCastException e) {
                final String message = e.getMessage();
                assertTrue(message, message.contains("Strange"));
                assertTrue(message, message.contains("Time"));
            }
        }
    }

    /**
     * A dummy quantity type for tests using a quantity type unknown to Apache SIS.
     */
    private static interface Strange extends Quantity<Strange> {
    }
}
