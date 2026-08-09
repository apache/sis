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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridRoundingMode;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.image.internal.shared.BatchComputedImage;
import org.apache.sis.util.collection.WeakValueHashMap;


/**
 * An image which simulates the existence of tiles when the source coverage has tiles too large.
 * An image may be tiled in only one direction. For example, a <abbr>TIFF</abbr> image using the
 * strip layout can be seen as an image tiled only in the direction of the <var>y</var> axis.
 *
 * <p>The pixel coordinates start always at (0,0) because this image represents, by definition,
 * the full coverage obtained in a call equivalent to {@code GridCoverage.render(null)}.
 * The tile coordinates start also at (0,0), but this is only for convenience.</p>
 *
 * <p>Used for {@link org.apache.sis.storage.RasterLoadingStrategy#AT_GET_TILE_TIME} only.
 * Other loading strategies should not instantiate this class.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ArtificiallyTiledImage extends BatchComputedImage {
    /**
     * The grid coverage resource from which to read sub-regions as tiles.
     */
    private final TiledGridCoverageResource source;

    /**
     * The domain represented by this image.
     */
    private final GridGeometry domain;

    /**
     * The dimension of the grid which is mapped to the <var>x</var>. This is usually 0.
     */
    private final int xDimension;

    /**
     * The dimension of the grid which is mapped to the <var>y</var>. This is usually 1.
     */
    private final int yDimension;

    /**
     * The image color model.
     */
    private final ColorModel colors;

    /**
     * The bands in the order they were requested, or {@code null} if all bands shall be included.
     */
    private final int[] requestedBands;

    /**
     * Cache of tiles. Keys are tile indexes. Note that the size of those tiles is not the same
     * as the size of the tiles cached in {@link TiledGridCoverageResource#rasters}.
     */
    private final WeakValueHashMap<Point, Raster> cache;

    /**
     * Creates a new artificially tiled coverage for the given resource subset.
     */
    protected ArtificiallyTiledImage(final TiledGridCoverageResource.Subset subset) {
        super(subset.getImageModel(), null, (RenderedImage[]) null);
        colors         = subset.colorsForBandSubset;
        source         = subset.resource();
        domain         = subset.domain;
        xDimension     = subset.xDimension();
        yDimension     = subset.yDimension();
        requestedBands = subset.requestedBands;
        cache = new WeakValueHashMap<>(Point.class);
    }

    /**
     * Returns the color model which has been specified to the constructor.
     */
    @Override
    public final ColorModel getColorModel() {
        return colors;
    }

    /**
     * Returns the resource extent in the direction of <var>x</var> pixel coordinates.
     */
    @Override
    public final int getWidth() {
        return getSize(xDimension);
    }

    /**
     * Returns the resource extent in the direction of <var>y</var> pixel coordinates.
     */
    @Override
    public final int getHeight() {
        return getSize(yDimension);
    }

    /**
     * Returns the domain size in the given dimension.
     *
     * @param  dimension  the dimension for which to get the size.
     * @return the size in the requested dimension.
     * @throws ArithmeticException if the size is too large for a 32-bits integer.
     */
    private int getSize(final int dimension) {
        return Math.toIntExact(domain.getExtent().getSize(dimension));
    }

    /**
     * Computes immediately and returns all tiles in the given ranges of tile indices.
     * Tiles are returned in row-major order.
     *
     * @param  tiles  range of tile indices for which to precompute tiles.
     * @return precomputed tiles for the given indices, in row-major fashion.
     * @throws DataStoreException if an error occurred while reading tiles.
     * @throws TransformException if an error occurred while computing extents.
     */
    @Override
    protected Raster[] computeTiles(final Rectangle tiles) throws DataStoreException, TransformException {
        if (tiles.isEmpty()) {
            return new Raster[0];
        }
        int minTileX = Integer.MAX_VALUE;
        int minTileY = Integer.MAX_VALUE;
        int maxTileX = Integer.MIN_VALUE;
        int maxTileY = Integer.MIN_VALUE;
        final int numXTiles = tiles.width;
        final var rasters = new Raster[Math.multiplyExact(numXTiles, tiles.height)];
        for (int i = 0; i < rasters.length; i++) {
            final int x = tiles.x + i % numXTiles;
            final int y = tiles.y + i / numXTiles;
            if ((rasters[i] = cache.get(new Point(x, y))) == null) {
                if (x < minTileX) minTileX = x;
                if (x > maxTileX) maxTileX = x;
                if (y < minTileY) minTileY = y;
                if (y > maxTileY) maxTileY = y;
            }
        }
        if (minTileX <= maxTileX && minTileY <= maxTileY) {
            GridExtent extent   = domain.getExtent();
            final int dimension = extent.getDimension();
            final long[] low    = new long[dimension];
            final long[] high   = new long[dimension];
            for (int i=0; i<dimension; i++) {
                final long base = extent.getLow(i);
                final int min, max, ts;
                if (i == xDimension) {
                    min = minTileX;
                    max = maxTileX;
                    ts  = getTileWidth();
                } else if (i == yDimension) {
                    min = minTileY;
                    max = maxTileY;
                    ts  = getTileHeight();
                } else {
                    low [i] = base;
                    high[i] = extent.getHigh(i);
                    continue;
                }
                low [i] = Math.addExact(base, Math.multiplyExact(ts, min));
                high[i] = Math.addExact(base, Math.multiplyExact(ts, max + 1L)) - 1;
            }
            /*
             * Request an area in a subregion of `domain` specified by `low` and `high` arrays.
             * The grid coordinate system of `domain` should be the same as the one of `source`,
             * but a conversion is done automatically by `source.read(…)` if nevertheless needed.
             * Then, the requested extent is converted to the coverage grid coordinate system.
             * Because this is also expected to be an identity operation or at most a translation,
             * the rounding more is set to `NEAREST` instead of `ENCLOSING`.
             */
            final GridGeometry request  = domain.relocate(extent.reshape(low, high, true));
            final GridCoverage coverage = source.readAtGetTileTime(request, requestedBands);
            extent = coverage.getGridGeometry().extentOf(request, PixelInCell.CELL_CORNER, GridRoundingMode.NEAREST);
            final RenderedImage image = coverage.render(extent);
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            final SampleModel sampleModel = getSampleModel();
            final long offsetX = Math.multiplyFull(minTileX, getTileWidth());
            final long offsetY = Math.multiplyFull(minTileY, getTileHeight());
            for (int y = minTileY; y <= maxTileY; y++) {
                for (int x = minTileX; x <= maxTileX; x++) {
                    // No integer arithmetic can overflow in this loop.
                    final int i = (y - tiles.y) * numXTiles + (x - tiles.x);
                    if (rasters[i] == null) {
                        rasters[i] = cache.computeIfAbsent(new Point(x, y), (key) -> {
                            final int tileWidth  = sampleModel.getWidth();
                            final int tileHeight = sampleModel.getHeight();
                            final int tileMinX   = Math.multiplyExact(key.x, tileWidth);
                            final int tileMinY   = Math.multiplyExact(key.y, tileHeight);
                            WritableRaster tile  = Raster.createWritableRaster(sampleModel, new Point(tileMinX, tileMinY));
                            /*
                             * By contract, image pixel coordinates (0,0) correspond to (minTileX, minTileY) in the request.
                             * We need to temporarily translate the raster where pixel values will be copied.
                             * The original raster is the parent of the translated raster.
                             */
                            if ((offsetX | offsetY) != 0) {
                                tile = tile.createWritableTranslatedChild(
                                        Math.toIntExact(tileMinX - offsetX),
                                        Math.toIntExact(tileMinY - offsetY));
                            }
                            Raster copy = image.copyData(tile);
                            /*
                             * Get the untranslated raster. It should be `tile` directory if we did not applied
                             * any translation, or the direct parent of `tile` other. We nevertheless search in
                             * all parents in case and fallback on a new raster if no parent is found.
                             */
                            while (copy.getMinX()   != tileMinX ||
                                   copy.getMinY()   != tileMinY ||
                                   copy.getWidth()  != tileWidth ||
                                   copy.getHeight() != tileHeight)
                            {
                                Raster parent = copy.getParent();
                                if (parent == null) {
                                    // Should never happen, but defined for safety.
                                    return tile.createTranslatedChild(tileMinX, tileMinY);
                                }
                                copy = parent;
                            }
                            return copy;
                        });
                    }
                }
            }
        }
        return rasters;
    }
}
