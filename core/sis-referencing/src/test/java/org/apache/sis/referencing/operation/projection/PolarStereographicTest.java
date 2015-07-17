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
import org.apache.sis.internal.referencing.provider.PolarStereographicA;
import org.apache.sis.test.DependsOn;
import org.junit.Test;


/**
 * Tests the {@link PolarStereographic} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@DependsOn(NormalizedProjectionTest.class)
public final strictfp class PolarStereographicTest extends MapProjectionTestCase {
    /**
     * Tests the <cite>Polar Stereographic (variant A)</cite> case (EPSG:9810).
     * This test is defined in GeoAPI conformance test suite.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     *
     * @see org.opengis.test.referencing.ParameterizedTransformTest#testPolarStereographicA()
     */
    @Test
    @org.junit.Ignore("To debug")
    public void testTransverseMercator() throws FactoryException, TransformException {
        createGeoApiTest(new PolarStereographicA()).testPolarStereographicA();
    }
}
