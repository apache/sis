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
package org.apache.sis.metadata.iso.extent;

import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.IncommensurableException;
import org.opengis.geometry.DirectPosition;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.VerticalExtent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.measure.Units;
import org.apache.sis.measure.MeasurementRange;
import static org.apache.sis.metadata.privy.ReferencingServices.NAUTICAL_MILE;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.mock.VerticalCRSMock;

// Specific to the main branch:
import org.opengis.referencing.datum.VerticalDatumType;


/**
 * Tests {@link Extents} static methods.
 *
 * <p><b>Note:</b> the {@link Extents#WORLD} constant is tested in another class,
 * {@link DefaultExtentTest#testWorldConstant()}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ExtentsTest extends TestCase {
    /**
     * One minute of angle, in degrees.
     */
    private static final double MINUTE = 1./60;

    /**
     * Creates a new test case.
     */
    public ExtentsTest() {
    }

    /**
     * Tests the {@link Extents#isWorld(Extent)} method.
     */
    @Test
    public void testIsWorld() {
        assertTrue (Extents.isWorld(Extents.WORLD));
        assertFalse(Extents.isWorld(new DefaultExtent(null, new DefaultGeographicBoundingBox(10, 20, 30, 40), null, null)));
    }

    /**
     * Tests {@link Extents#getVerticalRange(Extent)}.
     *
     * @throws IncommensurableException if a conversion between incompatible units were attempted.
     */
    @Test
    public void testGetVerticalRange() throws IncommensurableException {
        final List<DefaultVerticalExtent> extents = Arrays.asList(
                new DefaultVerticalExtent( -200,  -100, VerticalCRSMock.HEIGHT),
                new DefaultVerticalExtent(  150,   300, VerticalCRSMock.DEPTH),
                new DefaultVerticalExtent(  0.1,   0.2, VerticalCRSMock.SIGMA_LEVEL),
                new DefaultVerticalExtent( -600,  -300, VerticalCRSMock.HEIGHT_ft),         // [91.44 … 182.88] metres
                new DefaultVerticalExtent(10130, 20260, VerticalCRSMock.BAROMETRIC_HEIGHT)
        );
        Collections.shuffle(extents, TestUtilities.createRandomNumberGenerator());
        /*
         * Since we have shuffled the vertical extents in random order, the range that we will
         * test may be either in metres or in feet depending on which vertical extent is first.
         * So we need to check which linear unit is first.
         */
        Unit<?> unit = null;
        for (final DefaultVerticalExtent e : extents) {
            unit = e.getVerticalCRS().getCoordinateSystem().getAxis(0).getUnit();
            if (e.getVerticalCRS().getDatum().getVerticalDatumType() == VerticalDatumType.GEOIDAL) break;
        }
        final UnitConverter c = unit.getConverterToAny(Units.METRE);
        /*
         * The actual test. Arbitrarily compare the heights in metres, converting them if needed.
         */
        final var extent = new DefaultExtent();
        extent.setVerticalElements(extents);
        final MeasurementRange<Double> range = Extents.getVerticalRange(extent);
        assertNotNull(range);
        assertSame   (unit,   range.unit());
        assertEquals (-200,   c.convert(range.getMinDouble()), 0.001);
        assertEquals (-91.44, c.convert(range.getMaxDouble()), 0.001);
    }

    /**
     * Tests {@link Extents#intersection(GeographicBoundingBox, GeographicBoundingBox)}.
     */
    @Test
    public void testGeographicIntersection() {
        final var b1 = new DefaultGeographicBoundingBox(10, 20, 30, 40);
        final var b2 = new DefaultGeographicBoundingBox(15, 25, 26, 32);
        assertEquals(  new DefaultGeographicBoundingBox(15, 20, 30, 32), Extents.intersection(b1, b2));
        assertSame(b1, Extents.intersection(b1, null));
        assertSame(b2, Extents.intersection(null, b2));
        assertNull(    Extents.intersection((GeographicBoundingBox) null, (GeographicBoundingBox) null));
    }

    /**
     * Tests {@link Extents#intersection(VerticalExtent, VerticalExtent)}.
     * This test does not perform any unit conversion, because it would require the use of different CRS.
     * For a test with unit conversion, see {@code ServicesForMetadataTest.testVerticalIntersection()} in
     * {@code org.apache.sis.referencing} module.
     *
     * @throws TransformException should never happen since we do not test transformation in this class.
     */
    @Test
    public void testVerticalIntersection() throws TransformException {
        final var e1 = new DefaultVerticalExtent(10, 20, null);
        final var e2 = new DefaultVerticalExtent(15, 25, null);
        assertEquals(  new DefaultVerticalExtent(15, 20, null), Extents.intersection(e1, e2));
        assertSame(e1, Extents.intersection(e1, null));
        assertSame(e2, Extents.intersection(null, e2));
        assertNull(    Extents.intersection((VerticalExtent) null, (VerticalExtent) null));
    }

    /**
     * Tests {@link Extents#intersection(Extent, Extent)}.
     * This test is subject to the same limitation as {@link #testVerticalIntersection()}.
     */
    @Test
    public void testExtentIntersection() {
        final var e1 = new DefaultExtent(null, new DefaultGeographicBoundingBox(10, 20, 30, 40), new DefaultVerticalExtent(10, 20, null), null);
        final var e2 = new DefaultExtent(null, new DefaultGeographicBoundingBox(15, 25, 26, 32), new DefaultVerticalExtent(15, 25, null), null);
        assertEquals(  new DefaultExtent(null, new DefaultGeographicBoundingBox(15, 20, 30, 32), new DefaultVerticalExtent(15, 20, null), null), Extents.intersection(e1, e2));
        assertSame(e1, Extents.intersection(e1, null));
        assertSame(e2, Extents.intersection(null, e2));
        assertNull(    Extents.intersection((Extent) null, (Extent) null));
    }

    /**
     * Tests {@link Extents#area(GeographicBoundingBox)}.
     */
    @Test
    public void testArea() {
        /*
         * The nautical mile is equal to the length of 1 second of arc along a meridian or parallel at the equator.
         * Since we are using the GRS80 authalic sphere instead of WGS84, and since the nautical mile definition
         * itself is a little bit approximated, we add a slight empirical shift.
         */
        final var box = new DefaultGeographicBoundingBox(10, 10+MINUTE, 2.9685, 2.9685+MINUTE);
        assertEquals(NAUTICAL_MILE * NAUTICAL_MILE, Extents.area(box), 0.1);
        /*
         * Result should be much smaller near the poles.
         */
        box.setNorthBoundLatitude(90);
        box.setSouthBoundLatitude(90-MINUTE);
        assertEquals(499.5, Extents.area(box), 0.1);
        box.setSouthBoundLatitude(-90);
        box.setNorthBoundLatitude(-90+MINUTE);
        assertEquals(499.5, Extents.area(box), 0.1);
        /*
         * Spanning 360° of longitude.
         */
        box.setBounds(-180, +180, -90, 90);
        assertEquals(5.1E+14, Extents.area(box), 1E+11);
        /*
         * EPSG:1241    USA - CONUS including EEZ
         * This is only an anti-regression test - the value has not been validated.
         * However, the expected area MUST be greater than the Alaska's one below,
         * otherwise SIS will select the wrong datum shift operation over USA!!
         */
        box.setBounds(-129.16, -65.70, 23.82, 49.38);
        assertFalse(DefaultGeographicBoundingBoxTest.isCrossingAntiMeridian(box));
        assertEquals(15967665, Extents.area(box) / 1E6, 1);                             // Compare in km²
        /*
         * EPSG:2373    USA - Alaska including EEZ    (crossing the anti-meridian).
         * This is only an anti-regression test - the value has not been validated.
         * However, the expected area MUST be smaller than the CONUS's one above.
         */
        box.setBounds(167.65, -129.99, 47.88, 74.71);
        assertTrue(DefaultGeographicBoundingBoxTest.isCrossingAntiMeridian(box));
        assertEquals(9845438, Extents.area(box) / 1E6, 1);                              // Compare in km²
    }

    /**
     * Tests the {@link Extents#centroid(GeographicBoundingBox)} method. This method is defined here
     * but executed from the {@link org.apache.sis.referencing.internal.ServicesForMetadataTest} class
     * in {@code org.apache.sis.referencing} module.
     * This method cannot be executed in the {@code org.apache.sis.metadata} module
     * because it has a dependency to a referencing implementation class.
     */
    public static void testCentroid() {
        final var bbox = new DefaultGeographicBoundingBox(140, 160, 30, 50);
        DirectPosition pos = Extents.centroid(bbox);
        assertEquals(150, pos.getOrdinate(0), "longitude");
        assertEquals( 40, pos.getOrdinate(1), "latitude");
        /*
         * Test crossing anti-meridian.
         */
        bbox.setEastBoundLongitude(-160);
        pos = Extents.centroid(bbox);
        assertEquals(170, pos.getOrdinate(0), "longitude");
        assertEquals( 40, pos.getOrdinate(1), "latitude");
    }
}
