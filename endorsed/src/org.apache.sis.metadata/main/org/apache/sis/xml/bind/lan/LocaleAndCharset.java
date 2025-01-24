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
package org.apache.sis.xml.bind.lan;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.AbstractSet;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Iterator;
import java.util.Objects;
import java.nio.charset.Charset;
import org.apache.sis.util.privy.Bag;
import org.apache.sis.util.privy.Unsafe;
import org.apache.sis.util.privy.CollectionsExt;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable.Node;


/**
 * Utility methods for handling {@code Map<Locale,Charset>} as separated collections.
 * Locale and character set were separated properties in legacy ISO 19115:2003 but become combined
 * under a single {@link PT_Locale} entity in ISO 19115:2014. This change is not really convenient
 * in Java since the standard {@link Locale} and {@link Charset} objects are separated entities.
 * This class provides two services for managing that:
 *
 * <ul>
 *   <li>Static methods, used mostly for JAXB marshalling and unmarshalling.</li>
 *   <li>Implementation of {@link Node} for viewing a {@code Map.Entry<Locale,Charset>} as a
 *       {@link Locale} node with a {@link Charset} child. This is used for providing textual
 *       representation of metadata.</li>
 * </ul>
 *
 * Example:
 *
 * <pre class="text">
 *     Identification info
 *      ├─Abstract………………………………………………………………………………… Some data.
 *      ├─Locale (1 of 2)……………………………………………………………… en_US
 *      │   └─Character set………………………………………………………… US-ASCII
 *      └─Locale (2 of 2)……………………………………………………………… fr
 *          └─Character set………………………………………………………… ISO-8859-1</pre>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class LocaleAndCharset implements Node {
    /**
     * The node containing a {@code Map.Entry<Locale,Charset>} value.
     * This is the node to replace by this {@link LocaleAndCharset} view.
     */
    private final Node node;

    /**
     * Creates a new node for the given entry. The user object associated to
     * the given node must be an instance of {@code Map.Entry<Locale,Charset>}.
     *
     * @param  node  the node to wrap.
     */
    public LocaleAndCharset(final Node node) {
        this.node = node;
    }

    /**
     * Delegates to wrapped node since this {@link LocaleAndCharset} is a substitute for that node.
     */
    @Override
    public Node getParent() {
        return node.getParent();
    }

    /*
     * Inherit `isEditable(…)` from the `Node` interface:
     * Considers this node as non-editable since it represents the key in a map, and keys cannot be modified
     * through the `Map.Entry` interface. However, `Child` will be editable for the value column.
     */

    /**
     * Returns {@code false} since this node can have a children, which is the {@link Child}.
     */
    @Override
    public boolean isLeaf() {
        return false;
    }

    /**
     * Returns the key or the value of the given {@link Map.Entry}. If the given object is not a map entry
     * or is null, then it is returned as-is. This latter case should never happen (the object shall always be
     * a non-null map entry), but we nevertheless check for making the code more robust to ill-formed metadata.
     * We apply this tolerance because this method is used (indirectly) for {@code toString()} implementations,
     * and failure in those methods make debugging more difficult (string representations are often requested
     * when the developer knows that there is a problem to investigate).
     *
     * @param  object  the map entry for which to get the key or the value.
     * @param  key     {@code true} for fetching the key, or {@code false} for fetching the value.
     * @return the requested key or value, or the given object itself if it is not a map entry.
     */
    private static Object keyOrValue(Object object, final boolean key) {
        if (object instanceof Map.Entry<?,?>) {
            final Map.Entry<?,?> entry = (Map.Entry<?,?>) object;
            object = key ? entry.getKey() : entry.getValue();
        }
        return object;
    }

    /**
     * Returns the user object associated to this node. For this node, that object is the key (a {@link Locale}) of the
     * map entry. For the {@link Child}, the user object will be the value (a {@link Charset}) of the same map entry.
     */
    @Override
    public Object getUserObject() {
        return keyOrValue(node.getUserObject(), true);
    }

    /**
     * Returns the value associated to the given column of this node. This method delegates to the wrapped node,
     * then extract the key component of the map entry if the requested column is the value.
     */
    @Override
    public <V> V getValue(final TableColumn<V> column) {
        return separateValue(column, true);
    }

    /**
     * Implementation of {@link #getValue(TableColumn)} also used by the {@link Child}.
     */
    private <V> V separateValue(final TableColumn<V> column, final boolean key) {
        V value = node.getValue(column);
        if (column == TableColumn.VALUE) {
            value = column.getElementType().cast(keyOrValue(value, key));
        }
        return value;
    }

    /**
     * Always throws an exception since we cannot edit the key of a map entry. Attempts to edit other columns
     * than the value column will also cause an exception to be thrown, but the error message provided by the
     * wrapped node is more detailed.
     */
    @Override
    public <V> void setValue(final TableColumn<V> column, final V value) {
        if (column == TableColumn.VALUE) {
            throw new UnsupportedOperationException();
        } else {
            node.setValue(column, value);
        }
    }

    /**
     * Returns the list of children, which is implemented by this class itself.
     * The children are {@link Charset} values associated to the {@link Locale}.
     * The list contains O or 1 element.
     */
    @Override
    public Collection<Node> getChildren() {
        return new AbstractList<Node>() {
            /** Returns the number {@link Charset} associated to the {@link Locale}, which is 0 or 1. */
            @Override public int size() {
                return keyOrValue(node.getUserObject(), false) != null ? 1 : 0;
            }

            /** Returns a child node wrapping the {@link Charset} ad the given index. */
            @Override public Node get(final int index) {
                Objects.checkIndex(index, 1);
                return new Child();
            }
        };
    }

    /**
     * Creates a new child only if none exists.
     */
    @Override
    public Node newChild() {
        if (keyOrValue(node.getUserObject(), false) != null) {
            throw new UnsupportedOperationException();
        }
        return new Child();
    }

    /**
     * The only child of the node containing a {@link Locale} value. This child contains the associated {@link Charset} value.
     * That value is replaceable if the wrapped node is editable, which depends on whether the metadata instance is modifiable
     * or not.
     */
    private final class Child implements Node {
        @Override public Node             getParent()                  {return LocaleAndCharset.this;}
        @Override public Object           getUserObject()              {return keyOrValue(node.getUserObject(), false);}
        @Override public boolean          isEditable(TableColumn<?> c) {return node.isEditable(c);}
        @Override public boolean          isLeaf()                     {return true;}
        @Override public Collection<Node> getChildren()                {return Collections.emptySet();}

        /** Returns the value at the given column, with hard-coded names. */
        @Override public <V> V getValue(final TableColumn<V> column) {
            final String value;
            if (column == TableColumn.IDENTIFIER) {
                value = "characterSet";
            } else if (column == TableColumn.NAME) {
                value = "Character set";
            } else {
                return separateValue(column, false);
            }
            return column.getElementType().cast(value);
        }

        /** Sets the value in the map entry key wrapped by this node. */
        @Override public <V> void setValue(final TableColumn<V> column, V value) {
            if (column == TableColumn.VALUE) {
                /*
                 * We rely on Entry.setValue(Object) implementation to perform type checks.
                 * This is the case with SIS implementation backed by PropertyAccessor,
                 * but we cannot guarantee that this is the case with user-provided map.
                 */
                Unsafe.setValue((Map.Entry<?,?>) node.getUserObject(), value);
            } else {
                node.setValue(column, value);
            }
        }

        /** Returns a hash code value for this node. */
        @Override public int hashCode() {
            return ~getParent().hashCode();
        }

        /** Tests this node with the given object for equality. */
        @Override public boolean equals(final Object other) {
            return (other instanceof Child) && getParent().equals(((Child) other).getParent());
        }
    }

    /**
     * Returns a hash code value for this node.
     */
    @Override
    public int hashCode() {
        return node.hashCode() ^ 37;
    }

    /**
     * Tests this node with the given object for equality.
     * Two {@link LocaleAndCharset} instances are considered equal if they wrap the same node.
     */
    @Override
    public boolean equals(final Object other) {
        return (other instanceof LocaleAndCharset) && node.equals(((LocaleAndCharset) other).node);
    }

    /**
     * Returns the language(s) used within the resource. The returned collection supports the {@code add(Locale)} method
     * in order to enable XML unmarshalling of legacy ISO 19157 metadata documents. That hack is not needed for newer XML
     * documents (ISO 19115-3:2016).
     *
     * @param  locales  the map of locales and character sets, or {@code null}.
     * @return language(s) used within the resource, or {@code null}.
     */
    public static Collection<Locale> getLanguages(final Map<Locale,Charset> locales) {
        if (locales == null) {
            return null;
        }
        return new AbstractSet<Locale>() {
            @Override public int size() {
                return locales.size();
            }
            @Override public void clear() {
                locales.clear();                        // Default implementation would invoke Iterator.remove() anyway.
            }
            @Override public boolean contains(Object o) {
                return locales.containsKey(o);
            }
            @Override public Iterator<Locale> iterator() {
                return locales.keySet().iterator();
            }
            @Override public boolean add(final Locale locale) {
                // We need containsKey(…) check because value may be null: Map.putIfAbsent(…) is not sufficient.
                if (locale == null || locales.containsKey(locale)) {
                    return false;
                }
                final Charset encoding = locales.remove(null);
                locales.put(locale, encoding);
                return true;
            }
        };
    }

    /**
     * Returns the character coding standard used for the resource. The returned collection supports {@code add(Charset)}
     * method in order to enable XML unmarshalling of legacy ISO 19157 metadata documents. That hack is not be needed for
     * newer (ISO 19115-3:2016) XML documents.
     *
     * @param  locales  the map of locales and character sets, or {@code null}.
     * @return character coding standard(s) used, or {@code null}.
     */
    public static Collection<Charset> getCharacterSets(final Map<Locale,Charset> locales) {
        if (locales == null) {
            return null;
        }
        return new Bag<Charset>() {
            @Override public int size() {
                return locales.size();
            }
            @Override public void clear() {
                locales.clear();                        // Default implementation would invoke Iterator.remove() anyway.
            }
            @Override public boolean contains(Object o) {
                return locales.containsValue(o);
            }
            @Override public Iterator<Charset> iterator() {
                return locales.values().iterator();
            }
            @Override public boolean add(final Charset encoding) {
                if (encoding == null) {
                    return false;
                }
                for (final Map.Entry<Locale,Charset> entry : locales.entrySet()) {
                    if (entry.getValue() == null) {
                        entry.setValue(encoding);
                        return true;
                    }
                }
                return locales.putIfAbsent(null, encoding) != encoding;
            }
        };
    }

    /**
     * Sets the language(s) used within the resource.
     * This method preserves the character sets if possible.
     *
     * @param  locales    the map of locales and character sets, or {@code null}.
     * @param  newValues  the new languages.
     * @return the given map, or a new map if necessary and the given map was null.
     */
    public static Map<Locale,Charset> setLanguages(Map<Locale,Charset> locales, final Collection<? extends Locale> newValues) {
        final Charset encoding = (locales != null) ? CollectionsExt.first(locales.values()) : null;
        if (newValues == null || newValues.isEmpty()) {
            if (locales != null) {
                locales.clear();
            }
        } else {
            if (locales == null) {
                locales = new LinkedHashMap<>();
            }
            locales.keySet().retainAll(newValues);
            for (final Locale locale : newValues) {
                locales.putIfAbsent(locale, null);
            }
        }
        /*
         * If an encoding was defined before invocation of this method and is not associated to any
         * locale specified in `newValues`, preserve that encoding in an entry with null locale.
         * Note: `locales` is non-null if `encoding` is non-null.
         */
        if (encoding != null && !locales.values().contains(encoding)) {
            locales.put(null, encoding);
        }
        return locales;
    }

    /**
     * Sets the character coding standard(s) used within the resource. Current implementation takes
     * only the first {@link Charset} and set the encoding of all locales to that character set.
     * This is suboptimal, but this approach is used only for implementation of deprecated methods.
     *
     * @param  locales    the map of locales and character sets, or {@code null}.
     * @param  newValues  the new character coding standard(s).
     * @return the given map, or a new map if necessary and the given map was null.
     */
    public static Map<Locale,Charset> setCharacterSets(Map<Locale,Charset> locales, final Collection<? extends Charset> newValues) {
        final Charset encoding = CollectionsExt.first(newValues);
        if (locales != null || encoding != null) {
            if (locales == null) {
                locales = new LinkedHashMap<>();
            }
            if (locales.isEmpty()) {
                locales.put(null, encoding);
            } else {
                for (final Map.Entry<Locale,Charset> entry : locales.entrySet()) {
                    entry.setValue(encoding);
                }
            }
        }
        return locales;
    }
}
