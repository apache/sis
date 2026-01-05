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

import java.util.Map;
import java.util.IdentityHashMap;
import java.lang.reflect.Modifier;
import org.apache.sis.math.NumberType;
import org.apache.sis.xml.NilReason;


/**
 * A workaround for attaching properties ({@code nilreason}, {@code href}, <i>etc.</i>) to final classes.
 * The normal approach in SIS is to implement the {@link org.apache.sis.xml.NilObject} interface.
 * However, we cannot do so when the object is a final Java class such as {@link String}.
 * This class provides a workaround using specific instances of those wrappers.
 *
 * <h2>Historical note</h2>
 * In previous Apache SIS releases (before 1.4), this class was used mostly for primitive type wrappers such as
 * {@link Boolean}, {@link Byte}, {@link Short}, {@link Integer}, {@link Long}, {@link Float} and {@link Double}.
 * Support for those types has been removed because it depends on {@code java.lang} constructors now marked as
 * deprecated for removal.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see NilReason#createNilObject(Class)
 * @see <a href="https://issues.apache.org/jira/browse/SIS-586">SIS-586</a>
 */
public final class FinalClassExtensions {
    /**
     * The map where to store specific instances. Keys are instances considered as nil.
     * Values are the {@code NilReason} why the instance is missing,
     * or any other property we may want to attach.
     *
     * <h4>Identity comparisons</h4>
     * We really need an identity hash map; using the {@code Object.equals(Object)} method is not allowed here.
     * This is because "nil values" are real values. For example if the type is {@link String}, then the nil value
     * is a {@code String} instance having the value "". We don't want to consider every empty string value as nil,
     * but only the specific {@code String} instance used as sentinel value for nil.
     *
     * <h4>Weak references</h4>
     * We cannot use weak value references, because we don't want the {@link NilReason} (the map value) to be lost
     * while the sentinel value (the map key) is still in use. We could use weak references for the keys, but JDK 7
     * does not provides any map implementation which is both an {@code IdentityHashMap} and a {@code WeakHashMap}.
     *
     * For now we do not use weak references. This means that if a user creates a custom {@code NilReason} by a call
     * to {@link NilReason#valueOf(String)} and if (s)he uses that nil reason for a final class, then that custom
     * {@code NilReason} instance and its sentinel values will never be garbage-collected.
     * We presume that such cases will be rare enough for not being an issue in practice.
     *
     * <h4>Synchronization</h4>
     * All accesses to this map shall be synchronized on the map object.
     */
    private static final Map<Object,Object> SENTINEL_VALUES = new IdentityHashMap<>();

    /**
     * Do not allow instantiation of this class.
     */
    private FinalClassExtensions() {
    }

    /**
     * Returns {@code true} if the given type is a valid key. This {@code FinalClassExtensions}
     * class is a workaround to be used only for final classes on which we have no control.
     * Non-final classes shall implement {@link org.apache.sis.xml.NilObject} instead.
     * Primitive wrappers are not allowed neither because they will become value objects.
     */
    private static boolean isValidKey(final Class<?> type) {
        return Modifier.isFinal(type.getModifiers()) && !type.isPrimitive() &&
                !NumberType.forClass(type).orElse(NumberType.NULL).isConvertible();
    }

    /**
     * Associates the given property to the given instance.
     * The {@code instance} argument shall be a specific instance created by the {@code new} keyword,
     * not a shared instance like static constants or instances created by static factory methods.
     *
     * @param  instance  the {@link String} (or other final class) specific instance.
     * @param  property  the {@link NilReason} or other property to associate to the given instance.
     */
    public static void associate(final Object instance, final Object property) {
        assert isValidKey(instance.getClass()) : instance;
        synchronized (SENTINEL_VALUES) {
            final Object old = SENTINEL_VALUES.put(instance, property);
            if (old != null) {                          // Should never happen - this is rather debugging check.
                SENTINEL_VALUES.put(instance, old);
                throw new AssertionError(instance);
            }
        }
    }

    /**
     * Returns the property of the given final type, or {@code null} if none.
     *
     * @param  instance  the {@link String} (or other final class) specific instance.
     * @return the property associated to the given instance, or {@code null} if none.
     */
    public static Object property(final Object instance) {
        /*
         * No `assert isValidKey(instance)` because this method is sometimes invoked
         * only after a brief inspection (e.g. `NilReason.mayBeNil(Object)` method).
         */
        synchronized (SENTINEL_VALUES) {
            return SENTINEL_VALUES.get(instance);
        }
    }
}
