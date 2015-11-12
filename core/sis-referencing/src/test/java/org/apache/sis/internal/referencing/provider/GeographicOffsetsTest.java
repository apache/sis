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
import org.opengis.referencing.operation.TransformException;
import org.opengis.test.referencing.TransformTestCase;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.test.DependsOn;
import org.junit.Test;


/**
 * Tests the {@link GeographicOffsets} and {@link GeographicOffsets3D} classes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn(AffineTest.class)
public final strictfp class GeographicOffsetsTest extends TransformTestCase {
    /**
     * Tests {@code GeographicOffsets.createMathTransform(…)}.
     * This test uses the sample point given in §2.4.4.3 of EPSG guide (April 2015).
     *
     * @throws FactoryException should never happen.
     * @throws TransformException should never happen.
     */
    @Test
    public void testCreateMathTransform2D() throws FactoryException, TransformException {
        testCreateMathTransform(new GeographicOffsets());
    }

    /**
     * Tests {@code GeographicOffsets3D.createMathTransform(…)}.
     *
     * @throws FactoryException should never happen.
     * @throws TransformException should never happen.
     */
    @Test
    public void testCreateMathTransform3D() throws FactoryException, TransformException {
        testCreateMathTransform(new GeographicOffsets3D());
    }

    /**
     * Tests the {@code createMathTransform(…)} method of the given provider.
     * This test uses the two-dimensional sample point given in §2.4.4.3 of EPSG guide (April 2015),
     * leaving the height (if any) to zero.
     */
    private void testCreateMathTransform(final GeographicOffsets provider) throws FactoryException, TransformException {
        final ParameterValueGroup pv = provider.getParameters().createValue();
        pv.parameter("Latitude offset" ).setValue(-5.86 / 3600);
        pv.parameter("Longitude offset").setValue(+0.28 / 3600);
        transform = provider.createMathTransform(null, pv);
        tolerance = Formulas.ANGULAR_TOLERANCE;
        final double[] source = new double[transform.getSourceDimensions()];
        final double[] target = new double[transform.getTargetDimensions()];
        source[1] = 38 + ( 8 + 36.565 /60) /60;     // 38°08′36.565″N
        target[1] = 38 + ( 8 + 30.705 /60) /60;     // 38°08′30.705″N
        source[0] = 23 + (48 + 16.235 /60) /60;     // 23°48′16.235″E
        target[0] = 23 + (48 + 16.515 /60) /60;     // 23°48′16.515″E
        verifyTransform(source, target);
    }
}
