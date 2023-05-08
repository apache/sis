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

import java.lang.reflect.Proxy;
import javax.measure.Quantity;
import javax.measure.quantity.Area;
import javax.measure.quantity.Frequency;
import javax.measure.quantity.Length;
import javax.measure.quantity.Speed;
import javax.measure.quantity.Time;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.opengis.test.Assert.assertInstanceOf;
import static org.apache.sis.test.Assertions.assertSerializedEquals;


/**
 * Tests {@link Scalar} and its subclasses.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   0.8
 */
@DependsOn(UnitsTest.class)
public final class ScalarTest extends TestCase {
    /**
     * Tests {@link Scalar#add(Quantity)} and {@link Scalar#subtract(Quantity)}.
     * Those tests depend on proper working of {@link Scalar#create(double, Unit)}.
     */
    @Test
    public void testAddSubtract() {
        final Quantity<Length> q0 = new Scalar.Length(0,  Units.MILLIMETRE);
        final Quantity<Length> q1 = new Scalar.Length(24, Units.METRE);
        final Quantity<Length> q2 = new Scalar.Length(3,  Units.KILOMETRE);
        assertSame(q1, q1.add(q0));
        assertSame(q2, q2.subtract(q0));

        final Quantity<Length> q3 = q1.add(q2);
        assertSame  ("unit",  Units.METRE, q3.getUnit());
        assertEquals("value", 3024, q3.getValue().doubleValue(), STRICT);

        final Quantity<Length> q4 = q2.subtract(q1);
        assertSame  ("unit",  Units.KILOMETRE, q4.getUnit());
        assertEquals("value", 2.976, q4.getValue().doubleValue(), STRICT);
    }

    /**
     * Tests {@link Scalar#multiply(Number)} and {@link Scalar#divide(Number)}.
     * Those tests depend on proper working of {@link Scalar#create(double, Unit)}.
     */
    @Test
    public void testMultiplyDivideNumber() {
        final Quantity<Length> q1 = new Scalar.Length(24, Units.KILOMETRE);
        assertSame(q1, q1.multiply(1));
        assertSame(q1, q1.divide  (1));

        final Quantity<Length> q2 = q1.multiply(2);
        assertSame  ("unit",  Units.KILOMETRE, q2.getUnit());
        assertEquals("value", 48, q2.getValue().doubleValue(), STRICT);

        final Quantity<Length> q3 = q1.divide(3);
        assertSame  ("unit",  Units.KILOMETRE, q3.getUnit());
        assertEquals("value", 8, q3.getValue().doubleValue(), STRICT);
    }

    /**
     * Tests {@link Scalar#multiply(Quantity)}, {@link Scalar#divide(Quantity)} and {@link Quantity#inverse()}.
     * Those tests depend on proper working of {@link Quantities#create(double, Unit)}, which depends in turn on
     * proper declarations of {@link ScalarFactory} in {@link Units} initialization.
     */
    @Test
    public void testMultiplyDivideQuantity() {
        final Quantity<Length> q1 = new Scalar.Length(24, Units.METRE);
        final Quantity<Time>   q2 = new Scalar.Time  ( 4, Units.SECOND);
        final Quantity<Speed>  q3 = q1.divide(q2).asType(Speed.class);
        assertSame  ("unit", Units.METRES_PER_SECOND, q3.getUnit());
        assertEquals("value", 6, q3.getValue().doubleValue(), STRICT);
        assertInstanceOf("Length/Time", Scalar.Speed.class, q3);

        final Quantity<Area> q4 = q1.multiply(q1).asType(Area.class);
        assertSame  ("unit", Units.SQUARE_METRE, q4.getUnit());
        assertEquals("value", 576, q4.getValue().doubleValue(), STRICT);
        assertInstanceOf("Length⋅Length", Scalar.Area.class, q4);

        final Quantity<Frequency> q5 = q2.inverse().asType(Frequency.class);
        assertSame  ("unit", Units.HERTZ, q5.getUnit());
        assertEquals("value", 0.25, q5.getValue().doubleValue(), STRICT);
        assertInstanceOf("1/Time", Scalar.Frequency.class, q5);
    }

    /**
     * Tests {@link Scalar#toString()}.
     */
    @Test
    public void testToString() {
        assertEquals("toString()", "24 km",   new Scalar.Length       (24.00, Units.KILOMETRE).toString());
        assertEquals("toString()", "10.25 h", new Scalar.Time         (10.25, Units.HOUR)     .toString());
        assertEquals("toString()", "0.25",    new Scalar.Dimensionless( 0.25, Units.UNITY)    .toString());
    }

    /**
     * Tests {@link Scalar#equals(Object)} and {@link Scalar#hashCode()}.
     */
    @Test
    public void testEqualsAndHashCode() {
        final Quantity<Length> q1 = new Scalar.Length(24, Units.METRE);
        Quantity<Length> q2 = new Scalar.Length(24, Units.METRE);
        assertEquals("hashCode()", q1.hashCode(), q2.hashCode());
        assertEquals("equals(…)", q1, q2);

        q2 = new Scalar.Length(12, Units.METRE);
        assertNotEquals("hashCode()", q1.hashCode(), q2.hashCode());
        assertNotEquals("equals(…)", q1, q2);

        q2 = new Scalar.Length(24, Units.CENTIMETRE);
        assertNotEquals("hashCode()", q1.hashCode(), q2.hashCode());
        assertNotEquals("equals(…)", q1, q2);
    }

    /**
     * Tests {@link Scalar} serialization.
     */
    @Test
    public void testSerialization() {
        final Quantity<Length> q1 = new Scalar.Length(24, Units.KILOMETRE);
        assertNotSame(q1, assertSerializedEquals(q1));
    }

    /**
     * Tests {@link ScalarFallback}, used when no specialized implementation is available for a given quantity type.
     */
    @Test
    public void testFallback() {
        final Quantity<Length> q1 = ScalarFallback.factory(24, Units.KILOMETRE, Length.class);
        assertInstanceOf("Dynamic proxy", Proxy .class, q1);
        assertInstanceOf("Dynamic proxy", Length.class, q1);
        assertSame  ("unit", Units.KILOMETRE, q1.getUnit());
        assertEquals("value", 24, q1.getValue().doubleValue(), STRICT);
        assertEquals("toString()", "24 km", q1.toString());

        final Quantity<Length> q2 = ScalarFallback.factory(24, Units.KILOMETRE, Length.class);
        assertEquals("hashCode()", q1.hashCode(), q2.hashCode());
        assertEquals("equals(…)", q1, q2);

        final Quantity<Length> q3 = ScalarFallback.factory(1500, Units.METRE, Length.class);
        final Quantity<Length> q4 = q1.add(q3);
        assertInstanceOf("Dynamic proxy", Proxy .class, q4);
        assertInstanceOf("Dynamic proxy", Length.class, q4);
        assertSame  ("unit", Units.KILOMETRE, q4.getUnit());
        assertEquals("value", 25.5, q4.getValue().doubleValue(), STRICT);
        assertEquals("toString()", "25.5 km", q4.toString());

        final Quantity<Length> q5 = q1.multiply(q3).divide(q2).asType(Length.class);
        assertSame  ("unit", Units.METRE, q5.getUnit());
        assertEquals("value", 1500, q5.getValue().doubleValue(), STRICT);
    }
}
