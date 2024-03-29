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
package org.apache.sis.metadata;


/**
 * Thrown when a metadata is in a invalid state or has illegal property values.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.3
 */
public class InvalidMetadataException extends IllegalStateException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 947896851753786460L;

    /**
     * Creates a new exception with the specified detail message.
     *
     * @param  message  the details message, or {@code null} if none.
     */
    public InvalidMetadataException(final String message) {
        super(message);
    }

    /**
     * Creates a new exception with the specified cause.
     * The details message is copied from the cause.
     *
     * @param  cause  the cause, or {@code null} if none.
     *
     * @since 0.8
     */
    public InvalidMetadataException(final Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new exception with the specified detail message and cause.
     *
     * @param  message  the details message, or {@code null} if none.
     * @param  cause    the cause, or {@code null} if none.
     *
     * @since 0.8
     */
    public InvalidMetadataException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
