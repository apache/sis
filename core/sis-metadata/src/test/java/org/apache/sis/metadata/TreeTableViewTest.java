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

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.Assertions.assertMultilinesEquals;
import static org.apache.sis.test.TestUtilities.toTreeStructure;
import static org.apache.sis.test.TestUtilities.formatMetadata;


/**
 * Tests the {@link TreeTableView} class.
 * Unless otherwise specified, all tests use the {@link MetadataStandard#ISO_19115} constant.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.3
 */
@DependsOn(TreeNodeTest.class)
public final class TreeTableViewTest extends TestCase {
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
        assertMultilinesEquals(EXPECTED, formatMetadata(metadata));                             // Locale-independent
        assertArrayEquals(toTreeStructure(EXPECTED), toTreeStructure(metadata.toString()));     // Locale-dependent.
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
     *
     * @since 1.0
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
