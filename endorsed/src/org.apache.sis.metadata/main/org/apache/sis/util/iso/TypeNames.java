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
package org.apache.sis.util.iso;

import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.net.URI;
import java.math.BigDecimal;
import org.opengis.util.GenericName;
import org.opengis.util.TypeName;
import org.opengis.util.NameSpace;
import org.opengis.util.NameFactory;
import org.opengis.util.InternationalString;
import org.opengis.geometry.Geometry;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.resources.Errors;


/**
 * Implements the mapping between {@link Class} and {@link TypeName} documented in {@link DefaultTypeName}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class TypeNames {
    /**
     * The mapping between {@link TypeName} and {@link Class} as documented in {@link DefaultTypeName}.
     * When searching for a name from a class, the values will be tested in iteration order.
     *
     * <p>{@link #toTypeName(NameFactory, Class)} will <strong>not</strong> iterate over all entries.
     * Numbers and character strings are handled in a special way, so we do not need to iterate on them.
     * We arbitrarily use {@code Boolean.class} as the sentinel value for detecting when to stop iteration.</p>
     *
     * <p>This map shall not be modified after construction.</p>
     */
    private static final Map<String,Class<?>> MAPPING = new LinkedHashMap<>(16);
    static {
        final Map<String,Class<?>> m = MAPPING;
        m.put("URI",       URI.class);
        m.put("DateTime",  Date.class);
        m.put("PT_Locale", Locale.class);
        m.put("Geometry",  Geometry.class);             // Was GM_Object in ISO 19107:2003.
        m.put("Decimal",   BigDecimal.class);
        m.put("Boolean",   Boolean.class);              // Used as a sentinel value for stopping iteration.

        // MD_Metadata is not specified because it is handled through UML annotations.

        // Entries below this point are handled in a special way.
        m.put("FreeText",        InternationalString.class);
        m.put("CharacterString", String.class);
        m.put("Real",            Double.class);
        m.put("Decimal",         Double.class);
        m.put("Integer",         Integer.class);
    }

    /**
     * The "OGC" namespace.
     */
    private final NameSpace ogcNS;

    /**
     * The "class" namespace.
     */
    private final NameSpace classNS;

    /**
     * Creates a new factory of type names.
     */
    TypeNames(final NameFactory factory) {
        ogcNS   = factory.createNameSpace(factory.createLocalName(null, Constants.OGC), null);
        classNS = factory.createNameSpace(factory.createLocalName(null, "class"), null);
    }

    /**
     * Infers the type name from the given class.
     *
     * @param  factory     the same factory as the one given to the constructor.
     * @param  valueClass  the value class for which to get a type name.
     * @return a type name for the given class (never {@code null}).
     */
    final TypeName toTypeName(final NameFactory factory, final Class<?> valueClass) {
        String name;
        NameSpace ns = ogcNS;
search: if (CharSequence.class.isAssignableFrom(valueClass)) {
            name = InternationalString.class.isAssignableFrom(valueClass) ? "FreeText" : "CharacterString";
        } else if (Numbers.isNumber(valueClass)) {
            name = Numbers.isInteger(valueClass) ? "Integer" : "Real";
        } else {
            /*
             * Iterate over the special cases, excluding the numbers and character sequences
             * since they were verified in the above statements.
             */
            final Iterator<Map.Entry<String,Class<?>>> it = MAPPING.entrySet().iterator();
            Class<?> base;
            do {
                final Map.Entry<String,Class<?>> entry = it.next();
                base = entry.getValue();
                if (base.isAssignableFrom(valueClass)) {
                    name = entry.getKey();
                    break search;
                }
            } while (base != Boolean.class);    // See MAPPING javadoc for the role of Boolean as a sentinel value.
            /*
             * Found no special case. Checks for the UML annotation, to be also formatted in the "OGC:" namespace.
             * If no UML identifier is found, then we will format the Java class in the "class:" namespace. We use
             * Class.getName() - not Class.getCanonicalName() - because we want a name readable by Class.forName(…).
             */
            name = Types.getStandardName(valueClass);
            if (name == null) {
                ns = classNS;
                name = valueClass.getName();    // See above comment.
            }
        }
        if (factory instanceof DefaultNameFactory) {
            return ((DefaultNameFactory) factory).createTypeName(ns, name, valueClass);
        }
        return factory.createTypeName(ns, name);
    }

    /**
     * Returns the class for a {@code TypeName} made of the given scope and name.
     * This method is the converse of {@link #toTypeName(NameFactory, Class)}.
     * There is 3 kinds of return value:
     *
     * <ul>
     *   <li>{@code null} if the namespace or the name is unrecognized, without considering that as an error.</li>
     *   <li>{@code Void.TYPE} if {@code namespace} is recognized, but not the {@code name}.
     *       This is a sentinel value to be considered as an error by {@link DefaultTypeName} constructor.</li>
     *   <li>Otherwise the class for the given name.</li>
     * </ul>
     *
     * @param  namespace  the namespace, case-insensitive. Can be any value, but this method recognizes
     *         only {@code "OGC"}, {@code "class"} and {@code null}. Other namespaces will be ignored.
     * @param  name  the type name, case-sensitive.
     * @return the class, or {@code null} if the given namespace is not recognized,
     *         or {@code Void.TYPE} if the namespace is recognized but not the type name.
     * @throws ClassNotFoundException if {@code namespace} is {@code "class"} but {@code name} is not
     *         the name of a reachable class.
     */
    static Class<?> toClass(final String namespace, final String name) throws ClassNotFoundException {
        Class<?> c;
        if (namespace == null || namespace.equalsIgnoreCase(Constants.OGC)) {
            c = MAPPING.get(name);
            if (c == null) {
                c = Types.forStandardName(name);
                if (c == null && namespace != null) {
                    c = Void.TYPE;          // Unknown name in OGC namespace.
                }
            }
        } else if (namespace.equalsIgnoreCase("class")) {
            c = Class.forName(name);
        } else {
            c = null;                   // Not an "OGC" or "class" namespace.
        }
        return c;
    }

    /**
     * Ensures that the given class is non-null and not {@link Void#TYPE}.
     * This is a helper method for callers of {@link #toTypeName(NameFactory, Class)}.
     */
    static boolean isValid(final Class<?> valueClass) {
        if (valueClass == Void.TYPE) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "valueClass", "void"));
        }
        return (valueClass != null);
    }

    /**
     * Null-safe getter for the namespace argument to be given to {@link #toClass(String, String)}.
     */
    static String namespace(final NameSpace ns) {
        if (ns != null && !ns.isGlobal()) {
            final GenericName name = ns.name();
            if (name != null) {
                return name.toString();
            }
        }
        return null;
    }
}
