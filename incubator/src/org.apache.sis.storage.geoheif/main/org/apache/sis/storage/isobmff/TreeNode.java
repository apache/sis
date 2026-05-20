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
import java.util.Locale;
import java.util.logging.Logger;
import java.text.NumberFormat;
import java.math.BigInteger;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.annotation.Target;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.StandardCharsets;
import org.apache.sis.math.NumberType;
import org.apache.sis.util.Localized;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.DefaultTreeTable;
import org.apache.sis.util.collection.TreeTableFormat;
import org.apache.sis.util.internal.shared.PropertyFormat;
import org.apache.sis.util.internal.shared.TreeTableForGUI;
import org.apache.sis.storage.isobmff.base.ItemInfoEntry;
import org.apache.sis.storage.geoheif.GeoHeifStore;


/**
 * Base class of boxes or items. This class implements the {@link #toString()} method with reflection.
 * For allowing reflection, all subclasses must be public (but not necessarily in exported packages)
 * and all fields to show shall be public.
 *
 * @author Martin Desruisseaux (Geomatys)
 */
public abstract class TreeNode {
    /**
     * Marker annotation for fields such as four-character codes (<abbr>4CC</abbr>) or unsigned integers.
     * When this annotation is present, the field value should be converted with a call to a method such
     * as {@link Short#toUnsignedInt(short)}, depending on the {@linkplain #value() type}.
     */
    @Documented
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    protected @interface Interpretation {
        /**
         * How the field value should be interpreted.
         *
         * @return field interpretation (e.g., <abbr>4CC</abbr> or unsigned integer).
         */
        Type value();

        /**
         * Whether the field can be used as a summary of the box that contains this field.
         * If more than one field is flagged as a summary, the first field with a non-null value will be used.
         *
         * @return {@code true} if the field can be shown as a box summary.
         */
        boolean summary() default false;
    }

    /**
     * How to interpret an integer value (if not a signed integer) or a character string.
     * This is used for instructing {@link TreeNode#toString()} to replace numerical codes
     * by a human-readable strings.
     */
    protected enum Type {
        /**
         * No particular interpretation. This annotation can be used with fields of any type.
         * The use of this type is equivalent to a field with no annotation. This type is for
         * making possible to set the {@link Interpretation#summary()} flag on a string field.
         * The {@link #format(TreeBuilder, Number)} method should not be invoked on this value.
         */
        NONE,

        /**
         * The annotated integer should be interpreted as a four-character code (<abbr>4CC</abbr>).
         * The annotated field should be of type {@code int} or {@code int[]}.
         * Values will be formatted with {@link #formatFourCC(int)}.
         */
        FOURCC {
            @Override String format(final TreeBuilder tree, final Number value) {
                return formatFourCC(value.intValue());
            }
        },

        /**
         * The annotated integer is the identifier of an item, generally in a {@code itemID} field.
         * The name of the identified item is specified in a separated {@link ItemInfoEntry}.
         * The annotated field should be of type {@code int} or {@code int[]}.
         */
        IDENTIFIER {
            @Override String format(final TreeBuilder tree, final Number value) {
                return formatUnsigned(value);
            }
        },

        /**
         * The annotated integer should be interpreted as an unsigned integer.
         * Value will be formatted as with {@link Long#toUnsignedString(long)}.
         * If this annotation is absent, either negative values are allowed,
         * or the unsigned value has already been converted to a wider type.
         */
        UNSIGNED;

        /**
         * Formats the given integer according the type identified by this enumeration.
         *
         * @param  tree   builder of the tree to format.
         * @param  value  the integer value to format.
         * @return the formatted value, or {@code null} if absent.
         */
        String format(final TreeBuilder tree, Number value) {
            switch (value) {
                case Byte    i: value = Byte   .toUnsignedInt (i); break;
                case Short   i: value = Short  .toUnsignedInt (i); break;
                case Integer i: value = Integer.toUnsignedLong(i); break;
                default: {
                    final long n = value.longValue();
                    if (n < 0) {
                        value = new BigInteger(Long.toUnsignedString(n));
                    }
                    break;
                }
            }
            return tree.integerFormat.format(value);
        }

        /**
         * Returns the value of the given annotation, or {@code null} if none.
         *
         * @param  itpr  the annotation, or {@code null}.
         * @return the annotation value, or {@code null} if none.
         */
        static Type of(final Interpretation itpr) {
            if (itpr != null) {
                final Type type = itpr.value();
                if (type != NONE) return type;
            }
            return null;
        }
    }

    /**
     * Returns the string representation of the given four-character code (<abbr>4CC</abbr>).
     * This method can be used for formatting a human-readable message when a type is unrecognized.
     *
     * @param  value  the code for which to get the human-readable string representation.
     * @return string representation of the given four-character code, or {@code null} if the given code is zero.
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
     * Returns the string representation of the given number interpreted as an unsigned integer.
     *
     * @param  value  the value to format as an unsigned integer.
     * @return the formatted value.
     */
    static String formatUnsigned(final Number value) {
        return Long.toUnsignedString(switch (value) {
            case Byte    i -> Byte   .toUnsignedLong(i);
            case Short   i -> Short  .toUnsignedLong(i);
            case Integer i -> Integer.toUnsignedLong(i);
            default        -> value.longValue();
        });
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
     * Returns a string representation of this tree node for debugging purposes.
     * This method shows all non-static public fields, including inherited fields, in no particular order.
     *
     * @return a string representation of this element for debugging purposes.
     */
    @Override
    public final String toString() {
        return toTree(null, getClass().getSimpleName(), false).toString();
    }

    /**
     * Returns <abbr>HEIF</abbr> boxes and their fields as a tree.
     * Used for showing native metadata or for debugging purposes.
     *
     * @param  locale       the locale to use, or {@code null} for the default.
     * @param  rootName     name of the root node.
     * @param  withSummary  whether to put a summary text in container nodes.
     * @return fields contained in this node, together with child boxes.
     *
     * @todo It is sometime possible, through the tree cell values, to modify the content of internal arrays.
     *       This is unsafe, we should makes this implementation safer before this module is released.
     */
    public final TreeTable toTree(final Locale locale, final String rootName, final boolean withSummary) {
        final var builder = new TreeBuilder(locale, withSummary);
        final TreeTable.Node root = builder.tree.getRoot();
        root.setValue(TableColumn.NAME, rootName);
        builder.appendProperties(this, root);
        return builder.tree;
    }

    /**
     * A tree of box properties, with a string representation that excludes the {@code VALUES} column.
     * The value column is replaced by the {@code VALUE_AS_TEXT} column at {@link #toString()} time.
     */
    @SuppressWarnings("serial")
    private static final class Tree extends DefaultTreeTable implements TreeTableForGUI, Localized {
        /**
         * The locale to use, or {@code null} for the default.
         */
        private final Locale locale;

        /**
         * Creates an initially empty tree table.
         *
         * @param  locale  the locale to use in string representations.
         */
        Tree(final Locale locale) {
            super(TableColumn.NAME, TableColumn.VALUE, TableColumn.VALUE_AS_TEXT);
            this.locale = locale;
        }

        /**
         * Returns the locale used for formatting the tree.
         *
         * @return the locale to use, or {@code null} for the default.
         */
        @Override
        public final Locale getLocale() {
            return locale;
        }

        /**
         * Returns whether the given value produced by the given node is a title.
         */
        @Override
        public boolean isNodeTitle(final TreeTable.Node node, final Object value) {
            return (value instanceof NodeSummary);
        }

        /**
         * Returns a string representation of the tree table.
         */
        @Override
        public final String toString() {
            final var tf = new TreeTableFormat(locale, null);
            tf.setColumns(TableColumn.NAME, TableColumn.VALUE_AS_TEXT);
            return tf.format(this);
        }
    }

    /**
     * Builder of a string representation of tree nodes.
     * Columns are {@code NAME}, {@code VALUE} and {@code VALUE_AS_TEXT}.
     *
     * <h2>Properties</h2>
     * Producing a better formatting sometime requires contextual information.
     * For example, <abbr>HEIF</abbr> declares relationship between boxes with property index.
     * It is much easier to read the tree if property indexes are replaced by property names.
     * For making that possible, some information must be communicated between
     * the box that contains property names and the box that use them.
     * This is the purpose of the {@code set/getContext(…)} methods.
     */
    protected static final class TreeBuilder extends PropertyFormat {
        /**
         * The result for formatting the enclosing node and all children as a tree table.
         */
        private final Tree tree;

        /**
         * Properties saved during the creation of the tree.
         */
        private final Map<Class<?>, Object> properties;

        /**
         * Information about an item referenced by its identifier.
         * Keys are {@link ItemInfoEntry#itemID} values.
         */
        private final Map<Integer, ItemInfoEntry> names;

        /**
         * The format to use for integers.
         */
        private final NumberFormat integerFormat;

        /**
         * Whether the tree should put a summary of children node in the "value as text" column.
         * This summary duplicates the information contained inside the node, so this field should
         * be {@code false} for {@code toString()} implementation. However, it is helpful when the
         * node may be collapsed, as in <abbr>GUI</abbr> applications.
         */
        private final boolean withSummary;

        /**
         * Creates an initially empty tree table.
         */
        TreeBuilder(final Locale locale, final boolean withSummary) {
            super(new StringBuilder());
            setLineSeparator(" ¶ ");
            this.withSummary   = withSummary;
            this.names         = new HashMap<>();
            this.properties    = new HashMap<>();
            this.tree          = new Tree(locale);
            this.integerFormat = NumberFormat.getIntegerInstance(locale);
        }

        /**
         * Returns the locale used for formatting the tree.
         *
         * @return the locale to use, or {@code null} for the default.
         */
        @Override
        public final Locale getLocale() {
            return tree.getLocale();
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
         * Remembers the name of the given entry.
         *
         * @param  entry  entry for which to remember the name.
         */
        public final void addItemName(final ItemInfoEntry entry) {
            names.put(entry.itemID, entry);       // In case of conflict, keep the most recent item.
        }

        /**
         * Returns the name of the given entry.
         *
         * @param  itemID  identifier of the entry to get.
         * @return name of the identified entry, or {@code null} if none.
         */
        public final String getItemName(final Number itemID) {
            final ItemInfoEntry entry = names.get(itemID.intValue());
            return (entry != null) ? entry.itemName : null;
        }

        /**
         * Invoked by {@link PropertyFormat} for formatting a value which has not been recognized as one of
         * the types to be handled in a special way. In particular numbers and dates should be handled here.
         */
        @Override
        protected String toString(final Object value) {
            if (value instanceof Number && NumberType.isInteger(value.getClass())) {
                return integerFormat.format(value);
            } else {
                return super.toString(value);
            }
        }

        /**
         * Appends public fields as child nodes of the given root, ignoring null values and empty arrays.
         * This method may invoke itself recursively if some values are other {@code TreeNode} instances.
         *
         * @param  source  the tree node to format.
         * @param  target  the node where to add fields.
         */
        private void appendProperties(final TreeNode source, final TreeTable.Node target) {
            CharSequence summary = null;
            source.prependTreeNodes(this, target);
            for (final Field field : source.getClass().getFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                final Object value;
                try {
                    value = field.get(source);
                    if (value == null) continue;
                } catch (IllegalAccessException e) {
                    Logging.unexpectedException(Logger.getLogger(Reader.LOGGER_NAME), GeoHeifStore.class, "getNativeMetadata", e);
                    continue;
                }
                if (value instanceof TreeNode[]) {
                    for (final TreeNode child : (TreeNode[]) value) {
                        if (child != null) {
                            appendProperties(child, addNode(target, child.typeName(), child, null));
                        }
                    }
                } else if (value.getClass().isArray()) {
                    /*
                     * Case of an array of integers, floating point numbers or character strings.
                     * Identifiers are added as children because their `VALUE` column may provide
                     * the names of the identified items.
                     */
                    final Type type = Type.of(field.getAnnotation(Interpretation.class));
                    final TreeTable.Node addTo;
                    if (type == Type.IDENTIFIER) {
                        addTo = addNode(target, camelCaseToWords(field), value, null);
                        // No `VALUE_AS_TEXT` because this is an array that we will develop below.
                    } else {
                        addTo = null;
                    }
                    final var values = new String[Array.getLength(value)];
                    for (int i=0; i < values.length; i++) {
                        final Object element = Array.get(value, i);
                        if (element != null) {
                            if (type != null) {
                                final var n = (Number) element;
                                if (addTo != null) {
                                    addNode(addTo, formatUnsigned(n), n, getItemName(n));
                                } else {
                                    values[i] = type.format(this, n);
                                }
                            } else {
                                values[i] = formatUsingStringBuilder(element);
                            }
                        }
                    }
                    if (addTo == null) {
                        addNode(target, camelCaseToWords(field), value, String.join(", ", values));
                    }
                } else {
                    /*
                     * Case where the property is a single element (not an array).
                     * Identifier codes will be converted to their four-character code (4CC) representations.
                     * The fields to convert to 4CC are identified by the `@Interpretation` annotation.
                     */
                    if (value instanceof TreeNode addTo) {
                        appendProperties(addTo, addNode(target, camelCaseToWords(field), addTo, null));
                    } else {
                        final Interpretation itpr = field.getAnnotation(Interpretation.class);
                        final Type type = Type.of(itpr);
                        final String text;
                        if (type != null) {
                            text = type.format(this, (Number) value);
                        } else {
                            text = formatUsingStringBuilder(value);
                        }
                        if (text != null) {
                            addNode(target, camelCaseToWords(field), value, text);
                        }
                        /*
                         * If the field is annotated with `Interpretation(…, summary=true)`,
                         * take the field value is a summary of the whole node.
                         */
                        if (summary == null && withSummary && itpr != null && itpr.summary()) {
                            if (type == Type.IDENTIFIER) {
                                String name = getItemName((Number) value);
                                if (name != null) summary = name;
                                // Do not wrap in `NodeSummary` because we want that name always visible.
                            } else if (text != null) {
                                summary = new NodeSummary(text);
                            }
                        }
                    }
                }
            }
            source.appendTreeNodes(this, target);
            /*
             * The `VALUE_AS_TEXT` column of a `TreeNode` usually has no value (only the fields have values).
             * However, if we can identify a text that summarizes the content of the node, we will show that
             * text when the node is collapsed.
             */
            if (summary == null) {
                final TreeTable.Node child = Containers.peekIfSingleton(target.getChildren());
                if (child != null) {
                    summary = NodeSummary.of(child.getValue(TableColumn.VALUE_AS_TEXT));
                }
            }
            if (summary != null) {
                target.setValue(TableColumn.VALUE_AS_TEXT, summary);
            }
        }

        /**
         * Convenience method for adding a child node if the given value is non-null.
         *
         * @param  target  where to add the node.
         * @param  name    name of the node or property to add.
         * @param  value   value of the node, or {@code null} for skipping the node.
         */
        public final void addNode(final TreeTable.Node target, final String name, final Object value) {
            if (value != null) {
                addNode(target, name, value, formatUsingStringBuilder(value));
            }
        }

        /**
         * Convenience method for adding a child node.
         *
         * @param  target       where to add the node.
         * @param  name         name of the node or property to add.
         * @param  value        value of the node, or {@code null} for a node without value.
         * @param  valueAsText  string representation of the value, or {@code null} if none.
         * @return the node which has been added.
         */
        public static TreeTable.Node addNode(final TreeTable.Node target, String name, Object value, String valueAsText) {
            final TreeTable.Node child = target.newChild();
            child.setValue(TableColumn.NAME, name);
            child.setValue(TableColumn.VALUE, value);
            child.setValue(TableColumn.VALUE_AS_TEXT, valueAsText);
            return child;
        }

        /**
         * Returns the name of the given field with camel-case converted to a sentence of words.
         *
         * @param  field  the field for which to get the name.
         * @return field name as a sequence of words.
         */
        private static String camelCaseToWords(final Field field) {
            return CharSequences.camelCaseToWords(field.getName(), false).toString();
        }
    }

    /**
     * Inserts custom properties before the properties inferred from the public fields.
     * This method is invoked automatically by {@link #toTree toTree(…)} for generating
     * the first nodes, before the nodes inferred by reflection.
     *
     * @param  tree    builder of the tree to format.
     * @param  target  the node where to add properties.
     */
    protected void prependTreeNodes(TreeBuilder tree, TreeTable.Node target) {
    }

    /**
     * Appends custom properties after the properties inferred from the public fields.
     * This method is invoked automatically by {@link #toTree toTree(…)} for generating
     * the last nodes, after the nodes inferred by reflection.
     *
     * <p>The default implementation adds an artificial node saying that some nodes are missing
     * if the class is annotated with {@link Incomplete}.</p>
     *
     * @param  tree    builder of the tree to format.
     * @param  target  the node where to add properties.
     */
    protected void appendTreeNodes(TreeBuilder tree, TreeTable.Node target) {
        if (getClass().isAnnotationPresent(Incomplete.class)) {
            final Vocabulary vocabulary = Vocabulary.forLocale(tree.getLocale());
            final TreeTable.Node child = target.newChild();
            child.setValue(TableColumn.NAME, vocabulary.getString(Vocabulary.Keys.UnsupportedProperties));
            child.setValue(TableColumn.VALUE_AS_TEXT, vocabulary.getString(Vocabulary.Keys.Omitted));
        }
    }
}
