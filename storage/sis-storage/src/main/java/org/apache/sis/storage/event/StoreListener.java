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

import java.util.EventListener;
import org.apache.sis.storage.Resource;


/**
 * An object which listens for events (typically changes or warnings) occurring in a resource
 * or one of its children. The kind of event is defined by the subclass of the {@link StoreEvent}
 * instance given to the {@link #eventOccured(StoreEvent)} method. For example if a warning occurred
 * while reading data from a file, then the event will be an instance of {@link WarningEvent}.
 *
 * <p>{@link Resource} implementations are responsible for instantiating the most specific
 * {@code StoreEvent} subclass for the type of events. Then, all {@code StoreListener}s that
 * {@linkplain Resource#addListener(Class, StoreListener) declared an interest} for
 * {@code StoreEvent}s of that kind are notified, including listeners in parent resources.
 * Each listener is notified only once per event even if the listener is registered twice.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 *
 * @param  <E>  the type of events of interest to this listener.
 *
 * @see StoreEvent
 * @see Resource#addListener(Class, StoreListener)
 *
 * @since 1.0
 * @module
 */
public interface StoreListener<E extends StoreEvent> extends EventListener {
    /**
     * Invoked <em>after</em> a warning or a change occurred in a resource.
     * The {@link StoreEvent#getSource()} method gives the resource where the event occurred.
     * It is not necessarily the {@linkplain Resource#addListener resource in which this
     * listener has been registered}; it may be one of the resource children.
     *
     * @param  event  description of the change or warning that occurred in a resource. Shall not be {@code null}.
     */
    void eventOccured(E event);
}
