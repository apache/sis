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
package org.apache.sis.xml.bind.cat;

import java.util.List;
import java.util.Collection;
import jakarta.xml.bind.JAXBException;
import org.opengis.metadata.identification.TopicCategory;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.xml.Namespaces;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.xml.test.TestCase;
import static org.apache.sis.test.Assertions.assertSetEquals;

// Specific to the geoapi-4.0 branch:
import java.util.EnumSet;


/**
 * Tests the XML marshalling of {@code Enum}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 */
public final class EnumMarshallingTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public EnumMarshallingTest() {
    }

    /**
     * Tests (un)marshalling of an enumeration.
     *
     * @throws JAXBException if an error occurred while (un)marshalling the XML.
     */
    @Test
    public void testTopicCategories() throws JAXBException {
        final List<TopicCategory> topics = List.of(
                TopicCategory.OCEANS,
                TopicCategory.ENVIRONMENT,
                TopicCategory.IMAGERY_BASE_MAPS_EARTH_COVER);   // We need to test at least one enum with many words.

        final DefaultDataIdentification id = new DefaultDataIdentification();
        id.setTopicCategories(topics);
        String expected =
                "<mri:MD_DataIdentification xmlns:mri=\"" + Namespaces.MRI + "\">\n" +
                "  <mri:topicCategory>\n" +
                "    <mri:MD_TopicCategoryCode>environment</mri:MD_TopicCategoryCode>\n" +
                "  </mri:topicCategory>\n" +
                "  <mri:topicCategory>\n" +
                "    <mri:MD_TopicCategoryCode>imageryBaseMapsEarthCover</mri:MD_TopicCategoryCode>\n" +
                "  </mri:topicCategory>\n" +
                "  <mri:topicCategory>\n" +
                "    <mri:MD_TopicCategoryCode>oceans</mri:MD_TopicCategoryCode>\n" +
                "  </mri:topicCategory>\n" +
                "</mri:MD_DataIdentification>";

        final String xml = marshal(id, VERSION_2014);
        assertXmlEquals(expected, xml, "xmlns:*");
        /*
         * Unmarshal the above XML and verify that we find all the topic categories.
         */
        final Collection<TopicCategory> unmarshalled = unmarshal(DefaultDataIdentification.class, expected).getTopicCategories();
        assertInstanceOf(EnumSet.class, unmarshalled);
        assertSetEquals(topics, unmarshalled);
    }
}
