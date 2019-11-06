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

import java.util.List;
import java.util.ArrayList;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javafx.concurrent.Task;
import org.opengis.feature.Feature;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.util.collection.BackingStoreException;


/**
 * A task to execute in background thread for fetching feature instances.
 * This task does not load all features; only {@value #PAGE_SIZE} of them are loaded.
 *
 * <p>Loading processes are started by {@link org.apache.sis.gui.dataset.FeatureTable.InitialLoader}.
 * Only additional pages are loaded by ordinary {@code Loader}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
class FeatureLoader extends Task<List<Feature>> {
    /**
     * Maximum number of features to load in a background task.
     * If there is more features to load, we will use many tasks.
     *
     * @see FeatureTable#nextPageLoader
     */
    private static final int PAGE_SIZE = 100;

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
     * An estimation of the number of features, or {@link Long#MAX_VALUE} if unknown.
     */
    private long estimatedCount;

    /**
     * Creates a new loader. Callers shall invoke {@link #initialize(FeatureSet)}
     * in the worker thread before to let the {@link #call()} method be executed.
     *
     * @see #initialize(FeatureSet)
     */
    FeatureLoader() {
        estimatedCount = Long.MAX_VALUE;
    }

    /**
     * Initializes this task for reading features from the specified set.
     * This method is invoked by subclasses after construction but before
     * {@link #call()} execution.
     */
    final void initialize(final FeatureSet features) throws DataStoreException {
        toClose        = features.features(false);
        iterator       = toClose .spliterator();
        estimatedCount = iterator.estimateSize();
    }

    /**
     * Creates a new task for continuing the work of a previous task.
     * The new task will load the next {@value #PAGE_SIZE} features.
     *
     * @see #next()
     */
    private FeatureLoader(final FeatureLoader previous) {
        toClose        = previous.toClose;
        iterator       = previous.iterator;
        estimatedCount = previous.estimatedCount;
    }

    /**
     * If there is more features to load, returns a new task for loading the next
     * {@value #PAGE_SIZE} features. Otherwise returns {@code null}.
     */
    final FeatureLoader next() {
        return (iterator != null) ? new FeatureLoader(this) : null;
    }

    /**
     * Invoked in a background thread for loading up to {@value #PAGE_SIZE} features.
     * If this method completed successfully but there is still more feature to read,
     * then {@link #iterator} will have a non-null value and {@link #next()} should be
     * invoked for preparing the reading of another page of features. In other cases,
     * {@link #iterator} is null and the stream has been closed.
     */
    @Override
    protected List<Feature> call() throws DataStoreException {
        final Spliterator<Feature> it = iterator;
        iterator = null;                                // Clear now in case an exception happens below.
        final List<Feature> instances = new ArrayList<>((int) Math.min(estimatedCount, PAGE_SIZE));
        if (it != null) try {
            while (it.tryAdvance(instances::add)) {
                if (instances.size() >= PAGE_SIZE) {
                    iterator = it;                      // Remember that there is more instances to read.
                    return instances;                   // Intentionally skip the call to close().
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
        return instances;
    }

    /**
     * Closes the feature stream. This method can be invoked in worker thread or in JavaFX thread,
     * but only when {@link #call()} finished its work (if unsure, see {@link #waitAndClose()}).
     * It is safe to invoke this method again even if this loader has already been closed.
     */
    final void close() throws DataStoreException {
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
}
