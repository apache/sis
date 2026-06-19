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
package org.apache.sis.storage.base;

import java.awt.image.RenderedImage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.tiling.TileMatrix;
import org.apache.sis.storage.tiling.TileMatrixSet;


/**
 * Helper classes for writing a tiled resource.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class WritableTiledResourceSupport implements OverviewIterator {
    /**
     * The resource from which to read get the data to write.
     */
    private final GridCoverageResource resource;

    /**
     * Grid geometry of the level with finest resolution.
     */
    private final GridGeometry baseGrid;

    /**
     * Tile matrices from coarser resolution (highest scale denominator)
     * to most detailed resolution (lowest scale denominator).
     */
    private final TileMatrix[] matrices;

    /**
     * Index of the next level to return. We traverse {@link #matrices} in reverse order,
     * from finest resolution to coarsest resolution and ignoring the finest resolution.
     */
    private int index;

    /**
     * Creates a new helper class for writing the given tile matrix set.
     *
     * @param  resource  the resource from which to read the levels.
     * @param  tiling    the tile matrix to write.
     * @throws DataStoreException if the resource cannot be read.
     */
    public WritableTiledResourceSupport(final GridCoverageResource resource, final TileMatrixSet tiling) throws DataStoreException {
        this.resource = resource;
        this.baseGrid = resource.getGridGeometry();
        this.matrices = tiling.getTileMatrices().values().toArray(TileMatrix[]::new);
        index = matrices.length - 1;
    }

    /**
     * Returns the next overview level, from finest resolution to coarsest resolution.
     *
     * @param  previous  ignored.
     * @return the next overview level, or {@code null} if the iteration is finished.
     * @throws DataStoreException if the resource cannot be read.
     */
    @Override
    public final RenderedImage nextOverview(final RenderedImage previous) throws DataStoreException {
        if (--index >= 0) {
            final TileMatrix matrix = matrices[index];
            final GridGeometry domain = baseGrid.derive().subgrid(null, matrix.getResolution()).build();
            return resource.read(domain, (int[]) null).render(null);
        }
        return null;
    }
}
