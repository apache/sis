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
package org.apache.sis.map.service.common;

import java.awt.Graphics2D;
import java.util.List;
import java.util.Objects;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.map.MapLayer;
import org.apache.sis.map.MapLayers;
import org.apache.sis.map.service.GraphicsPortrayer;
import org.apache.sis.map.service.RenderingException;


/**
 * A validation case portraying map layers with their style, through the painters registered for the styles.
 * The layers are portrayed in order, so that layers drawing the source geometries should be last.
 *
 * @param description  short description of what is validated.
 * @param layers       the layers to portray, with their data and style.
 * @param warnings     the warnings emitted while creating the layers, for example by a style reader.
 *
 * @author Johann Sorel (Geomatys)
 */
public record PortrayalCase(String description, List<MapLayer> layers, List<String> warnings) implements ValidationCase {
    /**
     * Creates a validation case.
     *
     * @param description  short description of what is validated.
     * @param layers       the layers to portray, with their data and style.
     * @param warnings     the warnings emitted while creating the layers.
     */
    public PortrayalCase {
        Objects.requireNonNull(description, "description");
        layers   = List.copyOf(layers);
        warnings = List.copyOf(warnings);
    }

    /**
     * Portrays the layers in the given tile, and reports the warnings.
     *
     * @param  graphics  where to paint, in the display coordinates of the grid.
     * @param  grid      the rendering grid of the tile.
     * @param  problems  where to add the warnings.
     * @throws RenderingException if the rendering failed.
     */
    @Override
    public void paint(final Graphics2D graphics, final GridGeometry grid, final List<String> problems) throws RenderingException {
        problems.addAll(warnings);
        final MapLayers map = new MapLayers();
        map.getComponents().addAll(layers);
        new GraphicsPortrayer().setCanvas(graphics).setDomain(grid).portray(map);
    }
}
