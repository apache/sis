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
package org.apache.sis.referencing.operation.matrix;

import org.opengis.referencing.operation.NoninvertibleTransformException;


/**
 * Thrown when a matrix cannot be inverted.
 * This may be because the matrix is singular, or because the matrix is not square.
 *
 * <h2>Design note</h2>
 * This exception extends {@code NoninvertibleTransformException} because in the context
 * of Apache <abbr>SIS</abbr>, matrix inversions occur mostly during the construction of
 * a coordinate operation between a pair of <abbr>CRS</abbr>.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.4
 * @since   0.4
 */
public class NoninvertibleMatrixException extends NoninvertibleTransformException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4776332020257526053L;

    /**
     * Constructs a new exception with no message.
     */
    public NoninvertibleMatrixException() {
        super();
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message  the detail message, or {@code null} if none.
     */
    public NoninvertibleMatrixException(final String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message  the detail message, or {@code null} if none.
     * @param cause    the cause, or {@code null} if none.
     */
    public NoninvertibleMatrixException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
