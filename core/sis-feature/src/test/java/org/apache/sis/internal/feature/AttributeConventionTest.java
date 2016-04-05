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

import com.esri.core.geometry.Point;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.test.TestCase;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests {@link AttributeConvention}.
 *
 * @author Johann Sorel (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public class AttributeConventionTest extends TestCase {

    /**
     * Test of isConventionProperty method, of class AttributeConvention.
     */
    @Test
    public void testIsConventionProperty() {
        final AttributeTypeBuilder atb = new AttributeTypeBuilder();
        atb.setName("age");
        atb.setValueClass(Integer.class);
        assertFalse(AttributeConvention.isConventionProperty(atb.build()));

        atb.reset();
        atb.setName("http://sis.apache.org/feature","age");
        atb.setValueClass(Integer.class);
        assertTrue(AttributeConvention.isConventionProperty(atb.build()));

    }

    /**
     * Test of isGeometryAttribute method, of class AttributeConvention.
     */
    @Test
    public void testIsGeometryAttribute() {
        final AttributeTypeBuilder atb = new AttributeTypeBuilder();
        atb.setName("geometry");
        atb.setValueClass(Integer.class);
        assertFalse(AttributeConvention.isGeometryAttribute(atb.build()));

        atb.reset();
        atb.setName("geometry");
        atb.setValueClass(Point.class);
        assertTrue(AttributeConvention.isGeometryAttribute(atb.build()));
    }

    /**
     * Test of getCRSCharacteristic method, of class AttributeConvention.
     */
    @Test
    public void testGetCRSCharacteristic() {
        final AttributeTypeBuilder atb = new AttributeTypeBuilder();
        atb.setName("geometry");
        atb.setValueClass(Point.class);
        assertEquals(null,AttributeConvention.getCRSCharacteristic(atb.build()));

        atb.reset();
        atb.setName("geometry");
        atb.setValueClass(Point.class);
        atb.setCRSCharacteristic(CommonCRS.WGS84.geographic());
        assertEquals(CommonCRS.WGS84.geographic(),AttributeConvention.getCRSCharacteristic(atb.build()));
    }

    /**
     * Test of getLengthCharacteristic method, of class AttributeConvention.
     */
    @Test
    public void testGetLengthCharacteristic() {
        final AttributeTypeBuilder atb = new AttributeTypeBuilder();
        atb.setName("name");
        atb.setValueClass(String.class);
        assertEquals((Integer)null,AttributeConvention.getLengthCharacteristic(atb.build()));

        atb.reset();
        atb.setName("name");
        atb.setValueClass(String.class);
        atb.setLengthCharacteristic(120);
        assertEquals((Integer)120,AttributeConvention.getLengthCharacteristic(atb.build()));
    }


}
