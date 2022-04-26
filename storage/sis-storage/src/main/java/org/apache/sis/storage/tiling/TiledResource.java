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
import org.apache.sis.storage.Resource;


/**
 * A resource which content can be accessed by smaller chunks called tiles.
 * The use of {@code TiledResource} is not strictly necessary for efficient data loading because
 * {@code Resource} implementations should automatically take advantage of tiling when answering requests.
 * However clients may use this information for optimizing their loading strategy.
 *
 * <p>A {@code TiledResource} may contain multiple {@link TileMatrixSet} instances,
 * each one for a different {@link org.opengis.referencing.crs.CoordinateReferenceSystem}.
 * Most format specifications only support a single {@link TileMatrixSet},
 * but a few ones like WMTS may have several.</p>
 *
 * <p>All methods in this interface return non-null values.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
public interface TiledResource extends Resource {
    /**
     * Returns the collection of all available tile matrix sets in this resource.
     * The returned collection typically contains exactly one instance.
     *
     * @return all available {@link TileMatrixSet} instances, or an empty collection if none.
     * @throws DataStoreException if an error occurred while fetching the tile matrix sets.
     */
    Collection<? extends TileMatrixSet> getTileMatrixSets() throws DataStoreException;
}
