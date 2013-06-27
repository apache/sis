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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.opengis.util.TypeName;
import org.opengis.util.LocalName;
import org.opengis.util.ScopedName;
import org.opengis.util.MemberName;
import org.opengis.util.GenericName;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.util.iso.AbstractName;
import org.apache.sis.util.iso.DefaultTypeName;
import org.apache.sis.util.iso.DefaultMemberName;
import org.apache.sis.util.resources.Errors;


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
 * @since   0.3 (derived from geotk-2.5)
 * @version 0.3
 * @module
 */
abstract class NameAdapter<ValueType extends NameAdapter<ValueType,BoundType>, BoundType extends GenericName>
        extends XmlAdapter<ValueType, BoundType>
{
    /**
     * The generic name to be marshalled.
     */
    AbstractName name;

    /**
     * Empty constructor for subclasses only.
     */
    NameAdapter() {
    }

    /**
     * Ensures that the {@linkplain #name} is not already defined.
     *
     * @throws IllegalStateException If a name is already defined.
     */
    private void ensureUndefined() throws IllegalStateException {
        if (name != null) {
            throw new IllegalStateException(Errors.format(Errors.Keys.ValueAlreadyDefined_1, "name"));
        }
    }

    /**
     * Returns the {@code LocalName} generated from the metadata value.
     * The local name is returned only if it is not a {@link TypeName} or a {@link MemberName}
     * (otherwise, the corresponding {@code getXXX()} method needs to be invoked instead.
     *
     * @return The current local name, or {@code null} if none.
     *
     * @see #getTypeName()
     * @see #getMemberName()
     */
    @XmlElement(name = "LocalName")
    public final String getLocalName() {
        final AbstractName name = this.name;
        return (name instanceof LocalName) && !(name instanceof TypeName)
                && !(name instanceof MemberName) ? name.toString() : null;
    }

    /**
     * Sets the value for the {@code LocalName}.
     * This method is called at unmarshalling-time by JAXB.
     *
     * @param  value The new name.
     * @throws IllegalStateException If a name is already defined.
     */
    public final void setLocalName(final String value) throws IllegalStateException {
        ensureUndefined();
        if (value == null) {
            name = null;
        } else {
            /*
             * Following cast should be safe because the SIS_NAMES factory is fixed to a
             * DefaultNameFactory instance, which is known to create AbstractName instances.
             */
            name = (AbstractName) DefaultFactories.SIS_NAMES.createLocalName(null, value);
        }
    }

    /**
     * Returns the {@code ScopedName} generated from the metadata value.
     * This method is called at marshalling-time by JAXB.
     *
     * @return The current name, or {@code null} if none.
     */
    @XmlElement(name = "ScopedName")
    public final String getScopedName() {
        final AbstractName name = this.name;
        return (name instanceof ScopedName) ? name.toString() : null;
    }

    /**
     * Sets the value for the {@code ScopedName}.
     * This method is called at unmarshalling-time by JAXB.
     *
     * @param  value The new name.
     * @throws IllegalStateException If a name is already defined.
     */
    public final void setScopedName(final String value) throws IllegalStateException {
        ensureUndefined();
        if (value == null) {
            name = null;
        } else {
            /*
             * Following cast should be safe because the SIS_NAMES factory is fixed to a
             * DefaultNameFactory instance, which is known to create AbstractName instances.
             */
            name = (AbstractName) DefaultFactories.SIS_NAMES.parseGenericName(null, value);
        }
    }

    /**
     * Returns the {@code TypeName} generated from the metadata value.
     * This method is called at marshalling-time by JAXB.
     *
     * @return The current name, or {@code null} if none.
     */
    @XmlElement(name = "TypeName")
    public final DefaultTypeName getTypeName() {
        final AbstractName name = this.name;
        return (name instanceof DefaultTypeName) ? (DefaultTypeName) name : null;
    }

    /**
     * Sets the value for the {@code TypeName}.
     * This method is called at unmarshalling-time by JAXB.
     *
     * @param  value The new name.
     * @throws IllegalStateException If a name is already defined.
     */
    public final void setTypeName(final DefaultTypeName value) throws IllegalStateException {
        ensureUndefined();
        name = value;
    }

    /**
     * Returns the {@code MemberName} generated from the metadata value.
     * This method is called at marshalling-time by JAXB.
     *
     * @return The current name, or {@code null} if none.
     */
    @XmlElement(name = "MemberName")
    public final DefaultMemberName getMemberName() {
        final AbstractName name = this.name;
        return (name instanceof MemberName) ? (DefaultMemberName) name : null;
    }

    /**
     * Sets the value for the {@code MemberName}.
     * This method is called at unmarshalling-time by JAXB.
     *
     * @param  value The new name.
     * @throws IllegalStateException If a name is already defined.
     */
    public final void setMemberName(final DefaultMemberName value) throws IllegalStateException {
        ensureUndefined();
        name = value;
    }
}
