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
package org.apache.sis.gui.map.style;

import org.apache.sis.storage.Resource;


/**
 * Placeholder for {@code org.apache.sis.map.MapLayer}.
 * We use this temporary class because {@code org.apache.sis.map.MapLayer} is in incubator.
 *
 * @todo Replace by {@link org.apache.sis.map.MapLayer}.
 */
public final class MapLayer<R extends Resource> extends MapItem {
    /**
     * The resource managed by this map layer.
     */
    public final R resource;

    /**
     * Creates a new map item with the given resource.
     *
     * @param  resource  the resource managed by this map layer.
     */
    public MapLayer(final R resource, final String title) {
        super(title);
        this.resource = resource;
    }
}
