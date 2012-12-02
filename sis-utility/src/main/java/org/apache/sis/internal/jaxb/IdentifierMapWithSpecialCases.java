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
import org.apache.sis.xml.XLink;

// Related to JDK7
import org.apache.sis.internal.util.Objects;


/**
 * A map of identifiers which handles some identifiers in a special way.
 * The identifiers for the following authorities are handled in a special way.
 * See usages of {@link #specialCase(Object)} for spotting the code where
 * a special handling is applied.
 *
 * <ul>
 *   <li>{@link IdentifierSpace#HREF}, handled as a shortcut to {@link XLink#getHRef()}.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.19)
 * @version 0.3
 * @module
 */
final class IdentifierMapWithSpecialCases extends IdentifierMapAdapter {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5139573827448780289L;

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
     * object.
     */
    private URI setHRef(final URI href) {
        super.putSpecialized(IdentifierSpace.HREF, null);
        XLink link = super.getSpecialized(IdentifierSpace.XLINK);
        if (link != null) {
            final URI old = link.getHRef();
            link.setHRef(href);
            return old;
        }
        if (href != null) {
            link = new XLink();
            link.setHRef(href);
            super.putSpecialized(IdentifierSpace.XLINK, link);
        }
        return null;
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
     */
    @Override
    public String put(final Citation authority, final String code) throws UnsupportedOperationException {
        switch (specialCase(authority)) {
            case NonMarshalledAuthority.HREF: {
                try {
                    final URI old = setHRef((code != null) ? new URI(code) : null);
                    return (old != null) ? old.toString() : null;
                } catch (URISyntaxException e) {
                    // Do not log the exception, since it will be
                    // reported by super.put(Citation, String).
                }
                break;
            }
        }
        return super.put(authority, code);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T putSpecialized(final IdentifierSpace<T> authority, final T value) throws UnsupportedOperationException {
        switch (specialCase(authority)) {
            case NonMarshalledAuthority.HREF: {
                return (T) setHRef((URI) value);
            }
        }
        return super.putSpecialized(authority, value);
    }
}
