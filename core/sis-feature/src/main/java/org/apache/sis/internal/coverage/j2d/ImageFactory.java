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
package org.apache.sis.internal.coverage.j2d;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import org.apache.sis.util.Static;


/**
 * Convenience methods for creating new images.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final class ImageFactory extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private ImageFactory() {
    }

    /**
     * Creates an opaque image with a gray scale color model. The image can have an arbitrary
     * number of bands, but in current implementation only one band is used.
     *
     * <p><b>Warning:</b> displaying this image is very slow, except in a few special cases.
     * It should be used only when no standard color model can be used.</p>
     *
     * @param  dataType       the color model type as one of {@code DataBuffer.TYPE_*} constants.
     * @param  width          the desired image width.
     * @param  height         the desired image height.
     * @param  numComponents  the number of components.
     * @param  visibleBand    the band to use for computing colors.
     * @param  minimum        the minimal sample value expected.
     * @param  maximum        the maximal sample value expected.
     * @return the color space for the given range of values.
     */
    public static BufferedImage createGrayScale(final int dataType, final int width, final int height,
            final int numComponents, final int visibleBand, final double minimum, final double maximum)
    {
        switch (dataType) {
            case DataBuffer.TYPE_BYTE: {
                if (numComponents == 1 && minimum <= 0 && maximum >= 0xFF) {
                    return new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
                }
                break;
            }
            case DataBuffer.TYPE_USHORT: {
                if (numComponents == 1 && minimum <= 0 && maximum >= 0xFFFF) {
                    return new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
                }
                break;
            }
        }
        final ColorModel cm = ColorModelFactory.createGrayScale(DataBuffer.TYPE_INT, 1, 0, -10, 10);
        return new BufferedImage(cm, cm.createCompatibleWritableRaster(width, height), false, null);
    }
}
