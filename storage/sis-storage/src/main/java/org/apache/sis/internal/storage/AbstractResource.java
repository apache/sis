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
import org.opengis.metadata.Metadata;
import org.opengis.geometry.Envelope;
import org.apache.sis.util.Localized;
import org.apache.sis.util.logging.WarningListeners;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.event.ChangeEvent;
import org.apache.sis.storage.event.ChangeListener;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;


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
     * A description of this resource as an unmodifiable metadata, or {@code null} if not yet computed.
     * If non-null, this metadata shall contain at least the resource {@linkplain #getIdentifier() identifier}.
     *
     * <p>Those metadata are created by {@link #getMetadata()} when first needed.
     * Subclasses can set a value to this field directly if they wish to bypass the
     * default metadata creation process.</p>
     */
    protected Metadata metadata;

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
     * Returns the spatio-temporal envelope of this resource.
     * The default implementation returns {@code null}.
     *
     * @return the spatio-temporal resource extent, or {@code null} if none.
     *
     * @throws DataStoreException if an error occurred while reading or computing the envelope.
     */
    public Envelope getEnvelope() throws DataStoreException {
        return null;
    }

    /**
     * Returns a description of this set of features.
     * Current implementation sets only the resource name; this may change in any future Apache SIS version.
     */
    @Override
    public synchronized Metadata getMetadata() throws DataStoreException {
        if (metadata == null) {
            final DefaultMetadata metadata = new DefaultMetadata();
            final GenericName name = getIdentifier();
            if (name != null) {                         // Paranoiac check (should never be null).
                final DefaultCitation citation = new DefaultCitation(name.toInternationalString());
                final DefaultDataIdentification identification = new DefaultDataIdentification();
                identification.setCitation(citation);
            }
            metadata.transition(DefaultMetadata.State.FINAL);
            this.metadata = metadata;
        }
        return metadata;
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
