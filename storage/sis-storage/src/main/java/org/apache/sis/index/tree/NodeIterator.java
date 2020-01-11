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

import java.util.Spliterator;
import java.util.function.Consumer;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.function.Function;


/**
 * An iterator over the elements contained in a {@link KDTreeNode}.
 * The iterator applies a first filtering of elements by traversing only the nodes that <em>may</em>
 * intersect the Area Of Interest (AOI). But after a node has been retained, an additional check for
 * inclusion may be necessary. That additional check is performed by {@link #filter(Point2D)}
 * and can be overridden by subclasses.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @param  <E>  the type of elements stored in the {@link QuadTree}.
 *
 * @since 1.1
 * @module
 */
class NodeIterator<E> implements Spliterator<E> {
    /**
     * Sentinel value meaning that iteration is over.
     */
    private static final Object[] FINISHED = new Object[0];

    /**
     * The function computing a position for an arbitrary element of this tree.
     */
    private final Function<? super E, ? extends Point2D> evaluator;

    /**
     * The region on which to iterate.
     */
    private final double xmin, ymin, xmax, ymax;

    /**
     * The object to use for updating the {@link #current} field, or {@code null} if none.
     * This {@code NodeIterator} starts by returning elements in the {@link #current} array.
     * When iteration over {@link #current} array is finished, {@code NodeIterator} uses the
     * {@link Cursor} for changing the array reference to the next array. The iteration then
     * continues until all leaf nodes intersecting the area of interest (AOI) have been traversed.
     */
    private Cursor<E> cursor;

    /**
     * The elements for a current quadrant/octant in current node. After iteration over this
     * array is finished, this field is updated with elements for next quadrant/octant until
     * all nodes have been traversed.
     */
    private Object[] current;

    /**
     * Index of the next element to return from the {@link #current} array.
     */
    private int nextIndex;

    /**
     * A cursor that can be recycled, or {@code null} if none. Used for reducing the number
     * of {@link Cursor} allocations during tree traversal.
     */
    private Cursor<E> recycle;

    /**
     * Creates a new iterator for the specified bounding box.
     */
    @SuppressWarnings("ThisEscapedInObjectConstruction")
    NodeIterator(final QuadTree<E> tree, final Rectangle2D region) {
        evaluator = tree.evaluator;
        xmin   = region.getMinX();
        ymin   = region.getMinY();
        xmax   = region.getMaxX();
        ymax   = region.getMaxY();
        cursor = new Cursor<>(tree);
        cursor.findIntersections(this);
        current = next();
    }

    /**
     * A provider for arrays of elements of child nodes contained in a {@link KDTreeNode}.
     * The starting point is {@link QuadTree#root}. A new {@link Cursor} instance is created
     * for each level when the node at next level is itself a parent of at least two nodes
     * (if there is only one child node, then this implementation takes a shortcut).
     */
    private static final class Cursor<E> {
        /**
         * The cursor that created this cursor, or {@code null} if none. If non-null, we will
         * continue iteration with that parent after this {@link Cursor} finished to return
         * all element arrays.
         *
         * <p>If this {@link Cursor} instance is not used anymore (for now) and is made available
         * for recycling, then this field is instead used for storing the next recyclable instance
         * that may be used after this one, with no child-parent relationship.</p>
         */
        private Cursor<E> parent;

        /**
         * The node for which to iterate over elements. Only the quadrants/octants identified
         * by the {@link #quadrants} bitmask will be traversed.
         */
        QuadTreeNode node;

        /**
         * Bitmask of quadrants/octants on which to iterate. Quadrants/octants are iterated from rightmost
         * bit to leftmost bit. Bits are cleared when the corresponding quadrant/octant become the current
         * one. A value of 0 means that there is no more quadrant to iterate for the {@linkplain #node}.
         *
         * <p><b>Note:</b> we take "quadrant" name from {@link QuadTree}, but this algorithm can actually
         * be used with more dimensions.</p>
         */
        int quadrants;

        /**
         * (<var>x</var>,<var>y</var>) coordinate in the center of {@link #node} node,
         * and (<var>Δx</var>,<var>Δy</var>) size of the {@link #node} node.
         */
        private double cx, cy, dx, dy;

        /**
         * Creates a new cursor with all values initialized to zero.
         * It is caller responsibility to initialize all fields.
         */
        private Cursor() {
        }

        /**
         * Creates a new cursor initialized to the root node of the given tree.
         */
        Cursor(final QuadTree<E> tree) {
            node = tree.root;
            cx   = tree.centerX;
            cy   = tree.centerY;
            dx   = tree.width;
            dy   = tree.height;
        }

        /**
         * Computes a bitmask of all quadrants/octants that intersect the search area. This method
         * must be invoked after {@link #cx}, {@link #cy}, {@link #dx}, {@link #dy} fields changed.
         *
         * @todo the computation performed in this method is not necessary when the caller knows that
         *       the node is fully included in the AOI. We should carry a flag for this common case.
         *
         * @param  it  the iterator which defines the area of interest.
         */
        final void findIntersections(final NodeIterator<E> it) {
            quadrants = (1 << 4) - 1;                                                               // Bits initially set for all quadrants.
            if (!(it.xmin <= cx)) quadrants &= ~((1 << QuadTreeNode.NW) | (1 << QuadTreeNode.SW));  // Clear bits of all quadrant on West side.
            if (!(it.xmax >= cx)) quadrants &= ~((1 << QuadTreeNode.NE) | (1 << QuadTreeNode.SE));  // Clear bits of all quadrant on East side.
            if (!(it.ymin <= cy)) quadrants &= ~((1 << QuadTreeNode.SE) | (1 << QuadTreeNode.SW));  // Clear bits of all quadrant on South side.
            if (!(it.ymax >= cy)) quadrants &= ~((1 << QuadTreeNode.NE) | (1 << QuadTreeNode.NW));  // Clear bits of all quadrant on North side.
        }

        /**
         * Creates a new {@code Cursor} for getting element arrays in the {@linkplain #node} quadrant/octant,
         * without changing the state of this {@code Cursor}. This method is invoked when there is a need to
         * iterate in two more more children, in which case we can not yet discard the information contained
         * in this {@code Cursor} instance.
         *
         * <p>Caller is responsible to update the {@link #node} field after this method call
         * and to invoke {@link #findIntersections(NodeIterator)}.</p>
         *
         * @param  it  the enclosing iterator.
         * @param  q   the quadrant/octant in which to iterate.
         * @return the cursor to use for getting element arrays in the specified quadrant/octant.
         */
        final Cursor<E> push(final NodeIterator<E> it, final int q) {
            Cursor<E> c = it.recycle;
            if (c == null) {
                c = new Cursor<>();
            } else {
                it.recycle = c.parent;
            }
            c.parent = this;
            c.cx = cx + QuadTreeNode.factorX(q) * (c.dx = dx * 0.5);      // TODO: use Math.fma with JDK9.
            c.cy = cy + QuadTreeNode.factorY(q) * (c.dy = dy * 0.5);
            return c;
        }

        /**
         * Changes the state of this {@code Cursor} for getting elements in the specified quadrant/octant.
         * This method is invoked when there is no need to keep current {@code Cursor} information anymore,
         * because there is no other quadrant/octant than the specified one in which to iterate.
         *
         * <p>Caller is responsible to update the {@link #node} field after this method call
         * and to invoke {@link #findIntersections(NodeIterator)}.</p>
         *
         * @param  q  the quadrant/octant in which to iterate.
         */
        final void moveDown(final int q) {
            cx += QuadTreeNode.factorX(q) * (dx *= 0.5);        // Center and size of the child node.
            cy += QuadTreeNode.factorY(q) * (dy *= 0.5);
        }

        /**
         * Marks this {@link Cursor} as available for recycling and returns its parent.
         *
         * @param  it  the enclosing iterator.
         * @return the parent of this {@code Cursor}, or {@code null} if none.
         */
        final Cursor<E> getParentAndRecycle(final NodeIterator<E> it) {
            final Cursor<E> p = parent;
            parent = it.recycle;
            it.recycle = this;
            return p;
        }
    }

    /**
     * Returns an array of elements that <em>may</em> intersect the area of interest,
     * or {@link #FINISHED} if the iteration is finished. This method does not verify
     * if the points are really in the AOI; callers may need to filter the returned array.
     *
     * @return array of elements that may be in the area of interest (AOI),
     *         or {@link #FINISHED} if the iteration is finished.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    private Object[] next() {
        while (cursor != null) {
            while (cursor.quadrants != 0) {
                final int q = Integer.numberOfTrailingZeros(cursor.quadrants);
                cursor.quadrants &= ~(1 << q);
                final Object child = cursor.node.getChild(q);
                if (child != null) {
                    if (child instanceof Object[]) {
                        return (Object[]) child;
                    }
                    /*
                     * Need to iterate down in at least one child. If the `quadrants` mask said that there is
                     * maybe other children to check, build a chain of cursors for following the iterators of
                     * all children.
                     */
                    if (cursor.quadrants != 0) {
                        cursor = cursor.push(this, q);
                    } else {
                        /*
                         * At this point we know that there is exactly one child. Redo the same analysis on
                         * that unique child. This allows us to avoid consuming stack with recursive method
                         * calls. This case is common at least at beginning of search, when the tree nodes
                         * are still much bigger than the AOI.
                         */
                        cursor.moveDown(q);
                    }
                    cursor.node = (QuadTreeNode) child;
                    cursor.findIntersections(this);
                }
            }
            /*
             * No more intersection in this node. Continue with parent node.
             */
            cursor = cursor.getParentAndRecycle(this);
        }
        return FINISHED;
    }

    /**
     * If a remaining element exists, performs the given action on it and returns {@code true}.
     * Otherwise returns {@code false}.
     *
     * @param  action  the action to execute on the next element.
     * @return {@code false} if no remaining elements exist.
     */
    @Override
    public final boolean tryAdvance(final Consumer<? super E> action) {
        for (;;) {
            while (nextIndex >= current.length) {
                if (current == FINISHED) {
                    return false;
                }
                current = next();
                nextIndex = 0;
            }
            @SuppressWarnings("unchecked")
            final E element = (E) current[nextIndex++];
            if (filter(evaluator.apply(element))) {
                action.accept(element);
                return true;
            }
        }
    }

    /**
     * Returns whether the given position is included in the search region.
     * The default implementation verifies if the point is included in the bounding box.
     * Subclasses may override, for example for restricting the points to a radius around a central location.
     *
     * @param  position  the position to check for inclusion.
     * @return whether the position is in the search region.
     */
    protected boolean filter(final Point2D position) {
        final double x = position.getX();
        if (x >= xmin && x <= xmax) {
            final double y = position.getY();
            return (y >= ymin && y <= ymax);
        }
        return false;
    }

    /**
     * If this iterator can be partitioned, returns an iterator covering a strict prefix of the elements.
     *
     * @todo Checks {@link Cursor#quadrants} and take half of the bits.
     */
    @Override
    public final Spliterator<E> trySplit() {
        return null;
    }

    /**
     * Returns an estimate of the number of elements or {@link Long#MAX_VALUE} if too expensive to compute.
     */
    @Override
    public final long estimateSize() {
        return Long.MAX_VALUE;
    }

    /**
     * Returns a set of characteristics of this iterator and its elements.
     */
    @Override
    public final int characteristics() {
        return ORDERED | NONNULL;
    }
}
