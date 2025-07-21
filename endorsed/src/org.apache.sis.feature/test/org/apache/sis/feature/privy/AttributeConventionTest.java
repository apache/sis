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
package org.apache.sis.feature.privy;

import java.util.Map;
import com.esri.core.geometry.Point;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.feature.FeatureOperations;
import org.apache.sis.util.iso.Names;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.crs.HardCodedCRS;


/**
 * Tests {@link AttributeConvention}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class AttributeConventionTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public AttributeConventionTest() {
    }

    /**
     * Tests {@link AttributeConvention#contains(GenericName)} method.
     */
    @Test
    public void testIsConventionProperty() {
        assertFalse(AttributeConvention.contains(Names.createLocalName("MyFeature", ":", "City")));
        assertTrue (AttributeConvention.contains(AttributeConvention.ENVELOPE_PROPERTY));
        assertTrue (AttributeConvention.contains(AttributeConvention.ENVELOPE_PROPERTY.toFullyQualifiedName()));
    }

    /**
     * Tests {@link isGeometryAttribute(…)} method.
     */
    @Test
    public void testIsGeometryAttribute() {
        final Map<String,?> properties = Map.of(DefaultAttributeType.NAME_KEY, "geometry");

        assertFalse(AttributeConvention.isGeometryAttribute(
                new DefaultAttributeType<>(properties, Integer.class, 1, 1, null)));

        assertTrue(AttributeConvention.isGeometryAttribute(
                new DefaultAttributeType<>(properties, Point.class, 1, 1, null)));
    }

    /**
     * Tests {@code characterizedByCRS(…)} and {@code getCRSCharacteristic(…)} methods.
     */
    @Test
    public void testGetCrsCharacteristic() {
        final Map<String,?> properties = Map.of(DefaultAttributeType.NAME_KEY, "geometry");
        var attribute = new DefaultAttributeType<>(properties, Point.class, 1, 1, null);
        assertFalse(AttributeConvention.characterizedByCRS(attribute));
        assertFalse(attribute.characteristics().containsKey(AttributeConvention.CRS));
        /*
         * Creates an attribute associated to an attribute (i.e. a "characteristic") for storing
         * the Coordinate Reference System of the "geometry" attribute. Then test again.
         */
        final var characteristic = new DefaultAttributeType<CoordinateReferenceSystem>(
                Map.of(DefaultAttributeType.NAME_KEY, AttributeConvention.CRS_CHARACTERISTIC),
                CoordinateReferenceSystem.class, 1, 1, HardCodedCRS.WGS84);

        attribute = new DefaultAttributeType<>(properties, Point.class, 1, 1, null, characteristic);
        assertTrue(AttributeConvention.characterizedByCRS(attribute));
        assertEquals(HardCodedCRS.WGS84, AttributeConvention.getCRSCharacteristic(null, attribute));
        /*
         * Test again AttributeConvention.getCRSCharacteristic(…, PropertyType), but following link.
         */
        final var link    = FeatureOperations.link(Map.of(DefaultAttributeType.NAME_KEY, "geom"), attribute);
        final var feature = new DefaultFeatureType(Map.of(DefaultAttributeType.NAME_KEY, "feat"), false, null, attribute, link);
        assertEquals(HardCodedCRS.WGS84, AttributeConvention.getCRSCharacteristic(feature, link));
        assertNull(AttributeConvention.getCRSCharacteristic(null, link));
    }

    /**
     * Tests {@code characterizedByMaximalLength(…)} and {@code getMaximalLengthCharacteristic(…)} methods.
     */
    @Test
    public void testGetMaximalLengthCharacteristic() {
        final Map<String,?> properties = Map.of(DefaultAttributeType.NAME_KEY, "name");
        var attribute = new DefaultAttributeType<>(properties, String.class, 1, 1, null);
        assertFalse(AttributeConvention.characterizedByMaximalLength(attribute));
        assertNull(AttributeConvention.getMaximalLengthCharacteristic(null, attribute));
        /*
         * Creates an attribute associated to an attribute (i.e. a "characteristic") for storing
         * the maximal length of the "name" attribute. Then test again.
         */
        final var characteristic = new DefaultAttributeType<Integer>(
                Map.of(DefaultAttributeType.NAME_KEY, AttributeConvention.MAXIMAL_LENGTH_CHARACTERISTIC),
                Integer.class, 1, 1, 120);

        attribute = new DefaultAttributeType<>(properties, String.class, 1, 1, null, characteristic);
        assertTrue(AttributeConvention.characterizedByMaximalLength(attribute));
        assertEquals(Integer.valueOf(120), AttributeConvention.getMaximalLengthCharacteristic(null, attribute));
    }
}
