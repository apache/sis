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
package org.apache.sis.storage.geoheif;

import java.util.Arrays;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.tiling.Tile;
import org.apache.sis.storage.tiling.TileStatus;


/**
 * A tile item resource defined by a Grid Item.
 *
 * @author Johann Sorel (Geomatys)
 */
final class GimiTile implements Tile {

    private final GeoHeifStore store;
    private final long[] indices;
    private final int itemId;

    public GimiTile(GeoHeifStore store, long[] indices, int itemId) {
        this.store = store;
        this.indices = indices;
        this.itemId = itemId;
    }

    @Override
    public long[] getIndices() {
        return indices.clone();
    }

    @Override
    public TileStatus getStatus() {
        try {
            return store.getComponent(itemId) != null ? TileStatus.EXISTS : TileStatus.MISSING;
        } catch (DataStoreException ex) {
            return TileStatus.UNKNOWN;
        }
    }

    @Override
    public Resource getResource() throws DataStoreException {
        final Resource res = store.getComponent(itemId);
        if (res == null) {
            throw new DataStoreContentException("Missing tile at " + Arrays.toString(indices) + ", it should not be possible with GIMI model");
        }
        return res;
    }

}
