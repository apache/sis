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
package org.apache.sis.storage.tiling;

import java.util.stream.Stream;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.IncompatibleResourceException;
import org.apache.sis.storage.ReadOnlyStorageException;


/**
 * A {@code TileMatrix} that can write and delete tiles.
 *
 * <p>All methods in this interface expect non-null arguments.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
public interface WritableTileMatrix extends TileMatrix {
    /**
     * Writes a stream of tiles. The caller must ensure that all tiles are compatible
     * with the {@linkplain #getTilingScheme() tiling scheme} of this tile matrix set.
     * If a tile already exists, it will be overwritten.
     *
     * @param  tiles  the tiles to write.
     * @throws ReadOnlyStorageException if this tile matrix is not writable. It may be caused by insufficient credentials.
     * @throws IncompatibleResourceException if a tile is not compatible with the tiling scheme of this tile matrix.
     * @throws DataStoreException if writing the tiles failed for another reason.
     */
    void writeTiles(Stream<Tile> tiles) throws DataStoreException;

    /**
     * Deletes all existing tiles in the given region.
     * After this method call, the status of all tiles in the given region become {@link TileStatus#MISSING}.
     * Tiles that were already missing are silently ignored.
     *
     * @param  indicesRanges  ranges of tile indices in all dimensions, or {@code null} for all tiles.
     * @return number of tiles deleted (i.e. not counting the tiles that were already missing).
     * @throws ReadOnlyStorageException if this tile matrix is not writable. It may be caused by insufficient credentials.
     * @throws DataStoreException if deleting the tile failed for another reason.
     */
    long deleteTiles(GridExtent indicesRanges) throws DataStoreException;
}
