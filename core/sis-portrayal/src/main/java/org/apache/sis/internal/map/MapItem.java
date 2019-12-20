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
        this.title = title;
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
     * If this item is a {@link MapGroup}, then hiding this group should hide all components in this group.
     *
     * @param visible {@code false} to hide this item and all it's components.
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
