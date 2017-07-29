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
import java.io.Closeable;
import org.opengis.coverage.grid.SequenceType;
import org.apache.sis.util.ArgumentChecks;


/**
 * An iterator over sample values in a raster or an image. This iterator simplifies accesses to sample values
 * by hiding the {@linkplain SampleModel sample model} and tiling complexity. Iteration may be done on a full
 * image or on only a sub-area of it. Iteration order is implementation specific.
 *
 * @author  Rémi Marechal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public abstract class PixelIterator implements Closeable {
    /**
     * Define boundary, in pixel coordinates, of area traveled by this PixeIterator.
     *
     * @see #getDomain()
     */
    final RectIter areaIterate;

    /**
     * Define boundary, in pixel coordinates, of iterated object.
     */
    private final Rectangle objectBoundary;

    /**
     * Tile index of iterated object.
     * note : raster is considered as image of one tile.
     */
    final RectIter tileIndexArea;

    /**
     * Size of tiles from iterated object.
     */
    final int tileWidth, tileHeight;

    /**
     * Current raster which is followed by Iterator.
     */
    Raster currentRaster;

    /**
     * RenderedImage which is followed by Iterator.
     */
    final RenderedImage renderedImage;

    /**
     * Number of band.
     */
    private final int fixedNumBand;

    /**
     * Number of raster band.
     * WARNING ! this is used a bit everywhere in iterator as a 'updateTileRaster' flag.
     */
    int rasterNumBand;

    /**
     * Current band position in this current raster.
     */
    int band;

    /**
     * {@link SampleModel} from the iterate object.
     */
    private final SampleModel currentSampleModel;

    //-- Iteration attributs
    /**
     * Stored position of upper right corner of current traveled raster.
     * Generally when this values are reach an update of the current
     * traveled raster is effectuate.
     */
    int currentRasterMaxX;
    int currentRasterMaxY;

    /**
     * Current Tile index of current traveled raster.
     */
    int tX;
    int tY;

    /**
     * Create raster iterator to follow from minX, minY raster and rectangle intersection coordinate.
     *
     * @param raster will be followed by this iterator.
     * @param subArea {@code Rectangle} which define read iterator area.
     * @throws IllegalArgumentException if subArea don't intersect raster boundary.
     */
    PixelIterator(final Raster raster, final Rectangle subArea) {
        ArgumentChecks.ensureNonNull("raster", raster);
        objectBoundary     = new Rectangle(raster.getMinX(), raster.getMinY(), raster.getWidth(), raster.getHeight());
        currentRaster      = raster;
        renderedImage      = null;
        tileWidth          = objectBoundary.width;
        tileHeight         = objectBoundary.height;
        currentSampleModel = raster.getSampleModel();
        areaIterate        = new RectIter((subArea != null)
                             ? subArea.intersection(objectBoundary)
                             : objectBoundary);

        if (areaIterate.isEmpty())
            throw new IllegalArgumentException("No intersection between subArea and raster.\n "
                    + "Raster boundary  = " + objectBoundary + "\n "
                    + "subArea boundary = " + subArea);

        //-- in our case only one raster -> tile index 0 -> 1
        tileIndexArea      = new RectIter(0, 0, 1, 1);

        this.rasterNumBand = raster.getNumBands();
        this.fixedNumBand  = this.rasterNumBand; //-- je pense que c uniquement utilisé pour les direct iterators
        this.band = -1;
    }

    /**
     * Create default rendered image iterator.
     *
     * @param renderedImage image which will be follow by iterator.
     * @param subArea {@code Rectangle} which represent image sub area iteration.
     * @throws IllegalArgumentException if subArea don't intersect image boundary.
     */
    PixelIterator(final RenderedImage renderedImage, final Rectangle subArea) {
        ArgumentChecks.ensureNonNull("renderedImage", renderedImage);
        this.renderedImage = renderedImage;
        objectBoundary     = new Rectangle(renderedImage.getMinX(), renderedImage.getMinY(), renderedImage.getWidth(), renderedImage.getHeight());
        tileWidth          = renderedImage.getTileWidth();
        tileHeight         = renderedImage.getTileHeight();
        currentSampleModel = renderedImage.getSampleModel();
        areaIterate        = new RectIter((subArea != null)
                                         ? subArea.intersection(objectBoundary)
                                         : objectBoundary);
        if (areaIterate.isEmpty())
            throw new IllegalArgumentException("No intersection between subArea and raster.\n "
                    + "RenderedImage boundary  = " + objectBoundary + "\n "
                    + "subArea boundary = " + subArea);

        //--tiles index attributs computing
        {
            final int offTX = renderedImage.getTileGridXOffset();
            final int offTY = renderedImage.getTileGridYOffset();
            final int tMinX = (areaIterate.minx - objectBoundary.x) / tileWidth  + offTX;
            final int tMinY = (areaIterate.miny - objectBoundary.y) / tileHeight + offTY;
            final int tMaxX = (areaIterate.maxX - objectBoundary.x + tileWidth  - 1) / tileWidth  + offTX;
            final int tMaxY = (areaIterate.maxY - objectBoundary.y + tileHeight - 1) / tileHeight + offTY;
            tileIndexArea = new RectIter(tMinX, tMinY, tMaxX - tMinX, tMaxY - tMinY);
        }

        //initialize attributs to first iteration
        this.band = -1;
        this.rasterNumBand = 1;
        this.fixedNumBand  = currentSampleModel.getNumBands();
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
     * Initializes iterator.
     * Carry back iterator at its initial position like iterator is just build.
     */
    public abstract void rewind();

    /**
     * To release last tiles iteration from writable rendered image tiles array.
     * if this method is invoked in read-only iterator, method is idempotent (has no effect).
     */
    public abstract void close();

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
        if (x < areaIterate.minx || x >= areaIterate.maxX
            ||  y < areaIterate.miny || y >= areaIterate.maxY)
                throw new IllegalArgumentException("coordinate out of iteration area define by : ("+areaIterate+") \n "
                        + "given coordinates are : "+x+" "+y);
        if (b<0 || b>=fixedNumBand)
            throw new IllegalArgumentException("band index out of numband border define by: [0;"+fixedNumBand+"]");
    }

    /**
     * Returns the number of bands (samples per pixel) from Image or Raster within this Iterator.
     *
     * @return the number of bands (samples per pixel) from current raster or Image.
     */
    public int getNumBands() {
        return fixedNumBand;
    }

    /**
     * Returns the pixel coordinates of the area in which this iterator is doing the iteration.
     *
     * @return pixel coordinates of the iteration area.
     */
    public Rectangle getDomain() {
        return areaIterate.toRectangle();
    }

    //-- TODO : methodes suivantes a refactorer (code duplication) + mettre ailleur + static + package private
    /**
     * Check that the two input rasters are compatible for coupling in a {@link WritablePixelIterator}
     */
    public static void checkRasters(final Raster readableRaster, final WritableRaster writableRaster){
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
    public static void checkRenderedImage(final RenderedImage renderedImage, final WritableRenderedImage writableRI) {
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
    protected void checkRasters(final Raster readableRaster, final WritableRaster writableRaster, final Rectangle subArea) {
        final int wRmx = writableRaster.getMinX();
        final int wRmy = writableRaster.getMinY();
        final int wRw  = writableRaster.getWidth();
        final int wRh  = writableRaster.getHeight();
        if ((wRmx != areaIterate.minx)
          || wRmy != areaIterate.miny
          || wRw  != areaIterate.width
          || wRh  != areaIterate.height)

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
    protected void checkRenderedImage(final RenderedImage renderedImage, final WritableRenderedImage writableRI, final Rectangle subArea) {
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
        final boolean minTileX = (wrimtx == (areaIterate.minx - riMinX)/ riTileWidth  + rimtx);
        final boolean minTileY = (wrimty == (areaIterate.miny - riMinY)/ riTileHeight + rimty);

        //writable image correspond with iteration area
        if (writableRI.getMinX()  != areaIterate.minx    //areaiteration
         || writableRI.getMinY()  != areaIterate.miny    //areaiteration
         || writableRI.getWidth() != areaIterate.width//longueuriteration
         || writableRI.getHeight()!= areaIterate.height//largeuriteration
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

        if (renderedImage != null) {
            minTX = tileIndexArea.minx + (area.x - objectBoundary.x) / tileWidth;
            minTY = tileIndexArea.miny + (area.y - objectBoundary.y) / tileHeight;
            maxTX = tileIndexArea.minx + (area.x + area.width + tileWidth - 1) / tileWidth;
            maxTY = tileIndexArea.miny + (area.y + area.height + tileHeight - 1) / tileHeight;
        } else {
            minTX = minTY = 0;
            maxTX = maxTY = 1;
        }

        for (int ty = minTY; ty < maxTY; ty++) {
            for (int tx = minTX; tx < maxTX; tx++) {

                //-- intersection sur
                final Raster rast = (renderedImage != null) ? renderedImage.getTile(tx, ty) : currentRaster;
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

        if (renderedImage != null) {
            minTX = tileIndexArea.minx + (area.x - objectBoundary.x) / tileWidth;
            minTY = tileIndexArea.miny + (area.y - objectBoundary.y) / tileHeight;
            maxTX = tileIndexArea.minx + (area.x + area.width + tileWidth - 1) / tileWidth;
            maxTY = tileIndexArea.miny + (area.y + area.height + tileHeight - 1) / tileHeight;
        } else {
            minTX = minTY = 0;
            maxTX = maxTY = 1;
        }

        for (int ty = minTY; ty < maxTY; ty++) {
            for (int tx = minTX; tx < maxTX; tx++) {

                //-- intersection sur
                final Raster rast = (renderedImage != null) ? renderedImage.getTile(tx, ty) : currentRaster;
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

        final ComponentSampleModel compSM = (ComponentSampleModel) currentSampleModel;
        final int[] bankIndices = compSM.getBankIndices();
        assert bankIndices.length == getNumBands();
        final int[] bandOffsets = compSM.getBandOffsets();
        assert bandOffsets.length == getNumBands();

        final int sourceDataType = getSourceDatatype();

        final int minTX, minTY, maxTX, maxTY;

        if (renderedImage != null) {
            minTX = tileIndexArea.minx + (area.x - objectBoundary.x) / tileWidth;
            minTY = tileIndexArea.miny + (area.y - objectBoundary.y) / tileHeight;
            maxTX = tileIndexArea.minx + (area.x + area.width + tileWidth - 1) / tileWidth;
            maxTY = tileIndexArea.miny + (area.y + area.height + tileHeight - 1) / tileHeight;
        } else {
            minTX = minTY = 0;
            maxTX = maxTY = 1;
        }
        for (int b = 0; b < getNumBands(); b++) {
            for (int ty = minTY; ty < maxTY; ty++) {
                for (int tx = minTX; tx < maxTX; tx++) {

                    //-- intersection sur
                    final Raster rast = (renderedImage != null) ? renderedImage.getTile(tx, ty) : currentRaster;
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

        if (currentSampleModel instanceof ComponentSampleModel) {
            if (((ComponentSampleModel) currentSampleModel).getPixelStride() == 1) {
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
        final ComponentSampleModel compSM = (ComponentSampleModel) currentSampleModel;
        final int bankIndices = compSM.getBankIndices()[band];
        final int bandOffsets = compSM.getBandOffsets()[band];

        final int sourceDataType = getSourceDatatype();

        final int minTX, minTY, maxTX, maxTY;

        if (renderedImage != null) {
            minTX = tileIndexArea.minx + (area.x - objectBoundary.x) / tileWidth;
            minTY = tileIndexArea.miny + (area.y - objectBoundary.y) / tileHeight;
            maxTX = tileIndexArea.minx + (area.x + area.width + tileWidth - 1) / tileWidth;
            maxTY = tileIndexArea.miny + (area.y + area.height + tileHeight - 1) / tileHeight;
        } else {
            minTX = minTY = 0;
            maxTX = maxTY = 1;
        }
        for (int ty = minTY; ty < maxTY; ty++) {
            for (int tx = minTX; tx < maxTX; tx++) {

                //-- intersection sur
                final Raster rast = (renderedImage != null) ? renderedImage.getTile(tx, ty) : currentRaster;
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

        if (currentSampleModel instanceof ComponentSampleModel) {
            if (((ComponentSampleModel) currentSampleModel).getPixelStride() == 1) {
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
        return (renderedImage == null) ? currentRaster.getSampleModel().getDataType() : renderedImage.getSampleModel().getDataType();
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
}
