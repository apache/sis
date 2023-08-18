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

import java.io.File;
import java.util.Optional;
import java.nio.file.Path;
import javafx.event.Event;
import javafx.event.EventType;


/**
 * Event sent when a resource is loaded or closed. The {@linkplain #getSource() source}
 * of this event are the {@link ResourceTree} instance on which handlers are registered.
 * The event contains a {@link File} to the resource opened or closed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 *
 * @see ResourceTree#onResourceLoaded
 * @see ResourceTree#onResourceClosed
 *
 * @since 1.2
 */
@SuppressWarnings("CloneableImplementsClone")
public class ResourceEvent extends Event {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 1363812583271647542L;

    /**
     * The type for load events.
     *
     * @see ResourceTree#onResourceLoaded
     */
    static final EventType<ResourceEvent> LOADED = new EventType<>("LOADED");

    /**
     * The type for close events.
     *
     * @see ResourceTree#onResourceClosed
     */
    static final EventType<ResourceEvent> CLOSED = new EventType<>("CLOSED");

    /**
     * Path to the resource being opened or closed, or {@code null} if unknown.
     */
    private final File file;

    /**
     * Creates a new event.
     *
     * @param  source  the source of this event.
     * @param  path    path to the file being opened or closed.
     * @param  type    the type of event.
     */
    ResourceEvent(final ResourceTree source, final Path path, final EventType<ResourceEvent> type) {
        super(source, null, type);
        File f;
        try {
            f = path.toFile();
        } catch (UnsupportedOperationException e) {
            f = null;
        }
        file = f;
    }

    /**
     * Returns the path to the resource being opened or closed.
     *
     * @return path to the resource being opened or closed.
     *
     * @since 1.4
     */
    public Optional<File> getResourceFile() {
        return Optional.ofNullable(file);
    }
}
