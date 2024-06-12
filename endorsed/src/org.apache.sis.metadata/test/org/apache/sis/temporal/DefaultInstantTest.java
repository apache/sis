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
package org.apache.sis.temporal;

import java.time.LocalDate;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.temporal.IndeterminateValue;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link DefaultInstant} class.
 *
 * @author  Mehdi Sidhoum (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DefaultInstantTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultInstantTest() {
    }

    /**
     * Tests {@link DefaultInstant#getPosition()}.
     * Opportunistically tests {@link TemporalUtilities#createInstant(Temporal)} too.
     */
    @Test
    public void testGetPosition() {
        var date    = LocalDate.of(2010, 5, 1);
        var instant = TemporalUtilities.createInstant(date);
        assertEquals(date, instant.getPosition());
    }

    /**
     * Test of equals and hash code methods.
     */
    @Test
    public void testEquals() {
        var instant1 = new DefaultInstant(LocalDate.of(2000, 1, 1), null);
        var instant2 = new DefaultInstant(LocalDate.of(1988, 1, 1), null);
        var instant3 = new DefaultInstant(LocalDate.of(1988, 1, 1), null);

        assertNotEquals(instant1, instant2);
        assertNotEquals(instant1.hashCode(), instant2.hashCode());

        assertEquals(instant3, instant2);
        assertEquals(instant3.hashCode(), instant2.hashCode());
    }

    /**
     * Tests {@link DefaultInstant#toString()}.
     */
    @Test
    public void testToString() {
        var date = LocalDate.of(2010, 5, 1);
        assertEquals("2010-05-01", new DefaultInstant(date, null).toString());
        assertEquals("after 2010-05-01", new DefaultInstant(date, IndeterminateValue.AFTER).toString());
    }
}
