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

import java.util.Vector;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import org.apache.sis.internal.coverage.j2d.PlanarImage;


/**
 * A view over another image with the origin relocated to a new position.
 * If the image is tiled, this wrapper may also reduce the number of tiles.
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
     * Coordinate of the upper-left pixel.
     * Computed at construction time in order to detect integer overflows early.
     */
    private final int minX, minY;

    /**
     * Creates a new image with the same data than the given image but located at given coordinates.
     *
     * @param  image  the image to move.
     * @param  minX   <var>x</var> coordinate of upper-left pixel.
     * @param  minY   <var>y</var> coordinate of upper-left pixel.
     */
    private RelocatedImage(final RenderedImage image, final int minX, final int minY) {
        this.image = image;
        this.minX  = minX;
        this.minY  = minY;
    }

    /**
     * Returns an image with the same data than the given image but located at given coordinates.
     * Caller should verify that the given image is not null and not already at the given location.
     *
     * @param  image  the image to move.
     * @param  minX   <var>x</var> coordinate of upper-left pixel.
     * @param  minY   <var>y</var> coordinate of upper-left pixel.
     * @return image with the same data but at the given coordinates.
     */
    static RenderedImage moveTo(RenderedImage image, final int minX, final int minY) {
        if (image instanceof RelocatedImage) {
            image = (RelocatedImage) image;
            if (minX == image.getMinX() && minY == image.getMinY()) {
                return image;
            }
        }
        return new RelocatedImage(image, minX, minY);
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
    @Override public int         getWidth()               {return image.getWidth();}
    @Override public int         getHeight()              {return image.getHeight();}
    @Override public int         getNumXTiles()           {return image.getNumXTiles();}
    @Override public int         getNumYTiles()           {return image.getNumYTiles();}
    @Override public int         getMinTileX()            {return image.getMinTileX();}
    @Override public int         getMinTileY()            {return image.getMinTileY();}
    @Override public int         getTileWidth()           {return image.getTileWidth();}
    @Override public int         getTileHeight()          {return image.getTileHeight();}

    /**
     * Returns the minimum <var>x</var> coordinate (inclusive) specified at construction time.
     * This coordinate may differ from the coordinate of the wrapped image.
     */
    @Override
    public int getMinX() {
        return minX;
    }

    /**
     * Returns the minimum <var>y</var> coordinate (inclusive) specified at construction time.
     * This coordinate may differ from the coordinate of the wrapped image.
     */
    @Override
    public int getMinY() {
        return minY;
    }

    /**
     * Returns the <var>x</var> coordinate of the upper-left pixel of tile (0, 0).
     * That tile (0, 0) may not actually exist.
     */
    @Override
    public int getTileGridXOffset() {
        return offsetX(image.getTileGridXOffset());
    }

    /**
     * Returns the <var>y</var> coordinate of the upper-left pixel of tile (0, 0).
     * That tile (0, 0) may not actually exist.
     */
    @Override
    public int getTileGridYOffset() {
        return offsetY(image.getTileGridYOffset());
    }

    /**
     * Converts a column index from the coordinate system of the wrapped image
     * to the coordinate system of this image.
     *
     * @param  x  a column index of the wrapped image.
     * @return the corresponding column index in this image.
     */
    private int offsetX(final int x) {
        return Math.toIntExact(x + (minX - (long) image.getMinX()));
    }

    /**
     * Converts a row index from the coordinate system of the wrapped image
     * to the coordinate system of this image.
     *
     * @param  y  a row index of the wrapped image.
     * @return the corresponding row index in this image.
     */
    private int offsetY(final int y) {
        return Math.toIntExact(y + (minY - (long) image.getMinY()));
    }

    /**
     * Returns a raster with the same data than the given raster but with coordinates translated
     * from the coordinate system of the wrapped image to the coordinate system of this image.
     * The returned raster will have the given raster as its parent.
     */
    private Raster offset(final Raster data) {
        return data.createTranslatedChild(offsetX(data.getMinX()), offsetY(data.getMinY()));
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
        final long offsetX = minX - (long) image.getMinX();
        final long offsetY = minY - (long) image.getMinY();
        aoi = new Rectangle(aoi);
        aoi.x = Math.toIntExact(aoi.x - offsetX);       // Inverse of offsetX(int).
        aoi.y = Math.toIntExact(aoi.y - offsetY);
        final Raster data = image.getData(aoi);
        return data.createTranslatedChild(Math.toIntExact(data.getMinX() + offsetX),
                                          Math.toIntExact(data.getMinY() + offsetY));
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
        final long offsetX = minX - (long) image.getMinX();
        final long offsetY = minY - (long) image.getMinY();
        WritableRaster data;
        if (raster != null) {
            data = raster.createWritableTranslatedChild(
                    Math.toIntExact(raster.getMinX() - offsetX),
                    Math.toIntExact(raster.getMinY() - offsetY));
        } else {
            data = null;
        }
        data = image.copyData(data);
        if (data.getWritableParent() == raster) {
            return raster;
        }
        return data.createWritableTranslatedChild(Math.toIntExact(data.getMinX() + offsetX),
                                                  Math.toIntExact(data.getMinY() + offsetY));
    }
}
