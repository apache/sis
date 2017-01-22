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

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.sql.SQLException;
import org.opengis.util.InternationalString;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Static methods working with {@link Exception} instances.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.8
 * @module
 */
public final class Exceptions extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private Exceptions() {
    }

    /**
     * Returns the message of the given exception, localized in the given locale if possible.
     * Some exceptions created by SIS can format a message in different locales. This method
     * returns such localized message if possible, or fallback on the standard JDK methods otherwise.
     * More specifically:
     *
     * <ul>
     *   <li>If the given {@code exception} is null, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given {@code locale} is null, then this method returns {@link Exception#getMessage()}.
     *       This is consistent with the {@link Localized} policy saying that null locale stands for "unlocalized"
     *       message (usually in English) or message in the JVM {@linkplain Locale#getDefault() default locale}.</li>
     *   <li>Otherwise if the given {@code exception} is an instance of {@link LocalizedException} providing
     *       a non-null {@linkplain LocalizedException#getInternationalMessage() international message},
     *       then this method returns the result of {@link InternationalString#toString(Locale)}.</li>
     *   <li>Otherwise this method returns {@link Exception#getLocalizedMessage()}.</li>
     * </ul>
     *
     * @param  exception  the exception from which to get the localize message, or {@code null}.
     * @param  locale     the preferred locale for the message, or {@code null} for the JVM default locale.
     *                    This locale is honored on a <cite>best-effort</cite> basis only.
     * @return the message in the given locale if possible, or {@code null} if the {@code exception}
     *         argument was {@code null} or if the exception does not contain a message.
     *
     * @see LocalizedException#getLocalizedMessage()
     */
    public static String getLocalizedMessage(final Throwable exception, final Locale locale) {
        if (exception == null) {
            return null;
        }
        if (locale == null) {
            return exception.getMessage();      // See the policy documented in LocalizedException.getMessage()
        }
        if (exception instanceof LocalizedException) {
            final InternationalString i18n = ((LocalizedException) exception).getInternationalMessage();
            if (i18n != null) {
                final String message = i18n.toString(locale);
                if (message != null) {
                    return message;
                }
            }
        }
        return exception.getLocalizedMessage();
    }

    /**
     * Returns an exception of the same kind and with the same stack trace than the given
     * exception, but with a different message. This method simulates the functionality
     * that we would have if {@link Throwable} defined a {@code setMessage(String)} method.
     * We use this method when an external library throws an exception of the right type,
     * but with too few details.
     *
     * <p>This method tries to create a new exception using reflection. The exception class needs
     * to provide a public constructor expecting a single {@link String} argument. If the
     * exception class does not provide such constructor, then the given exception is returned
     * unchanged.</p>
     *
     * @param <T>        the type of the exception.
     * @param exception  the exception to copy with a different message.
     * @param message    the message to set in the exception to be returned.
     * @param append     if {@code true}, the existing message in the original exception (if any)
     *                   will be happened after the provided message.
     * @return a new exception with the given message, or the given exception if the exception
     *         class does not provide public {@code Exception(String)} constructor.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Throwable> T setMessage(final T exception, String message, final boolean append) {
        if (append) {
            final String em = CharSequences.trimWhitespaces(exception.getLocalizedMessage());
            if (em != null && !em.isEmpty()) {
                final StringBuilder buffer = new StringBuilder(CharSequences.trimWhitespaces(message));
                final int length = buffer.length();
                if (length != 0 && Character.isLetterOrDigit(buffer.charAt(length-1))) {
                    buffer.append(". ");
                }
                message = buffer.append(em).toString();
            }
        }
        final Throwable ne;
        try {
            ne = exception.getClass().getConstructor(String.class).newInstance(message);
        } catch (ReflectiveOperationException e) {
            return exception;
        }
        ne.setStackTrace(exception.getStackTrace());
        return (T) ne;
    }

    /**
     * Returns a string which contain the given message on the first line, followed by the
     * {@linkplain #getLocalizedMessage(Throwable, Locale) localized message} of the given exception
     * on the next line. If the exception has a {@linkplain Throwable#getCause() causes}, then
     * the class name and the localized message of the cause are formatted on the next line
     * and the process is repeated for the whole cause chain, omitting duplicated messages.
     *
     * <p>{@link SQLException} is handled especially in order to process the
     * {@linkplain SQLException#getNextException() next exception} instead than the cause.</p>
     *
     * <p>This method does not format the stack trace.</p>
     *
     * @param  locale  the preferred locale for the exception message, or {@code null}.
     * @param  header  the message to insert on the first line, or {@code null} if none.
     * @param  cause   the exception, or {@code null} if none.
     * @return the formatted message, or {@code null} if both the header was {@code null}
     *         and no exception provide a message.
     */
    public static String formatChainedMessages(final Locale locale, final String header, Throwable cause) {
        final List<String> previousLines = new ArrayList<>();
        StringBuilder buffer = null;
        Vocabulary resources = null;
        while (cause != null) {
            final String message = CharSequences.trimWhitespaces(getLocalizedMessage(cause, locale));
            if (message != null && !message.isEmpty()) {
                if (buffer == null) {
                    buffer = new StringBuilder(128);
                    if (header != null) {
                        final int length = CharSequences.skipTrailingWhitespaces(header, 0, header.length());
                        if (length > 0) {
                            buffer.append(header, 0, length);
                        }
                        previousLines.add(header);
                    }
                }
                if (!contains(previousLines, message)) {
                    previousLines.add(message);
                    if (buffer.length() != 0) {
                        if (resources == null) {
                            resources = Vocabulary.getResources(locale);
                        }
                        buffer.append(System.lineSeparator())
                              .append(resources.getString(Vocabulary.Keys.CausedBy_1, cause.getClass()))
                              .append(": ");
                    }
                    buffer.append(message);
                }
            }
            if (cause instanceof SQLException) {
                final SQLException next = ((SQLException) cause).getNextException();
                if (next != null) {
                    cause = next;
                    continue;
                }
            }
            cause = cause.getCause();
        }
        return (buffer != null) ? buffer.toString() : header;
    }

    /**
     * Returns {@code true} if a previous line contains the given exception message.
     */
    private static boolean contains(final List<String> previousLines, final String message) {
        for (int i = previousLines.size(); --i >= 0;) {
            if (previousLines.get(i).contains(message)) {
                return true;
            }
        }
        return false;
    }
}
