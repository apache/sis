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
package org.apache.sis.map.service.common;

import java.awt.Graphics2D;
import java.io.IOException;
import java.util.List;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.map.service.RenderingException;
import org.apache.sis.storage.DataStoreException;


/**
 * A rendering to validate visually in a tile of a {@link ValidationSheet}.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface ValidationCase {
    /**
     * Returns a short description of what is validated, written below the tile.
     *
     * @return the description of this case.
     */
    String description();

    /**
     * Paints this case in a transparent tile.
     * The source geometries should be drawn on top of the rendering in magenta.
     *
     * @param  graphics  where to paint, in the display coordinates of the grid.
     * @param  grid      the rendering grid of the tile.
     * @param  problems  where to add the warnings, written in red below the tile.
     * @throws IOException if a resource cannot be read.
     * @throws DataStoreException if the data cannot be read.
     * @throws RenderingException if the rendering failed.
     */
    void paint(Graphics2D graphics, GridGeometry grid, List<String> problems)
            throws IOException, DataStoreException, RenderingException;
}
