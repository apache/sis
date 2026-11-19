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
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.List;


/**
 * A shape filled with layers and stroked with a uniform paint.
 * The drawing is configured with builder methods. The fill layers are painted first, in order, then the stroke.
 * The fill can be a single paint, or layers such as hatches, dot patterns, stipples, textures and gradients.
 *
 * {@snippet lang="java" :
 *     Drawing line = new ShapeDrawing(shape).setStroke(new BasicStroke(2), Color.RED, 0.5f);
 *     }
 *
 * <ul>
 *   <li>A drawing without shape, or without fill and stroke, paints nothing.</li>
 *   <li>A single paint fill or a stroke with an opacity of zero is ignored, by painting and by hit tests.</li>
 *   <li>Each fill layer and the stroke are painted on a copy of the graphics, so they do not change its state.</li>
 *   <li>The hit tests use the filled area if there is a fill, and the stroked area if there is a stroke.</li>
 *   <li>Painting is synchronized on the graphics, because presentations may be rendered in parallel.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class DrawingShape implements Drawing {
    /**
     * The shape in display coordinates, or {@code null} if none.
     */
    private Shape shape;

    /**
     * The layers painted in the interior, in painting order. Empty for no fill.
     */
    private List<FillLayer> fillLayers;

    /**
     * The stroke of the outline, or {@code null} for no stroke.
     */
    private Stroke stroke;

    /**
     * The paint of the outline, or {@code null} for no stroke.
     */
    private Paint strokePaint;

    /**
     * Opacity of the outline, between 0 and 1.
     */
    private float strokeOpacity;

    /**
     * Creates a drawing without shape. The shape shall be {@linkplain #setShape(Shape) set} before painting.
     */
    public DrawingShape() {
        fillLayers = List.of();
    }

    /**
     * Creates a drawing of the given shape, without fill and stroke.
     *
     * @param  shape  the shape in display coordinates.
     */
    public DrawingShape(final Shape shape) {
        this.shape = shape;
        fillLayers = List.of();
    }

    /**
     * Sets the shape to draw.
     *
     * @param  shape  the shape in display coordinates, or {@code null} if none.
     * @return this drawing.
     */
    public DrawingShape setShape(final Shape shape) {
        this.shape = shape;
        return this;
    }

    /**
     * Sets the fill of the interior of the shape.
     *
     * @param  paint    the paint of the interior, or {@code null} for no fill.
     * @param  opacity  opacity of the interior, between 0 and 1.
     * @return this drawing.
     */
    public DrawingShape setFill(final Paint paint, final float opacity) {
        fillLayers = (paint != null && opacity > 0) ? List.of(new FillLayer.Paint(paint, opacity)) : List.of();
        return this;
    }

    /**
     * Sets the layers painted in the interior of the shape.
     *
     * @param  layers  the layers in painting order, empty for no fill.
     * @return this drawing.
     */
    public DrawingShape setFill(final List<? extends FillLayer> layers) {
        fillLayers = List.copyOf(layers);
        return this;
    }

    /**
     * Sets the stroke of the outline of the shape.
     *
     * @param  stroke   the stroke of the outline, or {@code null} for no stroke.
     * @param  paint    the paint of the outline, or {@code null} for no stroke.
     * @param  opacity  opacity of the outline, between 0 and 1.
     * @return this drawing.
     */
    public DrawingShape setStroke(final Stroke stroke, final Paint paint, final float opacity) {
        this.stroke   = stroke;
        strokePaint   = paint;
        strokeOpacity = opacity;
        return this;
    }

    /**
     * Returns whether this drawing has a fill.
     */
    private boolean hasFill() {
        return shape != null && !fillLayers.isEmpty();
    }

    /**
     * Returns whether this drawing has a stroke.
     */
    private boolean hasStroke() {
        return shape != null && stroke != null && strokePaint != null && strokeOpacity > 0;
    }

    /**
     * Fills then strokes the shape.
     *
     * @param  graphics  where to draw.
     */
    @Override
    public void paint(final Graphics2D graphics) {
        if (!hasFill() && !hasStroke()) {
            return;
        }
        synchronized (graphics) {
            if (hasFill()) {
                for (final FillLayer layer : fillLayers) {
                    final Graphics2D g = (Graphics2D) graphics.create();
                    try {
                        layer.fill(g, shape);
                    } finally {
                        g.dispose();
                    }
                }
            }
            if (hasStroke()) {
                final Graphics2D g = (Graphics2D) graphics.create();
                try {
                    Drawing.multiplyOpacity(g, strokeOpacity);
                    g.setPaint(strokePaint);
                    g.setStroke(stroke);
                    g.draw(shape);
                } finally {
                    g.dispose();
                }
            }
        }
    }

    /**
     * Returns whether the filled area or the stroked area intersects the given area.
     * The gaps between dashes are not part of the stroked area.
     *
     * @param  mask  the area to test, in display coordinates.
     * @return whether a painted part of the shape is inside the area.
     */
    @Override
    public boolean intersects(final Shape mask) {
        if (hasFill() && overlaps(shape, mask)) {
            return true;
        }
        return hasStroke() && overlaps(stroke.createStrokedShape(shape), mask);
    }

    /**
     * Returns whether the given painted area intersects the given mask.
     * Rectangular masks are tested directly. Other masks are tested with {@link Area}, which is accurate but slower.
     *
     * @param  area  the painted area, in display coordinates.
     * @param  mask  the area to test, in display coordinates.
     * @return whether the two areas intersect.
     */
    private boolean overlaps(final Shape area, final Shape mask) {
        if (mask instanceof Rectangle2D rectangle) {
            return area.intersects(rectangle);
        }
        if (!area.getBounds2D().intersects(mask.getBounds2D())) {
            return false;
        }
        final Area common = new Area(area);
        common.intersect(new Area(mask));
        return !common.isEmpty();
    }
}
