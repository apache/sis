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
package org.apache.sis.referencing.operation.matrix;

import java.util.Random;
import java.awt.geom.AffineTransform;
import static java.lang.StrictMath.*;
import Jama.Matrix;
import org.apache.sis.math.Statistics;
import org.apache.sis.util.internal.shared.DoubleDouble;
import org.apache.sis.referencing.internal.shared.ExtendedPrecisionMatrix;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;
import static org.apache.sis.test.Assertions.assertSerializedEquals;

// Specific to the main branch:
import static org.apache.sis.test.GeoapiAssert.assertMatrixEquals;


/**
 * Base classes of tests for {@link MatrixSIS} implementations.
 * This class uses the following {@code Matrices} factory methods:
 *
 * <ul>
 *   <li>{@link Matrices#createDiagonal(int, int)} (sometimes delegates to {@link Matrices#createIdentity(int)})</li>
 *   <li>{@link Matrices#create(int, int, double[])}</li>
 *   <li>{@link Matrices#createZero(int, int)}</li>
 * </ul>
 *
 * So this class is indirectly a test of those factory methods.
 * However, this class does not test any other {@code Matrices} methods.
 *
 * <p>This class uses <a href="http://math.nist.gov/javanumerics/jama">JAMA</a> as the reference implementation.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class MatrixTestCase extends TestCase {
    /**
     * {@code true} for reusing the same sequences of random numbers in every execution of test cases, or
     * {@code false} for "truly" random sequences of random numbers. This flag can be set to {@code false}
     * for testing purpose, but should be set to {@code true} otherwise for avoiding random test failure.
     * This is needed because we want to set {@link #TOLERANCE} to a small value, but it is very difficult
     * to guaranteed that a random sequence of numbers will not cause a larger discrepancy.
     *
     * <p>Note that this flag is set to {@code false} if double-double arithmetic is disabled because in such
     * case, the results should be identical to the JAMA results (i.e. equal using a {@link #TOLERANCE} of zero)
     * for any sequence of numbers.</p>
     */
    protected static final boolean DETERMINIST = !DoubleDouble.DISABLED;

    /**
     * Tolerance factor for comparisons of floating point numbers between SIS and JAMA implementation,
     * which is {@value}. Note that the matrix element values used in this class vary between 0 and 100,
     * and the {@code StrictMath.ulp(100.0)} value is approximately 1.4E-14.
     *
     * <h4>How this value is determined</h4>
     * Experience (by looking at {@link #statistics}) shows that the differences are usually smaller than 1E-12.
     * However, when using non-determinist sequence of random values ({@link #DETERMINIST} sets to {@code false}),
     * we do have from time-to-time a difference around 1E-9.
     *
     * Those differences exist because SIS uses double-double arithmetic, while JAMA uses ordinary double.
     * To remove that ambiguity, one can temporarily set {@link DoubleDouble#DISABLED} to {@code true},
     * in which case the SIS results should be strictly identical to the JAMA ones.
     *
     * @see SolverTest#TOLERANCE
     * @see NonSquareMatrixTest#printStatistics()
     */
    protected static final double TOLERANCE = DoubleDouble.DISABLED ? STRICT : 1E-11;

    /**
     * Number of random matrices to try in arithmetic operation tests.
     */
    static final int NUMBER_OF_REPETITIONS = 100;

    /**
     * The threshold in matrix determinant for attempting to compute the inverse.
     * Matrix with a determinant of 0 are not invertible, but we keep a margin for safety.
     */
    private static final double DETERMINANT_THRESHOLD = 0.001;

    /**
     * Statistics about the different between the JAMA and SIS matrix elements, or {@code null}
     * if those statistics do not need to be collected. This is used during the test development
     * phase for tuning the tolerance threshold.
     *
     * @see NonSquareMatrixTest#printStatistics()
     */
    static final Statistics statistics = VERBOSE ? new Statistics("|SIS - JAMA|") : null;

    /**
     * Random number generator, created by {@link #initialize(long)} as the first operation of
     * any test method which will use random numbers. This random number generator will use a
     * fixed seed if {@link #DETERMINIST} is {@code true}, which is the normal case.
     */
    private Random random;

    /**
     * For subclasses only.
     */
    MatrixTestCase() {
    }

    /**
     * Initializes the random number generator to the given seed. If {@link #DETERMINIST} is {@code false}
     * (which happen only when performing some more extensive tests), then the given seed will be replaced
     * by a random one.
     *
     * @param  seed  the initial seed.
     */
    final void initialize(final long seed) {
        random = DETERMINIST ? new Random(seed) : TestUtilities.createRandomNumberGenerator();
    }

    /**
     * Computes a random size for the next matrix to create. This method is overridden
     * only by subclasses that test matrix implementations supporting arbitrary sizes.
     *
     * @param  random  the random number generator to use for computing a random matrix size.
     */
    void prepareNewMatrixSize(final Random random) {
    }

    /** Returns the number of rows of the matrix being tested.    */ abstract int getNumRow();
    /** Returns the number of columns of the matrix being tested. */ abstract int getNumCol();

    /**
     * Validates the given matrix.
     * The default implementation verifies the matrix size. Subclasses should override this method
     * for additional checks, typically ensuring that it is an instance of the expected class.
     */
    void validateImplementation(final MatrixSIS matrix) {
        assertEquals(getNumRow(), matrix.getNumRow(), "numRow");
        assertEquals(getNumCol(), matrix.getNumCol(), "numCol");
        validate(matrix);
    }

    /**
     * Validates the given matrix by comparing the outcome of different methods.
     * This is an internal consistency check.
     *
     * @param  matrix  the matrix to validate, or {@code null} if none.
     */
    public static void validate(final org.opengis.referencing.operation.Matrix matrix) {
        if (matrix == null) {
            return;
        }
        final int numRow = matrix.getNumRow();
        final int numCol = matrix.getNumCol();
        final var extend = MatrixSIS.asExtendedPrecision(matrix);
        final Number[] numbers  = extend.getElementAsNumbers(false);
        final double[] elements = (matrix instanceof MatrixSIS) ? ((MatrixSIS) matrix).getElements() : null;
        assertEquals(numRow * numCol, numbers.length, "getElementAsNumbers");
        if (elements != null) {
            assertEquals(numbers.length, elements.length, "getElements");
        }
        for (int k=0; k<numbers.length; k++) {
            final int j = k / numCol;
            final int i = k % numCol;
            final Number number  = numbers[k];
            final double element = matrix.getElement(j, i);
            assertEquals(number, extend.getElementOrNull(j, i), "getElementOrNull");
            assertEquals(number == null, ExtendedPrecisionMatrix.isZero(number), "isZero");
            assertEquals((number != null) ? number.doubleValue() : 0, element, "getElement");
            if (elements != null) {
                assertEquals(element, elements[k], "getElements");
            }
        }
    }

    /**
     * Verifies that the SIS matrix is equal to the JAMA one, up to the given tolerance value.
     *
     * @param  expected   the JAMA matrix used as a reference implementation.
     * @param  actual     the SIS matrix to compare to JAMA.
     * @param  tolerance  the tolerance threshold, usually either {@link #STRICT} or {@link #TOLERANCE}.
     */
    static void assertEqualsJAMA(final Matrix expected, final MatrixSIS actual, final double tolerance) {
        final int numRow = actual.getNumRow();
        final int numCol = actual.getNumCol();
        assertEquals(expected.getRowDimension(),    numRow, "numRow");
        assertEquals(expected.getColumnDimension(), numCol, "numCol");
        final String name = actual.getClass().getSimpleName();
        for (int j=0; j<numRow; j++) {
            for (int i=0; i<numCol; i++) {
                final double e = expected.get(j,i);
                final double a = actual.getElement(j,i);
                assertEquals(e, a, tolerance, name);
                assertEquals(e, actual.getNumber(j,i).doubleValue(), tolerance, name);
                if (tolerance != STRICT && statistics != null) {
                    synchronized (statistics) {
                        statistics.accept(abs(e - a));
                    }
                }
            }
        }
    }

    /**
     * Asserts that the given matrix is equal to the given expected values, up to the given tolerance threshold.
     * This method compares the elements values in two slightly redundant ways.
     */
    static void assertEqualsElements(final double[] expected, final int numRow, final int numCol,
            final MatrixSIS actual, final double tolerance)
    {
        assertEquals(numRow, actual.getNumRow(), "numRow");
        assertEquals(numCol, actual.getNumCol(), "numCol");
        assertArrayEquals(expected, actual.getElements(), tolerance);           // First because more informative in case of failure.
        assertTrue(Matrices.create(numRow, numCol, expected).equals(actual, tolerance));
    }

    /**
     * Asserts that an element from the given matrix is equal to the expected value, using a relative threshold.
     */
    private static void assertEqualsRelative(final String message, final double expected,
            final MatrixSIS matrix, final int row, final int column)
    {
        assertEquals(expected, matrix.getElement(row, column), abs(expected) * 1E-12, message);
    }

    /**
     * Returns the next random number as a value between approximately -100 and 100
     * with the guarantee to be different than zero. The values returned by this method
     * are suitable for testing scale factors.
     */
    private double nextNonZeroRandom() {
        double value = random.nextDouble() * 200 - 100;
        value += copySign(0.001, value);
        if (random.nextBoolean()) {
            value = 1 / value;
        }
        return value;
    }

    /**
     * Creates an array of the given length filled with random values. All random values are between 0 inclusive
     * and 100 exclusive. This method never write negative values. Consequently, any strictly negative value set
     * by the test method is guaranteed to be different than all original values in the returned array.
     */
    final double[] createRandomPositiveValues(final int length) {
        final double[] elements = new double[length];
        for (int k=0; k<length; k++) {
            elements[k] = random.nextDouble() * 100;
        }
        return elements;
    }

    /**
     * Creates a matrix initialized with a random array of element values,
     * then tests the {@link MatrixSIS#getElement(int, int)} method for each element.
     * This test will use {@link Matrices#create(int, int, double[])} for creating the matrix.
     *
     * <p>If this test fails, then all other tests in this class will be skipped since it would
     * not be possible to verify the result of any matrix operation.</p>
     */
    @Test
    public void testGetElements() {
        initialize(3812872376135347328L);
        prepareNewMatrixSize(random);
        final int numRow = getNumRow();
        final int numCol = getNumCol();
        final double[] elements = createRandomPositiveValues(numRow * numCol);
        final MatrixSIS matrix = Matrices.create(numRow, numCol, elements);
        validateImplementation(matrix);
        /*
         * The JAMA constructor uses column-major array (FORTRAN convention), while SIS uses
         * row-major array. So we have to transpose the JAMA matrix after construction.
         */
        assertEqualsJAMA(new Matrix(elements, numCol).transpose(), matrix, STRICT);
        assertArrayEquals(elements, matrix.getElements(), "getElements");
    }

    /**
     * Tests {@link MatrixSIS#getElement(int, int)} and {@link MatrixSIS#setElement(int, int, double)}.
     * This test sets random values in elements at random index, and compares with a JAMA matrix taken
     * as the reference implementation.
     */
    @Test
    public void testSetElement() {
        initialize(-8079924100564483073L);
        prepareNewMatrixSize(random);
        final int numRow = getNumRow();
        final int numCol = getNumCol();
        final MatrixSIS matrix = Matrices.createZero(numRow, numCol);
        validateImplementation(matrix);
        final Matrix reference = new Matrix(numRow, numCol);
        /*
         * End of initialization - now perform the actual test.
         */
        assertEqualsJAMA(reference, matrix, STRICT);
        for (int k=0; k<NUMBER_OF_REPETITIONS; k++) {
            final int    j = random.nextInt(numRow);
            final int    i = random.nextInt(numCol);
            final double e = random.nextDouble() * 100;
            reference.set(j, i, e);
            matrix.setElement(j, i, e);
            assertEqualsJAMA(reference, matrix, STRICT);
        }
    }

    /**
     * Tests {@link MatrixSIS#isIdentity()}. This method will first invoke {@link Matrices#createDiagonal(int, int)}
     * and ensure that the result contains 1 on the diagonal and 0 elsewhere.
     *
     * <p>This method will opportunistically tests {@link MatrixSIS#isAffine()}. The two methods are related
     * since {@code isIdentity()} delegates part of its work to {@code isAffine()}.</p>
     */
    @Test
    public void testIsIdentity() {
        initialize(6173145457052452823L);
        prepareNewMatrixSize(random);
        final int numRow = getNumRow();
        final int numCol = getNumCol();
        final MatrixSIS matrix = Matrices.createDiagonal(numRow, numCol);
        validateImplementation(matrix);
        /*
         * End of initialization - now perform the actual test.
         */
        assertEquals(numRow == numCol, matrix.isAffine());
        assertEquals(numRow == numCol, matrix.isIdentity());
        for (int j=0; j<numRow; j++) {
            for (int i=0; i<numCol; i++) {
                final double element = matrix.getElement(j,i);
                assertEquals((i == j) ? 1 : 0, element);
                matrix.setElement(j, i, random.nextDouble() - 1.1);
                assertEquals((numRow == numCol) && (j != numRow-1), matrix.isAffine());
                assertFalse(matrix.isIdentity());
                matrix.setElement(j, i, element);
            }
        }
        assertEquals(numRow == numCol, matrix.isAffine());
        assertEquals(numRow == numCol, matrix.isIdentity());
    }

    /**
     * Tests {@link MatrixSIS#clone()}, {@link MatrixSIS#equals(Object)} and {@link MatrixSIS#hashCode()}.
     */
    @Test
    public void testCloneEquals() {
        initialize(-4572234104840706847L);
        prepareNewMatrixSize(random);
        final int numRow = getNumRow();
        final int numCol = getNumCol();
        final double[] elements = createRandomPositiveValues(numRow * numCol);
        final MatrixSIS matrix = Matrices.create(numRow, numCol, elements);
        final MatrixSIS clone  = matrix.clone();
        validateImplementation(matrix);
        validateImplementation(clone);
        assertNotSame(matrix, clone);
        assertEquals(matrix, clone);
        assertEquals(matrix.hashCode(), clone.hashCode());
        for (int j=0; j<numRow; j++) {
            for (int i=0; i<numCol; i++) {
                final double element = clone.getElement(j,i);
                clone.setElement(j, i, random.nextDouble() - 2);    // Negative value is guaranteed to be different.
                assertFalse(matrix.equals(clone));
                assertFalse(clone.equals(matrix));
                clone.setElement(j, i, element);
            }
        }
        assertEquals(matrix, clone);
    }

    /**
     * Tests {@link MatrixSIS#transpose()}.
     */
    @Test
    public void testTranspose() {
        initialize(585037875560696050L);
        prepareNewMatrixSize(random);
        final int numRow = getNumRow();
        final int numCol = getNumCol();
        final double[] elements = createRandomPositiveValues(numRow * numCol);
        final MatrixSIS matrix = Matrices.create(numRow, numCol, elements);
        validateImplementation(matrix);
        /*
         * The JAMA constructor uses column-major array (FORTRAN convention) while SIS uses row-major
         * array. In other words, the JAMA matrix is already transposed from the SIS point of view.
         */
        matrix.transpose();
        assertEqualsJAMA(new Matrix(elements, numCol), matrix, STRICT);
    }

    /**
     * Tests {@link MatrixSIS#normalizeColumns()}.
     */
    @Test
    public void testNormalizeColumns() {
        initialize(1549772118153010333L);
        prepareNewMatrixSize(random);
        final int numRow = getNumRow();
        final int numCol = getNumCol();
        final double[] elements = createRandomPositiveValues(numRow * numCol);
        final MatrixSIS matrix = Matrices.create(numRow, numCol, elements);
        validateImplementation(matrix);
        final double[] expected = new double[numCol];
        for (int i=0; i<numCol; i++) {
            expected[i] = magnitude(matrix, i, numRow);
        }
        assertArrayEquals(expected, matrix.normalizeColumns().getElements(), 1E-12);
        /*
         * Array elements after normalization shall be unit vectors.
         */
        for (int i=0; i<numCol; i++) {
            assertEquals(1, magnitude(matrix, i, numRow), 1E-12);
        }
    }

    /**
     * Computes the magnitude of vector in column <var>i</var>.
     */
    private static double magnitude(final MatrixSIS matrix, final int i, final int numRow) {
        double sum = 0;
        for (int j=0; j<numRow; j++) {
            final double e = matrix.getElement(j, i);
            sum += e*e;
        }
        return sqrt(sum);
    }

    /**
     * Tests {@link MatrixSIS#convertBefore(int, Number, Number)} using {@link AffineTransform}
     * as a reference implementation. This test can be run only with matrices of size 3×3.
     * Consequently, it is sub-classes responsibility to add a {@code testConvertBefore()} method
     * which invoke this method.
     *
     * @param  matrix     the matrix of size 3×3 to test.
     * @param  withShear  {@code true} for including shear in the matrix to test.
     *                    This value can be set to {@code false} if the subclass want to test a simpler case.
     */
    final void testConvertBefore(final MatrixSIS matrix, final boolean withShear) {
        initialize(4599164481916500056L);
        final AffineTransform at = new AffineTransform();
        if (withShear) {
            at.shear(nextNonZeroRandom(), nextNonZeroRandom());
            matrix.setElement(0, 1, at.getShearX());
            matrix.setElement(1, 0, at.getShearY());
        }
        for (int i=0; i<NUMBER_OF_REPETITIONS; i++) {
            /*
             * 1) For the first  30 iterations, test the result of applying only a scale.
             * 2) For the next   30 iterations, test the result of applying only a translation.
             * 3) For all remaining iterations, test combination of scale and translation.
             */
            final Number scale  = (i >= 60 || i < 30) ? nextNonZeroRandom() : null;
            final Number offset = (i >= 30)           ? nextNonZeroRandom() : null;
            /*
             * Apply the scale and offset on the affine transform, which we use as the reference
             * implementation. The scale and offset must be applied in the exact same order as
             * the order documented in MatrixSIS.concatenate(…) javadoc.
             */
            final int srcDim = (i & 1);
            if (offset != null) {
                switch (srcDim) {
                    case 0: at.translate(offset.doubleValue(), 0); break;
                    case 1: at.translate(0, offset.doubleValue()); break;
                }
            }
            if (scale != null) {
                switch (srcDim) {
                    case 0: at.scale(scale.doubleValue(), 1); break;
                    case 1: at.scale(1, scale.doubleValue()); break;
                }
            }
            /*
             * Apply the operation and compare with our reference implementation.
             */
            matrix.convertBefore(srcDim, scale, offset);
            assertCoefficientsEqual(at, matrix);
            validateImplementation(matrix);
        }
    }

    /**
     * Asserts that the given matrix has approximately the same coefficients as the given affine transform.
     */
    private static void assertCoefficientsEqual(final AffineTransform at, final MatrixSIS matrix) {
        assertEqualsRelative("m20",        0,                  matrix, 2, 0);
        assertEqualsRelative("m21",        0,                  matrix, 2, 1);
        assertEqualsRelative("m22",        1,                  matrix, 2, 2);
        assertEqualsRelative("translateX", at.getTranslateX(), matrix, 0, 2);
        assertEqualsRelative("translateY", at.getTranslateY(), matrix, 1, 2);
        assertEqualsRelative("scaleX",     at.getScaleX(),     matrix, 0, 0);
        assertEqualsRelative("scaleY",     at.getScaleY(),     matrix, 1, 1);
        assertEqualsRelative("shearX",     at.getShearX(),     matrix, 0, 1);
        assertEqualsRelative("shearY",     at.getShearY(),     matrix, 1, 0);
        assertTrue(matrix.isAffine());
    }

    /**
     * Tests {@link MatrixSIS#convertAfter(int, Number, Number)} using {@link AffineTransform}
     * as a reference implementation. This test can be run only with matrices of size 3×3.
     * Consequently, it is sub-classes responsibility to add a {@code testConvertAfter()} method
     * which invoke this method.
     *
     * @param  matrix  the matrix of size 3×3 to test.
     */
    final void testConvertAfter(final MatrixSIS matrix) {
        initialize(6501103578268988251L);
        final AffineTransform pre = new AffineTransform();
        final AffineTransform at = AffineTransform.getShearInstance(nextNonZeroRandom(), nextNonZeroRandom());
        matrix.setElement(0, 1, at.getShearX());
        matrix.setElement(1, 0, at.getShearY());
        for (int i=0; i<NUMBER_OF_REPETITIONS; i++) {
            final Number scale  = nextNonZeroRandom();
            final Number offset = nextNonZeroRandom();
            final int tgtDim = (i & 1);
            switch (tgtDim) {
                default: pre.setToIdentity();
                         break;
                case 0:  pre.setToTranslation(offset.doubleValue(), 0);
                         pre.scale(scale.doubleValue(), 1);
                         break;
                case 1:  pre.setToTranslation(0, offset.doubleValue());
                         pre.scale(1, scale.doubleValue());
                         break;
            }
            at.preConcatenate(pre);
            matrix.convertAfter(tgtDim, scale, offset);
            assertCoefficientsEqual(at, matrix);
            validateImplementation(matrix);
        }
    }

    /**
     * Tests {@link MatrixSIS#translate(double[])} using {@link AffineTransform} as a reference implementation.
     * This test can be run only with matrices of size 3×3. Consequently, it is sub-classes responsibility to add
     * a {@code testTranslateVector()} method which invoke this method.
     *
     * @param  matrix  an initially empty matrix of size 3×3 to test.
     */
    final void testTranslateVector(final MatrixSIS matrix) {
        initialize(-1691066807752485433L);
        final AffineTransform at = new AffineTransform();
        final double vector[] = new double[] {0, 0, 1};
        for (int n=0; n<NUMBER_OF_REPETITIONS; n++) {
            if ((n % 10) == 0) {
                setRandomValues(at, matrix);
            }
            at.translate(vector[0] = fma(random.nextDouble(), 50, -25),
                         vector[1] = fma(random.nextDouble(), 50, -25));
            matrix.translate(vector);
            assertMatrixEquals(AffineTransforms2D.toMatrix(at), matrix, TOLERANCE, "translate");
            validateImplementation(matrix);
        }
    }

    /**
     * Sets random values in the given affine transform and a copy of those values in the given matrix.
     */
    private void setRandomValues(final AffineTransform at, final MatrixSIS matrix) {
        at.setToRotation(random.nextDouble() * PI);
        at.scale(nextNonZeroRandom(), nextNonZeroRandom());
        at.translate(fma(random.nextDouble(), 100, - 50),
                     fma(random.nextDouble(), 100, - 50));
        matrix.setElements(new double[] {
            at.getScaleX(), at.getShearX(), at.getTranslateX(),
            at.getShearY(), at.getScaleY(), at.getTranslateY(),
                         0,              0,                  1
        });
    }

    /**
     * Tests {@link MatrixSIS#multiply(double[])} using {@link AffineTransform} as a reference implementation.
     * This test can be run only with matrices of size 3×3. Consequently, it is sub-classes responsibility to add
     * a {@code testMultiplyVector()} method which invoke this method.
     *
     * @param  matrix  an initially empty matrix of size 3×3 to test.
     */
    final void testMultiplyVector(final MatrixSIS matrix) {
        initialize(8433903323905121506L);
        final AffineTransform at = new AffineTransform();
        final double vector[] = new double[3];
        for (int n=0; n<NUMBER_OF_REPETITIONS; n++) {
            if ((n % 10) == 0) {
                setRandomValues(at, matrix);
            }
            vector[0] = fma(random.nextDouble(), 50, -25);
            vector[1] = fma(random.nextDouble(), 50, -25);
            vector[2] = 1;
            final double[] result = matrix.multiply(vector);        // The result to verify.
            at.transform(vector, 0, vector, 0, 1);                  // The expected result.
            assertEquals(vector[0], result[0], TOLERANCE);
            assertEquals(vector[1], result[1], TOLERANCE);
            validateImplementation(matrix);
        }
    }

    /**
     * Tests {@link MatrixSIS#multiply(org.opengis.referencing.operation.Matrix)}.
     */
    @Test
    public void testMultiply() {
        initialize(2478887638739725150L);
        for (int n=0; n<NUMBER_OF_REPETITIONS; n++) {
            prepareNewMatrixSize(random);
            final int numRow = getNumRow();
            final int numCol = getNumCol();
            double[] elements = createRandomPositiveValues(numRow * numCol);
            final MatrixSIS matrix = Matrices.create(numRow, numCol, elements);
            final Matrix reference = new Matrix(elements, numCol).transpose();
            /*
             * Computes new random value for the argument. We mix positive and negative values,
             * but with more positive values than negative ones in order to reduce the chances
             * to have a product of zero for an element.
             */
            final int nx = random.nextInt(8) + 1;
            elements = new double[numCol * nx];
            for (int k=0; k<elements.length; k++) {
                elements[k] = fma(random.nextDouble(), -10, 8);
            }
            final Matrix referenceArg = new Matrix(elements, nx).transpose();
            final MatrixSIS matrixArg = Matrices.create(numCol, nx, elements);
            /*
             * Performs the multiplication and compare.
             */
            final Matrix referenceResult = reference.times(referenceArg);
            final MatrixSIS matrixResult = matrix.multiply(matrixArg);
            assertEqualsJAMA(referenceResult, matrixResult, TOLERANCE);
            validateImplementation(matrix);
        }
    }

    /**
     * Tests {@link MatrixSIS#solve(org.opengis.referencing.operation.Matrix)}.
     *
     * @throws NoninvertibleMatrixException if the matrix cannot be inverted.
     */
    @Test
    public void testSolve() throws NoninvertibleMatrixException {
        initialize(2108474073121762243L);
        for (int n=0; n<NUMBER_OF_REPETITIONS; n++) {
            prepareNewMatrixSize(random);
            final int numRow = getNumRow();
            final int numCol = getNumCol();
            double[] elements = createRandomPositiveValues(numRow * numCol);
            final Matrix reference = new Matrix(elements, numCol).transpose();
            if (!(reference.det() >= DETERMINANT_THRESHOLD)) {
                continue;               // Too close to a singular matrix - search another one.
            }
            final MatrixSIS matrix = Matrices.create(numRow, numCol, elements);
            /*
             * Computes new random value for the argument. We mix positive and negative values,
             * but with more positive values than negative ones in order to reduce the chances
             * to have a product of zero for an element.
             */
            final int nx = random.nextInt(8) + 1;
            elements = new double[numCol * nx];
            for (int k=0; k<elements.length; k++) {
                elements[k] = fma(random.nextDouble(), -10, 8);
            }
            final Matrix referenceArg = new Matrix(elements, nx).transpose();
            final MatrixSIS matrixArg = Matrices.create(numCol, nx, elements);
            /*
             * Performs the operation and compare.
             */
            final Matrix referenceResult = reference.solve(referenceArg);
            final MatrixSIS matrixResult = matrix.solve(matrixArg);
            assertEqualsJAMA(referenceResult, matrixResult, SolverTest.TOLERANCE);
            validateImplementation(matrix);
        }
    }

    /**
     * Tests {@link MatrixSIS#inverse()}.
     * SIS implements the {@code inverse} operation as a special case of the {@code solve} operation.
     *
     * @throws NoninvertibleMatrixException if the matrix cannot be inverted.
     */
    @Test
    public void testInverse() throws NoninvertibleMatrixException {
        initialize(-9063921123024549789L);
        for (int n=0; n<NUMBER_OF_REPETITIONS; n++) {
            prepareNewMatrixSize(random);
            final int numRow = getNumRow();
            final int numCol = getNumCol();
            final double[] elements = createRandomPositiveValues(numRow * numCol);
            final Matrix reference = new Matrix(elements, numCol).transpose();
            if (!(reference.det() >= DETERMINANT_THRESHOLD)) {
                continue;               // Too close to a singular matrix - search another one.
            }
            final MatrixSIS matrix = Matrices.create(numRow, numCol, elements);
            assertEqualsJAMA(reference.inverse(), matrix.inverse(), TOLERANCE);
            validateImplementation(matrix);
        }
    }

    /**
     * Tests matrix serialization.
     */
    @Test
    public void testSerialization() {
        initialize(-3232759118744327281L);
        prepareNewMatrixSize(random);
        final int numRow = getNumRow();
        final int numCol = getNumCol();
        final MatrixSIS matrix = Matrices.create(numRow, numCol, createRandomPositiveValues(numRow * numCol));
        assertNotSame(matrix, assertSerializedEquals(matrix));
        validateImplementation(matrix);
    }
}
