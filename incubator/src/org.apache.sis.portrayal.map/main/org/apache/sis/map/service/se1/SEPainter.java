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
package org.apache.sis.map.service.se1;

import java.awt.Shape;
import java.util.List;
import java.util.stream.Stream;
import org.apache.sis.map.MapLayer;
import org.apache.sis.map.Presentation;
import org.apache.sis.map.SEPortrayer;
import org.apache.sis.map.service.Scene2D;
import org.apache.sis.map.service.StylePainter;
import org.apache.sis.map.service.common.LabelPlacer;
import org.apache.sis.style.Style;
import org.apache.sis.style.se1.Symbology;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class SEPainter implements StylePainter {
    /**
     * Margin in pixels added around a label when searching for overlaps with the labels already placed.
     */
    private static final double LABEL_PADDING = 0;

    /**
     * Creates a new painter.
     */
    public SEPainter() {
    }

    @Override
    public Class<? extends Style> getStyleClass() {
        return Symbology.class;
    }

    /**
     * Render the given map using default SEPortrayer configuration.
     *
     * @param layer to be rendered, not null.
     * @return this portrayer
     */
    @Override
    public void paint(Scene2D scene, MapLayer layer) {
        paint(scene, List.of(layer));
    }

    /**
     * Renders the given layers using default SEPortrayer configuration, with labels on top of all layers.
     * Labels which overlap a label already drawn are omitted.
     *
     * @param scene parameters for rendering.
     * @param layers layers to be rendered, in drawing order.
     */
    @Override
    public void paint(Scene2D scene, List<MapLayer> layers) {
        final LabelPlacer labels = new LabelPlacer(LABEL_PADDING);
        final PresentationToScene2D pts = new PresentationToScene2D(scene, labels);
        for (MapLayer layer : layers) {
            try (Stream<Presentation> stream = new SEPortrayer().present(scene.grid, layer)) {
                pts.render(stream);
            }
        }
        labels.paint(scene.graphics);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Stream<Presentation> present(Scene2D scene, MapLayer layer) {
        return new SEPortrayer().present(scene.grid, layer);
    }

    /**
     * Compute visual intersection of given map using default SEPortrayer configuration.
     *
     * @param layer to be processed, not null.
     * @param mask intersection mask, not null.
     * @return intersecting stream of presentations.
     */
    @Override
    public Stream<Presentation> intersects(Scene2D scene, MapLayer layer, Shape mask) {
        final Stream<Presentation> presentations = new SEPortrayer().present(scene.grid, layer);
        return new PresentationToScene2D(scene).intersects(presentations, mask);
    }
}
