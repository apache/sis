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
package org.apache.sis.referencing.gazetteer;

import java.util.List;
import org.opengis.metadata.extent.GeographicDescription;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;

// Branch-dependent imports
import org.opengis.referencing.gazetteer.LocationType;


/**
 * Tests {@link AbstractLocationType}, {@link FinalLocationType} and {@link ModifiableLocationType}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public final strictfp class LocationTypeTest extends TestCase {
    /**
     * Creates the example given in annex B of ISO 19112:2003.
     *
     * @param  inherit  {@code false} for defining all properties of all location types explicitely even
     *                  in case of redundancy, or {@code true} for relying on inheritance when possible.
     */
    static ModifiableLocationType[] create(final boolean inherit) {
        /*
         * From larger area to finer one. Each type is the child of the previous type,
         * except "street" which will have all the 3 parents.
         */
        final ModifiableLocationType area     = new ModifiableLocationType("administrative area");
        final ModifiableLocationType town     = new ModifiableLocationType("town");
        final ModifiableLocationType locality = new ModifiableLocationType("locality");
        final ModifiableLocationType street   = new ModifiableLocationType("street");
        final ModifiableLocationType property = new ModifiableLocationType("property");
        /*
         * Property used as the defining characteristic of the location type.
         */
        area    .setTheme("local administration");
        town    .setTheme("built environment");
        locality.setTheme("community");
        street  .setTheme("access");
        property.setTheme("built environment");
        /*
         * The way in which location instances are defined.
         */
        area    .setDefinition("area of responsibility of highest level local authority");
        town    .setDefinition("city or town");
        locality.setDefinition("neighbourhood, suburb, district, village, or settlement");
        street  .setDefinition("thoroughfare providing access to properties");
        property.setDefinition("land use");
        /*
         * The method(s) of uniquely identifying location instances.
         * The first 3 levels use the same method; it is possible to avoid repeating them.
         */
        area.addIdentification("name");
        if (!inherit) {
            town    .addIdentification("name");
            locality.addIdentification("name");
        }
        street  .addIdentification("unique street reference number");
        property.addIdentification("geographic address");
        /*
         * The name of the geographic area within which the location type occurs.
         * All levels in this test use the same geographic area, so it is possible to declare it only once.
         */
        area.setTerritoryOfUse("UK");
        if (!inherit) {
            town    .setTerritoryOfUse("UK");
            locality.setTerritoryOfUse("UK");
            street  .setTerritoryOfUse("UK");
            property.setTerritoryOfUse("UK");
        }
        /*
         * The name of the organization or class of organization able to create and destroy location instances.
         */
        area    .setOwner("UK government");
        town    .setOwner("Ordnance Survey");
        locality.setOwner("local authority");
        street  .setOwner("highway Authority");
        property.setOwner("local authority");
        /*
         * Hierarchy. Note that the street has 3 parents.
         */
        town    .addParent(area);
        locality.addParent(town);
        street  .addParent(locality);
        street  .addParent(town);
        street  .addParent(area);
        property.addParent(street);
        return new ModifiableLocationType[] {area, town, locality, street, property};
    }

    /**
     * Verifies the value of a "administrative area" location type.
     */
    private static void verify(final LocationType[] type) {
        assertEquals(5, type.length);
        verify(type[0], "administrative area",
                        "local administration",
                        "area of responsibility of highest level local authority",
                        "name",
                        "UK government");
        verify(type[1], "town",
                        "built environment",
                        "city or town",
                        "name",
                        "Ordnance Survey");
        verify(type[2], "locality",
                        "community",
                        "neighbourhood, suburb, district, village, or settlement",
                        "name",
                        "local authority");
        verify(type[3], "street",
                        "access",
                        "thoroughfare providing access to properties",
                        "unique street reference number",
                        "highway Authority");
        verify(type[4], "property",
                        "built environment",
                        "land use",
                        "geographic address",
                        "local authority");
    }

    /**
     * Verifies the value of a location type created by or copied from {@link #create(boolean)}.
     */
    private static void verify(final LocationType type, final String name, final String theme, final String definition,
            final String identification, final String owner)
    {
        assertEquals("name",           name,           String.valueOf(type.getName()));
        assertEquals("theme",          theme,          String.valueOf(type.getTheme()));
        assertEquals("definition",     definition,     String.valueOf(type.getDefinition()));
        assertEquals("identification", identification, String.valueOf(TestUtilities.getSingleton(type.getIdentifications())));
        assertEquals("owner",          owner,          String.valueOf(type.getOwner().getName()));
        assertEquals("territoryOfUse", "UK", ((GeographicDescription) type.getTerritoryOfUse()).getGeographicIdentifier().getCode());
    }

    /**
     * Tests the creation of the example given in annex B of ISO 19112:2003.
     * This method does not use inheritance.
     */
    @Test
    public void testExample() {
        verify(create(false));
    }

    /**
     * Tests the creation of the example given in annex B of ISO 19112:2003,
     * but without explicit declaration of property that can be inherited from the parent.
     */
    @Test
    @DependsOnMethod("testExample")
    public void testInheritance() {
        verify(create(true));
    }

    /**
     * Tests the creation of an unmodifiable snapshot.
     */
    @Test
    @DependsOnMethod("testInheritance")
    public void testSnapshot() {
        final List<LocationType> snapshot = ModifiableLocationType.snapshot(null, create(true));
        verify(snapshot.toArray(new LocationType[snapshot.size()]));
    }

    /**
     * Tests the string representation of location type.
     */
    @Test
    @DependsOnMethod("testInheritance")
    public void testToString() {
        verifyToString(create(true)[0]);
    }

    /**
     * Verify the string representation of "administrative area" location type.
     * This is the body of {@link #testToString()} method, but is also shared by other tests
     * as a convenient way to verify that a {@code ModifiableLocationType} did not changed.
     */
    private static void verifyToString(final ModifiableLocationType area) {
        assertMultilinesEquals(
                "administrative area………………… area of responsibility of highest level local authority\n" +
                "  ├─town……………………………………………… city or town\n" +
                "  │   ├─locality………………………… neighbourhood, suburb, district, village, or settlement\n" +
                "  │   │   └─street…………………… thoroughfare providing access to properties\n" +
                "  │   │       └─property…… land use\n" +
                "  │   └─street……………………………… thoroughfare providing access to properties\n" +
                "  │       └─property……………… land use\n" +
                "  └─street………………………………………… thoroughfare providing access to properties\n" +
                "      └─property………………………… land use\n", area.toString());
    }

    /**
     * Tests the equality and hash code value computation.
     */
    @Test
    @DependsOnMethod("testToString")                // Because in case of failure, JUnit invokes toString().
    public void testEquals() {
        final ModifiableLocationType t1 = create(false)[0];
        final ModifiableLocationType t2 = create(true )[0];
        assertEquals("hashCode", t1.hashCode(), t2.hashCode());
        assertEquals("equals", t1, t2);
        t2.removeIdentification("name");
        assertNotEquals("equals", t1, t2);
    }

    /**
     * Tests serialization.
     */
    @Test
    @DependsOnMethod("testEquals")
    public void testSerialization() {
        assertSerializedEquals(ModifiableLocationType.snapshot(null, create(true)));
    }

    /**
     * Tests the safety against infinite recursivity.
     * This method attempts to add "town" as a child of "street".
     */
    @Test
    @DependsOnMethod("testToString")
    public void testCheckForCycles() {
        final ModifiableLocationType[] types  = create(true);
        final ModifiableLocationType   town   = types[1];
        final ModifiableLocationType   street = types[3];
        try {
            town.addParent(street);
            fail("Shall not accept to add town as a child of street.");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("street"));
        }
        /*
         * Verify the string representation as a way to verify that parent addition
         * has been properly rolled back. If not, we may have an infinite loop here
         * until a StackOverflowError or OutOfMemoryError occurs.
         */
        verifyToString(types[0]);
    }
}
