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
package org.apache.sis.storage.isobmff;

import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.annotation.Target;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.StandardCharsets;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.DefaultTreeTable;
import org.apache.sis.util.collection.TreeTableFormat;
import org.apache.sis.storage.isobmff.base.ItemInfoEntry;


/**
 * Base class of box or items. This class implements the {@link #toString()} method with reflection.
 * For allowing reflection, all subclasses must be public (but not necessarily in exported packages)
 * and all fields to show shall be public.
 *
 * @author Martin Desruisseaux (Geomatys)
 */
public abstract class TreeNode {
    /**
     * Marker annotation for four-character codes (<abbr>4CC</abbr>) or unsigned integers.
     * When this annotation is present, the field value should be converted with a call to
     * a method such as {@link Short#toUnsignedInt(short)}. If this annotation is absent,
     * either negative values are allowed, or the unsigned value has already been converted
     * to a wider type.
     */
    @Documented
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    protected @interface Interpretation {
        /**
         * How the field value should be interpreted.
         *
         * @return field interpretation (<abbr>4CC</abbr> or unsigned integer).
         */
        Type value();
    }

    /**
     * How to interpret an integer value otherwise than as a signed integer.
     * This is used for instructing {@link TreeNode#toString()} to replace
     * the numerical code by a human-readable string.
     */
    protected enum Type {
        /**
         * The integer should be interpreted as a four-character code (<abbr>4CC</abbr>).
         * Value should be formatted with {@link #formatFourCC(int)}.
         */
        FOURCC {
            @Override String format(final Tree context, final Number value) {
                return formatFourCC(value.intValue());
            }
        },

        /**
         * An identifier as an integer value.
         * The item name will be appended after the identifier if it is found.
         */
        IDENTIFIER {
            @Override String format(final Tree context, final Number value) {
                String name = super.format(context, value);
                ItemInfoEntry entry = context.names.get(value.intValue());
                if (entry != null && entry.itemName != null) {
                    name = name + " → " + entry.itemName;
                }
                return name;
            }
        },

        /**
         * The integer should be interpreted as an unsigned integer.
         * Value should be formatted with {@link Long#toUnsignedString(long)}.
         */
        UNSIGNED;

        /**
         * Formats the given integer according the type identified by this enumeration.
         *
         * @param  context  the tree being formatted. Can be used for fetching contextual information.
         * @param  value    the integer value to format.
         * @return the formatted value, or {@code null} if absent.
         */
        String format(final Tree context, final Number value) {
            return Long.toUnsignedString(switch (value) {
                case Byte    i -> Byte   .toUnsignedLong(i);
                case Short   i -> Short  .toUnsignedLong(i);
                case Integer i -> Integer.toUnsignedLong(i);
                default        -> value.longValue();
            });
        }
    }

    /**
     * Returns the string representation of the given four-character code (<abbr>4CC</abbr>).
     * This method can be used for formatting a human-readable message when a type is unrecognized.
     *
     * @param  value  the code for which to get the human-readable string representation.
     * @return string representation of the given four-character code, or {@code null} if the given code is zero.
     *
     * @see #create(Reader, int)
     */
    public static String formatFourCC(final int value) {
        if (value == 0) return null;
        return new String(new byte[] {
            (byte) (value >>> 24),
            (byte) (value >>> 16),
            (byte) (value >>>  8),
            (byte) (value)
        }, StandardCharsets.ISO_8859_1).trim();
    }

    /**
     * Creates a new box or item.
     */
    protected TreeNode() {
    }

    /**
     * Returns a human-readable name for this node to shown in the tree.
     * The default implementation returns the class name with camel cases
     * replaced by spaces.
     *
     * @return Human-readable name of this node.
     */
    public String typeName() {
        return CharSequences.camelCaseToSentence(getClass().getSimpleName()).toString();
    }

    /**
     * Returns a string representation of element for debugging purposes.
     * This method shows all non-static public fields, including inherited fields, in no particular order.
     *
     * @return a string representation of this element for debugging purposes.
     */
    @Override
    public final String toString() {
        return toTree(getClass().getSimpleName(), false).toString();
    }

    /**
     * Returns <abbr>HEIF</abbr> boxes and their fields as a tree.
     * Used for showing native metadata or for debugging purposes.
     *
     * @param  rootName  name of the root node.
     * @param  withSummary  workaround, see {@link Tree#withSummary}.
     * @return fields contained in this node, together with child boxes.
     *
     * @todo It is sometime possible, through the tree cell values, to modify the content of internal arrays.
     *       This is unsafe, we should makes this implementation safer before this module is released.
     */
    public final TreeTable toTree(final String rootName, final boolean withSummary) {
        final var tree = new Tree(withSummary);
        final TreeTable.Node root = tree.getRoot();
        root.setValue(TableColumn.NAME, rootName);
        tree.appendProperties(this, root);
        return tree;
    }

    /**
     * A tree of box properties, with a string representation that excludes the {@code VALUES} column.
     * The value column is replaced by the {@code VALUE_AS_TEXT} column at {@link #toString()} time.
     *
     * @todo The selection of the column to show should instead be done by overriding a method,
     *       for making possible for the <abbr>GUI</abbr> to also use that information.
     *
     * <h2>Properties</h2>
     * Producing a better formatting sometime requires contextual information.
     * For example, <abbr>HEIF</abbr> declares relationship between boxes with property index.
     * It is much easier to read the tree if property indexes are replaced by property names.
     * For making that possible, some information must be communicated between the box that
     * contains property names and the box that use them. This is the purpose of the
     * {@code set/getContext(…)} methods.
     */
    @SuppressWarnings("serial")
    protected static final class Tree extends DefaultTreeTable {
        /**
         * Properties saved during the creation of the tree.
         */
        private final Map<Class<?>, Object> properties;

        /**
         * Information about an item referenced by its identifier.
         * This map is populated and read directly by {@link Box} subclasses that need it.
         */
        public final Map<Integer, ItemInfoEntry> names;

        /**
         * Whether the tree should put a summary of children node in the "value as text" column.
         * This summary duplicates the information contained inside the node, so this field should
         * be {@code false} for {@code toString()} implementation. However, it is helpful when the
         * node may be collapsed, as in <abbr>GUI</abbr> applications.
         *
         * @todo This is a workaround for not providing a summary in a more dynamic way.
         *       We should remove this field and replace it by a mechanism in GUI for showing
         *       and hiding the summary depending on whether the node is expanded or collapsed.
         */
        private final boolean withSummary;

        /**
         * Creates an initially empty tree table.
         */
        Tree(final boolean withSummary) {
            super(TableColumn.NAME, TableColumn.VALUE, TableColumn.VALUE_AS_TEXT);
            properties = new HashMap<>();
            names = new HashMap<>();
            this.withSummary = withSummary;
        }

        /**
         * Saves an arbitrary value as a contextual information.
         *
         * @param <E>    compile-time type of the value.
         * @param type   type of the value.
         * @param value  the value to save, or {@code null} for removing.
         */
        public final <E> void setContext(final Class<E> type, final E value) {
            if (value != null) {
                properties.put(type, value);
            } else {
                properties.remove(type);
            }
        }

        /**
         * Fetches an arbitrary value from the contextual information.
         *
         * @param  <E>   compile-time type of the value.
         * @param  type  type of the value.
         * @return the value, or {@code null} if none.
         */
        public final <E> E getContext(final Class<E> type) {
            return type.cast(properties.get(type));
        }

        /**
         * Returns a string representation of this tree table.
         */
        @Override public final String toString() {
            final var tf = new TreeTableFormat(null, null);
            tf.setColumns(TableColumn.NAME, TableColumn.VALUE_AS_TEXT);
            return tf.format(this);
        }

        /**
         * Appends public fields as child nodes of the given root, ignoring null values and empty arrays.
         * This method may invoke itself recursively if some values are other {@code TreeNode} instances.
         *
         * @param  source  the tree node to format.
         * @param  target  the node where to add fields.
         * @return proposed summary describing the node, or {@code null} if none.
         */
        private String appendProperties(final TreeNode source, final TreeTable.Node target) {
            String summary = null;
            source.appendTreeNodes(this, target, false);
            for (final Field field : source.getClass().getFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                Object value;
                try {
                    value = field.get(source);
                    if (value == null) continue;
                } catch (IllegalAccessException e) {
                    // May happen if the class is not public or not accessible.
                    value = e;
                }
                /*
                 * Convert identifier codes to their four-character code (4CC) representation.
                 * The fields to convert are identified by the `@Interpretation` annotation.
                 */
                final Class<?> componentType = value.getClass().getComponentType();
                Interpretation format = field.getAnnotation(Interpretation.class);
                if (format != null) {
                    final Type type = format.value();
                    if (value instanceof Number) {
                        value = type.format(this, (Number) value);
                        if (value == null) continue;
                    } else if (componentType != null) {
                        final var output = new String[Array.getLength(value)];
                        for (int i=0; i<output.length; i++) {
                            Object element = Array.get(value, i);
                            if (element != null) {
                                output[i] = (element instanceof Number) ? type.format(this, (Number) element) : element.toString();
                            }
                        }
                        value = output;
                    }
                    if (withSummary && type == Type.IDENTIFIER && summary == null && value instanceof String) {
                        summary = (String) value;
                    }
                } else if (componentType != null) {
                    /*
                     * If the value is an array of nested `TreeNode` instances, append the elements as child nodes.
                     */
                    final int length = Array.getLength(value);
                    if (length == 0) continue;
                    if (value instanceof TreeNode[] children) {
                        if (length != 1) {
                            for (final TreeNode node : children) {
                                if (node != null) {
                                    final TreeTable.Node child = target.newChild();
                                    child.setValue(TableColumn.NAME, node.typeName());
                                    child.setValue(TableColumn.VALUE_AS_TEXT, appendProperties(node, child));
                                    child.setValue(TableColumn.VALUE, child.getValue(TableColumn.VALUE_AS_TEXT));
                                    // VALUE should be null, but the GUI application currently doesn't pickup the
                                    // VALUE_AS_TEXT if VALUE is null.
                                }
                            }
                            continue;
                        } else {
                            value = children[0];
                        }
                    } else {
                        final var buffer = new StringBuilder();
                        Strings.appendWithHeuristic(value, buffer);
                        value = buffer.toString();
                    }
                }
                /*
                 * Finally append the string representation of the value,
                 * or the child elements if the value is a nested node.
                 */
                final TreeTable.Node child = target.newChild();
                String name = CharSequences.camelCaseToWords(field.getName(), true).toString();
                child.setValue(TableColumn.NAME, name);
                if (value instanceof TreeNode) {
                    appendProperties(((TreeNode) value), child);
                } else {
                    child.setValue(TableColumn.VALUE, value);
                    child.setValue(TableColumn.VALUE_AS_TEXT, Utilities.deepToString(value));
                }
            }
            source.appendTreeNodes(this, target, true);
            return summary;
        }
    }

    /**
     * Appends properties other than the ones defined by public fields.
     * This method is invoked automatically by {@link #toTree toTree(…)}
     * for generating the first or last nodes, before or after the nodes inferred by reflection.
     *
     * @param  context  the tree being formatted. Can be used for fetching contextual information.
     * @param  target   the node where to add properties.
     * @param  after    {@code false} for the first nodes, or {@code true} for the last nodes.
     */
    protected void appendTreeNodes(Tree context, TreeTable.Node target, boolean after) {
    }
}
