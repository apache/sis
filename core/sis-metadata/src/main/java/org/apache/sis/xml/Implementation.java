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
package org.apache.sis.xml;

import javax.xml.bind.JAXBContext;


/**
 * Known JAXB implementations.
 * This enumeration allows to set vendor-specific marshaller properties.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.8
 * @module
 */
enum Implementation {
    /**
     * JAXB implementation bundled in the JDK.
     */
    INTERNAL("com.sun.xml.internal.bind.indentString"),

    /**
     * JAXB implementation provided in a separated JAR, used for instance by Glassfish.
     */
    ENDORSED("com.sun.xml.bind.indentString"),

    /**
     * Unrecognized implementation.
     */
    OTHER(null);

    /**
     * The prefix of property names which are provided in external (endorsed) implementation of JAXB.
     * This is slightly different than the prefix used by the implementation bundled with the JDK 6,
     * which is {@code "com.sun.xml.internal.bind"}.
     */
    private static final String ENDORSED_PREFIX = "com.sun.xml.bind.";

    /**
     * The prefix of property names which are provided in internal implementation of JAXB
     * (the one bundled with the JDK 6).
     */
    private static final String INTERNAL_PREFIX = "com.sun.xml.internal.bind.";

    /**
     * The JAXB property for setting the indentation string, or {@code null} if none.
     */
    final String indentKey;

    /**
     * Creates a new enumeration value for a JAXB implementation.
     */
    private Implementation(final String indentKey) {
        this.indentKey = indentKey;
    }

    /**
     * Detects if we are using the endorsed JAXB implementation (the one provided in separated JAR files)
     * or the one bundled in JDK. We use the JAXB context package name as a criterion:
     *
     * <ul>
     *   <li>JAXB endorsed JAR uses    {@code "com.sun.xml.bind.*"}</li>
     *   <li>JAXB bundled in JDK uses  {@code "com.sun.xml.internal.bind.*"}</li>
     * </ul>
     *
     * @param  context  the JAXB context for which to detect the implementation.
     * @return the implementation, or {@code OTHER} if unknown.
     */
    public static Implementation detect(final JAXBContext context) {
        if (context != null) {
            final String classname = context.getClass().getName();
            if (classname.startsWith(INTERNAL_PREFIX)) {
                return INTERNAL;
            } else if (classname.startsWith(ENDORSED_PREFIX)) {
                return ENDORSED;
            }
        }
        return OTHER;
    }

    /**
     * Returns {@code false} if the given (un)marshaller property should be silently ignored.
     * A value of {@code true} does not necessarily mean that the given property is supported,
     * but that the caller should either support the property or throw an exception.
     *
     * <p>This method excludes the {@code "com.sun.xml.bind.*"} properties if the implementation
     * is not {@link #ENDORSED} or {@link #INTERNAL}. We do not distinguish between the endorsed
     * and internal namespaces since Apache SIS uses only the endorsed namespace and lets
     * {@code org.apache.sis.xml.Pooled} do the conversion to internal namespace if needed.</p>
     *
     * @param  key  the property key to test.
     * @return {@code false} if the given property should be silently ignored.
     */
    boolean filterProperty(final String key) {
        // We user 'indentKey' as a sentinel value for identifying INTERNAL and ENDORSED cases.
        return (indentKey != null) || !key.startsWith(ENDORSED_PREFIX);
    }

    /**
     * Converts the given key from {@code "com.sun.xml.bind.*"} to {@code "com.sun.xml.internal.bind.*"} namespace.
     * This method is invoked when the JAXB implementation is known to be the {@link #INTERNAL} one. We perform this
     * conversion for allowing Apache SIS to ignore the difference between internal and endorsed JAXB.
     *
     * @param  key  the key that may potentially a endorsed JAXB key.
     * @return the key as an internal JAXB key, or the given key unchanged if it is not an endorsed JAXB key.
     */
    static String toInternal(String key) {
        if (key.startsWith(ENDORSED_PREFIX)) {
            final StringBuilder buffer = new StringBuilder(key.length() + 10);
            key = buffer.append(INTERNAL_PREFIX).append(key, ENDORSED_PREFIX.length(), key.length()).toString();
        }
        return key;
    }
}
