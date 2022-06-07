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

import org.apache.sis.storage.Resource;


/**
 * Notifies listeners that a resource or a data store is being closed and should no longer be used.
 * Resources are automatically considered closed when a parent resource or data store is closed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   1.3
 * @version 1.3
 * @module
 */
public class CloseEvent extends StoreEvent {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 9121559491613566295L;

    /**
     * Constructs an event for a resource that has been closed.
     *
     * @param  source  the resource which has been closed.
     * @throws IllegalArgumentException if the given source is null.
     */
    public CloseEvent(final Resource source) {
        super(source);
    }




    /**
     * A listener to register on the parent of a resource for closing the resource
     * automatically if the parent is closed.
     *
     * @see StoreListeners#closeListener
     */
    static final class ParentListener implements StoreListener<CloseEvent> {
        /**
         * The parent resource to listen to.
         */
        private final Resource parent;

        /**
         * The listeners to notify.
         */
        private final StoreListeners listeners;

        /**
         * Creates a new listener to be registered on the parent of the given set of listeners.
         *
         * @param  parent     the parent resource to listen to.
         * @param  listeners  the child set of listeners.
         */
        ParentListener(final Resource parent, final StoreListeners listeners) {
            this.parent    = parent;
            this.listeners = listeners;
        }

        /**
         * Invoked when a parent resource or data store is closed.
         */
        @Override public void eventOccured(final CloseEvent event) {
            if (event.getSource() == parent) {      // Necessary check for avoiding never-ending loop.
                listeners.close();
            }
        }
    }
}
