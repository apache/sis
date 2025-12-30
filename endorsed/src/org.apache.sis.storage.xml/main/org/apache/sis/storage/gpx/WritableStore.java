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

import java.util.Iterator;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.opengis.metadata.Metadata;
import org.apache.sis.storage.WritableFeatureSet;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.ConcurrentReadException;
import org.apache.sis.storage.IllegalFeatureTypeException;
import org.apache.sis.util.collection.BackingStoreException;

// Specific to the main branch:
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.DefaultFeatureType;


/**
 * A GPX store capable to write GPX file.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class WritableStore extends Store implements WritableFeatureSet {
    /**
     * Creates a new GPX store from the given file, URL or stream object.
     * This constructor invokes {@link StorageConnector#closeAllExcept(Object)},
     * keeping open only the needed resource.
     *
     * @param  provider   the provider of this data store, or {@code null} if unspecified.
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @throws DataStoreException if an error occurred while opening the GPX file.
     */
    public WritableStore(final StoreProvider provider, final StorageConnector connector) throws DataStoreException {
        super(provider, connector);
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
    public void updateType(final DefaultFeatureType newType) throws DataStoreException {
        if (!newType.equals(getType())) {
            throw new IllegalFeatureTypeException(getLocale(), StoreProvider.NAME, newType.getName());
        }
    }

    /**
     * Appends new feature instances in this {@code FeatureSet}.
     * Any feature already present in this {@link WritableFeatureSet} will remain unmodified.
     *
     * @param  features  feature instances to append in this {@code FeatureSet}.
     * @throws DataStoreException if the feature stream cannot be obtained or updated.
     */
    @Override
    public synchronized void add(final Iterator<? extends AbstractFeature> features) throws DataStoreException {
        try (Updater updater = updater()) {
            updater.add(features);
            updater.flush();
        }
    }

    /**
     * Removes all feature instances from this {@code FeatureSet} which matches the given predicate.
     *
     * @param  filter  a predicate which returns {@code true} for feature instances to be removed.
     * @throws DataStoreException if the feature stream cannot be obtained or updated.
     */
    @Override
    public synchronized void removeIf(final Predicate<? super AbstractFeature> filter) throws DataStoreException {
        try (Updater updater = updater()) {
            updater.removeIf(filter);
            updater.flush();
        }
    }

    /**
     * Updates all feature instances from this {@code FeatureSet} which match the given predicate.
     * If the given operator returns {@code null}, then the filtered feature is removed.
     *
     * @param  filter       a predicate which returns {@code true} for feature instances to be updated.
     * @param  replacement  operation called for each matching {@code Feature} instance. May return {@code null}.
     * @throws DataStoreException if the feature stream cannot be obtained or updated.
     */
    @Override
    public synchronized void replaceIf(final Predicate<? super AbstractFeature> filter, final UnaryOperator<AbstractFeature> replacement)
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
            return new Updater(this, locationAsPath);
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
    @Deprecated(since="1.3")
    public synchronized void write(final Metadata metadata, final Stream<? extends AbstractFeature> features) throws DataStoreException {
        try {
            /*
             * If we created a reader for reading metadata, we need to close that reader now otherwise the call
             * to `new Writer(…)` will fail.  Note that if that reader was in use by someone else, the `reader`
             * field would be null and the `new Writer(…)` call should detect that a reader is in use somewhere.
             */
            closeReader();
            /*
             * Get the writer if no read or other write operation is in progress, then write the data.
             */
            try (Writer writer = new Writer(this, org.apache.sis.storage.gpx.Metadata.castOrCopy(metadata, dataLocale), null)) {
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
}
