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
import java.util.Collections;
import org.apache.sis.internal.referencing.NilReferencingObject;
import org.apache.sis.internal.referencing.provider.SatelliteTracking;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.datum.DefaultEllipsoid;
import org.apache.sis.referencing.operation.transform.MathTransformFactoryMock;
import org.apache.sis.test.DependsOn;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * Tests coordiantes computed by applying a conic satellite-tracking projection
 * with {@link ConicSatelliteTracking}.
 *
 * @author Matthieu Bastianelli (Geomatys)
 * @version 1.0
 * @since 1.0
 * @module
 */
@DependsOn(ConformalProjectionTest.class)
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
     * @param φ0 : lattitude_of_origin : latitude Crossing the central meridian
     * at the desired origin of rectangular coordinates (null or NaN for
     * cylindrical satellite tracking projection.)
     * @return
     */
    void createProjection(final double i,
            final double orbitalT, final double ascendingNodeT,
            final double λ0,       final double φ1, 
            final double φ2,       final double φ0) 
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
        }else{
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
     * @throws FactoryException if an error occurred while creating the map
     * projection.
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testTransform() throws FactoryException, TransformException {
        tolerance = 1E-5;
        createProjection(
                99.092,  //satellite_orbit_inclination
                103.267, //satellite_orbital_period
                1440.0,  //ascending_node_period
                -90,     //central_meridian
                45,      //standard_parallel_1
                70,      //standard_parallel_2
                30       //lattitude_of_origin
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

}
