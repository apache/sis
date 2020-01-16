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
package org.apache.sis.internal.feature;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import org.opengis.geometry.Envelope;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.math.Vector;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static java.lang.Double.NaN;
import static org.junit.Assert.*;


/**
 * Base class of {@link Java2D}, {@link ESRI} and {@link JTS} implementation tests.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.0
 * @module
 */
public abstract strictfp class GeometriesTestCase extends TestCase {
    /**
     * The factory to test.
     */
    private final Geometries<?> factory;

    /**
     * The geometry created by the test. Provided for allowing sub-classes to perform additional verifications.
     */
    Object geometry;

    /**
     * Creates a new test for the given factory.
     */
    GeometriesTestCase(final Geometries<?> factory) {
        this.factory = factory;
    }

    /**
     * Tests {@link Geometries#createPoint(double, double)} followed by {@link Geometries#tryGetPointCoordinates(Object)}.
     */
    @Test
    public void testTryGetPointCoordinates() {
        geometry = factory.createPoint(4, 5);
        assertNotNull("createPoint", geometry);
        assertArrayEquals("tryGetPointCoordinates", new double[] {4, 5}, factory.tryGetPointCoordinates(geometry), STRICT);
    }

    /**
     * Tests {@link Geometries#createPolyline(boolean, int, Vector...)}.
     * This method verifies the polylines by a call to {@link Geometries#tryGetEnvelope(Object)}.
     * Subclasses should perform more extensive tests by verifying the {@link #geometry} field.
     */
    @Test
    public void testCreatePolyline() {
        geometry = factory.createPolyline(false, 2, Vector.create(new double[] {
                  4,   5,
                  7,   9,
                  9,   3,
                  4,   5,
                NaN, NaN,
                 -3,  -2,
                 -2,  -5,
                 -1,  -6}));

        final GeneralEnvelope env = factory.tryGetEnvelope(geometry);
        assertEquals("xmin", -3, env.getLower(0), STRICT);
        assertEquals("ymin", -6, env.getLower(1), STRICT);
        assertEquals("xmax",  9, env.getUpper(0), STRICT);
        assertEquals("ymax",  9, env.getUpper(1), STRICT);
    }

    /**
     * Tests {@link Geometries#tryMergePolylines(Object, Iterator)}.
     * This method verifies the polylines by a call to {@link Geometries#tryGetEnvelope(Object)}.
     * Subclasses should perform more extensive tests by verifying the {@link #geometry} field.
     */
    @Test
    public void testTryMergePolylines() {
        final Iterator<Object> c1 = Arrays.asList(
                factory.createPoint(  4,   5),
                factory.createPoint(  7,   9),
                factory.createPoint(  9,   3),
                factory.createPoint(  4,   5),
                factory.createPoint(NaN, NaN),
                factory.createPoint( -3,  -2),
                factory.createPoint( -2,  -5),
                factory.createPoint( -1,  -6)).iterator();

        final Iterator<Object> c2 = Arrays.asList(
                factory.createPoint( 14,  12),
                factory.createPoint( 15,  11),
                factory.createPoint( 13,  10)).iterator();

        final Object g1 = factory.tryMergePolylines(c1.next(), c1);
        final Object g2 = factory.tryMergePolylines(c2.next(), c2);
        geometry = factory.tryMergePolylines(g1, Arrays.asList(factory.createPoint(13, 11), g2).iterator());

        final GeneralEnvelope env = factory.tryGetEnvelope(geometry);
        assertEquals("xmin", -3, env.getLower(0), STRICT);
        assertEquals("ymin", -6, env.getLower(1), STRICT);
        assertEquals("xmax", 15, env.getUpper(0), STRICT);
        assertEquals("ymax", 12, env.getUpper(1), STRICT);
    }

    /**
     * Tests {@link Geometries#tryFormatWKT(Object, double)}.
     */
    @Test
    @DependsOnMethod("testCreatePolyline")
    public void testTryFormatWKT() {
        geometry = factory.createPolyline(false, 2, Vector.create(new double[] {4,5, 7,9, 9,3, -1,-6}));
        final String text = factory.tryFormatWKT(geometry, 0);
        assertNotNull(text);
        assertWktEquals("LINESTRING (4 5, 7 9, 9 3, -1 -6)", text);
    }

    @Test
    public void testEnvelopeToLinearRing() {
        // First, ensure that behavior is constant in case wrap-around is de-activated.
        final GeneralEnvelope noWrapNeeded = new GeneralEnvelope(new DefaultGeographicBoundingBox(-30, 24, -60, -51));
        final double[] expectedResult = {-30, -60, -30, -51, 24, -51, 24, -60, -30, -60};
        for (WraparoundStrategy method : WraparoundStrategy.values()) assertConversion(noWrapNeeded, method, expectedResult);

        // Check behavior when wrap-around is on first axis.
        final GeneralEnvelope wrapped = new GeneralEnvelope(CommonCRS.defaultGeographic());
        wrapped.setEnvelope(165, 32, -170, 33);
        final EnumMap<WraparoundStrategy,double[]> expected = new EnumMap<>(WraparoundStrategy.class);
        expected.put(WraparoundStrategy.NONE,       new double[] { 165, 32,  165, 33, -170, 33, -170, 32,  165, 32});
        expected.put(WraparoundStrategy.CONTIGUOUS, new double[] { 165, 32,  165, 33,  190, 33,  190, 32,  165, 32});
        expected.put(WraparoundStrategy.EXPAND,     new double[] {-180, 32, -180, 33,  180, 33,  180, 32, -180, 32});
        expected.put(WraparoundStrategy.SPLIT,      new double[] { 165, 32,  165, 33,  180, 33,  180, 32,  165, 32,
                                                                  -180, 32, -180, 33, -170, 33, -170, 32, -180, 32});
        assertConversion(wrapped, expected);

        wrapped.setEnvelope(177, -42, 190, 2);
        expected.put(WraparoundStrategy.NONE,       new double[] { 177, -42,  177, 2,  190, 2,  190, -42,  177, -42});
        expected.put(WraparoundStrategy.CONTIGUOUS, new double[] { 177, -42,  177, 2,  190, 2,  190, -42,  177, -42});
        expected.put(WraparoundStrategy.EXPAND,     new double[] {-180, -42, -180, 2,  180, 2,  180, -42, -180, -42});
        expected.put(WraparoundStrategy.SPLIT,      new double[] { 177, -42,  177, 2,  180, 2,  180, -42,  177, -42,
                                                                  -180, -42, -180, 2, -170, 2, -170, -42, -180, -42});

        // Check wrap-around on second axis.
        wrapped.setCoordinateReferenceSystem(CommonCRS.WGS84.geographic());
        wrapped.setEnvelope(2, 89, 3, 19);
        expected.put(WraparoundStrategy.NONE,       new double[] {2,   89, 2,  19, 3,  19, 3,   89, 2,   89});
        expected.put(WraparoundStrategy.CONTIGUOUS, new double[] {2,   89, 2, 199, 3, 199, 3,   89, 2,   89});
        expected.put(WraparoundStrategy.EXPAND,     new double[] {2, -180, 2, 180, 3, 180, 3, -180, 2, -180});
        expected.put(WraparoundStrategy.SPLIT,      new double[] {2,   89, 2, 180, 3, 180, 3,   89, 2,   89,
                                                                  2, -180, 2,  19, 3,  19, 3, -180, 2, -180});

        // Ensure fail fast on dimension ambiguity
        wrapped.setCoordinateReferenceSystem(null);
        Stream.of(WraparoundStrategy.CONTIGUOUS, WraparoundStrategy.EXPAND, WraparoundStrategy.SPLIT)
                .forEach(method
                        -> expectFailFast(()
                                -> factory.toGeometry(wrapped, method)
                        , IllegalArgumentException.class)
                );

        final GeneralEnvelope wrapped3d = new GeneralEnvelope(3);
        expectFailFast(() -> factory.toGeometry(wrapped3d, WraparoundStrategy.NONE), IllegalArgumentException.class);
    }

    public static void expectFailFast(Callable whichMustFail, Class<? extends Exception>... expectedErrorTypes) {
        try {
            final Object result = whichMustFail.call();
            fail("Fail fast expected, but successfully returned "+result);
        } catch (Exception e) {
            if (expectedErrorTypes == null || expectedErrorTypes.length < 1) return; // Any error accepted.
            for (Class errorType : expectedErrorTypes) {
                if (errorType.isInstance(e)) {
                    // Expected behavior
                    return;
                }
            }
            // Unexpected error
            fail(String.format(
                    "A fail fast occurred, but thrown error type is unexpected.%nError type: %s%nStack-trace: %s",
                    e.getClass().getCanonicalName(), e
            ));
        }
    }

    private void assertConversion(final Envelope source, final Map<WraparoundStrategy, double[]> expectedResults) {
        expectedResults.entrySet().forEach(entry -> assertConversion(source, entry.getKey(), entry.getValue()));
    }

    private void assertConversion(final Envelope source, final WraparoundStrategy method, final double[] expectedOrdinates) {
        final double[] result = factory.tryGetAllCoordinates(factory.toGeometry(source, method));
        assertArrayEquals("Point list for: "+method, expectedOrdinates, result, 1e-9);
    }

    /**
     * Verifies that a WKT is equal to the expected one. If the actual WKT is a multi-lines or multi-polygons,
     * then this method may modify the expected WKT accordingly. This adjustment is done for the ESRI case by
     * overriding this method.
     */
    void assertWktEquals(String expected, final String actual) {
        assertEquals(expected, actual);
    }
}
