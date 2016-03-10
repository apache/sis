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

import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

import org.apache.sis.util.Configuration;
import org.apache.sis.util.Static;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.Classes;


/**
 * A set of utilities method for configuring loggings in SIS. Library implementors should fetch
 * their loggers using the {@link #getLogger(String)} static method defined in this {@code Logging}
 * class rather than the one defined in the standard {@link Logger} class, in order to give SIS a
 * chance to redirect the logs to an other framework like
 * <a href="http://commons.apache.org/logging/">Commons-logging</a> or
 * <a href="http://logging.apache.org/log4j">Log4J</a>.
 *
 * <p>This class provides also some convenience static methods, including:</p>
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
 * @since   0.3
 * @version 0.6
 * @module
 */
public final class Logging extends Static {
    /**
     * The threshold at which {@link #unexpectedException(Logger, String, String, Throwable, Level)} shall
     * set the throwable in the {@link LogRecord}. For any record to be logged at a lower {@link Level},
     * the {@link LogRecord#setThrown(Throwable)} method will not be invoked.
     *
     * <p>The default value is 600, which is the {@link PerformanceLevel#PERFORMANCE} value.
     * This value is between {@link Level#FINE} (500) and {@link Level#CONFIG} (700).
     * Consequently we will ignore the stack traces of recoverable failures, but will report
     * stack traces that may impact performance, configuration, or correctness.</p>
     */
    private static final int LEVEL_THRESHOLD_FOR_STACKTRACE = 600;

    /**
     * The factory for obtaining {@link Logger} instances, or {@code null} if none.
     * If {@code null} (the default), then the standard JDK logging framework will be used.
     * {@code Logging} scans the classpath for logger factories on class initialization.
     * The fully qualified factory classname shall be declared in the following file:
     *
     * {@preformat text
     *     META-INF/services/org.apache.sis.util.logging.LoggerFactory
     * }
     *
     * The factory found on the classpath is assigned to the {@link #factory} field. If more than one factory
     * is found, then the log messages will be sent to the logging frameworks managed by all those factories.
     *
     * <div class="note"><b>API note:</b>
     * A previous version was providing a {@code scanForPlugins()} method allowing developers to refresh the
     * object state when new {@link LoggerFactory} instances become available on the classpath of a running JVM.
     * However it usually doesn't work since loggers are typically stored in static final fields.</div>
     *
     * @see #setLoggerFactory(LoggerFactory)
     */
    private static volatile LoggerFactory<?> factory;
    static {
        LoggerFactory<?> factory = null;
        for (final LoggerFactory<?> found : ServiceLoader.load(LoggerFactory.class)) {
            if (factory == null) {
                factory = found;
            } else {
                factory = new DualLoggerFactory(factory, found);
            }
        }
        Logging.factory = factory;
    }

    /**
     * Do not allow instantiation of this class.
     */
    private Logging() {
    }

    /**
     * Sets a new factory to use for obtaining {@link Logger} instances.
     * If the given {@code factory} argument is {@code null} (the default),
     * then the standard Logging framework will be used.
     *
     * <div class="section">Limitation</div>
     * SIS classes typically declare a logger constant like below:
     *
     * {@preformat java
     *     public static final Logger LOGGER = Logging.getLogger("the.logger.name");
     * }
     *
     * Factory changes will take effect only if this method is invoked before the initialization
     * of such classes.
     *
     * @param factory The new logger factory, or {@code null} if none.
     */
    @Configuration
    public static void setLoggerFactory(final LoggerFactory<?> factory) {
        Logging.factory = factory;
    }

    /**
     * Returns the factory used for obtaining {@link Logger} instances, or {@code null} if none.
     *
     * @return The current logger factory, or {@code null} if none.
     */
    public static LoggerFactory<?> getLoggerFactory() {
        return factory;
    }

    /**
     * Returns a logger for the specified name. If a {@linkplain LoggerFactory logger factory} has been set,
     * then this method first {@linkplain LoggerFactory#getLogger(String) asks to the factory}.
     * This rule gives SIS a chance to redirect logging events to
     * <a href="http://commons.apache.org/logging/">commons-logging</a> or some equivalent framework.
     * Only if no factory was found or if the factory choose to not redirect the loggings, then this
     * method delegate to <code>{@linkplain Logger#getLogger(String) Logger.getLogger}(name)</code>.
     *
     * @param  name The logger name.
     * @return A logger for the specified name.
     */
    public static Logger getLogger(final String name) {
        final LoggerFactory<?> factory = Logging.factory;
        if (factory != null) {
            final Logger logger = factory.getLogger(name);
            if (logger != null) {
                return logger;
            }
        }
        return Logger.getLogger(name);
    }

    /**
     * Returns a logger for the specified class. This convenience method invokes
     * {@link #getLogger(String)} with the package name as the logger name.
     *
     * @param  classe The class for which to obtain a logger.
     * @return A logger for the specified class.
     */
    static Logger getLogger(Class<?> classe) {
        Class<?> outer;
        while ((outer = classe.getEnclosingClass()) != null) {
            classe = outer;
        }
        String name = classe.getName();
        final int separator = name.lastIndexOf('.');
        name = (separator >= 1) ? name.substring(0, separator) : "";
        if (name.startsWith("org.apache.sis.internal.")) {
            name = "org.apache.sis" + name.substring(23);       // Remove the "internal" part from SIS package name.
        }
        return getLogger(name);
    }

    /**
     * Logs the given record to the logger associated to the given class.
     * This convenience method performs the following steps:
     *
     * <ul>
     *   <li>Unconditionally {@linkplain LogRecord#setSourceClassName(String) set the source class name}
     *       to the {@linkplain Class#getCanonicalName() canonical name} of the given class;</li>
     *   <li>Unconditionally {@linkplain LogRecord#setSourceMethodName(String) set the source method name}
     *       to the given value;</li>
     *   <li>Get the logger for the {@linkplain LogRecord#getLoggerName() logger name} if specified,
     *       or for the {@code classe} package name otherwise;</li>
     *   <li>{@linkplain LogRecord#setLoggerName(String) Set the logger name} of the given record,
     *       if not already set;</li>
     *   <li>{@linkplain Logger#log(LogRecord) Log} the modified record.</li>
     * </ul>
     *
     * @param classe The class for which to obtain a logger.
     * @param method The name of the method which is logging a record.
     * @param record The record to log.
     */
    public static void log(final Class<?> classe, final String method, final LogRecord record) {
        record.setSourceClassName(classe.getCanonicalName());
        record.setSourceMethodName(method);
        final String loggerName = record.getLoggerName();
        final Logger logger;
        if (loggerName == null) {
            logger = getLogger(classe);
            record.setLoggerName(logger.getName());
        } else {
            logger = getLogger(loggerName);
        }
        logger.log(record);
    }

    /**
     * Invoked when an unexpected error occurred. This method logs a message at {@link Level#WARNING} to the
     * specified logger. The originating class name and method name are inferred from the error stack trace,
     * using the first {@linkplain StackTraceElement stack trace element} for which the class name is inside
     * a package or sub-package of the logger name.
     *
     * @param  logger Where to log the error, or {@code null} for inferring a default value from other arguments.
     * @param  error  The error that occurred, or {@code null} if none.
     * @return {@code true} if the error has been logged, or {@code false} if the given {@code error}
     *         was null or if the logger does not log anything at {@link Level#WARNING}.
     *
     * @deprecated Use {@link #unexpectedException(Logger, Class, String, Throwable)} instead.
     */
    @Deprecated
    public static boolean unexpectedException(final Logger logger, final Throwable error) {
        return unexpectedException(logger, null, null, error, Level.WARNING);
    }

    /**
     * Invoked when an unexpected error occurred. This method logs a message at {@link Level#WARNING}
     * to the specified logger. The originating class name and method name can optionally be specified.
     * If any of them is {@code null}, then it will be inferred from the error stack trace as described below.
     *
     * <div class="note"><b>Recommended usage:</b>
     * explicit value for class and method names are preferred to automatic inference for the following reasons:
     * <ul>
     *   <li>Automatic inference is not 100% reliable, since the Java Virtual Machine
     *       is free to omit stack frame in optimized code.</li>
     *   <li>When an exception occurred in a private method used internally by a public
     *       method, we sometime want to log the warning for the public method instead,
     *       since the user is not expected to know anything about the existence of the
     *       private method. If a developer really want to know about the private method,
     *       the stack trace is still available anyway.</li>
     * </ul></div>
     *
     * If the {@code classe} or {@code method} arguments are null, then the originating class name and method name
     * are inferred from the given {@code error} using the first {@linkplain StackTraceElement stack trace element}
     * for which the class name is inside a package or sub-package of the same name than the logger name.
     *
     * <div class="note"><b>Example:</b>
     * if the logger name is {@code "org.apache.sis.image"}, then this method will uses the first stack
     * trace element where the fully qualified class name starts with {@code "org.apache.sis.image"} or
     * {@code "org.apache.sis.image.io"}, but not {@code "org.apache.sis.imageio"}.</div>
     *
     * @param logger  Where to log the error, or {@code null} for inferring a default value from other arguments.
     * @param classe  The class where the error occurred, or {@code null} for inferring a default value from other arguments.
     * @param method  The method where the error occurred, or {@code null} for inferring a default value from other arguments.
     * @param error   The error, or {@code null} if none.
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
     * @param logger  Where to log the error, or {@code null} for inferring a default value from other arguments.
     * @param classe  The fully qualified class name where the error occurred, or {@code null} for inferring a default value from other arguments.
     * @param method  The method where the error occurred, or {@code null} for inferring a default value from other arguments.
     * @param error   The error, or {@code null} if none.
     * @param level   The logging level.
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
            logger = getLogger(paquet);
        }
        if (logger != null && !logger.isLoggable(level)) {
            return false;
        }
        /*
         * Loggeable, so complete the null argument from the stack trace if we can.
         */
        if (logger == null || classe == null || method == null) {
            String paquet = (logger != null) ? logger.getName() : null;
            for (final StackTraceElement element : error.getStackTrace()) {
                /*
                 * Searches for the first stack trace element with a classname matching the
                 * expected one. We compare preferably against the name of the class given
                 * in argument, or against the logger name (taken as the package name) otherwise.
                 */
                final String classname = element.getClassName();
                if (classe != null) {
                    if (!classname.equals(classe)) {
                        continue;
                    }
                } else if (paquet != null) {
                    if (!classname.startsWith(paquet)) {
                        continue;
                    }
                    final int length = paquet.length();
                    if (classname.length() > length) {
                        // We expect '.' but we accept also '$' or end of string.
                        final char separator = classname.charAt(length);
                        if (Character.isJavaIdentifierPart(separator)) {
                            continue;
                        }
                    }
                }
                /*
                 * Now that we have a stack trace element from the expected class (or any
                 * element if we don't know the class), make sure that we have the right method.
                 */
                final String methodName = element.getMethodName();
                if (method != null && !methodName.equals(method)) {
                    continue;
                }
                /*
                 * Now computes every values that are null, and stop the loop.
                 */
                if (paquet == null) {
                    final int separator = classname.lastIndexOf('.');
                    paquet = (separator >= 1) ? classname.substring(0, separator-1) : "";
                    logger = getLogger(paquet);
                    if (!logger.isLoggable(level)) {
                        return false;
                    }
                }
                if (classe == null) {
                    classe = classname;
                }
                if (method == null) {
                    method = methodName;
                }
                break;
            }
            /*
             * The logger may stay null if we have been unable to find a suitable
             * stack trace. Fallback on the global logger.
             */
            if (logger == null) {
                logger = getLogger(Logger.GLOBAL_LOGGER_NAME);
                if (!logger.isLoggable(level)) {
                    return false;
                }
            }
        }
        /*
         * Now prepare the log message. If we have been unable to figure out a source class and
         * method name, we will fallback on JDK logging default mechanism, which may returns a
         * less relevant name than our attempt to use the logger name as the package name.
         *
         * The message is fetched using Exception.getMessage() instead than getLocalizedMessage()
         * because in a client-server architecture, we want the locale on the server-side instead
         * than the locale on the client side.
         */
        final StringBuilder buffer = new StringBuilder(256).append(Classes.getShortClassName(error));
        String message = error.getMessage();                    // Targeted to system administrators.
        if (message != null) {
            buffer.append(": ").append(message);
        }
        message = buffer.toString();
        message = Exceptions.formatChainedMessages(null, message, error);
        final LogRecord record = new LogRecord(level, message);
        if (classe != null) {
            record.setSourceClassName(classe);
        }
        if (method != null) {
            record.setSourceMethodName(method);
        }
        if (level.intValue() >= LEVEL_THRESHOLD_FOR_STACKTRACE) {
            record.setThrown(error);
        }
        record.setLoggerName(logger.getName());
        logger.log(record);
        return true;
    }

    /**
     * Invoked when an unexpected error occurred while configuring the system. The error shall not
     * prevent the application from working, but may change the behavior in some minor aspects.
     *
     * <div class="note"><b>Example:</b>
     * If the {@code org.apache.sis.util.logging.MonolineFormatter.time} pattern declared in the
     * {@code jre/lib/logging.properties} file is illegal, then {@link MonolineFormatter} will log
     * this problem and use a default time pattern.</div>
     *
     * @param logger  Where to log the error, or {@code null} for inferring a default value from other arguments.
     * @param classe  The class where the error occurred, or {@code null} for inferring a default value from other arguments.
     * @param method  The method name where the error occurred, or {@code null} for inferring a default value from other arguments.
     * @param error   The error, or {@code null} if none.
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
     * @param logger  Where to log the error, or {@code null} for inferring a default value from other arguments.
     * @param classe  The class where the error occurred, or {@code null} for inferring a default value from other arguments.
     * @param method  The method name where the error occurred, or {@code null} for inferring a default value from other arguments.
     * @param error   The error, or {@code null} if none.
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
     * Invoked when a severe error occurred. This method is similar to
     * {@link #unexpectedException(Logger,Class,String,Throwable) unexpectedException}
     * except that it logs the message at the {@link Level#SEVERE SEVERE} level.
     *
     * @param logger  Where to log the error, or {@code null} for inferring a default value from other arguments.
     * @param classe  The class where the error occurred, or {@code null} for inferring a default value from other arguments.
     * @param method  The method name where the error occurred, or {@code null} for inferring a default value from other arguments.
     * @param error   The error, or {@code null} if none.
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
