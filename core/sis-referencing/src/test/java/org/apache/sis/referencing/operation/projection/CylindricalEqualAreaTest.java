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
import org.opengis.test.ToleranceModifier;
import org.apache.sis.internal.referencing.Formulas;
import org.junit.Test;


/**
 * Tests the {@link CylindricalEqualArea} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public final strictfp class CylindricalEqualAreaTest extends MapProjectionTestCase {
    /**
     * Creates a map projection.
     */
    private void createCompleteProjection(final boolean ellipse,
            final double centralMeridian, final double standardParallel) throws FactoryException
    {
        createCompleteProjection(new org.apache.sis.internal.referencing.provider.CylindricalEqualArea(),
                ellipse, centralMeridian, 0, standardParallel, 1, 0, 0);
    }

    /**
     * Tests projection of a point the the in ellipsoidal case.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testEllipsoidal() throws FactoryException, TransformException {
        createCompleteProjection(true, 0, 0);
        tolerance = Formulas.LINEAR_TOLERANCE;
        toleranceModifier = ToleranceModifier.PROJECTION;
        final double λ = 2;
        final double φ = 1;
        final double x = 222638.98;             // Test point from Proj.4.
        final double y = 110568.81;
        verifyTransform(new double[] {λ, φ,  -λ, φ,  λ, -φ,  -λ, -φ},
                        new double[] {x, y,  -x, y,  x, -y,  -x, -y});
    }

    /**
     * Tests projection of a point the the in spherical case.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testSpherical() throws FactoryException, TransformException {
        createCompleteProjection(false, 0, 0);
        tolerance = Formulas.LINEAR_TOLERANCE;
        toleranceModifier = ToleranceModifier.PROJECTION;
        final double λ = 2;
        final double φ = 1;
        final double x = 222390.10;
        final double y = 111189.40;
        verifyTransform(new double[] {λ, φ,  -λ, φ,  λ, -φ,  -λ, -φ},
                        new double[] {x, y,  -x, y,  x, -y,  -x, -y});
    }
}
