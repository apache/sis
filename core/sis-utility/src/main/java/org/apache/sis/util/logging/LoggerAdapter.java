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

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;

import org.apache.sis.util.Debug;


/**
 * An adapter that redirect all JDK logging events to an other logging framework. This
 * class redefines the {@link #severe(String) severe}, {@link #warning(String) warning},
 * {@link #info(String) info}, {@link #config(String) config}, {@link #fine(String) fine},
 * {@link #finer(String) finer} and {@link #finest(String) finest} methods as <em>abstract</em>
 * ones. Subclasses should implement those methods in order to map JDK logging levels to
 * the backend logging framework.
 *
 * <p>All {@link #log(Level,String) log} methods are overridden in order to redirect to one of the
 * above-cited methods. Note that this is the opposite approach than the JDK logging framework
 * one, which implements everything on top of {@link Logger#log(LogRecord)}. This adapter is
 * defined in terms of {@link #severe(String) severe} … {@link #finest(String) finest} methods
 * instead because external frameworks like <a href="http://commons.apache.org/logging/">Commons-logging</a>
 * don't work with {@link LogRecord}, and sometime provides nothing else than convenience methods
 * equivalent to {@link #severe(String) severe} … {@link #finest(String) finest}.</p>
 *
 * <div class="section">Restrictions</div>
 * Because the configuration is expected to be fully controlled by the external logging
 * framework, every configuration methods inherited from {@link Logger} are disabled:
 *
 * <ul>
 *   <li>{@link #addHandler(Handler)}
 *       since the handling is performed by the external framework.</li>
 *
 *   <li>{@link #setUseParentHandlers(boolean)}
 *       since this adapter never delegates to the parent handlers. This is consistent with the
 *       previous item and avoid mixing loggings from the external framework with JDK loggings.</li>
 *
 *   <li>{@link #setParent(Logger)}
 *       since this adapter should not inherits any configuration from a parent logger using the
 *       JDK logging framework.</li>
 *
 *   <li>{@link #setFilter(Filter)}
 *       for keeping this {@code LoggerAdapter} simple.</li>
 * </ul>
 *
 * Since {@code LoggerAdapter}s do not hold any configuration by themselves, it is not strictly
 * necessary to {@linkplain java.util.logging.LogManager#addLogger add them to the log manager}.
 * The adapters can be created, garbage-collected and recreated again while preserving their
 * behavior since their configuration is entirely contained in the external logging framework.
 *
 * <div class="section">Localization</div>
 * This logger is always created without resource bundles. Localizations shall be done through
 * explicit calls to {@code logrb} or {@link #log(LogRecord)} methods. This is sufficient for
 * SIS needs, which performs all localizations through the later. Note that those methods
 * will be slower in this {@code LoggerAdapter} than the default {@link Logger} because this
 * adapter localizes and formats records immediately instead of letting the {@linkplain Handler}
 * performs this work only if needed.
 *
 * <div class="section">Logging levels</div>
 * If a log record {@linkplain Level level} is not one of the predefined ones, then this class
 * maps to the first level below the specified one. For example if a log record has some level
 * between {@link Level#FINE FINE} and {@link Level#FINER FINER}, then the {@link #finer finer}
 * method will be invoked. See {@link #isLoggable(Level)} for implementation tips taking advantage
 * of this rule.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 *
 * @see Logging
 */
public abstract class LoggerAdapter extends Logger {
    /**
     * The pattern to use for detecting {@link MessageFormat}.
     */
    private static final Pattern MESSAGE_FORMAT = Pattern.compile("\\{\\d+\\}");

    /**
     * Creates a new logger.
     *
     * @param name The logger name.
     */
    protected LoggerAdapter(final String name) {
        super(name, null);
        /*
         * Must invokes the super-class method, because LoggerAdapter overrides it as a no-op.
         */
        super.setUseParentHandlers(false);
        /*
         * Sets the level to ALL as a matter of principle,  but we will never check the level
         * anyway (we will let the external logging framework do its own check). Note that we
         * must invoke the method in the super-class  because we want to set the java logging
         * level, not the external framework level.
         */
        super.setLevel(Level.ALL);
    }

    /**
     * Sets the level for this logger. Subclasses must redirect the call to the external
     * logging framework, or do nothing if the level can not be changed programmatically.
     *
     * @param level The new value for the log level (may be null).
     */
    @Override
    public abstract void setLevel(Level level);

    /**
     * Returns the level for this logger. Subclasses shall get this level from the
     * external logging framework.
     *
     * @return The logger's level.
     */
    @Override
    public abstract Level getLevel();

    /**
     * Returns the level for {@link #entering(String, String) entering(…)}, {@link #exiting(String, String) exiting(…)}
     * and {@link #throwing(String, String, Throwable) throwing(…)} methods.
     * The default implementation returns {@link Level#FINER}, which is consistent with the
     * value used in the JDK logging framework. Subclasses should override this method if
     * a different debug level is wanted.
     *
     * @return The level to use for debugging informations.
     */
    @Debug
    protected Level getDebugLevel() {
        return Level.FINER;
    }

    /**
     * Returns {@code true} if the specified level is loggable.
     *
     * <div class="section">Implementation tip</div>
     * Given that {@link Level#intValue} for all predefined levels are documented in the {@link Level}
     * specification and are multiple of 100, given that integer divisions are rounded toward zero and
     * given rule documented in this <a href="#skip-navbar_top">class javadoc</a>, then logging levels
     * can be efficiently mapped to predefined levels using {@code switch} statements as below. This
     * statement has good chances to be compiled to the {@code tableswitch} bytecode rather than
     * {@code lookupswitch} (see
     * <a href="http://java.sun.com/docs/books/jvms/second_edition/html/Compiling.doc.html#14942">Compiling
     * Switches</a> in <cite>The Java Virtual Machine Specification</cite>).
     *
     * {@preformat java
     *     public boolean isLoggable(Level level) {
     *         final int n = level.intValue();
     *         switch (n / 100) {
     *             default: {
     *                 // MAX_VALUE is a special value for Level.OFF. Otherwise and
     *                 // if positive, fallthrough since we are greater than SEVERE.
     *                 switch (n) {
     *                     case Integer.MIN_VALUE: return true;  // Level.ALL
     *                     case Integer.MAX_VALUE: return false; // Level.OFF
     *                     default: if (n < 0) return false;
     *                 }
     *             }
     *             case 10: return isSevereEnabled();
     *             case  9: return isWarningEnabled();
     *             case  8: return isInfoEnabled();
     *             case  7: return isConfigEnabled();
     *             case  6: // fallthrough
     *             case  5: return isFineEnabled();
     *             case  4: return isFinerEnabled();
     *             case  3: return isFinestEnabled();
     *             case  2: // fallthrough
     *             case  1: // fallthrough
     *             case  0: return false;
     *         }
     *     }
     * }
     *
     * @param  level A message logging level.
     * @return {@code true} if the given message level is currently being logged.
     */
    @Override
    public abstract boolean isLoggable(Level level);

    /**
     * Logs a {@link Level#SEVERE SEVERE} message.
     *
     * @param message The message to log.
     */
    @Override
    public abstract void severe(String message);

    /**
     * Logs a {@link Level#WARNING WARNING} message.
     *
     * @param message The message to log.
     */
    @Override
    public abstract void warning(String message);

    /**
     * Logs an {@link Level#INFO INFO} message.
     *
     * @param message The message to log.
     */
    @Override
    public abstract void info(String message);

    /**
     * Logs an {@link Level#CONFIG CONFIG} message.
     *
     * @param message The message to log.
     */
    @Override
    public abstract void config(String message);

    /**
     * Logs a {@link Level#FINE FINE} message.
     *
     * @param message The message to log.
     */
    @Override
    public abstract void fine(String message);

    /**
     * Logs a {@link Level#FINER FINER} message.
     *
     * @param message The message to log.
     */
    @Override
    public abstract void finer(String message);

    /**
     * Logs a {@link Level#FINEST FINEST} message.
     *
     * @param message The message to log.
     */
    @Override
    public abstract void finest(String message);

    /**
     * Logs a method entry to the {@linkplain #getDebugLevel debug level}. Compared to the
     * default {@link Logger}, this implementation bypass the level check in order to let
     * the backing logging framework do its own check.
     *
     * @param sourceClass  Name of class that issued the logging request.
     * @param sourceMethod Name of method that is being entered.
     */
    @Override
    public void entering(final String sourceClass, final String sourceMethod) {
        logp(getDebugLevel(), sourceClass, sourceMethod, "ENTRY");
    }

    /**
     * Logs a method entry to the {@linkplain #getDebugLevel debug level} with one parameter.
     * Compared to the default {@link Logger}, this implementation bypass the level check in
     * order to let the backing logging framework do its own check.
     *
     * @param sourceClass  Name of class that issued the logging request.
     * @param sourceMethod Name of method that is being entered.
     * @param param        Parameter to the method being entered.
     */
    @Override
    public void entering(String sourceClass, String sourceMethod, Object param) {
        logp(getDebugLevel(), sourceClass, sourceMethod, "ENTRY {0}", param);
    }

    /**
     * Logs a method entry to the {@linkplain #getDebugLevel debug level} with many parameters.
     * Compared to the default {@link Logger}, this implementation bypass the level check in
     * order to let the backing logging framework do its own check.
     *
     * @param sourceClass  Name of class that issued the logging request.
     * @param sourceMethod Name of method that is being entered.
     * @param params       Array of parameters to the method being entered.
     */
    @Override
    public void entering(final String sourceClass, final String sourceMethod, final Object[] params) {
        final String message;
        if (params == null) {
            message = "ENTRY";
        } else switch (params.length) {
            case 0: message = "ENTRY";         break;
            case 1: message = "ENTRY {0}";     break;
            case 2: message = "ENTRY {0} {1}"; break;
            default: {
                final StringBuilder builder = new StringBuilder("ENTRY");
                for (int i=0; i<params.length; i++) {
                    builder.append(" {").append(i).append('}');
                }
                message = builder.toString();
                break;
            }
        }
        logp(getDebugLevel(), sourceClass, sourceMethod, message, params);
    }

    /**
     * Logs a method return to the {@linkplain #getDebugLevel debug level}. Compared to the
     * default {@link Logger}, this implementation bypass the level check in order to let
     * the backing logging framework do its own check.
     *
     * @param sourceClass  Name of class that issued the logging request.
     * @param sourceMethod Name of the method.
     */
    @Override
    public void exiting(final String sourceClass, final String sourceMethod) {
        logp(getDebugLevel(), sourceClass, sourceMethod, "RETURN");
    }

    /**
     * Logs a method return to the {@linkplain #getDebugLevel debug level}. Compared to the
     * default {@link Logger}, this implementation bypass the level check in order to let
     * the backing logging framework do its own check.
     *
     * @param sourceClass  Name of class that issued the logging request.
     * @param sourceMethod Name of the method.
     * @param result       Object that is being returned.
     */
    @Override
    public void exiting(String sourceClass, String sourceMethod, Object result) {
        logp(getDebugLevel(), sourceClass, sourceMethod, "RETURN {0}", result);
    }

    /**
     * Logs a method failure to the {@linkplain #getDebugLevel debug level}. Compared to the
     * default {@link Logger}, this implementation bypass the level check in order to let
     * the backing logging framework do its own check.
     *
     * @param sourceClass  Name of class that issued the logging request.
     * @param sourceMethod Name of the method.
     * @param thrown       The Throwable that is being thrown.
     */
    @Override
    public void throwing(String sourceClass, String sourceMethod, Throwable thrown) {
        logp(getDebugLevel(), sourceClass, sourceMethod, "THROW", thrown);
    }

    /**
     * Logs a record. The default implementation delegates to one of the
     * {@link #logrb(Level,String,String,ResourceBundle,String,Object[]) logrb} or
     * {@link #logp(Level,String,String,String)} methods.
     *
     * @param record The log record to be published.
     */
    @Override
    public void log(final LogRecord record) {
        /*
         * The filter should always be null since we overrode the 'setFilter' method as a no-op.
         * But we check it anyway as matter of principle just in case some subclass overrides the
         * 'getFilter()' method. This is the only method where we can do this check cheaply. Note
         * that this is NOT the check for logging level; Filters are for user-specified criterions.
         */
        final Filter filter = getFilter();
        if (filter != null && !filter.isLoggable(record)) {
            return;
        }
        final Level     level        = record.getLevel();
        final String    sourceClass  = record.getSourceClassName();
        final String    sourceMethod = record.getSourceMethodName();
        final String    message      = record.getMessage();
        final Object[]  params       = record.getParameters();
        final Throwable thrown       = record.getThrown();
        final ResourceBundle bundle  = record.getResourceBundle();
        final boolean useThrown = (thrown != null) && (params == null || params.length == 0);
        if (bundle != null) {
            if (useThrown) {
                logrb(level, sourceClass, sourceMethod, bundle, message, thrown);
            } else {
                logrb(level, sourceClass, sourceMethod, bundle, message, params);
            }
        } else {
            final String bundleName = record.getResourceBundleName();
            if (bundleName != null) {
                if (useThrown) {
                    logrb(level, sourceClass, sourceMethod, bundleName, message, thrown);
                } else {
                    logrb(level, sourceClass, sourceMethod, bundleName, message, params);
                }
            } else {
                if (useThrown) {
                    logp(level, sourceClass, sourceMethod, message, thrown);
                } else {
                    logp(level, sourceClass, sourceMethod, message, params);
                }
            }
        }
    }

    /**
     * Logs a record at the specified level. The default implementation delegates to one of the
     * {@link #severe(String) severe}, {@link #warning(String) warning}, {@link #info(String) info},
     * {@link #config(String) config}, {@link #fine(String) fine}, {@link #finer(String) finer} or
     * {@link #finest(String) finest} methods according the supplied level.
     *
     * @param level   One of the message level identifiers.
     * @param message The message to log.
     */
    @Override
    @SuppressWarnings("fallthrough")
    public void log(final Level level, final String message) {
        final int n = level.intValue();
        switch (n / 100) {
            default: {
                if (n < 0 || n == Integer.MAX_VALUE) break;
                // MAX_VALUE is a special value for Level.OFF. Otherwise and
                // if positive, fallthrough since we are greater than SEVERE.
            }
            case 10: severe (message); break;
            case  9: warning(message); break;
            case  8: info   (message); break;
            case  7: config (message); break;
            case  6:
            case  5: fine   (message); break;
            case  4: finer  (message); break;
            case  3: finest (message); break;
            case  2: /* Logging OFF */
            case  1: /* Logging OFF */
            case  0: /* Logging OFF */ break;
        }
    }

    /**
     * Logs a record at the specified level. The default implementation discards the exception
     * and delegates to <code>{@linkplain #log(Level,String) log}(level, message)</code>.
     *
     * @param level   One of the message level identifiers.
     * @param message The message to log.
     * @param thrown  Throwable associated with log message.
     */
    @Override
    public void log(final Level level, final String message, final Throwable thrown) {
        log(level, message);
    }

    /**
     * Logs a record at the specified level. The default implementation delegates to
     * <code>{@linkplain #log(Level,String,Object[]) log}(level, message, params)</code>
     * where the {@code params} array is built from the {@code param} object.
     *
     * @param level   One of the message level identifiers.
     * @param message The message to log.
     * @param param   Parameter to the method being entered.
     */
    @Override
    public void log(final Level level, final String message, final Object param) {
        log(level, message, asArray(param));
    }

    /**
     * Logs a record at the specified level.
     * The default implementation formats the message immediately, then delegates to
     * <code>{@linkplain #log(Level,String) log}(level, message)</code>.
     *
     * @param level   One of the message level identifiers.
     * @param message The message to log.
     * @param params  Array of parameters to the method being entered.
     */
    @Override
    public void log(final Level level, final String message, final Object[] params) {
        log(level, format(message, params));
    }

    /**
     * Logs a record at the specified level. The default implementation discards
     * the source class and source method, then delegates to
     * <code>{@linkplain #log(Level,String) log}(level, message)</code>.
     *
     * @param level        One of the message level identifiers.
     * @param sourceClass  Name of class that issued the logging request.
     * @param sourceMethod Name of the method.
     * @param message      The message to log.
     */
    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod,
                     final String message)
    {
        log(level, message);
    }

    /**
     * Logs a record at the specified level. The default implementation discards
     * the source class and source method, then delegates to
     * <code>{@linkplain #log(Level,String,Throwable) log}(level, message, thrown)</code>.
     *
     * @param level        One of the message level identifiers.
     * @param sourceClass  Name of class that issued the logging request.
     * @param sourceMethod Name of the method.
     * @param message      The message to log.
     * @param thrown       Throwable associated with log message.
     */
    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod,
                     final String message, final Throwable thrown)
    {
        log(level, message, thrown);
    }

    /**
     * Logs a record at the specified level. The default implementation delegates to
     * <code>{@linkplain #logp(Level,String,String,String,Object[]) logp}(level, sourceClass,
     * sourceMethod, message, params)</code> where the {@code params} array is built from the
     * {@code param} object.
     *
     * <p>Note that {@code sourceClass} and {@code sourceMethod} will be discarded unless the
     * target {@link #logp(Level,String,String,String) logp} method has been overridden.</p>
     *
     * @param level        One of the message level identifiers.
     * @param sourceClass  Name of class that issued the logging request.
     * @param sourceMethod Name of the method.
     * @param message      The message to log.
     * @param param        Parameter to the method being entered.
     */
    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod,
                     final String message, final Object param)
    {
        logp(level, sourceClass, sourceMethod, message, asArray(param));
    }

    /**
     * Logs a record at the specified level. The default implementation formats the message
     * immediately, then delegates to <code>{@linkplain #logp(Level,String,String,String)
     * logp}(level, sourceClass, sourceMethod, message)</code>.
     *
     * <p>Note that {@code sourceClass} and {@code sourceMethod} will be discarded unless the
     * target {@link #logp(Level,String,String,String) logp} method has been overridden.</p>
     *
     * @param level        One of the message level identifiers.
     * @param sourceClass  Name of class that issued the logging request.
     * @param sourceMethod Name of the method.
     * @param message      The message to log.
     * @param params       Array of parameters to the method being entered.
     */
    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod,
                     final String message, final Object[] params)
    {
        logp(level, sourceClass, sourceMethod, format(message, params));
    }

    /**
     * Logs a localizable record at the specified level. The default implementation localizes the
     * message immediately, then delegates to <code>{@linkplain #logp(Level,String,String,String,
     * Object[]) logp}(level, sourceClass, sourceMethod, message, params)</code>.
     *
     * @param level        One of the message level identifiers.
     * @param sourceClass  Name of class that issued the logging request.
     * @param sourceMethod Name of the method.
     * @param bundle       The resource bundle for localizing the message, or {@code null}.
     * @param message      The message to log.
     * @param params       Array of parameters to the method being entered.
     *
     * @since 0.5
     */
    public void logrb(final Level level, final String sourceClass, final String sourceMethod,
                      final ResourceBundle bundle, final String message, final Object... params)
    {
        logp(level, sourceClass, sourceMethod, localize(bundle, message), params);
    }

    /**
     * Logs a localizable record at the specified level. The default implementation localizes the
     * message immediately, then delegates to <code>{@linkplain #logp(Level,String,String,String,
     * Throwable) logp}(level, sourceClass, sourceMethod, message, thrown)</code>.
     *
     * @param level        One of the message level identifiers.
     * @param sourceClass  Name of class that issued the logging request.
     * @param sourceMethod Name of the method.
     * @param bundle       The resource bundle for localizing the message, or {@code null}.
     * @param message      The message to log.
     * @param thrown       Throwable associated with log message.
     *
     * @since 0.5
     */
    public void logrb(final Level level, final String sourceClass, final String sourceMethod,
                      final ResourceBundle bundle, final String message, final Throwable thrown)
    {
        logp(level, sourceClass, sourceMethod, localize(bundle, message), thrown);
    }

    /**
     * Logs a localizable record at the specified level. The default implementation localizes the
     * message immediately, then delegates to <code>{@linkplain #logp(Level,String,String,String)
     * logp}(level, sourceClass, sourceMethod, message)</code>.
     *
     * @param level        One of the message level identifiers.
     * @param sourceClass  Name of class that issued the logging request.
     * @param sourceMethod Name of the method.
     * @param bundleName   Name of resource bundle to localize message, or {@code null}.
     * @param message      The message to log.
     *
     * @deprecated JDK 8 has deprecated this method.
     */
    @Override
    @Deprecated
    public void logrb(final Level level, final String sourceClass, final String sourceMethod,
                      final String bundleName, final String message)
    {
        logp(level, sourceClass, sourceMethod, localize(bundleName, message));
    }

    /**
     * Logs a localizable record at the specified level. The default implementation localizes the
     * message immediately, then delegates to <code>{@linkplain #logp(Level,String,String,String,
     * Throwable) logp}(level, sourceClass, sourceMethod, message, thrown)</code>.
     *
     * @param level        One of the message level identifiers.
     * @param sourceClass  Name of class that issued the logging request.
     * @param sourceMethod Name of the method.
     * @param bundleName   Name of resource bundle to localize message, or {@code null}.
     * @param message      The message to log.
     * @param thrown       Throwable associated with log message.
     *
     * @deprecated JDK 8 has deprecated this method.
     */
    @Override
    @Deprecated
    public void logrb(final Level level, final String sourceClass, final String sourceMethod,
                      final String bundleName, final String message, final Throwable thrown)
    {
        logp(level, sourceClass, sourceMethod, localize(bundleName, message), thrown);
    }

    /**
     * Logs a localizable record at the specified level. The default implementation localizes the
     * message immediately, then delegates to <code>{@linkplain #logp(Level,String,String,String,
     * Object) logp}(level, sourceClass, sourceMethod, message, param)</code>.
     *
     * @param level        One of the message level identifiers.
     * @param sourceClass  Name of class that issued the logging request.
     * @param sourceMethod Name of the method.
     * @param bundleName   Name of resource bundle to localize message, or {@code null}.
     * @param message      The message to log.
     * @param param        Parameter to the method being entered.
     *
     * @deprecated JDK 8 has deprecated this method.
     */
    @Override
    @Deprecated
    public void logrb(final Level level, final String sourceClass, final String sourceMethod,
                      final String bundleName, final String message, final Object param)
    {
        logp(level, sourceClass, sourceMethod, localize(bundleName, message), param);
    }

    /**
     * Logs a localizable record at the specified level. The default implementation localizes the
     * message immediately, then delegates to <code>{@linkplain #logp(Level,String,String,String,
     * Object[]) logp}(level, sourceClass, sourceMethod, message, params)</code>.
     *
     * @param level        One of the message level identifiers.
     * @param sourceClass  Name of class that issued the logging request.
     * @param sourceMethod Name of the method.
     * @param bundleName   Name of resource bundle to localize message, or {@code null}.
     * @param message      The message to log.
     * @param params       Array of parameters to the method being entered.
     *
     * @deprecated JDK 8 has deprecated this method.
     */
    @Override
    @Deprecated
    public void logrb(final Level level, final String sourceClass, final String sourceMethod,
                      final String bundleName, final String message, final Object[] params)
    {
        logp(level, sourceClass, sourceMethod, localize(bundleName, message), params);
    }

    /**
     * Do nothing since this logger adapter does not supports handlers.
     * The configuration should be fully controlled by the external logging framework
     * (e.g. <a href="http://commons.apache.org/logging/">Commons-logging</a>) instead,
     * which is not expected to use {@link Handler} objects.
     *
     * @param handler A logging handler, ignored in default implementation.
     */
    @Override
    public void addHandler(Handler handler) {
    }

    /**
     * Do nothing since this logger adapter does not support handlers.
     *
     * @param handler A logging handler, ignored in default implementation.
     */
    @Override
    public void removeHandler(Handler handler) {
    }

    /**
     * Do nothing since this logger never use parent handlers. This is consistent
     * with {@link #addHandler} not allowing to add any handlers, and avoid mixing
     * loggings from the external framework with JDK loggings.
     *
     * @param useParentHandlers Ignored in default implementation.
     */
    @Override
    public void setUseParentHandlers(boolean useParentHandlers) {
    }

    /**
     * Do nothing since this logger adapter does not support arbitrary parents.
     * More specifically, it should not inherits any configuration from a parent
     * logger using the JDK logging framework.
     *
     * @param parent Ignored in default implementation.
     */
    @Override
    public void setParent(Logger parent) {
    }

    /**
     * Do nothing since this logger adapter does not support filters.  It is difficult to query
     * efficiently the filter in this {@code LoggerAdapter} architecture (e.g. we would need to
     * make sure that {@link Filter#isLoggable} is invoked only once even if a {@code log} call
     * is cascaded into many other {@code log} calls, and this test must works in multi-threads
     * environment).
     *
     * @param filter Ignored in default implementation.
     */
    @Override
    public void setFilter(Filter filter) {
    }

    /**
     * Wraps the specified object in an array. This is a helper method for
     * {@code log(..., Object)} methods that delegate their work to {@code log(..., Object[])}
     */
    private static Object[] asArray(final Object param) {
        return (param != null) ? new Object[] {param} : null;
    }

    /**
     * Formats the specified message. This is a helper method for
     * {@code log(..., Object[])} methods that delegate their work to {@code log(...)}
     */
    private static String format(String message, final Object[] params) {
        if (params != null && params.length != 0) {
            if (MESSAGE_FORMAT.matcher(message).find()) try {
                message = MessageFormat.format(message, params);
            } catch (IllegalArgumentException e) {
                // The default Formatter.messageFormat implementation ignores this exception
                // and uses the pattern as the message, so we mimic its behavior here.
            }
        }
        return message;
    }

    /**
     * Localizes the specified message. This is a helper method for
     * {@code logrb(...)} methods that delegate their work to {@code logp(...)}
     */
    private static String localize(final String bundleName, String message) {
        if (bundleName != null) try {
            message = ResourceBundle.getBundle(bundleName).getString(message);
        } catch (MissingResourceException e) {
            // The default Formatter.messageFormat implementation ignores this exception
            // and uses the bundle key as the message, so we mimic its behavior here.
        }
        return message;
    }

    /**
     * Localizes the specified message. This is a helper method for
     * {@code logrb(...)} methods that delegate their work to {@code logp(...)}
     */
    private static String localize(final ResourceBundle bundle, String message) {
        if (bundle != null) try {
            message = bundle.getString(message);
        } catch (MissingResourceException e) {
            // The default Formatter.messageFormat implementation ignores this exception
            // and uses the bundle key as the message, so we mimic its behavior here.
        }
        return message;
    }
}
