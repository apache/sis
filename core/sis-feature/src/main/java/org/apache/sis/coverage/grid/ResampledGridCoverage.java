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
import java.util.Optional;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import org.opengis.util.FactoryException;
import org.opengis.coverage.CannotEvaluateException;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.image.Interpolation;
import org.apache.sis.image.ResampledImage;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.internal.coverage.j2d.ImageUtilities;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.CRS;

import static java.lang.Math.toIntExact;
import static java.lang.Math.negateExact;
import static java.lang.Math.subtractExact;


/**
 * A multi-dimensional grid coverage where each two-dimensional slice is the resampling
 * of data from another grid coverage. This class is used when the resampling can not be
 * stored in a {@link GridCoverage2D}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class ResampledGridCoverage extends GridCoverage {
    /**
     * The {@value} constant for identifying code specific to the two-dimensional case.
     */
    private static final int BIDIMENSIONAL = 2;

    /**
     * The coverage to resample.
     */
    private final GridCoverage source;

    /**
     * The transform from cell coordinates in this coverage to cell coordinates in {@linkplain #source} coverage.
     * Note that an offset may exist between cell coordinates and pixel coordinates, so some translations may need
     * to be concatenated with this transform on an image-by-image basis.
     */
    private final MathTransform toSource;

    /**
     * The interpolation method to use for resampling images.
     */
    private final Interpolation interpolation;

    /**
     * Indices of extent dimensions corresponding to image <var>x</var> and <var>y</var> coordinates.
     * Typical values are 0 for {@code xDimension} and 1 for {@code yDimension}, but different values
     * are allowed. This class select the dimensions having largest size.
     */
    private final int xDimension, yDimension;

    /**
     * Creates a new grid coverage which will be the resampling of the given source.
     *
     * @param  source         the coverage to resample.
     * @param  domain         the grid extent, CRS and conversion from cell indices to CRS.
     * @param  toSource       transform from cell coordinates in this coverage to source coverage.
     * @param  interpolation  the interpolation method to use for resampling images.
     */
    private ResampledGridCoverage(final GridCoverage source, final GridGeometry domain,
                               final MathTransform toSource, final Interpolation interpolation)
    {
        super(source, domain);
        this.source        = source;
        this.toSource      = toSource;
        this.interpolation = interpolation;
        final GridExtent extent = domain.getExtent();
        long size1 = 0; int idx1 = 0;
        long size2 = 0; int idx2 = 1;
        final int dimension = extent.getDimension();
        for (int i=0; i<dimension; i++) {
            final long size = extent.getSize(i);
            if (size > size1) {
                size2 = size1; idx2 = idx1;
                size1 = size;  idx1 = i;
            } else if (size > size2) {
                size2 = size;  idx2 = i;
            }
        }
        if (idx1 < idx2) {          // Keep (x,y) dimensions in the order they appear.
            xDimension = idx1;
            yDimension = idx2;
        } else {
            xDimension = idx2;
            yDimension = idx1;
        }
    }

    /**
     * If this coverage can be represented as a {@link GridCoverage2D} instance,
     * returns such instance. Otherwise returns {@code this}.
     */
    private GridCoverage specialize() {
        final GridExtent extent = gridGeometry.getExtent();
        if (extent.getSubDimension() > BIDIMENSIONAL) {
            return this;
        }
        return new GridCoverage2D(source, gridGeometry, extent, render(null), xDimension, yDimension);
    }

    /**
     * Implementation of {@link GridCoverageProcessor#resample(GridCoverage, GridGeometry)}.
     *
     * @param  source  the grid coverage to resample.
     * @param  target  the desired geometry of returned grid coverage. May be incomplete.
     * @return a grid coverage with the characteristics specified in the given grid geometry.
     * @throws IncompleteGridGeometryException if the source grid geometry is missing an information.
     * @throws TransformException if some coordinates can not be transformed to the specified target.
     */
    static GridCoverage create(final GridCoverage source, GridGeometry target, final Interpolation interpolation)
            throws FactoryException, TransformException
    {
        /*
         * Get the coordinate operation from source CRS to target CRS. It may be the identity operation,
         * or null only if there is not enough information for determining the operation. We try to take
         * envelopes in account because the operation choice may depend on the geographic area.
         */
        CoordinateOperation changeOfCRS = null;
        final GridGeometry sourceGG = source.getGridGeometry();
        if (sourceGG.isDefined(GridGeometry.ENVELOPE) && target.isDefined(GridGeometry.ENVELOPE)) {
            changeOfCRS = Envelopes.findOperation(sourceGG.getEnvelope(), target.getEnvelope());
        }
        if (changeOfCRS == null) try {
            final CoordinateReferenceSystem sourceCRS = source.getCoordinateReferenceSystem();
            final CoordinateReferenceSystem targetCRS = target.isDefined(GridGeometry.CRS) ?
                                                        target.getCoordinateReferenceSystem() : sourceCRS;
            DefaultGeographicBoundingBox areaOfInterest = null;
            if (sourceGG.isDefined(GridGeometry.ENVELOPE)) {
                areaOfInterest = new DefaultGeographicBoundingBox();
                areaOfInterest.setBounds(sourceGG.getEnvelope());
            }
            changeOfCRS = CRS.findOperation(sourceCRS, targetCRS, areaOfInterest);
        } catch (IncompleteGridGeometryException e) {
            // Happen if the source GridCoverage does not define a CRS.
            GridCoverageProcessor.recoverableException("resample", e);
        }
        /*
         * Compute the transform from source pixels to target CRS (to be completed to target pixels later).
         * The following line may throw IncompleteGridGeometryException, which is desired because if that
         * transform is missing, we can not continue (we have no way to guess it).
         */
        MathTransform toTarget = sourceGG.getGridToCRS(PixelInCell.CELL_CENTER);
        if (changeOfCRS != null) {
            toTarget = MathTransforms.concatenate(toTarget, changeOfCRS.getMathTransform());
        }
        /*
         * Compute the transform from target grid to target CRS. This transform may be unspecified,
         * in which case we need to compute a default transform trying to preserve resolution at the
         * point of interest.
         */
        MathTransform toSource = null;
        if (target.isDefined(GridGeometry.GRID_TO_CRS)) {
            toSource = target.getGridToCRS(PixelInCell.CELL_CENTER);
        } else {
            throw new UnsupportedOperationException("Automatic computation of gridToCRS not yet implemented.");
            // TODO: complete the target GridGeometry.
        }
        if (sourceGG.equals(target)) {
            return source;
        }
        /*
         * Complete the "target to source" transform.
         */
        toSource = MathTransforms.concatenate(toSource, toTarget.inverse());
        return new ResampledGridCoverage(source, target, toSource, interpolation).specialize();
    }

    /**
     * Returns a two-dimensional slice of resampled grid data as a rendered image.
     */
    @Override
    public RenderedImage render(final GridExtent sliceExtent) {
        if (sliceExtent != null) {
            throw new CannotEvaluateException("Slice extent not yet supported.");
        }
        final RenderedImage image        = source.render(null);           // TODO: compute slice.
        final GridExtent    sourceExtent = source.getGridGeometry().getExtent();
        final GridExtent    targetExtent = gridGeometry.getExtent();
        final Rectangle     bounds       = new Rectangle(toIntExact(targetExtent.getSize(xDimension)),
                                                         toIntExact(targetExtent.getSize(yDimension)));
        /*
         * `this.toSource` is a transform from source cell coordinates to target cell coordinates.
         * We need a transform from source pixel coordinates to target pixel coordinates (in images).
         * An offset may exist between cell coordinates and pixel coordinates.
         */
        final MathTransform pixelsToTransform = MathTransforms.translation(
                subtractExact(sourceExtent.getLow(xDimension), image.getMinX()),
                subtractExact(sourceExtent.getLow(yDimension), image.getMinY()));

        final MathTransform transformToPixels = MathTransforms.translation(
                negateExact(targetExtent.getLow(xDimension)),
                negateExact(targetExtent.getLow(yDimension)));

        final MathTransform toImage = MathTransforms.concatenate(pixelsToTransform, toSource, transformToPixels);
        /*
         * Get fill values from background values declared for each band, if any.
         * If no background value is declared, default is 0 for integer data or
         * NaN for floating point values.
         */
        final Number[] fillValues = new Number[ImageUtilities.getNumBands(image)];
        final List<SampleDimension> bands = getSampleDimensions();
        for (int i=Math.min(bands.size(), fillValues.length); --i >= 0;) {
            final SampleDimension band = bands.get(i);
            final Optional<Number> bg = band.getBackground();
            if (bg.isPresent()) {
                fillValues[i] = bg.get();
            }
        }
        return new ResampledImage(bounds, toImage, image, interpolation, fillValues);
    }
}
