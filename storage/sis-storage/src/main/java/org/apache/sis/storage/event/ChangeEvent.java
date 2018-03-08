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

import java.util.EventObject;
import org.apache.sis.storage.Resource;


/**
 * Parent class of all events related to a change in the metadata, content or structure of a resource.
 * Those events are created by {@link Resource} implementations and sent to all registered listeners.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 *
 * @see ChangeListener
 *
 * @since 1.0
 * @module
 */
public abstract class ChangeEvent extends EventObject {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1725093072445990248L;

    /**
     * Constructs an event that occurred in the given resource.
     *
     * @param  source  the resource on which the event initially occurred.
     * @throws IllegalArgumentException  if the given source is null.
     */
    public ChangeEvent(Resource source) {
        super(source);
    }

    /**
     * Returns the resource on which the event initially occurred.
     *
     * @return the resource on which the Event initially occurred.
     */
    @Override
    public Resource getSource() {
        return (Resource) source;
    }
}
