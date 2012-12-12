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
package org.apache.sis.xml;

import java.util.UUID;
import java.util.Locale;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.LocalizedException;


/**
 * Throws when an object is given a {@code UUID} which is already assigned to another object.
 * While the same XML identifier can be used in different documents, {@link UUID}s are expected
 * to be truly unique.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public class IdentifierAlreadyBoundException extends IllegalArgumentException implements LocalizedException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7571733911210491933L;

    /**
     * The locale to use for formatting the {@linkplain #getLocalizedMessage() localized error message}.
     */
    private final Locale locale;

    /**
     * The identifier which is already associated to another object.
     * May be an instance of {@link UUID} or {@link XLink}.
     *
     * (Current constructor accepts only UUID, but we may
     * expand the set of allowed types in a future version).
     */
    private final Object identifier;

    /**
     * Creates a new exception for the given identifier.
     *
     * @param locale     The locale to use for the {@linkplain #getLocalizedMessage() localized message}.
     * @param identifier The identifier which is already associated to another object.
     */
    public IdentifierAlreadyBoundException(final Locale locale, final UUID identifier) {
        super(Errors.format(Errors.Keys.IdentifierAlreadyBound_1, identifier));
        this.locale     = locale;
        this.identifier = identifier;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Locale getLocale() {
        return locale;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLocalizedMessage() {
        return Errors.getResources(locale).getString(Errors.Keys.IdentifierAlreadyBound_1, identifier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLocalizedMessage(final Locale locale) {
        return Errors.getResources(locale).getString(Errors.Keys.IdentifierAlreadyBound_1, identifier);
    }
}
