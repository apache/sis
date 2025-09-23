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
package org.apache.sis.metadata.simple;

import java.util.Objects;
import java.io.Serializable;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.IdentifiedObject;
import org.apache.sis.metadata.internal.shared.Identifiers;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.internal.shared.Constants;
import static org.apache.sis.util.collection.Containers.isNullOrEmpty;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import java.util.Optional;


/**
 * A trivial implementation of {@link IdentifiedObject} containing only a primary name.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see org.apache.sis.referencing.AbstractIdentifiedObject
 */
public class SimpleIdentifiedObject implements IdentifiedObject, LenientComparable, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5297493321524903545L;

    /**
     * The primary name by which this object is identified.
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    protected Identifier name;

    /**
     * Creates an identified object without identifier.
     * This constructor is mainly for JAXB.
     */
    protected SimpleIdentifiedObject() {
    }

    /**
     * Creates an identified object with the same identifier as the given one.
     *
     * @param  object  the identified object to partially copy.
     */
    public SimpleIdentifiedObject(final IdentifiedObject object) {
        name = object.getName();
    }

    /**
     * Creates an identified object with the given identifier.
     *
     * @param  name  the primary name by which this object is identified.
     */
    public SimpleIdentifiedObject(final Identifier name) {
        this.name = name;
    }

    /**
     * Returns the primary name by which this object is identified.
     *
     * @return the identifier given at construction time.
     */
    @Override
    public Identifier getName() {
        return name;
    }

    /**
     * Returns a narrative explanation of the role of this object.
     * The default implementation returns {@link Identifier#getDescription()}.
     *
     * @return a narrative explanation of the role of this object.
     */
    public Optional<InternationalString> getDescription() {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final Identifier name = this.name;
        return Optional.ofNullable((name != null) ? name.getDescription() : null);
    }

    /**
     * Returns a hash code value for this object.
     */
    @Override
    public int hashCode() {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final Identifier name = this.name;
        int code = (int) serialVersionUID;
        if (name != null) {
            code ^= name.hashCode();
        }
        return code;
    }

    /**
     * Compares this object with the given one for equality.
     *
     * @param  object  the object to compare with this reference system.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public final boolean equals(final Object object) {
        return equals(object, ComparisonMode.STRICT);
    }

    /**
     * Compares this object with the given one for equality.
     * This method compares the {@linkplain #name} only in "strict" or "by contract" modes.
     * If name is a critical component of this object, then it shall be compared by the subclass.
     * This behavior is consistent with {@link org.apache.sis.referencing.AbstractIdentifiedObject}.
     *
     * @param  object  the object to compare with this identified object.
     * @param  mode    the strictness level of the comparison.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true;
        }
        if (object instanceof IdentifiedObject) {
            if (mode != ComparisonMode.STRICT || object.getClass() == getClass()) {
                if (mode.isIgnoringMetadata()) {
                    return true;
                }
                final IdentifiedObject that = (IdentifiedObject) object;
                return Objects.equals(getName(), that.getName()) &&
                        isNullOrEmpty(that.getIdentifiers()) &&
                        isNullOrEmpty(that.getAlias()) &&
                        that.getRemarks().isEmpty();
            }
        }
        return false;
    }

    /**
     * Returns a pseudo-WKT representation for debugging purpose.
     */
    @Override
    public String toString() {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final Identifier name = this.name;
        final String code, codespace;
        final Citation authority;
        if (name != null) {
            code      = name.getCode();
            codespace = name.getCodeSpace();
            authority = name.getAuthority();
        } else {
            code      = null;
            codespace = null;
            authority = null;
        }
        final StringBuilder buffer = new StringBuilder("IdentifiedObject[\"");
        if (codespace != null) {
            buffer.append(codespace).append(Constants.DEFAULT_SEPARATOR);
        }
        buffer.append(code).append('"');
        final String identifier = Identifiers.getIdentifier(authority, true);
        if (identifier != null) {
            buffer.append(", Id[\"").append(identifier).append("\"]");   // "Id" should be consistent with WKTKeywords.Id.
        }
        return buffer.append(']').toString();
    }
}
