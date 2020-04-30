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
package org.apache.sis.referencing.operation.projection;

import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.referencing.provider.MapProjection;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.junit.Assert.*;
import static java.lang.Double.NaN;


/**
 * Tests the {@link HyperbolicCassiniSoldner} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
@DependsOn(CassiniSoldnerTest.class)
public final strictfp class HyperbolicCassiniSoldnerTest extends MapProjectionTestCase {
    /**
     * Returns the operation method for the projection tested in this class.
     */
    private static MapProjection method() {
        return new org.apache.sis.internal.referencing.provider.HyperbolicCassiniSoldner();
    }

    /**
     * Tests the point given in EPSG example for the standard case.
     * This is the same test than {@link #runGeoapiTest()} but is repeated here for easier debugging.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void testExampleEPSG() throws FactoryException, TransformException {
        createCompleteProjection(method(),
                317063.667,                             // Semi-major axis (chains)
                317063.667 * (20854895./20926202),      // Semi-minor axis (chains)
                179 + 20./60,                           // Longitude of natural origin
                -(16 + 15./60),                         // Latitude of natural origin
                NaN,                                    // Standard parallel 1 (none)
                NaN,                                    // Standard parallel 2 (none)
                NaN,                                    // Scale factor (none)
                12513.318,                              // False easting  (chains)
                16628.885);                             // False northing (chains)

        final double λ =  179 + (59 + 39.6115/60)/60;   // 179°59′39.6115″E
        final double φ = -(16 + (50 + 29.2435/60)/60);  //  16°50′29.2435″S
        final DirectPosition2D p = new DirectPosition2D(λ, φ);
        assertSame(p, transform.transform(p, p));
        assertEquals(16015.2890, p.x, 0.00005);
        assertEquals(13369.6601, p.y, 0.00005);

        assertSame(p, transform.inverse().transform(p, p));
        assertEquals(λ, p.x, Formulas.ANGULAR_TOLERANCE);
        assertEquals(φ, p.y, Formulas.ANGULAR_TOLERANCE);
    }

    /**
     * Tests the <cite>"Hyperbolic Cassini-Soldner"</cite> (EPSG:9833) projection method.
     * This test is defined in GeoAPI conformance test suite.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     *
     * @see org.opengis.test.referencing.ParameterizedTransformTest#testHyperbolicCassiniSoldner()
     */
    @Test
    public void runGeoapiTest() throws FactoryException, TransformException {
        createGeoApiTestNoDerivatives(method()).testHyperbolicCassiniSoldner();
    }
}
