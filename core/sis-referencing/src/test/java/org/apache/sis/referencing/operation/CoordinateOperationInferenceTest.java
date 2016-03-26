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
package org.apache.sis.referencing.operation;

import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import org.apache.sis.referencing.operation.transform.MathTransformTestCase;
import org.apache.sis.test.DependsOn;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link CoordinateOperationInference}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn({
    DefaultConversionTest.class,
    DefaultTransformationTest.class,
    DefaultPassThroughOperationTest.class,
    DefaultConcatenatedOperationTest.class
})
public final strictfp class CoordinateOperationInferenceTest extends MathTransformTestCase {
    /**
     * The transformation factory to use for testing.
     */
    private static DefaultCoordinateOperationFactory factory;

    /**
     * Creates a new {@link DefaultCoordinateOperationFactory} to use for testing purpose.
     * The same factory will be used for all tests in this class.
     */
    @BeforeClass
    public static void createFactory() {
        factory = new DefaultCoordinateOperationFactory();
    }

    /**
     * Disposes the factory created by {@link #createFactory()} after all tests have been executed.
     */
    @AfterClass
    public static void disposeFactory() {
        factory = null;
    }

    /**
     * Makes sure that {@code createOperation(sourceCRS, targetCRS)} returns an identity transform
     * when {@code sourceCRS} and {@code targetCRS} are identical.
     *
     * @throws FactoryException if the operation can not be created.
     */
    @Test
    public void testIdentityTransform() throws FactoryException {
        CoordinateReferenceSystem crs = CommonCRS.WGS84.normalizedGeographic();
        CoordinateOperation operation = factory.createOperation(crs, crs);
        assertSame("sourceCRS",  crs, operation.getSourceCRS());
        assertSame("targetCRS",  crs, operation.getTargetCRS());
        assertTrue("isIdentity", operation.getMathTransform().isIdentity());
    }
}
