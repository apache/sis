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
import java.awt.Rectangle;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.ImagingOpException;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.function.LongFunction;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.apache.sis.image.PixelIterator;
import org.apache.sis.image.PlanarImage;
import org.apache.sis.image.WritablePixelIterator;
import org.apache.sis.image.internal.shared.ColorModelFactory;
import org.apache.sis.image.internal.shared.WritableUntiledImage;
import org.apache.sis.image.internal.shared.FillValues;
import org.apache.sis.storage.util.TriFunction;
import org.apache.sis.util.ArgumentChecks;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class BufferedImages {

    /**
     * Create a new image, trying to preserve raster, sample model and color model
     * when possible.
     *
     * @param reference not null
     * @param width if null reference image width is copied
     * @param height if null reference image height is copied
     * @param nbBand if null reference image number of bands is copied
     * @param dataType if null reference image data type is copied
     */
    public static BufferedImage createImage(final RenderedImage reference, Integer width, Integer height, Integer nbBand, Integer dataType) throws IllegalArgumentException{
        return createImage(reference, width, height, nbBand, dataType, null);
    }

    /**
     * Create a new image, trying to preserve raster, sample model and color model
     * when possible.
     *
     * @param reference not null
     * @param width if null reference image width is copied
     * @param height if null reference image height is copied
     * @param nbBand if null reference image number of bands is copied
     * @param dataType if null reference image data type is copied
     * @param fillValue if not null, samples to fill image
     */
    public static BufferedImage createImage(final RenderedImage reference, Integer width, Integer height, Integer nbBand, Integer dataType, double[] fillValue) throws IllegalArgumentException{
        final SampleModel sm = reference.getSampleModel();
        if (width == null) width = reference.getWidth();
        if (height == null) height = reference.getHeight();
        if (nbBand == null) nbBand = sm.getNumBands();
        if (dataType == null) dataType = sm.getDataType();
        ArgumentChecks.ensureStrictlyPositive("width", width);
        ArgumentChecks.ensureStrictlyPositive("height", height);
        ArgumentChecks.ensureStrictlyPositive("nbBand", nbBand);

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
            return createImage(width, height, nbBand, dataType, fillValue);
        }
    }

    /**
     * Create a new image of the same type with a different size.
     */
    public static BufferedImage createImage(final int width, final int height, RenderedImage reference) throws IllegalArgumentException {
        return createImage(reference, width, height, null, null);
    }

    public static BufferedImage createImage(final int width, final int height, final int nbBand, final int dataType) throws IllegalArgumentException{
        return createImage(width, height, nbBand, dataType, null);
    }

    public static BufferedImage createImage(final int width, final int height, final int nbBand, final int dataType, double[] fillValue) throws IllegalArgumentException{
        ArgumentChecks.ensureStrictlyPositive("width", width);
        ArgumentChecks.ensureStrictlyPositive("height", height);
        final Point upperLeft = new Point(0,0);
        final WritableRaster raster = createRaster(width, height, nbBand, dataType, upperLeft, fillValue);

        //TODO try to reuse java colormodel if possible
        //create a temporary fallback colormodel which will always work
        //extract grayscale min/max from sample dimension
        final ColorModel graycm = ColorModelFactory.createGrayScale(dataType, nbBand, 0, 0, 1);
        return new WritableUntiledImage(graycm, raster, false, null);
    }

    public static WritableRaster createRaster(int width, int height, int nbBand, int dataType, Point upperLeft) throws IllegalArgumentException{
        return createRaster(width, height, nbBand, dataType, upperLeft, null);
    }

    public static WritableRaster createRaster(int width, int height, int nbBand, int dataType, Point upperLeft, double[] fillValue) throws IllegalArgumentException{
        ArgumentChecks.ensureStrictlyPositive("width", width);
        ArgumentChecks.ensureStrictlyPositive("height", height);

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
                    fill(raster, fillValue[0]);
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

    /**
     * Convert a primitive array to a DataBuffer.
     * This DataBuffer can then be used to create a WritableRaster.
     * The array is directly used by the buffer, they are not copied.
     *
     * @param data primitive array object with 1 or 2 dimensions.
     * @return DataBuffer never null
     * @throws IllegalArgumentException if the array type is not supported.
     */
    public static DataBuffer toDataBuffer(Object data) throws IllegalArgumentException{
        if(data instanceof byte[]){
            return new DataBufferByte((byte[])data,Array.getLength(data));
        }else if(data instanceof short[]){
            return new DataBufferShort((short[])data,Array.getLength(data));
        }else if(data instanceof int[]){
            return new DataBufferInt((int[])data,Array.getLength(data));
        }else if(data instanceof float[]){
            return new DataBufferFloat((float[])data,Array.getLength(data));
        }else if(data instanceof double[]){
            return new DataBufferDouble((double[])data,Array.getLength(data));
        }

        else if(data instanceof byte[][]){
            return new DataBufferByte((byte[][])data,Array.getLength(Array.get(data, 0)));
        }else if(data instanceof short[][]){
            return new DataBufferShort((short[][])data,Array.getLength(Array.get(data, 0)));
        }else if(data instanceof int[][]){
            return new DataBufferInt((int[][])data,Array.getLength(Array.get(data, 0)));
        }else if(data instanceof float[][]){
            return new DataBufferFloat((float[][])data,Array.getLength(Array.get(data, 0)));
        }else if(data instanceof double[][]){
            return new DataBufferDouble((double[][])data,Array.getLength(Array.get(data, 0)));
        }

        else{
            throw new IllegalArgumentException("Unexpected array type "+data.getClass());
        }
    }

    public static DataBuffer toDataBuffer1D(Object data) throws ArithmeticException {
        if (data instanceof byte[][]) {
            final byte[][] matrix = (byte[][]) data;
            final int height = matrix.length;
            final int width = matrix[0].length;
            final int size = Math.multiplyExact(height,width);
            final byte[] datas = new byte[size];
            for (int i = 0, offset=0; i < matrix.length; i++,offset+=width) {
                System.arraycopy(matrix[i], 0, datas, offset, width);
            }
            return new DataBufferByte(datas, datas.length);
        } else if (data instanceof short[][]) {
            final short[][] matrix = (short[][]) data;
            final int height = matrix.length;
            final int width = matrix[0].length;
            final int size = Math.multiplyExact(height,width);
            final short[] datas = new short[size];
            for (int i = 0, offset=0; i < matrix.length; i++,offset+=width) {
                System.arraycopy(matrix[i], 0, datas, offset, width);
            }
            return new DataBufferShort(datas, datas.length);
        } else if (data instanceof int[][]) {
            final int[][] matrix = (int[][]) data;
            final int height = matrix.length;
            final int width = matrix[0].length;
            final int size = Math.multiplyExact(height,width);
            final int[] datas = new int[size];
            for (int i = 0, offset=0; i < matrix.length; i++,offset+=width) {
                System.arraycopy(matrix[i], 0, datas, offset, width);
            }
            return new DataBufferInt(datas, datas.length);
        } else if (data instanceof float[][]) {
            final float[][] matrix = (float[][]) data;
            final int height = matrix.length;
            final int width = matrix[0].length;
            final int size = Math.multiplyExact(height,width);
            final float[] datas = new float[size];
            for (int i = 0, offset=0; i < matrix.length; i++,offset+=width) {
                System.arraycopy(matrix[i], 0, datas, offset, width);
            }
            return new DataBufferFloat(datas, datas.length);
        } else if (data instanceof double[][]) {
            final double[][] matrix = (double[][]) data;
            final int height = matrix.length;
            final int width = matrix[0].length;
            final int size = Math.multiplyExact(height,width);
            final double[] datas = new double[size];
            for (int i = 0, offset=0; i < matrix.length; i++,offset+=width) {
                System.arraycopy(matrix[i], 0, datas, offset, width);
            }
            return new DataBufferDouble(datas, datas.length);
        } else {
            throw new IllegalArgumentException("Unexpected array type "+data.getClass());
        }
    }

    /**
     * Compare the pixels of given image to reference pixel and return true
     * if all pixels share those same samples.
     *
     * @param img image to test
     * @param pixel reference pixel to compare with
     * @return true if all pixels is image are equal to reference pixel.
     */
    public static boolean isAll(RenderedImage img, double[] pixel) {
        final PixelIterator ite = PixelIterator.create(img);
        final double[] buffer = new double[pixel.length];
        while (ite.next()) {
            ite.getPixel(buffer);
            if (!Arrays.equals(pixel, buffer)) {
                return false;
            }
        }
        return true;
    }

    public static void setAll(WritableRenderedImage img, double[] pixel) {

        final int minTileX = img.getMinTileX();
        final int minTileY = img.getMinTileY();
        final int numXTiles = img.getNumXTiles();
        final int numYTiles = img.getNumYTiles();

        for (int y = minTileY, yn = minTileY + numYTiles; y < yn; y++) {
            for (int x = minTileX, xn = minTileX + numXTiles; x < xn; x++) {
                WritableRaster raster = img.getWritableTile(x, y);
                setAll(raster, pixel);
                img.releaseWritableTile(x, y);
            }
        }
    }

    public static void setAll(WritableRaster raster, double[] pixel) {
        final Number[] values = new Number[pixel.length];
        for (int i = 0; i < values.length; i++) values[i] = pixel[i];
        new FillValues(raster.getSampleModel(), values, true).fill(raster);
    }

    /**
     * Tests if all pixels in the image are identical and images have the same geometry.
     *
     * @return true if pixels are identical
     */
    public static boolean isPixelsIdenticals(RenderedImage image1, RenderedImage image2) {
        final PixelIterator ite1 = PixelIterator.create(image1);
        final PixelIterator ite2 = PixelIterator.create(image2);
        if (!ite1.getDomain().equals(ite2.getDomain())) {
            return false;
        }
        final double[] pixel1 = new double[ite1.getNumBands()];
        final double[] pixel2 = new double[ite2.getNumBands()];

        while (ite1.next()) {
            final Point position = ite1.getPosition();
            ite2.moveTo(position.x, position.y);
            ite1.getPixel(pixel1);
            ite2.getPixel(pixel2);
            if (!Arrays.equals(pixel1, pixel2)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Create a stream of point in the rectangle.
     */
    public static Stream<Point> pointStream(Rectangle rectangle) {
        return LongStream.range(0, rectangle.width * rectangle.height).mapToObj((long value) -> {
            final int x = (int) (value % rectangle.width);
            final int y = (int) (value / rectangle.width);
            return new Point(rectangle.x + x, rectangle.y + y);
        });
    }

    /**
     * Create a stream of rectangle covering each tile in the image.
     * <p>
     * A margin on each side can be defined to expend the rectangle in the limit of the image size.
     * This behavior allows to overlaps tile border in case the algorithm requires it.
     * </p>
     *
     * @param top top tile margin
     * @param right right tile margin
     * @param bottom bottom tile margin
     * @param left  left tile margin
     * @return Stream of Rectangle for each tile in the image.
     */
    public static Stream<Rectangle> tileStream(RenderedImage image, int top, int right, int bottom, int left) {
        final int tileWidth = image.getTileWidth();
        final int tileHeight = image.getTileHeight();
        final int numXTiles = image.getNumXTiles();
        final int numYTiles = image.getNumYTiles();
        final int minTileX = image.getMinTileX();
        final int minTileY = image.getMinTileY();
        final int tileGridXOffset = image.getTileGridXOffset();
        final int tileGridYOffset = image.getTileGridYOffset();
        final long nbTile = (long)numXTiles * numYTiles;
        final Rectangle imageBounds = new Rectangle(image.getMinX(), image.getMinY(), image.getWidth(), image.getHeight());

        final int rectWidth = tileWidth + right + left;
        final int rectHeight = tileHeight + top + bottom;

        return LongStream.range(0, nbTile)
                .mapToObj(new LongFunction<Rectangle>() {
                    @Override
                    public Rectangle apply(long value) {
                        final int tileX = (int) (value % numXTiles);
                        final int tileY = (int) (value / numXTiles);
                        final int rectX = (minTileX + tileX) * tileWidth + tileGridXOffset;
                        final int rectY = (minTileY + tileY) * tileHeight + tileGridYOffset;
                        return new Rectangle(rectX - left,rectY - bottom,rectWidth,rectHeight).intersection(imageBounds);
                    }
                });
    }

    /**
     * Ensure the given raster is compatible with raster template.
     * If not the data will be copied to a new raster.
     *
     * @param in raster to verify and transform if needed
     * @param rasterTemplate reference raster to compare with
     * @return compatible raster
     * @throws ImagingOpException if raster can not be transformed
     */
    public static Raster makeConform(Raster in, Raster rasterTemplate) throws ImagingOpException {
        final int inNumBands = in.getNumBands();
        final int outNumBands = rasterTemplate.getNumBands();
        if (inNumBands != outNumBands) {
            //this is a severe issue, the raster do no respect the expected number of samples
            throw new ImagingOpException("Mosaic tile image declares " + inNumBands
                    + " bands, but sample dimensions have " + outNumBands);
        }

        final int inDataType = in.getDataBuffer().getDataType();
        final int outDataType = rasterTemplate.getDataBuffer().getDataType();
        if (inDataType != outDataType) {
            //difference in input and output types, this may be caused by an aggregated resource
            final int x = 0;
            final int y = 0;
            final int width = in.getWidth();
            final int height = in.getHeight();
            final WritableRaster raster = rasterTemplate.createCompatibleWritableRaster(in.getMinX(), in.getMinY(), width, height);
            final int nbSamples = width * height * inNumBands;
            switch (outDataType) {
                case DataBuffer.TYPE_BYTE :
                case DataBuffer.TYPE_SHORT :
                case DataBuffer.TYPE_USHORT :
                case DataBuffer.TYPE_INT :
                    int[] arrayi = new int[nbSamples];
                    in.getPixels(x, y, width, height, arrayi);
                    raster.setPixels(x, y, width, height, arrayi);
                    break;
                case DataBuffer.TYPE_FLOAT :
                    float[] arrayf = new float[nbSamples];
                    in.getPixels(x, y, width, height, arrayf);
                    raster.setPixels(x, y, width, height, arrayf);
                    break;
                case DataBuffer.TYPE_DOUBLE :
                default :
                    double[] arrayd = new double[nbSamples];
                    in.getPixels(x, y, width, height, arrayd);
                    raster.setPixels(x, y, width, height, arrayd);
                    break;
            }
            in = raster;
        }
        return in;
    }

    /**
     * Merge images with a user defined function.
     * Images may have different sample models, number of bands or data types but must have the same geometry.
     *
     * @param source image to read from
     * @param target image to read and write into
     * @param filterByMask only apply merge operation where the source mask is valid, if mask is undefined this property has no effect.
     * @param merger function transforming values before writing. parameters are in order : position in image, source pixel, target pixel, result pixel
     *               if result pixel is null, the target pixel values are unchanged.
     * @throws IllegalArgumentException if source and target images have different geometries
     */
    public static void mergeImages(RenderedImage source, WritableRenderedImage target, boolean filterByMask, TriFunction<Point, double[], double[], double[]> merger) throws IllegalArgumentException{

        if (  source.getMinX() != target.getMinX()
           || source.getMinY() != target.getMinY()
           || source.getWidth() != target.getWidth()
           || source.getHeight() != target.getHeight()) {
            throw new IllegalArgumentException("Source and target images must have the same geometry.");
        }


        RenderedImage sourceMask = null;
        if (filterByMask) {
            Object cdt = source.getProperty(PlanarImage.MASK_KEY);
            if (cdt instanceof RenderedImage) {
                sourceMask = (RenderedImage) cdt;
            }
        }

        final PixelIterator sourceIte = new PixelIterator.Builder().create(source);
        final WritablePixelIterator targetIte = new WritablePixelIterator.Builder().createWritable(target);
        final double[] pixelr = new double[sourceIte.getNumBands()];
        final double[] pixelw = new double[targetIte.getNumBands()];

        Point position;

        if (sourceMask != null) {
            final PixelIterator sourceMaskIte = new PixelIterator.Builder().create(sourceMask);
            while (sourceMaskIte.next()) {

                //check the source mask
                if (sourceMaskIte.getSample(0) == 0) {
                    position = sourceMaskIte.getPosition();

                    sourceIte.moveTo(position.x, position.y);
                    targetIte.moveTo(position.x, position.y);
                    sourceIte.getPixel(pixelr);
                    targetIte.getPixel(pixelw);

                    final double[] result = merger.apply(position, pixelr, pixelw);
                    if (result != null) {
                        targetIte.setPixel(result);
                    }
                }
            }
        } else {
            while (sourceIte.next()) {
                position = sourceIte.getPosition();

                targetIte.moveTo(position.x, position.y);
                sourceIte.getPixel(pixelr);
                targetIte.getPixel(pixelw);

                final double[] result = merger.apply(position, pixelr, pixelw);
                if (result != null) {
                    targetIte.setPixel(result);
                }
            }
        }
    }

    private static boolean isAllZero(double[] array) {
        for (double s : array) {
            if (s != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sets every samples in the given image to the given value. This method is typically used
     * for clearing an image content.
     *
     * @param image The image to fill (modified in-place).
     * @param value The value to be given to every samples.
     * @see FillValues used pixel filling engine.
     */
    public static void fill(final WritableRenderedImage image, final Number value) {
        final SampleModel sampleModel = image.getSampleModel();
        final int numBands = sampleModel.getNumBands();
        final Number[] fillValues = new Number[numBands];
        Arrays.fill(fillValues, value);
        final FillValues filler = new FillValues(sampleModel, fillValues, true);
        int y = image.getMinTileY();
        for (int ny = image.getNumYTiles(); --ny >= 0; y++) {
            int x = image.getMinTileX();
            for (int nx = image.getNumXTiles(); --nx >= 0; x++) {
                final WritableRaster raster = image.getWritableTile(x, y);
                try {
                    filler.fill(raster);
                } finally {
                    image.releaseWritableTile(x, y);
                }
            }
        }
    }

    /**
     * @deprecated Use Apache SIS utilities instead.
     * @see FillValues#fill(WritableRaster)
     */
    @Deprecated
    public static void fill(final WritableRaster raster, final Number value) {
        new FillValues(raster.getSampleModel(), new Number[]{ value }, true).fill(raster);
    }
}
