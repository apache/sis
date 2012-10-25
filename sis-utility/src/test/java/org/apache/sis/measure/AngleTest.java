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

import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link Angle} class.
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @since   0.3 (derived from geotk-2.0)
 * @version 0.3
 * @module
 */
@DependsOn(AngleFormatTest.class)
public final strictfp class AngleTest extends TestCase {
    /**
     * Tests the {@link Angle#toString()} method.
     */
    @Test
    public void testToString() {
        assertEquals("45°30′00″",  new Angle    (45.5).toString());
        assertEquals("45°30′00″N", new Latitude (45.5).toString());
        assertEquals("45°30′00″E", new Longitude(45.5).toString());

        // Angle out of expected range.
        assertEquals( "720.0°E", new Longitude( 720).toString());
        assertEquals(  "99.0°S", new Latitude ( -99).toString());
        assertEquals("-361.0°",  new Angle    (-361).toString());

        // Small angles; should switch to scientific notation.
        assertEquals("3.6E-7″",  new Angle    ( 1E-10).toString());
        assertEquals("3.6E-7″N", new Latitude ( 1E-10).toString());
        assertEquals("3.6E-7″W", new Longitude(-1E-10).toString());
    }

    /**
     * Tests the parsing performed by the constructor.
     */
    @Test
    public void testParse() {
        assertEquals(new Angle    (45.5), new Angle    ("45°30′00″"));
        assertEquals(new Latitude (45.5), new Latitude ("45°30′00″N"));
        assertEquals(new Longitude(45.5), new Longitude("45°30′00″E"));

        // Test the capability to differentiate 'E' of 'East' from exponential notation.
        assertEquals(new Longitude(45.5), new Longitude("45.5°E"));
        assertEquals(new Longitude(45.5), new Longitude("45.5E"));
        assertEquals(new Longitude(455.), new Longitude("45.5E1"));
        assertEquals(new Longitude(4.55), new Longitude("45.5E-1"));
    }
}
