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
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link Quantities}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
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
}
