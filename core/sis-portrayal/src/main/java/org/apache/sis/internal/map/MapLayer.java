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
import org.opengis.style.Style;
import org.opengis.feature.Feature;
import org.opengis.coverage.Coverage;
import org.apache.sis.storage.Query;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.DataSet;
import org.apache.sis.storage.Aggregate;


/**
 * Data (resource) associated to visual representation (symbology).
 * Layers are the key elements of a map: they link data (given by {@link Resource}s) or a subset of
 * those data (filtered by {@link Query}) to their visual representation (defined by {@link Style}s).
 * The visual appearance of a layer should be similar with any rendering engine.
 * Some details may very because of different rendering strategies for label placements, 2D or 3D,
 * but the fundamentals aspect of each {@link Feature} or {@link Coverage} should be unchanged.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class MapLayer extends MapItem {
    /**
     * The {@value} property name, used for notifications about changes in map layer resource.
     * The resource provides the data to be rendered.
     * Associated values should be instances of {@link DataSet} or {@link Aggregate}.
     *
     * @see #getResource()
     * @see #setResource(Resource)
     */
    public static final String RESOURCE_PROPERTY = "resource";

    /**
     * The {@value} property name, used for notifications about changes in map layer query.
     * The query can filter resource data for rendering only a subset of available data.
     * Associated values are instances of {@link Query}.
     *
     * @see #getQuery()
     * @see #setQuery(Query)
     */
    public static final String QUERY_PROPERTY = "query";

    /**
     * The {@value} property name, used for notifications about changes in map layer style.
     * The style specifies the appearance of the filtered data to be rendered.
     * Associated values are instances of {@link Style}.
     *
     * @see #getStyle()
     * @see #setStyle(Style)
     */
    public static final String STYLE_PROPERTY = "style";

    /**
     * Data to be rendered, or {@code null} if unavailable.
     *
     * @see #RESOURCE_PROPERTY
     * @see #getResource()
     */
    private Resource resource;

    /**
     * Filter for rendering a subset of available data, or {@code null} if none.
     *
     * @see #QUERY_PROPERTY
     * @see #getQuery()
     */
    private Query query;

    /**
     * Visual representation of data, or {@code null} if none.
     *
     * @see #STYLE_PROPERTY
     * @see #getStyle()
     */
    private Style style;

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
     * The resource should be a {@link DataSet}, but {@link Aggregate} is also accepted.
     * The behavior in aggregate case depends on the rendering engine.
     *
     * @return data to be rendered, or {@code null} is unavailable.
     *
     * @see #RESOURCE_PROPERTY
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
     * <p>The given resource should be a {@link DataSet} or an {@link Aggregate} of data sets.
     * However this base class does not enforce those types. Subclasses may restrict the set
     * of resource types accepted by this method.</p>
     *
     * @param  newValue  the new data, or {@code null} if unavailable.
     */
    public void setResource(final Resource newValue) {
        final Resource oldValue = resource;
        if (!Objects.equals(oldValue, newValue)) {
            resource = newValue;
            firePropertyChange(RESOURCE_PROPERTY, oldValue, newValue);
        }
    }

    /**
     * Returns the filter for reducing the amount of data to render. Query filters can be
     * specified for rendering a smaller amount of data than what the resource can provide.
     * If the query is undefined, then all data will be rendered.
     *
     * @return filter for reducing data, or {@code null} for rendering all data.
     *
     * @see #QUERY_PROPERTY
     */
    public Query getQuery() {
        return query;
    }

    /**
     * Sets a filter for reducing the amount of data to render. If this method is never invoked, the default value
     * is {@code null}. If the given value is different than the previous value, then a change event is sent to all
     * listeners registered for the {@value #QUERY_PROPERTY} property.
     *
     * @param  newValue  filter for reducing data, or {@code null} for rendering all data.
     */
    public void setQuery(final Query newValue) {
        final Query oldValue = query;
        if (!Objects.equals(oldValue, newValue)) {
            query = newValue;
            firePropertyChange(QUERY_PROPERTY, oldValue, newValue);
        }
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
     * Sets the visual appearance of the data. If this method is never invoked, the default value is {@code null}.
     * If the given value is different than the previous value, then a change event is sent to all listeners
     * registered for the {@value #STYLE_PROPERTY} property.
     *
     * @param  newValue  description of data visual appearance, or {@code null} if unspecified.
     */
    public void setStyle(final Style newValue) {
        final Style oldValue = style;
        if (!Objects.equals(oldValue, newValue)) {
            style = newValue;
            firePropertyChange(STYLE_PROPERTY, oldValue, newValue);
        }
    }
}
