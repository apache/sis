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

import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.RenderedImage;
import org.apache.sis.internal.coverage.j2d.ColorModelFactory;


/**
 * An image with the same sample values than the wrapped image but a different color model.
 * Current implementation can only apply a gray scale. Future implementations may detect
 * the existing color model and try to preserve colors (for example by building an indexed
 * color model).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class RecoloredImage extends ImageAdapter {
    /**
     * The color model to associate with this recolored image.
     */
    private final ColorModel colors;

    /**
     * Creates a new recolored image with the given colors.
     */
    private RecoloredImage(final RenderedImage source, final ColorModel colors) {
        super(source);
        this.colors = colors;
    }

    /**
     * Wraps the given image with its colors ramp scaled between the given bounds. If the given image is
     * already using a color ramp for the given range of values, then that image is returned unchanged.
     *
     * @param  source       the image to recolor.
     * @param  visibleBand  the band to make visible.
     * @param  minimum      the sample value to display with the first color of the color ramp (black in a grayscale image).
     * @param  maximum      the sample value to display with the last color of the color ramp (white in a grayscale image).
     * @return the image with color ramp rescaled between the given bounds. May be the given image returned as-is.
     */
    static RenderedImage rescale(RenderedImage source, final int visibleBand, final double minimum, final double maximum) {
        final SampleModel sm = source.getSampleModel();
        final int dataType = sm.getDataType();
        final ColorModel colors = ColorModelFactory.createGrayScale(dataType, sm.getNumBands(), visibleBand, minimum, maximum);
        for (;;) {
            if (colors.equals(source.getColorModel())) {
                return source;
            }
            if (source instanceof RecoloredImage) {
                source = ((RecoloredImage) source).source;
            } else {
                break;
            }
        }
        return new RecoloredImage(source, colors);
    }

    /**
     * Returns the color model of this image.
     */
    @Override
    public ColorModel getColorModel() {
        return colors;
    }

    /**
     * Appends a content to show in the {@link #toString()} representation,
     * after the class name and before the string representation of the wrapped image.
     */
    @Override
    final Class<RecoloredImage> stringStart(final StringBuilder buffer) {
        buffer.append(colors.getColorSpace());
        return RecoloredImage.class;
    }
}
