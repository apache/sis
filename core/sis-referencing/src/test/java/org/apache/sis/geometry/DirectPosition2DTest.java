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
package org.apache.sis.geometry;

import org.opengis.geometry.DirectPosition;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static org.opengis.test.Validators.*;


/**
 * Tests the {@link DirectPosition2D} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@DependsOn(GeneralDirectPositionTest.class)
public final strictfp class DirectPosition2DTest extends TestCase {
    /**
     * Tests the {@link DirectPosition2D#toString()} method.
     */
    @Test
    public void testWktFormatting() {
        final DirectPosition2D position = new DirectPosition2D(6.5, 10);
        assertEquals("POINT(6.5 10)", position.toString());
        validate(position);
    }

    /**
     * Tests the {@link DirectPosition2D#DirectPosition2D(CharSequence)} constructor.
     */
    @Test
    public void testWktParsing() {
        final DirectPosition2D position = new DirectPosition2D("POINT(6 10)");
        assertEquals("POINT(6 10)", position.toString());
        validate(position);
    }

    /**
     * Tests {@link DirectPosition2D#equals(Object)} method between different implementations.
     * The purpose of this test is also to run the assertion in the direct position implementations.
     */
    @Test
    public void testEquals() {
        assertTrue(DirectPosition2D     .class.desiredAssertionStatus());
        assertTrue(GeneralDirectPosition.class.desiredAssertionStatus());

        DirectPosition p1 = new DirectPosition2D     (48.543261561072285, -123.47009555832284);
        DirectPosition p2 = new GeneralDirectPosition(48.543261561072285, -123.47009555832284);
        assertTrue(p1.equals(p2));
        assertTrue(p2.equals(p1));
        assertEquals(p2.hashCode(), p1.hashCode());

        p1.setOrdinate(0, p1.getOrdinate(0) + 1);
        assertFalse(p1.equals(p2));
        assertFalse(p2.equals(p1));
        assertFalse(p2.hashCode() == p1.hashCode());
    }

    /**
     * Tests {@link DirectPosition2D#clone()}.
     */
    @Test
    public void testClone() {
        final DirectPosition2D p1 = new DirectPosition2D(10, 30);
        final DirectPosition2D p2 = p1.clone();
        assertEquals("Expected the same CRS and ordinates.", p1, p2);
        assertEquals("Expected the same ordinates.", 10.0, p2.x, 0.0);
        assertEquals("Expected the same ordinates.", 30.0, p2.y, 0.0);
        validate(p2);
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialize() {
        final DirectPosition2D p1 = new DirectPosition2D(12, -20);
        final DirectPosition2D p2 = assertSerializedEquals(p1);
        assertNotSame(p1, p2);
        validate(p2);
    }
}
