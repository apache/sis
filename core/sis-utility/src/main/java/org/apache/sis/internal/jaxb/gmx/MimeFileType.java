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
package org.apache.sis.internal.jaxb.gmx;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;


/**
 * The {@code <gmx:MimeFileType>} element, which may be used as a substitute of {@code <gco:CharacterString>}.
 * This is used in {@link org.apache.sis.metadata.iso.identification.DefaultBrowseGraphic}.
 * Example:
 *
 * {@preformat xml
 *   <fileType>
 *     <gmx:MimeFileType type="image/tiff">Graphic TIFF</gmx:MimeFileType>
 *   </fileType>
 * }
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 *
 * @see <a href="https://issues.apache.org/jira/browse/SIS-119">SIS-119</a>
 */
@XmlType(name = "MimeFileType_PropertyType")
@XmlRootElement(name = "MimeFileType")
public final class MimeFileType {
    /**
     * The value of the {@code type} attribute, which is the mime type.
     */
    @XmlAttribute
    private String type;

    /**
     * A human-readable description of the mime type. If {@link #type} is null,
     * then this will be taken as the mime type.
     */
    @XmlValue
    private String value;

    /**
     * Empty constructor for JAXB only.
     */
    public MimeFileType() {
    }

    /**
     * Creates a new {@code <gml:MimeFileType>} for the given type.
     *
     * @param type The MIME type.
     */
    public MimeFileType(final String type) {
        this.type  = type;
        this.value = type; // May provide a more human-redeable value in a future SIS version.
    }

    /**
     * Returns the MIME type, or {@code null} if none.
     *
     * <div class="note"><b>Note:</b>
     * Returning {@code null} is usually not recommended for a {@code toString()} method,
     * but this class is for internal usage only.</div>
     *
     * @return The MIME type, or {@code null} if none.
     */
    @Override
    public String toString() {
        return (type != null) ? type : value;
    }
}
