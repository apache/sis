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
package org.apache.sis.xml.internal.shared;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.xml.sax.SAXParseException;
import org.apache.sis.system.Loggers;
import org.apache.sis.xml.bind.Context;
import org.apache.sis.util.resources.Errors;


/**
 * Simplifies an exception before to log it. This class reduces log flooding when an exception has a very long message
 * (e.g. JAXB enumerating all elements that it was expecting) and that long message is repeated in the exception cause.
 * This class also handles the identification of the location where the error occurred.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ExceptionSimplifier {
    /**
     * The key to use for producing an error message, or 0 for using the exception message.
     * Shall be a constant from {@link Errors.Keys}.
     */
    private short errorKey;

    /**
     * The values to insert in the error message, or {@code null} for using the exception message.
     */
    private Object[] errorValues;

    /**
     * The exception.
     */
    public final Exception exception;

    /**
     * Simplifies the given exception if possible, and produces an error message.
     *
     * @param source     the source (URI or Path) that couldn't be parsed, or {@code null} if unknown.
     * @param exception  the error that occurred while parsing the source.
     */
    public ExceptionSimplifier(Object source, Exception exception) {
        int line = -1, column = -1;
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SAXParseException) {
                var s = (SAXParseException) cause;
                if ((line | column) < 0) {
                    line = s.getLineNumber();
                    column = s.getColumnNumber();
                }
                if (source == null) {
                    source = s.getPublicId();
                    if (source == null) {
                        source = s.getSystemId();
                    }
                }
            }
            if (cause != exception) {
                final String msg = exception.getMessage();
                if (msg != null) {
                    final String s = cause.getMessage();
                    if (s != null && !msg.contains(s)) break;
                }
                if (exception.getClass().isInstance(cause)) {
                    exception = (Exception) cause;
                }
            }
        }
        this.exception = exception;
        if (source == null) {
            final ExternalLinkHandler handler = Context.linkHandler(Context.current());
            if (handler != null) {
                source = handler.getBase();
            }
        }
        if (source != null) {
            if ((line | column) < 0) {
                errorKey = Errors.Keys.CanNotRead_1;
                errorValues = new Object[] {source};
            } else {
                errorKey = Errors.Keys.CanNotRead_3;
                errorValues = new Object[] {source, line, column};
            }
        }
    }

    /**
     * Returns the error message.
     *
     * @param  locale  desired locale for the error message.
     * @return the error message, or {@code null} if none.
     */
    public String getMessage(final Locale locale) {
        if (errorKey != 0) {
            return Errors.forLocale(locale).getString(errorKey, errorValues);
        } else {
            return exception.getMessage();
        }
    }

    /**
     * Sends the exception to the warning listener if there is one, or logs the warning otherwise.
     * In the latter case, this method logs to {@link Context#LOGGER}.
     *
     * @param  context  the current context, or {@code null} if none.
     * @param  classe   the class to declare as the warning source.
     * @param  method   the name of the method to declare as the warning source.
     */
    public void report(final Context context, final Class<?> classe, final String method) {
        Context.warningOccured(context, Level.WARNING, classe, method, exception,
                (errorKey != 0) ? Errors.class : null, errorKey, errorValues);
    }

    /**
     * Creates a log record for the warning.
     *
     * @param  classe  the class to declare as the warning source.
     * @param  method  the name of the method to declare as the warning source.
     * @return the log record.
     */
    public LogRecord record(final Class<?> classe, final String method) {
        final LogRecord record;
        if (errorKey != 0) {
            record = Errors.forLocale(null).createLogRecord(Level.WARNING, errorKey, errorValues);
        } else {
            record = new LogRecord(Level.WARNING, exception.getMessage());
        }
        record.setLoggerName(Loggers.XML);
        record.setSourceClassName(classe.getCanonicalName());
        record.setSourceMethodName(method);
        record.setThrown(exception);
        return record;
    }
}
