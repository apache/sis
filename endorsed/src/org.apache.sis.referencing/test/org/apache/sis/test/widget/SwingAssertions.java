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
import java.util.function.Supplier;
import javax.swing.tree.TreeNode;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;


/**
 * Assertions on Swing objects.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class SwingAssertions {
    /**
     * Do not allow instantiation of this class.
     */
    private SwingAssertions() {
    }

    /**
     * Ensures that a tree is equal to another tree.
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
        assertEquals(expected.isLeaf(),            actual.isLeaf());
        assertEquals(expected.getAllowsChildren(), actual.getAllowsChildren());
        assertEquals(expected.getChildCount(),     actual.getChildCount());
        @SuppressWarnings("unchecked") final Enumeration<? extends TreeNode> ec = expected.children();
        @SuppressWarnings("unchecked") final Enumeration<? extends TreeNode> ac = actual  .children();

        int childIndex = 0;
        while (ec.hasMoreElements()) {
            assertTrue(ac.hasMoreElements());
            final TreeNode nextExpected = ec.nextElement();
            final TreeNode nextActual   = ac.nextElement();
            final int ci = childIndex;  // For lambda expression.
            final Supplier<String> message = () -> "getChildAt(" + ci + ')';
            assertSame(nextExpected, expected.getChildAt(childIndex), message);
            assertSame(nextActual,   actual  .getChildAt(childIndex), message);
            assertSame(expected, nextExpected.getParent());
            assertSame(actual,   nextActual  .getParent());
            assertSame(childIndex, expected.getIndex(nextExpected));
            assertSame(childIndex, actual  .getIndex(nextActual));
            n += assertTreeEquals(nextExpected, nextActual);
            childIndex++;
        }
        assertFalse(ac.hasMoreElements());
        assertEquals(expected.toString(), actual.toString());
        return n;
    }
}
