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

import java.util.Set;
import java.util.HashSet;
import java.awt.Point;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.ImagingOpException;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.lang.ref.WeakReference;
import org.apache.sis.internal.system.ReferenceQueueConsumer;
import org.apache.sis.image.PlanarImage;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Disposable;


/**
 * An image with tiles computed on-the-fly and cached for future reuse.
 * Computations are performed on a tile-by-tile basis and the result is
 * stored in a cache shared by all images on the platform. Tiles may be
 * discarded at any time, in which case they will need to be recomputed
 * when needed again.
 *
 * <p>This class is thread-safe. Multiple tiles may be computed in
 * different background threads.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public abstract class CachedImage extends PlanarImage {
    /**
     * Weak reference to the enclosing image together with necessary information for releasing resources
     * when image is disposed. This class shall not contain any strong reference to the enclosing image.
     */
    // MUST be static
    private static final class Cleaner extends WeakReference<CachedImage> implements Disposable {
        /**
         * Indices of all cached tiles. Used for removing tiles from the cache when the image is disposed.
         * All accesses to this collection must be synchronized. This field has to be declared here because
         * {@link Cleaner} is not allowed to keep a strong reference to the enclosing {@link CachedImage}.
         */
        private final Set<TileCache.Key> cachedTiles;

        /**
         * Creates a new weak reference to the given image.
         */
        Cleaner(final CachedImage image) {
            super(image, ReferenceQueueConsumer.QUEUE);
            cachedTiles = new HashSet<>();
        }

        /**
         * Remember that the given tile will need to be removed from the cache
         * when the enclosing image will be garbage-collected.
         */
        final void addTile(final TileCache.Key key) {
            synchronized (cachedTiles) {
                cachedTiles.add(key);
            }
        }

        /**
         * Invoked when the enclosing image has been garbage-collected. This method removes all cached tiles
         * that were owned by the enclosing image. This method should not perform other cleaning work than
         * removing cached tiles because it is not guaranteed to be invoked if {@link TileCache#GLOBAL}
         * does not contain any tile for the enclosing image.
         */
        @Override
        public void dispose() {
            synchronized (cachedTiles) {
                cachedTiles.forEach(TileCache.Key::dispose);
                cachedTiles.clear();
            }
        }
    }

    /**
     * Weak reference to this image, also used as a cleaner when the image is garbage-collected.
     * This reference is retained in {@link TileCache#GLOBAL}. Note that if that cache does not
     * cache any tile for this image, then this {@link Cleaner} may be garbage-collected in same
     * time than this image and its {@link Cleaner#dispose()} method never invoked.
     */
    private final Cleaner reference;

    /**
     * The sample model shared by all tiles in this image.
     * The {@linkplain SampleModel#getWidth() sample model width}
     * determines this {@linkplain #getTileWidth() image tile width},
     * and the {@linkplain SampleModel#getHeight() sample model height}
     * determines this {@linkplain #getTileHeight() image tile height}.
     *
     * <div class="note"><b>Design note:</b>
     * {@code CachedImage} requires the sample model to have exactly the desired tile size
     * otherwise tiles created by {@link #createTile(int, int)} will consume more memory
     * than needed.</div>
     */
    protected final SampleModel sampleModel;

    /**
     * Creates an initially empty image with the given sample model.
     * The tile size will be the width and height of the given sample model.
     *
     * @param  sampleModel  the sample model shared by all tiles in this image.
     */
    protected CachedImage(final SampleModel sampleModel) {
        ArgumentChecks.ensureNonNull("sampleModel", sampleModel);
        reference = new Cleaner(this);
        this.sampleModel = sampleModel;
    }

    /**
     * Creates an initially empty image with a sample model derived from the given image.
     * This constructor sets {@link #sampleModel} to a model compatible with the one used
     * by the given image, but with {@linkplain SampleModel#getWidth() width} and
     * {@linkplain SampleModel#getHeight() height} matching exactly the size of the tiles.
     *
     * <p>This constructor does <strong>not</strong> inherit other image properties.
     * In particular pixel coordinates and tile indices in this image start at (0,0)
     * unless subclass override {@link #getMinX()}, {@link #getMinY()}, {@link #getMinTileX()}
     * and {@link #getMinTileY()}.</p>
     *
     * @param  image  the image from which to get tile size.
     */
    protected CachedImage(final RenderedImage image) {
        ArgumentChecks.ensureNonNull("image", image);
        reference = new Cleaner(this);
        sampleModel = adapt(image.getSampleModel(), image.getTileWidth(), image.getTileHeight());
    }

    /**
     * Returns a sample model compatible with the given one, but with the specified width and height.
     * This method checks if the given sample model can be used as-is and create a new one only if needed.
     * This restriction about sample model size matching tile size is for reducing the amount of memory
     * consumed by {@link #createTile(int, int)}.
     */
    private static SampleModel adapt(SampleModel sampleModel, final int width, final int height) {
        if (sampleModel.getWidth() != width || sampleModel.getHeight() != height) {
            sampleModel = sampleModel.createCompatibleSampleModel(width, height);
        }
        return sampleModel;
    }

    /**
     * Returns the width of tiles in this image. The default implementation returns {@link SampleModel#getWidth()}.
     *
     * <div class="note"><b>Note:</b>
     * a raster can have a smaller width than its sample model, for example when a raster is a view over a subregion
     * of another raster. But this is not recommended in the particular case of this {@code CachedImage} class,
     * because it would cause {@link #createTile(int, int)} to consume more memory than necessary.</div>
     *
     * @return the width of this image in pixels.
     */
    @Override
    public int getTileWidth() {
        return sampleModel.getWidth();
    }

    /**
     * Returns the height of tiles in this image. The default implementation returns {@link SampleModel#getHeight()}.
     *
     * <div class="note"><b>Note:</b>
     * a raster can have a smaller height than its sample model, for example when a raster is a view over a subregion
     * of another raster. But this is not recommended in the particular case of this {@code CachedImage} class,
     * because it would cause {@link #createTile(int, int)} to consume more memory than necessary.</div>
     *
     * @return the height of this image in pixels.
     */
    @Override
    public int getTileHeight() {
        return sampleModel.getHeight();
    }

    /**
     * Returns a tile of this image, computing it when needed.
     * This method performs the first of the following actions that apply:
     *
     * <ol>
     *   <li>If the requested tile is present in the cache, then that tile is returned immediately.</li>
     *   <li>Otherwise if the requested tile is being computed in another thread, then this method blocks
     *       until the other thread completed its work and returns its result. If the other thread failed
     *       to compute the tile, an {@link ImagingOpException} is thrown.</li>
     *   <li>Otherwise this method computes the tile and caches the result before to return it.
     *       If an error occurred, an {@link ImagingOpException} is thrown.</li>
     * </ol>
     *
     * @param  tileX  the column index of the tile to get.
     * @param  tileY  the row index of the tile to get.
     * @return the tile at the given index (never null).
     * @throws IndexOutOfBoundsException if a given tile index is out of bounds.
     * @throws ImagingOpException if an error occurred while computing the image.
     */
    @Override
    public final Raster getTile(final int tileX, final int tileY) {
        final TileCache.Key key = new TileCache.Key(reference, tileX, tileY);
        final Cache<TileCache.Key,Raster> cache = TileCache.GLOBAL;
        Raster tile = cache.peek(key);
        if (tile == null) {
            final Cache.Handler<Raster> handler = cache.lock(key);
            try {
                tile = handler.peek();
                if (tile == null) {
                    tile = computeTile(tileX, tileY);
                    if (tile == null) {
                        throw new ImagingOpException("No data");    // TODO
                    }
                }
            } finally {
                handler.putAndUnlock(tile);     // Must be invoked even if an exception occurred.
            }
        }
        return tile;
    }

    /**
     * Invoked when a tile need to be computed. This method is invoked by {@link #getTile(int, int)}
     * when the requested tile is not in the cache. The returned tile will be automatically cached.
     *
     * @param  tileX  the column index of the tile to compute.
     * @param  tileY  the row index of the tile to compute.
     * @return computed tile for the given indices (can not be null).
     */
    protected abstract Raster computeTile(int tileX, int tileY);

    /**
     * Creates an initially empty tile at the given tile grid position.
     * This is a helper method for {@link #computeTile(int, int)} implementations.
     *
     * @param  tileX  the column index of the tile to create.
     * @param  tileY  the row index of the tile to create.
     * @return initially empty tile for the given indices (can not be null).
     */
    protected WritableRaster createTile(final int tileX, final int tileY) {
        // A temporary `int` overflow may occur before the final addition.
        final int x = Math.toIntExact((((long) tileX) - getMinTileX()) * getTileWidth()  + getMinX());
        final int y = Math.toIntExact((((long) tileY) - getMinTileY()) * getTileHeight() + getMinY());
        return WritableRaster.createWritableRaster(getSampleModel(), new Point(x,y));
    }

    /**
     * Advises this image that its tiles will no longer be requested.
     * This method removes all tiles from the cache.
     * This image should not be used anymore after this method call.
     *
     * <p><b>Note:</b> keep in mind that this image may be referenced as a source of other images.
     * In case of doubt, it may be safer to rely on the garbage collector instead than invoking this method.</p>
     */
    public void dispose() {
        reference.dispose();
    }
}
