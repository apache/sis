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
package org.apache.sis.index.tree;


/**
 * A node in a {@link KDTree} which is the parent of other nodes. The number of child nodes depends on
 * the number of dimensions of the tree: 4 children with two-dimensional {@link QuadTree}, 8 children
 * with three-dimensional {@code Octree}, <i>etc</i>. The child node can be another {@link KDTreeNode}
 * if that node is itself the parent of more nodes. Otherwise (i.e. if the child node is leaf) the child
 * is an instance of {@code Object[]}.
 *
 * <p>Features of arbitrary types are stored in {@code Object[]} arrays. Those arrays should be small:
 * if the number of elements in a leaf exceeds a maximal capacity specified by the {@link KDTree},
 * then the leaf is replaced by a new {@link KDTreeNode} and the {@code Object[]} content is distributed
 * between the new child nodes.</p>
 *
 * <p>Addition of new features in {@code Object[]} arrays uses a <cite>copy-on-write</cite> strategy
 * in order to keep memory usage minimal (a tree may have thousands of small arrays) and for making
 * easier to ensure thread-safety during concurrent read/write operations.</p>
 *
 * <h2>Design note</h2>
 * Trees may have huge amount of nodes. For that reason, the nodes should contain as few fields as possible.
 * We should also avoid classes that are just wrappers around arrays. This is the reason why leaf nodes are
 * stored directly as {@link Object[]} arrays instead than using a more object-oriented approach with some
 * {@code TreeNodeLeaf} class.
 *
 * @author  Chris Mattmann
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
abstract class KDTreeNode {
    /**
     * Constructs an initially empty {@link KDTree} node.
     */
    KDTreeNode() {
    }

    /**
     * Default implementation of {@link KDTreeNode} when no specialized class is available.
     * This default implementation stores children in an array. The usage of arrays allows
     * arbitrary lengths, but implies one more object to be created for each node instance.
     * Since this class should be used only for relatively large numbers of dimensions,
     * the cost of arrays creation should be less significant compared to array length.
     */
    private static final class Default extends KDTreeNode {
        /**
         * The nodes or element values in each quadrant/octant of this node.
         * Each array element can be null or an instance of one of those two classes:
         *
         * <ul>
         *   <li>Another {@link KDTreeNode} if the node in a quadrant/octant is itself a parent of other children.</li>
         *   <li>{@code Object[]} if the node in a quadrant/octant is a leaf. In such case, the array contains elements.
         *       We do not wrap the leaf in another {@link KDTreeNode} for reducing the number of objects created.</li>
         * </ul>
         */
        private final Object[] children;

        /**
         * Constructs an initially empty {@link KDTree} node.
         *
         * @param  n  must be 2<sup>k</sup> where <var>k</var> is the number of dimensions.
         */
        Default(final int n) {
            children = new Object[n];
        }
    }
}
