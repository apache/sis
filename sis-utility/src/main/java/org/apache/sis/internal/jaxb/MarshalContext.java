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

import java.util.Collection;
import java.util.Map;
import java.util.Locale;
import java.util.TimeZone;
import org.opengis.metadata.Identifier;
import org.apache.sis.util.Version;
import org.apache.sis.xml.ValueConverter;
import org.apache.sis.xml.ReferenceResolver;
import org.apache.sis.util.collection.UnmodifiableArrayList;


/**
 * Thread-local status of a marshalling or unmarshalling process.
 * Contains also static methods for managing the collections to be marshalled.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.07)
 * @version 0.3
 * @module
 */
public final class MarshalContext extends org.apache.sis.xml.MarshalContext {
    /**
     * The bit flag telling if a marshalling process is under progress.
     * This flag is unset for unmarshalling processes.
     */
    public static final int MARSHALING = 1;

    /**
     * The bit flag for enabling substitution of language codes by character strings.
     */
    public static final int SUBSTITUTE_LANGUAGE = 2;

    /**
     * The bit flag for enabling substitution of country codes by character strings.
     */
    public static final int SUBSTITUTE_COUNTRY = 4;

    /**
     * The thread-local context.
     */
    private static final ThreadLocal<MarshalContext> CURRENT = new ThreadLocal<>();

    /**
     * The value converter currently in use, or {@code null} for {@link ValueConverter#DEFAULT}.
     */
    private ValueConverter converter;

    /**
     * The reference resolver currently in use, or {@code null} for {@link ReferenceResolver#DEFAULT}.
     */
    private ReferenceResolver resolver;

    /**
     * The GML version to be marshalled or unmarshalled, or {@code null} if unspecified.
     * If null, than the latest version is assumed.
     */
    private Version versionGML;

    /**
     * The base URL of ISO 19139 (or other standards) schemas. The valid values
     * are documented in the {@link org.apache.sis.xml.XML#SCHEMAS} property.
     */
    private Map<String,String> schemas;

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
     * Various boolean attributes determines by the above static constants.
     */
    private int bitMasks;

    /**
     * The context which was previously used. This form a linked list allowing
     * to push properties (e.g. {@link #pushLocale(Locale)}) and pull back the
     * context to its previous state once finished.
     */
    private final MarshalContext previous;

    /**
     * Invoked when a marshalling or unmarshalling process is about to begin.
     * Must be followed by a call to {@link #finish()} in a {@code finally} block.
     *
     * {@preformat java
     *     MarshalContext context = new MarshalContext(…);
     *     try {
     *         ...
     *     } finally {
     *         context.finish();
     *     }
     * }
     *
     * @param  converter  The converter in use.
     * @param  resolver   The resolver in use.
     * @param  versionGML The GML version, or {@code null}.
     * @param  schemas    The schemas root URL, or {@code null} if none.
     * @param  locale     The locale, or {@code null} if unspecified.
     * @param  timezone   The timezone, or {@code null} if unspecified.
     * @param  bitMasks   A combination of {@link #MARSHALING}, {@link #SUBSTITUTE_LANGUAGE},
     *                    {@link #SUBSTITUTE_COUNTRY} or other bit masks.
     */
    public MarshalContext(final ValueConverter converter, final ReferenceResolver resolver,
            final Version versionGML, final Map<String,String> schemas,
            final Locale locale, final TimeZone timezone, final int bitMasks)
    {
        this.converter  = converter;
        this.resolver   = resolver;
        this.versionGML = versionGML;
        this.schemas    = schemas; // No clone, because this class is internal.
        this.locale     = locale;
        this.timezone   = timezone;
        this.bitMasks   = bitMasks;
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
    private MarshalContext(final MarshalContext previous) {
        if (previous != null) {
            converter  = previous.converter;
            resolver   = previous.resolver;
            versionGML = previous.versionGML;
            schemas    = previous.schemas;
            locale     = previous.locale;
            timezone   = previous.timezone;
            bitMasks   = previous.bitMasks;
        }
        this.previous = previous;
        CURRENT.set(this);
    }

    /**
     * Returns the context of the XML (un)marshalling currently progressing in the current thread,
     * or {@code null} if none.
     *
     * @return The current (un)marshalling context, or {@code null} if none.
     */
    public static MarshalContext current() {
        return CURRENT.get();
    }

    /**
     * Returns {@code true} if the given flag is set.
     *
     * @param  flag One of {@link #MARSHALING}, {@link #SUBSTITUTE_LANGUAGE},
     *         {@link #SUBSTITUTE_COUNTRY} or other bit masks.
     * @return {@code true} if the given flag is set.
     */
    public static boolean isFlagSet(final int flag) {
        final MarshalContext current = current();
        return (current != null) && (current.bitMasks & flag) != 0;
    }

    /**
     * Returns the value converter in use for the current marshalling or unmarshalling process.
     * If no converter were explicitely set, then this method returns {@link ValueConverter#DEFAULT}.
     *
     * {@note This method is static for the convenience of performing the check for null context.}
     *
     * @param  current The current context, or {@code null} if none.
     * @return The current value converter (never null).
     */
    public static ValueConverter converter(final MarshalContext current) {
        if (current != null) {
            final ValueConverter converter = current.converter;
            if (converter != null) {
                return converter;
            }
        }
        return ValueConverter.DEFAULT;
    }

    /**
     * Returns the reference resolver in use for the current marshalling or unmarshalling process.
     * If no resolver were explicitely set, then this method returns {@link ReferenceResolver#DEFAULT}.
     *
     * {@note This method is static for the convenience of performing the check for null context.}
     *
     * @param  current The current context, or {@code null} if none.
     * @return The current reference resolver (never null).
     */
    public static ReferenceResolver resolver(final MarshalContext current) {
        if (current != null) {
            final ReferenceResolver resolver = current.resolver;
            if (resolver != null) {
                return resolver;
            }
        }
        return ReferenceResolver.DEFAULT;
    }

    /**
     * Returns the base URL of ISO 19139 (or other standards) schemas.
     * The valid values are documented in the {@link org.apache.sis.xml.XML#SCHEMAS} property.
     *
     * {@note This method is static for the convenience of performing the check for null context.}
     *
     * @param  current The current context, or {@code null} if none.
     * @param  key One of the value documented in the "<cite>Map key</cite>" column of
     *         {@link org.apache.sis.xml.XML#SCHEMAS}.
     * @param  defaultSchema The value to return if no schema is found for the given key.
     * @return The base URL of the schema, or {@code null} if none were specified.
     */
    public static String schema(final MarshalContext current, final String key, final String defaultSchema) {
        if (current != null) {
            final Map<String,String> schemas = current.schemas;
            if (schemas != null) {
                final String schema = schemas.get(key);
                if (schema != null) {
                    return schema;
                }
            }
        }
        return defaultSchema;
    }

    /**
     * Returns {@code true} if the GML version is equals or newer than the specified version.
     * If no GML version were specified, then this method returns {@code true}, i.e. newest
     * version is assumed.
     *
     * {@note This method is static for the convenience of performing the check for null context.}
     *
     * @param  current The current context, or {@code null} if none.
     * @param  version The version to compare to.
     * @return {@code true} if the GML version is equals or newer than the specified version.
     *
     * @see #getVersion(String)
     */
    public static boolean isGMLVersion(final MarshalContext current, final Version version) {
        if (current != null) {
            final Version versionGML = current.versionGML;
            if (versionGML != null) {
                return versionGML.compareTo(version) >= 0;
            }
        }
        return true;
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

    /**
     * Returns the timezone, or {@code null} if none were explicitely defined.
     * In the later case, an implementation-default (typically UTC) timezone is used.
     */
    @Override
    public final TimeZone getTimeZone() {
        return timezone;
    }

    /**
     * Returns the locale to use for marshalling, or {@code null} if no locale were explicitly
     * specified. A {@code null} value means that some locale-neutral language should be used
     * if available, or an implementation-default locale (typically English) otherwise.
     */
    @Override
    public final Locale getLocale() {
        return locale;
    }

    /**
     * Sets the locale to the given value. The old locales are remembered and will
     * be restored by the next call to {@link #pullLocale()}.
     *
     * @param locale The locale to set, or {@code null}.
     */
    public static void push(final Locale locale) {
        final MarshalContext current = new MarshalContext(current());
        if (locale != null) {
            current.locale = locale;
        }
    }

    /**
     * Restores the locale (or any other setting) which was used prior the call to
     * {@link #push(Locale)}.
     */
    public static void pull() {
        final MarshalContext current = current();
        if (current != null) {
            current.finish();
        }
    }

    /**
     * If marshalling, filters the given collection of identifiers in order to omit any identifiers
     * for which the authority is one of the {@link org.apache.sis.xml.IdentifierSpace} constants.
     *
     * @param  identifiers The identifiers to filter, or {@code null}.
     * @return The identifiers to marshal, or {@code null} if none.
     */
    public static Collection<Identifier> filterIdentifiers(Collection<Identifier> identifiers) {
        if (identifiers != null && isFlagSet(MARSHALING)) {
            int count = identifiers.size();
            if (count != 0) {
                final Identifier[] copy = identifiers.toArray(new Identifier[count]);
                for (int i=count; --i>=0;) {
                    final Identifier id = copy[i];
                    if (id == null || (id.getAuthority() instanceof NonMarshalledAuthority)) {
                        System.arraycopy(copy, i+1, copy, i, --count - i);
                    }
                }
                identifiers = (count != 0) ? UnmodifiableArrayList.wrap(copy, 0, count) : null;
            }
        }
        return identifiers;
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
