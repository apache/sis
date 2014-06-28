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

import java.util.List;
import java.util.Arrays;
import java.io.Serializable;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.SingleOperation;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.parameter.Parameterized;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.internal.referencing.WKTUtilities;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.ArgumentChecks.ensureDimensionMatches;


/**
 * Provides a default implementation for most methods required by the {@link MathTransform} interface.
 * {@code AbstractMathTransform} provides a convenient base class from which transform implementations
 * can be easily derived. It also defines a few additional SIS-specific methods for convenience of performance.
 *
 * <p>The simplest way to implement this abstract class is to provide an implementation for the following methods
 * only:</p>
 * <ul>
 *   <li>{@link #getSourceDimensions()}</li>
 *   <li>{@link #getTargetDimensions()}</li>
 *   <li>{@link #transform(double[], int, double[], int, boolean)}</li>
 * </ul>
 *
 * However more performance may be gained by overriding the other {@code transform} methods as well.
 *
 * {@section Immutability and thread safety}
 * All Apache SIS implementations of {@code MathTransform} are immutable and thread-safe.
 * It is highly recommended that third-party implementations be immutable and thread-safe too.
 * This means that unless otherwise noted in the javadoc, {@code MathTransform} instances can
 * be shared by many objects and passed between threads without synchronization.
 *
 * {@section Serialization}
 * {@code MathTransform} may or may not be serializable, at implementation choices.
 * Most Apache SIS implementations are serializable, but the serialized objects are not guaranteed to be compatible
 * with future SIS versions. Serialization should be used only for short term storage or RMI between applications
 * running the same SIS version.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.5 (derived from geotk-1.2)
 * @version 0.5
 * @module
 */
public abstract class AbstractMathTransform extends FormattableObject
        implements MathTransform, Parameterized, LenientComparable
{
    /**
     * Maximum buffer size when creating temporary arrays. Must not be too big, otherwise the
     * cost of allocating the buffer may be greater than the benefit of transforming array of
     * coordinates. Remember that double number occupy 8 bytes, so a buffer of size 512 will
     * actually consumes 4 kb of memory.
     */
    static final int MAXIMUM_BUFFER_SIZE = 512;

    /**
     * Maximum amount of {@link TransformException} to catch while transforming a block of
     * {@value #MAXIMUM_BUFFER_SIZE} ordinate values in an array. The default implementation of
     * {@code transform} methods set un-transformable coordinates to {@linkplain Double#NaN NaN}
     * before to let the exception propagate. However if more then {@value} exceptions occur in
     * a block of {@value #MAXIMUM_BUFFER_SIZE} <em>ordinates</em> (not coordinates), then we
     * will give up. We put a limit in order to avoid slowing down the application too much if
     * a whole array is not transformable.
     *
     * <p>Note that in case of failure, the first {@code TransformException} is still propagated;
     * we do not "eat" it. We just set the ordinates to {@code NaN} before to let the propagation
     * happen. If no exception handling should be performed at all, then {@code MAXIMUM_FAILURES}
     * can be set to 0.</p>
     *
     * <p>Having {@code MAXIMUM_BUFFER_SIZE} sets to 512 and {@code MAXIMUM_FAILURES} sets to 32
     * means that we tolerate about 6.25% of un-transformable points.</p>
     */
    static final int MAXIMUM_FAILURES = 32;

    /**
     * The cached hash code value, or 0 if not yet computed. This field is calculated only when
     * first needed. We do not declare it {@code volatile} because it is not a big deal if this
     * field is calculated many times, and the same value should be produced by all computations.
     * The only possible outdated value is 0, which is okay.
     */
    private transient int hashCode;

    /**
     * Constructor for subclasses.
     */
    protected AbstractMathTransform() {
    }

    /**
     * Gets the dimension of input points.
     *
     * @return The dimension of input points.
     */
    @Override
    public abstract int getSourceDimensions();

    /**
     * Gets the dimension of output points.
     *
     * @return The dimension of output points.
     */
    @Override
    public abstract int getTargetDimensions();

    /**
     * Returns the parameter descriptors for this math transform, or {@code null} if unknown.
     *
     * <span class="note"><b>Relationship with ISO 19111:</b>
     * This method is similar to {@link OperationMethod#getParameters()}, except that typical
     * {@link MathTransform} implementations return parameters in standard units (usually
     * {@linkplain SI#METRE metres} or {@linkplain NonSI#DEGREE_ANGLE decimal degrees}).
     * </span>
     *
     * @return The parameter descriptors for this math transform, or {@code null}.
     *
     * @see OperationMethod#getParameters()
     */
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        return null;
    }

    /**
     * Returns a copy of the parameter values for this math transform, or {@code null} if unknown.
     * Since this method returns a copy of the parameter values, any change to a value will have no
     * effect on this math transform.
     *
     * <span class="note"><b>Relationship with ISO 19111:</b>
     * This method is similar to {@link SingleOperation#getParameterValues()}, except that typical
     * {@link MathTransform} implementations return parameters in standard units (usually
     * {@linkplain SI#METRE metres} or {@linkplain NonSI#DEGREE_ANGLE decimal degrees}).
     * </span>
     *
     * @return A copy of the parameter values for this math transform, or {@code null}.
     *
     * @see SingleOperation#getParameterValues()
     */
    @Override
    public ParameterValueGroup getParameterValues() {
        return null;
    }

    /**
     * Tests whether this transform does not move any points.
     * The default implementation always returns {@code false}.
     */
    @Override
    public boolean isIdentity() {
        return false;
    }

    /**
     * Constructs an error message for the {@link MismatchedDimensionException}.
     *
     * @param argument  The argument name with the wrong number of dimensions.
     * @param expected  The expected dimension.
     * @param dimension The wrong dimension.
     */
    static String mismatchedDimension(final String argument, final int expected, final int dimension) {
        return Errors.format(Errors.Keys.MismatchedDimension_3, argument, expected, dimension);
    }

    /**
     * Transforms the specified {@code ptSrc} and stores the result in {@code ptDst}.
     * The default implementation performs the following steps:
     *
     * <ul>
     *   <li>Ensures that the dimension of the given points are consistent with the
     *       {@linkplain #getSourceDimensions() source} and {@linkplain #getTargetDimensions()
     *       target dimensions} of this math transform.</li>
     *   <li>Delegates to the {@link #transform(double[], int, double[], int, boolean)} method.</li>
     * </ul>
     *
     * @param  ptSrc the coordinate point to be transformed.
     * @param  ptDst the coordinate point that stores the result of transforming {@code ptSrc}, or {@code null}.
     * @return the coordinate point after transforming {@code ptSrc} and storing the result in {@code ptDst},
     *         or a newly created point if {@code ptDst} was null.
     * @throws MismatchedDimensionException if {@code ptSrc} or {@code ptDst} doesn't have the expected dimension.
     * @throws TransformException if the point can not be transformed.
     */
    @Override
    public DirectPosition transform(final DirectPosition ptSrc, DirectPosition ptDst) throws TransformException {
        final int dimSource = getSourceDimensions();
        final int dimTarget = getTargetDimensions();
        ensureDimensionMatches("ptSrc", dimSource, ptSrc);
        if (ptDst != null) {
            ensureDimensionMatches("ptDst", dimTarget, ptDst);
            /*
             * Transforms the coordinates using a temporary 'double[]' buffer,
             * and copies the transformation result in the destination position.
             */
            final double[] array;
            if (dimSource >= dimTarget) {
                array = ptSrc.getCoordinate();
            } else {
                array = new double[dimTarget];
                for (int i=dimSource; --i>=0;) {
                    array[i] = ptSrc.getOrdinate(i);
                }
            }
            transform(array, 0, array, 0, false);
            for (int i=0; i<dimTarget; i++) {
                ptDst.setOrdinate(i, array[i]);
            }
        } else {
            /*
             * Destination not set. We are going to create the destination here. Since we know that the
             * destination will be the SIS implementation, write directly into the 'ordinates' array.
             */
            final GeneralDirectPosition destination = new GeneralDirectPosition(dimTarget);
            final double[] source;
            if (dimSource <= dimTarget) {
                source = destination.ordinates;
                for (int i=0; i<dimSource; i++) {
                    source[i] = ptSrc.getOrdinate(i);
                }
            } else {
                source = ptSrc.getCoordinate();
            }
            transform(source, 0, destination.ordinates, 0, false);
            ptDst = destination;
        }
        return ptDst;
    }

    /**
     * Transforms a single coordinate point in an array, and optionally computes the transform
     * derivative at that location. Invoking this method is conceptually equivalent to running
     * the following:
     *
     * {@preformat java
     *     Matrix derivative = null;
     *     if (derivate) {
     *         double[] ordinates = Arrays.copyOfRange(srcPts, srcOff, srcOff + getSourceDimensions());
     *         derivative = this.derivative(new GeneralDirectPosition(ordinates));
     *     }
     *     this.transform(srcPts, srcOff, dstPts, dstOff, 1);  // May overwrite srcPts.
     *     return derivative;
     * }
     *
     * However this method provides two advantages:
     *
     * <ul>
     *   <li>It is usually easier to implement for {@code AbstractMathTransform} subclasses.
     *   The default {@link #transform(double[], int, double[], int, int)} method implementation will invoke this
     *   method in a loop, taking care of the {@linkplain IterationStrategy iteration strategy} depending on the
     *   argument value.</li>
     *
     *   <li>When both the transformed point and its derivative are needed, this method may be significantly faster than
     *   invoking the {@code transform} and {@code derivative} methods separately because many internal calculations are
     *   the same. Computing those two information in a single step can help to reduce redundant calculation.</li>
     * </ul>
     *
     * {@section Note for implementors}
     * The source and destination may overlap. Consequently, implementors must read all source
     * ordinate values before to start writing the transformed ordinates in the destination array.
     *
     * @param srcPts   The array containing the source coordinate (can not be {@code null}).
     * @param srcOff   The offset to the point to be transformed in the source array.
     * @param dstPts   The array into which the transformed coordinate is returned. May be the same than {@code srcPts}.
     *                 May be {@code null} if only the derivative matrix is desired.
     * @param dstOff   The offset to the location of the transformed point that is stored in the destination array.
     * @param derivate {@code true} for computing the derivative, or {@code false} if not needed.
     * @return The matrix of the transform derivative at the given source position,
     *         or {@code null} if the {@code derivate} argument is {@code false}.
     * @throws TransformException If the point can not be transformed or if a problem occurred while calculating the
     *         derivative.
     *
     * @see #derivative(DirectPosition)
     * @see #transform(DirectPosition, DirectPosition)
     * @see MathTransforms#derivativeAndTransform(MathTransform, double[], int, double[], int)
     */
    public abstract Matrix transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, boolean derivate)
            throws TransformException;

    /**
     * Transforms a list of coordinate point ordinal values. This method is provided for efficiently
     * transforming many points. The supplied array of ordinal values will contain packed ordinal values.
     *
     * <div class="note"><b>Example:</b> if the source dimension is 3, then the ordinates will be packed in this order:
     * (<var>x<sub>0</sub></var>,<var>y<sub>0</sub></var>,<var>z<sub>0</sub></var>,
     *  <var>x<sub>1</sub></var>,<var>y<sub>1</sub></var>,<var>z<sub>1</sub></var> …).
     * </div>
     *
     * The default implementation invokes {@link #transform(double[], int, double[], int, boolean)} in a loop,
     * using an {@linkplain IterationStrategy iteration strategy} determined from the arguments for iterating
     * over the points.
     *
     * <div class="note"><b>Implementation note:</b> see {@link IterationStrategy} javadoc for a method skeleton.</div>
     *
     * @param  srcPts The array containing the source point coordinates.
     * @param  srcOff The offset to the first point to be transformed in the source array.
     * @param  dstPts The array into which the transformed point coordinates are returned.
     *                May be the same than {@code srcPts}.
     * @param  dstOff The offset to the location of the first transformed point that is stored in the destination array.
     * @param  numPts The number of point objects to be transformed.
     * @throws TransformException if a point can not be transformed. Some implementations will stop at the first failure,
     *         wile some other implementations will fill the untransformable points with {@linkplain Double#NaN} values,
     *         continue and throw the exception only at end. Implementations that fall in the later case should set the
     *         {@linkplain TransformException#getLastCompletedTransform last completed transform} to {@code this}.
     */
    @Override
    public void transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts)
            throws TransformException
    {
        if (numPts <= 0) {
            return;
        }
        /*
         * If case of overlapping source and destination arrays, determine if we should iterate
         * over the coordinates in ascending/descending order or copy the data in a temporary buffer.
         * The "offFinal" and "dstFinal" variables will be used only in the BUFFER_TARGET case.
         */
        double[] dstFinal = null;
        int offFinal = 0;
        int srcInc = getSourceDimensions();
        int dstInc = getTargetDimensions();
        if (srcPts == dstPts) {
            switch (IterationStrategy.suggest(srcOff, srcInc, dstOff, dstInc, numPts)) {
                case ASCENDING: {
                    break;
                }
                case DESCENDING: {
                    srcOff += (numPts-1) * srcInc; srcInc = -srcInc;
                    dstOff += (numPts-1) * dstInc; dstInc = -dstInc;
                    break;
                }
                default: // Following should alway work even for unknown cases.
                case BUFFER_SOURCE: {
                    srcPts = Arrays.copyOfRange(srcPts, srcOff, srcOff + numPts*srcInc);
                    srcOff = 0;
                    break;
                }
                case BUFFER_TARGET: {
                    dstFinal = dstPts; dstPts = new double[numPts * dstInc];
                    offFinal = dstOff; dstOff = 0;
                    break;
                }
            }
        }
        /*
         * Now apply the coordinate transformation, invoking the user-overrideable method
         * for each individual point. In case of failure, we will set the ordinates to NaN
         * and continue with other points, up to some maximal amount of failures.
         */
        TransformException failure = null;
        int failureCount = 0; // Count ordinates, not coordinates.
        int blockStart   = 0;
        do {
            try {
                transform(srcPts, srcOff, dstPts, dstOff, false);
            } catch (TransformException exception) {
                /*
                 * If an exception occurred, let it propagate if we reached the maximum amount
                 * of exceptions we try to handle. We do NOT invoke setLastCompletedTransform
                 * in this case since we gave up.
                 */
                failureCount += Math.abs(srcInc);
                if (failureCount > MAXIMUM_FAILURES) {
                    throw failure;
                }
                /*
                 * Otherwise fills the ordinate values to NaN and count the number of exceptions,
                 * so we known when to give up if there is too much of them. The first exception
                 * will be propagated at the end of this method.
                 */
                Arrays.fill(dstPts, dstOff, dstOff + Math.abs(dstInc), Double.NaN);
                if (failure == null) {
                    failure = exception; // Keep only the first failure.
                    blockStart = srcOff;
                } else {
                    failure.addSuppressed(exception);
                    if (Math.abs(srcOff - blockStart) > MAXIMUM_BUFFER_SIZE) {
                        failureCount = 0; // We started a new block of coordinates.
                        blockStart = srcOff;
                    }
                }
            }
            srcOff += srcInc;
            dstOff += dstInc;
        } while (--numPts != 0);
        if (dstFinal != null) {
            System.arraycopy(dstPts, 0, dstFinal, offFinal, dstPts.length);
        }
        /*
         * If some points failed to be transformed, let the first exception propagate.
         * But before doing so we declare that this transform has nevertheless be able
         * to process all coordinate points, setting them to NaN when transform failed.
         */
        if (failure != null) {
            failure.setLastCompletedTransform(this);
            throw failure;
        }
    }

    /**
     * Transforms a list of coordinate point ordinal values. The default implementation delegates
     * to {@link #transform(double[], int, double[], int, int)} using a temporary array of doubles.
     *
     * <div class="note"><b>Implementation note:</b> see {@link IterationStrategy} javadoc for a method skeleton.</div>
     *
     * @param srcPts The array containing the source point coordinates.
     * @param srcOff The offset to the first point to be transformed in the source array.
     * @param dstPts The array into which the transformed point coordinates are returned.
     *               May be the same than {@code srcPts}.
     * @param dstOff The offset to the location of the first transformed point that is stored in the destination array.
     * @param numPts The number of point objects to be transformed.
     * @throws TransformException if a point can't be transformed. Some implementations will stop at the first failure,
     *         wile some other implementations will fill the un-transformable points with {@link Float#NaN} values,
     *         continue and throw the exception only at end. Implementations that fall in the later case should set
     *         the {@linkplain TransformException#getLastCompletedTransform last completed transform} to {@code this}.
     */
    @Override
    public void transform(float[] srcPts, int srcOff, float[] dstPts, int dstOff, int numPts)
            throws TransformException
    {
        if (numPts <= 0) {
            return;
        }
        final int dimSource  = getSourceDimensions();
        final int dimTarget  = getTargetDimensions();
        final int dimLargest = Math.max(dimSource, dimTarget);
        /*
         * Computes the number of points in the buffer in such a way that the buffer
         * can contain at least one point and is not larger than MAXIMUM_BUFFER_SIZE
         * (except if a single point is larger than that).
         */
        int numBufferedPts = numPts;
        int bufferSize = numPts * dimLargest;
        if (bufferSize > MAXIMUM_BUFFER_SIZE) {
            numBufferedPts = Math.max(1, MAXIMUM_BUFFER_SIZE / dimLargest);
            bufferSize = numBufferedPts * dimLargest;
        }
        /*
         * We need to check if writing the transformed coordinates in the same array than the source
         * coordinates will cause an overlapping problem. However we can consider the whole buffer as
         * if it was a single coordinate with a very large dimension. Doing so increase the chances
         * that IterationStrategy.suggest(...) doesn't require us an other buffer  (hint: the -1 in
         * suggest(...) mathematic matter and reflect the contract saying that the input coordinate
         * must be fully read before the output coordinate is written - which is the behavior we get
         * with our buffer).
         */
        int srcInc = dimSource * numBufferedPts;
        int dstInc = dimTarget * numBufferedPts;
        int srcStop = srcInc; // src|dstStop will be used and modified in the do..while loop later.
        int dstStop = dstInc;
        if (srcPts == dstPts) {
            final int numPass = (numPts + numBufferedPts-1) / numBufferedPts; // Round toward higher integer.
            switch (IterationStrategy.suggest(srcOff, srcInc, dstOff, dstInc, numPass)) {
                case ASCENDING: {
                    break;
                }
                case DESCENDING: {
                    final int delta = numPts - numBufferedPts;
                    srcOff += delta * dimSource;
                    dstOff += delta * dimTarget;
                    srcInc = -srcInc;
                    dstInc = -dstInc;
                    break;
                }
                default: {
                    srcPts = Arrays.copyOfRange(srcPts, srcOff, srcOff + numPts*dimSource);
                    srcOff = 0;
                    break;
                }
            }
        }
        /*
         * Computes the offset of the first source coordinate in the buffer. The offset of the
         * first destination coordinate will always be zero.   We compute the source offset in
         * such a way that the default transform(double[],int,double[],int,int) implementation
         * should never needs to copy the source coordinates in yet an other temporary buffer.
         * We will verify that with an assert statement inside the do loop.
         */
        final int bufferedSrcOff = (dimSource >= dimTarget) ? 0 : dstStop - srcStop;
        final double[] buffer = new double[bufferSize];
        TransformException failure = null;
        do {
            if (numPts < numBufferedPts) {
                numBufferedPts = numPts;
                srcStop = numPts * dimSource;
                dstStop = numPts * dimTarget;
                if (srcInc < 0) {
                    // If we were applying IterationStrategy.DESCENDING, then srcOff and dstOff
                    // may be negative at this point because the last pass may not fill all the
                    // buffer space. We need to apply the correction below.
                    srcOff -= (srcStop + srcInc);
                    dstOff -= (dstStop + dstInc);
                }
            }
            for (int i=0; i<srcStop; i++) {
                buffer[bufferedSrcOff + i] = (double) srcPts[srcOff + i];
            }
            assert !IterationStrategy.suggest(bufferedSrcOff, dimSource, 0, dimTarget, numBufferedPts).needBuffer;
            try {
                transform(buffer, bufferedSrcOff, buffer, 0, numBufferedPts);
            } catch (TransformException exception) {
                /*
                 * If an exception occurred but the transform nevertheless declares having been
                 * able to process all coordinate points (setting to NaN those that can't be
                 * transformed), we will keep the first exception (to be propagated at the end
                 * of this method) and continue. Otherwise we will stop immediately.
                 */
                if (exception.getLastCompletedTransform() != this) {
                    throw exception;
                } else if (failure == null) {
                    failure = exception; // Keep only the first exception.
                } else {
                    failure.addSuppressed(exception);
                }
            }
            for (int i=0; i<dstStop; i++) {
                dstPts[dstOff + i] = (float) buffer[i];
            }
            srcOff += srcInc;
            dstOff += dstInc;
            numPts -= numBufferedPts;
        } while (numPts != 0);
        if (failure != null) {
            throw failure;
        }
    }

    /**
     * Transforms a list of coordinate point ordinal values. The default implementation delegates
     * to {@link #transform(double[], int, double[], int, int)} using a temporary array of doubles.
     *
     * @param srcPts The array containing the source point coordinates.
     * @param srcOff The offset to the first point to be transformed in the source array.
     * @param dstPts The array into which the transformed point coordinates are returned.
     * @param dstOff The offset to the location of the first transformed point that is stored in the destination array.
     * @param numPts The number of point objects to be transformed.
     * @throws TransformException if a point can not be transformed. Some implementations will stop at the first failure,
     *         wile some other implementations will fill the untransformable points with {@linkplain Float#NaN} values,
     *         continue and throw the exception only at end. Implementations that fall in the later case should set the
     *         {@linkplain TransformException#getLastCompletedTransform last completed transform} to {@code this}.
     */
    @Override
    public void transform(final double[] srcPts, int srcOff,
                          final float [] dstPts, int dstOff, int numPts)
            throws TransformException
    {
        if (numPts <= 0) {
            return;
        }
        final int dimSource = getSourceDimensions();
        final int dimTarget = getTargetDimensions();
        int numBufferedPts = numPts;
        int bufferSize = numPts * dimTarget;
        if (bufferSize > MAXIMUM_BUFFER_SIZE) {
            numBufferedPts = Math.max(1, MAXIMUM_BUFFER_SIZE / dimTarget);
            bufferSize = numBufferedPts * dimTarget;
        }
        int srcLength = numBufferedPts * dimSource;
        int dstLength = numBufferedPts * dimTarget;
        final double[] buffer = new double[bufferSize];
        TransformException failure = null;
        do {
            if (numPts < numBufferedPts) {
                numBufferedPts = numPts;
                srcLength = numPts * dimSource;
                dstLength = numPts * dimTarget;
            }
            try {
                transform(srcPts, srcOff, buffer, 0, numBufferedPts);
            } catch (TransformException exception) {
                // Same comment than in transform(float[], ...,float[], ...)
                if (exception.getLastCompletedTransform() != this) {
                    throw exception;
                } else if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
            for (int i=0; i<dstLength; i++) {
                dstPts[dstOff++] = (float) buffer[i];
            }
            srcOff += srcLength;
            numPts -= numBufferedPts;
        } while (numPts != 0);
        if (failure != null) {
            throw failure;
        }
    }

    /**
     * Transforms a list of coordinate point ordinal values. The default implementation delegates
     * to {@link #transform(double[], int, double[], int, int)} using a temporary array of doubles
     * if necessary.
     *
     * @param srcPts The array containing the source point coordinates.
     * @param srcOff The offset to the first point to be transformed in the source array.
     * @param dstPts The array into which the transformed point coordinates are returned.
     * @param dstOff The offset to the location of the first transformed point that is stored in the destination array.
     * @param numPts The number of point objects to be transformed.
     * @throws TransformException if a point can not be transformed. Some implementations will stop at the first failure,
     *         wile some other implementations will fill the untransformable points with {@linkplain Double#NaN} values,
     *         continue and throw the exception only at end. Implementations that fall in the later case should set the
     *         {@linkplain TransformException#getLastCompletedTransform last completed transform} to {@code this}.
     */
    @Override
    public void transform(final float [] srcPts, int srcOff,
                          final double[] dstPts, int dstOff, int numPts)
            throws TransformException
    {
        if (numPts <= 0) {
            return;
        }
        final int dimSource = getSourceDimensions();
        final int dimTarget = getTargetDimensions();
        if (dimSource == dimTarget) {
            final int n = numPts * dimSource;
            for (int i=0; i<n; i++) {
                dstPts[dstOff + i] = srcPts[srcOff + i];
            }
            transform(dstPts, dstOff, dstPts, dstOff, numPts);
            return;
        }
        int numBufferedPts = numPts;
        int bufferSize = numPts * dimSource;
        if (bufferSize > MAXIMUM_BUFFER_SIZE) {
            numBufferedPts = Math.max(1, MAXIMUM_BUFFER_SIZE / dimSource);
            bufferSize = numBufferedPts * dimSource;
        }
        int srcLength = numBufferedPts * dimSource;
        int dstLength = numBufferedPts * dimTarget;
        final double[] buffer = new double[bufferSize];
        TransformException failure = null;
        do {
            if (numPts < numBufferedPts) {
                numBufferedPts = numPts;
                srcLength = numPts * dimSource;
                dstLength = numPts * dimTarget;
            }
            for (int i=0; i<srcLength; i++) {
                buffer[i] = (double) srcPts[srcOff++];
            }
            try {
                transform(buffer, 0, dstPts, dstOff, numBufferedPts);
            } catch (TransformException exception) {
                // Same comment than in transform(float[], ...,float[], ...)
                if (exception.getLastCompletedTransform() != this) {
                    throw exception;
                } else if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
            dstOff += dstLength;
            numPts -= numBufferedPts;
        } while (numPts != 0);
        if (failure != null) {
            throw failure;
        }
    }

    /**
     * Gets the derivative of this transform at a point.
     * The default implementation performs the following steps:
     *
     * <ul>
     *   <li>Ensure that the {@code point} dimension is equals to this math transform
     *       {@linkplain #getSourceDimensions() source dimensions}.</li>
     *   <li>Copy the coordinate in a temporary array and pass that array to the
     *       {@link #transform(double[], int, double[], int, boolean)} method,
     *       with the {@code derivate} boolean argument set to {@code true}.</li>
     *   <li>If the later method returned a non-null matrix, returns that matrix.
     *       Otherwise throws {@link TransformException}.</li>
     * </ul>
     *
     * @param  point The coordinate point where to evaluate the derivative.
     * @return The derivative at the specified point (never {@code null}).
     * @throws NullPointerException if the derivative depends on coordinate and {@code point} is {@code null}.
     * @throws MismatchedDimensionException if {@code point} does not have the expected dimension.
     * @throws TransformException if the derivative can not be evaluated at the specified point.
     */
    @Override
    public Matrix derivative(final DirectPosition point) throws TransformException {
        final int dimSource = getSourceDimensions();
        final double[] coordinate = point.getCoordinate();
        if (coordinate.length != dimSource) {
            throw new MismatchedDimensionException(mismatchedDimension("point", coordinate.length, dimSource));
        }
        final Matrix derivative = transform(coordinate, 0, null, 0, true);
        if (derivative == null) {
            throw new TransformException(Errors.format(Errors.Keys.CanNotComputeDerivative));
        }
        return derivative;
    }

    /**
     * Returns the inverse transform of this object. The default implementation returns
     * {@code this} if this transform is an {@linkplain #isIdentity() identity} transform,
     * or throws an exception otherwise. Subclasses should override this method.
     *
     * <div class="note"><b>Implementation note:</b> the {@link Inverse} inner class can be used as
     * a base for inverse transform implementations.</div>
     */
    @Override
    public MathTransform inverse() throws NoninvertibleTransformException {
        if (isIdentity()) {
            return this;
        }
        throw new NoninvertibleTransformException(Errors.format(Errors.Keys.NonInvertibleTransform));
    }

    /**
     * Concatenates in an optimized way this math transform with the given one. A new math transform
     * is created to perform the combined transformation. The {@code applyOtherFirst} value determines
     * the transformation order as bellow:
     *
     * <ul>
     *   <li>If {@code applyOtherFirst} is {@code true}, then transforming a point
     *       <var>p</var> by the combined transform is equivalent to first transforming
     *       <var>p</var> by {@code other} and then transforming the result by {@code this}.</li>
     *   <li>If {@code applyOtherFirst} is {@code false}, then transforming a point
     *       <var>p</var> by the combined transform is equivalent to first transforming
     *       <var>p</var> by {@code this} and then transforming the result by {@code other}.</li>
     * </ul>
     *
     * If no special optimization is available for the combined transform, then this method returns {@code null}.
     * In the later case, the concatenation will be prepared by {@link DefaultMathTransformFactory} using a generic
     * {@link ConcatenatedTransform}.
     *
     * <p>The default implementation always returns {@code null}. This method is ought to be overridden
     * by subclasses capable of concatenating some combination of transforms in a special way.
     * Examples are {@link ExponentialTransform1D} and {@link LogarithmicTransform1D}.</p>
     *
     * @param  other The math transform to apply.
     * @param  applyOtherFirst {@code true} if the transformation order is {@code other} followed by {@code this},
     *         or {@code false} if the transformation order is {@code this} followed by {@code other}.
     * @return The combined math transform, or {@code null} if no optimized combined transform is available.
     */
    MathTransform concatenate(final MathTransform other, final boolean applyOtherFirst) {
        return null;
    }

    /**
     * Returns a hash value for this transform. This method invokes {@link #computeHashCode()}
     * when first needed and caches the value for future invocations. Subclasses shall override
     * {@code computeHashCode()} instead than this method.
     *
     * @return The hash code value. This value may change between different execution of the Apache SIS library.
     */
    @Override
    public final int hashCode() { // No need to synchronize; ok if invoked twice.
        int hash = hashCode;
        if (hash == 0) {
            hash = computeHashCode();
            if (hash == 0) {
                hash = -1;
            }
            hashCode = hash;
        }
        assert hash == -1 || hash == computeHashCode() : this;
        return hash;
    }

    /**
     * Computes a hash value for this transform. This method is invoked by {@link #hashCode()} when first needed.
     *
     * @return The hash code value. This value may change between different execution of the Apache SIS library.
     */
    protected int computeHashCode() {
        return getClass().hashCode() + getSourceDimensions() + 31 * getTargetDimensions();
    }

    /**
     * Compares the specified object with this math transform for strict equality.
     * This method is implemented as below (omitting assertions):
     *
     * {@preformat java
     *     return equals(other, ComparisonMode.STRICT);
     * }
     *
     * @param  object The object to compare with this transform.
     * @return {@code true} if the given object is a transform of the same class and using the same parameter values.
     */
    @Override
    public final boolean equals(final Object object) {
        final boolean eq = equals(object, ComparisonMode.STRICT);
        // If objects are equal, then they must have the same hash code value.
        assert !eq || computeHashCode() == ((AbstractMathTransform) object).computeHashCode() : this;
        return eq;
    }

    /**
     * Compares the specified object with this math transform for equality.
     * Two math transforms are considered equal if, given identical source positions, their
     * {@linkplain #transform(DirectPosition,DirectPosition) transformed} positions would be
     * equal or {@linkplain ComparisonMode#APPROXIMATIVE approximatively} equal.
     * This method may conservatively returns {@code false} if unsure.
     *
     * <p>The default implementation returns {@code true} if the following conditions are meet:</p>
     * <ul>
     *   <li>{@code object} is an instance of the same class than {@code this}. We require the
     *        same class because there is no interface for the various kinds of transform.</li>
     *   <li>The {@linkplain #getParameterDescriptors() parameter descriptors} are equal according
     *       the given comparison mode.</li>
     * </ul>
     *
     * The {@linkplain #getParameterValues() parameter values} are <strong>not</strong> compared because
     * subclasses can typically compare those values more efficiently by accessing to their member fields.
     *
     * @param  object The object to compare with this transform.
     * @param  mode The strictness level of the comparison. Default to {@link ComparisonMode#STRICT STRICT}.
     * @return {@code true} if the given object is considered equals to this math transform.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        // Do not check 'object==this' here, since this
        // optimization is usually done in subclasses.
        if (object != null && getClass() == object.getClass()) {
            final AbstractMathTransform that = (AbstractMathTransform) object;
            /*
             * If the classes are the same, then the hash codes should be computed in the same way. Since those
             * codes are cached, this is an efficient way to quickly check if the two objects are different.
             */
            if (mode.ordinal() < ComparisonMode.APPROXIMATIVE.ordinal()) {
                final int tc = hashCode;
                if (tc != 0) {
                    final int oc = that.hashCode;
                    if (oc != 0 && tc != oc) {
                        return false;
                    }
                }
            }
            // See the policy documented in the LenientComparable javadoc.
            if (mode.ordinal() >= ComparisonMode.IGNORE_METADATA.ordinal()) {
                return true;
            }
            return Utilities.deepEquals(this.getParameterDescriptors(),
                                        that.getParameterDescriptors(), mode);
        }
        return false;
    }

    /**
     * Formats the inner part of a <cite>Well Known Text</cite> version 1 (WKT 1) element.
     * The default implementation formats all parameter values returned by {@link #getParameterValues()}.
     * The parameter group name is used as the math transform name.
     *
     * @param  formatter The formatter to use.
     * @return The WKT element name, which is {@code "Param_MT"} in the default implementation.
     */
    @Override
    public String formatTo(final Formatter formatter) {
        final ParameterValueGroup parameters = getParameterValues();
        if (parameters != null) {
            WKTUtilities.appendName(parameters.getDescriptor(), formatter, null);
            WKTUtilities.append(parameters, formatter);
        }
        return "Param_MT";
    }

    /**
     * Strictly reserved to {@link AbstractMathTransform2D}, which will
     * override this method. The default implementation must do nothing.
     *
     * <p>This method is invoked only by {@link ConcatenatedTransform#getPseudoSteps()} in order to
     * get the {@link ParameterValueGroup} of a map projection, or to format a {@code PROJCS} WKT.</p>
     *
     * @param  transforms The full chain of concatenated transforms.
     * @param  index      The index of this transform in the {@code transforms} chain.
     * @param  inverse    Always {@code false}, except if we are formatting the inverse transform.
     * @return Index of the last transform processed. Iteration should continue at that index + 1.
     *
     * @see AbstractMathTransform2D#beforeFormat(List, int, boolean)
     * @see ConcatenatedTransform#getPseudoSteps()
     */
    int beforeFormat(List<Object> transforms, int index, boolean inverse) {
        return index;
    }

    /**
     * Base class for implementations of inverse math transforms.
     * This inner class is the inverse of the enclosing {@link AbstractMathTransform}.
     *
     * {@section Serialization}
     * Instances of this class are serializable only if the enclosing math transform is also serializable.
     * Serialized math transforms are not guaranteed to be compatible with future SIS versions.
     * Serialization, if allowed, should be used only for short term storage or RMI between applications
     * running the same SIS version.
     *
     * @author  Martin Desruisseaux (IRD, Geomatys)
     * @since   0.5 (derived from geotk-2.0)
     * @version 0.5
     * @module
     */
    protected abstract class Inverse extends AbstractMathTransform implements Serializable {
        /**
         * Serial number for inter-operability with different versions. This serial number is
         * especially important for inner classes, since the default {@code serialVersionUID}
         * computation will not produce consistent results across implementations of different
         * Java compiler. This is because different compilers may generate different names for
         * synthetic members used in the implementation of inner classes. See:
         *
         * http://developer.java.sun.com/developer/bugParade/bugs/4211550.html
         */
        private static final long serialVersionUID = 3528274816628012283L;

        /**
         * Constructs an inverse math transform.
         */
        protected Inverse() {
        }

        /**
         * Gets the dimension of input points.
         * The implementation returns the dimension of output points of the enclosing math transform.
         *
         * @return {@inheritDoc}
         */
        @Override
        public final int getSourceDimensions() {
            return AbstractMathTransform.this.getTargetDimensions();
        }

        /**
         * Gets the dimension of output points.
         * The implementation returns the dimension of input points of the enclosing math transform.
         *
         * @return {@inheritDoc}
         */
        @Override
        public final int getTargetDimensions() {
            return AbstractMathTransform.this.getSourceDimensions();
        }

        /**
         * Gets the derivative of this transform at a point.
         * The default implementation computes the inverse of the matrix returned by the enclosing math transform.
         *
         * @return {@inheritDoc}
         * @throws NullPointerException if the derivative depends on coordinate and {@code point} is {@code null}.
         * @throws MismatchedDimensionException if {@code point} does not have the expected dimension.
         * @throws TransformException if the derivative can not be evaluated at the specified point.
         */
        @Override
        public Matrix derivative(DirectPosition point) throws TransformException {
            if (point != null) {
                point = this.transform(point, null);
            }
            return MatrixSIS.castOrCopy(AbstractMathTransform.this.derivative(point)).inverse();
        }

        /**
         * Returns the inverse of this math transform, which is the enclosing math transform.
         *
         * @return The enclosing math transform.
         */
        @Override
        public MathTransform inverse() {
            return AbstractMathTransform.this;
        }

        /**
         * Tests whether this transform does not move any points.
         * The default implementation delegates this tests to the enclosing math transform.
         *
         * @return {@inheritDoc}
         */
        @Override
        public boolean isIdentity() {
            return AbstractMathTransform.this.isIdentity();
        }

        /**
         * {@inheritDoc}
         *
         * @return {@inheritDoc}
         */
        @Override
        protected int computeHashCode() {
            return super.computeHashCode() + 31*AbstractMathTransform.this.hashCode();
        }

        /**
         * Compares the specified object with this inverse math transform for equality.
         * The default implementation tests if {@code object} in an instance of the same class
         * than {@code this}, and if so compares their enclosing {@code AbstractMathTransform}.
         *
         * @return {@inheritDoc}
         */
        @Override
        public boolean equals(final Object object, final ComparisonMode mode) {
            if (object == this) {
                // Slight optimization
                return true;
            }
            if (object != null && object.getClass() == getClass()) {
                return AbstractMathTransform.this.equals(((Inverse) object).inverse(), mode);
            } else {
                return false;
            }
        }

        /**
         * Formats the inner part of a <cite>Well Known Text</cite> version 1 (WKT 1) element.
         * If this inverse math transform has any parameter values, then this method format the
         * WKT as in the {@linkplain AbstractMathTransform#formatWKT super-class method}.
         * Otherwise this method formats the math transform as an {@code "Inverse_MT"} entity.
         *
         * @param  formatter The formatter to use.
         * @return The WKT element name, which is {@code "Param_MT"} or
         *         {@code "Inverse_MT"} in the default implementation.
         */
        @Override
        public String formatTo(final Formatter formatter) {
            final ParameterValueGroup parameters = getParameterValues();
            if (parameters != null) {
                WKTUtilities.appendName(parameters.getDescriptor(), formatter, null);
                WKTUtilities.append(parameters, formatter);
                return "Param_MT";
            } else {
                formatter.append((FormattableObject) AbstractMathTransform.this);
                return "Inverse_MT";
            }
        }
    }
}
