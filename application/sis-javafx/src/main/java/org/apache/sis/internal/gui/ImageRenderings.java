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
package org.apache.sis.internal.gui;

import java.awt.image.RenderedImage;
import org.apache.sis.image.ImageProcessor;


/**
 * Operations on images for rendering purposes. The methods defined in this class delegate
 * to methods in the rest of SIS library with some arbitrary parameter value choices.
 * We use this class as a way to centralize where those choices are made for GUI purposes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final class ImageRenderings {
    /**
     * The set of operations to use.
     *
     * @todo Creates our own instance which listen to logging messages.
     *       We need to create a logging panel first.
     */
    private static final ImageProcessor PROCESSOR = new ImageProcessor();
    static {
        PROCESSOR.setErrorAction(ImageProcessor.ErrorAction.LOG);
    }

    /**
     * Do not allow instantiation of this class.
     */
    private ImageRenderings() {
    }

    /**
     * Rescale the given image between a minimum and maximum values determined from statistics.
     * If the given image is null or can not be rescaled, then it is returned as-is.
     *
     * @param  image  the image to rescale, or {@code null}.
     * @return the rescaled image.
     */
    public static RenderedImage automaticScale(final RenderedImage image) {
        return PROCESSOR.automaticColorRamp(image, 3);
    }
}
