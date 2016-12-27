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

import java.io.IOException;
import java.net.URISyntaxException;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import org.apache.sis.internal.xml.StaxDataStore;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Version;
import org.opengis.metadata.Metadata;

// Branch-dependent imports
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.io.UncheckedIOException;
import org.opengis.feature.Feature;


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
     * Version of the GPX file, or {@code null} if unknown.
     */
    Version version;

    /**
     * {@code true} if the {@linkplain #metadata} field has been initialized.
     * Note that metadata after initialization may still be {@code null}.
     */
    private boolean initialized;

    /**
     * The metadata, or {@code null} if not yet parsed.
     */
    private Metadata metadata;

    /**
     * If a reader has been created for parsing the {@linkplain #metadata} and has not yet been used
     * for iterating over the features, that reader. Otherwise {@code null}.
     */
    private GPXReader reader;

    /**
     * Creates a new GPX store from the given file, URL or stream object.
     * This constructor invokes {@link StorageConnector#closeAllExcept(Object)},
     * keeping open only the needed resource.
     *
     * @param  provider   the provider of this data store, or {@code null} if unspecified.
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @throws DataStoreException if an error occurred while opening the GPX file.
     */
    public GPXStore(final StoreProvider provider, final StorageConnector connector) throws DataStoreException {
        super(provider, connector);
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

    /**
     * Returns the GPX file version.
     *
     * @return the GPX file version, or {@code null} if none.
     * @throws DataStoreException if an error occurred while reading the metadata.
     */
    public synchronized Version getVersion() throws DataStoreException {
        if (version == null) {
            getMetadata();
        }
        return version;
    }

    /**
     * Sets the version of the file to write.
     *
     * @param  version  the target GPX file format.
     * @throws DataStoreException if an error occurred while setting the format.
     */
    public synchronized void setVersion(final Version version) throws DataStoreException {
        ArgumentChecks.ensureNonNull("version", version);
        this.version = version;
    }

    /**
     * Returns information about the dataset as a whole.
     *
     * @return information about the dataset, or {@code null} if none.
     * @throws DataStoreException if an error occurred while reading the metadata.
     */
    @Override
    public synchronized Metadata getMetadata() throws DataStoreException {
        if (!initialized) try {
            initialized = true;
            reader = new GPXReader(this);
            version = reader.initialize(true);
            metadata = reader.getMetadata();
        } catch (XMLStreamException | IOException | JAXBException e) {
            throw new DataStoreException(e);
        } catch (URISyntaxException | RuntimeException e) {
            throw new DataStoreContentException(e);
        }
        return metadata;
    }

    /**
     * Returns the stream of features.
     *
     * @return a stream over all features in the CSV file.
     * @throws DataStoreException if an error occurred while creating the feature stream.
     */
    @Override
    public synchronized Stream<Feature> getFeatures() throws DataStoreException {
        GPXReader r = reader;
        reader = null;
        if (r == null) try {
            r = new GPXReader(this);
            version = r.initialize(false);
        } catch (XMLStreamException | IOException | JAXBException e) {
            throw new DataStoreException(e);
        } catch (URISyntaxException | RuntimeException e) {
            throw new DataStoreContentException(e);
        }
        final Stream<Feature> features = StreamSupport.stream(r, false);
        return features.onClose(r);
    }

    /**
     * Replaces the content of this GPX file by the given metadata and features.
     *
     * @param  metadata  the metadata to write, or {@code null} if none.
     * @param  features  the features to write, or {@code null} if none.
     * @throws DataStoreException if an error occurred while writing the data.
     *
     * @todo verify that the given stream is not connected to this GPX file.
     */
    public synchronized void write(final Metadata metadata, final Stream<? extends Feature> features)
            throws DataStoreException
    {
        // TODO: convert the metadata if needed.
        try (final GPXWriter writer = new GPXWriter(this, (org.apache.sis.internal.gpx.Metadata) metadata)) {
            writer.writeStartDocument();
            if (features != null) {
                features.forEachOrdered(writer);
            }
            writer.writeEndDocument();
        } catch (BackingStoreException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof DataStoreException) {
                throw (DataStoreException) cause;
            }
            throw new DataStoreException(e.getMessage(), cause);
        } catch (Exception e) {
            if (e instanceof UncheckedIOException) {
                e = ((UncheckedIOException) e).getCause();
            }
            throw new DataStoreException(e);
        }
    }

    /**
     * Closes this data store and releases any underlying resources.
     *
     * @throws DataStoreException if an error occurred while closing this data store.
     */
    @Override
    public synchronized void close() throws DataStoreException {
        final GPXReader r = reader;
        reader = null;
        if (r != null) try {
            r.close();
        } catch (Exception e) {
            final DataStoreException ds = new DataStoreException(e);
            try {
                super.close();
            } catch (DataStoreException s) {
                ds.addSuppressed(s.getCause());
            }
            throw ds;
        }
        super.close();
    }
}
