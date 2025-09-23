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

import static java.lang.StrictMath.*;
import static org.apache.sis.metadata.internal.shared.ReferencingServices.NAUTICAL_MILE;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.sis.test.FailureDetailsReporter;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.test.referencing.TransformTestCase;


/**
 * Tests the {@link NormalizedProjection} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@ExtendWith(FailureDetailsReporter.class)
public final class NormalizedProjectionTest extends TransformTestCase {
    /**
     * Tolerance level for comparing floating point numbers.
     */
    static final double TOLERANCE = 1E-12;

    /**
     * Creates a new test case.
     */
    public NormalizedProjectionTest() {
    }

    /**
     * Tests the value documented in the javadoc. Those value may be freely changed;
     * those tests exist only to increase the chances that the documented values are right.
     */
    @Test
    public void testDocumentation() {
        double minutes = toDegrees(NormalizedProjection.ANGULAR_TOLERANCE) * 60;
        assertEquals(0.01, minutes*NAUTICAL_MILE, 0.005, "Documentation said 1 cm precision.");

        minutes = toDegrees(NormalizedProjection.ITERATION_TOLERANCE) * 60;
        assertEquals(0.0025, minutes*NAUTICAL_MILE, 0.0005, "Documentation said 2.5 mm precision.");
    }

    /**
     * Tests the {@link NormalizedProjection#eccentricity} value.
     */
    @Test
    public void testEccentricity() {
        NormalizedProjection projection;
        transform = projection = new NoOp(false);
        assertEquals(0.0, projection.eccentricity);
        /*
         * Tested methods. Note the similarity between (1) and (3).
         *
         *  (1) Using double        arithmetic and axis lengths:  0.08181919084262157
         *  (2) Using double-double arithmetic and axis lengths:  0.08181919084262244
         *  (3) Using double-double arithmetic and flattening:    0.0818191908426215
         */
        transform = projection = new NoOp(true, false);
        assertEquals(0.08181919084262244, projection.eccentricity);

        transform = projection = new NoOp(true, true);
        assertEquals(0.0818191908426215, projection.eccentricity);
    }
}
