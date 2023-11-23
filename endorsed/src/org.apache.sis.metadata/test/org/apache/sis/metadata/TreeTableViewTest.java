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
import java.util.Iterator;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.opengis.annotation.Obligation;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.xml.NilReason;

// Test dependencies
import org.junit.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertMultilinesEquals;
import static org.apache.sis.test.TestUtilities.toTreeStructure;
import static org.apache.sis.test.TestUtilities.formatMetadata;


/**
 * Tests the {@link TreeTableView} class.
 * Unless otherwise specified, all tests use the {@link MetadataStandard#ISO_19115} constant.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@DependsOn(TreeNodeTest.class)
public final class TreeTableViewTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public TreeTableViewTest() {
    }

    /**
     * Creates a table to be tested for the given value policy.
     */
    private static TreeTableView create(final ValueExistencePolicy valuePolicy) {
        return new TreeTableView(MetadataStandard.ISO_19115, TreeNodeTest.metadataWithHierarchy(), Citation.class, valuePolicy);
    }

    /**
     * The expected string representation of the tree created by {@link #create(ValueExistencePolicy)}
     * with {@link ValueExistencePolicy#NON_EMPTY}.
     */
    private static final String EXPECTED =
            "Citation………………………………………………………………………………………………… Some title\n" +
            "  ├─Alternate title (1 of 2)…………………………………………… First alternate title\n" +
            "  ├─Alternate title (2 of 2)…………………………………………… Second alternate title\n" +
            "  ├─Edition………………………………………………………………………………………… Some edition\n" +
            "  ├─Cited responsible party (1 of 2)\n" +
            "  │   ├─Role……………………………………………………………………………………… Distributor\n" +
            "  │   └─Organisation………………………………………………………………… Some organisation\n" +
            "  ├─Cited responsible party (2 of 2)\n" +
            "  │   ├─Role……………………………………………………………………………………… Point of contact\n" +
            "  │   └─Individual……………………………………………………………………… Some person of contact\n" +
            "  │       └─Contact info\n" +
            "  │           └─Address\n" +
            "  │               └─Electronic mail address…… Some email\n" +
            "  ├─Presentation form (1 of 2)……………………………………… Map digital\n" +
            "  ├─Presentation form (2 of 2)……………………………………… Map hardcopy\n" +
            "  └─Other citation details………………………………………………… Some other details\n";

    /**
     * Tests {@link TreeTableView#toString()}.
     * Since the result is locale-dependent, we cannot compare against an exact string.
     * We will only compare the beginning of each line.
     */
    @Test
    public void testToString() {
        final TreeTableView metadata = create(ValueExistencePolicy.COMPACT);
        assertFalse(metadata.getColumns().contains(MetadataColumn.NIL_REASON));
        assertMultilinesEquals(EXPECTED, formatMetadata(metadata));                             // Locale-independent
        assertArrayEquals(toTreeStructure(EXPECTED), toTreeStructure(metadata.toString()));     // Locale-dependent.
    }

    /**
     * Verifies most columns in the tree table. All nil reasons are null.
     */
    @Test
    public void testGetValues() {
        final TreeTableView metadata = create(ValueExistencePolicy.NON_NULL);
        assertTrue(metadata.getColumns().contains(MetadataColumn.NIL_REASON));
        verify(metadata.getRoot(), "Some title", null);
    }

    /**
     * Verifies columns in the tree table with some non-null nil reasons.
     */
    @Test
    public void testNilReasons() {
        final TreeTableView metadata = create(ValueExistencePolicy.NON_NULL);
        assertTrue(metadata.getColumns().contains(MetadataColumn.NIL_REASON));
        final var citation = (DefaultCitation) metadata.getRoot().getUserObject();
        citation.setTitle(NilReason.TEMPLATE.createNilObject(InternationalString.class));
        verify(metadata.getRoot(), null, NilReason.TEMPLATE);
    }

    /**
     * Verifies the values of the given root node and some of its children.
     * This method modifies the metadata for also testing some nil values.
     *
     * @param  node     root node to verify.
     * @param  title    expected citation title, or {@code null} if it is expected to be missing.
     * @param  titleNR  if the title is missing, the expected reason why.
     */
    private void verify(TreeTableView.Node node, final String title, final NilReason titleNR) {
        assertEquals("CI_Citation",  node.getValue(TableColumn.IDENTIFIER));
        assertNull  (                node.getValue(TableColumn.INDEX));
        assertEquals("Citation",     node.getValue(TableColumn.NAME));
        assertEquals(Citation.class, node.getValue(TableColumn.TYPE));
        assertNull  (                node.getValue(TableColumn.OBLIGATION));
        assertNull  (                node.getValue(TableColumn.VALUE));
        assertNull  (                node.getValue(MetadataColumn.NIL_REASON));
        /*
         * The first child of a Citation object should be the title.
         * Verify the title value, type, obligation, etc.
         */
        Iterator<TreeTable.Node> it = node.getChildren().iterator();
        node = it.next();
        assertEquals("title",                      node.getValue(TableColumn.IDENTIFIER));
        assertNull  (                              node.getValue(TableColumn.INDEX));
        assertEquals("Title",                      node.getValue(TableColumn.NAME));
        assertEquals(InternationalString.class,    node.getValue(TableColumn.TYPE));
        assertEquals(Obligation.MANDATORY,         node.getValue(TableColumn.OBLIGATION));
        assertI18nEq(title,                        node.getValue(TableColumn.VALUE));
        assertEquals(titleNR,                      node.getValue(MetadataColumn.NIL_REASON));
        /*
         * Declare the title as missing and verify that the change has been applied.
         */
        node.setValue(MetadataColumn.NIL_REASON, NilReason.MISSING);
        assertEquals(NilReason.MISSING, node.getValue(MetadataColumn.NIL_REASON));
        assertNull(node.getValue(TableColumn.VALUE));
        /*
         * The second child of the Citation use in this test should be an alternate title.
         * This property is a collection with two elements. Check the first one.
         */
        node = it.next();
        assertEquals("alternateTitle",              node.getValue(TableColumn.IDENTIFIER));
        assertEquals(0,                             node.getValue(TableColumn.INDEX));
        assertI18nEq("Alternate title (1 of 2)",    node.getValue(TableColumn.NAME));
        assertEquals(InternationalString.class,     node.getValue(TableColumn.TYPE));
        assertEquals(Obligation.OPTIONAL,           node.getValue(TableColumn.OBLIGATION));
        assertI18nEq("First alternate title",       node.getValue(TableColumn.VALUE));
        assertNull  (                               node.getValue(MetadataColumn.NIL_REASON));
        /*
         * Set the first element to nil, then check that the second element has not been impacted.
         * Contrarily to the previous test, this test modifies a collection elements instead of the
         * property as a whole.
         */
        node.setValue(MetadataColumn.NIL_REASON, NilReason.INAPPLICABLE);
        assertEquals(NilReason.INAPPLICABLE, node.getValue(MetadataColumn.NIL_REASON));
        assertNull(node.getValue(TableColumn.VALUE));
        node = it.next();
        assertI18nEq("Second alternate title", node.getValue(TableColumn.VALUE));
        assertNull(node.getValue(MetadataColumn.NIL_REASON));
    }

    /**
     * Verifies the value of the given international string in English.
     */
    private static void assertI18nEq(final String expected, Object text) {
        if (text instanceof InternationalString) {
            text = ((InternationalString) text).toString(Locale.ENGLISH);
        }
        assertEquals(expected, text);
    }

    /**
     * Tests serialization.
     *
     * @throws Exception if an error occurred during the serialization process.
     */
    @Test
    @DependsOnMethod("testToString")
    public void testSerialization() throws Exception {
        final Object original = create(ValueExistencePolicy.COMPACT);
        final Object deserialized;
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(buffer)) {
            out.writeObject(original);
        }
        // Now reads the object we just serialized.
        final byte[] data = buffer.toByteArray();
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data))) {
            deserialized = in.readObject();
        }
        assertMultilinesEquals(EXPECTED, formatMetadata((TreeTableView) deserialized));
    }

    /**
     * Tests formatting a tree containing a remark. We use a geographic bounding box crossing the anti-meridian.
     * In this test the longitude value and the remarks and separated by "……" characters, but this is because we
     * use the default {@link org.apache.sis.util.collection.TreeTableFormat}. When using {@link MetadataFormat}
     * specialization, the formatting is a little bit different
     */
    @Test
    public void testRemarks() {
        final DefaultGeographicBoundingBox bbox = new DefaultGeographicBoundingBox(170, -160, -30, 40);
        final String text = formatMetadata(bbox.asTreeTable());
        assertMultilinesEquals(
                "Geographic bounding box\n" +
                "  ├─West bound longitude…… 170°E\n" +
                "  ├─East bound longitude…… 160°W…… Bounding box crosses the antimeridian.\n" +   // See method javadoc.
                "  ├─South bound latitude…… 30°S\n" +
                "  ├─North bound latitude…… 40°N\n" +
                "  └─Extent type code……………… True\n", text);
    }
}
