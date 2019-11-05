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

import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CancellationException;
import javafx.application.Platform;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.WorkerStateEvent;
import javafx.concurrent.Task;
import javafx.util.Callback;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.util.InternationalString;
import org.apache.sis.internal.util.Strings;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.internal.gui.BackgroundThreads;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.storage.DataStoreException;


/**
 * A view of {@link FeatureSet} data organized as a table. The features are specified by a call
 * to {@link #setFeatures(FeatureSet)}, which will load the features in a background thread.
 * At first only {@value #PAGE_SIZE} features are loaded.
 * More features will be loaded only when the user scroll down.
 *
 * <p>If this view is removed from scene graph, then {@link #interrupt()} should be called
 * for stopping any loading process that may be under progress.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Smaniotto Enzo (GSoC)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class FeatureTable extends TableView<Feature> {
    /**
     * Maximum number of features to load in a background task.
     * If there is more features to load, we will use many tasks.
     *
     * @see #nextPageLoader
     */
    private static final int PAGE_SIZE = 100;

    /**
     * The locale to use for texts.
     */
    private final Locale textLocale;

    /**
     * The locale to use for dates/numbers.
     * This is often the same than {@link #textLocale}.
     */
    private final Locale dataLocale;

    /**
     * The type of features, or {@code null} if not yet determined.
     * This type determines the columns that will be shown.
     *
     * @see #setFeatureType(FeatureType)
     */
    private FeatureType featureType;

    /**
     * If not all features have been read, the task for loading the next batch of {@value #PAGE_SIZE} features.
     * This task will be executed only if there is a need to see new features.
     *
     * <p>If a loading is in progress, then this field is the loader doing the work.
     * But this field will be updated with next loader as soon as the loading is completed.</p>
     */
    private Loader nextPageLoader;

    /**
     * Creates an initially empty table.
     */
    public FeatureTable() {
        textLocale = Locale.getDefault(Locale.Category.DISPLAY);
        dataLocale = Locale.getDefault(Locale.Category.FORMAT);
        setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        setTableMenuButtonVisible(true);
    }

    /**
     * Sets the features to show in this table. This method loads an arbitrary amount of
     * features in a background thread. It does not load all features if the feature set
     * is large, unless the user scroll down.
     *
     * <p>If the loading of another {@code FeatureSet} was in progress at the time this method is invoked,
     * then that previous loading process is cancelled.</p>
     *
     * <p><b>Note:</b> the table content may appear unmodified after this method returns.
     * The modifications will appear at an undetermined amount of time later.</p>
     *
     * @param  features  the features, or {@code null} if none.
     */
    public void setFeatures(final FeatureSet features) {
        assert Platform.isFxApplicationThread();
        final Loader previous = nextPageLoader;
        if (previous != null) {
            nextPageLoader = null;
            previous.cancel();
        }
        if (features != null) {
            setLoader(new InitialLoader(features));
            BackgroundThreads.execute(nextPageLoader);
        } else {
            featureType = null;
            getItems().clear();
            getColumns().clear();
        }
    }

    /**
     * Sets {@link #nextPageLoader} to the given values and sets the listeners, but without starting the task yet.
     *
     * @param  loader  the loader for next {@value #PAGE_SIZE} features,
     *                 or {@code null} if there is no more features to load.
     */
    private void setLoader(final Loader loader) {
        if (loader != null) {
            loader.setOnSucceeded(this::addFeatures);
            loader.setOnCancelled(this::cancelled);
            loader.setOnFailed(this::cancelled);
        }
        nextPageLoader = loader;
    }

    /**
     * Invoked in JavaFX thread after new feature instances are ready.
     */
    private void addFeatures(final WorkerStateEvent event) {
        assert Platform.isFxApplicationThread();
        final Loader loader = (Loader) event.getSource();
        if (loader == nextPageLoader) {
            getItems().addAll((List<Feature>) event.getSource().getValue());
            setLoader(nextPageLoader.next());

            // TODO: temporary hack: we should not start the job now, but wait until we need it.
            if (nextPageLoader != null) {
                BackgroundThreads.execute(nextPageLoader);
            }
        } else try {
            loader.close();
        } catch (DataStoreException e) {
            unexpectedException("addFeatures", e);
        }
    }

    /**
     * Invoked in JavaFX thread when a loading process has been cancelled or failed.
     *
     * @see #interrupt()
     */
    private void cancelled(final WorkerStateEvent event) {
        assert Platform.isFxApplicationThread();
        final Loader loader = (Loader) event.getSource();
        final boolean isCurrentLoader = (loader == nextPageLoader);
        if (isCurrentLoader) {
            nextPageLoader = null;
        }
        /*
         * Loader should be already closed if error or cancellation happened during the reading process.
         * But it may not be closed if the task was cancelled before it started, or maybe because of some
         * other holes we missed. So close again as a double-check.
         */
        Throwable exception = loader.getException();
        try {
            loader.close();
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
                unexpectedException("cancelled", exception);
            }
        }
    }

    /**
     * A task to execute in background thread for fetching feature instances.
     * This task does not load all features; only {@value #PAGE_SIZE} of them are loaded.
     *
     * <p>Loading processes are started by {@link InitialLoader}.
     * Only additional pages are loaded by ordinary {@code Loader}.</p>
     */
    private static class Loader extends Task<List<Feature>> {
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
         * Creates a new loader. This constructor is for {@link InitialLoader} usage only.
         */
        Loader() {
            estimatedCount = Long.MAX_VALUE;
        }

        /**
         * Creates a new task for continuing the work of a previous task.
         * The new task will load the next {@value #PAGE_SIZE} features.
         */
        private Loader(final Loader previous) {
            toClose        = previous.toClose;
            iterator       = previous.iterator;
            estimatedCount = previous.estimatedCount;
        }

        /**
         * Initializes this task for reading features from the specified set.
         * This method shall be invoked by {@link InitialLoader} only.
         */
        final void initialize(final FeatureSet features) throws DataStoreException {
            toClose        = features.features(false);
            iterator       = toClose .spliterator();
            estimatedCount = iterator.estimateSize();
        }

        /**
         * If there is more features to load, returns a new task for loading the next
         * {@value #PAGE_SIZE} features. Otherwise returns {@code null}.
         */
        final Loader next() {
            return (iterator != null) ? new Loader(this) : null;
        }

        /**
         * Loads up to {@value #PAGE_SIZE} features.
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
            close();                                        // Loading has been cancelled.
            return instances;
        }

        /**
         * Closes the feature stream. This method can be invoked only when {@link #call()} finished its work.
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
         * Wait for {@link #call()} to finish its work either successfully or as a result of cancellation,
         * then close the stream. This method should be invoked in a background thread when we don't know
         * if the task is still running or not.
         *
         * @see FeatureTable#interrupt()
         */
        final void waitAndClose() {
            Throwable error = null;
            try {
                get();      // Wait for the task to stop before to close the stream.
            } catch (InterruptedException | CancellationException e) {
                // Ignore, we will try to close the stream right now.
                recoverableException("interrupt", e);
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
                // FeatureTable.interrupt is the public API calling this method.
                unexpectedException("interrupt", error);
            }
        }
    }

    /**
     * The task to execute in background thread for initiating the loading process.
     * This tasks is created only for the first {@value #PAGE_SIZE} features.
     * For all additional features, an ordinary {@link Loader} will be used.
     */
    private final class InitialLoader extends Loader {
        /**
         * The set of features to read.
         */
        private final FeatureSet features;

        /**
         * Initializes a new task for loading features from the given set.
         */
        InitialLoader(final FeatureSet features) {
            this.features = features;
        }

        /**
         * Gets the feature type, initialize the iterator and gets the first {@value #PAGE_SIZE} features.
         * The {@link FeatureType} should be given by {@link FeatureSet#getType()}, but this method is
         * robust to incomplete implementations where {@code getType()} returns {@code null}.
         */
        @Override
        protected List<Feature> call() throws DataStoreException {
            final boolean isTypeKnown = setType(features.getType());
            initialize(features);
            final List<Feature> instances = super.call();
            if (isTypeKnown) {
                return instances;
            }
            /*
             * Following code is a safety for FeatureSet that do not implement the `getType()` method.
             * This method is mandatory and implementation should not be allowed to return null, but
             * incomplete implementations exist so we are better to be safe. If we can not get the type
             * from the first feature instances, we will give up.
             */
            for (final Feature f : instances) {
                if (f != null && setType(f.getType())) {
                    return instances;
                }
            }
            throw new DataStoreException(Resources.forLocale(textLocale).getString(Resources.Keys.NoFeatureTypeInfo));
        }

        /**
         * Invoked when the feature type may have been found. If the given type is non-null,
         * then this method delegates to {@link FeatureTable#setFeatureType(FeatureType)} in
         * the JavaFX thread. This will erase the previous content and prepare new columns.
         *
         * @param  type  the feature type, or {@code null}.
         * @return whether the given type was non-null.
         */
        private boolean setType(final FeatureType type) {
            if (type != null) {
                Platform.runLater(() -> setFeatureType(type));
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Invoked in JavaFX thread after the feature type has been determined.
     * This method clears all rows and replaces all columns by new columns
     * determined from the given type.
     */
    private void setFeatureType(final FeatureType type) {
        assert Platform.isFxApplicationThread();
        getItems().clear();
        if (type != null && !type.equals(featureType)) {
            final Collection<? extends PropertyType> properties = type.getProperties(true);
            final List<TableColumn<Feature,?>> columns = new ArrayList<>(properties.size());
            for (final PropertyType pt : properties) {
                final String name = pt.getName().toString();
                String title = string(pt.getDesignation());
                if (title == null) {
                    title = string(pt.getName().toInternationalString());
                    if (title == null) title = name;
                }
                final TableColumn<Feature, Object> column = new TableColumn<>(title);
                column.setCellValueFactory(new ValueGetter(name));
                columns.add(column);
            }
            getColumns().setAll(columns);       // Change columns in an all or nothing operation.
        }
        featureType = type;
    }

    /**
     * Fetch values to show in the table cells.
     */
    private static final class ValueGetter implements Callback<TableColumn.CellDataFeatures<Feature,Object>, ObservableValue<Object>> {
        /**
         * The name of the feature property for which to fetch values.
         */
        final String name;

        /**
         * Creates a new getter of property values.
         *
         * @param  name  name of the feature property for which to fetch values.
         */
        ValueGetter(final String name) {
            this.name = name;
        }

        /**
         * Returns the value of the feature property wrapped by the given argument.
         * This method is invoked by JavaFX when a new cell needs to be rendered.
         */
        @Override
        public ObservableValue<Object> call(final TableColumn.CellDataFeatures<Feature, Object> cell) {
            Object value = cell.getValue().getPropertyValue(name);
            if (value instanceof Collection<?>) {
                value = "collection";               // TODO
            }
            return new ReadOnlyObjectWrapper<>(value);
        }
    }

    /**
     * Returns the given international string as a non-empty localized string, or {@code null} if none.
     */
    private String string(final InternationalString i18n) {
        return (i18n != null) ? Strings.trimOrNull(i18n.toString(textLocale)) : null;
    }

    /**
     * If a loading process was under way, interrupts it and close the feature stream.
     * This method returns immediately; the release of resources happens in a background thread.
     */
    public void interrupt() {
        assert Platform.isFxApplicationThread();
        final Loader loader = nextPageLoader;
        nextPageLoader = null;
        if (loader != null) {
            loader.cancel();
            BackgroundThreads.execute(loader::waitAndClose);
        }
    }

    /**
     * Reports an exception that we can not display in this widget, for example because it applies
     * to different data than the one currently viewed. The {@code method} argument should be the
     * public API (if possible) invoking the method where the exception is caught.
     */
    private static void unexpectedException(final String method, final Throwable exception) {
        Logging.unexpectedException(Logging.getLogger(Modules.APPLICATION), FeatureTable.class, method, exception);
    }

    /**
     * Reports an exception that we choose to ignore.
     */
    private static void recoverableException(final String method, final Exception exception) {
        Logging.recoverableException(Logging.getLogger(Modules.APPLICATION), FeatureTable.class, method, exception);
    }
}
