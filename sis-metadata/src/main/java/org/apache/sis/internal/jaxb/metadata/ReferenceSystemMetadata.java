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
package org.apache.sis.internal.jaxb.metadata;

import java.util.Set;
import java.util.Collection;
import java.util.Collections;
import java.io.Serializable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.extent.Extent;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.util.iso.DefaultNameSpace;

// Related to JDK7
import java.util.Objects;


/**
 * An implementation of {@link ReferenceSystem} marshalled as specified in ISO 19115.
 * This is different than the {@code ReferenceSystem} implementation provided in the
 * referencing module, since the later marshall the CRS as specified in GML (close
 * to ISO 19111 model).
 *
 * <p>Note that this implementation is very simple and serves no other purpose than being
 * a container for XML parsing or formatting. For real referencing service, consider using
 * {@link org.apache.sis.referencing.AbstractReferenceSystem} subclasses instead.</p>
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 *
 * @see org.apache.sis.referencing.AbstractReferenceSystem
 */
@XmlRootElement(name = "MD_ReferenceSystem")
public class ReferenceSystemMetadata implements ReferenceSystem, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 7008508657011563047L;

    /**
     * The primary name by which this object is identified.
     */
    @XmlElement
    @XmlJavaTypeAdapter(RS_Identifier.class)
    private ReferenceIdentifier referenceSystemIdentifier;

    /**
     * Creates a reference system without identifier.
     * This constructor is mainly for JAXB.
     */
    public ReferenceSystemMetadata() {
    }

    /**
     * Creates a new reference system from the given one.
     *
     * @param crs The reference system to partially copy.
     */
    public ReferenceSystemMetadata(final ReferenceSystem crs) {
        referenceSystemIdentifier = crs.getName();
    }

    /**
     * Creates a new reference system from the given identifier.
     *
     * @param name The primary name by which this object is identified.
     */
    public ReferenceSystemMetadata(final ReferenceIdentifier name) {
        referenceSystemIdentifier = name;
    }

    /**
     * Returns the primary name by which this object is identified.
     */
    @Override
    public ReferenceIdentifier getName() {
        return referenceSystemIdentifier;
    }

    /**
     * Current implementation returns an empty set.
     */
    @Override
    public Set<ReferenceIdentifier> getIdentifiers() {
        return Collections.emptySet();
    }

    /**
     * Current implementation returns an empty set.
     */
    @Override
    public Collection<GenericName> getAlias() {
        return Collections.emptySet();
    }

    /**
     * Current implementation returns {@code null}.
     */
    @Override
    public Extent getDomainOfValidity() {
        return null;
    }

    /**
     * Current implementation returns {@code null}.
     */
    @Override
    public InternationalString getScope() {
        return null;
    }

    /**
     * Current implementation returns {@code null}.
     */
    @Override
    public InternationalString getRemarks() {
        return null;
    }

    /**
     * Returns a hash code value for this object.
     */
    @Override
    public int hashCode() {
        int code = (int) serialVersionUID;
        final ReferenceIdentifier id = referenceSystemIdentifier;
        if (id != null) {
            code ^= id.hashCode();
        }
        return code;
    }

    /**
     * Compares this object with the given one for equality.
     *
     * @param object The object to compare with this reference system.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (object != null && object.getClass() == getClass()) {
            final ReferenceSystemMetadata that = (ReferenceSystemMetadata) object;
            return Objects.equals(referenceSystemIdentifier, that.referenceSystemIdentifier);
        }
        return false;
    }

    /**
     * Throws an exception in all cases, since this object can't be formatted in a valid WKT.
     *
     * @throws UnsupportedOperationException Always thrown.
     */
    @Override
    public String toWKT() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a pseudo-WKT representation.
     */
    @Override
    public String toString() {
        final String code, codespace;
        final Citation authority;
        final ReferenceIdentifier id = referenceSystemIdentifier;
        if (id != null) {
            code      = id.getCode();
            codespace = id.getCodeSpace();
            authority = id.getAuthority();
        } else {
            code      = null;
            codespace = null;
            authority = null;
        }
        return toString("RS", authority, codespace, code, false);
    }

    /**
     * Returns a pseudo-WKT representation.
     *
     * @param  type       The WKT heading text.
     * @param  authority  The authority to write in the {@code "AUTHORITY"} element.
     * @param  codespace  Usually an abbreviation of the authority name.
     * @param  code       The code to write in the {@code "AUTHORITY"} element, or {@code null} if none.
     * @param  deprecated {@code true} if the object to format is deprecated.
     * @return The pseudo-WKT.
     */
    public static String toString(final String type, final Citation authority,
            final String codespace, final String code, final boolean deprecated)
    {
        final StringBuilder buffer = new StringBuilder(type).append("[\"");
        if (codespace != null) {
            buffer.append(codespace).append(DefaultNameSpace.DEFAULT_SEPARATOR);
        }
        buffer.append(code).append('"');
        if (authority != null) {
            buffer.append(", AUTHORITY[\"").append(authority.getTitle()).append("\"]");
        }
        if (deprecated) {
            buffer.append(", DEPRECATED");
        }
        return buffer.append(']').toString();
    }
}
