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
package org.apache.sis.internal.system;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.security.AccessController;
import java.security.PrivilegedAction;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Messages;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Files;
import org.apache.sis.internal.jdk7.Path;
import org.apache.sis.internal.jdk7.Paths;
import org.apache.sis.internal.jdk7.InvalidPathException;


/**
 * Sub-directories of {@code SIS_DATA} where SIS looks for EPSG database, datum shift grids and other resources.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public enum DataDirectory {
    /**
     * The {@code "Databases"} directory.
     * This directory is used for storing EPSG database and other metadata.
     */
    DATABASES,

    /**
     * The {@code "DatumChanges"} directory.
     * This directory is used for storing datum shift grids.
     */
    DATUM_CHANGES,

    /**
     * The {@code "DomainsOfValidity"} directory.
     * This directory is used for storing shapefiles for the CRS domains of validity.
     */
    DOMAINS_OF_VALIDITY;

    /**
     * The name of the environment variable.
     */
    public static final String ENV = "SIS_DATA";

    /**
     * Key of the last message logged at {@link Level#WARNING}, or {@code null} if none.
     *
     * @see #getRootDirectory()
     */
    private static short lastWarning;

    /**
     * The root directory fetched from the {@code SIS_DATA} environment variable when first needed,
     * or {@code null} if none or not yet determined.
     *
     * @see #getRootDirectory()
     */
    private static Path rootDirectory;

    /**
     * The directory, or {@code null} if none or not yet determined.
     *
     * @see #getDirectory()
     */
    private Path directory;

    /**
     * Logs a message to the {@code "org.apache.sis.system"} logger only if different than the last warning.
     */
    private static void warning(final String method, final Exception e, final short key, final Object... parameters) {
        if (key != lastWarning) {
            lastWarning = key;
            log(Level.WARNING, method, e, key, parameters);
        }
    }

    /**
     * Logs a message to the {@code "org.apache.sis.system"} logger.
     */
    private static void log(final Level level, final String method, final Exception e, final short key, final Object... parameters) {
        final LogRecord record = Messages.getResources(null).getLogRecord(level, key, parameters);
        record.setLoggerName(Loggers.SYSTEM);
        if (e != null) {
            record.setThrown(e);
        }
        Logging.log(DataDirectory.class, method, record);
    }

    /**
     * Returns the root directory fetched from the {@code SIS_DATA} environment variable.
     * If the environment variable is not set or the directory does not exist, then this method returns {@code null}.
     *
     * @return The root SIS data directory, or {@code null} if none.
     */
    public static synchronized Path getRootDirectory() {
        if (rootDirectory == null) try {
            final String dir = AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override public String run() {
                    return System.getenv(ENV);
                }
            });
            if (dir == null || dir.isEmpty()) {
                warning("getRootDirectory", null, Messages.Keys.DataDirectoryNotSpecified_1, ENV);
            } else try {
                final Path path = Paths.get(dir);
                if (!Files.isDirectory(path)) {
                    warning("getRootDirectory", null, Messages.Keys.DataDirectoryDoesNotExist_2, ENV, path);
                } else if (!Files.isReadable(path)) {
                    warning("getRootDirectory", null, Messages.Keys.DataDirectoryNotReadable_2, ENV, path);
                } else {
                    log(Level.CONFIG, "getRootDirectory", null, Messages.Keys.DataDirectory_2, ENV, path);
                    rootDirectory = path;
                }
            } catch (InvalidPathException e) {
                warning("getRootDirectory", e, Messages.Keys.DataDirectoryDoesNotExist_2, ENV, dir);
            }
        } catch (SecurityException e) {
            warning("getRootDirectory", e, Messages.Keys.DataDirectoryNotAuthorized_1, ENV);
        }
        return rootDirectory;
    }

    /**
     * Returns the sub-directory identified by this enum, or {@code null} if the parent {@code $SIS_DATA}
     * directory was not specified. If the {@code $SIS_DATA} directory exists but not the sub-directory,
     * then this method creates the sub-directory.
     *
     * @return The sub-directory, or {@code null} if unspecified.
     */
    public synchronized Path getDirectory() {
        if (directory == null) {
            final Path root = getRootDirectory();
            if (root != null) {
                final StringBuilder buffer = new StringBuilder(name());
                for (int i=1; i<buffer.length(); i++) {
                    final char c = buffer.charAt(i);
                    if (c == '_') buffer.deleteCharAt(i);
                    else buffer.setCharAt(i, Character.toLowerCase(c));
                }
                final String name = buffer.toString();
                final Path dir = root.resolve(name).normalize();
                try {
                    if (Files.isDirectory(dir)) {
                        directory = dir;
                    } else if (Files.isWritable(root)) try {
                        directory = Files.createDirectory(dir);
                    } catch (IOException e) {
                        warning("getDirectory", e, Messages.Keys.DataDirectoryNotWritable_2, ENV, root);
                    } else {
                        warning("getDirectory", null, Messages.Keys.DataDirectoryNotWritable_2, ENV, root);
                    }
                } catch (SecurityException e) {
                    warning("getDirectory", e, Messages.Keys.DataDirectoryNotAccessible_2, ENV, name);
                }
            }
        }
        return directory;
    }

    /**
     * If the given path is relative, returns the path as a child of the directory represented this enum.
     * If no valid directory is configured by the {@code SIS_DATA} environment variable, then the relative
     * path is returned as-is.
     *
     * @param  file The path to resolve, or {@code null}.
     * @return The path to use, or {@code null} if the given path was null.
     */
    public Path resolve(Path file) {
        if (file != null && !file.isAbsolute()) {
            final Path dir = getDirectory();
            if (dir != null) {
                return dir.resolve(file);
            }
        }
        return file;
    }
}
