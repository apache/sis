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

import javax.measure.Unit;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.RangeMeaning;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.internal.metadata.AxisDirections;
import org.apache.sis.measure.Longitude;
import org.apache.sis.measure.Units;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.GeneralDirectPosition;


/**
 * Adjustments applied on an envelope for handling wraparound axes. The adjustments consist in shifting
 * coordinate values on some axes by an integer amount of periods (typically 360° of longitude) in order
 * to move an envelope or a position inside a given domain of validity.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final class WraparoundAdjustment {
    /**
     * The region inside which a given Area Of Interest (AOI) or Point Of Interest (POI) should be located.
     * This envelope may be initially in a projected CRS and converted later to geographic CRS in order to
     * allow identification of wraparound axes.
     */
    private Envelope domainOfValidity;

    /**
     * If the AOI or POI does not use the same CRS than {@link #domainOfValidity}, the transformation from
     * {@code domainOfValidity} to the AOI / POI. Otherwise {@code null}.
     *
     * <div class="note"><b>Note:</b>
     * this class does not check by itself if a coordinate operation is needed; it must be supplied. We do that
     * because {@code WraparoundAdjustment} is currently used in contexts where this transform is known anyway,
     * so we avoid to compute it twice.</div>
     */
    private final MathTransform domainToAOI;

    /**
     * A transform from the {@link #domainOfValidity} CRS to any user space at caller choice.
     * The object returned by {@code shift} will be transformed by this transform after all computations
     * have been finished. This is done in order to allow final transforms to be concatenated in a single step.
     */
    private final MathTransform domainToAny;

    /**
     * If {@code areaOfInterest} or {@code pointOfInterest} has been converted to a geographic CRS,
     * the transformation back to its original CRS. Otherwise {@code null}.
     */
    private MathTransform geographicToAOI;

    /**
     * The coordinate reference system of the Area Of Interest (AOI) or Point of Interest (POI).
     * May be replaced by a geographic CRS if the AOI or POI originally used a projected CRS.
     */
    private CoordinateReferenceSystem crs;

    /**
     * Whether {@link #domainOfValidity} has been transformed to the geographic CRS that is the source
     * of {@link #geographicToAOI}. This flag is used for ensuring that {@link #replaceCRS()} performs
     * the inverse projection only once.
     */
    private boolean isDomainTransformed;

    /**
     * Whether the Area Of Interest (AOI) or Point Of Interest (POI) has been transformed in order
     * to allow identification of wraparound axes. If {@code true}, then {@link #geographicToAOI}
     * needs to be applied in order to restore the AOI or POI to its original projected CRS.
     */
    private boolean isResultTransformed;

    /**
     * Creates a new instance for adjusting an Area Of Interest (AOI) or Point Of Interest (POI) to the given
     * domain of validity. The AOI or POI will be given later, but this method nevertheless requires in advance
     * the transform from {@code domainOfValidity} to AOI or POI.
     *
     * @param  domainOfValidity  the region where a given area or point of interest should be located.
     * @param  domainToAOI       if the AOI or POI are going to use a different CRS than {@code domainOfValidity}, the
     *                           transform from {@code domainOfValidity} to the AOI or POI CRS. Otherwise {@code null}.
     * @param  domainToAny       a transform from the {@code domainOfValidity} CRS to any user space at caller choice.
     */
    public WraparoundAdjustment(final Envelope domainOfValidity, final MathTransform domainToAOI, final MathTransform domainToAny) {
        this.domainOfValidity = domainOfValidity;
        this.domainToAOI      = domainToAOI;
        this.domainToAny      = domainToAny;
    }

    /**
     * Returns the range (maximum - minimum) of the given axis if it has wraparound meaning,
     * or {@link Double#NaN} otherwise. This method implements a fallback for the longitude
     * axis if it does not declare the minimum and maximum values as expected.
     *
     * @param  cs         the coordinate system for which to get wraparound range.
     * @param  dimension  dimension of the axis to test.
     * @return the wraparound range, or {@link Double#NaN} if none.
     */
    static double range(final CoordinateSystem cs, final int dimension) {
        final CoordinateSystemAxis axis = cs.getAxis(dimension);
        if (axis != null && RangeMeaning.WRAPAROUND.equals(axis.getRangeMeaning())) {
            double period = axis.getMaximumValue() - axis.getMinimumValue();
            if (period > 0 && period != Double.POSITIVE_INFINITY) {
                return period;
            }
            final AxisDirection dir = AxisDirections.absolute(axis.getDirection());
            if (AxisDirection.EAST.equals(dir) && cs instanceof EllipsoidalCS) {
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

    /**
     * Sets {@link #crs} to the given value if it is non-null, or to the {@link #domainOfValidity} CRS otherwise.
     * If no non-null CRS is available, returns {@code false} for instructing caller to terminate immediately.
     *
     * @return whether a non-null CRS has been set.
     */
    private boolean setIfNonNull(CoordinateReferenceSystem crs) {
        if (crs == null) {
            assert domainToAOI == null || domainToAOI.isIdentity();
            crs = domainOfValidity.getCoordinateReferenceSystem();          // Assumed to apply to AOI or POI too.
            if (crs == null) {
                return false;
            }
        }
        this.crs = crs;
        return true;
    }

    /**
     * If the coordinate reference system is a projected CRS, replaces it by another CRS where wraparound axes can
     * be identified. The wraparound axes are identifiable in base geographic CRS. If such replacement is applied,
     * remember that we may need to transform the result later.
     *
     * @return whether the replacement has been done. If {@code true}, then {@link #geographicToAOI} is non-null.
     */
    private boolean replaceCRS() {
        if (crs instanceof ProjectedCRS) {
            final ProjectedCRS p = (ProjectedCRS) crs;
            crs = p.getBaseCRS();                                          // Geographic, so a wraparound axis certainly exists.
            geographicToAOI = p.getConversionFromBase().getMathTransform();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Transforms {@link #domainOfValidity} to the same CRS than the Area Of Interest (AOI) or Point Of Interest (POI).
     * This method should be invoked only when the caller detected a wraparound axis. This method transforms the domain
     * the first time it is invoked, and does nothing on all subsequent calls.
     */
    private void transformDomainToAOI() throws TransformException {
        if (!isDomainTransformed) {
            isDomainTransformed = true;
            MathTransform domainToGeographic = domainToAOI;
            if (domainToGeographic == null) {
                domainToGeographic = geographicToAOI;
            } else if (geographicToAOI != null) {
                domainToGeographic = MathTransforms.concatenate(domainToGeographic, geographicToAOI.inverse());
            }
            if (domainToGeographic != null && !domainToGeographic.isIdentity()) {
                domainOfValidity = Envelopes.transform(domainToGeographic, domainOfValidity);
            }
        }
    }

    /**
     * Returns the final transform to apply on the AOI or POI before to return it to the user.
     */
    private MathTransform toFinal() throws TransformException {
        MathTransform mt = domainToAny;
        if (isResultTransformed && geographicToAOI != null) {
            mt = MathTransforms.concatenate(geographicToAOI, mt);
        }
        return mt;
    }

    /**
     * Computes an envelope with coordinates equivalent to the given {@code areaOfInterest}, but
     * potentially shifted for intersecting the domain of validity specified at construction time.
     * The dimensions that may be shifted are the ones having an axis with wraparound meaning.
     * In order to perform this operation, the envelope may be temporarily converted to a geographic CRS
     * and converted back to its original CRS.
     *
     * <p>The coordinate reference system should be specified in the {@code areaOfInterest},
     * or (as a fallback) in the {@code domainOfValidity} specified at construction time.</p>
     *
     * <p>This method does not intersect the area of interest with the domain of validity.
     * It is up to the caller to compute that intersection after this method call, if desired.</p>
     *
     * @param  areaOfInterest  the envelope to potentially shift toward domain of validity.
     *         If a shift is needed, then given envelope will be replaced by a new envelope;
     *         the given envelope will not be modified.
     * @return envelope potentially expanded or shifted toward the domain of validity.
     * @throws TransformException if a coordinate conversion failed.
     *
     * @see GeneralEnvelope#simplify()
     */
    public GeneralEnvelope shift(Envelope areaOfInterest) throws TransformException {
        if (setIfNonNull(areaOfInterest.getCoordinateReferenceSystem())) {
            /*
             * If the coordinate reference system is a projected CRS, it will not have any wraparound axis.
             * We need to perform the verification in its base geographic CRS instead, and remember that we
             * may need to transform the result later.
             */
            final DirectPosition lowerCorner;
            final DirectPosition upperCorner;
            GeneralEnvelope shifted;            // To be initialized to a copy of 'areaOfInterest' when first needed.
            if (replaceCRS()) {
                shifted     = Envelopes.transform(geographicToAOI.inverse(), areaOfInterest);
                lowerCorner = shifted.getLowerCorner();
                upperCorner = shifted.getUpperCorner();
            } else {
                shifted     = null;
                lowerCorner = areaOfInterest.getLowerCorner();
                upperCorner = areaOfInterest.getUpperCorner();
            }
            /*
             * We will not read 'areaOfInterest' anymore after we got its two corner points (except for creating
             * a copy if `shifted` is still null). The following loop searches for "wraparound" axes.
             */
            final CoordinateSystem cs = crs.getCoordinateSystem();
            for (int i=cs.getDimension(); --i >= 0;) {
                final double period = range(cs, i);
                if (period > 0) {
                    /*
                     * Found an axis (typically the longitude axis) with wraparound range meaning.
                     * We are going to need the domain of validity in the same CRS than the AOI.
                     * Transform that envelope when first needed.
                     */
                    transformDomainToAOI();
                    /*
                     * "Unroll" the range. For example if we have [+160 … -170]° of longitude, we can replace by [160 … 190]°.
                     * We do not change the 'lower' or 'upper' value now in order to avoid rounding error. Instead we compute
                     * how many periods we need to add to those values. We adjust the side which results in the value closest
                     * to zero, in order to reduce rounding error if no more adjustment is done in the next block.
                     */
                    final double lower = lowerCorner.getOrdinate(i);
                    final double upper = upperCorner.getOrdinate(i);
                    double lowerCycles = 0;                             // In number of periods.
                    double upperCycles = 0;
                    double delta = upper - lower;
                    if (MathFunctions.isNegative(delta)) {              // Use 'isNegative' for catching [+0 … -0] range.
                        final double cycles = (delta == 0) ? -1 : Math.floor(delta / period);         // Always negative.
                        delta = cycles * period;
                        if (Math.abs(lower + delta) < Math.abs(upper - delta)) {
                            lowerCycles = cycles;                                    // Will subtract periods to 'lower'.
                        } else {
                            upperCycles = -cycles;                                   // Will add periods to 'upper'.
                        }
                    }
                    /*
                     * The range may be before or after the domain of validity. Compute the distance from current
                     * lower/upper coordinate to the coordinate of validity domain  (the sign tells us whether we
                     * are before or after). The cases can be:
                     *
                     *   ┌─────────────┬────────────┬────────────────────────────┬───────────────────────────────┐
                     *   │lowerIsBefore│upperIsAfter│ Meaning                    │ Action                        │
                     *   ├─────────────┼────────────┼────────────────────────────┼───────────────────────────────┤
                     *   │    false    │    false   │ AOI is inside valid area   │ Nothing to do                 │
                     *   │    true     │    true    │ AOI encompasses valid area │ Nothing to do                 │
                     *   │    true     │    false   │ AOI on left of valid area  │ Add positive amount of period │
                     *   │    false    │    true    │ AOI on right of valid area │ Add negative amount of period │
                     *   └─────────────┴────────────┴────────────────────────────┴───────────────────────────────┘
                     *
                     * We try to compute multiples of 'periods' instead than just adding or subtracting 'periods' once in
                     * order to support images that cover more than one period, for example images over 720° of longitude.
                     * It may happen for example if an image shows data under the trajectory of a satellite.
                     */
                    final double  validStart        = domainOfValidity.getMinimum(i);
                    final double  validEnd          = domainOfValidity.getMaximum(i);
                    final double  lowerToValidStart = ((validStart - lower) / period) - lowerCycles;    // In number of periods.
                    final double  upperToValidEnd   = ((validEnd   - upper) / period) - upperCycles;
                    final boolean lowerIsBefore     = (lowerToValidStart > 0);
                    final boolean upperIsAfter      = (upperToValidEnd   < 0);
                    if (lowerIsBefore != upperIsAfter) {
                        final double upperToValidStart = ((validStart - upper) / period) - upperCycles;
                        final double lowerToValidEnd   = ((validEnd   - lower) / period) - lowerCycles;
                        if (lowerIsBefore) {
                            /*
                             * Notation: ⎣x⎦=floor(x) and ⎡x⎤=ceil(x).
                             *
                             * We need to add an integer amount of 'period' to both sides in order to move the range
                             * inside the valid area. We need  ⎣lowerToValidStart⎦  for reaching the point where:
                             *
                             *     (validStart - period) < (new lower) ≦ validStart
                             *
                             * But we may add more because there will be no intersection without following condition:
                             *
                             *     (new upper) ≧ validStart
                             *
                             * That second condition is met by  ⎡upperToValidStart⎤. However adding more may cause the
                             * range to move the AOI completely on the right side of the domain of validity. We prevent
                             * that with a third condition:
                             *
                             *     (new lower) < validEnd
                             */
                            final double cycles = Math.min(Math.floor(lowerToValidEnd),
                                                  Math.max(Math.floor(lowerToValidStart),
                                                           Math.ceil (upperToValidStart)));
                            /*
                             * If after the shift we see that the following condition hold:
                             *
                             *     (new lower) + period < validEnd
                             *
                             * Then we may have a situation like below:
                             *                  ┌────────────────────────────────────────────┐
                             *                  │             Domain of validity             │
                             *                  └────────────────────────────────────────────┘
                             *   ┌────────────────────┐                                ┌─────
                             *   │  Area of interest  │                                │  AOI
                             *   └────────────────────┘                                └─────
                             *    ↖……………………………………………………………period……………………………………………………………↗︎
                             *
                             * The user may be requesting two extremums of the domain of validity. We can not express
                             * that with a single envelope. Instead, we will expand the Area Of Interest to encompass
                             * the full domain of validity.
                             */
                            if (cycles + 1 < lowerToValidEnd) {
                                upperCycles += Math.ceil(upperToValidEnd);
                            } else {
                                upperCycles += cycles;
                            }
                            lowerCycles += cycles;
                        } else {
                            /*
                             * Same reasoning than above with sign reverted and lower/upper variables interchanged.
                             * In this block, 'upperToValidEnd' and 'lowerToValidEnd' are negative, contrarily to
                             * above block where they were positive.
                             */
                            final double cycles = Math.max(Math.ceil (upperToValidStart),
                                                  Math.min(Math.ceil (upperToValidEnd),
                                                           Math.floor(lowerToValidEnd)));
                            if (cycles - 1 > upperToValidStart) {
                                lowerCycles += Math.floor(lowerToValidStart);
                            } else {
                                lowerCycles += cycles;
                            }
                            upperCycles += cycles;
                        }
                    }
                    /*
                     * If there is change to apply, copy the envelope when first needed and set the fields.
                     * If we never enter in this block, then 'areaOfInterest' will stay the envelope given
                     * at construction time.
                     */
                    if (lowerCycles != 0 || upperCycles != 0) {
                        isResultTransformed = true;
                        if (shifted == null) {
                            shifted = new GeneralEnvelope(areaOfInterest);
                        }
                        areaOfInterest = shifted;                           // 'shifted' may have been set before the loop.
                        shifted.setRange(i, lower + lowerCycles * period,   // TODO: use Math.fma in JDK9.
                                            upper + upperCycles * period);
                    }
                }
            }
        }
        return Envelopes.transform(toFinal(), areaOfInterest);
    }

    /**
     * Computes a position with coordinates equivalent to the given {@code pointOfInterest}, but
     * potentially shifted to interior of the domain of validity specified at construction time.
     * The dimensions that may be shifted are the ones having an axis with wraparound meaning.
     * In order to perform this operation, the position may be temporarily converted to a geographic CRS
     * and converted back to its original CRS.
     *
     * <p>The coordinate reference system should be specified in the {@code pointOfInterest},
     * or (as a fallback) in the {@code domainOfValidity} specified at construction time.</p>
     *
     * @param  pointOfInterest  the position to potentially shift to domain of validity interior.
     *         If a shift is needed, then the given position will be replaced by a new position;
     *         the given position will not be modified.
     * @return position potentially shifted to the domain of validity interior.
     * @throws TransformException if a coordinate conversion failed.
     */
    public DirectPosition shift(DirectPosition pointOfInterest) throws TransformException {
        if (setIfNonNull(pointOfInterest.getCoordinateReferenceSystem())) {
            DirectPosition shifted;
            if (replaceCRS()) {
                shifted = geographicToAOI.inverse().transform(pointOfInterest, null);
            } else {
                shifted = pointOfInterest;              // To be replaced by a copy of 'pointOfInterest' when first needed.
            }
            final CoordinateSystem cs = crs.getCoordinateSystem();
            for (int i=cs.getDimension(); --i >= 0;) {
                final double period = range(cs, i);
                if (period > 0) {
                    transformDomainToAOI();
                    final double x = shifted.getOrdinate(i);
                    double delta = domainOfValidity.getMinimum(i) - x;
                    if (delta > 0) {                                        // Test for point before domain of validity.
                        delta = Math.ceil(delta / period);
                    } else {
                        delta = domainOfValidity.getMaximum(i) - x;
                        if (delta < 0) {                                    // Test for point after domain of validity.
                            delta = Math.floor(delta / period);
                        } else {
                            continue;
                        }
                    }
                    if (delta != 0) {
                        isResultTransformed = true;
                        if (shifted == pointOfInterest) {
                            shifted = new GeneralDirectPosition(pointOfInterest);
                        }
                        pointOfInterest = shifted;                         // 'shifted' may have been set before the loop.
                        shifted.setOrdinate(i, x + delta * period);        // TODO: use Math.fma in JDK9.
                    }
                }
            }
        }
        return toFinal().transform(pointOfInterest, null);
    }
}
