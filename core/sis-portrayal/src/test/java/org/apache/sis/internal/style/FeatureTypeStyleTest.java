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
package org.apache.sis.internal.style;

import java.util.List;
import org.apache.sis.metadata.iso.citation.DefaultOnlineResource;
import org.apache.sis.util.iso.Names;
import static org.junit.Assert.*;
import org.junit.Test;
import org.opengis.filter.InvalidFilterValueException;
import org.opengis.filter.ResourceId;
import org.opengis.style.SemanticType;

/**
 * Tests for {@link org.apache.sis.internal.style.FeatureTypeStyle}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class FeatureTypeStyleTest extends AbstractStyleTests {

    /**
     * Test of Name methods.
     */
    @Test
    public void testName() {
        FeatureTypeStyle cdt = new FeatureTypeStyle();

        //check defaults
        assertEquals(null, cdt.getName());

        //check get/set
        cdt.setName(SAMPLE_STRING);
        assertEquals(SAMPLE_STRING, cdt.getName());
    }

    /**
     * Test of Description methods.
     */
    @Test
    public void testDescription() {
        FeatureTypeStyle cdt = new FeatureTypeStyle();

        //check defaults
        assertEquals(null, cdt.getDescription());

        //check get/set
        cdt.setDescription(new Description(SAMPLE_ISTRING, SAMPLE_ISTRING));
        assertEquals(new Description(SAMPLE_ISTRING, SAMPLE_ISTRING), cdt.getDescription());
    }

    /**
     * Test of FeatureInstanceIDs methods.
     */
    @Test
    public void testFeatureInstanceIDs() {
        FeatureTypeStyle cdt = new FeatureTypeStyle();

        //check defaults
        assertEquals(null, cdt.getFeatureInstanceIDs());

        //check get/set
        final ResourceId rid = new ResourceId() {
            @Override
            public String getIdentifier() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public List getExpressions() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public boolean test(Object object) throws InvalidFilterValueException {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public Class getResourceClass() {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        };
        cdt.setFeatureInstanceIDs(rid);
        assertEquals(rid, cdt.getFeatureInstanceIDs());
    }

    /**
     * Test of featureTypeNames methods.
     */
    @Test
    public void testFeatureTypeNames() {
        FeatureTypeStyle cdt = new FeatureTypeStyle();

        //check defaults
        assertTrue(cdt.featureTypeNames().isEmpty());

        //check get/set
        cdt.featureTypeNames().add(Names.createLocalName(null, null, "test"));
        assertEquals(1, cdt.featureTypeNames().size());
    }

    /**
     * Test of semanticTypeIdentifiers methods.
     */
    @Test
    public void testSemanticTypeIdentifiers() {
        FeatureTypeStyle cdt = new FeatureTypeStyle();

        //check defaults
        assertTrue(cdt.semanticTypeIdentifiers().isEmpty());

        //check get/set
        cdt.semanticTypeIdentifiers().add(SemanticType.LINE);
        assertEquals(1, cdt.semanticTypeIdentifiers().size());
    }

    /**
     * Test of rules methods.
     */
    @Test
    public void testRules() {
        FeatureTypeStyle cdt = new FeatureTypeStyle();

        //check defaults
        assertTrue(cdt.rules().isEmpty());

        //check get/set
        cdt.rules().add(new Rule());
        assertEquals(1, cdt.rules().size());
    }

    /**
     * Test of OnlineResource methods.
     */
    @Test
    public void testOnlineResource() {
        FeatureTypeStyle cdt = new FeatureTypeStyle();

        //check defaults
        assertEquals(null, cdt.getOnlineResource());

        //check get/set
        cdt.setOnlineResource(new DefaultOnlineResource());
        assertEquals(new DefaultOnlineResource(), cdt.getOnlineResource());
    }

}
