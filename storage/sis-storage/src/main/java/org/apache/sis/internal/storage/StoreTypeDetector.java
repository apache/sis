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
package org.apache.sis.internal.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.spi.FileTypeDetector;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.DataStoreException;


/**
 * A {@code java.nio.file} service to be registered for probing content type.
 * The {@link #probeContentType(Path)} will be automatically invoked by
 * {@link java.nio.file.Files#probeContentType(Path)}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public class StoreTypeDetector extends FileTypeDetector {
    /**
     * Constructor for {@link java.util.ServiceLoader}.
     */
    public StoreTypeDetector() {
    }

    /**
     * Probes the given file by delegating to {@link DataStores#probeContentType(Object)}.
     *
     * @param  path the path to the file to probe.
     * @return The content type or {@code null} if the file type is not recognized.
     * @throws IOException if an I/O error occurs while reading the file.
     *
     * @see java.nio.file.Files#probeContentType(Path)
     */
    @Override
    public String probeContentType(final Path path) throws IOException {
        try {
            return DataStores.probeContentType(path);
        } catch (DataStoreException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw new IOException(e);
        }
    }
}
