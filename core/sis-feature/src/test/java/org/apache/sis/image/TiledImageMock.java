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
import java.awt.image.ColorModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RasterFormatException;
import java.awt.image.SampleModel;
import java.awt.image.TileObserver;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.util.ArraysExt;

import static org.junit.Assert.*;


/**
 * A rendered image which can contain an arbitrary number of tiles. Tiles are stored in memory.
 * We use this class for testing purpose only because tiled images in production use need a more
 * sophisticated implementation capable to store some tiles on disk (for memory consumption reasons).
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   0.8
 * @module
 */
public final strictfp class TiledImageMock extends PlanarImage implements WritableRenderedImage {
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
     */
    public TiledImageMock(final int dataType,  final int numBands,
                          final int minX,      final int minY,
                          final int width,     final int height,
                          final int tileWidth, final int tileHeight,
                          final int minTileX,  final int minTileY)
    {
        this.minX        = minX;
        this.minY        = minY;
        this.width       = width;
        this.height      = height;
        this.tileWidth   = tileWidth;
        this.tileHeight  = tileHeight;
        this.minTileX    = minTileX;
        this.minTileY    = minTileY;
        this.numXTiles   = Numerics.ceilDiv(width,  tileWidth);
        this.numYTiles   = Numerics.ceilDiv(height, tileHeight);
        this.tiles       = new WritableRaster[numXTiles * numYTiles];
        this.sampleModel = new PixelInterleavedSampleModel(dataType, tileWidth, tileHeight,
                                numBands, tileWidth * numBands, ArraysExt.range(0, numBands));
    }

    /**
     * No color model since this test images is not for rendering on screen.
     */
    @Override public ColorModel  getColorModel()  {return null;}
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
    public void validate() {
        assertNull(verify());
    }

    /**
     * Sets a sample value at the given location in pixel coordinates.
     * This is a helper method for testing purpose on small images only,
     * since invoking this method in a loop is inefficient.
     */
    final void setSample(final int x, final int y, final int b, final double value) {
        final int ox = x - minX;
        final int oy = y - minY;
        if (ox < 0 || ox >= width || oy < 0 || oy >= height) {
            throw new IndexOutOfBoundsException();
        }
        tile(StrictMath.floorDiv(ox, tileWidth)  + minTileX,
             StrictMath.floorDiv(oy, tileHeight) + minTileY, true).setSample(x, y, b, value);
    }

    /**
     * Initializes the sample values of all tiles to testing values.
     * The sample values will be 3 digits numbers of the form "TXY" where:
     * <ul>
     *   <li><var>T</var> is the tile index starting with 1 for the first tile and increasing in a row-major fashion.</li>
     *   <li><var>X</var> is the <var>x</var> coordinate (column index) of the sample value relative to current tile.</li>
     *   <li><var>Y</var> is the <var>y</var> coordinate (row index) of the sample value relative to current tile.</li>
     * </ul>
     *
     * @param  band  band index where to set values. Other bands will be unmodified.
     */
    public void initializeAllTiles(final int band) {
        int ti = 0;
        for (int ty=0; ty<numYTiles; ty++) {
            for (int tx=0; tx<numXTiles; tx++) {
                final int value = (ti + 1) * 100;
                final int x = tx * tileWidth  + minX;
                final int y = ty * tileHeight + minY;
                final WritableRaster raster = Raster.createWritableRaster(sampleModel, new Point(x, y));
                for (int j=0; j<tileHeight; j++) {
                    for (int i=0; i<tileWidth; i++) {
                        raster.setSample(x+i, y+j, band, value + 10*j + i);
                    }
                }
                tiles[ti++] = raster;
            }
        }
        assertEquals(tiles.length, ti);
    }

    /**
     * Returns the tile at the given location in tile coordinates.
     * This method verifies that no writable raster have been acquired. Actually this conditions is not part of
     * {@link WritableRenderedImage} contract, since a readable and writable rasters can be used in same time.
     * But we add this condition because they way {@link PixelIterator} are currently implemented, it would be
     * a bug if we ask for a readable tile while we already have a writable one. This condition may change in
     * any future Apache SIS version.
     */
    @Override
    public Raster getTile(final int tileX, final int tileY) {
        assertFalse("isTileAcquired", isTileAcquired);              // See javadoc.
        return tile(tileX, tileY, false);
    }

    /**
     * Returns the tile at the given location tile coordinates.
     */
    @Override
    public WritableRaster getWritableTile(final int tileX, final int tileY) {
        assertFalse("isTileAcquired", isTileAcquired);
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
    public void releaseWritableTile(final int tileX, final int tileY) {
        assertTrue("isTileAcquired", isTileAcquired);
        assertEquals("tileX", acquiredTileX, tileX);
        assertEquals("tileY", acquiredTileY, tileY);
        isTileAcquired = false;
    }

    /**
     * Returns {@code true} if the given tile indices are the one given to the last call to
     * {@link #getWritableTile(int, int)} and that tile has not yet been released.
     */
    @Override
    public boolean isTileWritable(final int tileX, final int tileY) {
        return isTileAcquired && (tileX == acquiredTileX) && (tileY == acquiredTileY);
    }

    /**
     * Returns {@code false} since we do not keep track of who called {@link #getWritableTile(int,int)}.
     */
    @Override
    public boolean hasTileWriters() {
        return isTileAcquired;
    }

    /**
     * Returns the indices of acquired tile, or {@code null} if none.
     */
    @Override
    public Point[] getWritableTileIndices() {
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
     * Not needed for the tests.
     */
    @Override
    public void setData(Raster r) {
        throw new UnsupportedOperationException();
    }
}
