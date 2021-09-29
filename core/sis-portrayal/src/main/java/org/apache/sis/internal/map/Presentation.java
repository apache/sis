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

import java.util.Objects;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.portrayal.MapLayer;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.Resource;
import org.opengis.feature.Feature;


/**
 * Parent class of all elements having a graphical representation on the map.
 * {@code Presentation} instances are organized in a tree closely related to the {@link MapLayer} tree.
 * The {@link MapLayer} tree specifies data and styles in a device-independent way and for all zoom levels.
 * The {@code Presentation} tree can be seen as {@link MapLayer} information filtered for the current rendering
 * context (map projection, zoom level, window size, <i>etc.</i>) and converted to data structures more directly
 * exploitable by the display device. In particular a {@code Presentation} object must encapsulate data without
 * costly evaluation, processing or loading work remaining to do: the {@link Feature} or the {@link GridCoverage}
 * (for instance) should have been read in advance from the {@link DataStore}.
 * The preparation of a {@link Presentation} tree before displaying may be done in a background thread.
 *
 * <p>Note that multiple presentations may be generated for the same feature.
 * Consequently many {@code Presentation} instances may encapsulate the same {@link Feature} instance.</p>
 *
 * <p>
 * NOTE: this class is a first draft subject to modifications.
 * </p>
 *
 * @todo Consider renaming as {@code Graphic} for emphasis that this is a graphical representation of something.
 *       This would be consistent with legacy GO-1 specification (even if retired, it still have worthy material).
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
public abstract class Presentation {

    private MapLayer layer;
    private Resource resource;
    private Feature candidate;

    public Presentation() {
    }

    public Presentation(MapLayer layer, Resource resource, Feature candidate) {
        this.layer = layer;
        this.resource = resource;
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

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    /**
     * Returns the original candidate having this presentation.
     * This is often a Coverage or a Feature.
     *
     * @return can be null if the presentation is not associated to any identifiable object.
     */
    public Feature getCandidate() {
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

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + Objects.hashCode(this.layer);
        hash = 89 * hash + Objects.hashCode(this.resource);
        hash = 89 * hash + Objects.hashCode(this.candidate);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Presentation other = (Presentation) obj;
        if (!Objects.equals(this.layer, other.layer)) {
            return false;
        }
        if (!Objects.equals(this.resource, other.resource)) {
            return false;
        }
        if (!Objects.equals(this.candidate, other.candidate)) {
            return false;
        }
        return true;
    }

}
