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
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import javafx.scene.layout.Region;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.apache.sis.gui.Widget;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.internal.gui.LogHandler;
import org.apache.sis.internal.gui.ImmutableObjectProperty;


/**
 * Shows a table of recent log records, optionally filtered to logs related to a specific resource.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class LogViewer extends Widget {
    /**
     * The table of log records.
     */
    private final TableView<LogRecord> table;

    /**
     * The data store or resource for which to show log records.
     * If this property value is {@code null}, then the system logs will be shown
     * if {@link #systemLogs} is {@code true}, or no logs will be shown otherwise.
     */
    public final ObjectProperty<Resource> source;

    /**
     * Whether to show system logs instead then the logs related to a specific resource.
     * If this property is set to {@code true}, then {@link #source} is automatically set to {@code null}.
     * Conversely if {@link #source} is set to a non-null value, then this property is set to {@code false}.
     */
    public final BooleanProperty systemLogs;

    /**
     * Whether this viewer has no log record to show.
     *
     * @see #isEmptyProperty()
     */
    private final Listener isEmpty;

    /**
     * Whether {@link #source} is modified in reaction to a {@link #systemLogs} change, or conversely.
     */
    private boolean isAdjusting;

    /**
     * The formatter for logging messages.
     */
    private final SimpleFormatter formatter;

    /**
     * Creates an initially empty viewer of log records. For viewing logs, {@link #source}
     * must be set to a non-null value or {@link #systemLogs} must be set to {@code true}.
     */
    public LogViewer() {
        this(Vocabulary.getResources((Locale) null));
    }

    /**
     * Creates a new view of log records.
     */
    LogViewer(final Vocabulary vocabulary) {
        formatter  = new SimpleFormatter();
        source     = new SimpleObjectProperty<>(this, "source");
        systemLogs = new SimpleBooleanProperty (this, "systemLogs");
        isEmpty    = new Listener(this);
        table      = new TableView<>(FXCollections.emptyObservableList());

        final TableColumn<LogRecord, String> level   = new TableColumn<>(vocabulary.getString(Vocabulary.Keys.Level));
        final TableColumn<LogRecord, String> time    = new TableColumn<>(vocabulary.getString(Vocabulary.Keys.DateAndTime));
        final TableColumn<LogRecord, String> logger  = new TableColumn<>(vocabulary.getString(Vocabulary.Keys.Logger));
        final TableColumn<LogRecord, String> classe  = new TableColumn<>(vocabulary.getString(Vocabulary.Keys.Class));
        final TableColumn<LogRecord, String> method  = new TableColumn<>(vocabulary.getString(Vocabulary.Keys.Method));
        final TableColumn<LogRecord, String> message = new TableColumn<>(vocabulary.getString(Vocabulary.Keys.Message));

        level  .setCellValueFactory((cell) -> toString(cell, Vocabulary.Keys.Level));
        time   .setCellValueFactory((cell) -> toString(cell, Vocabulary.Keys.DateAndTime));
        logger .setCellValueFactory((cell) -> toString(cell, Vocabulary.Keys.Logger));
        classe .setCellValueFactory((cell) -> toString(cell, Vocabulary.Keys.Class));
        method .setCellValueFactory((cell) -> toString(cell, Vocabulary.Keys.Method));
        message.setCellValueFactory((cell) -> toString(cell, Vocabulary.Keys.Message));

        level .setVisible(false);
        time  .setVisible(false);
        logger.setVisible(false);
        classe.setVisible(false);
        method.setVisible(false);

        table.getColumns().setAll(level, time, logger, classe, method, message);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setTableMenuButtonVisible(true);

        source.addListener((p,o,n) -> {
            if (!isAdjusting) try {
                isAdjusting = true;
                systemLogs.set(false);
                setItems(LogHandler.getRecords(n));
            } finally {
                isAdjusting = false;
            }
        });
        systemLogs.addListener((p,o,n) -> {
            if (!isAdjusting) try {
                isAdjusting = true;
                source.set(null);
                setItems(n ? LogHandler.getSystemRecords() : FXCollections.emptyObservableList());
            } finally {
                isAdjusting = false;
            }
        });
    }

    /**
     * Sets a new list of log records.
     */
    private void setItems(final ObservableList<LogRecord> records) {
        final boolean e = records.isEmpty();
        table.setItems(records);
        isEmpty.set(e);
        if (e) {
            records.addListener(isEmpty);
        }
    }

    /**
     * Implementation of {@link LogViewer#isEmpty} property.
     * Also a listener for being notified when the property value needs to be changed.
     */
    private static final class Listener extends ReadOnlyBooleanWrapper implements ListChangeListener<LogRecord> {
        /**
         * Creates the {@link LogViewer#isEmpty} property.
         */
        Listener(final LogViewer owner) {
            super(owner, "isEmpty", true);
        }

        /**
         * Invoked when the list of records changed.
         */
        @Override public void onChanged(final Change<? extends LogRecord> change) {
            final ObservableList<? extends LogRecord> list = change.getList();
            if (!list.isEmpty()) {
                list.removeListener(this);
            }
            set(false);
        }
    }

    /**
     * Whether this viewer has no log record to show.
     * This property is useful for disabling or enabling a tab.
     *
     * @return the property telling whether this viewer no log record to show.
     */
    public final ReadOnlyBooleanProperty isEmptyProperty() {
        return isEmpty.getReadOnlyProperty();
    }

    /**
     * Returns the string representation of a logger property for the given cell.
     */
    private ObservableValue<String> toString(final CellDataFeatures<LogRecord,String> cell, final int type) {
        if (cell != null) {
            final LogRecord log = cell.getValue();
            if (log != null) {
                String text;
                switch (type) {
                    case Vocabulary.Keys.Level: {
                        text = log.getLevel().getLocalizedName();
                        break;
                    }
                    case Vocabulary.Keys.DateAndTime: {
                        text = log.getInstant().toString();
                        break;
                    }
                    case Vocabulary.Keys.Logger: {
                        text = log.getLoggerName();
                        break;
                    }
                    case Vocabulary.Keys.Class: {
                        text = log.getSourceClassName();
                        if (text != null) {
                            text = text.substring(text.lastIndexOf('.') + 1);
                        }
                        break;
                    }
                    case Vocabulary.Keys.Method: {
                        text = log.getSourceMethodName();
                        break;
                    }
                    case Vocabulary.Keys.Message: {
                        text = formatter.formatMessage(log);
                        break;
                    }
                    default: throw new AssertionError(type);
                }
                if (text != null) {
                    return new ImmutableObjectProperty<>(text);
                }
            }
        }
        return null;
    }

    /**
     * Returns the control to show in the scene graph.
     * The implementation class may change in any future version.
     */
    @Override
    public Region getView() {
        return table;
    }
}
