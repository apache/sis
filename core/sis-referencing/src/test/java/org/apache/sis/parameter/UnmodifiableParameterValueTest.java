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

import java.util.Date;
import org.opengis.parameter.ParameterValue;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.apache.sis.util.ComparisonMode;
import org.junit.Test;

import static org.apache.sis.test.TestUtilities.date;
import static org.junit.Assert.*;


/**
 * Tests the {@link UnmodifiableParameterValue} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@DependsOn(DefaultParameterValueTest.class)
public final strictfp class UnmodifiableParameterValueTest extends TestCase {
    /**
     * Creates an {@link UnmodifiableParameterValue} implementation for the given parameter
     * and asserts that we got a new instance equivalent to the original one.
     */
    private static <T> DefaultParameterValue<T> assertEquivalent(final ParameterValue<T> modifiable) {
        final DefaultParameterValue<T> unmodifiable = DefaultParameterValue.unmodifiable(modifiable);
        assertNotSame("Expected a new instance.", modifiable, unmodifiable);
        assertTrue("New instance shall be equal to the original one.",
                unmodifiable.equals(modifiable, ComparisonMode.BY_CONTRACT));
        return unmodifiable;
    }

    /**
     * Tests the value returned by {@link DefaultParameterValue#unmodifiable(ParameterValue)}.
     */
    @Test
    public void testCreate() {
        final ParameterValue<Double> modifiable = DefaultParameterDescriptorTest
                .createSimpleOptional("Scale factor", Double.class).createValue();
        modifiable.setValue(0.9996); // Scale factor of all UTM projections.
        /*
         * Create and validate an unmodifiable parameter,
         * then verify that we can not modify its value.
         */
        final DefaultParameterValue<Double> unmodifiable = assertEquivalent(modifiable);
        assertSame("Double instances do not need to be cloned.", modifiable.getValue(), unmodifiable.getValue());
        modifiable.setValue(1.0);
        try {
            unmodifiable.setValue(1.0);
            fail("UnmodifiableParameterValue shall not allow modification.");
        } catch (UnsupportedOperationException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("DefaultParameterValue"));
        }
        assertEquals(1.0,      modifiable.doubleValue(), STRICT);
        assertEquals(0.9996, unmodifiable.doubleValue(), STRICT);
        /*
         * Verify that invoking again DefaultParameterValue.unmodifiable(…) return the same instance.
         * Opportunistically verify that we detect null value and instances already unmodifiable.
         */
        assertNull   (              DefaultParameterValue.unmodifiable(null));
        assertSame   (unmodifiable, DefaultParameterValue.unmodifiable(unmodifiable));
        assertNotSame(unmodifiable, assertEquivalent(modifiable));
        modifiable.setValue(0.9996); // Restore our original value.
        assertSame   (unmodifiable, assertEquivalent(modifiable));
    }

    /**
     * Verifies that {@link UnmodifiableParameterValue#getValue()} can clone the value.
     */
    @Test
    @DependsOnMethod("testCreate")
    public void testGetValue() {
        final ParameterValue<Date> modifiable = DefaultParameterDescriptorTest
                .createSimpleOptional("Time reference", Date.class).createValue();
        modifiable.setValue(date("1994-01-01 00:00:00"));
        /*
         * Create and validate an unmodifiable parameter,
         * then verify that the values are not the same.
         */
        final DefaultParameterValue<Date> unmodifiable = assertEquivalent(modifiable);
        final Date t1 =   modifiable.getValue();
        final Date t2 = unmodifiable.getValue();
        assertNotSame("Date should be cloned.", t1, t2);
        assertEquals(t1, t2);
        /*
         * Verify that cloning the parameter also clone its value.
         */
        final DefaultParameterValue<Date> clone = unmodifiable.clone();
        assertEquals(DefaultParameterValue.class, clone.getClass());
        final Date t3 = clone.getValue();
        assertNotSame(t1, t3);
        assertNotSame(t2, t3);
        assertEquals (t1, t3);
    }
}
