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
import org.apache.sis.util.resources.Vocabulary;


/**
 * Thrown when an invalid name is used for identifying a coverage, a feature or other kind of element in a data store.
 * A name may be invalid because no coverage or feature exists in the {@link DataStore} for that name,
 * or because the name is ambiguous (in which case the exception message should explain why),
 * or because the name of a new {@linkplain org.apache.sis.feature.DefaultFeatureType feature type}
 * to add in the {@code DataStore} conflicts with the name of an existing feature type.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see DataStore#findResource(String)
 *
 * @version 0.8
 * @since   0.8
 * @module
 */
public class IllegalNameException extends NoSuchDataException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2435437568097737351L;

    /**
     * Creates an exception with no cause and no details message.
     */
    public IllegalNameException() {
    }

    /**
     * Creates an exception with the specified details message.
     *
     * @param message  the detail message.
     */
    public IllegalNameException(final String message) {
        super(message);
    }

    /**
     * Creates an exception with the specified cause and no details message.
     *
     * @param cause  the cause for this exception.
     */
    public IllegalNameException(final Throwable cause) {
        super(cause);
    }

    /**
     * Creates an exception with the specified details message and cause.
     *
     * @param message  the detail message.
     * @param cause    the cause for this exception.
     */
    public IllegalNameException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new exception with a message saying that the feature of the given name has not been found.
     *
     * @param locale   the locale for the message to be returned by {@link #getLocalizedMessage()}.
     * @param store    name of the data store for which the feature has not been found, or {@code null} if unknown.
     * @param feature  name of the feature that has not been found.
     */
    public IllegalNameException(final Locale locale, final String store, final String feature) {
        super(locale, Resources.Keys.FeatureNotFound_2, (store != null) ? store
                : Vocabulary.formatInternational(Vocabulary.Keys.Unnamed), feature);
    }

    /**
     * Creates a new exception which will format a localized message in the given locale.
     *
     * @param locale      the locale for the message to be returned by {@link #getLocalizedMessage()}.
     * @param key         one of {@link Resources.Keys} constants.
     * @param parameters  parameters to use for formatting the messages.
     */
    IllegalNameException(final Locale locale, final short key, final Object... parameters) {
        super(locale, key, parameters);
    }
}
