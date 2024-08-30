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
package org.apache.sis.storage.gimi.internal;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.util.logging.Logger;
import org.opengis.util.GenericName;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
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
public abstract class MatrixGridRessource extends TiledGridResource {

    private int[] tileSize;
    private SampleModel sampleModel;
    private ColorModel colorModel;

    protected static final Logger LOGGER = Logger.getLogger("org.geotoolkit.storage.coverage");

    public MatrixGridRessource() {
        super(null);
    }

    protected abstract TileMatrix getTileMatrix();

    private void initialize() throws DataStoreException {
        if (sampleModel != null) return;
        final RenderedImage image = getTileImage(0,0);
        colorModel = image.getColorModel();
        sampleModel = image.getSampleModel();
        tileSize = new int[]{image.getWidth(), image.getHeight()};
    }

    @Override
    protected int[] getTileSize() {
        try {
            initialize();
        } catch (DataStoreException ex) {
            throw new RuntimeException(ex);
        }
        return tileSize.clone();
    }

    @Override
    protected SampleModel getSampleModel() throws DataStoreException {
        initialize();
        return sampleModel;
    }

    @Override
    protected ColorModel getColorModel() throws DataStoreException {
        initialize();
        return colorModel;
    }

    @Override
    protected Number getFillValue() throws DataStoreException {
        return Double.NaN;
    }

    @Override
    public GridGeometry getGridGeometry() throws DataStoreException {
        return getTileMatrix().getTilingScheme().upsample(getTileSize());
    }

    @Override
    public GridCoverage read(GridGeometry domain, int ... range) throws DataStoreException {
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
        protected Raster[] readTiles(AOI iterator) throws IOException, DataStoreException {
            final Raster[] result = new Raster[iterator.tileCountInQuery];
            synchronized (MatrixGridRessource.this.getSynchronizationLock()) {
                do {
                    final Raster tile = iterator.getCachedTile();
                    if (tile != null) {
                        result[iterator.getIndexInResultArray()] = tile;
                    } else {
                        long[] tileCoord = iterator.getPositionInSource();
                        final RenderedImage image = getTileImage(tileCoord);
                        result[iterator.getIndexInResultArray()] = image instanceof BufferedImage ? ((BufferedImage)image).getRaster() : image.getData();
                    }
                } while (iterator.next());
            }
            return result;
        }

    }

}
