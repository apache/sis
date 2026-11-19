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
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.opengis.feature.Feature;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.operation.OperationException;
import org.apache.sis.map.Presentation;
import org.apache.sis.map.SEPresentation;
import org.apache.sis.map.service.Scene2D;
import org.apache.sis.map.service.RenderingException;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.style.se1.Displacement;
import org.apache.sis.style.se1.Fill;
import org.apache.sis.style.se1.PolygonSymbolizer;
import org.apache.sis.style.se1.Stroke;


/**
 * Support for PolygonSymbolizer rendering.
 *
 * <p>All the {@link PolygonSymbolizer} parameters are supported:</p>
 * <ul>
 *   <li>Geometry: the geometry expression is evaluated on the feature. JTS geometries are viewed as SIS geometries.
 *       Polygons and curve polygons are rendered with their holes. Linear geometries are closed implicitly
 *       for filling, and only the original line is stroked. Points are rendered as small squares.</li>
 *   <li>Unit of measure: pixel or ground linear units (metre, foot, …). All distances are scaled accordingly.</li>
 *   <li>Fill: color, opacity and graphic fill (tiled pattern). A null fill disables filling.</li>
 *   <li>Stroke: all stroke parameters, as for the line symbolizer, applied on all rings.
 *       A null stroke disables the outline.</li>
 *   <li>Displacement: translation of the polygon, positive Y values upward.</li>
 *   <li>Perpendicular offset: positive values make the polygon larger (exterior ring moved outward,
 *       holes shrunk), negative values make it smaller, like a buffer with mitre joins.
 *       Parts which collapse are not rendered.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class PolygonToScene2D extends SymbolizerToScene2D<PolygonSymbolizer<?>> {

    /**
     * Maximum distance in pixels between curves and their linear approximation,
     * used when converting offset polygons back to geometries.
     */
    private static final double FLATNESS = 0.25;

    private PolygonToScene2D(Scene2D state, PolygonSymbolizer<?> symbolizer) {
        super(state, symbolizer);
    }

    @Override
    public void paint(SEPresentation presentation, Consumer<Stream<Presentation>> callback) throws RenderingException {
        RenderedVisual.paint(createVisuals(presentation), state.getGraphics());
    }

    @Override
    public boolean intersects(SEPresentation presentation, Shape mask, Consumer<Stream<Presentation>> callback) throws RenderingException {
        return RenderedVisual.intersects(createVisuals(presentation), mask);
    }

    /**
     * Creates the visual elements for the given presentation: the fill first, then the stroke.
     *
     * @return visual elements, may be empty but never null.
     */
    private List<RenderedVisual> createVisuals(SEPresentation presentation) throws RenderingException {
        final Feature feature = presentation.getCandidate();
        final Geometry geometry = GeometryResolver.resolve(evaluate(feature, symbolizer.getGeometry()));
        if (geometry == null) {
            return List.of();
        }
        final Fill<?>   fill   = symbolizer.getFill().orElse(null);
        final Stroke<?> stroke = symbolizer.getStroke().orElse(null);
        if (fill == null && stroke == null) {
            return List.of();
        }
        final double pixelsPerUnit = state.getPixelsPerUnit(symbolizer.getUnitOfMeasure());
        final List<LineString> rings = toDisplay(geometry, feature, pixelsPerUnit);
        if (rings.isEmpty()) {
            return List.of();
        }
        /*
         * Perpendicular offset: the polygon is buffered by stroking its outline with a width of twice
         * the offset, then adding (positive offset) or removing (negative offset) that stroked outline.
         * This handles collapsing rings, holes and concave corners. The new outline is used for the stroke.
         */
        Shape shape = StrokeVisuals.toPath(rings, Path2D.WIND_EVEN_ODD, true);
        List<LineString> outline = rings;
        final double offset = evaluateNumber(feature, symbolizer.getPerpendicularOffset(), 0.0) * pixelsPerUnit;
        if (offset != 0 && Double.isFinite(offset)) {
            final Area area = new Area(shape);
            final Area band = new Area(new BasicStroke((float) (2 * Math.abs(offset)),
                    BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f).createStrokedShape(shape));
            if (offset > 0) {
                area.add(band);
            } else {
                area.subtract(band);
            }
            if (area.isEmpty()) {
                return List.of();
            }
            shape = area;
            outline = new ArrayList<>();
            GeometryResolver.linearComponents(Geometries.fromAWT(area, FLATNESS), outline);
        }

        final List<RenderedVisual> visuals = new ArrayList<>(2);
        if (fill != null) {
            final RenderedShape rs = FillStyle.create(shape, fill, feature, pixelsPerUnit);
            if (rs != null) {
                visuals.add(rs);
            }
        }
        if (stroke != null) {
            visuals.addAll(StrokeVisuals.create(outline, stroke, feature, pixelsPerUnit));
        }
        return visuals;
    }

    /**
     * Extracts the polygon rings of the given geometry, transforms them to display space
     * and applies the displacement.
     *
     * @return rings in display space, exterior and interior rings of all polygons.
     */
    private List<LineString> toDisplay(Geometry geometry, Feature feature, double pixelsPerUnit) throws RenderingException {
        final List<List<LineString>> components = new ArrayList<>();
        GeometryResolver.polygonComponents(geometry, components);

        final Displacement<?> displacement = symbolizer.getDisplacement();
        double dx = 0, dy = 0;
        if (displacement != null) {
            dx = evaluateNumber(feature, displacement.getDisplacementX(), 0.0) * pixelsPerUnit;
            dy = evaluateNumber(feature, displacement.getDisplacementY(), 0.0) * pixelsPerUnit;
            if (!Double.isFinite(dx)) dx = 0;
            if (!Double.isFinite(dy)) dy = 0;
        }
        final List<LineString> display = new ArrayList<>();
        for (List<LineString> rings : components) {
            for (int i = 0; i < rings.size(); i++) {
                final LineString ring = rings.get(i);
                if (ring.getNumPoints() < 2) {
                    if (i == 0) break;      // No exterior ring, skip the polygon.
                    continue;
                }
                MathTransform dataToDisplay = state.getDataToDisplay(ring.getCoordinateReferenceSystem());
                if (dx != 0 || dy != 0) {
                    // SE displacement has Y axis oriented up while display has Y axis oriented down.
                    dataToDisplay = MathTransforms.concatenate(dataToDisplay, MathTransforms.translation(dx, -dy));
                }
                try {
                    display.add(GeometryResolver.toDisplay(ring, dataToDisplay));
                } catch (OperationException ex) {
                    throw new RenderingException(ex);
                }
            }
        }
        if (components.isEmpty()) {
            /*
             * SE: if a point geometry is referenced instead of a polygon, then a small, square,
             * ortho-normal polygon should be constructed for rendering. Its size is the default mark size.
             */
            final List<Point> points = new ArrayList<>();
            GeometryResolver.anchorPoints(geometry, points);
            final double h = GraphicRenderer.DEFAULT_SIZE / 2;
            for (Point point : points) {
                final double[] p = GeometryResolver.toDisplay(point, state.getDataToDisplay(point.getCoordinateReferenceSystem()));
                if (p != null) {
                    final double x = p[0] + dx, y = p[1] - dy;
                    display.add(GeometryFactory.DEFAULT.createLinearRing(GeometryFactory.DEFAULT.createDataPoints(
                            x - h, y - h,  x + h, y - h,  x + h, y + h,  x - h, y + h,  x - h, y - h)));
                }
            }
        }
        return display;
    }

    public static final class Spi implements SymbolizerToScene2D.Spi<PolygonSymbolizer> {

        @Override
        public Class<PolygonSymbolizer> getSymbolizerType() {
            return PolygonSymbolizer.class;
        }

        @Override
        public SymbolizerToScene2D create(Scene2D state, PolygonSymbolizer symbolizer) throws RenderingException {
            return new PolygonToScene2D(state, symbolizer);
        }
    }
}
