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
import org.opengis.metadata.Identifier;
import org.apache.sis.util.Emptiable;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.internal.jaxb.gmx.Anchor;
import org.apache.sis.internal.jaxb.Context;

import static org.apache.sis.util.ArgumentChecks.*;


/**
 * Controls the (un)marshaller behavior regarding the {@code xlink} or {@code uuidref} attributes.
 * At marshalling time, this class controls whether the marshaller is allowed to write a reference
 * to an existing instance instead than writing the full object definition.
 * At unmarshalling time, this class replaces (if possible) a reference by the full object definition.
 *
 * <p>Subclasses can override the methods defined in this class in order to search in their own catalog.
 * See the {@link XML#RESOLVER} javadoc for an example of registering a custom {@code ReferenceResolver}
 * to a unmarshaller.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
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
     *       an empty collection, an empty array, {@code null}, {@link Double#NaN NaN}, 0 or
     *       {@code false}, depending on the method return type.</li>
     * </ul>
     *
     * @param  <T>     The compile-time type of the {@code type} argument.
     * @param  context Context (GML version, locale, <i>etc.</i>) of the (un)marshalling process.
     * @param  type    The type of object to be unmarshalled, often as a GeoAPI interface.
     * @param  identifiers An arbitrary amount of identifiers. For each identifier, the
     *         {@linkplain Identifier#getAuthority() authority} is typically (but not
     *         necessarily) one of the constants defined in {@link IdentifierSpace}.
     * @return An object of the given type for the given identifiers, or {@code null} if none.
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
     * @param  <T>     The compile-time type of the {@code type} argument.
     * @param  context Context (GML version, locale, <i>etc.</i>) of the (un)marshalling process.
     * @param  type    The type of object to be unmarshalled, often as a GeoAPI interface.
     * @param  uuid The {@code uuid} attributes.
     * @return An object of the given type for the given {@code uuid} attribute, or {@code null} if none.
     */
    @SuppressWarnings("unchecked")
    public <T> T resolve(final MarshalContext context, final Class<T> type, final UUID uuid) {
        ensureNonNull("type", type);
        ensureNonNull("uuid", uuid);
        return null;
    }

    /**
     * Returns an object of the given type for the given {@code xlink} attribute, or {@code null} if none.
     * The default implementation performs the following lookups:
     *
     * <ul>
     *   <li>If the {@link XLink#getHRef() xlink:href} attribute is an {@linkplain URI#getFragment() URI fragment}
     *       of the form {@code "#foo"} and if an object of class {@code type} with the {@code gml:id="foo"} attribute
     *       has previously been seen in the same XML document, then that object is returned.</li>
     *   <li>Otherwise returns {@code null}.</li>
     * </ul>
     *
     * @param  <T>     The compile-time type of the {@code type} argument.
     * @param  context Context (GML version, locale, <i>etc.</i>) of the (un)marshalling process.
     * @param  type    The type of object to be unmarshalled, often as a GeoAPI interface.
     * @param  link    The {@code xlink} attributes.
     * @return An object of the given type for the given {@code xlink} attribute, or {@code null} if none.
     */
    public <T> T resolve(final MarshalContext context, final Class<T> type, final XLink link) {
        ensureNonNull("type",  type);
        ensureNonNull("xlink", link);
        final URI href = link.getHRef();
        if (href != null && href.toString().startsWith("#")) {
            final String id = href.getFragment();
            final Context c = (context instanceof Context) ? (Context) context : Context.current();
            final Object object = Context.getObjectForID(c, id);
            if (type.isInstance(object)) {
                return type.cast(object);
            } else {
                final short key;
                final Object args;
                if (object == null) {
                    key = Errors.Keys.NotABackwardReference_1;
                    args = id;
                } else {
                    key = Errors.Keys.UnexpectedTypeForReference_3;
                    args = new Object[] {id, type, object.getClass()};
                }
                Context.warningOccured(c, ReferenceResolver.class, "resolve", Errors.class, key, args);
            }
        }
        return null;
    }

    /**
     * Returns {@code true} if the marshaller can use a {@code xlink:href="#id"} reference to the given object
     * instead than writing the full XML element. This method is invoked by the marshaller when:
     *
     * <ul>
     *   <li>The given object has already been marshalled in the same XML document.</li>
     *   <li>The marshalled object had a {@code gml:id} attribute
     *     <ul>
     *       <li>either specified explicitely by
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
     * @param  <T>     The compile-time type of the {@code type} argument.
     * @param  context Context (GML version, locale, <i>etc.</i>) of the (un)marshalling process.
     * @param  type    The type of object to be unmarshalled, often as a GeoAPI interface.
     * @param  object  The object to be marshalled.
     * @param  id      The {@code gml:id} value of the object to be marshalled.
     * @return {@code true} if the marshaller can use the {@code xlink:href="#id"} attribute
     *         instead than marshalling the given object.
     *
     * @since 0.7
     */
    public <T> boolean canSubstituteByReference(final MarshalContext context, final Class<T> type, final T object, final String id) {
        return true;
    }

    /**
     * Returns {@code true} if the marshaller can use a reference to the given object
     * instead than writing the full XML element. This method is invoked when an object to
     * be marshalled has a UUID identifier. Because those object may be defined externally,
     * SIS can not know if the object shall be fully marshalled or not.
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
     * @param  <T>     The compile-time type of the {@code type} argument.
     * @param  context Context (GML version, locale, <i>etc.</i>) of the (un)marshalling process.
     * @param  type    The type of object to be unmarshalled, often as a GeoAPI interface.
     * @param  object  The object to be marshalled.
     * @param  uuid    The unique identifier of the object to be marshalled.
     * @return {@code true} if the marshaller can use the {@code uuidref} attribute
     *         instead than marshalling the given object.
     */
    public <T> boolean canSubstituteByReference(final MarshalContext context, final Class<T> type, final T object, final UUID uuid) {
        return (object instanceof NilObject) || (object instanceof Emptiable && ((Emptiable) object).isEmpty());
    }

    /**
     * Returns {@code true} if the marshaller can use a {@code xlink:href} reference to the given
     * object instead than writing the full XML element. This method is invoked when an object to be
     * marshalled has a {@link XLink} identifier. Because those object may be defined externally,
     * SIS can not know if the object shall be fully marshalled or not.
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
     * @param  <T>     The compile-time type of the {@code type} argument.
     * @param  context Context (GML version, locale, <i>etc.</i>) of the (un)marshalling process.
     * @param  type    The type of object to be unmarshalled, often as a GeoAPI interface.
     * @param  object  The object to be marshalled.
     * @param  link    The reference of the object to be marshalled.
     * @return {@code true} if the marshaller can use the {@code xlink:href} attribute
     *         instead than marshalling the given object.
     */
    public <T> boolean canSubstituteByReference(final MarshalContext context, final Class<T> type, final T object, final XLink link) {
        return (object instanceof NilObject) || (object instanceof Emptiable && ((Emptiable) object).isEmpty());
    }

    /**
     * Returns the {@code <gmx:Anchor>} to use for the given text, or {@code null} if none.
     * Anchors can appear in ISO 19139 documents where we would normally expect a character
     * sequence. For example:
     *
     * <table class="sis">
     * <caption>XML representations of string</caption>
     * <tr>
     *   <th>As {@code <gco:CharacterString>}</th>
     *   <th>As {@code <gmx:Anchor>}</th>
     * </tr><tr>
     * <td>
     *   <pre> &lt;gmd:country&gt;
     *     &lt;gco:CharacterString&gt;France&lt;/gco:CharacterString&gt;
     * &lt;/gmd:country&gt;</pre>
     * </td><td>
     *   <pre> &lt;gmd:country&gt;
     *     &lt;gmx:Anchor xlink:href="SDN:C320:2:FR"&gt;France&lt;/gmx:Anchor&gt;
     * &lt;/gmd:country&gt;</pre>
     * </td></tr>
     * </table>
     *
     * Subclasses can override this method if they can provide a mapping from some text
     * values to anchors.
     *
     * @param  context Context (GML version, locale, <i>etc.</i>) of the (un)marshalling process.
     * @param  value   The value for which an anchor is requested. Often the same instance than {@code text},
     *                 but can also be the {@link java.net.URI} or {@link java.util.Locale} instance for which
     *                 {@code text} is a string representation.
     * @param  text    The textual representation of the value for which to get the anchor.
     * @return The anchor for the given text, or {@code null} if none.
     */
    public XLink anchor(final MarshalContext context, final Object value, final CharSequence text) {
        return (text instanceof Anchor) ? (Anchor) text : null;
    }
}
