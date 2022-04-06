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
package org.apache.sis.test.storage;

import java.awt.Point;
import java.util.Arrays;
import java.util.Vector;
import java.awt.image.Raster;
import java.awt.image.ColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import org.apache.sis.image.PlanarImage;
import org.apache.sis.internal.util.Strings;

import static java.lang.StrictMath.floorDiv;
import static org.junit.Assert.*;


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
 * @version 1.2
 * @since   1.1
 * @module
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
        final SampleModel sourceModel = source.getSampleModel();
        if (sourceModel instanceof PixelInterleavedSampleModel) {
            final PixelInterleavedSampleModel sm = (PixelInterleavedSampleModel) sourceModel;
            final int   pixelStride    = sm.getPixelStride();
            final int   scanlineStride = sm.getScanlineStride();
            final int   offset         = pixelStride*offX + scanlineStride*offY;
            final int[] offsets        = sm.getBandOffsets();
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
            for (int i=0; i<offsets.length; i++) {
                offsets[i] += offset;
            }
            model = new PixelInterleavedSampleModel(sm.getDataType(),
                    divExclusive(sm.getWidth(),  subX),
                    divExclusive(sm.getHeight(), subY),
                    pixelStride*subX, scanlineStride*subY, offsets);
        } else if (sourceModel instanceof MultiPixelPackedSampleModel) {
            final MultiPixelPackedSampleModel sm = (MultiPixelPackedSampleModel) sourceModel;
            assertEquals("Subsampling on the X axis is not supported.", 1, subX);
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
     * This method returns {@code null} if the arguments are valid but the image can not be
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
                 * However this check rejects some valid layouts. Consider an image of size 16 × 3 pixels
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
            if (warning != null && (source instanceof PlanarImage)) {
                // Source warning may be "source.height", which we replace by "height".
                final String s = Strings.orEmpty(((PlanarImage) source).verify());
                assertEquals(s, s.substring(s.lastIndexOf('.') + 1), warning);
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
     * <div class="note"><b>Implementation note:</b>
     * in principle we should subtract the <var>subsampling offset</var>. However that offset is
     * zero in the context of {@link CoverageReadConsistency} test, because coordinates (0,0) of
     * {@linkplain #source} image is the first pixel in the Area Of Interest specified by user,
     * so there are no more offsets at this stage. Note that we are talking about offset in image
     * coordinate system, not to be confused with offset relative to the data bank
     * (given to the {@link SampleModel} at construction time).</div>
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
        return Raster.createRaster(model, tile.getDataBuffer(),
                new Point(divInclusive(tile.getMinX(), subX),
                          divInclusive(tile.getMinY(), subY)));
    }
}
