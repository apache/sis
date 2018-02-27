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
package org.apache.sis.internal.jaxb.gcx;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;


/**
 * The {@code <gcx:FileName>} element, which may be used as a substitute of {@code <gco:CharacterString>}.
 * This is used for the URI in {@link org.apache.sis.metadata.iso.identification.DefaultBrowseGraphic}.
 * Example:
 *
 * {@preformat xml
 *   <fileName>
 *      <gcx:FileName src="../path/wkj98723.jpg">Overview</gcx:FileName>
 *   </fileName>
 * }
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.4
 *
 * @see <a href="https://issues.apache.org/jira/browse/SIS-119">SIS-119</a>
 *
 * @since 0.4
 * @module
 */
@XmlType(name = "FileName_PropertyType")
@XmlRootElement(name = "FileName")
public final class FileName {
    /**
     * The value of the {@code src} attribute, which is the file path.
     */
    @XmlAttribute
    private String src;

    /**
     * A human-readable description of the filename.
     * If {@link #src} is null, then this will be taken as the file path.
     */
    @XmlValue
    private String value;

    /**
     * Empty constructor for JAXB only.
     */
    public FileName() {
    }

    /**
     * Creates a new {@code <gml:FileName>} for the given URI.
     *
     * @param uri  the string representation of the URI.
     */
    public FileName(final String uri) {
        src   = uri;
        value = uri.substring(uri.lastIndexOf('/') + 1);
    }

    /**
     * Returns the file path, or {@code null} if none.
     *
     * <div class="note"><b>Note:</b>
     * Returning {@code null} is usually not recommended for a {@code toString()} method,
     * but this class is for internal usage only.</div>
     *
     * @return the file path, or {@code null} if none.
     */
    @Override
    public String toString() {
        return (src != null) ? src : value;
    }
}
