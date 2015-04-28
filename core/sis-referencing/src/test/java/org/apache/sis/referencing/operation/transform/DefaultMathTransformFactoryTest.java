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

import java.util.Set;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.Projection;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.internal.referencing.provider.Affine;
import org.apache.sis.internal.referencing.provider.Mercator1SP;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.opengis.test.Assert.*;


/**
 * Tests the registration of operation methods in {@link DefaultMathTransformFactory}. This test uses the
 * providers registered in all {@code META-INF/services/org.opengis.referencing.operation.OperationMethod}
 * files found on the classpath.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@DependsOn({
    org.apache.sis.internal.referencing.provider.AllProvidersTest.class,
    OperationMethodSetTest.class
})
public final strictfp class DefaultMathTransformFactoryTest extends TestCase {
    /**
     * Returns the factory to use for the tests.
     *
     * @return The factory to use for the tests.
     */
    static DefaultMathTransformFactory factory() {
        final MathTransformFactory factory = DefaultFactories.forClass(MathTransformFactory.class);
        assertNotNull("No Apache SIS implementation of MathTransformFactory found in “META-INF/services”.", factory);
        assertEquals("Expected the default implementation of MathTransformFactory to be first in “META-INF/services”.",
                DefaultMathTransformFactory.class, factory.getClass());
        return (DefaultMathTransformFactory) factory;
    }

    /**
     * Tests the {@link DefaultMathTransformFactory#getOperationMethod(String)} method.
     *
     * @throws NoSuchIdentifierException Should never happen.
     */
    @Test
    public void testGetOperationMethod() throws NoSuchIdentifierException {
        final DefaultMathTransformFactory factory = factory();
        final OperationMethod affine   = factory.getOperationMethod(Constants.AFFINE);
        final OperationMethod mercator = factory.getOperationMethod("Mercator (variant A)");

        assertInstanceOf("Affine",               Affine.class,      affine);
        assertInstanceOf("Mercator (variant A)", Mercator1SP.class, mercator);

        // Same than above, using EPSG code and alias.
        assertSame("EPSG:9624",    affine,   factory.getOperationMethod("EPSG:9624"));
        assertSame("EPSG:9804",    mercator, factory.getOperationMethod("EPSG:9804"));
        assertSame("Mercator_1SP", mercator, factory.getOperationMethod("Mercator_1SP"));
    }

    /**
     * Tests non-existent operation method.
     */
    @Test
    @DependsOnMethod("testGetOperationMethod")
    public void testNonExistentCode() {
        final DefaultMathTransformFactory factory = factory();
        try {
            factory.getOperationMethod("EPXX:9624");
            fail("Expected NoSuchIdentifierException");
        } catch (NoSuchIdentifierException e) {
            final String message = e.getLocalizedMessage();
            assertTrue(message, message.contains("EPXX:9624"));
        }
    }

    /**
     * Tests the {@link DefaultMathTransformFactory#getAvailableMethods(Class)} method.
     *
     * @throws NoSuchIdentifierException Should never happen.
     */
    @Test
    @DependsOnMethod("testGetOperationMethod")
    public void testGetAvailableMethods() throws NoSuchIdentifierException {
        final DefaultMathTransformFactory factory = factory();
        final Set<OperationMethod> transforms  = factory.getAvailableMethods(SingleOperation.class);
        final Set<OperationMethod> conversions = factory.getAvailableMethods(Conversion.class);
        final Set<OperationMethod> projections = factory.getAvailableMethods(Projection.class);
        /*
         * Following tests should not cause loading of more classes than needed.
         */
        assertFalse(transforms .isEmpty());
        assertFalse(conversions.isEmpty());
        assertFalse(projections.isEmpty());
        assertTrue (conversions.contains(factory.getOperationMethod(Constants.AFFINE)));
        /*
         * Following tests will force instantiation of all remaining OperationMethod.
         */
        assertTrue("Conversions should be a subset of transforms.",  transforms .containsAll(conversions));
        assertTrue("Projections should be a subset of conversions.", conversions.containsAll(projections));
    }

    /**
     * Asks for names which are known to be duplicated. One of the duplicated elements is deprecated.
     * However Apache SIS uses the same implementation.
     *
     * @throws NoSuchIdentifierException Should never happen.
     */
    @Test
    public void testDuplicatedNames() throws NoSuchIdentifierException {
        final DefaultMathTransformFactory factory = factory();
        final OperationMethod current    = factory.getOperationMethod("EPSG:1029");
        final OperationMethod deprecated = factory.getOperationMethod("EPSG:9823");
        assertSame(current, factory.getOperationMethod("Equidistant Cylindrical (Spherical)"));
        assertSame("Should share the non-deprecated implementation.", current, deprecated);
    }
}
