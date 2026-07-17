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

/**
 * Data region description.
 *
 * Note : this class is in early stage and may evolve or be renamed.
 *
 * @author Johann Sorel (Geomatys)
 * @version 2.0
 * @since   2.0
 */
public abstract class TileMatrixStatus {

    /**
     * Query the matrix for a list of extents where tiles exist.
     * <ul>
     *   <li>All tiles in the matrix are guaranted to be included in those extents.</li>
     *   <li>The returned extents may contain missing tiles.</li>
     *   <li>An empty stream means the tilematrix is guaranted to be empty.</li>
     *   <li>An empty optional means the information is too expensive to compute or is not available.
     *      Therefor no assumption should be made and tiles may be anywhere on the TileMatrix.
     *   </li>
     * </ul>
     * @return stream of extent where tiles exists, empty optional if the information is too expensive to compute
     *        or is not available.
     */
    public abstract Stream<GridExtent> toGridExtents() throws DataStoreException;
}
