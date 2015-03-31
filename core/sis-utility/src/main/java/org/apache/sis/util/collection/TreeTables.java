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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.io.File;
import java.text.ParseException;
import org.opengis.util.InternationalString;
import org.apache.sis.util.Static;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.ArgumentChecks;


/**
 * Static methods working on {@link TreeTable} objects and their nodes.
 * This class provides methods for some tasks considered generic enough,
 * and example codes for more specialized tasks that developers can customize.
 *
 * <p>The remaining of this class javadoc contains example codes placed in public domain.
 * Developers can copy and adapt those examples as they see fit.</p>
 *
 * <div class="section">Example 1: Reduce the depth of a tree</div>
 * For every branch containing exactly one child, the following method concatenates in-place
 * that branch and its child together. This method can be used for simplifying depth trees into
 * something less verbose. For example given the tree on the left side, this method transforms
 * it into the tree on the right side:
 *
 * <table class="sis">
 * <caption>Example of tree depth reduction</caption>
 * <tr><th>Before</th><th class="sep">After</th></tr>
 * <tr><td>
 * {@preformat text
 *   root
 *     ├─users
 *     │   └─alice
 *     │       ├─data
 *     │       │   └─mercator
 *     │       └─document
 *     └─lib
 * }
 * </td><td class="sep">
 * {@preformat text
 *   root
 *     ├─users/alice
 *     │   ├─data/mercator
 *     │   └─document
 *     └─lib
 * }
 * </td></tr></table>
 * There is no pre-defined method for this task because there is too many parameters that
 * developers may want to customize (columns to merge, conditions for accepting the merge,
 * kind of objects to merge, name separator, <i>etc.</i>). In the following code snippet,
 * the content of the {@code NAME} columns are concatenated only if the {@code VALUE} column
 * has no value (for avoiding data lost when the node is discarded) and use the system file
 * separator as name separator:
 *
 * {@preformat java
 *     final TableColumn columnToProtect = TableColumn.VALUE;
 *     final TableColumn columnToConcatenate = TableColumn.NAME;
 *
 *     TreeTable.Node concatenateSingletons(final TreeTable.Node node) {
 *         // This simple example is restricted to nodes which are known to handle
 *         // their children in a list instead than some other kind of collection.
 *         final List<TreeTable.Node> children = (List<TreeTable.Node>) node.getChildren();
 *         final int size = children.size();
 *         for (int i=0; i<size; i++) {
 *             children.set(i, concatenateSingletons(children.get(i)));
 *         }
 *         if (size == 1) {
 *             final TreeTable.Node child = children.get(0);
 *             if (node.getValue(columnToProtect) == null) {
 *                 children.remove(0);
 *                 child.setValue(columnToConcatenate,
 *                         node .getValue(columnToConcatenate) + File.separator +
 *                         child.getValue(columnToConcatenate));
 *                 return child;
 *             }
 *         }
 *         return node;
 *     }
 * }
 *
 * @author  Martin Desruisseaux
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see TreeTable
 */
public final class TreeTables extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private TreeTables() {
    }

    /**
     * Finds the node for the given path, or creates a new node if none exists.
     * First, this method searches in the node {@linkplain TreeTable.Node#getChildren()
     * children collection} for the root element of the given path. If no such node is found,
     * a {@linkplain TreeTable.Node#newChild() new child} is created. Then this method
     * repeats the process (searching in the children of the child for the second path
     * element), until the last path element is reached.
     *
     * <p>For example if the given path is {@code "users/alice/data"}, then this method
     * finds or creates the nodes for the following tree, where {@code "from"} is the
     * node given in argument to this method:</p>
     *
     * {@preformat text
     *   from
     *     └─users
     *         └─alice
     *             └─data
     * }
     *
     * @param  from   The root node from which to start the search.
     * @param  column The column containing the file name.
     * @param  path   The file for which to find or create a node.
     * @return The node for the given file, either as an existing node or a new node.
     */
    public static TreeTable.Node nodeForPath(TreeTable.Node from,
            final TableColumn<? super String> column, final File path)
    {
        final File parent = path.getParentFile();
        if (parent != null) {
            from = nodeForPath(from, column, parent);
        }
        String name = path.getName();
        if (name.isEmpty() && parent == null) {
            name = File.separator; // Root directory.
        }
        for (final TreeTable.Node child : from.getChildren()) {
            if (name.equals(child.getValue(column))) {
                return child;
            }
        }
        from = from.newChild();
        from.setValue(column, name);
        return from;
    }

    /**
     * For every columns having values of type {@link CharSequence} or {@link String},
     * converts the values to localized {@code String}s. During conversions, this method also
     * replaces duplicated {@code String} instances by references to the same singleton instance.
     *
     * <p>This method may be invoked before to serialize the table in order to reduce the
     * serialization stream size.</p>
     *
     * @param  table  The table in which to replace values by their string representations.
     * @param  locale The locale to use when replacing {@link InternationalString} instances. Can be {@code null}.
     * @return Number of replacements done.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static int replaceCharSequences(final TreeTable table, final Locale locale) {
        ArgumentChecks.ensureNonNull("table", table);
        final List<TableColumn<?>> columns = table.getColumns();
        TableColumn<? super String>[] filtered = new TableColumn[columns.size()];
        int count = 0;
        for (final TableColumn<?> column : columns) {
            if (column.getElementType().isAssignableFrom(String.class)) {
                filtered[count++] = (TableColumn<? super String>) column;
            }
        }
        filtered = ArraysExt.resize(filtered, count);
        return replaceCharSequences(table.getRoot(), filtered, locale, new HashMap<String,String>());
    }

    /**
     * Implementation of the public {@link #replaceCharSequences(TreeTable, Locale)} method.
     *
     * @param  node    The node in which to replace values by their string representations.
     * @param  columns The columns where to perform the replacements.
     * @param  locale  The locale to use when replacing {@link InternationalString} instances. Can be {@code null}.
     * @param  pool    An initially empty pool of string representations, to be filled by this method.
     * @return Number of replacements done.
     */
    private static int replaceCharSequences(final TreeTable.Node node, final TableColumn<? super String>[] columns,
            final Locale locale, final Map<String,String> pool)
    {
        int changes = 0;
        for (final TreeTable.Node child : node.getChildren()) {
            changes += replaceCharSequences(child, columns, locale, pool);
        }
        for (final TableColumn<? super String> column : columns) {
            final Object value = node.getValue(column);
            if (value != null) {
                String text;
                if (value instanceof InternationalString) {
                    text = ((InternationalString) value).toString(locale);
                } else {
                    text = value.toString();
                }
                final String old = pool.put(text, text);
                if (old != null) {
                    pool.put(old, old);
                    text = old;
                }
                if (text != value) {
                    node.setValue(column, text);
                    changes++;
                }
            }
        }
        return changes;
    }

    /**
     * Returns a string representation of the given tree table.
     * The current implementation uses a shared instance of {@link TreeTableFormat}.
     * This is okay for debugging or occasional usages. However for more extensive usages,
     * developers are encouraged to create and configure their own {@code TreeTableFormat}
     * instance.
     *
     * @param  table The tree table to format.
     * @return A string representation of the given tree table.
     */
    public static String toString(final TreeTable table) {
        ArgumentChecks.ensureNonNull("table", table);
        synchronized (TreeTableFormat.INSTANCE) {
            return TreeTableFormat.INSTANCE.format(table);
        }
    }

    /**
     * Parses the given string as tree.
     * This helper method is sometime useful for quick tests or debugging purposes.
     * For more extensive use, consider using {@link TreeTableFormat} instead.
     *
     * @param  tree         The string representation of the tree to parse.
     * @param  labelColumn  The columns where to store the node labels. This is often {@link TableColumn#NAME}.
     * @param  otherColumns Optional columns where to store the values, if any.
     * @return A tree parsed from the given string.
     * @throws ParseException If an error occurred while parsing the tree.
     */
    public static TreeTable parse(final String tree, final TableColumn<?> labelColumn,
            final TableColumn<?>... otherColumns) throws ParseException
    {
        ArgumentChecks.ensureNonNull("tree", tree);
        ArgumentChecks.ensureNonNull("labelColumn", labelColumn);
        TableColumn<?>[] columns = null; // Default to singleton(NAME).
        if (otherColumns.length != 0 || labelColumn != TableColumn.NAME) {
            columns = ArraysExt.insert(otherColumns, 0, 1);
            columns[0] = labelColumn;
        }
        final TreeTableFormat format = TreeTableFormat.INSTANCE;
        synchronized (format) {
            try {
                format.setColumns(columns);
                return format.parseObject(tree);
            } finally {
                format.setColumns((TableColumn<?>[]) null);
            }
        }
    }
}
