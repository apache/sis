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

import java.awt.Color;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.apache.sis.map.Presentation;
import org.apache.sis.map.SEPresentation;
import org.apache.sis.map.service.Scene2D;
import org.apache.sis.map.service.RenderingException;
import org.apache.sis.map.service.common.Drawing;
import org.apache.sis.map.service.common.LabelCandidate;
import org.apache.sis.style.se1.LineSymbolizer;
import org.apache.sis.style.se1.PointSymbolizer;
import org.apache.sis.style.se1.PolygonSymbolizer;
import org.apache.sis.style.se1.RasterSymbolizer;
import org.apache.sis.style.se1.Symbolizer;
import org.apache.sis.style.se1.TextSymbolizer;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ObjectConverters;


/**
 * Transforms a {@link Presentation} to Java2D drawings, for painting and for hit tests.
 * There is one subclass per type of Symbology Encoding symbolizer: point, line, polygon, text and raster.
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class SymbolizerToScene2D<S extends Symbolizer> {
    /**
     * The logger of the renderer, shared with {@link Scene2D}.
     */
    protected static final Logger LOGGER = Scene2D.LOGGER;
    protected final Scene2D state;
    protected final S symbolizer;

    protected SymbolizerToScene2D(Scene2D state, S symbolizer) {
        this.state = state;
        this.symbolizer = symbolizer;
    }

    /**
     * Returns the drawing of the given presentation, used for painting and for hit tests.
     *
     * @param  presentation  the presentation to draw, not null.
     * @return the drawing in display coordinates, or {@code null} if nothing is drawn.
     * @throws RenderingException if the drawing cannot be computed.
     */
    protected abstract Drawing drawing(SEPresentation presentation) throws RenderingException;

    /**
     * Returns the labels of the given presentation, at their candidate positions.
     * Labels are placed without overlaps and drawn after all other presentations.
     * The labels of a presentation do not exclude each other.
     * The default implementation returns {@code null}, meaning that the presentation is painted immediately.
     *
     * @param  presentation  the presentation to label, not null.
     * @return the labels, or {@code null} if the presentation is not a label.
     * @throws RenderingException if the labels cannot be computed.
     */
    protected List<LabelCandidate> labels(SEPresentation presentation) throws RenderingException {
        return null;
    }

    /**
     * Evaluates an expression on the given feature, without type conversion.
     *
     * @param feature evaluated feature, may be null
     * @param exp expression to evaluate, may be null
     * @return expression value, null if expression is null
     */
    @SuppressWarnings("unchecked")
    static Object evaluate(Feature feature, Expression<?,?> exp) {
        return (exp == null) ? null : ((Expression<Feature,?>) exp).apply(feature);
    }

    /**
     * Evaluates an expression as a number.
     *
     * @param feature evaluated feature, may be null
     * @param exp expression to evaluate, may be null
     * @param fallback value returned if the expression is null or the value is not convertible to a number
     * @return evaluated number
     */
    static double evaluateNumber(Feature feature, Expression<?,?> exp, double fallback) {
        final Object value = evaluate(feature, exp);
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        if (value != null) try {
            final Double n = ObjectConverters.convert(value, Double.class);
            if (n != null) return n;
        } catch (RuntimeException ex) {
            LOGGER.log(Level.FINE, ex.getMessage(), ex);
        }
        return fallback;
    }

    /**
     * Evaluates an expression as a string.
     *
     * @param feature evaluated feature, may be null
     * @param exp expression to evaluate, may be null
     * @param fallback value returned if the expression is null or returns null
     * @return evaluated string
     */
    static String evaluateString(Feature feature, Expression<?,?> exp, String fallback) {
        final Object value = evaluate(feature, exp);
        return (value == null) ? fallback : value.toString();
    }

    /**
     * Evaluates an expression as a boolean. Strings "true" and "1" are accepted.
     */
    static boolean evaluateBoolean(Feature feature, Expression<?,?> exp, boolean fallback) {
        final Object value = evaluate(feature, exp);
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.doubleValue() != 0;
        if (value != null) {
            final String s = value.toString().trim();
            if (s.equalsIgnoreCase("true") || s.equals("1")) return true;
            if (s.equalsIgnoreCase("false") || s.equals("0")) return false;
        }
        return fallback;
    }

    /**
     * Evaluates an expression as a color.
     * Strings are decoded as hexadecimal colors ({@code #RRGGBB} or {@code #AARRGGBB}).
     *
     * @param feature evaluated feature, may be null
     * @param exp expression to evaluate, may be null
     * @param fallback value returned if the expression is null or the value is not convertible to a color
     * @return evaluated color
     */
    static Color evaluateColor(Feature feature, Expression<?,?> exp, Color fallback) {
        return toColor(evaluate(feature, exp), fallback);
    }

    /**
     * Converts the given value to a color.
     * Strings are decoded as hexadecimal colors ({@code #RRGGBB} or {@code #AARRGGBB}).
     *
     * @param value the value to convert, may be null
     * @param fallback value returned if the value is null or not convertible to a color
     * @return the color
     */
    static Color toColor(final Object value, final Color fallback) {
        if (value instanceof Color c) {
            return c;
        }
        if (value instanceof CharSequence cs) {
            final String str = cs.toString().trim();
            try {
                if (str.startsWith("#") && str.length() == 9) {
                    final long argb = Long.parseLong(str.substring(1), 16);
                    return new Color((int) argb, true);
                }
                return Color.decode(str);
            } catch (NumberFormatException ex) {
                LOGGER.log(Level.FINE, ex.getMessage(), ex);
            }
        } else if (value != null) try {
            final Color c = ObjectConverters.convert(value, Color.class);
            if (c != null) return c;
        } catch (RuntimeException ex) {
            LOGGER.log(Level.FINE, ex.getMessage(), ex);
        }
        return fallback;
    }

    /**
     * Creates the renderer of the given symbolizer.
     *
     * @param  state       the scene where to draw, not null.
     * @param  symbolizer  the symbolizer to render, not null.
     * @return the renderer, or {@code null} if the type of symbolizer is not supported.
     */
    public static SymbolizerToScene2D<?> create(final Scene2D state, final Symbolizer<?> symbolizer) {
        ArgumentChecks.ensureNonNull("symbolizer", symbolizer);
        return switch (symbolizer) {
            case PointSymbolizer<?>   s -> new PointToScene2D(state, s);
            case LineSymbolizer<?>    s -> new LineToScene2D(state, s);
            case PolygonSymbolizer<?> s -> new PolygonToScene2D(state, s);
            case TextSymbolizer<?>    s -> new TextToScene2D(state, s);
            case RasterSymbolizer<?>  s -> new RasterToScene2D(state, s);
            default -> null;
        };
    }
}
