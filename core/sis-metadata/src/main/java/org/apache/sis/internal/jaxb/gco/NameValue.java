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
 * {@code NameValue}s are used for:
 *
 * <ul>
 *   <li>{@code org.opengis.util.GenericName}</li>
 *   <li>{@link org.opengis.util.LocalName}</li>
 *   <li>{@link org.opengis.util.ScopedName}</li>
 * </ul>
 *
 * {@code NameValue}s are not used for {@code MemberName} and {@code TypeName}.
 * The two later use a quite different XML structure, with an {@code aName} element instead of a XML value.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.5
 * @since   0.5
 * @module
 */
@XmlSeeAlso({
    NameValue.Local.class,
    NameValue.Scoped.class
})
public class NameValue {
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
    @XmlSchemaType(name = "anyURI", namespace = Namespaces.GML)
    String codeSpace;

    /**
     * Invoked by reflection by JAXB on unmarshalling.
     */
    public NameValue() {
    }

    /**
     * Sets the value from the given name.
     *
     * @param name  the name to marshal.
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
     * @return the unmarshalled name.
     */
    public GenericName getName() {
        return Names.parseGenericName(codeSpace, null, value);
    }

    /**
     * The {@code NameValue} as a {@code gco:LocalName}.
     */
    @XmlRootElement(name = "LocalName")
    public static final class Local extends NameValue {
        @Override public GenericName getName() {
            return Names.createLocalName(codeSpace, null, value);
        }
    }

    /**
     * The {@code NameValue} as a {@code gco:ScopedName}.
     */
    @XmlRootElement(name = "ScopedName")
    public static final class Scoped extends NameValue {
    }
}
