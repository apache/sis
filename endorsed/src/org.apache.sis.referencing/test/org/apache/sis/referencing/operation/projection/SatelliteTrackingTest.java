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

import java.util.Map;
import static java.lang.StrictMath.sin;
import static java.lang.StrictMath.toRadians;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.internal.shared.NilReferencingObject;
import org.apache.sis.referencing.operation.provider.SatelliteTracking;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.datum.DefaultEllipsoid;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.referencing.operation.transform.MathTransformFactoryMock;


/**
 * Tests coordinates computed by applying a satellite-tracking projection with {@link SatelliteTracking}.
 *
 * @author  Matthieu Bastianelli (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class SatelliteTrackingTest extends MapProjectionTestCase {
    /**
     * Creates a new test case.
     */
    public SatelliteTrackingTest() {
    }

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
        final DefaultEllipsoid    sphere = DefaultEllipsoid.createEllipsoid(
                Map.of(DefaultEllipsoid.NAME_KEY, NilReferencingObject.UNNAMED), 1, 1, Units.METRE);

        values.parameter("semi_major")                 .setValue(sphere.getSemiMajorAxis());
        values.parameter("semi_minor")                 .setValue(sphere.getSemiMinorAxis());
        values.parameter("central_meridian")           .setValue(λ0);
        values.parameter("latitude_of_origin")         .setValue(φ0);
        values.parameter("standard_parallel_1")        .setValue(φ1);
        values.parameter("standard_parallel_2")        .setValue(φ2);
        values.parameter("satellite_orbit_inclination").setValue(  99.092);
        values.parameter("satellite_orbital_period")   .setValue( 103.267, Units.MINUTE);
        values.parameter("ascending_node_period")      .setValue(1440.0,   Units.MINUTE);
        transform = provider.createMathTransform(new MathTransformFactoryMock(provider), values);
        validate();
        /*
         * Assuming that tolerance has been set to the number of fraction digits published in Snyder tables,
         * relax the tolerance during inverse transforms for taking in account the increase in magnitude of
         * coordinate values. The transform results are between 0 and 1, while the inverse transform results
         * are between -90° and 90°, which is an increase in magnitude close to ×100.
         */
        toleranceModifier = (tolerance, coordinate, mode) -> {
            switch (mode) {
                case INVERSE_TRANSFORM: {
                    for (int i=0; i<tolerance.length; i++) {
                        tolerance[i] *= 50;
                    }
                    break;
                }
            }
        };
    }

    /**
     * Tests the projection of a few points using spherical formulas.
     * Test based on the numerical example given by Snyder pages 360 to 363
     * of <cite>Map Projections - A working Manual</cite>.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testCylindricalTransform() throws FactoryException, TransformException {
        tolerance = 1E-7;   // Number of digits in the output values provided by Snyder.
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
     * Tests the projection of a few points using conic formulas.
     * Test based on the numerical example given by Snyder pages 360 to 363
     * of <cite>Map Projections - A working Manual</cite>.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testConicTransform() throws FactoryException, TransformException {
        tolerance = 1E-7;   // Number of digits in the output values provided by Snyder.
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
     * published by Snyder. Snyder tables in chapter 28 do not give directly the (x,y) values. Instead
     * the tables give some intermediate values like <var>F₁</var>, which are verified in debugger.
     * This method converts intermediate values to final coordinate values to compare.
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
     * published by Snyder. Snyder tables in chapter 28 do not give directly the (x,y) values. Instead
     * the tables give some intermediate values like <var>F₁</var> and <var>n</var>, which are verified
     * in debugger. This method converts intermediate values to final coordinate values to compare.
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
     * Tests the projection of a few points using cylindrical formulas.
     * Test based on the sample coordinates for several of the Satellite-Tracking projections
     * shown in table 38 of <cite>Map Projections - A working Manual</cite>.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testCylindricalInternal() throws FactoryException, TransformException {
        /*
         * First group of 3 columns in Snyder table 38, for φ₁ = 0°.
         * Snyder gives the following values, which can be verified in a debugger:
         *
         *     F₁  =  13.09724°         can be verified with toDegrees(atan(1/cotF))
         *     x   =  0.017453⋅λ°       can be verified with toRadians(cosφ1)
         *
         * Accuracy is set to the number of fraction digits published by Snyder (5 digits)
         * with tolerance relaxed on the last digit. The verifyCylindricInternal(…) first
         * argument is the factor in above x equation, with additional digits obtained by
         * inspecting the value in a debugging session.
         */
        tolerance = 4E-5;
        createForLandsat(0, 0, 0, 0);
        verifyCylindricInternal(0.017453292519943295,   // See x in above comment.
                new double[] {                          // (λ,φ) coordinates in degrees to project.
                       0,   0,
                      10,   0,
                     -10,  10,
                      60,  40,
                      80,  70,
                    -120,  80.908                       // Tracking limit.
                },
                new double[] {                          // Expected (x,y) results on a unit sphere.
                       0,   0,
                      10,   0,
                     -10,   0.17579,
                      60,   0.79741,
                      80,   2.34465,
                    -120,   7.23571                     // Projection of tracking limit.
                });
        /*
         * Second group of 3 columns for φ₁ = -30°.
         *
         *     F₁  =  13.96868°
         *     x   =  0.015115⋅λ°
         */
        createForLandsat(0, 0, -30, 30);
        verifyCylindricInternal(0.015114994701951816,   // See x in above comment.
                new double[] {
                       0,   0,
                      10,   0,
                     -10,  10,
                      60,  40,
                      80,  70,
                    -120,  80.908                       // Tracking limit.
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
         * Third group of 3 columns for φ₁ = 45°
         *
         *     F₁  =  15.71115°
         *     x   =  0.012341⋅λ°
         */
        createForLandsat(0, 0, 45, -45);
        verifyCylindricInternal(0.012341341494884351,   // See x in above comment.
                new double[] {
                       0,   0,
                      10,   0,
                     -10,  10,
                      60,  40,
                      80,  70,
                    -120,  80.908                       // Tracking limit.
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
     * Tests the projection of a few points using conic formulas.
     * Test based on the sample coordinates for several of the Satellite-Tracking projections
     * shown in table 39 of <cite>Map Projections - A working Manual</cite>, page 238.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testConicInternal() throws FactoryException, TransformException {
        /*
         * First group of 3 columns in Snyder table 38, for φ₁ = 30° and φ₂ = 60°.
         * Snyder gives the following values, which can be verified in a debugger:
         *
         *     F₁  =  13.96868°         can be verified with toDegrees(F1)
         *     n   =   0.49073
         *
         * Accuracy is set to the number of fraction digits published by Snyder (5 digits)
         * with tolerance relaxed on the last digit. The verifyCylindricInternal(…) first
         * argument is the factor in above x equation, with additional digits obtained by
         * inspecting the value in a debugging session.
         */
        tolerance = 3E-5;
        createForLandsat(-90, 0, 30, 60);
        verifyConicInternal(-90, 0.4907267554554259,    // See n in above comment.
                new double[] {                          // (λ,φ) coordinates in degrees to project.
                       0, -10,
                       0,   0,
                       0,  10,
                       0,  70,
                    -120,  80.908                       // Tracking limit.
                },
                new double[] {                          // Expected (x,y) results on a unit sphere.
                    2.67991,  0.46093,
                    2.38332,  0.67369,
                    2.14662,  0.84348,
                    0.98470,  1.67697,
                    0.50439,  1.89549                   // Projection of tracking limit.
                });
        /*
         * Second group of 3 columns for φ₁ = 45° and φ₂ = 70°.
         *
         *     F₁  =  15.71115°
         *     n   =  0.69478
         */
        createForLandsat(-90, 0, 45, 70);
        verifyConicInternal(-90, 0.6947829166657693,    // See n in above comment.
                new double[] {
                    0, -10,
                    0,   0,
                    0,  10,
                    0,  70,
                 -120,  80.908                          // Tracking limit.
                },
                new double[] {
                    2.92503,  0.90110,
                    2.25035,  1.21232,
                    1.82978,  1.40632,
                    0.57297,  1.98605,
                    0.28663,  1.982485
                });
        /*
         * Second group of 3 columns for φ₁ = 45° and φ₂ = 80.908° (the tracking limit).
         *
         *     F₁  =  15.71115°
         *     n   =  0.88475
         */
        createForLandsat(-90, 0, 45, 80.908);
        verifyConicInternal(-90, 0.8847514352390218,    // See n in above comment.
                new double[] {
                       0, -10,
                       0,   0,
                       0,  10,
                       0,  70,
                    -120,  80.908                       // Tracking limit.
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
        tolerance = 1E-4;
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
        tolerance = 1E-4;
        verifyDerivative( -75, 40);
        verifyDerivative(-100,  3);
        verifyDerivative( -56, 50);
        verifyDerivative( -20, 47);
    }
}
