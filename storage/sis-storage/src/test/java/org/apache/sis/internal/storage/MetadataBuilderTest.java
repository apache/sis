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

import org.opengis.metadata.constraint.Restriction;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.constraint.DefaultLegalConstraints;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.MetadataAssert.*;
import static org.apache.sis.test.TestUtilities.date;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests {@link MetadataBuilder}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
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
        final DefaultLegalConstraints constraints = (DefaultLegalConstraints) getSingleton(getSingleton(
                builder.result().getIdentificationInfo()).getResourceConstraints());

        assertEquals("useConstraints", Restriction.COPYRIGHT, getSingleton(constraints.getUseConstraints()));
        final Citation ref = getSingleton(constraints.getReferences());
        assertTitleEquals("reference.title", notice, ref);
        assertPartyNameEquals("reference.citedResponsibleParty", "John Smith", (DefaultCitation) ref);
        assertEquals("date", date("1992-01-01 00:00:00"), getSingleton(ref.getDates()).getDate());
    }
}
