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

import static java.lang.Math.sin;
import java.util.Collections;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.referencing.NilReferencingObject;
import org.apache.sis.internal.referencing.provider.SatelliteTracking;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.datum.DefaultEllipsoid;
import org.apache.sis.referencing.operation.transform.MathTransformFactoryMock;
import org.apache.sis.test.DependsOn;
import org.junit.Test;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

import static java.lang.StrictMath.toRadians;
import static org.junit.Assert.assertTrue;

/**
 * Tests coordiantes computed by applying a conic satellite-tracking projection
 * with {@link ConicSatelliteTracking}.
 *
 * @author Matthieu Bastianelli (Geomatys)
 * @version 1.0
 * @since 1.0
 * @module
 */
public class ConicSatelliteTrackingTest extends MapProjectionTestCase {

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
    void createProjection(final double i,
            final double orbitalT, final double ascendingNodeT,
            final double λ0, final double φ1,
            final double φ2, final double φ0)
            throws FactoryException {

        final SatelliteTracking provider = new SatelliteTracking();
        final ParameterValueGroup values = provider.getParameters().createValue();
        final DefaultEllipsoid sphere = DefaultEllipsoid.createEllipsoid(
                Collections.singletonMap(DefaultEllipsoid.NAME_KEY, NilReferencingObject.UNNAMED),
                1, 1, Units.METRE);

        values.parameter("semi_major").setValue(sphere.getSemiMajorAxis());
        values.parameter("semi_minor").setValue(sphere.getSemiMinorAxis());
        values.parameter("satellite_orbit_inclination").setValue(i);
        values.parameter("satellite_orbital_period").setValue(orbitalT);
        values.parameter("ascending_node_period").setValue(ascendingNodeT);
        values.parameter("central_meridian").setValue(λ0);
        values.parameter("standard_parallel_1").setValue(φ1);

        if (!Double.isNaN(φ2)) {
            values.parameter("standard_parallel_2").setValue(φ2);
        } else {
            values.parameter("standard_parallel_2").setValue(-φ1); //Cylindrical case
        }
        if (!Double.isNaN(φ0)) {
            values.parameter("latitude_of_origin").setValue(φ0);
        }

        transform = new MathTransformFactoryMock(provider).createParameterizedTransform(values);
        validate();
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
    public void testTransform() throws FactoryException, TransformException {
        tolerance = 1E-5;
        createProjection(
                99.092, //satellite_orbit_inclination
                103.267, //satellite_orbital_period
                1440.0, //ascending_node_period
                -90, //central_meridian
                45, //standard_parallel_1
                70, //standard_parallel_2
                30 //latitude_of_origin
        );
        assertTrue(isInverseTransformSupported);
        verifyTransform(
                new double[]{ // (λ,φ) coordinates in degrees to project.
                    -75, 40
                },
                new double[]{ // Expected (x,y) results in metres.
                    0.2001910, 0.2121685
                });
    }

    /**
     * Tests the projection of a few points on a sphere.
     *
     * Test based on the sample coordinates for several of the
     * Satellite-Tracking Projections shown in table 39 from
     * <cite> Map Projections - A working Manual</cite>
     *
     * As this table only introduce the values for n, F1 and ρ coefficients, the
     * test was realized by checking these coefficients in debugger mode and by
     * extracting the computed results of the projection. Thus this method
     * should be used as a non-regression test.
     *
     * @throws FactoryException if an error occurred while creating the map
     * projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testSampleCoordinates() throws FactoryException, TransformException {

        //Following tests don't pass with the former tolerance.
        tolerance = Formulas.LINEAR_TOLERANCE;

        //----------------------------------------------------------------------
        // φ1 = 30° ; φ2 = 60°
        //---------------------
        createProjection(
                99.092, //satellite_orbit_inclination
                103.267, //satellite_orbital_period
                1440.0, //ascending_node_period
                -90, //central_meridian
                30, //standard_parallel_1
                60, //standard_parallel_2
                0 //latitude_of_origin
        );

        double n = 0.49073;

        verifyTransform(
                new double[]{ // (λ,φ) coordinates in degrees to project.
                       0, -10,
                       0, 0,
                       0, 10,
                       0, 70,
                    -120, 80.908  //Tracking limit
                },
                new double[]{ // Expected (x,y) results in metres.
                    2.67991 * sin(n * (toRadians(   0 - -90))),    0.46093,
                    2.38332 * sin(n * (toRadians(   0 - -90))),    0.67369,
                    2.14662 * sin(n * (toRadians(   0 - -90))),    0.84348,
                    0.98470 * sin(n * (toRadians(   0 - -90))),    1.67697,
                    0.50439 * sin(n * (toRadians(-120 - -90))),    1.89549 //Tracking limit
                });

        //----------------------------------------------------------------------
        // φ1 = 45° ; φ2 = 70°
        //---------------------
        createProjection(
                99.092, //satellite_orbit_inclination
                103.267, //satellite_orbital_period
                1440.0, //ascending_node_period
                -90, //central_meridian
                45, //standard_parallel_1
                70, //standard_parallel_2
                0 //latitude_of_origin
        );

        n = 0.69478;

        verifyTransform(
                new double[]{ // (λ,φ) coordinates in degrees to project.
                    0, -10,
                    0, 0,
                    0, 10,
                    0, 70,
                    -120, 80.908 //Tracking limit
                },
                new double[]{ // Expected (x,y) results in metres.
                    2.92503 * sin(n * (toRadians(   0 - -90))), 0.90110,
                    2.25035 * sin(n * (toRadians(   0 - -90))), 1.21232,
                    1.82978 * sin(n * (toRadians(   0 - -90))), 1.40632,
                    0.57297 * sin(n * (toRadians(   0 - -90))), 1.98605,
                    0.28663 * sin(n * (toRadians(-120 - -90))), 1.982485 //Tracking limit
                });
        //----------------------------------------------------------------------
        // φ1 = 45° ; φ2 = 70°
        //---------------------
        createProjection(
                99.092, //satellite_orbit_inclination
                103.267, //satellite_orbital_period
                1440.0, //ascending_node_period
                -90, //central_meridian
                45, //standard_parallel_1
                80.908, //standard_parallel_2
                0 //latitude_of_origin
        );

        n = 0.88475;

        verifyTransform(
                new double[]{ // (λ,φ) coordinates in degrees to project.
                       0, -10,
                       0,  0,
                       0, 10,
                       0, 70,
                    -120, 80.908 //Tracking limit
                },
                new double[]{ // Expected (x,y) results in metres.
                    4.79153 * sin(n * (toRadians(   0 - -90))), 1.80001,
                    2.66270 * sin(n * (toRadians(   0 - -90))), 2.18329,
                    1.84527 * sin(n * (toRadians(   0 - -90))), 2.33046,
                    0.40484 * sin(n * (toRadians(   0 - -90))), 2.58980,
                    0.21642 * sin(n * (toRadians(-120 - -90))), 2.46908 //Tracking limit
                });
    }
}
