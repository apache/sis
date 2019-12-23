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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;
import javax.swing.event.EventListenerList;

/**
 * Parent class of all map elements.
 *
 * <p>
 * NOTE: this class is a first draft subject to modifications.
 * </p>
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public abstract class MapItem {

    /** Identifies a change in the map item title. */
    public static final String TITLE_PROPERTY = "title";
    /** Identifies a change in the map item visibility state. */
    public static final String VISIBLE_PROPERTY = "visible";

    private final EventListenerList listeners = new EventListenerList();

    /**
     * The title of this map item, for display to the user.
     */
    private CharSequence title;

    /**
     * Whether this item should be shown on the map.
     */
    private boolean visible = true;

    /**
     * Only used by classes in this package.
     */
    MapItem() {
    }

    /**
     * Returns the title of this map item.
     * This title should be user friendly and may be an {@link org.opengis.util.InternationalString}.
     * It shall not be used as an identifier.
     *
     * @return title to be shown to the user, or {@code null} if none.
     */
    public CharSequence getTitle() {
        return title;
    }

    /**
     * Sets a new title for this map item.
     *
     * @param  title  title to be shown to the user, or {@code null} if none.
     */
    public void setTitle(CharSequence title) {
        if (!Objects.equals(this.title, title)) {
            CharSequence old = this.title;
            this.title = title;
            firePropertyChange(TITLE_PROPERTY, old, title);
        }
    }

    /**
     * Return whether this item should be shown on the map.
     *
     * @return {@code true} if this item is visible.
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Sets whether this item should be shown on the map.
     * If this item is a {@code MapGroup}, then hiding this group should hide all components in this group.
     *
     * @param visible {@code false} to hide this item and all it's components.
     */
    public void setVisible(boolean visible) {
        if (this.visible != visible) {
            this.visible = visible;
            firePropertyChange(VISIBLE_PROPERTY, !visible, visible);
        }
    }

    /**
     * Register a property listener.
     *
     * @param listener property listener to register
     */
    public final void addPropertyChangeListener(PropertyChangeListener listener) {
        listeners.add(PropertyChangeListener.class, listener);
    }

    /**
     * Unregister a property listener.
     *
     * @param listener property listener to register
     */
    public final void removePropertyChangeListener(PropertyChangeListener listener) {
        listeners.remove(PropertyChangeListener.class, listener);
    }

    protected void firePropertyChange(final String propertyName, final Object oldValue, final Object newValue) {
        final PropertyChangeListener[] listPs = listeners.getListeners(PropertyChangeListener.class);
        if (listPs.length == 0) return;

        final PropertyChangeEvent event = new PropertyChangeEvent(this,propertyName,oldValue,newValue);
        for (PropertyChangeListener listener : listPs) {
            listener.propertyChange(event);
        }
    }
}
