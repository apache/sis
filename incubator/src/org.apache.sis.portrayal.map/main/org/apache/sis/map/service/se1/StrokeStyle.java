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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.geom.Path2D;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.map.service.common.Drawing;
import org.apache.sis.map.service.common.GraphicPlacement;
import org.apache.sis.map.service.common.DrawingImage;
import org.apache.sis.map.service.common.LineWalker;
import org.apache.sis.map.service.common.DrawingShape;
import org.apache.sis.map.service.common.StrokeBuilder;
import org.apache.sis.style.se1.Fill;
import org.apache.sis.style.se1.GraphicFill;
import org.apache.sis.style.se1.GraphicStroke;
import org.apache.sis.style.se1.Stroke;


/**
 * Java2D equivalent of a Symbology Encoding {@link Fill} or {@link Stroke}, evaluated for one feature:
 * a paint with its opacity, and the Java2D stroke for a SE stroke.
 * All distances (width, dashes, dash offset, gaps) are converted to pixels.
 * This is shared by the point, line, polygon and text symbolizers.
 *
 * <ul>
 *   <li>The paint is the texture of the graphic fill if there is one, or the color otherwise.</li>
 *   <li>A fill or stroke is invisible if its opacity is zero, its color is transparent or its graphic fill draws nothing.</li>
 *   <li>The line join and cap default to bevel and square, as the Apache SIS style factory.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 */
final class StrokeStyle {

    /**
     * Java2D stroke, or {@code null} for a fill.
     */
    final BasicStroke stroke;

    /**
     * Paint of the fill or stroke: a color or a texture if the SE fill or stroke has a graphic fill. Never null.
     */
    final Paint paint;

    /**
     * Opacity of the fill or stroke, between 0 and 1.
     */
    final float opacity;

    private StrokeStyle(BasicStroke stroke, Paint paint, double opacity) {
        this.stroke    = stroke;
        this.paint     = paint;
        this.opacity   = (float) opacity;
    }

    /**
     * Evaluates the given SE fill for the given feature.
     *
     * @param  fill           the Symbology Encoding fill, not null.
     * @param  feature        the feature for which to evaluate expressions, may be null.
     * @param  pixelsPerUnit  number of pixels per unit of measure of the symbolizer.
     * @return the fill, or {@code null} if the fill is invisible.
     */
    static StrokeStyle create(Fill<?> fill, Feature feature, double pixelsPerUnit) {
        final double opacity = clamp(SymbolizerToScene2D.evaluateNumber(feature, fill.getOpacity(), 1.0));
        return create(fill.getGraphicFill().orElse(null), fill.getColor(), Color.GRAY, opacity, null, feature, pixelsPerUnit);
    }

    /**
     * Evaluates the given SE stroke for the given feature.
     * The graphic stroke, if any, is ignored: this method uses the color or the graphic fill.
     *
     * @param  se             the Symbology Encoding stroke, not null.
     * @param  feature        the feature for which to evaluate expressions, may be null.
     * @param  pixelsPerUnit  number of pixels per unit of measure of the symbolizer.
     * @return the stroke, or {@code null} if the stroke is invisible.
     */
    static StrokeStyle create(Stroke<?> se, Feature feature, double pixelsPerUnit) {
        final double width = SymbolizerToScene2D.evaluateNumber(feature, se.getWidth(), 1.0) * pixelsPerUnit;
        if (!(width > 0) || Double.isInfinite(width)) {
            return null;
        }
        final StrokeBuilder builder = new StrokeBuilder().setWidth(width)
                .setCap (toCap (SymbolizerToScene2D.evaluateString(feature, se.getLineCap(),  "square")))
                .setJoin(toJoin(SymbolizerToScene2D.evaluateString(feature, se.getLineJoin(), "bevel")));
        final double[] dashes = toDashes(SymbolizerToScene2D.evaluate(feature, se.getDashArray().orElse(null)), pixelsPerUnit);
        if (dashes != null) {
            builder.setDashes(dashes, SymbolizerToScene2D.evaluateNumber(feature, se.getDashOffset(), 0.0) * pixelsPerUnit);
        }
        return create(se.getGraphicFill().orElse(null), se.getColor(), Color.BLACK, opacity(se, feature),
                      builder.build(), feature, pixelsPerUnit);
    }

    /**
     * Evaluates the paint of a fill or stroke: the texture of the graphic fill if any, or the color otherwise.
     *
     * @param  graphicFill    the graphic fill, or {@code null} for using the color.
     * @param  color          the color expression, used if there is no graphic fill.
     * @param  absent         the color if the expression is null or does not give a color.
     * @param  opacity        the opacity of the fill or stroke, between 0 and 1.
     * @param  stroke         the Java2D stroke, or {@code null} for a fill.
     * @param  feature        the feature for which to evaluate expressions, may be null.
     * @param  pixelsPerUnit  number of pixels per unit of measure of the symbolizer.
     * @return the fill or stroke, or {@code null} if invisible.
     */
    private static StrokeStyle create(GraphicFill<?> graphicFill, Expression<?,?> color, Color absent, double opacity,
            BasicStroke stroke, Feature feature, double pixelsPerUnit)
    {
        if (!(opacity > 0)) {
            return null;
        }
        if (graphicFill != null) {
            final GraphicRenderer.Symbol symbol = GraphicRenderer.render(graphicFill.getGraphic(), feature, pixelsPerUnit);
            return (symbol != null) ? new StrokeStyle(stroke, symbol.texture(), opacity * symbol.opacity) : null;
        }
        final Color c = SymbolizerToScene2D.evaluateColor(feature, color, absent);
        return (c.getAlpha() != 0) ? new StrokeStyle(stroke, c, opacity) : null;
    }

    /**
     * Sets this fill or stroke on the given shape.
     *
     * @param  target  the shape to fill or stroke with this style.
     * @return the given shape.
     */
    DrawingShape applyTo(final DrawingShape target) {
        return (stroke != null) ? target.setStroke(stroke, paint, opacity) : target.setFill(paint, opacity);
    }

    /**
     * Creates the drawings for stroking the given lines.
     * If the stroke has a graphic stroke, graphics are repeated along the lines.
     * Otherwise the lines are stroked with a color or a graphic fill.
     *
     * @param  lines          lines in display space, not null.
     * @param  se             the Symbology Encoding stroke, not null.
     * @param  feature        the feature for which to evaluate expressions, may be null.
     * @param  pixelsPerUnit  number of pixels per unit of measure of the symbolizer.
     * @return the drawings, may be empty but never null.
     */
    static List<Drawing> drawings(List<LineString> lines, Stroke<?> se, Feature feature, double pixelsPerUnit) {
        if (lines.isEmpty()) {
            return List.of();
        }
        final GraphicStroke<?> graphicStroke = se.getGraphicStroke().orElse(null);
        if (graphicStroke != null) {
            final Drawing drawing = graphicStroke(lines, se, graphicStroke, feature, pixelsPerUnit);
            return (drawing != null) ? List.of(drawing) : List.of();
        }
        final StrokeStyle style = create(se, feature, pixelsPerUnit);
        if (style == null) {
            return List.of();
        }
        return List.of(style.applyTo(new DrawingShape(GeometryResolver.toPath(lines, Path2D.WIND_NON_ZERO, false))));
    }

    /**
     * Creates graphics repeated along the lines.
     * The first graphic is placed at the initial gap distance from the start of each line,
     * then graphics are repeated every graphic width plus gap.
     * Each graphic is rotated along the line direction, in addition of its own rotation.
     *
     * @return the repeated graphics, or {@code null} if nothing is visible.
     */
    private static Drawing graphicStroke(List<LineString> lines, Stroke<?> se, GraphicStroke<?> graphicStroke,
            Feature feature, double pixelsPerUnit)
    {
        final double opacity = opacity(se, feature);
        final GraphicRenderer.Symbol symbol = GraphicRenderer.render(graphicStroke.getGraphic(), feature, pixelsPerUnit);
        if (!(opacity > 0) || symbol == null) {
            return null;
        }
        double initialGap = SymbolizerToScene2D.evaluateNumber(feature, graphicStroke.getInitialGap(), 0.0) * pixelsPerUnit;
        double gap        = SymbolizerToScene2D.evaluateNumber(feature, graphicStroke.getGap(),        0.0) * pixelsPerUnit;
        if (!(initialGap >= 0) || Double.isInfinite(initialGap)) initialGap = 0;
        if (!(gap        >= 0) || Double.isInfinite(gap))        gap        = 0;
        // At least one pixel between graphics, for avoiding an excessive number of placements.
        final double step = Math.max(1, symbol.width + gap);
        final DrawingImage graphics = new DrawingImage(symbol.image, (float) opacity * symbol.opacity);
        for (LineString line : lines) {
            for (GraphicPlacement p : new LineWalker(line).walk(initialGap, step)) {
                graphics.add(symbol.placement(p.x(), p.y(), p.rotation()));
            }
        }
        return graphics;
    }

    /**
     * Evaluates the opacity of the given SE stroke, clamped to the [0 … 1] range.
     */
    static double opacity(Stroke<?> se, Feature feature) {
        return clamp(SymbolizerToScene2D.evaluateNumber(feature, se.getOpacity(), 1.0));
    }

    /**
     * Clamps the given opacity to the [0 … 1] range. NaN values are replaced by 1.
     */
    static double clamp(double opacity) {
        if (Double.isNaN(opacity)) return 1;
        return Math.max(0, Math.min(1, opacity));
    }

    /**
     * Converts a SE line join name to the Java2D constant.
     * SE leaves the default system-dependent, this method uses bevel as the Apache SIS style factory.
     */
    static int toJoin(String name) {
        switch (name.trim().toLowerCase(Locale.ROOT)) {
            case "mitre" :
            case "miter" : return BasicStroke.JOIN_MITER;
            case "round" : return BasicStroke.JOIN_ROUND;
            default      : return BasicStroke.JOIN_BEVEL;
        }
    }

    /**
     * Converts a SE line cap name to the Java2D constant.
     * SE leaves the default system-dependent, this method uses square as the Apache SIS style factory.
     */
    static int toCap(String name) {
        switch (name.trim().toLowerCase(Locale.ROOT)) {
            case "butt"  : return BasicStroke.CAP_BUTT;
            case "round" : return BasicStroke.CAP_ROUND;
            default      : return BasicStroke.CAP_SQUARE;
        }
    }

    /**
     * Converts a SE dash array value to dash lengths in pixels.
     * The value can be an array of numbers, a collection of numbers,
     * or a string of numbers separated by spaces or commas.
     * The lengths are validated by the {@link StrokeBuilder}.
     *
     * @return dash lengths in pixels, or {@code null} if there is no dash array or it cannot be parsed.
     */
    static double[] toDashes(Object value, double pixelsPerUnit) {
        final double[] values;
        if (value == null) {
            return null;
        } else if (value instanceof float[] array) {
            values = new double[array.length];
            for (int i = 0; i < array.length; i++) values[i] = array[i];
        } else if (value instanceof double[] array) {
            values = array;
        } else if (value instanceof int[] array) {
            values = new double[array.length];
            for (int i = 0; i < array.length; i++) values[i] = array[i];
        } else if (value instanceof Number[] array) {
            values = new double[array.length];
            for (int i = 0; i < array.length; i++) values[i] = array[i].doubleValue();
        } else if (value instanceof Collection<?> col) {
            values = new double[col.size()];
            int i = 0;
            for (Object v : col) {
                if (!(v instanceof Number n)) return null;
                values[i++] = n.doubleValue();
            }
        } else {
            final String str = value.toString().trim();
            if (str.isEmpty()) return null;
            final String[] parts = str.split("[\\s,]+");
            values = new double[parts.length];
            try {
                for (int i = 0; i < parts.length; i++) values[i] = Double.parseDouble(parts[i]);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        if (values.length == 0) return null;
        final double[] dashes = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            dashes[i] = values[i] * pixelsPerUnit;
        }
        return dashes;
    }
}
