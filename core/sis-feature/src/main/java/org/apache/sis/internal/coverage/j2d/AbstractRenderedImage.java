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
package org.apache.sis.internal.coverage.j2d;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.SampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RasterFormatException;
import java.awt.image.WritableRaster;
import java.awt.image.RenderedImage;
import java.util.Vector;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Classes;


/**
 * Skeleton implementation of {@link RenderedImage}.
 * Current implementation does not hold any state.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public abstract class AbstractRenderedImage implements RenderedImage {
    /**
     * Approximate size of the buffer to use for copying data from the image to a raster, in bits.
     * The actual buffer size may be smaller or larger, depending on the actual tile size.
     */
    private static final int BUFFER_SIZE = 8192 * Byte.SIZE;

    /**
     * Creates a new rendered image.
     */
    protected AbstractRenderedImage() {
    }

    /**
     * Returns the immediate sources of image data for this image.
     * This method returns {@code null} if the image has no information about its immediate sources.
     * It returns an empty vector if the image object has no immediate sources.
     *
     * <p>The default implementation returns {@code null}.
     * Note that this is not equivalent to an empty vector.</p>
     *
     * @return the immediate sources, or {@code null} if unknown.
     */
    @Override
    @SuppressWarnings("UseOfObsoleteCollectionType")
    public Vector<RenderedImage> getSources() {
        return null;
    }

    /**
     * Gets a property from this image.
     * This method returns {@link Image#UndefinedProperty} if the specified property is not defined.
     *
     * <p>The default implementation returns {@link Image#UndefinedProperty} in all cases.</p>
     *
     * @param  name  the name of the property to get.
     * @return the property value, or {@link Image#UndefinedProperty} if none.
     */
    @Override
    public Object getProperty(String name) {
        return Image.UndefinedProperty;
    }

    /**
     * Returns the names of all recognized properties,
     * or {@code null} if this image has no properties.
     *
     * <p>The default implementation returns {@code null}.</p>
     *
     * @return names of all recognized properties, or {@code null} if none.
     */
    @Override
    public String[] getPropertyNames() {
        return null;
    }

    /**
     * Returns the number of tiles in the X direction.
     *
     * <p>The default implementation computes this value from {@link #getWidth()} and {@link #getTileWidth()}.</p>
     *
     * @return returns the number of tiles in the X direction.
     */
    @Override
    public int getNumXTiles() {
        return Numerics.ceilDiv(getWidth(), getTileWidth());
    }

    /**
     * Returns the number of tiles in the Y direction.
     *
     * <p>The default implementation computes this value from {@link #getHeight()} and {@link #getTileHeight()}.</p>
     *
     * @return returns the number of tiles in the Y direction.
     */
    @Override
    public int getNumYTiles() {
        return Numerics.ceilDiv(getHeight(), getTileHeight());
    }

    /**
     * Returns the X coordinate of the upper-left pixel of tile (0, 0).
     * That tile (0, 0) may not actually exist.
     *
     * <p>The default implementation computes this value from {@link #getMinX()},
     * {@link #getMinTileX()} and {@link #getTileWidth()}.</p>
     *
     * @return the X offset of the tile grid relative to the origin.
     */
    @Override
    public int getTileGridXOffset() {
        return Math.subtractExact(getMinX(), Math.multiplyExact(getMinTileX(), getTileWidth()));
    }

    /**
     * Returns the Y coordinate of the upper-left pixel of tile (0, 0).
     * That tile (0, 0) may not actually exist.
     *
     * <p>The default implementation computes this value from {@link #getMinY()},
     * {@link #getMinTileY()} and {@link #getTileHeight()}.</p>
     *
     * @return the Y offset of the tile grid relative to the origin.
     */
    @Override
    public int getTileGridYOffset() {
        return Math.subtractExact(getMinY(), Math.multiplyExact(getMinTileY(), getTileHeight()));
    }

    /**
     * Creates a raster with the same sample model than this image and with the given size and location.
     * This method does not verify argument validity.
     */
    private WritableRaster createWritableRaster(final Rectangle aoi) {
        final SampleModel sm = getSampleModel().createCompatibleSampleModel(aoi.width, aoi.height);
        return Raster.createWritableRaster(sm, aoi.getLocation());
    }

    /**
     * Returns the size in bits of the transfer type, or an arbitrary value if that type is unknown.
     * For this class it is okay if the value is not accurate; this method is used only for adjusting
     * the {@link #BUFFER_SIZE} value.
     *
     * @param  raster  the raster for which to get transfer type size.
     * @return size in bits of transfer type. May be an arbitrary size.
     */
    private static int getTransferTypeSize(final Raster raster) {
        try {
            return DataBuffer.getDataTypeSize(raster.getTransferType());
        } catch (IllegalArgumentException e) {
            return Short.SIZE;
        }
    }

    /**
     * Returns a copy of this image as one large tile.
     * The returned raster will not be updated if the image is changed.
     *
     * @return a copy of this image as one large tile.
     */
    @Override
    public Raster getData() {
        final Rectangle aoi = ImageUtilities.getBounds(this);
        final WritableRaster raster = createWritableRaster(aoi);
        copyData(aoi, raster);
        return raster;
    }

    /**
     * Returns a copy of an arbitrary region of this image.
     * The returned raster will not be updated if the image is changed.
     *
     * @param  aoi  the region of this image to copy.
     * @return a copy of this image in the given area of interest.
     * @throws IllegalArgumentException if the given rectangle is not contained in this image bounds.
     */
    @Override
    public Raster getData(final Rectangle aoi) {
        ArgumentChecks.ensureNonNull("aoi", aoi);
        if (!ImageUtilities.getBounds(this).contains(aoi)) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.OutsideDomainOfValidity));
        }
        final WritableRaster raster = createWritableRaster(aoi);
        copyData(aoi, raster);
        return raster;
    }

    /**
     * Copies an arbitrary rectangular region of this image to the supplied writable raster.
     * The region to be copied is determined from the bounds of the supplied raster.
     * The supplied raster must have a {@link SampleModel} that is compatible with this image.
     * If the raster is {@code null}, an raster is created by this method.
     *
     * @param  raster  the raster to hold a copy of this image, or {@code null}.
     * @return the given raster if it was not-null, or a new raster otherwise.
     */
    @Override
    public WritableRaster copyData(WritableRaster raster) {
        final Rectangle aoi;
        if (raster != null) {
            aoi = raster.getBounds();
        } else {
            aoi = ImageUtilities.getBounds(this);
            raster = createWritableRaster(aoi);
        }
        copyData(aoi, raster);
        return raster;
    }

    /**
     * Implementation of {@link #getData()}, {@link #getData(Rectangle)} and {@link #copyData(WritableRaster)}.
     * It is caller responsibility to ensure that all arguments are non-null and that the rectangle is contained
     * inside both this image and the given raster.
     *
     * @param  aoi  the region of this image to copy.
     * @param  raster  the raster to hold a copy of this image, or {@code null}.
     */
    private void copyData(final Rectangle aoi, final WritableRaster raster) {
        final int  tileWidth       = getTileWidth();
        final int  tileHeight      = getTileHeight();
        final long tileGridXOffset = getTileGridXOffset();          // We want 64 bits arithmetic in operations below.
        final long tileGridYOffset = getTileGridYOffset();
        final int  minTileX = Math.toIntExact(Math.floorDiv(aoi.x                     - tileGridXOffset, tileWidth));
        final int  minTileY = Math.toIntExact(Math.floorDiv(aoi.y                     - tileGridYOffset, tileHeight));
        final int  maxTileX = Math.toIntExact(Math.floorDiv(aoi.x + (aoi.width  - 1L) - tileGridXOffset, tileWidth));
        final int  maxTileY = Math.toIntExact(Math.floorDiv(aoi.y + (aoi.height - 1L) - tileGridYOffset, tileHeight));
        /*
         * Iterate over all tiles that interesect the area of interest. For each tile,
         * copy a few rows in a temporary buffer, then copy that buffer to destination.
         * The buffer will be reused for each transfer, unless its size is insufficient.
         */
        Object buffer = null;
        int bufferCapacity = 0;
        for (int ty = minTileY; ty <= maxTileY; ty++) {
            for (int tx = minTileX; tx <= maxTileX; tx++) {
                final Raster tile = getTile(tx, ty);
                final Rectangle tb = aoi.intersection(tile.getBounds());        // Bounds of transfer buffer.
                if (tb.isEmpty()) {
                    /*
                     * Should never happen since we iterate only on the tiles
                     * that intersect the given area of interest.
                     */
                    throw new RasterFormatException("Inconsistent tile matrix.");
                }
                final int afterLastRow = Math.addExact(tb.y, tb.height);
                tb.height = Math.max(1, Math.min(BUFFER_SIZE / (getTransferTypeSize(tile) * tb.width), tb.height));
                final int transferCapacity = tb.width * tb.height;
                if (transferCapacity > bufferCapacity) {
                    bufferCapacity = transferCapacity;
                    buffer = null;                          // Will be allocated by Raster.getDataElements(…).
                }
                while (tb.y < afterLastRow) {
                    final int height = Math.min(tb.height, afterLastRow - tb.y);
                    buffer = tile.getDataElements(tb.x, tb.y, tb.width, height, buffer);
                    raster.setDataElements(tb.x, tb.y, tb.width, height, buffer);
                    tb.y += height;
                }
            }
        }
    }

    /**
     * Returns a string representation of this image for debugging purpose.
     * This string representation may change in any future SIS version.
     */
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder(100).append(Classes.getShortClassName(this))
                .append('[').append(getWidth()).append(" × ").append(getHeight()).append(" pixels");
        final SampleModel sm = getSampleModel();
        if (sm != null) {
            buffer.append(" × ").append(sm.getNumBands()).append(" bands");
            final String type = ImageUtilities.dataTypeName(sm.getDataType());
            if (type != null) {
                buffer.append(" of type ").append(type);
            }
        }
        /*
         * Write details about color model only if there is "useful" information for a geospatial raster.
         * The main category of interest are "color palette" versus "gray scale" versus everything else,
         * and whether the image may have transparent pixels.
         */
        final ColorModel cm = getColorModel();
colors: if (cm != null) {
            buffer.append("; ");
            if (cm instanceof IndexColorModel) {
                buffer.append(((IndexColorModel) cm).getMapSize()).append(" indexed colors");
            } else {
                final ColorSpace cs = cm.getColorSpace();
                if (cs != null) {
                    if (cs instanceof ScaledColorSpace) {
                        ((ScaledColorSpace) cs).formatRange(buffer.append("showing "));
                    } else if (cs.getType() == ColorSpace.TYPE_GRAY) {
                        buffer.append("; grayscale");
                    }
                }
            }
            final String transparency;
            switch (cm.getTransparency()) {
                case ColorModel.OPAQUE:      transparency = "opaque"; break;
                case ColorModel.TRANSLUCENT: transparency = "translucent"; break;
                case ColorModel.BITMASK:     transparency = "bitmask transparency"; break;
                default: break colors;
            }
            buffer.append("; ").append(transparency);
        }
        /*
         * Tiling information last because it is usually a secondary aspect compared
         * to above information.
         */
        final int tx = getNumXTiles();
        final int ty = getNumYTiles();
        if (tx != 1 || ty != 1) {
            buffer.append("; ").append(tx).append(" × ").append(ty).append(" tiles");
        }
        return buffer.append(']').toString();
    }
}
