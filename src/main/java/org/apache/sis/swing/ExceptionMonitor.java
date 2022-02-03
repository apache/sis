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
package org.apache.sis.swing;

import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Window;
import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JTextArea;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.AbstractButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.EventQueue;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.apache.sis.util.Classes;
import org.apache.sis.io.LineAppender;
import org.apache.sis.swing.internal.Resources;


/**
 * Dialog box for exception messages and eventually their traces.
 * The message will appear in a dialog box or in an internal window, depending on the parent.
 * <strong>Note:</strong> All methods in this class must be invoked in the same thread as the
 * <cite>Swing</cite> thread.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.1
 * @since   1.1
 */
@SuppressWarnings("serial")
final class ExceptionMonitor extends JOptionPane implements ActionListener {
    /**
     * Number of spaces to leave between each tab.
     */
    private static final int TAB_WIDTH = 4;

    /**
     * Width and height (in pixels) of the dialog box when it also shows the stack trace.
     */
    private static final int WIDTH = 600, HEIGHT = 400;

    /**
     * Dialog box made visible. This is an instance of {@link JDialog} or {@link JInternalFrame}.
     */
    private final Component dialog;

    /**
     * Exception to show in the dialog box. The {@link Throwable#getLocalizedMessage()} method
     * will be invoked to obtain the message to show.
     */
    private final Throwable exception;

    /**
     * Box which will contain the "message" part of the constructed dialog box.
     * This box will be expanded if the user asks to see the exception trace.
     * It will arrange the components using {@link BorderLayout}.
     */
    private final Container message;

    /**
     * Component showing the exception stack trace. Initially null.
     * Will be created if the stack trace is requested by the user.
     */
    private Container trace;

    /**
     * Indicates whether the trace is currently visible. This field value
     * will be inverted each time the user presses the button "trace".
     */
    private boolean traceVisible;

    /**
     * Button which makes the trace appear or disappear.
     */
    private final AbstractButton traceButton;

    /**
     * Initial size of the dialog box {@link #dialog}. This information will be used to
     * return the box to its initial size when the trace disappears.
     */
    private final Dimension initialSize;

    /**
     * Resources in the user's language.
     */
    private final Resources resources;

    /**
     * Constructs a pane which will show the specified error message.
     *
     * @param owner      parent Component of the dialog box to be created.
     * @param exception  exception we want to report.
     * @param message    message to show.
     * @param buttons    buttons to place under the message. These buttons should be in the order "Debug", "Close".
     * @param resources  resources in the user's language.
     */
    @SuppressWarnings("ThisEscapedInObjectConstruction")
    private ExceptionMonitor(final Component owner,   final Throwable exception,
                             final Container message, final AbstractButton[] buttons,
                             final Resources resources)
    {
        super(message, ERROR_MESSAGE, OK_CANCEL_OPTION, null, buttons);
        this.exception   = exception;
        this.message     = message;
        this.resources   = resources;
        this.traceButton = buttons[0];
        buttons[0].addActionListener(this);
        buttons[1].addActionListener(this);
        /*
         * Constructs the dialog box.  Automatically detects if we can use InternalFrame or if
         * we should be happy with JDialog. The exception trace will not be written immediately.
         */
        final String classname = Classes.getShortClassName(exception);
        final String title = resources.getLabel(Resources.Keys.Error) + ' ' + classname;
        final JDesktopPane desktop = getDesktopPaneForComponent(owner);
        if (desktop != null) {
            final JInternalFrame dialog = createInternalFrame(desktop, title);
            desktop.setLayer(dialog, JDesktopPane.MODAL_LAYER);
            dialog.setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);
            dialog.setResizable(false);
            dialog.pack();
            this.dialog = dialog;
        } else {
            final JDialog dialog = createDialog(owner, title);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setResizable(false);
            dialog.pack();
            this.dialog = dialog;
        }
        initialSize = dialog.getSize();
    }

    /**
     * Shows a dialog box which notifies the user that an exception has been produced.
     * This method should be invoked in the same thread as the Swing thread.
     */
    private static void showUnsafe(final Component owner, final Throwable exception, String message) {
        final Resources resources = Resources.forLocale((owner != null) ? owner.getLocale() : null);
        if (message == null) {
            message = exception.getLocalizedMessage();
            if (message == null) {
                final String classname = Classes.getShortClassName(exception);
                message = resources.getString(Resources.Keys.NoDetails_1, classname);
            }
        }
        final JTextArea textArea = new JTextArea(message);
        textArea.setLineWrap(true);
        final JComponent messageBox = new JPanel(new BorderLayout());
        messageBox.add(textArea, BorderLayout.NORTH);
        final ExceptionMonitor pane = new ExceptionMonitor(owner, exception, messageBox, new AbstractButton[] {
                new JButton(resources.getString(Resources.Keys.Debug)),
                new JButton(resources.getString(Resources.Keys.Close))
        }, resources);
        pane.dialog.setVisible(true);
    }

    /**
     * Shows an error message for the specified exception. Note that this method can
     * be invoked from any thread (not necessarily the <cite>Swing</cite> thread).
     *
     * @param  owner      component in which the exception occurred, or {@code null} if unknown.
     * @param  exception  exception which has been thrown and is to be reported to the user.
     * @param  message    message to show. If this parameter is null, then the message will be provided by
     *                    {@link Exception#getLocalizedMessage()}.
     */
    public static void show(final Component owner, final Throwable exception, final String message) {
        if (EventQueue.isDispatchThread()) {
            showUnsafe(owner, exception, message);
        } else {
            SwingUtilities.invokeAndWait(() -> showUnsafe(owner, exception, message));
        }
    }

    /**
     * Shows an error message for the specified exception. Note that this method can
     * be invoked from any thread (not necessarily the <cite>Swing</cite> thread).
     *
     * @param  owner      component in which the exception occurred, or {@code null} if unknown.
     * @param  exception  exception which has been thrown and is to be reported to the user.
     */
    public static void show(final Component owner, final Throwable exception) {
        show(owner, exception, null);
    }

    /**
     * Shows the exception stack trace below the message. This method is invoked when the dialog
     * box "Debug" button is pressed. If the exception trace still has not been written yet,
     * this method will construct the necessary components.
     *
     * @param  event  the event.
     */
    @Override
    public void actionPerformed(final ActionEvent event) {
        if (event.getSource() != traceButton) {
            dispose();
            return;
        }
        /*
         * Constructs the exception trace if it hasn't already been constructed.
         */
        if (trace == null) {
            JComponent traceComponent = null;
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                final JTextArea text = new JTextArea();
                text.setTabSize(4);
                text.setText(formatStackTrace(cause));
                text.setEditable(false);
                text.setCaretPosition(0);
                final JScrollPane scroll = new JScrollPane(text);
                if (traceComponent != null) {
                    if (!(traceComponent instanceof JTabbedPane)) {
                        traceComponent.setOpaque(false);
                        String classname = Classes.getShortClassName(exception);
                        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
                        tabs.addTab(classname, traceComponent);
                        traceComponent = tabs;
                    }
                    String classname = Classes.getShortClassName(cause);
                    ((JTabbedPane) traceComponent).addTab(classname, scroll);
                } else {
                    traceComponent = scroll;
                }
            }
            if (traceComponent == null) {
                // Should not happen
                return;
            }
            trace = Box.createVerticalBox();
            trace.add(Box.createVerticalStrut(12));
            trace.add(traceComponent);
        }
        /*
         * Inserts or hides the exception trace. Even if the trace is hidden,
         * it will not be destroyed in case the user want to show it again.
         */
        traceButton.setText(resources.getString(traceVisible ? Resources.Keys.Debug : Resources.Keys.Hide));
        traceVisible = !traceVisible;
        if (dialog instanceof Dialog) {
            ((Dialog) dialog).setResizable(traceVisible);
        } else {
            ((JInternalFrame) dialog).setResizable(traceVisible);
        }
        int dx = dialog.getWidth();
        int dy = dialog.getHeight();
        if (traceVisible) {
            message.add(trace, BorderLayout.CENTER);
            dialog.setSize(WIDTH, HEIGHT);
        } else {
            message.remove(trace);
            dialog.setSize(initialSize);
        }
        dx -= dialog.getWidth();
        dy -= dialog.getHeight();
        dialog.setLocation(Math.max(0, dialog.getX() + dx/2),
                           Math.max(0, dialog.getY() + dy/2));
        dialog.validate();
    }

    /**
     * Returns the exception trace as a string. This method gets the stack trace using the
     * {@link Throwable#printStackTrace(PrintWriter)} method, then replaces the tabulation
     * characters by 4 white spaces.
     *
     * @param  exception  the exception to format.
     * @return a string representation of the given exception.
     */
    private static String formatStackTrace(final Throwable exception) {
        final StringWriter writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));
        final StringBuilder buffer = new StringBuilder();
        final LineAppender formatter = new LineAppender(buffer);
        formatter.setTabulationWidth(TAB_WIDTH);
        try {
            formatter.append(writer.toString());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        return buffer.toString();
    }

    /**
     * Frees up the resources used by this dialog box. This method is invoked when the
     * user closes the dialog box which reported the exception.
     */
    private void dispose() {
        if (dialog instanceof Window) {
            ((Window) dialog).dispose();
        } else {
            ((JInternalFrame) dialog).dispose();
        }
    }
}
