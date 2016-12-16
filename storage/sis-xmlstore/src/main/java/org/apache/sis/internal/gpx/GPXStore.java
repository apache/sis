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
package org.apache.sis.internal.gpx;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.apache.sis.internal.xml.StaxDataStore;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Version;
import org.opengis.metadata.Metadata;


/**
 * A data store backed by GPX files.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public class GPXStore extends StaxDataStore {
    /**
     * The "1.0" version.
     */
    static final Version V1_0 = new Version("1.0");

    /**
     * The "1.1" version.
     */
    static final Version V1_1 = new Version("1.1");

    /**
     * The file encoding. Actually used only by the writer; ignored by the reader.
     */
    final Charset encoding;

    /**
     * Creates a new GPX store from the given file, URL or stream object.
     * This constructor invokes {@link StorageConnector#closeAllExcept(Object)},
     * keeping open only the needed resource.
     *
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @throws DataStoreException if an error occurred while opening the GPX file.
     */
    public GPXStore(final StorageConnector connector) throws DataStoreException {
        super(connector);
        ArgumentChecks.ensureNonNull("connector", connector);
        final Charset encoding = connector.getOption(OptionKey.ENCODING);
        this.encoding = (encoding != null) ? encoding : StandardCharsets.UTF_8;
    }

    /**
     * Returns the short name (abbreviation) of the format being read or written.
     *
     * @return {@code "GPX"}.
     */
    @Override
    public String getFormatName() {
        return "GPX";
    }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        return null;    // TODO
    }

    @Override
    public void close() throws DataStoreException {
        // TODO
    }
}
