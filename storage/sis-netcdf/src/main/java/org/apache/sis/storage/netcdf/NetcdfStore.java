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
package org.apache.sis.storage.netcdf;

import java.io.IOException;
import org.opengis.metadata.Metadata;
import org.apache.sis.util.Debug;
import org.apache.sis.util.Classes;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.internal.netcdf.Decoder;
import org.apache.sis.metadata.ModifiableMetadata;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Version;
import ucar.nc2.constants.CDM;


/**
 * A data store backed by NetCDF files.
 * Instances of this data store are created by {@link NetcdfStoreProvider#open(StorageConnector)}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.8
 * @module
 *
 * @see NetcdfStoreProvider
 */
public class NetcdfStore extends DataStore {
    /**
     * The object to use for decoding the NetCDF file content. There is two different implementations,
     * depending on whether we are using the embedded SIS decoder or a wrapper around the UCAR library.
     */
    private final Decoder decoder;

    /**
     * The object returned by {@link #getMetadata()}, created when first needed and cached.
     */
    private Metadata metadata;

    /**
     * Creates a new NetCDF store from the given file, URL, stream or {@link ucar.nc2.NetcdfFile} object.
     * This constructor invokes {@link StorageConnector#closeAllExcept(Object)}, keeping open only the
     * needed resource.
     *
     * @param  connector information about the storage (URL, stream, {@link ucar.nc2.NetcdfFile} instance, <i>etc</i>).
     * @throws DataStoreException if an error occurred while opening the NetCDF file.
     */
    public NetcdfStore(final StorageConnector connector) throws DataStoreException {
        ArgumentChecks.ensureNonNull("connector", connector);
        try {
            decoder = NetcdfStoreProvider.decoder(listeners, connector);
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
        if (decoder == null) {
            throw new DataStoreContentException(Errors.format(Errors.Keys.IllegalInputTypeForReader_2,
                    "NetCDF", Classes.getClass(connector.getStorage())));
        }
    }

    /**
     * Returns information about the dataset as a whole. The returned metadata object can contain information
     * such as the spatiotemporal extent of the dataset, contact information about the creator or distributor,
     * data quality, usage constraints and more.
     *
     * @return information about the dataset.
     * @throws DataStoreException if an error occurred while reading the data.
     */
    @Override
    public synchronized Metadata getMetadata() throws DataStoreException {
        if (metadata == null) try {
            final MetadataReader reader = new MetadataReader(decoder);
            metadata = reader.read();
            if (metadata instanceof ModifiableMetadata) {
                ((ModifiableMetadata) metadata).freeze();
            }
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
        return metadata;
    }

    /**
     * Returns the version number of the Climate and Forecast (CF) conventions used in the NetCDF file.
     * The use of CF convention is mandated by the OGC 11-165r2 standard
     * (<cite>CF-netCDF3 Data Model Extension standard</cite>).
     *
     * @return CF-convention version, or {@code null} if no information about CF convention has been found.
     * @throws DataStoreException if an error occurred while reading the data.
     *
     * @since 0.8
     */
    public synchronized Version getConventionVersion() throws DataStoreException {
        for (final CharSequence value : CharSequences.split(decoder.stringValue(CDM.CONVENTIONS), ',')) {
            if (CharSequences.regionMatches(value, 0, "CF-", true)) {
                return new Version(value.subSequence(3, value.length()).toString());
            }
        }
        return null;
    }

    /**
     * Closes this NetCDF store and releases any underlying resources.
     *
     * @throws DataStoreException if an error occurred while closing the NetCDF file.
     */
    @Override
    public synchronized void close() throws DataStoreException {
        metadata = null;
        try {
            decoder.close();
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
    }

    /**
     * Returns a string representation of this NetCDF store for debugging purpose.
     * The content of the string returned by this method may change in any future SIS version.
     *
     * @return a string representation of this datastore for debugging purpose.
     */
    @Debug
    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + decoder + ']';
    }
}
