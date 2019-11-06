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
package org.apache.sis.filter;

import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.Beyond;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.Crosses;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Disjoint;
import org.opengis.filter.spatial.Equals;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.spatial.Overlaps;
import org.opengis.filter.spatial.Touches;
import org.opengis.filter.spatial.Within;


/**
 * Tests {@link SpatialFunction} implementations.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 2.0
 * @since   2.0
 * @module
 */
public final strictfp class SpatialFunctionTest extends TestCase {
    /**
     * The factory to use for creating the objects to test.
     */
    private final FilterFactory2 FF;
    private final GeometryFactory GF;

    /**
     * Expressions used as constant for the tests.
     */
    private final Geometry GEOM_DISTANCE_1;
    private final Geometry GEOM_DISTANCE_3;
    private final Geometry GEOM_INTERSECT;
    private final Geometry GEOM_CONTAINS;
    private final Geometry GEOM_CROSSES;
    private final Geometry GEOM_TOUCHES;
    private final Geometry RIGHT_GEOMETRY;

    public SpatialFunctionTest() {
        FF = new DefaultFilterFactory();
        GF = new GeometryFactory();

        Coordinate[] coords = new Coordinate[5];
        coords[0] = new Coordinate(5, 1);
        coords[1] = new Coordinate(10, 1);
        coords[2] = new Coordinate(10, 4);
        coords[3] = new Coordinate(5, 4);
        coords[4] = new Coordinate(5, 1);
        LinearRing ring = GF.createLinearRing(coords);
        GEOM_DISTANCE_1 = GF.createPolygon(ring, new LinearRing[0]);

        coords = new Coordinate[5];
        coords[0] = new Coordinate(5, -1);
        coords[1] = new Coordinate(10, -1);
        coords[2] = new Coordinate(10, 2);
        coords[3] = new Coordinate(5, 2);
        coords[4] = new Coordinate(5, -1);
        ring = GF.createLinearRing(coords);
        GEOM_DISTANCE_3 = GF.createPolygon(ring, new LinearRing[0]);

        coords = new Coordinate[5];
        coords[0] = new Coordinate(7, 3);
        coords[1] = new Coordinate(9, 3);
        coords[2] = new Coordinate(9, 6);
        coords[3] = new Coordinate(7, 6);
        coords[4] = new Coordinate(7, 3);
        ring = GF.createLinearRing(coords);
        GEOM_INTERSECT = GF.createPolygon(ring, new LinearRing[0]);

        coords = new Coordinate[5];
        coords[0] = new Coordinate(1, 1);
        coords[1] = new Coordinate(11, 1);
        coords[2] = new Coordinate(11, 20);
        coords[3] = new Coordinate(1, 20);
        coords[4] = new Coordinate(1, 1);
        ring = GF.createLinearRing(coords);
        GEOM_CONTAINS = GF.createPolygon(ring, new LinearRing[0]);

        coords = new Coordinate[3];
        coords[0] = new Coordinate(4, 6);
        coords[1] = new Coordinate(7, 8);
        coords[2] = new Coordinate(12, 9);
        GEOM_CROSSES = GF.createLineString(coords);

        coords = new Coordinate[3];
        coords[0] = new Coordinate(4, 2);
        coords[1] = new Coordinate(7, 5);
        coords[2] = new Coordinate(9, 3);
        GEOM_TOUCHES = GF.createLineString(coords);


        coords = new Coordinate[5];
        coords[0] = new Coordinate(5, 5);
        coords[1] = new Coordinate(5, 10);
        coords[2] = new Coordinate(10,10);
        coords[3] = new Coordinate(10,5);
        coords[4] = new Coordinate(5,5);
        ring = GF.createLinearRing(coords);
        RIGHT_GEOMETRY = GF.createPolygon(ring, new LinearRing[0]);
    }

    @Test
    public void testBBOX() {
        BBOX bbox = FF.bbox(FF.literal(RIGHT_GEOMETRY), 1, 1, 6, 6, "EPSG:4326");
        assertTrue(bbox.evaluate(null));

        bbox = FF.bbox(FF.literal(RIGHT_GEOMETRY), -3, -2, 4, 1, "EPSG:4326");
        assertFalse(bbox.evaluate(null));
    }

    @Test
    public void testBeyond() {
        //we can not test units while using jts geometries

        Beyond beyond = FF.beyond(FF.literal(RIGHT_GEOMETRY), FF.literal(GEOM_DISTANCE_1), 1.5d, "m");
        assertFalse(beyond.evaluate(null));

        beyond = FF.beyond(FF.literal(RIGHT_GEOMETRY), FF.literal(GEOM_DISTANCE_3), 1.5d, "m");
        assertTrue(beyond.evaluate(null));
    }

    @Test
    public void testContains() {
        Contains contains = FF.contains(FF.literal(GEOM_CONTAINS),FF.literal(RIGHT_GEOMETRY));
        assertTrue(contains.evaluate(null));

        contains = FF.contains(FF.literal(GEOM_DISTANCE_1),FF.literal(RIGHT_GEOMETRY));
        assertFalse(contains.evaluate(null));
    }

    @Test
    public void testCrosses() {
        Crosses crosses = FF.crosses(FF.literal(GEOM_CONTAINS),FF.literal(RIGHT_GEOMETRY));
        assertFalse(crosses.evaluate(null));

        crosses = FF.crosses(FF.literal(GEOM_CROSSES),FF.literal(RIGHT_GEOMETRY));
        assertTrue(crosses.evaluate(null));

        crosses = FF.crosses(FF.literal(GEOM_DISTANCE_1),FF.literal(RIGHT_GEOMETRY));
        assertFalse(crosses.evaluate(null));
    }

    @Test
    public void testDWithin() {
        //we can not test units while using jts geometries

        DWithin within = FF.dwithin(FF.literal(RIGHT_GEOMETRY), FF.literal(GEOM_DISTANCE_1), 1.5d, "m");
        assertTrue(within.evaluate(null));

        within = FF.dwithin(FF.literal(RIGHT_GEOMETRY), FF.literal(GEOM_DISTANCE_3), 1.5d, "m");
        assertFalse(within.evaluate(null));
    }

    @Test
    public void testDisjoint() {
        Disjoint disjoint = FF.disjoint(FF.literal(GEOM_CONTAINS),FF.literal(RIGHT_GEOMETRY));
        assertFalse(disjoint.evaluate(null));

        disjoint = FF.disjoint(FF.literal(GEOM_CROSSES),FF.literal(RIGHT_GEOMETRY));
        assertFalse(disjoint.evaluate(null));

        disjoint = FF.disjoint(FF.literal(GEOM_DISTANCE_1),FF.literal(RIGHT_GEOMETRY));
        assertTrue(disjoint.evaluate(null));
    }

    @Test
    public void testEquals() {
        Equals equal = FF.equal(FF.literal(GEOM_CONTAINS),FF.literal(RIGHT_GEOMETRY));
        assertFalse(equal.evaluate(null));

        equal = FF.equal(FF.literal(GEOM_CROSSES),FF.literal(RIGHT_GEOMETRY));
        assertFalse(equal.evaluate(null));

        equal = FF.equal(FF.literal(GF.createGeometry(RIGHT_GEOMETRY)),FF.literal(RIGHT_GEOMETRY));
        assertTrue(equal.evaluate(null));
    }

    @Test
    public void testIntersect() {
        Intersects intersect = FF.intersects(FF.literal(GEOM_CONTAINS), FF.literal(RIGHT_GEOMETRY));
        assertTrue(intersect.evaluate(null));

        intersect = FF.intersects(FF.literal(GEOM_CROSSES), FF.literal(RIGHT_GEOMETRY));
        assertTrue(intersect.evaluate(null));

        intersect = FF.intersects(FF.literal(GEOM_INTERSECT), FF.literal(RIGHT_GEOMETRY));
        assertTrue(intersect.evaluate(null));

        intersect = FF.intersects(FF.literal(GEOM_DISTANCE_1), FF.literal(RIGHT_GEOMETRY));
        assertFalse(intersect.evaluate(null));

        intersect = FF.intersects(FF.literal(GEOM_DISTANCE_3), FF.literal(RIGHT_GEOMETRY));
        assertFalse(intersect.evaluate(null));
    }

    @Test
    public void testOverlaps() {
        Overlaps overlaps = FF.overlaps(FF.literal(GEOM_CONTAINS), FF.literal(RIGHT_GEOMETRY));
        assertFalse(overlaps.evaluate(null));

        overlaps = FF.overlaps(FF.literal(GEOM_DISTANCE_1), FF.literal(RIGHT_GEOMETRY));
        assertFalse(overlaps.evaluate(null));

        overlaps = FF.overlaps(FF.literal(GEOM_CROSSES), FF.literal(RIGHT_GEOMETRY));
        assertFalse(overlaps.evaluate(null));

        overlaps = FF.overlaps(FF.literal(GEOM_INTERSECT), FF.literal(RIGHT_GEOMETRY));
        assertTrue(overlaps.evaluate(null));
    }

    @Test
    public void testTouches() {
        Touches touches = FF.touches(FF.literal(GEOM_CONTAINS), FF.literal(RIGHT_GEOMETRY));
        assertFalse(touches.evaluate(null));

        touches = FF.touches(FF.literal(GEOM_CROSSES), FF.literal(RIGHT_GEOMETRY));
        assertFalse(touches.evaluate(null));

        touches = FF.touches(FF.literal(GEOM_DISTANCE_1), FF.literal(RIGHT_GEOMETRY));
        assertFalse(touches.evaluate(null));

        touches = FF.touches(FF.literal(GEOM_TOUCHES), FF.literal(RIGHT_GEOMETRY));
        assertTrue(touches.evaluate(null));
    }

    @Test
    public void testWithin() {
        Within within = FF.within(FF.literal(GEOM_CONTAINS), FF.literal(RIGHT_GEOMETRY));
        assertFalse(within.evaluate(null));

        within = FF.within(FF.literal(GEOM_CROSSES), FF.literal(RIGHT_GEOMETRY));
        assertFalse(within.evaluate(null));

        within = FF.within(FF.literal(GEOM_DISTANCE_1), FF.literal(RIGHT_GEOMETRY));
        assertFalse(within.evaluate(null));

        within = FF.within(FF.literal(GEOM_TOUCHES), FF.literal(RIGHT_GEOMETRY));
        assertFalse(within.evaluate(null));

        within = FF.within(FF.literal(RIGHT_GEOMETRY), FF.literal(GEOM_CONTAINS) );
        assertTrue(within.evaluate(null));
    }
}
