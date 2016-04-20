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
package org.apache.sis.internal.feature;

import java.util.Map;
import java.util.Collections;
import com.esri.core.geometry.Point;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.util.iso.Names;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link AttributeConvention}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final strictfp class AttributeConventionTest extends TestCase {
    /**
     * Tests {@link AttributeConvention#contains(GenericName)} method.
     */
    @Test
    public void testIsConventionProperty() {
        assertFalse("Feature-specific name", AttributeConvention.contains(Names.createLocalName("MyFeature", ":", "City")));
        assertTrue ("Conventional name",     AttributeConvention.contains(AttributeConvention.ENVELOPE_PROPERTY));
        assertTrue ("Fully qualified name",  AttributeConvention.contains(AttributeConvention.ENVELOPE_PROPERTY.toFullyQualifiedName()));
    }

    /**
     * Tests {@link AttributeConvention#isGeometryAttribute(IdentifiedType)} method.
     */
    @Test
    public void testIsGeometryAttribute() {
        final Map<String,?> properties = Collections.singletonMap(DefaultAttributeType.NAME_KEY, "geometry");

        assertFalse("AttributeType<Integer>", AttributeConvention.isGeometryAttribute(
                new DefaultAttributeType<Integer>(properties, Integer.class, 1, 1, null)));

        assertTrue("AttributeType<Point>", AttributeConvention.isGeometryAttribute(
                new DefaultAttributeType<Point>(properties, Point.class, 1, 1, null)));
    }

    /**
     * Tests {@link AttributeConvention#characterizedByCRS(IdentifiedType)} and
     * {@link AttributeConvention#getCRSCharacteristic(Property)} methods.
     */
    @Test
    public void testGetCrsCharacteristic() {
        final Map<String,?> properties = Collections.singletonMap(DefaultAttributeType.NAME_KEY, "geometry");
        DefaultAttributeType<Point> type = new DefaultAttributeType<Point>(properties, Point.class, 1, 1, null);
        assertFalse("characterizedByCRS",  AttributeConvention.characterizedByCRS(type));
        assertNull("getCRSCharacteristic", AttributeConvention.getCRSCharacteristic(type.newInstance()));
        /*
         * Creates an attribute associated to an attribute (i.e. a "characteristic") for storing
         * the Coordinate Reference System of the "geometry" attribute. Then test again.
         */
        final DefaultAttributeType<CoordinateReferenceSystem> characteristic = new DefaultAttributeType<CoordinateReferenceSystem>(
                Collections.singletonMap(DefaultAttributeType.NAME_KEY, AttributeConvention.CRS_CHARACTERISTIC),
                CoordinateReferenceSystem.class, 1, 1, HardCodedCRS.WGS84);

        type = new DefaultAttributeType<Point>(properties, Point.class, 1, 1, null, characteristic);
        assertTrue("characterizedByCRS", AttributeConvention.characterizedByCRS(type));
        assertEquals(HardCodedCRS.WGS84, AttributeConvention.getCRSCharacteristic(type.newInstance()));
    }

    /**
     * Tests {@link AttributeConvention#characterizedByMaximalLength(IdentifiedType)} and
     * {@link AttributeConvention#getMaximalLengthCharacteristic(Property)} methods.
     */
    @Test
    public void testGetMaximalLengthCharacteristic() {
        final Map<String,?> properties = Collections.singletonMap(DefaultAttributeType.NAME_KEY, "name");
        DefaultAttributeType<String> type = new DefaultAttributeType<String>(properties, String.class, 1, 1, null);
        assertFalse("characterizedByMaximalLength",  AttributeConvention.characterizedByMaximalLength(type));
        assertNull("getMaximalLengthCharacteristic", AttributeConvention.getMaximalLengthCharacteristic(type.newInstance()));
        /*
         * Creates an attribute associated to an attribute (i.e. a "characteristic") for storing
         * the maximal length of the "name" attribute. Then test again.
         */
        final DefaultAttributeType<Integer> characteristic = new DefaultAttributeType<Integer>(
                Collections.singletonMap(DefaultAttributeType.NAME_KEY, AttributeConvention.MAXIMAL_LENGTH_CHARACTERISTIC),
                Integer.class, 1, 1, 120);

        type = new DefaultAttributeType<String>(properties, String.class, 1, 1, null, characteristic);
        assertTrue("characterizedByMaximalLength", AttributeConvention.characterizedByMaximalLength(type));
        assertEquals(Integer.valueOf(120), AttributeConvention.getMaximalLengthCharacteristic(type.newInstance()));
    }
}
