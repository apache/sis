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

// Branch-dependent imports
import org.opengis.feature.AttributeType;
import org.opengis.feature.IdentifiedType;


/**
 * Tests {@link NameConvention}.
 *
 * @author  Johann Sorel (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final strictfp class NameConventionTest extends TestCase {
    /**
     * Tests {@link NameConvention#contains(GenericName)} method.
     */
    @Test
    public void testIsConventionProperty() {
        assertFalse("Feature-specific name", NameConvention.contains(Names.createLocalName("MyFeature", ":", "City")));
        assertTrue ("Conventional name",     NameConvention.contains(NameConvention.ENVELOPE_PROPERTY));
        assertTrue ("Fully qualified name",  NameConvention.contains(NameConvention.ENVELOPE_PROPERTY.toFullyQualifiedName()));
    }

    /**
     * Tests {@link NameConvention#isGeometryAttribute(IdentifiedType)} method.
     */
    @Test
    public void testIsGeometryAttribute() {
        final Map<String,?> properties = Collections.singletonMap(DefaultAttributeType.NAME_KEY, "geometry");

        assertFalse("AttributeType<Integer>", NameConvention.isGeometryAttribute(
                new DefaultAttributeType<>(properties, Integer.class, 1, 1, null)));

        assertTrue("AttributeType<Point>", NameConvention.isGeometryAttribute(
                new DefaultAttributeType<>(properties, Point.class, 1, 1, null)));
    }

    /**
     * Tests {@link NameConvention#getCoordinateReferenceSystem(IdentifiedType)} method.
     */
    @Test
    public void testGetCoordinateReferenceSystem() {
        final Map<String,?> properties = Collections.singletonMap(DefaultAttributeType.NAME_KEY, "geometry");
        DefaultAttributeType<Point> type = new DefaultAttributeType<>(properties, Point.class, 1, 1, null);
        assertNull("Without characteristic", NameConvention.getCoordinateReferenceSystem(type));
        /*
         * Creates an attribute associated to an attribute (i.e. a "characteristic") for storing
         * the Coordinate Reference System of the "geometry" attribute. Then test again.
         */
        final DefaultAttributeType<CoordinateReferenceSystem> characteristic = new DefaultAttributeType<>(
                Collections.singletonMap(DefaultAttributeType.NAME_KEY, NameConvention.CRS_CHARACTERISTIC),
                CoordinateReferenceSystem.class, 1, 1, HardCodedCRS.WGS84);

        type = new DefaultAttributeType<>(properties, Point.class, 1, 1, null, characteristic);
        assertEquals(HardCodedCRS.WGS84, NameConvention.getCoordinateReferenceSystem(type));
    }

    /**
     * Tests {@link NameConvention#getMaximalLength(AttributeType)} method.
     */
    @Test
    public void testGetMaximalLength() {
        final Map<String,?> properties = Collections.singletonMap(DefaultAttributeType.NAME_KEY, "name");
        DefaultAttributeType<String> type = new DefaultAttributeType<>(properties, String.class, 1, 1, null);
        assertNull("Without characteristic", NameConvention.getMaximalLength(type));
        /*
         * Creates an attribute associated to an attribute (i.e. a "characteristic") for storing
         * the maximal length of the "name" attribute. Then test again.
         */
        final DefaultAttributeType<Integer> characteristic = new DefaultAttributeType<>(
                Collections.singletonMap(DefaultAttributeType.NAME_KEY, NameConvention.MAXIMAL_LENGTH_CHARACTERISTIC),
                Integer.class, 1, 1, 120);

        type = new DefaultAttributeType<>(properties, String.class, 1, 1, null, characteristic);
        assertEquals(Integer.valueOf(120), NameConvention.getMaximalLength(type));
    }
}
