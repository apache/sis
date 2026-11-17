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
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.Stream;
import org.apache.sis.coverage.grid.GridCoverage2D;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.map.MapItem;
import org.apache.sis.map.MapLayer;
import org.apache.sis.map.MapLayers;
import org.apache.sis.map.Presentation;
import org.apache.sis.style.Style;


/**
 * Produce rendered image of styled resources.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class GraphicsPortrayer {

    /**
     * All painters found on the module path or class path, indexed by the style class they support.
     */
    private static final PainterRegistry PAINTERS = new PainterRegistry(
            ServiceLoader.load(StylePainter.class, StylePainter.class.getClassLoader()));

    private Graphics2D graphics;
    private GridGeometry domain;
    private BufferedImage image;

    public GraphicsPortrayer(){}

    /**
     * Set the output image to render into.
     *
     * @param image not null
     * @return this portrayer
     */
    public GraphicsPortrayer setCanvas(BufferedImage image) {
        this.image = Objects.requireNonNull(image);
        this.graphics = image.createGraphics();
        return this;
    }

    /**
     * Set the output Graphics2D to render into.
     *
     * @param graphics not null
     * @return this portrayer
     */
    public GraphicsPortrayer setCanvas(Graphics2D graphics) {
        this.graphics = Objects.requireNonNull(graphics);
        return this;
    }

    /**
     * Set the GridGeometry which is rendered.
     *
     * @param domain not null, lower extent coordinates must be on 0.
     */
    public GraphicsPortrayer setDomain(GridGeometry domain) {
        // Implicit null check. As of Java 14, exception message is informative.
        long[] low = domain.getExtent().getLow().getCoordinateValues();
        for (long l : low) {
            Objects.checkIndex((int) l, 1);
        }

        this.domain = domain;
        return this;
    }

    /**
     *
     * @return created image, may be null
     */
    public BufferedImage getImage() {
        return image;
    }

    /**
     * Get the rendering image as a coverage.
     * @return coverage, never null
     */
    public GridCoverage2D toCoverage() {
        return new GridCoverage2D(domain, null, getImage());
    }

    /**
     * Validate parameters and create image if needed.
     */
    private Scene2D init() {
        Objects.requireNonNull(domain, "domain");       // Not an argument.
        if (graphics == null) {
            setCanvas(new BufferedImage(
                    (int) domain.getExtent().getSize(0),
                    (int) domain.getExtent().getSize(1),
                    BufferedImage.TYPE_INT_ARGB));
        }

        return new Scene2D(domain, graphics);
    }

    /**
     * Paint given map.
     *
     * An image is created if not defined.
     *
     * @param map to paint, not null
     * @throws IllegalArgumentException if canvas is not property configured
     * @throws RenderingException if a rendering procedure fails.
     */
    public synchronized GraphicsPortrayer portray(MapItem map) throws RenderingException {
        final Scene2D scene = init();
        final List<MapLayer> layers = new ArrayList<>();
        collectVisibleLayers(map, layers);
        /*
         * Consecutive layers using the same painter are given together to that painter,
         * which can then draw across layers (labels on top of all layers, rendering passes, etc.).
         */
        StylePainter painter = null;
        final List<MapLayer> group = new ArrayList<>();
        for (final MapLayer layer : layers) {
            final StylePainter p = PAINTERS.get(layer.getStyle().getClass());
            if (p != painter) {
                if (painter != null) {
                    painter.paint(scene, List.copyOf(group));
                }
                group.clear();
                painter = p;
            }
            if (p != null) {
                group.add(layer);
            }
        }
        if (painter != null) {
            painter.paint(scene, List.copyOf(group));
        }
        return this;
    }

    /**
     * Adds the visible layers having a style to the given list, in drawing order.
     *
     * @param  map     the map item to traverse.
     * @param  target  where to add the layers.
     */
    private static void collectVisibleLayers(final MapItem map, final List<MapLayer> target) {
        if (map == null || !map.isVisible()) return;
        if (map instanceof MapLayer layer) {
            if (layer.getStyle() != null) {
                target.add(layer);
            }
        } else if (map instanceof MapLayers layers) {
            for (final MapItem item : layers.getComponents()) {
                collectVisibleLayers(item, target);
            }
        }
    }

    /**
     * Present given map.
     *
     * @param map to present, not null
     * @throws IllegalArgumentException if canvas is not property configured
     * @throws RenderingException if a rendering procedure fails.
     */
    public synchronized Stream<Presentation> present(MapItem map) throws RenderingException {
        return present(init(), map);
    }

    private Stream<Presentation> present(Scene2D scene, MapItem map) throws RenderingException {
        if (map == null || !map.isVisible()) return Stream.empty();
        if (map instanceof MapLayer) {
            return present(scene, (MapLayer) map);
        } else if (map instanceof MapLayers) {
            final MapLayers layers = (MapLayers) map;
            Stream<Presentation> stream = Stream.empty();
            for (MapItem item : layers.getComponents()) {
                stream = Stream.concat(stream, present(scene, item));
            }
            return stream;
        } else {
            return Stream.empty();
        }
    }

    private Stream<Presentation> present(Scene2D scene, MapLayer layer) throws RenderingException {
        final Style style = layer.getStyle();
        if (style == null) return Stream.empty();
        final StylePainter painter = PAINTERS.get(style.getClass());
        if (painter == null) return Stream.empty();
        return painter.present(scene, layer);
    }

    /**
     * Compute visual intersection of given map.
     *
     * @param mapItem to be processed, not null.
     * @param mask intersection mask, not null.
     * @return intersecting stream of presentations.
     * @throws IllegalArgumentException if canvas is not property configured
     * @throws RenderingException if a rendering procedure fails.
     */
    public Stream<Presentation> intersects(MapItem mapItem, Shape mask) throws RenderingException {
        return intersects(init(), mapItem, mask);
    }

    private Stream<Presentation> intersects(Scene2D scene, MapItem map, Shape mask) throws RenderingException{
        Stream<Presentation> results = Stream.empty();
        if (map == null || !map.isVisible()) return results;
        if (map instanceof MapLayer) {
            results = Stream.concat(results, intersects(scene, (MapLayer) map, mask));
        } else if (map instanceof MapLayers) {
            final MapLayers layers = (MapLayers) map;
            for (MapItem item : layers.getComponents()) {
                results = Stream.concat(results, intersects(scene, item, mask));
            }
        }
        return results;
    }

    private Stream<Presentation> intersects(Scene2D scene, MapLayer layer, Shape mask) throws RenderingException {
        final Style style = layer.getStyle();
        if (style == null) return Stream.empty();
        final StylePainter painter = PAINTERS.get(style.getClass());
        if (painter == null) return Stream.empty();
        return painter.intersects(scene, layer, mask);
    }

}
