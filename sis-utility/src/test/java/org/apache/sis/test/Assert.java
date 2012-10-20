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
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.image.RenderedImage;
import javax.swing.tree.TreeNode;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

import org.apache.sis.util.CharSequences;

// Related to JDK7
import java.util.Objects;


/**
 * Assertion methods used by the SIS project in addition of the JUnit and GeoAPI assertions.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
public strictfp class Assert extends org.opengis.test.Assert {
    /**
     * For subclass constructor only.
     */
    protected Assert() {
    }

    /**
     * Asserts that two strings are equal, ignoring the differences in EOL characters.
     * The comparisons is performed one a line-by-line basis. For each line, leading
     * and trailing spaces are ignored in order to make the comparison independent of
     * indentation.
     *
     * @param expected The expected string.
     * @param actual   The actual string.
     */
    public static void assertMultilinesEquals(final CharSequence expected, final CharSequence actual) {
        assertArrayEquals(CharSequences.split(expected, '\n'), CharSequences.split(actual, '\n'));
    }

    /**
     * Asserts that two strings are equal, ignoring the differences in EOL characters.
     * The comparisons is performed one a line-by-line basis. For each line, leading
     * and trailing spaces are ignored in order to make the comparison independent of
     * indentation.
     *
     * @param message  The message to print in case of failure, or {@code null} if none.
     * @param expected The expected string.
     * @param actual   The actual string.
     */
    public static void assertMultilinesEquals(final String message, final CharSequence expected, final CharSequence actual) {
        assertArrayEquals(message, CharSequences.split(expected, '\n'), CharSequences.split(actual, '\n'));
    }

    /**
     * Asserts that the given set contains the same elements.
     * In case of failure, this method lists the missing or unexpected elements.
     *
     * @param expected The expected set, or {@code null}.
     * @param actual   The actual set, or {@code null}.
     */
    public static void assertSetEquals(final Set<?> expected, final Set<?> actual) {
        if (expected != null && actual != null && !expected.isEmpty()) {
            final Set<Object> r = new LinkedHashSet<>(expected);
            assertTrue("The two sets are disjoint.",                 r.removeAll(actual));
            assertTrue("The set is missing elements: " + r,          r.isEmpty());
            assertTrue("The set unexpectedly became empty.",         r.addAll(actual));
            assertTrue("The two sets are disjoint.",                 r.removeAll(expected));
            assertTrue("The set contains unexpected elements: " + r, r.isEmpty());
        }
        assertEquals("Set.equals(Object) failed:", expected, actual);
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
     * Parses two XML tree as DOM documents, and compares the nodes.
     * The inputs given to this method can be any of the following types:
     *
     * <ul>
     *   <li>{@link org.w3c.dom.Node}: used directly without further processing.</li>
     *   <li>{@link java.io.File}, {@link java.net.URL} or {@link java.net.URI}: the
     *       stream is opened and parsed as a XML document.</li>
     *   <li>{@link String}: The string content is parsed directly as a XML document.
     *       Encoding <strong>must</strong> be UTF-8 (no other encoding is supported
     *       by current implementation of this method).</li>
     * </ul>
     *
     * This method will ignore comments and the optional attributes given in arguments.
     *
     * @param  expected The expected XML document.
     * @param  actual   The XML document to compare.
     * @param  ignoredAttributes The fully-qualified names of attributes to ignore
     *         (typically {@code "xmlns:*"} and {@code "xsi:schemaLocation"}).
     *
     * @see XMLComparator
     */
    public static void assertXmlEquals(final Object expected, final Object actual,
            final String... ignoredAttributes)
    {
        assertXmlEquals(expected, actual, 0, ignoredAttributes);
    }

    /**
     * Parses two XML tree as DOM documents, and compares the nodes with the given tolerance
     * threshold for numerical values. The inputs given to this method can be any of the types
     * documented {@linkplain #assertXmlEquals(Object, Object, String[]) above}. This method
     * will ignore comments and the optional attributes given in arguments.
     *
     * @param  expected  The expected XML document.
     * @param  actual    The XML document to compare.
     * @param  tolerance The tolerance threshold for comparison of numerical values.
     * @param  ignoredAttributes The fully-qualified names of attributes to ignore
     *         (typically {@code "xmlns:*"} and {@code "xsi:schemaLocation"}).
     *
     * @see XMLComparator
     */
    public static void assertXmlEquals(final Object expected, final Object actual,
            final double tolerance, final String... ignoredAttributes)
    {
        final XMLComparator comparator;
        try {
            comparator = new XMLComparator(expected, actual);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            // We don't throw directly those exceptions since failing to parse the XML file can
            // be considered as part of test failures and the JUnit exception for such failures
            // is AssertionError. Having no checked exception in "assert" methods allow us to
            // declare the checked exceptions only for the library code being tested.
            throw new AssertionError(e);
        }
        comparator.tolerance = tolerance;
        comparator.ignoreComments = true;
        comparator.ignoredAttributes.addAll(Arrays.asList(ignoredAttributes));
        comparator.compare();
    }

    /**
     * Tests if the given {@code outer} shape contains the given {@code inner} rectangle.
     * This method will also verify class consistency by invoking the {@code intersects}
     * method, and by interchanging the arguments.
     *
     * <p>This method can be used for testing the {@code outer} implementation -
     * it should not be needed for standard JDK implementations.</p>
     *
     * @param outer The shape which is expected to contains the given rectangle.
     * @param inner The rectangle which should be contained by the shape.
     */
    public static void assertContains(final RectangularShape outer, final Rectangle2D inner) {
        assertTrue("outer.contains(inner)",   outer.contains  (inner));
        assertTrue("outer.intersects(inner)", outer.intersects(inner));
        if (outer instanceof Rectangle2D) {
            assertTrue ("inner.intersects(outer)", inner.intersects((Rectangle2D) outer));
            assertFalse("inner.contains(outer)",   inner.contains  ((Rectangle2D) outer));
        }
        assertTrue("outer.contains(centerX, centerY)",
                outer.contains(inner.getCenterX(), inner.getCenterY()));
    }

    /**
     * Tests if the given {@code r1} shape is disjoint with the given {@code r2} rectangle.
     * This method will also verify class consistency by invoking the {@code contains}
     * method, and by interchanging the arguments.
     *
     * <p>This method can be used for testing the {@code r1} implementation - it should not
     * be needed for standard implementations.</p>
     *
     * @param r1 The first shape to test.
     * @param r2 The second rectangle to test.
     */
    public static void assertDisjoint(final RectangularShape r1, final Rectangle2D r2) {
        assertFalse("r1.intersects(r2)", r1.intersects(r2));
        assertFalse("r1.contains(r2)",   r1.contains(r2));
        if (r1 instanceof Rectangle2D) {
            assertFalse("r2.intersects(r1)", r2.intersects((Rectangle2D) r1));
            assertFalse("r2.contains(r1)",   r2.contains  ((Rectangle2D) r1));
        }
        for (int i=0; i<9; i++) {
            final double x, y;
            switch (i % 3) {
                case 0: x = r2.getMinX();    break;
                case 1: x = r2.getCenterX(); break;
                case 2: x = r2.getMaxX();    break;
                default: throw new AssertionError(i);
            }
            switch (i / 3) {
                case 0: y = r2.getMinY();    break;
                case 1: y = r2.getCenterY(); break;
                case 2: y = r2.getMaxY();    break;
                default: throw new AssertionError(i);
            }
            assertFalse("r1.contains(" + x + ", " + y + ')', r1.contains(x, y));
        }
    }

    /**
     * Asserts that two rectangles have the same location and the same size.
     *
     * @param expected The expected rectangle.
     * @param actual   The rectangle to compare with the expected one.
     * @param tolx     The tolerance threshold on location along the <var>x</var> axis.
     * @param toly     The tolerance threshold on location along the <var>y</var> axis.
     */
    public static void assertRectangleEquals(final RectangularShape expected,
            final RectangularShape actual, final double tolx, final double toly)
    {
        assertEquals("Min X",    expected.getMinX(),    actual.getMinX(),    tolx);
        assertEquals("Min Y",    expected.getMinY(),    actual.getMinY(),    toly);
        assertEquals("Max X",    expected.getMaxX(),    actual.getMaxX(),    tolx);
        assertEquals("Max Y",    expected.getMaxY(),    actual.getMaxY(),    toly);
        assertEquals("Center X", expected.getCenterX(), actual.getCenterX(), tolx);
        assertEquals("Center Y", expected.getCenterY(), actual.getCenterY(), toly);
        assertEquals("Width",    expected.getWidth(),   actual.getWidth(),   tolx*2);
        assertEquals("Height",   expected.getHeight(),  actual.getHeight(),  toly*2);
    }

    /**
     * Asserts that two images have the same origin and the same size.
     *
     * @param expected The image having the expected size.
     * @param actual   The image to compare with the expected one.
     */
    public static void assertBoundEquals(final RenderedImage expected, final RenderedImage actual) {
        assertEquals("Min X",  expected.getMinX(),   actual.getMinX());
        assertEquals("Min Y",  expected.getMinY(),   actual.getMinY());
        assertEquals("Width",  expected.getWidth(),  actual.getWidth());
        assertEquals("Height", expected.getHeight(), actual.getHeight());
    }

    /**
     * Serializes the given object in memory, deserialize it and ensures that the deserialized
     * object is equals to the original one. This method doesn't write anything to the disk.
     *
     * <p>If the serialization fails, then this method thrown an {@link AssertionError}
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
