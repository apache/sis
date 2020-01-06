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
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
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
import org.apache.sis.coverage.grid.GridExtent;     // For javadoc


/**
 * An image with tiles computed on-the-fly and cached for future reuse.
 * Computations are performed on a tile-by-tile basis (potentially in different threads)
 * and the results are stored in a cache shared by all images in the runtime environment.
 * Tiles may be discarded at any time or may become dirty if a source has been modified,
 * in which case those tiles will be recomputed when needed again.
 *
 * <p>{@code ComputedImage} may have an arbitrary number of source images, including zero.
 * A {@link TileObserver} is automatically registered to all sources that are instances of
 * {@link WritableRenderedImage}. If one of those sources sends a change event, then all
 * {@code ComputedImage} tiles that may be impacted by that change are marked as <cite>dirty</cite>
 * and will be computed again when needed.</p>
 *
 * <p>When this {@code ComputedImage} is garbage collected, all cached tiles are discarded
 * and the above-cited {@link TileObserver} is automatically removed from all sources.
 * This cleanup can be requested without waiting for garbage collection by invoking the
 * {@link #dispose()} method, but that call should be done only if the caller is certain
 * that this {@code ComputedImage} will not be used anymore.</p>
 *
 * <h2>Pixel coordinate system</h2>
 * Default implementation assumes that the pixel in upper-left left corner is located at coordinates (0,0).
 * This assumption is consistent with {@link org.apache.sis.coverage.grid.GridCoverage#render(GridExtent)}
 * contract, which produces an image located at (0,0) when the image region matches the {@code GridExtent}.
 * However subclasses can use a non-zero origin by overriding the methods documented in the
 * <cite>Sub-classing</cite> section below.
 *
 * <p>If this {@code ComputedImage} does not have any {@link WritableRenderedImage} source, then there is
 * no other assumption on the pixel coordinate system. But if there is writable sources, then the default
 * implementation assumes that source images occupy the same region as this {@code ComputedImage}:
 * all pixels at coordinates (<var>x</var>, <var>y</var>) in this {@code ComputedImage} depend on pixels
 * at the same (<var>x</var>, <var>y</var>) coordinates in the source images,
 * possibly expanded to neighborhood pixels as described in {@link #SOURCE_PADDING_PROPERTY}.
 * If this assumption does not hold, then subclasses should override the
 * {@link #sourceTileChanged(RenderedImage, int, int)} method.</p>
 *
 * <h2>Sub-classing</h2>
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
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public abstract class ComputedImage extends PlanarImage {
    /**
     * The property for declaring the amount of additional source pixels needed on each side of a destination pixel.
     * This property can be used for calculations that require only a fixed rectangular source region around a source
     * pixel in order to compute each destination pixel. A given destination pixel (<var>x</var>, <var>y</var>) may be
     * computed from the neighborhood of source pixels beginning at
     * (<var>x</var> - {@link Insets#left},
     *  <var>y</var> - {@link Insets#top}) and extending to
     * (<var>x</var> + {@link Insets#right},
     *  <var>y</var> + {@link Insets#bottom}) inclusive.
     * Those {@code left}, {@code top}, {@code right} and {@code bottom} attributes can be positive, zero or negative,
     * but their sums shall be positive with ({@code left} + {@code right}) ≥ 0 and ({@code top} + {@code bottom}) ≥ 0.
     *
     * <p>The property value shall be an instance of {@link Insets} or {@code Insets[]}.
     * The array form can be used when a different padding is required for each source image.
     * In that case, the image source index is used as the index for accessing the {@link Insets} element in the array.
     * Null or {@linkplain java.awt.Image#UndefinedProperty undefined} elements mean that no padding is applied.
     * If the array length is shorter than the number of source images, missing elements are considered as null.</p>
     *
     * @see #getProperty(String)
     * @see #sourceTileChanged(RenderedImage, int, int)
     */
    public static final String SOURCE_PADDING_PROPERTY = "sourcePadding";

    /**
     * Whether a tile in the cache is ready for use or needs to be recomputed because one if its sources
     * changed its data. If the tile is checkout out for a write operation, the write operation will have
     * precedence over the dirty state.
     */
    private enum TileStatus {
        /** The tile, if present, is ready for use. */
        VALID,

        /** The tile needs to be recomputed because at least one source changed its data. */
        DIRTY,

        /** The tile has been checked out for a write operation. */
        WRITABLE
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
         * Sets the status of the specified tile, discarding any previous status.
         */
        final void setTileStatus(final TileCache.Key key, final TileStatus status) {
            synchronized (cachedTiles) {
                cachedTiles.put(key, status);
            }
        }

        /**
         * Returns the status of the tile at the specified indices.
         * The main status of interest are:
         * <ul>
         *   <li>{@link TileStatus#DIRTY}    — if the tile needs to be recomputed.</li>
         *   <li>{@link TileStatus#WRITABLE} — if the tile is currently checked out for writing.</li>
         * </ul>
         */
        final TileStatus getTileStatus(final TileCache.Key key) {
            synchronized (cachedTiles) {
                return cachedTiles.get(key);
            }
        }

        /**
         * Adds in the given list the indices of all tiles which are checked out for writing.
         * If the given list is {@code null}, then this method stops the search at the first
         * tile checked out.
         *
         * @param  indices  the list where to add indices, or {@code null} if none.
         * @return whether at least one tile is checked out for writing.
         */
        final boolean getWritableTileIndices(final List<Point> indices) {
            synchronized (cachedTiles) {
                for (final Map.Entry<TileCache.Key, TileStatus> entry : cachedTiles.entrySet()) {
                    if (entry.getValue() == TileStatus.WRITABLE) {
                        if (indices == null) return true;
                        indices.add(entry.getKey().indices());
                    }
                }
            }
            return (indices != null) && !indices.isEmpty();
        }

        /**
         * Marks all tiles in the given range of indices as in need of being recomputed.
         * This method is invoked when some tiles of at least one source image changed.
         * All arguments, including maximum values, are inclusive.
         *
         * @see ComputedImage#markDirtyTiles(Rectangle)
         */
        final void markDirtyTiles(final int minTileX, final int minTileY, final int maxTileX, final int maxTileY) {
            synchronized (cachedTiles) {
                for (int tileY = minTileY; tileY <= maxTileY; tileY++) {
                    for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
                        final TileCache.Key key = new TileCache.Key(this, tileX, tileY);
                        cachedTiles.replace(key, TileStatus.VALID, TileStatus.DIRTY);
                    }
                }
            }
        }

        /**
         * Invoked when a source is changing the content of one of its tile.
         * This method is interested only in events fired after the change is done.
         * The tiles that depend on the modified tile are marked in need to be recomputed.
         *
         * @param source          the image that own the tile which is about to be updated.
         * @param tileX           the <var>x</var> index of the tile that is being updated.
         * @param tileY           the <var>y</var> index of the tile that is being updated.
         * @param willBeWritable  if {@code true}, the tile is grabbed for writing; otherwise it is being released.
         */
        @Override
        public void tileUpdate(final WritableRenderedImage source, int tileX, int tileY, final boolean willBeWritable) {
            if (!willBeWritable) {
                final ComputedImage target = get();
                if (target != null) {
                    target.sourceTileChanged(source, tileX, tileY);
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
         * that were owned by the enclosing image and stops observing all sources.
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
         * field immediately for allowing the garbage collector to release the sources in the event where
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
     * time than this image and its {@link Cleaner#dispose()} method may never be invoked.
     */
    private final Cleaner reference;

    /**
     * The sources of this image, or {@code null} if unknown. This array contains all sources.
     * By contrast the {@link Cleaner#sources} array contains only the modifiable sources, for
     * which we listen for changes.
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
            /*
             * If `count` is 0, then `ws` is null while `sources` is non-null. This is intentional:
             * a null `sources` array does not have the same meaning than an empty `sources` array.
             * In the case of `ws` however, the difference does not matter so we keep it to null.
             */
            if (count != 0) {
                if (count == sources.length) {
                    sources = ws;               // The two arrays have the same content; share the same array.
                } else {
                    ws = ArraysExt.resize(ws, count);
                }
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
     * Returns the property of the given name if it is of the given type, or {@code null} otherwise.
     * If the property value depends on the source image, then it can be an array of type {@code T[]},
     * in which case this method will return the element at the source index.
     *
     * @param  <T>     compile-tile value of {@code type} argument.
     * @param  type    class of the property to get.
     * @param  name    name of the property to get.
     * @param  source  the source image if the property may depend on the source.
     * @return requested property if it is an instance of the specified type, or {@code null} otherwise.
     */
    @SuppressWarnings("unchecked")
    private <T> T getProperty(final Class<T> type, final String name, final RenderedImage source) {
        Object value = getProperty(name);
        if (type.isInstance(value)) {
            return (T) value;
        }
        if (sources != null && value instanceof Object[]) {
            final Object[] array = (Object[]) value;
            final int n = Math.min(sources.length, array.length);
            for (int i=0; i<n; i++) {
                if (sources[i] == source) {
                    value = array[i];
                    if (type.isInstance(value)) {
                        return (T) value;
                    }
                }
            }
        }
        return null;
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
     *   <li>If the requested tile is present in the cache and is not dirty, then that tile is returned immediately.</li>
     *   <li>Otherwise if the requested tile is being computed in another thread, then this method blocks
     *       until the other thread completed its work and returns its result. If the other thread failed
     *       to compute the tile, an {@link ImagingOpException} is thrown.</li>
     *   <li>Otherwise this method computes the tile and caches the result before to return it.
     *       If an error occurred, an {@link ImagingOpException} is thrown.</li>
     * </ol>
     *
     * <h4>Race conditions with write operations</h4>
     * If this image implements the {@link WritableRenderedImage} interface, then a user may have acquired
     * the tile for a write operation outside the {@link #computeTile computeTile(…)} method. In such case,
     * there is no consistency guarantees on sample values: the tile returned by this method may show data
     * in an unspecified stage during the write operation. This situation may be detected by checking if
     * {@link #isTileWritable(int, int) isTileWritable(tileX, tileY)} returns {@code true}.
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
        if (tile == null || reference.getTileStatus(key) == TileStatus.DIRTY) {
            int min;
            ArgumentChecks.ensureBetween("tileX", (min = getMinTileX()), min + getNumXTiles() - 1, tileX);
            ArgumentChecks.ensureBetween("tileY", (min = getMinTileY()), min + getNumYTiles() - 1, tileY);
            final Cache.Handler<Raster> handler = cache.lock(key);
            try {
                tile = handler.peek();
                if (tile == null || reference.getTileStatus(key) == TileStatus.DIRTY) {
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
                    reference.setTileStatus(key, TileStatus.VALID);
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
     * Returns whether any tile is checked out for writing.
     * This method always returns {@code false} for read-only images, but may return {@code true}
     * if this {@code ComputedImage} is also a {@link WritableRenderedImage}.
     *
     * @return {@code true} if any tiles are checked out for writing; {@code false} otherwise.
     *
     * @see #markWritableTile(int, int, boolean)
     * @see WritableRenderedImage#hasTileWriters()
     */
    public boolean hasTileWriters() {
        return reference.getWritableTileIndices(null);
    }

    /**
     * Returns whether a tile is currently checked out for writing.
     * This method always returns {@code false} for read-only images, but may return {@code true}
     * if this {@code ComputedImage} is also a {@link WritableRenderedImage}.
     *
     * @param  tileX the X index of the tile.
     * @param  tileY the Y index of the tile.
     * @return {@code true} if specified tile is checked out for writing; {@code false} otherwise.
     *
     * @see #markWritableTile(int, int, boolean)
     * @see WritableRenderedImage#isTileWritable(int, int)
     */
    public boolean isTileWritable(final int tileX, final int tileY) {
        return reference.getTileStatus(new TileCache.Key(reference, tileX, tileY)) == TileStatus.WRITABLE;
    }

    /**
     * Returns an array of Point objects indicating which tiles are checked out for writing, or {@code null} if none.
     * This method always returns {@code null} for read-only images, but may return a non-empty array
     * if this {@code ComputedImage} is also a {@link WritableRenderedImage}.
     *
     * @return an array containing the locations of tiles that are checked out for writing, or {@code null} if none.
     *
     * @see #markWritableTile(int, int, boolean)
     * @see WritableRenderedImage#getWritableTileIndices()
     */
    public Point[] getWritableTileIndices() {
        final List<Point> indices = new ArrayList<>();
        if (reference.getWritableTileIndices(indices)) {
            return indices.toArray(new Point[indices.size()]);
        }
        return null;
    }

    /**
     * Marks a tile as checkout out for writing. This method is provided for subclasses that also implement
     * the {@link WritableRenderedImage} interface. This method can be used as below:
     *
     * {@preformat java
     *     class MyImage extends ComputedImage implements WritableRenderedImage {
     *         // Constructor omitted for brevity.
     *
     *         &#64;Override
     *         public WritableRaster getWritableTile(int tileX, int tileY) {
     *             WritableRaster raster = ...;             // Get the writable tile here.
     *             markWritableTile(tileX, tileY, true);
     *             return raster;
     *         }
     *
     *         &#64;Override
     *         public void releaseWritableTile(int tileX, int tileY) {
     *             markWritableTile(tileX, tileY, false);
     *             // Release the raster here.
     *         }
     *     }
     * }
     *
     * @param  tileX    the X index of the tile to acquire or release.
     * @param  tileY    the Y index of the tile to acquire or release.
     * @param  writing  {@code true} for acquiring the tile, or {@code false} for releasing it.
     *
     * @see WritableRenderedImage#getWritableTile(int, int)
     * @see WritableRenderedImage#releaseWritableTile(int, int)
     */
    protected void markWritableTile(final int tileX, final int tileY, final boolean writing) {
        final TileCache.Key key = new TileCache.Key(reference, tileX, tileY);
        reference.setTileStatus(key, writing ? TileStatus.WRITABLE : TileStatus.VALID);
    }

    /**
     * Marks all tiles in the given range of indices as in need of being recomputed.
     * The tiles will not be recomputed immediately, but only on next invocation of
     * {@link #getTile(int, int) getTile(tileX, tileY)} if the {@code (tileX, tileY)} indices
     * are {@linkplain Rectangle#contains(int, int) contained} if the specified rectangle.
     *
     * <p>Subclasses can invoke this method when the tiles in the given range depend on source data
     * that changed, typically (but not necessarily) {@linkplain #getSources() source images}.
     * Note that there is no need to invoke this method if the source images are instances of
     * {@link WritableRenderedImage}, because {@code ComputedImage} already has {@link TileObserver}
     * for them.</p>
     *
     * @param  tiles  indices of tiles to mark as dirty.
     */
    protected void markDirtyTiles(final Rectangle tiles) {
        reference.markDirtyTiles(tiles.x, tiles.y,
                   Math.addExact(tiles.x, tiles.width  - 1),
                   Math.addExact(tiles.y, tiles.height - 1));
    }

    /**
     * Invoked when a tile of a source image has been updated. This method should {@linkplain #markDirtyTiles
     * mark as dirty} all tiles of this {@code ComputedImage} that depend on the updated tile.
     *
     * <p>The default implementation assumes that source images use pixel coordinate systems aligned with this
     * {@code ComputedImage} in such a way that all pixels at coordinates (<var>x</var>, <var>y</var>) in the
     * {@code source} image are used for calculation of pixels at the same (<var>x</var>, <var>y</var>) coordinates
     * in this {@code ComputedImage}, possibly expanded to neighborhood pixels if the {@value #SOURCE_PADDING_PROPERTY}
     * property is defined. If this assumption does not hold, then subclasses should override this method and invoke
     * {@link #markDirtyTiles(Rectangle)} themselves.</p>
     *
     * @param source  the image that own the tile which has been updated.
     * @param tileX   the <var>x</var> index of the tile that has been updated.
     * @param tileY   the <var>y</var> index of the tile that has been updated.
     */
    protected void sourceTileChanged(final RenderedImage source, final int tileX, final int tileY) {
        final long sourceWidth  = source.getTileWidth();
        final long sourceHeight = source.getTileHeight();
        final long targetWidth  = this  .getTileWidth();
        final long targetHeight = this  .getTileHeight();
        final long tx           = tileX * sourceWidth  + source.getTileGridXOffset() - getTileGridXOffset();
        final long ty           = tileY * sourceHeight + source.getTileGridYOffset() - getTileGridYOffset();
        final Insets b = getProperty(Insets.class, SOURCE_PADDING_PROPERTY, source);
        reference.markDirtyTiles(Numerics.clamp(Math.floorDiv(tx - (b == null ? 0 : b.left), targetWidth)),
                                 Numerics.clamp(Math.floorDiv(ty - (b == null ? 0 : b.top),  targetHeight)),
                                 Numerics.clamp(Math.floorDiv(tx + (b == null ? 0 : b.right)  + sourceWidth  - 1, targetWidth)),
                                 Numerics.clamp(Math.floorDiv(ty + (b == null ? 0 : b.bottom) + sourceHeight - 1, targetHeight)));
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
