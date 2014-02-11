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
import org.junit.Test;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestStep;
import org.apache.sis.test.DependsOn;

import static org.apache.sis.test.Assert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;
import static org.apache.sis.util.collection.TableColumn.*;


/**
 * Tests the {@link DefaultTreeTable} class and its {@code Node} inner class.
 * This will also test indirectly the {@link TreeNodeList} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@DependsOn(TableColumnTest.class)
public final strictfp class DefaultTreeTableTest extends TestCase {
    /**
     * Tests the creation of an {@link DefaultTreeTable} with initially no root node.
     * The columns are {@code NAME} and {@code TYPE}.
     *
     * <p>This method is part of a chain.
     * The next method is {@link #testNodeCreation(DefaultTreeTable)}.</p>
     *
     * @return The created table, for chaining with methods testing the next step
     *         after this one.
     */
    @TestStep
    public static DefaultTreeTable testTableCreation() {
        final DefaultTreeTable table = new DefaultTreeTable(NAME, TYPE);
        assertEquals("Number of columns:",      2,                  table.columnIndices.size());
        assertEquals("Index of first column:",  Integer.valueOf(0), table.columnIndices.get(NAME));
        assertEquals("Index of second column:", Integer.valueOf(1), table.columnIndices.get(TYPE));
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
     * @param  table An initially empty table where to set the root.
     * @return The root node produced by this method.
     */
    @TestStep
    public static DefaultTreeTable.Node testNodeCreation(final DefaultTreeTable table) {
        /*
         * Create a root node with an initially empty list of children.
         */
        final DefaultTreeTable.Node root = new DefaultTreeTable.Node(table);
        assertSame("Internal table sharing:", table.columnIndices, root.columnIndices);
        assertTrue("Initial children list:",  root.getChildren().isEmpty());
        table.setRoot(root);
        /*
         * Create a first child node, which should be added automatically
         * to the root list of children.
         */
        final DefaultTreeTable.Node node1 = root.newChild();
        assertSame("Internal table sharing:",  table.columnIndices, node1.columnIndices);
        assertTrue("Initial children list:",   node1.getChildren().isEmpty());
        assertSame("Specified parent:",        root, node1.getParent());
        assertSame("Children list after add:", node1, getSingleton(root.getChildren()));
        /*
         * Create a child of the previous child.
         */
        final DefaultTreeTable.Node node2 = new DefaultTreeTable.Node(node1, 0);
        assertSame("Internal table sharing:",    table.columnIndices, node2.columnIndices);
        assertTrue("Initial children list:",     node2.getChildren().isEmpty());
        assertSame("Specified parent:",          node1, node2.getParent());
        assertSame("Children list after add:",   node2, getSingleton(node1.getChildren()));
        assertSame("Independent children list:", node1, getSingleton(root.getChildren()));
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
     * @param root The root node where to move children.
     */
    @TestStep
    public static void testNodeDisplacement(final TreeTable.Node root) {
        final Collection<TreeTable.Node> rootChildren, nodeChildren;
        final TreeTable.Node node1 = getSingleton(rootChildren = root .getChildren());
        final TreeTable.Node node2 = getSingleton(nodeChildren = node1.getChildren());
        try {
            assertTrue(rootChildren.add(node2));
            fail("Should not be allowed to add a child before we removed it from its previous parent.");
        } catch (IllegalArgumentException e) {
            // This is the expected exception.
            assertTrue(e.getMessage().contains("Node-0"));
        }
        assertSame("Initial parent:", node1,       node2.getParent());
        assertTrue(                                nodeChildren.remove(node2));
        assertTrue("Children list after removal:", nodeChildren.isEmpty());
        assertNull("Parent after removal:",        node2.getParent());
        assertTrue(                                rootChildren.add(node2));
        assertSame("Parent after add:", root,      node2.getParent());
        assertArrayEquals("Children list after add:",
                new TreeTable.Node[] {node1, node2}, rootChildren.toArray());
    }

    /**
     * Tests {@link DefaultTreeTable#clone()}.
     * This will also indirectly tests {@link DefaultTreeTable#equals(Object)}.
     *
     * <p>This method is part of a chain.
     * The previous method is {@link #testNodeDisplacement(TreeTable.Node)}.</p>
     *
     * @param  table The table to clone.
     * @throws CloneNotSupportedException Should never happen.
     */
    @TestStep
    public static void testClone(final DefaultTreeTable table) throws CloneNotSupportedException {
        final TreeTable newTable = table.clone();
        assertNotSame("clone", table, newTable);
        assertEquals("newTable.equals(table)", table, newTable);
        assertEquals("hashCode", table.hashCode(), newTable.hashCode());
        getChildrenList(newTable).get(1).setValue(NAME, "New name");
        assertFalse("newTable.equals(table)", newTable.equals(table));
    }

    /**
     * Tests {@link DefaultTreeTable} serialization.
     *
     * <p>This method is part of a chain.
     * The previous method is {@link #testNodeDisplacement(TreeTable.Node)}.</p>
     *
     * @param table The table to serialize.
     */
    @TestStep
    public static void testSerialization(final TreeTable table) {
        final TreeTable newTable = assertSerializedEquals(table);
        getChildrenList(newTable).get(1).setValue(NAME, "New name");
        assertFalse("newTable.equals(table)", newTable.equals(table));
    }

    /**
     * Returns the children of the root of the given table as a list.
     * Instances of {@link DefaultTreeTable.Node} shall be guaranteed
     * to store their children in a list.
     */
    private static List<TreeTable.Node> getChildrenList(final TreeTable table) {
        final Collection<TreeTable.Node> children = table.getRoot().getChildren();
        assertInstanceOf("TreeTable.Node.getChildren()", List.class, children);
        return (List<TreeTable.Node>) children;
    }

    /**
     * Tests the creation of a tree table with a few nodes, and tests the displacement of a node
     * from one branch to another. Finally tests the serialization of that table and the comparison
     * with the original object.
     *
     * <p>This test is actually a chain of {@link TestStep} methods.</p>
     *
     * @throws CloneNotSupportedException If the {@link DefaultTreeTable#clone()} method failed.
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
