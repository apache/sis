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
package org.apache.sis.image;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.RasterFormatException;
import org.opengis.coverage.grid.SequenceType;


/**
 * Default iterator used when no specialized implementation is available.
 * This iterator uses the {@link Raster} API for traversing the pixels in each tile.
 * Calls to {@link #next()} move the current position by increasing the following values, in order:
 *
 * <ol>
 *   <li>Column index in a single tile varies fastest (from left to right)</li>
 *   <li>Then, row index in a single tile varies from top to bottom.</li>
 *   <li>Then, {@code tileX} index from left to right.</li>
 *   <li>Then, {@code tileY} index from top to bottom.</li>
 * </ol>
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class DefaultIterator extends PixelIterator {
    /**
     * Current tile coordinate of current raster.
     */
    private int tileX, tileY;

    /**
     * Current column index in current raster.
     */
    private int x;

    /**
     * Current row index in current raster.
     */
    private int y;

    /**
     * Bounds of the region traversed by the iterator in current raster.
     * When iteration reaches the upper coordinates, the iterator needs to move to next tile.
     */
    private int currentLowerX, currentUpperX, currentUpperY;

    /**
     * Creates an iterator for the given region in the given raster.
     *
     * @param  data     the raster which contains the sample values on which to iterate.
     * @param  subArea  the raster region where to perform the iteration, or {@code null}
     *                  for iterating over all the raster domain.
     */
    DefaultIterator(final Raster data, final Rectangle subArea) {
        super(data, subArea);
        currentLowerX = lowerX;
        currentUpperX = upperX;
        currentUpperY = upperY;
        x = Math.decrementExact(lowerX);        // Set the position before first pixel.
        y = lowerY;
    }

    /**
     * Creates an iterator for the given region in the given image.
     *
     * @param  data     the image which contains the sample values on which to iterate.
     * @param  subArea  the image region where to perform the iteration, or {@code null}
     *                  for iterating over all the image domain.
     */
    DefaultIterator(final RenderedImage data, final Rectangle subArea) {
        super(data, subArea);
        tileX = Math.decrementExact(tileLowerX);
        tileY = tileLowerY;
        currentLowerX = lowerX;
        currentUpperX = lowerX;                 // Really 'lower', so the position is the tile before the first tile.
        currentUpperY = lowerY;
        x = Math.decrementExact(lowerX);        // Set the position before first pixel.
        y = lowerY;
    }

    /**
     * Restores this iterator to the same state it was after construction.
     */
    @Override
    public void rewind() {
        if (image == null) {
            tileX = 0;
            tileY = 0;
            currentUpperX = upperX;
            currentUpperY = upperY;
        } else {
            tileX = tileLowerX - 1;     // Note: no need for decrementExact(…) because already checked by constructor.
            tileY = tileLowerY;
            currentUpperX = lowerX;     // Really 'lower', so the position is the tile before the first tile.
        }
        currentLowerX = lowerX;
        currentUpperY = lowerY;
        x = lowerX - 1;                 // Set the position before first pixel.
        y = lowerY;
    }

    /**
     * Returns the order in which pixels are traversed.
     */
    @Override
    public SequenceType getIterationOrder() {
        if (image == null || image.getNumXTiles() <=1 && image.getNumYTiles() <= 1) {
            return SequenceType.LINEAR;
        } else {
            return null;            // Undefined order.
        }
    }

    /**
     * Returns the column (x) and row (y) indices of the current pixel.
     *
     * @return column and row indices of current iterator position.
     * @throws IllegalStateException if this method is invoked before the first call to {@link #next()}
     *         or after {@code next()} returned {@code false}.
     */
    @Override
    public Point getPosition() {
        final String message;
        if (x < lowerX) {
            message = "Iteration did not started.";
        } else if (x >= upperX) {
            message = "Iteration is finished.";
        } else {
            return new Point(x,y);
        }
        throw new IllegalStateException(message);       // TODO: localize
    }

    /**
     * Moves the pixel iterator to the given column (x) and row (y) indices.
     *
     * @param  px  the column index of the pixel to make current.
     * @param  py  the row index of the pixel to make current.
     * @throws IndexOutOfBoundsException if the given indices are outside the iteration domain.
     */
    @Override
    public void moveTo(final int px, final int py) {
        if (px < lowerX || px >= upperX ||  py < lowerY || py >= upperY) {
            throw new IndexOutOfBoundsException("Coordinate is outside iterator domain.");      // TODO: localize
        }
        if (image != null) {
            final int tx = Math.floorDiv(px - tileGridXOffset, tileWidth);
            final int ty = Math.floorDiv(py - tileGridYOffset, tileHeight);
            if (tx != tileX || ty != tileY) {
                tileX = tx;
                tileY = ty;
                fetchTile();
            }
        }
        x = px;
        y = py;
    }

    /**
     * Moves the iterator to the next pixel.
     *
     * @return {@code true} if the current pixel is valid, or {@code false} if there is no more pixels.
     * @throws IllegalStateException if this iterator already reached end of iteration in a previous call
     *         to {@code next()}, and {@link #rewind()} or {@link #moveTo(int,int)} have not been invoked.
     */
    @Override
    public boolean next() {
        if (++x == currentUpperX) {
            if (++y == currentUpperY) {
                if (++tileX == tileUpperX) {
                    if (Math.incrementExact(tileY) >= tileUpperY) {
                        /*
                         * Paranoiac safety: keep the x, y and tileX values before their maximal values
                         * in order to avoid overflow. The 'tileY' value is used for checking if next()
                         * is invoked again, in order to avoid a common misuse pattern.
                         */
                        x =  currentUpperX - 1;
                        y =  currentUpperY - 1;
                        tileX = tileUpperX - 1;
                        if (tileY > tileUpperY) {
                            throw new IllegalStateException("Iteration is finished.");      // TODO: localize
                        }
                        return false;
                    }
                    tileX = tileLowerX;
                }
                fetchTile();
            }
            x = currentLowerX;
        }
        return true;
    }

    /**
     * Fetches from the image a tile for the current {@link #tileX} and {@link #tileY} coordinates.
     * All fields prefixed by {@code current} are updated by this method.
     */
    private void fetchTile() {
        currentRaster  = image.getTile(tileX, tileY);
        final int minX = currentRaster.getMinX();
        final int minY = currentRaster.getMinY();
        currentLowerX  = Math.max(lowerX, minX);
        y              = Math.max(lowerY, minY);
        currentUpperX  = Math.min(upperX, minX + tileWidth);
        currentUpperY  = Math.min(upperY, minY + tileHeight);
        x = currentLowerX - 1;
        if (currentRaster.getNumBands() != numBands) {
            throw new RasterFormatException("Mismatched number of bands.");     // TODO: localize
        }
    }

    /**
     * Returns the sample value in the specified band of current pixel, without precision lost.
     */
    @Override
    public double getSample(final int band) {
        return currentRaster.getSampleDouble(x, y, band);
    }

    /**
     * Returns the sample value in the specified band of current pixel,
     * casted to a single-precision floating point number.
     */
    @Override
    public float getSampleFloat(final int band) {
        return currentRaster.getSampleFloat(x, y, band);
    }

    /**
     * Returns the sample value in the specified band of current pixel, casted to an integer.
     */
    @Override
    public int getSampleInt(final int band) {
        return currentRaster.getSample(x, y, band);
    }

    /**
     * Returns the sample values of current pixel for all bands.
     */
    @Override
    public double[] getPixel​(double[] dest) {
        return currentRaster.getPixel(x, y, dest);
    }

    /**
     * Returns the sample values of current pixel for all bands.
     */
    @Override
    public float[] getPixel​(float[] dest) {
        return currentRaster.getPixel(x, y, dest);
    }

    /**
     * Returns the sample values of current pixel for all bands.
     */
    @Override
    public int[] getPixel​(int[] dest) {
        return currentRaster.getPixel(x, y, dest);
    }

    /**
     * Returns the sample values in a region of the given size starting at the current pixel position.
     */
    @Override
    public Region region(final int width, final int height) {
        throw new UnsupportedOperationException();              // TODO
    }
}
