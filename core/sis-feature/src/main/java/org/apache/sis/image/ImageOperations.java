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

import java.awt.image.RenderedImage;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Static;


/**
 * Provides static methods working on images. Some of those methods create cheap <em>views</em>
 * sharing the same pixels storage than the original image, while some other methods may create
 * new tiles holding computation results. See the javadoc of each method for details.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final class ImageOperations extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private ImageOperations() {
    }

    /**
     * Returns an image with the same data than the given image but located at given coordinates.
     * The returned image is a <em>view</em>, i.e. this method does not copy any pixel.
     * Changes in the original image are reflected immediately in the returned image.
     * This method may return the given image directly if it is already located at the given position.
     *
     * @param  image  the image to move.
     * @param  minX   new <var>x</var> coordinate of upper-left pixel.
     * @param  minY   new <var>y</var> coordinate of upper-left pixel.
     * @return image with the same data but at the given coordinates.
     */
    public static RenderedImage moveTo(final RenderedImage image, final int minX, final int minY) {
        ArgumentChecks.ensureNonNull("image", image);
        if (minX == image.getMinX() && minY == image.getMinY()) {
            // Condition verified here for avoiding RelocatedImage class loading when not needed.
            return image;
        }
        return RelocatedImage.moveTo(image, minX, minY);
    }
}
