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
import java.util.IdentityHashMap;
import java.lang.reflect.Modifier;
import org.apache.sis.xml.NilReason;


/**
 * A workaround for attaching properties ({@code nilreason}, {@code href}, <i>etc.</i>) to primitive type wrappers.
 * The normal approach in SIS is to implement the {@link org.apache.sis.xml.NilObject} interface. However we can not
 * do so when the object is a final Java class like {@link Boolean}, {@link Integer}, {@link Double} or {@link String}.
 * This class provides a workaround using specific instances of those wrappers.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 *
 * @see NilReason#createNilObject(Class)
 */
public final class PrimitiveTypeProperties {
    /**
     * The map where to store specific instances. Keys are instances of the primitive wrappers considered as nil.
     * Values are the {@code NilReason} why the primitive is missing, or any other property we may want to attach.
     *
     * <div class="section">Identity comparisons</div>
     * We really need an identity hash map; using the {@code Object.equals(Object)} method is not allowed here.
     * This is because "nil values" are real values. For example if the type is {@link Integer}, then the nil value
     * is an {@code Integer} instance having the value 0. We don't want to consider every 0 integer value as nil,
     * but only the specific {@code Integer} instance used as sentinel value for nil.
     *
     * <div class="section">Weak references</div>
     * We can not use weak value references, because we don't want the {@link NilReason} (the map value) to be lost
     * while the sentinel value (the map key) is still in use. We could use weak references for the keys, but JDK 7
     * does not provides any map implementation which is both an {@code IdentityHashMap} and a {@code WeakHashMap}.
     *
     * For now we do not use weak references. This means that if a user creates a custom {@code NilReason} by a call
     * to {@link NilReason#valueOf(String)}, and if he uses that nil reason for a primitive type, then that custom
     * {@code NilReason} instance and its sentinel values will never be garbage-collected.
     * We presume that such cases will be rare enough for not being an issue in practice.
     *
     * <div class="section">Synchronization</div>
     * All accesses to this map shall be synchronized on the map object.
     */
    private static final Map<Object,Object> SENTINEL_VALUES = new IdentityHashMap<Object,Object>();

    /**
     * Do not allow instantiation of this class.
     */
    private PrimitiveTypeProperties() {
    }

    /**
     * Returns {@code true} if the given type is a valid key. This {@code PrimitiveTypeProperties}
     * class is a workaround to be used only for final classes on which we have no control.
     * Non-final classes shall implement {@link org.apache.sis.xml.NilObject} instead.
     */
    private static boolean isValidKey(final Object primitive) {
        return Modifier.isFinal(primitive.getClass().getModifiers());
    }

    /**
     * Associates the given property to the given primitive.
     * The {@code primitive} argument shall be a specific instance created by the {@code new} keyword, not
     * a shared instance link {@link Boolean#FALSE} or the values returned by {@link Integer#valueOf(int)}.
     *
     * @param primitive The {@link Boolean}, {@link Integer}, {@link Double} or {@link String} specific instance.
     * @param property  The {@link NilReason} or other property to associate to the given instance.
     */
    public static void associate(final Object primitive, final Object property) {
        assert isValidKey(primitive) : primitive;
        synchronized (SENTINEL_VALUES) {
            final Object old = SENTINEL_VALUES.put(primitive, property);
            if (old != null) { // Should never happen - this is rather debugging check.
                SENTINEL_VALUES.put(primitive, old);
                throw new AssertionError(primitive);
            }
        }
    }

    /**
     * Returns the property of the given primitive type, or {@code null} if none.
     *
     * @param  primitive The {@link Boolean}, {@link Integer}, {@link Double} or {@link String} specific instance.
     * @return The property associated to the given instance, or {@code null} if none.
     */
    public static Object property(final Object primitive) {
        // No 'assert isValidKey(primitive)' because this method is sometime invoked
        // only after a brief inspection (e.g. 'NilReason.mayBeNil(Object)' method).
        synchronized (SENTINEL_VALUES) {
            return SENTINEL_VALUES.get(primitive);
        }
    }
}
