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
package org.apache.sis.internal.feature.jts;

import java.awt.Shape;
import java.awt.geom.PathIterator;
import org.apache.sis.internal.feature.j2d.DecimatedShape;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link ShapeAdapter}.
 *
 * @author  Johann Sorel (Puzzle-GIS, Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
public final strictfp class ShapeAdapterTest extends TestCase {
    /**
     * The geometry factory used by the tests.
     */
    private final GeometryFactory factory;

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
        factory = new GeometryFactory();
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
        assertEquals("type", type, iterator.currentSegment(buffer));
        assertEquals("x", x, buffer[0], STRICT);
        assertEquals("y", y, buffer[1], STRICT);
        iterator.next();
    }

    /**
     * Verifies that the current segment is a {@link PathIterator#SEG_CLOSE}.
     * This method invokes {@link PathIterator#next()} after the verification.
     */
    private void assertSegmentClose() {
        assertFalse(iterator.isDone());
        assertEquals("type", PathIterator.SEG_CLOSE, iterator.currentSegment(buffer));
        iterator.next();
    }

    /**
     * Tests {@link ShapeAdapter} with a point.
     */
    @Test
    public void testPoint() {
        initialize(factory.createPoint(new Coordinate(10, 20)));
        assertSegmentEquals(PathIterator.SEG_MOVETO, 10, 20);
        assertTrue(iterator.isDone());
    }

    /**
     * Tests {@link ShapeAdapter} with a line string.
     */
    @Test
    public void testLineString() {
        initialize(factory.createLineString(new Coordinate[] {
            new Coordinate(3, 1),
            new Coordinate(7, 6),
            new Coordinate(5, 2)
        }));
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
        final LineString line1 = factory.createLineString(new Coordinate[] {
            new Coordinate(10, 12),
            new Coordinate(5, 2)
        });
        final LineString line2 = factory.createLineString(new Coordinate[] {
            new Coordinate(3, 1),
            new Coordinate(7, 6),
            new Coordinate(5, 2)
        });
        initialize(factory.createMultiLineString(new LineString[] {line1, line2}));
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
        final LinearRing ring = factory.createLinearRing(new Coordinate[] {
            new Coordinate(3, 1),
            new Coordinate(7, 6),
            new Coordinate(5, 2),
            new Coordinate(3, 1)
        });
        initialize(factory.createPolygon(ring, new LinearRing[0]));
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
        final LinearRing ring1 = factory.createLinearRing(new Coordinate[] {
            new Coordinate(3, 1),
            new Coordinate(7, 6),
            new Coordinate(5, 2),
            new Coordinate(3, 1)
        });
        final LinearRing ring2 = factory.createLinearRing(new Coordinate[] {
            new Coordinate(12, 3),
            new Coordinate(1, 9),
            new Coordinate(4, 6),
            new Coordinate(12, 3)
        });
        final Polygon polygon1 = factory.createPolygon(ring1, new LinearRing[0]);
        final Polygon polygon2 = factory.createPolygon(ring2, new LinearRing[0]);
        initialize(factory.createMultiPolygon(new Polygon[] {polygon1, polygon2}));

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
        final LineString line = factory.createLineString(new Coordinate[] {
            new Coordinate(0, 0),
            new Coordinate(1, 0),
            new Coordinate(2, 0)
        });
        final DecimatedShape shape = new DecimatedShape(new ShapeAdapter(line), new double[] {1.5, 1.5});
        assertTrue(shape.isValid());
        iterator = shape.getPathIterator(null);

        assertSegmentEquals(PathIterator.SEG_MOVETO, 0, 0);
        assertSegmentEquals(PathIterator.SEG_LINETO, 2, 0);
        assertTrue(iterator.isDone());
    }
}
