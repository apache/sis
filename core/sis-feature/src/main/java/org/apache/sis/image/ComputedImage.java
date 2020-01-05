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

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Vector;
import java.awt.Point;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.TileObserver;
import java.awt.image.ImagingOpException;
import java.lang.ref.WeakReference;
import org.apache.sis.internal.system.ReferenceQueueConsumer;
import org.apache.sis.internal.feature.Resources;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Disposable;


/**
 * An image with tiles computed on-the-fly and cached for future reuse.
 * Computations are performed on a tile-by-tile basis and the result is
 * stored in a cache shared by all images on the platform. Tiles may be
 * discarded at any time, in which case they will need to be recomputed
 * when needed again.
 *
 * <p>Subclasses need to implement at least the following methods:</p>
 * <ul>
 *   <li>{@link #getWidth()}  — the image width in pixels.</li>
 *   <li>{@link #getHeight()} — the image height in pixels.</li>
 *   <li>{@link #computeTile(int, int, WritableRaster)} — invoked when a
 *       requested tile is not in the cache or needs to be updated.</li>
 * </ul>
 *
 * <p>If pixel coordinates or tile indices do not start at zero,
 * then subclasses shall also override the following methods:</p>
 * <ul>
 *   <li>{@link #getMinX()}     — the minimum <var>x</var> coordinate (inclusive) of the image.</li>
 *   <li>{@link #getMinY()}     — the minimum <var>y</var> coordinate (inclusive) of the image.</li>
 *   <li>{@link #getMinTileX()} — the minimum tile index in the <var>x</var> direction.</li>
 *   <li>{@link #getMinTileY()} — the minimum tile index in the <var>y</var> direction.</li>
 * </ul>
 *
 * <p>This class is thread-safe: multiple tiles may be computed in different background threads.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public abstract class ComputedImage extends PlanarImage {
    /**
     * Whether a tile in the cache is ready for use or needs to be recomputed
     * because one if its sources changed its data.
     */
    private enum TileStatus {
        /** The tile is ready for use. */
        STORED,

        /** The tile needs to be recomputed because at least one source changed its data. */
        DIRTY,

        /** The tile has been checked out for a write operation. */
        CHECKED,

        /** The tile needs to be recomputed, but it is also checked for write operation by someone else. */
        CHECKED_AND_DIRTY;

        /** Remapping function for calls to {@link Map#merge(Object, Object, java.util.function.BiFunction)}. */
        static TileStatus merge(final TileStatus oldValue, TileStatus newValue) {
            if (newValue == DIRTY && oldValue == CHECKED) {
                newValue = CHECKED_AND_DIRTY;
            }
            return newValue;
        }
    }

    /**
     * Weak reference to the enclosing image together with necessary information for releasing resources
     * when image is disposed. This class shall not contain any strong reference to the enclosing image.
     */
    // MUST be static
    private static final class Cleaner extends WeakReference<ComputedImage> implements Disposable, TileObserver {
        /**
         * Indices of all cached tiles. Used for removing tiles from the cache when the image is disposed.
         * All accesses to this collection must be synchronized. This field has to be declared here because
         * {@link Cleaner} is not allowed to keep a strong reference to the enclosing {@link ComputedImage}.
         */
        private final Map<TileCache.Key, TileStatus> cachedTiles;

        /**
         * All {@link ComputedImage#sources} that are writable, or {@code null} if none.
         * This is used for removing tile observers when the enclosing image is garbage-collected.
         */
        private WritableRenderedImage[] sources;

        /**
         * Creates a new weak reference to the given image and registers this {@link Cleaner}
         * as a listener of all given sources. The listeners will be automatically removed
         * when the enclosing image is garbage collected.
         *
         * @param  image  the enclosing image for which to release tiles on garbage-collection.
         * @param  ws     sources to observe for changes, or {@code null} if none.
         */
        @SuppressWarnings("ThisEscapedInObjectConstruction")
        Cleaner(final ComputedImage image, final WritableRenderedImage[] ws) {
            super(image, ReferenceQueueConsumer.QUEUE);
            cachedTiles = new HashMap<>();
            sources = ws;
            if (ws != null) {
                int i = 0;
                try {
                    while (i < ws.length) {
                        WritableRenderedImage source = ws[i++];     // `i++` must be before `addTileObserver(…)` call.
                        source.addTileObserver(this);
                    }
                } catch (RuntimeException e) {
                    unregister(ws, i, e);                           // `unregister(…)` will rethrow the given exception.
                }
            }
        }

        /**
         * Remember that the given tile will need to be removed from the cache
         * when the enclosing image will be garbage-collected.
         */
        final void addTile(final TileCache.Key key) {
            synchronized (cachedTiles) {
                cachedTiles.put(key, TileStatus.STORED);
            }
        }

        /**
         * Returns {@code true} if the specified tile needs to be recomputed.
         */
        final boolean isDirty(final TileCache.Key key) {
            final TileStatus status;
            synchronized (cachedTiles) {
                status = cachedTiles.get(key);
            }
            return (status == TileStatus.DIRTY) || (status == TileStatus.CHECKED_AND_DIRTY);
        }

        /**
         * Invoked when a source is changing the content of one of its tile.
         * This method is interested only in events fired after the change is done.
         * The tiles that depend on the modified tile are marked in need to be recomputed.
         *
         * @param source          the image that own the tile which is about to be updated.
         * @param tileX           the <var>x</var> index of the tile that is being updated.
         * @param tileY           the <var>y</var> index of the tile that is being updated.
         * @param willBeWritable  if true, the tile is grabbed for writing; otherwise it is being released.
         */
        @Override
        public void tileUpdate(final WritableRenderedImage source, int tileX, int tileY, final boolean willBeWritable) {
            if (!willBeWritable) {
                final ComputedImage target = get();
                if (target != null) {
                    final long sourceWidth  = source.getTileWidth();
                    final long sourceHeight = source.getTileHeight();
                    final long targetWidth  = target.getTileWidth();
                    final long targetHeight = target.getTileHeight();
                    final long tx           = tileX * sourceWidth  + source.getTileGridXOffset() - target.getTileGridXOffset();
                    final long ty           = tileY * sourceHeight + source.getTileGridYOffset() - target.getTileGridYOffset();
                    final int  maxTileX     = Numerics.clamp(Math.floorDiv(tx + sourceWidth  - 1, targetWidth));
                    final int  maxTileY     = Numerics.clamp(Math.floorDiv(ty + sourceHeight - 1, targetHeight));
                    final int  minTileX     = Numerics.clamp(Math.floorDiv(tx, targetWidth));
                    final int  minTileY     = Numerics.clamp(Math.floorDiv(ty, targetHeight));
                    synchronized (cachedTiles) {
                        for (tileY = minTileY; tileY <= maxTileY; tileY++) {
                            for (tileX = minTileX; tileX <= maxTileX; tileX++) {
                                final TileCache.Key key = new TileCache.Key(this, tileX, tileY);
                                cachedTiles.merge(key, TileStatus.DIRTY, TileStatus::merge);
                            }
                        }
                    }
                } else {
                    /*
                     * Should not happen, unless maybe the source invoked this method before `dispose()`
                     * has done its work. Or maybe we have a bug in our code and this `Cleaner` is still
                     * alive but should not. In any cases there is no point to continue observing the source.
                     */
                    source.removeTileObserver(this);
                }
            }
        }

        /**
         * Invoked when the enclosing image has been garbage-collected. This method removes all cached tiles
         * that were owned by the enclosing image and unregister all tile observers.
         *
         * This method should not perform other cleaning work because it is not guaranteed to be invoked if
         * this {@code Cleaner} is not registered as a {@link TileObserver} and if {@link TileCache#GLOBAL}
         * does not contain any tile for the enclosing image. The reason is because there would be nothing
         * preventing this weak reference to be garbage collected before {@code dispose()} is invoked.
         *
         * @see ComputedImage#dispose()
         */
        @Override
        public void dispose() {
            synchronized (cachedTiles) {
                cachedTiles.keySet().forEach(TileCache.Key::dispose);
                cachedTiles.clear();
            }
            final WritableRenderedImage[] ws = sources;
            if (ws != null) {
                unregister(ws, ws.length, null);
            }
        }

        /**
         * Stops observing writable sources for modifications. This methods is invoked when the enclosing
         * image is garbage collected. It may also be invoked for rolling back observer registrations if
         * an error occurred during {@link Cleaner} construction. This method clears the {@link #sources}
         * field immediately for letting the garbage collector to collect the sources in the event where
         * this {@code Cleaner} would live longer than expected.
         *
         * @param  ws       a copy of {@link #sources}. Can not be null.
         * @param  i        index after the last source to stop observing.
         * @param  failure  if this method is invoked because an exception occurred, that exception.
         */
        private void unregister(final WritableRenderedImage[] ws, int i, RuntimeException failure) {
            sources = null;                     // Let GC to its work in case of error in this method.
            while (--i >= 0) try {
                ws[i].removeTileObserver(this);
            } catch (RuntimeException e) {
                if (failure == null) failure = e;
                else failure.addSuppressed(e);
            }
            if (failure != null) {
                throw failure;
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
     * The sources of this image, or {@code null} if unknown.
     *
     * @see #getSource(int)
     */
    private final RenderedImage[] sources;

    /**
     * The sample model shared by all tiles in this image.
     * The {@linkplain SampleModel#getWidth() sample model width}
     * determines this {@linkplain #getTileWidth() image tile width},
     * and the {@linkplain SampleModel#getHeight() sample model height}
     * determines this {@linkplain #getTileHeight() image tile height}.
     *
     * <div class="note"><b>Design note:</b>
     * {@code ComputedImage} requires the sample model to have exactly the desired tile size
     * otherwise tiles created by {@link #createTile(int, int)} will consume more memory
     * than needed.</div>
     */
    protected final SampleModel sampleModel;

    /**
     * Creates an initially empty image with the given sample model.
     * The default tile size will be the width and height of the given sample model
     * (this default setting minimizes the amount of memory consumed by {@link #createTile(int, int)}).
     * This constructor automatically registers a {@link TileObserver}
     * for all sources that are {@link WritableRenderedImage} instances.
     *
     * @param  sampleModel  the sample model shared by all tiles in this image.
     * @param  sources      sources of this image (may be an empty array), or a null array if unknown.
     */
    protected ComputedImage(final SampleModel sampleModel, RenderedImage... sources) {
        ArgumentChecks.ensureNonNull("sampleModel", sampleModel);
        this.sampleModel = sampleModel;
        /*
         * Verify the `sources` argument validity and opportunistically collect all writable sources
         * in a separated array. If at the end it appears that the two arrays have the same content,
         * the same array will be shared by this `ComputedImage` and its `TileObserver`.
         */
        WritableRenderedImage[] ws = null;
        if (sources != null) {
            sources = sources.clone();
            int count = 0;
            for (int i=0; i<sources.length; i++) {
                final RenderedImage source = sources[i];
                ArgumentChecks.ensureNonNullElement("sources", i, source);
                if (source instanceof WritableRenderedImage) {
                    if (ws == null) {
                        ws = new WritableRenderedImage[sources.length - i];
                    }
                    ws[count++] = (WritableRenderedImage) source;
                }
            }
            if (count == sources.length) {
                sources = ws;                   // The two arrays have the same content; share the same one.
            } else {
                ws = ArraysExt.resize(ws, count);
            }
        }
        this.sources = sources;             // Note: null value does not have same meaning than empty array.
        reference = new Cleaner(this, ws);  // Create cleaner last after all arguments have been validated.
    }

    /**
     * Returns the source at the given index.
     *
     * @param  index  index of the desired source.
     * @return source at the given index.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    protected final RenderedImage getSource(final int index) {
        if (sources != null) return sources[index];
        else throw new IndexOutOfBoundsException();
    }

    /**
     * Returns the immediate sources of image data for this image (may be {@code null}).
     * This method returns all sources specified at construction time.
     *
     * @return the immediate sources, or an empty vector is none, or {@code null} if unknown.
     */
    @Override
    @SuppressWarnings("UseOfObsoleteCollectionType")
    public Vector<RenderedImage> getSources() {
        return (sources != null) ? new Vector<>(Arrays.asList(sources)) : null;
    }

    /**
     * Returns the sample model associated with this image.
     * All rasters returned from this image will have this sample model.
     * In {@code ComputedImage} implementation, the sample model determines the tile size
     * (this is not necessarily true for all {@link RenderedImage} implementations).
     *
     * @return the sample model of this image.
     */
    @Override
    public SampleModel getSampleModel() {
        return sampleModel;
    }

    /**
     * Returns the width of tiles in this image. The default implementation returns {@link SampleModel#getWidth()}.
     *
     * <div class="note"><b>Note:</b>
     * a raster can have a smaller width than its sample model, for example when a raster is a view over a subregion
     * of another raster. But this is not recommended in the particular case of this {@code ComputedImage} class,
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
     * of another raster. But this is not recommended in the particular case of this {@code ComputedImage} class,
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
     * @throws IllegalArgumentException if a given tile index is out of bounds.
     * @throws ImagingOpException if an error occurred while computing the image.
     */
    @Override
    public final Raster getTile(final int tileX, final int tileY) {
        final TileCache.Key key = new TileCache.Key(reference, tileX, tileY);
        final Cache<TileCache.Key,Raster> cache = TileCache.GLOBAL;
        Raster tile = cache.peek(key);
        if (tile == null || reference.isDirty(key)) {
            int min;
            ArgumentChecks.ensureBetween("tileX", (min = getMinTileX()), min + getNumXTiles() - 1, tileX);
            ArgumentChecks.ensureBetween("tileY", (min = getMinTileY()), min + getNumYTiles() - 1, tileY);
            final Cache.Handler<Raster> handler = cache.lock(key);
            try {
                tile = handler.peek();
                if (tile == null || reference.isDirty(key)) {
                    final WritableRaster previous = (tile instanceof WritableRaster) ? (WritableRaster) tile : null;
                    Exception cause = null;
                    tile = null;
                    try {
                        tile = computeTile(tileX, tileY, previous);
                    } catch (ImagingOpException e) {
                        throw e;                            // Let that kind of exception propagate.
                    } catch (Exception e) {
                        cause = e;
                    }
                    if (tile == null) {
                        throw (ImagingOpException) new ImagingOpException(Resources.format(
                                Resources.Keys.CanNotComputeTile_2, tileX, tileY)).initCause(cause);
                    }
                    reference.addTile(key);
                }
            } finally {
                handler.putAndUnlock(tile);     // Must be invoked even if an exception occurred.
            }
        }
        return tile;
    }

    /**
     * Invoked when a tile need to be computed or updated. This method is invoked by {@link #getTile(int, int)}
     * when the requested tile is not in the cache, or when a writable source notified us that its data changed.
     * The returned tile will be automatically cached.
     *
     * <p>A typical implementation is as below:</p>
     * {@preformat java
     *     &#64;Override
     *     protected Raster computeTile(int tileX, int tileY, WritableRaster tile) {
     *         if (tile == null) {
     *             tile = createTile(tileX, tileY);
     *         }
     *         // Do calculation here and write results in tile.
     *         return tile;
     *     }
     * }
     *
     * @param  tileX     the column index of the tile to compute.
     * @param  tileY     the row index of the tile to compute.
     * @param  previous  if the tile already exists but needs to be updated, the tile to update. Otherwise {@code null}.
     * @return computed tile for the given indices. May be the {@code previous} tile after update but can not be null.
     * @throws Exception if an error occurred while computing the tile.
     */
    protected abstract Raster computeTile(int tileX, int tileY, WritableRaster previous) throws Exception;

    /**
     * Creates an initially empty tile at the given tile grid position.
     * This is a helper method for {@link #computeTile(int, int, WritableRaster)} implementations.
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
     * Advises this image that its tiles will no longer be requested. This method removes all
     * tiles from the cache and stops observation of {@link WritableRenderedImage} sources.
     * This image should not be used anymore after this method call.
     *
     * <p><b>Note:</b> keep in mind that this image may be referenced as a source of other images.
     * In case of doubt, it may be safer to rely on the garbage collector instead than invoking this method.</p>
     */
    public void dispose() {
        reference.dispose();
    }
}
