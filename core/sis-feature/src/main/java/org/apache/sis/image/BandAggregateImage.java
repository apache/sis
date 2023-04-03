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

import java.util.Arrays;
import java.util.Objects;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferDouble;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.internal.coverage.j2d.ImageUtilities;


/**
 * An image where each band is taken from a selection of bands in a sequence of source images.
 * This image will share the underlying data arrays when possible, or copy bands otherwise.
 * The actual strategy may be a mix of both bands copying and sharing.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 *
 * @see BandSelectImage
 * @see ImageCombiner
 *
 * @since 1.4
 */
final class BandAggregateImage extends ComputedImage {
    /**
     * The source images with only the bands to aggregate, in order.
     * Those images are views; the band sample values are not copied.
     */
    private final RenderedImage[] filteredSources;

    /**
     * Color model of the aggregated image.
     *
     * @see #getColorModel()
     */
    private final ColorModel colorModel;

    /**
     * Domain of pixel coordinates. All images shall share the same pixel coordinate space,
     * meaning that a pixel at coordinates (<var>x</var>, <var>y</var>) in this image will
     * contain the sample values of all source images at the same coordinates.
     * It does <em>not</em> mean that all source images shall have the same bounds.
     */
    private final int minX, minY, width, height;

    /**
     * Index of the first tile. Contrarily to pixel coordinates,
     * the tile coordinate space does not need to be the same for all images.
     */
    private final int minTileX, minTileY;

    /**
     * Whether all sources have tiles at the same locations and use the same scanline stride.
     * In such case, it is possible to share references to data arrays without copying them.
     */
    private final boolean allowSharing;

    /**
     * Creates a new aggregation of bands.
     *
     * @param  sources         images to combine, in order.
     * @param  bandsPerSource  bands to use for each source image, in order. May contain {@code null} elements.
     * @param  colorizer       provider of color model to use for this image, or {@code null} for automatic.
     * @param  allowSharing    whether to allow the sharing of data buffers (instead of copying) if possible.
     * @throws IllegalArgumentException if there is an incompatibility between some source images
     *         or if some band indices are duplicated or outside their range of validity.
     * @return the band aggregate image.
     */
    static RenderedImage create(final RenderedImage[] sources, final int[][] bandsPerSource,
                                final Colorizer colorizer, final boolean allowSharing)
    {
        final var layout = CombinedImageLayout.create(sources, bandsPerSource, allowSharing);
        final var image  = new BandAggregateImage(layout, colorizer);
        if (image.filteredSources.length == 1) {
            final RenderedImage c = image.filteredSources[0];
            if (image.colorModel == null) {
                return c;
            }
            final ColorModel cm = c.getColorModel();
            if (cm == null || image.colorModel.equals(cm)) {
                return c;
            }
        }
        return image;
    }

    /**
     * Creates a new aggregation of bands.
     *
     * @param  layout     pixel and tile coordinate spaces of this image, together with sample model.
     * @param  colorizer  provider of color model to use for this image, or {@code null} for automatic.
     */
    private BandAggregateImage(final CombinedImageLayout layout, final Colorizer colorizer) {
        super(layout.sampleModel, layout.sources);
        final Rectangle r = layout.domain;
        minX            = r.x;
        minY            = r.y;
        width           = r.width;
        height          = r.height;
        minTileX        = layout.minTileX;
        minTileY        = layout.minTileY;
        allowSharing    = layout.allowSharing;
        filteredSources = layout.getFilteredSources();
        colorModel      = layout.createColorModel(colorizer);
        ensureCompatible(colorModel);
    }

    /** Returns the information inferred at construction time. */
    @Override public ColorModel getColorModel() {return colorModel;}
    @Override public int        getWidth()      {return width;}
    @Override public int        getHeight()     {return height;}
    @Override public int        getMinX()       {return minX;}
    @Override public int        getMinY()       {return minY;}
    @Override public int        getMinTileX()   {return minTileX;}
    @Override public int        getMinTileY()   {return minTileY;}

    /**
     * Creates a raster containing the selected bands of source images.
     *
     * @param  tileX  the column index of the tile to compute.
     * @param  tileY  the row index of the tile to compute.
     * @param  tile   the previous tile, reused if non-null.
     */
    @Override
    protected Raster computeTile(final int tileX, final int tileY, WritableRaster tile) {
        /*
         * If we are allowed to share the data arrays, try that first.
         */
        if (allowSharing) {
            final Sharing sharing = Sharing.create(sampleModel.getDataType(), sampleModel.getNumBands());
            if (sharing != null) {
                final DataBuffer buffer = sharing.createDataBuffer(
                        Math.multiplyFull(tileX - minTileX, getTileWidth())  + minX,
                        Math.multiplyFull(tileY - minTileY, getTileHeight()) + minY,
                        filteredSources);
                if (buffer != null) {
                    return Raster.createRaster(sampleModel, buffer, computeTileLocation(tileX, tileY));
                }
            }
        }
        /*
         * Fallback when the data arrays can not be shared.
         * This code copies all sample values in new arrays.
         */
        if (tile == null) {
            tile = createTile(tileX, tileY);
        }
        int band = 0;
        for (final RenderedImage source : filteredSources) {
            final Rectangle aoi = tile.getBounds();
            ImageUtilities.clipBounds(source, aoi);
            final int numBands = ImageUtilities.getNumBands(source);
            final int[] bands = ArraysExt.range(band, band + numBands);
            var target = tile.createWritableChild(aoi.x, aoi.y, aoi.width, aoi.height,
                                                  aoi.x, aoi.y, bands);
            band += numBands;
            copyData(aoi, source, target);
        }
        return tile;
    }

    /**
     * A builder of data buffers sharing arrays of source images.
     * There is a subclass for each supported data type.
     */
    private abstract static class Sharing {
        /**
         * The offsets of the first valid element into each bank array.
         * Will be computed with the assumption that all offsets are zero
         * in the target {@link java.awt.image.BandedSampleModel}.
         */
        protected final int[] offsets;

        /**
         * For subclass constructors.
         */
        protected Sharing(final int numBands) {
            offsets = new int[numBands];
        }

        /**
         * Creates a new builder.
         *
         * @param  dataType  the data type as one of {@link DataBuffer} constants.
         * @param  numBands  number of banks of the data buffer to create.
         * @return the data buffer, or {@code null} if the dat type is not recognized.
         */
        static Sharing create(final int dataType, final int numBands) {
            switch (dataType) {
                case DataBuffer.TYPE_BYTE:   return new Bytes   (numBands);
                case DataBuffer.TYPE_SHORT:  return new Shorts  (numBands);
                case DataBuffer.TYPE_USHORT: return new UShorts (numBands);
                case DataBuffer.TYPE_INT:    return new Integers(numBands);
                case DataBuffer.TYPE_FLOAT:  return new Floats  (numBands);
                case DataBuffer.TYPE_DOUBLE: return new Doubles (numBands);
            }
            return null;
        }

        /**
         * Creates a data buffer sharing the arrays of all given sources, in order.
         * This method assumes a target {@link java.awt.image.BandedSampleModel} where
         * all band offsets are zero and where bank indices define an identity mapping.
         *
         * @param  x        <var>x</var> pixel coordinate of the tile.
         * @param  y        <var>y</var> pixel coordinate of the tile.
         * @param  sources  the sources for which to aggregate all bands.
         * @return a data buffer containing the aggregation of all bands, or {@code null} if it can not be created.
         */
        final DataBuffer createDataBuffer(final long x, final long y, final RenderedImage[] sources) {
            int band = 0;
            int size = Integer.MAX_VALUE;
            for (final RenderedImage source : sources) {
                final int tileWidth  = source.getTileWidth();
                final int tileHeight = source.getTileHeight();
                long tileX = x - source.getTileGridXOffset();
                long tileY = y - source.getTileGridYOffset();
                if (((tileX % tileWidth) | (tileY % tileHeight)) != 0) {
                    return null;    // Source tile not aligned on target tile.
                }
                tileX /= tileWidth;
                tileY /= tileHeight;
                final Raster raster = source.getTile(Math.toIntExact(tileX), Math.toIntExact(tileY));
                final SampleModel c = raster.getSampleModel();
                if (!(c instanceof ComponentSampleModel)) {
                    return null;    // Should never happen if `BandAggregateImage.allowSharing` is true.
                }
                final var   sm       = (ComponentSampleModel) c;
                final var   buffer   = raster.getDataBuffer();
                final int[] offsets1 = buffer.getOffsets();
                final int[] offsets2 = sm.getBandOffsets();
                final int[] indices  = sm.getBankIndices();
                for (int i=0; i<indices.length; i++) {
                    final int b = indices[i];
                    takeReference(buffer, b, band);
                    offsets[band] = offsets1[b] + offsets2[i];      // Assume zero offset in target `BandedSampleModel`.
                    band++;
                }
                size = Math.min(size, buffer.getSize());
            }
            final DataBuffer buffer = build(size);
            assert buffer.getNumBanks() == band;
            return buffer;
        }

        /**
         * Takes a reference to an array in the given data buffer.
         *
         * @param source  the data buffer from which to take a reference to an array.
         * @param src     bank index of the reference to take.
         * @param dst     band index where to store the reference.
         */
        abstract void takeReference(DataBuffer source, int src, int dst);

        /**
         * Builds the data buffer after all references have been taken.
         * The data buffer shall specify {@link #offsets} to the buffer constructor.
         *
         * @param  size  number of elements in the data buffer.
         * @return the new data buffer.
         */
        abstract DataBuffer build(int size);
    }

    /**
     * A builder of data buffer of {@link DataBuffer#TYPE_BYTE}.
     */
    private static final class Bytes extends Sharing {
        /** The shared arrays. */
        private final byte[][] data;

        /** Creates a new builder. */
        Bytes(final int numBands) {
            super(numBands);
            data = new byte[numBands][];
        }

        /** Takes a reference to an array in the given data buffer. */
        @Override void takeReference(DataBuffer buffer, int src, int dst) {
            data[dst] = ((DataBufferByte) buffer).getData(src);
        }

        /** Builds the data buffer after all references have been taken. */
        @Override DataBuffer build(int size) {
            return new DataBufferByte(data, size, offsets);
        }
    }

    /**
     * A builder of data buffer of {@link DataBuffer#TYPE_SHORT}.
     */
    private static final class Shorts extends Sharing {
        /** The shared arrays. */
        private final short[][] data;

        /** Creates a new builder. */
        Shorts(final int numBands) {
            super(numBands);
            data = new short[numBands][];
        }

        /** Takes a reference to an array in the given data buffer. */
        @Override void takeReference(DataBuffer buffer, int src, int dst) {
            data[dst] = ((DataBufferShort) buffer).getData(src);
        }

        /** Builds the data buffer after all references have been taken. */
        @Override DataBuffer build(int size) {
            return new DataBufferShort(data, size, offsets);
        }
    }

    /**
     * A builder of data buffer of {@link DataBuffer#TYPE_USHORT}.
     */
    private static final class UShorts extends Sharing {
        /** The shared arrays. */
        private final short[][] data;

        /** Creates a new builder. */
        UShorts(final int numBands) {
            super(numBands);
            data = new short[numBands][];
        }

        /** Takes a reference to an array in the given data buffer. */
        @Override void takeReference(DataBuffer buffer, int src, int dst) {
            data[dst] = ((DataBufferUShort) buffer).getData(src);
        }

        /** Builds the data buffer after all references have been taken. */
        @Override DataBuffer build(int size) {
            return new DataBufferUShort(data, size, offsets);
        }
    }

    /**
     * A builder of data buffer of {@link DataBuffer#TYPE_INT}.
     */
    private static final class Integers extends Sharing {
        /** The shared arrays. */
        private final int[][] data;

        /** Creates a new builder. */
        Integers(final int numBands) {
            super(numBands);
            data = new int[numBands][];
        }

        /** Takes a reference to an array in the given data buffer. */
        @Override void takeReference(DataBuffer buffer, int src, int dst) {
            data[dst] = ((DataBufferInt) buffer).getData(src);
        }

        /** Builds the data buffer after all references have been taken. */
        @Override DataBuffer build(int size) {
            return new DataBufferInt(data, size, offsets);
        }
    }

    /**
     * A builder of data buffer of {@link DataBuffer#TYPE_FLOAT}.
     */
    private static final class Floats extends Sharing {
        /** The shared arrays. */
        private final float[][] data;

        /** Creates a new builder. */
        Floats(final int numBands) {
            super(numBands);
            data = new float[numBands][];
        }

        /** Takes a reference to an array in the given data buffer. */
        @Override void takeReference(DataBuffer buffer, int src, int dst) {
            data[dst] = ((DataBufferFloat) buffer).getData(src);
        }

        /** Builds the data buffer after all references have been taken. */
        @Override DataBuffer build(int size) {
            return new DataBufferFloat(data, size, offsets);
        }
    }

    /**
     * A builder of data buffer of {@link DataBuffer#TYPE_DOUBLE}.
     */
    private static final class Doubles extends Sharing {
        /** The shared arrays. */
        private final double[][] data;

        /** Creates a new builder. */
        Doubles(final int numBands) {
            super(numBands);
            data = new double[numBands][];
        }

        /** Takes a reference to an array in the given data buffer. */
        @Override void takeReference(DataBuffer buffer, int src, int dst) {
            data[dst] = ((DataBufferDouble) buffer).getData(src);
        }

        /** Builds the data buffer after all references have been taken. */
        @Override DataBuffer build(int size) {
            return new DataBufferDouble(data, size, offsets);
        }
    }

    /**
     * Returns a hash code value for this image.
     */
    @Override
    public int hashCode() {
        return sampleModel.hashCode() + 37 * (Arrays.hashCode(filteredSources) + 31 * Objects.hashCode(colorModel));
    }

    /**
     * Compares the given object with this image for equality.
     *
     * <h4>Implementation note</h4>
     * We do not invoke {@link #equalsBase(Object)} for saving the comparisons of {@link ComputedImage#sources} array.
     * The comparison of {@link #filteredSources} array will indirectly include the comparison of raw source images.
     */
    @Override
    public boolean equals(final Object object) {
        if (object instanceof BandAggregateImage) {
            final BandAggregateImage other = (BandAggregateImage) object;
            return minTileX == other.minTileX &&
                   minTileY == other.minTileY &&
                   getBounds().equals(other.getBounds()) &&
                   sampleModel.equals(other.sampleModel) &&
                   Objects.equals(colorModel, other.colorModel) &&
                   Arrays.equals(filteredSources, other.filteredSources);
        }
        return false;
    }
}
