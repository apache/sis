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

import java.io.IOException;


/**
 * Information about the availability of a tile. Some {@link TileMatrix} implementations
 * may not know whether a tile exists or not before the first attempt to read that tile.
 * Consequently a tile status may be initially {@link #UNKNOWN} and transitions
 * at a later time to a state such as {@link #EXISTS}, {@link #MISSING} or {@link #IN_ERROR}.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 *
 * @see Tile#getStatus()
 * @see TileMatrix#getTileStatus(long...)
 *
 * @since 1.2
 * @module
 */
public enum TileStatus {
    /**
     * The tile status can not be known unless the tile is read. This value is returned
     * by some {@link TileMatrix} implementations when determining the availability of
     * a tile would require relatively costly I/O operations.
     */
    UNKNOWN,

    /**
     * The tile exists. However this is not a guarantee that no I/O error will happen when reading the tile,
     * neither that the tile will be non-empty. If an I/O error happens at tile reading time,
     * then the tile status should transition from {@code EXISTS} to {@link #IN_ERROR}.
     */
    EXISTS,

    /**
     * The tile is flagged as missing. It may happen in regions where no data is available.
     */
    MISSING,

    /**
     * The tile for which a status has been requested is outside the {@link TileMatrix} extent.
     */
    OUTSIDE_EXTENT,

    /**
     * The tile exists but attempt to read it failed.
     * It may be because an {@link IOException} occurred while reading the tile.
     */
    IN_ERROR
}
