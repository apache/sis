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
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.NoSuchFileException;
import java.util.logging.Level;
import java.util.concurrent.atomic.AtomicBoolean;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterNotFoundException;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.factory.FactoryDataException;
import org.apache.sis.referencing.factory.MissingFactoryResourceException;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.referencing.operation.provider.AbstractProvider;
import org.apache.sis.referencing.operation.transform.MathTransformBuilder;
import org.apache.sis.referencing.operation.transform.MathTransformProvider.Context;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.system.DataDirectory;
import org.apache.sis.system.DataURI;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Messages;


/**
 * Resolved path to a grid file. The starting point is the path specified by a parameter.
 * If that path is relative, then this class tries to resolve it in a directory specified
 * by the {@code SIS_DATA} environment variable. If the path cannot be resolved that way,
 * then this class checks if the path can be resolved relatively to the document containing
 * the parameter.
 *
 * <p>Instances of this class should be temporary.
 * This is an helper class for loading data and discarded after the loading completed.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class GridFile extends DataURI {
    /**
     * Whether the tip about the location of datum shift files has been logged.
     * We log this tip only once, and only if we failed to load at least one grid.
     */
    private static final AtomicBoolean datumDirectoryLogged = new AtomicBoolean();

    /**
     * Returns the directory where to search for a local copy of the data.
     */
    private static DataDirectory localDirectory() {
        return DataDirectory.DATUM_CHANGES;
    }

    /**
     * Resolves the given parameter as an absolute URI, resolved in the {@code "$SIS_DATA/DatumChanges"} directory
     * if the URI is relative. If the URI cannot be resolved, a {@link MissingFactoryResourceException} is thrown.
     * That exception type is necessary for letting the caller know that a coordinate operation is probably valid
     * but cannot be constructed because an optional configuration is missing.
     * It is typically because the {@code SIS_DATA} environment variable has not been set.
     *
     * @param  context  context of the transform to create, or {@code null}.
     * @param  group    the group of parameters from which to get the URI.
     * @param  param    identification of the parameter to fetch.
     * @throws ParameterNotFoundException if the specified parameter is not found in the given group.
     * @throws MissingFactoryResourceException if the path cannot be resolved.
     * @throws InvalidGeodeticParameterException if access is denied.
     */
    @SuppressWarnings("LocalVariableHidesMemberVariable")
    public GridFile(final Context context, final Parameters group, final ParameterDescriptor<URI> param)
            throws FactoryException
    {
        super(group.getMandatoryValue(param));
        /*
         * First, try to resolve the parameter relatively to the "$SIS_DATA/DatumChanges" directory.
         * That directory can be seen as a cache to be tried before to potentially download the data.
         */
        if (!tryResolve(localDirectory().getDirectoryAsURI()) || isFileMissing()) {
            /*
             * If the "$SIS_DATA/DatumChanges" directory cannot be used, assume a file in the same directory
             * as the document that provided the parameter. Throw an exception if no resolution was possible,
             * including with previous attempt. Do not throw an exception for file not found,
             * because that check will be done when the file will be opened.
             */
            if (!tryResolve(group.getSourceFile(param).orElse(null)) && resolved() == null) {
                /*
                 * If the URL cannot be resolved, the most important reason is because `SIS_DATA` was not set.
                 * Try to provide an helpful error message. This is not about whether the file exists.
                 */
                final String message;
                if (DataDirectory.getenv() == null) {
                    message = Messages.format(Messages.Keys.DataDirectoryNotSpecified_1, DataDirectory.ENV);
                } else {
                    message = Errors.format(Errors.Keys.CanNotOpen_1, parameter);
                }
                throw new MissingFactoryResourceException(message, error);
            }
        }
        if (error != null) {
            Logging.ignorableException(AbstractProvider.LOGGER, GridFile.class, "<init>", error);
        }
        /*
         * Verify authorization to read the file at the given URL. If there is no user-specified access control,
         * the default is the verify that the URL is not outside the local data directory or the parent directory.
         */
        if (context instanceof MathTransformBuilder) {
            final var builder = (MathTransformBuilder) context;
            switch (builder.getAccessControl().apply(param, resolved())) {
                case GRANTED: return;
                case DENIED: isRelative = false; break;
            }
        }
        if (!isRelative) {
            throw new InvalidGeodeticParameterException(accessDenied());
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
        if (!datumDirectoryLogged.get()) {
            final Path directory = localDirectory().getDirectory();
            if (directory != null && !datumDirectoryLogged.getAndSet(true)) {
                GridLoader.log(caller, Resources.forLocale(null).createLogRecord(Level.INFO,
                                       Resources.Keys.DatumChangesDirectory_1, directory));
            }
        }
        final boolean notFound = cause instanceof NoSuchFileException || cause instanceof FileNotFoundException;
        String message = Resources.format(notFound ? Resources.Keys.FileNotFound_2 : Resources.Keys.FileNotReadable_2, format, parameter);
        if (notFound) {
            return new MissingFactoryResourceException(message, cause);
        } else {
            return new FactoryDataException(message, cause);
        }
    }
}
