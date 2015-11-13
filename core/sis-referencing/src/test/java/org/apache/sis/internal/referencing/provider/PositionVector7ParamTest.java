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
package org.apache.sis.internal.referencing.provider;

import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransformTestCase;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static java.lang.StrictMath.toRadians;
import static org.junit.Assert.*;


/**
 * Tests {@link PositionVector7Param} and {@link PositionVector7Param3D}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn({
    GeocentricTranslationTest.class
})
public final strictfp class PositionVector7ParamTest extends MathTransformTestCase {
    /**
     * Returns the sample point for a step in the example given by the EPSG guidance note.
     *
     * <blockquote><b>Source:</b>
     * §2.4.3.3 <cite>Three-parameter geocentric translations</cite> in
     * IOGP Publication 373-7-2 – Geomatics Guidance Note number 7, part 2 – April 2015
     * </blockquote>
     *
     * @param  step The step as a value from 1 to 4 inclusive.
     * @return The sample point at the given step.
     */
    static double[] samplePoint(final int step) {
        switch (step) {
            case 1: return new double[] {
                        0,
                        0,
                        0
                    };
            case 2: return new double[] {
                        3657660.66,                 // X: Toward prime meridian
                         255768.55,                 // Y: Toward 90° east
                        5201382.11                  // Z: Toward north pole
                    };
            case 3: return new double[] {
                        3657660.78,                 // X: Toward prime meridian
                         255778.43,                 // Y: Toward 90° east
                        5201387.75                  // Z: Toward north pole
                    };
            case 4: return new double[] {           // Anti-regression values (NOT provided by EPSG)
                         0.5540 / 60 / 60,          // λ: Longitude
                         0.1465 / 60 / 60,          // φ: Latitude
                        -0.60                       // h: Height
                    };
            default: throw new AssertionError(step);
        }
    }

    /**
     * Creates the transformation from WGS 72 to WGS 84.
     *
     * @param method The operation method to use.
     * @param rotationSign {@code +1} for Position Vector, or -1 for Frame Rotation.
     */
    static MathTransform createTransform(final GeocentricAffine method, final int rotationSign) throws FactoryException {
        final ParameterValueGroup values = method.getParameters().createValue();
        values.parameter("Z-axis translation").setValue(+4.5  );                    // metres
        values.parameter("Z-axis rotation")   .setValue(+0.554 * rotationSign);     // arc-seconds
        values.parameter("Scale difference")  .setValue(+0.219);                    // parts per million
        if (method instanceof GeocentricAffineBetweenGeographic) {
            GeocentricTranslationTest.setEllipsoids(values, CommonCRS.WGS72.ellipsoid(), CommonCRS.WGS84.ellipsoid());
        }
        return method.createMathTransform(DefaultFactories.forBuildin(MathTransformFactory.class), values);
    }

    /**
     * Creates the transformation from WGS 72 to WGS 84.
     */
    private void createTransform(final GeocentricAffine method) throws FactoryException {
        transform = createTransform(method, +1);
        validate();
    }

    /**
     * Tests <cite>"Position Vector transformation (geocentric domain)"</cite> (EPSG:1033)
     * with a sample point from WGS 72 to WGS 84 (EPSG Dataset transformation code 1238).
     *
     * @throws FactoryException if an error occurred while creating the transform.
     * @throws TransformException if transformation of a point failed.
     */
    @Test
    public void testGeocentricDomain() throws FactoryException, TransformException {
        createTransform(new PositionVector7Param());
        tolerance = 0.01;  // Precision for (X,Y,Z) values given by EPSG
        derivativeDeltas = new double[] {100, 100, 100};    // In metres
        assertTrue(transform instanceof LinearTransform);
        verifyTransform(samplePoint(2), samplePoint(3));
    }

    /**
     * Tests <cite>"Position Vector transformation (geog3D domain)"</cite> (EPSG:1037).
     *
     * @throws FactoryException if an error occurred while creating the transform.
     * @throws TransformException if transformation of a point failed.
     */
    @Test
    @DependsOnMethod("testGeocentricDomain")
    public void testGeographicDomain() throws FactoryException, TransformException {
        final double delta = toRadians(100.0 / 60) / 1852;      // Approximatively 100 metres
        derivativeDeltas = new double[] {delta, delta, 100};    // (Δλ, Δφ, Δh)
        tolerance  = Formulas.ANGULAR_TOLERANCE;
        zTolerance = Formulas.LINEAR_TOLERANCE;
        zDimension = new int[] {2};
        tolerance  = Formulas.LINEAR_TOLERANCE;                 // Other SIS branches use a stricter threshold.
        createTransform(new PositionVector7Param3D());
        assertFalse(transform instanceof LinearTransform);
        verifyTransform(samplePoint(1), samplePoint(4));
    }
}
