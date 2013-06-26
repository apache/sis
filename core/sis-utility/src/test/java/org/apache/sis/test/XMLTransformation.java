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
package org.apache.sis.test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.util.StringBuilders;


/**
 * A predefined set of transformations that can be applied on XML strings before processing.
 * Those transformations are applied in order to allow to make some tests to be tolerant to
 * different JAXB implementations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public strictfp enum XMLTransformation {
    /**
     * Transformations related to XML in the {@value org.apache.xml.Namespaces#GML} namespace.
     */
    GML("gml", "(?s).*xmlns\\s*(:\\s*\\w+)?\\s*=\\s*\"" + Namespaces.GML + "\".*");

    /**
     * The expected prefix.
     */
    private final String standardPrefix;

    /**
     * The pattern for getting the XML prefix of a given namespace.
     */
    public final Pattern findPrefix;

    /**
     * Creates a new enum for the given pattern.
     */
    private XMLTransformation(final String prefix, final String findPrefix) {
        this.standardPrefix = prefix;
        this.findPrefix = Pattern.compile(findPrefix);
    }

    /**
     * If the given XML does not contain a {@code xmlns} attribute for the namespace represented by this enum,
     * removes the prefix which was expected for this namespace. For example if the expected XML was:
     *
     * {@preformat xml
     *   <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
     *   <gml:TimeInstant xmlns:gml="http://www.opengis.net/gml">
     *     <gml:timePosition>1992-01-01T01:00:00.000+01:00</gml:timePosition>
     *   </gml:TimeInstant>
     * }
     *
     * but the actual XML is:
     *
     * {@preformat xml
     *   <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
     *   <TimeInstant xmlns="http://www.opengis.net/gml">
     *     <timePosition>1992-01-01T01:00:00.000+01:00</timePosition>
     *   </TimeInstant>
     * }
     *
     * then this remove will remove all occurrence of {@code "gmd:"} in the expected string.
     *
     * @param  expected The expected XML.
     * @param  actual The actual XML.
     * @return The new expected XML.
     */
    public String optionallyRemovePrefix(String expected, final CharSequence actual) {
        final Matcher matcher = findPrefix.matcher(actual);
        if (matcher.matches() && matcher.groupCount() == 1) {
            final String prefix = matcher.group(1);
            if (prefix == null) {
                final StringBuilder buffer = new StringBuilder(expected);
                StringBuilders.replace(buffer, standardPrefix + ':', "");
                StringBuilders.replace(buffer, "xmlns:" + standardPrefix, "xmlns");
                expected = buffer.toString();
            }
        }
        return expected;
    }
}
