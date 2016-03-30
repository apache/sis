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
import java.util.Iterator;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import org.apache.sis.util.collection.CheckedContainer;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * Static methods for object comparisons in different ways (deeply, approximatively, <i>etc</i>).
 *
 * @author Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
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
     * Compares the specified objects for equality, ignoring metadata.
     * If this method returns {@code true}, then:
     *
     * <ul>
     *   <li>If the two given objects are
     *       {@linkplain org.apache.sis.referencing.operation.transform.AbstractMathTransform math transforms},
     *       then transforming a set of coordinate values using one transform will produce the same
     *       results than transforming the same coordinates with the other transform.</li>
     *
     *   <li>If the two given objects are
     *       {@linkplain org.apache.sis.referencing.crs.AbstractCRS Coordinate Reference Systems} (CRS), then a call to
     *       <code>{@linkplain org.apache.sis.referencing.CRS#findOperation findOperation}(crs1, crs2, null)</code>
     *       will return an identity operation.</li>
     * </ul>
     *
     * If a more lenient comparison allowing slight differences in numerical values is wanted,
     * then {@link #equalsApproximatively(Object, Object)} can be used instead.
     *
     * <div class="section">Implementation note</div>
     * This is a convenience method for the following method call:
     *
     * {@preformat java
     *     return deepEquals(object1, object2, ComparisonMode.IGNORE_METADATA);
     * }
     *
     * @param  object1 The first object to compare (may be null).
     * @param  object2 The second object to compare (may be null).
     * @return {@code true} if both objects are equal, ignoring metadata.
     *
     * @see #deepEquals(Object, Object, ComparisonMode)
     * @see ComparisonMode#IGNORE_METADATA
     */
    public static boolean equalsIgnoreMetadata(final Object object1, final Object object2) {
        return deepEquals(object1, object2, ComparisonMode.IGNORE_METADATA);
    }

    /**
     * Compares the specified objects for equality, ignoring metadata and slight differences
     * in numerical values. If this method returns {@code true}, then:
     *
     * <ul>
     *   <li>If the two given objects are
     *       {@linkplain org.apache.sis.referencing.operation.transform.AbstractMathTransform math transforms},
     *       then transforming a set of coordinate values using one transform will produce <em>approximatively</em>
     *       the same results than transforming the same coordinates with the other transform.</li>
     *
     *   <li>If the two given objects are
     *       {@linkplain org.apache.sis.referencing.crs.AbstractCRS Coordinate Reference Systems} (CRS), then a call to
     *       <code>{@linkplain org.apache.sis.referencing.CRS#findOperation findOperation}(crs1, crs2, null)</code>
     *       will return an operation close to identity.</li>
     * </ul>
     *
     * <div class="section">Implementation note</div>
     * This is a convenience method for the following method call:
     *
     * {@preformat java
     *     return deepEquals(object1, object2, ComparisonMode.APPROXIMATIVE);
     * }
     *
     * @param  object1 The first object to compare (may be null).
     * @param  object2 The second object to compare (may be null).
     * @return {@code true} if both objects are approximatively equal.
     *
     * @see #deepEquals(Object, Object, ComparisonMode)
     * @see ComparisonMode#APPROXIMATIVE
     */
    public static boolean equalsApproximatively(final Object object1, final Object object2) {
        return deepEquals(object1, object2, ComparisonMode.APPROXIMATIVE);
    }

    /**
     * Convenience method for testing two objects for equality using the given level of strictness.
     * If at least one of the given objects implement the {@link LenientComparable} interface, then
     * the comparison is performed using the {@link LenientComparable#equals(Object, ComparisonMode)}
     * method. Otherwise this method performs the same work than the
     * {@link Objects#deepEquals(Object, Object)} convenience method.
     *
     * <p>If both arguments are arrays or collections, then the elements are compared recursively.</p>
     *
     * @param  object1 The first object to compare, or {@code null}.
     * @param  object2 The second object to compare, or {@code null}.
     * @param  mode    The strictness level of the comparison.
     * @return {@code true} if both objects are equal for the given level of strictness.
     *
     * @see #equalsIgnoreMetadata(Object, Object)
     * @see #equalsApproximatively(Object, Object)
     */
    public static boolean deepEquals(final Object object1, final Object object2, final ComparisonMode mode) {
        if (object1 == object2) {
            return true;
        }
        if (object1 == null || object2 == null) {
            assert isNotDebug(mode) : "object" + (object1 == null ? '1' : '2') + " is null";
            return false;
        }
        if (object1 instanceof LenientComparable) {
            return ((LenientComparable) object1).equals(object2, mode);
        }
        if (object2 instanceof LenientComparable) {
            return ((LenientComparable) object2).equals(object1, mode);
        }
        if (object1 instanceof Map.Entry<?,?>) {
            if (object2 instanceof Map.Entry<?,?>) {
                final Map.Entry<?,?> e1 = (Map.Entry<?,?>) object1;
                final Map.Entry<?,?> e2 = (Map.Entry<?,?>) object2;
                return deepEquals(e1.getKey(),   e2.getKey(),   mode) &&
                       deepEquals(e1.getValue(), e2.getValue(), mode);
            }
            assert isNotDebug(mode) : mismatchedType(Map.Entry.class, object2);
            return false;
        }
        if (object1 instanceof Map<?,?>) {
            if (object2 instanceof Map<?,?>) {
                return equals(((Map<?,?>) object1).entrySet(),
                              ((Map<?,?>) object2).entrySet(), mode);
            }
            assert isNotDebug(mode) : mismatchedType(Map.class, object2);
            return false;
        }
        if (object1 instanceof Collection<?>) {
            if (object2 instanceof Collection<?>) {
                return equals((Collection<?>) object1,
                              (Collection<?>) object2, mode);
            }
            assert isNotDebug(mode) : mismatchedType(Collection.class, object2);
            return false;
        }
        if (object1 instanceof Object[]) {
            if (!(object2 instanceof Object[])) {
                assert isNotDebug(mode) : mismatchedType(Object[].class, object2);
                return false;
            }
            final Object[] array1 = (Object[]) object1;
            final Object[] array2 = (Object[]) object2;
            if (array1 instanceof LenientComparable[]) {
                return equals((LenientComparable[]) array1, array2, mode);
            }
            if (array2 instanceof LenientComparable[]) {
                return equals((LenientComparable[]) array2, array1, mode);
            }
            final int length = array1.length;
            if (array2.length != length) {
                assert isNotDebug(mode) : "Length " + length + " != " + array2.length;
                return false;
            }
            for (int i=0; i<length; i++) {
                if (!deepEquals(array1[i], array2[i], mode)) {
                    assert isNotDebug(mode) : "object[" + i + "] not equal";
                    return false;
                }
            }
            return true;
        }
        return Objects.deepEquals(object1, object2);
    }

    /**
     * Compares two arrays where at least one array is known to contain {@link LenientComparable}
     * elements. This knowledge avoid the need to test each element individually. The two arrays
     * shall be non-null.
     */
    private static boolean equals(final LenientComparable[] array1, final Object[] array2, final ComparisonMode mode) {
        final int length = array1.length;
        if (array2.length != length) {
            return false;
        }
        for (int i=0; i<length; i++) {
            final LenientComparable e1 = array1[i];
            final Object e2 = array2[i];
            if (e1 != e2 && (e1 == null || !e1.equals(e2, mode))) {
                assert isNotDebug(mode) : "object[" + i + "] not equal";
                return false;
            }
        }
        return true;
    }

    /**
     * Compares two collections. Order are significant, unless both collections implement the
     * {@link Set} interface.
     */
    private static boolean equals(final Iterable<?> object1, final Iterable<?> object2, final ComparisonMode mode) {
        final Iterator<?> it1 = object1.iterator();
        final Iterator<?> it2 = object2.iterator();
        while (it1.hasNext()) {
            if (!it2.hasNext()) {
                assert isNotDebug(mode) : mismatchedElement("Iterable", object1, object2, "size");
                return false;
            }
            Object element1 = it1.next();
            Object element2 = it2.next();
            if (deepEquals(element1, element2, mode)) {
                continue;
            }
            if (!(object1 instanceof Set<?> && object2 instanceof Set<?>)) {
                assert isNotDebug(mode) : mismatchedElement("Iterable", object1, object2, "element");
                return false;
            }
            /*
             * We have found an element which is not equals. However in the particular
             * case of Set, the element order is not significant. So we need to perform
             * a more costly check ensuring that the collections are still different if
             * ignoring order. Note that the test will ignore the elements successfuly
             * compared up to this point.
             */
            // Creates a copy of REMAINING elements in the first collection.
            final LinkedList<Object> copy = new LinkedList<Object>();
            copy.add(element1);
            while (it1.hasNext()) {
                copy.add(it1.next());
            }
            // Removes from the above copy all REMAINING elements from the second collection.
            while (true) {
                final Iterator<?> it = copy.iterator();
                do if (!it.hasNext()) {
                    assert isNotDebug(mode) : mismatchedElement("Set", object1, object2, "element");
                    return false; // An element has not been found.
                } while (!deepEquals(it.next(), element2, mode));
                it.remove();
                if (!it2.hasNext()) {
                    break;
                }
                element2 = it2.next();
            }
            return copy.isEmpty();
        }
        return !it2.hasNext();
    }

    /**
     * Returns {@code true} if the given mode is not {@link ComparisonMode#DEBUG}. In debug mode,
     * the expected behavior of {@link #deepEquals(Object, Object, ComparisonMode)} is to thrown
     * an exception (rather then returning {@code false}) when two objects are not equal.
     */
    private static boolean isNotDebug(final ComparisonMode mode) {
        return mode != ComparisonMode.DEBUG;
    }

    /**
     * Returns an assertion error message for mismatched types.
     *
     * @param  expected The expected type.
     * @param  actual The actual object (not its type).
     * @return The error message to use in assertions.
     */
    private static String mismatchedType(final Class<?> expected, final Object actual) {
        return "Expected " + expected + " but got " + actual.getClass();
    }

    /**
     * Returns an assertion error message for mismatched collections.
     */
    private static String mismatchedElement(final String header, final Iterable<?> object1, final Iterable<?> object2, final String tail) {
        Class<?> type = null;
        if (object1 instanceof CheckedContainer<?>) {
            type = ((CheckedContainer<?>) object1).getElementType();
        }
        if (type == null && object2 instanceof CheckedContainer<?>) {
            type = ((CheckedContainer<?>) object2).getElementType();
        }
        return header + '<' + (type != null ? type.getSimpleName() : "?") + ">: " + tail + " not equals.";
    }

    /**
     * Returns a hash code for the specified object, which may be an array.
     * This method returns one of the following values:
     *
     * <ul>
     *   <li>If the supplied object is {@code null}, then this method returns 0.</li>
     *   <li>Otherwise if the object is an array of objects, then
     *       {@link Arrays#deepHashCode(Object[])} is invoked.</li>
     *   <li>Otherwise if the object is an array of primitive type, then the corresponding
     *       {@link Arrays#hashCode(double[]) Arrays.hashCode(...)} method is invoked.</li>
     *   <li>Otherwise {@link Object#hashCode()} is invoked.</li>
     * </ul>
     *
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
     *
     * <ul>
     *   <li>If the object is an array of objects, then
     *       {@link Arrays#deepToString(Object[])} is invoked.</li>
     *   <li>Otherwise if the object is an array of primitive type, then the corresponding
     *       {@link Arrays#toString(double[]) Arrays.toString(...)} method is invoked.</li>
     *   <li>Otherwise {@link String#valueOf(Object)} is invoked.</li>
     * </ul>
     *
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
