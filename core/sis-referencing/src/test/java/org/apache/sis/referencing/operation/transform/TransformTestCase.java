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
package org.apache.sis.referencing.operation.transform;

import java.util.Random;

import static org.junit.Assume.*;
import static org.apache.sis.test.Assert.*;


/**
 * Placeholder for a GeoAPI 3.1 method which was not available in GeoAPI 3.0.
 * This placeholder does nothing. See Apache SIS JDK6 branch for a real test.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.6
 * @module
 */
public class TransformTestCase extends org.opengis.test.referencing.TransformTestCase {
    /**
     * The deltas to use for approximating math transform derivatives by the finite differences method.
     */
    protected double[] derivativeDeltas;

    /**
     * Placeholder for a GeoAPI 3.1 method which was not available in GeoAPI 3.0.
     * This placeholder does nothing. See Apache SIS JDK6 branch for a real test.
     *
     * @param coordinate Ignored.
     */
    protected final void verifyDerivative(final double... coordinate) {
        // See GeoAPI 3.1 for the real test.
        // The test is run on Apache SIS branches.
       assumeTrue(PENDING_NEXT_GEOAPI_RELEASE); // For reporting the test as skippped.
    }

    /**
     * Placeholder for a GeoAPI 3.1 method which was not available in GeoAPI 3.0.
     * This placeholder does nothing. See Apache SIS JDK6 branch for a real test.
     *
     * @param minOrdinates    Ignored.
     * @param maxOrdinates    Ignored.
     * @param numOrdinates    Ignored.
     * @param randomGenerator Ignored.
     */
    protected final void verifyInDomain(final double[] minOrdinates, final double[] maxOrdinates,
            final int[] numOrdinates, final Random randomGenerator)
    {
        // See GeoAPI 3.1 for the real test.
        // The test is run on Apache SIS branches.
       assumeTrue(PENDING_NEXT_GEOAPI_RELEASE); // For reporting the test as skippped.
    }
}
