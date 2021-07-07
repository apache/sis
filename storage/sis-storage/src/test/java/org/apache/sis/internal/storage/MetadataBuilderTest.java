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

import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.opengis.feature.FeatureType;
import org.opengis.metadata.constraint.LegalConstraints;
import org.opengis.metadata.constraint.Restriction;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.test.TestCase;
import org.junit.Test;
import org.opengis.metadata.content.FeatureCatalogueDescription;
import org.opengis.metadata.content.FeatureTypeInfo;
import org.opengis.util.GenericName;

import static org.apache.sis.test.MetadataAssert.*;
import static org.apache.sis.test.TestUtilities.date;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests {@link MetadataBuilder}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public final strictfp class MetadataBuilderTest extends TestCase {
    /**
     * Tests {@link MetadataBuilder#parseLegalNotice(String)}.
     * The expected result of this parsing is:
     *
     * {@preformat text
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
     *                     └─Role……………………………………… Owner
     * }
     */
    @Test
    public void testParseLegalNotice() {
        verifyCopyrightParsing("Copyright (C), John Smith, 1992. All rights reserved.");
        verifyCopyrightParsing("(C) 1992, John Smith. All rights reserved.");
        verifyCopyrightParsing("(C) COPYRIGHT 1992 John Smith.");
    }

    @Test
    public void negative_feature_count_are_ignored() {
        verifyFeatureInstanceCount("Feature count should not be written if it is negative", null, -1);
    }

    @Test
    public void no_overflow_on_feature_count() {
        verifyFeatureInstanceCount("Feature count should be limited to maximum 32bit integer value", Integer.MAX_VALUE, 7_000_000_000L);
    }

    @Test
    public void verify_feature_count_is_written() {
        verifyFeatureInstanceCount("Feature count should be written as is", 42, 42);
    }

    @Test
    public void feature_count_should_be_ignored_when_it_is_zero() {
        verifyFeatureInstanceCount("Feature count should not be written if it is 0", null, 0);
    }

    /**
     * Create a new simple metadata with a single simple feature type and the given
     * {@link FeatureTypeInfo#getFeatureInstanceCount() feature instance count}. Then, assert that the value in the
     * built metadata is compliant with a given control value.
     *
     * @param expected The feature instance count value we want to see in the metadata (control value)
     * @param valueToInsert The value to send to the metadata builder.
     *
     * @see MetadataBuilder#addFeatureType(FeatureType, long)
     */
    private static void verifyFeatureInstanceCount(final String errorMessage, final Integer expected, final long valueToInsert) {
        final FeatureType dataType = new FeatureTypeBuilder()
                .setName("Test type")
                .build();
        final MetadataBuilder builder = new MetadataBuilder();
        final GenericName name = builder.addFeatureType(dataType, valueToInsert);
        assertNotNull(name);

        final DefaultMetadata metadata = builder.build(true);
        final FeatureTypeInfo info = metadata.getContentInfo().stream()
                .filter(it -> it instanceof FeatureCatalogueDescription)
                .flatMap(it -> ((FeatureCatalogueDescription) it).getFeatureTypeInfo().stream())
                .reduce((v1, v2) -> { throw new AssertionError("A single feature type info is expected"); })
                .orElseThrow(() -> new AssertionError("A single feature type info is expected"));

        assertEquals(errorMessage, expected, info.getFeatureInstanceCount());
    }

    /**
     * Verifies the metadata that contains the result of parsing a copyright statement.
     * Should contains the "John Smith" name and 1992 year.
     *
     * @param notice  the copyright statement to parse.
     */
    private static void verifyCopyrightParsing(final String notice) {
        final MetadataBuilder builder = new MetadataBuilder();
        builder.parseLegalNotice(notice);
        final LegalConstraints constraints = (LegalConstraints) getSingleton(getSingleton(
                builder.build(false).getIdentificationInfo()).getResourceConstraints());

        assertEquals("useConstraints", Restriction.COPYRIGHT, getSingleton(constraints.getUseConstraints()));
        final Citation ref = getSingleton(constraints.getReferences());
        assertTitleEquals("reference.title", notice, ref);
        assertPartyNameEquals("reference.citedResponsibleParty", "John Smith", ref);
        assertEquals("date", date("1992-01-01 00:00:00"), getSingleton(ref.getDates()).getDate());
    }
}
