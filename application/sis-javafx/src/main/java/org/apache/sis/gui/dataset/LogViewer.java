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

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.GridPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.apache.sis.gui.Widget;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.internal.gui.Styles;
import org.apache.sis.internal.gui.LogHandler;
import org.apache.sis.internal.gui.ExceptionReporter;
import org.apache.sis.internal.gui.ImmutableObjectProperty;
import org.apache.sis.util.CharSequences;


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
     * Localized string representations of {@link Level}.
     * This map shall be read and written from JavaFX thread only.
     *
     * @see #toString(Level)
     */
    private static final Map<Level,String> LEVEL_NAMES = new HashMap<>();

    /**
     * The table of log records.
     */
    private final TableView<LogRecord> table;

    /**
     * The view combining the table with details about the selected record.
     *
     * @see #getView()
     */
    private final SplitPane view;

    /**
     * Details about selected record.
     */
    private final Label level, time, logger, classe, method;

    /**
     * Area where to show the log message.
     */
    private final TextArea message;

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
    private final IsEmpty isEmpty;

    /**
     * Whether {@link #source} is modified in reaction to a {@link #systemLogs} change, or conversely.
     */
    private boolean isAdjusting;

    /**
     * The formatter for logging messages.
     */
    private final SimpleFormatter formatter;

    /**
     * Format for dates and times using a short or long representation. The short representation is for
     * a column in the table, and the long representation is for the details panel below the table.
     */
    private final DateFormat shortDates, longDates;

    /**
     * The button for showing the main message or the stack trace.
     */
    private final ToggleButton messageButton, traceButton;

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
        source     = new SimpleObjectProperty<>(this, "source");
        systemLogs = new SimpleBooleanProperty (this, "systemLogs");
        isEmpty    = new IsEmpty(this);
        formatter  = new SimpleFormatter();
        shortDates = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, vocabulary.getLocale());
        longDates  = DateFormat.getDateTimeInstance(DateFormat.LONG,  DateFormat.LONG,  vocabulary.getLocale());
        table      = new TableView<>(FXCollections.emptyObservableList());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setTableMenuButtonVisible(true);
        table.getColumns().setAll(column(vocabulary, Vocabulary.Keys.Level),
                                  column(vocabulary, Vocabulary.Keys.DateAndTime),
                                  column(vocabulary, Vocabulary.Keys.Logger),
                                  column(vocabulary, Vocabulary.Keys.Class),
                                  column(vocabulary, Vocabulary.Keys.Method),
                                  column(vocabulary, Vocabulary.Keys.Message));

        final Font font = Font.font(null, FontWeight.SEMI_BOLD, -1);
        final GridPane details = Styles.createControlGrid(0,
                label(font, vocabulary, Vocabulary.Keys.Level,       level  = new Label()),
                label(font, vocabulary, Vocabulary.Keys.DateAndTime, time   = new Label()),
                label(font, vocabulary, Vocabulary.Keys.Logger,      logger = new Label()),
                label(font, vocabulary, Vocabulary.Keys.Class,       classe = new Label()),
                label(font, vocabulary, Vocabulary.Keys.Method,      method = new Label()));

        messageButton = new ToggleButton(vocabulary.getString(Vocabulary.Keys.Message));
        traceButton   = new ToggleButton(vocabulary.getString(Vocabulary.Keys.Trace));
        messageButton.setSelected(true);
        messageButton.setMaxWidth(Double.MAX_VALUE);
        traceButton  .setMaxWidth(Double.MAX_VALUE);
        final ToggleGroup buttonGroup = new ToggleGroup();
        buttonGroup.getToggles().setAll(messageButton, traceButton);
        final VBox textSelector = new VBox(6, messageButton, traceButton);
        final Insets margin = new Insets(6, 0, 0, 0);

        message = new TextArea();
        message.setEditable(false);
        GridPane.setConstraints(textSelector, 0, 5);
        GridPane.setConstraints(message, 1, 5);
        GridPane.setMargin(textSelector, margin);
        GridPane.setMargin(message, margin);
        details.getChildren().addAll(textSelector, message);
        details.setVgap(0);

        view = new SplitPane(table, new TitledPane(vocabulary.getString(Vocabulary.Keys.Details), details));
        view.setOrientation(Orientation.VERTICAL);
        SplitPane.setResizableWithParent(details, false);

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
        final ReadOnlyObjectProperty<LogRecord> selected = table.getSelectionModel().selectedItemProperty();
        buttonGroup.selectedToggleProperty().addListener((p,o,n) -> setMessageOrTrace(selected.get()));
        selected.addListener((p,o,n) -> selected(n));
    }

    /**
     * Creates a column and register its cell factory.
     * This is a helper method for the constructor.
     */
    private TableColumn<LogRecord, String> column(final Vocabulary vocabulary, final short key) {
        final TableColumn<LogRecord, String> column = new TableColumn<>(vocabulary.getString(key));
        column.setCellValueFactory((cell) -> toString(cell, key));
        column.setVisible(key == Vocabulary.Keys.Message);
        return column;
    }

    /**
     * Creates a label of the "details" pane.
     * This is a helper method for the constructor.
     */
    private static Label label(final Font font, final Vocabulary vocabulary, final short key, final Label content) {
        final Label label = new Label(vocabulary.getLabel(key));
        label.setLabelFor(content);
        label.setFont(font);
        return label;
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
    private static final class IsEmpty extends ReadOnlyBooleanWrapper implements ListChangeListener<LogRecord> {
        /**
         * Creates the {@link LogViewer#isEmpty} property.
         */
        IsEmpty(final LogViewer owner) {
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
     * Returns the localized string representations of given {@link Level}.
     */
    private static String toString(final Level level) {
        if (level == null) {
            return null;
        }
        return LEVEL_NAMES.computeIfAbsent(level,
                (v) -> CharSequences.upperCaseToSentence(v.getLocalizedName()).toString());
    }

    /**
     * Returns the string representation of a logger property for the given cell.
     */
    private ObservableValue<String> toString(final CellDataFeatures<LogRecord,String> cell, final short type) {
        if (cell != null) {
            final LogRecord log = cell.getValue();
            if (log != null) {
                String text;
                switch (type) {
                    case Vocabulary.Keys.Level: {
                        text = toString(log.getLevel());
                        break;
                    }
                    case Vocabulary.Keys.DateAndTime: {
                        text = shortDates.format(new Date(log.getMillis()));
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
     * Invoked when a log record is selected.
     */
    private void selected(final LogRecord log) {
        String level = null, time = null, logger = null, classe = null, method = null;
        if (log != null) {
            level   = toString(log.getLevel());
            time    = longDates.format(new Date(log.getMillis()));
            logger  = log.getLoggerName();
            classe  = log.getSourceClassName();
            method  = log.getSourceMethodName();
            final boolean td = (log.getThrown() == null);
            traceButton.setDisable(td);
            if (td) {
                messageButton.setSelected(true);
            }
        }
        this.level  .setText(level);
        this.time   .setText(time);
        this.logger .setText(logger);
        this.classe .setText(classe);
        this.method .setText(method);
        setMessageOrTrace(log);
    }

    /**
     * Sets the text or the exception stack trace, depending which button is selected.
     */
    private void setMessageOrTrace(final LogRecord log) {
        String text = null;
        if (messageButton.isSelected()) {
            message.setWrapText(true);
            text = formatter.formatMessage(log);
        } else if (traceButton.isSelected()) {
            message.setWrapText(false);
            final Throwable exception = log.getThrown();
            if (exception != null) {
                text = ExceptionReporter.getStackTrace(exception);
            }
        }
        message.setText(text);
    }

    /**
     * Returns the control to show in the scene graph.
     * The implementation class may change in any future version.
     */
    @Override
    public Region getView() {
        return view;
    }
}
