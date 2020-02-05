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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.storage.DataSet;


/**
 * A group of graphic elements to display together.
 * {@code MapLayers} can be used for grouping related layers under a same node.
 * This allows global actions, like {@linkplain #setVisible(boolean) hiding}
 * background layers in one call.
 *
 * <p>A {@code MapLayers} is often (but not necessarily) the root node of the tree of all graphic elements to
 * draw on the map. Those elements are typically {@link MapLayer} instances, but more generic {@link MapItem}
 * instances are also accepted. Those elements are listed by {@link #getComponents()} in <var>z</var> order.
 * In addition, {@code MapLayers} defines the {@linkplain #getAreaOfInterest() area of interest}
 * which should be zoomed by default when the map is rendered.</p>
 *
 * @todo Despite its name, this class is not a container of {@link MapLayer}s since it accepts more generic objects.
 *       Consider renaming as {@code Group} or {@code GraphicAggregate}.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class MapLayers extends MapItem {
    /**
     * The {@value} property name, used for notifications about changes in area of interest.
     * Associated values are instances of {@link Envelope}.
     *
     * @see #getAreaOfInterest()
     * @see #setAreaOfInterest(Envelope)
     */
    public static final String AREAOFINTEREST_PROPERTY = "areaOfInterest";

    /**
     * The components in this group, or an empty list if none.
     *
     * @todo Should be an observable list with event sent when an element is added/removed/modified.
     */
    private final List<MapItem> components;

    /**
     * The area of interest, or {@code null} is unspecified.
     */
    private Envelope areaOfInterest;

    /**
     * Creates an initially empty group of graphic elements.
     */
    public MapLayers() {
        components = new ArrayList<>();
    }

    /**
     * Gets the modifiable list of components contained in this group.
     * The components in the list are presented in rendering order.
     * This means that the first rendered component, which will be below
     * all other components on the rendered map, is located at index zero.
     *
     * <p>The returned list is modifiable: changes in the returned list will
     * be immediately reflected in this {@code MapGroup}, and conversely.</p>
     *
     * @return modifiable list of components in this group.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<MapItem> getComponents() {
        return components;
    }

    /**
     * Returns the map area to show by default.
     * This is not necessarily the {@linkplain DataSet#getEnvelope() envelope of data}
     * since one may want to zoom in a different spatiotemporal area.
     *
     * <p>The {@linkplain org.apache.sis.geometry.GeneralEnvelope#getCoordinateReferenceSystem() envelope CRS}
     * defines the map projection to use for rendering the map. It may be different than the CRS of the data.
     * The returned envelope may have {@linkplain org.apache.sis.geometry.GeneralEnvelope#isAllNaN() all its
     * coordinates set to NaN} if only the {@link CoordinateReferenceSystem} is specified.</p>
     *
     * @return map area to show by default, or {@code null} is unspecified.
     *
     * @see DataSet#getEnvelope()
     */
    public Envelope getAreaOfInterest() {
        return areaOfInterest;
    }

    /**
     * Sets the map area to show by default.
     * The given envelope is not necessarily related to the data contained in this group.
     * It may be wider, or smaller, and in a different {@link CoordinateReferenceSystem}.
     *
     * @param  newValue  new map area to show by default, or {@code null} is unspecified.
     */
    public void setAreaOfInterest(final Envelope newValue) {
        final Envelope oldValue = areaOfInterest;
        if (!Objects.equals(oldValue, newValue)) {
            areaOfInterest = newValue;
            firePropertyChange(AREAOFINTEREST_PROPERTY, oldValue, newValue);
        }
    }
}
