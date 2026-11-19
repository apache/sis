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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.util.Collection;
import java.util.Locale;
import org.opengis.feature.Feature;
import org.apache.sis.style.se1.Stroke;


/**
 * Java2D equivalent of a Symbology Encoding {@link Stroke}, evaluated for one feature.
 * All distances (width, dashes, dash offset) are converted to pixels.
 *
 * @author Johann Sorel (Geomatys)
 */
final class StrokeStyle {

    /**
     * Java2D stroke, never null.
     */
    final BasicStroke stroke;

    /**
     * Paint of the stroke: a color or a texture if the SE stroke has a graphic fill. Never null.
     */
    final Paint paint;

    /**
     * Composite holding the stroke opacity, never null.
     */
    final AlphaComposite composite;

    private StrokeStyle(BasicStroke stroke, Paint paint, double opacity) {
        this.stroke    = stroke;
        this.paint     = paint;
        this.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) opacity);
    }

    /**
     * Evaluates the given SE stroke for the given feature.
     * The graphic stroke, if any, is ignored: this method uses the color or the graphic fill.
     *
     * @param  se             the Symbology Encoding stroke, not null.
     * @param  feature        the feature for which to evaluate expressions, may be null.
     * @param  pixelsPerUnit  number of pixels per unit of measure of the symbolizer.
     * @return the Java2D stroke, or {@code null} if the stroke is invisible.
     */
    static StrokeStyle create(Stroke<?> se, Feature feature, double pixelsPerUnit) {
        double opacity = opacity(se, feature);
        final double width = SymbolizerToScene2D.evaluateNumber(feature, se.getWidth(), 1.0) * pixelsPerUnit;
        if (!(opacity > 0) || !(width > 0) || Double.isInfinite(width)) {
            return null;
        }
        final Paint paint;
        final var graphicFill = se.getGraphicFill().orElse(null);
        if (graphicFill != null) {
            final GraphicRenderer.Symbol symbol = GraphicRenderer.render(graphicFill.getGraphic(), feature, pixelsPerUnit);
            if (symbol == null) {
                return null;
            }
            paint = symbol.texture();
            opacity *= symbol.opacity;
        } else {
            final Color color = SymbolizerToScene2D.evaluateColor(feature, se.getColor(), Color.BLACK);
            if (color.getAlpha() == 0) {
                return null;
            }
            paint = color;
        }
        final int join = toJoin(SymbolizerToScene2D.evaluateString(feature, se.getLineJoin(), "bevel"));
        final int cap  = toCap (SymbolizerToScene2D.evaluateString(feature, se.getLineCap(),  "square"));
        final float[] dashes = toDashes(SymbolizerToScene2D.evaluate(feature, se.getDashArray().orElse(null)), pixelsPerUnit);
        final BasicStroke stroke;
        if (dashes != null) {
            double offset = SymbolizerToScene2D.evaluateNumber(feature, se.getDashOffset(), 0.0) * pixelsPerUnit;
            if (!(offset > 0) || Double.isInfinite(offset)) offset = 0;
            stroke = new BasicStroke((float) width, cap, join, 10f, dashes, (float) offset);
        } else {
            stroke = new BasicStroke((float) width, cap, join, 10f);
        }
        return new StrokeStyle(stroke, paint, opacity);
    }

    /**
     * Sets the stroke, paint and composite of the given shape to this style.
     *
     * @param  target  the shape to stroke with this style.
     * @return the given shape.
     */
    RenderedShape applyTo(final RenderedShape target) {
        target.stroke          = stroke;
        target.strokePaint     = paint;
        target.strokeComposite = composite;
        return target;
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
     * Converts a SE dash array value to Java2D dashes in pixels.
     * The value can be an array of numbers, a collection of numbers,
     * or a string of numbers separated by spaces or commas.
     *
     * @return dashes in pixels, or {@code null} if there is no valid dashes.
     */
    static float[] toDashes(Object value, double pixelsPerUnit) {
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
        final float[] dashes = new float[values.length];
        boolean nonZero = false;
        for (int i = 0; i < values.length; i++) {
            final double v = values[i] * pixelsPerUnit;
            if (!(v >= 0) || Double.isInfinite(v)) return null;
            dashes[i] = (float) v;
            nonZero |= (v > 0);
        }
        return nonZero ? dashes : null;
    }
}
