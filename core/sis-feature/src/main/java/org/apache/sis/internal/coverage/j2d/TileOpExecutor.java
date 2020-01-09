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
package org.apache.sis.internal.coverage.j2d;

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import java.awt.image.ImagingOpException;
import org.apache.sis.internal.feature.Resources;


/**
 * An action to execute on each tile of an image.
 * Subclasses should override one of the following methods:
 *
 * <ul>
 *   <li>{@link #readFrom(Raster)}</li>
 *   <li>{@link #writeTo(WritableRaster)}</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public abstract class TileOpExecutor {
    /**
     * Minimum/maximum index of tiles to process, inclusive.
     */
    private final int minTileX, minTileY, maxTileX, maxTileY;

    /**
     * Creates a new operation for tiles in the specified region of the specified image.
     * It is caller responsibility to ensure that {@code aoi} is contained in {@code image} bounds.
     *
     * @param  image  the image from which tiles will be fetched.
     * @param  aoi    region of interest.
     */
    protected TileOpExecutor(final RenderedImage image, final Rectangle aoi) {
        final int  tileWidth       = image.getTileWidth();
        final int  tileHeight      = image.getTileHeight();
        final long tileGridXOffset = image.getTileGridXOffset();   // We want 64 bits arithmetic in operations below.
        final long tileGridYOffset = image.getTileGridYOffset();
        minTileX = Math.toIntExact(Math.floorDiv(aoi.x                     - tileGridXOffset, tileWidth));
        minTileY = Math.toIntExact(Math.floorDiv(aoi.y                     - tileGridYOffset, tileHeight));
        maxTileX = Math.toIntExact(Math.floorDiv(aoi.x + (aoi.width  - 1L) - tileGridXOffset, tileWidth));
        maxTileY = Math.toIntExact(Math.floorDiv(aoi.y + (aoi.height - 1L) - tileGridYOffset, tileHeight));
    }

    /**
     * Returns the range of indices of tiles to be processed by this {@code TileOpExecutor}.
     *
     * @return range of tile indices to be processed.
     */
    public final Rectangle getTileIndices() {
        return new Rectangle(minTileX, minTileY, maxTileX - minTileX + 1, maxTileY - minTileY + 1);
    }

    /**
     * Executes the read operation on the given tile.
     * The default implementation does nothing.
     *
     * @param  source  the tile to read.
     */
    protected void readFrom(Raster source) {
    }

    /**
     * Executes the write operation on the given tile.
     * The default implementation does nothing.
     *
     * @param  target  the tile where to write.
     * @throws Exception if an error occurred while computing the values to write.
     */
    protected void writeTo(WritableRaster target) throws Exception {
    }

    /**
     * Executes the read action on tiles of the specified source image.
     * The given source should be the same than the image specified at construction time.
     * Only tiles intersecting the area of interest will be processed.
     *
     * <p>If a tile processing throws an exception, then this method stops immediately;
     * remaining tiles are not processed. This policy is suited to the cases where the
     * caller will not return any result in case of error.</p>
     *
     * <p>Current implementation does not parallelize tile operations, because this method is
     * invoked in contexts where it should apply on exactly one tile most of the times.</p>
     *
     * @param  source  the image to read. Should be the image specified at construction time.
     */
    public final void readFrom(final RenderedImage source) {
        for (int ty = minTileY; ty <= maxTileY; ty++) {
            for (int tx = minTileX; tx <= maxTileX; tx++) {
                readFrom(source.getTile(tx, ty));
            }
        }
    }

    /**
     * Executes the write action on tiles of the specified target image.
     * The given target should be the same than the image specified at construction time.
     * Only tiles intersecting the area of interest will be processed.
     *
     * <p>If a tile processing throws an exception, then this method continues processing other tiles
     * and will rethrow the exception only after all tiles have been processed. This policy is suited
     * to the cases where the target image will continue to exist after this method call and we want
     * to have a relatively consistent state.</p>
     *
     * <p>Current implementation does not parallelize tile operations, because this method is
     * invoked in contexts where it should apply on exactly one tile most of the times.</p>
     *
     * @param  target  the image where to write. Should be the image specified at construction time.
     * @throws ImagingOpException if a {@link #writeTo(WritableRaster)} call threw an exception.
     */
    public final void writeTo(final WritableRenderedImage target) {
        ImagingOpException error = null;
        for (int ty = minTileY; ty <= maxTileY; ty++) {
            for (int tx = minTileX; tx <= maxTileX; tx++) {
                final WritableRaster tile = target.getWritableTile(tx, ty);
                try {
                    writeTo(tile);
                } catch (Exception e) {
                    if (error == null) {
                        error = new ImagingOpException(Resources.format(Resources.Keys.CanNotUpdateTile_2, tx, ty));
                        error.initCause(e);
                    } else {
                        error.addSuppressed(e);
                    }
                } finally {
                    target.releaseWritableTile(tx, ty);
                }
            }
        }
        if (error != null) {
            throw error;
        }
    }
}
