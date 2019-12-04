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
package org.apache.sis.internal.coverage;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.Collection;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.internal.image.TranslatedRenderedImage;
import org.apache.sis.internal.referencing.AxisDirections;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.transform.TransformSeparator;
import org.apache.sis.util.ArgumentChecks;
import org.opengis.coverage.CannotEvaluateException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * A {@link GridCoverage} with data stored in a {@link RenderedImage}.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @author Johann Sorel (Geomatys)
 * @version 2.0
 * @since   2.0
 * @module
 */
public final class GridCoverage2D extends GridCoverage {
    /**
     * The sample values, stored as a RenderedImage.
     */
    private final RenderedImage image;
    private final int[] imageAxes;
    private final CoordinateReferenceSystem crs2d;

    /**
     * Result of the call to {@link #forConvertedValues(boolean)}, created when first needed.
     */
    private GridCoverage converted;

    /**
     * The given RenderedImage may not start at 0,0, so does the gridExtent of the grid geometry.
     * Image 0/0 coordinate is expected to match grid extent lower corner.
     *
     * @param grid  the grid extent, CRS and conversion from cell indices to CRS.
     * @param bands sample dimensions for each image band.
     * @param image the sample values as a RenderedImage, potentially multi-banded in packed view.
     */
    public GridCoverage2D(final GridGeometry grid, final Collection<? extends SampleDimension> bands, final RenderedImage image) throws FactoryException {
        super(grid, bands);
        this.image = image;
        ArgumentChecks.ensureNonNull("image", image);

        //extract the 2D Coordinater
        GridExtent extent = grid.getExtent();
        imageAxes = extent.getSubspaceDimensions(2);
        crs2d = CRS.reduce(grid.getCoordinateReferenceSystem(), imageAxes);

        //check image is coherent with grid geometry
        if (image.getWidth() != extent.getSize(imageAxes[0])) {
            throw new IllegalArgumentException("Image width " + image.getWidth() + " does not match grid extent width "+ extent.getSize(imageAxes[0]));
        }
        if (image.getHeight() != extent.getSize(imageAxes[1])) {
            throw new IllegalArgumentException("Image height " + image.getHeight()+ " does not match grid extent height "+ extent.getSize(imageAxes[1]));
        }
        if (image.getSampleModel().getNumBands() != bands.size()) {
            throw new IllegalArgumentException("Image sample model number of bands " + image.getSampleModel().getNumBands()+ " does not match number of sample dimensions "+ bands.size());
        }
    }

    /**
     * Returns the two-dimensional part of this grid coverage CRS. If the
     * {@linkplain #getCoordinateReferenceSystem complete CRS} is two-dimensional, then this
     * method returns the same CRS. Otherwise it returns a CRS for the two first axis having
     * a {@linkplain GridExtent#getSize span} greater than 1 in the grid envelope. Note that
     * those axis are guaranteed to appears in the same order than in the complete CRS.
     *
     * @return The two-dimensional part of the grid coverage CRS.
     *
     * @see #getCoordinateReferenceSystem
     */
    public CoordinateReferenceSystem getCoordinateReferenceSystem2D() {
        return crs2d;
    }

    /**
     * Returns the grid to CRS 2D transform in pixel center.
     *
     * @return MathTransform grid to CRS 2D transform
     * @throws FactoryException if separating 2d transform fails.
     */
    public MathTransform getGridToCrs2D() throws FactoryException {
        TransformSeparator sep = new TransformSeparator(getGridGeometry().getGridToCRS(PixelInCell.CELL_CENTER));
        int idx = AxisDirections.indexOfColinear(getCoordinateReferenceSystem().getCoordinateSystem(), crs2d.getCoordinateSystem());
        sep.addSourceDimensionRange(idx, idx+2);
        return sep.separate();
    }

    /**
     * Returns a grid coverage that contains real values or sample values, depending if {@code converted} is {@code true}
     * or {@code false} respectively.
     *
     * If the given value is {@code false}, then the default implementation returns a grid coverage which produces
     * {@link RenderedImage} views. Those views convert each sample value on the fly. This is known to be very slow
     * if an entire raster needs to be processed, but this is temporary until another implementation is provided in
     * a future SIS release.
     *
     * @return a coverage containing converted or packed values, depending on {@code converted} argument value.
     */
    @Override
    public GridCoverage forConvertedValues(final boolean converted) {
        if (converted) {
            synchronized (this) {
                if (this.converted == null) {
                    this.converted = BufferedGridCoverage.convert(this);
                }
                return this.converted;
            }
        }
        return this;
    }

    /**
     * Returns a two-dimensional slice of grid data as a rendered image.
     * This method may return a view or a copy.
     *
     * @return the grid slice as a rendered image.
     */
    @Override
    public RenderedImage render(GridExtent sliceExtent) throws CannotEvaluateException {
        if (sliceExtent == null || sliceExtent.equals(getGridGeometry().getExtent())) {
            return image;
        } else {
            final int subX = Math.toIntExact(sliceExtent.getLow(imageAxes[0]));
            final int subY = Math.toIntExact(sliceExtent.getLow(imageAxes[1]));
            final int subWidth = Math.toIntExact(Math.round(sliceExtent.getSize(imageAxes[0])));
            final int subHeight = Math.toIntExact(Math.round(sliceExtent.getSize(imageAxes[1])));

            if (image instanceof BufferedImage) {
                final BufferedImage bi = (BufferedImage) image;
                return bi.getSubimage(subX, subY, subWidth, subHeight);
            } else {
                return new TranslatedRenderedImage(image, subX, subY);
            }
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public double[] evaluate(DirectPosition position, double[] buffer) throws CannotEvaluateException {
        try {
            position = toGridCoord(position);
            long[] coord = toLongExact(position);
            int x = Math.toIntExact(Math.round(coord[imageAxes[0]]));
            int y = Math.toIntExact(Math.round(coord[imageAxes[1]]));
            return image.getTile(XToTileX(x), YToTileY(y)).getPixel(x, y, buffer);
        } catch (FactoryException | TransformException ex) {
            throw new CannotEvaluateException(ex.getMessage(), ex);
        }
    }

    /**
     * Converts a pixel's X coordinate into a horizontal tile index.
     * @param x pixel x coordinate
     * @return tile x coordinate
     */
    private int XToTileX(int x) {
        int tileWidth = image.getTileWidth();
        x -= image.getTileGridXOffset();
        if (x < 0) {
            x += 1 - tileWidth;
        }
        return x/tileWidth;
    }

    /**
     * Converts a pixel's Y coordinate into a vertical tile index.
     * @param y pixel x coordinate
     * @return tile y coordinate
     */
    private int YToTileY(int y) {
        int tileHeight = image.getTileHeight();
        y -= image.getTileGridYOffset();
        if (y < 0) {
            y += 1 - tileHeight;
        }
        return y/tileHeight;
    }

}
