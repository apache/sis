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

import java.util.ArrayList;
import java.util.List;
import org.opengis.feature.Feature;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.Point;
import org.apache.sis.map.SEPresentation;
import org.apache.sis.map.service.Scene2D;
import org.apache.sis.map.service.RenderingException;
import org.apache.sis.map.service.common.DrawingComposite;
import org.apache.sis.map.service.common.Drawing;
import org.apache.sis.map.service.common.DrawingImage;
import org.apache.sis.style.se1.Graphic;
import org.apache.sis.style.se1.PointSymbolizer;


/**
 * Support for PointSymbolizer rendering.
 *
 * <p>All the {@link PointSymbolizer} parameters are supported:</p>
 * <ul>
 *   <li>Geometry: the geometry expression is evaluated on the feature. JTS geometries are viewed as SIS geometries.
 *       A graphic is drawn on each point of points and multi-points. Other geometries get a single graphic
 *       on their representative point: a point on the surface for polygons, the middle for curves.</li>
 *   <li>Unit of measure: pixel or ground linear units (metre, foot, …), applied on the graphic size,
 *       the mark stroke and the displacement.</li>
 *   <li>Graphic: marks and external graphics, opacity, size, rotation (clockwise), anchor point
 *       and displacement (in display axes, positive Y upward, applied after the rotation).</li>
 * </ul>
 *
 * <p>Supported graphics are listed in {@link GraphicRenderer}.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class PointToScene2D extends SymbolizerToScene2D<PointSymbolizer<?>> {

    PointToScene2D(Scene2D state, PointSymbolizer<?> symbolizer) {
        super(state, symbolizer);
    }

    /**
     * Returns the drawing of the given presentation.
     *
     * @param  presentation  the presentation to draw.
     * @return the drawing, possibly empty.
     * @throws RenderingException if the drawing cannot be computed.
     */
    @Override
    protected Drawing drawing(SEPresentation presentation) throws RenderingException {
        return new DrawingComposite(createDrawings(presentation));
    }

    /**
     * Creates the graphics for the given presentation.
     *
     * @return the graphics placed on all anchor points, or an empty list if nothing is visible.
     */
    private List<DrawingImage> createDrawings(SEPresentation presentation) throws RenderingException {
        final Feature feature = presentation.getCandidate();
        final Geometry geometry = GeometryResolver.resolve(evaluate(feature, symbolizer.getGeometry()));
        final Graphic<?> graphic = symbolizer.getGraphic();
        if (geometry == null || graphic == null) {
            return List.of();
        }
        final List<Point> points = new ArrayList<>();
        GeometryResolver.anchorPoints(geometry, points);
        final double pixelsPerUnit = state.getPixelsPerUnit(symbolizer.getUnitOfMeasure());
        final GraphicRenderer.Symbol symbol = points.isEmpty() ? null : GraphicRenderer.render(graphic, feature, pixelsPerUnit);
        if (symbol == null) {
            return List.of();
        }
        final DrawingImage graphics = new DrawingImage(symbol.image, symbol.opacity);
        for (Point point : points) {
            final double[] xy = GeometryResolver.toDisplay(point, state.getDataToDisplay(point.getCoordinateReferenceSystem()));
            if (xy != null) {
                graphics.add(symbol.placement(xy[0], xy[1]));
            }
        }
        return List.of(graphics);
    }
}
