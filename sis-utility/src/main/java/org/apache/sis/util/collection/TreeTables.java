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
import java.nio.file.Path;
import java.text.ParseException;
import org.opengis.util.InternationalString;
import org.apache.sis.util.Static;
import org.apache.sis.util.ArgumentChecks;

import static org.apache.sis.util.Arrays.resize;
import static org.apache.sis.util.Arrays.insert;


/**
 * Static methods working on {@link TreeTable} objects and their nodes.
 * This class provides methods for some tasks considered generic enough,
 * and example codes for more specialized tasks that developers can customize.
 *
 * <p>The remaining of this class javadoc contains example codes placed in public domain.
 * Developers can copy and adapt those examples as they see fit.</p>
 *
 * {@section Example 1: Reduce the depth of a tree}
 * For every branch containing only one child and no value, the following method merges in-place
 * that branch and the node together. This method can be used for simplifying depth trees into
 * something less verbose. However for any column other than {@code NAME}, this method preserves
 * the values of the child node but lost all value of the parent node. For this reason, we perform
 * the merge only if the parent has no value.
 *
 * <p>For example given the tree on the left side, this method transforms it into the tree on the
 * right side:</p>
 *
 * <table class="compact"><tr><td>
 * {@preformat text
 *   root
 *   ├───users
 *   │   └───alice
 *   │       ├───data
 *   │       │   └───mercator
 *   │       └───document
 *   └───lib
 * }
 * </td><td>
 * {@preformat text
 *   root
 *   ├───users/alice
 *   │   ├───data/mercator
 *   │   └───document
 *   └───lib
 * }
 * </td></tr></table>
 * {@preformat java
 *   import static org.apache.sis.util.collection.TableColumn.NAME;          // The column to merge
 *   import static org.apache.sis.util.collection.TableColumn.VALUE_AS_TEXT; // The column which must be empty
 *
 *   public class MyClass {
 *       private static TreeTable.Node concatenateSingletons(final TreeTable.Node node) {
 *           final List<TreeTable.Node> children = node.getChildren();
 *           final int size = children.size();
 *           for (int i=0; i<size; i++) {
 *               children.set(i, concatenateSingletons(children.get(i)));
 *           }
 *           if (size == 1) {
 *               final TreeTable.Node child = children.get(0);
 *               if (node.getValue(VALUE_AS_TEXT) == null) {
 *                   children.remove(0);
 *                   child.setValue(NAME, node.getValue(NAME) + File.separator + child.getValue(NAME));
 *                   return child;
 *               }
 *           }
 *           return node;
 *       }
 *   }
 * }
 *
 * There is no pre-defined method for this task because there is too many parameters that
 * developers may want to customize (columns to merge, conditions for accepting the merge,
 * kind of objects to merge, name separator).
 *
 * @author  Martin Desruisseaux
 * @since   0.3
 * @version 0.3
 * @module
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
     * children list} for the root element of the given path. If no such node is found,
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
     *   └───users
     *       └───alice
     *           └───data
     * }
     *
     * @param  from   The root node from which to start the search.
     * @param  column The column containing the file name.
     * @param  path   The path for which to find or create a node.
     * @return The node for the given path, either as an existing node or a new node.
     */
    public static TreeTable.Node nodeForPath(TreeTable.Node from,
            final TableColumn<? super String> column, final Path path)
    {
        final Path parent = path.getParent();
        if (parent != null) {
            from = nodeForPath(from, column, parent);
        }
        Path filename = path.getFileName();
        if (filename == null) {
            filename = path.getRoot();
        }
        final String name = filename.toString();
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
     * Finds the node for the given file, or creates a new node if none exists.
     * This method performs the same work than the above variant, but working on
     * {@code File} instances rather than {@code Path}.
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
     * For every columns having values {@linkplain Class#isAssignableFrom(Class) assignable from}
     * {@code String}, converts the values to {@code String}s. During conversions, this method also
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
    public static int valuesAsStrings(final TreeTable table, final Locale locale) {
        ArgumentChecks.ensureNonNull("table", table);
        final List<TableColumn<?>> columns = table.getColumns();
        TableColumn<? super String>[] filtered = new TableColumn[columns.size()];
        int count = 0;
        for (final TableColumn<?> column : columns) {
            if (column.getElementType().isAssignableFrom(String.class)) {
                filtered[count++] = (TableColumn<? super String>) column;
            }
        }
        filtered = resize(filtered, count);
        return valuesAsStrings(table.getRoot(), filtered, locale, new HashMap<String,String>());
    }

    /**
     * Implementation of the public {@link #valuesAsStrings(TreeTable, Locale)} method.
     *
     * @param  node    The node in which to replace values by their string representations.
     * @param  columns The columns where to perform the replacements.
     * @param  locale  The locale to use when replacing {@link InternationalString} instances. Can be {@code null}.
     * @param  pool    An initially empty pool of string representations, to be filled by this method.
     * @return Number of replacements done.
     */
    private static int valuesAsStrings(final TreeTable.Node node, final TableColumn<? super String>[] columns,
            final Locale locale, final Map<String,String> pool)
    {
        int changes = 0;
        for (final TreeTable.Node child : node.getChildren()) {
            changes += valuesAsStrings(child, columns, locale, pool);
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
     * The default implementation uses a shared instance of {@link TreeTableFormat}.
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
     * @param  text   The string representation to parse.
     * @param  nodes  The columns where to store the node labels. This is often {@link TableColumn#NAME}.
     * @param  values Optional columns where to store the values, if any.
     * @return A tree parsed from the given string.
     * @throws ParseException If an error occurred while parsing the tree.
     */
    public static TreeTable parse(final String text, final TableColumn<?> nodes,
            final TableColumn<?>... values) throws ParseException
    {
        ArgumentChecks.ensureNonNull("text",  text);
        ArgumentChecks.ensureNonNull("nodes", nodes);
        TableColumn<?>[] columns = null; // Default to singleton(NAME).
        if (values.length != 0 || nodes != TableColumn.NAME) {
            columns = insert(values, 0, 1);
            columns[0] = nodes;
        }
        final TreeTableFormat format = TreeTableFormat.INSTANCE;
        synchronized (format) {
            try {
                format.setColumns(columns);
                return format.parseObject(text);
            } finally {
                format.setColumns((TableColumn<?>[]) null);
            }
        }
    }
}
