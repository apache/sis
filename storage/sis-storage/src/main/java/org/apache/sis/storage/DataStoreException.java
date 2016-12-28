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
 * Thrown when a {@link DataStore} can not complete a read or write operation.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.8
 * @module
 */
public class DataStoreException extends Exception {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1778987176103191950L;

    /**
     * The locale to use for formatting the localized error message, or {@code null} for the default.
     */
    private final Locale locale;

    /**
     * The resources key as one of the {@code Resources.Keys} constant, or 0 if none.
     */
    private final short key;

    /**
     * The arguments for the localization message, or {@code null} if none.
     */
    private final Object[] arguments;

    /**
     * Creates an exception with no cause and no details message.
     */
    public DataStoreException() {
        super();
        locale    = null;
        key       = 0;
        arguments = null;
    }

    /**
     * Creates an exception with the specified details message.
     *
     * @param message  the detail message.
     */
    public DataStoreException(final String message) {
        super(message);
        locale    = null;
        key       = 0;
        arguments = null;
    }

    /**
     * Creates an exception with the specified cause and no details message.
     *
     * @param cause  the cause for this exception.
     */
    public DataStoreException(final Throwable cause) {
        super(cause);
        locale    = null;
        key       = 0;
        arguments = null;
    }

    /**
     * Creates an exception with the specified details message and cause.
     *
     * @param message  the detail message.
     * @param cause    the cause for this exception.
     */
    public DataStoreException(final String message, final Throwable cause) {
        super(message, cause);
        locale    = null;
        key       = 0;
        arguments = null;
    }

    /**
     * Creates a new exception which will format a localized message in the given locale.
     *
     * @param locale     the locale for the message to be returned by {@link #getLocalizedMessage()}.
     * @param key        one of {@link Resources.Keys} constants.
     * @param arguments  arguments to use for formatting the messages.
     */
    DataStoreException(final Locale locale, final short key, final Object... arguments) {
        super(Resources.format(key, arguments));
        this.locale    = locale;
        this.key       = key;
        this.arguments = arguments;
    }

    /**
     * Returns a localized version of the exception message, typically for final user.
     * This is often the same message than the one returned by {@link #getMessage()},
     * but may in some occasions be different if {@link DataStore#setLocale(Locale)}
     * has been invoked with a different locale.
     *
     * <div class="section">{@code getMessage()} versus {@code getLocalizedMessage()}</div>
     * When {@code getMessage()} and {@code getLocalizedMessage()} are not equivalent, the Apache SIS policy
     * is that {@code getMessage()} returns the message in the {@linkplain Locale#getDefault() default locale}
     * while {@code getLocalizedMessage()} returns the message in a locale that depends on the context in which
     * the exception has been thrown.
     *
     * In a client-server architecture, the former is often the locale on the <em>server</em> side while the later
     * is the locale on the <em>client</em> side if that information has been provided to the {@link DataStore}.
     * {@code getMessage()} is targeted to the developer would will analyze the stack trace while
     * {@code getLocalizedMessage()} is targeted to the final user would may receive the error message
     * without stack trace.
     *
     * @return the localized message of this exception.
     *
     * @see DataStore#setLocale(Locale)
     *
     * @since 0.8
     */
    @Override
    public String getLocalizedMessage() {
        if (key != 0) {
            return Resources.forLocale(locale).getString(key, arguments);
        }
        return super.getLocalizedMessage();
    }
}
