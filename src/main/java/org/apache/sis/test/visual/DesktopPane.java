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
package org.apache.sis.test.visual;

import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.prefs.Preferences;
import java.awt.Toolkit;
import java.awt.Desktop;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyVetoException;
import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameAdapter;
import org.apache.sis.util.Classes;
import org.apache.sis.util.logging.Logging;


/**
 * The desktop pane where to put the widgets to be tested.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.1
 * @since   1.1
 */
@SuppressWarnings("serial")
final class DesktopPane extends JDesktopPane {
    /**
     * The key for screenshot directory in the user preferences.
     */
    private static final String SCREENSHOT_DIRECTORY_PREFS = "Screenshots";

    /**
     * The desktop which contain the internal frame for each widget.
     */
    static final DesktopPane INSTANCE = new DesktopPane();

    /**
     * The menu for creating new windows.
     */
    private final JMenu newMenu;

    /**
     * The last active component.
     */
    private JComponent active;

    /**
     * Whether all previous windows should be closed before new windows are created.
     */
    private final JCheckBoxMenuItem autoClose;

    /**
     * Creates the desktop, creates its frame and makes it visible.
     */
    private DesktopPane() {
        final JMenuBar menuBar = new JMenuBar();
        newMenu = new JMenu("New");
        menuBar.add(newMenu);
        {   // For keeping variables locale.
            final JMenu menu = new JMenu("Tools");
            menu.add(new AbstractAction("Screenshot") {
                @Override public void actionPerformed(final ActionEvent event) {screenshot();}
            });
            menu.add(new AbstractAction("Preferences") {
                @Override public void actionPerformed(final ActionEvent event) {preferences();}
            });
            menuBar.add(menu);
        }
        {   // For keeping variables locale.
            final JMenu menu = new JMenu("Windows");
            menu.add(new AbstractAction("List") {
                @Override public void actionPerformed(final ActionEvent event) {listWindows();}
            });
            menu.add(new AbstractAction("Close all") {
                @Override public void actionPerformed(final ActionEvent event) {closeAllWindows();}
            });
            menu.add(autoClose = new JCheckBoxMenuItem("Auto close"));
            menuBar.add(menu);
        }
        final JFrame frame = new JFrame("Widget tests");
        frame.setJMenuBar(menuBar);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setContentPane(this);
        frame.setSize(Toolkit.getDefaultToolkit().getScreenSize());
        frame.setLocationRelativeTo(null);                          // Put at screen center.
        frame.setVisible(true);
    }

    /**
     * Adds a test case to be shown in the "New" menu and show the widget immediately.
     * This method shall be invoked in Swing thread.
     *
     * @param  testCase  the test case for which the component is added.
     */
    final void addAndShow(final Visualization testCase) {
        newMenu.add(new AbstractAction(Classes.getShortName(testCase.testing)) {
            @Override public void actionPerformed(final ActionEvent event) {
                show(testCase);
            }
        });
        show(testCase);
    }

    /**
     * Shows the widget created by the given test case.
     */
    @SuppressWarnings("CallToPrintStackTrace")
    private void show(final Visualization testCase) {
        if (autoClose.isSelected()) {
            closeAllWindows();
        }
        try {
            for (int i=0; i<testCase.numTests; i++) {
                show(testCase, testCase.create(i), i);
            }
        } catch (Exception e) {
            // Not acceptable for a real application, but this widget is only for testing purpose.
            e.printStackTrace();
        }
    }

    /**
     * Shows the given component in a frame.
     *
     * @param  component  the component to show.
     */
    private void show(final Visualization testCase, final JComponent component, final int index) {
        final JInternalFrame frame = new JInternalFrame(testCase.title(index), true, true, true, true);
        frame.addInternalFrameListener(new InternalFrameAdapter() {
            @Override public void internalFrameActivated(final InternalFrameEvent event) {
                active = component;
            }

            @Override public void internalFrameClosed(final InternalFrameEvent event) {
                if (active == component) {
                    active = null;
                }
            }
        });
        frame.add(component);
        frame.pack();
        final Dimension size = frame.getMinimumSize();
        if (size != null) {
            frame.setSize(Math.max(frame.getWidth(),  size.width),
                          Math.max(frame.getHeight(), size.height));
        }
        final int numCols = (int) Math.ceil(Math.sqrt(testCase.numTests));
        final int numRows = (testCase.numTests + numCols - 1) / numCols;
        final int deltaX  = getWidth()  / numCols;
        final int deltaY  = getHeight() / numRows;
        frame.setLocation(Math.max(0, deltaX * (index % numCols) + (deltaX - frame.getWidth())  / 2),
                          Math.max(0, deltaY * (index / numCols) + (deltaY - frame.getHeight()) / 2));
        frame.setVisible(true);
        add(frame);
        try {
            frame.setSelected(true);
        } catch (PropertyVetoException e) {
            Logging.unexpectedException(null, DesktopPane.class, "show", e);
        }
    }

    /**
     * Lists windows known to this desktop.
     */
    private void listWindows() {
        final Component[] components = getComponents();
        final String[] titles = new String[components.length];
        for (int i=0; i<components.length; i++) {
            Component c = components[i];
            String title = String.valueOf(c.getName());
            if (c instanceof JInternalFrame) {
                final JInternalFrame frame = (JInternalFrame) c;
                title = String.valueOf(frame.getTitle());
                c = frame.getRootPane().getComponent(0);
            }
            final Dimension size = c.getSize();
            titles[i] = title + " : " + c.getClass().getSimpleName() +
                        '[' + size.width + " × " + size.height + ']';
        }
        final JInternalFrame frame = new JInternalFrame("Windows", true, true, true, true);
        frame.add(new JScrollPane(new JList<>(titles)));
        frame.pack();
        frame.setVisible(true);
        add(frame);
    }

    /**
     * Closes all windows known to this desktop.
     */
    private void closeAllWindows() {
        final Component[] components = getComponents();
        for (final Component c : components) {
            if (c instanceof JInternalFrame) {
                final JInternalFrame frame = (JInternalFrame) c;
                frame.dispose();
                remove(frame);
            }
        }
        active = null;
    }

    /**
     * Popups a dialog box for setting the preferences.
     */
    private void preferences() {
        final Preferences prefs = Preferences.userNodeForPackage(DesktopPane.class);
        final JFileChooser chooser = new JFileChooser(prefs.get(SCREENSHOT_DIRECTORY_PREFS, null));
        chooser.setDialogTitle("Output directory for screenshots");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        switch (chooser.showOpenDialog(this)) {
            case JFileChooser.APPROVE_OPTION: {
                final File directory = chooser.getSelectedFile();
                if (directory != null) {
                    prefs.put(SCREENSHOT_DIRECTORY_PREFS, directory.getPath());
                }
                break;
            }
        }
    }

    /**
     * Takes a screenshot of the currently active component.
     */
    private void screenshot() {
        final JComponent c = active;
        if (c != null && c.isValid()) {
            final BufferedImage image = new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_RGB);
            final Graphics2D handler = image.createGraphics();
            c.print(handler);
            handler.dispose();
            File file = new File(Preferences.userNodeForPackage(DesktopPane.class).get(SCREENSHOT_DIRECTORY_PREFS, "."));
            file = new File(file, Classes.getShortClassName(c) + ".png");
            try {
                if (ImageIO.write(image, "png", file)) {
                    file = file.getParentFile();
                    Desktop.getDesktop().open(file);
                } else {
                    JOptionPane.showInternalMessageDialog(this, "No PNG writer.", "Screenshot", JOptionPane.WARNING_MESSAGE);
                }
            } catch (IOException e) {
                JOptionPane.showInternalMessageDialog(c, e.getLocalizedMessage(),
                        e.getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showInternalMessageDialog(this, "No active window.", "Screenshot", JOptionPane.WARNING_MESSAGE);
        }
    }
}
