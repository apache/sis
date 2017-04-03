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
import org.opengis.util.InternationalString;


/**
 * An exception which can produce an error message in the client locale.
 * Exceptions implementing this interface apply the following policy:
 *
 * <ul>
 *   <li>{@link #getMessage()} returns the message in the {@linkplain Locale#getDefault() default locale}.
 *       In a client-server architecture, this is often the locale on the server side.</li>
 *   <li>{@link #getLocalizedMessage()} returns the message in a locale that depends on the context
 *       in which the exception has been thrown. This is often the locale used by a {@link java.text.Format}
 *       or {@link org.apache.sis.storage.DataStore} instance,
 *       and can be presumed to be the locale on the client side.</li>
 *   <li>{@link #getInternationalMessage()} may return the message in arbitrary locale (optional operation).
 *       This method is specific to Apache SIS.</li>
 * </ul>
 *
 * <div class="note"><b>Example:</b>
 * if an error occurred while a Japanese client connected to an European server, the localized message may be sent
 * to the client in Japanese language while the same error may be logged on the server side in the French language.
 * This allows system administrator to analyze the issue without the need to understand client's language.</div>
 *
 * The above policy is applied on a <em>best-effort</em> basis only. For example exceptions that
 * do not implement {@code LocalizedException} may use any locale (not necessarily the default one),
 * and {@code LocalizedException} used as {@linkplain Exception#Exception(Throwable) wrappers} around
 * other exception usually lost their localization capability.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 *
 * @see Exceptions#getLocalizedMessage(Throwable, Locale)
 * @see org.apache.sis.storage.DataStore#setLocale(Locale)
 *
 * @since 0.8
 * @module
 */
public interface LocalizedException {
    /**
     * Returns the message in the {@linkplain Locale#getDefault() default locale}.
     * In a client-server architecture, this is usually the locale on the server side.
     * This is the recommended language for logging messages to be read by system administrators.
     *
     * @return the exception message in the JVM {@linkplain Locale#getDefault() default locale}.
     */
    String getMessage();

    /**
     * Returns the message in the locale that depends on the context in which this exception has been thrown.
     * For example it may be the local of a client application connected to a distant server.
     * This is the recommended language to show in widgets.
     *
     * @return the exception message in the locale of a service configured for a particular client.
     */
    String getLocalizedMessage();

    /**
     * If this exception is capable to return the message in various locales, returns that message.
     * Otherwise returns {@code null}.
     *
     * @return the exception message, or {@code null} if this exception can not produce international message.
     */
    InternationalString getInternationalMessage();
}
