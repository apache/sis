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
import org.apache.sis.referencing.operation.DefaultConversion;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.crs.DefaultProjectedCRS;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.internal.referencing.provider.Affine;
import org.apache.sis.internal.referencing.provider.Mercator1SP;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.measure.Units;

// Test dependencies
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.cs.HardCodedCS;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.opengis.test.Assert.assertInstanceOf;
import static org.opengis.test.Assert.assertMatrixEquals;


/**
 * Tests the registration of operation methods in {@link DefaultMathTransformFactory}. This test uses the
 * providers registered in all {@code META-INF/services/org.opengis.referencing.operation.OperationMethod}
 * files found on the classpath.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   0.6
 */
@DependsOn({
    org.apache.sis.internal.referencing.provider.ProvidersTest.class,
    OperationMethodSetTest.class
})
public final class DefaultMathTransformFactoryTest extends TestCase {
    /**
     * Returns the factory to use for the tests.
     *
     * @return the factory to use for the tests.
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
     * @throws NoSuchIdentifierException if the operation was not found.
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
     * @throws NoSuchIdentifierException if the operation was not found.
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
     * However, Apache SIS uses the same implementation.
     *
     * @throws NoSuchIdentifierException if the operation was not found.
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
     * This test sets the semi-axis lengths and a few other mandatory parameter values.
     * For remaining parameters, we rely on default values.
     *
     * @throws FactoryException if the construction of a map projection failed.
     *
     * @since 0.7
     */
    @Test
    @SuppressWarnings("fallthrough")
    public void testAllMapProjections() throws FactoryException {
        /*
         * Gets all map projections and creates a projection using the WGS84 ellipsoid
         * and default parameter values.
         */
        final MathTransformFactory factory = factory();
        final Map<String,?> dummyName = Map.of(DefaultProjectedCRS.NAME_KEY, "Test");
        final Collection<OperationMethod> methods = factory.getAvailableMethods(Projection.class);
        for (final OperationMethod method : methods) {
            final String classification = method.getName().getCode();
            ParameterValueGroup pg = factory.getDefaultParameters(classification);
            pg.parameter("semi_major").setValue(6377563.396);
            pg.parameter("semi_minor").setValue(6356256.909237285);
            /*
             * Most parameters have default values, typically 0° or 0 metre, so we don't need to specify them for
             * the purpose of this test. But some map projections have mandatory parameters without default value.
             * In those cases, we must specify an arbitrary value otherwise the instantiation will fail.  When we
             * need to specify only one value, that value is remembered for opportunist verification.
             */
            String param = null;
            double value = Double.NaN;
            switch (classification) {
                case "Polar Stereographic (variant A)":           param = "Latitude of natural origin"; value = 90; break;
                case "Polar Stereographic (variant B)":
                case "Polar Stereographic (variant C)":           param = "Latitude of standard parallel"; value = 80; break;
                case "Hotine Oblique Mercator (variant A)":
                case "Hotine Oblique Mercator (variant B)":       param = "Azimuth of initial line"; value = 30; break;
                case "Lambert Conic Conformal (1SP)":
                case "Lambert Conic Conformal (West Orientated)": param = "Latitude of natural origin"; value = 45; break;
                case "Lambert Conic Conformal (2SP Michigan)":    param = "Ellipsoid scaling factor"; value = 1;  // Fall through for defining standard parallels too.
                case "Lambert Conic Conformal (2SP Belgium)":
                case "Lambert Conic Conformal (2SP)":
                case "Albers Equal Area": {
                    pg.parameter("Latitude of 1st standard parallel").setValue(30);
                    pg.parameter("Latitude of 2nd standard parallel").setValue(50);
                    break;
                }
                case "Hotine_Oblique_Mercator_Two_Point_Center":
                case "Hotine_Oblique_Mercator_Two_Point_Natural_Origin": {
                    pg.parameter( "Latitude_Of_1st_Point").setValue(30);
                    pg.parameter( "Latitude_Of_2nd_Point").setValue(50);
                    pg.parameter("Longitude_Of_1st_Point").setValue(10);
                    pg.parameter("Longitude_Of_2nd_Point").setValue(20);
                    break;
                }
                case "Satellite-Tracking": {
                    pg.parameter("satellite_orbit_inclination").setValue(  99.092);     // Landsat 1, 2 and 3.
                    pg.parameter("satellite_orbital_period")   .setValue( 103.267, Units.MINUTE);
                    pg.parameter("ascending_node_period")      .setValue(1440.0,   Units.MINUTE);
                    break;
                }
            }
            if (param != null) {
                pg.parameter(param).setValue(value);
            }
            final MathTransform mt;
            try {
                mt = factory.createParameterizedTransform(pg);
            } catch (InvalidGeodeticParameterException e) {
                fail(classification + ": " + e.getLocalizedMessage());
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
            pg = ((Parameterized) mt).getParameterValues();
            assertNotNull(classification, pg);
            assertEquals(classification, pg.getDescriptor().getName().getCode());
            assertEquals(classification, 6377563.396,       pg.parameter("semi_major").doubleValue(), 1E-4);
            assertEquals(classification, 6356256.909237285, pg.parameter("semi_minor").doubleValue(), 1E-4);
            if (param != null) {
                assertEquals(classification, value, pg.parameter(param).doubleValue(), 1E-4);
            }
            /*
             * Creates a ProjectedCRS from the map projection. This part is more an integration test than
             * a DefaultMathTransformFactory test. Again, the intent is to verify that the properties are
             * the one that we specified.
             */
            final DefaultProjectedCRS crs = new DefaultProjectedCRS(dummyName,
                    HardCodedCRS.WGS84,
                    new DefaultConversion(dummyName, method, mt, null),
                    HardCodedCS.PROJECTED);
            final Conversion projection = crs.getConversionFromBase();
            assertSame(classification, mt, projection.getMathTransform());
            assertEquals(classification, projection.getMethod().getName().getCode());
        }
    }

    /**
     * Tests {@link DefaultMathTransformFactory#swapAndScaleAxes(MathTransform, DefaultMathTransformFactory.Context)}
     * with different number of dimensions.
     *
     * @throws FactoryException if the transform construction failed.
     */
    @Test
    public void testSwapAndScaleAxes() throws FactoryException {
        final DefaultMathTransformFactory factory = factory();
        final DefaultMathTransformFactory.Context context = new DefaultMathTransformFactory.Context();
        context.setSource(HardCodedCS.GEODETIC_3D);
        context.setTarget(HardCodedCS.CARTESIAN_3D);
        /*
         * Simulate a case where the parameterized transform is a two-dimensional map projection,
         * but the input and output CRS are three-dimensional geographic and projected CRS respectively.
         */
        MathTransform mt = factory.swapAndScaleAxes(MathTransforms.identity(2), context);
        assertEquals(3, mt.getSourceDimensions());
        assertEquals(3, mt.getTargetDimensions());
        assertTrue(mt.isIdentity());
        /*
         * Transform from 3D to 2D. Height dimension is dropped.
         */
        context.setSource(HardCodedCS.GEODETIC_3D);
        context.setTarget(HardCodedCS.GEODETIC_2D);
        mt = factory.swapAndScaleAxes(MathTransforms.identity(2), context);
        assertMatrixEquals("3D → 2D", Matrices.create(3, 4, new double[] {
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 0, 1
        }), MathTransforms.getMatrix(mt), STRICT);
        /*
         * Transform from 2D to 3D. Coordinate values in the height dimension are unknown (NaN).
         * This case happen when the third dimension is handled as a "pass through" dimension.
         */
        context.setSource(HardCodedCS.GEODETIC_2D);
        context.setTarget(HardCodedCS.GEODETIC_3D);
        mt = factory.swapAndScaleAxes(MathTransforms.identity(2), context);
        assertMatrixEquals("2D → 3D", Matrices.create(4, 3, new double[] {
            1, 0, 0,
            0, 1, 0,
            0, 0, Double.NaN,
            0, 0, 1
        }), MathTransforms.getMatrix(mt), STRICT);
        /*
         * Same transform from 2D to 3D, but this time with the height consumed by the parameterized operation.
         * This is differentiated from the previous case by the fact that the parameterized operation is three-dimensional.
         */
        mt = factory.swapAndScaleAxes(MathTransforms.identity(3), context);
        assertMatrixEquals("2D → 3D", Matrices.create(4, 3, new double[] {
            1, 0, 0,
            0, 1, 0,
            0, 0, 0,
            0, 0, 1
        }), MathTransforms.getMatrix(mt), STRICT);
        /*
         * Test error message when adding a dimension that is not ellipsoidal height.
         */
        context.setSource(HardCodedCS.CARTESIAN_2D);
        context.setTarget(HardCodedCS.CARTESIAN_3D);
        try {
            factory.swapAndScaleAxes(MathTransforms.identity(2), context);
            fail("Should not have accepted the given coordinate systems.");
        } catch (InvalidGeodeticParameterException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("2D → tr(2D → 2D) → 3D"));
        }
    }

    /**
     * Tests {@link DefaultMathTransformFactory#caching(boolean)}.
     */
    @Test
    public void testCaching() {
        final DefaultMathTransformFactory factory = factory();
        final DefaultMathTransformFactory caching = factory.caching(false);

        assertNotSame(factory, caching);
        assertSame   (factory, factory.caching(true));
        assertSame   (factory,   caching.caching(true));
        assertSame   (caching,     caching.caching(false));
    }
}
