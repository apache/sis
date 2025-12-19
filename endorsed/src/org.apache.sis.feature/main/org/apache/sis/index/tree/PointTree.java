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

import java.util.Optional;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.CheckedContainer;


/**
 * A <var>k</var>-dimensional tree index for points.
 * For <var>k</var>=2, this is a <dfn>point QuadTree</dfn>.
 * For <var>k</var>=3, this is a point <dfn>Octree</dfn>.
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
 * More details are given in the {@linkplain #PointTree(Class, Envelope, Locator, int, boolean) constructor}.
 *
 * <h2>Thread-safety</h2>
 * This class is not thread-safe when the tree content is modified. But if the tree is kept unmodified
 * after construction, then multiple read operations in concurrent threads are safe. Users can synchronize
 * with {@link java.util.concurrent.locks.ReadWriteLock} (the read lock must be held for complete duration
 * of iterations or stream consumptions).
 *
 * <h2>Serialization</h2>
 * This tree is serializable if the {@code locator} function and all elements in the tree are also serializable.
 * However, the serialization details is implementation specific and may change in any future Apache SIS version.
 *
 * <h2>Limitations</h2>
 * Current implementation does not yet support removal of elements.
 *
 * <h2>References</h2>
 * Insertion algorithm is based on design of QuadTree index in H. Samet,
 * <u>The Design and Analysis of Spatial Data Structures</u>.
 * Massachusetts: Addison Wesley Publishing Company, 1989.
 *
 * @author  Chris Mattmann
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.6
 *
 * @param  <E>  the type of elements stored in this tree.
 *
 * @since 1.1
 */
public class PointTree<E> extends AbstractSet<E> implements CheckedContainer<E> {
    /*
     * We do not declare this class as Serializable because we would need to serialize
     * the lambda function `Locator`. While technically possible, this is not recommended.
     */

    /**
     * Provides the coordinates of any element stored in {@link PointTree}.
     *
     * @param  <E>  the type of elements stored in this tree.
     */
    @FunctionalInterface
    public interface Locator<E> {
        /**
         * Provides the coordinates of the given element. The coordinate shall be written in the supplied array.
         * The array length will be {@link PointTree#getDimension()} and this method shall overwrite all array values.
         *
         * @param  element  the element for which to get the coordinates.
         * @param  dest     a pre-allocated array where to store the coordinate values.
         */
        void getPositionOf(E element, double[] dest);
    }

    /**
     * The maximum number of dimensions (inclusive) that this class currently supports.
     * Current maximum is {@value}. This restriction come from 2⁶ = {@value Long#SIZE}.
     */
    public static final int MAXIMUM_DIMENSIONS = Numerics.LONG_SHIFT;

    /**
     * The type of elements in this set.
     *
     * @see #getElementType()
     */
    private final Class<E> elementType;

    /**
     * The coordinate reference system, or {@code null} if none.
     *
     * @see #getCoordinateReferenceSystem()
     */
    private final CoordinateReferenceSystem crs;

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
     *
     * @see #size()
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
     * This array content should not be modified, unless the entire tree is rebuilt (keep in
     * mind that this array may be shared by {@link PointTree} copies).
     */
    final double[] treeRegion;

    /**
     * Function computing the position of any element in this tree.
     * The length of arrays computed by this locator must be equal to {@link #getDimension()}.
     */
    final Locator<? super E> locator;

    /**
     * Whether the stream can be parallel by default.
     * Should be {@code false} if the {@link #locator} is not thread-safe.
     */
    private final boolean parallel;

    /**
     * Creates a new tree initialized to a copy of the given tree.
     * This copy constructor shares some data structure from the {@code other} tree for reducing memory usage,
     * but the two trees are nevertheless independent (changes in a tree does not affect the other tree).
     *
     * @param  other  the other tree to copy.
     */
    public PointTree(final PointTree<E> other) {
        root         = (PointTreeNode) other.root.clone();
        elementType  = other.elementType;
        crs          = other.crs;
        nodeCapacity = other.nodeCapacity;
        count        = other.count;
        treeRegion   = other.treeRegion;
        locator      = other.locator;
        parallel     = other.parallel;
    }

    /**
     * Creates an initially empty <var>k</var>-dimensional tree with the given capacity for each node.
     * The number of dimensions of the given envelope determines the number of dimensions of points in this tree.
     * The positions computed by {@code locator} must have the same number of dimensions as the given envelope.
     *
     * <p>The {@code bounds} argument specifies the expected region of points to be added in this {@code PointTree}.
     * Those bounds do not need to be exact; {@code PointTree} will work even if some points are located outside
     * those bounds. However, performances will be better if the {@linkplain Envelope#getMedian(int) envelope center}
     * is close to the median of the points to be inserted in the {@code PointTree}, and if the majority of points
     * are inside those bounds.</p>
     *
     * <p>The given {@code nodeCapacity} is a threshold value controlling when the content of a node should
     * be splited into smaller children nodes. That capacity should be a relatively small number,
     * for example 10. Determining the most efficient value may require benchmarking.</p>
     *
     * @param  elementType   the base type of all elements in this tree.
     * @param  bounds        bounds of the region of data to be inserted in the <var>k</var>-dimensional tree.
     * @param  locator       function computing the position of any element in this tree.
     * @param  nodeCapacity  the capacity of each node (not to be confused with a capacity of the tree).
     * @param  parallel      whether the stream can be parallel by default.
     *                       Should be {@code false} if the given {@code locator} is not thread-safe.
     */
    public PointTree(final Class<E> elementType, final Envelope bounds,
            final Locator<? super E> locator, final int nodeCapacity, final boolean parallel)
    {
        ArgumentChecks.ensureNonNull         ("elementType",  elementType);
        ArgumentChecks.ensureNonNull         ("bounds",       bounds);
        ArgumentChecks.ensureNonNull         ("locator",      locator);
        ArgumentChecks.ensureStrictlyPositive("nodeCapacity", nodeCapacity);
        final int n = bounds.getDimension();
        if (n > MAXIMUM_DIMENSIONS) {
            throw new ArithmeticException(Errors.format(Errors.Keys.ExcessiveNumberOfDimensions_1, n));
        }
        treeRegion = new double[n*2];
        boolean isValid = (n >= 2);
        for (int i=0; i<n; i++) {
            final double m = treeRegion[i]   = bounds.getMedian(i);
            final double s = treeRegion[i+n] = bounds.getSpan(i);
            isValid &= !Double.isNaN(m) && (s > 0);
            if (Double.isInfinite(m) || Double.isInfinite(s)) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.InfiniteArgumentValue_1, "treeRegion"));
            }
        }
        if (!isValid) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyEnvelope2D));
        }
        this.crs          = bounds.getCoordinateReferenceSystem();
        this.elementType  = elementType;
        this.nodeCapacity = Math.max(4, nodeCapacity);      // Here, 4 is an arbitrary value.
        this.locator      = locator;
        this.root         = (n == 2) ? new QuadTreeNode() : new PointTreeNode.Default(n);
        this.parallel     = parallel;
    }

    /**
     * Returns the coordinate reference system (CRS) of all points in this tree.
     * The CRS is taken from the envelope given in argument to the constructor.
     *
     * @return the CRS of all points in this tree, if presents.
     *
     * @see #getDimension()
     */
    public final Optional<CoordinateReferenceSystem> getCoordinateReferenceSystem() {
        return Optional.ofNullable(crs);
    }

    /**
     * Returns the number of dimensions of points in this tree.
     *
     * @return the number of dimensions of points in this tree.
     *
     * @see #getCoordinateReferenceSystem()
     */
    public final int getDimension() {
        return treeRegion.length >>> 1;
    }

    /**
     * Returns the base type of all elements in this tree.
     *
     * @return the element type.
     */
    @Override
    public final Class<E> getElementType() {
        return elementType;
    }

    /**
     * Indicates that this collection is modifiable (at least by default).
     *
     * @return {@link Mutability#MODIFIABLE} by default.
     * @since 1.6
     */
    @Override
    public Mutability getMutability() {
        return Mutability.MODIFIABLE;
    }

    /**
     * Removes all elements from this tree.
     */
    @Override
    public void clear() {
        root.clear();
        count = 0;
    }

    /**
     * Returns true if this set contains no elements.
     *
     * @return whether this set is empty.
     */
    @Override
    public boolean isEmpty() {
        return count == 0;
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
        final boolean modified = insert(root, treeRegion, Objects.requireNonNull(element), new double[getDimension()]);
        if (modified) count++;
        return modified;
    }

    /**
     * Inserts all elements from the specified collection into this tree if they are not already present.
     *
     * @param  elements  the elements to insert.
     * @return {@code true} if at least one element has been added.
     * @throws NullPointerException if an element is null.
     */
    @Override
    public boolean addAll(final Collection<? extends E> elements) {
        final double[] buffer = new double[getDimension()];
        boolean modified = false;
        int i = 0;
        for (final E element : elements) {
            ArgumentChecks.ensureNonNullElement("element", i++, element);
            if (insert(root, treeRegion, element, buffer)) {
                modified = true;
                count++;
            }
            /*
             * Note: if practice suggests that we need more performance in this method, we could temporarily
             * use java.util.ArrayList instead of the copy-on-write strategy applied by the insert(…) method
             * below for elements stored in Object[] arrays, then invoke List.toArray() after we finished to
             * insert all elements. Given the increase in code complexity, we wait to see if there is a need
             * for that.
             */
        }
        return modified;
    }

    /**
     * Inserts the specified data into the given node. This method will iterate through node children
     * until a suitable node is found. This method may invoke itself recursively.
     *
     * @param  parent   the parent where to add the given data.
     * @param  region   region of current node, as the center in first half and size in second half.
     * @param  element  the element to insert.
     * @param  point    a pre-allocated array where to store the coordinates. This method will write in this array.
     * @return {@code true} if the element has been added, or {@code false} if it was already present.
     */
    @SuppressWarnings("unchecked")
    private boolean insert(PointTreeNode parent, double[] region, final E element, final double[] point) {
        boolean isRegionCopied = false;
        locator.getPositionOf(element, point);
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
             * Leaf cannot add the given element because the leaf has reached its maximal capacity.
             * Replace the leaf node by a parent node and add all previous elements into it. After
             * data has been copied, continue attempts to insert the element given to this method.
             */
            if (!isRegionCopied) {
                isRegionCopied = true;
                region = region.clone();
            }
            PointTreeNode.enterQuadrant(region, quadrant);
            final PointTreeNode branch = parent.newInstance();
            final double[] buffer = new double[point.length];
            for (final Object e : data) {
                insert(branch, region, (E) e, buffer);
            }
            parent.setChild(quadrant, branch);
            parent = branch;
        }
    }

    /**
     * Returns {@code true} if this set contains the specified element.
     *
     * @param  element  the object to search.
     * @return whether this set contains the specified element.
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(final Object element) {
        if (!elementType.isInstance(element)) {
            return false;
        }
        PointTreeNode  parent = root;
        final double[] region = treeRegion.clone();
        final double[] point  = new double[getDimension()];
        locator.getPositionOf((E) element, point);
        for (;;) {
            final int quadrant = PointTreeNode.quadrant(point, region);
            final Object child = parent.getChild(quadrant);
            if (child != null) {
                if (child instanceof PointTreeNode) {
                    PointTreeNode.enterQuadrant(region, quadrant);
                    parent = (PointTreeNode) child;
                    continue;
                }
                for (Object data : (Object[]) child) {
                    if (element.equals(data)) {
                        return true;
                    }
                }
            }
            return false;
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
            @Override public    long    estimateSize()    {return count;}
            @Override public    int     characteristics() {return SIZED | DISTINCT | NONNULL;}
            @Override protected boolean filter(E e)       {return true;}
        };
    }

    /**
     * Returns a possibly parallel stream with this tree as its source.
     * It is allowable for this method to return a sequential stream.
     *
     * @return a possibly parallel stream over the elements in this tree.
     */
    @Override
    public Stream<E> parallelStream() {
        return StreamSupport.stream(spliterator(), parallel);
    }

    /**
     * Returns all elements in the given bounding box. The given envelope shall be in the same CRS
     * than the points in this tree (this is currently not verified). The returned stream may be
     * parallel by default, depending on the argument given to the constructor.
     * If the action to be applied on the stream cannot be parallel,
     * then user should invoke {@link Stream#sequential()} explicitly.
     *
     * @param  searchRegion  envelope representing the rectangular search region.
     * @return elements that are in the given search region (bounds inclusive).
     */
    public Stream<E> queryByBoundingBox(final Envelope searchRegion) {
        ArgumentChecks.ensureNonNull("searchRegion", searchRegion);
        ArgumentChecks.ensureDimensionMatches("searchRegion", getDimension(), searchRegion);
        return StreamSupport.stream(new NodeIterator<>(this, searchRegion), parallel);
    }

    /*
     * Returns all elements at the specified distance from a point.
     *
     * @param  center    center of the region where to search for elements.
     * @param  distance  distance from the center of elements to retain.
     * @return elements that are within the given radius from the point.
     *
     * @todo use GeodeticCalculator if the CRS is geographic. Otherwise if the CS is Cartesian,
     *       compute Euclidian distance. Otherwise raise an exception.
     *
    public Stream<E> queryByPointRadius(final DirectPosition center, final double distance) {
        ArgumentChecks.ensureNonNull("center", center);
        ArgumentChecks.ensureDimensionMatches("center", getDimension(), center);
        return StreamSupport.stream(new NodeIterator.ByDistance<>(this, center, distance), false);
    }
    */
}
