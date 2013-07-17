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
import java.util.Collection;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.xml.ValueConverter;
import org.apache.sis.xml.XLink;

// Related to JDK7
import java.util.Objects;


/**
 * A map of identifiers which handles some identifiers in a special way.
 * The identifiers for the following authorities are handled in a special way:
 *
 * <ul>
 *   <li>{@link IdentifierSpace#HREF}: handled as a shortcut to {@link XLink#getHRef()}.</li>
 * </ul>
 *
 * See usages of {@link #specialCase(Object)} for identifying the code locations where a special
 * handling is applied.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.19)
 * @version 0.3
 * @module
 */
public final class IdentifierMapWithSpecialCases extends IdentifierMapAdapter {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 8135179749011991090L;

    /**
     * Creates a new map which will be a view over the given identifiers.
     *
     * @param identifiers The identifiers to wrap in a map view.
     */
    public IdentifierMapWithSpecialCases(final Collection<Identifier> identifiers) {
        super(identifiers);
    }

    /**
     * If the given authority is a special case, returns its {@link NonMarshalledAuthority}
     * integer enum. Otherwise returns -1. See javadoc for more information about special cases.
     *
     * @param authority A {@link Citation} constant. The type is relaxed to {@code Object}
     *        because the signature of some {@code Map} methods are that way.
     */
    private static int specialCase(final Object authority) {
        if (authority == IdentifierSpace.HREF) return NonMarshalledAuthority.HREF;
        // A future Apache SIS version may add more special cases here.
        return -1;
    }

    /**
     * Extracts the {@code xlink:href} value from the {@link XLink} if presents. This method does
     * not test if an explicit {@code xlink:href} identifier exists - this check must be done by
     * the caller <strong>before</strong> to invoke this method, by invoking one of the getter
     * methods defined in the {@link IdentifierMapAdapter} super-class.
     */
    private String getHRef() {
        final XLink link = super.getSpecialized(IdentifierSpace.XLINK);
        if (link != null) {
            final URI href = link.getHRef();
            if (href != null) {
                return href.toString();
            }
        }
        return null;
    }

    /**
     * Sets the {@code xlink:href} value, which may be null. If an explicit {@code xlink:href}
     * identifier exists, it is removed before to set the new {@code href} in the {@link XLink}
     * object. The intend is to give precedence to the {@link XLink#getHRef()} property in every
     * cases where the {@code href} is parsable as a {@link URI}, and use the value associated
     * to the {@code HREF} key only as a fallback when the string can not be parsed.
     */
    private URI setHRef(final URI href) {
        URI old = super.putSpecialized(IdentifierSpace.HREF, null);
        XLink link = super.getSpecialized(IdentifierSpace.XLINK);
        if (link != null) {
            if (old == null) {
                old = link.getHRef();
            }
            link.setHRef(href);
        } else if (href != null) {
            link = new XLink();
            link.setHRef(href);
            super.putSpecialized(IdentifierSpace.XLINK, link);
        }
        return old;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsValue(final Object code) {
        return super.containsValue(code) || Objects.equals(code, getHRef());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsKey(final Object authority) {
        if (super.containsKey(authority)) {
            return true;
        }
        switch (specialCase(authority)) {
            case NonMarshalledAuthority.HREF: {
                final XLink link = super.getSpecialized(IdentifierSpace.XLINK);
                return (link != null) && (link.getHRef() != null);
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getSpecialized(final IdentifierSpace<T> authority) {
        T value = super.getSpecialized(authority);
        if (value == null) {
            switch (specialCase(authority)) {
                case NonMarshalledAuthority.HREF: {
                    final XLink link = super.getSpecialized(IdentifierSpace.XLINK);
                    if (link != null) {
                        value = (T) link.getHRef();
                    }
                    break;
                }
            }
        }
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String get(final Object authority) {
        String value = super.get(authority);
        if (value == null) {
            switch (specialCase(authority)) {
                case NonMarshalledAuthority.HREF: {
                    value = getHRef();
                    break;
                }
            }
        }
        return value;
    }

    /**
     * {@inheritDoc}
     *
     * <p>If the given {@code authority} is {@code HREF} and if the given string is parsable as a {@link URI},
     * then this method will actually store the value as the {@link XLink#getHRef()} property of the {@code XLink}
     * associated to the {@code XLINK} key. Only if the given string can not be parsed, then the value is stored
     * <cite>as-is</cite> under the {@code HREF} key.</p>
     */
    @Override
    public String put(final Citation authority, final String code)
            throws UnsupportedOperationException
    {
        final Context   context;
        final Object    removed;
        final Class<?>  type;
        final Exception exception;
        switch (specialCase(authority)) {
            default: {
                return super.put(authority, code);
            }
            case NonMarshalledAuthority.HREF: {
                URI uri = null;
                if (code != null) {
                    context = Context.current();
                    final ValueConverter converter = Context.converter(context);
                    try {
                        uri = converter.toURI(context, code);
                    } catch (URISyntaxException e) {
                        exception = e;
                        removed = setHRef(null);
                        type = URI.class;
                        break;
                    }
                }
                final String old = getUnspecialized(authority);
                uri = setHRef(uri);
                return (uri != null) ? uri.toString() : old;
            }
        }
        SpecializedIdentifier.parseFailure(context, this, code, type, exception);
        final String old = super.put(authority, code);
        return (old == null && removed != null) ? removed.toString() : old;
    }

    /**
     * {@inheritDoc}
     *
     * <p>If the given {@code authority} is {@code HREF}, then this method will actually store the value
     * as the {@link XLink#getHRef()} property of the {@code XLink} associated to the {@code XLINK} key.
     * The previous {@code HREF} value, if any, is discarded.</p>
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T putSpecialized(final IdentifierSpace<T> authority, final T value)
            throws UnsupportedOperationException
    {
        switch (specialCase(authority)) {
            default: {
                return super.putSpecialized(authority, value);
            }
            case NonMarshalledAuthority.HREF: {
                return (T) setHRef((URI) value);
            }
        }
    }
}
