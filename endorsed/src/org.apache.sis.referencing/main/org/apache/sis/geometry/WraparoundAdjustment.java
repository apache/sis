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
package org.apache.sis.geometry;

import java.util.Objects;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.metadata.privy.ReferencingServices;
import org.apache.sis.referencing.privy.ReferencingUtilities;
import org.apache.sis.referencing.privy.WraparoundAxesFinder;
import org.apache.sis.util.logging.Logging;


/**
 * An envelope or position converter making them more compatible with a given domain of validity.
 * For each axes having {@link org.opengis.referencing.cs.RangeMeaning#WRAPAROUND},
 * this class can add or subtract an integer number of periods (typically 360° of longitude)
 * in attempt to move positions or envelopes inside a domain of validity specified at construction time.
 *
 * <p>{@code WraparoundAdjustment} instances are not thread-safe.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.2
 */
public class WraparoundAdjustment {
    /**
     * The region inside which a given Area Of Interest (AOI) or Point Of Interest (POI) should be located.
     * This domain is specified at construction time and does not change.
     */
    private final ImmutableEnvelope domainOfValidity;

    /**
     * The domain of validity transformed to a CRS where wraparound axes exist, or {@code null} if not yet computed.
     * For example if {@link #domainOfValidity} is expressed in a projected CRS, then this envelope will be the same
     * domain but converted to the base geographic CRS in order to allow identification of wraparound axes.
     */
    private AbstractEnvelope shiftableDomain;

    /**
     * The geographic bounds of {@link #domainOfValidity}, or {@code null} if not applicable.
     * This is used for more accurate selection of a coordinate operation between a pair of CRS,
     * so it is okay to conservatively use an area covering the whole world.
     *
     * @see CRS#findOperation(CoordinateReferenceSystem, CoordinateReferenceSystem, GeographicBoundingBox)
     */
    private GeographicBoundingBox geographicDomain;

    /**
     * Whether {@link #geographicDomain} has been computed (result may be null).
     */
    private boolean geographicDomainKnown;

    /**
     * Coordinate reference system of the last Area Of Interest (AOI) or Point Of Interest (POI).
     * This is used for detecting when the input CRS changed.
     */
    private CoordinateReferenceSystem inputCRS;

    /**
     * Coordinate reference system of results, or {@code null} if unspecified.
     */
    private final CoordinateReferenceSystem resultCRS;

    /**
     * A transform from the {@link #inputCRS} to any destination user space at caller choice.
     * Objects returned by {@code shift(…)} methods will be transformed by this transform after all computations
     * have been finished. This is done in order to allow final transforms to be concatenated in a single step.
     *
     * <p>This field should be considered final if {@link #domainToInput} is non-null.</p>
     */
    private MathTransform inputToResult;

    /**
     * A transform from the {@link #domainOfValidity} CRS to the {@link #inputCRS} if it was explicitly specified,
     * or {@code null} otherwise. If non-null, all input envelopes or positions will be assumed in the CRS which
     * is the target of this transform. For performance reason, this assumption will not be verified.
     */
    private final MathTransform domainToInput;

    /**
     * If the input envelopes or positions need to be converted to a (usually) geographic CRS,
     * the transform to that CRS. Otherwise an identity transform. This is computed when first needed.
     */
    private MathTransform inputToShiftable;

    /**
     * The transform from the intermediate CRS to final objects, computed when first needed.
     *
     * @see #toResult(boolean)
     */
    private MathTransform shiftableToResult;

    /**
     * The span (maximum - minimum) of wraparound axes, with 0 value for axes that are not wraparound.
     * Initially null and computed when first needed. The length of this array may be shorter than the
     * CRS number of dimensions if all remaining axes are not wraparound axes.
     */
    private double[] periods;

    /**
     * Creates a new instance for adjusting Area Of Interest (AOI) or Point Of Interest (POI) to the given domain.
     * The results of {@code shift(…)} methods will be transformed (if needed) to the specified CRS.
     *
     * @param  domain  the region where a given area or point of interest should be located.
     * @param  target  the coordinate reference system of objects returned by {@code shift(…)} methods,
     *                 or {@code null} for the same CRS as the {@code domain} CRS.
     */
    public WraparoundAdjustment(final Envelope domain, final CoordinateReferenceSystem target) {
        domainOfValidity = ImmutableEnvelope.castOrCopy(Objects.requireNonNull(domain));
        resultCRS        = (target != null) ? target : domainOfValidity.getCoordinateReferenceSystem();
        domainToInput    = null;
    }

    /**
     * Creates a new instance with specified transforms from domain to the CRS of inputs, then to the CRS of outputs.
     * This constructor can be used when those transforms are known in advance; it avoids the cost of inferring them.
     * With this constructor, {@code WraparoundAdjustment} does <strong>not</strong> verify if a coordinate operation
     * is needed for a pair of CRS; it is caller's responsibility to ensure that input objects use the expected CRS.
     *
     * <h4>Example</h4>
     * In the context of {@link org.apache.sis.coverage.grid.GridGeometry}, the {@code domain} argument may be the
     * geospatial envelope of the grid and the {@code inputToResult} argument may be the "CRS to grid" transform.
     * This configuration allows to compute grid coordinates having more chances to be inside the grid.
     *
     * @param  domain          the region where a given area (AOI) or point of interest (POI) should be located.
     * @param  domainToInput   if the AOI or POI will use a different CRS than {@code domain}, the transform from
     *                         {@code domain} to the input CRS. Otherwise {@code null} for same CRS as the domain.
     * @param  inputToResult   a transform from the {@code domain} CRS to any user space at caller choice.
     *                         If {@code null}, the results will be expressed in same CRS as the inputs.
     */
    public WraparoundAdjustment(final Envelope domain, MathTransform domainToInput, MathTransform inputToResult) {
        domainOfValidity = ImmutableEnvelope.castOrCopy(Objects.requireNonNull(domain));
        if (domainToInput == null) {
            domainToInput = MathTransforms.identity(domainOfValidity.getDimension());
        }
        if (inputToResult == null) {
            inputToResult = MathTransforms.identity(domainToInput.getTargetDimensions());
        }
        this.domainToInput = domainToInput;
        this.inputToResult = inputToResult;
        this.resultCRS     = null;            // Not used by this instance.
    }

    /**
     * Finds a coordinate operation from the given source CRS to target CRS.
     * This method is invoked by all codes that need to find a coordinate operation.
     *
     * @param  source  the source CRS of the desired coordinate operation.
     * @param  target  the target CRS of the desired coordinate operation.
     * @return operation from {@code source} to {@code target}.
     * @throws TransformException if the operation cannot be computed.
     */
    private CoordinateOperation findOperation(final CoordinateReferenceSystem source,
                                              final CoordinateReferenceSystem target)
            throws TransformException
    {
        /*
         * The (source ≉ target) condition is a quick check for avoiding
         * unnecessary calculation of `geographicDomain` in common cases.
         */
        if (!geographicDomainKnown && !CRS.equivalent(source, target)) try {
            geographicDomainKnown = true;                       // Shall be set even in case of failure.
            geographicDomain = ReferencingServices.getInstance().setBounds(domainOfValidity, null, "shift");
        } catch (TransformException e) {
            Logging.ignorableException(Envelopes.LOGGER, WraparoundAdjustment.class, "<init>", e);
            // No more attempt will be done.
        }
        try {
            return CRS.findOperation(source, target, geographicDomain);
        } catch (FactoryException e) {
            throw new TransformException(e.getMessage(), e);
        }
    }

    /**
     * Initializes this {@code WraparoundAdjustment} for an AOI or POI having the given coordinate reference system.
     * If the given CRS is the same as the CRS given in last call to this method, then this method does nothing as
     * this {@code WraparoundAdjustment} is assumed already initialized. Otherwise this method performs those steps:
     *
     * <ul>
     *   <li>If the given coordinate reference system is a projected CRS,
     *       replaces it by another CRS where wraparound axes can be identified.</li>
     *   <li>Set {@link #shiftableDomain} to an envelope in above CRS.</li>
     *   <li>Set {@link #periods} to an array with the periods of wraparound axes.</li>
     *   <li>Set {@link #inputToResult} to the final transform to apply in {@code shift(…)} methods.</li>
     * </ul>
     *
     * @return whether there is at least one wraparound axis.
     */
    private boolean initialize(CoordinateReferenceSystem crs) throws TransformException {
        if (crs == null) {
            crs = domainOfValidity.getCoordinateReferenceSystem();
            if (crs == null && domainToInput == null) {
                /*
                 * If `inputCRS` is also null, `inputToResult` will not be initialized by next block.
                 * Initialize here with the assumption that following CRS as same as domain CRS:
                 *
                 *   - Input CRS, as specified in `shift(…)` method contract.
                 *   - Result CRS, as specified in constructor contract.
                 *
                 * Note that this field is considered modifiable only if `domainToInput` is null (see its javadoc).
                 */
                inputToResult = MathTransforms.identity(domainOfValidity.getDimension());
            }
        }
        if (crs != inputCRS) {
            inputCRS          = crs;
            periods           = null;
            inputToShiftable  = null;       // Will not be used if `crs` is null.
            shiftableToResult = null;
            shiftableDomain   = domainOfValidity;
            /*
             * Get the transform from input CRS (before replacement by "shiftable" CRS)
             * to the CRS of all results. It will be needed by all `shift(…)` methods.
             * Note that by convention, `inputToResult` is considered modifiable only
             * if `domainToInput` is null (see its javadoc).
             */
            if (domainToInput == null) {
                if (crs != null && resultCRS != null) {
                    inputToResult = findOperation(crs, resultCRS).getMathTransform();
                } else {
                    inputToResult = MathTransforms.identity(
                            (crs != null) ? ReferencingUtilities.getDimension(crs)
                                          : domainOfValidity.getDimension());
                }
            }
            /*
             * At this point we got a CRS which may have wraparound axes. Search for those axes.
             * The `periods` array will become non-null only if we find at least one such axis.
             */
            if (crs != null) {
                /*
                 * Replace the input CRS by an intermediate CRS where wraparound axes can be found.
                 * We try to select a CRS as close as possible (simplest transform) to the input.
                 */
                final WraparoundAxesFinder f = new WraparoundAxesFinder(crs);
                inputToShiftable = f.preferredToSpecified.inverse();
                if ((periods = f.periods()) != null) {
                    transformDomain(f.preferredCRS);
                }
            }
        }
        return (periods != null);
    }

    /**
     * Transforms {@link #domainOfValidity} to a CRS where wraparound axes can be identified.
     * This method should be invoked only when the caller detected at least one wraparound axis.
     *
     * <p>If a {@link #domainToInput} has been explicitly specified to the constructor,
     * that transform is unconditionally used and the {@code crs} argument is ignored.</p>
     *
     * <h4>Preconditions</h4>
     * <ul>
     *   <li>The {@link #inputToShiftable} transform must be initialized.</li>
     *   <li>The {@link #shiftableDomain} field is assumed initialized to {@link #domainOfValidity}.</li>
     * </ul>
     */
    private void transformDomain(final CoordinateReferenceSystem target) throws TransformException {
        final MathTransform domainToShiftable;
        if (domainToInput != null) {
            // Case of the constructor with `MathTransform` arguments.
            domainToShiftable = MathTransforms.concatenate(domainToInput, inputToShiftable);
            if (!domainToShiftable.isIdentity()) {
                shiftableDomain = Envelopes.transform(domainToShiftable, domainOfValidity);
            }
        } else {
            // Case of the constructor with `CoordinateReferenceSystem` argument.
            CoordinateOperation op = findOperation(domainOfValidity.getCoordinateReferenceSystem(), target);
            domainToShiftable = op.getMathTransform();
            if (!domainToShiftable.isIdentity()) {
                shiftableDomain = Envelopes.transform(op, domainOfValidity);
            }
        }
    }

    /**
     * Returns the final transform to apply on the AOI or POI before to return it to the user.
     * If {@link #inputCRS} is null, returns {@code null} for meaning "unknown transform".
     */
    private MathTransform toResult(final boolean isResultShifted) throws TransformException {
        if (isResultShifted) {
            if (shiftableToResult == null) {
                shiftableToResult = MathTransforms.concatenate(inputToShiftable.inverse(), inputToResult);
            }
            return shiftableToResult;
        } else {
            return inputToResult;
        }
    }

    /**
     * Computes an envelope with coordinates equivalent to the given {@code areaOfInterest}, but
     * potentially shifted for intersecting the domain of validity specified at construction time.
     * The dimensions that may be shifted are the ones having an axis with wraparound meaning.
     * In order to perform this operation, the envelope may be temporarily converted to a geographic CRS
     * and converted back to its original CRS.
     *
     * <p>The coordinate reference system should be specified in the {@code areaOfInterest}.
     * If not, then the CRS is assumed same as the CRS of the domain specified at construction time.</p>
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
        boolean isResultShifted = false;
        if (initialize(areaOfInterest.getCoordinateReferenceSystem())) {
            /*
             * If the coordinate reference system is a projected CRS, it will not have any wraparound axis.
             * We need to perform the verification in its base geographic CRS instead, and remember that we
             * may need to transform the result later.
             */
            final DirectPosition lowerCorner;
            final DirectPosition upperCorner;
            GeneralEnvelope shifted;            // To be initialized to a copy of `areaOfInterest` when first needed.
            if (inputToShiftable.isIdentity()) {
                shifted     = null;
                lowerCorner = areaOfInterest.getLowerCorner();
                upperCorner = areaOfInterest.getUpperCorner();
            } else {
                shifted     = Envelopes.transform(inputToShiftable, areaOfInterest);
                lowerCorner = shifted.getLowerCorner();
                upperCorner = shifted.getUpperCorner();
            }
            /*
             * We will not read `areaOfInterest` anymore after we got its two corner points (except for creating
             * a copy if `shifted` is still null). The following loop searches for "wraparound" axes.
             */
            for (int i=0; i<periods.length; i++) {
                final double period = periods[i];
                if (period > 0) {
                    /*
                     * Found an axis (typically the longitude axis) with wraparound range meaning.
                     * "Unroll" the range. For example if we have [+160 … -170]° of longitude, we can replace by [160 … 190]°.
                     * We do not change the `lower` or `upper` value now in order to avoid rounding error. Instead, we compute
                     * how many periods we need to add to those values. We adjust the side which results in the value closest
                     * to zero, in order to reduce rounding error if no more adjustment is done in the next block.
                     */
                    final double lower = lowerCorner.getOrdinate(i);
                    final double upper = upperCorner.getOrdinate(i);
                    double lowerCycles = 0;                             // In number of periods.
                    double upperCycles = 0;
                    double delta = upper - lower;
                    if (MathFunctions.isNegative(delta)) {              // Use `isNegative` for catching [+0 … -0] range.
                        final double cycles = (delta == 0) ? -1 : Math.floor(delta / period);         // Always negative.
                        delta = cycles * period;
                        if (Math.abs(lower + delta) < Math.abs(upper - delta)) {
                            lowerCycles = cycles;                                    // Will subtract periods to `lower`.
                        } else {
                            upperCycles = -cycles;                                   // Will add periods to `upper`.
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
                     *   │    true     │    false   │ AOI on left of valid area  │ Add positive number of period │
                     *   │    false    │    true    │ AOI on right of valid area │ Add negative number of period │
                     *   └─────────────┴────────────┴────────────────────────────┴───────────────────────────────┘
                     *
                     * We try to compute multiples of `periods` instead of just adding or subtracting `periods` once in
                     * order to support images that cover more than one period, for example images over 720° of longitude.
                     * It may happen for example if an image shows data under the trajectory of a satellite.
                     */
                    final double  validStart        = shiftableDomain.getMinimum(i);
                    final double  validEnd          = shiftableDomain.getMaximum(i);
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
                             * We need to add an integer number of `period` to both sides in order to move the range
                             * inside the valid area. We need  ⎣lowerToValidStart⎦  for reaching the point where:
                             *
                             *     (validStart - period) < (new lower) ≤ validStart
                             *
                             * But we may add more because there will be no intersection without following condition:
                             *
                             *     (new upper) ≥ validStart
                             *
                             * That second condition is met by  ⎡upperToValidStart⎤. However, adding more may cause the
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
                             * The user may be requesting two extremums of the domain of validity. We cannot express
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
                             * Same reasoning as above with sign reverted and lower/upper variables interchanged.
                             * In this block, `upperToValidEnd` and `lowerToValidEnd` are negative, contrarily to
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
                     * If we never enter in this block, then `areaOfInterest` will stay the envelope given
                     * at construction time.
                     */
                    if (lowerCycles != 0 || upperCycles != 0) {
                        isResultShifted = true;
                        if (shifted == null) {
                            shifted = new GeneralEnvelope(areaOfInterest);
                        }
                        areaOfInterest = shifted;           // `shifted` may have been set before the loop.
                        shifted.setRange(i, Math.fma(period, lowerCycles, lower),
                                            Math.fma(period, upperCycles, upper));
                    }
                }
            }
        }
        /*
         * Unconditionally apply the final transform (even if identity), unless
         * `inputCRS` is null in which case the transform to apply is unknown.
         */
        final MathTransform toResult = toResult(isResultShifted);
        if (toResult != null) {
            final GeneralEnvelope result = Envelopes.transform(toResult, areaOfInterest);
            result.setCoordinateReferenceSystem(resultCRS);
            return result;
        }
        return GeneralEnvelope.castOrCopy(areaOfInterest);
    }

    /**
     * Computes a position with coordinates equivalent to the given {@code pointOfInterest}, but
     * potentially shifted to interior of the domain of validity specified at construction time.
     * The dimensions that may be shifted are the ones having an axis with wraparound meaning.
     * In order to perform this operation, the position may be temporarily converted to a geographic CRS
     * and converted back to its original CRS.
     *
     * <p>The coordinate reference system should be specified in the {@code pointOfInterest}.
     * If not, then the CRS is assumed same as the CRS of the domain specified at construction time.</p>
     *
     * @param  pointOfInterest  the position to potentially shift to domain of validity interior.
     *         If a shift is needed, then the given position will be replaced by a new position;
     *         the given position will not be modified.
     * @return position potentially shifted to the domain of validity interior.
     * @throws TransformException if a coordinate conversion failed.
     */
    public DirectPosition shift(DirectPosition pointOfInterest) throws TransformException {
        boolean isResultShifted = false;
        if (initialize(pointOfInterest.getCoordinateReferenceSystem())) {
            DirectPosition shifted;
            if (inputToShiftable.isIdentity()) {
                shifted = pointOfInterest;          // To be replaced by a copy of `pointOfInterest` when first needed.
            } else {
                shifted = inputToShiftable.transform(pointOfInterest, null);
            }
            for (int i=0; i<periods.length; i++) {
                final double period = periods[i];
                if (period > 0) {
                    final double x = shifted.getOrdinate(i);
                    double delta = shiftableDomain.getMinimum(i) - x;
                    if (delta > 0) {                                        // Test for point before domain of validity.
                        delta = Math.ceil(delta / period);
                    } else {
                        delta = shiftableDomain.getMaximum(i) - x;
                        if (delta < 0) {                                    // Test for point after domain of validity.
                            delta = Math.floor(delta / period);
                        } else {
                            continue;
                        }
                    }
                    if (delta != 0) {
                        isResultShifted = true;
                        if (shifted == pointOfInterest) {
                            shifted = new GeneralDirectPosition(pointOfInterest);
                        }
                        pointOfInterest = shifted;                         // `shifted` may have been set before the loop.
                        shifted.setOrdinate(i, Math.fma(period, delta, x));
                    }
                }
            }
        }
        /*
         * Unconditionally apply the final transform (even if identity), unless
         * `inputCRS` is null in which case the transform to apply is unknown.
         */
        final MathTransform toResult = toResult(isResultShifted);
        if (toResult != null) {
            pointOfInterest = toResult.transform(pointOfInterest,
                    (resultCRS != null) ? new GeneralDirectPosition(resultCRS) : null);
        }
        return pointOfInterest;
    }
}
