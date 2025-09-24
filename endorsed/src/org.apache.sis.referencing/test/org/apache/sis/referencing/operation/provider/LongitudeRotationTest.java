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
package org.apache.sis.referencing.operation.provider;

import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.measure.Units;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.referencing.Assertions.assertWktEquals;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import static org.opengis.test.Assertions.assertMatrixEquals;


/**
 * Tests the {@link LongitudeRotation} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class LongitudeRotationTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public LongitudeRotationTest() {
    }

    /**
     * Tests {@code LongitudeRotation.createMathTransform(…)}.
     *
     * @throws FactoryException if an error occurred while creating the transform to test.
     */
    @Test
    public void testCreateMathTransform() throws FactoryException {
        final LongitudeRotation provider = new LongitudeRotation();
        ParameterValueGroup p = provider.getParameters().createValue();
        p.parameter("Longitude offset").setValue(2.5969213, Units.GRAD);   // Paris meridian
        final MathTransform mt = provider.createMathTransform(null, p);
        /*
         * Verify the full matrix. Note that the longitude offset is expected to be in degrees.
         * This conversion from grad to degrees is specific to Apache SIS and may be revised in
         * future version. See org.apache.sis.referencing.operation package javadoc.
         */
        var linear = assertInstanceOf(LinearTransform.class, mt, "Shall be an affine transform.");
        assertMatrixEquals(new Matrix3(1, 0, 2.33722917,
                                       0, 1, 0,
                                       0, 0, 1),
                linear.getMatrix(),
                1E-16, "Expected a longitude rotation");
    }

    /**
     * Tests WKT formatting. Note that we do not expect a {@code Param_MT[“Longitude rotation”, …]} text
     * since we want to make clear that Apache SIS implements longitude rotation by an affine transform.
     *
     * @throws FactoryException if an error occurred while creating the transform to test.
     */
    @Test
    public void testWKT() throws FactoryException {
        final LongitudeRotation provider = new LongitudeRotation();
        final ParameterValueGroup p = provider.getParameters().createValue();
        p.parameter("Longitude offset").setValue(2.5969213, Units.GRAD);
        assertWktEquals(Convention.WKT2,
                "PARAM_MT[“Affine”,\n" +
                "  PARAMETER[“num_row”, 3],\n"  +
                "  PARAMETER[“num_col”, 3],\n"  +
                "  PARAMETER[“elt_0_2”, 2.33722917]]", provider.createMathTransform(null, p));
    }
}
