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

import java.util.Map;
import org.apache.sis.geometry.AbstractEnvelope;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.referencing.CommonCRS;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;


/**
 * Tests {@link Empty}.
 *
 * <p>This class tests the behavior mandated by {@link Empty} on any implementation.
 * Subclasses provide the implementation to test by implementing
 * {@link #createEmpty(CoordinateReferenceSystem)}.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class EmptyTest {
    /**
     * An arbitrary two-dimensional coordinate reference system used by the tests.
     */
    protected static final CoordinateReferenceSystem CRS_2D = CommonCRS.WGS84.normalizedGeographic();

    /**
     * An arbitrary three-dimensional coordinate reference system used by the tests.
     */
    protected static final CoordinateReferenceSystem CRS_3D = CommonCRS.WGS84.geographic3D();

    /**
     * Creates a new test case.
     */
    protected EmptyTest() {
    }

    /**
     * Creates an empty geometry using the given coordinate reference system.
     * The returned geometry shall report the given system as its
     * {@linkplain Geometry#getCoordinateReferenceSystem() coordinate reference system}.
     *
     * @param  crs  the coordinate reference system of the geometry to create, not null.
     * @return a new empty geometry, never null.
     */
    protected abstract Empty createEmpty(CoordinateReferenceSystem crs);

    /**
     * Test of {@code isEmpty()}.
     */
    @Test
    public void testIsEmpty() {
        assertTrue(createEmpty(CRS_2D).isEmpty());
    }

    /**
     * Test of {@code getGeometryType()}.
     */
    @Test
    public void testGetGeometryType() {
        assertEquals(GeometryType.EMPTY, createEmpty(CRS_2D).getGeometryType());
    }

    /**
     * Test of {@code getTopologicDimension()}.
     */
    @Test
    public void testGetTopologicDimension() {
        assertEquals(-1, createEmpty(CRS_2D).getTopologicDimension());
        assertEquals(-1, createEmpty(CRS_3D).getTopologicDimension());
    }

    /**
     * Test of {@code isCycle()}.
     */
    @Test
    public void testIsCycle() {
        assertTrue(createEmpty(CRS_2D).isCycle(), "The boundary of the empty set is empty.");
    }

    /**
     * Test of {@code isSimple()}.
     */
    @Test
    public void testIsSimple() {
        assertTrue(createEmpty(CRS_2D).isSimple(), "The empty set has no anomalous position.");
    }

    /**
     * Test of {@code isValid()}.
     */
    @Test
    public void testIsValid() {
        assertTrue(createEmpty(CRS_2D).isValid());
    }

    /**
     * Test of {@code boundary()}.
     */
    @Test
    public void testBoundary() {
        final Empty empty = createEmpty(CRS_2D);
        final Geometry boundary = empty.boundary();
        assertSame(empty, boundary);
        assertTrue(boundary.isEmpty());
    }

    /**
     * Test of {@code getClosure()}.
     */
    @Test
    public void testGetClosure() {
        final Empty empty = createEmpty(CRS_2D);
        final Geometry closure = empty.getClosure();
        assertSame(empty, closure);
        assertTrue(closure.isEmpty());
    }

    /**
     * Test of {@code convexHull()}.
     */
    @Test
    public void testConvexHull() {
        final Empty empty = createEmpty(CRS_2D);
        final Geometry hull = empty.convexHull();
        assertSame(empty, hull);
        assertTrue(hull.isEmpty());
    }

    /**
     * Test of {@code getCentroid()}.
     */
    @Test
    public void testGetCentroid() {
        assertNull(createEmpty(CRS_2D).getCentroid(), "The centroid of the empty set is undefined.");
    }

    /**
     * Test of {@code getRepresentativePoint()}.
     */
    @Test
    public void testGetRepresentativePoint() {
        assertNull(createEmpty(CRS_2D).getRepresentativePoint(), "The empty set has no interior position.");
    }

    /**
     * Test of {@code getCoordinateReferenceSystem()}.
     */
    @Test
    public void testGetCoordinateReferenceSystem() {
        assertEquals(CRS_2D, createEmpty(CRS_2D).getCoordinateReferenceSystem());
        assertEquals(CRS_3D, createEmpty(CRS_3D).getCoordinateReferenceSystem());
    }

    /**
     * Test of {@code setCoordinateReferenceSystem(CoordinateReferenceSystem)}.
     */
    @Test
    public void testSetCoordinateReferenceSystem() {
        final Empty empty = createEmpty(CRS_2D);
        try {
            empty.setCoordinateReferenceSystem(CRS_3D);
        } catch (UnsupportedOperationException e) {
            // Immutable implementation. The geometry shall then be left unchanged.
            assertEquals(CRS_2D, empty.getCoordinateReferenceSystem());
            return;
        }
        assertEquals(CRS_3D, empty.getCoordinateReferenceSystem());
    }

    /**
     * Test of {@code getAttributesType()}.
     */
    @Test
    public void testGetAttributesType() {
        final Empty empty = createEmpty(CRS_2D);
        final DataPointsType type = empty.getDataPointsType();
        assertNotNull(type);
        assertNotNull(type.getAttributeNames());
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
        assertEquals(2, createEmpty(CRS_2D).getDimension());
        assertEquals(3, createEmpty(CRS_3D).getDimension());
    }

    /**
     * Test of {@code getDimension(DirectPosition)}.
     */
    @Test
    public void testGetDimension_DirectPosition() {
        final Empty empty = createEmpty(CRS_2D);
        assertEquals(-1, empty.getDimension(null), "Dimension of the whole geometry.");
        /*
         * TODO: the empty set has no interior position, therefore no position where the dimension
         * is unambiguous. Replace this check if a value is defined for that case.
         */
        final DirectPosition position = new GeneralDirectPosition(CRS_2D);
        assertThrows(UnsupportedOperationException.class, () -> empty.getDimension(position));
    }

    /**
     * Test of {@code is3D()}.
     */
    @Test
    public void testIs3D() {
        assertFalse(createEmpty(CRS_2D).is3D());
        assertTrue (createEmpty(CRS_3D).is3D());
    }

    /**
     * Test of {@code getSpatialDimension()}.
     */
    @Test
    public void testGetSpatialDimension() {
        assertEquals(2, createEmpty(CRS_2D).getSpatialDimension());
        assertEquals(3, createEmpty(CRS_3D).getSpatialDimension());
    }

    /**
     * Test of {@code getBoundaryType()}.
     */
    @Test
    @Disabled
    public void testGetBoundaryType() {
        final Empty empty = createEmpty(CRS_2D);
        /*
         * TODO: `Geometry.getBoundaryType()` is not implemented yet. The empty geometry has no
         * element, therefore no ambiguous position: every rule gives the same empty boundary.
         */
        assertThrows(UnsupportedOperationException.class, () -> empty.getBoundaryType());
    }

    /**
     * Test of {@code getEnvelope()}.
     */
    @Test
    public void testGetEnvelope() {
        final Empty empty = createEmpty(CRS_2D);
        final Envelope envelope = empty.getEnvelope();
        assertNotNull(envelope);
        assertEquals(CRS_2D, envelope.getCoordinateReferenceSystem());
        assertEquals(2, envelope.getDimension());
        assertTrue(AbstractEnvelope.castOrCopy(envelope).isEmpty(),
                   "The envelope of an empty geometry contains no position.");
    }

    /**
     * Test of {@code userProperties()}.
     */
    @Test
    public void testUserProperties() {
        final Empty empty = createEmpty(CRS_2D);
        final Map<String,Object> properties = empty.userProperties();
        if (properties == null) {
            // Allowed by the contract: the geometry cannot store additional information.
            return;
        }
        assertTrue(properties.isEmpty(), "A new geometry shall have no user property.");
        properties.put("A key", "A value");
        assertEquals("A value", empty.userProperties().get("A key"));
    }
}
