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
package org.apache.sis.coverage.grid;

import java.util.Vector;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import org.apache.sis.internal.coverage.j2d.PlanarImage;

import static java.lang.Math.min;
import static java.lang.Math.max;
import static java.lang.Math.addExact;
import static java.lang.Math.subtractExact;
import static java.lang.Math.floorDiv;
import static java.lang.Math.toIntExact;


/**
 * A view over another image with the origin relocated to a new position.
 * Only the pixel coordinates are changed; the tile indices stay the same.
 * However the image view may expose less tiles than the wrapped image.
 * This wrapper does not change image size otherwise than by an integer amount of tiles.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class RelocatedImage extends PlanarImage {
    /**
     * The image to translate.
     */
    private final RenderedImage image;

    /**
     * Value to add for converting a column index from the coordinate system of the wrapped image
     * to the coordinate system of this image. For a conversion in opposite direction, that value
     * shall be subtracted.
     */
    private final int offsetX;

    /**
     * Value to add for converting a row index from the coordinate system of the wrapped image to
     * the coordinate system of this image. For a conversion in opposite direction, that value
     * shall be subtracted.
     */
    private final int offsetY;

    /**
     * The image size in pixels. May be smaller than {@link #image} size by an integer amount of tiles.
     */
    private final int width, height;

    /**
     * Coordinate of the upper-left pixel.
     * Computed at construction time in order to detect integer overflows early.
     */
    private final int minX, minY;

    /**
     * Index in tile matrix of the upper-left tile.
     * Computed at construction time in order to detect integer overflows early.
     */
    private final int minTileX, minTileY;

    /**
     * Creates a new image with the same data than the given image but located at different coordinates.
     * In addition, this constructor can reduce the number of tiles.
     *
     * @param  image  the image to move.
     * @param  xmin   minimal <var>x</var> coordinate of the requested region, inclusive.
     * @param  ymin   minimal <var>y</var> coordinate of the requested region, inclusive.
     * @param  xmax   maximal <var>x</var> coordinate of the requested region, inclusive.
     * @param  ymax   maximal <var>y</var> coordinate of the requested region, inclusive.
     * @throws ArithmeticException if image indices would overflow 32 bits integer capacity.
     */
    RelocatedImage(final RenderedImage image, final long xmin, final long ymin, final long xmax, final long ymax) {
        this.image = image;
        /*
         * Compute indices of all tiles to retain in this image. All local fields are `long` in order to force
         * 64-bits integer arithmetic, because may have temporary 32-bits integer overflow during intermediate
         * calculation but still have a final result representable as an `int`. The use of `min` and `max` are
         * paranoiac safety against long integer overflow; real clamping will be done later.
         */
        final long lowerX = image.getMinX();                        // Lower source index (inclusive)
        final long lowerY = image.getMinY();
        final long upperX = image.getWidth()  + lowerX;             // Upper image index (exclusive).
        final long upperY = image.getHeight() + lowerY;
        final long tw     = image.getTileWidth();
        final long th     = image.getTileHeight();
        final long xo     = image.getTileGridXOffset();
        final long yo     = image.getTileGridYOffset();
        final long minTX  = floorDiv(max(lowerX, xmin) - xo, tw);   // Indices of the first tile to retain.
        final long minTY  = floorDiv(max(lowerY, ymin) - yo, th);
        final long maxTX  = floorDiv(min(upperX, xmax) - xo, tw);   // Indices of the last tile to retain (inclusive).
        final long maxTY  = floorDiv(min(upperY, ymax) - yo, th);
        /*
         * Coordinates in source image of the first pixel to show in this relocated image.
         * They are the coordinates of the upper-left corner of the first tile to retain,
         * clamped to image bounds if needed. This is not yet coordinates of this image.
         */
        final long sx = max(lowerX, minTX * tw + xo);
        final long sy = max(lowerY, minTY * th + yo);
        /*
         * As per GridCoverage2D contract, we shall set the (x,y) location to the difference between
         * requested region and actual region of this image. For example if the user requested image
         * starting at (5,5) but the data starts at (1,1), then we need to set location to (-4,-4).
         */
        final long x = subtractExact(sx, xmin);
        final long y = subtractExact(sy, ymin);
        minX     = toIntExact(x);
        minY     = toIntExact(y);
        width    = toIntExact(min(upperX, (maxTX + 1) * tw + xo) - sx);
        height   = toIntExact(min(upperY, (maxTY + 1) * th + yo) - sy);
        offsetX  = toIntExact(x - lowerX);
        offsetY  = toIntExact(y - lowerY);
        minTileX = toIntExact(minTX);
        minTileY = toIntExact(minTY);
    }

    /**
     * Returns {@code true} if this image does not move and does not subset the wrapped image.
     */
    final boolean isIdentity() {
        return offsetX == 0 && offsetY == 0 &&
               minX == image.getMinX() && width  == image.getWidth() &&
               minY == image.getMinY() && height == image.getHeight();
    }

    /**
     * Returns the immediate source of this image.
     */
    @Override
    @SuppressWarnings("UseOfObsoleteCollectionType")
    public Vector<RenderedImage> getSources() {
        final Vector<RenderedImage> sources = new Vector<>(1);
        sources.add(image);
        return sources;
    }

    /**
     * Delegates to the wrapped image with no change.
     */
    @Override public Object      getProperty(String name) {return image.getProperty(name);}
    @Override public String[]    getPropertyNames()       {return image.getPropertyNames();}
    @Override public ColorModel  getColorModel()          {return image.getColorModel();}
    @Override public SampleModel getSampleModel()         {return image.getSampleModel();}
    @Override public int         getTileWidth()           {return image.getTileWidth();}
    @Override public int         getTileHeight()          {return image.getTileHeight();}

    /**
     * Returns properties determined at construction time.
     */
    @Override public int getMinX()     {return minX;}
    @Override public int getMinY()     {return minY;}
    @Override public int getWidth()    {return width;}
    @Override public int getHeight()   {return height;}
    @Override public int getMinTileX() {return minTileX;}
    @Override public int getMinTileY() {return minTileY;}

    /**
     * Returns the <var>x</var> coordinate of the upper-left pixel of tile (0, 0).
     * That tile (0, 0) may not actually exist.
     */
    @Override
    public int getTileGridXOffset() {
        return addExact(image.getTileGridXOffset(), offsetX);
    }

    /**
     * Returns the <var>y</var> coordinate of the upper-left pixel of tile (0, 0).
     * That tile (0, 0) may not actually exist.
     */
    @Override
    public int getTileGridYOffset() {
        return addExact(image.getTileGridYOffset(), offsetY);
    }

    /**
     * Returns a raster with the same data than the given raster but with coordinates translated
     * from the coordinate system of the wrapped image to the coordinate system of this image.
     * The returned raster will have the given raster as its parent.
     */
    private Raster offset(final Raster data) {
        return data.createTranslatedChild(addExact(data.getMinX(), offsetX),
                                          addExact(data.getMinY(), offsetY));
    }

    /**
     * Returns the tile at the given tile indices (not to be confused with pixel indices).
     *
     * @param  tileX  the <var>x</var> index of the requested tile in the tile array.
     * @param  tileY  the <var>y</var> index of the requested tile in the tile array.
     * @return the tile specified by the specified indices.
     */
    @Override
    public Raster getTile(final int tileX, final int tileY) {
        return offset(image.getTile(tileX, tileY));
    }

    /**
     * Returns a copy of this image as one large tile.
     * The returned raster will not be updated if this image is changed.
     *
     * @return a copy of this image as one large tile.
     */
    @Override
    public Raster getData() {
        return offset(image.getData());
    }

    /**
     * Returns a copy of an arbitrary region of this image.
     * The returned raster will not be updated if this image is changed.
     *
     * @param  aoi  the region of this image to copy.
     * @return a copy of this image in the given area of interest.
     */
    @Override
    public Raster getData(Rectangle aoi) {
        aoi = new Rectangle(aoi);
        aoi.x = subtractExact(aoi.x, offsetX);      // Convert coordinate from this image to wrapped image.
        aoi.y = subtractExact(aoi.y, offsetY);
        final Raster data = image.getData(aoi);
        return data.createTranslatedChild(addExact(data.getMinX(), offsetX),
                                          addExact(data.getMinY(), offsetY));
    }

    /**
     * Copies an arbitrary rectangular region of this image to the supplied writable raster.
     * The region to be copied is determined from the bounds of the supplied raster.
     *
     * @param  raster  the raster to hold a copy of this image, or {@code null}.
     * @return the given raster if it was not-null, or a new raster otherwise.
     */
    @Override
    public WritableRaster copyData(final WritableRaster raster) {
        WritableRaster data;
        if (raster != null) {
            data = raster.createWritableTranslatedChild(
                    subtractExact(raster.getMinX(), offsetX),
                    subtractExact(raster.getMinY(), offsetY));
        } else {
            data = null;
        }
        data = image.copyData(data);
        if (data.getWritableParent() == raster) {
            return raster;
        }
        return data.createWritableTranslatedChild(addExact(data.getMinX(), offsetX),
                                                  addExact(data.getMinY(), offsetY));
    }

    /**
     * Verifies whether image layout information are consistent.
     */
    @Override
    public String verify() {
        final String error = super.verify();
        if (error == null) {
            if (getMinX() != image.getMinX() + offsetX) return "minX";
            if (getMinY() != image.getMinY() + offsetY) return "minY";
            if (getTileGridXOffset() != super.getTileGridXOffset()) return "tileGridXOffset";
            if (getTileGridYOffset() != super.getTileGridYOffset()) return "tileGridYOffset";
        }
        return error;
    }
}
