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
package org.apache.sis.image.privy;

import java.awt.Point;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.BandedSampleModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.RasterFormatException;
import java.awt.image.WritableRaster;
import java.awt.image.BufferedImage;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.IntBuffer;
import java.nio.FloatBuffer;
import java.nio.DoubleBuffer;
import java.nio.ReadOnlyBufferException;
import org.apache.sis.image.DataType;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Static;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.collection.WeakHashSet;


/**
 * Creates rasters from given properties. Contains also convenience methods for
 * creating {@link BufferedImage} since that kind of images wraps a single raster.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
public final class RasterFactory extends Static {
    /**
     * Shared instances of {@link SampleModel}s.
     *
     * @see #unique(SampleModel)
     */
    private static final WeakHashSet<SampleModel> POOL = new WeakHashSet<>(SampleModel.class);

    /**
     * Do not allow instantiation of this class.
     */
    private RasterFactory() {
    }

    /**
     * Creates an opaque image with a gray scale color model. The image can have an arbitrary
     * number of bands, but in current implementation only one band is used.
     *
     * <p><b>Warning:</b> displaying this image is very slow, except in a few special cases.
     * It should be used only when no standard color model can be used.</p>
     *
     * @param  dataType       the buffer type as one of {@code DataBuffer.TYPE_*} constants.
     * @param  width          the desired image width.
     * @param  height         the desired image height.
     * @param  numComponents  the number of components.
     * @param  visibleBand    the band to use for computing colors.
     * @param  minimum        the minimal sample value expected.
     * @param  maximum        the maximal sample value expected.
     * @return the color space for the given range of values.
     */
    public static BufferedImage createGrayScaleImage(final int dataType, final int width, final int height,
            final int numComponents, final int visibleBand, final double minimum, final double maximum)
    {
        switch (dataType) {
            case DataBuffer.TYPE_BYTE:
            case DataBuffer.TYPE_USHORT: {
                if (numComponents == 1 && ColorModelFactory.isStandardRange(dataType, minimum, maximum)) {
                    return new ObservableImage(width, height, (dataType == DataBuffer.TYPE_BYTE)
                                ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_USHORT_GRAY);
                }
                break;
            }
        }
        final ColorModel cm = ColorModelFactory.createGrayScale(dataType, numComponents, visibleBand, minimum, maximum);
        return new ObservableImage(cm, cm.createCompatibleWritableRaster(width, height), false, null);
    }

    /**
     * Wraps the given data buffer in a raster.
     * The sample model type is selected according the number of bands and the pixel stride.
     * The number of bands is determined by {@code bandOffsets.length}, which should be one of followings:
     *
     * <ul>
     *   <li>For banded sample model, all {@code bandOffsets} can be zero.</li>
     *   <li>For interleaved sample model ({@code buffer.getNumBanks()} = 1), each band needs a different offset.
     *       They may be 0, 1, 2, 3….</li>
     * </ul>
     *
     * @param  buffer          buffer that contains the sample values.
     * @param  width           raster width in pixels.
     * @param  height          raster height in pixels.
     * @param  pixelStride     number of data elements between two samples for the same band on the same line.
     * @param  scanlineStride  number of data elements between a given sample and the corresponding sample in the same column of the next line.
     * @param  bankIndices     bank indices for each band, or {@code null} for 0, 1, 2, 3….
     * @param  bandOffsets     number of data elements from the first element of the bank to the first sample of the band.
     * @param  location        the upper-left corner of the raster, or {@code null} for (0,0).
     * @return a raster built from given properties.
     * @throws NullPointerException if {@code buffer} is {@code null}.
     * @throws RasterFormatException if the width or height is less than or equal to zero, or if there is an integer overflow.
     *
     * @see WritableRaster#createInterleavedRaster(DataBuffer, int, int, int, int, int[], Point)
     * @see WritableRaster#createBandedRaster(DataBuffer, int, int, int, int[], int[], Point)
     */
    @SuppressWarnings("fallthrough")
    public static WritableRaster createRaster(final DataBuffer buffer,
            final int width, final int height, final int pixelStride, final int scanlineStride,
            int[] bankIndices, final int[] bandOffsets, final Point location)
    {
        /*
         * We do not verify the argument validity. Since this class is internal, caller should have done verification
         * itself. Furthermore, those arguments are verified by WritableRaster constructors anyway.
         */
        final int dataType = buffer.getDataType();
        /*
         * This SampleModel variable is a workaround for WritableRaster static methods not supporting all data types.
         * If `dataType` is unsupported, then we create a SampleModel ourselves in the `switch` statements below and
         * use it for creating a WritableRaster at the end of this method. This variable, together with the `switch`
         * statements, may be removed in a future SIS version if all types become supported by the JDK.
         */
        @Workaround(library = "JDK", version = "10")
        final SampleModel model;
        if (buffer.getNumBanks() == 1 && (bankIndices == null || bankIndices[0] == 0)) {
            /*
             * Sample data are stored for all bands in a single bank of the DataBuffer, in an interleaved fashion.
             * Each sample of a pixel occupies one data element of the DataBuffer, with a different offset since
             * the buffer beginning. The number of bands is inferred from bandOffsets.length.
             */
            switch (dataType) {
                case DataBuffer.TYPE_BYTE:
                case DataBuffer.TYPE_USHORT: {
                    // `scanlineStride` and `pixelStride` really interchanged in that method signature.
                    return WritableRaster.createInterleavedRaster(buffer, width, height, scanlineStride, pixelStride, bandOffsets, location);
                }
                case DataBuffer.TYPE_INT: {
                    if (bandOffsets.length == 1 && pixelStride == 1) {
                        /*
                         * From JDK javadoc: "To create a 1-band Raster of type TYPE_INT, use createPackedRaster()".
                         * However, this would require the creation of a PackedColorModel subclass. For SIS purposes,
                         * it is easier to create a banded sample model.
                         */
                        return WritableRaster.createBandedRaster(buffer, width, height, scanlineStride, new int[1], bandOffsets, location);
                    }
                    // else fallthrough.
                }
                default: {
                    model = new PixelInterleavedSampleModel(dataType, width, height, pixelStride, scanlineStride, bandOffsets);
                    break;
                }
            }
        } else {
            /*
             * Sample data are stored in different banks (arrays) for each band. If all pixels are consecutive (pixelStride = 1),
             * we have the classical banded sample model. Otherwise the type is not well identified; neither interleaved or banded.
             */
            if (bankIndices == null) {
                bankIndices = ArraysExt.range(0, bandOffsets.length);
            }
            if (pixelStride == 1) {
                switch (dataType) {
                    case DataBuffer.TYPE_BYTE:
                    case DataBuffer.TYPE_USHORT:
                    case DataBuffer.TYPE_INT: {
                        // This constructor supports only above-cited types.
                        return WritableRaster.createBandedRaster(buffer, width, height, scanlineStride, bankIndices, bandOffsets, location);
                    }
                    default: {
                        model = new BandedSampleModel(dataType, width, height, scanlineStride, bankIndices, bandOffsets);
                        break;
                    }
                }
            } else {
                model = new ComponentSampleModel(dataType, width, height, pixelStride, scanlineStride, bankIndices, bandOffsets);
            }
        }
        return WritableRaster.createWritableRaster(unique(model), buffer, location);
    }

    /**
     * Creates a NIO buffer of the specified capacity.
     * The buffer position will be 0 and its limit will be its capacity.
     *
     * @param  dataType  type of buffer to create.
     * @param  capacity  the {@code Buffer} size.
     * @return buffer of the specified type and size.
     */
    public static Buffer createBuffer(final DataType dataType, final int capacity) {
        switch (dataType) {
            case USHORT: // Fallthrough
            case SHORT:  return ShortBuffer .allocate(capacity);
            case BYTE:   return ByteBuffer  .allocate(capacity);
            case INT:    return IntBuffer   .allocate(capacity);
            case FLOAT:  return FloatBuffer .allocate(capacity);
            case DOUBLE: return DoubleBuffer.allocate(capacity);
            default: throw new AssertionError(dataType);
        }
    }

    /**
     * Creates a NIO buffer wrapping an existing Java2D buffer.
     * The buffer will be a view over the valid portion of the data array.
     * The buffer position is zero. The capacity and limit are the number of valid elements.
     *
     * @param  data  the Java2D buffer to wrap.
     * @param  bank  bank index of the data array to wrap.
     * @return buffer wrapping the data array of the specified bank.
     */
    public static Buffer wrapAsBuffer(final DataBuffer data, final int bank) {
        Buffer buffer;
        switch (data.getDataType()) {
            case DataBuffer.TYPE_BYTE:   buffer = ByteBuffer  .wrap(((DataBufferByte)   data).getData(bank)); break;
            case DataBuffer.TYPE_USHORT: buffer = ShortBuffer .wrap(((DataBufferUShort) data).getData(bank)); break;
            case DataBuffer.TYPE_SHORT:  buffer = ShortBuffer .wrap(((DataBufferShort)  data).getData(bank)); break;
            case DataBuffer.TYPE_INT:    buffer = IntBuffer   .wrap(((DataBufferInt)    data).getData(bank)); break;
            case DataBuffer.TYPE_FLOAT:  buffer = FloatBuffer .wrap(((DataBufferFloat)  data).getData(bank)); break;
            case DataBuffer.TYPE_DOUBLE: buffer = DoubleBuffer.wrap(((DataBufferDouble) data).getData(bank)); break;
            default: throw new IllegalArgumentException();
        }
        final int lower = data.getOffsets()[bank];
        final int upper = lower + data.getSize();
        if (lower != 0 || upper != buffer.capacity()) {
            buffer.position(lower).limit(upper).slice();        // TODO: use slice(lower, length) with JDK13.
        }
        return buffer;
    }

    /**
     * Wraps the backing arrays of given NIO buffers into Java2D buffers.
     * This method wraps the underlying array of primitive types; data are not copied.
     * For each buffer, the data starts at {@linkplain Buffer#position() buffer position}
     * and ends at {@linkplain Buffer#limit() limit}.
     *
     * @param  dataType  type of buffer to create.
     * @param  data      the data, one for each band.
     * @return buffer of the given type (never null).
     * @throws UnsupportedOperationException if a buffer is not backed by an accessible array.
     * @throws ReadOnlyBufferException if a buffer is backed by an array but is read-only.
     * @throws ArrayStoreException if the type of a backing array is not {@code dataType}.
     * @throws ArithmeticException if a buffer position overflows the 32 bits integer capacity.
     * @throws RasterFormatException if buffers do not have the same number of remaining values.
     */
    public static DataBuffer wrap(final DataType dataType, final Buffer... data) {
        final int numBands = data.length;
        final Object[] arrays;
        switch (dataType) {
            case USHORT: // fall through
            case SHORT:  arrays = new short [numBands][]; break;
            case INT:    arrays = new int   [numBands][]; break;
            case BYTE:   arrays = new byte  [numBands][]; break;
            case FLOAT:  arrays = new float [numBands][]; break;
            case DOUBLE: arrays = new double[numBands][]; break;
            default: throw new AssertionError(dataType);
        }
        final int[] offsets = new int[numBands];
        int length = 0;
        for (int i=0; i<numBands; i++) {
            final Buffer buffer = data[i];
            ArgumentChecks.ensureNonNullElement("data", i, buffer);
            arrays [i] = buffer.array();
            offsets[i] = Math.addExact(buffer.arrayOffset(), buffer.position());
            final int r = buffer.remaining();
            if (i == 0) length = r;
            else if (length != r) {
                throw new RasterFormatException(Resources.format(Resources.Keys.MismatchedBandSize));
            }
        }
        switch (dataType) {
            case BYTE:   return new DataBufferByte  (  (byte[][]) arrays, length, offsets);
            case SHORT:  return new DataBufferShort ( (short[][]) arrays, length, offsets);
            case USHORT: return new DataBufferUShort( (short[][]) arrays, length, offsets);
            case INT:    return new DataBufferInt   (   (int[][]) arrays, length, offsets);
            case FLOAT:  return new DataBufferFloat ( (float[][]) arrays, length, offsets);
            case DOUBLE: return new DataBufferDouble((double[][]) arrays, length, offsets);
            default: throw new AssertionError(dataType);
        }
    }

    /**
     * Returns a unique instance of the given sample model. This method can be invoked after a new sample
     * has been created in order to share the same instance for many similar {@code Raster} instances.
     *
     * @param  <T>          the type of the given {@code sampleModel}.
     * @param  sampleModel  the sample model to make unique.
     * @return a unique instance of the given sample model. May be {@code sampleModel} itself.
     */
    public static <T extends SampleModel> T unique(final T sampleModel) {
        return POOL.unique(sampleModel);
    }
}
