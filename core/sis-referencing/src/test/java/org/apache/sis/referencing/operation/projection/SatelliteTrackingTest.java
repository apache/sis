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

import java.util.Collections;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.referencing.NilReferencingObject;
import org.apache.sis.internal.referencing.provider.SatelliteTracking;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.datum.DefaultEllipsoid;
import org.apache.sis.referencing.operation.transform.MathTransformFactoryMock;
import org.junit.Test;

import static java.lang.StrictMath.sin;
import static java.lang.StrictMath.toRadians;
import static org.junit.Assert.assertTrue;


/**
 * Tests coordinates computed by applying a satellite-tracking projection with {@link SatelliteTracking}.
 *
 * @author  Matthieu Bastianelli (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final strictfp class SatelliteTrackingTest extends MapProjectionTestCase {
    /**
     * Creates a new instance of {@link SatelliteTracking} concatenated with the (de)normalization matrices.
     * The new instance is stored in the inherited {@link #transform} field.
     * This methods uses projection parameters for Landsat 3 satellite, namely:
     *
     * <table class="sis">
     *   <caption>Hard-coded projection parameters</caption>
     *   <tr><th>Symbol</th> <th>Value</th>       <th>Meaning</th></tr>
     *   <tr><td>i</td>  <td>99.092°</td>         <td>Angle of inclination between the plane of the Earth's Equator and the plane of the satellite orbit.</td></tr>
     *   <tr><td>P1</td> <td>1440 minutes</td>    <td>Length of Earth's rotation with respect to the precessed ascending node.</td></tr>
     *   <tr><td>P2</td> <td>103.267 minutes</td> <td>Time required for revolution of the satellite.</td></tr>
     * </table>
     *
     * The Earth radius is set to 1.
     *
     * @param λ0  central meridian.
     * @param φ0  latitude crossing the central meridian at the desired origin of rectangular coordinates.
     * @param φ1  first parallel of conformality (with true scale).
     * @param φ2  second parallel of conformality (without true scale), or -φ1 for a cylindrical projection.
     */
    private void createForLandsat(final double λ0, final double φ0, final double φ1, final double φ2)
            throws FactoryException
    {
        final SatelliteTracking provider = new SatelliteTracking();
        final ParameterValueGroup values = provider.getParameters().createValue();
        final DefaultEllipsoid sphere = DefaultEllipsoid.createEllipsoid(
                Collections.singletonMap(DefaultEllipsoid.NAME_KEY, NilReferencingObject.UNNAMED), 1, 1, Units.METRE);

        values.parameter("semi_major")                 .setValue(sphere.getSemiMajorAxis());
        values.parameter("semi_minor")                 .setValue(sphere.getSemiMinorAxis());
        values.parameter("central_meridian")           .setValue(λ0);
        values.parameter("latitude_of_origin")         .setValue(φ0);
        values.parameter("standard_parallel_1")        .setValue(φ1);
        values.parameter("standard_parallel_2")        .setValue(φ2);
        values.parameter("satellite_orbit_inclination").setValue(  99.092);
        values.parameter("satellite_orbital_period")   .setValue( 103.267, Units.MINUTE);
        values.parameter("ascending_node_period")      .setValue(1440.0,   Units.MINUTE);
        transform = new MathTransformFactoryMock(provider).createParameterizedTransform(values);
        validate();
    }

    /**
     * Tests the projection of a few points on a sphere.
     *
     * Test based on the numerical example given by Snyder p. 360 to 363 of
     * <cite> Map Projections - A working Manual</cite>
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testCylindricalTransform() throws FactoryException, TransformException {
        tolerance = 1E-7;   // Number of digits in the output values provided by Snyder.
        tolerance = 1E-5;   // TODO
        createForLandsat(-90, 0, 30, -30);
        assertTrue(isInverseTransformSupported);
        verifyTransform(
                new double[] {              // (λ,φ) coordinates in degrees to project.
                    -75, 40
                },
                new double[] {              // Expected (x,y) results on a unit sphere.
                    0.2267249,  0.6459071
                });
    }

    /**
     * Tests the projection of a few points on a sphere.
     * Test based on the numerical example given by Snyder pages 360 to 363 of
     * <cite>Map Projections - A working Manual</cite>.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testConicTransform() throws FactoryException, TransformException {
        tolerance = 1E-7;   // Number of digits in the output values provided by Snyder.
        tolerance = 1E-5;   // TODO
        createForLandsat(-90, 30, 45, 70);
        assertTrue(isInverseTransformSupported);
        verifyTransform(
                new double[] {              // (λ,φ) coordinates in degrees to project.
                    -75, 40
                },
                new double[] {              // Expected (x,y) results on a unit sphere.
                    0.2001910,  0.2121685
                });
        /*
         * Expected intermediate values (can be checked in a debugger):
         *
         *    F₀ = 13.9686735°
         *    F₁ = 15.7111447°
         *    F₂ = 28.7497148°
         */
    }

    /**
     * Compares the projection of a few points against expected values computed from intermediate values
     * published by Snyder. As Snyder tables in chapter 28 introduce only the values for <var>n</var>,
     * <var>F₁</var> and <var>ρ</var> coefficients, the test was realized by checking these coefficients
     * in debugger and by extracting the computed results of the projection.
     *
     * @param  xScale       scale to apply on <var>x</var> values.
     * @param  coordinates  the points to transform.
     * @param  internal     the expected intermediate transformation results.
     * @throws TransformException if the transformation failed.
     */
    private void verifyCylindricInternal(final double xScale, final double[] coordinates, final double[] internal)
            throws TransformException
    {
        for (int i=0; i<internal.length; i += 2) {
            internal[i] *= xScale;
        }
        verifyTransform(coordinates, internal);
    }

    /**
     * Compares the projection of a few points against expected values computed from intermediate values
     * published by Snyder. As Snyder tables in chapter 28 introduce only the values for <var>n</var>,
     * <var>F₁</var> and <var>ρ</var> coefficients, the test was realized by checking these coefficients
     * in debugger and by extracting the computed results of the projection.
     *
     * @param  λ0           central meridian.
     * @param  n            cone factor <var>n</var>.
     * @param  coordinates  the points to transform.
     * @param  internal     the expected intermediate transformation results.
     * @throws TransformException if the transformation failed.
     */
    private void verifyConicInternal(final double λ0, final double n, final double[] coordinates, final double[] internal)
            throws TransformException
    {
        for (int i=0; i<internal.length; i += 2) {
            internal[i] *= sin(n * toRadians(coordinates[i] - λ0));
        }
        verifyTransform(coordinates, internal);
    }

    /**
     * Tests the projection of a few points on a sphere.
     * Test based on the sample coordinates for several of the Satellite-Tracking projections
     * shown in table 38 of <cite>Map Projections - A working Manual</cite>.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testCylindricalInternal() throws FactoryException, TransformException {
        tolerance = NormalizedProjection.ANGULAR_TOLERANCE;
        tolerance = Formulas.LINEAR_TOLERANCE;                  // TODO
        /*
         * φ₁ = 0°
         */
        createForLandsat(0, 0, 0, 0);
        verifyCylindricInternal(0.017453,
                new double[] {                  // (λ,φ) coordinates in degrees to project.
                       0,   0,
                      10,   0,
                     -10,  10,
                      60,  40,
                      80,  70,
                    -120,  80.908               // Tracking limit.
                },
                new double[] {                  // Expected (x,y) results on a unit sphere.
                       0,   0,
                      10,   0,
                     -10,   0.17579,
                      60,   0.79741,
                      80,   2.34465,
                    -120,   7.23571             // Projection of tracking limit.
                });
        /*
         * φ₁ = -30°
         */
        createForLandsat(0, 0, -30, 30);
        verifyCylindricInternal(0.015115,
                new double[] {
                       0,   0,
                      10,   0,
                     -10,  10,
                      60,  40,
                      80,  70,
                    -120,  80.908
                },
                new double[] {
                       0,  0,
                      10,  0,
                     -10,  0.14239,
                      60,  0.64591,
                      80,  1.89918,
                    -120,  5.86095
                });
        /*
         * φ₁ = 45°
         */
        createForLandsat(0, 0, 45, -45);
        verifyCylindricInternal(0.012341,
                new double[] {
                       0,   0,
                      10,   0,
                     -10,  10,
                      60,  40,
                      80,  70,
                    -120,  80.908
                },
                new double[] {
                       0, 0,
                      10,  0,
                     -10,  0.10281,
                      60,  0.46636,
                      80,  1.37124,
                    -120,  4.23171
                });
    }

    /**
     * Tests the projection of a few points on a sphere.
     * Test based on the sample coordinates for several of the Satellite-Tracking projections
     * shown in table 39 of <cite>Map Projections - A working Manual</cite>, page 238.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testConicInternal() throws FactoryException, TransformException {
        tolerance = NormalizedProjection.ANGULAR_TOLERANCE;
        tolerance = Formulas.LINEAR_TOLERANCE;                  // TODO
        /*
         * φ₁ = 30° ; φ₂ = 60°
         */
        createForLandsat(-90, 0, 30, 60);
        verifyConicInternal(-90, 0.49073,
                new double[] {                  // (λ,φ) coordinates in degrees to project.
                       0, -10,
                       0,   0,
                       0,  10,
                       0,  70,
                    -120,  80.908               // Tracking limit.
                },
                new double[] {                  // Expected (x,y) results on a unit sphere.
                    2.67991,  0.46093,
                    2.38332,  0.67369,
                    2.14662,  0.84348,
                    0.98470,  1.67697,
                    0.50439,  1.89549           // Projection of tracking limit.
                });
        /*
         * φ₁ = 45° ; φ₂ = 70°
         */
        createForLandsat(-90, 0, 45, 70);
        verifyConicInternal(-90, 0.69478,
                new double[] {
                    0, -10,
                    0,   0,
                    0,  10,
                    0,  70,
                 -120,  80.908                  // Tracking limit.
                },
                new double[] {
                    2.92503,  0.90110,
                    2.25035,  1.21232,
                    1.82978,  1.40632,
                    0.57297,  1.98605,
                    0.28663,  1.982485          // Projection of tracking limit.
                });
        /*
         * φ₁ = 45° ; φ₂ = 80.908° (the tracking limit).
         */
        createForLandsat(-90, 0, 45, 80.908);
        verifyConicInternal(-90, 0.88475,
                new double[] {
                       0, -10,
                       0,   0,
                       0,  10,
                       0,  70,
                    -120,  80.908
                },
                new double[] {
                    4.79153,  1.80001,
                    2.66270,  2.18329,
                    1.84527,  2.33046,
                    0.40484,  2.58980,
                    0.21642,  2.46908
                });
    }

    /**
     * Tests the derivatives at a few points for cylindrical case. This method compares the derivatives computed
     * by the projection with an estimation of derivatives computed by the finite differences method.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testCylindricalDerivative() throws FactoryException, TransformException {
        createForLandsat(-90, 0, 30, -30);
        final double delta = (1.0 / 60) / 1852;                 // Approximately 1 metre.
        derivativeDeltas = new double[] {delta, delta};
        tolerance = NormalizedProjection.ANGULAR_TOLERANCE;
        tolerance = Formulas.LINEAR_TOLERANCE / 100;            // TODO
        verifyDerivative( -75, 40);
        verifyDerivative(-100,  3);
        verifyDerivative( -56, 50);
        verifyDerivative( -20, 47);
    }

    /**
     * Tests the derivatives at a few points for conic case. This method compares the derivatives computed
     * by the projection with an estimation of derivatives computed by the finite differences method.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testConicDerivative() throws FactoryException, TransformException {
        createForLandsat(-90, 30, 45, 70);
        final double delta = (1.0 / 60) / 1852;                 // Approximately 1 metre.
        derivativeDeltas = new double[] {delta, delta};
        tolerance = NormalizedProjection.ANGULAR_TOLERANCE;
        tolerance = Formulas.LINEAR_TOLERANCE/100 ;             // TODO
        verifyDerivative( -75, 40);
        verifyDerivative(-100,  3);
        verifyDerivative( -56, 50);
        verifyDerivative( -20, 47);
    }
}
