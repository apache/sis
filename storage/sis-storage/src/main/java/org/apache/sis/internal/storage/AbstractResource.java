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
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.util.Localized;
import org.apache.sis.util.logging.WarningListeners;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.event.ChangeEvent;
import org.apache.sis.storage.event.ChangeListener;


/**
 * Base implementation of resources contained in data stores. This class provides a {@link #getMetadata()}
 * which extracts information from other methods. Subclasses shall or should override the following methods:
 *
 * <ul>
 *   <li>{@link #getIdentifier()} (mandatory)</li>
 *   <li>{@link #getEnvelope()} (recommended)</li>
 *   <li>{@link #createMetadata(MetadataBuilder)} (optional)</li>
 * </ul>
 *
 * {@section Thread safety}
 * Default methods of this abstract class are thread-safe.
 * Synchronization, when needed, uses {@code this} lock.
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
     * Those metadata are created by {@link #getMetadata()} when first needed.
     */
    private Metadata metadata;

    /**
     * The set of registered warning listeners for the data store, or {@code null} if none.
     */
    private final WarningListeners<DataStore> listeners;

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
     * or with {@code null} listeners if they are unknown.
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
     * Returns the spatiotemporal envelope of this resource. This information is part of API only in some kind of resources
     * like {@link org.apache.sis.storage.FeatureSet}. But the method is provided in this base class for convenience and for
     * allowing {@link #getMetadata()} to use this information if available. The default implementation returns {@code null}.
     *
     * @return the spatiotemporal resource extent, or {@code null} if none.
     * @throws DataStoreException if an error occurred while reading or computing the envelope.
     */
    public Envelope getEnvelope() throws DataStoreException {
        return null;
    }

    /**
     * Returns a description of this resource. This method invokes {@link #createMetadata(MetadataBuilder)}
     * the first time it is invoked, then cache the result.
     *
     * @return information about this resource (never {@code null} in this implementation).
     * @throws DataStoreException if an error occurred while reading or computing the envelope.
     */
    @Override
    public final synchronized Metadata getMetadata() throws DataStoreException {
        if (metadata == null) {
            final MetadataBuilder builder = new MetadataBuilder();
            createMetadata(builder);
            metadata = builder.build(true);
        }
        return metadata;
    }

    /**
     * Invoked the first time that {@link #getMetadata()} is invoked. The default implementation populates
     * metadata based on information provided by {@link #getIdentifier()} and {@link #getEnvelope()}.
     * Subclasses should override if they can provide more information.
     *
     * @param  metadata  the builder where to set metadata properties.
     * @throws DataStoreException if an error occurred while reading metadata from the data store.
     */
    protected void createMetadata(final MetadataBuilder metadata) throws DataStoreException {
        final GenericName name = getIdentifier();
        if (name != null) {
            metadata.addTitle(name.toInternationalString());
        }
        try {
            metadata.addExtent(getEnvelope());
        } catch (TransformException | UnsupportedOperationException e) {
            warning(e);
        }
    }

    /**
     * Invoked when a non-fatal exception occurred.
     *
     * @param  e  the non-fatal exception to report.
     */
    protected final void warning(final Exception e) {
        listeners.warning(null, e);
    }

    /**
     * Clears any cache in this resource, forcing the data to be recomputed when needed again.
     * This method should be invoked if the data in underlying data store changed.
     */
    protected synchronized void clearCache() {
        metadata = null;
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
