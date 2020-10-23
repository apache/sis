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
package org.apache.sis.gui.dataset;

import java.nio.file.Path;
import javafx.event.Event;
import javafx.event.EventType;


/**
 * Event sent when a resource is loaded.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 */
public final class LoadEvent extends Event {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5935085957507976585L;

    /**
     * The only valid type for the load event.
     */
    private static final EventType<LoadEvent> LOAD = new EventType<>("LOAD");

    /**
     * Path to the resource being loaded.
     */
    private final Path path;

    /**
     * Creates a new event.
     *
     * @param  source  the source of this event.
     * @param  path    path to the file being loaded.
     */
    LoadEvent(final ResourceTree source, final Path path) {
        super(source, null, LOAD);
        this.path = path;
    }

    /**
     * Returns the path to the resource being loaded.
     *
     * @return path to the resource being loaded.
     */
    public Path getResourcePath() {
        return path;
    }
}
