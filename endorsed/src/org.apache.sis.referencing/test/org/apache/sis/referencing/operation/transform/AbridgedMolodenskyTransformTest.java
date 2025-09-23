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

import static java.lang.StrictMath.toRadians;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.internal.shared.Formulas;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.referencing.datum.HardCodedDatum;
import static org.apache.sis.test.Assertions.assertSerializedEquals;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.test.CalculationType;


/**
 * Tests {@link AbridgedMolodenskyTransform2D}. This class takes {@link MolodenskyTransform}
 * as a reference implementation and verifies that we get consistent results. We rely on the
 * fact that {@link MolodenskyTransform#transform(double[], int, double[], int, boolean)} is
 * not overridden.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class AbridgedMolodenskyTransformTest extends MathTransformTestCase {
    /**
     * Creates a new test case.
     */
    public AbridgedMolodenskyTransformTest() {
        final double delta = toRadians(100.0 / 60) / 1852;      // Approximately 100 metres
        derivativeDeltas   = new double[] {delta, delta};       // (Δλ, Δφ)
        λDimension         = new int[] {0};                     // Dimension for which to ignore ±360° differences.
        tolerance          = Formulas.ANGULAR_TOLERANCE;        // Tolerance for longitude and latitude in degrees
    }

    /**
     * Compares the abridged Molodensky transform with a non-abridged one.
     * This test works on angular values in radians, without concatenated transforms.
     *
     * @throws FactoryException if an error occurred while creating a transform step.
     * @throws TransformException if a transformation failed.
     *
     * @see MolodenskyTransformTest#compareWithGeocentricTranslation()
     */
    @Test
    public void compareWithReferenceImplementation() throws FactoryException, TransformException {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final AbstractMathTransform transform = new AbridgedMolodenskyTransform2D(
                HardCodedDatum.WGS84 .getEllipsoid(),
                HardCodedDatum.SPHERE.getEllipsoid());
        this.transform = transform;
        tolerance = toRadians(tolerance);
        final double[] sources  = generateRandomCoordinates(CoordinateDomain.GEOGRAPHIC_RADIANS, 0f);
        final double[] targets  = new double[2];
        final double[] expected = new double[2];
        for (int i=0; i<sources.length; i+=2) {
            transform.transform(sources, i, targets,  0, 1);        // Use the overridden method.
            transform.transform(sources, i, expected, 0, false);    // Use the MolodenskyTransform method.
            assertCoordinateEquals(expected, targets, 0, CalculationType.DIRECT_TRANSFORM, "transform");
        }
    }

    /**
     * Creates an abridged Molodensky transform working on geographic coordinates in degrees.
     * It should be an {@link AbridgedMolodenskyTransform2D} instance concatenated with affine transforms.
     */
    private static MathTransform create() throws FactoryException {
        final MathTransform tr = MolodenskyTransform.createGeodeticTransformation(
                        DefaultMathTransformFactory.provider(),
                        HardCodedDatum.WGS84 .getEllipsoid(), false,
                        HardCodedDatum.SPHERE.getEllipsoid(), false, 0, 0, 0, true);
        int n = 0;
        for (final MathTransform step : MathTransforms.getSteps(tr)) {
            if (!(step instanceof LinearTransform)) {
                assertInstanceOf(AbridgedMolodenskyTransform2D.class, step);
                n++;
            }
        }
        assertEquals(1, n);
        return tr;
    }

    /**
     * Verifies consistency of all transform methods. This method tests the full operation chain,
     * including the conversions from degrees to radians.
     *
     * @throws FactoryException if an error occurred while creating a transform step.
     * @throws TransformException if a transformation failed.
     */
    @Test
    public void testConsistency() throws FactoryException, TransformException {
        transform = create();
        validate();
        isDerivativeSupported = false;
        isInverseTransformSupported = false;
        verifyInDomain(CoordinateDomain.GEOGRAPHIC, -1941624852762631518L);
        /*
         * Calculation of "expected" transform derivative by finite difference
         * does not seem accurate enough for the default accuracy. (Actually,
         * we are not completely sure that there is no bug in derivative formula).
         */
        tolerance *= 10;
        isDerivativeSupported = true;
        verifyInDomain(CoordinateDomain.GEOGRAPHIC, 4350796528249596132L);
    }

    /**
     * Tests a deserialized instance. The intent is to verify that the transient fields
     * are correctly recomputed.
     *
     * @throws FactoryException if an error occurred while creating a transform step.
     * @throws TransformException if a transformation failed.
     */
    @Test
    public void testSerialization() throws FactoryException, TransformException {
        transform = assertSerializedEquals(create());
        validate();
        isDerivativeSupported = false;
        isInverseTransformSupported = false;
        verifyInDomain(CoordinateDomain.GEOGRAPHIC, 3534897149662911157L);
    }
}
