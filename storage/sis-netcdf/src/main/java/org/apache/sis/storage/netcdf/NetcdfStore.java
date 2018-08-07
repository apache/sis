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
import java.net.URI;
import java.util.List;
import java.util.Collection;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.UnsupportedStorageException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.internal.netcdf.Decoder;
import org.apache.sis.internal.storage.URIDataStore;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.metadata.ModifiableMetadata;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.event.ChangeEvent;
import org.apache.sis.storage.event.ChangeListener;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Version;
import ucar.nc2.constants.CDM;


/**
 * A data store backed by netCDF files.
 * Instances of this data store are created by {@link NetcdfStoreProvider#open(StorageConnector)}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @see NetcdfStoreProvider
 *
 * @since 0.3
 * @module
 */
public class NetcdfStore extends DataStore implements Aggregate {
    /**
     * The object to use for decoding the netCDF file content. There is two different implementations,
     * depending on whether we are using the embedded SIS decoder or a wrapper around the UCAR library.
     */
    private final Decoder decoder;

    /**
     * The {@link NetcdfStoreProvider#LOCATION} parameter value, or {@code null} if none.
     */
    private final URI location;

    /**
     * The object returned by {@link #getMetadata()}, created when first needed and cached.
     */
    private Metadata metadata;

    /**
     * The data (raster or features) found in the netCDF file. This list is created when first needed.
     *
     * @see #components()
     */
    private List<Resource> components;

    /**
     * Creates a new netCDF store from the given file, URL, stream or {@link ucar.nc2.NetcdfFile} object.
     * This constructor invokes {@link StorageConnector#closeAllExcept(Object)}, keeping open only the
     * needed resource.
     *
     * @param  provider   the factory that created this {@code DataStore} instance, or {@code null} if unspecified.
     * @param  connector  information about the storage (URL, stream, {@link ucar.nc2.NetcdfFile} instance, <i>etc</i>).
     * @throws DataStoreException if an error occurred while opening the netCDF file.
     *
     * @since 0.8
     */
    public NetcdfStore(final NetcdfStoreProvider provider, final StorageConnector connector) throws DataStoreException {
        super(provider, connector);
        location = connector.getStorageAs(URI.class);
        try {
            decoder = NetcdfStoreProvider.decoder(listeners, connector);
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
        if (decoder == null) {
            throw new UnsupportedStorageException(super.getLocale(), NetcdfStoreProvider.NAME,
                    connector.getStorage(), connector.getOption(OptionKey.OPEN_OPTIONS));
        }
    }

    /**
     * Returns the parameters used to open this netCDF data store.
     * If non-null, the parameters are described by {@link NetcdfStoreProvider#getOpenParameters()} and contains at
     * least a parameter named {@value org.apache.sis.storage.DataStoreProvider#LOCATION} with a {@link URI} value.
     * This method may return {@code null} if the storage input can not be described by a URI
     * (for example a netCDF file reading directly from a {@link java.nio.channels.ReadableByteChannel}).
     *
     * @return parameters used for opening this data store, or {@code null} if not available.
     *
     * @since 0.8
     */
    @Override
    public ParameterValueGroup getOpenParameters() {
        return URIDataStore.parameters(provider, location);
    }

    /**
     * Returns the version number of the Climate and Forecast (CF) conventions used in the netCDF file.
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
                ((ModifiableMetadata) metadata).transition(ModifiableMetadata.State.FINAL);
            }
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
        return metadata;
    }

    /**
     * Returns the resources (features or coverages) in this netCDF store.
     *
     * @return children resources that are components of this netCDF store.
     * @throws DataStoreException if an error occurred while fetching the components.
     *
     * @since 0.8
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public synchronized Collection<Resource> components() throws DataStoreException {
        if (components == null) try {
            components = UnmodifiableArrayList.wrap(decoder.getDiscreteSampling());
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
        return components;
    }

    /**
     * Ignored in current implementation, since this resource produces no events.
     *
     * @param  <T>        {@inheritDoc}
     * @param  listener   {@inheritDoc}
     * @param  eventType  {@inheritDoc}
     */
    @Override
    public <T extends ChangeEvent> void addListener(ChangeListener<? super T> listener, Class<T> eventType) {
    }

    /**
     * Ignored in current implementation, since this resource produces no events.
     *
     * @param  <T>        {@inheritDoc}
     * @param  listener   {@inheritDoc}
     * @param  eventType  {@inheritDoc}
     */
    @Override
    public <T extends ChangeEvent> void removeListener(ChangeListener<? super T> listener, Class<T> eventType) {
    }

    /**
     * Closes this netCDF store and releases any underlying resources.
     *
     * @throws DataStoreException if an error occurred while closing the netCDF file.
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
     * Returns a string representation of this netCDF store for debugging purpose.
     * The content of the string returned by this method may change in any future SIS version.
     *
     * @return a string representation of this data store for debugging purpose.
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + decoder + ']';
    }
}
