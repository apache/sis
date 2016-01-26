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
package org.apache.sis.referencing;

import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.SingleCRS;

// Test dependencies
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link EPSGFactoryFallback} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn({
  StandardDefinitionsTest.class,
  CommonCRSTest.class
})
public final strictfp class EPSGFactoryFallbackTest extends TestCase {
    /**
     * Tests {@link EPSGFactoryFallback#createCoordinateReferenceSystem(String)}.
     *
     * @throws FactoryException If a CRS can not be constructed.
     *
     * @see CRSTest#testForEpsgCode()
     * @see CRSTest#testForCrsCode()
     */
    @Test
    public void testForCode() throws FactoryException {
        verifyForCode(CommonCRS.WGS84 .geographic(),            "4326");
        verifyForCode(CommonCRS.WGS72 .geographic(),            "4322");
        verifyForCode(CommonCRS.SPHERE.geographic(),            "4047");
        verifyForCode(CommonCRS.NAD83 .geographic(),            "4269");
        verifyForCode(CommonCRS.NAD27 .geographic(),            "4267");
        verifyForCode(CommonCRS.ETRS89.geographic(),            "4258");
        verifyForCode(CommonCRS.ED50  .geographic(),            "4230");
        verifyForCode(CommonCRS.WGS84 .geocentric(),            "4978");
        verifyForCode(CommonCRS.WGS72 .geocentric(),            "4984");
        verifyForCode(CommonCRS.ETRS89.geocentric(),            "4936");
        verifyForCode(CommonCRS.WGS84 .geographic3D(),          "4979");
        verifyForCode(CommonCRS.WGS72 .geographic3D(),          "4985");
        verifyForCode(CommonCRS.ETRS89.geographic3D(),          "4937");
        verifyForCode(CommonCRS.Vertical.MEAN_SEA_LEVEL.crs(),  "5714");
        verifyForCode(CommonCRS.Vertical.DEPTH.crs(),           "5715");
    }

    /**
     * Asserts that the result of {@link CommonCRS#forCode(String, String, FactoryException)} is the given CRS.
     */
    private static void verifyForCode(final SingleCRS expected, final String code) throws FactoryException {
        assertSame(code, expected, EPSGFactoryFallback.INSTANCE.createCoordinateReferenceSystem(code));
    }
}
