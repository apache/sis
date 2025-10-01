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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/**
 * Ask user to specify the path to JavaFX installation directory.
 * This is used when JavaFX cannot be found on the module path.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class FXFinder {
    /**
     * Minimal version of JavaFX required by Apache SIS.
     */
    static final int JAVAFX_VERSION = 20;

    /**
     * The URL where to download JavaFX.
     */
    static final String JAVAFX_HOME = "https://openjfx.io/";

    /**
     * Prefix of JavaFX directory. This is checked only in ZIP files.
     * We do not check that name in decompressed directory because the
     * user is free to rename.
     */
    static final String JAVAFX_DIRECTORY_PREFIX = "javafx-sdk-";

    /**
     * The {@value} directory in JavaFX installation directory.
     * This is the directory where JAR files are expected to be found.
     */
    private static final String JAVAFX_LIB_DIRECTORY = "lib";

    /**
     * A file to search in the {@value #JAVAFX_LIB_DIRECTORY} directory for determining if JavaFX is present.
     */
    private static final String JAVAFX_SENTINEL_FILE = "javafx.controls.jar";

    /**
     * The environment variable containing the path to JavaFX {@value #JAVAFX_LIB_DIRECTORY} directory.
     */
    static final String PATH_VARIABLE = "PATH_TO_FX";

    /**
     * The {@value} directory in Apache SIS installation where the {@code setenv.sh} file
     * is expected to be located.
     */
    private static final String SIS_CONF_DIRECTORY = "conf";

    /**
     * The {@value} directory in Apache SIS installation where to unzip JavaFX.
     * This is relative to {@code $BASE_DIR} environment variable.
     *
     * @see #decompress(Wizard)
     */
    private static final String SIS_UNZIP_DIRECTORY = "opt";

    /**
     * File extension of Windows batch file. If the script file to edit does not have this extension,
     * then it is assumed to be a Unix bash script.
     */
    private static final String WINDOWS_BATCH_EXTENSION = ".bat";

    /**
     * Exit code to return when user cancelled the configuration process.
     */
    private static final int CANCEL_EXIT_CODE = 1;

    /**
     * Exit code to return if the wizard cannot start.
     */
    private static final int ERROR_EXIT_CODE = 2;

    /**
     * The JavaFX directory as specified by the user, or {@code null} if none.
     */
    private File specified;

    /**
     * The JavaFX directory validated by {@code FXFinder}, or {@code null} if the directory is invalid.
     * May be the same file as {@link #specified}, but not necessarily; it may be a subdirectory.
     */
    private File validated;

    /**
     * Path of the {@code setenv.sh} file to edit.
     */
    private final Path setenv;

    /**
     * The background task created if there is a JavaFX ZIP file to decompress.
     */
    private Inflater inflater;

    /**
     * Whether to use the relative path {@code $BASE_DIR/opt/javafx-sdk} instead of an absolute path.
     * This is {@code true} if we have decompressed the JavaFX ZIP file with the {@link Inflater}.
     */
    boolean useRelativePath;

    /**
     * {@code true} if this operation systems is Windows, or {@code false} if assumed Unix (Linux or MacOS).
     */
    private final boolean isWindows;

    /**
     * Creates a new finder.
     */
    private FXFinder(final String setenv) {
        this.setenv = Path.of(setenv).normalize();
        isWindows = setenv.endsWith(WINDOWS_BATCH_EXTENSION);
    }

    /**
     * Popups a modal dialog box asking user to choose a directory.
     *
     * @param  args  command line arguments. Should have a length of 1,
     *               with {@code args[0]} containing the path of the file to edit.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void main(String[] args) {
        if (args.length == 1) {
            if (Wizard.show(new FXFinder(args[0]))) {
                // Call to `System.exit(int)` will be done by `Wizard`.
                return;
            }
        } else {
            System.out.println("Required: path to setenv.sh");
        }
        System.exit(ERROR_EXIT_CODE);
    }

    /**
     * Returns {@code null} if the configuration file has been found and can be edited.
     * If this method returns a non-null, then the setup wizard should be cancelled.
     * The returned value can be used as an error message.
     */
    final String diagnostic() {
        if (Files.isReadable(setenv) && Files.isWritable(setenv)) {
            return null;
        }
        return "Cannot edit " + setenv;
    }

    /**
     * Returns the values of environment variables relevant to Apache SIS.
     * This is used for showing a summary after configuration finished.
     */
    final String[][] getEnvironmentVariables() {
        return new String[][] {
            getEnvironmentVariable("JAVA_HOME"),
            getEnvironmentVariable(PATH_VARIABLE),
            getEnvironmentVariable("SIS_DATA"),
            getEnvironmentVariable("SIS_OPTS"),
        };
    }

    /**
     * Returns the value of the environment variable of given name.
     * The returned array contains the following elements:
     *
     * <ul>
     *   <li>Variable name</li>
     *   <li>Value to show (never null)</li>
     * </ul>
     */
    private String[] getEnvironmentVariable(final String name) {
        String value;
        try {
            value = System.getenv(name);
            if (value == null) {
                value = "(undefined)";
            } else if (value.isEmpty()) {
                value = "(blank)";
            } else if (name.equals("SIS_DATA") && value.equals("bin/../data")) {
                value = Path.of(value).toAbsolutePath().toString();
            }
        } catch (SecurityException e) {
            value  = "(unreadable)";
        }
        return new String[] {name, value};
    }

    /**
     * Returns the name of JavaFX bundle to download, including the operating system name.
     * Example: "JavaFX Linux SDK". This is for helping the user to choose which file to
     * download on the {@value #JAVAFX_HOME} web page.
     */
    static String getJavafxBundleName() {
        String name;
        try {
            name = System.getProperty("os.name");
        } catch (SecurityException e) {
            name = null;
        }
        if (name == null) {
            name = "<operating system>";
        }
        return "JavaFX " + name + " SDK";
    }

    /**
     * Returns the directory as validated by {@code FXFinder}.
     * May be slightly different than the user-specified directory.
     */
    final String getValidatedDirectory() {
        return (validated != null) ? validated.getPath() : null;
    }

    /**
     * Returns the directory specified by the user, or {@code null} if none.
     */
    final File getDirectory() {
        return specified;
    }

    /**
     * Sets the JavaFX directory to the given value and checks its validity.
     * This method tries to locate the {@code lib} sub-folder that we expect
     * in a JavaFX installation directory.
     *
     * @param  dir  the directory from where to start the search.
     * @return whether the given directory seems valid.
     */
    final boolean setDirectory(final File dir) {
        specified = dir;
        validated = null;
        if (new File(dir, JAVAFX_SENTINEL_FILE).exists()) {
            validated = dir;
            return true;
        }
        final File lib = new File(dir, JAVAFX_LIB_DIRECTORY);
        if (new File(lib, JAVAFX_SENTINEL_FILE).exists()) {
            validated = lib;
            return true;
        }
        return false;
    }

    /**
     * Verifies whether the given file seems to be a valid ZIP file.
     * This method checks for a sentinel value in the ZIP entries.
     * The entry may be:
     *
     * <pre>javafx-sdk-&lt;version&gt;/lib/javafx.controls.jar</pre>
     *
     * If the file seems valid, {@code null} is returned.
     * Otherwise an error message in HTML is returned.
     *
     * @param  file  path to the zip file.
     * @return {@code null} on success, otherwise error message in HTML.
     */
    static String checkZip(final File file) throws IOException {
        try (ZipFile zip = new ZipFile(file)) {
            final Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    final String basedir = entry.getName();
                    if (basedir.startsWith(JAVAFX_DIRECTORY_PREFIX)) {
                        final int start = JAVAFX_DIRECTORY_PREFIX.length();
                        int end = start;
                        while (end < basedir.length()) {
                            final char c = basedir.charAt(end);
                            if (c < '0' || c > '9') break;
                            end++;
                        }
                        if (end > start) {
                            final int version = Integer.parseInt(basedir.substring(start, end));
                            if (version < JAVAFX_VERSION) {
                                return "<html>Apache SIS requires JavaFX version " + JAVAFX_VERSION + " or later. "
                                        + "The given file contains JavaFX version " + version + ".</html>";
                            }
                            if (zip.getEntry(basedir + JAVAFX_LIB_DIRECTORY + '/' + JAVAFX_SENTINEL_FILE) != null) {
                                return null;        // Valid file.
                            }
                        }
                    }
                    break;
                }
            }
        }
        return "<html>Not a recognized ZIP file for JavaFX SDK.</html>";
    }

    /**
     * Returns the destination directory where to decompress ZIP files.
     * This method assumes the following directory structure:
     *
     * <pre class="text">
     *     apache-sis       (can be any name)
     *     ├─ conf
     *     │  └─ setenv.sh
     *     └─ opt</pre>
     */
    final File getDestinationDirectory() throws IOException {
        File basedir = setenv.toAbsolutePath().toFile().getParentFile();
        if (basedir != null && SIS_CONF_DIRECTORY.equals(basedir.getName())) {
            basedir = basedir.getParentFile();
            if (basedir != null) {
                final File destination = new File(basedir, SIS_UNZIP_DIRECTORY);
                if (destination.isDirectory() || destination.mkdir()) {
                    return destination;
                }
                throw new IOException("Cannot create directory: " + destination);
            }
        }
        throw new FileNotFoundException("No parent directory to " + setenv + '.');
    }

    /**
     * If the user-specified file is a ZIP file, starts decompression in a background thread.
     *
     * @return whether decompression started.
     */
    final boolean decompress(final Wizard wizard) {
        if (validated == null) {
            inflater = new Inflater(wizard, specified);
            final Thread t = new Thread(inflater, "Inflater");
            t.start();
            return true;
        }
        return false;
    }

    /**
     * Cancels configuration, deletes decompressed files if any and exits.
     * This method is invoked by {@link Wizard} in the following situations:
     *
     * <ul>
     *   <li>User clicked on the "Cancel" button.</li>
     *   <li>User clicked on the "Close window" button in window title bar.</li>
     * </ul>
     *
     * If a decompression is in progress, it is stopped and all files are deleted.
     */
    final void cancel() {
        if (inflater != null) {
            inflater.cancel();
        }
        System.exit(CANCEL_EXIT_CODE);
    }

    /**
     * Commits the configuration by writing the JavaFX directory in the {@code setenv.sh} file.
     */
    final void commit() throws IOException {
        inflater = null;
        String command = PATH_VARIABLE;
        if (isWindows) {
            command = "SET " + command;                             // Microsoft Windows syntax.
        }
        /*
         * Read content of `setenv.sh` file excluding the line starting with `command` if any.
         * We remember the position where we found `command` in order to insert replacement at
         * the same line.
         */
        final var content = new ArrayList<String>();
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
        /*
         * Insert the new line for setting the `PATH_TO_FX` variable.
         * It will be either an absolute path, or a path relative to
         * the SIS installation directory.
         */
        final var value = new StringBuilder(100).append(command).append('=');
        if (useRelativePath) {
            final File relative = relativeToBase(validated);
            if (relative != null) {
                value.append(isWindows ? "%BASE_DIR%" : "$BASE_DIR")
                     .append(File.separatorChar).append(relative);
            } else {
                useRelativePath = false;
            }
        }
        if (!useRelativePath) {
            value.append(validated);
        }
        content.add(insertAt, value.toString());
        Files.write(setenv, content, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Makes the current directory relative to the SIS base directory. This method assumes that
     * the last directory named {@value #SIS_UNZIP_DIRECTORY} is the first directory relative to
     * the base. This is okay only if the JavaFX zip file does not contain directory of that name.
     */
    private static File relativeToBase(final File dir) {
        if (dir != null) {
            if (SIS_UNZIP_DIRECTORY.equals(dir.getName())) {
                return new File(SIS_UNZIP_DIRECTORY);
            }
            final File parent = relativeToBase(dir.getParentFile());
            if (parent != null) {
                return new File(parent, dir.getName());
            }
        }
        return null;
    }
}
