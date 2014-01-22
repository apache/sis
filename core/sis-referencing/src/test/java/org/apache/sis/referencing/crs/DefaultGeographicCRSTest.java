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
package org.apache.sis.referencing.crs;

import org.opengis.test.Validators;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link DefaultGeographicCRS}
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4 (derived from geotk-3.20)
 * @version 0.4
 * @module
 */
@DependsOn({
    DefaultGeodeticCRSTest.class
})
public final strictfp class DefaultGeographicCRSTest extends TestCase {
    /**
     * Tolerance threshold for strict floating point comparisons.
     */
    private static final double STRICT = 0;

    /**
     * Tests the {@link DefaultGeographicCRS#forConvention(AxesConvention)} method
     * for {@link AxesConvention#POSITIVE_RANGE}.
     */
    @Test
    public void testShiftLongitudeRange() {
        final DefaultGeographicCRS crs = HardCodedCRS.WGS84_3D;
        CoordinateSystemAxis axis = crs.getCoordinateSystem().getAxis(0);
        assertEquals("longitude.minimumValue", -180.0, axis.getMinimumValue(), STRICT);
        assertEquals("longitude.maximumValue", +180.0, axis.getMaximumValue(), STRICT);

        assertSame("Expected a no-op.", crs,  crs.forConvention(AxesConvention.RIGHT_HANDED));
        final DefaultGeographicCRS shifted =  crs.forConvention(AxesConvention.POSITIVE_RANGE);
        assertNotSame("Expected a new CRS.", crs, shifted);
        Validators.validate(shifted);

        axis = shifted.getCoordinateSystem().getAxis(0);
        assertEquals("longitude.minimumValue",      0.0, axis.getMinimumValue(), STRICT);
        assertEquals("longitude.maximumValue",    360.0, axis.getMaximumValue(), STRICT);
        assertSame("Expected a no-op.",         shifted, shifted.forConvention(AxesConvention.POSITIVE_RANGE));
        assertSame("Expected cached instance.", shifted, crs    .forConvention(AxesConvention.POSITIVE_RANGE));
    }

    /**
     * Tests the {@link DefaultGeographicCRS#forConvention(AxesConvention)} method
     * for {@link AxesConvention#NORMALIZED}.
     */
    @Test
    public void testNormalize() {
        final DefaultGeographicCRS crs = DefaultGeographicCRS.castOrCopy(CommonCRS.WGS84.geographic3D());
        final DefaultGeographicCRS normalized = crs.forConvention(AxesConvention.NORMALIZED);
        assertNotSame(crs, normalized);
        final EllipsoidalCS cs = normalized.getCoordinateSystem();
        final EllipsoidalCS ref = crs.getCoordinateSystem();
        assertSame("longitude", ref.getAxis(1), cs.getAxis(0));
        assertSame("latitude",  ref.getAxis(0), cs.getAxis(1));
        assertSame("height",    ref.getAxis(2), cs.getAxis(2));
    }
}
