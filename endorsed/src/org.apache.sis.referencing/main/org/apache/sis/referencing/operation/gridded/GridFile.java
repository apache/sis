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
package org.apache.sis.referencing.operation.gridded;

import java.net.URI;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.FileSystemNotFoundException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.concurrent.atomic.AtomicBoolean;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterNotFoundException;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.factory.FactoryDataException;
import org.apache.sis.referencing.factory.MissingFactoryResourceException;
import org.apache.sis.referencing.operation.provider.AbstractProvider;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.system.DataDirectory;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Messages;


/**
 * Resolved path to a grid file. The starting point is the path specified by a parameter.
 * If that path is relative, then this class tries to resolve it in a directory specified
 * by the {@code SIS_DATA} environment variable. If the path cannot be resolved that way,
 * then this method check if it can be resolved relatively to the GML or WKT file containing
 * the parameter.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class GridFile {
    /**
     * Whether the tip about the location of datum shift files has been logged.
     * We log this tip only once, and only if we failed to load at least one grid.
     */
    private static final AtomicBoolean datumDirectoryLogged = new AtomicBoolean();

    /**
     * The directory where to search for a local copy of the data, or {@code null} if none.
     */
    private final DataDirectory localDirectory;

    /**
     * The URI specified in the parameter. This URI is usually relative to an unspecified directory.
     *
     * @see #resolved()
     */
    public final URI parameter;

    /**
     * The URI as an absolute path.
     */
    private URI resolved;

    /**
     * The base URI used for resolving the parameter, or {@code null} if none.
     */
    private URI base;

    /**
     * The resolved URI as a path, or {@code null} if not yet computed or not convertible.
     */
    private Path asPath;

    /**
     * Creates a file for the given URI, assumed already resolved.
     * This constructor is for testing purposes.
     *
     * @param  resolved  the resolved URI.
     */
    public GridFile(final URI resolved) {
        parameter = resolved;
        this.resolved = resolved;
        localDirectory = DataDirectory.DATUM_CHANGES;
    }

    /**
     * Resolves the given parameter as an absolute URI, resolved in the {@code "$SIS_DATA/DatumChanges"} directory
     * if the URI is relative. If the URI cannot be resolved, a {@link MissingFactoryResourceException} is thrown.
     * That exception type is necessary for letting the caller know that a coordinate operation is probably valid
     * but cannot be constructed because an optional configuration is missing.
     * It is typically because the {@code SIS_DATA} environment variable has not been set.
     *
     * @param  group  the group of parameters from which to get the URI.
     * @param  param  identification of the parameter to fetch.
     * @throws ParameterNotFoundException if the specified parameter is not found in the given group.
     * @throws MissingFactoryResourceException if the path cannot be resolved.
     */
    public GridFile(final Parameters group, final ParameterDescriptor<URI> param) throws MissingFactoryResourceException {
        this(group, param, DataDirectory.DATUM_CHANGES);
    }

    /**
     * Resolves the given parameter as an absolute URI, resolved with the specified {@code DataDirectory}
     * if the URI is relative. If the URI cannot be resolved, a {@link MissingFactoryResourceException} is thrown.
     * That exception type is necessary for letting the caller know that a coordinate operation is probably valid
     * but cannot be constructed because an optional configuration is missing.
     * It is typically because the {@code SIS_DATA} environment variable has not been set.
     *
     * @param  group           the group of parameters from which to get the URI.
     * @param  param           identification of the parameter to fetch.
     * @param  localDirectory  the directory where to search for a local copy of the data, or {@code null} if none.
     * @throws ParameterNotFoundException if the specified parameter is not found in the given group.
     * @throws MissingFactoryResourceException if the path cannot be resolved.
     */
    public GridFile(final Parameters group, final ParameterDescriptor<URI> param, final DataDirectory localDirectory)
            throws MissingFactoryResourceException
    {
        RuntimeException error = null;
        this.localDirectory = localDirectory;
        parameter = group.getMandatoryValue(param);
        if (parameter.isAbsolute()) {
            resolved = parameter.normalize();
        } else {
            /*
             * First, try to resolve the parameter relative to the "$SIS_DATA/DatumChanges" directory.
             * That directory can be seen as a cache to be tried before to download data that may be
             * on the network.
             */
            if (localDirectory != null) {
                base = localDirectory.getDirectoryAsURI();
                if (base != null) try {
                    resolved = base.resolve(parameter).normalize();
                    asPath = Path.of(resolved);
                    if (Files.exists(asPath)) {
                        return;
                    }
                } catch (IllegalArgumentException | FileSystemNotFoundException e) {
                    error = e;
                }
            }
            /*
             * If the "$SIS_DATA/DatumChanges" directory cannot be used, check if we
             * have another base URI that we could try. If not, we cannot continue.
             */
            final URI document = group.getSourceFile(param).orElse(null);
            if (document == null) {
                if (resolved != null) {
                    return;             // NoSuchFileException will be thrown later by `newByteChannel()`.
                }
                final String message;
                if (parameter.isOpaque()) {
                    message = Errors.format(Errors.Keys.CanNotOpen_1, parameter);
                } else {
                    final String env = DataDirectory.getenv();
                    if (env == null) {
                        message = Messages.format(Messages.Keys.DataDirectoryNotSpecified_1, DataDirectory.ENV);
                    } else {
                        message = Messages.format(Messages.Keys.DataDirectoryNotAccessible_2, DataDirectory.ENV, env);
                    }
                }
                throw new MissingFactoryResourceException(message, error);
            }
            /*
             * Use the alternative base URI without checking if it exists.
             * This check will be done when the file will be opened.
             */
            base = document;
            resolved = document.resolve(parameter).normalize();
        }
        try {
            asPath = Path.of(resolved);
        } catch (IllegalArgumentException | FileSystemNotFoundException e) {
            if (error == null) error = e;
            else error.addSuppressed(e);
            asPath = null;
        }
        if (error != null) {
            Logging.ignorableException(AbstractProvider.LOGGER, GridFile.class, "<init>", error);
        }
    }

    /**
     * {@return the resolved URI}.
     *
     * @see #parameter
     */
    public URI resolved() {
        return resolved;
    }

    /**
     * {@return the resolved URI as a path if possible}.
     * A use case for this method is grids to open as a {@link org.apache.sis.storage.DataStore}.
     */
    public Optional<Path> path() {
        return Optional.ofNullable(asPath);
    }

    /**
     * Creates a channel for reading bytes from the file at the path specified at construction time.
     * This method tries to open using the file system before to open from the URL.
     *
     * @return a channel for reading bytes from the file.
     * @throws IOException if the channel cannot be created.
     */
    public ReadableByteChannel newByteChannel() throws IOException {
        if (asPath != null) {
            return Files.newByteChannel(asPath);
        } else {
            return Channels.newChannel(resolved.toURL().openStream());
        }
    }

    /**
     * Creates a buffered reader for reading characters from the file at the path specified at construction time.
     * This method tries to open using the file system before to open from the URL.
     *
     * @return a channel for reading bytes from the file.
     * @throws IOException if the reader cannot be created.
     */
    public BufferedReader newBufferedReader() throws IOException {
        if (asPath != null) {
            return Files.newBufferedReader(asPath);
        } else {
            return new BufferedReader(new InputStreamReader(resolved.toURL().openStream()));
        }
    }

    /**
     * Logs a message about a grid which is about to be loaded.
     * The logger will be {@code "org.apache.sis.referencing.operation"} and the originating
     * method will be {@code "createMathTransform"} in the specified {@code caller} class.
     *
     * @param  caller  the provider to logs as the source class.
     */
    public void startLoading(final Class<?> caller) {
        startLoading(caller, parameter);
    }

    /**
     * Logs a message about a grid which is about to be loaded.
     * The logger will be {@code "org.apache.sis.referencing.operation"} and the originating
     * method will be {@code "createMathTransform"} in the specified {@code caller} class.
     *
     * @param  caller  the provider to logs as the source class.
     * @param  file    the grid file, as a {@link String} or a {@link URI}.
     */
    public static void startLoading(final Class<?> caller, final Object file) {
        GridLoader.log(caller, Resources.forLocale(null)
                .createLogRecord(Level.FINE, Resources.Keys.LoadingDatumShiftFile_1, file));
    }

    /**
     * Creates the exception to throw when the provider failed to load the grid file.
     * The first time that this method is invoked, an information message is logged
     * as a tip to the user about where data where searched.
     *
     * @param  caller  the provider to logs as the source class if a warning occurs.
     * @param  format  the format name (e.g. "NTv2" or "NADCON").
     * @param  cause   the cause of the failure to load the grid file.
     */
    public FactoryException canNotLoad(final Class<?> caller, final String format, final Exception cause) {
        if (localDirectory != null && !datumDirectoryLogged.get()) {
            final Path directory = localDirectory.getDirectory();
            if (directory != null && !datumDirectoryLogged.getAndSet(true)) {
                GridLoader.log(caller, Resources.forLocale(null).createLogRecord(Level.INFO,
                                       Resources.Keys.DatumChangesDirectory_1, directory));
            }
        }
        if (cause instanceof NoSuchFileException || cause instanceof FileNotFoundException) {
            return new MissingFactoryResourceException(Resources.format(Resources.Keys.FileNotFound_2), cause);
        } else {
            return new FactoryDataException(Resources.format(Resources.Keys.FileNotReadable_2, format, parameter), cause);
        }
    }

    /**
     * {@return a string representation of this path for debugging purposes}.
     */
    @Override
    public String toString() {
        return String.valueOf(resolved);
    }
}
