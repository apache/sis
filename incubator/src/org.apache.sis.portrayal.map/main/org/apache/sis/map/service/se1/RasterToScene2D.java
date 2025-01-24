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

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Stream;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.map.Presentation;
import org.apache.sis.map.SEPresentation;
import org.apache.sis.map.service.Scene2D;
import org.apache.sis.map.service.RenderingException;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageBuilder;
import org.apache.sis.coverage.grid.GridCoverageProcessor;
import org.apache.sis.coverage.grid.GridDerivation;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.NoSuchDataException;
import org.apache.sis.storage.Resource;
import org.apache.sis.style.se1.RasterSymbolizer;


/**
 * Support for RasterSymbolizer rendering.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class RasterToScene2D extends SymbolizerToScene2D<RasterSymbolizer<?>> {

    private RasterToScene2D(Scene2D state, RasterSymbolizer<?> symbolizer) {
        super(state, symbolizer);
    }

    @Override
    public void paint(SEPresentation presentation, Consumer<Stream<Presentation>> callback) throws RenderingException {
        Resource resource = presentation.getResource();

        if (resource instanceof GridCoverageResource) {
            final GridCoverageResource gcr = (GridCoverageResource) resource;

            try {
                GridCoverage coverage = gcr.read(state.grid);

                // naive inefficient implementation to be improved
                // keep only a 2D slice for rendering
                final GridGeometry gridGeometry = coverage.getGridGeometry();
                final GridDerivation sliceGridBuilder = gridGeometry.derive().sliceByRatio(0.5, 0, 1);
                final GridExtent intersection = sliceGridBuilder.getIntersection();
                final GridGeometry sliceGrid = sliceGridBuilder.build().selectDimensions(0,1);
                final RenderedImage image = coverage.render(intersection);

                final GridCoverageBuilder gcb = new GridCoverageBuilder();
                gcb.setValues(image);
                gcb.setDomain(sliceGrid);
                gcb.setRanges(coverage.getSampleDimensions());
                final GridCoverage coverage2d = gcb.build();

                final GridCoverageProcessor gcp = new GridCoverageProcessor();
                final GridCoverage resampled = gcp.resample(coverage2d, state.grid);

                //TODO handle raster symbolizer parameters.

                state.graphics.drawRenderedImage(resampled.render(null), new AffineTransform());

            } catch (NoSuchDataException ex) {
                //do nothing
            } catch (DataStoreException ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            } catch (TransformException ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            }

        }
    }

    @Override
    public boolean intersects(SEPresentation presentation, Shape mask, Consumer<Stream<Presentation>> callback) throws RenderingException {
        return false;
    }

    public static final class Spi implements SymbolizerToScene2D.Spi<RasterSymbolizer> {

        @Override
        public Class<RasterSymbolizer> getSymbolizerType() {
            return RasterSymbolizer.class;
        }

        @Override
        public SymbolizerToScene2D create(Scene2D state, RasterSymbolizer symbolizer) throws RenderingException {
            return new RasterToScene2D(state, symbolizer);
        }
    }
}
