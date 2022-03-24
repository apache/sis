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

import java.util.Collection;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.IncompatibleResourceException;
import org.apache.sis.storage.NoSuchDataException;
import org.apache.sis.storage.ReadOnlyStorageException;


/**
 * A {@code TiledResource} that can write and delete tile matrix sets.
 *
 * <p>All methods in this interface expect non-null arguments are return non-null values.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
public interface WritableTiledResource extends TiledResource {
    /**
     * Returns the collection of all available tile matrix sets in this resource.
     * The returned collection is unmodifiable but live: additions or removals of
     * tile matrix sets in this resource are reflected in the returned collection.
     *
     * @return an unmodifiable view of all {@link TileMatrixSet} instances in this resource.
     * @throws DataStoreException if an error occurred while fetching the tile matrix sets.
     */
    @Override
    Collection<? extends WritableTileMatrixSet> getTileMatrixSets() throws DataStoreException;

    /**
     * Adds the given tile matrix set to this resource and returns a writable instance for later completion.
     * Typically the given {@link TileMatrixSet} instance contains no tile and is used only as a template.
     * If the {@code TileMatrixSet} is not empty, then the tiles that it contains are written immediately.
     *
     * <p>This method returns a writable tile matrix set with the same tiles than the given {@code TileMatrixSet}.
     * The identifier and the envelope of the returned set may be different, but the CRS and tiling scheme shall
     * be equivalent with a tolerance for rounding errors.</p>
     *
     * @param  tiles  the (potentially empty) tile matrix set to create.
     * @return a writable tile matrix set to use for adding more tiles.
     * @throws ReadOnlyStorageException if this resource is not writable. It may be caused by insufficient credentials.
     * @throws IncompatibleResourceException if the given tile matrix set is incompatible with this resource.
     * @throws DataStoreException if creating the tile matrix set failed for another reason.
     */
    WritableTileMatrixSet createTileMatrixSet(TileMatrixSet tiles) throws DataStoreException;

    /**
     * Deletes a {@code TileMatrixSet} identified by the given name. The given identifier shall be the
     * <code>{@linkplain TileMatrixSet#getIdentifier()}.toString()</code> value of the set to delete.
     *
     * @param  identifier  identifier of the {@link TileMatrixSet} to delete.
     * @throws NoSuchDataException if there is no tile matrix set associated to the given identifier in this resource.
     * @throws ReadOnlyStorageException if this resource is not writable. It may be caused by insufficient credentials.
     * @throws DataStoreException if deleting the tile matrix set failed for another reason.
     */
    void deleteTileMatrixSet(String identifier) throws DataStoreException;
}
