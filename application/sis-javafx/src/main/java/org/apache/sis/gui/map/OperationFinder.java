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
package org.apache.sis.gui.map;

import java.util.function.Predicate;
import javafx.concurrent.Task;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.gui.coverage.CoverageCanvas;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.internal.system.Modules;

import static java.util.logging.Logger.getLogger;


/**
 * Finds a coordinate operation between two CRS in the context of a {@link MapCanvas}.
 * The operation may depend on the region visible in the canvas and the resolution.
 * Computing the coordinate operation may be costly, and for this reason should be
 * done in a background thread.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
abstract class OperationFinder extends Task<MathTransform> {
    /**
     * The grid geometry of data, or {@code null} if none or unknown. This is used for getting the operation between
     * two CRSs when one of the source or target CRS is {@link org.apache.sis.referencing.CommonCRS.Engineering#GRID}.
     * Because the relationship from {@code GRID} CRS to a geospatial CRS is unknown to {@code CRS.findOperation(…)},
     * the operation can not be found without the help of this {@code dataGeometry} field.
     *
     * Actually this information is rarely needed. It is needed only if there is no known affine transform from grid
     * coordinates to some geospatial coordinates that we can use as a starting point (before to apply map projection
     * to other CRSs if desired). For example some netCDF files provides the coordinates of each pixel in data arrays.
     * Those data arrays can be stored (indirectly) in this {@code dataGeometry} object.
     */
    private final GridGeometry dataGeometry;

    /**
     * Region visible in the map canvas, or {@code null} if none. May be in any CRS.
     */
    private final Envelope areaOfInterest;

    /**
     * The source and target CRS requested by the user. This is usually the {@link #operation} source
     * and target CRS, unless a CRS was a {@linkplain #isGridCRS(CoordinateReferenceSystem) grid CRS}.
     */
    private final CoordinateReferenceSystem sourceCRS, targetCRS;

    /**
     * {@code true} if {@link #sourceCRS} or {@link #targetCRS} is a
     * {@linkplain #isGridCRS(CoordinateReferenceSystem) grid CRS}.
     */
    private boolean sourceIsGrid, targetIsGrid;

    /**
     * The coordinate operation from {@link #sourceCRS} to {@link #targetCRS}, computed in background thread.
     * The {@link CoordinateOperation#getMathTransform()} value may not be the complete transform returned by
     * {@link #getValue()} because the latter may include transform from/to {@linkplain #isGridCRS grid CRS}.
     */
    private CoordinateOperation operation;

    /**
     * Creates a new task for finding the coordinate operation between two CRS.
     *
     * @param canvas          the canvas for which we are searching a coordinate operation, or {@code null}.
     * @param areaOfInterest  region visible in the map canvas, or {@code null}. May be in any CRS.
     * @param sourceCRS       source CRS of the transform to compute.
     * @param targetCRS       target CRS of the transform to compute.
     */
    protected OperationFinder(final MapCanvas canvas,
                              final Envelope  areaOfInterest,
                              final CoordinateReferenceSystem sourceCRS,
                              final CoordinateReferenceSystem targetCRS)
    {
        this.dataGeometry   = dataGeometry(canvas);
        this.sourceCRS      = sourceCRS;
        this.targetCRS      = targetCRS;
        this.areaOfInterest = areaOfInterest;
    }

    /**
     * Returns the <em>data</em> (not canvas) grid geometry, or {@code null} if none.
     */
    private static GridGeometry dataGeometry(final MapCanvas canvas) {
        if (canvas instanceof CoverageCanvas) {
            final GridCoverage coverage = ((CoverageCanvas) canvas).getCoverage();
            if (coverage != null) {
                return coverage.getGridGeometry();
            }
        }
        return null;
    }

    /**
     * Computes the transform from the source CRS to the target CRS specified at construction time.
     * This method is invoked in a background thread and does not need to be invoked explicitly.
     */
    @Override
    protected MathTransform call() throws Exception {
        DefaultGeographicBoundingBox bbox = null;
        if (areaOfInterest != null) try {
            bbox = new DefaultGeographicBoundingBox();
            bbox.setBounds(areaOfInterest);
        } catch (TransformException e) {
            bbox = null;
            Logging.recoverableException(getLogger(Modules.APPLICATION), getCallerClass(), getCallerMethod(), e);
        }
        MathTransform before = null;
        MathTransform after  = null;
        CoordinateReferenceSystem source = sourceCRS;
        CoordinateReferenceSystem target = targetCRS;
        if (dataGeometry != null && dataGeometry.isDefined(GridGeometry.CRS | GridGeometry.GRID_TO_CRS)) {
            if (sourceIsGrid = isGridCRS(source)) {
                before = dataGeometry.getGridToCRS(PixelInCell.CELL_CENTER);
                source = dataGeometry.getCoordinateReferenceSystem();
            }
            if (targetIsGrid = isGridCRS(target)) {
                after  = dataGeometry.getGridToCRS(PixelInCell.CELL_CENTER).inverse();
                target = dataGeometry.getCoordinateReferenceSystem();
            }
        }
        operation = CRS.findOperation(source, target, bbox);
        MathTransform transform = operation.getMathTransform();
        if (before != null) transform = MathTransforms.concatenate(before, transform);
        if (after  != null) transform = MathTransforms.concatenate(transform, after);
        return transform;
    }

    /**
     * If the given CRS is a grid CRS, replaces it by a geospatial CRS if possible.
     * If the given CRS is not geospatial, then this method tries to replace it by
     * the CRS of the coverage shown by the canvas (this is not necessarily the
     * {@linkplain MapCanvas#getObjectiveCRS() objective CRS}).
     *
     * @param  crs     the CRS to eventually replace by a geospatial CRS.
     * @param  canvas  the canvas that this status bar is tracking.
     */
    static CoordinateReferenceSystem toGeospatial(CoordinateReferenceSystem crs, final MapCanvas canvas) {
        if (isGridCRS(crs)) {
            final GridGeometry dataGeometry = dataGeometry(canvas);
            if (dataGeometry != null && dataGeometry.isDefined(GridGeometry.CRS)) {
                crs = dataGeometry.getCoordinateReferenceSystem();
            }
        }
        return crs;
    }

    /**
     * Returns {@code true} if the given coordinate reference system is the CRS of the grid.
     * We use the {@link org.apache.sis.referencing.CommonCRS.Engineering#GRID} datum as a signature.
     */
    private static boolean isGridCRS(final CoordinateReferenceSystem crs) {
        return (crs instanceof SingleCRS) && CommonCRS.Engineering.GRID.datum().equals(((SingleCRS) crs).getDatum());
    }

    /**
     * Returns the coordinate operation computed by the {@link #call()} method. The associated transform can be
     * obtained by {@link #getValue()}. The {@link CoordinateOperation#getMathTransform()} method should not be
     * used because it may be incomplete if the source or target CRS was a grid CRS.
     */
    public final CoordinateOperation getOperation() {
        return operation;
    }

    /**
     * Returns the target CRS, giving precedence to {@link CoordinateOperation#getTargetCRS()} if suitable.
     * That precedence is because the {@link CoordinateOperation} may provide a more complete CRS from EPSG
     * database.
     */
    public final CoordinateReferenceSystem getTargetCRS() {
        if (!targetIsGrid) {
            final CoordinateReferenceSystem crs = operation.getTargetCRS();
            if (crs != null) return crs;
        }
        return targetCRS;
    }

    /**
     * Returns a predicate for determining if {@link OperationFinder} task need to be executed again.
     * If there is no need to perform such check, returns {@code null}.
     *
     * <p><b>Note:</b> actually recomputing everything is a bit overly aggresive.  We could keep the
     * {@link CoordinateOperation} found by {@link #call()} and just update the {@link MathTransform}
     * before or after the operation. But the use of a grid CRS should be rare enough that it is not
     * worth to do this optimization.</p>
     *
     * @see StatusBar#fullOperationSearchRequired
     */
    final Predicate<MapCanvas> fullOperationSearchRequired() {
        return (sourceIsGrid | targetIsGrid) ? new UpdateCheck(dataGeometry) : null;
    }

    /**
     * The predicate for determining if {@link OperationFinder} task needs to be executed again.
     */
    private static final class UpdateCheck implements Predicate<MapCanvas> {
        private final GridGeometry dataGeometry;

        UpdateCheck(final GridGeometry dataGeometry) {
            this.dataGeometry = dataGeometry;
        }

        @Override public boolean test(final MapCanvas canvas) {
            return !dataGeometry.equals(dataGeometry(canvas));
        }
    }

    /**
     * Returns the class to report as the caller in case of non-fatal error. This is used for logging.
     */
    protected abstract Class<?> getCallerClass();

    /**
     * Returns the method to report as the caller in case of non-fatal error. This is used for logging.
     */
    protected abstract String getCallerMethod();
}
