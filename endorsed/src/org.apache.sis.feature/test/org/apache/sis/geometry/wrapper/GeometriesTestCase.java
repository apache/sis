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
package org.apache.sis.geometry.wrapper;

import java.util.List;
import java.util.EnumSet;
import java.util.Iterator;
import static java.lang.Double.NaN;
import java.nio.DoubleBuffer;
import org.opengis.geometry.Envelope;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.WraparoundMethod;
import org.apache.sis.math.Vector;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.crs.HardCodedCRS;


/**
 * Base class of tests for Java2D, <abbr>ESRI</abbr> and <abbr>JTS</abbr> implementations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 */
public abstract class GeometriesTestCase extends TestCase {
    /**
     * The factory to test.
     */
    protected final Geometries<?> factory;

    /**
     * The geometry created by the test. Provided for allowing sub-classes to perform additional verifications.
     */
    protected Object geometry;

    /**
     * The wrapper of {@link #geometry}.
     */
    private GeometryWrapper wrapper;

    /**
     * Creates a new test for the given factory.
     *
     * @param  factory  the factory to test.
     */
    protected GeometriesTestCase(final Geometries<?> factory) {
        this.factory = factory;
    }

    /**
     * Tests {@link Geometries#factory(GeometryLibrary)}.
     */
    @Test
    public void testFactory() {
        assertEquals(factory, Geometries.factory(factory.library));
    }

    /**
     * Initializes the {@link #wrapper} from current value of {@link #geometry}.
     */
    private void createWrapper() {
        wrapper = factory.castOrWrap(geometry);
        assertNotNull(wrapper);
    }

    /**
     * Tests {@link Geometries#createPoint(double, double)} followed by {@link GeometryWrapper#getPointCoordinates()}.
     */
    @Test
    public void testGetPointCoordinates() {
        geometry = factory.createPoint(4, 5);
        assertNotNull(geometry);
        createWrapper();
        assertArrayEquals(new double[] {4, 5}, wrapper.getPointCoordinates());
    }

    /**
     * Creates a line string or polygon with the specified (x,y) tuples.
     * The geometry is stored in the {@link #geometry} field?
     */
    private void setGeometry(final double... coordinates) {
        geometry = factory.createPolyline(false, false, Dimensions.XY, DoubleBuffer.wrap(coordinates));
    }

    /**
     * Tests {@link Geometries#createPolyline(boolean, int, Vector...)}.
     * This method verifies the polylines by a call to {@link GeometryWrapper#getEnvelope()}.
     * Subclasses should perform more extensive tests by verifying the {@link #geometry} field.
     */
    @Test
    public void testCreatePolyline() {
        setGeometry(4,   5,
                    7,   9,
                    9,   3,
                    4,   5,
                  NaN, NaN,
                   -3,  -2,
                   -2,  -5,
                   -1,  -6);

        createWrapper();
        final GeneralEnvelope env = wrapper.getEnvelope();
        assertEquals(-3, env.getLower(0), "xmin");
        assertEquals(-6, env.getLower(1), "ymin");
        assertEquals( 9, env.getUpper(0), "xmax");
        assertEquals( 9, env.getUpper(1), "ymax");
    }

    /**
     * Tests {@link GeometryWrapper#mergePolylines(Iterator)} (or actually tests its strategy).
     * This method verifies the polylines by a call to {@link GeometryWrapper#getEnvelope()}.
     * Subclasses should perform more extensive tests by verifying the {@link #geometry} field.
     */
    @Test
    public void testMergePolylines() {
        final Iterator<Object> c1 = List.of(
                factory.createPoint(  4,   5),
                factory.createPoint(  7,   9),
                factory.createPoint(  9,   3),
                factory.createPoint(  4,   5),
                factory.createPoint(NaN, NaN),
                factory.createPoint( -3,  -2),
                factory.createPoint( -2,  -5),
                factory.createPoint( -1,  -6)).iterator();

        final Iterator<Object> c2 = List.of(
                factory.createPoint( 14,  12),
                factory.createPoint( 15,  11),
                factory.createPoint( 13,  10)).iterator();

        final Object g1 = factory.castOrWrap(c1.next()).mergePolylines(c1);
        final Object g2 = factory.castOrWrap(c2.next()).mergePolylines(c2);
        geometry = factory.castOrWrap(g1).mergePolylines(List.of(factory.createPoint(13, 11), g2).iterator());

        createWrapper();
        final GeneralEnvelope env = wrapper.getEnvelope();
        assertEquals(-3, env.getLower(0), "xmin");
        assertEquals(-6, env.getLower(1), "ymin");
        assertEquals(15, env.getUpper(0), "xmax");
        assertEquals(12, env.getUpper(1), "ymax");
    }

    /**
     * Tests {@link GeometryWrapper#formatWKT(double)}.
     */
    @Test
    public void testFormatWKT() {
        setGeometry(4,  5,
                    7,  9,
                    9,  3,
                   -1, -6);

        createWrapper();
        final String text = wrapper.formatWKT(0);
        assertNotNull(text);
        assertWktEquals("LINESTRING (4 5, 7 9, 9 3, -1 -6)", text);
    }

    /**
     * Tests {@link Geometries#toGeometry2D(Envelope, WraparoundMethod)} without longitude wraparound.
     */
    @Test
    public void testToGeometry2D() {
        final var envelope = new GeneralEnvelope(HardCodedCRS.WGS84);
        envelope.setRange(0, -30,  24);
        envelope.setRange(1, -60, -51);
        final double[] expected = {-30, -60, -30, -51, 24, -51, 24, -60, -30, -60};
        for (WraparoundMethod method : WraparoundMethod.values()) {
            if (method != WraparoundMethod.NORMALIZE) {
                assertToGeometryEquals(envelope, method, expected);
            }
        }
    }

    /**
     * Tests {@link Geometries#toGeometry2D(Envelope, WraparoundMethod)} with longitude wraparound.
     */
    @Test
    public void testToGeometryWraparound() {
        final var e = new GeneralEnvelope(HardCodedCRS.WGS84);
        e.setRange(0, 165, -170);
        e.setRange(1,  32,   33);
        assertToGeometryEquals(e, WraparoundMethod.NONE,              165, 32,  165, 33, -170, 33, -170, 32,  165, 32);
        assertToGeometryEquals(e, WraparoundMethod.CONTIGUOUS,        165, 32,  165, 33,  190, 33,  190, 32,  165, 32);
        assertToGeometryEquals(e, WraparoundMethod.CONTIGUOUS_LOWER, -195, 32, -195, 33, -170, 33, -170, 32, -195, 32);
        assertToGeometryEquals(e, WraparoundMethod.CONTIGUOUS_UPPER,  165, 32,  165, 33,  190, 33,  190, 32,  165, 32);
        assertToGeometryEquals(e, WraparoundMethod.EXPAND,           -180, 32, -180, 33,  180, 33,  180, 32, -180, 32);
        assertToGeometryEquals(e, WraparoundMethod.SPLIT,             165, 32,  165, 33,  180, 33,  180, 32,  165, 32,
                                                                     -180, 32, -180, 33, -170, 33, -170, 32, -180, 32);
        e.setRange(0, 177, -170);
        e.setRange(1, -42,    2);
        assertToGeometryEquals(e, WraparoundMethod.NONE,              177, -42,  177, 2, -170, 2, -170, -42,  177, -42);
        assertToGeometryEquals(e, WraparoundMethod.CONTIGUOUS,       -183, -42, -183, 2, -170, 2, -170, -42, -183, -42);
        assertToGeometryEquals(e, WraparoundMethod.CONTIGUOUS_UPPER,  177, -42,  177, 2,  190, 2,  190, -42,  177, -42);
        assertToGeometryEquals(e, WraparoundMethod.CONTIGUOUS_LOWER, -183, -42, -183, 2, -170, 2, -170, -42, -183, -42);
        assertToGeometryEquals(e, WraparoundMethod.EXPAND,           -180, -42, -180, 2,  180, 2,  180, -42, -180, -42);
        assertToGeometryEquals(e, WraparoundMethod.SPLIT,             177, -42,  177, 2,  180, 2,  180, -42,  177, -42,
                                                                     -180, -42, -180, 2, -170, 2, -170, -42, -180, -42);
    }

    /**
     * Tests {@link Geometries#toGeometry2D(Envelope, WraparoundMethod)} from a four-dimensional envelope.
     * Ensures that the horizontal component is identified, but otherwise <em>no</em> axis swapping is done.
     */
    @Test
    public void testToGeometryFrom4D() {
        final var e = new GeneralEnvelope(HardCodedCRS.GEOID_4D_MIXED_ORDER);
        e.setRange(0,  -20,   12);      // Height
        e.setRange(1, 1000, 1007);      // Time
        e.setRange(2,    2,    3);      // Latitude
        e.setRange(3,   89,   19);      // Longitude (span anti-meridian).
        assertToGeometryEquals(e, WraparoundMethod.NONE,       2,   89, 2,  19, 3,  19, 3,   89, 2,   89);
        assertToGeometryEquals(e, WraparoundMethod.CONTIGUOUS, 2, -271, 2,  19, 3,  19, 3, -271, 2, -271);
        assertToGeometryEquals(e, WraparoundMethod.EXPAND,     2, -180, 2, 180, 3, 180, 3, -180, 2, -180);
        assertToGeometryEquals(e, WraparoundMethod.SPLIT,      2,   89, 2, 180, 3, 180, 3,   89, 2,   89,
                                                               2, -180, 2,  19, 3,  19, 3, -180, 2, -180);
    }

    /**
     * Tests conversion of a world-wide envelope using all supported wraparound methods.
     */
    @Test
    public void testWorldToGeometry2D() {
        final EnumSet<WraparoundMethod> methods = EnumSet.allOf(WraparoundMethod.class);
        assertTrue(methods.remove(WraparoundMethod.NORMALIZE));
        Envelope2D env2d = new Envelope2D(HardCodedCRS.WGS84, -180, -90, 360, 180);
        for (WraparoundMethod method : methods) {
            assertToGeometryEquals(env2d, method, -180, -90,  -180, 90,  180, 90,  180, -90, -180, -90);
        }
        env2d = new Envelope2D(HardCodedCRS.WGS84_LATITUDE_FIRST, -90, -180, 180, 360);
        for (WraparoundMethod method : methods) {
            assertToGeometryEquals(env2d, method, -90, -180,  -90, 180,  90, 180,  90, -180,  -90, -180);
        }
    }

    /**
     * Verifies that call to {@link Geometries#toGeometry2D(Envelope, WraparoundMethod)}
     * with the given argument values produces a geometry will all expected coordinates.
     */
    private void assertToGeometryEquals(final Envelope source, final WraparoundMethod method, final double... expected) {
        wrapper = factory.toGeometry2D(source, method);
        geometry = wrapper.factory().getGeometry(wrapper);
        final double[] result = wrapper.getAllCoordinates();
        assertArrayEquals(expected, result, 1e-9);
    }

    /**
     * Verifies that a WKT is equal to the expected one. If the actual WKT is a multi-lines or multi-polygons,
     * then this method may modify the expected WKT accordingly. This adjustment is done for the ESRI case by
     * overriding this method.
     *
     * @param  expected  the expected WKT string.
     * @param  actual    the actual WKT string.
     */
    protected void assertWktEquals(String expected, final String actual) {
        assertEquals(expected, actual);
    }
}
