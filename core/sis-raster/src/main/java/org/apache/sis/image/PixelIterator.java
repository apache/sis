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

import java.awt.Rectangle;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.BandedSampleModel;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import org.opengis.coverage.grid.SequenceType;
import org.apache.sis.util.ArgumentChecks;


/**
 * An iterator over sample values in a raster or an image. This iterator simplifies accesses to pixel or sample values
 * by hiding {@linkplain SampleModel sample model} and tiling complexity. Iteration may be performed on full image or
 * on image sub-region. Iteration order is implementation specific.
 *
 * @author  Rémi Marechal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public abstract class PixelIterator {
    /**
     * The image in which iteration is occurring, or {@code null} if none.
     * If {@code null}, then {@link #currentRaster} must be non-null.
     */
    final RenderedImage image;

    /**
     * The current raster in which iteration is occurring. This may change when the iterator
     * reaches a new {@link #image} tile. May be {@code null} if not yet determined.
     */
    Raster currentRaster;

    /**
     * The sample model for all tiles in the {@linkplain #image}.
     * The {@link #currentRaster} shall always have this sample model.
     */
    private final SampleModel sampleModel;

    /**
     * Number of bands in all tiles in the {@linkplain #image}.
     * The {@link #currentRaster} shall always have this number of bands.
     */
    final int numBands;

    /**
     * Coordinates of upper-left corner in the complete image or raster.
     */
    private final int xmin, ymin;

    /**
     * Size of all tiles in the {@link #image}.
     * The {@link #currentRaster} shall always have this exact size.
     */
    final int tileWidth, tileHeight;

    /**
     * Domain, in pixel coordinates, of the region traversed by this pixel iterator.
     *
     * @see #getDomain()
     */
    final RectIter domain;

    /**
     * Domain, in tile coordinates, of the region traversed by this pixel iterator.
     */
    final RectIter timeDomain;

    /**
     * Current band position in current raster.
     */
    int band;

    /**
     * Coordinates of lower-right corner of current raster.
     * When iteration reaches this coordinates, the iterator needs to move to next tile.
     */
    int currentRasterMaxX, currentRasterMaxY;

    /**
     * Current <var>x</var> or <var>y</var> coordinates of current tile.
     */
    int tileX, tileY;

    /**
     * Creates an iterator for the given region in the given raster.
     *
     * @param  data     the raster which contains the sample values on which to iterate.
     * @param  subArea  the raster region where to perform the iteration, or {@code null}
     *                  for iterating over all the raster domain.
     */
    PixelIterator(final Raster data, final Rectangle subArea) {
        ArgumentChecks.ensureNonNull("data", data);
        final Rectangle bounds;
        image         = null;
        currentRaster = data;
        sampleModel   = data.getSampleModel();
        xmin          = data.getMinX();
        ymin          = data.getMinY();
        tileWidth     = data.getWidth();
        tileHeight    = data.getHeight();
        bounds        = new Rectangle(xmin, ymin, tileWidth, tileHeight);
        domain        = new RectIter(subArea != null ? bounds.intersection(subArea) : bounds);
        timeDomain    = new RectIter(0, 0, 1, 1);  // In this case only one raster: tile index = 0 … 1
        numBands      = data.getNumBands();
        band          = -1;
    }

    /**
     * Creates an iterator for the given region in the given image.
     *
     * @param  data     the image which contains the sample values on which to iterate.
     * @param  subArea  the image region where to perform the iteration, or {@code null}
     *                  for iterating over all the image domain.
     */
    PixelIterator(final RenderedImage data, final Rectangle subArea) {
        ArgumentChecks.ensureNonNull("data", data);
        final Rectangle bounds;
        image         = data;
        sampleModel   = data.getSampleModel();
        xmin          = data.getMinX();
        ymin          = data.getMinY();
        tileWidth     = data.getTileWidth();
        tileHeight    = data.getTileHeight();
        bounds        = new Rectangle(xmin, ymin, data.getWidth(), data.getHeight());
        domain        = new RectIter(subArea != null ? bounds.intersection(subArea) : bounds);
        {
            final int offTX = data.getTileGridXOffset();
            final int offTY = data.getTileGridYOffset();
            final int tMinX = (domain.minx - xmin) / tileWidth  + offTX;
            final int tMinY = (domain.miny - ymin) / tileHeight + offTY;
            final int tMaxX = (domain.maxX - xmin + tileWidth  - 1) / tileWidth  + offTX;
            final int tMaxY = (domain.maxY - ymin + tileHeight - 1) / tileHeight + offTY;
            timeDomain = new RectIter(tMinX, tMinY, tMaxX - tMinX, tMaxY - tMinY);
        }
        numBands = sampleModel.getNumBands();
        band = -1;
    }

    /**
     * Returns true if the iteration has more pixel(in other words if {@linkplain #next()} is possible)
     * and move forward iterator.
     *
     * @return true if next value exist else false.
     * @throws IllegalStateException if you call again this method when you have
     *         already reach the end of the iteration.
     */
    public abstract boolean next();

    /**
     * Returns next X iterator coordinate without move forward it.
     * User must call next() method before getX() method.
     *
     * @return X iterator position.
     */
    public abstract int getX();

    /**
     * Returns next Y iterator coordinate without move forward it.
     * User must call next() method before getY() method.
     *
     * @return Y iterator position.
     */
    public abstract int getY();

    /**
     * Returns the next integer value from iteration.
     *
     * @return the next integer value.
     */
    public abstract int getSample();

    /**
     * Returns the next float value from iteration.
     *
     * @return the next float value.
     */
    public abstract float getSampleFloat();

    /**
     * Returns the next double value from iteration.
     *
     * @return the next double value.
     */
    public abstract double getSampleDouble();

    /**
     * Return type of sequence iteration direction.
     *
     * @return type of sequence.
     */
    public abstract SequenceType getIterationDirection();

    /**
     * <p>Move forward iterator cursor at x, y coordinates. Cursor is automatically
     * positioned at band index.<br>
     *
     * Code example :<br>
     * {@code PixelIterator.moveTo(x, y, b);}<br>
     *
     * {@code       do} {<br>
     * {@code           PixelIterator.getSample();//for example}<br>
     *        } {@code while (PixelIterator.next());}<br>
     *
     * MoveTo method is configure to use do...while() loop after moveTo call.</p>
     *
     * @param x the x coordinate cursor position.
     * @param y the y coordinate cursor position.
     * @param b the band index cursor position.
     * @throws IllegalArgumentException if coordinates are out of iteration area boundary.
     */
    public void moveTo(int x, int y, int b){
        if (x < domain.minx || x >= domain.maxX ||  y < domain.miny || y >= domain.maxY) {
            throw new IllegalArgumentException("coordinate out of iteration area define by: (" + domain + ")\n "
                    + "given coordinates are: " + x + " " + y);
        }
        if (b < 0 || b >= numBands) {
            throw new IllegalArgumentException("band index out of numband border define by: [0;" + numBands + "]");
        }
    }

    /**
     * Returns the number of bands (samples per pixel) from Image or Raster within this Iterator.
     *
     * @return the number of bands (samples per pixel) from current raster or Image.
     */
    public int getNumBands() {
        return numBands;
    }

    /**
     * Returns the pixel coordinates of the area in which this iterator is doing the iteration.
     *
     * @return pixel coordinates of the iteration area.
     */
    public Rectangle getDomain() {
        return domain.toRectangle();
    }

    //-- TODO : methodes suivantes a refactorer (code duplication) + mettre ailleur + static + package private
    /**
     * Check that the two input rasters are compatible for coupling in a {@link WritablePixelIterator}
     */
    static void checkRasters(final Raster readableRaster, final WritableRaster writableRaster){
        //raster dimension
        if (readableRaster.getMinX()     != writableRaster.getMinX()
         || readableRaster.getMinY()     != writableRaster.getMinY()
         || readableRaster.getWidth()    != writableRaster.getWidth()
         || readableRaster.getHeight()   != writableRaster.getHeight()
         || readableRaster.getNumBands() != writableRaster.getNumBands())
         throw new IllegalArgumentException("raster and writable raster are not in same dimension"+readableRaster+writableRaster);
        //raster data type
        if (readableRaster.getDataBuffer().getDataType() != writableRaster.getDataBuffer().getDataType())
            throw new IllegalArgumentException("raster and writable raster haven't got same datas type");
    }

    /**
     * Verify Rendered image conformity.
     */
    static void checkRenderedImage(final RenderedImage renderedImage, final WritableRenderedImage writableRI) {
        //image dimensions
        if (renderedImage.getMinX()   != writableRI.getMinX()
         || renderedImage.getMinY()   != writableRI.getMinY()
         || renderedImage.getWidth()  != writableRI.getWidth()
         || renderedImage.getHeight() != writableRI.getHeight()
         || renderedImage.getSampleModel().getNumBands() != writableRI.getSampleModel().getNumBands())
         throw new IllegalArgumentException("rendered image and writable rendered image dimensions are not conform.\n" +
                 "First : "+renderedImage+"\nSecond : "+writableRI);
        final int wrimtx = writableRI.getMinTileX();
        final int wrimty = writableRI.getMinTileY();
        final int rimtx  = writableRI.getMinTileX();
        final int rimty  = writableRI.getMinTileY();
        //tiles dimensions
        if (rimtx != wrimtx
         || rimty != wrimty
         || renderedImage.getNumXTiles() != writableRI.getNumXTiles()
         || renderedImage.getNumYTiles() != writableRI.getNumYTiles()
         || renderedImage.getTileGridXOffset() != writableRI.getTileGridXOffset()
         || renderedImage.getTileGridYOffset() != writableRI.getTileGridYOffset()
         || renderedImage.getTileHeight() != writableRI.getTileHeight()
         || renderedImage.getTileWidth()  != writableRI.getTileWidth())
            throw new IllegalArgumentException("rendered image and writable rendered image tiles configuration are not conform.\n" +
                    "First : "+renderedImage+"\nSecond : "+writableRI);
        //data type
        // TODO : Should be required only for Direct iterators (working directly with data buffers)
        if (renderedImage.getTile(rimtx, rimty).getDataBuffer().getDataType() != writableRI.getTile(wrimtx, wrimty).getDataBuffer().getDataType())
            throw new IllegalArgumentException("rendered image and writable rendered image haven't got same datas type");

    }

    /**
     * Verify raster conformity.
     */
    final void checkRasters(final Raster readableRaster, final WritableRaster writableRaster, final Rectangle subArea) {
        final int wRmx = writableRaster.getMinX();
        final int wRmy = writableRaster.getMinY();
        final int wRw  = writableRaster.getWidth();
        final int wRh  = writableRaster.getHeight();
        if ((wRmx != domain.minx)
          || wRmy != domain.miny
          || wRw  != domain.width
          || wRh  != domain.height)

        //raster dimension
        if ((readableRaster.getMinX()   != wRmx)
          || readableRaster.getMinY()   != wRmy
          || readableRaster.getWidth()  != wRw
          || readableRaster.getHeight() != wRh)
         throw new IllegalArgumentException("raster and writable raster are not in same dimension"+readableRaster+writableRaster);

        if (readableRaster.getNumBands() != writableRaster.getNumBands())
            throw new IllegalArgumentException("raster and writable raster haven't got same band number");
        //raster data type
        if (readableRaster.getDataBuffer().getDataType() != writableRaster.getDataBuffer().getDataType())
            throw new IllegalArgumentException("raster and writable raster haven't got same datas type");
    }

    /**
     * Verify Rendered image conformity.
     */
    final void checkRenderedImage(final RenderedImage renderedImage, final WritableRenderedImage writableRI, final Rectangle subArea) {
        if (renderedImage.getSampleModel().getNumBands() != writableRI.getSampleModel().getNumBands())
            throw new IllegalArgumentException("renderedImage and writableRenderedImage haven't got same band number");
        final int riMinX   = renderedImage.getMinX();
        final int riMinY   = renderedImage.getMinY();
        final int riTileWidth = renderedImage.getTileWidth();
        final int riTileHeight = renderedImage.getTileHeight();
        final int rimtx  = renderedImage.getMinTileX();
        final int rimty  = renderedImage.getMinTileY();

        final int wrimtx = writableRI.getMinTileX();
        final int wrimty = writableRI.getMinTileY();

        //data type
        if (renderedImage.getTile(rimtx, rimty).getDataBuffer().getDataType() != writableRI.getTile(wrimtx, wrimty).getDataBuffer().getDataType())
            throw new IllegalArgumentException("rendered image and writable rendered image haven't got same datas type");

        //tiles dimensions
        if (renderedImage.getTileHeight() != writableRI.getTileHeight()
         || renderedImage.getTileWidth()  != writableRI.getTileWidth()
         || renderedImage.getTileGridXOffset() != writableRI.getTileGridXOffset()
         || renderedImage.getTileGridYOffset() != writableRI.getTileGridYOffset())
            throw new IllegalArgumentException("rendered image and writable rendered image tiles configuration are not conform"+renderedImage+writableRI);

        //verifier les index de tuiles au depart
        final boolean minTileX = (wrimtx == (domain.minx - riMinX)/ riTileWidth  + rimtx);
        final boolean minTileY = (wrimty == (domain.miny - riMinY)/ riTileHeight + rimty);

        //writable image correspond with iteration area
        if (writableRI.getMinX()  != domain.minx    //areaiteration
         || writableRI.getMinY()  != domain.miny    //areaiteration
         || writableRI.getWidth() != domain.width//longueuriteration
         || writableRI.getHeight()!= domain.height//largeuriteration
         || !minTileX || !minTileY )

        //image dimensions
        if (renderedImage.getMinX()   != writableRI.getMinX()
         || renderedImage.getMinY()   != writableRI.getMinY()
         || renderedImage.getWidth()  != writableRI.getWidth()
         || renderedImage.getHeight() != writableRI.getHeight()
         || rimtx != wrimtx || rimty != wrimty
         || renderedImage.getNumXTiles() != writableRI.getNumXTiles()
         || renderedImage.getNumYTiles() != writableRI.getNumYTiles())
         throw new IllegalArgumentException("rendered image and writable rendered image dimensions are not conform"+renderedImage+writableRI);
    }

    /**
     * Fill given buffer with samples within the given area at the specified image band.
     * Adapted for a {@link PixelInterleavedSampleModel} {@link SampleModel} type.
     *
     * @param area define needed samples area.
     * @param buffer array which will be filled by samples.
     * @param band the interest band.
     */
    private void getAreaByInterleaved(final Rectangle area, final Object buffer, final int band) {
        rewind();
        final int sourceDataType = getSourceDatatype();

        final int minTX, minTY, maxTX, maxTY;

        if (image != null) {
            minTX = timeDomain.minx + (area.x - xmin) / tileWidth;
            minTY = timeDomain.miny + (area.y - ymin) / tileHeight;
            maxTX = timeDomain.minx + (area.x + area.width + tileWidth - 1) / tileWidth;
            maxTY = timeDomain.miny + (area.y + area.height + tileHeight - 1) / tileHeight;
        } else {
            minTX = minTY = 0;
            maxTX = maxTY = 1;
        }

        for (int ty = minTY; ty < maxTY; ty++) {
            for (int tx = minTX; tx < maxTX; tx++) {

                //-- intersection sur
                final Raster rast = (image != null) ? image.getTile(tx, ty) : currentRaster;
                final int minX = Math.max(rast.getMinX(), area.x);
                final int minY = Math.max(rast.getMinY(), area.y);
                final int maxX = Math.min(rast.getMinX() + tileWidth, area.x + area.width);
                final int maxY = Math.min(rast.getMinY() + tileHeight, area.y + area.height);
                if (minX > maxX || minY > maxY) throw new IllegalArgumentException("Expected area don't intersect internal data.");

                final int readLength = (maxX - minX);
                int destId = 0;
                for (int y = minY; y < maxY; y++) {
                    moveTo(minX, y, band);
                    int s = 0;
                    int id = destId;
                    while (s < readLength) {

                        switch (sourceDataType) {
                            case DataBuffer.TYPE_BYTE : {
                                ((byte[]) buffer)[id++] = (byte) getSample();
                                break;
                            }
                            case DataBuffer.TYPE_USHORT :
                            case DataBuffer.TYPE_SHORT  : {
                                ((short[]) buffer)[id++] = (short) getSample();
                                break;
                            }
                            case DataBuffer.TYPE_INT : {
                                ((int[]) buffer)[id++] = getSample();
                                break;
                            }
                            case DataBuffer.TYPE_FLOAT : {
                                ((float[]) buffer)[id++] = getSampleFloat();
                                break;
                            }
                            case DataBuffer.TYPE_DOUBLE : {
                                ((double[]) buffer)[id++] = getSampleDouble();
                                break;
                            }
                            default : {
                                throw new IllegalStateException("Unknow datatype.");
                            }
                        }
                        int b = 0;
                        while (next()) {
                            if (++b == getNumBands()) break;
                        }
                        s++;
                    }
                    destId    += area.width;
                }
            }
        }
    }


    /**
     * Fill given buffer with samples within the given area and from all image band.
     * Adapted for a {@link PixelInterleavedSampleModel} {@link SampleModel} type.
     *
     * @param area define needed samples area.
     * @param buffer array which will be filled by samples.
     */
    private void getAreaByInterleaved(final Rectangle area, final Object[] buffer) {
        rewind();
        final int sourceDataType = getSourceDatatype();

        final int minTX, minTY, maxTX, maxTY;

        if (image != null) {
            minTX = timeDomain.minx + (area.x - xmin) / tileWidth;
            minTY = timeDomain.miny + (area.y - ymin) / tileHeight;
            maxTX = timeDomain.minx + (area.x + area.width + tileWidth - 1) / tileWidth;
            maxTY = timeDomain.miny + (area.y + area.height + tileHeight - 1) / tileHeight;
        } else {
            minTX = minTY = 0;
            maxTX = maxTY = 1;
        }

        for (int ty = minTY; ty < maxTY; ty++) {
            for (int tx = minTX; tx < maxTX; tx++) {

                //-- intersection sur
                final Raster rast = (image != null) ? image.getTile(tx, ty) : currentRaster;
                final int minX = Math.max(rast.getMinX(), area.x);
                final int minY = Math.max(rast.getMinY(), area.y);
                final int maxX = Math.min(rast.getMinX() + tileWidth, area.x + area.width);
                final int maxY = Math.min(rast.getMinY() + tileHeight, area.y + area.height);
                if (minX > maxX || minY > maxY) throw new IllegalArgumentException("Expected area don't intersect internal data.");

                final int readLength = (maxX - minX);
                int destId = 0;
                for (int y = minY; y < maxY; y++) {
                    moveTo(minX, y, 0);
                    int s = 0;
                    int id = destId;
                    while (s < readLength) {
                        int b = 0;
                        while (b < getNumBands()) {
                            switch (sourceDataType) {
                                case DataBuffer.TYPE_BYTE : {
                                    ((byte[]) buffer[b])[id++] = (byte) getSample();
                                    break;
                                }
                                case DataBuffer.TYPE_USHORT :
                                case DataBuffer.TYPE_SHORT  : {
                                    ((short[]) buffer[b])[id++] = (short) getSample();
                                    break;
                                }
                                case DataBuffer.TYPE_INT : {
                                    ((int[]) buffer[b])[id++] = getSample();
                                    break;
                                }
                                case DataBuffer.TYPE_FLOAT : {
                                    ((float[]) buffer[b])[id++] = getSampleFloat();
                                    break;
                                }
                                case DataBuffer.TYPE_DOUBLE : {
                                    ((double[]) buffer[b])[id++] = getSampleDouble();
                                    break;
                                }
                                default : {
                                    throw new IllegalStateException("Unknow datatype.");
                                }
                            }
                            b++;
                            next();
                        }
                        s++;
                    }
                    destId    += area.width;
                }
            }
        }
    }

    /**
     * Fill given buffer with samples within the given area and from all image band.
     * Adapted for a {@link BandedSampleModel} {@link SampleModel} type.
     *
     * @param area define needed samples area.
     * @param buffer array which will be filled by samples.
     */
    private void getAreaByBanded (final Rectangle area, final Object[] buffer) {

        final ComponentSampleModel compSM = (ComponentSampleModel) sampleModel;
        final int[] bankIndices = compSM.getBankIndices();
        assert bankIndices.length == getNumBands();
        final int[] bandOffsets = compSM.getBandOffsets();
        assert bandOffsets.length == getNumBands();

        final int sourceDataType = getSourceDatatype();

        final int minTX, minTY, maxTX, maxTY;

        if (image != null) {
            minTX = timeDomain.minx + (area.x - xmin) / tileWidth;
            minTY = timeDomain.miny + (area.y - ymin) / tileHeight;
            maxTX = timeDomain.minx + (area.x + area.width + tileWidth - 1) / tileWidth;
            maxTY = timeDomain.miny + (area.y + area.height + tileHeight - 1) / tileHeight;
        } else {
            minTX = minTY = 0;
            maxTX = maxTY = 1;
        }
        for (int b = 0; b < getNumBands(); b++) {
            for (int ty = minTY; ty < maxTY; ty++) {
                for (int tx = minTX; tx < maxTX; tx++) {

                    //-- intersection sur
                    final Raster rast = (image != null) ? image.getTile(tx, ty) : currentRaster;
                    final int minX = Math.max(rast.getMinX(), area.x);
                    final int minY = Math.max(rast.getMinY(), area.y);
                    final int maxX = Math.min(rast.getMinX() + tileWidth, area.x + area.width);
                    final int maxY = Math.min(rast.getMinY() + tileHeight, area.y + area.height);
                    if (minX > maxX || minY > maxY) throw new IllegalArgumentException("Expected area don't intersect internal data.");

                    final DataBuffer databuff = rast.getDataBuffer();
                    int srcRastId = bandOffsets[b] + ((minY - rast.getMinY()) * tileWidth + minX - rast.getMinX());
                    final int readLength = (maxX - minX);
                    int destId = 0;
                    for (int y = minY; y < maxY; y++) {

                        switch (sourceDataType) {
                            case DataBuffer.TYPE_BYTE : {
                                final byte[] src  = ((DataBufferByte) databuff).getData(bankIndices[b]);
                                System.arraycopy(src, srcRastId, (byte[]) buffer[b], destId, readLength);
                                break;
                            }
                            case DataBuffer.TYPE_USHORT : {
                                final short[] src  = ((DataBufferUShort) databuff).getData(bankIndices[b]);
                                System.arraycopy(src, srcRastId, (short[]) buffer[b], destId, readLength);
                                break;
                            }
                            case DataBuffer.TYPE_SHORT  : {
                                final short[] src  = ((DataBufferShort) databuff).getData(bankIndices[b]);
                                System.arraycopy(src, srcRastId, (short[]) buffer[b], destId, readLength);
                                break;
                            }
                            case DataBuffer.TYPE_INT : {
                                final int[] src  = ((DataBufferInt) databuff).getData(bankIndices[b]);
                                System.arraycopy(src, srcRastId, (int[]) buffer[b], destId, readLength);
                                break;
                            }
                            case DataBuffer.TYPE_FLOAT : {
                                final float[] src  = ((DataBufferFloat) databuff).getData(bankIndices[b]);
                                System.arraycopy(src, srcRastId, (float[]) buffer[b], destId, readLength);
                                break;
                            }
                            case DataBuffer.TYPE_DOUBLE : {
                                final double[] src  = ((DataBufferDouble) databuff).getData(bankIndices[b]);
                                System.arraycopy(src, srcRastId, (double[]) buffer[b], destId, readLength);
                                break;
                            }
                            default : {
                                throw new IllegalStateException("Unknow datatype.");
                            }
                        }
                        srcRastId += tileWidth;
                        destId    += area.width;
                    }
                }
            }
        }
    }

    /**
     * Fill given buffer with samples within the given area at the specified image band.
     *
     * @param area define needed samples area.
     * @param buffer array which will be filled by samples.
     * @param band the interest band.
     */
    public void getArea(final Rectangle area, final Object buffer, int band) {
        ArgumentChecks.ensureNonNull("area", area);
        ArgumentChecks.ensureNonNull("buffer", buffer);

        final int sourceDataType = getSourceDatatype();
        final int areaLength = area.width * area.height;

        switch (sourceDataType) {
            case DataBuffer.TYPE_BYTE : {
                if (!(buffer instanceof byte[])) throw new IllegalArgumentException("Buffer argument must be instance of byte[][] array");
                if (((byte[]) buffer).length < areaLength) throw new IllegalArgumentException("Buffer must have a length equal or upper than area sample number. Expected : "+areaLength);
                break;
            }
            case DataBuffer.TYPE_USHORT :
            case DataBuffer.TYPE_SHORT  : {
                if (!(buffer instanceof short[])) throw new IllegalArgumentException("Buffer argument must be instance of short[][] array");
                if (((short[]) buffer).length < areaLength) throw new IllegalArgumentException("Buffer must have a length equal or upper than area sample number. Expected : "+areaLength);
                break;
            }
            case DataBuffer.TYPE_INT : {
                if (!(buffer instanceof int[])) throw new IllegalArgumentException("Buffer argument must be instance of int[][] array");
                if (((int[]) buffer).length < areaLength) throw new IllegalArgumentException("Buffer must have a length equal or upper than area sample number. Expected : "+areaLength);
                break;
            }
            case DataBuffer.TYPE_FLOAT : {
                if (!(buffer instanceof float[])) throw new IllegalArgumentException("Buffer argument must be instance of float[][] array");
                if (((float[]) buffer).length < areaLength) throw new IllegalArgumentException("Buffer must have a length equal or upper than area sample number. Expected : "+areaLength);
                break;
            }
            case DataBuffer.TYPE_DOUBLE : {
                if (!(buffer instanceof double[])) throw new IllegalArgumentException("Buffer argument must be instance of double[][] array");
                if (((double[]) buffer).length < areaLength) throw new IllegalArgumentException("Buffer must have a length equal or upper than area sample number. Expected : "+areaLength);
                break;
            }
            default : {
                throw new IllegalStateException("Unknow datatype.");
            }
        }

        if (sampleModel instanceof ComponentSampleModel) {
            if (((ComponentSampleModel) sampleModel).getPixelStride() == 1) {
                getAreaByBanded(area, buffer, band);
                return;
            }
        }
        getAreaByInterleaved(area, buffer, band);
    }

    /**
     * Fill given buffer with samples within the given area at the specified image band.
     * Adapted for a {@link BandedSampleModel} {@link SampleModel} type.
     *
     * @param area define needed samples area.
     * @param buffer array which will be filled by samples.
     * @param band the interest band.
     */
    public void getAreaByBanded(final Rectangle area, final Object buffer, final int band) {
        final ComponentSampleModel compSM = (ComponentSampleModel) sampleModel;
        final int bankIndices = compSM.getBankIndices()[band];
        final int bandOffsets = compSM.getBandOffsets()[band];

        final int sourceDataType = getSourceDatatype();

        final int minTX, minTY, maxTX, maxTY;

        if (image != null) {
            minTX = timeDomain.minx + (area.x - xmin) / tileWidth;
            minTY = timeDomain.miny + (area.y - ymin) / tileHeight;
            maxTX = timeDomain.minx + (area.x + area.width + tileWidth - 1) / tileWidth;
            maxTY = timeDomain.miny + (area.y + area.height + tileHeight - 1) / tileHeight;
        } else {
            minTX = minTY = 0;
            maxTX = maxTY = 1;
        }
        for (int ty = minTY; ty < maxTY; ty++) {
            for (int tx = minTX; tx < maxTX; tx++) {

                //-- intersection sur
                final Raster rast = (image != null) ? image.getTile(tx, ty) : currentRaster;
                final int minX = Math.max(rast.getMinX(), area.x);
                final int minY = Math.max(rast.getMinY(), area.y);
                final int maxX = Math.min(rast.getMinX() + tileWidth, area.x + area.width);
                final int maxY = Math.min(rast.getMinY() + tileHeight, area.y + area.height);
                if (minX > maxX || minY > maxY) throw new IllegalArgumentException("Expected area don't intersect internal data.");

                final DataBuffer databuff = rast.getDataBuffer();
                int srcRastId = bandOffsets + ((minY - rast.getMinY()) * tileWidth + minX - rast.getMinX());
                final int readLength = (maxX - minX);
                int destId = 0;
                for (int y = minY; y < maxY; y++) {

                    switch (sourceDataType) {
                        case DataBuffer.TYPE_BYTE : {
                            final byte[] src  = ((DataBufferByte) databuff).getData(bankIndices);
                            System.arraycopy(src, srcRastId, (byte[]) buffer, destId, readLength);
                            break;
                        }
                        case DataBuffer.TYPE_USHORT : {
                            final short[] src  = ((DataBufferUShort) databuff).getData(bankIndices);
                            System.arraycopy(src, srcRastId, (short[]) buffer, destId, readLength);
                            break;
                        }
                        case DataBuffer.TYPE_SHORT  : {
                            final short[] src  = ((DataBufferShort) databuff).getData(bankIndices);
                            System.arraycopy(src, srcRastId, (short[]) buffer, destId, readLength);
                            break;
                        }
                        case DataBuffer.TYPE_INT : {
                            final int[] src  = ((DataBufferInt) databuff).getData(bankIndices);
                            System.arraycopy(src, srcRastId, (int[]) buffer, destId, readLength);
                            break;
                        }
                        case DataBuffer.TYPE_FLOAT : {
                            final float[] src  = ((DataBufferFloat) databuff).getData(bankIndices);
                            System.arraycopy(src, srcRastId, (float[]) buffer, destId, readLength);
                            break;
                        }
                        case DataBuffer.TYPE_DOUBLE : {
                            final double[] src  = ((DataBufferDouble) databuff).getData(bankIndices);
                            System.arraycopy(src, srcRastId, (double[]) buffer, destId, readLength);
                            break;
                        }
                        default : {
                            throw new IllegalStateException("Unknow datatype.");
                        }
                    }
                    srcRastId += tileWidth;
                    destId    += area.width;
                }
            }
        }
    }

    /**
     * Fill given buffer with samples within the given area and from all the source image band.
     *
     * @param area define needed samples area.
     * @param buffer array which will be filled by samples.
     */
    public void getArea(final Rectangle area, final Object[] buffer) {
        ArgumentChecks.ensureNonNull("area", area);
        ArgumentChecks.ensureNonNull("buffer", buffer);
        if (buffer.length < getNumBands()) throw new IllegalArgumentException("buffer must have length equals to numbands. Found : "+buffer.length+". Expected : "+getNumBands());

        final int sourceDataType = getSourceDatatype();
        final int areaLength = area.width * area.height * getNumBands();

        switch (sourceDataType) {
            case DataBuffer.TYPE_BYTE : {
                if (!(buffer instanceof byte[][])) throw new IllegalArgumentException("Buffer argument must be instance of byte[][] array");
                if (((byte[][]) buffer)[0].length < areaLength) throw new IllegalArgumentException("Buffer must have a length equal or upper than area sample number. Expected : "+areaLength);
                break;
            }
            case DataBuffer.TYPE_USHORT :
            case DataBuffer.TYPE_SHORT  : {
                if (!(buffer instanceof short[][])) throw new IllegalArgumentException("Buffer argument must be instance of short[][] array");
                if (((short[][]) buffer)[0].length < areaLength) throw new IllegalArgumentException("Buffer must have a length equal or upper than area sample number. Expected : "+areaLength);
                break;
            }
            case DataBuffer.TYPE_INT : {
                if (!(buffer instanceof int[][])) throw new IllegalArgumentException("Buffer argument must be instance of int[][] array");
                if (((int[][]) buffer)[0].length < areaLength) throw new IllegalArgumentException("Buffer must have a length equal or upper than area sample number. Expected : "+areaLength);
                break;
            }
            case DataBuffer.TYPE_FLOAT : {
                if (!(buffer instanceof float[][])) throw new IllegalArgumentException("Buffer argument must be instance of float[][] array");
                if (((float[][]) buffer)[0].length < areaLength) throw new IllegalArgumentException("Buffer must have a length equal or upper than area sample number. Expected : "+areaLength);
                break;
            }
            case DataBuffer.TYPE_DOUBLE : {
                if (!(buffer instanceof double[][])) throw new IllegalArgumentException("Buffer argument must be instance of double[][] array");
                if (((double[][]) buffer)[0].length < areaLength) throw new IllegalArgumentException("Buffer must have a length equal or upper than area sample number. Expected : "+areaLength);
                break;
            }
            default : {
                throw new IllegalStateException("Unknow datatype.");
            }
        }

        if (sampleModel instanceof ComponentSampleModel) {
            if (((ComponentSampleModel) sampleModel).getPixelStride() == 1) {
                getAreaByBanded(area, buffer);
                return;
            }
        }
        getAreaByInterleaved(area, buffer);
    }

    /**
     * Return type data from iterate source.
     * @return type data from iterate source.
     */
    public int getSourceDatatype() {
        return (image != null ? image.getSampleModel() : currentRaster.getSampleModel()).getDataType();
    }

    /**
     * Compute an array which give the number of data elements until the next sample in the pixel. Note that the first
     * element gives number of elements between the last sample of the previous pixel and the first sample of current one.
     * @param bandOffsets The bandOffsets table given by {@link java.awt.image.ComponentSampleModel#getBandOffsets()}.
     * @param pixelStride The pixel stride value given by {@link java.awt.image.ComponentSampleModel#getPixelStride()}
     * @return An array whose components are the number of elements to skip until the next sample.
     */
    static int[] getBandSteps(final int[] bandOffsets, final int pixelStride) {
        final int[] bandSteps = new int[bandOffsets.length];
        bandSteps[0] = bandOffsets[0] + pixelStride - bandOffsets[bandOffsets.length-1];
        for (int i = 1 ; i < bandSteps.length ; i++) {
            bandSteps[i] = bandOffsets[i] - bandOffsets[i-1];
        }
        return bandSteps;
    }

    /**
     * Restores the iterator to the start position. After this method has been invoked,
     * the iterator is in the same state than after construction.
     */
    public abstract void rewind();
}
