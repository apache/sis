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
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.operation.OperationException;
import org.apache.sis.map.SEPresentation;
import org.apache.sis.map.service.Scene2D;
import org.apache.sis.map.service.RenderingException;
import org.apache.sis.map.service.common.DrawingComposite;
import org.apache.sis.map.service.common.Drawing;
import org.apache.sis.style.se1.LineSymbolizer;
import org.apache.sis.style.se1.Stroke;


/**
 * Support for LineSymbolizer rendering.
 *
 * <p>All the {@link LineSymbolizer} parameters are supported:</p>
 * <ul>
 *   <li>Geometry: the geometry expression is evaluated on the feature. JTS geometries are viewed as SIS geometries.
 *       All linear components are rendered (line strings, curves when they can be linearized, polygon rings).</li>
 *   <li>Unit of measure: pixel or ground linear units (metre, foot, …). All distances are scaled accordingly.</li>
 *   <li>Perpendicular offset: positive values to the left of the line direction, negative to the right.</li>
 *   <li>Stroke: color, opacity, width, line join, line cap, dash array, dash offset,
 *       graphic fill (tiled pattern) and graphic stroke (repeated graphic with initial gap and gap).</li>
 * </ul>

 * <p>Supported graphics are listed in {@link GraphicRenderer}.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class LineToScene2D extends SymbolizerToScene2D<LineSymbolizer<?>> {

    LineToScene2D(Scene2D state, LineSymbolizer<?> symbolizer) {
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
     * Creates the visual elements for the given presentation.
     *
     * @return visual elements, may be empty but never null.
     */
    private List<Drawing> createDrawings(SEPresentation presentation) throws RenderingException {
        final Feature feature = presentation.getCandidate();
        final Geometry geometry = GeometryResolver.resolve(evaluate(feature, symbolizer.getGeometry()));
        if (geometry == null) {
            return List.of();
        }
        final Stroke<?> stroke = symbolizer.getStroke();
        if (stroke == null) {
            return List.of();
        }
        final double pixelsPerUnit = state.getPixelsPerUnit(symbolizer.getUnitOfMeasure());
        return StrokeStyle.drawings(toDisplay(geometry, feature, pixelsPerUnit), stroke, feature, pixelsPerUnit);
    }

    /**
     * Extracts the linear components of the given geometry, transforms them to display space
     * and applies the perpendicular offset.
     */
    private List<LineString> toDisplay(Geometry geometry, Feature feature, double pixelsPerUnit) throws RenderingException {
        final List<LineString> components = new ArrayList<>();
        GeometryResolver.linearComponents(geometry, components);
        final double offset = evaluateNumber(feature, symbolizer.getPerpendicularOffset(), 0.0) * pixelsPerUnit;
        final List<LineString> lines = new ArrayList<>(components.size());
        for (LineString line : components) {
            if (line.getNumPoints() < 2) continue;
            final MathTransform dataToDisplay = state.getDataToDisplay(line.getCoordinateReferenceSystem());
            final LineString display;
            try {
                display = GeometryResolver.toDisplay(line, dataToDisplay);
            } catch (OperationException ex) {
                throw new RenderingException(ex);
            }
            final LineString shifted = GeometryResolver.offset(display, offset);
            lines.add(shifted != null ? shifted : display);     // Rendered without offset if it cannot be computed.
        }
        return lines;
    }
}
