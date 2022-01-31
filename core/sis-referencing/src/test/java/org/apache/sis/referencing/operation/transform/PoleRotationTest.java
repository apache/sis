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
 * Tests {@link PoleRotation}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
public final strictfp class PoleRotationTest extends MathTransformTestCase {
    /**
     * Returns the transform factory to use for testing purpose.
     * This mock supports only the "affine" and "concatenate" operations.
     */
    private static MathTransformFactory factory() {
        return new MathTransformFactoryMock(null);
    }

    /**
     * Creates a new test case.
     */
    public PoleRotationTest() {
        tolerance = Formulas.ANGULAR_TOLERANCE;
    }

    /**
     * Creates a new transform which should be the inverse of current transform according the
     * parameters declared in {@link PoleRotation#context}. Those parameters may be wrong even
     * if the coordinates transformed by {@code transform.inverse()} are corrects because the
     * parameters are only for WKT formatting (they are not actually used for transformation,
     * unless we force their use as done in this method).
     */
    private void inverseSouthPoleTransform() throws FactoryException, TransformException {
        final ParameterValueGroup pg = ((Parameterized) transform.inverse()).getParameterValues();
        transform = PoleRotation.rotateSouthPole(factory(),
                pg.parameter("grid_south_pole_latitude") .doubleValue(),
                pg.parameter("grid_south_pole_longitude").doubleValue(),
                pg.parameter("grid_south_pole_angle")    .doubleValue());
    }

    /**
     * Creates a new transform which should be the inverse of current transform according
     * the parameters declared in {@link PoleRotation#context}. This is the same work than
     * {@link #inverseSouthPoleTransform()} but for the other transform.
     */
    private void inverseNorthPoleTransform() throws FactoryException, TransformException {
        final ParameterValueGroup pg = ((Parameterized) transform.inverse()).getParameterValues();
        transform = PoleRotation.rotateNorthPole(factory(),
                pg.parameter("grid_north_pole_latitude") .doubleValue(),
                pg.parameter("grid_north_pole_longitude").doubleValue(),
                pg.parameter("north_pole_grid_longitude").doubleValue());
    }

    /**
     * Tests a rotation of south pole with the new pole on Greenwich.
     * The {@link ucar.unidata.geoloc.projection.RotatedLatLon} class
     * has been used as a reference implementation for expected values.
     *
     * @throws FactoryException if the transform can not be created.
     * @throws TransformException if an error occurred while transforming a point.
     */
    @Test
    public void testRotateSouthPoleOnGreenwich() throws FactoryException, TransformException {
        transform = PoleRotation.rotateSouthPole(factory(), -60, 0, 0);
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
     * The {@link ucar.unidata.geoloc.projection.RotatedLatLon} class has
     * been used as a reference implementation for expected values.
     *
     * @throws FactoryException if the transform can not be created.
     * @throws TransformException if an error occurred while transforming a point.
     */
    @Test
    @DependsOnMethod("testRotateSouthPoleOnGreenwich")
    public void testRotateSouthPoleWithAngle() throws FactoryException, TransformException {
        transform = PoleRotation.rotateSouthPole(factory(), -50, 20, 10);
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

    /**
     * Tries rotating a pole to opposite hemisphere.
     *
     * @throws FactoryException if the transform can not be created.
     * @throws TransformException if an error occurred while transforming a point.
     */
    @Test
    public void testRotateSouthToOppositeHemisphere() throws FactoryException, TransformException {
        transform = PoleRotation.rotateSouthPole(factory(), 50, 20, 10);
        final double[] coordinates = {      // (λ,φ) coordinates to convert.
             20, 51,
             80, 44,
            -30, 89
        };
        final double[] expected = {         // (λ,φ) coordinates after conversion.
             -10.000000000, -89.000000000,
              64.651211252, -49.758697265,
             -11.207848626, -50.636582758
        };
        verifyTransform(coordinates, expected);
        inverseSouthPoleTransform();
        verifyTransform(expected, coordinates);
    }

    /**
     * Tests a rotation of north pole with the new pole on Greenwich.
     *
     * <h4>Comparison with UCAR library</h4>
     * {@link ucar.unidata.geoloc.projection.RotatedPole} in UCAR netCDF library version 5.5.2
     * gives results with an offset of 180° in longitude values compared to our implementation.
     * But geometrical reasoning suggests that our implementation is correct: if we rotate the
     * pole to 60°N, then latitude of 54°N on Greenwich meridian become only 6° below new pole,
     * i.e. 84°N but still on the same meridian (Greenwich) because we did not cross the pole.
     *
     * @throws FactoryException if the transform can not be created.
     * @throws TransformException if an error occurred while transforming a point.
     */
    @Test
    public void testRotateNorthPoleOnGreenwich() throws FactoryException, TransformException {
        transform = PoleRotation.rotateNorthPole(factory(), 60, 0, 0);
        final double[] coordinates = {      // (λ,φ) coordinates to convert.
             0, 54,
            20, 62,
           -30, 89
        };
        final double[] expected = {         // (λ,φ) coordinates after conversion.
               0.000000000, 84.000000000,
             110.307140436, 80.141810970,
            -178.973119126, 60.862133738
        };
        verifyTransform(coordinates, expected);
        inverseNorthPoleTransform();
        verifyTransform(expected, coordinates);
    }

    /**
     * Tests a rotation of north pole with the pole on arbitrary meridian.
     * Result can be compared with PROJ using the following command, where
     * {@code coords.txt} is a file containing input coordinates in (λ,φ)
     * order and the output is in (φ,λ) order.
     *
     * {@preformat shell
     *   cs2cs -I -E -f %g "EPSG:4326" +to +type=crs +proj=ob_tran +o_proj=longlat +datum=WGS84 +no_defs \
     *         +o_lat_p=70 +o_lon_p=40 +lon_0=10 coords.txt
     * }
     *
     * @throws FactoryException if the transform can not be created.
     * @throws TransformException if an error occurred while transforming a point.
     */
    @Test
    @DependsOnMethod("testRotateNorthPoleOnGreenwich")
    public void testRotateNorthPole() throws FactoryException, TransformException {
        transform = PoleRotation.rotateNorthPole(factory(), 70, 40, 10);
        final double[] coordinates = {      // (λ,φ) coordinates to convert.
             0, 54,
            20, 62,
           -30, 89
        };
        final double[] expected = {         // (λ,φ) coordinates after conversion.
             -58.817428350, 66.096411904,
             -44.967324181, 78.691210976,
            -167.208632734, 70.320491507
        };
        verifyTransform(coordinates, expected);
        inverseNorthPoleTransform();
        verifyTransform(expected, coordinates);
    }

    /**
     * Tries rotating a pole to opposite hemisphere.
     *
     * @throws FactoryException if the transform can not be created.
     * @throws TransformException if an error occurred while transforming a point.
     */
    @Test
    public void testRotateNorthToOppositeHemisphere() throws FactoryException, TransformException {
        transform = PoleRotation.rotateNorthPole(factory(), -50, 20, -10);
        final double[] coordinates = {      // (λ,φ) coordinates to convert.
             20, -51,
             80, -44,
            -30, -89
        };
        final double[] expected = {         // (λ,φ) coordinates after conversion.
             -10.000000000, 89.000000000,
              64.651211252, 49.758697265,
             -11.207848626, 50.636582758
        };
        verifyTransform(coordinates, expected);
        inverseNorthPoleTransform();
        verifyTransform(expected, coordinates);
    }

    /**
     * Tests derivative.
     *
     * @throws FactoryException if the transform can not be created.
     * @throws TransformException if an error occurred while computing a derivative.
     */
    @Test
    public void testDerivative() throws FactoryException, TransformException {
        transform = PoleRotation.rotateSouthPole(factory(), -50, 0, 0);
        derivativeDeltas = new double[] {1E-6, 1E-6};
        verifyDerivative(  0, -51);
        verifyDerivative( 20, -58);
        verifyDerivative(-30, -40);
    }
}
