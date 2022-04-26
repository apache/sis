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

import java.util.Optional;
import java.util.stream.Stream;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.NoSuchDataException;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.util.GenericName;


/**
 * A collection of tiles with the same size and properties placed on a regular grid with no overlapping.
 * A tile matrix usually has 2 dimensions (width and height), but this API allows any number of dimensions.
 * The number of dimensions is given by {@code getTilingScheme().getDimension()}.
 *
 * <p>Unless otherwise specified in the Javadoc,
 * all methods in this interface expect non-null arguments are return non-null values.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
public interface TileMatrix {
    /**
     * Returns an alphanumeric identifier which is unique in the {@link TileMatrixSet} that contains
     * this {@code TileMatrix}. The identifier is often a zoom level (as a number encoded in ASCII),
     * but this is not mandatory.
     *
     * @return a unique (within {@link TileMatrixSet}) identifier.
     */
    GenericName getIdentifier();

    /*
     * There is no `getTileSize()` method because tiles are not necessarily for grid coverages.
     * Tile size do not apply to vector tiles, quantized mesh or 3D tiles.
     */

    /**
     * Returns the resolution (in units of CRS axes) at which tiles in this matrix should be used.
     * If the tiled data is a {@link org.apache.sis.coverage.grid.GridCoverage},
     * then the resolution is typically the size of pixels in units of CRS axes.
     * That resolution may be evaluated at some representative point such as coverage center
     * if the pixel size is not constant everywhere.
     *
     * <p>The array length shall be the number of CRS dimensions, and value at index <var>i</var>
     * is the resolution along CRS dimension <var>i</var> in units of the CRS axis <var>i</var>.</p>
     *
     * @return resolution (in units of CRS axes) at which tiles in this matrix should be used.
     *
     * @see GridGeometry#getResolution(boolean)
     */
    double[] getResolution();

    /**
     * Returns a description about how space is partitioned into individual tiled units.
     * The description contains the extent of valid tile indices, the spatial reference system,
     * and the conversion from tile indices to the spatial reference system coordinates.
     * The CRS <em>shall</em> be the same as {@link TileMatrixSet#getCoordinateReferenceSystem()}.
     * The "grid to CRS" transform <em>should</em> be defined and <em>should</em> be affine.
     * The grid geometry <em>shall</em> have a {@link GridExtent} which gives the range of valid indices
     * that can be used in calls to {@link #getTile(long...)} and {@link #getTileStatus(long...)} methods.
     *
     * <p>The "grid to CRS" transform converts tile indices to "real world" coordinates.
     * This conversion can follow two conventions:</p>
     *
     * <ul class="verbose">
     *   <li>The {@link PixelInCell#CELL_CORNER} convention maps tile indices to the extreme corner
     *       (in the direction of smallest indices) of the bounding box of the tile.
     *       In a two-dimensional space having the usual display axis orientations,
     *       this is the top-left corner of the top-left pixel.</li>
     *   <li>The {@link PixelInCell#CELL_CENTER} convention maps tile indices to the median value
     *       of the tile bounding box in all dimensions.</li>
     * </ul>
     *
     * <h4>Relationship with OGC specification</h4>
     * OGC has a more generic definition of <cite>tiling scheme</cite>,
     * where the scheme specifies which space a uniquely identified tile occupies.
     * Reversely, the tiling scheme makes possible to find which unique identifier
     * corresponds to a space satisfying the geometric properties to be a tile.
     * In {@code TileMatrix}, the unique identifier of a tile is the sequence of
     * tile indices stored in a {@code long[]} array.
     * The space occupied by a tile can be computed by the above-cited "grid to CRS" transform.
     * Reversely the tile indices for a given space can be computed by the inverse of the "grid to CRS" transform.
     *
     * @return extent of valid tile indices (mandatory) and their relationship with "real world" coordinates (optional).
     *
     * @see TileMatrixSet#getCoordinateReferenceSystem()
     */
    GridGeometry getTilingScheme();

    /**
     * Fetches information about whether a tile exists, is missing or failed to load.
     * The accuracy of a tile status greatly varies with each protocol.
     * If the returned value is different than {@link TileStatus#UNKNOWN}, then:
     *
     * <table class="sis">
     * <caption>Relationship between return value and tile fetching behavior}</caption>
     * <tr><th>Return value</th>                      <th>Consequence</th></tr>
     * <tr><td>{@link TileStatus#EXISTS}</td>         <td>{@code getTile(indices)} should return a non-empty value.</td></tr>
     * <tr><td>{@link TileStatus#MISSING}</td>        <td>{@code getTile(indices)} should return an empty value.</td></tr>
     * <tr><td>{@link TileStatus#OUTSIDE_EXTENT}</td> <td>{@code getTile(indices)} should throw {@link NoSuchDataException}.</td></tr>
     * <tr><td>{@link TileStatus#IN_ERROR}</td>       <td>{@code getTile(indices)} should throw {@link DataStoreException} (or a sub-type).</td></tr>
     * </table>
     *
     * @param  indices  indices of the requested tile (may be outside the tile matrix extent).
     * @return information about the availability of the specified tile,
     *         or {@link TileStatus#OUTSIDE_EXTENT} if the given indices are invalid.
     * @throws DataStoreException if fetching the tile status failed.
     *
     * @see Tile#getStatus()
     */
    TileStatus getTileStatus(long... indices) throws DataStoreException;

    /**
     * Gets a tile at the given indices.
     *
     * @param  indices  indices of the tile to fetch, as coordinates inside the matrix {@link GridExtent}.
     * @return the tile if it {@linkplain TileStatus#EXISTS exists},
     *         or an empty value if the tile is {@linkplain TileStatus#MISSING missing}.
     * @throws NoSuchDataException if the given indices are
     *         {@linkplain TileStatus#OUTSIDE_EXTENT outside the matrix extent}.
     * @throws DataStoreException if fetching the tile failed for another reason.
     */
    Optional<Tile> getTile(long... indices) throws DataStoreException;

    /**
     * Retrieves a stream of existing tiles in the specified region. The stream contains
     * the {@linkplain TileStatus#EXISTS existing} tiles that are inside the given region
     * and excludes all {@linkplain TileStatus#MISSING missing} tiles.
     * If a tile is {@linkplain TileStatus#IN_ERROR in error},
     * then the stream should nevertheless return a {@link Tile} instance
     * but its {@link Tile#getResource()} method should throw the exception.
     *
     * <p>The {@code parallel} argument specifies whether a parallelized stream is desired.
     * If {@code false}, the stream is guaranteed to be sequential.
     * If {@code true}, the stream may or may not be parallel;
     * implementations are free to ignore this argument if they do not support parallelism.</p>
     *
     * @param  indicesRanges  ranges of tile indices in all dimensions, or {@code null} for all tiles.
     * @param  parallel  {@code true} for a parallel stream (if supported), or {@code false} for a sequential stream.
     * @return stream of tiles, excluding {@linkplain TileStatus#MISSING missing} tiles.
     *         Iteration order of the stream may vary from one implementation to another and from one call to another.
     * @throws DataStoreException if the stream creation failed.
     */
    Stream<Tile> getTiles(GridExtent indicesRanges, boolean parallel) throws DataStoreException;
}
