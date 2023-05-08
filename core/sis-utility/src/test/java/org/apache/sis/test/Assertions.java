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
package org.apache.sis.test;

import java.util.Set;
import java.util.Map;
import java.util.Objects;
import java.util.Iterator;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.stream.Stream;
import java.util.function.Consumer;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Static;

import static org.junit.Assert.*;


/**
 * Assertion methods used by the SIS project in addition of the JUnit and GeoAPI assertions.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.4
 * @since   0.3
 */
public final class Assertions extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private Assertions() {
    }

    /**
     * Asserts that the two given objects are not equal.
     * This method tests all {@link ComparisonMode} except {@code DEBUG}.
     *
     * @param  o1  the first object.
     * @param  o2  the second object.
     */
    public static void assertNotDeepEquals(final Object o1, final Object o2) {
        assertNotSame("same", o1, o2);
        assertFalse("equals",                      Objects  .equals    (o1, o2));
        assertFalse("deepEquals",                  Objects  .deepEquals(o1, o2));
        assertFalse("deepEquals(STRICT)",          Utilities.deepEquals(o1, o2, ComparisonMode.STRICT));
        assertFalse("deepEquals(BY_CONTRACT)",     Utilities.deepEquals(o1, o2, ComparisonMode.BY_CONTRACT));
        assertFalse("deepEquals(IGNORE_METADATA)", Utilities.deepEquals(o1, o2, ComparisonMode.IGNORE_METADATA));
        assertFalse("deepEquals(APPROXIMATE)",     Utilities.deepEquals(o1, o2, ComparisonMode.APPROXIMATE));
    }

    /**
     * Asserts that the two given objects are approximately equal, while slightly different.
     * More specifically, this method asserts that the given objects are equal according the
     * {@link ComparisonMode#APPROXIMATE} criterion, but not equal according the
     * {@link ComparisonMode#IGNORE_METADATA} criterion.
     *
     * @param  expected  the expected object.
     * @param  actual    the actual object.
     */
    public static void assertAlmostEquals(final Object expected, final Object actual) {
        assertFalse("Shall not be strictly equals",        Utilities.deepEquals(expected, actual, ComparisonMode.STRICT));
        assertFalse("Shall be slightly different",         Utilities.deepEquals(expected, actual, ComparisonMode.IGNORE_METADATA));
        assertTrue ("Shall be approximately equals",       Utilities.deepEquals(expected, actual, ComparisonMode.DEBUG));
        assertTrue ("DEBUG inconsistent with APPROXIMATE", Utilities.deepEquals(expected, actual, ComparisonMode.APPROXIMATE));
    }

    /**
     * Asserts that the two given objects are equal ignoring metadata.
     * See {@link ComparisonMode#IGNORE_METADATA} for more information.
     *
     * @param  expected  the expected object.
     * @param  actual    the actual object.
     */
    public static void assertEqualsIgnoreMetadata(final Object expected, final Object actual) {
        assertTrue("Shall be approximately equals",       Utilities.deepEquals(expected, actual, ComparisonMode.DEBUG));
        assertTrue("DEBUG inconsistent with APPROXIMATE", Utilities.deepEquals(expected, actual, ComparisonMode.APPROXIMATE));
        assertTrue("Shall be equal, ignoring metadata",   Utilities.deepEquals(expected, actual, ComparisonMode.IGNORE_METADATA));
    }

    /**
     * Asserts that the two given arrays contains objects that are equal ignoring metadata.
     * See {@link ComparisonMode#IGNORE_METADATA} for more information.
     *
     * @param  expected  the expected objects (array can be {@code null}).
     * @param  actual    the actual objects (array can be {@code null}).
     *
     * @since 0.7
     */
    public static void assertArrayEqualsIgnoreMetadata(final Object[] expected, final Object[] actual) {
        if (expected != actual) {
            if (expected == null) {
                assertNull("Expected null array.", actual);
            } else {
                assertNotNull("Expected non-null array.", actual);
                final int length = StrictMath.min(expected.length, actual.length);
                for (int i=0; i<length; i++) try {
                    assertEqualsIgnoreMetadata(expected[i], actual[i]);
                } catch (AssertionError e) {
                    throw new AssertionError(Exceptions.formatChainedMessages(null, "Comparison failure at index "
                            + i + " (a " + Classes.getShortClassName(actual[i]) + ").", e), e);
                }
                assertEquals("Unexpected array length.", expected.length, actual.length);
            }
        }
    }

    /**
     * Asserts that two strings are equal, ignoring the differences in EOL characters.
     * The comparisons are performed on a line-by-line basis. For each line, trailing
     * spaces (but not leading spaces) are ignored.
     *
     * @param  expected  the expected string.
     * @param  actual    the actual string.
     */
    public static void assertMultilinesEquals(final CharSequence expected, final CharSequence actual) {
        assertMultilinesEquals(null, expected, actual);
    }

    /**
     * Asserts that two strings are equal, ignoring the differences in EOL characters.
     * The comparisons is performed one a line-by-line basis. For each line, trailing
     * spaces (but not leading spaces) are ignored.
     *
     * @param  message   the message to print in case of failure, or {@code null} if none.
     * @param  expected  the expected string.
     * @param  actual    the actual string.
     */
    public static void assertMultilinesEquals(final String message, final CharSequence expected, final CharSequence actual) {
        final CharSequence[] expectedLines = CharSequences.splitOnEOL(expected);
        final CharSequence[] actualLines   = CharSequences.splitOnEOL(actual);
        final int length = StrictMath.min(expectedLines.length, actualLines.length);
        final StringBuilder buffer = new StringBuilder(message != null ? message : "Line").append('[');
        final int base = buffer.length();
        for (int i=0; i<length; i++) {
            CharSequence e = expectedLines[i];
            CharSequence a = actualLines[i];
            e = e.subSequence(0, CharSequences.skipTrailingWhitespaces(e, 0, e.length()));
            a = a.subSequence(0, CharSequences.skipTrailingWhitespaces(a, 0, a.length()));
            assertEquals(buffer.append(i).append(']').toString(), e, a);
            buffer.setLength(base);
        }
        if (expectedLines.length > actualLines.length) {
            fail(buffer.append(length).append("] missing line: ").append(expectedLines[length]).toString());
        }
        if (expectedLines.length < actualLines.length) {
            fail(buffer.append(length).append("] extraneous line: ").append(actualLines[length]).toString());
        }
    }

    /**
     * Verifies that the given stream produces the same values than the given iterator, in same order.
     * This method assumes that the given stream is sequential.
     *
     * @param  <E>       the type of values to test.
     * @param  expected  the expected values.
     * @param  actual    the stream to compare with the expected values.
     *
     * @since 0.8
     */
    public static <E> void assertSequentialStreamEquals(final Iterator<E> expected, final Stream<E> actual) {
        actual.forEach(new Consumer<E>() {
            private int count;

            @Override
            public void accept(final Object value) {
                if (!expected.hasNext()) {
                    fail("Expected " + count + " elements, but the stream contains more.");
                }
                final Object ex = expected.next();
                if (!Objects.equals(ex, value)) {
                    fail("Expected " + ex + " at index " + count + " but got " + value);
                }
                count++;
            }
        });
        assertFalse("Unexpected end of stream.", expected.hasNext());
    }

    /**
     * Verifies that the given stream produces the same values than the given iterator, in any order.
     * This method is designed for use with parallel streams, but works with sequential streams too.
     *
     * @param  <E>       the type of values to test.
     * @param  expected  the expected values.
     * @param  actual    the stream to compare with the expected values.
     *
     * @since 0.8
     */
    public static <E> void assertParallelStreamEquals(final Iterator<E> expected, final Stream<E> actual) {
        final Integer ONE = 1;          // For doing autoboxing only once.
        final ConcurrentMap<E,Integer> count = new ConcurrentHashMap<>();
        while (expected.hasNext()) {
            count.merge(expected.next(), ONE, (old, one) -> old + 1);
        }
        /*
         * Following may be parallelized in an arbitrary number of threads.
         */
        actual.forEach((value) -> {
            if (count.computeIfPresent(value, (key, old) -> old - 1) == null) {
                fail("Stream returned unexpected value: " + value);
            }
        });
        /*
         * Back to sequential order, verify that all elements have been traversed
         * by the stream and no more.
         */
        for (final Map.Entry<E,Integer> entry : count.entrySet()) {
            int n = entry.getValue();
            if (n != 0) {
                final String message;
                if (n < 0) {
                    message = "Stream returned too many occurrences of %s%n%d extraneous were found.";
                } else {
                    message = "Stream did not returned all expected occurrences of %s%n%d are missing.";
                }
                fail(String.format(message, entry.getKey(), StrictMath.abs(n)));
            }
        }
    }

    /**
     * Asserts that the given set contains the same elements, ignoring order.
     * In case of failure, this method lists the missing or unexpected elements.
     *
     * <p>The given collections are typically instances of {@link Set}, but this is not mandatory.</p>
     *
     * @param  expected  the expected set, or {@code null}.
     * @param  actual    the actual set, or {@code null}.
     */
    public static void assertSetEquals(final Collection<?> expected, final Collection<?> actual) {
        if (expected != null && actual != null && !expected.isEmpty()) {
            final Set<Object> r = new LinkedHashSet<>(expected);
            assertTrue("The two sets are disjoint.",                 r.removeAll(actual));
            assertTrue("The set is missing elements: " + r,          r.isEmpty());
            assertTrue("The set unexpectedly became empty.",         r.addAll(actual));
            assertTrue("The two sets are disjoint.",                 r.removeAll(expected));
            assertTrue("The set contains unexpected elements: " + r, r.isEmpty());
        }
        if (expected instanceof Set<?> && actual instanceof Set<?>) {
            assertEquals("Set.equals(Object) failed:", expected, actual);
            assertEquals("Unexpected hash code value.", expected.hashCode(), actual.hashCode());
        }
    }

    /**
     * Asserts that the given map contains the same entries.
     * In case of failure, this method lists the missing or unexpected entries.
     *
     * @param  expected  the expected map, or {@code null}.
     * @param  actual    the actual map, or {@code null}.
     */
    public static void assertMapEquals(final Map<?,?> expected, final Map<?,?> actual) {
        if (expected != null && actual != null && !expected.isEmpty()) {
            final Map<Object,Object> r = new LinkedHashMap<>(expected);
            for (final Map.Entry<?,?> entry : actual.entrySet()) {
                final Object key = entry.getKey();
                if (!r.containsKey(key)) {
                    fail("Unexpected entry for key " + key);
                }
                final Object ve = r.remove(key);
                final Object va = entry.getValue();
                if (!Objects.equals(ve, va)) {
                    fail("Wrong value for key " + key + ": expected " + ve + " but got " + va);
                }
            }
            if (!r.isEmpty()) {
                fail("The map is missing entries: " + r);
            }
            r.putAll(actual);
            for (final Map.Entry<?,?> entry : expected.entrySet()) {
                final Object key = entry.getKey();
                if (!r.containsKey(key)) {
                    fail("Missing an entry for key " + key);
                }
                final Object ve = entry.getValue();
                final Object va = r.remove(key);
                if (!Objects.equals(ve, va)) {
                    fail("Wrong value for key " + key + ": expected " + ve + " but got " + va);
                }
            }
            if (!r.isEmpty()) {
                fail("The map contains unexpected elements:" + r);
            }
        }
        assertEquals("Map.equals(Object) failed:", expected, actual);
    }

    /**
     * Serializes the given object in memory, deserializes it and ensures that the deserialized
     * object is equal to the original one. This method does not write anything to the disk.
     *
     * <p>If the serialization fails, then this method throws an {@link AssertionError}
     * as do the other JUnit assertion methods.</p>
     *
     * @param  <T>     the type of the object to serialize.
     * @param  object  the object to serialize.
     * @return the deserialized object.
     */
    public static <T> T assertSerializedEquals(final T object) {
        Objects.requireNonNull(object);
        final Object deserialized;
        try {
            final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(buffer)) {
                out.writeObject(object);
            }
            // Now reads the object we just serialized.
            final byte[] data = buffer.toByteArray();
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data))) {
                try {
                    deserialized = in.readObject();
                } catch (ClassNotFoundException e) {
                    throw new AssertionError(e);
                }
            }
        } catch (IOException e) {
            throw new AssertionError(e.toString(), e);
        }
        assertNotNull("Deserialized object shall not be null.", deserialized);
        /*
         * Compare with the original object and return it.
         */
        @SuppressWarnings("unchecked")
        final Class<? extends T> type = (Class<? extends T>) object.getClass();
        assertEquals("Deserialized object not equal to the original one.", object, deserialized);
        assertEquals("Deserialized object has a different hash code.",
                object.hashCode(), deserialized.hashCode());
        return type.cast(deserialized);
    }
}
