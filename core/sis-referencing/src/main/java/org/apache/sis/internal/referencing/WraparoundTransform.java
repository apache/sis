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
package org.apache.sis.internal.referencing;

import java.io.Serializable;
import java.util.List;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.transform.AbstractMathTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.measure.Longitude;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.collection.BackingStoreException;


/**
 * Enforces coordinate values in the range of a wraparound axis (typically longitude).
 * This transform is usually not needed for the [-180 … +180]° range since it is the
 * range of trigonometric functions. However this transform is useful for shifting
 * transformation results in the [0 … 360]° range.
 *
 * <p>{@code WraparoundTransform}s are not created automatically by {@link org.apache.sis.referencing.CRS#findOperation
 * CRS.findOperation(…)} because they introduce a discontinuity in coordinate transformations. Such discontinuities are
 * hurtless when transforming only a cloud of points, but produce undesirable artifacts when transforming geometries.
 * Callers need to invoke {@link #forTargetCRS forTargetCRS(…)} explicitly if discontinuities are acceptable.</p>
 *
 * <h2>Wraparound with more than one lap</h2>
 * Current implementation assumes that data cover only one lap. For wraparound on the longitude axis, it means that
 * raster or geometry data should be less than 360° width. Larger data exist, for example images with time varying
 * together with longitude, in which case the points at 0°, 360°, 720°, <i>etc.</i> represent the same spatial location
 * on Earth but at different times. It may not be possible to handle such cases with a wider wraparound range if the
 * {@link MathTransform} chain includes a map projection. If we want to support "multiple laps" scenario in a future
 * version, a strategy could be to define a new transform implementation which wraps a {@code WraparoundTransform}
 * and the map projection together. That implementation would inspect the source coordinates before map projection
 * for determining how many multiples of wraparound range to add to the output coordinates.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final class WraparoundTransform extends AbstractMathTransform implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -8811608024800445706L;

    /**
     * Number of dimensions of the first cached {@link WraparoundTransform} instance.
     * We cache only {@code WraparoundTransform} instances having a period of 360°
     * (i.e. longitudes).
     */
    private static final int FIRST_CACHED_DIMENSION = 2;

    /**
     * Frequently used {@link WraparoundTransform} instances.
     * The {@link #dimension} value ranges from {@value #FIRST_CACHED_DIMENSION} to 4 inclusive,
     * and the {@link #wraparoundDimension} value alternates between 0 and 1.
     */
    private static final WraparoundTransform[] CACHE = new WraparoundTransform[6];
    static {
        for (int i=0; i<CACHE.length; i++) {
            final int dimension = (i >>> 1) + FIRST_CACHED_DIMENSION;
            CACHE[i] = new WraparoundTransform(dimension, i & 1, Longitude.MAX_VALUE - Longitude.MIN_VALUE);
        }
    }

    /**
     * Returns a transform with a wraparound behavior in the given dimension.
     * Input and output values in the wraparound dimension shall be normalized in the [−p/2 … +p/2] range
     * where <var>p</var> is the period (typically 360° for longitude axis).
     */
    static WraparoundTransform create(final int dimension, final int wraparoundDimension, final double period) {
        if (period == (Longitude.MAX_VALUE - Longitude.MIN_VALUE) && (wraparoundDimension & ~1) == 0) {
            final int i = ((dimension - FIRST_CACHED_DIMENSION) << 1) | wraparoundDimension;
            if (i >= 0 && i < CACHE.length) {
                return CACHE[i];
            }
        }
        return new WraparoundTransform(dimension, wraparoundDimension, period);
    }

    /**
     * The dimension of source and target coordinates.
     */
    private final int dimension;

    /**
     * The dimension where to apply wraparound.
     */
    final int wraparoundDimension;

    /**
     * Period on wraparound axis. This is 360° for the longitude axis.
     * Coordinates will be normalized in the [−p/2 … +p/2] range.
     *
     * <h4>Design note</h4>
     * A previous version of {@code WraparoundTransform} had no period. It was expecting coordinates
     * normalized in the [0 … 1] range. Coordinates in [0 … 360]° range were divided by 360 using an
     * affine transforms before {@code WraparoundTransform} and multiplied by 360 using another affine
     * transform after {@code WraparoundTransform}. That approach allowed to delegate more work to the
     * affine transforms which can efficiently be combined with other affine transforms, but it caused
     * more rounding errors.
     */
    private final double period;

    /**
     * Creates a new transform with a wraparound behavior in the given dimension.
     * Input and output values in the wraparound dimension shall be normalized in
     * the [−p/2 … +p/2] range where <var>p</var> is the period (e.g. 360°).
     */
    private WraparoundTransform(final int dimension, final int wraparoundDimension, final double period) {
        this.dimension = dimension;
        this.wraparoundDimension = wraparoundDimension;
        this.period = period;
    }

    /**
     * Returns an instance with the number of dimensions compatible with the given matrix, while keeping
     * {@link #wraparoundDimension} and {@link #period} unchanged.
     * If no instance can be created for the given number of dimensions, then this method returns {@code null}.
     *
     * @param  other            matrix defining a transform which will be applied before or after {@code this}.
     * @param  applyOtherFirst  whether the transform defined by the matrix will be applied before {@code this}.
     */
    private WraparoundTransform redim(final boolean applyOtherFirst, final Matrix other) {
        final int n = (applyOtherFirst ? other.getNumRow() : other.getNumCol()) - 1;
        if (n == dimension) return this;
        if (n >= wraparoundDimension) return null;
        return create(n, wraparoundDimension, period);
    }

    /**
     * Returns the transform of the given coordinate operation augmented with a "wrap around" behavior if applicable.
     * The wraparound is applied on target coordinates and aims to clamp coordinate values inside the range of target
     * coordinate system axes.
     *
     * <p>This method tries to avoid unnecessary wraparounds on a best-effort basis. It makes its decision based
     * on an inspection of source and target CRS axes. For a method making decision based on a domain of use,
     * see {@link #forDomainOfUse forDomainOfUse(…)} instead.</p>
     *
     * @param  op  the coordinate operation for which to get the math transform.
     * @return the math transform for the given coordinate operation.
     * @throws TransformException if a coordinate can not be computed.
     */
    public static MathTransform forTargetCRS(final CoordinateOperation op) throws TransformException {
        MathTransform tr = op.getMathTransform();
        final CoordinateSystem targetCS = op.getTargetCRS().getCoordinateSystem();
        for (final int wraparoundDimension : CoordinateOperations.wrapAroundChanges(op)) {
            tr = concatenate(tr, wraparoundDimension, targetCS, null);
        }
        return tr;
    }

    /**
     * Returns the given transform augmented with a "wrap around" behavior if applicable.
     * The wraparound is applied on target coordinates and aims to clamp coordinate values
     * in a range centered on the given median.
     *
     * <p>The centered ranges may be different than the range declared by the coordinate system axes.
     * In such case, the wraparound range applied by this method will have a translation compared to
     * the range declared by the axis. This translation is useful when the target domain is known
     * (e.g. when transforming a raster) and we want that output coordinates to be continuous
     * in that domain, independently of axis ranges.</p>
     *
     * @param  tr        the transform to augment with "wrap around" behavior.
     * @param  targetCS  the target coordinate system.
     * @param  median    the coordinates to put at the center of new ranges.
     * @return the math transform with wraparound if needed.
     * @throws TransformException if a coordinate can not be computed.
     */
    public static MathTransform forDomainOfUse(MathTransform tr, final CoordinateSystem targetCS,
            final DirectPosition median) throws TransformException
    {
        final int dimension = targetCS.getDimension();
        for (int i=0; i<dimension; i++) {
            tr = concatenate(tr, i, targetCS, median);
        }
        return tr;
    }

    /**
     * Concatenates the given transform with a "wrap around" transform if applicable.
     * The wraparound is implemented by concatenations of affine transforms before and
     * after the {@link WraparoundTransform} instance.
     * If there is no wraparound to apply, then this method returns {@code tr} unchanged.
     *
     * @param  tr                   the transform to concatenate with a wraparound transform.
     * @param  wraparoundDimension  the dimension where "wrap around" behavior may apply.
     * @param  targetCS             the target coordinate system.
     * @param  median               the coordinate to put at the center of new range,
     *                              or {@code null} for standard axis center.
     * @return the math transform with "wrap around" behavior in the specified dimension.
     * @throws TransformException if a coordinate can not be computed.
     */
    private static MathTransform concatenate(final MathTransform tr, final int wraparoundDimension,
            final CoordinateSystem targetCS, final DirectPosition median) throws TransformException
    {
        final double period = WraparoundAdjustment.range(targetCS, wraparoundDimension);
        if (!(period > 0 && period != Double.POSITIVE_INFINITY)) {
            return tr;
        }
        double m;
        if (median == null) {
            final CoordinateSystemAxis axis = targetCS.getAxis(wraparoundDimension);
            m = (axis.getMinimumValue() + axis.getMaximumValue()) / 2;
        } else try {
            m = median.getOrdinate(wraparoundDimension);
        } catch (BackingStoreException e) {
            // Some implementations compute coordinates only when first needed.
            throw e.unwrapOrRethrow(TransformException.class);
        }
        if (Double.isFinite(m)) {
            /*
             * Round the median to a value having an exact representation in base 2 using about 10 bits.
             * The intent is to reduce the risk of rounding errors with add/subtract operations.
             */
            final int power = 10 - Math.getExponent(m);
            m = Math.scalb(Math.rint(Math.scalb(m, power)), -power);
        } else if (median == null) {
            /*
             * May happen if `WraparoundAdjustment.range(…)` recognized a longitude axis
             * despite the `CoordinateSystemAxis` not declarining minimum/maximum values.
             * Use 0 as the range center (e.g. center of [-180 … 180]° longitude range).
             */
            m = 0;
        } else {
            // Invalid median value. Assume caller means "no wrap".
            return tr;
        }
        final int dimension = tr.getTargetDimensions();
        MathTransform wraparound = create(dimension, wraparoundDimension, period);
        if (m != 0) {
            final double[] t = new double[dimension];
            t[wraparoundDimension] = m;
            final MathTransform denormalize = MathTransforms.translation(t);
            wraparound = MathTransforms.concatenate(denormalize.inverse(), wraparound, denormalize);
        }
        return MathTransforms.concatenate(tr, wraparound);
    }

    /**
     * Gets the dimension of input points.
     *
     * @return the dimension of input points.
     */
    @Override
    public int getSourceDimensions() {
        return dimension;
    }

    /**
     * Gets the dimension of output points.
     *
     * @return the dimension of output points.
     */
    @Override
    public int getTargetDimensions() {
        return dimension;
    }

    /**
     * Gets the derivative of this transform at a point.
     *
     * <h4>Mathematical note</h4>
     * Strictly speaking the derivative at (<var>n</var> + 0.5) × {@link #period} where <var>n</var> is an integer
     * should be infinite because the coordinate value jumps "instantaneously" from any value to ±{@link #period}/2.
     * However in practice we use derivatives as linear approximations around small regions, not for calculations
     * requiring strict mathematical values. An infinite value goes against the approximation goal.
     * Furthermore whether a source coordinate is an integer value or not is subject to rounding errors,
     * which may cause unpredictable behavior if those infinite values were provided.
     */
    @Override
    public Matrix derivative(final DirectPosition point) {
        return Matrices.createIdentity(dimension);
    }

    /**
     * Wraparounds a single coordinate point in an array,
     * and optionally computes the transform derivative at that location.
     */
    @Override
    public Matrix transform(final double[] srcPts, int srcOff,
                            final double[] dstPts, int dstOff, final boolean derivate)
    {
        if (dstPts != null) {
            System.arraycopy(srcPts, srcOff, dstPts, dstOff, dimension);
            dstOff += wraparoundDimension;
            dstPts[dstOff] = Math.IEEEremainder(dstPts[dstOff], period);
        }
        return derivate ? Matrices.createIdentity(dimension) : null;
    }

    /**
     * Transforms many coordinates in a list of ordinal values.
     */
    @Override
    public void transform(final double[] srcPts, int srcOff,
                          final double[] dstPts, int dstOff, int numPts)
    {
        System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * dimension);
        dstOff += wraparoundDimension;
        while (--numPts >= 0) {
            dstPts[dstOff] = Math.IEEEremainder(dstPts[dstOff], period);
            dstOff += dimension;
        }
    }

    /**
     * Transforms many coordinates in a list of ordinal values.
     */
    @Override
    public void transform(final float[] srcPts, int srcOff,
                          final float[] dstPts, int dstOff, int numPts)
    {
        System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * dimension);
        dstOff += wraparoundDimension;
        while (--numPts >= 0) {
            dstPts[dstOff] = (float) Math.IEEEremainder(dstPts[dstOff], period);
            dstOff += dimension;
        }
    }

    /**
     * Throws a {@code NoninvertibleTransformException}.
     * We do not return another {@code WraparoundTransform} for three reasons:
     *
     * <ul>
     *   <li>The inverse wraparound would work on a different range of values, but we do not know that range.</li>
     *   <li>Even if we knew the original range of values, creating the inverse transform would require the affine
     *       transforms before and after {@code WraparoundTransform} to be different; it would not be their inverse.
     *       This is impractical, especially since the transform matrices may have been multiplied with other affine
     *       transforms.</li>
     *   <li>Even if we were able to build the inverse {@code WraparoundTransform}, it would not necessarily be
     *       appropriate. For example in "ProjectedCRS → BaseCRS → GeographicCRS" operation chain, wraparound
     *       may happen after the geographic CRS. But in the "GeographicCRS → BaseCRS → ProjectedCRS" inverse
     *       operation, the wraparound would be between BaseCRS and ProjectedCRS, which is often not needed.</li>
     * </ul>
     *
     * We do not return an identity transform because it causes incorrect resampling operation steps when concatenated,
     * especially when testing if transforms are mutually the inverse of each other.
     *
     * @return never return.
     * @throws NoninvertibleTransformException always thrown.
     */
    @Override
    public MathTransform inverse() throws NoninvertibleTransformException {
        return super.inverse();
    }

    /**
     * Concatenates in an optimized way this math transform with the given one, if possible.
     */
    @Override
    protected MathTransform tryConcatenate(final boolean applyOtherFirst, final MathTransform other,
                                           final MathTransformFactory factory) throws FactoryException
    {
        /*
         * If the other transform is also a `WraparoundTransform` for the same dimension,
         * then there is no need to concatenate those two consecutive redudant transforms.
         */
        if (equals(other, null)) {
            return this;
        }
        final List<MathTransform> steps = MathTransforms.getSteps(other);
        final int count = steps.size();
        if (count >= 2) {
            final MathTransform middleTr = steps.get(applyOtherFirst ? count - 1 : 0);
            Matrix middle = MathTransforms.getMatrix(middleTr);
            if (middle != null) try {
                /*
                 * We have a matrix between this `WraparoundTransform` and something else,
                 * potentially another `WraparoundTransform`. Try to move as many rows as
                 * possible outside that `middle` matrix. Ideally we will be able to move
                 * the matrix completely, which increase the chances to multiply (outside
                 * this method) with another matrix.
                 */
                final MathTransformsOrFactory mf = MathTransformsOrFactory.wrap(factory);
                boolean modified = false;
                MathTransform step1 = this;
                MathTransform move = movable(middleTr, middle, mf);
                if (move != null) {
                    /*
                     * Update the middle matrix with everything that we could not put in `move`.
                     * If `applyOtherFirst` is false:
                     *
                     *     [move]  →  [redim]  →  [remaining]  →  [other]
                     *
                     * If `applyOtherFirst` is true:
                     *
                     *     [other]  →  [remaining]  →  [redim]  →  [move]
                     *
                     * Usually the matrix is square before the multiplication. But if it was not the case,
                     * the new matrix will have the same number of columns (source coordinates) but a new
                     * number of rows (target coordinates). The result should be a square matrix.
                     */
                    final Matrix remaining = remaining(applyOtherFirst, move, middle);
                    final WraparoundTransform redim = redim(applyOtherFirst, remaining);
                    if (redim != null) {
                        step1    = mf.concatenate(applyOtherFirst, move, redim);
                        middle   = remaining;
                        modified = true;
                    }
                }
                /*
                 * Now look at the non-linear transform. If it is another instance of `WraparoundTransform`,
                 * then we may move the calculation of some coordinates before it. This is the same algorithm
                 * than above but with `applyOtherFirst` reversed.
                 */
                MathTransform step2 = steps.get(applyOtherFirst ? count - 2 : 1);
                if (step2 instanceof WraparoundTransform) {
                    WraparoundTransform redim = (WraparoundTransform) step2;
                    move = redim.movable(null, middle, mf);
                    if (move != null) {
                        final Matrix remaining = remaining(!applyOtherFirst, move, middle);
                        redim = redim.redim(!applyOtherFirst, remaining);
                        if (redim != null) {
                            step2    = mf.concatenate(applyOtherFirst, redim, move);
                            middle   = remaining;
                            modified = true;
                        }
                    }
                }
                /*
                 * Done moving the linear operations that we can move. Put everything together.
                 */
                if (modified) {
                    MathTransform tr = mf.linear(middle);
                    tr = mf.concatenate(applyOtherFirst, tr, step2);
                    tr = mf.concatenate(applyOtherFirst, step1, tr);
                    if (applyOtherFirst) {
                        for (int i = count-2; --i >= 0;) {
                            tr = mf.concatenate(steps.get(i), tr);
                        }
                    } else {
                        for (int i = 2; i < count; i++) {
                            tr = mf.concatenate(tr, steps.get(i));
                        }
                    }
                    return tr;
                }
            } catch (NoninvertibleTransformException e) {
                // Should not happen. But if it is the case, just abandon the optimization effort.
                Logging.recoverableException(Logging.getLogger(Modules.REFERENCING), getClass(), "tryConcatenate", e);
            }
        }
        return null;
    }

    /**
     * Returns a transform based on the given matrix but converting only coordinates in dimensions
     * that can be processed indifferently before or after this {@code WraparoundTransform}.
     *
     * @param  tr       the transform of the matrix to analyze, or {@code null} if none.
     * @param  matrix   the matrix to analyze.
     * @param  factory  wrapper of the factory given to {@link #tryConcatenate tryConcatenate(…)}.
     * @return a transform processing only the movable parts, or {@code null} if identity.
     */
    private MathTransform movable(MathTransform tr, Matrix matrix, final MathTransformsOrFactory factory)
            throws FactoryException
    {
        final long moveAll = Numerics.bitmask(dimension) - 1;
        long canMoveAfter = moveAll;
        /*
         * If any matrix row (output coordinate) uses the wraparound dimension as input,
         * then we can not move that row because the coordinate value may not be the same
         * after execution of `WraparoundTransform`.
         */
        if (matrix.getNumCol() - 1 > wraparoundDimension) {
            for (int j = matrix.getNumRow(); --j >= 0;) {
                final double v = matrix.getElement(j, wraparoundDimension);
                if (v != (j == wraparoundDimension ? 1 : 0)) {
                    canMoveAfter &= ~Numerics.bitmask(j);
                }
            }
        }
        if (canMoveAfter != 0) {
            if (canMoveAfter != moveAll) {
                /*
                 * Create a matrix which will convert coordinates in all dimensions that we can process
                 * before or after this `WraparoundTransform`. We start with a copy and set to identity
                 * the rows that we can not move. Typically only one row will be set to identity, which
                 * makes the "start with a copy" strategy a good choice. Another reason is that we want
                 * to preserve the "double-double" storage.
                 */
                matrix = Matrices.copy(matrix);
                for (int j = matrix.getNumRow() - 1; --j >=0;) {
                    if ((canMoveAfter & Numerics.bitmask(j)) == 0) {    // True also for dimensions ≥ 64.
                        for (int i=matrix.getNumCol(); --i >= 0;) {
                            matrix.setElement(j, i, (i == j) ? 1 : 0);
                        }
                    }
                }
            } else if (tr != null) {
                return tr;
                // OTherwise `matrix` can be used as-is.
            }
            if (!matrix.isIdentity()) {
                return factory.linear(matrix);
            }
        }
        return null;
    }

    /**
     * Returns a matrix equivalent to applying the inverse of {@code move} first, followed by {@code middle}.
     * This is appropriate if the {@code move} transform is going to be applied first, followed by {@code remaining}.
     *
     * <p>If {@code reverse} is {@code true} then this method performs the concatenation in reverse order.
     * This is appropriate if the {@code remaining} matrix is going to be applied first, followed by {@code move}.</p>
     */
    private static Matrix remaining(final boolean reverse, final MathTransform move, final Matrix middle)
            throws NoninvertibleTransformException
    {
        final Matrix change = MathTransforms.getMatrix(move.inverse());
        return reverse ? Matrices.multiply(change, middle)
                       : Matrices.multiply(middle, change);
    }

    /**
     * Formats this transform as a pseudo-WKT element.
     *
     * @param  formatter  the formatter to use.
     * @return the WKT element name, which is {@code "Wraparound_MT"}.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        formatter.append(dimension);
        formatter.append(wraparoundDimension);
        formatter.append(period);
        formatter.setInvalidWKT(WraparoundTransform.class, null);
        return "Wraparound_MT";
    }

    /**
     * Compares this transform with the given object for equality.
     *
     * @param  object  the object to compare with this transform.
     * @param  mode    ignored, can be {@code null}.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object instanceof WraparoundTransform) {
            final WraparoundTransform other = (WraparoundTransform) object;
            return other.dimension == dimension && other.wraparoundDimension == wraparoundDimension
                    && Numerics.equals(period, other.period);
        }
        return false;
    }

    /**
     * Computes a hash code value for this transform.
     */
    @Override
    protected int computeHashCode() {
        return dimension * 31 + wraparoundDimension + Double.hashCode(period);
    }
}
