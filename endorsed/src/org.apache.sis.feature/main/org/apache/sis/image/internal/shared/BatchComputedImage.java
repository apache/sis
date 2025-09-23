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
package org.apache.sis.image.internal.shared;

import java.util.Map;
import java.awt.Rectangle;
import java.awt.Image;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.ImagingOpException;
import org.apache.sis.image.ComputedImage;
import org.apache.sis.util.Disposable;
import org.apache.sis.util.resources.Errors;


/**
 * A computed image for which it is more efficient to compute tiles in batch instead of one-by-one.
 * This class is useful only when users may prefetch in advance groups of tiles by calls to the
 * {@link org.apache.sis.image.ImageProcessor#prefetch(RenderedImage, Rectangle)} method.
 *
 * <h2>Caching</h2>
 * Implementations should manage their own cache for avoiding to compute the same tiles many times.
 * The caching mechanism inherited from {@link ComputedImage} is less useful here.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class BatchComputedImage extends ComputedImage {
    /**
     * Image properties, or an empty map if none.
     * May contain instances of {@link DeferredProperty}.
     */
    private final Map<String,Object> properties;

    /**
     * Tiles fetched by a calls to {@link #prefetch(Rectangle)}, or {@code null} if none.
     * This is a linked list, but the list will rarely have more than 1 element.
     */
    private Rasters prefetched;

    /**
     * The set of tiles fetched by a single call to {@link #prefetch(Rectangle)}.
     * This is a node in a linked list.
     */
    private final class Rasters implements Disposable {
        /** Tile indices of the fetched region. */
        final int x, y, width, height;

        /** The fetched tiles. */
        final Raster[] tiles;

        /** Next set of tiles in the linked list. */
        Rasters next;

        /** Creates a new set of fetched tiles. */
        Rasters(final Rectangle r, final Raster[] tiles) {
            x      = r.x;
            y      = r.y;
            width  = r.width;
            height = r.height;
            this.tiles = tiles;
        }

        /** Discards this set of tiles. */
        @Override public void dispose() {
            remove(this);
        }
    }

    /**
     * Creates an initially empty image with the given sample model.
     *
     * @param  sampleModel  the sample model shared by all tiles in this image.
     * @param  properties   image properties ({@link DeferredProperty} supported), or {@code null} if none.
     * @param  sources      sources of this image (may be an empty array), or a null array if unknown.
     */
    protected BatchComputedImage(final SampleModel sampleModel, final Map<String,Object> properties, final RenderedImage... sources) {
        super(sampleModel, sources);
        this.properties = (properties != null) ? Map.copyOf(properties) : Map.of();
    }

    /**
     * Gets a property from this image.
     *
     * @param  key  the name of the property to get.
     * @return the property value, or {@link Image#UndefinedProperty} if none.
     */
    @Override
    public Object getProperty(final String key) {
        Object value = properties.getOrDefault(key, Image.UndefinedProperty);
        if (value instanceof DeferredProperty) {
            value = ((DeferredProperty) value).compute(this);
        }
        return value;
    }

    /**
     * Returns the names of all recognized properties,
     * or {@code null} if this image has no properties.
     *
     * @return names of all recognized properties, or {@code null} if none.
     */
    @Override
    public String[] getPropertyNames() {
        final int n = properties.size();
        return (n == 0) ? null : properties.keySet().toArray(new String[n]);
    }

    /**
     * Computes immediately and returns all tiles in the given ranges of tile indices.
     * Tiles shall be returned in row-major order.
     * It is implementer responsibility to ensure that all rasters have consistent
     * {@link Raster#getMinX()}/{@code getMinY()} values.
     *
     * @todo The return type should be changed to something more reactive, maybe {@link java.util.concurrent.Flow}.
     *       It would allow processing (e.g. map reprojection) of some tiles as soon as they become available,
     *       without waiting for all tiles to be available. Note that the tiles can be returned in any order.
     *       For example, the TIFF reader is reading tiles in arbitrary order, then rearranges them in the array.
     *       So the "row-major" order in above javadoc could not apply anymore.
     *
     * @param  tiles  range of tile indices for which to precompute tiles.
     * @return precomputed tiles for the given indices, in row-major fashion.
     * @throws Exception if an error occurred when computing tiles.
     */
    protected abstract Raster[] computeTiles(Rectangle tiles) throws Exception;

    /**
     * Invoked when a single tile need to be computed.
     * This method first searches for the tile in the regions prepared by calls to {@link #prefetch(Rectangle)}.
     * If the requested tile is not already fetched, then this method delegates to {@link #computeTiles(Rectangle)}.
     *
     * @param  tileX     the column index of the tile to compute.
     * @param  tileY     the row index of the tile to compute.
     * @param  previous  ignored (this method creates a new raster on each invocation).
     * @return computed tile for the given indices.
     * @throws Exception if an error occurred while computing the tile.
     */
    @Override
    protected final Raster computeTile(final int tileX, final int tileY, WritableRaster previous) throws Exception {
        synchronized (this) {
            for (Rasters r = prefetched; r != null; r = r.next) {
                final int x = tileX - r.x;
                final int y = tileY - r.y;
                if ((x | y) >= 0 && x < r.width && y < r.height) {
                    return r.tiles[x + y * r.width];
                }
            }
        }
        final Raster[] tiles = computeTiles(new Rectangle(tileX, tileY, 1, 1));
        if (tiles.length == 1) {
            return tiles[0];
        }
        throw new ImagingOpException(Errors.format(Errors.Keys.OutsideDomainOfValidity));
    }

    /**
     * Notifies this image that tiles will be computed soon in the given region.
     *
     * @param  region  indices of the tiles which will be prefetched.
     * @return handler on which to invoke {@code dispose()} after the prefetch operation.
     */
    @Override
    protected Disposable prefetch(final Rectangle region) {
        final Raster[] tiles;
        try {
            tiles = computeTiles(region);
        } catch (Exception e) {
            throw (ImagingOpException) new ImagingOpException(null).initCause(e);
        }
        final Rasters r = new Rasters(region, tiles);
        synchronized (this) {
            r.next = prefetched;
            prefetched = r;
        }
        return r;
    }

    /**
     * Discards the given set of tiles. This method is invoked when the fetched tiles are no longer needed.
     *
     * @param  tiles  fetched tiles to removed from the {@link #prefetched} linked list.
     */
    private synchronized void remove(final Rasters tiles) {
        Rasters previous = null;
        Rasters r = prefetched;
        while (r != tiles) {
            if (r == null) return;
            previous = r;
            r = r.next;
        }
        r = r.next;
        if (previous != null) {
            previous.next = r;
        } else {
            prefetched = r;
        }
    }
}
