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
package org.apache.sis.internal.setup;

import java.awt.Desktop;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;


/**
 * Ask user to specify the path to JavaFX installation directory.
 * This is used one JavaFX can not be found on the classpath.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final class FXFinder {
    /**
     * The URL where to download JavaFX.
     */
    private static final String DOWNLOAD_URL = "https://openjfx.io/";

    /**
     * The {@value} directory in JavaFX installation directory.
     * This is the directory where JAR files are expected to be found.
     */
    private static final String LIB_DIRECTORY = "lib";

    /**
     * A file to search in the {@value #LIB_DIRECTORY} directory for determining if JavaFX is present.
     */
    private static final String SENTINEL_FILE = "javafx.controls.jar";

    /**
     * The environment variable containing the path to JavaFX {@value #LIB_DIRECTORY} directory.
     */
    private static final String PATH_VARIABLE = "PATH_TO_FX";

    /**
     * File extension of Windows batch file. If the script file to edit does not have this extension,
     * then it is assumed to be a Unix bash script.
     */
    private static final String WINDOWS_BATCH_EXTENSION = ".bat";

    /**
     * Do not allow instantiation of this class.
     */
    private FXFinder() {
    }

    /**
     * Popups a modal dialog box asking user to choose a directory.
     *
     * @param  args  command line arguments. Should have a length of 1,
     *               with {@code args[0]} containing the path of the file to edit.
     */
    public static void main(String[] args) {
        boolean success = false;
        try {
            success = askDirectory(Paths.get(args[0]).normalize());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        System.exit(success ? 0 : 1);
    }

    /**
     * Popups a modal dialog box asking user to choose a directory.
     *
     * @param  setenv  path of the {@code setenv.sh} file to edit.
     * @return {@code true} if we can continue with application launch,
     *         or {@code false} on error or cancellation.
     */
    private static boolean askDirectory(final Path setenv) throws Exception {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | UnsupportedLookAndFeelException e) {
            // Ignore.
        }
        /*
         * Checks now that we can edit `setenv.sh` content in order to not show the next
         * dialog box if we czn not read that file (e.g. because the file was not found).
         */
        if (!Files.isReadable(setenv) || !Files.isWritable(setenv)) {
            JOptionPane.showMessageDialog(null, "Can not edit " + setenv,
                    "Configuration error", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        /*
         * Ask the user what he wants to do.
         */
        final JLabel description = new JLabel(
                "<html><body><p style=\"width:400px; text-align:justify;\">" +
                "This application requires <b>JavaFX</b> version 13 or later. " +
                "Click on “Download” for opening the free download page. " +
                "If JavaFX is already installed on this computer, " +
                "click on “Set directory” for specifying the installation directory." +
                "</p></body></html>");

        description.setFont(description.getFont().deriveFont(Font.PLAIN));
        final Object[] options = {"Download", "Set directory", "Cancel"};
        final int choice = JOptionPane.showOptionDialog(null, description,
                "JavaFX installation directory",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[2]);

        if (choice == 0) {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(DOWNLOAD_URL));
            } else {
                JOptionPane.showMessageDialog(null, "See " + DOWNLOAD_URL,
                        "JavaFX download", JOptionPane.INFORMATION_MESSAGE);
            }
        } else if (choice == 1) {
            final JFileChooser fd = new JFileChooser();
            fd.setDialogTitle("JavaFX installation directory");
            fd.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            while (fd.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                final File dir = findSubDirectory(fd.getSelectedFile());
                if (dir == null) {
                    JOptionPane.showMessageDialog(null, "Not a JavaFX directory.",
                            "JavaFX installation directory", JOptionPane.WARNING_MESSAGE);
                } else {
                    setDirectory(setenv, dir);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Tries to locate the {@code lib} sub-folder in a JavaFX installation directory.
     *
     * @param  dir  the directory from where to start the search.
     * @return      the {@code lib} directory, or {@code null} if not found.
     */
    private static File findSubDirectory(final File dir) {
        if (new File(dir, SENTINEL_FILE).exists()) {
            return dir;
        }
        final File lib = new File(dir, LIB_DIRECTORY);
        if (new File(lib, SENTINEL_FILE).exists()) {
            return lib;
        }
        return null;
    }

    /**
     * Sets the JavaFX directory.
     *
     * @param  setenv  path to the {@code setenv.sh} file to edit.
     * @param  dir     directory selected by user.
     */
    private static void setDirectory(final Path setenv, final File dir) throws IOException {
        String command = PATH_VARIABLE;
        if (setenv.getFileName().toString().endsWith(WINDOWS_BATCH_EXTENSION)) {
            command = "SET " + command;                             // Microsoft Windows syntax.
        }
        final ArrayList<String> content = new ArrayList<>();
        int insertAt = -1;
        for (String line : Files.readAllLines(setenv)) {
            line = line.trim();
            if (line.startsWith(command)) {
                insertAt = content.size();
            } else {
                content.add(line);
            }
        }
        if (insertAt < 0) {
            insertAt = content.size();
        }
        content.add(insertAt, command + '=' + dir);
        Files.write(setenv, content, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
