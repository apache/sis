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

import java.util.List;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.coverage.MismatchedCoverageRangeException;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.image.AggregatedImage;
import org.apache.sis.image.ImageProcessor;
import org.apache.sis.image.internal.shared.ImageUtilities;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;


/**
 * A grid coverage which is the aggregation of data from other grid coverages.
 * Current implementation supports only the two-dimensional case returned as {@link GridCoverage2D}.
 * This class is added as a place-holder for a future version supporting the multi-dimensional case.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class AggregatedGridCoverage {
    /**
     * Do not allow instantiation of this class (for now).
     */
    private AggregatedGridCoverage() {
    }

    /**
     * Implementation of {@link GridCoverageProcessor#aggregate(GridCoverage, GridGeometry)}.
     * This method computes transform from <var>Source Grid</var> to <var>Target Grid</var>.
     * If the target {@link GridGeometry} is incomplete, this method provides default values
     * for the missing properties.
     *
     * @param  sources    the grid coverages to aggregates.
     * @param  target     the desired geometry of returned grid coverage. May be incomplete.
     * @param  processor  the processor to use for executing the aggregation of images.
     * @return a grid coverage with the characteristics specified in the given grid geometry.
     * @throws IncompleteGridGeometryException if the source grid geometry is missing an information.
     * @throws TransformException if some coordinates cannot be transformed to the specified target.
     *
     * @todo Current implementation always returns {@link GridCoverage2D} because multi-dimensional
     *       aggregation is not yet supported.
     */
    static GridCoverage create(final GridCoverage[] sources, GridGeometry target, ImageProcessor processor)
            throws TransformException, FactoryException
    {
        // The template is used only for its sample dimensions.
        GridCoverage template = sources[0];
        for (int i=1; i<sources.length; i++) {
            final GridCoverage c = sources[i];
            final List<SampleDimension> sd2 = c.getSampleDimensions();
            final List<SampleDimension> sd1 = template.getSampleDimensions();
            final int n = Math.min(sd1.size(), sd2.size());
            if (!sd1.subList(0, n).equals(sd2.subList(0, n))) {
                throw new MismatchedCoverageRangeException();
            }
            if (sd2.size() > sd1.size()) {
                template = c;
            }
        }
        /*
         * If the target grid geometry does not specify a CRS, try to find a good choice for the CRSs of all sources.
         * This search computes the union of all geographic bounding boxes as a side effect, because the search for a
         * CRS depends on the area of interest.
         */
        boolean modified = false;
        final CoordinateReferenceSystem crs;
        if (target.isDefined(GridGeometry.CRS)) {
            crs = target.getCoordinateReferenceSystem();
        } else {
            final var area = new DefaultGeographicBoundingBox();
            final var all = new CoordinateReferenceSystem[sources.length];
            for (int i=0; i<sources.length; i++) {
                final GridGeometry c = sources[i].getGridGeometry();
                if (c.isDefined(GridGeometry.CRS)) {
                    all[i] = c.getCoordinateReferenceSystem();
                }
                c.getGeographicExtent().ifPresent((box) -> {
                    if (area.isEmpty()) area.setBounds(box);
                    else area.add(box);
                });
            }
            crs = CRS.suggestCommonTarget(area.isEmpty() ? null : area, all);
            modified = true;
        }
        /*
         * If the target grid geometry does not specify a "grid to CRS" transform,
         * search for a linear approximation of the grid with coarsest resolution.
         */
        MathTransform gridToCRS = null;
        if (!target.isDefined(GridGeometry.GRID_TO_CRS)) {
            double max = 0;
            for (int i=0; i<sources.length; i++) {
                final GridGeometry c = sources[i].getGridGeometry();
                if (crs != null && c.isDefined(GridGeometry.CRS | GridGeometry.GRID_TO_CRS)) {
                    MathTransform linear = MathTransforms.concatenate(
                            c.getGridToCRS(PixelInCell.CELL_CORNER),
                            c.createChangeOfCRS(crs).getMathTransform());
                    Matrix matrix = MathTransforms.getMatrix(linear);
                    if (matrix == null) {
                        // Transform is not linear, try to get an approximation.
                        if (!c.isDefined(GridGeometry.EXTENT)) continue;
                        linear = c.getExtent().tangentAtPOI(linear, PixelInCell.CELL_CORNER);
                        matrix = MathTransforms.getMatrix(linear);
                    }
                    double sum =0;
                    for (int y = matrix.getNumRow() - 1; --y >= 0;) {       // Skip last row.
                        for (int x = matrix.getNumCol() - 1; --x >= 0;) {   // Skip last column.
                            double v = matrix.getElement(y, x);
                            if (Double.isFinite(v)) sum += v*v;
                        }
                    }
                    if (sum > max) {
                        max = sum;
                        gridToCRS = linear;
                        modified = true;
                    }
                }
            }
        }
        if (gridToCRS == null) {
            // Fallback, intentional `IncompleteGridGeometryException` if not defined.
            gridToCRS = target.getGridToCRS(PixelInCell.CELL_CORNER);
        }
        /*
         * Get the image bounds from the extent. If none, we will let `AggregatedImage`
         * compute and extent itself and use its result for a new `GridGeometry`.
         */
        GridExtent extent = null;
        Rectangle  bounds = null;
        if (target.isDefined(GridGeometry.EXTENT)) {
            extent = target.getExtent();
            bounds = extent.toRectangle(true);
        }
        if (modified) {
            target = new GridGeometry(extent, PixelInCell.CELL_CORNER, gridToCRS, crs);
        }
        /*
         * Build the list of source images, execute the aggregation, then wraps the result.
         */
        final var images = new AggregatedImage.Source[sources.length];
        for (int i=0; i<sources.length; i++) {
            final GridCoverage c = sources[i];
            final MathTransform tr = c.getGridGeometry().createTransformTo(target, PixelInCell.CELL_CORNER);
            images[i] = new AggregatedImage.Source(c.render(null), tr);
        }
        processor = processor.clone();
        processor.setFillValues(template.getBackground());
        final RenderedImage image = processor.aggregate(bounds, images);
        if (extent == null) {
            bounds = ImageUtilities.getBounds(image);
            extent = new GridExtent(bounds);
            target = new GridGeometry(extent, PixelInCell.CELL_CORNER, gridToCRS, crs);
        }
        return new GridCoverage2D(template, target, extent, image);
    }
}
