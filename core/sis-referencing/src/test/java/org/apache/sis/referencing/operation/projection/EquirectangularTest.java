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

import org.opengis.util.FactoryException;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.referencing.provider.Equirectangular;
import org.apache.sis.test.mock.MathTransformFactoryMock;
import org.junit.Test;


/**
 * Tests the affine transform created by the {@link Equirectangular} class. This map projection is a
 * special case since the transform is implemented by an affine transform instead than a class from
 * the {@link org.apache.sis.referencing.operation.projection} package.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public final strictfp class EquirectangularTest extends MapProjectionTestCase {
    /**
     * Initializes a simple Equirectangular projection on sphere.
     */
    private void initialize() throws FactoryException {
        final Equirectangular provider = new Equirectangular();
        final Parameters parameters = parameters(provider, false);
        transform = new MathTransformFactoryMock(provider).createParameterizedTransform(parameters);
        tolerance = Formulas.LINEAR_TOLERANCE;  // Not NORMALIZED_TOLERANCE since this is not a NormalizedProjection.
        validate();
    }

    /**
     * Tests the WKT formatting of an Equirectangular projection. While the projection is implemented by an
     * affine transform, the WKT formatter should handle this projection in a special way and show the parameters.
     *
     * @throws FactoryException should never happen.
     */
    @Test
    public void testWKT() throws FactoryException {
        initialize();
        assertWktEquals(
                "PARAM_MT[“Equirectangular”,\n" +
                "  PARAMETER[“semi_major”, 6371007.0],\n" +
                "  PARAMETER[“semi_minor”, 6371007.0]]");
    }
}
