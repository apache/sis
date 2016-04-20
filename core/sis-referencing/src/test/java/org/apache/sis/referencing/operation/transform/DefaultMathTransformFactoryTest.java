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

import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.Collections;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.Projection;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.parameter.Parameterized;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.operation.DefaultConversion;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.crs.DefaultProjectedCRS;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.internal.referencing.provider.Affine;
import org.apache.sis.internal.referencing.provider.Mercator1SP;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.util.CharSequences;

// Test dependencies
import org.apache.sis.referencing.cs.HardCodedCS;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the registration of operation methods in {@link DefaultMathTransformFactory}. This test uses the
 * providers registered in all {@code META-INF/services/org.opengis.referencing.operation.OperationMethod}
 * files found on the classpath.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 */
@DependsOn({
    org.apache.sis.internal.referencing.provider.ProvidersTest.class,
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
        assertTrue (transforms.contains(factory.getOperationMethod(Constants.AFFINE)));
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

    /**
     * Test {@link DefaultMathTransformFactory#createFromWKT(String)}. We test only a very small WKT here because
     * it is not the purpose of this class to test the parser. The main purpose of this test is to verify that
     * {@link DefaultMathTransformFactory} has been able to instantiate the parser.
     *
     * @throws FactoryException if the parsing failed.
     */
    @Test
    public void testCreateFromWKT() throws FactoryException {
        final MathTransform tr = factory().createFromWKT(
                "PARAM_MT[\"Affine\","
                    + "PARAMETER[\"num_row\",2],"
                    + "PARAMETER[\"num_col\",2],"
                    + "PARAMETER[\"elt_0_1\",7]]");

        assertMatrixEquals("Affine", new Matrix2(
                1, 7,
                0, 1), MathTransforms.getMatrix(tr), STRICT);
    }

    /**
     * Tests the creation of all registered map projections.
     * Only the semi-axis lengths are specified. For the rest, we rely on default values.
     *
     * @throws FactoryException if the construction of a map projection failed.
     *
     * @since 0.7
     */
    @Test
    public void testAllMapProjections() throws FactoryException {
        /*
         * Gets all map projections and creates a projection using the WGS84 ellipsoid
         * and default parameter values.
         */
        final Map<String,?> dummyName = Collections.singletonMap(DefaultProjectedCRS.NAME_KEY, "Test");
        final MathTransformFactory mtFactory = DefaultFactories.forBuildin(MathTransformFactory.class);
        final Collection<OperationMethod> methods = mtFactory.getAvailableMethods(Projection.class);
        for (final OperationMethod method : methods) {
            final String classification = method.getName().getCode();
            ParameterValueGroup param = mtFactory.getDefaultParameters(classification);
            param.parameter("semi_major").setValue(6377563.396);
            param.parameter("semi_minor").setValue(6356256.909237285);
            final MathTransform mt;
            try {
                mt = mtFactory.createParameterizedTransform(param);
            } catch (InvalidGeodeticParameterException e) {
                // Some map projections have mandatory parameters which we ignore for now
                // except for a few well-known projection that we know should not fail.
                if (classification.contains("Mercator")) {
                    throw e;
                }
                out.print(classification);
                out.print(CharSequences.spaces(42 - classification.length()));
                out.print(": ");
                out.println(e.getLocalizedMessage());
                continue;
            }
            /*
             * Verifies that the map projection properties are the ones that we specified.
             * Note that the Equirectangular projection has been optimized as an affine transform, which we skip.
             */
            if (mt instanceof LinearTransform) {
                continue;
            }
            assertInstanceOf(classification, Parameterized.class, mt);
            param = ((Parameterized) mt).getParameterValues();
            assertEquals(classification, param.getDescriptor().getName().getCode());
            assertEquals(classification, 6377563.396,       param.parameter("semi_major").doubleValue(), 1E-4);
            assertEquals(classification, 6356256.909237285, param.parameter("semi_minor").doubleValue(), 1E-4);
            /*
             * Creates a ProjectedCRS from the map projection. This part is more an integration test than
             * a DefaultMathTransformFactory test. Again, the intend is to verify that the properties are
             * the one that we specified.
             */
            final DefaultProjectedCRS crs = new DefaultProjectedCRS(dummyName,
                    CommonCRS.WGS84.normalizedGeographic(),
                    new DefaultConversion(dummyName, method, mt, null),
                    HardCodedCS.PROJECTED);
            final Conversion projection = crs.getConversionFromBase();
            assertSame(classification, mt, projection.getMathTransform());
            assertEquals(classification, projection.getMethod().getName().getCode());
        }
    }
}
