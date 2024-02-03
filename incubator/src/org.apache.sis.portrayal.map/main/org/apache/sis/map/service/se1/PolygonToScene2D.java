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
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.apache.sis.map.Presentation;
import org.apache.sis.map.SEPresentation;
import org.apache.sis.map.service.Scene2D;
import org.apache.sis.map.service.RenderingException;
import org.apache.sis.style.se1.PolygonSymbolizer;


/**
 * Support for PointSymbolizer rendering.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class PolygonToScene2D extends SymbolizerToScene2D<PolygonSymbolizer<?>> {

    private PolygonToScene2D(Scene2D state, PolygonSymbolizer<?> symbolizer) {
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
        //todo
        return null;
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
