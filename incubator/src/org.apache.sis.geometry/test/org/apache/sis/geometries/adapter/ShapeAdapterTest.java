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
package org.apache.sis.geometries.adapter;

import java.awt.Shape;
import java.awt.geom.PathIterator;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.LineString;
import org.apache.sis.geometries.LinearRing;
import org.apache.sis.geometries.Polygon;
import org.apache.sis.geometries.internal.shared.ArraySequence;
import org.apache.sis.geometries.math.NDArrays;
import org.apache.sis.geometries.math.SampleSystem;
import org.apache.sis.geometry.wrapper.j2d.DecimatedShape;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests {@link ShapeAdapter}.
 *
 * @author  Johann Sorel (Puzzle-GIS, Geomatys)
 */
public final class ShapeAdapterTest {

    private static SampleSystem CARTESIAN_2D = SampleSystem.cartesian(2);

    /**
     * An array of length 2 where to store (x,y) coordinates during path iteration.
     */
    private final double[] buffer;

    /**
     * Iterator over the shape to verify. Value is assigned by {@link #initialize(Geometry)}.
     */
    private PathIterator iterator;

    /**
     * Build a new test case.
     */
    public ShapeAdapterTest() {
        buffer  = new double[2];
    }

    /**
     * Initializes the test with the given geometry.
     */
    private void initialize(final Geometry geometry) {
        final Shape shape = new ShapeAdapter(geometry);
        iterator = shape.getPathIterator(null);
    }

    /**
     * Verifies that the current segment in the path iterator is of the given type.
     * This method invokes {@link PathIterator#next()} after the comparison.
     *
     * @param  type  expected type: {@link PathIterator#SEG_MOVETO} or {@link PathIterator#SEG_LINETO}.
     * @param  x     expected <var>x</var> coordinate.
     * @param  y     expected <var>y</var> coordinate.
     */
    private void assertSegmentEquals(final int type, final double x, final double y) {
        assertFalse(iterator.isDone());
        assertEquals(type, iterator.currentSegment(buffer));
        assertEquals(x, buffer[0]);
        assertEquals(y, buffer[1]);
        iterator.next();
    }

    /**
     * Verifies that the current segment is a {@link PathIterator#SEG_CLOSE}.
     * This method invokes {@link PathIterator#next()} after the verification.
     */
    private void assertSegmentClose() {
        assertFalse(iterator.isDone());
        assertEquals(PathIterator.SEG_CLOSE, iterator.currentSegment(buffer));
        iterator.next();
    }

    /**
     * Tests {@link ShapeAdapter} with a point.
     */
    @Test
    public void testPoint() {
        initialize(GeometryFactory.createPoint(CARTESIAN_2D, 10, 20));
        assertSegmentEquals(PathIterator.SEG_MOVETO, 10, 20);
        assertTrue(iterator.isDone());
    }

    /**
     * Tests {@link ShapeAdapter} with a line string.
     */
    @Test
    public void testLineString() {
        initialize(GeometryFactory.createLineString(new ArraySequence(NDArrays.of(CARTESIAN_2D, new double[]{
            3,1,
            7,6,
            5,2
        }))));
        assertSegmentEquals(PathIterator.SEG_MOVETO, 3, 1);
        assertSegmentEquals(PathIterator.SEG_LINETO, 7, 6);
        assertSegmentEquals(PathIterator.SEG_LINETO, 5, 2);
        assertTrue(iterator.isDone());
    }

    /**
     * Tests {@link ShapeAdapter} with a multi line string.
     */
    @Test
    public void testMultiLineString() {
        final LineString line1 = GeometryFactory.createLineString(new ArraySequence(NDArrays.of(CARTESIAN_2D, new double[]{
            10, 12,
            5, 2
        })));
        final LineString line2 = GeometryFactory.createLineString(new ArraySequence(NDArrays.of(CARTESIAN_2D, new double[]{
            3, 1,
            7, 6,
            5, 2
        })));
        initialize(GeometryFactory.createMultiLineString(line1, line2));
        assertSegmentEquals(PathIterator.SEG_MOVETO, 10, 12);
        assertSegmentEquals(PathIterator.SEG_LINETO, 5, 2);
        assertSegmentEquals(PathIterator.SEG_MOVETO, 3, 1);
        assertSegmentEquals(PathIterator.SEG_LINETO, 7, 6);
        assertSegmentEquals(PathIterator.SEG_LINETO, 5, 2);
        assertTrue(iterator.isDone());
    }

    /**
     * Tests {@link ShapeAdapter} with a polygon.
     */
    @Test
    public void testPolygon() {
        final LinearRing ring = GeometryFactory.createLinearRing(new ArraySequence(NDArrays.of(CARTESIAN_2D, new double[]{
            3, 1,
            7, 6,
            5, 2,
            3, 1
        })));
        initialize(GeometryFactory.createPolygon(ring, null));
        assertSegmentEquals(PathIterator.SEG_MOVETO, 3, 1);
        assertSegmentEquals(PathIterator.SEG_LINETO, 7, 6);
        assertSegmentEquals(PathIterator.SEG_LINETO, 5, 2);
        assertSegmentClose();
        assertTrue(iterator.isDone());
    }

    /**
     * Tests {@link ShapeAdapter} with a multi-polygon.
     */
    @Test
    public void testMultiPolygon() {
        final LinearRing ring1 = GeometryFactory.createLinearRing(new ArraySequence(NDArrays.of(CARTESIAN_2D, new double[]{
            3, 1,
            7, 6,
            5, 2,
            3, 1
        })));
        final LinearRing ring2 = GeometryFactory.createLinearRing(new ArraySequence(NDArrays.of(CARTESIAN_2D, new double[]{
            12, 3,
            1, 9,
            4, 6,
            12, 3
        })));
        final Polygon polygon1 = GeometryFactory.createPolygon(ring1, null);
        final Polygon polygon2 = GeometryFactory.createPolygon(ring2, null);
        initialize(GeometryFactory.createMultiPolygon(polygon1, polygon2));

        // First polygon.
        assertSegmentEquals(PathIterator.SEG_MOVETO, 3, 1);
        assertSegmentEquals(PathIterator.SEG_LINETO, 7, 6);
        assertSegmentEquals(PathIterator.SEG_LINETO, 5, 2);
        assertSegmentClose();

        // Second polygon.
        assertSegmentEquals(PathIterator.SEG_MOVETO, 12, 3);
        assertSegmentEquals(PathIterator.SEG_LINETO, 1, 9);
        assertSegmentEquals(PathIterator.SEG_LINETO, 4, 6);
        assertSegmentClose();
        assertTrue(iterator.isDone());
    }

    /**
     * Tests {@link ShapeAdapter} with the addition of a decimation.
     */
    @Test
    public void testAsDecimatedShapeLineString() {
        final LineString line = GeometryFactory.createLineString(new ArraySequence(NDArrays.of(CARTESIAN_2D, new double[]{
            0, 0,
            1, 0,
            2, 0
        })));
        final DecimatedShape shape = new DecimatedShape(new ShapeAdapter(line), new double[] {1.5, 1.5});
        assertTrue(shape.isValid());
        iterator = shape.getPathIterator(null);

        assertSegmentEquals(PathIterator.SEG_MOVETO, 0, 0);
        assertSegmentEquals(PathIterator.SEG_LINETO, 2, 0);
        assertTrue(iterator.isDone());
    }
}
