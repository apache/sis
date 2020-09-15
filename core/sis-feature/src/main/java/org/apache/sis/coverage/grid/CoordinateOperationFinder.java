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
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.internal.referencing.CoordinateOperations;
import org.apache.sis.internal.referencing.WraparoundTransform;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.geometry.AbstractDirectPosition;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.image.ImageProcessor;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.internal.system.Modules;


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
 * <b>Note:</b> except for the {@link #gridToGrid(PixelInCell)} convenience method,
 * this class does not provide the complete chain of operations from grid to grid.
 * It provides only the operation from <em>cell indices</em> in source grid to <em>coordinates in the CRS</em>
 * of destination grid. Callers must add the last step (conversion from target CRS to cell indices) themselves.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class CoordinateOperationFinder implements Supplier<double[]> {
    /**
     * Whether the operation is between cell centers or cell corners.
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
     *
     * @see #gridToCRS(PixelInCell)
     */
    private CoordinateOperation operation;

    /**
     * The {@link #operation} transform together with {@link WraparoundTransform} if needed.
     * The wraparound is used for handling images crossing the anti-meridian.
     *
     * @see #gridToCRS(PixelInCell)
     */
    private MathTransform forwardOp;

    /**
     * Inverse of {@link #operation} transform together with {@link WraparoundTransform} if needed.
     * The wraparound is used for handling images crossing the anti-meridian.
     *
     * <p>Contrarily to other {@link MathTransform} fields, determination of this {@code inverseOp}
     * transform should make an effort for checking if wraparound is really needed. This is because
     * {@code inverseOp} will be used much more extensively (for every pixels) than other transforms.</p>
     *
     * @see #isWraparoundNeeded
     * @see #isWraparoundApplied
     * @see #inverse(MathTransform)
     */
    private MathTransform inverseOp;

    /**
     * Transform from the target CRS to the source grid, with {@link WraparoundTransform} applied if needed.
     * This is the concatenation of {@code inverseOp} with inverse of {@link #source} "grid to CRS", except
     * for more conservative application of wraparound (i.e. determination of this transform should not do
     * the simplification effort mentioned in {@link #inverseOp}).
     *
     * @see #inverse(MathTransform)
     * @see #applyWraparound(MathTransform)
     */
    private MathTransform crsToGrid;

    /**
     * Whether {@link #inverseOp} needs to include a {@link WraparoundTransform} step. We do this check
     * only for {@link #inverseOp} because it is the transform which will be executed for every pixels.
     * By contrast, {@link #forwardOp} will systematically contains a {@link WraparoundTransform} step
     * because we use it only for transforming envelopes and for the test that determines the value of
     * this {@code isWraparoundNeeded} flag.
     */
    private boolean isWraparoundNeeded;

    /**
     * Whether {@link WraparoundTransform} has been applied on {@link #inverseOp}. This field complements
     * {@link #isWraparoundNeeded} because a delay may exist between the time we detected that wraparound
     * is needed and the time we added the necessary operation steps.
     *
     * <p>Note that despite this field name, a {@code true} value does not mean that {@link #inverseOp}
     * and {@link #crsToGrid} really contains some {@link WraparoundTransform} steps. It only means that
     * {@link WraparoundTransform#forDomainOfUse WraparoundTransform.forDomainOfUse(…)} has been invoked.
     * That method may have decided to not insert any wraparound steps.</p>
     *
     * @see #applyWraparound(MathTransform)
     */
    private boolean isWraparoundApplied;

    /**
     * Creates a new finder.
     *
     * @param  source  the grid geometry which is the source of the coordinate operation to find.
     * @param  target  the grid geometry which is the target of the coordinate operation to find.
     */
    CoordinateOperationFinder(final GridGeometry source, final GridGeometry target) {
        this.source = source;
        this.target = target;
    }

    /**
     * Returns the CRS of the source grid geometry. If neither the source and target grid geometry
     * define a CRS, then this method returns {@code null}.
     *
     * @throws IncompleteGridGeometryException if the target grid geometry has a CRS but the source
     *         grid geometry has none. Note that the converse is allowed, in which case the target
     *         CRS is assumed the same than the source.
     */
    private CoordinateReferenceSystem getSourceCRS() {
        return source.isDefined(GridGeometry.CRS) ||
               target.isDefined(GridGeometry.CRS) ? source.getCoordinateReferenceSystem() : null;
    }

    /**
     * Returns the target of the "corner to CRS" transform.
     * May be {@code null} if the neither the source and target grid geometry define a CRS.
     *
     * @throws IncompleteGridGeometryException if the target grid geometry has a CRS but the source
     *         grid geometry has none. Note that the converse is allowed, in which case the target
     *         CRS is assumed the same than the source.
     */
    final CoordinateReferenceSystem getTargetCRS() {
        return (operation != null) ? operation.getTargetCRS() : getSourceCRS();
    }

    /**
     * Computes the transform from “grid coordinates of the source” to “grid coordinates of the target”.
     * This is a concatenation of {@link #gridToCRS(PixelInCell)} with target "CRS to grid" transform.
     *
     * @param  anchor  whether the operation is between cell centers or cell corners.
     * @return operation from source grid indices to target grid indices.
     * @throws FactoryException if no operation can be found between the source and target CRS.
     * @throws TransformException if some coordinates can not be transformed to the specified target.
     * @throws IncompleteGridGeometryException if required CRS or a "grid to CRS" information is missing.
     */
    final MathTransform gridToGrid(final PixelInCell anchor) throws FactoryException, TransformException {
        final MathTransform step1 = gridToCRS(anchor);
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
     *   <li>{@link #operation} — cached for {@link #inverse(MathTransform)} usage.</li>
     *   <li>{@link #forwardOp} — cached for next invocation of this {@code gridToCRS(…)} method.</li>
     * </ul>
     *
     * @param  anchor  whether the operation is between cell centers or cell corners.
     * @return operation from source grid indices to target geospatial coordinates.
     * @throws FactoryException if no operation can be found between the source and target CRS.
     * @throws TransformException if some coordinates can not be transformed to the specified target.
     * @throws IncompleteGridGeometryException if required CRS or a "grid to CRS" information is missing.
     */
    final MathTransform gridToCRS(final PixelInCell anchor) throws FactoryException, TransformException {
        /*
         * If `coordinates` is non-null, it means that `CoordinateOperationContext.getConstantCoordinates()`
         * has been invoked during previous `gridToCRS(…)` execution, which implies that `operation` depends
         * on the`anchor` value. In such case we will need to recompute all fields that depend on `operation`.
         */
        this.anchor = anchor;
        if (coordinates != null) {
            coordinates = null;
            operation   = null;
            forwardOp   = null;
            inverseOp   = null;
            crsToGrid   = null;
            // Do not clear `isWraparoundNeeded`; its value is still valid.
        }
        /*
         * Get the operation performing the change of CRS. If more than one operation is defined for
         * the given pair of CRS, select the most appropriate operation for the area of interest.
         *
         * TODO: specify also the desired resolution, computed from target grid geometry.
         *       It will require more direct use of `CoordinateOperationContext`.
         *       As a side effect, we could remove `CONSTANT_COORDINATES` hack.
         */
        if (operation == null) {
            final Envelope sourceEnvelope = source.envelope;
            final Envelope targetEnvelope = target.envelope;
            try {
                CoordinateOperations.CONSTANT_COORDINATES.set(this);
                if (sourceEnvelope != null && targetEnvelope != null) {
                    operation = Envelopes.findOperation(sourceEnvelope, targetEnvelope);
                }
                if (operation == null && target.isDefined(GridGeometry.CRS)) {
                    final CoordinateReferenceSystem sourceCRS = getSourceCRS();
                    if (sourceCRS != null) {
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
                            recoverableException("gridToCRS", e);
                        }
                        operation = CRS.findOperation(sourceCRS, target.getCoordinateReferenceSystem(), areaOfInterest);
                    }
                }
            } catch (BackingStoreException e) {                         // May be thrown by getConstantCoordinates().
                throw e.unwrapOrRethrow(TransformException.class);
            } finally {
                CoordinateOperations.CONSTANT_COORDINATES.remove();
            }
        }
        /*
         * The following line may throw IncompleteGridGeometryException, which is desired because
         * if that transform is missing, we can not continue (we have no way to guess it).
         */
        final MathTransform tr = source.getGridToCRS(anchor);
        if (operation == null) {
            return tr;
        }
        /*
         * At this point we have a "grid → source CRS" transform. Append a "source CRS → target CRS" transform,
         * which may be identity. A wraparound may be applied for keeping target coordinates inside the expected
         * target domain.
         */
apply:  if (forwardOp == null) {
            forwardOp = operation.getMathTransform();
            final CoordinateSystem cs = operation.getTargetCRS().getCoordinateSystem();
            DirectPosition median = median(target, null);
            if (median == null) {
                median = median(source, forwardOp);
                if (median == null) break apply;
            }
            forwardOp = WraparoundTransform.forDomainOfUse(forwardOp, cs, median);
        }
        return MathTransforms.concatenate(tr, forwardOp);
    }

    /**
     * Returns the inverse of the transform returned by last call to {@link #gridToCRS(PixelInCell)}.
     * This is equivalent to invoking {@link MathTransform#inverse()} on the transform returned by
     * {@link #gridToCRS(PixelInCell)}, except in the way wraparounds are handled.
     *
     * <p>The {@code gridToCRS} argument controls whether to perform a more extensive check of wraparound occurrence.
     * If {@code null}, the result of last check is reused. If non-null, then it shall be the value returned by last
     * call to {@link #gridToCRS(PixelInCell)}, preferably with a transform mapping {@link PixelInCell#CELL_CORNER}.
     * The argument should be non-null the first time that {@code inverse(…)} is invoked and {@code null} next time.</p>
     *
     * @param  gridToCRS  result of previous call to {@link #gridToCRS(PixelInCell)}, or {@code null}
     *                    for reusing the {@link #isWraparoundNeeded} result of previous invocation.
     * @return operation from target geospatial coordinates to source grid indices.
     * @throws TransformException if some coordinates can not be transformed.
     */
    final MathTransform inverse(final MathTransform gridToCRS) throws TransformException {
        final MathTransform tr = source.getGridToCRS(anchor).inverse();
        if (operation == null) {
            return tr;
        }
        if (inverseOp != null) {
            // Here, `inverseOp` contains the wraparound step if needed.
            return MathTransforms.concatenate(inverseOp, tr);
        }
        /*
         * Need to compute transform with wraparound checks, but contrarily to `gridToCRS(…)` we do not want
         * `WraparoundTransform` to be systematically inserted. This is for performance reasons, because the
         * transform returned by this method will be applied on every pixels of destination image.  We start
         * with transforms without wraparound, and modify those fields later depending on whether wraparound
         * appears to be necessary or not.
         */
        final MathTransform inverseNoWrap   = operation.getMathTransform().inverse();
        final MathTransform crsToGridNoWrap = MathTransforms.concatenate(inverseNoWrap, tr);
        inverseOp = inverseNoWrap;
        crsToGrid = crsToGridNoWrap;
        isWraparoundApplied = false;
check:  if (gridToCRS != null) {
            /*
             * We will do a more extensive check by converting all corners of source grid to the target CRS,
             * then convert back to the source grid and see if coordinates match. Only if coordinates do not
             * match, `WraparoundTransform.isNeeded(…)` will request a `crsToGrid` transform which includes
             * wraparound steps in order to check if it improves the results. By using a `Supplier`,
             * we avoid creating `WraparoundTransform` in the common case where it is not needed.
             */
            final Supplier<MathTransform> withWraparound = () -> {
                try {
                    return applyWraparound(tr);
                } catch (TransformException e) {
                    throw new BackingStoreException(e);
                }
            };
            final GridExtent extent = source.getExtent();
            final boolean isCorner = (anchor == PixelInCell.CELL_CORNER);
            isWraparoundNeeded = WraparoundTransform.isNeeded(gridToCRS, crsToGridNoWrap, withWraparound, (dim) -> {
                long cc;                                          // Coordinate of a corner.
                if (dim < 0) {
                    cc = extent.getLow(~dim);
                } else {
                    cc = extent.getHigh(dim);                     // Inclusive.
                    if (isCorner && cc != Long.MAX_VALUE) cc++;   // Make exclusive.
                }
                return cc;
            });
        }
        /*
         * At this point we determined whether wraparound is needed. The `inverseOp` and `crsToGrid` fields
         * may have been updated as part of this determination process, but not necessarily. If wraparounds
         * are needed, reuse available calculation results (completing them if needed).  Otherwise rollback
         * any change that `applyWraparound(…)` may have done to `inverseOp` and `crsToGrid`.
         */
        if (isWraparoundNeeded) {
            return applyWraparound(tr);
        } else {
            inverseOp = inverseNoWrap;
            crsToGrid = crsToGridNoWrap;
            return crsToGridNoWrap;
        }
    }

    /**
     * If not already done, inserts {@link WraparoundTransform} steps into {@link #inverseOp} and {@link #crsToGrid}
     * transforms. The transform from geospatial target coordinates to source grid indices is returned for convenience.
     *
     * @param  tr  value of {@code source.getGridToCRS(anchor).inverse()}.
     * @return transform from geospatial target coordinates to source grid indices.
     * @throws TransformException if some coordinates can not be transformed.
     */
    private MathTransform applyWraparound(final MathTransform tr) throws TransformException {
        if (!isWraparoundApplied) {
            isWraparoundApplied = true;
            final CoordinateSystem cs = operation.getSourceCRS().getCoordinateSystem();
            final DirectPosition median = median(source, null);
            if (median != null) {
                inverseOp = WraparoundTransform.forDomainOfUse(inverseOp, cs, median);
                crsToGrid = MathTransforms.concatenate(inverseOp, tr);
            }
        }
        return crsToGrid;
    }

    /**
     * Returns the point of interest converted to the Coordinate Reference System.
     * If the grid does not define a point of interest or does not define a CRS,
     * then this method returns {@code null}.
     *
     * @param  grid       the source or target grid providing the point of interest.
     * @param  forwardOp  transform from source CRS to target CRS, or {@code null} if none.
     */
    private static DirectPosition median(final GridGeometry grid, final MathTransform forwardOp) throws TransformException {
        if (!grid.isDefined(GridGeometry.EXTENT | GridGeometry.GRID_TO_CRS)) {
            return null;
        }
        return new AbstractDirectPosition() {
            /** The coordinates, computed when first needed. */
            private double[] coordinates;

            @Override public int    getDimension()     {return coordinates().length;}
            @Override public double getOrdinate(int i) {return coordinates()[i];}

            /** Returns the coordinate tuple. */
            @SuppressWarnings("ReturnOfCollectionOrArrayField")
            private double[] coordinates() {
                if (coordinates == null) try {
                    final double[] poi = grid.getExtent().getPointOfInterest();
                    MathTransform tr = grid.getGridToCRS(PixelInCell.CELL_CENTER);
                    if (forwardOp != null) {
                        tr = MathTransforms.concatenate(tr, forwardOp);
                    }
                    coordinates = new double[tr.getTargetDimensions()];
                    tr.transform(poi, 0, coordinates, 0, 1);
                } catch (TransformException e) {
                    throw new BackingStoreException(e);
                }
                return coordinates;
            }
        };
    }

    /**
     * Invoked when the target CRS has some dimensions that the source CRS does not have.
     * For example this is invoked during the conversion from (<var>x</var>, <var>y</var>)
     * coordinates to (<var>x</var>, <var>y</var>, <var>t</var>). If constant values can
     * be given to the missing dimensions, than those values are returned. Otherwise this
     * method returns {@code null}.
     *
     * <p>The returned array has a length equals to the number of dimensions in the target CRS.
     * Only coordinates in dimensions without source (<var>t</var> in above example) will be used.
     * All other coordinate values will be ignored.</p>
     *
     * @see org.apache.sis.referencing.operation.CoordinateOperationContext#getConstantCoordinates()
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public double[] get() {
        if (coordinates == null && target.isDefined(GridGeometry.EXTENT | GridGeometry.GRID_TO_CRS)) {
            final MathTransform gridToCRS = target.getGridToCRS(anchor);
            coordinates = new double[gridToCRS.getTargetDimensions()];
            double[] gc = new double[gridToCRS.getSourceDimensions()];
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
             * After conversion with `gridToCRS`, the corresponding target dimensions will have
             * non-NaN coordinate values only if they really do not depend on any dimension other
             * than the one having a grid size of 1.
             */
            try {
                gridToCRS.transform(gc, 0, coordinates, 0, 1);
            } catch (TransformException e) {
                throw new BackingStoreException(e);
            }
        }
        return coordinates;
    }

    /**
     * Configures the accuracy hints on the given processor.
     */
    final void setAccuracyOf(final ImageProcessor processor) {
        final double accuracy = CRS.getLinearAccuracy(operation);
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
        Logging.recoverableException(Logging.getLogger(Modules.RASTER), CoordinateOperationFinder.class, caller, e);
    }
}
