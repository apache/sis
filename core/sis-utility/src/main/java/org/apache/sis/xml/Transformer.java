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

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import javax.xml.stream.events.Attribute;


/**
 * Base class of XML reader or writer replacing the namespaces used by JAXB by namespaces used in the XML document,
 * or conversely (depending on the direction of the I/O operation).
 *
 * See {@link TransformingNamespaces} for more information.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.0
 * @since   1.0
 * @module
 */
abstract class Transformer {
    /**
     * The external XML format version to (un)marshal from.
     */
    final TransformVersion version;

    /**
     * Temporary list of attributes after their namespace change.
     * This list is recycled for each XML element to be read or written.
     */
    final List<Attribute> renamedAttributes;

    /**
     * Creates a new XML reader or writer.
     */
    Transformer(final TransformVersion version) {
        this.version = version;
        renamedAttributes = new ArrayList<>();
    }

    /**
     * Removes the trailing slash in given URI, if any. It is caller's responsibility
     * to ensure that the URI is not null and not empty before to invoke this method.
     */
    static String removeTrailingSlash(String uri) {
        final int end = uri.length() - 1;
        if (uri.charAt(end) == '/') {
            uri = uri.substring(0, end);
        }
        return uri;
    }

    /**
     * Returns a snapshot of {@link #renamedAttributes} list and clears the later.
     */
    final List<Attribute> attributes() {
        final List<Attribute> attributes;
        switch (renamedAttributes.size()) {
            case 0:  attributes = Collections.emptyList(); break;      // Avoid object creation for this common case.
            case 1:  attributes = Collections.singletonList(renamedAttributes.remove(0)); break;
            default: attributes = Arrays.asList(renamedAttributes.toArray(new Attribute[renamedAttributes.size()]));
                     renamedAttributes.clear();
                     break;
        }
        return attributes;
    }
}
