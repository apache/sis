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

import java.util.Arrays;
import java.io.Serializable;


/**
 * A node in a {@link PointTree} which is the parent of other nodes. The number of child nodes depends
 * on the number of dimensions of the tree: 4 children with two-dimensional <cite>QuadTree</cite>,
 * 8 children with three-dimensional <cite>Octree</cite>, <i>etc</i>.
 * The child node can be another {@link PointTreeNode} if that node is itself the parent of more nodes.
 * Otherwise (i.e. if the child node is leaf) the child is an instance of {@code Object[]}.
 *
 * <p>Features of arbitrary types are stored in {@code Object[]} arrays. Those arrays should be small:
 * if the number of elements in a leaf exceeds a maximal capacity specified by the {@link PointTree},
 * then the leaf is replaced by a new {@link PointTreeNode} and the {@code Object[]} content
 * is distributed between the new child nodes.</p>
 *
 * <p>Addition of new features in {@code Object[]} arrays uses a <cite>copy-on-write</cite> strategy
 * in order to keep memory usage minimal (a tree may have thousands of small arrays) and for making
 * easier to ensure thread-safety during concurrent read/write operations.</p>
 *
 * <h2>Design note</h2>
 * Trees may have huge amount of nodes. For that reason, the nodes should contain as few fields as possible.
 * We should also avoid classes that are just wrappers around arrays. This is the reason why leaf nodes are
 * stored directly as {@link Object[]} arrays instead of using a more object-oriented approach with some
 * {@code TreeNodeLeaf} class.
 *
 * @author  Chris Mattmann
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
abstract class PointTreeNode implements Cloneable, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5911043832415017844L;

    /**
     * Constructs an initially empty {@link PointTree} node.
     */
    PointTreeNode() {
    }

    /**
     * Returns the quadrant/octant relative to the given point.
     * Each bit specifies the relative position for a dimension.
     * For (<var>x</var>, <var>y</var>, <var>z</var>, <var>t</var>) coordinates, the pattern is:
     *
     * <ul>
     *   <li>Bit 0 is the relative position of <var>x</var> coordinate: 0 for East   and 1 for West.</li>
     *   <li>Bit 1 is the relative position of <var>y</var> coordinate: 0 for North  and 1 for South.</li>
     *   <li>Bit 2 is the relative position of <var>z</var> coordinate: 0 for up     and 1 for down.</li>
     *   <li>Bit 3 is the relative position of <var>t</var> coordinate: 0 for future and 1 for past.</li>
     *   <li><i>etc.</i> for any additional dimensions.</li>
     * </ul>
     *
     * @param  point   data (<var>x</var>, <var>y</var>, …) coordinate.
     * @param  region  region of current node, as the center in first half and size in second half.
     * @return an identification of the quadrant where the given point is located.
     */
    static int quadrant(final double[] point, final double[] region) {
        int q = 0;
        for (int i = region.length >>> 1; --i >= 0;) {          // Iterate only over center coordinates.
            if (point[i] < region[i]) {
                q |= (1 << i);
            }
        }
        return q;
    }

    /**
     * Modifies in place the specified for describing the coordinates of the specified quadrant.
     *
     * @param  region    region of current node, as the center in first half and size in second half.
     * @param  quadrant  the quadrant as computed by {@link #quadrant(double[], double[])}.
     */
    static void enterQuadrant(final double[] region, final int quadrant) {
        final int n = region.length >>> 1;
        for (int i = n; --i >= 0;) {
            region[i] += factor(quadrant, i) * (region[i+n] *= 0.5);    // TODO: use Math.fma with JDK9.
        }
    }

    /**
     * Returns 0.5 if the given quadrant is in the East/North/Up/Future side,
     * or -0.5 if in the West/South/Down/Past side.
     *
     * @param  quadrant   the quadrant as computed by {@link #quadrant(double[], double[])}.
     * @param  dimension  the dimension index: 0 for <var>x</var>, 1 for <var>y</var>, <i>etc.</i>
     */
    static double factor(final int quadrant, final int dimension) {
        /*
         * The 3FE0000000000000 long value is the bit pattern of 0.5. The leftmost bit at (Long.SIZE - 1)
         * is the sign, which we set to the sign encoded in the quadrant value. This approach allow us to
         * get the value efficiently, without jump instructions.
         */
        return Double.longBitsToDouble(0x3FE0000000000000L |
                (((long) (quadrant & (1 << dimension))) << (Long.SIZE - 1 - dimension)));
    }

    /**
     * Removes all elements from this node.
     *
     * @see PointTree#clear()
     */
    abstract void clear();

    /**
     * Returns the child of this node that resides in the specified quadrant/octant.
     * The return value can be null or an instance of one of those two classes:
     *
     * <ul>
     *   <li>Another {@link PointTreeNode} if the node in a quadrant/octant is itself a parent of other children.</li>
     *   <li>{@code Object[]} if the node in a quadrant/octant is a leaf. In such case, the array contains elements.
     *       We do not wrap the leaf in another {@link PointTreeNode} for reducing the number of objects created.</li>
     * </ul>
     *
     * Any other kind of object is an error.
     *
     * @param  quadrant  quadrant/octant of child to get.
     * @return child in the specified quadrant/octant.
     * @throws IndexOutOfBoundsException if the specified quadrant/octant is out of bounds.
     */
    abstract Object getChild(int quadrant);

    /**
     * Sets the node's quadrant/octant to the specified child.
     * The {@code child} value must be one of the types documented in {@link #getChild(int)}.
     *
     * @param quadrant  quadrant/octant where the child resides.
     * @param child     child of this node in the specified quadrant/octant.
     * @throws IndexOutOfBoundsException if the specified quadrant/octant is out of bounds.
     */
    abstract void setChild(int quadrant, Object child);

    /**
     * Creates a new instance of the same class than this node.
     */
    abstract PointTreeNode newInstance();

    /**
     * Returns a clone of this node. This is invoked when creating a copy of {@link PointTree}.
     * The clone is a semi-deep clone: all children that are instances of {@link PointTreeNode}
     * shall be cloned recursively, but instances of {@code Object[]} (the leaf nodes) are not
     * cloned. It is safe to not clone the {@code Object[]} arrays because {@link PointTree}
     * uses a copy-on-write strategy when data need to be modified.
     */
    @Override
    protected Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Default implementation of {@link PointTreeNode} when no specialized class is available.
     * This default implementation stores children in an array. The usage of arrays allows
     * arbitrary lengths, but implies one more object to be created for each node instance.
     * Since this class should be used only for relatively large numbers of dimensions,
     * the cost of arrays creation should be less significant compared to array length.
     */
    static final class Default extends PointTreeNode {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 5726750714534959859L;

        /**
         * The nodes or element values in each quadrant/octant of this node.
         * Each array element can be null or an instance of one of the classes
         * documented in {@link PointTreeNode#getChild(int)}.
         */
        private final Object[] children;

        /**
         * Constructs an initially empty {@link PointTree} node.
         *
         * @param  n  must be 2<sup>k</sup> where <var>k</var> is the number of dimensions.
         */
        Default(final int n) {
            children = new Object[n];
        }

        /**
         * Creates a new node initialized to a copy of the given node.
         *
         * @see #clone()
         */
        private Default(final Default other) {
            children = other.children.clone();
            for (int i=0; i<children.length; i++) {
                final Object value = children[i];
                if (value instanceof PointTreeNode) {
                    children[i] = ((PointTreeNode) value).clone();
                }
                // Do not clone arrays because we use them as copy-on-write data structures.
            }
        }

        /**
         * Creates a new instance of the same class than this node.
         */
        @Override
        final PointTreeNode newInstance() {
            return new Default(children.length);
        }

        /**
         * Removes all elements from this node.
         */
        @Override
        final void clear() {
            Arrays.fill(children, null);
        }

        /**
         * Returns the child of this node that resides in the specified quadrant/octant.
         *
         * @param  quadrant  quadrant/octant of child to get.
         * @return child in the specified quadrant/octant.
         */
        @Override
        final Object getChild(final int quadrant) {
            return children[quadrant];
        }

        /**
         * Sets the node's quadrant/octant to the specified child.
         *
         * @param quadrant   quadrant/octant where the child resides.
         * @param child      child of this node in the specified quadrant/octant.
         */
        @Override
        final void setChild(final int quadrant, final Object child) {
            children[quadrant] = child;
        }

        /**
         * Returns a clone of this node. This is invoked when creating a copy of {@link PointTree}.
         */
        @Override
        @SuppressWarnings("CloneDoesntCallSuperClone")
        protected Object clone() {
            return new Default(this);
        }
    }
}
