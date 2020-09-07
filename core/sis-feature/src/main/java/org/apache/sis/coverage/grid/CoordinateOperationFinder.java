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
import org.opengis.referencing.cs.RangeMeaning;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.referencing.CoordinateOperations;
import org.apache.sis.internal.referencing.WraparoundTransform;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.image.ImageProcessor;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.referencing.CRS;


/**
 * Finds a transform from points expressed in the CRS of a source coverage to points in the CRS of a target coverage.
 * This class differs from {@link CRS#findOperation CRS#findOperation(…)} because of the gridded aspect of inputs.
 * {@linkplain GridGeometry grid geometries} give more information about how referencing is applied on datasets.
 * With them, we can detect dimensions where target coordinates are constrained to constant values
 * because the {@linkplain GridExtent#getSize(int) grid size} is only one cell in those dimensions.
 * This is an important difference because it allows us to find operations normally impossible,
 * where we still can produce an operation to a target CRS even if some dimensions have no corresponding source CRS.
 *
 * <p><b>Note:</b> this class does not provide complete chain of transformation from grid to grid.
 * It provides only the operation from the CRS of source to the CRS of destination.</p>
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
     * The value is cached in case {@code get()} is invoked multiple times during the same finder execution.
     */
    private double[] coordinates;

    /**
     * The coordinate operation from source to target CRS, computed when first needed.
     */
    private CoordinateOperation operation;

    /**
     * The {@link #operation} transform together with {@link WraparoundTransform} if needed.
     * The wraparound is used for handling images crossing the anti-meridian.
     */
    private MathTransform forwardOp;

    /**
     * Inverse of {@link #operation} transform together with {@link WraparoundTransform} if needed.
     * The wraparound is used for handling images crossing the anti-meridian.
     */
    private MathTransform inverseOp;

    /**
     * Whether {@link #inverseOp} needs to include a {@link WraparoundTransform} step. We do this check
     * only for {@link #inverseOp} because it is the transform which will be executed for every pixels.
     * By contrast, {@link #forwardOp} will systematically contains a {@link WraparoundTransform} step
     * because we use it only for transforming envelopes and for the test that determines the value of
     * this {@code isWraparoundNeeded} flag.
     */
    private boolean isWraparoundNeeded;

    /**
     * The factory to use for {@link MathTransform} creations. For now this is fixed to SIS implementation.
     * But it may become a configurable reference in a future version if useful.
     *
     * @see org.apache.sis.coverage.grid.ImageRenderer#mtFactory
     */
    private final MathTransformFactory mtFactory;

    /**
     * Creates a new finder.
     *
     * @param  source  the grid geometry which is the source of the coordinate operation to find.
     * @param  target  the grid geometry which is the target of the coordinate operation to find.
     */
    CoordinateOperationFinder(final GridGeometry source, final GridGeometry target) {
        this.source = source;
        this.target = target;
        mtFactory = DefaultFactories.forBuildin(MathTransformFactory.class,
                org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory.class).caching(false);
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
     * Computes the transform from the grid coordinates of the source to geospatial coordinates of the target.
     * It may be the identity operation. We try to take envelopes in account because the operation choice may
     * depend on the geographic area.
     *
     * <p>The transform returned by this method always apply wraparounds on every axes having wraparound range.
     * This method does not check if wraparounds actually happen. This is okay because this transform is used
     * only for transforming envelopes; it is not used for transforming pixel coordinates. By contrast pixel
     * transform (computed by {@link #inverse(MathTransform)}) will need to perform more extensive check.</p>
     *
     * @param  anchor  whether the operation is between cell centers or cell corners.
     * @return operation from source CRS to target CRS, or {@code null} if CRS are unspecified or equivalent.
     * @throws FactoryException if no operation can be found between the source and target CRS.
     * @throws TransformException if some coordinates can not be transformed to the specified target.
     * @throws IncompleteGridGeometryException if required CRS or a "grid to CRS" information is missing.
     */
    final MathTransform gridToCRS(final PixelInCell anchor) throws FactoryException, TransformException {
        /*
         * If `coordinates` is non-null, it means that the `get()` method has been invoked during previous
         * call to this `gridToCRS(…)` method, which implies that `operation` depends on the `anchor` value.
         * In such case we need to discard the previous `operation` value and recompute it.
         */
        this.anchor = anchor;
        if (coordinates != null) {
            coordinates = null;
            operation   = null;
            forwardOp   = null;
            inverseOp   = null;
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
                        if (sourceEnvelope != null || targetEnvelope != null) {
                            areaOfInterest = new DefaultGeographicBoundingBox();
                            areaOfInterest.setBounds(targetEnvelope != null ? targetEnvelope : sourceEnvelope);
                        }
                        operation = CRS.findOperation(sourceCRS, target.getCoordinateReferenceSystem(), areaOfInterest);
                    }
                }
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
        if (forwardOp == null) {
            forwardOp = operation.getMathTransform();
            final CoordinateSystem cs = operation.getTargetCRS().getCoordinateSystem();
wraparound: if (mayRequireWraparound(cs)) {
                DirectPosition median = median(target);
                if (median == null) {
                    median = median(source);
                    if (median == null) break wraparound;
                    median = forwardOp.transform(median, null);
                }
                forwardOp = WraparoundTransform.forDomainOfUse(mtFactory, forwardOp, cs, median);
            }
        }
        return mtFactory.createConcatenatedTransform(tr, forwardOp);
    }

    /**
     * Returns the inverse of the transform returned by last call to {@link #gridToCRS(PixelInCell)}.
     * This is equivalent to invoking {@link MathTransform#inverse()} on the transform returned by
     * {@link #gridToCRS(PixelInCell)}, except in the way wraparounds are handled.
     *
     * <p>The {@code gridToCRS} argument is the value returned by last call to {@link #gridToCRS(PixelInCell)}.
     * That transform is used for testing whether wraparound is needed for calculation of source coordinates.
     * This method performs a more extensive check than {@code gridToCRS(…)} because
     * the transform returned by {@code inverse(…)} will be applied on every pixels of destination image.
     * The argument should be non-null only the first time that this {@code inverse(…)} method is invoked,
     * preferably with a transform mapping {@link PixelInCell#CELL_CORNER}.
     * Subsequent invocations will use the {@link #isWraparoundNeeded} status determined by first invocation.</p>
     */
    final MathTransform inverse(final MathTransform gridToCRS) throws FactoryException, TransformException {
        final MathTransform tr = source.getGridToCRS(anchor).inverse();
        if (operation == null) {
            return tr;
        }
        if (inverseOp == null) {
            inverseOp = operation.getMathTransform().inverse();
check:      if (gridToCRS != null) {
                /*
                 * Transform all corners of source extent to the destination CRS, then back to source grid coordinates.
                 * We do not concatenate the forward and inverse transforms because we do not want MathTransformFactory
                 * to simplify the transformation chain (e.g. replacing "Mercator → Inverse of Mercator" by an identity
                 * transform), because such simplification would erase wraparound effects.
                 */
                final MathTransform crsToGrid = mtFactory.createConcatenatedTransform(inverseOp, tr);
                final GridExtent extent = source.getExtent();
                final int dimension = extent.getDimension();
                final double[] src = new double[dimension];
                final double[] tgt = new double[Math.max(dimension, gridToCRS.getTargetDimensions())];
                for (long maskOfUppers = Numerics.bitmask(dimension); --maskOfUppers != 0;) {
                    long bit = 1;
                    for (int i=0; i<dimension; i++) {
                        src[i] = (maskOfUppers & bit) != 0 ? extent.getHigh(i) + 1d : extent.getLow(i);
                        bit <<= 1;
                    }
                    gridToCRS.transform(src, 0, tgt, 0, 1);
                    crsToGrid.transform(tgt, 0, tgt, 0, 1);
                    for (int i=0; i<dimension; i++) {
                        /*
                         * Do not consider NaN as a need for wraparound because NaN values occur when
                         * the operation between the two CRS reduce the number of dimensions.
                         *
                         * TODO: we may require a more robust check for NaN values.
                         */
                        if (Math.abs(tgt[i] - src[i]) > 1) {
                            isWraparoundNeeded = true;
                            break check;
                        }
                    }
                }
                return crsToGrid;
            }
            /*
             * Potentially append a wraparound if we determined (either in this method invocation
             * or in a previous invocation of this method) that it is necessary.
             */
            if (isWraparoundNeeded) {
                final CoordinateSystem cs = operation.getSourceCRS().getCoordinateSystem();
                if (mayRequireWraparound(cs)) {
                    final DirectPosition median = median(source);
                    if (median != null) {
                        inverseOp = WraparoundTransform.forDomainOfUse(mtFactory, inverseOp, cs, median);
                    }
                }
            }
        }
        return mtFactory.createConcatenatedTransform(inverseOp, tr);
    }

    /**
     * Returns {@code true} if a transform to the specified target coordinate system may require handling
     * of wraparound axes. A {@code true} return value does not mean that wraparounds actually happen;
     * it only means that more expensive checks will be required.
     */
    private static boolean mayRequireWraparound(final CoordinateSystem cs) {
        final int dimension = cs.getDimension();
        for (int i=0; i<dimension; i++) {
            if (RangeMeaning.WRAPAROUND.equals(cs.getAxis(i).getRangeMeaning())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the point of interest converted to the Coordinate Reference System.
     * If the grid does not define a point of interest or does not define a CRS,
     * then this method returns {@code null}.
     */
    private static DirectPosition median(final GridGeometry grid) throws TransformException {
        if (grid.isDefined(GridGeometry.EXTENT | GridGeometry.GRID_TO_CRS)) {
            final double[] poi = grid.getExtent().getPointOfInterest();
            if (poi != null) {
                final MathTransform tr = grid.getGridToCRS(PixelInCell.CELL_CENTER);
                final GeneralDirectPosition median = new GeneralDirectPosition(tr.getTargetDimensions());
                tr.transform(poi, 0, median.coordinates, 0, 1);
                return median;
            }
        }
        return null;
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
}
