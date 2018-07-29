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
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.opengis.test.Assert.*;


/**
 * Tests {@link Quantities}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.8
 * @module
 */
@DependsOn(ScalarTest.class)
public final strictfp class QuantitiesTest extends TestCase {
    /**
     * Tests {@link Quantities#create(double, String)}.
     */
    @Test
    public void testCreate() {
        final Quantity<?> q = Quantities.create(5, "km");
        assertEquals("value", 5, q.getValue().doubleValue(), STRICT);
        assertSame  ("unit", Units.KILOMETRE, q.getUnit());
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
            @Override public Quantity<Length> add     (Quantity<Length> ignored) {return null;}
            @Override public Quantity<Length> subtract(Quantity<Length> ignored) {return null;}
            @Override public Quantity<?>      multiply(Quantity<?>      ignored) {return null;}
            @Override public Quantity<?>      divide  (Quantity<?>      ignored) {return null;}
            @Override public Quantity<Length> multiply(Number           ignored) {return null;}
            @Override public Quantity<Length> divide  (Number           ignored) {return null;}
            @Override public Quantity<?>      inverse ()                         {return null;}
            @Override public Quantity<Length> to      (Unit<Length>     ignored) {return null;}
            @Override public <T extends Quantity<T>> Quantity<T> asType(Class<T> ignored) {return null;}
        };
        final Length c = Quantities.castOrCopy(q);
        assertNotSame(q, c);
        assertEquals("value", 8, c.getValue().doubleValue(), STRICT);
        assertSame  ("unit", Units.CENTIMETRE, c.getUnit());
    }

    /**
     * Tests operations on temperature. The values shall be converted to Kelvin before any operation.
     * This produces counter-intuitive result, but is the only way to get results that are consistent
     * with arithmetic rules like commutativity and associativity.
     *
     * @since 1.0
     */
    @Test
    public void testTemperature() {
        final Quantity<Temperature> q1 = Quantities.create( 2, Units.CELSIUS);
        final Quantity<Temperature> q2 = Quantities.create( 3, Units.KELVIN);
        final Quantity<Temperature> q3 = Quantities.create(-8, Units.CELSIUS);

        assertInstanceOf( "2°C", DerivedScalar.TemperatureMeasurement.class, q1);
        assertInstanceOf( "3 K", DerivedScalar.Temperature.class,            q2);
        assertInstanceOf("-8°C", DerivedScalar.TemperatureMeasurement.class, q3);

        Quantity<Temperature> r = q1.add(q2);
        assertSame  ("unit",  Units.CELSIUS, r.getUnit());
        assertEquals("value", 5, r.getValue().doubleValue(), 1E-13);

        r = q2.add(q1);
        assertSame  ("unit",  Units.KELVIN, r.getUnit());
        assertEquals("value", 278.15, r.getValue().doubleValue(), 1E-13);

        r = q1.add(q3);
        assertSame  ("unit",  Units.CELSIUS, r.getUnit());
        assertEquals("value", 267.15, r.getValue().doubleValue(), 1E-13);

        r = q1.multiply(3);
        assertSame  ("unit",  Units.CELSIUS, r.getUnit());
        assertEquals("value", 552.3, r.getValue().doubleValue(), 1E-13);

        r = q1.multiply(1);
        assertSame(q1, r);
    }
}
