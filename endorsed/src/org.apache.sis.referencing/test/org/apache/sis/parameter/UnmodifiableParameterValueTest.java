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
import java.time.Instant;
import org.opengis.parameter.ParameterValue;
import org.apache.sis.util.ComparisonMode;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertMessageContains;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link UnmodifiableParameterValue} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class UnmodifiableParameterValueTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public UnmodifiableParameterValueTest() {
    }

    /**
     * Creates an {@link UnmodifiableParameterValue} implementation for the given parameter
     * and asserts that we got a new instance equivalent to the original one.
     */
    private static <T> DefaultParameterValue<T> assertEquivalent(final ParameterValue<T> modifiable) {
        final DefaultParameterValue<T> unmodifiable = DefaultParameterValue.unmodifiable(modifiable);
        assertNotSame(modifiable, unmodifiable);
        assertTrue(unmodifiable.equals(modifiable, ComparisonMode.BY_CONTRACT));
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
         * then verify that we cannot modify its value.
         */
        final DefaultParameterValue<Double> unmodifiable = assertEquivalent(modifiable);
        assertSame(modifiable.getValue(), unmodifiable.getValue(), "Instances of Double do not need to be cloned.");
        modifiable.setValue(1.0);

        var e = assertThrows(UnsupportedOperationException.class, () -> unmodifiable.setValue(1.0),
                             "UnmodifiableParameterValue shall not allow modification.");

        assertMessageContains(e, "DefaultParameterValue");
        assertEquals(1.0,      modifiable.doubleValue());
        assertEquals(0.9996, unmodifiable.doubleValue());
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
    public void testGetValue() {
        final ParameterValue<Date> modifiable = DefaultParameterDescriptorTest
                .createSimpleOptional("Time reference", Date.class).createValue();
        modifiable.setValue(Date.from(Instant.parse("1994-01-01T00:00:00Z")));
        /*
         * Create and validate an unmodifiable parameter,
         * then verify that the values are not the same.
         */
        final DefaultParameterValue<Date> unmodifiable = assertEquivalent(modifiable);
        final Date t1 =   modifiable.getValue();
        final Date t2 = unmodifiable.getValue();
        assertNotSame(t1, t2);
        assertEquals (t1, t2);
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
