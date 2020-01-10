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
package org.apache.sis.internal.map;

import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;
import org.apache.sis.internal.referencing.provider.Affine;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.crs.DefaultDerivedCRS;
import org.apache.sis.referencing.operation.DefaultConversion;
import org.apache.sis.referencing.operation.transform.TransformSeparator;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.collection.BackingStoreException;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;


/**
 * Area when an grid coverage is displayed.
 *
 * <p>
 * NOTE: this class is a first draft subject to modifications.
 * </p>
 *
 * @author  Johann Sorel (Geomatys)
 * @version 2.0
 * @since   2.0
 * @module
 */
public abstract class GridCanvas {
    /**
     * The operation method used by {@link #getDisplayCRS()}.
     * This is a temporary constant, as we will probably need to replace the creation
     * of a {@link DefaultDerivedCRS} by something else. After that replacement, this
     * constant will be removed.
     */
    private static final Affine DISPLAY_TO_OBJECTIVE_OPERATION = new Affine();

    private GridGeometry gridGeometry = new GridGeometry(new GridExtent(360, 180), CRS.getDomainOfValidity(CommonCRS.WGS84.normalizedGeographic()));
    private GridGeometry gridGeometry2d = gridGeometry;
    private boolean proportion = true;

    public GridGeometry getGridGeometry() {
        return gridGeometry;
    }

    /**
     * Set global N dimension grid geometry.
     *
     * @param gridGeometry new grid geometry
     */
    public final void setGridGeometry(GridGeometry gridGeometry) throws FactoryException {
        ArgumentChecks.ensureNonNull("gridGeometry", gridGeometry);
        if (this.gridGeometry.equals(gridGeometry)) return;
        final GridGeometry old = this.gridGeometry;
        this.gridGeometry = gridGeometry;

        {
            final CoordinateReferenceSystem crs = gridGeometry.getCoordinateReferenceSystem();
            if (crs.getCoordinateSystem().getDimension() == 2) {
                gridGeometry2d = gridGeometry;
            } else {
                final CoordinateReferenceSystem crs2d = getHorizontalComponent(crs);
                final int idx = getHorizontalIndex(crs);
                final MathTransform gridToCRS = gridGeometry.getGridToCRS(PixelInCell.CELL_CENTER);
                final TransformSeparator sep = new TransformSeparator(gridToCRS);
                sep.addTargetDimensions(idx, idx+1);
                final MathTransform gridToCRS2D = sep.separate();

                //we are expecting axis index to be preserved from grid to crs
                final GridExtent extent = gridGeometry.getExtent().reduce(idx, idx+1);
                gridGeometry2d = new GridGeometry(extent, PixelInCell.CELL_CENTER, gridToCRS2D, crs2d);
            }
        }
    }

    /**
     * Get 2 dimension grid geometry.
     * This grid geometry only has the 2D CRS part of the global grid geometry.
     *
     * @return GridGeometry 2D, never null
     */
    public final GridGeometry getGridGeometry2D() {
        return gridGeometry2d;
    }

    public final CoordinateReferenceSystem getObjectiveCRS() {
        return gridGeometry.getCoordinateReferenceSystem();
    }

    public final CoordinateReferenceSystem getObjectiveCRS2D() {
        return gridGeometry2d.getCoordinateReferenceSystem();
    }

    public final CoordinateReferenceSystem getDisplayCRS() {
        /*
         * TODO: will need a way to avoid the cast below. In my understanding, DerivedCRS may not be the appropriate
         *       CRS to create after all, because in ISO 19111 a DerivedCRS is more than just a base CRS with a math
         *       transform. A DerivedCRS may also "inherit" some characteritics of the base CRS. For example if the
         *       base CRS is a VerticalCRS, then the DerivedCRS may also implement VerticalCRS.
         *
         *       I'm not yet sure what should be the appropriate kind of CRS to create here. ImageCRS? EngineeringCRS?
         *       How to express the relationship to the base CRS is also not yet determined.
         */
        final SingleCRS objCRS2D = (SingleCRS) getObjectiveCRS2D();
        final Map<String,?> name = Collections.singletonMap(DefaultDerivedCRS.NAME_KEY, "Derived - "+objCRS2D.getName().toString());
        final CoordinateReferenceSystem displayCRS = DefaultDerivedCRS.create(name, objCRS2D,
                new DefaultConversion(name, DISPLAY_TO_OBJECTIVE_OPERATION, getObjectiveToDisplay2D(), null),
                objCRS2D.getCoordinateSystem());
        return displayCRS;
    }

    public final void setObjectiveCRS(final CoordinateReferenceSystem crs) throws TransformException, FactoryException {
        ArgumentChecks.ensureNonNull("Objective CRS", crs);
        if (Utilities.equalsIgnoreMetadata(gridGeometry.getCoordinateReferenceSystem(), crs)) {
            return;
        }

        //store the visible area to restore it later
        final GeneralEnvelope preserve = new GeneralEnvelope(gridGeometry.getEnvelope());

        final int newDim = crs.getCoordinateSystem().getDimension();
        final Envelope env = transform(preserve, crs);
        final int oldidx = getHorizontalIndex(gridGeometry.getCoordinateReferenceSystem());
        final int idx = getHorizontalIndex(crs);
        final GridExtent oldExtent = gridGeometry.getExtent();
        final long[] oldlow = oldExtent.getLow().getCoordinateValues();
        final long[] oldhigh = oldExtent.getHigh().getCoordinateValues();
        final long[] low = new long[newDim];
        final long[] high = new long[newDim];
        low[idx] = oldlow[oldidx];
        low[idx+1] = oldlow[oldidx+1];
        high[idx] = oldhigh[oldidx];
        high[idx+1] = oldhigh[oldidx+1];
        final GridExtent extent = new GridExtent(null, low, high, true);

        GridGeometry gridGeometry = new GridGeometry(extent, env);
        gridGeometry = preserverRatio(gridGeometry);
        setGridGeometry(gridGeometry);
    }

    /**
     * @return a snapshot objective To display transform, in Pixel CENTER
     */
    public final AffineTransform2D getObjectiveToDisplay2D() {
        try {
            MathTransform gridToCRS = getGridGeometry2D().getGridToCRS(PixelInCell.CELL_CENTER);
            return (AffineTransform2D) gridToCRS.inverse();
        } catch (org.opengis.referencing.operation.NoninvertibleTransformException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    public final AffineTransform2D getDisplayToObjective2D() {
        MathTransform gridToCRS = getGridGeometry2D().getGridToCRS(PixelInCell.CELL_CENTER);
        return (AffineTransform2D) gridToCRS;
    }

    /**
     * Set the proportions support between X and Y axis.
     * if false then no correction will be applied
     * if true then one unit in X will be equal to one unit in Y
     */
    public final void setAxisProportions(final boolean prop) {
        this.proportion = prop;
    }

    /**
     *
     * @return the X/Y proportion
     */
    public final boolean getAxisProportions() {
        return proportion;
    }

    public final Rectangle2D getDisplayBounds() {

        final GridGeometry gridGeometry = getGridGeometry();
        final CoordinateReferenceSystem crs = gridGeometry.getCoordinateReferenceSystem();
        final int idx = getHorizontalIndex(crs);

        //we are expecting axis index to be preserved from grid to crs
        final GridExtent extent = gridGeometry.getExtent().reduce(idx, idx+1);

        final Rectangle2D.Double bounds = new Rectangle2D.Double();
        bounds.x = extent.getLow(0);
        bounds.y = extent.getLow(1);
        bounds.width = extent.getSize(0);
        bounds.height = extent.getSize(1);
        return bounds;
    }

    public void setDisplayBounds(Rectangle2D bounds) {
        ArgumentChecks.ensureNonNull("Display bounds", bounds);

        final GridGeometry gridGeometry = getGridGeometry();
        final GridExtent extent = gridGeometry.getExtent();
        final int idx = getHorizontalIndex(gridGeometry.getCoordinateReferenceSystem());

        final long[] low = extent.getLow().getCoordinateValues();
        low[idx] = (long) bounds.getMinX();
        low[idx+1] = ((long) bounds.getMinY());
        final long[] high = extent.getHigh().getCoordinateValues();
        high[idx] = ((long) bounds.getMaxX()) - 1;
        high[idx+1] = ((long) bounds.getMaxY()) - 1;
        final GridExtent newExt = new GridExtent(null, low, high, true);

        final GridGeometry newGrid = new GridGeometry(newExt, PixelInCell.CELL_CENTER, gridGeometry.getGridToCRS(PixelInCell.CELL_CENTER), gridGeometry.getCoordinateReferenceSystem());
        try {
            setGridGeometry(newGrid);
        } catch (FactoryException ex) {
            //we are just changing the size, this should not cause the exception
            //should not happen with current parameters
            throw new BackingStoreException(ex.getMessage(), ex);
        }
    }

    private GridGeometry preserverRatio(GridGeometry gridGeometry) {
        if (proportion) {
            final CoordinateReferenceSystem crs = gridGeometry.getCoordinateReferenceSystem();
            final GridExtent extent = gridGeometry.getExtent();
            final Envelope envelope = gridGeometry.getEnvelope();
            final int idx = getHorizontalIndex(crs);
            final long width = extent.getSize(idx);
            final long height = extent.getSize(idx+1);
            final double sx = envelope.getSpan(idx) / width;
            final double sy = envelope.getSpan(idx+1) / height;
            if (sx != sy) {
                final GeneralEnvelope env = new GeneralEnvelope(envelope);
                if (sx < sy) {
                    double halfSpan = (sy * width / 2.0);
                    double median = env.getMedian(idx);
                    env.setRange(idx, median - halfSpan, median + halfSpan);
                } else {
                    double halfSpan = (sx * height / 2.0);
                    double median = env.getMedian(idx+1);
                    env.setRange(idx+1, median - halfSpan, median + halfSpan);
                }
                gridGeometry = new GridGeometry(extent, env);
            }
        }
        return gridGeometry;
    }

    private static CoordinateReferenceSystem getHorizontalComponent(CoordinateReferenceSystem envCRS) {
        final List<SingleCRS> dcrss = CRS.getSingleComponents(envCRS);
        // Following loop is a temporary hack for decomposing Geographic3D into Geographic2D + ellipsoidal height.
        // This is a wrong thing to do according international standards; we will revisit in a future version.
        for (SingleCRS crs : dcrss) {
            SingleCRS hcrs = CRS.getHorizontalComponent(crs);
            if (hcrs != null) {
                return hcrs;
            }
        }
        throw new RuntimeException("Coordinate system has no horizontal component");
    }

    private static int getHorizontalIndex(CoordinateReferenceSystem envCRS) {
        //set the extra xis if some exist
        int index=0;
        final List<SingleCRS> dcrss = CRS.getSingleComponents(envCRS);

        // Following loop is a temporary hack for decomposing Geographic3D into Geographic2D + ellipsoidal height.
        // This is a wrong thing to do according international standards; we will revisit in a future version.
        for (SingleCRS crs : dcrss) {
            SingleCRS hcrs = CRS.getHorizontalComponent(crs);
            if (hcrs != null) {
                return index;
            }
            index += crs.getCoordinateSystem().getDimension();
        }
        throw new RuntimeException("Coordinate system has no horizontal component");
    }

    /**
     * Transform the given envelope to the given crs.
     * Unlike Envelopes.transform this method handle growing number of dimensions by filling
     * other axes with default values.
     *
     * @param env source Envelope
     * @param targetCRS target CoordinateReferenceSystem
     * @return transformed envelope
     */
    private static Envelope transform(Envelope env, CoordinateReferenceSystem targetCRS) throws TransformException{
        try {
            return Envelopes.transform(env, targetCRS);
        } catch (TransformException ex) {
            //we tried...
        }

        //lazy transform
        final CoordinateReferenceSystem sourceCRS = env.getCoordinateReferenceSystem();
        final GeneralEnvelope result = new GeneralEnvelope(targetCRS);

        //decompose crs
        final List<SingleCRS> sourceParts = CRS.getSingleComponents(sourceCRS);
        final List<SingleCRS> targetParts = CRS.getSingleComponents(targetCRS);

        int sourceAxeIndex=0;
        sourceLoop:
        for (CoordinateReferenceSystem sourcePart : sourceParts){
            final int sourcePartDimension = sourcePart.getCoordinateSystem().getDimension();
            int targetAxeIndex=0;

            targetLoop:
            for (CoordinateReferenceSystem targetPart : targetParts){
                final int targetPartDimension = targetPart.getCoordinateSystem().getDimension();

                //try conversion
                try {
                    final MathTransform trs = CRS.findOperation(sourcePart, targetPart, null).getMathTransform();
                    //we could transform by using two coordinate, but envelope conversion allows to handle
                    //crs singularities more efficiently
                    final GeneralEnvelope partSource = new GeneralEnvelope(sourcePart);
                    for (int i=0;i<sourcePartDimension;i++){
                        partSource.setRange(i, env.getMinimum(sourceAxeIndex+i), env.getMaximum(sourceAxeIndex+i));
                    }
                    final Envelope partResult = Envelopes.transform(trs, partSource);
                    for (int i=0;i<targetPartDimension;i++){
                        result.setRange(targetAxeIndex+i, partResult.getMinimum(i), partResult.getMaximum(i));
                    }
                    break targetLoop;
                } catch (FactoryException ex) {
                    //we tried...
                }

                targetAxeIndex += targetPartDimension;
            }
            sourceAxeIndex += sourcePartDimension;
        }

        return result;
    }
}
