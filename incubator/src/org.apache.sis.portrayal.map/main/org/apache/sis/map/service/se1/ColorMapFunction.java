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
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.filter.Literal;
import org.opengis.filter.ValueReference;
import org.apache.sis.style.se1.function.Categorize;
import org.apache.sis.style.se1.ColorMap;
import org.apache.sis.style.se1.function.Interpolate;


/**
 * Evaluation of a Symbology Encoding {@link ColorMap} function on sample values.
 * The function shall be a {@link Categorize} or {@link Interpolate} expression.
 * Thresholds, interpolation points and colors are evaluated once at construction time.
 *
 * <p>Instances of this class are not thread-safe.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
final class ColorMapFunction {

    /**
     * The ARGB value of transparent pixels.
     */
    static final int TRANSPARENT = 0;

    /**
     * The function computing colors from sample values.
     */
    private final Function<Object,?> function;

    /**
     * Cache of ARGB values for colors which are not {@link Color} instances (e.g. strings).
     */
    private final Map<Object,Integer> cache = new IdentityHashMap<>();

    private ColorMapFunction(final Function<Object,?> function) {
        this.function = function;
    }

    /**
     * Returns whether the given color map uses a function supported by this class.
     *
     * @param  colorMap  the color map, may be null.
     * @return whether the color map function is supported.
     */
    static boolean isSupported(final ColorMap<?> colorMap) {
        if (colorMap == null) return false;
        final Expression<?,?> fn = colorMap.getFunction().orElse(null);
        return (fn instanceof Categorize<?,?>) || (fn instanceof Interpolate<?>);
    }

    /**
     * Returns the lookup value of the given color map function, as a band name.
     * The {@code "Rasterdata"} lookup value is returned as-is.
     *
     * @param  colorMap  a color map with a supported function.
     * @return the band name or {@code "Rasterdata"}, or {@code null} if not determined.
     */
    static String lookupBand(final ColorMap<?> colorMap) {
        final Expression<?,?> fn = colorMap.getFunction().orElse(null);
        final Expression<?,?> lookup;
        if (fn instanceof Categorize<?,?> c) {
            lookup = c.getLookupValue();
        } else if (fn instanceof Interpolate<?> i) {
            lookup = i.getLookupValue();
        } else {
            return null;
        }
        if (lookup instanceof Literal<?,?> literal) {
            final Object value = literal.getValue();
            return (value != null) ? value.toString() : null;
        }
        if (lookup instanceof ValueReference<?,?> ref) {
            return ref.getXPath();
        }
        return null;
    }

    /**
     * Creates the function for the given color map.
     *
     * @param  colorMap  the color map, not null.
     * @param  feature   feature for evaluating expressions, may be null.
     * @return the function, or {@code null} if the color map has no supported function.
     */
    @SuppressWarnings("unchecked")
    static ColorMapFunction create(final ColorMap<?> colorMap, final Feature feature) {
        final Expression<?,?> fn = colorMap.getFunction().orElse(null);
        if (fn instanceof Categorize<?,?> c) {
            return new ColorMapFunction(((Categorize<Feature,?>) c).bind(feature));
        }
        if (fn instanceof Interpolate<?> i) {
            return new ColorMapFunction(((Interpolate<Feature>) i).bind(feature));
        }
        return null;
    }

    /**
     * Returns the ARGB color for the given sample value.
     *
     * @param  value  the sample value.
     * @return the ARGB color, transparent if the function returns no color.
     */
    int argb(final double value) {
        final Object c = function.apply(value);
        if (c instanceof Color color) {
            return color.getRGB();
        }
        if (c == null) {
            return TRANSPARENT;
        }
        return cache.computeIfAbsent(c, (v) -> {
            final Color color = SymbolizerToScene2D.toColor(v, null);
            return (color != null) ? color.getRGB() : TRANSPARENT;
        });
    }
}
