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
import java.util.Objects;
import java.util.Comparator;
import java.lang.reflect.Array;


/**
 * Static methods for simple operations on arrays and array elements.
 * This is an extension to the standard {@link Arrays} utility class.
 * Some worthy methods are:
 *
 * <ul>
 *   <li>The {@link #resize(Object[], int) resize} methods, which are very similar to the
 *       {@link Arrays#copyOf(Object[], int) Arrays.copyOf(…)} methods except that they accept
 *       {@code null} arrays and do not copy anything if the given array already has the
 *       requested length.</li>
 *   <li>The {@link #insert(Object[], int, Object[], int, int) insert} and {@link #remove(Object[],
 *       int, int) remove} methods for adding and removing elements in the middle of an array.</li>
 *   <li>The {@link #isSorted(Object[], Comparator, boolean) isSorted} methods for verifying
 *       if an array is sorted, strictly or not.</li>
 * </ul>
 *
 * <h2>Handling of null values</h2>
 * Many (but not all) methods in this class are tolerant to null parameter values,
 * sometimes under certain conditions. See the method javadoc for details.
 *
 * <p>All methods in this class are tolerant to null elements in arrays.
 * Null and {@linkplain Double#NaN NaN} elements are ignored.</p>
 *
 * <h2>Performance consideration</h2>
 * The methods listed below are provided as convenience for <strong>casual</strong> use on
 * <strong>small</strong> arrays. For large arrays or for frequent use, consider using the
 * Java collection framework instead.
 *
 * <table class="sis">
 * <caption>Convenience methods for casual use on small arrays</caption>
 * <tr><th>Method</th>                                             <th class="sep">Alternative</th></tr>
 * <tr><td>{@link #resize(Object[], int)}</td>                     <td class="sep">{@link java.util.ArrayList}</td></tr>
 * <tr><td>{@link #append(Object[], Object)}</td>                  <td class="sep">{@link java.util.ArrayList}</td></tr>
 * <tr><td>{@link #insert(Object[], int, Object[], int, int)}</td> <td class="sep">{@link java.util.LinkedList}</td></tr>
 * <tr><td>{@link #remove(Object[], int, int)}</td>                <td class="sep">{@link java.util.LinkedList}</td></tr>
 * <tr><td>{@link #intersects(Object[], Object[])}</td>            <td class="sep">{@link java.util.HashSet}</td></tr>
 * <tr><td>{@link #contains(Object[], Object)}</td>                <td class="sep">{@link java.util.HashSet}</td></tr>
 * <tr><td>{@link #containsIdentity(Object[], Object)}</td>        <td class="sep">{@link java.util.IdentityHashMap}</td></tr>
 * </table>
 *
 * Note that this recommendation applies mostly to arrays of objects. It may not apply to arrays
 * of primitive types, since as of JDK7 the collection framework wraps every primitive types in
 * objects.
 *
 * @author Martin Desruisseaux (IRD, Geomatys)
 * @version 1.5
 *
 * @see Arrays
 *
 * @since 0.3
 */
@SuppressWarnings("ReturnOfCollectionOrArrayField")     // Array constants in this class are immutable empty arrays.
public final class ArraysExt extends Static {
    /**
     * An empty array of {@code double} primitive type.
     * Such arrays are immutable and can be safely shared.
     */
    public static final double[] EMPTY_DOUBLE = new double[0];

    /**
     * An empty array of {@code float} primitive type.
     * Such arrays are immutable and can be safely shared.
     */
    public static final float[] EMPTY_FLOAT = new float[0];

    /**
     * An empty array of {@code long} primitive type.
     * Such arrays are immutable and can be safely shared.
     */
    public static final long[] EMPTY_LONG = new long[0];

    /**
     * An empty array of {@code int} primitive type.
     * Such arrays are immutable and can be safely shared.
     */
    public static final int[] EMPTY_INT = new int[0];

    /**
     * An empty array of {@code short} primitive type.
     * Such arrays are immutable and can be safely shared.
     */
    public static final short[] EMPTY_SHORT = new short[0];

    /**
     * An empty array of {@code byte} primitive type.
     * Such arrays are immutable and can be safely shared.
     */
    public static final byte[] EMPTY_BYTE = new byte[0];

    /**
     * An empty array of {@code char} primitive type.
     * Such arrays are immutable and can be safely shared.
     */
    public static final char[] EMPTY_CHAR = new char[0];

    /**
     * An empty array of {@code boolean} primitive type.
     * Such arrays are immutable and can be safely shared.
     */
    public static final boolean[] EMPTY_BOOLEAN = new boolean[0];

    /**
     * Do not allow instantiation of this class.
     */
    private ArraysExt() {
    }

    /**
     * Returns an array containing the same elements as the given {@code array} but with the
     * specified {@code length}, truncating or padding with {@code null} if necessary.
     *
     * <ul>
     *   <li>If the given {@code length} is longer than the length of the given {@code array},
     *       then the returned array will contain all the elements of {@code array} at index
     *       <var>i</var> {@literal <} {@code array.length}. Elements at index
     *       <var>i</var> {@literal >=} {@code array.length} are initialized to {@code null}.</li>
     *
     *   <li>If the given {@code length} is shorter than the length of the given {@code array},
     *       then the returned array will contain only the elements of {@code array} at index
     *       <var>i</var> {@literal <} {@code length}. Remaining elements are not copied.</li>
     *
     *   <li>If the given {@code length} is equal to the length of the given {@code array},
     *       then {@code array} is returned unchanged. <strong>No copy</strong> is performed.
     *       This behavior is different than the {@link Arrays#copyOf(Object[], int)} one.</li>
     * </ul>
     *
     * Note that if the given array is {@code null}, then this method unconditionally returns
     * {@code null} no matter the value of the {@code length} argument.
     *
     * @param  <E>     the array elements.
     * @param  array   array to resize, or {@code null}.
     * @param  length  length of the desired array.
     * @return a new array of the requested length, or {@code array} if the given
     *         array is {@code null} or already have the requested length.
     * @throws NegativeArraySizeException if {@code length} is negative.
     *
     * @see Arrays#copyOf(Object[], int)
     */
    public static <E> E[] resize(final E[] array, final int length) {
        return (array == null || array.length == length) ? array : Arrays.copyOf(array, length);
    }

    /**
     * Returns an array containing the same elements as the given {@code array} but
     * specified {@code length}, truncating or padding with zeros if necessary.
     * This method returns {@code null} if and only if the given array is {@code null},
     * in which case the value of the {@code length} argument is ignored.
     *
     * @param  array   array to resize, or {@code null}.
     * @param  length  length of the desired array.
     * @return a new array of the requested length, or {@code array} if the given
     *         array is {@code null} or already have the requested length.
     * @throws NegativeArraySizeException if {@code length} is negative.
     *
     * @see Arrays#copyOf(double[], int)
     */
    public static double[] resize(final double[] array, final int length) {
        if (array != null) {
            if (length == 0) {
                return EMPTY_DOUBLE;
            }
            if (array.length != length) {
                return Arrays.copyOf(array, length);
            }
        }
        return array;
    }

    /**
     * Returns an array containing the same elements as the given {@code array} but
     * specified {@code length}, truncating or padding with zeros if necessary.
     * This method returns {@code null} if and only if the given array is {@code null},
     * in which case the value of the {@code length} argument is ignored.
     *
     * @param  array   array to resize, or {@code null}.
     * @param  length  length of the desired array.
     * @return a new array of the requested length, or {@code array} if the given
     *         array is {@code null} or already have the requested length.
     * @throws NegativeArraySizeException if {@code length} is negative.
     *
     * @see Arrays#copyOf(float[], int)
     */
    public static float[] resize(final float[] array, final int length) {
        if (array != null) {
            if (length == 0) {
                return EMPTY_FLOAT;
            }
            if (array.length != length) {
                return Arrays.copyOf(array, length);
            }
        }
        return array;
    }

    /**
     * Returns an array containing the same elements as the given {@code array} but
     * specified {@code length}, truncating or padding with zeros if necessary.
     * This method returns {@code null} if and only if the given array is {@code null},
     * in which case the value of the {@code length} argument is ignored.
     *
     * @param  array   array to resize, or {@code null}.
     * @param  length  length of the desired array.
     * @return a new array of the requested length, or {@code array} if the given
     *         array is {@code null} or already have the requested length.
     * @throws NegativeArraySizeException if {@code length} is negative.
     *
     * @see Arrays#copyOf(long[], int)
     */
    public static long[] resize(final long[] array, final int length) {
        if (array != null) {
            if (length == 0) {
                return EMPTY_LONG;
            }
            if (array.length != length) {
                return Arrays.copyOf(array, length);
            }
        }
        return array;
    }

    /**
     * Returns an array containing the same elements as the given {@code array} but
     * specified {@code length}, truncating or padding with zeros if necessary.
     * This method returns {@code null} if and only if the given array is {@code null},
     * in which case the value of the {@code length} argument is ignored.
     *
     * @param  array   array to resize, or {@code null}.
     * @param  length  length of the desired array.
     * @return a new array of the requested length, or {@code array} if the given
     *         array is {@code null} or already have the requested length.
     * @throws NegativeArraySizeException if {@code length} is negative.
     *
     * @see Arrays#copyOf(int[], int)
     */
    public static int[] resize(final int[] array, final int length) {
        if (array != null) {
            if (length == 0) {
                return EMPTY_INT;
            }
            if (array.length != length) {
                return Arrays.copyOf(array, length);
            }
        }
        return array;
    }

    /**
     * Returns an array containing the same elements as the given {@code array} but
     * specified {@code length}, truncating or padding with zeros if necessary.
     * This method returns {@code null} if and only if the given array is {@code null},
     * in which case the value of the {@code length} argument is ignored.
     *
     * @param  array   array to resize, or {@code null}.
     * @param  length  length of the desired array.
     * @return a new array of the requested length, or {@code array} if the given
     *         array is {@code null} or already have the requested length.
     * @throws NegativeArraySizeException if {@code length} is negative.
     *
     * @see Arrays#copyOf(short[], int)
     */
    public static short[] resize(final short[] array, final int length) {
        if (array != null) {
            if (length == 0) {
                return EMPTY_SHORT;
            }
            if (array.length != length) {
                return Arrays.copyOf(array, length);
            }
        }
        return array;
    }

    /**
     * Returns an array containing the same elements as the given {@code array} but
     * specified {@code length}, truncating or padding with zeros if necessary.
     * This method returns {@code null} if and only if the given array is {@code null},
     * in which case the value of the {@code length} argument is ignored.
     *
     * @param  array   array to resize, or {@code null}.
     * @param  length  length of the desired array.
     * @return a new array of the requested length, or {@code array} if the given
     *         array is {@code null} or already have the requested length.
     * @throws NegativeArraySizeException if {@code length} is negative.
     *
     * @see Arrays#copyOf(byte[], int)
     */
    public static byte[] resize(final byte[] array, final int length) {
        if (array != null) {
            if (length == 0) {
                return EMPTY_BYTE;
            }
            if (array.length != length) {
                return Arrays.copyOf(array, length);
            }
        }
        return array;
    }

   /**
     * Returns an array containing the same elements as the given {@code array} but
     * specified {@code length}, truncating or padding with zeros if necessary.
     * This method returns {@code null} if and only if the given array is {@code null},
     * in which case the value of the {@code length} argument is ignored.
     *
     * @param  array   array to resize, or {@code null}.
     * @param  length  length of the desired array.
     * @return a new array of the requested length, or {@code array} if the given
     *         array is {@code null} or already have the requested length.
     * @throws NegativeArraySizeException if {@code length} is negative.
     *
     * @see Arrays#copyOf(char[], int)
     */
    public static char[] resize(final char[] array, final int length) {
        if (array != null) {
            if (length == 0) {
                return EMPTY_CHAR;
            }
            if (array.length != length) {
                return Arrays.copyOf(array, length);
            }
        }
        return array;
    }

    /**
     * Returns an array containing the same elements as the given {@code array} but
     * specified {@code length}, truncating or padding with {@code false} if necessary.
     * This method returns {@code null} if and only if the given array is {@code null},
     * in which case the value of the {@code length} argument is ignored.
     *
     * @param  array   array to resize, or {@code null}.
     * @param  length  length of the desired array.
     * @return a new array of the requested length, or {@code array} if the given
     *         array is {@code null} or already have the requested length.
     * @throws NegativeArraySizeException if {@code length} is negative.
     *
     * @see Arrays#copyOf(boolean[], int)
     */
    public static boolean[] resize(final boolean[] array, final int length) {
        if (array != null) {
            if (length == 0) {
                return EMPTY_BOOLEAN;
            }
            if (array.length != length) {
                return Arrays.copyOf(array, length);
            }
        }
        return array;
    }

    /**
     * Returns an array containing the same elements as the given array except for the given range.
     * If the {@code length} argument is 0, then this method returns the {@code array} reference unchanged.
     * Otherwise this method creates a new array. In every cases, the given array is never modified.
     *
     * @param  <T>     the array type.
     * @param  array   array from which to remove elements. Can be {@code null} only if {@code length} is 0.
     * @param  first   index of the first element to remove from the given {@code array}.
     * @param  length  number of elements to remove.
     * @return array with the same elements as the given {@code array} except for the removed elements,
     *         or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullPointerException if {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException if {@code length} is negative.
     * @throws IndexOutOfBoundsException if {@code first} or {@code first+length} is out of array bounds.
     */
    @SuppressWarnings("SuspiciousSystemArraycopy")
    private static <T> T doRemove(final T array, final int first, final int length) {
        if (length == 0) {
            return array;               // May be null
        }
        ArgumentChecks.ensureNonNull ("array",  array);
        ArgumentChecks.ensurePositive("length", length);
        int arrayLength = Array.getLength(array);
        @SuppressWarnings("unchecked")
        final T newArray = (T) Array.newInstance(array.getClass().getComponentType(), arrayLength -= length);
        System.arraycopy(array, 0,            newArray, 0,                 first);
        System.arraycopy(array, first+length, newArray, first, arrayLength-first);
        return newArray;
    }

    /**
     * Returns an array containing the same elements as the given array except for the given range.
     * If the {@code length} argument is 0, then this method returns the {@code array} reference unchanged.
     * Otherwise this method creates a new array. In every cases, the given array is never modified.
     *
     * @param  <E>     the type of array elements.
     * @param  array   array from which to remove elements. Can be {@code null} only if {@code length} is 0.
     * @param  first   index of the first element to remove from the given {@code array}.
     * @param  length  number of elements to remove.
     * @return array with the same elements as the given {@code array} except for the removed elements,
     *         or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullPointerException if {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException if {@code length} is negative.
     * @throws IndexOutOfBoundsException if {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(Object[], int, int)
     */
    public static <E> E[] remove(final E[] array, final int first, final int length) {
        return doRemove(array, first, length);
    }

    /**
     * Returns an array containing the same elements as the given array except for the given range.
     * If the {@code length} argument is 0, then this method returns the {@code array} reference unchanged,
     * except for {@linkplain #EMPTY_DOUBLE empty} arrays. Otherwise this method creates a new array.
     * In every cases, the given array is never modified.
     *
     * @param  array   array from which to remove elements. Can be {@code null} only if {@code length} is 0.
     * @param  first   index of the first element to remove from the given {@code array}.
     * @param  length  number of elements to remove.
     * @return array with the same elements as the given {@code array} except for the removed elements,
     *         or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullPointerException if {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException if {@code length} is negative.
     * @throws IndexOutOfBoundsException if {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(double[], int, int)
     */
    public static double[] remove(final double[] array, final int first, final int length) {
        return (first == 0 && array != null && length == array.length)
                ? EMPTY_DOUBLE : doRemove(array, first, length);
    }

    /**
     * Returns an array containing the same elements as the given array except for the given range.
     * If the {@code length} argument is 0, then this method returns the {@code array} reference unchanged,
     * except for {@linkplain #EMPTY_FLOAT empty} arrays. Otherwise this method creates a new array.
     * In every cases, the given array is never modified.
     *
     * @param  array   array from which to remove elements. Can be {@code null} only if {@code length} is 0.
     * @param  first   index of the first element to remove from the given {@code array}.
     * @param  length  number of elements to remove.
     * @return array with the same elements as the given {@code array} except for the removed elements,
     *         or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullPointerException if {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException if {@code length} is negative.
     * @throws IndexOutOfBoundsException if {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(float[], int, int)
     */
    public static float[] remove(final float[] array, final int first, final int length) {
        return (first == 0 && array != null && length == array.length)
                ? EMPTY_FLOAT : doRemove(array, first, length);
    }

    /**
     * Returns an array containing the same elements as the given array except for the given range.
     * If the {@code length} argument is 0, then this method returns the {@code array} reference unchanged,
     * except for {@linkplain #EMPTY_LONG empty} arrays. Otherwise this method creates a new array.
     * In every cases, the given array is never modified.
     *
     * @param  array   array from which to remove elements. Can be {@code null} only if {@code length} is 0.
     * @param  first   index of the first element to remove from the given {@code array}.
     * @param  length  number of elements to remove.
     * @return array with the same elements as the given {@code array} except for the removed elements,
     *         or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullPointerException if {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException if {@code length} is negative.
     * @throws IndexOutOfBoundsException if {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(long[], int, int)
     */
    public static long[] remove(final long[] array, final int first, final int length) {
        return (first == 0 && array != null && length == array.length)
                ? EMPTY_LONG : doRemove(array, first, length);
    }

    /**
     * Returns an array containing the same elements as the given array except for the given range.
     * If the {@code length} argument is 0, then this method returns the {@code array} reference unchanged,
     * except for {@linkplain #EMPTY_INT empty} arrays. Otherwise this method creates a new array.
     * In every cases, the given array is never modified.
     *
     * @param  array   array from which to remove elements. Can be {@code null} only if {@code length} is 0.
     * @param  first   index of the first element to remove from the given {@code array}.
     * @param  length  number of elements to remove.
     * @return array with the same elements as the given {@code array} except for the removed elements,
     *         or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullPointerException if {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException if {@code length} is negative.
     * @throws IndexOutOfBoundsException if {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(int[], int, int)
     */
    public static int[] remove(final int[] array, final int first, final int length) {
        return (first == 0 && array != null && length == array.length)
                ? EMPTY_INT : doRemove(array, first, length);
    }

    /**
     * Returns an array containing the same elements as the given array except for the given range.
     * If the {@code length} argument is 0, then this method returns the {@code array} reference unchanged,
     * except for {@linkplain #EMPTY_SHORT empty} arrays. Otherwise this method creates a new array.
     * In every cases, the given array is never modified.
     *
     * @param  array   array from which to remove elements. Can be {@code null} only if {@code length} is 0.
     * @param  first   index of the first element to remove from the given {@code array}.
     * @param  length  number of elements to remove.
     * @return array with the same elements as the given {@code array} except for the removed elements,
     *         or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullPointerException if {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException if {@code length} is negative.
     * @throws IndexOutOfBoundsException if {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(short[], int, int)
     */
    public static short[] remove(final short[] array, final int first, final int length) {
        return (first == 0 && array != null && length == array.length) ?
                EMPTY_SHORT : doRemove(array, first, length);
    }

    /**
     * Returns an array containing the same elements as the given array except for the given range.
     * If the {@code length} argument is 0, then this method returns the {@code array} reference unchanged,
     * except for {@linkplain #EMPTY_BYTE empty} arrays. Otherwise this method creates a new array.
     * In every cases, the given array is never modified.
     *
     * @param  array   array from which to remove elements. Can be {@code null} only if {@code length} is 0.
     * @param  first   index of the first element to remove from the given {@code array}.
     * @param  length  number of elements to remove.
     * @return array with the same elements as the given {@code array} except for the removed elements,
     *         or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullPointerException if {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException if {@code length} is negative.
     * @throws IndexOutOfBoundsException if {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(byte[], int, int)
     */
    public static byte[] remove(final byte[] array, final int first, final int length) {
        return (first == 0 && array != null && length == array.length)
                ? EMPTY_BYTE : doRemove(array, first, length);
    }

    /**
     * Returns an array containing the same elements as the given array except for the given range.
     * If the {@code length} argument is 0, then this method returns the {@code array} reference unchanged,
     * except for {@linkplain #EMPTY_CHAR empty} arrays. Otherwise this method creates a new array.
     * In every cases, the given array is never modified.
     *
     * @param  array   array from which to remove elements. Can be {@code null} only if {@code length} is 0.
     * @param  first   index of the first element to remove from the given {@code array}.
     * @param  length  number of elements to remove.
     * @return array with the same elements as the given {@code array} except for the removed elements,
     *         or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullPointerException if {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException if {@code length} is negative.
     * @throws IndexOutOfBoundsException if {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(char[], int, int)
     */
    public static char[] remove(final char[] array, final int first, final int length) {
        return (first == 0 && array != null && length == array.length)
                ? EMPTY_CHAR : doRemove(array, first, length);
    }

    /**
     * Returns an array containing the same elements as the given array except for the given range.
     * If the {@code length} argument is 0, then this method returns the {@code array} reference unchanged,
     * except for {@linkplain #EMPTY_BOOLEAN empty} arrays. Otherwise this method creates a new array.
     * In every cases, the given array is never modified.
     *
     * @param  array   array from which to remove elements. Can be {@code null} only if {@code length} is 0.
     * @param  first   index of the first element to remove from the given {@code array}.
     * @param  length  number of elements to remove.
     * @return array with the same elements as the given {@code array} except for the removed elements,
     *         or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullPointerException if {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException if {@code length} is negative.
     * @throws IndexOutOfBoundsException if {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(boolean[], int, int)
     */
    public static boolean[] remove(final boolean[] array, final int first, final int length) {
        return (first == 0 && array != null && length == array.length)
                ? EMPTY_BOOLEAN : doRemove(array, first, length);
    }

    /**
     * Returns an array containing the same elements as the given array, with additional
     * "spaces" in the given range. These "spaces" will be made up of {@code null} elements.
     * If the {@code length} argument is 0, then this method returns the {@code array} reference unchanged.
     * Otherwise this method creates a new array. In every cases, the given array is never modified.
     *
     * @param  <T>     the array type.
     * @param  array   array in which to insert spaces. Can be {@code null} only if {@code length} is 0.
     * @param  first   index where the first space will be inserted. All {@code array} elements
     *                 having an index equal to or higher than {@code index} will be moved forward.
     *                 Can be {@code array.length} for inserting spaces at the end of the array.
     * @param  length  number of spaces to insert.
     * @return array containing the {@code array} elements with the additional space inserted,
     *         or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullPointerException if {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException if {@code length} is negative.
     * @throws IndexOutOfBoundsException if {@code first} or {@code first+length} is out of array bounds.
     */
    @SuppressWarnings("SuspiciousSystemArraycopy")
    private static <T> T doInsert(final T array, final int first, final int length) {
        if (length == 0) {
            return array;               // May be null
        }
        ArgumentChecks.ensureNonNull("array",  array);
        final int arrayLength = Array.getLength(array);
        ArgumentChecks.ensureBetween("first", 0, arrayLength, first);
        ArgumentChecks.ensurePositive("length", length);
        @SuppressWarnings("unchecked")
        final T newArray = (T) Array.newInstance(array.getClass().getComponentType(), arrayLength + length);
        System.arraycopy(array, 0,     newArray, 0,            first            );
        System.arraycopy(array, first, newArray, first+length, arrayLength-first);
        return newArray;
    }

    /**
     * Returns an array containing the same elements as the given array, with additional
     * "spaces" in the given range. These "spaces" will be made up of {@code null} elements.
     * If the {@code length} argument is 0, then this method returns the {@code array} reference unchanged.
     * Otherwise this method creates a new array. In every cases, the given array is never modified.
     *
     * @param  <E>     the type of array elements.
     * @param  array   array in which to insert spaces. Can be {@code null} only if {@code length} is 0.
     * @param  first   index where the first space will be inserted. All {@code array} elements
     *                 having an index equal to or higher than {@code index} will be moved forward.
     *                 Can be {@code array.length} for inserting spaces at the end of the array.
     * @param  length  number of spaces to insert.
     * @return array containing the {@code array} elements with the additional space inserted,
     *         or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullPointerException if {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException if {@code length} is negative.
     * @throws IndexOutOfBoundsException if {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(Object[], int, Object[], int, int)
     * @see #remove(Object[], int, int)
     */
    public static <E> E[] insert(final E[] array, final int first, final int length) {
        return doInsert(array, first, length);
    }

    /**
     * Returns an array containing the same elements as the given array, with additional "spaces"
     * in the given range. These "spaces" will be made up of elements initialized to zero.
     * If the {@code length} argument is 0, then this method returns the {@code array} reference unchanged.
     * Otherwise this method creates a new array. In every cases, the given array is never modified.
     *
     * @param  array   array in which to insert spaces. Can be {@code null} only if {@code length} is 0.
     * @param  first   index where the first space will be inserted. All {@code array} elements
     *                 having an index equal to or higher than {@code index} will be moved forward.
     *                 Can be {@code array.length} for inserting spaces at the end of the array.
     * @param  length  number of spaces to insert.
     * @return array containing the {@code array} elements with the additional space inserted,
     *         or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullPointerException if {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException if {@code length} is negative.
     * @throws IndexOutOfBoundsException if {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(double[], int, double[], int, int)
     * @see #remove(double[], int, int)
     */
    public static double[] insert(final double[] array, final int first, final int length) {
        return doInsert(array, first, length);
    }

    /**
     * Returns an array containing the same elements as the given array, with additional "spaces"
     * in the given range. These "spaces" will be made up of elements initialized to zero.
     * If the {@code length} argument is 0, then this method returns the {@code array} reference unchanged.
     * Otherwise this method creates a new array. In every cases, the given array is never modified.
     *
     * @param  array   array in which to insert spaces. Can be {@code null} only if {@code length} is 0.
     * @param  first   index where the first space will be inserted. All {@code array} elements
     *                 having an index equal to or higher than {@code index} will be moved forward.
     *                 Can be {@code array.length} for inserting spaces at the end of the array.
     * @param  length  number of spaces to insert.
     * @return array containing the {@code array} elements with the additional space inserted,
     *         or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullPointerException if {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException if {@code length} is negative.
     * @throws IndexOutOfBoundsException if {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(float[], int, float[], int, int)
     * @see #remove(float[], int, int)
     */
    public static float[] insert(final float[] array, final int first, final int length) {
        return doInsert(array, first, length);
    }

    /**
     * Returns an array containing the same elements as the given array, with additional "spaces"
     * in the given range. These "spaces" will be made up of elements initialized to zero.
     * If the {@code length} argument is 0, then this method returns the {@code array} reference unchanged.
     * Otherwise this method creates a new array. In every cases, the given array is never modified.
     *
     * @param  array   array in which to insert spaces. Can be {@code null} only if {@code length} is 0.
     * @param  first   index where the first space will be inserted. All {@code array} elements
     *                 having an index equal to or higher than {@code index} will be moved forward.
     *                 Can be {@code array.length} for inserting spaces at the end of the array.
     * @param  length  number of spaces to insert.
     * @return array containing the {@code array} elements with the additional space inserted,
     *         or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullPointerException if {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException if {@code length} is negative.
     * @throws IndexOutOfBoundsException if {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(long[], int, long[], int, int)
     * @see #remove(long[], int, int)
     */
    public static long[] insert(final long[] array, final int first, final int length) {
        return doInsert(array, first, length);
    }

    /**
     * Returns an array containing the same elements as the given array, with additional "spaces"
     * in the given range. These "spaces" will be made up of elements initialized to zero.
     * If the {@code length} argument is 0, then this method returns the {@code array} reference unchanged.
     * Otherwise this method creates a new array. In every cases, the given array is never modified.
     *
     * @param  array   array in which to insert spaces. Can be {@code null} only if {@code length} is 0.
     * @param  first   index where the first space will be inserted. All {@code array} elements
     *                 having an index equal to or higher than {@code index} will be moved forward.
     *                 Can be {@code array.length} for inserting spaces at the end of the array.
     * @param  length  number of spaces to insert.
     * @return array containing the {@code array} elements with the additional space inserted,
     *         or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullPointerException if {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException if {@code length} is negative.
     * @throws IndexOutOfBoundsException if {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(int[], int, int[], int, int)
     * @see #remove(int[], int, int)
     */
    public static int[] insert(final int[] array, final int first, final int length) {
        return doInsert(array, first, length);
    }

    /**
     * Returns an array containing the same elements as the given array, with additional "spaces"
     * in the given range. These "spaces" will be made up of elements initialized to zero.
     * If the {@code length} argument is 0, then this method returns the {@code array} reference unchanged.
     * Otherwise this method creates a new array. In every cases, the given array is never modified.
     *
     * @param  array   array in which to insert spaces. Can be {@code null} only if {@code length} is 0.
     * @param  first   index where the first space should be inserted. All {@code array} elements
     *                 having an index equal to or higher than {@code index} will be moved forward.
     * @param  length  number of spaces to insert.
     * @return array containing the {@code array} elements with the additional space inserted,
     *         or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullPointerException if {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException if {@code length} is negative.
     * @throws IndexOutOfBoundsException if {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(short[], int, short[], int, int)
     * @see #remove(short[], int, int)
     */
    public static short[] insert(final short[] array, final int first, final int length) {
        return doInsert(array, first, length);
    }

    /**
     * Returns an array containing the same elements as the given array, with additional "spaces"
     * in the given range. These "spaces" will be made up of elements initialized to zero.
     * If the {@code length} argument is 0, then this method returns the {@code array} reference unchanged.
     * Otherwise this method creates a new array. In every cases, the given array is never modified.
     *
     * @param  array   array in which to insert spaces. Can be {@code null} only if {@code length} is 0.
     * @param  first   index where the first space will be inserted. All {@code array} elements
     *                 having an index equal to or higher than {@code index} will be moved forward.
     *                 Can be {@code array.length} for inserting spaces at the end of the array.
     * @param  length  number of spaces to insert.
     * @return array containing the {@code array} elements with the additional space inserted,
     *         or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullPointerException if {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException if {@code length} is negative.
     * @throws IndexOutOfBoundsException if {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(byte[], int, byte[], int, int)
     * @see #remove(byte[], int, int)
     */
    public static byte[] insert(final byte[] array, final int first, final int length) {
        return doInsert(array, first, length);
    }

    /**
     * Returns an array containing the same elements as the given array, with additional "spaces"
     * in the given range. These "spaces" will be made up of elements initialized to zero.
     * If the {@code length} argument is 0, then this method returns the {@code array} reference unchanged.
     * Otherwise this method creates a new array. In every cases, the given array is never modified.
     *
     * @param  array   array in which to insert spaces. Can be {@code null} only if {@code length} is 0.
     * @param  first   index where the first space will be inserted. All {@code array} elements
     *                 having an index equal to or higher than {@code index} will be moved forward.
     *                 Can be {@code array.length} for inserting spaces at the end of the array.
     * @param  length  number of spaces to insert.
     * @return array containing the {@code array} elements with the additional space inserted,
     *         or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullPointerException if {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException if {@code length} is negative.
     * @throws IndexOutOfBoundsException if {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(char[], int, char[], int, int)
     * @see #remove(char[], int, int)
     */
    public static char[] insert(final char[] array, final int first, final int length) {
        return doInsert(array, first, length);
    }

    /**
     * Returns an array containing the same elements as the given array, with additional "spaces"
     * in the given range. These "spaces" will be made up of elements initialized to {@code false}.
     * If the {@code length} argument is 0, then this method returns the {@code array} reference unchanged.
     * Otherwise this method creates a new array. In every cases, the given array is never modified.
     *
     * @param  array   array in which to insert spaces. Can be {@code null} only if {@code length} is 0.
     * @param  first   index where the first space will be inserted. All {@code array} elements
     *                 having an index equal to or higher than {@code index} will be moved forward.
     *                 Can be {@code array.length} for inserting spaces at the end of the array.
     * @param  length  number of spaces to insert.
     * @return array containing the {@code array} elements with the additional space inserted,
     *         or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullPointerException if {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException if {@code length} is negative.
     * @throws IndexOutOfBoundsException if {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(boolean[], int, boolean[], int, int)
     * @see #remove(boolean[], int, int)
     */
    public static boolean[] insert(final boolean[] array, final int first, final int length) {
        return doInsert(array, first, length);
    }

    /**
     * Returns an array containing the same elements as the given array,
     * with the content of another array inserted at the given index.
     * If the {@code length} argument is 0, then this method returns the {@code dst} reference unchanged.
     * Otherwise this method creates a new array. In every cases, the given arrays are never modified.
     *
     * @param  <T>     the arrays type.
     * @param  src     array to entirely or partially insert into {@code dst}. Can be null only if {@code length} is 0.
     * @param  srcOff  index of the first element of {@code src} to insert into {@code dst}.
     * @param  dst     array in which to insert {@code src} data. Can be null only if {@code length} is 0.
     * @param  dstOff  index of the first element in {@code dst} where to insert {@code src} data.
     *                 All elements of {@code dst} whose index is equal to or greater than {@code dstOff}
     *                 will be moved forward.
     * @param  length  number of {@code src} elements to insert.
     * @return array which contains the merge of {@code src} and {@code dst}.
     *         This method returns directly {@code dst} when {@code length} is zero, but never return {@code src}.
     * @throws NullPointerException if {@code src} or {@code dst} is null while {@code length} is different than 0.
     * @throws IllegalArgumentException if {@code length} is negative.
     * @throws IndexOutOfBoundsException if {@code srcOff}, {@code srcOff+length} or {@code dstOff} is out of array bounds.
     */
    @SuppressWarnings("SuspiciousSystemArraycopy")
    private static <T> T doInsert(final T src, final int srcOff,
                                  final T dst, final int dstOff, final int length)
    {
        if (length == 0) {
            return dst;             // May be null
        }
        ArgumentChecks.ensureNonNull("src", src);
        ArgumentChecks.ensureNonNull("dst", dst);
        ArgumentChecks.ensurePositive("length", length);
        final int dstLength = Array.getLength(dst);
        @SuppressWarnings("unchecked")
        final T newArray = (T) Array.newInstance(dst.getClass().getComponentType(), dstLength+length);
        System.arraycopy(dst, 0,      newArray, 0,             dstOff          );
        System.arraycopy(src, srcOff, newArray, dstOff,        length          );
        System.arraycopy(dst, dstOff, newArray, dstOff+length, dstLength-dstOff);
        return newArray;
    }

    /**
     * Returns an array containing the same elements as the given array,
     * with the content of another array inserted at the given index.
     * If the {@code length} argument is 0, then this method returns the {@code dst} reference unchanged.
     * Otherwise this method creates a new array. In every cases, the given arrays are never modified.
     *
     * @param  <E>     the type of array elements.
     * @param  src     array to entirely or partially insert into {@code dst}. Can be null only if {@code length} is 0.
     * @param  srcOff  index of the first element of {@code src} to insert into {@code dst}.
     * @param  dst     array in which to insert {@code src} data. Can be null only if {@code length} is 0.
     * @param  dstOff  index of the first element in {@code dst} where to insert {@code src} data.
     *                 All elements of {@code dst} whose index is equal to or greater than {@code dstOff}
     *                 will be moved forward.
     * @param  length  number of {@code src} elements to insert.
     * @return array which contains the merge of {@code src} and {@code dst}.
     *         This method returns directly {@code dst} when {@code length} is zero, but never return {@code src}.
     * @throws NullPointerException if {@code src} or {@code dst} is null while {@code length} is different than 0.
     * @throws IllegalArgumentException if {@code length} is negative.
     * @throws IndexOutOfBoundsException if {@code srcOff}, {@code srcOff+length} or {@code dstOff} is out of array bounds.
     */
    public static <E> E[] insert(final E[] src, final int srcOff,
                                 final E[] dst, final int dstOff, final int length)
    {
        return doInsert(src, srcOff, dst, dstOff, length);
    }

    /**
     * Returns an array containing the same elements as the given array,
     * with the content of another array inserted at the given index.
     * If the {@code length} argument is 0, then this method returns the {@code dst} reference unchanged.
     * Otherwise this method creates a new array. In every cases, the given arrays are never modified.
     *
     * @param  src     array to entirely or partially insert into {@code dst}. Can be null only if {@code length} is 0.
     * @param  srcOff  index of the first element of {@code src} to insert into {@code dst}.
     * @param  dst     array in which to insert {@code src} data. Can be null only if {@code length} is 0.
     * @param  dstOff  index of the first element in {@code dst} where to insert {@code src} data.
     *                 All elements of {@code dst} whose index is equal to or greater than {@code dstOff}
     *                 will be moved forward.
     * @param  length  number of {@code src} elements to insert.
     * @return array which contains the merge of {@code src} and {@code dst}.
     *         This method returns directly {@code dst} when {@code length} is zero, but never return {@code src}.
     * @throws NullPointerException if {@code src} or {@code dst} is null while {@code length} is different than 0.
     * @throws IllegalArgumentException if {@code length} is negative.
     * @throws IndexOutOfBoundsException if {@code srcOff}, {@code srcOff+length} or {@code dstOff} is out of array bounds.
     */
    public static double[] insert(final double[] src, final int srcOff,
                                  final double[] dst, final int dstOff, final int length)
    {
        return doInsert(src, srcOff, dst, dstOff, length);
    }

    /**
     * Returns an array containing the same elements as the given array,
     * with the content of another array inserted at the given index.
     * If the {@code length} argument is 0, then this method returns the {@code dst} reference unchanged.
     * Otherwise this method creates a new array. In every cases, the given arrays are never modified.
     *
     * @param  src     array to entirely or partially insert into {@code dst}. Can be null only if {@code length} is 0.
     * @param  srcOff  index of the first element of {@code src} to insert into {@code dst}.
     * @param  dst     array in which to insert {@code src} data. Can be null only if {@code length} is 0.
     * @param  dstOff  index of the first element in {@code dst} where to insert {@code src} data.
     *                 All elements of {@code dst} whose index is equal to or greater than {@code dstOff}
     *                 will be moved forward.
     * @param  length  number of {@code src} elements to insert.
     * @return array which contains the merge of {@code src} and {@code dst}.
     *         This method returns directly {@code dst} when {@code length} is zero, but never return {@code src}.
     * @throws NullPointerException if {@code src} or {@code dst} is null while {@code length} is different than 0.
     * @throws IllegalArgumentException if {@code length} is negative.
     * @throws IndexOutOfBoundsException if {@code srcOff}, {@code srcOff+length} or {@code dstOff} is out of array bounds.
     */
    public static float[] insert(final float[] src, final int srcOff,
                                 final float[] dst, final int dstOff, final int length)
    {
        return doInsert(src, srcOff, dst, dstOff, length);
    }

    /**
     * Returns an array containing the same elements as the given array,
     * with the content of another array inserted at the given index.
     * If the {@code length} argument is 0, then this method returns the {@code dst} reference unchanged.
     * Otherwise this method creates a new array. In every cases, the given arrays are never modified.
     *
     * @param  src     array to entirely or partially insert into {@code dst}. Can be null only if {@code length} is 0.
     * @param  srcOff  index of the first element of {@code src} to insert into {@code dst}.
     * @param  dst     array in which to insert {@code src} data. Can be null only if {@code length} is 0.
     * @param  dstOff  index of the first element in {@code dst} where to insert {@code src} data.
     *                 All elements of {@code dst} whose index is equal to or greater than {@code dstOff}
     *                 will be moved forward.
     * @param  length  number of {@code src} elements to insert.
     * @return array which contains the merge of {@code src} and {@code dst}.
     *         This method returns directly {@code dst} when {@code length} is zero, but never return {@code src}.
     * @throws NullPointerException if {@code src} or {@code dst} is null while {@code length} is different than 0.
     * @throws IllegalArgumentException if {@code length} is negative.
     * @throws IndexOutOfBoundsException if {@code srcOff}, {@code srcOff+length} or {@code dstOff} is out of array bounds.
     */
    public static long[] insert(final long[] src, final int srcOff,
                                final long[] dst, final int dstOff, final int length)
    {
        return doInsert(src, srcOff, dst, dstOff, length);
    }

    /**
     * Returns an array containing the same elements as the given array,
     * with the content of another array inserted at the given index.
     * If the {@code length} argument is 0, then this method returns the {@code dst} reference unchanged.
     * Otherwise this method creates a new array. In every cases, the given arrays are never modified.
     *
     * @param  src     array to entirely or partially insert into {@code dst}. Can be null only if {@code length} is 0.
     * @param  srcOff  index of the first element of {@code src} to insert into {@code dst}.
     * @param  dst     array in which to insert {@code src} data. Can be null only if {@code length} is 0.
     * @param  dstOff  index of the first element in {@code dst} where to insert {@code src} data.
     *                 All elements of {@code dst} whose index is equal to or greater than {@code dstOff}
     *                 will be moved forward.
     * @param  length  number of {@code src} elements to insert.
     * @return array which contains the merge of {@code src} and {@code dst}.
     *         This method returns directly {@code dst} when {@code length} is zero, but never return {@code src}.
     * @throws NullPointerException if {@code src} or {@code dst} is null while {@code length} is different than 0.
     * @throws IllegalArgumentException if {@code length} is negative.
     * @throws IndexOutOfBoundsException if {@code srcOff}, {@code srcOff+length} or {@code dstOff} is out of array bounds.
     */
    public static int[] insert(final int[] src, final int srcOff,
                               final int[] dst, final int dstOff, final int length)
    {
        return doInsert(src, srcOff, dst, dstOff, length);
    }

    /**
     * Returns an array containing the same elements as the given array,
     * with the content of another array inserted at the given index.
     * If the {@code length} argument is 0, then this method returns the {@code dst} reference unchanged.
     * Otherwise this method creates a new array. In every cases, the given arrays are never modified.
     *
     * @param  src     array to entirely or partially insert into {@code dst}. Can be null only if {@code length} is 0.
     * @param  srcOff  index of the first element of {@code src} to insert into {@code dst}.
     * @param  dst     array in which to insert {@code src} data. Can be null only if {@code length} is 0.
     * @param  dstOff  index of the first element in {@code dst} where to insert {@code src} data.
     *                 All elements of {@code dst} whose index is equal to or greater than {@code dstOff}
     *                 will be moved forward.
     * @param  length  number of {@code src} elements to insert.
     * @return array which contains the merge of {@code src} and {@code dst}.
     *         This method returns directly {@code dst} when {@code length} is zero, but never return {@code src}.
     * @throws NullPointerException if {@code src} or {@code dst} is null while {@code length} is different than 0.
     * @throws IllegalArgumentException if {@code length} is negative.
     * @throws IndexOutOfBoundsException if {@code srcOff}, {@code srcOff+length} or {@code dstOff} is out of array bounds.
     */
    public static short[] insert(final short[] src, final int srcOff,
                                 final short[] dst, final int dstOff, final int length)
    {
        return doInsert(src, srcOff, dst, dstOff, length);
    }

    /**
     * Returns an array containing the same elements as the given array,
     * with the content of another array inserted at the given index.
     * If the {@code length} argument is 0, then this method returns the {@code dst} reference unchanged.
     * Otherwise this method creates a new array. In every cases, the given arrays are never modified.
     *
     * @param  src     array to entirely or partially insert into {@code dst}. Can be null only if {@code length} is 0.
     * @param  srcOff  index of the first element of {@code src} to insert into {@code dst}.
     * @param  dst     array in which to insert {@code src} data. Can be null only if {@code length} is 0.
     * @param  dstOff  index of the first element in {@code dst} where to insert {@code src} data.
     *                 All elements of {@code dst} whose index is equal to or greater than {@code dstOff}
     *                 will be moved forward.
     * @param  length  number of {@code src} elements to insert.
     * @return array which contains the merge of {@code src} and {@code dst}.
     *         This method returns directly {@code dst} when {@code length} is zero, but never return {@code src}.
     * @throws NullPointerException if {@code src} or {@code dst} is null while {@code length} is different than 0.
     * @throws IllegalArgumentException if {@code length} is negative.
     * @throws IndexOutOfBoundsException if {@code srcOff}, {@code srcOff+length} or {@code dstOff} is out of array bounds.
     */
    public static byte[] insert(final byte[] src, final int srcOff,
                                final byte[] dst, final int dstOff, final int length)
    {
        return doInsert(src, srcOff, dst, dstOff, length);
    }

    /**
     * Returns an array containing the same elements as the given array,
     * with the content of another array inserted at the given index.
     * If the {@code length} argument is 0, then this method returns the {@code dst} reference unchanged.
     * Otherwise this method creates a new array. In every cases, the given arrays are never modified.
     *
     * @param  src     array to entirely or partially insert into {@code dst}. Can be null only if {@code length} is 0.
     * @param  srcOff  index of the first element of {@code src} to insert into {@code dst}.
     * @param  dst     array in which to insert {@code src} data. Can be null only if {@code length} is 0.
     * @param  dstOff  index of the first element in {@code dst} where to insert {@code src} data.
     *                 All elements of {@code dst} whose index is equal to or greater than {@code dstOff}
     *                 will be moved forward.
     * @param  length  number of {@code src} elements to insert.
     * @return array which contains the merge of {@code src} and {@code dst}.
     *         This method returns directly {@code dst} when {@code length} is zero, but never return {@code src}.
     * @throws NullPointerException if {@code src} or {@code dst} is null while {@code length} is different than 0.
     * @throws IllegalArgumentException if {@code length} is negative.
     * @throws IndexOutOfBoundsException if {@code srcOff}, {@code srcOff+length} or {@code dstOff} is out of array bounds.
     */
    public static char[] insert(final char[] src, final int srcOff,
                                final char[] dst, final int dstOff, final int length)
    {
        return doInsert(src, srcOff, dst, dstOff, length);
    }

    /**
     * Returns an array containing the same elements as the given array,
     * with the content of another array inserted at the given index.
     * If the {@code length} argument is 0, then this method returns the {@code dst} reference unchanged.
     * Otherwise this method creates a new array. In every cases, the given arrays are never modified.
     *
     * @param  src     array to entirely or partially insert into {@code dst}. Can be null only if {@code length} is 0.
     * @param  srcOff  index of the first element of {@code src} to insert into {@code dst}.
     * @param  dst     array in which to insert {@code src} data. Can be null only if {@code length} is 0.
     * @param  dstOff  index of the first element in {@code dst} where to insert {@code src} data.
     *                 All elements of {@code dst} whose index is equal to or greater than {@code dstOff}
     *                 will be moved forward.
     * @param  length  number of {@code src} elements to insert.
     * @return array which contains the merge of {@code src} and {@code dst}.
     *         This method returns directly {@code dst} when {@code length} is zero, but never return {@code src}.
     * @throws NullPointerException if {@code src} or {@code dst} is null while {@code length} is different than 0.
     * @throws IllegalArgumentException if {@code length} is negative.
     * @throws IndexOutOfBoundsException if {@code srcOff}, {@code srcOff+length} or {@code dstOff} is out of array bounds.
     */
    public static boolean[] insert(final boolean[] src, final int srcOff,
                                   final boolean[] dst, final int dstOff, final int length)
    {
        return doInsert(src, srcOff, dst, dstOff, length);
    }

    /**
     * Returns a copy of the given array with a single element appended at the end.
     * This method should be invoked only on rare occasions.
     * If many elements are to be added, use {@link java.util.ArrayList} instead.
     *
     * @param  <T>      the type of elements in the array.
     * @param  array    the array to copy with a new element. The original array will not be modified.
     * @param  element  the element to add (can be null).
     * @return a copy of the given array with the given element appended at the end.
     * @throws NullPointerException if the given array is null.
     *
     * @see #concatenate(Object[][])
     */
    public static <T> T[] append(final T[] array, final T element) {
        final T[] copy = Arrays.copyOf(array, array.length + 1);
        copy[array.length] = element;
        return copy;
    }

    /**
     * Returns the concatenation of all given arrays. This method performs the following checks:
     *
     * <ul>
     *   <li>If the {@code arrays} argument is {@code null} or contains only {@code null}
     *       elements, then this method returns {@code null}.</li>
     *   <li>Otherwise if the {@code arrays} argument contains exactly one non-null array with
     *       a length greater than zero, then that array is returned. It is not copied.</li>
     *   <li>Otherwise a new array with a length equals to the sum of the length of every
     *       non-null arrays is created, and the content of non-null arrays are appended
     *       in the new array in declaration order.</li>
     * </ul>
     *
     * @param  <T>     the type of arrays.
     * @param  arrays  the arrays to concatenate, or {@code null}.
     * @return the concatenation of all non-null arrays (may be a direct reference to one
     *         of the given arrays if it can be returned with no change), or {@code null}.
     *
     * @see #append(Object[], Object)
     * @see #unionOfSorted(int[], int[])
     */
    @SafeVarargs
    public static <T> T[] concatenate(final T[]... arrays) {
        T[] result = null;
        if (arrays != null) {
            int length = 0;
            for (T[] array : arrays) {
                if (array != null) {
                    length += array.length;
                }
            }
            int offset = 0;
            for (T[] array : arrays) {
                if (array != null) {
                    if (result == null) {
                        if (array.length == length) {
                            return array;
                        }
                        if (array.length == 0) {    // Must be after the `array.length == length` test.
                            continue;               // For avoiding potentially unnecessary array copy.
                        }
                        /*
                         * A copy is needed. Search for a base class which is assignable from all other classes.
                         * Take all arrays in account, including the empty ones, because a suitable base type may
                         * be specified only in an empty array.
                         */
                        Class<? extends T[]> type = Classes.getClass(array);
                        for (T[] other : arrays) {
                            Class<? extends T[]> c = Classes.getClass(other);
                            if (c != null && c.isAssignableFrom(type)) type = c;
                        }
                        result = Arrays.copyOf(array, length, type);
                    } else {
                        System.arraycopy(array, 0, result, offset, array.length);
                    }
                    offset += array.length;
                }
            }
        }
        return result;
    }

    /**
     * Returns the concatenation of the given arrays.
     * If any of the supplied arrays is null or empty, then the other array is returned directly (not copied).
     *
     * @param  a1  the first array to concatenate, or {@code null}.
     * @param  a2  the second array to concatenate, or {@code null}.
     * @return the concatenation of given arrays. May be one of the given arrays returned without copying.
     *
     * @since 1.5
     */
    public static byte[] concatenate(final byte[] a1, final byte[] a2) {
        if (a1 == null || a1.length == 0) return a2;
        if (a2 == null || a2.length == 0) return a1;
        final byte[] copy = Arrays.copyOf(a1, a1.length + a2.length);
        System.arraycopy(a2, 0, copy, a1.length, a2.length);
        return copy;
    }

    /**
     * Returns the concatenation of the given arrays.
     * If any of the supplied arrays is null or empty, then the other array is returned directly (not copied).
     *
     * @param  a1  the first array to concatenate, or {@code null}.
     * @param  a2  the second array to concatenate, or {@code null}.
     * @return the concatenation of given arrays. May be one of the given arrays returned without copying.
     *
     * @since 1.4
     */
    public static int[] concatenate(final int[] a1, final int[] a2) {
        if (a1 == null || a1.length == 0) return a2;
        if (a2 == null || a2.length == 0) return a1;
        final int[] copy = Arrays.copyOf(a1, a1.length + a2.length);
        System.arraycopy(a2, 0, copy, a1.length, a2.length);
        return copy;
    }

    /**
     * Returns the concatenation of the given arrays.
     * If any of the supplied arrays is null or empty, then the other array is returned directly (not copied).
     *
     * @param  a1  the first array to concatenate, or {@code null}.
     * @param  a2  the second array to concatenate, or {@code null}.
     * @return the concatenation of given arrays. May be one of the given arrays returned without copying.
     *
     * @since 1.4
     */
    public static long[] concatenate(final long[] a1, final long[] a2) {
        if (a1 == null || a1.length == 0) return a2;
        if (a2 == null || a2.length == 0) return a1;
        final long[] copy = Arrays.copyOf(a1, a1.length + a2.length);
        System.arraycopy(a2, 0, copy, a1.length, a2.length);
        return copy;
    }

    /**
     * Returns the concatenation of the given arrays.
     * If any of the supplied arrays is null or empty, then the other array is returned directly (not copied).
     *
     * @param  a1  the first array to concatenate, or {@code null}.
     * @param  a2  the second array to concatenate, or {@code null}.
     * @return the concatenation of given arrays. May be one of the given arrays returned without copying.
     *
     * @since 1.5
     */
    public static double[] concatenate(final double[] a1, final double[] a2) {
        if (a1 == null || a1.length == 0) return a2;
        if (a2 == null || a2.length == 0) return a1;
        final double[] copy = Arrays.copyOf(a1, a1.length + a2.length);
        System.arraycopy(a2, 0, copy, a1.length, a2.length);
        return copy;
    }

    /**
     * Removes all null elements in the given array. For each null element found in the array at index <var>i</var>,
     * all elements at indices <var>i</var>+1, <var>i</var>+2, <var>i</var>+3, <i>etc.</i> are moved to indices
     * <var>i</var>, <var>i</var>+1, <var>i</var>+2, <i>etc.</i>
     * This method returns the new array length, which is {@code array.length} minus the number of null elements.
     * The array content at indices equal or greater than the new length is undetermined.
     *
     * <p>Callers can obtain an array of appropriate length using the following idiom.
     * Note that this idiom will create a new array only if necessary:</p>
     *
     * {@snippet lang="java" :
     *     T[] array = ...;
     *     array = resize(array, removeNulls(array));
     *     }
     *
     * @param  array  array from which to remove null elements, or {@code null}.
     * @return the number of remaining elements in the given array, or 0 if the given {@code array} was null.
     *
     * @since 1.5
     */
    public static int removeNulls(final Object[] array) {
        if (array == null) {
            return 0;
        }
        int i;
        for (i=0; ; i++) {
            if (i >= array.length) return i;            // Return if all values are non-null.
            if (array[i] == null) break;                // Stop without incrementing `i`.
        }
        Object value;
        int count = i;
        do if (++i >= array.length) return count;       // Common case where all remaining values are null.
        while ((value = array[i]) == null);

        // Start copying values only on the portion of the array where it is needed.
        array[count++] = value;
        while (++i < array.length) {
            value = array[i];
            if (value != null) {
                array[count++] = value;
            }
        }
        return count;
    }

    /**
     * Removes the duplicated elements in the given array. This method should be invoked only for small arrays,
     * typically less than 10 distinct elements. For larger arrays, use {@link java.util.LinkedHashSet} instead.
     *
     * <p>This method compares all pairs of elements using the {@link Objects#equals(Object, Object)} method -
     * so null elements are allowed. If duplicated values are found,
     * then only the first occurrence is retained and the second occurrence is removed in-place.
     * After all elements have been compared, this method returns the number of remaining elements in the array.
     * The free space at the end of the array is padded with {@code null} values.</p>
     *
     * <p>Callers can obtain an array of appropriate length using the following idiom.
     * Note that this idiom will create a new array only if necessary:</p>
     *
     * {@snippet lang="java" :
     *     T[] array = ...;
     *     array = resize(array, removeDuplicated(array));
     *     }
     *
     * @param  array array from which to remove duplicated elements, or {@code null}.
     * @return the number of remaining elements in the given array, or 0 if the given {@code array} was null.
     */
    public static int removeDuplicated(final Object[] array) {
        if (array == null) {
            return 0;
        }
        return removeDuplicated(array, array.length);
    }

    /**
     * Removes the duplicated elements in the first elements of the given array.
     * This method performs the same work as {@link #removeDuplicated(Object[])},
     * but taking in account only the first {@code length} elements. The latter argument
     * is convenient for chaining this method after {@link #removeNulls(Object[])} as below:
     *
     * {@snippet lang="java" :
     *     T[] array = ...;
     *     array = resize(array, removeDuplicated(array, removeNulls(array)));
     *     }
     *
     * @param  array   array from which to remove duplicated elements, or {@code null}.
     * @param  length  number of elements to examine at the beginning of the array.
     * @return the number of remaining elements in the given array, or 0 if the given {@code array} was null.
     * @throws ArrayIndexOutOfBoundsException if {@code length} is negative or greater than {@code array.length}.
     *
     * @since 1.5
     */
    public static int removeDuplicated(final Object[] array, int length) {
        for (int i=length; --i>=0;) {
            final Object value = array[i];
            for (int j=i; --j>=0;) {
                if (Objects.equals(array[j], value)) {
                    System.arraycopy(array, i+1, array, i, --length - i);
                    array[length] = null;
                    break;
                }
            }
        }
        return length;
    }

    /**
     * Reverses the order of elements in the given array.
     * This operation is performed in-place.
     * If the given array is {@code null}, then this method does nothing.
     *
     * @param  entries  the array in which to reverse the order of elements, or {@code null} if none.
     */
    public static void reverse(final Object[] entries) {
        if (entries != null) {
            int i = entries.length >>> 1;
            int j = i + (entries.length & 1);
            while (--i >= 0) {
                swap(entries, i, j++);
            }
        }
    }

    /**
     * Reverses the order of elements in the given array.
     * This operation is performed in-place.
     * If the given array is {@code null}, then this method does nothing.
     *
     * @param  values  the array in which to reverse the order of elements, or {@code null} if none.
     */
    public static void reverse(final int[] values) {
        if (values != null) {
            int i = values.length >>> 1;
            int j = i + (values.length & 1);
            while (--i >= 0) {
                swap(values, i, j++);
            }
        }
    }

    /**
     * Returns the ordered values in the range from {@code start} inclusive to {@code end} exclusive.
     * This method performs the same work as {@link java.util.stream.IntStream#range(int, int)} but
     * returning values in an array instead of in a stream. This method is okay for small sequences;
     * for large sequences the stream approach should be preferred.
     *
     * <p>For any array returned by this method, <code>{@link #isRange(int, int[]) isRange}(start, array)</code>
     * is guaranteed to return {@code true}.</p>
     *
     * <h4>Use case</h4>
     * This method is convenient for enumerating dimensions in a coordinate reference system or bands in an image.
     * Some methods in the Java library or in Apache SIS want dimensions or bands to be specified by their indices.
     * An example from the Java library is the {@code bankIndices} argument in
     * <code>{@linkplain java.awt.image.Raster#createBandedRaster(int, int, int, int, int[], int[], java.awt.Point)
     * Raster.createBandedRaster}(…, bankIndices, …)</code>.
     * An example from Apache SIS is the {@code range} argument in
     * <code>{@linkplain org.apache.sis.storage.GridCoverageResource#read GridCoverageResource.read}(…, range)</code>.
     * This {@code range(start, end)} method can be used in the common case where all bands are wanted in order.
     *
     * @param  start  first value (inclusive) in the array to return.
     * @param  end    upper bound (exclusive) of values in the array to return.
     * @return a finite arithmetic progression of common difference of 1 with all values in the specified range.
     * @throws ArithmeticException if the sequence length is greater than {@link Integer#MAX_VALUE}.
     *
     * @see java.util.stream.IntStream#range(int, int)
     * @see <a href="https://en.wikipedia.org/wiki/Arithmetic_progression">Arithmetic progression on Wikipedia</a>
     *
     * @since 1.0
     */
    public static int[] range(final int start, final int end) {
        if (end > start) {
            final int[] array = new int[Math.subtractExact(end, start)];
            for (int i=0; i<array.length; i++) {
                array[i] = start + i;
            }
            return array;
        } else {
            return EMPTY_INT;
        }
    }

    /**
     * Returns {@code true} if the given array is a finite arithmetic progression starting at the given value
     * and having a common difference of 1.
     * More specifically:
     *
     * <ul>
     *   <li>If {@code array} is {@code null}, then return {@code false}.</li>
     *   <li>Otherwise if {@code array} is empty, then return {@code true} for consistency with {@link #range}.</li>
     *   <li>Otherwise for any index 0 ≤ <var>i</var> {@literal <} {@code array.length}, if {@code array[i]}
     *       is equal to {@code start + i} (computed as if no overflow occurs), then return {@code true}.</li>
     *   <li>Otherwise return {@code false}.</li>
     * </ul>
     *
     * This method is useful when {@code array} is an argument specified to another method, and determining that the
     * argument values are {@code start}, {@code start}+1, {@code start}+2, <i>etc.</i> allows some optimizations.
     *
     * <h4>Example</h4>
     * {@code isRange(1, array)} returns {@code true} if the given array is {@code {1, 2, 3, 4}}
     * but {@code false} if the array is {@code {1, 2, 4}} (missing 3).
     *
     * @param  start  first value expected in the given {@code array}.
     * @param  array  the array to test, or {@code null}.
     * @return {@code true} if the given array is non-null and equal to
     *         <code>{@linkplain #range(int, int) range}(start, start + array.length)</code>.
     *
     * @see #range(int, int)
     *
     * @since 1.0
     */
    public static boolean isRange(final int start, final int[] array) {
        if (array == null) {
            return false;
        }
        if (array.length != 0) {
            if (start + (array.length - 1) < 0) {
                return false;                               // Overflow.
            }
            for (int i=0; i<array.length; i++) {
                if (array[i] != start + i) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if all elements in the specified array are in increasing order.
     * Special cases:
     *
     * <ul>
     *   <li>Empty arrays are considered as sorted.</li>
     *   <li>Null arrays are considered as unknown content and cause a {@code NullPointerException}
     *       to be thrown.</li>
     *   <li>{@code null} elements are considered unordered and may appear anywhere in the array;
     *       they will be silently ignored.</li>
     * </ul>
     *
     * @param  <E>         the type of array elements.
     * @param  array       the array to test for order.
     * @param  comparator  the comparator to use for comparing order.
     * @param  strict      {@code true} if elements should be strictly sorted
     *                     (i.e. equal elements are not allowed), or {@code false} otherwise.
     * @return {@code true} if all elements in the given array are sorted in increasing order.
     */
    public static <E> boolean isSorted(final E[] array, final Comparator<? super E> comparator, final boolean strict) {
        for (int i=0; i<array.length; i++) {
            E p = array[i];
            if (p != null) {
                while (++i < array.length) {
                    final E e = array[i];
                    if (e != null) {
                        final int c = comparator.compare(e, p);
                        if (strict ? c <= 0 : c < 0) {
                            return false;
                        }
                        p = e;
                    }
                }
                break;
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if all elements in the specified array are in increasing order.
     * Special cases:
     *
     * <ul>
     *   <li>Empty arrays are considered as sorted.</li>
     *   <li>Null arrays are considered as unknown content and cause a {@code NullPointerException}
     *       to be thrown.</li>
     *   <li>{@code null} elements are considered unordered and may appear anywhere in the array;
     *       they will be silently ignored.</li>
     * </ul>
     *
     * @param  <E>     the type of array elements.
     * @param  array   the array to test for order.
     * @param  strict  {@code true} if elements should be strictly sorted
     *                 (i.e. equal elements are not allowed), or {@code false} otherwise.
     * @return {@code true} if all elements in the given array are sorted in increasing order.
     */
    public static <E extends Comparable<? super E>> boolean isSorted(final E[] array, final boolean strict) {
        for (int i=0; i<array.length; i++) {
            E p = array[i];
            if (p != null) {
                while (++i < array.length) {
                    final E e = array[i];
                    if (e != null) {
                        final int c = e.compareTo(p);
                        if (strict ? c <= 0 : c < 0) {
                            return false;
                        }
                        p = e;
                    }
                }
                break;
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if all elements in the specified array are in increasing order.
     * Special cases:
     *
     * <ul>
     *   <li>Empty arrays are considered as sorted.</li>
     *   <li>Null arrays are considered as unknown content and cause a {@code NullPointerException}
     *       to be thrown.</li>
     *   <li>{@link Double#NaN NaN} elements are considered unordered and may appear anywhere
     *       in the array; they will be silently ignored.</li>
     * </ul>
     *
     * @param  array   the array to test for order.
     * @param  strict  {@code true} if elements should be strictly sorted
     *                 (i.e. equal elements are not allowed), or {@code false} otherwise.
     * @return {@code true} if all elements in the given array are sorted in increasing order.
     */
    public static boolean isSorted(final double[] array, final boolean strict) {
        for (int i=0; i<array.length; i++) {
            double p = array[i];
            if (!Double.isNaN(p)) {
                while (++i < array.length) {
                    final double e = array[i];
                    if (strict ? e <= p : e < p) {
                        return false;
                    }
                    if (!Double.isNaN(e)) {
                        p = e;
                    }
                }
                break;
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if all elements in the specified array are in increasing order.
     * Special cases:
     *
     * <ul>
     *   <li>Empty arrays are considered as sorted.</li>
     *   <li>Null arrays are considered as unknown content and cause a {@code NullPointerException}
     *       to be thrown.</li>
     *   <li>{@link Float#NaN NaN} elements are considered unordered and may appear anywhere
     *       in the array; they will be silently ignored.</li>
     * </ul>
     *
     * @param  array   the array to test for order.
     * @param  strict  {@code true} if elements should be strictly sorted
     *                 (i.e. equal elements are not allowed), or {@code false} otherwise.
     * @return {@code true} if all elements in the given array are sorted in increasing order.
     */
    public static boolean isSorted(final float[] array, final boolean strict) {
        for (int i=0; i<array.length; i++) {
            float p = array[i];
            if (!Float.isNaN(p)) {
                while (++i < array.length) {
                    final float e = array[i];
                    if (strict ? e <= p : e < p) {
                        return false;
                    }
                    if (!Float.isNaN(e)) {
                        p = e;
                    }
                }
                break;
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if all elements in the specified array are in increasing order.
     * Special cases:
     *
     * <ul>
     *   <li>Empty arrays are considered as sorted.</li>
     *   <li>Null arrays are considered as unknown content and cause a {@code NullPointerException}
     *       to be thrown.</li>
     * </ul>
     *
     * @param  array   the array to test for order.
     * @param  strict  {@code true} if elements should be strictly sorted
     *                 (i.e. equal elements are not allowed), or {@code false} otherwise.
     * @return {@code true} if all elements in the given array are sorted in increasing order.
     */
    public static boolean isSorted(final long[] array, final boolean strict) {
        if (array.length != 0) {
            long p = array[0];
            for (int i=1; i<array.length; i++) {
                final long e = array[i];
                if (strict ? e <= p : e < p) {
                    return false;
                }
                p = e;
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if all elements in the specified array are in increasing order.
     * Special cases:
     *
     * <ul>
     *   <li>Empty arrays are considered as sorted.</li>
     *   <li>Null arrays are considered as unknown content and cause a {@code NullPointerException}
     *       to be thrown.</li>
     * </ul>
     *
     * @param  array   the array to test for order.
     * @param  strict  {@code true} if elements should be strictly sorted
     *                 (i.e. equal elements are not allowed), or {@code false} otherwise.
     * @return {@code true} if all elements in the given array are sorted in increasing order.
     */
    public static boolean isSorted(final int[] array, final boolean strict) {
        if (array.length != 0) {
            int p = array[0];
            for (int i=1; i<array.length; i++) {
                final int e = array[i];
                if (strict ? e <= p : e < p) {
                    return false;
                }
                p = e;
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if all elements in the specified array are in increasing order.
     * Special cases:
     *
     * <ul>
     *   <li>Empty arrays are considered as sorted.</li>
     *   <li>Null arrays are considered as unknown content and cause a {@code NullPointerException}
     *       to be thrown.</li>
     * </ul>
     *
     * @param  array   the array to test for order.
     * @param  strict  {@code true} if elements should be strictly sorted
     *                 (i.e. equal elements are not allowed), or {@code false} otherwise.
     * @return {@code true} if all elements in the given array are sorted in increasing order.
     */
    public static boolean isSorted(final short[] array, final boolean strict) {
        if (array.length != 0) {
            short p = array[0];
            for (int i=1; i<array.length; i++) {
                final short e = array[i];
                if (strict ? e <= p : e < p) {
                    return false;
                }
                p = e;
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if all elements in the specified array are in increasing order.
     * Special cases:
     *
     * <ul>
     *   <li>Empty arrays are considered as sorted.</li>
     *   <li>Null arrays are considered as unknown content and cause a {@code NullPointerException}
     *       to be thrown.</li>
     * </ul>
     *
     * @param  array   the array to test for order.
     * @param  strict  {@code true} if elements should be strictly sorted
     *                 (i.e. equal elements are not allowed), or {@code false} otherwise.
     * @return {@code true} if all elements in the given array are sorted in increasing order.
     */
    public static boolean isSorted(final byte[] array, final boolean strict) {
        if (array.length != 0) {
            byte p = array[0];
            for (int i=1; i<array.length; i++) {
                final byte e = array[i];
                if (strict ? e <= p : e < p) {
                    return false;
                }
                p = e;
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if all elements in the specified array are in increasing order.
     * Special cases:
     *
     * <ul>
     *   <li>Empty arrays are considered as sorted.</li>
     *   <li>Null arrays are considered as unknown content and cause a {@code NullPointerException}
     *       to be thrown.</li>
     * </ul>
     *
     * @param  array   the array to test for order.
     * @param  strict  {@code true} if elements should be strictly sorted
     *                 (i.e. equal elements are not allowed), or {@code false} otherwise.
     * @return {@code true} if all elements in the given array are sorted in increasing order.
     */
    public static boolean isSorted(final char[] array, final boolean strict) {
        if (array.length != 0) {
            char p = array[0];
            for (int i=1; i<array.length; i++) {
                final char e = array[i];
                if (strict ? e <= p : e < p) {
                    return false;
                }
                p = e;
            }
        }
        return true;
    }

    /**
     * Swaps the elements at the given indices in the given array of {@code Object} values.
     *
     * <div class="note"><b>Note:</b>
     * While trivial, this method is provided because its need occurs relatively often
     * and the availability of a {@code swap} method makes the code easier to read.</div>
     *
     * @param  data  the array in which to swap elements.
     * @param  i0    index of one element to be swapped.
     * @param  i1    index of the other element to be swapped.
     *
     * @since 0.4
     */
    public static void swap(final Object[] data, final int i0, final int i1) {
        final Object t = data[i0];
        data[i0] = data[i1];
        data[i1] = t;
    }

    /**
     * Swaps the elements at the given indices in the given array of {@code double} values.
     *
     * <div class="note"><b>Note:</b>
     * While trivial, this method is provided because its need occurs relatively often
     * and the availability of a {@code swap} method makes the code easier to read.</div>
     *
     * @param  data the array in which to swap elements.
     * @param  i0   index of one element to be swapped.
     * @param  i1   index of the other element to be swapped.
     *
     * @since 0.4
     */
    public static void swap(final double[] data, final int i0, final int i1) {
        final double t = data[i0];
        data[i0] = data[i1];
        data[i1] = t;
    }

    /**
     * Swaps the elements at the given indices in the given array of {@code float} values.
     *
     * @param  data  the array in which to swap elements.
     * @param  i0   index of one element to be swapped.
     * @param  i1   index of the other element to be swapped.
     *
     * @since 0.4
     */
    public static void swap(final float[] data, final int i0, final int i1) {
        final float t = data[i0];
        data[i0] = data[i1];
        data[i1] = t;
    }

    /**
     * Swaps the elements at the given indices in the given array of {@code long} values.
     *
     * @param  data the array in which to swap elements.
     * @param  i0   index of one element to be swapped.
     * @param  i1   index of the other element to be swapped.
     *
     * @since 0.4
     */
    public static void swap(final long[] data, final int i0, final int i1) {
        final long t = data[i0];
        data[i0] = data[i1];
        data[i1] = t;
    }

    /**
     * Swaps the elements at the given indices in the given array of {@code int} values.
     *
     * <div class="note"><b>Note:</b>
     * While trivial, this method is provided because its need occurs relatively often
     * and the availability of a {@code swap} method makes the code easier to read.</div>
     *
     * @param  data the array in which to swap elements.
     * @param  i0   index of one element to be swapped.
     * @param  i1   index of the other element to be swapped.
     *
     * @since 0.4
     */
    public static void swap(final int[] data, final int i0, final int i1) {
        final int t = data[i0];
        data[i0] = data[i1];
        data[i1] = t;
    }

    /**
     * Swaps the elements at the given indices in the given array of {@code short} values.
     *
     * @param  data the array in which to swap elements.
     * @param  i0   index of one element to be swapped.
     * @param  i1   index of the other element to be swapped.
     *
     * @since 0.4
     */
    public static void swap(final short[] data, final int i0, final int i1) {
        final short t = data[i0];
        data[i0] = data[i1];
        data[i1] = t;
    }

    /**
     * Swaps the elements at the given indices in the given array of {@code byte} values.
     *
     * @param  data the array in which to swap elements.
     * @param  i0   index of one element to be swapped.
     * @param  i1   index of the other element to be swapped.
     *
     * @since 0.4
     */
    public static void swap(final byte[] data, final int i0, final int i1) {
        final byte t = data[i0];
        data[i0] = data[i1];
        data[i1] = t;
    }

    /**
     * Swaps the elements at the given indices in the given array of {@code char} values.
     *
     * @param  data the array in which to swap elements.
     * @param  i0   index of one element to be swapped.
     * @param  i1   index of the other element to be swapped.
     *
     * @since 0.4
     */
    public static void swap(final char[] data, final int i0, final int i1) {
        final char t = data[i0];
        data[i0] = data[i1];
        data[i1] = t;
    }

    /**
     * Replaces all occurrences of the given value by the given replacement.
     * This method compares the values using {@link Double#doubleToRawLongBits(double)}:
     *
     * <ul>
     *   <li>Positive zero is considered different then negative zero.</li>
     *   <li>The {@linkplain org.apache.sis.math.MathFunctions#toNanFloat(int) various
     *       possible NaN values} are considered different.</li>
     * </ul>
     *
     * A common usage for this method is to replace pad values by {@link Double#NaN} in the
     * sample values of a {@linkplain org.apache.sis.coverage.grid.GridCoverage grid coverage}.
     * This method does nothing if the given array is {@code null} or if {@code search} is the
     * same bits pattern as {@code replacement}.
     *
     * @param  array        the array where to perform the search and replace, or {@code null}.
     * @param  search       the value to search.
     * @param  replacement  the replacement.
     *
     * @since 1.0
     */
    public static void replace(final double[] array, final double search, final double replacement) {
        if (array != null) {
            final long bits = Double.doubleToRawLongBits(search);
            if (bits != Double.doubleToRawLongBits(replacement)) {
                for (int i=0; i<array.length; i++) {
                    if (Double.doubleToRawLongBits(array[i]) == bits) {
                        array[i] = replacement;
                    }
                }
            }
        }
    }

    /**
     * Replaces all occurrences of the given value by the given replacement.
     * This method compares the values using {@link Float#floatToRawIntBits(float)}:
     *
     * <ul>
     *   <li>Positive zero is considered different then negative zero.</li>
     *   <li>The {@linkplain org.apache.sis.math.MathFunctions#toNanFloat(int) various
     *       possible NaN values} are considered different.</li>
     * </ul>
     *
     * A common usage for this method is to replace pad values by {@link Float#NaN} in the
     * sample values of a {@linkplain org.apache.sis.coverage.grid.GridCoverage grid coverage}.
     * This method does nothing if the given array is {@code null} or if {@code search} is the
     * same bits pattern as {@code replacement}.
     *
     * @param  array        the array where to perform the search and replace, or {@code null}.
     * @param  search       the value to search.
     * @param  replacement  the replacement.
     *
     * @since 1.0
     */
    public static void replace(final float[] array, final float search, final float replacement) {
        if (array != null) {
            final int bits = Float.floatToRawIntBits(search);
            if (bits != Float.floatToRawIntBits(replacement)) {
                for (int i=0; i<array.length; i++) {
                    if (Float.floatToRawIntBits(array[i]) == bits) {
                        array[i] = replacement;
                    }
                }
            }
        }
    }

    /**
     * Returns a copy of the given array where each value has been casted to the {@code long} type.
     *
     * @param  data  the array to copy, or {@code null}.
     * @return a copy of the given array with values casted to the {@code long} type,
     *         or {@code null} if the given array was null.
     *
     * @since 1.1
     */
    public static long[] copyAsLongs(final int[] data) {
        if (data == null) return null;
        final long[] result = new long[data.length];
        for (int i=0; i<data.length; i++) {
            result[i] = data[i];
        }
        return result;
    }

    /**
     * Returns a copy of the given array where each value has been casted to the {@code double} type.
     * This method does not verify if the casts would cause data loss.
     *
     * @param  data  the array to copy, or {@code null}.
     * @return a copy of the given array with values casted to the {@code double} type,
     *         or {@code null} if the given array was null.
     *
     * @since 1.5
     */
    public static double[] copyAsDoubles(final long[] data) {
        if (data == null) return null;
        final double[] result = new double[data.length];
        for (int i=0; i<data.length; i++) {
            result[i] = data[i];
        }
        return result;
    }

    /**
     * Returns a copy of the given array where each value has been casted to the {@code float} type.
     * This method does not verify if the casts would cause data loss.
     *
     * @param  data  the array to copy, or {@code null}.
     * @return a copy of the given array with values casted to the {@code float} type,
     *         or {@code null} if the given array was null.
     *
     * @since 1.0
     */
    public static float[] copyAsFloats(final double[] data) {
        if (data == null) return null;
        final float[] result = new float[data.length];
        for (int i=0; i<data.length; i++) {
            result[i] = (float) data[i];
        }
        return result;
    }

    /**
     * Returns a copy of the given array where each value has been casted to the {@code float} type,
     * but only if all casts are lossless. If any cast causes data loss, then this method returns {@code null}.
     * This method is equivalent to the following code, but potentially more efficient:
     *
     * {@snippet lang="java" :
     *     if (isSinglePrecision(data)) {
     *         return copyAsFloat(data);
     *     } else {
     *         return null;
     *     }
     *     }
     *
     * @param  data  the array to copy, or {@code null}.
     * @return a copy of the given array with values casted to the {@code float} type, or
     *         {@code null} if the given array was null or if a cast would cause data lost.
     *
     * @since 1.0
     */
    public static float[] copyAsFloatsIfLossless(final double[] data) {
        if (data == null) return null;
        /*
         * Before to allocate a new array, performs a quick sampling of a few values.
         * Basically the first value, the last value, a value in the middle and a few others.
         */
        int i = data.length - 1;
        if (i < 0) {
            return ArraysExt.EMPTY_FLOAT;
        }
        for (;;) {
            final double d = data[i];
            if (Double.doubleToRawLongBits(d) != Double.doubleToRawLongBits((float) d)) {
                return null;
            }
            if (i == 0) break;
            i >>>= 1;
        }
        /*
         * At this point the quick sampling found no data loss. We can now allocate the array,
         * but we will still need to check for each value, which may interrupt the copy at any time.
         */
        final float[] result = new float[data.length];
        for (i = data.length; --i >= 0;) {
            final double d = data[i];
            final float  f = (float) d;
            if (Double.doubleToRawLongBits(d) != Double.doubleToRawLongBits(f)) {
                return null;
            }
            result[i] = f;
        }
        return result;
    }

    /**
     * Returns {@code true} if every values in the given {@code double} array could be casted to the
     * {@code float} type without data lost. If this method returns {@code true}, then the array can
     * be converted to the {@code float[]} type with {@link #copyAsFloats(double[])} and the exact
     * same {@code double} values can still be obtained by casting back each {@code float} value
     * to {@code double}.
     *
     * @param  values  the values to test for their precision, or {@code null}.
     * @return {@code true} if every values can be casted to the {@code float} type without data lost.
     *
     * @since 1.0
     */
    public static boolean isSinglePrecision(final double... values) {
        if (values != null) {
            for (final double value : values) {
                if (Double.doubleToRawLongBits(value) != Double.doubleToRawLongBits((float) value)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if the specified array contains at least one {@link Double#NaN NaN} value.
     *
     * @param  array  the array to check, or {@code null}.
     * @return {@code true} if the given array is non-null and contains at least one NaN value.
     */
    public static boolean hasNaN(final double[] array) {
        if (array != null) {
            for (int i=0; i<array.length; i++) {
                if (Double.isNaN(array[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the specified array contains at least one {@link Float#NaN NaN} value.
     *
     * @param  array  the array to check, or {@code null}.
     * @return {@code true} if the given array is non-null and contains at least one NaN value.
     */
    public static boolean hasNaN(final float[] array) {
        if (array != null) {
            for (int i=0; i<array.length; i++) {
                if (Float.isNaN(array[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if all values in the specified array are equal to the specified value,
     * which may be {@code null}. If the given array is empty, then this method returns {@code true}.
     *
     * @param  array  the array to check.
     * @param  value  the expected value.
     * @return {@code true} if all elements in the given array are equal to the given value.
     *
     * @since 0.8
     */
    public static boolean allEquals(final Object[] array, final Object value) {
        if (value == null) {
            for (int i=0; i<array.length; i++) {
                if (array[i] != null) {
                    return false;
                }
            }
        } else {
            for (int i=0; i<array.length; i++) {
                if (!value.equals(array[i])) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if all values in the specified array are equal to the specified value,
     * which may be {@link Double#NaN}. A NaN value is considered equal to all other NaN values.
     *
     * @param  array  the array to check.
     * @param  value  the expected value.
     * @return {@code true} if all elements in the given array are equal to the given value.
     */
    public static boolean allEquals(final double[] array, final double value) {
        if (Double.isNaN(value)) {
            for (int i=0; i<array.length; i++) {
                if (!Double.isNaN(array[i])) {
                    return false;
                }
            }
        } else {
            for (int i=0; i<array.length; i++) {
                if (array[i] != value) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if all values in the specified array are equal to the specified value,
     * which may be {@link Float#NaN}.
     *
     * @param  array  the array to check.
     * @param  value  the expected value.
     * @return {@code true} if all elements in the given array are equal to the given value.
     */
    public static boolean allEquals(final float[] array, final float value) {
        if (Float.isNaN(value)) {
            for (int i=0; i<array.length; i++) {
                if (!Float.isNaN(array[i])) {
                    return false;
                }
            }
        } else {
            for (int i=0; i<array.length; i++) {
                if (array[i] != value) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if all values in the specified array are equal to the specified value.
     *
     * @param  array  the array to check.
     * @param  value  the expected value.
     * @return {@code true} if all elements in the given array are equal to the given value.
     *
     * @since 1.2
     */
    public static boolean allEquals(final int[] array, final int value) {
        for (int i=0; i<array.length; i++) {
            if (array[i] != value) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if the specified array contains the specified value, ignoring case.
     * This method should be used only for very small arrays.
     *
     * @param  array  the array to search in. May be {@code null}.
     * @param  value  the value to search.
     * @return {@code true} if the array is non-null and contains the given value, or {@code false} otherwise.
     */
    public static boolean containsIgnoreCase(final String[] array, final String value) {
        if (array != null) {
            for (final String element : array) {
                if (value.equalsIgnoreCase(element)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the specified array contains the specified reference.
     * The comparisons are performed using the {@code ==} operator.
     *
     * <p>This method should be used only for very small arrays, or for searches to be performed
     * only once, because it performs a linear search. If more than one search need to be done
     * on the same array, consider using {@link java.util.IdentityHashMap} instead.</p>
     *
     * @param  array  the array to search in. May be {@code null} and may contains null elements.
     * @param  value  the value to search. May be {@code null}.
     * @return {@code true} if the array is non-null and contains the value (which may be null),
     *         or {@code false} otherwise.
     */
    public static boolean containsIdentity(final Object[] array, final Object value) {
        if (array != null) {
            for (final Object element : array) {
                if (element == value) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the specified array contains the specified value.
     * The comparisons are performed using the {@link Object#equals(Object)} method.
     *
     * <p>This method should be used only for very small arrays, or for searches to be performed
     * only once, because it performs a linear search. If more than one search need to be done
     * on the same array, consider using {@link java.util.HashSet} instead.</p>
     *
     * @param  array  the array to search in. May be {@code null} and may contains null elements.
     * @param  value  the value to search. May be {@code null}.
     * @return {@code true} if the array is non-null and contains the value (which may be null),
     *         or {@code false} otherwise.
     *
     * @see #intersects(Object[], Object[])
     */
    public static boolean contains(final Object[] array, final Object value) {
        if (array != null) {
            for (final Object element : array) {
                if (Objects.equals(element, value)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if at least one element in the first array is {@linkplain Object#equals
     * equals} to an element in the second array. The element doesn't need to be at the same index
     * in both array.
     *
     * <p>This method should be used only for very small arrays since it may be very slow. If the
     * arrays are large or if an array will be involved in more than one search, consider using
     * {@link java.util.HashSet} instead.</p>
     *
     * @param  array1  the first array, or {@code null}.
     * @param  array2  the second array, or {@code null}.
     * @return {@code true} if both array are non-null and have at least one element in common.
     *
     * @see #contains(Object[], Object)
     */
    public static boolean intersects(final Object[] array1, final Object[] array2) {
        if (array1 != null) {
            for (final Object element : array1) {
                if (contains(array2, element)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the union of two sorted arrays. The input arrays shall be sorted in strictly increasing order.
     * The output array is the union of the input arrays without duplicated values,
     * with elements sorted in strictly increasing order.
     *
     * <h4>Recommended assertions</h4>
     * Callers are encouraged to place the following assertions before calls to this method,
     * using the {@link #isSorted(int[], boolean)} and {@link Arrays#toString(int[])} methods:
     *
     * {@snippet lang="java" :
     *   assert isSorted(array1, true) : toString(array1);
     *   assert isSorted(array2, true) : toString(array2);
     *   }
     *
     * @param  array1  the first array, or {@code null}.
     * @param  array2  the second array, or {@code null}.
     * @return the union of the given array without duplicated values, or {@code null}
     *         if the two given arrays were null. May be one of the given arrays.
     *
     * @see #concatenate(Object[][])
     */
    public static int[] unionOfSorted(final int[] array1, final int[] array2) {
        if (array1 == null) return array2;
        if (array2 == null) return array1;
        int[] union = new int[Math.addExact(array1.length, array2.length)];
        int nu=0;
        for (int ix=0, iy=0;;) {
            if (ix == array1.length) {
                final int no = array2.length - iy;
                System.arraycopy(array2, iy, union, nu, no);
                nu += no;
                break;
            }
            if (iy == array2.length) {
                final int no = array1.length - ix;
                System.arraycopy(array1, ix, union, nu, no);
                nu += no;
                break;
            }
            final int sx = array1[ix];
            final int sy = array2[iy];
            final int s;
            if (sx <= sy) {
                s = sx;
                ix++;
                if (sx == sy) {
                    iy++;
                }
            } else {
                s = sy;
                iy++;
            }
            union[nu++] = s;
        }
        union = resize(union, nu);
        return union;
    }
}
