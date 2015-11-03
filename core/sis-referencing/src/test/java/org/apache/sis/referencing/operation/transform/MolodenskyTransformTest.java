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
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.referencing.CommonCRS;

import static java.lang.StrictMath.toRadians;

// Test dependencies
import org.apache.sis.internal.referencing.provider.GeocentricTranslationTest;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link MolodenskyTransform}.
 *
 * @author  Tara Athan
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn({
    CoordinateDomainTest.class,
    ContextualParametersTest.class
})
public final strictfp class MolodenskyTransformTest extends MathTransformTestCase {
    /**
     * Creates a Molodensky transform for a datum shift from WGS84 to ED50.
     * Tolerance thresholds are also initialized.
     *
     * @throws FactoryException if an error occurred while creating a transform step.
     */
    private void create(final boolean abridged) throws FactoryException {
        final Ellipsoid source = CommonCRS.WGS84.ellipsoid();
        final Ellipsoid target = CommonCRS.ED50.ellipsoid();
        final double a = source.getSemiMajorAxis();
        final double b = source.getSemiMinorAxis();
        transform = new MolodenskyTransform(abridged, a, b, true, target.getSemiMajorAxis() - a,
                1/target.getInverseFlattening() - 1/source.getInverseFlattening(), true, 84.87, 96.49, 116.95, SI.METRE)
                .createGeodeticTransformation(DefaultFactories.forBuildin(MathTransformFactory.class));

        final double delta = toRadians(100.0 / 60) / 1852;          // Approximatively 100 metres
        derivativeDeltas = new double[] {delta, delta, 100};        // (Δλ, Δφ, Δh)
        tolerance  = GeocentricTranslationTest.precision(1);        // Half the precision of target sample point
        zTolerance = GeocentricTranslationTest.precision(3);        // Required precision for h
        zDimension = new int[] {2};                                 // Dimension of h where to apply zTolerance
        assertFalse(transform.isIdentity());
    }

    /**
     * Tests using the sample point given by the EPSG guide.
     *
     * <ul>
     *   <li>Source point in WGS84: 53°48'33.820"N, 02°07'46.380"E, 73.00 metres.</li>
     *   <li>Target point in ED50:  53°48'36.565"N, 02'07"51.477"E, 28.02 metres.</li>
     *   <li>Datum shift: dX = +84.87m, dY = +96.49m, dZ = +116.95m.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating a transform step.
     * @throws TransformException if the transformation failed.
     */
    @Test
    public void testAbridgedMolodensky() throws FactoryException, TransformException {
        isInverseTransformSupported = false;
        isDerivativeSupported = false;
        create(true);
        validate();
        verifyTransform(GeocentricTranslationTest.samplePoint(1),
                        GeocentricTranslationTest.samplePoint(5));
    }

    /**
     * Tests using the same EPSG example than the one provided in {@link EllipsoidalToCartesianTransformTest}.
     *
     * <ul>
     *   <li>Source point in WGS84: 53°48'33.820"N, 02°07'46.380"E, 73.00 metres.</li>
     *   <li>Target point in ED50:  53°48'36.565"N, 02'07"51.477"E, 28.02 metres.</li>
     *   <li>Datum shift: dX = +84.87m, dY = +96.49m, dZ = +116.95m.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating a transform step.
     * @throws TransformException if the transformation failed.
     */
    @Test
    @DependsOnMethod("testAbridgedMolodensky")
    public void testMolodensky() throws FactoryException, TransformException {
        isInverseTransformSupported = false;
        isDerivativeSupported = false;
        create(false);
        validate();
        verifyTransform(GeocentricTranslationTest.samplePoint(1),
                        GeocentricTranslationTest.samplePoint(4));
    }

    /**
     * Tests conversion of random points. The test is performed with the Molodensky transform,
     * not the abridged one, because the errors caused by the abridged Molondeky method is too
     * high for this test.
     *
     * @throws FactoryException if an error occurred while creating a transform step.
     * @throws TransformException if a conversion failed.
     */
    @Test
    @DependsOnMethod("testMolodensky")
    public void testRandomPoints() throws FactoryException, TransformException {
        isInverseTransformSupported = false;
        isDerivativeSupported = false;
        create(false);
        verifyInDomain(CoordinateDomain.GEOGRAPHIC, 208129394);
    }
}
