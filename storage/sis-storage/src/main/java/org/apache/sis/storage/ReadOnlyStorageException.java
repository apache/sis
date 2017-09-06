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

import org.apache.sis.util.Localized;


/**
 * Thrown when a {@code DataStore} can not perform a write operations.
 * This exception may occur either because:
 *
 * <ul>
 *   <li>the data store does not support write operations, or</li>
 *   <li>write operations are supported but the channel is read-only.</li>
 * </ul>
 *
 * @author  Johann Sorel (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 *
 * @see ForwardOnlyStorageException
 */
public class ReadOnlyStorageException extends DataStoreException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5710116172772560023L;

    /**
     * Creates an exception with no cause and no details message.
     */
    public ReadOnlyStorageException() {
    }

    /**
     * Creates an exception with the specified details message.
     *
     * @param message  the detail message.
     */
    public ReadOnlyStorageException(final String message) {
        super(message);
    }

    /**
     * Creates an exception with the specified cause and no details message.
     *
     * @param cause  the cause for this exception.
     */
    public ReadOnlyStorageException(final Throwable cause) {
        super(cause);
    }

    /**
     * Creates an exception with the specified details message and cause.
     *
     * @param message  the detail message.
     * @param cause    the cause for this exception.
     */
    public ReadOnlyStorageException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new exception which will format a localized message in the resource locale.
     *
     * @param originator  the instance throwing this exception, or {@code null} if unknown.
     * @param key         one of {@link org.apache.sis.internal.storage.Resources.Keys} constants.
     * @param parameters  parameters to use for formatting the messages.
     */
    ReadOnlyStorageException(final Resource originator, final short key, final Object... parameters) {
        super((originator instanceof Localized) ? ((Localized) originator).getLocale() : null, key, parameters);
    }
}
