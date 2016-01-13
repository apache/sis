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

import java.net.URI;


/**
 * Place holder for {@link java.nio.file.Paths}.
 * This class exists only on the JDK6 branch of SIS.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final class Paths {
    /**
     * Do not allow instantiation of this class.
     */
    private Paths() {
    }

    /**
     * Creates a path.
     *
     * @param first First element of the path.
     * @param more Additional element of the path.
     * @return The path.
     * @throws InvalidPathException if the path can not be created.
     */
    public static Path get(final String first, final String... more) throws InvalidPathException {
        try {
            Path path = new Path(first);
            for (final String other : more) {
                path = new Path(path, other);
            }
            return path;
        } catch (RuntimeException e) {
            throw new InvalidPathException(e);
        }
    }

    /**
     * Returns a path for the given URI.
     *
     * @param uri The URI
     * @return The path.
     * @throws IllegalArgumentException if the path can not be created.
     */
    public static Path get(final URI uri) throws IllegalArgumentException {
        return new Path(uri);
    }
}
