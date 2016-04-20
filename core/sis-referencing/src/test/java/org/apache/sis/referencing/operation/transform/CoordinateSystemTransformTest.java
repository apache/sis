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

import javax.measure.unit.SI;
import org.opengis.util.FactoryException;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.crs.DefaultGeocentricCRS;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import org.opengis.test.referencing.TransformTestCase;
import org.apache.sis.referencing.cs.HardCodedCS;
import org.apache.sis.test.DependsOn;
import org.apache.sis.util.ArraysExt;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;


/**
 * Tests the {@link CoordinateSystemTransform} static factory method.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn({
    SphericalToCartesianTest.class,
    CartesianToSphericalTest.class
})
public final strictfp class CoordinateSystemTransformTest extends TransformTestCase {
    /**
     * A right-handed spherical coordinate system.
     */
    private static SphericalCS spherical;

    /**
     * The factory to use for creating the affine transforms and concatenated transforms.
     */
    private static MathTransformFactory factory;

    /**
     * Creates the {@link MathTransformFactory} to be used for the tests.
     * We do not use the system-wide factory in order to have better tests isolation.
     */
    @BeforeClass
    public static void createFactory() {
        factory = new DefaultMathTransformFactory();
        spherical = (SphericalCS) DefaultGeocentricCRS.castOrCopy(CommonCRS.WGS84.spherical())
                            .forConvention(AxesConvention.RIGHT_HANDED).getCoordinateSystem();
    }

    /**
     * Disposes the {@link MathTransformFactory} used for the tests.
     */
    @AfterClass
    public static void disposeFactory() {
        spherical = null;
        factory = null;
    }

    /**
     * Returns the given coordinate system but with linear axes in centimetres instead of metres.
     */
    private static CoordinateSystem toCentimetres(final CoordinateSystem cs) {
        return CoordinateSystems.replaceLinearUnit(cs, SI.CENTIMETRE);
    }

    /**
     * Returns {@link SphericalToCartesianTest#testData()} modified for the source and target
     * coordinate systems used in this class.
     */
    private static double[][] sphericalTestData() {
        final double[][] data = SphericalToCartesianTest.testData();
        final double[] source = data[0];
        for (int i=0; i<source.length; i += 3) {
            ArraysExt.swap(source, i, i+1);
        }
        final double[] target = data[1];
        for (int i=0; i<target.length; i++) {
            target[i] *= 100;
        }
        return data;
    }

    /**
     * Tests {@link CoordinateSystemTransform#create(MathTransformFactory, CoordinateSystem, CoordinateSystem)}.
     * for a conversion between two spherical coordinate systems.
     *
     * @throws FactoryException if an error occurred while creating the transform.
     * @throws TransformException if an error occurred while transforming the test point.
     */
    @Test
    public void testSphericalToSpherical() throws FactoryException, TransformException {
        transform = CoordinateSystemTransform.create(factory, HardCodedCS.SPHERICAL, spherical);
        tolerance = 0;
        final double[][] data = SphericalToCartesianTest.testData();
        final double[] source = data[0];
        final double[] target = data[1];
        System.arraycopy(source, 0, target, 0, source.length);
        for (int i=0; i<source.length; i += 3) {
            ArraysExt.swap(source, i, i+1);
        }
        verifyTransform(source, target);
    }

    /**
     * Tests {@link CoordinateSystemTransform#create(MathTransformFactory, CoordinateSystem, CoordinateSystem)}.
     * for a conversion from spherical to Cartesian coordinates.
     *
     * @throws FactoryException if an error occurred while creating the transform.
     * @throws TransformException if an error occurred while transforming the test point.
     */
    @Test
    public void testSphericalToCartesian() throws FactoryException, TransformException {
        tolerance = 1E-9;
        transform = CoordinateSystemTransform.create(factory, HardCodedCS.SPHERICAL, toCentimetres(HardCodedCS.GEOCENTRIC));
        final double[][] data = sphericalTestData();
        verifyTransform(data[0], data[1]);
    }

    /**
     * Tests {@link CoordinateSystemTransform#create(MathTransformFactory, CoordinateSystem, CoordinateSystem)}.
     * for a conversion from Cartesian to spherical coordinates.
     *
     * @throws FactoryException if an error occurred while creating the transform.
     * @throws TransformException if an error occurred while transforming the test point.
     */
    @Test
    public void testCartesianToSpherical() throws FactoryException, TransformException {
        tolerance = 1E-9;
        transform = CoordinateSystemTransform.create(factory, toCentimetres(HardCodedCS.GEOCENTRIC), HardCodedCS.SPHERICAL);
        final double[][] data = sphericalTestData();
        verifyTransform(data[1], data[0]);
    }

    /**
     * Returns {@link PolarToCartesianTest#testData(boolean)} modified for the source and target
     * coordinate systems used in this class.
     */
    private static double[][] polarTestData(final boolean withHeight) {
        final int dimension = withHeight ? 3 : 2;
        final double[][] data = PolarToCartesianTest.testData(withHeight);
        final double[] source = data[0];
        for (int i=1; i<source.length; i += dimension) {
            source[i] = -source[i];                 // Change counterclockwise direction into clockwise direction.
        }
        final double[] target = data[1];
        for (int i=0; i<target.length; i++) {
            target[i] *= 100;
        }
        return data;
    }

    /**
     * Tests {@link CoordinateSystemTransform#create(MathTransformFactory, CoordinateSystem, CoordinateSystem)}.
     * for a conversion from cylindrical to Cartesian coordinates.
     *
     * @throws FactoryException if an error occurred while creating the transform.
     * @throws TransformException if an error occurred while transforming the test point.
     */
    @Test
    public void testCylindricalToCartesian() throws FactoryException, TransformException {
        tolerance = 1E-9;
        transform = CoordinateSystemTransform.create(factory, HardCodedCS.CYLINDRICAL, toCentimetres(HardCodedCS.CARTESIAN_3D));
        final double[][] data = polarTestData(true);
        verifyTransform(data[0], data[1]);
    }

    /**
     * Tests {@link CoordinateSystemTransform#create(MathTransformFactory, CoordinateSystem, CoordinateSystem)}.
     * for a conversion from Cartesian to cylindrical coordinates.
     *
     * @throws FactoryException if an error occurred while creating the transform.
     * @throws TransformException if an error occurred while transforming the test point.
     */
    @Test
    public void testCartesianToCylindrical() throws FactoryException, TransformException {
        tolerance = 1E-9;
        transform = CoordinateSystemTransform.create(factory, toCentimetres(HardCodedCS.CARTESIAN_3D), HardCodedCS.CYLINDRICAL);
        final double[][] data = polarTestData(true);
        verifyTransform(data[1], data[0]);
    }

    /**
     * Tests {@link CoordinateSystemTransform#create(MathTransformFactory, CoordinateSystem, CoordinateSystem)}.
     * for a conversion from polar to Cartesian coordinates.
     *
     * @throws FactoryException if an error occurred while creating the transform.
     * @throws TransformException if an error occurred while transforming the test point.
     */
    @Test
    public void testPolarToCartesian() throws FactoryException, TransformException {
        tolerance = 1E-9;
        transform = CoordinateSystemTransform.create(factory, HardCodedCS.POLAR, toCentimetres(HardCodedCS.CARTESIAN_2D));
        final double[][] data = polarTestData(false);
        verifyTransform(data[0], data[1]);
    }

    /**
     * Tests {@link CoordinateSystemTransform#create(MathTransformFactory, CoordinateSystem, CoordinateSystem)}.
     * for a conversion from Cartesian to polar coordinates.
     *
     * @throws FactoryException if an error occurred while creating the transform.
     * @throws TransformException if an error occurred while transforming the test point.
     */
    @Test
    public void testCartesianToPolar() throws FactoryException, TransformException {
        tolerance = 1E-9;
        transform = CoordinateSystemTransform.create(factory, toCentimetres(HardCodedCS.CARTESIAN_2D), HardCodedCS.POLAR);
        final double[][] data = polarTestData(false);
        verifyTransform(data[1], data[0]);
    }
}
