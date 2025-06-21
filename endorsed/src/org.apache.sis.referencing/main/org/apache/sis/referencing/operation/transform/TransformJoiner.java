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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.IntFunction;
import java.util.function.UnaryOperator;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.privy.ExtendedPrecisionMatrix;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.BackingStoreException;


/**
 * Handler used during concatenations for simplifying a single transform with its neighbor transforms.
 * Instances of {@code TransformJoiner} may be created automatically by Apache <abbr>SIS</abbr> at any
 * step during a {@linkplain MathTransforms#concatenate(MathTransform, MathTransform) concatenation}.
 * Those {@code TransformJoiner} instances give to {@link AbstractMathTransform} implementations
 * an opportunity to replace the default concatenation algorithm by an optimized alternative.
 * Some examples of optimizations are:
 *
 * <ul>
 *   <li>detecting when two consecutive non-linear operations are equivalent to a third non-linear operation,</li>
 *   <li>modifying the linear transforms that are executed before or after the transform to optimize
 *       (it is sometime possible to remove a transform step that way).</li>
 * </ul>
 *
 * Because the simplifications that are possible depend on the transform's formulas,
 * each {@link AbstractMathTransform} subclass uses {@code TransformJoiner} in a different way.
 *
 * <h2>Usage</h2>
 * This class is used by overriding the {@link AbstractMathTransform#tryConcatenate(TransformJoiner)} method.
 * The implementation can inspect the surrounding transforms with calls to {@link #getTransform(int)}.
 * The argument given to {@code getTransform(…)} is a relative index:
 * value 0 identifies the transform on which {@code tryConcatenate(…)} has been invoked,
 * -1 the transform immediately before it, and +1 the transform immediately after it.
 * The {@code tryConcatenate(…)} implementation can replace the transform at relative index 0 together with
 * a neighbor transform (at relative index -1 or +1) by a call to {@link #replace(int, MathTransform)}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @see AbstractMathTransform#tryConcatenate(TransformJoiner)
 *
 * @since 1.5
 */
public final class TransformJoiner {
    /**
     * Tolerance threshold for considering a matrix as identity. Since the value used here is smaller
     * than 1 ULP (about 2.22E-16), it applies only to the zero terms in the matrix. The terms on the
     * diagonal are still expected to be exactly 1.
     */
    private static final double IDENTITY_TOLERANCE = 1E-16;

    /**
     * Partial or complete list of transform instances to concatenate.
     * This list may contain instances of {@link ConcatenatedTransform}.
     * Those instances are hidden by {@link #getTransform(int)}, but are kept in this internal list
     * in order to reuse any existing {@link ConcatenatedTransform} instances that do need to be replaced.
     * This is also for avoiding to reanalyze transform chain fragments that have already been analyzed.
     *
     * <p>In the current implementation, this list should be short (between 2 and 4 elements)
     * because {@link #replacement} will replace the whole list, and we do not want to rebuild
     * the full transform chain. Therefore, this list contains only a fragment of the chain.</p>
     *
     * <h4>Mutability</h4>
     * This list shall not be modified by public methods of this class.
     * This list can be modified by calls to {@link #simplify()} and {@link #reassemble()}.
     */
    private final List<MathTransform> steps;

    /**
     * Index of the transform to simplify in the {@link #steps} list.
     * This is the index of the transform on which {@code tryConcatenate(…)} has been invoked.
     */
    private int replaceIndex;

    /**
     * The factory to use for creating the transform to use as a replacement of the transforms to simplify.
     * Never null.
     */
    public final MathTransformFactory factory;

    /**
     * A pool of concatenated transforms, used only for reusing instances when possible.
     * The content of this list does not change the result, except for the instance reusage.
     *
     * @see #concatenate(MathTransform, MathTransform)
     */
    private final ConcatenatedTransform[] pool;

    /**
     * The concatenated transform to use as a replacement, or {@code null} if none.
     * If non-null, this transform replaces all transforms in the {@link #steps} list.
     */
    MathTransform replacement;

    /**
     * Creates new contextual information for a concatenation to optimize.
     * The given list is stored by reference without copy (this is intentional and required).
     *
     * @param steps    partial or complete list of coordinate operations to concatenate.
     * @param factory  the factory to use for creating the concatenated transform.
     * @param pool     a pool of concatenated transforms, used only for reusing instances when possible.
     */
    TransformJoiner(final List<MathTransform> steps, final MathTransformFactory factory,
                    final List<ConcatenatedTransform> pool)
    {
        this.steps   = steps;
        this.factory = factory;
        this.pool    = pool.toArray(ConcatenatedTransform[]::new);
    }

    /**
     * Resets this {@code TransformJoiner} and makes it ready for reuse for a transform at a new index.
     *
     * @param i  index of the transform to simplify in the {@link #steps} list.
     */
    final void reset(final int i) {
        replaceIndex  = i;
        replacement = null;
    }

    /**
     * Returns a transform before or after the transform to simplify. By definition, invoking this method
     * with a relative index of 0 always returns the transform on which {@code tryConcatenate(…)} has been invoked.
     * A relative index of -1 returns the previous transform while a relative index of +1 returns the next transform.
     *
     * <p>This method may return an empty value if the requested transform is not available from this
     * {@code TransformJoiner} instance. It does not mean that the transform does not exist at all in
     * the complete chain of transforms, because each {@code TransformJoiner} instance may know only
     * a fragment of that chain.</p>
     *
     * @param  relativeIndex  index relative to the transform on which {@code tryConcatenate(…)} has been invoked.
     * @return the requested transform. An empty value does not necessarily mean that the transform does not exist.
     */
    public Optional<MathTransform> getTransform(final int relativeIndex) {
        final int i = replaceIndex + relativeIndex;
valid:  if (i >= 0 && i < steps.size()) {
            final int direction = Integer.signum(relativeIndex);
            for (int j = replaceIndex; (j += direction) != i;) {
                if (steps.get(j) instanceof ConcatenatedTransform) {
                    // Intermediate concatenated transforms (before the requested index) are not supported.
                    break valid;
                }
            }
            MathTransform step = steps.get(i);
            switch (direction) {
                case -1: step = MathTransforms.getLastStep (step); break;
                case +1: step = MathTransforms.getFirstStep(step); break;
            }
            return Optional.of(step);
        }
        return Optional.empty();
    }

    /**
     * Returns a transform as a matrix if that transform is linear.
     * This is equivalent to invoking {@link #getTransform(int)} followed by fetching the transform matrix.
     *
     * @param  relativeIndex  index of the matrix to get,
     *         relative to the transform on which {@code tryConcatenate(…)} has been invoked.
     * @return the matrix of the requested transform if that transform is linear, or {@code null} otherwise.
     */
    final Matrix getMatrix(final int relativeIndex) {
        return MathTransforms.getMatrix(getTransform(relativeIndex).orElse(null));
    }

    /**
     * If the specified range of dimensions is unused by the specified neighbor transform, removes those dimensions.
     * A range of dimensions is considered unused by a neighbor if
     * <code>{@link #getTransform(int) getTransform}(relativeIndex)</code> returns a
     * linear transform represented by a matrix having the following characteristic:
     *
     * <ul class="verbose">
     *   <li>For negative {@code relativeIndex}, all values are zeros in the <em>rows</em> from {@code lower}
     *       inclusive to {@code upper} exclusive. In such case, the transform at index {@code relativeIndex}+1
     *       receives input coordinates where the values are always 0 in those dimensions.</li>
     *   <li>For positive {@code relativeIndex}, all values are zeros in the <em>columns</em> from {@code lower}
     *       inclusive to {@code upper} exclusive. In such case, the transform at index {@code relativeIndex}-1
     *       produces output coordinates where the values are always ignored in those dimensions.</li>
     * </ul>
     *
     * If the specified range of dimensions is considered unused, then this method performs the following steps:
     *
     * <ol class="verbose">
     *   <li>Reduce the size of the matrix of the neighbor transform specified by {@code relativeIndex}.
     *       That reduction is done by removing the rows or columns of above-mentioned zero values.</li>
     *   <li>Invoke {@code reduce.apply(dimension)} for getting the replacement to use in place of all
     *       transforms at relative indexes between 0 (inclusive) and {@code relativeIndex} (exclusive).
     *       The {@code reduce} function will receive in argument the new number of dimensions.
     *       If {@code reduce} returns {@code null}, this method exits without doing any replacement.</li>
     *   <li>The transform described by the reduced matrix (step 1) is concatenated with the transform of step 2.
     *       The concatenation result {@linkplain #replace replaces} all transforms at relative indexes between 0
     *       (inclusive) and {@code relativeIndex} (also inclusive).</li>
     * </ol>
     *
     * <h4>Example</h4>
     * For a matrix that describes an affine transform, if the column <var>i</var> has only zero values,
     * then the coordinate at index <var>i</var> in source coordinate tuples is unused and can be discarded.
     * For example, a matrix of size 4×3 describes an affine transform from three-dimensional coordinates to
     * two-dimensional coordinates. In many cases, such affine transform drops the <var>z</var> coordinate.
     * In order to check and potentially replace such transform by a two-dimensional transform working only
     * with <var>x</var> and <var>y</var> coordinates, the following code can be used:
     *
     * {@snippet lang="java" :
     *     class MyTransform3D extends AbstractTransform {
     *         private static final int Z_DIM = 2;
     *
     *         @Override
     *         protected void tryConcatenate(TransformJoiner context) throws FactoryException {
     *             if (!context.removeUnusedDimensions(+1, Z_DIM, Z_DIM+1, this::reduce)) {
     *                 super.tryConcatenate(context);    // Try other strategies.
     *             }
     *         }
     *
     *         private MathTransform reduce(int newDim) {
     *             switch (newDim) {
     *                 case 2:  return new MyTransform2D();
     *                 default: return null;
     *             }
     *         }
     *     }
     *     }
     *
     * In some implementations, such replacement of a {@code MyTransform3D} class by a {@code MyTransform2D} class
     * may avoid the unnecessary calculation of <var>z</var> output coordinate values.
     *
     * @param  relativeIndex  relative index of an affine transform step which may be ignoring some dimensions.
     * @param  lower   index of the first dimension to remove if the dimensions are unused.
     * @param  upper   index after the last dimension to remove if the dimensions are unused.
     * @param  reduce  provider of a transform equivalent to the transform on which {@code tryConcatenate(…)}
     *                 has been invoked, but with the number of dimensions given in argument to that provider.
     * @return whether the replacement has been done.
     * @throws FactoryException if the reduced transform is not a valid replacement.
     */
    // Waiting for more experience before to decide if this method should be public.
    final boolean removeUnusedDimensions(final int relativeIndex, final int lower, final int upper,
            final IntFunction<MathTransform> reduce) throws FactoryException
    {
        final Matrix matrix = getMatrix(relativeIndex);
        if (matrix == null || relativeIndex == 0) {
            return false;
        }
        final boolean before = (relativeIndex < 0);
        for (int dimension = lower; dimension < upper; dimension++) {
            if (before) {
                for (int i = matrix.getNumCol(); --i >= 0;) {
                    if (matrix.getElement(dimension, i) != 0) {
                        return false;
                    }
                }
            } else {
                for (int j = matrix.getNumRow(); --j >= 0;) {
                    if (matrix.getElement(j, dimension) != 0) {
                        return false;
                    }
                }
            }
        }
        /*
         * The specified range of dimensions is unused in the neighbor transform.
         * Try to create the non-linear transform with a reduced number of dimensions.
         */
        final int dimension = (before ? matrix.getNumRow() : matrix.getNumCol()) - (upper - lower) - 1;
        final MathTransform reduced;
        try {
            reduced = reduce.apply(dimension);
        } catch (BackingStoreException e) {
            throw e.unwrapOrRethrow(FactoryException.class);
        }
        if (reduced == null) {
            return false;
        }
        MatrixSIS m = MatrixSIS.castOrCopy(matrix);
        m = before ? m.removeRows   (lower, upper)
                   : m.removeColumns(lower, upper);

        final MathTransform linear  = factory.createAffineTransform(m);
        return replace(relativeIndex, concatenate(before ? linear : reduced,
                                                  before ? reduced : linear));
    }

    /**
     * Tries to replace the transform at relative index 0, together with a neighbor, by the given transform.
     * The {@code firstOrLast} argument specifies which neighbors are replaced by the specified transform:
     *
     * <ul class="verbose">
     *   <li>If -1, then transforming a point <var>p</var> by {@code concatenation} shall be equivalent
     *       to first transforming <var>p</var> by {@code getTransform(-1)}, and then transforming the
     *       result by {@code getTransform(0)}.</li>
     *   <li>If +1, then transforming a point <var>p</var> by {@code concatenation} shall be equivalent
     *       to first transforming <var>p</var> by {@code getTransform(0)}, and then transforming the
     *       result by {@code getTransform(+1)}.</li>
     *   <li>If 0, then only the transform at index 0 is replaced. Neighbor are unchanged.</li>
     * </ul>
     *
     * @param  firstOrLast    relative index of first (if negative) or last (if positive) transform to replace.
     * @param  concatenation  the transform to use instead of the one at index 0 and the specified neighbors.
     * @return whether the replacement has been done. May be {@code false} if {@code firstOrLast} is too large.
     * @throws FactoryException if the given concatenation is not a valid replacement.
     */
    public boolean replace(final int firstOrLast, MathTransform concatenation) throws FactoryException {
        return replace(Math.min(firstOrLast, 0), Math.max(firstOrLast, 0), concatenation);
    }

    /**
     * Tries to replace the transforms in the given range of relative indices, which must include 0.
     * This method is similar to {@link #replace(int, MathTransform)}, but allowing to replace the
     * neighbor transforms on both sides.
     *
     * <h4>Example</h4>
     * A call to {@code transform(-1, +1, tr)} replaces the transform on which {@code tryConcatenate(…)}
     * has been invoked (this is the transform at relative index 0) together with the transforms located
     * immediately before (-1) and immediately after (+1) by the given {@code tr} transform.
     *
     * @param  from  relative index of the first transform to replace, inclusive. This is usually -1 or 0.
     * @param  to    relative index of the last transform to replace, inclusive. This is usually 0 or +1.
     * @param  concatenation  the transform to use instead of the transforms in the given range.
     * @return whether the replacement has been done.
     * @throws FactoryException if the given concatenation is not a valid replacement.
     */
    public boolean replace(int from, int to, MathTransform concatenation) throws FactoryException {
        ArgumentChecks.ensureNonNull("concatenation", concatenation);
        if (from > 0 || to < 0) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalRange_2, from, to));
        }
        if (replacement == null) {
            from += replaceIndex;
            to   += replaceIndex;
            final int count = steps.size();
            if (from >= 0 && to < count) {
                for (int i = from; ++i < to;) {
                    if (i != replaceIndex && steps.get(i) instanceof ConcatenatedTransform) {
                        // Intermediate concatenated transforms (other than at the extremities) are not supported.
                        return false;
                    }
                }
                // Adjustment needed if a neighbor transform shall be only partially replaced.
                if (from < replaceIndex) concatenation = replaceStep(steps.get(from), concatenation, 1);
                if (to   > replaceIndex) concatenation = replaceStep(steps.get(to),   concatenation, 0);
                while (--from >= 0)  concatenation = concatenate(steps.get(from), concatenation);
                while (++to < count) concatenation = concatenate(concatenation, steps.get(to));
                replacement = concatenation;
                return true;
            }
        }
        return false;
    }

    /**
     * Replaces the first step or last step in a potentially concatenated transform.
     * The transform replaced by this method must be consistent with the transform
     * returned by {@link #getTransform(int)}.
     *
     * @param  step           the step to replace.
     * @param  concatenation  the replacement for the given step.
     * @param  firstOrLast    0 for replacing the first transform, or 1 for replacing the last transform.
     * @return the transform with the replacement applied.
     * @throws FactoryException if an error occurred during transform concatenation.
     */
    private MathTransform replaceStep(final MathTransform step, MathTransform concatenation, final int firstOrLast)
            throws FactoryException
    {
        if (step instanceof ConcatenatedTransform) {
            final var c = (ConcatenatedTransform) step;
            final var tr = new MathTransform[] {c.transform1, c.transform2};
            tr[firstOrLast] = replaceStep(tr[firstOrLast], concatenation, firstOrLast);
            concatenation = concatenate(tr[0], tr[1]);
        }
        return concatenation;
    }

    /**
     * Tries to replace a <var>forward</var> → <var>middle</var> → <var>inverse</var> chain by a new transform.
     * The transform that determines whether a replacement is possible is the <var>middle</var> transform,
     * and its relative index is given by the {@code middle} argument in this method.
     * There is two main scenarios:
     *
     * <ul>
     *   <li>If {@code middle} is +1, then <var>forward</var> is the transform at relative index 0
     *       and <var>inverse</var> is the transform at relative index 2.</li>
     *   <li>If {@code middle} is -1, then <var>forward</var> is the transform at relative index -2
     *       and <var>inverse</var> is the transform at relative index 0.</li>
     * </ul>
     *
     * If <var>inverse</var> is the {@linkplain #inverse() inverse} of <var>forward</var>, then this method invokes
     * {@code mapper.apply(middle)} where <var>middle</var> is the transform between the forward and inverse steps.
     * If the mapper returns a non-null value, then that value replaces the three steps of above-cited chain.
     *
     * <h4>Example</h4>
     * In a chain of <var>Reverse Mercator</var> → <var>Longitude rotation</var> → <var>Forward Mercator</var>
     * transforms where the forward and reverse Mercator steps operate on coordinates in radians on an ellipsoid
     * having a semi-major axis length of one (this is how Apache <abbr>SIS</abbr> implements map projections),
     * then because these Mercator steps just pass-through the longitude value unchanged and without using them
     * in calculation, these three steps can be replaced by the <var>Longitude rotation</var> step alone
     * (it is mathematically equivalent because of the radian units and the semi-major axis length of 1).
     *
     * @param  middle  relative index of the transform which is potentially a middle step. Value can be -1 or +1 for
     *                 the step before or after the transform on which {@code tryConcatenate(…)} has been invoked.
     * @param  mapper  a function receiving the middle transform in argument and returning the replacement
     *                 for the <var>forward</var> → <var>middle</var> → <var>inverse</var> chain.
     *                 May return {@code null} if the replacement cannot be done.
     * @return whether the replacement has been done as a result of this method call.
     * @throws FactoryException if the mapped transform is not a valid replacement.
     */
    public boolean replaceRoundtrip(final int middle, final UnaryOperator<MathTransform> mapper) throws FactoryException {
        if (Math.abs(middle) == 1) {
            final int indexOfInverse = middle * 2;
            final MathTransform inverse = getTransform(indexOfInverse).orElse(null);
            if (inverse != null && isInverseEquals(steps.get(replaceIndex), inverse)) {
                final MathTransform concatenation = mapper.apply(getTransform(middle).get());
                if (concatenation != null) {
                    // The transform at index `middle` is a concatenation of the real middle with `inverse`.
                    return replace(indexOfInverse, concatenation);
                }
            }
        }
        return false;
    }

    /**
     * Tries to simplify the transform chain when some coordinates are passed-through the transform at index 0.
     * A "pass-through" coordinate is an input coordinate which is copied in the output tuple by the transform
     * with <em>no change</em> and <em>without using this coordinate value in calculation</em>. In such cases,
     * for any {@code MathTransform} that use or modify this coordinate value immediately before or after the
     * transform at relative index 0, it does not matter if the order of those operations is modified.
     *
     * <p>{@link AbstractMathTransform#tryConcatenate(TransformJoiner)} Implementations can invoke this method
     * with a map containing the zero-based indexes of the dimensions that are passed-through by the transform
     * on which {@code tryConcatenate(…)} has been invoked. Each map key is the index of a pass-through coordinate
     * in source tuples, while the associated map value is the index for the same coordinate but in target tuples.
     * In the common case where the transform does not change the coordinate order,
     * the keys and the values are the same numbers.</p>
     *
     * <h4>Example</h4>
     * In a <q>spherical to geographic</q> conversion using a biaxial ellipsoid, the longitude value is a
     * pass-through coordinate because the <em>spherical</em> longitude and the <em>geodetic</em> longitude
     * are the same value. Since the implementation of {@link EllipsoidToCentricTransform} does not use the
     * longitude value in its calculation (in spherical case only), it does not matter if longitudes values
     * are converted from degrees to something else before or after the spherical to geographic conversion.
     * By invoking the following method, unit conversion of longitude values (at dimension 0) may be moved
     * before or after the transform if this method detects that such move would simplify the transform chain:
     *
     * {@snippet lang="java" :
     *     @Override
     *     protected void tryConcatenate(TransformJoiner context) throws FactoryException {
     *         if (!joiner.replacePassThrough(Map.of(0, 0))) {
     *             super.tryConcatenate(joiner);
     *         }
     *     }
     *     }
     *
     * @param  dimensions  the pass-through dimensions in source tuples (map keys) and target tuples (map values).
     * @return whether the transform chain has been modified as a result of this method call.
     * @throws FactoryException if the concatenation is not a valid replacement.
     * @throws IllegalArgumentException if a value is repeated twice in the given map.
     */
    public boolean replacePassThrough(Map<Integer,Integer> dimensions) throws FactoryException {
        Matrix before, after;
        if (!isAffine(before = getMatrix(-1)) || !isAffine(after = getMatrix(+1))) {
            return false;
        }
        /*
         * Add the lower-right corner of the matrix as a pass-through dimension.
         * That corner is not a coordinate dimension but exists in the matrix.
         * In an affine transform, the last row is always [0 0 … 0 1], therefore
         * the 1 value can be seen as passing-through.
         */
        final var sourceToTarget = new LinkedHashMap<>(dimensions);
        final var targetToSource = new LinkedHashMap<Integer,Integer>();
        final MathTransform tr   = steps.get(replaceIndex);
        sourceToTarget.put(tr.getSourceDimensions(),
                           tr.getTargetDimensions());   // For the last row and the translation column in matrices.
        for (final var it = sourceToTarget.entrySet().iterator(); it.hasNext();) {
            final Map.Entry<Integer, Integer> entry = it.next();
            final Integer source = entry.getKey();
            final Integer target = entry.getValue();
            if (targetToSource.put(target, source) != null) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.DuplicatedNumber_1, target));
            }
            // See the "Invariants: When `moveFromBeforeToAfter` = true" comment below in this method.
            if (source >= before.getNumCol()) {
                it.remove();
            }
        }
        /*
         * Modify the maps for keeping only the dimensions that we can move. Then, choose which matrix
         * to partially simplify in order to get a matrix as close as possible to an identity matrix:
         * If there is no obvious gain in changing a matrix, conservatively do nothing.
         */
        int typeIfSimplifyBefore = removeInvalidPassThrough(sourceToTarget, before, true);
        int typeIfSimplifyAfter  = removeInvalidPassThrough(targetToSource, after, false);
        int typeOfSimplestMatrix = Math.min(typeIfSimplifyBefore >>> Short.SIZE, typeIfSimplifyAfter >>> Short.SIZE);
        typeIfSimplifyBefore &= Short.MAX_VALUE;
        typeIfSimplifyAfter  &= Short.MAX_VALUE;
        if (Math.min(typeIfSimplifyBefore, typeIfSimplifyAfter) >= typeOfSimplestMatrix) {
            /*
             * it is important to be conservative, otherwise we may fall in a never-ending loop
             * where each invocation of the method reverts what the previous invocation did.
             */
            return false;
        }
        final boolean moveFromBeforeToAfter;
        if (typeIfSimplifyBefore != typeIfSimplifyAfter) {
            moveFromBeforeToAfter = typeIfSimplifyBefore < typeIfSimplifyAfter;
        } else {
            /*
             * No winner. Move from largest matrix to smallest matrix on the assumption that if,
             * after other optimizations, it become possible to eliminate one of the two matrices,
             * there is more benefits in eliminating the largest one. Also, there is more chances
             * that two terms get combined in a single term when moving to the smallest matrix.
             */
            moveFromBeforeToAfter = after.getNumRow() * after.getNumCol() <
                                   before.getNumRow() * before.getNumCol();
        }
        dimensions = moveFromBeforeToAfter ? sourceToTarget : targetToSource;
        if (dimensions.isEmpty()) {
            return false;
        }
        /*
         * Creates a copy of the matrix which will contain the remaining elements after the moves.
         * Create an initially empty matrix which will contain the elements that have been moved.
         * Invariants:
         *
         *   When `moveFromBeforeToAfter` = true:
         *   Simplify `before` and will prepend `moved` before `after`.
         *       simplified.numRow  =  before.numRow  ≥  (srcRow = dimensions.key)       ⟶ SAFE
         *       simplified.numCol  =  before.numCol        (unrelated to keys)          ⟶ UNSAFE
         *            moved.numRow  =   after.numcol  ≥  (tgtRow = dimensions.values)    ⟶ SAFE
         *            moved.numCol  =   after.numcol  ≥  (tgtCol = dimensions.values)    ⟶ SAFE
         *
         *   When `moveFromBeforeToAfter` = false:
         *   Simplify `after` and will append `moved` after `before`.
         *       simplified.numRow  =   after.numRow        (unrelated to keys)          ⟶ UNSAFE
         *       simplified.numCol  =   after.numCol  ≥  (srcCol = dimensions.keys)      ⟶ SAFE
         *            moved.numRow  =  before.numRow  ≥  (tgtRow = dimensions.values)    ⟶ SAFE
         *            moved.numCol  =  before.numRow  ≥  (tgtCol = dimensions.values)    ⟶ SAFE
         *
         * The unsafe case of the latter is excluded in the following loop.
         * The unsafe case of the former was handled in code above by removing the keys that are too large.
         */
        final MatrixSIS simplified = Matrices.copy(moveFromBeforeToAfter ? before : after);
        final int dim = moveFromBeforeToAfter ? after.getNumCol() : before.getNumRow();
        final MatrixSIS moved = Matrices.create(dim, dim, ExtendedPrecisionMatrix.CREATE_IDENTITY);
        for (final Map.Entry<Integer,Integer> entry : dimensions.entrySet()) {
            final int srcRow = entry.getKey();
            final int tgtRow = entry.getValue();
            moved.setElement(tgtRow, tgtRow, 0);    // Remove the 1 on the diagonal for that row.
            /*
             * See the comment in `removeInvalidPassThrough(…)` for an explanation about why we need this check.
             * Note: when `moveFromBeforeToAfter` is false (which is the only case where we need bound check),
             * `simplified` is of the size of `after`.
             */
            if (moveFromBeforeToAfter || srcRow < simplified.getNumRow()) {
                dimensions.forEach((srcCol, tgtCol) -> {
                    moved.setNumber(tgtRow, tgtCol, simplified.getElement(srcRow, srcCol));
                });
                // Simplify the row only after all elements have been copied.
                for (int i = simplified.getNumCol(); --i >= 0;) {
                    simplified.setElement(srcRow, i, (i == srcRow) ? 1 : 0);
                }
            }
        }
        /*
         * Rebuild the transform after the rows have been moved. We must do the matrix multiplication
         * in this method instead of relying on `createConcatenatedTransform(…)` doing that for us in
         * order to avoid never-ending recursive calls to this method.
         */
        if (moveFromBeforeToAfter) {
            before = simplified;
            after  = Matrices.multiply(after, moved);
        } else {
            after  = simplified;
            before = Matrices.multiply(moved, before);
        }
        return replace(-1, +1, concatenate(    factory.createAffineTransform(before),
                               concatenate(tr, factory.createAffineTransform(after))));
    }

    /**
     * Removes all "pass-through" dimensions which depend on dimensions that are not themselves pass-through.
     * This method modifies the given {@code dimensions} map by removing the entries that cannot be used.
     * Returns an arbitrary measurement of how far the modified matrix would be from an identity transform.
     *
     * <p>The return value contains the matrix type both before simplification (in highest bits)
     * and after simplification (in lowest bits).</p>
     *
     * @param  dimensions  dimensions to pass through as keys. The associated map values are ignored.
     * @param  matrix      the matrix that the caller wants to move if possible and useful.
     * @return how far the modified matrix would be from an identity transform. Lower is better.
     */
    private static int removeInvalidPassThrough(final Map<Integer,Integer> dimensions, final Matrix matrix,
                                                final boolean moveFromBeforeToAfter)
    {
        /*
         * Remove all dimensions that depend on a non-pass-through dimension.
         * If a dimension has been removed, then we need to rerun the loop
         * because it may invalidate previously accepted dimensions.
         */
        boolean again;
        do {
            again = false;
            for (final Iterator<Integer> it = dimensions.keySet().iterator(); it.hasNext();) {
                final int srcRow = it.next();
                if (!(moveFromBeforeToAfter || srcRow < matrix.getNumRow())) {
                    /*
                     * Use case: the matrix after the transform reduces the number of dimensions.
                     * In such case, `transform` has more rows than `after`. Since any row can be
                     * a pass-through dimension, it is not an error if `j` is out of matrix bounds.
                     * This is because the requirement for `after` is that the column exists, not the row.
                     * This is the opposite of `before`, for which the requirement is that the row exists.
                     * The latter is verified by the `matrix.getElement(j, i)` call. We do not remove the
                     * entry from the map because it still a valid pass-through dimension.
                     */
                    continue;
                }
                for (int srcCol = matrix.getNumCol(); --srcCol >= 0;) {
                    /*
                     * Note: all matrices below are shown in execution order, not in multiplication order.
                     *
                     * MOVE AFTER TO BEFORE: `matrix = after` — example with `targetToSource = (1, 0)`
                     * The row shown in `matrix` can be moved because it depends only on target dimension 1,
                     * which is pass-through. The target dimension 1 become source dimension 0 in this example.
                     *
                     * before     moved    transform     matrix
                     * ┌     ┐   ┌     ┐     ╔════╗     ┌     ┐
                     * │     │ ← │ m 0 │  ←  ╟ 🯓  ║     │     │
                     * │     │   │     │     ║  🯒 ╢  ←  │ 0 m │
                     * └     ┘   └     ┘     ╚════╝     └     ┘
                     *
                     * MOVE BEFORE TO AFTER: `matrix = before` — example with `sourceToTarget = (0, 1)`
                     * The row shown in `matrix` can be moved because, after the move, it will be used
                     * only by source dimension 0, which is pass-through. The source dimension 0 will
                     * become target dimension 1 after the move in this example.
                     *
                     * matrix    transform     moved     after
                     * ┌     ┐     ╔════╗     ┌     ┐   ┌     ┐
                     * │ m 0 │  →  ╟ 🯓  ║     │     │   │     │
                     * │     │     ║  🯒 ╢  →  │ 0 m │ → │     │
                     * └     ┘     ╚════╝     └     ┘   └     ┘
                     *
                     * AXIS SWAPPING
                     * Note that in the case of anti-diagonal matrix, this algorithm will usually fail to detect
                     * that a dimension could pass-through. This is okay because even if we could move the rows,
                     * we would still need a matrix doing the axis swapping anyway.
                     */
                    final double value = matrix.getElement(srcRow, srcCol);
                    if (!(value == 0 || Double.isNaN(value) || dimensions.containsKey(srcCol))) {
                        it.remove();
                        again = true;
                        break;          // Move to next row.
                    }
                }
            }
        } while (again);
        /*
         * Arbitrary measure of how far the matrix would be from an identity matrix.
         * The current implementation classifies the matrix in the following types:
         *
         *   0: Identity. This is the ultimate goal, always having precedence over all other types.
         *   1: Change of axis order only.
         *   2: Translation only.
         *   3: Translation + change of axis order.
         *   4: Scale only.
         *  ≥5: Generic case.
         */
        int typeOfOriginalMatrix   = 0;
        int typeOfSimplifiedMatrix = 0;
        final int translationColumn = matrix.getNumCol() - 1;
        for (int j = matrix.getNumRow(); --j >= 0;) {
            for (int i = translationColumn; i >= 0; i--) {
                final double element = matrix.getElement(j, i);
                if (element != ((i == j) ? 1 : 0)) {
                    final int type;
                    if (i == translationColumn) {
                        type = 2;   // Translation
                    } else if (element == 0 || element == 1) {
                        type = 1;   // Change of axis order
                    } else if (i == j) {
                        type = 4;   // Scale
                    } else {
                        type = 5;   // Scale + change of coordinate order
                    }
                    typeOfOriginalMatrix |= type;
                    if (!dimensions.containsKey(j)) {
                        typeOfSimplifiedMatrix |= type;
                    }
                }
            }
        }
        return (typeOfOriginalMatrix << Short.SIZE) | typeOfSimplifiedMatrix;
    }

    /**
     * Creates a transform by concatenating two existing transforms.
     * This is a shortcut for {@code factory.createConcatenatedTransform(tr1, tr2)},
     * except that this method reuse existing concatenated transform when possible.
     *
     * <p>Note that it is not always useful to invoke this method. For example, it will usually not bring any benefit
     * compared to direct use of the {@linkplain #factory} if one of the arguments is a newly created transform.</p>
     *
     * @param  tr1  the first transform to apply to points.
     * @param  tr2  the second transform to apply to points.
     * @return the concatenated transform, as an existing instance if possible.
     * @throws FactoryException if the transform creation failed.
     */
    final MathTransform concatenate(final MathTransform tr1, final MathTransform tr2) throws FactoryException {
        /*
         * The `pool` array is very short, usually 0, 1 or 2 elements. Therefore, it is not worth using an hash map.
         * Identity comparisons are okay because the typical situation for reuse is when the caller changed nothing.
         */
        for (final ConcatenatedTransform tr : pool) {
            if (tr.transform1 == tr1 && tr.transform2 == tr2) {
                return tr;
            }
        }
        return factory.createConcatenatedTransform(tr1, tr2);
    }

    /**
     * Restores the original {@link ConcatenatedTransform} instances in the list of steps.
     */
    final void reassemble() {
        boolean again;
        do {
            again = false;
            MathTransform tr2 = null;
            for (int i = steps.size(); --i >= 0;) {
                MathTransform tr1 = steps.get(i);
                for (final ConcatenatedTransform tr : pool) {
                    if (tr.transform1 == tr1 && tr.transform2 == tr2) {
                        steps.remove(i+1);
                        steps.set(i, tr);
                        tr1 = tr;
                        again = true;
                        break;
                    }
                }
                tr2 = tr1;
            }
        } while (again);
    }

    /**
     * Applies generic simplification rules on the sequence of steps.
     * The {@link #steps} list is modified in-place, which may invalidate the {@link #replaceIndex} index.
     * The method tries to:
     *
     * <ul>
     *   <li>Remove all {@linkplain MathTransform#isIdentity() identity} transforms.</li>
     *   <li>Remove pairs of transforms where the first transform is immediately followed by its inverse.</li>
     *   <li>Replace sequences of consecutive linear transforms by a single linear transform defined by a matrix
     *       which is the product of the matrix of the original transforms.</li>
     * </ul>
     *
     * @return whether the {@link #steps} list has changed as a result of this method call.
     * @throws FactoryException if an error occurred when creating the new transforms.
     */
    final boolean simplify() throws FactoryException {
        boolean changed = false, again;
        do {
            again = false;
            steps.removeIf(MathTransform::isIdentity);      // Trivial but actually essential for this method.
            /*
             * If two consecutive transforms are the inverse of each other, remove that pair.
             * We need to test this case before the linear transform case in the next loop,
             * because the matrices may contain NaN values.
             */
            for (int i = steps.size() - 1; --i >= 0;) {
                final MathTransform tr1 = steps.get(i);
                final MathTransform tr2 = steps.get(i+1);
                if (isInverseEquals(tr1, tr2) || isInverseEquals(tr2, tr1)) {
                    assert tr1.getSourceDimensions() == tr2.getTargetDimensions();
                    assert tr1.getTargetDimensions() == tr2.getSourceDimensions();
                    steps.subList(i, i+2).clear();
                    again = true;
                    i--;        // Because element at index i has also been removed.
                }
            }
            /*
             * If two consecutive transforms use matrix, create a single transform set to the matrix product.
             */
            for (int i = steps.size() - 1; --i >= 0;) {
                final MathTransform tr1 = steps.get(i);
                final MathTransform tr2 = steps.get(i+1);
                final MathTransform concatenated = multiply(tr1, tr2);
                if (concatenated != null) {
                    if (concatenated instanceof AbstractLinearTransform) {
                        final var impl = (AbstractLinearTransform) concatenated;
                        /*
                         * Following code computes the inverse of `concatenated` transform. In principle, this is not
                         * necessary because `AbstractLinearTransform.inverse()` inverses the matrix when first needed.
                         * However, if the matrices are not square (e.g. if a transform is dropping a dimension), some
                         * information may be lost. By computing the inverse transform now as the product of matrices
                         * provided by the two inverse transforms (as opposed to inverting the product of the matrices
                         * of the forward transform), we use information that would otherwise be lost. For example,
                         * the inverse of the transform dropping a dimension may be a transform setting that dimension
                         * to a constant value. Therefore, the inverse transform computed here may have real values for
                         * coefficients that `AbstractLinearTransform.inverse()` would have set to NaN, or may succeed
                         * where `AbstractLinearTransform.inverse()` would have throw an exception.
                         */
                        if (impl.inverse == null) try {
                            final MathTransform inverse = multiply(tr2.inverse(), tr1.inverse());
                            if (inverse instanceof LinearTransform) {
                                impl.inverse = (LinearTransform) inverse;
                            }
                        } catch (NoninvertibleTransformException e) {
                            /*
                             * Ignore. The `concatenated.inverse()` method will try to compute the inverse itself,
                             * possible at the cost of more NaN values than what above code would have produced.
                             */
                            recoverableException(e);
                        }
                    }
                    steps.set(i, concatenated);
                    steps.remove(i+1);
                    again = true;
                }
            }
            changed |= again;
        } while (again);
        return changed;
    }

    /**
     * Returns a transform resulting from the multiplication of the matrices of the given transforms.
     * If a given transform is null or does not provide matrix, then this method returns {@code null}.
     */
    private MathTransform multiply(final MathTransform tr1, final MathTransform tr2) throws FactoryException {
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
                 * shifts are very small. The shift may be of the same order of magnitude as the tolerance threshold.
                 * Instead, Apache SIS performs matrix operations using double-double arithmetic in the hope to get
                 * exact results at the `double` accuracy.
                 */
                return factory.createAffineTransform(matrix);
            }
        }
        // No optimized case found.
        return null;
    }

    /**
     * Returns whether the last row contains only zeros except the last element which shall be 1.
     * This method differs from {@link Matrices#isAffine(Matrix)} in that it returns {@code false}
     * if the given argument is {@code null} and does not require the matrix to square.
     */
    private static boolean isAffine(final Matrix matrix) {
        if (matrix == null) {
            return false;
        }
        double e = 1;
        final int j = matrix.getNumRow() - 1;
        for (int i = matrix.getNumCol(); --i >= 0;) {
            if (matrix.getElement(j, i) != e) {
                return false;
            }
            e = 0;
        }
        return true;
    }

    /**
     * Returns {@code true} if {@code tr1} is the inverse of {@code tr2}.
     * If this method is unsure, it conservatively returns {@code false}.
     * The transform that may be inverted is {@code tr1}.
     *
     * @param  tr1  the transform to inverse.
     * @param  tr2  the transform that may be the inverse of {@code tr1}.
     * @return whether this transform is the inverse of the given transform. If unsure, {@code false}.
     */
    private static boolean isInverseEquals(MathTransform tr1, final MathTransform tr2) {
        if (tr1.getSourceDimensions() != tr2.getTargetDimensions() ||
            tr1.getTargetDimensions() != tr2.getSourceDimensions())
        {
            return false;
        }
        if (MathTransforms.isLinear(tr1) != MathTransforms.isLinear(tr2)) {
            // For avoiding creation of inverse transform for a result that should be false.
            return false;
        }
        try {
            tr1 = tr1.inverse();
        } catch (NoninvertibleTransformException e) {
            recoverableException(e);
            return false;
        }
        if (tr1 == tr2) {
            return true;
        }
        if (tr1 instanceof LenientComparable) {
            return ((LenientComparable) tr1).equals(tr2, ComparisonMode.APPROXIMATE);
        }
        if (tr2 instanceof LenientComparable) {
            return ((LenientComparable) tr2).equals(tr1, ComparisonMode.APPROXIMATE);
        }
        return tr1.equals(tr2);
    }

    /**
     * Logs a warning about an exception that can be ignored. The record declares {@link #simplify()}
     * as the source because this is the method where the warning indirectly occurred.
     */
    private static void recoverableException(final NoninvertibleTransformException e) {
        Logging.recoverableException(AbstractMathTransform.LOGGER, TransformJoiner.class, "simplify", e);
    }
}
