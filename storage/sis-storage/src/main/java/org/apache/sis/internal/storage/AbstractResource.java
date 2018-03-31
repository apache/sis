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
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.identification.Identification;
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
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.8
 * @module
 */
public abstract class AbstractResource implements Resource, Localized {
    /**
     * The set of registered warning listeners for the data store.
     */
    protected final WarningListeners<DataStore> listeners;

    /**
     * Creates a new resource.
     *
     * @param listeners  the set of registered warning listeners for the data store.
     */
    protected AbstractResource(final WarningListeners<DataStore> listeners) {
        this.listeners = listeners;
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
     * The default implementation computes the union of all {@link GeographicBoundingBox} in the resource metadata,
     * assuming the {@linkplain org.apache.sis.referencing.CommonCRS#defaultGeographic() default geographic CRS}
     * (usually WGS 84).
     *
     * @return the spatio-temporal resource extent, or {@code null} if none.
     * @throws DataStoreException if an error occurred while reading or computing the envelope.
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
                if (identification != null) {                                               // Paranoiac check.
                    for (final Extent extent : identification.getExtents()) {
                        if (extent != null) {                                               // Paranoiac check.
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
