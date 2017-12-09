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

import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.CoordinateSystem;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.cs.HardCodedAxes;
import org.apache.sis.referencing.cs.HardCodedCS;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.operation.HardCodedConversions;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link CoordinateOperations}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public final strictfp class CoordinateOperationsTest extends TestCase {
    /**
     * Tests {@link CoordinateOperations#isWrapAround(CoordinateSystemAxis)}.
     */
    @Test
    public void testIsWrapAround() {
        assertFalse("GEODETIC_LATITUDE",  CoordinateOperations.isWrapAround(HardCodedAxes.GEODETIC_LATITUDE));
        assertTrue ("GEODETIC_LONGITUDE", CoordinateOperations.isWrapAround(HardCodedAxes.GEODETIC_LONGITUDE));
        assertFalse("ELLIPSOIDAL_HEIGHT", CoordinateOperations.isWrapAround(HardCodedAxes.ELLIPSOIDAL_HEIGHT));
    }

    /**
     * Tests {@link CoordinateOperations#wrapAroundChanges(CoordinateReferenceSystem, CoordinateSystem)}.
     */
    @Test
    @DependsOnMethod("testIsWrapAround")
    public void testWrapAroundChanges() {
        CoordinateReferenceSystem sourceCRS = HardCodedCRS.WGS84_3D;
        CoordinateSystem          targetCS = HardCodedCS.GEODETIC_2D;
        assertTrue("(λ,φ,h) → (λ,φ)", CoordinateOperations.wrapAroundChanges(sourceCRS, targetCS).isEmpty());

        sourceCRS = HardCodedCRS.WGS84_3D.forConvention(AxesConvention.POSITIVE_RANGE);
        assertArrayEquals("(λ′,φ,h) → (λ,φ)", new Integer[] {0},
                CoordinateOperations.wrapAroundChanges(sourceCRS, targetCS).toArray());

        targetCS = HardCodedCS.GEODETIC_φλ;
        assertArrayEquals("(λ′,φ,h) → (φ,λ)", new Integer[] {1},
                CoordinateOperations.wrapAroundChanges(sourceCRS, targetCS).toArray());

        sourceCRS = HardCodedConversions.mercator((GeographicCRS) sourceCRS);
        assertArrayEquals("(λ′,φ,h) → (φ,λ)", new Integer[] {1},
                CoordinateOperations.wrapAroundChanges(sourceCRS, targetCS).toArray());

    }
}
