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
 * A map group is collection of layers.
 *
 * <p>
 * Groups are used in map contexts to regroup similar layers under a same node.
 * This allows global actions, like hiding background layers in one call.
 * </p>
 *
 * <p>
 * NOTE : this class is a first draft subject to modifications.
 * </p>
 *
 * @author Johann Sorel (Geomatys)
 * @since 1.0
 * @module
 */
public class MapGroup extends MapItem {

    private final List<MapItem> components = new ArrayList<>();

    /**
     * Get the list of layers contained in this group.
     *
     * <p>
     * The layers in the list are presented in rendering order.
     * This means the first rendered layer which will be under all others on the
     * result map is at index zero.
     * </p>
     *
     * <p>
     * The returned list is modifiable.
     * </p>
     *
     * @return List of layers, never null
     */
    public List<MapItem> getComponents() {
        return components;
    }

}
