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
package org.apache.sis.test.widget;

import java.util.Enumeration;
import javax.swing.tree.TreeNode;

import static org.junit.Assert.*;


/**
 * Assertions on Swing objects.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final strictfp class SwingAssertions {
    /**
     * Do not allow instantiation of this class.
     */
    private SwingAssertions() {
    }

    /**
     * Ensures that a tree is equal to an other tree.
     * This method invokes itself recursively for every child nodes.
     *
     * @param  expected  the expected tree, or {@code null}.
     * @param  actual    the tree to compare with the expected one, or {@code null}.
     * @return the number of nodes.
     */
    public static int assertTreeEquals(final TreeNode expected, final TreeNode actual) {
        if (expected == null) {
            assertNull(actual);
            return 0;
        }
        int n = 1;
        assertNotNull(actual);
        assertEquals("isLeaf()",            expected.isLeaf(),            actual.isLeaf());
        assertEquals("getAllowsChildren()", expected.getAllowsChildren(), actual.getAllowsChildren());
        assertEquals("getChildCount()",     expected.getChildCount(),     actual.getChildCount());
        @SuppressWarnings("unchecked") final Enumeration<? extends TreeNode> ec = expected.children();
        @SuppressWarnings("unchecked") final Enumeration<? extends TreeNode> ac = actual  .children();

        int childIndex = 0;
        while (ec.hasMoreElements()) {
            assertTrue("hasMoreElements()", ac.hasMoreElements());
            final TreeNode nextExpected = ec.nextElement();
            final TreeNode nextActual   = ac.nextElement();
            final String message = "getChildAt(" + childIndex + ')';
            assertSame(message, nextExpected, expected.getChildAt(childIndex));
            assertSame(message, nextActual,   actual  .getChildAt(childIndex));
            assertSame("getParent()", expected, nextExpected.getParent());
            assertSame("getParent()", actual,   nextActual  .getParent());
            assertSame("getIndex(TreeNode)", childIndex, expected.getIndex(nextExpected));
            assertSame("getIndex(TreeNode)", childIndex, actual  .getIndex(nextActual));
            n += assertTreeEquals(nextExpected, nextActual);
            childIndex++;
        }
        assertFalse("hasMoreElements()", ac.hasMoreElements());
        assertEquals("toString()", expected.toString(), actual.toString());
        return n;
    }
}
