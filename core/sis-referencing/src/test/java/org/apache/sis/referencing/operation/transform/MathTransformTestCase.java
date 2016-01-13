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

import java.util.Random;
import java.io.IOException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.metadata.Identifier;
import org.apache.sis.parameter.Parameterized;
import org.apache.sis.util.Debug;
import org.apache.sis.util.Classes;
import org.apache.sis.io.TableAppender;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.internal.util.Numerics;
import static java.lang.StrictMath.*;

// Test imports
import org.opengis.test.Validators;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.ReferencingAssert;
import static org.opengis.test.Assert.*;


/**
 * Base class for tests of {@link AbstractMathTransform} implementations.
 * This base class provides the following methods, some of them inherited from GeoAPI:
 *
 * <p>Various assertion methods:</p>
 * <ul>
 *   <li>{@link #assertCoordinateEquals assertCoordinateEquals(…)}  — from GeoAPI</li>
 *   <li>{@link #assertMatrixEquals     assertMatrixEquals(…)}      — from GeoAPI</li>
 *   <li>{@link #assertParameterEquals  assertParameterEquals(…)}   — from Apache SIS</li>
 *   <li>{@link #assertWktEquals        assertWktEquals(…)}         — from Apache SIS</li>
 * </ul>
 *
 * <p>Various test methods:</p>
 * <ul>
 *   <li>{@link #verifyConsistency(float...)}           — from GeoAPI</li>
 *   <li>{@link #verifyInverse(double...)}              — from GeoAPI</li>
 *   <li>{@link #verifyDerivative(double...)}           — from GeoAPI</li>
 *   <li>{@link #verifyInDomain verifyInDomain(…)}      — from GeoAPI</li>
 *   <li>{@link #verifyTransform(double[], double[])}   — from GeoAPI and Apache SIS</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.6
 * @module
 */
public abstract strictfp class MathTransformTestCase extends TransformTestCase {
    /**
     * The number of ordinates to use for stressing the math transform. We use a number that
     * encompass at least 2 time the default buffer size in order to test the code that use
     * the buffer. We add an arbitrary number just for making the transform job harder.
     */
    static final int ORDINATE_COUNT = AbstractMathTransform.MAXIMUM_BUFFER_SIZE * 2 + 137;

    /**
     * The dimension of longitude, or {@code null} if none. If non-null, then the comparison of
     * ordinate values along that dimension will ignore 360° offsets.
     *
     * <p>The first array element is the dimension during forward transforms, and the second
     * array element is the dimension during inverse transforms (can be omitted if the later
     * is the same than the dimension during forward transforms).</p>
     */
    protected int[] λDimension;

    /**
     * The vertical dimension, or {@code null} if none. This is the dimension for which the
     * {@link #zTolerance} value will be used rather than {@link #tolerance}.
     *
     * <p>The first array element is the dimension during forward transforms, and the second
     * array element is the dimension during inverse transforms (can be omitted if the later
     * is the same than the dimension during forward transforms).</p>
     */
    protected int[] zDimension;

    /**
     * The tolerance level for height above the ellipsoid. This tolerance is usually higher
     * than the {@linkplain #tolerance tolerance} level for horizontal ordinate values.
     */
    protected double zTolerance;

    /**
     * Creates a new test case.
     */
    protected MathTransformTestCase() {
    }

    /**
     * Returns a name for the current math transform. This method is used only for reporting errors.
     * This information is not reliable for the actual tests as the names may not be stable.
     *
     * @return A name for the current math transform.
     */
    @Debug
    private String getName() {
        if (transform instanceof Parameterized) {
            final ParameterDescriptorGroup descriptor = ((Parameterized) transform).getParameterDescriptors();
            if (descriptor != null) {
                final Identifier identifier = descriptor.getName();
                if (identifier != null) {
                    final String code = identifier.getCode();
                    if (code != null) {
                        return code;
                    }
                }
            }
        }
        return Classes.getShortClassName(transform);
    }

    /**
     * Validates the current {@linkplain #transform transform}. This method verifies that
     * the transform implements {@link MathTransform1D} or {@link MathTransform2D} if the
     * transform dimension suggests that it should. In addition, all Apache SIS transforms
     * shall implement {@link Parameterized}.
     *
     * @see Validators#validate(MathTransform)
     */
    protected final void validate() {
        assertNotNull("The 'transform' field shall be assigned a value.", transform);
        Validators.validate(transform);
        final int dimension = transform.getSourceDimensions();
        if (transform.getTargetDimensions() == dimension) {
            assertEquals("transform instanceof MathTransform1D:", (transform instanceof MathTransform1D), dimension == 1);
            assertEquals("transform instanceof MathTransform2D:", (transform instanceof MathTransform2D), dimension == 2);
        } else {
            assertFalse("transform instanceof MathTransform1D:", transform instanceof MathTransform1D);
            assertFalse("transform instanceof MathTransform2D:", transform instanceof MathTransform2D);
        }
        assertInstanceOf("The transform does not implement all expected interfaces.", Parameterized.class, transform);
    }

    /**
     * Transforms the given coordinates and verifies that the result is equals (within a positive delta)
     * to the expected ones. If the difference between an expected and actual ordinate value is greater
     * than the {@linkplain #tolerance tolerance} threshold, then the assertion fails.
     *
     * <p>If {@link #isInverseTransformSupported} is {@code true}, then this method will also transform
     * the expected coordinate points using the {@linkplain MathTransform#inverse() inverse transform} and
     * compare with the source coordinates.</p>
     *
     * <p>This method verifies also the consistency of {@code MathTransform.transform(…)} method variants.</p>
     *
     * @param  coordinates The coordinate points to transform.
     * @param  expected The expect result of the transformation, or
     *         {@code null} if {@code coordinates} is expected to be null.
     * @throws TransformException if the transformation failed.
     */
    @Override
    protected final void verifyTransform(final double[] coordinates, final double[] expected) throws TransformException {
        super.verifyTransform(coordinates, expected);
        /*
         * In addition to the GeoAPI "verifyTransform" check, check also for consistency of various variant
         * of MathTransform.transform(…) methods.  In GeoAPI, 'verifyTransform' and 'verifyConsistency' are
         * two independent steps because not all developers may want to perform both verifications together.
         * But in Apache SIS, we want to verify consistency for all math transforms. A previous Geotk version
         * had a bug with the Google projection which was unnoticed because of lack of this consistency check.
         */
        final float[] asFloats = Numerics.copyAsFloats(coordinates);
        final float[] result   = verifyConsistency(asFloats);
        for (int i=0; i<coordinates.length; i++) {
            assertEquals("Detected change in source coordinates.", (float) coordinates[i], asFloats[i], 0f); // Paranoiac check.
        }
        /*
         * The comparison below needs a higher tolerance threshold, because we converted the source
         * ordinates to floating points which induce a lost of precision. The multiplication factor
         * used here has been determined empirically. The value is quite high, but this is only an
         * oportunist check anyway. The "real" test is the one performed by 'verifyConsistency'.
         * We do not perform this check for non-linear transforms, because the difference in input
         * have too unpredictable consequences on the output.
         */
        if (transform instanceof LinearTransform) {
            for (int i=0; i<expected.length; i++) {
                final double e = expected[i];
                double tol = 1E-6 * abs(e);
                if (!(tol > tolerance)) {   // Use '!' for replacing NaN by 'tolerance'.
                    tol = tolerance;
                }
                assertEquals(e, result[i], tol);
            }
        }
    }

    /**
     * Stress the current {@linkplain #transform transform} using random ordinates in the given domain.
     * First, this method creates a grid of regularly spaced points along all dimensions in the given domain.
     * Next, this method adds small random displacements to every points and shuffle the coordinates in random order.
     * Finally this method delegates the resulting array of coordinates to the following methods:
     *
     * <ul>
     *   <li>{@link #verifyConsistency(float[])}</li>
     *   <li>{@link #verifyInverse(float[])}</li>
     *   <li>{@link #verifyDerivative(double[])}</li>
     * </ul>
     *
     * This method does not {@linkplain #validate() validate} the transform; it is caller responsibility
     * to validate if desired.
     *
     * @param  domain The domain of the numbers to be generated.
     * @param  randomSeed The seed for the random number generator, or 0 for choosing a random seed.
     * @throws TransformException If a conversion, transformation or derivative failed.
     *
     * @since 0.6
     */
    @SuppressWarnings("fallthrough")
    protected final void verifyInDomain(final CoordinateDomain domain, final long randomSeed) throws TransformException {
        final int      dimension    = transform.getSourceDimensions();
        final double[] minOrdinates = new double[dimension];
        final double[] maxOrdinates = new double[dimension];
        final int[]    numOrdinates = new int   [dimension];
        switch (dimension) {
            default: throw new UnsupportedOperationException("Too many dimensions.");
            case 3: minOrdinates[2] = domain.zmin; maxOrdinates[2] = domain.zmax; numOrdinates[2] = 3;     // Fall through
            case 2: minOrdinates[1] = domain.ymin; maxOrdinates[1] = domain.ymax; numOrdinates[1] = 8;     // Fall through
            case 1: minOrdinates[0] = domain.xmin; maxOrdinates[0] = domain.xmax; numOrdinates[0] = 8;     // Fall through
            case 0: break;
        }
        final Random random = (randomSeed == 0)
                ? TestUtilities.createRandomNumberGenerator()
                : TestUtilities.createRandomNumberGenerator(randomSeed);

        verifyInDomain(minOrdinates, maxOrdinates, numOrdinates, random);
    }

    /**
     * Generates random numbers that can be used for the current transform.
     *
     * @param  domain  The domain of the numbers to be generated.
     * @param  propNaN Approximative percentage of NaN values as a fraction between 0 and 1, or 0 if none.
     * @return Random  coordinates in the given domain.
     */
    final double[] generateRandomCoordinates(final CoordinateDomain domain, final float propNaN) {
        assertNotNull("The 'transform' field shall be assigned a value.", transform);
        final int dimension = transform.getSourceDimensions();
        final int numPts    = ORDINATE_COUNT / dimension;
        final Random random = TestUtilities.createRandomNumberGenerator();
        final double[] coordinates = domain.generateRandomInput(random, dimension, numPts);
        for (int i = round(coordinates.length * propNaN); --i >= 0;) {
            coordinates[random.nextInt(coordinates.length)] = Double.NaN;
        }
        return coordinates;
    }

    /**
     * Asserts that the parameters of current {@linkplain #transform transform} are equal to the given ones.
     * This method can check the descriptor separately, for easier isolation of mismatch in case of failure.
     *
     * @param descriptor
     *          The expected parameter descriptor, or {@code null} for bypassing this check.
     *          The descriptor is required to be strictly the same instance, since Apache SIS
     *          implementation returns constant values.
     * @param values
     *          The expected parameter values, or {@code null} for bypassing this check.
     *          Floating points values are compared in the units of the expected value,
     *          tolerating a difference up to the {@linkplain #tolerance(double) tolerance threshold}.
     */
    protected final void assertParameterEquals(final ParameterDescriptorGroup descriptor, final ParameterValueGroup values) {
        assertInstanceOf("The transform does not implement all expected interfaces.", Parameterized.class, transform);
        if (descriptor != null) {
            assertSame("transform.getParameterDescriptors():", descriptor,
                    ((Parameterized) transform).getParameterDescriptors());
        }
        if (values != null) {
            assertSame(descriptor, values.getDescriptor());
            ReferencingAssert.assertParameterEquals(values,
                    ((Parameterized) transform).getParameterValues(), tolerance);
        }
    }

    /**
     * Asserts that the current {@linkplain #transform transform} produces the given WKT.
     *
     * @param expected The expected WKT.
     *
     * @see #printInternalWKT()
     */
    protected final void assertWktEquals(final String expected) {
        assertNotNull("The 'transform' field shall be assigned a value.", transform);
        ReferencingAssert.assertWktEquals(Convention.WKT1, expected, transform);
    }

    /**
     * Asserts that the current {@linkplain #transform transform} produces a WKT matching the given regular expression.
     *
     * @param expected A regular expression for the expected WKT.
     *
     * @see #printInternalWKT()
     *
     * @since 0.6
     */
    protected final void assertWktEqualsRegex(final String expected) {
        assertNotNull("The 'transform' field shall be assigned a value.", transform);
        ReferencingAssert.assertWktEqualsRegex(Convention.WKT1, expected, transform);
    }

    /**
     * Asserts that the current {@linkplain #transform transform} produces the given internal WKT.
     *
     * @param expected The expected internal WKT.
     *
     * @since 0.7
     */
    protected final void assertInternalWktEquals(final String expected) {
        assertNotNull("The 'transform' field shall be assigned a value.", transform);
        ReferencingAssert.assertWktEquals(Convention.INTERNAL, expected, transform);
    }

    /**
     * Asserts that the current {@linkplain #transform transform} produces an internal WKT
     * matching the given regular expression.
     *
     * @param expected A regular expression for the expected internal WKT.
     *
     * @since 0.7
     */
    protected final void assertInternalWktEqualsRegex(final String expected) {
        assertNotNull("The 'transform' field shall be assigned a value.", transform);
        ReferencingAssert.assertWktEqualsRegex(Convention.INTERNAL, expected, transform);
    }

    /**
     * Prints the current {@linkplain #transform transform} as normal and internal WKT.
     * This method is for debugging purpose only.
     *
     * @see #verifyWKT(String)
     */
    @Debug
    protected final void printInternalWKT() {
        @SuppressWarnings("UseOfSystemOutOrSystemErr")
        final TableAppender table = new TableAppender(System.out);
        table.setMultiLinesCells(true);
        table.appendHorizontalSeparator();
        table.append("WKT of “").append(getName()).append('”').nextColumn();
        table.append("Internal WKT").appendHorizontalSeparator();
        String wkt;
        try {
            wkt = transform.toWKT();
        } catch (UnsupportedOperationException e) {
            wkt = transform.toString();
        }
        table.append(wkt).nextColumn();
        if (transform instanceof FormattableObject) {
            wkt = ((FormattableObject) transform).toString(Convention.INTERNAL);
        } else {
            wkt = transform.toString();
        }
        table.append(wkt).appendHorizontalSeparator();
        try {
            table.flush();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
