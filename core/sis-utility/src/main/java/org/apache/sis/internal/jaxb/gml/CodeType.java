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
package org.apache.sis.internal.jaxb.gml;

import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import org.opengis.util.NameSpace;
import org.opengis.util.GenericName;
import org.apache.sis.util.iso.Names;
import org.apache.sis.xml.Namespaces;


/**
 * A generalized type to be used for a term, keyword or name.
 * The following schema fragment specifies the expected content contained within this class.
 *
 * {@preformat xml
 *   <complexType name="CodeType">
 *     <simpleContent>
 *       <extension base="<http://www.w3.org/2001/XMLSchema>string">
 *         <attribute name="codeSpace" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *       </extension>
 *     </simpleContent>
 *   </complexType>
 * }
 *
 * {@code CodeType}s are used for:
 *
 * <ul>
 *   <li>{@code GenericName}</li>
 *   <li>{@link LocalName}</li>
 *   <li>{@link ScopedName}</li>
 * </ul>
 *
 * {@code CodeType}s are not used for {@code MemberName} and {@code TypeName}.
 * The two later use a quite different XML structure, with an {@code aName} element instead than a XML value.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@XmlSeeAlso({
    CodeType.LocalName.class,
    CodeType.ScopedName.class
})
public class CodeType {
    /**
     * The term.
     */
    @XmlValue
    String value;

    /**
     * Dictionary, thesaurus, classification scheme, authority, or pattern for the term,
     * or {@code null} if none.
     */
    @XmlAttribute
    @XmlSchemaType(name = "anyURI")
    String codeSpace;

    /**
     * Sets the value from the given name.
     *
     * @param name The name to marshal.
     */
    public final void setName(final GenericName name) {
        this.value = name.toString();
        final NameSpace scope = name.scope();
        if (scope != null && !scope.isGlobal()) {
            codeSpace = scope.name().toString();
        }
    }

    /**
     * Returns the name from the current value.
     *
     * @return The unmarshalled name.
     */
    public GenericName getName() {
        return Names.parseGenericName(codeSpace, ":", value);
    }

    /**
     * The {@code CodeType} as a {@code gco:LocalName}.
     */
    @XmlRootElement(name = "LocalName", namespace = Namespaces.GCO)
    public static final class LocalName extends CodeType {
        @Override public GenericName getName() {
            return Names.createLocalName(codeSpace, ":", value);
        }
    }

    /**
     * The {@code CodeType} as a {@code gco:ScopedName}.
     */
    @XmlRootElement(name = "ScopedName", namespace = Namespaces.GCO)
    public static final class ScopedName extends CodeType {
    }
}
