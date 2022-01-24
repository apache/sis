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

import org.junit.Test;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.parameter.Parameterized;
import org.apache.sis.test.DependsOnMethod;


/**
 * Tests {@link RotatedPole}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
public final strictfp class RotatedPoleTest extends MathTransformTestCase {
    /**
     * Returns the transform factory to use for testing purpose.
     * This mock supports only the "affine" and "concatenate" operations.
     */
    private static MathTransformFactory factory() {
        return new MathTransformFactoryMock(null);
    }

    /**
     * Creates a new transform which should be the inverse of current transform according the
     * parameters declared in {@link RotatedPole#context}. Those parameters may be wrong even
     * if the coordinates transformed by {@code transform.inverse()} are corrects because the
     * parameters are only for WKT formatting (they are not actually used for transformation,
     * unless we force their use as done in this method).
     */
    private void inverseSouthPoleTransform() throws FactoryException, TransformException {
        final ParameterValueGroup pg = ((Parameterized) transform.inverse()).getParameterValues();
        transform = RotatedPole.rotateSouthPole(factory(),
                pg.parameter("grid_south_pole_longitude").doubleValue(),
                pg.parameter("grid_south_pole_latitude") .doubleValue(),
                pg.parameter("grid_south_pole_angle")    .doubleValue());

    }

    /**
     * Tests a rotation of south pole with the new pole on Greenwich.
     * The {@link ucar.unidata.geoloc.LatLonPoint} class has been used
     * as a reference implementation for computing the expected values.
     *
     * @throws FactoryException if the transform can not be created.
     * @throws TransformException if an error occurred while transforming a point.
     */
    @Test
    public void testRotateSouthPoleOnGreenwich() throws FactoryException, TransformException {
        transform = RotatedPole.rotateSouthPole(factory(), 0, -60, 0);
        tolerance = Formulas.ANGULAR_TOLERANCE;
        isDerivativeSupported = false;
        final double[] coordinates = {      // (λ,φ) coordinates to convert.
              0, -51,
             20, -51,
            100, -61
        };
        final double[] expected = {         // (λ,φ) coordinates after conversion.
              0.000000000, -81.000000000,
             60.140453893, -75.629715301,
            136.900518716, -45.671868261
        };
        verifyTransform(coordinates, expected);
        inverseSouthPoleTransform();
        verifyTransform(expected, coordinates);
    }

    /**
     * Tests a rotation of south pole with the pole on arbitrary meridian.
     * The {@link ucar.unidata.geoloc.LatLonPoint} class has been used as
     * a reference implementation for computing the expected values.
     *
     * @throws FactoryException if the transform can not be created.
     * @throws TransformException if an error occurred while transforming a point.
     */
    @Test
    @DependsOnMethod("testRotateSouthPoleOnGreenwich")
    public void testRotateSouthPoleWithAngle() throws FactoryException, TransformException {
        transform = RotatedPole.rotateSouthPole(factory(), 20, -50, 10);
        tolerance = Formulas.ANGULAR_TOLERANCE;
        isDerivativeSupported = false;
        final double[] coordinates = {      // (λ,φ) coordinates to convert.
             20, -51,
             80, -44,
            -30, -89
        };
        final double[] expected = {         // (λ,φ) coordinates after conversion.
             170.000000000, -89.000000000,
              95.348788748, -49.758697265,
            -188.792151374, -50.636582758
        };
        verifyTransform(coordinates, expected);
        inverseSouthPoleTransform();
        verifyTransform(expected, coordinates);
    }
}
