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

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.Point;
import java.util.ArrayList;
import java.util.List;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.test.TestCase;
import static org.junit.Assert.*;
import org.junit.Test;
import org.opengis.feature.AttributeType;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;

/**
 * Tests {@link FeatureTypeBuilder}.
 *
 * @author Johann Sorel (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public class FeatureTypeBuilderTest extends TestCase {

    /**
     * Test a builder with the minimum number of parameters.
     */
    @Test
    public void buildMinimal() {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();

        //check at least name must be set
        try{
            ftb.build();
            fail("Builder should have failed if there is not at least a name set.");
        }catch(IllegalArgumentException ex){
            //ok
        }

        ftb.reset();
        ftb.setName("scope","test");
        FeatureType type = ftb.build();

        assertEquals("scope:test", type.getName().toString());
        assertFalse(type.isAbstract());
        assertEquals(0, type.getProperties(true).size());
        assertEquals(0, type.getSuperTypes().size());

    }

    /**
     * Test adding properties.
     */
    @Test
    public void testAddProperties() {

        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("myScope","myName");
        ftb.setDefinition("test definition");
        ftb.setDesignation("test designation");
        ftb.setDescription("test description");
        ftb.setAbstract(true);
        ftb.addProperty("name", String.class);
        ftb.addProperty("age", Integer.class);
        ftb.addProperty("location", Point.class, CommonCRS.WGS84.normalizedGeographic());
        ftb.addProperty("score", Double.class, 5, 50, 10.0);

        final FeatureType type = ftb.build();
        assertEquals("myScope:myName", type.getName().toString());
        assertEquals("test definition", type.getDefinition().toString());
        assertEquals("test description", type.getDescription().toString());
        assertEquals("test designation", type.getDesignation().toString());
        assertTrue(type.isAbstract());
        assertEquals(4, type.getProperties(true).size());

        final List<PropertyType> properties = new ArrayList(type.getProperties(true));
        assertEquals("name",    properties.get(0).getName().toString());
        assertEquals("age",     properties.get(1).getName().toString());
        assertEquals("location",properties.get(2).getName().toString());
        assertEquals("score",   properties.get(3).getName().toString());

        assertEquals(String.class,  ((AttributeType)properties.get(0)).getValueClass());
        assertEquals(Integer.class, ((AttributeType)properties.get(1)).getValueClass());
        assertEquals(Point.class,   ((AttributeType)properties.get(2)).getValueClass());
        assertEquals(Double.class,  ((AttributeType)properties.get(3)).getValueClass());

        assertEquals(1, ((AttributeType)properties.get(0)).getMinimumOccurs());
        assertEquals(1, ((AttributeType)properties.get(1)).getMinimumOccurs());
        assertEquals(1, ((AttributeType)properties.get(2)).getMinimumOccurs());
        assertEquals(5, ((AttributeType)properties.get(3)).getMinimumOccurs());

        assertEquals( 1, ((AttributeType)properties.get(0)).getMaximumOccurs());
        assertEquals( 1, ((AttributeType)properties.get(1)).getMaximumOccurs());
        assertEquals( 1, ((AttributeType)properties.get(2)).getMaximumOccurs());
        assertEquals(50, ((AttributeType)properties.get(3)).getMaximumOccurs());

        assertEquals(null, ((AttributeType)properties.get(0)).getDefaultValue());
        assertEquals(null, ((AttributeType)properties.get(1)).getDefaultValue());
        assertEquals(null, ((AttributeType)properties.get(2)).getDefaultValue());
        assertEquals(10.0, ((AttributeType)properties.get(3)).getDefaultValue());
    }

    /**
     * Test adding properties.
     */
    @Test
    public void testCopy() {

        FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("myScope","myName");
        ftb.setDefinition("test definition");
        ftb.setDesignation("test designation");
        ftb.setDescription("test description");
        ftb.setAbstract(true);
        ftb.addProperty("name", String.class);
        ftb.addProperty("age", Integer.class);
        ftb.addProperty("location", Point.class, CommonCRS.WGS84.normalizedGeographic());
        ftb.addProperty("score", Double.class, 5, 50, 10.0);

        FeatureType type = ftb.build();

        //copy the feature type
        ftb = new FeatureTypeBuilder();
        ftb.copy(type);
        type = ftb.build();


        assertEquals("myScope:myName", type.getName().toString());
        assertEquals("test definition", type.getDefinition().toString());
        assertEquals("test description", type.getDescription().toString());
        assertEquals("test designation", type.getDesignation().toString());
        assertTrue(type.isAbstract());
        assertEquals(4, type.getProperties(true).size());

        final List<PropertyType> properties = new ArrayList(type.getProperties(true));
        assertEquals("name",    properties.get(0).getName().toString());
        assertEquals("age",     properties.get(1).getName().toString());
        assertEquals("location",properties.get(2).getName().toString());
        assertEquals("score",   properties.get(3).getName().toString());

        assertEquals(String.class,  ((AttributeType)properties.get(0)).getValueClass());
        assertEquals(Integer.class, ((AttributeType)properties.get(1)).getValueClass());
        assertEquals(Point.class,   ((AttributeType)properties.get(2)).getValueClass());
        assertEquals(Double.class,  ((AttributeType)properties.get(3)).getValueClass());

        assertEquals(1, ((AttributeType)properties.get(0)).getMinimumOccurs());
        assertEquals(1, ((AttributeType)properties.get(1)).getMinimumOccurs());
        assertEquals(1, ((AttributeType)properties.get(2)).getMinimumOccurs());
        assertEquals(5, ((AttributeType)properties.get(3)).getMinimumOccurs());

        assertEquals( 1, ((AttributeType)properties.get(0)).getMaximumOccurs());
        assertEquals( 1, ((AttributeType)properties.get(1)).getMaximumOccurs());
        assertEquals( 1, ((AttributeType)properties.get(2)).getMaximumOccurs());
        assertEquals(50, ((AttributeType)properties.get(3)).getMaximumOccurs());

        assertEquals(null, ((AttributeType)properties.get(0)).getDefaultValue());
        assertEquals(null, ((AttributeType)properties.get(1)).getDefaultValue());
        assertEquals(null, ((AttributeType)properties.get(2)).getDefaultValue());
        assertEquals(10.0, ((AttributeType)properties.get(3)).getDefaultValue());
    }

    /**
     * Test reset operation.
     */
    @Test
    public void testReset(){

        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("myScope","myName");
        ftb.setDefinition("test definition");
        ftb.setDesignation("test designation");
        ftb.setDescription("test description");
        ftb.setAbstract(true);
        ftb.addProperty("name", String.class);
        ftb.addProperty("age", Integer.class);
        ftb.addProperty("location", Point.class, CommonCRS.WGS84.normalizedGeographic());
        ftb.addProperty("score", Double.class, 5, 50, 10.0);

        FeatureType type = ftb.build();
        ftb.reset();
        ftb.setName("scope","test");
        type = ftb.build();

        assertEquals("scope:test", type.getName().toString());
        assertFalse(type.isAbstract());
        assertEquals(0, type.getProperties(true).size());
        assertEquals(0, type.getSuperTypes().size());
    }

    /**
     * Test convention properties.
     */
    @Test
    @org.junit.Ignore("Temporarily broken builder.")
    public void testConventionProperties() {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("scope","test");
        ftb.setIdOperation("pref.", "name");
        ftb.setDefaultGeometryOperation("shape");
        ftb.addProperty("name", String.class);
        ftb.addProperty("shape", Geometry.class, CommonCRS.WGS84.normalizedGeographic());

        final FeatureType type = ftb.build();
        assertEquals("scope:test", type.getName().toString());
        assertFalse(type.isAbstract());
        assertEquals(5, type.getProperties(true).size());

        final List<PropertyType> properties = new ArrayList(type.getProperties(true));
        assertEquals(AttributeConvention.ATTRIBUTE_ID, properties.get(0).getName());
        assertEquals(AttributeConvention.ATTRIBUTE_DEFAULT_GEOMETRY, properties.get(1).getName());
        assertEquals(AttributeConvention.ATTRIBUTE_BOUNDS, properties.get(2).getName());
        assertEquals("name", properties.get(3).getName().toString());
        assertEquals("shape", properties.get(4).getName().toString());
    }
}
