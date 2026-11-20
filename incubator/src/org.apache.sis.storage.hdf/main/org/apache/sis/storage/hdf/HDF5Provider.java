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
package org.apache.sis.storage.hdf;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.storage.base.Capability;
import org.apache.sis.storage.base.StoreMetadata;
import org.apache.sis.storage.base.URIDataStoreOption;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.hdf.base.SuperBlock;
import org.apache.sis.storage.hdf.internal.Constants;
import org.opengis.parameter.ParameterDescriptorGroup;


/**
 * The provider of {@link HDF5Store} instances.
 *
 * <p>Specification: <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html">HDF5 File Format Specification Version 3.0</a></p>
 *
 * @author Johann Sorel (Geomatys)
 */
@StoreMetadata(formatName    = HDF5Provider.NAME,
               fileSuffixes  = {"h5", "hdf5", "he5", "nc"},
               capabilities  = Capability.READ,
               resourceTypes = Resource.class)
public class HDF5Provider extends DataStoreProvider {
    /**
     * Format name, also used as provider short name and parameter group name.
     */
    static final String NAME = "hdf5";

    /**
     * MIME type of <abbr>HDF5</abbr> files.
     */
    private static final String MIME_TYPE = "application/x-hdf5";

    /**
     * Description of the parameters required for opening an <abbr>HDF5</abbr> store.
     */
    private static final ParameterDescriptorGroup OPEN_DESCRIPTOR = URIDataStoreOption.createForLocationOnly(NAME);

    /**
     * Creates a new provider.
     */
    public HDF5Provider() {
    }

    /**
     * Returns a short name or abbreviation for the data format.
     *
     * @return a short name or abbreviation for the data format.
     */
    @Override
    public String getShortName() {
        return NAME;
    }

    /**
     * Returns a description of all parameters accepted by this provider for opening an <abbr>HDF5</abbr> file.
     *
     * @return description of available parameters for opening an <abbr>HDF5</abbr> file.
     */
    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return OPEN_DESCRIPTOR;
    }

    /**
     * Returns {@link ProbeResult#SUPPORTED} if the given storage appears to be supported by {@link HDF5Store}.
     * The file must be readable as a {@link Path} and start with the <abbr>HDF5</abbr> signature.
     *
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @return whether the given storage is supported by this provider.
     * @throws DataStoreException if an I/O error occurred.
     */
    @Override
    public ProbeResult probeContent(final StorageConnector connector) throws DataStoreException {
        if (connector.getStorageAs(Path.class) == null) {
            return ProbeResult.UNSUPPORTED_STORAGE;
        }
        final ByteBuffer buffer = connector.getStorageAs(ByteBuffer.class);
        if (buffer == null) {
            return ProbeResult.UNSUPPORTED_STORAGE;
        }
        if (buffer.remaining() < SuperBlock.SIGNATURE.length) {
            return ProbeResult.INSUFFICIENT_BYTES;
        }
        final long offset = SuperBlock.findSignatureOffset(buffer);
        if (offset == 0) {
            return new ProbeResult(true, MIME_TYPE, null);
        }
        if (offset > 0) {
            /*
            The file is HDF5 but starts with a user block, which this reader can
            not follow because it treats the stored addresses as absolute.
            */
            Constants.LOGGER.log(Level.FINE, () -> "Found an HDF5 signature at offset " + offset
                    + ", meaning the file has a user block, which is not supported yet.");
        }
        return ProbeResult.UNSUPPORTED_STORAGE;
    }

    /**
     * Returns an {@link HDF5Store} implementation associated with this provider.
     *
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @return a data store implementation associated with this provider for the given storage.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     */
    @Override
    public DataStore open(final StorageConnector connector) throws DataStoreException {
        final Path path = connector.getStorageAs(Path.class);
        try {
            return new HDF5Store(this, path);
        } catch (IllegalArgumentException | IOException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
    }

    /**
     * Returns the logger used by <abbr>HDF5</abbr> stores.
     *
     * @return the logger for <abbr>HDF5</abbr> stores.
     */
    @Override
    public Logger getLogger() {
        return Constants.LOGGER;
    }
}
