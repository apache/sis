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
import java.util.Collections;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link JTS} implementation.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final strictfp class JTSTest extends TestCase {

    private static double DELTA = 0.000000001d;
    private static final GeometryFactory GF = new GeometryFactory();

    /**
     * Tests {@link JTS#getCoordinateReferenceSystem(Geometry)}.
     *
     * @throws FactoryException if an EPSG code can not be resolved.
     */
    @Test
    public void testGetCoordinateReferenceSystem() throws FactoryException {
        final GeometryFactory factory = new GeometryFactory();
        final Geometry geometry = factory.createPoint(new Coordinate(5, 6));

        CoordinateReferenceSystem crs = JTS.getCoordinateReferenceSystem(geometry);
        assertNull(crs);
        /*
         * Test CRS as user data.
         */
        geometry.setUserData(CommonCRS.ED50.geographic());
        assertEquals(CommonCRS.ED50.geographic(), JTS.getCoordinateReferenceSystem(geometry));
        /*
         * Test CRS as map value.
         */
        geometry.setUserData(Collections.singletonMap(JTS.CRS_KEY, CommonCRS.NAD83.geographic()));
        assertEquals(CommonCRS.NAD83.geographic(), JTS.getCoordinateReferenceSystem(geometry));
        /*
         * Test CRS as srid.
         */
        geometry.setUserData(null);
        geometry.setSRID(4326);
        assertEquals(CommonCRS.WGS84.geographic(), JTS.getCoordinateReferenceSystem(geometry));
    }

    /**
     * Tests various {@code transform} methods. This includes (sometime indirectly):
     *
     * <ul>
     *   <li>{@link JTS#transform(Geometry, CoordinateReferenceSystem)}</li>
     *   <li>{@link JTS#transform(Geometry, CoordinateOperation)}</li>
     *   <li>{@link JTS#transform(Geometry, MathTransform)}</li>
     * </ul>
     *
     * @throws FactoryException if an EPSG code can not be resolved.
     * @throws TransformException if a coordinate can not be transformed.
     */
    @Test
    public void testTransform() throws FactoryException, TransformException {
        final GeometryFactory factory = new GeometryFactory();
        final Geometry in = factory.createPoint(new Coordinate(5, 6));
        /*
         * Test transforming geometry without CRS.
         */
        assertSame(in, JTS.transform(in, CommonCRS.WGS84.geographic()));
        /*
         * Test axes inversion transform.
         */
        in.setUserData(CommonCRS.WGS84.normalizedGeographic());
        Geometry out = JTS.transform(in, CommonCRS.WGS84.geographic());
        assertTrue(out instanceof Point);
        assertEquals(6, ((Point) out).getX(), STRICT);
        assertEquals(5, ((Point) out).getY(), STRICT);
        assertEquals(CommonCRS.WGS84.geographic(), out.getUserData());
        /*
         * Test affine transform. User data must be preserved.
         */
        final AffineTransform2D trs = new AffineTransform2D(1, 0, 0, 1, 10, 20);
        out = JTS.transform(in, trs);
        assertTrue(out instanceof Point);
        assertEquals(15, ((Point) out).getX(), STRICT);
        assertEquals(26, ((Point) out).getY(), STRICT);
    }

    /**
     * Tests {@link JTS#asShape(org.locationtech.jts.geom.Geometry, org.opengis.referencing.operation.MathTransform)} with a point type geometry.
     */
    @Test
    public void testAsShapePoint() {

        final Point point = GF.createPoint(new Coordinate(10, 20));

        final Shape shape = JTS.asShape(point, null);
        final PathIterator ite = shape.getPathIterator(null);

        double[] buffer = new double[2];
        int type;

        assertFalse(ite.isDone());
        type = ite.currentSegment(buffer);
        assertEquals(10, buffer[0], DELTA);
        assertEquals(20, buffer[1], DELTA);
        assertEquals(PathIterator.SEG_MOVETO, type);
        ite.next();

        assertTrue(ite.isDone());
    }

    /**
     * Tests {@link JTS#asShape(org.locationtech.jts.geom.Geometry, org.opengis.referencing.operation.MathTransform)} with a line string type geometry.
     */
    @Test
    public void testAsShapeLineString() {

        final LineString line = GF.createLineString(new Coordinate[]{
            new Coordinate(3, 1),
            new Coordinate(7, 6),
            new Coordinate(5, 2)
        });

        final Shape shape = JTS.asShape(line, null);
        final PathIterator ite = shape.getPathIterator(null);

        double[] buffer = new double[2];
        int type;

        assertFalse(ite.isDone());
        type = ite.currentSegment(buffer);
        assertEquals(3, buffer[0], DELTA);
        assertEquals(1, buffer[1], DELTA);
        assertEquals(PathIterator.SEG_MOVETO, type);
        ite.next();

        assertFalse(ite.isDone());
        type = ite.currentSegment(buffer);
        assertEquals(7, buffer[0], DELTA);
        assertEquals(6, buffer[1], DELTA);
        assertEquals(PathIterator.SEG_LINETO, type);
        ite.next();

        assertFalse(ite.isDone());
        type = ite.currentSegment(buffer);
        assertEquals(5, buffer[0], DELTA);
        assertEquals(2, buffer[1], DELTA);
        assertEquals(PathIterator.SEG_LINETO, type);
        ite.next();

        assertTrue(ite.isDone());
    }

    /**
     * Tests {@link JTS#asShape(org.locationtech.jts.geom.Geometry, org.opengis.referencing.operation.MathTransform)} with a multi line string type geometry.
     */
    @Test
    public void testAsShapeMultiLineString() {

        final LineString line1 = GF.createLineString(new Coordinate[]{
            new Coordinate(10, 12),
            new Coordinate(5, 2)
        });
        final LineString line2 = GF.createLineString(new Coordinate[]{
            new Coordinate(3, 1),
            new Coordinate(7, 6),
            new Coordinate(5, 2)
        });

        final MultiLineString ml = GF.createMultiLineString(new LineString[]{line1,line2});
        final Shape shape = JTS.asShape(ml, null);
        final PathIterator ite = shape.getPathIterator(null);

        double[] buffer = new double[2];
        int type;

        assertFalse(ite.isDone());
        type = ite.currentSegment(buffer);
        assertEquals(10, buffer[0], DELTA);
        assertEquals(12, buffer[1], DELTA);
        assertEquals(PathIterator.SEG_MOVETO, type);
        ite.next();

        assertFalse(ite.isDone());
        type = ite.currentSegment(buffer);
        assertEquals(5, buffer[0], DELTA);
        assertEquals(2, buffer[1], DELTA);
        assertEquals(PathIterator.SEG_LINETO, type);
        ite.next();

        assertFalse(ite.isDone());
        type = ite.currentSegment(buffer);
        assertEquals(3, buffer[0], DELTA);
        assertEquals(1, buffer[1], DELTA);
        assertEquals(PathIterator.SEG_MOVETO, type);
        ite.next();

        assertFalse(ite.isDone());
        type = ite.currentSegment(buffer);
        assertEquals(7, buffer[0], DELTA);
        assertEquals(6, buffer[1], DELTA);
        assertEquals(PathIterator.SEG_LINETO, type);
        ite.next();

        assertFalse(ite.isDone());
        type = ite.currentSegment(buffer);
        assertEquals(5, buffer[0], DELTA);
        assertEquals(2, buffer[1], DELTA);
        assertEquals(PathIterator.SEG_LINETO, type);
        ite.next();

        assertTrue(ite.isDone());
    }

    /**
     * Tests {@link JTS#asShape(org.locationtech.jts.geom.Geometry, org.opengis.referencing.operation.MathTransform)} with a polygon type geometry.
     */
    @Test
    public void testAsShapePolygon() {

        final LinearRing ring = GF.createLinearRing(new Coordinate[]{
            new Coordinate(3, 1),
            new Coordinate(7, 6),
            new Coordinate(5, 2),
            new Coordinate(3, 1)
        });

        final Polygon polygon = GF.createPolygon(ring, new LinearRing[0]);

        final Shape shape = JTS.asShape(polygon, null);
        final PathIterator ite = shape.getPathIterator(null);

        double[] buffer = new double[2];
        int type;

        assertFalse(ite.isDone());
        type = ite.currentSegment(buffer);
        assertEquals(3, buffer[0], DELTA);
        assertEquals(1, buffer[1], DELTA);
        assertEquals(PathIterator.SEG_MOVETO, type);
        ite.next();

        assertFalse(ite.isDone());
        type = ite.currentSegment(buffer);
        assertEquals(7, buffer[0], DELTA);
        assertEquals(6, buffer[1], DELTA);
        assertEquals(PathIterator.SEG_LINETO, type);
        ite.next();

        assertFalse(ite.isDone());
        type = ite.currentSegment(buffer);
        assertEquals(5, buffer[0], DELTA);
        assertEquals(2, buffer[1], DELTA);
        assertEquals(PathIterator.SEG_LINETO, type);
        ite.next();

        assertFalse(ite.isDone());
        type = ite.currentSegment(buffer);
        assertEquals(PathIterator.SEG_CLOSE, type);
        ite.next();

        assertTrue(ite.isDone());
    }

    /**
     * Tests {@link JTS#asShape(org.locationtech.jts.geom.Geometry, org.opengis.referencing.operation.MathTransform)} with a multi polygon type geometry.
     */
    @Test
    public void testAsShapeMultiPolygon() {

        final LinearRing ring1 = GF.createLinearRing(new Coordinate[]{
            new Coordinate(3, 1),
            new Coordinate(7, 6),
            new Coordinate(5, 2),
            new Coordinate(3, 1)
        });

        final LinearRing ring2 = GF.createLinearRing(new Coordinate[]{
            new Coordinate(12, 3),
            new Coordinate(1, 9),
            new Coordinate(4, 6),
            new Coordinate(12, 3)
        });

        final Polygon polygon1 = GF.createPolygon(ring1, new LinearRing[0]);
        final Polygon polygon2 = GF.createPolygon(ring2, new LinearRing[0]);
        final MultiPolygon poly = GF.createMultiPolygon(new Polygon[]{polygon1,polygon2});

        final Shape shape = JTS.asShape(poly, null);
        final PathIterator ite = shape.getPathIterator(null);

        double[] buffer = new double[2];
        int type;

        //first polygon
        assertFalse(ite.isDone());
        type = ite.currentSegment(buffer);
        assertEquals(3, buffer[0], DELTA);
        assertEquals(1, buffer[1], DELTA);
        assertEquals(PathIterator.SEG_MOVETO, type);
        ite.next();

        assertFalse(ite.isDone());
        type = ite.currentSegment(buffer);
        assertEquals(7, buffer[0], DELTA);
        assertEquals(6, buffer[1], DELTA);
        assertEquals(PathIterator.SEG_LINETO, type);
        ite.next();

        assertFalse(ite.isDone());
        type = ite.currentSegment(buffer);
        assertEquals(5, buffer[0], DELTA);
        assertEquals(2, buffer[1], DELTA);
        assertEquals(PathIterator.SEG_LINETO, type);
        ite.next();

        assertFalse(ite.isDone());
        type = ite.currentSegment(buffer);
        assertEquals(PathIterator.SEG_CLOSE, type);
        ite.next();

        // second polygon
        assertFalse(ite.isDone());
        type = ite.currentSegment(buffer);
        assertEquals(12, buffer[0], DELTA);
        assertEquals(3, buffer[1], DELTA);
        assertEquals(PathIterator.SEG_MOVETO, type);
        ite.next();

        assertFalse(ite.isDone());
        type = ite.currentSegment(buffer);
        assertEquals(1, buffer[0], DELTA);
        assertEquals(9, buffer[1], DELTA);
        assertEquals(PathIterator.SEG_LINETO, type);
        ite.next();

        assertFalse(ite.isDone());
        type = ite.currentSegment(buffer);
        assertEquals(4, buffer[0], DELTA);
        assertEquals(6, buffer[1], DELTA);
        assertEquals(PathIterator.SEG_LINETO, type);
        ite.next();

        assertFalse(ite.isDone());
        type = ite.currentSegment(buffer);
        assertEquals(PathIterator.SEG_CLOSE, type);
        ite.next();

        assertTrue(ite.isDone());
    }

    /**
     * Tests {@link JTS#asDecimatedShape(org.locationtech.jts.geom.Geometry, double[])} with a line string type geometry.
     */
    @Test
    public void testAsDecimatedShapeLineString() {

        final LineString line = GF.createLineString(new Coordinate[]{
            new Coordinate(0, 0),
            new Coordinate(1, 0),
            new Coordinate(2, 0)
        });

        final Shape shape = JTS.asDecimatedShape(line, new double[]{1.5, 1.5});
        final PathIterator ite = shape.getPathIterator(null);

        double[] buffer = new double[2];
        int type;

        assertFalse(ite.isDone());
        type = ite.currentSegment(buffer);
        assertEquals(0, buffer[0], DELTA);
        assertEquals(0, buffer[1], DELTA);
        assertEquals(PathIterator.SEG_MOVETO, type);
        ite.next();

        assertFalse(ite.isDone());
        type = ite.currentSegment(buffer);
        assertEquals(2, buffer[0], DELTA);
        assertEquals(0, buffer[1], DELTA);
        assertEquals(PathIterator.SEG_LINETO, type);
        ite.next();

        assertTrue(ite.isDone());
    }
}
