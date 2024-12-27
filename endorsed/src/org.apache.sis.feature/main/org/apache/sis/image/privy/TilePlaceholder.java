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
package org.apache.sis.image.privy;

import java.util.Arrays;
import java.util.Objects;
import java.awt.Point;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.RenderedImage;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import org.apache.sis.pending.jdk.JDK16;
import org.apache.sis.util.Disposable;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.collection.WeakHashSet;
import org.apache.sis.util.privy.Numerics;
import org.apache.sis.system.ReferenceQueueConsumer;


/**
 * Source of read-only empty tiles to use as placeholder for tiles that were not computed.
 * This class reduces memory usage by sharing the same data buffer for all tiles.
 * Subclasses can optionally draw some symbols in the tile, for example to signal an error.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public class TilePlaceholder {
    /**
     * Identifies workaround for two JDK bugs.
     *
     * <p><a href="https://bugs.openjdk.java.net/browse/JDK-8166038">JDK-8166038</a>:
     * If {@link BufferedImage} cannot be used, fallback on {@link org.apache.sis.coverage.grid.ReshapedImage}
     * at the cost of an image larger than necessary. In such case, the tests need to specify the sub-region
     * of pixels to verify.</p>
     *
     * <p><a href="https://bugs.openjdk.java.net/browse/JDK-8275345">JDK-8275345</a>:
     * call to {@code Graphics2D.drawRenderedImage(…)} fails if the image contains more than one tile
     * (or a single tile not located at 0,0) and the tiles are not instances of {@link WritableRaster}
     * (i.e. are instances of the read-only {@link Raster} parent class). The exception thrown is:</p>
     *
     * <pre class="text">
     *   Exception in thread "main" java.awt.image.RasterFormatException: (parentX + width) is outside raster
     *       at java.desktop/java.awt.image.WritableRaster.createWritableChild(WritableRaster.java:228)
     *       at java.desktop/sun.java2d.SunGraphics2D.drawTranslatedRenderedImage(SunGraphics2D.java:2852)
     *       at java.desktop/sun.java2d.SunGraphics2D.drawRenderedImage(SunGraphics2D.java:2711)</pre>
     */
    @Workaround(library="JDK", version="17")
    public static final boolean PENDING_JDK_FIX = true;

    /**
     * Cache of empty tiles for different sample models.
     */
    private static final WeakHashSet<TilePlaceholder> CACHE = new WeakHashSet<>(TilePlaceholder.class);

    /**
     * The sample model of the empty tiles to create.
     */
    protected final SampleModel model;

    /**
     * The data buffers for empty tiles. The same data buffer is shared by all {@link Raster}
     * instances using the same sample model. Data may contain some drawing such as a cross (X).
     */
    private BufferRef reference;

    /**
     * Weak reference to a data buffer. The enclosing {@link TilePlaceholder} is protected
     * from garbage collection as long as the weak reference is live.
     */
    private static final class BufferRef extends WeakReference<DataBuffer> implements Disposable {
        /**
         * Prevents the enclosing class to be garbage-collected too early by the {@link #CACHE}.
         * If the {@link DataBuffer} is still referenced by some {@link Raster}, then we need the
         * {@link TilePlaceholder} to continue to exist in the {@link #CACHE} in order to be able
         * to reuse that {@code DataBuffer}.
         */
        private TilePlaceholder owner;

        /**
         * Creates a new reference to the given buffer.
         */
        BufferRef(TilePlaceholder owner, final DataBuffer buffer) {
            super(buffer, ReferenceQueueConsumer.QUEUE);
            this.owner = owner;
        }

        /**
         * Allows the enclosing {@link TilePlaceholder} to be garbage-collected.
         */
        @Override
        public void dispose() {
            owner = null;
        }
    }

    /**
     * Creates a initially empty set of empty tiles.
     *
     * @param  model  sample model of the empty tiles.
     */
    protected TilePlaceholder(final SampleModel model) {
        this.model = Objects.requireNonNull(model);
    }

    /**
     * Returns a provider of empty tiles for the given sample model.
     * All pixel values will be zero in all bands.
     *
     * @param  model  sample model of the empty tiles.
     * @return provider of fully empty tiles.
     */
    public static TilePlaceholder empty(final SampleModel model) {
        return CACHE.unique(new TilePlaceholder(model));
    }

    /**
     * Returns a provider of empty tiles filled with the given values in all bands.
     * A {@code null} array is interpreted as 0 for integer types or NaN for floating point types.
     *
     * @param  model       sample model of the empty tiles.
     * @param  fillValues  the values to use for filling empty spaces in rasters, or {@code null} for the default.
     * @return provider of filled tiles.
     */
    public static TilePlaceholder filled(final SampleModel model, final Number[] fillValues) {
        if (fillValues == null) {
            return empty(model);
        }
        return filled(model, new FillValues(model, fillValues, true));
    }

    /**
     * Returns a provider of empty tiles filled with the given values.
     *
     * @param  model  sample model of the empty tiles.
     * @param  fill   the fill values.
     * @return provider of filled tiles.
     */
    public static TilePlaceholder filled(final SampleModel model, final FillValues fill) {
        return CACHE.unique(fill.isFullyZero ? new TilePlaceholder(model) : new Filled(model, fill));
    }

    /**
     * Returns a source of "empty" tiles with a white border and a white cross.
     *
     * @param  image  sample model and color model of the tiles to create.
     * @return a source of "empty" tiles with white borders and crosses.
     */
    public static TilePlaceholder withCross(final RenderedImage image) {
        return CACHE.unique(new WithCross(image));
    }

    /**
     * Creates a tile to use as a placeholder when a tile cannot be computed.
     *
     * @param  location  minimum x and y coordinates of the tile raster.
     * @return placeholder for a tile at the given location.
     */
    public final Raster create(final Point location) {
        DataBuffer buffer;
        synchronized (this) {
            if (reference == null || (buffer = reference.get()) == null) {
                buffer = model.createDataBuffer();
                reference = new BufferRef(this, buffer);      // Cache even if only partially drawn.
                if (getClass() != TilePlaceholder.class) {
                    final WritableRaster tile = Raster.createWritableRaster(model, buffer, location);
                    draw(tile);
                    return tile;
                }
                // Else prefer read-only tile (created below) if we do not need to draw anything.
            }
        }
        // Reuse same `DataBuffer` with only a different location.
        return Raster.createRaster(model, buffer, location);
    }

    /**
     * Returns {@code true} if this factory is the creator of given raster.
     *
     * @param  tile  the tile to test, or {@code null}.
     * @return whether this tile is the creator of given raster.
     */
    public final boolean isCreatorOf(final Raster tile) {
        if (tile != null) {
            final BufferRef r;
            synchronized (this) {
                r = reference;
            }
            if (r != null) {
                return JDK16.refersTo(r, tile.getDataBuffer());
            }
        }
        return false;
    }

    /**
     * Invoked when a new empty tile is created. Subclasses can override this method
     * for drawing some visual indication that the tile is missing.
     * The default implementation does nothing.
     *
     * @param  tile  the tile where to draw.
     */
    protected void draw(final WritableRaster tile) {
    }




    /**
     * A provider of tile placeholder with a fill value.
     */
    private static final class Filled extends TilePlaceholder {
        /**
         * The object to use for filling the raster.
         */
        private final FillValues fill;

        /**
         * Creates a new provider for the given fill value.
         */
        Filled(final SampleModel model, final FillValues fill) {
            super(model);
            this.fill = fill;
        }

        /**
         * Fills the given raster.
         */
        @Override
        protected void draw(final WritableRaster tile) {
            fill.fill(tile);
        }

        /**
         * Compares this object with given object for equality.
         */
        @Override
        public boolean equals(final Object obj) {
            return super.equals(obj) && fill.equals(((Filled) obj).fill);
        }

        /**
         * Returns a hash code value for this provider of tile placeholders.
         */
        @Override
        public int hashCode() {
            return super.hashCode() + fill.hashCode();
        }
    }




    /**
     * A provider of tile placeholder with a white border and white cross in the tile.
     */
    private static final class WithCross extends TilePlaceholder {
        /**
         * The sample values to use for the border and the cross.
         */
        private final double[] samples;

        /**
         * Creates a new provider for the sample model and color model of given image.
         *
         * @param  image  sample model and color model of the tiles to create.
         */
        WithCross(final RenderedImage image) {
            super(image.getSampleModel());
            samples = new double[model.getNumBands()];
            if (ImageUtilities.isIntegerType(model)) {
                final boolean isUnsigned = ImageUtilities.isUnsignedType(model);
                for (int i=0; i<samples.length; i++) {
                    int size = model.getSampleSize(i);
                    if (!isUnsigned) size--;
                    samples[i] = Numerics.bitmask(size) - 1;
                }
            } else {
                final ColorSpace cs;
                final ColorModel cm = image.getColorModel();
                if (cm != null && (cs = cm.getColorSpace()) != null) {
                    for (int i = Math.min(cs.getNumComponents(), samples.length); --i >=0;) {
                        samples[i] = cs.getMaxValue(i);
                    }
                } else {
                    Arrays.fill(samples, 1);
                }
            }
        }

        /**
         * Draw borders around the tile as dotted lines. The left border will have (usually) white pixels
         * at even coordinates relative to upper-left corner, while right border will have same pixels at
         * odd coordinates. The same pattern applies to top and bottom borders.
         */
        @Override
        protected void draw(final WritableRaster tile) {
            final int width  = tile.getWidth();
            final int height = tile.getHeight();
            final int xmin   = tile.getMinX();
            final int ymin   = tile.getMinY();
            final int xmax   = width  + xmin - 1;
            final int ymax   = height + ymin - 1;
            if (height >= 4) {
                for (int x = xmin; x <= xmax; x++) {
                    tile.setPixel(x, ymax-1, samples);
                    tile.setPixel(x, ymin,   samples); if (++x > xmax) break;
                    tile.setPixel(x, ymin+1, samples);
                    tile.setPixel(x, ymax,   samples);
                }
            }
            if (width >= 4) {
                for (int y = ymin; y <= ymax; y++) {
                    tile.setPixel(xmax-1, y, samples);
                    tile.setPixel(xmin,   y, samples); if (++y > ymax) break;
                    tile.setPixel(xmin+1, y, samples);
                    tile.setPixel(xmax,   y, samples);
                }
            }
            /*
             * Add a cross (X) inside the tile.
             */
            final int t = 1;        // Thickness to add on each side of the line.
            if (width >= height) {
                final double step = height / (double) width;
                final int m = ymax + ymin;
                for (int x = xmin; x <= xmax; x++) {
                    final int y = ymin + (int) ((x-xmin)*step);
                    final int s = Math.min(x+t, xmax);
                    for  (int i = Math.max(x-t, xmin); i <= s; i++) {
                        tile.setPixel(i,   y, samples);
                        tile.setPixel(i, m-y, samples);
                    }
                }
            } else {
                final double step = width / (double) height;
                final int m = xmax + xmin;
                for (int y = ymin; y <= ymax; y++) {
                    final int x = xmin + (int) ((y-ymin)*step);
                    final int s = Math.min(y+t, ymax);
                    for  (int i = Math.max(y-t, ymin); i <= s; i++) {
                        tile.setPixel(  x, i, samples);
                        tile.setPixel(m-x, i, samples);
                    }
                }
            }
        }

        /**
         * Compares this object with given object for equality.
         */
        @Override
        public boolean equals(final Object obj) {
            return super.equals(obj) && Arrays.equals(((WithCross) obj).samples, samples);
        }

        /**
         * Returns a hash code value for this provider of tile placeholders.
         */
        @Override
        public int hashCode() {
            return super.hashCode() + Arrays.hashCode(samples);
        }
    }

    /**
     * Compares this object with given object for equality.
     *
     * @param  obj  the other object to compare with this object.
     * @return {@code true} if the two objects will create equivalent empty tiles.
     */
    @Override
    public boolean equals(final Object obj) {
        return (obj != null) && (obj.getClass() == getClass()) && model.equals(((TilePlaceholder) obj).model);
    }

    /**
     * Returns a hash code value for this provider of tile placeholders.
     */
    @Override
    public int hashCode() {
        return getClass().hashCode() - model.hashCode();
    }
}
