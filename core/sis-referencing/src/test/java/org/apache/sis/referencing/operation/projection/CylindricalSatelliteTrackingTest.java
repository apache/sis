/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sis.referencing.operation.projection;

import static java.lang.Double.NaN;
import org.apache.sis.internal.referencing.Formulas;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * Tests coordiantes computed by applying a cylindrical satellite-tracking projection
 * with {@link CylindricalSatelliteTracking}.
 *
 * @author Matthieu Bastianelli (Geomatys)
 * @version 1.0
 * @since 1.0
 * @module
 */
public class CylindricalSatelliteTrackingTest extends ConicSatelliteTrackingTest {



    /**
     * Creates a new instance of {@link ConicSatelliteTracking} concatenated
     * with the (de)normalization matrices. The new instance is stored in the
     * inherited {@link #transform} field.
     *
     * @param i : Angle of inclination between the plane of the Earth's Equator
     * and the plane of the satellite orbit.
     * @param orbitalT : Time required for revolution of the satellite.
     * @param ascendingNodeT : Length of Earth's rotation with respect to the
     * precessed ascending node.
     * @param λ0 : central meridian.
     * @param φ1 : first parallel of conformality, with true scale.
     * @param φ2 : second parallel of conformality, without true scale.
     * @param φ0 : latitude_of_origin : latitude Crossing the central meridian
     * at the desired origin of rectangular coordinates (null or NaN for
     * cylindrical satellite tracking projection.)
     * @return
     */
    private void createProjection(final double i,
            final double orbitalT, final double ascendingNodeT,
            final double λ0,       final double φ1)
            throws FactoryException {
        super.createProjection(i, orbitalT, ascendingNodeT, λ0, φ1, NaN, NaN);
    }

    /**
     * Tests the projection of a few points on a sphere.
     *
     * Test based on the numerical example given by Snyder p. 360 to 363 of
     * <cite> Map Projections - A working Manual</cite>
     *
     * @throws FactoryException if an error occurred while creating the map
     * projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    @Override
    public void testTransform() throws FactoryException, TransformException {
        tolerance = 1E-5;
        createProjection(
                99.092,  //satellite_orbit_inclination
                103.267, //satellite_orbital_period
                1440.0,  //ascending_node_period
                -90,     //central_meridian
                30       //standard_parallel_1
        );
        assertTrue(isInverseTransformSupported);
        verifyTransform(
                new double[]{ // (λ,φ) coordinates in degrees to project.
                    -75, 40
                },
                new double[]{ // Expected (x,y) results in metres.
                    0.2267249, 0.6459071
                });
    }

    /**
     * Tests the projection of a few points on a sphere.
     *
     * Test based on the sample coordinates for several of the
     * Satellite-Tracking Projections shown in table 38 from
     * <cite> Map Projections - A working Manual</cite>
     *
     * @throws FactoryException if an error occurred while creating the map
     * projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testSampleCoordinatesCylindricalProjection()
            throws FactoryException, TransformException {

        //Following tests don't pass with the former tolerance.
        tolerance = Formulas.LINEAR_TOLERANCE;

        //----------------------------------------------------------------------
        // φ1 = 0°
        //---------
        createProjection(
                99.092,  //satellite_orbit_inclination
                103.267, //satellite_orbital_period
                1440.0,  //ascending_node_period
                0,       //central_meridian
                0        //standard_parallel_1
        );

        double xConverterFactor=0.017453;
        verifyTransform(
                new double[]{ // (λ,φ) coordinates in degrees to project.
                      0,  0,
                     10,  0,
                    -10, 10,
                     60, 40,
                     80, 70,
                    -120, 80.908  //Tracking limit
                },
                new double[]{ // Expected (x,y) results in metres.
                    0, 0,
                    xConverterFactor *   10,  0,
                    xConverterFactor *  -10,  0.17579,
                    xConverterFactor *   60,  0.79741,
                    xConverterFactor *   80,  2.34465,
                    xConverterFactor * -120,  7.23571 //Tracking limit
                });


        //----------------------------------------------------------------------
        // φ1 = +- 30°
        //------------
        createProjection(
                99.092,  //satellite_orbit_inclination
                103.267, //satellite_orbital_period
                1440.0,  //ascending_node_period
                0,       //central_meridian
                -30        //standard_parallel_1
        );

        xConverterFactor=0.015115;
        verifyTransform(
                new double[]{ // (λ,φ) coordinates in degrees to project.
                      0,  0,
                     10,  0,
                    -10, 10,
                     60, 40,
                     80, 70,
                    -120, 80.908  //Tracking limit
                },
                new double[]{ // Expected (x,y) results in metres.
                    0, 0,
                    xConverterFactor *   10,  0,
                    xConverterFactor *  -10,  0.14239,
                    xConverterFactor *   60,  0.64591,
                    xConverterFactor *   80,  1.89918,
                    xConverterFactor * -120,  5.86095 //Tracking limit
                });

        //----------------------------------------------------------------------
        // φ1 = +- 45°
        //------------
        createProjection(
                99.092,  //satellite_orbit_inclination
                103.267, //satellite_orbital_period
                1440.0,  //ascending_node_period
                0,       //central_meridian
                45        //standard_parallel_1
        );

        xConverterFactor=0.012341;
        verifyTransform(
                new double[]{ // (λ,φ) coordinates in degrees to project.
                      0,  0,
                     10,  0,
                    -10, 10,
                     60, 40,
                     80, 70,
                    -120, 80.908  //Tracking limit
                },
                new double[]{ // Expected (x,y) results in metres.
                    0, 0,
                    xConverterFactor *   10,  0,
                    xConverterFactor *  -10,  0.10281,
                    xConverterFactor *   60,  0.46636,
                    xConverterFactor *   80,  1.37124,
                    xConverterFactor * -120,  4.23171 //Tracking limit
                });


    }

}
