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

import java.util.Optional;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import org.opengis.util.NameFactory;
import org.opengis.util.FactoryException;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.distribution.Format;
import org.apache.sis.storage.WritableFeatureSet;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.ConcurrentReadException;
import org.apache.sis.storage.IllegalNameException;
import org.apache.sis.storage.IllegalFeatureTypeException;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.storage.StoreUtilities;
import org.apache.sis.internal.storage.xml.stream.StaxDataStore;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Version;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.util.iso.DefaultNameFactory;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.distribution.DefaultFormat;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;


/**
 * A data store backed by GPX files.
 * This store does not cache the feature instances.
 * Any new {@linkplain #features(boolean) request for features} will re-read from the file.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   0.8
 * @module
 */
public final class Store extends StaxDataStore implements WritableFeatureSet {
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
     * The {@link org.opengis.feature.FeatureType} for routes, tracks, way points, <i>etc</i>.
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
        listeners.useReadOnlyEvents();
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
    public FeatureType getType() {
        return types.parent;
    }

    /**
     * Returns the feature type for the given name. The {@code name} argument should be the result of calling
     * {@link org.opengis.util.GenericName#toString()} on the name of one of the feature types in this data store.
     *
     * @param  name  the name or alias of the feature type to get.
     * @return the feature type of the given name or alias (never {@code null}).
     * @throws IllegalNameException if the given name was not found or is ambiguous.
     *
     * @deprecated We are not sure yet if we will keep this method. Decision is pending acquisition of
     *             more experience with the API proposed by {@link org.apache.sis.storage.FeatureSet}.
     */
    @Deprecated
    public FeatureType getFeatureType(final String name) throws IllegalNameException {
        return types.names.get(this, name);
    }

    /**
     * Verifies the type of feature instances in this feature set.
     * This method does nothing if the specified type is equal to {@link #getType()},
     * or throws {@link IllegalFeatureTypeException} otherwise.
     *
     * @param  newType  new feature type definition (not {@code null}).
     * @throws DataStoreException if the given type is not compatible with the types supported by the store.
     */
    @Override
    public void updateType(final FeatureType newType) throws DataStoreException {
        if (!newType.equals(getType())) {
            throw new IllegalFeatureTypeException(getLocale(), StoreProvider.NAME, newType.getName());
        }
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
    public final synchronized Stream<Feature> features(boolean parallel) throws DataStoreException {
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
        final Stream<Feature> features = StreamSupport.stream(r, false);
        return features.onClose(r);
    }

    /**
     * Appends new feature instances in this {@code FeatureSet}.
     * Any feature already present in this {@link FeatureSet} will remain unmodified.
     *
     * @param  features  feature instances to append in this {@code FeatureSet}.
     * @throws DataStoreException if the feature stream cannot be obtained or updated.
     */
    @Override
    public synchronized void add(final Iterator<? extends Feature> features) throws DataStoreException {
        try (Updater updater = updater()) {
            updater.add(features);
            updater.flush();
        }
    }

    /**
     * Removes all feature instances from this {@code FeatureSet} which matches the given predicate.
     *
     * @param  filter  a predicate which returns {@code true} for feature instances to be removed.
     * @return {@code true} if any elements were removed.
     * @throws DataStoreException if the feature stream cannot be obtained or updated.
     */
    @Override
    public synchronized boolean removeIf(final Predicate<? super Feature> filter) throws DataStoreException {
        try (Updater updater = updater()) {
            return updater.removeIf(filter);
        }
    }

    /**
     * Updates all feature instances from this {@code FeatureSet} which match the given predicate.
     * If the given operator returns {@code null}, then the filtered feature is removed.
     *
     * @param  filter       a predicate which returns {@code true} for feature instances to be updated.
     * @param  replacement  operation called for each matching {@link Feature} instance. May return {@code null}.
     * @throws DataStoreException if the feature stream cannot be obtained or updated.
     */
    @Override
    public synchronized void replaceIf(final Predicate<? super Feature> filter, final UnaryOperator<Feature> replacement)
            throws DataStoreException
    {
        try (Updater updater = updater()) {
            updater.replaceIf(filter, replacement);
            updater.flush();
        }
    }

    /**
     * Returns the helper object to use for updating the GPX file.
     *
     * @todo In current version, we flush the updater after each write operation.
     *       In a future version, we should keep it in a private field and flush
     *       only after some delay, on close, or before a read operation.
     */
    private Updater updater() throws DataStoreException {
        try {
            return new Updater(this, getSpecifiedPath());
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
    }

    /**
     * Replaces the content of this GPX file by the given metadata and features.
     *
     * @param  metadata  the metadata to write, or {@code null} if none.
     * @param  features  the features to write, or {@code null} if none.
     * @throws ConcurrentReadException if the {@code features} stream was provided by this data store.
     * @throws DataStoreException if an error occurred while writing the data.
     *
     * @deprecated To be replaced by {@link #add(Iterator)}, after we resolved how to specify metadata.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-411">SIS-411</a>
     */
    @Deprecated
    public synchronized void write(final Metadata metadata, final Stream<? extends Feature> features) throws DataStoreException {
        try {
            /*
             * If we created a reader for reading metadata, we need to close that reader now otherwise the call
             * to `new Writer(…)` will fail.  Note that if that reader was in use by someone else, the `reader`
             * field would be null and the `new Writer(…)` call should detect that a reader is in use somewhere.
             */
            final Reader r = reader;
            if (r != null) {
                reader = null;
                r.close();
            }
            /*
             * Get the writer if no read or other write operation is in progress, then write the data.
             */
            try (Writer writer = new Writer(this, org.apache.sis.internal.storage.gpx.Metadata.castOrCopy(metadata, locale), null)) {
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
        listeners.close();                  // Should never fail.
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
