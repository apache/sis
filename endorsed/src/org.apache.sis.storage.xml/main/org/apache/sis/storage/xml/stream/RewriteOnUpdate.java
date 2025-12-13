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
package org.apache.sis.storage.xml.stream;

import java.util.Locale;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.ReadOnlyStorageException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.collection.BackingStoreException;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;


/**
 * Helper class for updating an existing XML file, with no feature type change permitted.
 * The implementation strategy is to rewrite fully the updated features in a temporary file,
 * then replaces the source file by the temporary file when ready.
 *
 * <p>The {@link #flush()} method should always been invoked before a {@code RewriteOnUpdate}
 * reference is lost, otherwise data may be lost.</p>
 *
 * <h2>Multi-threading</h2>
 * This class is not synchronized for multi-threading. Synchronization is caller's responsibility,
 * because the caller usually needs to take in account other data store operations such as reads.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class RewriteOnUpdate implements AutoCloseable {
    /**
     * The set of features to update. This is the set specified at construction time.
     */
    protected final FeatureSet source;

    /**
     * The main file, or {@code null} if unknown.
     */
    private final Path location;

    /**
     * Whether the store is initially empty.
     * It may be the underlying file does not exist or has a length of zero.
     */
    private boolean isSourceEmpty;

    /**
     * The features to write, fetched when first needed.
     *
     * @see #filtered()
     */
    private Stream<? extends Feature> filtered;

    /**
     * Creates an updater for the given source of features.
     *
     * @param  source    the set of features to update.
     * @param  location  the main file, or {@code null} if unknown.
     * @throws IOException if an error occurred while determining whether the file is empty.
     */
    public RewriteOnUpdate(final FeatureSet source, final Path location) throws IOException {
        this.source   = source;
        this.location = location;
        isSourceEmpty = (location == null) || Files.notExists(location) || Files.size(location) == 0;
    }

    /**
     * Returns the locale to use for locale-sensitive data, or {@code null} if unspecified.
     * This is <strong>not</strong> for logging or warning messages.
     *
     * @return the data locale, or {@code null}.
     */
    protected final Locale getLocale() {
        return (source instanceof StaxDataStore) ? ((StaxDataStore) source).getDataLocale() : null;
    }

    /**
     * Returns {@code true} if there is currently no data.
     */
    private boolean isEmpty() throws ReadOnlyStorageException {
        if (isSourceEmpty) {
            return filtered == null;
        } else if (location != null) {
            return false;
        } else {
            throw new ReadOnlyStorageException();
        }
    }

    /**
     * Returns the features to write.
     *
     * @throws DataStoreException if the feature stream cannot be obtained.
     */
    private Stream<? extends Feature> filtered() throws DataStoreException {
        if (filtered == null) {
            filtered = features();
        }
        return filtered;
    }

    /**
     * Returns the stream of features to copy.
     * The default implementation delegates to {@link FeatureSet#features(boolean)}.
     *
     * @return all features contained in the dataset.
     * @throws DataStoreException if an error occurred while fetching the features.
     */
    protected Stream<? extends Feature> features() throws DataStoreException {
        return source.features(false);
    }

    /**
     * Appends new feature instances in the {@code FeatureSet}.
     * Any feature already present in the {@link FeatureSet} will remain unmodified.
     *
     * @param  features  feature instances to append in the {@code FeatureSet}.
     * @throws DataStoreException if the feature stream cannot be obtained or updated.
     */
    public void add(final Iterator<? extends Feature> features) throws DataStoreException {
        ArgumentChecks.ensureNonNull("features", features);
        final Stream<? extends Feature> toAdd = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(features, Spliterator.ORDERED), false);
        if (isEmpty()) {
            filtered = toAdd;
        } else {
            filtered = Stream.concat(filtered(), toAdd);
        }
    }

    /**
     * Removes all feature instances from the {@code FeatureSet} which matches the given predicate.
     *
     * @param  filter  a predicate which returns {@code true} for feature instances to be removed.
     * @throws DataStoreException if the feature stream cannot be obtained or updated.
     */
    public void removeIf(final Predicate<? super Feature> filter) throws DataStoreException {
        ArgumentChecks.ensureNonNull("filter", filter);
        if (!isEmpty()) {
            filtered = filtered().filter((feature) -> {
                return !filter.test(feature);
            });
        }
    }

    /**
     * Updates all feature instances from the {@code FeatureSet} which match the given predicate.
     * If the given operator returns {@code null}, then the filtered feature is removed.
     *
     * @param  filter   a predicate which returns {@code true} for feature instances to be updated.
     * @param  updater  operation called for each matching {@link Feature} instance. May return {@code null}.
     * @throws DataStoreException if the feature stream cannot be obtained or updated.
     */
    public void replaceIf(final Predicate<? super Feature> filter, final UnaryOperator<Feature> updater) throws DataStoreException {
        ArgumentChecks.ensureNonNull("filter",  filter);
        ArgumentChecks.ensureNonNull("updater", updater);
        if (!isEmpty()) {
            filtered = filtered().map((feature) -> (feature != null) && filter.test(feature) ? updater.apply(feature) : feature);
        }
    }

    /**
     * Creates an initially empty temporary file.
     *
     * @return the temporary file.
     * @throws IOException if an error occurred while creating the temporary file.
     */
    protected abstract Path createTemporaryFile() throws IOException;

    /**
     * Creates a new XML document writer for an output in the specified temporary file.
     * Caller is responsible for closing the writer.
     *
     * @param  temporary  the temporary stream where to write, or {@code null} for writing directly in the store file.
     * @return the writer where to copy updated features.
     * @throws Exception if an error occurred while creating the writer.
     *         May be {@link DataStoreException}, {@link IOException}, {@link RuntimeException}, <i>etc.</i>
     */
    protected abstract StaxStreamWriter createWriter(OutputStream temporary) throws Exception;

    /**
     * Writes immediately all feature instances.
     * This method does nothing if there is no data to write.
     *
     * @throws DataStoreException if an error occurred.
     */
    public void flush() throws DataStoreException {
        try (Stream<? extends Feature> content = filtered) {
            if (content != null) {
                filtered = null;
                OutputStream temporary = null;
                Path target = isSourceEmpty ? null : createTemporaryFile();
                try {
                    if (target != null) {
                        temporary = Files.newOutputStream(target);
                    }
                    try (StaxStreamWriter writer = createWriter(temporary)) {
                        temporary = null;       // Stream will be closed by writer.
                        isSourceEmpty = false;
                        writer.writeStartDocument();
                        content.sequential().forEachOrdered(writer);
                        writer.writeEndDocument();
                    }
                    if (target != null) {
                        Files.move(target, location, StandardCopyOption.REPLACE_EXISTING);
                        target = null;
                    }
                } finally {
                    if (temporary != null) temporary.close();
                    if (target != null) Files.delete(target);       // Delete the temporary file if an error occurred.
                }
            }
        } catch (DataStoreException e) {
            throw e;
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
     * Releases resources used by this updater. If {@link #flush()} has not been invoked, data may be lost.
     * This method is useful in try-with-resource in case something fails before {@link #flush()} invocation.
     */
    @Override
    public void close() {
        final Stream<? extends Feature> content = filtered;
        if (content != null) {
            filtered = null;
            content.close();
        }
    }
}
