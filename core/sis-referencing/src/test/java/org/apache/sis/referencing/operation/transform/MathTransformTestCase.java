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
import java.io.PrintStream;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.geometry.DirectPosition;
import org.opengis.metadata.Identifier;
import org.apache.sis.parameter.Parameterized;
import org.apache.sis.util.Debug;
import org.apache.sis.util.Classes;
import org.apache.sis.math.Statistics;
import org.apache.sis.math.StatisticsFormat;
import org.apache.sis.io.TableAppender;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.internal.util.Numerics;
import static java.lang.StrictMath.*;

// Test imports
import org.opengis.test.Validators;
import org.opengis.test.referencing.TransformTestCase;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;
import static org.apache.sis.test.ReferencingAssert.*;

// Branch-dependent imports
import org.opengis.test.CalculationType;
import org.opengis.test.ToleranceModifier;


/**
 * Base class for tests of {@link AbstractMathTransform} implementations.
 * This base class inherits the convenience methods defined in GeoAPI and adds a few {@code verifyFoo} methods.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5 (derived from geotk-2.0)
 * @version 0.5
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
     * An optional message to pre-concatenate to the error message if one of the {@code assert}
     * methods fail. This field shall contain information about the test configuration that may
     * be useful in determining the cause of a test failure.
     */
    protected String messageOnFailure;

    /**
     * Creates a new test case.
     */
    protected MathTransformTestCase() {
        /*
         * Use 'zTolerance' threshold instead of 'tolerance' when comparing vertical coordinate values.
         */
        toleranceModifier = new ToleranceModifier() {
            @Override
            public void adjust(final double[] tolerance, final DirectPosition coordinate, final CalculationType mode) {
                if (mode != CalculationType.IDENTITY) {
                    final int i = forComparison(zDimension, mode);
                    if (i >= 0 && i < tolerance.length) {
                        tolerance[i] = zTolerance;
                    }
                }
            }
        };
    }

    /**
     * Returns the value to use from the {@link #λDimension} or {@link zDimension} for the
     * given comparison mode, or -1 if none.
     */
    @SuppressWarnings("fallthrough")
    static int forComparison(final int[] config, final CalculationType mode) {
        if (config != null) {
            switch (mode) {
                case INVERSE_TRANSFORM: if (config.length >= 2) return config[1]; // Intentional fallthrough.
                case DIRECT_TRANSFORM:  if (config.length >= 1) return config[0];
            }
        }
        return -1;
    }

    /**
     * Invoked by all {@code assertCoordinateEqual(…)} methods before two positions are compared.
     * The SIS implementation ensures that longitude values are contained in the ±180° range,
     * applying 360° shifts if needed.
     *
     * @param expected The expected ordinate value provided by the test case.
     * @param actual   The ordinate value computed by the {@linkplain #transform transform} being tested.
     * @param mode     Indicates if the coordinates being compared are the result of a direct
     *                 or inverse transform, or if strict equality is requested.
     */
    @Override
    protected final void normalize(final DirectPosition expected, final DirectPosition actual, final CalculationType mode) {
        final int i = forComparison(λDimension, mode);
        if (i >= 0) {
            double e;
            e = expected.getOrdinate(i); e -= 360*floor(e/360); expected.setOrdinate(i, e);
            e =   actual.getOrdinate(i); e -= 360*floor(e/360);   actual.setOrdinate(i, e);
        }
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
     * Completes the error message by pre-concatenating {@link #messageOnFailure} if non-null.
     */
    private String completeMessage(final String message) {
        if (messageOnFailure == null) {
            return message;
        }
        final String lineSeparator = System.lineSeparator();
        // Note: JUnit message will begin with a space.
        return messageOnFailure + lineSeparator + message + lineSeparator + "JUnit message:";
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
            assertEquals(completeMessage("MathTransform1D"), dimension == 1, (transform instanceof MathTransform1D));
            assertEquals(completeMessage("MathTransform2D"), dimension == 2, (transform instanceof MathTransform2D));
        } else {
            assertFalse(completeMessage("MathTransform1D"), transform instanceof MathTransform1D);
            assertFalse(completeMessage("MathTransform2D"), transform instanceof MathTransform2D);
        }
        assertInstanceOf(completeMessage("Parameterized"), Parameterized.class, transform);
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
    protected final void verifyParameters(final ParameterDescriptorGroup descriptor, final ParameterValueGroup values) {
        assertInstanceOf(completeMessage("TransformTestCase.transform"), Parameterized.class, transform);
        if (descriptor != null) {
            assertSame("ParameterDescriptor", descriptor, ((Parameterized) transform).getParameterDescriptors());
        }
        if (values != null) {
            assertSame(descriptor, values.getDescriptor());
            assertParameterEquals(values, ((Parameterized) transform).getParameterValues(), tolerance);
        }
    }

    /**
     * Verifies if {@link MathTransform#isIdentity()} on the current {@linkplain #transform transform}.
     * If the current transform is linear, then this method will also verifies {@link Matrix#isIdentity()}.
     *
     * @param expected The expected return value of {@code isIdentit()} methods.
     */
    protected final void verifyIsIdentity(final boolean expected) {
        assertEquals(completeMessage("isIdentity()"), expected, transform.isIdentity());
        if (transform instanceof LinearTransform) {
            assertEquals(completeMessage("getMatrix().isIdentity()"), expected,
                    ((LinearTransform) transform).getMatrix().isIdentity());
        }
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
         * But in Apache SIS, we want to verify consistency for all math transform. A previous Geotk version
         * had a bug with the Google projection which was unnoticed because of lack of this consistency check.
         */
        final float[] asFloats = Numerics.copyAsFloats(coordinates);
        final float[] result   = verifyConsistency(asFloats);
        final String  message  = completeMessage("Detected change in source coordinates.");
        for (int i=0; i<coordinates.length; i++) {
            assertEquals(message, (float) coordinates[i], asFloats[i], 0f); // Paranoiac check.
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
     * Generates random numbers that can be used for the current transform.
     *
     * @param  domain  The domain of the numbers to be generated.
     * @param  propNaN Approximative percentage of NaN values as a fraction between 0 and 1, or 0 if none.
     * @return Random  coordinates in the given domain.
     */
    protected final double[] generateRandomCoordinates(final CoordinateDomain domain, final float propNaN) {
        assertNotNull("Transform field must be assigned a value.", transform);
        final int dimension = transform.getSourceDimensions();
        final int numPts    = ORDINATE_COUNT / dimension;
        final Random random = TestUtilities.createRandomNumberGenerator();
        final double[] coordinates = domain.generateRandomInput(random, dimension, numPts);
        for (int i = round(coordinates.length * propNaN); --i >= 0;) {
            coordinates[random.nextInt(coordinates.length)] = Double.NaN;
        }
        if (TestCase.verbose) {
            final PrintStream out = out();
            out.print("Random input coordinates for ");
            out.print(domain); out.println(" domain:");
            final Statistics[] stats = new Statistics[dimension];
            for (int i=0; i<stats.length; i++) {
                stats[i] = new Statistics(null);
            }
            for (int i=0; i<coordinates.length; i++) {
                stats[i % dimension].accept(coordinates[i]);
            }
            final StatisticsFormat format = StatisticsFormat.getInstance();
            format.setBorderWidth(1);
            try {
                format.format(stats, out);
            } catch (IOException e) {
                throw new AssertionError(e);
            }
            out.println();
            out.flush();
        }
        return coordinates;
    }

    /**
     * Asserts that the current {@linkplain #transform transform} produces the given WKT.
     *
     * @param expected The expected WKT.
     *
     * @see #printInternalWKT()
     */
    protected final void verifyWKT(final String expected) {
        assertNotNull("Transform field must be assigned a value.", transform);
        assertEquals("WKT comparison with tolerance not yet implemented.", 0.0, tolerance, 0.0);
        assertWktEquals(Convention.WKT1, expected, transform);
    }

    /**
     * Prints the current {@linkplain #transform transform} as normal and internal WKT.
     * This method is for debugging purpose only.
     *
     * @see #verifyWKT(String)
     */
    @Debug
    protected final void printInternalWKT() {
        final TableAppender table = new TableAppender(out());
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

    /**
     * Where to write debugging information.
     */
    @Debug
    private static PrintStream out() {
        return System.out;
    }
}
