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
package org.apache.sis.referencing.operation.projection;

import org.opengis.referencing.operation.TransformException;
import org.apache.sis.util.resources.Errors;


/**
 * Thrown by {@link NormalizedProjection} when a map projection failed.
 *
 * @author  André Gosselin (MPO)
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public class ProjectionException extends TransformException {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 3031350727691500915L;

    /**
     * Constructs a new exception with no detail message.
     */
    public ProjectionException() {
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message The details message, or {@code null} if none.
     */
    public ProjectionException(final String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified cause.
     * The details message is copied from the cause.
     *
     * @param cause The cause, or {@code null} if none.
     */
    public ProjectionException(final Throwable cause) {
        super(cause.getLocalizedMessage(), cause);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message The details message, or {@code null} if none.
     * @param cause   The cause, or {@code null} if none.
     */
    public ProjectionException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param code One of the constants suitable for {@link Errors#format(short)}.
     */
    ProjectionException(final short code) {
        this(Errors.format(code));
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param code One of the constants suitable for {@link Errors#format(short)}.
     * @param value An argument value to be formatted.
     */
    ProjectionException(final short code, final Object value) {
        this(Errors.format(code, value));
    }
}
