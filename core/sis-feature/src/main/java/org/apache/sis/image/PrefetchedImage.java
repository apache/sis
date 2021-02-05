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
package org.apache.sis.image;

import java.awt.Point;
import java.util.Arrays;
import java.util.Vector;
import java.awt.Rectangle;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.RasterFormatException;
import org.apache.sis.internal.coverage.j2d.ImageUtilities;
import org.apache.sis.internal.coverage.j2d.TileOpExecutor;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.util.resources.Errors;


/**
 * An image which delegate all tile requests to another image except for some tiles that are fetched in advance.
 * This image has the same coordinate systems than the source image.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @see ImageProcessor#prefetch(RenderedImage, Rectangle)
 *
 * @since 1.1
 * @module
 */
final class PrefetchedImage extends PlanarImage {
    /**
     * The source image from which to prefetch tiles.
     */
    final RenderedImage source;

    /**
     * Index of the first prefetched tile. This index is determined from the area of interest (AOI)
     * specified at construction time; it is not necessarily the index of the first tile in the image.
     */
    private final int minTileX, minTileY;

    /**
     * Number of prefetched tiles. This number is determined from the area of interest (AOI)
     * specified at construction time; it is not necessarily include all tiles in the image.
     */
    private final int numXTiles, numYTiles;

    /**
     * The prefetched tiles. This array contains only the tiles in the area of interest (AOI)
     * specified at construction time; it does not necessarily contains all tiles in the image.
     * Some element may be {@code null} if an error occurred while computing that tile,
     * in which case a placeholder will be computed by {@link #getTile(int, int)}.
     */
    private final Raster[] tiles;

    /**
     * Creates a new prefetched image.
     *
     * @param source          the image to compute immediately (may be {@code null}).
     * @param areaOfInterest  pixel coordinates of the region to prefetch, or {@code null} for the whole image.
     * @param errorHandler    action to execute (throw an exception or log a warning) if an error occurs.
     * @param parallel        whether to execute computation in parallel.
     */
    PrefetchedImage(final RenderedImage source, Rectangle areaOfInterest,
                    final ErrorHandler errorHandler, final boolean parallel)
    {
        this.source = source;
        if (areaOfInterest != null) {
            areaOfInterest = new Rectangle(areaOfInterest);
            ImageUtilities.clipBounds(source, areaOfInterest);
            if (areaOfInterest.isEmpty()) {
                minTileX  = 0;
                minTileY  = 0;
                numXTiles = 0;
                numYTiles = 0;
                tiles = null;
                return;
            }
        }
        final Worker worker = new Worker(source, areaOfInterest);
        final Rectangle ti = worker.getTileIndices();
        minTileX  = ti.x;
        minTileY  = ti.y;
        numXTiles = ti.width;
        numYTiles = ti.height;
        tiles     = new Raster[Math.multiplyExact(numYTiles, numXTiles)];
        worker.setErrorHandler(errorHandler);
        if (parallel) {
            worker.parallelReadFrom(source);
        } else {
            worker.readFrom(source);
        }
        /*
         * If an error occurred during a tile computation, the array element corresponding
         * to that tile still null. Replace them by a placeholder. Note that it may happen
         * only if the error handler is not `ErrorHandler.THROW`.
         */
        Raster previous = null;
        for (int i=0; i<tiles.length; i++) {
            if (tiles[i] == null) {
                final int tileX = (i % numXTiles) + minTileX;
                final int tileY = (i / numXTiles) + minTileY;
                tiles[i] = previous = createPlaceholder(tileX, tileY, previous);
            }
        }
    }

    /**
     * A worker for prefetching tiles in an image.
     */
    private final class Worker extends TileOpExecutor {
        /** Image properties for converting pixel coordinates to tile indices. */
        private final long tileWidth, tileHeight, tileGridXOffset, tileGridYOffset;

        /** Prepares an instance for prefetching tiles from the given image. */
        Worker(final RenderedImage source, final Rectangle aoi) {
            super(source, aoi);
            tileWidth       = source.getTileWidth();
            tileHeight      = source.getTileHeight();
            tileGridXOffset = source.getTileGridXOffset();
            tileGridYOffset = source.getTileGridYOffset();
        }

        /** Invoked when a tile has been computed, possibly in a background thread. */
        @Override protected void readFrom(final Raster source) {
            final long tx = Math.floorDiv(source.getMinX() - tileGridXOffset, tileWidth)  - minTileX;
            final long ty = Math.floorDiv(source.getMinY() - tileGridYOffset, tileHeight) - minTileY;
            assert tx >= 0 && tx < numXTiles : tx;
            assert ty >= 0 && ty < numYTiles : ty;
            final int index = Math.toIntExact(tx + ty*numXTiles);
            synchronized (tiles) {
                if (tiles[index] != null) {
                    throw new RasterFormatException(Errors.format(
                            Errors.Keys.DuplicatedElement_1, "Tile[" + tx + ", " + ty + ']'));
                }
                tiles[index] = source;
            }
        }
    }

    /**
     * Returns {@code true} if this image does not prefetch any tiles.
     */
    final boolean isEmpty() {
        return (numXTiles | numYTiles) == 0;
    }

    /**
     * Returns the immediate source of image data for this image.
     */
    @Override
    @SuppressWarnings("UseOfObsoleteCollectionType")
    public Vector<RenderedImage> getSources() {
        final Vector<RenderedImage> sources = new Vector<>(1);
        sources.add(source);
        return sources;
    }

    /**
     * Gets a property from the source image.
     * All properties are inherited from the source with no change.
     */
    @Override
    public Object getProperty(final String name) {
        return source.getProperty(name);
    }

    /** Delegate to the source image. */
    @Override public String[]    getPropertyNames()   {return source.getPropertyNames();}
    @Override public ColorModel  getColorModel()      {return source.getColorModel();}
    @Override public SampleModel getSampleModel()     {return source.getSampleModel();}
    @Override public int         getWidth()           {return source.getWidth();}
    @Override public int         getHeight()          {return source.getHeight();}
    @Override public int         getMinX()            {return source.getMinX();}
    @Override public int         getMinY()            {return source.getMinY();}
    @Override public int         getNumXTiles()       {return source.getNumXTiles();}
    @Override public int         getNumYTiles()       {return source.getNumYTiles();}
    @Override public int         getMinTileX()        {return source.getMinTileX();}
    @Override public int         getMinTileY()        {return source.getMinTileY();}
    @Override public int         getTileWidth()       {return source.getTileWidth();}
    @Override public int         getTileHeight()      {return source.getTileHeight();}
    @Override public int         getTileGridXOffset() {return source.getTileGridXOffset();}
    @Override public int         getTileGridYOffset() {return source.getTileGridYOffset();}

    /**
     * Returns the tile at the given location in tile coordinates. If the requested
     * tile is in the region of prefetched tiles, that tile is returned directly.
     * Otherwise this method delegates to the source image.
     */
    @Override
    public Raster getTile(final int tileX, final int tileY) {
        final int tx = Math.subtractExact(tileX, minTileX);
        if (tx >= 0 && tileX < numXTiles) {
            final int ty = Math.subtractExact(tileY, minTileY);
            if (ty >= 0 && tileY < numYTiles) {
                return tiles[tx + ty * numXTiles];
            }
        }
        return source.getTile(tileX, tileY);
    }

    /**
     * Creates a tile to use as a placeholder when a tile can not be computed.
     *
     * @param  tileX     column index of the tile for which to create a placeholder.
     * @param  tileY     row index of the tile for which to create a placeholder.
     * @param  previous  tile previously created by this method, or {@code null} if none.
     * @return placeholder for the tile at given indices.
     */
    private Raster createPlaceholder(final int tileX, final int tileY, final Raster previous) {
        final SampleModel model = getSampleModel();
        final Point location = new Point(ImageUtilities.tileToPixelX(source, tileX),
                                         ImageUtilities.tileToPixelY(source, tileY));
        if (previous != null) {
            // Reuse same `DataBuffer` with only a different location.
            return Raster.createRaster(model, previous.getDataBuffer(), location);
        }
        final double[] samples = new double[model.getNumBands()];
        if (ImageUtilities.isIntegerType(model)) {
            final boolean isUnsigned = ImageUtilities.isUnsignedType(model);
            for (int i=0; i<samples.length; i++) {
                int size = model.getSampleSize(i);
                if (!isUnsigned) size--;
                samples[i] = Numerics.bitmask(size) - 1;
            }
        } else {
            final ColorSpace cs;
            final ColorModel cm = getColorModel();
            if (cm != null && (cs = cm.getColorSpace()) != null) {
                for (int i = Math.min(cs.getNumComponents(), samples.length); --i >=0;) {
                    samples[i] = cs.getMaxValue(i);
                }
            } else {
                Arrays.fill(samples, 1);
            }
        }
        /*
         * Draw borders around the tile as dotted lines. The left border will have (usually) white pixels
         * at even coordinates relative to upper-left corner, while right border will have same pixels at
         * odd coordinates. The same pattern applies to top and bottom borders.
         */
        final WritableRaster tile = WritableRaster.createWritableRaster(model, model.createDataBuffer(), location);
        final int width  = tile.getWidth();
        final int height = tile.getHeight();
        final int xmin   = tile.getMinX();
        final int ymin   = tile.getMinY();
        final int xmax   = width  + xmin - 1;
        final int ymax   = height + ymin - 1;
        int x = xmin;
        while (x < xmax) {
            tile.setPixel(x++, ymin, samples);
            tile.setPixel(x++, ymax, samples);
        }
        int y = ymin;
        while (y < ymax) {
            tile.setPixel(xmin, y++, samples);
            tile.setPixel(xmax, y++, samples);
        }
        if (x == xmax) tile.setPixel(xmax, ymin, samples);
        if (y == ymax) tile.setPixel(xmin, ymax, samples);
        /*
         * Add a cross (X) inside the tile.
         */
        if (width >= height) {
            final double step = height / (double) width;
            for (int i=0; i<width; i++) {
                x = xmin + i;
                y = (int) (i*step);
                tile.setPixel(x, ymin + y, samples);
                tile.setPixel(x, ymax - y, samples);
            }
        } else {
            final double step = width / (double) height;
            for (int i=0; i<height; i++) {
                y = ymin + i;
                x = (int) (i*step);
                tile.setPixel(xmin + x, y, samples);
                tile.setPixel(xmax - x, y, samples);
            }
        }
        return tile;
    }
}
