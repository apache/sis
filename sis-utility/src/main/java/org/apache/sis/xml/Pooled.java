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
package org.apache.sis.xml;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.ConcurrentModificationException;
import java.util.Locale;
import java.util.TimeZone;
import javax.xml.validation.Schema;
import javax.xml.bind.Marshaller;
import javax.xml.bind.JAXBException;
import javax.xml.bind.PropertyException;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.apache.sis.util.Version;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.internal.jaxb.MarshalContext;


/**
 * Base class of {@link PooledMarshaller} and {@link PooledUnmarshaller}.
 * This class provides basic service for saving the initial values of (un)marshaller properties,
 * in order to reset them to their initial values after usage. This is required in order to allow
 * (un)marshaller reuse. In addition this base class translates properties key from JDK 6 names to
 * "endorsed JAR" names if needed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
abstract class Pooled {
    /**
     * The keys of entries that can be stored in the {@link #schemas} map.
     * Those keys are documented in {@link XML#SCHEMAS}.
     */
    private static final String[] SCHEMA_KEYS = {"gmd"};

    /**
     * The prefix of property names which are provided in external (endorsed) implementation of JAXB.
     * This is slightly different than the prefix used by the implementation bundled with the JDK 6,
     * which is {@code "com.sun.xml.internal.bind"}.
     *
     * @see #convertPropertyKey(String)
     */
    static final String ENDORSED_PREFIX = "com.sun.xml.bind.";

    /**
     * {@code true} if the JAXB implementation is the one bundled in JDK 6, or {@code false}
     * if this is the external implementation provided as a JAR file in the endorsed directory.
     * If {@code true}, then an additional {@code "internal"} package name needs to be inserted
     * in the property keys.
     *
     * @see #convertPropertyKey(String)
     */
    private final boolean internal;

    /**
     * The initial state of the (un)marshaller. Will be filled only as needed,
     * often with null values (which must be supported by the map implementation).
     *
     * <ul>
     *   <li>For each entry having a key of type {@link Class}, the value is the argument
     *       to be given to a {@code marshaller.setFoo(value)} method.</li>
     *   <li>For each entry having a key of type {@link String}, the value is the argument
     *       to be given to the {@code marshaller.setProperty(key, value)} method.</li>
     * </ul>
     */
    private final Map<Object,Object> initialProperties;

    /**
     * The object converters to use during (un)marshalling.
     * Can be set by the {@link XML#CONVERTER} property.
     */
    private ValueConverter converter;

    /**
     * The reference resolver to use during unmarshalling.
     * Can be set by the {@link XML#RESOLVER} property.
     */
    private ReferenceResolver resolver;

    /**
     * The GML version to be marshalled or unmarshalled, or {@code null} if unspecified.
     * If null, then the latest version is assumed.
     */
    private Version gmlVersion;

    /**
     * The base URL of ISO 19139 (or other standards) schemas. It shall be an unmodifiable
     * instance because {@link #getProperty(String)} returns a direct reference to the user.
     * The valid values are documented in the {@link XML#SCHEMAS} property.
     */
    private Map<String,String> schemas;

    /**
     * An optional locale for {@link org.opengis.util.InternationalString} and
     * {@link org.opengis.util.CodeList}. Can be set by the {@link XML#LOCALE} property.
     */
    private Locale locale;

    /**
     * The timezone, or {@code null} if unspecified.
     *  Can be set by the {@link XML#TIMEZONE} property.
     */
    private TimeZone timezone;

    /**
     * Bit masks for various boolean attributes. This include whatever the language codes
     * or the country codes should be substituted by a simpler character string elements.
     * Those bits are determined by the {@link XML#STRING_SUBSTITUTES} property.
     */
    private int bitMasks;

    /**
     * The {@link System#nanoTime()} value of the last call to {@link #reset()}.
     * This is used for disposing (un)marshallers that have not been used for a while,
     * since {@code reset()} is invoked just before to push a (un)marshaller in the pool.
     */
    volatile long resetTime;

    /**
     * Default constructor.
     *
     * @param internal {@code true} if the JAXB implementation is the one bundled in JDK 6,
     *        or {@code false} if this is the external implementation provided as a JAR file
     *        in the endorsed directory.
     */
    Pooled(final boolean internal) {
        this.internal = internal;
        initialProperties = new LinkedHashMap<Object,Object>();
        bitMasks = initialBitMasks();
    }

    /**
     * Returns the initial value of {@link MarshalContext#bitMasks}. Shall be 0 if this object is
     * an unmarshaller, or {@link MarshalContext#MARSHALLING} if it is an {@link Unmarshaller}.
     */
    private int initialBitMasks() {
        return (this instanceof Marshaller) ? MarshalContext.MARSHALLING : 0;
    }

    /**
     * Releases resources and resets the (un)marshaller to its initial state.
     * This method is invoked by {@link MarshallerPool} just before to push a
     * (un)marshaller in the pool after its usage.
     *
     * @throws JAXBException If an error occurred while restoring a property.
     */
    public final void reset() throws JAXBException {
        for (final Map.Entry<Object,Object> entry : initialProperties.entrySet()) {
            reset(entry.getKey(), entry.getValue());
        }
        initialProperties.clear();
        converter  = null;
        resolver   = null;
        gmlVersion = null;
        schemas    = null;
        locale     = null;
        timezone   = null;
        bitMasks   = initialBitMasks();
        resetTime  = System.nanoTime();
    }

    /**
     * Resets the given marshaller property to its initial state. This method is invoked
     * automatically by the {@link #reset()} method. The key is either a {@link String}
     * or a {@link Class}. If this is a string, then the value shall be given to the
     * {@code setProperty(key, value)} method. Otherwise the value shall be given to
     * {@code setFoo(value)} method where {@code "Foo"} is determined from the key.
     *
     * @param  key   The property to reset.
     * @param  value The initial value to give to the property.
     * @throws JAXBException If an error occurred while restoring a property.
     */
    protected abstract void reset(final Object key, final Object value) throws JAXBException;

    /**
     * Returns {@code true} if the initial property is already saved for the given key.
     * Note that a property set to {@code null} is still considered as defined.
     */
    final boolean isPropertySaved(final Class<?> key) {
        return initialProperties.containsKey(key);
    }

    /**
     * Saves the current value of a property. This method is invoked before a value is
     * modified for the first time, in order to allow {@link #reset()} to restore the
     * (un)marshaller to its initial state.
     *
     * @param type  The property to save.
     * @param value The current value of the property.
     */
    final <E> void saveProperty(final Class<E> type, final E value) {
        if (initialProperties.put(type, value) != null) {
            // Should never happen, unless on concurrent changes in a backgroung thread.
            throw new ConcurrentModificationException(Errors.format(Errors.Keys.UnexpectedChange_1,
                    type.getInterfaces()[0].getSimpleName() + ".get" + type.getSimpleName()));
        }
    }

    /**
     * Converts a property key from the JAXB name to the underlying implementation name.
     * This applies only to property keys in the {@code "com.sun.xml.bind"} namespace.
     *
     * @param  key The JAXB property key.
     * @return The property key to use.
     */
    private String convertPropertyKey(String key) {
        if (internal && key.startsWith(ENDORSED_PREFIX)) {
            final StringBuilder buffer = new StringBuilder(key.length() + 10);
            key = buffer.append("com.sun.xml.internal.bind.")
                    .append(key, ENDORSED_PREFIX.length(), key.length()).toString();
        }
        return key;
    }

    /**
     * A method which is common to both {@code Marshaller} and {@code Unmarshaller}.
     * It saves the initial state if it was not already done, but subclasses will
     * need to complete the work.
     */
    public final void setProperty(String name, final Object value) throws PropertyException {
        try {
            /* switch (name) */ {
                if (name.equals(XML.CONVERTER)) {
                    converter = (ValueConverter) value;
                    return;
                }
                else if (name.equals(XML.RESOLVER)) {
                    resolver = (ReferenceResolver) value;
                    return;
                }
                else if (name.equals(XML.SCHEMAS)) {
                    final Map<?,?> map = (Map<?,?>) value;
                    Map<String,String> copy = null;
                    if (map != null) {
                        copy = new HashMap<String,String>(4);
                        for (final String key : SCHEMA_KEYS) {
                            final Object schema = map.get(key);
                            if (schema != null) {
                                if (!(schema instanceof String)) {
                                    throw new PropertyException(Errors.format(Errors.Keys.IllegalPropertyClass_2,
                                            name + "[\"" + key + "\"]", value.getClass()));
                                }
                                copy.put(key, (String) schema);
                            }
                        }
                        copy = CollectionsExt.unmodifiableOrCopy(copy);
                    }
                    schemas = copy;
                    return;
                }
                else if (name.equals(XML.GML_VERSION)) {
                    gmlVersion = (value instanceof CharSequence) ? new Version(value.toString()) : (Version) value;
                    return;
                }
                else if (name.equals(XML.LOCALE)) {
                    locale = (Locale) value;
                    return;
                }
                else if (name.equals(XML.TIMEZONE)) {
                    timezone = (TimeZone) value;
                    return;
                }
                else if (name.equals(XML.STRING_SUBSTITUTES)) {
                    int mask = initialBitMasks();
                    final CharSequence[] substitutes = CharSequences.split((CharSequence) value, ',');
                    if (substitutes != null) {
                        for (final CharSequence substitute : substitutes) {
                            if (CharSequences.equalsIgnoreCase(substitute, "language")) {
                                mask |= MarshalContext.SUBSTITUTE_LANGUAGE;
                            } else if (CharSequences.equalsIgnoreCase(substitute, "country")) {
                                mask |= MarshalContext.SUBSTITUTE_COUNTRY;
                            }
                        }
                    }
                    bitMasks = mask;
                    return;
                }
            }
        } catch (ClassCastException e) {
            throw new PropertyException(Errors.format(
                    Errors.Keys.IllegalPropertyClass_2, name, value.getClass()), e);
        }
        name = convertPropertyKey(name);
        if (!initialProperties.containsKey(name)) {
            if (initialProperties.put(name, getStandardProperty(name)) != null) {
                // Should never happen, unless on concurrent changes in a backgroung thread.
                throw new ConcurrentModificationException(name);
            }
        }
        setStandardProperty(name, value);
    }

    /**
     * A method which is common to both {@code Marshaller} and {@code Unmarshaller}.
     */
    public final Object getProperty(final String name) throws PropertyException {
        /*switch (name)*/ {
            if (name.equals(XML.CONVERTER))   return converter;
            if (name.equals(XML.RESOLVER))    return resolver;
            if (name.equals(XML.SCHEMAS))     return schemas;
            if (name.equals(XML.GML_VERSION)) return gmlVersion;
            if (name.equals(XML.LOCALE))      return locale;
            if (name.equals(XML.TIMEZONE))    return timezone;
            if (name.equals(XML.STRING_SUBSTITUTES)) {
                final StringBuilder buffer = new StringBuilder();
                if ((bitMasks & MarshalContext.SUBSTITUTE_LANGUAGE) != 0) buffer.append("language,");
                if ((bitMasks & MarshalContext.SUBSTITUTE_COUNTRY)  != 0) buffer.append("country,");
                final int length = buffer.length();
                if (length != 0) {
                    buffer.setLength(length - 1); // Remove the last coma.
                    return buffer.toString();
                }
                return null;
            }
            else {
                return getStandardProperty(convertPropertyKey(name));
            }
        }
    }

    /**
     * Sets the given property to the wrapped (un)marshaller. This method is invoked
     * automatically when the property given to the {@link #setProperty(String, Object)}
     * method was not one of the {@link XML} constants.
     */
    abstract void setStandardProperty(String name, Object value) throws PropertyException;

    /**
     * Gets the given property from the wrapped (un)marshaller. This method is invoked
     * automatically when the property key given to the {@link #getProperty(String)}
     * method was not one of the {@link XML} constants.
     */
    abstract Object getStandardProperty(String name) throws PropertyException;

    /**
     * Delegates to {@code setAdapter(adapter.getClass(), adapter)} as specified
     * in {@code [Un]Marshaller} javadoc.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    public final void setAdapter(final XmlAdapter adapter) {
        setAdapter((Class) adapter.getClass(), adapter);
    }

    /**
     * A method which is common to both {@code Marshaller} and {@code Unmarshaller}.
     * It saves the initial state if it was not already done, but subclasses will
     * need to complete the work.
     */
    @SuppressWarnings("rawtypes")
    public <A extends XmlAdapter> void setAdapter(final Class<A> type, final A adapter) {
        if (!isPropertySaved(type)) {
            saveProperty(type, getAdapter(type));
        }
    }

    /**
     * A method which is common to both {@code Marshaller} and {@code Unmarshaller}.
     */
    @SuppressWarnings("rawtypes")
    public abstract <A extends XmlAdapter> A getAdapter(final Class<A> type);

    /**
     * A method which is common to both {@code Marshaller} and {@code Unmarshaller}.
     * It saves the initial state if it was not already done, but subclasses will
     * need to complete the work.
     */
    public void setSchema(final Schema schema) {
        if (!isPropertySaved(Schema.class)) {
            saveProperty(Schema.class, getSchema());
        }
    }

    /**
     * A method which is common to both {@code Marshaller} and {@code Unmarshaller}.
     */
    public abstract Schema getSchema();

    /**
     * A method which is common to both {@code Marshaller} and {@code Unmarshaller}.
     * It saves the initial state if it was not already done, but subclasses will
     * need to complete the work.
     */
    public void setEventHandler(final ValidationEventHandler handler) throws JAXBException {
        if (!initialProperties.containsKey(ValidationEventHandler.class)) {
            saveProperty(ValidationEventHandler.class, getEventHandler());
        }
    }

    /**
     * A method which is common to both {@code Marshaller} and {@code Unmarshaller}.
     */
    public abstract ValidationEventHandler getEventHandler() throws JAXBException;

    /**
     * Must be invoked by subclasses before a {@code try} block performing a (un)marshalling
     * operation. Must be followed by a call to {@code finish()} in a {@code finally} block.
     *
     * {@preformat java
     *     MarshalContext context = begin();
     *     try {
     *         ...
     *     } finally {
     *         context.finish();
     *     }
     * }
     *
     * @see MarshalContext#finish();
     */
    final MarshalContext begin() {
        return new MarshalContext(converter, resolver, gmlVersion, schemas, locale, timezone, bitMasks);
    }
}
