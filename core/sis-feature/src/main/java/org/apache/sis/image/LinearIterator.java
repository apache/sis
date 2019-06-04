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

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import java.awt.image.RasterFormatException;
import org.opengis.coverage.grid.SequenceType;
import org.apache.sis.internal.feature.Resources;


/**
 * Iterator for the {@link SequenceType#LINEAR} traversal order.
 * This iterator behaves as is the while image was a single tile.
 * Calls to {@link #next()} move the current position by increasing the following values, in order:
 *
 * <ol>
 *   <li>Column index in image (from left to right)</li>
 *   <li>Row index in image (from top to bottom).</li>
 * </ol>
 *
 * This class uses the {@link Raster} API for traversing the pixels of the image,
 * i.e. it does not yet provide optimization for commonly used sample models.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class LinearIterator extends DefaultIterator {
    /**
     * Creates an iterator for the given region in the given raster.
     *
     * @param  input    the raster which contains the sample values to read.
     * @param  output   the raster where to write the sample values, or {@code null} for read-only iterator.
     * @param  subArea  the raster region where to perform the iteration, or {@code null} for iterating over all the raster domain.
     * @param  window   size of the window to use in {@link #createWindow(TransferType)} method, or {@code null} if none.
     */
    LinearIterator(final Raster input, final WritableRaster output, final Rectangle subArea, final Dimension window) {
        super(input, output, subArea, window);
    }

    /**
     * Creates an iterator for the given region in the given image.
     *
     * @param  input    the image which contains the sample values to read.
     * @param  output   the image where to write the sample values, or {@code null} for read-only iterator.
     * @param  subArea  the image region where to perform the iteration, or {@code null} for iterating over all the image domain.
     * @param  window   size of the window to use in {@link #createWindow(TransferType)} method, or {@code null} if none.
     */
    LinearIterator(final RenderedImage input, final WritableRenderedImage output, final Rectangle subArea, final Dimension window) {
        super(input, output, subArea, window);
    }

    /**
     * Returns the order in which pixels are traversed.
     */
    @Override
    public SequenceType getIterationOrder() {
        return SequenceType.LINEAR;
    }

    /**
     * Moves the iterator to the next pixel on the current row, or to the next row.
     * This method behaves as if the whole image was a single tile.
     *
     * @return {@code true} if the current pixel is valid, or {@code false} if there is no more pixels.
     * @throws IllegalStateException if this iterator already reached end of iteration in a previous call
     *         to {@code next()}, and {@link #rewind()} or {@link #moveTo(int,int)} have not been invoked.
     */
    @Override
    public boolean next() {
        if (++x >= currentUpperX) {                 // Move to next column, potentially on a different tile.
            if (x < upperX) {
                close();                            // Must be invoked before `tileX` change.
                tileX++;
            } else {
                x = lowerX;                         // Beginning of next row.
                if (++y >= currentUpperY) {         // Move to next line.
                    close();                        // Must be invoked before `tileY` change.
                    if (++tileY >= tileUpperY) {
                        endOfIteration();
                        return false;
                    }
                } else if (tileX == tileLowerX) {
                    return true;                    // Beginning of next row is in the same tile.
                }
                close();                            // Must be invoked before `tileX` change.
                tileX = tileLowerX;
            }
            /*
             * At this point the (x,y) pixel coordinates have been updated and are inside the domain of validity.
             * We may need to change tile, either because we moved to the tile on the right or because we start a
             * new row (in which case we need to move to the leftmost tile). The only case where we can skip tile
             * change is when the image has only one tile width (in which case tileX == tileLowerX) and the next
             * row is still on the same tile.
             */
            if (fetchTile() > y) {
                throw new RasterFormatException(Resources.format(Resources.Keys.IncompatibleTile_2, tileX, tileY));
            }
        }
        return true;
    }
}
