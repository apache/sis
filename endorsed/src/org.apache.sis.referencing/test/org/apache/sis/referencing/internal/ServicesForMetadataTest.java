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
package org.apache.sis.referencing.internal;

import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.VerticalExtent;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.DefaultVerticalExtent;
import org.apache.sis.metadata.iso.extent.DefaultSpatialTemporalExtent;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.crs.HardCodedCRS;
import static org.apache.sis.test.Assertions.assertEqualsIgnoreMetadata;
import static org.apache.sis.test.Assertions.assertSingleton;


/**
 * Tests {@link ServicesForMetadata}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ServicesForMetadataTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public ServicesForMetadataTest() {
    }

    /**
     * Tests {@link org.apache.sis.metadata.iso.extent.Extents#centroid(GeographicBoundingBox)}.
     */
    @Test
    public void testGeographicBoundingBoxCentroid() {
        org.apache.sis.metadata.iso.extent.ExtentsTest.testCentroid();
    }

    /**
     * Creates a test envelope with the given CRS and initialized with
     * [-10 … 70]° of longitude, [-20 … 30]° of latitude, [-40 … 60] metres of elevation
     * and [51000 … 52000] modified Julian days.
     */
    @SuppressWarnings("fallthrough")
    private static GeneralEnvelope createEnvelope(final CoordinateReferenceSystem crs) {
        final var envelope = new GeneralEnvelope(crs);
        switch (crs.getCoordinateSystem().getDimension()) {
            default: throw new AssertionError();
            case 4: envelope.setRange(3, 51000, 52000);                 // Fall through
            case 3: envelope.setRange(2, -10, 70);                      // Fall through
            case 2: envelope.setRange(1, -20, 30);                      // Fall through
            case 1: envelope.setRange(0, -40, 60);
            case 0: break;
        }
        return envelope;
    }

    /**
     * Verifies the values of the given geographic bounding box.
     */
    private static void verifySpatialExtent(final GeographicBoundingBox box) {
        assertEquals(-40, box.getWestBoundLongitude());
        assertEquals( 60, box.getEastBoundLongitude());
        assertEquals(-20, box.getSouthBoundLatitude());
        assertEquals( 30, box.getNorthBoundLatitude());
        assertEquals(Boolean.TRUE, box.getInclusion());
    }

    /**
     * Verifies the values of the given vertical extent.
     */
    private static void verifyVerticalExtent(final CommonCRS.Vertical expectedCRS, final VerticalExtent extent) {
        assertEquals(-10, extent.getMinimumValue());
        assertEquals( 70, extent.getMaximumValue());
        assertEqualsIgnoreMetadata(expectedCRS.crs(), extent.getVerticalCRS());
    }

    /**
     * Tests (indirectly) {@link ServicesForMetadata#setBounds(Envelope, DefaultGeographicBoundingBox, String)}
     * from a three-dimensional envelope.
     *
     * @throws TransformException should never happen.
     */
    @Test
    public void testSetGeographicBoundsFrom3D() throws TransformException {
        final var box = new DefaultGeographicBoundingBox();
        box.setBounds(createEnvelope(HardCodedCRS.WGS84_3D));
        verifySpatialExtent(box);
    }

    /**
     * Tests (indirectly) {@link ServicesForMetadata#setBounds(Envelope, DefaultGeographicBoundingBox, String)}
     * from a for-dimensional envelope.
     *
     * @throws TransformException should never happen.
     */
    @Test
    public void testSetGeographicBoundsFrom4D() throws TransformException {
        final var box = new DefaultGeographicBoundingBox();
        box.setBounds(createEnvelope(HardCodedCRS.GEOID_4D));
        verifySpatialExtent(box);
    }

    /**
     * Tests (indirectly) {@link ServicesForMetadata#setBounds(Envelope, DefaultVerticalExtent)}
     * from an ellipsoidal height
     *
     * @throws TransformException should never happen.
     */
    @Test
    public void testSetVerticalBoundsFromEllipsoid() throws TransformException {
        final var extent = new DefaultVerticalExtent();
        extent.setBounds(createEnvelope(HardCodedCRS.WGS84_3D));
        verifyVerticalExtent(CommonCRS.Vertical.ELLIPSOIDAL, extent);
    }

    /**
     * Tests (indirectly) {@link ServicesForMetadata#setBounds(Envelope, DefaultVerticalExtent)}
     * from a geoidal height
     *
     * @throws TransformException should never happen.
     */
    @Test
    public void testSetVerticalBoundsFromGeoid() throws TransformException {
        final var extent = new DefaultVerticalExtent();
        extent.setBounds(createEnvelope(HardCodedCRS.GEOID_4D));
        verifyVerticalExtent(CommonCRS.Vertical.MEAN_SEA_LEVEL, extent);
    }

    /**
     * Tests (indirectly) {@link ServicesForMetadata#setBounds(Envelope, DefaultSpatialTemporalExtent)}.
     *
     * @throws TransformException should never happen.
     */
    @Test
    public void testSetSpatialTemporalBounds() throws TransformException {
        final var extent = new DefaultSpatialTemporalExtent();
        extent.setBounds(createEnvelope(HardCodedCRS.GEOID_3D));
        verifySpatialExtent(assertInstanceOf(GeographicBoundingBox.class, assertSingleton(extent.getSpatialExtent())));
        verifyVerticalExtent(CommonCRS.Vertical.MEAN_SEA_LEVEL, extent.getVerticalExtent());
    }

    /**
     * Tests (indirectly) {@link ServicesForMetadata#setBounds(Envelope, DefaultGeographicBoundingBox, String)}
     * from an envelope crossing the antimeridian.
     *
     * @throws TransformException should never happen.
     */
    @Test
    public void testSetGeographicBoundsCrossingAntimeridian() throws TransformException {
        final var box = new DefaultGeographicBoundingBox();
        final GeneralEnvelope envelope = createEnvelope(HardCodedCRS.WGS84);
        envelope.setRange(0, 170, 195);
        box.setBounds(envelope);
        assertEquals( 170, box.getWestBoundLongitude());
        assertEquals(-165, box.getEastBoundLongitude());
        envelope.setRange(0, 0, 360);
        box.setBounds(envelope);
        assertEquals(-180, box.getWestBoundLongitude());
        assertEquals(+180, box.getEastBoundLongitude());
        assertEquals( -20, box.getSouthBoundLatitude());
        assertEquals(  30, box.getNorthBoundLatitude());
        assertEquals(Boolean.TRUE, box.getInclusion());
    }

    /**
     * Tests {@link DefaultVerticalExtent#intersect(VerticalExtent)}.
     *
     * @throws TransformException if the transformation failed.
     */
    @Test
    public void testVerticalIntersection() throws TransformException {
        final var e1 = new DefaultVerticalExtent(1000, 2000, HardCodedCRS.ELLIPSOIDAL_HEIGHT_cm);
        final var e2 = new DefaultVerticalExtent(15,   25,   HardCodedCRS.ELLIPSOIDAL_HEIGHT);
        e1.intersect(e2);
        assertEquals(new DefaultVerticalExtent(1500, 2000, HardCodedCRS.ELLIPSOIDAL_HEIGHT_cm), e1);
    }
}
