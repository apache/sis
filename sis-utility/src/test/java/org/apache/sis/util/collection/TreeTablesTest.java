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
package org.apache.sis.util.collection;

import java.util.List;
import java.io.File;
import java.text.ParseException;
import org.junit.Test;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOn;
import org.apache.sis.util.iso.SimpleInternationalString;

import static org.apache.sis.test.Assert.*;
import static org.apache.sis.util.collection.TreeTables.*;
import static org.apache.sis.util.collection.TableColumn.*;


/**
 * Tests the {@link TreeTables} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@DependsOn({
    DefaultTreeTableTest.class
})
public final strictfp class TreeTablesTest extends TestCase {
    /**
     * The {@code concatenateSingletons(…)} example documented in the {@link TreeTables} class javadoc.
     * This simple code assumes that the children collection in the given node is a {@link List}.
     *
     * @param  node The root of the node to simplify.
     * @return The root of the simplified tree. May be the given {@code node} or a child.
     */
    public static TreeTable.Node concatenateSingletons(final TreeTable.Node node) {
        final List<TreeTable.Node> children = (List<TreeTable.Node>) node.getChildren();
        final int size = children.size();
        for (int i=0; i<size; i++) {
            children.set(i, concatenateSingletons(children.get(i)));
        }
        if (size == 1) {
            final TreeTable.Node child = children.get(0);
            if (node.getValue(VALUE_AS_TEXT) == null) {
                children.remove(0);
                child.setValue(NAME, node.getValue(NAME) + File.separator + child.getValue(NAME));
                return child;
            }
        }
        return node;
    }

    /**
     * Tests the {@link #concatenateSingletons(TreeTable.Node)} example.
     *
     * @throws ParseException Should never happen.
     */
    @Test
    public void testConcatenateSingletons() throws ParseException {
        final TreeTable table = TreeTables.parse(
                "root\n" +
                "  ├─users\n" +
                "  │   └─alice\n" +
                "  │       ├─data\n" +
                "  │       │   └─mercator\n" +
                "  │       └─document\n" +
                "  └─lib\n", NAME);
        ((DefaultTreeTable) table).setRoot(concatenateSingletons(table.getRoot()));
        assertMultilinesEquals((
                "root\n" +
                "  ├─users/alice\n" +
                "  │   ├─data/mercator\n" +
                "  │   └─document\n" +
                "  └─lib\n").replace('/', File.separatorChar), table.toString());
    }

    /**
     * Tests the {@link TreeTables#nodeForPath(TreeTable.Node, TableColumn, File)} method.
     */
    @Test
    public void testNodeForPathAsFile() {
        final TreeTable table = new DefaultTreeTable(NAME, VALUE_AS_NUMBER);
        final TreeTable.Node files = table.getRoot();
        files.setValue(NAME, "Root");
        nodeForPath(files, NAME, new File("users/Alice/data"))         .setValue(VALUE_AS_NUMBER, 10);
        nodeForPath(files, NAME, new File("users/Bob/data"))           .setValue(VALUE_AS_NUMBER, 20);
        nodeForPath(files, NAME, new File("users/Bob"))                .setValue(VALUE_AS_NUMBER, 30);
        nodeForPath(files, NAME, new File("lib"))                      .setValue(VALUE_AS_NUMBER, 40);
        nodeForPath(files, NAME, new File("users/Alice/document"))     .setValue(VALUE_AS_NUMBER, 50);
        nodeForPath(files, NAME, new File("users/Alice/data/mercator")).setValue(VALUE_AS_NUMBER, 60);
        assertMultilinesEquals(
                "Root\n" +
                "  ├─users\n" +
                "  │   ├─Alice\n" +
                "  │   │   ├─data………………………… 10\n" +
                "  │   │   │   └─mercator…… 60\n" +
                "  │   │   └─document……………… 50\n" +
                "  │   └─Bob……………………………………… 30\n" +
                "  │       └─data………………………… 20\n" +
                "  └─lib………………………………………………… 40\n", table.toString());
    }

    /**
     * Tests the {@link TreeTables#replaceCharSequences(TreeTable, Locale)} method.
     */
    @Test
    public void testReplaceCharSequences() {
        final TreeTable table = new DefaultTreeTable(NAME, VALUE_AS_NUMBER);
        final TreeTable.Node root   = table .getRoot();
        final TreeTable.Node parent = root  .newChild();
        final TreeTable.Node child1 = parent.newChild();
        final TreeTable.Node child2 = root  .newChild();
        root  .setValue(NAME, new StringBuilder("Root"));
        parent.setValue(NAME, "A parent");
        child1.setValue(NAME, new StringBuilder("A child"));
        child2.setValue(NAME, new SimpleInternationalString("A child"));
        root  .setValue(VALUE_AS_NUMBER, 8);
        parent.setValue(VALUE_AS_NUMBER, 4);

        final String asString = table.toString();
        assertEquals(3, replaceCharSequences(table, null));
        assertInstanceOf("replaceCharSequences:", String.class, root  .getValue(NAME));
        assertInstanceOf("replaceCharSequences:", String.class, parent.getValue(NAME));
        assertInstanceOf("replaceCharSequences:", String.class, child1.getValue(NAME));
        assertInstanceOf("replaceCharSequences:", String.class, child2.getValue(NAME));
        assertSame("Expected unique instance of String.", child1.getValue(NAME), child2.getValue(NAME));
        assertEquals("String representation shall be the same.", asString, table.toString());
    }
}
