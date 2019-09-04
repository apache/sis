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
 * Defines an object which listens for events in resources (changes or warnings).
 * The events in resources are described by {@link StoreEvent} instances.
 * {@link Resource} implementations are responsible for instantiating the most specific {@code StoreEvent} subclass
 * for the type of event, for example:
 *
 * <ul>
 *   <li>When a warning occurred.</li>
 *   <li>When the data store content changed (e.g. new feature instance added or removed).</li>
 *   <li>When the data store structure changed (e.g. a column is added in tabular data).</li>
 *   <li>Any other change at implementation choice.</li>
 * </ul>
 *
 * Then, all {@code StoreListener}s that declared an interest in {@code StoreEvent}s of that kind are notified.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 *
 * @param  <T>  the type of events of interest to this listener.
 *
 * @see StoreEvent
 *
 * @since 1.0
 * @module
 */
public interface StoreListener<T extends StoreEvent> {
    /**
     * Invoked <em>after</em> a warning or a change occurred in a resource.
     *
     * @param  event  description of the change or warning that occurred in a resource. Shall not be {@code null}.
     */
    void eventOccured(T event);
}
