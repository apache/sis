/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.sis.image;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import org.apache.sis.internal.raster.Resources;
import org.opengis.coverage.grid.SequenceType;

/**
 * Linear iterator used when common line y line iteration is requiered.
 * This iterator uses the {@link Raster} API for traversing the pixels of the image.
 * Calls to {@link #next()} move the current position by increasing the following values, in order:
 *
 * <ol>
 *   <li>Column index in image (from left to right)</li>
 *   <li>Row index in image (from top to bottom).</li>
 * </ol>
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 *
 */
public class LinearIterator extends DefaultIterator {

    public LinearIterator(Raster input, WritableRaster output, Rectangle subArea, Dimension window) {
        super(input, output, subArea, window);
    }

    public LinearIterator(final RenderedImage input, final WritableRenderedImage output, final Rectangle subArea, final Dimension window) {
        super(input, output, subArea, window);
    }

    @Override
    public SequenceType getIterationOrder() {
        return SequenceType.LINEAR;
    }
    
    @Override
    public boolean next() {

        if (++x >= upperX) {
            //move to next line
            x = lowerX;
            if (++y >= upperY) {
                x = lowerX;
                return false;
            }
        } else if (y >= upperY) {
            x = lowerX;
            //second time or more get in the next method, raise error
            throw new IllegalStateException(Resources.format(Resources.Keys.IterationIsFinished));
        }

        if (image != null) {
            final int tx = Math.floorDiv(x - tileGridXOffset, tileWidth);
            final int ty = Math.floorDiv(y - tileGridYOffset, tileHeight);
            if (tx != tileX || ty != tileY) {
                close(); // Release current writable raster, if any.
                tileX = tx;
                tileY = ty;
                int ry = y;
                fetchTile();
                y = ry; //y is changed by fetchTile method
            }
        }
        return true;
    }

}
