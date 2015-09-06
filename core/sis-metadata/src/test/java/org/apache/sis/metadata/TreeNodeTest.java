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
import org.apache.sis.metadata.iso.citation.DefaultOrganisation;
import org.apache.sis.metadata.iso.citation.DefaultIndividual;
import org.apache.sis.metadata.iso.citation.AbstractParty;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.MetadataAssert.*;
import static java.util.Collections.singleton;


/**
 * Tests the {@link TreeNode} class.
 * Unless otherwise specified, all tests use the {@link MetadataStandard#ISO_19115} constant.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
@DependsOn(TreeNodeChildrenTest.class)
public final strictfp class TreeNodeTest extends TestCase {
    /**
     * Creates a metadata hierarchy to be used for the tests.
     * This method creates the following metadata:
     *
     * {@preformat text
     *   Citation
     *     ├─Title…………………………………………………………………………………………… Some title
     *     ├─Alternate title (1 of 2)………………………………………… First alternate title
     *     ├─Alternate title (2 of 2)………………………………………… Second alternate title
     *     ├─Edition……………………………………………………………………………………… Some edition
     *     ├─Cited responsible party (1 of 2)
     *     │   └─Party (Organisation)
     *     │      ├─Name…………………………………………………………………………… Some organisation
     *     │      └─Role…………………………………………………………………………… Distributor
     *     ├─Cited responsible party (2 of 2)
     *     │   └─Party (Individual)
     *     │      ├─Name…………………………………………………………………………… Some person of contact
     *     │      ├─Contact info
     *     │      │   └─Address
     *     │      │       └─Electronic mail address…… Some email
     *     │      └─Role…………………………………………………………………………… Point of contact
     *     ├─Presentation form (1 of 2)…………………………………… Map digital
     *     ├─Presentation form (2 of 2)…………………………………… map hardcopy
     *     └─Other citation details……………………………………………… Some other details
     * }
     */
    static DefaultCitation metadataWithHierarchy() {
        final DefaultCitation citation = TreeNodeChildrenTest.metadataWithMultiOccurrences();
        AbstractParty party = new DefaultOrganisation("Some organisation", null, null, null);
        DefaultResponsibleParty responsibility = new DefaultResponsibleParty(Role.DISTRIBUTOR);
        responsibility.setParties(singleton(party));
        assertTrue(citation.getCitedResponsibleParties().add(responsibility));

        // Add a second responsible party with deeper hierarchy.
        final DefaultContact contact = new DefaultContact();
        final DefaultAddress address = new DefaultAddress();
        address.setElectronicMailAddresses(singleton("Some email"));
        contact.setAddresses(singleton(address));
        party = new DefaultIndividual("Some person of contact", null, contact);
        responsibility = new DefaultResponsibleParty(Role.POINT_OF_CONTACT);
        responsibility.setParties(singleton(party));
        assertTrue(citation.getCitedResponsibleParties().add(responsibility));
        return citation;
    }

    /**
     * Creates a node to be tested for the given metadata object and value policy.
     */
    private static TreeNode create(final AbstractMetadata metadata, final ValueExistencePolicy valuePolicy) {
        final MetadataStandard  standard = MetadataStandard.ISO_19115;
        final TreeTableView table = new TreeTableView(standard, metadata, valuePolicy);
        return (TreeNode) table.getRoot();
    }

    /**
     * Tests the properties of the root node.
     */
    @Test
    public void testRootNode() {
        final DefaultCitation citation = TreeNodeChildrenTest.metadataWithoutCollections();
        final TreeNode node = create(citation, ValueExistencePolicy.NON_EMPTY);
        assertEquals("getName()",        "Citation",     node.getName());
        assertEquals("getIdentifier()",  "CI_Citation",  node.getIdentifier());
        assertEquals("getElementType()", Citation.class, node.getElementType());
        assertSame  ("getUserObject()",  citation,       node.getUserObject());
        assertFalse ("isWritable()",                     node.isWritable());
        assertNull  ("getParent()",                      node.getParent());
        assertFalse ("isLeaf()",                         node.isLeaf());

        final TreeNodeChildren children = (TreeNodeChildren) node.getChildren();
        assertSame ("children.metadata", citation, children.metadata);
        assertFalse("children.isEmpty()", node.getChildren().isEmpty());
        assertSame ("children.parent", node, children.iterator().next().getParent());
    }

    /**
     * Tests {@link TreeNode#getName()} on a metadata with only one entry in collections.
     * Those names shall <em>not</em> contain numbering like <cite>"(1 of 2)"</cite>.
     */
    @Test
    @DependsOnMethod("testRootNode") // Because tested more basic methods than 'getValue(TableColumn)'.
    public void testGetNameForSingleton() {
        final DefaultCitation citation = TreeNodeChildrenTest.metadataWithSingletonInCollections();
        assertColumnContentEquals(create(citation, ValueExistencePolicy.NON_EMPTY), TableColumn.NAME,
            "Citation",
              "Title",
              "Alternate title",
              "Edition",
              "Presentation form",
              "Other citation details");
    }

    /**
     * Tests {@link TreeNode#getName()} on a metadata with more than one entry in collections.
     * Those names <em>shall</em> contain numbering like <cite>"(1 of 2)"</cite>.
     */
    @Test
    @DependsOnMethod("testGetNameForSingleton")
    public void testGetNameForMultiOccurrences() {
        final DefaultCitation citation = TreeNodeChildrenTest.metadataWithMultiOccurrences();
        assertColumnContentEquals(create(citation, ValueExistencePolicy.NON_EMPTY), TableColumn.NAME,
            "Citation",
              "Title",
              "Alternate title (1 of 2)",
              "Alternate title (2 of 2)",
              "Edition",
              "Presentation form (1 of 2)",
              "Presentation form (2 of 2)",
              "Other citation details");
    }

    /**
     * Tests {@link TreeNode#getName()} on a metadata with a deeper hierarchy.
     */
    @Test
    @DependsOnMethod("testGetNameForMultiOccurrences")
    public void testGetNameForHierarchy() {
        final DefaultCitation citation = metadataWithHierarchy();
        assertColumnContentEquals(create(citation, ValueExistencePolicy.NON_EMPTY), TableColumn.NAME,
            "Citation",
              "Title",
              "Alternate title (1 of 2)",
              "Alternate title (2 of 2)",
              "Edition",
              "Cited responsible party (1 of 2)",
                "Role",
                "Party",
                  "Name",
              "Cited responsible party (2 of 2)",
                "Role",
                "Party",
                  "Name",
                  "Contact info",
                    "Address",
                      "Electronic mail address",
              "Presentation form (1 of 2)",
              "Presentation form (2 of 2)",
              "Other citation details");
    }

    /**
     * Tests {@link TreeNode#getIdentifier()} on a metadata with a hierarchy.
     * Those names shall <em>not</em> contain numbering like <cite>"(1 of 2)"</cite>, even if the same
     * identifiers are repeated. Those identifiers are not intended to be unique in a list of children.
     * The repetition of the same identifier means that they shall be part of a collection.
     */
    @Test
    @DependsOnMethod("testGetNameForMultiOccurrences") // Because similar to names, which were tested progressively.
    public void testGetIdentifier() {
        final DefaultCitation citation = metadataWithHierarchy();
        assertColumnContentEquals(create(citation, ValueExistencePolicy.NON_EMPTY), TableColumn.IDENTIFIER,
            "CI_Citation",
              "title",
              "alternateTitle",
              "alternateTitle",
              "edition",
              "citedResponsibleParty",
                "role",
                "party",
                  "name",
              "citedResponsibleParty",
                "role",
                "party",
                  "name",
                  "contactInfo",
                    "address",
                      "electronicMailAddress",
              "presentationForm",
              "presentationForm",
              "otherCitationDetails");
    }

    /**
     * Tests {@link TreeNode#getIndex()} on a metadata with a hierarchy.
     */
    @Test
    @DependsOnMethod("testGetIdentifier")
    public void testGetIndex() {
        final Integer ZERO = 0;
        final Integer ONE  = 1;
        final DefaultCitation citation = metadataWithHierarchy();
        assertColumnContentEquals(create(citation, ValueExistencePolicy.NON_EMPTY), TableColumn.INDEX,
            null,           // CI_Citation
              null,         // title
              ZERO,         // alternateTitle
              ONE,          // alternateTitle
              null,         // edition
              ZERO,         // citedResponsibleParty
                null,       // role
                ZERO,       // party (organisation)
                  null,     // name
              ONE,          // citedResponsibleParty
                null,       // role
                ZERO,       // party (individual)
                  null,     // name
                  ZERO,     // contactInfo
                    ZERO,   // address
                      ZERO, // electronicMailAddress
              ZERO,         // presentationForm
              ONE,          // presentationForm
              null);        // otherCitationDetails
    }

    /**
     * Tests {@link TreeNode#getElementType()} on a metadata with a hierarchy.
     */
    @Test
    @DependsOnMethod("testGetIdentifier") // Because if identifiers are wrong, we are looking at wrong properties.
    public void testGetElementType() {
        final DefaultCitation citation = metadataWithHierarchy();
        assertColumnContentEquals(create(citation, ValueExistencePolicy.NON_EMPTY), TableColumn.TYPE,
            Citation.class,
              InternationalString.class,
              InternationalString.class,
              InternationalString.class,
              InternationalString.class,
              ResponsibleParty.class,
                Role.class,
                AbstractParty.class,
                  InternationalString.class,
              ResponsibleParty.class,
                Role.class,
                AbstractParty.class,
                  InternationalString.class,
                  Contact.class,
                    Address.class,
                      String.class,
              PresentationForm.class,
              PresentationForm.class,
              InternationalString.class);
    }

    /**
     * Tests {@link TreeNode#getValue(TableColumn)} for the value column.
     */
    @Test
    @DependsOnMethod("testGetIdentifier") // Because if identifiers are wrong, we are looking at wrong properties.
    public void testGetValue() {
        final DefaultCitation citation = metadataWithHierarchy();
        assertColumnContentEquals(create(citation, ValueExistencePolicy.NON_EMPTY), TableColumn.VALUE,
            null, // Citation
              "Some title",
              "First alternate title",
              "Second alternate title",
              "Some edition",
              null, // ResponsibleParty
                Role.DISTRIBUTOR,
                null, // Party (organisation)
                  "Some organisation",
              null, // ResponsibleParty
                Role.POINT_OF_CONTACT,
                null, // Party (individual)
                  "Some person of contact",
                  null, // Contact
                    null, // Address
                      "Some email",
              PresentationForm.MAP_DIGITAL,
              PresentationForm.MAP_HARDCOPY,
              "Some other details");
    }

    /**
     * Tests {@link TreeNode#newChild()}.
     */
    @Test
    @DependsOnMethod("testGetValue")
    public void testNewChild() {
        final DefaultCitation citation = metadataWithHierarchy();
        final TreeNode node = create(citation, ValueExistencePolicy.NON_EMPTY);
        /*
         * Ensure that we can not overwrite existing nodes.
         */
        TreeTable.Node child = node.newChild();
        child.setValue(TableColumn.IDENTIFIER, "title");
        try {
            child.setValue(TableColumn.VALUE, "A new title");
            fail("Attemps to overwrite an existing value shall fail.");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("title"));
        }
        /*
         * Clear the title and try again. This time, it shall work.
         */
        citation.setTitle(null);
        child = node.newChild();
        child.setValue(TableColumn.IDENTIFIER, "title");
        child.setValue(TableColumn.VALUE, "A new title");
        assertTitleEquals("citation", "A new title", citation);
        assertSame(citation.getTitle(), child.getValue(TableColumn.VALUE));
        /*
         * Try adding a new element in a collection.
         * Note that the code below imply a conversion from String to InternationalString.
         */
        child = node.newChild();
        child.setValue(TableColumn.IDENTIFIER, "alternateTitle");
        child.setValue(TableColumn.VALUE, "Third alternate title");
        assertEquals(3, citation.getAlternateTitles().size());
        assertEquals("Third alternate title", child.getValue(TableColumn.VALUE).toString());
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
    private static void assertColumnContentEquals(final TreeNode node,
            final TableColumn<?> column, final Object... values)
    {
        assertEquals("Missing values in the tested metadata.", values.length,
                assertColumnContentEquals(node, column, values, 0));
    }

    /**
     * Implementation of the above {@code assertGetterReturns}, to be invoked recursively.
     */
    private static int assertColumnContentEquals(final TreeTable.Node node, final TableColumn<?> column,
            final Object[] values, int index)
    {
        final Object actual = node.getValue(column);
        Object unlocalized = actual;
        if (unlocalized instanceof InternationalString) {
            unlocalized = ((InternationalString) unlocalized).toString(Locale.ROOT);
        }
        assertEquals("values[" + index + ']', values[index++], unlocalized);
        for (final TreeTable.Node child : node.getChildren()) {
            index = assertColumnContentEquals(child, column, values, index);
        }
        assertSame("Value shall be stable.", actual, node.getValue(column));
        return index;
    }
}
