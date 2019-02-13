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
package org.apache.sis.coverage.grid;

import java.util.Arrays;
import java.nio.Buffer;
import java.awt.Point;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.RasterFormatException;
import org.opengis.geometry.MismatchedDimensionException;
import org.apache.sis.coverage.SubspaceNotSpecifiedException;
import org.apache.sis.coverage.MismatchedCoverageRangeException;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.internal.raster.ColorModelFactory;
import org.apache.sis.internal.raster.RasterFactory;
import org.apache.sis.internal.raster.Resources;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.util.NullArgumentException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.math.Vector;


/**
 * A builder for the rendered image to be returned by {@link GridCoverage#render(GridExtent)}.
 * This builder does not copy any sample values. Instead, it wraps existing data arrays into
 * {@link java.awt.image.Raster} objects by computing required information such as
 * {@linkplain java.awt.image.ComponentSampleModel#getPixelStride() pixel stride},
 * {@linkplain java.awt.image.ComponentSampleModel#getScanlineStride() scanline stride} and
 * {@linkplain java.awt.image.ComponentSampleModel#getBandOffsets() band offsets}.
 * Different {@code setData(…)} methods are provided for allowing to specify the data arrays
 * from different objects such as Java2D {@link DataBuffer} or NIO {@link Buffer}.
 *
 * <p>All {@code setData(…)} methods assume that the first valid element in each array is the value
 * located at <code>{@linkplain GridCoverage#getGridGeometry()}.{@linkplain GridGeometry#getExtent()
 * getExtent()}.{@linkplain GridExtent#getLow(int) getLow()}</code>. This {@code ImageRenderer} class
 * computes automatically the offsets from that position to the position of the first value included
 * in the {@code sliceExtent} given to the constructor.</p>
 *
 * <div class="note"><b>Usage example:</b>
 * {@preformat java
 *     class MyResource extends GridCoverage {
 *         &#64;Override
 *         public RenderedImage render(GridExtent sliceExtent) {
 *             try {
 *                 ImageRenderer renderer = new ImageRenderer(this, sliceExtent);
 *                 renderer.setData(data);
 *                 return renderer.image();
 *             } catch (IllegalArgumentException | ArithmeticException | RasterFormatException e) {
 *                 throw new CannotEvaluateException("Can not create an image.", e);
 *             }
 *         }
 *     }
 * }
 * </div>
 *
 * <div class="section">Limitations</div>
 * Current implementation constructs only images made of a single tile.
 * Support for tiled images will be added in a future version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @see GridCoverage#render(GridExtent)
 *
 * @since 1.0
 * @module
 */
public class ImageRenderer {
    /**
     * Pixel coordinates of the image upper-left location.
     * This is often left to zero since {@link BufferedImage} has this constraint.
     *
     * @see #setLocation(int, int)
     */
    private int x, y;

    /**
     * Width (number of pixels in a row) of the image to render.
     * This is usually set to the grid extent along the first dimension
     * having a {@linkplain GridExtent#getSize(int) size} greater than 1.
     *
     * @see RenderedImage#getWidth()
     */
    private final int width;

    /**
     * Height (number of pixels in a column) of the image to render.
     * This is usually set to the grid extent along the second dimension
     * having a {@linkplain GridExtent#getSize(int) size} greater than 1.
     *
     * @see RenderedImage#getHeight()
     */
    private final int height;

    /**
     * Number of data elements between two samples for the same band on the same line.
     * This is set to the product of {@linkplain GridExtent#getSize(int) grid sizes} of enclosing
     * {@code GridCoverage} in all dimensions before the dimension of image {@linkplain #width}.
     *
     * @see java.awt.image.ComponentSampleModel#pixelStride
     */
    private final int pixelStride;

    /**
     * Number of data elements between a given sample and the corresponding sample in the same column of the next line.
     * This is set to the product of {@linkplain GridExtent#getSize(int) grid sizes} of enclosing {@code GridCoverage}
     * in all dimensions before the dimension of image {@linkplain #height}.
     *
     * @see java.awt.image.ComponentSampleModel#scanlineStride
     */
    private final int scanlineStride;

    /**
     * The sample dimensions, to be used for defining the bands.
     */
    private final SampleDimension[] bands;

    /**
     * Bank indices for each band, or {@code null} for 0, 1, 2, 3….
     * If non-null, this array length must be equal to {@link #bands} array length.
     */
    private int[] bankIndices;

    /**
     * Number of data elements from the first element of the bank to the first sample of the band, or {@code null} for all 0.
     * If non-null, this array length must be equal to {@link #bands} array length.
     */
    private final int[] bandOffsets;

    /**
     * The band to be made visible (usually 0). All other bands, if any will be ignored.
     */
    private int visibleBand;

    /**
     * The data to render, or {@code null} if not yet specified.
     * If non-null, {@link DataBuffer#getNumBanks()} must be equal to {@link #bands} array length.
     */
    private DataBuffer buffer;

    /**
     * Creates a new image renderer for the given slice extent. The image will have only one tile.
     *
     * @param  coverage     the grid coverage for which to build an image.
     * @param  sliceExtent  the grid geometry from which to create an image, or {@code null} for the {@code coverage} extent.
     * @throws SubspaceNotSpecifiedException if this method can not infer a two-dimensional slice from {@code sliceExtent}.
     * @throws ArithmeticException if a stride calculation overflows the 32 bits integer capacity.
     */
    public ImageRenderer(final GridCoverage coverage, GridExtent sliceExtent) {
        ArgumentChecks.ensureNonNull("coverage", coverage);
        bands = CollectionsExt.toArray(coverage.getSampleDimensions(), SampleDimension.class);
        final GridExtent source = coverage.getGridGeometry().getExtent();
        if (sliceExtent != null) {
            final int dimension = sliceExtent.getDimension();
            if (source.getDimension() != dimension) {
                throw new MismatchedDimensionException(Errors.format(
                        Errors.Keys.MismatchedDimension_3, "target", source.getDimension(), dimension));
            }
        } else {
            sliceExtent = source;
        }
        final int[] dimensions = sliceExtent.getSubspaceDimensions(2);
        int  xd = dimensions[0];
        int  yd = dimensions[1];
        long xo = sliceExtent.getLow(xd);
        long yo = sliceExtent.getLow(yd);
        width  = Math.toIntExact(sliceExtent.getSize(xd));
        height = Math.toIntExact(sliceExtent.getSize(yd));
        /*
         * After this point, xd and yd should be indices relative to source extent.
         * For now we keep them unchanged on the assumption that the two grid extents have the same dimensions.
         */
        xo = Math.subtractExact(xo, source.getLow(xd));
        yo = Math.subtractExact(yo, source.getLow(yd));
        long pixelStride = 1;
        for (int i=0; i<xd; i++) {
            pixelStride = Math.multiplyExact(pixelStride, source.getSize(i));
        }
        long scanlineStride = pixelStride;
        for (int i=xd; i<yd; i++) {
            scanlineStride = Math.multiplyExact(scanlineStride, source.getSize(i));
        }
        this.pixelStride    = Math.toIntExact(pixelStride);
        this.scanlineStride = Math.toIntExact(scanlineStride);
        this.bandOffsets    = new int[getNumBands()];
        Arrays.fill(bandOffsets, Math.toIntExact(xo + Math.multiplyExact(yo, scanlineStride)));
    }

    /**
     * Returns the number of bands that the image will have. By default, this is the number of
     * {@linkplain GridCoverage#getSampleDimensions() sample dimensions} in the grid coverage.
     *
     * @return the number of bands in the rendered image.
     */
    public final int getNumBands() {
        return bands.length;
    }

    /**
     * Ensures that the given number is equals to the expected number of bands.
     */
    private void ensureExpectedBandCount(final int n) {
        final int e = getNumBands();
        if (n != e) {
            throw new MismatchedCoverageRangeException(Resources.format(Resources.Keys.UnexpectedNumberOfBands_2, e, n));
        }
    }

    /**
     * Sets the pixel coordinates of the upper-left corner of the rendered image to create.
     * If this method is not invoked, then the default value is (0,0).
     * That default value is often suitable since {@link BufferedImage} constraints that corner to (0,0).
     *
     * @param  x  the minimum <var>x</var> coordinate (inclusive) of the rendered image.
     * @param  y  the minimum <var>y</var> coordinate (inclusive) of the rendered image.
     *
     * @see RenderedImage#getMinX()
     * @see RenderedImage#getMinY()
     */
    public void setLocation(final int x, final int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Returns the location of the image upper-left corner.
     * This is the last value set by a call to {@link #setLocation(int, int)}.
     * The default value is (0,0).
     *
     * @return the image location (never null).
     */
    public final Point getLocation() {
        return new Point(x,y);
    }

    /**
     * Sets the data as a Java2D buffer. The {@linkplain DataBuffer#getNumBanks() number of banks}
     * in the given buffer must be equal to the {@linkplain #getNumBands() expected number of bands}.
     * In each bank, the value located at the {@linkplain DataBuffer#getOffsets() bank offset} is the value
     * located at <code>{@linkplain GridCoverage#getGridGeometry()}.{@linkplain GridGeometry#getExtent()
     * getExtent()}.{@linkplain GridExtent#getLow(int) getLow()}</code>, as specified in class javadoc.
     *
     * @param  data  the Java2D buffer containing data for all bands.
     * @throws NullArgumentException if {@code data} is null.
     * @throws MismatchedCoverageRangeException if the given data buffer does not have the expected amount of banks.
     */
    public void setData(final DataBuffer data) {
        ArgumentChecks.ensureNonNull("data", data);
        ensureExpectedBandCount(data.getNumBanks());
        buffer = data;
    }

    /**
     * Sets the data as NIO buffers. The number of buffers must be equal to the {@linkplain #getNumBands() expected
     * number of bands}. All buffers must be {@linkplain Buffer#array() backed by arrays} of the type specified by
     * the {@code dataType} argument and have the same amount of {@linkplain Buffer#remaining() remaining elements}.
     * This method wraps the underlying arrays of a primitive type into a Java2D buffer; data are not copied.
     * For each buffer, the grid coverage data (not only the slice data) starts at {@linkplain Buffer#position()
     * buffer position} and ends at that position + {@linkplain Buffer#remaining() remaining}.
     *
     * <p>The data type must be specified in order to distinguish between the signed and unsigned types.
     * {@link DataBuffer#TYPE_BYTE} and {@link DataBuffer#TYPE_USHORT} are unsigned, all other supported
     * types are signed.</p>
     *
     * <p><b>Implementation note:</b> the Java2D buffer is set by a call to {@link #setData(DataBuffer)},
     * which can be overridden by subclasses if desired.</p>
     *
     * @param  dataType  type of data as one of {@link DataBuffer#TYPE_BYTE}, {@link DataBuffer#TYPE_SHORT TYPE_SHORT}
     *         {@link DataBuffer#TYPE_USHORT TYPE_USHORT}, {@link DataBuffer#TYPE_INT TYPE_INT},
     *         {@link DataBuffer#TYPE_FLOAT TYPE_FLOAT} or {@link DataBuffer#TYPE_DOUBLE TYPE_DOUBLE} constants.
     * @param  data  the buffers wrapping arrays of primitive type.
     * @throws NullArgumentException if {@code data} is null or one of {@code data} element is null.
     * @throws IllegalArgumentException if {@code dataType} is not a supported value.
     * @throws MismatchedCoverageRangeException if the number of specified buffers is not equal to the number of bands.
     * @throws UnsupportedOperationException if a buffer is not backed by an accessible array or is read-only.
     * @throws ArrayStoreException if a buffer type is incompatible with {@code dataType}.
     * @throws RasterFormatException if buffers do not have the same amount of remaining values.
     * @throws ArithmeticException if a buffer position overflows the 32 bits integer capacity.
     */
    public void setData(final int dataType, final Buffer... data) {
        ArgumentChecks.ensureNonNull("data", data);
        ensureExpectedBandCount(data.length);
        final DataBuffer banks = RasterFactory.wrap(dataType, data);
        if (banks == null) {
            throw new IllegalArgumentException(Resources.format(Resources.Keys.UnknownDataType_1, dataType));
        }
        setData(banks);
    }

    /**
     * Sets the data as vectors. The number of vectors must be equal to the {@linkplain #getNumBands() expected number of bands}.
     * All vectors must be backed by arrays (indirectly, through {@linkplain Vector#buffer() buffers} backed by arrays) and have
     * the same {@linkplain Vector#size() size}.
     * This method wraps the underlying arrays of a primitive type into a Java2D buffer; data are not copied.
     *
     * <p><b>Implementation note:</b> the NIO buffers are set by a call to {@link #setData(int, Buffer...)},
     * which can be overridden by subclasses if desired.</p>
     *
     * @param  data  the vectors wrapping arrays of primitive type.
     * @throws NullArgumentException if {@code data} is null or one of {@code data} element is null.
     * @throws MismatchedCoverageRangeException if the number of specified vectors is not equal to the number of bands.
     * @throws UnsupportedOperationException if a vector is not backed by an accessible array or is read-only.
     * @throws RasterFormatException if vectors do not have the same size.
     * @throws ArithmeticException if a buffer position overflows the 32 bits integer capacity.
     */
    public void setData(final Vector... data) {
        ArgumentChecks.ensureNonNull("data", data);
        ensureExpectedBandCount(data.length);
        final Buffer[] buffers = new Buffer[data.length];
        int dataType = DataBuffer.TYPE_UNDEFINED;
        for (int i=0; i<data.length; i++) {
            final Vector v = data[i];
            ArgumentChecks.ensureNonNullElement("data", i, v);
            final int t = RasterFactory.getType(v.getElementType(), v.isUnsigned());
            if (dataType != t) {
                if (i != 0) {
                    throw new RasterFormatException(Resources.format(Resources.Keys.MismatchedDataType));
                }
                dataType = t;
            }
            buffers[i] = v.buffer().orElseThrow(UnsupportedOperationException::new);
        }
        setData(dataType, buffers);
    }

    /**
     * Creates a raster with the data specified by the last call to a {@code setData(…)} method.
     * The raster upper-left corner is located at the position given by {@link #getLocation()}.
     *
     * @return the raster.
     * @throws IllegalStateException if no {@code setData(…)} method has been invoked before this method call.
     * @throws RasterFormatException if a call to a {@link WritableRaster} factory method failed.
     */
    public WritableRaster raster() {
        if (buffer == null) {
            throw new IllegalStateException(Resources.format(Resources.Keys.UnspecifiedRasterData));
        }
        final Point location = ((x | y) != 0) ? new Point(x,y) : null;
        return RasterFactory.createRaster(buffer, width, height, pixelStride, scanlineStride, bankIndices, bandOffsets, location);
    }

    /**
     * Creates an image with the data specified by the last call to a {@code setData(…)} method.
     * The image upper-left corner is located at the position given by {@link #getLocation()}.
     *
     * @return the image.
     * @throws IllegalStateException if no {@code setData(…)} method has been invoked before this method call.
     * @throws RasterFormatException if a call to a {@link WritableRaster} factory method failed.
     */
    public RenderedImage image() {
        WritableRaster raster = raster();
        ColorModel colors = ColorModelFactory.createColorModel(bands, visibleBand, buffer.getDataType(), ColorModelFactory.GRAYSCALE);
        return new BufferedImage(colors, raster, false, null);
    }
}
