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
package org.apache.sis.storage.base;

import java.time.Year;
import java.util.Map;
import java.util.Locale;
import org.opengis.util.GenericName;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.content.ContentInformation;
import org.opengis.metadata.constraint.Restriction;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.feature.DefaultFeatureType;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.metadata.Assertions.assertTitleEquals;
import static org.apache.sis.metadata.Assertions.assertPartyNameEquals;
import static org.apache.sis.test.TestUtilities.getSingleton;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.content.FeatureCatalogueDescription;
import org.opengis.metadata.content.FeatureTypeInfo;
import org.opengis.metadata.constraint.LegalConstraints;
import org.opengis.feature.FeatureType;


/**
 * Tests {@link MetadataBuilder}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 */
public final class MetadataBuilderTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public MetadataBuilderTest() {
    }

    /**
     * Tests {@link MetadataBuilder#parseLegalNotice(String)}.
     * The expected result of this parsing is:
     *
     * <pre class="text">
     *   Metadata
     *     └─Identification info
     *         └─Resource constraints
     *             ├─Use constraints……………………………… Copyright
     *             └─Reference
     *                 ├─Title……………………………………………… Copyright (C), John Smith, 1992. All rights reserved.
     *                 ├─Date
     *                 │   ├─Date……………………………………… 1992-01-01
     *                 │   └─Date type………………………… In force
     *                 └─Cited responsible party
     *                     ├─Party
     *                     │   └─Name…………………………… John Smith
     *                     └─Role……………………………………… Owner</pre>
     */
    @Test
    public void testParseLegalNotice() {
        verifyCopyrightParsing("Copyright (C), John Smith, 1992. All rights reserved.");
        verifyCopyrightParsing("(C) 1992, John Smith. All rights reserved.");
        verifyCopyrightParsing("(C) COPYRIGHT 1992 John Smith.");
    }

    /**
     * Verifies the metadata that contains the result of parsing a copyright statement.
     * Should contain the "John Smith" name and 1992 year.
     *
     * @param notice  the copyright statement to parse.
     */
    private static void verifyCopyrightParsing(final String notice) {
        final var builder = new MetadataBuilder();
        builder.parseLegalNotice(null, notice);
        final Citation ref = copyright(builder);
        assertTitleEquals(notice, ref, "reference.title");
        assertPartyNameEquals("John Smith", ref, "reference.citedResponsibleParty");
        assertEquals(Year.of(1992), getSingleton(ref.getDates()).getReferenceDate());
    }

    /**
     * Returns the citation of the legal constraints built by the given builder.
     * This method verifies that the constraint is a copyright.
     */
    private static Citation copyright(final MetadataBuilder builder) {
        final var id = getSingleton(builder.build().getIdentificationInfo());
        final var constraints = assertInstanceOf(LegalConstraints.class, getSingleton(id.getResourceConstraints()));
        assertEquals(Restriction.COPYRIGHT, getSingleton(constraints.getUseConstraints()));
        return getSingleton(constraints.getReferences());
    }

    /**
     * Tests {@link MetadataBuilder#parseLegalNotice(String)} with different languages.
     */
    @Test
    public void testParseLegalNoticeLocalized() {
        final var builder = new MetadataBuilder();
        builder.parseLegalNotice(Locale.ENGLISH, "Copyright (C), John Smith, 1997. All rights reserved.");
        builder.parseLegalNotice(Locale.FRENCH,  "Copyright (C), John Smith, 1997. Tous droits réservés.");
        final Citation ref = copyright(builder);
        assertEquals(Year.of(1997), getSingleton(ref.getDates()).getReferenceDate());
        assertPartyNameEquals("John Smith", ref, "reference.citedResponsibleParty");
        final var title = ref.getTitle();
        assertEquals("Copyright (C), John Smith, 1997. All rights reserved.",  title.toString(Locale.ENGLISH));
        assertEquals("Copyright (C), John Smith, 1997. Tous droits réservés.", title.toString(Locale.FRENCH));
    }

    /**
     * Tests {@link MetadataBuilder#addFeatureType(FeatureType, long)}.
     */
    @Test
    public void testAddFeatureType() {
        final var dataType = new DefaultFeatureType(Map.of(DefaultFeatureType.NAME_KEY, "Test type"), false, null);
        verifyFeatureInstanceCount(dataType, "Feature count should not be written if it is negative", null, -1);
        verifyFeatureInstanceCount(dataType, "Feature count should be limited to maximum 32bit integer value", Integer.MAX_VALUE, 7_000_000_000L);
        verifyFeatureInstanceCount(dataType, "Feature count should be written as is", 42, 42);
        verifyFeatureInstanceCount(dataType, "Feature should not be written if count is 0", null, 0);
    }

    /**
     * Creates a new simple metadata with a single simple feature type and the given
     * {@linkplain FeatureTypeInfo#getFeatureInstanceCount() feature instance count}.
     * Then, asserts that the value in the built metadata is compliant with a given control value.
     *
     * @param expected       the feature instance count value we want to see in the metadata (control value).
     * @param valueToInsert  the value to send to the metadata builder.
     */
    private static void verifyFeatureInstanceCount(final DefaultFeatureType dataType,
            final String errorMessage, final Integer expected, final long valueToInsert)
    {
        final var builder  = new MetadataBuilder();
        final GenericName name = builder.addFeatureType(dataType, valueToInsert);
        assertNotNull(name);

        final DefaultMetadata metadata = builder.build();
        if (valueToInsert == 0) {
            assertTrue(metadata.getContentInfo().isEmpty());
        } else {
            final ContentInformation content = getSingleton(metadata.getContentInfo());
            assertInstanceOf(FeatureCatalogueDescription.class, content);
            final FeatureTypeInfo info = getSingleton(((FeatureCatalogueDescription) content).getFeatureTypeInfo());
            assertEquals(expected, info.getFeatureInstanceCount(), errorMessage);
        }
    }
}
