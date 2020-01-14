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
import java.util.stream.StreamSupport;
import java.util.function.Function;
import org.opengis.geometry.Envelope;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;


/**
 * A <var>k</var>-dimensional tree index for points. For <var>k</var>=2, this is a point {@link QuadTree}.
 * For <var>k</var>=3, this is an point {@code Octree}. Higher dimensions are also accepted.
 *
 * <h2>Thread-safety</h2>
 * This class is not thread-safe when the tree content is modified. But if the tree is kept unmodified
 * after construction, then multiple read operations in concurrent threads are safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @param  <E>  the type of elements stored in this tree.
 *
 * @since 1.1
 * @module
 */
class KDTree<E> {
    /**
     * The root node of this <var>k</var>-dimensional tree.
     */
    final KDTreeNode root;

    /**
     * The maximal capacity of each node in this tree. It should be a relatively small number.
     * If the number of elements in a leaf node exceeds this capacity, then the node will be
     * transformed into a parent node with children nodes.
     */
    private final int capacity;

    /**
     * Number of elements in this <var>k</var>-dimensional tree.
     */
    private int count;

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
     */
    final double[] treeRegion;

    /**
     * The function computing a position for an arbitrary element in this tree.
     */
    final Function<? super E, double[]> evaluator;

    /**
     * Creates an initially empty <var>k</var>-dimensional tree with the given capacity for each node.
     * The number of dimensions of the given envelope determines the number of dimensions of points in
     * this tree. The position computed by {@code evaluator} must have the same number of dimensions.
     *
     * <p>The given {@code capacity} is a threshold value controlling when the content of a node should
     * be splited into smaller children nodes. That capacity should be a relatively small number,
     * for example 10. Determining the most efficient value may require benchmarking.</p>
     *
     * @param  bounds     bounds of the region of data to be inserted in the <var>k</var>-dimensional tree.
     * @param  evaluator  function computing a position for an arbitrary element of this tree.
     * @param  capacity   the capacity of each node.
     */
    KDTree(final Envelope bounds, final Function<? super E, double[]> evaluator, final int capacity) {
        final int n = bounds.getDimension();
        if (n < 2) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.MismatchedDimension_3, "bounds", 2, n));
        }
        if (n > NodeIterator.MAX_DIMENSION) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.ExcessiveNumberOfDimensions_1, n));
        }
        treeRegion = new double[n*2];
        for (int i=0; i<n; i++) {
            treeRegion[i]   = bounds.getMedian(i);
            treeRegion[i+n] = bounds.getSpan(i);
        }
        this.capacity  = Math.max(4, capacity);
        this.evaluator = evaluator;
        this.root      = (n == 2) ? new QuadTreeNode() : new KDTreeNode.Default(n);
    }

    /**
     * Inserts the specified data into this tree.
     *
     * @param  element  the element to insert.
     * @return always {@code true} in current implementation.
     */
    public boolean add(final E element) {
        ArgumentChecks.ensureNonNull("element", element);
        insert(root, treeRegion, element);
        count++;
        return true;
    }

    /**
     * Inserts the specified data into the given node. This method will iterate through node children
     * until a suitable node is found. This method may invoke itself recursively.
     *
     * @param  parent   the parent where to add the given data.
     * @param  region   region of current node, as the center in first half and size in second half.
     * @param  element  the element to insert.
     */
    @SuppressWarnings("unchecked")
    private void insert(KDTreeNode parent, double[] region, final E element) {
        boolean isRegionCopied = false;
        final double[] point = evaluator.apply(element);
        for (;;) {
            final int quadrant = KDTreeNode.quadrant(point, region);
            final Object child = parent.getChild(quadrant);
            /*
             * If the element will be stored in a new quadrant never used before,
             * create a leaf node for that new quadrant. This is the easiest case.
             */
            if (child == null) {
                parent.setChild(quadrant, new Object[] {element});
                return;
            }
            /*
             * If the quadrant where to store the element is a parent containing other nodes,
             * enter in that quadrant and repeat all above checks with that node as the new parent.
             * We continue down the tree until a leaf node is found.
             */
            if (child instanceof KDTreeNode) {
                if (!isRegionCopied) {
                    isRegionCopied = true;
                    region = region.clone();
                }
                KDTreeNode.enterQuadrant(region, quadrant);
                parent = (KDTreeNode) child;
                continue;
            }
            /*
             * At this point we reached a leaf of the tree. Store the element in that leaf
             * if there is enough room.
             */
            final Object[] data = (Object[]) child;
            final int n = data.length;
            if (n < capacity) {
                final Object[] copy = new Object[n+1];
                System.arraycopy(data, 0, copy, 0, n);
                copy[n] = element;
                parent.setChild(quadrant, copy);
                return;                                     // Leaf node can take the data — done.
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
            KDTreeNode.enterQuadrant(region, quadrant);
            final KDTreeNode branch = parent.newInstance();
            for (final Object e : data) {
                insert(branch, region, (E) e);
            }
            parent.setChild(quadrant, branch);
            parent = branch;
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
    public Stream<E> queryByBoundingBox(final Envelope searchRegion) {
        ArgumentChecks.ensureNonNull("searchRegion", searchRegion);
        ArgumentChecks.ensureDimensionMatches("searchRegion", treeRegion.length >>> 1, searchRegion);
        return StreamSupport.stream(new NodeIterator<>(this, searchRegion), false);
        // TODO: make parallel after we tested most extensively.
    }
}
