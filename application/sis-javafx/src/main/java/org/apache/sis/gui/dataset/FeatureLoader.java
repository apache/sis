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
 * <p>Loading processes are started by {@link Initial} loader.
 * Only additional pages are loaded by ordinary {@code FeatureLoader}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
class FeatureLoader extends Task<Boolean> implements Consumer<Feature> {
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
     * Creates a new loader. Callers shall invoke {@link #initialize(FeatureSet)}
     * in the worker thread before to let the {@link #call()} method be executed.
     *
     * @see #initialize(FeatureSet)
     */
    FeatureLoader(final FeatureTable table) {
        this.table = table;
    }

    /**
     * Initializes this task for reading features from the specified set.
     * This method is invoked by subclasses after construction but before
     * {@link #call()} execution.
     */
    final void initialize(final FeatureSet features) throws DataStoreException {
        toClose  = features.features(false);
        iterator = toClose.spliterator();
    }

    /**
     * Creates a new task for continuing the work of a previous task.
     * The new task will load the next {@value #PAGE_SIZE} features.
     */
    private FeatureLoader(final FeatureLoader previous) {
        table    = previous.table;
        toClose  = previous.toClose;
        iterator = previous.iterator;
    }

    /**
     * Returns the list where to add features.
     * All methods on the returned list shall be invoked from JavaFX thread.
     */
    private FeatureList destination() {
        return (FeatureList) table.getItems();
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
     * should be prepared for reading of another page of features. In other cases,
     * {@link #iterator} is null and the stream has been closed.
     *
     * @return whether there is more features to load.
     */
    @Override
    protected Boolean call() throws DataStoreException {
        // Note: iterator.estimateSize() is a count or remaining elements.
        final int stopAt = (int) Math.min(iterator.estimateSize(), PAGE_SIZE);
        loaded = new Feature[stopAt];
        try {
            while (iterator.tryAdvance(this)) {
                if (count >= stopAt) {
                    return Boolean.TRUE;                // Intentionally skip the call to close().
                }
                if (isCancelled()) {
                    break;
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
        close();                                        // Loading completed or has been cancelled.
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
            // Someone does not want to let us wait before closing.
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
    protected final void succeeded() {
        final FeatureList addTo = destination();
        if (addTo.isCurrentLoader(this)) {
            if (this instanceof Initial) {
                addTo.setFeatures(iterator.estimateSize(), iterator.characteristics(), loaded, count);
            } else {
                addTo.addFeatures(loaded, count);
            }
            addTo.setNextPage(getValue() ? new FeatureLoader(this) : null);
        } else try {
            close();
        } catch (DataStoreException e) {
            FeatureTable.unexpectedException("setFeatures", e);
        }
    }

    /**
     * Invoked in JavaFX thread when a loading process has been cancelled or failed.
     * This method closes the {@link FeatureLoader} if it did not closed itself,
     * then eventually shows the error in the table area.
     *
     * @see FeatureTable#interrupt()
     */
    @Override
    protected final void cancelled() {
        final FeatureList addTo = destination();
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
                exception.printStackTrace();        // TODO: write somewhere in the widget.
            } else {
                // Since we moved to other data, not appropriate anymore for current widget.
                FeatureTable.unexpectedException("cancelled", exception);
            }
        }
    }

    /**
     * Invoked in JavaFX thread when a loading process failed.
     */
    @Override
    protected final void failed() {
        cancelled();
    }

    /**
     * The task to execute in background thread for initiating the loading process.
     * This tasks is created only for the first {@value #PAGE_SIZE} features.
     * For all additional features, an ordinary {@link FeatureLoader} will be used.
     */
    static final class Initial extends FeatureLoader {
        /**
         * The set of features to read.
         */
        private final FeatureSet features;

        /**
         * Initializes a new task for loading features from the given set.
         */
        Initial(final FeatureTable table, final FeatureSet features) {
            super(table);
            this.features = features;
        }

        /**
         * Gets the feature type, initializes the iterator and gets the first {@value #PAGE_SIZE} features.
         * The {@link FeatureType} should be given by {@link FeatureSet#getType()} but this method is robust
         * to incomplete implementations where {@code getType()} returns {@code null}.
         */
        @Override
        protected Boolean call() throws DataStoreException {
            final boolean isTypeKnown = setType(features.getType());
            initialize(features);
            final Boolean status = super.call();
            if (isTypeKnown) {
                setTypeFromFirst();
            }
            return status;
        }
    }

    /**
     * Invoked when the feature type may have been found. If the given type is non-null,
     * then this method delegates to {@link FeatureTable#setFeatureType(FeatureType)} in
     * the JavaFX thread. This will erase the previous content and prepare new columns.
     *
     * @param  type  the feature type, or {@code null}.
     * @return whether the given type was non-null.
     */
    final boolean setType(final FeatureType type) {
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
    final void setTypeFromFirst() throws DataStoreException {
        for (int i=0; i<count; i++) {
            final Feature f = loaded[i];
            if (f != null && setType(f.getType())) {
                return;
            }
        }
        throw new DataStoreException(Resources.forLocale(table.textLocale).getString(Resources.Keys.NoFeatureTypeInfo));
    }
}
