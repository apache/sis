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
import java.util.Objects;
import java.sql.SQLException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryIteratorException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;
import org.opengis.util.InternationalString;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.collection.BackingStoreException;


/**
 * Static methods working with {@link Exception} instances.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.3
 * @since   0.3
 */
public final class Exceptions {
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
     *                    This locale is honored on a <em>best effort</em> basis only.
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
     * Returns {@code true} if the given exceptions are of the same class and contains the same message.
     * This method does not compare the {@linkplain Throwable#getStackTrace() stack trace},
     * {@linkplain Throwable#getCause() cause} or {@linkplain Throwable#getSuppressed() suppressed exceptions}.
     *
     * @param  first   the first exception, or {@code null}.
     * @param  second  the second exception, or {@code null}.
     * @return {@code true} if both exceptions are {@code null}, or both exceptions are non-null,
     *         of the same class and with the same {@linkplain Throwable#getMessage() message}.
     *
     * @since 1.0
     */
    public static boolean messageEquals(final Throwable first, final Throwable second) {
        if (first == second) {
            return true;
        }
        if (first == null || second == null) {
            return false;
        }
        return first.getClass() == second.getClass() && Objects.equals(first.getMessage(), second.getMessage());
    }

    /**
     * Returns a string which contains the given message on the first line,
     * followed by the localized message of the given exception on the next line.
     * If the exception has a {@linkplain Throwable#getCause() causes}, then the class name and the
     * {@linkplain #getLocalizedMessage(Throwable, Locale) localized message} of the cause are formatted
     * on the next line. The process is repeated for the whole cause chain, omitting duplicated messages.
     * This method does not format the stack trace.
     *
     * <h4>Special cases</h4>
     * {@link SQLException} is handled is a special way by giving precedence to
     * {@link SQLException#getNextException()} over {@link Throwable#getCause()}.
     *
     * <h4>When to use</h4>
     * This method should not be used when the given exception will be reported through, for example,
     * {@link Throwable#initCause(Throwable)} or {@link java.util.logging.LogRecord#setThrown(Throwable)},
     * because the redundancy may be confusing. This method is rather for situations where the exception
     * will be discarded or hidden.
     *
     * @param  locale  the preferred locale for the exception message, or {@code null}.
     * @param  header  the message to insert on the first line, or {@code null} if none.
     * @param  cause   the exception, or {@code null} if none.
     * @return the formatted message, or {@code null} if both the header was {@code null}
     *         and no exception provide a message.
     */
    public static String formatChainedMessages(final Locale locale, final String header, Throwable cause) {
        final var previousLines = new ArrayList<String>();
        StringBuilder buffer = null;
        Vocabulary resources = null;
        while (cause != null) {
            String message = getLocalizedMessage(cause, locale);
            if (message != null && !(message = message.strip()).isEmpty()) {
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
                            resources = Vocabulary.forLocale(locale);
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

    /**
     * If the given exception is a wrapper for another exception, returns the unwrapped exception.
     * Otherwise returns the given argument unchanged. An exception is considered a wrapper if:
     *
     * <ul>
     *   <li>It is an instance of {@link InvocationTargetException} (could be wrapping anything).</li>
     *   <li>It is an instance of {@link ExecutionException} (could be wrapping anything).</li>
     *   <li>It is an instance of {@link BackingStoreException} (typically wrapping a checked exception).</li>
     *   <li>It is an instance of {@link UncheckedIOException} (wrapping a {@link java.io.IOException}).</li>
     *   <li>It is an instance of {@link DirectoryIteratorException} (wrapping a {@link java.io.IOException}).</li>
     *   <li>It is a parent type of its cause. For example, some JDBC drivers wrap {@link SQLException} in another
     *       {@code SQLException} without additional information. When the wrapper is a parent class of the cause,
     *       details about the reason are less accessible.</li>
     * </ul>
     *
     * This method uses only the exception class as criterion;
     * it does not verify if the exception messages are the same.
     *
     * @param  exception  the exception to unwrap (may be {@code null}.
     * @return the unwrapped exception (may be the given argument itself).
     *
     * @since 0.8
     */
    public static Exception unwrap(Exception exception) {
        if (exception != null) {
            while (exception instanceof InvocationTargetException ||
                   exception instanceof ExecutionException ||
                   exception instanceof BackingStoreException ||
                   exception instanceof UncheckedIOException ||
                   exception instanceof DirectoryIteratorException)
            {
                final Throwable cause = exception.getCause();
                if (!(cause instanceof Exception)) break;
                copySuppressed(exception, cause);
                exception = (Exception) cause;
            }
            Throwable cause;
            while (exception.getClass().isInstance(cause = exception.getCause())) {
                copySuppressed(exception, cause);
                exception = (Exception) cause;      // Should never fail because of isInstance(…) check.
            }
        }
        return exception;
    }

    /**
     * Unwraps and copies suppressed exceptions from the given source to the given target.
     *
     * @param  source  the exception from which to copy suppressed exceptions.
     * @param  target  the exception where to add suppressed exceptions.
     */
    private static void copySuppressed(final Exception source, final Throwable target) {
        for (final Throwable suppressed : source.getSuppressed()) {
            target.addSuppressed(suppressed instanceof Exception ? unwrap((Exception) suppressed) : suppressed);
        }
    }
}
