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

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Collection;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.util.ArgumentChecks;
import org.opengis.coverage.CannotEvaluateException;
import org.opengis.coverage.PointOutsideCoverageException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * A {@link GridCoverage} with data stored in a {@link RenderedImage}.
 *
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
     * Image minX/MinY coordinate is expected to be located grid extent lower corner.
     *
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
            throw new IllegalArgumentException("Image width " + image.getWidth() + "does not match grid extent width "+ extent.getSize(imageAxes[0]));
        }
        if (image.getHeight()!= extent.getSize(imageAxes[1])) {
            throw new IllegalArgumentException("Image height " + image.getHeight()+ "does not match grid extent height "+ extent.getSize(imageAxes[1]));
        }

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
                //todo : current approach makes a copy of the datas, a better solution should be found
                final WritableRaster raster = image.getTile(image.getMinTileX(), image.getMinTileY()).createCompatibleWritableRaster(subWidth, subHeight);
                final WritableRaster derivate = raster.createWritableTranslatedChild(subX, subY);
                image.copyData(derivate);
                ColorModel cm = image.getColorModel();
                return new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);
            }
        }
    }

    public double[] evaluate(DirectPosition position, double[] pixel) throws CannotEvaluateException {

        try {
            position = toGridCoord(position);

            int x = 0;
            int y = 0;
            for (int i = 0, n = position.getDimension(); i < n; i++) {
                final double dv = position.getOrdinate(i);
                if (Double.isFinite(dv)) {
                    throw new PointOutsideCoverageException("Position outside coverage, axis " + i + " value " + dv);
                }

                final int v = Math.toIntExact(Math.round(dv));
                if (i == imageAxes[0]) {
                    x = v;
                } else if (i == imageAxes[1]) {
                    y = v;
                } else if (v != 0) {
                    //coverage is a slice, all other indices must be zero, otherwise we are outside coverage
                    throw new PointOutsideCoverageException("Position outside coverage, axis " + i + " value " + v);
                }
            }

            if (getBounds().contains(x,y)) {
                return image.getTile(XToTileX(x), YToTileY(y)).getPixel(x, y, pixel);
            }
            throw new PointOutsideCoverageException("");
        } catch (FactoryException | TransformException ex) {
            throw new CannotEvaluateException(ex.getMessage(), ex);
        }
    }

    /**
     * Converts the specified point to grid coordinate.
     * @param point point to transform to grid coordinate
     * @return point in grid coordinate
     * @throws org.opengis.util.FactoryException if creating transformation fails
     * @throws org.opengis.referencing.operation.TransformException if transformation fails
     */
    protected DirectPosition toGridCoord(final DirectPosition point)
            throws FactoryException, TransformException
    {
        final CoordinateReferenceSystem sourceCRS = point.getCoordinateReferenceSystem();
        final MathTransform trs;
        if (sourceCRS != null) {
            MathTransform toCrs = CRS.findOperation(sourceCRS, getCoordinateReferenceSystem(), null).getMathTransform();
            trs = MathTransforms.concatenate(toCrs, getGridGeometry().getGridToCRS(PixelInCell.CELL_CENTER).inverse());
        } else {
            trs = getGridGeometry().getGridToCRS(PixelInCell.CELL_CENTER);
        }

        return trs.transform(point, null);
    }

    /**
     * Utility method to convert image bounds as {@link java.awt.Rectangle}.
     * @return {@link java.awt.Rectangle} bounds.
     */
    private Rectangle getBounds() {
        return new Rectangle(image.getMinX(), image.getMinY(), image.getWidth(), image.getHeight());
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
