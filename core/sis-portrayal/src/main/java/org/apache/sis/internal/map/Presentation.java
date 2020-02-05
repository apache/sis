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
import org.apache.sis.storage.Resource;
import org.apache.sis.coverage.grid.GridCoverage;


/**
 * Parent class of all elements having a graphical representation on the map.
 * {@code Presentation} instances are organized in a tree parallel to the {@link MapLayer} tree.
 * The {@link MapLayer} tree links data and styles in a device-independent way, while
 * the {@code Presentation} tree can be seen as map layers information "compiled"
 * in a form more directly exploitable by the display device.
 * In particular a {@code Presentation} objects must encapsulate data without
 * costly evaluation, processing or loading work remaining to be done.
 * (for example {@link Feature} or {@link GridCoverage} instances instead than {@link Resource}s).
 *
 * <p>Note that multiple presentations may be generated for the same feature.
 * Consequently many {@code Presentation} instances may encapsulate the same {@link Feature} instances.</p>
 *
 * <p>
 * NOTE: this class is a first draft subject to modifications.
 * </p>
 *
 * @todo Consider renaming as {@code Graphic} for emphasis that this is a graphical representation of something.
 *       This would be consistent with legacy GO-1 specification (even if retired, it still have worthy material).
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
