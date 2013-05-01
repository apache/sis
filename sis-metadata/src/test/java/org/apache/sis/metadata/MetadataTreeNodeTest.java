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

import java.util.Locale;
import org.opengis.metadata.citation.Address;
import org.opengis.metadata.citation.Contact;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.metadata.citation.PresentationForm;
import org.opengis.metadata.citation.Role;
import org.opengis.util.InternationalString;
import org.apache.sis.metadata.iso.citation.DefaultAddress;
import org.apache.sis.metadata.iso.citation.DefaultContact;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultResponsibleParty;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link MetadataTreeNode} class.
 * Unless otherwise specified, all tests use the {@link MetadataStandard#ISO_19115} constant.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@DependsOn(MetadataTreeChildrenTest.class)
public final strictfp class MetadataTreeNodeTest extends TestCase {
    /**
     * Creates a metadata hierarchy to be used for the tests.
     * This method creates the following metadata:
     *
     * {@preformat text
     *   DefaultCitation
     *     ├─Title…………………………………………………………………………………… Some title
     *     ├─Alternate title (1 of 2)………………………………… First alternate title
     *     ├─Alternate title (2 of 2)………………………………… Second alternate title
     *     ├─Edition……………………………………………………………………………… Some edition
     *     ├─Cited responsible party (1 of 2)
     *     │   ├─Organisation name………………………………………… Some organisation
     *     │   └─Role…………………………………………………………………………… Distributor
     *     ├─Cited responsible party (2 of 2)
     *     │   ├─Individual name……………………………………………… Some person of contact
     *     │   ├─Contact info
     *     │   │   └─Address
     *     │   │       └─Electronic mail address…… Some email
     *     │   └─Role…………………………………………………………………………… Point of contact
     *     ├─Presentation form (1 of 2)…………………………… Map digital
     *     ├─Presentation form (2 of 2)…………………………… map hardcopy
     *     └─Other citation details……………………………………… Some other details
     * }
     */
    static DefaultCitation metadataWithHierarchy() {
        final DefaultCitation citation = MetadataTreeChildrenTest.metadataWithMultiOccurrences();
        DefaultResponsibleParty party = new DefaultResponsibleParty(Role.DISTRIBUTOR);
        party.setOrganisationName(new SimpleInternationalString("Some organisation"));
        citation.getCitedResponsibleParties().add(party);

        // Add a second responsible party with deeper hierarchy.
        party = new DefaultResponsibleParty(Role.POINT_OF_CONTACT);
        party.setIndividualName("Some person of contact");
        final DefaultContact contact = new DefaultContact();
        final DefaultAddress address = new DefaultAddress();
        address.getElectronicMailAddresses().add("Some email");
        contact.setAddress(address);
        party.setContactInfo(contact);
        citation.getCitedResponsibleParties().add(party);
        return citation;
    }

    /**
     * Creates a node to be tested for the given metadata object and value policy.
     */
    private static MetadataTreeNode create(final AbstractMetadata metadata, final ValueExistencePolicy valuePolicy) {
        final MetadataStandard  standard = MetadataStandard.ISO_19115;
        final MetadataTreeTable table    = new MetadataTreeTable(standard, metadata, valuePolicy);
        return (MetadataTreeNode) table.getRoot();
    }

    /**
     * Tests the properties of the root node.
     */
    @Test
    public void testRootNode() {
        final DefaultCitation citation = MetadataTreeChildrenTest.metadataWithoutCollections();
        final MetadataTreeNode node = create(citation, ValueExistencePolicy.NON_EMPTY);
        assertEquals("getName()",        "DefaultCitation", node.getName());
        assertEquals("getIdentifier()",  "CI_Citation",     node.getIdentifier());
        assertEquals("getElementType()", Citation.class,    node.getElementType());
        assertSame  ("getUserObject()",  citation,          node.getUserObject());
        assertFalse ("isWritable()",                        node.isWritable());
        assertNull  ("getParent()",                         node.getParent());
        assertFalse ("isLeaf()",                            node.isLeaf());

        final MetadataTreeChildren children = (MetadataTreeChildren) node.getChildren();
        assertSame ("children.metadata", citation, children.metadata);
        assertFalse("children.isEmpty()", node.getChildren().isEmpty());
        assertSame ("children.parent", node, children.iterator().next().getParent());
    }

    /**
     * Tests {@link MetadataTreeNode#getName()} on a metadata with only one entry in collections.
     * Those names shall <em>not</em> contain numbering like "<cite>(1 of 2)</cite>".
     */
    @Test
    @DependsOnMethod("testRootNode") // Because tested more basic methods than 'getValue(TableColumn)'.
    public void testGetNameForSingleton() {
        final DefaultCitation citation = MetadataTreeChildrenTest.metadataWithSingletonInCollections();
        assertColumnEquals(create(citation, ValueExistencePolicy.NON_EMPTY), TableColumn.NAME,
            "DefaultCitation",
              "Title",
              "Alternate title",
              "Edition",
              "Presentation form",
              "Other citation details");
    }

    /**
     * Tests {@link MetadataTreeNode#getName()} on a metadata with more than one entry in collections.
     * Those names <em>shall</em> contain numbering like "<cite>(1 of 2)</cite>".
     */
    @Test
    @DependsOnMethod("testGetNameForSingleton")
    public void testGetNameForMultiOccurrences() {
        final DefaultCitation citation = MetadataTreeChildrenTest.metadataWithMultiOccurrences();
        assertColumnEquals(create(citation, ValueExistencePolicy.NON_EMPTY), TableColumn.NAME,
            "DefaultCitation",
              "Title",
              "Alternate title (1 of 2)",
              "Alternate title (2 of 2)",
              "Edition",
              "Presentation form (1 of 2)",
              "Presentation form (2 of 2)",
              "Other citation details");
    }

    /**
     * Tests {@link MetadataTreeNode#getName()} on a metadata with a deeper hierarchy.
     */
    @Test
    @DependsOnMethod("testGetNameForMultiOccurrences")
    public void testGetNameForHierarchy() {
        final DefaultCitation citation = metadataWithHierarchy();
        assertColumnEquals(create(citation, ValueExistencePolicy.NON_EMPTY), TableColumn.NAME,
            "DefaultCitation",
              "Title",
              "Alternate title (1 of 2)",
              "Alternate title (2 of 2)",
              "Edition",
              "Cited responsible party (1 of 2)",
                "Organisation name",
                "Role",
              "Cited responsible party (2 of 2)",
                "Individual name",
                "Contact info",
                  "Address",
                    "Electronic mail address",
                "Role",
              "Presentation form (1 of 2)",
              "Presentation form (2 of 2)",
              "Other citation details");
    }

    /**
     * Tests {@link MetadataTreeNode#getIdentifier()} on a metadata with a hierarchy.
     * Those names shall <em>not</em> contain numbering like "<cite>(1 of 2)</cite>", even if the same
     * identifiers are repeated. Those identifiers are not intended to be unique in a list of children.
     * The repetition of the same identifier means that they shall be part of a collection.
     */
    @Test
    @DependsOnMethod("testGetNameForMultiOccurrences") // Because similar to names, which were tested progressively.
    public void testGetIdentifier() {
        final DefaultCitation citation = metadataWithHierarchy();
        assertColumnEquals(create(citation, ValueExistencePolicy.NON_EMPTY), TableColumn.IDENTIFIER,
            "CI_Citation",
              "title",
              "alternateTitle",
              "alternateTitle",
              "edition",
              "citedResponsibleParty",
                "organisationName",
                "role",
              "citedResponsibleParty",
                "individualName",
                "contactInfo",
                  "address",
                    "electronicMailAddress",
                "role",
              "presentationForm",
              "presentationForm",
              "otherCitationDetails");
    }

    /**
     * Tests {@link MetadataTreeNode#getElementType()} on a metadata with a hierarchy.
     */
    @Test
    @DependsOnMethod("testGetIdentifier") // Because if identifiers are wrong, we are looking at wrong properties.
    public void testGetElementType() {
        final DefaultCitation citation = metadataWithHierarchy();
        assertColumnEquals(create(citation, ValueExistencePolicy.NON_EMPTY), TableColumn.TYPE,
            Citation.class,
              InternationalString.class,
              InternationalString.class,
              InternationalString.class,
              InternationalString.class,
              ResponsibleParty.class,
                InternationalString.class,
                Role.class,
              ResponsibleParty.class,
                String.class,
                Contact.class,
                  Address.class,
                    String.class,
                Role.class,
              PresentationForm.class,
              PresentationForm.class,
              InternationalString.class);
    }

    /**
     * Tests {@link MetadataTreeNode#getValue(TableColumn)} for the value column.
     */
    @Test
    @DependsOnMethod("testGetIdentifier") // Because if identifiers are wrong, we are looking at wrong properties.
    public void testGetValue() {
        final DefaultCitation citation = metadataWithHierarchy();
        assertColumnEquals(create(citation, ValueExistencePolicy.NON_EMPTY), TableColumn.VALUE,
            null, // Citation
              "Some title",
              "First alternate title",
              "Second alternate title",
              "Some edition",
              null, // ResponsibleParty
                "Some organisation",
                Role.DISTRIBUTOR,
              null, // ResponsibleParty
                "Some person of contact",
                null, // Contact
                  null, // Address
                    "Some email",
                Role.POINT_OF_CONTACT,
              PresentationForm.MAP_DIGITAL,
              PresentationForm.MAP_HARDCOPY,
              "Some other details");
    }

    /**
     * Compares the result of the given getter method invoked on the given node, then invoked
     * on all children of that given. In the particular case of the {@link #NAME} method,
     * international strings are replaced by unlocalized strings before comparisons.
     *
     * @param node     The node for which to test the children.
     * @param column   The column from which to get a value.
     * @param values   The expected values. The first value is the result of the getter method
     *                 applied on the given node, and all other values are the result of the
     *                 getter method applied on the children, in iteration order.
     */
    private static void assertColumnEquals(final MetadataTreeNode node,
            final TableColumn<?> column, final Object... values)
    {
        assertEquals("Missing values in the tested metadata.", values.length,
                assertColumnEquals(node, column, values, 0));
    }

    /**
     * Implementation of the above {@code assertGetterReturns}, to be invoked recursively.
     */
    private static int assertColumnEquals(final TreeTable.Node node, final TableColumn<?> column,
            final Object[] values, int index)
    {
        Object actual = node.getValue(column);
        if (actual instanceof InternationalString) {
            actual = ((InternationalString) actual).toString(Locale.ROOT);
        }
        assertEquals("values[" + index + ']', values[index++], actual);
        for (final TreeTable.Node child : node.getChildren()) {
            index = assertColumnEquals(child, column, values, index);
        }
        return index;
    }
}
