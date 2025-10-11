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

import javax.measure.Unit;
import javax.measure.Quantity;
import javax.measure.quantity.Length;
import javax.measure.quantity.Temperature;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link Quantities}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class QuantitiesTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public QuantitiesTest() {
    }

    /**
     * Tests {@link Quantities#create(double, String)}.
     */
    @Test
    public void testCreate() {
        final Quantity<?> q = Quantities.create(5, "km");
        assertEquals(5, q.getValue().doubleValue());
        assertSame  (Units.KILOMETRE, q.getUnit());
    }

    /**
     * Tests {@link Quantities#castOrCopy(Quantity)}.
     */
    @Test
    public void testCastOrCopy() {
        Quantity<Length> q = Quantities.create(5, Units.KILOMETRE);
        assertSame(q, Quantities.castOrCopy(q));

        q = new Quantity<Length>() {
            @Override public Number           getValue()                         {return 8;}
            @Override public Unit<Length>     getUnit ()                         {return Units.CENTIMETRE;}
            @Override public Scale            getScale()                         {return Scale.ABSOLUTE;}
            @Override public Quantity<Length> add     (Quantity<Length> ignored) {return null;}
            @Override public Quantity<Length> subtract(Quantity<Length> ignored) {return null;}
            @Override public Quantity<?>      multiply(Quantity<?>      ignored) {return null;}
            @Override public Quantity<?>      divide  (Quantity<?>      ignored) {return null;}
            @Override public Quantity<Length> multiply(Number           ignored) {return null;}
            @Override public Quantity<Length> divide  (Number           ignored) {return null;}
            @Override public Quantity<?>      inverse ()                         {return null;}
            @Override public Quantity<Length> negate  ()                         {return null;}
            @Override public Quantity<Length> to      (Unit<Length>     ignored) {return null;}
            @Override public boolean    isEquivalentTo(Quantity<Length> ignored) {return false;}
            @Override public <T extends Quantity<T>> Quantity<T> asType(Class<T> ignored) {return null;}
        };
        final Length c = Quantities.castOrCopy(q);
        assertNotSame(q, c);
        assertEquals(8, c.getValue().doubleValue());
        assertSame(Units.CENTIMETRE, c.getUnit());
    }

    /**
     * Tests operations on temperature. The values shall be converted to Kelvin before any operation.
     * This produces counter-intuitive result, but is the only way to get results that are consistent
     * with arithmetic rules like commutativity and associativity.
     */
    @Test
    public void testTemperature() {
        final Quantity<Temperature> q1 = Quantities.create( 2, Units.CELSIUS);
        final Quantity<Temperature> q2 = Quantities.create( 3, Units.KELVIN);
        final Quantity<Temperature> q3 = Quantities.create(-8, Units.CELSIUS);

        assertInstanceOf(DerivedScalar.TemperatureMeasurement.class, q1,  "2°C");
        assertInstanceOf(DerivedScalar.Temperature.class,            q2,  "3 K");
        assertInstanceOf(DerivedScalar.TemperatureMeasurement.class, q3, "-8°C");

        Quantity<Temperature> r = q1.add(q2);
        assertSame(Units.CELSIUS, r.getUnit());
        assertEquals(5, r.getValue().doubleValue(), 1E-13);

        r = q2.add(q1);
        assertSame(Units.KELVIN, r.getUnit());
        assertEquals(278.15, r.getValue().doubleValue(), 1E-13);

        r = q1.add(q3);
        assertSame(Units.CELSIUS, r.getUnit());
        assertEquals(267.15, r.getValue().doubleValue(), 1E-13);

        r = q1.multiply(3);
        assertSame(Units.CELSIUS, r.getUnit());
        assertEquals(552.3, r.getValue().doubleValue(), 1E-13);

        r = q1.multiply(1);
        assertSame(q1, r);
    }

    /**
     * Tests a multiply operation that result in a quantity for which we have no specialized sub-interface.
     */
    @Test
    public void testUnspecialized() {
        final Quantity<?> quantity = Quantities.create(3, Units.CENTIMETRE).multiply(Quantities.create(4, Units.SECOND));
        assertEquals(12, quantity.getValue().doubleValue());
        assertEquals("cm⋅s", quantity.getUnit().toString());
    }

    /**
     * Tests {@link Scalar#equals(Object)} and {@link Scalar#hashCode()}.
     * This test uses a unit without specific {@link Scalar} subclass, in order to
     * verify that tested methods work even though the {@link ScalarFallback} proxy.
     */
    @Test
    public void testEqualsAndHashcode() {
        Quantity<?> q1 = Quantities.create(2, Units.VOLT);
        Quantity<?> q2 = Quantities.create(2, Units.VOLT);
        Quantity<?> q3 = Quantities.create(3, Units.VOLT);
        assertEquals   (q1.hashCode(), q2.hashCode());
        assertNotEquals(q1.hashCode(), q3.hashCode());
        assertNotEquals(0, q1.hashCode());
        assertTrue (q1.equals(q2));
        assertFalse(q1.equals(q3));
    }

    /**
     * Tests {@link Quantities#min(Quantity, Quantity)} and {@link Quantities#max(Quantity, Quantity)}.
     */
    @Test
    public void testMinAndMax() {
        Quantity<Length> q1 = Quantities.create(5,      Units.KILOMETRE);
        Quantity<Length> q2 = Quantities.create(600,    Units.METRE);
        Quantity<Length> q3 = Quantities.create(700000, Units.CENTIMETRE);
        assertSame(q2, Quantities.min(q1, q2));
        assertSame(q1, Quantities.max(q1, q2));
        assertSame(q1, Quantities.min(q1, q3));
        assertSame(q3, Quantities.max(q1, q3));
    }
}
