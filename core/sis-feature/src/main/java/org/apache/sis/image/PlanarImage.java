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
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.SampleModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import java.awt.image.RenderedImage;
import java.util.Vector;
import org.apache.sis.util.Classes;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.internal.coverage.j2d.ImageUtilities;
import org.apache.sis.internal.coverage.j2d.ColorModelFactory;


/**
 * Base class of {@link RenderedImage} implementations in Apache SIS.
 * The "Planar" part in the class name emphases that this image is a representation
 * of two-dimensional data and should not contain an image with three-dimensional effects.
 * Planar images can be used as data storage for {@link org.apache.sis.coverage.grid.GridCoverage2D}.
 *
 * <div class="note"><b>Note: inspirational source</b>
 * <p>This class takes some inspiration from the {@code javax.media.jai.PlanarImage}
 * class defined in the <cite>Java Advanced Imaging</cite> (<abbr>JAI</abbr>) library.
 * That excellent library was 20 years in advance on thematic like defining a chain of image operations,
 * multi-threaded execution, distribution over a computer network, <i>etc.</i>
 * But unfortunately the <abbr>JAI</abbr> library does not seems to be maintained anymore.
 * We do not try to reproduce the full set of JAI functionalities here, but we progressively
 * reproduce some little bits of functionalities as they are needed by Apache SIS.</p></div>
 *
 * <p>This base class does not store any state,
 * but assumes that numbering of pixel coordinates and tile indices start at zero.
 * Subclasses need to implement at least the following methods:</p>
 * <ul>
 *   <li>{@link #getWidth()}       — the image width in pixels.</li>
 *   <li>{@link #getHeight()}      — the image height in pixels.</li>
 *   <li>{@link #getTileWidth()}   — the tile width in pixels.</li>
 *   <li>{@link #getTileHeight()}  — the tile height in pixels.</li>
 *   <li>{@link #getTile(int,int)} — the tile at given tile indices.</li>
 * </ul>
 *
 * <p>If pixel coordinates or tile indices do not start at zero,
 * then subclasses shall also override the following methods:</p>
 * <ul>
 *   <li>{@link #getMinX()}        — the minimum <var>x</var> coordinate (inclusive) of the image.</li>
 *   <li>{@link #getMinY()}        — the minimum <var>y</var> coordinate (inclusive) of the image.</li>
 *   <li>{@link #getMinTileX()}    — the minimum tile index in the <var>x</var> direction.</li>
 *   <li>{@link #getMinTileY()}    — the minimum tile index in the <var>y</var> direction.</li>
 * </ul>
 *
 * Default implementations are provided for {@link #getNumXTiles()}, {@link #getNumYTiles()},
 * {@link #getTileGridXOffset()}, {@link #getTileGridYOffset()}, {@link #getData()},
 * {@link #getData(Rectangle)} and {@link #copyData(WritableRaster)}
 * in terms of above methods.
 *
 * <h2>Writable images</h2>
 * Some subclasses may implement the {@link WritableRenderedImage} interface. If this image is writable, then the
 * {@link WritableRenderedImage#getWritableTile getWritableTile(…)} and {@link WritableRenderedImage#releaseWritableTile
 * releaseWritableTile(…)} methods <strong>must</strong> be invoked in {@code try ... finally} block like below:
 *
 * {@preformat java
 *     WritableRenderedImage image = ...;
 *     WritableRaster tile = image.getWritableTile(tileX, tileY);
 *     try {
 *         // Do some process on the tile.
 *     } finally {
 *         image.releaseWritableTile(tileX, tileY);
 *     }
 * }
 *
 * The reason is because some implementations may acquire and release synchronization locks in the
 * {@code getWritableTile(…)} and {@code releaseWritableTile(…)} methods.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public abstract class PlanarImage implements RenderedImage {
    /**
     * Creates a new rendered image.
     */
    protected PlanarImage() {
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
     * Returns the minimum <var>x</var> coordinate (inclusive) of this image.
     *
     * <p>Default implementation returns zero.
     * Subclasses shall override this method if the image starts at another coordinate.</p>
     *
     * @return the minimum <var>x</var> coordinate (column) of this image.
     */
    @Override
    public int getMinX() {
        return 0;
    }

    /**
     * Returns the minimum <var>y</var> coordinate (inclusive) of this image.
     *
     * <p>The default implementation returns zero.
     * Subclasses shall override this method if the image starts at another coordinate.</p>
     *
     * @return the minimum <var>y</var> coordinate (row) of this image.
     */
    @Override
    public int getMinY() {
        return 0;
    }

    /**
     * Returns the minimum tile index in the <var>x</var> direction.
     *
     * <p>The default implementation returns zero.
     * Subclasses shall override this method if the tile grid starts at another index.</p>
     *
     * @return the minimum tile index in the <var>x</var> direction.
     */
    @Override
    public int getMinTileX() {
        return 0;
    }

    /**
     * Returns the minimum tile index in the <var>y</var> direction.
     *
     * <p>The default implementation returns zero.
     * Subclasses shall override this method if the tile grid starts at another index.</p>
     *
     * @return the minimum tile index in the <var>y</var> direction.
     */
    @Override
    public int getMinTileY() {
        return 0;
    }

    /**
     * Returns the number of tiles in the <var>x</var> direction.
     *
     * <p>The default implementation computes this value from {@link #getWidth()} and {@link #getTileWidth()}
     * on the assumption that {@link #getMinX()} is the coordinate of the leftmost pixels of tiles located at
     * {@link #getMinTileX()} index. This assumption can be verified by {@link #verify()}.</p>
     *
     * @return returns the number of tiles in the <var>x</var> direction.
     */
    @Override
    public int getNumXTiles() {
        /*
         * If assumption documented in javadoc does not hold, the calculation performed here would need to be
         * more complicated: compute tile index of minX, compute tile index of maxX, return difference plus 1.
         */
        return Numerics.ceilDiv(getWidth(), getTileWidth());
    }

    /**
     * Returns the number of tiles in the <var>y</var> direction.
     *
     * <p>The default implementation computes this value from {@link #getHeight()} and {@link #getTileHeight()}
     * on the assumption that {@link #getMinY()} is the coordinate of the uppermost pixels of tiles located at
     * {@link #getMinTileY()} index. This assumption can be verified by {@link #verify()}.</p>
     *
     * @return returns the number of tiles in the <var>y</var> direction.
     */
    @Override
    public int getNumYTiles() {
        return Numerics.ceilDiv(getHeight(), getTileHeight());
    }

    /**
     * Returns the <var>x</var> coordinate of the upper-left pixel of tile (0, 0).
     * That tile (0, 0) may not actually exist.
     *
     * <p>The default implementation computes this value from {@link #getMinX()},
     * {@link #getMinTileX()} and {@link #getTileWidth()}.</p>
     *
     * @return the <var>x</var> offset of the tile grid relative to the origin.
     */
    @Override
    public int getTileGridXOffset() {
        // We may have temporary `int` overflow after multiplication but exact result after addition.
        return Math.toIntExact(getMinX() - getMinTileX() * ((long) getTileWidth()));
    }

    /**
     * Returns the <var>y</var> coordinate of the upper-left pixel of tile (0, 0).
     * That tile (0, 0) may not actually exist.
     *
     * <p>The default implementation computes this value from {@link #getMinY()},
     * {@link #getMinTileY()} and {@link #getTileHeight()}.</p>
     *
     * @return the <var>y</var> offset of the tile grid relative to the origin.
     */
    @Override
    public int getTileGridYOffset() {
        return Math.toIntExact(getMinY() - getMinTileY() * ((long) getTileHeight()));
    }

    /**
     * Creates a raster with the same sample model than this image and with the given size and location.
     * This method does not verify argument validity.
     */
    private WritableRaster createWritableRaster(final Rectangle aoi) {
        SampleModel sm = getSampleModel();
        if (sm.getWidth() != aoi.width || sm.getHeight() != aoi.height) {
            sm = sm.createCompatibleSampleModel(aoi.width, aoi.height);
        }
        return Raster.createWritableRaster(sm, aoi.getLocation());
    }

    /**
     * Returns a copy of this image as one large tile.
     * The returned raster will not be updated if this image is changed.
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
     * The returned raster will not be updated if this image is changed.
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
         * Note that `tb` should never be empty since we restrict iteration to the tiles
         * that intersect the given area of interest.
         */
        Object buffer = null;
        int bufferCapacity = 0;
        for (int ty = minTileY; ty <= maxTileY; ty++) {
            for (int tx = minTileX; tx <= maxTileX; tx++) {
                final Raster tile = getTile(tx, ty);
                final Rectangle tb = aoi.intersection(tile.getBounds());        // Bounds of transfer buffer.
                final int afterLastRow = ImageUtilities.prepareTransferRegion(tb, tile.getTransferType());
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
     * Verifies whether image layout information are consistent. This method verifies that the coordinates
     * of image upper-left corner are equal to the coordinates of the upper-left corner of the tile in the
     * upper-left corner, and that image size is equal to the sum of the sizes of all tiles. Compatibility
     * of sample model and color model is also verified.
     *
     * <p>The default implementation may return the following identifiers, in that order
     * (i.e. this method returns the identifier of the first test that fail):</p>
     * <ul>
     *   <li>{@code "SampleModel"} — Sample model is incompatible with color model.</li>
     *   <li>{@code "tileWidth"}   — tile width is greater than sample model width.</li>
     *   <li>{@code "tileHeight"}  — tile height is greater than sample model height.</li>
     *   <li>{@code "numXTiles"}   — number of tiles on the X axis is inconsistent with image width.</li>
     *   <li>{@code "numYTiles"}   — number of tiles on the Y axis is inconsistent with image height.</li>
     *   <li>{@code "tileX"}       — {@ode minTileX} and/or {@code tileGridXOffset} is inconsistent.</li>
     *   <li>{@code "tileY"}       — {@ode minTileY} and/or {@code tileGridYOffset} is inconsistent.</li>
     * </ul>
     *
     * Subclasses may perform additional checks. For example some subclasses also check specifically
     * for {@code "minX"}, {@code "minY"}, {@code "tileGridXOffset"} and {@code "tileGridYOffset"}.
     *
     * @return {@code null} if image layout information are consistent,
     *         or the name of inconsistent attribute if a problem is found.
     */
    public String verify() {
        final int tileWidth  = getTileWidth();
        final int tileHeight = getTileHeight();
        final SampleModel sm = getSampleModel();
        if (sm != null) {
            final ColorModel cm = getColorModel();
            if (cm != null) {
                if (!cm.isCompatibleSampleModel(sm)) return "SampleModel";
            }
            /*
             * The SampleModel size represents the physical layout of pixels in the data buffer,
             * while the Raster may be a virtual view over a sub-region of a parent raster.
             */
            if (sm.getWidth()  < tileWidth)  return "tileWidth";
            if (sm.getHeight() < tileHeight) return "tileHeight";
        }
        if (((long) getNumXTiles()) * tileWidth  != getWidth())  return "numXTiles";
        if (((long) getNumYTiles()) * tileHeight != getHeight()) return "numYTiles";
        if (((long) getMinTileX())  * tileWidth  + getTileGridXOffset() != getMinX()) return "tileX";
        if (((long) getMinTileY())  * tileHeight + getTileGridYOffset() != getMinY()) return "tileY";
        return null;
    }

    /**
     * Returns a string representation of this image for debugging purpose.
     * This string representation may change in any future SIS version.
     *
     * @return a string representation of this image for debugging purpose only.
     */
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder(100).append(Classes.getShortClassName(this))
                .append('[').append(getWidth()).append(" × ").append(getHeight()).append(" pixels");
        final SampleModel sm = getSampleModel();
        if (sm != null) {
            buffer.append(" × ").append(sm.getNumBands()).append(" bands");
            final String type = ImageUtilities.getDataTypeName(sm);
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
                ColorModelFactory.formatDescription(cm.getColorSpace(), buffer);
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
         * Tiling information last because it is usually a secondary aspect compared to above information.
         * If a warning is emitted, it will usually be a tiling problem so it is useful to keep it close.
         */
        final int tx = getNumXTiles();
        final int ty = getNumYTiles();
        if (tx != 1 || ty != 1) {
            buffer.append("; ").append(tx).append(" × ").append(ty).append(" tiles");
        }
        final String error = verify();
        if (error != null) {
            buffer.append("; ⚠ mismatched ").append(error);
        }
        return buffer.append(']').toString();
    }
}
