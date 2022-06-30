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
package org.apache.sis.storage.event;

import java.util.Locale;
import java.util.EventObject;
import org.apache.sis.util.Localized;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.DataStore;
import org.apache.sis.internal.storage.StoreResource;


/**
 * Parent class of events happening in a data store resource.
 * The event may be a warning or a change in the metadata, content or structure of a resource.
 * Those events are created by {@link Resource} implementations and sent to all registered listeners.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.3
 *
 * @see StoreListener
 *
 * @since 1.0
 * @module
 */
public abstract class StoreEvent extends EventObject implements Localized {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1725093072445990248L;

    /**
     * Whether this event has been consumed.
     * A consumed event is not propagated to other listeners.
     */
    private boolean consumed;

    /**
     * Whether to consume this event after all listeners registered on the {@linkplain #getSource() source}
     * resource but before listeners registered on the parent resource or data store.
     */
    private boolean consumeLater;

    /**
     * Constructs an event that occurred in the given resource.
     *
     * @param  source  the resource where the event occurred.
     * @throws IllegalArgumentException if the given source is null.
     */
    protected StoreEvent(Resource source) {
        super(source);
    }

    /**
     * Returns the resource where the event occurred. It is not necessarily the {@linkplain Resource#addListener
     * resource in which listeners have been registered}; it may be one of the resource children.
     *
     * @return the resource where the event occurred.
     */
    @Override
    public Resource getSource() {
        return (Resource) super.getSource();
    }

    /**
     * Returns the locale associated to this event, or {@code null} if unspecified.
     * That locale may be used for formatting messages related to this event.
     * The event locale is typically inherited from the {@link DataStore} locale.
     *
     * @return the locale associated to this event (typically specified by the data store),
     *         or {@code null} if unknown.
     *
     * @see DataStore#getLocale()
     */
    @Override
    public Locale getLocale() {
        if (source instanceof Localized) {
            final Locale locale = ((Localized) source).getLocale();
            if (locale != null) return locale;
        }
        if (source instanceof StoreResource) {
            final DataStore ds = ((StoreResource) source).getOriginator();
            if (ds != null) {
                return ds.getLocale();
            }
        }
        return null;
    }

    /**
     * Indicates whether this event has been consumed by any listener.
     * A consumed event is not propagated further to other listeners.
     *
     * @return {@code true} if this event has been consumed, {@code false} otherwise.
     *
     * @since 1.3
     */
    public final boolean isConsumed() {
        return consumed;
    }

    /**
     * Returns {@code true} if the event propagation can continue with parent listeners.
     */
    final boolean isConsumedForParent() {
        return consumed |= consumeLater;
    }

    /**
     * Marks this event as consumed. This stops its further propagation to other listeners.
     *
     * @param  later  {@code false} for consuming now, or {@code true} for consuming after all listeners
     *         registered on the {@linkplain #getSource() source} resource but before listeners registered
     *         on the parent resource or data store.
     *
     * @since 1.3
     */
    public void consume(final boolean later) {
        if (later) consumeLater = true;
        else consumed = true;
    }
}
