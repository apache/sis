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

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.function.Function;
import org.opengis.geometry.Envelope;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;


/**
 * A <var>k</var>-dimensional tree index for points.
 * For <var>k</var>=2, this is a <cite>point QuadTree</cite>.
 * For <var>k</var>=3, this is a point <cite>Octree</cite>.
 * Higher dimensions are also accepted up to {@value #MAXIMUM_DIMENSIONS} dimensions.
 * Elements are stored in this {@code PointTree} as arbitrary non-null objects with their coordinates
 * computed by a user-specified {@code locator} function. That function expects an element {@code E}
 * in argument and returns its coordinates as a {@code double[]} array.
 * The coordinates of each elements must be <em>stable</em>, i.e. applying the {@code locator} function
 * twice on the same element must return the same coordinates.
 * Searches based on element coordinates can be done with the following methods:
 *
 * <ul>
 *   <li>{@link #queryByBoundingBox(Envelope)}</li>
 * </ul>
 *
 * The performances of this {@code PointTree} depends on two parameters: an estimated bounding box of the points
 * to be added in this tree and a maximal capacity of leaf nodes (not to be confused with a capacity of the tree).
 * More details are given in the {@linkplain #PointTree(Envelope, Function, int) constructor}.
 *
 * <h2>Thread-safety</h2>
 * This class is not thread-safe when the tree content is modified. But if the tree is kept unmodified
 * after construction, then multiple read operations in concurrent threads are safe. Users can synchronize
 * with {@link java.util.concurrent.locks.ReadWriteLock} (the read lock must be held for complete duration
 * of iterations or stream consumptions).
 *
 * <h2>Serialization</h2>
 * This tree is serializable if all elements in the tree are also serializable. However the serialization
 * details is implementation specific and may change in any future Apache SIS version.
 *
 * <h2>References:</h2>
 * Insertion algorithm is based on design of QuadTree index in H. Samet,
 * <u>The Design and Analysis of Spatial Data Structures</u>.
 * Massachusetts: Addison Wesley Publishing Company, 1989.
 *
 * @author  Chris Mattmann
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @param  <E>  the type of elements stored in this tree.
 *
 * @since 1.1
 * @module
 */
public class PointTree<E> extends AbstractSet<E> implements Serializable {
    /**
     * The maximum number of dimensions (inclusive) that this class currently supports.
     * Current maximum is {@value}. This restriction come from 2⁶ = {@value Long#SIZE}.
     */
    public static final int MAXIMUM_DIMENSIONS = 6;

    /**
     * The root node of this <var>k</var>-dimensional tree.
     */
    final PointTreeNode root;

    /**
     * The maximal capacity of each node in this tree. It should be a relatively small number.
     * If the number of elements in a leaf node exceeds this capacity, then the node will be
     * transformed into a parent node with children nodes.
     */
    private final int nodeCapacity;

    /**
     * Number of elements in this <var>k</var>-dimensional tree.
     * This is used as an unsigned integer (we do not expect to have as many elements,
     * but in this case it is easy to get the extra bit provided by unsigned integer).
     */
    private long count;

    /**
     * The regions covered by this tree, encoded as a center and a size.
     * This array contains two parts:
     *
     * <ul>
     *   <li>The first half contains coordinates of the point in the center of the region covered by this tree.</li>
     *   <li>The second half contains the size of the region along each dimension.</li>
     * </ul>
     *
     * The length of this array is two times the number of dimensions of points in this tree.
     * This array content should not be modified, until the entire tree is rebuilt.
     */
    final double[] treeRegion;

    /**
     * The function computing a position for an arbitrary element in this tree.
     * The length of arrays computed by this locator must be equal to {@link #getDimension()}.
     */
    final Function<? super E, double[]> locator;

    /**
     * Creates an initially empty <var>k</var>-dimensional tree with the given capacity for each node.
     * The number of dimensions of the given envelope determines the number of dimensions of points in this tree.
     * The positions computed by {@code locator} must have the same number of dimensions than the given envelope.
     *
     * <p>The {@code bounds} argument specifies the expected region of points to be added in this {@code PointTree}.
     * Those bounds do not need to be exact; {@code PointTree} will work even if some points are located outside
     * those bounds. However performances will be better if the {@linkplain Envelope#getMedian(int) envelope center}
     * is close to the median of the points to be inserted in the {@code PointTree}, and if the majority of points
     * are inside those bounds.</p>
     *
     * <p>The given {@code nodeCapacity} is a threshold value controlling when the content of a node should
     * be splited into smaller children nodes. That capacity should be a relatively small number,
     * for example 10. Determining the most efficient value may require benchmarking.</p>
     *
     * @param  bounds        bounds of the region of data to be inserted in the <var>k</var>-dimensional tree.
     * @param  locator       function computing a position for an arbitrary element of this tree.
     * @param  nodeCapacity  the capacity of each node (not to be confused with a capacity of the tree).
     */
    public PointTree(final Envelope bounds, final Function<? super E, double[]> locator, final int nodeCapacity) {
        ArgumentChecks.ensureNonNull("bounds",  bounds);
        ArgumentChecks.ensureNonNull("locator", locator);
        ArgumentChecks.ensureStrictlyPositive("nodeCapacity", nodeCapacity);
        final int n = bounds.getDimension();
        if (n < 2) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.MismatchedDimension_3, "bounds", 2, n));
        }
        if (n > MAXIMUM_DIMENSIONS) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.ExcessiveNumberOfDimensions_1, n));
        }
        treeRegion = new double[n*2];
        for (int i=0; i<n; i++) {
            ArgumentChecks.ensureFinite("treeRegion", treeRegion[i]   = bounds.getMedian(i));
            ArgumentChecks.ensureFinite("treeRegion", treeRegion[i+n] = bounds.getSpan(i));
        }
        this.nodeCapacity = Math.max(4, nodeCapacity);      // Here, 4 is an arbitrary value.
        this.locator      = locator;
        this.root         = (n == 2) ? new QuadTreeNode() : new PointTreeNode.Default(n);
    }

    /**
     * Returns the number of dimensions of points in this tree.
     *
     * @return the number of dimensions of points in this tree.
     */
    public final int getDimension() {
        return treeRegion.length >>> 1;
    }

    /**
     * Returns the number of elements in this tree.
     *
     * @return the number of elements in this tree, or {@link Integer#MAX_VALUE}
     *         if there is more elements than what an {@code int} can represent.
     */
    @Override
    public int size() {
        // Negative value would be `long` overflow to be returned as MAX_VALUE.
        return (count >>> Integer.SIZE) == 0 ? (int) count : Integer.MAX_VALUE;
    }

    /**
     * Inserts the specified element into this tree if it is not already present.
     *
     * @param  element  the element to insert.
     * @return {@code true} if the element has been added, or {@code false} if it was already present.
     * @throws NullPointerException if the given element is null.
     */
    @Override
    public boolean add(final E element) {
        ArgumentChecks.ensureNonNull("element", element);
        final boolean p = insert(root, treeRegion, element);
        if (p) count++;
        return p;
    }

    /**
     * Inserts the specified data into the given node. This method will iterate through node children
     * until a suitable node is found. This method may invoke itself recursively.
     *
     * @param  parent   the parent where to add the given data.
     * @param  region   region of current node, as the center in first half and size in second half.
     * @param  element  the element to insert.
     * @return {@code true} if the element has been added, or {@code false} if it was already present.
     */
    @SuppressWarnings("unchecked")
    private boolean insert(PointTreeNode parent, double[] region, final E element) {
        boolean isRegionCopied = false;
        final double[] point = locator.apply(element);
        for (;;) {
            final int quadrant = PointTreeNode.quadrant(point, region);
            final Object child = parent.getChild(quadrant);
            /*
             * If the element will be stored in a new quadrant never used before,
             * create a leaf node for that new quadrant. This is the easiest case.
             */
            if (child == null) {
                parent.setChild(quadrant, new Object[] {element});
                return true;
            }
            /*
             * If the quadrant where to store the element is a parent containing other nodes,
             * enter in that quadrant and repeat all above checks with that node as the new parent.
             * We continue down the tree until a leaf node is found.
             */
            if (child instanceof PointTreeNode) {
                if (!isRegionCopied) {
                    isRegionCopied = true;
                    region = region.clone();
                }
                PointTreeNode.enterQuadrant(region, quadrant);
                parent = (PointTreeNode) child;
                continue;
            }
            /*
             * At this point we reached a leaf of the tree. First, verify that the element is not
             * already present. If not, store the element in that leaf if there is enough room.
             */
            final Object[] data = (Object[]) child;
            final int n = data.length;
            for (int i=0; i<n; i++) {
                if (element.equals(data[i])) {
                    return false;
                }
            }
            if (n < nodeCapacity) {
                final Object[] copy = new Object[n+1];
                System.arraycopy(data, 0, copy, 0, n);
                copy[n] = element;
                parent.setChild(quadrant, copy);
                return true;                                // Leaf node can take the data — done.
            }
            /*
             * Leaf can not add the given element because the leaf has reached its maximal capacity.
             * Replace the leaf node by a parent node and add all previous elements into it. After
             * data has been copied, continue attempts to insert the element given to this method.
             */
            if (!isRegionCopied) {
                isRegionCopied = true;
                region = region.clone();
            }
            PointTreeNode.enterQuadrant(region, quadrant);
            final PointTreeNode branch = parent.newInstance();
            for (final Object e : data) {
                insert(branch, region, (E) e);
            }
            parent.setChild(quadrant, branch);
            parent = branch;
        }
    }

    /**
     * Creates an iterator over all elements in this set.
     * In current implementation, the iterator does not support element removal.
     */
    @Override
    public Iterator<E> iterator() {
        return Spliterators.iterator(spliterator());
    }

    /**
     * Creates an iterator over all elements in this set. The iterator characteristics are
     * {@linkplain Spliterator#SIZED sized}, {@linkplain Spliterator#DISTINCT distinct} and
     * {@link Spliterator#NONNULL non-null}.
     */
    @Override
    public Spliterator<E> spliterator() {
        return new NodeIterator<E>(this, null) {
            @Override public    long    estimateSize()     {return count;}
            @Override public    int     characteristics()  {return SIZED | DISTINCT | NONNULL;}
            @Override protected boolean filter(double[] p) {return true;}
        };
    }

    /**
     * Returns all elements in the given bounding box.
     *
     * @param  searchRegion  envelope representing the rectangular search region.
     * @return elements that are within the given radius from the point.
     */
    public Stream<E> queryByBoundingBox(final Envelope searchRegion) {
        ArgumentChecks.ensureNonNull("searchRegion", searchRegion);
        ArgumentChecks.ensureDimensionMatches("searchRegion", getDimension(), searchRegion);
        return StreamSupport.stream(new NodeIterator<>(this, searchRegion), false);
        // TODO: make parallel by default after we tested most extensively.
    }
}
