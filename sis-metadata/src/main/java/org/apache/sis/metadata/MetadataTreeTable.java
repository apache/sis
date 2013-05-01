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
import java.text.Format;
import java.io.Serializable;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTableFormat;
import org.apache.sis.internal.util.UnmodifiableArrayList;


/**
 * A tree table view over a metadata object.
 * The tree table is made of three columns:
 *
 * <ul>
 *   <li>{@link TableColumn#IDENTIFIER} - the property standard identifier.</li>
 *   <li>{@link TableColumn#NAME}       - the property name.</li>
 *   <li>{@link TableColumn#TYPE}       - the element type.</li>
 *   <li>{@link TableColumn#VALUE}      - the property value.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final class MetadataTreeTable implements TreeTable, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 6320615192545089879L;

    /**
     * The columns to be returned by {@link #getColumns()}.
     */
    static final List<TableColumn<?>> COLUMNS = UnmodifiableArrayList.wrap(new TableColumn<?>[] {
        TableColumn.IDENTIFIER,
        TableColumn.NAME,
        TableColumn.TYPE,
        TableColumn.VALUE
    });

    /**
     * The {@link TreeTableFormat} to use for the {@link #toString()} method implementation.
     * Created when first needed.
     */
    private static Format format;

    /**
     * The root of the metadata tree.
     */
    private final Node root;

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
     * @param standard    The metadata standard implemented by the given metadata.
     * @param metadata    The metadata object to wrap.
     * @param valuePolicy The behavior of this map toward null or empty values.
     */
    MetadataTreeTable(final MetadataStandard standard, final Object metadata, final ValueExistencePolicy valuePolicy) {
        this.standard    = standard;
        this.valuePolicy = valuePolicy;
        this.root = new MetadataTreeNode(this, metadata);
    }

    /**
     * Returns the columns included in this tree table.
     */
    @Override
    public List<TableColumn<?>> getColumns() {
        return COLUMNS;
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
     * @return A string representation of this tree table.
     */
    @Override
    public String toString() {
        synchronized (MetadataTreeTable.class) {
            if (format == null) {
                final TreeTableFormat f = new TreeTableFormat(null, null);
                f.setColumns(TableColumn.NAME, TableColumn.VALUE);
                format = f;
            }
            return format.format(this);
        }
    }
}
