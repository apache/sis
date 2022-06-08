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

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutionException;
import org.apache.sis.storage.Resource;


/**
 * An event which, when occurring on a parent resource, is also fired by all children resources.
 * For example when an {@link org.apache.sis.storage.Aggregate} (typically a data store) is closed,
 * a {@link CloseEvent} is automatically fired by all resources that are components of the aggregate.
 * This is similar to "cascade delete" in SQL databases.
 *
 * <h2>Difference between {@code StoreEvent} and {@code CascadedStoreEvent}</h2>
 * By default {@link StoreEvent}s are propagated from children to parents.
 * For example when a {@link WarningEvent} occurs in a child resource,
 * all listeners registered on that resource are notified,
 * then all listeners registered on the parent resource, and so forth until the root resource.
 * All those listeners receive the same {@link WarningEvent} instance,
 * i.e. the {@linkplain WarningEvent#getSource() event source} is always the resource where the warning occurred.
 *
 * <p>By contrast {@code CascadedStoreEvent} are fired in the opposite direction, from parent to children.
 * Furthermore each child creates its own {@code CascadedStoreEvent}. For example if a {@link CloseEvent} is
 * fired in a {@link org.apache.sis.storage.DataStore}, then it causes all resources of that data store to fire
 * their own {@link CloseEvent} declaring themselves as the {@linkplain CloseEvent#getSource() event source}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since 1.3
 *
 * @param <E> the type of the event subclass.
 *
 * @version 1.3
 * @module
 */
public abstract class CascadedStoreEvent<E extends CascadedStoreEvent<E>> extends StoreEvent {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1319167650150261418L;

    /**
     * Constructs an event that occurred in the given resource.
     *
     * @param  source  the resource where the event occurred.
     * @throws IllegalArgumentException if the given source is null.
     */
    protected CascadedStoreEvent(Resource source) {
        super(source);
    }

    /**
     * Creates a new event of the same type than this event but with a different source.
     * This method is invoked for creating the event to be fired by the children of the
     * resource where the original event occurred.
     *
     * @param  child  the child resource for which to create the event to cascade.
     * @return an event of the same type than this event but with the given resource.
     */
    protected abstract E forSource(Resource child);




    /**
     * A listener to register on the parent of a resource for cascading an event to the children.
     *
     * @see StoreListeners#cascadedListeners
     */
    static final class ParentListener<E extends CascadedStoreEvent<E>> implements StoreListener<E> {
        /**
         * The type of event to listen.
         */
        private final Class<E> eventType;

        /**
         * The parent resource to listen to.
         */
        private final StoreListeners parent;

        /**
         * The listeners to notify.
         */
        private final WeakReference<StoreListeners> listeners;

        /**
         * Creates a new listener to be registered on the parent of the given set of listeners.
         *
         * @param  eventType  the type of event to listen.
         * @param  parent     the parent resource to listen to.
         * @param  listeners  the child set of listeners.
         */
        ParentListener(final Class<E> eventType, final StoreListeners parent, final StoreListeners listeners) {
            this.eventType = eventType;
            this.parent    = parent;
            this.listeners = new WeakReference<>(listeners);
        }

        /**
         * Invoked when an event is fired on a parent resource.
         * This method causes similar event to be fired on children resources.
         */
        @Override public void eventOccured(final E event) {
            final StoreListeners r = listeners.get();
            if (r == null) {
                parent.removeListener(eventType, this);
            } else try {
                final E cascade = event.forSource(r.getSource());
                cascade.consume(true);          // For avoiding never-ending loop.
                StoreListeners.fire(r, eventType, cascade);
            } catch (ExecutionException e) {
                StoreListeners.canNotNotify("fire (cascade)", e);
            }
        }
    }
}
