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
package org.apache.sis.storage.gpx;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.net.URISyntaxException;
import org.opengis.util.FactoryException;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.distribution.Format;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.base.StoreUtilities;
import org.apache.sis.storage.xml.stream.StaxDataStore;
import org.apache.sis.util.Version;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.util.iso.DefaultNameFactory;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.distribution.DefaultFormat;

// Specific to the main branch:
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.DefaultFeatureType;


/**
 * A data store backed by GPX files.
 * This store does not cache the feature instances.
 * Any new {@linkplain #features(boolean) request for features} will re-read from the file.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public class Store extends StaxDataStore implements FeatureSet {
    /**
     * Version of the GPX file, or {@code null} if unknown.
     */
    Version version;

    /**
     * The metadata, or {@code null} if not yet parsed.
     */
    private Metadata metadata;

    /**
     * If a reader has been created for parsing the {@linkplain #metadata} and has not yet been used
     * for iterating over the features, that reader. Otherwise {@code null}.
     * Used for continuing XML parsing after metadata header instead of closing and reopening the file.
     */
    private Reader reader;

    /**
     * The {@code FeatureType} for routes, tracks, way points, <i>etc</i>.
     * Currently always {@link Types#DEFAULT}, but we use a field for keeping {@code Reader}
     * and {@code Writer} ready to handle profiles or extensions.
     */
    final Types types;

    /**
     * Creates a new GPX store from the given file, URL or stream object.
     * This constructor invokes {@link StorageConnector#closeAllExcept(Object)},
     * keeping open only the needed resource.
     *
     * @param  provider   the provider of this data store, or {@code null} if unspecified.
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @throws DataStoreException if an error occurred while opening the GPX file.
     */
    public Store(final StoreProvider provider, final StorageConnector connector) throws DataStoreException {
        super(provider, connector);
        final GeometryLibrary library = connector.getOption(OptionKey.GEOMETRY_LIBRARY);
        if (library == null || Types.DEFAULT.geometries.library == library) {
            types = Types.DEFAULT;
        } else try {
            types = new Types(DefaultNameFactory.provider(), null, library);
        } catch (FactoryException e) {
            throw new DataStoreException(e);
        }
        listeners.useReadOnlyEvents();
    }

    /**
     * Returns a more complete description of the GPX format.
     * The format will be part of the metadata returned by {@link #getMetadata()}.
     *
     * @see StoreProvider#getFormat()
     * @see org.apache.sis.storage.gpx.Metadata#getResourceFormats()
     */
    final Format getFormat() {
        assert Thread.holdsLock(this);
        Format format = ((StoreProvider) provider).getFormat(listeners);
        if (version != null) {
            final var df = new DefaultFormat(format);
            final var citation = new DefaultCitation(df.getFormatSpecificationCitation());
            citation.setEdition(new SimpleInternationalString(version.toString()));
            df.setFormatSpecificationCitation(citation);
            format = df;
        }
        return format;
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
        this.version = Objects.requireNonNull(version);
    }

    /**
     * Returns information about the dataset as a whole.
     *
     * @return information about the dataset, or {@code null} if none.
     * @throws DataStoreException if an error occurred while reading the metadata.
     */
    @Override
    public synchronized Metadata getMetadata() throws DataStoreException {
        if (metadata == null) try {
            reader   = new Reader(this);
            version  = reader.initialize(true);
            metadata = reader.getMetadata();
        } catch (DataStoreException e) {
            throw e;
        } catch (URISyntaxException | RuntimeException e) {
            throw new DataStoreContentException(e);
        } catch (Exception e) {
            throw new DataStoreException(e);
        }
        return metadata;
    }

    /**
     * Returns the spatiotemporal envelope of this resource.
     *
     * @return the spatiotemporal resource extent.
     * @throws DataStoreException if an error occurred while reading or computing the envelope.
     */
    @Override
    public Optional<Envelope> getEnvelope() throws DataStoreException {
        return Optional.ofNullable(StoreUtilities.getEnvelope(getMetadata()));
    }

    /**
     * Returns the base type of all GPX types.
     *
     * @return base type of all GPX types.
     */
    @Override
    public DefaultFeatureType getType() {
        return types.parent;
    }

    /**
     * Returns the stream of features.
     * This store does not cache the features. Any new iteration over features will re-read from the file.
     * The XML file is kept open until the feature stream is closed;
     * callers should not modify the file while an iteration is in progress.
     *
     * @param  parallel  ignored in current implementation.
     * @return a stream over all features in the XML file.
     * @throws DataStoreException if an error occurred while creating the feature stream.
     */
    @Override
    public final synchronized Stream<AbstractFeature> features(boolean parallel) throws DataStoreException {
        Reader r = reader;
        reader = null;
        if (r == null) try {
            r = new Reader(this);
            version = r.initialize(false);
        } catch (DataStoreException e) {
            throw e;
        } catch (URISyntaxException | RuntimeException e) {
            throw new DataStoreContentException(e);
        } catch (Exception e) {
            throw new DataStoreException(e);
        }
        final Stream<AbstractFeature> features = StreamSupport.stream(r, false);
        return features.onClose(r);
    }

    /**
     * Closes only the reader, without closing this store.
     * This method may be invoked before write operation.
     * It must be invoked inside a synchronized block.
     */
    final void closeReader() throws Exception {
        final Reader r = reader;
        if (r != null) {
            reader = null;
            r.close();
        }
    }

    /**
     * Closes this data store and releases any underlying resources.
     *
     * @throws DataStoreException if an error occurred while closing this data store.
     */
    @Override
    public synchronized void close() throws DataStoreException {
        listeners.close();                  // Should never fail.
        version  = null;
        metadata = null;
        try {
            closeReader();
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
