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
package org.apache.sis.internal.jdk7;

import java.util.Arrays;


/**
 * Place holder for {@link java.util.Objects}.
 * This class exists only on the JDK6 branch of SIS.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from GeoAPI)
 * @version 0.3
 * @module
 */
public final class Objects {
    /**
     * Do not allow instantiation of this class.
     */
    private Objects() {
    }

    /**
     * See JDK7 javadoc.
     *
     * @param  <T> The type of the value to check.
     * @param  value Reference to check against null value.
     * @return The given {@code value}, guaranteed to be non null.
     */
    public static <T> T requireNonNull(final T value) {
        if (value == null) {
            throw new NullPointerException();
        }
        return value;
    }

    /**
     * See JDK7 javadoc.
     *
     * @param  <T> The type of the value to check.
     * @param  value Reference to check against null value.
     * @param  message Exception message.
     * @return The given {@code value}, guaranteed to be non null.
     */
    public static <T> T requireNonNull(final T value, final String message) {
        if (value == null) {
            throw new NullPointerException(message);
        }
        return value;
    }

    /**
     * Convenience method for testing two objects for equality. One or both objects may be null.
     * This method do <strong>not</strong> iterates recursively in array elements. If array needs
     * to be compared, use one of {@link Arrays} method or {@link #deepEquals deepEquals} instead.
     * <p>
     * <b>Note on assertions:</b> There is no way to ensure at compile time that this method
     * is not invoked with array arguments, while doing so would usually be a program error.
     * Performing a systematic argument check would impose a useless overhead for correctly
     * implemented {@link Object#equals} methods. As a compromise we perform this check at runtime
     * only if assertions are enabled. Using assertions for argument check in a public API is
     * usually a deprecated practice, but this particular method is for internal use only.
     * <p>
     * <strong>WARNING: This method will be removed when SIS will switch to JDK7.
     * This method will be replaced by the new {@code java.util.Objects.equals} method.
     * Developers who are already on JDK7 should use that JDK method instead.</strong>
     *
     * @param  o1 First object to compare.
     * @param  o2 Second object to compare.
     * @return {@code true} if both objects are equal.
     * @throws AssertionError If assertions are enabled and at least one argument is an array.
     */
    public static boolean equals(final Object o1, final Object o2) {
        assert o1 == null || !o1.getClass().isArray() : o1;
        assert o2 == null || !o2.getClass().isArray() : o2;
        return (o1 == o2) || (o1 != null && o1.equals(o2));
    }

    /**
     * Convenience method for testing two objects for equality. One or both objects may be null.
     * If both are non-null and are arrays, then every array elements will be compared.
     * <p>
     * This method may be useful when the objects may or may not be array. If they are known
     * to be arrays, consider using {@link Arrays#deepEquals(Object[],Object[])} or one of its
     * primitive counter-part instead.
     * <p>
     * <strong>Rules for choosing an {@code equals} or {@code deepEquals} method</strong>
     * <ul>
     *   <li>If <em>both</em> objects are declared as {@code Object[]} (not anything else like
     *   {@code String[]}), consider using {@link Arrays#deepEquals(Object[],Object[])} except
     *   if it is known that the array elements can never be other arrays.</li>
     *
     *   <li>Otherwise if both objects are arrays (e.g. {@code Expression[]}, {@code String[]},
     *   {@code int[]}, <i>etc.</i>), use {@link Arrays#equals(Object[],Object[])}. This
     *   rule is applicable to arrays of primitive type too, since {@code Arrays.equals} is
     *   overridden with primitive counter-parts.</li>
     *
     *   <li>Otherwise if at least one object is anything else than {@code Object} (e.g.
     *   {@code String}, {@code Expression}, <i>etc.</i>), use {@link #equals(Object,Object)}.
     *   Using this {@code deepEquals} method would be an overkill since there is no chance that
     *   {@code String} or {@code Expression} could be an array.</li>
     *
     *   <li>Otherwise if <em>both</em> objects are declared exactly as {@code Object} type and
     *   it is known that they could be arrays, only then invoke this {@code deepEquals} method.
     *   In such case, make sure that the hash code is computed using {@link #deepHashCode(Object)}
     *   for consistency.</li>
     * </ul>
     * <p>
     * <strong>WARNING: This method will be removed when SIS will switch to JDK7.
     * This method will be replaced by the new {@code java.util.Objects.deepEquals} method.
     * Developers who are already on JDK7 should use that JDK method instead.</strong>
     *
     * @param  object1 The first object to compare, or {@code null}.
     * @param  object2 The second object to compare, or {@code null}.
     * @return {@code true} if both objects are equal.
     */
    public static boolean deepEquals(final Object object1, final Object object2) {
        if (object1 == object2) {
            return true;
        }
        if (object1 == null || object2 == null) {
            return false;
        }
        if (object1 instanceof Object[]) {
            return (object2 instanceof Object[]) &&
                    Arrays.deepEquals((Object[]) object1, (Object[]) object2);
        }
        if (object1 instanceof double[]) {
            return (object2 instanceof double[]) &&
                    Arrays.equals((double[]) object1, (double[]) object2);
        }
        if (object1 instanceof float[]) {
            return (object2 instanceof float[]) &&
                    Arrays.equals((float[]) object1, (float[]) object2);
        }
        if (object1 instanceof long[]) {
            return (object2 instanceof long[]) &&
                    Arrays.equals((long[]) object1, (long[]) object2);
        }
        if (object1 instanceof int[]) {
            return (object2 instanceof int[]) &&
                    Arrays.equals((int[]) object1, (int[]) object2);
        }
        if (object1 instanceof short[]) {
            return (object2 instanceof short[]) &&
                    Arrays.equals((short[]) object1, (short[]) object2);
        }
        if (object1 instanceof byte[]) {
            return (object2 instanceof byte[]) &&
                    Arrays.equals((byte[]) object1, (byte[]) object2);
        }
        if (object1 instanceof char[]) {
            return (object2 instanceof char[]) &&
                    Arrays.equals((char[]) object1, (char[]) object2);
        }
        if (object1 instanceof boolean[]) {
            return (object2 instanceof boolean[]) &&
                    Arrays.equals((boolean[]) object1, (boolean[]) object2);
        }
        return object1.equals(object2);
    }

    /**
     * Delegates to {@link Arrays#hashCode(Object[])}.
     *
     * @param  values The object for which to compute hash code values.
     * @return The hash code value for the given objects.
     */
    public static int hash(Object... values) {
        return Arrays.hashCode(values);
    }

    /**
     * Returns the hash code of the given object, or 0 if the object is {@code null}.
     * If non-null, then the given object is not allowed to be an array.
     *
     * @param  object The object for which to compute the hash code.
     * @return The hash code of the given object.
     */
    public static int hashCode(final Object object) {
        if (object == null) {
            return 0;
        }
        assert !object.getClass().isArray() : object;
        return object.hashCode();
    }
}
