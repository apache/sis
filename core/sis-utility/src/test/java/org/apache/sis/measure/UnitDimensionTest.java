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

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import javax.measure.Unit;
import javax.measure.Dimension;
import org.apache.sis.math.Fraction;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.util.UnconvertibleObjectException;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link UnitDimension} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public final strictfp class UnitDimensionTest extends TestCase {
    /**
     * The dimension declared by the base {@link Units} constant.
     * We should not create our own instance for avoiding to pollute the {@link UnitDimension} cache.
     */
    static final Dimension LENGTH        = Units.METRE            .getDimension(),
                           TIME          = Units.SECOND           .getDimension(),
                           MASS          = Units.KILOGRAM         .getDimension(),
                           FORCE         = Units.NEWTON           .getDimension(),
                           SPEED         = Units.METRES_PER_SECOND.getDimension(),
                           AREA          = Units.SQUARE_METRE     .getDimension(),
                           VOLUME        = Units.CUBIC_METRE      .getDimension(),
                           DIMENSIONLESS = Units.UNITY            .getDimension();

    /**
     * Tests {@link UnitDimension#multiply(Dimension)}.
     */
    @Test
    @DependsOnMethod("testEqualsAndHashCode")
    public void testMultiply() {
        assertSame(LENGTH, LENGTH.multiply(DIMENSIONLESS));
        assertSame(AREA,   LENGTH.multiply(LENGTH));
        assertSame(VOLUME, LENGTH.multiply(AREA));
        assertSame(VOLUME, AREA  .multiply(LENGTH));

        final Map<Dimension,Integer> expected = new HashMap<>(4);
        assertNull(expected.put(LENGTH, 1));
        assertNull(expected.put(TIME,   1));
        assertMapEquals(expected, LENGTH.multiply(TIME).getBaseDimensions());
    }

    /**
     * Tests {@link UnitDimension#divide(Dimension)}.
     */
    @Test
    @DependsOnMethod("testEqualsAndHashCode")
    public void testDivide() {
        assertSame(LENGTH, LENGTH.divide(DIMENSIONLESS));
        assertSame(LENGTH, AREA  .divide(LENGTH));
        assertSame(LENGTH, VOLUME.divide(AREA));
        assertSame(AREA,   VOLUME.divide(LENGTH));
        assertSame(SPEED,  LENGTH.divide(TIME));
    }

    /**
     * Tests {@link UnitDimension#pow(int)}.
     */
    @Test
    @DependsOnMethod("testEqualsAndHashCode")
    public void testPow() {
        assertSame("DIMENSIONLESS", DIMENSIONLESS, DIMENSIONLESS.pow(4));
        assertSame("AREA",          AREA,          LENGTH.pow(2));
        assertSame("VOLUME",        VOLUME,        LENGTH.pow(3));
    }

    /**
     * Tests {@link UnitDimension#root(int)}.
     */
    @Test
    @DependsOnMethod("testEqualsAndHashCode")
    public void testRoot() {
        assertSame("DIMENSIONLESS", DIMENSIONLESS, DIMENSIONLESS.root(4));
        assertSame("AREA",          LENGTH,        AREA.root(2));
        assertSame("VOLUME",        LENGTH,        VOLUME.root(3));
    }

    /**
     * Returns the specific detectivity dimension, which is T^2.5 / (M⋅L).
     */
    static Dimension specificDetectivity() {
        return TIME.pow(2).divide(MASS.multiply(LENGTH)).multiply(TIME.root(2));
    }

    /**
     * Tests a dimension with rational power. This tests use the specific detectivity, which dimension is T^2.5 / (M⋅L).
     */
    @Test
    @DependsOnMethod({"testMultiply", "testDivide", "testPow", "testRoot"})
    public void testRationalPower() {
        final Dimension dim = specificDetectivity();
        final Map<Dimension,Fraction> expected = new HashMap<>(4);
        assertNull(expected.put(TIME,   new Fraction( 5, 2)));
        assertNull(expected.put(MASS,   new Fraction(-1, 1)));
        assertNull(expected.put(LENGTH, new Fraction(-1, 1)));
        assertMapEquals(expected, ((UnitDimension) dim).components);
        try {
            dim.getBaseDimensions().toString();
            fail("Mapping from Fraction to Integer should not be allowed.");
        } catch (UnconvertibleObjectException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("Integer"));
        }
        // 'toString()' formatting tested in UnitFormatTest.testRationalPower().
    }

    /**
     * Tests {@link UnitDimension#getBaseDimensions()}. This method indirectly tests the results
     * of {@link UnitDimension#multiply(Dimension)}, {@link UnitDimension#divide(Dimension)} and
     * {@link UnitDimension#pow(int)} since this test uses constants that were created with above
     * operations.
     */
    @Test
    public void testGetBaseDimensions() {
        assertNull("LENGTH",        LENGTH       .getBaseDimensions());     // Null value as per JSR-363 specification.
        assertNull("TIME",          TIME         .getBaseDimensions());
        assertTrue("DIMENSIONLESS", DIMENSIONLESS.getBaseDimensions().isEmpty());
        assertMapEquals(Collections.singletonMap(LENGTH, 3), VOLUME.getBaseDimensions());

        final Map<Dimension,Integer> expected = new HashMap<>(4);
        assertNull(expected.put(MASS,    1));
        assertNull(expected.put(LENGTH,  1));
        assertNull(expected.put(TIME,   -2));
        assertMapEquals(expected, FORCE.getBaseDimensions());
    }

    /**
     * Tests the {@link UnitDimension#equals(Object)} and {@link UnitDimension#hashCode()} methods.
     */
    @Test
    public void testEqualsAndHashCode() {
        verifyEqualsAndHashCode("Base dimensions",    true,  Units.METRE,  Units.METRE);
        verifyEqualsAndHashCode("Base dimensions",    true,  Units.SECOND, Units.SECOND);
        verifyEqualsAndHashCode("Base dimensions",    false, Units.METRE,  Units.SECOND);
        verifyEqualsAndHashCode("Derived dimensions", true,  Units.NEWTON, Units.NEWTON);
        verifyEqualsAndHashCode("Derived dimensions", true,  Units.JOULE,  Units.JOULE);
        verifyEqualsAndHashCode("Derived dimensions", false, Units.NEWTON, Units.JOULE);
        verifyEqualsAndHashCode("Dimensionsless",     true,  Units.UNITY,  Units.UNITY);
        verifyEqualsAndHashCode("Dimensionsless",     true,  Units.DEGREE, Units.DEGREE);
        verifyEqualsAndHashCode("Dimensionsless",     true,  Units.UNITY,  Units.DEGREE);    // Really true (not false) as per JSR-363 specification.
        verifyEqualsAndHashCode("Mixed types",        false, Units.METRE,  Units.UNITY);
        verifyEqualsAndHashCode("Mixed types",        false, Units.METRE,  Units.NEWTON);
    }

    /**
     * Verifies that the test for equality between two dimensions produce the expected result.
     * This method expects {@link Unit} instances instead than {@link Dimension} for convenience,
     * but only the dimensions will be compared.
     *
     * @param message   the message to show in case of failure.
     * @param expected  the expected result of {@link UnitDimension#equals(Object)}.
     * @param a         {@link Units} constant from which to get the first dimension to compare.
     * @param b         {@link Units} constant from which to get the first dimension to compare.
     */
    private static void verifyEqualsAndHashCode(final String message, final boolean expected, final Unit<?> a, final Unit<?> b) {
        final Dimension da = a.getDimension();
        final Dimension db = b.getDimension();
        assertEquals(message, expected, da.equals(db));
        assertEquals(message, expected, db.equals(da));
        assertEquals(message, expected, da.hashCode() == db.hashCode());
    }

    /**
     * Serializes some dimensions, deserializes them and verifies that we get the same instance.
     */
    @Test
    @DependsOnMethod("testEqualsAndHashCode")
    public void testSerialization() {
        assertSame(LENGTH,        assertSerializedEquals(LENGTH));
        assertSame(TIME,          assertSerializedEquals(TIME));
        assertSame(FORCE,         assertSerializedEquals(FORCE));
        assertSame(DIMENSIONLESS, assertSerializedEquals(DIMENSIONLESS));
    }
}
