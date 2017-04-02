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
package org.apache.sis.storage;

import java.util.Locale;
import org.apache.sis.internal.storage.Resources;


/**
 * Thrown when an operation can not be performed while a write operation is in progress.
 * This exception is thrown for example if a read operation is attempted on a data store
 * that does not support concurrent read and write operations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 *
 * @see ConcurrentReadException
 *
 * @since 0.8
 * @module
 */
public class ConcurrentWriteException extends DataStoreException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4005018924099696792L;

    /**
     * Creates an exception with no cause and no details message.
     */
    public ConcurrentWriteException() {
    }

    /**
     * Creates an exception with the specified details message.
     *
     * @param message  the detail message.
     */
    public ConcurrentWriteException(String message) {
        super(message);
    }

    /**
     * Creates an exception with the specified details message and cause.
     *
     * @param message  the detail message.
     * @param cause    the cause for this exception.
     */
    public ConcurrentWriteException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a localized exception for an operation that can not be executed while a write operation is in progress.
     * Arguments given to this constructor are hints for building an error message.
     *
     * @param locale    the locale of the message to be returned by {@link #getLocalizedMessage()}, or {@code null}.
     * @param filename  name of the file or data store where a write operation is in progress.
     */
    public ConcurrentWriteException(final Locale locale, final String filename) {
        super(locale, Resources.Keys.ConcurrentWrite_1, filename);
    }
}
