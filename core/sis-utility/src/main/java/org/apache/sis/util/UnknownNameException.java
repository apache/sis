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
package org.apache.sis.util;


/**
 * Thrown when an operation can not complete because a given name is unrecognized.
 * The unrecognized name may be a {@link org.opengis.util.GenericName}, an
 * {@link org.opengis.metadata.Identifier}, a {@link String} used as a name or identifier,
 * or any other objects with similar purpose.
 *
 * <p><b>Note:</b> in the particular case of objects created from a {@link org.opengis.util.Factory},
 * the exception for unrecognized identifiers is rather {@link org.opengis.util.NoSuchIdentifierException}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public class UnknownNameException extends RuntimeException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 8184564092008827669L;

    /**
     * Constructs a new exception with no message.
     */
    public UnknownNameException() {
        super();
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message The detail message, or {@code null} if none.
     */
    public UnknownNameException(final String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message The detail message, or {@code null} if none.
     * @param cause The cause, or {@code null} if none.
     */
    public UnknownNameException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
