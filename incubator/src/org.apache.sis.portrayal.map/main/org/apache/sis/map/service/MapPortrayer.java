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
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.map.MapItem;
import org.apache.sis.map.MapLayer;
import org.apache.sis.map.MapLayers;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.style.Style;
import org.apache.sis.util.ArgumentChecks;

/**
 * Draft.
 * Class used to render a map using Java2D API.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class MapPortrayer {

    /**
     * Reference all painters.
     */
    private static final Map<Class<? extends Style>,StylePainter> PAINTERS = new HashMap<>();

    private GridGeometry grid = new GridGeometry(new GridExtent(360, 180), CRS.getDomainOfValidity(CommonCRS.WGS84.normalizedGeographic()), GridOrientation.REFLECTION_Y);
    private BufferedImage image = new BufferedImage((int)grid.getExtent().getSize(0), (int)grid.getExtent().getSize(0), BufferedImage.TYPE_INT_ARGB);
    private Graphics2D graphics = image.createGraphics();
    private boolean valid = false;

    static {
        final ServiceLoader<StylePainter> loader = ServiceLoader.load(StylePainter.class, StylePainter.class.getClassLoader());
        for (StylePainter painter : loader) {
            PAINTERS.put(painter.getStyleClass(), painter);
        }
    }

    public MapPortrayer() {
    }

    /**
     * Update canvas.
     *
     * @param image image to write into, if null the graphics must be defined
     * @param graphics graphics to paint into, if null the image must be defined
     */
    public synchronized void setCanvas(BufferedImage image, Graphics2D graphics) {
        this.image = image;
        if (graphics == null) {
            graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
        this.graphics = graphics;
        valid = false;
    }

    /**
     * Update canvas area.
     *
     * @param grid GridGeometry of the rendered area
     */
    public synchronized void setGridGeometry(GridGeometry grid) {
        ArgumentChecks.ensureNonNull("grid", grid);
        this.grid = grid;
        valid = false;
    }

    /**
     * @return currently used Graphics2D, can be null
     */
    public synchronized Graphics2D getGraphics() {
        return graphics;
    }

    /**
     * @return currently used Image, can be null
     */
    public BufferedImage getImage() {
        return image;
    }

    /**
     * Paint given map.
     *
     * An image is created if not defined.
     *
     * @param map to paint, not null
     * @throws PortrayalException
     * @throws IllegalArgumentException if canvas is not property configured
     */
    public synchronized void portray(MapItem map) throws PortrayalException {

        if (!valid) {
            //check canvas configuration
            if (grid == null) {
                throw new IllegalArgumentException("Grid geometry has not been initialize");
            }
            if (graphics == null) {

            }
            valid = true;
        }

        //render given map
        if (map == null || !map.isVisible()) return;
        if (map instanceof MapLayer) {
            portray((MapLayer) map);
        } else if (map instanceof MapLayers) {
            final MapLayers layers = (MapLayers) map;
            for (MapItem item : layers.getComponents()) {
                portray(item);
            }
        }
    }

    private void portray(MapLayer layer) throws PortrayalException {
        final Style style = layer.getStyle();
        if (style == null) return;
        final StylePainter painter = PAINTERS.get(style.getClass());
        if (painter == null) return;
        painter.paint(graphics, image, grid, layer);
    }


}
