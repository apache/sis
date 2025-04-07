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
import java.util.function.UnaryOperator;
import java.util.logging.Filter;
import java.time.ZoneId;
import java.time.DateTimeException;
import javax.xml.validation.Schema;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.PropertyException;
import jakarta.xml.bind.ValidationEventHandler;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.apache.sis.util.Locales;
import org.apache.sis.util.Version;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.privy.Strings;
import org.apache.sis.xml.bind.Context;
import org.apache.sis.xml.bind.TypeRegistration;
import org.apache.sis.xml.privy.LegacyNamespaces;
import org.apache.sis.xml.privy.ExternalLinkHandler;


/**
 * Base class of {@link PooledMarshaller} and {@link PooledUnmarshaller}.
 * This class provides basic service for saving the initial values of (un)marshaller properties,
 * in order to reset them to their initial values after usage.
 * This is required in order to allow (un)marshaller reuse.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 */
abstract class Pooled {
    /**
     * The keys of entries that can be stored in the {@link #schemas} map.
     * Those keys are documented in {@link XML#SCHEMAS}.
     */
    private static final String[] SCHEMA_KEYS = {"cat", "gmd", "gmi", "gml"};

    /**
     * The pool that produced this marshaller or unmarshaller.
     */
    private final MarshallerPool pool;

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
     *
     * This map is never {@code null}.
     */
    final Map<Object,Object> initialProperties = new LinkedHashMap<>();

    /**
     * Bit masks for various boolean attributes. This include whatever the language codes
     * or the country codes should be substituted by a simpler character string elements.
     * Those bits are determined by the {@link XML#STRING_SUBSTITUTES} property.
     * The meaning of the bits are defined by constants in {@link Context} class.
     */
    private int bitMasks;

    /**
     * An optional locale for {@link org.opengis.util.InternationalString} and
     * {@link org.opengis.util.CodeList}. Can be set by the {@link XML#LOCALE} property.
     */
    private Locale locale;

    /**
     * The timezone, or {@code null} if unspecified.
     * Can be set by the {@link XML#TIMEZONE} property.
     */
    private ZoneId timezone;

    /**
     * The base URL of ISO 19115-3 (or other standards) schemas. It shall be an unmodifiable
     * instance because {@link #getProperty(String)} returns a direct reference to the user.
     * The valid values are documented in the {@link XML#SCHEMAS} property.
     */
    private Map<String,String> schemas;

    /**
     * The GML version to be marshalled or unmarshalled, or {@code null} if unspecified.
     * If null, then the latest version is assumed.
     *
     * @see Context#getVersion(String)
     */
    private Version versionGML;

    /**
     * The metadata version to be marshalled or unmarshalled, or {@code null} if unspecified.
     * If null, then the latest version is assumed.
     *
     * @see Context#getVersion(String)
     */
    private Version versionMetadata;

    /**
     * If the document to (un)marshal is included inside a larger document,
     * the {@code systemId} of the included document. Otherwise {@code null}.
     * This is used for caching the map of fragments (identified by {@code gml:id} attributes)
     * included inside a document referenced by an {@code xlink:href} attribute.
     *
     * @see Context#getObjectForID(Context, Object, String)
     */
    private Object includedDocumentSystemId;

    /**
     * The reference resolver to use during unmarshalling.
     * Can be set by the {@link XML#RESOLVER} property.
     */
    private ReferenceResolver resolver;

    /**
     * The object converters to use during (un)marshalling.
     * Can be set by the {@link XML#CONVERTER} property.
     */
    private ValueConverter converter;

    /**
     * Converters from arbitrary classes implementing GeoAPI interfaces to Apache SIS implementations
     * providing JAXB annotations, or null or an empty array if none. This is used at marshalling time.
     *
     * @see #getRootAdapters()
     */
    private UnaryOperator<Object>[] rootAdapters;

    /**
     * The object to inform about warnings, or {@code null} if none.
     */
    private Filter logFilter;

    /**
     * The {@link System#nanoTime()} value of the last call to {@link #reset(Pooled)}.
     * This is used for disposing (un)marshallers that have not been used for a while,
     * since {@code reset()} is invoked just before to push a (un)marshaller in the pool.
     */
    volatile long resetTime;

    /**
     * Creates a {@link PooledTemplate}.
     *
     * @param pool  the pool that produced this template.
     */
    Pooled(final MarshallerPool pool) {
        this.pool = pool;
    }

    /**
     * Creates a {@link PooledMarshaller} or {@link PooledUnmarshaller}. The {@link #initialize(Pooled)}
     * method must be invoked after this constructor for completing the initialization.
     *
     * @param template the {@link PooledTemplate} from which to get the initial values.
     */
    Pooled(final Pooled template) {
        pool = template.pool;
    }

    /**
     * Completes the creation of a {@link PooledMarshaller} or {@link PooledUnmarshaller}.
     * This method is not invoked in the {@link #Pooled(Pooled)} constructor in order to
     * give to subclasses a chance to complete their construction first.
     *
     * @param  template  the {@link PooledTemplate} from which to get the initial values.
     * @throws JAXBException if an error occurred while setting a property.
     */
    final void initialize(final Pooled template) throws JAXBException {
        reset(template);     // Set the SIS properties first. JAXB properties are set below.
        for (final Map.Entry<Object,Object> entry : template.initialProperties.entrySet()) {
            setStandardProperty((String) entry.getKey(), entry.getValue());
        }
    }

    /**
     * Releases resources and resets the (un)marshaller to its initial state.
     * This method is invoked by {@link MarshallerPool} just before to push a
     * (un)marshaller in the pool after its usage.
     *
     * @param  template  the {@link PooledTemplate} from which to get the initial values.
     * @throws JAXBException if an error occurred while restoring a property.
     */
    public final void reset(final Pooled template) throws JAXBException {
        for (final Map.Entry<Object,Object> entry : initialProperties.entrySet()) {
            reset(entry.getKey(), entry.getValue());
        }
        initialProperties.clear();
        includedDocumentSystemId = null;
        bitMasks         = template.bitMasks;
        locale           = template.locale;
        timezone         = template.timezone;
        schemas          = template.schemas;
        versionGML       = template.versionGML;
        versionMetadata  = template.versionMetadata;
        resolver         = template.resolver;
        converter        = template.converter;
        rootAdapters     = template.rootAdapters;
        logFilter        = template.logFilter;
        resetTime        = System.nanoTime();
        if (this instanceof Marshaller) {
            bitMasks |= Context.MARSHALLING;
        }
    }

    /**
     * Resets the given marshaller property to its initial state. This method is invoked automatically
     * by the {@link #reset(Pooled)} method. The key is either a {@link String} or a {@link Class}.
     * If this is a string, then the value shall be given to the {@code setProperty(key, value)} method.
     * Otherwise the value shall be given to {@code setFoo(value)} method
     * where {@code "Foo"} is determined from the key.
     *
     * @param  key    the property to reset.
     * @param  value  the initial value to give to the property.
     * @throws JAXBException if an error occurred while restoring a property.
     */
    protected abstract void reset(final Object key, final Object value) throws JAXBException;

    /**
     * Returns the {@code TransformVersion} enumeration value to use for the current GML or metadata version,
     * or {@code null} if the SIS native versions are suitable. If this method returns a non-null value, then
     * the output generated by JAXB will need to go through a {@link TransformingWriter} in order to replace
     * the namespace of versions implemented by SIS by the namespace of versions requested by the user.
     *
     * @see Transformer
     */
    final TransformVersion getTransformVersion() {
        /*
         * If no version is specified and unmarshalling is lenient, then the default behavior will be:
         *   - enable namespace replacement on unmarshalling (in order to accept all versions)
         *   - disable namespace replacement on marshalling (in order to use latest version).
         */
        final boolean byDefault = (bitMasks & (Context.MARSHALLING | Context.LENIENT_UNMARSHAL)) == Context.LENIENT_UNMARSHAL;
        /*
         * Bitwise combination of legacy schemas to support:
         *   1: namespace replacement needed for GML
         *   2: namespace replacement needed for metadata.
         */
        int combine = (specificBitMasks() & Context.LEGACY_METADATA) != 0 ? 2 : 0;
        if (versionGML      == null ? byDefault : versionGML     .compareTo(LegacyNamespaces.VERSION_3_2_1) < 0) combine  = 1;
        if (versionMetadata == null ? byDefault : versionMetadata.compareTo(LegacyNamespaces.VERSION_2014)  < 0) combine |= 2;
        switch (combine) {
            case 0:  return null;
            case 1:  return TransformVersion.GML31;
            case 2:  return TransformVersion.ISO19139;
            case 3:  return TransformVersion.ALL;
            default: throw new AssertionError(combine);     // Should never happen.
        }
    }

    /**
     * Returns {@code true} if the initial property is already saved for the given key.
     * Note that a property set to {@code null} is still considered as defined.
     */
    final boolean isPropertySaved(final Class<?> key) {
        return initialProperties.containsKey(key);
    }

    /**
     * Saves the current value of a property. This method is invoked before a value is
     * modified for the first time, in order to allow {@link #reset(Pooled)} to restore
     * the (un)marshaller to its initial state.
     *
     * @param  type   the property to save.
     * @param  value  the current value of the property.
     */
    final <E> void saveProperty(final Class<E> type, final E value) {
        if (initialProperties.put(type, value) != null) {
            // Should never happen, unless on concurrent changes in a backgroung thread.
            throw new ConcurrentModificationException(Errors.format(Errors.Keys.UnexpectedChange_1,
                    type.getInterfaces()[0].getSimpleName() + ".get" + type.getSimpleName()));
        }
    }

    /**
     * A method which is common to both {@code Marshaller} and {@code Unmarshaller}.
     * It saves the initial state if it was not already done, but subclasses will
     * need to complete the work.
     */
    public final void setProperty(String name, final Object value) throws PropertyException {
        try {
            switch (name) {
                case XML.LOCALE: {
                    locale = (value instanceof CharSequence) ? Locales.parse(value.toString()) : (Locale) value;
                    return;
                }
                case XML.TIMEZONE: {
                    if (value instanceof CharSequence) {
                        String id = value.toString();
                        try {
                            timezone = ZoneId.of(id);
                        } catch (DateTimeException e) {
                            timezone = TimeZone.getTimeZone(id).toZoneId();
                            if (timezone.getId().equals("GMT")) {
                                throw e;
                            }
                        }
                    } else {
                        timezone = (value instanceof TimeZone) ? ((TimeZone) value).toZoneId() : (ZoneId) value;
                    }
                    return;
                }
                case XML.SCHEMAS: {
                    final Map<?,?> map = (Map<?,?>) value;
                    Map<String,String> copy = null;
                    if (map != null) {
                        copy = new HashMap<>(4);
                        for (final String key : SCHEMA_KEYS) {
                            final Object schema = map.get(key);
                            if (schema != null) {
                                if (!(schema instanceof String)) {
                                    throw new PropertyException(Errors.format(Errors.Keys.IllegalPropertyValueClass_2,
                                            Strings.bracket(name, key), schema.getClass()));
                                }
                                copy.put(key, (String) schema);
                            }
                        }
                        copy = Map.copyOf(copy);
                    }
                    schemas = copy;
                    return;
                }
                case XML.GML_VERSION: {
                    versionGML = (value instanceof CharSequence) ? new Version(value.toString()) : (Version) value;
                    return;
                }
                case XML.METADATA_VERSION: {
                    versionMetadata = (value instanceof CharSequence) ? new Version(value.toString()) : (Version) value;
                    return;
                }
                case XML.RESOLVER: {
                    resolver = (ReferenceResolver) value;
                    return;
                }
                case XML.CONVERTER: {
                    converter = (ValueConverter) value;
                    return;
                }
                case XML.LENIENT_UNMARSHAL: {
                    if (value != null && ((value instanceof CharSequence) ?
                            Boolean.parseBoolean(value.toString()) : (Boolean) value))
                    {
                        bitMasks |= Context.LENIENT_UNMARSHAL;
                    } else {
                        bitMasks &= ~Context.LENIENT_UNMARSHAL;
                    }
                    return;
                }
                case XML.STRING_SUBSTITUTES: {
                    bitMasks &= ~(Context.SUBSTITUTE_LANGUAGE |
                                  Context.SUBSTITUTE_COUNTRY  |
                                  Context.SUBSTITUTE_FILENAME |
                                  Context.SUBSTITUTE_MIMETYPE);
                    if (value != null) {
                        for (final CharSequence substitute : (CharSequence[]) value) {
                            if (CharSequences.equalsIgnoreCase(substitute, "language")) {
                                bitMasks |= Context.SUBSTITUTE_LANGUAGE;
                            } else if (CharSequences.equalsIgnoreCase(substitute, "country")) {
                                bitMasks |= Context.SUBSTITUTE_COUNTRY;
                            } else if (CharSequences.equalsIgnoreCase(substitute, "filename")) {
                                bitMasks |= Context.SUBSTITUTE_FILENAME;
                            } else if (CharSequences.equalsIgnoreCase(substitute, "mimetype")) {
                                bitMasks |= Context.SUBSTITUTE_MIMETYPE;
                            }
                        }
                    }
                    return;
                }
                case XML.WARNING_FILTER: {
                    logFilter = (Filter) value;
                    return;
                }
                case TypeRegistration.ROOT_ADAPTERS: {
                    @SuppressWarnings("unchecked")
                    UnaryOperator<Object>[] c = (UnaryOperator<Object>[]) value;
                    rootAdapters = c;       // No clone for now because ROOT_ADAPTERS is not yet a public API.
                    return;
                }
            }
        } catch (RuntimeException e) {
            throw new PropertyException(Errors.format(
                    Errors.Keys.IllegalPropertyValueClass_2, name, value.getClass()), e);
        }
        /*
         * If we reach this point, the given name is not a SIS property. Try to handle
         * it as a (un)marshaller-specific property, after saving the previous value.
         */
        if (!initialProperties.containsKey(name) && initialProperties.put(name, getStandardProperty(name)) != null) {
            // Should never happen, unless on concurrent changes in a backgroung thread.
            throw new ConcurrentModificationException(name);
        }
        setStandardProperty(name, value);
    }

    /**
     * A method which is common to both {@code Marshaller} and {@code Unmarshaller}.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")     // Because unmodifiable.
    public final Object getProperty(String name) throws PropertyException {
        switch (name) {
            case XML.LOCALE:            return locale;
            case XML.TIMEZONE:          return timezone;
            case XML.SCHEMAS:           return schemas;
            case XML.GML_VERSION:       return versionGML;
            case XML.METADATA_VERSION:  return versionMetadata;
            case XML.RESOLVER:          return resolver;
            case XML.CONVERTER:         return converter;
            case XML.WARNING_FILTER:    return logFilter;
            case XML.LENIENT_UNMARSHAL: return (bitMasks & Context.LENIENT_UNMARSHAL) != 0;
            case XML.STRING_SUBSTITUTES: {
                int n = 0;
                final String[] substitutes = new String[4];
                if ((bitMasks & Context.SUBSTITUTE_LANGUAGE) != 0) substitutes[n++] = "language";
                if ((bitMasks & Context.SUBSTITUTE_COUNTRY)  != 0) substitutes[n++] = "country";
                if ((bitMasks & Context.SUBSTITUTE_FILENAME) != 0) substitutes[n++] = "filename";
                if ((bitMasks & Context.SUBSTITUTE_MIMETYPE) != 0) substitutes[n++] = "mimetype";
                return (n != 0) ? ArraysExt.resize(substitutes, n) : null;
            }
            case TypeRegistration.ROOT_ADAPTERS: return (rootAdapters != null) ? rootAdapters.clone() : null;
            default: return getStandardProperty(name);
        }
    }

    /**
     * Sets the given property to the wrapped (un)marshaller. This method is invoked
     * automatically when the property given to the {@link #setProperty(String, Object)}
     * method was not one of the {@link XML} constants.
     */
    abstract void setStandardProperty(String name, Object value) throws PropertyException;

    /**
     * Gets the given property from the wrapped (un)marshaller. This method is invoked
     * automatically when the property key given to the {@link #getProperty(String)}
     * method was not one of the {@link XML} constants.
     */
    abstract Object getStandardProperty(String name) throws PropertyException;

    /**
     * Delegates to {@code setAdapter(adapter.getClass(), adapter)} as specified
     * in {@code [Un]Marshaller} javadoc.
     */
    @SuppressWarnings("unchecked")
    public <A extends XmlAdapter<?,?>> void setAdapter(final A adapter) {
        setAdapter((Class<A>) adapter.getClass(), adapter);
    }

    /**
     * A method which is common to both {@code Marshaller} and {@code Unmarshaller}.
     * It saves the initial state if it was not already done, but subclasses will
     * need to complete the work.
     */
    public <A extends XmlAdapter<?,?>> void setAdapter(final Class<A> type, final A adapter) {
        if (!isPropertySaved(type)) {
            saveProperty(type, getAdapter(type));
        }
    }

    /**
     * A method which is common to both {@code Marshaller} and {@code Unmarshaller}.
     */
    public abstract <A extends XmlAdapter<?,?>> A getAdapter(final Class<A> type);

    /**
     * Returns the adapters to apply on the root object to marshal, or {@code null} or an empty array if none.
     * This is used for converting from arbitrary implementations of GeoAPI interfaces to Apache SIS implementations
     * providing JAXB annotations.
     *
     * @return a direct reference to the internal array of converters - do not modify.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final UnaryOperator<Object>[] getRootAdapters() {
        return rootAdapters;
    }

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
     * Returns bit masks specific to the object being marshalled. This mask will be combined with the
     * bit masks managed by the {@link Pooled} base class. This is used mostly for mandating legacy
     * metadata format (ISO 19139:2007) for some object to marshal.
     *
     * <p>This is a hopefully temporary hack for marshalling metadata in GML.
     * May be deleted if we implement SIS-401.</p>
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-401">SIS-401</a>
     */
    int specificBitMasks() {
        return 0;
    }

    /**
     * Notifies this object that it will be used for marshalling or unmarshalling a document
     * included in a larger document. It happens when following {@code xlink:href}.
     *
     * @param  systemId  key to use for caching the parsing result in the marshal {@link Context}.
     */
    final void forIncludedDocument(final Object systemId) {
        includedDocumentSystemId = systemId;
    }

    /**
     * Must be invoked by subclasses before a {@code try} block performing a (un)marshalling operation.
     * Must be followed by a call to {@code finish()} in a {@code finally} block.
     *
     * {@snippet lang="java" :
     *     Context context = begin(linkHandler);
     *     try {
     *         ...
     *     } finally {
     *         context.finish();
     *     }
     *     }
     *
     * @see Context#finish()
     *
     * @param  linkHandler  the document-dependent resolver or relativizer of URIs, or {@code null}.
     */
    final Context begin(final ExternalLinkHandler linkHandler) {
        if (includedDocumentSystemId != null) {
            final Context current = Context.current();
            if (current != null) {
                return current.createChild(includedDocumentSystemId, linkHandler);
            }
        }
        return new Context(bitMasks | specificBitMasks(), pool, locale, timezone,
                           schemas, versionGML, versionMetadata,
                           linkHandler, resolver, converter, logFilter);
    }

    /**
     * {@return a string representation of this (un)marshaller for debugging purposes}.
     */
    @Override
    public String toString() {
        final Context current = Context.current();
        return Strings.toString(getClass(),
                "baseURI", Context.linkHandler(current).getBase(),
                "locale", locale, "timezone", timezone,
                "versionGML", versionGML, "versionMetadata", versionMetadata);
    }
}
