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

import java.util.Set;
import java.util.List;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.citation.PresentationForm;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTableFormat;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.metadata.iso.content.DefaultBand;
import org.apache.sis.metadata.iso.content.DefaultImageDescription;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultCitationTest;
import org.apache.sis.metadata.iso.citation.DefaultResponsibility;
import org.apache.sis.metadata.iso.content.DefaultAttributeGroup;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.metadata.iso.lineage.DefaultProcessing;
import org.apache.sis.measure.Units;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assertions.assertMultilinesEquals;


/**
 * Tests the {@link TreeTableFormat} applied to the formatting of metadata tree.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.3
 */
@DependsOn(TreeTableViewTest.class)
public final class TreeTableFormatTest extends TestCase {
    /**
     * The formatter to use.
     */
    private final TreeTableFormat format;

    /**
     * Creates a new test case.
     */
    public TreeTableFormatTest() {
        format = new TreeTableFormat(null, null);
        format.setColumns(TableColumn.NAME, TableColumn.VALUE);
    }

    /**
     * Creates a band for the given minimum and maximum wavelengths, in centimetres.
     */
    private static DefaultBand createBand(final double min, final double max) {
        final DefaultBand band = new DefaultBand();
        band.setMinValue(min);
        band.setMaxValue(max);
        band.setUnits(Units.CENTIMETRE);
        return band;
    }

    /**
     * Tests the formatting of a {@link DefaultCitation} object.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-298">SIS-298</a>
     */
    @Test
    public void testCitation() {
        final DefaultCitation citation = DefaultCitationTest.create();
        final String text = format.format(citation.asTreeTable());
        assertMultilinesEquals(
            "Citation……………………………………………………………………………… Undercurrent\n" +
            "  ├─Alternate title………………………………………………… Andākarento\n" +
            "  ├─Cited responsible party (1 of 2)\n" +
            "  │   ├─Role…………………………………………………………………… Author\n" +
            "  │   └─Individual…………………………………………………… Testsuya Toyoda\n" +
            "  ├─Cited responsible party (2 of 2)\n" +
            "  │   ├─Role…………………………………………………………………… Editor\n" +
            "  │   ├─Extent……………………………………………………………… World\n" +
            "  │   │   └─Geographic element\n" +
            "  │   │       ├─West bound longitude…… 180°W\n" +
            "  │   │       ├─East bound longitude…… 180°E\n" +
            "  │   │       ├─South bound latitude…… 90°S\n" +
            "  │   │       ├─North bound latitude…… 90°N\n" +
            "  │   │       └─Extent type code……………… True\n" +
            "  │   └─Organisation……………………………………………… Kōdansha\n" +
            "  ├─Presentation form (1 of 2)…………………… Document digital\n" +
            "  ├─Presentation form (2 of 2)…………………… Document hardcopy\n" +
            "  └─ISBN……………………………………………………………………………… 9782505004509\n", text);
    }

    /**
     * Tests the formatting of a {@link DefaultProcessing} object.
     */
    @Test
    public void testProcessing() {
        final DefaultCitation   titled = new DefaultCitation("Some specification");
        final DefaultCitation    coded = new DefaultCitation();
        final DefaultCitation untitled = new DefaultCitation();
        titled  .setPresentationForms(Set.of(PresentationForm.DOCUMENT_HARDCOPY));
        coded   .setPresentationForms(Set.of(PresentationForm.IMAGE_HARDCOPY));
        untitled.setCitedResponsibleParties(Set.of(new DefaultResponsibility(Role.AUTHOR, null, null)));
        final DefaultProcessing processing = new DefaultProcessing();
        processing.setDocumentations(List.of(titled, coded, untitled));
        final String text = format.format(processing.asTreeTable());
        assertMultilinesEquals(
            "Processing\n" +
            "  ├─Documentation (1 of 3)…………… Some specification\n" +
            "  │   └─Presentation form……………… Document hardcopy\n" +
            "  ├─Documentation (2 of 3)\n" +
            "  │   └─Presentation form……………… Image hardcopy\n" +
            "  └─Documentation (3 of 3)\n" +
            "      └─Cited responsible party\n" +
            "          └─Role……………………………………… Author\n", text);
    }

    /**
     * Tests the formatting of a {@link DefaultImageDescription} object.
     */
    @Test
    public void testImageDescription() {
        final DefaultImageDescription image = new DefaultImageDescription();
        image.setAttributeGroups(List.of(
            new DefaultAttributeGroup(null, createBand(0.25, 0.26)),
            new DefaultAttributeGroup(null, createBand(0.28, 0.29))
        ));
        final String text = format.format(image.asTreeTable());
        assertMultilinesEquals(
            "Image description\n" +
            "  ├─Attribute group (1 of 2)\n" +
            "  │   └─Attribute\n" +
            "  │       ├─Max value………………… 0.26\n" +
            "  │       ├─Min value………………… 0.25\n" +
            "  │       └─Units…………………………… centimetre\n" +
            "  └─Attribute group (2 of 2)\n" +
            "      └─Attribute\n" +
            "          ├─Max value………………… 0.29\n" +
            "          ├─Min value………………… 0.28\n" +
            "          └─Units…………………………… centimetre\n", text);
    }

    /**
     * Tests the formatting of a {@link DefaultDataIdentification} object with custom code list elements
     */
    @Test
    public void testTreeWithCustomElements() {
        final DefaultCitation citation = new DefaultCitation();
        citation.setAlternateTitles(List.of(
                new SimpleInternationalString("Apple"),
                new SimpleInternationalString("Orange"),
                new SimpleInternationalString("Kiwi")));

        citation.setPresentationForms(List.of(
                PresentationForm.IMAGE_DIGITAL,
                PresentationForm.valueOf("AUDIO_DIGITAL"),  // Existing form
                PresentationForm.valueOf("test")));         // Custom form

        final String text = format.format(citation.asTreeTable());
        assertMultilinesEquals(
            "Citation\n" +
            "  ├─Alternate title (1 of 3)………… Apple\n" +
            "  ├─Alternate title (2 of 3)………… Orange\n" +
            "  ├─Alternate title (3 of 3)………… Kiwi\n" +
            "  ├─Presentation form (1 of 3)…… Image digital\n" +
            "  ├─Presentation form (2 of 3)…… Audio digital\n" +
            "  └─Presentation form (3 of 3)…… Test\n",
            text);
    }
}
