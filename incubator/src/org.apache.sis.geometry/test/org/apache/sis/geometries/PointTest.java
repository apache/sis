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
package org.apache.sis.geometries;

import java.util.List;
import java.util.Map;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.SampleSystem;
import org.apache.sis.maths.Tuple;
import org.apache.sis.maths.Vector;
import org.apache.sis.maths.Vectors;
import org.apache.sis.referencing.CommonCRS;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;


/**
 * Tests {@link Point}.
 *
 * <p>This class tests the behavior mandated by {@link Point} on any implementation.
 * Subclasses provide the implementation to test by implementing
 * {@link #createPoint(CoordinateReferenceSystem, double[])}.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class PointTest {

    protected static final CoordinateReferenceSystem CRS_2D = CommonCRS.WGS84.normalizedGeographic();
    protected static final CoordinateReferenceSystem CRS_3D = CommonCRS.WGS84.geographic3D();

    private static final double[] POSITION_2D = {10, 5};
    private static final double[] POSITION_3D = {10, 5, 100};

    protected PointTest() {
    }

    /**
     * Creates a point at the given location in the given coordinate reference system.
     *
     * @param  crs          the coordinate reference system of the point to create, not null.
     * @param  coordinates  the coordinates of the point, in the axis order of the given system.
     * @return a new point at the given location, never null.
     */
    protected abstract Point createPoint(CoordinateReferenceSystem crs, double... coordinates);

    /**
     * Creates a point at {@link #POSITION_2D} in {@link #CRS_2D}.
     */
    private Point createPoint2D() {
        return createPoint(CRS_2D, POSITION_2D.clone());
    }

    /**
     * Creates a point at {@link #POSITION_3D} in {@link #CRS_3D}.
     */
    private Point createPoint3D() {
        return createPoint(CRS_3D, POSITION_3D.clone());
    }

    /**
     * Test of {@code getPosition()}.
     */
    @Test
    public void testGetPosition() {
        final Tuple<?> position = createPoint2D().getPosition();
        assertNotNull(position);
        assertEquals(2, position.getDimension());
        assertArrayEquals(POSITION_2D, position.toArrayDouble());
        assertArrayEquals(POSITION_3D, createPoint3D().getPosition().toArrayDouble());
    }

    /**
     * Test of {@code getAttribute(String)}.
     */
    @Test
    public void testGetAttribute() {
        final Point point = createPoint2D();
        final Tuple<?> position = point.getAttribute(DataPointsType.ATT_POSITION);
        assertNotNull(position, "The coordinates are always carried by the positions attribute.");
        assertArrayEquals(POSITION_2D, position.toArrayDouble());
        assertNull(point.getAttribute("Not an attribute of this point."));
    }

    /**
     * Test of {@code setAttribute(String, Tuple)}.
     */
    @Test
    public void testSetAttribute() {
        final Point point = createPoint2D();
        final Tuple<?> moved = point.getPosition().copy();
        moved.set(new double[] {20, 15});
        point.setAttribute(DataPointsType.ATT_POSITION, moved);
        assertArrayEquals(new double[] {20, 15}, point.getPosition().toArrayDouble(),
                          "Setting the positions attribute shall move the point.");
    }

    /**
     * Test of {@code asDataPoint()}.
     */
    @Test
    public void testAsDataPoint() {
        final Point point = createPoint2D();
        final DataPoints points = point.asDataPoint();
        assertNotNull(points);
        assertEquals(1, points.size(), "A point is a sequence of a single position.");
        assertFalse(points.isEmpty());
        assertArrayEquals(POSITION_2D, points.getPosition(0).toArrayDouble());
        assertArrayEquals(POSITION_2D, points.getPoint(0).getPosition().toArrayDouble());
        assertEquals(CRS_2D, points.getCoordinateReferenceSystem());
        assertNotNull(points.getType());
    }

    /**
     * Test of {@code getGeometryType()}.
     */
    @Test
    public void testGetGeometryType() {
        assertEquals(GeometryType.POINT, createPoint2D().getGeometryType());
    }

    /**
     * Test of {@code getTopologicDimension()}.
     */
    @Test
    public void testGetTopologicDimension() {
        assertEquals(0, createPoint2D().getTopologicDimension());
        assertEquals(0, createPoint3D().getTopologicDimension());
    }

    /**
     * Test of {@code getSegments()}.
     */
    @Test
    public void testGetSegments() {
        final List<Primitive> segments = createPoint2D().getSegments();
        assertNotNull(segments);
        assertTrue(segments.isEmpty(), "A point cannot be decomposed.");
    }

    /**
     * Test of {@code isCycle()}.
     */
    @Test
    public void testIsCycle() {
        assertTrue(createPoint2D().isCycle(), "The boundary of a point is empty.");
    }

    /**
     * Test of {@code isSimple()}.
     */
    @Test
    public void testIsSimple() {
        assertTrue(createPoint2D().isSimple(), "A single location can neither self-intersect nor self-tangent.");
    }

    /**
     * Test of {@code isValid()}.
     */
    @Test
    public void testIsValid() {
        assertTrue(createPoint2D().isValid());
    }

    /**
     * Test of {@code isEmpty()}.
     */
    @Test
    public void testIsEmpty() {
        assertFalse(createPoint2D().isEmpty(), "A point always has a location.");
    }

    /**
     * Test of {@code boundary()}.
     */
    @Test
    public void testBoundary() {
        final Point point = createPoint2D();
        final Geometry boundary = point.boundary();
        assertNotNull(boundary);
        assertTrue(boundary.isEmpty(), "The boundary of a point is the empty set.");
        assertEquals(GeometryType.EMPTY, boundary.getGeometryType());
        assertEquals(CRS_2D, boundary.getCoordinateReferenceSystem());
    }

    /**
     * Test of {@code getClosure()}.
     */
    @Test
    public void testGetClosure() {
        final Point point = createPoint2D();
        assertSame(point, point.getClosure(), "A point contains its empty boundary, therefore it is its own closure.");
    }

    /**
     * Test of {@code getCentroid()}.
     */
    @Test
    public void testGetCentroid() {
        final Point point = createPoint2D();
        assertSame(point, point.getCentroid(), "A point is its own centroid.");
    }

    /**
     * Test of {@code getRepresentativePoint()}.
     */
    @Test
    public void testGetRepresentativePoint() {
        final Point point = createPoint2D();
        assertSame(point, point.getRepresentativePoint(), "A point is interior to itself.");
    }

    /**
     * Test of {@code getMaximalComplex()}.
     */
    @Test
    @Disabled
    public void testGetMaximalComplex() {
        // todo
    }

    /**
     * Test of {@code getMetadata()}.
     */
    @Test
    @Disabled
    public void testGetMetadata() {
        // todo
    }

    /**
     * Test of {@code getCoordinateReferenceSystem()}.
     */
    @Test
    public void testGetCoordinateReferenceSystem() {
        assertEquals(CRS_2D, createPoint2D().getCoordinateReferenceSystem());
        assertEquals(CRS_3D, createPoint3D().getCoordinateReferenceSystem());
    }

    /**
     * Test of {@code setCoordinateReferenceSystem(CoordinateReferenceSystem)}.
     */
    @Test
    public void testSetCoordinateReferenceSystem() {
        final Point point = createPoint2D();
        final CoordinateReferenceSystem other = CommonCRS.WGS84.geographic();
        try {
            point.setCoordinateReferenceSystem(other);
        } catch (UnsupportedOperationException e) {
            // Immutable implementation. The geometry shall then be left unchanged.
            assertEquals(CRS_2D, point.getCoordinateReferenceSystem());
            return;
        }
        assertEquals(other, point.getCoordinateReferenceSystem());
        assertArrayEquals(POSITION_2D, point.getPosition().toArrayDouble(),
                          "Changing the reference system shall not move the coordinates.");
    }

    /**
     * Test of {@code getAttributesType()}.
     */
    @Test
    public void testGetAttributesType() {
        final Point point = createPoint2D();
        final DataPointsType type = point.getDataPointsType();
        assertNotNull(type);
        assertTrue(type.getAttributeNames().contains(DataPointsType.ATT_POSITION),
                   "The coordinates are always carried by the positions attribute.");
        /*
         * The positions of a geometry use the coordinate reference system of that geometry,
         * as stated in the constraints of the `Geometry` interface.
         */
        assertEquals(CRS_2D, type.getAttributeSystem(DataPointsType.ATT_POSITION).getCoordinateReferenceSystem());
    }

    /**
     * Test of {@code getDimension()}.
     */
    @Test
    public void testGetDimension() {
        assertEquals(2, createPoint2D().getDimension());
        assertEquals(3, createPoint3D().getDimension());
    }

    /**
     * Test of {@code is3D()}.
     */
    @Test
    public void testIs3D() {
        assertFalse(createPoint2D().is3D());
        assertTrue (createPoint3D().is3D());
    }

    /**
     * Test of {@code getSpatialDimension()}.
     */
    @Test
    public void testGetSpatialDimension() {
        assertEquals(2, createPoint2D().getSpatialDimension());
        assertEquals(3, createPoint3D().getSpatialDimension());
    }

    /**
     * Test of {@code getBoundaryType()}.
     */
    @Test
    @Disabled
    public void testGetBoundaryType() {
        final Point point = createPoint2D();
        /*
         * TODO
         */
        assertThrows(UnsupportedOperationException.class, () -> point.getBoundaryType());
    }

    /**
     * Test of {@code getEnvelope()}.
     */
    @Test
    public void testGetEnvelope() {
        final Point point = createPoint2D();
        final Envelope envelope = point.getEnvelope();
        assertNotNull(envelope);
        assertEquals(CRS_2D, envelope.getCoordinateReferenceSystem());
        assertEquals(2, envelope.getDimension());

        for (int i = 0; i < POSITION_2D.length; i++) {
            assertEquals(POSITION_2D[i], envelope.getMinimum(i));
            assertEquals(POSITION_2D[i], envelope.getMaximum(i));
        }
    }

    /**
     * Test of {@code vectorToPoint(DirectPosition)}.
     */
    @Test
    @Disabled
    public void testVectorToPoint() {
        // TODO
    }

    /**
     * Test of {@code bearing(DirectPosition)}.
     */
    @Test
    @Disabled
    public void testBearing() {
        // TODO
    }

    /**
     * Test of {@code pointAtDistance(Vector)}.
     */
    @Test
    @Disabled
    public void testPointAtDistance() {
        // TODO
    }

    /**
     * Test of {@code userProperties()}.
     */
    @Test
    public void testUserProperties() {
        final Point point = createPoint2D();
        final Map<String,Object> properties = point.userProperties();
        if (properties == null) {
            // Allowed by the contract: the geometry cannot store additional information.
            return;
        }
        assertTrue(properties.isEmpty(), "A new geometry shall have no user property.");
        properties.put("A key", "A value");
        assertEquals("A value", point.userProperties().get("A key"));
    }
}
