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
package org.apache.sis.internal.jdk7;

import java.io.File;
import java.net.URI;


/**
 * Place holder for {@link java.nio.file.Path}.
 * This class exists only on the JDK6 branch of SIS.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@SuppressWarnings("serial")
public final class Path extends File {
    /**
     * Creates a path for the given file.
     */
    Path(final String path) {
        super(path);
    }

    /**
     * Creates a path for the given URI.
     */
    Path(final URI path) throws IllegalArgumentException {
        super(path);
    }

    /**
     * For {@link #resolve(String)} implementation.
     */
    Path(final File path, final String other) {
        super(path, other);
    }

    /**
     * For inter-operability.
     *
     * @param file The file to cast or copy.
     * @return The given file as a path.
     */
    public static Path castOrCopy(final File file) {
        return (file == null || file instanceof Path) ? (Path) file : new Path(file.getPath());
    }

    /**
     * Returns the last element in the path.
     *
     * @return Last element in the path.
     */
    public Path getFileName() {
        return new Path(getName());
    }

    /**
     * Unimplemented.
     *
     * @return this.
     */
    public Path normalize() {
        return this;
    }

    /**
     * Returns a sub-directory of this path, or the given path if it is absolute.
     *
     * @param other The sub-directory name.
     * @return The path with the sub-directory.
     */
    public Path resolve(final Path other) {
        if (other.isAbsolute()) {
            return other;
        }
        return new Path(this, other.toString());
    }

    /**
     * Returns a sub-directory of this path, provided that the given path is not absolute.
     *
     * @param other The sub-directory name.
     * @return The path with the sub-directory.
     */
    public Path resolve(final String other) {
        return resolve(new Path(other));
    }

    /**
     * Resolves the given path against the parent of this path.
     *
     * @param other The sub-directory name.
     * @return The path with the sub-directory.
     */
    public Path resolveSibling(final Path other) {
        if (other.isAbsolute()) {
            return other;
        }
        return new Path(getParentFile(), other.toString());
    }

    /**
     * Resolves the given path against the parent of this path.
     *
     * @param other The sub-directory name.
     * @return The path with the sub-directory.
     */
    public Path resolveSibling(final String other) {
        return resolveSibling(new Path(other));
    }

    /**
     * Returns the given path relative to this path.
     *
     * @param other The path to make relative to this path.
     * @return The relative path (if possible).
     */
    public Path relativize(final Path other) {
        final String start = getAbsolutePath();
        final String full  = other.getAbsolutePath();
        if (full.startsWith(start)) {
            final int length = start.length();
            if (full.length() > length && full.charAt(length) == separatorChar) {
                return new Path(full.substring(length + 1));
            }
        }
        return other;
    }


    /**
     * Returns this path as a URI.
     *
     * @return The URI for this path.
     */
    public URI toUri() {
        return toURI();
    }

    /**
     * Returns the path as an absolute path.
     *
     * @return The absolute path.
     */
    public Path toAbsolutePath() {
        return isAbsolute() ? this : new Path(getAbsolutePath());
    }
}
