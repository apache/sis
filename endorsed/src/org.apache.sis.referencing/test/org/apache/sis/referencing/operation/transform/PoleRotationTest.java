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

import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.internal.shared.Formulas;
import org.apache.sis.parameter.Parameterized;

// Test dependencies
import org.junit.jupiter.api.Test;


/**
 * Tests {@link PoleRotation}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class PoleRotationTest extends MathTransformTestCase {
    /**
     * Returns the transform factory to use for testing purpose.
     * This mock supports only the "affine" and "concatenate" operations.
     */
    private static MathTransformFactory factory() {
        return new MathTransformFactoryMock();
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
     * the parameters declared in {@link PoleRotation#context}. This is the same work as
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
     * The {@link ucar.unidata.geoloc.projection.RotatedLatLon} class has
     * been used as a reference implementation for computing expected values.
     *
     * @throws FactoryException if the transform cannot be created.
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
              0,               -81,
             60.1404538930820, -75.6297153018960,
            136.9005187159727, -45.6718682605614
        };
        verifyTransform(coordinates, expected);
        inverseSouthPoleTransform();
        verifyTransform(expected, coordinates);
    }

    /**
     * Tests a rotation of south pole with the new pole on a non-zero longitude.
     * The {@link ucar.unidata.geoloc.projection.RotatedLatLon} class has been
     * used as a reference implementation for computing expected values.
     *
     * @throws FactoryException if the transform cannot be created.
     * @throws TransformException if an error occurred while transforming a point.
     */
    @Test
    public void testRotateSouthPoleOnOtherLongitude() throws FactoryException, TransformException {
        transform = PoleRotation.rotateSouthPole(factory(), -70, 25, 0);
        final double[] coordinates = {      // (λ,φ) coordinates to convert.
             25, -69,
             20, -51,
            100, -71
        };
        final double[] expected = {         // (λ,φ) coordinates after conversion.
              0,               -89,
             -9.6282124673448, -70.8563796930179,
            127.8310735055447, -66.5368804564497
        };
        verifyTransform(coordinates, expected);
        inverseSouthPoleTransform();
        verifyTransform(expected, coordinates);
    }

    /**
     * Tests a rotation of south pole with the pole on arbitrary meridian.
     * The {@link ucar.unidata.geoloc.projection.RotatedLatLon} class has
     * been used as a reference implementation for computing expected values.
     *
     * @throws FactoryException if the transform cannot be created.
     * @throws TransformException if an error occurred while transforming a point.
     */
    @Test
    public void testRotateSouthPoleWithAngle() throws FactoryException, TransformException {
        transform = PoleRotation.rotateSouthPole(factory(), -50, 20, 10);
        final double[] coordinates = {      // (λ,φ) coordinates to convert.
             20, -51,
             80, -44,
            -30, -89
        };
        final double[] expected = {         // (λ,φ) coordinates after conversion.
             170,               -89,
              95.3487887483185, -49.7586972646198,
            -188.7921513735695, -50.6365827575445
        };
        verifyTransform(coordinates, expected);
        inverseSouthPoleTransform();
        verifyTransform(expected, coordinates);
    }

    /**
     * Tries rotating a pole to opposite hemisphere.
     * The {@link ucar.unidata.geoloc.projection.RotatedLatLon} class has
     * been used as a reference implementation for computing expected values.
     *
     * @throws FactoryException if the transform cannot be created.
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
             -10,               -89,
              64.6512112516815, -49.7586972646198,
             -11.2078486264305, -50.6365827575445
        };
        verifyTransform(coordinates, expected);
        inverseSouthPoleTransform();
        verifyTransform(expected, coordinates);
    }

    /**
     * Tests a rotation of north pole with the new pole on Greenwich.
     * The {@link ucar.unidata.geoloc.projection.RotatedPole} class
     * has been used as a reference implementation for expected values.
     *
     * @throws FactoryException if the transform cannot be created.
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
            180, 84,
            -69.6928595614074,  80.1418109704940,
              1.0268808754468,  60.8621337379806
        };
        verifyTransform(coordinates, expected);
        inverseNorthPoleTransform();
        verifyTransform(expected, coordinates);
    }

    /**
     * Tests a rotation of north pole with the new pole on a non-zero longitude.
     * The {@link ucar.unidata.geoloc.projection.RotatedPole} class has been used
     * as a reference implementation for computing expected values.
     *
     * @throws FactoryException if the transform cannot be created.
     * @throws TransformException if an error occurred while transforming a point.
     */
    @Test
    public void testRotateNorthPoleOnOtherLongitude() throws FactoryException, TransformException {
        transform = PoleRotation.rotateNorthPole(factory(), 70, 25, 0);
        final double[] coordinates = {      // (λ,φ) coordinates to convert.
             25, 72,
             20, 51,
            100, 71
        };
        final double[] expected = {         // (λ,φ) coordinates after conversion.
              0,                88,
            170.3717875326552,  70.8563796930179,
            -52.1689264944553,  66.5368804564497
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
     * {@snippet lang="shell" :
     *   cs2cs -I -E -f %g "EPSG:4326" +to +type=crs +proj=ob_tran +o_proj=longlat +datum=WGS84 +no_defs \
     *         +o_lat_p=70 +o_lon_p=40 +lon_0=190 coords.txt
     *   }
     *
     * Note that a 180° offset must be added to the {@code +lon_0} parameter.
     *
     * @throws FactoryException if the transform cannot be created.
     * @throws TransformException if an error occurred while transforming a point.
     */
    @Test
    public void testRotateNorthPole() throws FactoryException, TransformException {
        transform = PoleRotation.rotateNorthPole(factory(), 70, 40, 10);
        final double[] coordinates = {      // (λ,φ) coordinates to convert.
             0, 54,
            20, 62,
           -30, 89
        };
        final double[] expected = {         // (λ,φ) coordinates after conversion.
             121.1825716500646,  66.0964119035041,
             135.0326758188633,  78.6912109761956,
              12.7913672657394,  70.3204915065785
        };
        verifyTransform(coordinates, expected);
        inverseNorthPoleTransform();
        verifyTransform(expected, coordinates);
    }

    /**
     * Tries rotating a pole to opposite hemisphere.
     *
     * @throws FactoryException if the transform cannot be created.
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
             170,                89,
            -115.3487887483185,  49.7586972646198,
             168.7921513735695,  50.6365827575445
        };
        verifyTransform(coordinates, expected);
        inverseNorthPoleTransform();
        verifyTransform(expected, coordinates);
    }

    /**
     * Tests derivative for a south pole rotation.
     *
     * @throws FactoryException if the transform cannot be created.
     * @throws TransformException if an error occurred while computing a derivative.
     */
    @Test
    public void testDerivativeSouth() throws FactoryException, TransformException {
        transform = PoleRotation.rotateSouthPole(factory(), -50, 0, 0);
        derivativeDeltas = new double[] {1E-6, 1E-6};
        verifyDerivative(  0, -51);
        verifyDerivative( 20, -58);
        verifyDerivative(-30, -40);
    }

    /**
     * Tests derivative for a north pole rotation.
     *
     * @throws FactoryException if the transform cannot be created.
     * @throws TransformException if an error occurred while computing a derivative.
     */
    @Test
    public void testDerivativeNorth() throws FactoryException, TransformException {
        transform = PoleRotation.rotateNorthPole(factory(), 50, 0, 0);
        derivativeDeltas = new double[] {1E-5, 1E-5};
        verifyDerivative(  0, 51);
        verifyDerivative( 20, 58);
        verifyDerivative(-30, 40);
    }
}
