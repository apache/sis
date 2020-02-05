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
package org.apache.sis.internal.map;

import org.opengis.feature.Feature;


/**
 * When rendering resources, the first step is to generate the visual aspect
 * of each feature or coverage, we call each one a Presentation.
 * <p>
 * A presentation object must be a simplistic description without any evaluation,
 * processing or loading work remaining.
 * </p>
 * <p>
 * It is important to note that multiple presentations may be generated for the
 * same feature.
 * </p>
 *
 * <p>
 * NOTE: this class is a first draft subject to modifications.
 * </p>
 *
 * @todo Maybe should be not be part of public API. This functionality could be done with a
 *       {@link MapItem} subclasses containing only a {@link Feature} or {@code GridCoverage}
 *       instance, as opposed to {@link MapLayer} containing {@code Resource}.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public abstract class Presentation {

    private MapLayer layer;
    private Object candidate;

    public Presentation() {
    }

    public Presentation(MapLayer layer, Object candidate) {
        this.layer = layer;
        this.candidate = candidate;
    }

    /**
     * Returns the original map layer the feature comes from.
     *
     * @return MapLayer can be null if the presentation is not associated to a layer.
     */
    public MapLayer getLayer() {
        return layer;
    }

    /**
     * Set map layer this presentation comes from.
     *
     * @param layer may be null
     */
    public void setLayer(MapLayer layer) {
        this.layer = layer;
    }

    /**
     * Returns the original candidate having this presentation.
     * This is often a Coverage or a Feature.
     *
     * @return can be null if the presentation is not associated to any identifiable object.
     */
    public Object getCandidate() {
        return candidate;
    }

    /**
     * Set feature this presentation comes from.
     *
     * @param feature may be null
     */
    public void setCandidate(Feature feature) {
        this.candidate = feature;
    }
}
