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
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Stream;
import org.apache.sis.map.Presentation;
import org.apache.sis.map.SEPresentation;
import org.apache.sis.map.service.Scene2D;
import org.apache.sis.map.service.RenderingException;
import org.apache.sis.map.service.common.Drawing;
import org.apache.sis.map.service.common.HitCollector;
import org.apache.sis.map.service.common.LabelCandidate;
import org.apache.sis.map.service.common.LabelPlacer;
import org.apache.sis.style.se1.Symbolizer;


/**
 * Generate a 2D scene from canvas and graphic presentations.
 *
 * @author Johann Sorel (Geomatys)
 */
final class PresentationToScene2D {

    private final Scene2D state;

    /**
     * Where to collect the labels for drawing them after all presentations, or {@code null} for drawing them immediately.
     */
    private final LabelPlacer labels;

    /**
     * Create converter from an existing scene state.
     *
     * @param state, not null
     */
    public PresentationToScene2D(Scene2D state) {
        this(state, null);
    }

    /**
     * Create converter from an existing scene state, collecting the labels instead of drawing them.
     * The caller shall {@linkplain LabelPlacer#paint paint} the labels after all presentations.
     *
     * @param state, not null
     * @param labels where to collect the labels, or {@code null} for drawing them immediately.
     */
    public PresentationToScene2D(Scene2D state, LabelPlacer labels) {
        this.state  = Objects.requireNonNull(state);
        this.labels = labels;
    }

    /**
     * Convert and add given presentation to the scene.
     * Labels are collected in the order of the presentations, if a label placer was specified.
     * Exceptions will be logged.
     *
     * @param presentations, not null, will be closed.
     */
    public void render(Stream<Presentation> presentations) {
        try {
            final List<List<LabelCandidate>> found = presentations.parallel().map((presentation) -> {
                try {
                    return render(presentation);
                } catch (Exception ex) {
                    Scene2D.LOGGER.log(Level.INFO, ex.getMessage(), ex);
                    return null;
                }
            }).filter((group) -> group != null && !group.isEmpty()).toList();
            // The list is empty if the labels are not collected.
            for (List<LabelCandidate> group : found) {
                labels.add(group);
            }
        } finally {
            presentations.close();
        }
    }

    /**
     * Paints the given presentation, or returns its labels if they are collected.
     *
     * @return the labels to place, or {@code null} if the presentation has been painted.
     */
    private List<LabelCandidate> render(Presentation presentation) throws RenderingException {
        //standard presentation types
        if (presentation instanceof SEPresentation) {
            return render((SEPresentation) presentation);
        } else {
            //unknown type
            return null;
        }
    }

    private List<LabelCandidate> render(SEPresentation presentation) throws RenderingException {
        final SymbolizerToScene2D<?> sts = getRenderer(presentation.getSymbolizer());
        if (sts != null) {
            if (labels != null) {
                final List<LabelCandidate> found = sts.labels(presentation);
                if (found != null) {
                    return found;
                }
            }
            final Drawing drawing = sts.drawing(presentation);
            if (drawing != null) {
                drawing.paint(state.graphics);
            }
        }
        return null;
    }

    /**
     * Process given presentations and retain only those whose drawings intersect the requested shape.
     * Each presentation is retained at most once.
     *
     * @param presentations the presentations to test, not null.
     * @param mask the area to test, in display coordinates.
     * @return the presentations intersecting the area.
     */
    public Stream<Presentation> intersects(Stream<Presentation> presentations, Shape mask) {
        final HitCollector collector = new HitCollector(mask);
        return presentations.parallel().filter((presentation) -> {
            try {
                return collector.add(presentation, drawing(presentation));
            } catch (RenderingException ex) {
                Scene2D.LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                return false;
            }
        });
    }

    /**
     * Returns the drawing of the given presentation, or {@code null} if none.
     */
    private Drawing drawing(Presentation presentation) throws RenderingException {
        if (presentation instanceof SEPresentation se) {
            final SymbolizerToScene2D<?> sts = getRenderer(se.getSymbolizer());
            if (sts != null) {
                return sts.drawing(se);
            }
        }
        return null;
    }

    /**
     * Returns the renderer of the given symbolizer, or logs a message and returns {@code null} if not supported.
     * Renderers are lightweight objects, so a new one is created for each presentation.
     */
    private SymbolizerToScene2D<?> getRenderer(Symbolizer<?> symbolizer) {
        final SymbolizerToScene2D<?> sts = SymbolizerToScene2D.create(state, symbolizer);
        if (sts == null) {
            Scene2D.LOGGER.log(Level.INFO, "Unknown symbolizer {0}", symbolizer.getClass().getName());
        }
        return sts;
    }
}
