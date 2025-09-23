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
import java.awt.Shape;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Feature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.map.Presentation;
import org.apache.sis.map.SEPresentation;
import org.apache.sis.map.service.Scene2D;
import org.apache.sis.map.service.RenderingException;
import org.apache.sis.feature.internal.shared.AttributeConvention;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.apache.sis.geometry.wrapper.jts.JTS;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.style.se1.LineSymbolizer;


/**
 * Support for LineSymbolizer rendering.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class LineToScene2D extends SymbolizerToScene2D<LineSymbolizer<?>> {

    private LineToScene2D(Scene2D state, LineSymbolizer<?> symbolizer) {
        super(state, symbolizer);
    }

    @Override
    public void paint(SEPresentation presentation, Consumer<Stream<Presentation>> callback) throws RenderingException {
        final RenderedShape visual = createVisual(presentation);
        if (visual != null) {
            visual.paint(state.getGraphics());
        }
    }

    @Override
    public boolean intersects(SEPresentation presentation, Shape mask, Consumer<Stream<Presentation>> callback) throws RenderingException {
        final RenderedShape visual = createVisual(presentation);
        if (visual != null) {
            return visual.intersects(mask);
        }
        return false;
    }

    private RenderedShape createVisual(SEPresentation presentation) throws RenderingException {
        final Feature feature = presentation.getCandidate();
        Object geometry = feature.getPropertyValue(AttributeConvention.GEOMETRY);

        if (geometry instanceof Geometry) {

            final MathTransform gridToCRS = state.grid.getGridToCRS(PixelInCell.CELL_CENTER);

            final GeometryWrapper geomWrap = Geometries.wrap(geometry).get();
            final CoordinateReferenceSystem geomCrs = geomWrap.getCoordinateReferenceSystem();

            final Geometry jts;
            try {
                final CoordinateOperation coop = CRS.findOperation(geomCrs, state.grid.getCoordinateReferenceSystem(), null);
                final MathTransform geomToGrid = MathTransforms.concatenate(coop.getMathTransform(), gridToCRS.inverse());

                jts = JTS.transform((Geometry) geometry, geomToGrid);
            } catch (FactoryException | TransformException ex) {
                throw new RenderingException(ex);
            }

            Shape shape = JTS.asShape(jts);

            //TODO geometry world wrap and styling

            RenderedShape rs = new RenderedShape();
            rs.shape = shape;
            rs.stroke = new BasicStroke(1);
            rs.strokePaint = Color.BLACK;

            return rs;
        }

        return null;
    }

    public static final class Spi implements SymbolizerToScene2D.Spi<LineSymbolizer> {

        @Override
        public Class<LineSymbolizer> getSymbolizerType() {
            return LineSymbolizer.class;
        }

        @Override
        public SymbolizerToScene2D create(Scene2D state, LineSymbolizer symbolizer) throws RenderingException {
            return new LineToScene2D(state, symbolizer);
        }
    }
}
