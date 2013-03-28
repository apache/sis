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
import java.util.IdentityHashMap;
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
 *   <li>If the property order is specified by a {@link XmlType} annotation,
 *       then this comparator complies to that order.</li>
 *   <li>Otherwise this comparator sorts mandatory methods first, followed by
 *       conditional methods, then optional ones.</li>
 *   <li>If the order can not be inferred from the above, then the comparator
 *       fallbacks on alphabetical order.</li>
 * </ol>
 *
 * The first criterion (mandatory methods first) is necessary for reducing the risk
 * of ambiguity in the {@link MetadataTreeTable#parse} method.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.4)
 * @version 0.3
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
     * Methods specified in the {@link XmlType} annotation, or {@code null} if none.
     */
    private final String[] order;

    /**
     * Indices of methods in the {@link #order} array, created when first needed.
     */
    private Map<Method,Integer> indices;

    /**
     * Creates a new comparator for the given implementation class.
     *
     * @param implementation The implementation class, or {@code null} if unknown.
     */
    PropertyComparator(final Class<?> implementation) {
        if (implementation != null) {
            final XmlType xml = implementation.getAnnotation(XmlType.class);
            if (xml != null) {
                order = xml.propOrder();
                return;
            }
        }
        order = null;
    }

    /**
     * Compares the given methods for order.
     */
    @Override
    public int compare(final Method m1, final Method m2) {
        int c = indexOf(m1) - indexOf(m2);
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
     * Returns the index of the given method, or {@code order.length} if the method is not found.
     */
    private int indexOf(final Method method) {
        int i = 0;
        if (order != null) {
            if (indices == null) {
                indices = new IdentityHashMap<>();
            } else {
                Integer index = indices.get(method);
                if (index != null) {
                    return index;
                }
            }
            String name = method.getName();
            name = toPropertyName(name, prefix(name).length());
            while (i < order.length) {
                if (name.equals(order[i])) {
                    break;
                }
                i++;
            }
            indices.put(method, i);
        }
        return i;
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
