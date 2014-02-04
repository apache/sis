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
package org.apache.sis.internal.simple;

import java.io.Serializable;
import org.opengis.util.InternationalString;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.internal.util.Citations;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Debug;

import static org.apache.sis.util.iso.DefaultNameSpace.DEFAULT_SEPARATOR;

// Related to JDK7
import java.util.Objects;


/**
 * An implementation of {@link ReferenceIdentifier} as a wrapper around a {@link Citation}.
 * {@code ReferenceIdentifier} is defined by the ISO 19111 standard and is implemented publicly
 * in the {@link org.apache.sis.referencing} package. This class is provided for codes that do
 * not depend on the {@code sis-referencing} module but still need a lightweight implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
public class SimpleReferenceIdentifier implements ReferenceIdentifier, Serializable {
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
     * Creates a new reference identifier.
     *
     * @param authority Responsible party for definition and maintenance of the code, or null.
     * @param code Alphanumeric value identifying an instance in the namespace.
     */
    public SimpleReferenceIdentifier(final Citation authority, final String code) {
        this.authority = authority;
        this.code = code;
    }

    /**
     * Organization or party responsible for definition and maintenance of the
     * {@linkplain #getCode() code}, or {@code null} if none. It can be a
     * bibliographical reference to an international standard such as ISO 19115.
     *
     * <p>The default implementation returns the citation specified at construction time;</p>
     *
     * @return The authority given at construction time, or {@code null} if none.
     */
    @Override
    public Citation getAuthority() {
        return authority;
    }

    /**
     * Returns the name or identifier of the person or organization responsible for namespace,
     * or {@code null} if none. The default implementation returns the shortest identifier of
     * the {@linkplain #getAuthority() authority}, if any.
     *
     * @return A code space inferred from the authority given at construction time, or {@code null} if none.
     */
    @Override
    public String getCodeSpace() {
        return Citations.getIdentifier(authority);
    }

    /**
     * Returns the alphanumeric value identifying an instance in the namespace.
     * It can be for example the name of a class defined by the international standard
     * referenced by the {@linkplain #getAuthority() authority} citation.
     *
     * <p>The default implementation returns the code specified at construction time;</p>
     *
     * @return The code given at construction time, or {@code null} if none.
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
     * @return A version inferred from the authority given at construction time, or {@code null} if none.
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
     * Returns {@code true} if the given object is of the same class than this
     * {@code SimpleReferenceIdentifier} and has the same values.
     *
     * @param  obj The object to compare with this {@code SimpleReferenceIdentifier} for equality.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj != null && obj.getClass() == getClass()) {
            final SimpleReferenceIdentifier that = (SimpleReferenceIdentifier) obj;
            return Objects.equals(code, that.code) && Objects.equals(authority, that.authority);
        }
        return false;
    }

    /**
     * Returns a hash code value for this identifier.
     *
     * @return A hash code value for this identifier.
     */
    @Override
    public int hashCode() {
        return Objects.hash(authority, code) ^ (int) serialVersionUID;
    }

    /**
     * Returns a string representation of this identifier.
     */
    @Debug
    @Override
    public String toString() {
        final String classname = Classes.getShortClassName(this);
        final StringBuilder buffer = new StringBuilder(classname.length() + CharSequences.length(code) + 10);
        buffer.append(classname).append('[');
        final String codespace = getCodeSpace(); // Subclasses may have overridden this method.
        boolean open = false;
        if (codespace != null) {
            buffer.append('“').append(codespace);
            open = true;
        }
        if (code != null) {
            buffer.append(open ? DEFAULT_SEPARATOR : '“').append(code);
            open = true;
        }
        if (open) {
            buffer.append('”');
        }
        appendToString(buffer);
        return buffer.append(']').toString();
    }

    /**
     * Invoked by {@link #toString()} in order to allow subclasses to add additional information.
     * This method is invoked just before the final {@code ']'} is appended to the buffer.
     *
     * @param buffer A buffer filled with the {@link #toString()} characters,
     *               that subclasses can update.
     */
    protected void appendToString(final StringBuilder buffer) {
    }

    /**
     * Returns a pseudo Well Known Text for this identifier.
     * While this method is not defined in the {@link ReferenceIdentifier} interface, it is often
     * defined in related interfaces like {@link org.opengis.referencing.IdentifiedObject}.
     *
     * @return Pseudo Well Known Text for this identifier.
     */
    public String toWKT() {
        final StringBuilder buffer = new StringBuilder(40).append("ID[");
        append(buffer, Citations.getIdentifier(authority)); // Do not invoke getCodeSpace().
        append(buffer.append(", "), code);
        return buffer.append(']').toString();
    }

    /**
     * Appends the given value in the given buffer between quotes, except if the
     * given value is null in which case {@code null} is appended without quotes.
     */
    private static void append(final StringBuilder buffer, final String value) {
        if (value == null) {
            buffer.append("null");
        } else {
            buffer.append('"').append(value).append('"');
        }
    }
}
