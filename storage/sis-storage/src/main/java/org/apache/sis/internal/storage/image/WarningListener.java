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
package org.apache.sis.internal.storage.image;

import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.event.IIOWriteWarningListener;
import org.apache.sis.storage.event.StoreListeners;


/**
 * A listener for warnings emitted during read or write operations.
 * This class forwards the warnings to the listeners associated to the data store.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
final class WarningListener implements IIOReadWarningListener, IIOWriteWarningListener {
    /**
     * The set of registered {@link StoreListener}s for the data store.
     */
    private final StoreListeners listeners;

    /**
     * Creates a new image I/O listener.
     */
    WarningListener(final StoreListeners listeners) {
        this.listeners = listeners;
    }

    /**
     * Reports a non-fatal error in decoding.
     *
     * @param source   the reader calling this method.
     * @param message  the warning.
     */
    @Override
    public void warningOccurred(final ImageReader source, final String message) {
        listeners.warning(message);
    }

    /**
     * Reports a non-fatal error in encoding.
     *
     * @param source      the writer calling this method.
     * @param imageIndex  index of the image being written.
     * @param message     the warning.
     */
    @Override
    public void warningOccurred(final ImageWriter source, final int imageIndex, final String message) {
        listeners.warning(message);
    }
}
