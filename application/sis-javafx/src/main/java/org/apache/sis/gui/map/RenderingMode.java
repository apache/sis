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
package org.apache.sis.gui.map;

import java.awt.Graphics2D;
import java.awt.image.RenderedImage;
import java.awt.geom.AffineTransform;


/**
 * Rendering strategies for {@link MapCanvasAWT}.
 * This enumeration controls whether {@link MapCanvasAWT.Renderer#paint(Graphics2D)}
 * should write directly into the {@linkplain MapCanvasAWT#image JavaFX image} buffer,
 * or write in a temporary buffer to be copied later into the JavaFX image.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public enum RenderingMode {
    /**
     * {@link MapCanvasAWT.Renderer#paint(Graphics2D) Renderer.paint(…)} is invoked in the JavaFX thread
     * and paints directly into the JavaFX image buffer. This mode blocks the JavaFX thread for the duration
     * of the paint event. This mode should be used only when that painting is known to be very fast,
     * for example a single call to {@link Graphics2D#drawRenderedImage(RenderedImage, AffineTransform)}
     * with an identity transform or a transform having only translation terms.
     */
    DIRECT,

    /**
     * {@link MapCanvasAWT.Renderer#paint(Graphics2D) Renderer.paint(…)} is invoked in a background thread
     * and paints into a temporary buffer. That temporary buffer is copied later into the JavaFX image.
     * This is the default rendering mode.
     */
    DOUBLE_BUFFERED
}
