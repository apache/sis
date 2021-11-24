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

import java.util.Collections;
import org.apache.sis.geometry.GeneralEnvelope;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.feature.GeometryWrapper;
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
     * Tests {@link Wrapper#setCoordinateReferenceSystem(org.opengis.referencing.crs.CoordinateReferenceSystem)}.
     *
     * @throws FactoryException if an EPSG code can not be resolved.
     */
    @Test
    public void testSetCoordinateReferenceSystem() throws FactoryException {
        final GeometryFactory factory = new GeometryFactory();

        {   /*
             * Test set a 2D CRS on a 2 dimensions geometry.
             */
            final CoordinateReferenceSystem crs = CommonCRS.ED50.geographic();
            final Geometry geometry = factory.createPoint(new CoordinateXY(5, 6));
            final GeometryWrapper<?> wrapper = Geometries.wrap(geometry).get();
            wrapper.setCoordinateReferenceSystem(crs);
            assertEquals(crs, wrapper.getCoordinateReferenceSystem());
        }

        {   /*
             * Test set a 2D CRS on a 3 dimensions geometry.
             * This case is tolerate for backward compatibility.
             */
            final CoordinateReferenceSystem crs = CommonCRS.ED50.geographic();
            final Geometry geometry = factory.createPoint(new Coordinate(5, 6, 7));
            final GeometryWrapper<?> wrapper = Geometries.wrap(geometry).get();
            wrapper.setCoordinateReferenceSystem(crs);
            assertEquals(crs, wrapper.getCoordinateReferenceSystem());
        }

        {   /*
             * Test set a 3D CRS on a 3 dimensions geometry.
             */
            final CoordinateReferenceSystem crs = CommonCRS.WGS84.geographic3D();
            final Geometry geometry = factory.createPoint(new Coordinate(5, 6, 7));
            final GeometryWrapper<?> wrapper = Geometries.wrap(geometry).get();
            wrapper.setCoordinateReferenceSystem(crs);
            assertEquals(crs, wrapper.getCoordinateReferenceSystem());
        }

        {   /*
             * Test set a 3D CRS on a 2 dimensions geometry.
             */
            final CoordinateReferenceSystem crs = CommonCRS.WGS84.geographic3D();
            final Geometry geometry = factory.createPoint(new CoordinateXY(5, 6));
            GeometryWrapper<?> wrapper = Geometries.wrap(geometry).get();
            try {
                wrapper.setCoordinateReferenceSystem(crs);
                fail("Setting a 3D crs on a 2D geometry must fail");
            } catch (MismatchedDimensionException ex) {
                //ok
            }
        }
    }

    /**
     * Tests {@link Wrapper#getEnvelope()}.
     *
     * @throws FactoryException if an EPSG code can not be resolved.
     */
    @Test
    public void testGetEnvelope() throws FactoryException {
        final GeometryFactory factory = new GeometryFactory();

        {   /*
             * Test 2D Envelope on a 2 dimensions geometry.
             */
            final CoordinateReferenceSystem crs = CommonCRS.ED50.geographic();
            final Geometry geometry = factory.createPoint(new CoordinateXY(5, 6));
            final GeometryWrapper<?> wrapper = Geometries.wrap(geometry).get();
            wrapper.setCoordinateReferenceSystem(crs);
            final GeneralEnvelope envelope = wrapper.getEnvelope();
            assertEquals(crs, envelope.getCoordinateReferenceSystem());
            assertArrayEquals(new double[]{5, 6}, envelope.getLowerCorner().getCoordinate(), STRICT);
            assertArrayEquals(new double[]{5, 6}, envelope.getUpperCorner().getCoordinate(), STRICT);
        }

        {   /*
             * Test 2D Envelope on a 3 dimensions geometry.
             * This case is tolerate for backward compatibility.
             */
            final CoordinateReferenceSystem crs = CommonCRS.ED50.geographic();
            final Geometry geometry = factory.createPoint(new Coordinate(5, 6, 7));
            final GeometryWrapper<?> wrapper = Geometries.wrap(geometry).get();
            wrapper.setCoordinateReferenceSystem(crs);
            final GeneralEnvelope envelope = wrapper.getEnvelope();
            assertEquals(crs, envelope.getCoordinateReferenceSystem());
            assertArrayEquals(new double[]{5, 6}, envelope.getLowerCorner().getCoordinate(), STRICT);
            assertArrayEquals(new double[]{5, 6}, envelope.getUpperCorner().getCoordinate(), STRICT);
        }

        {   /*
             * Test 3D Envelope on a 3 dimensions geometry.
             * TODO : JTS do not return 3D envelopes for geoemtry internal envelope
             * should we loop on the full geometry ?
             */
            /*
            final CoordinateReferenceSystem crs = CommonCRS.WGS84.geographic3D();
            final Geometry geometry = factory.createPoint(new Coordinate(5, 6, 7));
            final GeometryWrapper<?> wrapper = Geometries.wrap(geometry).get();
            wrapper.setCoordinateReferenceSystem(crs);
            final GeneralEnvelope envelope = wrapper.getEnvelope();
            assertEquals(crs, envelope.getCoordinateReferenceSystem());
            assertArrayEquals(new double[]{5, 6, 7}, envelope.getLowerCorner().getCoordinate(), STRICT);
            assertArrayEquals(new double[]{5, 6, 7}, envelope.getUpperCorner().getCoordinate(), STRICT);
            */
        }
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
}
