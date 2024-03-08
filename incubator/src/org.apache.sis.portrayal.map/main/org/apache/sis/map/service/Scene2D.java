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
package org.apache.sis.map.service;

import java.awt.Graphics2D;
import java.util.Objects;
import java.util.logging.Logger;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.measure.Units;

/**
 * Holds the rendering properties.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Scene2D {

    public static final Logger LOGGER = Logger.getLogger("org.apache.sis.internal.renderer");

    /**
     * The rendering grid geometry.
     */
    public final GridGeometry grid;
    /**
     * Graphics to render into.
     * When modified by renderers, it must be reset accordingly.
     */
    public final Graphics2D graphics;

    /**
     * Definition from OGC SLD/SE :
     * The portrayal unit “pixel” is the default unit of measure.
     * If available, the pixel size depends on the viewer client resolution, otherwise it is equal to 0.28mm * 0.28mm (~ 90 DPI).
     *
     * In facts, all displays have there own DPI, but the common is around 96dpi (old 72dpi x 4/3).
     * This dpi is the default on windows and replicated on different tools such as Apache Batik user agents.
     *
     * TODO : should we use a transform as in GraphicsConfiguration.getNormalizingTransform() ?
     */
    private double dpi = 96;

    public Scene2D(GridGeometry grid, Graphics2D graphics) {
        this.grid = Objects.requireNonNull(grid);
        this.graphics = Objects.requireNonNull(graphics);
    }

    public Graphics2D getGraphics() {
        return graphics;
    }

    /**
     * Set current rendering DPI.
     * Default is 99.
     *
     * @param dpi new DPI
     */
    public void setDpi(double dpi) {
        this.dpi = dpi;
    }

    /**
     * @return current DPI.
     */
    public double getDpi() {
        return dpi;
    }

    /**
     * Convert given distance to pixels.
     *
     * @param distance to convert
     * @param unit distance unit, not null
     * @return distance in pixels
     */
    public double toPixels(double distance, Unit<Length> unit) {
        return unit.getConverterTo(Units.INCH).convert(distance) * dpi;
    }

}
