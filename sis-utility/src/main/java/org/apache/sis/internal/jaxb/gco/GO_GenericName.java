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
import org.opengis.util.MemberName;
import org.opengis.util.GenericName;

import org.apache.sis.util.type.AbstractName;
import org.apache.sis.util.type.DefaultTypeName;
import org.apache.sis.util.type.DefaultMemberName;
import org.apache.sis.util.type.DefaultScopedName;
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
public final class GO_GenericName extends XmlAdapter<GO_GenericName, GenericName> {
    /**
     * The generic name to be marshalled.
     */
    private AbstractName name;

    /**
     * Empty constructor for JAXB only.
     */
    public GO_GenericName() {
    }

    /**
     * Wraps a name at marshalling-time.
     */
    private GO_GenericName(final AbstractName name) {
        this.name = name;
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
    public String getLocalName() {
        final Object name = this.name;
        return (name instanceof LocalName) && !(name instanceof TypeName)
                && !(name instanceof MemberName) ? name.toString() : null;
    }

    /**
     * Sets the value for the {@code LocalName}.
     * This method is called at unmarshalling-time by JAXB.
     *
     * @param  name The new name.
     * @throws IllegalStateException If a name is already defined.
     */
    public void setLocalName(final String name) throws IllegalStateException {
        ensureUndefined();
        if (name == null) {
            this.name = null;
        } else {
            /*
             * Following cast should be safe because the getNameFactory() method asked specifically
             * for a DefaultNameFactory instance, which is known to create AbstractName instances.
             */
            this.name = (AbstractName) LocalNameAdapter.getNameFactory().createLocalName(null, name);
        }
    }

    /**
     * Returns the {@code ScopedName} generated from the metadata value.
     * This method is called at marshalling-time by JAXB.
     *
     * @return The current name, or {@code null} if none.
     */
    @XmlElement(name = "ScopedName")
    public DefaultScopedName getScopedName() {
        final Object name = this.name;
        return (name instanceof DefaultScopedName) ? (DefaultScopedName) name : null;
    }

    /**
     * Sets the value for the {@code ScopedName}.
     * This method is called at unmarshalling-time by JAXB.
     *
     * @param  name The new name.
     * @throws IllegalStateException If a name is already defined.
     */
    public void setScopedName(final DefaultScopedName name) throws IllegalStateException {
        ensureUndefined();
        this.name = name;
    }

    /**
     * Returns the {@code TypeName} generated from the metadata value.
     * This method is called at marshalling-time by JAXB.
     *
     * @return The current name, or {@code null} if none.
     */
    @XmlElement(name = "TypeName")
    public DefaultTypeName getTypeName() {
        final Object name = this.name;
        return (name instanceof DefaultTypeName) ? (DefaultTypeName) name : null;
    }

    /**
     * Sets the value for the {@code TypeName}.
     * This method is called at unmarshalling-time by JAXB.
     *
     * @param  name The new name.
     * @throws IllegalStateException If a name is already defined.
     */
    public void setTypeName(final DefaultTypeName name) throws IllegalStateException {
        ensureUndefined();
        this.name = name;
    }

    /**
     * Returns the {@code MemberName} generated from the metadata value.
     * This method is called at marshalling-time by JAXB.
     *
     * @return The current name, or {@code null} if none.
     */
    @XmlElement(name = "MemberName")
    public DefaultMemberName getMemberName() {
        final Object name = this.name;
        return (name instanceof MemberName) ? (DefaultMemberName) name : null;
    }

    /**
     * Sets the value for the {@code MemberName}.
     * This method is called at unmarshalling-time by JAXB.
     *
     * @param  name The new name.
     * @throws IllegalStateException If a name is already defined.
     */
    public void setMemberName(final DefaultMemberName name) throws IllegalStateException {
        ensureUndefined();
        this.name = name;
    }

    /**
     * Does the link between an {@link AbstractName} and the adapter associated.
     * JAXB calls automatically this method at marshalling-time.
     *
     * @param  value The implementing class for this metadata value.
     * @return An wrapper which contains the metadata value.
     */
    @Override
    public GO_GenericName marshal(final GenericName value) {
        if (value == null) {
            return null;
        }
        final AbstractName name;
        if (value instanceof AbstractName) {
            name = (AbstractName) value;
        } else {
            /*
             * Following cast should be safe because the getNameFactory() method asked specifically
             * for a DefaultNameFactory instance, which is known to create AbstractName instances.
             */
            name = (AbstractName) ScopedNameAdapter.wrap(value, LocalNameAdapter.getNameFactory());
        }
        return new GO_GenericName(name);
    }

    /**
     * Does the link between adapters and the way they will be unmarshalled.
     * JAXB calls automatically this method at unmarshalling-time.
     *
     * @param  value The wrapper, or {@code null} if none.
     * @return The implementing class.
     */
    @Override
    public GenericName unmarshal(final GO_GenericName value) {
        return (value != null) ? value.name : null;
    }
}
