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
package org.apache.sis.internal.jdk8;

import java.util.Map;
import java.util.Date;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.xml.bind.DatatypeConverter;

// Branch-dependent imports
import java.util.Objects;
import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigInteger;


/**
 * Place holder for some functionalities defined only in JDK8.
 * This file will be deleted on the SIS JDK8 branch.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.8
 * @module
 */
public final class JDK8 {
    /**
     * A shared Gregorian calendar to use for {@link #printDateTime(Date)}.
     * We share a single instance instead than using {@link ThreadLocal} instances
     * on the assumption that usages of this calendar will be relatively rare.
     */
    private static final AtomicReference<Calendar> CALENDAR = new AtomicReference<>();

    /**
     * Do not allow instantiation of this class.
     */
    private JDK8() {
    }

    /**
     * Formats the given elements as a list.
     *
     * @param  delimiter  the characters to insert between each elements.
     * @param  elements   the elements to format.
     * @return the given elements formatted as a list.
     *
     * @since 0.8
     */
    public static String join(final CharSequence delimiter, final CharSequence... elements) {
        final StringBuffer buffer = new StringBuffer();
        for (final CharSequence cs : elements) {
            if (buffer.length() != 0) {
                buffer.append(delimiter);
            }
            buffer.append(cs);
        }
        return buffer.toString();
    }

    /**
     * Returns {@code true} if the given value is neither NaN or infinite.
     *
     * @param  value  the value to test.
     * @return whether the given value is finite.
     *
     * @since 0.8
     */
    public static boolean isFinite(final double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    /**
     * Compares two numbers as unsigned long.
     *
     * @param  x  first unsigned value.
     * @param  y  second unsigned value.
     * @return comparison result.
     *
     * @since 0.8
     */
    public static int compareUnsigned(final long x, final long y) {
        return Long.compare(x + Long.MIN_VALUE, y + Long.MIN_VALUE);
    }

    /**
     * Compares two numbers as unsigned.
     *
     * @param  x  first unsigned value.
     * @param  y  second unsigned value.
     * @return comparison result.
     *
     * @since 0.7
     */
    public static int compareUnsigned(final int x, final int y) {
        return Integer.compare(x + Integer.MIN_VALUE, y + Integer.MIN_VALUE);
    }

    /**
     * Returns the given value as an unsigned string.
     * This implementation is inefficient.
     * This is only a placeholder; JDK8 provides a better implementation.
     *
     * @param  x  the value to format.
     * @return a string representation of the given value.
     *
     * @since 0.8
     */
    public static String toUnsignedString(final long x) {
        if (x >= 0) return Long.toString(x);
        return new BigInteger(Long.toHexString(x), 16).toString();
    }

    /**
     * Returns the given byte as an unsigned long.
     *
     * @param  x  the byte to return as an unsigned long.
     * @return the unsigned value of the given byte.
     *
     * @since 0.8
     */
    public static long toUnsignedLong(final byte x) {
        return x & 0xFFL;
    }

    /**
     * Returns the given byte as an unsigned integer.
     *
     * @param  x  the byte to return as an unsigned integer.
     * @return the unsigned value of the given byte.
     *
     * @since 0.7
     */
    public static int toUnsignedInt(final byte x) {
        return x & 0xFF;
    }

    // Do not provide overloaded 'toUnsigned' methods for the short and int types.
    // This is a cause confusion since the mask to apply depends on the type.

    /**
     * Safe cast of the given long to integer.
     *
     * @param  value  the value to cast.
     * @return the casted value.
     * @throws ArithmeticException if the value overflows.
     *
     * @since 0.7
     */
    public static int toIntExact(final long value) {
        final int vi = (int) value;
        if (vi != value) {
            throw new ArithmeticException();
        }
        return vi;
    }

    /**
     * Safe sum of the given numbers.
     *
     * @param  x  first value to add.
     * @param  y  second value to add.
     * @return the sum.
     * @throws ArithmeticException if the result overflows.
     *
     * @since 0.8
     */
    public static int addExact(final int x, final int y) {
        final long r = x + y;
        if ((r & 0xFFFFFFFF00000000L) == 0) return (int) r;
        throw new ArithmeticException();
    }

    /**
     * Safe sum of the given numbers.
     *
     * @param  x  first value to add.
     * @param  y  second value to add.
     * @return the sum.
     * @throws ArithmeticException if the result overflows.
     *
     * @since 0.8
     */
    public static long addExact(final long x, final long y) {
        final long r = x + y;
        if (((x ^ r) & (y ^ r)) >= 0) return r;
        throw new ArithmeticException();
    }

    /**
     * Safe subtraction of the given numbers.
     *
     * @param  x  first value.
     * @param  y  second value to subtract.
     * @return the difference.
     * @throws ArithmeticException if the result overflows.
     *
     * @since 0.8
     */
    public static long subtractExact(final long x, final long y) {
        final long r = x - y;
        if (((x ^ r) & (y ^ r)) >= 0) return r;
        throw new ArithmeticException();
    }

    /**
     * Safe product of the arguments.
     *
     * @param  x  first value to multiply.
     * @param  y  second value to multiply.
     * @return the product.
     * @throws ArithmeticException if the result overflows (Note: not implemented in this placeholder).
     *
     * @since 0.8
     */
    public static long multiplyExact(final long x, final long y) {
        return x * y;   // Check for overflow not implemented in this placeholder.
    }

    /**
     * Safe product of the arguments.
     *
     * @param  x  the first value.
     * @param  y  the second value.
     * @return the product.
     * @throws ArithmeticException if the value overflows.
     *
     * @since 0.7
     */
    public static int multiplyExact(final int x, final int y) {
        return toIntExact(x * (long) y);
    }

    /**
     * Returns the floating-point value adjacent to {@code value} in the direction of negative infinity.
     *
     * @param  value  the value for which to get the adjacent value.
     * @return the adjacent value in the direction of negative infinity.
     *
     * @since 0.4
     */
    public static double nextDown(final double value) {
        return Math.nextAfter(value, Double.NEGATIVE_INFINITY);
    }

    /**
     * Returns the value of the given key, or the given default value if there is no value for that key.
     *
     * @param  <V>  the type of values.
     * @param  map  the map from which to get the value.
     * @param  key  the key for the value to fetch.
     * @param  defaultValue the value to return if the map does not contain any value for the given key.
     * @return the value for the given key (which may be {@code null}), or {@code defaultValue}.
     */
    public static <V> V getOrDefault(final Map<?,V> map, final Object key, final V defaultValue) {
        V value = map.get(key);
        if (value == null && !map.containsKey(key)) {
            value = defaultValue;
        }
        return value;
    }

    /**
     * Stores the value in the given map, provided that no value were set.
     * This implementation presumes that the map can not contain null values.
     *
     * @param  <K>    the type of keys.
     * @param  <V>    the type of values.
     * @param  map    the map where to store the value.
     * @param  key    the key for the value to store.
     * @param  value  the value to store.
     * @return the previous value, or {@code null} if none.
     */
    public static <K,V> V putIfAbsent(final Map<K,V> map, final K key, final V value) {
        final V previous = map.put(key, value);
        if (previous != null) {
            // Restore previous value.
            map.put(key, previous);
        }
        return previous;
    }

    /**
     * Same as {@link #putIfAbsent(Map, Object, Object)} but using a more conservative strategy
     * (actually the same one than the default JDK8 implementation). This method is preferred
     * when the {@code put(…)} operation may have significant side-effect.
     *
     * @param  <K>    the type of keys.
     * @param  <V>    the type of values.
     * @param  map    the map where to store the value.
     * @param  key    the key for the value to store.
     * @param  value  the value to store.
     * @return the previous value, or {@code null} if none.
     *
     * @since 0.8
     */
    public static <K,V> V putIfAbsentConservative(final Map<K,V> map, final K key, final V value) {
        V previous = map.get(key);
        if (previous == null) {
            previous = map.put(key, value);
        }
        return previous;
    }

    /**
     * Removes the entry for the given key, provided that it is currently mapped to the given value.
     *
     * @param  <K>    the type of keys.
     * @param  <V>    the type of values.
     * @param  map    the map from where to remove the value.
     * @param  key    the key for the value to remove.
     * @param  value  the value that must exist for allowing removal.
     * @return {@code true} if the entry has been removed.
     *
     * @since 0.7
     */
    public static <K,V> boolean remove(final Map<K,V> map, final Object key, final Object value) {
        final Object current = map.get(key);
        final boolean c = Objects.equals(current, value) && (current != null || map.containsKey(key));
        if (c) {
            map.remove(key);
        }
        return c;
    }

    /**
     * Removes all elements for which the given filter returns {@code true}.
     *
     * @param  <E>         the type of elements in the given collection.
     * @param  collection  the collection from which to remove element.
     * @param  filter      the condition for elements to remove.
     * @return {@code true} if at least one element has been removed.
     */
    public static <E> boolean removeIf(final Collection<E> collection, final Predicate<? super E> filter) {
        boolean changed = false;
        for (final Iterator<E> it = collection.iterator(); it.hasNext();) {
            if (filter.test(it.next())) {
                it.remove();
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Replaces the value for the given key if the current value is {@code oldValue}.
     *
     * @param  <K>       the type of keys.
     * @param  <V>       the type of values.
     * @param  map       the map from where to replace the values.
     * @param  key       the key of value to replace.
     * @param  oldValue  the expected current value.
     * @param  newValue  the new value to store if the current value is the expected one.
     * @return whether the replacement has been done.
     *
     * @since 0.7
     */
    public static <K,V> boolean replace(final Map<K,V> map, final K key, final V oldValue, final V newValue) {
        final Object c = map.get(key);
        if (Objects.equals(c, oldValue) && (c != null || map.containsKey(key))) {
            map.put(key, newValue);
            return true;
        }
        return false;
    }

    /**
     * Replaces the values for all entries in the given map.
     *
     * @param  <K>       the type of keys.
     * @param  <V>       the type of values.
     * @param  map       the map from where to replace the values.
     * @param  function  the function performing the value replacements.
     *
     * @since 0.7
     */
    public static <K,V> void replaceAll(final Map<K,V> map, BiFunction<? super K, ? super V, ? extends V> function) {
        for (final Map.Entry<K,V> entry : map.entrySet()) {
            entry.setValue(function.apply(entry.getKey(), entry.getValue()));
        }
    }

    /**
     * Atomically computes and stores the value for the given key. This is a substitute for
     * {@link ConcurrentMap#compute(java.lang.Object, java.util.function.BiFunction)}
     * on pre-JDK8 branches.
     *
     * @param  <K>  the type of keys.
     * @param  <V>  the type of values.
     * @param  map  the map where to store the value.
     * @param  key  the key for the value to compute and store.
     * @param  remappingFunction  the function for computing the value.
     * @return the new value computed by the given function.
     *
     * @since 0.5
     */
    public static <K,V> V compute(final ConcurrentMap<K,V> map, final K key,
            BiFunction<? super K, ? super V, ? extends V> remappingFunction)
    {
        V newValue;
        boolean success;
        do {
            final V oldValue = map.get(key);
            newValue = remappingFunction.apply(key, oldValue);
            if (newValue != null) {
                if (oldValue != null) {
                    success = map.replace(key, oldValue, newValue);
                } else {
                    success = (map.putIfAbsent(key, newValue) == null);
                }
            } else {
                if (oldValue != null) {
                    success = map.remove(key, oldValue);
                } else {
                    return null;
                }
            }
        } while (!success);
        return newValue;
    }

    /**
     * Substitute for {@link Map#merge} on pre-JDK8 branches.
     *
     * @param  <K>    the type of keys.
     * @param  <V>    the type of values.
     * @param  map    the map where to store the value.
     * @param  key    the key for the value to compute and store.
     * @param  value  the value to store if none exists.
     * @param  remappingFunction  the function for computing the value.
     * @return the new value computed by the given function.
     *
     * @since 0.8
     */
    public static <K,V> V merge(final Map<K,V> map, final K key, final V value,
            BiFunction<? super V, ? super V, ? extends V> remappingFunction)
    {
        final V oldValue = map.get(key);
        final V newValue = (oldValue == null) ? value : remappingFunction.apply(oldValue, value);
        if (newValue == null) {
            map.remove(key);
        } else {
            map.put(key, newValue);
        }
        return newValue;
    }

    /**
     * Parses a date from a string in ISO 8601 format. More specifically, this method expects the
     * format defined by <cite>XML Schema Part 2: Datatypes for {@code xsd:dateTime}</cite>.
     *
     * <p>This method will be replaced by {@link java.time.format.DateTimeFormatter} on the JDK8 branch.</p>
     *
     * @param  date  the date to parse.
     * @return the parsed date.
     * @throws IllegalArgumentException if the given date can not be parsed.
     *
     * @see DatatypeConverter#parseDateTime(String)
     */
    public static Date parseDateTime(final String date) throws IllegalArgumentException {
        return DatatypeConverter.parseDateTime(date).getTime();
    }

    /**
     * Formats a date value in a string, assuming UTC timezone and US locale.
     * This method should be used only for occasional formatting.
     *
     * <p>This method will be replaced by {@link java.time.format.DateTimeFormatter} on the JDK8 branch.</p>
     *
     * @param  date  the date to format.
     * @return the formatted date.
     *
     * @see DatatypeConverter#printDateTime(Calendar)
     */
    public static String printDateTime(final Date date) {
        Calendar calendar = CALENDAR.getAndSet(null);
        if (calendar == null) {
            calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.US);
        }
        calendar.setTime(date);
        final String text = DatatypeConverter.printDateTime(calendar);
        CALENDAR.set(calendar);                                                 // Recycle for future usage.
        return text;
    }

    /**
     * Creates a buffered reader using UTF-8 encoding.
     *
     * @param  path  the file to open.
     * @return the reader.
     * @throws IOException if an error occurred while opening the reader.
     *
     * @since 0.7
     */
    public static BufferedReader newBufferedReader(final Path path) throws IOException {
        return Files.newBufferedReader(path, StandardCharsets.UTF_8);
    }

    /**
     * Creates a buffered writer using UTF-8 encoding.
     *
     * @param  path  the file to open.
     * @return the writer.
     * @throws IOException if an error occurred while opening the writer.
     *
     * @since 0.7
     */
    public static BufferedWriter newBufferedWriter(final Path path) throws IOException {
        return Files.newBufferedWriter(path, StandardCharsets.UTF_8);
    }
}
