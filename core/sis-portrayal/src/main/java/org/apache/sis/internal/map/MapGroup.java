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


/**
 * A collection of layers.
 * Groups are used in map contexts to regroup similar layers under a same node.
 * This allows global actions, like {@linkplain #setVisible(boolean) hiding}
 * background layers in one call.
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
public class MapGroup extends MapItem {
    /**
     * The components in this group.
     */
    private final List<MapItem> components;

    /**
     * Creates an initially empty group.
     */
    public MapGroup() {
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
}
