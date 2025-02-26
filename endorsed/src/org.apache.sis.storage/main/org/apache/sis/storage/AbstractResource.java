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
package org.apache.sis.storage;

import java.util.Locale;
import java.util.Optional;
import org.opengis.util.GenericName;
import org.opengis.metadata.Metadata;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.CRS;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.storage.internal.Resources;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.xml.NilReason;


/**
 * Default implementations of several methods for classes that want to implement the {@link Resource} interface.
 * This class provides a {@link #getMetadata()} method which extracts information from other methods.
 * Subclasses should override the following methods:
 *
 * <ul>
 *   <li>{@link #getIdentifier()} (strongly recommended)</li>
 *   <li>{@link #getEnvelope()} (recommended)</li>
 *   <li>{@link #createMetadata()} (optional)</li>
 *   <li>{@link #getSynchronizationLock()} (optional)</li>
 * </ul>
 *
 * <h2>Thread safety</h2>
 * Default methods of this abstract class are thread-safe.
 * Synchronization, when needed, uses {@link #getSynchronizationLock()}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.2
 */
public abstract class AbstractResource implements Resource {
    /**
     * The set of registered {@link StoreListener}s for this resources.
     * This {@code StoreListeners} has {@link DataStore#listeners} has a parent.
     */
    protected final StoreListeners listeners;

    /**
     * A description of this resource as an unmodifiable metadata, or {@code null} if not yet computed.
     * This metadata should contain at least the resource {@linkplain #getIdentifier() identifier}.
     * This field is initialized by {@link #getMetadata()} when first needed.
     *
     * @see #getMetadata()
     * @see #createMetadata()
     */
    private volatile Metadata metadata;

    /**
     * Creates a new resource, potentially as a child of another resource.
     * The parent resource is typically, but not necessarily, an {@link Aggregate}.
     *
     * @param  parent  the parent resource, or {@code null} if none.
     *
     * @since 1.4
     */
    @SuppressWarnings("this-escape")
    protected AbstractResource(final Resource parent) {
        StoreListeners parentListeners = null;
        if (parent instanceof AbstractResource) {
            parentListeners = ((AbstractResource) parent).listeners;
        } else if (parent instanceof DataStore) {
            parentListeners = ((DataStore) parent).listeners;
        }
        listeners = new StoreListeners(parentListeners, this);
    }

    /**
     * Creates a new resource which can send notifications to the given set of listeners.
     * If {@code hidden} is {@code false} (the recommended value), then this resource will have its own set of
     * listeners with this resource declared as the {@linkplain StoreListeners#getSource() source of events}.
     * It will be possible to add and remove listeners independently from the set of parent listeners.
     * Conversely if {@code hidden} is {@code true}, then the given listeners will be used directly
     * and this resource will not appear as the source of any event.
     *
     * <p>In any cases, the listeners of all parents (ultimately the data store that created this resource)
     * will always be notified, either directly if {@code hidden} is {@code true}
     * or indirectly if {@code hidden} is {@code false}.</p>
     *
     * @param  parentListeners  listeners of the parent resource, or {@code null} if none.
     *         This is usually the listeners of the {@link DataStore} that created this resource.
     * @param  hidden  {@code false} if this resource shall use its own {@link StoreListeners}
     *         with the specified parent, or {@code true} for using {@code parentListeners} directly.
     */
    @SuppressWarnings("this-escape")
    protected AbstractResource(final StoreListeners parentListeners, final boolean hidden) {
        if (hidden && parentListeners != null) {
            listeners = parentListeners;
        } else {
            listeners = new StoreListeners(parentListeners, this);
        }
    }

    /**
     * Returns the resource persistent identifier if available.
     * The default implementation returns an empty value.
     * Subclasses are strongly encouraged to override if they can provide a value.
     *
     * <h4>Relationship with metadata</h4>
     * The default implementation of {@link #createMetadata()} uses this identifier for initializing
     * the {@code metadata/identificationInfo/citation/title} property.
     *
     * @return a persistent identifier unique within the data store.
     * @throws DataStoreException if an error occurred while fetching the identifier.
     */
    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return Optional.empty();
    }

    /**
     * Returns the spatiotemporal envelope of this resource. This information is part of API only in some kinds
     * of resource such as {@link FeatureSet}. But the method is provided in this base class for convenience.
     * The default implementation gives an empty value.
     *
     * <h4>Implementation note</h4>
     * The default implementation of {@link #createMetadata()} uses this identifier for initializing
     * the {@code metadata/identificationInfo/extent/geographicElement} property.
     *
     * @return the spatiotemporal resource extent.
     * @throws DataStoreException if an error occurred while reading or computing the envelope.
     */
    public Optional<Envelope> getEnvelope() throws DataStoreException {
        return Optional.empty();
    }

    /**
     * Returns a description of this resource.
     * The implementation invokes {@link #createMetadata()}
     * in a synchronized block when first needed, then caches the result.
     *
     * @return information about this resource (never {@code null}).
     * @throws DataStoreException if an error occurred while reading or computing the metadata.
     */
    @Override
    public final Metadata getMetadata() throws DataStoreException {
        Metadata md = metadata;
        if (md == null) {
            synchronized (getSynchronizationLock()) {
                md = metadata;
                if (md == null) {
                    md = createMetadata();
                    if (md == null) {
                        md = NilReason.UNKNOWN.createNilObject(Metadata.class);
                    } else if (md instanceof DefaultMetadata) {
                        ((DefaultMetadata) md).transitionTo(DefaultMetadata.State.FINAL);
                    }
                    metadata = md;
                }
            }
        }
        return md;
    }

    /**
     * Invoked in a synchronized block the first time that {@link #getMetadata()} is invoked.
     * The default implementation populates metadata based on information
     * provided by {@link #getIdentifier()} and {@link #getEnvelope()}.
     * Subclasses should override if they can provide more information.
     * The default value can be completed by casting to {@link DefaultMetadata}.
     *
     * @return the newly created metadata, or {@code null} if unknown.
     * @throws DataStoreException if an error occurred while reading metadata from this resource.
     */
    protected Metadata createMetadata() throws DataStoreException {
        final var builder = new MetadataBuilder();
        builder.addDefaultMetadata(this, listeners);
        return builder.build();
    }

    /**
     * Clears any cache in this resource, forcing the data to be recomputed when needed again.
     * The default implementation clears the cached metadata object, which will cause
     * {@link #createMetadata()} to be invoked again when first needed.
     */
    protected void clearCache() {
        // No need for synchronization because `metadata` is volatile.
        metadata = null;
    }

    /**
     * Returns the object on which to perform synchronizations for thread-safety.
     * The default implementation returns {@code this}.
     * Subclasses can override for example for synchronizing everything on an enclosing {@link DataStore}.
     *
     * @return the synchronization lock.
     */
    protected Object getSynchronizationLock() {
        return this;
    }

    /**
     * Registers a listener to notify when the specified kind of event occurs in this resource or in children.
     * The default implementation forwards to <code>{@linkplain #listeners}.addListener(eventType, listener)</code>.
     *
     * @param  <T>        compile-time value of the {@code eventType} argument.
     * @param  listener   listener to notify about events.
     * @param  eventType  type of {@link StoreEvent}s to listen (cannot be {@code null}).
     */
    @Override
    public <T extends StoreEvent> void addListener(Class<T> eventType, StoreListener<? super T> listener) {
        listeners.addListener(eventType, listener);
    }

    /**
     * Unregisters a listener previously added to this resource for the given type of events.
     * The default implementation forwards to <code>{@linkplain #listeners}.removeListener(eventType, listener)</code>
     *
     * @param  <T>        compile-time value of the {@code eventType} argument.
     * @param  listener   listener to stop notifying about events.
     * @param  eventType  type of {@link StoreEvent}s which were listened (cannot be {@code null}).
     */
    @Override
    public <T extends StoreEvent> void removeListener(Class<T> eventType, StoreListener<? super T> listener) {
        listeners.removeListener(eventType, listener);
    }

    /**
     * Creates an exception message for a resource that cannot be read.
     * If the error is potentially created by a request out of bounds,
     * this method tries to build a message with the problematic coordinates.
     *
     * @param  filename  some identification (typically a file name) of the data that cannot be read.
     * @param  request   the requested domain, or {@code null} if the problem is not a request out of bounds.
     * @return the message to provide in an exception.
     */
    final String createExceptionMessage(final String filename, Envelope request) {
        final Locale locale = listeners.getLocale();
        String message = Errors.forLocale(locale).getString(Errors.Keys.CanNotRead_1, filename);
        if (request != null) try {
            Envelope envelope = getEnvelope().orElse(null);
            if (envelope != null) {
                final CoordinateReferenceSystem crs = CRS.suggestCommonTarget(null,
                        envelope.getCoordinateReferenceSystem(),
                        request.getCoordinateReferenceSystem());
                request  = Envelopes.transform(request,  crs);
                envelope = Envelopes.transform(envelope, crs);
                final int dimension = request.getDimension();
                StringBuilder buffer = null;
                for (int i=0; i<dimension; i++) {
                    final double rmin =  request.getMinimum(i);
                    final double rmax =  request.getMaximum(i);
                    final double vmin = envelope.getMinimum(i);
                    final double vmax = envelope.getMaximum(i);
                    if (rmax < vmin || rmin > vmax) {
                        final String axis;
                        if (crs != null) {
                            axis = IdentifiedObjects.getDisplayName(crs.getCoordinateSystem().getAxis(i), locale);
                        } else if (i < 3) {
                            axis = String.valueOf((char) ('x' + i));
                        } else {
                            axis = "#" + (i+1);
                        }
                        if (buffer == null) {
                            buffer = new StringBuilder(message);
                        }
                        buffer.append(System.lineSeparator()).append(" • ").append(Resources.forLocale(locale)
                                .getString(Resources.Keys.RequestOutOfBounds_5, axis, vmin, vmax, rmin, rmax));
                    }
                }
                if (buffer != null) {
                    message = buffer.toString();
                }
            }
        } catch (DataStoreException | TransformException e) {
            Logging.ignorableException(listeners.getLogger(), AbstractResource.class, "createExceptionMessage", e);
        }
        return message;
    }
}
