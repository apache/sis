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
import java.util.Comparator;
import java.lang.reflect.Array;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


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
 * <div class="section">Handling of null values</div>
 * Many (but not all) methods in this class are tolerant to null parameter values,
 * sometime under certain conditions. See the method javadoc for details.
 *
 * <p>All methods in this class are tolerant to null elements in arrays.
 * Null and {@linkplain Double#NaN NaN} elements are ignored.</p>
 *
 * <div class="section">Performance consideration</div>
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
 * @since   0.3
 * @version 0.4
 * @module
 *
 * @see Arrays
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
     * @param  <E> The array elements.
     * @param  array  Array to resize, or {@code null}.
     * @param  length Length of the desired array.
     * @return A new array of the requested length, or {@code array} if the given
     *         array is {@code null} or already have the requested length.
     * @throws NegativeArraySizeException If {@code length} is negative.
     *
     * @see Arrays#copyOf(Object[], int)
     */
    public static <E> E[] resize(final E[] array, final int length) throws NegativeArraySizeException {
        return (array == null || array.length == length) ? array : Arrays.copyOf(array, length);
    }

    /**
     * Returns an array containing the same elements as the given {@code array} but
     * specified {@code length}, truncating or padding with zeros if necessary.
     *
     * <p>This method returns {@code null} if and only if the given array is {@code null},
     * in which case the value of the {@code length} argument is ignored.</p>
     *
     * @param  array  Array to resize, or {@code null}.
     * @param  length Length of the desired array.
     * @return A new array of the requested length, or {@code array} if the given
     *         array is {@code null} or already have the requested length.
     * @throws NegativeArraySizeException If {@code length} is negative.
     *
     * @see Arrays#copyOf(double[], int)
     */
    public static double[] resize(final double[] array, final int length) throws NegativeArraySizeException {
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
     *
     * <p>This method returns {@code null} if and only if the given array is {@code null},
     * in which case the value of the {@code length} argument is ignored.</p>
     *
     * @param  array  Array to resize, or {@code null}.
     * @param  length Length of the desired array.
     * @return A new array of the requested length, or {@code array} if the given
     *         array is {@code null} or already have the requested length.
     * @throws NegativeArraySizeException If {@code length} is negative.
     *
     * @see Arrays#copyOf(float[], int)
     */
    public static float[] resize(final float[] array, final int length) throws NegativeArraySizeException {
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
     *
     * <p>This method returns {@code null} if and only if the given array is {@code null},
     * in which case the value of the {@code length} argument is ignored.</p>
     *
     * @param  array  Array to resize, or {@code null}.
     * @param  length Length of the desired array.
     * @return A new array of the requested length, or {@code array} if the given
     *         array is {@code null} or already have the requested length.
     * @throws NegativeArraySizeException If {@code length} is negative.
     *
     * @see Arrays#copyOf(long[], int)
     */
    public static long[] resize(final long[] array, final int length) throws NegativeArraySizeException {
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
     *
     * <p>This method returns {@code null} if and only if the given array is {@code null},
     * in which case the value of the {@code length} argument is ignored.</p>
     *
     * @param  array  Array to resize, or {@code null}.
     * @param  length Length of the desired array.
     * @return A new array of the requested length, or {@code array} if the given
     *         array is {@code null} or already have the requested length.
     * @throws NegativeArraySizeException If {@code length} is negative.
     *
     * @see Arrays#copyOf(int[], int)
     */
    public static int[] resize(final int[] array, final int length) throws NegativeArraySizeException {
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
     *
     * <p>This method returns {@code null} if and only if the given array is {@code null},
     * in which case the value of the {@code length} argument is ignored.</p>
     *
     * @param  array  Array to resize, or {@code null}.
     * @param  length Length of the desired array.
     * @return A new array of the requested length, or {@code array} if the given
     *         array is {@code null} or already have the requested length.
     * @throws NegativeArraySizeException If {@code length} is negative.
     *
     * @see Arrays#copyOf(short[], int)
     */
    public static short[] resize(final short[] array, final int length) throws NegativeArraySizeException {
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
     *
     * <p>This method returns {@code null} if and only if the given array is {@code null},
     * in which case the value of the {@code length} argument is ignored.</p>
     *
     * @param  array  Array to resize, or {@code null}.
     * @param  length Length of the desired array.
     * @return A new array of the requested length, or {@code array} if the given
     *         array is {@code null} or already have the requested length.
     * @throws NegativeArraySizeException If {@code length} is negative.
     *
     * @see Arrays#copyOf(byte[], int)
     */
    public static byte[] resize(final byte[] array, final int length) throws NegativeArraySizeException {
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
     *
     * <p>This method returns {@code null} if and only if the given array is {@code null},
     * in which case the value of the {@code length} argument is ignored.</p>
     *
     * @param  array  Array to resize, or {@code null}.
     * @param  length Length of the desired array.
     * @return A new array of the requested length, or {@code array} if the given
     *         array is {@code null} or already have the requested length.
     * @throws NegativeArraySizeException If {@code length} is negative.
     *
     * @see Arrays#copyOf(char[], int)
     */
    public static char[] resize(final char[] array, final int length) throws NegativeArraySizeException {
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
     *
     * <p>This method returns {@code null} if and only if the given array is {@code null},
     * in which case the value of the {@code length} argument is ignored.</p>
     *
     * @param  array  Array to resize, or {@code null}.
     * @param  length Length of the desired array.
     * @return A new array of the requested length, or {@code array} if the given
     *         array is {@code null} or already have the requested length.
     * @throws NegativeArraySizeException If {@code length} is negative.
     *
     * @see Arrays#copyOf(boolean[], int)
     */
    public static boolean[] resize(final boolean[] array, final int length) throws NegativeArraySizeException {
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
     * Returns an array containing the same elements than the given array except for
     * the given range.
     *
     * <p>If the {@code length} argument is 0, then this method returns the {@code array}
     * reference unchanged. Otherwise this method creates a new array.
     * In every cases, the given array is never modified.</p>
     *
     * @param <T>     The array type.
     * @param array   Array from which to remove elements. Can be {@code null} only if {@code length} is 0.
     * @param first   Index of the first element to remove from the given {@code array}.
     * @param length  Number of elements to remove.
     * @return        Array with the same elements than the given {@code array} except for the
     *                removed elements, or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullArgumentException     If {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException  If {@code length} is negative.
     * @throws IndexOutOfBoundsException If {@code first} or {@code first+length} is out of array bounds.
     */
    @SuppressWarnings("SuspiciousSystemArraycopy")
    private static <T> T doRemove(final T array, final int first, final int length)
            throws NullArgumentException, IllegalArgumentException, IndexOutOfBoundsException
    {
        if (length == 0) {
            return array; // May be null
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
     * Returns an array containing the same elements than the given array except for
     * the given range.
     *
     * <p>If the {@code length} argument is 0, then this method returns
     * the {@code array} reference unchanged (except if empty). Otherwise this method
     * creates a new array. In every cases, the given array is never modified.</p>
     *
     * @param <E>     The type of array elements.
     * @param array   Array from which to remove elements. Can be {@code null} only if {@code length} is 0.
     * @param first   Index of the first element to remove from the given {@code array}.
     * @param length  Number of elements to remove.
     * @return        Array with the same elements than the given {@code array} except for the
     *                removed elements, or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullArgumentException     If {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException  If {@code length} is negative.
     * @throws IndexOutOfBoundsException If {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(Object[], int, int)
     */
    public static <E> E[] remove(final E[] array, final int first, final int length)
            throws NullArgumentException, IllegalArgumentException, IndexOutOfBoundsException
    {
        return doRemove(array, first, length);
    }

    /**
     * Returns an array containing the same elements than the given array except for
     * the given range.
     *
     * <p>If the {@code length} argument is 0, then this method returns the {@code array}
     * reference unchanged, except for {@linkplain #EMPTY_DOUBLE empty} arrays. Otherwise
     * this method creates a new array. In every cases, the given array is never modified.</p>
     *
     * @param array   Array from which to remove elements. Can be {@code null} only if {@code length} is 0.
     * @param first   Index of the first element to remove from the given {@code array}.
     * @param length  Number of elements to remove.
     * @return        Array with the same elements than the given {@code array} except for the
     *                removed elements, or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullArgumentException     If {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException  If {@code length} is negative.
     * @throws IndexOutOfBoundsException If {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(double[], int, int)
     */
    public static double[] remove(final double[] array, final int first, final int length)
            throws NullArgumentException, IllegalArgumentException, IndexOutOfBoundsException
    {
        return (first == 0 && array != null && length == array.length)
                ? EMPTY_DOUBLE : doRemove(array, first, length);
    }

    /**
     * Returns an array containing the same elements than the given array except for
     * the given range.
     *
     * <p>If the {@code length} argument is 0, then this method returns the {@code array}
     * reference unchanged, except for {@linkplain #EMPTY_FLOAT empty} arrays. Otherwise
     * this method creates a new array. In every cases, the given array is never modified.</p>
     *
     * @param array   Array from which to remove elements. Can be {@code null} only if {@code length} is 0.
     * @param first   Index of the first element to remove from the given {@code array}.
     * @param length  Number of elements to remove.
     * @return        Array with the same elements than the given {@code array} except for the
     *                removed elements, or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullArgumentException     If {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException  If {@code length} is negative.
     * @throws IndexOutOfBoundsException If {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(float[], int, int)
     */
    public static float[] remove(final float[] array, final int first, final int length)
            throws NullArgumentException, IllegalArgumentException, IndexOutOfBoundsException
    {
        return (first == 0 && array != null && length == array.length)
                ? EMPTY_FLOAT : doRemove(array, first, length);
    }

    /**
     * Returns an array containing the same elements than the given array except for
     * the given range.
     *
     * <p>If the {@code length} argument is 0, then this method returns the {@code array}
     * reference unchanged, except for {@linkplain #EMPTY_LONG empty} arrays. Otherwise
     * this method creates a new array. In every cases, the given array is never modified.</p>
     *
     * @param array   Array from which to remove elements. Can be {@code null} only if {@code length} is 0.
     * @param first   Index of the first element to remove from the given {@code array}.
     * @param length  Number of elements to remove.
     * @return        Array with the same elements than the given {@code array} except for the
     *                removed elements, or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullArgumentException     If {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException  If {@code length} is negative.
     * @throws IndexOutOfBoundsException If {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(long[], int, int)
     */
    public static long[] remove(final long[] array, final int first, final int length)
            throws NullArgumentException, IllegalArgumentException, IndexOutOfBoundsException
    {
        return (first == 0 && array != null && length == array.length)
                ? EMPTY_LONG : doRemove(array, first, length);
    }

    /**
     * Returns an array containing the same elements than the given array except for
     * the given range.
     *
     * <p>If the {@code length} argument is 0, then this method returns the {@code array}
     * reference unchanged, except for {@linkplain #EMPTY_INT empty} arrays. Otherwise
     * this method creates a new array. In every cases, the given array is never modified.</p>
     *
     * @param array   Array from which to remove elements. Can be {@code null} only if {@code length} is 0.
     * @param first   Index of the first element to remove from the given {@code array}.
     * @param length  Number of elements to remove.
     * @return        Array with the same elements than the given {@code array} except for the
     *                removed elements, or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullArgumentException     If {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException  If {@code length} is negative.
     * @throws IndexOutOfBoundsException If {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(int[], int, int)
     */
    public static int[] remove(final int[] array, final int first, final int length)
            throws NullArgumentException, IllegalArgumentException, IndexOutOfBoundsException
    {
        return (first == 0 && array != null && length == array.length)
                ? EMPTY_INT : doRemove(array, first, length);
    }

    /**
     * Returns an array containing the same elements than the given array except for
     * the given range.
     *
     * <p>If the {@code length} argument is 0, then this method returns the {@code array}
     * reference unchanged, except for {@linkplain #EMPTY_SHORT empty} arrays. Otherwise
     * this method creates a new array. In every cases, the given array is never modified.</p>
     *
     * @param array   Array from which to remove elements. Can be {@code null} only if {@code length} is 0.
     * @param first   Index of the first element to remove from the given {@code array}.
     * @param length  Number of elements to remove.
     * @return        Array with the same elements than the given {@code array} except for the
     *                removed elements, or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullArgumentException     If {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException  If {@code length} is negative.
     * @throws IndexOutOfBoundsException If {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(short[], int, int)
     */
    public static short[] remove(final short[] array, final int first, final int length)
            throws NullArgumentException, IllegalArgumentException, IndexOutOfBoundsException
    {
        return (first == 0 && array != null && length == array.length) ?
                EMPTY_SHORT : doRemove(array, first, length);
    }

    /**
     * Returns an array containing the same elements than the given array except for
     * the given range.
     *
     * <p>If the {@code length} argument is 0, then this method returns the {@code array}
     * reference unchanged, except for {@linkplain #EMPTY_BYTE empty} arrays. Otherwise
     * this method creates a new array. In every cases, the given array is never modified.</p>
     *
     * @param array   Array from which to remove elements. Can be {@code null} only if {@code length} is 0.
     * @param first   Index of the first element to remove from the given {@code array}.
     * @param length  Number of elements to remove.
     * @return        Array with the same elements than the given {@code array} except for the
     *                removed elements, or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullArgumentException     If {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException  If {@code length} is negative.
     * @throws IndexOutOfBoundsException If {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(byte[], int, int)
     */
    public static byte[] remove(final byte[] array, final int first, final int length)
            throws NullArgumentException, IllegalArgumentException, IndexOutOfBoundsException
    {
        return (first == 0 && array != null && length == array.length)
                ? EMPTY_BYTE : doRemove(array, first, length);
    }

    /**
     * Returns an array containing the same elements than the given array except for
     * the given range.
     *
     * <p>If the {@code length} argument is 0, then this method returns the {@code array}
     * reference unchanged, except for {@linkplain #EMPTY_CHAR empty} arrays. Otherwise
     * this method creates a new array. In every cases, the given array is never modified.</p>
     *
     * @param array   Array from which to remove elements. Can be {@code null} only if {@code length} is 0.
     * @param first   Index of the first element to remove from the given {@code array}.
     * @param length  Number of elements to remove.
     * @return        Array with the same elements than the given {@code array} except for the
     *                removed elements, or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullArgumentException     If {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException  If {@code length} is negative.
     * @throws IndexOutOfBoundsException If {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(char[], int, int)
     */
    public static char[] remove(final char[] array, final int first, final int length)
            throws NullArgumentException, IllegalArgumentException, IndexOutOfBoundsException
    {
        return (first == 0 && array != null && length == array.length)
                ? EMPTY_CHAR : doRemove(array, first, length);
    }

    /**
     * Returns an array containing the same elements than the given array except for
     * the given range.
     *
     * <p>If the {@code length} argument is 0, then this method returns the {@code array}
     * reference unchanged, except for {@linkplain #EMPTY_BOOLEAN empty} arrays. Otherwise
     * this method creates a new array. In every cases, the given array is never modified.</p>
     *
     * @param array   Array from which to remove elements. Can be {@code null} only if {@code length} is 0.
     * @param first   Index of the first element to remove from the given {@code array}.
     * @param length  Number of elements to remove.
     * @return        Array with the same elements than the given {@code array} except for the
     *                removed elements, or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullArgumentException     If {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException  If {@code length} is negative.
     * @throws IndexOutOfBoundsException If {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(boolean[], int, int)
     */
    public static boolean[] remove(final boolean[] array, final int first, final int length)
            throws NullArgumentException, IllegalArgumentException, IndexOutOfBoundsException
    {
        return (first == 0 && array != null && length == array.length)
                ? EMPTY_BOOLEAN : doRemove(array, first, length);
    }

    /**
     * Returns an array containing the same elements than the given array, with additional
     * "spaces" in the given range. These "spaces" will be made up of {@code null} elements.
     *
     * <p>If the {@code length} argument is 0, then this method returns the {@code array}
     * reference unchanged. Otherwise this method creates a new array. In every cases,
     * the given array is never modified.</p>
     *
     * @param <T>     The array type.
     * @param array   Array in which to insert spaces. Can be {@code null} only if {@code length} is 0.
     * @param first   Index where the first space should be inserted. All {@code array} elements
     *                having an index equal to or higher than {@code index} will be moved forward.
     * @param length  Number of spaces to insert.
     * @return        Array containing the {@code array} elements with the additional space
     *                inserted, or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullArgumentException     If {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException  If {@code length} is negative.
     * @throws IndexOutOfBoundsException If {@code first} or {@code first+length} is out of array bounds.
     */
    @SuppressWarnings("SuspiciousSystemArraycopy")
    private static <T> T doInsert(final T array, final int first, final int length)
            throws NullArgumentException, IllegalArgumentException, IndexOutOfBoundsException
    {
        if (length == 0) {
            return array; // May be null
        }
        ArgumentChecks.ensureNonNull ("array",  array);
        ArgumentChecks.ensurePositive("length", length);
        final int arrayLength = Array.getLength(array);
        @SuppressWarnings("unchecked")
        final T newArray = (T) Array.newInstance(array.getClass().getComponentType(), arrayLength + length);
        System.arraycopy(array, 0,     newArray, 0,            first            );
        System.arraycopy(array, first, newArray, first+length, arrayLength-first);
        return newArray;
    }

    /**
     * Returns an array containing the same elements than the given array, with additional
     * "spaces" in the given range. These "spaces" will be made up of {@code null} elements.
     *
     * <p>If the {@code length} argument is 0, then this method returns the {@code array}
     * reference unchanged. Otherwise this method creates a new array. In every cases,
     * the given array is never modified.</p>
     *
     * @param <E>     The type of array elements.
     * @param array   Array in which to insert spaces. Can be {@code null} only if {@code length} is 0.
     * @param first   Index where the first space should be inserted. All {@code array} elements
     *                having an index equal to or higher than {@code index} will be moved forward.
     * @param length  Number of spaces to insert.
     * @return        Array containing the {@code array} elements with the additional space
     *                inserted, or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullArgumentException     If {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException  If {@code length} is negative.
     * @throws IndexOutOfBoundsException If {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(Object[], int, Object[], int, int)
     * @see #remove(Object[], int, int)
     */
    public static <E> E[] insert(final E[] array, final int first, final int length)
            throws NullArgumentException, IllegalArgumentException, IndexOutOfBoundsException
    {
        return doInsert(array, first, length);
    }

    /**
     * Returns an array containing the same elements than the given array, with additional
     * "spaces" in the given range. These "spaces" will be made up of elements initialized
     * to zero.
     *
     * <p>If the {@code length} argument is 0, then this method returns the {@code array}
     * reference unchanged. Otherwise this method creates a new array. In every cases,
     * the given array is never modified.</p>
     *
     * @param array   Array in which to insert spaces. Can be {@code null} only if {@code length} is 0.
     * @param first   Index where the first space should be inserted. All {@code array} elements
     *                having an index equal to or higher than {@code index} will be moved forward.
     * @param length  Number of spaces to insert.
     * @return        Array containing the {@code array} elements with the additional space
     *                inserted, or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullArgumentException     If {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException  If {@code length} is negative.
     * @throws IndexOutOfBoundsException If {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(double[], int, double[], int, int)
     * @see #remove(double[], int, int)
     */
    public static double[] insert(final double[] array, final int first, final int length)
            throws NullArgumentException, IllegalArgumentException, IndexOutOfBoundsException
    {
        return doInsert(array, first, length);
    }

    /**
     * Returns an array containing the same elements than the given array, with additional
     * "spaces" in the given range. These "spaces" will be made up of elements initialized
     * to zero.
     *
     * <p>If the {@code length} argument is 0, then this method returns the {@code array}
     * reference unchanged. Otherwise this method creates a new array. In every cases,
     * the given array is never modified.</p>
     *
     * @param array   Array in which to insert spaces. Can be {@code null} only if {@code length} is 0.
     * @param first   Index where the first space should be inserted. All {@code array} elements
     *                having an index equal to or higher than {@code index} will be moved forward.
     * @param length  Number of spaces to insert.
     * @return        Array containing the {@code array} elements with the additional space
     *                inserted, or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullArgumentException     If {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException  If {@code length} is negative.
     * @throws IndexOutOfBoundsException If {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(float[], int, float[], int, int)
     * @see #remove(float[], int, int)
     */
    public static float[] insert(final float[] array, final int first, final int length)
            throws NullArgumentException, IllegalArgumentException, IndexOutOfBoundsException
    {
        return doInsert(array, first, length);
    }

    /**
     * Returns an array containing the same elements than the given array, with additional
     * "spaces" in the given range. These "spaces" will be made up of elements initialized
     * to zero.
     *
     * <p>If the {@code length} argument is 0, then this method returns the {@code array}
     * reference unchanged. Otherwise this method creates a new array. In every cases,
     * the given array is never modified.</p>
     *
     * @param array   Array in which to insert spaces. Can be {@code null} only if {@code length} is 0.
     * @param first   Index where the first space should be inserted. All {@code array} elements
     *                having an index equal to or higher than {@code index} will be moved forward.
     * @param length  Number of spaces to insert.
     * @return        Array containing the {@code array} elements with the additional space
     *                inserted, or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullArgumentException     If {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException  If {@code length} is negative.
     * @throws IndexOutOfBoundsException If {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(long[], int, long[], int, int)
     * @see #remove(long[], int, int)
     */
    public static long[] insert(final long[] array, final int first, final int length)
            throws NullArgumentException, IllegalArgumentException, IndexOutOfBoundsException
    {
        return doInsert(array, first, length);
    }

    /**
     * Returns an array containing the same elements than the given array, with additional
     * "spaces" in the given range. These "spaces" will be made up of elements initialized
     * to zero.
     *
     * <p>If the {@code length} argument is 0, then this method returns the {@code array}
     * reference unchanged. Otherwise this method creates a new array. In every cases,
     * the given array is never modified.</p>
     *
     * @param array   Array in which to insert spaces. Can be {@code null} only if {@code length} is 0.
     * @param first   Index where the first space should be inserted. All {@code array} elements
     *                having an index equal to or higher than {@code index} will be moved forward.
     * @param length  Number of spaces to insert.
     * @return        Array containing the {@code array} elements with the additional space
     *                inserted, or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullArgumentException     If {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException  If {@code length} is negative.
     * @throws IndexOutOfBoundsException If {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(int[], int, int[], int, int)
     * @see #remove(int[], int, int)
     */
    public static int[] insert(final int[] array, final int first, final int length)
            throws NullArgumentException, IllegalArgumentException, IndexOutOfBoundsException
    {
        return doInsert(array, first, length);
    }

    /**
     * Returns an array containing the same elements than the given array, with additional
     * "spaces" in the given range. These "spaces" will be made up of elements initialized
     * to zero.
     *
     * <p>If the {@code length} argument is 0, then this method returns the {@code array}
     * reference unchanged. Otherwise this method creates a new array. In every cases,
     * the given array is never modified.</p>
     *
     * @param array   Array in which to insert spaces. Can be {@code null} only if {@code length} is 0.
     * @param first   Index where the first space should be inserted. All {@code array} elements
     *                having an index equal to or higher than {@code index} will be moved forward.
     * @param length  Number of spaces to insert.
     * @return        Array containing the {@code array} elements with the additional space
     *                inserted, or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullArgumentException     If {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException  If {@code length} is negative.
     * @throws IndexOutOfBoundsException If {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(short[], int, short[], int, int)
     * @see #remove(short[], int, int)
     */
    public static short[] insert(final short[] array, final int first, final int length)
            throws NullArgumentException, IllegalArgumentException, IndexOutOfBoundsException
    {
        return doInsert(array, first, length);
    }

    /**
     * Returns an array containing the same elements than the given array, with additional
     * "spaces" in the given range. These "spaces" will be made up of elements initialized
     * to zero.
     *
     * <p>If the {@code length} argument is 0, then this method returns the {@code array}
     * reference unchanged. Otherwise this method creates a new array. In every cases,
     * the given array is never modified.</p>
     *
     * @param array   Array in which to insert spaces. Can be {@code null} only if {@code length} is 0.
     * @param first   Index where the first space should be inserted. All {@code array} elements
     *                having an index equal to or higher than {@code index} will be moved forward.
     * @param length  Number of spaces to insert.
     * @return        Array containing the {@code array} elements with the additional space
     *                inserted, or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullArgumentException     If {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException  If {@code length} is negative.
     * @throws IndexOutOfBoundsException If {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(byte[], int, byte[], int, int)
     * @see #remove(byte[], int, int)
     */
    public static byte[] insert(final byte[] array, final int first, final int length)
            throws NullArgumentException, IllegalArgumentException, IndexOutOfBoundsException
    {
        return doInsert(array, first, length);
    }

    /**
     * Returns an array containing the same elements than the given array, with additional
     * "spaces" in the given range. These "spaces" will be made up of elements initialized
     * to zero.
     *
     * <p>If the {@code length} argument is 0, then this method returns the {@code array}
     * reference unchanged. Otherwise this method creates a new array. In every cases,
     * the given array is never modified.</p>
     *
     * @param array   Array in which to insert spaces. Can be {@code null} only if {@code length} is 0.
     * @param first   Index where the first space should be inserted. All {@code array} elements
     *                having an index equal to or higher than {@code index} will be moved forward.
     * @param length  Number of spaces to insert.
     * @return        Array containing the {@code array} elements with the additional space
     *                inserted, or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullArgumentException     If {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException  If {@code length} is negative.
     * @throws IndexOutOfBoundsException If {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(char[], int, char[], int, int)
     * @see #remove(char[], int, int)
     */
    public static char[] insert(final char[] array, final int first, final int length)
            throws NullArgumentException, IllegalArgumentException, IndexOutOfBoundsException
    {
        return doInsert(array, first, length);
    }

    /**
     * Returns an array containing the same elements than the given array, with additional
     * "spaces" in the given range. These "spaces" will be made up of elements initialized
     * to {@code false}.
     *
     * <p>If the {@code length} argument is 0, then this method returns the {@code array}
     * reference unchanged. Otherwise this method creates a new array. In every cases,
     * the given array is never modified.</p>
     *
     * @param array   Array in which to insert spaces. Can be {@code null} only if {@code length} is 0.
     * @param first   Index where the first space should be inserted. All {@code array} elements
     *                having an index equal to or higher than {@code index} will be moved forward.
     * @param length  Number of spaces to insert.
     * @return        Array containing the {@code array} elements with the additional space
     *                inserted, or {@code array} (which may be null) if {@code length} is 0.
     * @throws NullArgumentException     If {@code array} is null and {@code length} is different than 0.
     * @throws IllegalArgumentException  If {@code length} is negative.
     * @throws IndexOutOfBoundsException If {@code first} or {@code first+length} is out of array bounds.
     *
     * @see #insert(boolean[], int, boolean[], int, int)
     * @see #remove(boolean[], int, int)
     */
    public static boolean[] insert(final boolean[] array, final int first, final int length)
            throws NullArgumentException, IllegalArgumentException, IndexOutOfBoundsException
    {
        return doInsert(array, first, length);
    }

    /**
     * Returns an array containing the same elements than the given array, with the content
     * of an other array inserted at the given index.
     *
     * <p>If the {@code length} argument is 0, then this method returns the {@code dst}
     * reference unchanged. Otherwise this method creates a new array. In every cases,
     * the given arrays are never modified.</p>
     *
     * @param <T>     The arrays type.
     * @param src     Array to entirely or partially insert into {@code dst}.
     *                Can be null only if {@code length} is 0.
     * @param srcOff  Index of the first element of {@code src} to insert into {@code dst}.
     * @param dst     Array in which to insert {@code src} data.
     *                Can be null only if {@code length} is 0.
     * @param dstOff  Index of the first element in {@code dst} where to insert {@code src} data.
     *                All elements of {@code dst} whose index is equal to or greater than
     *                {@code dstOff} will be moved forward.
     * @param length  Number of {@code src} elements to insert.
     * @return        Array which contains the merge of {@code src} and {@code dst}. This method
     *                returns directly {@code dst} when {@code length} is zero, but never return
     *                {@code src}.
     * @throws NullArgumentException     If {@code src} or {@code dst} is null while {@code length} is different than 0.
     * @throws IllegalArgumentException  If {@code length} is negative.
     * @throws IndexOutOfBoundsException If {@code srcOff}, {@code srcOff+length} or {@code dstOff} is out of array bounds,
     */
    @SuppressWarnings("SuspiciousSystemArraycopy")
    private static <T> T doInsert(final T src, final int srcOff,
                                  final T dst, final int dstOff, final int length)
            throws NullArgumentException, IllegalArgumentException, IndexOutOfBoundsException
    {
        if (length == 0) {
            return dst; // May be null
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
     * Returns an array containing the same elements than the given array, with the content
     * of an other array inserted at the given index.
     *
     * <p>If the {@code length} argument is 0, then this method returns the {@code dst}
     * reference unchanged. Otherwise this method creates a new array. In every cases,
     * the given arrays are never modified.</p>
     *
     * @param <E>     The type of array elements.
     * @param src     Array to entirely or partially insert into {@code dst}.
     *                Can be null only if {@code length} is 0.
     * @param srcOff  Index of the first element of {@code src} to insert into {@code dst}.
     * @param dst     Array in which to insert {@code src} data.
     *                Can be null only if {@code length} is 0.
     * @param dstOff  Index of the first element in {@code dst} where to insert {@code src} data.
     *                All elements of {@code dst} whose index is equal to or greater than
     *                {@code dstOff} will be moved forward.
     * @param length  Number of {@code src} elements to insert.
     * @return        Array which contains the merge of {@code src} and {@code dst}. This method
     *                returns directly {@code dst} when {@code length} is zero, but never return
     *                {@code src}.
     * @throws NullArgumentException
     *          If {@code src} or {@code dst} is null while {@code length} is different than 0.
     * @throws IllegalArgumentException
     *          If {@code length} is negative.
     * @throws IndexOutOfBoundsException
     *          If {@code srcOff}, {@code srcOff+length} or {@code dstOff} is out of array bounds,
     */
    public static <E> E[] insert(final E[] src, final int srcOff,
                                 final E[] dst, final int dstOff, final int length)
            throws NullArgumentException, IllegalArgumentException, IndexOutOfBoundsException
    {
        return doInsert(src, srcOff, dst, dstOff, length);
    }

    /**
     * Returns an array containing the same elements than the given array, with the content
     * of an other array inserted at the given index.
     *
     * <p>If the {@code length} argument is 0, then this method returns the {@code dst}
     * reference unchanged. Otherwise this method creates a new array. In every cases,
     * the given arrays are never modified.</p>
     *
     * @param src     Array to entirely or partially insert into {@code dst}.
     *                Can be null only if {@code length} is 0.
     * @param srcOff  Index of the first element of {@code src} to insert into {@code dst}.
     * @param dst     Array in which to insert {@code src} data.
     *                Can be null only if {@code length} is 0.
     * @param dstOff  Index of the first element in {@code dst} where to insert {@code src} data.
     *                All elements of {@code dst} whose index is equal to or greater than
     *                {@code dstOff} will be moved forward.
     * @param length  Number of {@code src} elements to insert.
     * @return        Array which contains the merge of {@code src} and {@code dst}. This method
     *                returns directly {@code dst} when {@code length} is zero, but never return
     *                {@code src}.
     * @throws NullArgumentException
     *          If {@code src} or {@code dst} is null while {@code length} is different than 0.
     * @throws IllegalArgumentException
     *          If {@code length} is negative.
     * @throws IndexOutOfBoundsException
     *          If {@code srcOff}, {@code srcOff+length} or {@code dstOff} is out of array bounds,
     */
    public static double[] insert(final double[] src, final int srcOff,
                                  final double[] dst, final int dstOff, final int length)
            throws NullArgumentException, IllegalArgumentException, IndexOutOfBoundsException
    {
        return doInsert(src, srcOff, dst, dstOff, length);
    }

    /**
     * Returns an array containing the same elements than the given array, with the content
     * of an other array inserted at the given index.
     *
     * <p>If the {@code length} argument is 0, then this method returns the {@code dst}
     * reference unchanged. Otherwise this method creates a new array. In every cases,
     * the given arrays are never modified.</p>
     *
     * @param src     Array to entirely or partially insert into {@code dst}.
     *                Can be null only if {@code length} is 0.
     * @param srcOff  Index of the first element of {@code src} to insert into {@code dst}.
     * @param dst     Array in which to insert {@code src} data.
     *                Can be null only if {@code length} is 0.
     * @param dstOff  Index of the first element in {@code dst} where to insert {@code src} data.
     *                All elements of {@code dst} whose index is equal to or greater than
     *                {@code dstOff} will be moved forward.
     * @param length  Number of {@code src} elements to insert.
     * @return        Array which contains the merge of {@code src} and {@code dst}. This method
     *                returns directly {@code dst} when {@code length} is zero, but never return
     *                {@code src}.
     * @throws NullArgumentException
     *          If {@code src} or {@code dst} is null while {@code length} is different than 0.
     * @throws IllegalArgumentException
     *          If {@code length} is negative.
     * @throws IndexOutOfBoundsException
     *          If {@code srcOff}, {@code srcOff+length} or {@code dstOff} is out of array bounds,
     */
    public static float[] insert(final float[] src, final int srcOff,
                                 final float[] dst, final int dstOff, final int length)
            throws NullArgumentException, IllegalArgumentException, IndexOutOfBoundsException
    {
        return doInsert(src, srcOff, dst, dstOff, length);
    }

    /**
     * Returns an array containing the same elements than the given array, with the content
     * of an other array inserted at the given index.
     *
     * <p>If the {@code length} argument is 0, then this method returns the {@code dst}
     * reference unchanged. Otherwise this method creates a new array. In every cases,
     * the given arrays are never modified.</p>
     *
     * @param src     Array to entirely or partially insert into {@code dst}.
     *                Can be null only if {@code length} is 0.
     * @param srcOff  Index of the first element of {@code src} to insert into {@code dst}.
     * @param dst     Array in which to insert {@code src} data.
     *                Can be null only if {@code length} is 0.
     * @param dstOff  Index of the first element in {@code dst} where to insert {@code src} data.
     *                All elements of {@code dst} whose index is equal to or greater than
     *                {@code dstOff} will be moved forward.
     * @param length  Number of {@code src} elements to insert.
     * @return        Array which contains the merge of {@code src} and {@code dst}. This method
     *                returns directly {@code dst} when {@code length} is zero, but never return
     *                {@code src}.
     * @throws NullArgumentException
     *          If {@code src} or {@code dst} is null while {@code length} is different than 0.
     * @throws IllegalArgumentException
     *          If {@code length} is negative.
     * @throws IndexOutOfBoundsException
     *          If {@code srcOff}, {@code srcOff+length} or {@code dstOff} is out of array bounds,
     */
    public static long[] insert(final long[] src, final int srcOff,
                                final long[] dst, final int dstOff, final int length)
            throws NullArgumentException, IllegalArgumentException, IndexOutOfBoundsException
    {
        return doInsert(src, srcOff, dst, dstOff, length);
    }

    /**
     * Returns an array containing the same elements than the given array, with the content
     * of an other array inserted at the given index.
     *
     * <p>If the {@code length} argument is 0, then this method returns the {@code dst}
     * reference unchanged. Otherwise this method creates a new array. In every cases,
     * the given arrays are never modified.</p>
     *
     * @param src     Array to entirely or partially insert into {@code dst}.
     *                Can be null only if {@code length} is 0.
     * @param srcOff  Index of the first element of {@code src} to insert into {@code dst}.
     * @param dst     Array in which to insert {@code src} data.
     *                Can be null only if {@code length} is 0.
     * @param dstOff  Index of the first element in {@code dst} where to insert {@code src} data.
     *                All elements of {@code dst} whose index is equal to or greater than
     *                {@code dstOff} will be moved forward.
     * @param length  Number of {@code src} elements to insert.
     * @return        Array which contains the merge of {@code src} and {@code dst}. This method
     *                returns directly {@code dst} when {@code length} is zero, but never return
     *                {@code src}.
     * @throws NullArgumentException
     *          If {@code src} or {@code dst} is null while {@code length} is different than 0.
     * @throws IllegalArgumentException
     *          If {@code length} is negative.
     * @throws IndexOutOfBoundsException
     *          If {@code srcOff}, {@code srcOff+length} or {@code dstOff} is out of array bounds,
     */
    public static int[] insert(final int[] src, final int srcOff,
                               final int[] dst, final int dstOff, final int length)
            throws NullArgumentException, IllegalArgumentException, IndexOutOfBoundsException
    {
        return doInsert(src, srcOff, dst, dstOff, length);
    }

    /**
     * Returns an array containing the same elements than the given array, with the content
     * of an other array inserted at the given index.
     *
     * <p>If the {@code length} argument is 0, then this method returns the {@code dst}
     * reference unchanged. Otherwise this method creates a new array. In every cases,
     * the given arrays are never modified.</p>
     *
     * @param src     Array to entirely or partially insert into {@code dst}.
     *                Can be null only if {@code length} is 0.
     * @param srcOff  Index of the first element of {@code src} to insert into {@code dst}.
     * @param dst     Array in which to insert {@code src} data.
     *                Can be null only if {@code length} is 0.
     * @param dstOff  Index of the first element in {@code dst} where to insert {@code src} data.
     *                All elements of {@code dst} whose index is equal to or greater than
     *                {@code dstOff} will be moved forward.
     * @param length  Number of {@code src} elements to insert.
     * @return        Array which contains the merge of {@code src} and {@code dst}. This method
     *                returns directly {@code dst} when {@code length} is zero, but never return
     *                {@code src}.
     * @throws NullArgumentException
     *          If {@code src} or {@code dst} is null while {@code length} is different than 0.
     * @throws IllegalArgumentException
     *          If {@code length} is negative.
     * @throws IndexOutOfBoundsException
     *          If {@code srcOff}, {@code srcOff+length} or {@code dstOff} is out of array bounds,
     */
    public static short[] insert(final short[] src, final int srcOff,
                                 final short[] dst, final int dstOff, final int length)
            throws NullArgumentException, IllegalArgumentException, IndexOutOfBoundsException
    {
        return doInsert(src, srcOff, dst, dstOff, length);
    }

    /**
     * Returns an array containing the same elements than the given array, with the content
     * of an other array inserted at the given index.
     *
     * <p>If the {@code length} argument is 0, then this method returns the {@code dst}
     * reference unchanged. Otherwise this method creates a new array. In every cases,
     * the given arrays are never modified.</p>
     *
     * @param src     Array to entirely or partially insert into {@code dst}.
     *                Can be null only if {@code length} is 0.
     * @param srcOff  Index of the first element of {@code src} to insert into {@code dst}.
     * @param dst     Array in which to insert {@code src} data.
     *                Can be null only if {@code length} is 0.
     * @param dstOff  Index of the first element in {@code dst} where to insert {@code src} data.
     *                All elements of {@code dst} whose index is equal to or greater than
     *                {@code dstOff} will be moved forward.
     * @param length  Number of {@code src} elements to insert.
     * @return        Array which contains the merge of {@code src} and {@code dst}. This method
     *                returns directly {@code dst} when {@code length} is zero, but never return
     *                {@code src}.
     * @throws NullArgumentException
     *          If {@code src} or {@code dst} is null while {@code length} is different than 0.
     * @throws IllegalArgumentException
     *          If {@code length} is negative.
     * @throws IndexOutOfBoundsException
     *          If {@code srcOff}, {@code srcOff+length} or {@code dstOff} is out of array bounds,
     */
    public static byte[] insert(final byte[] src, final int srcOff,
                                final byte[] dst, final int dstOff, final int length)
            throws NullArgumentException, IllegalArgumentException, IndexOutOfBoundsException
    {
        return doInsert(src, srcOff, dst, dstOff, length);
    }

    /**
     * Returns an array containing the same elements than the given array, with the content
     * of an other array inserted at the given index.
     *
     * <p>If the {@code length} argument is 0, then this method returns the {@code dst}
     * reference unchanged. Otherwise this method creates a new array. In every cases,
     * the given arrays are never modified.</p>
     *
     * @param src     Array to entirely or partially insert into {@code dst}.
     *                Can be null only if {@code length} is 0.
     * @param srcOff  Index of the first element of {@code src} to insert into {@code dst}.
     * @param dst     Array in which to insert {@code src} data.
     *                Can be null only if {@code length} is 0.
     * @param dstOff  Index of the first element in {@code dst} where to insert {@code src} data.
     *                All elements of {@code dst} whose index is equal to or greater than
     *                {@code dstOff} will be moved forward.
     * @param length  Number of {@code src} elements to insert.
     * @return        Array which contains the merge of {@code src} and {@code dst}. This method
     *                returns directly {@code dst} when {@code length} is zero, but never return
     *                {@code src}.
     * @throws NullArgumentException
     *          If {@code src} or {@code dst} is null while {@code length} is different than 0.
     * @throws IllegalArgumentException
     *          If {@code length} is negative.
     * @throws IndexOutOfBoundsException
     *          If {@code srcOff}, {@code srcOff+length} or {@code dstOff} is out of array bounds,
     */
    public static char[] insert(final char[] src, final int srcOff,
                                final char[] dst, final int dstOff, final int length)
            throws NullArgumentException, IllegalArgumentException, IndexOutOfBoundsException
    {
        return doInsert(src, srcOff, dst, dstOff, length);
    }

    /**
     * Returns an array containing the same elements than the given array, with the content
     * of an other array inserted at the given index.
     *
     * <p>If the {@code length} argument is 0, then this method returns the {@code dst}
     * reference unchanged. Otherwise this method creates a new array. In every cases,
     * the given arrays are never modified.</p>
     *
     * @param src     Array to entirely or partially insert into {@code dst}.
     *                Can be null only if {@code length} is 0.
     * @param srcOff  Index of the first element of {@code src} to insert into {@code dst}.
     * @param dst     Array in which to insert {@code src} data.
     *                Can be null only if {@code length} is 0.
     * @param dstOff  Index of the first element in {@code dst} where to insert {@code src} data.
     *                All elements of {@code dst} whose index is equal to or greater than
     *                {@code dstOff} will be moved forward.
     * @param length  Number of {@code src} elements to insert.
     * @return        Array which contains the merge of {@code src} and {@code dst}. This method
     *                returns directly {@code dst} when {@code length} is zero, but never return
     *                {@code src}.
     * @throws NullArgumentException
     *          If {@code src} or {@code dst} is null while {@code length} is different than 0.
     * @throws IllegalArgumentException
     *          If {@code length} is negative.
     * @throws IndexOutOfBoundsException
     *          If {@code srcOff}, {@code srcOff+length} or {@code dstOff} is out of array bounds,
     */
    public static boolean[] insert(final boolean[] src, final int srcOff,
                                   final boolean[] dst, final int dstOff, final int length)
            throws NullArgumentException, IllegalArgumentException, IndexOutOfBoundsException
    {
        return doInsert(src, srcOff, dst, dstOff, length);
    }

    /**
     * Returns a copy of the given array with a single element appended at the end.
     * This method should be invoked only on rare occasions. If many elements are to
     * be added, use {@link java.util.ArrayList} instead.
     *
     * @param <T>      The type of elements in the array.
     * @param array    The array to copy with a new element. The original array will not be modified.
     * @param element  The element to add (can be null).
     * @return         A copy of the given array with the given element appended at the end.
     * @throws NullArgumentException If the given array is null.
     *
     * @see #concatenate(Object[][])
     */
    public static <T> T[] append(final T[] array, final T element) throws NullArgumentException{
        ArgumentChecks.ensureNonNull("array", array);
        final T[] copy = Arrays.copyOf(array, array.length + 1);
        copy[array.length] = element;
        return copy;
    }

    /**
     * Removes the duplicated elements in the given array. This method should be invoked
     * only for small arrays, typically less than 10 distinct elements. For larger arrays,
     * use {@link java.util.LinkedHashSet} instead.
     *
     * <p>This method compares all pair of elements using the {@link Objects#equals(Object, Object)}
     * method - so null elements are allowed. If duplicated values are found, then only the first
     * occurrence is retained; the second occurrence is removed in-place. After all elements have
     * been compared, this method returns the number of remaining elements in the array. The free
     * space at the end of the array is padded with {@code null} values.</p>
     *
     * <p>Callers can obtain an array of appropriate length using the following idiom.
     * Note that this idiom will create a new array only if necessary:</p>
     *
     * {@preformat java
     *     T[] array = ...;
     *     array = resize(array, removeDuplicated(array));
     * }
     *
     * <div class="note"><b>API note:</b>
     * This method return type is not an array in order to make obvious that the given array will be modified in-place.
     * This behavior is different than the behavior of many other methods in this class, which do not modify the given
     * source array.</div>
     *
     * @param  array Array from which to remove duplicated elements, or {@code null}.
     * @return The number of remaining elements in the given array, or 0 if the given
     *         {@code array} was null.
     */
    public static int removeDuplicated(final Object[] array) {
        if (array == null) {
            return 0;
        }
        int length = array.length;
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
     * @param entries The array in which to reverse the order of elements, or {@code null} if none.
     */
    public static void reverse(final Object[] entries) {
        if (entries != null) {
            int i = entries.length >>> 1;
            int j = i + (entries.length & 1);
            while (--i >= 0) {
                final Object tmp = entries[i];
                entries[i] = entries[j];
                entries[j++] = tmp;
            }
        }
    }

    /**
     * Reverses the order of elements in the given array.
     * This operation is performed in-place.
     * If the given array is {@code null}, then this method does nothing.
     *
     * @param values The array in which to reverse the order of elements, or {@code null} if none.
     */
    public static void reverse(final int[] values) {
        if (values != null) {
            int i = values.length >>> 1;
            int j = i + (values.length & 1);
            while (--i >= 0) {
                final int tmp = values[i];
                values[i] = values[j];
                values[j++] = tmp;
            }
        }
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
     * @param <E>         The type of array elements.
     * @param array       The array to test for order.
     * @param comparator  The comparator to use for comparing order.
     * @param strict      {@code true} if elements should be strictly sorted (i.e. equal
     *                    elements are not allowed), or {@code false} otherwise.
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
     * @param <E>         The type of array elements.
     * @param array       The array to test for order.
     * @param strict      {@code true} if elements should be strictly sorted (i.e. equal
     *                    elements are not allowed), or {@code false} otherwise.
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
     * @param array  The array to test for order.
     * @param strict {@code true} if elements should be strictly sorted (i.e. equal elements
     *               are not allowed), or {@code false} otherwise.
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
     * @param array  The array to test for order.
     * @param strict {@code true} if elements should be strictly sorted (i.e. equal elements
     *               are not allowed), or {@code false} otherwise.
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
     * @param array  The array to test for order.
     * @param strict {@code true} if elements should be strictly sorted (i.e. equal elements
     *               are not allowed), or {@code false} otherwise.
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
     * @param array  The array to test for order.
     * @param strict {@code true} if elements should be strictly sorted (i.e. equal elements
     *               are not allowed), or {@code false} otherwise.
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
     * @param array  The array to test for order.
     * @param strict {@code true} if elements should be strictly sorted (i.e. equal elements
     *               are not allowed), or {@code false} otherwise.
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
     * @param array  The array to test for order.
     * @param strict {@code true} if elements should be strictly sorted (i.e. equal elements
     *               are not allowed), or {@code false} otherwise.
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
     * @param array  The array to test for order.
     * @param strict {@code true} if elements should be strictly sorted (i.e. equal elements
     *               are not allowed), or {@code false} otherwise.
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
     * @param data The array in which to swap elements.
     * @param i0   Index of one element to be swapped.
     * @param i1   Index of the other element to be swapped.
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
     * @param data The array in which to swap elements.
     * @param i0   Index of one element to be swapped.
     * @param i1   Index of the other element to be swapped.
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
     * @param data The array in which to swap elements.
     * @param i0   Index of one element to be swapped.
     * @param i1   Index of the other element to be swapped.
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
     * @param data The array in which to swap elements.
     * @param i0   Index of one element to be swapped.
     * @param i1   Index of the other element to be swapped.
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
     * @param data The array in which to swap elements.
     * @param i0   Index of one element to be swapped.
     * @param i1   Index of the other element to be swapped.
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
     * @param data The array in which to swap elements.
     * @param i0   Index of one element to be swapped.
     * @param i1   Index of the other element to be swapped.
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
     * @param data The array in which to swap elements.
     * @param i0   Index of one element to be swapped.
     * @param i1   Index of the other element to be swapped.
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
     * @param data The array in which to swap elements.
     * @param i0   Index of one element to be swapped.
     * @param i1   Index of the other element to be swapped.
     *
     * @since 0.4
     */
    public static void swap(final char[] data, final int i0, final int i1) {
        final char t = data[i0];
        data[i0] = data[i1];
        data[i1] = t;
    }

    /**
     * Returns {@code true} if all values in the specified array are equal to the specified
     * value, which may be {@link Double#NaN}.
     *
     * @param array The array to check.
     * @param value The expected value.
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
     * Returns {@code true} if all values in the specified array are equal to the specified
     * value, which may be {@link Float#NaN}.
     *
     * @param array The array to check.
     * @param value The expected value.
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
     * Returns {@code true} if the specified array contains at least one
     * {@link Double#NaN NaN} value.
     *
     * @param array The array to check, or {@code null}.
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
     * Returns {@code true} if the specified array contains at least one
     * {@link Float#NaN NaN} value.
     *
     * @param array The array to check, or {@code null}.
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
     * Returns {@code true} if the specified array contains the specified value, ignoring case.
     * This method should be used only for very small arrays.
     *
     * @param  array The array to search in. May be {@code null}.
     * @param  value The value to search.
     * @return {@code true} if the array is non-null and contains the given value,
     *         or {@code false} otherwise.
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
     * @param  array The array to search in. May be {@code null} and may contains null elements.
     * @param  value The value to search. May be {@code null}.
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
     * @param  array The array to search in. May be {@code null} and may contains null elements.
     * @param  value The value to search. May be {@code null}.
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
     * @param array1 The first array, or {@code null}.
     * @param array2 The second array, or {@code null}.
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
     * @param <T>    The type of arrays.
     * @param arrays The arrays to concatenate, or {@code null}.
     * @return       The concatenation of all non-null arrays (may be a direct reference to one
     *               of the given array if it can be returned with no change), or {@code null}.
     *
     * @see #append(Object[], Object)
     * @see #unionOfSorted(int[], int[])
     */
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
                        result = Arrays.copyOf(array, length);
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
     * Returns the union of two sorted arrays. The input arrays shall be sorted in strictly
     * increasing order. The output array is the union of the input arrays without duplicated
     * values, with elements sorted in strictly increasing order.
     *
     * <div class="section">Recommended assertions</div>
     * Callers are encouraged to place the following assertions before calls to this method,
     * using the {@link #isSorted(int[], boolean)} and {@link Arrays#toString(int[])} methods:
     *
     * {@preformat java
     *   assert isSorted(array1, true) : toString(array1);
     *   assert isSorted(array2, true) : toString(array2);
     * }
     *
     * @param  array1 The first array, or {@code null}.
     * @param  array2 The second array, or {@code null}.
     * @return The union of the given array without duplicated values, or {@code null} if the two
     *         given arrays were null. May be one of the given arrays.
     *
     * @see #concatenate(Object[][])
     */
    public static int[] unionOfSorted(final int[] array1, final int[] array2) {
        if (array1 == null) return array2;
        if (array2 == null) return array1;
        int[] union = new int[array1.length + array2.length];
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
