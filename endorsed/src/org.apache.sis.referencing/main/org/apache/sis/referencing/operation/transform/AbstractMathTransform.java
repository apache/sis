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
import java.util.Optional;
import java.util.logging.Logger;
import org.opengis.util.FactoryException;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.parameter.Parameterized;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.internal.shared.WKTUtilities;
import org.apache.sis.referencing.internal.shared.WKTKeywords;
import org.apache.sis.system.Loggers;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArgumentCheckByAssertion;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.OptionalCandidate;
import org.apache.sis.util.resources.Errors;

// Specific to the main branch:
import org.opengis.geometry.MismatchedDimensionException;


/**
 * Provides a default implementation for most methods required by the {@link MathTransform} interface.
 * A {@code MathTransform} is an object that actually does the work of applying a
 * {@linkplain org.apache.sis.referencing.operation.DefaultFormula formula} to coordinate values.
 * The math transform does not know or care how the coordinates relate to positions in the real world.
 * For example if an affine transform scales <var>z</var> values by a factor of 1000,
 * then it could be converting metres to millimetres, or it could be converting kilometres to metres.
 *
 * <p>{@code AbstractMathTransform} provides a convenient base class from which {@code MathTransform} implementations
 * can be easily derived. It also defines a few additional SIS-specific methods for convenience of performance.
 * The simplest way to implement this abstract class is to provide an implementation for the following methods only:</p>
 *
 * <ul>
 *   <li>{@link #getSourceDimensions()}</li>
 *   <li>{@link #getTargetDimensions()}</li>
 *   <li>{@link #transform(double[], int, double[], int, boolean)}</li>
 * </ul>
 *
 * However, more performance may be gained by overriding the other {@code transform(…)} methods as well.
 *
 * <h2>Immutability and thread safety</h2>
 * All Apache SIS implementations of {@code MathTransform} are immutable and thread-safe.
 * It is highly recommended that third-party implementations be immutable and thread-safe too.
 * This means that unless otherwise noted in the javadoc, {@code MathTransform} instances can
 * be shared by many objects and passed between threads without synchronization.
 *
 * <h2>Serialization</h2>
 * {@code MathTransform} may or may not be serializable, at implementation choices.
 * Most Apache SIS implementations are serializable, but the serialized objects are not guaranteed to be compatible
 * with future SIS versions. Serialization should be used only for short term storage or RMI between applications
 * running the same SIS version.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.5
 *
 * @see DefaultMathTransformFactory
 * @see org.apache.sis.referencing.operation.AbstractCoordinateOperation
 *
 * @since 0.5
 */
public abstract class AbstractMathTransform extends FormattableObject
        implements MathTransform, Parameterized, LenientComparable
{
    /**
     * The logger for coordinate operations.
     */
    static final Logger LOGGER = Logger.getLogger(Loggers.COORDINATE_OPERATION);

    /**
     * Maximum buffer size when creating temporary arrays. Must not be too big, otherwise the
     * cost of allocating the buffer may be greater than the benefit of transforming array of
     * coordinates. Remember that double number occupy 8 bytes, so a buffer of size 512 will
     * actually consumes 4 kb of memory.
     */
    static final int MAXIMUM_BUFFER_SIZE = 512;

    /**
     * Maximum number of {@link TransformException}s to catch while transforming a block of
     * {@value #MAXIMUM_BUFFER_SIZE} coordinate values in an array. The default implementation of
     * {@code transform} methods set un-transformable coordinates to {@linkplain Double#NaN NaN}
     * before to let the exception propagate. However if more then {@value} exceptions occur in
     * a block of {@value #MAXIMUM_BUFFER_SIZE} <em>coordinates</em> (not coordinate tuples),
     * then we will give up. We put a limit in order to avoid slowing down the application
     * too much if a whole array is not transformable.
     *
     * <p>Note that in case of failure, the first {@code TransformException} is still propagated;
     * we do not "eat" it. We just set the coordinates to {@code NaN} before to let the propagation
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
     * Returns the number of dimensions of input points.
     *
     * @return the number of dimensions of input points.
     *
     * @see org.apache.sis.referencing.operation.DefaultOperationMethod#getSourceDimensions()
     */
    @Override
    public abstract int getSourceDimensions();

    /**
     * Returns the number of dimensions of output points.
     *
     * @return the number of dimensions of output points.
     *
     * @see org.apache.sis.referencing.operation.DefaultOperationMethod#getTargetDimensions()
     */
    @Override
    public abstract int getTargetDimensions();

    /**
     * Returns the ranges of coordinate values which can be used as inputs.
     * They are limits where the transform is mathematically and numerically applicable.
     * This is <em>not</em> the domain of validity for which a coordinate reference system has been defined,
     * because this method ignores "real world" considerations such as datum and country boundaries.
     *
     * <p>This method is for allowing callers to crop their data for removing areas that may cause numerical problems.
     * For example, results of Mercator projection tend to infinity when the latitude value approaches a pole.
     * For avoiding data structures with unreasonably large values or {@link Double#NaN},
     * we commonly crop data to some arbitrary maximal latitude value (typically 80 or 84°) before projection.
     * Those limits are arbitrary, the transform does not become suddenly invalid after a limit.
     * The {@link DomainDefinition} gives some controls on the criteria for choosing a limit.</p>
     *
     * <p>Many transforms, in particular all affine transforms, have no mathematical limits.
     * Consequently, the default implementation returns an empty value.
     * Again it does not mean that the {@linkplain org.apache.sis.referencing.operation.AbstractCoordinateOperation
     * coordinate operation} has no geospatial domain of validity, but the latter is not the purpose of this method.
     * This method is (for example) for preventing a viewer to crash when attempting to render a world-wide image.</p>
     *
     * <p>Callers do not need to search through {@linkplain MathTransforms#getSteps(MathTransform) transform steps}.
     * The Apache <abbr>SIS</abbr> implementation of {@link MathTransforms#concatenate(MathTransform, MathTransform)
     * concatenated transforms} does that automatically.</p>
     *
     * @param  criteria  controls the definition of transform domain.
     * @return estimation of a domain where this transform is considered numerically applicable.
     * @throws TransformException if the domain cannot be estimated.
     *
     * @see MathTransforms#getDomain(MathTransform)
     * @see org.opengis.referencing.operation.CoordinateOperation#getDomainOfValidity()
     *
     * @since 1.3
     */
    public Optional<Envelope> getDomain(DomainDefinition criteria) throws TransformException {
        ArgumentChecks.ensureNonNull("criteria", criteria);
        return Optional.empty();
    }

    /**
     * Returns the parameter descriptors for this math transform, or {@code null} if unknown.
     *
     * <h4>Relationship with ISO 19111</h4>
     * This method is similar to {@link OperationMethod#getParameters()}, except that typical
     * {@link MathTransform} implementations return parameters in standard units (usually
     * {@linkplain org.apache.sis.measure.Units#METRE metres} or
     * {@linkplain org.apache.sis.measure.Units#DEGREE decimal degrees}).
     *
     * @return the parameter descriptors for this math transform, or {@code null} if unspecified.
     *
     * @see org.apache.sis.referencing.operation.DefaultOperationMethod#getParameters()
     */
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        final ContextualParameters parameters = getContextualParameters();
        return (parameters != null) ? parameters.getDescriptor() : null;
    }

    /**
     * Returns the parameter values for this math transform, or {@code null} if unknown.
     * This is not necessarily the parameters that the user specified at construction time,
     * since implementations may have applied normalizations.
     *
     * <h4>Normalized and contextual parameters</h4>
     * Most Apache SIS implementations of map projections perform their calculations on an ellipsoid
     * having a semi-major axis length of 1. In such cases, the group returned by this method contains
     * a {@code "semi_major"} parameter with a value of 1. If the real axis length is desired, we need
     * to take in account the context of this math transform, i.e. the scales and offsets applied before
     * and after this transform. This information is provided by {@link #getContextualParameters()}.
     *
     * @return the parameter values for this math transform, or {@code null} if unspecified.
     *         Note that those parameters may be normalized (e.g. represent a transformation
     *         of an ellipsoid of semi-major axis length of 1).
     *
     * @see #getContextualParameters()
     * @see org.apache.sis.referencing.operation.DefaultConversion#getParameterValues()
     */
    @Override
    @OptionalCandidate
    public ParameterValueGroup getParameterValues() {
        /*
         * Do NOT try to infer the parameters from getContextualParameters(). This is usually not appropriate
         * because if ContextualParameters declares "normalize" and "denormalize" affine transforms, then those
         * transforms need to be taken in account in a way that only the subclass know.
         */
        return null;
    }

    /**
     * Returns the parameters for a sequence of <i>normalize</i> → {@code this} → <i>denormalize</i>
     * transforms (<i>optional operation</i>).
     *
     * Subclasses can override this method if they choose to split their computation in linear and non-linear parts.
     * Such split is optional: it can leads to better performance (because SIS can concatenate efficiently consecutive
     * linear transforms), but should not change significantly the result (ignoring differences in rounding errors).
     * If a split has been done, then this {@code MathTransform} represents only the non-linear step and Apache SIS
     * needs this method for reconstructing the parameters of the complete transform.
     *
     * @return the parameter values for the <i>normalize</i> → {@code this} → <i>denormalize</i> chain of transforms,
     *         or {@code null} if unspecified.
     *         Callers should not modify the returned parameters, since modifications (if allowed)
     *         will generally not be reflected back in this {@code MathTransform}.
     *
     * @since 0.6
     */
    @OptionalCandidate
    protected ContextualParameters getContextualParameters() {
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
     * @param  argument   the argument name with the wrong number of dimensions.
     * @param  expected   the expected dimension.
     * @param  dimension  the wrong dimension.
     */
    static MismatchedDimensionException mismatchedDimension(final String argument, final int expected, final int dimension) {
        return new MismatchedDimensionException(Errors.format(Errors.Keys.MismatchedDimension_3, argument, expected, dimension));
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
     * This method does not update the associated {@link org.opengis.referencing.crs.CoordinateReferenceSystem} value.
     *
     * @param  ptSrc  the coordinate tuple to be transformed.
     * @param  ptDst  the coordinate tuple that stores the result of transforming {@code ptSrc}, or {@code null}.
     * @return the coordinate tuple after transforming {@code ptSrc} and storing the result in {@code ptDst},
     *         or a newly created point if {@code ptDst} was null.
     * @throws MismatchedDimensionException if {@code ptSrc} or {@code ptDst} doesn't have the expected dimension.
     * @throws TransformException if the point cannot be transformed.
     */
    @Override
    public DirectPosition transform(final DirectPosition ptSrc, DirectPosition ptDst) throws TransformException {
        final int dimSource = getSourceDimensions();
        final int dimTarget = getTargetDimensions();
        ArgumentChecks.ensureDimensionMatches("ptSrc", dimSource, ptSrc);
        if (ptDst != null) {
            ArgumentChecks.ensureDimensionMatches("ptDst", dimTarget, ptDst);
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
             * destination will be the SIS implementation, write directly into the `coordinates` array.
             */
            final var destination = new GeneralDirectPosition(dimTarget);
            final double[] source;
            if (dimSource <= dimTarget) {
                source = destination.coordinates;
                for (int i=0; i<dimSource; i++) {
                    source[i] = ptSrc.getOrdinate(i);
                }
            } else {
                source = ptSrc.getCoordinate();
            }
            transform(source, 0, destination.coordinates, 0, false);
            ptDst = destination;
        }
        return ptDst;
    }

    /**
     * Transforms a single coordinate tuple in an array, and optionally computes the transform
     * derivative at that location. Invoking this method is conceptually equivalent to running
     * the following:
     *
     * {@snippet lang="java" :
     *     Matrix derivative = null;
     *     if (derivate) {
     *         double[] coordinates = Arrays.copyOfRange(srcPts, srcOff, srcOff + getSourceDimensions());
     *         derivative = this.derivative(new GeneralDirectPosition(coordinates));
     *     }
     *     this.transform(srcPts, srcOff, dstPts, dstOff, 1);                   // May overwrite srcPts.
     *     return derivative;
     *     }
     *
     * However, this method provides two advantages:
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
     * <h4>Note for implementers</h4>
     * The source and destination may overlap. Consequently, implementers must read all source
     * coordinate values before to start writing the transformed coordinates in the destination array.
     *
     * @param  srcPts    the array containing the source coordinates (cannot be {@code null}).
     * @param  srcOff    the offset to the point to be transformed in the source array.
     * @param  dstPts    the array into which the transformed coordinates is returned. May be the same as {@code srcPts}.
     *                   May be {@code null} if only the derivative matrix is desired.
     * @param  dstOff    the offset to the location of the transformed point that is stored in the destination array.
     * @param  derivate  {@code true} for computing the derivative, or {@code false} if not needed.
     * @return the matrix of the transform derivative at the given source position,
     *         or {@code null} if the {@code derivate} argument is {@code false}.
     * @throws TransformException if the point cannot be transformed or
     *         if a problem occurred while calculating the derivative.
     *
     * @see #derivative(DirectPosition)
     * @see #transform(DirectPosition, DirectPosition)
     * @see MathTransforms#derivativeAndTransform(MathTransform, double[], int, double[], int)
     */
    public abstract Matrix transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, boolean derivate)
            throws TransformException;

    /**
     * Transforms a list of coordinate tuples. This method is provided for efficiently transforming many points.
     * The supplied array of coordinate values will contain packed coordinate values.
     *
     * <h4>Example</h4>
     * If the source dimension is 3, then the coordinates will be packed in this order:
     * (<var>x₀</var>,<var>y₀</var>,<var>z₀</var>,
     *  <var>x₁</var>,<var>y₁</var>,<var>z₁</var> …).
     *
     * <h4>Implementation note</h4>
     * The default implementation invokes {@link #transform(double[], int, double[], int, boolean)} in a loop,
     * using an {@linkplain IterationStrategy iteration strategy} determined from the arguments for iterating
     * over the points. For creating a more efficient implementation,
     * see {@link IterationStrategy} javadoc for a method skeleton.
     *
     * @param  srcPts  the array containing the source point coordinates.
     * @param  srcOff  the offset to the first point to be transformed in the source array.
     * @param  dstPts  the array into which the transformed point coordinates are returned.
     *                 May be the same as {@code srcPts}.
     * @param  dstOff  the offset to the location of the first transformed point that is stored in the destination array.
     * @param  numPts  the number of point objects to be transformed.
     * @throws TransformException if a point cannot be transformed. Some implementations will stop at the first failure,
     *         wile some other implementations will fill the untransformable points with {@linkplain Double#NaN} values,
     *         continue and throw the exception only at end. Implementations that fall in the latter case should set the
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
                default:                    // Following should alway work even for unknown cases.
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
         * for each individual point. In case of failure, we will set the coordinates to NaN
         * and continue with other points, up to some maximal number of failures.
         */
        TransformException failure = null;
        int failureCount = 0;                           // Count coordinates, not coordinate tuples.
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
                 * Otherwise set the coordinate values to NaN and count the number of exceptions,
                 * so we know when to give up if there is too much of them. The first exception
                 * will be propagated at the end of this method.
                 */
                Arrays.fill(dstPts, dstOff, dstOff + Math.abs(dstInc), Double.NaN);
                if (failure == null) {
                    failure = exception;                        // Keep only the first failure.
                    blockStart = srcOff;
                } else {
                    failure.addSuppressed(exception);
                    if (Math.abs(srcOff - blockStart) > MAXIMUM_BUFFER_SIZE) {
                        failureCount = 0;                       // We started a new block of coordinates.
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
         * to process all coordinate tuples, setting them to NaN when transform failed.
         */
        if (failure != null) {
            failure.setLastCompletedTransform(this);
            throw failure;
        }
    }

    /**
     * Transforms a list of coordinate tuples. The default implementation delegates
     * to {@link #transform(double[], int, double[], int, int)} using a temporary array of doubles.
     *
     * <h4>Implementation note</h4>
     * See {@link IterationStrategy} javadoc for a method skeleton.
     *
     * @param  srcPts  the array containing the source point coordinates.
     * @param  srcOff  the offset to the first point to be transformed in the source array.
     * @param  dstPts  the array into which the transformed point coordinates are returned.
     *                 May be the same as {@code srcPts}.
     * @param  dstOff  the offset to the location of the first transformed point that is stored in the destination array.
     * @param  numPts  the number of point objects to be transformed.
     * @throws TransformException if a point cannot be transformed. Some implementations will stop at the first failure,
     *         wile some other implementations will fill the un-transformable points with {@link Float#NaN} values,
     *         continue and throw the exception only at end. Implementations that fall in the latter case should set
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
         * We need to check if writing the transformed coordinates in the same array as the source
         * coordinates will cause an overlapping problem. However, we can consider the whole buffer as
         * if it was a single coordinate tuple with a very large dimension. Doing so increase the chances
         * that IterationStrategy.suggest(...) doesn't require us another buffer  (hint: the -1 in
         * suggest(...) mathematic matter and reflect the contract saying that the input coordinates
         * must be fully read before the output coordinates is written - which is the behavior we get
         * with our buffer).
         */
        int srcInc = dimSource * numBufferedPts;
        int dstInc = dimTarget * numBufferedPts;
        int srcStop = srcInc;                   // src|dstStop will be used and modified in the do..while loop later.
        int dstStop = dstInc;
        if (srcPts == dstPts) {
            final int numPass = (numPts + numBufferedPts-1) / numBufferedPts;         // Round toward higher integer.
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
         * Computes the offset of the first source coordinates in the buffer. The offset of the
         * first destination coordinates will always be zero.   We compute the source offset in
         * such a way that the default transform(double[],int,double[],int,int) implementation
         * should never needs to copy the source coordinates in yet another temporary buffer.
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
                    /*
                     * If we were applying IterationStrategy.DESCENDING, then srcOff and dstOff
                     * may be negative at this point because the last pass may not fill all the
                     * buffer space. We need to apply the correction below.
                     */
                    srcOff -= (srcStop + srcInc);
                    dstOff -= (dstStop + dstInc);
                }
            }
            for (int i=0; i<srcStop; i++) {
                buffer[bufferedSrcOff + i] = srcPts[srcOff + i];
            }
            assert !IterationStrategy.suggest(bufferedSrcOff, dimSource, 0, dimTarget, numBufferedPts).needBuffer;
            try {
                transform(buffer, bufferedSrcOff, buffer, 0, numBufferedPts);
            } catch (TransformException exception) {
                /*
                 * If an exception occurred but the transform nevertheless declares having been
                 * able to process all coordinate tuples (setting to NaN those that cannot be
                 * transformed), we will keep the first exception (to be propagated at the end
                 * of this method) and continue. Otherwise we will stop immediately.
                 */
                if (exception.getLastCompletedTransform() != this) {
                    throw exception;
                } else if (failure == null) {
                    failure = exception;                        // Keep only the first exception.
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
     * Transforms a list of coordinate tuples. The default implementation delegates
     * to {@link #transform(double[], int, double[], int, int)} using a temporary array of doubles.
     *
     * @param  srcPts  the array containing the source point coordinates.
     * @param  srcOff  the offset to the first point to be transformed in the source array.
     * @param  dstPts  the array into which the transformed point coordinates are returned.
     * @param  dstOff  the offset to the location of the first transformed point that is stored in the destination array.
     * @param  numPts  the number of point objects to be transformed.
     * @throws TransformException if a point cannot be transformed. Some implementations will stop at the first failure,
     *         wile some other implementations will fill the untransformable points with {@linkplain Float#NaN} values,
     *         continue and throw the exception only at end. Implementations that fall in the latter case should set the
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
                // Same comment as in transform(float[], ...,float[], ...)
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
     * Transforms a list of coordinate tuples. The default implementation delegates
     * to {@link #transform(double[], int, double[], int, int)} using a temporary array of doubles
     * if necessary.
     *
     * @param  srcPts  the array containing the source point coordinates.
     * @param  srcOff  the offset to the first point to be transformed in the source array.
     * @param  dstPts  the array into which the transformed point coordinates are returned.
     * @param  dstOff  the offset to the location of the first transformed point that is stored in the destination array.
     * @param  numPts  the number of point objects to be transformed.
     * @throws TransformException if a point cannot be transformed. Some implementations will stop at the first failure,
     *         wile some other implementations will fill the untransformable points with {@linkplain Double#NaN} values,
     *         continue and throw the exception only at end. Implementations that fall in the latter case should set the
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
                buffer[i] = srcPts[srcOff++];
            }
            try {
                transform(buffer, 0, dstPts, dstOff, numBufferedPts);
            } catch (TransformException exception) {
                // Same comment as in transform(float[], ...,float[], ...)
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
     *   <li>Ensure that the {@code point} dimension is equal to this math transform
     *       {@linkplain #getSourceDimensions() source dimensions}.</li>
     *   <li>Copy the coordinates in a temporary array and pass that array to the
     *       {@link #transform(double[], int, double[], int, boolean)} method,
     *       with the {@code derivate} boolean argument set to {@code true}.</li>
     *   <li>If the latter method returned a non-null matrix, returns that matrix.
     *       Otherwise throws {@link TransformException}.</li>
     * </ul>
     *
     * @param  point  the coordinate tuple where to evaluate the derivative.
     * @return the derivative at the specified point (never {@code null}).
     * @throws NullPointerException if the derivative depends on coordinates and {@code point} is {@code null}.
     * @throws MismatchedDimensionException if {@code point} does not have the expected dimension.
     * @throws TransformException if the derivative cannot be evaluated at the specified point.
     */
    @Override
    public Matrix derivative(final DirectPosition point) throws TransformException {
        final int dimSource = getSourceDimensions();
        final double[] coordinates = point.getCoordinate();
        if (coordinates.length != dimSource) {
            throw mismatchedDimension("point", dimSource, coordinates.length);
        }
        final Matrix derivative = transform(coordinates, 0, null, 0, true);
        if (derivative == null) {
            throw new TransformException(Resources.format(Resources.Keys.CanNotComputeDerivative));
        }
        return derivative;
    }

    /**
     * Returns the inverse transform of this object. The default implementation returns
     * {@code this} if this transform is an {@linkplain #isIdentity() identity} transform,
     * or throws an exception otherwise. Subclasses should override this method.
     *
     * <h4>Implementation note</h4>
     * The {@link Inverse} inner class can be used as a base for inverse transform implementations.
     */
    @Override
    public MathTransform inverse() throws NoninvertibleTransformException {
        if (isIdentity()) {
            return this;
        }
        throw new NoninvertibleTransformException(Resources.format(Resources.Keys.NonInvertibleTransform));
    }

    /**
     * Concatenates or pre-concatenates in an optimized way this math transform with the given one, if possible.
     * If an optimization is possible, a new math transform is created to perform the combined transformation.
     * The {@code applyOtherFirst} value determines the transformation order as bellow:
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
     * If no optimization is available for the combined transform, then this method returns {@code null}.
     *
     * @param  applyOtherFirst  {@code true} if the transformation order is {@code other} followed by {@code this}, or
     *                          {@code false} if the transformation order is {@code this} followed by {@code other}.
     * @param  other            the other math transform to (pre-)concatenate with this transform.
     * @param  factory          the factory which is (indirectly) invoking this method, or {@code null} if none.
     * @return the math transforms combined in an optimized way, or {@code null} if no such optimization is available.
     * @throws FactoryException if an error occurred while combining the transforms.
     *
     * @since 0.8
     *
     * @deprecated Replaced by {@link #tryConcatenate(TransformJoiner)}.
     *             See <a href="https://issues.apache.org/jira/browse/SIS-595">SIS-595</a>.
     */
    @Deprecated(forRemoval=true, since="1.5")
    protected MathTransform tryConcatenate(boolean applyOtherFirst, MathTransform other, MathTransformFactory factory)
            throws FactoryException
    {
        return null;
    }

    /**
     * Concatenates in an optimized way this math transform with its neighbor, if possible.
     * If an optimization is possible, a new {@link MathTransform} is created to perform a calculation
     * equivalent to the {@linkplain MathTransforms#getSteps(MathTransform) sequence of transform steps}.
     * If no optimization is available, the concatenation will be done by a generic implementation which
     * will execute one transform step after the other.
     *
     * <p>This method is ought to be overridden by subclasses capable of replacing a specific sequence of steps.
     * The replacement is specific to each {@code AbstractMathTransform} implementation because the replacement
     * typically uses mathematical identities that depend on the formulas used by this transform.
     * This method does not need to care about the following more generic cases:
     * {@linkplain #isIdentity() identity} transforms, transforms immediately followed by their {@linkplain #inverse() inverses},
     * multiplication of {@linkplain MathTransforms#getMatrix(MathTransform) matrices} in consecutive transforms.</p>
     *
     * @param  context  information about the neighbor transforms, and the object where to set the result.
     * @throws FactoryException if an error occurred while combining the transforms.
     *
     * @see DefaultMathTransformFactory#createConcatenatedTransform(MathTransform, MathTransform)
     *
     * @since 1.5
     */
    protected void tryConcatenate(final TransformJoiner context) throws FactoryException {
        // TODO: make this method empty after the deprecated method has been removed.
        if (context.replacement == null) {
            boolean applyOtherFirst = false;
            do {
                final MathTransform other = context.getTransform(applyOtherFirst ? -1 : +1).orElse(null);
                if (other != null) {
                    context.replacement = tryConcatenate(applyOtherFirst, other, context.factory);
                    if (context.replacement != null) {
                        break;
                    }
                }
            } while ((applyOtherFirst = !applyOtherFirst) == true);
        }
    }

    /**
     * Returns a hash value for this transform. This method invokes {@link #computeHashCode()}
     * when first needed and caches the value for future invocations. Subclasses shall override
     * {@code computeHashCode()} instead of this method.
     *
     * @return the hash code value. This value may change between different execution of the Apache SIS library.
     */
    @Override
    public final int hashCode() {               // No need to synchronize; ok if invoked twice.
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
     * @return the hash code value. This value may change between different execution of the Apache SIS library.
     */
    protected int computeHashCode() {
        return getClass().hashCode() + getSourceDimensions() + 31 * getTargetDimensions();
    }

    /**
     * Compares the specified object with this math transform for strict equality.
     * This method is implemented as below (omitting assertions):
     *
     * {@snippet lang="java" :
     *     return equals(other, ComparisonMode.STRICT);
     *     }
     *
     * @param  object  the object to compare with this transform.
     * @return {@code true} if the given object is a transform of the same class and using the same parameter values.
     * @throws AssertionError if assertions are enabled and the objects are equal but their hash codes are different.
     */
    @Override
    @ArgumentCheckByAssertion
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
     * equal or {@link ComparisonMode#APPROXIMATE approximately} equal.
     * This method may conservatively returns {@code false} if unsure.
     *
     * <p>The default implementation returns {@code true} if the following conditions are met:</p>
     * <ul>
     *   <li>{@code object} is an instance of the same class as {@code this}. We require the
     *       same class because there is no interface for the various kinds of transform.</li>
     *   <li>If the hash code value has already been {@linkplain #computeHashCode() computed} for both
     *       instances, their values are the same <i>(opportunist performance enhancement)</i>.</li>
     *   <li>The {@linkplain #getContextualParameters() contextual parameters} are equal according
     *       the given comparison mode.</li>
     * </ul>
     *
     * @param  object  the object to compare with this transform.
     * @param  mode    the strictness level of the comparison. Default to {@link ComparisonMode#STRICT STRICT}.
     * @return {@code true} if the given object is considered equals to this math transform.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        /*
         * Do not check 'object==this' here, since this
         * optimization is usually done in subclasses.
         */
        if (object != null && getClass() == object.getClass()) {
            final var that = (AbstractMathTransform) object;
            /*
             * If the classes are the same, then the hash codes should be computed in the same way. Since those
             * codes are cached, this is an efficient way to quickly check if the two objects are different.
             */
            if (!mode.isApproximate()) {
                final int tc = hashCode;
                if (tc != 0) {
                    final int oc = that.hashCode;
                    if (oc != 0 && tc != oc) {
                        return false;
                    }
                }
            }
            // See the policy documented in the LenientComparable javadoc.
            if (mode.isIgnoringMetadata()) {
                return true;
            }
            /*
             * We do not compare getParameters() because they usually duplicate the internal fields.
             * Contextual parameters, on the other hand, typically contain new information.
             */
            return Utilities.deepEquals(this.getContextualParameters(),
                                        that.getContextualParameters(), mode);
        }
        return false;
    }

    /**
     * Given a transformation chain, replaces the elements around {@code transforms.get(index)} transform by
     * alternative objects to use when formatting WKT. The replacement is performed in-place in the given list.
     *
     * <p>This method shall replace only the previous element and the few next elements that need
     * to be changed as a result of the previous change. This method is not expected to continue
     * the iteration after the changes that are of direct concern to this object.</p>
     *
     * <p>This method is invoked only by {@link ConcatenatedTransform#getPseudoSteps()} in order to
     * get the {@link ParameterValueGroup} of a map projection, or to format a {@code PROJCS} WKT.</p>
     *
     * @param  transforms  the full chain of concatenated transforms.
     * @param  index       the index of this transform in the {@code transforms} chain.
     * @param  inverse     always {@code false}, except if we are formatting the inverse transform.
     * @return index of this transform in the {@code transforms} chain after processing.
     *
     * @see ConcatenatedTransform#getPseudoSteps()
     */
    int beforeFormat(final List<Object> transforms, final int index, final boolean inverse) {
        assert unwrap(transforms.get(index), inverse) == this;
        final ContextualParameters parameters = getContextualParameters();
        return (parameters != null) ? parameters.beforeFormat(transforms, index, inverse) : index;
    }

    /**
     * Formats the inner part of a <i>Well Known Text</i> version 1 (WKT 1) element.
     * The default implementation formats all parameter values returned by {@link #getParameterValues()}.
     * The parameter group name is used as the math transform name.
     *
     * <h4>Compatibility note</h4>
     * {@code Param_MT} is defined in the WKT 1 specification only.
     * If the {@linkplain Formatter#getConvention() formatter convention} is set to WKT 2,
     * then this method silently uses the WKT 1 convention without raising an error
     * (unless this {@code MathTransform} cannot be formatted as valid WKT 1 neither).
     *
     * @param  formatter  the formatter to use.
     * @return the WKT element name, which is {@code "Param_MT"} in the default implementation.
     *
     * @see DefaultMathTransformFactory#createFromWKT(String)
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        WKTUtilities.appendParamMT(getParameterValues(), formatter);
        return WKTKeywords.Param_MT;
    }

    /**
     * Unwraps the given object if it is expected to be an inverse transform.
     * This is used for assertions only.
     */
    private static Object unwrap(final Object object, final boolean inverse) {
        return inverse ? ((Inverse) object).inverse() : object;
    }

    /**
     * Base class for implementations of inverse math transforms.
     * Subclasses need to implement the {@link #inverse()} method.
     *
     * <h2>Serialization</h2>
     * This object may or may not be serializable, at implementation choices.
     * Most Apache SIS implementations are serializable, but the serialized objects are not guaranteed to be compatible
     * with future SIS versions. Serialization should be used only for short term storage or RMI between applications
     * running the same SIS version.
     *
     * @author  Martin Desruisseaux (IRD, Geomatys)
     * @version 1.0
     * @since   0.5
     */
    protected abstract static class Inverse extends AbstractMathTransform {
        /**
         * Constructs an inverse math transform.
         */
        protected Inverse() {
        }

        /**
         * Gets the dimension of input points.
         * The default implementation returns the dimension of output points
         * of the {@linkplain #inverse() inverse} math transform.
         *
         * @return {@inheritDoc}
         */
        @Override
        public int getSourceDimensions() {
            return inverse().getTargetDimensions();
        }

        /**
         * Gets the dimension of output points.
         * The default implementation returns the dimension of input points
         * of the {@linkplain #inverse() inverse} math transform.
         *
         * @return {@inheritDoc}
         */
        @Override
        public int getTargetDimensions() {
            return inverse().getSourceDimensions();
        }

        /**
         * Returns the ranges of coordinate values which can be used as inputs.
         * The default implementation invokes {@code inverse().getDomain(criteria)}
         * and transforms the returned envelope.
         *
         * @param  criteria  controls the definition of transform domain.
         * @return estimation of a domain where this transform is considered numerically applicable.
         * @throws TransformException if the domain cannot be estimated.
         *
         * @since 1.3
         */
        @Override
        public Optional<Envelope> getDomain(final DomainDefinition criteria) throws TransformException {
            final MathTransform inverse = inverse();
            if (inverse instanceof AbstractMathTransform) {
                final Optional<Envelope> domain = ((AbstractMathTransform) inverse).getDomain(criteria);
                return Optional.ofNullable(criteria.intersectOrTransform(domain.orElse(null), inverse));
            }
            return Optional.empty();
        }

        /**
         * Gets the derivative of this transform at a point.
         * The default implementation computes the inverse of the matrix
         * returned by the {@linkplain #inverse() inverse} math transform.
         *
         * @return {@inheritDoc}
         * @throws NullPointerException if the derivative depends on coordinates and {@code point} is {@code null}.
         * @throws MismatchedDimensionException if {@code point} does not have the expected dimension.
         * @throws TransformException if the derivative cannot be evaluated at the specified point.
         */
        @Override
        public Matrix derivative(DirectPosition point) throws TransformException {
            if (point != null) {
                point = this.transform(point, null);
            }
            return Matrices.inverse(inverse().derivative(point));
        }

        /**
         * Returns the inverse of this math transform.
         * The returned transform should be the enclosing math transform.
         *
         * @return the inverse of this transform.
         */
        @Override
        public abstract MathTransform inverse();

        /**
         * Tests whether this transform does not move any points.
         * The default implementation delegates this tests to the {@linkplain #inverse() inverse} math transform.
         *
         * @return {@inheritDoc}
         */
        @Override
        public boolean isIdentity() {
            return inverse().isIdentity();
        }

        /**
         * {@inheritDoc}
         *
         * @return {@inheritDoc}
         */
        @Override
        protected int computeHashCode() {
            return super.computeHashCode() + 31 * inverse().hashCode();
        }

        /**
         * Compares the specified object with this inverse math transform for equality.
         * The default implementation tests if {@code object} in an instance of the same class as {@code this},
         * and if so compares their {@linkplain #inverse() inverse} {@code MathTransform}.
         *
         * @return {@inheritDoc}
         */
        @Override
        public boolean equals(final Object object, final ComparisonMode mode) {
            if (object == this) {
                return true;                                            // Slight optimization
            }
            if (object != null && object.getClass() == getClass()) {
                final MathTransform other = ((Inverse) object).inverse();
                final MathTransform inverse = inverse();
                if (inverse instanceof LenientComparable) {
                    return ((LenientComparable) inverse).equals(other, mode);
                } else {
                    return inverse.equals(other);
                }
            } else {
                return false;
            }
        }

        /**
         * Same work as {@link AbstractMathTransform#beforeFormat(List, int, boolean)}
         * but with the knowledge that this transform is an inverse transform.
         */
        @Override
        int beforeFormat(final List<Object> transforms, final int index, final boolean inverse) {
            final ContextualParameters parameters = getContextualParameters();
            if (parameters != null) {
                return parameters.beforeFormat(transforms, index, inverse);
            } else {
                final MathTransform inv = inverse();
                if (inv instanceof AbstractMathTransform) {
                    return ((AbstractMathTransform) inv).beforeFormat(transforms, index, !inverse);
                } else {
                    return index;
                }
            }
        }

        /**
         * Formats the inner part of a <i>Well Known Text</i> version 1 (WKT 1) element.
         * If this inverse math transform has any parameter values, then this method formats
         * the WKT as in the {@linkplain AbstractMathTransform#formatWKT super-class method}.
         * Otherwise this method formats the math transform as an {@code "Inverse_MT"} entity.
         *
         * <h4>Compatibility note</h4>
         * {@code Param_MT} and {@code Inverse_MT} are defined in the WKT 1 specification only.
         *
         * @param  formatter  the formatter to use.
         * @return the WKT element name, which is {@code "Param_MT"} or
         *         {@code "Inverse_MT"} in the default implementation.
         */
        @Override
        protected String formatTo(final Formatter formatter) {
            final ParameterValueGroup parameters = getParameterValues();
            if (parameters != null) {
                WKTUtilities.appendParamMT(parameters, formatter);
                return WKTKeywords.Param_MT;
            } else {
                formatter.newLine();
                formatter.append((FormattableObject) inverse());
                return WKTKeywords.Inverse_MT;
            }
        }
    }
}
