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

import org.apache.sis.map.service.Scene2D;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.apache.sis.map.Presentation;
import org.apache.sis.map.SEPresentation;
import org.apache.sis.map.service.RenderingException;
import org.apache.sis.style.se1.Symbolizer;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ObjectConverters;
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;

/**
 * Transforms a {@link Presentation} to Java2D graphics.
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class SymbolizerToScene2D<S extends Symbolizer> {

    private static final List<SymbolizerToScene2D.Spi<?>> SPIS;
    private static final Map<Class<?>,SymbolizerToScene2D.Spi<?>> SPI_MAP = new HashMap<>();
    static {
        //collect all symbolizer SPI
        List<SymbolizerToScene2D.Spi<?>> spis = new ArrayList<>();
        ServiceLoader.load(SymbolizerToScene2D.Spi.class).iterator().forEachRemaining(spis::add);
        SPIS = Collections.unmodifiableList(spis);
        for (SymbolizerToScene2D.Spi<?> spi : SPIS) {
            if (SPI_MAP.put(spi.getSymbolizerType(), spi) != null) {
                throw new IllegalStateException("More then one SymbolizerToScene2D.Spi is registered for type " + spi.getSymbolizerType().getName());
            }
        }
    }

    protected static final Logger LOGGER = Logger.getLogger("com.examind.sdk.render");
    protected final Scene2D state;
    protected final S symbolizer;

    protected SymbolizerToScene2D(Scene2D state, S symbolizer) {
        this.state = state;
        this.symbolizer = symbolizer;
    }

    /**
     * Define a shared cache instance.
     * Shared caches avoid multiple loading of a resource (example : images, models)
     *
     * @param cache not null
     */
    public void sharedCache(SymbolizerCache cache) {
    }

    /**
     * Paint the given {@link SEPresentation}.
     *
     * @param presentation not null
     * @param callback not null, can be used to create new presentation treated in the rendering loop.
     * @throws RenderingException
     */
    public abstract void paint(SEPresentation presentation, Consumer<Stream<Presentation>> callback) throws RenderingException;

    /**
     * Test intersection of the given {@link SEPresentation}.
     *
     * @param presentation not null
     * @param callback not null, can be used to test new presentations treated in the rendering loop.
     * @throws RenderingException
     */
    public boolean intersects(SEPresentation presentation, Shape mask, Consumer<Stream<Presentation>> callback) throws RenderingException {
        return false;
    }

    static <T> T evaluate(Feature feature, Expression<? super Feature,?> exp, Expression<? super Feature,?> fallback, Class<T> clazz) {
        T value = null;
        if (exp != null) {
            value = ObjectConverters.convert(exp.apply(feature), clazz);
        }
        if (value == null && fallback != null) {
            value = ObjectConverters.convert(fallback.apply(feature), clazz);
        }
        return value;
    }

    /**
     * Create a symbolizer to scene processor.
     *
     * @param state not null
     * @param symbolizer not null
     * @return may be null if no Spi support this symbolizer.
     * @throws RenderingException if the symbolizer is incorrectly defined or some assets cannot be resolved.
     */
    public static SymbolizerToScene2D<?> create(Scene2D state, Symbolizer<?> symbolizer) throws RenderingException {
        ArgumentChecks.ensureNonNull("symbolizer", symbolizer);
        final Spi<Symbolizer> spi = (Spi<Symbolizer>) getSpi(symbolizer.getClass());
        return spi == null ? null : spi.create(state, symbolizer);
    }

    /**
     * Create a symbolizer cache.
     *
     * @param symbolizer not null
     * @return may be null if no Spi support this symbolizer.
     * @throws RenderingException if the symbolizer is incorrectly defined or some assets cannot be resolved.
     */
    public static SymbolizerCache createCache(Symbolizer symbolizer) throws RenderingException {
        for (SymbolizerToScene2D.Spi spi : SPIS) {
            final SymbolizerCache sts = spi.createCache(symbolizer);
            if (sts != null) {
                return sts;
            }
        }
        return null;
    }
    /**
     * Get the Spi capable to handle given symbolizer.
     *
     * @return may be null if no Spi support this symbolizer.
     */
    public static synchronized <T extends Symbolizer> SymbolizerToScene2D.Spi<T> getSpi(Class<T> clazz) {
        Spi<T> cdt = (Spi<T>) SPI_MAP.get(clazz);
        if (cdt == null) {
            for (SymbolizerToScene2D.Spi spi : SPIS) {
                if (spi.getSymbolizerType().isAssignableFrom(clazz)) {
                    SPI_MAP.put(clazz, spi);
                    cdt = spi;
                    break;
                }
            }
        }
        return cdt;
    }

    /**
     * Factory to create new transformation instances.
     *
     * @param <T> symbolizer type supported
     */
    public interface Spi<T extends Symbolizer> {

        /**
         * Returns the support symbolizer class.
         * @return supported symbolizer class, not null.
         */
        Class<T> getSymbolizerType();

        /**
         * Create a cache for given {@link Symbolizer}.
         *
         * @param symbolizer not null
         * @return cache or null if symbolizer is not supported or no cache is needed.
         * @throws RenderingException if symbolizer declaration contains errors.
         */
        default SymbolizerCache createCache(T symbolizer) throws RenderingException {
            return null;
        }

        /**
         * Create a transformation instance.
         *
         * @param state scene state, not null
         * @param symbolizer not null
         * @return instance or null if symbolizer is not supported
         * @throws RenderingException if symbolizer declaration contains errors.
         */
        SymbolizerToScene2D create(Scene2D state, T symbolizer) throws RenderingException;
    }
}
