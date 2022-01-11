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
package org.apache.sis.referencing.operation.builder;

import org.opengis.util.InternationalString;
import org.apache.sis.referencing.factory.FactoryDataException;


/**
 * Thrown when a localization grid can not be computed, presumably because of a problem with grid data.
 * It may be because some grid coordinates are out of CRS domain of validity, causing either
 * {@link org.opengis.referencing.operation.MathTransform} to be thrown or {@link Double#NaN}
 * coordinate values to be computed.
 *
 * <h2>Additional information on exception cause</h2>
 * It is sometime difficult to determine the root cause of this exception.
 * For example grid points slightly outside the CRS domain of validity will not necessarily cause a failure.
 * A strategy can be to try to build the grid anyway, and in case of failure declare that the grid was maybe
 * too far from CRS domain of validity. Because the potential causes are better known by the code that wants
 * a localization grid instead of the {@link LocalizationGridBuilder} class, {@code LocalizationGridException}
 * provides a {@link #setPotentialCause(CharSequence)} method for allowing top-level code to attach additional
 * information to this exception.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 */
public class LocalizationGridException extends FactoryDataException {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -9069664783475360076L;

    /**
     * Additional information about what may be the cause of this exception.
     * Example: <cite>"The grid spans more than 180° of longitude"</cite>,
     * which may be a cause of map projection failures.
     *
     * @see #getPotentialCause()
     */
    private CharSequence potentialCause;

    /**
     * Construct an exception with no detail message.
     */
    public LocalizationGridException() {
    }

    /**
     * Constructs an exception with the specified detail message.
     *
     * @param message  the detail message, saved for later retrieval by the {@link #getMessage()} method.
     */
    public LocalizationGridException(String message) {
        super(message);
    }

    /**
     * Constructs an exception with the specified cause.
     *
     * @param cause  the cause, saved for later retrieval by the {@link #getCause()} method.
     */
    public LocalizationGridException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs an exception with the specified detail message and cause.
     * The cause is the exception thrown in the underlying database
     * (e.g. {@link java.io.IOException} or {@link java.sql.SQLException}).
     *
     * @param message  the detail message, saved for later retrieval by the {@link #getMessage()} method.
     * @param cause    the cause, saved for later retrieval by the {@link #getCause()} method.
     */
    public LocalizationGridException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Specifies additional information about what may be the cause of this exception.
     * Example: <cite>"The grid spans more than 180° of longitude"</cite>,
     * which may be a cause of map projection failures.
     *
     * @param  details  a potential cause, or {@code null} if none.
     *         The type should be {@link String} or {@link InternationalString}.
     */
    public synchronized void setPotentialCause(CharSequence details) {
        potentialCause = details;
    }

    /**
     * Returns the value given to the last call of {@link #setPotentialCause(CharSequence)}.
     *
     * @return potential cause, or {@code null} if none.
     *         The type should be {@link String} or {@link InternationalString}.
     */
    public synchronized CharSequence getPotentialCause() {
        return potentialCause;
    }
}
