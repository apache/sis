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

import java.io.Closeable;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import org.apache.sis.internal.raster.Resources;


/**
 * A pixel iterator capable to write sample values. This iterator can edit pixel values in place,
 * or write values in a different destination image than the source image. Source and destination
 * images must use the same sample model and the same coordinates (both for pixels and tiles).
 *
 * <p>Contrarily to {@code PixelIterator}, {@code WritablePixelIterator} needs to be closed after
 * iteration in order to release tiles. Example:</p>
 *
 * {@preformat java
 *     try (WritablePixelIterator it = WritablePixelIterator.create(image)) {
 *         double[] samples = null;
 *         while (it.next()) {
 *             samples = it.getPixel(samples);      // Get values in all bands.
 *             // Perform computation here...
 *             it.setPixels(sample);                // Replace values in all bands.
 *         }
 *     }
 * }
 *
 * <div class="section">Casting a {@code PixelIterator}</div>
 * To check if a {@code PixelIterator} can be used for writing pixels, a {@code … instanceof WritablePixelIterator}
 * check is not sufficient. The {@link PixelIterator#isWritable()} method should be invoked instead.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public abstract class WritablePixelIterator extends PixelIterator implements Closeable {
    /**
     * The image where pixels will be written, or {@code null} if the image is read-only.
     * The destination image may or may not be the same instance than the source {@link #image}.
     * However the sample model, the minimal X and Y values and the tile grid must be the same.
     */
    final WritableRenderedImage destination;

    /**
     * The current tile where pixels will be written, or {@code null} if no write operation is under way.
     * It may or may not be the same instance than {@link #currentRaster}.
     *
     * @see WritableRenderedImage#getWritableTile(int, int)
     * @see WritableRenderedImage#releaseWritableTile(int, int)
     */
    WritableRaster destRaster;

    /**
     * Creates an iterator for the given region in the given raster.
     *
     * @param  input    the raster which contains the sample values to read.
     * @param  output   the raster where to write the sample values, or {@code null} for read-only iterator.
     * @param  subArea  the raster region where to perform the iteration, or {@code null}
     *                  for iterating over all the raster domain.
     * @param  window   size of the window to use in {@link #createWindow(TransferType)} method, or {@code null} if none.
     */
    WritablePixelIterator(final Raster input, final WritableRaster output,
                          final Rectangle subArea, final Dimension window)
    {
        super(input, subArea, window);
        destRaster  = output;
        destination = null;
        if (output != null) {
            if (!input.getSampleModel().equals(output.getSampleModel())) {
                throw new IllegalArgumentException(Resources.format(Resources.Keys.MismatchedSampleModel));
            } else if (!input.getBounds().equals(output.getBounds())) {
                throw new IllegalArgumentException(Resources.format(Resources.Keys.MismatchedImageLocation));
            }
        }
    }

    /**
     * Creates an iterator for the given region in the given image.
     *
     * @param  input    the image which contains the sample values to read.
     * @param  output   the image where to write the sample values, or {@code null} for read-only iterator.
     * @param  subArea  the image region where to perform the iteration, or {@code null}
     *                  for iterating over all the image domain.
     * @param  window   size of the window to use in {@link #createWindow(TransferType)} method, or {@code null} if none.
     */
    WritablePixelIterator(final RenderedImage input, final WritableRenderedImage output,
                          final Rectangle subArea, final Dimension window)
    {
        super(input, subArea, window);
        destRaster  = null;
        destination = output;
        if (output != null) {
            if (!input.getSampleModel().equals(output.getSampleModel())) {
                throw new IllegalArgumentException(Resources.format(Resources.Keys.MismatchedSampleModel));
            } else if (input.getMinX()   != output.getMinX()  ||
                       input.getMinY()   != output.getMinY()  ||
                       input.getWidth()  != output.getWidth() ||
                       input.getHeight() != output.getHeight())
            {
                throw new IllegalArgumentException(Resources.format(Resources.Keys.MismatchedImageLocation));
            } else if (input.getMinTileX()   != output.getMinTileX()  ||
                       input.getMinTileY()   != output.getMinTileY()  ||
                       input.getTileWidth()  != output.getTileWidth() ||
                       input.getTileHeight() != output.getTileHeight())
            {
                throw new IllegalArgumentException(Resources.format(Resources.Keys.MismatchedTileGrid));
            }
        }
    }

    /**
     * Creates an iterator for all pixels in the given image.
     * This is a convenience method for {@code new Builder().createWritable(data)}.
     *
     * @param  data  the image which contains the sample values on which to iterate.
     * @return a new iterator traversing all pixels in the given image, in arbitrary order.
     */
    public static WritablePixelIterator create(WritableRenderedImage data) {
        return new Builder().createWritable(data);
    }

    /**
     * Returns {@code true} if this iterator can write pixel values.
     * This method should be used instead than {@code instanceof} check because, for some implementations, being an
     * instance of {@code WritablePixelIterator} is not a sufficient condition. However this method is guaranteed to
     * return {@code true} for any iterator created by {@code WritablePixelIterator.create(…)} methods.
     *
     * @return {@code true} if this iterator can be used for writing pixel values.
     */
    @Override
    public boolean isWritable() {
        return (destination != null) || (destRaster != null);
    }

    /**
     * Writes a sample value in the specified band of current pixel.
     * The {@link #next()} method must have returned {@code true}, or the {@link #moveTo(int,int)} method must have
     * been invoked successfully, before this {@code setSample(int, int)} method is invoked. If above condition is
     * not met, then this method behavior is undefined (there is no explicit bounds check for performance reasons).
     *
     * @param  band   the band in which to set the sample value.
     * @param  value  the sample value to write in the specified band.
     *
     * @see WritableRaster#setSample(int, int, int, int)
     * @see #getSample(int)
     */
    public abstract void setSample(int band, int value);

    /**
     * Writes a sample value in the specified band of current pixel.
     * The {@link #next()} method must have returned {@code true}, or the {@link #moveTo(int,int)} method must have
     * been invoked successfully, before this {@code setSample(int, float)} method is invoked. If above condition is
     * not met, then this method behavior is undefined (there is no explicit bounds check for performance reasons).
     *
     * @param  band   the band in which to set the sample value.
     * @param  value  the sample value to write in the specified band.
     *
     * @see WritableRaster#setSample(int, int, int, float)
     * @see #getSampleFloat(int)
     */
    public abstract void setSample(int band, float value);

    /**
     * Writes a sample value in the specified band of current pixel.
     * The {@link #next()} method must have returned {@code true}, or the {@link #moveTo(int,int)} method must have
     * been invoked successfully, before this {@code setSample(int, double)} method is invoked. If above condition is
     * not met, then this method behavior is undefined (there is no explicit bounds check for performance reasons).
     *
     * @param  band   the band in which to set the sample value.
     * @param  value  the sample value to write in the specified band.
     *
     * @see WritableRaster#setSample(int, int, int, double)
     * @see #getSampleDouble(int)
     */
    public abstract void setSample(int band, double value);

    /**
     * Sets the sample values of current pixel for all bands.
     * The {@link #next()} method must have returned {@code true}, or the {@link #moveTo(int,int)} method must have
     * been invoked successfully, before this {@code setPixel(…)} method is invoked. If above condition is not met,
     * then this method behavior is undefined (there is no explicit bounds check for performance reasons).
     *
     * @param  values  the new sample values for current pixel.
     *
     * @see WritableRaster#setPixel(int, int, int[])
     * @see #getPixel(int[])
     */
    public abstract void setPixel​(int[] values);

    /**
     * Sets the sample values of current pixel for all bands.
     * The {@link #next()} method must have returned {@code true}, or the {@link #moveTo(int,int)} method must have
     * been invoked successfully, before this {@code setPixel(…)} method is invoked. If above condition is not met,
     * then this method behavior is undefined (there is no explicit bounds check for performance reasons).
     *
     * @param  values  the new sample values for current pixel.
     *
     * @see WritableRaster#setPixel(int, int, float[])
     * @see #getPixel(float[])
     */
    public abstract void setPixel​(float[] values);

    /**
     * Sets the sample values of current pixel for all bands.
     * The {@link #next()} method must have returned {@code true}, or the {@link #moveTo(int,int)} method must have
     * been invoked successfully, before this {@code setPixel(…)} method is invoked. If above condition is not met,
     * then this method behavior is undefined (there is no explicit bounds check for performance reasons).
     *
     * @param  values  the new sample values for current pixel.
     *
     * @see WritableRaster#setPixel(int, int, double[])
     * @see #getPixel(double[])
     */
    public abstract void setPixel​(double[] values);

    /**
     * Releases any resources hold by this iterator.
     * Invoking this method may flush some tiles content to disk.
     */
    @Override
    public abstract void close();
}
