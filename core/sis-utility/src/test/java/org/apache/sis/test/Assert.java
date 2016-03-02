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
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.swing.tree.TreeNode;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.Classes;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * Assertion methods used by the SIS project in addition of the JUnit and GeoAPI assertions.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
public strictfp class Assert extends GeoapiAssert {
    /**
     * For subclass constructor only.
     */
    protected Assert() {
    }

    /**
     * Asserts that the two given objects are not equal.
     * This method tests all {@link ComparisonMode} except {@code DEBUG}.
     *
     * @param o1  The first object.
     * @param o2  The second object.
     */
    public static void assertNotDeepEquals(final Object o1, final Object o2) {
        assertNotSame("same", o1, o2);
        assertFalse("equals",                      Objects  .equals    (o1, o2));
        assertFalse("deepEquals",                  Objects  .deepEquals(o1, o2));
        assertFalse("deepEquals(STRICT)",          Utilities.deepEquals(o1, o2, ComparisonMode.STRICT));
        assertFalse("deepEquals(BY_CONTRACT)",     Utilities.deepEquals(o1, o2, ComparisonMode.BY_CONTRACT));
        assertFalse("deepEquals(IGNORE_METADATA)", Utilities.deepEquals(o1, o2, ComparisonMode.IGNORE_METADATA));
        assertFalse("deepEquals(APPROXIMATIVE)",   Utilities.deepEquals(o1, o2, ComparisonMode.APPROXIMATIVE));
    }

    /**
     * Asserts that the two given objects are approximatively equal, while slightly different.
     * More specifically, this method asserts that the given objects are equal according the
     * {@link ComparisonMode#APPROXIMATIVE} criterion, but not equal according the
     * {@link ComparisonMode#IGNORE_METADATA} criterion.
     *
     * @param expected  The expected object.
     * @param actual    The actual object.
     */
    public static void assertAlmostEquals(final Object expected, final Object actual) {
        assertFalse("Shall not be strictly equals",          Utilities.deepEquals(expected, actual, ComparisonMode.STRICT));
        assertFalse("Shall be slightly different",           Utilities.deepEquals(expected, actual, ComparisonMode.IGNORE_METADATA));
        assertTrue ("Shall be approximatively equals",       Utilities.deepEquals(expected, actual, ComparisonMode.DEBUG));
        assertTrue ("DEBUG inconsistent with APPROXIMATIVE", Utilities.deepEquals(expected, actual, ComparisonMode.APPROXIMATIVE));
    }

    /**
     * Asserts that the two given objects are equal ignoring metadata.
     * See {@link ComparisonMode#IGNORE_METADATA} for more information.
     *
     * @param expected  The expected object.
     * @param actual    The actual object.
     */
    public static void assertEqualsIgnoreMetadata(final Object expected, final Object actual) {
        assertTrue("Shall be approximatively equals",       Utilities.deepEquals(expected, actual, ComparisonMode.DEBUG));
        assertTrue("DEBUG inconsistent with APPROXIMATIVE", Utilities.deepEquals(expected, actual, ComparisonMode.APPROXIMATIVE));
        assertTrue("Shall be equal, ignoring metadata",     Utilities.deepEquals(expected, actual, ComparisonMode.IGNORE_METADATA));
    }

    /**
     * Asserts that the two given arrays contains objects that are equal ignoring metadata.
     * See {@link ComparisonMode#IGNORE_METADATA} for more information.
     *
     * @param expected  The expected objects (array can be {@code null}).
     * @param actual    The actual objects (array can be {@code null}).
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
                    final AssertionError ne = new AssertionError(Exceptions.formatChainedMessages(null,
                            "Comparison failure at index " + i + " (a " + Classes.getShortClassName(actual[i]) + ").", e));
                    ne.initCause(e);
                    throw ne;
                }
                assertEquals("Unexpected array length.", expected.length, actual.length);
            }
        }
    }

    /**
     * Asserts that two strings are equal, ignoring the differences in EOL characters.
     * The comparisons is performed one a line-by-line basis. For each line, trailing
     * spaces (but not leading spaces) are ignored.
     *
     * @param expected The expected string.
     * @param actual   The actual string.
     */
    public static void assertMultilinesEquals(final CharSequence expected, final CharSequence actual) {
        assertMultilinesEquals(null, expected, actual);
    }

    /**
     * Asserts that two strings are equal, ignoring the differences in EOL characters.
     * The comparisons is performed one a line-by-line basis. For each line, trailing
     * spaces (but not leading spaces) are ignored.
     *
     * @param message  The message to print in case of failure, or {@code null} if none.
     * @param expected The expected string.
     * @param actual   The actual string.
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
     * Asserts that the given set contains the same elements, ignoring order.
     * In case of failure, this method lists the missing or unexpected elements.
     *
     * <p>The given collections are typically instances of {@link Set}, but this is not mandatory.</p>
     *
     * @param expected The expected set, or {@code null}.
     * @param actual   The actual set, or {@code null}.
     */
    public static void assertSetEquals(final Collection<?> expected, final Collection<?> actual) {
        if (expected != null && actual != null && !expected.isEmpty()) {
            final Set<Object> r = new LinkedHashSet<Object>(expected);
            assertTrue("The two sets are disjoint.",                 r.removeAll(actual));
            assertTrue("The set is missing elements: " + r,          r.isEmpty());
            assertTrue("The set unexpectedly became empty.",         r.addAll(actual));
            assertTrue("The two sets are disjoint.",                 r.removeAll(expected));
            assertTrue("The set contains unexpected elements: " + r, r.isEmpty());
        }
        if (expected instanceof Set<?> && actual instanceof Set<?>) {
            assertEquals("Set.equals(Object) failed:", expected, actual);
        }
    }

    /**
     * Asserts that the given map contains the same entries.
     * In case of failure, this method lists the missing or unexpected entries.
     *
     * @param expected The expected map, or {@code null}.
     * @param actual   The actual map, or {@code null}.
     */
    public static void assertMapEquals(final Map<?,?> expected, final Map<?,?> actual) {
        if (expected != null && actual != null && !expected.isEmpty()) {
            final Map<Object,Object> r = new LinkedHashMap<Object,Object>(expected);
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
     * Ensures that a tree is equals to an other tree.
     * This method invokes itself recursively for every child nodes.
     *
     * @param  expected The expected tree, or {@code null}.
     * @param  actual   The tree to compare with the expected one, or {@code null}.
     * @return The number of nodes.
     */
    public static int assertTreeEquals(final TreeNode expected, final TreeNode actual) {
        if (expected == null) {
            assertNull(actual);
            return 0;
        }
        int n = 1;
        assertNotNull(actual);
        assertEquals("isLeaf()",            expected.isLeaf(),            actual.isLeaf());
        assertEquals("getAllowsChildren()", expected.getAllowsChildren(), actual.getAllowsChildren());
        assertEquals("getChildCount()",     expected.getChildCount(),     actual.getChildCount());
        @SuppressWarnings("unchecked") final Enumeration<? extends TreeNode> ec = expected.children();
        @SuppressWarnings("unchecked") final Enumeration<? extends TreeNode> ac = actual  .children();

        int childIndex = 0;
        while (ec.hasMoreElements()) {
            assertTrue("hasMoreElements()", ac.hasMoreElements());
            final TreeNode nextExpected = ec.nextElement();
            final TreeNode nextActual   = ac.nextElement();
            final String message = "getChildAt(" + childIndex + ')';
            assertSame(message, nextExpected, expected.getChildAt(childIndex));
            assertSame(message, nextActual,   actual  .getChildAt(childIndex));
            assertSame("getParent()", expected, nextExpected.getParent());
            assertSame("getParent()", actual,   nextActual  .getParent());
            assertSame("getIndex(TreeNode)", childIndex, expected.getIndex(nextExpected));
            assertSame("getIndex(TreeNode)", childIndex, actual  .getIndex(nextActual));
            n += assertTreeEquals(nextExpected, nextActual);
            childIndex++;
        }
        assertFalse("hasMoreElements()", ac.hasMoreElements());
        assertEquals("toString()", expected.toString(), actual.toString());
        return n;
    }

    /**
     * Parses two XML trees as DOM documents, and compares the nodes.
     * The inputs given to this method can be any of the following types:
     *
     * <ul>
     *   <li>{@link org.w3c.dom.Node}: used directly without further processing.</li>
     *   <li>{@link java.io.File}, {@link java.net.URL} or {@link java.net.URI}: the
     *       stream is opened and parsed as a XML document.</li>
     *   <li>{@link String}: The string content is parsed directly as a XML document.</li>
     * </ul>
     *
     * The comparison will ignore comments and the optional attributes given in arguments.
     *
     * <div class="section">Ignored attributes substitution</div>
     * For convenience, this method replaces some well known prefixes in the {@code ignoredAttributes}
     * array by their full namespace URLs. For example this method replaces{@code "xsi:schemaLocation"}
     * by {@code "http://www.w3.org/2001/XMLSchema-instance:schemaLocation"}.
     * If such substitution is not desired, consider using {@link XMLComparator} directly instead.
     *
     * <p>The current substitution map is as below (may be expanded in any future SIS version):</p>
     *
     * <table class="sis">
     *   <caption>Predefined prefix mapping</caption>
     *   <tr><th>Prefix</th> <th>URL</th></tr>
     *   <tr><td>xmlns</td>  <td>{@code "http://www.w3.org/2000/xmlns"}</td></tr>
     *   <tr><td>xlink</td>  <td>{@value org.apache.sis.xml.Namespaces#XLINK}</td></tr>
     *   <tr><td>xsi</td>    <td>{@value org.apache.sis.xml.Namespaces#XSI}</td></tr>
     *   <tr><td>gml</td>    <td>{@value org.apache.sis.xml.Namespaces#GML}</td></tr>
     *   <tr><td>gmd</td>    <td>{@value org.apache.sis.xml.Namespaces#GMD}</td></tr>
     *   <tr><td>gmx</td>    <td>{@value org.apache.sis.xml.Namespaces#GMX}</td></tr>
     *   <tr><td>gmi</td>    <td>{@value org.apache.sis.xml.Namespaces#GMI}</td></tr>
     *   <tr><td>gco</td>    <td>{@value org.apache.sis.xml.Namespaces#GCO}</td></tr>
     * </table>
     *
     * <p>For example in order to ignore the namespace, type and schema location declaration,
     * the following strings can be given to the {@code ignoredAttributes} argument:</p>
     *
     * {@preformat text
     *   "xmlns:*", "xsi:schemaLocation", "xsi:type"
     * }
     *
     * @param  expected The expected XML document.
     * @param  actual   The XML document to compare.
     * @param  ignoredAttributes The fully-qualified names of attributes to ignore
     *         (typically {@code "xmlns:*"} and {@code "xsi:schemaLocation"}).
     *
     * @see XMLComparator
     */
    public static void assertXmlEquals(final Object expected, final Object actual, final String... ignoredAttributes) {
        assertXmlEquals(expected, actual, TestCase.STRICT, null, ignoredAttributes);
    }

    /**
     * Parses two XML trees as DOM documents, and compares the nodes with the given tolerance
     * threshold for numerical values. The inputs given to this method can be any of the types
     * documented {@linkplain #assertXmlEquals(Object, Object, String[]) above}. This method
     * will ignore comments and the optional attributes given in arguments as documented in the
     * above method.
     *
     * @param  expected  The expected XML document.
     * @param  actual    The XML document to compare.
     * @param  tolerance The tolerance threshold for comparison of numerical values.
     * @param  ignoredNodes The fully-qualified names of the nodes to ignore, or {@code null} if none.
     * @param  ignoredAttributes The fully-qualified names of attributes to ignore
     *         (typically {@code "xmlns:*"} and {@code "xsi:schemaLocation"}).
     *
     * @see XMLComparator
     */
    public static void assertXmlEquals(final Object expected, final Object actual,
            final double tolerance, final String[] ignoredNodes, final String[] ignoredAttributes)
    {
        final XMLComparator comparator;
        try {
            comparator = new XMLComparator(expected, actual);
        } catch (IOException e) {
            // We don't throw directly those exceptions since failing to parse the XML file can
            // be considered as part of test failures and the JUnit exception for such failures
            // is AssertionError. Having no checked exception in "assert" methods allow us to
            // declare the checked exceptions only for the library code being tested.
            throw new AssertionError(e);
        } catch (ParserConfigurationException e) {
            throw new AssertionError(e);
        } catch (SAXException e) {
            throw new AssertionError(e);
        }
        comparator.tolerance = tolerance;
        comparator.ignoreComments = true;
        if (ignoredNodes != null) {
            for (final String node : ignoredNodes) {
                comparator.ignoredNodes.add(XMLComparator.substitutePrefix(node));
            }
        }
        if (ignoredAttributes != null) {
            for (final String attribute : ignoredAttributes) {
                comparator.ignoredAttributes.add(XMLComparator.substitutePrefix(attribute));
            }
        }
        comparator.compare();
    }

    /**
     * Serializes the given object in memory, deserializes it and ensures that the deserialized
     * object is equals to the original one. This method does not write anything to the disk.
     *
     * <p>If the serialization fails, then this method throws an {@link AssertionError}
     * as do the other JUnit assertion methods.</p>
     *
     * @param  <T> The type of the object to serialize.
     * @param  object The object to serialize.
     * @return The deserialized object.
     */
    public static <T> T assertSerializedEquals(final T object) {
        final Object deserialized;
        try {
            final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            final ObjectOutputStream out = new ObjectOutputStream(buffer);
            try {
                out.writeObject(object);
            } finally {
                out.close();
            }
            // Now reads the object we just serialized.
            final byte[] data = buffer.toByteArray();
            final ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data));
            try {
                try {
                    deserialized = in.readObject();
                } catch (ClassNotFoundException e) {
                    throw new AssertionError(e);
                }
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        // Compares with the original object and returns it.
        @SuppressWarnings("unchecked")
        final Class<? extends T> type = (Class<? extends T>) object.getClass();
        assertEquals("Deserialized object not equal to the original one.", object, deserialized);
        assertEquals("Deserialized object has a different hash code.",
                object.hashCode(), deserialized.hashCode());
        return type.cast(deserialized);
    }
}
