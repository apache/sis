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

import java.util.Set;
import java.util.HashSet;
import java.io.DataOutput;
import java.io.IOException;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageReaderSpi;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.internal.storage.Capability;
import org.apache.sis.internal.storage.StoreMetadata;
import org.apache.sis.internal.storage.PRJDataStore;
import org.apache.sis.internal.storage.io.IOUtilities;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.ProbeResult;


/**
 * The provider of {@link Store} instances.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
@StoreMetadata(formatName    = StoreProvider.NAME,
               fileSuffixes  = {"jpeg", "jpg", "png", "gif", "bmp"},    // Non-exhaustive list.
               capabilities  = {Capability.READ, Capability.WRITE, Capability.CREATE},
               resourceTypes = GridCoverageResource.class)
public final class StoreProvider extends PRJDataStore.Provider {
    /**
     * The format name.
     */
    static final String NAME = "World file";

    /**
     * Creates a new provider.
     */
    public StoreProvider() {
    }

    /**
     * Returns a generic name for this data store, used mostly in warnings or error messages.
     *
     * @return a short name or abbreviation for the data format.
     */
    @Override
    public String getShortName() {
        return NAME;
    }

    /**
     * Returns a {@link Store} implementation associated with this provider.
     * The data store will be writable if {@link java.nio.file.StandardOpenOption#WRITE} is provided,
     * or if the storage is a writable object such as {@link javax.imageio.stream.ImageOutputStream}.
     *
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @return a data store implementation associated with this provider for the given storage.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     */
    @Override
    public DataStore open(final StorageConnector connector) throws DataStoreException {
        final Object storage = connector.getStorage();
        boolean isWritable = (storage instanceof ImageWriter);
        if (!isWritable) {
            if (storage instanceof ImageReader) {
                Object input = ((ImageReader) storage).getInput();
                isWritable = (input instanceof DataOutput);         // Parent of ImageOutputStream.
            } else {
                isWritable = isWritable(connector);
            }
        }
        try {
            if (isWritable) {
                return new WritableStore(this, connector);
            } else {
                return new Store(this, connector, true);
            }
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
    }

    /**
     * Returns the MIME type if the image file is recognized by an Image I/O reader.
     * A {@linkplain ProbeResult#isSupported() supported} status does not guarantee that reading
     * or writing will succeed, only that there appears to be a reasonable chance of success
     * based on a brief inspection of the file header.
     *
     * @return a {@linkplain ProbeResult#isSupported() supported} status with the MIME type
     *         if the given storage seems to be readable as an image.
     * @throws DataStoreException if an I/O error occurred.
     */
    @Override
    public ProbeResult probeContent(final StorageConnector connector) throws DataStoreException {
        final Set<ImageReaderSpi> deferred = new HashSet<>();
        final String suffix = IOUtilities.extension(connector.getStorage());
        ImageReaderSpi provider;
        try {
            provider = FormatFilter.SUFFIX.findProvider(suffix, connector, deferred);
            if (provider == null) {
                provider = FormatFilter.SUFFIX.findProvider(null, connector, deferred);
                if (provider == null) {
                    return ProbeResult.UNSUPPORTED_STORAGE;
                }
            }
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
        final String[] types = provider.getMIMETypes();
        if (types != null && types.length != 0) {
            return new ProbeResult(true, types[0], null);
        }
        return ProbeResult.SUPPORTED;
    }
}
