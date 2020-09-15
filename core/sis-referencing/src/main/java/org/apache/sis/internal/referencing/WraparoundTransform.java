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

import java.util.List;
import java.util.function.Supplier;
import java.util.function.IntToDoubleFunction;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.AbstractMathTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.io.wkt.Formatter;
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
public final class WraparoundTransform extends AbstractMathTransform {
    /**
     * Number of dimensions of the first cached {@link WraparoundTransform} instance.
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
            CACHE[i] = new WraparoundTransform(dimension, i & 1);
        }
    }

    /**
     * Returns a transform with a wraparound behavior in the given dimension.
     * Input and output values in the wraparound dimension shall be normalized in the [0 … 1] range.
     */
    static WraparoundTransform create(final int dimension, final int wraparoundDimension) {
        if ((wraparoundDimension & ~1) == 0) {
            final int i = ((dimension - FIRST_CACHED_DIMENSION) << 1) | wraparoundDimension;
            if (i >= 0 && i < CACHE.length) {
                return CACHE[i];
            }
        }
        return new WraparoundTransform(dimension, wraparoundDimension);
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
     * Creates a new transform with a wraparound behavior in the given dimension.
     * Input and output values in the wraparound dimension shall be normalized in
     * the [0 … 1] range.
     */
    private WraparoundTransform(final int dimension, final int wraparoundDimension) {
        this.dimension = dimension;
        this.wraparoundDimension = wraparoundDimension;
    }

    /**
     * Returns an instance with the given number of dimensions while keeping {@link #wraparoundDimension} unchanged.
     * If no instance can be created for the given number of dimensions, then this method returns {@code null}.
     */
    private WraparoundTransform redim(final int n) {
        if (n == dimension) return this;
        if (n >= wraparoundDimension) return null;
        return create(n, wraparoundDimension);
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
     * @param  op       the coordinate operation for which to get the math transform.
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
        final double span = WraparoundAdjustment.range(targetCS, wraparoundDimension);
        if (!(span > 0 && span != Double.POSITIVE_INFINITY)) {
            return tr;
        }
        double start;
        if (median != null) {
            try {
                start = median.getOrdinate(wraparoundDimension) + -0.5 * span;
            } catch (BackingStoreException e) {
                // Some implementations compute coordinates only when first needed.
                throw e.unwrapOrRethrow(TransformException.class);
            }
            if (!Double.isFinite(start)) return tr;
        } else {
            start = targetCS.getAxis(wraparoundDimension).getMinimumValue();
            if (!Double.isFinite(start)) {
                /*
                 * May happen if `WraparoundAdjustment.range(…)` recognized a longitude axis
                 * despite the `CoordinateSystemAxis` not declarining minimum/maximum values.
                 * Use 0 as the range center (e.g. center of [-180 … 180]° longitude range).
                 */
                start = -0.5 * span;
            }
        }
        final int dimension = tr.getTargetDimensions();
        final MatrixSIS m = Matrices.createIdentity(dimension + 1);
        m.setElement(wraparoundDimension, wraparoundDimension, span);
        m.setElement(wraparoundDimension, dimension, start);
        final MathTransform denormalize = MathTransforms.linear(m);
        MathTransform wraparound = create(dimension, wraparoundDimension);
        /*
         * Do not use the 3-arguments method because we need to
         * control the order in which concatenation is applied.
         */
        wraparound = MathTransforms.concatenate(denormalize.inverse(),
                     MathTransforms.concatenate(wraparound, denormalize));
        return MathTransforms.concatenate(tr, wraparound);
    }

    /**
     * Verifies whether wraparound is needed for a "CRS to grid" transform.
     * This method converts coordinates of all corners of a grid to an arbitrary CRS, then back to the grid.
     * The forward and backward transforms are not exactly the inverse of each other: the forward transform
     * applies wraparounds while the backward transform does not. By checking whether or not the roundtrip
     * result is equal to the original coordinates, we determine if wraparound is necessary or not.
     *
     * <p>The coordinates to test are provided by {@code gridExtent} using the following convention.
     * For function argument {@code int} <var>i</var>:</p>
     * <ul>
     *   <li>If <var>i</var> ≥ 0, return the upper value at dimension {@code i}.</li>
     *   <li>If <var>i</var> &lt; 0, return the lower value at dimension {@code ~i} (tild operator, not minus).</li>
     * </ul>
     *
     * @param  gridToCRS       a transform to an arbitrary CRS with handling of wraparound axes.
     * @param  crsToGrid       inverse of {@code gridToCRS} but without handling of wraparound axes.
     * @param  withWraparound  provider of a transform equivalent to {@code crsToGrid} but with wraparound applied.
     * @param  gridExtent      provider of grid corner coordinates (see above javadoc).
     * @return whether wraparound transform seems needed.
     * @throws TransformException if an error occurred while transforming coordinates.
     */
    public static boolean isNeeded(final MathTransform gridToCRS, final MathTransform crsToGrid,
            final Supplier<MathTransform> withWraparound, final IntToDoubleFunction gridExtent)
            throws TransformException
    {
        MathTransform  watr = null;                     // Transform with wraparound (fetched only if needed).
        final int dimension = gridToCRS.getSourceDimensions();
        final double[]  src = new double[dimension];    // Coordinates of a corner.
        final double[]  nwp = new double[dimension];    // Roundtrip result without wraparound.
        final double[]  tgt = new double[Math.max(gridToCRS.getTargetDimensions(), dimension)];
        long  maskOfUppers  = Numerics.bitmask(dimension);
        long  maskOfChanges = 0;
        do {
            maskOfChanges ^= --maskOfUppers;            // Source coordinates that changed since last iteration.
            do {
                final int i = Long.numberOfTrailingZeros(maskOfChanges);
                final long bit = 1L << i;
                src[i] = gridExtent.applyAsDouble((maskOfUppers & bit) == 0 ? ~i : i);
                maskOfChanges &= ~bit;
            } while (maskOfChanges != 0);               // Iterate only over the coordinates that changed.
            maskOfChanges = maskOfUppers;
            /*
             * Transform corner from the source extent to the destination CRS, then back to source grid coordinates.
             * We do not concatenate the forward and inverse transforms because we do not want MathTransformFactory
             * to simplify the transformation chain (e.g. replacing "Mercator → Inverse of Mercator" by an identity
             * transform), because such simplification would erase wraparound effects.
             */
            boolean watrApplied = false;
            gridToCRS.transform(src, 0, tgt, 0, 1);
            crsToGrid.transform(tgt, 0, nwp, 0, 1);
            for (int i=0; i<dimension; i++) {
                final double error = Math.abs(nwp[i] - src[i]);
                if (!(error <= 1)) {                                // Use `!` for catching NaN.
                    if (!watrApplied) try {
                        watrApplied = true;
                        if (watr == null) watr = withWraparound.get();
                        watr.transform(tgt, 0, tgt, 0, 1);
                    } catch (BackingStoreException e) {
                        throw e.unwrapOrRethrow(TransformException.class);
                    }
                    /*
                     * Do not consider NaN in `tgt` as a need for wraparound because NaN values
                     * occur when an operation between two CRS reduces the number of dimensions.
                     */
                    if (Math.abs(tgt[i] - src[i]) < (error <= Double.MAX_VALUE ? error : 1)) {
                        return true;
                    }
                }
            }
        } while (maskOfUppers != 0);
        return false;
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
     * Whether to allow discontinuities in transform derivatives. Strictly speaking the derivative
     * at 1, 2, 3… should be infinite because the coordinate value jumps "instantaneously" from an
     * integer value to 0.  However in practice we use derivatives as linear approximations around
     * small regions, not for calculations requiring strict mathematical values. An infinite value
     * goes against the approximation goal.  Furthermore whether a source coordinate is an integer
     * value or not is subject to rounding errors, which may cause unpredictable behavior if
     * discontinuities are allowed.
     */
    private static final boolean ALLOW_DISCONTINUITIES = false;

    /**
     * Gets the derivative of this transform at a point.
     */
    @Override
    public Matrix derivative(final DirectPosition point) {
        final MatrixSIS derivative = Matrices.createIdentity(dimension);
        if (ALLOW_DISCONTINUITIES) {
            final double v = point.getOrdinate(wraparoundDimension);
            if (v == Math.floor(v)) {
                derivative.setElement(wraparoundDimension, wraparoundDimension, Double.NEGATIVE_INFINITY);
            }
        }
        return derivative;
    }

    /**
     * Wraparounds a single coordinate point in an array,
     * and optionally computes the transform derivative at that location.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff, final boolean derivate)
    {
        double v = srcPts[srcOff];
        v -= Math.floor(v);
        if (dstPts != null) {
            System.arraycopy(srcPts, srcOff, dstPts, dstOff, dimension);
            dstPts[dstOff + wraparoundDimension] = v;
        }
        if (!derivate) {
            return null;
        }
        final MatrixSIS derivative = Matrices.createIdentity(dimension);
        if (ALLOW_DISCONTINUITIES && v == 0) {
            derivative.setElement(wraparoundDimension, wraparoundDimension, Double.NEGATIVE_INFINITY);
        }
        return derivative;
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
            dstPts[dstOff] -= Math.floor(dstPts[dstOff]);
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
            dstPts[dstOff] -= Math.floor(dstPts[dstOff]);
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
        /*
         * If two `WraparoundTransform` instances are separated only by a `LinearTransform`,
         * then that linear transform is moved before or after this `WraparoundTransform`
         * for increasing the chances to concatenate it using a matrix multiplication.
         */
        if (applyOtherFirst) {
            final List<MathTransform> steps = MathTransforms.getSteps(other);
            if (steps.size() == 2) {
                final MathTransform middle = steps.get(1);
                Matrix matrix = MathTransforms.getMatrix(middle);
                if (matrix != null) try {
                    final MathTransformsOrFactory mf = MathTransformsOrFactory.wrap(factory);
                    boolean modified = false;
                    MathTransform step2 = this;
                    final MathTransform after = movable(matrix, mf);
                    if (after != null) {
                        /*
                         * Update the middle matrix with everything that we could not put in `after`.
                         * Usually the matrix is square before the multiplication. But if it was not the case,
                         * the new matrix will have the same number of columns (source coordinates) but a new
                         * number of rows (target coordinates). The result should be a square matrix.
                         */
                        final Matrix remaining = Matrices.multiply(MathTransforms.getMatrix(after.inverse()), matrix);
                        final WraparoundTransform redim = redim(remaining.getNumRow() - 1);
                        if (redim != null) {
                            step2 = mf.concatenate(redim, after);
                            matrix = remaining;
                            modified = true;
                        }
                    }
                    /*
                     * Now look at the non-linear transform. If it is another instance of `WraparoundTransform`,
                     * then we may move the calculation of some coordinates before it.
                     */
                    MathTransform step1 = steps.get(0);
                    if (step1 instanceof WraparoundTransform) {
                        WraparoundTransform wb = (WraparoundTransform) step1;
                        final MathTransform before = wb.movable(matrix, mf);
                        if (before != null) {
                            final Matrix remaining = Matrices.multiply(matrix, MathTransforms.getMatrix(before.inverse()));
                            wb = wb.redim(remaining.getNumCol() - 1);
                            if (wb != null) {
                                step1 = mf.concatenate(before, wb);
                                matrix = remaining;
                                modified = true;
                            }
                        }
                    }
                    /*
                     * Done moving the linear operations that we can move. Put everything together.
                     * Do not use the 3-arguments method because we need to control the order in which
                     * concatenation is applied.
                     */
                    if (modified) {
                        return mf.concatenate(mf.concatenate(step1, mf.linear(matrix)), step2);
                    }
                } catch (NoninvertibleTransformException e) {
                    // Should not happen. But if it is the case, just abandon the optimization effort.
                    Logging.recoverableException(Logging.getLogger(Modules.REFERENCING), getClass(), "tryConcatenate", e);
                }
            }
        }
        return null;
    }

    /**
     * Returns a transform based on the given matrix but converting only coordinates in dimensions
     * that can be processed indifferently before or after this {@code WraparoundTransform}.
     *
     * @param  matrix   the matrix to analyze.
     * @param  factory  wrapper of the factory given to {@link #tryConcatenate tryConcatenate(…)}.
     * @return a transform processing only the movable parts, or {@code null} if identity.
     */
    private MathTransform movable(Matrix matrix, final MathTransformsOrFactory factory) throws FactoryException {
        long canMoveAfter = Numerics.bitmask(dimension) - 1;
        canMoveAfter &= ~Numerics.bitmask(wraparoundDimension);
        /*
         * If any matrix row (output coordinate) uses the wraparound dimension as input,
         * then we can not move that row because the coordinate value may not be the same
         * after execution of `WraparoundTransform`.
         */
        if (matrix.getNumCol() - 1 > wraparoundDimension) {
            for (int j = matrix.getNumRow(); --j >= 0;) {
                if (matrix.getElement(j, wraparoundDimension) != 0) {
                    canMoveAfter &= ~Numerics.bitmask(j);
                }
            }
        }
        if (canMoveAfter != 0) {
            /*
             * Create a matrix which will convert coordinates in all dimensions
             * that we can process before or after this `WraparoundTransform`.
             * We start with a copy and set to identity the rows that we can not move.
             */
            matrix = Matrices.copy(matrix);
            for (int j = matrix.getNumRow() - 1; --j >=0;) {
                if ((canMoveAfter & Numerics.bitmask(j)) == 0) {
                    for (int i=matrix.getNumCol(); --i >= 0;) {
                        matrix.setElement(j, i, (i == j) ? 1 : 0);
                    }
                }
            }
        }
        if (!matrix.isIdentity()) {
            return factory.linear(matrix);
        }
        return null;
    }

    /**
     * Formats this transform as a pseudo-WKT element.
     *
     * @param  formatter  the formatter to use.
     * @return the WKT element name, which is {@code "Wraparound_MT"}.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        formatter.append(wraparoundDimension);
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
            return other.dimension == dimension && other.wraparoundDimension == wraparoundDimension;
        }
        return false;
    }

    /**
     * Computes a hash code value for this transform.
     */
    @Override
    protected int computeHashCode() {
        return dimension * 31 + wraparoundDimension;
    }
}
