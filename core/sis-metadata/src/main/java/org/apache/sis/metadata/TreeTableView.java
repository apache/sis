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
import java.util.Locale;
import java.util.TimeZone;
import java.text.Format;
import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.function.Predicate;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTableFormat;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.internal.jaxb.SpecializedIdentifier;
import org.apache.sis.internal.jaxb.NonMarshalledAuthority;
import org.apache.sis.internal.util.TreeFormatCustomization;
import org.apache.sis.internal.system.LocalizedStaticObject;
import org.apache.sis.internal.system.Semaphores;


/**
 * A tree table view over a metadata object.
 * The tree table is made of the following columns:
 *
 * <ul>
 *   <li>{@link TableColumn#IDENTIFIER} - the property identifier as defined by the UML (if any).</li>
 *   <li>{@link TableColumn#INDEX}      - the index in the collection, or null if the property is not a collection.</li>
 *   <li>{@link TableColumn#NAME}       - the human-readable property name, inferred from the identifier and index.</li>
 *   <li>{@link TableColumn#TYPE}       - the base interface of property values.</li>
 *   <li>{@link TableColumn#VALUE}      - the property value.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 */
final class TreeTableView implements TreeTable, TreeFormatCustomization, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3911016927808764394L;

    /**
     * The columns to be returned by {@link #getColumns()}.
     */
    static final List<TableColumn<?>> COLUMNS = UnmodifiableArrayList.wrap(new TableColumn<?>[] {
        TableColumn.IDENTIFIER,
        TableColumn.INDEX,
        TableColumn.NAME,
        TableColumn.TYPE,
        TableColumn.VALUE
    });

    /**
     * The {@link TreeTableFormat} to use for the {@link #toString()} method implementation.
     * Created when first needed. Would need to be reset to {@code null} on locale or timezone
     * changes, but we do not yet have any listener for such information.
     */
    @LocalizedStaticObject
    private static Format format;

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
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<TableColumn<?>> getColumns() {
        return COLUMNS;                                 // Unmodifiable
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
        synchronized (TreeTableView.class) {
            if (format == null) {
                final TreeTableFormat f = new TreeTableFormat(
                        Locale.getDefault(Locale.Category.FORMAT), TimeZone.getDefault());
                f.setColumns(TableColumn.NAME, TableColumn.VALUE);
                format = f;
            }
            /*
             * The NULL_COLLECTION semaphore prevents creation of new empty collections by getter methods
             * (a consequence of lazy instantiation). The intent is to avoid creation of unnecessary objects
             * for all unused properties. Users should not see behavioral difference, except if they override
             * some getters with an implementation invoking other getters. However in such cases, users would
             * have been exposed to null values at XML marshalling time anyway.
             */
            final boolean allowNull = Semaphores.queryAndSet(Semaphores.NULL_COLLECTION);
            try {
                return format.format(this);
            } finally {
                if (!allowNull) {
                    Semaphores.clear(Semaphores.NULL_COLLECTION);
                }
            }
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
                 * been cached by TreeNodeChildren.Iter.next(). Try to use this value instead than computing it again.
                 */
                value = ((TreeNode) node).cachedValue;
            }
            if (value == null) {
                value = node.getUserObject();
            }
            /*
             * Filter out the ISBN and ISSN identifiers if they are inside a Citation object.
             * We keep them if the user added them to other kind of objects.
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
     * @throws ClassNotFoundException if the class serialized on the stream is not on the classpath.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        final Class<?> baseType = (Class<?>) in.readObject();
        root = new TreeNode(this, in.readObject(), baseType);
    }
}
