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
import java.util.Iterator;
import org.apache.sis.math.Vector;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static java.lang.Double.NaN;
import static org.junit.Assert.*;


/**
 * Base class of {@link Java2D}, {@link ESRI} and {@link JTS} implementation tests.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
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
     * Tests {@link Geometries#createPoint(double, double)} followed by {@link Geometries#tryGetCoordinate(Object)}.
     */
    @Test
    public void testTryGetCoordinate() {
        geometry = factory.createPoint(4, 5);
        assertNotNull("createPoint", geometry);
        assertArrayEquals("tryGetCoordinate", new double[] {4, 5}, factory.tryGetCoordinate(geometry), STRICT);
    }

    /**
     * Tests {@link Geometries#createPolyline(int, Vector...)}.
     * This method verifies the polylines by a call to {@link Geometries#tryGetEnvelope(Object)}.
     * Subclasses should perform more extensive tests by verifying the {@link #geometry} field.
     */
    @Test
    public void testCreatePolyline() {
        geometry = factory.createPolyline(2, Vector.create(new double[] {
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
}
