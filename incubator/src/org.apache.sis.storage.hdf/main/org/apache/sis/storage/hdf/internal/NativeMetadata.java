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
package org.apache.sis.storage.hdf.internal;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.sis.util.collection.DefaultTreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.storage.hdf.Node;
import org.apache.sis.storage.hdf.ObjectReference;
import org.apache.sis.storage.hdf.base.IOStructure;
import org.apache.sis.storage.hdf.base.SuperBlock;


/**
 * Builds the tree table returned by {@code HDF5Store.getNativeMetadata()}.
 * The structures read from the file (superblock, datatypes, dataspaces, layouts, filters, …)
 * are shown by reflection on their fields, which avoids the need to export those classes.
 * Values are copied as immutable objects or strings, so the tree does not expose internal arrays.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class NativeMetadata {
    /**
     * Maximal depth of nested structures, as a safety against unexpectedly deep object graphs.
     */
    private static final int MAX_DEPTH = 8;

    /**
     * Maximal number of array elements to show. Longer arrays are truncated.
     */
    private static final int MAX_ARRAY_LENGTH = 32;

    /**
     * Prefix of the names of the classes which are structures of this module, shown by reflection.
     */
    private static final String STRUCTURE_PACKAGE = "org.apache.sis.storage.hdf.";

    /**
     * Package of the input/output classes, which are not structures of the file.
     */
    private static final String IO_PACKAGE = "org.apache.sis.storage.hdf.io";

    /**
     * Structures in the process of being formatted, for protection against cycles.
     */
    private final Set<Object> path = Collections.newSetFromMap(new IdentityHashMap<>());

    /**
     * Creates a new builder.
     */
    private NativeMetadata() {
    }

    /**
     * Returns the structures of a <abbr>HDF5</abbr> file as a tree.
     *
     * @param  superblock  the superblock of the file.
     * @param  root        the root group of the file.
     * @return the structures of the file as a tree with {@code NAME} and {@code VALUE} columns.
     */
    public static TreeTable create(final SuperBlock superblock, final GroupNode root) {
        final var table = new DefaultTreeTable(TableColumn.NAME, TableColumn.VALUE);
        final TreeTable.Node top = table.getRoot();
        top.setValue(TableColumn.NAME, Constants.FORMAT_NAME);
        final var builder = new NativeMetadata();
        builder.appendStructure(top, "Superblock", superblock, 0);
        builder.appendNode(top, root);
        return table;
    }

    /**
     * Appends a group or a dataset, together with its content.
     *
     * @param parent  the tree node where to append the group or dataset.
     * @param node    the group or dataset to append.
     */
    private void appendNode(final TreeTable.Node parent, final Node node) {
        final TreeTable.Node child = parent.newChild();
        child.setValue(TableColumn.NAME, node.getName());
        if (node instanceof GroupNode group) {
            child.setValue(TableColumn.VALUE, "Group");
            appendAttributes(child, group.getAttributes());
            appendMap(child, "Soft links", group.getSoftLinks());
            appendMap(child, "External links", group.getExternalLinks());
            for (final Node component : group.components()) {
                appendNode(child, component);
            }
        } else if (node instanceof DatasetNode dataset) {
            child.setValue(TableColumn.VALUE, (dataset instanceof DimensionScaleNode) ? "Dimension scale" : "Dataset");
            appendStructure(child, "Datatype",   dataset.getDataType(),  0);
            appendStructure(child, "Dataspace",  dataset.getDataspace(), 0);
            appendStructure(child, "Layout",     dataset.getLayout(),    0);
            appendStructure(child, "Filters",    dataset.getFilter(),    0);
            appendStructure(child, "Fill value", dataset.getFillValue(), 0);
            appendAttributes(child, dataset.getAttributes());
        }
    }

    /**
     * Appends the attributes of a group or dataset, sorted by name.
     *
     * @param parent      the tree node where to append the attributes.
     * @param attributes  the attributes to append.
     */
    private void appendAttributes(final TreeTable.Node parent, final Map<String,Object> attributes) {
        appendMap(parent, "Attributes", new TreeMap<>(attributes));
    }

    /**
     * Appends the entries of the given map under a node of the given name, if the map is not empty.
     *
     * @param parent   the tree node where to append the entries.
     * @param name     name of the node containing the entries.
     * @param entries  the entries to append.
     */
    private void appendMap(final TreeTable.Node parent, final String name, final Map<String,?> entries) {
        if (!entries.isEmpty()) {
            final TreeTable.Node node = parent.newChild();
            node.setValue(TableColumn.NAME, name);
            for (final Map.Entry<String,?> entry : entries.entrySet()) {
                final TreeTable.Node child = node.newChild();
                child.setValue(TableColumn.NAME,  entry.getKey());
                child.setValue(TableColumn.VALUE, toValue(entry.getValue()));
            }
        }
    }

    /**
     * Appends a structure read from the file, with its fields as children.
     *
     * @param parent  the tree node where to append the structure.
     * @param name    name of the node to create.
     * @param value   the structure, or {@code null} if none.
     * @param depth   depth of the structure, for protection against too deep graphs.
     */
    private void appendStructure(final TreeTable.Node parent, final String name, final Object value, final int depth) {
        if (value == null) {
            return;
        }
        final TreeTable.Node node = parent.newChild();
        node.setValue(TableColumn.NAME,  name);
        node.setValue(TableColumn.VALUE, value.getClass().getSimpleName());
        if (depth >= MAX_DEPTH || !path.add(value)) {
            return;
        }
        try {
            for (final Field field : IOStructure.getAllFields(new ArrayList<>(), value.getClass())) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                final Object v;
                try {
                    field.setAccessible(true);
                    v = field.get(value);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    continue;       // Field not accessible, ignore.
                }
                appendProperty(node, field.getName(), v, depth + 1);
            }
        } finally {
            path.remove(value);
        }
    }

    /**
     * Appends a field value: either a nested structure, a list of structures, or a single value.
     *
     * @param parent  the tree node where to append the value.
     * @param name    name of the field.
     * @param value   value of the field, or {@code null} if none.
     * @param depth   depth of the value, for protection against too deep graphs.
     */
    private void appendProperty(final TreeTable.Node parent, final String name, final Object value, final int depth) {
        if (value == null) {
            return;
        }
        if (isStructure(value)) {
            appendStructure(parent, name, value, depth);
            return;
        }
        final List<?> elements;
        if (value instanceof Object[] array) {
            elements = java.util.Arrays.asList(array);
        } else if (value instanceof Iterable<?> iterable) {
            final var list = new ArrayList<Object>();
            iterable.forEach(list::add);
            elements = list;
        } else {
            elements = null;
        }
        if (elements != null && elements.stream().anyMatch(NativeMetadata::isStructure)) {
            final TreeTable.Node node = parent.newChild();
            node.setValue(TableColumn.NAME,  name);
            node.setValue(TableColumn.VALUE, elements.size() + " elements");
            final int n = Math.min(elements.size(), MAX_ARRAY_LENGTH);
            for (int i=0; i<n; i++) {
                appendProperty(node, "[" + i + ']', elements.get(i), depth);
            }
            return;
        }
        final TreeTable.Node node = parent.newChild();
        node.setValue(TableColumn.NAME,  name);
        node.setValue(TableColumn.VALUE, toValue(value));
    }

    /**
     * Returns whether the given value is a structure of this module to show by reflection.
     * Groups, datasets, input/output classes and enumerations are excluded.
     *
     * @param  value  the value to test.
     * @return whether the value should be shown by reflection on its fields.
     */
    private static boolean isStructure(final Object value) {
        if (value == null || value instanceof Enum<?> || value instanceof Node || value instanceof ObjectReference) {
            return false;
        }
        final Class<?> type = value.getClass();
        return type.getName().startsWith(STRUCTURE_PACKAGE) && !type.getPackageName().equals(IO_PACKAGE);
    }

    /**
     * Returns the given value as an immutable object for the {@code VALUE} column.
     * Arrays are formatted as strings, truncated if too long.
     *
     * @param  value  the value to convert.
     * @return an immutable representation of the given value.
     */
    private static Object toValue(final Object value) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean
                || value instanceof Character || value instanceof Enum<?> || value instanceof ObjectReference)
        {
            return value;
        }
        if (value instanceof Class<?> type) {
            return type.getSimpleName();
        }
        if (value.getClass().isArray()) {
            return arrayToString(value);
        }
        return String.valueOf(value);
    }

    /**
     * Formats the given array, truncated to {@value #MAX_ARRAY_LENGTH} elements.
     *
     * @param  array  the array to format.
     * @return string representation of the array.
     */
    private static String arrayToString(final Object array) {
        final int length = Array.getLength(array);
        final var sb = new StringBuilder().append('[');
        final int n = Math.min(length, MAX_ARRAY_LENGTH);
        for (int i=0; i<n; i++) {
            if (i != 0) sb.append(", ");
            final Object e = Array.get(array, i);
            sb.append((e != null && e.getClass().isArray()) ? arrayToString(e) : String.valueOf(e));
        }
        if (length > n) {
            sb.append(", … (").append(length).append(" elements)");
        }
        return sb.append(']').toString();
    }
}
