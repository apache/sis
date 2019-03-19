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


/**
 * Adjustments applied on an envelope for handling wraparound axes. The adjustments consist in shifting
 * some axes by an integer amount of periods, typically (not necessarily) 360° of longitude.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final class WraparoundAdjustment {
    /**
     * The envelope to potentially shift in order to fit in the domain of validity. If a shift is needed, then
     * this envelope will be replaced by a new envelope; the user-specified envelope will not be modified.
     */
    private Envelope areaOfInterest;

    /**
     * If {@link #areaOfInterest} has been converted to a geographic CRS, the transformation back to its original CRS.
     * Otherwise {@code null}.
     */
    private MathTransform geographicToAOI;

    /**
     * Creates a new instance for adjusting the given envelope.
     * The given envelope will not be modified; a copy will be created if needed.
     *
     * @param  areaOfInterest  the envelope to potentially shift toward the domain of validity.
     */
    public WraparoundAdjustment(final Envelope areaOfInterest) {
        this.areaOfInterest = areaOfInterest;
    }

    /**
     * Returns the range (maximum - minimum) of the given axis if it has wraparound meaning,
     * or {@link Double#NaN} otherwise. This method implements a fallback for the longitude
     * axis if it does not declare the minimum and maximum values as expected.
     *
     * @param  cs         the coordinate system for which to get wraparound range, or {@code null}.
     * @param  dimension  dimension of the axis to test.
     * @return the wraparound range, or {@link Double#NaN} if none.
     */
    static double range(final CoordinateSystem cs, final int dimension) {
        if (cs != null) {
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
        }
        return Double.NaN;
    }

    /**
     * Computes an envelope with coordinates equivalent to the {@code areaOfInterest} specified
     * at construction time, but potentially shifted for intersecting the given domain of validity.
     * The dimensions that may be shifted are the ones having an axis with wraparound meaning.
     * The envelope may have been converted to a geographic CRS for performing this operation.
     *
     * <p>The coordinate reference system must be specified in the {@code areaOfInterest}
     * specified at construction time, or (as a fallback) in the given {@code domainOfValidity}.
     * If none of those envelopes have a CRS, then this method does nothing.</p>
     *
     * <p>This method does not intersect the area of interest with the domain of validity.
     * It is up to the caller to compute that intersection after this method call, if desired.</p>
     *
     * @param  domainOfValidity  the domain of validity, or {@code null} if none.
     * @param  validToAOI        if the envelopes do not use the same CRS, the transformation from {@code domainOfValidity}
     *                           to {@code areaOfInterest}. Otherwise {@code null}. This method does not check by itself if
     *                           a coordinate operation is needed; it must be supplied.
     * @throws TransformException if an envelope transformation was required but failed.
     *
     * @see GeneralEnvelope#simplify()
     */
    public void shiftInto(Envelope domainOfValidity, MathTransform validToAOI) throws TransformException {
        CoordinateReferenceSystem crs = areaOfInterest.getCoordinateReferenceSystem();
        if (crs == null) {
            crs = domainOfValidity.getCoordinateReferenceSystem();      // Assumed to apply to AOI too.
            if (crs == null) {
                return;
            }
        }
        /*
         * If the coordinate reference system is a projected CRS, it will not have any wraparound axis.
         * We need to perform the verification in its base geographic CRS instead, and remember that we
         * may need to transform the result later.
         */
        final MathTransform  projection;
        final DirectPosition lowerCorner;
        final DirectPosition upperCorner;
        GeneralEnvelope shifted = null;         // To be initialized to a copy of 'areaOfInterest' when first needed.
        if (crs instanceof ProjectedCRS) {
            final ProjectedCRS p = (ProjectedCRS) crs;
            crs = p.getBaseCRS();
            projection  = p.getConversionFromBase().getMathTransform();
            shifted     = Envelopes.transform(projection.inverse(), areaOfInterest);
            lowerCorner = shifted.getLowerCorner();
            upperCorner = shifted.getUpperCorner();
            if (validToAOI == null) {
                validToAOI = MathTransforms.identity(projection.getTargetDimensions());
            }
        } else {
            projection  = null;
            lowerCorner = areaOfInterest.getLowerCorner();
            upperCorner = areaOfInterest.getUpperCorner();
        }
        /*
         * We will not read 'areaOfInterest' anymore after we got its two corner points.
         * The following loop search for "wraparound" axis.
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
                if (validToAOI != null) {
                    if (projection != null) {
                        validToAOI = MathTransforms.concatenate(validToAOI, projection.inverse());
                    }
                    if (!validToAOI.isIdentity()) {
                        domainOfValidity = Envelopes.transform(validToAOI, domainOfValidity);
                    }
                    validToAOI = null;
                }
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
                final boolean upperIsAfter      = (upperToValidEnd < 0);
                if (lowerIsBefore != upperIsAfter) {
                    final double upperToValidStart = ((validStart - upper) / period) - upperCycles;
                    final double lowerToValidEnd   = ((validEnd   - lower) / period) - lowerCycles;
                    if (lowerIsBefore) {
                        /*
                         * We need to add an integer amount of 'period' to both sides in order to move the range
                         * inside the valid area. We need  ⎣lowerToValidStart⎦  for reaching the point where:
                         *
                         *     (validStart - period) < (new lower) ≦ validStart
                         *
                         * But we may add more because there will be no intersection without following condition:
                         *
                         *     (new upper) ≧ validStart
                         *
                         * That second condition is met by  ⎡upperToValidStart⎤. Note: ⎣x⎦=floor(x) and ⎡x⎤=ceil(x).
                         */
                        final double cycles = Math.max(Math.floor(lowerToValidStart), Math.ceil(upperToValidStart));
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
                        final double cycles = Math.min(Math.ceil(upperToValidEnd), Math.floor(lowerToValidEnd));
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
                    if (shifted == null) {
                        shifted = new GeneralEnvelope(areaOfInterest);
                    }
                    areaOfInterest  = shifted;                          // 'shifted' may have been set before the loop.
                    geographicToAOI = projection;
                    shifted.setRange(i, lower + lowerCycles * period,       // TODO: use Math.fma in JDK9.
                                        upper + upperCycles * period);
                }
            }
        }
    }

    /**
     * Returns the (potentially shifted and/or expanded) area of interest converted by the given transform.
     *
     * @param  mt  a transform from the CRS of the {@code areaOfInterest} given to the constructor.
     * @return the area of interest transformed by the given {@code MathTransform}.
     * @throws TransformException if the transformation failed.
     */
    public GeneralEnvelope result(MathTransform mt) throws TransformException {
        if (geographicToAOI != null) {
            mt = MathTransforms.concatenate(geographicToAOI, mt);
        }
        return Envelopes.transform(mt, areaOfInterest);
    }
}
