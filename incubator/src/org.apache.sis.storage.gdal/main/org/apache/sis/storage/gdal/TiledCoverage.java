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
package org.apache.sis.storage.gdal;

import java.io.IOException;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import org.opengis.util.GenericName;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.base.TiledGridCoverage;
import org.apache.sis.storage.base.TiledGridResource;


/**
 * Coverage read from {@code TiledResource}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class TiledCoverage extends TiledGridCoverage {
    /**
     * The resource from which this coverage has been read.
     */
    private final TiledResource owner;

    /**
     * Creates a new tiled grid coverage.
     *
     * @param  owner   the resource from which this coverage has been read.
     * @param  subset  description of the {@link TiledGridResource} subset to cover.
     */
    TiledCoverage(final TiledResource owner, final TiledGridResource.Subset subset) {
        super(subset);
        this.owner = owner;
    }

    /**
     * Returns a unique name that identifies this coverage.
     * The name shall be unique in the {@code TileMatrixSet}.
     */
    @Override
    protected GenericName getIdentifier() {
        return owner.getIdentifier().orElse(null);
    }

    /**
     * Returns all tiles in the given area of interest. Tile indices (0,0) locates the tile in the upper-left corner
     * of this {@code TiledGridCoverage} (not necessarily the upper-left corner of the {@link TiledGridResource}).
     * The {@link Raster#getMinX()} and {@code getMinY()} coordinates of returned rasters
     * shall start at the given {@code iterator.offsetAOI} values.
     *
     * @param  iterator  an iterator over the tiles that intersect the Area Of Interest specified by user.
     * @return tiles decoded from the {@link TiledGridResource}.
     */
    @Override
    protected Raster[] readTiles(final AOI iterator) throws IOException, DataStoreException {
        synchronized (owner.getSynchronizationLock()) {
            final var result = new Raster[iterator.tileCountInQuery];
            do {
                final WritableRaster tile = iterator.createRaster();
                final Rectangle bounds = tile.getBounds();
                toFullResolution(bounds);
                owner.transfer(OpenFlag.READ, bounds, tile, includedBands);
                result[iterator.getIndexInResultArray()] = tile;
            } while (iterator.next());
            return result;
        }
    }

    /**
     * Transfers (reads or writes) sample values between <abbr>GDAL</abbr> raster and Java2D raster for all bands.
     * The full area of the Java2D raster is transferred. It may corresponds to a sub-area of the GDAL raster.
     *
     * <h4>Prerequisites</h4>
     * <ul>
     *   <li>The Java2D raster shall use a {@link java.awt.image.ComponentSampleModel}.</li>
     *   <li>In read mode, the given raster shall be an instance of {@link WritableRaster}.</li>
     * </ul>
     *
     * @param  gdal    set of handles for invoking <abbr>GDAL</abbr> functions.
     * @param  rwFlag  {@link OpenFlag#READ} or {@link OpenFlag#WRITE}.
     * @param  aoi     region of the image to read or write. (0,0) is the upper-left pixel.
     * @param  raster  the Java2D raster where to store of fetch the values to read or write.
     * @throws ClassCastException if a prerequisite about expected classes is not true.
     * @throws DataStoreException if <var>GDAL</var> reported a warning or fatal error.
     */
    private void transfer(final GDAL gdal, final int rwFlag, final Rectangle aoi, final Raster raster)
            throws DataStoreException
    {

    }
}
