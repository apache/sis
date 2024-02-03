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
import java.util.stream.Stream;
import org.apache.sis.map.MapLayer;
import org.apache.sis.map.Presentation;
import org.apache.sis.map.SEPortrayer;
import org.apache.sis.map.service.Scene2D;
import org.apache.sis.map.service.StylePainter;
import org.apache.sis.style.Style;
import org.apache.sis.style.se1.Symbology;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class SEPainter implements StylePainter {


    @Override
    public Class<? extends Style> getStyleClass() {
        return Symbology.class;
    }

    /**
     * Render the given map using default SEPortrayer configuration.
     *
     * @param mapItem to be rendered, not null.
     * @return this portrayer
     */
    public void paint(Scene2D scene, MapLayer mapItem) {
        try (Stream<Presentation> stream = new SEPortrayer().present(scene.grid, mapItem)) {
            paint(scene, stream);
        }
    }

    /**
     * Render the given stream of Presentations.
     *
     * @param presentations to be rendered, not null.
     * @return this portrayer
     */
    public void paint(Scene2D scene, Stream<Presentation> presentations) {
        final PresentationToScene2D pts = new PresentationToScene2D(scene);
        pts.render(presentations);
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
        Stream<Presentation> stream = new SEPortrayer().present(scene.grid, layer);
        return intersects(scene, stream, mask);
    }

    /**
     * Compute visual intersection of given map using default SEPortrayer configuration.
     *
     * @param presentations to be processed, not null.
     * @param mask intersection mask, not null.
     * @return intersecting stream of presentations.
     */
    public Stream<Presentation> intersects(Scene2D scene, Stream<Presentation> presentations, Shape mask) {
        final PresentationToScene2D pts = new PresentationToScene2D(scene);
        return pts.intersects(presentations, mask);
    }
}
