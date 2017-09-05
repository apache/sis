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

/**
 * Thrown when a writing operation on a {@code DataStore} is not supported.
 *
 * @author Johann Sorel (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public class ReadOnlyDataStoreException extends DataStoreException {

    private static final long serialVersionUID = 5710116172772560023L;

    /**
     * Creates an exception with no cause and no details message.
     */
    public ReadOnlyDataStoreException() {
    }

    /**
     * Creates an exception with the specified details message.
     *
     * @param message  the detail message.
     */
    public ReadOnlyDataStoreException(final String message) {
        super(message);
    }

    /**
     * Creates an exception with the specified cause and no details message.
     *
     * @param cause  the cause for this exception.
     */
    public ReadOnlyDataStoreException(final Throwable cause) {
        super(cause);
    }

    /**
     * Creates an exception with the specified details message and cause.
     *
     * @param message  the detail message.
     * @param cause    the cause for this exception.
     */
    public ReadOnlyDataStoreException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new exception which will format a localized message in the given locale.
     *
     * @param locale      the locale for the message to be returned by {@link #getLocalizedMessage()}.
     * @param key         one of {@link org.apache.sis.internal.storage.Resources.Keys} constants.
     * @param parameters  parameters to use for formatting the messages.
     */
    ReadOnlyDataStoreException(final Locale locale, final short key, final Object... parameters) {
        super(locale, key, parameters);
    }
}
