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

import org.apache.sis.test.TestCase;
import org.junit.Test;
import org.opengis.feature.AttributeType;
import static org.junit.Assert.*;

/**
 * Tests {@link AttributeTypeBuilder}.
 *
 * @author Johann Sorel (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public class AttributeTypeBuilderTest extends TestCase {

    /**
     * Test a builder with the minimum number of parameters.
     */
    @Test
    public void testMinimumProperties() {

        final AttributeTypeBuilder atb = new AttributeTypeBuilder();

        //check at least name must be set
        try{
            atb.build();
            fail("Builder should have failed if there is not at least a name set.");
        }catch(IllegalArgumentException ex){
            //ok
        }

        atb.setName("myScope","myName");
        final AttributeType att = atb.build();

        assertEquals("myScope:myName", att.getName().toString());
        assertEquals(Object.class, att.getValueClass());
        assertEquals(null, att.getDefaultValue());
        assertEquals(null, att.getDefinition());
        assertEquals(null, att.getDescription());
        assertEquals(null, att.getDesignation());
        assertEquals(1, att.getMinimumOccurs());
        assertEquals(1, att.getMaximumOccurs());
        
    }

    /**
     * Test all properties.
     */
    @Test
    public void testAllProperties() {

        final AttributeTypeBuilder atb = new AttributeTypeBuilder();
        atb.setName("myScope","myName");
        atb.setDefinition("test definition");
        atb.setDesignation("test designation");
        atb.setDescription("test description");
        atb.setValueClass(String.class);
        atb.setDefaultValue("test text with words and letters.");
        atb.setMinimumOccurs(10);
        atb.setMaximumOccurs(60);
        atb.setLengthCharacteristic(80);
        final AttributeType att = atb.build();

        assertEquals("myScope:myName", att.getName().toString());
        assertEquals("test definition", att.getDefinition().toString());
        assertEquals("test description", att.getDescription().toString());
        assertEquals("test designation", att.getDesignation().toString());
        assertEquals(String.class, att.getValueClass());
        assertEquals("test text with words and letters.", att.getDefaultValue());
        assertEquals(10, att.getMinimumOccurs());
        assertEquals(60, att.getMaximumOccurs());
        assertEquals(80, (int)AttributeConvention.getLengthCharacteristic(att));
    }

    /**
     * Test copying properties.
     */
    @Test
    public void testCopy() {

        AttributeTypeBuilder atb = new AttributeTypeBuilder();
        atb.setName("myScope","myName");
        atb.setDefinition("test definition");
        atb.setDesignation("test designation");
        atb.setDescription("test description");
        atb.setValueClass(String.class);
        atb.setDefaultValue("test text with words and letters.");
        atb.setMinimumOccurs(10);
        atb.setMaximumOccurs(60);
        atb.setLengthCharacteristic(80);
        AttributeType att = atb.build();

        //copy the attribute
        atb = new AttributeTypeBuilder();
        atb.copy(att);
        att = atb.build();

        assertEquals("myScope:myName", att.getName().toString());
        assertEquals("test definition", att.getDefinition().toString());
        assertEquals("test description", att.getDescription().toString());
        assertEquals("test designation", att.getDesignation().toString());
        assertEquals(String.class, att.getValueClass());
        assertEquals("test text with words and letters.", att.getDefaultValue());
        assertEquals(10, att.getMinimumOccurs());
        assertEquals(60, att.getMaximumOccurs());
        assertEquals(80, (int)AttributeConvention.getLengthCharacteristic(att));
    }

    /**
     * Test reset operation.
     */
    @Test
    public void testReset() {

        final AttributeTypeBuilder atb = new AttributeTypeBuilder();
        atb.setName("myScope","myName1");
        atb.setDefinition("test definition");
        atb.setDesignation("test designation");
        atb.setDescription("test description");
        atb.setValueClass(String.class);
        atb.setDefaultValue("test text with words and letters.");
        atb.setMinimumOccurs(10);
        atb.setMaximumOccurs(60);
        atb.setLengthCharacteristic(80);
        AttributeType att = atb.build();

        atb.reset();
        atb.setName("myScope","myName2");
        att = atb.build();

        assertEquals("myScope:myName2", att.getName().toString());
        assertEquals(Object.class, att.getValueClass());
        assertEquals(null, att.getDefaultValue());
        assertEquals(null, att.getDefinition());
        assertEquals(null, att.getDescription());
        assertEquals(null, att.getDesignation());
        assertEquals(1, att.getMinimumOccurs());
        assertEquals(1, att.getMaximumOccurs());

    }

}
