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
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.ListIterator;
import org.opengis.metadata.citation.PresentationForm;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.TestUtilities.createRandomNumberGenerator;


/**
 * Tests the {@link MetadataTreeChildren} class.
 * Unless otherwise specified, all tests use the {@link MetadataStandard#ISO_19115} constant.
 *
 * {@section Test dependency}
 * This class uses the {@link MetadataTreeNode#getUserObject()} method for comparing the values.
 * We can hardly avoid to use some {@code MetadataTreeNode} methods because of the cross-dependencies.
 * However we try to use nothing else than {@code getUserObject()} because the purpose of this class
 * is not to test {@link MetadataTreeNode}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@DependsOn(PropertyAccessorTest.class)
public final strictfp class MetadataTreeChildrenTest extends TestCase {
    /**
     * Creates a shallow metadata object without collections.
     */
    static DefaultCitation metadataWithoutCollections() {
        final DefaultCitation citation = new DefaultCitation("Some title");
        citation.setEdition(new SimpleInternationalString("Some edition"));
        citation.setOtherCitationDetails(new SimpleInternationalString("Some other details"));
        return citation;
    }

    /**
     * Creates a shallow metadata object with singleton value in collections.
     */
    static DefaultCitation metadataWithSingletonInCollections() {
        final DefaultCitation citation = metadataWithoutCollections();
        assertTrue(citation.getAlternateTitles().add(new SimpleInternationalString("First alternate title")));
        assertTrue(citation.getPresentationForms().add(PresentationForm.MAP_DIGITAL));
        return citation;
    }

    /**
     * Creates a shallow metadata object with multi-occurrences
     * (i.e. more than one value in collections).
     */
    static DefaultCitation metadataWithMultiOccurrences() {
        final DefaultCitation citation = metadataWithSingletonInCollections();
        assertTrue(citation.getAlternateTitles().add(new SimpleInternationalString("Second alternate title")));
        assertTrue(citation.getPresentationForms().add(PresentationForm.MAP_HARDCOPY));
        return citation;
    }

    /**
     * Creates a list to be tested for the given metadata object and value policy.
     */
    private static MetadataTreeChildren create(final AbstractMetadata metadata, final ValueExistencePolicy valuePolicy) {
        final MetadataStandard  standard = MetadataStandard.ISO_19115;
        final MetadataTreeTable table    = new MetadataTreeTable(standard, metadata, valuePolicy);
        final MetadataTreeNode  node     = (MetadataTreeNode) table.getRoot();
        final PropertyAccessor  accessor = standard.getAccessor(metadata.getClass(), true);
        return new MetadataTreeChildren(node, metadata, accessor);
    }

    /**
     * Tests read-only operations on a list of properties for a shallow metadata object without collections.
     */
    @Test
    public void testReadOnlyWithoutCollections() {
        random = createRandomNumberGenerator("testReadOnlyWithoutCollections");
        final DefaultCitation      citation = metadataWithoutCollections();
        final MetadataTreeChildren children = create(citation, ValueExistencePolicy.NON_EMPTY);
        final String[] expected = {
            "Some title",
            "Some edition",
            "Some other details"
        };
        assertFalse ("isEmpty()", children.isEmpty());
        assertEquals("size()", expected.length, children.size());

        assertAllNextEqual(expected, children.iterator());
        assertAllEqual(true, expected, children.listIterator());
        testGet(expected, children);
    }

    /**
     * Tests read-only operations on a list of properties for a shallow metadata object with singleton
     * values in collections.
     */
    @Test
    @DependsOnMethod("testReadOnlyWithoutCollections")
    public void testReadOnlyWithSingletonInCollections() {
        random = createRandomNumberGenerator("testReadOnlyWithSingletonInCollections");
        final DefaultCitation      citation = metadataWithSingletonInCollections();
        final MetadataTreeChildren children = create(citation, ValueExistencePolicy.NON_EMPTY);
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
        assertAllEqual(true, expected, children.listIterator());
        testGet(expected, children);
    }

    /**
     * Tests read-only operations on a list of properties for a shallow metadata object with more
     * than one values in collections.
     */
    @Test
    @DependsOnMethod("testReadOnlyWithSingletonInCollections")
    public void testReadOnlyWithMultiOccurrences() {
        random = createRandomNumberGenerator("testReadOnlyWithMultiOccurrences");
        final DefaultCitation      citation = metadataWithMultiOccurrences();
        final MetadataTreeChildren children = create(citation, ValueExistencePolicy.NON_EMPTY);
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
        assertAllEqual(false, expected, children.listIterator());
        testGet(expected, children);
    }


    // ------------------------ Support methods for the above tests ------------------------


    /**
     * Random number generator used by the {@code assert*} methods.
     * Must be initialized by the public test methods.
     */
    private Random random;

    /**
     * Returns the string representation of the user object in the given node.
     * This is the value that we are going compare in the assertion methods below.
     *
     * <p>We use only {@link MetadataTreeNode#getUserObject()}, nothing else,
     * because the purpose of this class is not to test {@link MetadataTreeNode}.</p>
     */
    private static String valueOf(final TreeTable.Node node) {
        return String.valueOf(node.getUserObject());
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
     * Same assertion than {@link #assertAllNextEqual(String[], Iterator)},
     * but move randomly forward and backward.
     *
     * @param cached {@code true} if all nodes returned by the iterator are expected to be cached,
     *               i.e. if asking for the element at the same index shall return the same instance.
     */
    private void assertAllEqual(final boolean cached, final String[] expected, final ListIterator<TreeTable.Node> it) {
        final TreeTable.Node[] cache = cached ? new TreeTable.Node[expected.length] : null;
        int index = 0; // For verification purpose only.
        boolean forward = true;
        for (int i=0; i<50; i++) {
            /*
             * Select randomly a traversal direction for this step. We reverse the
             * direction only 1/3 of time in order to give the iterator more chances
             * to span the full range of expected values.
             */
            if (index == 0) {
                assertFalse(it.hasPrevious());
                forward = true;
            } else if (index == expected.length) {
                assertFalse(it.hasNext());
                forward = false;
            } else if (random.nextInt(3) == 0) {
                forward = !forward;
            }
            /*
             * Get the next or previous node, depe,ding on the current traversal direction.
             */
            final TreeTable.Node node;
            final String message;
            final int at;
            if (forward) {
                message = "next index=" + index + " iter="+i;
                assertEquals(message, index, it.nextIndex());
                assertTrue(message, it.hasNext());
                node = it.next();
                assertEquals(message, index, it.previousIndex());
                at = index++;
            } else {
                at = --index;
                message = "previous index=" + index + " iter="+i;
                assertEquals(message, index, it.previousIndex());
                assertTrue(message, it.hasPrevious());
                node = it.previous();
                assertEquals(message, index, it.nextIndex());
            }
            assertEquals(message, expected[at], valueOf(node));
            /*
             * If the nodes are expected to be cached, verify that.
             */
            if (cached) {
                if (cache[at] == null) {
                    cache[at] = node;
                } else {
                    assertSame(message, cache[at], node);
                }
            }
        }
    }

    /**
     * Tests {@link MetadataTreeChildren#get(int)}, which will indirectly tests
     * {@link MetadataTreeChildren#listIterator(int)}. The indices will be tested
     * in random order.
     */
    private void testGet(final String[] expected, final MetadataTreeChildren children) {
        final Integer[] index = new Integer[expected.length];
        for (int i=0; i<index.length; i++) {
            index[i] = i;
        }
        Collections.shuffle(Arrays.asList(index), random);
        for (int j=0; j<index.length; j++) {
            final int i = index[j];
            assertEquals(expected[i], valueOf(children.get(i)));
        }
    }
}
