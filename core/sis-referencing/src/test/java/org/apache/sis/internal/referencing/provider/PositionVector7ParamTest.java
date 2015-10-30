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
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransformTestCase;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

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
     * @param  step The step as a value from 2 to 3 inclusive.
     * @return The sample point at the given step.
     */
    static double[] samplePoint(final int step) {
        switch (step) {
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
            default: throw new AssertionError(step);
        }
    }

    /**
     * Creates the transform for EPSG:1238.
     */
    private void createTransform() throws FactoryException {
        final PositionVector7Param method = new PositionVector7Param();
        final ParameterValueGroup values = method.getParameters().createValue();
        values.parameter("Z-axis translation").setValue(+4.5  );    // metres
        values.parameter("Z-axis rotation")   .setValue(+0.554);    // arc-seconds
        values.parameter("Scale difference")  .setValue(+0.219);    // parts per million
        transform = method.createMathTransform(DefaultFactories.forBuildin(MathTransformFactory.class), values);
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
        createTransform();
        tolerance = 0.01;  // Precision for (X,Y,Z) values given by EPSG
        derivativeDeltas = new double[] {100, 100, 100};    // In metres
        assertTrue(transform instanceof LinearTransform);
        verifyTransform(samplePoint(2), samplePoint(3));
        validate();
    }
}
