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
package org.apache.sis.gui.setup;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileFilter;


/**
 * Configuration wizard for Apache SIS.
 * The wizard contains the following step:
 *
 * <ul>
 *   <li>Introduction</li>
 *   <li>Internet page where to download JavaFX.</li>
 *   <li>Path to JavaFX installation directory.</li>
 *   <li>Configuration summary</li>
 * </ul>
 *
 * This class provides all the Graphical User Interface (GUI) using Swing widgets.
 * The class doing actual work for managing SIS configuration is {@link FXFinder}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class Wizard extends FileFilter implements ActionListener, PropertyChangeListener, DropTargetListener {
    /**
     * Initializes Look and Feel before to construct any Swing component.
     */
    static {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | UnsupportedLookAndFeelException e) {
            // Ignore.
        }
        UIManager.put("FileChooser.readOnly", Boolean.TRUE);
    }

    /**
     * Scale factor to apply on all sizes expressed in pixels.
     * This is a value greater than 1 on screens with a high resolution.
     */
    private final float scaleFactoryForHiDPI;

    /**
     * The window width, in pixels.
     * Needs to be multiplied by {@link #scaleFactoryForHiDPI} after that value become known.
     */
    private static final int WIDTH = 700, HEIGHT = 500;

    /**
     * Label of button to show in the wizard, also used as action identifier.
     */
    private static final String BACK = "Back", NEXT = "Next", CANCEL = "Cancel",
            JAVAFX_HOME = "Open JavaFX home page", BROWSE = "Browse", SELECT = "Select";

    /**
     * Color of {@linkplain #titles} for pages other than the current page.
     *
     * Conceptually a {@code static final} constant, but declared non-static for initializing
     * it only at {@link Wizard} creation time and because that creation will happen only once.
     */
    private final Color TITLE_COLOR = new Color(36, 113, 163);

    /**
     * Color of title for the {@linkplain #currentPage current page}.
     *
     * Conceptually a {@code static final} constant, but declared non-static for initializing
     * it only at {@link Wizard} creation time and because that creation will happen only once.
     */
    private final Color SELECTED_TITLE_COLOR = new Color(21, 67, 96);

    /**
     * Bullet in front of (selected) titles. Bullets should have the same width
     * for avoiding change in {@link #titles} text position when user move from
     * one page to the other.
     */
    private static final String TITLE_BULLET = "• ", SELECTED_TITLE_BULLET = "‣ ";

    /**
     * The normal {@link #javafxPath} border.
     *
     * Conceptually a {@code static final} constant, but declared non-static for initializing
     * it only at {@link Wizard} creation time and because that creation will happen only once.
     */
    private final Border JAVAFX_PATH_BORDER = new LineBorder(Color.GRAY);

    /**
     * The {@link #javafxPath} border during drag and drop action. We use a green border.
     *
     * Conceptually a {@code static final} constant, but declared non-static for initializing
     * it only at {@link Wizard} creation time and because that creation will happen only once.
     */
    private final Border JAVAFX_PATH_BORDER_DND = new LineBorder(new Color(40, 180, 99), 3);

    /**
     * The top-level window where wizard will be shown.
     */
    private final JFrame wizard;

    /**
     * The panel where each wizard step is shown. This panel uses a {@link CardLayout}.
     */
    private final JPanel cardPanel;

    /**
     * The button for moving to next page.
     * Its label will be changed from "Next" to "Finish" when on the last page.
     */
    private final JButton nextButton;

    /**
     * The button for moving to previous page.
     * Disabled when on the first page.
     */
    private final JButton backButton;

    /**
     * The button for cancelling setup.
     * Disabled when on the last page.
     */
    private final JButton cancelButton;

    /**
     * The button for selecting a directory or a ZIP file. This button may be
     * non-null only during the time that a {@link JFileChooser} is visible.
     *
     * @see #findSelectButton(Container)
     */
    private JButton selectButton;

    /**
     * Titles of each page. This is highlighted during navigation.
     */
    private final JLabel[] titles;

    /**
     * The page currently shown.
     */
    private WizardPage currentPage;

    /**
     * Whether this wizard accepts the JavaFX location specified in the {@link WizardPage#JAVAFX_LOCATION} page.
     */
    private boolean acceptLocation;

    /**
     * JavaFX directory or ZIP file.
     *
     * @see #setJavafxPath(File)
     */
    final FXFinder javafxFinder;

    /**
     * View of the path to JavaFX installation directory. This is the value of {@link FXFinder#getDirectory()},
     * potentially shown in red if the location is not valid.
     */
    private final JLabel javafxPath;

    /**
     * If the {@link #javafxPath} is not valid, a message for the user. Otherwise this label is empty.
     */
    private final JLabel javafxPathError;

    /**
     * The message shown on the last page. This is <q>Apache SIS setup is completed</q>,
     * but may be changed if the setup failed.
     */
    private JLabel finalMessage;

    /**
     * Final value of JavaFX path shown in the last page. May be slightly different than {@link #javafxPath}.
     */
    private JLabel finalJavafxPath;

    /**
     * Information about progress of decompression process.
     */
    final JProgressBar inflateProgress;

    /**
     * Creates a new wizard.
     *
     * @see #show(FXFinder)
     */
    private Wizard(final FXFinder javafxFinder) {
        this.javafxFinder = javafxFinder;
        wizard = new JFrame("Apache SIS setup");
        final Container content = wizard.getContentPane();
        scaleFactoryForHiDPI = content.getFont().getSize() / 14f;
        content.setLayout(new BorderLayout());
        /*
         * Back, Next, Cancel button.
         */
        {   // For keeping variables in a local scope.
            final Box buttons = Box.createHorizontalBox();
            buttons.setBorder(newEmptyBorder(9, 12, 9, 15));        // Top, left, bottom, right.
            backButton   = createButton(buttons, BACK); buttons.add(Box.createHorizontalStrut(scaledSize(10)));
            nextButton   = createButton(buttons, NEXT); buttons.add(Box.createHorizontalStrut(scaledSize(30)));
            cancelButton = createButton(buttons, CANCEL);
            backButton.setEnabled(false);

            final var bottom = new JPanel(new BorderLayout());
            bottom.add(new JSeparator(), BorderLayout.NORTH);
            bottom.add(buttons, java.awt.BorderLayout.EAST);
            content.add(bottom, BorderLayout.SOUTH);
        }
        /*
         * Navigation panel on the left side with the following titles
         * (currently shown page is highlighted):
         *
         *    - Introduction
         *    - Download
         *    - Set directory
         *    - Summary
         */
        final WizardPage[] pages = WizardPage.values();
        {
            titles = new JLabel[pages.length];
            final var padding = newEmptyBorder(3, 0, 3, 0);
            final Box summary = Box.createVerticalBox();
            for (int i=0; i<pages.length; i++) {
                final String title = (i == 0 ? SELECTED_TITLE_BULLET : TITLE_BULLET) + pages[i].title;
                final JLabel label = new JLabel(title, JLabel.LEFT);
                label.setForeground(i == 0 ? SELECTED_TITLE_COLOR : TITLE_COLOR);
                label.setBorder(padding);
                summary.add(titles[i] = label);
            }
            final var pane = new JPanel();
            pane.setBackground(new Color(169, 204, 227));
            pane.setBorder(newEmptyBorder(40, 15, 9, 24));          // Top, left, bottom, right.
            pane.add(summary);
            content.add(pane, BorderLayout.WEST);
        }
        /*
         * The main content where text is shown, together with download button, directory chooser, etc.
         * The content of each page is created by `createPage(…)`. They all have in common to start with
         * a description text formatted in HTML.
         */
        {
            final var font = new Font(Font.SERIF, Font.PLAIN, scaledSize(14));
            javafxPath = new JLabel();
            javafxPath.setBorder(JAVAFX_PATH_BORDER);
            javafxPathError = new JLabel();
            javafxPathError.setForeground(Color.RED);
            javafxPathError.setFont(font);
            inflateProgress = new JProgressBar();
            cardPanel = new JPanel(new CardLayout());
            cardPanel.setBorder(newEmptyBorder(30, 30, 9, 30));     // Top, left, bottom, right.
            cardPanel.setBackground(Color.WHITE);
            for (final WizardPage page : pages) {
                cardPanel.add(createPage(page, font), page.name());
                // The initially visible component is the first added.
            }
            currentPage = pages[0];
            content.add(cardPanel, BorderLayout.CENTER);
        }
        wizard.setSize(scaledSize(WIDTH), scaledSize(HEIGHT));      // Must be before `setLocationRelativeTo(…)`.
        wizard.setLocationRelativeTo(null);
        wizard.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) {
                javafxFinder.cancel();
            }
        });
    }

    /**
     * Returns the given size scaled for HiDPI screens.
     */
    private int scaledSize(final int size) {
        return (int) (scaleFactoryForHiDPI * size);
    }

    /**
     * Creates an empty border scaled for HiDPI screens.
     */
    @SuppressWarnings("lossy-conversions")
    private EmptyBorder newEmptyBorder(int top, int left, int bottom, int right) {
        top    *= scaleFactoryForHiDPI;
        left   *= scaleFactoryForHiDPI;
        bottom *= scaleFactoryForHiDPI;
        right  *= scaleFactoryForHiDPI;
        return new EmptyBorder(top, left, bottom, right);
    }

    /**
     * Creates a dimension scaled for HiDPI screens.
     */
    @SuppressWarnings("lossy-conversions")
    private Dimension newDimension(int width, int height) {
        width  *= scaleFactoryForHiDPI;
        height *= scaleFactoryForHiDPI;
        return new Dimension(width, height);
    }

    /**
     * Invoked by the constructor for preparing in advance each page in a {@link CardLayout}.
     * Each page starts with a text formatted in HTML using a Serif font (such as Times),
     * followed by control specific to each page.
     *
     * @param  page  identifies the page to create.
     * @param  font  Serif font to use for the text.
     */
    private Box createPage(final WizardPage page, final Font font) {
        final Box content = Box.createVerticalBox();
        final var text = new JLabel(page.text, JLabel.LEFT);
        text.setFont(font);
        content.add(text);
        content.add(Box.createVerticalStrut(scaledSize(30)));
        switch (page) {
            case DOWNLOAD_JAVAFX: {
                final JButton button = createButton(content, JAVAFX_HOME);
                button.setToolTipText(FXFinder.JAVAFX_HOME);
                button.setEnabled(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE));
                final var instruction = new JLabel(WizardPage.downloadSteps());
                instruction.setFont(font.deriveFont(scaledSize(12)));
                content.add(instruction);
                break;
            }
            case JAVAFX_LOCATION: {
                javafxPath.setMinimumSize(newDimension(  100, 30));
                javafxPath.setMaximumSize(newDimension(WIDTH, 30));
                content.add(javafxPath);
                content.add(Box.createVerticalStrut(scaledSize(12)));
                createButton(content, BROWSE);
                content.add(Box.createVerticalStrut(scaledSize(24)));
                content.add(javafxPathError);
                content.setDropTarget(new DropTarget(content, this));
                break;
            }
            case DECOMPRESS: {
                inflateProgress.setMinimumSize(newDimension(  100, 21));
                inflateProgress.setMaximumSize(newDimension(WIDTH, 21));
                content.add(inflateProgress);
                break;
            }
            case COMPLETED: {
                finalMessage = text;
                final var vb = newEmptyBorder(0, 15, 9, 0);
                final var fn = new Font(Font.MONOSPACED, Font.BOLD,  scaledSize(13));
                final var fv = new Font(Font.SANS_SERIF, Font.PLAIN, scaledSize(13));
                for (final String[] variable : javafxFinder.getEnvironmentVariables()) {
                    final var name  = new JLabel(variable[0] + ':');
                    final var value = new JLabel(variable[1]);
                    name .setForeground(Color.DARK_GRAY);
                    value.setForeground(Color.DARK_GRAY);
                    name .setFont(fn);
                    value.setFont(fv);
                    value.setBorder(vb);
                    name.setLabelFor(value);
                    content.add(name);
                    content.add(value);
                    if (FXFinder.PATH_VARIABLE.equals(variable[0])) {
                        finalJavafxPath = value;
                    }
                }
                break;
            }
        }
        return content;
    }

    /**
     * Creates a button and adds it to the given box. A listener is registered
     * for an action having the same name as the button label.
     *
     * @param  addTo  the horizontal box where to add the button.
     * @param  label  button labels, also used as action identifier.
     * @return the added button.
     */
    private JButton createButton(final Box addTo, final String label) {
        var button = new JButton(label);
        button.setActionCommand(label);
        button.addActionListener(this);
        addTo.add(button);
        return button;
    }

    /**
     * Invoked when user clicks on a button.
     * The action name is the label given to {@link #createButton(Box, String)}.
     */
    @Override
    public void actionPerformed(final ActionEvent event) {
        switch (event.getActionCommand()) {
            case CANCEL:      javafxFinder.cancel();  break;
            case BACK:        nextOrPreviousPage(-1); break;
            case NEXT:        nextOrPreviousPage(+1); break;
            case JAVAFX_HOME: openJavafxHomePage();   break;
            case BROWSE:      showDirectoryChooser(); break;
        }
    }

    /**
     * Invoked when the user clicks on the {@value #BACK} or {@value #NEXT} button for moving to the
     * previous page or to the next page. This method changes the highlighted title on the left side,
     * updates the buttons enabled status and shows the new page.
     *
     * <p>Moving to the next page may cause the following actions:</p>
     * <ul>
     *   <li>Moving to the last page cause a call to {@link FXFinder#commit()}.</li>
     *   <li>Moving after the last page cause a system exit (wizard finished).</li>
     * </ul>
     *
     * @param  n  -1 for previous page, or +1 for next page.
     */
    private void nextOrPreviousPage(final int n) {
        /*
         * Restore title (on the left side) of current page to default color.
         * In other words, remove highlighting.
         */
        int index = currentPage.ordinal();
        JLabel title = titles[index];
        title.setForeground(TITLE_COLOR);
        title.setText(TITLE_BULLET + currentPage.title);
        final WizardPage[] pages = WizardPage.values();
        if ((index += n) >= pages.length) {
            /*
             * User clicked on "Finish" in the last page:
             * wizard finished successfully.
             */
            wizard.dispose();
            System.exit(0);
            return;
        }
        /*
         * Highlight title (on the left side) of new current page.
         * Next, there is some specific actions depending on the new page.
         */
        currentPage = pages[index];
        title = titles[index];
        title.setForeground(SELECTED_TITLE_COLOR);
        title.setText(SELECTED_TITLE_BULLET + currentPage.title);
        backButton.setEnabled(index > 0);
        nextButton.setEnabled(true);
        switch (currentPage) {
            case JAVAFX_LOCATION: {
                nextButton.setEnabled(acceptLocation);
                break;
            }
            case DECOMPRESS: {
                backButton.setEnabled(false);
                nextButton.setEnabled(false);
                if (!javafxFinder.decompress(this)) {
                    nextOrPreviousPage(n);              // Nothing to decompress, skip this page.
                    return;
                }
                break;
            }
            case COMPLETED: {
                backButton.setEnabled(false);
                nextButton.setText("Finish");
                try {
                    javafxFinder.commit();
                    cancelButton.setEnabled(false);
                    finalJavafxPath.setText(javafxFinder.getValidatedDirectory());
                } catch (IOException e) {
                    nextButton.setEnabled(false);
                    finalMessage.setForeground(Color.RED);
                    finalMessage.setText(getHtmlMessage("Apache SIS setup cannot be completed.", e));
                }
               break;
            }
        }
        ((CardLayout) cardPanel.getLayout()).show(cardPanel, currentPage.name());
    }

    /**
     * Invoked in Swing thread after decompression finished either successfully or on failure.
     * Note that there is no method for cancelled operation because in such case,
     * {@link FXFinder#cancel()} will be invoked directly.
     *
     * @param  destination  the directory where ZIP files have been decompressed.
     * @param  failure      if decompression failed, the error. Otherwise {@code null}.
     */
    final void decompressionFinished(final File destination, final Exception failure) {
        final boolean isValid;
        if (failure != null) {
            isValid = false;
            javafxPathError.setText(getHtmlMessage("Cannot decompress the file.", failure));
        } else {
            isValid = setJavafxPath(destination);
        }
        javafxFinder.useRelativePath = isValid;     // Must be before the call to `nextOrPreviousPage(…)`.
        nextOrPreviousPage(isValid ? +1 : -1);
    }

    /**
     * Returns a non-null message for given exception with HTML characters escaped.
     */
    private static String getHtmlMessage(final String header, final Exception e) {
        final var buffer = new StringBuilder(100).append("<html>").append(header)
                    .append("<br><b>").append(e.getClass().getSimpleName()).append("</b>");
        String message = e.getLocalizedMessage();
        if (message != null) {
            message = message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
            buffer.append(": ").append(message);
        }
        return buffer.append("</html>").toString();
    }

    /**
     * Invoked when the user clicks on the {@value #JAVAFX_HOME} button
     * for opening the {@value FXFinder#JAVAFX_HOME} URL in a browser.
     */
    private void openJavafxHomePage() {
        try {
            Desktop.getDesktop().browse(new URI(FXFinder.JAVAFX_HOME));
        } catch (URISyntaxException | IOException | RuntimeException e) {
            JOptionPane.showMessageDialog(wizard, e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Invoked when user clicks on the {@value #BROWSE} button for choosing a JavaFX installation directory.
     * If user selects an invalid file or directory, the chooser popups again until the user selects a valid
     * file or cancels.
     */
    private void showDirectoryChooser() {
        final var fd = new JFileChooser(javafxFinder.getDirectory());
        fd.addChoosableFileFilter(this);
        fd.setFileFilter(this);
        fd.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fd.setDialogTitle("JavaFX installation directory");
        fd.setApproveButtonText(SELECT);
        selectButton = findSelectButton(fd);
        fd.setApproveButtonText(null);
        if (selectButton != null) {
            selectButton.setEnabled(false);
            fd.addPropertyChangeListener(this);
        }
        fd.setPreferredSize(newDimension(WIDTH + 100, HEIGHT + 100));
        if (fd.showOpenDialog(wizard) == JFileChooser.APPROVE_OPTION) {
            setJavafxPath(fd.getSelectedFile());
        }
        selectButton = null;
    }

    /**
     * Searches recursively for the {@value #SELECT} button in the given container. This is used for
     * locating the "Open" button in {@link JFileChooser}. Caller needs to temporarily change button
     * text to {@value #SELECT} before to invoke this method. We cannot search directly for "Open"
     * text because that text may be localized.
     *
     * @param  c  the container where to search for the {@value #SELECT} button.
     */
    private static JButton findSelectButton(final Container c) {
        final int n = c.getComponentCount();
        for (int i=0; i<n; i++) {
            final Component child = c.getComponent(i);
            if (child instanceof JButton) {
                final var button = (JButton) child;
                if (SELECT.equals(button.getText())) {
                    return button;
                }
            } else if (child instanceof Container) {
                final JButton button = findSelectButton((Container) child);
                if (button != null) return button;
            }
        }
        return null;
    }

    /**
     * Returns the description to show in {@link JFileChooser} for possible JavaFX installation files.
     * The list of accepted file formats includes ZIP files.
     *
     * @return description of this filter to show in file chooser.
     */
    @Override
    public String getDescription() {
        return "ZIP files";
    }

    /**
     * Returns whether the given file is shown in {@link JFileChooser} as a possible JavaFX installation file.
     * This method performs a cheap test based on the extension.
     *
     * @param  file  the file to test.
     * @return whether the given file should be shown in the file chooser.
     */
    @Override
    public boolean accept(final File file) {
        if (file.isDirectory()) {
            return true;
        }
        final String name = file.getName();
        final int s = name.lastIndexOf('.');
        return (s >= 0) && name.regionMatches(true, s+1, "zip", 0, 3);
    }

    /**
     * Invoked when a {@link JFileChooser} property changed. If the property change tells us that
     * file selection changed (including the case where user changed directory), then this method
     * checks if the new selection is valid. This determines whether {@value #NEXT} button should
     * be enabled.
     *
     * @param  event  a description of the change.
     */
    @Override
    @SuppressWarnings("CallToPrintStackTrace")
    public void propertyChange(final PropertyChangeEvent event) {
        final File file;
        switch (event.getPropertyName()) {
            default: {
                return;
            }
            case JFileChooser.SELECTED_FILE_CHANGED_PROPERTY: {
                file = (File) event.getNewValue();
                break;
            }
            case JFileChooser.DIRECTORY_CHANGED_PROPERTY: {
                file = ((JFileChooser) event.getSource()).getSelectedFile();
                break;
            }
        }
        /*
         * Perform a cheap validity check (without opening the file) because this method may
         * be invoked often while user navigates through files. A more extensive check will
         * be done later by `setJavafxPath(File)`.
         */
        boolean enabled = false;
        if (file != null) {
            if (file.isFile()) {
                enabled = true;
            } else if (file.isDirectory()) {
                enabled = javafxFinder.setDirectory(file);
            }
        }
        selectButton.setEnabled(enabled);
    }

    /**
     * Sets the JavaFX directory and enables or disables the {@value #NEXT} button depending
     * on whether that file or directory is valid. If the file is not valid, a message will
     * be set in {@link #javafxPathError} (below the path).
     *
     * @param  dir  the JavaFX directory or ZIP file, or {@code null} if none.
     * @return whether the given file or directory is valid.
     */
    private boolean setJavafxPath(final File dir) {
        String error = null;
        boolean isValid = javafxFinder.setDirectory(dir);
        if (!isValid) {
            if (dir.isFile()) try {
                error = FXFinder.checkZip(dir);
                isValid = (error == null);
            } catch (IOException e) {
                error = getHtmlMessage("Cannot open the file.", e);
            } else {
                error = "<html>Not a recognized JavaFX directory or ZIP file.</html>";
            }
        }
        javafxPath.setText(dir != null ? dir.getPath() : null);
        javafxPath.setForeground(isValid ? Color.DARK_GRAY : Color.RED);
        if (currentPage == WizardPage.JAVAFX_LOCATION) {
            nextButton.setEnabled(isValid);
        }
        javafxPathError.setText(error);
        acceptLocation = isValid;
        return isValid;
    }

    /**
     * Invoked when user drops files in the wizard. This is an alternative way to set the JavaFX directory.
     *
     * @param  event  the drop event.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void drop(final DropTargetDropEvent event) {
        for (final DataFlavor flavor : event.getCurrentDataFlavors()) {
            if (flavor.isFlavorJavaFileListType()) {
                javafxPath.setBorder(JAVAFX_PATH_BORDER);
                event.acceptDrop(DnDConstants.ACTION_LINK);
                try {
                    for (final File file : (Iterable<File>) event.getTransferable().getTransferData(DataFlavor.javaFileListFlavor)) {
                        if (setJavafxPath(file)) break;
                    }
                } catch (UnsupportedFlavorException | IOException e) {
                    javafxPathError.setText(getHtmlMessage("Cannot open the file.", e));
                }
                event.dropComplete(true);
                return;
            }
        }
        event.rejectDrop();
    }

    /**
     * Invoked when the user is doing a drag and drop action and is entering in the target area.
     * This method sets a visual hint for telling to the user that the wizard is ready to receives the files.
     */
    @Override
    public void dragEnter(final DropTargetDragEvent event) {
        for (final DataFlavor flavor : event.getCurrentDataFlavors()) {
            if (flavor.isFlavorJavaFileListType()) {
                javafxPath.setBorder(JAVAFX_PATH_BORDER_DND);
                break;
            }
        }
    }

    /**
     * Invoked when the user is doing a drag and drop action and is exiting in the target area.
     * This method cancels the visual hint created by {@link #dragEnter(DropTargetDragEvent)}.
     */
    @Override
    public void dragExit(final DropTargetEvent event) {
        javafxPath.setBorder(JAVAFX_PATH_BORDER);
    }

    /** Ignored. */
    @Override public void dragOver(final DropTargetDragEvent event) {}

    /** Ignored. */
    @Override public void dropActionChanged(final DropTargetDragEvent event) {}

    /**
     * Shows the installation wizard.
     *
     * @return {@code true} if the wizard has been started, or {@code false} on configuration error.
     */
    public static boolean show(final FXFinder javafxFinder) {
        /*
         * Checks now that we can edit `setenv.sh` content in order to not show the wizard
         * if we cannot read that file (e.g. because the file was not found).
         */
        final String diagnostic = javafxFinder.diagnostic();
        if (diagnostic != null) {
            JOptionPane.showMessageDialog(null, diagnostic, "Configuration error", JOptionPane.ERROR_MESSAGE);
            return false;
        } else {
            final var wizard = new Wizard(javafxFinder);
            wizard.wizard.setVisible(true);
            return true;
        }
    }
}
