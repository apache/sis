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

import jakarta.xml.bind.JAXBContext;


/**
 * Known JAXB implementations.
 * This enumeration allows to set vendor-specific marshaller properties.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
enum Implementation {
    /**
     * JAXB implementation provided by Jakarta Glassfish.
     */
    GLASSFISH("org.glassfish.jaxb.indentString"),

    /**
     * JAXB implementation before JAXB moved to Jakarta.
     */
    ENDORSED("com.sun.xml.bind.indentString"),

    /**
     * Unrecognized implementation.
     */
    OTHER(null);

    /**
     * The prefix of property names for {@link #ENDORSED} implementation.
     */
    private static final String ENDORSED_PREFIX = "com.sun.xml.bind.";

    /**
     * The prefix of property names for {@link #GLASSFISH} implementation.
     */
    private static final String GLASSFISH_PREFIX = "org.glassfish.jaxb.";

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
     * Detects the JAXB implementation in use.
     * This method uses the JAXB context package name as a criterion.
     *
     * @param  context  the JAXB context for which to detect the implementation.
     * @return the implementation, or {@link #OTHER} if unknown.
     */
    public static Implementation detect(final JAXBContext context) {
        if (context != null) {
            final String classname = context.getClass().getName();
            if (classname.startsWith(GLASSFISH_PREFIX)) {
                return GLASSFISH;
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
     * @param  key  the property key to test.
     * @return {@code false} if the given property should be silently ignored.
     */
    boolean filterProperty(final String key) {
        // We user `indentKey` as a sentinel value for identifying a recognized implementation.
        return (indentKey != null) || !key.startsWith(ENDORSED_PREFIX);
    }
}
