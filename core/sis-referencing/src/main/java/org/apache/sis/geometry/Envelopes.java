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
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.cs.RangeMeaning;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.util.Static;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.transform.AbstractMathTransform;
import org.apache.sis.internal.referencing.CoordinateOperations;
import org.apache.sis.internal.referencing.DirectPositionView;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.system.Loggers;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.apache.sis.util.StringBuilders.trimFractionalPart;


/**
 * Transforms envelopes to new Coordinate Reference Systems, and miscellaneous utilities.
 *
 * <div class="section">Envelope transformations</div>
 * All {@code transform(…)} methods in this class take in account the curvature of the transformed shape.
 * For example the shape of a geographic envelope (figure below on the left side) is not rectangular in a
 * conic projection (figure below on the right side). In order to get the envelope represented by the red
 * rectangle, projecting the four corners of the geographic envelope is not sufficient since we would miss
 * the southerner part.
 *
 * <center><table class="sis">
 *   <caption>Example of curvature induced by a map projection</caption>
 *   <tr>
 *     <th>Envelope before map projection</th>
 *     <th>Shape of the projected envelope</th>
 *   </tr><tr>
 *     <td><img src="doc-files/GeographicArea.png" alt="Envelope in a geographic CRS"></td>
 *     <td><img src="doc-files/ConicArea.png" alt="Shape of the envelope transformed in a conic projection"></td>
 *   </tr>
 * </table></center>
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
 * @since   0.3
 * @version 0.5
 * @module
 *
 * @see org.apache.sis.metadata.iso.extent.Extents
 * @see CRS
 */
public final class Envelopes extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private Envelopes() {
    }

    /**
     * Returns {@code true} if the given axis is of kind "Wrap Around".
     */
    private static boolean isWrapAround(final CoordinateSystemAxis axis) {
        return RangeMeaning.WRAPAROUND.equals(axis.getRangeMeaning());
    }

    /**
     * Invoked when a recoverable exception occurred. Those exceptions must be minor enough
     * that they can be silently ignored in most cases.
     */
    private static void recoverableException(final TransformException exception) {
        Logging.recoverableException(Logging.getLogger(Loggers.GEOMETRY), Envelopes.class, "transform", exception);
    }

    /**
     * A buckle method for calculating derivative and coordinate transformation in a single step,
     * if the given {@code derivative} argument is {@code true}.
     *
     * @param transform The transform to use.
     * @param srcPts    The array containing the source coordinate at offset 0.
     * @param dstPts    the array into which the transformed coordinate is returned.
     * @param dstOff    The offset to the location of the transformed point that is stored in the destination array.
     * @param derivate  {@code true} for computing the derivative, or {@code false} if not needed.
     * @return The matrix of the transform derivative at the given source position,
     *         or {@code null} if the {@code derivate} argument is {@code false}.
     * @throws TransformException If the point can not be transformed
     *         or if a problem occurred while calculating the derivative.
     */
    private static Matrix derivativeAndTransform(final MathTransform transform, final double[] srcPts,
            final double[] dstPts, final int dstOff, final boolean derivate) throws TransformException
    {
        if (transform instanceof AbstractMathTransform) {
            return ((AbstractMathTransform) transform).transform(srcPts, 0, dstPts, dstOff, derivate);
        }
        // Derivative must be calculated before to transform the coordinate.
        final Matrix derivative = derivate ? transform.derivative(new DirectPositionView(srcPts, 0, transform.getSourceDimensions())) : null;
        transform.transform(srcPts, 0, dstPts, dstOff, 1);
        return derivative;
    }

    /**
     * Transforms the given envelope to the specified CRS. If any argument is null, or if the
     * {@linkplain GeneralEnvelope#getCoordinateReferenceSystem() envelope CRS} is null or the
     * same instance than the given target CRS, then the given envelope is returned unchanged.
     * Otherwise a new transformed envelope is returned.
     *
     * <div class="section">Performance tip</div>
     * If there is many envelopes to transform with the same source and target CRS, then it is more efficient
     * to get the {@link CoordinateOperation} or {@link MathTransform} instance once and invoke one of the
     * others {@code transform(…)} methods.
     *
     * @param  envelope The envelope to transform (may be {@code null}).
     * @param  targetCRS The target CRS (may be {@code null}).
     * @return A new transformed envelope, or directly {@code envelope} if no change was required.
     * @throws TransformException If a transformation was required and failed.
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
                    final CoordinateOperation operation;
                    try {
                        operation = CoordinateOperations.factory().createOperation(sourceCRS, targetCRS);
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
     * Transforms an envelope using the given math transform.
     * The transformation is only approximative: the returned envelope may be bigger than necessary,
     * or smaller than required if the bounding box contains a pole.
     *
     * <div class="section">Limitation</div>
     * This method can not handle the case where the envelope contains the North or South pole,
     * or when it crosses the ±180° longitude, because {@link MathTransform} does not carry sufficient information.
     * For a more robust envelope transformation, use {@link #transform(CoordinateOperation, Envelope)} instead.
     *
     * @param  transform The transform to use.
     * @param  envelope Envelope to transform, or {@code null}. This envelope will not be modified.
     * @return The transformed envelope, or {@code null} if {@code envelope} was null.
     * @throws TransformException if a transform failed.
     *
     * @see #transform(CoordinateOperation, Envelope)
     *
     * @since 0.5
     */
    public static GeneralEnvelope transform(final MathTransform transform, final Envelope envelope)
            throws TransformException
    {
        ensureNonNull("transform", transform);
        return (envelope != null) ? transform(transform, envelope, null) : null;
    }

    /**
     * Implementation of {@link #transform(MathTransform, Envelope)} with the opportunity to
     * save the projected center coordinate.
     *
     * @param targetPt After this method call, the center of the source envelope projected to
     *        the target CRS. The length of this array must be the number of target dimensions.
     *        May be {@code null} if this information is not needed.
     */
    @SuppressWarnings("null")
    private static GeneralEnvelope transform(final MathTransform transform,
                                             final Envelope      envelope,
                                             final double[]      targetPt)
            throws TransformException
    {
        if (transform.isIdentity()) {
            /*
             * Slight optimization: Just copy the envelope. Note that we need to set the CRS
             * to null because we don't know what the target CRS was supposed to be. Even if
             * an identity transform often imply that the target CRS is the same one than the
             * source CRS, it is not always the case. The metadata may be differents, or the
             * transform may be a datum shift without Bursa-Wolf parameters, etc.
             */
            final GeneralEnvelope transformed = new GeneralEnvelope(envelope);
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
         * Allocates all needed objects. The value '3' below is because the following 'while'
         * loop uses a 'pointIndex' to be interpreted as a number in base 3 (see the comment
         * inside the loop).  The coordinate to transform must be initialized to the minimal
         * ordinate values. This coordinate will be updated in the 'switch' statement inside
         * the 'while' loop.
         */
        if (sourceDim >= 20) {          // Maximal value supported by Formulas.pow3(int) is 19.
            throw new IllegalArgumentException(Errors.format(Errors.Keys.ExcessiveNumberOfDimensions_1));
        }
        int             pointIndex            = 0;
        boolean         isDerivativeSupported = true;
        GeneralEnvelope transformed           = null;
        final Matrix[]  derivatives           = new Matrix[Formulas.pow3(sourceDim)];
        final double[]  ordinates             = new double[derivatives.length * targetDim];
        final double[]  sourcePt              = new double[sourceDim];
        for (int i=sourceDim; --i>=0;) {
            sourcePt[i] = envelope.getMinimum(i);
        }
        // A window over a single coordinate in the 'ordinates' array.
        final DirectPositionView ordinatesView = new DirectPositionView(ordinates, 0, targetDim);
        /*
         * Iterates over every minimal, maximal and median ordinate values (3 points) along each
         * dimension. The total number of iterations is 3 ^ (number of source dimensions).
         */
        transformPoint: while (true) {
            /*
             * Compute the derivative (optional operation). If this operation fails, we will
             * set a flag to 'false' so we don't try again for all remaining points. We try
             * to compute the derivative and the transformed point in a single operation if
             * we can. If we can not, we will compute those two information separately.
             *
             * Note that the very last point to be projected must be the envelope center.
             * There is usually no need to calculate the derivative for that last point,
             * but we let it does anyway for safety.
             */
            final int offset = pointIndex * targetDim;
            try {
                derivatives[pointIndex] = derivativeAndTransform(transform,
                        sourcePt, ordinates, offset, isDerivativeSupported);
            } catch (TransformException e) {
                if (!isDerivativeSupported) {
                    throw e;                    // Derivative were already disabled, so something went wrong.
                }
                isDerivativeSupported = false;
                transform.transform(sourcePt, 0, ordinates, offset, 1);
                recoverableException(e);        // Log only if the above call was successful.
            }
            /*
             * The transformed point has been saved for future reuse after the enclosing
             * 'while' loop. Now add the transformed point to the destination envelope.
             */
            if (transformed == null) {
                transformed = new GeneralEnvelope(targetDim);
                for (int i=0; i<targetDim; i++) {
                    final double value = ordinates[offset + i];
                    transformed.setRange(i, value, value);
                }
            } else {
                ordinatesView.offset = offset;
                transformed.add(ordinatesView);
            }
            /*
             * Get the next point coordinate. The 'coordinateIndex' variable is an index in base 3
             * having a number of digits equals to the number of source dimensions.  For example a
             * 4-D space have indexes ranging from "0000" to "2222" (numbers in base 3). The digits
             * are then mapped to minimal (0), maximal (1) or central (2) ordinates. The outer loop
             * stops when the counter roll back to "0000". Note that 'targetPt' must keep the value
             * of the last projected point, which must be the envelope center identified by "2222"
             * in the 4-D case.
             */
            int indexBase3 = ++pointIndex;
            for (int dim=sourceDim; --dim>=0; indexBase3 /= 3) {
                switch (indexBase3 % 3) {
                    case 0:  sourcePt[dim] = envelope.getMinimum(dim); break;   // Continue the loop.
                    case 1:  sourcePt[dim] = envelope.getMaximum(dim); continue transformPoint;
                    case 2:  sourcePt[dim] = envelope.getMedian (dim); continue transformPoint;
                    default: throw new AssertionError(indexBase3);     // Should never happen
                }
            }
            break;
        }
        assert pointIndex == derivatives.length : pointIndex;
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
        DirectPosition temporary = null;
        final DirectPositionView sourceView = new DirectPositionView(sourcePt, 0, sourceDim);
        final CurveExtremum extremum = new CurveExtremum();
        for (pointIndex=0; pointIndex < derivatives.length; pointIndex++) {
            final Matrix D1 = derivatives[pointIndex];
            if (D1 != null) {
                int indexBase3 = pointIndex, power3 = 1;
                for (int i=sourceDim; --i>=0; indexBase3 /= 3, power3 *= 3) {
                    final int digitBase3 = indexBase3 % 3;
                    if (digitBase3 != 2) { // Process only if we are not already located on the median along the dimension i.
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
                                extremum.resolve(x1, ordinates[offset1 + j], D1.getElement(j,i),
                                                 x2, ordinates[offset2 + j], D2.getElement(j,i));
                                boolean isP2 = false;
                                do { // Executed exactly twice, one for each extremum point.
                                    final double x = isP2 ? extremum.ex2 : extremum.ex1;
                                    if (x > xmin && x < xmax) {
                                        final double y = isP2 ? extremum.ey2 : extremum.ey1;
                                        if (y < transformed.getMinimum(j) ||
                                            y > transformed.getMaximum(j))
                                        {
                                            /*
                                             * At this point, we have determined that adding the extremum point
                                             * would expand the envelope. However we will not add that point
                                             * directly because its position may not be quite right (since we
                                             * used a cubic curve approximation). Instead, we project the point
                                             * on the envelope border which is located vis-à-vis the extremum.
                                             */
                                            for (int ib3 = pointIndex, dim = sourceDim; --dim >= 0; ib3 /= 3) {
                                                final double ordinate;
                                                if (dim == i) {
                                                    ordinate = x;                         // Position of the extremum.
                                                } else switch (ib3 % 3) {
                                                    case 0:  ordinate = envelope.getMinimum(dim); break;
                                                    case 1:  ordinate = envelope.getMaximum(dim); break;
                                                    case 2:  ordinate = envelope.getMedian (dim); break;
                                                    default: throw new AssertionError(ib3);     // Should never happen
                                                }
                                                sourcePt[dim] = ordinate;
                                            }
                                            temporary = transform.transform(sourceView, temporary);
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
        if (targetPt != null) {
            // Copy the coordinate of the center point.
            System.arraycopy(ordinates, ordinates.length - targetDim, targetPt, 0, targetDim);
        }
        return transformed;
    }

    /**
     * Transforms an envelope using the given coordinate operation.
     * The transformation is only approximative: the returned envelope may be bigger than the
     * smallest possible bounding box, but should not be smaller in most cases.
     *
     * <p>This method can handle the case where the envelope contains the North or South pole,
     * or when it cross the ±180° longitude.</p>
     *
     * <div class="note"><b>Note:</b>
     * If the envelope CRS is non-null, then the caller should ensure that the operation source CRS
     * is the same than the envelope CRS. In case of mismatch, this method transforms the envelope
     * to the operation source CRS before to apply the operation. This extra step may cause a lost
     * of accuracy. In order to prevent this method from performing such pre-transformation (if not desired),
     * callers can ensure that the envelope CRS is {@code null} before to call this method.</div>
     *
     * @param  operation The operation to use.
     * @param  envelope Envelope to transform, or {@code null}. This envelope will not be modified.
     * @return The transformed envelope, or {@code null} if {@code envelope} was null.
     * @throws TransformException if a transform failed.
     *
     * @see #transform(MathTransform, Envelope)
     *
     * @since 0.5
     */
    @SuppressWarnings("null")
    public static GeneralEnvelope transform(final CoordinateOperation operation, Envelope envelope)
            throws TransformException
    {
        ensureNonNull("operation", operation);
        if (envelope == null) {
            return null;
        }
        final CoordinateReferenceSystem sourceCRS = operation.getSourceCRS();
        if (sourceCRS != null) {
            final CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
            if (crs != null && !Utilities.equalsIgnoreMetadata(crs, sourceCRS)) {
                /*
                 * Argument-check: the envelope CRS seems inconsistent with the given operation.
                 * However we need to push the check a little bit further, since 3D-GeographicCRS
                 * are considered not equal to CompoundCRS[2D-GeographicCRS + ellipsoidal height].
                 * Checking for identity MathTransform is a more powerfull (but more costly) check.
                 * Since we have the MathTransform, perform an opportunist envelope transform if it
                 * happen to be required.
                 */
                final MathTransform mt;
                try {
                    mt = CoordinateOperations.factory().createOperation(crs, sourceCRS).getMathTransform();
                } catch (FactoryException e) {
                    throw new TransformException(Errors.format(Errors.Keys.CanNotTransformEnvelope), e);
                }
                if (!mt.isIdentity()) {
                    envelope = transform(mt, envelope);
                }
            }
        }
        MathTransform mt = operation.getMathTransform();
        final double[] centerPt = new double[mt.getTargetDimensions()];
        final GeneralEnvelope transformed = transform(mt, envelope, centerPt);
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
                            sourcePt.setOrdinate(j, envelope.getMedian(j));
                        }
                    }
                    if (b1) {
                        sourcePt.setOrdinate(i, v1);
                        transformed.add(targetPt = mt.transform(sourcePt, targetPt));
                    }
                    if (b2) {
                        sourcePt.setOrdinate(i, v2);
                        transformed.add(targetPt = mt.transform(sourcePt, targetPt));
                    }
                    sourcePt.setOrdinate(i, envelope.getMedian(i));
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
         * Checks for singularity points. For example the south pole is a singularity point in
         * geographic CRS because is is located at the maximal value allowed by one particular
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
         *             to the source CRS. Note that the longitude is set to the the center of
         *             the envelope longitude range (more on this below).
         *
         * 2) If the singularity point computed above is inside the source envelope, add that
         *    point to the target (transformed) envelope.
         *
         * 3) If step #2 added the point, iterate over all other axes. If an other bounded axis
         *    is found and that axis is of kind "WRAPAROUND", test for inclusion the same point
         *    than the point tested at step #1, except for the ordinate of the axis found in this
         *    step. That ordinate is set to the minimal and maximal values of that axis.
         *
         *    Example: If the above steps found that the point (90°S, 30°W) need to be included,
         *             then this step #3 will also test phe points (90°S, 180°W) and (90°S, 180°E).
         *
         * NOTE: we test (-180°, centerY), (180°, centerY), (centerX, -90°) and (centerX, 90°)
         * at step #1 before to test (-180°, -90°), (180°, -90°), (-180°, 90°) and (180°, 90°)
         * at step #3 because the later may not be supported by every projections. For example
         * if the target envelope is located between 20°N and 40°N, then a Mercator projection
         * may fail to transform the (-180°, 90°) coordinate while the (-180°, 30°) coordinate
         * is a valid point.
         */
        TransformException warning = null;
        AbstractEnvelope generalEnvelope = null;
        DirectPosition sourcePt = null;
        DirectPosition targetPt = null;
        long includedMinValue = 0;              // A bitmask for each dimension.
        long includedMaxValue = 0;
        long isWrapAroundAxis = 0;
        long dimensionBitMask = 1;
        final int dimension = targetCS.getDimension();
        for (int i=0; i<dimension; i++, dimensionBitMask <<= 1) {
            final CoordinateSystemAxis axis = targetCS.getAxis(i);
            if (axis == null) {                 // Should never be null, but check as a paranoiac safety.
                continue;
            }
            boolean testMax = false;            // Tells if we are testing the minimal or maximal value.
            do {
                final double extremum = testMax ? axis.getMaximumValue() : axis.getMinimumValue();
                if (Double.isInfinite(extremum) || Double.isNaN(extremum)) {
                    /*
                     * The axis is unbounded. It should always be the case when the target CRS is
                     * a map projection, in which case this loop will finish soon and this method
                     * will do nothing more (no object instantiated, no MathTransform inversed...)
                     */
                    continue;
                }
                if (targetPt == null) {
                    try {
                        mt = mt.inverse();
                    } catch (NoninvertibleTransformException exception) {
                        /*
                         * If the transform is non invertible, this method can't do anything. This
                         * is not a fatal error because the envelope has already be transformed by
                         * the caller. We lost the check for singularity points performed by this
                         * method, but it make no difference in the common case where the source
                         * envelope didn't contains any of those points.
                         *
                         * Note that this exception is normal if target dimension is smaller than
                         * source dimension, since the math transform can not reconstituate the
                         * lost dimensions. So we don't log any warning in this case.
                         */
                        if (dimension >= mt.getSourceDimensions()) {
                            recoverableException(exception);
                        }
                        return transformed;
                    }
                    targetPt = new GeneralDirectPosition(mt.getSourceDimensions());
                    for (int j=0; j<dimension; j++) {
                        targetPt.setOrdinate(j, centerPt[j]);
                    }
                    // TODO: avoid the hack below if we provide a contains(DirectPosition)
                    //       method in the GeoAPI org.opengis.geometry.Envelope interface.
                    generalEnvelope = AbstractEnvelope.castOrCopy(envelope);
                }
                targetPt.setOrdinate(i, extremum);
                try {
                    sourcePt = mt.transform(targetPt, sourcePt);
                } catch (TransformException exception) {
                    /*
                     * This exception may be normal. For example if may occur when projecting
                     * the latitude extremums with a cylindrical Mercator projection.  Do not
                     * log any message (unless logging level is fine) and try the other points.
                     */
                    if (warning == null) {
                        warning = exception;
                    } else {
                        // warning.addSuppressed(exception) on the JDK7 branch.
                    }
                    continue;
                }
                if (generalEnvelope.contains(sourcePt)) {
                    transformed.add(targetPt);
                    if (testMax) includedMaxValue |= dimensionBitMask;
                    else         includedMinValue |= dimensionBitMask;
                }
            } while ((testMax = !testMax) == true);
            /*
             * Keep trace of axes of kind WRAPAROUND, except if the two extremum values of that
             * axis have been included in the envelope  (in which case the next step after this
             * loop doesn't need to be executed for that axis).
             */
            if ((includedMinValue & includedMaxValue & dimensionBitMask) == 0 && isWrapAround(axis)) {
                isWrapAroundAxis |= dimensionBitMask;
            }
            // Restore 'targetPt' to its initial state, which is equals to 'centerPt'.
            if (targetPt != null) {
                targetPt.setOrdinate(i, centerPt[i]);
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
                dimensionBitMask = 1 << wrapAroundDimension;
                isWrapAroundAxis &= ~dimensionBitMask; // Clear now the bit, for the next iteration.
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
                         * Set the ordinate value along the axis having the singularity point
                         * (cases c=0 and c=2). If the envelope did not included that point,
                         * then skip completly this case and the next one, i.e. skip c={0,1}
                         * or skip c={2,3}.
                         */
                        double value = max;
                        if ((c & 1) == 0) {         // 'true' if we are testing "wrapAroundMin".
                            if (((c == 0 ? includedMinValue : includedMaxValue) & bm) == 0) {
                                c++;                // Skip also the case for "wrapAroundMax".
                                continue;
                            }
                            targetPt.setOrdinate(axisIndex, (c == 0) ? axis.getMinimumValue() : axis.getMaximumValue());
                            value = min;
                        }
                        targetPt.setOrdinate(wrapAroundDimension, value);
                        try {
                            sourcePt = mt.transform(targetPt, sourcePt);
                        } catch (TransformException exception) {
                            if (warning == null) {
                                warning = exception;
                            } else {
                                // warning.addSuppressed(exception) on the JDK7 branch.
                            }
                            continue;
                        }
                        if (generalEnvelope.contains(sourcePt)) {
                            transformed.add(targetPt);
                        }
                    }
                    targetPt.setOrdinate(axisIndex, centerPt[axisIndex]);
                }
                targetPt.setOrdinate(wrapAroundDimension, centerPt[wrapAroundDimension]);
            }
        }
        if (warning != null) {
            recoverableException(warning);
        }
        return transformed;
    }

    /**
     * Returns the bounding box of a geometry defined in <cite>Well Known Text</cite> (WKT) format.
     * This method does not check the consistency of the provided WKT. For example it does not check
     * that every points in a {@code LINESTRING} have the same dimension. However this method
     * ensures that the parenthesis are balanced, in order to catch some malformed WKT.
     *
     * <p>Example:</p>
     * <ul>
     *   <li>{@code BOX(-180 -90, 180 90)} (not really a geometry, but understood by many softwares)</li>
     *   <li>{@code POINT(6 10)}</li>
     *   <li>{@code MULTIPOLYGON(((1 1, 5 1, 1 5, 1 1),(2 2, 3 2, 3 3, 2 2)))}</li>
     *   <li>{@code GEOMETRYCOLLECTION(POINT(4 6),LINESTRING(3 8,7 10))}</li>
     * </ul>
     *
     * See {@link GeneralEnvelope#GeneralEnvelope(CharSequence)} for more information about the
     * parsing rules.
     *
     * @param  wkt The {@code BOX}, {@code POLYGON} or other kind of element to parse.
     * @return The envelope of the given geometry.
     * @throws FactoryException If the given WKT can not be parsed.
     *
     * @see #toString(Envelope)
     * @see CRS#fromWKT(String)
     * @see org.apache.sis.io.wkt
     */
    public static Envelope fromWKT(final CharSequence wkt) throws FactoryException {
        ensureNonNull("wkt", wkt);
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
     * <div class="note"><b>Note:</b>
     * The {@code BOX} element is not part of the standard <cite>Well Known Text</cite> (WKT) format.
     * However it is understood by many softwares, for example GDAL and PostGIS.</div>
     *
     * The string returned by this method can be {@linkplain GeneralEnvelope#GeneralEnvelope(CharSequence)
     * parsed} by the {@code GeneralEnvelope} constructor.
     *
     * @param  envelope The envelope to format.
     * @return This envelope as a {@code BOX} or {@code BOX3D} (most typical dimensions) element.
     *
     * @see #fromWKT(CharSequence)
     * @see org.apache.sis.io.wkt
     */
    public static String toString(final Envelope envelope) {
        return AbstractEnvelope.toString(envelope, false);
    }

    /**
     * Formats the given envelope as a {@code POLYGON} element in the <cite>Well Known Text</cite>
     * (WKT) format. {@code POLYGON} can be used as an alternative to {@code BOX} when the element
     * needs to be considered as a standard WKT geometry.
     *
     * <p>The string returned by this method can be {@linkplain GeneralEnvelope#GeneralEnvelope(CharSequence)
     * parsed} by the {@code GeneralEnvelope} constructor.</p>
     *
     * @param  envelope The envelope to format.
     * @return The envelope as a {@code POLYGON} in WKT format.
     * @throws IllegalArgumentException if the given envelope can not be formatted.
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
            if (!Double.isNaN(length) && !Double.isInfinite(length)) {
                break;
            }
            dimension--;
        }
        if (dimension < 2) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyEnvelope2D));
        }
        final StringBuilder buffer = new StringBuilder("POLYGON(");
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
}
