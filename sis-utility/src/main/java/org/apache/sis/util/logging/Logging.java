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

import java.util.Comparator;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import org.apache.sis.util.Configuration;
import org.apache.sis.util.Static;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.Classes;

import static java.util.Arrays.binarySearch;
import static org.apache.sis.util.Arrays.insert;


/**
 * A set of utilities method for configuring loggings in SIS. Library implementors shall fetch
 * their logger using the {@link #getLogger(Class)} static method provided in this class rather
 * than {@link Logger#getLogger(String)}, in order to give SIS a chance to redirect the logging
 * to an other framework like <A HREF="http://commons.apache.org/logging/">Commons-logging</A> or
 * <A HREF="http://logging.apache.org/log4j">Log4J</A>.
 * <p>
 * This method provides also a {@link #log(Class, String, LogRecord)} convenience static method, which
 * {@linkplain LogRecord#setLoggerName set the logger name} of the given record before to log it.
 * An other worthy static method is {@link #unexpectedException(Class, String, Throwable)}, for
 * reporting an anomalous but nevertheless non-fatal exception.
 *
 * {@section Configuration}
 * The log records can be redirected explicitly to an other logging framework using the
 * {@link #setLoggerFactory(LoggerFactory)} method.
 * Note however that this method call is performed automatically if the
 * {@code sis-logging-commons.jar} or the {@code sis-logging-log4j.jar} file is
 * found on the classpath, so it usually doesn't need to be invoked explicitely.
 * See the {@link #scanLoggerFactory()} method for more details on automatic logger
 * factory registration.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.4)
 * @version 0.3
 * @module
 */
public final class Logging extends Static {
    /**
     * Compares {@link Logging} or {@link String} objects for alphabetical order.
     */
    private static final Comparator<Object> COMPARATOR = new Comparator<Object>() {
        @Override public int compare(final Object o1, final Object o2) {
            final String n1 = (o1 instanceof Logging) ? ((Logging) o1).name : o1.toString();
            final String n2 = (o2 instanceof Logging) ? ((Logging) o2).name : o2.toString();
            return n1.compareTo(n2);
        }
    };

    /**
     * An empty array of loggings. Also used for locks.
     */
    private static final Logging[] EMPTY = new Logging[0];

    /**
     * Logging configuration that apply to all packages.
     */
    public static final Logging ALL = new Logging();
    static { // Must be run after ALL assignation and before SIS (or any other Logging) creation.
        ALL.scanLoggerFactory();
    }

    /**
     * Logging configuration that apply only to "{@code org.apache.sis}" packages.
     */
    public static final Logging SIS = getLogging("org.apache.sis");

    /**
     * The name of the base package.
     */
    public final String name;

    /**
     * The children {@link Logging} objects.
     * <p>
     * The plain array used there is not efficient for adding new items (an {@code ArrayList}
     * would be more efficient), but we assume that very few new items will be added. Furthermore
     * a plain array is efficient for reading, and the later is way more common than the former.
     */
    private Logging[] children = EMPTY;

    /**
     * The factory for creating loggers, or {@code null} if none. If {@code null}
     * (the default), then the standard JDK logging framework will be used.
     *
     * @see #setLoggerFactory(LoggerFactory)
     */
    private LoggerFactory<?> factory;

    /**
     * {@code true} if every {@link Logging} instances use the same {@link LoggerFactory}.
     * This is an optimization for a very common case.
     */
    private static boolean sameLoggerFactory = true;

    /**
     * Creates an instance for the root logger. This constructor should not be used for anything
     * else than {@link #ALL} construction; use the {@link #getLogging(String)} method instead.
     */
    private Logging() {
        name = "";
    }

    /**
     * Creates an instance for the specified base logger. This constructor should
     * not be public; use the {@link #getLogging(String)} method instead.
     *
     * @param parent The parent {@code Logging} instance.
     * @param name   The logger name for the new instance.
     */
    private Logging(final Logging parent, final String name) {
        this.name = name;
        factory = parent.factory;
        assert name.startsWith(parent.name) : name;
    }

    /**
     * Logs the given record to the logger associated to the given class.
     * This convenience method performs the following steps:
     * <p>
     * <ul>
     *   <li>Get the logger using {@link #getLogger(Class)};</li>
     *   <li>{@linkplain LogRecord#setLoggerName(String) Set the logger name} of the given record,
     *       if not already set;</li>
     *   <li>Unconditionally {@linkplain LogRecord#setSourceClassName(String) set the source class
     *       name} to the {@linkplain Class#getCanonicalName() canonical name} of the given class;</li>
     *   <li>Unconditionally {@linkplain LogRecord#setSourceMethodName(String) set the source method
     *       name} to the given value;</li>
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
        final Logger logger = getLogger(classe);
        if (record.getLoggerName() == null) {
            record.setLoggerName(logger.getName());
        }
        logger.log(record);
    }

    /**
     * Returns a logger for the specified class. This convenience method invokes
     * {@link #getLogger(String)} with the package name as the logger name.
     *
     * @param  classe The class for which to obtain a logger.
     * @return A logger for the specified class.
     */
    public static Logger getLogger(Class<?> classe) {
        Class<?> outer;
        while ((outer = classe.getEnclosingClass()) != null) {
            classe = outer;
        }
        String name = classe.getName();
        final int separator = name.lastIndexOf('.');
        name = (separator >= 1) ? name.substring(0, separator) : "";
        return getLogger(name);
    }

    /**
     * Returns a logger for the specified name. If a {@linkplain LoggerFactory logger factory} has
     * been set, then this method first {@linkplain LoggerFactory#getLogger ask to the factory}.
     * It gives SIS a chance to redirect logging events to
     * <A HREF="http://commons.apache.org/logging/">commons-logging</A>
     * or some equivalent framework.
     * <p>
     * If no factory was found or if the factory choose to not redirect the loggings, then this
     * method returns the usual <code>{@linkplain Logger#getLogger Logger.getLogger}(name)</code>.
     *
     * @param  name The logger name.
     * @return A logger for the specified name.
     */
    public static Logger getLogger(final String name) {
        synchronized (EMPTY) {
            final Logging logging = sameLoggerFactory ? ALL : getLogging(name, false);
            if (logging != null) { // Paranoiac check ('getLogging' should not returns null).
                final LoggerFactory<?> factory = logging.factory;
                assert getLogging(name, false).factory == factory : name;
                if (factory != null) {
                    final Logger logger = factory.getLogger(name);
                    if (logger != null) {
                        return logger;
                    }
                }
            }
        }
        return Logger.getLogger(name);
    }

    /**
     * Returns a {@code Logging} instance for the specified base logger.
     * This instance can be used for controlling logging configuration in SIS.
     * <p>
     * {@code Logging} instances follow the same hierarchy than {@link Logger}, i.e.
     * {@code "org.apache.sis"} is the parent of {@code "org.apache.sis.referencing"},
     * {@code "org.apache.sis.metadata"}, <i>etc</i>.
     *
     * @param name The base logger name.
     * @return The logging instance for the given name.
     */
    public static Logging getLogging(final String name) {
        synchronized (EMPTY) {
            return getLogging(name, true);
        }
    }

    /**
     * Returns a logging instance for the specified base logger. If no instance is found for
     * the specified name and {@code create} is {@code true}, then a new instance will be
     * created. Otherwise the nearest parent is returned.
     *
     * @param base The root logger name.
     * @param create {@code true} if this method is allowed to create new {@code Logging} instance.
     * @return The logging instance for the given name.
     */
    private static Logging getLogging(final String base, final boolean create) {
        assert Thread.holdsLock(EMPTY);
        Logging logging = ALL;
        if (!base.isEmpty()) {
            int offset = 0;
            do {
                Logging[] children = logging.children;
                offset = base.indexOf('.', offset);
                final String name = (offset >= 0) ? base.substring(0, offset) : base;
                int i = binarySearch(children, name, COMPARATOR);
                if (i < 0) {
                    // No exact match found.
                    if (!create) {
                        // We are not allowed to create new Logging instance.
                        // 'logging' is the nearest parent, so stop the loop now.
                        break;
                    }
                    i = ~i;
                    children = insert(children, i, 1);
                    children[i] = new Logging(logging, name);
                    logging.children = children;
                }
                logging = children[i];
            } while (++offset != 0);
        }
        return logging;
    }

    /**
     * Returns the logger factory, or {@code null} if none. This method returns the logger set
     * by the last call to {@link #setLoggerFactory(LoggerFactory)} on this {@code Logging}
     * instance or on one of its parent.
     *
     * @return The current logger factory.
     */
    public LoggerFactory<?> getLoggerFactory() {
        synchronized (EMPTY) {
            return factory;
        }
    }

    /**
     * Sets a new logger factory for this {@code Logging} instance and every children. The
     * specified factory will be used by <code>{@linkplain #getLogger(String) getLogger}(name)</code>
     * when {@code name} is this {@code Logging} name or one of its children.
     * <p>
     * If the factory is set to {@code null} (the default), then the standard Logging framework
     * will be used.
     *
     * @param factory The new logger factory, or {@code null} if none.
     */
    @Configuration
    public void setLoggerFactory(final LoggerFactory<?> factory) {
        synchronized (EMPTY) {
            this.factory = factory;
            for (int i=0; i<children.length; i++) {
                children[i].setLoggerFactory(factory);
            }
            sameLoggerFactory = sameLoggerFactory(ALL.children, ALL.factory); // NOSONAR: really want static field.
        }
    }

    /**
     * Returns {@code true} if all children use the specified factory.
     * Used in order to detect a possible optimization for this very common case.
     */
    private static boolean sameLoggerFactory(final Logging[] children, final LoggerFactory<?> factory) {
        assert Thread.holdsLock(EMPTY);
        for (int i=0; i<children.length; i++) {
            final Logging logging = children[i];
            if (logging.factory != factory || !sameLoggerFactory(logging.children, factory)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Scans the classpath for logger factories. The fully qualified factory classname shall be
     * declared in the following file:
     *
     * {@preformat text
     *     META-INF/services/org.apache.sis.util.logging.LoggerFactory
     * }
     *
     * The factory found on the classpath is given to {@link #setLoggerFactory(LoggerFactory)}.
     * If more than one factory is found, then the log messages will be sent to the logging
     * frameworks managed by all those factories.
     * <p>
     * This method usually doesn't need to be invoked explicitly, since it is automatically
     * invoked on {@code Logging} class initialization. However developers may invoke it if
     * new {@code LoggerFactory}s are added later on the classpath of a running JVM.
     */
    @Configuration
    public void scanLoggerFactory() {
        LoggerFactory<?> factory = null;
        for (final LoggerFactory<?> found : ServiceLoader.load(LoggerFactory.class)) {
            if (factory == null) {
                factory = found;
            } else {
                factory = new DualLoggerFactory(factory, found);
            }
        }
        setLoggerFactory(factory);
    }

    /**
     * Flushes all {@linkplain Handler handlers} used by the logger named {@link #name}.
     * If that logger {@linkplain Logger#getUseParentHandlers() uses parent handlers},
     * then those handlers will be flushed as well.
     * <p>
     * If the log records seem to be interleaved with the content of {@link System#out}
     * or {@link System#err}, invoking this method before to write to the standard streams
     * may help.
     *
     * @see Handler#flush()
     */
    public void flush() {
        for (Logger logger=getLogger(name); logger!=null; logger=logger.getParent()) {
            for (final Handler handler : logger.getHandlers()) {
                handler.flush();
            }
            if (!logger.getUseParentHandlers()) {
                break;
            }
        }
    }

    /**
     * Invoked when an unexpected error occurs. This method logs a message at the
     * {@link Level#WARNING WARNING} level to the specified logger. The originating
     * class name and method name are inferred from the error stack trace, using the
     * first {@linkplain StackTraceElement stack trace element} for which the class
     * name is inside a package or sub-package of the logger name. For example if
     * the logger name is {@code "org.apache.sis.image"}, then this method will uses
     * the first stack trace element where the fully qualified class name starts with
     * {@code "org.apache.sis.image"} or {@code "org.apache.sis.image.io"}, but not
     * {@code "org.apache.sis.imageio"}.
     *
     * @param  logger Where to log the error.
     * @param  error  The error that occurred.
     * @return {@code true} if the error has been logged, or {@code false} if the logger
     *         doesn't log anything at the {@link Level#WARNING WARNING} level.
     */
    public static boolean unexpectedException(final Logger logger, final Throwable error) {
        return unexpectedException(logger, null, null, error, Level.WARNING);
    }

    /**
     * Invoked when an unexpected error occurs. This method logs a message at the
     * {@link Level#WARNING WARNING} level to the specified logger. The originating
     * class name and method name can optionally be specified. If any of them is
     * {@code null}, then it will be inferred from the error stack trace as in
     * {@link #unexpectedException(Logger, Throwable)}.
     * <p>
     * Explicit value for class and method names are sometime preferred to automatic
     * inference for the following reasons:
     * <p>
     * <ul>
     *   <li>Automatic inference is not 100% reliable, since the Java Virtual Machine
     *       is free to omit stack frame in optimized code.</li>
     *   <li>When an exception occurred in a private method used internally by a public
     *       method, we sometime want to log the warning for the public method instead,
     *       since the user is not expected to know anything about the existence of the
     *       private method. If a developer really want to know about the private method,
     *       the stack trace is still available anyway.</li>
     * </ul>
     *
     * @param logger  Where to log the error.
     * @param classe  The class where the error occurred, or {@code null}.
     * @param method  The method where the error occurred, or {@code null}.
     * @param error   The error.
     * @return {@code true} if the error has been logged, or {@code false} if the logger
     *         doesn't log anything at the {@link Level#WARNING WARNING} level.
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
     * Invoked when an unexpected error occurs. This method logs a message at the
     * {@link Level#WARNING WARNING} level to a logger inferred from the given class.
     *
     * @param classe  The class where the error occurred.
     * @param method  The method where the error occurred, or {@code null}.
     * @param error   The error.
     * @return {@code true} if the error has been logged, or {@code false} if the logger
     *         doesn't log anything at the {@link Level#WARNING WARNING} level.
     *
     * @see #recoverableException(Class, String, Throwable)
     */
    public static boolean unexpectedException(Class<?> classe, String method, Throwable error) {
        return unexpectedException((Logger) null, classe, method, error);
    }

    /**
     * Implementation of {@link #unexpectedException(Logger, Class, String, Throwable)}.
     *
     * @param logger  Where to log the error, or {@code null}.
     * @param classe  The fully qualified class name where the error occurred, or {@code null}.
     * @param method  The method where the error occurred, or {@code null}.
     * @param error   The error.
     * @param level   The logging level.
     * @return {@code true} if the error has been logged, or {@code false} if the logger
     *         doesn't log anything at the specified level.
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
        if (logger==null || classe==null || method==null) {
            String paquet = (logger != null) ? logger.getName() : null;
            final StackTraceElement[] elements = error.getStackTrace();
            for (int i=0; i<elements.length; i++) {
                /*
                 * Searches for the first stack trace element with a classname matching the
                 * expected one. We compare preferably against the name of the class given
                 * in argument, or against the logger name (taken as the package name) otherwise.
                 */
                final StackTraceElement element = elements[i];
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
         */
        final StringBuilder buffer = new StringBuilder(Classes.getShortClassName(error));
        String message = error.getLocalizedMessage();
        if (message != null) {
            buffer.append(": ").append(message);
        }
        message = buffer.toString();
        message = Exceptions.formatChainedMessages(message, error);
        final LogRecord record = new LogRecord(level, message);
        if (classe != null) {
            record.setSourceClassName(classe);
        }
        if (method != null) {
            record.setSourceMethodName(method);
        }
        if (level.intValue() > 500) {
            record.setThrown(error);
        }
        record.setLoggerName(logger.getName());
        logger.log(record);
        return true;
    }

    /**
     * Invoked when a recoverable error occurs. This method is similar to
     * {@link #unexpectedException(Class,String,Throwable) unexpectedException}
     * except that it doesn't log the stack trace and uses a lower logging level.
     *
     * @param classe  The class where the error occurred.
     * @param method  The method name where the error occurred.
     * @param error   The error.
     * @return {@code true} if the error has been logged, or {@code false} if the logger
     *         doesn't log anything at the {@link Level#FINE FINE} level.
     *
     * @see #unexpectedException(Class, String, Throwable)
     */
    public static boolean recoverableException(final Class<?> classe, final String method,
                                               final Throwable error)
    {
        return recoverableException(null, classe, method, error);
    }

    /**
     * Invoked when a recoverable error occurs. This method is similar to
     * {@link #unexpectedException(Logger,Class,String,Throwable) unexpectedException}
     * except that it doesn't log the stack trace and uses a lower logging level.
     *
     * @param logger  Where to log the error.
     * @param classe  The class where the error occurred.
     * @param method  The method name where the error occurred.
     * @param error   The error.
     * @return {@code true} if the error has been logged, or {@code false} if the logger
     *         doesn't log anything at the {@link Level#FINE FINE} level.
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
     * Invoked when a severe error occurs. This method is similar to
     * {@link #unexpectedException(Logger,Class,String,Throwable) unexpectedException}
     * except that it logs the message at the {@link Level#SEVERE SEVERE} level.
     *
     * @param logger  Where to log the error.
     * @param classe  The class where the error occurred.
     * @param method  The method name where the error occurred.
     * @param error   The error.
     * @return {@code true} if the error has been logged, or {@code false} if the logger
     *         doesn't log anything at the {@link Level#SEVERE SEVERE} level.
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
