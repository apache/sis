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
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.lang.reflect.Proxy;
import javax.xml.transform.Source;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.JAXBException;
import org.opengis.metadata.Identifier;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Emptiable;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.internal.Strings;
import org.apache.sis.xml.bind.Context;
import org.apache.sis.xml.bind.gcx.Anchor;
import org.apache.sis.xml.util.ExternalLinkHandler;


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
     * Creates a default {@code ReferenceResolver}. This constructor is for subclasses only.
     */
    protected ReferenceResolver() {
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
     *   <li>Otherwise, if {@code xlink:href} {@linkplain URI#isAbsolute() is absolute} or has a non-empty
     *       {@linkplain URI#getPath() path}, delegate to {@link #resolveExternal(MarshalContext, Source)}.</li>
     *   <li>Otherwise, if {@code xlink:href} is a {@linkplain URI#getFragment() fragment} of the form {@code "#foo"}
     *       and if an object of class {@code type} with the {@code gml:id="foo"} attribute has previously been seen
     *       in the same XML document, then return that object.</li>
     *   <li>Otherwise, returns {@code null}.</li>
     * </ul>
     *
     * If an object is found but is not of the class declared in {@code type},
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
        final Object label, object;
        final Context c = (context instanceof Context) ? (Context) context : Context.current();
        if (!href.isAbsolute() && Strings.isNullOrEmpty(href.getPath())) {
            final String id = href.getFragment();       // Taken as the `gml:id` value to look for.
            if (Strings.isNullOrEmpty(id)) {
                return null;
            }
            object = Context.getObjectForID(c, id);
            label  = id;                // Used if the object is invalid.
        } else try {
            final Source source = Context.linkHandler(c).openReader(href);
            object = (source != null) ? resolveExternal(c, source) : null;
            label  = href;              // Used if the object is invalid.
        } catch (Exception e) {
            Context.warningOccured(c, Level.WARNING, ReferenceResolver.class, "resolve",
                                   e, Errors.class, Errors.Keys.CanNotRead_1, href);
            return null;
        }
        if (type.isInstance(object)) {
            return type.cast(object);
        } else {
            final short key;
            final Object[] args;
            if (object == null) {
                key = Errors.Keys.NotABackwardReference_1;
                args = new Object[] {label.toString()};
            } else {
                key = Errors.Keys.UnexpectedTypeForReference_3;
                args = new Object[] {label.toString(), type, object.getClass()};
            }
            Context.warningOccured(c, ReferenceResolver.class, "resolve", Errors.class, key, args);
        }
        return null;
    }

    /**
     * Returns an object defined in an external document, or {@code null} if none.
     * This method is invoked automatically by {@link #resolve(MarshalContext, Class, XLink)}
     * when the {@code xlink:href} attribute is absolute or contains the path to a document.
     * The default implementation loads the file from the given source if it is not in the cache,
     * then returns the object identified by the fragment part of the URI.
     *
     * <p>The URL of the document to load, if known, should be given by {@link Source#getSystemId()}.</p>
     *
     * @param  context  context (GML version, locale, <i>etc.</i>) of the (un)marshalling process.
     * @param  source   source of the document specified by the {@code xlink:href} attribute value.
     * @return an object for the given source, or {@code null} if none.
     * @throws IOException if an error occurred while opening the document.
     * @throws JAXBException if an error occurred while parsing the document.
     *
     * @since 1.5
     */
    protected Object resolveExternal(final MarshalContext context, final Source source) throws IOException, JAXBException {
        final MarshallerPool pool = context.getPool();
        final Unmarshaller m = pool.acquireUnmarshaller();
        final URI uri = ExternalLinkHandler.ifOnlyURI(source);
        final Object object;
        if (uri != null) {
            object = m.unmarshal(uri.toURL());
        } else {
            object = m.unmarshal(source);
        }
        pool.recycle(m);
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
     * @param  value    the value for which an anchor is requested. Often the same instance than {@code text},
     *                  but can also be the {@link java.net.URI} or {@link java.util.Locale} instance for which
     *                  {@code text} is a string representation.
     * @param  text     the textual representation of the value for which to get the anchor.
     * @return the anchor for the given text, or {@code null} if none.
     */
    public XLink anchor(final MarshalContext context, final Object value, final CharSequence text) {
        return (text instanceof Anchor) ? (Anchor) text : null;
    }
}
