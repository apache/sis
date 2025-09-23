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
import org.opengis.metadata.citation.Citation;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Deprecable;
import org.apache.sis.util.internal.shared.Constants;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.referencing.ReferenceIdentifier;


/**
 * An implementation of {@link ReferenceIdentifier} as a wrapper around a {@link Citation}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public class SimpleIdentifier implements ReferenceIdentifier, Deprecable, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -3544709943777129514L;

    /**
     * Organization or party responsible for definition and maintenance of the
     * {@linkplain #code}, or {@code null} if none. It can be a bibliographical
     * reference to an international standard such as ISO 19115.
     *
     * @see #getAuthority()
     * @see #getCodeSpace()
     * @see #getVersion()
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    protected final Citation authority;

    /**
     * Alphanumeric value identifying an instance in the namespace.
     * It can be for example the name of a class defined by the international standard
     * referenced by the {@linkplain #authority} citation.
     *
     * @see #getCode()
     */
    protected final String code;

    /**
     * {@code true} if this identifier is deprecated.
     */
    protected final boolean isDeprecated;

    /**
     * Creates a new reference identifier.
     *
     * @param authority     responsible party for definition and maintenance of the code, or null.
     * @param code          alphanumeric value identifying an instance in the namespace.
     * @param isDeprecated  {@code true} if this identifier is deprecated.
     */
    public SimpleIdentifier(final Citation authority, final String code, final boolean isDeprecated) {
        this.authority    = authority;
        this.code         = code;
        this.isDeprecated = isDeprecated;
    }

    /**
     * Returns the organization or party responsible for definition and maintenance
     * of the {@linkplain #getCode() code}, or {@code null} if none. It can be a
     * bibliographical reference to an international standard such as ISO 19115.
     *
     * <p>The default implementation returns the citation specified at construction time.</p>
     *
     * @return the authority given at construction time, or {@code null} if none.
     */
    @Override
    public Citation getAuthority() {
        return authority;
    }

    /**
     * Returns the identifier or namespace in which the code is valid, or {@code null} if none.
     * The default implementation returns the shortest identifier of the {@linkplain #getAuthority() authority},
     * if any.
     *
     * @return a code space inferred from the authority given at construction time, or {@code null} if none.
     */
    @Override
    public String getCodeSpace() {
        return org.apache.sis.metadata.iso.citation.Citations.toCodeSpace(authority);
    }

    /**
     * Returns the alphanumeric value identifying an instance in the namespace.
     * It can be for example the name of a class defined by the international standard
     * referenced by the {@linkplain #getAuthority() authority} citation.
     *
     * <p>The default implementation returns the code specified at construction time.</p>
     *
     * @return the code given at construction time, or {@code null} if none.
     */
    @Override
    public String getCode() {
        return code;
    }

    /**
     * Version identifier for the namespace, as specified by the code authority.
     * When appropriate, the edition is identified by the effective date, coded
     * using ISO 8601 date format.
     *
     * @return a version inferred from the authority given at construction time, or {@code null} if none.
     */
    @Override
    public String getVersion() {
        if (authority != null) {
            final InternationalString version = authority.getEdition();
            if (version != null) {
                return version.toString();
            }
        }
        return null;
    }

    /**
     * An optional free text.
     */
    @Override
    public InternationalString getRemarks() {
        return null;
    }

    /**
     * {@code true} if this identifier is deprecated.
     */
    @Override
    public boolean isDeprecated() {
        return isDeprecated;
    }

    /**
     * Returns {@code true} if the given object is of the same class as this
     * {@code SimpleIdentifier} and has the same values.
     *
     * @param  obj  the object to compare with this {@code SimpleIdentifier} for equality.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj != null && obj.getClass() == getClass()) {
            final SimpleIdentifier that = (SimpleIdentifier) obj;
            return Objects.equals(code, that.code) &&
                   Objects.equals(authority, that.authority) &&
                   isDeprecated == that.isDeprecated;
        }
        return false;
    }

    /**
     * Returns a hash code value for this identifier.
     *
     * @return a hash code value for this identifier.
     */
    @Override
    public int hashCode() {
        return Objects.hash(authority, code, isDeprecated) ^ (int) serialVersionUID;
    }

    /**
     * Returns a string representation of this identifier.
     *
     * <p>For customizing this string representation, see {@link #appendStringTo(StringBuilder)}.</p>
     */
    @Override
    public final String toString() {
        final String classname = Classes.getShortClassName(this);
        final StringBuilder buffer = new StringBuilder(classname.length() + CharSequences.length(code) + 10);
        buffer.append(classname).append('[');
        final String codespace = getCodeSpace();                // Subclasses may have overridden this method.
        boolean open = false;
        if (codespace != null) {
            buffer.append('“').append(codespace);
            open = true;
        }
        if (code != null) {
            buffer.append(open ? Constants.DEFAULT_SEPARATOR : '“').append(code);
            open = true;
        }
        if (open) {
            buffer.append('”');
        }
        appendStringTo(buffer);
        return buffer.append(']').toString();
    }

    /**
     * Invoked by {@link #toString()} in order to allow subclasses to add additional information.
     * This method is invoked just before the final {@code ']'} is appended to the buffer.
     *
     * @param  buffer  a buffer filled with the {@link #toString()} characters, that subclasses can update.
     */
    protected void appendStringTo(final StringBuilder buffer) {
    }
}
