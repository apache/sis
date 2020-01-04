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
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.RasterFormatException;
import org.opengis.geometry.MismatchedDimensionException;
import org.apache.sis.coverage.SubspaceNotSpecifiedException;
import org.apache.sis.coverage.MismatchedCoverageRangeException;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.internal.coverage.j2d.ColorModelFactory;
import org.apache.sis.internal.coverage.j2d.RasterFactory;
import org.apache.sis.internal.feature.Resources;
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
 * <h2>Limitations</h2>
 * Current implementation constructs only images made of a single tile.
 * Support for tiled images will be added in a future version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @see GridCoverage#render(GridExtent)
 *
 * @since 1.0
 * @module
 */
public class ImageRenderer {
    /**
     * Location of the first image pixel relative to the grid coverage extent. The (0,0) offset means that the first pixel
     * in the {@code sliceExtent} (specified at construction time) is the first pixel in the whole {@link GridCoverage}.
     *
     * <div class="note"><b>Note:</b> if those offsets exceed 32 bits integer capacity, then it may not be possible to build
     * an image for given {@code sliceExtent} from a single {@link DataBuffer}, because accessing sample values would exceed
     * the capacity of index in Java arrays. In those cases the image needs to be tiled.</div>
     */
    private final long offsetX, offsetY;

    /**
     * Pixel coordinates of the image upper-left corner, as an offset relative to the {@code sliceExtent}.
     * This is initially zero (unless {@code sliceExtent} is partially outside the grid coverage extent),
     * but a different value may be used if the given data are tiled.
     *
     * @see RenderedImage#getMinX()
     * @see RenderedImage#getMinY()
     */
    private final int imageX, imageY;

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
     * This is the product of {@linkplain GridExtent#getSize(int) grid sizes} of enclosing {@code GridCoverage}
     * in all dimensions before the dimension of image {@linkplain #width}. This stride does <strong>not</strong>
     * include the multiplication factor for the number of bands in a <cite>pixel interleaved sample model</cite>
     * because whether this factor is needed or not depends on the data {@linkplain #buffer}, which is not known
     * at construction time.
     *
     * @see #strideFactor
     * @see java.awt.image.ComponentSampleModel#pixelStride
     */
    private final int pixelStride;

    /**
     * Number of data elements between a given sample and the corresponding sample in the same column of the next line.
     * This is the product of {@linkplain GridExtent#getSize(int) grid sizes} of enclosing {@code GridCoverage} in all
     * dimensions before the dimension of image {@linkplain #height}. This stride does <strong>not</strong> include the
     * multiplication factor for the number of bands in a <cite>pixel interleaved sample model</cite> because whether
     * this factor is needed or not depends on the data {@linkplain #buffer}, which is not known at construction time.
     *
     * @see #strideFactor
     * @see java.awt.image.ComponentSampleModel#scanlineStride
     */
    private final int scanlineStride;

    /**
     * Multiplication factor for {@link #pixelStride} and {@link #scanlineStride}. This is the number of data elements
     * between two samples in the data {@link #buffer}. There is no direct equivalent in {@link java.awt.image} because
     * <cite>pixel stride</cite> and <cite>scanline stride</cite> in {@link SampleModel} are pre-multiplied by this factor,
     * but we need to keep this information separated in this builder because its value depends on which methods are invoked:
     *
     * <ul>
     *   <li>If {@link #setInterleavedPixelOffsets(int, int[])} is invoked, this is the value given to that method.</li>
     *   <li>Otherwise if {@link #setData(DataBuffer)} is invoked and the given buffer has only
     *       {@linkplain DataBuffer#getNumBanks() one bank}, then this is {@link #getNumBands()}.</li>
     *   <li>Otherwise this is 1.</li>
     * </ul>
     *
     * @see #setInterleavedPixelOffsets(int, int[])
     */
    private int strideFactor;

    /**
     * The sample dimensions, to be used for defining the bands.
     */
    private final SampleDimension[] bands;

    /**
     * Offset to add to index of sample values in each band in order to reach the value in the {@link DataBuffer} bank.
     * This is closely related to {@link java.awt.image.ComponentSampleModel#bandOffsets} but not identical, because of
     * the following differences:
     *
     * <ul>
     *   <li>Another offset for {@link #offsetX} and {@link #offsetY} may need to be added
     *       before to give the {@code bandOffsets} to {@link SampleModel} constructor.</li>
     *   <li>If null, a default value is inferred depending on whether the {@link SampleModel}
     *       to construct is banded or interleaved.</li>
     * </ul>
     *
     * @see #setInterleavedPixelOffsets(int, int[])
     */
    private int[] bandOffsets;

    /**
     * Bank indices for each band, or {@code null} for 0, 1, 2, 3….
     * If non-null, this array length must be equal to {@link #bands} array length.
     */
    private int[] bankIndices;

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
     * Creates a new image renderer for the given slice extent.
     *
     * @param  coverage     the source coverage for which to build an image.
     * @param  sliceExtent  the domain from which to create an image, or {@code null} for the {@code coverage} extent.
     * @throws SubspaceNotSpecifiedException if this method can not infer a two-dimensional slice from {@code sliceExtent}.
     * @throws DisjointExtentException if the given extent does not intersect the given coverage.
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
        final int  xd   = dimensions[0];
        final int  yd   = dimensions[1];
        final long xcov = source.getLow(xd);
        final long ycov = source.getLow(yd);
        final long xreq = sliceExtent.getLow(xd);
        final long yreq = sliceExtent.getLow(yd);
        final long xmin = Math.max(xreq, xcov);
        final long ymin = Math.max(yreq, ycov);
        final long xmax = Math.min(sliceExtent.getHigh(xd), source.getHigh(xd));
        final long ymax = Math.min(sliceExtent.getHigh(yd), source.getHigh(yd));
        if (xmax < xmin || ymax < ymin) {                                           // max are inclusive.
            final int d = (xmax < xmin) ? xd : yd;
            throw new DisjointExtentException(source, sliceExtent, d);
        }
        width   = Math.incrementExact(Math.toIntExact(xmax - xmin));
        height  = Math.incrementExact(Math.toIntExact(ymax - ymin));
        imageX  = Math.toIntExact(Math.subtractExact(xreq, xmin));
        imageY  = Math.toIntExact(Math.subtractExact(yreq, ymin));
        offsetX = Math.subtractExact(xmin, xcov);
        offsetY = Math.subtractExact(ymin, ycov);
        /*
         * At this point, the RenderedImage properties have been computed on the assumption
         * that the returned image will be a single tile. Now compute SampleModel properties.
         */
        long pixelStride  = 1;
        for (int i=0; i<xd; i++) {
            pixelStride = Math.multiplyExact(pixelStride, source.getSize(i));
        }
        long scanlineStride = pixelStride;
        for (int i=xd; i<yd; i++) {
            scanlineStride = Math.multiplyExact(scanlineStride, source.getSize(i));
        }
        this.pixelStride    = Math.toIntExact(pixelStride);
        this.scanlineStride = Math.toIntExact(scanlineStride);
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
     * The given number shall be either 1 (case of interleaved sample model) or
     * {@link #getNumBands()} (case of banded sample model).
     */
    private void ensureExpectedBandCount(final int n, final boolean acceptOne) {
        if (!(n == 1 & acceptOne)) {
            final int e = getNumBands();
            if (n != e) {
                throw new MismatchedCoverageRangeException(Resources.format(Resources.Keys.UnexpectedNumberOfBands_2, e, n));
            }
        }
    }

    /**
     * Returns the location of the image upper-left corner together with the image size.
     *
     * @return the rendered image location and size (never null).
     */
    public final Rectangle getBounds() {
        return new Rectangle(imageX, imageY, width, height);
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
        ensureExpectedBandCount(data.getNumBanks(), true);
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
        ensureExpectedBandCount(data.length, true);
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
        ensureExpectedBandCount(data.length, true);
        final Buffer[] buffers = new Buffer[data.length];
        int dataType = DataBuffer.TYPE_UNDEFINED;
        for (int i=0; i<data.length; i++) {
            final Vector v = data[i];
            ArgumentChecks.ensureNonNullElement("data", i, v);
            final int t = RasterFactory.getDataType(v.getElementType(), v.isUnsigned());
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
     * Specifies the offsets to add to sample index in each band in order to reach the sample value in the {@link DataBuffer} bank.
     * This method should be invoked when the data given to {@code setData(…)} contains only one {@link Vector}, {@link Buffer} or
     * {@link DataBuffer} bank, and the bands in that unique bank are interleaved.
     *
     * <div class="note"><b>Example:</b>
     * for an image having three bands named Red (R), Green (G) and Blue (B), if the sample values are stored in a single bank in a
     * R₀,G₀,B₀, R₁,G₁,B₁, R₂,G₂,B₂, R₃,G₃,B₃, <i>etc.</i> fashion, then this method should be invoked as below:
     *
     * {@preformat java
     *     setInterleavedPixelOffsets(3, new int[] {0, 1, 2});
     * }
     * </div>
     *
     * @param  pixelStride  the number of data elements between each pixel in the data vector or buffer.
     * @param  bandOffsets  offsets to add to sample index in each band. This is typically {0, 1, 2, …}.
     *                      The length of this array shall be equal to {@link #getNumBands()}.
     */
    public void setInterleavedPixelOffsets(final int pixelStride, final int[] bandOffsets) {
        ArgumentChecks.ensureStrictlyPositive("pixelStride", pixelStride);
        ArgumentChecks.ensureNonNull("bandOffsets", bandOffsets);
        ensureExpectedBandCount(bandOffsets.length, false);
        this.strideFactor = pixelStride;
        this.bandOffsets = bandOffsets.clone();
    }

    /**
     * Creates a raster with the data specified by the last call to a {@code setData(…)} method.
     * The raster upper-left corner is located at the position given by {@link #getBounds()}.
     *
     * @return the raster.
     * @throws IllegalStateException if no {@code setData(…)} method has been invoked before this method call.
     * @throws RasterFormatException if a call to a {@link WritableRaster} factory method failed.
     * @throws ArithmeticException if a property of the raster to construct exceeds the capacity of 32 bits integers.
     */
    public WritableRaster raster() {
        if (buffer == null) {
            throw new IllegalStateException(Resources.format(Resources.Keys.UnspecifiedRasterData));
        }
        final boolean isInterleaved = (buffer.getNumBanks() == 1);
        if (bandOffsets == null) {
            strideFactor = isInterleaved ? getNumBands() : 1;
        }
        final int ls = Math.multiplyExact(scanlineStride, strideFactor);    // Real scanline stride.
        final int ps = pixelStride * strideFactor;                          // Can not fail if above operation did not fail.
        /*
         * Number of data elements from the first element of the bank to the first sample of the band.
         * This is usually 0 for all bands, unless the upper-left corner (minX, minY) is not (0,0).
         */
        final int[] offsets = new int[getNumBands()];
        Arrays.fill(offsets, Math.toIntExact(Math.addExact(
                Math.multiplyExact(offsetX, ps),
                Math.multiplyExact(offsetY, ls))));
        /*
         * Add the offset specified by the user (if any), or the default offset. The default is 0, 1, 2…
         * for interleaved sample model (all bands in one bank) and 0, 0, 0… for banded sample model.
         */
        if (bandOffsets != null) {
            for (int i=0; i<offsets.length; i++) {
                offsets[i] = Math.addExact(offsets[i], bandOffsets[i]);
            }
        } else if (isInterleaved) {
            for (int i=1; i<offsets.length; i++) {
                offsets[i] = Math.addExact(offsets[i], i);
            }
        }
        final Point location = new Point(imageX, imageY);
        return RasterFactory.createRaster(buffer, width, height, ps, ls, bankIndices, offsets, location);
    }

    /**
     * Creates an image with the data specified by the last call to a {@code setData(…)} method.
     * The image upper-left corner is located at the position given by {@link #getBounds()}.
     *
     * @return the image.
     * @throws IllegalStateException if no {@code setData(…)} method has been invoked before this method call.
     * @throws RasterFormatException if a call to a {@link WritableRaster} factory method failed.
     * @throws ArithmeticException if a property of the image to construct exceeds the capacity of 32 bits integers.
     */
    public RenderedImage image() {
        WritableRaster raster = raster();
        ColorModel colors = ColorModelFactory.createColorModel(bands, visibleBand, buffer.getDataType(), ColorModelFactory.GRAYSCALE);
        return new BufferedImage(colors, raster, false, null);
    }
}
