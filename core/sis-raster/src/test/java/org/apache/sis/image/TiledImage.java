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

import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.TileObserver;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import java.util.Vector;

import static org.junit.Assert.*;


/**
 * A rendered image which can contain an arbitrary number of tiles. Tiles are stored in memory.
 * We use this class for testing purpose only because tiled images in production need a more
 * sophisticated implementation capable to store some tiles on disk (for memory consumption reasons).
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class TiledImage implements WritableRenderedImage {
    /**
     * The minimum X or Y coordinate (inclusive) of the rendered image.
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
     * The minimum tile index in the X or Y direction.
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
     * Creates a new tiled image.
     *
     * @param dataType  sample data type as one of the {@link java.awt.image.DataBuffer} constants.
     * @param numBands  number of bands in the sample model to create.
     */
    TiledImage(final int dataType,  final int numBands,
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
        this.numXTiles   = (width  + tileWidth  - 1) / tileWidth;      // Round toward upper integer value.
        this.numYTiles   = (height + tileHeight - 1) / tileHeight;
        this.tiles       = new WritableRaster[numXTiles * numYTiles];
        this.sampleModel = new PixelInterleavedSampleModel(dataType, tileWidth, tileHeight,
                                numBands, tileWidth * numBands, bandOffsets(numBands));
    }

    /**
     * Returns arbitrary band offsets for the given number of bands.
     */
    static int[] bandOffsets(final int numBands) {
        final int[] bandOffsets = new int[numBands];
        for (int i=1; i<numBands; i++) {
            bandOffsets[i] = i;
        }
        return bandOffsets;
    }

    /*
     * No source, no property, no color model since this test images is not for rendering on screen.
     */
    @Override public Vector<RenderedImage> getSources()             {return null;}
    @Override public Object                getProperty(String name) {return Image.UndefinedProperty;}
    @Override public String[]              getPropertyNames()       {return null;}
    @Override public ColorModel            getColorModel()          {return null;}
    @Override public SampleModel           getSampleModel()         {return sampleModel;}

    /*
     * Information specified to the constructor.
     */
    @Override public int getMinX()            {return minX;}
    @Override public int getMinY()            {return minY;}
    @Override public int getWidth()           {return width;}
    @Override public int getHeight()          {return height;}
    @Override public int getTileWidth()       {return tileWidth;}
    @Override public int getTileHeight()      {return tileHeight;}
    @Override public int getNumXTiles()       {return numXTiles;}
    @Override public int getNumYTiles()       {return numYTiles;}
    @Override public int getMinTileX()        {return minTileX;}
    @Override public int getMinTileY()        {return minTileY;}
    @Override public int getTileGridXOffset() {return minX - (minTileX * tileWidth);}
    @Override public int getTileGridYOffset() {return minY - (minTileY * tileHeight);}

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
        tile(ox / tileWidth  + minTileX,
             oy / tileHeight + minTileY).setSample(x, y, b, value);
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
        return tile(tileX, tileY);
    }

    /**
     * Returns the tile at the given location tile coordinates.
     */
    @Override
    public WritableRaster getWritableTile(final int tileX, final int tileY) {
        assertFalse("isTileAcquired", isTileAcquired);
        isTileAcquired = true;
        acquiredTileX  = tileX;
        acquiredTileY  = tileY;
        return tile(tileX, tileY);
    }

    /**
     * Returns the tile at the given index without any verification. It is caller responsibility to verify if this
     * method is invoked in a consistent context (for example after a writable raster has been properly acquired).
     */
    private WritableRaster tile(int tileX, int tileY) {
        if ((tileX -= minTileX) < 0 || tileX >= numXTiles ||
            (tileY -= minTileY) < 0 || tileY >= numYTiles)
        {
            throw new IndexOutOfBoundsException();
        }
        final int i = tileY * numXTiles + tileX;
        WritableRaster raster = tiles[i];
        if (raster == null) {
            tiles[i] = raster = Raster.createWritableRaster(sampleModel,
                    new Point(tileX * tileWidth  + minX,
                              tileY * tileHeight + minY));
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
     * Unsupported since we do not need tile observers for the tests.
     */
    @Override
    public void addTileObserver(TileObserver to) {
        throw new UnsupportedOperationException();
    }

    /**
     * Ignored since no listener can have been registered.
     */
    @Override
    public void removeTileObserver(TileObserver to) {
    }

    /*
     * Not needed for the tests.
     */
    @Override public Raster         getData()                       {throw new UnsupportedOperationException();}
    @Override public Raster         getData(Rectangle rect)         {throw new UnsupportedOperationException();}
    @Override public void           setData(Raster r)               {throw new UnsupportedOperationException();}
    @Override public WritableRaster copyData(WritableRaster raster) {throw new UnsupportedOperationException();}
}
