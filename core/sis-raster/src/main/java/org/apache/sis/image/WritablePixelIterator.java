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
import java.io.IOException;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RasterFormatException;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;


/**
 * A pixel iterator capable to write sample values.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
abstract class WritablePixelIterator extends PixelIterator implements Closeable {

    private final WritableRaster wRaster;

    private final WritableRenderedImage wRenderedImage;

    public WritablePixelIterator(WritableRaster raster, Rectangle subArea) {
        super(raster, subArea);
        wRaster        = raster;
        wRenderedImage = null;
    }

    public WritablePixelIterator(WritableRenderedImage renderedImage, Rectangle subArea) {
        super(renderedImage, subArea);
        wRaster        = null;
        wRenderedImage = renderedImage;
    }

    /**
     * Ensures that the given output raster has the same grid geometry than the input raster.
     */
    private static void checkCompatibility(final Raster input, final WritableRaster output) {
        final String message;
        if (!input.getSampleModel().equals(output.getSampleModel())) {
            message = "Incompatible sample model.";
        } else if (!input.getBounds().equals(output.getBounds())) {
            message = "Mismatched location.";
        } else {
            return;
        }
        throw new RasterFormatException(message);       // TODO: localize
    }

    /**
     * Ensures that the given output image has the same grid geometry than the input image.
     */
    private static void checkCompatibility(final RenderedImage input, final WritableRenderedImage output) {
        final String message;
        if (!input.getSampleModel().equals(output.getSampleModel())) {
            message = "Incompatible sample model.";
        } else if (input.getMinX()   != output.getMinX()   ||
                   input.getMinY()   != output.getMinY()   ||
                   input.getWidth()  != output.getWidth()  ||
                   input.getHeight() != output.getHeight())
        {
            message = "Mismatched location.";
        } else if (input.getMinTileX()   != output.getMinTileX()  ||
                   input.getMinTileY()   != output.getMinTileY()  ||
                   input.getTileWidth()  != output.getTileWidth() ||
                   input.getTileHeight() != output.getTileHeight())
        {
            message = "Mismatched tile grid.";
        } else {
            return;
        }
        throw new RasterFormatException(message);       // TODO: localize
    }

    /**
     * Write integer value at current iterator position.
     *
     * @param value integer to write.
     */
    public abstract void setSample(final int value);

    /**
     * Write float value at current iterator position.
     *
     * @param value float to write.
     */
    public abstract void setSampleFloat(final float value);

    /**
     * Write double value at current iterator position.
     *
     * @param value double to write.
     */
    public abstract void setSampleDouble(final double value);

    /**
     * Releases any resources hold by this iterator.
     * Invoking this method may flush some tiles content to disk.
     */
    @Override
    public abstract void close() throws IOException;
}
