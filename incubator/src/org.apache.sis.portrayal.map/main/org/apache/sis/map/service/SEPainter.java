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
package org.apache.sis.map.service;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Stream;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.map.ExceptionPresentation;
import org.apache.sis.map.MapLayer;
import org.apache.sis.map.Presentation;
import org.apache.sis.map.SEPortrayer;
import org.apache.sis.map.SEPresentation;
import org.apache.sis.style.Style;
import org.apache.sis.style.se1.Symbolizer;
import org.apache.sis.style.se1.Symbology;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class SEPainter implements StylePainter {

    /**
     * Reference all painters.
     */
    private static final List<SymbolizerPainterDescription<?>> PAINTERS = new ArrayList<>();

    static {
        final ServiceLoader<SymbolizerPainterDescription> loader = ServiceLoader.load(SymbolizerPainterDescription.class, SymbolizerPainterDescription.class.getClassLoader());
        for (SymbolizerPainterDescription painter : loader) {
            PAINTERS.add(painter);
        }
    }

    private static final SymbolizerPainter NONE = new SymbolizerPainter(null) {
        @Override
        public void paint(Graphics2D g, GridGeometry gridGeometry, MapLayer layer) {
        }
    };

    @Override
    public Class<? extends Style> getStyleClass() {
        return Symbology.class;
    }

    @Override
    public void paint(Graphics2D g, BufferedImage image, GridGeometry gridGeometry, MapLayer layer) {

        final Map<Symbolizer, SymbolizerPainter> subPainters = new HashMap<>();

        final SEPortrayer p = new SEPortrayer();
        try (Stream<Presentation> presentations = p.present(gridGeometry, layer)) {

            //process presentations in order
            final Iterator<Presentation> ite = presentations.iterator();
            while (ite.hasNext()) {
                final Presentation presentation = ite.next();
                if (presentation instanceof SEPresentation) {
                    final SEPresentation sepre = (SEPresentation) presentation;
                    final Symbolizer<?> symbolizer = sepre.getSymbolizer();
                    SymbolizerPainter subPainter = subPainters.get(symbolizer);

                    creation:
                    if (subPainter == null) {
                        subPainter = NONE;
                        for (SymbolizerPainterDescription s : PAINTERS) {
                            if (s.getSymbolizerClass().isInstance(symbolizer)) {
                                subPainter = s.createRenderer(symbolizer);
                                break;
                            }
                        }
                        subPainters.put(symbolizer, subPainter);
                    }

                    if (subPainter == NONE) {
                        continue;
                    }

                    subPainter.paint(g, gridGeometry, layer);

                } else if (presentation instanceof ExceptionPresentation) {
                    //todo
                } else {
                    //todo
                }
            }

        }

    }

}
