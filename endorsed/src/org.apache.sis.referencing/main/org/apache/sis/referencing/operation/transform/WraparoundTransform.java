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

import java.util.Objects;
import java.util.HashMap;
import java.util.function.Function;
import java.io.Serializable;
import org.opengis.util.FactoryException;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.provider.Wraparound;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.parameter.Parameters;


/**
 * Enforces coordinate values in the range of a wraparound axis (typically longitude).
 * For example, this transform can shift longitudes from the [0 … 360]° range to the [-180 … +180]° range.
 * The destination range is centered at 0 with a minimal value of −{@link #period}/2 and a maximal value
 * of {@link #period}/2. For a range centered on a different value,
 * a {@linkplain MathTransforms#translation(double...) translation}
 * can be applied before and after the {@code WraparoundTransform}.
 *
 * <h2>Instantiation</h2>
 * {@code WraparoundTransform}s are not created automatically by
 * {@link org.apache.sis.referencing.CRS#findOperation CRS.findOperation(…)}
 * because they introduce discontinuities in coordinate transformations.
 * Such discontinuities are hurtless when transforming only a cloud of points,
 * but produce undesirable artifacts when transforming envelopes or geometries.
 * Callers need to create {@code WraparoundTransform} instances explicitly if discontinuities are acceptable.
 *
 * <h2>Subclassing</h2>
 * In order to control the discontinuity problem, it may be necessary to subclass {@code WraparoundTransform}
 * and override the {@link #shift(double)} method. For example, a subclass may control the wraparounds in a way
 * to prevent the {@linkplain org.apache.sis.geometry.AbstractEnvelope#getLowerCorner() lower corner} of an envelope
 * to become greater than the {@linkplain org.apache.sis.geometry.AbstractEnvelope#getUpperCorner() upper corner}.
 *
 * <h2>Inverse transform</h2>
 * The {@link #inverse()} method can return another {@code WraparoundTransform} with the same {@linkplain #period}
 * but {@linkplain #sourceMedian centered on a different value}, which must be specified at construction time.
 * For example if this transform is converting from <i>something</i> to [-180 … +180]° range,
 * then inverse transform is possible only if <i>"something"</i> has been specified
 * (it may be the [0 … 360]° range, but not necessarily).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @see org.apache.sis.geometry.Envelopes#transformWithWraparound(MathTransform, Envelope)
 *
 * @since 1.1
 */
public class WraparoundTransform extends AbstractMathTransform implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1959034793759509170L;

    /**
     * Number of dimensions of source and target coordinates.
     */
    private final int dimension;

    /**
     * The dimension where to apply wraparound.
     */
    public final int wraparoundDimension;

    /**
     * Period on wraparound axis, always greater than zero. This is 360° for the longitude axis.
     * Coordinates will be normalized in the [−<var>period</var>/2 … +<var>period</var>/2] range.
     */
    public final double period;
    /*
     * DESIGN NOTE:
     * A previous version of `WraparoundTransform` had no period. Instead, it was expecting coordinates normalized
     * in the [0 … 1] range. Coordinates in [0 … 360]° range were divided by 360 using an affine transforms before
     * `WraparoundTransform` and multiplied by 360 using another affine transform after `WraparoundTransform`.
     * That approach allowed to delegate more work to the affine transforms which can efficiently be combined
     * with other affine transforms, but it caused more rounding errors.
     */

    /**
     * Coordinate in the wraparound dimension which is at the center of the range of valid source coordinates.
     * For example if this transform wraps coordinates from the [0 … 360]° range to the [-180 … +180]° range,
     * then {@code sourceMedian} should be 180° (the value at the center of [0 … 360]° range).
     * The value may be {@link Double#NaN} if unknown.
     *
     * <p>This field is used for inverse transforms only. It has no effect on the forward transforms.
     * If not NaN, this value is used for building the transform returned by {@link #inverse()}.</p>
     *
     * <h4>Design note</h4>
     * There is no {@code targetMedian} field because the target median is fixed to 0 in {@code WraparoundTransform}.
     * Non-zero target medians are implemented by {@linkplain MathTransforms#translation(double...) translations}
     * applied before and after {@code WraparoundTransform}. Because of this translation, the value of this field
     * is related to the arguments given to the {@link #create create(…)} method by
     * {@code this.sourceMeridian = sourceMeridian - targetMeridian}.
     */
    public final double sourceMedian;

    /**
     * Inverse of this transform, computed when first needed.
     *
     * @see #inverse()
     */
    private transient volatile MathTransform inverse;

    /**
     * Creates a new transform with a wraparound behavior in the given dimension.
     * Output values in the wraparound dimension will be in the [−p/2 … +p/2] range
     * where <var>p</var> is the period (e.g. 360°).
     *
     * @param  dimension            number of dimensions of source and target coordinates.
     * @param  wraparoundDimension  the dimension where to apply wraparound.
     * @param  period               period on wraparound axis.
     * @param  sourceMedian         coordinate at the center of the range of valid source coordinates, or NaN if unknown.
     *                              This argument is used for inverse transforms only (ignored in forward transforms).
     *
     * @see #create(int, int, double, double, double)
     */
    protected WraparoundTransform(final int dimension, final int wraparoundDimension, final double period, final double sourceMedian) {
        this.dimension           = dimension;
        this.wraparoundDimension = wraparoundDimension;
        this.period              = period;
        this.sourceMedian        = sourceMedian;
    }

    /**
     * Creates a new transform with the same parameters as the given transform.
     * This constructor can be used by subclasses applying the same wraparound as
     * an existing transform but with a different {@link #shift(double)} implementation.
     *
     * @param  other  the other transform from which to copy the parameters.
     */
    protected WraparoundTransform(final WraparoundTransform other) {
        dimension           = other.dimension;
        wraparoundDimension = other.wraparoundDimension;
        period              = other.period;
        sourceMedian        = other.sourceMedian;
        inverse             = other.inverse;
    }

    /**
     * Returns an instance with the number of dimensions compatible with the given matrix,
     * while keeping {@link #wraparoundDimension}, {@link #period} and {@link #sourceMedian} unchanged.
     * If no instance can be created for the given number of dimensions, then this method returns {@code null}.
     *
     * @param  middle       matrix defining a transform which will be applied before or after {@code this}.
     * @param  linearFirst  whether the transform defined by the matrix will be applied before {@code this}.
     */
    private WraparoundTransform redimension(final boolean linearFirst, final Matrix middle) {
        final int n = (linearFirst ? middle.getNumRow() : middle.getNumCol()) - 1;
        if (n == dimension) {
            return this;
        }
        if (n < wraparoundDimension && getClass() == WraparoundTransform.class) {
            return new WraparoundTransform(n, wraparoundDimension, period, sourceMedian);
        }
        return null;
    }

    /**
     * Returns a transform with a wraparound behavior in the given dimension.
     * Output values in the wraparound dimension will be in the [t−p/2 … t+p/2] range
     * where <var>t</var> is the target median and <var>p</var> is the period (typically 360° for longitude axis).
     *
     * <p>The {@code sourceMedian} argument is optional (can be {@link Double#NaN} if unknown) and has no effect on
     * the forward transform. This argument is used only for creating the {@linkplain #inverse() inverse} transform.</p>
     *
     * <h4>Examples</h4>
     * <ul>
     *   <li>Wraparound longitudes in (φ,λ) coordinates from [-180 … +180]° range to [0 … 360]° range:
     *       {@code create(2, 0, 360, 0, 180)}.</li>
     *   <li>Wraparound longitudes in (φ,λ,h) coordinates from unknown range to [-180 … +180]° range:
     *       {@code create(3, 0, 360, Double.NaN, 0)} (non-invertible).</li>
     * </ul>
     *
     * @param  dimension            number of dimensions of the transform to create.
     * @param  wraparoundDimension  dimension where wraparound happens.
     * @param  period               period on wraparound axis.
     * @param  sourceMedian         coordinate at the center of the range of valid source coordinates, or NaN if unknown.
     * @param  targetMedian         coordinate at the center of the range of valid target coordinates.
     * @return the wraparound transform.
     */
    public static MathTransform create(final int dimension, final int wraparoundDimension, final double period,
                                       final double sourceMedian, final double targetMedian)
    {
        ArgumentChecks.ensureStrictlyPositive("dimension", dimension);
        ArgumentChecks.ensureBetween("wraparoundDimension", 0, dimension - 1, wraparoundDimension);
        ArgumentChecks.ensureStrictlyPositive("period", period);
        ArgumentChecks.ensureFinite("targetMedian", targetMedian);
        /*
         * Since we are going to apply a `-targetMedian` translation before the `WraparoundTransform`, the
         * `sourceMedian` used for computing the inverse of that transform must have the same translation.
         */
        final var tr = new WraparoundTransform(dimension, wraparoundDimension, period, sourceMedian - targetMedian);
        if (targetMedian == 0) {
            return tr;
        } else try {
            final var vector = new double[dimension];
            vector[wraparoundDimension] = targetMedian;
            final MathTransform denormalize = MathTransforms.translation(vector);
            final MathTransform ct = MathTransforms.concatenate(denormalize.inverse(), tr, denormalize);
            if (sourceMedian == 0) {
                tr.inverse = tr;        // For preventing a stack overflow in `DomainDefinition.estimateOnInverse(…)`
            }
            return ct;
        } catch (NoninvertibleTransformException e) {
            throw new IllegalArgumentException(e);              // Should never happen actually.
        }
    }

    /**
     * Replaces all {@code WraparoundTransform} instances in a chain of transform steps.
     * For each instance found in the {@linkplain MathTransforms#getSteps(MathTransform) list of transform steps},
     * the given function is invoked with the {@code WraparoundTransform} instance found. If that function returns
     * a different instance, then this method creates a new chain of transforms with the same steps as the given
     * {@code transform}, except for the {@code WraparoundTransform} steps that are replaced by the steps returned
     * by the {@code replacement} function.
     *
     * <p>This method allows injection of a specialized type of {@code WraparoundTransform}, for example in order
     * to override the {@link #shift(double)} method with finer control of wraparound operations.</p>
     *
     * @param  transform    the transform in which to replace {@link WraparoundTransform} steps.
     * @param  replacement  function providing replacements for {@code WraparoundTransform} steps.
     * @return chain of transforms with {@link WraparoundTransform} steps replaced (if any).
     */
    public static MathTransform replace(MathTransform transform,
            final Function<? super WraparoundTransform, ? extends WraparoundTransform> replacement)
    {
        ArgumentChecks.ensureNonNull("transform",   transform);
        ArgumentChecks.ensureNonNull("replacement", replacement);
        if (transform instanceof ConcatenatedTransform) {
            final var ct = (ConcatenatedTransform) transform;
            final MathTransform tr1 = replace(ct.transform1, replacement);
            final MathTransform tr2 = replace(ct.transform2, replacement);
            if (tr1 != ct.transform1 || tr2 != ct.transform2) {
                transform = MathTransforms.concatenate(tr1, tr2);
            }
        } else if (transform instanceof WraparoundTransform) {
            // Tested last because less frequent (most often, does not happen at all).
            transform = Objects.requireNonNull(replacement.apply((WraparoundTransform) transform));
        }
        return transform;
    }

    /**
     * Gets the dimension of input points.
     *
     * @return the dimension of input points.
     */
    @Override
    public final int getSourceDimensions() {
        return dimension;
    }

    /**
     * Gets the dimension of output points.
     *
     * @return the dimension of output points.
     */
    @Override
    public final int getTargetDimensions() {
        return dimension;
    }

    /**
     * Applies the wraparound on the given coordinate value. This method is invoked by default implementation
     * of all {@code transform(…)} methods defined in this {@code WraparoundTransform} class.
     * It provides a single method to override if a different wraparound strategy is desired.
     * The default implementation is:
     *
     * {@snippet lang="java" :
     *     return Math.IEEEremainder(x, period);
     *     }
     *
     * Subclasses may override this method for applying wraparound only under some conditions,
     * in order to reduce discontinuities.
     *
     * @param  x  the value on which to apply wraparound.
     * @return the value after wraparound.
     *
     * @see Math#IEEEremainder(double, double)
     */
    protected double shift(final double x) {
        return Math.IEEEremainder(x, period);
    }

    /**
     * Applies wraparounds on a single point and optionally computes the transform derivative at that location.
     * The default implementation delegates to {@link #shift(double)} and {@link #derivative(DirectPosition)}.
     */
    @Override
    public Matrix transform(final double[] srcPts, int srcOff,
                            final double[] dstPts, int dstOff, final boolean derivate)
    {
        if (dstPts != null) {
            System.arraycopy(srcPts, srcOff, dstPts, dstOff, dimension);
            dstOff += wraparoundDimension;
            dstPts[dstOff] = shift(dstPts[dstOff]);
        }
        return derivate ? derivative(null) : null;
    }

    /**
     * Transforms many positions in a sequence of coordinate tuples.
     * The default implementation delegates to {@link #shift(double)} for each point.
     */
    @Override
    public void transform(final double[] srcPts, int srcOff,
                          final double[] dstPts, int dstOff, int numPts)
    {
        System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * dimension);
        dstOff += wraparoundDimension;
        while (--numPts >= 0) {
            dstPts[dstOff] = shift(dstPts[dstOff]);
            dstOff += dimension;
        }
    }

    /**
     * Transforms many positions in a sequence of coordinate tuples.
     * The default implementation delegates to {@link #shift(double)} for each point.
     */
    @Override
    public void transform(final float[] srcPts, int srcOff,
                          final float[] dstPts, int dstOff, int numPts)
    {
        System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * dimension);
        dstOff += wraparoundDimension;
        while (--numPts >= 0) {
            dstPts[dstOff] = (float) shift(dstPts[dstOff]);
            dstOff += dimension;
        }
    }

    /**
     * Gets the derivative of this transform at a point.
     *
     * <h4>Mathematical note</h4>
     * Strictly speaking the derivative at (<var>n</var> + 0.5) × {@link #period} where <var>n</var> is an integer
     * should be infinite because the coordinate value jumps "instantaneously" from any value to ±{@link #period}/2.
     * However, in practice we use derivatives as linear approximations around small regions, not for calculations
     * requiring strict mathematical values. An infinite value goes against the approximation goal.
     * Furthermore, whether a source coordinate is an integer value or not is subject to rounding errors,
     * which may cause unpredictable behavior if infinite values were returned.
     *
     * @param  point  the position where to evaluate the derivative
     *                (ignored in default implementation, may be {@code null}).
     * @return transform derivative (identity matrix in default implementation).
     */
    @Override
    public Matrix derivative(final DirectPosition point) {
        return Matrices.createIdentity(dimension);
    }

    /**
     * Returns a wraparound transform producing values in the range of source coordinate values.
     * Output values in the wraparound dimension will be in the [s−p/2 … s+p/2] range
     * where <var>s</var> is {@link #sourceMedian} and <var>p</var> is {@link #period}.
     *
     * @return wraparound transform producing values in the range of source coordinate values.
     * @throws NoninvertibleTransformException if {@link #sourceMedian} is NaN or infinite.
     */
    @Override
    public MathTransform inverse() throws NoninvertibleTransformException {
        MathTransform tr = inverse;
        if (tr == null) {
            if (!Double.isFinite(sourceMedian)) {
                return super.inverse();
            } else if (sourceMedian == 0) {
                inverse = tr = this;
            } else synchronized (this) {
                tr = inverse;
                if (tr == null) {
                    tr = create(dimension, wraparoundDimension, period, 0, sourceMedian);
                    ConcatenatedTransform.setInverse(tr, this);
                    inverse = tr;
                }
            }
        }
        return tr;
    }

    /**
     * Concatenates in an optimized way this math transform with the given one, if possible.
     * If this method detects a chain of operations like below:
     *
     * <blockquote>[wraparound]  ⇄  [affine]  ⇄  [wraparound]</blockquote>
     *
     * Then this method tries to move the [affine] step before or after the [wraparound] step.
     * This move increases the chances that [affine] step is combined with other affine operations.
     * Only dimensions that do not depend on {@link #wraparoundDimension} can be moved.
     */
    @Override
    protected void tryConcatenate(final TransformJoiner context) throws FactoryException {
        int relativeIndex = +1;
        do {
            /*
             * If the neighbor transform is also a `WraparoundTransform` for the same dimension,
             * then there is no need to concatenate those two consecutive redudant transforms.
             * Keep the first transform because it will be the last one in inverse transform
             * (i.e., will have precedence).
             */
            final MathTransform neighbor = context.getTransform(relativeIndex).orElse(null);
            if (neighbor instanceof WraparoundTransform) {
                final var other = (WraparoundTransform) neighbor;
                final WraparoundTransform merged = (relativeIndex < 0) ? other.merge(this) : merge(other);
                if (merged != null && context.replace(relativeIndex, merged)) {
                    return;
                }
            }
            /*
             * Case where the other transform is affine and is located between two `WraparoundTransform`.
             * We will try to move the affine transform out of there.
             */
            final Matrix middle = MathTransforms.getMatrix(neighbor);
            if (middle != null) {
                final int firstOrLast = relativeIndex * 2;
                final MathTransform tr = context.getTransform(firstOrLast).orElse(null);
                if (tr instanceof WraparoundTransform) {
                    final var other = (WraparoundTransform) tr;
                    /*
                     * Found two `WraparoundTransform` instances that we may merge in a single one.
                     * But first, we need to move the linear transform that stands between the two.
                     * Determine in which direction we can move the linear transform: first or last.
                     */
                    final boolean linearFirst;
                    if (canMove(middle)) {
                        linearFirst = (relativeIndex >= 0);
                    } else if (other.canMove(middle)) {
                        linearFirst = (relativeIndex < 0);
                    } else {
                        continue;
                    }
                    /*
                     * Try to merge the two consecutive `WraparoundTransform` instances in a single one.
                     * If they cannot be merged, we will need to keep them as a concatenated transform.
                     * If they could be merged, we need to take in account that they may not have the
                     * same number of dimensions if the matrix between them was non-square.
                     */
                    WraparoundTransform merged = (relativeIndex < 0) ? other.merge(this) : merge(other);
                    if (merged != null) {
                        merged = merged.redimension(linearFirst, middle);
                    }
                    MathTransform concatenated = merged;
                    if (concatenated == null) {
                        concatenated = (relativeIndex >= 0) ? context.concatenate(this, other)
                                                            : context.concatenate(other, this);
                    }
                    concatenated = linearFirst ? context.concatenate(neighbor, concatenated)
                                               : context.concatenate(concatenated, neighbor);
                    if (context.replace(firstOrLast, concatenated)) {
                        return;
                    }
                }
            }
        } while ((relativeIndex = -relativeIndex) < 0);
        /*
         * If we reach this point, there is no two consecutive `WraparoundTransform` instances that we could simplify.
         * Maybe the two instances were separated by a linear transform but we could not move that step out of there.
         * However, even if we cannot move the linear transform fully, the `context.replacePassThrough(…)` method may
         * still be capable to simplify it.
         */
        final var dimensions = new HashMap<Integer, Integer>();
        for (int i=0; i<dimension; i++) {
            if (i != wraparoundDimension) {
                dimensions.put(i, i);
            }
        }
        if (!context.replacePassThrough(dimensions)) {
            super.tryConcatenate(context);
        }
    }

    /**
     * Tests if the given matrix can be moved before or after this transform.
     * If any matrix row (output coordinate) uses the wraparound dimension as input,
     * then we cannot move that row because the coordinate value may not be the same
     * after execution of {@code WraparoundTransform}.
     *
     * @todo Current implementation is conservative, checking both the row and column,
     *       because the matrix will be moved from before to after the wrap-around transform.
     *       We could do better: detect when a coordinate is moved, take the scale factor for
     *       adjusting the {@linkplain #period}.
     */
    private boolean canMove(final Matrix middle) {
        int i = middle.getNumCol();
        int j = middle.getNumRow();
        if (wraparoundDimension < i) {
            while (--j >= 0) {
                final double v = middle.getElement(j, wraparoundDimension);
                if (v != (j == wraparoundDimension ? 1 : 0)) {
                    return false;
                }
            }
            j = middle.getNumRow();
        }
        if (wraparoundDimension < j) {
            while (--i >= 0) {
                final double v = middle.getElement(wraparoundDimension, i);
                if (v != (i == wraparoundDimension ? 1 : 0)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * If this transform is redundant with the given transform, returns a single instance.
     * Otherwise, returns {@code null} for instructing the caller that it must keep the two transforms.
     * This transform must be before the {@code other} transform in the chain of transform steps.
     *
     * @todo Handle the case when a period is a multiple of the other.
     *
     * @param  other  the other transform. Must be after this transform in the chain of transform steps.
     * @return the transform to use instead of the two transforms, or {@code null} if there is no substitute.
     */
    private WraparoundTransform merge(final WraparoundTransform other) {
        if (other.wraparoundDimension == wraparoundDimension) {
            if (Numerics.equals(period, other.period)) {        // TODO: relax this rule.
                return this;        // First step in the chain of transforms have precedence.
            }
        }
        return null;
    }

    /**
     * Returns the parameter descriptors for this math transform.
     *
     * @return the parameter descriptors for this math transform.
     */
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        return Wraparound.PARAMETERS;
    }

    /**
     * Returns the parameter values for this math transform. The set of parameters include the number of dimensions,
     * the {@linkplain #wraparoundDimension wraparound dimension} and the {@linkplain #period} values.
     * The default implementation does not include the {@linkplain #sourceMedian source median} because
     * that parameter has no effect on forward transforms (it is used for creating the inverse transform).
     *
     * @return the parameter values for this math transform.
     */
    @Override
    public ParameterValueGroup getParameterValues() {
        final Parameters pg = Parameters.castOrWrap(getParameterDescriptors().createValue());
        pg.getOrCreate(Wraparound.DIMENSION).setValue(dimension);
        pg.getOrCreate(Wraparound.WRAPAROUND_DIMENSION).setValue(wraparoundDimension);
        pg.getOrCreate(Wraparound.PERIOD).setValue(period);
        return pg;
    }

    /**
     * Compares this transform with the given object for equality.
     *
     * @param  object  the object to compare with this transform.
     * @param  mode    ignored, can be {@code null}.
     * @return {@code true} if the given object is considered equals to this math transform.
     *
     * @hidden because nothing new to said.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (super.equals(object, mode)) {
            final var other = (WraparoundTransform) object;
            return other.dimension == dimension &&
                   other.wraparoundDimension == wraparoundDimension &&
                   Numerics.equals(period, other.period) &&
                   Numerics.equals(sourceMedian, other.sourceMedian);
        }
        return false;
    }

    /**
     * Computes a hash code value for this transform.
     *
     * @return the hash code value.
     *
     * @hidden because nothing new to said.
     */
    @Override
    protected int computeHashCode() {
        return dimension * 31 + wraparoundDimension + Double.hashCode(period) + 7*Double.hashCode(sourceMedian);
    }
}
