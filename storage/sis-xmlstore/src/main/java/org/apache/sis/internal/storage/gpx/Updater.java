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
package org.apache.sis.internal.storage.gpx;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.apache.sis.internal.storage.xml.stream.RewriteOnUpdate;
import org.apache.sis.internal.storage.xml.stream.StaxStreamWriter;
import org.apache.sis.storage.DataStoreException;
import org.opengis.feature.Feature;


/**
 * Updates the content of a GPX file by rewriting it.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 */
final class Updater extends RewriteOnUpdate {
    /**
     * The metadata to write.
     */
    private Metadata metadata;

    /**
     * Creates an updater for the given source of features.
     *
     * @param  source    the set of features to update.
     * @param  location  the main file, or {@code null} if unknown.
     * @throws IOException if an error occurred while determining whether the file is empty.
     */
    Updater(final WritableStore source, final Path location) throws IOException {
        super(source, location);
    }

    /**
     * Returns the stream of features to copy.
     *
     * @return all features contained in the dataset.
     * @throws DataStoreException if an error occurred while fetching the features.
     */
    @Override
    protected Stream<? extends Feature> features() throws DataStoreException {
        metadata = Metadata.castOrCopy(source.getMetadata(), getLocale());
        return super.features();
    }

    /**
     * Creates an initially empty temporary file.
     *
     * @return the temporary file.
     * @throws IOException if an error occurred while creating the temporary file.
     */
    @Override
    protected Path createTemporaryFile() throws IOException {
        return Files.createTempFile(StoreProvider.NAME, ".xml");
    }

    /**
     * Creates a new GPX writer for an output in the specified file.
     *
     * @param  temporary  the temporary stream where to write, or {@code null} for writing directly in the store file.
     * @return the writer where to copy updated features.
     * @throws Exception if an error occurred while creating the writer.
     */
    @Override
    protected StaxStreamWriter createWriter(OutputStream temporary) throws Exception {
        return new Writer((WritableStore) source, metadata, temporary);
    }
}
