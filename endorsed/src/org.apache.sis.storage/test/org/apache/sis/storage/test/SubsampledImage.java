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
package org.apache.sis.storage.test;

import java.awt.Point;
import java.util.Arrays;
import java.util.Vector;
import java.util.Objects;
import java.awt.image.Raster;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import static java.lang.StrictMath.floorDiv;
import org.apache.sis.image.PlanarImage;
import org.apache.sis.util.internal.shared.Strings;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;


/**
 * An image which is a subsampling of another image.
 * This image is a view; sample values are not copied.
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>Sample model must be an instance of {@link PixelInterleavedSampleModel} or {@link MultiPixelPackedSampleModel}.</li>
 *   <li>Subsampling must be a divisor of tile size, except in dimensions having only one tile.</li>
 *   <li>Conversion from source coordinates to target coordinates is a division only, without subsampling offsets.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class SubsampledImage extends PlanarImage {
    /**
     * The image at full resolution.
     */
    private final RenderedImage source;

    /**
     * The subsampling to apply.
     */
    private final int subX, subY;

    /**
     * The offset to apply after subsampling.
     */
    private final int offX, offY;

    /**
     * The subsampled model.
     */
    private final SampleModel model;

    /**
     * Creates a new image as a subsampling of the given image.
     * The offsets given to this constructor apply to the {@link java.awt.image.DataBuffer},
     * <strong>not</strong> to the image coordinate system. An offset needs to be added for
     * accesses to the data buffers because those buffers are shared with the source image,
     * and have a fixed origin regardless the origin of image coordinate systems.
     *
     * Current version does not take a subsampling offset for the image coordinate system
     * because in the context of {@link CoverageReadConsistency} test, coordinates (0,0) of
     * {@linkplain #source} image is the first pixel in the Area Of Interest specified by user,
     * so the subsampling offsets on image coordinates are already applied at this stage.
     *
     * @param  source  the image at full resolution.
     * @param  subX    the subsampling factor along X axis.
     * @param  subY    the subsampling factor along Y axis.
     * @param  offX    the offset along X axis in the shared data buffer.
     * @param  offY    the offset along Y axis in the shared data buffer.
     */
    private SubsampledImage(final RenderedImage source, final int subX, final int subY, final int offX, final int offY) {
        this.source = source;
        this.subX   = subX;
        this.subY   = subY;
        this.offX   = offX;
        this.offY   = offY;
        final SampleModel sourceModel = source.getSampleModel();
        if (sourceModel instanceof PixelInterleavedSampleModel sm) {
            final int   pixelStride    = sm.getPixelStride();
            final int   scanlineStride = sm.getScanlineStride();
            final int   strideOffset   = pixelStride*offX + scanlineStride*offY;
            final int[] bandOffsets    = sm.getBandOffsets();
            /*
             * Conversion from subsampled coordinate x′ to full resolution x is:
             *
             *    x = (x′ × subsampling) + offset
             *
             * We simulate the offset addition by adding the value in the offset bands.
             * PixelInterleavedSampleModel uses that value for computing array index as below:
             *
             *    y*scanlineStride + x*pixelStride + bandOffsets[b]
             */
            for (int i=0; i<bandOffsets.length; i++) {
                bandOffsets[i] += strideOffset;
            }
            model = new PixelInterleavedSampleModel(sm.getDataType(),
                    divExclusive(sm.getWidth(),  subX),
                    divExclusive(sm.getHeight(), subY),
                    pixelStride*subX, scanlineStride*subY, bandOffsets);
        } else if (sourceModel instanceof MultiPixelPackedSampleModel sm) {
            assertEquals(1, subX, "Subsampling on the X axis is not supported.");
            model = new MultiPixelPackedSampleModel(sm.getDataType(),
                    divExclusive(sm.getWidth(),  subX),
                    divExclusive(sm.getHeight(), subY),
                    sm.getPixelBitStride(),
                    sm.getScanlineStride() * subY,
                    sm.getDataBitOffset());
        } else {
            throw new AssertionError("Unsupported sample model: " + sourceModel);
        }
        /*
         * Conditions documented in class javadoc.
         */
        if (getNumXTiles() > 1) assertEquals(0, sourceModel.getWidth()  % subX);
        if (getNumYTiles() > 1) assertEquals(0, sourceModel.getHeight() % subY);
    }

    /**
     * Returns an image as a subsampling of the given image.
     * This method returns {@code null} if the arguments are valid but the image cannot be
     * created because of a restriction in {@link PixelInterleavedSampleModel} constructor.
     *
     * @param  source  the image at full resolution.
     * @param  subX    the subsampling factor along X axis.
     * @param  subY    the subsampling factor along Y axis.
     * @param  offX    the subsampling offset along X axis.
     * @param  offY    the subsampling offset along Y axis.
     */
    static RenderedImage create(final RenderedImage source, final int subX, final int subY, final int offX, final int offY) {
        if (subX == 1 && subY == 1) {
            return source;
        } else {
            final SubsampledImage image;
            try {
                image = new SubsampledImage(source, subX, subY, offX, offY);
            } catch (IllegalArgumentException e) {
                /*
                 * PixelInterleavedSampleModel constructor has the following argument check:
                 *
                 *     if (pixelStride * width > scanlineStride) {
                 *         throw new IllegalArgumentException("Pixel stride times width must be less than or equal to the scanline stride");
                 *     }
                 *
                 * However, this check rejects some valid layouts. Consider an image of size 16 × 3 pixels
                 * with a single band and subsamplig factors (5,1). In the illustration below, "X" and "-"
                 * are pixels from the source images and "X" are pixels retained in the subsampled image.
                 * Note that the last column of the source image is included in the subsampled image.
                 *
                 *     X----X----X----X
                 *     X----X----X----X
                 *     X----X----X----X
                 *
                 * With a a pixelStride = 5, a width = 4 and scanlineStride = 16 we get!
                 *
                 *     pixelStride*w > scanlineStride
                 *               5*4 > 16
                 *                20 > 16           →  true  →  IllegalArgumentException is thrown.
                 *
                 * Above condition is equivalent to requiring image to be
                 *
                 *     like that:   X----X----X----X----
                 *     instead of:  X----X----X----X
                 *
                 * The amended check below checks if there is enough room for storing the last sample value of a row,
                 * ignoring the remaining of pixel stride that are just skipped.
                 *
                 *     pixelStride*(w-1) + maxBandOff >= scanlineStride
                 *               5*(4-1) + 0          >= 16
                 *                   15               >= 16     →  false  →  no exception thrown.
                 */
                final PixelInterleavedSampleModel sm = (PixelInterleavedSampleModel) source.getSampleModel();
                final int pixelStride    = sm.getPixelStride() * subX;
                final int scanlineStride = sm.getScanlineStride() * subY;
                final int width = divExclusive(sm.getWidth(), subX);
                if (pixelStride * width > scanlineStride) {
                    final int minBandOff = Arrays.stream(sm.getBandOffsets()).min().getAsInt();
                    final int maxBandOff = Arrays.stream(sm.getBandOffsets()).max().getAsInt();
                    if (pixelStride * (width - 1) + (maxBandOff - minBandOff) < scanlineStride) {
                        return null;
                    }
                }
                throw e;
            }
            final String warning = image.verify();
            if (warning != null && (source instanceof PlanarImage planar)) {
                // Source warning may be "source.height", which we replace by "height".
                final String s = Strings.orEmpty(planar.verify());
                assertEquals(s.substring(s.lastIndexOf('.') + 1), warning, s);
            }
            return image;
        }
    }

    /**
     * Returns the image at full resolution.
     */
    @Override
    @SuppressWarnings("UseOfObsoleteCollectionType")
    public Vector<RenderedImage> getSources() {
        final Vector<RenderedImage> sources = new Vector<>(1);
        sources.add(source);
        return sources;
    }

    @Override public SampleModel getSampleModel() {return model;}
    @Override public ColorModel  getColorModel()  {return source.getColorModel();}
    @Override public int         getNumXTiles()   {return source.getNumXTiles();}
    @Override public int         getNumYTiles()   {return source.getNumYTiles();}
    @Override public int         getMinTileX()    {return source.getMinTileX();}
    @Override public int         getMinTileY()    {return source.getMinTileY();}
    @Override public int         getTileWidth()   {return divExclusive(source.getTileWidth(),  subX);}
    @Override public int         getTileHeight()  {return divExclusive(source.getTileHeight(), subY);}
    @Override public int         getWidth()       {return divExclusive(source.getWidth(),  subX);}
    @Override public int         getHeight()      {return divExclusive(source.getHeight(), subY);}
    @Override public int         getMinX()        {return divInclusive(source.getMinX(), subX);}
    @Override public int         getMinY()        {return divInclusive(source.getMinY(), subY);}

    /**
     * Computes {@code (coordinate - offset) / subsampling} rounded toward 0.
     * The subsampling offset is assumed 0 in current version.
     *
     * <h4>Implementation note</h4>
     * in principle we should subtract the <var>subsampling offset</var>. However, that offset is
     * zero in the context of {@link CoverageReadConsistency} test, because coordinates (0,0) of
     * {@linkplain #source} image is the first pixel in the Area Of Interest specified by user,
     * so there are no more offsets at this stage. Note that we are talking about offset in image
     * coordinate system, not to be confused with offset relative to the data bank
     * (given to the {@link SampleModel} at construction time).
     */
    private static int divInclusive(final int coordinate, final int subsampling) {
        return floorDiv(coordinate, subsampling);
    }

    /**
     * Computes {@code (coordinate - offset) / subsampling}, but handling the given {@code coordinate} as exclusive.
     * The subsampling offset is assumed 0 in current version (see {@link #divInclusive(int, int)} for explanation).
     */
    private static int divExclusive(final int coordinate, final int subsampling) {
        return floorDiv(coordinate - 1, subsampling) + 1;
    }

    /**
     * Returns the tile at the given index.
     */
    @Override
    public Raster getTile(final int tileX, final int tileY) {
        final Raster tile = source.getTile(tileX, tileY);
        final int x       = divInclusive(tile.getMinX(),   subX);
        final int y       = divInclusive(tile.getMinY(),   subY);
        final int width   = divExclusive(tile.getWidth(),  subX);
        final int height  = divExclusive(tile.getHeight(), subY);
        int tx = tile.getMinX() - tile.getSampleModelTranslateX();
        int ty = tile.getMinY() - tile.getSampleModelTranslateY();
        if ((tx % subX) != 0 || (ty % subY) != 0) {
            /*
             * We cannot create a view over the existing raster if `tx` and `ty` are not a divisor
             * of `subX` and `subY` respectively. In such case, as a workaround we create a copy.
             * Note that this is rarely needed because `tx` and `ty` are usually zero.
             */
            return rewriteTile(tile, tile.createCompatibleWritableRaster(x, y, width, height));
        }
        /*
         * We need to create the raster from scratch in order to specify our custom sample model.
         * The static `createRaster(SampleModel, DataBuffer, Point)` method makes two assumptions:
         *
         *   - Raster size is equal to sample model size.
         *   - The first pixel is located at the beginning of the data buffer.
         *     This is implied by `Raster.sampleModelTranslateX|Y` equal to `Raster.minX|Y`.
         *
         * Above conditions are usually meet in Apache SIS implementations of `DataStore`.
         * But they are not meet if the source tile was itself a view over sub-region of another tile
         * for example as produced by `BufferedImage.getSubimage(…)`, It happens when using Image I/O.
         * In such cases `tile.sampleModelTranslateX|Y` may have values different than `tile.minX|Y`.
         *
         * As of Java 17 there is no factory method for creating a raster having a different size
         * or different `sampleModelTranslate`, but we can specify those values afterward by call
         * to `parent.createChild(…)`. The latter method computes translation as below:
         *
         *     child.sampleModelTranslateX = parent.sampleModelTranslateX + childMinX - parentX
         *     (idem for Y)
         *
         * Given the following:
         *
         *     - parent.sampleModelTranslateX = parent.minX = x     by `Raster.createRaster(…) construction
         *     - childMinX = x                                      by `this.getTile(…)` method contract
         *     - parentX   = x + Δx
         *       Δx        ≈ (tile.minX - tile.sampleModelTranslateX) / subX
         *
         * We get:
         *
         *     child.sampleModelTranslateX = x + x - parentX   =   x - Δx
         *         = x - tile.minX/subX + tile.sampleModelTranslateX/subX
         *         ≈ tile.sampleModelTranslateX / subX
         *
         * The last term is the desired value. The "almost equal" is because of quotient rounding
         * if values in `tile` are not divisible by the subsampling.
         */
        Raster subsampled = Raster.createRaster(model, tile.getDataBuffer(), new Point(x, y));
        if ((tx | ty) != 0 || subsampled.getWidth() != width || subsampled.getHeight() != height) {
            tx = x + divInclusive(tx, subX);
            ty = y + divInclusive(ty, subY);
            subsampled = subsampled.createChild(tx, ty, width, height, x, y, null);
        }
        assertEquals(x, subsampled.getMinX());
        assertEquals(y, subsampled.getMinY());
        return subsampled;
    }

    /**
     * Invoked when we cannot create a subsampled tile as a view of the original tile.
     * This method rewrites fully the tile by copying sample values in a new data buffer.
     *
     * @param  tile    the tile to rewrite.
     * @param  target  an initially empty tile with the expected location and size.
     * @return the {@code target} tile with rewritten values.
     */
    private Raster rewriteTile(final Raster tile, final WritableRaster target) {
        final int width  = target.getWidth();
        final int height = target.getHeight();
        final int xmin   = target.getMinX();
        final int ymin   = target.getMinY();
        final int xs     = tile.getMinX() + offX;
        final int ys     = tile.getMinY() + offY;
        double[] buffer = null;
        for (int y=0; y<height; y++) {
            for (int x=0; x<width; x++) {
                buffer = tile.getPixel(x*subX + xs, y*subY + ys, buffer);
                target.setPixel(x + xmin, y + ymin, buffer);
            }
        }
        return target;
    }

    /**
     * Returns a hash code value for this image.
     */
    @Override
    public int hashCode() {
        return Objects.hash(source, subX, subY, offX, offY);
    }

    /**
     * Compares the given object with this image for equality.
     */
    @Override
    public boolean equals(final Object object) {
        if (object instanceof SubsampledImage other) {
            return source.equals(other.source) &&
                   subX == other.subX &&
                   subY == other.subY &&
                   offX == other.offX &&
                   offY == other.offY;
        }
        return false;
    }
}
