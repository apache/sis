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
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;


/**
 * Extracts a sub-transform from a given {@code MathTransform} and source or target dimension indices.
 * Given an arbitrary {@link MathTransform}, this class tries to return a new math transform that operates
 * only on a given set of source or target dimensions.
 *
 * <div class="note"><b>Example:</b>
 * if the supplied {@code transform} has (<var>x</var>,<var>y</var>,<var>z</var>) inputs
 * and (<var>λ</var>,<var>φ</var>,<var>h</var>) outputs, then the following code:
 *
 * {@preformat java
 *     TransformSeparator s = new TransformSeparator(theTransform);
 *     s.addSourceDimensionRange(0, 2);
 *     MathTransform mt = s.separate();
 * }
 *
 * will return a transform with (<var>x</var>,<var>y</var>) inputs and (probably) (<var>λ</var>,<var>φ</var>) outputs.
 * The output dimensions can be verified with a call to {@link #getTargetDimensions()}.</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
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
    protected final MathTransformFactory factory;

    /**
     * Constructs a separator for the given transform.
     *
     * @param transform The transform to separate.
     */
    public TransformSeparator(final MathTransform transform) {
        this(transform, DefaultFactories.forBuildin(MathTransformFactory.class));
    }

    /**
     * Constructs a separator for the given transform and using the given factory.
     *
     * @param transform The transform to separate.
     * @param factory The factory to use for creating new math transforms.
     */
    public TransformSeparator(final MathTransform transform, final MathTransformFactory factory) {
        ArgumentChecks.ensureNonNull("transform", transform);
        ArgumentChecks.ensureNonNull("factory", factory);
        this.transform = transform;
        this.factory   = factory;
    }

    /**
     * Clears any {@linkplain #getSourceDimensions() source} and {@linkplain #getTargetDimensions() target dimension}
     * settings. This method can be invoked when the same {@code MathTransform} needs to be separated in more than one
     * part, for example an horizontal and a vertical component.
     */
    public void clear() {
        sourceDimensions = null;
        targetDimensions = null;
    }

    /**
     * Inserts the specified {@code dimension} in the specified sequence at a position that preserve increasing order.
     * This method does nothing if the given dimension already exists in the given array.
     *
     * <div class="note"><b>Note:</b>
     * we do not provide public API for this method because we rather encourage bulk operations (adding many values
     * at once), and because the public API does not allow to specify values in random order (for avoiding risk of
     * confusion as some users could expect the separated transform to use the dimensions in the order he specified
     * them).</div>
     *
     * @param sequence   The {@link #sourceDimensions} or {@link #targetDimensions} sequence to update.
     * @param dimension  The value to add to the given sequence.
     */
    private static int[] insert(int[] sequence, int dimension) throws IllegalArgumentException {
        if (sequence == null) {
            return new int[] {dimension};
        }
        assert ArraysExt.isSorted(sequence, true);
        int i = Arrays.binarySearch(sequence, dimension);
        if (i < 0) {
            i = ~i;   // Tild, not the minus sign.
            sequence = ArraysExt.insert(sequence, i, 1);
            sequence[i] = dimension;
        }
        assert Arrays.binarySearch(sequence, dimension) == i;
        return sequence;
    }

    /**
     * Adds the specified {@code dimensions} to the specified sequence.
     * Values must be given in strictly increasing order.
     *
     * @param  sequence    The {@link #sourceDimensions} or {@link #targetDimensions} sequence to update.
     * @param  dimensions  The user-supplied dimensions to add to the given sequence.
     * @param  max         The maximal value allowed, exclusive.
     * @throws IllegalArgumentException if a {@code dimensions} value does not meet the conditions.
     */
    private static int[] add(int[] sequence, final int[] dimensions, final int max) throws IllegalArgumentException {
        int offset = 0;
        int previous = -1;  // This initial value will ensure that we have no negative value.
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
            final int value = sequence[i];
            if (value <= previous || value >= max) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.ValueOutOfRange_4,
                        "dimensions[" + (i - offset) + ']', previous + 1, max - 1, value));
            }
            previous = value;
        }
        return sequence;
    }

    /**
     * Adds the specified range to the specified sequence.
     *
     * @param  sequence  The {@link #sourceDimensions} or {@link #targetDimensions} sequence to update.
     * @param  lower     The lower value of the range to add, inclusive.
     * @param  upper     The upper value of the range to add, exclusive.
     * @param  max       The maximal value allowed, exclusive.
     * @throws IllegalArgumentException if the {@code lower} or {@code upper} value does not meet the conditions.
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
            sequence = series(lower, upper);
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
     * Returns a series of increasing values starting at {@code lower}.
     */
    private static int[] series(final int lower, final int upper) throws IllegalArgumentException {
        final int[] sequence = new int[upper - lower];
        for (int i = 0; i < sequence.length; i++) {
            sequence[i] = i + lower;
        }
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
     * @param  dimensions A sequence of source dimensions to keep, in strictly increasing order.
     * @throws IllegalArgumentException if {@code dimensions} contains negative values
     *         or if values are not in a strictly increasing order.
     */
    public void addSourceDimensions(final int... dimensions) throws IllegalArgumentException {
        ArgumentChecks.ensureNonNull("dimensions", dimensions);
        sourceDimensions = add(sourceDimensions, dimensions, transform.getSourceDimensions());
    }

    /**
     * Adds a range of input dimensions to keep in the separated transform.
     * The {@code lower} and {@code upper} values define a range of  <em>source</em> dimension indices
     * of the transform given to the constructor.
     *
     * @param  lower The lower dimension, inclusive. Shall not be smaller than 0.
     * @param  upper The upper dimension, exclusive. Shall be smaller than {@link MathTransform#getSourceDimensions()}.
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
     *       It is often, but not necessarily, all source dimensions of the transform given at construction time.</li>
     *
     *   <li>Otherwise an exception is thrown.</li>
     * </ol>
     *
     * @return The input dimension as a sequence of strictly increasing values.
     * @throws IllegalStateException if input dimensions have not been set and
     *         {@link #separate()} has not yet been invoked.
     */
    public int[] getSourceDimensions() throws IllegalStateException {
        if (sourceDimensions != null) {
            return sourceDimensions.clone();
        }
        throw new IllegalStateException(Errors.format(Errors.Keys.UnspecifiedDimensions));
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
     * @param  dimensions A sequence of target dimensions to keep, in strictly increasing order.
     * @throws IllegalArgumentException if {@code dimensions} contains negative values
     *         or if values are not in a strictly increasing order.
     */
    public void addTargetDimensions(final int... dimensions) throws IllegalArgumentException {
        ArgumentChecks.ensureNonNull("dimensions", dimensions);
        targetDimensions = add(targetDimensions, dimensions, transform.getTargetDimensions());
    }

    /**
     * Adds a range of output dimensions to keep in the separated transform.
     * The {@code lower} and {@code upper} values define a range of <em>target</em> dimension indices
     * of the transform given to the constructor.
     *
     * @param  lower The lower dimension, inclusive. Shall not be smaller than 0.
     * @param  upper The upper dimension, exclusive. Shall be smaller than {@link MathTransform#getTargetDimensions()}.
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
     * @return The output dimension as a sequence of strictly increasing values.
     * @throws IllegalStateException if output dimensions have not been set and
     *         {@link #separate()} has not yet been invoked.
     */
    public int[] getTargetDimensions() throws IllegalStateException {
        if (targetDimensions != null) {
            return targetDimensions.clone();
        }
        throw new IllegalStateException(Errors.format(Errors.Keys.UnspecifiedDimensions));
    }

    /**
     * Separates the math transform specified at construction time for given dimension indices.
     * This method creates a math transform that use only the {@linkplain #addSourceDimensions(int...) specified
     * source dimensions} and return only the {@linkplain #addTargetDimensions(int...) specified target dimensions}.
     * If the source or target dimensions were not specified, then they will be inferred as below:
     *
     * <ul class="verbose">
     *   <li>If source dimensions were unspecified, then the returned transform will keep at least all source
     *       dimensions needed for computing the specified target dimensions. In many cases the returned transform
     *       unconditionally keep all source dimensions, but not necessarily. If all source dimensions need to be
     *       kept, it is better to {@linkplain #addSourceDimensionRange(int, int) specify that explicitely}.</li>
     *
     *   <li>If target dimensions were unspecified, then the returned transform will expect only the specified
     *       source dimensions as inputs, and the target dimensions will be inferred automatically.</li>
     * </ul>
     *
     * The source and target dimensions actually used can be queried by calls to {@link #getSourceDimensions()}
     * or {@link #getTargetDimensions()} after this {@code separate()} method.
     *
     * @return The separated math transform.
     * @throws FactoryException if the transform can not be separated.
     */
    public MathTransform separate() throws FactoryException {
        MathTransform tr = transform;
        if (sourceDimensions == null || containsAll(sourceDimensions, 0, tr.getSourceDimensions())) {
            if (targetDimensions != null && !containsAll(targetDimensions, 0, tr.getTargetDimensions())) {
                tr = filterTargetDimensions(tr, targetDimensions);
            }
            if (sourceDimensions == null) {
                sourceDimensions = series(0, transform.getSourceDimensions());
            }
            if (targetDimensions == null) {
                targetDimensions = series(0, transform.getTargetDimensions());
            }
        } else {
            /*
             * At this point there is at least one source dimensions to take in account.
             * Source dimensions are more difficult to process than target dimensions.
             */
            final int[] requested = targetDimensions;
            tr = filterSourceDimensions(tr, sourceDimensions);  // May update targetDimensions.
            assert ArraysExt.isSorted(targetDimensions, true) : "targetDimensions";
            if (requested != null) {
                final int[] inferred = targetDimensions;
                targetDimensions = requested;
                final int[] subDimensions = new int[requested.length];
                for (int i=0; i<requested.length; i++) {
                    final int r = requested[i];
                    final int j = Arrays.binarySearch(inferred, r);
                    if (j < 0) {
                        /*
                         * The user asked for some target dimensions that we can not keep, probably
                         * because at least one of the requested target dimensions has a dependency to a
                         * source dimension that does not appear in the list of source dimensions to keep.
                         */
                        throw new FactoryException(Errors.format(Errors.Keys.CanNotSeparateTargetDimension_1, r));
                    }
                    subDimensions[i] = j;
                }
                tr = filterTargetDimensions(tr, subDimensions);
            }
        }
        /*
         * We are done. But do a final verification on the number of dimensions.
         */
        int type     = 0;
        int expected = sourceDimensions.length;
        int actual   = tr.getSourceDimensions();
        if (actual == expected) {
            type     = 1;
            expected = targetDimensions.length;
            actual   = tr.getTargetDimensions();
            if (actual == expected) {
                return tr;
            }
        }
        throw new FactoryException(Errors.format(Errors.Keys.MismatchedTransformDimension_3, type, expected, actual));
    }

    /**
     * Creates a transform for the same mathematic than the given {@code step}
     * but expecting only the given dimensions as inputs.
     * This method is invoked by {@link #separate()} when user-specified source dimensions need to be taken in account.
     * The given {@code step} and {@code dimensions} are typically the values of
     * {@link #transform} and {@link #sourceDimensions} fields respectively, but not necessarily.
     * In particular those arguments will differ when this method is invoked recursively for processing
     * concatenated or {@linkplain PassThroughTransform#getSubTransform() sub-transforms}.
     *
     * <p>Subclasses can override this method if they need to handle some {@code MathTransform} implementations
     * in a special way. However all implementations of this method shall obey to the following contract:</p>
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
     * @param  step The transform for which to retain only a subset of the source dimensions.
     * @param  dimensions Indices of the source dimensions of {@code step} to retain.
     * @return A transform expecting only the given source dimensions.
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
            targetDimensions = series(0, numTgt);
            return step;
        }
        if (step.isIdentity()) {
            targetDimensions = dimensions;
            return IdentityTransform.create(dimensions.length);
        }
        if (step instanceof ConcatenatedTransform) {
            final ConcatenatedTransform ctr = (ConcatenatedTransform) step;
            final MathTransform step1 = filterSourceDimensions(ctr.transform1, dimensions);
            final MathTransform step2 = filterSourceDimensions(ctr.transform2, targetDimensions);
            return factory.createConcatenatedTransform(step1, step2);
            // Keep the 'targetDimensions' computed by the last step.
        }
        /*
         * Special case for the passthrough transform: if at least one input dimension belong to the pass-
         * through sub-transform, then invoke this method recursively for the sub-transform dimensions.
         */
        if (step instanceof PassThroughTransform) {
            final PassThroughTransform passThrough = (PassThroughTransform) step;
            final int numSubSrc = passThrough.subTransform.getSourceDimensions();
            final int numNewDim = passThrough.subTransform.getTargetDimensions() - numSubSrc;
            final int subLower  = passThrough.firstAffectedOrdinate;
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
                // Dimension n belong to heading or trailing dimensions.
                // Passthrough, after adjustment for trailing dimensions.
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
             * the list of target dimensions. We need to offset the target dimensions by the amount of leading
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
             * transform implementation. The "consecutive numbers" requirement (expressed in the 'if' statement below)
             * is a consequence of a limitation in our current implementation: our current passthrough transform does
             * not accept arbitrary index for modified ordinates.
             */
            if (containsAll(dimensions, lower, subLower) && containsAll(dimensions, subUpper, upper)) {
                return factory.createPassThroughTransform(subLower - lower, subTransform, upper - subUpper);
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
            int startOfRow = 0;
            boolean isLastRowAccepted = false;
            final int numFilteredColumns = (dimensions.length + 1);
            double[] elements = new double[(numTgt + 1) * numFilteredColumns];
reduce:     for (int j=0; j <= numTgt; j++) {
                /*
                 * For each target dimension (i.e. a matrix row), find the matrix elements (excluding translation
                 * terms in the last column) for each source dimension to be kept. If a dependancy to at least one
                 * discarded input dimension is found, then the whole output dimension is discarded.
                 */
                int filteredColumn = 0;
                for (int i=0; i<numSrc; i++) {
                    final double element = matrix.getElement(j,i);
                    if (filteredColumn < dimensions.length && dimensions[filteredColumn] == i) {
                        elements[startOfRow + filteredColumn++] = element;
                    } else if (element != 0) {
                        // Output dimension 'j' depends on one of discarded input dimension 'i'.
                        // The whole row will be discarded.
                        continue reduce;
                    }
                }
                elements[startOfRow + filteredColumn++] = matrix.getElement(j, numSrc);  // Copy the translation term.
                assert filteredColumn == numFilteredColumns : filteredColumn;            // We should have used all values in the 'dimensions' array.
                startOfRow += numFilteredColumns;
                if (j == numTgt) {
                    // In an affine transform, the last row is usually [0 0 0 … 1].
                    // This is not a real dimension, but nevertheless mandatory.
                    isLastRowAccepted = true;
                } else {
                    targetDimensions = insert(targetDimensions, j);
                }
            }
            if (isLastRowAccepted) {
                elements = ArraysExt.resize(elements, startOfRow);
                return factory.createAffineTransform(Matrices.create(startOfRow / numFilteredColumns, numFilteredColumns, elements));
            }
            /*
             * In an affine transform, the last row is not supposed to have dependency to any source dimension.
             * But if we reach this point, our matrix has such dependencies.
             */
        }
        throw new FactoryException(Errors.format(Errors.Keys.NotAnAffineTransform));
    }

    /**
     * Creates a transform for the same mathematic than the given {@code step}
     * but producing only the given dimensions as outputs.
     * This method is invoked by {@link #separate()} when user-specified target dimensions need to be taken in account.
     * The given {@code step} and {@code dimensions} are typically the values of
     * {@link #transform} and {@link #targetDimensions} fields respectively, but not necessarily.
     *
     * <p>Subclasses can override this method if they need to handle some {@code MathTransform} implementations
     * in a special way. However all implementations of this method shall obey to the following contract:</p>
     * <ul>
     *   <li>{@link #sourceDimensions} and {@link #targetDimensions} should not be assumed accurate.</li>
     *   <li>{@link #sourceDimensions} should not be modified by this method.</li>
     *   <li>{@link #targetDimensions} should not be modified by this method.</li>
     * </ul>
     *
     * The number and nature of inputs stay unchanged. For example if the supplied {@code transform}
     * has (<var>longitude</var>, <var>latitude</var>, <var>height</var>) outputs, then a filtered
     * transform may keep only the (<var>longitude</var>, <var>latitude</var>) part for the same inputs.
     * In most cases, the filtered transform is non-invertible since it loose informations.
     *
     * @param  step The transform for which to retain only a subset of the target dimensions.
     * @param  dimensions Indices of the target dimensions of {@code step} to retain.
     * @return A transform producing only the given target dimensions.
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
            final PassThroughTransform passThrough = (PassThroughTransform) step;
            final int subLower  = passThrough.firstAffectedOrdinate;
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
     * Returns {@code true} if the given sequence contains all index in the range {@code lower} inclusive
     * to {@code upper} exclusive.
     *
     * @param  sequence The {@link #sourceDimensions} or {@link #targetDimensions} sequence to test.
     * @param  lower    The lower value, inclusive.
     * @param  upper    The upper value, exclusive.
     * @return {@code true} if the full range was found in the sequence.
     */
    private static boolean containsAll(final int[] sequence, final int lower, int upper) {
        if (lower == upper) {
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
     * @param  sequence The {@link #sourceDimensions} or {@link #targetDimensions} sequence to test.
     * @param  lower    The lower value, inclusive.
     * @param  upper    The upper value, exclusive.
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
            index = ~index;    // Tild, not minus sign.
            return (index < sequence.length) && (sequence[index] < upper);
        }
        return false;
    }
}
