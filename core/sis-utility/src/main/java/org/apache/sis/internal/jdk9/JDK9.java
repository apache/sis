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
package org.apache.sis.internal.jdk9;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.Set;
import java.util.List;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.internal.util.UnmodifiableArrayList;


/**
 * Place holder for some functionalities defined only in JDK9.
 * This file will be deleted on the SIS JDK9 branch.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   1.1
 * @version 0.8
 * @module
 */
public final class JDK9 {
    /**
     * Do not allow instantiation of this class.
     */
    private JDK9() {
    }

    /**
     * Placeholder for {@link Optional#ifPresentOrElse(Consumer, Runnable)}.
     */
    public static <T> void ifPresentOrElse(Optional<T> optional, Consumer<? super T> action, Runnable emptyAction) {
        if (optional.isPresent()) {
            action.accept(optional.get());
        } else {
            emptyAction.run();
        }
    }

    /**
     * Placeholder for {@code List.of(...)}.
     *
     * @param  <E>       type of elements.
     * @param  elements  the elements to put in an unmodifiable list.
     * @return an unmodifiable list of the given elements.
     */
    @SafeVarargs
    public static <E> List<E> listOf(final E... elements) {
        switch (elements.length) {
            case 0:  return Collections.emptyList();
            case 1:  return Collections.singletonList(elements[0]);
            default: return UnmodifiableArrayList.wrap(elements);
        }
    }

    /**
     * Placeholder for {@code Set.of(...)}.
     *
     * @param  <E>       type of elements.
     * @param  elements  the elements to put in an unmodifiable set.
     * @return an unmodifiable set of the given elements.
     */
    @SafeVarargs
    public static <E> Set<E> setOf(final E... elements) {
        switch (elements.length) {
            case 0:  return Collections.emptySet();
            case 1:  return Collections.singleton(elements[0]);
        }
        final Set<E> c = new LinkedHashSet<>(Arrays.asList(elements));
        if (c.size() != elements.length) {
            throw new IllegalArgumentException("Duplicated elements.");
        }
        return Collections.unmodifiableSet(c);
    }

    /**
     * Placeholder for {@code Map.of(...)}.
     */
    public static <K,V> Map<K,V> mapOf(final Object... entries) {
        final Map map = new HashMap();
        for (int i=0; i<entries.length;) {
            if (map.put(entries[i++], entries[i++]) != null) {
                throw new IllegalArgumentException("Duplicated elements.");
            }
        }
        return map;
    }

    /**
     * Placeholder for {@code Set.copyOf(...)} (actually a JDK10 method).
     */
    public static <V> Set<V> copyOf(final Set<V> set) {
        switch (set.size()) {
            case 0:  return Collections.emptySet();
            case 1:  return Collections.singleton(set.iterator().next());
            default: return new HashSet<>(set);
        }
    }

    /**
     * Placeholder for {@code Map.copyOf(...)} (actually a JDK10 method).
     */
    public static <K,V> Map<K,V> copyOf(final Map<K,V> map) {
        return map.size() < 2 ? CollectionsExt.compact(map) : new HashMap<>(map);
    }

    /**
     * Place holder for {@code Buffer.slice()}.
     *
     * @param  b the buffer to slice.
     * @return the sliced buffer.
     */
    public static Buffer slice(Buffer b) {
        if (b instanceof ByteBuffer)   return ((ByteBuffer) b).slice();
        if (b instanceof ShortBuffer)  return ((ShortBuffer) b).slice();
        if (b instanceof IntBuffer)    return ((IntBuffer) b).slice();
        if (b instanceof LongBuffer)   return ((LongBuffer) b).slice();
        if (b instanceof FloatBuffer)  return ((FloatBuffer) b).slice();
        if (b instanceof DoubleBuffer) return ((DoubleBuffer) b).slice();
        throw new IllegalArgumentException();
    }

    /**
     * Place holder for {@code Buffer.duplicate()}.
     *
     * @param  b the buffer to duplicate.
     * @return the duplicate buffer.
     */
    public static Buffer duplicate(Buffer b) {
        if (b instanceof ByteBuffer)   return ((ByteBuffer) b).duplicate();
        if (b instanceof ShortBuffer)  return ((ShortBuffer) b).duplicate();
        if (b instanceof IntBuffer)    return ((IntBuffer) b).duplicate();
        if (b instanceof LongBuffer)   return ((LongBuffer) b).duplicate();
        if (b instanceof FloatBuffer)  return ((FloatBuffer) b).duplicate();
        if (b instanceof DoubleBuffer) return ((DoubleBuffer) b).duplicate();
        throw new IllegalArgumentException();
    }

    /**
     * Place holder for {@code ByteBuffer.get(int, byte[])}.
     *
     * @param  b     the buffer from which to get bytes.
     * @param  index index from which the first byte will be read.
     * @param  dst   destination array
     */
    public static void get(final ByteBuffer b, int index, final byte[] dst) {
        JDK9.get(b, index, dst, 0, dst.length);
    }

    /**
     * Place holder for {@code ByteBuffer.get(int, byte[], int, int)}.
     *
     * @param  b       the buffer from which to get bytes.
     * @param  index   index from which the first byte will be read.
     * @param  dst     destination array
     * @param  offset  offset in the array of the first byte to write.
     * @param  length  number of bytes to write.
     */
    public static void get(final ByteBuffer b, final int index, final byte[] dst, final int offset, final int length) {
        for (int i=0; i<length; i++) {
            dst[offset + i] = b.get(index + i);
        }
    }

    /**
     * Place holder for {@code Class.getPackageName()}.
     *
     * @param  c  the class for which to get the package name.
     * @return the name of the package.
     */
    public static String getPackageName(Class<?> c) {
        Class<?> outer;
        while ((outer = c.getEnclosingClass()) != null) {
            c = outer;
        }
        String name = c.getName();
        final int separator = name.lastIndexOf('.');
        name = (separator >= 1) ? name.substring(0, separator) : "";
        return name;
    }

    /**
     * Place holder for {@code Math.multiplyFull​(int, int)}.
     *
     * @param  x  the first value.
     * @param  y  the second value.
     * @return Product of the two values.
     */
    public static long multiplyFull​(int x, int y) {
        return ((long) x) * ((long) y);
    }

    /**
     * Place holder for {@link java.util.Arrays} method added in JDK9.
     * This placeholder does not perform range check (JDK9 method does).
     */
    public static boolean equals(final char[] a, int ai, final int aUp,
                                 final char[] b, int bi, final int bUp)
    {
        if (aUp - ai != bUp - bi) {
            return false;
        }
        while (ai < aUp) {
            if (a[ai++] != b[bi++]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Place holder for {@link java.util.Arrays} method added in JDK9.
     * This placeholder does not perform range check (JDK9 method does).
     */
    public static boolean equals(final byte[] a, int ai, final int aUp,
                                 final byte[] b, int bi, final int bUp)
    {
        if (aUp - ai != bUp - bi) {
            return false;
        }
        while (ai < aUp) {
            if (a[ai++] != b[bi++]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Place holder for {@link java.util.Arrays} method added in JDK9.
     * This placeholder does not perform range check (JDK9 method does).
     */
    public static boolean equals(final short[] a, int ai, final int aUp,
                                 final short[] b, int bi, final int bUp)
    {
        if (aUp - ai != bUp - bi) {
            return false;
        }
        while (ai < aUp) {
            if (a[ai++] != b[bi++]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Place holder for {@link java.util.Arrays} method added in JDK9.
     * This placeholder does not perform range check (JDK9 method does).
     */
    public static boolean equals(final int[] a, int ai, final int aUp,
                                 final int[] b, int bi, final int bUp)
    {
        if (aUp - ai != bUp - bi) {
            return false;
        }
        while (ai < aUp) {
            if (a[ai++] != b[bi++]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Place holder for {@link java.util.Arrays} method added in JDK9.
     * This placeholder does not perform range check (JDK9 method does).
     */
    public static boolean equals(final long[] a, int ai, final int aUp,
                                 final long[] b, int bi, final int bUp)
    {
        if (aUp - ai != bUp - bi) {
            return false;
        }
        while (ai < aUp) {
            if (a[ai++] != b[bi++]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Place holder for {@link java.util.Arrays} method added in JDK9.
     * This placeholder does not perform range check (JDK9 method does).
     */
    public static boolean equals(final float[] a, int ai, final int aUp,
                                 final float[] b, int bi, final int bUp)
    {
        if (aUp - ai != bUp - bi) {
            return false;
        }
        while (ai < aUp) {
            if (Float.floatToIntBits(a[ai++]) != Float.floatToIntBits(b[bi++])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Place holder for {@link java.util.Arrays} method added in JDK9.
     * This placeholder does not perform range check (JDK9 method does).
     */
    public static boolean equals(final double[] a, int ai, final int aUp,
                                 final double[] b, int bi, final int bUp)
    {
        if (aUp - ai != bUp - bi) {
            return false;
        }
        while (ai < aUp) {
            if (Double.doubleToLongBits(a[ai++]) != Double.doubleToLongBits(b[bi++])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Place holder for {@link Stream#toList()} method added in JDK16.
     */
    public static <T> List<T> toList(final Stream<T> s) {
        return (List<T>) UnmodifiableArrayList.wrap(s.toArray());
    }
}
