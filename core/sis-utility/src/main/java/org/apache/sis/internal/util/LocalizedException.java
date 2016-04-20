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
package org.apache.sis.internal.util;

import java.util.Locale;
import org.apache.sis.util.Localized;


/**
 * An exception which can produce an error message in the given locale.
 * Exceptions implementing this interface use the following policy:
 *
 * <ul>
 *   <li>{@link Throwable#getMessage()} returns the message in the {@linkplain Locale#getDefault() default locale}.
 *       In a client-server architecture, this is often the locale on the server side.</li>
 *   <li>{@link Throwable#getLocalizedMessage()} returns the message in a locale that depends on the context
 *       in which the exception has been thrown. This is often the locale used by a {@link java.text.Format}
 *       object for example, and can be presumed to be the locale on the client side.</li>
 *   <li>{@link #getLocalizedMessage(Locale)} returns the message in the given locale.
 *       This method is specific to Apache SIS however.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 *
 * @see org.apache.sis.util.Exceptions#getLocalizedMessage(Throwable, Locale)
 */
public interface LocalizedException {
    /**
     * Returns the message in the {@linkplain Locale#getDefault() default locale}.
     *
     * @return The exception message in the default locale.
     */
    String getMessage();

    /**
     * Returns the message in the locale that depends on the context in which this exception has been thrown.
     * For example it may be the local of a client application connected to a distant server.
     *
     * <p>If the context locale is known, then this {@code LocalizedException} instance will also implement
     * the {@link Localized} interface and the context locale can be obtained by a call to
     * {@link Localized#getLocale()}.</p>
     *
     * @return The localized exception message.
     */
    String getLocalizedMessage();

    /**
     * Returns the message in the given locale.
     *
     * @param  locale The locale of the message to produce, or {@code null} for the default locale.
     * @return The exception message in the given locale.
     */
    String getLocalizedMessage(Locale locale);
}
