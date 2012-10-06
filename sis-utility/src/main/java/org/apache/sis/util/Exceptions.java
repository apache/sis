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

import java.sql.SQLException;
import java.util.Set;
import java.util.HashSet;


/**
 * Static methods working with {@link Exception} instances.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-2.0)
 * @version 0.3
 * @module
 */
public final class Exceptions extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private Exceptions() {
    }

    /**
     * Returns an exception of the same kind and with the same stack trace than the given
     * exception, but with a different message. This method simulates the functionality
     * that we would have if {@link Throwable} defined a {@code setMessage(String)} method.
     * We use this method when an external library throws an exception of the right type,
     * but with too few details.
     * <p>
     * This method tries to create a new exception using reflection. The exception class needs
     * to provide a public constructor expecting a single {@link String} argument. If the
     * exception class does not provide such constructor, then the given exception is returned
     * unchanged.
     *
     * @param <T>       The type of the exception.
     * @param exception The exception to copy with a different message.
     * @param message   The message to set in the exception to be returned.
     * @param append    If {@code true}, the existing message in the original exception (if any)
     *                  will be happened after the provided message.
     * @return A new exception with the given message, or the given exception if the exception
     *         class does not provide public {@code Exception(String)} constructor.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Throwable> T setMessage(final T exception, String message, final boolean append) {
        if (append) {
            String em = exception.getLocalizedMessage();
            if (em != null && !(em = em.trim()).isEmpty()) {
                final StringBuilder buffer = new StringBuilder(message.trim());
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
        } catch (Exception e) { // Too many exception for listing them all.
            return exception;
        }
        ne.setStackTrace(exception.getStackTrace());
        return (T) ne;
    }

    /**
     * Returns a string which contain the given message on the first line, followed by the
     * {@linkplain Throwable#getLocalizedMessage() localized message} of the given exception
     * on the next line. If the exception has a {@linkplain Throwable#getCause() causes}, then
     * the localized message of the cause is formatted on the next line and the process is
     * repeated for the whole cause chain.
     * <p>
     * {@link SQLException} is handled especially in order to process the
     * {@linkplain SQLException#getNextException() next exception} instead than the cause.
     * <p>
     * This method does not format the stack trace.
     *
     * @param  header The message to insert on the first line, or {@code null} if none.
     * @param  cause  The exception, or {@code null} if none.
     * @return The formatted message, or {@code null} if both the header was {@code null}
     *         and no exception provide a message.
     */
    public static String formatChainedMessages(String header, Throwable cause) {
        Set<String> done = null;
        String lineSeparator = null;
        StringBuilder buffer = null;
        while (cause != null) {
            String message = cause.getLocalizedMessage();
            if (message != null && !(message = message.trim()).isEmpty()) {
                if (buffer == null) {
                    done = new HashSet<>();
                    buffer = new StringBuilder(128);
                    lineSeparator = System.getProperty("line.separator", "\n");
                    if (header != null && !(header = header.trim()).isEmpty()) {
                        buffer.append(header);
                        done.add(header);
                        /*
                         * The folowing is for avoiding to repeat the same message in the
                         * common case where the header contains the exception class name
                         * followed by the message, as in:
                         *
                         * FooException: InnerException: the inner message.
                         */
                        int s=0;
                        while ((s=header.indexOf(':', s)) >= 0) {
                            done.add(header.substring(++s).trim());
                        }
                    }
                }
                if (done.add(message)) {
                    if (buffer.length() != 0) {
                        buffer.append(lineSeparator);
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
        if (buffer != null) {
            header = buffer.toString();
        }
        return header;
    }
}
