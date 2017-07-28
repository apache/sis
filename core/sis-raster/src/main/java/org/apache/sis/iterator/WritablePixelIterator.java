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
package org.apache.sis.iterator;

import java.awt.Rectangle;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;

/**
 *
 * @author Remi Marechal (Geomatys).
 */
abstract class WritablePixelIterator extends PixelIterator {

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

}
