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
 *
 * <p>
 * NOTE : this class is a first draft subject to modifications.
 * </p>
 * 
 * @author Johann Sorel (Geomatys)
 * @since 1.0
 * @module
 */
public abstract class MapItem {

    private CharSequence title;
    private boolean visible = true;

    /**
     * Only used by classes in this package
     */
    MapItem() {

    }

    /**
     * Returns the title of this map item.
     *
     * This title should be user friendly and may be a InternationalString.
     * It must not be used as an identifier.
     *
     * @return user friendly title, may be null
     */
    public CharSequence getTitle() {
        return title;
    }

    /**
     * Set map item title.
     *
     * @param title user friendly title, can be null
     */
    public void setTitle(CharSequence title) {
        this.title = title;
    }

    /**
     * Return visibility state of this map item.
     *
     * @return true if item is visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Set visibility state of this item.
     * In the case of a MapGroup, all components should be hidden too when
     * rendering.
     *
     * @param visible set to false to hide item and all it's components
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

}
