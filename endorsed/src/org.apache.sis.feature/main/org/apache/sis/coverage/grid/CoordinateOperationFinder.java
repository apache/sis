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
package org.apache.sis.coverage.grid;

import java.util.Arrays;
import java.util.function.Supplier;
import javax.measure.Quantity;
import javax.measure.quantity.Length;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.privy.CoordinateOperations;
import org.apache.sis.referencing.privy.WraparoundApplicator;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.WraparoundTransform;
import org.apache.sis.geometry.AbstractDirectPosition;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.image.ImageProcessor;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.privy.Numerics;


/**
 * Finds a transform from grid cells in a source coverage to geospatial positions in the CRS of a target coverage.
 * This class differs from {@link CRS#findOperation CRS#findOperation(…)} because of the gridded aspect of inputs.
 * {@linkplain GridGeometry grid geometries} give more information about how referencing is applied on datasets.
 * With them, this class provides three additional benefits:
 *
 * <ul class="verbose">
 *   <li>Detect dimensions where target coordinates are constrained to constant values because
 *     the {@linkplain GridExtent#getSize(int) grid size} is only one cell in those dimensions.
 *     This is an important because it makes possible to find operations normally impossible:
 *     we can still produce an operation to a target CRS even if some dimensions have no corresponding
 *     source CRS.</li>
 *
 *   <li>Detect how to handle wraparound axes. For example if a raster spans 150° to 200° of longitude,
 *     this class understands that -170° of longitude should be translated to 190° for thar particular
 *     raster. This will work even if the minimum and maximum values declared in the longitude axis do
 *     not match that range.</li>
 *
 *   <li>Use the area of interest and grid resolution for refining the coordinate operation between two CRS.</li>
 * </ul>
 *
 * <b>Note:</b> except for the {@link #gridToGrid()} convenience method,
 * this class does not provide the complete chain of operations from grid to grid.
 * It provides only the operation from <em>cell indices</em> in source grid to <em>coordinates in the CRS</em>
 * of destination grid. Callers must add the last step (conversion from target CRS to cell indices) themselves.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
final class CoordinateOperationFinder implements Supplier<double[]> {
    /**
     * Whether the operation is between cell centers or cell corners.
     *
     * @see #setAnchor(PixelInCell)
     */
    private PixelInCell anchor;

    /**
     * The grid geometry which is the source/target of the coordinate operation to find.
     */
    private final GridGeometry source, target;

    /**
     * The target coordinate values, computed only if needed. This is computed by {@link #get()}, which is
     * itself invoked indirectly by {@link org.apache.sis.referencing.operation.CoordinateOperationFinder}.
     * The value is cached in case {@code get()} is invoked many times during the same finder execution.
     *
     * @see #get()
     */
    private double[] coordinates;

    /**
     * The coordinate operation from source to target CRS, computed when first needed.
     * May be {@code null} if there is no information about the source and target CRS.
     * Note that identity operation is not equivalent to {@code null} because identity operation will still
     * be checked for wraparound axes (because the CRS is known), while null operation will have no check.
     *
     * @see #knowChangeOfCRS
     * @see #changeOfCRS()
     */
    private CoordinateOperation changeOfCRS;

    /**
     * Whether the {@link #changeOfCRS} operation has been determined.
     * Note that result of determining that operation may be {@code null}, which is why we need this flag.
     */
    private boolean knowChangeOfCRS;

    /**
     * The {@link #changeOfCRS} transform together with {@link WraparoundTransform} if needed.
     * The wraparound is used for handling images crossing the anti-meridian.
     *
     * @see #gridToCRS()
     */
    private MathTransform forwardChangeOfCRS;

    /**
     * Inverse of {@link #changeOfCRS} transform together with {@link WraparoundTransform} if needed.
     * The wraparound is used for handling images crossing the anti-meridian.
     *
     * <p>Contrarily to {@link #forwardChangeOfCRS}, the process that determine this {@code inverseChangeOfCRS}
     * transform should check if wraparound is really needed. This is because {@code inverseChangeOfCRS} will be
     * used much more extensively (for every pixels) than other transforms.</p>
     *
     * @see #isWraparoundNeeded
     * @see #isWraparoundApplied
     * @see #inverse()
     */
    private MathTransform inverseChangeOfCRS;

    /**
     * Transform from “grid coordinates of the source” to “geospatial coordinates of the target”.
     * This is the concatenation of {@link #source} "grid to CRS" with {@link #forwardChangeOfCRS},
     * possibly with wraparound handling and cached for reuse by {@link #inverse()}:
     *
     * {@snippet lang="java" :
     *     forwardChangeOfCRS = changeOfCRS.getMathTransform();
     *     // + wraparound handling if applicable.
     *     gridToCRS = source.getGridToCRS(anchor);
     *     gridToCRS = MathTransforms.concatenate(gridToCRS, forwardChangeOfCRS);
     *     }
     *
     * @see #gridToCRS()
     */
    private MathTransform gridToCRS;

    /**
     * Transform from the target CRS to the source grid, with {@link WraparoundTransform} applied if needed.
     * This is the concatenation of {@link #inverseChangeOfCRS} with inverse of {@link #source} "grid to CRS",
     * possibly with wraparound handling:
     *
     * {@snippet lang="java" :
     *     inverseChangeOfCRS = forwardChangeOfCRS.inverse();
     *     // + wraparound handling if applicable.
     *     crsToGrid = gridToCRS.inverse();
     *     crsToGrid = MathTransforms.concatenate(inverseChangeOfCRS, crsToGrid);
     *     }
     *
     * @see #inverse()
     * @see #applyWraparound(MathTransform)
     */
    private MathTransform crsToGrid;

    /**
     * Whether the {@link #isWraparoundNeeded} value has been determined. This flag controls whether to perform
     * a more extensive check of wraparound occurrence. This flag should be {@code false} the first time that
     * {@link #inverse()} is invoked and {@code true} the next time.
     */
    private boolean isWraparoundNeedVerified;

    /**
     * Whether {@link #inverseChangeOfCRS} needs to include a {@link WraparoundTransform} step. We do this check
     * only for {@link #inverseChangeOfCRS} because it is the transform which will be executed for every pixels.
     * By contrast, {@link #forwardChangeOfCRS} will systematically contain a {@link WraparoundTransform} step
     * because we use it only for transforming envelopes and for the test that determines the value of
     * this {@code isWraparoundNeeded} flag.
     */
    private boolean isWraparoundNeeded;

    /**
     * Whether {@link WraparoundTransform} has been applied on {@link #inverseChangeOfCRS}. This field complements
     * {@link #isWraparoundNeeded} because a delay may exist between the time we detected that wraparound is needed
     * and the time we applied the necessary operation steps.
     *
     * <p>Note that despite this field name, a {@code true} value does not imply that {@link #inverseChangeOfCRS}
     * and {@link #crsToGrid} transforms really contain some {@link WraparoundTransform} steps. It only means that
     * the {@link WraparoundApplicator#forDomainOfUse WraparoundApplicator.forDomainOfUse(…)} method has been invoked.
     * That method may have decided to not insert any wraparound steps.</p>
     *
     * @see #applyWraparound(MathTransform)
     */
    private boolean isWraparoundApplied;

    /**
     * Whether to disable completely all wraparounds checks.
     * If {@code true}, then calculation done in this class should be equivalent to following code:
     *
     * {@snippet lang="java" :
     *     forwardChangeOfCRS = changeOfCRS.getMathTransform();
     *     inverseChangeOfCRS = forwardChangeOfCRS.inverse();
     *     gridToCRS          = source.getGridToCRS(anchor);
     *     crsToGrid          = gridToCRS.inverse();
     *     gridToCRS          = MathTransforms.concatenate(gridToCRS, forwardChangeOfCRS);
     *     crsToGrid          = MathTransforms.concatenate(inverseChangeOfCRS, crsToGrid);
     *     }
     *
     * <b>Tip:</b> searching usage of this field should help to identify code doing wraparound handling.
     *
     * @see #nowraparound()
     */
    private boolean isWraparoundDisabled;

    /**
     * Creates a new finder initialized to {@link PixelInCell#CELL_CORNER} anchor.
     *
     * @param  source  the grid geometry which is the source of the coordinate operation to find.
     * @param  target  the grid geometry which is the target of the coordinate operation to find.
     */
    CoordinateOperationFinder(final GridGeometry source, final GridGeometry target) {
        this.source = source;
        this.target = target;
        this.anchor = PixelInCell.CELL_CORNER;
    }

    /**
     * Verifies the presence of a CRS considered mandatory,
     * unless the CRS of the opposite grid is also missing.
     *
     * @param  rs  {@code true} is source CRS is mandatory, {@code false} if target CRS is mandatory.
     * @throws IncompleteGridGeometryException if a mandatory CRS is missing.
     */
    final void verifyPresenceOfCRS(final boolean rs) {
        if ((rs ? target : source).isDefined(GridGeometry.CRS)) {
            if ((rs ? source : target).getCoordinateReferenceSystem() == null) {
                throw new IncompleteGridGeometryException();
            }
        }
    }

    /**
     * Sets whether operations will be between cell centers or cell corners.
     * This method must be invoked before any other method in this class.
     * The {@link PixelInCell#CELL_CORNER} value should be used first
     * in order to cache values computed relative to pixel corners.
     *
     * @param  newValue  whether operations will be between cell centers or cell corners.
     */
    final void setAnchor(final PixelInCell newValue) {
        /*
         * If `coordinates` is non-null, it means that `CoordinateOperationContext.getConstantCoordinates()`
         * has been invoked during previous `gridToCRS()` execution, which implies that `changeOfCRS` depends
         * on the `anchor` value. In such case we will need to recompute all fields that depend on `changeOfCRS`.
         */
        anchor = newValue;
        gridToCRS = null;
        crsToGrid = null;
        if (coordinates != null) {
            coordinates        = null;
            changeOfCRS        = null;
            forwardChangeOfCRS = null;
            inverseChangeOfCRS = null;
            knowChangeOfCRS    = false;
            // Do not clear `isWraparoundNeeded`; its value is still valid.
        }
    }

    /**
     * Disables completely all wraparounds operation.
     *
     * @see #isWraparoundDisabled
     */
    final void nowraparound() {
        gridToCRS                = null;        // For forcing recomputation.
        crsToGrid                = null;
        forwardChangeOfCRS       = null;
        inverseChangeOfCRS       = null;
        isWraparoundNeeded       = false;
        isWraparoundApplied      = true;
        isWraparoundNeedVerified = true;
        isWraparoundDisabled     = true;
    }

    /**
     * Returns the target of the "corner to CRS" transform.
     * May be {@code null} if the neither the source and target grid geometry define a CRS.
     */
    final CoordinateReferenceSystem getTargetCRS() {
        return (changeOfCRS != null) ? changeOfCRS.getTargetCRS() :
                source.isDefined(GridGeometry.CRS) ? source.getCoordinateReferenceSystem() : null;
    }

    /**
     * Returns the coordinate operation from source CRS to target CRS. It may be the identity operation.
     * We try to take envelopes in account because the operation choice may depend on the geographic area.
     *
     * @todo Specify also the desired resolution, computed from target grid geometry. It will require
     *       more direct use of {@link org.apache.sis.referencing.operation.CoordinateOperationContext}.
     *       As a side effect, we could remove {@link CoordinateOperations#CONSTANT_COORDINATES} hack.
     *
     * @return operation from source CRS to target CRS, or {@code null} if a CRS is not specified.
     * @throws FactoryException if no operation can be found between the source and target CRS.
     * @throws TransformException if some coordinates cannot be transformed to the specified target.
     */
    private CoordinateOperation changeOfCRS() throws FactoryException, TransformException {
        if (!knowChangeOfCRS) {
            final Envelope sourceEnvelope = source.envelope;
            final Envelope targetEnvelope = target.envelope;
            try {
                CoordinateOperations.CONSTANT_COORDINATES.set(this);
                if (sourceEnvelope != null && targetEnvelope != null) {
                    changeOfCRS = Envelopes.findOperation(sourceEnvelope, targetEnvelope);
                }
                if (changeOfCRS == null && source.isDefined(GridGeometry.CRS) && target.isDefined(GridGeometry.CRS)) {
                    final CoordinateReferenceSystem sourceCRS = source.getCoordinateReferenceSystem();
                    /*
                     * Unconditionally create operation even if CRS are the same. A non-null operation trig
                     * the check for wraparound axes, which is necessary even if the transform is identity.
                     */
                    DefaultGeographicBoundingBox areaOfInterest = null;
                    if (sourceEnvelope != null || targetEnvelope != null) try {
                        areaOfInterest = new DefaultGeographicBoundingBox();
                        areaOfInterest.setBounds(targetEnvelope != null ? targetEnvelope : sourceEnvelope);
                    } catch (TransformException e) {
                        areaOfInterest = null;
                        recoverableException("changeOfCRS", e);
                    }
                    changeOfCRS = CRS.findOperation(sourceCRS, target.getCoordinateReferenceSystem(), areaOfInterest);
                }
            } catch (BackingStoreException e) {                         // May be thrown by getConstantCoordinates().
                throw e.unwrapOrRethrow(TransformException.class);
            } finally {
                CoordinateOperations.CONSTANT_COORDINATES.remove();
            }
            knowChangeOfCRS = true;
        }
        return changeOfCRS;
    }

    /**
     * Computes the transform from “grid coordinates of the source” to “grid coordinates of the target”.
     * This is a concatenation of {@link #gridToCRS()} with target "CRS to grid" transform.
     *
     * @return operation from source grid indices to target grid indices.
     * @throws FactoryException if no operation can be found between the source and target CRS.
     * @throws TransformException if some coordinates cannot be transformed to the specified target.
     * @throws IncompleteGridGeometryException if required CRS or a "grid to CRS" information is missing.
     */
    final MathTransform gridToGrid() throws FactoryException, TransformException {
        final MathTransform step1 = gridToCRS();
        final MathTransform step2 = target.getGridToCRS(anchor);
        if (step1.equals(step2)) {                                          // Optimization for a common case.
            return MathTransforms.identity(step1.getSourceDimensions());
        } else {
            return MathTransforms.concatenate(step1, step2.inverse());
        }
    }

    /**
     * Computes the transform from “grid coordinates of the source” to “geospatial coordinates of the target”.
     * It may be the identity operation. We try to take envelopes in account because the operation choice may
     * depend on the geographic area.
     *
     * <p>The transform returned by this method applies wraparound checks systematically on every axes having
     * wraparound range. This method does not verify whether those checks are needed (i.e. whether wraparound
     * can possibly happen). This is okay because this transform is used only for transforming envelopes;
     * it is not used for transforming pixel coordinates.</p>
     *
     * <h4>Implementation note</h4>
     * After invocation of this method, the following fields are valid:
     * <ul>
     *   <li>{@link #changeOfCRS} — cached for {@link #inverse()} usage.</li>
     *   <li>{@link #forwardChangeOfCRS} — cached for next invocation of this {@code gridToCRS()} method.</li>
     * </ul>
     *
     * @return operation from source grid indices to target geospatial coordinates.
     * @throws FactoryException if no operation can be found between the source and target CRS.
     * @throws TransformException if some coordinates cannot be transformed to the specified target.
     * @throws IncompleteGridGeometryException if required CRS or a "grid to CRS" information is missing.
     */
    final MathTransform gridToCRS() throws FactoryException, TransformException {
        if (gridToCRS == null) {
            /*
             * The following line may throw IncompleteGridGeometryException, which is desired because
             * if that transform is missing, we cannot continue (we have no way to guess it).
             */
            gridToCRS = source.getGridToCRS(anchor);
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            final CoordinateOperation changeOfCRS = changeOfCRS();
            if (changeOfCRS != null) {
                /*
                 * At this point we have a "grid → source CRS" transform. Append a "source CRS → target CRS" transform,
                 * which may be identity. A wraparound may be applied for keeping target coordinates inside the expected
                 * target domain.
                 */
apply:          if (forwardChangeOfCRS == null) {
                    forwardChangeOfCRS = changeOfCRS.getMathTransform();
                    if (!isWraparoundDisabled) {
                        DirectPosition sourceMedian = median(source, forwardChangeOfCRS);
                        DirectPosition targetMedian = median(target, null);
                        if (targetMedian == null) {
                            if (sourceMedian == null) {
                                break apply;
                            }
                            targetMedian = sourceMedian;
                            sourceMedian = null;
                        }
                        final WraparoundApplicator ap = new WraparoundApplicator(sourceMedian,
                                targetMedian, changeOfCRS.getTargetCRS().getCoordinateSystem());
                        forwardChangeOfCRS = ap.forDomainOfUse(forwardChangeOfCRS);
                    }
                }
                gridToCRS = MathTransforms.concatenate(gridToCRS, forwardChangeOfCRS);
            }
        }
        return gridToCRS;
    }

    /**
     * Computes the transform from “geospatial coordinates of the target” to “grid coordinates of the source”.
     * This is similar to invoking {@link MathTransform#inverse()} on {@link #gridToCRS()}, except in the way
     * wraparounds are handled.
     *
     * @return operation from target geospatial coordinates to source grid indices.
     * @throws FactoryException if no operation can be found between the source and target CRS.
     * @throws TransformException if some coordinates cannot be transformed.
     */
    final MathTransform inverse() throws FactoryException, TransformException {
        final MathTransform sourceCrsToGrid = source.getGridToCRS(anchor).inverse();
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final CoordinateOperation changeOfCRS = changeOfCRS();
        if (changeOfCRS == null) {
            return sourceCrsToGrid;
        }
        if (inverseChangeOfCRS == null) {
            inverseChangeOfCRS = changeOfCRS.getMathTransform().inverse();
            if (!isWraparoundDisabled) {
                isWraparoundApplied = false;
                if (!isWraparoundNeedVerified) {
                    isWraparoundNeedVerified = true;
                    /*
                     * Need to compute transform with wraparound checks, but contrarily to `gridToCRS()` we do not want
                     * `WraparoundTransform` to be systematically inserted. This is for performance reasons, because the
                     * transform returned by this method will be applied on every pixels of destination image. We create
                     * both transforms with and without wraparound, and check if their results differ.
                     *
                     * We give precedence to corners specified by target extent. The corners specified by source extent
                     * are used only as a fallback if the target extent has not been specified, in which case we assume
                     * that caller will fallback on source extent transformed to target coordinates.  The target extent
                     * is preferred because it may cover only a sub-region of the source, or conversely it may be world.
                     * If smaller, wraparound may become useless (i.e. sub-region may not cross anti-meridian anymore).
                     * If larger with [-180 … +180]° longitude range, the use of source extent may fail to detect that
                     * a part of the raster need to be rendered on each side of the [-180 … +180]° range.
                     */
                    final MathTransform inverseNoWrap = inverseChangeOfCRS;
                    final MathTransform crsToGridNoWrap = MathTransforms.concatenate(inverseNoWrap, sourceCrsToGrid);
                    if (target.isDefined(GridGeometry.EXTENT | GridGeometry.CRS)) {
                        if (applyWraparound(sourceCrsToGrid)) {
                            isWraparoundNeeded = isWraparoundNeeded(target.getExtent(),
                                    target.getGridToCRS(anchor), crsToGridNoWrap, null);
                        }
                    } else if (source.isDefined(GridGeometry.EXTENT)) {
                        isWraparoundNeeded = isWraparoundNeeded(source.getExtent(),
                                gridToCRS(), crsToGridNoWrap, sourceCrsToGrid);
                    }
                    if (!isWraparoundNeeded) {
                        inverseChangeOfCRS = inverseNoWrap;     // Discard the transform that was applying wraparound.
                        crsToGrid = crsToGridNoWrap;
                    }
                }
                if (isWraparoundNeeded) {
                    applyWraparound(sourceCrsToGrid);           // Update `inverseChangeOfCRS` if possible.
                }
            }
        }
        /*
         * Here, `inverseChangeOfCRS` already contains the wraparound step if needed.
         */
        if (crsToGrid == null) {
            crsToGrid = MathTransforms.concatenate(inverseChangeOfCRS, sourceCrsToGrid);
        }
        return crsToGrid;
    }

    /**
     * Verifies whether wraparound is needed for a "CRS to grid" transform.
     * This method converts coordinates of all corners of a grid (source or target) to the target CRS,
     * then (potentially back) to the source grid. This method uses one transform applying wraparounds
     * and another transform without wraparounds. By checking whether grid coordinates are equal with
     * both transforms, we determine if wraparound is necessary or not.
     *
     * @param  extent           the grid extent which is providing all corners to project.
     * @param  extentToCRS      transform from {@code extent} to target CRS.
     * @param  crsToGridNoWrap  inverse of {@code gridToCRS} but without handling of wraparound axes.
     * @param  sourceCrsToGrid  if {@code extent} is {@link #target} extent, shall be {@code null}.
     *                          If {@code extent} is {@link #source} extent, shall be the transform to
     *                          concatenate with {@link #inverseChangeOfCRS} for creating {@link #crsToGrid}.
     * @return whether wraparound transform seems needed.
     * @throws TransformException if an error occurred while transforming coordinates.
     */
    private boolean isWraparoundNeeded(final GridExtent extent, final MathTransform extentToCRS,
            final MathTransform crsToGridNoWrap, final MathTransform sourceCrsToGrid)
            throws FactoryException, TransformException
    {
        final boolean  mapCorner    = (anchor == PixelInCell.CELL_CORNER);
        final int      extentDim    = extent.getDimension();
        final int      gridDim      = crsToGridNoWrap.getTargetDimensions();
        final double[] buffer       = new double[Math.max(extentToCRS.getTargetDimensions(), gridDim)];
        final double[] reference    = new double[Math.max(extentDim, gridDim)];
        final double[] withoutWrap  = new double[gridDim];
        long maskOfUppers = Numerics.bitmask(extentDim);
        while (--maskOfUppers != 0) {
            for (int i=0; i<extentDim; i++) {
                final long bit = 1L << i;
                long cc;                                                // Grid coordinate of a corner.
                if ((maskOfUppers & bit) == 0) {
                    cc = extent.getLow(i);
                } else {
                    cc = extent.getHigh(i);                             // Inclusive.
                    if (mapCorner && cc != Long.MAX_VALUE) cc++;        // Make exclusive.
                }
                reference[i] = cc;
            }
            /*
             * Transform corner from the extent to target CRS, then (potentially back) to source grid coordinates.
             * We do not concatenate the forward and inverse transforms because we do not want MathTransformFactory
             * to simplify the transformation chain (e.g. replacing "Mercator → Inverse of Mercator" by an identity
             * transform), because such simplification would erase wraparound effects.
             */
            extentToCRS.transform(reference, 0, buffer, 0, 1);              // To coordinates in target CRS.
            crsToGridNoWrap.transform(buffer, 0, withoutWrap, 0, 1);        // To coordinates in source grid.
            /*
             * The reference must be a corner in the `source` grid. If the given extent was from `target` grid,
             * convert to source grid coordinates by completing the "target → CRS → source" chain of transforms.
             * The `crsToGrid` transform includes the wraparound, contrarily to `crsToGridNoWrap` used above.
             */
            if (sourceCrsToGrid == null) {
                // `applyWraparound()` already invoked by caller.
                crsToGrid.transform(buffer, 0, reference, 0, 1);
            }
            /*
             * Compare coordinates without wraparound with the reference. If they differ, we may consider that
             * wraparounds are needed.
             */
            boolean isBufferTransformed = false;
            for (int i=0; i<gridDim; i++) {
                final double error = Math.abs(withoutWrap[i] - reference[i]);
                if (!(error <= 1)) {                                            // Use `!` for catching NaN.
                    if (sourceCrsToGrid == null) {
                        /*
                         * If the `extent` specified in argument was the target extent, then the `reference`
                         * corner has already been computed using a transform that include wraparound checks.
                         * We do not consider NaN in `reference` as a need for wraparound because NaN values
                         * may occur when an operation between two CRS reduces the number of dimensions.
                         */
                        if (!Double.isNaN(reference[i])) {
                            return true;
                        }
                    } else {
                        /*
                         * If the `extent` specified in argument was the source extent, then the `reference`
                         * corner has been computed with a transform that does not use `inverseChangeOfCRS`.
                         * So it may be worth to double-check with another calculation of the corner, this
                         * time including wraparound steps.
                         */
                        if (!isBufferTransformed) {
                            isBufferTransformed = true;
                            if (!applyWraparound(sourceCrsToGrid)) {
                                return false;
                            }
                            crsToGrid.transform(buffer, 0, buffer, 0, 1);
                        }
                        if (Math.abs(buffer[i] - reference[i]) < (error <= Double.MAX_VALUE ? error : 1)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Inserts {@link WraparoundTransform} steps into {@link #inverseChangeOfCRS} transform if possible.
     * IF this method returns {@code true}, then the {@link #inverseChangeOfCRS} and {@link #crsToGrid}
     * fields have been updated to transforms applying wraparound.
     *
     * @param  sourceCrsToGrid  value of {@code source.getGridToCRS(anchor).inverse()}.
     * @return whether at least one wraparound step has been added.
     * @throws TransformException if some coordinates cannot be transformed.
     */
    private boolean applyWraparound(final MathTransform sourceCrsToGrid) throws FactoryException, TransformException {
        if (!isWraparoundApplied) {
            isWraparoundApplied = true;
            DirectPosition sourceMedian = median(source, null);
            DirectPosition targetMedian = median(target, inverseChangeOfCRS);
            if (sourceMedian == null) {
                sourceMedian = targetMedian;
                targetMedian = null;
            }
            if (sourceMedian != null) {
                final MathTransform inverseNoWrap = inverseChangeOfCRS;
                final WraparoundApplicator ap = new WraparoundApplicator(targetMedian,
                        sourceMedian, changeOfCRS().getSourceCRS().getCoordinateSystem());
                inverseChangeOfCRS = ap.forDomainOfUse(inverseNoWrap);
                if (inverseChangeOfCRS != inverseNoWrap) {
                    crsToGrid = MathTransforms.concatenate(inverseChangeOfCRS, sourceCrsToGrid);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the point of interest converted to the Coordinate Reference System.
     * If the grid does not define a point of interest or does not define a CRS,
     * then this method returns {@code null}.
     *
     * @param  grid         the source or target grid providing the point of interest.
     * @param  changeOfCRS  transform from source CRS to target CRS, or {@code null} if none.
     */
    private static DirectPosition median(final GridGeometry grid, final MathTransform changeOfCRS) throws TransformException {
        if (!grid.isDefined(GridGeometry.EXTENT | GridGeometry.GRID_TO_CRS)) {
            return null;
        }
        return new AbstractDirectPosition() {
            /** The coordinates, computed when first needed. */
            private double[] coordinates;

            /** Returns the number of dimensions. */
            @Override public int getDimension() {
                return coordinates().length;
            }

            /** Returns the coordinate tuple, computed when first needed. */
            @SuppressWarnings("ReturnOfCollectionOrArrayField")
            private double[] coordinates() {
                if (coordinates == null) try {
                    final double[] poi = grid.getExtent().getPointOfInterest(PixelInCell.CELL_CENTER);
                    MathTransform tr = grid.getGridToCRS(PixelInCell.CELL_CENTER);
                    if (changeOfCRS != null) {
                        tr = MathTransforms.concatenate(tr, changeOfCRS);
                    }
                    coordinates = new double[tr.getTargetDimensions()];
                    tr.transform(poi, 0, coordinates, 0, 1);
                } catch (TransformException e) {
                    throw new BackingStoreException(e);
                }
                return coordinates;
            }

            /**
             * Returns the median rounded to a value having an exact representation in base 2 using about 10 bits.
             * The intent is to reduce the risk of rounding errors with add/subtract operations.
             */
            @Override public double getCoordinate(final int i) {
                final double m = coordinates()[i];
                final int power = 10 - Math.getExponent(m);
                return Math.scalb(Math.rint(Math.scalb(m, power)), -power);
            }
        };
    }

    /**
     * Invoked when the target CRS has some dimensions that the source CRS does not have.
     * For example, this is invoked during the conversion from (<var>x</var>, <var>y</var>)
     * coordinates to (<var>x</var>, <var>y</var>, <var>t</var>). If constant values can
     * be given to the missing dimensions, than those values are returned. Otherwise this
     * method returns {@code null}.
     *
     * <p>The returned array has a length equals to the number of dimensions in the target CRS.
     * Only coordinates in dimensions without source (<var>t</var> in the above example) will be used.
     * All other coordinate values will be ignored.</p>
     *
     * @see org.apache.sis.referencing.operation.CoordinateOperationContext#getConstantCoordinates()
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public double[] get() {
        if (coordinates == null && target.isDefined(GridGeometry.EXTENT | GridGeometry.GRID_TO_CRS)) {
            final MathTransform tr = target.getGridToCRS(anchor);
            coordinates = new double[tr.getTargetDimensions()];
            double[] gc = new double[tr.getSourceDimensions()];
            Arrays.fill(gc, Double.NaN);
            final GridExtent extent = target.getExtent();
            for (int i=0; i<gc.length; i++) {
                final long low = extent.getLow(i);
                if (low == extent.getHigh(i)) {
                    gc[i] = low;
                }
            }
            /*
             * At this point, the only grid coordinates with finite values are the ones where the
             * grid size is one cell (i.e. conversion to target CRS can produce only one value).
             * After conversion with `getGidToCRS(anchor)`, the corresponding target dimensions
             * will have non-NaN coordinate values only if they do not depend on any dimension
             * other than the one having a grid size of 1.
             */
            try {
                tr.transform(gc, 0, coordinates, 0, 1);
            } catch (TransformException e) {
                throw new BackingStoreException(e);
            }
        }
        return coordinates;
    }

    /**
     * Configures the accuracy hints on the given processor.
     *
     * <h4>Prerequisite</h4>
     * This method assumes that {@link #gridToCRS()} or {@link #inverse()}
     * has already been invoked before this method.
     */
    final void setAccuracyOf(final ImageProcessor processor) {
        final double accuracy = CRS.getLinearAccuracy(changeOfCRS);
        if (accuracy > 0) {
            Length qm = Quantities.create(accuracy, Units.METRE);
            Quantity<?>[] hints = processor.getPositionalAccuracyHints();       // Array is already a copy.
            for (int i=0; i<hints.length; i++) {
                if (Units.isLinear(hints[i].getUnit())) {
                    hints[i] = qm;
                    qm = null;
                }
            }
            if (qm != null) {
                hints = ArraysExt.append(hints, qm);
            }
            processor.setPositionalAccuracyHints(hints);                        // Null elements will be ignored.
        }
    }

    /**
     * Invoked when an ignorable exception occurred.
     *
     * @param  caller  the method where the exception occurred.
     * @param  e       the ignorable exception.
     */
    private static void recoverableException(final String caller, final Exception e) {
        Logging.recoverableException(GridExtent.LOGGER, CoordinateOperationFinder.class, caller, e);
    }
}
