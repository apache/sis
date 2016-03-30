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
import javax.xml.bind.DatatypeConverter;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.JDK7;
import org.apache.sis.internal.jdk7.Objects;
import org.apache.sis.internal.jdk7.Files;
import org.apache.sis.internal.jdk7.Path;
import org.apache.sis.internal.jdk7.StandardCharsets;


/**
 * Place holder for some functionalities defined only in JDK8.
 * This file will be deleted on the SIS JDK8 branch.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
public final class JDK8 {
    /**
     * A shared Gregorian calendar to use for {@link #printDateTime(Date)}.
     * We share a single instance instead than using {@link ThreadLocal} instances
     * on the assumption that usages of this calendar will be relatively rare.
     */
    private static final AtomicReference<Calendar> CALENDAR = new AtomicReference<Calendar>();

    /**
     * Do not allow instantiation of this class.
     */
    private JDK8() {
    }

    /**
     * Compares two numbers as unsigned.
     *
     * @param  x First unsigned value.
     * @param  y Second unsigned value.
     * @return Comparison result.
     *
     * @since 0.7
     */
    public static int compareUnsigned(final int x, final int y) {
        return JDK7.compare(x + Integer.MIN_VALUE, y + Integer.MIN_VALUE);
    }

    /**
     * Returns the given byte as an unsigned integer.
     *
     * @param x The byte to return as an unsigned integer.
     * @return The unsigned value of the given byte.
     *
     * @since 0.7
     */
    public static int toUnsignedInt(final byte x) {
        return x & 0xFF;
    }

    /**
     * Returns the given short as an unsigned integer.
     *
     * @param x The short to return as an unsigned integer.
     * @return The unsigned value of the given short.
     *
     * @since 0.7
     */
    public static int toUnsignedInt(final short x) {
        return x & 0xFFFF;
    }

    /**
     * Safe cast of the given long to integer.
     *
     * @param value The value to cast.
     * @return The casted value.
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
     * Safe product of the arguments.
     *
     * @param x The first value.
     * @param y The second value.
     * @return The product.
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
     * @param  value The value for which to get the adjacent value.
     * @return The adjacent value in the direction of negative infinity.
     *
     * @since 0.4
     */
    public static double nextDown(final double value) {
        return Math.nextAfter(value, Double.NEGATIVE_INFINITY);
    }

    /**
     * Returns the value of the given key, or the given default value if there is no value for that key.
     *
     * @param <V> The type of values.
     * @param map The map from which to get the value.
     * @param key The key for the value to fetch.
     * @param defaultValue The value to return if the map does not contain any value for the given key.
     * @return The value for the given key (which may be {@code null}), or {@code defaultValue}.
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
     * @param  <K>   The type of keys.
     * @param  <V>   The type of values.
     * @param  map   The map where to store the value.
     * @param  key   The key for the value to store.
     * @param  value The value to store.
     * @return The previous value, or {@code null} if none.
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
     * Removes the entry for the given key, provided that it is currently mapped to the given value.
     *
     * @param  <K>   The type of keys.
     * @param  <V>   The type of values.
     * @param  map   The map from where to remove the value.
     * @param  key   The key for the value to remove.
     * @param  value The value that must exist for allowing removal.
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
     * @param  <E>        The type of elements in the given collection.
     * @param  collection The collection from which to remove element.
     * @param  filter     The condition for elements to remove.
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
     * @param  <K>      The type of keys.
     * @param  <V>      The type of values.
     * @param  map      The map from where to replace the values.
     * @param  key      The key of value to replace.
     * @param  oldValue The expected current value.
     * @param  newValue The new value to store if the current value is the expected one.
     * @return Whether the replacement has been done.
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
     * @param  <K>      The type of keys.
     * @param  <V>      The type of values.
     * @param  map      The map from where to replace the values.
     * @param  function The function performing the value replacements.
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
     * @param  <K> The type of keys.
     * @param  <V> The type of values.
     * @param  map The map where to store the value.
     * @param  key The key for the value to compute and store.
     * @param  remappingFunction The function for computing the value.
     * @return The new value computed by the given function.
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
     * Parses a date from a string in ISO 8601 format. More specifically, this method expects the
     * format defined by <cite>XML Schema Part 2: Datatypes for {@code xsd:dateTime}</cite>.
     *
     * <p>This method will be replaced by {@link java.time.format.DateTimeFormatter} on the JDK8 branch.</p>
     *
     * @param  date The date to parse.
     * @return The parsed date.
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
     * @param  date The date to format.
     * @return The formatted date.
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
     * @param path The file to open.
     * @return The reader.
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
     * @param path The file to open.
     * @return The writer.
     * @throws IOException if an error occurred while opening the writer.
     *
     * @since 0.7
     */
    public static BufferedWriter newBufferedWriter(final Path path) throws IOException {
        return Files.newBufferedWriter(path, StandardCharsets.UTF_8);
    }
}
