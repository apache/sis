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
package org.apache.sis.internal.jaxb.gco;

import javax.xml.bind.annotation.XmlValue;


/**
 * JAXB wrapper for an URI in a {@code <gmd:URL>} element, for ISO 19139:2007 compliance.
 * This type was used by legacy XML format inside {@code <gmd:CI_OnlineResource>}, but has
 * been replaced by {@code <gcx:FileName>} in newer ISO 19115-3:2016 standard. Example:
 *
 * {@preformat xml
 *   <gmd:linkage>
 *      <gmd:URL>https://tools.ietf.org/html/rfc1149</gmd:URL>
 *   </gmd:linkage>
 * }
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class GO_URL {
    /**
     * The URI as a string. We uses a string in order to allow
     * the user to catch potential error at unmarshalling time.
     */
    @XmlValue
    private String uri;

    /**
     * Empty constructor for JAXB only.
     */
    GO_URL() {
    }

    /**
     * Builds an adapter for the given URI.
     *
     * @param  value  the URI to marshal.
     */
    GO_URL(final String value) {
        uri = value;
    }

    /**
     * Returns the URI.
     */
    @Override
    public String toString() {
        return uri;
    }
}
