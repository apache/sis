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
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.provider.ZonedTransverseMercator;
import org.apache.sis.referencing.internal.shared.Formulas;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.parameter.Parameterized;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.measure.Units;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.referencing.operation.transform.MathTransformFactoryMock;


/**
 * Tests the {@link ZonedGridSystem} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ZonedGridSystemTest extends MapProjectionTestCase {
    /**
     * Creates a new test case.
     */
    public ZonedGridSystemTest() {
    }

    /**
     * Creates a new instance of {@link ZonedGridSystem}.
     *
     * @param  ellipsoidal  {@code false} for a sphere, or {@code true} for WGS84 ellipsoid.
     */
    private void createProjection(final boolean ellipsoidal) throws FactoryException {
        final var method = new ZonedTransverseMercator();
        final Parameters values = parameters(method, ellipsoidal);
        values.parameter(Constants.SCALE_FACTOR) .setValue(0.9996, Units.UNITY );
        values.parameter(Constants.FALSE_EASTING).setValue(500000, Units.METRE );
        values.parameter("Initial longitude")    .setValue(  -180, Units.DEGREE);
        values.parameter("Zone width")           .setValue(     6, Units.DEGREE);
        transform = method.createMathTransform(new MathTransformFactoryMock(method), values);
        tolerance = Formulas.LINEAR_TOLERANCE;
        validate();
    }

    /**
     * Tests converting a point using the <cite>Transverse Mercator Zoned Grid System</cite> projection.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while transforming a coordinate.
     */
    @Test
    public void testUTM() throws FactoryException, TransformException {
        createProjection(true);
        /*
         * Verify parameters.
         */
        final ParameterValueGroup values = ((Parameterized) transform).getParameterValues();
        assertEquals(0.9996, values.parameter(Constants.SCALE_FACTOR) .doubleValue(Units.UNITY ));
        assertEquals(500000, values.parameter(Constants.FALSE_EASTING).doubleValue(Units.METRE ));
        assertEquals(  -180, values.parameter("Initial longitude")    .doubleValue(Units.DEGREE));
        assertEquals(     6, values.parameter("Zone width")           .doubleValue(Units.DEGREE));
        /*
         * Tests projection of CN Tower coordinate, which is in UTM zone 17.
         */
        verifyTransform(new double[] {
            -79 - (23 - 13.70/60)/60,   // 79°23′13.70″W
             43 + (38 + 33.24/60)/60    // 43°38′33.24″N
        }, new double[] {
            17630698.19, 4833450.51
        });
    }
}
