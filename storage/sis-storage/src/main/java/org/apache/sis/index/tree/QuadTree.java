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

import java.util.stream.Stream;
import java.util.function.Function;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.stream.StreamSupport;
import org.apache.sis.util.ArgumentChecks;


/**
 * Implementation of QuadTree Index.
 * See {@link KDTree} for a note about thread-safety.
 *
 * <p><b>References:</b></p>
 * Insertion algorithm implemented based on design of QuadTree index
 * in H. Samet, The Design and Analysis of Spatial Data Structures.
 * Massachusetts: Addison Wesley Publishing Company, 1989.
 *
 * @author  Chris Mattmann
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @param  <E>  the type of elements stored in this tree.
 *
 * @since 0.1
 * @module
 */
public final class QuadTree<E> extends KDTree<E> {
    /**
     * The root node of this QuadTree.
     */
    final QuadTreeNode root;

    /**
     * The maximal capacity of each node in this tree. Should be a relatively small number.
     * If the number of elements in a leaf node exceeds this capacity, then the node will be
     * transformed into a parent node with 4 children.
     */
    private final int capacity;

    /**
     * Number of elements in this QuadTree.
     */
    private int count;

    /**
     * Coordinates of the point in the center of the area of interest.
     */
    final double centerX, centerY;

    /**
     * Size of the area of interest.
     */
    final double width, height;

    /**
     * The function computing a position for an arbitrary element of this tree.
     */
    final Function<? super E, ? extends Point2D> evaluator;

    /**
     * Creates an initially empty QuadTree with the given capacity for each node.
     * The capacity should be a relatively small number. If the number of elements
     * in a leaf node exceeds this capacity, then the node will be transformed into
     * a parent node with 4 children.
     *
     * @param  bounds     bounds of the region of data to be inserted in the QuadTree.
     * @param  evaluator  function computing a position for an arbitrary element of this tree.
     * @param  capacity   the capacity of each node.
     */
    public QuadTree(final Rectangle2D bounds, final Function<? super E, ? extends Point2D> evaluator, final int capacity) {
        this.centerX   = bounds.getCenterX();
        this.centerY   = bounds.getCenterY();
        this.width     = bounds.getWidth();
        this.height    = bounds.getHeight();
        this.capacity  = Math.max(4, capacity);
        this.evaluator = evaluator;
        this.root      = new QuadTreeNode();
    }

    /**
     * Inserts the specified data into this QuadTree.
     *
     * @param  element  the element to insert.
     * @return always {@code true} in current implementation.
     */
    public boolean add(final E element) {
        ArgumentChecks.ensureNonNull("element", element);
        insert(root, centerX, centerY, width, height, element);
        count++;
        return true;
    }

    /**
     * Inserts the specified data into the given node. This method will iterate through node children
     * until a suitable node is found. This method may invoke itself recursively.
     *
     * @param  parent   the parent where to add the given data.
     * @param  cx       <var>x</var> coordinate in the center of {@code parent} node.
     * @param  cy       <var>y</var> coordinate in the center of {@code parent} node.
     * @param  dx       size of the {@code parent} node along the <var>x</var> axis.
     * @param  dy       size of the {@code parent} node along the <var>y</var> axis.
     * @param  element  the element to insert.
     */
    @SuppressWarnings("unchecked")
    private void insert(QuadTreeNode parent, double cx, double cy, double dx, double dy, final E element) {
        final Point2D position = evaluator.apply(element);
        final double x = position.getX();
        final double y = position.getY();
        for (;;) {
            final int q = QuadTreeNode.quadrant(x, y, cx, cy);
            final Object child = parent.getChild(q);
            if (child == null) {
                // New quadrant created in an existing parent (easy case).
                parent.setChild(q, new Object[] {element});
                break;
            }
            cx += QuadTreeNode.factorX(q) * (dx *= 0.5);        // Center and size of the child node.
            cy += QuadTreeNode.factorY(q) * (dy *= 0.5);        // TODO: use Math.fma with JDK9.
            if (child instanceof QuadTreeNode) {
                parent = (QuadTreeNode) child;                  // Continue until leaf node is found.
            } else {
                final Object[] data = (Object[]) child;
                final int n = data.length;
                if (n < capacity) {
                    final Object[] copy = new Object[n+1];
                    System.arraycopy(data, 0, copy, 0, n);
                    copy[n] = element;
                    parent.setChild(q, copy);
                    break;                                      // Leaf node can take the data — done.
                }
                /*
                 * Leaf can not add the given element because the leaf has reached its maximal capacity.
                 * Replace the leaf node by a parent node and add all previous elements into it. After
                 * data has been copied, continue attempts to insert the element given to this method.
                 */
                final QuadTreeNode branch = new QuadTreeNode();
                for (final Object e : data) {
                    insert(branch, cx, cy, dx, dy, (E) e);
                }
                parent.setChild(q, branch);
                parent = branch;
            }
        }
    }

    /**
     * Returns the number of elements in this tree.
     *
     * @return the number of elements in this tree.
     */
    public int size() {
        return count;
    }

    /**
     * Performs bounding box search.
     *
     * @param  searchRegion  envelope representing the rectangular search region.
     * @return elements that are within the given radius from the point.
     */
    public Stream<E> queryByBoundingBox(final Rectangle2D searchRegion) {
        return StreamSupport.stream(new NodeIterator<>(this, searchRegion), false);
        // TODO: make parallel after we tested most extensively.
    }
}
