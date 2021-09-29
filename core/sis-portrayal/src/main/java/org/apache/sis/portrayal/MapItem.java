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

import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.sis.storage.DataStoreException;
import org.opengis.geometry.Envelope;
import org.opengis.util.InternationalString;


/**
 * Base class of map layer or group of map layers. This base class does not represent graphical elements.
 * Instead it contains information (data and style) for creating a tree of portrayal objects.
 * A {@code MapItem} contains the following properties:
 *
 * <ul>
 *   <li>An {@linkplain #getIdentifier() identifier}, which can be any {@link String} at developer choice.</li>
 *   <li>A human-readable {@linkplain #getTitle() title} for pick lists, for example in GUI.</li>
 *   <li>A {@linkplain #getAbstract() narrative description} providing more details.</li>
 * </ul>
 *
 * Additional information can be added in a map of {@linkplain #getUserProperties() user properties}.
 * The actual feature or coverage data, together with styling information, are provided by subclasses.
 *
 * <h2>Synchronization</h2>
 * {@code MapItem} instances are not thread-safe. Synchronization, if desired, is caller responsibility.
 *
 * @todo Rename as {@code LayerNode}? "Item" suggests an element in a list, while {@link MapLayers} actually
 *       creates a tree. Furthermore having {@code Layer} in the name would add emphasis that this is a tree
 *       of layers and not a tree of arbitrary objects.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
public abstract class MapItem extends Observable {
    /**
     * The {@value} property name, used for notifications about changes in map item identifier.
     * The identifier (or name) can be used to reference the item externally.
     * Associated values are instances of {@link String}.
     *
     * @see #getIdentifier()
     * @see #setIdentifier(String)
     * @see #addPropertyChangeListener(String, PropertyChangeListener)
     *
     * @todo This property seems to be named {@code "se:Name"} in SLD specification. Should we rename?
     */
    public static final String IDENTIFIER_PROPERTY = "identifier";

    /**
     * The {@value} property name, used for notifications about changes in map item title.
     * The title is a short description for item that might be displayed in a GUI pick list.
     * Associated values are instances of {@link String} or {@link InternationalString}.
     *
     * @see #getTitle()
     * @see #setTitle(CharSequence)
     * @see #addPropertyChangeListener(String, PropertyChangeListener)
     */
    public static final String TITLE_PROPERTY = "title";

    /**
     * The {@value} property name, used for notifications about changes in map item description.
     * The abstract is a narrative description providing additional information.
     * It is more detailed than the {@value #TITLE_PROPERTY} property and may be a few paragraphs long.
     * Associated values are instances of {@link String} or {@link InternationalString}.
     *
     * @see #getAbstract()
     * @see #setAbstract(CharSequence)
     * @see #addPropertyChangeListener(String, PropertyChangeListener)
     */
    public static final String ABSTRACT_PROPERTY = "abstract";

    /**
     * The {@value} property name, used for notifications about changes in map item visibility state.
     * Associated values are instances of {@link Boolean}.
     */
    public static final String VISIBLE_PROPERTY = "visible";

    /**
     * Identifier of this map item.
     *
     * @see #IDENTIFIER_PROPERTY
     * @see #getIdentifier()
     */
    private String identifier;

    /**
     * The title of this map item, for display to the user.
     *
     * @see #TITLE_PROPERTY
     * @see #getTitle()
     */
    private CharSequence title;

    /**
     * A description of this map item, for display to the user. The property name is
     * {@value #ABSTRACT_PROPERTY} but we use a different field name because "abstract"
     * is a reserved keyword.
     *
     * @see #ABSTRACT_PROPERTY
     * @see #getAbstract()
     */
    private CharSequence description;

    /**
     * Whether this item should be shown on the map.
     *
     * @see #VISIBLE_PROPERTY
     * @see #isVisible()
     */
    private boolean visible;

    /**
     * Additional user defined properties, created when first requested.
     *
     * @see #getUserProperties()
     */
    private Map<String,Object> userMap;

    /**
     * Only used by classes in this package.
     */
    MapItem() {
        visible = true;
    }

    /**
     * Returns the identifier of this map item. The identifier can be any character string at developer choice;
     * there is currently no restriction on identifier syntax and no restriction about identifier uniqueness.
     * That identifier is currently not used by Apache SIS; it is made available as a user convenience for
     * referencing {@code MapItem} instances externally.
     *
     * <p>NOTE: restriction about identifier syntax and uniqueness may be added in a future version.</p>
     *
     * @return identifier, or {@code null} if none.
     *
     * @see #IDENTIFIER_PROPERTY
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Sets a new identifier for this map item. If this method is never invoked, the default value is {@code null}.
     * If the given value is different than the previous value, then a change event is sent to all listeners
     * registered for the {@value #IDENTIFIER_PROPERTY} property.
     *
     * @param  newValue  the new identifier, or {@code null} if none.
     */
    public void setIdentifier(final String newValue) {
        final String oldValue = identifier;
        if (!Objects.equals(oldValue, newValue)) {
            identifier = newValue;
            firePropertyChange(IDENTIFIER_PROPERTY, oldValue, newValue);
        }
    }

    /**
     * Returns a human-readable short description for pick lists.
     * This title should be user friendly and may be a {@link String} or {@link InternationalString} instance.
     * It shall not be used as an identifier.
     *
     * @return a short description to be shown to the user, or {@code null} if none.
     *
     * @see #TITLE_PROPERTY
     */
    public CharSequence getTitle() {
        return title;
    }

    /**
     * Sets a new human-readable short description for pick lists. If this method is never invoked,
     * the default value is {@code null}. If the given value is different than the previous value,
     * then a change event is sent to all listeners registered for the {@value #TITLE_PROPERTY} property.
     *
     * @param  newValue  a short description to be shown to the user, or {@code null} if none.
     */
    public void setTitle(final CharSequence newValue) {
        final CharSequence oldValue = title;
        if (!Objects.equals(oldValue, newValue)) {
            title = newValue;
            firePropertyChange(TITLE_PROPERTY, oldValue, newValue);
        }
    }

    /**
     * Returns a narrative description providing additional information.
     * The abstract is more detailed than the {@linkplain #getTitle() title} property and may be a few paragraphs long.
     * This abstract should be user friendly and may be a {@link String} or {@link InternationalString} instance.
     *
     * @return narrative description to be shown to the user, or {@code null} if none.
     *
     * @see #ABSTRACT_PROPERTY
     */
    public CharSequence getAbstract() {
        return description;
    }

    /**
     * Sets a new a narrative description providing additional information. If this method is never invoked,
     * the default value is {@code null}. If the given value is different than the previous value, then
     * a change event is sent to all listeners registered for the {@value #ABSTRACT_PROPERTY} property.
     *
     * @param  newValue  a narrative description to be shown to the user, or {@code null} if none.
     */
    public void setAbstract(final CharSequence newValue) {
        final CharSequence oldValue = description;
        if (!Objects.equals(oldValue, newValue)) {
            description = newValue;
            firePropertyChange(ABSTRACT_PROPERTY, oldValue, newValue);
        }
    }

    /**
     * Returns whether this item should be shown on the map. If this item is a {@code MapGroup},
     * then a {@code false} visibility status implies that all group components are also hidden.
     *
     * @return {@code true} if this item is visible.
     *
     * @see #VISIBLE_PROPERTY
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Sets whether this item should be shown on the map.
     * If this method is never invoked, the default value is {@code true}.
     * If the given value is different than the previous value, then a change event
     * is sent to all listeners registered for the {@value #VISIBLE_PROPERTY} property.
     *
     * <p>If this item is a {@code MapLayers}, then hiding this group should hide all components in this group,
     * but without changing the individual {@value #VISIBLE_PROPERTY} property of those components.
     * Consequently making the group visible again restore each component to the visibility state
     * it has before the group was hidden (assuming those states have not been changed in other ways).</p>
     *
     * @param  newValue  {@code false} to hide this item and all it's components.
     */
    public void setVisible(final boolean newValue) {
        final boolean oldValue = visible;
        if (oldValue != newValue) {
            visible = newValue;
            firePropertyChange(VISIBLE_PROPERTY, oldValue, newValue);
        }
    }

    /**
     * Returns the envelope of this {@code MapItem}.
     * If this instance is a {@code MapLayers} the envelope is the concatenation of all it's components,
     * in case of multiple CRS for each MapLayer, the resulting envelope CRS is unpredictable.
     * If this instance is a {@code MapLayer} the envelope is the resource data envelope.
     *
     * @return the spatiotemporal extent. May be absent if none or too costly to compute.
     * @throws DataStoreException if an error occurred while reading or computing the envelope.
     */
    public Optional<Envelope> getEnvelope() throws DataStoreException {
        return Optional.empty();
    }

    /**
     * Returns a modifiable map of user properties.
     * The content of this map is left to users; Apache SIS does not use it in any way.
     * This map is not thread-safe; synchronization if desired is user responsibility.
     *
     * @return map of user properties. This map is live: changes in this map
     *         are immediately reflected in this {@code MapItem}.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Map<String,Object> getUserProperties() {
        if (userMap == null) {
            userMap = new HashMap<>();
        }
        return userMap;
    }
}
