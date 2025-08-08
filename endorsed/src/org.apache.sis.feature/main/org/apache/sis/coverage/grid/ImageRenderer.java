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
import java.util.Hashtable;
import java.util.Objects;
import java.util.function.Function;
import java.nio.Buffer;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.ImagingOpException;
import java.awt.image.RasterFormatException;
import java.awt.image.Raster;
import static java.lang.Math.addExact;
import static java.lang.Math.subtractExact;
import static java.lang.Math.multiplyExact;
import static java.lang.Math.incrementExact;
import static java.lang.Math.toIntExact;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.image.DataType;
import org.apache.sis.coverage.SubspaceNotSpecifiedException;
import org.apache.sis.coverage.MismatchedCoverageRangeException;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.Category;
import org.apache.sis.image.privy.ColorScaleBuilder;
import org.apache.sis.image.privy.DeferredProperty;
import org.apache.sis.image.privy.RasterFactory;
import org.apache.sis.image.privy.ObservableImage;
import org.apache.sis.image.privy.TiledImage;
import org.apache.sis.image.privy.WritableTiledImage;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.math.Vector;
import static org.apache.sis.image.PlanarImage.XY_DIMENSIONS_KEY;
import static org.apache.sis.image.PlanarImage.GRID_GEOMETRY_KEY;
import static org.apache.sis.image.PlanarImage.SAMPLE_DIMENSIONS_KEY;

// Specific to the geoapi-4.0 branch:
import org.opengis.coordinate.MismatchedDimensionException;


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
 * <h2>Usage example</h2>
 * {@snippet lang="java" :
 *     class MyResource extends GridCoverage {
 *         @Override
 *         public RenderedImage render(GridExtent sliceExtent) {
 *             ImageRenderer renderer = new ImageRenderer(this, sliceExtent);
 *             try {
 *                 renderer.setData(data);
 *                 return renderer.createImage();
 *             } catch (IllegalArgumentException | ArithmeticException | RasterFormatException e) {
 *                 throw new CannotEvaluateException("Cannot create an image.", e);
 *             }
 *         }
 *     }
 *     }
 *
 * <h2>Limitations</h2>
 * Current implementation constructs only images made of a single tile.
 * Support for tiled images will be added in a future version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 *
 * @see GridCoverage#render(GridExtent)
 *
 * @since 1.0
 */
public class ImageRenderer {
    /**
     * The grid geometry of the {@link GridCoverage} specified at construction time.
     * Never {@code null}.
     */
    private final GridGeometry geometry;

    /**
     * The requested slice, or {@code null} if unspecified.
     * If unspecified, then the extent to use is the full coverage grid extent.
     */
    private final GridExtent sliceExtent;

    /**
     * The dimensions to select in the grid coverage for producing an image. This is an array of length
     * {@value GridCoverage2D#BIDIMENSIONAL} obtained by {@link GridExtent#getSubspaceDimensions(int)}.
     * The array content is almost always {0,1}, but this class should work with other dimensions too.
     *
     * @see #getXYDimensions()
     */
    private final int[] gridDimensions;

    /**
     * The result of {@link #getImageGeometry(int)} if the specified number of dimension 2.
     * This is cached for avoiding to recompute this geometry if asked many times.
     *
     * @see #getImageGeometry(int)
     */
    private GridGeometry imageGeometry;

    /**
     * Offset to add to {@link #buffer} offset for reaching the first sample value for the slice to render.
     * This is zero for a two-dimensional image, but may be greater for cube having more dimensions.
     * Despite the "Z" letter in the field name, this field actually combines the offset for <em>all</em>
     * dimensions other than X and Y.
     */
    private final long offsetZ;

    /**
     * Location of the first image pixel relative to the grid coverage extent. The (0,0) offset means that the first pixel
     * in the {@code sliceExtent} (specified at construction time) is the first pixel in the whole {@link GridCoverage}.
     *
     * <h4>Implementation note</h4>
     * If those offsets exceed 32 bits integer capacity, then it may not be possible to build an image
     * for given {@code sliceExtent} from a single {@link DataBuffer}, because accessing sample values
     * would exceed the capacity of index in Java arrays. In those cases the image needs to be tiled.
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
     * include the multiplication factor for the number of bands in a <i>pixel interleaved sample model</i>
     * because whether this factor is needed or not depends on the data {@linkplain #buffer},
     * which is not known at construction time.
     *
     * @see #strideFactor
     * @see java.awt.image.ComponentSampleModel#pixelStride
     */
    private final int pixelStride;

    /**
     * Number of data elements between a given sample and the corresponding sample in the same column of the next line.
     * This is the product of {@linkplain GridExtent#getSize(int) grid sizes} of enclosing {@code GridCoverage} in all
     * dimensions before the dimension of image {@linkplain #height}. This stride does <strong>not</strong> include the
     * multiplication factor for the number of bands in a <i>pixel interleaved sample model</i> because whether this
     * factor is needed or not depends on the data {@linkplain #buffer}, which is not known at construction time.
     *
     * @see #strideFactor
     * @see java.awt.image.ComponentSampleModel#scanlineStride
     */
    private final int scanlineStride;

    /**
     * Multiplication factor for {@link #pixelStride} and {@link #scanlineStride}. This is the number of data elements
     * between two samples in the data {@link #buffer}. There is no direct equivalent in {@link java.awt.image} because
     * <var>pixel stride</var> and <var>scanline stride</var> in {@link SampleModel} are pre-multiplied by this factor,
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
     * The band to use for defining pixel colors when the image is displayed on screen.
     * All other bands, if any, will exist in the raster but be ignored at display time.
     *
     * @see #setVisibleBand(int)
     */
    private int visibleBand;

    /**
     * The data to render, or {@code null} if not yet specified.
     * If non-null, {@link DataBuffer#getNumBanks()} must be equal to {@link #bands} array length.
     */
    private DataBuffer buffer;

    /**
     * The colors to use for each category. Never {@code null}.
     * The function may return {@code null}, which means transparent.
     * The default value is {@link ColorScaleBuilder#GRAYSCALE}.
     *
     * @see #setCategoryColors(Function)
     */
    private Function<Category,Color[]> colors;

    /**
     * The properties to give to the image, or {@code null} if none.
     *
     * @see #addProperty(String, Object)
     */
    @SuppressWarnings("UseOfObsoleteCollectionType")
    private Hashtable<String,Object> properties;

    /**
     * The factory to use for {@link org.opengis.referencing.operation.MathTransform} creations,
     * or {@code null} for a default factory.
     *
     * <p>For now this is fixed to {@code null}. But it may become a non-static, non-final field
     * in a future version if we want to make this property configurable.</p>
     */
    private static final MathTransformFactory mtFactory = null;

    /**
     * Creates a new image renderer for the given slice extent.
     *
     * @param  coverage     the source coverage for which to build an image.
     * @param  sliceExtent  the domain from which to create an image, or {@code null} for the {@code coverage} extent.
     * @throws SubspaceNotSpecifiedException if this method cannot infer a two-dimensional slice from {@code sliceExtent}.
     * @throws DisjointExtentException if the given extent does not intersect the given coverage.
     * @throws ArithmeticException if a stride calculation overflows the 32 bits integer capacity.
     */
    @SuppressWarnings("LocalVariableHidesMemberVariable")
    public ImageRenderer(final GridCoverage coverage, GridExtent sliceExtent) {
        bands = coverage.getSampleDimensions().toArray(SampleDimension[]::new);
        geometry = coverage.getGridGeometry();
        final GridExtent source = geometry.getExtent();
        final int dimension = source.getDimension();
        this.sliceExtent = sliceExtent;
        if (sliceExtent != null) {
            if (sliceExtent.getDimension() != dimension) {
                throw new MismatchedDimensionException(Errors.format(
                        Errors.Keys.MismatchedDimension_3, "sliceExtent", dimension, sliceExtent.getDimension()));
            }
        } else {
            sliceExtent = source;
        }
        gridDimensions  = sliceExtent.getSubspaceDimensions(GridCoverage2D.BIDIMENSIONAL);
        final int  xd   = gridDimensions[0];
        final int  yd   = gridDimensions[1];
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
        width   = incrementExact(toIntExact(xmax - xmin));
        height  = incrementExact(toIntExact(ymax - ymin));
        imageX  = toIntExact(subtractExact(xmin, xreq));
        imageY  = toIntExact(subtractExact(ymin, yreq));
        offsetX = subtractExact(xmin, xcov);
        offsetY = subtractExact(ymin, ycov);
        /*
         * At this point, the RenderedImage properties have been computed as if the image was a single tile.
         * Now compute `SampleModel` properties (the strides). Current version still assumes a single tile,
         * but it could be changed in the future if we want to add tiling support. The following loop also
         * computes a "global" offset to add for reachining the beginning of the slice if we are rendering
         * a slice in a three-dimensional (or more) cube.
         */
        long stride         = 1;
        long pixelStride    = 0;
        long scanlineStride = 0;
        long offsetZ        = 0;
        for (int i=0; i<dimension; i++) {
            if (i == xd) {
                pixelStride = stride;
            } else if (i == yd) {
                scanlineStride = stride;
            } else {
                final long min = source.getLow(i);
                final long c = sliceExtent.getLow(i);
                if (c > min) {
                    offsetZ = addExact(offsetZ, multiplyExact(stride, c - min));
                }
            }
            stride = multiplyExact(stride, source.getSize(i));
        }
        this.pixelStride    = toIntExact(pixelStride);
        this.scanlineStride = toIntExact(scanlineStride);
        this.offsetZ        = offsetZ;
        this.colors         = ColorScaleBuilder.GRAYSCALE;
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
     * Ensures that the given number is equal to the expected number of bands.
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
     * Returns the location of the image upper-left corner together with the image size. The image coordinate system
     * is relative to the {@code sliceExtent} specified at construction time: the (0,0) pixel coordinates correspond
     * to the {@code sliceExtent} {@linkplain GridExtent#getLow(int) low coordinates}. Consequently, the rectangle
     * {@linkplain Rectangle#x <var>x</var>} and {@linkplain Rectangle#y <var>y</var>} coordinates are (0,0) if
     * the image is located exactly in the area requested by {@code sliceExtent}, or is shifted as below otherwise:
     *
     * <blockquote>( <var>x</var>, <var>y</var> ) =
     * (grid coordinates of actually provided region) − (grid coordinates of requested region)</blockquote>
     *
     * @return the rendered image location and size (never null).
     */
    public final Rectangle getBounds() {
        return new Rectangle(imageX, imageY, width, height);
    }

    /**
     * The dimensions to select in the grid coverage for producing an image. This is the array obtained
     * by <code>{@link GridExtent#getSubspaceDimensions(int) GridExtent.getSubspaceDimensions(2)}</code>.
     * The array content is almost always {0,1}, i.e. the 2 first dimensions in a coordinate tuple.
     *
     * @return indices of <var>x</var> and <var>y</var> coordinate values in a grid coordinate tuple.
     *
     * @see org.apache.sis.image.PlanarImage#XY_DIMENSIONS_KEY
     *
     * @since 1.3
     */
    public final int[] getXYDimensions() {
        return gridDimensions.clone();
    }

    /**
     * Computes the conversion from pixel coordinates to CRS, together with the geospatial envelope of the image.
     * The {@link GridGeometry} returned by this method is derived from the {@linkplain GridCoverage#getGridGeometry()
     * coverage grid geometry} with the following changes:
     *
     * <ul>
     *   <li>The {@linkplain GridGeometry#getDimension() number of grid dimensions} is always 2.</li>
     *   <li>The number of {@linkplain GridGeometry#getCoordinateReferenceSystem() CRS} dimensions
     *       is specified by {@code dimCRS} (usually 2).</li>
     *   <li>The {@linkplain GridGeometry#getEnvelope() envelope} may be a sub-region of the coverage envelope.</li>
     *   <li>The {@linkplain GridGeometry#getExtent() grid extent} is the {@linkplain #getBounds() image bounds}.</li>
     *   <li>The {@linkplain GridGeometry#getGridToCRS grid to CRS} transform is derived from the coverage transform
     *       with a translation for mapping the {@code sliceExtent} {@linkplain GridExtent#getLow(int) low coordinates}
     *       to (0,0) pixel coordinates.</li>
     * </ul>
     *
     * @param  dimCRS  desired number of dimensions in the CRS. This is usually 2.
     * @return conversion from pixel coordinates to CRS of the given number of dimensions,
     *         together with image bounds and geospatial envelope if possible.
     *
     * @see org.apache.sis.image.PlanarImage#GRID_GEOMETRY_KEY
     *
     * @since 1.1
     */
    public GridGeometry getImageGeometry(final int dimCRS) {
        GridGeometry ig = imageGeometry;
        if (ig == null || dimCRS != GridCoverage2D.BIDIMENSIONAL) {
            if (imageUseSameGeometry(dimCRS)) {
                ig = geometry;
            } else try {
                ig = new SliceGeometry(geometry, sliceExtent, gridDimensions, mtFactory)
                        .reduce(new GridExtent(imageX, imageY, width, height), dimCRS);
            } catch (FactoryException e) {
                throw SliceGeometry.canNotCompute(e);
            }
            if (dimCRS == GridCoverage2D.BIDIMENSIONAL) {
                imageGeometry = ig;
            }
        }
        return ig;
    }

    /**
     * Returns the value associated to the given property.
     * The properties recognized by current implementation are:
     *
     * <ul>
     *   <li>{@value org.apache.sis.image.PlanarImage#XY_DIMENSIONS_KEY}.</li>
     *   <li>{@value org.apache.sis.image.PlanarImage#GRID_GEOMETRY_KEY}.</li>
     *   <li>{@value org.apache.sis.image.PlanarImage#SAMPLE_DIMENSIONS_KEY}.</li>
     *   <li>Any property added by calls to {@link #addProperty(String, Object)}.</li>
     * </ul>
     *
     * @param  key  the property for which to get a value.
     * @return value associated to the given property, or {@code null} if none.
     *
     * @since 1.1
     */
    public Object getProperty(final String key) {
        switch (key) {
            case XY_DIMENSIONS_KEY:     return getXYDimensions();
            case GRID_GEOMETRY_KEY:     return getImageGeometry(GridCoverage2D.BIDIMENSIONAL);
            case SAMPLE_DIMENSIONS_KEY: return bands.clone();
        }
        return (properties != null) ? properties.get(key) : null;
    }

    /**
     * Adds a value associated to a property. This method can be invoked only once for each {@code key}.
     * Those properties will be given to the image created by the {@link #createImage()} method.
     *
     * @param  key    key of the property to set.
     * @param  value  value to associate to the given key.
     * @throws IllegalArgumentException if a value is already associated to the given key.
     *
     * @since 1.1
     */
    @SuppressWarnings("UseOfObsoleteCollectionType")
    public void addProperty(final String key, final Object value) {
        ArgumentChecks.ensureNonNull("key",   key);
        ArgumentChecks.ensureNonNull("value", value);
        switch (key) {
            case XY_DIMENSIONS_KEY:
            case GRID_GEOMETRY_KEY:
            case SAMPLE_DIMENSIONS_KEY: break;
            default: {
                if (properties == null) {
                    properties = new Hashtable<>();
                }
                if (properties.putIfAbsent(key, value) == null) {
                    return;
                }
            }
        }
        throw new IllegalArgumentException(Errors.format(Errors.Keys.ElementAlreadyPresent_1, key));
    }

    /**
     * Returns {@code true} if a {@link #getImageGeometry(int)} request for the given number of CRS dimensions
     * can return {@link #geometry} directly. This common case avoids the need for more costly computation with
     * {@link SliceGeometry}.
     */
    private boolean imageUseSameGeometry(final int dimCRS) {
        final int tgtDim = geometry.getTargetDimension();
        ArgumentChecks.ensureBetween("dimCRS", GridCoverage2D.BIDIMENSIONAL, tgtDim, dimCRS);
        if (tgtDim == dimCRS && geometry.getDimension() == gridDimensions.length) {
            final GridExtent extent = geometry.extent;
            if (sliceExtent == null) {
                return extent == null || extent.startsAtZero();
            } else if (sliceExtent.equals(extent, ComparisonMode.IGNORE_METADATA)) {
                return sliceExtent.startsAtZero();
            }
        }
        return false;
    }

    /**
     * Sets the data as a Java2D buffer. The {@linkplain DataBuffer#getNumBanks() number of banks}
     * in the given buffer must be equal to the {@linkplain #getNumBands() expected number of bands}.
     * In each bank, the value located at the {@linkplain DataBuffer#getOffsets() bank offset} is the value
     * located at <code>{@linkplain GridCoverage#getGridGeometry()}.{@linkplain GridGeometry#getExtent()
     * getExtent()}.{@linkplain GridExtent#getLow(int) getLow()}</code>, as specified in class javadoc.
     *
     * @param  data  the Java2D buffer containing data for all bands.
     * @throws NullPointerException if {@code data} is null.
     * @throws MismatchedCoverageRangeException if the given data buffer does not have the expected number of banks.
     */
    public void setData(final DataBuffer data) {
        ensureExpectedBandCount(data.getNumBanks(), true);
        buffer = data;
    }

    /**
     * Sets the data as NIO buffers. The number of buffers must be equal to the {@linkplain #getNumBands() expected
     * number of bands}. All buffers must be {@linkplain Buffer#array() backed by arrays} of the type specified by
     * the {@code dataType} argument and have the same number of {@linkplain Buffer#remaining() remaining elements}.
     * This method wraps the underlying arrays of a primitive type into a Java2D buffer; data are not copied.
     * For each buffer, the grid coverage data (not only the slice data) starts at {@linkplain Buffer#position()
     * buffer position} and ends at that position + {@linkplain Buffer#remaining() remaining}.
     *
     * <p>The data type must be specified in order to distinguish between the signed and unsigned types.
     * {@link DataType#BYTE} and {@link DataType#USHORT} are unsigned, all other supported types are signed.</p>
     *
     * <p><b>Implementation note:</b> the Java2D buffer is set by a call to {@link #setData(DataBuffer)},
     * which can be overridden by subclasses if desired.</p>
     *
     * @param  dataType  type of data.
     * @param  data  the buffers wrapping arrays of primitive type.
     * @throws NullPointerException if {@code data} is null or one of {@code data} element is null.
     * @throws MismatchedCoverageRangeException if the number of specified buffers is not equal to the number of bands.
     * @throws UnsupportedOperationException if a buffer is not backed by an accessible array or is read-only.
     * @throws ArrayStoreException if a buffer type is incompatible with {@code dataType}.
     * @throws RasterFormatException if buffers do not have the same number of remaining values.
     * @throws ArithmeticException if a buffer position overflows the 32 bits integer capacity.
     *
     * @since 1.1
     */
    public void setData(final DataType dataType, final Buffer... data) {
        ArgumentChecks.ensureNonNull("dataType", dataType);
        ensureExpectedBandCount(data.length, true);
        setData(RasterFactory.wrap(dataType, data));
    }

    /**
     * Sets the data as vectors. The number of vectors must be equal to the {@linkplain #getNumBands() expected number of bands}.
     * All vectors must be backed by arrays (indirectly, through {@linkplain Vector#buffer() buffers} backed by arrays) and have
     * the same {@linkplain Vector#size() size}.
     * This method wraps the underlying arrays of a primitive type into a Java2D buffer; data are not copied.
     *
     * <p><b>Implementation note:</b> the NIO buffers are set by a call to {@link #setData(DataType, Buffer...)},
     * which can be overridden by subclasses if desired.</p>
     *
     * @param  data  the vectors wrapping arrays of primitive type.
     * @throws NullPointerException if {@code data} is null or one of {@code data} element is null.
     * @throws MismatchedCoverageRangeException if the number of specified vectors is not equal to the number of bands.
     * @throws UnsupportedOperationException if a vector is not backed by an accessible array or is read-only.
     * @throws RasterFormatException if vectors do not have the same size.
     * @throws ArithmeticException if a buffer position overflows the 32 bits integer capacity.
     */
    public void setData(final Vector... data) {
        ensureExpectedBandCount(data.length, true);
        final var buffers = new Buffer[data.length];
        DataType dataType = null;
        for (int i=0; i<data.length; i++) {
            final Vector v = data[i];
            ArgumentChecks.ensureNonNullElement("data", i, v);
            final DataType t = DataType.forPrimitiveType(v.getElementType(), v.isUnsigned());
            if (dataType == null) {
                dataType = t;
            } else if (dataType != t) {
                throw new RasterFormatException(Resources.format(Resources.Keys.MismatchedDataType));
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
     * <h4>Example</h4>
     * For an image having three bands named Red (R), Green (G) and Blue (B), if the sample values are stored in a single bank in a
     * R₀,G₀,B₀, R₁,G₁,B₁, R₂,G₂,B₂, R₃,G₃,B₃, <i>etc.</i> fashion, then this method should be invoked as below:
     *
     * {@snippet lang="java" :
     *     setInterleavedPixelOffsets(3, new int[] {0, 1, 2});
     *     }
     *
     * @param  pixelStride  the number of data elements between each pixel in the data vector or buffer.
     * @param  bandOffsets  offsets to add to sample index in each band. This is typically {0, 1, 2, …}.
     *                      The length of this array shall be equal to {@link #getNumBands()}.
     */
    public void setInterleavedPixelOffsets(final int pixelStride, final int[] bandOffsets) {
        ArgumentChecks.ensureStrictlyPositive("pixelStride", pixelStride);
        ensureExpectedBandCount(bandOffsets.length, false);
        this.strideFactor = pixelStride;
        this.bandOffsets = bandOffsets.clone();
    }

    /**
     * Specifies the band to use for defining pixel colors when the image is displayed on screen.
     * All other bands, if any, will exist in the raster but be ignored at display time.
     * The default value is 0, the first (and often only) band.
     *
     * <h4>Implementation note</h4>
     * An {@link java.awt.image.IndexColorModel} will be used for displaying the image.
     *
     * @param  band  the band to use for display purpose.
     * @throws IllegalArgumentException if the given band is not between 0 (inclusive)
     *         and {@link #getNumBands()} (exclusive).
     *
     * @see org.apache.sis.image.Colorizer.Target#getVisibleBand()
     *
     * @since 1.2
     */
    public void setVisibleBand(final int band) {
        ArgumentChecks.ensureBetween("band", 0, getNumBands() - 1, band);
        visibleBand = band;
    }

    /**
     * Specifies the colors to apply for each category in a sample dimension.
     * The given function can return {@code null} for unrecognized categories.
     * If this method is never invoked, or if a category is unrecognized,
     * then the default is a grayscale for
     * {@linkplain Category#isQuantitative() quantitative categories} and
     * transparent for qualitative categories (typically "no data" values).
     *
     * <h4>Example</h4>
     * The following code specifies a color palette from blue to red with white in the middle.
     * This is useful for data with a clear 0 (white) in the middle of the range,
     * with a minimal value equals to the negative of the maximal value.
     *
     * {@snippet lang="java" :
     *     setCategoryColors((category) -> category.isQuantitative() ? new Color[] {
     *             Color.BLUE, Color.CYAN, Color.WHITE, Color.YELLOW, Color.RED} : null);
     *     }
     *
     * @param colors  the colors to use for each category. The {@code colors} argument cannot be null,
     *                but {@code colors.apply(Category)} can return null.
     *
     * @since 1.2
     */
    public void setCategoryColors(final Function<Category,Color[]> colors) {
        this.colors = Objects.requireNonNull(colors);
    }

    /**
     * Creates a raster with the data specified by the last call to a {@code setData(…)} method.
     * The raster upper-left corner is located at the position given by {@link #getBounds()}.
     * The returned raster is often an instance of {@link WritableRaster}, but read-only rasters are also allowed.
     *
     * @return the raster, usually (but not necessarily) an instance of {@link WritableRaster}.
     * @throws IllegalStateException if no {@code setData(…)} method has been invoked before this method call.
     * @throws RasterFormatException if a call to a {@link Raster} factory method failed.
     * @throws ArithmeticException if a property of the raster to construct exceeds the capacity of 32 bits integers.
     *
     * @since 1.1
     */
    public Raster createRaster() {
        if (buffer == null) {
            throw new IllegalStateException(Resources.format(Resources.Keys.UnspecifiedRasterData));
        }
        final boolean isInterleaved = (buffer.getNumBanks() == 1);
        if (bandOffsets == null) {
            strideFactor = isInterleaved ? getNumBands() : 1;
        }
        final int ls = multiplyExact(scanlineStride, strideFactor);     // Real scanline stride.
        final int ps = pixelStride * strideFactor;                      // Cannot fail if above operation did not fail.
        /*
         * Number of data elements from the first element of the bank to the first sample of the band.
         * This is usually 0 for all bands, unless the upper-left corner (minX, minY) is not (0,0).
         */
        final int[] offsets = new int[getNumBands()];
        Arrays.fill(offsets, toIntExact(addExact(addExact(
                multiplyExact(offsetX, ps),
                multiplyExact(offsetY, ls)),
                              offsetZ)));
        /*
         * Add the offset specified by the user (if any), or the default offset. The default is 0, 1, 2…
         * for interleaved sample model (all bands in one bank) and 0, 0, 0… for banded sample model.
         */
        if (bandOffsets != null || isInterleaved) {
            for (int i=0; i<offsets.length; i++) {
                offsets[i] = addExact(offsets[i], (bandOffsets != null) ? bandOffsets[i] : i);
            }
        }
        final Point location = new Point(imageX, imageY);
        return RasterFactory.createRaster(buffer, width, height, ps, ls, bankIndices, offsets, location);
    }

    /**
     * Creates an image with the data specified by the last call to a {@code setData(…)} method.
     * The image upper-left corner is located at the position given by {@link #getBounds()}.
     * The two-dimensional {@linkplain #getImageGeometry(int) image geometry} is stored as
     * a property associated to the {@value org.apache.sis.image.PlanarImage#GRID_GEOMETRY_KEY} key.
     * The dimensions of the source grid that are represented in the image are associated to the
     * {@value org.apache.sis.image.PlanarImage#XY_DIMENSIONS_KEY} key.
     * The sample dimensions are stored as a property associated to the
     * {@value org.apache.sis.image.PlanarImage#SAMPLE_DIMENSIONS_KEY} key.
     *
     * <p>The default implementation returns an instance of {@link java.awt.image.WritableRenderedImage}
     * if the {@link #createRaster()} return value is an instance of {@link WritableRaster},
     * or a read-only {@link RenderedImage} otherwise.</p>
     *
     * @return the image.
     * @throws IllegalStateException if no {@code setData(…)} method has been invoked before this method call.
     * @throws RasterFormatException if a call to a {@link Raster} factory method failed.
     * @throws ArithmeticException if a property of the image to construct exceeds the capacity of 32 bits integers.
     *
     * @since 1.1
     */
    @SuppressWarnings("UseOfObsoleteCollectionType")
    public RenderedImage createImage() {
        final Raster raster = createRaster();
        final var colorizer = new ColorScaleBuilder(colors, null, false);
        final ColorModel cm;
        final SampleModel sm = raster.getSampleModel();
        if (colorizer.initialize(sm, bands[visibleBand]) || colorizer.initialize(sm, visibleBand)) {
            DataType type = DataType.forDataBufferType(buffer.getDataType());
            cm = colorizer.createColorModel(type, bands.length, visibleBand);
        } else {
            cm = ColorScaleBuilder.NULL_COLOR_MODEL;
        }
        SliceGeometry supplier = null;
        if (imageGeometry == null) {
            if (imageUseSameGeometry(GridCoverage2D.BIDIMENSIONAL)) {
                imageGeometry = geometry;
            } else {
                supplier = new SliceGeometry(geometry, sliceExtent, gridDimensions, mtFactory);
            }
        }
        final WritableRaster wr = (raster instanceof WritableRaster) ? (WritableRaster) raster : null;
        if (wr != null && cm != null && (imageX | imageY) == 0) {
            return new Untiled(cm, wr, properties, gridDimensions, imageGeometry, supplier, bands);
        }
        if (properties == null) {
            properties = new Hashtable<>();
        }
        properties.putIfAbsent(XY_DIMENSIONS_KEY, gridDimensions);
        properties.putIfAbsent(GRID_GEOMETRY_KEY, (supplier != null) ? new DeferredProperty(supplier) : imageGeometry);
        properties.putIfAbsent(SAMPLE_DIMENSIONS_KEY, bands);
        if (wr != null) {
            return new WritableTiledImage(properties, cm, width, height, 0, 0, wr);
        } else {
            return new TiledImage(properties, cm, width, height, 0, 0, raster);
        }
    }

    /**
     * A {@link BufferedImage} which will compute the {@value org.apache.sis.image.PlanarImage#GRID_GEOMETRY_KEY}
     * property when first needed. We use this class even when the property value is known in advance because it
     * has the desired side-effect of not letting {@link #getSubimage(int, int, int, int)} inherit that property.
     * The use of a {@link BufferedImage} subclass is desired because Java2D rendering pipeline has optimizations
     * in the form {@code if (image instanceof BufferedImage)}.
     */
    private static final class Untiled extends ObservableImage {
        /**
         * The value associated to the {@value org.apache.sis.image.PlanarImage#XY_DIMENSIONS_KEY} key.
         */
        private final int[] gridDimensions;

        /**
         * The value associated to the {@value org.apache.sis.image.PlanarImage#GRID_GEOMETRY_KEY} key,
         * or {@code null} if not yet computed.
         */
        private GridGeometry geometry;

        /**
         * The object to use for computing {@link #geometry}, or {@code null} if not needed.
         * This field is cleared after {@link #geometry} has been computed.
         */
        private SliceGeometry supplier;

        /**
         * The value associated to the {@value org.apache.sis.image.PlanarImage#SAMPLE_DIMENSIONS_KEY} key.
         */
        private final SampleDimension[] bands;

        /**
         * Creates a new buffered image wrapping the given raster.
         */
        @SuppressWarnings("UseOfObsoleteCollectionType")
        Untiled(final ColorModel colors, final WritableRaster raster, final Hashtable<?,?> properties,
                final int[] gridDimensions, final GridGeometry geometry, final SliceGeometry supplier, final SampleDimension[] bands)
        {
            super(colors, raster, false, properties);
            this.gridDimensions = gridDimensions;
            this.geometry       = geometry;
            this.supplier       = supplier;
            this.bands          = bands;
        }

        /**
         * Returns the names of properties that this image can provide.
         */
        @Override
        public String[] getPropertyNames() {
            return ArraysExt.concatenate(super.getPropertyNames(), new String[] {
                    XY_DIMENSIONS_KEY,
                    GRID_GEOMETRY_KEY,
                    SAMPLE_DIMENSIONS_KEY});
        }

        /**
         * Returns the property associated to the given key.
         * If the key is {@value org.apache.sis.image.PlanarImage#GRID_GEOMETRY_KEY},
         * then the {@link GridGeometry} will be computed when first needed.
         *
         * @throws ImagingOpException if the property value cannot be computed.
         */
        @Override
        public Object getProperty(final String key) {
            switch (key) {
                default: return super.getProperty(key);
                case SAMPLE_DIMENSIONS_KEY: return bands.clone();
                case XY_DIMENSIONS_KEY: return gridDimensions.clone();
                case GRID_GEOMETRY_KEY: {
                    synchronized (this) {
                        if (geometry == null) {
                            final SliceGeometry s = supplier;
                            if (s != null) {
                                supplier = null;                // Let GC do its work.
                                geometry = s.apply(this);
                            }
                        }
                        return geometry;
                    }
                }
            }
        }
    }
}
