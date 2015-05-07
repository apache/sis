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

import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.apache.sis.internal.metadata.WKTKeywords;
import org.apache.sis.referencing.cs.HardCodedCS;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link DefaultDerivedCRS} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@DependsOn({
    DefaultProjectedCRSTest.class   // Has many similarities with DerivedCRS, but is simpler.
})
public final strictfp class DefaultDerivedCRSTest extends TestCase {
    /**
     * Tests {@link DefaultDerivedCRS#getType(SingleCRS, CoordinateSystem)}.
     */
    @Test
    public void testGetType() {
        assertEquals("Using consistent arguments.", WKTKeywords.VerticalCRS,
                DefaultDerivedCRS.getType(HardCodedCRS.ELLIPSOIDAL_HEIGHT, HardCodedCS.GRAVITY_RELATED_HEIGHT));

        assertNull("Using inconsistent arguments.",
                DefaultDerivedCRS.getType(HardCodedCRS.ELLIPSOIDAL_HEIGHT, HardCodedCS.SECONDS));

        assertEquals("Using consistent arguments.", WKTKeywords.TimeCRS,
                DefaultDerivedCRS.getType(HardCodedCRS.TIME, HardCodedCS.SECONDS));

        assertNull("Using inconsistent arguments.",
                DefaultDerivedCRS.getType(HardCodedCRS.TIME, HardCodedCS.GRAVITY_RELATED_HEIGHT));

        assertEquals("Using consistent arguments.", WKTKeywords.GeodeticCRS,
                DefaultDerivedCRS.getType(HardCodedCRS.WGS84, HardCodedCS.GEODETIC_2D));

        assertEquals("Using consistent arguments but one more dimension.", WKTKeywords.GeodeticCRS,
                DefaultDerivedCRS.getType(HardCodedCRS.WGS84, HardCodedCS.GEODETIC_3D));

        assertEquals("Using consistent arguments.", WKTKeywords.GeodeticCRS,
                DefaultDerivedCRS.getType(HardCodedCRS.GEOCENTRIC, HardCodedCS.CARTESIAN_3D));

        assertEquals("Using consistent arguments but one less dimension.", WKTKeywords.GeodeticCRS,
                DefaultDerivedCRS.getType(HardCodedCRS.GEOCENTRIC, HardCodedCS.CARTESIAN_2D));

        assertEquals("Using different coordinate system type.", WKTKeywords.EngineeringCRS,
                DefaultDerivedCRS.getType(HardCodedCRS.GEOCENTRIC, HardCodedCS.SPHERICAL));

        assertEquals("Using different coordinate system type.", WKTKeywords.EngineeringCRS,
                DefaultDerivedCRS.getType(HardCodedCRS.WGS84, HardCodedCS.CARTESIAN_2D));

        assertEquals("Using illegal coordinate system type.", WKTKeywords.EngineeringCRS,
                DefaultDerivedCRS.getType(HardCodedCRS.WGS84, HardCodedCS.GRAVITY_RELATED_HEIGHT));
    }
}
