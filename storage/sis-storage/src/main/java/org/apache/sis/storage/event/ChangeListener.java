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
 * Defines an object which listens for changes in resources.
 * The changes in resources are described by {@link ChangeEvent} instances.
 * {@link Resource} implementations are responsible for instantiating the most specific {@code ChangeEvent} subclass
 * for the type of event, for example:
 *
 * <ul>
 *   <li>When the data store content changed (e.g. new feature instance added or removed).</li>
 *   <li>When the data store structure changed (e.g. a column is added in tabular data).</li>
 *   <li>Any other change at implementation choice.</li>
 * </ul>
 *
 * Then, all {@code ChangeListener}s that declared an interest in {@code ChangeEvent}s of that kind are notified.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 *
 * @param  <T>  the type of events of interest to this listener.
 *
 * @see ChangeEvent
 * @see org.apache.sis.util.logging.WarningListener
 *
 * @since 1.0
 * @module
 */
public interface ChangeListener<T extends ChangeEvent> {
    /**
     * Invoked <em>after</em> a change occurred in a resource.
     *
     * @param  event  resource event, never null.
     */
    void changeOccured(T event);
}
