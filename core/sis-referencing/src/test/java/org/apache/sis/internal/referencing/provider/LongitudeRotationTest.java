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

import javax.measure.unit.NonSI;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.MetadataAssert.*;


/**
 * Tests the {@link LongitudeRotation} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@DependsOn(AffineTest.class)
public final strictfp class LongitudeRotationTest extends TestCase {
    /**
     * Tests {@code LongitudeRotation.createMathTransform(…)}.
     */
    @Test
    public void testCreateMathTransform() {
        final LongitudeRotation provider = new LongitudeRotation();
        ParameterValueGroup p = provider.getParameters().createValue();
        p.parameter("Longitude offset").setValue(2.5969213, NonSI.GRADE);   // Paris meridian
        final MathTransform mt = provider.createMathTransform(null, p);
        /*
         * Verify the full matrix. Note that the longitude offset is expected to be in degrees.
         * This conversion from grad to degrees is specific to Apache SIS and may be revised in
         * future version. See org.apache.sis.referencing.operation package javadoc.
         */
        assertInstanceOf("Shall be an affine transform.", LinearTransform.class, mt);
        assertMatrixEquals("Expected a longitude rotation",
                new Matrix3(1, 0, 2.33722917,
                            0, 1, 0,
                            0, 0, 1), ((LinearTransform) mt).getMatrix(), 1E-16);
    }

    /**
     * Tests WKT formatting. Note that we do not expect a {@code Param_MT[“Longitude rotation”, …]} text
     * since we want to make clear that Apache SIS implements longitude rotation by an affine transform.
     */
    @Test
    @DependsOnMethod("testCreateMathTransform")
    public void testWKT() {
        final LongitudeRotation provider = new LongitudeRotation();
        final ParameterValueGroup p = provider.getParameters().createValue();
        p.parameter("Longitude offset").setValue(2.5969213, NonSI.GRADE);
        assertWktEquals(
                "PARAM_MT[“Affine parametric transformation”,\n" +
                "  PARAMETER[“A2”, 2.33722917, ID[“EPSG”, 8625]]]", provider.createMathTransform(null, p));
    }
}
