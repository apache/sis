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
package org.apache.sis.util;

import java.util.Arrays;


/**
 * Miscellaneous static methods.
 *
 * @author Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-1.2)
 * @version 0.3
 * @module
 */
public final class Utilities extends Static {
    /**
     * Do not allow object creation.
     */
    private Utilities() {
    }

    /**
     * Returns {@code true} if the given floats are equals. Positive and negative zero are
     * considered different, while a {@link Float#NaN NaN} value is considered equal to all
     * other NaN values.
     *
     * @param o1 The first value to compare.
     * @param o2 The second value to compare.
     * @return {@code true} if both values are equal.
     *
     * @see Float#equals(Object)
     */
    public static boolean equals(final float o1, final float o2) {
        return Float.floatToIntBits(o1) == Float.floatToIntBits(o2);
    }

    /**
     * Returns {@code true} if the given doubles are equals. Positive and negative zero are
     * considered different, while a {@link Double#NaN NaN} value is considered equal to all
     * other NaN values.
     *
     * @param o1 The first value to compare.
     * @param o2 The second value to compare.
     * @return {@code true} if both values are equal.
     *
     * @see Double#equals(Object)
     */
    public static boolean equals(final double o1, final double o2) {
        return Double.doubleToLongBits(o1) == Double.doubleToLongBits(o2);
    }

    /**
     * Returns a hash code for the specified object, which may be an array.
     * This method returns one of the following values:
     * <p>
     * <ul>
     *   <li>If the supplied object is {@code null}, then this method returns 0.</li>
     *   <li>Otherwise if the object is an array of objects, then
     *       {@link Arrays#deepHashCode(Object[])} is invoked.</li>
     *   <li>Otherwise if the object is an array of primitive type, then the corresponding
     *       {@link Arrays#hashCode(double[]) Arrays.hashCode(...)} method is invoked.</li>
     *   <li>Otherwise {@link Object#hashCode()} is invoked.<li>
     * </ul>
     * <p>
     * This method should be invoked <strong>only</strong> if the object type is declared
     * exactly as {@code Object}, not as some subtype like {@code Object[]}, {@code String} or
     * {@code float[]}. In the later cases, use the appropriate {@link Arrays} method instead.
     *
     * @param object The object to compute hash code. May be {@code null}.
     * @return The hash code of the given object.
     */
    public static int deepHashCode(final Object object) {
        if (object == null) {
            return 0;
        }
        if (object instanceof Object[])  return Arrays.deepHashCode((Object[])  object);
        if (object instanceof double[])  return Arrays.hashCode    ((double[])  object);
        if (object instanceof float[])   return Arrays.hashCode    ((float[])   object);
        if (object instanceof long[])    return Arrays.hashCode    ((long[])    object);
        if (object instanceof int[])     return Arrays.hashCode    ((int[])     object);
        if (object instanceof short[])   return Arrays.hashCode    ((short[])   object);
        if (object instanceof byte[])    return Arrays.hashCode    ((byte[])    object);
        if (object instanceof char[])    return Arrays.hashCode    ((char[])    object);
        if (object instanceof boolean[]) return Arrays.hashCode    ((boolean[]) object);
        return object.hashCode();
    }

    /**
     * Returns a string representation of the specified object, which may be an array.
     * This method returns one of the following values:
     * <p>
     * <ul>
     *   <li>If the object is an array of objects, then
     *       {@link Arrays#deepToString(Object[])} is invoked.</li>
     *   <li>Otherwise if the object is an array of primitive type, then the corresponding
     *       {@link Arrays#toString(double[]) Arrays.toString(...)} method is invoked.</li>
     *   <li>Otherwise {@link String#valueOf(Object)} is invoked.</li>
     * </ul>
     * <p>
     * This method should be invoked <strong>only</strong> if the object type is declared
     * exactly as {@code Object}, not as some subtype like {@code Object[]}, {@code Number} or
     * {@code float[]}. In the later cases, use the appropriate {@link Arrays} method instead.
     *
     * @param object The object to format as a string. May be {@code null}.
     * @return A string representation of the given object.
     */
    public static String deepToString(final Object object) {
        if (object instanceof Object[])  return Arrays.deepToString((Object[]) object);
        if (object instanceof double[])  return Arrays.toString    ((double[]) object);
        if (object instanceof float[])   return Arrays.toString    ((float[]) object);
        if (object instanceof long[])    return Arrays.toString    ((long[]) object);
        if (object instanceof int[])     return Arrays.toString    ((int[]) object);
        if (object instanceof short[])   return Arrays.toString    ((short[]) object);
        if (object instanceof byte[])    return Arrays.toString    ((byte[]) object);
        if (object instanceof char[])    return Arrays.toString    ((char[]) object);
        if (object instanceof boolean[]) return Arrays.toString    ((boolean[]) object);
        return String.valueOf(object);
    }
}
