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

import java.util.Locale;


/**
 * An exception which can produce an error message in the given locale.
 * Exceptions implementing this interface uses the following policy:
 *
 * <ul>
 *   <li>{@link Throwable#getMessage()} returns the message in the {@linkplain Locale#getDefault() default locale}.
 *       In a client-server architecture, this is often the locale on the server side.</li>
 *   <li>{@link Throwable#getLocalizedMessage()} returns the message in the locale returned by the
 *       {@link #getLocale()} method. This is often the locale used by a {@link java.text.Format}
 *       object for example, and can be presumed to be the locale on the client side.</li>
 *   <li>{@link #getMessage(Locale)} returns the message in the given locale.
 *       This method is specific to Apache SIS however.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see Exceptions#getMessage(Throwable, Locale)
 */
interface LocalizedException extends Localized {
    /**
     * The locale of the string returned by {@link #getLocalizedMessage()}.
     *
     * @return The locale of the localized exception message.
     */
    @Override
    Locale getLocale();

    /**
     * Returns the message in the default locale.
     *
     * @return The exception message in the default locale.
     */
    String getMessage();

    /**
     * Returns the message in the locale specified by {@link #getLocale()}.
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
    String getMessage(Locale locale);
}
