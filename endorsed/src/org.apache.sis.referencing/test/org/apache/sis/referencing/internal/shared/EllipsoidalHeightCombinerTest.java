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
package org.apache.sis.referencing.internal.shared;

import java.util.Map;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.util.FactoryException;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.factory.GeodeticObjectFactory;
import org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory;
import org.apache.sis.metadata.iso.extent.Extents;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.referencing.operation.HardCodedConversions;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.cs.HardCodedCS;
import org.apache.sis.referencing.crs.HardCodedCRS;
import static org.apache.sis.test.Assertions.assertEqualsIgnoreMetadata;
import static org.apache.sis.test.Assertions.assertArrayEqualsIgnoreMetadata;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import static org.opengis.test.Assertions.assertAxisDirectionsEqual;


/**
 * Tests {@link EllipsoidalHeightCombiner}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see <a href="https://issues.apache.org/jira/browse/SIS-303">SIS-303</a>
 */
public final class EllipsoidalHeightCombinerTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public EllipsoidalHeightCombinerTest() {
    }

    /**
     * Creates an instance to be tested.
     */
    private static EllipsoidalHeightCombiner create() {
        final GeodeticObjectFactory factory = GeodeticObjectFactory.provider();
        return new EllipsoidalHeightCombiner(factory, factory, new DefaultCoordinateOperationFactory());
    }

    /**
     * Tests {@link EllipsoidalHeightCombiner#createCompoundCRS EllipsoidalHeightCombiner.createCompoundCRS(…)}
     * with a geographic CRS.
     *
     * @throws FactoryException if a CRS cannot be created.
     */
    @Test
    public void testGeographicCRS() throws FactoryException {
        final EllipsoidalHeightCombiner services = create();
        final var properties = Map.of(CoordinateReferenceSystem.NAME_KEY, "WGS 84 (4D)");
        final GeographicCRS horizontal = HardCodedCRS.WGS84;
        final GeographicCRS volumetric = HardCodedCRS.WGS84_3D;
        final VerticalCRS   vertical   = HardCodedCRS.ELLIPSOIDAL_HEIGHT;
        final TemporalCRS   temporal   = HardCodedCRS.TIME;
        final VerticalCRS   geoidal    = HardCodedCRS.GRAVITY_RELATED_HEIGHT;
        /*
         * createCompoundCRS(…) should not combine GeographicCRS with non-ellipsoidal height.
         */
        CoordinateReferenceSystem compound = services.createCompoundCRS(properties, horizontal, geoidal, temporal);
        assertArrayEqualsIgnoreMetadata(new SingleCRS[] {horizontal, geoidal, temporal}, CRS.getSingleComponents(compound).toArray());
        /*
         * createCompoundCRS(…) should combine GeographicCRS with ellipsoidal height.
         */
        compound = services.createCompoundCRS(properties, horizontal, vertical);
        assertArrayEqualsIgnoreMetadata(new SingleCRS[] {volumetric}, CRS.getSingleComponents(compound).toArray());
        /*
         * createCompoundCRS(…) should combine GeographicCRS with ellipsoidal height and keep time.
         */
        compound = services.createCompoundCRS(properties, horizontal, vertical, temporal);
        assertArrayEqualsIgnoreMetadata(new SingleCRS[] {volumetric, temporal}, CRS.getSingleComponents(compound).toArray());
        /*
         * Non-standard feature: accept (VerticalCRS + GeodeticCRS) order.
         * The test below use the reverse order for all axes compared to the previous test.
         */
        compound = services.createCompoundCRS(properties, temporal, vertical, HardCodedCRS.WGS84_LATITUDE_FIRST);
        final Object[] components = CRS.getSingleComponents(compound).toArray();
        assertEquals(2, components.length);
        assertEqualsIgnoreMetadata(temporal, components[0]);

        final String message = "Shall be a three-dimensional geographic CRS.";
        var c = assertInstanceOf(GeographicCRS.class, components[1], message);
        assertAxisDirectionsEqual(c.getCoordinateSystem(), new AxisDirection[] {
                    AxisDirection.UP, AxisDirection.NORTH, AxisDirection.EAST
                }, message);
    }

    /**
     * Tests {@link EllipsoidalHeightCombiner#createCompoundCRS EllipsoidalHeightCombiner.createCompoundCRS(…)}
     * with a projected CRS.
     *
     * @throws FactoryException if a CRS cannot be created.
     */
    @Test
    public void testProjectedCRS() throws FactoryException {
        final EllipsoidalHeightCombiner services = create();
        final var factory = GeodeticObjectFactory.provider();
        final var properties = Map.of(CoordinateReferenceSystem.NAME_KEY, "World Mercator (4D)");
        final ProjectedCRS horizontal = factory.createProjectedCRS(properties, HardCodedCRS.WGS84,    HardCodedConversions.MERCATOR, HardCodedCS.PROJECTED);
        final ProjectedCRS volumetric = factory.createProjectedCRS(properties, HardCodedCRS.WGS84_3D, HardCodedConversions.MERCATOR, HardCodedCS.PROJECTED_3D);
        final VerticalCRS  vertical   = HardCodedCRS.ELLIPSOIDAL_HEIGHT;
        final TemporalCRS  temporal   = HardCodedCRS.TIME;
        final VerticalCRS  geoidal    = HardCodedCRS.GRAVITY_RELATED_HEIGHT;
        /*
         * createCompoundCRS(…) should not combine ProjectedCRS with non-ellipsoidal height.
         */
        CoordinateReferenceSystem compound = services.createCompoundCRS(properties, horizontal, geoidal, temporal);
        assertArrayEqualsIgnoreMetadata(new SingleCRS[] {horizontal, geoidal, temporal}, CRS.getSingleComponents(compound).toArray());
        /*
         * createCompoundCRS(…) should combine ProjectedCRS with ellipsoidal height.
         */
        compound = services.createCompoundCRS(properties, horizontal, vertical);
        assertArrayEqualsIgnoreMetadata(new SingleCRS[] {volumetric}, CRS.getSingleComponents(compound).toArray());
        /*
         * createCompoundCRS(…) should combine ProjectedCRS with ellipsoidal height and keep time.
         */
        compound = services.createCompoundCRS(properties, horizontal, vertical, temporal);
        assertArrayEqualsIgnoreMetadata(new SingleCRS[] {volumetric, temporal}, CRS.getSingleComponents(compound).toArray());
        /*
         * Non-standard feature: accept (VerticalCRS + ProjectedCRS) order.
         */
        compound = services.createCompoundCRS(properties, temporal, vertical, horizontal);
        final Object[] components = CRS.getSingleComponents(compound).toArray();
        assertEquals(2, components.length);
        assertEqualsIgnoreMetadata(temporal, components[0]);

        final String message = "Shall be a three-dimensional projected CRS.";
        var c = assertInstanceOf(ProjectedCRS.class, components[1], message);
        assertAxisDirectionsEqual(c.getCoordinateSystem(), new AxisDirection[] {
                    AxisDirection.UP, AxisDirection.EAST, AxisDirection.NORTH
                }, message);
    }

    /**
     * Tests {@link EllipsoidalHeightCombiner#properties(CoordinateReferenceSystem...)}.
     */
    @Test
    public void testProperties() {
        final Map<String,?> properties = EllipsoidalHeightCombiner.properties(HardCodedCRS.WGS84, HardCodedCRS.GRAVITY_RELATED_HEIGHT, HardCodedCRS.TIME);
        assertEquals("WGS 84 + MSL height + Time", properties.remove("name"));
        assertEquals(Extents.WORLD, properties.remove("domainOfValidity"));
        assertTrue(properties.isEmpty());
    }
}
