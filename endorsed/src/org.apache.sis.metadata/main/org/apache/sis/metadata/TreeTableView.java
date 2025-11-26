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
package org.apache.sis.metadata;

import java.util.List;
import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.function.Predicate;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTableFormat;
import org.apache.sis.util.internal.shared.UnmodifiableArrayList;
import org.apache.sis.util.internal.shared.TreeFormatCustomization;
import org.apache.sis.xml.bind.SpecializedIdentifier;
import org.apache.sis.xml.bind.NonMarshalledAuthority;
import org.apache.sis.system.Semaphores;


/**
 * A tree table view over a metadata object.
 * The tree table is made of the following columns:
 *
 * <ul>
 *   <li>{@link MetadataColumn#IDENTIFIER} - the property identifier as defined by the UML (if any).</li>
 *   <li>{@link MetadataColumn#INDEX}      - the index in the collection, or null if the property is not a collection.</li>
 *   <li>{@link MetadataColumn#NAME}       - the human-readable property name, inferred from the identifier and index.</li>
 *   <li>{@link MetadataColumn#TYPE}       - the base interface of property values.</li>
 *   <li>{@link MetadataColumn#OBLIGATION} - whether the property is mandatory, optional or conditional.</li>
 *   <li>{@link MetadataColumn#VALUE}      - the property value.</li>
 *   <li>{@link MetadataColumn#NIL_REASON} - if the property is mandatory and nevertheless absent, the reason why.</li>
 *   <li>{@link MetadataColumn#REMARKS}    - remarks on the property value.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class TreeTableView implements TreeTable, TreeFormatCustomization, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3911016927808764394L;

    /**
     * The columns to be returned by {@link #getColumns()}.
     * The filtered columns are the columns without the nil reason.
     * The latter column is useless if {@link ValueExistencePolicy} is excluding nil values.
     */
    private static final List<TableColumn<?>> COLUMNS, FILTERED_COLUMNS;
    static {
        var columns = new TableColumn<?>[] {
            MetadataColumn.IDENTIFIER,
            MetadataColumn.INDEX,
            MetadataColumn.NAME,
            MetadataColumn.TYPE,
            MetadataColumn.OBLIGATION,
            MetadataColumn.VALUE,
            MetadataColumn.NIL_REASON,
            MetadataColumn.REMARKS
        };
        COLUMNS = UnmodifiableArrayList.wrap(columns);
        columns = ArraysExt.remove(columns, 6, 1);
        FILTERED_COLUMNS = UnmodifiableArrayList.wrap(columns);
    }

    /**
     * The root of the metadata tree.
     * Consider this field as final - it is modified only on
     * deserialization by {@link #readObject(ObjectInputStream)}.
     */
    private transient TreeNode root;

    /**
     * The metadata standard implemented by the metadata objects.
     */
    final MetadataStandard standard;

    /**
     * The behavior of this tree table toward null or empty values.
     */
    final ValueExistencePolicy valuePolicy;

    /**
     * Creates a tree table for the specified metadata object.
     *
     * @param  standard     the metadata standard implemented by the given metadata.
     * @param  metadata     the metadata object to wrap.
     * @param  baseType     base type of {@code metadata} interfaces to take in account.
     * @param  valuePolicy  the behavior of this map toward null or empty values.
     */
    TreeTableView(final MetadataStandard standard, final Object metadata,
            final Class<?> baseType, final ValueExistencePolicy valuePolicy)
    {
        this.standard    = standard;
        this.valuePolicy = valuePolicy;
        this.root        = new TreeNode(this, metadata, baseType);
    }

    /**
     * Returns the columns included in this tree table.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")     // Because the returned collection is unmodifiable.
    public List<TableColumn<?>> getColumns() {
        return valuePolicy.acceptNilValues() ? COLUMNS : FILTERED_COLUMNS;
    }

    /**
     * Returns the root of this metadata tree.
     */
    @Override
    public Node getRoot() {
        return root;
    }

    /**
     * Returns a string representation of this tree table.
     * The current implementation uses a shared instance of {@link TreeTableFormat}.
     * This is okay for debugging or occasional usages. However for more extensive usages,
     * developers are encouraged to create and configure their own {@link TreeTableFormat}
     * instance.
     *
     * @return a string representation of this tree table.
     */
    @Override
    public String toString() {
        /*
         * The NULL_COLLECTION semaphore prevents creation of new empty collections by getter methods
         * (a consequence of lazy instantiation). The intent is to avoid creation of unnecessary objects
         * for all unused properties. Users should not see behavioral difference, except if they override
         * some getters with an implementation invoking other getters. However in such cases, users would
         * have been exposed to null values at XML marshalling time anyway.
         */
        final boolean allowNull = Semaphores.queryAndSet(Semaphores.NULL_COLLECTION);
        try {
            synchronized (MetadataFormat.INSTANCE) {
                return MetadataFormat.INSTANCE.format(this);
            }
        } finally {
            Semaphores.clearIfFalse(Semaphores.NULL_COLLECTION, allowNull);
        }
    }

    /**
     * Returns the filter to use when formatting an instance of this {@code TreeTable}.
     * This filter will be combined with the filter that the user may specify by a call
     * to {@link TreeTableFormat#setNodeFilter(Predicate)}.
     */
    @Override
    public Predicate<TreeTable.Node> filter() {
        return TreeTableView::filter;
    }

    /**
     * Invoked during the formatting of a tree node for hiding the ISBN and ISSN identifiers of a {@link Citation}.
     * Those identifiers will be formatted in the {@code ISBN} and {@code ISSN} properties instead. We apply this
     * filtering for avoiding redundancies in the tree representation.
     */
    private static boolean filter(final TreeTable.Node node) {
        /*
         * The special case implemented in this method applies only to two attributes in the Citation interface.
         * We test for this condition first because the call to TreeNode.getParent() is cheap and allow to detect
         * soon the metadata instances that do not need further examination.
         */
        final Node parent = node.getParent();
        if (parent instanceof TreeNode && Citation.class.isAssignableFrom(((TreeNode) parent).baseType)) {
            Object value = null;
            if (node instanceof TreeNode) {
                /*
                 * Since this method is invoked (indirectly) during iteration over the children, the value may have
                 * been cached by TreeNodeChildren.Iter.next(). Try to use this value instead of computing it again.
                 */
                value = ((TreeNode) node).cachedValue;
            }
            if (value == null) {
                value = node.getUserObject();
            }
            /*
             * Filter out the ISBN and ISSN identifiers if they are inside a Citation object.
             * We keep them if the user added them to other kinds of objects.
             */
            if (value instanceof SpecializedIdentifier) {
                final Citation authority = ((SpecializedIdentifier) value).getAuthority();
                if (authority instanceof NonMarshalledAuthority && ((NonMarshalledAuthority) authority).isBookOrSerialNumber()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Invoked on serialization. Write the metadata object instead of the {@linkplain #root} node.
     *
     * @param  out  the output stream where to serialize this object.
     * @throws IOException if an I/O error occurred while writing.
     */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(root.baseType);
        out.writeObject(root.metadata);
    }

    /**
     * Invoked on deserialization. Recreate the {@linkplain #root} node from the metadata object.
     *
     * @param  in  the input stream from which to deserialize an object.
     * @throws IOException if an I/O error occurred while reading or if the stream contains invalid data.
     * @throws ClassNotFoundException if the class serialized on the stream is not on the module path.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        final Class<?> baseType = (Class<?>) in.readObject();
        root = new TreeNode(this, in.readObject(), baseType);
    }
}
