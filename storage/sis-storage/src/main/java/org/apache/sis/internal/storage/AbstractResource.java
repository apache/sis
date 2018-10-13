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
package org.apache.sis.internal.storage;

import java.util.Locale;
import org.opengis.util.GenericName;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.identification.Identification;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.util.Localized;
import org.apache.sis.util.logging.WarningListeners;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.event.ChangeEvent;
import org.apache.sis.storage.event.ChangeListener;


/**
 * Base implementation of resources contained in data stores.
 * This class provides default implementation of {@link #getIdentifier()} and {@link #getEnvelope()}
 * methods which extract their information from the value returned by {@link #getMetadata()}.
 * Subclasses should override those methods if they can provide those information more efficiently.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.8
 * @module
 */
public abstract class AbstractResource implements Resource, Localized {
    /**
     * The set of registered warning listeners for the data store, or {@code null} if none.
     */
    protected final WarningListeners<DataStore> listeners;

    /**
     * Creates a new resource.
     *
     * @param listeners  the set of registered warning listeners for the data store, or {@code null} if none.
     */
    protected AbstractResource(final WarningListeners<DataStore> listeners) {
        this.listeners = listeners;
    }

    /**
     * Creates a new resource with the same warning listeners than the given resource,
     * or {@code null} if the listeners are unknown.
     *
     * @param resource  the resources from which to get the listeners, or {@code null} if none.
     */
    protected AbstractResource(final Resource resource) {
        listeners = (resource instanceof AbstractResource) ? ((AbstractResource) resource).listeners : null;
    }

    /**
     * Returns the locale for error messages or warnings.
     * Returns {@code null} if no locale is explicitly defined.
     *
     * @return the locale, or {@code null} if not explicitly defined.
     */
    @Override
    public final Locale getLocale() {
        return (listeners != null) ? listeners.getLocale() : null;
    }

    /**
     * Returns the display name of the data store, or {@code null} if none.
     * This is a convenience method for formatting error messages in subclasses.
     *
     * @return the data store display name, or {@code null}.
     *
     * @see DataStore#getDisplayName()
     */
    protected final String getStoreName() {
        return (listeners != null) ? listeners.getSource().getDisplayName() : null;
    }

    /**
     * Returns an identifier for this resource. The default implementation returns the first identifier
     * of {@code Metadata/​identificationInfo/​citation}, provided that exactly one such citation is found.
     * If more than one citation is found, then this method returns {@code null} since the identification
     * is considered ambiguous. This is the same default implementation than {@link DataStore#getIdentifier()}.
     *
     * @return the resource identifier inferred from metadata, or {@code null} if none or ambiguous.
     * @throws DataStoreException if an error occurred while fetching the identifier.
     *
     * @see DataStore#getIdentifier()
     */
    @Override
    public GenericName getIdentifier() throws DataStoreException {
        return identifier(getMetadata());
    }

    /**
     * Implementation of {@link #getIdentifier()}, provided as a separated method for implementations
     * that do not extend {@code AbstractResource}.
     *
     * @param  metadata  the metadata from which to infer the identifier, or {@code null}.
     * @return the resource identifier inferred from metadata, or {@code null} if none or ambiguous.
     *
     * @see StoreUtilities#getAnyIdentifier(Metadata, boolean)
     */
    public static GenericName identifier(final Metadata metadata) {
        if (metadata != null) {
            Citation citation = null;
            for (final Identification id : metadata.getIdentificationInfo()) {
                final Citation c = id.getCitation();
                if (c != null) {
                    if (citation != null && citation != c) return null;                 // Ambiguity.
                    citation = c;
                }
            }
            if (citation != null) {
                Identifier first = null;
                for (final Identifier c : citation.getIdentifiers()) {
                    if (c instanceof GenericName) {
                        return (GenericName) c;
                    } else if (first == null) {
                        first = c;
                    }
                }
                if (first != null) {
                    return new NamedIdentifier(first);
                }
            }
        }
        return null;
    }

    /**
     * Returns the spatio-temporal envelope of this resource.
     * The default implementation computes the union of all {@link GeographicBoundingBox} in the resource metadata,
     * assuming the {@linkplain org.apache.sis.referencing.CommonCRS#defaultGeographic() default geographic CRS}
     * (usually WGS 84).
     *
     * @return the spatio-temporal resource extent, or {@code null} if none.
     * @throws DataStoreException if an error occurred while reading or computing the envelope.
     *
     * @see org.apache.sis.storage.DataSet#getEnvelope()
     */
    public Envelope getEnvelope() throws DataStoreException {
        return envelope(getMetadata());
    }

    /**
     * Implementation of {@link #getEnvelope()}, provided as a separated method for
     * {@link org.apache.sis.storage.DataSet} implementations that do not extend {@code AbstractResource}.
     *
     * @param  metadata  the metadata from which to compute the envelope, or {@code null}.
     * @return the spatio-temporal resource extent, or {@code null} if none.
     */
    public static Envelope envelope(final Metadata metadata) {
        GeneralEnvelope bounds = null;
        if (metadata != null) {
            for (final Identification identification : metadata.getIdentificationInfo()) {
                for (final Extent extent : identification.getExtents()) {
                    for (final GeographicExtent ge : extent.getGeographicElements()) {
                        if (ge instanceof GeographicBoundingBox) {
                            final GeneralEnvelope env = new GeneralEnvelope((GeographicBoundingBox) ge);
                            if (bounds == null) {
                                bounds = env;
                            } else {
                                bounds.add(env);
                            }
                        }
                    }
                }
            }
        }
        return bounds;
    }

    /**
     * Ignored in current implementation, on the assumption that most resources produce no events.
     *
     * @param  <T>        {@inheritDoc}
     * @param  listener   {@inheritDoc}
     * @param  eventType  {@inheritDoc}
     */
    @Override
    public <T extends ChangeEvent> void addListener(ChangeListener<? super T> listener, Class<T> eventType) {
    }

    /**
     * Ignored in current implementation, on the assumption that most resources produce no events.
     *
     * @param  <T>        {@inheritDoc}
     * @param  listener   {@inheritDoc}
     * @param  eventType  {@inheritDoc}
     */
    @Override
    public <T extends ChangeEvent> void removeListener(ChangeListener<? super T> listener, Class<T> eventType) {
    }
}
