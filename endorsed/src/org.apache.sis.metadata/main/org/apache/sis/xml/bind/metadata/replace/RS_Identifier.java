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
package org.apache.sis.xml.bind.metadata.replace;

import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.Identifier;
import org.apache.sis.metadata.TitleProperty;
import org.apache.sis.metadata.iso.*;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;


/**
 * Identifier using {@code <gmd:RS_Identifier>} XML element name.
 * This is used for (un)marshalling legacy metadata only. Example:
 *
 * {@snippet lang="xml" :
 *   <gmd:RS_Identifier>
 *     <gmd:authority>
 *       <gmd:CI_Citation>
 *         <gmd:title>
 *           <gco:CharacterString>EPSG</gco:CharacterString>
 *         </gmd:title>
 *       </gmd:CI_Citation>
 *     </gmd:authority>
 *     <gmd:code>
 *       <gco:CharacterString>4326</gco:CharacterString>
 *     </gmd:code>
 *   </gmd:RS_Identifier>
 *   }
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@TitleProperty(name = "code")
@XmlType(name = "RS_Identifier_Type", namespace = LegacyNamespaces.GMD)
@XmlRootElement(name = "RS_Identifier", namespace = LegacyNamespaces.GMD)
public final class RS_Identifier extends DefaultIdentifier {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 1297774369491643461L;

    /**
     * Constructor for JAXB.
     */
    public RS_Identifier() {
    }

    /**
     * Creates a new identifier from the specified one.
     *
     * @see #wrap(Identifier)
     */
    private RS_Identifier(final Identifier identifier) {
        super(identifier);
    }

    /**
     * Returns the given identifier as a {@code RS_Identifier} instance.
     *
     * @param  object  the identifier to wrap, or {@code null} if none.
     * @return the wrapped object, or {@code null}.
     */
    public static RS_Identifier wrap(final Identifier object) {
        if (object == null || object instanceof RS_Identifier) {
            return (RS_Identifier) object;
        }
        return new RS_Identifier(object);
    }
}
