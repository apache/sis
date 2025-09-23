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
package org.apache.sis.geometry.wrapper.jts;

import java.util.Map;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.apache.sis.referencing.internal.shared.AffineTransform2D;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;

// Specific to the main branch:
import org.opengis.geometry.MismatchedDimensionException;


/**
 * Tests {@link JTS} implementation.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class JTSTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public JTSTest() {
    }

    /**
     * Tests {@link JTS#getCoordinateReferenceSystem(Geometry)}.
     *
     * @throws FactoryException if an EPSG code cannot be resolved.
     */
    @Test
    public void testGetCoordinateReferenceSystem() throws FactoryException {
        final GeometryFactory factory = Factory.INSTANCE.factory(false);
        final Geometry geometry = factory.createPoint(new CoordinateXY(5, 6));

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
        geometry.setUserData(Map.of(JTS.CRS_KEY, CommonCRS.NAD83.geographic()));
        assertEquals(CommonCRS.NAD83.geographic(), JTS.getCoordinateReferenceSystem(geometry));
        /*
         * Test CRS as srid.
         */
        geometry.setUserData(null);
        geometry.setSRID(4326);
        assertEquals(CommonCRS.WGS84.normalizedGeographic(), JTS.getCoordinateReferenceSystem(geometry));
    }

    /**
     * Tests {@link Wrapper#setCoordinateReferenceSystem(CoordinateReferenceSystem)}.
     */
    @Test
    public void testSetCoordinateReferenceSystem() {
        final GeometryFactory factory = Factory.INSTANCE.factory(false);
        final CoordinateReferenceSystem crs2D = CommonCRS.WGS84.geographic();
        final CoordinateReferenceSystem crs3D = CommonCRS.WGS84.geographic3D();

        {   /*
             * Test setting a 2D CRS on a 2 dimensional geometry.
             */
            final Geometry geometry = factory.createPoint(new CoordinateXY(5, 6));
            final GeometryWrapper wrapper = Geometries.wrap(geometry).orElseThrow();
            wrapper.setCoordinateReferenceSystem(crs2D);
            assertEquals(crs2D, wrapper.getCoordinateReferenceSystem());
        }

        {   /*
             * Test setting a 2D CRS on a 3 dimensional geometry.
             */
            final Geometry geometry = factory.createPoint(new Coordinate(5, 6, 7));
            final GeometryWrapper wrapper = Geometries.wrap(geometry).get();
            try {
                wrapper.setCoordinateReferenceSystem(crs2D);
                fail("Setting a 2D CRS on a 3D geometry must fail.");
            } catch (MismatchedDimensionException ex) {
                assertTrue(ex.getMessage().contains("crs"));
                // ok
            }
        }

        {   /*
             * Test setting a 3D CRS on a 3 dimensional geometry.
             */
            final Geometry geometry = factory.createPoint(new Coordinate(5, 6, 7));
            final GeometryWrapper wrapper = Geometries.wrap(geometry).get();
            wrapper.setCoordinateReferenceSystem(crs3D);
            assertEquals(crs3D, wrapper.getCoordinateReferenceSystem());
        }

        {   /*
             * Test setting a 3D CRS on a 2 dimensional geometry.
             */
            final Geometry geometry = factory.createPoint(new CoordinateXY(5, 6));
            final GeometryWrapper wrapper = Geometries.wrap(geometry).get();
            try {
                wrapper.setCoordinateReferenceSystem(crs3D);
                fail("Setting a 3D CRS on a 2D geometry must fail.");
            } catch (MismatchedDimensionException ex) {
                assertTrue(ex.getMessage().contains("crs"));
                // ok
            }
        }
    }

    /**
     * Tests {@link Wrapper#getEnvelope()}.
     */
    @Test
    public void testGetEnvelope() {
        final GeometryFactory factory = Factory.INSTANCE.factory(false);

        {   /*
             * Test 2D Envelope on a 2 dimensional geometry.
             */
            final CoordinateReferenceSystem crs = CommonCRS.WGS84.geographic();
            final Geometry geometry = factory.createPoint(new CoordinateXY(5, 6));
            final GeometryWrapper wrapper = Geometries.wrap(geometry).orElseThrow();
            wrapper.setCoordinateReferenceSystem(crs);
            final GeneralEnvelope envelope = wrapper.getEnvelope();
            assertEquals(crs, envelope.getCoordinateReferenceSystem());
            assertArrayEquals(new double[] {5, 6}, envelope.getLowerCorner().getCoordinate());
            assertArrayEquals(new double[] {5, 6}, envelope.getUpperCorner().getCoordinate());
        }

        {   /*
             * Test 3D Envelope on a 3 dimensional geometry.
             *
             * TODO: JTS does not set the Z values for geometry internal envelope.
             *       Should we loop over all coordinates in the geometry?
             */
            final CoordinateReferenceSystem crs = CommonCRS.WGS84.geographic3D();
            final Geometry geometry = factory.createPoint(new Coordinate(5, 6, 7));
            final GeometryWrapper wrapper = Geometries.wrap(geometry).orElseThrow();
            wrapper.setCoordinateReferenceSystem(crs);
            final GeneralEnvelope envelope = wrapper.getEnvelope();
            assertEquals(crs, envelope.getCoordinateReferenceSystem());
            assertArrayEquals(new double[] {5, 6, Double.NaN}, envelope.getLowerCorner().getCoordinate());
            assertArrayEquals(new double[] {5, 6, Double.NaN}, envelope.getUpperCorner().getCoordinate());
        }
    }

    /**
     * Tests various {@code transform} methods. This includes (sometimes indirectly):
     *
     * <ul>
     *   <li>{@link JTS#transform(Geometry, CoordinateReferenceSystem)}</li>
     *   <li>{@link JTS#transform(Geometry, CoordinateOperation)}</li>
     *   <li>{@link JTS#transform(Geometry, MathTransform)}</li>
     * </ul>
     *
     * @throws FactoryException if an EPSG code cannot be resolved.
     * @throws TransformException if a coordinate cannot be transformed.
     */
    @Test
    public void testTransform() throws FactoryException, TransformException {
        final GeometryFactory factory = Factory.INSTANCE.factory(false);
        final Geometry in = factory.createPoint(new CoordinateXY(5, 6));
        /*
         * Test transforming geometry without CRS.
         */
        assertSame(in, JTS.transform(in, CommonCRS.WGS84.geographic()));
        /*
         * Test axes inversion transform.
         */
        in.setUserData(CommonCRS.WGS84.normalizedGeographic());
        Point p = assertInstanceOf(Point.class, JTS.transform(in, CommonCRS.WGS84.geographic()));
        assertEquals(6, p.getX());
        assertEquals(5, p.getY());
        assertEquals(CommonCRS.WGS84.geographic(), p.getUserData());
        /*
         * Test affine transform. User data must be preserved.
         */
        final AffineTransform2D trs = new AffineTransform2D(1, 0, 0, 1, 10, 20);
        p = assertInstanceOf(Point.class, JTS.transform(in, trs));
        assertEquals(15, p.getX());
        assertEquals(26, p.getY());
    }

    /**
     * Tests various {@code transform} method with a three-dimensional geometry.
     *
     * @throws FactoryException if an EPSG code cannot be resolved.
     * @throws TransformException if a coordinate cannot be transformed.
     */
    @Test
    public void testTransform3D() throws FactoryException, TransformException {
        final GeometryFactory factory = Factory.INSTANCE.factory(false);
        final Point in = factory.createPoint(new Coordinate(5, 6, 2));
        assertEquals(Factory.TRIDIMENSIONAL, in.getCoordinateSequence().getDimension());
        assertSame(in, JTS.transform(in, CommonCRS.WGS84.geographic()));

        in.setUserData(CommonCRS.WGS84.geographic3D());
        final Point p = assertInstanceOf(Point.class, JTS.transform(in, CommonCRS.WGS84.geographic()));
        assertEquals(5, p.getX());
        assertEquals(6, p.getY());
        assertEquals(CommonCRS.WGS84.geographic(), p.getUserData());
        assertEquals(Factory.BIDIMENSIONAL, p.getCoordinateSequence().getDimension());
    }

    /**
     * Test {@code transform} method on an empty geometry.
     *
     * @throws FactoryException if an EPSG code cannot be resolved.
     * @throws TransformException if a coordinate cannot be transformed.
     */
    @Test
    public void testTransformEmpty() throws FactoryException, TransformException {
        final GeometryFactory factory = Factory.INSTANCE.factory(false);
        final Point in = factory.createPoint();
        in.setUserData(CommonCRS.WGS84.geographic());
        final Geometry r = JTS.transform(in, CommonCRS.WGS84.normalizedGeographic());
        assertEquals(CommonCRS.WGS84.normalizedGeographic(), r.getUserData());
        assertTrue(r.isEmpty());
    }
}
