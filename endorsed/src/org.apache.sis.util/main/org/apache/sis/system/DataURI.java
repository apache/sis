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
package org.apache.sis.system;

import java.util.Optional;
import java.net.URI;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.FileSystemNotFoundException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import org.apache.sis.io.Authorization;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.internal.shared.Strings;


/**
 * Helper base class for services which will access an user-specified <abbr>URI</abbr>.
 * This is a support class for access control. For example, the <abbr>URI</abbr> can be
 * restricted to the directory specified by {@link DataDirectory}.
 *
 * <p>Instances of this class should be temporary.
 * This is an helper class for loading data and discarded after the loading completed.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see DataDirectory
 * @see Authorization
 */
public class DataURI {
    /**
     * The <abbr>URI</abbr> specified in parameter, usually relative to an unspecified directory.
     * This information is stored for formatting error messages in case of failure to load data.
     * For reading the data, use {@link #resolved()} instead.
    *
     * @see #resolved()
     */
    public final URI parameter;

    /**
     * The <abbr>URI</abbr> parameter as a resolved (usually absolute) and normalized reference.
     */
    private URI resolved;

    /**
     * The resolved <abbr>URI</abbr> as a path, or {@code null} if not convertible.
     */
    private Path asPath;

    /**
     * Errors that occurred while trying to resolve the <abbr>URI</abbr> or convert it to a path.
     * Used for logging purpose or for declaration as the cause of another exception.
     */
    protected Exception error;

    /**
     * Whether the resolved <abbr>URI</abbr> is relative to the last specified base.
     * This is {@code true} if the last call to {@link #tryResolve(URI)} resulted in an <abbr>URI</abbr>
     * starting with the given base. This information can be used for access control, in order to ensure
     * that the file is inside the expected directory.
     */
    protected boolean isRelative;

    /**
     * Creates a new instance for the given user-specified <abbr>URI</abbr>.
     *
     * @param  parameter  the <abbr>URI</abbr> from a user-specified parameter.
     */
    protected DataURI(final URI parameter) {
        this.parameter = parameter;
    }

    /**
     * Tries to resolve the user-specified parameter relatively to the given base.
     * This method returns {@code true} if the parameter has been resolved, not necessarily by using
     * the given {@code base} parameter. For distinguishing whether the resolved <abbr>URI</abbr> is
     * relative to the given base, see the {@link #isRelative} flag.
     *
     * <p>If the {@code base} argument is a file instead of a directory, then this method
     * resolves the user-specified {@linkplain #parameter} as a sibling of the given file.
     * This is {@link URI#resolve(URI)} standard behavior, not a special case of this method.
     * This behavior is useful when a file is expected to be found in the same directory as the
     * <abbr>JSON</abbr>, <abbr>GML</abbr> or <abbr>WKT</abbr> document containing the parameter.</p>
     *
     * @param  base  base directory, or {@code null} if unknown.
     * @return whether the parameter could be resolved.
     */
    protected final boolean tryResolve(final URI base) {
        if (base == null || parameter == null) {
            return false;
        }
        final URI result = base.resolve(parameter).normalize();
        if (result != resolved) {
            if (result == parameter) {
                isRelative = false;
            } else {
                String path = Strings.orEmpty(base.getPath());
                path = path.substring(0, path.lastIndexOf('/') + 1);
                isRelative = Strings.orEmpty(result.getPath()).startsWith(path);
            }
            resolved = result;
            try {
                asPath = Path.of(result);
            } catch (IllegalArgumentException | FileSystemNotFoundException e) {
                if (error == null) error = e;
                else error.addSuppressed(e);
                asPath = null;
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if the file is inside the expected directory but does not exists.
     * In case of doubt, or if the file is outside the expected directory, returns {@code false}.
     *
     * @return {@code true} if the path does <em>not</em> exists.
     */
    protected final boolean isFileMissing() {
        return isRelative && (asPath != null) && Files.notExists(asPath);
    }

    /**
     * Returns an error message saying that we are not authorized to read from the user-provided <abbr>URL</abbr>.
     *
     * @return error message to provide in the exception to be thrown.
     */
    protected final String accessDenied() {
        return Errors.format(Errors.Keys.AccessDenied_1, (asPath != null) ? asPath : resolved);
    }

    /**
     * Returns the <abbr>URI</abbr> parameter as a resolved (usually absolute) and normalized reference.
     *
     * @return the resolved and normalized <abbr>URI</abbr>, or {@code null} if none.
     *
     * @see #parameter
     */
    public final URI resolved() {
        return resolved;
    }

    /**
     * Returns the resolved <abbr>URI</abbr> as a path if possible.
     * A use case for this method is grids to open as a {@link org.apache.sis.storage.DataStore}.
     *
     * @return the resolved <abbr>URI</abbr> as a path.
     */
    public final Optional<Path> path() {
        return Optional.ofNullable(asPath);
    }

    /**
     * Creates a channel for reading bytes from the file at the path specified at construction time.
     * This method tries to open using the file system before to open from the <abbr>URL</abbr>.
     * Caller should have verified authorization before to invoke this method.
     *
     * @return a channel for reading bytes from the file.
     * @throws IOException if the channel cannot be created.
     */
    public final ReadableByteChannel newByteChannel() throws IOException {
        if (asPath != null) {
            return Files.newByteChannel(asPath);
        } else {
            return Channels.newChannel(resolved.toURL().openStream());
        }
    }

    /**
     * Creates a buffered reader for reading characters from the file at the path specified at construction time.
     * This method tries to open using the file system before to open from the <abbr>URL</abbr>.
     * Caller should have verified authorization before to invoke this method.
     *
     * @return a channel for reading bytes from the file.
     * @throws IOException if the reader cannot be created.
     */
    public final BufferedReader newBufferedReader() throws IOException {
        if (asPath != null) {
            return Files.newBufferedReader(asPath);
        } else {
            return new BufferedReader(new InputStreamReader(resolved.toURL().openStream()));
        }
    }

    /**
     * Returns a string representation of this path for debugging purposes.
     *
     * @return string representation for debugging purposes.
     */
    @Override
    public String toString() {
        return String.valueOf(resolved != null ? resolved : parameter);
    }
}
