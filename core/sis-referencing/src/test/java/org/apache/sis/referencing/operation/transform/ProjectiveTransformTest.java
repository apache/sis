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
package org.apache.sis.referencing.operation.transform;

import org.opengis.referencing.operation.Matrix;
import org.opengis.test.referencing.AffineTransformTest;
import org.apache.sis.test.DependsOn;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


/**
 * Tests the {@link ProjectiveTransform} class by inheriting the tests defined in GeoAPI conformance module.
 * We use the {@link AffineTransform2D} class as a reference, so we need to avoid NaN values.
 * Note that {@link CopyTransformTest} will use {@code ProjectiveTransform} as a reference,
 * this time with NaN values.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5 (derived from geotk-3.08)
 * @version 0.5
 * @module
 */
@RunWith(JUnit4.class)
@DependsOn(AbstractMathTransformTest.class)
public final strictfp class ProjectiveTransformTest extends AffineTransformTest {
    /**
     * Creates a new test suite.
     */
    public ProjectiveTransformTest() {
        super(new MathTransformFactoryBase() {
            @Override
            public ProjectiveTransform createAffineTransform(final Matrix matrix) {
                if (matrix.getNumRow() == 3 && matrix.getNumCol() == 3) {
                    return new ProjectiveTransform2D(matrix);
                } else {
                    return new ProjectiveTransform(matrix);
                }
            }
        });
    }

    /*
     * Inherit all the tests from GeoAPI.
     */
}
