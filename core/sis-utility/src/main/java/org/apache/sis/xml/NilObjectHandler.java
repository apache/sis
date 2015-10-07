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
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.lang.reflect.Proxy;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import org.opengis.metadata.Identifier;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.jaxb.IdentifierMapAdapter;
import org.apache.sis.internal.jaxb.ModifiableIdentifierMap;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * The handler for an object where all methods returns null or empty collections, except
 * a few methods related to object identity. This handler is used only when no concrete
 * definition were found for a XML element identified by {@code xlink} or {@code uuidref}
 * attributes.
 *
 * <div class="note"><b>Implementation note:</b>
 * The same handler could be used for every proxy having the same XLink.
 * For now, it doesn't seem worth to cache the handlers.</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final class NilObjectHandler implements InvocationHandler {
    /**
     * The identifiers as an {@link IdentifierMapAdapter} object, or the {@code nilReason}
     * attribute as a {@link NilReason} object. We don't use separated fields because
     * those attributes are exclusive, and some operations like {@code toString()},
     * {@code hashCode()} and {@code equals(Object)} are the same for both types.
     */
    private final Object attribute;

    /**
     * Creates a new handler for an object identified by the given identifiers.
     * The identifiers are wrapped in a mutable list, so users can add, remove
     * or modify identifiers.
     */
    NilObjectHandler(final Identifier[] identifiers) {
        final List<Identifier> asList = new ArrayList<Identifier>(identifiers.length);
        for (final Identifier identifier : identifiers) {
            if (identifier != null) {
                asList.add(identifier);
            }
        }
        attribute = new ModifiableIdentifierMap(asList);
    }

    /**
     * Creates a new handler for an object which is nil for the given reason.
     */
    NilObjectHandler(final NilReason nilReason) {
        attribute = nilReason;
    }

    /**
     * Returns {@code true} if the given type is one of the interfaces ignored by
     * {@link #getInterface(Object)}.
     */
    static boolean isIgnoredInterface(final Class<?> type) {
        return IdentifiedObject.class.isAssignableFrom(type) ||
               NilObject.class.isAssignableFrom(type) ||
               LenientComparable.class.isAssignableFrom(type);
    }

    /**
     * Returns the interface implemented by the given proxy.
     */
    private static Class<?> getInterface(final Object proxy) {
        for (final Class<?> type : proxy.getClass().getInterfaces()) {
            if (!isIgnoredInterface(type)) {
                return type;
            }
        }
        throw new AssertionError(proxy); // Should not happen.
    }

    /**
     * Processes a method invocation. For any invocation of a getter method, there is a choice:
     *
     * <ul>
     *   <li>If the invoked method is {@code getIdentifiers()}, returns the identifiers given at
     *       construction time.</li>
     *   <li>If the invoked method is {@code getIdentifierMap()}, returns a view over the
     *       identifiers given at construction time.</li>
     *   <li>If the invoked method is any other kind of getter, returns null except if:<ul>
     *       <li>the return type is a collection, in which case an empty collection is returned;</li>
     *       <li>the return type is a primitive, in which case the nil value for that primitive
     *           type is returned.</li></ul></li>
     *   <li>If the invoked method is a setter method, throw a {@link UnsupportedOperationException}
     *       since the proxy instance is assumed unmodifiable.</li>
     *   <li>If the invoked method is one of the {@link Object} method, delegate to the
     *       {@link #reference}.</li>
     * </ul>
     */
    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        final String name = method.getName();
        if (args == null) {
            /*switch (name)*/ {
                if ("getNilReason".equals(name)) {
                    return (attribute instanceof NilReason) ? (NilReason) attribute : null;
                }
                if ("getIdentifierMap".equals(name)) {
                    return (attribute instanceof IdentifierMap) ? (IdentifierMap) attribute : null;
                }
                if ("getIdentifiers".equals(name)) {
                    return (attribute instanceof IdentifierMapAdapter) ?
                            ((IdentifierMapAdapter) attribute).identifiers : null;
                }
                if ("toString".equals(name)) {
                    return getInterface(proxy).getSimpleName() + '[' + attribute + ']';
                }
                if ("hashCode".equals(name)) {
                    return ~attribute.hashCode();
                }
            }
            if (name.startsWith("get") || name.startsWith("is")) {
                return Numbers.valueOfNil(method.getReturnType());
            }
        } else switch (args.length) {
            case 1: {
                if (name.equals("equals")) {
                    return equals(proxy, args[0], ComparisonMode.STRICT);
                }
                if (name.startsWith("set")) {
                    throw new UnsupportedOperationException(Errors.format(
                            Errors.Keys.UnmodifiableObject_1, getInterface(proxy)));
                }
                break;
            }
            case 2: {
                if (name.equals("equals")) {
                    return equals(proxy, args[0], (ComparisonMode) args[1]);
                }
                break;
            }
        }
        throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnsupportedOperation_1,
                getInterface(proxy).getSimpleName() + '.' + name));
    }

    /**
     * Compares the given objects to the given level of strictness. The first object shall
     * be the proxy, and the second object an arbitrary implementation. This method returns
     * {@code true} if the given arbitrary implementation contains only null or empty attributes.
     */
    private boolean equals(final Object proxy, final Object other, final ComparisonMode mode) throws Throwable {
        if (other == proxy) return true;
        if (other == null) return false;
        if (proxy.getClass() == other.getClass()) {
            if (mode.isIgnoringMetadata()) {
                return true;
            }
            final NilObjectHandler h = (NilObjectHandler) Proxy.getInvocationHandler(other);
            return attribute.equals(h.attribute);
        }
        switch (mode) {
            case STRICT: return false; // The above test is the only relevant one for this mode.
            case BY_CONTRACT: {
                Object tx = attribute, ox = null;
                if (tx instanceof IdentifierMapAdapter) {
                    tx = ((IdentifierMapAdapter) tx).identifiers;
                    if (other instanceof IdentifiedObject) {
                        ox = ((IdentifiedObject) other).getIdentifiers();
                    }
                } else {
                    if (other instanceof NilObject) {
                        ox = ((NilObject) other).getNilReason();
                    }
                }
                if (!Objects.equals(tx, ox)) {
                    return false;
                }
                break;
            }
        }
        /*
         * Having two objects declaring the same identifiers and implementing the same interface,
         * ensures that all properties in the other objects are null or empty collections.
         */
        final Class<?> type = getInterface(proxy);
        if (!type.isInstance(other)) {
            return false;
        }
        for (final Method getter : type.getMethods()) {
            if (Classes.isPossibleGetter(getter)) {
                final Object value;
                try {
                    value = getter.invoke(other, (Object[]) null);
                } catch (InvocationTargetException e) {
                    throw e.getTargetException();
                }
                if (value != null) {
                    if ((value instanceof Collection<?>) && ((Collection<?>) value).isEmpty()) {
                        continue; // Empty collection, which is consistent with this proxy behavior.
                    }
                    if ((value instanceof Map<?,?>) && ((Map<?,?>) value).isEmpty()) {
                        continue; // Empty collection, which is consistent with this proxy behavior.
                    }
                    return false;
                }
            }
        }
        return true;
    }
}
