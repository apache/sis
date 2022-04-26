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

import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.Resource;


/**
 * A small hyper-rectangular representation of data which is part of a tiling scheme.
 * A tile is uniquely defined in a tile matrix by an integer index in each dimension.
 * Tiles can be a coverage subsets, or a feature based representation (e.g. vector tiles).
 *
 * <p>All methods in this interface return non-null values.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 *
 * @see TileMatrix#getTiles(GridExtent, boolean)
 *
 * @since 1.2
 * @module
 */
public interface Tile {
    /**
     * Returns the indices of this tile in the {@code TileMatrix}.
     * If this tile was obtained by a call to {@link TileMatrix#getTile(long...)},
     * then the returned array contains the indices that were given in that call.
     *
     * <p>The returned array contains coordinates in the space defined by
     * the {@linkplain GridGeometry#getExtent() extent} of
     * the {@linkplain TileMatrix#getTilingScheme() tiling scheme}.
     * As such, it complies with the following constraints:</p>
     * <ul>
     *   <li>The array length is equal to {@link GridExtent#getDimension()}.</li>
     *   <li>The axis order — usually (<var>column</var>, <var>row/</var>) — is the
     *       {@linkplain GridExtent#getAxisType(int) extent axis} order.</li>
     *   <li>Values are between the {@linkplain GridExtent#getLow(int) extent low}
     *       and {@linkplain GridExtent#getHigh(int) high} values, inclusive.</li>
     * </ul>
     *
     * @return indices of this tile in the {@link TileMatrix},
     *         as coordinates inside the matrix {@link GridExtent}.
     *
     * @see TileMatrix#getTile(long...)
     */
    long[] getIndices();

    /**
     * Returns information about whether the tile failed to load.
     * The return value can be {@link TileStatus#EXISTS} or {@link TileStatus#IN_ERROR};
     * other enumeration values should not happen after a user successfully obtained this {@code Tile} instance.
     *
     * <h4>State transition</h4>
     * {@link TileStatus#EXISTS} is not a guarantee that a call to {@link #getResource()} will succeed.
     * The error may be detected only during the first attempt to read the resource.
     * Consequently this method may initially return {@code EXISTS},
     * then return {@code IN_ERROR} later after the first read attempt.
     *
     * @return information about the availability of this tile.
     *
     * @see TileMatrix#getTileStatus(long...)
     */
    TileStatus getStatus();

    /**
     * Returns the tile content as a resource.
     * The resource type is typically {@link GridCoverageResource},
     * but it may also be other types (e.g. vector tiles).
     *
     * @return the tile content.
     * @throws DataStoreException if an error occurred while reading the content.
     */
    Resource getResource() throws DataStoreException;
}
