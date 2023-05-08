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

import java.util.Set;
import java.util.Locale;
import javax.measure.Unit;
import javax.measure.format.UnitFormat;
import javax.measure.spi.FormatService;
import javax.measure.spi.ServiceProvider;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.Assertions.assertSetEquals;


/**
 * Test {@link UnitServices}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   0.8
 */
@DependsOn(UnitsTest.class)
public final class UnitServicesTest extends TestCase {
    /**
     * Tests the default system of units.
     */
    @Test
    public void testDefaultSystemOfUnits() {
        final ServiceProvider provider = ServiceProvider.current();
        Set<? extends Unit<?>> units = provider.getSystemOfUnitsService().getSystemOfUnits().getUnits();
        assertTrue("METRE",                units.contains(Units.METRE));
        assertTrue("KILOMETRE",            units.contains(Units.KILOMETRE));
        assertTrue("CUBIC_METRE",          units.contains(Units.CUBIC_METRE));
        assertTrue("METRES_PER_SECOND",    units.contains(Units.METRES_PER_SECOND));
        assertTrue("KILOMETRES_PER_HOUR",  units.contains(Units.KILOMETRES_PER_HOUR));
        assertTrue("NAUTICAL_MILE",        units.contains(Units.NAUTICAL_MILE));
        assertTrue("STATUTE_MILE",         units.contains(Units.STATUTE_MILE));
        assertTrue("DEGREE",               units.contains(Units.DEGREE));
        assertTrue("RADIAN",               units.contains(Units.RADIAN));
        assertTrue("GRAD",                 units.contains(Units.GRAD));
    }

    /**
     * Tests the "SI" system of units.
     */
    @Test
    public void testSI() {
        final ServiceProvider provider = ServiceProvider.current();
        Set<? extends Unit<?>> units = provider.getSystemOfUnitsService().getSystemOfUnits("SI").getUnits();
        assertTrue ("METRE",                units.contains(Units.METRE));
        assertTrue ("KILOMETRE",            units.contains(Units.KILOMETRE));
        assertTrue ("CUBIC_METRE",          units.contains(Units.CUBIC_METRE));
        assertTrue ("METRES_PER_SECOND",    units.contains(Units.METRES_PER_SECOND));
        assertFalse("KILOMETRES_PER_HOUR",  units.contains(Units.KILOMETRES_PER_HOUR));
        assertFalse("NAUTICAL_MILE",        units.contains(Units.NAUTICAL_MILE));
        assertFalse("STATUTE_MILE",         units.contains(Units.STATUTE_MILE));
        assertFalse("DEGREE",               units.contains(Units.DEGREE));
        assertTrue ("RADIAN",               units.contains(Units.RADIAN));
        assertFalse("GRAD",                 units.contains(Units.GRAD));
    }

    /**
     * Tests the "SI + accepted" system of units.
     */
    @Test
    public void testAccepted() {
        final ServiceProvider provider = ServiceProvider.current();
        Set<? extends Unit<?>> units = provider.getSystemOfUnitsService().getSystemOfUnits("SI + accepted").getUnits();
        assertTrue ("METRE",                units.contains(Units.METRE));
        assertTrue ("KILOMETRE",            units.contains(Units.KILOMETRE));
        assertTrue ("CUBIC_METRE",          units.contains(Units.CUBIC_METRE));
        assertTrue ("METRES_PER_SECOND",    units.contains(Units.METRES_PER_SECOND));
        assertTrue ("KILOMETRES_PER_HOUR",  units.contains(Units.KILOMETRES_PER_HOUR));
        assertFalse("NAUTICAL_MILE",        units.contains(Units.NAUTICAL_MILE));
        assertFalse("STATUTE_MILE",         units.contains(Units.STATUTE_MILE));
        assertTrue ("DEGREE",               units.contains(Units.DEGREE));
        assertTrue ("RADIAN",               units.contains(Units.RADIAN));
        assertFalse("GRAD",                 units.contains(Units.GRAD));
    }

    /**
     * Tests the "Imperial" system of units.
     */
    @Test
    public void testImperial() {
        final ServiceProvider provider = ServiceProvider.current();
        Set<? extends Unit<?>> units = provider.getSystemOfUnitsService().getSystemOfUnits("Imperial").getUnits();
        assertFalse("METRE",                units.contains(Units.METRE));
        assertFalse("KILOMETRE",            units.contains(Units.KILOMETRE));
        assertFalse("CUBIC_METRE",          units.contains(Units.CUBIC_METRE));
        assertFalse("METRES_PER_SECOND",    units.contains(Units.METRES_PER_SECOND));
        assertFalse("KILOMETRES_PER_HOUR",  units.contains(Units.KILOMETRES_PER_HOUR));
        assertTrue ("STATUTE_MILE",         units.contains(Units.STATUTE_MILE));
        assertFalse("DEGREE",               units.contains(Units.DEGREE));
        assertFalse("RADIAN",               units.contains(Units.RADIAN));
        assertFalse("GRAD",                 units.contains(Units.GRAD));
    }

    /**
     * Tests {@link UnitServices#getAvailableFormatNames(UnitServices.FormatType)}.
     */
    @Test
    public void testGetAvailableFormatNames() {
        final ServiceProvider provider = ServiceProvider.current();
        final FormatService service = provider.getFormatService();
        final Set<String> formats = service.getAvailableFormatNames(FormatService.FormatType.UNIT_FORMAT);
        assertSetEquals(Set.of("SYMBOL", "UCUM", "NAME"), formats);
    }

    /**
     * Tests {@link UnitServices#getUnitFormat(String)}.
     */
    @Test
    public void testGetUnitFormat() {
        final ServiceProvider provider = ServiceProvider.current();
        final UnitFormat f = provider.getFormatService().getUnitFormat("name");
        ((org.apache.sis.measure.UnitFormat) f).setLocale(Locale.US);
        assertEquals("CUBIC_METRE", "cubic meter", f.format(Units.CUBIC_METRE));
    }
}
