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

import java.util.Arrays;
import java.util.Locale;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.SystemColor;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.table.TableColumn;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import org.apache.sis.swing.internal.Resources;
import org.apache.sis.util.Static;


/**
 * A collection of utility methods for Swing. Every {@code show*} methods delegate
 * their work to the corresponding method in {@link JOptionPane}, with two differences:
 *
 * <ul>
 *   <li>{@code SwingUtilities}'s method may be invoked from any thread.
 *       If they are invoked from a non-Swing thread, execution will be delegate
 *       to the Swing thread and the calling thread will block until completion.</li>
 *   <li>If a parent component is a {@link javax.swing.JDesktopPane},
 *       dialog will be rendered as internal frames instead of frames.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.1
 * @since   1.1
 */
final class SwingUtilities extends Static {
    /**
     * Do not allow any instance of this class to be created.
     */
    private SwingUtilities() {
    }

    /**
     * Shows the given component in a {@link JFrame}.
     * This is used (indirectly) mostly for debugging purpose.
     *
     * @param  panel  the panel to show.
     * @param  title  the frame title.
     */
    public static void show(final JComponent panel, final String title) {
        final JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * Brings up a "Ok/Cancel" dialog with no icon, using the installed window handler.
     * In the default configuration, this will result in a call to the
     * {@link #showOptionDialog(Component, Object, String)} method just below.
     *
     * @param  owner   the parent component. Dialog will apears on top of this owner.
     * @param  dialog  the dialog content to show.
     * @param  title   the title string for the dialog.
     * @return {@code true} if user clicked "Ok", {@code false} otherwise.
     */
    public static boolean showDialog(final Component owner, final Component dialog, final String title) {
        final WindowCreator.Handler handler;
        if (owner instanceof WindowCreator) {
            handler = ((WindowCreator) owner).getWindowHandler();
        } else {
            handler = WindowCreator.getDefaultWindowHandler();
        }
        return handler.showDialog(owner, dialog, title);
    }

    /**
     * Brings up a "Ok/Cancel" dialog with no icon. This method can be invoked
     * from any thread and blocks until the user click on "Ok" or "Cancel".
     *
     * @param  owner   the parent component. Dialog will apears on top of this owner.
     * @param  dialog  the dialog content to show.
     * @param  title   the title string for the dialog.
     * @return {@code true} if user clicked "Ok", {@code false} otherwise.
     */
    public static boolean showOptionDialog(final Component owner, final Object dialog, final String title) {
        return showOptionDialog(owner, dialog, title, null);
    }

    /**
     * Brings up a "Ok/Cancel/Reset" dialog with no icon. This method can be invoked
     * from any thread and blocks until the user click on "Ok" or "Cancel".
     *
     * @param  owner   the parent component. Dialog will apears on top of this owner.
     * @param  dialog  the dialog content to show.
     * @param  title   the title string for the dialog.
     * @param  reset   action to execute when user press "Reset", or {@code null} if there is no "Reset" button.
     *                 If {@code reset} is an instance of {@link Action}, the button label will be set according
     *                 the action's properties.
     * @return {@code true} if user clicked "Ok", {@code false} otherwise.
     */
    public static boolean showOptionDialog(final Component owner, final Object dialog, final String title, final ActionListener reset) {
        /*
         * Delegates to Swing thread if this method is invoked from an other thread.
         */
        if (!EventQueue.isDispatchThread()) {
            final boolean[] result = new boolean[1];
            invokeAndWait(() -> result[0] = showOptionDialog(owner, dialog, title, reset));
            return result[0];
        }
        /*
         * Constructs the buttons bar.
         */
        Object[]    options = null;
        Object initialValue = null;
        int okChoice = JOptionPane.OK_OPTION;
        if (reset != null) {
            final Resources resources = Resources.forLocale(owner!=null ? owner.getLocale() : null);
            final JButton button;
            if (reset instanceof Action) {
                button = new JButton((Action)reset);
            } else {
                button = new JButton(resources.getString(Resources.Keys.Reset));
                button.addActionListener(reset);
            }
            options = new Object[] {
                resources.getString(Resources.Keys.Ok),
                resources.getString(Resources.Keys.Cancel),
                button
            };
            initialValue = options[okChoice=0];
        }
        /*
         * Brings ups the dialog box.
         */
        final int choice;
        if (JOptionPane.getDesktopPaneForComponent(owner)!=null) {
            choice = JOptionPane.showInternalOptionDialog(
                    owner,                         // Parent component
                    dialog,                        // Message
                    title,                         // Title of dialog box
                    JOptionPane.OK_CANCEL_OPTION,  // Button to shown
                    JOptionPane.PLAIN_MESSAGE,     // Message type
                    null,                          // Icon
                    options,                       // Button list
                    initialValue);                 // Default button
        } else {
            choice = JOptionPane.showOptionDialog(
                    owner,                         // Parent component
                    dialog,                        // Message
                    title,                         // Title of dialog box
                    JOptionPane.OK_CANCEL_OPTION,  // Button to shown
                    JOptionPane.PLAIN_MESSAGE,     // Message type
                    null,                          // Icon
                    options,                       // Button list
                    initialValue);                 // Default button
        }
        return choice == okChoice;
    }

    /**
     * Brings up a message dialog with a "Ok" button. This method can be invoked
     * from any thread and blocks until the user click on "Ok".
     *
     * @param  owner    the parent component. Dialog will apears on top of this owner.
     * @param  message  the dialog content to show.
     * @param  title    the title string for the dialog.
     * @param  type     the message type
     *                ({@link JOptionPane#ERROR_MESSAGE},
     *                 {@link JOptionPane#INFORMATION_MESSAGE},
     *                 {@link JOptionPane#WARNING_MESSAGE},
     *                 {@link JOptionPane#QUESTION_MESSAGE} or
     *                 {@link JOptionPane#PLAIN_MESSAGE}).
     */
    public static void showMessageDialog(final Component owner, final Object message, final String title, final int type) {
        if (!EventQueue.isDispatchThread()) {
            invokeAndWait(() -> showMessageDialog(owner, message, title, type));
            return;
        }
        if (JOptionPane.getDesktopPaneForComponent(owner)!=null) {
            JOptionPane.showInternalMessageDialog(
                    owner,     // Parent component
                    message,   // Message
                    title,     // Title of dialog box
                    type);     // Message type
        } else {
            JOptionPane.showMessageDialog(
                    owner,     // Parent component
                    message,   // Message
                    title,     // Title of dialog box
                    type);     // Message type
        }
    }

    /**
     * Brings up a confirmation dialog with "Yes/No" buttons. This method can be
     * invoked from any thread and blocks until the user click on "Yes" or "No".
     *
     * @param  owner    the parent component. Dialog will apears on top of this owner.
     * @param  message  the dialog content to show.
     * @param  title    the title string for the dialog.
     * @param  type     the message type
     *                ({@link JOptionPane#ERROR_MESSAGE},
     *                 {@link JOptionPane#INFORMATION_MESSAGE},
     *                 {@link JOptionPane#WARNING_MESSAGE},
     *                 {@link JOptionPane#QUESTION_MESSAGE} or
     *                 {@link JOptionPane#PLAIN_MESSAGE}).
     * @return {@code true} if user clicked on "Yes", {@code false} otherwise.
     */
    public static boolean showConfirmDialog(final Component owner, final Object message, final String title, final int type) {
        if (!EventQueue.isDispatchThread()) {
            final boolean[] result = new boolean[1];
            invokeAndWait(() -> result[0] = showConfirmDialog(owner, message, title, type));
            return result[0];
        }
        final int choice;
        if (JOptionPane.getDesktopPaneForComponent(owner)!=null) {
            choice = JOptionPane.showInternalConfirmDialog(
                    owner,                           // Parent component
                    message,                         // Message
                    title,                           // Title of dialog box
                    JOptionPane.YES_NO_OPTION,       // Button to shown
                    type);                           // Message type
        } else {
            choice = JOptionPane.showConfirmDialog(
                    owner,                           // Parent component
                    message,                         // Message
                    title,                           // Title of dialog box
                    JOptionPane.YES_NO_OPTION,       // Button to shown
                    type);                           // Message type
        }
        return choice == JOptionPane.YES_OPTION;
    }

    /**
     * Setups the given table for usage as row-header.
     * This method setups the background color to the same one than the column headers.
     *
     * <div class="note"><b>Note:</b> in a previous version, we were assigning to the row headers
     * the same cell renderer than the one created by <cite>Swing</cite> for the column headers.
     * But it produced strange effects when the L&amp;F uses a vertical gradient instead than a uniform color.</div>
     *
     * @param  table  the table to setup as row headers.
     * @return the renderer which has been assigned to the table.
     */
    public static TableCellRenderer setupAsRowHeader(final JTable table) {
        final JTableHeader header = table.getTableHeader();
        Color background = header.getBackground();
        Color foreground = header.getForeground();
        if (background == null || background.equals(table.getBackground())) {
            if (!SystemColor.control.equals(background)) {
                background = SystemColor.control;
                foreground = SystemColor.controlText;
            } else {
                final Locale locale = table.getLocale();
                background = UIManager.getColor("Label.background", locale);
                foreground = UIManager.getColor("Label.foreground", locale);
            }
        }
        final DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setBackground(background);
        renderer.setForeground(foreground);
        renderer.setHorizontalAlignment(DefaultTableCellRenderer.RIGHT);
        final TableColumn column = table.getColumnModel().getColumn(0);
        column.setCellRenderer(renderer);
        column.setPreferredWidth(60);
        table.setPreferredScrollableViewportSize(table.getPreferredSize());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setCellSelectionEnabled(false);
        return renderer;
    }

    /**
     * Removes the given elements from the given list. This method tries to use
     * {@link DefaultListModel#removeRange} when possible in order to group events together.
     *
     * <p><strong>Warning:</strong> this method override the given {@code indices} array.</p>
     *
     * @param  list     the list from which to remove elements.
     * @param  indices  the index of elements to remove.
     */
    public static void remove(final DefaultListModel<?> list, final int[] indices) {
        /*
         * We must iterate in reverse order, because the
         * index after the removed elements will change.
         */
        int i = indices.length;
        if (i != 0) {
            Arrays.sort(indices);
            int upper = indices[--i];
            int lower = upper;
            while (i != 0) {
                int previous = indices[--i];
                if (previous != lower - 1) {
                    if (lower == upper) {
                        list.remove(lower);
                    } else {
                        list.removeRange(lower, upper);
                    }
                    upper = previous;
                }
                lower = previous;
            }
            if (lower == upper) {
                list.remove(lower);
            } else {
                list.removeRange(lower, upper);
            }
        }
    }

    /**
     * Adds the given listener to the first window ancestor found in the hierarchy.
     * If an {@link JInternalFrame} is found in the hierarchy, it the listener will
     * be wrapped in an adapter before to be given to that internal frame.
     *
     * @param  component  the component for which to search for a window ancestor.
     * @param  listener   the listener to register.
     */
    public static void addWindowListener(Component component, final WindowListener listener) {
        while (component != null) {
            if (component instanceof org.apache.sis.swing.Window) {
                ((org.apache.sis.swing.Window) component).addWindowListener(listener);
                break;
            }
            if (component instanceof Window) {
                ((Window) component).addWindowListener(listener);
                break;
            }
            if (component instanceof JInternalFrame) {
                ((JInternalFrame) component).addInternalFrameListener(InternalWindowListener.wrap(listener));
                break;
            }
            component = component.getParent();
        }
    }

    /**
     * Removes the given listener from the first window ancestor found in the hierarchy.
     * This method is the converse of {@code addWindowListener(listener, component)}.
     *
     * @param  component  the component for which to search for a window ancestor.
     * @param  listener   the listener to unregister.
     */
    public static void removeWindowListener(Component component, final WindowListener listener) {
        while (component != null) {
            if (component instanceof org.apache.sis.swing.Window) {
                ((org.apache.sis.swing.Window) component).removeWindowListener(listener);
                break;
            }
            if (component instanceof Window) {
                ((Window) component).removeWindowListener(listener);
                break;
            }
            if (component instanceof JInternalFrame) {
                InternalWindowListener.removeWindowListener((JInternalFrame) component, listener);
                break;
            }
            component = component.getParent();
        }
    }

    /**
     * Causes runnable to have its run method invoked in the dispatch thread of the event queue.
     * This will happen after all pending events are processed.
     * The call blocks until this has happened.
     *
     * @param  runnable  the task to run in the dispatch thread.
     */
    public static void invokeAndWait(final Runnable runnable) {
        if (EventQueue.isDispatchThread()) {
            runnable.run();
        } else {
            try {
                EventQueue.invokeAndWait(runnable);
            } catch (InterruptedException exception) {
                // Someone don't want to let us sleep. Go back to work.
            } catch (InvocationTargetException target) {
                final Throwable exception = target.getTargetException();
                if (exception instanceof RuntimeException) {
                    throw (RuntimeException) exception;
                }
                if (exception instanceof Error) {
                    throw (Error) exception;
                }
                // Should not happen since `Runnable.run()` does not allow checked exception.
                throw new UndeclaredThrowableException(exception, exception.getLocalizedMessage());
            }
        }
    }
}
