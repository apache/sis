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

import org.apache.sis.storage.Query;
import org.apache.sis.storage.Resource;
import org.opengis.style.Style;


/**
 * Data (resource) associated to visual representation (symbology).
 * Layers are the key elements of a map: they link datas (given by a {@link Resource})
 * to their visual representation (defined by a {@link Style}).
 * The visual appearance of a layer should be similar with any rendering engine.
 * Some details may very because of different rendering strategies for label placements, 2D or 3D,
 * but the fundamentals aspect of each {@link org.opengis.feature.Feature} or
 * {@link org.opengis.coverage.Coverage} should be unchanged.
 *
 * <p>
 * NOTE: this class is a first draft subject to modifications.
 * TODO : missing events
 * </p>
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class MapLayer extends MapItem {
    /**
     * Data to be rendered.
     */
    private Resource resource;

    /**
     * Visual representation of data.
     */
    private Style style;

    /**
     * User defined query.
     */
    private Query query;

    /**
     * Constructs an initially empty map layer.
     *
     * @todo Expect {@code Resource} and {@code Style} in argument, for discouraging
     *       the use of {@code MapLayer} with null resource and null style?
     */
    public MapLayer() {
    }

    /**
     * Returns the data (resource) represented by this layer.
     * The resource should be a {@link org.apache.sis.storage.DataSet},
     * but {@link org.apache.sis.storage.Aggregate} are also accepted.
     * The behavior in such case depends on the rendering engine.
     *
     * @return data to be rendered, or {@code null} is unavailable.
     */
    public Resource getResource() {
        return resource;
    }

    /**
     * Sets the data (resource) to be rendered.
     * The resource should never be null, still the null case is tolerated to indicate
     * that the layer should have existed but is unavailable for an unspecified reason.
     * This case may happen with processing or distant services resources.
     *
     * @param  resource  the new data, or {@code null} if unavailable.
     */
    public void setResource(Resource resource) {
        this.resource = resource;
    }

    /**
     * Returns the visual appearance of the data.
     * If the style is undefined, the behavior is left to the rendering engine.
     * It is expected that a default style should be used.
     *
     * @return description of data visual appearance, or {@code null} if unspecified.
     */
    public Style getStyle() {
        return style;
    }

    /**
     * Sets the visual appearance of the data.
     *
     * @param  style  description of data visual appearance, or {@code null} if unspecified.
     */
    public void setStyle(Style style) {
        this.style = style;
    }

    /**
     * Returns the definition query for this layer.If no definition
     * query has been defined {@code null} is returned.
     * @return query, or {@code null} if unspecified.
     */
    public Query getQuery() {
        return query;
    }

    /**
     * Sets a filter query for this layer.
     *
     * <p>
     * Query filters should be used to reduce searched or displayed resource
     * when rendering or analyzing this layer.
     * </p>
     *
     * @param query the query for this layer, or {@code null} if unavailable.
     */
    public void setQuery(Query query) {
        this.query = query;
    }
}
