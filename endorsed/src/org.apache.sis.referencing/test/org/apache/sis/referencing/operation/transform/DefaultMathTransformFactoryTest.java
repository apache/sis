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
import java.util.Iterator;
import java.util.ServiceLoader;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.parameter.Parameterized;
import org.apache.sis.referencing.crs.DefaultProjectedCRS;
import org.apache.sis.referencing.operation.DefaultConversion;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.provider.Affine;
import org.apache.sis.referencing.operation.provider.Mercator1SP;
import org.apache.sis.referencing.operation.provider.MapProjection;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.measure.Units;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.cs.HardCodedCS;
import org.apache.sis.referencing.crs.HardCodedCRS;
import static org.apache.sis.test.Assertions.assertMessageContains;
import static org.apache.sis.referencing.Assertions.assertMatrixEquals;

// Specific to the main branch:
import static org.apache.sis.referencing.internal.shared.CoordinateOperations.builder;


/**
 * Tests the registration of operation methods in {@link DefaultMathTransformFactory}. This test uses the
 * providers registered as an {@code org.opengis.referencing.operation.OperationMethod} in all available modules.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class DefaultMathTransformFactoryTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultMathTransformFactoryTest() {
    }

    /**
     * Returns the factory to use for the tests.
     *
     * @return the factory to use for the tests.
     */
    static DefaultMathTransformFactory factory() {
        return DefaultMathTransformFactory.provider();
    }

    /**
     * Tests the registration as a service provider.
     */
    @Test
    @Disabled("Pending the completion of migration to JDK 9")
    public void testServiceProvider() {
        final MathTransformFactory factory = ServiceLoader.load(MathTransformFactory.class).findFirst().orElse(null);
        assertNotNull(factory, "No Apache SIS implementation of MathTransformFactory found in “module-info”.");
        assertEquals(DefaultMathTransformFactory.class, factory.getClass(),
                "Expected the default implementation of MathTransformFactory to be first in “module-info”.");
        assertSame(factory(), factory);
    }

    /**
     * Tests the correction for a Java 8 bug. In Java 8, {@link ServiceLoader} didn't supported
     * the usage of two {@link Iterator} instances before the first iteration is finished.
     * This problem has been fixed with Java Platform Module System (JPMS) implementation.
     */
    @Test
    public void testServiceLoaderIterator() {
        ServiceLoader<?> loader = ServiceLoader.load(OperationMethod.class);

        Iterator<?> it1 = loader.iterator();
        assertTrue   (it1.hasNext());
        assertNotNull(it1.next());

        Iterator<?> it2 = loader.iterator();
        assertTrue   (it1.hasNext());
        assertTrue   (it2.hasNext());
        assertNotNull(it1.next());
        assertNotNull(it2.next());      // ConcurrentModificationException was used to be thownn here.
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

        assertInstanceOf(Affine.class,      affine);
        assertInstanceOf(Mercator1SP.class, mercator);

        // Same as above, using EPSG code and alias.
        assertSame(affine,   factory.getOperationMethod("EPSG:9624"));
        assertSame(mercator, factory.getOperationMethod("EPSG:9804"));
        assertSame(mercator, factory.getOperationMethod("Mercator_1SP"));
    }

    /**
     * Tests non-existent operation method.
     */
    @Test
    public void testNonExistentCode() {
        final DefaultMathTransformFactory factory = factory();
        var e = assertThrows(NoSuchIdentifierException.class, () -> factory.getOperationMethod("EPXX:9624"));
        assertMessageContains(e, "EPXX:9624");
    }

    /**
     * Tests the {@link DefaultMathTransformFactory#getAvailableMethods(Class)} method.
     *
     * @throws NoSuchIdentifierException if the operation was not found.
     */
    @Test
    public void testGetAvailableMethods() throws NoSuchIdentifierException {
        final DefaultMathTransformFactory factory = factory();
        final Set<OperationMethod> transforms  = factory.getAvailableMethods(SingleOperation.class);
        final Set<OperationMethod> conversions = factory.getAvailableMethods(Conversion.class);
        /*
         * Following tests should not cause loading of more classes than needed.
         */
        assertFalse(transforms .isEmpty());
        assertFalse(conversions.isEmpty());
        assertTrue (transforms.contains(factory.getOperationMethod(Constants.AFFINE)));
        /*
         * Following tests will force instantiation of all remaining OperationMethod.
         */
        assertTrue(transforms.containsAll(conversions), "Conversions should be a subset of transforms.");
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
        assertSame(current, deprecated, "Should share the non-deprecated implementation.");
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

        assertMatrixEquals(new Matrix2(1, 7, 0, 1), tr, "Affine");
    }

    /**
     * Tests the creation of all registered map projections.
     * This test sets the semi-axis lengths and a few other mandatory parameter values.
     * For remaining parameters, we rely on default values.
     *
     * @throws FactoryException if the construction of a map projection failed.
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
        final Collection<OperationMethod> methods = factory.getAvailableMethods(Conversion.class);
        int count = 0;
        for (final OperationMethod method : methods) {
            if (!(method instanceof MapProjection)) {
                continue;
            }
            final String classification = method.getName().getCode();
            final var builder = builder(factory, classification);
            ParameterValueGroup pg = builder.parameters();
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
                case "Hotine Oblique Mercator (variant B)":       param = "Azimuth at projection centre"; value = 30; break;
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
                mt = builder.create();
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
            pg = assertInstanceOf(Parameterized.class, mt, classification).getParameterValues();
            assertNotNull(pg, classification);
            assertEquals(pg.getDescriptor().getName().getCode(), classification);
            assertEquals(6377563.396,       pg.parameter("semi_major").doubleValue(), 1E-4, classification);
            assertEquals(6356256.909237285, pg.parameter("semi_minor").doubleValue(), 1E-4, classification);
            if (param != null) {
                assertEquals(value, pg.parameter(param).doubleValue(), 1E-4, classification);
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
            assertSame(mt, projection.getMathTransform(), classification);
            assertEquals(projection.getMethod().getName().getCode(), classification);
            count++;
        }
        assertTrue(count >= 15, "Map projection methods not found.");
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
