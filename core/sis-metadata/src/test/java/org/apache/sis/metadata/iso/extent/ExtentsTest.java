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
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.converter.UnitConverter;
import javax.measure.converter.ConversionException;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.measure.Units;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.test.mock.VerticalCRSMock;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;
import org.junit.Test;

import static org.apache.sis.internal.metadata.ReferencingServices.NAUTICAL_MILE;
import static org.junit.Assert.*;


/**
 * Tests {@link Extents} static methods.
 *
 * <p><b>Note:</b> the {@link Extents#WORLD} constant is tested in another class,
 * {@link DefaultExtentTest#testWorldConstant()}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.6
 * @module
 */
@DependsOn(DefaultGeographicBoundingBoxTest.class)
public final strictfp class ExtentsTest extends TestCase {
    /**
     * One minute of angle, in degrees.
     */
    private static final double MINUTE = 1./60;

    /**
     * Tests {@link Extents#getVerticalRange(Extent)}.
     *
     * @throws ConversionException should never happen in this test.
     */
    @Test
    @SuppressWarnings("null")
    public void testGetVerticalRange() throws ConversionException {
        final List<DefaultVerticalExtent> extents = Arrays.asList(
                new DefaultVerticalExtent( -200,  -100, VerticalCRSMock.HEIGHT),
                new DefaultVerticalExtent(  150,   300, VerticalCRSMock.DEPTH),
                new DefaultVerticalExtent(  0.1,   0.2, VerticalCRSMock.SIGMA_LEVEL),
                new DefaultVerticalExtent( -600,  -300, VerticalCRSMock.HEIGHT_ft), // [91.44 … 182.88] metres
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
            if (Units.isLinear(unit)) break;
        }
        final UnitConverter c = unit.getConverterToAny(SI.METRE);
        /*
         * The actual test. Arbitrarily compare the heights in metres, converting them if needed.
         */
        final DefaultExtent extent = new DefaultExtent();
        extent.setVerticalElements(extents);
        final MeasurementRange<Double> range = Extents.getVerticalRange(extent);
        assertNotNull("getVerticalRange", range);
        assertSame   ("unit",    unit,    range.unit());
        assertEquals ("minimum", -300,    c.convert(range.getMinDouble()), 0.001);
        assertEquals ("maximum", -91.44,  c.convert(range.getMaxDouble()), 0.001);
    }

    /**
     * Tests {@link Extents#intersection(GeographicBoundingBox, GeographicBoundingBox)}.
     */
    @Test
    public void testIntersection() {
        final GeographicBoundingBox b1 = new DefaultGeographicBoundingBox(10, 20, 30, 40);
        final GeographicBoundingBox b2 = new DefaultGeographicBoundingBox(15, 25, 26, 32);
        assertEquals(new DefaultGeographicBoundingBox(15, 20, 30, 32), Extents.intersection(b1, b2));
        assertSame(b1, Extents.intersection(b1,   null));
        assertSame(b2, Extents.intersection(null, b2));
        assertNull(    Extents.intersection(null, null));
    }

    /**
     * Tests {@link Extents#area(GeographicBoundingBox)}.
     */
    @Test
    public void testArea() {
        /*
         * The nautical mile is equals to the length of 1 second of arc along a meridian or parallel at the equator.
         * Since we are using the GRS80 authalic sphere instead than WGS84, and since the nautical mile definition
         * itself is a little bit approximative, we add a slight empirical shift.
         */
        final DefaultGeographicBoundingBox box = new DefaultGeographicBoundingBox(10, 10+MINUTE, 2.9685, 2.9685+MINUTE);
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
         * EPSG:1241    USA - CONUS including EEZ
         * This is only an anti-regression test - the value has not been validated.
         * However the expected area MUST be greater than the Alaska's one below,
         * otherwise SIS will select the wrong datum shift operation over USA!!
         */
        box.setBounds(-129.16, -65.70, 23.82, 49.38);
        assertFalse(DefaultGeographicBoundingBoxTest.isSpanningAntiMeridian(box));
        assertEquals(15967665, Extents.area(box) / 1E6, 1); // Compare in km²
        /*
         * EPSG:2373    USA - Alaska including EEZ    (spanning the anti-meridian).
         * This is only an anti-regression test - the value has not been validated.
         * However the expected area MUST be smaller than the CONUS's one above.
         */
        box.setBounds(167.65, -129.99, 47.88, 74.71);
        assertTrue(DefaultGeographicBoundingBoxTest.isSpanningAntiMeridian(box));
        assertEquals(9845438, Extents.area(box) / 1E6, 1); // Compare in km²
    }
}
