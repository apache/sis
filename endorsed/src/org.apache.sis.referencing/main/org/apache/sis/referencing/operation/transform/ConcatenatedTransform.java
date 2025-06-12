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
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.system.Semaphores;
import org.apache.sis.util.Classes;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.privy.Strings;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.util.resources.Errors;

// Specific to the geoapi-4.0 branch:
import org.opengis.coordinate.MismatchedDimensionException;


/**
 * Base class for concatenated transforms. Instances can be created by calls to the
 * {@link #create(MathTransformFactory, MathTransform...)} method.
 * When possible, the above-cited method concatenates {@linkplain ProjectiveTransform projective transforms}
 * before to fallback on the creation of new {@code ConcatenatedTransform} instances.
 *
 * <p>Concatenated transforms are serializable if all their step transforms are serializable.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 *
 * @see MathTransformFactory#createConcatenatedTransform(MathTransform, MathTransform)
 */
class ConcatenatedTransform extends AbstractMathTransform implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 5772066656987558634L;

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
            throw new IllegalArgumentException(Resources.format(
                    Resources.Keys.CanNotConcatenateTransforms_2,
                    getName(transform1), getName(transform2)));
        }
    }

    /**
     * Returns a name for the specified math transform.
     * This is used for formatting the message in exceptions.
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
     * Replaces the last element of the given list by the first or last step of the transform chains.
     * If the last element of the {@code steps} list is an instance of {@link ConcatenatedTransform},
     * then this method replaces that element by {@link #transform1} if {@code first} is {@code true},
     * or by {@link #transform2} if {@code first} is {@code false}. The other step of this transform
     * is added without being expanded.
     *
     * <h4>Rational</h4>
     * We do not expand all steps because this is usually not needed. If the steps in the middle
     * have not be optimized previously, there is no reason to try to optimize them again since
     * the neighbor transforms have not yet changed.
     *
     * @param  steps  the list to modify.
     * @param  pairs  the list where to opportunistically add {@link ConcatenatedTransform} instances.
     * @param  first  whether to expand {@link #transform1} ({@code true}) or {@link #transform2} ({@code false})
     */
    private static void expand(final List<MathTransform> steps,
                               final List<ConcatenatedTransform> pairs,
                               final boolean first)
    {
        int replaceIndex = steps.size() - 1;
        MathTransform tr = steps.get(replaceIndex);
        while (tr instanceof ConcatenatedTransform) {
            final var ct = (ConcatenatedTransform) tr;
            pairs.add(ct);
            steps.set(replaceIndex,   ct.transform1);
            steps.add(replaceIndex+1, ct.transform2);
            if (first) {
                tr = ct.transform1;
            } else {
                tr = ct.transform2;
                replaceIndex++;
            }
        }
    }

    /**
     * Concatenates the given transforms.
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
     * @param  factory     the factory which is (indirectly) invoking this method.
     * @param  transforms  the transform steps to concatenate.
     * @return the concatenated transform.
     *
     * @see MathTransforms#concatenate(MathTransform, MathTransform)
     */
    public static MathTransform create(final MathTransformFactory factory, MathTransform... transforms)
            throws FactoryException, MismatchedDimensionException
    {
        /*
         * Check arguments. Should be done first, before to exclude identity transforms,
         * because even the identity transforms shall have compatible number of dimensions.
         */
        for (int i=1; i<transforms.length; i++) {
            final MathTransform tr1 = transforms[i-1];
            final MathTransform tr2 = transforms[i  ];
            final int dim1 = tr1.getTargetDimensions();
            final int dim2 = tr2.getSourceDimensions();
            if (dim1 != dim2) {
                String message = Resources.format(Resources.Keys.CanNotConcatenateTransforms_2, getName(tr1), getName(tr2));
                throw new MismatchedDimensionException(message + ' ' + Errors.format(Errors.Keys.MismatchedDimension_2, dim1, dim2));
            }
        }
        /*
         * Create a semi-shallow (or semi-deep) list of steps by expanding only the `ConcatenatedTransform` instances
         * that are located at the junction between two elements of the given `transforms` array. For each junction
         * at index `i`, the last step of `transforms[i]` and the first step of `transforms[i+1]` are unwrapped.
         * It may force the unwrapping of some other steps in order to not lose any of them.
         * Other instances of `ConcatenatedTransform` are added as-is in the `steps` list.
         */
        final var steps   = new ArrayList<MathTransform>();
        final var pairs   = new ArrayList<ConcatenatedTransform>();
        final var context = new TransformJoiner(steps, factory, pairs);
        boolean changed;
        do {
            for (final MathTransform tr : transforms) {
                if (!tr.isIdentity()) {
                    if (steps.isEmpty()) {
                        steps.add(tr);
                    } else {
                        expand(steps, pairs, false);    // Expand `transform2` of the previous step.
                        steps.add(tr);
                        expand(steps, pairs, true);     // Expand `transform1` of this step.
                    }
                }
            }
            /*
             * Thank to above expansion, we can now see when there is two consecutive linear transforms or when a
             * transform is immediately followed by its inverse. The non-expanded `ConcatenatedTransform` instances
             * were already analyzed in a previous call of this method and do not need to be analyzed again.
             */
            changed = context.simplify();
            switch (steps.size()) {
                case 0: return MathTransforms.identity(transforms[0].getSourceDimensions());
                case 1: return steps.get(0);
            }
            if (changed) {
                transforms = steps.toArray(MathTransform[]::new);
                steps.clear();
                pairs.clear();
            }
        } while (changed);
        /*
         * Give to `AbstractMathTransform` a chance to return an optimized concatenation.
         * If an optimization is provided, it replaces the whole `steps` chain. Examples:
         *
         *   - If a `LogarithmicTransform` is concatenated with an `ExponentialTransform`,
         *     the optimization can produce a new formula implemented by a different class.
         *   - `PassThrouthTransform` may concatenate its sub-transform.
         *   - etc.
         */
        MathTransform concatenated = null;
        for (int i=steps.size(); --i >= 0;) {
            final MathTransform tr = steps.get(i);
            if (tr instanceof AbstractMathTransform) {
                context.reset(i);
                ((AbstractMathTransform) tr).tryConcatenate(context);
                final MathTransform replacement = context.replacement;
                if (replacement != null) {
                    if (concatenated == null || getStepCount(replacement) < getStepCount(concatenated)) {
                        concatenated = replacement;
                    }
                }
            }
        }
        /*
         * If no optimization was found, we cannot avoid the creation of a `ConcatenatedTransform` object.
         * Check for the type to create (1D, 2D, or general case) and whether an efficient "direct" variant
         * can be used.
         */
        if (concatenated == null) {
            context.reassemble();
            concatenated = steps.get(0);
            final int count = steps.size();
            for (int i=1; i<count; i++) {
                final MathTransform tr2 = steps.get(i);
                final int dimSource = concatenated.getSourceDimensions();
                final int dimTarget = tr2.getTargetDimensions();
                if (dimSource == AbstractMathTransform1D.DIMENSION &&
                    dimTarget == AbstractMathTransform1D.DIMENSION)
                {
                    if (concatenated instanceof MathTransform1D && tr2 instanceof MathTransform1D) {
                        concatenated = new ConcatenatedTransformDirect1D((MathTransform1D) concatenated, (MathTransform1D) tr2);
                    } else {
                        concatenated = new ConcatenatedTransform1D(concatenated, tr2);
                    }
                } else if (dimSource == AbstractMathTransform2D.DIMENSION &&
                           dimTarget == AbstractMathTransform2D.DIMENSION)
                {
                    if (concatenated instanceof MathTransform2D && tr2 instanceof MathTransform2D) {
                        concatenated = new ConcatenatedTransformDirect2D((MathTransform2D) concatenated, (MathTransform2D) tr2);
                    } else {
                        concatenated = new ConcatenatedTransform2D(concatenated, tr2);
                    }
                } else if (dimSource == concatenated.getTargetDimensions() &&
                           dimTarget == tr2.getSourceDimensions())
                {
                    concatenated = new ConcatenatedTransformDirect(concatenated, tr2);
                } else {
                    concatenated = new ConcatenatedTransform(concatenated, tr2);
                }
            }
        }
        assert isValid(MathTransforms.getSteps(concatenated)) : concatenated;
        return concatenated;
    }

    /**
     * Verifies that the given list does not contain two consecutive linear transforms.
     * Consecutive linear transforms are considered an error because their matrices should
     * have been multiplied together. Also checks that the source dimension of each step
     * is equal to the target dimension of the previous step.
     * This method is used for assertion purposes only.
     */
    private static boolean isValid(final List<MathTransform> steps) {
        boolean wasLinear = false;
        MathTransform previous = null;
        for (final MathTransform step : steps) {
            if (previous != null) {
                if (previous.getTargetDimensions() != step.getSourceDimensions()) {
                    return false;
                }
            }
            if (step instanceof LinearTransform) {
                if (wasLinear) {
                    return false;
                }
                wasLinear = true;
            } else {
                wasLinear = false;
            }
            previous = step;
        }
        return true;
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
        addStepsTo(transforms);
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
        addStepsTo(transforms);
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
     * @param  transforms  the list where to add the transform steps.
     */
    private void addStepsTo(final List<? super MathTransform> transforms) {
        if (transform1 instanceof ConcatenatedTransform) {
            ((ConcatenatedTransform) transform1).addStepsTo(transforms);
        } else {
            transforms.add(transform1);
        }
        if (transform2 instanceof ConcatenatedTransform) {
            ((ConcatenatedTransform) transform2).addStepsTo(transforms);
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
            inverse = create(DefaultMathTransformFactory.provider(), transform2.inverse(), transform1.inverse());
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
