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

import java.text.ParseException;
import org.junit.Test;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOn;

import static org.apache.sis.test.Assert.*;
import static org.apache.sis.util.collection.ColumnConstant.*;


/**
 * Tests the {@link TreeTableFormat} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
@DependsOn({
    DefaultTreeTableTest.class,
    org.apache.sis.io.TableFormatterTest.class
})
public final strictfp class TreeTableFormatTest extends TestCase {
    /**
     * Creates a node with a single column for object names.
     */
    private static DefaultTreeTable.Node createNode(final CharSequence name) {
        return new DefaultTreeTable.Node(NAME_MAP, new CharSequence[] {name});
    }

    /**
     * Tests the formatting as a tree, with control on the indentation.
     */
    @Test
    public void testTreeFormat() {
        final DefaultTreeTable.Node root   = createNode("Node #1");
        final DefaultTreeTable.Node branch = createNode("Node #2");
        root.getChildren().add(branch);
        root.getChildren().add(createNode("Node #3"));
        branch.getChildren().add(createNode("Node #4"));

        final TreeTableFormat tf = new TreeTableFormat(null, null);
        tf.setVerticalLinePosition(2);
        assertMultilinesEquals(
                "Node #1\n" +
                "  ├─Node #2\n" +
                "  │   └─Node #4\n" +
                "  └─Node #3\n", tf.format(new DefaultTreeTable(root)));
    }

    /**
     * Tests the parsing of a tree. This method parses and reformats a tree,
     * and performs its check on the assumption that the tree formatting is
     * accurate.
     *
     * @throws ParseException Should never happen.
     */
    @Test
    public void testTreeParse() throws ParseException {
        final TreeTableFormat tf = new TreeTableFormat(null, null);
        tf.setVerticalLinePosition(0);
        final String text =
                "Node #1\n" +
                "├───Node #2\n" +
                "│   └───Node #4\n" +
                "└───Node #3\n";
        final TreeTable table = tf.parseObject(text);
        assertMultilinesEquals(text, tf.format(table));
    }

    /**
     * Tests the formatting of a tree table.
     */
    @Test
    public void testTreeTableFormat() {
        final StringColumn     valueA = new StringColumn("value #1");
        final StringColumn     valueB = new StringColumn("value #2");
        final DefaultTreeTable table  = new DefaultTreeTable(NAME, valueA, valueB);
        final TreeTable.Node   root   = new DefaultTreeTable.Node(table);
        root.setValue(NAME,   "Node #1");
        root.setValue(valueA, "Value #1A");
        root.setValue(valueB, "Value #1B");
        final TreeTable.Node branch1 = new DefaultTreeTable.Node(table);
        branch1.setValue(NAME,   "Node #2");
        branch1.setValue(valueA, "Value #2A");
        root.getChildren().add(branch1);
        final TreeTable.Node branch2 = new DefaultTreeTable.Node(table);
        branch2.setValue(NAME,   "Node #3");
        branch2.setValue(valueB, "Value #3B");
        root.getChildren().add(branch2);
        final TreeTable.Node leaf = new DefaultTreeTable.Node(table);
        leaf.setValue(NAME,   "Node #4");
        leaf.setValue(valueA, "Value #4A");
        leaf.setValue(valueB, "ext #4\tafter tab\nand a new line");
        branch1.getChildren().add(leaf);
        table.setRoot(root);

        final TreeTableFormat tf = new TreeTableFormat(null, null);
        tf.setVerticalLinePosition(1);
        final String text = tf.format(table);
        assertMultilinesEquals(
                "Node #1…………………………Value #1A……Value #1B\n" +
                " ├──Node #2………………Value #2A……\n" +
                " │   └──Node #4……Value #4A……ext #4  after tab ¶ and a new line\n" +
                " └──Node #3……………………………………………Value #3B\n", text);
    }
}
