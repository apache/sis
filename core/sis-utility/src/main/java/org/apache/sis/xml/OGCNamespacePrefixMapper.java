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

import com.sun.xml.internal.bind.marshaller.NamespacePrefixMapper;


/**
 * A mapper between namespace prefixes and URL they represent.
 * This class is an alternative to the standard {@link javax.xml.bind.annotation.XmlSchema}
 * annotation. However this class extends the non-standard {@code NamespacePrefixMapper} class,
 * which is bundled in Sun/Oracle JDK since version 6. We have to use this mapper because the
 * {@code @XmlSchema} annotation doesn't work as expected before JAXB 2.2.4, and the JDK 6 is
 * bundled with JAXB 2.1. Even with working {@code @XmlSchema} annotations, this mapper still
 * a convenient may to gain more control like choosing a default namespace at runtime.
 *
 * <div class="section">Immutability and thread safety</div>
 * This final class is immutable and thus inherently thread-safe.
 *
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see <a href="http://java.sun.com/webservices/docs/1.5/jaxb/vendorProperties.html">JAXB extensions</a>
 * @see <a href="https://issues.apache.org/jira/browse/SIS-74">SIS-74</a>
 */
final class OGCNamespacePrefixMapper extends NamespacePrefixMapper {
    /**
     * If non-null, this namespace will be the default namespace (the one without prefix).
     */
    private final String defaultNamespace;

    /**
     * Creates a new prefix mapper.
     * This constructor is invoked by reflection and needs to be public for that reason.
     *
     * @param defaultNamespace The namespace which doesn't need prefix, or {@code null} if none.
     */
    public OGCNamespacePrefixMapper(final String defaultNamespace) {
        this.defaultNamespace = defaultNamespace;
    }

    /**
     * Returns a preferred prefix for the given namespace URI.
     *
     * @param  namespace  The namespace URI for which the prefix needs to be found.
     * @param  suggestion The suggested prefix, returned if the given namespace is not recognized.
     * @param  required   {@code true} if this method is not allowed to return the empty string.
     * @return The prefix inferred from the namespace URI.
     */
    public String getPreferredPrefix(final String namespace, final String suggestion, final boolean required) {
        /*
         * If the given namespace is the one defined as default namespace, this implementation
         * just returns an empty string. This namespace will be defined with a xmlns parameter,
         * and all tags in this namespace will not have any namespace prefix.
         */
        if (!required && namespace.equals(defaultNamespace)) {
            return "";
        }
        return Namespaces.getPreferredPrefix(namespace, suggestion);
    }
}
