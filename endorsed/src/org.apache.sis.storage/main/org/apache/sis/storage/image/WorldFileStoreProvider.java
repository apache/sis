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
package org.apache.sis.storage.image;

import java.util.Set;
import java.util.HashSet;
import java.util.logging.Logger;
import java.io.IOException;
import javax.imageio.spi.ImageReaderSpi;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.base.Capability;
import org.apache.sis.storage.base.StoreMetadata;
import org.apache.sis.storage.base.PRJDataStore;
import org.apache.sis.io.stream.IOUtilities;
import org.apache.sis.util.ArraysExt;


/**
 * The provider of {@link WorldFileStore} instances.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@StoreMetadata(formatName    = WorldFileStoreProvider.NAME,
               fileSuffixes  = {"jpeg", "jpg", "png", "gif", "bmp"},    // Non-exhaustive list, intentionally excluding TIFF.
               capabilities  = {Capability.READ, Capability.WRITE, Capability.CREATE},
               resourceTypes = {Aggregate.class, GridCoverageResource.class})
public final class WorldFileStoreProvider extends PRJDataStore.Provider {
    /**
     * The format name.
     */
    static final String NAME = "World file";

    /**
     * Name of image formats that are considered to allow only one image.
     * There is no public Image I/O API giving this information, so we have to use a hard-coded list.
     * All formats not in this list are assumed to allow more than one image.
     *
     * <h4>Case of JPEG</h4>
     * The JPEG image reader implementation in standard JDK seems to count a number of images that can be anything.
     * However, documentation on the web often describes the JPEG format as a container for a single image.
     * It is not clear if we should include JPEG in this list or not.
     */
    private static final String[] SINGLE_IMAGE_FORMATS = {"PNG", "BMP", "WBMP", "JPEG"};

    /**
     * The logger used by image stores.
     *
     * @see #getLogger()
     */
    private static final Logger LOGGER = Logger.getLogger("org.apache.sis.storage.image");

    /**
     * Whether the provider is allowed to create {@link GridCoverageResource} instances
     * instead of {@link Aggregate} instances.
     */
    private final boolean allowSingleton;

    /**
     * Creates a new provider.
     */
    public WorldFileStoreProvider() {
        allowSingleton = true;
    }

    /**
     * Creates a new provider with the given configuration.
     * If {@code allowSingleton} is {@code false}, then this provider will unconditionally create
     * {@link WorldFileStore} instances that implement the {@link Aggregate} interface, regardless
     * if the image format allows many pictures or not.
     *
     * @param allowSingleton  whether the provider is allowed to create {@code GridCoverageResource} instances
     *        instead of {@code Aggregate} instances.
     */
    public WorldFileStoreProvider(final boolean allowSingleton) {
        this.allowSingleton = allowSingleton;
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
     * Returns a {@link WorldFileStore} implementation associated with this provider.
     * The data store will be writable if {@link java.nio.file.StandardOpenOption#WRITE} is provided,
     * or if the storage is a writable object such as {@link javax.imageio.stream.ImageOutputStream}.
     *
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @return a data store implementation associated with this provider for the given storage.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     */
    @Override
    public WorldFileStore open(final StorageConnector connector) throws DataStoreException {
        final WorldFileStore store;
        try (FormatFinder format = new FormatFinder(this, connector)) {
            boolean isSingleton = false;
            if (allowSingleton) {
                final String[] names = format.getFormatName();
                if (names != null) {
                    for (final String name : names) {
                        isSingleton = ArraysExt.containsIgnoreCase(SINGLE_IMAGE_FORMATS, name);
                        if (isSingleton) break;
                    }
                }
            }
            if (format.isWritable) {
                store = isSingleton ? new WritableSingleImageStore(format)
                                    : new MultiImageStore.Writable(format);
            } else {
                store = isSingleton ? new SingleImageStore(format)
                                    : new  MultiImageStore(format);
            }
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
        return store;
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
                if (suffix != null) {
                    provider = FormatFilter.SUFFIX.findProvider(null, connector, deferred);
                }
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

    /**
     * Returns the logger used by image stores.
     */
    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}
