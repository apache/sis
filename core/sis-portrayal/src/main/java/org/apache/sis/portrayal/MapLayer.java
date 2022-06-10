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
package org.apache.sis.portrayal;

import java.util.Objects;
import java.util.Optional;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataSet;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Query;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.ArgumentChecks;
import org.opengis.coverage.Coverage;
import org.opengis.feature.Feature;
import org.opengis.geometry.Envelope;
import org.opengis.style.Style;


/**
 * Data (resource) associated to rules for visual representation (symbology).
 * Layers are the key elements of a map: they link data (given by {@link Resource}s) or a subset of
 * those data (filtered by {@link Query}) to their visual representation (defined by {@link Style}s).
 * The visual appearance of a layer should be similar with any rendering engine.
 * Some details may very because of different rendering strategies for label placements, 2D or 3D,
 * but the fundamentals aspect of each {@link Feature} or {@link Coverage} should be unchanged.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
public class MapLayer extends MapItem {
    /**
     * The {@value} property name, used for notifications about changes in map layer resource.
     * The data resource provides the digital data to be rendered. Note that not all kinds of resources
     * are digital data. For example a resource may be a citation of facts or figures printed on paper,
     * photographic material, or other media (see all {@link org.opengis.metadata.citation.PresentationForm}
     * values having the {@code _HARDCOPY} suffix in their name).
     * Associated values should be instances of {@link DataSet} or {@link Aggregate}.
     *
     * @see #getData()
     * @see #setData(Resource)
     */
    public static final String DATA_PROPERTY = "data";

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
     * The {@value} property name, used for notifications about changes in map layer opacity.
     * The opacity specifies the gloabal opacity of the data to be rendered.
     *
     * @see #getOpacity()
     * @see #setOpacity(double)
     */
    public static final String OPACITY_PROPERTY = "opacity";

    /**
     * Data to be rendered, or {@code null} if unavailable.
     *
     * @see #DATA_PROPERTY
     * @see #getData()
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
     * Visual transparency of data, or {@code null} if none.
     *
     * @see #OPACITY_PROPERTY
     * @see #getOpacity()
     */
    private double opacity = 1.0;

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
     * @see #DATA_PROPERTY
     */
    public Resource getData() {
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
     * <p>Note that not all kinds of resources are digital data. For example a resource may be an organization,
     * or citation of facts, tables and figures printed on paper, photographic material, or other media
     * (see all {@link org.opengis.metadata.citation.PresentationForm} values having the {@code _HARDCOPY}
     * suffix in their name). The kind of resources in {@code MapLayer} shall be one of those representing
     * digital data.</p>
     *
     * @param  newValue  the new data, or {@code null} if unavailable.
     */
    public void setData(final Resource newValue) {
        final Resource oldValue = resource;
        if (!Objects.equals(oldValue, newValue)) {
            resource = newValue;
            firePropertyChange(DATA_PROPERTY, oldValue, newValue);
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

    /**
     * Returns the global opacity of this layer.
     * Based on the rendering context this property may be impossible to implement,
     * it is therefor recommended to modify the style symbolizer opacity properties.
     *
     * @return opacity between 0.0 and 1.0
     */
    public double getOpacity() {
        return opacity;
    }

    /**
     * Sets the global rendering opacity of this layer.
     *
     * @param opacity must be betwen 0.0 and 1.0
     */
    public void setOpacity(double opacity) {
        ArgumentChecks.ensureBetween(OPACITY_PROPERTY, 0.0, 1.0, opacity);
        if (this.opacity != opacity) {
            double old = this.opacity;
            this.opacity = opacity;
            firePropertyChange(OPACITY_PROPERTY, old, opacity);
        }
    }

    /**
     * Returns the envelope of this {@code MapLayer}.
     * The envelope is the resource data envelope.
     *
     * @return the spatiotemporal extent. May be absent if none or too costly to compute.
     * @throws DataStoreException if an error occurred while reading or computing the envelope.
     */
    @Override
    public Optional<Envelope> getEnvelope() throws DataStoreException {
        Resource data = getData();
        if (data instanceof DataSet) {
            return ((DataSet) data).getEnvelope();
        }
        return Optional.empty();
    }
}
