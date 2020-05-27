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
import org.opengis.geometry.Envelope;
import org.opengis.util.FactoryException;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.internal.referencing.CoordinateOperations;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;


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
     * The target coordinate values, computed only if needed.
     */
    private double[] coordinates;

    /**
     * The coordinate operation from source to target CRS, computed when first needed.
     */
    private CoordinateOperation operation;

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
     * Computes the transform from the grid coordinates of the source to geospatial coordinates of the target.
     * It may be the identity operation. We try to take envelopes in account because the operation choice may
     * depend on the geographic area.
     *
     * @param  anchor  whether the operation is between cell centers or cell corners.
     * @return operation from source CRS to target CRS, or {@code null} if CRS are unspecified or equivalent.
     * @throws FactoryException if no operation can be found between the source and target CRS.
     * @throws TransformException if some coordinates can not be transformed to the specified target.
     * @throws IncompleteGridGeometryException if required CRS or a "grid to CRS" information is missing.
     */
    final MathTransform gridToCRS(final PixelInCell anchor) throws FactoryException, TransformException {
        /*
         * If `coordinates` is non-null, it means that the `get()` method has been invoked in previous call
         * to this `cornerToCRS(…)` method, which implies that `operation` depends on the `anchor` value.
         * In such case we need to discard the previous `operation` value and recompute it.
         */
        this.anchor = anchor;
        if (operation == null || coordinates != null) {
            operation   = null;
            coordinates = null;
            final CoordinateReferenceSystem sourceCRS = getSourceCRS();
            final CoordinateReferenceSystem targetCRS = target.isDefined(GridGeometry.CRS) ?
                                                        target.getCoordinateReferenceSystem() : sourceCRS;
            final Envelope sourceEnvelope = source.envelope;
            final Envelope targetEnvelope = target.envelope;
            try {
                CoordinateOperations.CONSTANT_COORDINATES.set(this);
                if (sourceEnvelope != null && targetEnvelope != null) {
                    operation = Envelopes.findOperation(sourceEnvelope, targetEnvelope);
                }
                if (operation == null && sourceCRS != null) {
                    DefaultGeographicBoundingBox areaOfInterest = null;
                    if (sourceEnvelope != null || targetEnvelope != null) {
                        areaOfInterest = new DefaultGeographicBoundingBox();
                        areaOfInterest.setBounds(targetEnvelope != null ? targetEnvelope : sourceEnvelope);
                    }
                    operation = CRS.findOperation(sourceCRS, targetCRS, areaOfInterest);
                }
            } finally {
                CoordinateOperations.CONSTANT_COORDINATES.remove();
            }
        }
        /*
         * The following line may throw IncompleteGridGeometryException, which is desired because
         * if that transform is missing, we can not continue (we have no way to guess it).
         */
        MathTransform tr = source.getGridToCRS(anchor);
        if (operation != null) {
            tr = MathTransforms.concatenate(tr, operation.getMathTransform());
        }
        return tr;
    }

    /**
     * Invoked when the target CRS have some dimensions that the source CRS does not have.
     * For example this is invoked during the conversion from (<var>x</var>, <var>y</var>)
     * coordinates to (<var>x</var>, <var>y</var>, <var>t</var>). If constant values can
     * be given to the missing dimensions, than those values are returned. Otherwise this
     * method returns {@code null}.
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
}
