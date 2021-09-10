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
package org.apache.sis.gui.dataset;

import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.function.Consumer;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.internal.gui.Resources;


/**
 * A task to execute in background thread for fetching feature instances.
 * This task does not load all features; only {@value #PAGE_SIZE} of them are loaded.
 * The boolean value returned by this task tells whether there is more features to load.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class FeatureLoader extends Task<Boolean> implements Consumer<Feature> {
    /**
     * Maximum number of features to load in a background task.
     * If there is more features to load, we will use many tasks.
     *
     * @see FeatureList#nextPageLoader
     */
    private static final int PAGE_SIZE = 100;

    /**
     * The table where to add the features loaded by this task.
     * All methods on this object shall be invoked from JavaFX thread.
     */
    private final FeatureTable table;

    /**
     * The feature set from which to get the initial configuration.
     * This is non-null only for the task loading the first {@value #PAGE_SIZE} instances,
     * then become null for all subsequent tasks.
     */
    private final FeatureSet initializer;

    /**
     * The stream to close after we finished to iterate over features.
     * This stream should not be used for any other purpose.
     */
    private Stream<Feature> toClose;

    /**
     * If the reading process is not finished, the iterator for reading more feature instances.
     */
    private Spliterator<Feature> iterator;

    /**
     * The features loaded by this task. This array is created in a background thread,
     * then added to {@link #table} in the JavaFX thread.
     */
    private Feature[] loaded;

    /**
     * Number of features loaded by this task.
     * This is the number of valid elements in the {@link #loaded} array.
     */
    private int count;

    /**
     * Creates a new loader for the given set of features.
     */
    FeatureLoader(final FeatureTable table, final FeatureSet features) {
        this.table  = table;
        initializer = features;
    }

    /**
     * Creates a new task for continuing the work of a previous task.
     * The new task will load the next {@value #PAGE_SIZE} features.
     */
    private FeatureLoader(final FeatureLoader previous) {
        table       = previous.table;
        toClose     = previous.toClose;
        iterator    = previous.iterator;
        initializer = null;
    }

    /**
     * Callback method for {@link Spliterator#tryAdvance(Consumer)},
     * defined for {@link #call()} internal purpose only.
     */
    @Override
    public void accept(final Feature feature) {
        loaded[count++] = feature;
    }

    /**
     * Invoked in a background thread for loading up to {@value #PAGE_SIZE} features.
     * If this method completed successfully but there is still more feature to read,
     * then {@link #iterator} will keep a non-null value and a new {@link FeatureLoader}
     * well be prepared by {@link #succeeded()} for reading of another page of features.
     * In other cases, {@link #iterator} is null and the stream has been closed.
     *
     * @return whether there is more features to load.
     */
    @Override
    protected Boolean call() throws DataStoreException {
        final boolean isTypeKnown;
        if (initializer != null) {
            isTypeKnown = setType(initializer.getType());
            toClose     = initializer.features(false);
            iterator    = toClose.spliterator();
        } else {
            isTypeKnown = true;
        }
        /*
         * iterator.estimateSize() is a count or remaining elements (not the total number).
         * If the number of remaining elements is equal to smaller than the page size, try
         * to read one more element in order to check if we really reached the stream end.
         * We do that because the estimated count is only approximate.
         */
        final long remaining = iterator.estimateSize();
        final int stopAt = (remaining > PAGE_SIZE) ? PAGE_SIZE : 1 + (int) remaining;
        loaded = new Feature[stopAt];
        try {
            while (iterator.tryAdvance(this)) {
                if (count >= stopAt) {
                    setMissingType(isTypeKnown);
                    return Boolean.TRUE;                // Intentionally skip the call to close().
                }
                if (isCancelled()) {
                    close();
                    return Boolean.FALSE;
                }
            }
        } catch (BackingStoreException e) {
            try {
                close();
            } catch (DataStoreException s) {
                e.addSuppressed(s);
            }
            throw e.unwrapOrRethrow(DataStoreException.class);
        }
        close();                                        // Loading completed successfully.
        setMissingType(isTypeKnown);
        return Boolean.FALSE;
    }

    /**
     * Closes the feature stream. This method can be invoked in worker thread or in JavaFX thread,
     * but only when {@link #call()} finished its work (if unsure, see {@link #waitAndClose()}).
     * It is safe to invoke this method again even if this loader has already been closed.
     */
    private void close() throws DataStoreException {
        iterator = null;
        final Stream<Feature> c = toClose;
        if (c != null) try {
            toClose = null;                             // Clear now in case an exception happens below.
            c.close();
        } catch (BackingStoreException e) {
            throw e.unwrapOrRethrow(DataStoreException.class);
        }
    }

    /**
     * Waits for {@link #call()} to finish its work either successfully or as a result of cancellation,
     * then closes the stream. This method should be invoked in a background thread when we don't know
     * if the task is still running or not.
     *
     * @see FeatureTable#interrupt()
     */
    final void waitAndClose() {
        Throwable error = null;
        try {
            get();                            // Wait for the task to stop before to close the stream.
        } catch (InterruptedException | CancellationException e) {
            /*
             * Someone does not want to let us wait before closing. Log the exception so that
             * if a ClosedChannelException happens in another thread, we can understand why.
             */
            FeatureTable.recoverableException("interrupt", e);
        } catch (ExecutionException e) {
            error = e.getCause();
        }
        try {
            close();
        } catch (DataStoreException e) {
            if (error != null) {
                error.addSuppressed(e);
            } else {
                error = e;
            }
        }
        if (error != null) {
            // FeatureTable.interrupt() is the public API calling this method.
            FeatureTable.unexpectedException("interrupt", error);
        }
    }

    /**
     * Invoked in JavaFX thread after new feature instances are ready.
     * This method adds the new rows in the table and prepares another
     * task for loading the next batch of features when needed.
     */
    @Override
    protected void succeeded() {
        final FeatureList addTo = table.getFeatureList();
        if (addTo.isCurrentLoader(this)) {
            final boolean hasMore = getValue();
            if (initializer != null) {
                final long remainingCount;
                final int characteristics;
                if (hasMore) {
                    remainingCount  = iterator.estimateSize();
                    characteristics = iterator.characteristics();
                } else {
                    remainingCount  = 0;
                    characteristics = Spliterator.SIZED;
                }
                addTo.setFeatures(remainingCount, characteristics, loaded, count, hasMore);
            } else {
                addTo.addFeatures(loaded, count, hasMore);
            }
            addTo.setNextPage(hasMore ? new FeatureLoader(this) : null);
        } else try {
            close();
        } catch (DataStoreException e) {
            FeatureTable.unexpectedException("setFeatures", e);
        }
    }

    /**
     * Invoked in JavaFX thread when a loading process has been cancelled or failed.
     *
     * @see FeatureTable#interrupt()
     */
    @Override
    protected void cancelled() {
        stop("cancelled");
    }

    /**
     * Invoked in JavaFX thread when a loading process failed.
     */
    @Override
    protected void failed() {
        stop("failed");
    }

    /**
     * Closes the {@link FeatureLoader} if it did not closed itself,
     * then eventually shows the error in the table area.
     */
    private void stop(final String caller) {
        final FeatureList addTo = table.getFeatureList();
        final boolean isCurrentLoader = addTo.isCurrentLoader(this);
        if (isCurrentLoader) {
            addTo.setNextPage(null);
        }
        /*
         * Loader should be already closed if error or cancellation happened during the reading process.
         * But it may not be closed if the task was cancelled before it started, or maybe because of some
         * other holes we missed. So close again as a double-check.
         */
        Throwable exception = getException();
        try {
            close();
        } catch (DataStoreException e) {
            if (exception == null) {
                exception = e;
            } else {
                exception.addSuppressed(e);
            }
        }
        if (exception != null) {
            if (isCurrentLoader) {
                table.setException(exception);
            } else {
                // Since we moved to other data, not appropriate anymore for current widget.
                FeatureTable.unexpectedException(caller, exception);
            }
        }
    }

    /**
     * Invoked when the feature type may have been found. If the given type is non-null,
     * then this method delegates to {@link FeatureTable#setFeatureType(FeatureType)} in
     * the JavaFX thread. This will erase the previous content and prepare new columns.
     *
     * <p>This method is invoked, directly or indirectly, only from the {@link #call()}
     * method with non-null {@link #initializer}. Consequently the new rows have not yet
     * been added at this time.</p>
     *
     * @param  type  the feature type, or {@code null}.
     * @return whether the given type was non-null.
     */
    private boolean setType(final FeatureType type) {
        if (type != null) {
            Platform.runLater(() -> table.setFeatureType(type));
            return true;
        } else {
            return false;
        }
    }

    /**
     * Safety for data stores that do not implement the {@link FeatureSet#getType()} method.
     * That method is mandatory and implementations should not be allowed to return null, but
     * incomplete implementations exist so we are better to be safe. If we can not get the type
     * from the first feature instances, we will give up.
     */
    private void setMissingType(final boolean isTypeKnown) throws DataStoreException {
        if (!isTypeKnown) {
            for (int i=0; i<count; i++) {
                final Feature f = loaded[i];
                if (f != null && setType(f.getType())) {
                    return;
                }
            }
            throw new DataStoreException(Resources.forLocale(table.textLocale).getString(Resources.Keys.NoFeatureTypeInfo));
        }
    }
}
