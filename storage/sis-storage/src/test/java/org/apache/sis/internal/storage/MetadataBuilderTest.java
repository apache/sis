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
package org.apache.sis.internal.storage;

import java.util.Map;
import org.opengis.util.GenericName;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.content.ContentInformation;
import org.opengis.metadata.content.FeatureCatalogueDescription;
import org.opengis.metadata.content.FeatureTypeInfo;
import org.opengis.metadata.constraint.LegalConstraints;
import org.opengis.metadata.constraint.Restriction;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.opengis.test.Assert.assertInstanceOf;
import static org.apache.sis.metadata.Assertions.assertTitleEquals;
import static org.apache.sis.metadata.Assertions.assertPartyNameEquals;
import static org.apache.sis.test.TestUtilities.date;
import static org.apache.sis.test.TestUtilities.getSingleton;

// Branch-dependent imports
import org.opengis.feature.FeatureType;


/**
 * Tests {@link MetadataBuilder}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.2
 * @since   0.8
 */
public final class MetadataBuilderTest extends TestCase {
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
        final MetadataBuilder builder = new MetadataBuilder();
        builder.parseLegalNotice(notice);
        final LegalConstraints constraints = (LegalConstraints) getSingleton(getSingleton(
                builder.build().getIdentificationInfo()).getResourceConstraints());

        assertEquals("useConstraints", Restriction.COPYRIGHT, getSingleton(constraints.getUseConstraints()));
        final Citation ref = getSingleton(constraints.getReferences());
        assertTitleEquals("reference.title", notice, ref);
        assertPartyNameEquals("reference.citedResponsibleParty", "John Smith", ref);
        assertEquals("date", date("1992-01-01 00:00:00"), getSingleton(ref.getDates()).getDate());
    }

    /**
     * Tests {@link MetadataBuilder#addFeatureType(FeatureType, long)}.
     *
     * @todo Combine the 4 tests in a single one for leveraging the same {@link DefaultFeatureType} instance?
     *       It would be consistent with {@link #testParseLegalNotice()}, and the error message in those tests
     *       are already quite clear.
     */
    @Test
    public void negative_feature_count_are_ignored() {
        verifyFeatureInstanceCount("Feature count should not be written if it is negative", null, -1);
    }

    /**
     * Tests {@link MetadataBuilder#addFeatureType(FeatureType, long)}.
     */
    @Test
    public void no_overflow_on_feature_count() {
        verifyFeatureInstanceCount("Feature count should be limited to maximum 32bit integer value", Integer.MAX_VALUE, 7_000_000_000L);
    }

    /**
     * Tests {@link MetadataBuilder#addFeatureType(FeatureType, long)}.
     */
    @Test
    public void verify_feature_count_is_written() {
        verifyFeatureInstanceCount("Feature count should be written as is", 42, 42);
    }

    /**
     * Tests {@link MetadataBuilder#addFeatureType(FeatureType, long)}.
     */
    @Test
    public void feature_should_be_ignored_when_count_is_zero() {
        verifyFeatureInstanceCount("Feature should not be written if count is 0", null, 0);
    }

    /**
     * Creates a new simple metadata with a single simple feature type and the given
     * {@linkplain FeatureTypeInfo#getFeatureInstanceCount() feature instance count}.
     * Then, asserts that the value in the built metadata is compliant with a given control value.
     *
     * @param expected       the feature instance count value we want to see in the metadata (control value).
     * @param valueToInsert  the value to send to the metadata builder.
     */
    private static void verifyFeatureInstanceCount(final String errorMessage, final Integer expected, final long valueToInsert) {
        final var dataType = new DefaultFeatureType(Map.of(DefaultFeatureType.NAME_KEY, "Test type"), false, null);
        final var builder  = new MetadataBuilder();
        final GenericName name = builder.addFeatureType(dataType, valueToInsert);
        assertNotNull(name);

        final DefaultMetadata metadata = builder.build();
        if (valueToInsert == 0) {
            assertTrue(metadata.getContentInfo().isEmpty());
        } else {
            final ContentInformation content = getSingleton(metadata.getContentInfo());
            assertInstanceOf("Metadata.contentInfo", FeatureCatalogueDescription.class, content);
            final FeatureTypeInfo info = getSingleton(((FeatureCatalogueDescription) content).getFeatureTypeInfo());
            assertEquals(errorMessage, expected, info.getFeatureInstanceCount());
        }
    }
}
