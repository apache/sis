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
package org.apache.sis.util.collection;

import java.util.Arrays;
import java.util.AbstractList;
import java.util.Objects;
import java.io.Serializable;
import org.apache.sis.util.resources.Errors;


/**
 * A list of children in a {@link TreeTable.Node}. This list accepts only nodes that either have no
 * {@link TreeTable.Node#getParent() parent} at addition time, or already have the parent for which
 * this list manages the children. The {@link #add add} and {@link #remove remove} operations shall
 * update the parent when needed.
 *
 * <p>This list does not support duplicated elements. Attempts to add a node which is already an
 * element of another {@code TreeNodeList} will cause an {@link IllegalArgumentException} to be
 * thrown.</p>
 *
 * <p>Operations receiving a single {@code TreeTable.Node} argument are <i>all or nothing</i>
 * operations: in case of failure, the list will be left in the same state as if no operation were
 * attempted. If a failure occurs during a bulk operations, then the list may be left in a state
 * where some elements where processed and others not.</p>
 *
 * <p>Subclasses need to define the {@link #setParentOf(TreeTable.Node, int)} method
 * because the way to set the parent is specific to the node implementation:</p>
 *
 * <h2>Implementation note</h2>
 * We do not extend {@link java.util.ArrayList} because:
 * <ul>
 *   <li>We want to use identity comparisons rather than {@link Object#equals(Object)}.</li>
 *   <li>We don't want this list to be cloneable, because it would complexify the management
 *       of references to the parent node.</li>
 *   <li>Extending {@link AbstractList} reduce the number of methods to override, since
 *       {@code ArrayList} overrides bulk operations with optimized code which are not suitable
 *       to {@code TreeNodeList} (we need the slower path implemented in {@code AbstractList}).</li>
 * </ul>
 *
 * <h3>Note on serialization</h3>
 * Being serializable may seem contradictory with the non-cloneable requirement.
 * But serializing {@code TreeNodeList} will also serialize the parent, thus
 * creating new copy on deserialization. So the parents should not be mixed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class TreeNodeList extends AbstractList<TreeTable.Node>
        implements CheckedContainer<TreeTable.Node>, Serializable
{
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -8723469207489667631L;

    /**
     * Enumeration constant for {@link #setParentOf(TreeTable.Node, int)}.
     */
    protected static final int NULL=0, THIS=1, DRY_RUN=2;

    /**
     * The parent of all children managed by this list.
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    protected final TreeTable.Node parent;

    /**
     * The children, or {@code null} if none.
     * This array will be created when first needed.
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    private TreeTable.Node[] children;

    /**
     * Number of valid elements in the {@link #children} array.
     */
    private int size;

    /**
     * Creates an initially empty list.
     */
    protected TreeNodeList(final TreeTable.Node parent) {
        this.parent = parent;
    }

    /**
     * Returns {@code true} if the node associated to this list is already the parent of the given
     * node, {@code false} if the given node has no parent, or throws an exception otherwise.
     *
     * @param  node  the node for which to check the parent.
     * @return {@code true} if the given node already has its parent set, or {@code false} otherwise.
     * @throws IllegalArgumentException if the given node is the children of another list.
     */
    private boolean isParentOf(final TreeTable.Node node) throws IllegalArgumentException {
        if (node == parent) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.NodeChildOfItself_1, node));
        }
        final TreeTable.Node p = node.getParent();
        if (p == null)   return false;
        if (p == parent) return true;
        throw new IllegalArgumentException(Errors.format(Errors.Keys.NodeHasAnotherParent_1, node));
    }

    /**
     * Sets or clears the parent of the given node. This method doesn't need to care about the
     * current node parent, since {@code TreeNodeList} will take care of removing the tree node
     * from its previous parent before to invoke this method.
     *
     * <p>The {@code mode} argument specifies the parent value to set, as one of the following
     * values:</p>
     * <ul>
     *   <li>{@link #NULL} - set the node parent to {@code null}.</li>
     *   <li>{@link #THIS} - set the node parent to {@link #parent}.</li>
     *   <li>{@link #DRY_RUN} - only check if this method can set the parent of the given node;
     *       do not change the node parent yet.</li>
     * </ul>
     *
     * @param  node  the node on which to set the parent (never {@code null}).
     * @param  mode  one of the {@link #NULL}, {@link #THIS} or {@link #DRY_RUN} constants.
     * @throws IllegalArgumentException if this method cannot set the parent of the given node.
     */
    protected abstract void setParentOf(TreeTable.Node node, int mode) throws IllegalArgumentException;

    /**
     * Returns the type of elements in this list.
     *
     * @return fixed to {@code TreeTable.Node}.
     */
    @Override
    public final Class<TreeTable.Node> getElementType() {
        return TreeTable.Node.class;
    }

    /**
     * Indicates that this container is modifiable.
     *
     * @return {@link Mutability#MODIFIABLE}.
     */
    @Override
    public final Mutability getMutability() {
        return Mutability.MODIFIABLE;
    }

    /**
     * Returns the number of nodes in this list.
     *
     * @return the number of nodes.
     */
    @Override
    public final int size() {
        return size;
    }

    /**
     * Returns the node at the specified index in this list.
     *
     * @param  index  the index of the node to fetch.
     * @return the node at the given index (never {@code null}).
     */
    @Override
    public TreeTable.Node get(final int index) {
        return children[Objects.checkIndex(index, size)];
    }

    /**
     * Sets the node at the specified index in this list.
     *
     * @param  index  the index of the node to set.
     * @param  node   the node to store at the given index (cannot be {@code null}).
     * @return the node which was previously stored at the given index (never {@code null}).
     * @throws IllegalArgumentException if this list cannot add the given node, for example
     *         if the node is already an element of another {@code TreeNodeList}.
     */
    @Override
    public TreeTable.Node set(final int index, final TreeTable.Node node) throws IllegalArgumentException {
        final TreeTable.Node old = children[Objects.checkIndex(index, size)];
        if (old != Objects.requireNonNull(node)) {
            if (isParentOf(node)) {
                ensureNotPresent(node);
                setParentOf(old, NULL);
            } else {
                setParentOf(node, DRY_RUN);
                setParentOf(old,  NULL);
                setParentOf(node, THIS);
            }
            children[index] = node;
            modCount++;
        }
        return old;
    }

    /**
     * Adds the given node at the given index in this list, shifting all nodes currently at
     * and after the given index.
     *
     * @param  index  the index where to insert the node.
     * @param  node   the node to store at the given index (cannot be {@code null}).
     * @throws IllegalArgumentException if this list cannot add the given node, for example
     *         if the node is already an element of another {@code TreeNodeList}.
     */
    @Override
    public void add(final int index, final TreeTable.Node node) throws IllegalArgumentException {
        Objects.checkIndex(index, size + 1);
        if (isParentOf(Objects.requireNonNull(node))) {
            ensureNotPresent(node);
        } else {
            setParentOf(node, THIS);
        }
        addChild(index, node);
    }

    /**
     * Adds the given node at the given index in this list, without any check for the parent.
     * The {@linkplain TreeTable.Node#getParent() parent} of the given node shall already be
     * set to {@code this} before this method is invoked.
     */
    final void addChild(final int index, final TreeTable.Node node) {
        if (children == null) {
            children = new TreeTable.Node[4];
        } else if (size == children.length) {
            children = Arrays.copyOf(children, size*2);
        }
        System.arraycopy(children, index, children, index+1, size - index);
        children[index] = node;
        size++;
        modCount++;
    }

    /**
     * Ensures the given node is not already present in this list. This checks is performed
     * only if a newly added node declares to have this list {@linkplain #parent}. Such case may
     * occur either because the node is a custom user implementation with pre-set parent, or
     * because the node is already presents in this list.
     *
     * @param  node  the node to check.
     * @throws IllegalArgumentException if the given node is already present in this list.
     */
    private void ensureNotPresent(final TreeTable.Node node) throws IllegalArgumentException {
        for (int i=size; --i>=0;) {
            if (children[i] == node) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.ElementAlreadyPresent_1));
            }
        }
    }

    /**
     * Removes all children in the given range of this list. This method removes the nodes in
     * reverse order (last added nodes are removed first). If this method failed to remove a
     * node, then that node and all nodes at lower index will be left in the list.
     *
     * @throws IllegalArgumentException if this method failed to remove a node in the given range.
     */
    @Override
    protected void removeRange(final int lower, final int upper) throws IllegalArgumentException {
        Objects.checkFromToIndex(lower, upper, size);
        int i = upper;
        try {
            while (i != lower) {
                setParentOf(children[i-1], NULL);
                i--;            // Must be decremented only after 'setParentOf' returned successfully.
            }
        } finally {
            modCount++;
            if (children != null) {
                System.arraycopy(children, upper, children, i, size - upper);
                Arrays.fill(children, upper, size, null);
            }
            size -= (upper - i);
        }
    }

    /**
     * Removes from this list the node at the given index.
     * All nodes after the given index will be shifted by one.
     *
     * @param  index  the index of the node to remove.
     * @return the node which was previously at the given index (never {@code null}).
     */
    @Override
    public final TreeTable.Node remove(final int index) throws IllegalArgumentException {
        final TreeTable.Node old = children[Objects.checkIndex(index, size)];
        setParentOf(old, NULL);
        System.arraycopy(children, index+1, children, index, --size - index);
        children[size] = null;
        modCount++;
        return old;
    }

    /**
     * Removes the first occurrence of the given node from this list, if presents.
     * The default implementation searches the node using the {@link #indexOf(Object)},
     * then removes it (if the node has been found) using the {@link #remove(int)} method.
     *
     * @param  node  the node to remove. {@code null} values are ignored.
     * @return {@code true} if the node has been removed, or {@code false} if this list does not
     *         contain the given node.
     * @throws IllegalArgumentException if the node has been found but this list cannot remove it.
     */
    @Override
    public boolean remove(final Object node) throws IllegalArgumentException {
        final int index = indexOf(node);
        if (index >= 0) {
            remove(index);
            return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if this list contains the given node. This implementation only checks
     * if the {@linkplain TreeTable.Node#getParent() node parent} is the {@link #parent} instance.
     * This implementation does not iterate over the children.
     *
     * @param  node  the node to check (can be {@code null}).
     * @return {@code true} if this list contains the given node.
     */
    @Override
    public final boolean contains(final Object node) {
        return (node instanceof TreeTable.Node) && ((TreeTable.Node) node).getParent() == parent;
    }

    /**
     * Returns the index of the first occurrence of the specified node in this list.
     * This method delegates to {@link #lastIndexOf(Object)} because the list is not
     * expected to contain duplicated values.
     *
     * @param  node  the node to search (can be {@code null}).
     * @return index of the given node, or -1 if not found.
     */
    @Override
    public final int indexOf(final Object node) {
        return lastIndexOf(node);
    }

    /**
     * Returns the index of the last occurrence of the specified node in this list.
     *
     * @param  node  the node to search (can be {@code null}).
     * @return index of the given node, or -1 if not found.
     */
    @Override
    public final int lastIndexOf(final Object node) {
        if (contains(node)) {
            for (int i=size; --i>=0;) {
                if (children[i] == node) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Returns an array containing all the children in this list.
     */
    @Override
    public Object[] toArray() {
        return Arrays.copyOf(children, size);
    }
}
