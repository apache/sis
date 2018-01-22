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

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.opengis.util.TypeName;
import org.opengis.util.LocalName;
import org.opengis.util.ScopedName;
import org.opengis.util.MemberName;
import org.opengis.util.GenericName;
import org.apache.sis.util.iso.DefaultLocalName;
import org.apache.sis.util.iso.DefaultTypeName;
import org.apache.sis.util.iso.DefaultMemberName;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.jaxb.FilterByVersion;


/**
 * JAXB wrapper in order to map implementing class with the GeoAPI interface.
 * This adapter is used for all the following mutually exclusive properties
 * (only one can be defined at time):
 *
 * <ul>
 *   <li>{@code LocalName}</li>
 *   <li>{@code ScopedName}</li>
 *   <li>{@code TypeName}</li>
 *   <li>{@code MemberName}</li>
 * </ul>
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 */
abstract class NameAdapter<ValueType extends NameAdapter<ValueType,BoundType>, BoundType extends GenericName>
        extends XmlAdapter<ValueType, BoundType>
{
    /**
     * The generic name to be marshalled.
     */
    GenericName name;

    /**
     * Empty constructor for subclasses only.
     */
    NameAdapter() {
    }

    /**
     * Ensures that the {@linkplain #name} is not already defined.
     *
     * @throws IllegalStateException if a name is already defined.
     */
    private void ensureUndefined() throws IllegalStateException {
        if (name != null) {
            throw new IllegalStateException(Errors.format(Errors.Keys.ValueAlreadyDefined_1, "name"));
        }
    }

    /**
     * Returns the {@code LocalName} or {@code ScopedName} to marshal. Returns {@code null} if the name
     * is a {@link TypeName} or a {@link MemberName}, in order to use {@link #getName()} instead.
     * Example:
     *
     * {@preformat xml
     *   <gml:alias>
     *     <gco:LocalName codeSpace=\"A code space\">A name in a scope</gco:LocalName>
     *   </gml:alias>
     * }
     *
     * @return the code for the current name, or {@code null} if none.
     */
    @XmlElementRef
    public final NameValue getValue() {
        final GenericName name = this.name;
        final NameValue code;
        if (name instanceof LocalName) {
            if (name instanceof TypeName || name instanceof MemberName) {
                return null;
            } else if (FilterByVersion.LEGACY_METADATA.accept()) {
                code = new NameValue.Local();
            } else {
                // ISO 19115-3:2016 does not seem to define gco:LocalName anymore.
                code = new NameValue.Scoped();
            }
        } else if (name instanceof ScopedName) {
            code = new NameValue.Scoped();
        } else {
            return null;
        }
        code.setName(name);
        return code;
    }

    /**
     * Returns the {@code TypeName} or {@code MemberName} to marshal. Returns {@code null} if the name
     * is a {@link LocalName} or {@link ScopedName}, in order to use {@link #getValue()} instead.
     * Example:
     *
     * {@preformat xml
     *   <gml:alias>
     *     <gco:TypeName>
     *       <gco:aName>
     *         <gco:CharacterString>An other local name</gco:CharacterString>
     *       </gco:aName>
     *     </gco:TypeName>
     *   </gml:alias>
     * }
     *
     * @return the current name, or {@code null} if none.
     */
    @XmlElementRef
    public final DefaultLocalName getName() {
        final GenericName name = this.name;
        if (name instanceof TypeName) {
            return DefaultTypeName.castOrCopy((TypeName) name);
        } else if (name instanceof MemberName) {
            return DefaultMemberName.castOrCopy((MemberName) name);
        } else {
            return null;
        }
    }

    /**
     * Sets the value for the {@code LocalName} or {@code ScopedName}.
     * This method is called at unmarshalling-time by JAXB.
     *
     * @param  code  the new name.
     * @throws IllegalStateException if a name is already defined.
     */
    public final void setValue(final NameValue code) throws IllegalStateException {
        ensureUndefined();
        if (code != null) {
            name = code.getName();
        }
    }

    /**
     * Sets the value from the {@code TypeName} or {@code MemberName}.
     * This method is called at unmarshalling-time by JAXB.
     *
     * @param  value  the new name.
     * @throws IllegalStateException if a name is already defined.
     */
    public final void setName(final DefaultLocalName value) throws IllegalStateException {
        ensureUndefined();
        name = value;
    }
}
