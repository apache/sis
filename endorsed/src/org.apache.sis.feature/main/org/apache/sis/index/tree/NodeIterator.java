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
import java.util.Spliterator;
import java.util.function.Consumer;
import org.opengis.geometry.Envelope;
import org.apache.sis.util.internal.shared.Numerics;


/**
 * An iterator over the elements contained in a {@link PointTreeNode}.
 * The iterator applies a first filtering of elements by traversing only the nodes that <em>may</em>
 * intersect the Area Of Interest (AOI). But after a node has been retained, an additional check for
 * inclusion may be necessary. That additional check is performed by {@link #filter(Object)} and can
 * be overridden by subclasses.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <E>  the type of elements stored in the {@link PointTree}.
 */
@SuppressWarnings("CloneableImplementsClone")
class NodeIterator<E> implements Spliterator<E>, Cloneable {
    /**
     * Sentinel value meaning that iteration is over.
     */
    private static final Object[] FINISHED = new Object[0];

    /**
     * The function computing a position for an arbitrary element of the tree.
     */
    private final PointTree.Locator<? super E> locator;

    /**
     * A mask with a bit set for all quadrants. This is used as the initial value of
     * {@link Cursor#quadrants} before to clear to bits of quadrants to not search.
     */
    private final long bitmask;

    /**
     * The region on which to iterate. The first half are minimal coordinates and the second
     * half are maximal coordinates. The content of this array should not be modified.
     */
    private final double[] bounds;

    /**
     * A pre-allocated buffer where to store point coordinates, or {@code null} if not needed.
     */
    private double[] point;

    /**
     * The object to use for updating the {@link #current} field, or {@code null} if none.
     * This {@code NodeIterator} starts by returning elements in the {@link #current} array.
     * When iteration over {@link #current} array is finished, {@code NodeIterator} uses the
     * {@link Cursor} for changing the array reference to the next array. The iteration then
     * continues until all leaf nodes intersecting the search region have been traversed.
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
     * Creates a new iterator for the specified search region.
     * If the given region is null, then infinite bounds are assumed.
     */
    NodeIterator(final PointTree<E> tree, final Envelope searchRegion) {
        final int n = tree.getDimension();
        bitmask = Numerics.bitmask(1 << n) - 1;
        bounds  = new double[n*2];
        if (searchRegion != null) {
            point = new double[n];
            for (int i = n; --i >= 0;) {
                bounds[i]   = searchRegion.getMinimum(i);
                bounds[i+n] = searchRegion.getMaximum(i);
            }
        } else {
            Arrays.fill(bounds, 0, n,   Double.NEGATIVE_INFINITY);
            Arrays.fill(bounds, n, n*2, Double.POSITIVE_INFINITY);
        }
        locator = tree.locator;
        cursor  = new Cursor<>(tree.treeRegion);
        cursor.node = tree.root;
        cursor.findIntersections(this);
        current = next();
    }

    /**
     * Invoked after {@link #clone()} for copying the fields that cannot be shared between two
     * {@link NodeIterator} instances. This is used for {@link #trySplit()} implementation.
     *
     * @param  quadrants  the value to assign to {@link Cursor#quadrants}.
     *         That bitmask shall not intersect the bitmask of {@code other.cursor}.
     * @return whether this iterator has to data to iterate.
     */
    private boolean postClone(final long quadrants) {
        final Cursor<E> c = cursor;
        if (point != null) {
            point = new double[point.length];
        }
        cursor            = new Cursor<>(c.region);
        cursor.parent     = c.parent;
        cursor.node       = c.node;
        cursor.quadrants  = quadrants;
        recycle           = null;
        nextIndex         = 0;
        current           = next();
        return (current != null);
    }

    /**
     * A provider for arrays of elements of child nodes contained in a {@link PointTreeNode}.
     * The starting point is {@link PointTree#root}. A new {@link Cursor} instance is created
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
        PointTreeNode node;

        /**
         * Bitmask of quadrants/octants on which to iterate. Quadrants/octants are iterated from rightmost
         * bit to leftmost bit. Bits are cleared when the corresponding quadrant/octant become the current
         * one. A value of 0 means that there are no more quadrants to iterate for the {@linkplain #node}.
         *
         * <p><b>Note:</b> we take "quadrant" name from QuadTree, but this algorithm can actually be used
         * with more dimensions.</p>
         *
         * @see PointTree#MAXIMUM_DIMENSIONS
         */
        long quadrants;

        /**
         * (<var>x</var>,<var>y</var>,…) coordinates in the center of {@link #node} node,
         * followed by (<var>Δx</var>,<var>Δy</var>,…) sizes of the {@link #node} node.
         */
        private final double[] region;

        /**
         * Creates a new cursor. It is caller responsibility to initialize the {@link #node} field, the
         * {@link #parent} field if a parent exists, and to invoke {@link #findIntersections(NodeIterator)}.
         */
        Cursor(final double[] region) {
            this.region = region.clone();       // Must be cloned because this class may modify those values.
        }

        /**
         * Computes a bitmask of all quadrants/octants that intersect the search area. This method
         * must be invoked after {@link #region} values changed.
         *
         * @todo the computation performed in this method is not necessary when the caller knows that
         *       the node is fully included in the AOI. We should carry a flag for this common case.
         *
         * @param  it  the iterator which defines the area of interest.
         */
        final void findIntersections(final NodeIterator<E> it) {
            final double[] bounds = it.bounds;
            quadrants = it.bitmask;                                 // Bits initially set for all quadrants.
            final int n = bounds.length >>> 1;
            for (int i = n; --i >= 0;) {
                final double c = region[i];
                /*
                 * Example: given xmin and xmax the bounds of the search region in dimension of x,
                 * and cx the x coordinate of the center of current node, then:
                 *
                 *   if !(xmin <= cx) then we want to clear the bits of all quadrants on West side.
                 *   if !(xmax >= cx) then we want to clear the bits of all quadrants on East side.
                 *
                 * Quadrants on the East side are all quadrants with a number where bit 0 is unset.
                 * Those quadrants are 0, 2, 4, 6, etc. Conversely quadrants on the West side have
                 * bit 0 set. They are 1, 3, 5, 7, etc.
                 *
                 * Applying the same rational on y:
                 *
                 *   if !(ymin <= cy) then we want to clear bits of all quadrants on South side.
                 *   if !(ymax >= cy) then we want to clear bits of all quadrants on North side.
                 *
                 * Quadrants on the North side have bit 1 unset:
                 */
                if (!(bounds[i]   <= c)) quadrants &=  CLEAR_MASKS[i];      // Use '!' for catching NaN.
                if (!(bounds[i+n] >= c)) quadrants &= ~CLEAR_MASKS[i];
            }
        }

        /**
         * Masks for clearing the bits of all quadrants that do not intersect the search region on the left side.
         * For example, for <var>x</var> dimension, this is the mask to apply if the {@code xmin <= cx} condition
         * is false. In this example {@code CLEAR_MASKS[0]} clears the bits of all quadrants on the West side,
         * which are quadrants 1, 3, 5, <i>etc.</i>
         *
         * <p>The index in this array is the dimension in which the quadrant do not intersect the search region.
         * The length of this array should be equal to {@link PointTree#MAXIMUM_DIMENSIONS}.</p>
         */
        private static final long[] CLEAR_MASKS = {
            0b0101010101010101010101010101010101010101010101010101010101010101L,
            0b0011001100110011001100110011001100110011001100110011001100110011L,
            0b0000111100001111000011110000111100001111000011110000111100001111L,
            0b0000000011111111000000001111111100000000111111110000000011111111L,
            0b0000000000000000111111111111111100000000000000001111111111111111L,
            0b0000000000000000000000000000000011111111111111111111111111111111L
        };

        /**
         * Creates a new {@code Cursor} for getting element arrays in the {@linkplain #node} quadrant/octant,
         * without changing the state of this {@code Cursor}. This method is invoked when there is a need to
         * iterate in two more more children, in which case we cannot yet discard the information contained
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
                c = new Cursor<>(region);
            } else {
                it.recycle = c.parent;
                System.arraycopy(region, 0, c.region, 0, region.length);
            }
            PointTreeNode.enterQuadrant(c.region, q);
            c.parent = this;
            return c;
        }

        /**
         * Changes the state of this {@code Cursor} for getting elements in the specified quadrant/octant.
         * This method is invoked when there is no need to keep current {@code Cursor} information anymore,
         * because there are no other quadrants/octants than the specified one in which to iterate.
         *
         * <p>Caller is responsible to update the {@link #node} field after this method call
         * and to invoke {@link #findIntersections(NodeIterator)}.</p>
         *
         * @param  q  the quadrant/octant in which to iterate.
         */
        final void moveDown(final int q) {
            PointTreeNode.enterQuadrant(region, q);
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
     * Returns an array of elements that <em>may</em> intersect the search region,
     * or {@link #FINISHED} if the iteration is finished. This method does not verify
     * if the points are really in the search region; callers may need to filter the
     * returned array.
     *
     * @return array of elements that may be in the search region,
     *         or {@link #FINISHED} if the iteration is finished.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    private Object[] next() {
        while (cursor != null) {
            while (cursor.quadrants != 0) {
                final int q = Long.numberOfTrailingZeros(cursor.quadrants);
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
                    cursor.node = (PointTreeNode) child;
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
            if (filter(element)) {
                action.accept(element);
                return true;
            }
        }
    }

    /**
     * Returns whether the given element is included in the search region.
     * The default implementation verifies if the point is included in the bounding box.
     * Subclasses may override, for example for restricting the points to a radius around a central location.
     *
     * @param  element  the element to check for inclusion.
     * @return whether the element is in the search region.
     */
    protected boolean filter(final E element) {
        locator.getPositionOf(element, point);
        final int n = bounds.length >>> 1;
        for (int i = n; --i >= 0;) {
            final double p = point[i];
            if (!(p >= bounds[i] && p <= bounds[i+n])) {            // Use '!' for catching NaN.
                return false;
            }
        }
        return true;
    }

    /**
     * If this iterator can be partitioned, returns an iterator covering about half of the elements.
     * Otherwise returns {@code null}.
     */
    @Override
    public final Spliterator<E> trySplit() {
        final Cursor<E> c = cursor;
        if (c != null) {
            long half = 0;
            for (int n = Long.bitCount(c.quadrants) / 2; n >= 0; --n) {
                final long q = Long.lowestOneBit(c.quadrants);
                c.quadrants &= ~q;
                half |= q;
            }
            if (half != 0) try {
                @SuppressWarnings("unchecked")
                final NodeIterator<E> second = (NodeIterator<E>) clone();
                if (second.postClone(half)) return second;
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
            // TODO: go down in the tree and explore other nodes.
        }
        return null;
    }

    /**
     * Returns an estimate of the number of elements or {@link Long#MAX_VALUE} if too expensive to compute.
     *
     * @todo Compute an estimated size by multiplying {@link PointTree#count} by the percentage of bits set
     *       in {@link Cursor#quadrants} (use {@link #bitmask} for the number of bits in the 100% case).
     */
    @Override
    public long estimateSize() {
        return Long.MAX_VALUE;
    }

    /**
     * Returns a set of characteristics of this iterator and its elements.
     */
    @Override
    public int characteristics() {
        return DISTINCT | NONNULL;
    }
}
