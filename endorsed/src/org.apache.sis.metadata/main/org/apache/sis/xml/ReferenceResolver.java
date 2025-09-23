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

import java.net.URI;
import java.util.UUID;
import java.lang.reflect.Proxy;
import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;
import jakarta.xml.bind.Unmarshaller;
import org.opengis.metadata.Identifier;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Emptiable;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.xml.bind.Context;
import org.apache.sis.xml.bind.gcx.Anchor;
import org.apache.sis.xml.internal.shared.ExternalLinkHandler;
import org.apache.sis.xml.internal.shared.URISource;
import org.apache.sis.xml.internal.shared.XmlUtilities;


/**
 * Controls the (un)marshaller behavior regarding the {@code xlink} or {@code uuidref} attributes.
 * At marshalling time, this class controls whether the marshaller is allowed to write a reference
 * to an existing instance instead of writing the full object definition.
 * At unmarshalling time, this class replaces (if possible) a reference by the full object definition.
 *
 * <p>Subclasses can override the methods defined in this class in order to search in their own catalog.
 * See the {@link XML#RESOLVER} javadoc for an example of registering a custom {@code ReferenceResolver}
 * to a unmarshaller.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   0.3
 */
public class ReferenceResolver {
    /**
     * The default and thread-safe instance. This instance is used at unmarshalling time when
     * no {@code ReferenceResolver} was explicitly set by the {@link XML#RESOLVER} property.
     */
    public static final ReferenceResolver DEFAULT = new ReferenceResolver();

    /**
     * Provider of sources to use for unmarshalling objects referenced by links to another document.
     * It provides the {@code source} argument in {@link #resolveExternal(MarshalContext, Source)}.
     * If {@code null}, a default resolution is done.
     *
     * @since 1.5
     */
    protected final URIResolver externalSourceResolver;

    /**
     * Creates a default {@code ReferenceResolver}. This constructor is for subclasses only.
     */
    protected ReferenceResolver() {
        externalSourceResolver = null;
    }

    /**
     * Creates a new resolver which will use the specified provider of sources for unmarshalling external documents.
     * The specified resolver is invoked when a {@code xlink:href} attribute is found, and the associated URI value
     * has a path to an external document. The resolver provides the {@code source} argument which will be given to
     * {@link #resolveExternal(MarshalContext, Source)}. If the specified resolver returns {@code null} for a given
     * {@code xlink:href}, then {@code ReferenceResolver} will try to resolve the URI itself.
     *
     * @param externalSourceResolver  resolver of sources, or {@code null} for letting {@code ReferenceResolver}
     *        resolving the URIs itself.
     *
     * @since 1.5
     */
    public ReferenceResolver(final URIResolver externalSourceResolver) {
        this.externalSourceResolver = externalSourceResolver;
    }

    /**
     * Returns an empty object of the given type having the given identifiers.
     * The object returned by the default implementation has the following properties:
     *
     * <ul>
     *   <li>Implements the given {@code type} interface.</li>
     *   <li>Implements the {@link NilObject} and {@link IdentifiedObject} interfaces from this package.</li>
     *   <li>{@link IdentifiedObject#getIdentifiers()} will return the given identifiers.</li>
     *   <li>{@link IdentifiedObject#getIdentifierMap()} will return a {@link java.util.Map}
     *       view over the given identifiers.</li>
     *   <li>All other methods except the ones inherited from the {@link Object} class will return
     *       an empty collection, an empty array, {@code null}, {@link Double#NaN}, 0 or {@code false},
     *       depending on the method return type.</li>
     * </ul>
     *
     * @param  <T>          the compile-time type of the {@code type} argument.
     * @param  context      context (GML version, locale, <i>etc.</i>) of the (un)marshalling process.
     * @param  type         the type of object to be unmarshalled, often as a GeoAPI interface.
     * @param  identifiers  an arbitrary number of identifiers. For each identifier,
     *         the {@linkplain org.apache.sis.referencing.ImmutableIdentifier#getAuthority() authority}
     *         is typically (but not necessarily) one of the constants defined in {@link IdentifierSpace}.
     * @return an object of the given type for the given identifiers, or {@code null} if none.
     */
    @SuppressWarnings("unchecked")
    public <T> T newIdentifiedObject(final MarshalContext context, final Class<T> type, final Identifier... identifiers) {
        if (NilObjectHandler.isIgnoredInterface(type)) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "type", type));
        }
        return (T) Proxy.newProxyInstance(ReferenceResolver.class.getClassLoader(),
                new Class<?>[] {type, IdentifiedObject.class, NilObject.class, LenientComparable.class},
                new NilObjectHandler(identifiers));
    }

    /**
     * Returns an object of the given type for the given {@code uuid} attribute, or {@code null} if none.
     * The default implementation returns {@code null} in all cases.
     *
     * @param  <T>      the compile-time type of the {@code type} argument.
     * @param  context  context (GML version, locale, <i>etc.</i>) of the (un)marshalling process.
     * @param  type     the type of object to be unmarshalled, often as a GeoAPI interface.
     * @param  uuid     the {@code uuid} attributes.
     * @return an object of the given type for the given {@code uuid} attribute, or {@code null} if none.
     */
    public <T> T resolve(final MarshalContext context, final Class<T> type, final UUID uuid) {
        ArgumentChecks.ensureNonNull("type", type);
        ArgumentChecks.ensureNonNull("uuid", uuid);
        return null;
    }

    /**
     * Returns an object of the given type for the given {@code xlink} attribute, or {@code null} if none.
     * The default implementation fetches the {@link XLink#getHRef() xlink:href} attribute, then:
     *
     * <ul>
     *   <li>If {@code xlink:href} is null or {@linkplain URI#isOpaque() opaque}, returns {@code null}.</li>
     *   <li>Otherwise, if the {@code xlink:href} URI is {@linkplain URI#isAbsolute() is absolute} or has a
     *       {@linkplain URI#getPath() path}, delegates to {@link #resolveExternal(MarshalContext, Source)}.</li>
     *   <li>Otherwise, the URI is a {@linkplain URI#getFragment() fragment} such as {@code "#foo"}. Then:
     *     <ul>
     *       <li>If an object of class {@code type} with an identifier attribute such as {@code gml:id="foo"}
     *           has previously been seen in the same XML document (i.e., "foo" is a backward reference),
     *           returns that object.</li>
     *       <li>Otherwise, emits a warning and returns {@code null}.
     *           Note that it may happen if the {@code xlink:href} is a forward reference.</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * If an object is found but is not of the class declared in {@code type},
     * or if an {@link Exception} was thrown during object unmarshalling,
     * then this method emits a warning and returns {@code null}.
     *
     * @param  <T>      the compile-time type of the {@code type} argument.
     * @param  context  context (GML version, locale, <i>etc.</i>) of the (un)marshalling process.
     * @param  type     the type of object to be unmarshalled, often as a GeoAPI interface.
     * @param  link     the {@code xlink} attributes.
     * @return an object of the given type for the given {@code xlink} attribute, or {@code null} if none.
     */
    public <T> T resolve(final MarshalContext context, final Class<T> type, final XLink link) {
        ArgumentChecks.ensureNonNull("type",  type);
        ArgumentChecks.ensureNonNull("xlink", link);
        final URI href = link.getHRef();
        if (href == null || href.isOpaque()) {
            return null;
        }
        final Object object;
        final short reasonIfNull;             // An Errors.Key value with one parameter, or 0.
        final Context c = (context instanceof Context) ? (Context) context : Context.current();
        if (!href.isAbsolute() && Strings.isNullOrEmpty(href.getPath())) {
            /*
             * URI defined only by an anchor in the same document. There is nothing to load,
             * we just check for previously unmarshalled objects in the same marshal context.
             * Current implementation supports only backward references.
             *
             * For forward references, see https://issues.apache.org/jira/browse/SIS-420
             */
            final String fragment = Strings.trimOrNull(href.getFragment());
            if (fragment == null) {
                return null;
            }
            object = Context.getObjectForID(c, fragment);
            reasonIfNull = Errors.Keys.NotABackwardReference_1;
        } else try {
            /*
             * URI to an external document. If a `javax.xml.stream.XMLResolver` property was set on the unmarshaller,
             * use the user supplied `URIResolver`. If there is no URI resolver or the URI resolver cannot resolve,
             * fallback on the Apache SIS `ExternalLinkHandler` implementation. The latter is the usual case.
             */
            final ExternalLinkHandler handler = Context.linkHandler(c);
            Source source = null;
            if (externalSourceResolver != null) {
                Object base = handler.getBase();
                if (base != null) {
                    source = externalSourceResolver.resolve(href.toString(), base.toString());
                }
            }
            if (source == null && (source = handler.openReader(href)) == null) {
                reasonIfNull = Errors.Keys.CanNotResolveAsAbsolutePath_1;
                object = null;
            } else {
                object = resolveExternal(context, source);
                reasonIfNull = 0;
            }
        } catch (Exception e) {
            ExternalLinkHandler.warningOccured(href, e);
            return null;
        }
        /*
         * At this point, the referenced object has been fetched or unmarshalled.
         * The result may be null, in which case the warning to emit depends on the
         * reason why the object is null: could not resolve, or could not unmarshall.
         */
        if (type.isInstance(object)) {
            return type.cast(object);
        }
        final short key;
        final Object[] args;
        if (object == null) {
            if (reasonIfNull == 0) {
                return null;
            }
            key = reasonIfNull;
            args = new Object[] {href.toString()};
        } else {
            key = Errors.Keys.UnexpectedTypeForReference_3;
            args = new Object[] {href.toString(), type, object.getClass()};
        }
        Context.warningOccured(c, ReferenceResolver.class, "resolve", Errors.class, key, args);
        return null;
    }

    /**
     * Returns an object defined in an external document, or {@code null} if none.
     * This method is invoked automatically by {@link #resolve(MarshalContext, Class, XLink)}
     * when the {@code xlink:href} attribute is absolute or contains the path to a document.
     * The default implementation loads the file from the given source if it is not in the cache,
     * then returns the object identified by the fragment part of the URI.
     *
     * <p>The {@code source} argument should have been determined by the caller has below:</p>
     * <ul>
     *   <li>If an {@link URIResolver} has been specified at construction time, delegates to it.</li>
     *   <li>Otherwise or if the above returned {@code null}, then if the source of the current document
     *       is associated to a {@link javax.xml.stream.XMLResolver}, delegates to it.</li>
     *   <li>Otherwise, the caller tries to resolve the URI itself.</li>
     * </ul>
     * The resolved URL, if known, should be available in {@link Source#getSystemId()}.
     *
     * <h4>Error handling</h4>
     * The default implementation keeps a cache during the execution of an {@code XML.unmarshall(…)} method
     * (or actually, during a {@linkplain MarshallerPool pooled unmarshaller} method).
     * If an exception is thrown during the document unmarshalling, this failure is also recorded in the cache.
     * Therefore, the exception is thrown only during the first attempt to read the document
     * and {@code null} is returned directly on next attempts for the same source.
     * Exceptions thrown by this method are caught by {@link #resolve(MarshalContext, Class, XLink) resolve(…)}
     * and reported as warnings.
     *
     * @param  context  context (GML version, locale, <i>etc.</i>) of the (un)marshalling process.
     * @param  source   source of the document specified by the {@code xlink:href} attribute value.
     * @return an object for the given source, or {@code null} if none, for example because of failure in a previous attempt.
     * @throws Exception if an error occurred while opening or parsing the document.
     *
     * @since 1.5
     */
    protected Object resolveExternal(final MarshalContext context, final Source source) throws Exception {
        final Object document;
        final String fragment;
        final URI uri;
        if (source instanceof URISource) {
            final var s = (URISource) source;
            uri = s.getReadableURI();
            document = s.document;
            fragment = s.fragment;
        } else {
            uri = null;
            final int s;
            final String systemId = source.getSystemId();
            if (systemId != null && (s = systemId.lastIndexOf('#')) >= 0) {
                document = Strings.trimOrNull(systemId.substring(0,s));
                fragment = Strings.trimOrNull(systemId.substring(s+1));
            } else {
                document = systemId;
                fragment = null;
            }
        }
        /*
         * At this point, we got the system identifier (usually as a resolved URI, but not necessarily)
         * and the URI fragment to use as a GML identifier. Check if the document is in the cache.
         * Note that if the fragment is null, then by convention we lookup for the whole document.
         */
        final Context c = (context instanceof Context) ? (Context) context : Context.current();
        if (c != null) {
            final Object object = c.getExternalObjectForID(document, fragment);
            if (object != null) {
                XmlUtilities.close(source);
                return (object != Context.INVALID_OBJECT) ? object : null;
            }
        }
        /*
         * Object not found in the cache. Parse it. As a side-effect of unmarshalling the document,
         * a map of fragments found in the document will be populated. We use that map at the end
         * for extracting the requested object.
         */
        final MarshallerPool pool = context.getPool();
        final Unmarshaller m = pool.acquireUnmarshaller();
        if (m instanceof Pooled) {
            ((Pooled) m).forIncludedDocument(document);
        }
        final Object object;
        try {
            if (uri != null) {
                object = m.unmarshal(uri.toURL());
            } else {
                object = m.unmarshal(source);
            }
        } catch (Exception e) {
            if (c != null) {
                c.cacheDocument(document, Context.INVALID_OBJECT);
            }
            throw e;
        }
        pool.recycle(m);
        /*
         * All fragments in the referenced document should be in the cache now.
         * Cache the whole document, then request the fragment from the cache.
         */
        if (c != null) {
            c.cacheDocument(document, object);
            if (fragment != null) {
                Object part = c.getExternalObjectForID(document, fragment);
                if (part != null || uri != null) return part;
                /*
                 * Fragment not found. The source was not built by ourselves (`uri == null`),
                 * so maybe the user provided a source which was returning directly the fragment.
                 */
            }
        }
        return object;
    }

    /**
     * Returns {@code true} if the marshaller can use a {@code xlink:href="#id"} reference to the given object
     * instead of writing the full XML element. This method is invoked by the marshaller when:
     *
     * <ul>
     *   <li>The given object has already been marshalled in the same XML document.</li>
     *   <li>The marshalled object had a {@code gml:id} attribute
     *     <ul>
     *       <li>either specified explicitly by
     *         <code>{@linkplain IdentifierMap#put IdentifierMap.put}({@linkplain IdentifierSpace#ID}, id)</code></li>
     *       <li>or inferred automatically by the marshalled object
     *         (e.g. {@link org.apache.sis.referencing.AbstractIdentifiedObject}).</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * Note that if this method returns {@code true}, then the use of {@code xlink:href="#id"} will have
     * precedence over {@linkplain #canSubstituteByReference(MarshalContext, Class, Object, UUID) UUID}
     * and {@linkplain #canSubstituteByReference(MarshalContext, Class, Object, XLink) XLink alternatives}.
     *
     * <p>The default implementation unconditionally returns {@code true}.
     * Subclasses can override this method if they want to filter which objects to declare by reference.</p>
     *
     * @param  <T>      the compile-time type of the {@code type} argument.
     * @param  context  context (GML version, locale, <i>etc.</i>) of the (un)marshalling process.
     * @param  type     the type of object to be unmarshalled, often as a GeoAPI interface.
     * @param  object   the object to be marshalled.
     * @param  id       the {@code gml:id} value of the object to be marshalled.
     * @return {@code true} if the marshaller can use the {@code xlink:href="#id"} attribute
     *         instead of marshalling the given object.
     *
     * @since 0.7
     */
    public <T> boolean canSubstituteByReference(final MarshalContext context, final Class<T> type, final T object, final String id) {
        return true;
    }

    /**
     * Returns {@code true} if the marshaller can use a reference to the given object
     * instead of writing the full XML element. This method is invoked when an object to
     * be marshalled has a UUID identifier. Because those object may be defined externally,
     * SIS cannot know if the object shall be fully marshalled or not.
     * Such information needs to be provided by the application.
     *
     * <p>The default implementation returns {@code true} in the following cases:</p>
     * <ul>
     *   <li>If {@code object} implements {@link NilObject}.</li>
     *   <li>If {@code object} implements {@link Emptiable} and its {@code isEmpty()} method returns {@code true}.</li>
     * </ul>
     *
     * Subclasses can override this method if they know whether the receiver will be able to resolve the reference.
     *
     * @param  <T>      the compile-time type of the {@code type} argument.
     * @param  context  context (GML version, locale, <i>etc.</i>) of the (un)marshalling process.
     * @param  type     the type of object to be unmarshalled, often as a GeoAPI interface.
     * @param  object   the object to be marshalled.
     * @param  uuid     the unique identifier of the object to be marshalled.
     * @return {@code true} if the marshaller can use the {@code uuidref} attribute
     *         instead of marshalling the given object.
     */
    public <T> boolean canSubstituteByReference(final MarshalContext context, final Class<T> type, final T object, final UUID uuid) {
        return (object instanceof NilObject) || (object instanceof Emptiable && ((Emptiable) object).isEmpty());
    }

    /**
     * Returns {@code true} if the marshaller can use a {@code xlink:href} reference to the given
     * object instead of writing the full XML element. This method is invoked when an object to be
     * marshalled has a {@link XLink} identifier. Because those object may be defined externally,
     * SIS cannot know if the object shall be fully marshalled or not.
     * Such information needs to be provided by the application.
     *
     * <p>The default implementation returns {@code true} in the following cases:</p>
     * <ul>
     *   <li>If {@code object} implements {@link NilObject}.</li>
     *   <li>If {@code object} implements {@link Emptiable} and its {@code isEmpty()} method returns {@code true}.</li>
     * </ul>
     *
     * Subclasses can override this method if they know whether the receiver will be able to resolve the reference.
     *
     * @param  <T>      the compile-time type of the {@code type} argument.
     * @param  context  context (GML version, locale, <i>etc.</i>) of the (un)marshalling process.
     * @param  type     the type of object to be unmarshalled, often as a GeoAPI interface.
     * @param  object   the object to be marshalled.
     * @param  link     the reference of the object to be marshalled.
     * @return {@code true} if the marshaller can use the {@code xlink:href} attribute
     *         instead of marshalling the given object.
     */
    public <T> boolean canSubstituteByReference(final MarshalContext context, final Class<T> type, final T object, final XLink link) {
        return (object instanceof NilObject) || (object instanceof Emptiable && ((Emptiable) object).isEmpty());
    }

    /**
     * Returns the {@code <gcx:Anchor>} to use for the given text, or {@code null} if none.
     * Anchors can appear in ISO 19115-3 documents where we would normally expect a character sequence.
     * For example:
     *
     * <table class="sis">
     * <caption>XML representations of string</caption>
     * <tr>
     *   <th>As {@code <gco:CharacterString>}</th>
     *   <th>As {@code <gcx:Anchor>}</th>
     * </tr><tr>
     * <td>
     *   <pre> &lt;cit:country&gt;
     *     &lt;gco:CharacterString&gt;France&lt;/gco:CharacterString&gt;
     * &lt;/cit:country&gt;</pre>
     * </td><td>
     *   <pre> &lt;cit:country&gt;
     *     &lt;gcx:Anchor xlink:href="SDN:C320:2:FR"&gt;France&lt;/gcx:Anchor&gt;
     * &lt;/cit:country&gt;</pre>
     * </td></tr>
     * </table>
     *
     * Subclasses can override this method if they can provide a mapping from some text values to anchors.
     *
     * @param  context  context (GML version, locale, <i>etc.</i>) of the (un)marshalling process.
     * @param  value    the value for which an anchor is requested. Often the same instance as {@code text},
     *                  but can also be the {@link java.net.URI} or {@link java.util.Locale} instance for which
     *                  {@code text} is a string representation.
     * @param  text     the textual representation of the value for which to get the anchor.
     * @return the anchor for the given text, or {@code null} if none.
     */
    public XLink anchor(final MarshalContext context, final Object value, final CharSequence text) {
        return (text instanceof Anchor) ? (Anchor) text : null;
    }
}
