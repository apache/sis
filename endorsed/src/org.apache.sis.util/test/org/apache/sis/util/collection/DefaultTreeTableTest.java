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
import java.util.Collection;
import static org.apache.sis.util.collection.TableColumn.*;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestStep;
import static org.apache.sis.test.Assertions.assertMessageContains;
import static org.apache.sis.test.Assertions.assertSerializedEquals;
import static org.apache.sis.test.Assertions.assertSingleton;


/**
 * Tests the {@link DefaultTreeTable} class and its {@code Node} inner class.
 * This will also test indirectly the {@link TreeNodeList} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class DefaultTreeTableTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultTreeTableTest() {
    }

    /**
     * Tests the creation of an {@link DefaultTreeTable} with initially no root node.
     * The columns are {@code NAME} and {@code TYPE}.
     *
     * <p>This method is part of a chain.
     * The next method is {@link #testNodeCreation(DefaultTreeTable)}.</p>
     *
     * @return the created table, for chaining with methods testing the next step after this one.
     */
    @TestStep
    public static DefaultTreeTable testTableCreation() {
        final var table = new DefaultTreeTable(NAME, TYPE);
        assertEquals(2,                  table.columnIndices.size());
        assertEquals(Integer.valueOf(0), table.columnIndices.get(NAME));
        assertEquals(Integer.valueOf(1), table.columnIndices.get(TYPE));
        assertArrayEquals(new TableColumn<?>[] {NAME, TYPE}, table.getColumns().toArray());
        return table;
    }

    /**
     * Tests the creation of children nodes.
     * The root node has only one child, which itself has only one child.
     *
     * <p>This method is part of a chain.
     * The previous method is {@link #testTableCreation()} and
     * the next method is {@link #testNodeDisplacement(TreeTable.Node)}.</p>
     *
     * @param  table  an initially empty table where to set the root.
     * @return the root node produced by this method.
     */
    @TestStep
    public static DefaultTreeTable.Node testNodeCreation(final DefaultTreeTable table) {
        /*
         * Create a root node with an initially empty list of children.
         */
        final var root = new DefaultTreeTable.Node(table);
        assertSame(table.columnIndices, root.columnIndices, "Internal table sharing:");
        assertTrue(root.getChildren().isEmpty(),            "Initial children list:");
        table.setRoot(root);
        /*
         * Create a first child node, which should be added automatically
         * to the root list of children.
         */
        final DefaultTreeTable.Node node1 = root.newChild();
        assertSame(table.columnIndices, node1.columnIndices,    "Internal table sharing:");
        assertTrue(node1.getChildren().isEmpty(),               "Initial children list:");
        assertSame(root, node1.getParent(),                     "Specified parent:");
        assertSame(node1, assertSingleton(root.getChildren()),  "Children list after add:");
        /*
         * Create a child of the previous child.
         */
        final DefaultTreeTable.Node node2 = new DefaultTreeTable.Node(node1, 0);
        assertSame(table.columnIndices, node2.columnIndices,    "Internal table sharing:");
        assertTrue(node2.getChildren().isEmpty(),               "Initial children list:");
        assertSame(node1, node2.getParent(),                    "Specified parent:");
        assertSame(node2, assertSingleton(node1.getChildren()), "Children list after add:");
        assertSame(node1, assertSingleton(root.getChildren()),  "Independent children list:");
        /*
         * For chaining with next tests.
         */
        assertSame(root, table.getRoot());
        return root;
    }

    /**
     * Tests the displacement of nodes, in particular ensures that the parent is updated.
     *
     * <p>This method is part of a chain.
     * The previous method is {@link #testNodeCreation(DefaultTreeTable)} and
     * the next method is {@link #testSerialization(TreeTable)}.</p>
     *
     * @param  root  the root node where to move children.
     */
    @TestStep
    public static void testNodeDisplacement(final TreeTable.Node root) {
        final Collection<TreeTable.Node> rootChildren, nodeChildren;
        final TreeTable.Node node1 = assertSingleton(rootChildren = root .getChildren());
        final TreeTable.Node node2 = assertSingleton(nodeChildren = node1.getChildren());
        var e = assertThrows(IllegalArgumentException.class, () -> rootChildren.add(node2),
                "Should not be allowed to add a child before we removed it from its previous parent.");
        assertMessageContains(e, "Node-0");

        assertSame(node1, node2.getParent(), "Initial parent:");
        assertTrue(nodeChildren.remove(node2));
        assertTrue(nodeChildren.isEmpty(), "Children list after removal:");
        assertNull(node2.getParent(), "Parent after removal:");
        assertTrue(rootChildren.add(node2));
        assertSame(root, node2.getParent(), "Parent after add:");
        assertArrayEquals(new TreeTable.Node[] {node1, node2}, rootChildren.toArray(), "Children list after add:");
    }

    /**
     * Tests {@link DefaultTreeTable#clone()}.
     * This will also indirectly tests {@link DefaultTreeTable#equals(Object)}.
     *
     * <p>This method is part of a chain.
     * The previous method is {@link #testNodeDisplacement(TreeTable.Node)}.</p>
     *
     * @param  table  the table to clone.
     * @throws CloneNotSupportedException if the table cannot be cloned.
     */
    @TestStep
    public static void testClone(final DefaultTreeTable table) throws CloneNotSupportedException {
        final TreeTable newTable = table.clone();
        assertNotSame(table, newTable);
        assertEquals(table, newTable);
        assertEquals(table.hashCode(), newTable.hashCode());
        getChildrenList(newTable).get(1).setValue(NAME, "New name");
        assertNotEquals(newTable, table);
    }

    /**
     * Tests {@link DefaultTreeTable} serialization.
     *
     * <p>This method is part of a chain.
     * The previous method is {@link #testNodeDisplacement(TreeTable.Node)}.</p>
     *
     * @param  table  the table to serialize.
     */
    @TestStep
    public static void testSerialization(final TreeTable table) {
        final TreeTable newTable = assertSerializedEquals(table);
        getChildrenList(newTable).get(1).setValue(NAME, "New name");
        assertNotEquals(newTable, table);
    }

    /**
     * Returns the children of the root of the given table as a list.
     * Instances of {@link DefaultTreeTable.Node} shall be guaranteed
     * to store their children in a list.
     */
    private static List<TreeTable.Node> getChildrenList(final TreeTable table) {
        final Collection<TreeTable.Node> children = table.getRoot().getChildren();
        assertInstanceOf(List.class, children);
        return (List<TreeTable.Node>) children;
    }

    /**
     * Tests the creation of a tree table with a few nodes, and tests the displacement of a node
     * from one branch to another. Finally tests the serialization of that table and the comparison
     * with the original object.
     *
     * <p>This test is actually a chain of {@link TestStep} methods.</p>
     *
     * @throws CloneNotSupportedException if the {@link DefaultTreeTable#clone()} method failed.
     */
    @Test
    public void testTreeTableCreation() throws CloneNotSupportedException {
        final DefaultTreeTable table = testTableCreation();
        final TreeTable.Node   root  = testNodeCreation(table);
        testNodeDisplacement(root);
        testClone(table);
        testSerialization(table);
    }

    /**
     * Tests {@link DefaultTreeTable.Node#setValue(TableColumn, Object)}.
     */
    @Test
    public void testNodeValues() {
        final DefaultTreeTable table = new DefaultTreeTable(NAME, TYPE);
        final TreeTable.Node   node  = new DefaultTreeTable.Node(table);
        assertNull(node.getValue(NAME));
        assertNull(node.getValue(TYPE));
        node.setValue(NAME, "A number");
        node.setValue(TYPE, Number.class);
        assertEquals("A number",   node.getValue(NAME));
        assertEquals(Number.class, node.getValue(TYPE));
    }
}
