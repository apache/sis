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
package org.apache.sis.gui.coverage;

import java.util.Map;
import java.util.LinkedHashMap;
import org.apache.sis.system.Configuration;
import static org.apache.sis.image.ImageLayout.DEFAULT_TILE_SIZE;


/**
 * A map of tiles with a fixed capacity. When the maximal capacity is exceeded, eldest entries are cleaned.
 * This is a trivial implementation on top of {@link LinkedHashMap} used only for very simple caching.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings({"serial", "CloneableImplementsClone"})               // Not intended to be serialized.
final class GridTileCache extends LinkedHashMap<GridTile,GridTile> {
    /**
     * Cache size in number of tiles.
     */
    @Configuration
    private static final int CACHE_SIZE = 10 * (1024 * 1024) / (DEFAULT_TILE_SIZE * DEFAULT_TILE_SIZE);

    /**
     * Creates a new cache of tiles.
     */
    GridTileCache() {
        super(16, 0.75f, true);
    }

    /**
     * Clears the eldest tile if this map has reached its maximal capacity.
     * In the common case where there is no error, the whole entry will be discarded.
     *
     * @param  entry  the eldest entry.
     * @return whether to remove the entry.
     */
    @Override
    protected boolean removeEldestEntry(final Map.Entry<GridTile,GridTile> entry) {
        if (size() > CACHE_SIZE) {
            return entry.getValue().clearTile();
        }
        return false;
    }
}
