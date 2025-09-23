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
package org.apache.sis.io.wkt;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import java.util.function.Consumer;
import java.io.Serializable;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.referencing.internal.shared.WKTKeywords;


/**
 * A tree of {@link Element}s saved for later use. {@code StoredTree}s are created in following situations:
 *
 * <ul>
 *   <li>{@link WKTFormat#addFragment(String, String)} for defining shortcuts
 *       to be inserted into an arbitrary number of other WKT strings.</li>
 *   <li>{@link WKTDictionary#addDefinitions(Stream)} for preparing WKT definitions to be parsed
 *       only when first needed. While WKT trees are waiting, they may share references to same
 *       {@code Node} instances for reducing memory usage.</li>
 * </ul>
 *
 * This class does not store {@link Element} instances directly because {@code Element}s are not easily shareable.
 * Contrarily to {@code Element} design, {@code StoredTree} needs unmodifiable {@link Element#children} list and
 * needs to store {@link Element#offset} values in separated arrays. Those changes make possible to have many
 * {@code StoredTree} instances sharing the same {@code Node} instances in the common case where some WKT elements
 * are repeated in many trees.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class StoredTree implements Serializable {
    /**
     * Indirectly for {@link WKTFormat} serialization compatibility.
     */
    private static final long serialVersionUID = 8436779786449395346L;

    /**
     * Unmodifiable copy of {@link Element} without contextual information such as {@link Element#offset}.
     * The removal of contextual information increase greatly the possibility to reuse the same {@code Node}
     * instances in many {@link StoredTree}s. For example, the {@code UNIT["degrees", 0.0174532925199433]} node
     * is repeated a lot, so we want to share only one {@code Node} instance for every places in the WKT tree
     * where degrees unit is declared, even if they appear at different offsets in the WKT string.
     *
     * @see StoredTree#root
     */
    private static final class Node implements Serializable {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 1463070931527783896L;

        /**
         * Copy of {@link Element#keyword} reference, or {@code null} if this node is anonymous.
         * Anonymous nodes are used only as wrappers for array of roots in the corner cases
         * documented by {@link StoredTree#root}.
         *
         * @see StoredTree#keyword()
         */
        final String keyword;

        /**
         * Snapshot of {@link Element#children} list. Array content shall not be modified.
         * This array is {@code null} if the keyword was not followed by a pair of brackets
         * (e.g. "north"). A null value is not equivalent to an empty list. For example, the
         * list is null when parsing {@code "FOO"} but is empty when parsing {@code "FOO[]"}.
         */
        @SuppressWarnings("serial")                 // Not statically typed as Serializable.
        private final Object[] children;

        /**
         * Creates an anonymous node for an array of roots. This constructor is only for the corner
         * case documented in <q>Multi roots</q> section of {@link StoredTree#root} javadoc.
         *
         * @see StoredTree#StoredTree(List, Map)
         */
        Node(final Deflater deflater, final List<Element> elements) {
            keyword = null;
            children = new Node[elements.size()];
            for (int i=0; i<children.length; i++) {
                children[i] = deflater.unique(new Node(deflater, elements.get(i)));
            }
        }

        /**
         * Creates an immutable copy of the given element. Keywords and children references
         * are copied in this new {@code Node} but {@link Element#offset}s are copied in a
         * separated array for making possible to share {@code Node} instances.
         *
         * @see StoredTree#StoredTree(Element, Map)
         */
        Node(final Deflater deflater, final Element element) {
            keyword  = (String) deflater.unique(element.keyword);
            children = element.getChildren();
            if (children != null) {
                for (int i=0; i<children.length; i++) {
                    Object child = children[i];
                    if (child instanceof Element) {
                        child = new Node(deflater, (Element) child);
                    }
                    children[i] = deflater.unique(child);
                }
            }
            deflater.addOffset(element);
        }

        /**
         * Copies this node in modifiable {@link Element}s and add them to the given list.
         * This is the converse of the {@link #Node(Deflater, List)} constructor.
         * This method usually adds exactly one element to the given list, except
         * for the "multi-roots" corner case documented in {@link StoredTree#root}.
         *
         * @see StoredTree#toElements(AbstractParser, Collection, int)
         */
        final void toElements(final Inflater inflater, final Collection<? super Element> addTo) {
            if (keyword != null) {
                addTo.add(toElement(inflater));         // Standard case.
            } else {
                for (final Node child : (Node[]) children) {
                    addTo.add(child.toElement(inflater));
                }
            }
        }

        /**
         * Copies this node in a modifiable {@link Element}.
         * This is the converse of the {@link #Node(Deflater, Element)} constructor.
         */
        private Element toElement(final Inflater inflater) {
            final LinkedList<Object> list;
            if (children == null) {
                list = null;
            } else {
                list = new LinkedList<>();
                for (Object child : children) {
                    if (child instanceof Node) {
                        child = ((Node) child).toElement(inflater);
                    }
                    list.add(child);
                }
            }
            // Offsets must be read in the same order as they have been written.
            return new Element(keyword, list, inflater.nextOffset(), inflater.errorLocale);
        }

        /**
         * Returns the last element of the given names.
         * This method searches only in children of this node.
         * It does not search recursively in children of children.
         *
         * @param  keys  the element names (e.g. {@code "ID"}).
         * @return the last {@link Node} of the given names found in the children, or {@code null} if none.
         */
        final Node peekLastElement(final String... keys) {
            if (children != null) {
                for (int i = children.length; --i >= 0;) {
                    final Object object = children[i];
                    if (object instanceof Node) {
                        final Node node = (Node) object;
                        if (node.children != null) {
                            for (final String key : keys) {
                                // Keyword is never null for children.
                                if (node.keyword.equalsIgnoreCase(key)) {
                                    return node;
                                }
                            }
                        }
                    }
                }
            }
            return null;
        }

        /**
         * Returns the next values (not child elements).
         * The maximum number of values fetched is the length of the given array.
         * If there is less WKT elements, remaining array elements are unchanged.
         *
         * @param  addTo  non-empty array where to store the values.
         * @param  index  index where to store the first element in given array.
         */
        final void peekValues(final Object[] addTo, int index) {
            for (final Object object : children) {
                if (!(object instanceof Node)) {
                    addTo[index] = object;
                    if (++index >= addTo.length) break;
                }
            }
        }

        /**
         * Adds keyword and children to the given supplier.
         * Children of children are added recursively.
         * This method is for testing purposes only.
         *
         * @see StoredTree#forEachValue(Consumer)
         */
        final void forEachValue(final Consumer<Object> addTo) {
            if (keyword != null) {
                addTo.accept(keyword);
            }
            if (children != null) {
                for (final Object child : children) {
                    addTo.accept(child);
                    if (child instanceof Node) {
                        ((Node) child).forEachValue(addTo);
                    }
                }
            }
        }

        /**
         * Returns the string representation of the first value, which is usually the element name.
         * For example, in {@code DATUM["WGS 84", …]} this is "WGS 84". If there are no children then
         * this method returns the keyword, which is usually an enumeration value (for example "NORTH"}).
         *
         * @see StoredTree#toString()
         */
        @Override
        public String toString() {
            return (children != null && children.length != 0) ? String.valueOf(children[0]) : keyword;
        }

        /**
         * Returns a hash code value for this node. It uses hash codes of child elements, except
         * for children that are instances of {@link Node} for which identity hash codes are used.
         * We avoid requesting "normal" hash code of child {@link Node} because the tree structure
         * may cause the same hash codes to be computed many times. The use of identity hash codes
         * is sufficient if children have been replaced by unique instances before to compute the
         * hash code of this {@link Node}. This replacement is done by {@link Deflater#unique(Node)}.
         *
         * @see Deflater#unique(Node)
         */
        @Override
        public int hashCode() {
            // We never use hashCode()/equals(Object) with anonymous node (null keyword).
            int hash = keyword.hashCode();
            if (children != null) {
                for (final Object value : children) {
                    hash = 31*hash + ((value instanceof Node) ? System.identityHashCode(value) : value.hashCode());
                }
            }
            return hash;
        }

        /**
         * Returns whether the given object is equal to this {@code Node}, comparing keyword and children.
         * Nested {@link Node}s are compared by identity comparisons; see {@link #hashCode()} for rational.
         *
         * @see #hashCode()
         * @see Deflater#unique(Node)
         */
        @Override
        public boolean equals(final Object other) {
            if (other instanceof Node) {
                final Node that = (Node) other;
                // We never use hashCode()/equals(Object) with anonymous node (null keyword).
                if (keyword.equals(that.keyword)) {
                    if (children == that.children) {
                        return true;
                    }
                    if (children != null && that.children != null && children.length == that.children.length) {
                        for (int i=0; i<children.length; i++) {
                            final Object value = children[i];
                            final Object otherValue = that.children[i];
                            if (!(value instanceof Node ? (value == otherValue) : value.equals(otherValue))) {
                                // Identity comparison of `Node` instances for consistency with `hashCode()`.
                                return false;
                            }
                        }
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * Root of a tree of {@link Element} snapshots.
     *
     * <h4>Multi-roots</h4>
     * There is exactly one root in the vast majority of cases. However, there is a situation
     * where we need to allow more roots: when user wants to represent a coordinate system.
     * A WKT 2 coordinate system looks like:
     *
     * {@snippet lang="wkt" :
     *   CS[Cartesian, 2],
     *     Axis["Easting (E)", east],
     *     Axis["Northing (N)", north],
     *     Unit["metre", 1]
     *   }
     *
     * While axes are conceptually parts of coordinate system, they are not declared inside the {@code CS[…]}
     * element for historical reasons (for compatibility with WKT 1). For representing such "flattened tree",
     * we need an array of roots. We do that by wrapping that array in a synthetic {@link Node} with null
     * {@link Node#keyword} (an "anonymous node").
     */
    private final Node root;

    /**
     * Indices in the WKT string where elements have been found. If negative, the actual offset
     * value is {@code ~offset} and {@link Element#isFragment} shall be set to {@code true}.
     * This array shall not be modified because it may be shared by many {@link StoredTree}s.
     *
     * @see Deflater#addOffset(Element)
     * @see Inflater#nextOffset()
     */
    private final short[] offsets;

    /**
     * Creates a new {@code StoredTree} with a snapshot of given tree of elements.
     *
     * @param  tree          root of the tree of WKT elements.
     * @param  sharedValues  pool to use for sharing unique instances of values.
     */
    StoredTree(final Element tree, final Map<Object,Object> sharedValues) {
        final Deflater deflater = new Deflater(sharedValues);
        root = (Node) deflater.unique(new Node(deflater, tree));
        offsets = deflater.offsets();
    }

    /**
     * Creates a new {@code StoredTree} with a snapshot of given trees of elements.
     * This is for a corner case only; see <q>Multi roots</q> in {@link #root}.
     *
     * @param  trees         roots of the trees of WKT elements.
     * @param  sharedValues  pool to use for sharing unique instances of values.
     */
    StoredTree(final List<Element> trees, final Map<Object,Object> sharedValues) {
        final Deflater deflater = new Deflater(sharedValues);
        root = new Node(deflater, trees);       // Do not invoke `unique(…)` on anymous node.
        offsets = deflater.offsets();
    }

    /**
     * Recreates {@link Element} trees. This method is the converse of the constructor.
     * This method usually adds exactly one element to the given list, except
     * for the "multi-roots" corner case documented in {@link #root}.
     *
     * @param  parser      the parser which will be used for parsing the tree.
     * @param  addTo       where to add the elements.
     * @param  isFragment  non-zero if and only if {@link Element#isFragment} shall be {@code true}.
     *                     In such case, this value must be <code>~{@linkplain Element#offset}</code>.
     */
    final void toElements(final AbstractParser parser, final Collection<? super Element> addTo, final int isFragment) {
        root.toElements(new Inflater(parser, offsets, isFragment), addTo);
    }

    /**
     * A helper class for compressing a tree of {@link Element}s as a tree of {@link Node}s.
     * Contrarily to {@code Element} instances, {@code Node}s instances can be shared between many trees.
     * Each instances shall be used for constructing only one {@link Node}. After node construction, this
     * instance lives longer in the {@link #sharedValues} map for sharing {@link #offsets} arrays.
     *
     * @see StoredTree#StoredTree(List, Map)
     */
    private static final class Deflater {
        /**
         * Pool to use for sharing unique instances of values.
         * This is a copy of {@link WKTFormat#sharedValues} map.
         * This is reset to {@code null} when not needed anymore.
         */
        private Map<Object,Object> sharedValues;

        /**
         * The {@link Element#offset} value of {@link StoredTree#root} together with offsets of all
         * {@link Element#children} in iteration order. Order is defined by {@link Node} constructor.
         * This array is expanded as needed. Shall not be modified after call to {@link #offsets()}.
         */
        private short[] offsets;

        /**
         * Number of valid elements in {@link #offsets}.
         */
        private int count;

        /**
         * Pool of previously constructed values used for replacing equal instances by unique instances.
         * May contain {@link String}, {@link Long}, {@link Double} and {@link Node} instances among others.
         *
         * @param  sharedValues  pool of previously created objects.
         */
        Deflater(final Map<Object,Object> sharedValues) {
            this.sharedValues = sharedValues;
            offsets = new short[24];
        }

        /**
         * Returns a unique instance of given object. The given value can be a {@link Node} instance
         * provided that it is not an anonymous node (i.e. {@link Node#keyword} shall be non-null).
         *
         * @param  value  the value for which to get a unique instance.
         * @return a previous instance from the pool, or {@code value} if none.
         *
         * @see Node#hashCode()
         * @see Node#equals(Object)
         */
        final Object unique(final Object value) {
            final Object existing = sharedValues.putIfAbsent(value, value);
            return (existing != null) ? existing : value;
        }

        /**
         * Adds the given {@link Element#offset} value.
         *
         * @see Inflater#nextOffset()
         */
        final void addOffset(final Element element) {
            if (count >= offsets.length) {
                offsets = Arrays.copyOf(offsets, count * 2);
            }
            int offset = Math.min(Short.MAX_VALUE, Math.max(0, element.offset));
            if (element.isFragment) offset = ~offset;
            offsets[count++] = (short) offset;
        }

        /**
         * Returns all {@link Element#offset} values in iteration order.
         * This method may return an array shared by different {@link Node} instances; do not modify.
         */
        @SuppressWarnings("ReturnOfCollectionOrArrayField")
        final short[] offsets() {
            offsets = ArraysExt.resize(offsets, count);
            final short[] other = (short[]) sharedValues.putIfAbsent(this, offsets);
            sharedValues = null;
            if (other != null) {
                offsets = other;
            }
            return offsets;
        }

        /**
         * Compares the {@link #offsets} arrays for equality. This is used by {@link #offsets()}
         * (indirectly, through {@link Map}) as a workaround for Java arrays not overriding
         * {@code equals(Object)} method.
         */
        @Override
        public boolean equals(final Object other) {
            assert offsets.length == count;
            return (other instanceof Deflater) && Arrays.equals(offsets, ((Deflater) other).offsets);
        }

        /**
         * Computes a hash code value based only on the {@link #offsets} array.
         * See {@link #equals(Object)} for rational.
         */
        @Override
        public int hashCode() {
            assert offsets.length == count;
            return Arrays.hashCode(offsets);
        }
    }

    /**
     * A helper class for decompressing a tree of {@link Element}s from a tree of {@link Node}s.
     * This is the converse of {@link Deflater}.
     *
     * @see StoredTree#toElements(AbstractParser, Collection, int)
     */
    private static final class Inflater {
        /**
         * If {@link Element#offset} must be fixed to a value, the bitwise NOT value of that offset.
         * Otherwise 0. This field packs two information:
         *
         * <ul>
         *   <li>{@link Element#isFragment} = ({@code isFragment} != 0)</li>
         *   <li>If {@code isFragment} is {@code true}, then:
         *     <ul><li>{@link Element#offset} = {@code ~isFragment}</li></ul>
         *   </li>
         * </ul>
         */
        private final int isFragment;

        /**
         * The {@link StoredTree#offsets} array. Shall not be modified because potentially shared.
         * Ignored if {@link #isFragment} != 0.
         */
        private final short[] offsets;

        /**
         * Index of the next offset to return in the {@link #offsets} array.
         * Ignored if {@link #isFragment} != 0.
         */
        private int index;

        /**
         * Locale to use for producing error message.
         */
        final Locale errorLocale;

        /**
         * Creates a new inflater.
         *
         * @param  parser      the parser which will be used for parsing the tree.
         * @param  offsets     the {@link StoredTree#offsets} array. Will not be modified.
         * @param  isFragment  non-zero if and only if {@link Element#isFragment} is {@code true}.
         *                     In such case, this value must be <code>~{@linkplain Element#offset}</code>.
         */
        Inflater(final AbstractParser parser, final short[] offsets, final int isFragment) {
            this.errorLocale = parser.errorLocale;
            this.isFragment  = isFragment;
            this.offsets     = offsets;
        }

        /**
         * Returns the value to assign to {@link Element#offset} for the next element.
         */
        final int nextOffset() {
            return (isFragment != 0) ? isFragment : offsets[index++];
        }
    }

    /**
     * Stores identifier information in the given array. This method locates the last {@code "ID"} (WKT 2)
     * or {@code "AUTHORITY"} (WKT 1) node and optionally the {@code "CITATION"} sub-node. Values are copied
     * in the given array, in that order!
     *
     * <ol>
     *   <li>Code space</li>
     *   <li>Code</li>
     *   <li>Version if present</li>
     *   <li>Authority if present (skipped if the array length is less than 4)</li>
     * </ol>
     *
     * If any of above values is missing, the corresponding array element is left unchanged.
     * Callers should set all array elements to {@code null} before to invoke this method.
     *
     * @param  fullId  where to store code space, code, version, authority.
     */
    final void peekIdentifiers(final Object[] fullId) {
        Node id = root.peekLastElement(MathTransformParser.ID_KEYWORDS);
        if (id != null) {
            id.peekValues(fullId, 0);
            if (fullId.length >= 4) {
                id = id.peekLastElement(WKTKeywords.Citation);
                if (id != null) {
                    id.peekValues(fullId, 3);
                }
            }
        }
    }

    /**
     * Adds keywords and children to the given supplier. This is for testing purposes only.
     *
     * @see WKTDictionary#forEachValue(Consumer)
     */
    final void forEachValue(final Consumer<Object> addTo) {
        root.forEachValue(addTo);
    }

    /**
     * Returns the keyword of the root element.
     */
    final String keyword() {
        return root.keyword;
    }

    /**
     * Returns the string representation of the first value of the root element, which is usually the element name.
     * For example, in {@code DATUM["WGS 84", …]} this is "WGS 84". If there are no children then this method returns
     * the keyword, which is usually an enumeration value (for example "NORTH"}).
     */
    @Override
    public String toString() {
        return root.toString();
    }
}
