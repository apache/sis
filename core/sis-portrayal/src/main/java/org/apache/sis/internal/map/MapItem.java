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

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Objects;
import java.util.ConcurrentModificationException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.opengis.util.InternationalString;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;


/**
 * Parent class of all map elements.
 * This base class does not make any assumption about how this {@code MapItem} will be rendered;
 * the actual feature or coverage data, together with styling information, are provided by subclasses.
 * A {@code MapItem} contains the following properties:
 *
 * <ul>
 *   <li>An {@linkplain #getIdentifier() identifier}, which can be any {@link String} at developer choice.</li>
 *   <li>A human-readable {@linkplain #getTitle() title} for pick lists, for example in GUI.</li>
 *   <li>A {@linkplain #getAbstract() narrative description} providing additional information.</li>
 * </ul>
 *
 * Additional information can be added in a map of {@linkplain #getUserProperties() user properties}.
 *
 * <h2>Synchronization</h2>
 * {@code MapItem} instances are not thread-safe. Synchronization, if desired, is caller responsibility.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public abstract class MapItem {
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
     * The registered listeners for each property, created when first needed.
     *
     * @see #addPropertyChangeListener(String, PropertyChangeListener)
     * @see #removePropertyChangeListener(String, PropertyChangeListener)
     */
    private Map<String,PropertyChangeListener[]> listeners;

    /**
     * Only used by classes in this package.
     */
    MapItem() {
        visible = true;
    }

    /**
     * Returns the identifier of this map item. The identifier can be any character string at developer choice;
     * there is currently no restriction on the identifier form and no restriction about identifier uniqueness.
     * The identifier is currently not used by Apache SIS; it is made available as a user convenience for
     * referencing {@code MapItem} instances externally.
     *
     * <p>NOTE: restriction about identifier form and uniqueness may be added in a future version.</p>
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
     * <p>If this item is a {@code MapGroup}, then hiding this group should hide all components in this group,
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

    /**
     * Register a listener for the property of the given name.
     * The listener will be notified every time that the property of the given name got a new value.
     * The {@code propertyName} can be one of the following values:
     *
     * <ul>
     *   <li>{@value #IDENTIFIER_PROPERTY} — for changes in identifier of this map item.</li>
     *   <li>{@value #TITLE_PROPERTY}      — for changes in human-readable short description.</li>
     *   <li>{@value #ABSTRACT_PROPERTY}   — for changes in narrative description.</li>
     *   <li>{@value #VISIBLE_PROPERTY}    — for changes in visibility state.</li>
     *   <li>Any other property defined by subclasses.</li>
     * </ul>
     *
     * If the same listener is registered twice for the same property, then it will be notified twice
     * (this method does not perform duplication checks).
     *
     * @param  propertyName  name of the property to listen.
     * @param  listener      property listener to register.
     */
    public final void addPropertyChangeListener(final String propertyName, final PropertyChangeListener listener) {
        ArgumentChecks.ensureNonEmpty("propertyName", propertyName);
        ArgumentChecks.ensureNonNull("listener", listener);
        if (listeners == null) {
            listeners = new HashMap<>(4);       // Assume few properties will be listened.
        }
        final PropertyChangeListener[] oldList = listeners.get(propertyName);
        final PropertyChangeListener[] newList;
        final int n;
        if (oldList != null) {
            n = oldList.length;
            newList = Arrays.copyOf(oldList, n+1);
        } else {
            n = 0;
            newList = new PropertyChangeListener[1];
        }
        newList[n] = listener;
        if (!listeners.replace(propertyName, oldList, newList)) {
            // Opportunistic safety against some multi-threading misuse.
            throw new ConcurrentModificationException();
        }
    }

    /**
     * Unregister a property listener. The given {@code propertyName} can be any of the name documented in
     * {@link #addPropertyChangeListener(String, PropertyChangeListener)}. If the specified listener is not
     * registered for the specified property, then nothing happen. If the listener has been registered twice,
     * then only one registration is removed (one registration will remain).
     *
     * @param  propertyName  name of the listened property.
     * @param  listener      property listener to unregister.
     */
    public final void removePropertyChangeListener(final String propertyName, final PropertyChangeListener listener) {
        ArgumentChecks.ensureNonEmpty("propertyName", propertyName);
        ArgumentChecks.ensureNonNull("listener", listener);
        if (listeners != null) {
            final PropertyChangeListener[] oldList = listeners.get(propertyName);
            if (oldList != null) {
                for (int i=oldList.length; --i >= 0;) {
                    if (oldList[i] == listener) {
                        if (oldList.length != 1) {
                            final PropertyChangeListener[] newList = ArraysExt.remove(oldList, i, 1);
                            if (listeners.replace(propertyName, oldList, newList)) {
                                return;
                            }
                        } else if (listeners.remove(propertyName, oldList)) {
                            return;
                        }
                        // Opportunistic safety against some multi-threading misuse.
                        throw new ConcurrentModificationException();
                    }
                }
            }
        }
    }

    /**
     * Notifies all registered listener that a property of the given name changed its value.
     * It is caller responsibility to verify that the old and new values are not equal
     * (this method does not verify).
     *
     * @param  propertyName  name of the property that changed its value.
     * @param  oldValue      the old property value (may be {@code null}).
     * @param  newValue      the new property value (may be {@code null}).
     */
    protected void firePropertyChange(final String propertyName, final Object oldValue, final Object newValue) {
        if (listeners != null) {
            final PropertyChangeListener[] list = listeners.get(propertyName);
            if (list != null) {
                final PropertyChangeEvent event = new PropertyChangeEvent(this, propertyName, oldValue, newValue);
                for (final PropertyChangeListener listener : list) {
                    listener.propertyChange(event);
                }
            }
        }
    }
}
