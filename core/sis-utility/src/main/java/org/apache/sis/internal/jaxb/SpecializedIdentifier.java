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
package org.apache.sis.internal.jaxb;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;
import java.io.Serializable;
import java.util.logging.Level;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.xml.XLink;
import org.apache.sis.xml.IdentifierMap;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.xml.ValueConverter;
import org.apache.sis.util.Debug;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.internal.util.Citations;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * Wraps a {@link XLink}, {@link URI} or {@link UUID} as an identifier in the {@link IdentifierMap}.
 * The {@linkplain #authority} is typically an instance of {@link NonMarshalledAuthority}. The value
 * is an object of a type constrained by the authority.
 *
 * @param  <T> The value type, typically {@link XLink}, {@link UUID} or {@link String}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
public final class SpecializedIdentifier<T> implements Identifier, Cloneable, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1699757455535495848L;

    /**
     * The authority, typically as a {@link NonMarshalledAuthority} instance.
     * Null value is not recommended, but this {@code SpecializedIdentifier}
     * is tolerant to such cases.
     *
     * @see #getAuthority()
     */
    private final IdentifierSpace<T> authority;

    /**
     * The identifier value. The identifier {@linkplain #getCode() code} will be the
     * {@linkplain Object#toString() string representation} of this value, if non-null.
     *
     * <p>This value is set at construction time, but may be modified later by
     * {@link IdentifierMapAdapter#putSpecialized(IdentifierSpace, Object)}.</p>
     *
     * @see #getValue()
     * @see #getCode()
     */
    T value;

    /**
     * Creates a new adapter for the given authority and identifier value.
     *
     * @param authority The identifier authority.
     * @param value The identifier value, or {@code null} if not yet defined.
     */
    public SpecializedIdentifier(final IdentifierSpace<T> authority, final T value) {
        this.authority = authority;
        this.value = value;
    }

    /**
     * Creates an identifier from a text value. This method creates an instance of
     * {@code SpecializedIdentifier} if the given authority is one of the "special"
     * authorities declared in the {@link IdentifierSpace} interface. Otherwise a
     * plain {@link IdentifierMapEntry} is created.
     *
     * @param authority The authority, typically as one of the {@link IdentifierSpace} constants.
     * @param code      The identifier code to parse.
     *
     * @see IdentifierMapAdapter#put(Citation, String)
     */
    static Identifier parse(final Citation authority, final String code) {
        if (authority instanceof NonMarshalledAuthority) {
            final int ordinal = ((NonMarshalledAuthority) authority).ordinal;
            switch (ordinal) {
                case NonMarshalledAuthority.ID: {
                    return new SpecializedIdentifier<String>(IdentifierSpace.ID, code);
                }
                case NonMarshalledAuthority.UUID: {
                    final Context context = Context.current();
                    final ValueConverter converter = Context.converter(context);
                    try {
                        return new SpecializedIdentifier<UUID>(IdentifierSpace.UUID, converter.toUUID(context, code));
                    } catch (IllegalArgumentException e) {
                        parseFailure(context, code, UUID.class, e);
                        break;
                    }
                }
                case NonMarshalledAuthority.HREF:
                case NonMarshalledAuthority.XLINK: {
                    final Context context = Context.current();
                    final ValueConverter converter = Context.converter(context);
                    final URI href;
                    try {
                        href = converter.toURI(context, code);
                    } catch (URISyntaxException e) {
                        parseFailure(context, code, URI.class, e);
                        break;
                    }
                    if (ordinal == NonMarshalledAuthority.HREF) {
                        return new SpecializedIdentifier<URI>(IdentifierSpace.HREF, href);
                    }
                    final XLink xlink = new XLink();
                    xlink.setHRef(href);
                    return new SpecializedIdentifier<XLink>(IdentifierSpace.XLINK, xlink);
                }
            }
        }
        return new IdentifierMapEntry(authority, code);
    }

    /**
     * Invoked by {@link #parse(Citation,String)} when a string can not be parsed.
     * This is considered a non-fatal error, because the parse method can fallback
     * on the generic {@link IdentifierMapEntry} in such cases.
     *
     * <p>This method assumes that {@link IdentifierMap#put(Object, Object)} is
     * the public API by which this method has been invoked.</p>
     *
     * @param context The marshalling context, or {@code null} if none.
     * @param value   The value that we failed to parse.
     * @param type    The target type of the parsing process.
     * @param cause   The exception that occurred during the parsing process.
     */
    static void parseFailure(final Context context, final String value, final Class<?> type, final Exception cause) {
        Context.warningOccured(context, Level.WARNING, IdentifierMap.class, "put", cause,
                Messages.class, Messages.Keys.UnparsableValueStoredAsText_2, type, value);
    }

    /**
     * Returns the authority specified at construction time.
     *
     * @return The identifier authority.
     */
    @Override
    public Citation getAuthority() {
        return authority;
    }

    /**
     * Returns the identifier value. This is the {@linkplain #getCode() code} expressed as
     * an object more specialized than {@link String}.
     *
     * @return The identifier value, or {@code null} if none.
     */
    public T getValue() {
        return value;
    }

    /**
     * Returns a string representation of the {@linkplain #getValue() identifier value},
     * or {@code null} if none.
     *
     * @return The identifier value.
     */
    @Override
    public String getCode() {
        final T value = this.value;
        return (value != null) ? value.toString() : null;
    }

    /**
     * Returns a hash code value for this identifier.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(value) + 31 * Objects.hashCode(authority);
    }

    /**
     * Compares this identifier with the given object for equality.
     *
     * @param other The object to compare with this identifier for equality.
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof SpecializedIdentifier<?>) {
            final SpecializedIdentifier<?> that = (SpecializedIdentifier<?>) other;
            return Objects.equals(authority, that.authority) &&
                   Objects.equals(value, that.value);
        }
        return false;
    }

    /**
     * Returns a clone of this identifier.
     *
     * @return A shallow clone of this identifier.
     */
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);    // Should never happen, since we are cloneable.
        }
    }

    /**
     * Returns a string representation of this identifier.
     * Example: {@code Identifier[gco:uuid=“42924124-032a-4dfe-b06e-113e3cb81cf0”]}.
     *
     * @see IdentifierMapAdapter#toString()
     */
    @Debug
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder(60).append("Identifier[");
        format(buffer, authority, getCode());
        return buffer.append(']').toString();
    }

    /**
     * Formats the given (authority, code) par value in the given buffer.
     */
    static void format(final StringBuilder buffer, final Citation authority, final String code) {
        buffer.append(Citations.getIdentifier(authority, false)).append('=');
        final boolean quote = (code != null) && (code.indexOf('[') < 0);
        if (quote) buffer.append('“');
        buffer.append(code);
        if (quote) buffer.append('”');
    }
}
