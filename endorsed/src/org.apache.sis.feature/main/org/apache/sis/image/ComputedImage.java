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

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;
import java.util.Objects;
import java.lang.ref.Reference;
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
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Disposable;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.privy.Numerics;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.coverage.grid.GridExtent;     // For javadoc
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.coverage.privy.ImageUtilities;


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
 * {@code ComputedImage} tiles that may be impacted by that change are marked as <dfn>dirty</dfn>
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
 * However, subclasses can use a non-zero origin by overriding the methods documented in the
 * <i>Sub-classing</i> section below.
 *
 * <p>If this {@code ComputedImage} does not have any {@link WritableRenderedImage} source, then there is
 * no other assumption on the pixel coordinate system. But if there is writable sources, then the default
 * implementation assumes that source images occupy the same region as this {@code ComputedImage}:
 * all pixels at coordinates (<var>x</var>, <var>y</var>) in this {@code ComputedImage} depend on pixels
 * at the same (<var>x</var>, <var>y</var>) coordinates in the source images,
 * possibly shifted or expanded to neighborhood pixels as described in {@link #SOURCE_PADDING_KEY}.
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
 * <h2>Writable computed images</h2>
 * {@code ComputedImage} can itself be a {@link WritableRenderedImage} if subclasses decide so.
 * A writable computed image is an image which can retro-propagate sample value changes to the source images.
 * This class provides {@link #hasTileWriters()}, {@link #getWritableTileIndices()}, {@link #isTileWritable(int, int)}
 * and {@link #markTileWritable(int, int, boolean)} methods for {@link WritableRenderedImage} implementations convenience.
 *
 * <p>Apache SIS <a href="https://issues.apache.org/jira/browse/SIS-487">does not yet define a synchronization policy</a>
 * between {@link #getTile(int, int) getTile(…)} method and {@link WritableRenderedImage#getWritableTile(int, int)
 * WritableRenderedImage.getWritableTile}/{@link WritableRenderedImage#releaseWritableTile releaseWritableTile(…)} methods.
 * For example if a call to {@code getTile(tileX, tileY)} is followed by a call to {@code getWritableTile(tileX, tileY)}
 * in another thread, there is no guarantee about whether or not the sample values seen in the {@link Raster} would be
 * isolated from the write operations done concurrently in the {@link WritableRaster}.
 * A future SIS version may define a policy (possibly based on {@link java.util.concurrent.locks.ReadWriteLock}).</p>
 *
 * <p>Note that despite above-cited issue, all methods in this {@code ComputedImage} class are thread-safe.
 * What is not thread-safe is writing into a {@link WritableRaster} from outside the {@link #computeTile
 * computeTile(…)} method, or reading a {@link Raster} after it {@linkplain #markDirtyTiles became dirty}
 * if the change to dirty state happened after the call to {@link #getTile(int, int) getTile(…)}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.1
 */
public abstract class ComputedImage extends PlanarImage implements Disposable {
    /**
     * The property for declaring the number of additional source pixels needed on each side of a destination pixel.
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
    public static final String SOURCE_PADDING_KEY = "org.apache.sis.SourcePadding";

    /**
     * Weak reference to this image, also used as a cleaner when the image is garbage-collected.
     * This reference is retained in {@link TileCache#GLOBAL}. Note that if that cache does not
     * cache any tile for this image, then that {@link ComputedTiles} may be garbage-collected
     * at the same time as this image and its {@link ComputedTiles#dispose()} method may never
     * be invoked.
     */
    private final ComputedTiles reference;

    /**
     * The sources of this image, or {@code null} if unknown. This array contains all sources.
     * By contrast the {@link ComputedTiles#sources} array contains only the modifiable sources,
     * for which we listen for changes.
     *
     * @see #getSource(int)
     */
    private final RenderedImage[] sources;

    /**
     * If the computed image shall be written in an existing image, that image. Otherwise {@code null}.
     * If non-null, the sample model of this image shall be equal to {@link #sampleModel} and the tile
     * indices &amp; pixel coordinate systems shall be aligned.
     *
     * <p>The destination image may be larger or smaller than this {@code ComputedImage}, by containing
     * more or less tiles (the presence or absence of a tile is a "all or nothing" decision). When this
     * class needs to compute a tile, one of the following choices is executed:</p>
     *
     * <ul class="verbose">
     *   <li>If the ({@code tileX}, {@code tileY}) indices of the tile to compute are valid tile indices of
     *       {@code destination} image, then the {@linkplain WritableRenderedImage#getWritableTile(int,int)
     *       destination tile is acquired}, given to {@link #computeTile(int, int, WritableRaster)} method
     *       and finally {@linkplain WritableRenderedImage#releaseWritableTile(int,int) released}.</li>
     *   <li>Otherwise {@link #computeTile(int, int, WritableRaster)} is invoked with a {@code null} tile.</li>
     * </ul>
     *
     * If this field is set to a non-null value, then this assignment should be done
     * soon after construction time before any tile computation started.
     *
     * <h4>Note on interaction with tile cache</h4>
     * The use of a destination image may produce unexpected result if {@link #computeTile(int, int, WritableRaster)}
     * is invoked two times or more for the same destination tile. It may look like a problem because computed tiles
     * can be discarded and recomputed at any time. However, this problem should not happen because tiles computed by
     * this {@code ComputedImage} will not be discarded as long as {@code destination} has a reference to that tile.
     * If a {@code ComputedImage} tile has been discarded, then it implies that the corresponding {@code destination}
     * tile has been discarded as well, in which case the tile computation will restart from scratch; it will not be
     * a recomputation of only this {@code ComputedImage} on top of an old {@code destination} tile.
     *
     * @see #setDestination(WritableRenderedImage)
     */
    private WritableRenderedImage destination;

    /**
     * The sample model shared by all tiles in this image.
     * The {@linkplain SampleModel#getWidth() sample model width}
     * determines this {@linkplain #getTileWidth() image tile width},
     * and the {@linkplain SampleModel#getHeight() sample model height}
     * determines this {@linkplain #getTileHeight() image tile height}.
     *
     * <h4>Design note</h4>
     * {@code ComputedImage} requires the sample model to have exactly the desired tile size
     * otherwise tiles created by {@link #createTile(int, int)} will consume more memory
     * than needed.
     *
     * @see #getSampleModel()
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
    @SuppressWarnings("this-escape")        // `this` escaped as weak reference only.
    protected ComputedImage(final SampleModel sampleModel, RenderedImage... sources) {
        this.sampleModel = Objects.requireNonNull(sampleModel);
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
             * a null `sources` array does not have the same meaning as an empty `sources` array.
             * In the case of `ws` however, the difference does not matter so we keep it to null.
             */
            if (count != 0) {
                if (count == sources.length) {
                    sources = ws;                   // The two arrays have the same content; share the same array.
                } else {
                    ws = ArraysExt.resize(ws, count);
                }
            }
        }
        this.sources = sources;                     // Note: null value does not have same meaning as empty array.
        reference = new ComputedTiles(this, ws);    // Create cleaner last after all arguments have been validated.
    }

    /**
     * Returns a weak reference to this image. Using weak reference instead of strong reference may help to
     * reduce memory usage when recomputing the image is cheap. This method should not be public because the
     * returned instance implements public interfaces that caller could invoke.
     */
    final Reference<ComputedImage> reference() {
        return reference;
    }

    /**
     * Sets an existing image where to write the computation result. The sample model of specified image shall
     * be equal to {@link #sampleModel} and the tile indices &amp; pixel coordinate systems shall be aligned.
     * However, the target image may be larger or smaller than this {@code ComputedImage}, by containing more
     * or less tiles (the presence or absence of a tile is a "all or nothing" decision). When this class needs
     * to compute a tile, one of the following choices is executed:
     *
     * <ul class="verbose">
     *   <li>If the ({@code tileX}, {@code tileY}) indices of the tile to compute are valid tile indices
     *       of {@code target} image, then the {@linkplain WritableRenderedImage#getWritableTile(int,int)
     *       destination tile is acquired}, given to {@link #computeTile(int, int, WritableRaster)} method
     *       and finally {@linkplain WritableRenderedImage#releaseWritableTile(int,int) released}.</li>
     *   <li>Otherwise {@link #computeTile(int, int, WritableRaster)} is invoked with a {@code null} tile.</li>
     * </ul>
     *
     * If this method is invoked, then is should be done soon after construction time
     * before any tile computation starts.
     *
     * @param  target  the destination image, or {@code null} if none.
     */
    final void setDestination(final WritableRenderedImage target) {
        if (destination != null) {
            throw new IllegalStateException(Errors.format(Errors.Keys.AlreadyInitialized_1, "destination"));
        }
        if (target != null) {
            if (!sampleModel.equals(target.getSampleModel())) {
                throw new IllegalArgumentException(Resources.format(Resources.Keys.MismatchedSampleModel));
            }
            if (target.getTileGridXOffset() != getTileGridXOffset() ||
                target.getTileGridYOffset() != getTileGridYOffset())
            {
                throw new IllegalArgumentException(Resources.format(Resources.Keys.MismatchedTileGrid));
            }
            destination = target;
        }
    }

    /**
     * Returns the destination, or {@code null} if none.
     */
    final WritableRenderedImage getDestination() {
        return destination;
    }

    /**
     * Returns the source at index 0 without any check. This method is invoked by subclasses who know that a
     * single source exist. This method should not be in public API because of the absence of verification.
     */
    final RenderedImage getSource() {
        return sources[0];
    }

    /**
     * Returns the sources as an array.
     * A future version may remove this method if Java allows unmodifiable arrays.
     */
    final RenderedImage[] getSourceArray() {
        return sources.clone();
    }

    /**
     * Returns the number of sources, or 0 if none or unknown.
     * This is the size of the vector returned by {@link #getSources()}.
     *
     * @return number of sources, or 0 if none or unknown.
     */
    final int getNumSources() {
        return (sources != null) ? sources.length : 0;
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
    public final SampleModel getSampleModel() {
        return sampleModel;
    }

    /**
     * Returns the width of tiles in this image.
     * In {@code ComputedImage} implementation, this is fixed to {@link SampleModel#getWidth()}.
     *
     * <h4>Design note</h4>
     * In theory it is legal to have a tile width smaller than the sample model width,
     * for example when a raster is a view over a subregion of another raster.
     * But this is not allowed in {@code ComputedImage} class, because it would
     * cause {@link #createTile(int, int)} to consume more memory than necessary.
     *
     * @return the width of this image in pixels.
     */
    @Override
    public final int getTileWidth() {
        return sampleModel.getWidth();
    }

    /**
     * Returns the height of tiles in this image.
     * In {@code ComputedImage} implementation, this is fixed to {@link SampleModel#getHeight()}.
     *
     * <h4>Design note</h4>
     * In theory it is legal to have a tile height smaller than the sample model height,
     * for example when a raster is a view over a subregion of another raster.
     * But this is not allowed in {@code ComputedImage} class, because it would
     * cause {@link #createTile(int, int)} to consume more memory than necessary.
     *
     * @return the height of this image in pixels.
     */
    @Override
    public final int getTileHeight() {
        return sampleModel.getHeight();
    }

    /**
     * Verifies that an index is inside the expected range of tile indices.
     * If the index is out of bounds, then this method throws an {@code IndexOutOfBoundsException}
     * for consistency with {@link java.awt.image.BufferedImage#getTile(int, int)} public contract.
     *
     * @throws IndexOutOfBoundsException if the given tile index is out of bounds.
     */
    private static void checkTileIndex(final String name, final int min, final int count, final int value) {
        final int max = min + count;
        if (value < min || value >= max) {
            throw new IndexOutOfBoundsException(Errors.format(
                    Errors.Keys.ValueOutOfRange_4, name, min, max - 1, value));
        }
    }

    /**
     * Returns a tile of this image, computing it when needed.
     * This method performs the first of the following actions that apply:
     *
     * <ol>
     *   <li>If the requested tile is present in the cache and is not dirty, then that tile is returned immediately.</li>
     *   <li>Otherwise if the requested tile is being {@linkplain #computeTile computed} in another thread,
     *       then this method blocks until the other thread completed its work and returns its result.
     *       If the other thread failed to compute the tile, an {@link ImagingOpException} is thrown.</li>
     *   <li>Otherwise this method computes the tile and caches the result before to return it.
     *       If an error occurred, an {@link ImagingOpException} is thrown.</li>
     * </ol>
     *
     * <h4>Race conditions with write operations</h4>
     * If this image implements the {@link WritableRenderedImage} interface, then a user may acquire the same
     * tile for a write operation after this method returned. In such case there is no consistency guarantee
     * on sample values: the tile returned by this method may show data in an unspecified stage during the
     * write operation. A synchronization policy <a href="https://issues.apache.org/jira/browse/SIS-487">may
     * be defined in a future Apache SIS version</a>.
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
        if (tile == null || reference.isTileDirty(key)) {
            /*
             * Tile not found in the cache or in need to be recomputed. Validate given arguments
             * only now (if the tile was found, it would have implied that the indices are valid).
             * We will need to check the cache again after we got the lock in case computation has
             * happened in the short time between above check and lock acquisition.
             */
            checkTileIndex("tileX", getMinTileX(), getNumXTiles(), tileX);
            checkTileIndex("tileY", getMinTileY(), getNumYTiles(), tileY);
            Throwable error = null;
            final Cache.Handler<Raster> handler = cache.lock(key);
            try {
                tile = handler.peek();
                final boolean marked = reference.trySetComputing(key);              // May throw ImagingOpException.
                if (marked || tile == null) {
                    /*
                     * The requested tile needs to be computed. If a destination image has been specified
                     * and the tile indices are valid for that destination, we will use the tile provided
                     * by that destination. The write operation shall happen between `getWritableTile(…)`
                     * and `releaseWritableTile(…)` method calls.
                     */
                    int min;
                    @SuppressWarnings("LocalVariableHidesMemberVariable")
                    final WritableRenderedImage destination = this.destination;     // Protect from change (paranoiac).
                    final boolean writeInDestination = (destination != null)
                            && (tileX >= (min = destination.getMinTileX()) && tileX < min + destination.getNumXTiles())
                            && (tileY >= (min = destination.getMinTileY()) && tileY < min + destination.getNumYTiles());

                    final WritableRaster previous;
                    if (writeInDestination) {
                        previous = destination.getWritableTile(tileX, tileY);
                    } else if (tile instanceof WritableRaster) {
                        previous = (WritableRaster) tile;
                    } else {
                        previous = null;
                    }
                    /*
                     * Actual computation.
                     */
                    try {
                        tile = computeTile(tileX, tileY, previous);
                    } catch (Exception e) {
                        tile = null;
                        error = Exceptions.unwrap(e);
                    } finally {
                        if (writeInDestination) {
                            destination.releaseWritableTile(tileX, tileY);
                        }
                    }
                    if (marked) {
                        reference.endWrite(key, error == null);
                    }
                }
            } finally {
                handler.putAndUnlock(tile);     // Must be invoked even if an exception occurred.
            }
            if (tile == null) {                 // Null in case of exception or if `computeTile(…)` returned null.
                if (!(error instanceof ImagingOpException)) {
                    error = new ImagingOpException(key.error(Resources.Keys.CanNotComputeTile_2)).initCause(error);
                }
                throw (ImagingOpException) error;
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
     * {@snippet lang="java" :
     *     @Override
     *     protected Raster computeTile(int tileX, int tileY, WritableRaster tile) {
     *         if (tile == null) {
     *             tile = createTile(tileX, tileY);
     *         }
     *         // Do calculation here and write results in tile.
     *         return tile;
     *     }
     * }
     *
     * <h4>Error handling</h4>
     * If this method throws an exception or returns {@code null}, then {@link #getTile(int, int) getTile(…)}
     * will set an error flag on the tile and throw an {@link ImagingOpException} with the exception thrown
     * by {@code computeTile(…)} as its cause. Future invocations of {@code getTile(tileX, tileY)} with the
     * same tile indices will cause an {@link ImagingOpException} to be thrown immediately without invocation
     * of {@code compute(tileX, tileY)}. If the error has been fixed, then users can invoke
     * {@link #clearErrorFlags(Rectangle)} for allowing the tile to be computed again.
     *
     * @param  tileX     the column index of the tile to compute.
     * @param  tileY     the row index of the tile to compute.
     * @param  previous  if the tile already exists but needs to be updated, the tile to update. Otherwise {@code null}.
     * @return computed tile for the given indices. May be the {@code previous} tile after update but cannot be null.
     * @throws Exception if an error occurred while computing the tile.
     */
    protected abstract Raster computeTile(int tileX, int tileY, WritableRaster previous) throws Exception;

    /**
     * Creates an initially empty tile at the given tile grid position.
     * This is a helper method for {@link #computeTile(int, int, WritableRaster)} implementations.
     *
     * @param  tileX  the column index of the tile to create.
     * @param  tileY  the row index of the tile to create.
     * @return initially empty tile for the given indices (cannot be null).
     */
    protected WritableRaster createTile(final int tileX, final int tileY) {
        final int x = ImageUtilities.tileToPixelX(this, tileX);
        final int y = ImageUtilities.tileToPixelY(this, tileY);
        return WritableRaster.createWritableRaster(sampleModel, new Point(x,y));
    }

    /**
     * Notifies this image that tiles will be computed soon in the given region.
     * This method is invoked by {@link ImageProcessor#prefetch(RenderedImage, Rectangle)}
     * before to request (potentially in multi-threads) all tiles in the area of interest.
     * If the returned {@code Disposable} is non-null, {@code ImageProcessor} guarantees
     * that the {@link Disposable#dispose()} method will be invoked after the prefetch
     * operation completed, successfully or not.
     *
     * <p>The default implementation does nothing. Subclasses can override this method
     * if they need to allocate and release resources once for a group of tiles.</p>
     *
     * @param  tiles  indices of the tiles which will be prefetched.
     * @return handler on which to invoke {@code dispose()} after the prefetch operation
     *         completed (successfully or not), or {@code null} if none.
     *
     * @since 1.2
     */
    @Override
    protected Disposable prefetch(Rectangle tiles) {
        return null;
    }

    /**
     * Returns whether any tile is under computation or is checked out for writing.
     * There are two reasons why this method may return {@code true}:
     *
     * <ul>
     *   <li>At least one {@link #computeTile(int, int, WritableRaster) computeTile(…)}
     *       call is running in another thread.</li>
     *   <li>There is at least one call to <code>{@linkplain #markTileWritable(int, int, boolean)
     *       markTileWritable}(tileX, tileY, true)</code> call without matching call to
     *       {@code markTileWritable(tileX, tileY, false)}. This second case may happen
     *       if this {@code ComputedImage} is also a {@link WritableRenderedImage}.</li>
     * </ul>
     *
     * @return whether any tiles are under computation or checked out for writing.
     *
     * @see #markTileWritable(int, int, boolean)
     * @see WritableRenderedImage#hasTileWriters()
     */
    public boolean hasTileWriters() {
        return reference.getWritableTileIndices(null);
    }

    /**
     * Returns whether the specified tile is currently under computation or checked out for writing.
     * There are two reasons why this method may return {@code true}:
     *
     * <ul>
     *   <li><code>{@linkplain #computeTile(int, int, WritableRaster) computeTile}(tileX, tileY, …)</code>
     *       is running in another thread.</li>
     *   <li>There is at least one call to <code>{@linkplain #markTileWritable(int, int, boolean)
     *       markTileWritable}(tileX, tileY, true)</code> call without matching call to
     *       {@code markTileWritable(tileX, tileY, false)}. This second case may happen
     *       if this {@code ComputedImage} is also a {@link WritableRenderedImage}.</li>
     * </ul>
     *
     * @param  tileX the X index of the tile to check.
     * @param  tileY the Y index of the tile to check.
     * @return whether the specified tile is under computation or checked out for writing.
     *
     * @see #markTileWritable(int, int, boolean)
     * @see WritableRenderedImage#isTileWritable(int, int)
     */
    public boolean isTileWritable(final int tileX, final int tileY) {
        return reference.isTileWritable(new TileCache.Key(reference, tileX, tileY));
    }

    /**
     * Returns the indices of all tiles under computation or checked out for writing, or {@code null} if none.
     * This method lists all tiles for which the condition documented in {@link #isTileWritable(int, int)} is
     * {@code true}.
     *
     * @return an array containing the indices of tiles that are under computation or checked out for writing,
     *         or {@code null} if none.
     *
     * @see #markTileWritable(int, int, boolean)
     * @see WritableRenderedImage#getWritableTileIndices()
     */
    public Point[] getWritableTileIndices() {
        final List<Point> indices = new ArrayList<>();
        if (reference.getWritableTileIndices(indices)) {
            return indices.toArray(Point[]::new);
        }
        return null;
    }

    /**
     * Sets or clears whether a tile is checked out for writing.
     * This method is provided for subclasses that implement the {@link WritableRenderedImage} interface.
     * This method can be used as below:
     *
     * {@snippet lang="java" :
     *     class MyImage extends ComputedImage implements WritableRenderedImage {
     *         // Constructor omitted for brevity.
     *
     *         @Override
     *         public WritableRaster getWritableTile(int tileX, int tileY) {
     *             WritableRaster raster = ...;             // Get the writable tile here.
     *             markTileWritable(tileX, tileY, true);
     *             return raster;
     *         }
     *
     *         @Override
     *         public void releaseWritableTile(int tileX, int tileY) {
     *             markTileWritable(tileX, tileY, false);
     *             // Release the raster here.
     *         }
     *     }
     *     }
     *
     * @param  tileX    the <var>x</var> index of the tile to acquire or release.
     * @param  tileY    the <var>y</var> index of the tile to acquire or release.
     * @param  writing  {@code true} for acquiring the tile, or {@code false} for releasing it.
     * @return {@code true} if the tile goes from having no writers to having one writer
     *         (may happen if {@code writing} is {@code true}), or goes from having one
     *         writer to no writers (may happen if {@code writing} is {@code false}).
     *
     * @see WritableRenderedImage#getWritableTile(int, int)
     * @see WritableRenderedImage#releaseWritableTile(int, int)
     */
    protected boolean markTileWritable(final int tileX, final int tileY, final boolean writing) {
        final TileCache.Key key = new TileCache.Key(reference, tileX, tileY);
        if (writing) {
            return reference.startWrite(key);
        } else {
            return reference.endWrite(key, true);
        }
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
     * @return {@code true} if at least one tile has been marked dirty.
     */
    protected boolean markDirtyTiles(final Rectangle tiles) {
        return reference.markDirtyTiles(tiles.x, tiles.y,
                          Math.addExact(tiles.x, tiles.width  - 1),
                          Math.addExact(tiles.y, tiles.height - 1), false);
    }

    /**
     * Clears the error status of all tiles in the given range of indices.
     * Those tiles will be marked as dirty and recomputed next time the
     * {@link #getTile(int, int)} method is invoked.
     * The status of valid tiles is unchanged by this method call.
     *
     * @param  tiles  indices of tiles for which to clear the error status.
     * @return {@code true} if at least one tile got its error flag cleared.
     *
     * @see #computeTile(int, int, WritableRaster)
     */
    protected boolean clearErrorFlags(final Rectangle tiles) {
        return reference.markDirtyTiles(tiles.x, tiles.y,
                          Math.addExact(tiles.x, tiles.width  - 1),
                          Math.addExact(tiles.y, tiles.height - 1), true);
    }

    /**
     * Invoked when a tile of a source image has been updated. This method should {@linkplain #markDirtyTiles
     * mark as dirty} all tiles of this {@code ComputedImage} that depend on the updated tile.
     *
     * <p>The default implementation assumes that source images use pixel coordinate systems aligned with this
     * {@code ComputedImage} in such a way that all pixels at coordinates (<var>x</var>, <var>y</var>) in the
     * {@code source} image are used for calculation of pixels at the same (<var>x</var>, <var>y</var>) coordinates
     * in this {@code ComputedImage}, possibly expanded to neighborhood pixels if the {@value #SOURCE_PADDING_KEY}
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
        final Insets b = getProperty(Insets.class, SOURCE_PADDING_KEY, source);
        reference.markDirtyTiles(Numerics.clamp(Math.floorDiv(tx - (b == null ? 0 : b.left), targetWidth)),
                                 Numerics.clamp(Math.floorDiv(ty - (b == null ? 0 : b.top),  targetHeight)),
                                 Numerics.clamp(Math.floorDiv(tx + (b == null ? 0 : b.right)  + sourceWidth  - 1, targetWidth)),
                                 Numerics.clamp(Math.floorDiv(ty + (b == null ? 0 : b.bottom) + sourceHeight - 1, targetHeight)), false);
    }

    /**
     * Advises this image that its tiles will no longer be requested. This method removes all
     * tiles from the cache and stops observation of {@link WritableRenderedImage} sources.
     * This image should not be used anymore after this method call.
     *
     * <p><b>Note:</b> keep in mind that this image may be referenced as a source of other images.
     * In case of doubt, it may be safer to rely on the garbage collector instead of invoking this method.</p>
     */
    @Override
    public void dispose() {
        reference.dispose();
    }

    /**
     * Returns a hash code value based on the fields known to this base class.
     * This is a helper method for {@link #hashCode()} implementation in subclasses.
     * It should <strong>not</strong> be used by {@link WritableRenderedImage} implementations,
     * because those images have listeners that are attached to a specific instance.
     */
    final int hashCodeBase() {
        return Arrays.hashCode(sources) + 31*sampleModel.hashCode() + 37*Objects.hash(destination);
    }

    /**
     * Compares the given object with this image for equality using the fields known to this base class.
     * This is a helper method for {@link #equals(Object)} implementation in subclasses.
     * It should <strong>not</strong> be used by {@link WritableRenderedImage} implementations,
     * because those images have listeners that are attached to a specific instance.
     */
    final boolean equalsBase(final Object object) {
        if (object != null && getClass().equals(object.getClass())) {
            final var other = (ComputedImage) object;
            return Arrays .equals(sources,     other.sources) &&
                   Objects.equals(destination, other.destination) &&
                   sampleModel.equals(other.sampleModel);
        }
        return false;
    }
}
