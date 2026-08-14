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
package org.apache.sis.storage.image.internal;

import java.awt.Point;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import java.util.Arrays;
import org.apache.sis.image.internal.shared.ColorModelFactory;
import org.apache.sis.image.internal.shared.FillValues;
import org.apache.sis.image.internal.shared.WritableUntiledImage;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ImageBuilder {

    private RenderedImage reference;
    private int width = 1;
    private int height = 1;
    private int dataType = DataBuffer.TYPE_BYTE;
    private int nbBand = 1;
    private double[] fillValue;

    public ImageBuilder() {
    }

    public ImageBuilder copyFrom(RenderedImage model) {
        this.reference = model;
        final SampleModel sm = reference.getSampleModel();
        width = reference.getWidth();
        height = reference.getHeight();
        nbBand = sm.getNumBands();
        dataType = sm.getDataType();
        return this;
    }

    public ImageBuilder setSize(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public ImageBuilder setDataType(int dataType) {
        this.dataType = dataType;
        return this;
    }

    public ImageBuilder setNumBands(int numBands) {
        this.nbBand = numBands;
        return this;
    }

    public ImageBuilder setFillValue(double[] fillValue) {
        this.fillValue = fillValue;
        return this;
    }

    public BufferedImage createBufferedImage() {

        if (reference != null) {
            final SampleModel sm = reference.getSampleModel();

            if (nbBand == sm.getNumBands() && dataType == sm.getDataType()) {
                //we can preserver color model and raster configuration
                final Raster anyTile = reference.getTile(reference.getMinTileX(), reference.getMinTileY());
                final WritableRaster raster = anyTile.createCompatibleWritableRaster(width, height);
                if (fillValue != null && !isAllZero(fillValue)) setAll(raster, fillValue);
                ColorModel cm = reference.getColorModel();
                if (cm == null) {
                    cm = ColorModelFactory.createGrayScale(dataType, nbBand, 0, 0, 1);
                }
                final BufferedImage resultImage = new WritableUntiledImage(cm, raster, cm.isAlphaPremultiplied(), null);
                return resultImage;
            } else {
                //we need to create a new image
            }
        }

        //create a new image
        final WritableRaster raster = createRaster();

        //TODO try to reuse java colormodel if possible
        //create a temporary fallback colormodel which will always work
        //extract grayscale min/max from sample dimension
        final ColorModel graycm = ColorModelFactory.createGrayScale(dataType, nbBand, 0, 0, 1);
        return new WritableUntiledImage(graycm, raster, false, null);

    }

    public WritableRenderedImage createImage() {
        return createBufferedImage();
    }

    public WritableRaster createRaster() {
        final Point upperLeft = new Point(0,0);

        if (fillValue != null) {
            if (fillValue.length != nbBand) {
                throw new IllegalArgumentException("Fill value size " + fillValue.length + "do not match nb band " + nbBand);
            }
            /*
            Created rasters are filled with 0 by default, set fillValue to null if all samples are 0
            */
            if (isAllZero(fillValue)) {
                fillValue = null;
            }
        }

        final WritableRaster raster;
        if (nbBand == 1) {
            if (dataType == DataBuffer.TYPE_BYTE || dataType == DataBuffer.TYPE_USHORT || dataType == DataBuffer.TYPE_INT) {
                raster = WritableRaster.createBandedRaster(dataType, width, height, nbBand, upperLeft);
                if (fillValue != null) {
                    new FillValues(raster.getSampleModel(), new Number[]{ fillValue[0] }, true).fill(raster);
                }
            } else {
                //create it ourself
                final int bufferSize = Math.multiplyExact(width, height);

                final DataBuffer buffer;
                if (dataType == DataBuffer.TYPE_SHORT) {
                    final short[] data = new short[bufferSize];
                    if (fillValue != null) Arrays.fill(data, (short) fillValue[0]);
                    buffer = new DataBufferShort(data, bufferSize);
                } else if(dataType == DataBuffer.TYPE_FLOAT) {
                    final float[] data = new float[bufferSize];
                    if (fillValue != null) Arrays.fill(data, (float) fillValue[0]);
                    buffer = new DataBufferFloat(data, bufferSize);
                } else if(dataType == DataBuffer.TYPE_DOUBLE) {
                    final double[] data = new double[bufferSize];
                    if (fillValue != null) Arrays.fill(data, fillValue[0]);
                    buffer = new DataBufferDouble(data, bufferSize);
                } else {
                    throw new IllegalArgumentException("Type not supported "+dataType);
                }
                final int[] zero = new int[1];
                //TODO create our own raster factory to avoid JAI
                raster = org.apache.sis.image.internal.shared.RasterFactory.createRaster(buffer, width, height, 1, width, zero, zero, upperLeft);
                //raster = RasterFactory.createBandedRaster(buffer, width, height, width, zero, zero, upperLeft);
            }

        } else {
            if (dataType == DataBuffer.TYPE_BYTE || dataType == DataBuffer.TYPE_USHORT) {
                raster = WritableRaster.createInterleavedRaster(dataType, width, height, nbBand, upperLeft);
                if (fillValue != null) {
                    setAll(raster, fillValue);
                }
            } else {
                //create it ourself
                final int size = Math.multiplyExact(Math.multiplyExact(width, height), nbBand);
                final DataBuffer buffer;
                switch (dataType) {
                    case DataBuffer.TYPE_SHORT: buffer = new DataBufferShort(size); break;
                    case DataBuffer.TYPE_INT: buffer = new DataBufferInt(size); break;
                    case DataBuffer.TYPE_FLOAT: buffer = new DataBufferFloat(size); break;
                    case DataBuffer.TYPE_DOUBLE: buffer = new DataBufferDouble(size); break;
                    default: throw new IllegalArgumentException("Type not supported "+dataType);
                }
                final int[] bankIndices = new int[nbBand];
                final int[] bandOffsets = new int[nbBand];
                for(int i=1;i<nbBand;i++){
                    bandOffsets[i] = bandOffsets[i-1] + width*height;
                }
                /*
                 * The following line creates a `BandedSampleModel` with only one bank shared by all bands.
                 * This is unusual, as the standard pattern of `BandedSampleModel` convenience constructors
                 * is to create one bank per band. It is possible to have many banks wrapping the same array
                 * at different offsets. The use of a single bank, as done below, is more typically done with
                 * `PixelInterleavedSampleModel`.
                 *
                 * However, despite being unusual, Java2D seems to accept this configuration. The following
                 * code uses that configuration because this is what the initial version of this method was
                 * doing. But it should not be used as a source of inspiration for new codes.
                 */
                var sm = new BandedSampleModel(dataType, width, height, width, bankIndices, bandOffsets);
                raster = Raster.createWritableRaster(sm, buffer, upperLeft);
                if (fillValue != null) {
                    setAll(raster, fillValue);
                }
            }
        }
        return raster;
    }

    private static boolean isAllZero(double[] array) {
        for (double s : array) {
            if (s != 0) {
                return false;
            }
        }
        return true;
    }

    private static void setAll(WritableRaster raster, double[] pixel) {
        final Number[] values = new Number[pixel.length];
        for (int i = 0; i < values.length; i++) values[i] = pixel[i];
        new FillValues(raster.getSampleModel(), values, true).fill(raster);
    }
}
