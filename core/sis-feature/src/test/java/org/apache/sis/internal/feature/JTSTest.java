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

import java.awt.geom.AffineTransform;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;
import org.apache.sis.referencing.CommonCRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiLineString;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

import static org.junit.Assert.*;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.operation.TransformException;


/**
 * Tests {@link JTS} implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final strictfp class JTSTest extends GeometriesTestCase {

    private static final double DELTA = 0.0000001;

    /**
     * Creates a new test case.
     */
    public JTSTest() {
        super(new JTS());
    }

    /**
     * Tests {@link JTS#createPolyline(int, Vector...)}.
     */
    @Test
    @Override
    public void testCreatePolyline() {
        super.testCreatePolyline();
        final MultiLineString mp = (MultiLineString) geometry;
        assertEquals("numGeometries", 2, mp.getNumGeometries());
        verifyTwoFirstGeometries(mp);
    }

    /**
     * Verifies the coordinates of the two first geometries of the given multi line string.
     * If there is more than 2 geometries, it is caller responsibility to verify the other ones.
     */
    private static void verifyTwoFirstGeometries(final MultiLineString mp) {
        assertArrayEquals(new Coordinate[] {
                new Coordinate(4, 5),
                new Coordinate(7, 9),
                new Coordinate(9, 3),
                new Coordinate(4, 5)}, mp.getGeometryN(0).getCoordinates());

        assertArrayEquals(new Coordinate[] {
                new Coordinate(-3, -2),
                new Coordinate(-2, -5),
                new Coordinate(-1, -6)}, mp.getGeometryN(1).getCoordinates());
    }

    /**
     * Tests {@link Geometries#tryMergePolylines(Object, Iterator)}.
     */
    @Test
    @Override
    public void testTryMergePolylines() {
        super.testTryMergePolylines();
        final MultiLineString mp = (MultiLineString) geometry;
        assertEquals("numGeometries", 3, mp.getNumGeometries());
        verifyTwoFirstGeometries(mp);
        assertArrayEquals(new Coordinate[] {
                new Coordinate(13, 11),
                new Coordinate(14, 12),
                new Coordinate(15, 11),
                new Coordinate(13, 10)}, mp.getGeometryN(2).getCoordinates());
    }

    /**
     * Tests {@link JTS#findCoordinateReferenceSystem(org.locationtech.jts.geom.Geometry) }.
     */
    @Test
    public void testFindCoordinateReferenceSystem() throws FactoryException {
        final GeometryFactory gf = new GeometryFactory();
        final Geometry geometry = gf.createPoint(new Coordinate(5, 6));

        CoordinateReferenceSystem crs = JTS.findCoordinateReferenceSystem(geometry);
        assertNull(crs);

        // test crs as user data
        geometry.setUserData(CommonCRS.ED50.geographic());
        assertEquals(CommonCRS.ED50.geographic(), JTS.findCoordinateReferenceSystem(geometry));

        // test crs as map value
        geometry.setUserData(Collections.singletonMap("crs", CommonCRS.NAD83.geographic()));
        assertEquals(CommonCRS.NAD83.geographic(), JTS.findCoordinateReferenceSystem(geometry));

        // test crs as srid
        geometry.setUserData(null);
        geometry.setSRID(4326);
        assertEquals(CommonCRS.WGS84.geographic(), JTS.findCoordinateReferenceSystem(geometry));
    }

    /**
     * Tests {@link JTS#transform(org.locationtech.jts.geom.Geometry, org.opengis.referencing.crs.CoordinateReferenceSystem) }.
     * Tests {@link JTS#transform(org.locationtech.jts.geom.Geometry, org.opengis.referencing.operation.CoordinateOperation) }.
     * Tests {@link JTS#transform(org.locationtech.jts.geom.Geometry, org.opengis.referencing.operation.MathTransform) }.
     */
    @Test
    public void testTransform() throws FactoryException, TransformException {
        final GeometryFactory gf = new GeometryFactory();
        final Geometry in = gf.createPoint(new Coordinate(5, 6));

        // test exception when transforming geometry without CRS.
        try {
            JTS.transform(in, CommonCRS.WGS84.geographic());
            fail("Geometry has no CRS, transform should have failed");
        } catch (TransformException ex) {
            //ok
        }

        // test axes inversion transform
        in.setUserData(CommonCRS.WGS84.normalizedGeographic());
        Geometry out = JTS.transform(in, CommonCRS.WGS84.geographic());
        assertTrue(out instanceof Point);
        assertEquals(6.0, ((Point) out).getX(), 0.0);
        assertEquals(5.0, ((Point) out).getY(), 0.0);
        assertEquals(CommonCRS.WGS84.geographic(), out.getUserData());

        // test affine transform, user data must be preserved
        final AffineTransform2D trs = new AffineTransform2D(1,0,0,1,10,20);
        out = JTS.transform(in, trs);
        assertTrue(out instanceof Point);
        assertEquals(15.0, ((Point) out).getX(), 0.0);
        assertEquals(26.0, ((Point) out).getY(), 0.0);
        assertEquals(CommonCRS.WGS84.normalizedGeographic(), out.getUserData());

    }

}
