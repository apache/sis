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

import java.awt.Graphics2D;
import java.awt.Shape;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Stream;
import org.opengis.coordinate.MismatchedDimensionException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.map.Presentation;
import org.apache.sis.map.SEPresentation;
import org.apache.sis.map.service.Scene2D;
import org.apache.sis.map.service.RenderingException;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.style.se1.Symbolizer;


/**
 * Generate a 2D scene from canvas and graphic presentations.
 *
 * @author Johann Sorel (Geomatys)
 */
final class PresentationToScene2D {

    /**
     * Flag instance for missing symbolizers converters.
     */
    private static final SymbolizerToScene2D<?> NONE = new SymbolizerToScene2D<Symbolizer<?>>(null, null){
        @Override
        public void paint(SEPresentation presentation, Consumer<Stream<Presentation>> callback) throws RenderingException {
            throw new UnsupportedOperationException("Should not called.");
        }
    };
    private static final SymbolizerCache NO_CACHE = new SymbolizerCache(){};

    private final Scene2D state;

    private final Map<Symbolizer<?>,SymbolizerToScene2D<?>> cache = new HashMap<>();
    private Map<Symbolizer<?>,SymbolizerCache> symbolizerCaches;

    /**
     * Prepare parameters for scene creation.The grid geometry will provide the base coordinate system and scale informations.
     * All further presentations must have been build with given grid as base.
     *
     * @param grid, not null
     */
    public PresentationToScene2D(GridGeometry grid, Graphics2D graphics) throws FactoryException, MismatchedDimensionException, TransformException {
        // Null values are verified by the Scene2D constuctor.
        state = new Scene2D(grid, graphics);
    }

    /**
     * Create converter from an existing scene state.
     *
     * @param state, not null
     */
    public PresentationToScene2D(Scene2D state) {
        this.state = Objects.requireNonNull(state);
    }

    /**
     * Define global shared cache map instance.
     *
     * @param symbolizerCaches
     */
    public void setSymbolizerCaches(Map<Symbolizer<?>, SymbolizerCache> symbolizerCaches) {
        this.symbolizerCaches = symbolizerCaches;
    }

    /**
     * Convert and add given presentation to the scene.
     * Exceptions will be logged.
     *
     * @param presentations, not null, will be closed.
     */
    public void render(Stream<Presentation> presentations) {
        try {
            presentations.parallel().forEach(new Consumer<Presentation>() {
                @Override
                public void accept(Presentation t) {
                    try {
                        render(t);
                    } catch (Exception ex) {
                        Scene2D.LOGGER.log(Level.INFO, ex.getMessage(), ex);
                    }
                }
                });
        } finally {
            presentations.close();
        }
    }

    private void render(Presentation presentation) throws RenderingException, IOException, NoninvertibleTransformException, TransformException, URISyntaxException, FactoryException, DataStoreException {
        //standard presentation types
        if (presentation instanceof SEPresentation) {
            render((SEPresentation) presentation);
        } else {
            //unknown type
        }
    }

    private void render(SEPresentation presentation) throws MismatchedDimensionException, TransformException, FactoryException, DataStoreException, IOException, RenderingException {
        final SymbolizerToScene2D<?> sts = getRenderer(presentation.getSymbolizer());

        if (sts != null) {
            sts.paint(presentation, this::render);
        } else {
            Scene2D.LOGGER.log(Level.INFO, "Unnowned symbolizer {0}", presentation.getSymbolizer().getClass().getName());
        }
    }

    /**
     * Process given presentations and retain only thos who intersects the requested shape.
     *
     * @param presentations, not null.
     */
    public Stream<Presentation> intersects(Stream<Presentation> presentations, Shape mask) {

        final Consumer<Stream<Presentation>> callback = (Stream<Presentation> t) -> {intersects(t, mask);};

        return presentations.parallel().filter(new Predicate<Presentation>() {
            @Override
            public boolean test(Presentation t) {
                try {
                    return intersects(t, mask, callback);
                } catch (RenderingException ex) {
                    Scene2D.LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                }
                return false;
            }
        });
    }

    private boolean intersects(Presentation presentation, Shape mask,  Consumer<Stream<Presentation>> callback) throws RenderingException {
        //standard presentation types
        if (presentation instanceof SEPresentation) {
            return intersects((SEPresentation) presentation, mask, callback);
        } else {
            //unknown type
            return false;
        }
    }

    private boolean intersects(SEPresentation presentation, Shape mask,  Consumer<Stream<Presentation>> callback) throws RenderingException {
        final SymbolizerToScene2D<?> sts = getRenderer(presentation.getSymbolizer());

        if (sts != null) {
            return sts.intersects(presentation, mask, callback);
        } else {
            Scene2D.LOGGER.log(Level.INFO, "Unnowned symbolizer {0}", presentation.getSymbolizer().getClass().getName());
            return false;
        }
    }

    private SymbolizerToScene2D<?> getRenderer(Symbolizer<?> symbolizer) throws RenderingException {
        SymbolizerToScene2D<?> sts;
        synchronized (cache) {
            sts = cache.get(symbolizer);
            if (sts == NONE) {
                sts = null;
            } else if (sts == null) {
                sts = SymbolizerToScene2D.create(state, symbolizer);
                if (sts == null) sts = NONE;
                cache.put(symbolizer, sts);
                if (sts == NONE) {
                    sts = null;
                } else if (symbolizerCaches != null) {
                    // get or create shared cache
                    SymbolizerCache cache = symbolizerCaches.get(symbolizer);
                    if (cache == null) {
                        cache = SymbolizerToScene2D.createCache(symbolizer);
                        if (cache == null) {
                            cache = NO_CACHE;
                        }
                        symbolizerCaches.put(symbolizer, cache);
                    }
                    if (cache != NO_CACHE) {
                        sts.sharedCache(cache);
                    }
                }
            }
        }
        return sts;
    }

}
