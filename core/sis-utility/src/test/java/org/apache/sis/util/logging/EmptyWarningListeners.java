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
package org.apache.sis.util.logging;

import java.util.Locale;
import java.util.logging.Logger;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;


/**
 * A unmodifiable empty list of listeners. Calls to {@link #addWarningListener(WarningListener) addWarningListener(…)}
 * will throw {@link UnsupportedOperationException}. Since this listener list is empty, it doesn't need a source.
 *
 * <p>This class is used in some modules like {@code sis-netcdf}, when a JUnit test is testing some low-level
 * component where the real {@link WarningListeners} instance is not yet available.</p>
 *
 * @param <S> If the listener list had a source, that would be type type of the source.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final strictfp class EmptyWarningListeners<S> extends WarningListeners<S> {
    /**
     * The locale to be returned by {@link #getLocale()}. Can be {@code null}.
     */
    private final Locale locale;

    /**
     * The logger to be returned by {@link #getLogger()}.
     */
    private final Logger logger;

    /**
     * Creates a new instance for the given locale and logger.
     *
     * @param locale The locale to be returned by {@link #getLocale()}. Can be {@code null}.
     * @param logger The name of the logger to be returned by {@link #getLogger()}.
     */
    public EmptyWarningListeners(final Locale locale, final String logger) {
        ArgumentChecks.ensureNonNull("logger", logger);
        this.locale = locale;
        this.logger = Logging.getLogger(logger);
    }

    /** Returns the value given at construction time. */ @Override public Locale getLocale() {return locale;}
    /** Returns the value given at construction time. */ @Override public Logger getLogger() {return logger;}

    /** Do not allow registration of warning listeners. */
    @Override public void addWarningListener(WarningListener<? super S> listener) {
        throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnmodifiableObject_1, "WarningListeners"));
    }
}
