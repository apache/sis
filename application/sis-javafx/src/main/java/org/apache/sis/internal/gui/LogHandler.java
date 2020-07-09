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
package org.apache.sis.internal.gui;

import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.event.StoreListener;
import org.apache.sis.storage.event.WarningEvent;


/**
 * A collector of log records emitted either by the logging system or by {@link DataStore} instances.
 * This class maintains both a global (system) list and a list of log records specific to each resource.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final class LogHandler extends Handler implements StoreListener<WarningEvent> {
    /**
     * Maximal number of log records stored by this class.
     */
    private static final int LIMIT = 1000;

    /**
     * The unique instance of this handler. This handler is registered on the root logger.
     */
    private static final LogHandler INSTANCE = new LogHandler();

    /**
     * Loggings related to the SIS library as a whole, not specific to any particular resources.
     * May also contain loggings from libraries other than SIS. The length of this list is limited
     * to {@value #LIMIT} elements. This list shall be read and written in JavaFX thread only.
     */
    private final ObservableList<LogRecord> systemLogs;

    /**
     * The list of log records specific to each resource.
     * Read and write operations on this map shall be synchronized on {@code resourceLogs}.
     * Read and write operations on map values shall be done in JavaFX thread only.
     */
    private final WeakHashMap<Resource, ObservableList<LogRecord>> resourceLogs;

    /**
     * The list of log records for which loading are in progress. Keys are thread identifiers
     * and values are values of {@link #resourceLogs}. Addition must be followed by a removal
     * in a {@code try ... finally} block. Read and write operations on map values shall be
     * done in JavaFX thread only.
     */
    private final ConcurrentMap<Long, ObservableList<LogRecord>> inProgress;

    /**
     * Creates an initially empty collector.
     */
    private LogHandler() {
        systemLogs   = FXCollections.observableArrayList();
        resourceLogs = new WeakHashMap<>();
        inProgress   = new ConcurrentHashMap<>();
    }

    /**
     * Registers or unregisters the unique handler instance on the root logger.
     *
     * @param  enabled  {@code true} for registering or {@code false} for unregistering.
     */
    public static void register(final boolean enabled) {
        final Logger root = Logger.getLogger("");
        if (enabled) {
            root.addHandler(INSTANCE);
        } else {
            root.removeHandler(INSTANCE);
            INSTANCE.close();
        }
    }

    /**
     * Installs warning listener on the given resource. There is not uninstall method;
     * it is okay to rely on the garbage collector when the resource is no longer used.
     *
     * @param  resource  the resource on which to install listener. May be {@code null}.
     */
    public static void installListener(final Resource resource) {
        if (resource != null) {
            resource.addListener(WarningEvent.class, INSTANCE);
        }
    }

    /**
     * Notifies this {@code LogHandler} that an operation is about to start on the given resource.
     * Call to this method must be followed by call to {@link #loadingStop(Long)} in a {@code finally} block.
     *
     * @param  source  the resource on which an operation is about to start in current thread.
     * @return key to use in call to {@link #loadingStop(Long)} when the operation is finished.
     */
    public static Long loadingStart(final Resource source) {
        final Long id = Thread.currentThread().getId();
        INSTANCE.inProgress.put(id, INSTANCE.getRecordsNonNull(source));
        return id;
    }

    /**
     * Notifies this {@code LogHandler} than an operation done on a resource is finished, either successfully or
     * with an exception thrown. Must be invoked in a {@code finally} block after {@link #loadingStart(Resource)}.
     *
     * @param  id  the value returned by {@link #loadingStart(Resource)}.
     */
    public static void loadingStop(final Long id) {
        INSTANCE.inProgress.remove(id);
    }

    /**
     * Returns the loggings related to the SIS library as a whole, not specific to any particular resources.
     * The returned list shall be read in JavaFX thread only.
     *
     * @return loggings related to the SIS library as a whole, not specific to any particular resources.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public static ObservableList<LogRecord> getSystemRecords() {
        return INSTANCE.systemLogs;
    }

    /**
     * Returns the list of log records for the given resource, or {@code null} if the given source is null.
     *
     * @param  source  the resource for which to get the list of log records, or {@code null}.
     * @return the records for the given resource, or {@code null} if the given source is null.
     */
    public static ObservableList<LogRecord> getRecords(final Resource source) {
        return (source != null) ? INSTANCE.getRecordsNonNull(source) : null;
    }

    /**
     * Returns the list of log records for the given resource.
     * The given resource shall not be null (the check is done by {@link ConcurrentHashMap}).
     *
     * @param  source  the resource for which to get the list of log records.
     * @return the records for the given resource.
     */
    private ObservableList<LogRecord> getRecordsNonNull(final Resource source) {
        synchronized (resourceLogs) {
            return resourceLogs.computeIfAbsent(source, (k) -> FXCollections.observableArrayList());
        }
    }

    /**
     * Invoked when a {@link DataStore} emitted a warning. This method adds the warning to the list
     * of log records specific to that resource. The record is not added to the global (system) list.
     *
     * @param  event  the warning event.
     */
    @Override
    public void eventOccured(final WarningEvent event) {
        final LogRecord log = event.getDescription();
        if (isLoggable(log)) {
            final ObservableList<LogRecord> records = getRecordsNonNull(event.getSource());
            if (Platform.isFxApplicationThread()) {
                records.add(log);
            } else {
                Platform.runLater(() -> records.add(log));
            }
        }
    }

    /**
     * Invoked when a log record is published by the {@link java.util.logging} system.
     * The log is added to the global (system) list, with oldest record potentially discarded.
     * In addition, if the log has been emitted in a thread monitored by {@link #inProgress},
     * then the log is also added to resource-specific log list.
     *
     * @param  log  the record to publish (may be {@code null}).
     */
    @Override
    public void publish(final LogRecord log) {
        if (isLoggable(log)) {
            // TODO: replace by log.getLongThreadId() with JDK16.
            final Long id = Thread.currentThread().getId();
            final ObservableList<LogRecord> records = inProgress.get(id);
            if (Platform.isFxApplicationThread()) {
                add(log, records);
            } else {
                Platform.runLater(() -> add(log, records));
            }
        }
    }

    /**
     * Adds the given log record to the global (system) list of logs and to the resource-specific
     * list of logs, if any.
     *
     * @param log      the log to add (must be non-null).
     * @param records  list of resource-specific logs, or {@code null} if none.
     */
    private void add(final LogRecord log, final ObservableList<LogRecord> records) {
        if (systemLogs.size() >= LIMIT) {
            systemLogs.remove(0);
        }
        systemLogs.add(log);
        if (records != null) {
            records.add(log);
        }
    }

    /**
     * No operation.
     */
    @Override
    public void flush() {
    }

    /**
     * Release resources. It is still possible to use this {@code LogHandler} after this method call,
     * but according {@link Handler#close()} documentation it should not be allowed.
     */
    @Override
    public void close() {
        synchronized (resourceLogs) {
            resourceLogs.clear();
        }
        inProgress.clear();
        // Do not clear `systemLogs` because it would need to be done in JavaFX thread.
    }
}
