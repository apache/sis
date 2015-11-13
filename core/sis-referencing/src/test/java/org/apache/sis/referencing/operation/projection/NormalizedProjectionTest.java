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

import org.apache.sis.referencing.operation.transform.TransformTestCase;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static java.lang.StrictMath.*;
import static org.apache.sis.internal.metadata.ReferencingServices.NAUTICAL_MILE;
import static org.junit.Assert.*;


/**
 * Tests the {@link NormalizedProjection} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@DependsOn({
    // Following dependency is where the basic parameters (e.g. SEMI_MAJOR) are tested.
    // Those parameters are needed by NoOp pseudo-projection, which is used in this package.
    org.apache.sis.internal.referencing.provider.MapProjectionTest.class,
    InitializerTest.class
})
public final strictfp class NormalizedProjectionTest extends TransformTestCase {
    /**
     * Tolerance level for comparing floating point numbers.
     */
    static final double TOLERANCE = 1E-12;

    /**
     * Tests the value documented in the javadoc. Those value may be freely changed;
     * those tests exist only to increase the chances that the documented values are right.
     */
    @Test
    public void testDocumentation() {
        double minutes = toDegrees(NormalizedProjection.ANGULAR_TOLERANCE) * 60;
        assertEquals("Documentation said 1 cm precision.", 0.01, minutes*NAUTICAL_MILE, 0.005);

        minutes = toDegrees(NormalizedProjection.ITERATION_TOLERANCE) * 60;
        assertEquals("Documentation said 2.5 mm precision.", 0.0025, minutes*NAUTICAL_MILE, 0.0005);
    }

    /**
     * Tests the {@link NormalizedProjection#eccentricity} value.
     */
    @Test
    public void testEccentricity() {
        NormalizedProjection projection;
        transform = projection = new NoOp(false);
        assertEquals("eccentricity", 0.0, projection.eccentricity, 0.0);
        /*
         * Tested methods. Note the similarity between (1) and (3).
         *
         *  (1) Using double        arithmetic and axis lengths:  0.08181919084262157
         *  (2) Using double-double arithmetic and axis lengths:  0.08181919084262244
         *  (3) Using double-double arithmetic and flattening:    0.0818191908426215
         */
        transform = projection = new NoOp(true, false);
        assertEquals("eccentricity", 0.08181919084262244, projection.eccentricity, 0.0);

        transform = projection = new NoOp(true, true);
        assertEquals("eccentricity", 0.0818191908426215, projection.eccentricity, 0.0);
    }
}
