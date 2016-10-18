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
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link UnitFormat} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public final strictfp class UnitFormatTest extends TestCase {
    /**
     * Tests {@link UnitFormat#format(Object)} using the system-wide instance.
     */
    @Test
    public void testFormat() {
        final UnitFormat f = UnitFormat.INSTANCE;
        assertEquals("m",     f.format(Units.METRE));
        assertEquals("rad",   f.format(Units.RADIAN));
        assertEquals("s",     f.format(Units.SECOND));
        assertEquals("min",   f.format(Units.MINUTE));
        assertEquals("h",     f.format(Units.HOUR));
        assertEquals("Hz",    f.format(Units.HERTZ));
        assertEquals("Pa",    f.format(Units.PASCAL));
        assertEquals("kg",    f.format(Units.KILOGRAM));
        assertEquals("N",     f.format(Units.NEWTON));
        assertEquals("J",     f.format(Units.JOULE));
        assertEquals("W",     f.format(Units.WATT));
        assertEquals("K",     f.format(Units.KELVIN));
        assertEquals("℃",     f.format(Units.CELSIUS));
        assertEquals("m∕s",   f.format(Units.METRES_PER_SECOND));
        assertEquals("km∕h",  f.format(Units.KILOMETRES_PER_HOUR));
        assertEquals("m²",    f.format(Units.SQUARE_METRE));
        assertEquals("m³",    f.format(Units.CUBIC_METRE));
        assertEquals("",      f.format(Units.UNITY));
        assertEquals("%",     f.format(Units.PERCENT));
        assertEquals("psu",   f.format(Units.PSU));
        assertEquals("px",    f.format(Units.PIXEL));
        assertEquals("d",     f.format(Units.DAY));
        assertEquals("wk",    f.format(Units.WEEK));
    }
}
