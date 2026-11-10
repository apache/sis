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
import java.awt.geom.Path2D;
import java.util.List;
import java.util.logging.Level;
import org.opengis.feature.Feature;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.operation.OperationException;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;
import org.apache.sis.style.se1.GraphicStroke;
import org.apache.sis.style.se1.Stroke;


/**
 * Creates the visual elements of a Symbology Encoding {@link Stroke} applied on lines in display space.
 * This is shared by the line and polygon symbolizers.
 *
 * @author Johann Sorel (Geomatys)
 */
final class StrokeVisuals {

    private StrokeVisuals() {
    }

    /**
     * Creates the visual elements for stroking the given lines.
     * If the stroke has a graphic stroke, graphics are repeated along the lines.
     * Otherwise the lines are stroked with a color or a graphic fill.
     *
     * @param  lines          lines in display space, not null.
     * @param  stroke         the Symbology Encoding stroke, not null.
     * @param  feature        the feature for which to evaluate expressions, may be null.
     * @param  pixelsPerUnit  number of pixels per unit of measure of the symbolizer.
     * @return the visual elements, may be empty but never null.
     */
    static List<RenderedVisual> create(List<LineString> lines, Stroke<?> stroke, Feature feature, double pixelsPerUnit) {
        if (lines.isEmpty()) {
            return List.of();
        }
        final GraphicStroke<?> graphicStroke = stroke.getGraphicStroke().orElse(null);
        if (graphicStroke != null) {
            final RenderedVisual visual = createGraphicStroke(lines, stroke, graphicStroke, feature, pixelsPerUnit);
            return (visual != null) ? List.of(visual) : List.of();
        }
        final StrokeStyle style = StrokeStyle.create(stroke, feature, pixelsPerUnit);
        if (style == null) {
            return List.of();
        }
        final RenderedShape rs = style.applyTo(new RenderedShape());
        rs.shape = toPath(lines, Path2D.WIND_NON_ZERO, false);
        return List.of(rs);
    }

    /**
     * Appends all given lines in a single Java2D path.
     * Closed lines (polygon rings) are always closed in the path,
     * so that their first vertex gets a line join instead of two line caps.
     *
     * @param  lines        lines in display space.
     * @param  windingRule  {@link Path2D#WIND_EVEN_ODD} or {@link Path2D#WIND_NON_ZERO}.
     * @param  close        whether to close all lines, for filling them as polygon rings.
     * @return the path.
     */
    static Path2D toPath(List<LineString> lines, int windingRule, boolean close) {
        final Path2D path = new Path2D.Double(windingRule);
        for (LineString line : lines) {
            path.append(Geometries.asShape(line), false);
            if (close || line.isClosed()) path.closePath();
        }
        return path;
    }

    /**
     * Offsets the given line in display space.
     * Positive offsets are on the left side of the line direction as seen on screen,
     * as specified by Symbology Encoding.
     *
     * @param  line    line in display space.
     * @param  offset  offset in pixels, positive on the left side.
     * @return the offset line, or {@code null} if it cannot be computed.
     */
    static LineString offset(LineString line, double offset) {
        if (offset == 0 || !Double.isFinite(offset)) {
            return line;
        }
        /*
         * SIS offset curves put positive distances on the left side, but for a coordinate system
         * with y axis oriented up while the display has y axis oriented down. Hence the negation.
         */
        try {
            return GeometryFactory.DEFAULT.createOffsetCurve(line, Quantities.create(-offset, Units.UNITY), null).asLine(null, null);
        } catch (OperationException | UnsupportedOperationException ex) {
            SymbolizerToScene2D.LOGGER.log(Level.FINE, "Cannot compute the perpendicular offset.", ex);
            return null;
        }
    }

    /**
     * Creates graphics repeated along the lines.
     * The first graphic is placed at the initial gap distance from the start of each line,
     * then graphics are repeated every graphic width plus gap.
     * Each graphic is rotated along the line direction, in addition of its own rotation.
     *
     * @return the repeated graphics, or {@code null} if nothing is visible.
     */
    private static RenderedVisual createGraphicStroke(List<LineString> lines, Stroke<?> stroke, GraphicStroke<?> graphicStroke,
            Feature feature, double pixelsPerUnit)
    {
        final double opacity = StrokeStyle.opacity(stroke, feature);
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
        final RenderedGraphics graphics = new RenderedGraphics(symbol.image,
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) opacity * symbol.opacity));
        final double[] position = new double[3];
        for (LineString line : lines) {
            final PolylineWalker walker = PolylineWalker.of(line);
            final double length = walker.length();
            for (double s = initialGap; s <= length && length > 0; s += step) {
                walker.position(s, position);
                graphics.placements.add(symbol.placement(position[0], position[1], position[2]));
            }
        }
        return graphics;
    }
}
