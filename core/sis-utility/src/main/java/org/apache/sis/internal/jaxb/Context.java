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
package org.apache.sis.internal.jaxb;

import java.util.Map;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.apache.sis.util.Version;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.logging.WarningListener;
import org.apache.sis.xml.MarshalContext;
import org.apache.sis.xml.ValueConverter;
import org.apache.sis.xml.ReferenceResolver;


/**
 * Thread-local status of a marshalling or unmarshalling processes, also occasionally used for other processes.
 * All non-static methods in this class except {@link #finish()} are implementation of public API.
 * All static methods are internal API. Those methods expect a {@code Context} instance as their first argument.
 * They should be though as if they were normal member methods, except that they accept {@code null} instance
 * if no (un)marshalling is in progress.
 *
 * <p>While this class is primarily used for (un)marshalling processes, it may also be opportunistically used
 * for other processes like {@link org.apache.sis.metadata.AbstractMetadata#equals(Object)}. The class name is
 * only "{@code Context}" for that reason.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.07)
 * @version 0.4
 * @module
 */
public final class Context extends MarshalContext {
    /**
     * The bit flag telling if a marshalling process is under progress.
     * This flag is unset for unmarshalling processes.
     */
    public static final int MARSHALLING = 1;

    /**
     * The bit flag for enabling substitution of language codes by character strings.
     *
     * @see org.apache.sis.xml.XML#STRING_SUBSTITUTES
     */
    public static final int SUBSTITUTE_LANGUAGE = 2;

    /**
     * The bit flag for enabling substitution of country codes by character strings.
     *
     * @see org.apache.sis.xml.XML#STRING_SUBSTITUTES
     */
    public static final int SUBSTITUTE_COUNTRY = 4;

    /**
     * The bit flag for enabling substitution of filenames by character strings.
     *
     * @see org.apache.sis.xml.XML#STRING_SUBSTITUTES
     */
    public static final int SUBSTITUTE_FILENAME = 8;

    /**
     * The bit flag for enabling substitution of mime types by character strings.
     *
     * @see org.apache.sis.xml.XML#STRING_SUBSTITUTES
     */
    public static final int SUBSTITUTE_MIMETYPE = 16;

    /**
     * The thread-local context. Elements are created in the constructor, and removed in a
     * {@code finally} block by the {@link #finish()} method. This {@code ThreadLocal} shall
     * not contain any value when no (un)marshalling is in progress.
     */
    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<Context>();

    /**
     * Various boolean attributes determines by the above static constants.
     */
    private int bitMasks;

    /**
     * The locale to use for marshalling, or {@code null} if no locale were explicitly specified.
     */
    private Locale locale;

    /**
     * The timezone, or {@code null} if unspecified.
     * In the later case, an implementation-default (typically UTC) timezone is used.
     */
    private TimeZone timezone;

    /**
     * The base URL of ISO 19139 (or other standards) schemas. The valid values
     * are documented in the {@link org.apache.sis.xml.XML#SCHEMAS} property.
     */
    private Map<String,String> schemas;

    /**
     * The GML version to be marshalled or unmarshalled, or {@code null} if unspecified.
     * If null, than the latest version is assumed.
     */
    private Version versionGML;

    /**
     * The reference resolver currently in use, or {@code null} for {@link ReferenceResolver#DEFAULT}.
     */
    private ReferenceResolver resolver;

    /**
     * The value converter currently in use, or {@code null} for {@link ValueConverter#DEFAULT}.
     */
    private ValueConverter converter;

    /**
     * The object to inform about warnings, or {@code null} if none.
     */
    private WarningListener<?> warningListener;

    /**
     * The context which was previously used. This form a linked list allowing
     * to push properties (e.g. {@link #pushLocale(Locale)}) and pull back the
     * context to its previous state once finished.
     */
    private final Context previous;

    /**
     * Invoked when a marshalling or unmarshalling process is about to begin.
     * Must be followed by a call to {@link #finish()} in a {@code finally} block.
     *
     * {@preformat java
     *     Context context = new Context(…);
     *     try {
     *         ...
     *     } finally {
     *         context.finish();
     *     }
     * }
     *
     * @param  bitMasks        A combination of {@link #MARSHALLING}, {@code SUBSTITUTE_*} or other bit masks.
     * @param  locale          The locale, or {@code null} if unspecified.
     * @param  timezone        The timezone, or {@code null} if unspecified.
     * @param  schemas         The schemas root URL, or {@code null} if none.
     * @param  versionGML      The GML version, or {@code null}.
     * @param  resolver        The resolver in use.
     * @param  converter       The converter in use.
     * @param  warningListener The object to inform about warnings.
     */
    public Context(final int                bitMasks,
                   final Locale             locale,   final TimeZone       timezone,
                   final Map<String,String> schemas,  final Version        versionGML,
                   final ReferenceResolver  resolver, final ValueConverter converter,
                   final WarningListener<?> warningListener)
    {
        this.bitMasks        = bitMasks;
        this.locale          = locale;
        this.timezone        = timezone;
        this.schemas         = schemas; // No clone, because this class is internal.
        this.versionGML      = versionGML;
        this.resolver        = resolver;
        this.converter       = converter;
        this.warningListener = warningListener;
        previous = current();
        CURRENT.set(this);
    }

    /**
     * Inherits all configuration from the previous context, if any.
     *
     * @param previous The context from which to inherit the configuration, or {@code null}.
     *
     * @see #push(Locale)
     */
    private Context(final Context previous) {
        if (previous != null) {
            bitMasks         = previous.bitMasks;
            locale           = previous.locale;
            timezone         = previous.timezone;
            schemas          = previous.schemas;
            versionGML       = previous.versionGML;
            resolver         = previous.resolver;
            converter        = previous.converter;
            warningListener  = previous.warningListener;
        }
        this.previous = previous;
        CURRENT.set(this);
    }

    /**
     * Returns the locale to use for marshalling, or {@code null} if no locale were explicitly
     * specified.
     */
    @Override
    public final Locale getLocale() {
        return locale;
    }

    /**
     * Returns the timezone to use for marshalling, or {@code null} if none were explicitely
     * specified.
     */
    @Override
    public final TimeZone getTimeZone() {
        return timezone;
    }

    /**
     * Returns the schema version of the XML document being (un)marshalled.
     * See the super-class javadoc for the list of prefix that we shall support.
     */
    @Override
    public final Version getVersion(final String prefix) {
        if (prefix.equals("gml")) {
            return versionGML;
        }
        // Future SIS versions may add more cases here.
        return null;
    }

    /*
     * ---- END OF PUBLIC API --------------------------------------------------------------
     *
     * Following are internal API. They are provided as static methods with a Context
     * argument rather than normal member methods in order to accept null context.
     */

    /**
     * Returns the context of the XML (un)marshalling currently progressing in the current thread,
     * or {@code null} if none.
     *
     * @return The current (un)marshalling context, or {@code null} if none.
     */
    public static Context current() {
        return CURRENT.get();
    }

    /**
     * Returns {@code true} if XML marshalling is under progress.
     * This convenience method is implemented by:
     *
     * {@preformat java
     *     return isFlagSet(current(), MARSHALLING);
     * }
     *
     * Callers should use the {@link #isFlagSet(Context, int)} method instead if the
     * {@code Context} instance is known, for avoiding a call to {@link #current()}.
     *
     * @return {@code true} if XML marshalling is under progress.
     */
    public static boolean isMarshalling() {
        return isFlagSet(current(), MARSHALLING);
    }

    /**
     * Returns {@code true} if the given flag is set.
     *
     * @param  context The current context, or {@code null} if none.
     * @param  flag One of {@link #MARSHALLING}, {@link #SUBSTITUTE_LANGUAGE},
     *         {@link #SUBSTITUTE_COUNTRY} or other bit masks.
     * @return {@code true} if the given flag is set.
     */
    public static boolean isFlagSet(final Context context, final int flag) {
        return (context != null) && (context.bitMasks & flag) != 0;
    }

    /**
     * Returns the base URL of ISO 19139 (or other standards) schemas.
     * The valid values are documented in the {@link org.apache.sis.xml.XML#SCHEMAS} property.
     * If the returned value is not empty, then this method guarantees it ends with {@code '/'}.
     *
     * {@note This method is static for the convenience of performing the check for null context.}
     *
     * @param  context The current context, or {@code null} if none.
     * @param  key One of the value documented in the "<cite>Map key</cite>" column of
     *         {@link org.apache.sis.xml.XML#SCHEMAS}.
     * @param  defaultSchema The value to return if no schema is found for the given key.
     * @return The base URL of the schema, or an empty buffer if none were specified.
     */
    public static StringBuilder schema(final Context context, final String key, String defaultSchema) {
        final StringBuilder buffer = new StringBuilder(128);
        if (context != null) {
            final Map<String,String> schemas = context.schemas;
            if (schemas != null) {
                final String schema = schemas.get(key);
                if (schema != null) {
                    defaultSchema = schema;
                }
            }
        }
        buffer.append(defaultSchema);
        final int length = buffer.length();
        if (length != 0 && buffer.charAt(length - 1) != '/') {
            buffer.append('/');
        }
        return buffer;
    }

    /**
     * Returns {@code true} if the GML version is equals or newer than the specified version.
     * If no GML version were specified, then this method returns {@code true}, i.e. newest
     * version is assumed.
     *
     * {@note This method is static for the convenience of performing the check for null context.}
     *
     * @param  context The current context, or {@code null} if none.
     * @param  version The version to compare to.
     * @return {@code true} if the GML version is equals or newer than the specified version.
     *
     * @see #getVersion(String)
     */
    public static boolean isGMLVersion(final Context context, final Version version) {
        if (context != null) {
            final Version versionGML = context.versionGML;
            if (versionGML != null) {
                return versionGML.compareTo(version) >= 0;
            }
        }
        return true;
    }

    /**
     * Returns the reference resolver in use for the current marshalling or unmarshalling process.
     * If no resolver were explicitely set, then this method returns {@link ReferenceResolver#DEFAULT}.
     *
     * {@note This method is static for the convenience of performing the check for null context.}
     *
     * @param  context The current context, or {@code null} if none.
     * @return The current reference resolver (never null).
     */
    public static ReferenceResolver resolver(final Context context) {
        if (context != null) {
            final ReferenceResolver resolver = context.resolver;
            if (resolver != null) {
                return resolver;
            }
        }
        return ReferenceResolver.DEFAULT;
    }

    /**
     * Returns the value converter in use for the current marshalling or unmarshalling process.
     * If no converter were explicitely set, then this method returns {@link ValueConverter#DEFAULT}.
     *
     * {@note This method is static for the convenience of performing the check for null context.}
     *
     * @param  context The current context, or {@code null} if none.
     * @return The current value converter (never null).
     */
    public static ValueConverter converter(final Context context) {
        if (context != null) {
            final ValueConverter converter = context.converter;
            if (converter != null) {
                return converter;
            }
        }
        return ValueConverter.DEFAULT;
    }

    /**
     * Sends the given warning to the warning listener if there is one, or logs the warning otherwise.
     * In the later case, this method logs to the logger specified by {@link LogRecord#getLoggerName()}
     * if defined, or to the {@code "org.apache.sis.xml"} logger otherwise.
     *
     * @param context The current context, or {@code null} if none.
     * @param source  The object that emitted a warning. Can not be null.
     * @param warning The warning.
     */
    @SuppressWarnings("unchecked")
    public static void warningOccured(final Context context, final Object source, final LogRecord warning) {
        String logger = warning.getLoggerName();
        if (logger == null) {
            warning.setLoggerName(logger = "org.apache.sis.xml");
        }
        if (context != null) {
            final WarningListener<?> warningListener = context.warningListener;
            if (warningListener != null && warningListener.getSourceClass().isInstance(source)) {
                ((WarningListener) warningListener).warningOccured(source, warning);
                return;
            }
        }
        /*
         * Log the warning without stack-trace, since this method shall be used only for non-fatal warnings
         * and we want to avoid polluting the logs.
         */
        warning.setThrown(null);
        Logging.getLogger(logger).log(warning);
    }

    /**
     * Convenience method for sending a warning for the given exception.
     * The logger will be {@code "org.apache.sis.xml"}.
     *
     * @param context The current context, or {@code null} if none.
     * @param source  The object that emitted a warning. Can not be null.
     * @param classe  The name of the class to declare as the warning source.
     * @param method  The name of the method to declare as the warning source.
     * @param cause   The exception which occurred.
     * @param warning {@code true} for {@link Level#WARNING}, or {@code false} for {@link Level#FILE}.
     */
    public static void warningOccured(final Context context, final Object source, final Class<?> classe,
            final String method, final Exception cause, final boolean warning)
    {
        final LogRecord record = new LogRecord(warning ? Level.WARNING : Level.FINE,
                Exceptions.formatChainedMessages(context != null ? context.getLocale() : null, null, cause));
        record.setSourceClassName(classe.getCanonicalName());
        record.setSourceMethodName(method);
        record.setThrown(cause);
        warningOccured(context, source, record);
    }

    /**
     * Sets the locale to the given value. The old locales are remembered and will
     * be restored by the next call to {@link #pull()}. This method can be invoked
     * when marshalling object that need to marshall their children in a different
     * locale, like below:
     *
     * {@preformat java
     *     private void beforeMarshal(Marshaller marshaller) {
     *         Context.push(language);
     *     }
     *
     *     private void afterMarshal(Marshaller marshaller) {
     *         Context.pull();
     *     }
     * }
     *
     * @param locale The locale to set, or {@code null}.
     */
    public static void push(final Locale locale) {
        final Context context = new Context(current());
        if (locale != null) {
            context.locale = locale;
        }
    }

    /**
     * Restores the locale (or any other setting) which was used prior the call
     * to {@link #push(Locale)}. It is not necessary to invoke this method in a
     * {@code finally} block if the parent {@code Context} is itself
     * disposed in a {@code finally} block.
     */
    public static void pull() {
        final Context current = current();
        if (current != null) {
            current.finish();
        }
    }

    /**
     * Invoked in a {@code finally} block when a unmarshalling process is finished.
     */
    public final void finish() {
        if (previous != null) {
            CURRENT.set(previous);
        } else {
            CURRENT.remove();
        }
    }
}
