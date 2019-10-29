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

import java.nio.file.Path;
import java.io.PrintWriter;
import java.io.StringWriter;
import javafx.concurrent.Worker;
import javafx.concurrent.WorkerStateEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextArea;
import org.apache.sis.util.Classes;


/**
 * A modal dialog box reporting an exception.
 * This dialog box contains an expandable section with the full stack trace.
 *
 * @author  Smaniotto Enzo
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final class ExceptionReporter {
    /**
     * Do not allow instantiation of this class.
     */
    private ExceptionReporter() {
    }

    /**
     * Shows the reporter for the exception that occurred during a task.
     *
     * @param  event  an event for the task where an error occurred.
     */
    public static void show(final WorkerStateEvent event) {
        final Worker<?> worker = event.getSource();
        final Throwable exception = worker.getException();
        if (worker instanceof TaskOnFile) {
            canNotReadFile(((TaskOnFile) worker).file, exception);
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
    public static void canNotReadFile(final Path file, final Throwable exception) {
        show(Resources.Keys.ErrorOpeningFile, Resources.Keys.CanNotReadFile_1,
                new Object[] {file.getFileName()}, exception);
    }

    /**
     * Constructs and shows the exception reporter. The title and text are keys from the {@link Resources}.
     * If the title and/or text are 0, then the {@link Alert} default title and text will be used.
     *
     * @param title       {@link Resources.Keys} of the title, or 0 if unknown.
     * @param text        {@link Resources.Keys} of the text (possibly with arguments), or 0 if unknown.
     * @param arguments   the arguments for creating the text identified by the {@code text} key.
     * @param exception   the exception to report.
     */
    private static void show(final short title, final short text, final Object[] arguments, final Throwable exception) {
        if (exception != null) {
            String message = exception.getLocalizedMessage();
            if (message == null) {
                message = Classes.getShortClassName(exception);
            }
            final Alert alert = new Alert(Alert.AlertType.ERROR);
            if ((title | text) != 0) {
                final Resources resources = Resources.getInstance();
                if (title != 0) {
                    alert.setTitle(resources.getString(title));
                }
                if (text != 0) {
                    alert.setHeaderText(resources.getString(text, arguments));
                }
            }
            alert.setContentText(message);
            /*
             * Format the stack trace to be shown in the expandable section.
             */
            final StringWriter buffer = new StringWriter();
            final PrintWriter  writer = new PrintWriter(buffer);
            exception.printStackTrace(writer);
            final TextArea trace = new TextArea(buffer.toString());
            trace.setPadding(Insets.EMPTY);
            trace.setPrefRowCount​(20);
            trace.setEditable(false);

            final DialogPane pane = alert.getDialogPane();
            pane.setExpandableContent(trace);
            pane.setPrefWidth(650);
            alert.show();
        }
    }
}
