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

import java.awt.Shape;
import java.util.stream.Stream;
import org.apache.sis.map.MapLayer;
import org.apache.sis.map.Presentation;
import org.apache.sis.style.Style;


/**
 * A Painter is responsible for portraying and querying resource on a scene.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface StylePainter {

    /**
     * Get the supported style implementation this painter can portray.
     *
     * @return supported style class.
     */
    Class<? extends Style> getStyleClass();

    /**
     * Stateless portraying of the given map layer.
     *
     * @param scene parameters for rendering
     * @param layer having supported style class.
     * @throws RenderingException if an error occured while rendering
     */
    void paint(Scene2D scene, MapLayer layer) throws RenderingException;

    /**
     * Statefull portraying of the given map layer.
     * <p>
     * Any exception should be returned in the stream as ExceptionPresentation, this allows
     * to still have some results even if a data caused an error.
     * <p>
     * The nature of the Presentation instance should be related to the style API used.
     *
     * @param scene parameters for rendering
     * @param layer having supported style class.
     * @return stream of presentation objects.
     */
    Stream<Presentation> present(Scene2D scene, MapLayer layer);

    /**
     * Search for elements in the scene which intersect the given area.
     * <p>
     * Any exception should be returned in the stream as ExceptionPresentation, this allows
     * to still have some results even if a data caused an error.
     *
     * @param scene parameters of the scene
     * @param layer to seach in
     * @param mask to search for intersection
     * @return a stream of presentation instances that intersect the searched area.
     */
    Stream<Presentation> intersects(Scene2D scene, MapLayer layer, Shape mask);

}
