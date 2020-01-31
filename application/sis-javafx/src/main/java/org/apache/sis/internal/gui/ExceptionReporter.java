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

import java.io.PrintWriter;
import java.io.StringWriter;
import javafx.concurrent.Worker;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Region;
import org.apache.sis.util.Classes;


/**
 * A modal dialog box reporting an exception.
 * This dialog box contains an expandable section with the full stack trace.
 *
 * @todo If another error happen in a background thread while this dialog is visible,
 *       add the error in the existing dialog instead than opening a new one.
 *
 * @author  Smaniotto Enzo (GSoC)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final class ExceptionReporter {
    /**
     * The margin to use for the stack trace. We add some spaces on the top and left sides.
     */
    private static final Insets MARGIN = new Insets(9, 0, 0, 40);

    /**
     * The exception that occurred.
     */
    private final Throwable exception;

    /**
     * The component where to show the stack trace.
     */
    private final Label trace;

    /**
     * Creates a new exception reporter showing the stack trace of the given exception.
     *
     * @param  exception  the error to report.
     */
    @SuppressWarnings("ThisEscapedInObjectConstruction")
    public ExceptionReporter(final Throwable exception) {
        this.exception = exception;
        trace = new Label(getStackTrace(exception));
        trace.setAlignment(Pos.TOP_LEFT);
        trace.setPadding(MARGIN);

        final Resources localized = Resources.getInstance();
        final Menu sendTo = new Menu(localized.getString(Resources.Keys.SendTo));
        sendTo.getItems().add(localized.menu(Resources.Keys.StandardErrorStream, this::printStackTrace));
        final ContextMenu menu = new ContextMenu(localized.menu(Resources.Keys.Copy, this::copy), sendTo);
        trace.setContextMenu(menu);
    }

    /**
     * Updates the content of this reporter with a new stack trace.
     *
     * @param  exception  the new error to report.
     */
    public void setException(final Throwable exception) {
        trace.setText(getStackTrace(exception));
    }

    /**
     * Gets the stack trace of the given exception.
     */
    private static String getStackTrace(final Throwable exception) {
        final StringWriter buffer = new StringWriter();
        final PrintWriter  writer = new PrintWriter(buffer);
        exception.printStackTrace(writer);
        return buffer.toString();
    }

    /**
     * Returns the control to insert in a scene for showing the stack trace.
     *
     * @return the stack trace viewer.
     */
    public final Region getView() {
        return trace;
    }

    /**
     * Shows the reporter for the exception that occurred during a task.
     *
     * @param  event  an event for the task where an error occurred.
     */
    public static void show(final WorkerStateEvent event) {
        final Worker<?> worker = event.getSource();
        final Throwable exception = worker.getException();
        if (worker instanceof ResourceLoader) {
            canNotReadFile(((ResourceLoader) worker).getFileName(), exception);
        } else {
            show((short) 0, (short) 0, null, exception);
        }
    }

    /**
     * Shows the reporter for a failure to read a file.
     * This method does nothing if the exception is null.
     *
     * @param  file       the file that can not be read.
     * @param  exception  the error that occurred.
     */
    public static void canNotReadFile(final String file, final Throwable exception) {
        show(Resources.Keys.ErrorOpeningFile, Resources.Keys.CanNotReadFile_1, new Object[] {file}, exception);
    }

    /**
     * Shows the reporter for a failure to close a file.
     * This method does nothing if the exception is null.
     *
     * @param  file       the file that can not be closed.
     * @param  exception  the error that occurred.
     */
    public static void canNotCloseFile(final String file, final Throwable exception) {
        show(Resources.Keys.ErrorClosingFile, Resources.Keys.CanNotClose_1, new Object[] {file}, exception);
    }

    /**
     * Shows the reporter for a failure to create a CRS.
     * This method does nothing if the exception is null.
     *
     * @param  code       code of the CRS that can not be created.
     * @param  exception  the error that occurred.
     */
    public static void canNotCreateCRS(final String code, final Throwable exception) {
        show(Resources.Keys.ErrorCreatingCRS, Resources.Keys.CanNotCreateCRS_1, new Object[] {code}, exception);
    }

    /**
     * Shows the reporter for a failure to use a store resource.
     * This method does nothing if the exception is null.
     *
     * @param  exception  the error that occurred.
     */
    public static void canNotUseResource(final Throwable exception) {
        show(Resources.Keys.ErrorDataAccess, Resources.Keys.ErrorDataAccess, new Object[0], exception);
    }

    /**
     * Constructs and shows the exception reporter. The title and text are keys from the {@link Resources}.
     * If the title and/or text are 0, then the {@link Alert} default title and text will be used.
     *
     * @param title       {@link Resources.Keys} of the title, or 0 if unknown.
     * @param text        {@link Resources.Keys} of the text (possibly with arguments), or 0 if unknown.
     * @param arguments   the arguments for creating the text identified by the {@code text} key.
     * @param exception   the exception to report, or {@code null} if none.
     */
    private static void show(final short title, final short text, final Object[] arguments, final Throwable exception) {
        if (exception != null) {
            String t = null, h = null;
            if ((title | text) != 0) {
                final Resources resources = Resources.getInstance();
                if (title != 0) t = resources.getString(title);
                if (text  != 0) h = resources.getString(text, arguments);
            }
            show(t, h, exception);
        }
    }

    /**
     * Constructs and shows the exception reporter.
     *
     * @param title      the window the title, or {@code null} if none.
     * @param text       the text in the dialog box, or {@code null} if none.
     * @param exception  the exception to report.
     */
    public static void show(final String title, final String text, final Throwable exception) {
        String message = exception.getLocalizedMessage();
        if (message == null) {
            message = Classes.getShortClassName(exception);
        }
        final Alert alert = new Alert(Alert.AlertType.ERROR);
        if (title != null) alert.setTitle(title);
        if (text  != null) alert.setHeaderText(text);
        alert.setContentText(message);
        final DialogPane pane = alert.getDialogPane();
        pane.setExpandableContent(new ExceptionReporter(exception).trace);
        pane.setPrefWidth(650);
        alert.show();
    }

    /**
     * Invoked when the user selected the "Copy" action in contextual menu.
     *
     * @param event ignored.
     */
    private void copy(final ActionEvent event) {
        final ClipboardContent content = new ClipboardContent();
        content.putString(trace.getText());
        Clipboard.getSystemClipboard().setContent(content);
    }

    /**
     * Invoked when the user selected the "Send to ⏵ Standard error stream" action in contextual menu.
     *
     * @param event ignored.
     */
    @SuppressWarnings("CallToPrintStackTrace")
    private void printStackTrace(final ActionEvent event) {
        exception.printStackTrace();
    }
}
