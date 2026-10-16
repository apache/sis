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
package org.apache.sis.metadata;

import java.util.Locale;
import org.apache.sis.util.Localized;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.resources.Errors;


/**
 * Thrown when a {@link MetadataVisitor#visit(Class, Object)} method failed.
 * The details message is created only when requested.
 * The context-sensitive locale is inherited from the cause.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class MetadataVisitorException extends BackingStoreException implements Localized {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3779183393705626697L;

    /**
     * Path to the element that we failed to process.
     */
    private final String[] propertyPath;

    /**
     * Constructs a new exception with the specified cause.
     *
     * @param path   path to the element that we failed to process.
     * @param type   the class that was visited when the exception occurred.
     * @param cause  the cause, saved for later retrieval by the {@link #getCause()} method.
     */
    public MetadataVisitorException(final String[] path, final Class<?> type, final Exception cause) {
        super(type.getSimpleName(), cause);
        propertyPath = path;
    }

    /**
     * Returns an error message giving the location of the failure together with the cause.
     *
     * @return the detail message in the default locale.
     */
    @Override
    public String getMessage() {
        return getMessage(false);
    }

    /**
     * Returns an error message in a context-dependent locale if available.
     *
     * @return the detail message in the context-dependent locale.
     */
    @Override
    public String getLocalizedMessage() {
        return getMessage(true);
    }

    /**
     * Returns an error message giving the location of the failure together with the cause.
     *
     * @param  localized  whether to use context-dependent locale.
     * @return the detail message in the given locale.
     */
    private String getMessage(final boolean localized) {
        short key = Errors.Keys.CanNotProcessProperty_2;
        int count = 2;
        String location = super.getMessage();
        int pathLength = propertyPath.length;
        if (pathLength != 0) {
            location += '.' + propertyPath[--pathLength];
            if (pathLength != 0) {
                key = Errors.Keys.CanNotProcessPropertyAtPath_3;
                count = 3;
            }
        }
        final Throwable cause = getCause();
        Object message = localized ? cause.getLocalizedMessage() : cause.getMessage();
        if (message == null) {
            message = cause.getClass();
        }
        final var arguments = new Object[count];
        arguments[--count] = message;
        arguments[--count] = location;
        if (count != 0) {
            arguments[0] = String.join(".", Containers.viewAsUnmodifiableList(propertyPath, 0, pathLength));
        }
        return Errors.forLocale(getLocale()).getString(key, arguments);
    }

    /**
     * Returns the locale of the localized exception message, or {@code null} if unspecified.
     * If non-null, this is the effective language of the string returned by {@link #getLocalizedMessage()}.
     *
     * @return the locale of the localized message, or {@code null} if not explicitly defined.
     */
    @Override
    public Locale getLocale() {
        final Throwable cause = getCause();
        return (cause instanceof Localized) ? ((Localized) cause).getLocale() : null;
    }
}
