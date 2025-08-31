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
package org.apache.sis.referencing.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.DefaultTreeTable;

// Specific to the main and geoapi-3.1 branches:
import org.apache.sis.geometry.MismatchedReferenceSystemException;


/**
 * Placeholder for a RTree. This simple implementation is not designed for scalability or performance.
 * This class is there for minimal service, to be replaced in some future Apache SIS version by a more
 * sophisticated tree. Current version of this class is okay for small trees where big nodes are added
 * before small nodes.
 *
 * <p>A {@code RTreeNode} instance is used as the root of the tree. Children nodes are stored as a linked list
 * (the list is implemented by the {@link #sibling} field, which reference the next element in the list).</p>
 *
 * <h2>Possible evolution</h2>
 * A future version could avoid extending {@link GeneralEnvelope}. Instead, we could provide abstract
 * {@code contains(…)} methods and let subclasses define them, with possibly more efficient implementations.
 * We would still need an implementation that delegate to {@link GeneralEnvelope} since that class has the
 * advantage of handling envelopes crossing the anti-meridian.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("CloneableImplementsClone")
public class RTreeNode extends GeneralEnvelope {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -6544217991652682694L;

    /**
     * The parent, or {@code null} if none.
     *
     * @see #getParent()
     */
    private RTreeNode parent;

    /**
     * The first node fully included in this code, or {@code null} if none. If non-null, that node and all
     * its {@linkplain #sibling}s shall be fully included in this {@code RTreeNode}. The child may contain
     * other children, thus forming a tree from this wider node to smaller nodes.
     *
     * @see #getChildren()
     */
    private RTreeNode firstChild;

    /**
     * The next node having the same parent as this node. This is used for creating a linked list of nodes
     * that are the children of the {@linkplain #parent}.
     *
     * <h4>Design note</h4>
     * An {@code RTreeNode children} array instead of {@link #firstChild} + {@link #sibling} would be more intuitive.
     * But the use of linked list avoid one level of indirection and is one less object to create for each node.
     * The gain may be negligible with a few hundreds nodes, but a future version of this class may target much
     * more numerous nodes.
     */
    private RTreeNode sibling;

    /**
     * Creates a new node for the given envelope.
     *
     * @param  area  the region covered by this node.
     */
    public RTreeNode(final Envelope area) {
        super(area);
    }

    /**
     * Returns the parent of this node, or {@code null} if none.
     *
     * @return the parent of this node, or {@code null} if none.
     */
    public final RTreeNode getParent() {
        return parent;
    }

    /**
     * Returns the immediate children of this node, or an empty list if none.
     *
     * @return the immediate children of this node.
     */
    public final List<RTreeNode> getChildren() {
        final List<RTreeNode> children = new ArrayList<>();
        for (RTreeNode child = firstChild; child != null; child = child.sibling) {
            assert child.parent == this : child;
            children.add(child);
        }
        return children;
    }

    /**
     * Executes the given action on the given node, all its children and all its siblings.
     *
     * @param node    the node on which to execute the action, or {@code null} if none.
     * @param action  the action to execute.
     */
    public static void walk(RTreeNode node, final Consumer<? super RTreeNode> action) {
        while (node != null) {
            action.accept(node);
            walk(node.firstChild, action);
            node = node.sibling;
        }
    }

    /**
     * Adds the given node to the tree having this node as the root. This method assumes that this
     * node or its siblings are likely to contain the nodes given to this method. This method will
     * work even if this assumption does not hold, but the tree will be inefficient in such case.
     *
     * <p>A "professional" R-Tree would make a better effort for creating a balanced tree here.
     * Current version of this class uses a simple algorithm, okay for small trees where big nodes
     * are added before small nodes. This is not a good general purpose R-Tree.</p>
     *
     * @param  node  the node to add to this tree. If this node has sibling, they will be added too.
     *
     * @see #finish()
     */
    public final void addNode(RTreeNode node) {
detach: for (RTreeNode next; node != null; node = next) {
            next = node.sibling;
            node.sibling = null;                    // Detach the siblings for adding each node individually.
            RTreeNode tail, receiver = this;
            do {
                tail = receiver;
                if (receiver.tryAddChild(node)) {
                    continue detach;                // Node accepted by a child, process the next node if any.
                }
                receiver = receiver.sibling;
            } while (receiver != null);
            tail.sibling = node;                    // Not a child of this node, but assume common parent.
            node.parent = parent;
        }
    }

    /**
     * Tries to add a child. This method checks if the given candidate is fully included in this {@code RTreeNode}.
     * The given node may have children but shall not have any {@linkplain #sibling}, because this method does not
     * check if the siblings would also be included in this node.
     *
     * @param  candidate  the single node (without sibling) to add as a child if possible.
     * @return whether the given node has been added.
     */
    private boolean tryAddChild(final RTreeNode candidate) {
        assert candidate.sibling == null : candidate;
        if (contains(candidate)) {
            RTreeNode child = firstChild;
            if (child == null) {
                firstChild = candidate;                     // This node had no children before this method call.
            } else {
                RTreeNode lastChild;
                do {
                    lastChild = child;
                    if (child.tryAddChild(candidate)) {
                        return true;                        // Given node added to a child instead of to this node.
                    }
                    child = child.sibling;
                } while (child != null);
                lastChild.sibling = candidate;              // Add last in the linked list of this node children.
            }
            candidate.parent = this;
            return true;
        }
        return false;
    }

    /**
     * Finishes the construction of the tree. This method should be invoked only on the instance
     * on which {@link #addNode(RTreeNode)} has been invoked. It performs the following tasks:
     *
     * <ol>
     *   <li>Verify that all nodes have the same CRS or null CRS. An exception is thrown if incompatible CRS are found.
     *       This method does not verify the number of dimensions; this check should have been done by the caller.</li>
     *   <li>Set the CRS of all nodes to the common value found in previous step.</li>
     *   <li>Ensure that the tree has a single root, by creating a synthetic parent if necessary.</li>
     * </ol>
     *
     * @return the root of the tree, which is {@code this} if this node has no sibling.
     */
    public final RTreeNode finish() {
        final Uniformizer action = new Uniformizer();
        walk(this, action);
        action.set = true;
        walk(this, action);
        RTreeNode next = sibling;
        if (next == null) {
            return this;
        }
        /*
         * If there is more than one node, create a synthetic parent containing them all.
         * We do not need to traverse all nodes for this task; only the immediate children
         * of the new parent. The purpose of the synthetic parent is to have a single root
         * (i.e. no sibling).
         */
        parent = new RTreeNode(this);
        parent.firstChild = this;
        do {
            parent.add(next);           // Compute union of envelopes (not an addition of node).
            next.parent = parent;
            next = next.sibling;
        } while (next != null);
        return parent;
    }

    /**
     * The action for getting a common CRS for all nodes, then for setting the CRS of all nodes.
     * This action will be executed on the whole tree, with recursive traversal of children.
     */
    private static final class Uniformizer implements Consumer<RTreeNode> {
        /** The CRS common to all nodes. */
        private CoordinateReferenceSystem common;

        /** {@code false} for collecting information, or {@code true} for setting all node CRS. */
        boolean set;

        /** Invoked for each node of the tree. */
        @Override public void accept(final RTreeNode node) {
            if (set) {
                node.setCoordinateReferenceSystem(common);
            } else {
                final CoordinateReferenceSystem crs = node.getCoordinateReferenceSystem();
                if (common == null) {
                    common = crs;
                } else if (crs != null && !CRS.equivalent(common, crs)) {
                    throw new MismatchedReferenceSystemException(Errors.format(Errors.Keys.MismatchedCRS));
                }
            }
        }
    }

    /**
     * Returns the node that contains the given position, or {@code null} if none.
     * The given node does not need to be the root of the tree; it can be any node.
     * This method will check that node first. If it does not contain the position,
     * then this method goes up in the parent nodes.
     *
     * <p>If consecutive positions are close to each other, then a good strategy is
     * to give to this method the most recently used node.</p>
     *
     * @param  node  any node in the tree. Should be node most likely to contain the position.
     * @param  pos   the position of the node to locate, or {@code null} if none.
     * @return the smallest node containing the given position, or {@code null} if none.
     */
    public static RTreeNode locate(RTreeNode node, final DirectPosition pos) {
        RTreeNode skip = null;      // For avoiding to invoke `contains(pos)` twice on the same mode.
        if (node != null) do {
            if (node.contains(pos)) {
                /*
                 * Before to return the node we just found, check if a child contains the point.
                 * If we find a child containing the point, then `node` is updated to that child.
                 */
                RTreeNode candidate = node.firstChild;
                while (candidate != null) {
                    if (candidate != skip && candidate.contains(pos)) {
                        node = candidate;
                        candidate = node.firstChild;        // Continue checking children of the child.
                    } else {
                        candidate = candidate.sibling;      // Check other children of the parent node.
                    }
                }
                return node;
            }
            skip = node;
            node = node.parent;
        } while (node != null);
        return null;
    }

    /**
     * Returns a hash code value based on the this node and its children,
     * ignoring the parent and the siblings.
     *
     * @return a hash code value for this node and its children.
     */
    @Override
    public int hashCode() {
        return super.hashCode() + 37 * getChildren().hashCode();
    }

    /**
     * Compares this node with the given object for equality, ignoring the parent and the siblings.
     *
     * @param  obj  the object to compare with this node.
     * @return whether the two objects are of the same class, have equal envelope and equal children list.
     */
    @Override
    public boolean equals(final Object obj) {
        return super.equals(obj) && getChildren().equals(((RTreeNode) obj).getChildren());
    }

    /**
     * Formats this envelope as a "{@code DOMAIN}" element (non-standard).
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        super.formatTo(formatter);
        return "Domain";
    }

    /**
     * Returns a string representation of this node and its children as a tree.
     * This method is for debugging purposes.
     *
     * @return a string representation of this node and all its children.
     */
    @Override
    public String toString() {
        return toTree().toString();
    }

    /**
     * Returns a representation of this node and its children as a tree.
     * This is mostly for debugging purposes.
     *
     * @return a tree representation of this node and all its children.
     */
    public final TreeTable toTree() {
        final DefaultTreeTable tree = new DefaultTreeTable(TableColumn.VALUE);
        toTree(tree.getRoot());
        return tree;
    }

    /**
     * Adds this node and its children to the given node. This method invokes itself recursively.
     */
    private void toTree(final TreeTable.Node addTo) {
        addTo.setValue(TableColumn.VALUE, super.toString());
        for (final RTreeNode child : getChildren()) {
            child.toTree(addTo.newChild());
        }
    }
}
