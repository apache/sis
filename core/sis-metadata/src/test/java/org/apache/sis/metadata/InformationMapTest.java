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
package org.apache.sis.metadata;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Locale;
import org.opengis.metadata.ExtendedElementInformation;
import org.opengis.metadata.acquisition.EnvironmentalRecord;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.content.CoverageDescription;
import org.opengis.metadata.content.ImageDescription;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link InformationMap} class on instances created by
 * {@link MetadataStandard#asInformationMap(Class, KeyNamePolicy)}.
 * Unless otherwise specified, all tests use the {@link MetadataStandard#ISO_19115} constant.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
@DependsOn({PropertyAccessorTest.class, PropertyInformationTest.class})
public final strictfp class InformationMapTest extends TestCase {
    /**
     * Tests {@link InformationMap#get(Object)} on a few specific properties of the {@link Citation} type.
     * This test duplicates {@link PropertyInformationTest}, but is done here again as an integration test.
     */
    @Test
    public void testGet() {
        final Map<String,ExtendedElementInformation> map = MetadataStandard.ISO_19115.asInformationMap(
                Citation.class, KeyNamePolicy.JAVABEANS_PROPERTY);
        PropertyInformationTest.validateTitle(map.get("title"));
        PropertyInformationTest.validatePresentationForm(map.get("presentationForms"));
        assertNull("Shall not exists.", map.get("dummy"));
    }

    /**
     * Tests {@code InformationMap.keySet()}.
     * This method uses the {@link EnvironmentalRecord} metadata type.
     * There is no requirement on the properties order.
     */
    @Test
    public void testKeySet() {
        final Map<String,ExtendedElementInformation> descriptions = MetadataStandard.ISO_19115.asInformationMap(
                EnvironmentalRecord.class, KeyNamePolicy.UML_IDENTIFIER);

        final Set<String> expected = new HashSet<String>(Arrays.asList(
            "averageAirTemperature", "maxAltitude", "maxRelativeHumidity", "meteorologicalConditions"
        ));
        assertEquals(expected, descriptions.keySet());
    }

    /**
     * Tests on {@link ImageDescription} a property defined in the {@link CoverageDescription} parent class.
     */
    @Test
    public void testInheritance() {
        final Map<String,ExtendedElementInformation> descriptions = MetadataStandard.ISO_19115.asInformationMap(
                ImageDescription.class, KeyNamePolicy.UML_IDENTIFIER);

        assertEquals("Testing a property defined directly in the ImageDescription type.",
                "Area of the dataset obscured by clouds, expressed as a percentage of the spatial extent.",
                descriptions.get("cloudCoverPercentage").getDefinition().toString(Locale.ENGLISH));

        assertEquals("Testing a property inherited from the CoverageDescription parent.",
                "Description of the attribute described by the measurement value.",
                descriptions.get("attributeDescription").getDefinition().toString(Locale.ENGLISH));
    }
}
