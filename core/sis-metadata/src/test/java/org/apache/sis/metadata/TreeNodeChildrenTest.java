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

import java.util.Random;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import org.opengis.metadata.citation.PresentationForm;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.util.collection.DefaultTreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.TestUtilities.createRandomNumberGenerator;


/**
 * Tests the {@link TreeNodeChildren} class.
 * Unless otherwise specified, all tests use the {@link MetadataStandard#ISO_19115} constant.
 *
 * <div class="section">Test dependency</div>
 * This class uses the {@link TreeNode#getUserObject()} method for comparing the values.
 * We can hardly avoid to use some {@code TreeNode} methods because of the cross-dependencies.
 * However we try to use nothing else than {@code getUserObject()} because the purpose of this
 * class is not to test {@link TreeNode}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
@DependsOn(PropertyAccessorTest.class)
public final strictfp class TreeNodeChildrenTest extends TestCase {
    /**
     * Creates a shallow metadata object without collections.
     *
     * {@preformat text
     *   DefaultCitation
     *     ├─Title………………………………………………… Some title
     *     ├─Edition…………………………………………… Some edition
     *     └─Other citation details…… Some other details
     * }
     */
    static DefaultCitation metadataWithoutCollections() {
        final DefaultCitation citation = new DefaultCitation("Some title");
        citation.setEdition(new SimpleInternationalString("Some edition"));
        citation.setOtherCitationDetails(new SimpleInternationalString("Some other details"));
        return citation;
    }

    /**
     * Creates a shallow metadata object with singleton value in collections.
     * This method creates the following metadata:
     *
     * {@preformat text
     *   DefaultCitation
     *     ├─Title………………………………………………… Some title
     *     ├─Alternate title……………………… First alternate title
     *     ├─Edition…………………………………………… Some edition
     *     ├─Presentation form………………… Map digital
     *     └─Other citation details…… Some other details
     * }
     */
    static DefaultCitation metadataWithSingletonInCollections() {
        final DefaultCitation citation = metadataWithoutCollections();
        assertTrue(citation.getAlternateTitles().add(new SimpleInternationalString("First alternate title")));
        assertTrue(citation.getPresentationForms().add(PresentationForm.MAP_DIGITAL));
        return citation;
    }

    /**
     * Creates a shallow metadata object with multi-occurrences (i.e. more than one value in collections).
     * This method creates the following metadata:
     *
     * {@preformat text
     *   DefaultCitation
     *     ├─Title………………………………………………………… Some title
     *     ├─Alternate title (1 of 2)……… First alternate title
     *     ├─Alternate title (2 of 2)……… Second alternate title
     *     ├─Edition…………………………………………………… Some edition
     *     ├─Presentation form (1 of 2)… Map digital
     *     ├─Presentation form (2 of 2)… map hardcopy
     *     └─Other citation details…………… Some other details
     * }
     */
    static DefaultCitation metadataWithMultiOccurrences() {
        final DefaultCitation citation = metadataWithSingletonInCollections();
        assertTrue(citation.getAlternateTitles().add(new SimpleInternationalString("Second alternate title")));
        assertTrue(citation.getPresentationForms().add(PresentationForm.MAP_HARDCOPY));
        return citation;
    }

    /**
     * Creates a collection to be tested for the given metadata object and value policy.
     */
    private static TreeNodeChildren create(final AbstractMetadata metadata, final ValueExistencePolicy valuePolicy) {
        final MetadataStandard standard = MetadataStandard.ISO_19115;
        final TreeTableView    table    = new TreeTableView(standard, metadata, valuePolicy);
        final TreeNode         node     = (TreeNode) table.getRoot();
        final PropertyAccessor accessor = standard.getAccessor(metadata.getClass(), true);
        return new TreeNodeChildren(node, metadata, accessor);
    }

    /**
     * Tests read-only operations on a list of properties for a shallow metadata object without collections.
     */
    @Test
    public void testReadOnlyWithoutCollections() {
        final DefaultCitation  citation = metadataWithoutCollections();
        final TreeNodeChildren children = create(citation, ValueExistencePolicy.NON_EMPTY);
        final String[] expected = {
            "Some title",
            "Some edition",
            "Some other details"
        };
        assertFalse ("isEmpty()", children.isEmpty());
        assertEquals("size()", expected.length, children.size());
        assertAllNextEqual(expected, children.iterator());
    }

    /**
     * Tests read-only operations on a list of properties for a shallow metadata object with singleton
     * values in collections.
     */
    @Test
    @DependsOnMethod("testReadOnlyWithoutCollections")
    public void testReadOnlyWithSingletonInCollections() {
        final DefaultCitation  citation = metadataWithSingletonInCollections();
        final TreeNodeChildren children = create(citation, ValueExistencePolicy.NON_EMPTY);
        final String[] expected = {
            "Some title",
            "First alternate title",
            "Some edition",
            "PresentationForm[MAP_DIGITAL]",
            "Some other details"
        };
        assertFalse ("isEmpty()", children.isEmpty());
        assertEquals("size()", expected.length, children.size());
        assertAllNextEqual(expected, children.iterator());
    }

    /**
     * Tests read-only operations on a list of properties for a shallow metadata object with more
     * than one values in collections.
     */
    @Test
    @DependsOnMethod("testReadOnlyWithSingletonInCollections")
    public void testReadOnlyWithMultiOccurrences() {
        final DefaultCitation  citation = metadataWithMultiOccurrences();
        final TreeNodeChildren children = create(citation, ValueExistencePolicy.NON_EMPTY);
        final String[] expected = {
            "Some title",
            "First alternate title",
            "Second alternate title",
            "Some edition",
            "PresentationForm[MAP_DIGITAL]",
            "PresentationForm[MAP_HARDCOPY]",
            "Some other details"
        };
        assertFalse ("isEmpty()", children.isEmpty());
        assertEquals("size()", expected.length, children.size());
        assertAllNextEqual(expected, children.iterator());
    }

    /**
     * Tests the {@link TreeNodeChildren#add(TreeTable.Node)} method.
     */
    @Test
    @DependsOnMethod("testReadOnlyWithMultiOccurrences")
    public void testAdd() {
        final DefaultCitation  citation = metadataWithMultiOccurrences();
        final TreeNodeChildren children = create(citation, ValueExistencePolicy.NON_EMPTY);
        final DefaultTreeTable.Node toAdd = new DefaultTreeTable.Node(new DefaultTreeTable(
                TableColumn.IDENTIFIER,
                TableColumn.VALUE));
        final String[] expected = {
            "Some title",
            "First alternate title",
            "Second alternate title",
            "Third alternate title",  // After addition
            "New edition", // After "addition" (actually change).
            "PresentationForm[IMAGE_DIGITAL]", // After addition
            "PresentationForm[MAP_DIGITAL]",
            "PresentationForm[MAP_HARDCOPY]",
            "Some other details"
        };
        toAdd.setValue(TableColumn.IDENTIFIER, "edition");
        toAdd.setValue(TableColumn.VALUE, citation.getEdition());
        assertFalse("Adding the same value shall be a no-op.", children.add(toAdd));
        toAdd.setValue(TableColumn.VALUE, "New edition");
        try {
            children.add(toAdd);
            fail("Setting a different value shall be refused.");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("edition"));
        }
        citation.setEdition(null); // Clears so we are allowed to add.
        assertTrue("Setting a new value shall be a change.", children.add(toAdd));

        toAdd.setValue(TableColumn.IDENTIFIER, "presentationForm");
        toAdd.setValue(TableColumn.VALUE, PresentationForm.MAP_DIGITAL);
        assertFalse("Adding the same value shall be a no-op.", children.add(toAdd));
        toAdd.setValue(TableColumn.VALUE, PresentationForm.IMAGE_DIGITAL);
        assertTrue("Adding a new value shall be a change.", children.add(toAdd));

        toAdd.setValue(TableColumn.IDENTIFIER, "alternateTitle");
        toAdd.setValue(TableColumn.VALUE, "Third alternate title");
        assertTrue("Adding a new value shall be a change.", children.add(toAdd));

        assertEquals("size()", expected.length, children.size());
        assertAllNextEqual(expected, children.iterator());
    }

    /**
     * Tests the {@link Iterator#remove()} operation on a list of properties without collections.
     */
    @Test
    @DependsOnMethod("testReadOnlyWithoutCollections")
    public void testRemoveWithoutCollections() {
        final DefaultCitation  citation = metadataWithoutCollections();
        final TreeNodeChildren children = create(citation, ValueExistencePolicy.NON_EMPTY);
        testRemove(createRandomNumberGenerator(), children);
    }

    /**
     * Tests the {@link Iterator#remove()} operation on a list of properties with
     * collections containing only one element.
     */
    @Test
    @DependsOnMethod({
        "testRemoveWithoutCollections",
        "testReadOnlyWithSingletonInCollections"
    })
    public void testRemoveWithSingletonInCollections() {
        final DefaultCitation  citation = metadataWithSingletonInCollections();
        final TreeNodeChildren children = create(citation, ValueExistencePolicy.NON_EMPTY);
        testRemove(createRandomNumberGenerator(), children);
    }

    /**
     * Tests the {@link Iterator#remove()} operation on a list of properties with
     * collections containing more than one element.
     */
    @Test
    @DependsOnMethod({
        "testRemoveWithSingletonInCollections",
        "testReadOnlyWithMultiOccurrences"
    })
    public void testRemoveWithMultiOccurrences() {
        final DefaultCitation  citation = metadataWithSingletonInCollections();
        final TreeNodeChildren children = create(citation, ValueExistencePolicy.NON_EMPTY);
        testRemove(createRandomNumberGenerator(), children);
    }

    /**
     * Tests the {@link TreeNodeChildren#clear()} method.
     */
    @Test
    public void testClear() {
        final DefaultCitation  citation = metadataWithSingletonInCollections();
        final TreeNodeChildren children = create(citation, ValueExistencePolicy.NON_EMPTY);
        assertFalse(children.isEmpty());
        children.clear();
        assertTrue(children.isEmpty());
        assertNull(citation.getTitle());
        assertTrue(citation.getAlternateTitles().isEmpty());
    }

    /**
     * Tests the children list with the {@link ValueExistencePolicy#ALL}.
     */
    @Test
    @DependsOnMethod("testReadOnlyWithMultiOccurrences")
    public void testShowAll() {
        final DefaultCitation  citation = metadataWithMultiOccurrences();
        final TreeNodeChildren children = create(citation, ValueExistencePolicy.ALL);
        final String[] expected = {
            "Some title",
            "First alternate title",
            "Second alternate title",
            null, // dates (collection)
            "Some edition",
            null, // edition date
            null, // identifiers (collection)
            null, // cited responsibly parties (collection)
            "PresentationForm[MAP_DIGITAL]",
            "PresentationForm[MAP_HARDCOPY]",
            null, // series
            "Some other details",
//          null, // collective title  -- deprecated as of ISO 19115:2014.
            null, // ISBN
            null, // ISSN
            null, // onlineResources (collection)
            null  // graphics (collection)
        };
        assertFalse ("isEmpty()", children.isEmpty());
        assertEquals("size()", expected.length, children.size());
        assertAllNextEqual(expected, children.iterator());
    }


    // ------------------------ Support methods for the above tests ------------------------

    /**
     * Returns the string representation of the user object in the given node.
     * This is the value that we are going compare in the assertion methods below.
     *
     * <p>We use only {@link TreeNode#getUserObject()}, nothing else,
     * because the purpose of this class is not to test {@link TreeNode}.</p>
     */
    private static String valueOf(final TreeTable.Node node) {
        final Object value = node.getUserObject();
        return (value != null) ? value.toString() : null;
    }

    /**
     * Asserts that the string representation of user objects of all next element are equal
     * to the expected strings.
     */
    private static void assertAllNextEqual(final String[] expected, final Iterator<TreeTable.Node> it) {
        for (final String e : expected) {
            assertTrue(e, it.hasNext());
            assertEquals("Iterator.next()", e, valueOf(it.next()));
        }
        assertFalse("Iterator.hasNext()", it.hasNext());
    }

    /**
     * Asserts that all next elements traversed by the {@code actual} iterator are equal
     * to the next elements traversed by {@code expected}.
     *
     * @param expected The iterator over expected values.
     * @param actual   The iterator over actual values.
     */
    private static void assertAllNextEqual(final Iterator<?> expected, final Iterator<?> actual) {
        while (expected.hasNext()) {
            assertTrue("Iterator.hasNext()", actual.hasNext());
            assertEquals("Iterator.next()", expected.next(), actual.next());
        }
        assertFalse("Iterator.hasNext()", actual.hasNext());
    }

    /**
     * Tests the {@link Iterator#remove()} operation on the given collection of children.
     * Elements are removed randomly until the collection is empty. After each removal,
     * the remaining elements are compared with the content of a standard Java collection.
     *
     * @param random   A random number generator.
     * @param children The collection from which to remove elements.
     */
    private static void testRemove(final Random random, final TreeNodeChildren children) {
        final List<TreeTable.Node> reference = new ArrayList<TreeTable.Node>(children);
        assertFalse("The collection shall not be initially empty.", reference.isEmpty());
        do {
            final Iterator<TreeTable.Node> rit = reference.iterator(); // The reference iterator.
            final Iterator<TreeTable.Node> cit = children .iterator(); // The children iterator to be tested.
            while (rit.hasNext()) {
                assertTrue(cit.hasNext());
                assertSame(rit.next(), cit.next());
                if (random.nextInt(3) == 0) { // Remove only 1/3 of entries on each pass.
                    rit.remove();
                    cit.remove();
                    assertAllNextEqual(reference.iterator(), children.iterator());
                }
            }
        } while (!reference.isEmpty());
        assertTrue(children.isEmpty());
    }
}
