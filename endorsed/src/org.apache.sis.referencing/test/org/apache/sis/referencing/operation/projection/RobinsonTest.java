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
import org.apache.sis.parameter.Parameters;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.internal.shared.Formulas;
import org.apache.sis.referencing.operation.provider.MapProjection;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests the {@link Robinson} projection.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class RobinsonTest extends MapProjectionTestCase {
    /**
     * Creates a new test case.
     */
    public RobinsonTest() {
        final double delta = (100.0 / 60) / 1852;      // Approximately 100 metres.
        derivativeDeltas = new double[] {delta, delta};
    }

    /**
     * Returns the provider for the "Robinson" projection.
     */
    private static MapProjection provider() {
        return new org.apache.sis.referencing.operation.provider.Robinson();
    }

    /**
     * Tests the first point given in Snyder example which does not involve interpolation.
     * Tests also a few simple points at equator and on pole.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void testSimplePoints() throws FactoryException, TransformException {
        final MapProjection provider = provider();
        final Parameters pg = Parameters.castOrWrap(provider.getParameters().createValue());
        pg.parameter("semi-major").setValue(1);
        pg.parameter("semi-minor").setValue(1);
        transform = provider.createMathTransform(null, pg);
        tolerance = Formulas.ANGULAR_TOLERANCE;

        final double[] expected = {
            0.740630, 0,            // 0°N.
            0.711005, 0.503055,     // 30°N, result from Snyder.
            0.591467, 0.993400,     // 60°N.
            0.394164, 1.352300,     // 90°N.
        };
        for (int i=0; i <= 3; i++) {
            double λ = 50;
            double φ = i * 30;
            final var p = new DirectPosition2D(λ, φ);
            assertSame(p, transform.transform(p, p));
            assertEquals(expected[i*2    ], p.x, 0.000001);
            assertEquals(expected[i*2 + 1], p.y, 0.000001);

            assertSame(p, transform.inverse().transform(p, p));
            assertEquals(λ, p.x, Formulas.ANGULAR_TOLERANCE);
            assertEquals(φ, p.y, Formulas.ANGULAR_TOLERANCE);

            verifyDerivative(λ, φ);
        }
    }

    /**
     * Tests the second point given in Snyder example which involves interpolation.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void testInterpolation() throws FactoryException, TransformException {
        final MapProjection provider = provider();
        final Parameters pg = Parameters.castOrWrap(provider.getParameters().createValue());
        pg.parameter("semi-major").setValue(6370997);
        pg.parameter("semi-minor").setValue(6370997);
        transform = provider.createMathTransform(null, pg);
        tolerance = Formulas.LINEAR_TOLERANCE;

        final double λ = -102;
        final double φ = -47;
        final var p = new DirectPosition2D(λ, φ);
        assertSame(p, transform.transform(p, p));
        assertEquals(-8521076, p.x, 5);
        assertEquals(-5009012, p.y, 5);

        assertSame(p, transform.inverse().transform(p, p));
        assertEquals(λ, p.x, Formulas.ANGULAR_TOLERANCE);
        assertEquals(φ, p.y, Formulas.ANGULAR_TOLERANCE);

        verifyDerivative(λ, φ);
    }
}
