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
package org.apache.sis.referencing.internal.shared;

import javax.measure.Unit;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.cs.RangeMeaning;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.WraparoundTransform;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.measure.Longitude;
import org.apache.sis.measure.Units;


/**
 * Appends a {@link WraparoundTransform} to an existing {@link MathTransform}.
 * Each {@code WraparoundTransform} instance should be used only once.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class WraparoundApplicator {
    /**
     * Coordinates at the center of source envelope, or {@code null} if none.
     * This coordinate shall be expressed in the source CRS of the {@link WraparoundTransform},
     * not in the source CRS of the concatenated transform to be created by this applicator.
     */
    private final DirectPosition sourceMedian;

    /**
     * Coordinates to put at the center of new coordinate ranges,
     * or {@code null} for standard axis center.
     */
    private final DirectPosition targetMedian;

    /**
     * The target coordinate system.
     */
    private final CoordinateSystem targetCS;

    /**
     * Creates a new applicator. The median coordinates are optional and can be null.
     * Coordinates shall be in the source and target CRS of the {@link WraparoundTransform},
     * not in the source CRS of the concatenated transform to be created by this applicator.
     *
     * @param  sourceMedian  the coordinates at the center of source envelope, or {@code null} if none.
     * @param  targetMedian  the coordinates to put at the center of new range, or {@code null} for standard axis center.
     * @param  targetCS      the target coordinate system.
     */
    public WraparoundApplicator(final DirectPosition sourceMedian, final DirectPosition targetMedian, final CoordinateSystem targetCS) {
        this.sourceMedian = sourceMedian;
        this.targetMedian = targetMedian;
        this.targetCS     = targetCS;
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
     * @throws TransformException if a coordinate cannot be computed.
     */
    public static MathTransform forTargetCRS(final CoordinateOperation op) throws TransformException {
        final WraparoundApplicator ap = new WraparoundApplicator(null, null, op.getTargetCRS().getCoordinateSystem());
        MathTransform tr = op.getMathTransform();
        for (final int wraparoundDimension : CoordinateOperations.wrapAroundChanges(op)) {
            tr = ap.concatenate(tr, wraparoundDimension);
        }
        return tr;
    }

    /**
     * Returns the given transform augmented with a "wrap around" behavior if applicable.
     * The wraparound is applied on target coordinates and aims to clamp coordinate values
     * in a range centered on the target median given at construction time.
     *
     * <p>The centered ranges may be different than the range declared by the coordinate system axes.
     * In such case, the wraparound range applied by this method will have a translation compared to
     * the range declared by the axis. This translation is useful when the target domain is known
     * (e.g. when transforming a raster) and we want that output coordinates to be continuous
     * in that domain, independently of axis ranges.</p>
     *
     * <p>If a non-null {@code sourceMedian} position has been specified at construction time,
     * those coordinates shall be expressed in the <em>target</em> CRS of given transform.</p>
     *
     * @param  tr  the transform to augment with "wrap around" behavior.
     * @return the math transform with wraparound if needed.
     * @throws TransformException if a coordinate cannot be computed.
     */
    public MathTransform forDomainOfUse(MathTransform tr) throws TransformException {
        final int dimension = targetCS.getDimension();
        for (int i=0; i<dimension; i++) {
            tr = concatenate(tr, i);
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
     * @return the math transform with "wrap around" behavior in the specified dimension.
     * @throws TransformException if a coordinate cannot be computed.
     */
    private MathTransform concatenate(final MathTransform tr, final int wraparoundDimension) throws TransformException {
        final double period = range(targetCS, wraparoundDimension);
        if (!(period > 0 && period != Double.POSITIVE_INFINITY)) {
            return tr;
        }
        double m, sm;
        try {
            if (targetMedian == null) {
                final CoordinateSystemAxis axis = targetCS.getAxis(wraparoundDimension);
                m = (axis.getMinimumValue() + axis.getMaximumValue()) / 2;
            } else {
                m = targetMedian.getOrdinate(wraparoundDimension);
            }
            if (!Double.isFinite(m)) {
                if (targetMedian != null) {
                    // Invalid median value. Assume caller means "no wrap".
                    return tr;
                }
                /*
                 * May happen if `WraparoundAdjustment.range(…)` recognized a longitude axis
                 * despite the `CoordinateSystemAxis` not declarining minimum/maximum values.
                 * Use 0 as the range center (e.g. center of [-180 … 180]° longitude range).
                 */
                m = 0;
            }
            sm = (sourceMedian != null) ? sourceMedian.getOrdinate(wraparoundDimension) : Double.NaN;
        } catch (BackingStoreException e) {
            // Some `DirectPosition` implementations compute coordinates only when first needed.
            throw e.unwrapOrRethrow(TransformException.class);
        }
        /*
         * If the two medians are very close to each other, make them equal. It simplifies the chain of operations.
         * This code applies an arbitrary threshold for avoiding the effect of rounding errors, because `create(…)`
         * will use the difference between those two values.
         */
        final double max = Math.max(Math.abs(sm), Math.abs(m));
        if (Math.abs(sm - m) < max * Numerics.COMPARISON_THRESHOLD) {
            // Arbitrarily take the value closest to an integer.
            final int n = Math.getExponent(max);
            if (error(sm, n) <= error(m, n)) {
                m = sm;
            } else {
                sm = m;
            }
        }
        final MathTransform wraparound = WraparoundTransform.create(tr.getTargetDimensions(), wraparoundDimension, period, sm, m);
        return MathTransforms.concatenate(tr, wraparound);
    }

    /**
     * Returns an arbitrary measurement of the error of given value.
     * We use this measurement for arbitrarily taking the value closest to an integer.
     */
    private static double error(double m, final int n) {
        m = Math.scalb(m, 6 - n);           // 6 is an arbitrary extra accuracy to keep for comparison.
        return Math.abs(m - Math.rint(m));
    }

    /**
     * Returns the range (maximum - minimum) of axis in specified dimension if it has wraparound meaning,
     * or {@link Double#NaN} otherwise. This method implements a fallback for longitude axis if it does
     * not declare the minimum and maximum values as expected.
     *
     * @param  cs         the coordinate system for which to get wraparound range.
     * @param  dimension  dimension of the axis to test.
     * @return the wraparound range, or {@link Double#NaN} if none.
     */
    static double range(final CoordinateSystem cs, final int dimension) {
        final CoordinateSystemAxis axis = cs.getAxis(dimension);
        if (axis != null && axis.getRangeMeaning() == RangeMeaning.WRAPAROUND) {
            double period = axis.getMaximumValue() - axis.getMinimumValue();
            if (period > 0 && period != Double.POSITIVE_INFINITY) {
                return period;
            }
            final AxisDirection dir = AxisDirections.absolute(axis.getDirection());
            if (dir == AxisDirection.EAST && cs instanceof EllipsoidalCS) {
                period = Longitude.MAX_VALUE - Longitude.MIN_VALUE;
                final Unit<?> unit = axis.getUnit();
                if (unit != null) {
                    period = Units.DEGREE.getConverterTo(Units.ensureAngular(unit)).convert(period);
                }
                return period;
            }
        }
        return Double.NaN;
    }
}
