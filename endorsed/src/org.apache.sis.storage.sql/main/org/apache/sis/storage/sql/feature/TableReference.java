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
package org.apache.sis.storage.sql.feature;

import java.util.Objects;
import java.util.function.Consumer;
import org.opengis.util.LocalName;
import org.opengis.util.GenericName;
import org.apache.sis.storage.sql.SQLStoreProvider;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Debug;
import org.apache.sis.util.collection.DefaultTreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable;
import static org.apache.sis.util.privy.Strings.trimOrNull;


/**
 * A (catalog, schema, table) name tuple, which can be used as keys in hash map.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public class TableReference {
    /**
     * The catalog, schema and table name of a table.
     * The table name is mandatory, but the schema and catalog names may be null or empty.
     * Note that an empty string and null values have different meanings in JDBC metadata:
     *
     * <ul>
     *   <li>An empty string means that the table name has no catalog (or schema).</li>
     *   <li>A null value means that the catalog (or schema) name should be ignored.</li>
     * </ul>
     *
     * Names are stored here as they were given by JDBC because those names are sometimes
     * compared with other JDBC metadata or used for fetching more table metadata.
     */
    public final String catalog, schema, table;

    /**
     * Ignored by this class; reserved for caller and subclasses usage.
     * May be null. If non-null, then it is guaranteed non-empty and without leading or trailing spaces.
     */
    final String freeText;

    /**
     * Creates a new tuple with the given names.
     */
    TableReference(final String catalog, final String schema, final String table, final String freeText) {
        this.catalog  = catalog;
        this.schema   = schema;
        this.table    = table;
        this.freeText = trimOrNull(freeText);
    }

    /**
     * Creates a new tuple with the components of the given name.
     */
    TableReference(final GenericName name, final String comments) {
        final String[] names = splitName(name);
        catalog  = names[2];
        schema   = names[1];
        table    = names[0];
        freeText = trimOrNull(comments);
    }

    /**
     * Splits the given name in (catalog, schema, table) tuple.
     * Those components are returned in an array of length 3, in reverse order.
     */
    static String[] splitName(final GenericName name) {
        String[] parts = name.getParsedNames().stream().map(LocalName::toString).toArray(String[]::new);
        ArraysExt.reverse(parts);               // Reorganize in (table, schema, catalog) order.
        return ArraysExt.resize(parts, 3);      // Pad with null values if necessary.
    }

    /**
     * Creates a name for the feature type backed by this table.
     * This method does not cache the value;
     * caller is expected to invoke only once and store the name.
     *
     * @param  analyzer  the object which is analyzing the database schema for inferring feature types.
     */
    final GenericName getName(final Analyzer analyzer) {
        return analyzer.nameFactory.createLocalName(
               analyzer.namespace(trimOrNull(catalog), trimOrNull(schema)), table);
    }

    /**
     * Returns {@code true} if the given object is a {@code TableReference} with equal table, schema and catalog names.
     * All other properties that may be defined in subclasses (column names, action on delete, etc.) are ignored. This
     * method is <strong>not</strong> for testing if two {@link Relation} are fully equal. The purpose of this method
     * is only to use {@code TableReference} as keys in a {@code HashSet} for remembering full coordinates of tables
     * that may need to be analyzed later.
     *
     * @return whether the given object is another {@code TableReference} for the same table.
     */
    @Override
    public final boolean equals(final Object obj) {
        if (obj instanceof TableReference) {
            final TableReference other = (TableReference) obj;
            return table.equals(other.table) && Objects.equals(schema, other.schema) && Objects.equals(catalog, other.catalog);
            // Other properties (freeText, columns, cascadeOnDelete) intentionally omitted.
        }
        return false;
    }

    /**
     * Computes a hash code from the catalog, schema and table names.
     * See {@link #equals(Object)} for information about the purpose.
     *
     * @return a hash code value for this table reference.
     */
    @Override
    public final int hashCode() {
        return table.hashCode() + 31*Objects.hashCode(schema) + 37*Objects.hashCode(catalog);
    }

    /**
     * Adds a child of the given name to the given parent node.
     * This is a convenience method for {@code toString()} implementations.
     *
     * @param  parent  the node where to add a child.
     * @param  name    the name to assign to the child.
     * @return the child added to the parent.
     */
    @Debug
    static TreeTable.Node newChild(final TreeTable.Node parent, final String name) {
        final TreeTable.Node child = parent.newChild();
        child.setValue(TableColumn.NAME, name);
        return child;
    }

    /**
     * Formats a graphical representation of an object for debugging purpose. This representation
     * can be printed to the {@linkplain System#out standard output stream} (for example)
     * if the output device uses a monospaced font and supports Unicode.
     */
    static String toString(final Object owner, final Consumer<TreeTable.Node> appender) {
        final var table = new DefaultTreeTable(TableColumn.NAME);
        final TreeTable.Node root = table.getRoot();
        root.setValue(TableColumn.NAME, owner.getClass().getSimpleName());
        appender.accept(root);
        return table.toString();
    }

    /**
     * Formats a string representation of this object for debugging purpose.
     *
     * @return a string representation of this table reference.
     */
    @Override
    public String toString() {
        return SQLStoreProvider.createTableName(trimOrNull(catalog), trimOrNull(schema), table).toString();
    }
}
