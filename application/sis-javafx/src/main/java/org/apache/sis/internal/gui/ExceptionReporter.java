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
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.stage.Window;
import javafx.scene.Node;
import javafx.scene.text.Text;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.apache.sis.gui.Widget;
import org.apache.sis.util.Classes;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.Resource;
import org.apache.sis.internal.storage.StoreResource;


/**
 * A modal dialog box reporting an exception.
 * This dialog box contains an expandable section with the full stack trace.
 *
 * @author  Smaniotto Enzo (GSoC)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.1
 * @module
 */
public final class ExceptionReporter extends Widget {
    /**
     * The margin around stack trace in side the scroll pane.
     * This is the space between the text and scroll bars.
     */
    private static final Insets MARGIN = new Insets(9, 3, 3, 9);

    /**
     * The margin around (outside) the scroll pane.
     */
    private static final Insets PADDING = new Insets(9, 3, 1, 3);

    /**
     * If an alert dialog box is already visible, the dialog. Otherwise {@code null}.
     * This is used for avoiding to popup many dialog boxes when many errors occur.
     * In current implementation, only the most recent exception is shown.
     * This field shall be read and written in JavaFX thread only.
     */
    private static Alert currentlyShown;

    /**
     * The exception that occurred.
     */
    private Throwable exception;

    /**
     * The component where to show the stack trace.
     * Contains (indirectly) the {@link #trace}.
     */
    private final VBox view;

    /**
     * The node where stack trace is written.
     */
    private final Text trace;

    /**
     * Creates a new exception reporter showing the stack trace of the given exception.
     *
     * @param  exception  the error to report.
     */
    public ExceptionReporter(final Throwable exception) {
        this.exception = exception;
        trace = new Text(getStackTrace(exception));
        final ScrollPane pane = new ScrollPane(trace);
        pane.setFitToWidth(true);
        pane.setFitToHeight(true);
        pane.setPadding(MARGIN);

        final Resources localized = Resources.getInstance();
        final Menu sendTo = new Menu(localized.getString(Resources.Keys.SendTo));
        sendTo.getItems().add(localized.menu(Resources.Keys.StandardErrorStream, this::printStackTrace));
        final ContextMenu menu = new ContextMenu(localized.menu(Resources.Keys.Copy, this::copy), sendTo);
        pane.setContextMenu(menu);

        final Label header = new Label(localized.getString(Resources.Keys.ErrorAt));
        view = new VBox(header, pane);
        VBox.setVgrow(pane, Priority.ALWAYS);
        VBox.setMargin(pane, PADDING);
        VBox.setMargin(header, PADDING);
        view.setPrefHeight(400);
    }

    /**
     * Updates the content of this reporter with a new stack trace.
     *
     * @param  exception  the new error to report.
     */
    public void setException(final Throwable exception) {
        trace.setText(getStackTrace(exception));
        this.exception = exception;
    }

    /**
     * Gets the stack trace of the given exception.
     *
     * @param  exception  the exception for which to get the stack trace.
     * @return the stack trace.
     */
    public static String getStackTrace(final Throwable exception) {
        final StringWriter buffer = new StringWriter();
        exception.printStackTrace(new PrintWriter(buffer));
        return buffer.toString();
    }

    /**
     * Returns the control to insert in a scene for showing the stack trace.
     *
     * @return the stack trace viewer.
     */
    @Override
    public final Region getView() {
        return view;
    }

    /**
     * Shows the reporter for the exception that occurred during a task.
     *
     * @param  owner  control in the window which will own the dialog, or {@code null} if unknown.
     * @param  event  an event for the task where an error occurred.
     */
    public static void show(final Node owner, final WorkerStateEvent event) {
        final Worker<?> worker = event.getSource();
        final Throwable exception = worker.getException();
        if (worker instanceof DataStoreOpener) {
            canNotReadFile(owner, ((DataStoreOpener) worker).getFileName(), exception);
        } else {
            show(GUIUtilities.getWindow(owner), (short) 0, (short) 0, null, exception);
        }
    }

    /**
     * Shows the reporter for a failure to read a file.
     * This method does nothing if the exception is null.
     *
     * @param  owner      control in the window which will own the dialog, or {@code null} if unknown.
     * @param  resource   the resource that can not be read.
     * @param  exception  the error that occurred.
     */
    public static void canNotReadFile(final Node owner, Resource resource, final Throwable exception) {
        String name = null;
        if (resource instanceof StoreResource) {
            final DataStore ds = ((StoreResource) resource).getOriginator();
            if (ds != null) name = ds.getDisplayName();
        }
        if (name == null && resource instanceof DataStore) {
            name = ((DataStore) resource).getDisplayName();
        }
        if (name == null) {
            canNotUseResource(owner, exception);
            return;
        }
        canNotReadFile(owner, name, exception);
    }

    /**
     * Shows the reporter for a failure to read a file.
     * This method does nothing if the exception is null.
     *
     * @param  owner      control in the window which will own the dialog, or {@code null} if unknown.
     * @param  file       the file that can not be read.
     * @param  exception  the error that occurred.
     */
    public static void canNotReadFile(final Node owner, final String file, final Throwable exception) {
        show(GUIUtilities.getWindow(owner), Resources.Keys.ErrorOpeningFile, Resources.Keys.CanNotReadFile_1,
                new Object[] {file}, exception);
    }

    /**
     * Shows the reporter for a failure to close a file.
     * This method does nothing if the exception is null.
     *
     * @param  owner      control in the window which will own the dialog, or {@code null} if unknown.
     * @param  file       the file that can not be closed.
     * @param  exception  the error that occurred.
     */
    public static void canNotCloseFile(final Node owner, final String file, final Throwable exception) {
        show(GUIUtilities.getWindow(owner), Resources.Keys.ErrorClosingFile, Resources.Keys.CanNotClose_1,
                new Object[] {file}, exception);
    }

    /**
     * Shows the reporter for a failure to create a CRS.
     * This method does nothing if the exception is null.
     *
     * @param  owner      the owner window of the dialog, or {@code null} if none.
     * @param  code       code of the CRS that can not be created.
     * @param  exception  the error that occurred.
     */
    public static void canNotCreateCRS(final Window owner, final String code, final Throwable exception) {
        show(owner, Resources.Keys.ErrorCreatingCRS, Resources.Keys.CanNotCreateCRS_1,
                new Object[] {code}, exception);
    }

    /**
     * Shows the reporter for a failure to use a store resource.
     * This method does nothing if the exception is null.
     *
     * @param  owner      control in the window which will own the dialog, or {@code null} if unknown.
     * @param  exception  the error that occurred.
     */
    public static void canNotUseResource(final Node owner, final Throwable exception) {
        show(GUIUtilities.getWindow(owner), Resources.Keys.ErrorDataAccess, Resources.Keys.ErrorDataAccess,
                new Object[0], exception);
    }

    /**
     * Constructs and shows the exception reporter. The title and text are keys from the {@link Resources}.
     * If the title and/or text are 0, then the {@link Alert} default title and text will be used.
     *
     * @param owner       the owner window of the dialog, or {@code null} if none.
     * @param title       {@link Resources.Keys} of the title, or 0 if unknown.
     * @param text        {@link Resources.Keys} of the text (possibly with arguments), or 0 if unknown.
     * @param arguments   the arguments for creating the text identified by the {@code text} key.
     * @param exception   the exception to report, or {@code null} if none.
     */
    private static void show(final Window owner, final short title,
            final short text, final Object[] arguments, final Throwable exception)
    {
        if (exception != null) {
            String t = null, h = null;
            if ((title | text) != 0) {
                final Resources resources = Resources.getInstance();
                if (title != 0) t = resources.getString(title);
                if (text  != 0) h = resources.getString(text, arguments);
            }
            show(owner, t, h, exception);
        }
    }

    /**
     * Constructs and shows the exception reporter.
     * This method can be invoked from any thread.
     *
     * @param owner      control in the window which will own the dialog, or {@code null} if unknown.
     * @param title      the window title, or {@code null} if none.
     * @param text       the text in the dialog box, or {@code null} if none.
     * @param exception  the exception to report.
     */
    public static void show(final Node owner, final String title, final String text, final Throwable exception) {
        show(GUIUtilities.getWindow(owner), title, text, exception);
    }

    /**
     * Constructs and shows the exception reporter.
     * This method can be invoked from any thread.
     * All other {@code show(…)} methods in this class ultimately delegate to this method.
     *
     * @param owner      the owner window of the dialog, or {@code null} if none.
     * @param title      the window title, or {@code null} if none.
     * @param text       the text in the dialog box, or {@code null} if none.
     * @param exception  the exception to report.
     */
    public static void show(final Window owner, final String title, final String text, final Throwable exception) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> show(owner, title, text, exception));
            return;
        }
        String message = exception.getLocalizedMessage();
        if (message == null) {
            message = Classes.getShortClassName(exception);
        }
        Alert alert = currentlyShown;
        if (alert == null) {
            alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(owner);
            alert.setOnHidden((event) -> currentlyShown = null);
            final ExceptionReporter content = new ExceptionReporter(exception);
            final DialogPane pane = alert.getDialogPane();
            pane.setExpandableContent(content.getView());
            pane.setPrefWidth(650);
            pane.setUserData(content);
        } else {
            final ExceptionReporter content = (ExceptionReporter) alert.getDialogPane().getUserData();
            content.setException(exception);
        }
        if (title != null) alert.setTitle(title);
        if (text  != null) alert.setHeaderText(text);
        alert.setContentText(message);
        alert.show();
        currentlyShown = alert;
    }

    /**
     * Constructs and shows the exception reporter for the given task.
     *
     * @param  owner  control in the window which will own the dialog, or {@code null} if unknown.
     * @param  task   the task that failed.
     */
    public static void show(final Node owner, final Task<?> task) {
        show(owner, task.getTitle(), null, task.getException());
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
