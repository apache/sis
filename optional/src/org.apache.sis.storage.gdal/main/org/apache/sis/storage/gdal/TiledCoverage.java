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
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import org.opengis.util.GenericName;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.base.TiledGridCoverage;
import org.apache.sis.storage.base.TiledGridResource;
import org.apache.sis.image.privy.AssertionMessages;


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
     * Returns the length of tiles in bytes.
     */
    private long getTileLength() {
        return Math.ceilDiv(Math.multiplyFull(model.getWidth(), model.getHeight()) * owner.dataType.numBits, Byte.SIZE);
    }

    /**
     * Returns all tiles in the given area of interest. Tile indices (0,0) locates the tile in the upper-left corner
     * of this {@code TiledGridCoverage} (not necessarily the upper-left corner of the {@link TiledGridResource}).
     * The {@link Raster#getMinX()} and {@code getMinY()} coordinates of returned rasters
     * shall start at the given {@code iterator.offsetAOI} values.
     *
     * @param  iterator  an iterator over the tiles that intersect the Area Of Interest specified by user.
     * @return tiles decoded from the {@link TiledGridResource}.
     * @throws ArithmeticException if an integer overflow occurred.
     */
    @Override
    protected Raster[] readTiles(final TileIterator iterator) throws IOException, DataStoreException {
        Rectangle resourceBounds = bidimensional(iterator.getFullRegionInResourceCoordinates());
        Rectangle imageBounds;
        try {
            imageBounds = iterator.resourceToImage(resourceBounds, false);
        } catch (ArithmeticException e) {
            // Ignore, this is used only as a hint.
            Logging.ignorableException(GDALStoreProvider.LOGGER, GDALStore.class, "read", e);
            imageBounds = null;
        }
        synchronized (owner.getSynchronizationLock()) {
            final Band[]   bands      = owner.bands(includedBands);
            final GDAL     gdal       = owner.parent.getProvider().GDAL();
            final DataType rasterType = owner.dataType.forDataBufferType(model.getDataType());
            if (imageBounds != null) {
                // Give a chance to the GDAL driver to prepare itself for the reading of all tiles in the AOI.
                for (final Band band : bands) {
                    if (!band.adviseRead(gdal, resourceBounds, imageBounds, rasterType)) break;
                }
            }
            final var result = new WritableRaster[iterator.tileCountInQuery];
            try (Arena arena = Arena.ofConfined()) {
                final MemorySegment transferBuffer = arena.allocate(getTileLength());
                do {
                    final WritableRaster tile = iterator.createRaster();
                    final Rectangle rasterBounds = iterator.getRegionInsideTile(true);
                    if (rasterBounds != null) {
                        rasterBounds.translate(tile.getMinX(), tile.getMinY());
                        assert (imageBounds == null) || imageBounds.contains(rasterBounds)
                                : AssertionMessages.notContained(imageBounds, rasterBounds);

                        resourceBounds = iterator.imageToResource(rasterBounds, false);
                        owner.clipReadRegion(resourceBounds);

                        // Conversion from uncropped coordinates to cropped coordinates before reading.
                        iterator.getUncroppedTileLocation().ifPresent((p) -> rasterBounds.translate(p.x, p.y));
                        if (!Band.transfer(gdal, OpenFlag.READ, bands, owner.dataType, resourceBounds, tile, rasterBounds, transferBuffer)) {
                            break;      // Exception will be thrown by `throwOnFailure(…)`
                        }
                    }
                    result[iterator.getTileIndexInResultArray()] = tile;
                } while (iterator.next());
            } finally {
                ErrorHandler.throwOnFailure(owner.parent, "read");      // Public caller of this method.
            }
            return result;
        }
    }
}
