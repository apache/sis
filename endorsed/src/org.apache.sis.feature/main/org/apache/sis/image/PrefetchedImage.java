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

import java.util.Vector;
import java.util.Objects;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.Raster;
import java.awt.image.RasterFormatException;
import org.apache.sis.image.privy.ImageUtilities;
import org.apache.sis.image.privy.TileErrorHandler;
import org.apache.sis.image.privy.TileOpExecutor;
import org.apache.sis.image.privy.TilePlaceholder;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Disposable;
import org.apache.sis.util.resources.Errors;


/**
 * An image which delegate all tile requests to another image except for some tiles that are fetched in advance.
 * This image has the same coordinate systems as the source image.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see ImageProcessor#prefetch(RenderedImage, Rectangle)
 */
final class PrefetchedImage extends PlanarImage implements TileErrorHandler.Executor {
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
     * If error(s) occurred while computing one or more tiles, data shared by {@link Raster} placeholders.
     * This is data for a tile showing a cross (X) in a box.
     *
     * @see #createPlaceholder(int, int)
     */
    private volatile TilePlaceholder placeholderPixels;

    /**
     * Non-null if errors should be handled during {@link #getTile(int, int)} execution for tiles outside
     * the area of interest specified at construction time.
     *
     * @see #execute(Runnable, TileErrorHandler)
     */
    private ErrorHandler.Report errorReport;

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
        worker.setErrorHandler(errorHandler, ImageProcessor.class, "prefetch");
        final Disposable ph = (source instanceof PlanarImage) ? ((PlanarImage) source).prefetch(ti) : null;
        try {
            if (parallel) {
                worker.parallelReadFrom(source);
            } else {
                worker.readFrom(source);
            }
        } catch (Throwable ex) {
            if (ph != null) try {
                ph.dispose();
            } catch (Throwable e) {
                ex.addSuppressed(e);
            }
            throw ex;
        }
        if (ph != null) {
            ph.dispose();
        }
        /*
         * If an error occurred during a tile computation, the array element corresponding
         * to that tile still null. Replace them by a placeholder. Note that it may happen
         * only if the error handler is not `ErrorHandler.THROW`.
         */
        for (int i=0; i<tiles.length; i++) {
            if (tiles[i] == null) {
                final int tileX = (i % numXTiles) + minTileX;
                final int tileY = (i / numXTiles) + minTileY;
                tiles[i] = createPlaceholder(tileX, tileY);
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
        /*
         * If the requested tile is not one of the tiles that we computed in advance,
         * fetch directly from the source (may imply computation in current thread).
         * If an error occurs and this method is invoked inside `execute(…)` block,
         * apply a similar error handling than the one applied in constructor.
         */
        try {
            return source.getTile(tileX, tileY);
        } catch (RuntimeException e) {
            final ErrorHandler.Report report = errorReport;
            if (report == null) {
                throw e;
            }
            report.add(new Point(tileX, tileY), e, null);
            assert Thread.holdsLock(this);
            return createPlaceholder(tileX, tileY);
        }
    }

    /**
     * Executes the given action in a mode where errors occurring in {@link RenderedImage#getTile(int, int)}
     * are reported to the given handler instead of stopping the operation. The given action is typically
     * some operation invoking, directly or indirectly, {@link #getTile(int, int)} with tile indices that
     * may be outside the area of interest specified at construction time. Exceptions that occurred inside
     * the area of interest were caught by the constructor and this method makes no difference for them.
     * But exceptions occurring outside that area are interest are redirected to the {@link #source} image,
     * which may fail. This method provides a way to catch also those errors.
     *
     * @param  action        the action to execute (for example drawing the image).
     * @param  errorHandler  the handler to notify if errors occur.
     */
    @Override
    public synchronized void execute(final Runnable action, final TileErrorHandler errorHandler) {
        ArgumentChecks.ensureNonNull("action", action);
        ArgumentChecks.ensureNonNull("errorHandler", errorHandler);
        errorReport = new ErrorHandler.Report();
        try {
            action.run();
        } finally {
            final ErrorHandler.Report report = errorReport;
            errorReport = null;
            errorHandler.publish(report);
        }
    }

    /**
     * Creates a tile to use as a placeholder when a tile cannot be computed.
     *
     * @param  tileX  column index of the tile for which to create a placeholder.
     * @param  tileY  row index of the tile for which to create a placeholder.
     * @return placeholder for the tile at given indices.
     */
    private Raster createPlaceholder(final int tileX, final int tileY) {
        TilePlaceholder p = placeholderPixels;
        if (p == null) {
            // Not a problem if invoked concurrently in two threads.
            placeholderPixels = p = TilePlaceholder.withCross(source);
        }
        return p.create(new Point(ImageUtilities.tileToPixelX(source, tileX),
                                  ImageUtilities.tileToPixelY(source, tileY)));
    }

    /**
     * Returns a hash code value for this image.
     * Defined for consistency with {@link #equals(Object)}.
     */
    @Override
    public int hashCode() {
        return Objects.hash(source, minTileX, minTileY, numXTiles, numYTiles);
    }

    /**
     * Compares the given object with this image for equality. This is defined as a matter of principle,
     * but is a little bit useless for {@link PrefetchedImage} because tiles have already been computed
     * in the constructor. So it is too late for caching for example.
     */
    @Override
    public boolean equals(final Object object) {
        if (object instanceof PrefetchedImage) {
            final PrefetchedImage other = (PrefetchedImage) object;
            return source.equals(other.source)  &&
                   minTileX  == other.minTileX  &&
                   minTileY  == other.minTileY  &&
                   numXTiles == other.numXTiles &&
                   numYTiles == other.numYTiles;
        }
        return false;
    }
}
