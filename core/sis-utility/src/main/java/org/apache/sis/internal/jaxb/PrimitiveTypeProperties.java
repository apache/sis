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

import java.lang.reflect.Modifier;
import org.apache.sis.xml.NilReason;
import org.apache.sis.util.collection.WeakValueHashMap;


/**
 * A workaround for attaching properties ({@code nilreason}, {@code href}, <i>etc.</i>) to primitive type wrappers.
 * The normal approach in SIS is to implement the {@link org.apache.sis.xml.NilObject} interface. However we can not
 * do so when the object is a final Java class like {@link Boolean}, {@link Integer}, {@link Double} or {@link String}.
 * This class provides a workaround using specific instances of some primitive types.
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
     * The map where to store specific instances. We really need an identity hash map;
     * using the {@code Object.equals(Object)} method is not allowed here.
     *
     * <p>Keys are the primitive type instances. Values are the {@code NilReason} why this value is missing,
     * or any other property we may want to attach.</p>
     */
    private static final WeakValueHashMap<Object,Object> SENTINAL_VALUES = new WeakValueHashMap<>(Object.class, true);

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
        final Object old = SENTINAL_VALUES.put(primitive, property);
        if (old != null) { // Should never happen - this is rather debugging check.
            SENTINAL_VALUES.put(primitive, old);
            throw new AssertionError(primitive);
        }
    }

    /**
     * Returns the property of the given primitive type, or {@code null} if none.
     *
     * @param  primitive The {@link Boolean}, {@link Integer}, {@link Double} or {@link String} specific instance.
     * @return The property associated to the given instance, or {@code null} if none.
     */
    public static Object property(final Object primitive) {
        assert isValidKey(primitive) : primitive;
        return SENTINAL_VALUES.get(primitive);
    }
}
