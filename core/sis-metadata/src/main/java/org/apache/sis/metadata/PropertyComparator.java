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

import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Method;
import javax.xml.bind.annotation.XmlType;

import org.opengis.annotation.UML;
import org.opengis.annotation.Obligation;


/**
 * The comparator for sorting the properties in a metadata object.
 * Since the comparator uses (among other criterion) the property names, this class
 * incidentally defines static methods for inferring those names from the methods.
 *
 * <p>This comparator uses the following criterion, in priority order:</p>
 * <ol>
 *   <li>Deprecated properties are last.</li>
 *   <li>If the property order is specified by a {@link XmlType} annotation,
 *       then this comparator complies to that order.</li>
 *   <li>Otherwise this comparator sorts mandatory methods first, followed by
 *       conditional methods, then optional ones.</li>
 *   <li>If the order can not be inferred from the above, then the comparator
 *       fallbacks on alphabetical order.</li>
 * </ol>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
final class PropertyComparator implements Comparator<Method> {
    /**
     * The prefix for getters on boolean values.
     */
    private static final String IS = "is";

    /**
     * The prefix for getters (general case).
     */
    private static final String GET = "get";

    /**
     * The prefix for setters.
     */
    static final String SET = "set";

    /**
     * Methods and property names specified in the {@link XmlType} annotation.
     * Entries description:
     *
     * <ul>
     *   <li>Keys in this map are either {@link String} or {@link Method} instances:
     *     <ul>
     *       <li>{@code String} keys property names as given by {@link XmlType#propOrder()}.
     *           They are computed at construction time and do not change after construction.</li>
     *       <li>{@code Method} keys will be added after construction, only as needed.</li>
     *     </ul>
     *   </li>
     *
     *   <li>Key is associated to an index that specify its position in descending order.
     *       For example the property associated to integer 0 shall be sorted last.
     *       This descending order is only an implementation convenience.</li>
     * </ul>
     */
    private final Map<Object,Integer> order;

    /**
     * The implementation class, or the interface is the implementation class is unknown.
     */
    private final Class<?> implementation;

    /**
     * Creates a new comparator for the given implementation class.
     *
     * @param implementation The implementation class, or the interface if the implementation class is unknown.
     */
    PropertyComparator(Class<?> implementation) {
        this.implementation = implementation;
        order = new HashMap<Object,Integer>();
        do {
            final XmlType xml = implementation.getAnnotation(XmlType.class);
            if (xml != null) {
                final String[] propOrder = xml.propOrder();
                for (int i=propOrder.length; --i>=0;) {
                    /*
                     * Add the entries in reverse order because we are iterating from the child class to
                     * the parent class, and we want the properties in the parent class to be sorted first.
                     * If duplicated properties are found, keep the first occurence (i.e. sort the property
                     * with the most specialized child that declared it).
                     */
                    final Integer old = order.put(propOrder[i], order.size());
                    if (old != null) {
                        order.put(propOrder[i], old);
                    }
                }
            }
            implementation = implementation.getSuperclass();
        } while (implementation != null);
    }

    /**
     * Returns {@code true} if the given method is deprecated, either in the interface that declare the method
     * or in the implementation class. A method may be deprecated in the implementation but not in the interface
     * when the implementation has been updated for a new standard, while the interface is still reflecting the
     * old standard.
     *
     * @param  implementation The implementation class, or the interface is the implementation class is unknown.
     * @param  method The method to check for deprecation.
     * @return {@code true} if the method is deprecated.
     */
    static boolean isDeprecated(final Class<?> implementation, Method method) {
        if (!MetadataStandard.IMPLEMENTATION_CAN_ALTER_API) {
            return method.isAnnotationPresent(Deprecated.class);
        }
        if (method.isAnnotationPresent(Deprecated.class)) {
            return true;
        }
        if (method.getDeclaringClass() == implementation) {
            return false;
        }
        try {
            method = implementation.getMethod(method.getName(), (Class[]) null);
        } catch (NoSuchMethodException e) {
            // Should never happen since the implementation is supposed to implement
            // the interface that declare the method given in argument.
            throw new AssertionError(e);
        }
        return method.isAnnotationPresent(Deprecated.class);
    }

    /**
     * Compares the given methods for order.
     */
    @Override
    public int compare(final Method m1, final Method m2) {
        final boolean deprecated = isDeprecated(implementation, m1);
        if (deprecated != isDeprecated(implementation, m2)) {
            return deprecated ? +1 : -1;
        }
        int c = indexOf(m2) - indexOf(m1); // indexOf(…) are sorted in descending order.
        if (c == 0) {
            final UML a1 = m1.getAnnotation(UML.class);
            final UML a2 = m2.getAnnotation(UML.class);
            if (a1 != null) {
                if (a2 == null) return +1;   // Sort annotated elements first.
                c = order(a1) - order(a2);   // Mandatory elements must be first.
                if (c == 0) {
                    // Fallback on alphabetical order.
                    c = a1.identifier().compareToIgnoreCase(a2.identifier());
                }
                return c;
            } else if (a2 != null) {
                return -1; // Sort annotated elements first.
            }
            // Fallback on alphabetical order.
            c = m1.getName().compareToIgnoreCase(m2.getName());
        }
        return c;
    }

    /**
     * Returns a higher number for obligation which should be first.
     */
    private static int order(final UML uml) {
        final Obligation obligation = uml.obligation();
        if (obligation != null) {
            switch (obligation) {
                case MANDATORY:   return 1;
                case CONDITIONAL: return 2;
                case OPTIONAL:    return 3;
                case FORBIDDEN:   return 4;
            }
        }
        return 5;
    }

    /**
     * Returns the index of the given method, or -1 if the method is not found.
     * If positive, the index returned by this method correspond to a sorting in descending order.
     */
    private int indexOf(final Method method) {
        /*
         * Check the cached value computed by previous call to 'indexOf(…)'.
         * Example: "getExtents"
         */
        Integer index = order.get(method);
        if (index == null) {
            /*
             * Check the value computed from @XmlType.propOrder() value.
             * Inferred from the method name, so name is often plural.
             * Example: "extents"
             */
            String name = method.getName();
            name = toPropertyName(name, prefix(name).length());
            index = order.get(name);
            if (index == null) {
                /*
                 * Do not happen, except when we have private methods or deprecated public methods
                 * used as bridge between legacy and more recent standards (e.g. ISO 19115:2003 to
                 * ISO 19115:2014), especially when cardinality changed between the two standards.
                 * Example: "extent"
                 */
                final UML uml = method.getAnnotation(UML.class);
                if (uml == null || (index = order.get(uml.identifier())) == null) {
                    index = -1;
                }
            }
            order.put(method, index);
        }
        return index;
    }

    /**
     * Returns the prefix of the specified method name. If the method name doesn't starts with
     * a prefix (for example {@link org.opengis.metadata.quality.ConformanceResult#pass()}),
     * then this method returns an empty string.
     */
    static String prefix(final String name) {
        if (name.startsWith(GET)) {
            return GET;
        }
        if (name.startsWith(IS)) {
            return IS;
        }
        if (name.startsWith(SET)) {
            return SET;
        }
        return "";
    }

    /**
     * Returns {@code true} if the specified string starting at the specified index contains
     * no lower case characters. The characters don't have to be in upper case however (e.g.
     * non-alphabetic characters)
     */
    private static boolean isAcronym(final String name, int offset) {
        final int length = name.length();
        while (offset < length) {
            final int c = name.codePointAt(offset);
            if (Character.isLowerCase(c)) {
                return false;
            }
            offset += Character.charCount(c);
        }
        return true;
    }

    /**
     * Removes the {@code "get"} or {@code "is"} prefix and turn the first character after the
     * prefix into lower case. For example the method name {@code "getTitle"} will be replaced
     * by the property name {@code "title"}. We will perform this operation only if there is
     * at least 1 character after the prefix.
     *
     * @param  name The method name (can not be {@code null}).
     * @param  base Must be the result of {@code prefix(name).length()}.
     * @return The property name (never {@code null}).
     */
    static String toPropertyName(String name, final int base) {
        final int length = name.length();
        if (length > base) {
            if (isAcronym(name, base)) {
                name = name.substring(base);
            } else {
                final int up = name.codePointAt(base);
                final int lo = Character.toLowerCase(up);
                if (up != lo) {
                    name = new StringBuilder(length - base).appendCodePoint(lo)
                            .append(name, base + Character.charCount(up), length).toString();
                } else {
                    name = name.substring(base);
                }
            }
        }
        return name.intern();
    }
}
