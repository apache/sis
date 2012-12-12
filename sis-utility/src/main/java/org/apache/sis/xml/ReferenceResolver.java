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

import java.util.UUID;
import java.lang.reflect.Proxy;
import org.opengis.metadata.Identifier;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.internal.jaxb.UUIDs;

import static org.apache.sis.util.ArgumentChecks.*;


/**
 * Controls the (un)marshaller behavior regarding the {@code xlink} or {@code uuidref} attributes.
 * At marshalling time, this class controls whether the marshaller is allowed to write a reference
 * to an existing instance instead than writing the full object definition.
 * At unmarshalling time, this class replaces (if possible) a reference by the full object definition.
 *
 * <p>Subclasses can override the methods defined in this class in order to search in their
 * own catalog. See the {@link XML#RESOLVER} javadoc for an example of registering a custom
 * {@code ReferenceResolver} to a unmarshaller.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.18)
 * @version 0.3
 * @module
 */
public class ReferenceResolver {
    /**
     * The default and thread-safe instance. This instance is used at unmarshalling time
     * when no {@code ReferenceResolver} was explicitly set by the {@link XML#RESOLVER}
     * property.
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
     *   <li>Implements the {@link IdentifiedObject} interface.</li>
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
     * @param  type    The type of object to be unmarshalled as an <strong>interface</strong>.
     *                 This is usually a <a href="http://www.geoapi.org">GeoAPI</a> interface.
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
     * Returns an object of the given type for the given {@code uuid} attribute, or {@code null}
     * if none. The default implementation looks in an internal map for previously unmarshalled
     * object having the given UUID. If no existing instance is found, then this method returns
     * {@code null}.
     *
     * @param  <T>     The compile-time type of the {@code type} argument.
     * @param  context Context (GML version, locale, <i>etc.</i>) of the (un)marshalling process.
     * @param  type    The type of object to be unmarshalled as an <strong>interface</strong>.
     *                 This is usually a <a href="http://www.geoapi.org">GeoAPI</a> interface.
     * @param  uuid The {@code uuid} attributes.
     * @return An object of the given type for the given {@code uuid} attribute, or {@code null} if none.
     */
    @SuppressWarnings("unchecked")
    public <T> T resolve(final MarshalContext context, final Class<T> type, final UUID uuid) {
        ensureNonNull("type", type);
        ensureNonNull("uuid", uuid);
        final Object object = UUIDs.lookup(uuid);
        return type.isInstance(object) ? (T) object : null;
    }

    /**
     * Returns an object of the given type for the given {@code xlink} attribute, or {@code null}
     * if none. The default implementation returns {@code null} in all cases.
     *
     * @param  <T>     The compile-time type of the {@code type} argument.
     * @param  context Context (GML version, locale, <i>etc.</i>) of the (un)marshalling process.
     * @param  type    The type of object to be unmarshalled as an <strong>interface</strong>.
     *                 This is usually a <a href="http://www.geoapi.org">GeoAPI</a> interface.
     * @param  link The {@code xlink} attributes.
     * @return An object of the given type for the given {@code xlink} attribute, or {@code null} if none.
     */
    public <T> T resolve(final MarshalContext context, final Class<T> type, final XLink link) {
        ensureNonNull("type",  type);
        ensureNonNull("xlink", link);
        return null;
    }

    /**
     * Returns {@code true} if the marshaller can use a {@code xlink:href} reference to the given
     * metadata instead than writing the full element. This method is invoked when a metadata to be
     * marshalled has a {@link XLink} identifier. Because those metadata may be defined externally,
     * SIS can not know if the metadata shall be fully marshalled or not.
     * Such information needs to be provided by the application.
     *
     * <p>The default implementation conservatively returns {@code false} in every cases.
     * Subclasses can override this method if they know whether the receiver will be able
     * to resolve such references.</p>
     *
     * @param  <T>     The compile-time type of the {@code type} argument.
     * @param  context Context (GML version, locale, <i>etc.</i>) of the (un)marshalling process.
     * @param  type    The type of object to be marshalled as an <strong>interface</strong>.
     *                 This is usually a <a href="http://www.geoapi.org">GeoAPI</a> interface.
     * @param  object  The object to be marshalled.
     * @param  link    The reference of the object to be marshalled.
     * @return {@code true} if the marshaller can use the {@code xlink:href} attribute
     *         instead than marshalling the given metadata.
     */
    public <T> boolean canSubstituteByReference(final MarshalContext context, final Class<T> type, final T object, final XLink link) {
        return false;
    }

    /**
     * Returns {@code true} if the marshaller can use a reference to the given metadata
     * instead than writing the full element. This method is invoked when a metadata to
     * be marshalled has a UUID identifier. Because those metadata may be defined externally,
     * SIS can not know if the metadata shall be fully marshalled or not.
     * Such information needs to be provided by the application.
     *
     * <p>The default implementation conservatively returns {@code false} in every cases.
     * Subclasses can override this method if they know whether the receiver will be able
     * to resolve such references.</p>
     *
     * @param  <T>     The compile-time type of the {@code type} argument.
     * @param  context Context (GML version, locale, <i>etc.</i>) of the (un)marshalling process.
     * @param  type    The type of object to be marshalled as an <strong>interface</strong>.
     *                 This is usually a <a href="http://www.geoapi.org">GeoAPI</a> interface.
     * @param  object  The object to be marshalled.
     * @param  uuid    The unique identifier of the object to be marshalled.
     * @return {@code true} if the marshaller can use the {@code uuidref} attribute
     *         instead than marshalling the given metadata.
     */
    public <T> boolean canSubstituteByReference(final MarshalContext context, final Class<T> type, final T object, final UUID uuid) {
        return false;
    }
}
