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
 * Thrown when an object can not be {@linkplain ObjectConverter#apply(Object) converted}
 * from the <cite>source</cite> type to the <cite>target</cite> type.
 *
 * <p>Some converters may attempt many strategies before to give up, resulting in more than
 * one exception being caught. In such case, all the failed attempts will be reported as
 * {@linkplain #getSuppressed() suppressed exceptions} and the {@linkplain #getCause() cause}
 * will be an arbitrary item of this list.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public class UnconvertibleObjectException extends IllegalArgumentException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4436966248421454692L;

    /**
     * Constructs a new exception with no message.
     */
    public UnconvertibleObjectException() {
        super();
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message The detail message, or {@code null} if none.
     */
    public UnconvertibleObjectException(final String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message The detail message, or {@code null} if none.
     * @param cause The cause, or {@code null} if none.
     */
    public UnconvertibleObjectException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new exception with the specified cause.
     *
     * @param cause The cause, or {@code null} if none.
     */
    public UnconvertibleObjectException(final Throwable cause) {
        super(cause);
    }
}
