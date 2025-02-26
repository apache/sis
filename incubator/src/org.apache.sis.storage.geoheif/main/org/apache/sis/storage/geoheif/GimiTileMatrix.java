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

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import org.opengis.util.GenericName;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.tiling.Tile;
import org.apache.sis.storage.tiling.TileMatrix;
import org.apache.sis.storage.tiling.TileStatus;
import org.apache.sis.util.iso.Names;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
final class GimiTileMatrix implements TileMatrix {

    private final ImageResource grid;
    private final GenericName identifier;
    private final GridGeometry tilingScheme;
    private final long[] tileSize;

    public GimiTileMatrix(ImageResource grid, GridGeometry tilingScheme, long[] tileSize) {
        this.grid = grid;
        this.identifier = Names.createLocalName(null, null, grid.getIdentifier().get().tip().toString() + "_tm");
        this.tilingScheme = tilingScheme;
        this.tileSize = tileSize;
    }

    @Override
    public GenericName getIdentifier() {
        return identifier;
    }

    @Override
    public double[] getResolution() {
        double[] resolution = tilingScheme.getResolution(true);
        resolution[0] /= tileSize[0];
        resolution[1] /= tileSize[1];
        return resolution;
    }

    @Override
    public GridGeometry getTilingScheme() {
        return tilingScheme;
    }

    @Override
    public TileStatus getTileStatus(long... indices) throws DataStoreException {
        return tilingScheme.getExtent().contains(indices) ? TileStatus.EXISTS : TileStatus.OUTSIDE_EXTENT;
    }

    @Override
    public Optional<Tile> getTile(long... indices) throws DataStoreException {
        final int itemIdx = Math.toIntExact(indices[0] + indices[1] * tilingScheme.getExtent().getSize(0));
        return Optional.of(new GimiTile(grid.getItem().store, indices, grid.getItem().getReferences().get(0).toItemId[itemIdx]));
    }

    @Override
    public Stream<Tile> getTiles(GridExtent indicesRanges, boolean parallel) throws DataStoreException {
        Stream<long[]> stream = TileMatrices.pointStream(indicesRanges);
        stream = parallel ? stream.parallel() : stream.sequential();
        return stream.map(new Function<long[], Tile>() {
            @Override
            public Tile apply(final long[] indices) {
                try {
                    return getTile(indices).orElse(null);
                } catch (DataStoreException ex) {
                    return new Tile() {
                        @Override
                        public long[] getIndices() {
                            return indices;
                        }

                        @Override
                        public TileStatus getStatus() {
                            return TileStatus.IN_ERROR;
                        }

                        @Override
                        public Resource getResource() throws DataStoreException {
                            throw ex;
                        }
                    };
                }
            }
        });
    }

    @Override
    public String toString() {
        return identifier + "\n" + getTilingScheme().getExtent().toString();
    }

}
