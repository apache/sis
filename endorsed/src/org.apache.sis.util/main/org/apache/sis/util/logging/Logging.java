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

import java.util.Arrays;
import java.util.stream.Stream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Static;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.Classes;
import org.apache.sis.system.Modules;
import org.apache.sis.system.Configuration;
import org.apache.sis.system.SystemListener;


/**
 * A set of utilities method for configuring loggings in SIS.
 * This class provides also some convenience static methods, including:
 * <ul>
 *   <li>{@link #log(Class, String, LogRecord)} for {@linkplain LogRecord#setLoggerName(String) setting
 *       the logger name}, {@linkplain LogRecord#setSourceClassName(String) source class name} and
 *       {@linkplain LogRecord#setSourceMethodName(String) source method name} of the given record
 *       before to log it.</li>
 *   <li>{@link #unexpectedException(Logger, Class, String, Throwable)} for reporting an anomalous but
 *       nevertheless non-fatal exception.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   0.3
 */
public final class Logging extends Static {
    /**
     * The threshold at which {@link #unexpectedException(Logger, String, String, Throwable, Level)} shall
     * set the throwable in the {@link LogRecord}. For any record to be logged at a lower {@link Level},
     * the {@link LogRecord#setThrown(Throwable)} method will not be invoked.
     *
     * <p>The default value is 600, which is the {@link PerformanceLevel#PERFORMANCE} value.
     * This value is between {@link Level#FINE} (500) and {@link Level#CONFIG} (700).
     * Consequently, we will ignore the stack traces of recoverable failures, but will report
     * stack traces that may impact performance, configuration, or correctness.</p>
     */
    @Configuration
    private static final int LEVEL_THRESHOLD_FOR_STACKTRACE = 600;

    /**
     * Do not allow instantiation of this class.
     */
    private Logging() {
    }

    /**
     * Returns a logger for the package of the specified class. This convenience method invokes
     * {@link Logger#getLogger(String)} with the package name of the given class taken as the logger name.
     *
     * @param  source  the class which will emit a logging message.
     * @return a logger for the specified class.
     *
     * @since 1.0
     */
    public static Logger getLogger(final Class<?> source) {
        return Logger.getLogger(source.getPackageName());
    }

    /**
     * Completes the properties of the given record, then logs to the specified or inferred logger.
     * If the {@code logger} argument is {@code null}, the logger is inferred from the other arguments.
     * This convenience method performs the following steps:
     *
     * <ul>
     *   <li>{@linkplain LogRecord#setSourceClassName(String) Set the source class name}
     *       to the {@linkplain Class#getCanonicalName() canonical name} of the given class.</li>
     *   <li>{@linkplain LogRecord#setSourceMethodName(String) Set the source method name}
     *       to the given value.</li>
     *   <li>{@linkplain LogRecord#setLoggerName(String) Set the logger name}
     *       to the {@linkplain Logger#getName() name} of the given logger if non null.
     *       Otherwise use the {@linkplain Class#getPackageName() package name of the class}.</li>
     *   <li>{@linkplain Logger#log(LogRecord) Log} the modified record to the specified {@code logger} if non null,
     *       or to the logger specified by the {@linkplain LogRecord#getLoggerName() logger name} otherwise.</li>
     * </ul>
     *
     * @param  logger  the logger where to log, or {@code null} for inferring from the logger or package name.
     * @param  classe  the class to report as the source of the logging message.
     * @param  method  the method to report as the source of the logging message.
     * @param  record  the record to complete and log.
     *
     * @since 1.4
     */
    public static void completeAndLog(Logger logger, final Class<?> classe, final String method, final LogRecord record) {
        ArgumentChecks.ensureNonNull("record", record);
        if (logger != null) {
            record.setLoggerName(logger.getName());
        } else {
            final String loggerName = record.getLoggerName();
            if (loggerName == null) {
                logger = getLogger(classe);
                record.setLoggerName(logger.getName());
            } else {
                logger = Logger.getLogger(loggerName);
            }
        }
        if (classe != null && method != null) {
            record.setSourceClassName(classe.getCanonicalName());
            record.setSourceMethodName(method);
        } else try {
            /*
             * If the given class or method is null, infer them from stack trace. We do not document this feature
             * in public API because the rules applied here are heuristic and may change in any future SIS version.
             */
            final Logger fl = logger;       // Because lambda functions require final values.
            logger = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk((stream) ->
                    inferCaller(fl, (classe != null) ? classe.getCanonicalName() : null, method,
                            stream.filter((frame) -> Classes.isPublic(frame.getDeclaringClass()))
                                  .map((StackWalker.StackFrame::toStackTraceElement)), record));
        } catch (SecurityException e) {
            ignorableException(SystemListener.LOGGER, Logging.class, "completeAndLog", e);
        }
        logger.log(record);
    }

    /**
     * Sets the {@code LogRecord} source class and method names according values inferred from the given stack trace.
     * This method inspects the given stack trace, skips what looks like internal API based on heuristic rules, then
     * if some arguments are non-null tries to match them.
     *
     * <p>This method should be invoked only if at least one of {@code logger}, {@code classe} or {@code method}
     * is null.</p>
     *
     * @param  logger  where the log record will be sent after this method call, or {@code null} if unknown.
     * @param  classe  the name of the class to report in the log record, or {@code null} if unknown.
     * @param  method  the name of the method to report in the log record, or {@code null} if unknown.
     * @param  trace   the stack trace to use for inferring the class and method names.
     * @param  record  the record where to set the class and method names.
     * @return the record to use for logging the record.
     */
    private static Logger inferCaller(Logger logger, final String classe, final String method,
            final Stream<StackTraceElement> trace, final LogRecord record)
    {
        final StackTraceElement first = trace.filter((element) -> {
            /*
             * Search for the first stack trace element with a class name and method name matching the expected ones.
             * Null argument (class or method name) will not be compared (i.e. any name is accepted).
             *
             * Note: a previous version also compared logger name with package name.
             * This has been removed because those names are only loosely related.
             */
            return ((classe != null) ?  classe.equals(element.getClassName()) : isPublic(element)) &&
                   ((method == null) || method.equals(element.getMethodName()));
        }).findFirst().orElse(null);
        if (logger == null) {
            String name = Logger.GLOBAL_LOGGER_NAME;
            if (first != null) {
                final String classname = first.getClassName();
                final int separator = classname.lastIndexOf('.');
                name = (separator >= 1) ? classname.substring(0, separator-1) : "";
            }
            logger = Logger.getLogger(name);
        }
        if (classe != null || first != null) record.setSourceClassName (first == null ? classe : first.getClassName());
        if (method != null || first != null) record.setSourceMethodName(first == null ? method : first.getMethodName());
        return logger;
    }

    /**
     * Returns {@code true} if the given stack trace element describes a method considered part of public API.
     * This method is invoked in order to infer the class and method names to declare in a {@link LogRecord}.
     * We do not document this feature in public Javadoc because it is based on heuristic rules that may change.
     *
     * @param  e  a stack trace element.
     * @return {@code true} if the class and method specified by the given element can be considered public API.
     */
    private static boolean isPublic(final StackTraceElement e) {
        final String classname = e.getClassName();
        if (classname.startsWith("java") || classname.indexOf('$') >= 0 || e.getMethodName().indexOf('$') >= 0) {
            return false;
        }
        if (classname.startsWith(Modules.CLASSNAME_PREFIX + "util.logging.")) {
            return classname.endsWith("Test");      // Consider JUnit tests as public.
        }
        return true;
    }

    /**
     * Invoked when an unexpected error occurred. This method logs a message at {@link Level#WARNING}
     * to the specified logger. The originating class name and method name can optionally be specified.
     * If any of them is {@code null}, then it will be inferred from the error stack trace as described below.
     * If the {@code classe} or {@code method} arguments are null, then the originating class name and method name
     * are inferred from the given {@code error} using the first {@linkplain StackTraceElement stack trace element}
     * for which the class name is inside a package or sub-package of the same name as the logger name.
     *
     * <h4>Recommended usage</h4>
     * Explicit value for class and method names are preferred to automatic inference for the following reasons:
     * <ul>
     *   <li>Automatic inference is not 100% reliable, since the Java Virtual Machine
     *       is free to omit stack frame in optimized code.</li>
     *   <li>When an exception occurred in a private method used internally by a public
     *       method, we sometimes want to log the warning for the public method instead,
     *       since the user is not expected to know anything about the existence of the
     *       private method. If a developer really want to know about the private method,
     *       the stack trace is still available anyway.</li>
     * </ul>
     *
     * <h4>Example</h4>
     * If the logger name is {@code "org.apache.sis.image"}, then this method will uses the first stack
     * trace element where the fully qualified class name starts with {@code "org.apache.sis.image"} or
     * {@code "org.apache.sis.image.io"}, but not {@code "org.apache.sis.imageio"}.
     *
     * @param  logger  where to log the error, or {@code null} for inferring a default value from other arguments.
     * @param  classe  the class where the error occurred, or {@code null} for inferring a default value from other arguments.
     * @param  method  the method where the error occurred, or {@code null} for inferring a default value from other arguments.
     * @param  error   the error, or {@code null} if none.
     * @return {@code true} if the error has been logged, or {@code false} if the given {@code error}
     *         was null or if the logger does not log anything at {@link Level#WARNING}.
     *
     * @see #recoverableException(Logger, Class, String, Throwable)
     * @see #severeException(Logger, Class, String, Throwable)
     */
    public static boolean unexpectedException(final Logger logger, final Class<?> classe,
                                              final String method, final Throwable error)
    {
        final String classname = (classe != null) ? classe.getName() : null;
        return unexpectedException(logger, classname, method, error, Level.WARNING);
    }

    /**
     * Implementation of {@link #unexpectedException(Logger, Class, String, Throwable)}.
     *
     * @param  logger  where to log the error, or {@code null} for inferring a default value from other arguments.
     * @param  classe  the fully qualified class name where the error occurred, or {@code null} for inferring a default value from other arguments.
     * @param  method  the method where the error occurred, or {@code null} for inferring a default value from other arguments.
     * @param  error   the error, or {@code null} if none.
     * @param  level   the logging level.
     * @return {@code true} if the error has been logged, or {@code false} if the given {@code error}
     *         was null or if the logger does not log anything at the specified level.
     */
    private static boolean unexpectedException(Logger logger, String classe, String method,
                                               final Throwable error, final Level level)
    {
        /*
         * Checks if loggable, inferring the logger from the classe name if needed.
         */
        if (error == null) {
            return false;
        }
        if (logger == null && classe != null) {
            final int separator = classe.lastIndexOf('.');
            final String paquet = (separator >= 1) ? classe.substring(0, separator-1) : "";
            logger = Logger.getLogger(paquet);
        }
        if (logger != null && !logger.isLoggable(level)) {
            return false;
        }
        /*
         * The message is fetched using Exception.getMessage() instead of getLocalizedMessage()
         * because in a client-server architecture, we want the locale on the server-side instead
         * than the locale on the client side. See LocalizedException policy.
         */
        final StringBuilder buffer = new StringBuilder(256).append(Classes.getShortClassName(error));
        String message = error.getMessage();        // Targeted to system administrators (see above).
        if (message != null) {
            buffer.append(": ").append(message);
        }
        message = buffer.toString();
        final LogRecord record;
        if (level.intValue() >= LEVEL_THRESHOLD_FOR_STACKTRACE) {
            record = new LogRecord(level, message);
            record.setThrown(error);
        } else {
            record = new LogRecord(level, Exceptions.formatChainedMessages(null, message, error));
        }
        if (logger == null || classe == null || method == null) {
            logger = inferCaller(logger, classe, method, Arrays.stream(error.getStackTrace()), record);
        } else {
            record.setSourceClassName(classe);
            record.setSourceMethodName(method);
        }
        record.setLoggerName(logger.getName());
        logger.log(record);
        return true;
    }

    /**
     * Invoked when an unexpected error occurred while configuring the system. The error shall not
     * prevent the application from working, but may change the behavior in some minor aspects.
     *
     * <h4>Example</h4>
     * If the {@code org.apache.sis.util.logging.MonolineFormatter.time} pattern declared in the
     * {@code jre/lib/logging.properties} file is illegal, then {@link MonolineFormatter} will log
     * this problem and use a default time pattern.
     *
     * @param  logger  where to log the error, or {@code null} for inferring a default value from other arguments.
     * @param  classe  the class where the error occurred, or {@code null} for inferring a default value from other arguments.
     * @param  method  the method name where the error occurred, or {@code null} for inferring a default value from other arguments.
     * @param  error   the error, or {@code null} if none.
     * @return {@code true} if the error has been logged, or {@code false} if the given {@code error}
     *         was null or if the logger does not log anything at {@link Level#CONFIG}.
     *
     * @see #unexpectedException(Logger, Class, String, Throwable)
     */
    static boolean configurationException(final Logger logger, final Class<?> classe, final String method, final Throwable error) {
        final String classname = (classe != null) ? classe.getName() : null;
        return unexpectedException(logger, classname, method, error, Level.CONFIG);
    }

    /**
     * Invoked when a recoverable error occurred. This method is similar to
     * {@link #unexpectedException(Logger,Class,String,Throwable) unexpectedException(…)}
     * except that it does not log the stack trace and uses a lower logging level.
     *
     * @param  logger  where to log the error, or {@code null} for inferring a default value from other arguments.
     * @param  classe  the class where the error occurred, or {@code null} for inferring a default value from other arguments.
     * @param  method  the method name where the error occurred, or {@code null} for inferring a default value from other arguments.
     * @param  error   the error, or {@code null} if none.
     * @return {@code true} if the error has been logged, or {@code false} if the given {@code error}
     *         was null or if the logger does not log anything at {@link Level#FINE}.
     *
     * @see #unexpectedException(Logger, Class, String, Throwable)
     * @see #severeException(Logger, Class, String, Throwable)
     */
    public static boolean recoverableException(final Logger logger, final Class<?> classe,
                                               final String method, final Throwable error)
    {
        final String classname = (classe != null) ? classe.getName() : null;
        return unexpectedException(logger, classname, method, error, Level.FINE);
    }

    /**
     * Invoked when an ignorable error occurred. This method is similar to
     * {@link #recoverableException(Logger,Class,String,Throwable) unexpectedException(…)}
     * except that it uses a lower logging level.
     *
     * @param  logger  where to log the error, or {@code null} for inferring a default value from other arguments.
     * @param  classe  the class where the error occurred, or {@code null} for inferring a default value from other arguments.
     * @param  method  the method name where the error occurred, or {@code null} for inferring a default value from other arguments.
     * @param  error   the error, or {@code null} if none.
     * @return {@code true} if the error has been logged, or {@code false} if the given {@code error}
     *         was null or if the logger does not log anything at {@link Level#FINER}.
     *
     * @since 1.0
     */
    public static boolean ignorableException(final Logger logger, final Class<?> classe,
                                             final String method, final Throwable error)
    {
        final String classname = (classe != null) ? classe.getName() : null;
        return unexpectedException(logger, classname, method, error, Level.FINER);
    }

    /**
     * Invoked when a severe error occurred. This method is similar to
     * {@link #unexpectedException(Logger,Class,String,Throwable) unexpectedException}
     * except that it logs the message at the {@link Level#SEVERE SEVERE} level.
     *
     * @param  logger  where to log the error, or {@code null} for inferring a default value from other arguments.
     * @param  classe  the class where the error occurred, or {@code null} for inferring a default value from other arguments.
     * @param  method  the method name where the error occurred, or {@code null} for inferring a default value from other arguments.
     * @param  error   the error, or {@code null} if none.
     * @return {@code true} if the error has been logged, or {@code false} if the given {@code error}
     *         was null or if the logger does not log anything at {@link Level#SEVERE}.
     *
     * @see #unexpectedException(Logger, Class, String, Throwable)
     * @see #recoverableException(Logger, Class, String, Throwable)
     */
    public static boolean severeException(final Logger logger, final Class<?> classe,
                                          final String method, final Throwable error)
    {
        final String classname = (classe != null) ? classe.getName() : null;
        return unexpectedException(logger, classname, method, error, Level.SEVERE);
    }
}
