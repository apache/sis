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

import java.net.URISyntaxException;
import org.opengis.util.NameFactory;
import org.opengis.util.FactoryException;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.distribution.Format;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.ConcurrentReadException;
import org.apache.sis.storage.IllegalNameException;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.storage.xml.stream.StaxDataStore;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Version;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.util.iso.DefaultNameFactory;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.distribution.DefaultFormat;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.Stream;
import org.apache.sis.internal.jdk8.StreamSupport;
import org.apache.sis.internal.jdk8.UncheckedIOException;
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.DefaultFeatureType;


/**
 * A data store backed by GPX files.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public final class Store extends StaxDataStore {
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
            types = new Types(DefaultFactories.forBuildin(NameFactory.class, DefaultNameFactory.class), null, library);
        } catch (FactoryException e) {
            throw new DataStoreException(e);
        }
    }

    /**
     * Returns a more complete description of the GPX format.
     * The format will be part of the metadata returned by {@link #getMetadata()}.
     *
     * @see StoreProvider#getFormat()
     * @see org.apache.sis.internal.storage.gpx.Metadata#getResourceFormats()
     */
    final Format getFormat() {
        assert Thread.holdsLock(this);
        Format format = ((StoreProvider) provider).getFormat(listeners);
        if (version != null) {
            final DefaultFormat df = new DefaultFormat(format);
            final DefaultCitation citation = new DefaultCitation(df.getFormatSpecificationCitation());
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
     * Returns the feature type for the given name. The {@code name} argument should be the result of calling
     * {@link org.opengis.util.GenericName#toString()} on the name of one of the feature types in this data store.
     *
     * @param  name  the name or alias of the feature type to get.
     * @return the feature type of the given name or alias (never {@code null}).
     * @throws IllegalNameException if the given name was not found or is ambiguous.
     */
    @Override
    public DefaultFeatureType getFeatureType(final String name) throws IllegalNameException {
        return types.names.get(this, name);
    }

    /**
     * Returns the stream of features.
     *
     * @return a stream over all features in the XML file.
     * @throws DataStoreException if an error occurred while creating the feature stream.
     */
    @Override
    public synchronized Stream<AbstractFeature> features() throws DataStoreException {
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
     * Replaces the content of this GPX file by the given metadata and features.
     *
     * @param  metadata  the metadata to write, or {@code null} if none.
     * @param  features  the features to write, or {@code null} if none.
     * @throws ConcurrentReadException if the {@code features} stream was provided by this data store.
     * @throws DataStoreException if an error occurred while writing the data.
     */
    public synchronized void write(final Metadata metadata, final Stream<? extends AbstractFeature> features)
            throws DataStoreException
    {
        try {
            /*
             * If we created a reader for reading metadata, we need to close that reader now otherwise the call
             * to 'new Writer(…)' will fail.  Note that if that reader was in use by someone else, the 'reader'
             * field would be null and the 'new Writer(…)' call should detect that a reader is in use somewhere.
             */
            final Reader r = reader;
            if (r != null) {
                reader = null;
                r.close();
            }
            /*
             * Get the writer if no read or other write operation is in progress, then write the data.
             */
            try (Writer writer = new Writer(this, org.apache.sis.internal.storage.gpx.Metadata.castOrCopy(metadata, locale))) {
                writer.writeStartDocument();
                if (features != null) {
                    features.forEachOrdered(writer);
                }
                writer.writeEndDocument();
            }
        } catch (BackingStoreException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof DataStoreException) {
                throw (DataStoreException) cause;
            }
            throw new DataStoreException(e.getLocalizedMessage(), cause);
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
        final Reader r = reader;
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
