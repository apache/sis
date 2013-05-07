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
package org.apache.sis.internal.util;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Localized;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Messages;


/**
 * Utilities methods for emitting warnings that may be of interest to the users.
 * The current implementation does not have any mechanism for registering listeners,
 * because we don't know yet what would be the most appropriate API. One possibility
 * would be as below, based on the observation that the final destination of a warning
 * is often the logger and that {@link LogRecord} provides many useful information like
 * the source class and the stack trace:
 *
 * {@preformat java
 *     public interface WarningListener extends EventListener {
 *         boolean warningOccurred(LogRecord record);
 *     }
 * }
 *
 * In the main time, this class is used as a central place where we can trace the code
 * that emit warnings of potential interest to the user.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.08)
 * @version 0.3
 * @module
 */
public final class WarningListeners {
    /**
     * A temporary variable for identifying places in the code that may need to be revisited if we
     * provide warning listeners. For example we do not log stack traces at this time for avoiding
     * to flood the logs, but we would want to provide the stack trace as information to listeners.
     */
    private static final boolean HAS_LISTENERS = false;

    /**
     * Do not allow instantiation of this class (for now - may change in a future version).
     */
    private WarningListeners() {
    }

    /**
     * Convenience method for reporting a warning for the given exception. If the given exception
     * has no message of a message containing only one word (e.g. {@link IndexOutOfBoundsException}
     * with only the index number), then this method tries to make the message more informative by
     * prepending the exception class name.
     *
     * @param  level     The logging level, or {@code null} for the default one.
     * @param  caller    The public class which is invoking this method.
     * @param  method    The public method which is invoking this method.
     * @param  exception The exception to log.
     */
    public static void warningOccurred(Level level, final Class<?> caller, final String method, final Exception exception) {
        if (level == null) {
            level = Level.WARNING;
        }
        String message = exception.getLocalizedMessage();
        if (message == null || message.indexOf(' ') < 0) {
            final String word = message;
            message = Classes.getShortClassName(exception);
            if (word != null) {
                message = message + ": " + word;
            }
        }
        final LogRecord record = new LogRecord(level, message);
        if (HAS_LISTENERS) {
            record.setThrown(exception);
        }
        Logging.log(caller, method, record);
    }

    /**
     * Convenience method for logging a warning with the given message.
     *
     * @param  level     The logging level, or {@code null} for the default one.
     * @param  caller    The public class which is invoking this method.
     * @param  method    The public method which is invoking this method.
     * @param  message   The message to log.
     */
    public static void warningOccurred(Level level, final Class<?> caller, final String method, final String message) {
        if (level == null) {
            level = Level.WARNING;
        }
        final LogRecord record = new LogRecord(level, message);
        Logging.log(caller, method, record);
    }

    /**
     * Convenience method for logging a warning with the given localized message.
     * The message will be produced using the {@link Messages} resources bundle.
     *
     * @param  level     The logging level, or {@code null} for the default one.
     * @param  instance  The instance invoking this method, or {@code null}.
     * @param  caller    The public class which is invoking this method.
     * @param  method    The public method which is invoking this method.
     * @param  key       The key from the message resource bundle to use for creating a message.
     * @param  arguments The arguments to be used together with the key for building the message.
     */
    public static void message(Level level, final Localized instance, final Class<?> caller, final String method,
            final int key, final Object... arguments)
    {
        if (level == null) {
            level = Level.WARNING;
        }
        final Locale locale = (instance != null) ? instance.getLocale() : null;
        final LogRecord record = Messages.getResources(locale).getLogRecord(level, key, arguments);
        Logging.log(caller, method, record);
    }
}
