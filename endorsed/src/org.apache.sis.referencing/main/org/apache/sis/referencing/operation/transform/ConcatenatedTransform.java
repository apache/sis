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
import java.util.ArrayList;
import java.util.Optional;
import java.io.Serializable;
import org.opengis.util.FactoryException;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.parameter.Parameterized;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.provider.GeocentricAffine;
import org.apache.sis.referencing.privy.WKTKeywords;
import org.apache.sis.referencing.privy.ReferencingUtilities;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.system.Semaphores;
import org.apache.sis.util.Classes;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.privy.Strings;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.referencing.ExportableTransform;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.geometry.MismatchedDimensionException;


/**
 * Base class for concatenated transforms. Instances can be created by calls to the
 * {@link #create(MathTransform, MathTransform, MathTransformFactory)} method.
 * When possible, the above-cited method concatenates {@linkplain ProjectiveTransform projective transforms}
 * before to fallback on the creation of new {@code ConcatenatedTransform} instances.
 *
 * <p>Concatenated transforms are serializable if all their step transforms are serializable.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 *
 * @see MathTransformFactory#createConcatenatedTransform(MathTransform, MathTransform)
 */
class ConcatenatedTransform extends AbstractMathTransform implements Serializable, ExportableTransform {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 5772066656987558634L;

    /**
     * Tolerance threshold for considering a matrix as identity. Since the value used here is smaller
     * than 1 ULP (about 2.22E-16), it applies only to the zero terms in the matrix. The terms on the
     * diagonal are still expected to be exactly 1.
     */
    private static final double IDENTITY_TOLERANCE = 1E-16;

    /**
     * The first math transform.
     */
    @SuppressWarnings("serial")                 // Most SIS implementations are serializable.
    protected final MathTransform transform1;

    /**
     * The second math transform.
     */
    @SuppressWarnings("serial")                 // Most SIS implementations are serializable.
    protected final MathTransform transform2;

    /**
     * The inverse transform. This field will be computed only when needed.
     * But it is serialized in order to avoid rounding errors if the inverse
     * transform is serialized instead of the original one.
     *
     * @see #inverse()
     */
    @SuppressWarnings("serial")                 // Most SIS implementations are serializable.
    private MathTransform inverse;

    /**
     * Constructs a concatenated transform. This constructor is for subclasses only.
     * To create a concatenated transform, use the {@link #create create(…)} method instead.
     *
     * @param  transform1  the first math transform.
     * @param  transform2  the second math transform.
     */
    protected ConcatenatedTransform(final MathTransform transform1,
                                    final MathTransform transform2)
    {
        this.transform1 = transform1;
        this.transform2 = transform2;
        if (!isValid()) {
            throw new IllegalArgumentException(Resources.format(Resources.Keys.CanNotConcatenateTransforms_2,
                    getName(transform1), getName(transform2)));
        }
    }

    /**
     * Concatenates the two given transforms.
     * If the concatenation result works with two-dimensional input and output points,
     * then the returned transform will implement {@link MathTransform2D}.
     * Likewise if the concatenation result works with one-dimensional input and output points,
     * then the returned transform will implement {@link MathTransform1D}.
     *
     * <h4>Implementation note</h4>
     * {@code ConcatenatedTransform} implementations are available in two versions: direct and non-direct.
     * The "non-direct" versions use an intermediate buffer when performing transformations; they are slower
     * and consume more memory. They are used only as a fallback when a "direct" version cannot be created.
     *
     * @param  tr1      the first math transform.
     * @param  tr2      the second math transform.
     * @param  factory  the factory which is (indirectly) invoking this method, or {@code null} if none.
     * @return the concatenated transform.
     *
     * @see MathTransforms#concatenate(MathTransform, MathTransform)
     */
    public static MathTransform create(final MathTransform tr1, final MathTransform tr2, final MathTransformFactory factory)
            throws FactoryException, MismatchedDimensionException
    {
        final int dim1 = tr1.getTargetDimensions();
        final int dim2 = tr2.getSourceDimensions();
        if (dim1 != dim2) {
            throw new MismatchedDimensionException(Resources.format(Resources.Keys.CanNotConcatenateTransforms_2, getName(tr1),
                    getName(tr2)) + ' ' + Errors.format(Errors.Keys.MismatchedDimension_2, dim1, dim2));
        }
        final MathTransform mt = tryOptimized(tr1, tr2, factory);
        if (mt != null) {
            return mt;
        }
        /*
         * Cannot avoid the creation of a ConcatenatedTransform object.
         * Check for the type to create (1D, 2D, general case...)
         */
        final int dimSource = tr1.getSourceDimensions();
        final int dimTarget = tr2.getTargetDimensions();
        if (dimSource == 1 && dimTarget == 1) {
            /*
             * Result needs to be a MathTransform1D.
             */
            if (tr1 instanceof MathTransform1D && tr2 instanceof MathTransform1D) {
                return new ConcatenatedTransformDirect1D((MathTransform1D) tr1,
                                                         (MathTransform1D) tr2);
            } else {
                return new ConcatenatedTransform1D(tr1, tr2);
            }
        } else if (dimSource == 2 && dimTarget == 2) {
            /*
             * Result needs to be a MathTransform2D.
             */
            if (tr1 instanceof MathTransform2D && tr2 instanceof MathTransform2D) {
                return new ConcatenatedTransformDirect2D((MathTransform2D) tr1,
                                                         (MathTransform2D) tr2);
            } else {
                return new ConcatenatedTransform2D(tr1, tr2);
            }
        } else if (dimSource == dim1 && dimTarget == dim2) {
            return new ConcatenatedTransformDirect(tr1, tr2);
        } else {
            return new ConcatenatedTransform(tr1, tr2);
        }
    }

    /**
     * Tries to return an optimized concatenation, for example by merging two affine transforms
     * into a single one. If no optimized case has been found, returns {@code null}. In the latter
     * case, the caller will need to create a more heavy {@link ConcatenatedTransform} instance.
     *
     * @param  factory  the factory which is (indirectly) invoking this method, or {@code null} if none.
     */
    private static MathTransform tryOptimized(final MathTransform tr1, final MathTransform tr2,
                                              MathTransformFactory factory) throws FactoryException
    {
        /*
         * Trivial - but actually essential!! - check for the identity cases.
         */
        if (tr1.isIdentity()) return tr2;
        if (tr2.isIdentity()) return tr1;
        /*
         * Give a chance to AbstractMathTransform to return an optimized object. For example, LogarithmicTransform
         * concatenated with ExponentialTransform can produce a new formula, PassThrouthTransform may concatenate
         * its sub-transform, etc. We try both ways (concatenation and pre-concatenation) and see which way gives
         * the shortest concatenation chain. It is not that much expensive given that most implementations return
         * null directly.
         */
        int stepCount = 0;
        MathTransform shortest = null;
        boolean inverseCaseTested = false;
        if (tr1 instanceof AbstractMathTransform) {
            // TODO: after removal of the deprecated method, invoke `tryConcatenate(Joiner)` only once.
            @SuppressWarnings("removal")
            final MathTransform optimized = ((AbstractMathTransform) tr1).tryConcatenate(false, tr2, factory);
            inverseCaseTested = true;
            if (optimized != null) {
                stepCount = getStepCount(optimized);
                shortest  = optimized;
            }
        }
        if (tr2 instanceof AbstractMathTransform) {
            @SuppressWarnings("removal")
            final MathTransform optimized = ((AbstractMathTransform) tr2).tryConcatenate(true, tr1, factory);
            inverseCaseTested = true;
            if (optimized != null) {
                if (shortest == null || getStepCount(optimized) < stepCount) {
                    return optimized;
                }
                shortest = optimized;
            }
        }
        if (shortest != null) {
            return shortest;
        }
        /*
         * If one transform is the inverse of the other, return the identity transform.
         * We need to test this case before the linear transform case below, because the
         * matrices may contain NaN values.
         */
        if (!inverseCaseTested && (isInverseEquals(tr1, tr2) || isInverseEquals(tr2, tr1))) {
            assert tr1.getSourceDimensions() == tr2.getTargetDimensions();
            assert tr1.getTargetDimensions() == tr2.getSourceDimensions();
            return MathTransforms.identity(tr1.getSourceDimensions());          // Returns a cached instance.
        }
        /*
         * If both transforms use matrix, then we can create
         * a single transform using the concatenated matrix.
         */
        factory = ReferencingUtilities.nonNull(factory);
        final MathTransform concatenated = multiply(tr1, tr2, factory);
        if (concatenated instanceof AbstractLinearTransform) {
            /*
             * Following code computes the inverse of `concatenated` transform.  In principle this is not necessary
             * because `AbstractLinearTransform.inverse()` can inverse the matrix when first needed. However if the
             * matrices are not square (e.g. if a transform is dropping a dimension), some information may be lost.
             * By computing inverse transform now as the product of matrices provided by the two inverse transforms
             * (as opposed to inverting the product of forward transform matrices), we use information that would
             * otherwise be lost (e.g. the inverse of the transform dropping a dimension may be a transform setting
             * that dimension to a constant value, often zero). Consequently, the inverse transform here may have real
             * values for coefficients that `AbstractLinearTransform.inverse()` would have set to NaN, or may succeed
             * where `AbstractLinearTransform.inverse()` would have throw an exception. Even with square matrices,
             * computing the inverse transform now may avoid some rounding errors.
             */
            final var impl = (AbstractLinearTransform) concatenated;
            if (impl.inverse == null) try {
                final MathTransform inverse = multiply(tr2.inverse(), tr1.inverse(), factory);
                if (inverse != null) {
                    if (inverse.isIdentity()) {
                        return inverse;
                    }
                    if (inverse instanceof LinearTransform) {
                        impl.inverse = (LinearTransform) inverse;
                    }
                }
            } catch (NoninvertibleTransformException e) {
                /*
                 * Ignore. The `concatenated.inverse()` method will try to compute the inverse itself,
                 * possible at the cost of more NaN values than what above code would have produced.
                 */
                Logging.recoverableException(LOGGER, ConcatenatedTransform.class, "create", e);
            }
        }
        return concatenated;
    }

    /**
     * Returns a transform resulting from the multiplication of the matrices of given transforms.
     * If the given transforms does not provide matrix, then this method returns {@code null}.
     *
     * @param  factory  wrapper for the factory which is (indirectly) invoking this method.
     */
    private static MathTransform multiply(final MathTransform tr1, final MathTransform tr2,
            final MathTransformFactory factory) throws FactoryException
    {
        final Matrix matrix1 = MathTransforms.getMatrix(tr1);
        if (matrix1 != null) {
            final Matrix matrix2 = MathTransforms.getMatrix(tr2);
            if (matrix2 != null) {
                final Matrix matrix = Matrices.multiply(matrix2, matrix1);
                if (Matrices.isIdentity(matrix, IDENTITY_TOLERANCE)) {
                    return MathTransforms.identity(matrix.getNumRow() - 1);         // Returns a cached instance.
                }
                /*
                 * NOTE: It is quite tempting to "fix rounding errors" in the matrix before to create the transform.
                 * But this is often wrong for datum shift transformations (Molodensky and the like) since the datum
                 * shifts are very small. The shift may be the order of magnitude of the tolerance threshold. Instead,
                 * Apache SIS performs matrix operations using double-double arithmetic in the hope to get exact
                 * results at the `double` accuracy, which avoid the need for a tolerance threshold.
                 */
                return factory.createAffineTransform(matrix);
            }
        }
        // No optimized case found.
        return null;
    }

    /**
     * Returns a name for the specified math transform.
     */
    private static String getName(final MathTransform transform) {
        ParameterValueGroup params = null;
        if (transform instanceof AbstractMathTransform) {
            params = ((AbstractMathTransform) transform).getContextualParameters();
        }
        if (params == null && (transform instanceof Parameterized)) {
            params = ((Parameterized) transform).getParameterValues();
        }
        if (params != null) {
            final String name = Strings.trimOrNull(params.getDescriptor().getName().getCode());
            if (name != null) {
                return name;
            }
        }
        return Classes.getShortClassName(transform);
    }

    /**
     * Checks if transforms are compatibles. The default
     * implementation check if transfer dimension match.
     */
    boolean isValid() {
        return transform1.getTargetDimensions() == transform2.getSourceDimensions();
    }

    /**
     * Gets the dimension of input points.
     *
     * @return {@inheritDoc}
     */
    @Override
    public final int getSourceDimensions() {
        return transform1.getSourceDimensions();
    }

    /**
     * Gets the dimension of output points.
     *
     * @return {@inheritDoc}
     */
    @Override
    public final int getTargetDimensions() {
        return transform2.getTargetDimensions();
    }

    /**
     * Returns the number of single {@link MathTransform} steps.
     * Nested concatenated transforms (if any) are explored recursively in order to
     * get the count of single (non-nested) transforms.
     *
     * @return the number of single transform steps.
     */
    private int getStepCount() {
        return getStepCount(transform1) + getStepCount(transform2);
    }

    /**
     * Returns the number of single {@linkplain MathTransform math transform} steps performed
     * by the given transform. As a special case, we returns 0 for the identity transform since
     * it should be omitted from the final chain.
     */
    private static int getStepCount(final MathTransform transform) {
        if (transform.isIdentity()) {
            return 0;
        }
        if (!(transform instanceof ConcatenatedTransform)) {
            return 1;
        }
        return ((ConcatenatedTransform) transform).getStepCount();
    }

    /**
     * Returns all concatenated transforms. The returned list contains only <i>single</i> transforms,
     * i.e. all nested concatenated transforms (if any) have been flattened.
     *
     * @return all single math transforms performed by this concatenated transform.
     *
     * @see MathTransforms#getSteps(MathTransform)
     */
    public final List<MathTransform> getSteps() {
        final var transforms = new ArrayList<MathTransform>(5);
        getSteps(transforms);
        return transforms;
    }

    /**
     * Returns all concatenated transforms, modified with the pre- and post-processing required for WKT formatting.
     * More specifically, if there is any Apache SIS implementation of Map Projection in the chain, then the
     * (<var>normalize</var>, <var>normalized projection</var>, <var>denormalize</var>) tuples are replaced by single
     * (<var>projection</var>) elements, which does not need to be instances of {@link MathTransform}.
     *
     * <p>This method is used only for producing human-readable parameter values.
     * It is not used for coordinate operations or construction of operation chains.</p>
     *
     * @return the pseudo-steps as instances of {@link MathTransform}, {@link Matrix} or {@link FormattableObject}.
     */
    private List<Object> getPseudoSteps() {
        final var transforms = new ArrayList<Object>();
        getSteps(transforms);
        /*
         * Pre-process the transforms before to format. Some steps may be merged, or new
         * steps may be created. Do not move size() out of the loop, because it may change.
         */
        for (int i=0; i<transforms.size(); i++) {
            final Object step = transforms.get(i);
            if (step instanceof AbstractMathTransform) {
                i = ((AbstractMathTransform) step).beforeFormat(transforms, i, false);
            }
        }
        /*
         * Merge consecutive affine transforms. The transforms list should never contain consecutive instances
         * of LinearTransform because the ConcatenatedTransform.create(…) method already merged them  (this is
         * verified by assertions in MathTransforms). However, the above loop may have created synthetic affine
         * transforms for WKT formatting purpose. Those synthetic affine transforms are actually represented by
         * Matrix objects (rather than full MathTransform objects), and two matrices may have been generated
         * consecutively.
         */
        Matrix after = null;
        for (int i = transforms.size(); --i >= 0;) {
            final Object step = transforms.get(i);
            if (step instanceof Matrix) {
                if (after != null) {
                    final Matrix merged = Matrices.multiply(after, (Matrix) step);
                    if (merged.isIdentity()) {
                        transforms.subList(i, i+2).clear();
                        after = null;
                    } else {
                        transforms.set(i, MathTransforms.linear(merged));
                        transforms.remove(i+1);
                        after = merged;
                    }
                } else {
                    after = (Matrix) step;
                }
            } else {
                after = null;
            }
        }
        /*
         * Special case for datum shifts. Need to be done only after we processed
         * `beforeFormat(…)` for all objects and concatenated the affine transforms.
         */
        GeocentricAffine.asDatumShift(transforms);
        return transforms;
    }

    /**
     * Adds all concatenated transforms in the given list.
     *
     * @param  transforms  the list where to add concatenated transforms.
     */
    private void getSteps(final List<? super MathTransform> transforms) {
        if (transform1 instanceof ConcatenatedTransform) {
            ((ConcatenatedTransform) transform1).getSteps(transforms);
        } else {
            transforms.add(transform1);
        }
        if (transform2 instanceof ConcatenatedTransform) {
            ((ConcatenatedTransform) transform2).getSteps(transforms);
        } else {
            transforms.add(transform2);
        }
    }

    /**
     * If there is exactly one transform step which is {@linkplain Parameterized parameterized},
     * returns that transform step. Otherwise returns {@code null}.
     *
     * <p>This method normally requires that there is exactly one transform step remaining after we
     * processed map projections in the special way described in {@link #getParameterValues()},
     * because if they were more than one remaining steps, the returned parameters would not be
     * sufficient for rebuilding the full concatenated transform. Returning parameters when there
     * is more than one remaining step, even if all other transform steps are not parameterizable,
     * would be a contract violation.</p>
     *
     * <p>However, in the special case where we are getting the parameters of a {@code CoordinateOperation} instance
     * through {@link org.apache.sis.referencing.operation.AbstractCoordinateOperation#getParameterValues()} method
     * (often indirectly trough WKT formatting of a {@code "ProjectedCRS"} element), then the above rule is slightly
     * relaxed: we ignore affine transforms in order to accept axis swapping or unit conversions. We do that in that
     * particular case only because the coordinate systems given with the enclosing {@code CoordinateOperation} or
     * {@code DerivedCRS} specify the axis swapping and unit conversions.
     * This special case is internal to SIS implementation and should be unknown to users.</p>
     *
     * @return the parameterizable transform step, or {@code null} if none.
     */
    private Parameterized getParameterised() {
        Parameterized param = null;
        final List<Object> transforms = getPseudoSteps();
        if (transforms.size() == 1 || Semaphores.query(Semaphores.ENCLOSED_IN_OPERATION)) {
            for (final Object candidate : transforms) {
                /*
                 * Search for non-linear parameters only, ignoring affine transforms and the matrices
                 * computed by ContextualParameters. Note that the `transforms` list is guaranteed to
                 * contains at least one non-linear parameter, otherwise we would not have created a
                 * `ConcatenatedTransform` instance.
                 */
                if (!(candidate instanceof Matrix || MathTransforms.isLinear(candidate))) {
                    if ((param == null) && (candidate instanceof Parameterized)) {
                        param = (Parameterized) candidate;
                    } else {
                        /*
                         * Found more than one group of non-linear parameters, or found an object
                         * that do not declare its parameters.  In the latter case, conservatively
                         * returns `null` because we do not know what the real parameters are.
                         */
                        return null;
                    }
                }
            }
        }
        return param;
    }

    /**
     * Returns the parameter descriptor, or {@code null} if none.
     * This method performs the same special check as {@link #getParameterValues()}.
     */
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        final Parameterized param = getParameterised();
        return (param != null) ? param.getParameterDescriptors() : null;
    }

    /**
     * Returns the parameter values, or {@code null} if none. Concatenated transforms usually have
     * no parameters; instead the parameters of the individual components ({@link #transform1} and
     * {@link #transform2}) need to be inspected. However, map projections in SIS are implemented as
     * (<i>normalize</i> – <i>non-linear kernel</i> – <i>denormalize</i>) tuples.
     * This method detects such concatenation chains in order to return the parameter values that
     * describe the projection as a whole.
     */
    @Override
    public ParameterValueGroup getParameterValues() {
        final Parameterized param = getParameterised();
        return (param != null) ? param.getParameterValues() : null;
    }

    /**
     * Transforms the specified {@code ptSrc} and stores the result in {@code ptDst}.
     *
     * @throws TransformException if {@link #transform1} or {@link #transform2} failed.
     */
    @Override
    public DirectPosition transform(final DirectPosition ptSrc, final DirectPosition ptDst)
            throws TransformException
    {
        assert isValid();
        /*
         * Note: If we know that the transfer dimension is the same as source
         *       and target dimension, then we don't need to use an intermediate
         *       point. This optimization is done in ConcatenatedTransformDirect.
         */
        return transform2.transform(transform1.transform(ptSrc, null), ptDst);
    }

    /**
     * Transforms a single position in a list of coordinate values,
     * and optionally returns the derivative at that location.
     *
     * @throws TransformException if {@link #transform1} or {@link #transform2} failed.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate) throws TransformException
    {
        assert isValid();
        final int bufferDim = transform2.getSourceDimensions();
        final int targetDim = transform2.getTargetDimensions();
        final double[] buffer;
        final int offset;
        if (bufferDim > targetDim) {
            buffer = new double[bufferDim];
            offset = 0;
        } else {
            buffer = dstPts;
            offset = dstOff;
        }
        if (derivate) {
            final Matrix matrix1 = MathTransforms.derivativeAndTransform(transform1, srcPts, srcOff, buffer, offset);
            final Matrix matrix2 = MathTransforms.derivativeAndTransform(transform2, buffer, offset, dstPts, dstOff);
            return Matrices.multiply(matrix2, matrix1);
        } else {
            transform1.transform(srcPts, srcOff, buffer, offset, 1);
            transform2.transform(buffer, offset, dstPts, dstOff, 1);
            return null;
        }
    }

    /**
     * Transforms many positions in a list of coordinate values. The source points are first
     * transformed by {@link #transform1}, then the intermediate points are transformed by
     * {@link #transform2}. The transformations are performed without intermediate buffer
     * if it can be avoided.
     *
     * @throws TransformException if {@link #transform1} or {@link #transform2} failed.
     */
    @Override
    public void transform(final double[] srcPts, int srcOff,
                          final double[] dstPts, int dstOff, int numPts)
            throws TransformException
    {
        assert isValid();
        int bufferDim = transform2.getSourceDimensions();
        int targetDim = transform2.getTargetDimensions();
        /*
         * If the transfer dimension is not greater than the target dimension, then we
         * don't need to use an intermediate buffer. Note that this optimization is done
         * unconditionally in ConcatenatedTransformDirect.
         */
        if (bufferDim <= targetDim) {
            transform1.transform(srcPts, srcOff, dstPts, dstOff, numPts);
            transform2.transform(dstPts, dstOff, dstPts, dstOff, numPts);
            return;
        }
        if (numPts <= 0) {
            return;
        }
        /*
         * Creates a temporary array for the intermediate result. The array may be smaller than
         * the length necessary for containing every coordinates. In such case the concatenated
         * transform will need to be applied piecewise with special care in case of overlapping
         * arrays.
         */
        boolean descending = false;
        int sourceDim = transform1.getSourceDimensions();
        int numBuf = numPts;
        int length = numBuf * bufferDim;
        if (length > MAXIMUM_BUFFER_SIZE) {
            numBuf = Math.max(1, MAXIMUM_BUFFER_SIZE / bufferDim);
            if (srcPts == dstPts) {
                // Since we are using a buffer, the whole buffer is like a single coordinate tuple.
                switch (IterationStrategy.suggest(srcOff, numBuf*sourceDim, dstOff, numBuf*targetDim, numPts)) {
                    default: {
                        // Needs to copy the whole data.
                        numBuf = numPts;
                        break;
                    }
                    case ASCENDING: {
                        // No special care needed.
                        break;
                    }
                    case DESCENDING: {
                        // Traversing in reverse order is sufficient.
                        final int shift = numPts - numBuf;
                        srcOff += shift*sourceDim; sourceDim = -sourceDim;
                        dstOff += shift*targetDim; targetDim = -targetDim;
                        descending = true;
                        break;
                    }
                }
            }
            length = numBuf * bufferDim;
        }
        final double[] buf = new double[length];
        do {
            if (!descending && numBuf > numPts) {
                // Must be done before transforms if we are iterating in ascending order.
                numBuf = numPts;
            }
            transform1.transform(srcPts, srcOff, buf, 0, numBuf);
            transform2.transform(buf, 0, dstPts, dstOff, numBuf);
            numPts -= numBuf;
            if (descending && numBuf > numPts) {
                // Must be done after transforms if we are iterating in descending order.
                numBuf = numPts;
            }
            srcOff += numBuf * sourceDim;
            dstOff += numBuf * targetDim;
        } while (numPts != 0);
    }

    /**
     * Transforms many positions in a list of coordinate values. The source points are first
     * transformed by {@link #transform1}, then the intermediate points are transformed by
     * {@link #transform2}. An intermediate buffer of type {@code double[]} for intermediate
     * results is used for reducing rounding errors.
     *
     * @throws TransformException if {@link #transform1} or {@link #transform2} failed.
     */
    @Override
    public void transform(final float[] srcPts, int srcOff,
                          final float[] dstPts, int dstOff, int numPts)
            throws TransformException
    {
        assert isValid();
        if (numPts <= 0) {
            return;
        }
        boolean descending = false;
        int sourceDim = transform1.getSourceDimensions();
        int bufferDim = transform1.getTargetDimensions();
        int targetDim = transform2.getTargetDimensions();
        int dimension = Math.max(targetDim, bufferDim);
        int numBuf = numPts;
        int length = numBuf * dimension;
        if (length > MAXIMUM_BUFFER_SIZE) {
            numBuf = Math.max(1, MAXIMUM_BUFFER_SIZE / dimension);
            if (srcPts == dstPts) {
                switch (IterationStrategy.suggest(srcOff, numBuf*sourceDim, dstOff, numBuf*targetDim, numPts)) {
                    default: {
                        numBuf = numPts;
                        break;
                    }
                    case ASCENDING: {
                        break;
                    }
                    case DESCENDING: {
                        final int shift = numPts - numBuf;
                        srcOff += shift*sourceDim; sourceDim = -sourceDim;
                        dstOff += shift*targetDim; targetDim = -targetDim;
                        descending = true;
                        break;
                    }
                }
            }
            length = numBuf * dimension;
        }
        final double[] buf = new double[length];
        do {
            if (!descending && numBuf > numPts) {
                numBuf = numPts;
            }
            transform1.transform(srcPts, srcOff, buf, 0, numBuf);
            transform2.transform(buf, 0, dstPts, dstOff, numBuf);
            numPts -= numBuf;
            if (descending && numBuf > numPts) {
                numBuf = numPts;
            }
            srcOff += numBuf * sourceDim;
            dstOff += numBuf * targetDim;
        } while (numPts != 0);
    }

    /**
     * Transforms many positions in a list of coordinate values. The source points are first
     * transformed by {@link #transform1}, then the intermediate points are transformed by
     * {@link #transform2}. An intermediate buffer of type {@code double[]} for intermediate
     * results is used for reducing rounding errors.
     *
     * @throws TransformException if {@link #transform1} or {@link #transform2} failed.
     */
    @Override
    public void transform(final double[] srcPts, int srcOff,
                          final float [] dstPts, int dstOff, int numPts)
            throws TransformException
    {
        // Same code as transform(float[], ..., float[], ...) but the method calls
        // are actually different because of overloading of the "transform" methods.
        assert isValid();
        if (numPts <= 0) {
            return;
        }
        final int sourceDim = transform1.getSourceDimensions();
        final int bufferDim = transform1.getTargetDimensions();
        final int targetDim = transform2.getTargetDimensions();
        final int dimension = Math.max(targetDim, bufferDim);
        int numBuf = numPts;
        int length = numBuf * dimension;
        if (length > MAXIMUM_BUFFER_SIZE) {
            numBuf = Math.max(1, MAXIMUM_BUFFER_SIZE / dimension);
            length = numBuf * dimension;
        }
        final double[] buf = new double[length];
        do {
            if (numBuf > numPts) {
                numBuf = numPts;
            }
            transform1.transform(srcPts, srcOff, buf, 0, numBuf);
            transform2.transform(buf, 0, dstPts, dstOff, numBuf);
            srcOff += numBuf * sourceDim;
            dstOff += numBuf * targetDim;
            numPts -= numBuf;
        } while (numPts != 0);
    }

    /**
     * Transforms many positions in a list of coordinate values. The source points are first
     * transformed by {@link #transform1}, then the intermediate points are transformed by
     * {@link #transform2}. The transformations are performed without intermediate buffer
     * if it can be avoided.
     *
     * @throws TransformException if {@link #transform1} or {@link #transform2} failed.
     */
    @Override
    public void transform(final float [] srcPts, int srcOff,
                          final double[] dstPts, int dstOff, int numPts)
            throws TransformException
    {
        /*
         * Same code as transform(double[], ..., double[], ...) but the method calls
         * are actually different because of overloading of the "transform" methods.
         */
        assert isValid();
        final int bufferDim = transform2.getSourceDimensions();
        final int targetDim = transform2.getTargetDimensions();
        if (bufferDim <= targetDim) {
            transform1.transform(srcPts, srcOff, dstPts, dstOff, numPts);
            transform2.transform(dstPts, dstOff, dstPts, dstOff, numPts);
            return;
        }
        if (numPts <= 0) {
            return;
        }
        int numBuf = numPts;
        int length = numBuf * bufferDim;
        if (length > MAXIMUM_BUFFER_SIZE) {
            numBuf = Math.max(1, MAXIMUM_BUFFER_SIZE / bufferDim);
            length = numBuf * bufferDim;
        }
        final double[] buf = new double[length];
        final int sourceDim = getSourceDimensions();
        do {
            if (numBuf > numPts) {
                numBuf = numPts;
            }
            transform1.transform(srcPts, srcOff, buf, 0, numBuf);
            transform2.transform(buf, 0, dstPts, dstOff, numBuf);
            srcOff += numBuf * sourceDim;
            dstOff += numBuf * targetDim;
            numPts -= numBuf;
        } while (numPts != 0);
    }

    @Override
    public String toECMAScript() throws UnsupportedOperationException {
        final int sub1DimSource = transform1.getSourceDimensions();
        final int sub2DimSource = transform2.getSourceDimensions();

        final StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        if (transform1 instanceof ExportableTransform) {
            final ExportableTransform exp = (ExportableTransform) transform1;
            sb.append("\t_sub1 : ").append(exp.toECMAScript().replace("\n", "\n\t")).append(",\n");
        } else {
            throw new UnsupportedOperationException(transform1.getClass().getName() + " is not an ExportableTransform.");
        }

        if (transform2 instanceof ExportableTransform) {
            final ExportableTransform exp = (ExportableTransform) transform2;
            sb.append("\t_sub2 : ").append(exp.toECMAScript().replace("\n", "\n\t")).append(",\n");
        } else {
            throw new UnsupportedOperationException(transform2.getClass().getName() + " is not an ExportableTransform.");
        }

        sb.append(
            "\ttransform : function(src) {\n" +
            "\t\tsrc = this._sub1.transform(src);\n" +
            "\t\tsrc = this._sub2.transform(src);\n" +
            "\t\treturn src;\n" +
            "\t}\n");

        sb.append("}");
        return sb.toString();
    }

    /**
     * Gets the derivative of this transform at a point.
     *
     * @param  point  the position where to evaluate the derivative.
     * @return the derivative at the specified point (never {@code null}).
     * @throws TransformException if the derivative cannot be evaluated at the specified point.
     */
    @Override
    public Matrix derivative(final DirectPosition point) throws TransformException {
        final Matrix matrix1 = transform1.derivative(point);
        final Matrix matrix2 = transform2.derivative(transform1.transform(point, null));
        return Matrices.multiply(matrix2, matrix1);
    }

    /**
     * Creates the inverse transform of this object.
     */
    @Override
    public synchronized MathTransform inverse() throws NoninvertibleTransformException {
        assert isValid();
        if (inverse == null) try {
            inverse = create(transform2.inverse(), transform1.inverse(), null);
            setInverse(inverse, this);
        } catch (FactoryException e) {
            throw new NoninvertibleTransformException(Resources.format(Resources.Keys.NonInvertibleTransform), e);
        }
        return inverse;
    }

    /**
     * If the given transform is an instance of {@code ConcatenatedTransform}, sets its inverse to the given value.
     * Otherwise does nothing.
     *
     * @param  tr       the transform on which to set the inverse.
     * @param  inverse  the inverse to assign to the given transform.
     */
    static void setInverse(final MathTransform tr, final MathTransform inverse) {
        if (tr instanceof ConcatenatedTransform) {
            assert OnewayLinearTransform.isNullOrDelegate(((ConcatenatedTransform) tr).inverse, inverse);
            ((ConcatenatedTransform) tr).inverse = inverse;
        }
    }

    /**
     * Returns the intersection of domains declared in transform steps.
     * The result is in the units of input coordinates.
     *
     * <p>This method shall not be invoked recursively; the result would be in wrong units.
     * The {@code estimateOnInverse(…)} method implementations performs {@code instanceof}
     * checks for preventing that.</p>
     *
     * @param  criteria  domain builder passed to each transform steps.
     */
    @Override
    public final Optional<Envelope> getDomain(final DomainDefinition criteria) throws TransformException {
        final MathTransform head = transform1.inverse();            // == inverse().transform2
        criteria.estimateOnInverse(transform2.inverse(), head);
        criteria.estimateOnInverse(head);
        return criteria.result();
    }

    /**
     * Concatenates or pre-concatenates in an optimized way this transform with the given transform, if possible.
     * This method tries to delegate the concatenation to {@link #transform1} or {@link #transform2}.
     * Assuming that transforms are associative, this is equivalent to trying the following arrangements:
     *
     * <pre class="text">
     *   Instead of : other → tr1 → tr2
     *   Try:         (other → tr1) → tr2          where (…) denote an optimized concatenation.
     *
     *   Instead of : tr1 → tr2 → other
     *   Try:         tr1 → (tr2 → other)          where (…) denote an optimized concatenation.</pre>
     *
     * @throws FactoryException if an error occurred while combining the transforms.
     */
    @Override
    protected void tryConcatenate(final Joiner context) throws FactoryException {
        int relativeIndex = +1;
        do {
            MathTransform step1 = transform2;     // In the first iteration, we will try to concatenate after this.
            MathTransform step2 = context.getTransform(relativeIndex).orElse(null);
            if (step2 != null) {
                if (relativeIndex < 0) {
                    step1 = step2;
                    step2 = transform1;           // In the second iteration, we will try to concatenate before this.
                }
                step2 = tryOptimized(step1, step2, context.factory);
                if (step2 != null) {
                    step1 = transform1;           // In the first iteration, keep this and replace after.
                    if (relativeIndex < 0) {
                        step1 = step2;
                        step2 = transform2;       // In the second iteration, keep this and replace before.
                    }
                    context.replace(relativeIndex, create(step1, step2, context.factory));
                    return;
                }
            }
        } while ((relativeIndex = -relativeIndex) < 0);
        /*
         * Do not invoke `super.tryConcatenate(context)`. The test of whether `this` is the
         * inverse of `other` has been done indirectly by the calls to `tryOptimized(…)`.
         */
    }

    /**
     * Tests whether this transform does not move any points.
     * Implementation checks if the two transforms are identity.
     *
     * <h4>Implementation note</h4>
     * This method should always returns {@code false}, because
     * {@code create(…)} should have created specialized implementations for identity cases.
     * Nevertheless we perform the full check as a safety, in case someone instantiated this
     * class directly instead of using a factory method, or in case the given math transforms
     * are mutable (they should not, be we cannot control what the user gave to us).
     */
    @Override
    public boolean isIdentity() {
        return transform1.isIdentity() && transform2.isIdentity();
    }

    /**
     * @hidden because nothing new to said.
     */
    @Override
    protected int computeHashCode() {
        return super.computeHashCode() ^ getSteps().hashCode();
    }

    /**
     * Compares the specified object with this math transform for equality.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {                                                   // Slight optimization
            return true;
        }
        /*
         * Do not invoke super.equals(...) because we don't want to compare descriptors.
         * Their computation may be expensive and this information is derived from the
         * transform steps anyway.
         */
        if (object instanceof ConcatenatedTransform) {
            final var that = (ConcatenatedTransform) object;
            return Utilities.deepEquals(this.getSteps(), that.getSteps(), mode);
        }
        return false;
    }

    /**
     * Formats the inner part of a <i>Well Known Text</i> version 1 (WKT 1) element.
     *
     * <h4>Compatibility note</h4>
     * {@code Concat_MT} is defined in the WKT 1 specification only.
     *
     * @param  formatter  the formatter to use.
     * @return the WKT element name, which is {@code "Concat_MT"}.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        final List<? super MathTransform> transforms;
        if (formatter.getConvention() == Convention.INTERNAL) {
            transforms = getSteps();
        } else {
            transforms = getPseudoSteps();
        }
        /*
         * Now formats the list that we got. Note that as a result of the above
         * pre-processing the list may have been reduced to a singleton, in which
         * case this is no longer a CONCAT_MT.
         */
        if (transforms.size() == 1) {
            return formatter.delegateTo(transforms.get(0));
        }
        for (final Object step : transforms) {
            formatter.newLine();
            if (step instanceof FormattableObject) {
                formatter.append((FormattableObject) step);         // May not implement MathTransform.
            } else if (step instanceof MathTransform) {
                formatter.append((MathTransform) step);
            } else {
                /*
                 * Matrices may happen in a chain of pseudo-steps. For now wrap in a MathTransform.
                 * We could define a Formatter.append(Matrix) method instead, but it is probably not
                 * worth the cost.
                 */
                formatter.append(MathTransforms.linear((Matrix) step));
            }
        }
        return WKTKeywords.Concat_MT;
    }
}
