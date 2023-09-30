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
import java.awt.image.Raster;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.BandedSampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import java.awt.image.RasterFormatException;


/**
 * A raster where some or all bands are shared with other rasters.
 * This implementation is restricted to {@link BandedSampleModel}.
 *
 * <h2>Performance note</h2>
 * The standard Java library has many specialized implementations for different sample models.
 * By using our own implementation, we block ourselves from using those specialized subclasses.
 * However as of OpenJDK 19, all those specialized subclasses are for sample models other than
 * {@link BandedSampleModel}. Consequently we do not expect a big difference in this case.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class BandSharedRaster extends WritableRaster {
    /**
     * The sources of this raster for which bands are shared.
     * A null element means that the pixels of the corresponding source needs to be copied.
     * The non-null elements are not directly used but kept for avoiding garbage collection.
     * Because this {@code BandSharedRaster} keep references to {@code sources} data arrays,
     * garbage collection of source {@link Raster} instances will not free a lot of memory.
     * Quite the opposite, it would consume more memory if a source raster needs to be recomputed.
     */
    private final Raster[] parents;

    /**
     * Sources for which a writable raster has been acquired.
     * The length is always the total number of sources, but elements in this array may be null.
     * Non-null elements exist only if this tile has been acquired for write operations by a call
     * to {@link WritableRenderedImage#getWritableTile(int, int)}.
     */
    private final WritableRenderedImage[] writableSources;

    /**
     * Indices of tiles in source images.
     * Values at even indices are <var>x</var> tile coordinates and
     * values at odd  indices are <var>y</var> tile coordinates.
     * Values may be invalid when the corresponding {@code parents} element is null.
     */
    private final int[] sourceTileIndices;

    /**
     * Creates a new raster.
     *
     * @param srcCount  total number of source images.
     * @param parents   the sources of this raster for which bands are shared.
     * @param model     the sample model that specifies the layout.
     * @param buffer    the buffer that contains the image data.
     * @param location  the coordinate of upper-left corner.
     */
    BandSharedRaster(final int[] sourceTileIndices, final Raster[] parents,
            final SampleModel model, final DataBuffer buffer, final Point location)
    {
        super(model, buffer, location);
        writableSources = new WritableRenderedImage[sourceTileIndices.length >>> 1];
        this.sourceTileIndices = sourceTileIndices;
        this.parents = parents;
        int numBands = 0;
        for (final Raster source : parents) {
            if (source != null) {
                final int n = source.getNumBands();
                if (n > numBands) {
                    numBands = n;
                    parent = source;
                }
            }
        }
    }

    /**
     * Returns {@code true} if pixel values for the given source index needs to be copied.
     * It may happen because {@code BandSharedRaster} does not necessarily share the data arrays
     * of all sources. We may have a mix of shared sources and sources that need to be copied.
     *
     * @param  i  index of a source image.
     * @return whether pixel values for the specified source needs to be copied.
     */
    final boolean needCopy(final int i) {
        return parents[i] == null;
    }

    /**
     * Notifies all shared sources that the tile is about to be written.
     *
     * @param  sources  all sources of the band aggregate image.
     */
    final synchronized void acquireWritableTiles(final RenderedImage[] sources) {
        final var pending = new WritableRenderedImage[sources.length];
        try {
            for (int i=0; i < sources.length; i++) {
                final Raster parent = parents[i];
                if (parent != null && writableSources[i] == null) {
                    final int n = i << 1;
                    final WritableRenderedImage target = (WritableRenderedImage) sources[i];
                    final WritableRaster tile = target.getWritableTile(sourceTileIndices[n], sourceTileIndices[n+1]);
                    pending[i] = target;
                    if (parent != tile) {       // Quick test for the most common case.
                        if (parent.getDataBuffer() != tile.getDataBuffer() ||
                           !parent.getSampleModel().equals(tile.getSampleModel()))
                        {
                            throw new RasterFormatException("DataBuffer replacement not yet supported.");
                        }
                    }
                }
            }
        } catch (RuntimeException error) {
            releaseWritableTiles(pending, error);       // Rollback the tile acquisitions.
        }
        /*
         * Save the writable status only after we know that the operation is successful.
         * We want a "all or nothing" behavior: after we acquired all tiles and the method
         * returns successfully, or we acquired none of them and the method throws an exception.
         */
        for (int i=0; i < pending.length; i++) {
            final WritableRenderedImage target = pending[i];
            if (target != null) writableSources[i] = target;
        }
    }

    /**
     * Release all tiles which were acquired for write operations.
     *
     * @param  error  the exception to throw after this method completed, or {@code null} if none.
     */
    final synchronized void releaseWritableTiles(RuntimeException error) {
        releaseWritableTiles(writableSources, error);
    }

    /**
     * Release all non-null tiles in the specified array.
     * Released tiles are set to null.
     *
     * @param  sources  the band aggregate image sources for which to release writable tiles.
     * @param  error    the exception to throw after this method completed, or {@code null} if none.
     */
    private void releaseWritableTiles(final WritableRenderedImage[] sources, RuntimeException error) {
        for (int i=0; i < sources.length; i++) {
            final WritableRenderedImage source = sources[i];
            if (source != null) try {
                sources[i] = null;
                final int n = i << 1;
                source.releaseWritableTile(sourceTileIndices[n], sourceTileIndices[n+1]);
            } catch (RuntimeException e) {
                if (error == null) error = e;
                else error.addSuppressed(e);
            }
        }
        if (error != null) {
            throw error;
        }
    }
}
