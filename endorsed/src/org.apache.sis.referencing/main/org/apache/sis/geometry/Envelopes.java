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


/*
 * Do not add dependency to java.awt.Rectangle2D in this class, because not every platforms
 * support Java2D (e.g. Android),  or applications that do not need it may want to avoid to
 * force installation of the Java2D module (e.g. JavaFX/SWT).
 */
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.LinkedHashMap;
import java.util.ConcurrentModificationException;
import java.util.logging.Logger;
import java.time.Instant;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.OperationNotFoundException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.util.FactoryException;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.internal.shared.ReferencingServices;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.AbstractCoordinateOperation;
import org.apache.sis.referencing.operation.transform.AbstractMathTransform;
import org.apache.sis.referencing.operation.transform.WraparoundTransform;
import org.apache.sis.referencing.internal.shared.CoordinateOperations;
import org.apache.sis.referencing.internal.shared.DirectPositionView;
import org.apache.sis.referencing.internal.shared.TemporalAccessor;
import org.apache.sis.system.Loggers;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;
import org.apache.sis.measure.Range;
import org.apache.sis.math.MathFunctions;

import static org.apache.sis.util.StringBuilders.trimFractionalPart;


/**
 * Transforms envelopes to new Coordinate Reference Systems, and miscellaneous utilities.
 *
 * <h2>Envelope transformations</h2>
 * All {@code transform(…)} methods in this class take in account the curvature of the transformed shape.
 * For example, the shape of a geographic envelope (figure below on the left side) is not rectangular in a
 * conic projection (figure below on the right side). In order to get the envelope represented by the red
 * rectangle, projecting the four corners of the geographic envelope is not sufficient since we would miss
 * the southerner part.
 *
 * <table class="sis">
 *   <caption>Example of curvature induced by a map projection</caption>
 *   <tr>
 *     <th>Envelope before map projection</th>
 *     <th>Shape of the projected envelope</th>
 *   </tr><tr>
 *     <td><img src="doc-files/GeographicArea.png" alt="Envelope in a geographic CRS"></td>
 *     <td><img src="doc-files/ConicArea.png" alt="Shape of the envelope transformed in a conic projection"></td>
 *   </tr>
 * </table>
 *
 * Apache SIS tries to detect the curvature by transforming intermediate points in addition to the corners.
 * While optional, it is strongly recommended that all {@code MathTransform} implementations involved in the
 * operation (directly or indirectly) support {@linkplain MathTransform#derivative(DirectPosition) derivative},
 * for more accurate calculation of curve extremum. This is the case of most Apache SIS implementations.
 *
 * <p>The {@code transform(…)} methods in this class expect an arbitrary {@link Envelope} with <strong>one</strong>
 * of the following arguments: {@link MathTransform}, {@link CoordinateOperation} or {@link CoordinateReferenceSystem}.
 * The recommended method is the one expecting a {@code CoordinateOperation} object,
 * since it contains sufficient information for handling the cases of envelopes that encompass a pole.
 * The method expecting a {@code CoordinateReferenceSystem} object is merely a convenience method that
 * infers the coordinate operation itself, but at the cost of performance if the same operation needs
 * to be applied on many envelopes.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 1.6
 *
 * @see org.apache.sis.metadata.iso.extent.Extents
 * @see CRS
 *
 * @since 0.3
 */
public final class Envelopes {
    /**
     * The logger for geometry operations.
     */
    static final Logger LOGGER = Logger.getLogger(Loggers.GEOMETRY);

    /**
     * Fraction of the axis span to accept as close enough to an envelope boundary. This is used for coordinates
     * that are suppose to be on a boundary, for checking if it is really on the boundary side where it should be.
     * For example, on the longitude axis, bounds are -180° and +180° with wraparound meaning and the span is 360°.
     * A {@code SPAN_FRACTION_AS_BOUND} value of 0.25 means that we accept a margin of 0.25 × 360° = 90° on each
     * side: longitudes between -180 and -90 are clipped to the -180° bounds, and longitudes between +180 and +90
     * and clipped to the +180° bounds. We use a large fraction because we use it in contexts where the longitude
     * is not supposed to be in the envelope interior. We could use a 0.5 value for clipping to the nearest bound,
     * but a smaller value is used for safety.
     */
    static final double SPAN_FRACTION_AS_BOUND = 0.25;

    /**
     * Do not allow instantiation of this class.
     */
    private Envelopes() {
    }

    /**
     * Puts together a list of envelopes, each of them using an independent coordinate reference system.
     * The dimension of the returned envelope is the sum of the dimension of all components.
     * If all components have a coordinate reference system, then the returned envelope will
     * have a compound coordinate reference system.
     *
     * @param  components  the envelopes to aggregate in a single envelope, in the given order.
     * @return the aggregation of all given envelopes.
     * @throws FactoryException if the geodetic factory failed to create the compound CRS.
     *
     * @see org.apache.sis.referencing.CRS#compound(CoordinateReferenceSystem...)
     * @see org.apache.sis.referencing.operation.transform.MathTransforms#compound(MathTransform...)
     *
     * @since 1.0
     */
    public static Envelope compound(final Envelope... components) throws FactoryException {
        ArgumentChecks.ensureNonNull("components", components);
        int sum = 0;
        for (int i=0; i<components.length; i++) {
            final Envelope env = components[i];
            ArgumentChecks.ensureNonNullElement("components", i, env);
            sum += env.getDimension();
        }
        final var compound = new GeneralEnvelope(sum);
        CoordinateReferenceSystem[] crsComponents = null;
        int firstAffectedCoordinate = 0;
        for (int i=0; i<components.length; i++) {
            final Envelope env = components[i];
            final int dim = env.getDimension();
            compound.subEnvelope(firstAffectedCoordinate, firstAffectedCoordinate += dim).setEnvelope(env);
            if (i == 0) {
                final CoordinateReferenceSystem crs = env.getCoordinateReferenceSystem();
                if (crs != null) {
                    crsComponents = new CoordinateReferenceSystem[components.length];
                    crsComponents[0] = crs;
                }
            } else if (crsComponents != null) {
                if ((crsComponents[i] = env.getCoordinateReferenceSystem()) == null) {
                    crsComponents = null;               // Not all CRS are non-null.
                }
            }
        }
        if (firstAffectedCoordinate != sum) {
            // Should never happen unless the number of dimensions of an envelope changed during iteration.
            throw new ConcurrentModificationException();
        }
        if (crsComponents != null) {
            compound.setCoordinateReferenceSystem(CRS.compound(crsComponents));
        }
        return compound;
    }

    /**
     * Computes the union of all given envelopes, transforming them to a common CRS if necessary.
     * If all envelopes use {@linkplain CRS#equivalent equivalent} <abbr>CRS</abbr>,
     * or if the CRS of all envelopes is {@code null}, then the {@linkplain GeneralEnvelope#add(Envelope)
     * union is computed} without transforming any envelope. Otherwise, all envelopes are transformed
     * to a {@linkplain CRS#suggestCommonTarget common CRS} before union is computed.
     * The CRS of the returned envelope may different than the CRS of all given envelopes.
     *
     * @param  envelopes  the envelopes for which to compute union. Null elements are ignored.
     * @return union of given envelopes, or {@code null} if the given array does not contain non-null elements.
     * @throws TransformException if this method cannot determine a common CRS, or if a transformation failed.
     *
     * @see GeneralEnvelope#add(Envelope)
     *
     * @since 1.0
     */
    public static GeneralEnvelope union(final Envelope... envelopes) throws TransformException {
        return EnvelopeReducer.UNION.reduce(envelopes);
    }

    /**
     * Computes the intersection of all given envelopes, transforming them to a common CRS if necessary.
     * If all envelopes use {@linkplain CRS#equivalent equivalent} <abbr>CRS</abbr>,
     * or if the CRS of all envelopes is {@code null}, then the {@linkplain GeneralEnvelope#intersect(Envelope)
     * intersection is computed} without transforming any envelope. Otherwise, all envelopes are transformed
     * to a {@linkplain CRS#suggestCommonTarget common CRS} before intersection is computed.
     * The CRS of the returned envelope may different than the CRS of all given envelopes.
     *
     * @param  envelopes  the envelopes for which to compute intersection. Null elements are ignored.
     * @return intersection of given envelopes, or {@code null} if the given array does not contain non-null elements.
     * @throws TransformException if this method cannot determine a common CRS, or if a transformation failed.
     *
     * @see GeneralEnvelope#intersect(Envelope)
     *
     * @since 1.0
     */
    public static GeneralEnvelope intersect(final Envelope... envelopes) throws TransformException {
        return EnvelopeReducer.INTERSECT.reduce(envelopes);
    }

    /**
     * Finds a mathematical operation from the CRS of the given source envelope to the CRS of the given target envelope.
     * For non-null georeferenced envelopes, this method is equivalent to the following code with {@code areaOfInterest}
     * computed as the union of the two envelopes:
     *
     * <blockquote><code>return {@linkplain CRS#findOperation(CoordinateReferenceSystem, CoordinateReferenceSystem,
     * GeographicBoundingBox)} CRS.findOperation(source.getCoordinateReferenceSystem(), target.getCoordinateReferenceSystem(),
     * <var>areaOfInterest</var>)</code></blockquote>
     *
     * If at least one envelope is null or has no CRS, then this method returns {@code null}.
     *
     * @param  source  the source envelope, or {@code null}.
     * @param  target  the target envelope, or {@code null}.
     * @return the mathematical operation from {@code source} CRS to {@code target} CRS,
     *         or {@code null} if at least one argument is null or has no CRS.
     * @throws OperationNotFoundException if no operation was found between the given pair of CRS.
     * @throws FactoryException if the operation cannot be created for another reason.
     *
     * @see CRS#findOperation(CoordinateReferenceSystem, CoordinateReferenceSystem, GeographicBoundingBox)
     *
     * @since 1.0
     */
    public static CoordinateOperation findOperation(final Envelope source, final Envelope target) throws FactoryException {
        if (source != null && target != null) {
            final CoordinateReferenceSystem sourceCRS, targetCRS;
            if ((sourceCRS = source.getCoordinateReferenceSystem()) != null &&
                (targetCRS = target.getCoordinateReferenceSystem()) != null)
            {
                /*
                 * Computing the area of interest (AOI) unconditionally would be harmless, but it is useless if the CRS
                 * are the same since the result will be identity anyway. Conversely we could skip AOI computation more
                 * often for example with the following condition instead of !=:
                 *
                 *     !Utilities.deepEquals(sourceCRS, targetCRS, ComparisonMode.ALLOW_VARIANT)
                 *
                 * but it would not be very advantageous if testing the condition is almost as long as computing the AOI.
                 * For now we keep != as a very cheap test which will work quite often.
                 */
                DefaultGeographicBoundingBox areaOfInterest = null;
                if (sourceCRS != targetCRS) try {
                    final DefaultGeographicBoundingBox targetAOI;
                    final ReferencingServices converter = ReferencingServices.getInstance();
                    areaOfInterest = converter.setBounds(source, null, "findOperation");                // Should be first.
                    targetAOI      = converter.setBounds(target, null, "findOperation");
                    if (areaOfInterest == null) {
                        areaOfInterest = targetAOI;
                    } else if (targetAOI != null) {
                        areaOfInterest.add(targetAOI);
                    }
                } catch (TransformException e) {
                    /*
                     * Note: we may succeed to transform `source` and fail to transform `target` to geographic bounding box,
                     * but the opposite is unlikely because `source` should not have less dimensions than `target`.
                     */
                    Logging.recoverableException(LOGGER, Envelopes.class, "findOperation", e);
                }
                return CRS.findOperation(sourceCRS, targetCRS, areaOfInterest);
            }
        }
        return null;
    }

    /**
     * Invoked when a recoverable exception occurred.
     * Those exceptions must be minor enough that they can be silently ignored in most cases.
     */
    static void recoverableException(final Class<?> caller, final TransformException exception) {
        Logging.recoverableException(LOGGER, caller, "transform", exception);
    }

    /**
     * A buckle method for calculating derivative and coordinate transformation in a single step,
     * if the given {@code derivative} argument is {@code true}.
     *
     * @param  transform  the transform to use.
     * @param  srcPts     the array containing the source coordinate at offset 0.
     * @param  dstPts     the array into which the transformed coordinate is returned.
     * @param  dstOff     the offset to the location of the transformed point that is stored in the destination array.
     * @param  derivate   {@code true} for computing the derivative, or {@code false} if not needed.
     * @return the matrix of the transform derivative at the given source position,
     *         or {@code null} if the {@code derivate} argument is {@code false}.
     * @throws TransformException if the point cannot be transformed
     *         or if a problem occurred while calculating the derivative.
     */
    static Matrix derivativeAndTransform(final MathTransform transform, final double[] srcPts,
            final double[] dstPts, final int dstOff, final boolean derivate) throws TransformException
    {
        if (transform instanceof AbstractMathTransform) {
            return ((AbstractMathTransform) transform).transform(srcPts, 0, dstPts, dstOff, derivate);
        }
        // Derivative must be calculated before to transform the coordinate.
        final Matrix derivative = derivate ? transform.derivative(
                new DirectPositionView.Double(srcPts, 0, transform.getSourceDimensions())) : null;
        transform.transform(srcPts, 0, dstPts, dstOff, 1);
        return derivative;
    }

    /**
     * Transforms the given envelope to the specified CRS. If any argument is null, or if the
     * {@linkplain GeneralEnvelope#getCoordinateReferenceSystem() envelope CRS} is null or the
     * same instance as the given target CRS, then the given envelope is returned unchanged.
     * Otherwise a new transformed envelope is returned.
     *
     * <h4>Performance tip</h4>
     * If there is many envelopes to transform with the same source and target CRS, then it is more efficient
     * to get the {@link CoordinateOperation} or {@link MathTransform} instance once and invoke one of the
     * others {@code transform(…)} methods.
     *
     * @param  envelope   the envelope to transform (may be {@code null}).
     * @param  targetCRS  the target CRS (may be {@code null}).
     * @return a new transformed envelope, or directly {@code envelope} if no change was required.
     * @throws TransformException if a transformation was required and failed.
     *
     * @since 0.5
     */
    public static Envelope transform(Envelope envelope, final CoordinateReferenceSystem targetCRS)
            throws TransformException
    {
        if (envelope != null && targetCRS != null) {
            final CoordinateReferenceSystem sourceCRS = envelope.getCoordinateReferenceSystem();
            if (sourceCRS != targetCRS) {
                if (sourceCRS == null) {
                    // Slight optimization: just copy the given Envelope.
                    envelope = new GeneralEnvelope(envelope);
                    ((GeneralEnvelope) envelope).setCoordinateReferenceSystem(targetCRS);
                } else {
                    // TODO: create an CoordinateOperationContext with the envelope as geographic area.
                    //       May require that we optimize the search for CoordinateOperation with non-null context first.
                    //       See https://issues.apache.org/jira/browse/SIS-442
                    final CoordinateOperation operation;
                    try {
                        operation = CRS.findOperation(sourceCRS, targetCRS, null);
                    } catch (FactoryException exception) {
                        throw new TransformException(Errors.format(Errors.Keys.CanNotTransformEnvelope), exception);
                    }
                    envelope = transform(operation, envelope);
                }
                assert Utilities.deepEquals(targetCRS, envelope.getCoordinateReferenceSystem(), ComparisonMode.DEBUG);
            }
        }
        return envelope;
    }

    /**
     * Shared implementation of {@link #transform(MathTransform, Envelope)} and
     * {@link #transformWithWraparound(MathTransform, Envelope)} public methods.
     * Offers also the opportunity to save the transformed center coordinates.
     *
     * @param  transform  the transform to use.
     * @param  envelope   envelope to transform. This envelope will not be modified.
     * @param  targetPt   after this method call, the center of the source envelope transformed to the target CRS.
     *                    The length of this array must be the number of target dimensions.
     *                    May be {@code null} if this information is not needed.
     * @param  results    where to store the individual results when the transform contains wraparound steps,
     *                    or {@code null} for computing the union of all results instead.
     * @return the transformed envelope. May be {@code null} if {@code results} was non-null.
     */
    private static GeneralEnvelope transform(final MathTransform transform, final Envelope envelope,
            double[] targetPt, final Map<Parameters, GeneralEnvelope> results) throws TransformException
    {
        if (transform.isIdentity()) {
            /*
             * Slight optimization: Just copy the envelope. Note that we need to set the CRS
             * to null because we don't know what the target CRS was supposed to be. Even if
             * an identity transform often implies that the target CRS is the same one as the
             * source CRS, it is not always the case. The metadata may be differents, or the
             * transform may be a datum shift without Bursa-Wolf parameters, etc.
             */
            final var transformed = new GeneralEnvelope(envelope);
            transformed.setCoordinateReferenceSystem(null);
            if (targetPt != null) {
                for (int i=envelope.getDimension(); --i>=0;) {
                    targetPt[i] = transformed.getMedian(i);
                }
            }
            return transformed;
        }
        /*
         * Checks argument validity: envelope and math transform dimensions must be consistent.
         */
        final int sourceDim = transform.getSourceDimensions();
        final int targetDim = transform.getTargetDimensions();
        if (envelope.getDimension() != sourceDim) {
            throw new MismatchedDimensionException(Errors.format(Errors.Keys.MismatchedDimension_2,
                      sourceDim, envelope.getDimension()));
        }
        /*
         * Allocates all needed objects. The power of 3 below is because the following `while` loop
         * uses a `pointIndex` to be interpreted as a number in base 3 (see the comment inside the loop).
         * The coordinate tuple to transform must be initialized to the minimal coordinate values.
         * This coordinate will be updated in the `switch` statement inside the `while` loop.
         */
        if (sourceDim >= 20) {          // Maximal value supported by Formulas.pow3(int) is 19.
            throw new ArithmeticException(Errors.format(Errors.Keys.ExcessiveNumberOfDimensions_1, sourceDim));
        }
        boolean isDerivativeSupported = true;
        DirectPosition  temporary     = null;
        GeneralEnvelope transformed   = null;
        final Matrix[]  derivatives   = new Matrix[Math.toIntExact(MathFunctions.pow(3, sourceDim))];
        final double[]  coordinates   = new double[Math.multiplyExact(derivatives.length, targetDim)];
        final double[]  sourcePt      = new double[sourceDim];
        // A window over a single coordinate in the `coordinates` array.
        final var ordinatesView = new DirectPositionView.Double(coordinates, 0, targetDim);
        final var wc = new WraparoundInEnvelope.Controller(transform);
        do {
            for (int i=sourceDim; --i>=0;) {
                sourcePt[i] = envelope.getMinimum(i);
            }
            /*
             * Iterates over every minimal, maximal and median coordinate values (3 points) along each dimension.
             * The total number of iterations is 3 ^ (number of source dimensions).
             */
nextPoint:  for (int pointIndex = 0;;) {                // Break condition at the end of this block.
                /*
                 * Compute the derivative (optional operation). If this operation fails, we will
                 * set a flag to `false` so we don't try again for all remaining points. We try
                 * to compute the derivative and the transformed point in a single operation if
                 * we can. If we cannot, we will compute those two information separately.
                 *
                 * Note that the very last point to be projected must be the envelope center.
                 * There is usually no need to calculate the derivative for that last point,
                 * but we let it does anyway for safety.
                 */
                final int offset = pointIndex * targetDim;
                try {
                    derivatives[pointIndex] = derivativeAndTransform(wc.transform,
                            sourcePt, coordinates, offset, isDerivativeSupported);
                } catch (TransformException e) {
                    if (!isDerivativeSupported) {
                        throw e;                    // Derivative were already disabled, so something went wrong.
                    }
                    isDerivativeSupported = false;
                    wc.transform.transform(sourcePt, 0, coordinates, offset, 1);
                    recoverableException(Envelopes.class, e);       // Log only if the above call was successful.
                }
                /*
                 * The transformed point has been saved for future reuse after the enclosing
                 * `for(;;)` loop. Now add the transformed point to the destination envelope.
                 */
                if (transformed == null) {
                    transformed = new GeneralEnvelope(targetDim);
                    for (int i=0; i<targetDim; i++) {
                        final double value = coordinates[offset + i];
                        transformed.setRange(i, value, value);
                    }
                } else {
                    ordinatesView.offset = offset;
                    transformed.add(ordinatesView);
                }
                /*
                 * Get the next point coordinate. The `coordinateIndex` variable is an index in base 3
                 * having a number of digits equals to the number of source dimensions.  For example, a
                 * 4-D space have indexes ranging from "0000" to "2222" (numbers in base 3). The digits
                 * are then mapped to minimal (0), maximal (1) or central (2) coordinates. The outer loop
                 * stops when the counter roll back to "0000". Note that `targetPt` will be set to value
                 * of the last projected point, which must be the envelope center identified by "2222"
                 * in the 4-D case.
                 */
                int indexBase3 = ++pointIndex;
                for (int dim = sourceDim; --dim >= 0; indexBase3 /= 3) {
                    switch (indexBase3 % 3) {
                        case 0:  sourcePt[dim] = envelope.getMinimum(dim); continue;            // Continue the loop.
                        case 1:  sourcePt[dim] = envelope.getMaximum(dim); continue nextPoint;
                        case 2:  sourcePt[dim] = envelope.getMedian (dim); continue nextPoint;
                        default: throw new AssertionError(indexBase3);                          // Should never happen
                    }
                }
                assert pointIndex == derivatives.length : pointIndex;
                break;
            }
            /*
             * At this point we finished to build an envelope from all sampled positions. Now iterate
             * over all points. For each point, iterate over all line segments from that point to a
             * neighbor median point.  Use the derivate information for approximating the transform
             * behavior in that area by a cubic curve. We can then find analytically the curve extremum.
             *
             * The same technic is applied in transform(MathTransform, Rectangle2D), except that in
             * the Rectangle2D case the calculation was bundled right inside the main loop in order
             * to avoid the need for storage.
             */
            final var sourceView = new DirectPositionView.Double(sourcePt, 0, sourceDim);
            final var extremum = new CurveExtremum();
            for (int pointIndex=0; pointIndex < derivatives.length; pointIndex++) {
                final Matrix D1 = derivatives[pointIndex];
                if (D1 != null) {
                    int indexBase3 = pointIndex, power3 = 1;
                    for (int i = sourceDim; --i >= 0; indexBase3 /= 3, power3 *= 3) {
                        final int digitBase3 = indexBase3 % 3;
                        // Process only if we are not already located on the median along the dimension i.
                        if (digitBase3 != 2) {
                            final int medianIndex = pointIndex + power3 * (2 - digitBase3);
                            final Matrix D2 = derivatives[medianIndex];
                            if (D2 != null) {
                                final double xmin = envelope.getMinimum(i);
                                final double xmax = envelope.getMaximum(i);
                                final double x2   = envelope.getMedian (i);
                                final double x1   = (digitBase3 == 0) ? xmin : xmax;
                                final int offset1 = targetDim * pointIndex;
                                final int offset2 = targetDim * medianIndex;
                                for (int j=0; j<targetDim; j++) {
                                    extremum.resolve(x1, coordinates[offset1 + j], D1.getElement(j,i),
                                                     x2, coordinates[offset2 + j], D2.getElement(j,i));
                                    boolean isP2 = false;
                                    do {
                                        // Executed exactly twice, one for each extremum point.
                                        final double x = isP2 ? extremum.ex2 : extremum.ex1;
                                        if (x > xmin && x < xmax) {
                                            final double y = isP2 ? extremum.ey2 : extremum.ey1;
                                            if (y < transformed.getMinimum(j) ||
                                                y > transformed.getMaximum(j))
                                            {
                                                /*
                                                 * At this point, we have determined that adding the extremum point
                                                 * would expand the envelope. However, we will not add that point
                                                 * directly because its position may not be quite right (since we
                                                 * used a cubic curve approximation). Instead, we project the point
                                                 * on the envelope border which is located vis-à-vis the extremum.
                                                 */
                                                for (int ib3 = pointIndex, dim = sourceDim; --dim >= 0; ib3 /= 3) {
                                                    final double coordinate;
                                                    if (dim == i) {
                                                        coordinate = x;                       // Position of the extremum.
                                                    } else switch (ib3 % 3) {
                                                        case 0:  coordinate = envelope.getMinimum(dim); break;
                                                        case 1:  coordinate = envelope.getMaximum(dim); break;
                                                        case 2:  coordinate = envelope.getMedian (dim); break;
                                                        default: throw new AssertionError(ib3);     // Should never happen.
                                                    }
                                                    sourcePt[dim] = coordinate;
                                                }
                                                temporary = wc.transform.transform(sourceView, temporary);
                                                transformed.add(temporary);
                                            }
                                        }
                                    } while ((isP2 = !isP2) == true);
                                }
                            }
                        }
                    }
                    derivatives[pointIndex] = null;                 // Let GC do its job earlier.
                }
            }
            /*
             * Copy the coordinate of the center point. We take the point of the
             * first iteration because it is the one before translation is applied.
             */
            if (targetPt != null) {
                System.arraycopy(coordinates, coordinates.length - targetDim, targetPt, 0, targetDim);
                targetPt = null;
            }
            /*
             * If the caller wants individual results, store them in the list.
             * Do not filter empty envelopes, because some callers such as
             * `GridExtent` have algorithms for completing empty envelopes.
             */
            if (results != null) {
                final GeneralEnvelope e = results.putIfAbsent(wc.state(), transformed);
                if (e != null) e.add(transformed);    // Should never happen, but let be safe.
                transformed = null;
            }
        } while (wc.translate());
        return transformed;
    }

    /**
     * Transforms an envelope using the given coordinate operation.
     * The transformation is only approximated: the returned envelope may be bigger than the
     * smallest possible bounding box, but should not be smaller in most cases.
     *
     * <p>This method can handle the case where the envelope contains the North or South pole,
     * or when it cross the ±180° longitude.</p>
     *
     * <h4>Usage note</h4>
     * If the envelope CRS is non-null, then the caller should ensure that the operation source CRS
     * is the same as the envelope CRS. In case of mismatch, this method transforms the envelope
     * to the operation source CRS before to apply the operation. This extra step may cause a lost
     * of accuracy. In order to prevent this method from performing such pre-transformation (if not desired),
     * callers can ensure that the envelope CRS is {@code null} before to call this method.
     *
     * @param  operation  the operation to use.
     * @param  envelope   envelope to transform, or {@code null}. This envelope will not be modified.
     * @return the transformed envelope, or {@code null} if {@code envelope} was null.
     * @throws TransformException if a transform failed.
     *
     * @see #transform(MathTransform, Envelope)
     *
     * @since 0.5
     */
    public static GeneralEnvelope transform(final CoordinateOperation operation, Envelope envelope)
            throws TransformException
    {
        ArgumentChecks.ensureNonNull("operation", operation);
        if (envelope == null) {
            return null;
        }
        boolean isOperationComplete = true;
        final CoordinateReferenceSystem sourceCRS = operation.getSourceCRS();
        if (sourceCRS != null) {
            final CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
            if (crs != null && !CRS.equivalent(crs, sourceCRS)) {
                /*
                 * Argument-check: the envelope CRS seems inconsistent with the given operation.
                 * However, we need to push the check a little bit further, since 3D-GeographicCRS
                 * are considered not equal to CompoundCRS[2D-GeographicCRS + ellipsoidal height].
                 * Checking for identity MathTransform is a more powerfull (but more costly) check.
                 * Since we have the MathTransform, perform an opportunist envelope transform if it
                 * happen to be required.
                 */
                final MathTransform mt;
                try {
                    mt = CRS.findOperation(crs, sourceCRS, null).getMathTransform();
                } catch (FactoryException e) {
                    throw new TransformException(Errors.format(Errors.Keys.CanNotTransformEnvelope), e);
                }
                if (!mt.isIdentity()) {
                    isOperationComplete = false;
                    envelope = transform(mt, envelope);
                }
            }
        }
        final MathTransform mt = operation.getMathTransform();
        final double[] centerPt = new double[mt.getTargetDimensions()];
        final GeneralEnvelope transformed = transform(mt, envelope, centerPt, null);
        /*
         * If the source envelope crosses the expected range of valid coordinates, also projects
         * the range bounds as a safety. Example: if the source envelope goes from 150 to 200°E,
         * some map projections will interpret 200° as if it was -160°, and consequently produce
         * an envelope which do not include the 180°W extremum. We will add those extremum points
         * explicitly as a safety. It may leads to bigger than necessary target envelope, but the
         * contract is to include at least the source envelope, not to return the smallest one.
         */
        if (sourceCRS != null) {
            final CoordinateSystem cs = sourceCRS.getCoordinateSystem();
            if (cs != null) {                           // Should never be null, but check as a paranoiac safety.
                DirectPosition sourcePt = null;
                DirectPosition targetPt = null;
                final int dimension = cs.getDimension();
                for (int i=0; i<dimension; i++) {
                    final CoordinateSystemAxis axis = cs.getAxis(i);
                    if (axis == null) {                 // Should never be null, but check as a paranoiac safety.
                        continue;
                    }
                    final double min = envelope.getMinimum(i);
                    final double max = envelope.getMaximum(i);
                    final double  v1 = axis.getMinimumValue();
                    final double  v2 = axis.getMaximumValue();
                    final boolean b1 = (v1 > min && v1 < max);
                    final boolean b2 = (v2 > min && v2 < max);
                    if (!b1 && !b2) {
                        continue;
                    }
                    if (sourcePt == null) {
                        sourcePt = new GeneralDirectPosition(dimension);
                        for (int j=0; j<dimension; j++) {
                            sourcePt.setCoordinate(j, envelope.getMedian(j));
                        }
                    }
                    if (b1) {
                        sourcePt.setCoordinate(i, v1);
                        transformed.add(targetPt = mt.transform(sourcePt, targetPt));
                    }
                    if (b2) {
                        sourcePt.setCoordinate(i, v2);
                        transformed.add(targetPt = mt.transform(sourcePt, targetPt));
                    }
                    sourcePt.setCoordinate(i, envelope.getMedian(i));
                }
            }
        }
        /*
         * Now takes the target CRS in account...
         */
        final CoordinateReferenceSystem targetCRS = operation.getTargetCRS();
        if (targetCRS == null) {
            return transformed;
        }
        transformed.setCoordinateReferenceSystem(targetCRS);
        final CoordinateSystem targetCS = targetCRS.getCoordinateSystem();
        if (targetCS == null) {
            // It should be an error, but we keep this method tolerant.
            return transformed;
        }
        /*
         * Checks for singularity points. For example, the south pole is a singularity point in
         * geographic CRS because it is located at the maximal value allowed by one particular
         * axis, namely latitude. This point is not a singularity in the stereographic projection,
         * because axes extends toward infinity in all directions (mathematically) and because the
         * South pole has nothing special apart being the origin (0,0).
         *
         * Algorithm:
         *
         * 1) Inspect the target axis, looking if there is any bounds. If bounds are found, get
         *    the coordinates of singularity points and project them from target to source CRS.
         *
         *    Example: If the transformed envelope above is (80 … 85°S, 10 … 50°W), and if the
         *             latitude in the target CRS is bounded at 90°S, then project (90°S, 30°W)
         *             to the source CRS. Note that the longitude is set to the center of
         *             the envelope longitude range (more on this below).
         *
         * 2) If the singularity point computed above is inside the source envelope,
         *    add that point to the target (transformed) envelope.
         *
         * 3) For each singularity point found at step #2, check if there is a wraparound axis
         *    in a dimension other than the dimension that was set to an axis bounds value.
         *    If such wraparound axis is found, test for inclusion of the same point as the
         *    point tested at step #1, except for the coordinate of the wraparound axis which
         *    is set to the minimal and maximal values (reminder: step #1 used the center value).
         *
         *    Example: If the above steps found that the point (90°S, 30°W) need to be included,
         *             then this step #3 will also test the points (90°S, 180°W) and (90°S, 180°E).
         *
         * NOTE: we test (-180°, centerY), (180°, centerY), (centerX, -90°) and (centerX, 90°)
         * at step #1 before to test (-180°, -90°), (180°, -90°), (-180°, 90°) and (180°, 90°)
         * at step #3 because the latter may not be supported by every projections. For example
         * if the target envelope is located between 20°N and 40°N, then a Mercator projection
         * may fail to transform the (-180°, 90°) coordinate while the (-180°, 30°) coordinate
         * is a valid point.
         */
        MathTransform      inverse   = null;
        TransformException warning   = null;
        AbstractEnvelope   sourceBox = null;
        DirectPosition     sourcePt  = null;
        DirectPosition     targetPt  = null;
        DirectPosition     revertPt  = null;
        long includedMinValue = 0;              // A bitmask for each dimension.
        long includedMaxValue = 0;
        long isWrapAroundAxis = 0;
        final int dimension = targetCS.getDimension();
poles:  for (int i=0; i<dimension; i++) {
            final long dimensionBitMask = Numerics.bitmask(i);
            final CoordinateSystemAxis axis = targetCS.getAxis(i);
            if (axis == null) {                 // Should never be null, but check as a paranoiac safety.
                continue;
            }
            boolean testMax = false;            // Tells if we are testing the minimal or maximal value.
            do {
                final double extremum = testMax ? axis.getMaximumValue() : axis.getMinimumValue();
                if (!Double.isFinite(extremum)) {
                    /*
                     * The axis is unbounded. It should always be the case when the target CRS is
                     * a map projection, in which case this loop will finish soon and this method
                     * will do nothing more (no object instantiated, no MathTransform inversed...)
                     */
                    continue;
                }
                if (inverse == null) {
                    try {
                        inverse = mt.inverse();
                    } catch (NoninvertibleTransformException exception) {
                        /*
                         * If the transform is non-invertible, this "poles" loop cannot do anything.
                         * This is not a fatal error because the envelope has already be transformed
                         * by the code before this loop. We lost the check below for singularity points,
                         * but it makes no difference in the common case where all those points would
                         * have been outside the source envelope anyway.
                         *
                         * Note that this exception is normal if the number of target dimension is smaller
                         * than the number of source dimension, because the math transform cannot guess
                         * coordinates in the lost dimensions. So we do not log any warning in this case.
                         */
                        if (dimension >= mt.getSourceDimensions()) {
                            warning = exception;
                        }
                        break poles;
                    }
                    targetPt  = new GeneralDirectPosition(centerPt.clone());
                    sourceBox = AbstractEnvelope.castOrCopy(envelope);
                }
                targetPt.setCoordinate(i, extremum);
                try {
                    sourcePt = inverse.transform(targetPt, sourcePt);
                    if (sourceBox.contains(sourcePt)) {
                        /*
                         * The point is inside the source envelope and consequently should be added in the
                         * transformed envelope. However, there is a possible confusion: if the axis that we
                         * tested is a wraparound axis, then (for example) +180° and -180° of longitude may
                         * be the same point in source CRS, despite being 2 very different points in target
                         * CRS. We do yet another projection in opposite direction for checking if we really
                         * have the point that we wanted to test.
                         */
                        if (CoordinateOperations.isWrapAround(axis)) {
                            revertPt = mt.transform(sourcePt, revertPt);
                            final double delta = Math.abs(revertPt.getCoordinate(i) - extremum);
                            if (!(delta < SPAN_FRACTION_AS_BOUND * (axis.getMaximumValue() - axis.getMinimumValue()))) {
                                continue;
                            }
                        }
                        transformed.add(targetPt);
                        if (testMax) includedMaxValue |= dimensionBitMask;
                        else         includedMinValue |= dimensionBitMask;
                    }
                } catch (TransformException exception) {
                    /*
                     * This exception may be normal. For example if may occur when projecting
                     * the latitude extremums with a cylindrical Mercator projection.  Do not
                     * log any message (unless logging level is fine) and try the other points.
                     */
                    if (warning == null) {
                        warning = exception;
                    } else {
                        warning.addSuppressed(exception);
                    }
                }
            } while ((testMax = !testMax) == true);
            /*
             * Keep trace of axes of kind WRAPAROUND, except if the two extremum values of that
             * axis have been included in the envelope  (in which case the next step after this
             * loop does not need to be executed for this axis).
             */
            if ((includedMinValue & includedMaxValue & dimensionBitMask) == 0 && CoordinateOperations.isWrapAround(axis)) {
                isWrapAroundAxis |= dimensionBitMask;
            }
            // Restore `targetPt` to its initial state, which is equal to `centerPt`.
            if (targetPt != null) {
                targetPt.setCoordinate(i, centerPt[i]);
            }
        }
        /*
         * Step #3 described in the above "Algorithm" section: iterate over all dimensions
         * of type "WRAPAROUND" for which minimal or maximal axis values have not yet been
         * included in the envelope. The set of axes is specified by a bitmask computed in
         * the above loop.  We examine only the points that have not already been included
         * in the envelope.
         */
        final long includedBoundsValue = (includedMinValue | includedMaxValue);
        if (includedBoundsValue != 0) {
            while (isWrapAroundAxis != 0) {
                final int wrapAroundDimension = Long.numberOfTrailingZeros(isWrapAroundAxis);
                final long dimensionBitMask = Numerics.bitmask(wrapAroundDimension);
                isWrapAroundAxis &= ~dimensionBitMask;              // Clear now the bit, for the next iteration.
                final CoordinateSystemAxis wrapAroundAxis = targetCS.getAxis(wrapAroundDimension);
                final double min = wrapAroundAxis.getMinimumValue();
                final double max = wrapAroundAxis.getMaximumValue();
                /*
                 * Iterate over all axes for which a singularity point has been previously found,
                 * excluding the "wrap around axis" currently under consideration.
                 */
                for (long am=(includedBoundsValue & ~dimensionBitMask), bm; am != 0; am &= ~bm) {
                    bm = Long.lowestOneBit(am);
                    final int axisIndex = Long.numberOfTrailingZeros(bm);
                    final CoordinateSystemAxis axis = targetCS.getAxis(axisIndex);
                    /*
                     * switch (c) {
                     *   case 0: targetPt = (..., singularityMin, ..., wrapAroundMin, ...)
                     *   case 1: targetPt = (..., singularityMin, ..., wrapAroundMax, ...)
                     *   case 2: targetPt = (..., singularityMax, ..., wrapAroundMin, ...)
                     *   case 3: targetPt = (..., singularityMax, ..., wrapAroundMax, ...)
                     * }
                     */
                    for (int c=0; c<4; c++) {
                        /*
                         * Set the coordinate value along the axis having the singularity point
                         * (cases c=0 and c=2).  If the envelope did not included that point,
                         * then skip completely this case and the next one, i.e. skip c={0,1}
                         * or skip c={2,3}.
                         */
                        final double value;
                        if ((c & 1) == 0) {         // `true` if we are testing "wrapAroundMin".
                            if (((c == 0 ? includedMinValue : includedMaxValue) & bm) == 0) {
                                c++;                // Skip also the case for "wrapAroundMax".
                                continue;
                            }
                            targetPt.setCoordinate(axisIndex, (c == 0) ? axis.getMinimumValue() : axis.getMaximumValue());
                            value = min;
                        } else {
                            value = max;
                        }
                        targetPt.setCoordinate(wrapAroundDimension, value);
                        try {
                            sourcePt = inverse.transform(targetPt, sourcePt);
                            if (sourceBox.contains(sourcePt)) {
                                /*
                                 * The `value` limit is typically the 180°E or 180°W longitude value. We could test
                                 * its validity as below (see similar code in other loop above for explanation):
                                 *
                                 *     revertPt = mt.transform(sourcePt, revertPt);
                                 *     final double delta = Math.abs(revertPt.getCoordinate(wrapAroundDimension) - value);
                                 *     if (delta < SPAN_FRACTION_AS_BOUND * (max - min)) {
                                 *         transformed.add(targetPt);
                                 *     }
                                 *
                                 * But we don't because this block is executed when another coordinate is at its bounds,
                                 * and that other coordinate is usually the latitude at 90°S or 90°N. In such case, all
                                 * longitude values are the same point on Earth (a pole) and can be anything. Comparing
                                 * `revertPt` coordinate with `value` is meaningless. In order to keep above check, we
                                 * would need to determine if `targetPt` is at a pole. But we have fully reliable way.
                                 */
                                transformed.add(targetPt);
                            }
                        } catch (TransformException exception) {
                            if (warning == null) {
                                warning = exception;
                            } else {
                                warning.addSuppressed(exception);
                            }
                        }
                    }
                    targetPt.setCoordinate(axisIndex, centerPt[axisIndex]);
                }
                targetPt.setCoordinate(wrapAroundDimension, centerPt[wrapAroundDimension]);
            }
        }
        /*
         * At this point we finished envelope transformation. Verify if some coordinates need to be "wrapped around"
         * as a result of the coordinate operation.  This is usually the longitude axis where the source CRS uses
         * the [-180 … +180]° range and the target CRS uses the [0 … 360]° range, or the converse. We do not wrap
         * around if the source and target axes use the same range (e.g. the longitude stay [-180 … +180]°) in order
         * to reduce the risk of discontinuities. If the user really wants unconditional wrap around, (s)he can call
         * `GeneralEnvelope.normalize()`.
         */
        final Set<Integer> wrapAroundChanges;
        if (isOperationComplete && operation instanceof AbstractCoordinateOperation) {
            wrapAroundChanges = ((AbstractCoordinateOperation) operation).getWrapAroundChanges();
        } else {
            wrapAroundChanges = CoordinateOperations.wrapAroundChanges(sourceCRS, targetCS);
        }
        transformed.normalize(targetCS, 0, wrapAroundChanges.size(), wrapAroundChanges.iterator());
        if (warning != null) {
            recoverableException(Envelopes.class, warning);
        }
        return transformed;
    }

    /**
     * Transforms an envelope using the given math transform.
     * The transformation is only approximated: the returned envelope may be bigger than necessary,
     * or smaller than required if the bounding box contains a pole.
     * The coordinate reference system of the returned envelope will be null.
     *
     * <h4>Limitation</h4>
     * This method cannot handle the case where the envelope contains the North or South pole.
     * Furthermore, envelopes crossing the ±180° longitude are handled only if the given transform
     * contains {@link WraparoundTransform} steps, as this method does not add such steps itself.
     * For a more robust envelope transformation, use {@link #transform(CoordinateOperation, Envelope)} instead.
     *
     * @param  transform  the transform to use.
     * @param  envelope   envelope to transform, or {@code null}. This envelope will not be modified.
     * @return the transformed envelope, or {@code null} if {@code envelope} was null.
     * @throws TransformException if a transform failed.
     *
     * @see #transform(CoordinateOperation, Envelope)
     *
     * @since 0.5
     */
    public static GeneralEnvelope transform(final MathTransform transform, final Envelope envelope)
            throws TransformException
    {
        ArgumentChecks.ensureNonNull("transform", transform);
        return (envelope != null) ? transform(transform, envelope, null, null) : null;
    }

    /**
     * Transforms potentially many times an envelope using the given math transform.
     * If the given envelope is {@code null}, then this method returns an empty map.
     * Otherwise, if the given transform does not contain any {@link WraparoundTransform} step,
     * then this method is equivalent to {@link #transform(MathTransform, Envelope)} returned in a singleton map.
     * Otherwise, this method returns many transformed envelopes where each envelope describes approximately the
     * same region. For example:
     *
     * <ul>
     *   <li>If the envelope <abbr>CRS</abbr> is geographic,
     *       the map values are the same envelope shifted by 360° of longitude.</li>
     *   <li>If the envelope <abbr>CRS</abbr> is projected, then the 360° shifts are applied
     *       before the map projection. It may result in very different coordinates.</li>
     * </ul>
     *
     * The keys identify which translations were applied on wraparound axes for computing the associated envelope.
     * For example, a key may identify an envelope computed with a shift of 360° of longitude on some coordinates,
     * and another key may identify the same envelope but computed with a shift of −360° of longitude.
     * The content of those keys is implementation dependent and users should not rely on the exact parameters.
     * The main contract is that, for envelopes computed with the same transform,
     * the values that are associated with {@linkplain Object#equals(Object) equal} keys
     * were computed with wraparounds applied in the same way (e.g. +360° versus −360°).
     *
     * <p><b>Example:</b> the union of two envelopes taking in account wraparounds can be computed as below:</p>
     *
     * {@snippet lang="java" :
     *     MathTransform transform = ...;
     *     Map<Parameters, GeneralEnvelope> result = Envelopes.transformWithWraparound(transform, envelope1);
     *     Envelopes.transformWithWraparound(transform, envelope2).forEach((key, value) -> {
     *         GeneralEnvelope previous = result.putIfAbsent(key, value);
     *         if (previous != null) previous.add(value);   // Envelope union.
     *     });
     * }
     *
     * Note that the key may be {@code null} if the given transform
     * does not contain any {@link WraparoundTransform} step.
     *
     * @param  transform  the transform to use.
     * @param  envelope   envelope to transform, or {@code null}. This envelope will not be modified.
     * @return the transformed envelopes, or an empty map if {@code envelope} was null.
     * @throws TransformException if a transform failed.
     *
     * @see #transform(MathTransform, Envelope)
     * @see WraparoundTransform
     *
     * @since 1.5
     */
    public static Map<Parameters, GeneralEnvelope> transformWithWraparound(
            final MathTransform transform, final Envelope envelope) throws TransformException
    {
        ArgumentChecks.ensureNonNull("transform", transform);
        if (envelope == null) {
            return Map.of();
        }
        final var results = new LinkedHashMap<Parameters, GeneralEnvelope>(4);
        final GeneralEnvelope transformed = transform(transform, envelope, null, results);
        if (results.isEmpty() && transformed != null) {
            results.put(null, transformed);
        }
        return results;
    }

    /**
     * Returns the bounding box of a geometry defined in <i>Well Known Text</i> (WKT) format.
     * This method does not check the consistency of the provided WKT. For example, it does not check
     * that every points in a {@code LINESTRING} have the same dimension. However, this method
     * ensures that the parenthesis are balanced, in order to catch some malformed WKT.
     * See {@link GeneralEnvelope#GeneralEnvelope(CharSequence)} for more information about the parsing rules.
     *
     * <h4>Examples</h4>
     * <ul>
     *   <li>{@code BOX(-180 -90, 180 90)} (not really a geometry, but understood by many software products)</li>
     *   <li>{@code POINT(6 10)}</li>
     *   <li>{@code MULTIPOLYGON(((1 1, 5 1, 1 5, 1 1),(2 2, 3 2, 3 3, 2 2)))}</li>
     *   <li>{@code GEOMETRYCOLLECTION(POINT(4 6),LINESTRING(3 8,7 10))}</li>
     * </ul>
     *
     * @param  wkt  the {@code BOX}, {@code POLYGON} or other kind of element to parse.
     * @return the envelope of the given geometry.
     * @throws FactoryException if the given WKT cannot be parsed.
     *
     * @see #toString(Envelope)
     * @see CRS#fromWKT(String)
     * @see org.apache.sis.io.wkt
     */
    public static Envelope fromWKT(final CharSequence wkt) throws FactoryException {
        ArgumentChecks.ensureNonNull("wkt", wkt);
        try {
            return new GeneralEnvelope(wkt);
        } catch (IllegalArgumentException e) {
            throw new FactoryException(Errors.format(
                    Errors.Keys.UnparsableStringForClass_2, Envelope.class), e);
        }
    }

    /**
     * Formats the given envelope as a {@code BOX} element. The output is like below,
     * where <var>n</var> is the {@linkplain Envelope#getDimension() number of dimensions}
     * (omitted if equals to 2):
     *
     * <blockquote>{@code BOX}<var>n</var>{@code D(}{@linkplain Envelope#getLowerCorner() lower
     * corner}{@code ,} {@linkplain Envelope#getUpperCorner() upper corner}{@code )}</blockquote>
     *
     * The string returned by this method can be {@linkplain GeneralEnvelope#GeneralEnvelope(CharSequence)
     * parsed} by the {@code GeneralEnvelope} constructor.
     *
     * <h4>Note on standards</h4>
     * The {@code BOX} element is not part of the standard <i>Well Known Text</i> (WKT) format.
     * However, it is understood by many software libraries, for example GDAL and PostGIS.
     *
     * @param  envelope  the envelope to format.
     * @return this envelope as a {@code BOX} or {@code BOX3D} (most typical dimensions) element.
     *
     * @see #fromWKT(CharSequence)
     * @see org.apache.sis.io.wkt
     */
    public static String toString(final Envelope envelope) {
        return AbstractEnvelope.toString(envelope, false);
    }

    /**
     * Formats the given envelope as a {@code POLYGON} element in the <i>Well Known Text</i>
     * (WKT) format. {@code POLYGON} can be used as an alternative to {@code BOX} when the element
     * needs to be considered as a standard WKT geometry.
     *
     * <p>The string returned by this method can be {@linkplain GeneralEnvelope#GeneralEnvelope(CharSequence)
     * parsed} by the {@code GeneralEnvelope} constructor.</p>
     *
     * @param  envelope  the envelope to format.
     * @return the envelope as a {@code POLYGON} in WKT format.
     * @throws IllegalArgumentException if the given envelope cannot be formatted.
     *
     * @see org.apache.sis.io.wkt
     */
    public static String toPolygonWKT(final Envelope envelope) throws IllegalArgumentException {
        /*
         * Get the dimension, ignoring the trailing ones which have infinite values.
         */
        int dimension = envelope.getDimension();
        while (dimension != 0) {
            final double length = envelope.getSpan(dimension - 1);
            if (Double.isFinite(length)) {
                break;
            }
            dimension--;
        }
        if (dimension < 2) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyEnvelope2D));
        }
        final var buffer = new StringBuilder("POLYGON(");
        String separator = "(";
        for (int corner = 0; corner < CORNERS.length; corner += 2) {
            for (int i=0; i<dimension; i++) {
                final double value;
                switch (i) {
                    case  0: // Fall through
                    case  1: value = CORNERS[corner+i] ? envelope.getMaximum(i) : envelope.getMinimum(i); break;
                    default: value = envelope.getMedian(i); break;
                }
                trimFractionalPart(buffer.append(separator).append(value));
                separator = " ";
            }
            separator = ", ";
        }
        return buffer.append("))").toString();
    }

    /**
     * Enumeration of the 4 corners in an envelope, with repetition of the first point.
     * The values are (x,y) pairs with {@code false} meaning "minimal value" and {@code true}
     * meaning "maximal value". This is used by {@link #toPolygonWKT(Envelope)} only.
     */
    private static final boolean[] CORNERS = {
        false, false,
        false, true,
        true,  true,
        true,  false,
        false, false
    };

    /**
     * Returns the time range of the first dimension associated to a temporal CRS.
     * This convenience method converts floating point values to instants using
     * {@link org.apache.sis.referencing.crs.DefaultTemporalCRS#toInstant(double)}.
     *
     * @param  envelope  envelope from which to extract time range, or {@code null} if none.
     * @return time range in the given envelope.
     *
     * @see AbstractEnvelope#getTimeRange()
     * @see GeneralEnvelope#setTimeRange(Instant, Instant)
     *
     * @since 1.1
     */
    public static Optional<Range<Instant>> toTimeRange(final Envelope envelope) {
        if (envelope != null) {
            final TemporalAccessor t = TemporalAccessor.of(envelope.getCoordinateReferenceSystem(), 0);
            if (t != null) {
                return Optional.of(t.getTimeRange(envelope));
            }
        }
        return Optional.empty();
    }
}
