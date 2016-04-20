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
package org.apache.sis.feature;

import java.util.Locale;
import org.opengis.util.InternationalString;
import org.apache.sis.internal.util.LocalizedException;


/**
 * Thrown when a feature fails at least one conformance test.
 *
 * <div class="note"><b>Note:</b>
 * this exception extends {@link InvalidPropertyValueException} because an Apache SIS feature
 * can be invalid only if a property is invalid.</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 *
 * @see Features#validate(Feature)
 */
final class InvalidFeatureException extends IllegalArgumentException implements LocalizedException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 7288810679876346027L;

    /**
     * A description of the negative conformance result.
     */
    private final InternationalString message;

    /**
     * Creates a new exception with the given explanation message.
     *
     * @param message  a description of the negative conformance result.
     */
    InvalidFeatureException(final InternationalString message) {
        super(message.toString());
        this.message = message;
    }

    /**
     * Returns the message localized in the given language, or in a default language if the requested
     * localization is not available.
     *
     * @param  locale  the desired language.
     * @return the message in the given locale, or in a default locale if the requested localization is not available.
     */
    @Override
    public String getLocalizedMessage(final Locale locale) {
        return message.toString(locale);
    }
}
