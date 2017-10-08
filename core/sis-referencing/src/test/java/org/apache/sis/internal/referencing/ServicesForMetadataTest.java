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
package org.apache.sis.internal.referencing;

import java.util.Map;
import java.util.Collections;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.VerticalExtent;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.internal.metadata.ReferencingServices;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.DefaultVerticalExtent;
import org.apache.sis.metadata.iso.extent.DefaultSpatialTemporalExtent;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.cs.HardCodedCS;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.factory.GeodeticObjectFactory;
import org.apache.sis.referencing.operation.HardCodedConversions;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests {@link ServicesForMetadata}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.5
 * @module
 */
@DependsOn({
    org.apache.sis.referencing.CRSTest.class,
    org.apache.sis.referencing.CommonCRSTest.class
})
public final strictfp class ServicesForMetadataTest extends TestCase {
    /**
     * Tests {@link org.apache.sis.metadata.iso.extent.Extents#centroid(GeographicBoundingBox)}.
     *
     * @since 0.8
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
    private static Envelope createEnvelope(final CoordinateReferenceSystem crs) {
        final GeneralEnvelope envelope = new GeneralEnvelope(crs);
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
        assertEquals(-40, box.getWestBoundLongitude(), STRICT);
        assertEquals( 60, box.getEastBoundLongitude(), STRICT);
        assertEquals(-20, box.getSouthBoundLatitude(), STRICT);
        assertEquals( 30, box.getNorthBoundLatitude(), STRICT);
        assertEquals(Boolean.TRUE, box.getInclusion());
    }

    /**
     * Verifies the values of the given vertical extent.
     */
    private static void verifyVerticalExtent(final CommonCRS.Vertical expectedCRS, final VerticalExtent extent) {
        assertEquals(-10, extent.getMinimumValue(), STRICT);
        assertEquals( 70, extent.getMaximumValue(), STRICT);
        assertEqualsIgnoreMetadata(expectedCRS.crs(), extent.getVerticalCRS());
    }

    /**
     * Tests (indirectly) {@link ServicesForMetadata#setBounds(Envelope, DefaultGeographicBoundingBox)}
     * from a three-dimensional envelope.
     *
     * @throws TransformException should never happen.
     */
    @Test
    public void testSetGeographicBoundsFrom3D() throws TransformException {
        final DefaultGeographicBoundingBox box = new DefaultGeographicBoundingBox();
        box.setBounds(createEnvelope(HardCodedCRS.WGS84_3D));
        verifySpatialExtent(box);
    }

    /**
     * Tests (indirectly) {@link ServicesForMetadata#setBounds(Envelope, DefaultGeographicBoundingBox)}
     * from a for-dimensional envelope.
     *
     * @throws TransformException should never happen.
     */
    @Test
    public void testSetGeographicBoundsFrom4D() throws TransformException {
        final DefaultGeographicBoundingBox box = new DefaultGeographicBoundingBox();
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
        final DefaultVerticalExtent extent = new DefaultVerticalExtent();
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
        final DefaultVerticalExtent extent = new DefaultVerticalExtent();
        extent.setBounds(createEnvelope(HardCodedCRS.GEOID_4D));
        verifyVerticalExtent(CommonCRS.Vertical.MEAN_SEA_LEVEL, extent);
    }

    /**
     * Tests (indirectly) {@link ServicesForMetadata#setBounds(Envelope, DefaultSpatialTemporalExtent)}.
     *
     * @throws TransformException should never happen.
     */
    @Test
    @DependsOnMethod({"testSetGeographicBoundsFrom4D", "testSetVerticalBoundsFromGeoid"})
    public void testSetSpatialTemporalBounds() throws TransformException {
        final DefaultSpatialTemporalExtent extent = new DefaultSpatialTemporalExtent();
        extent.setBounds(createEnvelope(HardCodedCRS.GEOID_3D));
        verifySpatialExtent((GeographicBoundingBox) getSingleton(extent.getSpatialExtent()));
        verifyVerticalExtent(CommonCRS.Vertical.MEAN_SEA_LEVEL, extent.getVerticalExtent());
    }

    /**
     * Tests {@link ServicesForMetadata#createCompoundCRS ReferencingUtilities.createCompoundCRS(…)}
     * with a geographic CRS.
     *
     * @throws FactoryException if a CRS can not be created.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-303">SIS-303</a>
     *
     * @since 0.7
     */
    @Test
    public void testCreateCompoundGeographicCRS() throws FactoryException {
        final ReferencingServices  services = ServicesForMetadata.getInstance();
        final GeodeticObjectFactory factory = new GeodeticObjectFactory();
        final Map<String,String> properties = Collections.singletonMap(CoordinateReferenceSystem.NAME_KEY, "WGS 84 (4D)");
        final GeographicCRS horizontal   = HardCodedCRS.WGS84;
        final GeographicCRS horizontal3D = HardCodedCRS.WGS84_3D;
        final VerticalCRS   vertical     = HardCodedCRS.ELLIPSOIDAL_HEIGHT;
        final TemporalCRS   temporal     = HardCodedCRS.TIME;
        final VerticalCRS   geoidal      = HardCodedCRS.GRAVITY_RELATED_HEIGHT;
        /*
         * createCompoundCRS(…) should not combine GeographicCRS with non-ellipsoidal height.
         */
        CoordinateReferenceSystem compound = services.createCompoundCRS(factory, factory, properties, horizontal, geoidal, temporal);
        assertArrayEqualsIgnoreMetadata(new SingleCRS[] {horizontal, geoidal, temporal}, CRS.getSingleComponents(compound).toArray());
        /*
         * createCompoundCRS(…) should combine GeographicCRS with ellipsoidal height.
         */
        compound = services.createCompoundCRS(factory, factory, properties, horizontal, vertical);
        assertArrayEqualsIgnoreMetadata(new SingleCRS[] {horizontal3D}, CRS.getSingleComponents(compound).toArray());
        /*
         * createCompoundCRS(…) should combine GeographicCRS with ellipsoidal height and keep time.
         */
        compound = services.createCompoundCRS(factory, factory, properties, horizontal, vertical, temporal);
        assertArrayEqualsIgnoreMetadata(new SingleCRS[] {horizontal3D, temporal}, CRS.getSingleComponents(compound).toArray());
        /*
         * Non-standard feature: accept (VerticalCRS + GeodeticCRS) order.
         * The test below use the reverse order for all axes compared to the previous test.
         */
        compound = services.createCompoundCRS(factory, factory, properties, temporal, vertical, HardCodedCRS.WGS84_φλ);
        final Object[] components = CRS.getSingleComponents(compound).toArray();
        assertEquals(2, components.length);
        assertEqualsIgnoreMetadata(temporal, components[0]);
        assertInstanceOf("Shall be a three-dimensional geographic CRS.", GeographicCRS.class, components[1]);
        assertAxisDirectionsEqual("Shall be a three-dimensional geographic CRS.",
                ((CoordinateReferenceSystem) components[1]).getCoordinateSystem(),
                AxisDirection.UP, AxisDirection.NORTH, AxisDirection.EAST);
    }

    /**
     * Tests {@link ServicesForMetadata#createCompoundCRS ReferencingUtilities.createCompoundCRS(…)}
     * with a projected CRS.
     *
     * @throws FactoryException if a CRS can not be created.
     *
     * @since 0.8
     */
    @Test
    @DependsOnMethod("testCreateCompoundGeographicCRS")
    public void testCreateCompoundProjectedCRS() throws FactoryException {
        final ReferencingServices  services = ServicesForMetadata.getInstance();
        final GeodeticObjectFactory factory = new GeodeticObjectFactory();
        final Map<String,String> properties = Collections.singletonMap(CoordinateReferenceSystem.NAME_KEY, "World Mercator (4D)");
        final ProjectedCRS horizontal   = factory.createProjectedCRS(properties, HardCodedCRS.WGS84,    HardCodedConversions.MERCATOR, HardCodedCS.PROJECTED);
        final ProjectedCRS horizontal3D = factory.createProjectedCRS(properties, HardCodedCRS.WGS84_3D, HardCodedConversions.MERCATOR, HardCodedCS.PROJECTED_3D);
        final VerticalCRS  vertical     = HardCodedCRS.ELLIPSOIDAL_HEIGHT;
        final TemporalCRS  temporal     = HardCodedCRS.TIME;
        final VerticalCRS  geoidal      = HardCodedCRS.GRAVITY_RELATED_HEIGHT;
        /*
         * createCompoundCRS(…) should not combine ProjectedCRS with non-ellipsoidal height.
         */
        CoordinateReferenceSystem compound = services.createCompoundCRS(factory, factory, properties, horizontal, geoidal, temporal);
        assertArrayEqualsIgnoreMetadata(new SingleCRS[] {horizontal, geoidal, temporal}, CRS.getSingleComponents(compound).toArray());
        /*
         * createCompoundCRS(…) should combine ProjectedCRS with ellipsoidal height.
         */
        if (true) return;       // TODO - debug after this point.
        compound = services.createCompoundCRS(factory, factory, properties, horizontal, vertical);
        assertArrayEqualsIgnoreMetadata(new SingleCRS[] {horizontal3D}, CRS.getSingleComponents(compound).toArray());
        /*
         * createCompoundCRS(…) should combine GeographicCRS with ellipsoidal height and keep time.
         */
        compound = services.createCompoundCRS(factory, factory, properties, horizontal, vertical, temporal);
        assertArrayEqualsIgnoreMetadata(new SingleCRS[] {horizontal3D, temporal}, CRS.getSingleComponents(compound).toArray());
        /*
         * Non-standard feature: accept (VerticalCRS + GeodeticCRS) order.
         * The test below use the reverse order for all axes compared to the previous test.
         */
        compound = services.createCompoundCRS(factory, factory, properties,
                temporal, vertical, HardCodedCRS.WGS84_φλ);
        final Object[] components = CRS.getSingleComponents(compound).toArray();
        assertEquals(2, components.length);
        assertEqualsIgnoreMetadata(temporal, components[0]);
        assertInstanceOf("Shall be a three-dimensional geographic CRS.", GeographicCRS.class, components[1]);
        assertAxisDirectionsEqual("Shall be a three-dimensional geographic CRS.",
                ((CoordinateReferenceSystem) components[1]).getCoordinateSystem(),
                AxisDirection.UP, AxisDirection.NORTH, AxisDirection.EAST);
    }
}
