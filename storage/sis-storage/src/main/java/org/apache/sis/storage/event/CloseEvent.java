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
 * Firing a {@code CloseEvent} on a parent resource (typically a data store)
 * automatically fires a {@code CloseEvent} in all children resources.
 * See {@link CascadedStoreEvent} javadoc for more information.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since 1.3
 *
 * @see StoreListeners#close()
 *
 * @version 1.3
 * @module
 */
public class CloseEvent extends CascadedStoreEvent<CloseEvent> {
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
     * Creates a new event of the same type than this event but with a different source.
     *
     * @param  child  the child resource for which to create the event to cascade.
     * @return an event of the same type than this event but with the given resource.
     */
    @Override
    protected CloseEvent forSource(final Resource child) {
        return new CloseEvent(child);
    }
}
