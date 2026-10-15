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
package org.apache.sis.geometries.internal.shared;

import org.apache.sis.maths.DataType;
import org.apache.sis.maths.Vector;
import org.apache.sis.maths.Vectors;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.apache.sis.geometries.GeometryTest;


/**
 * Tests {@link GeometryEnvelope}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class GeometryEnvelopeTest extends GeometryTest {

    private static Vector<?> vector(final double x, final double y) {
        return Vectors.create(CRS_2D, DataType.DOUBLE).set(new double[] {x, y});
    }

    @Test
    public void testConstructFromVectors() {
        final var envelope = new GeometryEnvelope(vector(1, 2), vector(3, 5));
        assertEquals(CRS_2D, envelope.getCoordinateReferenceSystem());
        assertEquals(1, envelope.getMinimum(0), TOLERANCE);
        assertEquals(2, envelope.getMinimum(1), TOLERANCE);
        assertEquals(3, envelope.getMaximum(0), TOLERANCE);
        assertEquals(5, envelope.getMaximum(1), TOLERANCE);
    }

    @Test
    public void testAddAndContains() {
        final Vector<?> position = vector(1, 2);
        final var envelope = new GeometryEnvelope(position, position);
        envelope.add(vector(-4, 7));
        envelope.add(Vectors.unmodifiable(vector(3, 0)));
        assertArrayEquals(new double[] {-4, 0}, envelope.getLower().toArrayDouble(), TOLERANCE);
        assertArrayEquals(new double[] { 3, 7}, envelope.getUpper().toArrayDouble(), TOLERANCE);
        assertTrue (envelope.contains(vector(0, 5)));
        assertTrue (envelope.contains(vector(3, 7)));
        assertFalse(envelope.contains(vector(4, 5)));
    }

    @Test
    public void testCorners() {
        final var envelope = new GeometryEnvelope(vector(1, 2), vector(3, 6));
        final Vector<?> lower  = envelope.getLower();
        final Vector<?> upper  = envelope.getUpper();
        final Vector<?> center = envelope.getCenter();
        assertEquals(CRS_2D, lower.getCoordinateReferenceSystem());
        assertArrayEquals(new double[] {1, 2}, lower .toArrayDouble(), TOLERANCE);
        assertArrayEquals(new double[] {3, 6}, upper .toArrayDouble(), TOLERANCE);
        assertArrayEquals(new double[] {2, 4}, center.toArrayDouble(), TOLERANCE);
        // The returned vectors are copies.
        lower.set(0, -100);
        assertEquals(1, envelope.getMinimum(0), TOLERANCE);
    }

    @Test
    public void testNoCRS() {
        final var envelope = new GeometryEnvelope(3);
        envelope.setEnvelope(0, 0, 0, 1, 2, 3);
        assertArrayEquals(new double[] {1, 2, 3}, envelope.getUpper().toArrayDouble(), TOLERANCE);
    }

    @Test
    public void testClone() {
        final var envelope = new GeometryEnvelope(vector(1, 2), vector(3, 6));
        final GeometryEnvelope clone = envelope.clone();
        assertEquals(envelope, clone);
        assertNotSame(envelope, clone);
    }
}
