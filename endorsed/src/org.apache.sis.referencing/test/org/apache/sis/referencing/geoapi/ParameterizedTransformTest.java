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
package org.apache.sis.referencing.geoapi;

import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.sis.test.FailureDetailsReporter;


/**
 * Runs a suite of tests provided in the GeoAPI project.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@ExtendWith(FailureDetailsReporter.class)
public final class ParameterizedTransformTest extends org.opengis.test.referencing.ParameterizedTransformTest {
    /**
     * Creates a new test suite using the singleton factory instance.
     */
    public ParameterizedTransformTest() {
        super(DefaultMathTransformFactory.provider());
    }

    /**
     * Every map projections shall be instances of {@link MathTransform2D}.
     * Note that some tests inherited from the parent class are not about
     * map projections.
     */
    @AfterEach
    public void ensureMathTransform2D() {
        final MathTransform tr = transform;
        if (tr != null && tr.getSourceDimensions() == 2 && tr.getTargetDimensions() == 2) {
            assertInstanceOf(MathTransform2D.class, tr);
        }
    }

    /**
     * Disables the derivative (Jacobian) tests because not yet implemented.
     *
     * @throws FactoryException if the math transform cannot be created.
     * @throws TransformException if the example point cannot be transformed.
     */
    @Test
    @Override
    public void testModifiedAzimuthalEquidistant() throws FactoryException, TransformException {
        isDerivativeSupported = false;
        super.testModifiedAzimuthalEquidistant();
    }

    /**
     * Disables the derivative (Jacobian) tests because not yet implemented.
     *
     * @throws FactoryException if the math transform cannot be created.
     * @throws TransformException if the example point cannot be transformed.
     */
    @Test
    @Override
    public void testHyperbolicCassiniSoldner() throws FactoryException, TransformException {
        isDerivativeSupported = false;
        super.testHyperbolicCassiniSoldner();
    }
}