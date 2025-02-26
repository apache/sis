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

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.util.logging.Logger;
import org.opengis.util.GenericName;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridRoundingMode;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.base.TiledGridCoverage;
import org.apache.sis.storage.base.TiledGridResource;
import org.apache.sis.storage.tiling.TileMatrix;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
abstract class MatrixGridRessource extends TiledGridResource {

    protected int[] tileSize;
    protected SampleModel sampleModel;
    protected ColorModel colorModel;

    protected static final Logger LOGGER = Logger.getLogger("org.geotoolkit.storage.coverage");

    public MatrixGridRessource() {
        super(null);
    }

    protected abstract TileMatrix getTileMatrix();

    protected void initialize() throws DataStoreException {
        if (sampleModel != null) return;
        final RenderedImage image = getTileImage(0,0);
        colorModel = image.getColorModel();
        sampleModel = image.getSampleModel();
        tileSize = new int[]{image.getWidth(), image.getHeight()};
    }

    @Override
    protected int[] getTileSize() throws DataStoreException {
        initialize();
        return tileSize.clone();
    }

    @Override
    protected SampleModel getSampleModel(int[] bands) throws DataStoreException {
        if (bands != null) {
            return null;
        }
        initialize();
        return sampleModel;
    }

    @Override
    protected ColorModel getColorModel(int[] bands) throws DataStoreException {
        if (bands != null) {
            return null;
        }
        initialize();
        return colorModel;
    }

    @Override
    public GridGeometry getGridGeometry() throws DataStoreException {
        return getTileMatrix().getTilingScheme().upsample(getTileSize());
    }

    @Override
    public GridCoverage read(GridGeometry domain, int ... range) throws DataStoreException {
        if (domain != null) {
            GridGeometry grid = getGridGeometry();
            final GridExtent intersection = grid.derive()
                    .rounding(GridRoundingMode.ENCLOSING)
                    .subgrid(domain).getIntersection();
            domain = grid.derive().subgrid(intersection).build();
        }
        return new MatrixCoverage(new Subset(domain, range));
    }

    private RenderedImage getTileImage(long ... coordinate) throws DataStoreException {
        final Resource resource = getTileMatrix().getTile(coordinate).get().getResource();
        final GridCoverageResource gcr = (GridCoverageResource) resource;
        final GridCoverage coverage = gcr.read(null);
        return coverage.render(null);
    }

    private class MatrixCoverage extends TiledGridCoverage {

        public MatrixCoverage(TiledGridResource.Subset subset) {
            super(subset);
        }

        @Override
        protected GenericName getIdentifier() {
            return MatrixGridRessource.this.getTileMatrix().getIdentifier();
        }

        @Override
        protected Raster[] readTiles(final TileIterator iterator) throws IOException, DataStoreException {
            final Raster[] result = new Raster[iterator.tileCountInQuery];
            synchronized (MatrixGridRessource.this.getSynchronizationLock()) {
                do {
                    final Raster tile = iterator.getCachedTile();
                    if (tile != null) {
                        result[iterator.getTileIndexInResultArray()] = tile;
                    } else {
                        long[] tileCoord = iterator.getTileCoordinatesInResource();
                        final RenderedImage image = getTileImage(tileCoord);
                        var s = new Snapshot(iterator);
                        Raster raster;
                        if (image instanceof BufferedImage) {
                            raster = ((BufferedImage)image).getRaster();
                        } else if (image.getNumXTiles() == 1 && image.getNumYTiles() == 1) {
                            raster = image.getTile(0, 0);
                        } else {
                            raster = image.getData();
                        }
                        raster = raster.createTranslatedChild(s.originX, s.originY);
                        result[iterator.getTileIndexInResultArray()] = iterator.cache(raster);
                    }
                } while (iterator.next());
            }
            return result;
        }

    }

}
