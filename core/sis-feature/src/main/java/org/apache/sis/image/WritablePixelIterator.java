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
import org.opengis.coverage.grid.SequenceType;
import org.apache.sis.internal.feature.Resources;


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
 * <h2>Casting a {@code PixelIterator}</h2>
 * To check if a {@code PixelIterator} can be used for writing pixels, a {@code … instanceof WritablePixelIterator}
 * check is not sufficient. The {@link PixelIterator#isWritable()} method should be invoked instead.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.0
 * @module
 */
public class WritablePixelIterator extends PixelIterator implements Closeable {
    /**
     * The image where pixels will be written, or {@code null} if the image is read-only.
     * The destination image may or may not be the same instance than the source {@link #image}.
     * However the sample model, the minimal X and Y values and the tile grid must be the same.
     */
    private final WritableRenderedImage destination;

    /**
     * The current tile where pixels will be written, or {@code null} if no write operation is under way.
     * It may or may not be the same instance than {@link #currentRaster}.
     *
     * @see WritableRenderedImage#getWritableTile(int, int)
     * @see WritableRenderedImage#releaseWritableTile(int, int)
     */
    private WritableRaster destRaster;

    /**
     * Creates an iterator for the given region in the given raster.
     *
     * @param  input    the raster which contains the sample values to read.
     * @param  output   the raster where to write the sample values, or {@code null} for read-only iterator.
     * @param  subArea  the raster region where to perform the iteration, or {@code null}
     *                  for iterating over all the raster domain.
     * @param  window   size of the window to use in {@link #createWindow(TransferType)} method, or {@code null} if none.
     * @param  order    {@code null} or {@link SequenceType#LINEAR}. Other values may be added in future versions.
     */
    WritablePixelIterator(final Raster input, final WritableRaster output,
                          final Rectangle subArea, final Dimension window, final SequenceType order)
    {
        super(input, subArea, window, order);
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
     * @param  order    {@code null} or {@link SequenceType#LINEAR}. Other values may be added in future versions.
     */
    WritablePixelIterator(final RenderedImage input, final WritableRenderedImage output,
                          final Rectangle subArea, final Dimension window, final SequenceType order)
    {
        super(input, subArea, window, order);
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
     * Returns {@code true} if this iterator can write pixel values. For some implementations, being an
     * instance of {@code WritablePixelIterator} is not sufficient for being able to write pixel values.
     *
     * <p><b>Note:</b> all instances created by {@code WritablePixelIterator.create(…)} methods are
     * guaranteed to {@code true}.</p>
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
    public void setSample(final int band, final int value) {
        destRaster.setSample(x, y, band, value);
    }

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
    public void setSample(final int band, final float value) {
        destRaster.setSample(x, y, band, value);
    }

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
    public void setSample(final int band, final double value) {
        destRaster.setSample(x, y, band, value);
    }

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
    public void setPixel​(final int[] values) {
        destRaster.setPixel(x, y, values);
    }

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
    public void setPixel​(final float[] values) {
        destRaster.setPixel(x, y, values);
    }

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
    public void setPixel​(final double[] values) {
        destRaster.setPixel(x, y, values);
    }

    /**
     * Sets the data elements (not necessarily band values) of current pixel.
     * The {@code Object} argument is a relatively opaque format (it may be {@code int[]}, {@code byte[]}, <i>etc.</i>).
     * It should be the value provided by a call to {@link #getDataElements(Object)} on an image using
     * a compatible sample model.
     *
     * @param  values  the new the data elements.
     *
     * @see WritableRaster#setDataElements​(int, int, Object)
     * @see #getDataElements(Object)
     *
     * @since 1.1
     */
    public void setDataElements(final Object values) {
        destRaster.setDataElements(x, y, values);
    }

    /**
     * The current raster destination raster, or {@code null} if none.
     */
    final WritableRaster destination() {
        return destRaster;
    }

    /**
     * Invoked by {@link #fetchTile()} when iteration switch to a new tile.
     *
     * @return if the new writable tile can also be used for reading, that tile. Otherwise {@code null}.
     */
    @Override
    final Raster fetchWritableTile() {
        if (destination != null) {
            destRaster = destination.getWritableTile(tileX, tileY);
            if (destination == image) return destRaster;
        }
        return super.fetchWritableTile();
    }

    /**
     * Releases the tiles acquired by this iterator, if any.
     * This method does nothing if the iterator is read-only.
     */
    @Override
    void releaseTile() {
        super.releaseTile();
        if (destination != null && destRaster != null) {
            destRaster = null;
            destination.releaseWritableTile(tileX, tileY);
        }
    }

    /**
     * Releases any resources hold by this iterator.
     * If some pixel values have been written, the changes are committed.
     */
    @Override
    public void close() {
        releaseTile();
    }
}
