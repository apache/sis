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
package org.apache.sis.test.mock;

import java.util.Arrays;
import java.util.Set;
import java.util.Collection;
import java.io.Serializable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.internal.jaxb.gco.GO_GenericName;


/**
 * A dummy implementation of {@link IdentifiedObject} with minimal XML (un)marshalling capability.
 * This object can also be its own identifier, with a {@linkplain #getCode() code} defined in the
 * {@code "test"} {@linkplain #getCodeSpace() codespace}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
@SuppressWarnings("serial")
@XmlRootElement(name = "IO_IdentifiedObject")
public strictfp class IdentifiedObjectMock implements IdentifiedObject, ReferenceIdentifier, Serializable {
    /**
     * The object name to be returned by {@link #getCode()}.
     */
    private String code;

    /**
     * The alias to (un)marshal to XML
     */
    @XmlElement
    @XmlJavaTypeAdapter(GO_GenericName.class)
    public GenericName alias;

    /**
     * Returns all properties defined in this object,
     * for the convenience of {@link #equals(Object)} and {@link #hashCode()}.
     *
     * @return The properties to use in hash code computations and in comparisons.
     */
    protected Object[] properties() {
        return new Object[] {code, alias};
    }

    /**
     * Creates an initially empty identified object.
     * This constructor is required by JAXB.
     */
    public IdentifiedObjectMock() {
    }

    /**
     * Creates an identified object of the given name.
     * Callers are free to assign new value to the {@link #alias} field directly.
     *
     * @param code The initial {@link #getCode()} value, or {@code null} if none.
     */
    public IdentifiedObjectMock(final String code) {
        this.code = code;
    }

    /**
     * Creates an identified object of the given alias.
     * Callers are free to assign new value to the {@link #alias} field directly.
     *
     * @param code  The initial {@link #getCode()} value, or {@code null} if none.
     * @param alias The initial {@link #alias} value, or {@code null} if none.
     */
    public IdentifiedObjectMock(final String code, final GenericName alias) {
        this.code  = code;
        this.alias = alias;
    }

    /**
     * Returns the object name, or {@code null} if none.
     *
     * @return The name of this object, or {@code null} if none.
     */
    @Override
    public final ReferenceIdentifier getName() {
        return (code != null) ? this : null;
    }

    /**
     * Returns the code supplied at construction time, or {@code null} if none.
     *
     * @return The object code, or {@code null}.
     */
    @Override
    public final String getCode() {
        return code;
    }

    /**
     * Returns the codespace, which is fixed to {@code "test"}.
     *
     * @return {@code "test"}.
     */
    @Override
    public final String getCodeSpace() {
        return "test";
    }

    /**
     * Returns the namespace version ({@code null} for now).
     *
     * @return The namespace version.
     */
    @Override
    public final String getVersion() {
        return null;
    }

    /**
     * Returns the authority that define the object ({@code null} for now).
     *
     * @return The defining authority.
     */
    @Override
    public final Citation getAuthority() {
        return null;
    }

    /**
     * Returns {@link #alias} in an unmodifiable collection, or an empty collection if the alias is null.
     *
     * @return {@link #alias} singleton or an empty collection.
     */
    @Override
    public final Collection<GenericName> getAlias() {
        return CollectionsExt.singletonOrEmpty(alias);
    }

    /**
     * Returns the identifiers (currently null).
     *
     * @return The identifiers of this object.
     */
    @Override
    public final Set<ReferenceIdentifier> getIdentifiers() {
        return null;
    }

    /**
     * Returns the remarks (currently null).
     *
     * @return The remarks associated to this object.
     */
    @Override
    public final InternationalString getRemarks() {
        return null;
    }

    /**
     * Returns the WKT representation (currently none).
     *
     * @return The WLK representation of this object.
     * @throws UnsupportedOperationException If there is no WKT representation.
     */
    @Override
    public final String toWKT() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a string representation for debugging purpose.
     */
    @Override
    public final String toString() {
        return getClass().getSimpleName() + '[' + code + ']';
    }

    /**
     * Returns a hash code value for this object.
     *
     * @return A hash code value.
     */
    @Override
    public final int hashCode() {
        return Arrays.hashCode(properties());
    }

    /**
     * Compares this object with the given object for equality.
     *
     * @param  object The other object, or {@code null}.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public final boolean equals(final Object object) {
        if (object != null && object.getClass() == getClass()) {
            return Arrays.equals(properties(), ((IdentifiedObjectMock) object).properties());
        }
        return false;
    }
}
