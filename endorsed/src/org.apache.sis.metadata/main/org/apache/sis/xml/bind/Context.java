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
package org.apache.sis.xml.bind;

import java.net.URI;
import java.time.ZoneId;
import java.util.Map;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Collections;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.logging.Filter;
import org.apache.sis.util.Version;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.CorruptedObjectException;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.util.resources.IndexedResourceBundle;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.xml.MarshalContext;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.xml.ValueConverter;
import org.apache.sis.xml.ReferenceResolver;
import org.apache.sis.xml.bind.gco.PropertyType;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.xml.internal.shared.ExternalLinkHandler;
import org.apache.sis.system.Semaphores;
import org.apache.sis.system.Loggers;


/**
 * Thread-local status of a marshalling or unmarshalling processes.
 * All non-static methods in this class except {@link #finish()} are implementation of public API.
 * All static methods are internal API. Those methods expect a {@code Context} instance as their first argument.
 * They can be though as if they were normal member methods, except that they accept {@code null} instance
 * if no (un)marshalling is in progress.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 */
public final class Context extends MarshalContext {
    /**
     * The bit flag telling if a marshalling process is under progress.
     * This flag is unset for unmarshalling processes.
     */
    public static final int MARSHALLING = 0x1;

    /**
     * The bit flag for enabling substitution of language codes by character strings.
     *
     * @see org.apache.sis.xml.XML#STRING_SUBSTITUTES
     */
    public static final int SUBSTITUTE_LANGUAGE = 0x2;

    /**
     * The bit flag for enabling substitution of country codes by character strings.
     *
     * @see org.apache.sis.xml.XML#STRING_SUBSTITUTES
     */
    public static final int SUBSTITUTE_COUNTRY = 0x4;

    /**
     * The bit flag for enabling substitution of filenames by character strings.
     *
     * @see org.apache.sis.xml.XML#STRING_SUBSTITUTES
     */
    public static final int SUBSTITUTE_FILENAME = 0x8;

    /**
     * The bit flag for enabling substitution of mime types by character strings.
     *
     * @see org.apache.sis.xml.XML#STRING_SUBSTITUTES
     */
    public static final int SUBSTITUTE_MIMETYPE = 0x10;

    /**
     * Whether we are (un)marshalling legacy metadata as defined in 2003 model (ISO 19139:2007).
     * If this flag is not set, then we assume latest metadata as defined in 2014 model (ISO 19115-3).
     */
    public static final int LEGACY_METADATA = 0x20;

    /**
     * Whether the unmarshalling process should accept any metadata or GML version if the user did not
     * specified an explicit version. Accepting any version may have surprising results since namespace
     * substitutions may be performed on the fly.
     */
    public static final int LENIENT_UNMARSHAL = 0x40;

    /**
     * Bit where to store whether {@link #finish()} shall invoke {@code Semaphores.clear(Semaphores.NULL_COLLECTION)}.
     */
    private static final int CLEAR_SEMAPHORE = 0x80;

    /**
     * The thread-local context. Elements are created in the constructor, and removed in a
     * {@code finally} block by the {@link #finish()} method. This {@code ThreadLocal} shall
     * not contain any value when no (un)marshalling is in progress.
     */
    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();

    /**
     * A sentinel value meaning that unmarshalling of a document was already attempted before and failed.
     * This is used for documents referenced from a larger document using {@code xlink:href}.
     *
     * @see #getExternalObjectForID(Object, String)
     * @see #cacheDocument(Object, Object)
     */
    public static final Object INVALID_OBJECT = Void.TYPE;

    /**
     * The logger to use for warnings that are specific to XML.
     *
     * @see #warningOccured(Context, Level, Class, String, Throwable, Class, short, Object...)
     */
    public static final Logger LOGGER = Logger.getLogger(Loggers.XML);

    /**
     * The pool that produced the marshaller or unmarshaller.
     */
    private final MarshallerPool pool;

    /**
     * Various boolean attributes determines by the above static constants.
     */
    final int bitMasks;

    /**
     * The locale to use for marshalling, or {@code null} if no locale was explicitly specified.
     */
    private final Locale locale;

    /**
     * The timezone, or {@code null} if unspecified.
     * In the latter case, an implementation-default (typically UTC) timezone is used.
     */
    private final ZoneId timezone;

    /**
     * The base URL of ISO 19115-3 (or other standards) schemas.
     * The valid values are documented in the {@link org.apache.sis.xml.XML#SCHEMAS} property.
     */
    private final Map<String,String> schemas;

    /**
     * The GML version to be marshalled or unmarshalled, or {@code null} if unspecified.
     * If null, than the latest version is assumed.
     */
    private final Version versionGML;

    /**
     * The {@code XLink} reference resolver for converting relative URL to absolute URL.
     * Contrarily to {@link #resolver}, this instance depends on the document being read.
     * If {@code null}, then {@link ExternalLinkHandler#DEFAULT} is assumed.
     */
    private final ExternalLinkHandler linkHandler;

    /**
     * The reference resolver currently in use, or {@code null} for {@link ReferenceResolver#DEFAULT}.
     * This class does not necessarily parses XML document. It may returns objects from a database.
     * The same instance may be used for many documents to parse.
     */
    private final ReferenceResolver resolver;

    /**
     * The value converter currently in use, or {@code null} for {@link ValueConverter#DEFAULT}.
     */
    private final ValueConverter converter;

    /**
     * The objects associated to XML identifiers in the current document.
     * Map keys are {@code gml:id} attribute values, and map values are the identified objects.
     * At marhalling time, this map is used for avoiding duplicated identifiers in the same XML document.
     * At unmarshalling time, this is used for getting a previously unmarshalled object from its identifier.
     *
     * <p>By convention, the {@code null} key is associated to the whole document. This convention is used if
     * the document being unmarshalled is part of a larger document and was referenced by {@code xlink:href}.
     * In such case, this map is also a value of the {@link #documentToXmlids} map.</p>
     *
     * @see #getObjectForID(Context, String)
     */
    private final Map<String,Object> xmlidToObject;

    /**
     * The identifiers used for marshalled objects in the current document.
     * This is the converse of the {@link #xmlidToObject} map, used in order to identify which {@code gml:id} to use
     * for a given object. The {@code gml:id} values to use are not necessarily the same as the values associated
     * to {@link IdentifierSpace#ID} if some identifiers were already used for other objects in the same XML document.
     */
    private final Map<Object,String> objectToXmlid;

    /**
     * The {@link #xmlidToObject} map for each document being unmarshalled.
     * This cache is populated if the document contains {@code xlink:href} to other documents.
     * This cache contains only the documents seen by following the references since the root document.
     * This is not a system-wide cache.
     *
     * <p>Keys are {@code systemId} as instances of {@link URI} or {@link String}. For documents that are read
     * from a file or an URL, {@code systemId} should be the value of {@link java.net.URI#toASCIIString()} for
     * consistency with {@link javax.xml.transform.stream.StreamSource}.  However, URI instances are preferred
     * in this map because the {@link URI#equals(Object)} method applies some rules regarding case-sensitivity
     * that {@link String#equals(Object)} cannot know.</p>
     *
     * <p>Values of this map are the {@link #xmlidToObject} maps of the corresponding document.
     * See {@link #xmlidToObject} for a description of the meaning of those maps.</p>
     */
    private final Map<Object, Map<String,Object>> documentToXmlids;

    /**
     * All identified objects associated to a global identifier (not {@code gml:id}).
     * This map differs from {@link #xmlidToObject} in that it is not local to the current document,
     * but instead contains all identified objects in all documents parsed since the root document.
     * The keys of this map cannot be {@code gml:id} attribute values, because the latter are local.
     * Instead, keys are typically {@link org.opengis.referencing.IdentifiedObject#getIdentifiers()}.
     * The map may contain many entries for the same object if that object has many identifiers.
     */
    final Map<ScopedIdentifier<?>, Object> identifiedObjects;

    /**
     * The object to inform about warnings, or {@code null} if none.
     *
     * @see org.apache.sis.xml.XML#WARNING_FILTER
     */
    private final Filter logFilter;

    /**
     * The {@code <gml:*PropertyType>} which is wrapping the {@code <gml:*Type>} object to (un)marshal, or
     * {@code null} if this information is not provided. See {@link #getWrapper(Context)} for an example.
     *
     * <p>For performance reasons, this {@code wrapper} information is not provided by default.
     * See {@link #setWrapper(Context, PropertyType)} for more information.</p>
     *
     * @see #getWrapper(Context)
     * @see #setWrapper(Context, PropertyType)
     */
    private PropertyType<?,?> wrapper;

    /**
     * The context which was previously used. This form a linked list allowing to push properties
     * and pull back the context to its previous state once finished.
     */
    private final Context previous;

    /**
     * Invoked when a marshalling or unmarshalling process is about to begin.
     * Must be followed by a call to {@link #finish()} in a {@code finally} block.
     *
     * {@snippet lang="java" :
     *     Context context = new Context(…);
     *     try {
     *         ...
     *     } finally {
     *         context.finish();
     *     }
     *     }
     *
     * @param  bitMasks         a combination of {@link #MARSHALLING}, {@code SUBSTITUTE_*} or other bit masks.
     * @param  pool             the pool that produced the marshaller or unmarshaller.
     * @param  locale           the locale, or {@code null} if unspecified.
     * @param  timezone         the timezone, or {@code null} if unspecified.
     * @param  schemas          the schemas root URL, or {@code null} if none.
     * @param  versionGML       the GML version, or {@code null}.
     * @param  versionMetadata  the metadata version, or {@code null}.
     * @param  linkHandler      the document-dependent resolver of relative URIs, or {@code null}.
     * @param  resolver         the resolver in use.
     * @param  converter        the converter in use.
     * @param  logFilter        the object to inform about warnings.
     */
    public Context(int                       bitMasks,
                   final MarshallerPool      pool,
                   final Locale              locale,
                   final ZoneId              timezone,
                   final Map<String,String>  schemas,
                   final Version             versionGML,
                   final Version             versionMetadata,
                   final ExternalLinkHandler linkHandler,
                   final ReferenceResolver   resolver,
                   final ValueConverter      converter,
                   final Filter              logFilter)
    {
        if (versionMetadata != null && versionMetadata.compareTo(LegacyNamespaces.VERSION_2014) < 0) {
            bitMasks |= LEGACY_METADATA;
        }
        this.pool         = pool;
        this.locale       = locale;
        this.timezone     = timezone;
        this.schemas      = schemas;            // No clone, because this class is internal.
        this.versionGML   = versionGML;
        this.linkHandler  = linkHandler;
        this.resolver     = resolver;
        this.converter    = converter;
        this.logFilter    = logFilter;
        xmlidToObject     = new HashMap<>();
        objectToXmlid     = new IdentityHashMap<>();
        documentToXmlids  = new HashMap<>();
        identifiedObjects = new HashMap<>();
        previous          = CURRENT.get();
        if ((bitMasks & MARSHALLING) != 0) {
            /*
             * Set global semaphore last after our best effort to ensure that construction
             * will not fail with an OutOfMemoryError. This is preferable for allowing the
             * caller to invoke finish() in a finally block.
             */
            if (!Semaphores.queryAndSet(Semaphores.NULL_COLLECTION)) {
                bitMasks |= CLEAR_SEMAPHORE;
            }
        }
        this.bitMasks = bitMasks;
        CURRENT.set(this);
    }

    /**
     * Creates a new context with different properties than the current context.
     * The old context shall be restored by a call to {@link #pull()} in a {@code finally} block.
     *
     * @param parent       the context from which to inherit.
     * @param locale       the locale in the new context.
     * @param linkHandler  the document-dependent resolver of relative URIs, or {@code null}.
     * @param inline       {@code true} if the context is for reading the same document, or
     *                     {@code false} for reading a separated document.
     */
    private Context(final Context parent, final Locale locale, final ExternalLinkHandler linkHandler,
                    final boolean inline)
    {
        assert parent == CURRENT.get();
        this.previous    = parent;
        this.locale      = locale;
        this.linkHandler = linkHandler;
        this.pool        = parent.pool;
        this.timezone    = parent.timezone;
        this.schemas     = parent.schemas;
        this.versionGML  = parent.versionGML;
        this.resolver    = parent.resolver;
        this.converter   = parent.converter;
        this.logFilter   = parent.logFilter;
        this.bitMasks    = parent.bitMasks & ~CLEAR_SEMAPHORE;
        if (inline) {
            xmlidToObject = parent.xmlidToObject;
            objectToXmlid = parent.objectToXmlid;
        } else {
            xmlidToObject = new HashMap<>();
            objectToXmlid = new IdentityHashMap<>();
        }
        documentToXmlids  = parent.documentToXmlids;
        identifiedObjects = parent.identifiedObjects;
    }

    /**
     * Returns the marshaller pool that produced the marshaller or unmarshaller in use.
     *
     * @return the pool in the context of current (un)marshalling process.
     */
    @Override
    public final MarshallerPool getPool() {
        return pool;
    }

    /**
     * Returns the locale to use for marshalling, or {@code null} if no locale was explicitly specified.
     *
     * @return the locale in the context of current (un)marshalling process.
     */
    @Override
    public final Locale getLocale() {
        return locale;
    }

    /**
     * Returns the timezone to use for marshalling, or {@code null} if none were explicitly specified.
     *
     * @return the timezone in the context of current (un)marshalling process.
     */
    @Override
    public final ZoneId getZoneId() {
        return timezone;
    }

    /**
     * Returns the schema version of the XML document being (un)marshalled.
     * See the super-class javadoc for the list of prefix that we shall support.
     *
     * @return the version in the context of current (un)marshalling process.
     */
    @Override
    public final Version getVersion(final String prefix) {
        switch (prefix) {
            case "gml": return versionGML;
            case "gmd": {
                if ((bitMasks & MARSHALLING) == 0) break;   // If unmarshalling, we don't know the version.
                return (bitMasks & LEGACY_METADATA) == 0 ? LegacyNamespaces.VERSION_2016 : LegacyNamespaces.VERSION_2007;
            }
            // Future SIS versions may add more cases here.
        }
        return null;
    }

    /**
     * Returns the URI of the document being (un)marshalled, if this URI is known.
     */
    @Override
    public final Optional<URI> getDocumentURI() {
        return (linkHandler != null) ? Optional.ofNullable(linkHandler.getURI()) : Optional.empty();
    }




    /*
     ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
     ┃                                                                        ┃
     ┃    END OF PUBLIC (non-internal) API.                                   ┃
     ┃                                                                        ┃
     ┃    Following are internal API. They are provided as static methods     ┃
     ┃    with a Context argument rather than normal member methods           ┃
     ┃    in order to accept null context.                                    ┃
     ┃                                                                        ┃
     ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
     */

    /**
     * Returns the context of the XML (un)marshalling currently progressing in the current thread,
     * or {@code null} if none.
     *
     * @return the current (un)marshalling context, or {@code null} if none.
     */
    public static Context current() {
        return CURRENT.get();
    }

    /**
     * Creates the context for following a {@code xlink:href} to a separated document.
     * Because the separated document may be in a different directory, the new context
     * will use a different {@link ExternalLinkHandler}. The returned context shall be
     * used in the same way as a context created from the public constructor.
     *
     * <p>The {@code systemId} argument is the key to use for caching the result of (un)marshalling a document
     * in the new context. This is usually the same value as the {@link String}, {@link File} or {@link URL}
     * specified at {@code linkHandler} construction time, but as an {@link URI} or {@link String} instance.</p>
     *
     * @param  systemId     {@link URI} or {@link String} identifier of the document to (un)marshal.
     * @param  linkHandler  the document-dependent resolver of relative URIs, or {@code null}.
     * @return the context as a child of current context, never {@code null}.
     *
     * @see Context#getExternalObjectForID(Object, String)
     */
    public final Context createChild(final Object systemId, final ExternalLinkHandler linkHandler) {
        // Use Map.put(…) instead of putIfAbsent(…) because maybe the previous map is unmodifiable.
        final Context context = new Context(this, locale, linkHandler, false);
        documentToXmlids.put(systemId, context.xmlidToObject);
        CURRENT.set(context);
        return context;
    }

    /**
     * Sets the locale to the given value. The old locales are remembered and will
     * be restored by the next call to {@link #pull()}. This method can be invoked
     * when marshalling object that need to marshal their children in a different
     * locale, like below:
     *
     * {@snippet lang="java" :
     *     private void beforeMarshal(Marshaller marshaller) {
     *         Context.push(language);
     *     }
     *
     *     private void afterMarshal(Marshaller marshaller) {
     *         Context.pull();
     *     }
     * }
     *
     * @param  locale  the locale to set, or {@code null}.
     */
    public static void push(Locale locale) {
        final Context current = current();
        if (current != null) {
            if (locale == null) {
                locale = current.locale;
            }
            CURRENT.set(new Context(current, locale, current.linkHandler, true));
        }
    }

    /**
     * Restores the locale which was used prior the call to {@code push(…)}.
     * It is not necessary to invoke this method in a {@code finally} block,
     * provided that {@link #finish()} is invoked in a finally block by some
     * enclosing method calls.
     */
    public static void pull() {
        Context c = current();
        if (c != null) {
            c = c.previous;
            if (c != null) {
                CURRENT.set(c);
            } else {
                CURRENT.remove();       // For more robustness, but should not happen.
            }
        }
    }

    /**
     * Returns {@code true} if the given flag is set.
     *
     * @param  context  the current context, or {@code null} if none.
     * @param  flag     one of {@link #MARSHALLING}, {@link #SUBSTITUTE_LANGUAGE}, {@link #SUBSTITUTE_COUNTRY}
     *                  or other bit masks.
     * @return {@code true} if the given flag is set.
     */
    public static boolean isFlagSet(final Context context, final int flag) {
        return (context != null) && (context.bitMasks & flag) != 0;
    }

    /**
     * Returns {@code true} if the GML version is equal or newer than the specified version.
     * If no GML version was specified, then this method returns {@code true}, i.e. newest
     * version is assumed.
     *
     * <div class="note"><b>API note:</b>
     * This method is static for the convenience of performing the check for null context.</div>
     *
     * @param  context  the current context, or {@code null} if none.
     * @param  version  the version to compare to.
     * @return {@code true} if the GML version is equal or newer than the specified version.
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
     * Returns the base URL of ISO 19115-3 (or other standards) schemas.
     * The valid values are documented in the {@link org.apache.sis.xml.XML#SCHEMAS} property.
     * If the returned value is not empty, then this method guarantees it ends with {@code '/'}.
     *
     * <div class="note"><b>API note:</b>
     * This method is static for the convenience of performing the check for null context.</div>
     *
     * @param  context        the current context, or {@code null} if none.
     * @param  key            one of the value documented in the <q>Map key</q> column of
     *                        {@link org.apache.sis.xml.XML#SCHEMAS}.
     * @param  defaultSchema  the value to return if no schema is found for the given key.
     * @return the base URL of the schema, or an empty buffer if none were specified.
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
     * Returns the {@code <gml:*PropertyType>} which is wrapping the {@code <gml:*Type>} object to (un)marshal,
     * or {@code null} if this information is not provided. The {@code <gml:*PropertyType>} element can contains
     * information not found in {@code <gml:*Type>} objects like XLink or UUID.
     *
     * <h4>Example</h4>
     * before unmarshalling the {@code <gml:OperationParameter>} (upper case {@code O}) element below,
     * {@code wrapper} will be set to the temporary object representing {@code <gml:operationParameter>}.
     * That adapter provides important information for the SIS {@code <gml:OperationParameter>} constructor.
     *
     * {@snippet lang="xml" :
     *   <gml:ParameterValue>
     *     <gml:valueFile>http://www.opengis.org</gml:valueFile>
     *     <gml:operationParameter>
     *       <gml:OperationParameter>
     *         <gml:name>A parameter of type URI</gml:name>
     *       </gml:OperationParameter>
     *     </gml:operationParameter>
     *   </gml:ParameterValue>
     *   }
     *
     * <h4>Design note</h4>
     * For performance reasons, this {@code wrapper} information is not provided by default.
     * See {@link #setWrapper(Context, PropertyType)} for more information.
     *
     * @param  context  the current context, or {@code null} if none.
     * @return the {@code <gml:*PropertyType>} which is wrapping the {@code <gml:*Type>} object to (un)marshal,
     *         or {@code null} if unknown.
     */
    public static PropertyType<?,?> getWrapper(final Context context) {
        return (context != null) ? context.wrapper : null;
    }

    /**
     * Invoked by {@link PropertyType} implementations for declaring the {@code <gml:*PropertyType>}
     * instance which is wrapping the {@code <gml:*Type>} object to (un)marshal.
     *
     * <p>For performance reasons, this {@code wrapper} information is not provided by default.
     * To get this information, the {@code PropertyType} implementation needs to define the
     * {@code beforeUnmarshal(…)} method. For an implementation example, see
     * {@link org.apache.sis.xml.bind.referencing.CC_OperationParameter}.</p>
     *
     * @param context  the current context, or {@code null} if none.
     * @param wrapper  the {@code <gml:*PropertyType>} which is wrapping the {@code <gml:*Type>} object to (un)marshal,
     *                 or {@code null} if unknown.
     */
    public static void setWrapper(final Context context, final PropertyType<?,?> wrapper) {
        if (context != null) {
            context.wrapper = wrapper;
        }
    }

    /**
     * If a {@code gml:id} value has already been used for the given object in the current XML document,
     * returns that identifier. Otherwise returns {@code null}.
     *
     * @param  context  the current context, or {@code null} if none.
     * @param  object   the object for which to get the {@code gml:id}.
     * @return the identifier used in the current XML document for the given object, or {@code null} if none.
     */
    public static String getObjectID(final Context context, final Object object) {
        return (context != null) ? context.objectToXmlid.get(object) : null;
    }

    /**
     * Returns the object for the given {@code gml:id}, or {@code null} if none.
     * GML identifiers can be referenced by the fragment part of URI in XLinks.
     * This association is valid only for the current XML document.
     *
     * @param  context  the current context, or {@code null} if none.
     * @param  id       the identifier for which to get the object.
     * @return the object associated to the given identifier, or {@code null} if none.
     */
    public static Object getObjectForID(final Context context, final String id) {
        return (context != null) ? context.xmlidToObject.get(id) : null;
    }

    /**
     * Associates the given object to the given identifier if that identifier is available.
     * Returns {@code true} if the given identifier was available, or {@code false} if used by another object.
     * In the latter case, this method does nothing (the existing object is not replaced) and the caller should
     * try again with another identifier candidate.
     *
     * @param  context  the current context, or {@code null} if none.
     * @param  object   the object for which to assign the {@code gml:id}.
     * @param  id       the identifier to assign to the given object.
     * @return {@code true} if the given identifier can be used.
     */
    public static boolean setObjectForID(final Context context, final Object object, final String id) {
        if (context != null) {
            final Object existing = context.xmlidToObject.putIfAbsent(id, object);
            if (existing != null) {
                // Note: `existing.equals(object)` would be unreliable because object construction is unfinished.
                return existing == object;
            }
            if (context.objectToXmlid.put(object, id) != null) {
                // Should never happen because all put calls are in this method.
                throw new CorruptedObjectException(id);
            }
        }
        return true;
    }

    /**
     * Returns the object for the given {@code gml:id} in the specified document, or {@code null} if none.
     * GML identifiers can be referenced by the fragment part of URI in XLinks. This association is valid
     * only for documents referenced in {@code xlink:href} attributes since the root document of current
     * unmarshalling process. This is not a system-wide cache.
     *
     * <p>For documents that are read from a file or an URL, the {@code systemId} argument should be the value of
     * {@link java.net.URI#toASCIIString()} for consistency with {@link javax.xml.transform.stream.StreamSource}.
     * However, the original URI instances should be used instead when they are available.
     * By convention, a null {@code id} returns the whole document.</p>
     *
     * @param  systemId  document identifier (without the fragment part) as an {@link URI} or a {@link String}.
     * @param  fragment  the fragment part of the URI, or {@code null} for the whole document.
     * @return the object associated to the given identifier, or {@code null} if none,
     *         or {@link #INVALID_OBJECT} if a parsing was previously attempted and failed.
     */
    public final Object getExternalObjectForID(final Object systemId, final String fragment) {
        final Map<String,Object> cache = documentToXmlids.get(systemId);
        if (cache == null) {
            return null;
        }
        final Object value = cache.get(fragment);
        if (value == null && cache.get(null) == INVALID_OBJECT) {
            return INVALID_OBJECT;
        }
        return value;
    }

    /**
     * Adds in the cache the result of unmarshalling a document referenced by {@code xlink:href}.
     * This method cache the whole document, ignoring the fragment in the {@code xlink:href}.
     * The fragment part is obtained by {@link #getExternalObjectForID(Object, String)}.
     *
     * @param  systemId  document identifier (without the fragment part) as an {@link URI} or a {@link String}.
     * @param  document  the document to cache, or {@link #INVALID_OBJECT} for recording a failure to read the document.
     */
    public final void cacheDocument(final Object systemId, final Object document) {
        final Map<String, Object> cache = documentToXmlids.get(systemId);
        if (cache == null || cache.isEmpty()) {
            documentToXmlids.put(systemId, Collections.singletonMap(null, document));
        } else {
            cache.put(null, document);
        }
    }

    /**
     * Returns the {@code XLink} reference resolver for the current marshalling or unmarshalling process.
     * If no link handler has been explicitly set, then this method returns {@link ExternalLinkHandler#DEFAULT}.
     *
     * <div class="note"><b>API note:</b>
     * This method is static for the convenience of performing the check for null context.</div>
     *
     * @param  context  the current context, or {@code null} if none.
     * @return the current link handler (never null).
     */
    public static ExternalLinkHandler linkHandler(final Context context) {
        if (context != null) {
            final ExternalLinkHandler linkHandler = context.linkHandler;
            if (linkHandler != null) {
                return linkHandler;
            }
        }
        return ExternalLinkHandler.DEFAULT;
    }

    /**
     * Returns the reference resolver in use for the current marshalling or unmarshalling process.
     * If no resolver has been explicitly set, then this method returns {@link ReferenceResolver#DEFAULT}.
     *
     * <div class="note"><b>API note:</b>
     * This method is static for the convenience of performing the check for null context.</div>
     *
     * @param  context  the current context, or {@code null} if none.
     * @return the current reference resolver (never null).
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
     * If no converter were explicitly set, then this method returns {@link ValueConverter#DEFAULT}.
     *
     * <div class="note"><b>API note:</b>
     * This method is static for the convenience of performing the check for null context.</div>
     *
     * @param  context  the current context, or {@code null} if none.
     * @return the current value converter (never null).
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
     * Sends a warning to the warning listener if there is one, or logs the warning otherwise.
     * In the latter case, this method logs to {@link #LOGGER}.
     *
     * <p>If the given {@code resources} is {@code null}, then this method will build the log
     * message from the {@code exception}.</p>
     *
     * @param  context    the current context, or {@code null} if none.
     * @param  level      the logging level.
     * @param  classe     the class to declare as the warning source.
     * @param  method     the name of the method to declare as the warning source.
     * @param  exception  the exception thrown, or {@code null} if none.
     * @param  resources  either {@code Errors.class} or {@code Messages.class}, or {@code null} for the exception message.
     * @param  key        the resource keys as one of the constants defined in the {@code Keys} inner class.
     * @param  arguments  the arguments to be given to {@code MessageFormat} for formatting the log message.
     */
    public static void warningOccured(final Context context,
            final Level level, final Class<?> classe, final String method, final Throwable exception,
            final Class<? extends IndexedResourceBundle> resources, final short key, final Object... arguments)
    {
        final Locale locale = (context != null) ? context.locale : null;
        final LogRecord record;
        if (resources != null) {
            final IndexedResourceBundle bundle;
            if (resources == Errors.class) {
                bundle = Errors.forLocale(locale);
            } else if (resources == Messages.class) {
                bundle = Messages.forLocale(locale);
            } else {
                throw new IllegalArgumentException(String.valueOf(resources));
            }
            record = bundle.createLogRecord(level, key, arguments);
        } else {
            record = new LogRecord(level, Exceptions.formatChainedMessages(locale, null, exception));
        }
        record.setSourceClassName(classe.getCanonicalName());
        record.setSourceMethodName(method);
        record.setLoggerName(Loggers.XML);
        if (context != null) {
            final Filter logFilter = context.logFilter;
            if (logFilter != null) {
                record.setThrown(exception);
                if (!logFilter.isLoggable(record)) {
                    return;
                }
                record.setThrown(null);
            }
        }
        /*
         * Log the warning without stack-trace, since this method shall be used
         * only for non-fatal warnings and we want to avoid polluting the logs.
         */
        LOGGER.log(record);
    }

    /**
     * Convenience method for sending a warning for the given message from the {@link Errors} or {@link Messages}
     * resources. The message will be logged at {@link Level#WARNING}.
     *
     * @param  context    the current context, or {@code null} if none.
     * @param  classe     the class to declare as the warning source.
     * @param  method     the name of the method to declare as the warning source.
     * @param  resources  either {@code Errors.class} or {@code Messages.class}.
     * @param  key        the resource keys as one of the constants defined in the {@code Keys} inner class.
     * @param  arguments  the arguments to be given to {@code MessageFormat} for formatting the log message.
     */
    public static void warningOccured(final Context context, final Class<?> classe, final String method,
            final Class<? extends IndexedResourceBundle> resources, final short key, final Object... arguments)
    {
        warningOccured(context, Level.WARNING, classe, method, null, resources, key, arguments);
    }

    /**
     * Convenience method for sending a warning for the given exception.
     * The log message will be the message of the given exception.
     *
     * @param  context    the current context, or {@code null} if none.
     * @param  classe     the class to declare as the warning source.
     * @param  method     the name of the method to declare as the warning source.
     * @param  cause      the exception which occurred.
     * @param  isWarning  {@code true} for {@link Level#WARNING}, or {@code false} for {@link Level#FINE}.
     */
    public static void warningOccured(final Context context, final Class<?> classe,
            final String method, final Exception cause, final boolean isWarning)
    {
        warningOccured(context, isWarning ? Level.WARNING : Level.FINE, classe, method, cause,
                null, (short) 0, (Object[]) null);
    }

    /**
     * Invoked in a {@code finally} block when a (un)marshalling process is finished.
     * This method should be invoked on the instance created by the constructor or
     * on the instance returned by the {@link #push(ExternalLinkHandler)} method,
     * <em>not</em> on the instance returned by {@link #current()} because the latter
     * may cause a memory leak if some calls to the {@link #pull()} method are missing.
     */
    public final void finish() {
        if ((bitMasks & CLEAR_SEMAPHORE) != 0) {
            Semaphores.clear(Semaphores.NULL_COLLECTION);
        }
        if (previous != null) {
            CURRENT.set(previous);
        } else {
            CURRENT.remove();
        }
    }

    /**
     * {@return a string representation of this context for debugging purposes}.
     */
    @Override
    public String toString() {
        return Strings.toString(getClass(), "operation", (bitMasks & MARSHALLING) != 0 ? "marshal" : "unmarshal",
                "locale", locale, "timezone", timezone,
                null, (bitMasks & LENIENT_UNMARSHAL) != 0 ? "lenient"  : null);
    }
}
