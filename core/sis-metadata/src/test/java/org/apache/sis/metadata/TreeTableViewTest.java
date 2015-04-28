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
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static org.apache.sis.test.TestUtilities.toTreeStructure;
import static org.apache.sis.test.TestUtilities.formatNameAndValue;


/**
 * Tests the {@link TreeTableView} class.
 * Unless otherwise specified, all tests use the {@link MetadataStandard#ISO_19115} constant.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
@DependsOn(TreeNodeTest.class)
public final strictfp class TreeTableViewTest extends TestCase {
    /**
     * Creates a table to be tested for the given value policy.
     */
    private static TreeTableView create(final ValueExistencePolicy valuePolicy) {
        return new TreeTableView(MetadataStandard.ISO_19115, TreeNodeTest.metadataWithHierarchy(), valuePolicy);
    }

    /**
     * The expected string representation of the tree created by {@link #create(ValueExistencePolicy)}
     * with {@link ValueExistencePolicy#NON_EMPTY}.
     */
    private static final String EXPECTED =
            "Citation\n" +
            "  ├─Title……………………………………………………………………………………………… Some title\n" +
            "  ├─Alternate title (1 of 2)…………………………………………… First alternate title\n" +
            "  ├─Alternate title (2 of 2)…………………………………………… Second alternate title\n" +
            "  ├─Edition………………………………………………………………………………………… Some edition\n" +
            "  ├─Cited responsible party (1 of 2)\n" +
            "  │   ├─Role……………………………………………………………………………………… Distributor\n" +
            "  │   └─Party\n" +
            "  │       └─Name…………………………………………………………………………… Some organisation\n" +
            "  ├─Cited responsible party (2 of 2)\n" +
            "  │   ├─Role……………………………………………………………………………………… Point of contact\n" +
            "  │   └─Party\n" +
            "  │       ├─Name…………………………………………………………………………… Some person of contact\n" +
            "  │       └─Contact info\n" +
            "  │           └─Address\n" +
            "  │               └─Electronic mail address…… Some email\n" +
            "  ├─Presentation form (1 of 2)……………………………………… Map digital\n" +
            "  ├─Presentation form (2 of 2)……………………………………… Map hardcopy\n" +
            "  └─Other citation details………………………………………………… Some other details\n";

    /**
     * Tests {@link TreeTableView#toString()}.
     * Since the result is locale-dependant, we can not compare against an exact string.
     * We will only compare the beginning of each line.
     */
    @Test
    public void testToString() {
        final TreeTableView metadata = create(ValueExistencePolicy.NON_EMPTY);
        assertMultilinesEquals(EXPECTED, formatNameAndValue(metadata)); // Locale-independent
        assertArrayEquals(toTreeStructure(EXPECTED), toTreeStructure(metadata.toString())); // Locale-dependent.
    }

    /**
     * Tests serialization.
     *
     * @throws Exception If an error occurred during the serialization process.
     */
    @Test
    @DependsOnMethod("testToString")
    public void testSerialization() throws Exception {
        final Object original = create(ValueExistencePolicy.NON_EMPTY);
        final Object deserialized;
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final ObjectOutputStream out = new ObjectOutputStream(buffer);
        try {
            out.writeObject(original);
        } finally {
            out.close();
        }
        // Now reads the object we just serialized.
        final byte[] data = buffer.toByteArray();
        final ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data));
        try {
            deserialized = in.readObject();
        } finally {
            in.close();
        }
        assertMultilinesEquals(EXPECTED, formatNameAndValue((TreeTableView) deserialized));
    }
}
