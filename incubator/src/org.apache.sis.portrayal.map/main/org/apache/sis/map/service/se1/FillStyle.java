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
import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import org.opengis.feature.Feature;
import org.apache.sis.style.se1.Fill;


/**
 * Java2D equivalent of a Symbology Encoding {@link Fill}, evaluated for one feature.
 * This is shared by the polygon and text symbolizers.
 *
 * @author Johann Sorel (Geomatys)
 */
final class FillStyle {

    private FillStyle() {
    }

    /**
     * Creates the fill of the given shape: a color or a graphic fill (tiled pattern), with an opacity.
     *
     * @param  shape          the shape to fill, in display space.
     * @param  fill           the Symbology Encoding fill, not null.
     * @param  feature        the feature for which to evaluate expressions, may be null.
     * @param  pixelsPerUnit  number of pixels per unit of measure of the symbolizer.
     * @return the filled shape, or {@code null} if the fill is invisible.
     */
    static RenderedShape create(Shape shape, Fill<?> fill, Feature feature, double pixelsPerUnit) {
        double opacity = StrokeStyle.clamp(SymbolizerToScene2D.evaluateNumber(feature, fill.getOpacity(), 1.0));
        if (!(opacity > 0)) {
            return null;
        }
        final Paint paint;
        final var graphicFill = fill.getGraphicFill().orElse(null);
        if (graphicFill != null) {
            final GraphicRenderer.Symbol symbol = GraphicRenderer.render(graphicFill.getGraphic(), feature, pixelsPerUnit);
            if (symbol == null) {
                return null;
            }
            paint = symbol.texture();
            opacity *= symbol.opacity;
        } else {
            final Color color = SymbolizerToScene2D.evaluateColor(feature, fill.getColor(), Color.GRAY);
            if (color.getAlpha() == 0) {
                return null;
            }
            paint = color;
        }
        final RenderedShape rs = new RenderedShape();
        rs.shape         = shape;
        rs.fillPaint     = paint;
        rs.fillComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) opacity);
        return rs;
    }
}
