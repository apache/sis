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
 * Tests the {@link EquidistantCylindrical} projection.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class EquidistantCylindricalTest extends MapProjectionTestCase {
    /**
     * Creates a new test case.
     */
    public EquidistantCylindricalTest() {
    }

    /**
     * Returns the provider for the "Equidistant Cylindrical" projection.
     */
    private static MapProjection provider() {
        return new org.apache.sis.referencing.operation.provider.EquidistantCylindrical();
    }

    /**
     * Tests the point given in EPSG example.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void testSinglePoint() throws FactoryException, TransformException {
        final MapProjection provider = provider();
        final Parameters pg = Parameters.castOrWrap(provider.getParameters().createValue());
        pg.parameter("semi-major").setValue(WGS84_A);
        pg.parameter("semi-minor").setValue(WGS84_B);
        transform = provider.createMathTransform(null, pg);

        final double λ = 10;
        final double φ = 55;
        final var p = new DirectPosition2D(λ, φ);
        assertSame(p, transform.transform(p, p));
        assertEquals(1113194.91, p.x, 0.005);
        assertEquals(6097230.31, p.y, 0.005);

        assertSame(p, transform.inverse().transform(p, p));
        assertEquals(λ, p.x, Formulas.ANGULAR_TOLERANCE);
        assertEquals(φ, p.y, Formulas.ANGULAR_TOLERANCE);
    }
}
