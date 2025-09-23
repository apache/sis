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
import java.util.Random;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicInteger;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.BandedSampleModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.ImagingOpException;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RasterFormatException;
import java.awt.image.SampleModel;
import java.awt.image.TileObserver;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import org.apache.sis.image.internal.shared.ImageUtilities;
import org.apache.sis.image.internal.shared.WritableTiledImage;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.pending.jdk.JDK18;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;


/**
 * A rendered image which can contain an arbitrary number of tiles. Tiles are stored in memory.
 * We use this class for testing purpose only because tiled images in production use need a more
 * sophisticated implementation capable to store some tiles on disk (for memory consumption reasons).
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 */
public final class TiledImageMock extends PlanarImage implements WritableRenderedImage {
    /**
     * Inverse of the probability that a tile has failure.
     * This is used only if {@link #failRandomly(Random, boolean)} is invoked.
     */
    private static final int FAILURE_PROBABILITY = 10;

    /**
     * Location of the upper-left pixel of the image. Should be a non-trivial
     * value for increasing the chances to detect error in index calculation.
     */
    private final int minX, minY;

    /**
     * Number of pixels along X or Y axis in the whole rendered image.
     */
    private final int width, height;

    /**
     * Number of pixels along X or Y axis in a single tile of the image.
     * All tiles must have the same size.
     */
    private final int tileWidth, tileHeight;

    /**
     * Number of tiles in the X or Y direction. This is computed from the image size and tile size.
     */
    private final int numXTiles, numYTiles;

    /**
     * Index of the first tile in the image. Should be a non-trivial value
     * for increasing the chances to detect error in index calculation.
     */
    private final int minTileX, minTileY;

    /**
     * The tiles, created when first needed.
     */
    private final WritableRaster[] tiles;

    /**
     * The sample model to use for creating the tiles.
     */
    private final SampleModel sampleModel;

    /**
     * Indices of acquired tiles. Those indices are valid only if {@link #isTileAcquired} is {@code true}.
     */
    private int acquiredTileX, acquiredTileY;

    /**
     * Whether a tile has been acquired by a call to {@link #getWritableTile(int, int)} and not yet released.
     */
    private boolean isTileAcquired;

    /**
     * If some tiles should fail randomly, the random number generator to use for deciding if a
     * {@link #getTile(int, int)} invocation should fail. Note that two consecutive invocations
     * of {@code getTile(…)} may give different result.
     *
     * @see #failRandomly(Random, boolean)
     */
    private Random randomFailures;

    /**
     * Tiles to keep in error on all invocations of {@link #getTile(int, int)}. This is an alternative
     * to {@link #randomFailures} for creating artificial errors. Contrarily to {@link #randomFailures},
     * all calls to {@link #getTile(int, int)} with the same indices have the same behavior.
     */
    private boolean[] constantFailures;

    /**
     * Sequential number for use in production of error messages, to differentiate them.
     * Since {@link #getTile(int, int)} may be invoked in background threads, this field
     * should be safe to concurrent threads.
     *
     * @see #failRandomly(Random, boolean)
     */
    private AtomicInteger errorSequence;

    /**
     * The color model, created only if requested.
     * This is needed only for visualizing the image on screen; most tests do not need it.
     *
     * @see #getColorModel()
     */
    private ColorModel colorModel;

    /**
     * Creates a new tiled image. Testers should invoke {@link #validate()} after construction.
     *
     * @param dataType     sample data type as one of the {@link java.awt.image.DataBuffer} constants.
     * @param numBands     number of bands in the sample model to create.
     * @param minX         minimum X coordinate (inclusive) of the rendered image.
     * @param minY         minimum Y coordinate (inclusive) of the rendered image.
     * @param width        number of pixels along X axis in the whole rendered image.
     * @param height       number of pixels along Y axis in the whole rendered image.
     * @param tileWidth    number of pixels along X axis in a single tile of the image.
     * @param tileHeight   number of pixels along Y axis in a single tile of the image.
     * @param minTileX     minimum tile index in the X direction.
     * @param minTileY     minimum tile index in the Y direction.
     * @param banded       whether to use {@link BandedSampleModel} instead of {@link PixelInterleavedSampleModel}.
     */
    public TiledImageMock(final int dataType,  final int numBands,
                          final int minX,      final int minY,
                          final int width,     final int height,
                          final int tileWidth, final int tileHeight,
                          final int minTileX,  final int minTileY,
                          final boolean banded)
    {
        this.minX        = minX;
        this.minY        = minY;
        this.width       = width;
        this.height      = height;
        this.tileWidth   = tileWidth;
        this.tileHeight  = tileHeight;
        this.minTileX    = minTileX;
        this.minTileY    = minTileY;
        this.numXTiles   = JDK18.ceilDiv(width,  tileWidth);
        this.numYTiles   = JDK18.ceilDiv(height, tileHeight);
        this.tiles       = new WritableRaster[numXTiles * numYTiles];
        this.sampleModel = banded ? new BandedSampleModel(dataType, tileWidth, tileHeight, numBands) :
                          new PixelInterleavedSampleModel(dataType, tileWidth, tileHeight, numBands,
                                 StrictMath.multiplyExact(numBands, tileWidth), ArraysExt.range(0, numBands));
    }

    /**
     * Sets the color model. This method can be invoked at most once.
     *
     * @param  cm  the color model to use.
     */
    public synchronized void setColorModel(final ColorModel cm) {
        assertNull(colorModel, "Already initialized.");
        assertTrue(cm.isCompatibleSampleModel(sampleModel));
        colorModel = cm;
    }

    /**
     * Returns the color model.
     * The default value is a gray scale color model if the data type is byte, or {@code null} otherwise.
     * More color models may be supported in future versions if there is a need for them.
     */
    @Override
    public synchronized ColorModel getColorModel() {
        if (colorModel == null && sampleModel instanceof ComponentSampleModel && sampleModel.getNumBands() == 1) {
            final int dataType = sampleModel.getDataType();
            if (dataType <= DataBuffer.TYPE_USHORT) {
                colorModel = new ComponentColorModel(
                        ColorSpace.getInstance(ColorSpace.CS_GRAY),
                        new int[] {DataBuffer.getDataTypeSize(dataType)},
                        false, true, Transparency.OPAQUE, dataType);
            }
        }
        return colorModel;
    }

    /** Returns a sample model for data type given to the constructor. */
    @Override public SampleModel getSampleModel() {return sampleModel;}

    /*
     * Information specified to the constructor.
     */
    @Override public int getMinX()       {return minX;}
    @Override public int getMinY()       {return minY;}
    @Override public int getWidth()      {return width;}
    @Override public int getHeight()     {return height;}
    @Override public int getTileWidth()  {return tileWidth;}
    @Override public int getTileHeight() {return tileHeight;}
    @Override public int getNumXTiles()  {return numXTiles;}
    @Override public int getNumYTiles()  {return numYTiles;}
    @Override public int getMinTileX()   {return minTileX;}
    @Override public int getMinTileY()   {return minTileY;}

    /**
     * Verifies that image and tile layouts are consistent.
     * This method should be invoked after construction.
     *
     * @see #verify()
     */
    public synchronized void validate() {
        assertNull(verify());
    }

    /**
     * Causes some {@link #getTile(int, int)} and {@link #getWritableTile(int, int)} calls to fail.
     * if {@code onEachCall} is {@code true}, then two consecutive invocations of {@code getTile(…)}
     * may give different result.
     *
     * @param  random      the random number generator to use for deciding if a tile request should fail.
     * @param  onEachCall  {@code true} if failure may happen on any {@code getTile(…)}, or {@code false}
     *                     for constant behavior when {@code getTile(…)} is invoked with same tile indices.
     */
    public synchronized void failRandomly(final Random random, final boolean onEachCall) {
        if (onEachCall) {
            randomFailures = random;
            constantFailures = null;
        } else {
            randomFailures = null;
            constantFailures = new boolean[tiles.length];
            for (int i=0; i<constantFailures.length; i++) {
                constantFailures[i] = (random.nextInt(FAILURE_PROBABILITY) == 0);
            }
        }
        if (errorSequence == null) {
            errorSequence = new AtomicInteger();
        }
    }

    /**
     * Sets a sample value at the given location in pixel coordinates.
     * This is a helper method for testing purpose on small images only,
     * since invoking this method in a loop is inefficient.
     *
     * @param  x      column index of the pixel to set.
     * @param  y      row index of the pixel to set.
     * @param  b      band index of the sample value to set.
     * @param  value  the new value.
     */
    public synchronized void setSample(final int x, final int y, final int b, final double value) {
        final int ox = x - minX;
        final int oy = y - minY;
        if (ox < 0 || ox >= width || oy < 0 || oy >= height) {
            throw new IndexOutOfBoundsException();
        }
        tile(StrictMath.floorDiv(ox, tileWidth)  + minTileX,
             StrictMath.floorDiv(oy, tileHeight) + minTileY, true).setSample(x, y, b, value);
    }

    /**
     * Initializes the sample values of all all bands in all tiles to testing values.
     * The sample value pattern is described by {@link #initializeAllTiles(int[], int)}.
     */
    public void initializeAllTiles() {
        initializeAllTiles(ArraysExt.range(0, sampleModel.getNumBands()), 0);
    }

    /**
     * Initializes the sample values of all tiles to testing values.
     * The sample values will be 4 digits numbers of the form "BTYX" where:
     * <ul>
     *   <li><var>B</var> is the band index starting with 1 for the first band.</li>
     *   <li><var>T</var> is the tile index starting with 1 for the first tile and increasing in a row-major fashion.</li>
     *   <li><var>Y</var> is the <var>y</var> coordinate (row 0-based index) of the sample value relative to current tile.</li>
     *   <li><var>X</var> is the <var>x</var> coordinate (column 0-based index) of the sample value relative to current tile.</li>
     * </ul>
     *
     * The "BTYX" pattern holds if all values are less than 10. If some values are greater than 10,
     * then the sample values are a mix of above values resulting from the following arithmetic sum:
     *
     * <blockquote>
     * <var>sample</var> = <var>B</var> * 1000 + <var>T</var> * 100 + <var>Y</var> * 10 + <var>X</var>
     * </blockquote>
     *
     * This method creates a new raster for each tile.
     * Any modification to previously existing rasters will be lost.
     * Bands that are not specified in the {@code bands} array will have their values initialized to zero.
     *
     * @param bands   indices of the bands where to apply the "BTYX" pattern. Other bands will be initialized to zero.
     * @param offset  an arbitrary offset to add to all values.
     */
    public synchronized void initializeAllTiles(final int[] bands, final int offset) {
        if (bands.length > 1) {   // Skip the following check for the common case of a single band.
            if (Arrays.stream(bands).boxed().collect(Collectors.toSet()).size() != bands.length) {
                throw new IllegalArgumentException("Input band list contains duplicated values.");
            }
        }
        int ti = 0;
        for (int ty=0; ty<numYTiles; ty++) {
            for (int tx=0; tx<numXTiles; tx++) {
                final int first = (ti + 1) * 100 + offset;
                final int x = tx * tileWidth  + minX;
                final int y = ty * tileHeight + minY;
                final WritableRaster raster = Raster.createWritableRaster(sampleModel, new Point(x, y));
                for (int j=0; j<tileHeight; j++) {
                    for (int i=0; i<tileWidth; i++) {
                        final int sample = first + 10 * j + i;
                        for (final int band : bands) {
                            raster.setSample(x+i, y+j, band, (band + 1) * 1000 + sample);
                        }
                    }
                }
                tiles[ti++] = raster;
            }
        }
        assertEquals(tiles.length, ti);
    }

    /**
     * Initializes the sample values of all tiles to testing values in a single band.
     * The sample values will be 3 digits numbers of the form "TYX" where:
     * <ul>
     *   <li><var>T</var> is the tile index starting with 1 for the first tile and increasing in a row-major fashion.</li>
     *   <li><var>Y</var> is the <var>y</var> coordinate (row 0-based index) of the sample value relative to current tile.</li>
     *   <li><var>X</var> is the <var>x</var> coordinate (column 0-based index) of the sample value relative to current tile.</li>
     * </ul>
     *
     * The "TYX" pattern holds if all values are less than 10. If some values are greater than 10,
     * then the sample values are a mix of above values resulting from the arithmetic sum defined
     * in {@link #initializeAllTiles(int[], int)}.
     *
     * @param band  index of the band where to apply the "TYX" pattern. Other bands will be initialized to zero.
     */
    public void initializeAllTiles(final int band) {
        initializeAllTiles(new int[] {band}, (band + 1) * -1000);       // Offset cancels the "B" in "BTYX" pattern.
    }

    /**
     * Initializes the sample values of all tiles to random values. The image must have been
     * initialized by a call to {@link #initializeAllTiles(int)} before to invoke this method.
     *
     * @param  band       band index where to set values. Other bands will be unmodified.
     * @param  generator  the random number generator to use for obtaining values.
     * @param  upper      upper limit (exclusive) of random numbers to generate.
     */
    public synchronized void setRandomValues(final int band, final Random generator, final int upper) {
        for (final WritableRaster raster : tiles) {
            final int x = raster.getMinX();
            final int y = raster.getMinY();
            for (int j=0; j<tileHeight; j++) {
                for (int i=0; i<tileWidth; i++) {
                    raster.setSample(x+i, y+j, band, generator.nextInt(upper));
                }
            }
        }
    }

    /**
     * Returns the tile at the given location in tile coordinates.
     * This method verifies that no writable raster have been acquired. Actually this conditions is not part of
     * {@link WritableRenderedImage} contract, since a readable and writable rasters can be used at the same time.
     * But we add this condition because they way {@link PixelIterator} are currently implemented, it would be
     * a bug if we ask for a readable tile while we already have a writable one. This condition may change in
     * any future Apache SIS version.
     */
    @Override
    public synchronized Raster getTile(final int tileX, final int tileY) {
        assertFalse(isTileAcquired, "isTileAcquired");              // See javadoc.
        return tile(tileX, tileY, false);
    }

    /**
     * Returns the tile at the given location tile coordinates.
     */
    @Override
    public synchronized WritableRaster getWritableTile(final int tileX, final int tileY) {
        assertFalse(isTileAcquired, "isTileAcquired");
        final WritableRaster raster = tile(tileX, tileY, true);
        isTileAcquired = true;
        acquiredTileX  = tileX;
        acquiredTileY  = tileY;
        return raster;
    }

    /**
     * Returns the tile at the given index without any verification. It is caller responsibility to verify if this
     * method is invoked in a consistent context (for example after a writable raster has been properly acquired).
     */
    private WritableRaster tile(int tileX, int tileY, final boolean allowCreate) {
        if ((tileX -= minTileX) < 0 || tileX >= numXTiles ||
            (tileY -= minTileY) < 0 || tileY >= numYTiles)
        {
            throw new IndexOutOfBoundsException();
        }
        final int i = tileY * numXTiles + tileX;
        if ((constantFailures != null && constantFailures[i]) ||
            (randomFailures != null && randomFailures.nextInt(FAILURE_PROBABILITY) == 0))
        {
            throw new ImagingOpException("Artificial error #" + errorSequence.incrementAndGet()
                                       + " on tile (" + tileX + ", " + tileY + ").");
        }
        WritableRaster raster = tiles[i];
        if (raster == null) {
            if (!allowCreate) {
                throw new RasterFormatException("Requested tile has not yet been defined.");
            }
            final int x = tileX * tileWidth  + minX;
            final int y = tileY * tileHeight + minY;
            tiles[i] = raster = Raster.createWritableRaster(sampleModel, new Point(x, y));
        }
        return raster;
    }

    /**
     * Verifies that the given tile has been acquired.
     */
    @Override
    public synchronized void releaseWritableTile(final int tileX, final int tileY) {
        assertTrue(isTileAcquired, "isTileAcquired");
        assertEquals(acquiredTileX, tileX, "tileX");
        assertEquals(acquiredTileY, tileY, "tileY");
        isTileAcquired = false;
    }

    /**
     * Returns {@code true} if the given tile indices are the one given to the last call to
     * {@link #getWritableTile(int, int)} and that tile has not yet been released.
     */
    @Override
    public synchronized boolean isTileWritable(final int tileX, final int tileY) {
        return isTileAcquired && (tileX == acquiredTileX) && (tileY == acquiredTileY);
    }

    /**
     * Returns {@code false} since we do not keep track of who called {@link #getWritableTile(int,int)}.
     */
    @Override
    public synchronized boolean hasTileWriters() {
        return isTileAcquired;
    }

    /**
     * Returns the indices of acquired tile, or {@code null} if none.
     */
    @Override
    public synchronized Point[] getWritableTileIndices() {
        return isTileAcquired ? new Point[] {new Point(acquiredTileX, acquiredTileY)} : null;
    }

    /**
     * Ignored since we do not need tile observers for the tests.
     */
    @Override
    public void addTileObserver(TileObserver to) {
    }

    /**
     * Ignored since no listener can have been registered.
     */
    @Override
    public void removeTileObserver(TileObserver to) {
    }

    /**
     * Sets a rectangle of this image to the contents of given raster.
     * The raster is assumed to be in the same coordinate space as this image.
     * Current implementation can set raster covering only one tile.
     */
    @Override
    public synchronized void setData(final Raster r) {
        final int minX = r.getMinX();
        final int minY = r.getMinY();
        final int tx = ImageUtilities.pixelToTileX(this, minX);
        final int ty = ImageUtilities.pixelToTileY(this, minY);
        assertEquals(tx, ImageUtilities.pixelToTileX(this, minX + r.getWidth()  - 1), "Unsupported operation.");
        assertEquals(ty, ImageUtilities.pixelToTileX(this, minY + r.getHeight() - 1), "Unsupported operation.");
        tile(tx, ty, true).setRect(r);
    }

    /**
     * Returns this image as a {@link WritableTiledImage} implementation.
     * This is useful if a more complete implementation of {@link #setData(Raster)} (for example) is needed.
     *
     * @return this image as a more complete implementation.
     */
    public synchronized WritableTiledImage toWritableTiledImage() {
        return new WritableTiledImage(null, null, width, height, minTileX, minTileY, tiles);
    }
}
