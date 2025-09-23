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
import java.awt.image.SampleModel;
import java.awt.image.BandedSampleModel;
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
import org.apache.sis.image.internal.shared.ImageUtilities;


/**
 * A builder of data buffers sharing arrays of source images.
 * There is a subclass for each supported data type.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class BandSharing {
    /**
     * The offsets of the first valid element into each bank array.
     * Will be computed with the assumption that all offsets are zeros
     * in the target {@link BandedSampleModel}.
     */
    protected final int[] offsets;

    /**
     * The sample model of the raster to create.
     * All band offsets shall be zeros and bank indices shall define an identity mapping.
     */
    private final BandedSampleModel target;

    /**
     * The sources of the tile for which bands can be shared.
     * The length of this array is the number of source images.
     * Some elements may be {@code null} if we cannot share data arrays
     * of the corresponding source and instead need to copy pixel values.
     *
     * @see BandSharedRaster#parents
     */
    private Raster[] parents;

    /**
     * Indices of tiles in source images.
     * Values at even indices are <var>x</var> tile coordinates and
     * values at odd  indices are <var>y</var> tile coordinates.
     * Values may be invalid when the corresponding {@code parents} element is null.
     */
    private int[] sourceTileIndices;

    /**
     * For subclass constructors.
     */
    protected BandSharing(final BandedSampleModel target) {
        this.target = target;
        offsets = new int[target.getNumBands()];
    }

    /**
     * Creates a new builder.
     *
     * @param  target  the sample model of the tile to create.
     * @return the data buffer, or {@code null} if the data type is not recognized.
     */
    static BandSharing create(final BandedSampleModel target) {
        switch (target.getDataType()) {
            case DataBuffer.TYPE_BYTE:   return new Bytes   (target);
            case DataBuffer.TYPE_SHORT:  return new Shorts  (target);
            case DataBuffer.TYPE_USHORT: return new UShorts (target);
            case DataBuffer.TYPE_INT:    return new Integers(target);
            case DataBuffer.TYPE_FLOAT:  return new Floats  (target);
            case DataBuffer.TYPE_DOUBLE: return new Doubles (target);
        }
        return null;
    }

    /**
     * Prepares sharing the arrays of the given sources when possible.
     * This method does not allocate new {@link DataBuffer} banks.
     *
     * @param  location  smallest (<var>x</var>,<var>y</var>) pixel coordinates of the tile.
     * @param  sources   the sources for which to aggregate all bands.
     * @return data buffer size, or 0 if there is nothing to share.
     */
    private int prepare(final Point location, final RenderedImage[] sources) {
        final int tileWidth      = target.getWidth();
        final int tileHeight     = target.getHeight();
        final int scanlineStride = target.getScanlineStride();
        int size = scanlineStride * tileHeight;     // Size of the data buffer to create.
        int band = 0;                               // Band of the target image.
        boolean sharing = false;
        parents = new Raster[sources.length];
        sourceTileIndices = new int[sources.length * 2];
        for (int si=0; si < sources.length; si++) {
            final RenderedImage source = sources[si];
            if (source.getTileWidth()  == tileWidth &&
                source.getTileHeight() == tileHeight)
            {
                int tileX = Math.subtractExact(location.x, source.getTileGridXOffset());
                int tileY = Math.subtractExact(location.y, source.getTileGridYOffset());
                if (((tileX % tileWidth) | (tileY % tileHeight)) == 0) {
                    tileX /= tileWidth;
                    tileY /= tileHeight;
                    final int n  = si << 1;
                    sourceTileIndices[n  ] = tileX;
                    sourceTileIndices[n+1] = tileY;
                    final Raster raster = source.getTile(tileX, tileY);
                    final SampleModel c = raster.getSampleModel();
                    if (c instanceof ComponentSampleModel) {
                        final var sm = (ComponentSampleModel) c;
                        if (sm.getPixelStride()    == 1 &&
                            sm.getScanlineStride() == scanlineStride)
                        {
                            final var   buffer   = raster.getDataBuffer();
                            final int[] offsets1 = buffer.getOffsets();
                            final int[] offsets2 = sm.getBandOffsets();
                            final int[] indices  = sm.getBankIndices();
                            for (int i=0; i < indices.length; i++) {
                                final int b = indices[i];
                                takeReference(buffer, b, band);
                                offsets[band] = offsets1[b] + offsets2[i];      // Assume zero offset in target `BandedSampleModel`.
                                band++;
                            }
                            size = Math.max(size, buffer.getSize());
                            parents[si] = raster;
                            sharing = true;
                            continue;
                        }
                    }
                }
            }
            /*
             * If we reach this point, it was not possible to share the data arrays of a source.
             * We will need to copy the pixels. New arrays will be allocated for holding the copy.
             */
            band += ImageUtilities.getNumBands(source);
        }
        if (band != offsets.length) {   // No `assert` keyword because it is okay to let this check be unconditional.
            throw new AssertionError();
        }
        return sharing ? size : 0;
    }

    /**
     * Creates a raster sharing the arrays of given sources when possible.
     * This method assumes a target {@link BandedSampleModel} where all band offsets.
     *
     * @param  location  smallest (<var>x</var>,<var>y</var>) pixel coordinates of the tile.
     * @param  sources   the sources for which to aggregate all bands.
     * @return a raster  containing the aggregation of all bands, or {@code null} if there is nothing to share.
     */
    final BandSharedRaster createRaster(final Point location, final RenderedImage[] sources) {
        final int size = prepare(location, sources);
        if (size == 0) {
            return null;
        }
        final DataBuffer buffer = allocate(size);
        return new BandSharedRaster(sourceTileIndices, parents, target, buffer, location);
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
     * Allocates banks for all bands that are not shared, then builds the data buffer.
     * Subclasses shall specify the {@link #offsets} array to the buffer constructor.
     *
     * @param  size  number of elements in the data buffer.
     * @return the new data buffer.
     */
    abstract DataBuffer allocate(int size);


    /**
     * A builder of data buffer of {@link DataBuffer#TYPE_BYTE}.
     */
    private static final class Bytes extends BandSharing {
        /** The shared arrays. */
        private final byte[][] data;

        /** Creates a new builder. */
        Bytes(final BandedSampleModel target) {
            super(target);
            data = new byte[offsets.length][];
        }

        /** Takes a reference to an array in the given data buffer. */
        @Override void takeReference(DataBuffer buffer, int src, int dst) {
            data[dst] = ((DataBufferByte) buffer).getData(src);
        }

        /** Builds the data buffer after all references have been taken. */
        @Override DataBuffer allocate(int size) {
            for (int i=0; i<data.length; i++) {
                if (data[i] == null) {
                    data[i] = new byte[size];
                }
            }
            return new DataBufferByte(data, size, offsets);
        }
    }

    /**
     * A builder of data buffer of {@link DataBuffer#TYPE_SHORT}.
     */
    private static final class Shorts extends BandSharing {
        /** The shared arrays. */
        private final short[][] data;

        /** Creates a new builder. */
        Shorts(final BandedSampleModel target) {
            super(target);
            data = new short[offsets.length][];
        }

        /** Takes a reference to an array in the given data buffer. */
        @Override void takeReference(DataBuffer buffer, int src, int dst) {
            data[dst] = ((DataBufferShort) buffer).getData(src);
        }

        /** Builds the data buffer after all references have been taken. */
        @Override DataBuffer allocate(int size) {
            for (int i=0; i<data.length; i++) {
                if (data[i] == null) {
                    data[i] = new short[size];
                }
            }
            return new DataBufferShort(data, size, offsets);
        }
    }

    /**
     * A builder of data buffer of {@link DataBuffer#TYPE_USHORT}.
     */
    private static final class UShorts extends BandSharing {
        /** The shared arrays. */
        private final short[][] data;

        /** Creates a new builder. */
        UShorts(final BandedSampleModel target) {
            super(target);
            data = new short[offsets.length][];
        }

        /** Takes a reference to an array in the given data buffer. */
        @Override void takeReference(DataBuffer buffer, int src, int dst) {
            data[dst] = ((DataBufferUShort) buffer).getData(src);
        }

        /** Builds the data buffer after all references have been taken. */
        @Override DataBuffer allocate(int size) {
            for (int i=0; i<data.length; i++) {
                if (data[i] == null) {
                    data[i] = new short[size];
                }
            }
            return new DataBufferUShort(data, size, offsets);
        }
    }

    /**
     * A builder of data buffer of {@link DataBuffer#TYPE_INT}.
     */
    private static final class Integers extends BandSharing {
        /** The shared arrays. */
        private final int[][] data;

        /** Creates a new builder. */
        Integers(final BandedSampleModel target) {
            super(target);
            data = new int[offsets.length][];
        }

        /** Takes a reference to an array in the given data buffer. */
        @Override void takeReference(DataBuffer buffer, int src, int dst) {
            data[dst] = ((DataBufferInt) buffer).getData(src);
        }

        /** Builds the data buffer after all references have been taken. */
        @Override DataBuffer allocate(int size) {
            for (int i=0; i<data.length; i++) {
                if (data[i] == null) {
                    data[i] = new int[size];
                }
            }
            return new DataBufferInt(data, size, offsets);
        }
    }

    /**
     * A builder of data buffer of {@link DataBuffer#TYPE_FLOAT}.
     */
    private static final class Floats extends BandSharing {
        /** The shared arrays. */
        private final float[][] data;

        /** Creates a new builder. */
        Floats(final BandedSampleModel target) {
            super(target);
            data = new float[offsets.length][];
        }

        /** Takes a reference to an array in the given data buffer. */
        @Override void takeReference(DataBuffer buffer, int src, int dst) {
            data[dst] = ((DataBufferFloat) buffer).getData(src);
        }

        /** Builds the data buffer after all references have been taken. */
        @Override DataBuffer allocate(int size) {
            for (int i=0; i<data.length; i++) {
                if (data[i] == null) {
                    data[i] = new float[size];
                }
            }
            return new DataBufferFloat(data, size, offsets);
        }
    }

    /**
     * A builder of data buffer of {@link DataBuffer#TYPE_DOUBLE}.
     */
    private static final class Doubles extends BandSharing {
        /** The shared arrays. */
        private final double[][] data;

        /** Creates a new builder. */
        Doubles(final BandedSampleModel target) {
            super(target);
            data = new double[offsets.length][];
        }

        /** Takes a reference to an array in the given data buffer. */
        @Override void takeReference(DataBuffer buffer, int src, int dst) {
            data[dst] = ((DataBufferDouble) buffer).getData(src);
        }

        /** Builds the data buffer after all references have been taken. */
        @Override DataBuffer allocate(int size) {
            for (int i=0; i<data.length; i++) {
                if (data[i] == null) {
                    data[i] = new double[size];
                }
            }
            return new DataBufferDouble(data, size, offsets);
        }
    }
}
