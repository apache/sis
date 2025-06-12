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

import java.util.Arrays;
import java.util.Objects;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.privy.ReferencingUtilities;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.privy.Strings;
import org.apache.sis.util.resources.Errors;


/**
 * Extracts a sub-transform from a given {@code MathTransform} and source or target dimension indices.
 * Given an arbitrary {@link MathTransform}, this class tries to return a new math transform that operates
 * only on a given set of source or target dimensions.
 *
 * <h2>Example</h2>
 * If the supplied {@code transform} has (<var>x</var>,<var>y</var>,<var>z</var>) inputs
 * and (<var>λ</var>,<var>φ</var>,<var>h</var>) outputs, then the following code:
 *
 * {@snippet lang="java" :
 *     TransformSeparator s = new TransformSeparator(theTransform);
 *     s.addSourceDimensionRange(0, 2);
 *     MathTransform mt = s.separate();
 *     }
 *
 * will return a transform with (<var>x</var>,<var>y</var>) inputs and (probably) (<var>λ</var>,<var>φ</var>) outputs.
 * The output dimensions can be verified with a call to {@link #getTargetDimensions()}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   0.7
 */
public class TransformSeparator {
    /**
     * The transform to separate.
     */
    protected final MathTransform transform;

    /**
     * Indices of transform input dimensions to keep, or {@code null} if not yet defined.
     * If non-null, the indices in the array must be sorted in strictly increasing order.
     * This sequence can contain any integers in the range 0 inclusive to
     * {@link MathTransform#getSourceDimensions()} exclusive.
     *
     * <p>Values in this array should never be modified. For adding, removing or editing indices,
     * new arrays should be created and assigned to this field. This approach makes easier to keep snapshots
     * of indices arrays at various stages during the process of separating a {@code MathTransform}.</p>
     *
     * @see #getSourceDimensions()
     * @see #addSourceDimensions(int...)
     * @see #addSourceDimensionRange(int, int)
     */
    protected int[] sourceDimensions;

    /**
     * Indices of transform output dimensions to keep, or {@code null} if not yet defined.
     * If non-null, the indices in the array must be sorted in strictly increasing order.
     * This sequence can contain any integers in the range 0 inclusive to
     * {@link MathTransform#getTargetDimensions()} exclusive.
     *
     * <p>Values in this array should never be modified. For adding, removing or editing indices,
     * new arrays should be created and assigned to this field. This approach makes easier to keep snapshots
     * of indices arrays at various stages during the process of separating a {@code MathTransform}.</p>
     *
     * @see #getTargetDimensions()
     * @see #addTargetDimensions(int...)
     * @see #addTargetDimensionRange(int, int)
     */
    protected int[] targetDimensions;

    /**
     * The factory to use for creating new math transforms.
     */
    final MathTransformFactory factory;

    /**
     * Whether {@link #separate()} is allowed to add new dimensions in {@link #sourceDimensions}
     * if this is required for computing all values specified in {@link #targetDimensions}.
     *
     * @see #isSourceExpandable()
     * @see #setSourceExpandable(boolean)
     */
    private boolean isSourceExpandable;

    /**
     * Constructs a separator for the given transform.
     *
     * @param transform  the transform to separate.
     */
    public TransformSeparator(final MathTransform transform) {
        this(transform, null);
    }

    /**
     * Constructs a separator for the given transform and using the given factory.
     *
     * @param transform  the transform to separate.
     * @param factory    the factory to use for creating new math transforms, or {@code null} if none.
     */
    public TransformSeparator(final MathTransform transform, final MathTransformFactory factory) {
        this.transform = Objects.requireNonNull(transform);
        this.factory   = ReferencingUtilities.nonNull(factory);
    }

    /**
     * Resets this transform separator in the same state as after construction. This method clears any
     * {@linkplain #getSourceDimensions() source} and {@linkplain #getTargetDimensions() target dimensions}
     * settings and disables {@linkplain #isSourceExpandable() source expansion}.
     * This method can be invoked when the same {@code MathTransform} needs to be separated in more than one part,
     * for example an horizontal and a vertical component.
     */
    public void clear() {
        sourceDimensions   = null;
        targetDimensions   = null;
        isSourceExpandable = false;
    }

    /**
     * Inserts the specified {@code dimension} in the specified sequence at a position that preserve increasing order.
     * This method does nothing if the given dimension already exists in the given array.
     *
     * <h4>API note</h4>
     * We do not provide public API for this method because we rather encourage bulk operations (adding many values
     * at once), and because the public API does not allow to specify values in random order (for avoiding risk of
     * confusion as some users could expect the separated transform to use the dimensions in the order he specified
     * them).
     *
     * @param sequence   the {@link #sourceDimensions} or {@link #targetDimensions} sequence to update.
     * @param dimension  the value to add to the given sequence.
     */
    private static int[] insert(int[] sequence, int dimension) throws IllegalArgumentException {
        if (sequence == null) {
            return new int[] {dimension};
        }
        assert ArraysExt.isSorted(sequence, true);
        int i = Arrays.binarySearch(sequence, dimension);
        if (i < 0) {
            i = ~i;                                                 // Tild, not the minus sign.
            sequence = ArraysExt.insert(sequence, i, 1);
            sequence[i] = dimension;
        }
        assert Arrays.binarySearch(sequence, dimension) == i;
        return sequence;
    }

    /**
     * Adds the specified {@code dimensions} to the specified sequence.
     * Values must be given in strictly increasing order (this will be verified by this method).
     *
     * @param  sequence    the {@link #sourceDimensions} or {@link #targetDimensions} sequence to update.
     * @param  dimensions  the user supplied dimensions to add to the given sequence.
     * @param  max         the maximal value allowed, exclusive.
     * @throws IllegalArgumentException if a {@code dimensions} value does not met the conditions.
     */
    private static int[] add(int[] sequence, final int[] dimensions, final int max) throws IllegalArgumentException {
        ArgumentChecks.ensureNonNull("dimensions", dimensions);
        int offset = 0;
        int previous = -1;                          // This initial value will ensure that we have no negative value.
        if (sequence != null && (offset = sequence.length) != 0) {
            previous = sequence[offset - 1];
            sequence = Arrays.copyOf(sequence, offset + dimensions.length);
            System.arraycopy(dimensions, 0, sequence, offset, dimensions.length);
        } else {
            sequence = dimensions.clone();
        }
        /*
         * Ensure that the specified array contains strictly increasing non-negative values.
         * We verify after the copy as a paranoiac safety against concurrent changes.
         */
        for (int i=offset; i<sequence.length; i++) {
            String message = validate("dimensions", i - offset, previous, max, previous = sequence[i]);
            if (message != null) throw new IllegalArgumentException(message);
        }
        return sequence;
    }

    /**
     * If the given value is out of bounds, returns the error message for the exception to throw.
     * This is used during validation of an array expected to be in strictly increasing order.
     *
     * @param  name      name of the argument.
     * @param  i         index of the array element in the argument.
     * @param  previous  the value during previous iteration.
     * @param  max       the maximal value, exclusive.
     * @param  value     the value to validate.
     * @return {@code null} if the value is valid, otherwise the message to put in an exception.
     */
    static String validate(final String name, final int i, final int previous, final int max, final int value) {
        if (value <= previous || value >= max) {
            return Errors.format(Errors.Keys.ValueOutOfRange_4,
                    Strings.toIndexed("dimensions", i), previous + 1, max - 1, value);
        }
        return null;
    }

    /**
     * Adds the specified range to the specified sequence.
     *
     * @param  sequence  the {@link #sourceDimensions} or {@link #targetDimensions} sequence to update.
     * @param  lower     the lower value of the range to add, inclusive.
     * @param  upper     the upper value of the range to add, exclusive.
     * @param  max       the maximal value allowed, exclusive.
     * @throws IllegalArgumentException if the {@code lower} or {@code upper} value does not met the conditions.
     */
    private static int[] add(int[] sequence, final int lower, final int upper, final int max) throws IllegalArgumentException {
        if (lower < 0 || lower > upper) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalRange_2, lower, upper));
        }
        int min    = 0;  // Inclusive
        int offset = 0;
        if (sequence != null && (offset = sequence.length) != 0) {
            min = sequence[offset - 1] + 1;
        }
        final boolean isOutOfRange = (upper > max);
        if (isOutOfRange || lower < min) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.ValueOutOfRange_4,
                    isOutOfRange ? "upper" : "lower", min, max-1, isOutOfRange ? upper : lower));
        }
        if (offset == 0) {
            sequence = ArraysExt.range(lower, upper);
        } else {
            sequence = Arrays.copyOf(sequence, (offset -= lower) + upper);
            for (int i=lower; i<upper; i++) {
                sequence[offset + i] = i;
            }
        }
        assert containsAll(sequence, lower, upper);
        return sequence;
    }

    /**
     * Adds input dimensions to keep in the separated transform.
     * The given values are <em>source</em> dimension indices of the transform given to the constructor.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>All numbers shall be in the range 0 inclusive to {@link MathTransform#getSourceDimensions()} exclusive.</li>
     *   <li>The {@code dimensions} values shall be in strictly increasing order.</li>
     *   <li>The {@code dimensions} values shall be greater than all values specified by all previous calls
     *       of this method since construction or since the last call to {@link #clear()}.</li>
     * </ul>
     *
     * @param  dimensions  a sequence of source dimensions to keep, in strictly increasing order.
     * @throws IllegalArgumentException if {@code dimensions} contains negative values
     *         or if values are not in a strictly increasing order.
     */
    public void addSourceDimensions(final int... dimensions) throws IllegalArgumentException {
        sourceDimensions = add(sourceDimensions, dimensions, transform.getSourceDimensions());
    }

    /**
     * Adds a range of input dimensions to keep in the separated transform.
     * The {@code lower} and {@code upper} values define a range of  <em>source</em> dimension indices
     * of the transform given to the constructor.
     *
     * @param  lower  the lower dimension, inclusive. Shall not be smaller than 0.
     * @param  upper  the upper dimension, exclusive. Shall be smaller than {@link MathTransform#getSourceDimensions()}.
     * @throws IllegalArgumentException if {@code lower} or {@code upper} are out of bounds.
     */
    public void addSourceDimensionRange(final int lower, final int upper) throws IllegalArgumentException {
        sourceDimensions = add(sourceDimensions, lower, upper, transform.getSourceDimensions());
    }

    /**
     * Returns the input dimensions to keep or kept in the separated transform.
     * This method performs the first applicable action in the following list:
     *
     * <ol class="verbose">
     *   <li>Source dimensions have been explicitly set by at least one call to {@link #addSourceDimensions(int...)}
     *       or {@link #addSourceDimensionRange(int, int)} since construction or since last call to {@link #clear()}.
     *       In such case, this method returns all specified source dimensions.</li>
     *
     *   <li>No source dimensions were set but {@link #separate()} has been invoked.
     *       In such case, this method returns the sequence of source dimensions that {@code separate()} chooses to retain.
     *       It may be all source dimensions of the transform given at construction time, but not necessarily.</li>
     *
     *   <li>Otherwise an exception is thrown.</li>
     * </ol>
     *
     * If source dimensions have not been set explicitly, {@code TransformSeparator} tries to reduce
     * the set of source dimensions to the smallest set required for computing the target dimensions.
     *
     * @return the input dimension as a sequence of strictly increasing values.
     * @throws IllegalStateException if input dimensions have not been set and
     *         {@link #separate()} has not yet been invoked.
     */
    public int[] getSourceDimensions() throws IllegalStateException {
        if (sourceDimensions != null) {
            return sourceDimensions.clone();
        }
        throw new IllegalStateException(Resources.format(Resources.Keys.UnspecifiedDimensions));
    }

    /**
     * Adds output dimensions to keep in the separated transform.
     * The given values are <em>target</em> dimension indices of the transform given to the constructor.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>All numbers shall be in the range 0 inclusive to {@link MathTransform#getTargetDimensions()} exclusive.</li>
     *   <li>The {@code dimensions} values shall be in strictly increasing order.</li>
     *   <li>The {@code dimensions} values shall be greater than all values specified by all previous calls
     *       of this method since construction or since the last call to {@link #clear()}.</li>
     * </ul>
     *
     * @param  dimensions  a sequence of target dimensions to keep, in strictly increasing order.
     * @throws IllegalArgumentException if {@code dimensions} contains negative values
     *         or if values are not in a strictly increasing order.
     */
    public void addTargetDimensions(final int... dimensions) throws IllegalArgumentException {
        targetDimensions = add(targetDimensions, dimensions, transform.getTargetDimensions());
    }

    /**
     * Adds a range of output dimensions to keep in the separated transform.
     * The {@code lower} and {@code upper} values define a range of <em>target</em> dimension indices
     * of the transform given to the constructor.
     *
     * @param  lower  the lower dimension, inclusive. Shall not be smaller than 0.
     * @param  upper  the upper dimension, exclusive. Shall be smaller than {@link MathTransform#getTargetDimensions()}.
     * @throws IllegalArgumentException if {@code lower} or {@code upper} are out of bounds.
     */
    public void addTargetDimensionRange(final int lower, final int upper) throws IllegalArgumentException {
        targetDimensions = add(targetDimensions, lower, upper, transform.getTargetDimensions());
    }

    /**
     * Returns the output dimensions to keep or kept in the separated transform.
     * This method performs the first applicable action in the following list:
     *
     * <ol class="verbose">
     *   <li>Target dimensions have been explicitly set by at least one call to {@link #addTargetDimensions(int...)}
     *       or {@link #addTargetDimensionRange(int, int)} since construction or since last call to {@link #clear()}.
     *       In such case, this method returns all specified target dimensions.</li>
     *
     *   <li>No target dimensions were set but {@link #separate()} has been invoked.
     *       In such case, the target dimensions are inferred automatically from the {@linkplain #getSourceDimensions()
     *       source dimensions} and the transform.</li>
     *
     *   <li>Otherwise an exception is thrown.</li>
     * </ol>
     *
     * @return the output dimension as a sequence of strictly increasing values.
     * @throws IllegalStateException if output dimensions have not been set and
     *         {@link #separate()} has not yet been invoked.
     */
    public int[] getTargetDimensions() throws IllegalStateException {
        if (targetDimensions != null) {
            return targetDimensions.clone();
        }
        throw new IllegalStateException(Resources.format(Resources.Keys.UnspecifiedDimensions));
    }

    /**
     * Returns whether {@code separate()} is allowed to expand the list of source dimensions.
     * The default value is {@code false}, which means that {@link #separate()} either returns
     * a {@link MathTransform} having exactly the requested {@linkplain #getSourceDimensions()
     * source dimensions}, or throws a {@link FactoryException}.
     *
     * @return whether {@code separate()} is allowed to add new source dimensions
     *         instead of throwing a {@link FactoryException}.
     *
     * @since 1.1
     */
    public boolean isSourceExpandable() {
        return isSourceExpandable;
    }

    /**
     * Sets whether {@code separate()} is allowed to expand the list of source dimensions.
     * The default value is {@code false}, which means that {@code separate()} will throw a {@link FactoryException}
     * if some {@linkplain #getTargetDimensions() target dimensions} cannot be computed without inputs that are not
     * in the list of {@linkplain #getSourceDimensions() source dimensions}. If this flag is set to {@code true},
     * then {@link #separate()} will be allowed to augment the list of source dimensions with any inputs that are
     * essential for producing all requested outputs.
     *
     * @param  enabled  whether to allow source dimensions expansion.
     *
     * @since 1.1
     */
    public void setSourceExpandable(final boolean enabled) {
        isSourceExpandable = enabled;
    }

    /**
     * Separates the math transform specified at construction time for given dimension indices.
     * This method creates a math transform that use only the {@linkplain #addSourceDimensions(int...) specified
     * source dimensions} and return only the {@linkplain #addTargetDimensions(int...) specified target dimensions}.
     * If the source or target dimensions were not specified, then they will be inferred as below:
     *
     * <ul class="verbose">
     *   <li>If source dimensions were unspecified, then the returned transform will keep only the source dimensions
     *       needed for computing the specified target dimensions. If all source dimensions need to be kept,
     *       then they should be {@linkplain #addSourceDimensionRange(int, int) specified explicitly}.</li>
     *
     *   <li>If target dimensions were unspecified, then the returned transform will expect only the specified
     *       source dimensions as inputs, and the target dimensions will be inferred automatically.</li>
     *
     *   <li>If neither source and target positions were specified, then the returned transform will have the same
     *       set of target dimensions, but only the set of source dimensions required for computing those targets.
     *       In other words, this method drops unused source dimensions.</li>
     * </ul>
     *
     * The source and target dimensions actually used can be queried by calls to {@link #getSourceDimensions()}
     * or {@link #getTargetDimensions()} after this {@code separate()} method.
     *
     * @return the separated math transform.
     * @throws FactoryException if the transform cannot be separated.
     */
    public MathTransform separate() throws FactoryException {
        MathTransform tr = transform;
        final int[] specifiedSources = sourceDimensions;
        if (isSourceExpandable) {
            sourceDimensions = null;                        // Take all sources for now, will filter later.
        }
        if (sourceDimensions == null || containsAll(sourceDimensions, 0, tr.getSourceDimensions())) {
            if (targetDimensions != null && !containsAll(targetDimensions, 0, tr.getTargetDimensions())) {
                tr = filterTargetDimensions(tr, targetDimensions);
            }
            if (sourceDimensions == null) {
                sourceDimensions = ArraysExt.range(0, transform.getSourceDimensions());
            }
            if (targetDimensions == null) {
                targetDimensions = ArraysExt.range(0, transform.getTargetDimensions());
            }
        } else {
            /*
             * At this point there is at least one source dimension to take in account.
             * Source dimensions are more difficult to process than target dimensions.
             */
            final int[] requested = targetDimensions;
            tr = filterSourceDimensions(tr, sourceDimensions);            // May update targetDimensions.
            assert ArraysExt.isSorted(targetDimensions, true);
            if (requested != null) {
                final int[] inferred = targetDimensions;
                targetDimensions = requested;
                final int[] subDimensions = new int[requested.length];
                for (int i=0; i<requested.length; i++) {
                    final int r = requested[i];
                    final int j = Arrays.binarySearch(inferred, r);
                    if (j < 0) {
                        /*
                         * The user asked for some target dimensions that we cannot keep, probably
                         * because at least one of the requested target dimensions has a dependency to a
                         * source dimension that does not appear in the list of source dimensions to keep.
                         */
                        throw new FactoryException(Resources.format(Resources.Keys.CanNotSeparateTargetDimension_1, r));
                    }
                    subDimensions[i] = j;
                }
                tr = filterTargetDimensions(tr, subDimensions);
            }
        }
        /*
         * We are done for the separation based on specified dimensions. Do a final verification on the number of dimensions.
         * Then, if the user asked the minimal set of source dimensions, verify if we can remove some of those dimensions.
         */
        int side     = 0;
        int expected = sourceDimensions.length;
        int actual   = tr.getSourceDimensions();
        if (actual == expected) {
            side     = 1;
            expected = targetDimensions.length;
            actual   = tr.getTargetDimensions();
            if (actual == expected) {
                if (specifiedSources == null || isSourceExpandable) {
                    tr = removeUnusedSourceDimensions(tr, specifiedSources);
                }
                return tr;
            }
        }
        throw new FactoryException(Resources.format(Resources.Keys.CanNotSeparateTransform_3, side, expected, actual));
    }

    /**
     * Creates a transform for the same mathematic as the given {@code step}
     * but expecting only the given dimensions as inputs.
     * This method is invoked by {@link #separate()} when user-specified source dimensions need to be taken in account.
     * The given {@code step} and {@code dimensions} are typically the values of
     * {@link #transform} and {@link #sourceDimensions} fields respectively, but not necessarily.
     * In particular those arguments will differ when this method is invoked recursively for processing
     * concatenated or {@linkplain PassThroughTransform#getSubTransform() sub-transforms}.
     *
     * <p>Subclasses can override this method if they need to handle some {@code MathTransform} implementations
     * in a special way. However, all implementations of this method shall obey to the following contract:</p>
     * <ul class="verbose">
     *   <li>{@link #sourceDimensions} and {@link #targetDimensions} should not be assumed accurate
     *       since they may be temporarily outdated or modified during recursive calls to this method.</li>
     *   <li>{@link #sourceDimensions} should not be modified by this method.</li>
     *   <li>{@link #targetDimensions} <strong>must</strong> be <em>overwritten</em> (not updated) by this method to the
     *       sequence of all target dimensions of {@code step} that are also target dimensions of the returned transform.
     *       The indices shall be in strictly increasing order from 0 inclusive to
     *       {@code step.getTargetDimensions()} exclusive.</li>
     * </ul>
     *
     * @param  step        the transform for which to retain only a subset of the source dimensions.
     * @param  dimensions  indices of the source dimensions of {@code step} to retain.
     * @return a transform expecting only the given source dimensions.
     * @throws FactoryException if the given transform is not separable.
     */
    @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
    protected MathTransform filterSourceDimensions(final MathTransform step, final int[] dimensions) throws FactoryException {
        if (dimensions.length == 0) {
            return IdentityTransform.create(0);
        }
        final int numSrc = step.getSourceDimensions();
        final int numTgt = step.getTargetDimensions();
        final int lower  = dimensions[0];
        final int upper  = dimensions[dimensions.length - 1] + 1;
        if (lower == 0 && upper == numSrc && dimensions.length == numSrc) {
            targetDimensions = ArraysExt.range(0, numTgt);
            return step;
        }
        if (step.isIdentity()) {
            targetDimensions = dimensions;
            return IdentityTransform.create(dimensions.length);
        }
        if (step instanceof ConcatenatedTransform) {
            final var ctr = (ConcatenatedTransform) step;
            final MathTransform step1 = filterSourceDimensions(ctr.transform1, dimensions);
            final MathTransform step2 = filterSourceDimensions(ctr.transform2, targetDimensions);
            return factory.createConcatenatedTransform(step1, step2);
            // Keep the `targetDimensions` computed by the last step.
        }
        /*
         * Special case for the passthrough transform: if at least one input dimension belong to the pass-
         * through sub-transform, then invoke this method recursively for the sub-transform dimensions.
         */
        if (step instanceof PassThroughTransform) {
            final var passThrough = (PassThroughTransform) step;
            final int numSubSrc = passThrough.subTransform.getSourceDimensions();
            final int numNewDim = passThrough.subTransform.getTargetDimensions() - numSubSrc;
            final int subLower  = passThrough.firstAffectedCoordinate;
            final int subUpper  = subLower + numSubSrc;
            int[] subDimensions = new int[dimensions.length];
            targetDimensions    = null;
            int n = 0;
            for (int dim : dimensions) {
                if (dim >= subLower) {
                    if (dim < subUpper) {
                        // Dimension n belong to the subtransform.
                        subDimensions[n++] = dim - subLower;
                        continue;
                    }
                    dim += numNewDim;
                }
                /*
                 * Dimension n belong to heading or trailing dimensions.
                 * Passthrough, after adjustment for trailing dimensions.
                 */
                targetDimensions = insert(targetDimensions, dim);
            }
            subDimensions = ArraysExt.resize(subDimensions, n);
            /*
             * If no source dimension belong to the sub-transform, then all source dimensions are heading or
             * trailing dimensions. A passthrough transform without its sub-transform is an identity transform.
             */
            if (n == 0) {
                return IdentityTransform.create(dimensions.length);
            }
            /*
             * There is at least one dimension to separate in the sub-transform. Perform this separation and get
             * the list of target dimensions. We need to offset the target dimensions by the number of leading
             * dimensions once the separation is done, in order to translate from the sub-transform's dimension
             * numbering to the transform's numbering.
             */
            int[] target = targetDimensions;
            final MathTransform subTransform = filterSourceDimensions(passThrough.subTransform, subDimensions);
            for (final int dim : targetDimensions) {
                target = insert(target, dim + subLower);
            }
            targetDimensions = target;
            /*
             * If all source dimensions not in the sub-transform are consecutive numbers, we can use our passthrough
             * transform implementation. The "consecutive numbers" requirement (expressed in the `if` statement below)
             * is a consequence of a limitation in our current implementation: our current passthrough transform does
             * not accept arbitrary indices for modified coordinates. We cannot delegate to the static factory method
             * `MathTransforms.passThrough(int[] modifiedCoordinates, ...)` because that method itself relies on this
             * `TransformSeparator` for separating the transform components at non-consecutive indices.
             */
            if (containsAll(dimensions, lower, subLower) && containsAll(dimensions, subUpper, upper)) {
                final int offset = subDimensions[0];
                assert containsAll(subDimensions, offset, offset + subDimensions.length) : offset;
                return factory.createPassThroughTransform(offset + subLower - lower, subTransform, Math.max(0, upper - subUpper));
            }
        }
        /*
         * If the transform is affine (or at least projective), express the transform as a matrix. Then, select
         * target dimensions that depend only on specified source dimensions. If a target dimension depends on
         * at least one discarded source dimension, then that output dimension will be discarded as well.
         */
        final Matrix matrix = MathTransforms.getMatrix(step);
        if (matrix != null) {
            targetDimensions = null;
            int startOfRow = 0;                         // Index of next row to be stored in the `elements` array.
            boolean isLastRowAccepted = false;          // To be set to `true` if we complete successfully up to last row.
            final int numFilteredColumns = (dimensions.length + 1);
            double[] elements = new double[(numTgt + 1) * numFilteredColumns];
reduce:     for (int j=0; j <= numTgt; j++) {
                /*
                 * For each target dimension (i.e. a matrix row), find the matrix elements (excluding translation
                 * terms in the last column) for each source dimension to be kept. If a dependency to at least one
                 * discarded input dimension is found, then the whole output dimension is discarded.
                 */
                int filteredColumn = 0;
                for (int i=0; i<numSrc; i++) {
                    final double element = matrix.getElement(j,i);
                    if (filteredColumn < dimensions.length && dimensions[filteredColumn] == i) {
                        elements[startOfRow + filteredColumn++] = element;
                    } else if (element != 0) {
                        /*
                         * Output dimension `j` depends on one of discarded input dimension `i`.
                         * The whole row will be discarded.
                         */
                        continue reduce;
                    }
                }
                /*
                 * We reach this point only if we determined that for current matrix row, all dependencies are listed
                 * in the array of source dimensions to keep. The matrix coefficients for that row are copied in the
                 * `elements` array.
                 */
                elements[startOfRow + filteredColumn++] = matrix.getElement(j, numSrc);  // Copy the translation term.
                assert filteredColumn == numFilteredColumns : filteredColumn;            // We should have used all values in the `dimensions` array.
                startOfRow += numFilteredColumns;
                /*
                 * In an affine transform, the last row is usually [0 0 0 … 1].
                 * This is not a real dimension, but nevertheless mandatory and
                 * needs to be identified as valid by above code.
                 */
                isLastRowAccepted = (j == numTgt);
                if (!isLastRowAccepted) {                           // Update target dimensions for every rows except the last one.
                    targetDimensions = insert(targetDimensions, j);
                }
            }
            if (isLastRowAccepted) {
                if (targetDimensions == null) {
                    targetDimensions = ArraysExt.EMPTY_INT;
                    return MathTransforms.identity(0);
                }
                elements = ArraysExt.resize(elements, startOfRow);
                return factory.createAffineTransform(Matrices.create(startOfRow / numFilteredColumns, numFilteredColumns, elements));
            }
            /*
             * In an affine transform, the last row is not supposed to have dependency to any source dimension.
             * But if we reach this point, our matrix has such dependencies.
             */
        }
        throw new FactoryException(Resources.format(Resources.Keys.NotAnAffineTransform));
    }

    /**
     * Creates a transform for the same mathematic as the given {@code step}
     * but producing only the given dimensions as outputs.
     * This method is invoked by {@link #separate()} when user-specified target dimensions need to be taken in account.
     * The given {@code step} and {@code dimensions} are typically the values of
     * {@link #transform} and {@link #targetDimensions} fields respectively, but not necessarily.
     *
     * <p>Subclasses can override this method if they need to handle some {@code MathTransform} implementations
     * in a special way. However, all implementations of this method shall obey to the following contract:</p>
     * <ul>
     *   <li>{@link #sourceDimensions} and {@link #targetDimensions} should not be assumed accurate.</li>
     *   <li>{@link #sourceDimensions} should not be modified by this method.</li>
     *   <li>{@link #targetDimensions} should not be modified by this method.</li>
     * </ul>
     *
     * The number and nature of inputs stay unchanged. For example if the supplied {@code transform}
     * has (<var>longitude</var>, <var>latitude</var>, <var>height</var>) outputs, then a filtered
     * transform may keep only the (<var>longitude</var>, <var>latitude</var>) part for the same inputs.
     * In most cases, the filtered transform is non-invertible since it looses information.
     *
     * @param  step        the transform for which to retain only a subset of the target dimensions.
     * @param  dimensions  indices of the target dimensions of {@code step} to retain.
     * @return a transform producing only the given target dimensions.
     * @throws FactoryException if the given transform is not separable.
     */
    protected MathTransform filterTargetDimensions(MathTransform step, final int[] dimensions) throws FactoryException {
        final int numSrc = step.getSourceDimensions();
              int numTgt = step.getTargetDimensions();
        final int lower  = dimensions[0];
        final int upper  = dimensions[dimensions.length - 1];
        if (lower == 0 && upper == numTgt && dimensions.length == numTgt) {
            return step;
        }
        /*
         * If the transform is an instance of passthrough transform but no dimension from its sub-transform
         * is requested, then ignore the sub-transform (i.e. treat the whole transform as identity, except
         * for the number of target dimension which may be different from the number of input dimension).
         */
        int removeAt = 0;
        int numRemoved = 0;
        if (step instanceof PassThroughTransform) {
            final var passThrough = (PassThroughTransform) step;
            final int subLower  = passThrough.firstAffectedCoordinate;
            final int numSubTgt = passThrough.subTransform.getTargetDimensions();
            if (!containsAny(dimensions, subLower, subLower + numSubTgt)) {
                step = IdentityTransform.create(numTgt = numSrc);
                removeAt = subLower;
                numRemoved = numSubTgt - passThrough.subTransform.getSourceDimensions();
            }
        }
        /*                                                  ┌  ┐     ┌          ┐ ┌ ┐
         * Create the matrix to be used as a filter         │x'│     │1  0  0  0│ │x│
         * and concatenate it to the transform. The         │z'│  =  │0  0  1  0│ │y│
         * matrix will contain 1 only in the target         │1 │     │0  0  0  1│ │z│
         * dimensions to keep, as in this example:          └  ┘     └          ┘ │1│
         *                                                                        └ ┘
         */
        final Matrix matrix = Matrices.createZero(dimensions.length + 1, numTgt + 1);
        for (int j=0; j<dimensions.length; j++) {
            int i = dimensions[j];
            if (i >= removeAt) {
                i -= numRemoved;
            }
            matrix.setElement(j, i, 1);
        }
        matrix.setElement(dimensions.length, numTgt, 1);
        return factory.createConcatenatedTransform(step, factory.createAffineTransform(matrix));
    }

    /**
     * Removes the sources dimensions that are not required for computing the target dimensions.
     * This method is invoked only if {@link #sourceDimensions} is null at {@link #separate()} invocation time.
     * This method can operate only on the first transform of a transformation chain.
     * If this method succeed, then {@link #sourceDimensions} will be updated.
     *
     * <p>This method can process only linear transforms (potentially indirectly through a concatenated transform).
     * Actually it would be possible to also process pass-through transform followed by a linear transform, but this
     * case should have been optimized during transform concatenation. If it is not the case, consider improving the
     * {@link PassThroughTransform#tryConcatenate(TransformJoiner)} method instead of this one.</p>
     *
     * @param  head      the first transform of a transformation chain.
     * @param  required  sources to keep even if not necessary, or {@code null} if none.
     * @return the reduced transform, or {@code head} if this method did not reduced the transform.
     */
    private MathTransform removeUnusedSourceDimensions(final MathTransform head, final int[] required) {
        Matrix m = MathTransforms.getMatrix(head);
        if (m != null) {
            final int numRows   = m.getNumRow();            // Number of target dimensions + 1.
            final int dimension = m.getNumCol() - 1;        // Number of source dimensions (ignore translations column).
            int   retainedCount = 0;                        // Number of source dimensions to keep.
            int[] retainedDimensions = new int[dimension];
            for (int i=0; i<dimension; i++) {
                if (required != null && Arrays.binarySearch(required, i) >= 0) {
                    // Dimension to retain unconditionally.
                    retainedDimensions[retainedCount++] = i;
                } else {
                    for (int j=0; j<numRows; j++) {
                        if (m.getElement(j,i) != 0) {
                            // Found a source dimension which is required by target dimension.
                            retainedDimensions[retainedCount++] = i;
                            break;
                        }
                    }
                }
            }
            if (retainedCount != dimension) {
                retainedDimensions = Arrays.copyOf(retainedDimensions, retainedCount);
                /*
                 * If we do not retain all dimensions, remove the matrix columns corresponding to the excluded
                 * source dimensions and create a new transform. We remove consecutive columns in single calls
                 * to `removeColumns`, from `lower` inclusive to `upper` exclusive.
                 */
                int upper = dimension;
                for (int i = retainedCount; --i >= -1;) {
                    final int keep = (i >= 0) ? retainedDimensions[i] : -1;
                    final int lower = keep + 1;                                     // First column to exclude.
                    if (lower != upper) {
                        // Remove source dimensions that are not retained.
                        m = MatrixSIS.castOrCopy(m).removeColumns(lower, upper);
                    }
                    upper = keep;
                }
                /*
                 * If the user specified source dimensions, the indices need to be adjusted.
                 * This loop has no effect if all source dimensions were kept before this method call.
                 */
                for (int i=0; i<retainedCount; i++) {
                    retainedDimensions[i] = sourceDimensions[retainedDimensions[i]];
                }
                sourceDimensions = retainedDimensions;
                return MathTransforms.linear(m);
            }
        } else if (head instanceof ConcatenatedTransform) {
            final MathTransform transform1 = ((ConcatenatedTransform) head).transform1;
            final MathTransform reduced = removeUnusedSourceDimensions(transform1, required);
            if (reduced != transform1) {
                return MathTransforms.concatenate(reduced, ((ConcatenatedTransform) head).transform2);
            }
        }
        return head;
    }

    /**
     * Returns {@code true} if the given sequence contains all indices in the range {@code lower} inclusive
     * to {@code upper} exclusive.
     *
     * @param  sequence  the {@link #sourceDimensions} or {@link #targetDimensions} sequence to test.
     * @param  lower     the lower value, inclusive.
     * @param  upper     the upper value, exclusive.
     * @return {@code true} if the full range was found in the sequence.
     */
    private static boolean containsAll(final int[] sequence, final int lower, int upper) {
        if (lower >= upper) {
            return true;
        }
        if (sequence != null) {
            assert ArraysExt.isSorted(sequence, true);
            int index = Arrays.binarySearch(sequence, lower);
            if (index >= 0) {
                index += --upper - lower;
                if (index >= 0 && index < sequence.length) {
                    return sequence[index] == upper;
                }
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the given sequence contains any value in the given range.
     *
     * @param  sequence  the {@link #sourceDimensions} or {@link #targetDimensions} sequence to test.
     * @param  lower     the lower value, inclusive.
     * @param  upper     the upper value, exclusive.
     * @return {@code true} if the sequence contains at least one value in the given range.
     */
    private static boolean containsAny(final int[] sequence, final int lower, final int upper) {
        if (upper == lower) {
            return true;
        }
        if (sequence != null) {
            assert ArraysExt.isSorted(sequence, true);
            int index = Arrays.binarySearch(sequence, lower);
            if (index >= 0) {
                return true;
            }
            index = ~index;                                                 // Tild, not minus sign.
            return (index < sequence.length) && (sequence[index] < upper);
        }
        return false;
    }
}
