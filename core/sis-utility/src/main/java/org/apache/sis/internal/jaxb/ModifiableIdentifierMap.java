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
import java.util.Iterator;
import java.util.Collection;
import java.net.URISyntaxException;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.xml.ValueConverter;
import org.apache.sis.xml.XLink;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * A map of identifiers which support {@code put} and {@code remove} operations.
 *
 * <div class="section">Thread safety</div>
 * This class is thread safe if the underlying identifier collection is thread safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 *
 * @see org.apache.sis.xml.IdentifiedObject
 */
public final class ModifiableIdentifierMap extends IdentifierMapAdapter {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -80325787192055778L;

    /**
     * Creates a new map which will be a view over the given identifiers.
     *
     * @param identifiers The identifiers to wrap in a map view.
     */
    public ModifiableIdentifierMap(final Collection<Identifier> identifiers) {
        super(identifiers);
    }

    /**
     * Sets the {@code xlink:href} value, which may be null. If an explicit {@code xlink:href} identifier exists,
     * then it will removed before to set the new {@code href} in the {@link XLink} object. The intend is to give
     * precedence to the {@link XLink#getHRef()} property in every cases where the {@code href} is parsable as a
     * {@link URI}, and use the value associated to the {@code HREF} key only as a fallback when the string can not
     * be parsed.
     *
     * @param  href The new value, or {@code null} for removing the value.
     * @return The previous value, or {@code null} if none.
     *
     * @see #getHRef()
     */
    private URI setHRef(final URI href) {
        URI old = store(IdentifierSpace.HREF, null);
        final Identifier identifier = getIdentifier(IdentifierSpace.XLINK);
        if (identifier instanceof SpecializedIdentifier<?>) {
            final Object link = ((SpecializedIdentifier<?>) identifier).value;
            if (link instanceof XLink) {
                if (old == null) {
                    old = ((XLink) link).getHRef();
                }
                ((XLink) link).setHRef(href);
                return old;
            }
        }
        if (href != null) {
            final XLink link = new XLink();
            link.setHRef(href);
            store(IdentifierSpace.XLINK, link);
        }
        return old;
    }




    ////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                        ////////
    ////////    END OF SPECIAL CASES.                                               ////////
    ////////                                                                        ////////
    ////////    Implementation of IdentifierMap methods follow. Each method may     ////////
    ////////    have a switch statement over the special cases declared above.      ////////
    ////////                                                                        ////////
    ////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns {@code true} since this map support {@code put} and {@code remove} operations.
     */
    @Override
    final boolean isModifiable() {
        return true;
    }

    /**
     * Removes every entries in the underlying collection.
     */
    @Override
    public void clear() {
        identifiers.clear();
    }

    /**
     * Removes all identifiers associated with the given {@linkplain Identifier#getAuthority() authority}.
     * The default implementation delegates to {@link #put(Citation, String)} with a {@code null} value.
     *
     * @param  authority The authority to search, which should be an instance of {@link Citation}.
     * @return The code of the identifier for the given authority, or {@code null} if none.
     */
    @Override
    public String remove(final Object authority) {
        return (authority instanceof Citation) ? put((Citation) authority, null) : null;
    }

    /**
     * Sets the code of the identifier having the given authority to the given value.
     * If no identifier is found for the given authority, a new one is created.
     * If more than one identifier is found for the given authority, then all previous identifiers may be removed
     * in order to ensure that the new entry will be the first entry, so it can be find by the {@code get} method.
     *
     * <p>If the given {@code authority} is {@code HREF} and if the given string is parsable as a {@link URI},
     * then this method will actually store the value as the {@link XLink#getHRef()} property of the {@code XLink}
     * associated to the {@code XLINK} key. Only if the given string can not be parsed, then the value is stored
     * <cite>as-is</cite> under the {@code HREF} key.</p>
     *
     * @param  authority The authority for which to set the code.
     * @param  code The new code for the given authority, or {@code null} for removing the entry.
     * @return The previous code for the given authority, or {@code null} if none.
     */
    @Override
    public String put(final Citation authority, final String code) {
        ArgumentChecks.ensureNonNull("authority", authority);
        String previous  = null;
        Object discarded = null;
        switch (specialCase(authority)) {
            case NonMarshalledAuthority.HREF: {
                URI uri = null;
                if (code != null) {
                    final Context context = Context.current();
                    final ValueConverter converter = Context.converter(context);
                    try {
                        uri = converter.toURI(context, code);
                    } catch (URISyntaxException e) {
                        SpecializedIdentifier.parseFailure(context, code, URI.class, e);
                        discarded = setHRef(null);
                        break;  // Fallback on generic code below.
                    }
                }
                final Identifier identifier = getIdentifier(authority);
                uri = setHRef(uri);
                if (uri != null) {
                    previous = uri.toString();
                } else if (identifier != null) {
                    previous = identifier.getCode();
                }
                return previous;
            }
            // A future Apache SIS version may add more special cases here.
        }
        /*
         * Generic code to be executed when the given authority is not one of the special case,
         * or when it was a special case but parsing of the given string failed.
         */
        final Iterator<? extends Identifier> it = identifiers.iterator();
        while (it.hasNext()) {
            final Identifier identifier = it.next();
            if (identifier == null) {
                it.remove(); // Opportunist cleaning, but should not happen.
            } else if (Objects.equals(authority, identifier.getAuthority())) {
                if (code != null && identifier instanceof IdentifierMapEntry) {
                    return ((IdentifierMapEntry) identifier).setValue(code);
                    // No need to suppress other occurrences of the key (if any)
                    // because we made a replacement in the first entry, so the
                    // new value will be visible by the getter methods.
                }
                if (previous == null) {
                    previous = identifier.getCode();
                }
                it.remove();
                // Continue the iteration in order to remove all other occurrences,
                // in order to ensure that the getter methods will see the new value.
            }
        }
        if (code != null) {
            identifiers.add(SpecializedIdentifier.parse(authority, code));
        }
        if (previous == null && discarded != null) {
            previous = discarded.toString();
        }
        return previous;
    }

    /**
     * Sets the identifier associated with the given authority, and returns the previous value.
     *
     * <p>If the given {@code authority} is {@code HREF}, then this method will actually store the value
     * as the {@link XLink#getHRef()} property of the {@code XLink} associated to the {@code XLINK} key.
     * The previous {@code HREF} value, if any, is discarded.</p>
     *
     * @param  <T> The identifier type.
     * @param  authority The namespace with which the given identifier is to be associated.
     * @param  value The identifier to be associated with the given namespace.
     * @return The previous identifier associated with {@code authority}, or {@code null}
     *         if there was no mapping of the specialized type for {@code authority}.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T putSpecialized(final IdentifierSpace<T> authority, final T value) {
        switch (specialCase(authority)) {
            default: return store(authority, value);
            case NonMarshalledAuthority.HREF: return (T) setHRef((URI) value);
            // A future Apache SIS version may add more special cases here.
        }
    }

    /**
     * Sets the identifier associated with the given authority, without processing for special cases.
     *
     * @param  <T> The identifier type.
     * @param  authority The namespace with which the given identifier is to be associated.
     * @param  value The identifier to be associated with the given namespace.
     * @return The previous identifier associated with {@code authority}, or {@code null}
     *         if there was no mapping of the specialized type for {@code authority}.
     */
    private <T> T store(final IdentifierSpace<T> authority, final T value) {
        ArgumentChecks.ensureNonNull("authority", authority);
        T old = null;
        final Iterator<? extends Identifier> it = identifiers.iterator();
        while (it.hasNext()) {
            final Identifier identifier = it.next();
            if (identifier == null) {
                it.remove(); // Opportunist cleaning, but should not happen.
            } else if (Objects.equals(authority, identifier.getAuthority())) {
                if (identifier instanceof SpecializedIdentifier<?>) {
                    @SuppressWarnings("unchecked")
                    final SpecializedIdentifier<T> id = (SpecializedIdentifier<T>) identifier;
                    if (old == null) {
                        old = id.value;
                    }
                    if (value != null) {
                        id.value = value;
                        return old;
                        // No need to suppress other occurrences of the key (if any)
                        // because we made a replacement in the first entry, so the
                        // new value will be visible by the getter methods.
                    }
                }
                it.remove();
                // Continue the iteration in order to remove all other occurrences,
                // in order to ensure that the getter methods will see the new value.
            }
        }
        if (value != null) {
            identifiers.add(new SpecializedIdentifier<T>(authority, value));
        }
        return old;
    }
}
