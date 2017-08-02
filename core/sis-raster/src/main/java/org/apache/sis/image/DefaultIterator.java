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
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.RasterFormatException;
import org.opengis.coverage.grid.SequenceType;
import org.apache.sis.internal.raster.Resources;


/**
 * Default iterator used when no specialized implementation is available.
 * This iterator uses the {@link Raster} API for traversing the pixels in each tile.
 * Calls to {@link #next()} move the current position by increasing the following values, in order:
 *
 * <ol>
 *   <li>Column index in a single tile (from left to right)</li>
 *   <li>Row index in a single tile (from top to bottom).</li>
 *   <li>Then, {@code tileX} index from left to right.</li>
 *   <li>Then, {@code tileY} index from top to bottom.</li>
 * </ol>
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 *
 * @todo Change iteration order on tiles for using Hilbert iterator.
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
     * A temporary array used by {@link #window} for transferring data.
     */
    private transient double[] transfer;

    /**
     * Creates an iterator for the given region in the given raster.
     *
     * @param  data     the raster which contains the sample values on which to iterate.
     * @param  subArea  the raster region where to perform the iteration, or {@code null}
     *                  for iterating over all the raster domain.
     * @param  window   size of the window to use in {@link #window()} method, or {@code null} if none.
     */
    DefaultIterator(final Raster data, final Rectangle subArea, final Dimension window) {
        super(data, subArea, window);
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
     * @param  window   size of the window to use in {@link #window()} method, or {@code null} if none.
     */
    DefaultIterator(final RenderedImage data, final Rectangle subArea, final Dimension window) {
        super(data, subArea, window);
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
            currentUpperY = lowerY;
        }
        currentLowerX = lowerX;
        x = lowerX - 1;                 // Set the position before first pixel.
        y = lowerY;
    }

    /**
     * Returns the order in which pixels are traversed.
     */
    @Override
    public SequenceType getIterationOrder() {
        if (image == null || (tileUpperX - tileLowerX) <=1 && (tileUpperY - tileLowerY) <= 1) {
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
     *         or {@link #moveTo(int, int)}, or after {@code next()} returned {@code false}.
     */
    @Override
    public Point getPosition() {
        final short message;
        if (x < lowerX) {
            message = Resources.Keys.IterationNotStarted;
        } else if (x >= upperX) {
            message = Resources.Keys.IterationIsFinished;
        } else {
            return new Point(x,y);
        }
        throw new IllegalStateException(Resources.format(message));
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
            throw new IndexOutOfBoundsException(Resources.format(Resources.Keys.CoordinateOutsideDomain_2, px, py));
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
        if (++x >= currentUpperX) {
            if (++y >= currentUpperY) {                     // Strict equality (==) would work, but use >= as a safety.
                if (++tileX >= tileUpperX) {                // Strict equality (==) would work, but use >= as a safety.
                    tileY = Math.incrementExact(tileY);     // 'incrementExact' because 'tileY > tileUpperY' is allowed.
                    if (tileY >= tileUpperY) {
                        /*
                         * Paranoiac safety: keep the x, y and tileX values before their maximal values
                         * in order to avoid overflow. The 'tileY' value is used for checking if next()
                         * is invoked again, in order to avoid a common misuse pattern. In principle
                         * 'tileY' needs to be compared only to 'tileUpperY', but we also compare to
                         * 'tileLowerY + 1' for handling the empty iterator case.
                         */
                        x =  currentUpperX - 1;
                        y =  currentUpperY - 1;
                        tileX = tileUpperX - 1;
                        if (tileY > Math.max(tileUpperY, tileLowerY + 1)) {
                            throw new IllegalStateException(Resources.format(Resources.Keys.IterationIsFinished));
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
     * All fields prefixed by {@code current} are updated by this method. This method also updates
     * the {@link #y} field, but caller is responsible for updating the {@link #x} field.
     */
    private void fetchTile() {
        currentRaster  = image.getTile(tileX, tileY);
        final int minX = currentRaster.getMinX();
        final int minY = currentRaster.getMinY();
        currentLowerX  = Math.max(lowerX, minX);
        y              = Math.max(lowerY, minY);
        currentUpperX  = Math.min(upperX, minX + tileWidth);
        currentUpperY  = Math.min(upperY, minY + tileHeight);
        if (currentRaster.getNumBands() != numBands) {
            throw new RasterFormatException(Resources.format(Resources.Keys.IncompatibleTile_2, tileX, tileY));
        }
    }

    /**
     * Returns the sample value in the specified band of current pixel, rounded toward zero.
     * This method assumes that {@link #next()} or {@link #moveTo(int,int)} has been invoked.
     */
    @Override
    public int getSample(final int band) {
        return currentRaster.getSample(x, y, band);
    }

    /**
     * Returns the sample value in the specified band of current pixel as a single-precision floating point number.
     * This method assumes that {@link #next()} or {@link #moveTo(int,int)} has been invoked.
     */
    @Override
    public float getSampleFloat(final int band) {
        return currentRaster.getSampleFloat(x, y, band);
    }

    /**
     * Returns the sample value in the specified band of current pixel, without precision lost.
     * This method assumes that {@link #next()} or {@link #moveTo(int,int)} has been invoked.
     */
    @Override
    public double getSampleDouble(final int band) {
        return currentRaster.getSampleDouble(x, y, band);
    }

    /**
     * Returns the sample values of current pixel for all bands.
     * This method assumes that {@link #next()} or {@link #moveTo(int,int)} has been invoked.
     */
    @Override
    public double[] getPixel​(double[] dest) {
        return currentRaster.getPixel(x, y, dest);
    }

    /**
     * Returns the sample values of current pixel for all bands.
     * This method assumes that {@link #next()} or {@link #moveTo(int,int)} has been invoked.
     */
    @Override
    public float[] getPixel​(float[] dest) {
        return currentRaster.getPixel(x, y, dest);
    }

    /**
     * Returns the sample values of current pixel for all bands.
     * This method assumes that {@link #next()} or {@link #moveTo(int,int)} has been invoked.
     */
    @Override
    public int[] getPixel​(int[] dest) {
        return currentRaster.getPixel(x, y, dest);
    }

    /**
     * Returns the sample values in a region of the window size starting at the current pixel position.
     * This method assumes that {@link #next()} or {@link #moveTo(int,int)} has been invoked.
     */
    @Override
    public double[] window() {
        if (window == null) {
            window   = new double[numBands * windowWidth * windowHeight];
            transfer = new double[window.length /*- numBands * Math.min(windowWidth, windowHeight)*/];
            // 'transfer' will always have at least one row or one column less than 'window'.
        }
        Raster  raster    = currentRaster;
        int     subEndX   = (raster.getMinX() - x) + raster.getWidth();
        int     subEndY   = (raster.getMinY() - y) + raster.getHeight();
        int     subWidth  = Math.min(windowWidth,  subEndX);
        int     subHeight = Math.min(windowHeight, subEndY);
        boolean fullWidth = (subWidth == windowWidth);
        if (fullWidth && subHeight == windowHeight) {
            /*
             * Optimization for the case where the full window is inside current raster.
             * This is the vast majority of cases, so we perform this check soon before
             * to compute more internal variables.
             */
            return raster.getPixels(x, y, subWidth, subHeight, window);
        }
        /*
         * At this point, we determined that the window is overlapping two or more tiles.
         * We will need more variables for iterating over the tiles around 'currentRaster'.
         */
        int destOffset   = 0;                       // Index in 'window' array where to copy the sample values.
        int subX         = 0;                       // Upper-left corner of a sub-window inside the window.
        int subY         = 0;
        int tileSubX     = tileX;                   // The tile where is located the (subX, subY) coordinate.
        int tileSubY     = tileY;
        final int stride = windowWidth * numBands;  // Number of samples between two rows in the 'windows' array.
        final int rewind = subEndX;
        for (;;) {
            if (subWidth > 0 && subHeight > 0) {
                final double[] data = raster.getPixels(x + subX, y + subY, subWidth, subHeight, transfer);
                if (fullWidth) {
                    final int fullLength = stride * subHeight;
                    System.arraycopy(data, 0, window, destOffset, fullLength);
                    destOffset += fullLength;
                } else {
                    final int  rowLength = numBands  * subWidth;
                    final int fullLength = rowLength * subHeight;
                    for (int srcOffset=0; srcOffset < fullLength; srcOffset += rowLength) {
                        System.arraycopy(data, srcOffset, window, destOffset, rowLength);
                        destOffset += stride;
                    }
                }
            }
            /*
             * At this point, we copied all sample values that we could obtain from the current tile.
             * Move to the next tile on current row, or if we reached the end of row move to the next row.
             */
            if (subEndX < windowWidth) {
                subX     = subEndX;
                subEndX += tileWidth;                       // Next tile on the same row.
                tileSubX++;
            } else {
                if (subEndY >= windowHeight) {
                    return window;                          // Completed last row of tiles.
                }
                subY     = subEndY;
                subEndY += tileHeight;                      // Tile on the next row.
                tileSubY++;
                tileSubX = tileX;
                subEndX  = rewind;
                subX     = 0;                               // Move x position back to the window left border.
            }
            raster     = image.getTile(tileSubX, tileSubY);
            destOffset = (subY * windowWidth + subX) * numBands;
            subWidth   = Math.min(windowWidth,  subEndX) - subX;
            subHeight  = Math.min(windowHeight, subEndY) - subY;
            fullWidth  = (subWidth == windowWidth);
        }
    }
}
